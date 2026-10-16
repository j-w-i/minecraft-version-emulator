"""SPDX-License-Identifier: LGPL-3.0-or-later
Offline snapshot/check of explicitly named PROJECT FIXTURES. Never writes world data.
Run snapshot after the isolated Indev server stops, check after its Infdev reload stops.
"""
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('census', ROOT / 'scripts/region-block-census.py')
census = importlib.util.module_from_spec(spec)
spec.loader.exec_module(census)
TERRAIN = {'minecraft:' + s for s in ('noise', 'surface', 'carvers', 'features', 'initialize_light', 'light', 'spawn', 'full')}
NATURAL = {'minecraft:' + s for s in ('stone', 'bedrock', 'dirt', 'grass_block', 'sand', 'gravel', 'coal_ore', 'iron_ore', 'gold_ore', 'diamond_ore')}


def chunks(world):
    result = {}
    for region in (world / 'region').glob('*.mca'):
        for raw in census.iter_chunks(region):
            tag = census.read_nbt(raw)
            if tag.get('Status') in TERRAIN:
                result[tag['xPos'], tag['zPos']] = tag
    return result


def states(chunk):
    result = ['minecraft:air'] * 32768
    for section in chunk.get('sections', []):
        y = section['Y']
        if not 0 <= y < 8:
            continue
        container = section.get('block_states', {})
        palette = container.get('palette', [])
        if not palette:
            continue
        names = [json.dumps(p, sort_keys=True, separators=(',', ':')) for p in palette]
        indices = [0] * 4096 if len(palette) == 1 else census.unpack_indices(
            container['data'], max(4, (len(palette)-1).bit_length()), 4096)
        result[y * 4096:(y + 1) * 4096] = [names[i] for i in indices]
    return result


def digest(chunk, version=1):
    values = states(chunk)
    if version >= 2:
        values = ['{"Name":"minecraft:air"}' if value == 'minecraft:air' else value for value in values]
    return hashlib.sha256(('\n'.join(values) + json.dumps(chunk.get('block_entities', []), sort_keys=True)).encode()).hexdigest()


def protected_digest(chunk):
    # Canonical USED biome cells, not palette order/unused entries; normal light/time counters may change on save.
    biomes = {}
    for section in chunk.get('sections', []):
        b = section.get('biomes', {})
        palette = b.get('palette', [])
        if palette:
            bits = max(1, (len(palette)-1).bit_length())
            indices = [0]*64 if len(palette) == 1 else census.unpack_indices(b['data'], bits, 64)
            biomes[section['Y']] = [palette[i] for i in indices]
    return hashlib.sha256((digest(chunk, 2) + json.dumps(biomes, sort_keys=True)).encode()).hexdigest()


def height_and_high_bedrock(chunk, x, z):
    h = -1
    high_bedrock = 0
    for section in chunk.get('sections', []):
        sy = section['Y']
        bs = section.get('block_states', {})
        pp = bs.get('palette', [])
        if not pp:
            continue
        ix = [0] * 4096 if len(pp) == 1 else census.unpack_indices(bs['data'], max(4, (len(pp)-1).bit_length()), 4096)
        for y in range(16):
            name = pp[ix[y * 256 + (z & 15) * 16 + (x & 15)]]['Name']
            wy = sy * 16 + y
            if name in NATURAL:
                h = max(h, wy)
            if wy >= 8 and name == 'minecraft:bedrock':
                high_bedrock += 1
    return h, high_bedrock


