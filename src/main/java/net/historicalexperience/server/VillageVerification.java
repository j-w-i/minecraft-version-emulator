/*
 * Verification harness for the ported historical village. This file is NOT part of the port: it exists so
 * that "a historical_experience:beta_1_8_1 world really contains a village" can be checked against the
 * saved world rather than against a log line, and so that the block ids inside a generated village can be
 * reported one by one.
 *
 * It registers only when -Dhistorical_experience.villageProbe=true is set, and it never runs in an
 * ordinary game launch. It writes its report into the world directory and halts the server, exactly like
 * the project's existing generation probe.
 *
 * The ported code it exercises is the NeoForge mod "OldVillages"
 * (https://github.com/MaxTechnik/OldVillages, pinned revision 7578013ec061303982539fac7606df67577ffb26,
 * MIT as declared in its gradle.properties; that checkout ships no LICENSE file).
 */
package net.historicalexperience.server;

import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import com.google.gson.GsonBuilder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.historicalexperience.HistoricalExperienceMod;
import net.historicalexperience.generation.HistoricalBlockPalette;
import net.historicalexperience.worldgen.HistoricalVillageBlocks;
import net.historicalexperience.worldgen.HistoricalVillagePieces;
import net.historicalexperience.worldgen.HistoricalVillageRules;
import net.historicalexperience.worldgen.HistoricalVillageStructure;
import net.historicalexperience.worldgen.HistoricalVillageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Finds a real village, forces it to generate, and reports exactly which blocks are in it.
 *
 * <h2>How the village is found</h2>
 *
 * <p>A structure set is placed by {@link RandomSpreadStructurePlacement}, whose own
 * {@code getPotentialStructureChunk} answers "which chunk would this grid cell pick". Walking grid cells
 * outwards and asking the placement that question is exact rather than probabilistic: it is the same
 * arithmetic the game used to decide where the structure could be. Each candidate is then checked for a
 * permitted biome at its centre before the chunks are generated, because a village whose centre chunk is
 * an ocean would be a probe defect rather than a generation finding.
 */
public final class VillageVerification {
    private VillageVerification() {
    }

    /** Registers the probe only when it was explicitly asked for. */
    public static void registerIfRequested() {
        if (Boolean.getBoolean("historical_experience.villageProbe")) {
            ServerLifecycleEvents.SERVER_STARTED.register(server -> server.execute(() -> run(server)));
        }
    }

    private static void run(MinecraftServer server) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("runToken", System.getProperty("historical_experience.villageProbe.token"));
        try {
            ServerLevel level = server.overworld();
            ServerChunkCache cache = level.getChunkSource();
            var generator = cache.getGenerator();
            String alias =
                net.historicalexperience.generation.HistoricalFeatureExclusions.historicalAlias(generator);
            report.put("alias", alias);
            report.put("generator", generator.getClass().getName());
            report.put("levelSeed", level.getSeed());
            report.put("structureTypesRegistered", HistoricalVillageTypes.isRegistered());
            report.put("villageVersions", HistoricalVillageRules.villageVersions());
            report.put("noVillageVersions", HistoricalVillageRules.noVillageVersions());
            report.put("permittedBiomes", HistoricalVillageRules.permittedBiomes().stream().sorted().toList());
            report.put("paletteBlocks", HistoricalVillageBlocks.placedBlockIds().stream().sorted().toList());
            report.put("declinedForBiome", HistoricalVillageStructure.declinedForBiomeCount());
            report.put("pathColumnsPlaced", HistoricalVillagePieces.VillagePiece.placedPathColumnCount());
            report.put("droppedUpstreamBlocks", HistoricalVillagePieces.VillagePiece.droppedUpstreamBlockCount());

            var lookup = level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET);
            var structures = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
            var placementHolder = lookup.getHolder(
                net.minecraft.resources.ResourceKey.create(Registries.STRUCTURE_SET,
                    HistoricalVillageTypes.structureSetId()));
            if (placementHolder.isEmpty()) {
                throw new IllegalStateException("the structure set "
                    + HistoricalVillageTypes.structureSetId() + " is not loaded, so the village data file "
                    + "did not parse or was not found");
            }
            var structureSet = placementHolder.get().value();
            report.put("structureSetLoaded", HistoricalVillageTypes.structureSetId().toString());
            report.put("structureSetStructures", structureSet.structures().stream()
                .map(entry -> structures.getKey(entry.structure().value()).toString()).toList());
            StructurePlacement placement = structureSet.placement();
            if (!(placement instanceof RandomSpreadStructurePlacement spread)) {
                throw new IllegalStateException("the structure set is not random_spread: " + placement);
            }
            report.put("spacing", spread.spacing());
            report.put("separation", spread.separation());

