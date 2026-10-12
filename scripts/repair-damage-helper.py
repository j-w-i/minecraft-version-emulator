"""Repair the half-applied OLD_DAMAGE_VALUES port.

The broken state: eraAttackDamage() was inserted using SwordItem/AxeItem/PickaxeItem/ShovelItem/DiggerItem and
TieredItem without importing them, the orphaned javadoc for the removed tierBonus() was left behind, and
rewrite() still called kind.base() + tierBonus(stack) - a method that no longer exists.
"""
import pathlib

path = (pathlib.Path(__file__).resolve().parents[1]
        / 'src/main/java/net/historicalexperience/gameplay/HistoricalDamageValues.java')
text = path.read_text(encoding='utf-8')

# 1. Imports.
anchor = 'import net.minecraft.world.item.Item;'
assert anchor in text, 'import anchor not found'
imports = '''import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;'''
text = text.replace('import net.minecraft.world.item.Item;', imports, 1)

# 2. Drop the orphaned javadoc left by the earlier edit.
orphan = '''    /**
     * The tier bonus, found the way upstream finds it: by asking what repairs the item.
     *
     * <p>26.3 also has a copper tier that upstream predates. It is deliberately not listed: copper tools are
     * modern content that a historical world does not contain, so treating one as an unlisted tier is the
     * safe answer rather than inventing a value for it.
     */
'''
assert orphan in text, 'orphaned javadoc not found'
text = text.replace(orphan, '', 1)

# 3. rewrite() takes upstream's shape: guard on the tiered item, and use the era value directly.
old = '''    public static ItemAttributeModifiers rewrite(ItemStack stack, ItemAttributeModifiers modern) {
        Kind kind = kindOf(stack);
        if (kind == null) {
            return modern;
        }
        return REWRITTEN.computeIfAbsent(stack.getItem().getDescriptionId(), key -> {
            ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
            float damage = kind.base() + tierBonus(stack);'''
new = '''    public static ItemAttributeModifiers rewrite(ItemStack stack, ItemAttributeModifiers modern) {
        if (!(stack.getItem() instanceof TieredItem tieredItem) || !rewritesAttackDamage(tieredItem)) {
            // Upstream leaves a hoe, and anything untiered, on its vanilla modifier.
            return modern;
        }
        return REWRITTEN.computeIfAbsent(stack.getItem().getDescriptionId(), key -> {
            ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
            float damage = eraAttackDamage(tieredItem);'''
assert old in text, 'rewrite anchor not found'
text = text.replace(old, new, 1)

path.write_text(text, encoding='utf-8')
print('repaired the damage helper')
