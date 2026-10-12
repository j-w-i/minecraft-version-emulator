/*
 * Ported from the NeoForge mod "OldVillages" (upstream file
 * src/main/java/cz/maxtechnik/old_villages/worldgen/OldVillagePieces.java).
 * Upstream repository: https://github.com/MaxTechnik/OldVillages (pinned revision
 * 7578013ec061303982539fac7606df67577ffb26).
 * Upstream declares "mod_license=MIT" in its gradle.properties. The reference checkout
 * ships no LICENSE file, so that gradle.properties declaration is the whole licence
 * statement available; it is recorded here rather than a licence file being claimed.
 * Licence: MIT.
 */
package net.historicalexperience.worldgen;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.NotNull;

/**
 * The village pieces themselves: the well, the roads, and the twelve buildings the roads place.
 *
 * <h2>What changed from upstream, and what did not</h2>
 *
 * <p>The geometry is upstream's, block for block, including its quirks - the blacksmith's lava pit, the
 * library's roof steps that reach one block outside its bounding box, the tavern's two doors. What changed
 * is everything that would put a block into a version that did not have it:
 *
 * <ul>
 *   <li>Every block state comes from {@link Style}, which is built from
 *       {@link HistoricalVillageBlocks#forVersion} for the version the world was created as.</li>
 *   <li>The version is stored in the piece's own NBT. The version a village was built as has to survive a
 *       save and a reload, and reading it from the piece rather than from ambient state means a village
 *       keeps the blocks of its own era even in a world that has since been upgraded to a later profile.</li>
 *   <li>The 1.14 workstations are the crafting table, furnace and bookshelf they replaced; the smooth and
 *       chiseled slabs are stone and sandstone slabs; the blacksmith's forge rail is a fence; and the crops
 *       are limited to what the version grew. See {@link HistoricalVillageBlocks} for the evidence.</li>
 *   <li><b>No entities.</b> Upstream spawns villagers in five of the pieces. Villagers are Beta 1.9
 *       Prerelease content and this structure generates from Beta 1.8.1, so a villager in a Beta 1.8.1
 *       village would be an anachronism; rather than make entity spawning conditional, this port places no
 *       entities at all. That is a deliberate, recorded reduction - see {@link HistoricalVillageRules}.</li>
 *   <li><b>No heightmap lookup through the level.</b> Upstream calls {@code level.getHeight(Heightmap.Types,
 *       x, z)}, which no longer exists on {@code WorldGenLevel} in 1.21.1. The surface is found by walking
 *       the column down from the piece's own top instead, which is bounded by the piece box and therefore
 *       cannot wander into another structure's terrain.</li>
 * </ul>
 */
public final class HistoricalVillagePieces {
    /** The well. Upstream's piece type 0. */
    public static final int PIECE_WELL = 0;
    /** A road. Upstream's piece type 1. */
    public static final int PIECE_PATH = 1;
    /** A small house. Upstream's piece type 2. */
    public static final int PIECE_SMALL_HOUSE = 2;
    /** A small house with a fenced roof. Upstream's piece type 3. */
    public static final int PIECE_SMALL_HOUSE_FENCE = 3;
    /** The large house with beds. Upstream's piece type 4. */
    public static final int PIECE_LARGE_HOUSE = 4;
    /** A farm. Upstream's piece type 5. */
    public static final int PIECE_FARM = 5;
    /** A double farm. Upstream's piece type 6. */
    public static final int PIECE_LARGE_FARM = 6;
    /** The blacksmith, with its lava pit and loot chest. Upstream's piece type 7. */
    public static final int PIECE_BLACKSMITH = 7;
    /** The tavern. Upstream's piece type 8. */
    public static final int PIECE_TAVERN = 8;
    /** The church. Upstream's piece type 9. */
    public static final int PIECE_CHURCH = 9;
    /** A shack. Upstream's piece type 10. */
    public static final int PIECE_SHACK = 10;
    /** A shack with a raised roof. Upstream's piece type 11. */
    public static final int PIECE_SHACK_ROOF = 11;
    /** The library. Upstream's piece type 12. */
    public static final int PIECE_LIBRARY = 12;

    /** The village style whose walls are sandstone. The other style is plains. */
    public static final int STYLE_DESERT = 1;
    /** The plains style: oak planks and cobblestone. */
    public static final int STYLE_PLAINS = 0;

    /** The loot table a blacksmith chest uses from Release 1.6.1 onward. */
    private static final ResourceKey<LootTable> BLACKSMITH_LOOT_TABLE = ResourceKey.create(
        Registries.LOOT_TABLE,
        ResourceLocation.fromNamespaceAndPath("historical_experience",
            "chests/historical_village_blacksmith"));

    /**
     * The loot table a blacksmith chest uses before Release 1.6.1.
     *
     * <p>Two tables rather than one because the only interesting difference is horse armour, which is
     * Release 1.6.1 content, and a chest's contents are the one place a block census cannot see an
     * anachronism. The older table is upstream's list with the three horse armours removed.
     */
    private static final ResourceKey<LootTable> BLACKSMITH_LOOT_TABLE_EARLY = ResourceKey.create(
        Registries.LOOT_TABLE,
        ResourceLocation.fromNamespaceAndPath("historical_experience",
            "chests/historical_village_blacksmith_early"));

    private HistoricalVillagePieces() {
    }

    /** The centre well: a 6x7x6 box, upstream's {@code new VillagePiece(0, 0, x, y, z, 6, 7, 6, ...)}. */
    public static VillagePiece well(
        int x, int y, int z, Direction orientation, int villageStyle, String versionId
    ) {
        return new VillagePiece(PIECE_WELL, 0, x, y, z, 6, 7, 6, orientation, villageStyle, versionId);
    }

    /** A road piece with an already-computed bounding box. */
    public static VillagePiece path(
        BoundingBox box, Direction orientation, int depth, int villageStyle, String versionId
    ) {
        return new VillagePiece(PIECE_PATH, depth, box, orientation, villageStyle, versionId);
    }

    /** A building piece with an already-computed absolute bounding box. */
    public static VillagePiece house(
        int pieceType,
        BoundingBox box,
        Direction orientation,
        int villageStyle,
        String versionId
    ) {
        return new VillagePiece(pieceType, 1, box, orientation, villageStyle, versionId);
    }

    /**
     * The resolved block states one piece of one version places.
     *
     * <p>A thin translation of the id table: no version decisions live here, which is what lets the palette
     * test run without Minecraft.
     */
    public static final class Style {
        private final Map<String, BlockState> states = new LinkedHashMap<>();
        private final boolean desert;

        private Style(HistoricalVillageBlocks.VillageBlockSet set, boolean desert) {
            this.desert = desert;
            Map<String, String> slots = new LinkedHashMap<>(set.blocks());
            if (desert) {
                slots.putAll(HistoricalVillageBlocks.desertSubstitutions(set));
            }
            for (Map.Entry<String, String> entry : slots.entrySet()) {
                if (entry.getKey().startsWith("crop:")) {
                    continue;
                }
                states.put(entry.getKey(), HistoricalVillageStates.state(entry.getValue()));
            }
        }

        /** The state in a slot, or a failure naming the slot. */
        public BlockState get(String slot) {
            BlockState state = states.get(slot);
            if (state == null) {
                throw new IllegalStateException("the village style has no block for slot '" + slot
                    + "'; the slots are " + states.keySet());
            }
            return state;
        }

        /** Whether this style is the desert one, for the diagnostics. */
        public boolean desert() {
            return desert;
        }

        public BlockState planks() {
            return get("planks");
        }

        public BlockState stairs() {
            return get("stairs");
        }

        public BlockState log() {
            return get("log");
        }

        public BlockState door() {
            return get("door");
        }

        public BlockState fence() {
            return get("fence");
        }

        public BlockState pressurePlate() {
            return get("pressurePlate");
        }

        public BlockState pathSurface() {
            return get("pathSurface");
        }

        public BlockState cobble() {
            return get("cobble");
        }

        public BlockState cobbleStairs() {
            return get("cobbleStairs");
        }

        public BlockState stoneSlab() {
            return get("stoneSlab");
        }

        public BlockState sandstone() {
            return get("sandstone");
        }

        public BlockState sandstoneStairs() {
            return get("sandstoneStairs");
        }

        public BlockState sandstoneSlab() {
            return get("sandstoneSlab");
        }

        public BlockState wool() {
            return get("wool");
        }

        public BlockState glassPane() {
            return get("glassPane");
        }

        public BlockState dirt() {
            return get("dirt");
        }

        public BlockState grass() {
            return get("grass");
        }

        public BlockState shortGrass() {
            return get("shortGrass");
        }

        public BlockState farmland() {
            return get("farmland");
        }

        public BlockState wheat() {
            return get("crop:wheat");
        }

        /** Null when this version grew no carrots, so a caller has to decide what to do instead. */
        public BlockState carrots() {
            return states.get("crop:carrots");
        }

        /** Null when this version grew no potatoes. */
        public BlockState potatoes() {
            return states.get("crop:potatoes");
        }

        public BlockState chest() {
            return get("chest");
        }

        public BlockState furnace() {
            return get("furnace");
        }

        public BlockState craftingTable() {
            return get("craftingTable");
        }

        public BlockState bookshelf() {
            return get("bookshelf");
        }

        /** The church's workstation: a brewing stand from Beta 1.9 Prerelease 3, a crafting table before. */
        public BlockState workstation() {
            return get(HistoricalVillageBlocks.WORKSTATION_SLOT);
        }

