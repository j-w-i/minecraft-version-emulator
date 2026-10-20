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

import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Level: unit.
 *
 * <p>Regression for a defect found by the architecture audit: {@code HistoricalVersionSync} sets the active session
 * to {@code null} for an ordinary world, and the portal and food-stack mixins <em>cancel</em> when their gate answers
 * false. {@code netherAvailable(null)} and {@code foodStacks(null)} answered false, so an ordinary world could neither
 * light a portal nor stack food. These predicates are the single input of every one of those hooks (two portal mixins,
 * and the container, menu, inventory and slot food-stack mixins through {@link HistoricalFoodStackLimit}).
 */
class OrdinaryWorldPortalAndFoodTest {
    private static final String[] ORDINARY = {null, "", "moderner_beta:beta", "example:not_a_version"};

    @AfterEach
    void clearSession() {
        HistoricalActiveSession.clear();
    }

    @Test
    @DisplayName("an ordinary world keeps the modern Nether and stackable food")
    void ordinaryWorldsKeepTheModernAnswers() {
        for (String ordinary : ORDINARY) {
            assertTrue(HistoricalGameplayTweaks.netherAvailable(ordinary), "Nether for " + ordinary);
            assertTrue(HistoricalGameplayTweaks.foodStacks(ordinary), "food stacking for " + ordinary);
        }
    }

    @Test
    @DisplayName("the session a mixin actually reads gives the same answer, including after leaving a historical world")
    void sessionPathMatches() {
        // No session at all: a freshly started game or an ordinary world.
        HistoricalActiveSession.clear();
        assertTrue(HistoricalGameplayTweaks.netherAvailable(HistoricalActiveSession.current()));
        assertTrue(HistoricalGameplayTweaks.foodStacks(HistoricalActiveSession.current()));

        // Historical world, then back to ordinary (disconnect and rejoin): nothing may leak.
        HistoricalActiveSession.set("historical_experience:alpha_1_0_1");
        assertFalse(HistoricalGameplayTweaks.netherAvailable(HistoricalActiveSession.current()));
        assertFalse(HistoricalGameplayTweaks.foodStacks(HistoricalActiveSession.current()));
        HistoricalActiveSession.set(null);
        assertTrue(HistoricalGameplayTweaks.netherAvailable(HistoricalActiveSession.current()));
        assertTrue(HistoricalGameplayTweaks.foodStacks(HistoricalActiveSession.current()));
    }

    @Test
    @DisplayName("every declared version still answers exactly as before")
    void declaredVersionsAreUnchanged() {
        for (HistoricalVersion version : HistoricalVersions.all()) {
            assertEquals(version.chronology() >= HistoricalGameplayTweaks.netherBoundaryChronology(),
                HistoricalGameplayTweaks.netherAvailable(version.id()), "Nether on " + version.id());
            assertEquals(version.chronology() >= HistoricalGameplayTweaks.preBeta18BoundaryChronology(),
                HistoricalGameplayTweaks.foodStacks(version.id()), "food stacking on " + version.id());
        }
    }
}
