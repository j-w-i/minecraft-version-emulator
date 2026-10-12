package net.historicalexperience.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pure unit tests for the world column measurements the runtime probe reports.
 *
 * <p>No Minecraft runtime: the analysis takes block identifiers, so the arithmetic that produces
 * the probe's terrain numbers is pinned here rather than being validated only by a live run.
 */
class ColumnStatsTest {
    private static final String AIR = "minecraft:air";
    private static final String BEDROCK = "minecraft:bedrock";
    private static final String STONE = "minecraft:stone";
    private static final String DIRT = "minecraft:dirt";
    private static final String WATER = "minecraft:water";

    private static List<String> column(String... samples) {
        List<String> column = new ArrayList<>(samples.length);
        Collections.addAll(column, samples);
        return column;
    }

    @Test
    @DisplayName("a solid column reports one bedrock, no air and no water")
    void solidColumn() {
        ColumnStats.Stats stats = ColumnStats.analyze(
            column(BEDROCK, STONE, STONE, DIRT, DIRT));

        assertEquals(ColumnStats.NONE, stats.lowestAir(),
            "a fully solid column has no air sample to report");
        assertEquals(4, stats.top());
        assertEquals(1, stats.bedrock());
        assertEquals(0, stats.water());
        assertEquals(ColumnStats.NONE, stats.waterTop());
        assertEquals(5, stats.nonAirSamples());
        assertEquals(0, stats.airAboveTop());
        assertTrue(stats.hasTerrain());
    }

    @Test
    @DisplayName("a normal terrain column reports its surface and the air above it")
    void normalColumn() {
        ColumnStats.Stats stats = ColumnStats.analyze(
            column(BEDROCK, STONE, STONE, DIRT, DIRT, AIR, AIR, AIR));

        assertEquals(5, stats.lowestAir(), "air starts immediately above the surface");
        assertEquals(4, stats.top());
        assertEquals(1, stats.bedrock());
        assertEquals(5, stats.nonAirSamples());
        assertEquals(3, stats.airAboveTop());
        assertEquals(8, stats.sampleCount());
        assertEquals(3, stats.airSamples());
    }

    @Test
    @DisplayName("a water column reports the water count and the highest water sample")
    void waterColumn() {
        ColumnStats.Stats stats = ColumnStats.analyze(
            column(BEDROCK, STONE, STONE, WATER, WATER, WATER, AIR, AIR));

        assertEquals(3, stats.water());
        assertEquals(5, stats.waterTop());
        assertEquals(5, stats.top(), "the water top is the highest non-air sample here");
        assertEquals(1, stats.bedrock());
        assertEquals(2, stats.airAboveTop());
    }

    @Test
    @DisplayName("an all-air column has no terrain and no spurious surface")
    void allAirColumn() {
        ColumnStats.Stats stats = ColumnStats.analyze(column(AIR, AIR, AIR, AIR));

        assertFalse(stats.hasTerrain());
        assertEquals(ColumnStats.NONE, stats.top());
        assertEquals(0, stats.lowestAir());
        assertEquals(0, stats.nonAirSamples());
        assertEquals(0, stats.airAboveTop());
        assertTrue(stats.describe().contains("<none>"), stats.describe());
    }

    @Test
    @DisplayName("an empty column is reported without throwing")
    void emptyColumn() {
        ColumnStats.Stats stats = ColumnStats.analyze(List.of());

        assertFalse(stats.hasTerrain());
        assertEquals(0, stats.sampleCount());
        assertEquals(ColumnStats.NONE, stats.top());
    }

    @Test
    @DisplayName("unreadable samples are counted as air and never become the surface")
    void unreadableSamplesAreAir() {
        List<String> samples = new ArrayList<>();
        samples.add(BEDROCK);
        samples.add(null);
        samples.add(null);
        samples.add(STONE);

        ColumnStats.Stats stats = ColumnStats.analyze(samples);

        assertEquals(3, stats.top(), "a null sample must not be reported as the top block");
        assertEquals(1, stats.lowestAir());
        assertEquals(4, stats.sampleCount());
        assertEquals(2, stats.nonAirSamples());
    }

    @Test
    @DisplayName("multiple bedrock samples are counted, not assumed to be one")
    void multipleBedrockSamplesAreCounted() {
        ColumnStats.Stats stats = ColumnStats.analyze(
            column(BEDROCK, BEDROCK, STONE, AIR));

        assertEquals(2, stats.bedrock(),
            "the probe must report what it measured rather than the expected value");
    }

    @Test
    @DisplayName("resource locations are recognised with or without a namespace")
    void identifierRecognitionIsNamespaceAgnostic() {
        assertTrue(ColumnStats.isAir("air"));
        assertTrue(ColumnStats.isAir("minecraft:air"));
        assertTrue(ColumnStats.isAir("minecraft:void_air"));
        assertTrue(ColumnStats.isAir("minecraft:cave_air"));
        assertFalse(ColumnStats.isAir(STONE));
        assertTrue(ColumnStats.isBedrock("bedrock"));
        assertTrue(ColumnStats.isBedrock(BEDROCK));
        assertTrue(ColumnStats.isWater("water"));
        assertTrue(ColumnStats.isWater(WATER));
        assertTrue(ColumnStats.isLava("minecraft:lava"));
        assertFalse(ColumnStats.isWater("minecraft:lava"));
    }

