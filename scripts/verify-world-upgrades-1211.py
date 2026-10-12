"""Production-server operator upgrades, restart/future chunks, real backup recovery and refusals.
Every fault is injected into an owned fixture COPY, never into the original evidence or a user world.
"""
import gzip
import hashlib
import json
from pathlib import Path
import shutil
import struct
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
FIXTURES = ROOT / 'run/verification'
VERIFY = ROOT / 'scripts/verify-generation-1211.py'
JOURNAL = 'historical_experience_upgrade.journal'
METADATA = 'historical_experience_metadata.json'
SOURCE = 'historical_experience:beta_1_7_3_content_preview_4'
BETA = 'historical_experience:beta_1_7_3_content_preview_9'
RELEASE = 'historical_experience:release_1_2_5_cave_fix'
steps = []
PREFIX = sys.argv[1] if len(sys.argv) > 1 else ''
assert not PREFIX or PREFIX.replace('-', '').replace('_', '').isalnum(), 'Prefix must be a simple name'


def fixture(name):
    return FIXTURES / (PREFIX + name if name.startswith('sol-upgrade') else name)


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def copy_fixture(source, destination):
    src, dst = fixture(source).resolve(), fixture(destination).resolve()
    assert src.is_relative_to(FIXTURES.resolve()) and dst.is_relative_to(FIXTURES.resolve())
    assert src != dst and not dst.exists(), (src, dst)
    shutil.copytree(src, dst)
    return dst / 'world'


def run(name, *, reload=True, center=0, upgrade=None, preset=None, accepted=True):
    name = fixture(name).name
    args = [sys.executable, str(VERIFY), name, '--production', '--radius', '3', '--center', str(center)]
    if reload:
        args.append('--reload')
    if preset:
        args += ['--preset', preset]
    if upgrade:
        args += ['--upgrade-to', upgrade]
    result = subprocess.run(args, cwd=ROOT)
    label = name + ('-reload' if reload else '-fresh') + ('-far' if center else '') + ('-upgrade' if upgrade else '')
    if not accepted:
        assert result.returncode != 0, 'Negative control unexpectedly passed: ' + label
        steps.append({'fixture': label, 'expectedRefusal': True, 'passed': True})
        return None
    assert result.returncode == 0, label
    evidence = json.loads((ROOT / 'logs' / ('sol-1211-' + label + '.json')).read_text(encoding='utf-8'))
    assert evidence['success'] and evidence.get('runToken'), label
    steps.append({'fixture': label, 'alias': evidence['alias'], 'passed': True})
    return evidence


def preserved(before, after):
    for field in ['fullChunkBlockHashes', 'fullChunkBlockEntityHashes']:
        for pos, value in before[field].items():
            assert after[field].get(pos) == value, (field, pos)


def state(world):
    return json.loads((world / METADATA).read_text(encoding='utf-8'))


def check_backups(world):
    manifests = sorted((world / 'historical_experience_backups').glob('*/backup-manifest.json'))
    for path in manifests:
        document = json.loads(path.read_text(encoding='utf-8'))
        assert {e['relativePath'] for e in document['entries']} == {METADATA, 'level.dat'}
        for entry in document['entries']:
            file = path.parent / entry['relativePath'].replace('/', '_')
            assert file.stat().st_size == entry['size'] and digest(file) == entry['sha256'], path
    return manifests


def inject_pending(copy_name, baseline_name, completed_metadata, manifest, published=False, corrupt=False):
    world = copy_fixture(baseline_name, copy_name)
    backup = world / manifest.parent.relative_to(fixture('sol-upgrade-1211') / 'world')
    shutil.copyfile(backup / 'level.dat', world / 'level.dat')
    shutil.copyfile(backup / METADATA, world / METADATA)
    if published:
        (world / METADATA).write_text(json.dumps(completed_metadata, indent=2), encoding='utf-8')
    document = json.loads(manifest.read_text(encoding='utf-8'))
    (world / JOURNAL).write_text(json.dumps({'schema': 1,
        'phase': 'METADATA_APPLIED' if published else 'BACKUP_WRITTEN',
        'timestamp': document['timestamp'], 'backupDirectory': str(backup.relative_to(world)).replace('\\', '/'),
        'fromPreset': SOURCE, 'targetMetadataJson': json.dumps(completed_metadata)}), encoding='utf-8')
    if corrupt:
        with (backup / 'level.dat').open('ab') as stream:
            stream.write(b'corrupt-backup-negative-control')
    return world


