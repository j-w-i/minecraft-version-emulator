import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipFile;

/**
 * Evidence tool: report the exact bytes of a texture entry in any archive, so a pack's art can be matched to the era
 * whose original client shipped the same art.
 *
 * <p>Usage: {@code java tools/HashTextureEntries.java <archive> <entry> [<archive> <entry> ...]}
 */
public final class HashTextureEntries {
    public static void main(String[] args) throws Exception {
        for (int i = 0; i + 1 < args.length; i += 2) {
            Path archive = Path.of(args[i]);
            String entryName = args[i + 1];
            if (!Files.isRegularFile(archive)) {
                System.out.printf("%-28s MISSING%n", archive.getFileName());
                continue;
            }
            try (ZipFile zip = new ZipFile(archive.toFile())) {
                var entry = zip.getEntry(entryName);
                if (entry == null) {
                    System.out.printf("%-28s %-46s absent%n", archive.getFileName(), entryName);
                    continue;
                }
                byte[] bytes;
                try (var in = zip.getInputStream(entry)) {
                    bytes = in.readAllBytes();
                }
                System.out.printf("%-28s %-46s %6d B  sha256=%s%n", archive.getFileName(), entryName,
                    bytes.length, sha256(bytes).substring(0, 16));
            }
        }
    }

    private static String sha256(byte[] data) throws Exception {
        return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
    }

    private HashTextureEntries() {
    }
}
