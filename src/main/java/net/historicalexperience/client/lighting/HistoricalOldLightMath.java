package net.historicalexperience.client.lighting;

/**
 * The arithmetic of the old (pre-Beta-1.8) lighting, with no Minecraft types so that it can be tested offline.
 *
 * <p>Every method here is a direct port of Nostalgic Tweaks {@code helper/candy/light/LightingHelper} or
 * {@code LightTextureHelper} (LGPL-3.0; see THIRD_PARTY_NOTICES.md). The constants are upstream's and are not
 * re-derived: they are measured values from the old engine (the sky-light-by-time-of-day table is the game's own
 * 22 000-tick curve sampled at each step), so changing one is a claim about the old game that needs its own evidence.
 *
 * <h2>What the old engine was</h2>
 *
 * <p>Before the Beta 1.8 Pre-release, the time of day changed the SKY LIGHT stored in the world, not just the colour
 * of the lightmap. Every dusk and dawn the game re-lit chunks, a few at a time - which is the "round robin" - and the
 * light was grey, because the lightmap carried no tint. The wiki's Beta 1.8 Pre-release entry records the end of it:
 * <i>"Day/night cycles no longer require chunk updates and have a smooth transition"</i> and <i>"The lighting on a
 * block is given a tint based on the most prominent source of light."</i>
 */
public final class HistoricalOldLightMath {
    private HistoricalOldLightMath() {
    }

    /**
     * Where each sky-light level BEGINS, on the falling (dusk) side. Index 0 is level 14, index 9 is level 5; the
     * night minimum of 4 begins at {@link #DUSK_NIGHT_START}. Upstream's lower bounds, unchanged.
     */
    private static final float[] DUSK_START = {
        1.3650552F, 1.4111987F, 1.4571424F, 1.5028657F, 1.548349F,
        1.5938551F, 1.6390872F, 1.684883F, 1.7306658F, 1.7769997F};

    private static final float DUSK_NIGHT_START = 1.8235918F;
    private static final float DAWN_NIGHT_END = 4.459594F;

    /**
     * Where each level begins on the rising (dawn) side. Index 0 is level 5, index 9 is level 14. Upstream's lower
     * bounds, unchanged.
     */
    private static final float[] DAWN_START = {
        4.459884F, 4.5064745F, 4.552807F, 4.598588F, 4.6443815F,
        4.6898937F, 4.7353964F, 4.780876F, 4.826319F, 4.8722606F};

    private static final float DAWN_DAY_START = 4.9184027F;

    /**
     * The sky light the old engine had at a time of day, 4 (midnight) to 15 (day).
     *
     * <p>Upstream expresses this as pairs of closed intervals, and its intervals do not quite touch: between the end
     * of one level and the start of the next there is a gap about one tick wide (1.4109247 to 1.4111987 is one).
     * An angle that lands in a gap matches no interval and falls through to the default of 15, so for a single tick
     * the sky would flash to full brightness and trigger a full re-light of every section. A test sweeping the whole
     * day found it. Here each level runs from its own start to the next level's start, which gives the same answer
     * as upstream everywhere upstream has an answer and closes the gaps.
     *
     * @param time the day fraction multiplied by 2 pi, as upstream computes it
     */
    public static int skyLightFromTime(float time) {
        if (time >= DUSK_NIGHT_START && time <= DAWN_NIGHT_END) {
            return 4;
        }
        if (time >= DUSK_START[0] && time < DUSK_NIGHT_START) {
            // Falling: level 14 at the first start, down to 5. The last level whose start is not past `time`.
            int level = 14;
            for (int i = 1; i < DUSK_START.length; i++) {
                if (time >= DUSK_START[i]) {
                    level = 14 - i;
                }
            }
            return level;
        }
        if (time > DAWN_NIGHT_END && time <= DAWN_DAY_START) {
            // Rising: level 5 at the first start, up to 14.
            int level = 4;
            for (int i = 0; i < DAWN_START.length; i++) {
                if (time >= DAWN_START[i]) {
                    level = 5 + i;
                }
            }
            return level;
        }
        return 15;
    }
    /** How many sky-light levels the weather takes off: rain in three steps, a thunderstorm five. */
    public static int skyLightFromWeather(float rain, float thunder) {
        int rainDiff = 0;
        if (rain >= 0.3F) {
            rainDiff = 1;
        }
        if (rain >= 0.6F) {
            rainDiff = 2;
        }
        if (rain >= 0.9F) {
            rainDiff = 3;
        }
        int thunderDiff = thunder >= 0.8F ? 5 : 0;
        return Math.max(rainDiff, thunderDiff);
    }