# Narrow retry for the last assertion's Gson null-omission bug. All prior upgrade/recovery
# assertions must already have completed; retry the real ordinary-world command rather than
# rebuilding every successful fixture or relabelling an unchecked startup as success.
if len(sys.argv) > 2 and sys.argv[2] == '--resume-final':
    summary_path = ROOT / 'logs/sol-1211-upgrade-summary.json'
    previous = json.loads(summary_path.read_text(encoding='utf-8'))
    assert previous['success'] is False and previous['failure'] == "'alias'", previous
    assert len(previous['steps']) == 11 and all(step['passed'] for step in previous['steps'])
    steps.extend(previous['steps'][:-1])
    ordinary = fixture('sol-upgrade-ordinary-refusal') / 'world'
    run('sol-upgrade-ordinary-refusal', upgrade='beta_1_7_3', accepted=False)
    refusal = json.loads((ordinary / 'generation-probe.json').read_text(encoding='utf-8'))
    assert refusal['upgradeCommandResult'] == 0 and refusal['success'] is False and refusal.get('alias') is None
    assert not (ordinary / METADATA).exists() and not (ordinary / JOURNAL).exists()
    summary_path.write_text(json.dumps({'success': True, 'steps': steps,
        'sameVersionRecipeUpgrade': True, 'crossEraUpgrade': True,
        'earlyRecoveryBeforeDecode': True, 'corruptBackupRefusedWithoutWrites': True,
        'oldBlocksContainersAndPlayerFilePreserved': True,
        'finalOrdinaryAssertionRetriedAfterNullOmissionFix': True}, indent=2) + '\n', encoding='utf-8')
    print('ALL WORLD-UPGRADE CONTROLS PASSED:', len(steps), 'production server runs')
    raise SystemExit(0)

