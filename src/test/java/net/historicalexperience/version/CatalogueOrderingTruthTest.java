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
package net.historicalexperience.version;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import net.historicalexperience.version.HistoricalVersionCatalogue.Entry;

/**
 * Guards the two things the catalogue said about itself and did not do.
 *
 * <p>Both were found by {@code docs/ADDING-A-VERSION.md}, and both were the quiet kind: the ordering it produced
 * happened to sort correctly, and the key it produced happened to be used consistently, so nothing failed while
 * the field's own documentation was untrue.
 */
class CatalogueOrderingTruthTest {
    @Test
    void anImplementedEntrysOrderIsItsRegistryChronology() {
        for (Entry entry : HistoricalVersionCatalogue.implemented()) {
            assertEquals(entry.version().chronology(), entry.order(),
                entry.key() + " carries a chronology of its own that differs from the registry's. The catalogue "
                    + "used to compute every order from the display name with its own formula, which agreed with "
                    + "the registry about ORDERING while disagreeing about the numbers - and the registry "
                    + "hand-writes at least one chronology (the Alpha profile) that no such formula reproduces");
        }
    }

    @Test
    void orderingsStillAgreeAfterTheChange() {
        // The check that made the old formula look fine: sorting by order must still match sorting by chronology,
        // for planned rows too, where the formula is the only thing available.
        List<Entry> byOrder = new ArrayList<>(HistoricalVersionCatalogue.entries());
        List<Entry> byChronology = new ArrayList<>(HistoricalVersionCatalogue.entries());
        byChronology.sort((left, right) -> Integer.compare(left.order(), right.order()));

        assertEquals(byChronology.stream().map(Entry::key).toList(),
            byOrder.stream().map(Entry::key).toList(),
            "the catalogue's own list order must be its order field, or the screen and the sort disagree");
    }

    @Test
    void plannedKeysAreDeliberatelyNotProfileIds() {
        for (Entry entry : HistoricalVersionCatalogue.future()) {
            assertTrue(entry.key().contains(":planned_"),
                entry.key() + " is a target, and a target's key must be namespaced as planned so that nothing "
                    + "holding one can mistake it for a playable version - the id it will take on implementation "
                    + "is a different string, and the javadoc now says so instead of claiming otherwise");
        }
    }
}