    /**
     * Combines a stored sky light with the block light and the CURRENT time of day into the value the old engine
     * would have stored. This is the heart of the round robin: the world holds sky light 15 under open sky all day,
     * and this decides what the old game would have held there right now.
     *
     * @param lightFromTime  {@link #skyLightFromTime}, or negative before it is known
     * @param weatherDiff    {@link #skyLightFromWeather}
     * @param maxBlockLight  upstream's {@code MAX_BLOCK_LIGHT}, 15 by default
     */
    public static int combinedLight(int skyLight, int blockLight, int maxLightLevel, int lightFromTime,
            int weatherDiff, boolean fixedTime, int maxBlockLight) {
        if (skyLight <= 0) {
            return 0;
        }
        int minSkyLight = Math.max(0, (fixedTime ? lightFromTime : maxLightLevel) - 11);
        int minLight = skyLight >= maxLightLevel ? minSkyLight : 0;
        int maxLight = Math.max(blockLight, maxBlockLight);
        int oldSkyLight = lightFromTime - weatherDiff;
        int offsetSkyLight = maxLightLevel - skyLight;

        if (skyLight != maxLightLevel && oldSkyLight <= minSkyLight) {
            oldSkyLight += weatherDiff;
        }
        int wanted = Math.max(oldSkyLight - offsetSkyLight, blockLight);
        return Math.max(minLight, Math.min(maxLight, wanted));
    }

    /**
     * The brightness of one lightmap row or column in the old grey lightmap.
     *
     * <p>Two tables, because the old engine's sky and block light were different curves, and a second pair for the
     * round-robin engine, whose block light was brighter. The numbers are upstream's measured values.
     *
     * @param gamma   the player's brightness option, 0 to 1
     * @param dynamic upstream's {@code DYNAMIC_LIGHT_BRIGHTNESS}: only lift the very darkest sky-light rows
     */
    public static float lightmapBrightness(int i, boolean isSkyLight, boolean roundRobin, double gamma,
            boolean dynamic) {
        float light = 1.0F - (float) i / 15.0F;
        float brightness;

        if (roundRobin) {
            if (i <= 4 && !isSkyLight) {
                brightness = 3.0F;
            } else if (isSkyLight) {
                brightness = i >= 14 ? 0.0F : 2.6F;
            } else if (i < 14) {
                brightness = switch (i) {
                    case 13 -> 2.7F;
                    case 12 -> 3.0F;
                    default -> 3.4F;
                };
            } else {
                brightness = 1.8F;
            }
        } else if (i <= 4 && isSkyLight) {
            brightness = 2.6F;
        } else if (isSkyLight) {
            brightness = i >= 14 ? 0.0F : 2.6F;
        } else {
            brightness = switch (i) {
                case 15 -> 0.6F;
                case 14 -> 1.2F;
                case 13 -> 1.8F;
                case 12, 11 -> 2.4F;
                case 9, 5 -> 2.5F;
                case 4 -> 2.9F;
                default -> 2.6F;
            };
        }

        float lightmapBrightness = ((1.0F - light) / (light * brightness + 1.0F)) * (1.0F - 0.05F) + 0.05F;

        if (gamma > 0.0D && isSkyLight && i < 5 && dynamic) {
            float ofGamma = switch (i) {
                case 1 -> 0.09F;
                case 2 -> 0.1F;
                case 3 -> 0.11F;
                case 4 -> 0.12F;
                default -> 0.08F;
            };
            float maxBrightness = lightmapBrightness(5, true, roundRobin, gamma, dynamic) - 0.05F;
            float shifted = lightmapBrightness + (float) (ofGamma * gamma);
            lightmapBrightness = Math.max(lightmapBrightness, Math.min(maxBrightness, shifted));
        }
        return lightmapBrightness;
    }

    /**
     * How much sky light the lightmap subtracts for the time of day. Zero under the round robin, because there the
     * world's own stored sky light already carries the time of day (see {@link #combinedLight}).
     *
     * @param modernNether true when the Nether is NOT using the old lighting, which dims it to a base of 7 instead of 15
     */
    public static int skylightSubtracted(boolean roundRobin, boolean modernNether, float timeOfDay, float rain,
            float thunder, boolean preventWeather) {
        if (roundRobin) {
            return 0;
        }
        float forceBrightness = modernNether ? 7.0F : 15.0F;
        float skyDarken = 1.0F - ((float) Math.cos(timeOfDay * (float) (Math.PI * 2.0)) * 2.0F + 0.5F);
        float rainLevel = preventWeather ? 0.0F : rain;
        float thunderLevel = preventWeather ? 0.0F : thunder;

        skyDarken = 1.0F - Math.max(0.0F, Math.min(1.0F, skyDarken));
        skyDarken = (float) (skyDarken * (1.0D - (double) (rainLevel * 5.0F) / 16.0D));
        skyDarken = (float) (skyDarken * (1.0D - (double) (thunderLevel * 5.0F) / 16.0D));
        skyDarken = 1.0F - skyDarken;
        return (int) (skyDarken * (forceBrightness - 4.0F) + (15.0F - forceBrightness));
    }
}
