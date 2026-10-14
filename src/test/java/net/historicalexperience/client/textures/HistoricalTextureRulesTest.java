/*
 * Minecraft Historical Experience
 * Copyright (C) 2026 Minecraft Historical Experience contributors
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package net.historicalexperience.client.textures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Version to texture-stack resolution, asserted against the real registry's chronology.
 *
 * <p>The pure plan is what the resource layer asks, so these tests assert the behaviour that ships rather than a model
 * of it.
 */
class HistoricalTextureRulesTest {
    private static final String MC = "minecraft";
    private static final String COBBLE = "textures/block/cobblestone.png";
    private static final String XP_BG = "textures/gui/sprites/hud/experience_bar_background.png";
    private static final String XP_FILL = "textures/gui/sprites/hud/experience_bar_progress.png";
    private static final Set<TextureSource> ALL_INSTALLED = EnumSet.allOf(TextureSource.class);

    private static HistoricalVersion at(String id) {
        return HistoricalVersions.byId("historical_experience:" + id)
            .orElseThrow(() -> new AssertionError("no such version: " + id));
    }

    private static List<TextureSource> stack(String id) {
        return HistoricalTextureRules.planFor(at(id)).sources().stream()
            .filter(TextureSource::isDependency).toList();
    }

    private static final List<TextureSource> PA_ONLY = List.of(TextureSource.PROGRAMMER_ART, TextureSource.CONTINUATION);
    private static final List<TextureSource> WITH_BASE = List.of(TextureSource.PROGRAMMER_ART,
        TextureSource.CONTINUATION, TextureSource.GOLDEN_DAYS_BASE);
    private static final List<TextureSource> WITH_ALPHA = List.of(TextureSource.PROGRAMMER_ART,
        TextureSource.CONTINUATION, TextureSource.GOLDEN_DAYS_BASE, TextureSource.GOLDEN_DAYS_ALPHA);

    @Test
    @DisplayName("pre-Alpha, Infdev and early Alpha get Programmer Art + Continuation + Golden Days Base + Alpha")
    void earlyEras() {
        assertEquals(WITH_ALPHA, stack("infdev_420"));
        assertEquals(WITH_ALPHA, stack("infdev_611"));
        assertEquals(WITH_ALPHA, stack("alpha_1_0_1"));
        assertEquals(WITH_ALPHA, stack("alpha_1_0_17"));
    }

    @Test
    @DisplayName("Alpha 1.2 and everything declared before it keep Golden Days Alpha; Alpha 1.2.0 is inclusive")
    void alphaBoundary() {
        assertEquals(WITH_ALPHA, stack("alpha_1_1_0"));
        assertEquals(WITH_ALPHA, stack("alpha_1_2_0"));
        // The first declared build after Alpha 1.2.x is Beta 1.0; no Alpha build sits between them.
        assertEquals(WITH_BASE, stack("beta_1_0"));
        assertTrue(HistoricalTextureRules.GOLDEN_DAYS_ALPHA_END > at("alpha_1_2_0").chronology());
        assertTrue(HistoricalTextureRules.GOLDEN_DAYS_ALPHA_END <= at("beta_1_0").chronology());
    }

    @Test
    @DisplayName("after Alpha 1.2 through Release 1.2.5: Programmer Art + Continuation + Golden Days Base")
    void middleEra() {
        assertEquals(WITH_BASE, stack("beta_1_0"));
        assertEquals(WITH_BASE, stack("beta_1_6"));
        assertEquals(WITH_BASE, stack("beta_1_7_3"));
        assertEquals(WITH_BASE, stack("beta_1_8_1"));
        assertEquals(WITH_BASE, stack("beta_1_9_pre_1"));
        assertEquals(WITH_BASE, stack("beta_1_9_pre_6"));
        assertEquals(WITH_BASE, stack("release_1_0_0"));
        assertEquals(WITH_BASE, stack("release_1_1"));
        assertEquals(WITH_BASE, stack("release_1_2_5"));
    }

