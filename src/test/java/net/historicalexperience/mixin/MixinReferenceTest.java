/*
 * Minecraft Historical Experience
 * Copyright (C) 2026 Minecraft Historical Experience contributors
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package net.historicalexperience.mixin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Level: unit (source lint). The one rule about referencing mixins that a compiler cannot enforce.
 *
 * <p>Everything under {@code net.historicalexperience.mixin} is owned by the mixin configuration, and the transformer
 * refuses an ordinary direct reference to any class in it - the failure is a runtime {@code IllegalClassLoadError} whose
 * message is quoted in {@code HistoricalFoodStackLimit}, not a compile error. The single exemption is an
 * {@code @Accessor}/{@code @Invoker} interface, which Mixin makes loadable.
 *
 * <p>So this test pins the exact set of mixin interfaces referenced from outside the tree, and checks that each really
 * is an accessor interface. Adding a reference to a normal mixin therefore fails here instead of at the first container
 * click in a player's world.
 */
class MixinReferenceTest {
    private static final Path MAIN = Path.of("src/main/java");
    private static final Path MIXIN_ROOT = MAIN.resolve("net/historicalexperience/mixin");

    /**
     * Every mixin interface referenced from outside the mixin tree, with the code that needs it. An accessor is
     * loadable; nothing else in this tree is.
     */
    private static final Set<String> ALLOWED = new TreeSet<>(Set.of(
        "AxeAccess",                       // HistoricalToolInteractionEvents: strip/axe behaviour
        "ContainerScreenAccess",           // TexturesProbe, SnapshotFeatureProbe: slot geometry
        "CreativeScreenProbeAccess",       // SnapshotAnvilProbe: select a creative tab
        "GuiAccess",                       // HudHelper, GenerationVerification: GUI geometry
        "ImageButtonHistoricalSpritesAccess", // HistoricalRecipeBookButton: sprite override
        "LevelRendererHistoricalStarsAccess", // HistoricalStars: rebuild the star buffer
        "LivingEntityEquipmentInvoker",    // HistoricalEquipmentRefresh: reset equipment drops
        "NaturalSpawnerProbeAccessor",     // GenerationVerification: invoke the spawner
        "SheepAccess",                     // HistoricalMobLootHelper: sheared state
        "ShovelAccess",                    // HistoricalToolInteractionEvents
        "WorldListEntryAccessor"));        // ManageWorldHistoricalVersion: world list metadata

    @Test
    @DisplayName("only @Accessor/@Invoker mixin interfaces are referenced from outside the mixin tree")
    void outsideReferencesAreAccessorsOnly() throws IOException {
        Set<String> referenced = new LinkedHashSet<>();
        List<String> offenders = new ArrayList<>();
        Pattern importPattern = Pattern.compile("net\\.historicalexperience\\.mixin\\.[\\w.]*\\.([A-Z]\\w+)");

        try (Stream<Path> files = Files.walk(MAIN)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                if (file.startsWith(MIXIN_ROOT)) {
                    continue;
                }
                String body = Files.readString(file, StandardCharsets.UTF_8);
                // Javadoc mentions are not references; only imports and qualified uses are.
                String code = body.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", "");
                Matcher matcher = importPattern.matcher(code);
                while (matcher.find()) {
                    referenced.add(matcher.group(1));
                }
            }
        }

        for (String name : referenced) {
            Path source = findSource(name);
            if (source == null) {
                offenders.add(name + " is referenced but no file declares it");
                continue;
            }
            String text = Files.readString(source, StandardCharsets.UTF_8);
            boolean isInterface = text.contains("public interface " + name);
            boolean accessorOnly = text.contains("@Accessor") || text.contains("@Invoker");
            if (!isInterface || !accessorOnly) {
                offenders.add(name + " is not an accessor/invoker interface (a direct reference to a normal mixin is "
                    + "an IllegalClassLoadError at runtime)");
            }
        }
        assertTrue(offenders.isEmpty(), "outside references must be accessor interfaces: " + offenders);

        assertEquals(ALLOWED, new TreeSet<>(referenced),
            "the set of mixin interfaces referenced from outside changed. If that is deliberate, add it here with the "
                + "code that needs it AND confirm it declares only @Accessor/@Invoker methods");
    }

    @Test
    @DisplayName("no non-mixin class is ever placed under the mixin package")
    void everyFileInTheMixinTreeIsAMixin() throws IOException {
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(MIXIN_ROOT)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String text = Files.readString(file, StandardCharsets.UTF_8);
                if (!text.contains("@Mixin(")) {
                    offenders.add(MIXIN_ROOT.relativize(file).toString());
                }
            }
        }
        assertTrue(offenders.isEmpty(),
            "a helper class under the mixin package is owned by the transformer and cannot be loaded directly; put it "
                + "in gameplay/generation/client instead: " + offenders);
    }

    private static Path findSource(String simpleName) throws IOException {
        try (Stream<Path> files = Files.walk(MIXIN_ROOT)) {
            return files.filter(path -> path.getFileName().toString().equals(simpleName + ".java"))
                .findFirst().orElse(null);
        }
    }
}
