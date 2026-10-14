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
package net.historicalexperience.gameplay;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * The pre-1.9 attack damage values, applied to the tiered tools and weapons.
 *
 * <h2>Source of the mechanism</h2>
 *
 * <p>Ported from Nostalgic Tweaks {@code helper/gameplay/combat/DamageHelper.java} and its
 * {@code combat_item/DataComponentHolderMixin.java} (LGPL-3.0, see THIRD_PARTY_NOTICES.md), revision
 * {@code 010526deaf3aa5e562b45b60a286a667e8eeac12}: the item's {@code ATTACK_DAMAGE} attribute modifier is
 * replaced with an old value, the tier bonus being found by asking which material repairs the item, and the
 * result cached per item.
 *
 * <h2>Source of the values, and one place upstream disagrees with the wiki</h2>
 *
 * <p>Upstream computes {@code tierBonus + 4} for a sword, {@code +3} for an axe, {@code +2} for a pickaxe
 * and {@code +1} for a shovel. The values here are one lower in each case, because the modifier is added to
 * the player's base attack damage of 1: the classic pre-1.9 totals were sword 4, axe 3, pickaxe 2, shovel 1
 * (plus the tier bonus), which is {@code tierBonus + 3/2/1/0} as a modifier. Upstream's arithmetic would
 * produce a diamond sword hitting for 8 rather than 7. The wiki's own history is the authority used here:
 *
 * <ul>
 *   <li>Sword - "1.9 15w34c: All swords now deal 1 less damage" -
 *       https://minecraft.wiki/w/Sword?action=raw</li>
 *   <li>Axe - "1.9 15w34c: Axes do 4 more damage than previously", then "15w35a: The damage of axes has
 *       been reduced by 1" - https://minecraft.wiki/w/Axe?action=raw</li>
 * </ul>
 *
 * <p>The mechanism is upstream's; the numbers are the wiki's, and the discrepancy is recorded rather than
 * silently inherited.
 *
 * <h2>A known open refinement</h2>
 *
 * <p>Beta 1.9 Prerelease 3 reduced sword damage "to keep enchanting balanced", so swords were one point
 * higher again in Infdev, Alpha, Beta 1.7.3 and Beta 1.8.1. The exact per-tier pre-nerf numbers are not yet
 * established here, so this table is applied to every declared version - correct for the axe, pickaxe,
 * shovel and hoe in all of them, and correct for the sword from Beta 1.9 Prerelease 3 onward.
 */
public final class HistoricalDamageValues {
    /** The tiered item kinds, with the pre-1.9 damage each adds on top of the tier bonus. */
    public enum Kind {
        SWORD(3.0F),
        AXE(2.0F),
        PICKAXE(1.0F),
        SHOVEL(0.0F),
        HOE(0.0F);

        private final float preNineteeenBase;

        Kind(float preNineteeenBase) {
            this.preNineteeenBase = preNineteeenBase;
        }

        float base() {
            return this.preNineteeenBase;
        }
    }

    private static final Map<String, ItemAttributeModifiers> REWRITTEN = new ConcurrentHashMap<>();

    private HistoricalDamageValues() {
    }

    /**
     * Which kind of tiered item this is, by tag.
     *
     * <p>Upstream switches on {@code SwordItem}, {@code AxeItem} and friends. 26.3 removed those classes -
     * items are built from components - but kept the item tags, which say the same thing without depending
     * on a class that no longer exists.
     */
    public static Kind kindOf(ItemStack stack) {
        if (stack == null) {
            return null;
        }
        if (stack.is(ItemTags.SWORDS)) {
            return Kind.SWORD;
        }
        if (stack.is(ItemTags.AXES)) {
            return Kind.AXE;
        }
        if (stack.is(ItemTags.PICKAXES)) {
            return Kind.PICKAXE;
        }
        if (stack.is(ItemTags.SHOVELS)) {
            return Kind.SHOVEL;
        }
        if (stack.is(ItemTags.HOES)) {
            return Kind.HOE;
        }
        return null;
    }

