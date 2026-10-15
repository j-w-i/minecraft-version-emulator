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
package net.historicalexperience.world.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The biome palette rewrite, tested on hand-built chunk tags rather than on a world.
 *
 * <p>This exists because the failure mode here is silent. Building the same transform in the development tool
 * reported "0 changed" three times while skipping every section, and a no-op looks exactly like success: the world
 * loads, nothing errors, and the stored biome quietly stays wrong. These tests assert a NON-ZERO count, which is the
 * assertion that would have caught it.
 */
class HistoricalRegionBiomesTest {
    private static final String TARGET = "moderner_beta:late_beta_plains";

    /** One section holding the given biome names, in the shape a real chunk uses. */
    private static CompoundTag section(String... biomes) {
        ListTag palette = new ListTag();
        for (String biome : biomes) {
            palette.add(StringTag.valueOf(biome));
        }
        CompoundTag container = new CompoundTag();
        container.put("palette", palette);
        CompoundTag section = new CompoundTag();
        section.put("biomes", container);
        return section;
    }

    private static CompoundTag chunk(CompoundTag... sections) {
        ListTag list = new ListTag();
        for (CompoundTag section : sections) {
            list.add(section);
        }
        CompoundTag chunk = new CompoundTag();
        chunk.put("sections", list);
        return chunk;
    }

    private static String firstBiome(CompoundTag chunk) {
        return chunk.getList("sections", Tag.TAG_COMPOUND).getCompound(0)
            .getCompound("biomes").getList("palette", Tag.TAG_STRING).getString(0);
    }

    /** The two destinations this file exercises: the late beta generation and the early release one. */
    private static final HistoricalVersion TO_BETA_181 =
        HistoricalVersions.byId("historical_experience:beta_1_8_1").orElseThrow();
    private static final HistoricalVersion TO_RELEASE_1_0 =
        HistoricalVersions.byId("historical_experience:release_1_0_0").orElseThrow();

    @Test
    @DisplayName("a stored biome is carried to its own counterpart, and the count is not zero")
    void carriesBiomeToItsCounterpart() {
        CompoundTag chunk = chunk(section("moderner_beta:beta_taiga"));
        int changed = HistoricalRegionBiomes.rewriteBiomes(chunk, TO_BETA_181);
        assertEquals(1, changed, "the rewrite must report the entry it changed, because a silent no-op looks like success");
        assertEquals("moderner_beta:late_beta_taiga", firstBiome(chunk),
            "a taiga must become a taiga at a Beta 1.8.1 destination; writing one biome over everything is the bug "
                + "the owner found");
    }

    @Test
    @DisplayName("each biome in a mixed chunk moves to its own counterpart, and unlisted ones do not move")
    void carriesEachBiomeIndependently() {
        CompoundTag chunk = chunk(
            section("moderner_beta:beta_plains", "minecraft:ocean"),
            section("moderner_beta:beta_taiga"),
            section("moderner_beta:early_release_swampland", "minecraft:desert"));
        // Five palette entries, three of which move at a LATE BETA destination: the ocean and the desert have no
        // counterpart anywhere, and early_release_swampland belongs to a LATER generation so the gate pulls it back to
        // this one's name. That direction is deliberate - the destination decides where a biome lands, so a world moved
        // to Beta 1.8.1 holds that generation's names and not a future generation's.
        int changed = HistoricalRegionBiomes.rewriteBiomes(chunk, TO_BETA_181);
        assertEquals(3, changed, "every name from an earlier or later generation moves to this one; the rest are left");
        assertEquals("moderner_beta:late_beta_plains", chunk.getList("sections", Tag.TAG_COMPOUND)
            .getCompound(0).getCompound("biomes").getList("palette", Tag.TAG_STRING).getString(0));
        assertEquals("minecraft:ocean", chunk.getList("sections", Tag.TAG_COMPOUND)
            .getCompound(0).getCompound("biomes").getList("palette", Tag.TAG_STRING).getString(1),
            "an ocean has no counterpart anywhere in the hierarchy, so it is left alone");
        assertEquals("moderner_beta:late_beta_taiga", chunk.getList("sections", Tag.TAG_COMPOUND)
            .getCompound(1).getCompound("biomes").getList("palette", Tag.TAG_STRING).getString(0));
        assertEquals("moderner_beta:late_beta_swampland", chunk.getList("sections", Tag.TAG_COMPOUND)
            .getCompound(2).getCompound("biomes").getList("palette", Tag.TAG_STRING).getString(0),
            "an early release name is pulled back to this generation's name");
        assertEquals("minecraft:desert", chunk.getList("sections", Tag.TAG_COMPOUND)
            .getCompound(2).getCompound("biomes").getList("palette", Tag.TAG_STRING).getString(1));
    }

