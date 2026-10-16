"""SPDX-License-Identifier: LGPL-3.0-or-later
Extract factual recipe tables, not source code, from independently hash-verified archive jars.
Needs a JDK (JAVA_HOME). Jars stay in ignored .upstream/legacy-jars; no downloads here.
"""
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
BUILDS = {'indev_latest': 'in-20100223', 'infdev_420': 'inf-20100420', 'infdev_611': 'inf-20100611'}
# Historical numeric IDs -> modern storage equivalents. Cloth id 35 is light gray, NOT modern white wool.
# The historical smooth stone step used cobblestone as its ingredient. Leather armour represents cloth armour.
IDS = {-1: '', 1: 'stone', 4: 'cobblestone', 5: 'oak_planks', 12: 'sand', 14: 'gold_ore',
       15: 'iron_ore', 17: 'oak_log', 20: 'glass', 35: 'light_gray_wool', 39: 'brown_mushroom',
       40: 'red_mushroom', 41: 'gold_block', 42: 'iron_block', 44: 'smooth_stone_slab', 46: 'tnt',
       50: 'torch', 51: 'fire', 54: 'chest', 56: 'diamond_ore', 57: 'diamond_block',
       58: 'crafting_table', 61: 'furnace', 65: 'ladder', 256: 'iron_shovel', 257: 'iron_pickaxe',
       258: 'iron_axe', 259: 'flint_and_steel', 260: 'apple', 261: 'bow', 262: 'arrow', 263: 'coal',
       264: 'diamond', 265: 'iron_ingot', 266: 'gold_ingot', 267: 'iron_sword', 268: 'wooden_sword',
       269: 'wooden_shovel', 270: 'wooden_pickaxe', 271: 'wooden_axe', 272: 'stone_sword',
       273: 'stone_shovel', 274: 'stone_pickaxe', 275: 'stone_axe', 276: 'diamond_sword',
       277: 'diamond_shovel', 278: 'diamond_pickaxe', 279: 'diamond_axe', 280: 'stick', 281: 'bowl',
       282: 'mushroom_stew', 283: 'golden_sword', 284: 'golden_shovel', 285: 'golden_pickaxe',
       286: 'golden_axe', 287: 'string', 288: 'feather', 289: 'gunpowder', 290: 'wooden_hoe',
       291: 'stone_hoe', 292: 'iron_hoe', 293: 'diamond_hoe', 294: 'golden_hoe', 295: 'wheat_seeds',
       296: 'wheat', 297: 'bread', 318: 'flint', 319: 'porkchop', 320: 'cooked_porkchop',
       321: 'painting', 322: 'golden_apple', 323: 'oak_sign', 324: 'oak_door'}
for offset, tier in enumerate(('leather', 'chainmail', 'iron', 'diamond', 'golden')):
    for slot, name in enumerate(('helmet', 'chestplate', 'leggings', 'boots')):
        IDS[298 + 4 * offset + slot] = f'{tier}_{name}'
IDS = {key: 'minecraft:' + value if value else '' for key, value in IDS.items()}


def extract_leaf(java, jar):
    """Exhaust every quantity bucket and stored four-bit metadata value, not Monte Carlo sampling."""
    result = subprocess.run([str(java), str(ROOT / 'scripts/EarlyLeafExtractor.java'), str(jar)],
                            check=True, text=True, capture_output=True)
    import uuid
    log = ROOT / 'logs' / f'early-leaf-{jar.stem}-{uuid.uuid4().hex}.txt'
    log.parent.mkdir(parents=True, exist_ok=True)
    with log.open('x', encoding='utf8') as output:
        output.write(result.stdout)
    rows = {'quantity': [], 'item': []}
    methods = {}
    kind = None
    registry = leaf_class = None
    for line in result.stdout.splitlines():
        if line.startswith('REGISTRY '):
            assert registry is None, 'Ambiguous block registry'
            registry = line.split()[1]
        elif line.startswith('LEAF_CLASS '):
            leaf_class = line.split()[1]
        elif line.startswith('PROBE '):
            kind = 'item' if '(int,java.util.Random)' in line else 'quantity'
            assert kind not in methods, 'Ambiguous leaf drop method'
            methods[kind] = line.removeprefix('PROBE ')
        elif line.startswith('RESULT '):
            match = re.fullmatch(r'RESULT meta=(\d+) roll=(\d+) result=(-?\d+) bounds=(\[.*\])', line)
            assert match and kind is not None, line
            meta, roll, value, bounds = match.groups()
            rows[kind].append((int(meta), int(roll), int(value), json.loads(bounds)))
    expected = {(meta, roll) for meta in range(16) for roll in range(10)}
    for kind in ('quantity', 'item'):
        assert len(rows[kind]) == 160 and {(m, r) for m, r, _, _ in rows[kind]} == expected
        for meta, roll, value, bounds in rows[kind]:
            if kind == 'quantity':
                assert bounds == [10] and value == (1 if roll == 0 else 0), (meta, roll, value, bounds)
            else:
                assert bounds == [] and value == 6, (meta, roll, value, bounds)
    assert registry and leaf_class
    facts = {'block': 'minecraft:oak_leaves', 'result': 'minecraft:oak_sapling', 'count': 1,
             'chanceDenominator': 10, 'successRoll': 0}
    evidence = {'registryField': registry, 'leafClass': leaf_class, 'quantityMethod': methods['quantity'],
                'itemMethod': methods['item'], 'numericLeafId': 18, 'numericDropId': 6,
                'metadataValuesChecked': 16, 'quantityBucketsCheckedPerMetadata': 10,
                'quantityRandomBounds': [10], 'quantityOutputs': [1] + [0] * 9, 'itemRandomBounds': [],
                'toolParameters': False, 'confidence': 'high',
                'scope': 'Quantity/item callbacks only; explosion survival, decay timing and pickup physics not established.'}
    print('Leaf facts verified:', jar.name, '320 calls;', log.relative_to(ROOT))
    return facts, evidence