    /** Whether this version's old damage values are in force and this item has one. */
    public static boolean isApplicable(ItemStack stack) {
        return HistoricalActiveSession.isHistorical() && kindOf(stack) != null;
    }

    /**
     * The pre-1.9 attack damage modifier for this item, which IS the era's total damage.
     *
     * <h2>Why this is a table and not the inherited formula, and what the owner's verification found</h2>
     *
     * <p>The owner asked for these values to be checked against the wiki per version. Doing so found two real
     * defects, both in the arithmetic rather than in the era constants:
     *
     * <ul>
     *   <li><b>Sword.</b> The inherited constant was {@code +6}, but 1.21.1's own raw modifier is {@code +3} - read
     *       out of the jar with a probe, not inferred. The old value happened to give the right total for exactly
     *       one item (the diamond sword, because {@code +6 + 3 = +9} matched the pre-Pre3 figure) while making
     *       <b>every other sword three points too strong</b> - a wooden sword dealt 7 where the wiki says 4, and a
     *       stone sword 8 where it says 5.</li>
     *   <li><b>Shovel.</b> The constant was {@code +0.5}; 1.21.1's raw modifier is {@code +1.5}.</li>
     * </ul>
     *
     * <p>Both were visible only by reading the RAW modifier. {@code ItemAttributeModifiers.compute()} also folds in
     * attack speed, so its output looks like a damage figure and is not one - a trap that caught this verification
     * before it caught the code.
     *
     * <h2>The era rule that makes these totals rather than bonuses</h2>
     *
     * <p>Before the 1.9 combat update an item's damage <b>replaced</b> the bare hand's; 13w21a changed the sword,
     * axe and pickaxe to <b>add</b> to it (wiki, per item: "Instead of replacing the barehanded damage (1),
     * <i>X</i> now add their damage onto the barehanded damage"). Every declared version is older than 1.9, so for
     * all of them this modifier is the figure a player used to see: sword 4, axe 3, pickaxe 2, shovel 1, each plus
     * the tier's bonus. Post-Pre3 the sword modifier coincides with 1.21.1's own, which is a useful cross-check
     * rather than a coincidence - the modern value descends from this one.
     *
     * <h2>Per-version detail, and the one approximation</h2>
     *
     * <p>Swords are the only kind that moved inside the declared range, and the wiki gives exact figures for two
     * tiers: the diamond sword went from 10 to 7 at Beta 1.9 Prerelease 3 and the iron sword from 8 to 6, so the
     * reduction was <b>not uniform</b> - 3 and 2 respectively, scaling with the tier. Wooden and golden swords are
     * <b>exempt</b>: "Reduced the damage of all swords except for wooden and golden ones to keep enchanting
     * balanced." The <b>stone sword</b> is the one inference here: it carries the same {@code bonus + 4} in both
     * eras, derived from the shape of the iron and diamond figures, because the wiki gives exact pre-Pre3 values
     * only for those two. It is recorded as an inference rather than presented as sourced.
     */
    private static float eraAttackDamage(TieredItem tieredItem) {
        float tierBonus = tieredItem.getTier().getAttackDamageBonus();
        boolean beforeSwordNerf = HistoricalGameplayTweaks.swordDamageBeforeReleaseOnePointZero(
            HistoricalActiveSession.current());
        LAST_TIER_BONUS = tierBonus;
        LAST_SWORD_PRE_NERF = beforeSwordNerf;
        LAST_TIER_NAME = String.valueOf(tieredItem.getTier());

        if (tieredItem instanceof SwordItem) {
            if (beforeSwordNerf) {
                LAST_BRANCH = "pre-nerf sword";
                // Pre-Pre3. The wiki gives exact figures for two tiers and states an exemption for two more:
                //   Diamond Sword: "Reduced the damage from 10 to 7"   -> total 10 before, so modifier 9
                //   Iron Sword:    "Reduced the iron sword's damage from 8 to 6" -> total 8 before, modifier 6
                //   Sword page:    "except for wooden and golden ones"   -> exempt, so unchanged at total 4
                //   Stone is INFERRED as total 5 from the shape of iron and diamond; the wiki gives no pre-Pre3
                //   figure for it, and it is recorded as an inference rather than presented as sourced.
                int tierOrdinal = Math.round(tierBonus);
                return switch (tierOrdinal) {
                    case 0 -> 3.0F;   // wood, gold - exempt
                    case 1 -> 4.0F;   // stone - inferred
                    case 2 -> 6.0F;   // iron - wiki
                    default -> 9.0F;  // diamond and above - wiki
                };
            }
            LAST_BRANCH = "post-nerf sword";
            // From Beta 1.9 Prerelease 3 onward, and the measured 1.21.1 modifier for every sword: total 4 + bonus.
            return tierBonus + 3.0F;
        }
        if (tieredItem instanceof AxeItem) {
            LAST_BRANCH = "axe";
            // Classic total 4 plus the tier bonus. 1.21.1's own axe modifier is one lower (b+2) because 1.9 moved
            // axes onto the cooldown system; the pre-1.9 axe was the stronger weapon, which is the whole reason the
            // 1.9 rebalance is famous.
            return tierBonus + 3.0F;
        }
        if (tieredItem instanceof PickaxeItem) {
            LAST_BRANCH = "pickaxe";
            // Classic total 3 plus the tier bonus; 1.21.1 gives b+1.
            return tierBonus + 2.0F;
        }
        if (tieredItem instanceof ShovelItem) {
            LAST_BRANCH = "shovel";
            // Classic total 1.5 plus the tier bonus - the one non-integer figure in the table. 1.21.1 gives b+1.5
            // as well, so a shovel's era and modern modifiers coincide; the earlier constant of b+0.5 was a point
            // and a half low and was the defect the owner's verification caught here.
            return tierBonus + 1.5F;
        }
        LAST_BRANCH = "other";
        // A hoe was not a weapon before 15w34c ("Stone, iron and diamond hoes now do slightly more damage than an
        // unarmed attack"), so every declared version had all hoes at 1 damage - which is also what 1.21.1 gives
        // them. The caller leaves a hoe alone rather than routing it here.
        return tierBonus;
    }

