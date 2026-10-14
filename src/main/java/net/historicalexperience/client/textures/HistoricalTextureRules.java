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
import java.util.Set;
import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;

/**
 * The declared texture policy: which whole packs are layered for a version, and which per-asset exceptions apply.
 *
 * <h2>Whole-pack layering (the owner's table)</h2>
 *
 * <ul>
 *   <li>Programmer Art and Programmer Art Continuation: every historical version.</li>
 *   <li>Golden Days Base: every version through Release 1.2.5, none afterwards.</li>
 *   <li>Golden Days Alpha: every Alpha 1.2.x and earlier version (pre-Alpha, Infdev and early Alpha included), none
 *       from Beta 1.0.</li>
 * </ul>
 *
 * <p>Every boundary is a chronology key from {@link HistoricalVersions#chronology}, never a name, so a snapshot or an
 * intermediate build is placed by when it is.
 *
 * <h2>Evidence for each boundary (inspected, not assumed)</h2>
 *
 * <ul>
 *   <li><b>Alpha 1.2.</b> The art Golden Days Alpha supplies for grass and leaves is, pixel for pixel, the Alpha
 *       v1.1.2_01 {@code terrain.png} art ({@code grass_block_top} against tile 0, {@code oak_leaves} against tiles
 *       52 and 53, mean colour distance 0.0). A diff of the original clients' {@code terrain.png} shows tiles 0, 14, 52,
 *       53, 102-105, 112, 118-120 and 128 first change in Alpha v1.2.x, which is the wiki's "Grass block / Leaves:
 *       texture changed" in Alpha v1.2.0. So the pack's art is strictly pre-1.2.0; the owner's rule extends it through
 *       1.2.x, and that is what is implemented, as one named constant ({@link #GOLDEN_DAYS_ALPHA_END}). Nothing is
 *       declared between Alpha 1.2.0 and Beta 1.0, so every declared Alpha build falls on the "with Alpha" side.</li>
 *   <li><b>Release 1.2.5.</b> The owner's rule. The window ends one chronology step after 1.2.5 so no later build is
 *       caught.</li>
 *   <li><b>Cobblestone, Beta 1.7.</b> The cobblestone tile in {@code terrain.png} is identical from Alpha v1.0.11
 *       through Beta 1.6.6 and changes in Beta 1.7.3 (wiki: "1.7: The texture of cobblestone has been changed").
 *       Golden Days Base's {@code cobblestone.png} equals the pre-1.7 tile and Programmer Art's equals the 1.7-onward
 *       tile, both exactly. So "Golden Days through Beta 1.6, Programmer Art afterwards" is the real transition, and
 *       Beta 1.6.x stays on the Golden Days side.</li>
 *   <li><b>XP bar.</b> Golden Days Base overrides {@code gui/sprites/hud/experience_bar_background.png} and
 *       {@code experience_bar_progress.png}. Neither Programmer Art nor Continuation supplies them in this game
 *       version, so with Golden Days excluded the answer is the game's own sprite; vanilla is pinned into the
 *       historical group so a user's pack cannot replace it either.</li>
 * </ul>
 *
 * <p>The rules are listed in priority order; see {@link AssetRule} for how conflicts resolve.
 */
public final class HistoricalTextureRules {
    /** First chronology after Alpha 1.2.x: Golden Days Alpha is layered for everything before this. */
    public static final int GOLDEN_DAYS_ALPHA_END =
        HistoricalVersions.chronology(HistoricalVersions.ERA_ALPHA, 1, 3, 0);

    /** One step after Release 1.2.5: Golden Days Base is layered for everything before this. */
    public static final int GOLDEN_DAYS_BASE_END =
        HistoricalVersions.chronology(HistoricalVersions.ERA_RELEASE, 1, 2, 5) + 1;

    /** Beta 1.7, where the cobblestone texture changed. Beta 1.6.x is before it. */
    public static final int COBBLESTONE_CHANGE =
        HistoricalVersions.chronology(HistoricalVersions.ERA_BETA, 1, 7, 0);

    private static final String MINECRAFT = "minecraft";

    /** The two GUI sprites Golden Days uses for its experience bar in this game version. */
    public static final Set<String> XP_BAR_PATHS = Set.of(
        "textures/gui/sprites/hud/experience_bar_background.png",
        "textures/gui/sprites/hud/experience_bar_progress.png");

    public static final Set<String> COBBLESTONE_PATHS = Set.of("textures/block/cobblestone.png");

    public static final AssetRule XP_BAR = new AssetRule(
        "xp_bar_never_golden_days", MINECRAFT, XP_BAR_PATHS, VersionWindow.ALWAYS,
        Set.of(TextureSource.GOLDEN_DAYS_BASE, TextureSource.GOLDEN_DAYS_ALPHA), null, true,
        "The Golden Days experience bar is never used in any historical version.");

    public static final AssetRule COBBLESTONE_GOLDEN_DAYS = new AssetRule(
        "cobblestone_golden_days", MINECRAFT, COBBLESTONE_PATHS, VersionWindow.before(COBBLESTONE_CHANGE),
        Set.of(), TextureSource.GOLDEN_DAYS_BASE, false,
        "Through Beta 1.6 cobblestone is the pre-Beta-1.7 texture, which Golden Days Base carries.");

    public static final AssetRule COBBLESTONE_PROGRAMMER_ART = new AssetRule(
        "cobblestone_programmer_art", MINECRAFT, COBBLESTONE_PATHS, VersionWindow.from(COBBLESTONE_CHANGE),
        Set.of(TextureSource.GOLDEN_DAYS_BASE, TextureSource.GOLDEN_DAYS_ALPHA), TextureSource.PROGRAMMER_ART, false,
        "From Beta 1.7 cobblestone is Programmer Art's, even while Golden Days stays active for other textures; "
            + "Continuation is stepped over so it cannot replace it.");

    /** Every rule, in priority order. */
    public static final List<AssetRule> ALL = List.of(XP_BAR, COBBLESTONE_GOLDEN_DAYS, COBBLESTONE_PROGRAMMER_ART);

    private HistoricalTextureRules() {
    }

    /** The plan for a version, or {@link TexturePlan#NONE} for an ordinary world (null). */
    public static TexturePlan planFor(HistoricalVersion version) {
        return version == null ? TexturePlan.NONE : planFor(version.chronology());
    }

    /** The plan for a chronology key. */
    public static TexturePlan planFor(int chronology) {
        List<TextureSource> sources = new ArrayList<>();
        List<AssetRule> rules = new ArrayList<>();
        for (AssetRule rule : ALL) {
            if (rule.activeAt(chronology)) {
                rules.add(rule);
            }
        }
        if (rules.stream().anyMatch(AssetRule::pinToVanilla)) {
            sources.add(TextureSource.VANILLA_PIN);
        }
        sources.add(TextureSource.PROGRAMMER_ART);
        sources.add(TextureSource.CONTINUATION);
        if (chronology < GOLDEN_DAYS_BASE_END) {
            sources.add(TextureSource.GOLDEN_DAYS_BASE);
        }
        if (chronology < GOLDEN_DAYS_ALPHA_END) {
            sources.add(TextureSource.GOLDEN_DAYS_ALPHA);
        }
        return new TexturePlan(chronology, List.copyOf(sources), List.copyOf(rules));
    }
}
