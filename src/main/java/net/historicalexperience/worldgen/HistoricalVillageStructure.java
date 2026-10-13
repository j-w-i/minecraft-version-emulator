/*
 * Ported from the NeoForge mod "OldVillages" (upstream file
 * src/main/java/cz/maxtechnik/old_villages/worldgen/OldVillageStructure.java).
 * Upstream repository: https://github.com/MaxTechnik/OldVillages (pinned revision
 * 7578013ec061303982539fac7606df67577ffb26).
 * Upstream declares "mod_license=MIT" in its gradle.properties. The reference checkout
 * ships no LICENSE file, so that gradle.properties declaration is the whole licence
 * statement available; it is recorded here rather than a licence file being claimed.
 * Licence: MIT.
 */
package net.historicalexperience.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.BlockColumn;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import org.jetbrains.annotations.NotNull;

/**
 * The pre-1.14 village: a well at the centre with up to four roads out of it, houses placed along those
 * roads, and the roads extended up to three times.
 *
 * <h2>What changed from upstream, and why</h2>
 *
 * <ul>
 *   <li><b>No config, no NeoForge.</b> Upstream reads its options from a NeoForge common config and its
 *       village style from the biome path. Here the version gate, the biome set and every block come from
 *       {@link HistoricalVillageRules} and {@link HistoricalVillageBlocks}.</li>
 *   <li><b>The biome is matched, not substring-searched.</b> Upstream's {@code path.contains("desert")}
 *       would make {@code minecraft:desert_lakes}, or any addon biome whose id contains "desert", a desert
 *       village. Only the three researched biomes pass, and a biome outside them produces no village.</li>
 *   <li><b>The version gate is here rather than only in the structure registry.</b> A version before Beta
 *       1.8.1 returns no generation point from this method, which is the same "nothing was generated"
 *       answer the game already uses for a structure that does not fit. The project's
 *       {@code StructureHistoricalSuppressionMixin} would also suppress it for those versions, because
 *       they allow no structure at all; asserting it here as well means the boundary holds even for a
 *       world whose generator does not carry the project's marker in the shape that hook expects, and the
 *       unit test can check the boundary without a Minecraft runtime.</li>
 * </ul>
 *
 * <p>The rest of the generation is upstream's algorithm, kept deliberately close to it: the same
 * 6x7x6 well, the same four road stubs, the same 40% house-per-slot chance, the same house size table and
 * the same up-to-three road extensions with the same quarter-turn roll.
 */
public class HistoricalVillageStructure extends Structure {
    public static final MapCodec<HistoricalVillageStructure> CODEC =
        RecordCodecBuilder.mapCodec(instance -> instance.group(settingsCodec(instance))
            .apply(instance, HistoricalVillageStructure::new));

    public HistoricalVillageStructure(StructureSettings settings) {
        super(settings);
    }

