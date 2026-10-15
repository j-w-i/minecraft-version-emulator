import java.io.DataInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Evidence tool: list the float constants of classes in a legacy client, to find hard-coded colours.
 *
 * <p>Before the colourmaps (before Alpha v1.2.0) the clients stored the grass and foliage tint as literal RGB floats
 * in the renderer, so there is no image to read. This reads each class's constant pool and prints the float constants
 * that a 0..1 colour could be built from, which makes the literals visible without a full decompiler.
 *
 * <p>Usage: {@code java tools/ScanLegacyColourConstants.java <client.jar> [class-name-substring]}
 */
public final class ScanLegacyColourConstants {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("usage: ScanLegacyColourConstants <client.jar> [class-substring]");
            return;
        }
        Path jar = Path.of(args[0]);
        String filter = args.length > 1 ? args[1] : "";
        Map<String, Set<Float>> byClass = new LinkedHashMap<>();
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            List<? extends ZipEntry> entries = zip.stream()
                .filter(entry -> entry.getName().endsWith(".class"))
                .sorted(Comparator.comparing(ZipEntry::getName))
                .toList();
            for (ZipEntry entry : entries) {
                String name = entry.getName().substring(0, entry.getName().length() - ".class".length())
                    .replace('/', '.');
                if (!name.contains(filter)) {
                    continue;
                }
                Set<Float> colours = new LinkedHashSet<>();
                try (InputStream in = zip.getInputStream(entry)) {
                    for (float value : floatConstants(new DataInputStream(in))) {
                        // A colour channel literal in the legacy clients is written as k/255F, so only these qualify.
                        if (value > 0.05F && value < 1.01F) {
                            int asByte = Math.round(value * 255.0F);
                            if (Math.abs(value - asByte / 255.0F) < 1.0E-6F) {
                                colours.add(value);
                            }
                        }
                    }
                }
                if (colours.size() >= 3 && colours.size() <= 10) {
                    byClass.put(name, colours);
                }
            }
        }
        byClass.forEach((name, values) -> {
            List<Float> sorted = new ArrayList<>(values);
            sorted.sort(Comparator.naturalOrder());
            System.out.printf("%-44s %s%n", name, sorted);
            if (sorted.size() == 3) {
                int[] rgb = new int[3];
                for (int i = 0; i < 3; i++) {
                    rgb[i] = Math.round(sorted.get(i) * 255.0F);
                }
                System.out.printf("%-44s   -> RGB %d,%d,%d  0x%02X%02X%02X%n", "", rgb[0], rgb[1], rgb[2],
                    rgb[0], rgb[1], rgb[2]);
            }
        });
    }

    /** Constant-pool walk that returns every CONSTANT_Float. */
    private static List<Float> floatConstants(DataInputStream in) throws Exception {
        List<Float> floats = new ArrayList<>();
        in.readInt();                       // magic
        in.readUnsignedShort();             // minor
        in.readUnsignedShort();             // major
        int count = in.readUnsignedShort();
        for (int i = 1; i < count; i++) {
            int tag = in.readUnsignedByte();
            switch (tag) {
                case 1 -> in.skipBytes(in.readUnsignedShort());          // Utf8
                case 3 -> in.skipBytes(4);                               // Integer
                case 4 -> floats.add(in.readFloat());                    // Float
                case 5, 6 -> {                                           // Long, Double (two slots)
                    in.skipBytes(8);
                    i++;
                }
                case 7, 8, 16, 19, 20 -> in.skipBytes(2);                // Class, String, MethodType, Module, Package
                case 15 -> in.skipBytes(3);                              // MethodHandle
                case 9, 10, 11, 12, 17, 18 -> in.skipBytes(4);           // Fieldref..InvokeDynamic
                default -> {
                    return floats;
                }
            }
        }
        return floats;
    }

    private ScanLegacyColourConstants() {
    }
}
