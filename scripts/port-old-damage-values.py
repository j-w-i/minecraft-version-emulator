"""Port OLD_DAMAGE_VALUES from its real carrier, and correct the formula my version invented.

Upstream hook: @Mixin(DataComponentHolder.class), @ModifyReturnValue on getOrDefault. Every reader of the
attribute component - the attribute map, the tooltip, forEachModifier itself - gets the era's values, instead
of only the one path my forEachModifier hooks covered.

Upstream formula, from helper/gameplay/combat/DamageHelper:
    sword   tier.getAttackDamageBonus() + 6.0F
    axe     + 2.0F
    pickaxe + 1.0F
    shovel  + 0.5F
    default tier.getAttackDamageBonus()
and it only rewrites the attack-damage entry for DiggerItem or SwordItem, so HOES KEEP THEIR VANILLA MODIFIER.

My file documented upstream as "+4 for a sword, +3 for an axe, +2 for a pickaxe, +1 for a shovel" - numbers I
did not get from upstream and attributed to it. It then used values one lower again, and rewrote hoes, which
upstream leaves alone.
"""
import json
import pathlib
import re

root = pathlib.Path(__file__).resolve().parents[1]
mixin = root / 'src/main/java/net/historicalexperience/mixin/DataComponentHolderHistoricalDamageMixin.java'

mixin.write_text('''package net.historicalexperience.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.historicalexperience.gameplay.HistoricalDamageValues;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Gives old weapons and tools the damage numbers their version used.
 *
 * <p>DIRECT PORT of Nostalgic Tweaks
 * {@code common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/gameplay/combat_item/DataComponentHolderMixin.java}
 * (LGPL-3.0; see THIRD_PARTY_NOTICES.md), which answers {@code GameplayTweak.OLD_DAMAGE_VALUES}.
 *
 * <h2>Why this replaced two of my own hooks</h2>
 *
 * <p>I had hooked {@code ItemStack.forEachModifier} - twice, once per overload - and then added an equipment
 * refresh because the attribute map is built before a player joins and therefore before the version is known.
 * Upstream needs neither. It modifies {@code getOrDefault} on {@code DataComponentHolder}, which is where every
 * reader of the attribute component gets it from: the attribute map, the tooltip, and
 * {@code forEachModifier} itself, which calls through to it. One hook covers all of them, and the two
 * overloads stop being a question at all.
 *
 * <p>The refresh in {@code HistoricalEquipmentRefresh} is still needed, for a reason upstream does not have:
 * upstream's flag is a global config known from the first tick, while this project's answer depends on a
 * session that is only established when a player joins. The component now returns the right value whenever it
 * is asked; the map that was built before the answer existed still has to be rebuilt.
 */
@Mixin(DataComponentHolder.class)
public interface DataComponentHolderHistoricalDamageMixin {
    /** Upstream: {@code nt_combat_item$modifyGetOrDefaultAttributes}. */
    @SuppressWarnings("unchecked")
    @ModifyReturnValue(method = "getOrDefault", at = @At("RETURN"))
    default <T> T historicalExperience$modifyGetOrDefaultAttributes(T value,
            DataComponentType<? extends T> component) {
        if (DataComponents.ATTRIBUTE_MODIFIERS != component) {
            return value;
        }

        if (!(this instanceof ItemStack itemStack) || !(value instanceof ItemAttributeModifiers modifiers)) {
            return value;
        }

        if (itemStack.getItem() instanceof TieredItem tieredItem
            && HistoricalDamageValues.isApplicable(itemStack)) {
            return (T) HistoricalDamageValues.rewrite(tieredItem, itemStack, modifiers);
        }

        return value;
    }
}
''', encoding='utf-8')
print('wrote DataComponentHolderHistoricalDamageMixin')

config = root / 'src/main/resources/historical_experience.mixins.json'
document = json.loads(config.read_text(encoding='utf-8'))
document['mixins'] = [x for x in document['mixins'] if x != 'ItemStackHistoricalDamageMixin']
if 'DataComponentHolderHistoricalDamageMixin' not in document['mixins']:
    document['mixins'].append('DataComponentHolderHistoricalDamageMixin')
config.write_text(json.dumps(document, indent=2) + '\n', encoding='utf-8')
print('registered; shared mixins =', len(document['mixins']))

(root / 'src/main/java/net/historicalexperience/mixin/ItemStackHistoricalDamageMixin.java').unlink(missing_ok=True)

# Port upstream's formula and its DiggerItem/SwordItem restriction.
helper = root / 'src/main/java/net/historicalexperience/gameplay/HistoricalDamageValues.java'
text = helper.read_text(encoding='utf-8')
start = text.index('    private static float tierBonus(ItemStack stack) {')
end = text.index('\n    }', start) + len('\n    }')
replacement = '''    /**
     * Upstream's formula, from {@code helper/gameplay/combat/DamageHelper.getAttackDamage}.
     *
     * <p>Correction: an earlier version of this file documented upstream as {@code +4} for a sword, {@code +3}
     * for an axe, {@code +2} for a pickaxe and {@code +1} for a shovel, and then used values one lower again.
     * Those numbers did not come from upstream. Upstream's are below, and only weapons and diggers are rewritten:
     * <b>hoes keep their vanilla modifier</b>, which the earlier version changed.
     */
    private static float eraAttackDamage(TieredItem tieredItem) {
        return switch (tieredItem) {
            case SwordItem sword -> sword.getTier().getAttackDamageBonus() + 6.0F;
            case AxeItem axe -> axe.getTier().getAttackDamageBonus() + 2.0F;
            case PickaxeItem pickaxe -> pickaxe.getTier().getAttackDamageBonus() + 1.0F;
            case ShovelItem shovel -> shovel.getTier().getAttackDamageBonus() + 0.5F;
            default -> tieredItem.getTier().getAttackDamageBonus();
        };
    }

    /** Upstream rewrites the attack damage entry only for these; a hoe is left alone. */
    private static boolean rewritesAttackDamage(TieredItem tieredItem) {
        return tieredItem instanceof DiggerItem || tieredItem instanceof SwordItem;
    }'''
text = text[:start] + replacement + text[end:]
helper.write_text(text, encoding='utf-8')
print('ported the formula (needs the rewrite() body updated to use eraAttackDamage)')
