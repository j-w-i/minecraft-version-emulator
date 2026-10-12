"""Read-only: what did the generated availability table actually record for a few blocks?"""
import json
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[1]
text = (ROOT / 'src/main/java/net/historicalexperience/generation/HistoricalBlockAvailability.java'
        ).read_text(encoding='utf-8')
call = re.compile(r'introduce\("([^"]+)",\s*(true|false),\s*((?:"[^"]+"\s*,?\s*)+)\);')
recorded = {}
order = re.search(r'CHRONOLOGY = List\.of\((.*?)\);', text, re.S).group(1)
chronology = re.findall(r'"([^"]+)"', order)
for match in call.finditer(text):
    version, timeline = match.group(1), match.group(2) == 'true'
    for block in re.findall(r'"([^"]+)"', match.group(3)):
        recorded[block] = (version, timeline,
                           'as "%s"' % re.search(r'as "([^"]+)"', match.group(0)).group(1)
                           if 'as "' in match.group(0) else None)
print('chronology:', chronology)
print('dated blocks:', len(recorded))
for probe in ['minecraft:short_grass', 'minecraft:terracotta', 'minecraft:vine', 'minecraft:sandstone',
              'minecraft:spawner', 'minecraft:stone_bricks', 'minecraft:fern', 'minecraft:clay_hardened']:
    print('%-32s %s' % (probe, recorded.get(probe, '-- NOT DATED')))
# which mapping names have no palette anywhere
palettes = {}
for name in ['b1_7_3', 'b1_8_1', 'b1_9_pre3', '1_0_0', '1_1', '1_2_5', '1_6_4']:
    document = json.loads((ROOT / ('research/block-palettes/%s.json' % name)).read_text(encoding='utf-8'))
    palettes[name] = set(document['blockNames']) | set(document.get('aliasNames') or [])
print()
print('clayHardened in 1_6_4 palette:', 'clayHardened' in palettes['1_6_4'])
print('tallgrass in b1_7_3 palette:', 'tallgrass' in palettes['b1_7_3'])
