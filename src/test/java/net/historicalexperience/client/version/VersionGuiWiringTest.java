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
package net.historicalexperience.client.version;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * Guards how the version interface is wired, by reading the source.
 *
 * <p>The defects this exists to make impossible are the ones that look fine on screen. A screen that wrote the
 * metadata document itself would appear to work, and would diverge from {@code /hex upgrade} the first time either
 * changed. A screen that rewrote {@code level.dat} directly would appear to work and would drop one of the three
 * preset references, leaving a world whose terrain and cave biomes disagree. A screen that kept its own list of
 * versions would appear to work until a version was added. None of that is visible in a screenshot.
 *
 * <p>The project already guards wiring this way - see {@code HistoricalLootWiring1211Test} and
 * {@code HistoricalClientSideGateTest} - for the same reason: a passing build says nothing about which code path a
 * feature actually took.
 */
class VersionGuiWiringTest {
    private static final Path GUI = Path.of("src/main/java/net/historicalexperience/client/version");
    private static final Path CLIENT_INIT =
        Path.of("src/main/java/net/historicalexperience/client/HistoricalExperienceClient.java");
    private static final Path MIXIN_CONFIG = Path.of("src/main/resources/historical_experience.mixins.json");

    @Test
    void theInterfaceChangesVersionsOnlyThroughTheSharedService() throws IOException {
        String manage = read(GUI.resolve("ManageWorldHistoricalVersion.java"));

        assertTrue(manage.contains("HistoricalUpgradeService.upgradeUnloadedWorld"),
            "changing a world's version must go through the same service the command calls, so the two cannot "
                + "diverge in what they write");
        assertTrue(manage.contains("HistoricalUpgradeService.inspect"),
            "and it must ask that service what a world is rather than deciding for itself");
    }

    @Test
    void noGuiClassWritesWorldStateItself() throws IOException {
        // Every one of these is a real way for a screen to bypass the transaction the service performs: writing the
        // document, encoding one, editing the level stem, or making a backup by hand.
        List<String> forbidden = List.of(
            "HistoricalMetadataStore",
            "MetadataJson",
            "NbtIo",
            "WorldGenSettingsAdvance",
            "WorldBackup",
            "GenerationUpgrade");

        for (Path file : javaFiles(GUI)) {
            String source = read(file);

            for (String api : forbidden) {
                assertFalse(source.contains(api),
                    file.getFileName() + " names " + api + ". World state is written by the upgrade service and "
                        + "nowhere else; a screen that writes it would skip the backup, the journal and the "
                        + "revision rules, and would disagree with /hex about what it did.");
            }
        }
    }

    @Test
    void theInterfaceNeverBuildsGenerationSettings() throws IOException {
        // The mod ships one world preset per version and this interface selects it. Anything here that assembled a
        // preset, a generator or a settings object would be a second world-generation system.
        List<String> forbidden = List.of("ModernBetaSettingsPreset", "ChunkGenerator", "WorldGenSettings",
            "settings_preset");

        for (Path file : javaFiles(GUI)) {
            String source = read(file);

            for (String api : forbidden) {
                assertFalse(source.contains(api),
                    file.getFileName() + " names " + api + ". Selecting a historical version means selecting the "
                        + "world preset this project already ships; composing generation settings in a screen "
                        + "would duplicate the generator layer.");
            }
        }
    }

    @Test
    void theIndevAdapterDelegatesRatherThanBuildingAGenerator() throws IOException {
        // The version GUI must not know what a generator is, so the one piece of client code that has to touch one
        // lives outside it - and is held to the same rule by its own list. It may READ the settings the selected
        // world type already resolved and hand them to upstream's modifier; it may not construct a generator, resolve
        // or assemble a settings preset, touch noise settings, or read/write settings JSON.
        Path adapter = Path.of("src", "main", "java", "net", "historicalexperience", "client", "indev");
        List<String> forbidden = List.of("new ModernBetaChunkGenerator", "ModernBetaSettingsPreset",
            "WorldGenSettings", "NoiseGeneratorSettings", "settings_preset", "fromCompound", "toCompound",
            "ModernerBetaSettingsPresets", "IRegistryHandler");

        List<Path> files = new java.util.ArrayList<>();
        try (var stream = java.nio.file.Files.walk(adapter)) {
            stream.filter(path -> path.toString().endsWith(".java")).forEach(files::add);
        }
        assertFalse(files.isEmpty(), "the Indev adapter must exist for this check to mean anything");

        for (Path file : files) {
            String source = read(file);
            for (String api : forbidden) {
                assertFalse(source.contains(api),
                    file.getFileName() + " names " + api + ". The Indev options change one component of the settings "
                        + "the chosen world type already resolved and hand it to Moderner Beta's own modifier; "
                        + "anything that assembles a generator or a preset would be a second world-generation system.");
            }
        }

        boolean delegates = false;
        for (Path file : files) {
            if (read(file).contains("ModernBetaWorldScreenProvider.createModifier")) {
                delegates = true;
            }
        }
        assertTrue(delegates, "something in the Indev adapter package must hand its result to upstream's own modifier "
            + "rather than applying settings by hand");
    }

    @Test
    void oneSelectorServesBothWorldCreationAndWorldManagement() throws IOException {
        assertTrue(read(GUI.resolve("CreateWorldHistoricalVersion.java")).contains("HistoricalVersionScreen"),
            "world creation uses the shared selector");
        assertTrue(read(GUI.resolve("ManageWorldHistoricalVersion.java")).contains("HistoricalVersionScreen"),
            "and so does world management, rather than each having its own list of versions");
    }

    @Test
    void bothHooksAreRegisteredOnTheClient() throws IOException {
        String initialiser = read(CLIENT_INIT);

        assertTrue(initialiser.contains("CreateWorldHistoricalVersion.register()"),
            "a screen hook that is never registered is dead code, and the feature would silently not exist");
        assertTrue(initialiser.contains("ManageWorldHistoricalVersion.register()"),
            "the same for changing an existing world's version");
    }

    @Test
    void theWorldRowAccessorIsRegisteredAsAClientMixin() throws IOException {
        String config = read(MIXIN_CONFIG);

        assertTrue(config.contains("WorldListEntryAccessor"),
            "the accessor that exposes a world row's summary must be registered, or the version manager cannot "
                + "resolve which save it is acting on");
        assertTrue(config.toLowerCase(Locale.ROOT).contains("client"),
            "and it belongs to the client list: a dedicated server has no world list");
    }

    @Test
    void versionsComeFromTheCatalogueRatherThanAListInTheScreen() throws IOException {
        for (Path file : javaFiles(GUI)) {
            String source = read(file);

            assertFalse(source.contains("\"Beta 1.") || source.contains("\"Alpha 1."),
                file.getFileName() + " hard-codes a version name. The hierarchy comes from the version catalogue, "
                    + "so adding a version must not require editing a screen.");
        }
    }

    private static List<Path> javaFiles(Path directory) throws IOException {
        try (var stream = Files.list(directory)) {
            return stream.filter(path -> path.toString().endsWith(".java")).toList();
        }
    }

    private static String read(Path path) throws IOException {
        return Files.readString(path);
    }
}