        public BlockState bed() {
            return get("bed");
        }

        public BlockState ladder() {
            return get("ladder");
        }

        public BlockState torch() {
            return get("torch");
        }

        public BlockState wallTorch() {
            return get("wallTorch");
        }

        public BlockState water() {
            return net.minecraft.world.level.block.Blocks.WATER.defaultBlockState();
        }

        /** Air, for the fill-with-air steps. */
        public BlockState air() {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }

        /** Lava, for the blacksmith's forge. */
        public BlockState lava() {
            return net.minecraft.world.level.block.Blocks.LAVA.defaultBlockState();
        }
    }

    /** Builds a style for a version and village style, for the structure to hand to its pieces. */
    static Style styleFor(String versionId, int villageStyle) {
        return new Style(HistoricalVillageBlocks.forVersion(versionId),
            villageStyle == STYLE_DESERT);
    }

    /** One village piece. One class rather than thirteen, as upstream has it. */
    public static class VillagePiece extends StructurePiece {
        /** The piece type, upstream's {@code pieceType}. */
        private final int pieceType;
        /** The locked village style, upstream's {@code villageStyle}. */
        private final int villageStyle;
        /** The version whose blocks this piece places, so a reload cannot change its era. */
        private String versionId;
        /** The resolved styles, built once from {@link #versionId} and {@link #villageStyle}. */
        private Style plainsStyle;
        private Style desertStyle;

        public VillagePiece(
            int pieceType,
            int genDepth,
            int x, int y, int z,
            int sizeX, int sizeY, int sizeZ,
            Direction orientation,
            int villageStyle,
            String versionId
        ) {
            this(pieceType, genDepth,
                BoundingBox.orientBox(x, y, z, 0, 0, 0, sizeX, sizeY, sizeZ, orientation),
                orientation, villageStyle, versionId);
        }

        public VillagePiece(
            int pieceType,
            int genDepth,
            BoundingBox box,
            Direction orientation,
            int villageStyle,
            String versionId
        ) {
            super(HistoricalVillageTypes.HISTORICAL_VILLAGE_PIECE, genDepth, box);
            this.pieceType = pieceType;
            this.villageStyle = villageStyle;
            lockVersion(versionId);
            this.setOrientation(orientation);
        }

        /**
         * Rebuilds a piece from saved NBT.
         *
         * <p>The bounding box, the generation depth and the orientation are read by
         * {@code StructurePiece(StructurePieceType, CompoundTag)} itself - that is the whole point of that
         * constructor - so this only has to restore what this port added: the piece type, the village style
         * and the version whose blocks the piece places.
         */
        public VillagePiece(StructurePieceType type, CompoundTag tag) {
            super(type, tag);
            this.pieceType = tag.getInt("PieceType");
            this.villageStyle = tag.getInt("VillageStyle");
            lockVersion(tag.getString("HexVersion"));
        }

        /** Records the version and resolves the two styles from it. Null means "no palette yet". */
        private void lockVersion(String versionId) {
            this.versionId = versionId == null || versionId.isEmpty() ? null : versionId;
            if (this.versionId == null) {
                this.plainsStyle = null;
                this.desertStyle = null;
            } else {
                this.plainsStyle = styleFor(this.versionId, STYLE_PLAINS);
                this.desertStyle = styleFor(this.versionId, STYLE_DESERT);
            }
        }

        /**
         * The style this piece places with.
         *
         * <p>The desert style is a substitution over the plains palette, so the plains style is always
         * built; keeping both means the loaded-from-NBT path is identical to the generated one.
         */
        private Style style() {
            if (plainsStyle == null) {
                throw new IllegalStateException("village piece " + pieceType + " has no block palette: it "
                    + "was neither created with a historical version nor loaded with one in its NBT");
            }
            return villageStyle == STYLE_DESERT ? desertStyle : plainsStyle;
        }

        /** The version this village was built as, or empty for a piece saved before the field existed. */
        public java.util.Optional<String> version() {
            return versionId == null || versionId.isEmpty()
                ? java.util.Optional.empty()
                : java.util.Optional.of(versionId);
        }

        public int pieceType() {
            return pieceType;
        }

        public int villageStyle() {
            return villageStyle;
        }

        @Override
        protected void addAdditionalSaveData(
            @NotNull StructurePieceSerializationContext context,
            @NotNull CompoundTag tag
        ) {
            tag.putInt("PieceType", this.pieceType);
            tag.putInt("VillageStyle", this.villageStyle);
            // The version has to be persisted: a chunk generated as Beta 1.8.1 and re-placed after an
            // upgrade must still build a Beta 1.8.1 village.
            tag.putString("HexVersion", this.versionId == null ? "" : this.versionId);
        }

        // ---- helpers ---------------------------------------------------------------------------