    @Test
    @DisplayName("after Release 1.2.5: Programmer Art + Continuation only")
    void afterReleaseOneTwoFive() {
        assertEquals(PA_ONLY, stack("release_1_3"));
        assertEquals(PA_ONLY, stack("release_1_4"));
        assertEquals(PA_ONLY, stack("release_1_5"));
        assertEquals(PA_ONLY, stack("release_1_6_4"));
        assertTrue(HistoricalTextureRules.GOLDEN_DAYS_BASE_END > at("release_1_2_5").chronology());
        assertTrue(HistoricalTextureRules.GOLDEN_DAYS_BASE_END <= at("release_1_3").chronology());
    }

    @Test
    @DisplayName("the stack is placed by chronology, so a snapshot or in-between key lands on the right side")
    void chronologyNotNames() {
        int betweenAlphaAndBeta = at("alpha_1_2_0").chronology() + 1;
        assertEquals(HistoricalTextureRules.planFor(betweenAlphaAndBeta).sources().contains(TextureSource.GOLDEN_DAYS_ALPHA),
            betweenAlphaAndBeta < HistoricalTextureRules.GOLDEN_DAYS_ALPHA_END);
        // A key one step past 1.2.5 - what a 1.2.6 build would be - is already outside Golden Days Base.
        int justAfter = at("release_1_2_5").chronology() + 1;
        assertFalse(HistoricalTextureRules.planFor(justAfter).sources().contains(TextureSource.GOLDEN_DAYS_BASE));
        // And one step before the cobblestone change is still on the Golden Days side.
        int beta16 = HistoricalTextureRules.COBBLESTONE_CHANGE - 1;
        assertTrue(HistoricalTextureRules.planFor(beta16).serves(TextureSource.GOLDEN_DAYS_BASE, MC, COBBLE));
        // Programmer Art and Continuation are the baseline of every historical version, with nothing skipped.
        for (HistoricalVersion version : HistoricalVersions.all()) {
            TexturePlan plan = HistoricalTextureRules.planFor(version);
            assertTrue(plan.sources().contains(TextureSource.PROGRAMMER_ART), version.id());
            assertTrue(plan.sources().contains(TextureSource.CONTINUATION), version.id());
        }
    }

    @Test
    @DisplayName("an ordinary world (no historical version) has no plan and no signature")
    void ordinaryWorld() {
        TexturePlan plan = HistoricalTextureRules.planFor((HistoricalVersion) null);
        assertFalse(plan.isActive());
        assertEquals("none", plan.signature());
        for (TextureSource source : TextureSource.values()) {
            assertFalse(plan.serves(source, MC, COBBLE), "ordinary worlds are never touched: " + source);
        }
    }

    @Test
    @DisplayName("the Golden Days experience bar is never selected in any historical version")
    void xpBarNeverGoldenDays() {
        for (HistoricalVersion version : HistoricalVersions.all()) {
            TexturePlan plan = HistoricalTextureRules.planFor(version);
            for (String path : new String[] {XP_BG, XP_FILL}) {
                assertFalse(plan.serves(TextureSource.GOLDEN_DAYS_BASE, MC, path), version.id() + " base " + path);
                assertFalse(plan.serves(TextureSource.GOLDEN_DAYS_ALPHA, MC, path), version.id() + " alpha " + path);
                // The rule is "never Golden Days", so the winner is never a Golden Days source...
                TextureSource winner = plan.winner(ALL_INSTALLED, MC, path);
                assertTrue(winner != TextureSource.GOLDEN_DAYS_BASE && winner != TextureSource.GOLDEN_DAYS_ALPHA,
                    version.id() + " " + path + " won by " + winner);
                // ...and a user pack can never be the one to answer it: vanilla is pinned into the group.
                assertTrue(plan.serves(TextureSource.VANILLA_PIN, MC, path), version.id() + " pin " + path);
            }
        }
    }

    @Test
    @DisplayName("with the real packs' contents (no Programmer Art or Continuation XP bar) the XP bar is vanilla's")
    void xpBarIsVanillaWhenNoLowerPackHasIt() {
        // Verified against the installed packs: neither Programmer Art nor Continuation supplies these two sprites in
        // this game version, so once Golden Days is excluded nothing above vanilla has them. Modelled by naming only
        // the sources that really do not have the file as available.
        Set<TextureSource> withoutXpFiles = EnumSet.of(TextureSource.GOLDEN_DAYS_BASE, TextureSource.VANILLA_PIN);
        for (String id : new String[] {"alpha_1_0_1", "beta_1_7_3", "release_1_2_5", "release_1_6_4"}) {
            TexturePlan plan = HistoricalTextureRules.planFor(at(id));
            assertEquals(TextureSource.VANILLA_PIN, plan.winner(withoutXpFiles, MC, XP_BG), id);
            assertEquals(TextureSource.VANILLA_PIN, plan.winner(withoutXpFiles, MC, XP_FILL), id);
        }
    }

