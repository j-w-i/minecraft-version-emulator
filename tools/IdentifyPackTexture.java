import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipFile;

/**
 * Evidence tool: match a modern resource pack's single-tile texture against the legacy {@code terrain.png} tiles, to
 * say which era's art a pack actually carries.
 *
 * <p>Usage: {@code java tools/IdentifyPackTexture.java <libraries-dir> <pack.zip> <entry> <legacy-tile-index>}
 * <p>Example: {@code ... golden-days-base.zip assets/minecraft/textures/block/mossy_cobblestone.png 36}
 */
public final class IdentifyPackTexture {
    private static final String[] VERSIONS = {
        "b1.6.6", "b1.7.3", "b1.8.1", "1.0",
    };

    public static void main(String[] args) throws Exception {
        Path libraries = Path.of(args[0]);
        Path pack = Path.of(args[1]);
        String entryName = args[2];
        int tile = Integer.parseInt(args[3]);

        Map<String, String> legacyHashes = new LinkedHashMap<>();
        for (String version : VERSIONS) {
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
                int[] pixels = new int[256];
                sheet.getRGB(column * 16, row * 16, 16, 16, pixels, 0, 16);
                legacyHashes.put(version, hash(pixels));
            }
        }

        int[] packPixels = new int[256];
        String packHash;
        try (ZipFile zip = new ZipFile(pack.toFile())) {
            var entry = zip.getEntry(entryName);
            if (entry == null) {
                System.out.println("pack has no " + entryName);
                return;
            }
            BufferedImage image = ImageIO.read(zip.getInputStream(entry));
            // A pack texture is already one tile; scale it to 16x16 if it is larger.
            BufferedImage scaled = image.getWidth() == 16 && image.getHeight() == 16 ? image
                : resize(image, 16);
            scaled.getRGB(0, 0, 16, 16, packPixels, 0, 16);
            packHash = hash(packPixels);
        }
        System.out.println("pack " + pack.getFileName() + " " + entryName + " -> " + packHash);
        legacyHashes.forEach((version, hash) -> System.out.printf("  %-9s %s%s%n", version, hash,
            hash.equals(packHash) ? "   <-- matches" : ""));
    }

    private static BufferedImage resize(BufferedImage source, int size) {
        BufferedImage target = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        var graphics = target.createGraphics();
        graphics.drawImage(source, 0, 0, size, size, null);
        graphics.dispose();
        return target;
    }

    private static String hash(int[] pixels) throws Exception {
        byte[] bytes = new byte[pixels.length * 4];
        for (int i = 0; i < pixels.length; i++) {
            bytes[i * 4] = (byte) (pixels[i] >>> 24);
            bytes[i * 4 + 1] = (byte) (pixels[i] >>> 16);
            bytes[i * 4 + 2] = (byte) (pixels[i] >>> 8);
            bytes[i * 4 + 3] = (byte) pixels[i];
        }
        return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)).substring(0, 12);
    }

    private IdentifyPackTexture() {
    }
}