    /**
     * The ground height for a house, upstream's {@code getRealHouseHeight}.
     *
     * <p>A house whose column is below sea level sits on top of the water rather than at the terrain
     * height, unless the column two blocks under sea level is not water - upstream's way of not floating a
     * village over an ocean while still letting a lakeside village stand on the shore.
     */
    private static int getRealHouseHeight(GenerationContext context, int x, int z, int seaLevel) {
        int houseY = context.chunkGenerator().getFirstOccupiedHeight(
            x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
        if (houseY < seaLevel) {
            BlockColumn column =
                context.chunkGenerator().getBaseColumn(x, z, context.heightAccessor(), context.randomState());
            if (column.getBlock(seaLevel - 2).is(Blocks.WATER)) {
                return seaLevel - 1;
            }
        }
        return houseY;
    }

    @Override
    protected @NotNull Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        // The version gate, in BOTH directions, and the second direction is the one that was wrong.
        //
        //   * no historical marker at all - an ordinary vanilla world, or a plain Moderner Beta world that
        //     this project does not own - must generate NOTHING from this structure. The earlier version of
        //     this guard allowed exactly that case through, on the theory that the structure was only
        //     reachable through a datapack; it is not, because a structure set is global, so an ordinary
        //     world whose biomes match the tag would have received historical villages. Ordinary worlds are
        //     required to be unaffected, so a missing marker now refuses.
        //   * a historical version below Beta 1.8.1 had no villages at all.
        String alias = HistoricalVillageRules.aliasOf(context.chunkGenerator()).orElse(null);
        if (alias == null || !HistoricalVillageRules.generationEnabledForAlias(alias)) {
            return Optional.empty();
        }
        // The pieces persist the VERSION, not the recipe alias: two revisions of one version place the
        // same blocks, and a version id is what the block palette is keyed by.
        final String versionId = net.historicalexperience.version.HistoricalVersions
            .byRevisionAlias(alias)
            .map(net.historicalexperience.version.HistoricalVersion::id)
            .orElse(alias);

        ChunkPos chunkPos = context.chunkPos();
        int blockX = chunkPos.getMinBlockX() + 8;
        int blockZ = chunkPos.getMinBlockZ() + 8;

        // The biome is sampled at the SURFACE, not at sea level, and that distinction is the whole reason no
        // village ever placed. These profiles declare cave biomes (every settings preset here carries a
        // cave_provider_settings block), so a sample taken at the fixed sea-level Y can land on a CAVE biome,
        // biomeIsPermitted then refuses, and the structure returns empty for every candidate chunk. The village
        // is built at the surface, so the surface is the only Y whose biome decides anything.
        int surfaceY = context.chunkGenerator().getFirstFreeHeight(
            blockX, blockZ, Heightmap.Types.WORLD_SURFACE, context.heightAccessor(), context.randomState());
        Holder<Biome> biome = context.biomeSource().getNoiseBiome(
            QuartPos.fromBlock(blockX), QuartPos.fromBlock(Math.max(surfaceY, context.heightAccessor().getMinBuildHeight())),
            QuartPos.fromBlock(blockZ), context.randomState().sampler());
        String biomeId = biome.unwrapKey().map(key -> key.location().toString()).orElse(null);
        if (biomeId == null || !HistoricalVillageRules.biomeIsPermitted(biomeId)) {
            // Not a village biome: no village. Reported by the diagnostic so a world that generates none
            // can be told apart from one whose biomes are wrong, and exposed in the generation probe.
            NOT_A_VILLAGE_BIOME.increment();
            return Optional.empty();
        }

        int seaLevel = context.chunkGenerator().getSeaLevel();
        int height = getRealHouseHeight(context, blockX, blockZ, seaLevel);
        if (height <= context.heightAccessor().getMinBuildHeight()) {
            height = context.chunkGenerator().getFirstFreeHeight(
                blockX, blockZ, Heightmap.Types.WORLD_SURFACE, context.heightAccessor(), context.randomState());
        }
        BlockPos startPos = new BlockPos(blockX, height, blockZ);
        return Optional.of(new GenerationStub(startPos, builder ->
            generatePieces(builder, context, startPos, biomeId, versionId)));
    }