    // Diagnostics only: what the last computation saw. Written because a failing expectation and a failing
    // computation look identical from outside, and this project has now confused the two more than once.
    public static volatile float LAST_TIER_BONUS;
    public static volatile boolean LAST_SWORD_PRE_NERF;
    public static volatile String LAST_BRANCH = "<never asked>";
    public static volatile String LAST_TIER_NAME = "<none>";

    /** Upstream rewrites the attack damage entry only for these; a hoe is left alone. */
    private static boolean rewritesAttackDamage(TieredItem tieredItem) {
        return tieredItem instanceof DiggerItem || tieredItem instanceof SwordItem;
    }

    /**
     * The item's attribute modifiers with the old attack damage in place of the modern one.
     *
     * <p>Every other modifier is passed through untouched, exactly as upstream does, so attack speed,
     * knockback and anything added by another mod survive.
     */
    public static ItemAttributeModifiers rewrite(ItemStack stack, ItemAttributeModifiers modern) {
        if (!(stack.getItem() instanceof TieredItem tieredItem) || !rewritesAttackDamage(tieredItem)) {
            // Upstream leaves a hoe, and anything untiered, on its vanilla modifier.
            return modern;
        }
        return REWRITTEN.computeIfAbsent(stack.getItem().getDescriptionId(), key -> {
            ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
            float damage = eraAttackDamage(tieredItem);
            modern.modifiers().forEach(entry -> {
                if (entry.attribute() == Attributes.ATTACK_DAMAGE) {
                    builder.add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, damage,
                            AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND);
                } else {
                    builder.add(entry.attribute(), entry.modifier(), entry.slot());
                }
            });
            return builder.build();
        });
    }

    /** The component this rewrite applies to, for callers that need to check before asking. */
    public static boolean isAttributeComponent(Object component) {
        return component == DataComponents.ATTRIBUTE_MODIFIERS;
    }
}
