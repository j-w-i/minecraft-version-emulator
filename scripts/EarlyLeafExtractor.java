/* SPDX-License-Identifier: LGPL-3.0-or-later
 * Independent reflection research tool; factual behavior only, no copied Minecraft implementation.
 * JDK source launch: java scripts/EarlyLeafExtractor.java .upstream/legacy-jars/<build>.jar
 */
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.*;
import java.security.MessageDigest;
import java.util.*;

public class EarlyLeafExtractor {
    private record Pin(String sha1, long bytes) { }
    private static final Map<String, Pin> PINS = Map.of(
        "in-20100223.jar", new Pin("801ce486bb7fd1b43a56bc5d226dfb1370c08678", 555633),
        "inf-20100420.jar", new Pin("27010a5137abd2c8d8df85e99c14f5406ec197b3", 552012),
        "inf-20100611.jar", new Pin("421318a554f17463a56a271d08e9597941d066d9", 566417));
    private static final class ProbeRandom extends Random {
        final int forced;
        final List<Integer> bounds = new ArrayList<>();
        ProbeRandom(int value) { super(0); forced = value; }
        @Override public int nextInt(int bound) { bounds.add(bound); return Math.floorMod(forced, bound); }
        @Override protected int next(int bits) { throw new IllegalStateException("Untraced random draw: " + bits); }
    }
    public static void main(String[] args) throws Exception {
        Path path = Path.of(args[0]); Pin pin = Objects.requireNonNull(PINS.get(path.getFileName().toString()), "Unpinned artifact");
        byte[] bytes = Files.readAllBytes(path);
        String sha1 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(bytes));
        if (bytes.length != pin.bytes || !sha1.equals(pin.sha1)) throw new IllegalStateException("Artifact identity mismatch");
        try (var loader = new URLClassLoader(new URL[] {path.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
            loader.loadClass("net.minecraft.a.b.a.f").getMethod("a").invoke(null);
            Class<?> blocks = loader.loadClass(path.getFileName().toString().equals("inf-20100611.jar")
                ? "net.minecraft.a.a.b.y" : "net.minecraft.a.a.b.x");
            Object[] registry = null;
            for (Field field : blocks.getDeclaredFields()) if (Modifier.isStatic(field.getModifiers())
                    && field.getType().isArray() && field.getType().getComponentType() == blocks) {
                field.setAccessible(true); registry = (Object[]) field.get(null);
                System.out.println("REGISTRY " + blocks.getName() + "." + field.getName() + " length=" + registry.length);
            }
            Object leaf = Objects.requireNonNull(registry)[18];
            System.out.println("ARTIFACT " + path.getFileName() + " sha1=" + sha1 + " bytes=" + bytes.length);
            System.out.println("LEAF_CLASS " + leaf.getClass().getName());
            for (Method method : leaf.getClass().getDeclaredMethods()) System.out.println("DECLARED " + method);
            for (Method method : leaf.getClass().getMethods()) {
                Class<?>[] types = method.getParameterTypes();
                if (method.getReturnType() != int.class || !(Arrays.equals(types, new Class<?>[] {Random.class})
                        || Arrays.equals(types, new Class<?>[] {int.class, Random.class}))) continue;
                System.out.println("PROBE " + method);
                for (int metadata = 0; metadata < 16; metadata++) for (int roll = 0; roll < 10; roll++) {
                    var random = new ProbeRandom(roll);
                    Object result = types.length == 1 ? method.invoke(leaf, random) : method.invoke(leaf, metadata, random);
                    System.out.println("RESULT meta=" + metadata + " roll=" + roll + " result=" + result + " bounds=" + random.bounds);
                }
            }
        }
    }
}
