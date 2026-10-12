"""Fix the two compile errors from the OLD_DAMAGE_VALUES port.

1. Replacing tierBonus() left rewrite()'s body calling a method that no longer exists. tierBonus is restored as
   a thin adapter over upstream's formula, so rewrite() keeps working while the values come from upstream.
2. An interface mixin cannot say `this instanceof ItemStack` - the compiler sees the mixin interface type. The
   `(Object) this` cast is the mixin idiom, and it is why upstream uses ClassUtil.cast instead.
"""
import pathlib

root = pathlib.Path(__file__).resolve().parents[1]

helper = root / 'src/main/java/net/historicalexperience/gameplay/HistoricalDamageValues.java'
text = helper.read_text(encoding='utf-8')
anchor = '    /**\n     * Upstream rewrites the attack damage entry only for these; a hoe is left alone.\n     */'
assert anchor in text, 'helper anchor not found'
adapter = '''    /**
     * Adapter kept so {@code rewrite} reads through one path: the era value for a stack's tiered item, or zero
     * when the item is not tiered.
     */
    private static float tierBonus(ItemStack stack) {
        return stack.getItem() instanceof TieredItem tieredItem ? eraAttackDamage(tieredItem) : 0.0F;
    }

'''
text = text.replace(anchor, adapter + anchor, 1)
helper.write_text(text, encoding='utf-8')
print('restored tierBonus as an adapter over the upstream formula')

mixin = root / 'src/main/java/net/historicalexperience/mixin/DataComponentHolderHistoricalDamageMixin.java'
text = mixin.read_text(encoding='utf-8')
old = '        if (!(this instanceof ItemStack itemStack) || !(value instanceof ItemAttributeModifiers modifiers)) {'
new = '        if (!((Object) this instanceof ItemStack itemStack)\n            || !(value instanceof ItemAttributeModifiers modifiers)) {'
assert old in text, 'mixin anchor not found'
mixin.write_text(text.replace(old, new, 1), encoding='utf-8')
print('interface mixin now uses the (Object) cast idiom')