def extract_leaf_only():
    java = Path(os.environ['JAVA_HOME']) / 'bin/java.exe'
    for profile, build in BUILDS.items():
        path = ROOT / 'src/main/resources/data/historical_experience/early_gameplay' / (profile + '.json')
        document = json.loads(path.read_text(encoding='utf8'))
        jar = ROOT / '.upstream/legacy-jars' / (build + '.jar')
        assert document['profile'] == 'historical_experience:' + profile and document['artifact']['id'] == build
        assert hashlib.sha1(jar.read_bytes()).hexdigest() == document['artifact']['sha1']
        assert jar.stat().st_size == document['artifact']['bytes']
        document['leafDrop'], document['leafDropEvidence'] = extract_leaf(java, jar)
        path.write_text(json.dumps(document, indent=2) + '\n', encoding='utf8')


def extract():
    for profile, build in BUILDS.items():
        jar = ROOT / '.upstream/legacy-jars' / (build + '.jar')
        url = f'https://meta.celestia.sh/versions/v1/{build}.json'
        metadata = json.load(urllib.request.urlopen(url))['downloads']['client']
        digest = hashlib.sha1(jar.read_bytes()).hexdigest()
        assert digest == metadata['sha1'] and jar.stat().st_size == metadata['size'], build
        java = Path(os.environ['JAVA_HOME']) / 'bin/java.exe'
        result = subprocess.run([str(java), str(ROOT / 'scripts/EarlyGameplayExtractor.java'), str(jar)],
                                check=True, text=True, capture_output=True)
        recipes = []
        smelts = []
        food = {}
        food_evidence = []
        food_type, heal_field, id_field = {
            'indev_latest': ('p', 'as', 'ap'), 'infdev_420': ('p', 'au', 'ar'),
            'infdev_611': ('r', 'aw', 'at')}[profile]
        for line in result.stdout.splitlines():
            if line.startswith('RECIPE '):
                match = re.fullmatch(r'RECIPE (\d+) (\d+) (\[.*\]) -> (\d+) x(\d+)', line)
                width, height, inputs, output, count = match.groups()
                inputs = json.loads(inputs)
                recipes.append({'width': int(width), 'height': int(height),
                                'ingredients': [IDS[i] for i in inputs], 'result': IDS[int(output)],
                                'count': int(count), 'dormant': 51 in inputs})
            elif line.startswith('SMELT '):
                match = re.fullmatch(r'SMELT (\d+) -> (\d+)', line)
                smelts.append({'input': IDS[int(match[1])], 'result': IDS[int(match[2])]})
            elif line.startswith('ITEM ') and (f' {food_type}.{heal_field}=' in line):
                field, instance_type = line.split()[1:3]
                heal = int(re.search(rf' {food_type}\.{heal_field}=(\d+)', line)[1])
                item_id = int(re.search(rf' i\.{id_field}=(\d+)', line)[1])
                food[IDS[item_id]] = heal
                food_evidence.append({'item': IDS[item_id], 'healPoints': heal,
                                      'registryField': 'net.minecraft.a.b.i.' + field,
                                      'instanceClass': instance_type, 'healField': food_type + '.' + heal_field})
        assert len(food) == (4 if profile == 'indev_latest' else 6)
        assert len(recipes) == {'indev_latest': 68, 'infdev_420': 69, 'infdev_611': 72}[profile]
        assert len(smelts) == 6
        document = {'schema': 1, 'profile': 'historical_experience:' + profile,
                    'artifact': {'id': build, 'sha1': digest, 'bytes': jar.stat().st_size,
                                 'hashMetadata': url, 'download': metadata['url']},
                    'mappingNotes': ['Numeric cloth 35 maps to light gray wool (texture 78), not white.',
                                     'Numeric step 44 maps to smooth stone slab, made from cobblestone.',
                                     'Leather equipment represents the original cloth armour.',
                                     'Four chainmail recipes require unobtainable fire; retained as dormant, never offered.'],
                    'recipes': recipes, 'smelting': smelts, 'food': food, 'foodEvidence': food_evidence}
        document['leafDrop'], document['leafDropEvidence'] = extract_leaf(java, jar)
        path = ROOT / 'src/main/resources/data/historical_experience/early_gameplay' / (profile + '.json')
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(document, indent=2) + '\n', encoding='utf8')
        print(profile, len(recipes), 'recipes,', len(smelts), 'smelts, verified', digest)


if __name__ == '__main__':
    import argparse
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--leaf-only', action='store_true', help='Offline: preserve existing artifact/recipe/food facts and refresh leaf facts only')
    options = parser.parse_args()
    extract_leaf_only() if options.leaf_only else extract()
