/*
 * Minecraft Historical Experience
 * Copyright (C) 2026 Minecraft Historical Experience contributors
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package net.historicalexperience.client.lighting;

/**
 * The historical sky colours, taken from the original clients rather than from a mod's menu of approximations.
 *
 * <p><b>Why this file exists.</b> Nostalgic Tweaks offers a flat "universal sky colour" per era and its Beta entry is
 * {@code 0x97A3FF}. That value is not the Beta sky: the Beta clients computed the sky from a temperature field, so no
 * single flat colour is correct. This class carries the values that could actually be extracted, and
 * {@link #temperatureSkyColour(float)} is the function the Beta clients themselves called.
 *
 * <h2>Evidence, per era</h2>
 *
 * <ul>
 *   <li><b>Alpha v1.0.11</b> ({@code minecraft-a1.0.11-client.jar}, decompiled into
 *       {@code .upstream/legacy-decompile/a1011}): {@code cj.java:20} holds the sky colour field
 *       {@code private long A = 8961023L} and the sky getter at {@code cj.java:868} multiplies its channels by the
 *       day's brightness. {@code 8961023 == 0x8BBDFF}. Moderner Beta's {@code ALPHA_SKY_COLOR} is the same value.
 *       Nostalgic Tweaks' {@code ALPHA} entry also happens to be {@code 0x8BBDFF}, so that one was right.</li>
 *   <li><b>Infdev</b>: no Infdev client is present on this machine, so the value is Moderner Beta's extracted
 *       {@code INFDEV_420_SKY_COLOR} / {@code INFDEV_415} / {@code INFDEV_325} / {@code INFDEV_611_SKY_COLOR}
 *       ({@code 10079487 == 0x99CCFF}), quoted in its {@code ModernBetaBiomeColors} under the comment "All colors
 *       below extracted from respective original versions, so they should be accurate". Nostalgic Tweaks' Infdev
 *       entry ({@code 0xC6DEFF}) carries no such provenance and disagrees with it.</li>
 *   <li><b>Beta 1.0, 1.7.3 and 1.8.1</b> (decompiled into {@code .upstream/legacy-decompile/b10}, {@code /b173},
 *       {@code /b181}): all three compute the sky the same way, so there is no flat colour to extract. See
 *       {@link #temperatureSkyColour(float)}.</li>
 * </ul>
 *
 * <p>Nothing here is copied from a mod's configuration: every value is either read out of a client jar or is the
 * arithmetic of that jar's own method.
 */
public final class HistoricalSkyColours {
    /**
     * Infdev sky, {@code 10079487 == 0x99CCFF}. Moderner Beta's extracted {@code INFDEV_*_SKY_COLOR} constants; no
     * Infdev client jar is available here to re-derive it independently.
     */
    public static final int INFDEV = 0x99CCFF;

    /**
     * Alpha sky, {@code 8961023 == 0x8BBDFF}. Read directly from the Alpha v1.0.11 client's world sky-colour field
     * ({@code cj.java:20}); Moderner Beta's {@code ALPHA_SKY_COLOR} agrees.
     */
    public static final int ALPHA = 0x8BBDFF;

    private HistoricalSkyColours() {
    }

    /**
     * Beta's own sky colour for a temperature: the body of {@code BiomeGenBase.getSkyColorByTemp(float)}, identical in
     * Beta 1.0, Beta 1.7.3 and Beta 1.8.1 ({@code gq.java:96}, {@code kd.java:126}, {@code vh.java:106}):
     *
     * <pre>
     * temperature /= 3.0F;
     * if (temperature &lt; -1.0F) temperature = -1.0F;
     * if (temperature &gt; 1.0F) temperature = 1.0F;
     * return Color.getHSBColor(0.62222224F - temperature * 0.05F, 0.5F + temperature * 0.1F, 1.0F).getRGB();
     * </pre>
     *
     * <p>Modern Minecraft's {@code OverworldBiomes.calculateSkyColor(float)} is the same maths, but it converts HSV
     * with {@code Mth.hsvToRgb}, which truncates where {@code java.awt.Color} rounds. The two differ by one unit on
     * most temperatures (Beta {@code 0x80A1FF} against modern {@code 0x7FA1FF} at a neutral temperature), so this is
     * {@link #hsbToRgb}'s rounding rather than Minecraft's: the point is to reproduce the original, not to match
     * today's client.
     *
     * @param temperature the temperature at the player's position, exactly as the era's world supplied it
     * @return a packed {@code 0xRRGGBB} sky colour, before the day's brightness, rain, thunder and lightning are
     *     applied by the level (which the modern client still does, and the Beta clients did too)
     */
    public static int temperatureSkyColour(float temperature) {
        float t = temperature / 3.0F;
        t = Math.max(-1.0F, Math.min(1.0F, t));
        return hsbToRgb(0.62222224F - t * 0.05F, 0.5F + t * 0.1F, 1.0F);
    }

    /**
     * {@code java.awt.Color.HSBtoRGB}'s maths, reproduced so the mod needs no AWT: hue wraps through
     * {@code floor(hue)}, and each channel is rounded with {@code +0.5F} rather than truncated.
     */
    static int hsbToRgb(float hue, float saturation, float brightness) {
        int r;
        int g;
        int b;
        if (saturation == 0.0F) {
            r = g = b = (int) (brightness * 255.0F + 0.5F);
        } else {
            float h = (hue - (float) Math.floor(hue)) * 6.0F;
            int i = (int) h;
            float f = h - (float) i;
            float p = brightness * (1.0F - saturation);
            float q = brightness * (1.0F - saturation * f);
            float t = brightness * (1.0F - (1.0F - f) * saturation);
            switch (i) {
                case 0 -> {
                    r = (int) (brightness * 255.0F + 0.5F);
                    g = (int) (t * 255.0F + 0.5F);
                    b = (int) (p * 255.0F + 0.5F);
                }
                case 1 -> {
                    r = (int) (q * 255.0F + 0.5F);
                    g = (int) (brightness * 255.0F + 0.5F);
                    b = (int) (p * 255.0F + 0.5F);
                }
                case 2 -> {
                    r = (int) (p * 255.0F + 0.5F);
                    g = (int) (brightness * 255.0F + 0.5F);
                    b = (int) (t * 255.0F + 0.5F);
                }
                case 3 -> {
                    r = (int) (p * 255.0F + 0.5F);
                    g = (int) (q * 255.0F + 0.5F);
                    b = (int) (brightness * 255.0F + 0.5F);
                }
                case 4 -> {
                    r = (int) (t * 255.0F + 0.5F);
                    g = (int) (p * 255.0F + 0.5F);
                    b = (int) (brightness * 255.0F + 0.5F);
                }
                default -> {
                    r = (int) (brightness * 255.0F + 0.5F);
                    g = (int) (p * 255.0F + 0.5F);
                    b = (int) (q * 255.0F + 0.5F);
                }
            }
        }
        return r << 16 | g << 8 | b;
    }
}
