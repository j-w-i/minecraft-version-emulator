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
package net.historicalexperience.client.tooltip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The tooltip boundaries the owner gave: none before Beta 1.0, the legacy box from Beta 1.0, and the modern box from
 * Beta 1.9 Pre-release 2. Asserted across the real registry so a version cannot drift across a boundary silently.
 */
class HistoricalTooltipsTest {
    private static HistoricalVersion at(String id) {
        return HistoricalVersions.byId("historical_experience:" + id)
            .orElseThrow(() -> new AssertionError("no such version: " + id));
    }

    private static HistoricalTooltips.Era era(String id) {
        return HistoricalTooltips.eraFor(at(id).chronology());
    }

    @Test
    @DisplayName("no tooltips before Beta 1.0, on every earlier declared profile")
    void absentBeforeBetaOne() {
        for (String id : new String[] {"infdev_420", "infdev_611", "alpha_1_0_1", "alpha_1_0_17", "alpha_1_1_0",
            "alpha_1_2_0"}) {
            assertEquals(HistoricalTooltips.Era.ABSENT, era(id), id);
        }
    }

    @Test
    @DisplayName("the legacy tooltip box from Beta 1.0 through Beta 1.9 Prerelease 1")
    void legacyFromBetaOne() {
        for (String id : new String[] {"beta_1_0", "beta_1_2", "beta_1_3", "beta_1_4", "beta_1_5", "beta_1_6",
            "beta_1_7_3", "beta_1_8_1", "beta_1_9_pre_1"}) {
            assertEquals(HistoricalTooltips.Era.LEGACY, era(id), id);
        }
    }

    @Test
    @DisplayName("the modern tooltip from Beta 1.9 Prerelease 2 onwards, including every release")
    void modernFromBeta19Pre2() {
        for (String id : new String[] {"beta_1_9_pre_2", "beta_1_9_pre_3", "beta_1_9_pre_4", "beta_1_9_pre_5",
            "beta_1_9_pre_6", "release_1_0_0", "release_1_1", "release_1_2_5", "release_1_3", "release_1_4",
            "release_1_5", "release_1_6_4"}) {
            assertEquals(HistoricalTooltips.Era.MODERN, era(id), id);
        }
        // The two boundaries are exactly where the owner put them, one chronology step either side.
        assertTrue(HistoricalTooltips.eraFor(at("beta_1_0").chronology() - 1) == HistoricalTooltips.Era.ABSENT,
            "one step before Beta 1.0 is still absent");
        assertTrue(HistoricalTooltips.eraFor(at("beta_1_9_pre_2").chronology() - 1) == HistoricalTooltips.Era.LEGACY,
            "one step before Beta 1.9 Pre-release 2 is still legacy");
    }

    @Test
    @DisplayName("nothing is hidden or restyled in an ordinary world, and the box colour is NT's own default")
    void ordinaryWorldUntouched() {
        // No historical version active means era() answers MODERN, so neither hook substitutes anything. With no game
        // running at all there is no world and therefore no active version, which is the ordinary case.
        assertEquals(HistoricalTooltips.Era.MODERN, HistoricalTooltips.era(),
            "an unknown version must mean 'draw the game's own tooltip'");
        assertFalse(HistoricalTooltips.hidden(), "an ordinary world must never hide tooltips");
        assertFalse(HistoricalTooltips.legacyBox(), "an ordinary world must never restyle tooltips");
        // #000000C0, the value Nostalgic Tweaks' TOOLTIP_BACKGROUND_COLOR defaults to.
        assertEquals(0xC0000000, HistoricalTooltips.LEGACY_BACKGROUND);
        assertEquals(0xC0, (HistoricalTooltips.LEGACY_BACKGROUND >>> 24) & 0xFF, "75% alpha");
        assertEquals(0x000000, HistoricalTooltips.LEGACY_BACKGROUND & 0xFFFFFF, "pure black");
    }

    @Test
    @DisplayName("every declared version maps to exactly one era, so no profile falls through the gaps")
    void everyVersionHasAnEra() {
        for (HistoricalVersion version : HistoricalVersions.all()) {
            HistoricalTooltips.Era era = HistoricalTooltips.eraFor(version.chronology());
            assertTrue(era != null, version.id());
            boolean beforeBeta = version.chronology()
                < HistoricalVersions.chronology(HistoricalVersions.ERA_BETA, 1, 0, 0);
            if (beforeBeta) {
                assertEquals(HistoricalTooltips.Era.ABSENT, era, version.id());
            }
        }
    }

    @Test
    @DisplayName("the attribute block is suppressed up to Release 1.5 and allowed from Release 1.6.1")
    void attributeBlockBoundary() {
        // Where the numbers started, verified against the clients themselves rather than remembered:
        // generic.attackDamage, attribute.modifier.plus.0 and the attribute.name.* family are in no class or lang file
        // of Beta 1.7.3 or Release 1.5, and are present in Release 1.6.1 and 1.6.4. The attribute system shipped in
        // 1.6.1, and with it the tooltip began printing what an item in hand was worth.
        for (HistoricalVersion version : HistoricalVersions.all()) {
            boolean expected = version.chronology()
                >= HistoricalVersions.chronology(HistoricalVersions.ERA_RELEASE, 1, 6, 1);
            assertEquals(expected, HistoricalTooltips.showsAttributeTooltip(version),
                version.id() + ": attribute block presence must follow Release 1.6.1 exactly");
        }
    }

    @Test
    @DisplayName("the boundary is exactly Release 1.6.1: 1.5 and 1.6.4 sit either side of it")
    void attributeBoundaryIsExactlyRelease161() {
        assertFalse(HistoricalTooltips.showsAttributeTooltip(at("release_1_5")),
            "Release 1.5 has no attribute lines");
        assertFalse(HistoricalTooltips.showsAttributeTooltip(at("beta_1_9_pre_3")),
            "so a Beta 1.9 Prerelease 3 sword shows no Attack Damage line");
        assertTrue(HistoricalTooltips.showsAttributeTooltip(at("release_1_6_4")),
            "the 1.6.x profile is at or after 1.6.1, so it DOES show the block - it is the only declared profile "
                + "that does");
        // The header is later still and is not part of this boundary: item.modifiers.* is in no language file of
        // 1.6.4, 1.7.10 or 1.8.9, and first appears in 1.9.
        assertFalse(HistoricalVersions.chronology(HistoricalVersions.ERA_RELEASE, 1, 6, 4)
            >= HistoricalVersions.chronology(HistoricalVersions.ERA_RELEASE, 1, 9, 0),
            "1.8.9-style headers are past even the last declared profile");
    }

    @Test
    @DisplayName("an ordinary world keeps its attribute block")
    void ordinaryWorldKeepsAttributes() {
        assertTrue(HistoricalTooltips.showsAttributeTooltip((HistoricalVersion) null),
            "a world with no historical version must not have its tooltips altered");
    }
}
