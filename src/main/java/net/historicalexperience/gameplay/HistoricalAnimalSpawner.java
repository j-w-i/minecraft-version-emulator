package net.historicalexperience.gameplay;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The passive-mob spawner the versions before Beta 1.8 actually ran, so an emptied old world repopulates.
 *
 * <h2>Why this exists at all</h2>
 *
 * <p>The owner killed every entity in an Alpha world, set tick speed to 10000, waited hours, and nothing came
 * back. The cause was architectural rather than a wrong number: everything this project did about spawning was an
 * <b>allow-list filter</b> over the candidates vanilla offers, and vanilla's modern cycle is not the old one. So
 * the candidates were correct - the probe measured chicken, cow, pig and sheep offered and none removed - and
 * nothing ever asked for them.
 *
 * <h2>The algorithm, read from the jars rather than guessed</h2>
 *
 * <p>Both eras were disassembled from hash-verified clients and are recorded in
 * {@code research/pre-beta-1.8-spawning-evidence.json}. The two differ in ways that matter, so they are
 * parameters here rather than one average:
 *
 * <pre>
 *                       Alpha v1.1.2_01 (before the v1.2.0 rework)   Beta 1.7.3
 *   creature cap        15, FLAT against countEntities              15 * eligibleChunks / 256, SCALED
 *   chunks per player   radius 4  (9x9)                             radius 8  (17x17)
 *   outer iterations    3 passes                                    3 packs
 *   inner attempts      2 per chunk                                 4 per chunk
 *   per-step offset     +- (rand(6) - rand(6)) on x/z, y FIXED      same
 *   start y             rand(128)                                   from the chunk height
 *   player exclusion    24 blocks                                   24 blocks
 *   world-spawn excl.   >= 576 squared (24 blocks)                  same
 *   final check         entity.getCanSpawnHere()                    entity.getCanSpawnHere()
 * </pre>
 *
 * <p>The creature cap being <b>15 in both</b> is the useful coincidence: the two eras differ in how the cap is
 * applied, not in its size, which is why the Alpha value and NT's default agree.
 *
 * <h2>What is deliberately not reproduced</h2>
 *
 * <p>The monster half is not implemented. Modern hostile spawning already works, the two eras' hostile caps
 * differ (200 flat against 70 scaled), and changing it without the same jar care would risk the behaviour that is
 * currently correct. Beta's per-mob {@code getMaxSpawnedInChunk} ceiling and its pack grouping are also not
 * reproduced yet; both are recorded as open work rather than approximated.
 *
 * <p>The light rule needs no code here. It lives in the entity in both eras - {@code EntityAnimal} demanded grass
 * below and full block light above 8 - and {@link net.minecraft.world.entity.SpawnPlacements} enforces the same
 * condition for the same entity types today, so calling it is the faithful equivalent of the jar's
 * {@code getCanSpawnHere()} rather than a substitute for it.
 */
public final class HistoricalAnimalSpawner {
    /** Both eras' creature cap. Alpha compares it flat; Beta scales it by eligible chunks. */
    static final int CREATURE_CAP = 15;

    /** Alpha and Beta walk the same offsets: {@code rand(6) - rand(6)} horizontally, and y does not move. */
    static final int STEP_OFFSET = 6;

    /** Neither era would spawn within 24 blocks of a player, or within 24 of the world spawn (576 = 24^2). */
    static final double PLAYER_EXCLUSION = 24.0D;
    static final double SPAWN_EXCLUSION_SQUARED = 576.0D;

    /** The Alpha client's {@code rand(128)} for the starting height, and its fixed world height. */
    static final int ALPHA_START_Y_BOUND = 128;

    private static long spawnAttempts;
    private static long spawned;

    private HistoricalAnimalSpawner() {
    }

    /** The era's shape, as a value so the boundary is testable without a running world. */
    public record Shape(int chunkRadius, int outerIterations, int attemptsPerChunk, boolean scaledCap) {
    }

