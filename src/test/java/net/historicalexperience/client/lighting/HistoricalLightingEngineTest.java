package net.historicalexperience.client.lighting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.historicalexperience.version.HistoricalVersions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The classic engine's version boundary, its toggle, and the ported light rule.
 *
 * <h2>What is asserted and why these are the things that can silently break</h2>
 *
 * <p>Every failure mode here is silent. A wrong boundary shows old lighting in Beta 1.3 or new lighting in Beta
 * 1.2 and nothing errors; a toggle that applies where it is not offered changes a render the player did not ask
 * for; and an inverted sky rule gives full brightness underground, which looks like a mod conflict rather than a
 * porting mistake. So the boundary is asserted in BOTH directions, the rule is asserted as a table including the
 * pass-through cases whose ORDER matters in the upstream code, and an unknown version is asserted to mean "leave
 * lighting alone".
 *
 * <p>Level: unit. These are pure functions; the runtime half is `<code>getLightValue</code>` and is exercised by a
 * server probe plus the client itself.
 */
class HistoricalLightingEngineTest {
    private static final String BETA_1_2 = "historical_experience:beta_1_2";
    private static final String BETA_1_3 = "historical_experience:beta_1_3";

    @Test
    @DisplayName("offered for every build before Beta 1.3 and removed from Beta 1.3 onward")
    void theToggleExistsOnlyBeforeBetaOneThree() {
        assertTrue(HistoricalLightingEngine.availableFor(HistoricalVersions.byId(BETA_1_2).orElseThrow()),
            "Beta 1.2 predates the smooth-lighting engine, so the toggle must be offered there");

        assertFalse(HistoricalLightingEngine.availableFor(HistoricalVersions.byId(BETA_1_3).orElseThrow()),
            "Beta 1.3 ADDED the smooth-lighting engine (research/minecraft-version-history.md), so from 1.3 the "
                + "toggle must be REMOVED rather than offered and ignored - this is the owner's stated boundary");

        // The rest of the matrix, both sides, so a boundary moved by accident cannot pass.
        int offered = 0;
        int removed = 0;
        for (var version : HistoricalVersions.all()) {
            boolean available = HistoricalLightingEngine.availableFor(version);
            if (version.id().equals(BETA_1_3) || version.chronology() > HistoricalVersions.byId(BETA_1_3)
                    .orElseThrow().chronology()) {
                assertFalse(available, version.id() + " is Beta 1.3 or later and must not offer the toggle");
                removed++;
            } else {
                assertTrue(available, version.id() + " predates Beta 1.3 and must offer the toggle");
                offered++;
            }
        }
        assertTrue(offered > 8, "expected the pre-Beta-1.3 group to be substantial, saw " + offered);
        assertTrue(removed > 8, "expected the Beta-1.3-and-later group to be substantial, saw " + removed);
    }

    @Test
    @DisplayName("an unknown version leaves lighting alone, and the toggle cannot be switched on where it is absent")
    void unknownMeansDoNothing() {
        assertFalse(HistoricalLightingEngine.availableFor(null), "unknown is not 'yes'");
        assertFalse(HistoricalLightingEngine.activeFor(null, true),
            "a client that cannot establish the version must not change lighting: it would render one thing while "
                + "the server believed another");

        // Off is off, wherever it is offered.
        assertFalse(HistoricalLightingEngine.activeFor(HistoricalVersions.byId(BETA_1_2).orElseThrow(), false));

        // On is only on where it is offered.
        assertTrue(HistoricalLightingEngine.activeFor(HistoricalVersions.byId(BETA_1_2).orElseThrow(), true));
        assertFalse(HistoricalLightingEngine.activeFor(HistoricalVersions.byId(BETA_1_3).orElseThrow(), true),
            "Beta 1.3 must ignore the toggle even if a stale config says it is on");
    }

    @Test
    @DisplayName("block light is the constant 13 the upstream engine returns")
    void blockLightIsConstant() {
        assertEquals(13, HistoricalLightingEngine.classicBlockLight(),
            "NostalgicDataLayer returns 13 for the BLOCK layer under this engine: torches and lamps made no "
                + "difference in Classic, and that is the behaviour being reproduced");
    }

    @Test
    @DisplayName("sky light is binary below the extremes, and the upstream pass-through order is preserved")
    void skyLightTable() {
        // A dimension with no sky light is 0 BEFORE anything else, as upstream does.
        assertEquals(0, HistoricalLightingEngine.classicSkyLight(15, 15, false, false));

        // Values already at either extreme pass through untouched, which is also where upstream returns early.
        assertEquals(15, HistoricalLightingEngine.classicSkyLight(15, 15, true, false));
        assertEquals(0, HistoricalLightingEngine.classicSkyLight(0, 15, true, false));

        // In between, it is all or nothing: the sky is visible, or something blocks it.
        assertEquals(15, HistoricalLightingEngine.classicSkyLight(7, 15, true, false));
        assertEquals(0, HistoricalLightingEngine.classicSkyLight(7, 15, true, true));

        // The extreme pass-through beats the obstruction test, because upstream returns before scanning upward.
        assertEquals(15, HistoricalLightingEngine.classicSkyLight(15, 15, true, true));
    }

    @Test
    @DisplayName("the mixin's injection target still exists with the signature the handler assumes")
    void theInjectionTargetIsReal() throws Exception {
        // The mixin lives in the CLIENT section of the mixin config, so a dedicated-server probe never loads it and
        // cannot prove it applies - the probe only proves it no longer crashes a server, which it did while it was
        // wrongly listed as common. This is the cheap half of that verification: the named target must exist with
        // the return type the handler is written against, so a mapping change or a rename fails here instead of
        // silently leaving lighting modern.
        var method = net.minecraft.world.level.lighting.LightEngine.class
            .getMethod("getLightValue", net.minecraft.core.BlockPos.class);
        assertEquals(int.class, method.getReturnType(),
            "@ModifyReturnValue's handler returns the modified light value, so the target must still return int");

        // And the handler's own annotation must name that exact method, so the two cannot drift apart.
        var annotation = Class.forName("net.historicalexperience.mixin.LightEngineHistoricalClassicMixin")
            .getDeclaredMethod("historicalExperience$classicLightValue", int.class,
                net.minecraft.core.BlockPos.class)
            .getAnnotation(com.llamalad7.mixinextras.injector.ModifyReturnValue.class);
        assertTrue(annotation != null, "the handler must still carry @ModifyReturnValue");
        // MixinExtras exposes this as a String[] in this version, so compare the whole array rather than assuming.
        assertEquals(java.util.List.of("getLightValue"), java.util.List.of(annotation.method()),
            "the handler must name the method this test just proved exists");
    }
}