try:
    name = 'sol-upgrade-1211'
    before = run(name, reload=False, preset=SOURCE)
    world = fixture(name) / 'world'
    # Valid compressed player-data NBT sentinel: the upgrade never loads or rewrites this player's file.
    player = world / 'playerdata/00000000-0000-0000-0000-000000000001.dat'
    player.parent.mkdir(exist_ok=True)
    def string(value):
        encoded = value.encode()
        return struct.pack('>H', len(encoded)) + encoded
    player.write_bytes(gzip.compress(b'\x0a\x00\x00\x03' + string('XpLevel') + struct.pack('>i', 17)
        + b'\x08' + string('InventorySentinel') + string('preserve-player-inventory') + b'\x00'))
    player_before = digest(player)
    staged = run(name, upgrade='historical_experience:beta_1_7_3')
    preserved(before, staged)
    assert staged['loadedAliasAfterStaging'] == SOURCE
    assert staged['upgradeStaged']['generationPreset'] == BETA
    assert SOURCE in staged['upgradeStaged']['storedGenerator']
    assert staged['upgradeStaged']['journalPending'] == 'true'
    assert not (world / JOURNAL).exists(), 'Normal stop did not finish the staged transaction'
    assert state(world)['generationPreset'] == BETA
    manifests = check_backups(world)
    assert len(manifests) == 1
    completed_beta = state(world)
    copy_fixture(name, 'sol-upgrade-1211-beta-checkpoint')
    after = run(name, center=64)
    preserved(before, after)
    assert after['alias'] == BETA and after['ores']['veinsAttempted'] != '0'
    assert after['structureSuppressions'] > 0 and after['sampledPaletteFailures'] == {}
    assert after['appliedSettings']['chunkMatches'] and after['appliedSettings']['caveMatches']
    assert digest(player) == player_before
    era_staged = run(name, center=64, upgrade='release_1_2_5')
    assert era_staged['loadedAliasAfterStaging'] == BETA
    assert not (world / JOURNAL).exists()
    assert len(check_backups(world)) == 2
    era = run(name, center=128)
    preserved(before, era)
    preserved(after, era)
    assert era['alias'] == RELEASE
    assert era['historicalState']['profileId'] == 'historical_experience:release_1_2_5'
    assert all(era['appliedSettings'][key] for key in ['chunkMatches', 'biomeMatches', 'caveMatches'])
    assert len(era['fullChunkBlockHashes']) > len(after['fullChunkBlockHashes'])
    assert digest(player) == player_before
    for published in [False, True]:
        recovery_name = 'sol-upgrade-recovery-' + ('published' if published else 'journal')
        restored = inject_pending(recovery_name, 'sol-upgrade-1211-beta-checkpoint', completed_beta,
                                  manifests[0], published=published)
        recovered = run(recovery_name, center=96)
        assert recovered['alias'] == BETA and recovered['historicalState']['generationPreset'] == BETA
        assert not (restored / JOURNAL).exists()
        preserved(before, recovered)
    corrupt = inject_pending('sol-upgrade-corrupt-backup', 'sol-upgrade-1211-beta-checkpoint',
                             completed_beta, manifests[0], corrupt=True)
    protected = {file.relative_to(corrupt): digest(file) for file in [corrupt / METADATA, corrupt / 'level.dat']}
    region_before = {file.name: digest(file) for file in (corrupt / 'region').glob('*.mca')}
    run('sol-upgrade-corrupt-backup', accepted=False)
    corruption_log = ROOT / 'logs' / ('sol-1211-' + fixture('sol-upgrade-corrupt-backup').name + '-reload.log')
    assert 'backup of level.dat has size' in corruption_log.read_text(encoding='utf-8'), 'Wrong refusal reason'
    assert (corrupt / JOURNAL).exists()
    assert all(digest(corrupt / name) == value for name, value in protected.items())
    assert all(digest(corrupt / 'region' / name) == value for name, value in region_before.items())
    for invalid in ['unknown_version', 'historical_experience:release_1_2_5']:
        metadata_before = (world / METADATA).read_bytes()
        backups_before = len(check_backups(world))
        run('sol-upgrade-1211', upgrade=invalid, accepted=False)
        refusal = json.loads((world / 'generation-probe.json').read_text(encoding='utf-8'))
        assert refusal['upgradeCommandResult'] == 0 and refusal['success'] is False, invalid
        assert refusal['alias'] == RELEASE, invalid
        assert (world / METADATA).read_bytes() == metadata_before and not (world / JOURNAL).exists()
        assert len(check_backups(world)) == backups_before
    ordinary = copy_fixture('sol-negative-vanilla', 'sol-upgrade-ordinary-refusal')
    run('sol-upgrade-ordinary-refusal', upgrade='beta_1_7_3', accepted=False)
    refusal = json.loads((ordinary / 'generation-probe.json').read_text(encoding='utf-8'))
    assert refusal['upgradeCommandResult'] == 0 and refusal['success'] is False and refusal.get('alias') is None
    assert not (ordinary / METADATA).exists() and not (ordinary / JOURNAL).exists()
except Exception as failure:
    (ROOT / 'logs/sol-1211-upgrade-summary.json').write_text(json.dumps({'success': False,
        'failure': str(failure), 'steps': steps}, indent=2) + '\n', encoding='utf-8')
    raise
(ROOT / 'logs/sol-1211-upgrade-summary.json').write_text(json.dumps({'success': True,
    'steps': steps, 'sameVersionRecipeUpgrade': True, 'crossEraUpgrade': True,
    'earlyRecoveryBeforeDecode': True, 'corruptBackupRefusedWithoutWrites': True,
    'oldBlocksContainersAndPlayerFilePreserved': True}, indent=2) + '\n', encoding='utf-8')
print('ALL WORLD-UPGRADE CONTROLS PASSED:', len(steps), 'production server runs')
