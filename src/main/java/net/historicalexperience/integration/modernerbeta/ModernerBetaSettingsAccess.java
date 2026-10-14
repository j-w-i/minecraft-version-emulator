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
package net.historicalexperience.integration.modernerbeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import mod.bluestaggo.modernerbeta.settings.ModernBetaSettingsPreset;
import mod.bluestaggo.modernerbeta.settings.SettingsComponentTypes;
import mod.bluestaggo.modernerbeta.registry.ModernBetaResourceKeys;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.chunk.ChunkGenerator;

/**
 * Read-only inspection of Moderner Beta settings presets, on the adapter side of the boundary.
 *
 * <p>Every Moderner Beta type used here stays inside
 * {@code net.historicalexperience.integration.modernerbeta}; callers outside the package receive
 * plain records and strings. This exists so that verification code can assert facts about the
 * project owned preset without importing Moderner Beta itself.
 */
public final class ModernerBetaSettingsAccess {
    /** ResourceLocation of this project's own settings preset. */
    public static final ResourceLocation HISTORICAL_PRESET_ID =
        net.historicalexperience.HistoricalMarker.HISTORICAL_PRESET;

    /** ResourceLocation of the upstream preset this project's preset must inherit from. */
    public static final ResourceLocation UPSTREAM_PRESET_ID =
        net.historicalexperience.HistoricalMarker.UPSTREAM_PRESET;

    private ModernerBetaSettingsAccess() {
    }

    /** Moderner Beta registry key for settings presets, as a plain re-export. */
    public static ResourceKey<Registry<ModernBetaSettingsPreset>> settingsPresetRegistryKey() {
        return ModernBetaResourceKeys.SETTINGS_PRESET;
    }

    /** Plain description of a loaded generator; see {@link ModernerBetaAdapter.GeneratorFacts}. */
    public static ModernerBetaAdapter.GeneratorFacts describe(ChunkGenerator generator) {
        return ModernerBetaAdapter.describeGenerator(generator).facts();
    }

    /** Upstream provider triple, diagnostics only. */
    public static String upstreamBeta173ProviderTriple(ChunkGenerator generator) {
        return ModernerBetaAdapter.upstreamBeta173ProviderTriple(generator);
    }

    /** Whether the loaded generator's provider triple equals upstream's, diagnostics only. */
    public static boolean providersMatchUpstreamBeta173(ChunkGenerator generator) {
        return ModernerBetaAdapter.providersMatchUpstreamBeta173(generator);
    }

    /**
     * A plain-language report about the project owned preset as it exists in a live registry.
     *
     * @param chunkInheritsPreset     the chunk settings' {@code moderner_beta:preset} component
     * @param biomeInheritsPreset     the biome settings' component
     * @param caveBiomeInheritsPreset the cave biome settings' component
     * @param chunkProvider           resolved chunk provider identifier
     * @param biomeProvider           resolved biome provider identifier
     * @param caveBiomeProvider       resolved cave biome provider identifier
     * @param selfCycle               true when any block references the project preset itself,
     *                                which makes {@code ModernBetaSettings.mapPreset} non
     *                                terminating
     * @param resolvable              true when the preset could be resolved at all
     * @param resolutionError         the resolution failure, when there was one
     */
    public record PresetInspection(
        String chunkInheritsPreset,
        String biomeInheritsPreset,
        String caveBiomeInheritsPreset,
        String chunkProvider,
        String biomeProvider,
        String caveBiomeProvider,
        boolean selfCycle,
        boolean resolvable,
        String resolutionError
    ) {
        /** True when all three settings blocks inherit the pinned upstream preset. */
        public boolean inheritsUpstream() {
            String expected = UPSTREAM_PRESET_ID.toString();
            return expected.equals(chunkInheritsPreset)
                && expected.equals(biomeInheritsPreset)
                && expected.equals(caveBiomeInheritsPreset);
        }
    }

    /**
     * Inspects the project owned preset in a live settings preset registry.
     *
     * <p>Resolution goes through {@link ModernBetaSettingsPreset#getPreset(ResourceLocation,
     * net.minecraft.core.HolderGetter)}, then through
     * {@link ModernBetaSettingsPreset#mapped(net.minecraft.core.HolderGetter)}, which is the same
     * inheritance walk a level load performs. A self reference is reported rather than followed,
     * because following it would not terminate.
     */
    public static PresetInspection inspectHistoricalPreset(
        Registry<ModernBetaSettingsPreset> presetRegistry
    ) {
        Objects.requireNonNull(presetRegistry, "presetRegistry");

        // 1.21.1's Registry.get(ResourceLocation) returns the value, nullable; 26.3 returned a Holder.
        ModernBetaSettingsPreset raw = presetRegistry.get(HISTORICAL_PRESET_ID);
        if (raw == null) {
            return new PresetInspection("<absent>", "<absent>", "<absent>", "<absent>", "<absent>",
                "<absent>", false, false,
                "preset '" + HISTORICAL_PRESET_ID + "' is not registered");
        }

        String chunkPreset = String.valueOf(
            raw.chunkSettings().get(SettingsComponentTypes.PRESET));
        String biomePreset = String.valueOf(
            raw.biomeSettings().get(SettingsComponentTypes.PRESET));
        String cavePreset = String.valueOf(
            raw.caveBiomeSettings().get(SettingsComponentTypes.PRESET));

        boolean selfCycle = HISTORICAL_PRESET_ID.toString().equals(chunkPreset)
            || HISTORICAL_PRESET_ID.toString().equals(biomePreset)
            || HISTORICAL_PRESET_ID.toString().equals(cavePreset);
        if (selfCycle) {
            return new PresetInspection(chunkPreset, biomePreset, cavePreset, "<unresolved>",
                "<unresolved>", "<unresolved>", true, false,
                "self referential preset would loop in mapPreset; not followed");
        }

        try {
            ModernBetaSettingsPreset mapped = raw.mapped(presetRegistry.asLookup());
            return new PresetInspection(
                chunkPreset,
                biomePreset,
                cavePreset,
                String.valueOf(mapped.chunkSettings().getProvider()),
                String.valueOf(mapped.biomeSettings().getProvider()),
                String.valueOf(mapped.caveBiomeSettings().getProvider()),
                false,
                true,
                "");
        } catch (RuntimeException e) {
            return new PresetInspection(chunkPreset, biomePreset, cavePreset, "<unresolved>",
                "<unresolved>", "<unresolved>", false, false,
                e.getClass().getName() + ": " + e.getMessage());
        }
    }

    /**
     * Eagerly resolves a preset's inheritance for unit-style checks that do not have a server.
     * Present so callers can enumerate the three inherited identifiers as a plain list.
     */
    public static List<String> inheritedPresetIds(ModernBetaSettingsPreset preset) {
        List<String> ids = new ArrayList<>(3);
        ids.add(String.valueOf(preset.chunkSettings().get(SettingsComponentTypes.PRESET)));
        ids.add(String.valueOf(preset.biomeSettings().get(SettingsComponentTypes.PRESET)));
        ids.add(String.valueOf(preset.caveBiomeSettings().get(SettingsComponentTypes.PRESET)));
        return ids;
    }
}