    @Test
    @DisplayName("the exclusion is scoped: Golden Days still serves its other GUI and block textures")
    void xpBarExclusionDoesNotRemoveUnrelatedGoldenDays() {
        TexturePlan plan = HistoricalTextureRules.planFor(at("beta_1_7_3"));
        assertTrue(plan.serves(TextureSource.GOLDEN_DAYS_BASE, MC, "textures/gui/icons.png"));
        assertTrue(plan.serves(TextureSource.GOLDEN_DAYS_BASE, MC, "textures/gui/sprites/hud/heart/full.png"));
        assertTrue(plan.serves(TextureSource.GOLDEN_DAYS_BASE, MC, "textures/block/stone.png"));
        assertTrue(plan.serves(TextureSource.GOLDEN_DAYS_BASE, MC, "textures/gui/sprites/hud/hotbar.png"));
        assertEquals(TextureSource.GOLDEN_DAYS_BASE, plan.winner(ALL_INSTALLED, MC, "textures/block/stone.png"));
    }

    @Test
    @DisplayName("a metadata companion follows the texture it describes, not a different source")
    void mcmetaFollowsImage() {
        TexturePlan plan = HistoricalTextureRules.planFor(at("beta_1_7_3"));
        // The XP-bar image is pinned away from Golden Days, so its .mcmeta is too.
        assertFalse(plan.serves(TextureSource.GOLDEN_DAYS_BASE, MC, XP_BG + ".mcmeta"));
        assertTrue(plan.serves(TextureSource.VANILLA_PIN, MC, XP_BG + ".mcmeta"));
        // Cobblestone at Beta 1.7.3 belongs to Programmer Art, so its .mcmeta must not come from Golden Days or
        // Continuation either: an animation file paired with the wrong image is the failure this prevents.
        assertTrue(plan.serves(TextureSource.PROGRAMMER_ART, MC, COBBLE + ".mcmeta"));
        assertFalse(plan.serves(TextureSource.GOLDEN_DAYS_BASE, MC, COBBLE + ".mcmeta"));
        assertFalse(plan.serves(TextureSource.CONTINUATION, MC, COBBLE + ".mcmeta"));
    }

    @Test
    @DisplayName("cobblestone is Golden Days through Beta 1.6")
    void cobblestoneGoldenDaysThroughBeta16() {
        for (String id : new String[] {"alpha_1_0_1", "alpha_1_2_0", "beta_1_0", "beta_1_3", "beta_1_5", "beta_1_6"}) {
            TexturePlan plan = HistoricalTextureRules.planFor(at(id));
            assertEquals(TextureSource.GOLDEN_DAYS_BASE, plan.winner(ALL_INSTALLED, MC, COBBLE), id);
            // Golden Days Alpha does not supply cobblestone and must not be chosen for it; the preference caps the rank.
            assertFalse(plan.serves(TextureSource.GOLDEN_DAYS_ALPHA, MC, COBBLE), id);
        }
    }

    @Test
    @DisplayName("cobblestone is Programmer Art from Beta 1.7, while Golden Days stays active for other textures")
    void cobblestoneProgrammerArtAfterBeta16() {
        for (String id : new String[] {"beta_1_7_3", "beta_1_8_1", "release_1_0_0", "release_1_2_5"}) {
            TexturePlan plan = HistoricalTextureRules.planFor(at(id));
            assertEquals(TextureSource.PROGRAMMER_ART, plan.winner(ALL_INSTALLED, MC, COBBLE), id);
            assertFalse(plan.serves(TextureSource.GOLDEN_DAYS_BASE, MC, COBBLE), id);
            // Continuation ranks above Programmer Art, so it must be stepped over for this path.
            assertFalse(plan.serves(TextureSource.CONTINUATION, MC, COBBLE), id);
        }
        // Golden Days is genuinely still on for everything else at Beta 1.7.3 - the boundary is per asset.
        TexturePlan beta173 = HistoricalTextureRules.planFor(at("beta_1_7_3"));
        assertEquals(TextureSource.GOLDEN_DAYS_BASE, beta173.winner(ALL_INSTALLED, MC, "textures/block/stone.png"));
    }

