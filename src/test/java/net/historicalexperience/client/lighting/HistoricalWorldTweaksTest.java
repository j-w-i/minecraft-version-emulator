package net.historicalexperience.client.lighting;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The windows for the sky, cloud, item-rendering and lighting toggles, asserted against the registry.
 *
 * <p>Each boundary here rests on a quotation recorded on {@link HistoricalWorldTweaks} or
 * {@link HistoricalLightingTweaks}. The point of the test is that the window cannot drift silently: a version moved
 * across a boundary fails here rather than changing what a world renders.
 */
class HistoricalWorldTweaksTest {
    private static HistoricalVersion at(String id) {
        return HistoricalVersions.byId(id).orElseThrow(() -> new AssertionError("no such version: " + id));
    }

    @Test
    @DisplayName("clouds are at the old height only for the builds before Beta 1.8, not for the later ones")
    void cloudHeight() {
        // The declared catalogue spans BOTH sides of Beta 1.8: Infdev to Beta 1.7.3 are below it, and Beta 1.8.1
        // through Release 1.6.4 are above it. So the old height is NOT every declared version, which is what an
        // earlier draft of this test asserted and what the test then caught.
        assertTrue(HistoricalWorldTweaks.availableForCloudHeight(at("historical_experience:infdev_420")));
        assertTrue(HistoricalWorldTweaks.availableForCloudHeight(at("historical_experience:alpha_1_0_1")));
        assertTrue(HistoricalWorldTweaks.availableForCloudHeight(at("historical_experience:beta_1_7_3")));
        assertFalse(HistoricalWorldTweaks.availableForCloudHeight(at("historical_experience:beta_1_8_1")),
            "Beta 1.8 raised the clouds to 128");
        assertFalse(HistoricalWorldTweaks.availableForCloudHeight(at("historical_experience:release_1_6_4")),
            "Release 1.6.4 is AFTER Beta 1.8, so it has the raised clouds");
        assertFalse(HistoricalWorldTweaks.availableForCloudHeight(null), "unknown is not 'yes'");
    }

    @Test
    @DisplayName("2D dropped items apply before the Release 1.4 profile, which is where the 3D model arrived")
    void twoDimensionalItems() {
        // Item (entity) history dates the 3D spinning model to 12w50a, released in 1.4.6.
        assertTrue(HistoricalWorldTweaks.availableFor2dItems(at("historical_experience:alpha_1_0_1")));
        assertTrue(HistoricalWorldTweaks.availableFor2dItems(at("historical_experience:beta_1_7_3")));
        assertTrue(HistoricalWorldTweaks.availableFor2dItems(at("historical_experience:release_1_3")));
        assertFalse(HistoricalWorldTweaks.availableFor2dItems(at("historical_experience:release_1_4")),
            "the Release 1.4 profile is 1.4.x, which has the 3D item");
        assertFalse(HistoricalWorldTweaks.availableFor2dItems(at("historical_experience:release_1_6_4")));
    }

    @Test
    @DisplayName("the recipe book is offered for no declared version, because it was added in 1.12")
    void recipeBook() {
        for (HistoricalVersion version : HistoricalVersions.all()) {
            assertFalse(HistoricalWorldTweaks.availableForRecipeBook(version),
                version.id() + " predates the recipe book (1.12), so the option must not be offered for it");
        }
    }

    @Test
    @DisplayName("leaves and water lighting are on before Beta 1.8 and unavailable from it")
    void lightingTweaks() {
        assertTrue(HistoricalLightingTweaks.availableFor(at("historical_experience:alpha_1_0_1")));
        assertTrue(HistoricalLightingTweaks.availableFor(at("historical_experience:beta_1_7_3")));
        assertFalse(HistoricalLightingTweaks.availableFor(at("historical_experience:beta_1_8_1")),
            "Beta 1.8 replaced the lighting engine these rules belong to");
        assertFalse(HistoricalLightingTweaks.availableFor(null));
        // Upstream's water light-block figure, pinned so it cannot drift.
        org.junit.jupiter.api.Assertions.assertEquals(3, HistoricalLightingTweaks.OLD_WATER_LIGHT_BLOCK);
    }

    @Test
    @DisplayName("every toggle is inert with no historical version active, so an ordinary world is untouched")
    void ordinaryWorldsAreUntouched() {
        // The settings read the active version, which is unset in a unit test, so all of these answer false.
        assertFalse(HistoricalWorldTweaks.oldCloudHeight());
        assertFalse(HistoricalWorldTweaks.old2dItems());
        assertFalse(HistoricalWorldTweaks.recipeBookDisabled());
        assertFalse(HistoricalLightingTweaks.oldLeavesLighting());
        assertFalse(HistoricalLightingTweaks.oldWaterLighting());
    }
}
