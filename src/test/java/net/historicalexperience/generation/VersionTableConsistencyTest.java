package net.historicalexperience.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;

/**
 * Guards the two per-version tables that are still written out by hand.
 *
 * <p>{@code docs/ADDING-A-VERSION.md} found that the registry is not the single declaration point it claims to be.
 * Two tables remain hand-enumerated, and both fail in ways that produce no error:
 *
 * <ul>
 *   <li><b>{@link HistoricalBlockAvailability#CHRONOLOGY} is an ORDERED list, and comparison is
 *       {@code indexOf}.</b> That makes its position of a version meaningful: inserting a newly declared version
 *       in the wrong place does not merely leave it out, it silently reverses every block-availability comparison
 *       that involves it. A version in the wrong slot generates blocks from the wrong era, and no test, log or
 *       metadata would show it.</li>
 *   <li><b>{@link HistoricalStructureRules} keys its rules by version id.</b> A misspelt or stale key is dead
 *       data, and a version whose registry rules say it HAS structures but which has no entry would fall back to
 *       whatever the lookup does for an unknown version rather than to its own researched allow-list.</li>
 * </ul>
 */
class VersionTableConsistencyTest {
    @Test
    void theBlockAvailabilityOrderMatchesTheRegistrarsChronology() {
        // Sorted by the registry's chronology, NOT taken in declaration order: HistoricalVersions.all() lists the
        // versions in the order they were written, which is not historical order. Comparing against that would
        // have demanded the hand-written table be wrong.
        List<String> declared = HistoricalVersions.all().stream()
            .sorted(java.util.Comparator.comparingInt(HistoricalVersion::chronology))
            .map(HistoricalVersion::id)
            .toList();

        // The hand-written list may legitimately contain versions this build does not declare - withdrawn
        // profiles, or ones kept for future work - so the guard compares only the declared ones, and compares
        // their ORDER, which is what indexOf turns into behaviour.
        List<String> declaredInHandWrittenOrder = HistoricalBlockAvailability.CHRONOLOGY.stream()
            .filter(declared::contains)
            .toList();

        assertEquals(declared, declaredInHandWrittenOrder,
            "HistoricalBlockAvailability.CHRONOLOGY lists the declared versions in a different order from the "
                + "registry's chronology. Its indexOf is the comparison, so a version in the wrong slot silently "
                + "reverses every block-availability answer involving it - and a new version inserted in the wrong "
                + "place does the same");
    }

    @Test
    void everyDeclaredVersionIsCoveredByTheBlockAvailabilityChronology() {
        List<String> missing = new ArrayList<>();

        for (HistoricalVersion version : HistoricalVersions.all()) {
            if (!HistoricalBlockAvailability.CHRONOLOGY.contains(version.id())) {
                missing.add(version.id());
            }
        }

        assertTrue(missing.isEmpty(),
            "these versions are absent from HistoricalBlockAvailability.CHRONOLOGY, so None of their blocks can "
                + "be dated and every anachronism check quietly passes: " + missing);
    }

    @Test
    void structureRuleKeysAreAllDeclaredVersions() {
        Set<String> declared = HistoricalVersions.all().stream()
            .map(HistoricalVersion::id)
            .collect(Collectors.toSet());

        List<String> stale = HistoricalStructureRules.declaredVersions().stream()
            .filter(key -> !declared.contains(key))
            .toList();

        assertTrue(stale.isEmpty(),
            "HistoricalStructureRules names versions this build does not declare, which is dead data that would "
                + "resurface if an id were reused: " + stale);
    }

    @Test
    void versionsWhoseRulesSayTheyHaveStructuresHaveStructureRules() {
        List<String> missing = new ArrayList<>();

        for (HistoricalVersion version : HistoricalVersions.all()) {
            if (!version.rules().hasStructures()) {
                continue;
            }
            if (!HistoricalStructureRules.declaredVersions().contains(version.id())) {
                missing.add(version.id());
            }
        }

        assertTrue(missing.isEmpty(),
            "these versions declare that they HAVE structures but name no structure rules, so which structures "
                + "may generate is left to the unknown-key fallback rather than to their own researched "
                + "allow-list: " + missing);
    }
}