    @Test
    @DisplayName("the described line carries every field the probe reports")
    void describeCarriesEveryField() {
        ColumnStats.Stats stats = ColumnStats.analyze(
            column(BEDROCK, STONE, WATER, AIR));

        String described = stats.describe();
        for (String field : List.of("top=", "lowestAir=", "samples=", "bedrock=", "water=",
                "waterTop=", "nonAir=", "airAboveTop=")) {
            assertTrue(described.contains(field), "missing " + field + " in: " + described);
        }
    }

    @Test
    @DisplayName("relative sample indices convert to absolute Y for the probe report")
    void indicesConvertToAbsoluteY() {
        // A normal overworld chunk: minY -64, bedrock at index 0, surface at index 4.
        ColumnStats.Stats stats = ColumnStats.analyze(
            column(BEDROCK, STONE, STONE, DIRT, DIRT, AIR, AIR));

        int minY = -64;
        assertEquals(minY, stats.absoluteBedrockBottomY(minY));
        assertEquals(minY, stats.absoluteBedrockTopY(minY), "one bedrock layer at the bottom");
        assertEquals(minY + 4, stats.absoluteTopY(minY));
        assertEquals(minY + 5, stats.absoluteLowestAirY(minY));
        assertEquals(ColumnStats.NONE, stats.absoluteWaterTopY(minY));
    }

    @Test
    @DisplayName("absolute Y conversion reports NONE rather than a bogus coordinate")
    void absoluteYOfMissingFeaturesIsNone() {
        ColumnStats.Stats allAir = ColumnStats.analyze(column(AIR, AIR, AIR));

        assertEquals(ColumnStats.NONE, allAir.absoluteTopY(-64));
        assertEquals(ColumnStats.NONE, allAir.absoluteBedrockTopY(-64));
        assertEquals(ColumnStats.NONE, allAir.absoluteBedrockBottomY(-64));
        assertEquals(ColumnStats.NONE, allAir.absoluteWaterTopY(-64));
        assertEquals(-64, allAir.absoluteLowestAirY(-64), "air starts at the chunk floor");
    }

    @Test
    @DisplayName("bedrock position is measured, not assumed to be the bottom sample")
    void bedrockPositionsAreMeasured() {
        ColumnStats.Stats stats = ColumnStats.analyze(
            column(BEDROCK, BEDROCK, BEDROCK, STONE, AIR));

        assertEquals(3, stats.bedrock());
        assertEquals(0, stats.bedrockBottom(), "lowest bedrock sample index");
        assertEquals(2, stats.bedrockTop(), "highest bedrock sample index");
        assertEquals(-62, stats.absoluteBedrockTopY(-64));
        assertEquals(-64, stats.absoluteBedrockBottomY(-64));
    }

    @Test
    @DisplayName("negative chunk coordinates produce the real negative block columns")
    void negativeChunkCoordinatesArePositionedCorrectly() {
        // Chunk (-1,-1) spans blocks -16..-1 in both axes. A formula that is not a signed shift
        // would place the marker column in chunk 0 instead.
        assertEquals(-16, ColumnStats.absoluteBlockCoordinate(-1, 0));
        assertEquals(-1, ColumnStats.absoluteBlockCoordinate(-1, 15));
        assertEquals(-8, ColumnStats.absoluteBlockCoordinate(-1, 8),
            "the probe's marker column inside chunk (-1,-1)");
        assertEquals(0, ColumnStats.absoluteBlockCoordinate(0, 0));
        assertEquals(15, ColumnStats.absoluteBlockCoordinate(0, 15));
        assertEquals(8, ColumnStats.absoluteBlockCoordinate(0, 8));
        assertEquals(-4096, ColumnStats.absoluteBlockCoordinate(-256, 0));
    }

    @Test
    @DisplayName("the marker chunk's sampled columns are genuinely negative and inside the chunk")
    void markerChunkColumnsAreInsideTheChunk() {
        int chunkMinX = -1 << 4;
        int chunkMinZ = -1 << 4;
        for (int[] offset : new int[][] {{0, 0}, {8, 8}, {15, 15}}) {
            int worldX = chunkMinX + offset[0];
            int worldZ = chunkMinZ + offset[1];
            assertTrue(worldX >= -16 && worldX <= -1, "x in chunk: " + worldX);
            assertTrue(worldZ >= -16 && worldZ <= -1, "z in chunk: " + worldZ);
            assertEquals(worldX, ColumnStats.absoluteBlockCoordinate(worldX >> 4, worldX & 15),
                "signed shift and mask must round-trip for negative coordinates");
            assertEquals(worldZ, ColumnStats.absoluteBlockCoordinate(worldZ >> 4, worldZ & 15),
                "signed shift and mask must round-trip for negative coordinates");
        }
    }

    @Test
    @DisplayName("unreadable samples are reported as unreadable, not silently as stone")
    void unreadableSamplesAreIdentified() {
        assertTrue(ColumnStats.isUnreadable(null));
        assertFalse(ColumnStats.isUnreadable(STONE));
        assertFalse(ColumnStats.isUnreadable(AIR));
    }
}
