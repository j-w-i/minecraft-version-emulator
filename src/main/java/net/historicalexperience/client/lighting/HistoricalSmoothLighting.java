package net.historicalexperience.client.lighting;

import net.historicalexperience.client.HistoricalClientRules;
import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;

/**
 * The Beta 1.3–1.7.3 smooth lighting engine, which Beta 1.8 replaced.
 *
 * <h2>What "old smooth lighting" actually is</h2>
 *
 * <p>Not a different lighting model - the two engines differ in two specific places, and this class gates both:
 *
 * <ol>
 *   <li><b>Corner shading.</b> The modern engine darkens a corner towards 0.2 in {@code getShadeBrightness}; from
 *       Beta 1.3 to Beta 1.7.3 it did not darken the corner at all. That is the whole of the block-geometry side,
 *       and it is the rule ported from Nostalgic Tweaks' {@code BlockBehaviourMixin}.</li>
 *   <li><b>The lightmap.</b> The brightness curve per light level is the other half. It is recorded here as a known
 *       gap rather than claimed: this class does not yet carry the lightmap table, so the corner shading applies
 *       and the curve does not.</li>
 * </ol>
 *
 * <h2>Sources for the boundary</h2>
 *
 * <p>minecraft.wiki, Light, History / Java Edition:
 *
 * <ul>
 *   <li><i>"1.3 — Smooth lighting engine added with the help of MrMessiah and his 'BetterLight' mod."</i></li>
 *   <li><i>"1.8 Pre-release — Implemented a new lighting engine. The lighting on a block is given a tint based on
 *       the most prominent source of light."</i></li>
 * </ul>
 *
 * <p>So the window is <b>Beta 1.3 inclusive to Beta 1.8 exclusive</b>, and {@link #availableFor} is written as two
 * comparisons rather than one boundary for that reason - a single "older than Beta 1.8" test would also cover every
 * version before Beta 1.3, where there was no smooth lighting at all and the correct behaviour is
 * {@link HistoricalLightingEngine}'s classic engine instead. The two windows are disjoint by construction, which is
 * the property a single "old lighting" boolean would have got wrong.
 *
 * <p>An unknown version answers false. A client that guessed would render one thing while the server believed
 * another, which is the failure mode {@link HistoricalLightingEngine} documents at greater length.
 */
public final class HistoricalSmoothLighting {
    /** The build that introduced the smooth lighting engine, so the window opens here. */
    private static final String FIRST_SMOOTH_LIGHTING_BUILD = "historical_experience:beta_1_3";

    /** The build that replaced it, so the window closes before this one. */
    private static final String FIRST_NEW_LIGHTING_BUILD = "historical_experience:beta_1_8_1";

    private HistoricalSmoothLighting() {
    }

    /** Whether the 1.3-1.7.3 engine is the one this version should render with. */
    public static boolean availableFor(HistoricalVersion version) {
        if (version == null) {
            return false;
        }
        var first = HistoricalVersions.byId(FIRST_SMOOTH_LIGHTING_BUILD).orElse(null);
        var replaced = HistoricalVersions.byId(FIRST_NEW_LIGHTING_BUILD).orElse(null);
        if (first == null || replaced == null) {
            // A registry that cannot resolve its own boundary constants must not guess a window.
            return false;
        }
        return version.chronology() >= first.chronology() && version.chronology() < replaced.chronology();
    }

    /** Whether the old engine should actually be used for the client's current version. */
    public static boolean activeNow() {
        return availableFor(HistoricalClientRules.activeVersion());
    }
}
