/* SPDX-License-Identifier: LGPL-3.0-or-later
 * Independent reflection-only research tool. Does not copy Minecraft implementation code.
 * Run against the archive jars with the JDK source launcher; see extract-early-gameplay.py.
 */
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

public class EarlyGameplayExtractor {
    static Object field(Object object, String name) throws Exception {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }

    public static void main(String[] args) throws Exception {
        try (var loader = new URLClassLoader(new URL[] {Path.of(args[0]).toUri().toURL()},
                ClassLoader.getPlatformClassLoader())) {
            Class<?> manager = loader.loadClass("net.minecraft.a.b.a.f");
            Object instance = manager.getMethod("a").invoke(null);
            for (Object recipe : (List<?>) field(instance, "b")) {
                Object stack = field(recipe, "d");
                System.out.println("RECIPE " + field(recipe, "a") + " " + field(recipe, "b") + " "
                    + Arrays.toString((int[]) field(recipe, "c")) + " -> "
                    + field(stack, "c") + " x" + field(stack, "a"));
            }
            Class<?> furnace = loader.loadClass("net.minecraft.a.a.b.a.b");
            Method smelt = furnace.getDeclaredMethod("d", int.class);
            smelt.setAccessible(true);
            for (int input = 0; input < 400; input++) {
                int output = (int) smelt.invoke(null, input);
                if (output >= 0) System.out.println("SMELT " + input + " -> " + output);
            }
            Class<?> items = loader.loadClass("net.minecraft.a.b.i");
            for (Field field : items.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) || field.getType() != items) continue;
                Object item = field.get(null);
                if (item == null) continue;
                System.out.print("ITEM " + field.getName() + " " + item.getClass().getName());
                for (Class<?> type = item.getClass(); type != Object.class; type = type.getSuperclass()) {
                    for (Field value : type.getDeclaredFields()) {
                        if (!Modifier.isStatic(value.getModifiers()) && value.getType() == int.class) {
                            value.setAccessible(true);
                            System.out.print(" " + type.getSimpleName() + "." + value.getName() + "=" + value.get(item));
                        }
                    }
                }
                System.out.println();
            }
        }
    }
}
