"""Read-only: how the six newly available palettes line up with the modern->legacy mapping table."""
import json
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[1]
src = (ROOT / 'src/main/java/net/historicalexperience/generation/HistoricalBlockPalette.java'
       ).read_text(encoding='utf-8')
pairs = []
for match in re.finditer(r'^\s*map\("([^"]+)",\s*(.*?)\);\s*$', src, re.M | re.S):
    legacy = match.group(1)
    for modern in re.findall(r'"([^"]+)"', match.group(2)):
        pairs.append((modern if modern.startswith('minecraft:') else 'minecraft:' + modern, legacy))
print('modern->legacy pairs: %d, distinct modern ids: %d' % (len(pairs), len(set(p[0] for p in pairs))))

FILES = {'beta_1_7_3': 'b1_7_3', 'beta_1_8_1': 'b1_8_1', 'beta_1_9_pre_3': 'b1_9_pre3',
         'release_1_0_0': '1_0_0', 'release_1_1': '1_1', 'release_1_2_5': '1_2_5',
         'release_1_6_4': '1_6_4'}
palettes = {}
for version, name in FILES.items():
    document = json.loads((ROOT / ('research/block-palettes/%s.json' % name)).read_text(encoding='utf-8'))
    palettes[version] = set(document['blockNames'])

legacy_names = sorted({legacy for _, legacy in pairs})
print()
for version in FILES:
    palette = palettes[version]
    missing = [name for name in legacy_names if name not in palette]
    print('%-22s palette=%-4d mapped-names-absent=%-3d' % (version, len(palette), len(missing)))

never = [name for name in legacy_names if not any(name in palette for palette in palettes.values())]
print()
print('legacy names in the mapping that appear in NO extracted palette (typos or later-only):')
print('  %d: %s' % (len(never), never))

base = palettes['beta_1_7_3']
newer = sorted(name for name in legacy_names
               if name not in base and any(name in palette for palette in palettes.values()))
print()
print('legacy names absent from Beta 1.7.3 but present later (post-1.7.3 block names in the mapping):')
print('  %d: %s' % (len(newer), newer))

print()
print('per-version additions relative to the previous palette:')
order = list(FILES)
for index, version in enumerate(order[1:], start=1):
    previous = palettes[order[index - 1]]
    added = sorted(name for name in palettes[version] if name not in previous)
    removed = sorted(name for name in previous if name not in palettes[version])
    print('%-22s +%d %s' % (version, len(added), added))
    if removed:
        print('%-22s -%d %s' % ('', len(removed), removed))
