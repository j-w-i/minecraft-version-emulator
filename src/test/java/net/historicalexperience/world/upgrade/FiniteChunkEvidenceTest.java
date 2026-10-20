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

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

class FiniteChunkEvidenceTest {
    static FiniteRegionSnapshot.Chunk saved() {
        var tag = FiniteApronReleaseTest.apron(); tag.putInt("xPos", 8); tag.putInt("zPos", 0);
        return new FiniteRegionSnapshot.Chunk(8, 8, 0, tag);
    }
    static void biomes(CompoundTag tag, String id) {
        for (Tag entry : tag.getList("sections", Tag.TAG_COMPOUND)) {
            var palette = new ListTag(); palette.add(StringTag.valueOf(id));
            var biome = new CompoundTag(); biome.put("palette", palette); ((CompoundTag) entry).put("biomes", biome);
        }
    }
    private static FiniteChunkEvidence converted(Long time) { return new FiniteChunkEvidence(time, Set.of("moderner_beta:alpha")); }

    @Test void nativeNormalAndOutOfBoundsVoidAreSupported() {
        var saved = saved(); assertTrue(FiniteChunkEvidence.nativeOnly().allows(saved));
        biomes(saved.tag(), "minecraft:the_void"); assertTrue(FiniteChunkEvidence.nativeOnly().allows(saved));
    }
    @Test void unusedForeignPaletteEntryDoesNotInvalidateNativeCellsButUnusedNativeDoesNotProveForeignCells() {
        var saved = saved();
        var b = saved.tag().getList("sections", Tag.TAG_COMPOUND).getCompound(0).getCompound("biomes");
        b.getList("palette", Tag.TAG_STRING).add(StringTag.valueOf("moderner_beta:infdev_420"));
        b.putLongArray("data", new long[] {0});
        assertTrue(FiniteChunkEvidence.nativeOnly().allows(saved));
        var palette = new ListTag(); palette.add(StringTag.valueOf("moderner_beta:infdev_420")); palette.add(StringTag.valueOf("moderner_beta:indev_normal"));
        b.put("palette", palette);
        assertFalse(FiniteChunkEvidence.nativeOnly().allows(saved));
    }
    @Test void evenOneUsedForeignBiomeCellIsNotDefaultFiniteSource() {
        var saved = saved();
        var b = saved.tag().getList("sections", Tag.TAG_COMPOUND).getCompound(7).getCompound("biomes");
        b.getList("palette", Tag.TAG_STRING).add(StringTag.valueOf("moderner_beta:infdev_611"));
        b.putLongArray("data", new long[] {Long.MIN_VALUE}); // Last of all 512 biome cells, not just the first.
        assertFalse(FiniteChunkEvidence.nativeOnly().allows(saved));
    }
    @Test void actualNativeNormalVoidMixtureIsSupported() {
        var saved = saved();
        for (Tag entry : saved.tag().getList("sections", Tag.TAG_COMPOUND)) {
            var b = ((CompoundTag) entry).getCompound("biomes");
            b.getList("palette", Tag.TAG_STRING).add(StringTag.valueOf("minecraft:the_void"));
            b.putLongArray("data", new long[] {0xeeeeeeeeeeeeeeeeL});
        }
        assertTrue(FiniteChunkEvidence.nativeOnly().allows(saved));
    }
    @Test void convertedCellsRequireActualOldLongTimestampAndVerifiedCutoff() {
        var saved = saved(); biomes(saved.tag(), "moderner_beta:alpha");
        assertFalse(converted(100L).allows(saved));
        saved.tag().putLong("LastUpdate", 100); assertTrue(converted(100L).allows(saved));
        assertFalse(converted(null).allows(saved)); assertFalse(FiniteChunkEvidence.nativeOnly().allows(saved));
        saved.tag().putLong("LastUpdate", 101); assertFalse(converted(100L).allows(saved));
        saved.tag().putLong("LastUpdate", -1); assertFalse(converted(100L).allows(saved));
        saved.tag().putInt("LastUpdate", 0); assertFalse(converted(100L).allows(saved));
    }
    @Test void missingAndDuplicateSectionsAreNotGuessed() {
        var saved = saved(); var sections = saved.tag().getList("sections", Tag.TAG_COMPOUND);
        sections.add(sections.getCompound(0).copy()); assertFalse(FiniteChunkEvidence.nativeOnly().allows(saved));
        sections.remove(8); sections.remove(7); assertFalse(FiniteChunkEvidence.nativeOnly().allows(saved));
    }
    @Test void malformedPackedBiomesAreNotNativeEvidence() {
        var saved = saved();
        var b = saved.tag().getList("sections", Tag.TAG_COMPOUND).getCompound(0).getCompound("biomes");
        b.putInt("data", 0); assertFalse(FiniteChunkEvidence.nativeOnly().allows(saved)); b.remove("data");
        b.getList("palette", Tag.TAG_STRING).add(StringTag.valueOf("minecraft:the_void"));
        assertFalse(FiniteChunkEvidence.nativeOnly().allows(saved)); // Missing data for a multi-entry palette.
        b.getList("palette", Tag.TAG_STRING).add(StringTag.valueOf("moderner_beta:indev_normal"));
        b.putLongArray("data", new long[] {3, 0}); // Two-bit index 3 exceeds a three-entry palette.
        assertFalse(FiniteChunkEvidence.nativeOnly().allows(saved));
    }
    @Test void unknownRootAndSectionAttachmentsArePreserved() {
        var saved = saved(); saved.tag().put("claim_mod:owned_data", new CompoundTag());
        assertFalse(FiniteChunkEvidence.nativeOnly().allows(saved)); saved.tag().remove("claim_mod:owned_data");
        var section = saved.tag().getList("sections", Tag.TAG_COMPOUND).getCompound(0);
        section.put("claim_mod:owned_data", new CompoundTag());
        assertFalse(FiniteChunkEvidence.nativeOnly().allows(saved)); section.remove("claim_mod:owned_data");
        section.getCompound("block_states").putInt("claim_mod:owned_data", 1);
        assertFalse(FiniteChunkEvidence.nativeOnly().allows(saved));
    }
    @Test void prefilledFiniteProtochunksHaveKnownStatusAndVanillaGenerationQueuesNotPlayerData() {
        var saved = saved(); saved.tag().putString("Status", "minecraft:structure_starts");
        saved.tag().put("CarvingMasks", new CompoundTag());
        var lists = new ListTag(); for (int section = 0; section < 8; section++) lists.add(new ListTag());
        var queue = (ListTag) lists.get(3); queue.add(net.minecraft.nbt.ShortTag.valueOf((short) 4095));
        saved.tag().put("PostProcessing", lists);
        assertTrue(FiniteChunkEvidence.nativeOnly().allows(saved));
        queue.add(net.minecraft.nbt.ShortTag.valueOf((short) 4096)); assertFalse(FiniteChunkEvidence.nativeOnly().allows(saved));
        queue.remove(1); saved.tag().getCompound("CarvingMasks").putLongArray("AIR", new long[] {1});
        assertFalse(FiniteChunkEvidence.nativeOnly().allows(saved));
        saved.tag().put("CarvingMasks", new CompoundTag()); saved.tag().putString("Status", "unknown:status");
        assertFalse(FiniteChunkEvidence.nativeOnly().allows(saved));
    }

    @Test void scheduledTicksStructuresAndWrongStoredCoordinatesAreNotPristineSourceMetadata() {
        var saved = saved(); var ticks = new ListTag(); ticks.add(new CompoundTag()); saved.tag().put("block_ticks", ticks);
        assertFalse(FiniteChunkEvidence.nativeOnly().allows(saved)); saved.tag().remove("block_ticks");
        var structures = new CompoundTag(); var starts = new CompoundTag(); starts.put("custom:claim", new CompoundTag());
        structures.put("starts", starts); saved.tag().put("structures", structures);
        assertFalse(FiniteChunkEvidence.nativeOnly().allows(saved)); saved.tag().remove("structures");
        saved.tag().putInt("xPos", 9); assertFalse(FiniteChunkEvidence.nativeOnly().allows(saved));
    }
}
