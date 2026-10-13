"""Run every implemented public 26.3 profile on 1.21.1 plus preservation/isolation controls."""
import json
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
VERIFY = ROOT / 'scripts/verify-generation-1211.py'
NORMAL_TAG = ROOT / 'src/main/resources/data/minecraft/tags/worldgen/world_preset/normal.json'
rows = []

# The versions that predate Beta 1.8, which are the ones that must not generate short grass. Kept in step with
# HistoricalGameplayTweaks.preBeta18, which PreBeta18BoundaryTest asserts against the registry; this list exists
# only because the assertion below runs per measured fixture.
PRE_BETA_1_8 = ('alpha', 'infdev_420', 'infdev_611')


def selectable_versions():
    """Every version world creation can choose, read from the tag the create-world screen searches.

    Derived rather than listed: a hardcoded list meant a newly declared version was registered, made selectable,
    and then skipped by the only harness that measures saved chunks - with nothing reporting that it had been
    skipped.
    """
    values = json.loads(NORMAL_TAG.read_text(encoding='utf-8'))['values']
    return [value.split(':', 1)[1] for value in values if value.startswith('historical_experience:')]


def execute(name, preset, production=False, reload=False, center=0, seed='hexbackport'):
    command = [sys.executable, str(VERIFY), name, '--preset', preset, '--radius', '3',
               '--center', str(center), '--seed', seed]
    if production:
        command.append('--production')
    if reload:
        command.append('--reload')
    subprocess.run(command, cwd=ROOT, check=True)
    label = name + ('-reload' if reload else '-fresh') + ('-far' if center else '')
    path = ROOT / 'logs' / ('sol-1211-' + label + '.json')
    evidence = json.loads(path.read_text(encoding='utf-8'))
    assert evidence['success'], label
    assert evidence['fullChunkBlockHashes'], 'No completed saved chunks: ' + label
    if preset.startswith('historical_experience:'):
        assert evidence['alias'].startswith('historical_experience:'), label
        assert evidence['generatorMinY'] == 0 and evidence['generatorDepth'] == 128, label
        assert all(evidence['appliedSettings'][key] for key in ['chunkMatches', 'biomeMatches', 'caveMatches']), label
        assert evidence['appliedDeltas']['deepslateDisabled'], label
        assert evidence['features']['unresolvedExclusions'] == '[]', label
        assert not set(evidence['biomes']) & {'minecraft:lush_caves', 'minecraft:dripstone_caves', 'minecraft:deep_dark'}, label
    else:
        assert evidence.get('alias') is None, label
        assert evidence['features']['skippedPlacements'] == '0', label
        assert evidence['ores']['veinsAttempted'] == '0', label
        assert evidence['structureSuppressions'] == 0, label
        assert len(evidence['decoratorControlWrites']) == 3, label
        fixture = ROOT / 'run/verification' / name / 'world'
        assert not (fixture / 'historical_experience_metadata.json').exists(), label
    if preset == 'historical_experience:beta_1_7_3':
        assert evidence['sampledPaletteFailures'] == {}, label
        assert evidence['sampledUnknownBlocks'] == {}, label
        assert evidence['savedValidStructureStarts'] == {}, label
        assert len(evidence['decoratorControlWrites']) == 1, label
        blocks = evidence['sampledFullBlockCounts']
        assert blocks.get('minecraft:bedrock', 0) > 0, label
        for ore in ['coal', 'iron', 'gold', 'diamond', 'lapis', 'redstone']:
            assert blocks.get('minecraft:' + ore + '_ore', 0) > 0, (label, ore)
    rows.append({'fixture': label, 'preset': preset, 'productionArtifact': production,
                 'savedFullChunks': len(evidence['fullChunkBlockHashes']),
                 'sampledFullChunks': evidence['sampledFullChunks'], 'passed': True})
    return evidence


try:
    before = execute('sol-production-beta', 'historical_experience:beta_1_7_3', production=True)
    metadata = ROOT / 'run/verification/sol-production-beta/world/historical_experience_metadata.json'
    metadata_before = metadata.read_bytes()
    after = execute('sol-production-beta', 'historical_experience:beta_1_7_3', production=True, reload=True)
    assert metadata.read_bytes() == metadata_before, 'Reload changed metadata'
    for pos, digest in before['fullChunkBlockHashes'].items():
        assert after['fullChunkBlockHashes'].get(pos) == digest, 'Reload changed chunk blocks at ' + pos
    far = execute('sol-production-beta', 'historical_experience:beta_1_7_3', production=True, reload=True, center=64)
    for pos, digest in before['fullChunkBlockHashes'].items():
        assert far['fullChunkBlockHashes'].get(pos) == digest, 'New terrain changed an old chunk at ' + pos
    execute('sol-production-beta-seed2', 'historical_experience:beta_1_7_3', production=True, seed='12345')
    # The versions to measure are READ from the tag world creation searches, not listed here. A hardcoded list
    # meant a newly declared version was skipped by the only harness that measures saved chunks - it would have
    # been registered, made selectable in the GUI, and never measured, with nothing saying so.
    for version in selectable_versions():
        evidence = execute('sol-matrix-' + version, 'historical_experience:' + version)
        if version in PRE_BETA_1_8:
            assert evidence['sampledFullBlockCounts'].get('minecraft:short_grass', 0) == 0, version
    execute('sol-negative-vanilla', 'minecraft:normal')
    execute('sol-negative-mb', 'moderner_beta:moderner_beta')
except Exception as failure:
    (ROOT / 'logs/sol-1211-matrix-summary.json').write_text(json.dumps({
        'success': False, 'failure': str(failure), 'runs': rows}, indent=2) + '\n', encoding='utf-8')
    raise
(ROOT / 'logs/sol-1211-matrix-summary.json').write_text(json.dumps({
    'success': True, 'runs': rows, 'savedReloadPreserved': True,
    'futureGenerationPreservedOldChunks': True}, indent=2) + '\n', encoding='utf-8')
print('ALL GENERATION MATRIX CONTROLS PASSED:', len(rows), 'server runs')