def proven_finite_backup(world, current_level):
    """Independent snapshot provenance for both newly migrated and OLD already-upgraded fixtures."""
    current = json.loads((world / 'historical_experience_metadata.json').read_text(encoding='utf-8'))
    current_gen = current_level['Data']['WorldGenSettings']['dimensions']['minecraft:overworld']['generator']
    current_refs = (current_gen['provider_settings'], current_gen['biome_source']['provider_settings'],
                    current_gen['biome_source']['cave_provider_settings'])
    assert all(s.get('moderner_beta:preset') == current['generationPreset'] for s in current_refs), 'metadata/generator mismatch'
    for directory in sorted((world / 'historical_experience_backups').glob('*')):
        try:
            manifest = json.loads((directory / 'backup-manifest.json').read_text(encoding='utf-8'))
            entries = {e['relativePath']: e for e in manifest['entries']}
            assert manifest['schema'] == 1 and {'level.dat', 'historical_experience_metadata.json'} <= entries.keys()
            for entry in entries.values():
                name = entry['relativePath'].replace('/', '_')
                assert '\\' not in name and Path(name).name == name
                data = (directory / name).read_bytes()
                assert len(data) == entry['size'] and hashlib.sha256(data).hexdigest() == entry['sha256']
            source = json.loads((directory / 'historical_experience_metadata.json').read_text(encoding='utf-8'))
            prior = census.read_nbt(__import__('gzip').decompress((directory / 'level.dat').read_bytes()))
            gen = prior['Data']['WorldGenSettings']['dimensions']['minecraft:overworld']['generator']
            preset = gen['provider_settings']['moderner_beta:preset']
            refs = (gen['provider_settings'], gen['biome_source']['provider_settings'], gen['biome_source']['cave_provider_settings'])
            assert preset in {'historical_experience:indev_latest' + suffix for suffix in ('', '_no_grass', '_no_modern_content', '_with_ores')}
            assert all(settings == {'moderner_beta:preset': preset} for settings in refs)
            assert prior['Data']['WorldGenSettings']['seed'] == current_level['Data']['WorldGenSettings']['seed']
            assert source['profileId'] == 'historical_experience:indev_latest' and source['generationPreset'] == preset
            assert manifest['fromRevision'] == source['revision'] and manifest['toRevision'] == source['revision'] + 1
            count = len(source['history'])
            assert current['history'][:count] == source['history'] and len(current['history']) > count
            action = current['history'][count]
            assert action['revision'] == manifest['toRevision']
            assert any(action['note'].startswith('version upgrade to historical_experience:' + dest + ' on generation recipe ')
                       for dest in ('infdev_420', 'infdev_611'))
            return {'backup': str(directory.relative_to(world)), 'sourcePreset': preset, 'sourceRevision': source['revision']}
        except (OSError, ValueError, KeyError, AssertionError):
            continue
    raise AssertionError('no hash-verified, same-seed, matching-history Indev source backup')