    private void generatePieces(
        StructurePiecesBuilder builder,
        GenerationContext context,
        BlockPos pos,
        String biomeId,
        String versionId
    ) {
        List<BoundingBox> placedBoxes = new ArrayList<>();
        RandomSource random = context.random();
        Direction wellDirection = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        int villageStyle = HistoricalVillageRules.isDesert(biomeId)
            ? HistoricalVillagePieces.STYLE_DESERT
            : HistoricalVillagePieces.STYLE_PLAINS;

        HistoricalVillagePieces.VillagePiece well = HistoricalVillagePieces.well(
            pos.getX(), pos.getY(), pos.getZ(), wellDirection, villageStyle, versionId);
        builder.addPiece(well);
        BoundingBox wellBox = well.getBoundingBox();
        placedBoxes.add(wellBox);

        int minX = wellBox.minX();
        int maxX = wellBox.maxX();
        int minZ = wellBox.minZ();
        int maxZ = wellBox.maxZ();
        int y = wellBox.minY();

        List<PathRecord> pathQueue = new ArrayList<>();
        List<HistoricalVillagePieces.VillagePiece> deferredPaths = new ArrayList<>();

        BoundingBox north = new BoundingBox(minX + 1, y - 30, minZ - 18, minX + 3, y + 30, minZ - 1);
        if (isAreaClear(placedBoxes, north)) {
            deferredPaths.add(HistoricalVillagePieces.path(north, Direction.NORTH, 1, villageStyle, versionId));
            placedBoxes.add(north);
            pathQueue.add(new PathRecord(north, Direction.NORTH, 1));
        }
        BoundingBox south = new BoundingBox(minX + 1, y - 30, maxZ + 1, minX + 3, y + 30, maxZ + 18);
        if (isAreaClear(placedBoxes, south)) {
            deferredPaths.add(HistoricalVillagePieces.path(south, Direction.SOUTH, 1, villageStyle, versionId));
            placedBoxes.add(south);
            pathQueue.add(new PathRecord(south, Direction.SOUTH, 1));
        }
        BoundingBox west = new BoundingBox(minX - 18, y - 30, minZ + 1, minX - 1, y + 30, minZ + 3);
        if (isAreaClear(placedBoxes, west)) {
            deferredPaths.add(HistoricalVillagePieces.path(west, Direction.WEST, 1, villageStyle, versionId));
            placedBoxes.add(west);
            pathQueue.add(new PathRecord(west, Direction.WEST, 1));
        }
        BoundingBox east = new BoundingBox(maxX + 1, y - 30, minZ + 1, maxX + 18, y + 30, minZ + 3);
        if (isAreaClear(placedBoxes, east)) {
            deferredPaths.add(HistoricalVillagePieces.path(east, Direction.EAST, 1, villageStyle, versionId));
            placedBoxes.add(east);
            pathQueue.add(new PathRecord(east, Direction.EAST, 1));
        }

        while (!pathQueue.isEmpty()) {
            PathRecord currentPath = pathQueue.removeFirst();
            placeHousesAlongPath(context, builder, placedBoxes, currentPath.box(), currentPath.dir(),
                random, villageStyle, versionId);
            if (currentPath.depth() >= 3) {
                continue;
            }
            float roll = random.nextFloat();
            List<Direction> nextDirections = new ArrayList<>();
            if (roll < 0.25F) {
                nextDirections.add(currentPath.dir());
            } else if (roll < 0.55F) {
                nextDirections.add(currentPath.dir().getCounterClockWise());
            } else if (roll < 0.85F) {
                nextDirections.add(currentPath.dir().getClockWise());
            } else if (roll < 0.95F) {
                nextDirections.add(currentPath.dir().getCounterClockWise());
                nextDirections.add(currentPath.dir().getClockWise());
            }
            for (Direction nextDir : nextDirections) {
                int nextLength = random.nextInt(7) + 10;
                BoundingBox nextPathBox =
                    createNextPathBox(currentPath.box(), currentPath.dir(), nextDir, nextLength);
                if (isAreaClear(placedBoxes, nextPathBox)) {
                    deferredPaths.add(HistoricalVillagePieces.path(
                        nextPathBox, nextDir, currentPath.depth() + 1, villageStyle, versionId));
                    placedBoxes.add(nextPathBox);
                    pathQueue.add(new PathRecord(nextPathBox, nextDir, currentPath.depth() + 1));
                }
            }
        }

        // Paths are added after every house has been placed, so a road piece can never write over a
        // building that was placed after it in the list. Same ordering as upstream.
        for (HistoricalVillagePieces.VillagePiece pathPiece : deferredPaths) {
            builder.addPiece(pathPiece);
        }
    }