    private static final Shape ALPHA_SHAPE = new Shape(4, 3, 2, false);
    private static final Shape BETA_SHAPE = new Shape(8, 3, 4, true);

    /**
     * Which shape a version ran, or {@code null} when this spawner does not apply to it.
     *
     * <p>The Alpha shape is the pre-Alpha-v1.2.0 algorithm, which the {@code alpha} profile
     * (Alpha v1.1.2_01) is the last declared version of. Alpha v1.2.0 reworked the algorithm - the wiki records
     * that a solid block no longer aborts the whole cycle and that chunk (0,0) stopped being evaluated first - so
     * from there the shape is Beta's, which the Beta 1.7.3 jar shows directly.
     *
     * <p>Infdev is excluded rather than swept in. It demonstrably had natural spawning, but its own jar was not
     * read for this and borrowing Alpha's numbers would be exactly the kind of borrowed evidence this project
     * rejects; {@code inf-20100420.jar} is on disk for that separate slice.
     */
    public static Shape shapeFor(String versionId) {
        if (versionId == null || !HistoricalGameplayTweaks.preBeta18(versionId)) {
            return null;
        }
        HistoricalVersion version = HistoricalVersions.byId(versionId).orElse(null);
        if (version == null || !"alpha".equals(version.era()) && !"beta".equals(version.era())) {
            return null;
        }
        int alphaRework = HistoricalVersions.byId("historical_experience:alpha_1_2_0")
            .orElseThrow().chronology();
        return version.chronology() < alphaRework ? ALPHA_SHAPE : BETA_SHAPE;
    }

    /** How many individual spawn attempts this spawner has made since process start. */
    public static long attempts() {
        return spawnAttempts;
    }

    /** How many mobs it has placed. */
    public static long spawnedCount() {
        return spawned;
    }

    /** The live tick path: the observers are the players, as in both jars. */
    public static int tick(ServerLevel level, String versionId) {
        List<BlockPos> observers = new ArrayList<>();
        for (var player : level.players()) {
            observers.add(player.blockPosition());
        }
        return tick(level, versionId, observers);
    }

    /**
     * The spawner itself, with the observers supplied.
     *
     * <p>Split so the verification probe can drive the <b>real</b> algorithm with a synthetic observer: a
     * dedicated server has no players, and a probe that re-implemented the loop would agree with itself while the
     * game did something else - the failure this project has already been caught by more than once.
     */
    public static int tick(ServerLevel level, String versionId, List<BlockPos> observers) {
        Shape shape = shapeFor(versionId);
        if (shape == null || observers.isEmpty()) {
            return 0;
        }

        Set<String> allowed = allowedCreatureTypes(versionId);
        if (allowed.isEmpty()) {
            return 0;
        }

        int present = countCreatures(level);
        Set<ChunkPos> eligible = eligibleChunks(observers, shape.chunkRadius());
        int cap = shape.scaledCap() ? CREATURE_CAP * eligible.size() / 256 : CREATURE_CAP;
        if (present >= cap) {
            return 0;
        }

        int placed = 0;
        BlockPos spawn = level.getSharedSpawnPos();
        // Two passes only in the Alpha shape's sense: it is "outer iterations", not the number of mobs.
        for (int outer = 0; outer < shape.outerIterations(); outer++) {
            for (ChunkPos chunk : eligible) {
                int baseX = chunk.getMinBlockX() + level.random.nextInt(16);
                int baseZ = chunk.getMinBlockZ() + level.random.nextInt(16);
                int y = shape.scaledCap()
                    ? level.getHeight(Heightmap.Types.WORLD_SURFACE, baseX, baseZ) + 1
                    : level.random.nextInt(ALPHA_START_Y_BOUND);

                for (int attempt = 0; attempt < shape.attemptsPerChunk(); attempt++) {
                    int x = baseX + level.random.nextInt(STEP_OFFSET) - level.random.nextInt(STEP_OFFSET);
                    int z = baseZ + level.random.nextInt(STEP_OFFSET) - level.random.nextInt(STEP_OFFSET);

                    spawnAttempts++;
                    EntityType<?> type = pick(level, allowed);
                    if (type == null) {
                        continue;
                    }
                    if (place(level, type, x, y, z, spawn)) {
                        placed++;
                    }
                }
            }
        }

        spawned += placed;
        return placed;
    }

