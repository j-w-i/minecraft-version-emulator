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

import net.historicalexperience.version.Milestones;

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
 *       texture changed" in Alpha v1.2.0. So the pack's art is strictly pre-1.2.0, and the owner's rule is that it
 *       applies only to Alpha versions BELOW Alpha 1.2 - Alpha 1.2.0 itself is on the other side, as one named constant
 *       ({@link #GOLDEN_DAYS_ALPHA_END}). That boundary is also where the game's own colouring changed: Alpha v1.2.0 is
 *       the Halloween Update, which introduced biome-based weather and the {@code grasscolor}/{@code foliagecolor}
 *       lookups, so the pre-1.2 art and the pre-1.2 colouring stop in the same build. Nothing is declared between Alpha
 *       1.2.0 and Beta 1.0, so every declared build below Alpha 1.2.0 falls on the "with Alpha" side and Alpha 1.2.0
 *       falls on the other.</li>
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
    /**
     * Alpha 1.2.0 itself: Golden Days Alpha is layered for everything strictly before this.
     *
     * <p>The owner's rule is "only apply to Alpha versions below Alpha 1.2", so 1.2.0 is the first build WITHOUT it. That
     * matches the art, which is pre-1.2.0 pixel for pixel, and it matches the colouring, because Alpha v1.2.0 is the
     * Halloween Update that introduced biome-based weather and the colour maps.
     */
    public static final int GOLDEN_DAYS_ALPHA_END =
        Milestones.ALPHA_1_2_0.key();

    /** One step after Release 1.2.5: Golden Days Base is layered for everything before this. */
    public static final int GOLDEN_DAYS_BASE_END =
        Milestones.RELEASE_1_2_5.after().key();

    /** Beta 1.7, where the cobblestone texture changed. Beta 1.6.x is before it. */
    public static final int COBBLESTONE_CHANGE =
        Milestones.BETA_1_7.key();

    /** Beta 1.8, where the mossy cobblestone texture changed. Beta 1.7.x is before it. */
    public static final int MOSSY_COBBLESTONE_CHANGE =
        Milestones.BETA_1_8.key();

    /** Beta 1.9 Prerelease 4, where the End portal frame was redrawn in dark green and yellow. */
    public static final int END_PORTAL_FRAME_REDRAW =
        Milestones.BETA_1_9_PRE_4.key();

    /** One step after Beta 1.8.1: the Golden Days chest is used up to and including 1.8.1, and not after it. */
    public static final int GOLDEN_DAYS_CHEST_END =
        Milestones.BETA_1_8_1.after().key();

    private static final String MINECRAFT = "minecraft";

    /** The two GUI sprites Golden Days uses for its experience bar in this game version. */
    public static final Set<String> XP_BAR_PATHS = Set.of(
        "textures/gui/sprites/hud/experience_bar_background.png",
        "textures/gui/sprites/hud/experience_bar_progress.png");

    public static final Set<String> COBBLESTONE_PATHS = Set.of("textures/block/cobblestone.png");

    public static final Set<String> MOSSY_COBBLESTONE_PATHS = Set.of("textures/block/mossy_cobblestone.png");

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

    /**
     * Mossy cobblestone through Beta 1.7: Golden Days Base's art is, pixel for pixel, the mossy tile that Alpha v1.0.11
     * through Beta 1.7.3 shipped (mean channel distance 0.0), while Programmer Art's is the Beta 1.8 art. Note this is
     * the opposite pairing to plain cobblestone, which is why the two need separate boundaries and cannot share a rule.
     */
    public static final AssetRule MOSSY_COBBLESTONE_GOLDEN_DAYS = new AssetRule(
        "mossy_cobblestone_golden_days", MINECRAFT, MOSSY_COBBLESTONE_PATHS,
        VersionWindow.before(MOSSY_COBBLESTONE_CHANGE),
        Set.of(TextureSource.PROGRAMMER_ART), TextureSource.GOLDEN_DAYS_BASE, false,
        "Through Beta 1.7 mossy cobblestone is the pre-Beta-1.8 texture, which Golden Days Base carries.");

    /**
     * Mossy cobblestone from Beta 1.8: Programmer Art carries exactly the Beta 1.8 art. Golden Days Base stops being
     * layered after Release 1.2.5 anyway, so this rule only has to beat Continuation from there on.
     */
    public static final AssetRule MOSSY_COBBLESTONE_PROGRAMMER_ART = new AssetRule(
        "mossy_cobblestone_programmer_art", MINECRAFT, MOSSY_COBBLESTONE_PATHS,
        VersionWindow.from(MOSSY_COBBLESTONE_CHANGE),
        Set.of(TextureSource.GOLDEN_DAYS_BASE, TextureSource.GOLDEN_DAYS_ALPHA), TextureSource.PROGRAMMER_ART, false,
        "From Beta 1.8 mossy cobblestone is Programmer Art's, whose art matches that build exactly.");

    /** The two glowing-eye overlays Golden Days redraws in green; the originals were purple. */
    public static final Set<String> ENDER_EYE_PATHS = Set.of(
        "textures/entity/enderman/enderman_eyes.png",
        "textures/entity/enderdragon/dragon_eyes.png");

    /**
     * Golden Days draws the enderman's and the dragon's eyes green. The originals were purple, and so is the game's
     * own overlay, so the pack is simply not allowed to serve them: the next layer down - or the game - does.
     * Never windowed: no version this project declares had green eyes.
     */
    public static final AssetRule ENDER_EYES_NEVER_GOLDEN_DAYS = new AssetRule(
        "ender_eyes_never_golden_days", MINECRAFT, ENDER_EYE_PATHS, VersionWindow.ALWAYS,
        Set.of(TextureSource.GOLDEN_DAYS_BASE, TextureSource.GOLDEN_DAYS_ALPHA), null, false,
        "Golden Days' green enderman and dragon eyes are not what any version drew; the eyes were purple.");

    /**
     * The frame's textures AND its two models. Both halves are needed: Golden Days' models name
     * {@code end_portal_frame_empty} and {@code end_portal_frame_bottom}, which the game does not have (its own frame
     * uses {@code end_portal_frame_top}). Excluding only the textures left Golden Days' model in place pointing at
     * nothing, and the frame drew with missing-texture magenta on top - found in a screenshot, not by a test.
     */
    public static final Set<String> END_PORTAL_FRAME_PATHS = Set.of(
        "textures/block/end_portal_frame_bottom.png",
        "textures/block/end_portal_frame_empty.png",
        "textures/block/end_portal_frame_eye.png",
        "textures/block/end_portal_frame_side.png",
        "models/block/end_portal_frame.json",
        "models/block/end_portal_frame_filled.json");

    /**
     * The End portal frame keeps Golden Days' art only until Beta 1.9 Prerelease 4, which redrew it (wiki: "The End
     * portal now has a dark green and yellow texture"). From that build the pack stops serving the four textures.
     */
    public static final AssetRule END_PORTAL_FRAME_AFTER_REDRAW = new AssetRule(
        "end_portal_frame_not_golden_days_from_pre4", MINECRAFT, END_PORTAL_FRAME_PATHS,
        VersionWindow.from(END_PORTAL_FRAME_REDRAW),
        Set.of(TextureSource.GOLDEN_DAYS_BASE, TextureSource.GOLDEN_DAYS_ALPHA), null, false,
        "Beta 1.9 Prerelease 4 redrew the End portal frame, so Golden Days' version of it ends there.");

    /**
     * Everything Golden Days supplies for the ender chest: the in-world texture, the three block faces and the two
     * item definitions that point at its own model. All of them, because serving the model without the texture it was
     * drawn for (or the other way round) would leave a chest that matches neither the pack nor the game.
     */
    public static final Set<String> ENDER_CHEST_PATHS = Set.of(
        "textures/entity/chest/ender.png",
        "textures/block/ender_chest_front.png",
        "textures/block/ender_chest_side.png",
        "textures/block/ender_chest_top.png",
        "models/item/ender_chest.json",
        "items/ender_chest.json");

    public static final AssetRule ENDER_CHEST_NEVER_GOLDEN_DAYS = new AssetRule(
        "ender_chest_never_golden_days", MINECRAFT, ENDER_CHEST_PATHS, VersionWindow.ALWAYS,
        Set.of(TextureSource.GOLDEN_DAYS_BASE, TextureSource.GOLDEN_DAYS_ALPHA), null, false,
        "The owner removed Golden Days' ender chest for every version.");

    /**
     * The chest's in-world textures AND the item models and block faces that go with them. Golden Days' item models
     * name {@code block/chest_front}, {@code chest_side}, {@code chest_top} and their trapped and Christmas variants,
     * none of which the game has; leaving the models while dropping the textures (or the reverse) would show a chest
     * that is half one pack and half the other, or missing-texture magenta.
     */
    public static final Set<String> CHEST_PATHS = Set.of(
        "models/item/chest.json", "models/item/chest_christmas.json",
        "models/item/trapped_chest.json", "models/item/trapped_chest_christmas.json",
        "items/chest.json", "items/trapped_chest.json",
        "textures/block/chest_front.png", "textures/block/chest_side.png", "textures/block/chest_top.png",
        "textures/block/christmas_chest_front.png", "textures/block/christmas_chest_side.png",
        "textures/block/christmas_chest_top.png",
        "textures/block/trapped_chest_front.png", "textures/block/trapped_chest_front_christmas.png",
        "textures/entity/chest/normal.png", "textures/entity/chest/normal_left.png",
        "textures/entity/chest/normal_right.png",
        "textures/entity/chest/trapped.png", "textures/entity/chest/trapped_left.png",
        "textures/entity/chest/trapped_right.png",
        "textures/entity/chest/christmas.png", "textures/entity/chest/christmas_left.png",
        "textures/entity/chest/christmas_right.png");

    /** Golden Days' chest ends after Beta 1.8.1; later versions use the game's own chest. */
    public static final AssetRule CHEST_NOT_GOLDEN_DAYS_AFTER_1_8_1 = new AssetRule(
        "chest_not_golden_days_after_1_8_1", MINECRAFT, CHEST_PATHS, VersionWindow.from(GOLDEN_DAYS_CHEST_END),
        Set.of(TextureSource.GOLDEN_DAYS_BASE, TextureSource.GOLDEN_DAYS_ALPHA), null, false,
        "The owner ended Golden Days' chest after Beta 1.8.1.");

    /** Every rule, in priority order. */
    public static final List<AssetRule> ALL = List.of(XP_BAR, COBBLESTONE_GOLDEN_DAYS, COBBLESTONE_PROGRAMMER_ART,
        MOSSY_COBBLESTONE_GOLDEN_DAYS, MOSSY_COBBLESTONE_PROGRAMMER_ART, ENDER_EYES_NEVER_GOLDEN_DAYS,
        END_PORTAL_FRAME_AFTER_REDRAW, ENDER_CHEST_NEVER_GOLDEN_DAYS, CHEST_NOT_GOLDEN_DAYS_AFTER_1_8_1);

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
