/*
 * Minecraft Historical Experience
 * Copyright (C) 2026 Minecraft Historical Experience contributors
 *
 * Contains material copied or adapted from Nostalgic Tweaks
 * (https://github.com/Nostalgica-Reverie/Nostalgic-Tweaks), Copyright (C) Adrenix and
 * Nostalgic Tweaks contributors, licensed LGPL-3.0-only, and MODIFIED for this project.
 * The upstream file or tweak this class came from is named in its documentation comment.
 *
 * This file is part of a work distributed under the GNU Lesser General Public License,
 * version 3. See LICENSE and THIRD_PARTY_NOTICES.md.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later AND LGPL-3.0-only
 */
package net.historicalexperience.client.tooltip;

import net.historicalexperience.client.HistoricalClientRules;
import net.historicalexperience.version.HistoricalVersion;
import net.historicalexperience.version.HistoricalVersions;

/**
 * When tooltips exist, and which shape they have, for the active historical version.
 *
 * <p>Ported from Nostalgic Tweaks' two tooltip tweaks - {@code CandyTweak.OLD_NO_ITEM_TOOLTIPS} ("Disables rendering of
 * tooltips when hovering over items", its {@code item_tooltip/AbstractContainerScreenMixin}) and
 * {@code CandyTweak.OLD_TOOLTIP_BOXES} ("Renders the old semi-transparent black background", its
 * {@code TooltipListener.render} and the fabric {@code tooltip_background/TooltipRenderUtilMixin}). Upstream exposes
 * both as player settings with a global default; here they are answers about a version, because this project gates by
 * version rather than by a config flag, and the boundaries below are the owner's:
 *
 * <ul>
 *   <li><b>Before Beta 1.0</b>: item tooltips did not exist, so none are drawn.</li>
 *   <li><b>Beta 1.0 to Beta 1.9 Prerelease 1</b>: the legacy tooltip - a flat semi-transparent black box, drawn at
 *       {@code #000000C0} exactly as upstream's own default, with no modern border.</li>
 *   <li><b>Beta 1.9 Prerelease 2 onwards</b>: the modern tooltip. This is "the tweak disabled from that point onwards",
 *       so nothing is substituted and the game draws its own box.</li>
 * </ul>
 *
 * <p>A version that is not historical (an ordinary world, or no world) is never touched.
 */
public final class HistoricalTooltips {
    /**
     * NT {@code TOOLTIP_BACKGROUND_COLOR}'s default, {@code "#000000C0"}: black at 75% alpha. Kept as upstream's value
     * so the legacy box matches NT's own rendering rather than a re-invented colour.
     */
    public static final int LEGACY_BACKGROUND = 0xC0000000;

    /** Where tooltips begin: the owner's "tooltips were added in Beta 1.0". */
    private static final int TOOLTIPS_ADDED = HistoricalVersions.chronology(HistoricalVersions.ERA_BETA, 1, 0, 0);

    /** Where the legacy box ends: "modern tooltips from Beta 1.9 Pre-release 2". */
    private static final int MODERN_TOOLTIPS_FROM =
        HistoricalVersions.chronology(HistoricalVersions.ERA_BETA, 1, 9, 2);

    /**
     * Where the item tooltip gained its attribute block (Attack Damage, Speed, and later the hand header).
     *
     * <p>Verified against the clients themselves rather than from memory: {@code generic.attackDamage},
     * {@code attribute.modifier.plus.0} and the {@code attribute.name.*} family are absent from Beta 1.7.3 and
     * Release 1.5 - searched across every class and lang file - and present in Release 1.6.1 and 1.6.4. The attribute
     * system shipped in Release 1.6.1, and with it the numbers started being printed.
     *
     * <p>The {@code When in Main Hand:} header is later still and belongs to neither: {@code item.modifiers.*} is in
     * no language file of 1.6.4, 1.7.10 or 1.8.9, and first appears in 1.9 (as {@code When in main hand:}, lower
     * case). So for everything this project emulates, gating this one boundary removes the header as well.
     */
    private static final int ATTRIBUTE_TOOLTIPS_FROM =
        HistoricalVersions.chronology(HistoricalVersions.ERA_RELEASE, 1, 6, 1);

    private HistoricalTooltips() {
    }

    /** The era of the active version: NONE before tooltips existed, LEGACY, or MODERN. */
    public enum Era {
        /** Before Beta 1.0: no tooltips at all. */
        ABSENT,
        /** Beta 1.0 through Beta 1.9 Pre-release 1: the flat black box. */
        LEGACY,
        /** Beta 1.9 Pre-release 2 onwards, and every ordinary world: the game's own box. */
        MODERN
    }

    /** The era for a chronology key. */
    public static Era eraFor(int chronology) {
        if (chronology < TOOLTIPS_ADDED) {
            return Era.ABSENT;
        }
        return chronology < MODERN_TOOLTIPS_FROM ? Era.LEGACY : Era.MODERN;
    }

    /** The era for the client's active version; {@link Era#MODERN} (hands off) when nothing is active. */
    public static Era era() {
        HistoricalVersion version = HistoricalClientRules.activeVersion();
        return version == null ? Era.MODERN : eraFor(version.chronology());
    }

    /** Whether item tooltips must not be drawn at all. */
    public static boolean hidden() {
        return era() == Era.ABSENT;
    }

    /** Whether the flat legacy box must be drawn instead of the game's modern one. */
    public static boolean legacyBox() {
        return era() == Era.LEGACY;
    }

    /**
     * Whether an item tooltip may show its attribute block: Attack Damage, Attack Speed and the slot header.
     *
     * <p>Everything this project emulates is before Release 1.6.1 except nothing at all - the range ends at 1.7.10 -
     * so in practice this is false for every historical version and true only for an ordinary world. Swords therefore
     * show their name and their enchantments and no numbers, which is what they did.
     */
    public static boolean showsAttributeTooltip() {
        return showsAttributeTooltip(HistoricalClientRules.activeVersion());
    }

    /** Whether a version shows the attribute block. An ordinary world has no version and is left alone. */
    public static boolean showsAttributeTooltip(HistoricalVersion version) {
        return version == null || version.chronology() >= ATTRIBUTE_TOOLTIPS_FROM;
    }
}