    /** One position, judged exactly as both jars judged it. */
    private static boolean place(ServerLevel level, EntityType<?> type, int x, int y, int z, BlockPos worldSpawn) {
        if (y < level.getMinBuildHeight() + 1 || y >= level.getMaxBuildHeight() - 1) {
            return false;
        }

        BlockPos pos = new BlockPos(x, y, z);
        // The jars' four block tests: support below, two free blocks, and the block itself not an opaque cube.
        if (!level.getBlockState(pos.below()).isSolidRender(level, pos.below())) {
            return false;
        }
        BlockState at = level.getBlockState(pos);
        if (at.isSolidRender(level, pos) || at.canOcclude()) {
            return false;
        }
        if (level.getBlockState(pos.above()).isSolidRender(level, pos.above())) {
            return false;
        }

        // No player within 24 blocks...
        if (level.getNearestPlayer(x + 0.5D, y, z + 0.5D, PLAYER_EXCLUSION, false) != null) {
            return false;
        }

        // ...and at least 24 blocks from the world spawn.
        double dx = x + 0.5D - worldSpawn.getX();
        double dy = y - (double) worldSpawn.getY();
        double dz = z + 0.5D - worldSpawn.getZ();
        if (dx * dx + dy * dy + dz * dz < SPAWN_EXCLUSION_SQUARED) {
            return false;
        }

        // The jars' `entity.getCanSpawnHere()`. For the animals of this era that is exactly the modern
        // placement rule: a valid block below and full block light above 8.
        if (!net.minecraft.world.entity.SpawnPlacements.checkSpawnRules(
                type, level, MobSpawnType.NATURAL, pos, level.random)) {
            return false;
        }

        return type.spawn(level, pos, MobSpawnType.NATURAL) != null;
    }

    /** The biome-independent candidate set: what this version was allowed to have, restricted to animals. */
    private static Set<String> allowedCreatureTypes(String versionId) {
        return HistoricalSpawnRules.forProfile(versionId)
            .map(rule -> rule.allowedEntityTypes().stream()
                .filter(HistoricalAnimalSpawner::isCreature)
                .collect(java.util.stream.Collectors.toUnmodifiableSet()))
            .orElse(Set.of());
    }

    private static boolean isCreature(String entityTypeId) {
        ResourceLocation id = ResourceLocation.tryParse(entityTypeId);
        if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
            return false;
        }
        return BuiltInRegistries.ENTITY_TYPE.get(id).getCategory() == MobCategory.CREATURE;
    }

    private static EntityType<?> pick(ServerLevel level, Set<String> allowed) {
        List<String> ids = List.copyOf(allowed);
        return BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(ids.get(level.random.nextInt(ids.size()))));
    }

    /**
     * The animals already loaded, counted the way the jars counted them - a whole-world total, early-exiting at
     * the cap so a full world costs almost nothing.
     */
    private static int countCreatures(ServerLevel level) {
        int count = 0;
        for (var entity : level.getAllEntities()) {
            if (entity.getType().getCategory() == MobCategory.CREATURE) {
                if (++count >= CREATURE_CAP) {
                    return count;
                }
            }
        }
        return count;
    }

    /** Every chunk within the era's radius of any observer, deduplicated as both jars deduplicated them. */
    private static Set<ChunkPos> eligibleChunks(List<BlockPos> observers, int radius) {
        Set<ChunkPos> chunks = new java.util.LinkedHashSet<>();
        for (BlockPos observer : observers) {
            int cx = Mth.floor(observer.getX() / 16.0D);
            int cz = Mth.floor(observer.getZ() / 16.0D);
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    chunks.add(new ChunkPos(cx + dx, cz + dz));
                }
            }
        }
        return chunks;
    }
}