def main():
    p = argparse.ArgumentParser()
    p.add_argument('fixture')
    p.add_argument('mode', choices=['snapshot', 'check'])
    a = p.parse_args()
    if not a.fixture.replace('-', '').replace('_', '').isalnum():
        raise SystemExit('fixture must be a simple project fixture name')
    world = ROOT / 'run/verification' / a.fixture / 'world'
    saved = ROOT / 'logs' / (a.fixture + '-finite-source.json')
    loaded = chunks(world)
    if a.mode == 'snapshot':
        if saved.exists():
            raise SystemExit('source snapshot already exists; choose a fresh fixture, never overwrite evidence')
        level = census.read_nbt(__import__('gzip').decompress((world / 'level.dat').read_bytes()))
        gen = level['Data']['WorldGenSettings']['dimensions']['minecraft:overworld']['generator']
        preset = gen['provider_settings']['moderner_beta:preset']
        proof = {'storedFinitePreset': preset}
        if 'indev_latest' not in preset:
            # An old upgrade has no finite plan yet. Prove source through hashes, seed, settings and history instead.
            proof = proven_finite_backup(world, level)
            for path in (world / 'historical_experience_finite_transition_backup').glob('*.before'):
                for raw in census.iter_chunks(path):
                    prior = census.read_nbt(raw)
                    key = prior['xPos'], prior['zPos']
                    if prior.get('Status') in TERRAIN and all(-8 <= c < 8 for c in key):
                        assert digest(prior) == digest(loaded[key]), 'release changed an island chunk: ' + str(key)
        values = {f'{x},{z}': digest(tag, 2) for (x, z), tag in loaded.items() if -8 <= x < 8 and -8 <= z < 8}
        assert len(values) == 256, 'full finite island was not captured'
        protected = {}
        if (world / 'finite-repair-build-fixture.json').exists():
            protected = {f'{x},{z}': protected_digest(loaded[x, z]) for x, z in ((8, 0), (9, 0))}
        players = {path.name: hashlib.sha256(path.read_bytes()).hexdigest() for path in (world / 'playerdata').glob('*.dat')}
        saved.write_text(json.dumps({'fixture': a.fixture, 'sourceProof': proof, 'digestVersion': 2,
                                    'islandDigests': values, 'protectedExteriorDigests': protected,
                                    'savedPlayerDataDigests': players}, indent=2) + '\n', encoding='utf8')
        print('Source island chunks snapshotted:', len(values), saved)
        return
    source = json.loads(saved.read_text(encoding='utf8'))
    before = source['islandDigests']
    for key, value in before.items():
        x, z = map(int, key.split(','))
        assert digest(loaded[x, z], source.get('digestVersion', 1)) == value, 'ISLAND BLOCK/CONTAINER CHANGE: ' + key
    protected = source.get('protectedExteriorDigests', {})
    for key, value in protected.items():
        x, z = map(int, key.split(','))
        assert protected_digest(loaded[x, z]) == value, 'PROTECTED EXTERIOR BLOCK/CONTAINER/BIOME CHANGE: ' + key
    for name, value in source.get('savedPlayerDataDigests', {}).items():
        assert Path(name).name == name
        assert hashlib.sha256((world / 'playerdata' / name).read_bytes()).hexdigest() == value, 'PLAYER DATA CHANGE: ' + name
    plan = json.loads((world / 'historical_experience_finite_transition.json').read_text(encoding='utf8'))
    assert plan['complete']
    pairs = []
    retained_pairs = []
    for index, expected in enumerate(plan['edgeHeights']):
        if expected < 0:
            continue
        side, offset = divmod(index, 256)
        x, z = ((offset-128, -129), (offset-128, 128), (-129, offset-128), (128, offset-128))[side]
        chunk = loaded.get((x // 16, z // 16))
        if chunk is None:
            continue
        actual, high_bedrock = height_and_high_bedrock(chunk, x, z)
        pair = {'x': x, 'z': z, 'sourceHeight': expected, 'destinationHeight': actual, 'highBedrock': high_bedrock}
        # Only explicitly snapshotted, independently verified preserved builds may retain the old wall.
        (retained_pairs if f'{x // 16},{z // 16}' in protected else pairs).append(pair)
    result = {'fixture': a.fixture, 'preservedIslandChunks': len(before), 'releasedChunks': plan['releasedChunks'],
              'preservedExteriorChunks': plan['preservedExteriorChunks'],
              'knownEdgeColumns': sum(h >= 0 for h in plan['edgeHeights']), 'sampledPairs': len(pairs),
              'maxEdgeHeightDifference': max((abs(r['sourceHeight'] - r['destinationHeight']) for r in pairs), default=None),
              'highBedrockBlocksInSampledColumns': sum(r['highBedrock'] for r in pairs),
              'protectedExteriorChunksPreserved': len(protected),
              'savedPlayerDataFilesPreserved': len(source.get('savedPlayerDataDigests', {})),
              'intentionallyPreservedBoundaryColumns': len(retained_pairs), 'retainedPairs': retained_pairs, 'pairs': pairs}
    path = ROOT / 'logs' / (a.fixture + '-finite-check.json')
    path.write_text(json.dumps(result, indent=2) + '\n', encoding='utf8')
    print({k: v for k, v in result.items() if k not in ('pairs', 'retainedPairs')})
    print(path)
    assert pairs, 'no generated boundary pairs were sampled'
    assert result['highBedrockBlocksInSampledColumns'] == 0, 'artificial bedrock wall survived'
    assert result['maxEdgeHeightDifference'] <= 2, 'coast seam exceeds two-block surface tolerance'


if __name__ == '__main__':
    main()