    private static void placeHousesAlongPath(
        GenerationContext context,
        StructurePiecesBuilder builder,
        List<BoundingBox> placedBoxes,
        BoundingBox pathBox,
        Direction pathDir,
        RandomSource random,
        int villageStyle,
        String versionId
    ) {
        int seaLevel = context.chunkGenerator().getSeaLevel();
        boolean alongZ = pathDir == Direction.NORTH || pathDir == Direction.SOUTH;
        if (alongZ) {
            // East side of the road.
            int cursor = pathBox.minZ() + 1;
            while (cursor < pathBox.maxZ()) {
                int[] choice = chooseHouse(random, true);
                int type = choice[0];
                int sizeX = choice[1];
                int sizeZ = choice[2];
                if (cursor + sizeZ > pathBox.maxZ()) {
                    type = random.nextBoolean()
                        ? HistoricalVillagePieces.PIECE_SHACK
                        : HistoricalVillagePieces.PIECE_SHACK_ROOF;
                    sizeX = 5;
                    sizeZ = 4;
                    if (cursor + sizeZ > pathBox.maxZ()) {
                        break;
                    }
                }
                if (random.nextFloat() < 0.40F) {
                    int houseY = getRealHouseHeight(context, pathBox.minX(), cursor, seaLevel);
                    buildAbsoluteHouse(builder, placedBoxes,
                        pathBox.minX() - sizeX, houseY, cursor,
                        pathBox.minX() - 1, houseY + 12, cursor + sizeZ - 1,
                        Direction.EAST, type, villageStyle, versionId);
                    cursor += sizeZ + random.nextInt(4) + 4;
                } else {
                    cursor += random.nextInt(4) + 3;
                }
            }
            // West side of the road.
            cursor = pathBox.minZ() + 1;
            while (cursor < pathBox.maxZ()) {
                int[] choice = chooseHouse(random, true);
                int type = choice[0];
                int sizeX = choice[1];
                int sizeZ = choice[2];
                if (cursor + sizeZ > pathBox.maxZ()) {
                    type = random.nextBoolean()
                        ? HistoricalVillagePieces.PIECE_SHACK
                        : HistoricalVillagePieces.PIECE_SHACK_ROOF;
                    sizeX = 5;
                    sizeZ = 4;
                    if (cursor + sizeZ > pathBox.maxZ()) {
                        break;
                    }
                }
                if (random.nextFloat() < 0.40F) {
                    int houseY = getRealHouseHeight(context, pathBox.maxX(), cursor, seaLevel);
                    buildAbsoluteHouse(builder, placedBoxes,
                        pathBox.maxX() + 1, houseY, cursor,
                        pathBox.maxX() + sizeX, houseY + 12, cursor + sizeZ - 1,
                        Direction.WEST, type, villageStyle, versionId);
                    cursor += sizeZ + random.nextInt(4) + 4;
                } else {
                    cursor += random.nextInt(4) + 3;
                }
            }
            return;
        }
        // East/west road: the houses are placed on the south and north sides, and their X/Z sizes swap.
        int cursor = pathBox.minX() + 1;
        while (cursor < pathBox.maxX()) {
            int[] choice = chooseHouse(random, false);
            int type = choice[0];
            int sizeX = choice[1];
            int sizeZ = choice[2];
            if (cursor + sizeX > pathBox.maxX()) {
                type = random.nextBoolean()
                    ? HistoricalVillagePieces.PIECE_SHACK
                    : HistoricalVillagePieces.PIECE_SHACK_ROOF;
                sizeX = 4;
                sizeZ = 5;
                if (cursor + sizeX > pathBox.maxX()) {
                    break;
                }
            }
            if (random.nextFloat() < 0.40F) {
                int houseY = getRealHouseHeight(context, cursor, pathBox.minZ(), seaLevel);
                buildAbsoluteHouse(builder, placedBoxes,
                    cursor, houseY, pathBox.minZ() - sizeZ,
                    cursor + sizeX - 1, houseY + 12, pathBox.minZ() - 1,
                    Direction.SOUTH, type, villageStyle, versionId);
                cursor += sizeX + random.nextInt(4) + 4;
            } else {
                cursor += random.nextInt(4) + 3;
            }
        }
        cursor = pathBox.minX() + 1;
        while (cursor < pathBox.maxX()) {
            int[] choice = chooseHouse(random, false);
            int type = choice[0];
            int sizeX = choice[1];
            int sizeZ = choice[2];
            if (cursor + sizeX > pathBox.maxX()) {
                type = random.nextBoolean()
                    ? HistoricalVillagePieces.PIECE_SHACK
                    : HistoricalVillagePieces.PIECE_SHACK_ROOF;
                sizeX = 4;
                sizeZ = 5;
                if (cursor + sizeX > pathBox.maxX()) {
                    break;
                }
            }
            if (random.nextFloat() < 0.40F) {
                int houseY = getRealHouseHeight(context, cursor, pathBox.maxZ(), seaLevel);
                buildAbsoluteHouse(builder, placedBoxes,
                    cursor, houseY, pathBox.maxZ() + 1,
                    cursor + sizeX - 1, houseY + 12, pathBox.maxZ() + sizeZ,
                    Direction.NORTH, type, villageStyle, versionId);
                cursor += sizeX + random.nextInt(4) + 4;
            } else {
                cursor += random.nextInt(4) + 3;
            }
        }
    }

