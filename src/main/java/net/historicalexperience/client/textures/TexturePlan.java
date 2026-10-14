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
package net.historicalexperience.client.textures;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The resolved texture stack for one version: which sources are layered and which asset rules are live.
 *
 * <p>The plan is pure data. {@link #serves} is the single question the composition layer asks of it, and it is also
 * what the tests assert, so the behaviour that is verified is the behaviour that ships.
 *
 * @param chronology the version's chronology key, or {@code Integer.MIN_VALUE} for {@link #NONE}
 * @param sources    the layered sources, lowest priority first
 * @param rules      the rules that apply at this chronology, in priority order
 */
public record TexturePlan(int chronology, List<TextureSource> sources, List<AssetRule> rules) {
    /** No historical textures: ordinary worlds, and every case where the version is unknown. */
    public static final TexturePlan NONE = new TexturePlan(Integer.MIN_VALUE, List.of(), List.of());

    public boolean isActive() {
        return !sources.isEmpty();
    }

    /** Whether {@code source} supplies {@code namespace:path} under this plan. */
    public boolean serves(TextureSource source, String namespace, String path) {
        if (!sources.contains(source)) {
            return false;
        }
        Set<TextureSource> excluded = EnumSet.noneOf(TextureSource.class);
        TextureSource preferred = null;
        boolean pinned = false;
        boolean matched = false;
        for (AssetRule rule : rules) {
            if (!rule.matches(namespace, path)) {
                continue;
            }
            matched = true;
            excluded.addAll(rule.excluded());
            pinned |= rule.pinToVanilla();
        }
        if (matched) {
            // Second pass so an exclusion declared by a later rule still beats an earlier preference.
            for (AssetRule rule : rules) {
                if (rule.matches(namespace, path) && rule.preferred() != null
                    && !excluded.contains(rule.preferred())) {
                    preferred = rule.preferred();
                    break;
                }
            }
        }
        if (source == TextureSource.VANILLA_PIN) {
            // Vanilla joins the group only for resources a rule pinned; everything else is left to the user's packs
            // and the game exactly as an ordinary world would have it.
            return pinned;
        }
        if (excluded.contains(source)) {
            return false;
        }
        return preferred == null || source.rank() <= preferred.rank();
    }

    /** The source that wins {@code namespace:path} among {@code available}, or null when none of them serves it. */
    public TextureSource winner(Set<TextureSource> available, String namespace, String path) {
        TextureSource best = null;
        for (TextureSource source : sources) {
            if (available.contains(source) && serves(source, namespace, path)
                && (best == null || source.rank() > best.rank())) {
                best = source;
            }
        }
        return best;
    }

    /**
     * A short identity of the effective stack. Two adjacent versions with the same signature need no resource
     * reload between them, which is what keeps version switching from reloading when nothing visible changed.
     */
    public String signature() {
        if (!isActive()) {
            return "none";
        }
        String stack = sources.stream().map(TextureSource::id).collect(Collectors.joining("+"));
        String rule = rules.stream().map(AssetRule::id).collect(Collectors.joining(","));
        return stack + "|" + rule;
    }
}