        private void fillWithBlocks(
            WorldGenLevel level, BoundingBox box,
            int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
            BlockState blockState
        ) {
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        this.placeBlock(level, blockState, x, y, z, box);
                    }
                }
            }
        }

        /**
         * Finds the surface beside a piece's own base.
         *
         * <p>Upstream used the level's WORLD_SURFACE_WG heightmap. {@code WorldGenLevel} no longer exposes
         * that in 1.21.1, and the piece's own bounding box already says how deep the terrain may be probed,
         * so the column is walked down from the top of the piece until something solid appears.
         */
        private static int surfaceY(WorldGenLevel level, LevelHeightAccessor limits, int x, int z, int startY) {
            int y = Math.min(startY, limits.getMaxBuildHeight() - 1);
            int floor = limits.getMinBuildHeight();
            while (y > floor) {
                BlockState state = level.getBlockState(new BlockPos(x, y, z));
                if (!state.isAir() && !state.canBeReplaced()) {
                    return y;
                }
                y--;
            }
            return floor;
        }

        private void placeChest(
            WorldGenLevel level, BoundingBox box,
            int x, int y, int z, Direction facing,
            ResourceKey<LootTable> lootTable
        ) {
            BlockPos worldPos = this.getWorldPos(x, y, z);
            BlockState chestState = style().chest().setValue(ChestBlock.FACING, facing);
            this.placeBlock(level, chestState, x, y, z, box);
            if (level.getBlockEntity(worldPos) instanceof ChestBlockEntity chest) {
                // A stable seed from the world seed and the chest's own position, so the same chest contains
                // the same loot every time the chunk is re-placed.
                long stableChestSeed = level.getSeed() ^ worldPos.asLong();
                chest.setLootTable(lootTable);
                chest.setLootTableSeed(stableChestSeed);
            }
        }

        // ---- the pieces ------------------------------------------------------------------------

        private void generateWell(WorldGenLevel level, BoundingBox box) {
            Style style = style();
            BlockState cobble = style.cobble();
            this.fillWithBlocks(level, box, 0, 1, 0, 5, 4, 5, style.air());
            for (int x = 0; x <= 5; x++) {
                for (int z = 0; z <= 5; z++) {
                    this.placeBlock(level, cobble, x, 0, z, box);
                    for (int y = -1; y >= -40; y--) {
                        BlockState under = this.getBlock(level, x, y, z, box);
                        if (isSoftForFoundations(under)) {
                            this.placeBlock(level, cobble, x, y, z, box);
                        } else {
                            break;
                        }
                    }
                }
            }
            this.fillWithBlocks(level, box, 1, -2, 1, 4, -2, 4, cobble);
            for (int y = -1; y <= 1; y++) {
                for (int i = 1; i <= 4; ++i) {
                    this.placeBlock(level, cobble, i, y, 1, box);
                    this.placeBlock(level, cobble, i, y, 4, box);
                    this.placeBlock(level, cobble, 1, y, i, box);
                    this.placeBlock(level, cobble, 4, y, i, box);
                }
            }
            this.placeBlock(level, cobble, 2, 0, 2, box);
            this.placeBlock(level, cobble, 3, 0, 2, box);
            this.placeBlock(level, cobble, 2, 0, 3, box);
            this.placeBlock(level, cobble, 3, 0, 3, box);
            this.fillWithBlocks(level, box, 2, -1, 2, 3, 0, 3, style.water());
            BlockState fence = style.fence();
            this.placeBlock(level, fence, 1, 2, 1, box);
            this.placeBlock(level, fence, 1, 2, 4, box);
            this.placeBlock(level, fence, 4, 2, 1, box);
            this.placeBlock(level, fence, 4, 2, 4, box);
            this.placeBlock(level, fence, 1, 3, 1, box);
            this.placeBlock(level, fence, 1, 3, 4, box);
            this.placeBlock(level, fence, 4, 3, 1, box);
            this.placeBlock(level, fence, 4, 3, 4, box);
            this.fillWithBlocks(level, box, 1, 4, 1, 4, 4, 4, cobble);
            // Upstream put a bell at 1,2,2 on the well's rim. A bell is Release 1.14 and nothing in this era
            // filled that role on a well, so the block is not placed. This is the one upstream block that is
            // dropped rather than substituted, and the count makes the difference visible in a report.
            droppedUpstreamBlocks.increment();
        }

        private static boolean isSoftForFoundations(BlockState state) {
            return state.isAir()
                || state.is(net.minecraft.world.level.block.Blocks.WATER)
                || state.is(net.minecraft.world.level.block.Blocks.LAVA)
                || state.is(net.minecraft.world.level.block.Blocks.SHORT_GRASS)
                || state.is(net.minecraft.world.level.block.Blocks.TALL_GRASS)
                || state.is(net.minecraft.world.level.block.Blocks.SNOW)
                || state.is(net.minecraft.world.level.block.Blocks.SPRUCE_LEAVES)
                || state.is(net.minecraft.world.level.block.Blocks.OAK_LEAVES)
                || state.is(net.minecraft.world.level.block.Blocks.SPRUCE_LOG)
                || state.is(net.minecraft.world.level.block.Blocks.OAK_LOG);
        }

        private void createBase(
            WorldGenLevel level, BoundingBox box,
            int minX, int minZ, int maxX, int maxZ, BlockState blockState
        ) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    for (int yLoc = 0; yLoc >= -200; yLoc--) {
                        BlockState state = this.getBlock(level, x, yLoc, z, box);
                        if (state.isAir()
                            || state.is(net.minecraft.world.level.block.Blocks.SHORT_GRASS)
                            || state.is(net.minecraft.world.level.block.Blocks.TALL_GRASS)
                            || state.is(net.minecraft.world.level.block.Blocks.OAK_LEAVES)
                            || state.is(net.minecraft.world.level.block.Blocks.SPRUCE_LEAVES)
                            || state.is(net.minecraft.world.level.block.Blocks.WATER)) {
                            this.placeBlock(level, blockState, x, yLoc, z, box);
                        } else {
                            break;
                        }
                    }
                }
            }
        }

        private void createBaseStairs(WorldGenLevel level, BoundingBox box, int x, int z) {
            BlockState cobble = style().cobble();
            for (int yLoc = 0; yLoc >= -200; yLoc--) {
                BlockState state = this.getBlock(level, x, yLoc, z, box);
                if (state.isAir()
                    || state.is(net.minecraft.world.level.block.Blocks.SHORT_GRASS)
                    || state.is(net.minecraft.world.level.block.Blocks.TALL_GRASS)) {
                    this.placeBlock(level, cobble, x, yLoc, z, box);
                } else {
                    break;
                }
            }
        }

        /**
         * The roads.
         *
         * <p>Upstream re-reads the world's surface for every column of the road with a heightmap and then
         * walks down to the first solid block. Here the surface is found by walking the column down from the
         * piece's own top, which is bounded by the piece box and never depends on a heightmap being primed
         * for a neighbouring chunk. The lamp posts are kept, with black wool instead of the modern lantern.
         */
        private void generatePath(WorldGenLevel level, BoundingBox box) {
            Style style = style();
            BoundingBox pieceBox = this.getBoundingBox();
            BlockState surface = style.pathSurface();
            int topY = pieceBox.maxY();
            int steps = 0;
            for (int x = pieceBox.minX(); x <= pieceBox.maxX(); x++) {
                for (int z = pieceBox.minZ(); z <= pieceBox.maxZ(); z++) {
                    int surfaceY = surfaceY(level, level, x, z, topY) + 1;
                    BlockPos pathPos = new BlockPos(x, surfaceY, z);
                    if (!box.isInside(pathPos)) {
                        continue;
                    }
                    steps++;
                    BlockState current = level.getBlockState(pathPos);
                    if (current.is(net.minecraft.world.level.block.Blocks.WATER)) {
                        level.setBlock(pathPos, surface, 2);
                    } else if (current.isAir()
                        || current.is(net.minecraft.world.level.block.Blocks.SHORT_GRASS)
                        || current.is(net.minecraft.world.level.block.Blocks.TALL_GRASS)
                        || current.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)
                        || current.is(net.minecraft.world.level.block.Blocks.DIRT)
                        || current.is(net.minecraft.world.level.block.Blocks.STONE)
                        || current.is(net.minecraft.world.level.block.Blocks.SAND)) {
                        level.setBlock(pathPos, surface, 2);
                    }
                    for (int up = 1; up <= 2; up++) {
                        BlockPos above = pathPos.above(up);
                        BlockState state = level.getBlockState(above);
                        if (state.isAir()
                            || state.is(net.minecraft.world.level.block.Blocks.SHORT_GRASS)
                            || state.is(net.minecraft.world.level.block.Blocks.TALL_GRASS)
                            || state.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)
                            || state.is(net.minecraft.world.level.block.Blocks.DIRT)) {
                            level.setBlock(above, style.air(), 2);
                        }
                    }
                }
            }
            pathColumns.add(steps);

            long stablePathSeed = level.getSeed()
                ^ BlockPos.asLong(pieceBox.minX(), pieceBox.minY(), pieceBox.minZ());
            RandomSource stableRandom = RandomSource.create(stablePathSeed);
            boolean isNorthSouth = pieceBox.getZSpan() > pieceBox.getXSpan();
            if (isNorthSouth) {
                int zSpan = pieceBox.getZSpan();
                for (int zOffset = 4; zOffset < zSpan - 4; zOffset += 10 + stableRandom.nextInt(6)) {
                    if (stableRandom.nextFloat() < 0.7F) {
                        lamp(level, box, stableRandom, pieceBox.minX() + 2, pieceBox.minZ() + zOffset,
                            false, style);
                    }
                }
            } else {
                int xSpan = pieceBox.getXSpan();
                for (int xOffset = 4; xOffset < xSpan - 4; xOffset += 10 + stableRandom.nextInt(6)) {
                    if (stableRandom.nextFloat() < 0.7F) {
                        lamp(level, box, stableRandom, pieceBox.minX() + xOffset, pieceBox.minZ() + 2,
                            true, style);
                    }
                }
            }
        }

        private static final java.util.concurrent.atomic.LongAdder pathColumns =
            new java.util.concurrent.atomic.LongAdder();

        /** How many road columns this structure has written since process start. Diagnostic only. */
        public static long placedPathColumnCount() {
            return pathColumns.sum();
        }

        /**
         * How many upstream blocks have been dropped rather than substituted since process start.
         *
         * <p>Dropping is not the same as substituting, and the count exists so the difference stays visible:
         * a block census can only see a missing block if it is known to have been meant to be there.
         */
        private static final java.util.concurrent.atomic.LongAdder droppedUpstreamBlocks =
            new java.util.concurrent.atomic.LongAdder();

        /** How many upstream blocks this structure has dropped because no era block fits. */
        public static long droppedUpstreamBlockCount() {
            return droppedUpstreamBlocks.sum();
        }

        private void lamp(
            WorldGenLevel level,
            BoundingBox box,
            RandomSource stableRandom,
            int alongX,
            int alongZ,
            boolean placeOnZ,
            Style style
        ) {
            int lampX = placeOnZ ? alongX : (stableRandom.nextBoolean() ? box.minX() - 1 : box.maxX() + 1);
            int lampZ = placeOnZ ? (stableRandom.nextBoolean() ? box.minZ() - 1 : box.maxZ() + 1) : alongZ;
            int ground = surfaceY(level, level, lampX, lampZ, box.maxY());
            BlockPos base = new BlockPos(lampX, ground + 1, lampZ);
            if (!box.isInside(base)) {
                return;
            }
            BlockState below = level.getBlockState(base.below());
            if (!below.isFaceSturdy(level, base.below(), Direction.UP)
                || below.is(net.minecraft.world.level.block.Blocks.GRAVEL)
                || below.is(style.planks().getBlock())) {
                return;
            }
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    for (int h = 0; h <= 6; h++) {
                        BlockState state = level.getBlockState(base.offset(dx, h, dz));
                        if (!state.isAir()
                            && !state.is(net.minecraft.world.level.block.Blocks.SHORT_GRASS)
                            && !state.is(net.minecraft.world.level.block.Blocks.TALL_GRASS)
                            && !state.is(net.minecraft.world.level.block.Blocks.GRAVEL)
                            && !state.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)
                            && !state.is(net.minecraft.world.level.block.Blocks.DIRT)) {
                            return;
                        }
                    }
                }
            }
            spawnLampPost(level, base, style);
        }

        /**
         * The lamp post: three fence blocks, a wool block, and four wall torches around the wool.
         *
         * <p>Upstream's {@code mc14torches} config chose between wall torches and floor torches standing on
         * the sides of the wool. A floor torch beside a block is not valid in this era's game and a wall
         * torch on the wool is exactly what the block was for, so the wall-torch form is what is placed. The
         * config option is not ported: it is a fidelity toggle, not a version boundary.
         */
        private void spawnLampPost(WorldGenLevel level, BlockPos basePos, Style style) {
            level.setBlock(basePos, style.fence(), 2);
            level.setBlock(basePos.above(), style.fence(), 2);
            level.setBlock(basePos.above(2), style.fence(), 2);
            BlockPos woolPos = basePos.above(3);
            level.setBlock(woolPos, style.wool(), 2);
            level.setBlock(woolPos.north(), style.wallTorch()
                .setValue(WallTorchBlock.FACING, Direction.NORTH), 2);
            level.setBlock(woolPos.south(), style.wallTorch()
                .setValue(WallTorchBlock.FACING, Direction.SOUTH), 2);
            level.setBlock(woolPos.east(), style.wallTorch()
                .setValue(WallTorchBlock.FACING, Direction.EAST), 2);
            level.setBlock(woolPos.west(), style.wallTorch()
                .setValue(WallTorchBlock.FACING, Direction.WEST), 2);
        }

        private void generateSmallHouse(WorldGenLevel level, BoundingBox box, boolean fenceRoof) {
            Style style = style();
            BlockState cobble = style.cobble();
            BlockState planks = style.planks();
            BlockState stairs = style.stairs();
            BlockState log = style.log();
            BlockState fence = style.fence();
            this.fillWithBlocks(level, box, 0, 1, 0, 4, 5, 4, style.air());
            createBase(level, box, 0, 0, 4, 4, cobble);
            createBaseStairs(level, box, 2, 5);
            this.fillWithBlocks(level, box, 0, 1, 0, 4, 1, 4, cobble);
            this.placeBlock(level, stairs.setValue(StairBlock.FACING, Direction.SOUTH), 2, 1, 5, box);
            this.fillWithBlocks(level, box, 0, 2, 0, 0, 4, 0, cobble);
            this.fillWithBlocks(level, box, 4, 2, 0, 4, 4, 0, cobble);
            this.fillWithBlocks(level, box, 0, 2, 4, 0, 4, 4, cobble);
            this.fillWithBlocks(level, box, 4, 2, 4, 4, 4, 4, cobble);
            this.fillWithBlocks(level, box, 0, 2, 1, 0, 4, 3, planks);
            this.fillWithBlocks(level, box, 4, 2, 1, 4, 4, 3, planks);
            this.fillWithBlocks(level, box, 1, 2, 0, 3, 4, 0, planks);
            this.fillWithBlocks(level, box, 1, 2, 4, 3, 4, 4, planks);
            this.placeBlock(level, style.wallTorch().setValue(WallTorchBlock.FACING, Direction.SOUTH),
                2, 4, 3, box);
            this.fillWithBlocks(level, box, 2, 2, 4, 2, 3, 4, style.air());
            this.placeBlock(level, style.glassPane()
                .setValue(CrossCollisionBlock.EAST, true).setValue(CrossCollisionBlock.WEST, true),
                2, 3, 0, box);
            this.placeBlock(level, style.glassPane()
                .setValue(CrossCollisionBlock.NORTH, true).setValue(CrossCollisionBlock.SOUTH, true),
                0, 3, 2, box);
            this.placeBlock(level, style.glassPane()
                .setValue(CrossCollisionBlock.NORTH, true).setValue(CrossCollisionBlock.SOUTH, true),
                4, 3, 2, box);
            this.fillWithBlocks(level, box, 0, 5, 0, 4, 5, 4, log);
            this.fillWithBlocks(level, box, 1, 5, 1, 3, 5, 3, planks);
            if (fenceRoof) {
                this.fillWithBlocks(level, box, 0, 6, 0, 4, 7, 4, style.air());
                this.fillWithBlocks(level, box, 1, 2, 1, 1, 5, 1,
                    style.ladder().setValue(LadderBlock.FACING, Direction.NORTH));
                this.fillWithBlocks(level, box, 1, 6, 0, 3, 6, 0, fence
                    .setValue(CrossCollisionBlock.EAST, true).setValue(CrossCollisionBlock.WEST, true));
                this.fillWithBlocks(level, box, 1, 6, 4, 3, 6, 4, fence
                    .setValue(CrossCollisionBlock.EAST, true).setValue(CrossCollisionBlock.WEST, true));
                this.fillWithBlocks(level, box, 0, 6, 1, 0, 6, 3, fence
                    .setValue(CrossCollisionBlock.NORTH, true).setValue(CrossCollisionBlock.SOUTH, true));
                this.fillWithBlocks(level, box, 4, 6, 1, 4, 6, 3, fence
                    .setValue(CrossCollisionBlock.NORTH, true).setValue(CrossCollisionBlock.SOUTH, true));
                this.placeBlock(level, fence
                    .setValue(CrossCollisionBlock.EAST, true).setValue(CrossCollisionBlock.NORTH, true),
                    0, 6, 0, box);
                this.placeBlock(level, fence
                    .setValue(CrossCollisionBlock.WEST, true).setValue(CrossCollisionBlock.NORTH, true),
                    4, 6, 0, box);
                this.placeBlock(level, fence
                    .setValue(CrossCollisionBlock.EAST, true).setValue(CrossCollisionBlock.SOUTH, true),
                    0, 6, 4, box);
                this.placeBlock(level, fence
                    .setValue(CrossCollisionBlock.WEST, true).setValue(CrossCollisionBlock.SOUTH, true),
                    4, 6, 4, box);
            }
        }

        private void generateLargeHouse(WorldGenLevel level, BoundingBox box) {
            Style style = style();
            BlockState cobble = style.cobble();
            BlockState planks = style.planks();
            BlockState stairs = style.stairs();
            BlockState log = style.log();
            this.fillWithBlocks(level, box, 0, 1, 0, 9, 7, 11, style.air());
            createBase(level, box, 0, 0, 6, 5, cobble);
            createBase(level, box, 0, 5, 8, 10, cobble);
            createBaseStairs(level, box, 6, 11);
            this.fillWithBlocks(level, box, 0, 1, 0, 6, 1, 4, cobble);
            this.fillWithBlocks(level, box, 0, 1, 5, 8, 1, 10, cobble);
            this.placeBlock(level, stairs.setValue(StairBlock.FACING, Direction.SOUTH), 6, 1, 11, box);
            this.fillWithBlocks(level, box, 1, 1, 1, 5, 1, 5, planks);
            this.fillWithBlocks(level, box, 1, 1, 6, 7, 1, 9, planks);
            this.fillWithBlocks(level, box, 0, 2, 0, 6, 4, 0, cobble);
            this.fillWithBlocks(level, box, 0, 2, 1, 0, 4, 10, cobble);
            this.fillWithBlocks(level, box, 6, 2, 1, 6, 4, 4, cobble);
            this.fillWithBlocks(level, box, 6, 2, 5, 8, 4, 5, cobble);
            this.fillWithBlocks(level, box, 8, 2, 6, 8, 4, 10, cobble);
            this.fillWithBlocks(level, box, 1, 2, 10, 7, 4, 10, cobble);
            this.fillWithBlocks(level, box, 1, 3, 10, 7, 4, 10, planks);
            this.fillWithBlocks(level, box, 6, 3, 5, 7, 4, 5, planks);
            this.placeBlock(level, style.door()
                .setValue(DoorBlock.FACING, Direction.SOUTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), 6, 2, 10, box);
            this.placeBlock(level, style.door()
                .setValue(DoorBlock.FACING, Direction.SOUTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 6, 3, 10, box);
            this.fillWithBlocks(level, box, 2, 3, 10, 4, 3, 10, log);
            this.fillWithBlocks(level, box, 8, 3, 6, 8, 3, 9, log);
            this.fillWithBlocks(level, box, 6, 3, 1, 6, 3, 4, log);
            this.fillWithBlocks(level, box, 0, 3, 6, 0, 3, 9, log);
            this.fillWithBlocks(level, box, 0, 3, 1, 0, 3, 9, log);
            this.placeBlock(level, planks, 0, 3, 5, box);
            this.fillWithBlocks(level, box, 0, 3, 2, 0, 3, 3, style.glassPane()
                .setValue(CrossCollisionBlock.NORTH, true).setValue(CrossCollisionBlock.SOUTH, true));
            this.fillWithBlocks(level, box, 0, 3, 7, 0, 3, 8, style.glassPane()
                .setValue(CrossCollisionBlock.NORTH, true).setValue(CrossCollisionBlock.SOUTH, true));
            this.fillWithBlocks(level, box, 6, 3, 2, 6, 3, 3, style.glassPane()
                .setValue(CrossCollisionBlock.NORTH, true).setValue(CrossCollisionBlock.SOUTH, true));
            this.fillWithBlocks(level, box, 8, 3, 7, 8, 3, 8, style.glassPane()
                .setValue(CrossCollisionBlock.NORTH, true).setValue(CrossCollisionBlock.SOUTH, true));
            this.placeBlock(level, style.glassPane()
                .setValue(CrossCollisionBlock.EAST, true).setValue(CrossCollisionBlock.WEST, true),
                4, 3, 10, box);
            this.placeBlock(level, style.wallTorch().setValue(WallTorchBlock.FACING, Direction.SOUTH),
                6, 4, 9, box);
            this.fillWithBlocks(level, box, 8, 5, 7, 8, 5, 8, planks);
            this.fillWithBlocks(level, box, 0, 5, 7, 0, 5, 8, planks);
            this.fillWithBlocks(level, box, 0, 5, 9, 8, 5, 9, planks);
            this.fillWithBlocks(level, box, 0, 6, 8, 8, 6, 8, planks);
            this.fillWithBlocks(level, box, 2, 5, 0, 4, 5, 0, log);
            this.placeBlock(level, style.glassPane()
                .setValue(CrossCollisionBlock.EAST, true).setValue(CrossCollisionBlock.WEST, true),
                3, 5, 0, box);
            this.placeBlock(level, planks, 3, 6, 0, box);
            this.placeBlock(level, planks, 7, 4, 4, box);
            this.placeBlock(level, planks, 6, 5, 5, box);
            this.placeBlock(level, planks, 5, 6, 6, box);
            this.placeBlock(level, planks, 0, 5, 6, box);
            this.placeBlock(level, planks, 0, 6, 7, box);
            this.placeBlock(level, planks, 1, 6, 6, box);
            this.fillWithBlocks(level, box, 1, 5, 0, 1, 5, 8, planks);
            this.fillWithBlocks(level, box, 5, 5, 0, 5, 5, 6, planks);
            this.fillWithBlocks(level, box, 1, 6, 7, 8, 6, 7, planks);
            this.fillWithBlocks(level, box, 6, 5, 6, 8, 5, 6, planks);
            this.fillWithBlocks(level, box, 2, 6, 0, 2, 6, 6, planks);
            this.fillWithBlocks(level, box, 4, 6, 0, 4, 6, 6, planks);
            this.fillWithBlocks(level, box, 3, 7, 0, 3, 7, 6, planks);
            this.fillWithBlocks(level, box, 2, 7, 7, 4, 7, 7, planks);
            this.placeBlock(level, stairs.setValue(StairBlock.FACING, Direction.NORTH), 8, 4, 4, box);
            this.fillWithBlocks(level, box, 7, 5, 5, 8, 5, 5,
                stairs.setValue(StairBlock.FACING, Direction.NORTH));
            this.fillWithBlocks(level, box, 6, 6, 6, 8, 6, 6,
                stairs.setValue(StairBlock.FACING, Direction.NORTH));
            this.fillWithBlocks(level, box, 5, 7, 7, 8, 7, 7,
                stairs.setValue(StairBlock.FACING, Direction.NORTH));
            this.fillWithBlocks(level, box, 7, 4, 0, 7, 4, 3,
                stairs.setValue(StairBlock.FACING, Direction.WEST));
            this.fillWithBlocks(level, box, 6, 5, 0, 6, 5, 4,
                stairs.setValue(StairBlock.FACING, Direction.WEST));
            this.fillWithBlocks(level, box, 5, 6, 0, 5, 6, 5,
                stairs.setValue(StairBlock.FACING, Direction.WEST));
            this.fillWithBlocks(level, box, 4, 7, 0, 4, 7, 6,
                stairs.setValue(StairBlock.FACING, Direction.WEST));
            this.placeBlock(level, stairs.setValue(StairBlock.FACING, Direction.NORTH), 0, 6, 6, box);
            this.fillWithBlocks(level, box, 0, 7, 7, 1, 7, 7,
                stairs.setValue(StairBlock.FACING, Direction.NORTH));
            this.fillWithBlocks(level, box, 0, 5, 0, 0, 5, 5,
                stairs.setValue(StairBlock.FACING, Direction.EAST));
            this.fillWithBlocks(level, box, 1, 6, 0, 1, 6, 5,
                stairs.setValue(StairBlock.FACING, Direction.EAST));
            this.fillWithBlocks(level, box, 2, 7, 0, 2, 7, 6,
                stairs.setValue(StairBlock.FACING, Direction.EAST));
            this.fillWithBlocks(level, box, 0, 4, 11, 8, 4, 11,
                stairs.setValue(StairBlock.FACING, Direction.SOUTH));
            this.fillWithBlocks(level, box, 0, 5, 10, 8, 5, 10,
                stairs.setValue(StairBlock.FACING, Direction.SOUTH));
            this.fillWithBlocks(level, box, 0, 6, 9, 8, 6, 9,
                stairs.setValue(StairBlock.FACING, Direction.SOUTH));
            this.fillWithBlocks(level, box, 0, 7, 8, 8, 7, 8,
                stairs.setValue(StairBlock.FACING, Direction.SOUTH));
            this.placeBlock(level, style.bed()
                .setValue(BedBlock.PART, BedPart.HEAD).setValue(BedBlock.FACING, Direction.SOUTH),
                2, 2, 1, box);
            this.placeBlock(level, style.bed()
                .setValue(BedBlock.PART, BedPart.HEAD).setValue(BedBlock.FACING, Direction.SOUTH),
                4, 2, 1, box);
            this.placeBlock(level, style.bed()
                .setValue(BedBlock.PART, BedPart.FOOT).setValue(BedBlock.FACING, Direction.SOUTH),
                2, 2, 2, box);
            this.placeBlock(level, style.bed()
                .setValue(BedBlock.PART, BedPart.FOOT).setValue(BedBlock.FACING, Direction.SOUTH),
                4, 2, 2, box);
            // Upstream spawns two villagers here. This port places no entities: villagers are Beta 1.9
            // Prerelease content and this structure generates from Beta 1.8.1. See the class comment.
        }

        private BlockState cropFor(int typeRoll, RandomSource random, Style style) {
            BlockState carrots = style.carrots();
            BlockState potatoes = style.potatoes();
            if (typeRoll < 50) {
                return style.wheat().setValue(CropBlock.AGE, random.nextInt(8));
            }
            if (typeRoll < 70 && carrots != null) {
                return carrots.setValue(CropBlock.AGE, random.nextInt(8));
            }
            if (typeRoll < 90 && potatoes != null) {
                return potatoes.setValue(CropBlock.AGE, random.nextInt(8));
            }
            // Beetroot is Release 1.9 and no declared version grows it, so a roll in its band is wheat in
            // every version this build can create.
            return style.wheat().setValue(CropBlock.AGE, random.nextInt(8));
        }

        private void generateFarm(WorldGenLevel level, BoundingBox box) {
            Style style = style();
            createBase(level, box, 0, 0, 6, 8, style.dirt());
            this.fillWithBlocks(level, box, 0, 1, 0, 6, 3, 8, style.air());
            this.fillWithBlocks(level, box, 0, 1, 0, 6, 1, 8, style.log());
            this.fillWithBlocks(level, box, 1, 1, 1, 5, 1, 7,
                style.farmland().setValue(FarmBlock.MOISTURE, 7));
            this.fillWithBlocks(level, box, 3, 1, 1, 3, 1, 7, style.water());
            BoundingBox pieceBox = this.getBoundingBox();
            long stableSeed = level.getSeed()
                ^ BlockPos.asLong(pieceBox.minX(), pieceBox.minY(), pieceBox.minZ());
            RandomSource stableRandom = RandomSource.create(stableSeed);
            int leftPlot = stableRandom.nextInt(100);
            for (int x = 1; x <= 2; x++) {
                for (int z = 1; z <= 7; z++) {
                    this.placeBlock(level, cropFor(leftPlot, stableRandom, style), x, 2, z, box);
                }
            }
            int rightPlot = stableRandom.nextInt(100);
            for (int x = 4; x <= 5; x++) {
                for (int z = 1; z <= 7; z++) {
                    this.placeBlock(level, cropFor(rightPlot, stableRandom, style), x, 2, z, box);
                }
            }
        }

        private void generateLargeFarm(WorldGenLevel level, BoundingBox box) {
            Style style = style();
            createBase(level, box, 0, 0, 12, 8, style.dirt());
            this.fillWithBlocks(level, box, 0, 1, 0, 12, 3, 8, style.air());
            this.fillWithBlocks(level, box, 0, 1, 0, 12, 1, 8, style.log());
            this.fillWithBlocks(level, box, 1, 1, 1, 5, 1, 7,
                style.farmland().setValue(FarmBlock.MOISTURE, 7));
            this.fillWithBlocks(level, box, 7, 1, 1, 11, 1, 7,
                style.farmland().setValue(FarmBlock.MOISTURE, 7));
            this.fillWithBlocks(level, box, 3, 1, 1, 3, 1, 7, style.water());
            this.fillWithBlocks(level, box, 9, 1, 1, 9, 1, 7, style.water());
            BoundingBox pieceBox = this.getBoundingBox();
            long stableSeed = level.getSeed()
                ^ BlockPos.asLong(pieceBox.minX(), pieceBox.minY(), pieceBox.minZ());
            RandomSource stableRandom = RandomSource.create(stableSeed);
            int type1 = stableRandom.nextInt(100);
            for (int x = 1; x <= 2; x++) {
                for (int z = 1; z <= 7; z++) {
                    this.placeBlock(level, cropFor(type1, stableRandom, style), x, 2, z, box);
                }
            }
            int type2 = stableRandom.nextInt(100);
            for (int x = 4; x <= 5; x++) {
                for (int z = 1; z <= 7; z++) {
                    this.placeBlock(level, cropFor(type2, stableRandom, style), x, 2, z, box);
                }
            }
            int type3 = stableRandom.nextInt(100);
            for (int x = 7; x <= 8; x++) {
                for (int z = 1; z <= 7; z++) {
                    this.placeBlock(level, cropFor(type3, stableRandom, style), x, 2, z, box);
                }
            }
            int type4 = stableRandom.nextInt(100);
            for (int x = 10; x <= 11; x++) {
                for (int z = 1; z <= 7; z++) {
                    this.placeBlock(level, cropFor(type4, stableRandom, style), x, 2, z, box);
                }
            }
            // Upstream put a composter at 1,2,7 (Release 1.14). The block that stood there in this era is the
            // crafting table the farm hands used, so that is what is placed.
            this.placeBlock(level, style.craftingTable(), 1, 2, 7, box);
            this.placeBlock(level, style.dirt(), 1, 1, 7, box);
        }

        private void generateBlacksmith(WorldGenLevel level, BoundingBox box) {
            Style style = style();
            BlockState cobble = style.cobble();
            BlockState planks = style.planks();
            BlockState stairs = style.stairs();
            BlockState log = style.log();
            BlockState fence = style.fence();
            createBase(level, box, 0, 0, 9, 6, cobble);
            createBaseStairs(level, box, 6, 7);
            createBaseStairs(level, box, 7, 7);
            createBaseStairs(level, box, 8, 7);
            this.fillWithBlocks(level, box, 0, 1, 0, 9, 5, 6, style.air());
            this.fillWithBlocks(level, box, 0, 1, 0, 9, 1, 6, cobble);
            this.fillWithBlocks(level, box, 6, 1, 7, 8, 1, 7,
                style.cobbleStairs().setValue(StairBlock.FACING, Direction.SOUTH));
            this.fillWithBlocks(level, box, 9, 2, 6, 9, 4, 6, fence);
            this.fillWithBlocks(level, box, 5, 2, 6, 5, 4, 6, fence);
            // Upstream: a smooth stone double slab at 8,2,5 (Release 1.2.1) and a grindstone at 8,2,4
            // (Release 1.14). Both become the stone slab and the cobblestone of this era.
            this.placeBlock(level, style.stoneSlab().setValue(SlabBlock.TYPE, SlabType.DOUBLE),
                8, 2, 5, box);
            this.placeBlock(level, cobble, 8, 2, 4, box);
            this.fillWithBlocks(level, box, 0, 5, 0, 9, 5, 6, cobble);
            this.fillWithBlocks(level, box, 0, 2, 6, 0, 5, 6, log);
            this.fillWithBlocks(level, box, 0, 2, 0, 0, 5, 0, log);
            this.fillWithBlocks(level, box, 3, 2, 6, 3, 5, 6, log);
            this.placeChest(level, box, 5, 2, 1, Direction.NORTH, blacksmithLootTable());
            this.fillWithBlocks(level, box, 6, 2, 0, 9, 4, 0, cobble);
            this.fillWithBlocks(level, box, 6, 2, 1, 9, 2, 2, cobble);
            this.fillWithBlocks(level, box, 6, 4, 1, 9, 4, 2, cobble);
            // Upstream's iron bars beside the forge are Release 1.8; a fence is what this era used for the
            // same guard rail, so the piece keeps its shape with a block the version had.
            this.placeBlock(level, fence
                .setValue(CrossCollisionBlock.NORTH, true).setValue(CrossCollisionBlock.SOUTH, true),
                9, 3, 1, box);
            this.placeBlock(level, fence.setValue(CrossCollisionBlock.SOUTH, true), 9, 3, 2, box);
            this.fillWithBlocks(level, box, 7, 2, 1, 8, 2, 1, style.lava());
            this.fillWithBlocks(level, box, 6, 3, 1, 6, 3, 2, cobble);
            this.placeBlock(level, cobble, 6, 2, 3, box);
            this.fillWithBlocks(level, box, 6, 3, 3, 6, 4, 3, style.furnace());
            this.fillWithBlocks(level, box, 1, 2, 0, 5, 4, 0, planks);
            this.fillWithBlocks(level, box, 0, 2, 1, 0, 4, 5, planks);
            this.fillWithBlocks(level, box, 1, 2, 6, 2, 4, 6, planks);
            this.placeBlock(level, planks, 1, 2, 1, box);
            this.placeBlock(level, stairs.setValue(StairBlock.FACING, Direction.SOUTH), 2, 2, 1, box);
            this.placeBlock(level, stairs.setValue(StairBlock.FACING, Direction.WEST), 1, 2, 2, box);
            this.placeBlock(level, fence, 2, 2, 2, box);
            this.placeBlock(level, style.pressurePlate(), 2, 3, 2, box);
            this.fillWithBlocks(level, box, 4, 2, 3, 5, 4, 3, planks);
            this.fillWithBlocks(level, box, 3, 2, 4, 3, 4, 5, planks);
            this.fillWithBlocks(level, box, 3, 2, 5, 3, 3, 5, style.air());
            this.placeBlock(level, style.glassPane()
                .setValue(CrossCollisionBlock.WEST, true).setValue(CrossCollisionBlock.EAST, true),
                2, 3, 0, box);
            this.placeBlock(level, style.glassPane()
                .setValue(CrossCollisionBlock.WEST, true).setValue(CrossCollisionBlock.EAST, true),
                4, 3, 0, box);
            this.placeBlock(level, style.glassPane()
                .setValue(CrossCollisionBlock.NORTH, true).setValue(CrossCollisionBlock.SOUTH, true),
                0, 3, 2, box);
            this.placeBlock(level, style.glassPane()
                .setValue(CrossCollisionBlock.NORTH, true).setValue(CrossCollisionBlock.SOUTH, true),
                0, 3, 4, box);
            this.fillWithBlocks(level, box, 0, 6, 0, 9, 6, 6,
                style.stoneSlab().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
            this.fillWithBlocks(level, box, 1, 6, 1, 8, 6, 5, style.air());
            // Upstream spawns a villager here.
        }

        /** The table this blacksmith chest uses: horse armour only exists from Release 1.6.1. */
        private ResourceKey<LootTable> blacksmithLootTable() {
            String version = versionId == null ? "" : versionId;
            boolean horseArmour = net.historicalexperience.version.HistoricalVersions.byId(version)
                .map(entry -> entry.chronology()
                    >= net.historicalexperience.version.HistoricalVersions.chronology(600_000, 1, 6, 1))
                .orElse(false);
            return horseArmour ? BLACKSMITH_LOOT_TABLE : BLACKSMITH_LOOT_TABLE_EARLY;
        }

        private void generateTavern(WorldGenLevel level, BoundingBox box) {
            Style style = style();
            BlockState cobble = style.cobble();
            BlockState planks = style.planks();
            BlockState stairs = style.stairs();
            BlockState log = style.log();
            BlockState door = style.door();
            this.fillWithBlocks(level, box, 0, 1, 0, 8, 7, 10, style.air());
            createBase(level, box, 0, 0, 6, 4, style.dirt());
            createBase(level, box, 0, 5, 8, 10, cobble);
            this.fillWithBlocks(level, box, 0, 1, 0, 6, 1, 4, style.dirt());
            this.fillWithBlocks(level, box, 0, 1, 5, 8, 1, 10, cobble);
            createBaseStairs(level, box, 6, 11);
            this.placeBlock(level, stairs.setValue(StairBlock.FACING, Direction.SOUTH), 6, 1, 11, box);
            this.placeBlock(level, cobble, 2, 1, 4, box);
            this.fillWithBlocks(level, box, 1, 1, 6, 7, 1, 9, planks);
            // Upstream: smooth stone double slabs for the floor (Release 1.2.1). Stone slabs otherwise.
            this.fillWithBlocks(level, box, 1, 1, 7, 3, 1, 9,
                style.stoneSlab().setValue(SlabBlock.TYPE, SlabType.DOUBLE));
            this.fillWithBlocks(level, box, 0, 2, 5, 0, 4, 10, cobble);
            this.fillWithBlocks(level, box, 8, 2, 5, 8, 4, 10, cobble);
            this.fillWithBlocks(level, box, 1, 2, 5, 7, 2, 5, cobble);
            this.fillWithBlocks(level, box, 1, 2, 10, 7, 2, 10, cobble);
            this.fillWithBlocks(level, box, 1, 3, 5, 7, 4, 5, planks);
            this.fillWithBlocks(level, box, 1, 3, 10, 7, 4, 10, planks);
            this.placeBlock(level, door.setValue(DoorBlock.FACING, Direction.SOUTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), 6, 2, 10, box);
            this.placeBlock(level, door.setValue(DoorBlock.FACING, Direction.SOUTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 6, 3, 10, box);
            this.placeBlock(level, door.setValue(DoorBlock.FACING, Direction.SOUTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), 2, 2, 5, box);
            this.placeBlock(level, door.setValue(DoorBlock.FACING, Direction.SOUTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 2, 3, 5, box);
            this.placeBlock(level, style.wallTorch().setValue(WallTorchBlock.FACING, Direction.SOUTH),
                6, 4, 9, box);
            this.placeBlock(level, style.wallTorch().setValue(WallTorchBlock.FACING, Direction.NORTH),
                2, 4, 6, box);
            this.fillWithBlocks(level, box, 0, 3, 6, 0, 3, 9, log);
            this.fillWithBlocks(level, box, 8, 3, 6, 8, 3, 9, log);
            this.fillWithBlocks(level, box, 0, 3, 7, 0, 3, 8, style.glassPane()
                .setValue(CrossCollisionBlock.NORTH, true).setValue(CrossCollisionBlock.SOUTH, true));
            this.fillWithBlocks(level, box, 8, 3, 7, 8, 3, 8, style.glassPane()
                .setValue(CrossCollisionBlock.NORTH, true).setValue(CrossCollisionBlock.SOUTH, true));
            this.fillWithBlocks(level, box, 5, 3, 5, 6, 3, 5, style.glassPane()
                .setValue(CrossCollisionBlock.EAST, true).setValue(CrossCollisionBlock.WEST, true));
            this.placeBlock(level, style.glassPane()
                .setValue(CrossCollisionBlock.EAST, true).setValue(CrossCollisionBlock.WEST, true),
                3, 3, 10, box);
            BlockState fence = style.fence();
            this.placeBlock(level, fence
                .setValue(CrossCollisionBlock.NORTH, true).setValue(CrossCollisionBlock.EAST, true),
                0, 2, 0, box);
            this.placeBlock(level, fence
                .setValue(CrossCollisionBlock.NORTH, true).setValue(CrossCollisionBlock.WEST, true),
                6, 2, 0, box);
            this.fillWithBlocks(level, box, 0, 2, 1, 0, 2, 4, fence
                .setValue(CrossCollisionBlock.NORTH, true).setValue(CrossCollisionBlock.SOUTH, true));
            this.fillWithBlocks(level, box, 6, 2, 1, 6, 2, 4, fence
                .setValue(CrossCollisionBlock.NORTH, true).setValue(CrossCollisionBlock.SOUTH, true));
            this.fillWithBlocks(level, box, 1, 2, 0, 5, 2, 0, fence
                .setValue(CrossCollisionBlock.EAST, true).setValue(CrossCollisionBlock.WEST, true));
            this.fillWithBlocks(level, box, 0, 5, 7, 0, 5, 8, planks);
            this.fillWithBlocks(level, box, 8, 5, 7, 8, 5, 8, planks);
            this.fillWithBlocks(level, box, 0, 5, 6, 8, 5, 6, planks);
            this.fillWithBlocks(level, box, 0, 5, 9, 8, 5, 9, planks);
            this.fillWithBlocks(level, box, 0, 6, 7, 8, 6, 8, planks);
            this.fillWithBlocks(level, box, 0, 4, 4, 8, 4, 4,
                stairs.setValue(StairBlock.FACING, Direction.NORTH));
            this.fillWithBlocks(level, box, 0, 5, 5, 8, 5, 5,
                stairs.setValue(StairBlock.FACING, Direction.NORTH));
            this.fillWithBlocks(level, box, 0, 6, 6, 8, 6, 6,
                stairs.setValue(StairBlock.FACING, Direction.NORTH));
            this.fillWithBlocks(level, box, 0, 7, 7, 8, 7, 7,
                stairs.setValue(StairBlock.FACING, Direction.NORTH));
            this.fillWithBlocks(level, box, 0, 4, 11, 8, 4, 11,
                stairs.setValue(StairBlock.FACING, Direction.SOUTH));
            this.fillWithBlocks(level, box, 0, 5, 10, 8, 5, 10,
                stairs.setValue(StairBlock.FACING, Direction.SOUTH));
            this.fillWithBlocks(level, box, 0, 6, 9, 8, 6, 9,
                stairs.setValue(StairBlock.FACING, Direction.SOUTH));
            this.fillWithBlocks(level, box, 0, 7, 8, 8, 7, 8,
                stairs.setValue(StairBlock.FACING, Direction.SOUTH));
            // Upstream put a smoker (Release 1.14) at 1,2,6; a furnace is the block that cooked food here.
            this.placeBlock(level, style.furnace(), 1, 2, 6, box);
            this.placeBlock(level, planks, 7, 2, 6, box);
            this.placeBlock(level, stairs.setValue(StairBlock.FACING, Direction.SOUTH), 6, 2, 6, box);
            this.placeBlock(level, stairs.setValue(StairBlock.FACING, Direction.EAST), 7, 2, 7, box);
            this.placeBlock(level, fence, 6, 2, 7, box);
            this.placeBlock(level, style.pressurePlate(), 6, 3, 7, box);
            this.fillWithBlocks(level, box, 2, 2, 8, 2, 2, 9,
                style.stoneSlab().setValue(SlabBlock.TYPE, SlabType.DOUBLE));
            // Upstream spawns two villagers here.
        }

        private void generateChurch(WorldGenLevel level, BoundingBox box) {
            Style style = style();
            BlockState cobble = style.cobble();
            BlockState stairs = style.cobbleStairs();
            this.fillWithBlocks(level, box, 0, 1, 0, 4, 12, 8, style.air());
            createBase(level, box, 1, 0, 3, 8, cobble);
            createBase(level, box, 0, 1, 4, 4, cobble);
            this.fillWithBlocks(level, box, 1, 1, 0, 3, 1, 8, cobble);
            this.fillWithBlocks(level, box, 0, 1, 1, 4, 1, 4, cobble);
            createBaseStairs(level, box, 2, 9);
            this.placeBlock(level, stairs.setValue(StairBlock.FACING, Direction.SOUTH), 2, 1, 9, box);
            this.fillWithBlocks(level, box, 1, 2, 0, 3, 5, 0, cobble);
            this.fillWithBlocks(level, box, 0, 2, 1, 0, 5, 7, cobble);
            this.fillWithBlocks(level, box, 4, 2, 1, 4, 5, 7, cobble);
            this.fillWithBlocks(level, box, 1, 2, 8, 3, 11, 8, cobble);
            this.fillWithBlocks(level, box, 0, 6, 5, 0, 11, 7, cobble);
            this.fillWithBlocks(level, box, 4, 6, 5, 4, 11, 7, cobble);
            this.fillWithBlocks(level, box, 1, 5, 4, 3, 11, 4, cobble);
            this.fillWithBlocks(level, box, 0, 5, 5, 4, 5, 8, cobble);
            this.fillWithBlocks(level, box, 0, 10, 4, 4, 10, 8, cobble);
            this.placeBlock(level, cobble, 0, 12, 6, box);
            this.placeBlock(level, cobble, 4, 12, 6, box);
            this.placeBlock(level, cobble, 2, 12, 4, box);
            this.placeBlock(level, cobble, 2, 12, 8, box);
            this.fillWithBlocks(level, box, 1, 6, 1, 3, 6, 3, cobble);
            this.fillWithBlocks(level, box, 1, 2, 1, 3, 2, 2, cobble);
            this.placeBlock(level, stairs.setValue(StairBlock.FACING, Direction.SOUTH), 1, 2, 3, box);
            this.placeBlock(level, stairs.setValue(StairBlock.FACING, Direction.SOUTH), 3, 2, 3, box);
            this.placeBlock(level, stairs.setValue(StairBlock.FACING, Direction.SOUTH), 2, 2, 2, box);
            this.placeBlock(level, stairs.setValue(StairBlock.FACING, Direction.WEST), 1, 3, 1, box);
            this.placeBlock(level, stairs.setValue(StairBlock.FACING, Direction.EAST), 3, 3, 1, box);
            this.placeBlock(level, style.wallTorch().setValue(WallTorchBlock.FACING, Direction.EAST),
                1, 5, 2, box);
            this.placeBlock(level, style.wallTorch().setValue(WallTorchBlock.FACING, Direction.WEST),
                3, 5, 2, box);
            this.placeBlock(level, style.wallTorch().setValue(WallTorchBlock.FACING, Direction.NORTH),
                2, 5, 1, box);
            this.placeBlock(level, style.door().setValue(DoorBlock.FACING, Direction.SOUTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), 2, 2, 8, box);
            this.placeBlock(level, style.door().setValue(DoorBlock.FACING, Direction.SOUTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 2, 3, 8, box);
            this.placeBlock(level, style.glassPane()
                .setValue(CrossCollisionBlock.WEST, true).setValue(CrossCollisionBlock.EAST, true),
                2, 4, 0, box);
            this.placeBlock(level, style.glassPane()
                .setValue(CrossCollisionBlock.SOUTH, true).setValue(CrossCollisionBlock.NORTH, true),
                0, 4, 2, box);
            this.placeBlock(level, style.glassPane()
                .setValue(CrossCollisionBlock.SOUTH, true).setValue(CrossCollisionBlock.NORTH, true),
                4, 4, 2, box);
            this.fillWithBlocks(level, box, 0, 3, 6, 0, 4, 6, style.glassPane()
                .setValue(CrossCollisionBlock.SOUTH, true).setValue(CrossCollisionBlock.NORTH, true));
            this.fillWithBlocks(level, box, 4, 3, 6, 4, 4, 6, style.glassPane()
                .setValue(CrossCollisionBlock.SOUTH, true).setValue(CrossCollisionBlock.NORTH, true));
            this.fillWithBlocks(level, box, 0, 7, 6, 0, 8, 6, style.glassPane()
                .setValue(CrossCollisionBlock.SOUTH, true).setValue(CrossCollisionBlock.NORTH, true));
            this.fillWithBlocks(level, box, 4, 7, 6, 4, 8, 6, style.glassPane()
                .setValue(CrossCollisionBlock.SOUTH, true).setValue(CrossCollisionBlock.NORTH, true));
            this.fillWithBlocks(level, box, 2, 7, 8, 2, 8, 8, style.glassPane()
                .setValue(CrossCollisionBlock.EAST, true).setValue(CrossCollisionBlock.WEST, true));
            this.fillWithBlocks(level, box, 2, 7, 4, 2, 8, 4, style.glassPane()
                .setValue(CrossCollisionBlock.EAST, true).setValue(CrossCollisionBlock.WEST, true));
            this.fillWithBlocks(level, box, 1, 2, 5, 1, 10, 5,
                style.ladder().setValue(LadderBlock.FACING, Direction.EAST));
            // Upstream put a brewing stand here. It is Beta 1.9 Prerelease 3 content, so Beta 1.8.1 gets the
            // crafting table instead; the block table decides, not this method.
            this.placeBlock(level, style.workstation(), 3, 2, 7, box);
            // Upstream spawns a villager here.
        }

        private void generateShack(WorldGenLevel level, BoundingBox box, boolean highRoof) {
            Style style = style();
            BlockState cobble = style.cobble();
            BlockState planks = style.planks();
            BlockState log = style.log();
            BlockState door = style.door();
            this.fillWithBlocks(level, box, 0, 1, 0, 3, 6, 4, style.air());
            createBase(level, box, 0, 0, 3, 4, cobble);
            this.fillWithBlocks(level, box, 0, 1, 0, 3, 1, 4, cobble);
            createBaseStairs(level, box, 2, 5);
            this.placeBlock(level, style.cobbleStairs().setValue(StairBlock.FACING, Direction.SOUTH),
                2, 1, 5, box);
            this.fillWithBlocks(level, box, 0, 2, 0, 3, 4, 0, log);
            this.fillWithBlocks(level, box, 0, 2, 4, 3, 4, 4, log);
            this.fillWithBlocks(level, box, 1, 2, 0, 2, 4, 0, planks);
            this.fillWithBlocks(level, box, 1, 2, 4, 2, 4, 4, planks);
            this.fillWithBlocks(level, box, 0, 2, 1, 0, 4, 3, planks);
            this.fillWithBlocks(level, box, 3, 2, 1, 3, 4, 3, planks);
            this.placeBlock(level, door.setValue(DoorBlock.FACING, Direction.SOUTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), 2, 2, 4, box);
            this.placeBlock(level, door.setValue(DoorBlock.FACING, Direction.SOUTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 2, 3, 4, box);
            this.fillWithBlocks(level, box, 1, 1, 1, 2, 1, 3, style.dirt());
            // Upstream's tall grass block. In this era it is the same block under its old name, so the
            // palette's short grass is exactly what upstream meant.
            this.placeBlock(level, style.shortGrass(), 2, 2, 3, box);
            this.placeBlock(level, style.fence()
                .setValue(CrossCollisionBlock.EAST, true).setValue(CrossCollisionBlock.SOUTH, true),
                2, 2, 1, box);
            this.placeBlock(level, style.pressurePlate(), 2, 3, 1, box);
            this.placeBlock(level, style.glassPane()
                .setValue(CrossCollisionBlock.NORTH, true).setValue(CrossCollisionBlock.SOUTH, true),
                0, 3, 2, box);
            this.placeBlock(level, style.glassPane()
                .setValue(CrossCollisionBlock.NORTH, true).setValue(CrossCollisionBlock.SOUTH, true),
                3, 3, 2, box);
            this.fillWithBlocks(level, box, 0, 5, 1, 3, 5, 3, log);
            this.fillWithBlocks(level, box, 1, 5, 0, 2, 5, 4, log);
            if (highRoof) {
                this.fillWithBlocks(level, box, 1, 5, 1, 2, 5, 3, style.air());
                this.fillWithBlocks(level, box, 1, 6, 1, 2, 6, 3, log);
            }
        }

        private void generateLibrary(WorldGenLevel level, BoundingBox box) {
            Style style = style();
            BlockState cobble = style.cobble();
            BlockState planks = style.planks();
            BlockState stairs = style.stairs();
            BlockState door = style.door();
            BlockState fence = style.fence();
            this.fillWithBlocks(level, box, 0, 1, 0, 8, 9, 5, style.air());
            createBase(level, box, 0, 0, 8, 5, cobble);
            this.fillWithBlocks(level, box, 0, 1, 0, 8, 1, 5, cobble);
            createBaseStairs(level, box, 7, 6);
            this.placeBlock(level, style.cobbleStairs().setValue(StairBlock.FACING, Direction.SOUTH),
                7, 1, 6, box);
            this.fillWithBlocks(level, box, 1, 2, 0, 7, 2, 0, cobble);
            this.fillWithBlocks(level, box, 1, 2, 5, 7, 2, 5, cobble);
            this.fillWithBlocks(level, box, 0, 2, 0, 0, 6, 5, cobble);
            this.fillWithBlocks(level, box, 8, 2, 0, 8, 6, 5, cobble);
            this.fillWithBlocks(level, box, 0, 8, 2, 8, 8, 3, cobble);
            this.fillWithBlocks(level, box, 0, 3, 1, 0, 5, 4, planks);
            this.fillWithBlocks(level, box, 8, 3, 1, 8, 5, 4, planks);
            this.fillWithBlocks(level, box, 1, 3, 0, 7, 5, 0, planks);
            this.fillWithBlocks(level, box, 1, 3, 5, 7, 5, 5, planks);
            this.fillWithBlocks(level, box, 1, 6, 0, 7, 6, 5, cobble);
            this.fillWithBlocks(level, box, 0, 7, 1, 8, 7, 4, cobble);
            this.placeBlock(level, door.setValue(DoorBlock.FACING, Direction.SOUTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), 7, 2, 5, box);
            this.placeBlock(level, door.setValue(DoorBlock.FACING, Direction.SOUTH)
                .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 7, 3, 5, box);
            this.fillWithBlocks(level, box, 2, 3, 0, 3, 3, 0, style.glassPane()
                .setValue(CrossCollisionBlock.EAST, true).setValue(CrossCollisionBlock.WEST, true));
            this.fillWithBlocks(level, box, 5, 3, 0, 6, 3, 0, style.glassPane()
                .setValue(CrossCollisionBlock.EAST, true).setValue(CrossCollisionBlock.WEST, true));
            this.fillWithBlocks(level, box, 2, 3, 5, 4, 4, 5, style.glassPane()
                .setValue(CrossCollisionBlock.EAST, true).setValue(CrossCollisionBlock.WEST, true));
            this.fillWithBlocks(level, box, 0, 3, 2, 0, 4, 3, style.glassPane()
                .setValue(CrossCollisionBlock.NORTH, true).setValue(CrossCollisionBlock.SOUTH, true));
            this.fillWithBlocks(level, box, 8, 3, 2, 8, 4, 3, style.glassPane()
                .setValue(CrossCollisionBlock.NORTH, true).setValue(CrossCollisionBlock.SOUTH, true));
            // Upstream reaches one block outside its own bounding box here (z = -1). Kept as upstream has it:
            // the block is written through the piece machinery, which refuses a position outside the piece
            // box, so the effect is the original's and not a larger structure.
            this.fillWithBlocks(level, box, 0, 6, -1, 8, 6, -1,
                stairs.setValue(StairBlock.FACING, Direction.NORTH));
            this.fillWithBlocks(level, box, 0, 7, 0, 8, 7, 0,
                stairs.setValue(StairBlock.FACING, Direction.NORTH));
            this.fillWithBlocks(level, box, 0, 8, 1, 8, 8, 1,
                stairs.setValue(StairBlock.FACING, Direction.NORTH));
            this.fillWithBlocks(level, box, 0, 9, 2, 8, 9, 2,
                stairs.setValue(StairBlock.FACING, Direction.NORTH));
            this.fillWithBlocks(level, box, 0, 6, 6, 8, 6, 6,
                stairs.setValue(StairBlock.FACING, Direction.SOUTH));
            this.fillWithBlocks(level, box, 0, 7, 5, 8, 7, 5,
                stairs.setValue(StairBlock.FACING, Direction.SOUTH));
            this.fillWithBlocks(level, box, 0, 8, 4, 8, 8, 4,
                stairs.setValue(StairBlock.FACING, Direction.SOUTH));
            this.fillWithBlocks(level, box, 0, 9, 3, 8, 9, 3,
                stairs.setValue(StairBlock.FACING, Direction.SOUTH));
            this.placeBlock(level, planks, 1, 2, 1, box);
            this.placeBlock(level, stairs.setValue(StairBlock.FACING, Direction.WEST), 1, 2, 2, box);
            // Upstream: a lectern (Release 1.14) at 1,2,3. The crafting table is the same workstation role in
            // this era, so both of these are the palette's crafting table.
            this.placeBlock(level, style.craftingTable(), 1, 2, 3, box);
            this.placeBlock(level, style.craftingTable(), 1, 2, 4, box);
            this.fillWithBlocks(level, box, 2, 2, 1, 5, 2, 1,
                stairs.setValue(StairBlock.FACING, Direction.SOUTH));
            this.placeBlock(level, fence, 2, 2, 2, box);
            this.placeBlock(level, style.pressurePlate(), 2, 3, 2, box);
            this.placeBlock(level, fence, 4, 2, 2, box);
            this.placeBlock(level, style.pressurePlate(), 4, 3, 2, box);
            this.fillWithBlocks(level, box, 1, 5, 1, 7, 5, 1, planks);
            this.fillWithBlocks(level, box, 1, 5, 4, 7, 5, 4, planks);
            this.fillWithBlocks(level, box, 1, 4, 1, 7, 4, 1, style.bookshelf());
            // Upstream spawns a villager here.
        }

        @Override
        public void postProcess(
            @NotNull WorldGenLevel level,
            @NotNull StructureManager structureManager,
            @NotNull ChunkGenerator generator,
            @NotNull RandomSource random,
            @NotNull BoundingBox box,
            @NotNull ChunkPos chunkPos,
            @NotNull BlockPos startPos
        ) {
            switch (this.pieceType) {
                case PIECE_WELL -> generateWell(level, box);
                case PIECE_PATH -> generatePath(level, box);
                case PIECE_SMALL_HOUSE -> generateSmallHouse(level, box, false);
                case PIECE_SMALL_HOUSE_FENCE -> generateSmallHouse(level, box, true);
                case PIECE_LARGE_HOUSE -> generateLargeHouse(level, box);
                case PIECE_FARM -> generateFarm(level, box);
                case PIECE_LARGE_FARM -> generateLargeFarm(level, box);
                case PIECE_BLACKSMITH -> generateBlacksmith(level, box);
                case PIECE_TAVERN -> generateTavern(level, box);
                case PIECE_CHURCH -> generateChurch(level, box);
                case PIECE_SHACK -> generateShack(level, box, false);
                case PIECE_SHACK_ROOF -> generateShack(level, box, true);
                case PIECE_LIBRARY -> generateLibrary(level, box);
                default -> {
                    // An unknown piece type is a piece written by a future version of this structure or a
                    // corrupt save. Placing nothing is the answer that cannot damage the world.
                }
            }
        }
    }
}
