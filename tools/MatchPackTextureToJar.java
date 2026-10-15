import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipFile;

/**
 * Evidence tool: say which original client's tile a pack's texture is identical to.
 *
 * <p>A pack ships one 16x16 block texture; the original clients ship a 256x256 {@code terrain.png} whose tiles are the
 * same 16x16 art. This compares the pack's pixels with each jar's tile and reports the mean channel distance, so "0"
 * means the pack carries exactly that build's art.
 *
 * <p>Usage: {@code java tools/MatchPackTextureToJar.java <libraries-dir> <pack.zip> <entry> <legacy-tile-index>
 * [version ...]}
 */
public final class MatchPackTextureToJar {
    private static final String[] DEFAULT_VERSIONS = {
        "a1.0.11", "a1.2.6", "b1.0", "b1.6.6", "b1.7.3", "b1.8-pre2", "b1.8.1", "b1.9-pre6", "1.0",
    };

    public static void main(String[] args) throws Exception {
        Path libraries = Path.of(args[0]);
        Path pack = Path.of(args[1]);
        String entryName = args[2];
        int tile = Integer.parseInt(args[3]);
        String[] versions = args.length > 4
            ? java.util.Arrays.copyOfRange(args, 4, args.length) : DEFAULT_VERSIONS;

        int[] packPixels;
        try (ZipFile zip = new ZipFile(pack.toFile())) {
            var entry = zip.getEntry(entryName);
            if (entry == null) {
                System.out.println("pack has no " + entryName);
                return;
            }
            BufferedImage image = ImageIO.read(zip.getInputStream(entry));
            if (image.getWidth() != 16 || image.getHeight() != 16) {
                System.out.printf("pack texture is %dx%d, not 16x16; comparison skipped%n",
                    image.getWidth(), image.getHeight());
                return;
            }
            packPixels = new int[256];
            image.getRGB(0, 0, 16, 16, packPixels, 0, 16);
        }

        Map<String, Double> distances = new LinkedHashMap<>();
        for (String version : versions) {
            Path jar = libraries.resolve(version).resolve("minecraft-" + version + "-client.jar");
            if (!Files.isRegularFile(jar)) {
                continue;
            }
            try (ZipFile zip = new ZipFile(jar.toFile())) {
                var entry = zip.getEntry("terrain.png");
                if (entry == null) {
                    continue;
                }
                BufferedImage sheet = ImageIO.read(zip.getInputStream(entry));
                int column = tile % 16;
                int row = tile / 16;
                int[] tilePixels = new int[256];
                sheet.getRGB(column * 16, row * 16, 16, 16, tilePixels, 0, 16);
                distances.put(version, meanDistance(packPixels, tilePixels));
            }
        }
        System.out.printf("%s %s (tile %d) against terrain.png tiles:%n", pack.getFileName(), entryName, tile);
        distances.forEach((version, distance) -> System.out.printf("  %-11s mean channel distance %s%s%n",
            version, String.format("%.2f", distance), distance == 0.0 ? "   <-- identical" : ""));
    }

    private static double meanDistance(int[] left, int[] right) {
        long total = 0;
        for (int i = 0; i < left.length; i++) {
            total += Math.abs(((left[i] >> 16) & 0xFF) - ((right[i] >> 16) & 0xFF));
            total += Math.abs(((left[i] >> 8) & 0xFF) - ((right[i] >> 8) & 0xFF));
            total += Math.abs((left[i] & 0xFF) - (right[i] & 0xFF));
        }
        return (double) total / left.length;
    }

    private MatchPackTextureToJar() {
    }
}