    @Test
    @DisplayName("the same stored biomes land differently depending only on the destination generation")
    void theGateDecidesTheDestination() {
        // This is the whole point of the gate, in one assertion: the SAME stored biome lands in two different places
        // depending on where the world is being taken.
        CompoundTag toBeta = chunk(section("moderner_beta:beta_taiga"));
        assertEquals(1, HistoricalRegionBiomes.rewriteBiomes(toBeta, TO_BETA_181));
        assertEquals("moderner_beta:late_beta_taiga", firstBiome(toBeta));

        CompoundTag toRelease = chunk(section("moderner_beta:beta_taiga"));
        assertEquals(1, HistoricalRegionBiomes.rewriteBiomes(toRelease, TO_RELEASE_1_0));
        assertEquals("moderner_beta:early_release_taiga", firstBiome(toRelease),
            "from Release 1.0 the early release name is selected, which is the owner's 'and then early release'");

        // And a destination before Beta 1.8.1 collapses it to the single Biome plains instead.
        HistoricalVersion before = HistoricalVersions.byId("historical_experience:beta_1_7_3").orElseThrow();
        CompoundTag toEarly = chunk(section("moderner_beta:infdev_611"));
        assertEquals(1, HistoricalRegionBiomes.rewriteBiomes(toEarly, before));
        assertEquals("moderner_beta:beta_plains", firstBiome(toEarly),
            "an Infdev placeholder going to a pre-1.8.1 build becomes Beta plains, never a Late Beta name");
    }

    @Test
    @DisplayName("placeholder chunks around generated terrain are not 'generated beforehand'")
    void placeholderChunksAreNotPreExisting() {
        // Measured in a real upgraded world: a third of the recorded chunks were structure_starts placeholders, which
        // have no terrain or biome and must stay free to generate normally.
        for (String status : new String[] {"minecraft:empty", "minecraft:structure_starts",
            "minecraft:structure_references"}) {
            CompoundTag chunk = new CompoundTag();
            chunk.putString("Status", status);
            assertFalse(HistoricalRegionBiomes.hasGeneratedBiomes(chunk), status + " has no biomes yet");
        }
        for (String status : new String[] {"minecraft:biomes", "minecraft:noise", "minecraft:carvers",
            "minecraft:initialize_light", "minecraft:light", "minecraft:full"}) {
            CompoundTag chunk = new CompoundTag();
            chunk.putString("Status", status);
            assertTrue(HistoricalRegionBiomes.hasGeneratedBiomes(chunk), status + " already holds computed biomes");
        }
        assertTrue(HistoricalRegionBiomes.hasGeneratedBiomes(new CompoundTag()),
            "an older chunk with no status field predates placeholders and was generated");
    }

    @Test
    @DisplayName("a chunk that needs nothing reports no change, so a second run writes nothing")
    void alreadyConvertedIsANoOp() {
        CompoundTag chunk = chunk(section("moderner_beta:late_beta_plains"),
            section("minecraft:ocean", "moderner_beta:late_beta_taiga"));
        assertEquals(0, HistoricalRegionBiomes.rewriteBiomes(chunk, TO_BETA_181));
        assertFalse(HistoricalRegionBiomes.holdsCarriedBiome(chunk, TO_BETA_181));
    }

    @Test
    @DisplayName("the scan sees a stored biome that would move, and not one that would not")
    void scanDetectsCarriedBiomes() {
        assertTrue(HistoricalRegionBiomes.holdsCarriedBiome(
            chunk(section("moderner_beta:late_beta_plains"), section("moderner_beta:beta_taiga")), TO_BETA_181));
        // The Alpha placeholder - what an Infdev or early-Alpha world stores - is exactly what must be carried forward
        // at a Beta 1.8.1 destination.
        assertTrue(HistoricalRegionBiomes.holdsCarriedBiome(chunk(section("moderner_beta:alpha")), TO_BETA_181),
            "the Alpha biome is what an Infdev or Alpha world holds, and it moves at a Beta 1.8.1 destination");
        // A declared Infdev profile name is NOT the same thing: Moderner Beta can name a biome after a build, but only
        // the Alpha placeholder is what those worlds actually store, so a stale infdev_* name has no counterpart.
        assertFalse(HistoricalRegionBiomes.holdsCarriedBiome(chunk(section("moderner_beta:infdev_611")), TO_BETA_181),
            "a stored infdev_* name has no counterpart in the default sequence, so it is left alone");
        assertFalse(HistoricalRegionBiomes.holdsCarriedBiome(
            chunk(section("moderner_beta:late_beta_plains"), section("minecraft:ocean")), TO_BETA_181));
        // A chunk with no sections at all must not be mistaken for one that needs work.
        assertFalse(HistoricalRegionBiomes.holdsCarriedBiome(new CompoundTag(), TO_BETA_181));
    }
}
