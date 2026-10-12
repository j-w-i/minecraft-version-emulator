"""Validate the final packaged artifact and retained 1.21.1 evidence, then publish a small JSON summary."""
import gzip
import hashlib
import importlib.util
import json
from pathlib import Path
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[1]
PROJECT = ROOT
jar = PROJECT / 'build/libs/historical-experience-1211-0.1.0.jar'
suites = [ET.parse(path).getroot() for path in (PROJECT / 'build/test-results/test').glob('TEST-*.xml')]
counts = {key: sum(int(s.get(key, '0')) for s in suites) for key in ['tests', 'failures', 'errors', 'skipped']}
assert counts['tests'] == 187 and counts['failures'] == counts['errors'] == counts['skipped'] == 0, counts
matrix = json.loads((ROOT / 'logs/sol-1211-matrix-summary.json').read_text(encoding='utf-8'))
assert matrix['success'] and len(matrix['runs']) == 15
upgrades = json.loads((ROOT / 'logs/sol-1211-upgrade-summary.json').read_text(encoding='utf-8'))
assert upgrades['success'] and len(upgrades['steps']) == 11
with zipfile.ZipFile(jar) as archive:
    mixins = json.loads(archive.read('historical_experience.mixins.json'))
    assert mixins['required']
    assert set(mixins['mixins']) == {'FeaturePlacerHistoricalContentMixin', 'ChunkGeneratorHistoricalOreMixin',
        'TreeDecoratorContextHistoricalSuppressionMixin', 'StructureHistoricalSuppressionMixin',
        'NaturalSpawnerHistoricalSpawnMixin', 'LevelStorageHistoricalUpgradeRecoveryMixin'}
    assert 'THIRD_PARTY_NOTICES.md' in archive.namelist()
    assert not any(name.startswith('data/historical_experience/worldgen/noise_settings/')
                   or name.startswith('data/historical_experience/worldgen/density_function/') for name in archive.namelist())
    packaged_mod = json.loads(archive.read('fabric.mod.json'))
    assert packaged_mod['depends']['minecraft'] == '1.21.1'
    public = json.loads(archive.read('data/minecraft/tags/worldgen/world_preset/normal.json'))
    assert public['replace'] is False and len(public['values']) == 10
spec = importlib.util.spec_from_file_location('census', ROOT / 'scripts/region-block-census.py')
census = importlib.util.module_from_spec(spec)
spec.loader.exec_module(census)
world = PROJECT / 'run/verification/sol-production-beta/world'
nbt = census.read_nbt(gzip.decompress((world / 'level.dat').read_bytes()))
stored = nbt['Data']['WorldGenSettings']['dimensions']['minecraft:overworld']['generator']
refs = [stored['provider_settings']['moderner_beta:preset'],
        stored['biome_source']['provider_settings']['moderner_beta:preset'],
        stored['biome_source']['cave_provider_settings']['moderner_beta:preset']]
assert refs == ['historical_experience:beta_1_7_3_content_preview_9'] * 3, refs
fresh = json.loads((ROOT / 'logs/sol-1211-sol-production-beta-fresh.json').read_text(encoding='utf-8'))
final_reload = json.loads((ROOT / 'logs/sol-1211-sol-production-beta-reload.json').read_text(encoding='utf-8'))
assert final_reload['success'] and not final_reload['sampledPaletteFailures'] and not final_reload['sampledUnknownBlocks']
for pos, digest in fresh['fullChunkBlockHashes'].items():
    assert final_reload['fullChunkBlockHashes'].get(pos) == digest, pos
assert fresh['spawns']['removedEntries'] != '0'
summary = {'success': True, 'minecraft': '1.21.1', 'jar': str(jar.relative_to(ROOT)),
    'sha256': hashlib.sha256(jar.read_bytes()).hexdigest(), 'bytes': jar.stat().st_size,
    'testSuites': len(suites), **counts, 'matrixServerRuns': len(matrix['runs']), 'worldUpgradeServerRuns': len(upgrades['steps']),
    'finalArtifactProductionReloadPassed': True, 'savedNBTDataVersion': nbt['Data']['DataVersion'],
    'savedNBTRecipeReferences': refs, 'historicalGenerationMixinsPackaged': [m for m in mixins['mixins'] if m != 'LevelStorageHistoricalUpgradeRecoveryMixin'],
    'earlyUpgradeRecoveryMixinPackaged': 'LevelStorageHistoricalUpgradeRecoveryMixin' in mixins['mixins'],
    'sourceNoticesIncluded': True, 'noVendoredNoiseRouter': True, 'publicWorldTypes': public['values'],
    'preservedFullChunkHashes': len(fresh['fullChunkBlockHashes'])}
(ROOT / 'logs/sol-1211-final-verification-summary.json').write_text(json.dumps(summary, indent=2) + '\n', encoding='utf-8')
print(json.dumps(summary, indent=2))
