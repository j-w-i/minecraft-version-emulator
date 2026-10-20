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
package net.historicalexperience.worldgen;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The original Indev map settings: level type, theme, and the shape/size pair that fixes the map's dimensions.
 *
 * <h2>Where the values come from</h2>
 *
 * <p>The option lists, the dimension table and the dates are recorded with their source in
 * {@code research/indev-map-settings.json}. In one line: the level type was Island/Floating/Flat/Original from
 * Indev 0.31 20100106, "Original" became "Inland" on 20100109, the themes Normal and Hell arrived on 20100107 and
 * Paradise and Woods on 20100214 - all of it before the project's Indev profile, 20100223, which is the last Indev
 * build.
 *
 * <h2>What this class is not</h2>
 *
 * <p>It does not generate terrain. It is the option set and the numbers, which is what a chooser needs and what a
 * test can hold to account. Terrain stays Moderner Beta's: these values are handed to its own
 * {@code moderner_beta:finite_level_properties} component.
 *
 * <h2>Flat, and the one number the sources disagree about</h2>
 *
 * <p>"Flat" was in the original list and upstream has no id for it - its Indev types are island, floating, inland
 * and classic, and "classic" is Classic-era terrain, not Indev's Flat. So Flat is listed here as part of the
 * original set and {@link #available(LevelType)} reports it as unavailable, with the reason, rather than being
 * quietly dropped or mapped onto a type that would generate something else.
 *
 * <p>The same table gives a height of 64 for every Square and Long map, while the pinned upstream Indev preset
 * ships 256x256x<b>128</b>. The width and length columns agree with that preset, so the footprint is corroborated
 * and the height is not. Nothing here decides that conflict: {@link #dimensions} reports the researched numbers,
 * the project's default world keeps upstream's own preset untouched, and the disagreement is recorded as an open
 * question rather than averaged away.
 */
public final class IndevMapSettings {
    private IndevMapSettings() {
    }

    /** The original level types, in the original screen's order. {@code flat} has no upstream id. */
    public enum LevelType {
        ISLAND("island", "Island"),
        FLOATING("floating", "Floating"),
        FLAT(null, "Flat"),
        INLAND("inland", "Inland");

        private final String id;
        private final String display;

        LevelType(String id, String display) {
            this.id = id;
            this.display = display;
        }

        /** The upstream {@code finite_level_properties.type} id, or null when upstream cannot express this type. */
        public String id() {
            return id;
        }

        public String displayName() {
            return display;
        }
    }

    /** The four original themes, whose ids are exactly upstream's. */
    public enum Theme {
        NORMAL("normal", "Normal"),
        HELL("hell", "Hell"),
        PARADISE("paradise", "Paradise"),
        WOODS("woods", "Woods");

        private final String id;
        private final String display;

        Theme(String id, String display) {
            this.id = id;
            this.display = display;
        }

        public String id() {
            return id;
        }

        public String displayName() {
            return display;
        }
    }

    /** The original map shapes. */
    public enum Shape {
        SQUARE("Square"), LONG("Long"), DEEP("Deep");

        private final String display;

        Shape(String display) {
            this.display = display;
        }

        public String displayName() {
            return display;
        }
    }

    /** The original map sizes. */
    public enum Size {
        SMALL("Small"), NORMAL("Normal"), HUGE("Huge");

        private final String display;

        Size(String display) {
            this.display = display;
        }

        public String displayName() {
            return display;
        }
    }

    /**
     * The vertical extent the pinned provider itself uses for Indev, whatever the shape and size.
     *
     * <p>This is 128 because that is what Moderner Beta's own Indev preset stores and what the level envelope actually
     * is - MEASURED, not taken from the published table: an untouched Indev world saves
     * {@code finite_level_properties.height = 128} and loads with {@code maxY = 128}.
     *
     * <p>The published table's height column (64 for every Square and Long map, 256 for Deep) is the one part of it
     * this project does NOT follow, and the reason is a real defect it caused: a world created through the Indev map
     * chooser was generated at height 64, half the provider's own terrain, and after an upgrade to an infinite
     * generator its short terrain no longer met the new terrain - the owner's screenshot of an Infdev world with a
     * wall of water hanging off the island. The table's width and length columns ARE corroborated by that same preset
     * (Normal square is 256x256, exactly what the table says), so the footprint is kept from the table and only the
     * height is taken from the provider.
     */
    public static final int PROVIDER_HEIGHT = 128;

    /**
     * The researched footprint for a size and shape, with the provider's own height.
     *
     * <p>Shape changes the footprint only. The published Deep shape claims four times the height (256) which is twice
     * this provider's envelope and would be meaningless to assert, so it is not claimed here.
     */
    private static final Map<Size, Map<Shape, int[]>> DIMENSIONS = buildDimensions();

    private static Map<Size, Map<Shape, int[]>> buildDimensions() {
        Map<Size, Map<Shape, int[]>> table = new LinkedHashMap<>();
        table.put(Size.SMALL, Map.of(
            Shape.SQUARE, new int[] {128, 128, PROVIDER_HEIGHT},
            Shape.LONG, new int[] {256, 64, PROVIDER_HEIGHT},
            Shape.DEEP, new int[] {64, 64, PROVIDER_HEIGHT}));
        table.put(Size.NORMAL, Map.of(
            Shape.SQUARE, new int[] {256, 256, PROVIDER_HEIGHT},
            Shape.LONG, new int[] {512, 128, PROVIDER_HEIGHT},
            Shape.DEEP, new int[] {128, 128, PROVIDER_HEIGHT}));
        table.put(Size.HUGE, Map.of(
            Shape.SQUARE, new int[] {512, 512, PROVIDER_HEIGHT},
            Shape.LONG, new int[] {1024, 256, PROVIDER_HEIGHT},
            Shape.DEEP, new int[] {256, 256, PROVIDER_HEIGHT}));
        return Map.copyOf(table);
    }

    /** The researched width/length/height for a size and shape. */
    public static int[] dimensions(Size size, Shape shape) {
        int[] found = DIMENSIONS.get(size).get(shape);
        return new int[] {found[0], found[1], found[2]};
    }

    /**
     * One short sentence for a chooser about the type upstream cannot generate, kept beside the long reason so the
     * two cannot drift apart.
     */
    public static final String FLAT_NOTE =
        "Flat is in the original list and cannot be chosen: the pinned provider has island, floating, inland and "
            + "classic Indev types, and classic is Classic-era terrain rather than Indev's flat map.";

    /** Whether the pinned upstream provider can generate this level type, and why not when it cannot. */
    public static String available(LevelType type) {
        if (type.id() != null) {
            return "";
        }
        return "Flat was in the original list, but " + "the pinned Moderner Beta has no flat Indev type - its types "
            + "are island, floating, inland and classic, and classic is Classic-era terrain rather than Indev's flat "
            + "map. It is shown so the original list is complete, and cannot be chosen.";
    }

    /** One complete choice, which is what a chooser produces and a world is created with. */
    public record Choice(LevelType type, Theme theme, Shape shape, Size size) {
        public Choice {
            if (type == null || theme == null || shape == null || size == null) {
                throw new IllegalArgumentException("an Indev map choice needs a type, a theme, a shape and a size");
            }
        }

        /** Width, length and height for this choice. */
        public int[] dimensions() {
            return IndevMapSettings.dimensions(this.size, this.shape);
        }

        public int width() {
            return dimensions()[0];
        }

        public int length() {
            return dimensions()[1];
        }

        public int height() {
            return dimensions()[2];
        }

        /** One line for a button or a details panel, e.g. {@code Island, Normal, Square Normal 256x256x64}. */
        public String describe() {
            int[] size = dimensions();
            return this.type.displayName() + ", " + this.theme.displayName() + ", "
                + this.shape.displayName() + " " + this.size.displayName() + " "
                + size[0] + "x" + size[1] + "x" + size[2];
        }
    }

    /**
     * The choice the project already generates with: Island, Normal, Normal square - upstream's own Indev preset.
     *
     * <p>Its dimensions here read 256x256x64 from the researched table while the preset itself ships height 128.
     * That is the recorded conflict, not an oversight: a world created without touching the chooser keeps upstream's
     * preset exactly as before, and this default is the option names rather than a promise about the height.
     */
    public static final Choice DEFAULT = new Choice(LevelType.ISLAND, Theme.NORMAL, Shape.SQUARE, Size.NORMAL);

    /**
     * The {@code moderner_beta:finite_level_properties} component for a choice.
     *
     * <p>Returned as data - the same shape the settings JSON uses - so the caller decides where it is written, and a
     * test can compare it without a running game. Nothing here reads or writes a file.
     */
    public static Map<String, Object> finiteLevelProperties(Choice choice) {
        String reason = available(choice.type());
        if (!reason.isEmpty()) {
            throw new IllegalArgumentException("cannot build finite level properties: " + reason);
        }
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("type", choice.type().id());
        properties.put("theme", choice.theme().id());
        properties.put("width", choice.width());
        properties.put("length", choice.length());
        properties.put("height", choice.height());
        return properties;
    }
}
