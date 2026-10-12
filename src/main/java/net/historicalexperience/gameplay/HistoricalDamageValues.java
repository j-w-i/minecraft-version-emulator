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
     * Upstream's formula, from {@code helper/gameplay/combat/DamageHelper.getAttackDamage}.
     *
     * <p>Correction: an earlier version of this file documented upstream as {@code +4} for a sword, {@code +3}
     * for an axe, {@code +2} for a pickaxe and {@code +1} for a shovel, and then used values one lower again.
     * Those numbers did not come from upstream. Upstream's are below, and only weapons and diggers are rewritten:
     * <b>hoes keep their vanilla modifier</b>, which the earlier version changed.
     */
    private static float eraAttackDamage(TieredItem tieredItem) {
        float damage = switch (tieredItem) {
            case SwordItem sword -> sword.getTier().getAttackDamageBonus() + 6.0F;
            case AxeItem axe -> axe.getTier().getAttackDamageBonus() + 2.0F;
            case PickaxeItem pickaxe -> pickaxe.getTier().getAttackDamageBonus() + 1.0F;
            case ShovelItem shovel -> shovel.getTier().getAttackDamageBonus() + 0.5F;
            default -> tieredItem.getTier().getAttackDamageBonus();
        };

        // Beta 1.9 Prerelease 3 reduced sword damage "to keep enchanting balanced", so every sword is one point
        // higher before it. This file said so for twenty rounds while applying the later value to all ten profiles.
        if (tieredItem instanceof SwordItem
            && HistoricalGameplayTweaks.swordDamageBeforeReleaseOnePointZero(
                HistoricalActiveSession.current())) {
            // Three points, from ten damage to seven: "diamond sword from 5 hearts to 3.5 hearts" at Release 1.0.0.
            damage += 3.0F;
        }

        return damage;
    }

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
