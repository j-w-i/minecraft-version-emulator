import java.awt.Color;
import java.util.Locale;

/**
 * Scratch evidence tool: Beta's own sky colour function vs modern Minecraft's.
 *
 * Beta 1.0 (dd.java:952), Beta 1.7.3 (fd.java:927) and Beta 1.8.1 (rv.java:997) all call
 * BiomeGenBase.getSkyColorByTemp(float), which is:
 *     t /= 3.0F; clamp(-1, 1);
 *     return Color.getHSBColor(0.62222224F - t * 0.05F, 0.5F + t * 0.1F, 1.0F).getRGB();
 * (see b1.0/gq.java:96, b1.7.3/kd.java:126, b1.8.1/vh.java:106)
 *
 * Modern Minecraft's OverworldBiomes.calculateSkyColor(float) is the same maths with
 * Mth.hsvToRgb instead of Color.getHSBColor. This prints both so the two can be compared
 * and so the reachable colour range is known.
 */
public final class SkyTable {
    private SkyTable() {
    }

    /** Byte-for-byte the maths of Mth.hsvToArgb in 1.21.1 (see javap of net.minecraft.util.Mth). */
    static int hsvToRgb(float hue, float saturation, float value) {
        int i = (int) (hue * 6.0F) % 6;
        float f = hue * 6.0F - (float) i;
        float g = value * (1.0F - saturation);
        float h = value * (1.0F - f * saturation);
        float j = value * (1.0F - (1.0F - f) * saturation);
        float r;
        float gr;
        float b;
        switch (i) {
            case 0 -> { r = value; gr = j; b = g; }
            case 1 -> { r = h; gr = value; b = g; }
            case 2 -> { r = g; gr = value; b = j; }
            case 3 -> { r = g; gr = h; b = value; }
            case 4 -> { r = j; gr = g; b = value; }
            case 5 -> { r = value; gr = g; b = h; }
            default -> throw new RuntimeException("" + hue + "," + saturation + "," + value);
        }
        return clamp((int) (r * 255.0F)) << 16 | clamp((int) (gr * 255.0F)) << 8 | clamp((int) (b * 255.0F));
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    /** Beta's own function, verbatim from the decompiled clients. */
    static int betaSkyColorByTemp(float temperature) {
        float t = temperature / 3.0F;
        if (t < -1.0F) {
            t = -1.0F;
        }
        if (t > 1.0F) {
            t = 1.0F;
        }
        return Color.getHSBColor(0.62222224F - t * 0.05F, 0.5F + t * 0.1F, 1.0F).getRGB() & 0xFFFFFF;
    }

    /** Modern Minecraft's function. */
    static int calculateSkyColor(float temperature) {
        float t = temperature / 3.0F;
        t = Math.max(-1.0F, Math.min(1.0F, t));
        return hsvToRgb(0.62222224F - t * 0.05F, 0.5F + t * 0.1F, 1.0F);
    }

    public static void main(String[] args) {
        System.out.println("temperature  beta(jar)  modern   rgb(beta)");
        int mismatches = 0;
        for (int step = -12; step <= 12; step++) {
            float t = step * 0.25F;
            int beta = betaSkyColorByTemp(t);
            int modern = calculateSkyColor(t);
            if (beta != modern) {
                mismatches++;
            }
            System.out.printf(Locale.ROOT, "%9.2f   0x%06X  0x%06X  (%d,%d,%d)%s%n",
                t, beta, modern, (beta >> 16) & 0xFF, (beta >> 8) & 0xFF, beta & 0xFF,
                beta == modern ? "" : "   <-- DIFFERS");
        }
        System.out.println("extreme cold  (t<=-3): 0x" + Integer.toHexString(betaSkyColorByTemp(-3.0F)).toUpperCase(Locale.ROOT));
        System.out.println("neutral       (t= 0) : 0x" + Integer.toHexString(betaSkyColorByTemp(0.0F)).toUpperCase(Locale.ROOT));
        System.out.println("extreme warm  (t>= 3): 0x" + Integer.toHexString(betaSkyColorByTemp(3.0F)).toUpperCase(Locale.ROOT));
        System.out.println("mismatches: " + mismatches);
    }
}
