import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Evidence tool: extract the colour maps out of the original clients and report them, without writing any of the
 * original art anywhere.
 *
 * <p>Run from the repository root:
 * <pre>java tools/ExtractHistoricalColormaps.java &lt;libraries-dir&gt; --report</pre>
 *
 * <p>Every historical client from Alpha v1.2.0 onwards stores its grass/foliage tint lookup as a 256x256
 * {@code misc/grasscolor.png} and {@code misc/foliagecolor.png}. The client reads them straight into a 65536-entry
 * array ({@code BufferedImage.getRGB(0, 0, 256, 256, pixels, 0, 256)}) and indexes that array with
 * <pre>index = ((int) ((1.0 - rain * temp) * 255.0) &lt;&lt; 8) | (int) ((1.0 - temp) * 255.0)</pre>
 * so the map is a direct table of (temperature, rainfall) -&gt; colour. This tool prints those values and hashes each
 * archive so the extraction is reproducible and auditable.
 */
public final class ExtractHistoricalColormaps {
    /** client version -> the two entries, as they are named inside that jar. */
    private static final Map<String, String[]> VERSIONS = new LinkedHashMap<>();

    static {
        VERSIONS.put("a1.2.6", new String[] {"misc/grasscolor.png", "misc/foliagecolor.png"});
        VERSIONS.put("b1.0", new String[] {"misc/grasscolor.png", "misc/foliagecolor.png"});
        VERSIONS.put("b1.2_02", new String[] {"misc/grasscolor.png", "misc/foliagecolor.png"});
        VERSIONS.put("b1.3_01", new String[] {"misc/grasscolor.png", "misc/foliagecolor.png"});
        VERSIONS.put("b1.4_01", new String[] {"misc/grasscolor.png", "misc/foliagecolor.png"});
        VERSIONS.put("b1.5_01", new String[] {"misc/grasscolor.png", "misc/foliagecolor.png"});
        VERSIONS.put("b1.6.6", new String[] {"misc/grasscolor.png", "misc/foliagecolor.png"});
        VERSIONS.put("b1.7.3", new String[] {"misc/grasscolor.png", "misc/foliagecolor.png"});
        VERSIONS.put("b1.8-pre2", new String[] {"misc/grasscolor.png", "misc/foliagecolor.png"});
        VERSIONS.put("b1.8.1", new String[] {"misc/grasscolor.png", "misc/foliagecolor.png"});
        VERSIONS.put("b1.9-pre6", new String[] {"misc/grasscolor.png", "misc/foliagecolor.png"});
        VERSIONS.put("1.0", new String[] {"misc/grasscolor.png", "misc/foliagecolor.png"});
    }

    public static void main(String[] args) throws Exception {
        Path libraries = Path.of(args.length > 0 ? args[0]
            : System.getenv("APPDATA") + "\\PrismLauncher\\libraries\\com\\mojang\\minecraft");
        boolean dumpTables = args.length > 1 && args[1].equals("--dump");
        Path dumpDir = Path.of("logs/colormaps");

        for (Map.Entry<String, String[]> version : VERSIONS.entrySet()) {
            Path jar = libraries.resolve(version.getKey()).resolve("minecraft-" + version.getKey() + "-client.jar");
            if (!Files.isRegularFile(jar)) {
                System.out.printf("%-11s jar missing%n", version.getKey());
                continue;
            }
            String jarHash = sha256(Files.readAllBytes(jar)).substring(0, 16);
            List<String> lines = new ArrayList<>();
            for (String entry : version.getValue()) {
                try (ZipFile zip = new ZipFile(jar.toFile())) {
                    ZipEntry zipEntry = zip.getEntry(entry);
                    if (zipEntry == null) {
                        System.out.printf("%-11s %-22s absent%n", version.getKey(), entry);
                        continue;
                    }
                    BufferedImage image = ImageIO.read(zip.getInputStream(zipEntry));
                    int[] pixels = new int[65536];
                    image.getRGB(0, 0, 256, 256, pixels, 0, 256);
                    String hash = sha256(toBytes(pixels)).substring(0, 16);
                    System.out.printf("%-11s %-22s %dx%d unique=%d pixelsHash=%s jarSha256=%s%n",
                        version.getKey(), entry, image.getWidth(), image.getHeight(),
                        unique(pixels), hash, jarHash);
                    if (dumpTables) {
                        lines.add(entry + " " + hash);
                        lines.add(String.join(",", java.util.Arrays.stream(pixels)
                            .mapToObj(value -> String.format("0x%06X", value & 0xFFFFFF)).toArray(String[]::new)));
                    }
                    // A few probe cells, so a reader can sanity-check the extraction by eye.
                    for (double temp : new double[] {0.0, 0.5, 0.8, 1.0}) {
                        for (double rain : new double[] {0.0, 0.5, 1.0}) {
                            int index = ((int) ((1.0 - rain * temp) * 255.0) << 8) | (int) ((1.0 - temp) * 255.0);
                            System.out.printf("      temp=%.2f rain=%.2f index=%d -> 0x%06X%n",
                                temp, rain, index, pixels[index] & 0xFFFFFF);
                        }
                    }
                }
            }
            if (dumpTables) {
                Files.createDirectories(dumpDir);
                try (PrintWriter out = new PrintWriter(dumpDir.resolve(version.getKey() + ".txt").toFile(), "UTF-8")) {
                    lines.forEach(out::println);
                }
            }
        }
        if (dumpTables) {
            System.out.println("dumped tables to " + dumpDir.toAbsolutePath());
        }
    }

    private static int unique(int[] pixels) {
        java.util.Set<Integer> seen = new java.util.HashSet<>();
        for (int pixel : pixels) {
            seen.add(pixel & 0xFFFFFF);
        }
        return seen.size();
    }

    private static byte[] toBytes(int[] pixels) {
        byte[] bytes = new byte[pixels.length * 4];
        for (int i = 0; i < pixels.length; i++) {
            int value = pixels[i];
            bytes[i * 4] = (byte) (value >>> 24);
            bytes[i * 4 + 1] = (byte) (value >>> 16);
            bytes[i * 4 + 2] = (byte) (value >>> 8);
            bytes[i * 4 + 3] = (byte) value;
        }
        return bytes;
    }

    private static String sha256(byte[] data) throws Exception {
        return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
    }

    private ExtractHistoricalColormaps() {
    }
}