    @Test
    @DisplayName("after Release 1.2.5 cobblestone is whatever Programmer Art / Continuation give, with no rule at all")
    void cobblestoneAfterGoldenDays() {
        TexturePlan plan = HistoricalTextureRules.planFor(at("release_1_6_4"));
        assertEquals(TextureSource.PROGRAMMER_ART, plan.winner(ALL_INSTALLED, MC, COBBLE));
        // Continuation is allowed here (no cobblestone rule outlives Golden Days only if it is Programmer Art's),
        // but it has no cobblestone file in the real pack, so Programmer Art is the observable answer.
        assertTrue(plan.rules().stream().anyMatch(rule -> rule.id().equals("cobblestone_programmer_art")));
    }

    @Test
    @DisplayName("a missing preferred source degrades to the next source down instead of leaving a hole")
    void missingPreferredDegrades() {
        TexturePlan plan = HistoricalTextureRules.planFor(at("beta_1_6"));
        Set<TextureSource> withoutGoldenDays = EnumSet.of(TextureSource.PROGRAMMER_ART, TextureSource.CONTINUATION,
            TextureSource.VANILLA_PIN);
        assertEquals(TextureSource.CONTINUATION, plan.winner(withoutGoldenDays, MC, COBBLE));
        Set<TextureSource> nothing = EnumSet.noneOf(TextureSource.class);
        assertNull(plan.winner(nothing, MC, COBBLE), "no available source means no answer, so the game falls through");
    }

    @Test
    @DisplayName("an exclusion beats a preference declared for the same resource (conflict resolution)")
    void exclusionBeatsPreference() {
        AssetRule prefer = new AssetRule("prefer", MC, Set.of("textures/block/x.png"), VersionWindow.ALWAYS,
            Set.of(), TextureSource.GOLDEN_DAYS_BASE, false, "test");
        AssetRule exclude = new AssetRule("exclude", MC, Set.of("textures/block/x.png"), VersionWindow.ALWAYS,
            Set.of(TextureSource.GOLDEN_DAYS_BASE), null, false, "test");
        TexturePlan plan = new TexturePlan(0, List.of(TextureSource.PROGRAMMER_ART, TextureSource.GOLDEN_DAYS_BASE),
            List.of(prefer, exclude));
        assertFalse(plan.serves(TextureSource.GOLDEN_DAYS_BASE, MC, "textures/block/x.png"));
        assertEquals(TextureSource.PROGRAMMER_ART,
            plan.winner(EnumSet.of(TextureSource.PROGRAMMER_ART, TextureSource.GOLDEN_DAYS_BASE), MC,
                "textures/block/x.png"));
    }

    @Test
    @DisplayName("adjacent versions with the same effective stack share a signature, so no reload is needed between them")
    void signaturesAvoidNeedlessReloads() {
        assertEquals(HistoricalTextureRules.planFor(at("beta_1_8_1")).signature(),
            HistoricalTextureRules.planFor(at("beta_1_9_pre_3")).signature());
        assertEquals(HistoricalTextureRules.planFor(at("release_1_0_0")).signature(),
            HistoricalTextureRules.planFor(at("release_1_1")).signature());
        assertEquals(HistoricalTextureRules.planFor(at("release_1_3")).signature(),
            HistoricalTextureRules.planFor(at("release_1_6_4")).signature());
        // Crossing a real boundary changes it.
        assertFalse(HistoricalTextureRules.planFor(at("alpha_1_2_0")).signature()
            .equals(HistoricalTextureRules.planFor(at("beta_1_0")).signature()));
        assertFalse(HistoricalTextureRules.planFor(at("beta_1_6")).signature()
            .equals(HistoricalTextureRules.planFor(at("beta_1_7_3")).signature()));
        assertFalse(HistoricalTextureRules.planFor(at("release_1_2_5")).signature()
            .equals(HistoricalTextureRules.planFor(at("release_1_3")).signature()));
    }
}
