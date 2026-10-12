"""One-off: 1.21.1's Registry.get returns an Optional; 26.3's getValue returned the value.

Read from the jar rather than assumed: the compiler reported "cannot find symbol" on getValue at both call
sites, which is the signature that does not exist here.
"""
import pathlib

path = pathlib.Path('src/main/java/net/historicalexperience/integration/modernerbeta/'
                    'ModernerBetaAdapter.java')
text = path.read_text(encoding='utf-8')
before = text

text = text.replace(
    'stems.getValue(net.minecraft.world.level.dimension.LevelStem.OVERWORLD);',
    'stems.get(net.minecraft.world.level.dimension.LevelStem.OVERWORLD).orElse(null);')
text = text.replace(
    'net.minecraft.world.level.dimension.LevelStem stem = stems.getValue(key);',
    'net.minecraft.world.level.dimension.LevelStem stem = stems.get(key).orElse(null);')

if text != before:
    path.write_text(text, encoding='utf-8')
    print('patched ModernerBetaAdapter')
else:
    print('no change made - check the call sites by hand')
