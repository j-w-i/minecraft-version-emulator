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

import java.util.Objects;

import mod.bluestaggo.modernerbeta.level.biome.ModernBetaBiomeSource;
import mod.bluestaggo.modernerbeta.level.chunk.ModernBetaChunkGenerator;
import mod.bluestaggo.modernerbeta.settings.ModernBetaSettingsPreset;
import net.minecraft.core.HolderGetter;
import net.minecraft.world.level.chunk.ChunkGenerator;

/**
 * Precise (non-heuristic) characterisation of the Moderner Beta generator loaded for a world.
 *
 * <p>This exists for diagnostics only. It deliberately does <em>not</em> decide whether a world is
 * historical: provider identities are shared between many Moderner Beta presets, so a provider
 * match cannot prove which preset a world was created with. The only authoritative record of
 * historical intent is the persisted metadata document, which is written explicitly and never
 * inferred. Nothing in this mod auto-tags a world based on generator identity.
 */
public final class ModernerBetaGeneratorIdentity {
    private ModernerBetaGeneratorIdentity() {
    }

    /**
     * A comparable description of the provider configuration actually loaded for a generator.
     */
    public record Identity(
        String generatorClass,
        String chunkProvider,
        String biomeProvider,
        String caveBiomeProvider
    ) {
        public Identity {
            Objects.requireNonNull(generatorClass, "generatorClass");
            Objects.requireNonNull(chunkProvider, "chunkProvider");
            Objects.requireNonNull(biomeProvider, "biomeProvider");
            Objects.requireNonNull(caveBiomeProvider, "caveBiomeProvider");
        }
    }

    /** True when the generator is a Moderner Beta generator at all. */
    public static boolean isModernerBeta(ChunkGenerator generator) {
        return generator instanceof ModernBetaChunkGenerator;
    }

    /**
     * Describes the loaded generator's provider configuration.
     *
     * @return the identity, or {@code null} when the generator is not a Moderner Beta generator
     */
    public static Identity describe(ChunkGenerator generator) {
        if (!(generator instanceof ModernBetaChunkGenerator modernBeta)) {
            return null;
        }
        String biomeProvider = "<not-a-moderner-beta-biome-source>";
        String caveBiomeProvider = "<not-a-moderner-beta-biome-source>";
        if (modernBeta.getBiomeSource() instanceof ModernBetaBiomeSource biomeSource) {
            biomeProvider = String.valueOf(biomeSource.getBiomeSettings().getProvider());
            caveBiomeProvider = String.valueOf(biomeSource.getCaveBiomeSettings().getProvider());
        }
        return new Identity(
            generator.getClass().getName(),
            String.valueOf(modernBeta.getChunkSettings().getProvider()),
            biomeProvider,
            caveBiomeProvider);
    }

    /** Describes the provider configuration produced by a preset. */
    public static Identity describePreset(ModernBetaSettingsPreset preset) {
        Objects.requireNonNull(preset, "preset");
        return new Identity(
            "<preset>",
            String.valueOf(preset.chunkSettings().getProvider()),
            String.valueOf(preset.biomeSettings().getProvider()),
            String.valueOf(preset.caveBiomeSettings().getProvider()));
    }

    /**
     * Compares only the provider triple of two identities, ignoring the generator class.
     *
     * <p>Diagnostic only. A {@code true} result means "same providers", never "same preset".
     */
    public static boolean providersEqual(Identity loaded, Identity preset) {
        Objects.requireNonNull(loaded, "loaded");
        Objects.requireNonNull(preset, "preset");
        return loaded.chunkProvider().equals(preset.chunkProvider())
            && loaded.biomeProvider().equals(preset.biomeProvider())
            && loaded.caveBiomeProvider().equals(preset.caveBiomeProvider());
    }

    /** Convenience: resolves the pinned upstream preset and describes its providers. */
    public static Identity upstreamBeta173Providers(
        HolderGetter<ModernBetaSettingsPreset> presetRegistry
    ) {
        return describePreset(ModernerBetaAdapter.resolveUpstreamBeta173(presetRegistry));
    }
}
