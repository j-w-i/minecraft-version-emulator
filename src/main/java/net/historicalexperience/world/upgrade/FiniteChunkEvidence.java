/* SPDX-License-Identifier: LGPL-3.0-or-later */
package net.historicalexperience.world.upgrade;

import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** Saved evidence in addition to the exact block table. Palette membership alone is NOT evidence: unused entries remain.
 * Native default finite biomes were measured in real pinned-provider saves (Indev normal + out-of-bounds void).
 * Converted biomes require an actual saved LastUpdate no newer than the verified pre-upgrade backup's game time.
 * Missing/rewritten/unsupported evidence is retained, never inferred from a later conversion census.
 * This is conservative encoded-save evidence, not a cryptographic receipt against deliberately forged NBT.
 */
record FiniteChunkEvidence(Long sourceGameTime, Set<String> convertedBiomes) {
    private static final Set<String> NATIVE = Set.of("moderner_beta:indev_normal", "minecraft:the_void");
    private static final Set<String> ROOT_FIELDS = Set.of("DataVersion", "xPos", "yPos", "zPos", "Status", "LastUpdate",
        "structures", "InhabitedTime", "Heightmaps", "sections", "isLightOn", "block_entities", "entities",
        "block_ticks", "fluid_ticks", "PostProcessing", "CarvingMasks");
    private static final Set<String> STATUSES = Set.of("minecraft:empty", "minecraft:structure_starts", "minecraft:structure_references",
        "minecraft:biomes", "minecraft:noise", "minecraft:surface", "minecraft:carvers", "minecraft:features",
        "minecraft:initialize_light", "minecraft:light", "minecraft:spawn", "minecraft:full");
    FiniteChunkEvidence {
        if (sourceGameTime != null && sourceGameTime < 0) throw new IllegalArgumentException("Invalid source game time");
        convertedBiomes = Set.copyOf(convertedBiomes);
    }
    static FiniteChunkEvidence nativeOnly() { return new FiniteChunkEvidence(null, Set.of()); }

    boolean allows(FiniteRegionSnapshot.Chunk saved) {
        CompoundTag chunk = saved.tag();
        if (!chunk.getAllKeys().stream().allMatch(ROOT_FIELDS::contains)
                || !chunk.contains("xPos", Tag.TAG_INT) || chunk.getInt("xPos") != saved.x()
                || !chunk.contains("zPos", Tag.TAG_INT) || chunk.getInt("zPos") != saved.z()
                || !STATUSES.contains(chunk.getString("Status"))) return false;
        // The finite provider pre-fills neighbouring protochunks before vanilla's status pipeline catches up.
        // A FULL-only test retains real walls. Empty vanilla carving metadata is not a mod attachment.
        if (chunk.contains("CarvingMasks") && (!(chunk.get("CarvingMasks") instanceof CompoundTag masks) || !masks.isEmpty())) return false;
        for (String key : Set.of("block_ticks", "fluid_ticks", "entities", "block_entities")) {
            if (chunk.contains(key) && (!(chunk.get(key) instanceof net.minecraft.nbt.ListTag list) || !list.isEmpty())) return false;
        }
        if (chunk.contains("structures")) {
            if (!(chunk.get("structures") instanceof CompoundTag structures)) return false;
            for (String key : structures.getAllKeys()) {
                if (!Set.of("References", "starts").contains(key)
                        || !(structures.get(key) instanceof CompoundTag contents) || !contents.isEmpty()) return false;
            }
        }
        if (chunk.contains("PostProcessing")) {
            if (!(chunk.get("PostProcessing") instanceof net.minecraft.nbt.ListTag lists) || (!lists.isEmpty() && lists.size() != 8)) return false;
            for (Tag tag : lists) {
                if (!(tag instanceof net.minecraft.nbt.ListTag list)) return false;
                for (Tag position : list) {
                    if (!(position instanceof net.minecraft.nbt.ShortTag value) || value.getAsShort() < 0 || value.getAsShort() >= 4096) return false;
                }
            }
        }
        if (biomesAre(chunk, NATIVE)) return true;
        return sourceGameTime != null && !convertedBiomes.isEmpty() && chunk.contains("LastUpdate", Tag.TAG_LONG)
            && chunk.getLong("LastUpdate") >= 0 && chunk.getLong("LastUpdate") <= sourceGameTime
            && biomesAre(chunk, convertedBiomes);
    }

    /** Decode ALL 64 used biome cells in every finite section; reject missing, duplicate and malformed sections/data. */
    private static boolean biomesAre(CompoundTag chunk, Set<String> allowed) {
        var sections = chunk.getList("sections", Tag.TAG_COMPOUND);
        boolean[] seen = new boolean[8];
        for (Tag entry : sections) {
            CompoundTag section = (CompoundTag) entry;
            if (!section.getAllKeys().stream().allMatch(key -> Set.of("Y", "block_states", "biomes", "BlockLight", "SkyLight").contains(key))
                    || !section.contains("Y", Tag.TAG_BYTE)) return false;
            int y = section.getByte("Y");
            if (y < 0 || y >= 8) return false;
            if (seen[y]) return false;
            seen[y] = true;
            if (section.contains("block_states")) {
                if (!(section.get("block_states") instanceof CompoundTag blocks)
                        || !blocks.getAllKeys().stream().allMatch(key -> key.equals("palette") || key.equals("data"))
                        || (blocks.contains("data") && !blocks.contains("data", Tag.TAG_LONG_ARRAY))) return false;
                for (Tag state : blocks.getList("palette", Tag.TAG_COMPOUND)) {
                    var value = (CompoundTag) state;
                    if (!value.getAllKeys().stream().allMatch(key -> key.equals("Name") || key.equals("Properties"))
                            || (value.contains("Properties") && !(value.get("Properties") instanceof CompoundTag))) return false;
                }
            }
            var biomes = section.getCompound("biomes");
            if (!biomes.getAllKeys().stream().allMatch(key -> key.equals("palette") || key.equals("data"))) return false;
            if (biomes.contains("data") && !biomes.contains("data", Tag.TAG_LONG_ARRAY)) return false;
            var palette = biomes.getList("palette", Tag.TAG_STRING);
            if (palette.isEmpty() || palette.size() > 64) return false;
            int bits = Math.max(1, 32 - Integer.numberOfLeadingZeros(palette.size() - 1));
            int perLong = 64 / bits;
            long[] data = biomes.getLongArray("data");
            if (palette.size() > 1 && (!biomes.contains("data", Tag.TAG_LONG_ARRAY)
                    || data.length != (64 + perLong - 1) / perLong)) return false;
            for (int cell = 0; cell < 64; cell++) {
                int index = palette.size() == 1 ? 0 : (int) ((data[cell / perLong] >>> ((cell % perLong) * bits)) & ((1L << bits) - 1));
                if (index >= palette.size() || !allowed.contains(palette.getString(index))) return false;
            }
        }
        for (boolean value : seen) if (!value) return false;
        return true;
    }
}
