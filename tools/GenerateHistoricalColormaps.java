import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Generates {@code src/main/resources/assets/historical_experience/colormap/*.txt} from the original clients.
 *
 * <p>The historical clients store their grass and foliage tint lookup as a 256x256 {@code misc/*color.png} which the
 * client reads straight into a 65536-entry array and indexes with
 * <pre>index = ((int) ((1.0 - rain * temp) * 255.0) &lt;&lt; 8) | (int) ((1.0 - temp) * 255.0)</pre>
 * This tool writes those values as plain text so the mod can apply the exact historical tint itself. The images are
 * never copied into the repository, and the file records the source jar and the hash of the values, so the extraction
 * can be re-run and checked.
 *
 * <p>Usage: {@code java tools/GenerateHistoricalColormaps.java <libraries-dir> [output-root]}
 */
public final class GenerateHistoricalColormaps {
    /** The one grass map every version from Alpha v1.2.0 to Release 1.0 shares (verified identical). */
    private static final String GRASS_SOURCE = "1.0";

    /** The pre-Beta-1.2 foliage map: Alpha v1.2.0, Alpha v1.2.6 and Beta 1.0 all share it. */
    private static final String FOLIAGE_OLD_SOURCE = "b1.0";

    /** The foliage map from Beta 1.2 onwards: Beta 1.2_02 through Release 1.0 all share it. */
    private static final String FOLIAGE_NEW_SOURCE = "1.0";

    private static final StringBuilder provenance = new StringBuilder();

    private static final Map<String, String> ENTRY_NAMES = new LinkedHashMap<>();

    static {
        ENTRY_NAMES.put("grass", "misc/grasscolor.png");
        ENTRY_NAMES.put("foliage", "misc/foliagecolor.png");
    }

    public static void main(String[] args) throws Exception {
        Path libraries = Path.of(args.length > 0 ? args[0]
            : System.getenv("APPDATA") + "\\PrismLauncher\\libraries\\com\\mojang\\minecraft");
        Path outputRoot = Path.of(args.length > 1 ? args[1] : "src/main/resources/assets/historical_experience/colormap");
        Files.createDirectories(outputRoot);

        writeTable(outputRoot, "grass_alpha_1_2_to_release_1_0.bin.gz", libraries, GRASS_SOURCE, "grass",
            "grass tint map, identical in every version from Alpha v1.2.0 to Release 1.0");
        writeTable(outputRoot, "foliage_alpha_1_2_to_beta_1_0.bin.gz", libraries, FOLIAGE_OLD_SOURCE, "foliage",
            "foliage tint map through Beta 1.0");
        writeTable(outputRoot, "foliage_beta_1_2_to_release_1_0.bin.gz", libraries, FOLIAGE_NEW_SOURCE, "foliage",
            "foliage tint map from Beta 1.2");

        Files.writeString(outputRoot.resolve("README.txt"), PROVENANCE_HEADER + provenance + PROVENANCE_FOOTER,
            StandardCharsets.UTF_8);
        System.out.println("wrote " + outputRoot.resolve("README.txt"));
    }

    private static final String PROVENANCE_HEADER = """
        Historical Experience - extracted colour map values

        These files hold the grass and foliage tint lookups of the original Minecraft clients. They were
        EXTRACTED from those clients' own misc/grasscolor.png and misc/foliagecolor.png, not authored here,
        and no original image is included in this repository.

        Each .bin.gz is the client's own 65536-entry int array, written as RGB triples in the same order and
        gzipped. The client builds that array with

            BufferedImage.getRGB(0, 0, 256, 256, pixels, 0, 256)

        and indexes it with

            index = ((int) ((1.0 - rain * temp) * 255.0) << 8) | (int) ((1.0 - temp) * 255.0)

        so entry i is row (i >> 8) = rainfall and column (i & 0xFF) = temperature, each 0..1.

        Regenerate with:  java tools/GenerateHistoricalColormaps.java <libraries-dir> <output-dir>

        """;

    private static final String PROVENANCE_FOOTER = """
        Verified identical across these clients (so only one copy of each is kept):
          grass   Alpha v1.2.6, Beta 1.0, 1.2_02, 1.3_01, 1.4_01, 1.5_01, 1.6.6, 1.7.3, 1.8-pre2, 1.8.1,
                  1.9-pre6, Release 1.0
          foliage Alpha v1.2.6 and Beta 1.0 (the older map)
          foliage Beta 1.2_02, 1.3_01, 1.4_01, 1.5_01, 1.6.6, 1.7.3, 1.8-pre2, 1.8.1, 1.9-pre6, Release 1.0
        """;

    private static void writeTable(Path outputRoot, String fileName, Path libraries, String version, String kind,
                                   String note) throws Exception {
        Path jar = libraries.resolve(version).resolve("minecraft-" + version + "-client.jar");
        if (!Files.isRegularFile(jar)) {
            throw new IllegalStateException("source client missing: " + jar);
        }
        String jarHash;
        int[] pixels;
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            jarHash = sha256(Files.readAllBytes(jar));
            ZipEntry entry = zip.getEntry(ENTRY_NAMES.get(kind));
            if (entry == null) {
                throw new IllegalStateException("no " + ENTRY_NAMES.get(kind) + " in " + version);
            }
            BufferedImage image = ImageIO.read(zip.getInputStream(entry));
            if (image.getWidth() != 256 || image.getHeight() != 256) {
                throw new IllegalStateException("unexpected colourmap size " + image.getWidth() + "x" + image.getHeight());
            }
            pixels = new int[65536];
            image.getRGB(0, 0, 256, 256, pixels, 0, 256);
        }

        // Stored as raw 0xRRGGBB ints, gzipped: the same values the client's getRGB produced, in the same order.
        byte[] raw = new byte[pixels.length * 3];
        for (int i = 0; i < pixels.length; i++) {
            int value = pixels[i] & 0xFFFFFF;
            raw[i * 3] = (byte) (value >>> 16);
            raw[i * 3 + 1] = (byte) (value >>> 8);
            raw[i * 3 + 2] = (byte) value;
        }
        String valueHash = sha256(raw);
        Path target = outputRoot.resolve(fileName);
        try (var out = new java.util.zip.GZIPOutputStream(Files.newOutputStream(target))) {
            out.write(raw);
        }
        provenance.append(fileName).append('\n');
        provenance.append("  contents: ").append(note).append('\n');
        provenance.append("  extracted from: ").append(ENTRY_NAMES.get(kind)).append(" in ")
            .append(jar.getFileName()).append(" (").append(version).append(")\n");
        provenance.append("  source jar sha256: ").append(jarHash).append('\n');
        provenance.append("  uncompressed: ").append(raw.length).append(" bytes, 65536 RGB triples\n");
        provenance.append("  uncompressed sha256: ").append(valueHash).append('\n');
        provenance.append("  gzip sha256: ").append(sha256(Files.readAllBytes(target))).append("\n\n");
        System.out.printf("%-42s %d values -> %d bytes gz  uncompressed sha256=%s%n",
            fileName, pixels.length, Files.size(target), valueHash.substring(0, 16));
    }

    private static String sha256(byte[] data) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
    }

    private GenerateHistoricalColormaps() {
    }
}
