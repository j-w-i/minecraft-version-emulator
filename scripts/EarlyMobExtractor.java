/* SPDX-License-Identifier: LGPL-3.0-or-later */
import java.lang.reflect.*;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Independent factual numeric item-selector probes, NOT the original death/attack implementation. */
class EarlyMobExtractor {
    record Pin(String sha1, long bytes) { }
    static final Map<String, Pin> PINS = Map.of(
        "in-20100223.jar", new Pin("801ce486bb7fd1b43a56bc5d226dfb1370c08678", 555633),
        "inf-20100420.jar", new Pin("27010a5137abd2c8d8df85e99c14f5406ec197b3", 552012),
        "inf-20100611.jar", new Pin("421318a554f17463a56a271d08e9597941d066d9", 566417));
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("One pinned artifact required");
        Path path = Path.of(args[0]); Pin pin = Objects.requireNonNull(PINS.get(path.getFileName().toString()), "Unknown artifact");
        byte[] bytes = Files.readAllBytes(path);
        String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(bytes));
        if (bytes.length != pin.bytes || !digest.equals(pin.sha1)) throw new IllegalArgumentException("Artifact identity mismatch");
        String selector = path.getFileName().toString().startsWith("in-") ? "c" : "b";
        try (var loader = new URLClassLoader(new java.net.URL[] {path.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
            Class.forName("net.minecraft.a.b.a.f", true, loader).getMethod("a").invoke(null);
            Class<?> world = Class.forName("net.minecraft.a.a.g", true, loader);
            String[][] rows = {{"zombie","net.minecraft.a.c.a.f","288"}, {"skeleton","net.minecraft.a.c.a.c","262"},
                {"pig","net.minecraft.a.c.b.c","319"}, {"spider","net.minecraft.a.c.a.b","287"},
                {"creeper","net.minecraft.a.c.a.d","289"}, {"sheep","net.minecraft.a.c.b.b","0"}};
            for (var row : rows) {
                Class<?> type = Class.forName(row[1], true, loader);
                Object entity = type.getDeclaredConstructor(world).newInstance((Object) null);
                Method method = null;
                for (Class<?> owner = type; owner != null && method == null; owner = owner.getSuperclass()) {
                    for (Method candidate : owner.getDeclaredMethods()) {
                        if (candidate.getName().equals(selector) && candidate.getParameterCount() == 0 && candidate.getReturnType() == int.class) {
                            if (method != null) throw new IllegalStateException("Ambiguous selector"); method = candidate;
                        }
                    }
                }
                if (method == null) throw new IllegalStateException("Missing item selector");
                method.setAccessible(true); int id = (Integer) method.invoke(entity);
                if (id != Integer.parseInt(row[2])) throw new IllegalStateException("Unexpected numeric drop " + row[0] + " " + id);
                System.out.printf("DROP {\"entity\":\"%s\",\"class\":\"%s\",\"selectorOwner\":\"%s\",\"selector\":\"%s()I\",\"numericDrop\":%d}%n",
                    row[0], row[1], method.getDeclaringClass().getName(), selector, id);
            }
        }
    }
}
