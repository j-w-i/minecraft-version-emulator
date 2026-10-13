package net.historicalexperience.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;

/**
 * Guards the offhand boundary: no supported version has one.
 *
 * <p>Reported by the owner: "the offhand is available in all versions currently and should not be available at all,
 * not in the gui or via a hotkey". Dual wielding arrives in Release 1.9, and the newest version here is Release
 * 1.6.4, so the answer is the same for every version this project declares.
 *
 * <p>The tests assert that in both directions, because "no version has it" is only half the claim:
 *
 * <ul>
 *   <li>every declared version must refuse it;</li>
 *   <li>an unresolved version must NOT refuse it, or an ordinary world - one this mod is not managing - would lose
 *       an offhand it is entitled to.</li>
 * </ul>
 *
 * <p>The second matters more than it looks. A null session is what a world with no historical metadata produces,
 * and this project's rule is that ordinary worlds are left alone.
 */
class OffhandBoundaryTest {
    private static final int ERA_RELEASE = 600_000;

    @Test
    void everyDeclaredVersionRefusesTheOffhand() {
        for (HistoricalVersion version : HistoricalVersions.all()) {
            assertTrue(HistoricalGameplayTweaks.offhandUnavailable(version.id()),
                version.id() + " (" + version.displayName() + ") predates Release 1.9, so it must have no offhand");
        }
    }

    @Test
    void anOrdinaryWorldKeepsItsOffhand() {
        assertFalse(HistoricalGameplayTweaks.offhandUnavailable(null),
            "a world with no historical metadata is an ordinary world, and removing its offhand would break "
                + "vanilla play rather than emulate a version");
        assertFalse(HistoricalGameplayTweaks.offhandUnavailable("historical_experience:no_such_version"),
            "an unresolvable version is not a claim about the offhand");
    }

    @Test
    void theBoundaryLiteralFollowsTheRegistrarsOwnNumbering() {
        assertEquals(HistoricalVersions.chronology(ERA_RELEASE, 1, 9, 0),
            HistoricalGameplayTweaks.offhandBoundaryChronology(),
            "the offhand arrives in Release 1.9, which is outside the declared matrix, so the boundary is a literal "
                + "tied to the registry's scheme. This asserts it cannot drift into gating the wrong release");

        assertTrue(HistoricalGameplayTweaks.offhandBoundaryChronology()
                > HistoricalVersions.byId("historical_experience:release_1_6_4").orElseThrow().chronology(),
            "Release 1.6.4, the newest version supported, must sit below the offhand boundary - which is why every "
                + "declared version refuses it");
    }
}
