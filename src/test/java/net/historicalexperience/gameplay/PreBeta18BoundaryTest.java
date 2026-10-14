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
package net.historicalexperience.gameplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.historicalexperience.version.HistoricalVersions;

/**
 * Guards the pre-Beta-1.8 boundary, which seven gates read and which a hand-written list kept getting wrong.
 *
 * <p>{@code BEFORE_BETA_1_8} used to be four ids written out by hand. Adding any pre-1.8 version would have left it
 * out of the set, and the gates that read it - sword blocking, its fall-damage window, the critical-hit formula,
 * exhaustion costs, the food hunger values, the regeneration model and the golden apple - would have applied Beta
 * 1.8 behaviour to an earlier build. Nothing would have failed: the version would simply behave as the wrong era.
 *
 * <p>It is now derived from the registry, so the set cannot disagree with the versions that exist. These tests hold
 * that down from the outside, through the gates rather than the private field, and tie the boundary literal to the
 * registry's numbering so it cannot drift either.
 */
class PreBeta18BoundaryTest {
    @Test
    void betaOnePointEightItselfIsNotBeforeBetaOnePointEight() {
        assertFalse(HistoricalGameplayTweaks.preBeta18("historical_experience:beta_1_8_1"),
            "Beta 1.8.1 is the first version with hunger, so it must NOT be treated as pre-1.8");
        assertTrue(HistoricalGameplayTweaks.preBeta18("historical_experience:beta_1_7_3"),
            "Beta 1.7.3 is before Beta 1.8");

        // And the gate that reads the boundary must say the opposite of it: the pre-Release-1.1 golden apple
        // applies FROM Beta 1.8.1, which is exactly the version the boundary excludes. Getting this pair backwards
        // was the first version of this test's mistake, so it is asserted rather than assumed.
        assertTrue(HistoricalGameplayTweaks.betaGoldenApple("historical_experience:beta_1_8_1"),
            "Beta 1.8.1 has the 10-hunger regenerating golden apple");
        assertFalse(HistoricalGameplayTweaks.betaGoldenApple("historical_experience:beta_1_7_3"),
            "Beta 1.7.3 has no hunger meter, so the hunger-based golden apple gate does not apply at all");
    }

    @Test
    void everyDeclaredVersionIsOnTheCorrectSideOfTheBoundary() {
        int boundary = HistoricalGameplayTweaks.preBeta18BoundaryChronology();

        for (var version : HistoricalVersions.all()) {
            boolean expected = version.chronology() < boundary;

            assertEquals(expected, HistoricalGameplayTweaks.preBeta18(version.id()),
                version.id() + " (chronology " + version.chronology() + ") disagrees with the registry about which "
                    + "side of the Beta 1.8 boundary it is on. The set is derived from the registry, so this fails "
                    + "only if the derivation itself is wrong - which is the point of testing it directly rather "
                    + "than through seven gates");
        }
    }

    @Test
    void theBoundaryLiteralSitsOneBuildBeforeBetaOnePointEightPointOne() {
        int beta181 = HistoricalVersions.byId("historical_experience:beta_1_8_1").orElseThrow().chronology();

        assertEquals(beta181 - 1, HistoricalGameplayTweaks.preBeta18BoundaryChronology(),
            "the boundary is a literal because Beta 1.8 itself is not a declared profile. This ties it to the "
                + "registry's numbering so a drift in era offset, major, minor or patch fails here rather than "
                + "silently moving seven gates to the wrong era");
    }
}
