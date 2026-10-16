/* SPDX-License-Identifier: LGPL-3.0-or-later */
package net.historicalexperience.world.upgrade;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BiomeRecordRepairTest {
    @TempDir Path world;

    @Test void negativeRegionKeysAndUnknownFieldsArePreservedAndPruningIsIdempotent() throws Exception {
        long released = ChunkPos.asLong(-1, -1), kept = ChunkPos.asLong(-33, -33), origin = ChunkPos.asLong(0, 0);
        BiomeConversionRecord.record(world, "infdev", 888, Set.of(released, kept, origin), 42);
        Path file = BiomeConversionRecord.fileFor(world);
        var root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        root.addProperty("futureOwnedSentinel", "keep this field"); Files.writeString(file, root.toString());
        byte[] original = Files.readAllBytes(file);
        assertEquals(1, BiomeConversionRecord.forgetReleasedChunks(world, Set.of(released)));
        assertEquals(Set.of(kept, origin), BiomeConversionRecord.convertedChunks(world));
        assertEquals(42, BiomeConversionRecord.completedUpgradeRevision(world));
        assertEquals(888, BiomeConversionRecord.lastConvertedAt(world));
        assertEquals("keep this field", JsonParser.parseString(Files.readString(file)).getAsJsonObject().get("futureOwnedSentinel").getAsString());
        Path backup = world.resolve("historical_experience_finite_transition_backup/biome-conversion."
            + WorldBackup.sha256(original) + ".before.json");
        assertArrayEquals(original, Files.readAllBytes(backup));
        byte[] after = Files.readAllBytes(file);
        assertEquals(0, BiomeConversionRecord.forgetReleasedChunks(world, Set.of(released)));
        assertArrayEquals(after, Files.readAllBytes(file));
    }

    @Test void malformedMaskRefusesInsteadOfTreatingRecordAsEmpty() throws Exception {
        BiomeConversionRecord.record(world, "infdev", 888, Set.of(0L), 42);
        Path file = BiomeConversionRecord.fileFor(world);
        var root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        root.getAsJsonArray("convertedChunks").get(0).getAsJsonObject().addProperty("chunks", "invalid-mask");
        Files.writeString(file, root.toString()); byte[] original = Files.readAllBytes(file);
        assertThrows(IOException.class, () -> BiomeConversionRecord.forgetReleasedChunks(world, Set.of(0L)));
        assertArrayEquals(original, Files.readAllBytes(file));
        assertFalse(Files.exists(world.resolve("historical_experience_finite_transition_backup")));
    }

    @Test void corruptContentAddressedBackupIsNeverOverwrittenOrUsed() throws Exception {
        BiomeConversionRecord.record(world, "infdev", 888, Set.of(0L), 42);
        Path file = BiomeConversionRecord.fileFor(world); byte[] original = Files.readAllBytes(file);
        Path backup = world.resolve("historical_experience_finite_transition_backup/biome-conversion."
            + WorldBackup.sha256(original) + ".before.json");
        Files.createDirectories(backup.getParent()); Files.writeString(backup, "corrupt");
        assertThrows(IOException.class, () -> BiomeConversionRecord.forgetReleasedChunks(world, Set.of(0L)));
        assertArrayEquals(original, Files.readAllBytes(file)); assertEquals("corrupt", Files.readString(backup));
    }
}
