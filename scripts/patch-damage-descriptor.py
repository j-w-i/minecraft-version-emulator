"""One-off: give the damage mixin an explicit descriptor (1.21.1 declares forEachModifier twice)."""
import pathlib

path = (pathlib.Path(__file__).resolve().parents[1]
        / 'src/main/java/net/historicalexperience/mixin/ItemStackHistoricalDamageMixin.java')
text = path.read_text(encoding='utf-8')
old = '    @Inject(method = "forEachModifier", at = @At("HEAD"), cancellable = true)'
new = ('    // The FULL descriptor, not just the name: 1.21.1 declares forEachModifier twice (per equipment\n'
       '    // slot and per slot group), and a bare name is ambiguous - a FATAL mixin error at startup, not a\n'
       '    // warning. Found by booting the game, not by compiling it.\n'
       '    @Inject(method = "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlot;'
       'Ljava/util/function/BiConsumer;)V",\n'
       '        at = @At("HEAD"), cancellable = true)')
assert old in text, 'anchor not found'
path.write_text(text.replace(old, new, 1), encoding='utf-8')
print('descriptor added')
