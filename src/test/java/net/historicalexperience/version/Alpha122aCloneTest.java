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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Alpha v1.2.2a, the owner's requested clone of Alpha v1.2.0 with the F4 portal key added.
 *
 * <p>The point of these assertions is that "clone" is checked against the version it copied rather than against a
 * second copy of the data: if 1.2.0's rules change and 1.2.2a stops matching, that is a finding, not a nuisance.
 */
class Alpha122aCloneTest {
    private static HistoricalVersion v(String id) {
        return HistoricalVersions.byId("historical_experience:" + id)
            .orElseThrow(() -> new AssertionError("no such version: " + id));
    }

    @Test
    @DisplayName("the build is declared, and sorts immediately after Alpha 1.2.0")
    void declaredAndOrdered() {
        HistoricalVersion a122a = v("alpha_1_2_2a");
        assertEquals("Alpha v1.2.2a", a122a.displayName());
        assertEquals("alpha", a122a.era());
        assertTrue(a122a.chronology() > v("alpha_1_2_0").chronology(),
            "1.2.2a is a later build than 1.2.0");
        assertTrue(a122a.chronology() < v("beta_1_0").chronology(),
            "and still an Alpha build: nothing may sort between it and Beta 1.0");
        assertNotNull(a122a.latestRevision());
        assertTrue(a122a.revisions().size() >= 2, "it carries 1.2.0's revisions");
    }

    @Test
    @DisplayName("it IS 1.2.0's clone: the same rules and the same generation, under a new id")
    void isACloneOf120() {
        HistoricalVersion source = v("alpha_1_2_0");
        HistoricalVersion clone = v("alpha_1_2_2a");

        assertEquals(source.rules(), clone.rules(),
            "the rules are taken from the 1.2.0 version object, so a divergence means one of them was edited alone");
        assertEquals(source.revisions().size(), clone.revisions().size(),
            "same number of generation revisions");

        for (int i = 0; i < source.revisions().size(); i++) {
            var from = source.revisions().get(i);
            var to = clone.revisions().get(i);
            assertEquals(from.revision(), to.revision(), "revision number " + i);
            assertEquals(from.foundation(), to.foundation(), "foundation of revision " + i);
            assertEquals(from.experimental(), to.experimental(), "experimental flag of revision " + i);
            // The aliases are the ONLY thing that may differ, and only by their version id, because a preset file is
            // named after the alias and each version needs its own.
            assertEquals(from.alias().replace("alpha_1_2_0", "alpha_1_2_2a"), to.alias(),
                "alias of revision " + i + " must differ from 1.2.0's only by the version id");
        }
    }

    @Test
    @DisplayName("every alias of the clone is registered in the registry's own lookup")
    void aliasesResolve() {
        for (HistoricalVersion.GenerationRevision revision : v("alpha_1_2_2a").revisions()) {
            assertTrue(HistoricalVersions.byRevisionAlias(revision.alias()).isPresent(),
                revision.alias() + " must resolve, because a world persists it");
        }
    }
}