            var villageHolder = structures.getHolder(
                net.minecraft.resources.ResourceKey.create(Registries.STRUCTURE,
                    HistoricalVillageTypes.structureId()));
            if (villageHolder.isEmpty()) {
                throw new IllegalStateException("the structure "
                    + HistoricalVillageTypes.structureId() + " is not loaded");
            }
            Structure village = villageHolder.get().value();
            report.put("structureLoaded", HistoricalVillageTypes.structureId().toString());
            report.put("structureBiomeTagEntries", village.biomes().size());

            int maxCells = Integer.getInteger("historical_experience.villageProbe.cells", 24);
            long levelSeed = cache.getGeneratorState().getLevelSeed();
            ChunkPos found = null;
            String foundBiome = null;
            int examined = 0;
            int permitted = 0;
            outer:
            for (int ring = 0; ring <= maxCells; ring++) {
                for (int gridX = -ring; gridX <= ring; gridX++) {
                    for (int gridZ = -ring; gridZ <= ring; gridZ++) {
                        if (Math.max(Math.abs(gridX), Math.abs(gridZ)) != ring) {
                            continue;
                        }
                        examined++;
                        ChunkPos candidate = spread.getPotentialStructureChunk(levelSeed, gridX, gridZ);
                        if (candidate.x != gridX || candidate.z != gridZ) {
                            continue;
                        }
                        Holder<net.minecraft.world.level.biome.Biome> biome = level.getBiome(
                            new BlockPos(candidate.getMinBlockX() + 8, 64, candidate.getMinBlockZ() + 8));
                        String biomeId = biome.unwrapKey().map(key -> key.location().toString()).orElse("?");
                        if (!HistoricalVillageRules.biomeIsPermitted(biomeId)) {
                            continue;
                        }
                        permitted++;
                        // Generate the nine chunks the well and the four road stubs can reach.
                        for (int dx = -1; dx <= 1; dx++) {
                            for (int dz = -1; dz <= 1; dz++) {
                                cache.getChunk(candidate.x + dx, candidate.z + dz,
                                    net.minecraft.world.level.chunk.status.ChunkStatus.FULL, true);
                            }
                        }
                        LevelChunk centre = cache.getChunkNow(candidate.x, candidate.z);
                        if (centre == null) {
                            continue;
                        }
                        StructureStart start = centre.getStartForStructure(village);
                        if (start != null && start.isValid()) {
                            found = candidate;
                            foundBiome = biomeId;
                            break outer;
                        }
                    }
                }
            }
            report.put("gridCellsExamined", examined);
            report.put("permittedBiomeCandidates", permitted);
            if (found == null) {
                report.put("villageFound", false);
                report.put("success", false);
                report.put("failure", "no valid historical_village start in " + examined
                    + " grid cells (" + permitted + " were in a permitted biome)");
                return;
            }
            report.put("villageFound", true);
            report.put("villageChunk", found.x + "," + found.z);
            report.put("villageBiome", foundBiome);

