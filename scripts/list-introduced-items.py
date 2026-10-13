"""Print the reviewed per-build 'introduced' entries that name a craftable item.

Read-only reconnaissance: research/version-transitions.json is a reviewed artifact, and this only lists what it
already states, so an item table built from its output can cite it rather than inventing a boundary.
"""
import json
import pathlib

ROOT = pathlib.Path(__file__).resolve().parents[1]
data = json.loads((ROOT / 'research/version-transitions.json').read_text(encoding='utf-8'))

versions = data['versions'] if isinstance(data, dict) and 'versions' in data else data
if isinstance(versions, dict):
    versions = list(versions.values())

WORDS = ('shears', 'map', 'cookie', 'dye', 'bowl', 'bucket', 'compass', 'clock', 'paper', 'book',
         'cake', 'painting', 'sign', 'ladder', 'boat', 'minecart', 'rail', 'piston', 'trapdoor',
         'bed', 'fishing', 'sugar', 'bread', 'apple', 'stew', 'armour', 'armor', 'sword', 'pickaxe',
         'hoe', 'axe', 'shovel', 'flint', 'bone meal', 'stick', 'torch', 'bottle', 'brewing',
         'bow', 'arrow', 'bucket', 'egg', 'sapling', 'seeds')

for version in versions:
    name = version.get('displayName') or version.get('id')
    for line in (version.get('introduced') or []):
        if any(word in line.lower() for word in WORDS):
            print('%-24s | %s' % (name, line[:160]))
