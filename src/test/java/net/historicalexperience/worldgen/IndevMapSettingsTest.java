/* SPDX-License-Identifier: LGPL-3.0-or-later */
package net.historicalexperience.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Holds the Indev option set to the researched table, so a typo in a dimension or a silently mapped "Flat" fails
 * here rather than in a generated world.
 */
class IndevMapSettingsTest {
    @Test
    void theOriginalOptionListsAreCompleteAndInOrder() {
        assertEquals(List.of("Island", "Floating", "Flat", "Inland"),
            List.of(IndevMapSettings.LevelType.values()).stream().map(IndevMapSettings.LevelType::displayName).toList(),
            "the original level types, with Original already renamed to Inland by 20100223");
        assertEquals(List.of("Normal", "Hell", "Paradise", "Woods"),
            List.of(IndevMapSettings.Theme.values()).stream().map(IndevMapSettings.Theme::displayName).toList());
        assertEquals(List.of("Square", "Long", "Deep"),
            List.of(IndevMapSettings.Shape.values()).stream().map(IndevMapSettings.Shape::displayName).toList());
        assertEquals(List.of("Small", "Normal", "Huge"),
            List.of(IndevMapSettings.Size.values()).stream().map(IndevMapSettings.Size::displayName).toList());
    }

    @Test
    void upstreamIdsAreTheAvailableOnesAndFlatHasNone() {
        assertEquals("island", IndevMapSettings.LevelType.ISLAND.id());
        assertEquals("floating", IndevMapSettings.LevelType.FLOATING.id());
        assertEquals("inland", IndevMapSettings.LevelType.INLAND.id());
        assertNull(IndevMapSettings.LevelType.FLAT.id(), "no upstream id may be invented for Flat");
        assertTrue(IndevMapSettings.available(IndevMapSettings.LevelType.FLAT).contains("no flat Indev type"));
        for (var available : List.of(IndevMapSettings.LevelType.ISLAND, IndevMapSettings.LevelType.FLOATING,
                IndevMapSettings.LevelType.INLAND)) {
            assertEquals("", IndevMapSettings.available(available), available + " is implemented upstream");
        }
        assertEquals(List.of("normal", "hell", "paradise", "woods"),
            List.of(IndevMapSettings.Theme.values()).stream().map(IndevMapSettings.Theme::id).toList());
    }

    @Test
    void theFootprintComesFromTheTableAndTheHeightFromTheProvider() {
        // Width and length are the published footprint, corroborated by the provider's own preset (Normal square is
        // 256x256). Height is the provider's 128 for every cell, NOT the table's 64/256: following the table's height
        // generated half-height Indev worlds, and after an upgrade their short terrain met the new infinite terrain as
        // a wall - the defect the owner reported.
        Map<IndevMapSettings.Size, Map<IndevMapSettings.Shape, int[]>> expected = Map.of(
            IndevMapSettings.Size.SMALL, Map.of(
                IndevMapSettings.Shape.SQUARE, new int[] {128, 128},
                IndevMapSettings.Shape.LONG, new int[] {256, 64},
                IndevMapSettings.Shape.DEEP, new int[] {64, 64}),
            IndevMapSettings.Size.NORMAL, Map.of(
                IndevMapSettings.Shape.SQUARE, new int[] {256, 256},
                IndevMapSettings.Shape.LONG, new int[] {512, 128},
                IndevMapSettings.Shape.DEEP, new int[] {128, 128}),
            IndevMapSettings.Size.HUGE, Map.of(
                IndevMapSettings.Shape.SQUARE, new int[] {512, 512},
                IndevMapSettings.Shape.LONG, new int[] {1024, 256},
                IndevMapSettings.Shape.DEEP, new int[] {256, 256}));
        for (IndevMapSettings.Size size : IndevMapSettings.Size.values()) {
            for (IndevMapSettings.Shape shape : IndevMapSettings.Shape.values()) {
                int[] want = expected.get(size).get(shape);
                int[] got = IndevMapSettings.dimensions(size, shape);
                assertEquals(want[0] + "x" + want[1] + "x" + IndevMapSettings.PROVIDER_HEIGHT,
                    got[0] + "x" + got[1] + "x" + got[2], size + " " + shape);
                assertEquals(IndevMapSettings.PROVIDER_HEIGHT, got[2], "every cell uses the provider's height");
            }
        }
        int[] copy = IndevMapSettings.dimensions(IndevMapSettings.Size.NORMAL, IndevMapSettings.Shape.SQUARE);
        copy[0] = 1;
        assertEquals(256, IndevMapSettings.dimensions(IndevMapSettings.Size.NORMAL, IndevMapSettings.Shape.SQUARE)[0],
            "a caller cannot edit the table through the returned array");
    }

    @Test
    void theDefaultIsTheWorldTheProjectAlreadyGenerates() {
        var choice = IndevMapSettings.DEFAULT;
        assertEquals(IndevMapSettings.LevelType.ISLAND, choice.type());
        assertEquals(IndevMapSettings.Theme.NORMAL, choice.theme());
        assertEquals(IndevMapSettings.Shape.SQUARE, choice.shape());
        assertEquals(IndevMapSettings.Size.NORMAL, choice.size());
        assertEquals("256x256", choice.width() + "x" + choice.length(),
            "the footprint agrees with the project's existing Indev preset");
        assertTrue(choice.describe().contains("Island, Normal, Square Normal"));
    }

    @Test
    void theSettingsComponentIsBuiltWithUpstreamsOwnKeys() {
        var properties = IndevMapSettings.finiteLevelProperties(
            new IndevMapSettings.Choice(IndevMapSettings.LevelType.FLOATING, IndevMapSettings.Theme.HELL,
                IndevMapSettings.Shape.DEEP, IndevMapSettings.Size.SMALL));
        assertEquals(Map.of("type", "floating", "theme", "hell", "width", 64, "length", 64,
            "height", IndevMapSettings.PROVIDER_HEIGHT), properties,
            "exactly upstream's five finite_level_properties keys, at the provider's own height");
        assertThrows(IllegalArgumentException.class, () -> IndevMapSettings.finiteLevelProperties(
            new IndevMapSettings.Choice(IndevMapSettings.LevelType.FLAT, IndevMapSettings.Theme.NORMAL,
                IndevMapSettings.Shape.SQUARE, IndevMapSettings.Size.NORMAL)),
            "an unavailable type cannot silently become a settings component");
        assertThrows(IllegalArgumentException.class, () -> new IndevMapSettings.Choice(
            null, IndevMapSettings.Theme.NORMAL, IndevMapSettings.Shape.SQUARE, IndevMapSettings.Size.NORMAL));
        assertFalse(properties.containsKey("classic"), "classic is not an original Indev type");
    }
}
