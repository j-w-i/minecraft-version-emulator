package net.historicalexperience.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.historicalexperience.HistoricalExperience;
import net.historicalexperience.world.metadata.HistoricalIdentifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the declarative profile table.
 *
 * <p>Level: unit. No Minecraft runtime.
 */
class HistoricalProfilesTest {
    @Test
    @DisplayName("the Beta 1.7.3 profile is bound to the pinned Moderner Beta preset")
    void beta173IsBoundToPinnedPreset() {
        HistoricalProfile profile = HistoricalProfiles.beta173();

        assertEquals(HistoricalIdentifier.parse("historical_experience:beta_1_7_3"),
            profile.id());
        assertEquals(HistoricalIdentifier.parse("historical_experience:beta_1_7_3"),
            profile.generationPreset());
        assertEquals(HistoricalExperience.GenerationPresets.HISTORICAL_BETA_1_7_3,
            profile.generationPreset().value());
    }

    @Test
    @DisplayName("the profile display name does not claim implemented era gameplay")
    void displayNameDoesNotOverclaimGameplay() {
        HistoricalProfile profile = HistoricalProfiles.beta173();

        // The name must name the version and must NOT claim the era is complete: several simulation
        // rules exist, but most subsystems do not, so a name that reads as finished would overclaim.
        String name = profile.displayName().toLowerCase();
        assertTrue(name.contains("beta 1.7.3"),
            "the display name must name the version, was: " + profile.displayName());
        for (String overclaim : java.util.List.of("complete", "full", "accurate", "faithful")) {
            assertFalse(name.contains(overclaim),
                "the display name must not claim " + overclaim + ": " + profile.displayName());
        }
    }

    @Test
    @DisplayName("lookup by identifier and by raw string agree")
    void lookupAgreesBetweenForms() {
        assertEquals(HistoricalProfiles.beta173(),
            HistoricalProfiles.byId("historical_experience:beta_1_7_3").orElseThrow());
        assertEquals(HistoricalProfiles.beta173(),
            HistoricalProfiles.byId(
                HistoricalIdentifier.parse("historical_experience:beta_1_7_3")).orElseThrow());
    }

    @Test
    @DisplayName("unknown and malformed profile identifiers resolve to empty")
    void unknownProfilesResolveEmpty() {
        assertTrue(HistoricalProfiles.byId("historical_experience:nope").isEmpty());
        assertTrue(HistoricalProfiles.byId("not an identifier").isEmpty());
        assertTrue(HistoricalProfiles.byId("").isEmpty());
    }

    @Test
    @DisplayName("profiles are immutable and non-empty")
    void tableIsImmutable() {
        // One canonical profile per declared version; the count is derived, not pinned, so adding a
        // version does not require editing this test.
        assertEquals(net.historicalexperience.version.HistoricalVersions.all().size(),
            HistoricalProfiles.all().size());
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class,
            () -> HistoricalProfiles.all().clear());
    }
}