            // Force-load the whole neighbourhood so the region file contains every piece, then read the
            // finished blocks back out of the generated chunks.
            StringBuilder area = new StringBuilder();
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (area.length() > 0) {
                        area.append(' ');
                    }
                    area.append(found.x + dx).append(' ').append(found.z + dz);
                }
            }
            int commandResult = server.getCommands().getDispatcher().execute(
                "forceload add " + area, server.createCommandSourceStack());
            report.put("forceloadCommandResult", commandResult);

            StructureStart start = cache.getChunkNow(found.x, found.z).getStartForStructure(village);
            ChunkPos startChunk = start.getChunkPos();
            report.put("structureStartChunk", startChunk.x + "," + startChunk.z);
            report.put("structureStartPieces", start.getPieces().size());
            report.put("structureBoundingBox", start.getBoundingBox().toString());
            java.util.List<String> pieceTypes = new java.util.ArrayList<>();
            for (var piece : start.getPieces()) {
                pieceTypes.add(piece.getClass().getSimpleName()
                    + (piece instanceof HistoricalVillagePieces.VillagePiece village2
                        ? "[type=" + village2.pieceType() + ",style=" + village2.villageStyle()
                            + ",version=" + village2.version().orElse("unset") + "]" : ""));
            }
            report.put("structurePieceTypes", pieceTypes);

            // The survey area: the structure's bounding box grown by 16 blocks, in chunk steps.
            var bb = start.getBoundingBox();
            int minChunkX = (bb.minX() - 16) >> 4;
            int maxChunkX = (bb.maxX() + 16) >> 4;
            int minChunkZ = (bb.minZ() - 16) >> 4;
            int maxChunkZ = (bb.maxZ() + 16) >> 4;
            report.put("surveyChunksX", maxChunkX - minChunkX + 1);
            report.put("surveyChunksZ", maxChunkZ - minChunkZ + 1);

            Map<String, Long> counts = new TreeMap<>();
            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                    LevelChunk chunk = cache.getChunkNow(chunkX, chunkZ);
                    if (chunk == null) {
                        continue;
                    }
                    for (int sectionY = chunk.getMinSection(); sectionY < chunk.getMaxSection(); sectionY++) {
                        var section = chunk.getSection(chunk.getSectionIndexFromSectionY(sectionY));
                        section.getStates().count((state, count) -> counts.merge(
                            BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(),
                            (long) count, Long::sum));
                    }
                }
            }
            Map<String, Long> villageBlocks = new TreeMap<>();
            java.util.Set<String> palette = HistoricalVillageBlocks.placedBlockIds();
            for (Map.Entry<String, Long> entry : counts.entrySet()) {
                if (palette.contains(entry.getKey())) {
                    villageBlocks.put(entry.getKey(), entry.getValue());
                }
            }
            report.put("surveyBlockCounts", villageBlocks);
            report.put("surveyDistinctPaletteBlocksFound", villageBlocks.size());
            report.put("surveyDistinctPaletteBlocksDeclared", palette.size());
            java.util.List<String> missing = palette.stream()
                .filter(id -> !villageBlocks.containsKey(id))
                .sorted()
                .toList();
            report.put("surveyPaletteBlocksNotPresent", missing);

            // Evidence that the historical version has not been violated: every block found near the
            // village is judged against this version's extracted palette.
            Map<String, String> verdicts = new TreeMap<>();
            for (String id : counts.keySet()) {
                if (java.util.Set.of("minecraft:air", "minecraft:cave_air", "minecraft:void_air",
                        "minecraft:light").contains(id)) {
                    continue;
                }
                String legacy = HistoricalBlockPalette.legacyNameFor(id);
                verdicts.put(id, legacy == null ? "UNMAPPED" : legacy);
            }
            report.put("surveyAllBlocksFound", counts);
            report.put("surveyLegacyNames", verdicts);

            server.saveEverything(true, true, true);
            report.put("success", true);
        } catch (Throwable failure) {
            report.put("success", false);
            report.put("failure", failure.toString());
            HistoricalExperienceMod.LOGGER.error("Village verification failed", failure);
        } finally {
            try {
                Files.writeString(
                    server.getWorldPath(LevelResource.ROOT).resolve("village-probe.json"),
                    new GsonBuilder().setPrettyPrinting().create().toJson(report));
            } catch (Exception failure) {
                HistoricalExperienceMod.LOGGER.error("Cannot retain village verification evidence", failure);
            }
            HistoricalExperienceMod.LOGGER.info("VILLAGE_VERIFICATION {}", report);
            server.halt(false);
        }
    }
}