    /**
     * Upstream's house table.
     *
     * <p>Type 12 is the library and type 8 the tavern, which are the two pieces whose interiors were full
     * of 1.14 workstations; they are kept, with those blocks substituted, because a village without a
     * library is a different village rather than a more historical one.
     *
     * @param alongZ whether the road runs north/south, which swaps the building's X and Z extents
     * @return {@code [pieceType, sizeX, sizeZ]}
     */
    private static int[] chooseHouse(RandomSource random, boolean alongZ) {
        int roll = random.nextInt(100);
        if (roll < 30) {
            return new int[] {
                random.nextBoolean() ? HistoricalVillagePieces.PIECE_SMALL_HOUSE
                    : HistoricalVillagePieces.PIECE_SMALL_HOUSE_FENCE,
                5, 5};
        }
        if (roll < 42) {
            return new int[] {
                random.nextBoolean() ? HistoricalVillagePieces.PIECE_SHACK
                    : HistoricalVillagePieces.PIECE_SHACK_ROOF,
                alongZ ? 5 : 4, alongZ ? 4 : 5};
        }
        if (roll < 54) {
            return new int[] {HistoricalVillagePieces.PIECE_LARGE_HOUSE, alongZ ? 11 : 9, alongZ ? 9 : 11};
        }
        if (roll < 64) {
            return new int[] {HistoricalVillagePieces.PIECE_FARM, alongZ ? 9 : 7, alongZ ? 7 : 9};
        }
        if (roll < 74) {
            return new int[] {HistoricalVillagePieces.PIECE_LARGE_FARM, alongZ ? 9 : 13, alongZ ? 13 : 9};
        }
        if (roll < 84) {
            return new int[] {HistoricalVillagePieces.PIECE_BLACKSMITH, alongZ ? 7 : 10, alongZ ? 10 : 7};
        }
        if (roll < 90) {
            return new int[] {HistoricalVillagePieces.PIECE_LIBRARY, alongZ ? 6 : 9, alongZ ? 9 : 6};
        }
        if (roll < 95) {
            return new int[] {HistoricalVillagePieces.PIECE_TAVERN, alongZ ? 11 : 9, alongZ ? 9 : 11};
        }
        return new int[] {HistoricalVillagePieces.PIECE_CHURCH, alongZ ? 9 : 5, alongZ ? 5 : 9};
    }

