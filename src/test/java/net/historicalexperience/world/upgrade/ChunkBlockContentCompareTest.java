/* SPDX-License-Identifier: LGPL-3.0-or-later */
package net.historicalexperience.world.upgrade;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

/**
 * Developer measurement, skipped unless armed: are two stored copies of the SAME chunk equal in their BLOCKS, and if not,
 * where exactly do they differ? Uses the project's own region reader, so it parses real NBT instead of counting byte markers
 * (the round-16 diff did the latter and said so).
 *
 * <p>Armed with {@code -PchunkCompare=<regionA>;<regionB>;<chunkX>;<chunkZ>}. It compares, per section: the block-state palette
 * and the packed block-state data, then lists which other top-level keys differ. It asserts nothing: what it prints is the
 * measurement. The comparison is deliberately "everything but lighting is equal?", because the open question is whether a
 * re-saved chunk changed anything a player could see.
 */
class ChunkBlockContentCompareTest {
    @Test
    void compareTwoStoredCopiesOfAChunkWhenArmed() throws Exception {
        String armed = System.getProperty("historical_experience.chunkCompare");
        assumeTrue(armed != null && !armed.isBlank(), "measurement only; arm with -PchunkCompare=<A>;<B>;<x>;<z>");
        String[] parts = armed.split(";");
        Path a = Path.of(parts[0]);
        Path b = Path.of(parts[1]);
        int x = Integer.parseInt(parts[2]);
        int z = Integer.parseInt(parts[3]);
        CompoundTag ta = chunk(a, x, z);
        CompoundTag tb = chunk(b, x, z);
        System.out.println("[chunk-compare] chunk " + x + "," + z + " present A=" + (ta != null) + " B=" + (tb != null));
        if (ta == null || tb == null) {
            return;
        }
        ListTag sa = ta.getList("sections", Tag.TAG_COMPOUND);
        ListTag sb = tb.getList("sections", Tag.TAG_COMPOUND);
        System.out.println("[chunk-compare] sections A=" + sa.size() + " B=" + sb.size());
        int blockSectionsEqual = 0;
        int blockSectionsDifferent = 0;
        List<String> detail = new ArrayList<>();
        for (int i = 0; i < Math.min(sa.size(), sb.size()); i++) {
            CompoundTag ca = sa.getCompound(i);
            CompoundTag cb = sb.getCompound(i);
            int y = ca.getByte("Y");
            Tag bsA = ca.get("block_states");
            Tag bsB = cb.get("block_states");
            boolean blocksEqual = java.util.Objects.equals(bsA, bsB);
            if (blocksEqual) {
                blockSectionsEqual++;
            } else {
                blockSectionsDifferent++;
                detail.add("section Y=" + y + " block_states DIFFER");
            }
            boolean skyA = ca.contains("SkyLight");
            boolean skyB = cb.contains("SkyLight");
            boolean blA = ca.contains("BlockLight");
            boolean blB = cb.contains("BlockLight");
            boolean biomesEqual = java.util.Objects.equals(ca.get("biomes"), cb.get("biomes"));
            if (skyA != skyB || blA != blB || !biomesEqual) {
                detail.add("section Y=" + y + " SkyLight " + skyA + "->" + skyB + " BlockLight " + blA + "->" + blB
                    + " biomesEqual=" + biomesEqual);
            }
        }
        System.out.println("[chunk-compare] block_states: equal sections=" + blockSectionsEqual
            + " different sections=" + blockSectionsDifferent);
        detail.forEach(line -> System.out.println("[chunk-compare]   " + line));
        List<String> keys = new ArrayList<>();
        for (String key : ta.getAllKeys()) {
            if (!key.equals("sections") && !java.util.Objects.equals(ta.get(key), tb.get(key))) {
                keys.add(key);
            }
        }
        for (String key : tb.getAllKeys()) {
            if (!ta.contains(key) && !keys.contains(key)) {
                keys.add(key + "(only in B)");
            }
        }
        System.out.println("[chunk-compare] other top-level keys that differ: " + keys);
        System.out.println("[chunk-compare] block_entities equal=" + java.util.Objects.equals(
            ta.get("block_entities"), tb.get("block_entities")) + " blocks-only verdict="
            + (blockSectionsDifferent == 0 ? "BLOCKS IDENTICAL" : "BLOCKS DIFFER"));
    }

    private static CompoundTag chunk(Path regionDir, int x, int z) throws Exception {
        Path file = regionDir.resolve("r." + (x >> 5) + "." + (z >> 5) + ".mca");
        if (!Files.isRegularFile(file)) {
            return null;
        }
        for (var chunk : new FiniteRegionSnapshot(file).chunks) {
            if (chunk.x() == x && chunk.z() == z) {
                return chunk.tag();
            }
        }
        return null;
    }
}
