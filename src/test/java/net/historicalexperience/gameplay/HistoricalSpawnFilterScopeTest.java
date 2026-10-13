package net.historicalexperience.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import net.historicalexperience.generation.HistoricalFeatureExclusions;
import net.historicalexperience.generation.HistoricalGenerationScope;
import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The live spawn filter resolves the world through the <b>active</b> historical scope, not the generator's own
 * marker.
 *
 * <p>Reading the generator alone answered null for the Nether and the End of a historical world, whose level
 * stems are ordinary vanilla generators: no spawn rule ran there, so a Beta 1.7.3 Nether kept 1.21.1's
 * {@code nether_wastes} list. {@code HistoricalFeatureExclusions.activeAlias} reads the generator first and
 * falls back to the running server's scope only when it carries no marker.
 *
 * <p>Level: unit. A real {@code ChunkGenerator} cannot be built without a registry bootstrap, so the
 * resolution is asserted where it can be - the scope itself, the precedence, and the alias-to-profile step -
 * plus one source-level check that the filter still calls {@code activeAlias}, because the defect was a
 * resolution that compiled and answered null on real hardware. The runtime {@code mobsAt} probe covers the
 * rest.
 */
class HistoricalSpawnFilterScopeTest {
    private static final String BETA_1_7_3 = "historical_experience:beta_1_7_3";
    private static final Path FILTER_SOURCE = Path.of("").toAbsolutePath().resolve(
        "src/main/java/net/historicalexperience/gameplay/HistoricalSpawnFilter.java");

    /** The scope is static server-wide state; a leaked alias would make the next test lie. */
    @AfterEach
    void clearScope() {
        HistoricalGenerationScope.clear();
    }

    @Test
    @DisplayName("with no scope, nothing resolves and vanilla lists are left alone")
    void ordinaryWorldResolvesToNoProfile() {
        HistoricalGenerationScope.clear();
        assertNull(HistoricalGenerationScope.generationAlias());
        // The composition profileIdForGenerator performs, with a generator carrying no marker of its own.
        assertNull(HistoricalFeatureExclusions.activeAlias(null));
        assertNull(HistoricalSpawnRules.profileIdForAlias(null));
        assertNull(HistoricalSpawnFilter.profileIdForGenerator(null));
    }

    @Test
    @DisplayName("a historical server scope resolves to a profile that actually restricts")
    void historicalScopeFallbackResolvesToARestrictingProfile() {
        HistoricalGenerationScope.set(BETA_1_7_3);

        String alias = HistoricalFeatureExclusions.activeAlias(null);
        assertEquals(BETA_1_7_3, alias,
            "the running server's scope must be the fallback when the generator carries no marker");

        String profileId = HistoricalSpawnRules.profileIdForAlias(alias);
        assertEquals(BETA_1_7_3, profileId,
            "the scope alias must resolve to the same gameplay profile the server lifecycle uses");
        assertEquals(BETA_1_7_3, HistoricalSpawnFilter.profileIdForGenerator(null),
            "the live resolver must use the owning world scope when a dimension has no marker");
        assertTrue(HistoricalSpawnRules.forProfile(profileId).isPresent(),
            "a profile that resolves but has no rule is the defect: the hook would run and remove nothing, "
                + "which looks exactly like a correct world");
    }

    @Test
    @DisplayName("the scope clears, and an unknown scope selects no rule")
    void scopeClearsAndUnknownScopesSelectNoRule() {
        HistoricalGenerationScope.set(BETA_1_7_3);
        assertNotNull(HistoricalFeatureExclusions.activeAlias(null));

        HistoricalGenerationScope.clear();
        assertNull(HistoricalGenerationScope.generationAlias(),
            "a stopped server must forget the alias, or the next world in the same process inherits it");

        HistoricalGenerationScope.set("   ");
        assertNull(HistoricalGenerationScope.generationAlias(), "a blank alias is not a scope");

        // An unrecognised scope reports itself but resolves to no rule, so it cannot make an ordinary world
        // obey a historical filter.
        for (String unknown : new String[] {
                "moderner_beta:beta", "other_mod:beta_1_7_3",
                "historical_experience:beta_1_7_3_content_preview_99"}) {
            HistoricalGenerationScope.set(unknown);
            assertEquals(unknown, HistoricalFeatureExclusions.activeAlias(null));
            assertNull(HistoricalSpawnRules.profileIdForAlias(unknown),
                unknown + " is not a declared historical recipe, so it must select no spawn rule");
        }
    }

    @Test
    @DisplayName("every revision alias reaches a restricting profile, so the fallback is never a no-op")
    void everyRevisionAliasCarriesARule() {
        // The scope records a revision alias, so this is the exact value set the fallback can receive.
        for (HistoricalVersion version : HistoricalVersions.all()) {
            for (String alias : version.revisionAliases()) {
                String profileId = HistoricalSpawnRules.profileIdForAlias(alias);
                assertEquals(version.id(), profileId, alias + " must resolve to its own version's profile");
                assertTrue(HistoricalSpawnRules.forProfile(profileId).isPresent(),
                    alias + " resolves to a profile with no spawn rule, so the fallback would be inert");
            }
        }
    }

    @Test
    @DisplayName("the filter resolves through activeAlias, not the marker-only lookup")
    void theFilterUsesTheScopeAwareResolution() throws Exception {
        // Source-level on purpose: no data-only test can see a resolution that answers null for the vanilla
        // Nether generator. This is the cheap structural guard; the mobsAt probe is the behavioural one.
        String source = Files.readString(FILTER_SOURCE);
        assertTrue(source.contains("\n            .activeAlias(generator)"),
            "profileIdForGenerator must resolve through activeAlias, or a historical Nether is unfiltered");
        assertFalse(source.contains(".historicalAlias(generator)"),
            "the marker-only lookup must not come back: it answers null for the vanilla Nether and End");
    }
}