    /** Upstream's {@code createNextPathBox}: the next road, turned off the current one. */
    private static BoundingBox createNextPathBox(
        BoundingBox parent,
        Direction parentDir,
        Direction nextDir,
        int length
    ) {
        int minY = parent.minY();
        int maxY = parent.maxY();
        if (parentDir == Direction.NORTH) {
            if (nextDir == Direction.NORTH) {
                return new BoundingBox(parent.minX(), minY, parent.minZ() - length,
                    parent.maxX(), maxY, parent.minZ() - 1);
            }
            if (nextDir == Direction.WEST) {
                return new BoundingBox(parent.minX() - length, minY, parent.minZ(),
                    parent.minX() - 1, maxY, parent.minZ() + 2);
            }
            if (nextDir == Direction.EAST) {
                return new BoundingBox(parent.maxX() + 1, minY, parent.minZ(),
                    parent.maxX() + length, maxY, parent.minZ() + 2);
            }
        }
        if (parentDir == Direction.SOUTH) {
            if (nextDir == Direction.SOUTH) {
                return new BoundingBox(parent.minX(), minY, parent.maxZ() + 1,
                    parent.maxX(), maxY, parent.maxZ() + length);
            }
            if (nextDir == Direction.WEST) {
                return new BoundingBox(parent.minX() - length, minY, parent.maxZ() - 2,
                    parent.minX() - 1, maxY, parent.maxZ());
            }
            if (nextDir == Direction.EAST) {
                return new BoundingBox(parent.maxX() + 1, minY, parent.maxZ() - 2,
                    parent.maxX() + length, maxY, parent.maxZ());
            }
        }
        if (parentDir == Direction.WEST) {
            if (nextDir == Direction.WEST) {
                return new BoundingBox(parent.minX() - length, minY, parent.minZ(),
                    parent.minX() - 1, maxY, parent.maxZ());
            }
            if (nextDir == Direction.NORTH) {
                return new BoundingBox(parent.minX(), minY, parent.minZ() - length,
                    parent.minX() + 2, maxY, parent.minZ() - 1);
            }
            if (nextDir == Direction.SOUTH) {
                return new BoundingBox(parent.minX(), minY, parent.maxZ() + 1,
                    parent.minX() + 2, maxY, parent.maxZ() + length);
            }
        }
        if (parentDir == Direction.EAST) {
            if (nextDir == Direction.EAST) {
                return new BoundingBox(parent.maxX() + 1, minY, parent.minZ(),
                    parent.maxX() + length, maxY, parent.maxZ());
            }
            if (nextDir == Direction.NORTH) {
                return new BoundingBox(parent.maxX() - 2, minY, parent.minZ() - length,
                    parent.maxX(), maxY, parent.minZ() - 1);
            }
            if (nextDir == Direction.SOUTH) {
                return new BoundingBox(parent.maxX() - 2, minY, parent.maxZ() + 1,
                    parent.maxX(), maxY, parent.maxZ() + length);
            }
        }
        return parent;
    }

    private static void buildAbsoluteHouse(
        StructurePiecesBuilder builder,
        List<BoundingBox> placedBoxes,
        int minX, int minY, int minZ,
        int maxX, int maxY, int maxZ,
        Direction facing,
        int pieceType,
        int villageStyle,
        String versionId
    ) {
        BoundingBox houseBox = new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
        if (isAreaClear(placedBoxes, houseBox)) {
            builder.addPiece(HistoricalVillagePieces.house(
                pieceType, houseBox, facing, villageStyle, versionId));
            placedBoxes.add(houseBox);
        }
    }

    private static boolean isAreaClear(List<BoundingBox> boxes, BoundingBox targetBox) {
        for (BoundingBox box : boxes) {
            if (box.intersects(targetBox)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public @NotNull StructureType<?> type() {
        return HistoricalVillageTypes.HISTORICAL_VILLAGE;
    }

    /** Villages declined because the centre chunk's biome is not a village biome. Diagnostic only. */
    private static final java.util.concurrent.atomic.LongAdder NOT_A_VILLAGE_BIOME =
        new java.util.concurrent.atomic.LongAdder();

    /** How many centre chunks this structure has declined for a non-village biome since process start. */
    public static long declinedForBiomeCount() {
        return NOT_A_VILLAGE_BIOME.sum();
    }

    private record PathRecord(BoundingBox box, Direction dir, int depth) {
    }
}
