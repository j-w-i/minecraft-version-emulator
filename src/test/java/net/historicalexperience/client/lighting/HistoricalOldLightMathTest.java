package net.historicalexperience.client.lighting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pins the ported old-lighting arithmetic. The constants are Nostalgic Tweaks' measured values, so these tests do not
 * re-derive them; they prove the port kept their SHAPE: monotone through the day, bounded, and right at the edges.
 */
class HistoricalOldLightMathTest {
    private static final float TWO_PI = (float) (Math.PI * 2.0);

    @Test
    @DisplayName("midday is full sky light and midnight is the night minimum of 4")
    void dayAndNight() {
        assertEquals(15, HistoricalOldLightMath.skyLightFromTime(0.0F), "time 0 is noon");
        assertEquals(4, HistoricalOldLightMath.skyLightFromTime(3.0F), "mid-night is the minimum");
    }

    @Test
    @DisplayName("sky light by time never leaves 4..15 anywhere in the day, and each level is reached")
    void tableIsBoundedAndComplete() {
        boolean[] seen = new boolean[16];
        for (int tick = 0; tick < 24000; tick++) {
            int value = HistoricalOldLightMath.skyLightFromTime(tick / 24000.0F * TWO_PI);
            assertTrue(value >= 4 && value <= 15, "out of range at tick " + tick + ": " + value);
            seen[value] = true;
        }
        for (int level = 4; level <= 15; level++) {
            assertTrue(seen[level], "the day never produces sky light " + level);
        }
    }

    @Test
    @DisplayName("weather takes off 1, 2 or 3 levels for rain, and 5 for a thunderstorm")
    void weather() {
        assertEquals(0, HistoricalOldLightMath.skyLightFromWeather(0.0F, 0.0F));
        assertEquals(1, HistoricalOldLightMath.skyLightFromWeather(0.3F, 0.0F));
        assertEquals(2, HistoricalOldLightMath.skyLightFromWeather(0.6F, 0.0F));
        assertEquals(3, HistoricalOldLightMath.skyLightFromWeather(0.9F, 0.0F));
        assertEquals(5, HistoricalOldLightMath.skyLightFromWeather(1.0F, 0.8F),
            "a thunderstorm outranks even full rain");
    }

    @Test
    @DisplayName("no sky light stays no light, and an open position follows the time of day")
    void combinedLight() {
        assertEquals(0, HistoricalOldLightMath.combinedLight(0, 5, 15, 15, 0, false, 15));
        // Open sky (15) at noon is 15; at midnight (4) it is 4 - the round robin's whole point.
        assertEquals(15, HistoricalOldLightMath.combinedLight(15, 0, 15, 15, 0, false, 15));
        assertEquals(4, HistoricalOldLightMath.combinedLight(15, 0, 15, 4, 0, false, 15));
        // A torch still lights a cave at night.
        assertEquals(13, HistoricalOldLightMath.combinedLight(15, 13, 15, 4, 0, false, 15));
    }

    @Test
    @DisplayName("deeper sky light is offset by how far it is from open sky")
    void combinedLightOffsetsByDepth() {
        // Sky light 12 is three below open sky, so at noon it reads 12.
        assertEquals(12, HistoricalOldLightMath.combinedLight(12, 0, 15, 15, 0, false, 15));
    }

    @Test
    @DisplayName("the grey lightmap is monotone in the light level on both curves")
    void lightmapIsMonotone() {
        for (boolean roundRobin : new boolean[] {true, false}) {
            for (boolean sky : new boolean[] {true, false}) {
                float previous = -1.0F;
                for (int i = 0; i <= 15; i++) {
                    float value = HistoricalOldLightMath.lightmapBrightness(i, sky, roundRobin, 0.0D, false);
                    assertTrue(value >= previous - 1e-6F, "brightness fell at level " + i + " sky=" + sky
                        + " roundRobin=" + roundRobin + ": " + previous + " -> " + value);
                    assertTrue(value >= 0.05F && value <= 1.0F, "out of [0.05, 1] at " + i + ": " + value);
                    previous = value;
                }
            }
        }
    }

    @Test
    @DisplayName("round robin subtracts no sky light; the classic form does, and darkens at night")
    void skylightSubtracted() {
        assertEquals(0, HistoricalOldLightMath.skylightSubtracted(true, false, 0.5F, 1.0F, 1.0F, false));
        int noon = HistoricalOldLightMath.skylightSubtracted(false, false, 0.0F, 0.0F, 0.0F, false);
        int midnight = HistoricalOldLightMath.skylightSubtracted(false, false, 0.5F, 0.0F, 0.0F, false);
        assertTrue(midnight > noon, "night must subtract more than noon: " + noon + " vs " + midnight);
    }

    @Test
    @DisplayName("no angle falls into a gap and flashes to full brightness mid-dusk or mid-dawn")
    void noGapsInTheTable() {
        // Upstream's intervals leave ~1-tick gaps that fall through to 15. Sweep far finer than a tick (a tick is
        // 2 pi / 24000 = 0.000262 rad) across the whole falling and rising edges.
        int previous = 15;
        for (double angle = 1.3650552; angle < 1.8235918; angle += 0.00001) {
            int value = HistoricalOldLightMath.skyLightFromTime((float) angle);
            assertTrue(value <= previous, "dusk rose from " + previous + " to " + value + " at " + angle);
            assertTrue(value <= 14, "dusk reads full brightness at " + angle);
            previous = value;
        }
        previous = 4;
        for (double angle = 4.459594; angle < 4.9184027; angle += 0.00001) {
            int value = HistoricalOldLightMath.skyLightFromTime((float) angle);
            assertTrue(value >= previous, "dawn fell from " + previous + " to " + value + " at " + angle);
            assertTrue(value < 15, "dawn reads full brightness at " + angle);
            previous = value;
        }
    }

    @Test
    @DisplayName("where upstream has an answer, this agrees with it")
    void agreesWithUpstreamInsideItsIntervals() {
        // One point inside each of upstream's published intervals.
        assertEquals(14, HistoricalOldLightMath.skyLightFromTime(1.40F));
        assertEquals(13, HistoricalOldLightMath.skyLightFromTime(1.43F));
        assertEquals(9, HistoricalOldLightMath.skyLightFromTime(1.62F));
        assertEquals(5, HistoricalOldLightMath.skyLightFromTime(1.80F));
        assertEquals(5, HistoricalOldLightMath.skyLightFromTime(4.48F));
        assertEquals(10, HistoricalOldLightMath.skyLightFromTime(4.70F));
        assertEquals(14, HistoricalOldLightMath.skyLightFromTime(4.90F));
    }
}
