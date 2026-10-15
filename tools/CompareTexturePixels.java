import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipFile;

/**
 * Evidence tool: print a small corner of a texture so two sources can be compared by eye, which is what settles a
 * question like "is this pack's mossy cobblestone the pre-Beta-1.8 art or the newer art".
 *
 * <p>Usage: {@code java tools/CompareTexturePixels.java <libraries-dir> <pack-or-jar> <entry> [legacy-tile-index]}
 */
public final class CompareTexturePixels {
    public static void main(String[] args) throws Exception {
        Path libraries = Path.of(args[0]);
        Path source = Path.of(args[1]);
        String entryName = args[2];
        Integer legacyTile = args.length > 3 ? Integer.parseInt(args[3]) : null;

        try (ZipFile zip = new ZipFile(source.toFile())) {
            var entry = zip.getEntry(entryName);
            if (entry == null) {
                System.out.println("no entry " + entryName + " in " + source.getFileName());
                return;
            }
            BufferedImage image = ImageIO.read(zip.getInputStream(entry));
            System.out.printf("%s %s %dx%d%n", source.getFileName(), entryName, image.getWidth(), image.getHeight());
            printCorner(image, 0, 0, 16);
            printCorner(image, 0, 0, 32);
        }

        if (legacyTile != null) {
            for (String version : new String[] {"b1.6.6", "b1.7.3", "b1.8.1"}) {
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
                    int column = legacyTile % 16;
                    int row = legacyTile / 16;
                    BufferedImage tile = sheet.getSubimage(column * 16, row * 16, 16, 16);
                    System.out.printf("legacy %s tile %d:%n", version, legacyTile);
                    printCorner(tile, 0, 0, 16);
                }
            }
        }
    }

    private static void printCorner(BufferedImage image, int x0, int y0, int size) {
        if (x0 + size > image.getWidth() || y0 + size > image.getHeight()) {
            return;
        }
        StringBuilder out = new StringBuilder();
        for (int y = y0; y < y0 + 4; y++) {
            out.append("      ");
            for (int x = x0; x < x0 + 4; x++) {
                out.append(String.format("%06X ", image.getRGB(x, y) & 0xFFFFFF));
            }
            out.append('\n');
        }
        System.out.printf("    corner (4x4 of %d):%n%s", size, out);
    }

    private CompareTexturePixels() {
    }
}
