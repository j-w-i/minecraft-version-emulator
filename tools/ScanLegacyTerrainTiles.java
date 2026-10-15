import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipFile;

/**
 * Evidence tool: report the tile hash of named {@code terrain.png} tiles in each legacy client, to date a texture
 * change exactly.
 *
 * <p>Usage: {@code java tools/ScanLegacyTerrainTiles.java <libraries-dir>}
 */
public final class ScanLegacyTerrainTiles {
    /** terrain.png is a 16x16 grid; these are the tiles this project needs dated. */
    private static final Map<String, Integer> TILES = new LinkedHashMap<>();

    static {
        TILES.put("cobblestone", 16);
        TILES.put("mossy_cobblestone", 36);
    }

    private static final String[] VERSIONS = {
        "a1.0.11", "a1.1.2_01", "a1.2.6", "b1.0", "b1.2_02", "b1.3_01", "b1.4_01", "b1.5_01", "b1.6.6",
        "b1.7.3", "b1.8-pre2", "b1.8.1", "b1.9-pre6", "1.0", "1.8.9",
    };

    public static void main(String[] args) throws Exception {
        Path libraries = Path.of(args.length > 0 ? args[0]
            : System.getenv("APPDATA") + "\\PrismLauncher\\libraries\\com\\mojang\\minecraft");
        System.out.printf("%-11s %-34s %-34s%n", "version", "cobblestone (tile 16)", "mossy_cobblestone (tile 36)");
        for (String version : VERSIONS) {
            Path jar = libraries.resolve(version).resolve("minecraft-" + version + "-client.jar");
            if (!Files.isRegularFile(jar)) {
                System.out.printf("%-11s jar missing%n", version);
                continue;
            }
            try (ZipFile zip = new ZipFile(jar.toFile())) {
                // 1.8.9 moved the block sheet to a per-block layout, so terrain.png is only the older clients.
                var entry = zip.getEntry("terrain.png");
                if (entry == null) {
                    System.out.printf("%-11s no terrain.png (post-1.6 block sheet layout)%n", version);
                    continue;
                }
                BufferedImage sheet = ImageIO.read(zip.getInputStream(entry));
                StringBuilder row = new StringBuilder();
                row.append(String.format("%-11s", version));
                for (Map.Entry<String, Integer> tile : TILES.entrySet()) {
                    row.append(' ').append(String.format("%-33s", hashTile(sheet, tile.getValue())));
                }
                System.out.println(row);
            }
        }
    }

    private static String hashTile(BufferedImage sheet, int index) throws Exception {
        int column = index % 16;
        int row = index / 16;
        int[] pixels = new int[256];
        sheet.getRGB(column * 16, row * 16, 16, 16, pixels, 0, 16);
        byte[] bytes = new byte[pixels.length * 4];
        for (int i = 0; i < pixels.length; i++) {
            bytes[i * 4] = (byte) (pixels[i] >>> 24);
            bytes[i * 4 + 1] = (byte) (pixels[i] >>> 16);
            bytes[i * 4 + 2] = (byte) (pixels[i] >>> 8);
            bytes[i * 4 + 3] = (byte) pixels[i];
        }
        return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)).substring(0, 12);
    }

    private ScanLegacyTerrainTiles() {
    }
}
