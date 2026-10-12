"""Final two errors in the damage port: the interface-mixin cast, and a stale three-argument call."""
import pathlib

path = (pathlib.Path(__file__).resolve().parents[1]
        / 'src/main/java/net/historicalexperience/mixin/DataComponentHolderHistoricalDamageMixin.java')
text = path.read_text(encoding='utf-8')

old = '        if (!(this instanceof ItemStack itemStack)'
new = '        if (!((Object) this instanceof ItemStack itemStack)'
assert old in text, 'cast anchor not found'
text = text.replace(old, new, 1)

old = '            return (T) HistoricalDamageValues.rewrite(tieredItem, itemStack, modifiers);'
new = '            return (T) HistoricalDamageValues.rewrite(itemStack, modifiers);'
assert old in text, 'rewrite call anchor not found'
text = text.replace(old, new, 1)

path.write_text(text, encoding='utf-8')
print('fixed the cast and the call')
