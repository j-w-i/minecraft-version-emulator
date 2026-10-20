/* SPDX-License-Identifier: LGPL-3.0-or-later */
package net.historicalexperience.verification;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import net.historicalexperience.world.upgrade.WorldBackup;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.LevelResource;

/** Opt-in, owned-project-fixture preparation/acceptance ONLY. Never runs in a normal client/server.
 * The build/chest are real server blocks; the saved-player NBT is an explicit synthetic file sentinel, not a login test.
 */
public final class FiniteRepairFixtureVerification {
    private static final String MARKER = "finite-repair-build-fixture.json";
    private static final String PLAYER = "71e913b0-56c6-4f22-9278-739097cc0001";
    private static final String BIOME = "moderner_beta:infdev_420";
    private static final BlockPos CHEST = new BlockPos(130, 65, 2);
    private FiniteRepairFixtureVerification() { }

    private static Path ownedWorld(ServerLevel level) throws Exception {
        Path world = level.getServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
        if (!Boolean.getBoolean("historical_experience.generationProbe") || !world.getFileName().toString().equals("world")
                || !world.getParent().getFileName().toString().startsWith("sol-")
                || !world.getParent().getParent().getFileName().toString().equals("verification")
                || !Files.isRegularFile(world.resolve("historical_experience_diagnostic_replica.json"))) {
            throw new IllegalStateException("Build fixture preparation requires a new diagnostic project replica");
        }
        var receipt = JsonParser.parseString(Files.readString(world.resolve("historical_experience_diagnostic_replica.json"))).getAsJsonObject();
        if (!receipt.has("sourceFilesVerifiedUnchanged") || !receipt.get("sourceFilesVerifiedUnchanged").getAsBoolean()) {
            throw new IllegalStateException("Diagnostic source-unchanged receipt is missing");
        }
        return world;
    }

    public static void prepare(ServerLevel level) throws Exception {
        Path world = ownedWorld(level);
        if (Files.exists(world.resolve(MARKER)) || Files.exists(world.resolve("playerdata/" + PLAYER + ".dat"))) {
            throw new IllegalStateException("Never overwrite an existing build/player fixture");
        }
        for (int x = 129; x <= 131; x++) for (int z = 1; z <= 3; z++) {
            if (!level.getBlockState(new BlockPos(x, 64, z)).isAir()) throw new IllegalStateException("Fixture build area is occupied");
        }
        if (!level.getBlockState(CHEST).isAir()) throw new IllegalStateException("Fixture chest position is occupied");
        assertReplicaBlocks(level);
        for (int x = 129; x <= 131; x++) for (int z = 1; z <= 3; z++) level.setBlockAndUpdate(new BlockPos(x, 64, z), Blocks.COBBLESTONE.defaultBlockState());
        level.setBlockAndUpdate(CHEST, Blocks.CHEST.defaultBlockState());
        if (!(level.getBlockEntity(CHEST) instanceof ChestBlockEntity chest)) throw new IllegalStateException("No fixture chest");
        chest.setItem(0, new ItemStack(Items.DIAMOND, 7)); chest.setChanged();
        int result = level.getServer().getCommands().getDispatcher().execute(
            "fillbiome 144 0 0 159 127 15 " + BIOME, level.getServer().createCommandSourceStack());
        if (result <= 0) throw new IllegalStateException("Could not create destination-native exact-pattern negative fixture");

        var player = new CompoundTag();
        player.putUUID("UUID", UUID.fromString(PLAYER)); player.putString("Dimension", "minecraft:overworld");
        player.putInt("DataVersion", net.minecraft.SharedConstants.getCurrentVersion().getDataVersion().getVersion());
        player.putFloat("Health", 20); player.putInt("foodLevel", 20); player.putString("FixtureKind", "synthetic saved-player sentinel; not a login test");
        var inventory = new ListTag();
        var stack = (CompoundTag) new ItemStack(Items.DIAMOND, 11).save(level.registryAccess()); stack.putByte("Slot", (byte) 0); inventory.add(stack);
        player.put("Inventory", inventory);
        Path data = world.resolve("playerdata/" + PLAYER + ".dat"); Files.createDirectories(data.getParent()); NbtIo.writeCompressed(player, data);
        var marker = Map.of("schema", 1, "player", PLAYER, "playerDataSha256", WorldBackup.sha256(Files.readAllBytes(data)),
            "replicaBiome", BIOME, "chestDiamonds", 7, "playerDataKind", "synthetic NBT file, not authenticated-player acceptance");
        Files.writeString(world.resolve(MARKER), new GsonBuilder().setPrettyPrinting().create().toJson(marker));
        level.getServer().saveEverything(true, true, true);
    }

    public static Map<String, Object> verify(ServerLevel level) throws Exception {
        Path root = level.getServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
        if (!Files.exists(root.resolve(MARKER))) return Map.of("present", false);
        Path world = ownedWorld(level);
        var marker = JsonParser.parseString(Files.readString(world.resolve(MARKER))).getAsJsonObject();
        if (marker.get("schema").getAsInt() != 1 || !marker.get("player").getAsString().equals(PLAYER)) throw new IllegalStateException("Unknown fixture marker");
        for (int x = 129; x <= 131; x++) for (int z = 1; z <= 3; z++) {
            if (!level.getBlockState(new BlockPos(x, 64, z)).is(Blocks.COBBLESTONE)) throw new IllegalStateException("Saved exterior platform changed");
        }
        if (!(level.getBlockEntity(CHEST) instanceof ChestBlockEntity chest)
                || !chest.getItem(0).is(Items.DIAMOND) || chest.getItem(0).getCount() != 7) throw new IllegalStateException("Saved exterior chest/inventory changed");
        String playerHash = WorldBackup.sha256(Files.readAllBytes(world.resolve("playerdata/" + PLAYER + ".dat")));
        if (!playerHash.equals(marker.get("playerDataSha256").getAsString())) throw new IllegalStateException("Saved player-data bytes changed");
        assertReplicaBlocks(level);
        var chunk = level.getChunk(9, 0);
        for (int y = 0; y < 32; y++) for (int z = 0; z < 4; z++) for (int x = 36; x < 40; x++) {
            if (!chunk.getNoiseBiome(x, y, z).unwrapKey().orElseThrow().location().toString().equals(BIOME)) {
                throw new IllegalStateException("Destination-native exact-pattern chunk lost its edited biome");
            }
        }
        return Map.of("present", true, "exteriorPlatformBlocks", 9, "chestDiamonds", 7, "replicaBiomeCells", 512,
            "savedPlayerDataSha256", playerHash, "savedPlayerDataKind", "synthetic file sentinel; real login not verified");
    }

    private static void assertReplicaBlocks(ServerLevel level) {
        for (int y = 0; y < 128; y++) for (int z = 0; z < 16; z++) for (int x = 144; x < 160; x++) {
            var expected = y < 54 ? Blocks.BEDROCK : y == 54 ? Blocks.DIRT : y < 64 ? Blocks.WATER : Blocks.AIR;
            if (!level.getBlockState(new BlockPos(x, y, z)).is(expected)) throw new IllegalStateException("Negative replica fixture is not an exact apron block table");
        }
    }
}
