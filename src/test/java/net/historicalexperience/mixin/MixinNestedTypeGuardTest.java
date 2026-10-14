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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Forbids a nested type inside a mixin class, because referencing one is fatal at RUNTIME.
 *
 * <h2>The defect this exists to prevent, which crashed the owner's client</h2>
 *
 * <p>{@code HistoricalFoodStackLimit} used to be a nested helper inside {@code ContainerHistoricalFoodStackMixin},
 * referenced by name from three other mixins. It compiled, the mixin applied, the game launched, a world loaded -
 * and then the first container click in a Beta 1.2 world died:
 *
 * <pre>
 * IllegalClassLoadError: net.historicalexperience.mixin.ContainerHistoricalFoodStackMixin$HistoricalFoodStackLimit
 * is in a defined mixin package net.historicalexperience.mixin.* owned by historical_experience.mixins.json and
 * cannot be referenced directly
 * </pre>
 *
 * <p>The cause is not "nested classes are bad". The mixin config declares
 * {@code "package": "net.historicalexperience.mixin"}, which makes the transformer claim <b>every</b> class in that
 * package: only it may load them, so ordinary code cannot reach a nested member no matter how it is named. Nothing
 * before the actual call site exercises that.
 *
 * <h2>Why this is a source scan rather than an assertion about loaded classes</h2>
 *
 * <p>Loading a mixin class needs Minecraft's registries bootstrapped, which a plain unit test cannot do - the same
 * wall this project has hit on {@code ItemStack}, {@code Blocks} and {@code SoundEvents}. The property being tested
 * is structural ("does this file declare a nested type?"), so reading the source is not a weaker check here; it is
 * the only one available, and it is exactly the property that must hold.
 *
 * <h2>What is still legal, and should stay legal</h2>
 *
 * <p>Referencing a TOP-LEVEL mixin from outside is fine and used deliberately: {@code SheepAccess},
 * {@code AxeAccess}, {@code GuiAccess} and {@code NaturalSpawnerProbeAccessor} are accessor/invoker interfaces that
 * ordinary gameplay code calls on purpose. This test does not forbid those, because forbidding them would break
 * working code for no reason - the illegal shape is specifically a nested type.
 */
class MixinNestedTypeGuardTest {
    /** Matches a type declaration, capturing the keyword, with leading whitespace (so a top-level one is excluded). */
    private static final Pattern NESTED_TYPE = Pattern.compile(
        "^\\s+(?:(?:public|protected|private|static|final|abstract|sealed|non-sealed)\\s+)*"
            + "(class|interface|enum|record|@interface)\\s+(\\w+)");

    private static Path mixinSourceDir() {
        // The test runs with the repository root as its working directory (see build.gradle's `workingDir`).
        return Path.of("src", "main", "java", "net", "historicalexperience", "mixin");
    }

    @Test
    @DisplayName("no mixin class declares a nested type, because referencing one is fatal at runtime")
    void mixinsDeclareNoNestedTypes() throws IOException {
        Path dir = mixinSourceDir();
        assertTrue(Files.isDirectory(dir), "the mixin source directory must exist at " + dir.toAbsolutePath());

        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(dir)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                List<String> lines = Files.readAllLines(file);
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    // A javadoc line is not code. The guard's own documentation quotes the fatal class name, and
                    // matching that would make this test fail on its own explanation.
                    String trimmed = line.trim();
                    if (trimmed.startsWith("*") || trimmed.startsWith("//") || trimmed.startsWith("/*")) {
                        continue;
                    }
                    Matcher matcher = NESTED_TYPE.matcher(line);
                    if (matcher.find()) {
                        offenders.add(file.getFileName() + ":" + (i + 1) + " declares nested "
                            + matcher.group(1) + " " + matcher.group(2));
                    }
                }
            }
        }

        assertTrue(offenders.isEmpty(),
            "A nested type inside a mixin cannot be referenced from ordinary code: the mixin config claims every "
                + "class in the mixin package, so the reference throws IllegalClassLoadError at the CALL SITE - "
                + "after a successful compile, a successful mixin apply and a successful world load. Move the shared "
                + "logic to net.historicalexperience.gameplay instead. Offenders: " + offenders);
    }

    @Test
    @DisplayName("the scan actually detects a nested type, so a passing result means something")
    void theScanIsNotVacuous() {
        // A guard that cannot fail is worse than no guard, and this project has shipped one of those before - a
        // damage range test that compared an expression with itself. So the pattern is proved against a sample that
        // must match and one that must not.
        assertTrue(NESTED_TYPE.matcher("    final class Helper {").find(),
            "the pattern must match a nested class declaration");
        assertTrue(NESTED_TYPE.matcher("  public static final class Helper {").find(),
            "the pattern must match a nested class with modifiers");
        assertTrue(NESTED_TYPE.matcher("    private record Helper(int a) {").find(),
            "the pattern must match a nested record");
        assertFalse(NESTED_TYPE.matcher("public class SomeMixin {").find(),
            "the pattern must NOT match a top-level class, which is the normal and legal shape");
        assertFalse(NESTED_TYPE.matcher("    private static boolean helper() {").find(),
            "the pattern must NOT match a method");
    }
}
