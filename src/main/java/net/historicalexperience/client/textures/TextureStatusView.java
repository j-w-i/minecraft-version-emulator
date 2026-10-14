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

import java.util.ArrayList;
import java.util.List;
import net.historicalexperience.client.HistoricalClientRules;
import net.historicalexperience.version.HistoricalVersion;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Turns scan results and the active plan into the text the setup and configuration screens show, so the two screens
 * can never disagree with each other or with what the resource layer actually does.
 */
public final class TextureStatusView {
    public static final String PACK_PAGE_GOLDEN_DAYS = "https://modrinth.com/resourcepack/golden-days";
    public static final String PACK_PAGE_GOLDEN_DAYS_ALPHA = "https://modrinth.com/resourcepack/golden-days-alpha";
    public static final String PACK_PAGE_CONTINUATION = "https://modrinth.com/resourcepack/pacp";

    private TextureStatusView() {
    }

    /** One line per dependency: a tick or cross, the pack's role, and what was found (or why not). */
    public static List<Component> dependencyLines() {
        PackScanner.Result scan = HistoricalTextureService.scan();
        List<Component> lines = new ArrayList<>();
        for (TextureSource source : TextureSource.values()) {
            if (!source.isDependency()) {
                continue;
            }
            Component role = Component.translatable(source.translationKey());
            if (source == TextureSource.PROGRAMMER_ART) {
                boolean present = programmerArtPresent();
                lines.add(line(present, role, present
                    ? Component.translatable("historical_experience.textures.status.builtin")
                    : Component.translatable("historical_experience.textures.status.builtin_missing")));
                continue;
            }
            PackScanner.Found found = scan.of(source);
            if (found != null && found.usable()) {
                String name = found.chosen().getFileName().toString();
                String detail = found.classification().version().isEmpty() ? name
                    : name + " (" + found.classification().version() + ")";
                if (!found.duplicates().isEmpty()) {
                    detail += " +" + found.duplicates().size() + " duplicate";
                }
                lines.add(line(true, role, Component.literal(detail)));
            } else {
                lines.add(line(false, role, missingReason(found)));
            }
        }
        return lines;
    }

    private static Component missingReason(PackScanner.Found found) {
        if (found == null || found.classification().status() == PackClassifier.Status.NOT_A_DEPENDENCY) {
            return Component.translatable("historical_experience.textures.status.missing");
        }
        return switch (found.classification().status()) {
            case WRONG_VARIANT -> Component.translatable("historical_experience.textures.status.wrong_variant");
            case UNSUPPORTED_FORMAT -> Component.translatable("historical_experience.textures.status.unsupported");
            default -> Component.translatable("historical_experience.textures.status.missing");
        };
    }

    private static Component line(boolean ok, Component role, Component detail) {
        return Component.empty()
            .append(Component.literal(ok ? "\u2714 " : "\u2716 ").withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED))
            .append(role.copy().withStyle(ChatFormatting.WHITE))
            .append(Component.literal(" \u2014 ").withStyle(ChatFormatting.DARK_GRAY))
            .append(detail.copy().withStyle(ok ? ChatFormatting.GRAY : ChatFormatting.RED));
    }

    public static boolean programmerArtPresent() {
        Minecraft client = Minecraft.getInstance();
        return client != null && client.getResourcePackRepository().getPack("programmer_art") != null;
    }

    public static boolean allDependenciesPresent() {
        return programmerArtPresent() && HistoricalTextureService.scan().usable().size() == 3;
    }

    /** The version the stack is resolved from, as text, or a note that none is active. */
    public static Component versionLine() {
        HistoricalVersion version = HistoricalClientRules.activeVersion();
        return version == null
            ? Component.translatable("historical_experience.textures.version.none")
            : Component.translatable("historical_experience.textures.version.active", version.displayName());
    }

    /** The effective stack for the active version, lowest priority first, as one line. */
    public static Component stackLine() {
        TexturePlan plan = HistoricalTextureService.desiredPlan();
        if (!HistoricalTextureConfig.enabled()) {
            return Component.translatable("historical_experience.textures.stack.disabled");
        }
        if (!plan.isActive()) {
            return Component.translatable("historical_experience.textures.stack.ordinary");
        }
        StringBuilder out = new StringBuilder();
        for (TextureSource source : plan.sources()) {
            if (source == TextureSource.VANILLA_PIN) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(" \u203A ");
            }
            out.append(Component.translatable(source.translationKey()).getString());
        }
        return Component.literal(out.toString());
    }

    /** What is pinned right now, one line per live rule, so the player can see what is being enforced. */
    public static List<Component> ruleLines() {
        List<Component> lines = new ArrayList<>();
        TexturePlan plan = HistoricalTextureService.desiredPlan();
        if (!HistoricalTextureConfig.enabled() || !plan.isActive()) {
            return lines;
        }
        for (AssetRule rule : plan.rules()) {
            lines.add(Component.translatable("historical_experience.textures.rule." + rule.id())
                .withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }
}
