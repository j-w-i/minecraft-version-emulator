"""Bounded, isolated 1.21.1 server run. Stops normally and retains saved-chunk evidence.

Only touches its explicit fixture under run/verification. Never kills unrelated Java processes.
Uses the project's already accepted EULA, never accepts it on the user's behalf.
"""
import argparse
import importlib.util
import json
import os
from pathlib import Path
import shutil
import subprocess

ROOT = Path(__file__).resolve().parents[1]
PROJECT = ROOT
spec = importlib.util.spec_from_file_location('census', ROOT / 'scripts/region-block-census.py')
census = importlib.util.module_from_spec(spec)
spec.loader.exec_module(census)


def main():
    p = argparse.ArgumentParser()
    p.add_argument('fixture')
    p.add_argument('--preset', default='historical_experience:beta_1_7_3')
    p.add_argument('--radius', type=int, default=3)
    p.add_argument('--center', type=int, default=0)
    p.add_argument('--reload', action='store_true')
    p.add_argument('--seed', default='hexbackport')
    p.add_argument('--production', action='store_true', help='verify the remapped distributable, not dev classes')
    p.add_argument('--upgrade-to', help='exercise the actual operator command, followed by normal stop')
    p.add_argument('--repair-finite-edge', action='store_true', help='stage the actual explicit repair command in an already-upgraded fixture')
    p.add_argument('--prepare-finite-repair-fixture', action='store_true', help='place real exterior platform/chest and explicit synthetic player-data sentinel in a NEW diagnostic replica')
    a = p.parse_args()
    if a.prepare_finite_repair_fixture and (not a.reload or a.upgrade_to or a.repair_finite_edge):
        raise SystemExit('prepare-build requires a diagnostic existing replica, separately from repair/upgrade')
    if a.repair_finite_edge and (not a.reload or a.upgrade_to):
        raise SystemExit('repair requires an existing fixture and cannot interleave with a version upgrade')
    import uuid
    token = uuid.uuid4().hex
    if not a.fixture.replace('-', '').replace('_', '').isalnum():
        raise SystemExit('fixture must be a simple name')
    run = PROJECT / 'run/verification' / a.fixture
    if a.reload:
        if not (run / 'world/level.dat').exists():
            raise SystemExit('reload requires an existing fixture')
    else:
        if run.exists():
            raise SystemExit('fresh fixture already exists; choose a new name')
        eula = PROJECT / 'run/eula.txt'
        if 'eula=true' not in eula.read_text(encoding='utf-8'):
            raise SystemExit('project EULA has not already been accepted')
        run.mkdir(parents=True)
        shutil.copyfile(eula, run / 'eula.txt')
        (run / 'server.properties').write_text('\n'.join([
            'level-name=world', 'level-type=' + a.preset, 'level-seed=' + a.seed,
            'generator-settings={}', 'generate-structures=true', 'online-mode=false',
            'server-ip=127.0.0.1', 'server-port=25641', 'view-distance=3',
            'simulation-distance=3', 'spawn-protection=0', 'max-tick-time=120000',
            'sync-chunk-writes=true', 'enable-rcon=false', 'motd=Isolated generation test',
        ]) + '\n', encoding='utf-8')
    label = a.fixture + ('-reload' if a.reload else '-fresh') + ('-far' if a.center else '')
    log = ROOT / 'logs' / ('sol-1211-' + label + '.log')
    env = dict(os.environ)
    env['JAVA_HOME'] = r'C:\Program Files\Java\jdk-25.0.4.1'
    env['PATH'] = env['JAVA_HOME'] + r'\bin;' + env['PATH']
    command = ['cmd.exe', '/d', '/c', 'gradlew.bat', '--console=plain',
               'runServer', '-PgenerationProbe=true', '-PgenerationRunDir=' + str(run),
               '-PprobeRadius=' + str(a.radius), '-PprobeCenter=' + str(a.center),
               '-PprobeToken=' + token]
    if a.production:
        # Official, unremapped dependencies; never copy the Loom named development artifacts.
        cache = Path.home() / '.gradle/caches/modules-2/files-2.1'
        mods = run / 'mods'
        mods.mkdir(exist_ok=True)
        artifacts = [PROJECT / 'build/libs/historical-experience-1211-0.1.0.jar',
            next((cache / 'maven.modrinth/moderner-beta/5.0.0-alpha.4+1.21.1-fabric').glob('*/*.jar')),
            next((cache / 'net.fabricmc.fabric-api/fabric-api/0.116.5+1.21.1').glob('*/*.jar'))]
        for artifact in artifacts:
            shutil.copyfile(artifact, mods / artifact.name)
        launcher = ROOT / '.upstream/fabric1211/fabric-server-launch.jar'
        command = [r'C:\Program Files\Java\jdk-21\bin\java.exe', '-Xmx2G',
                   '-Dhistorical_experience.generationProbe=true',
                   '-Dhistorical_experience.generationProbe.token=' + token,
                   '-Dhistorical_experience.generationProbe.radius=' + str(a.radius),
                   '-Dhistorical_experience.generationProbe.center=' + str(a.center),
                   '-jar', str(launcher), 'nogui']
    if a.upgrade_to:
        if a.production:
            command.insert(-3, '-Dhistorical_experience.generationProbe.upgradeTo=' + a.upgrade_to)
        else:
            command.append('-PprobeUpgradeTo=' + a.upgrade_to)
        label += '-upgrade'
        log = ROOT / 'logs' / ('sol-1211-' + label + '.log')
    if a.prepare_finite_repair_fixture:
        if a.production:
            command.insert(-3, '-Dhistorical_experience.generationProbe.prepareFiniteRepairFixture=true')
        else:
            command.append('-PprobePrepareFiniteRepairFixture=true')
        label += '-prepare-build'
        log = ROOT / 'logs' / ('sol-1211-' + label + '.log')
    if a.repair_finite_edge:
        if a.production:
            command.insert(-3, '-Dhistorical_experience.generationProbe.repairFiniteEdge=true')
        else:
            command.append('-PprobeRepairFiniteEdge=true')
        label += '-repair'
        log = ROOT / 'logs' / ('sol-1211-' + label + '.log')
    with log.open('w', encoding='utf-8') as output:
        proc = subprocess.Popen(command, cwd=run if a.production else PROJECT, env=env, stdin=subprocess.DEVNULL,
                                stdout=output, stderr=subprocess.STDOUT)
        try:
            exit_code = proc.wait(timeout=300)
        except subprocess.TimeoutExpired:
            # This is the verified process we started, not all Java processes or all recent processes.
            subprocess.run(['taskkill', '/PID', str(proc.pid), '/T', '/F'], check=False)
            raise SystemExit('Owned verification process timed out; see ' + str(log))
    print('Server exit:', exit_code, 'log:', log)
    if exit_code:
        raise SystemExit(exit_code)
    evidence = json.loads((run / 'world/generation-probe.json').read_text(encoding='utf-8'))
    if evidence.get('runToken') != token:
        raise SystemExit('No fresh probe from this process (startup failed before verification); see ' + str(log))
    if not evidence.get('success'):
        raise SystemExit('Probe failed: ' + str(evidence))
    result = census.census_world(run / 'world')
    # Hash semantic section contents of completed chunks, not compressed bytes or mutable timestamps.
    import hashlib
    full = {}
    starts = {}
    container_hashes = {}
    for region in sorted((run / 'world/region').glob('*.mca')):
        for raw in census.iter_chunks(region):
            chunk = census.read_nbt(raw)
            if chunk.get('Status') not in ('minecraft:full', 'full'):
                continue
            blocks = []
            for section in sorted(chunk.get('sections', []), key=lambda s: s['Y']):
                states = section.get('block_states')
                if not states:
                    continue
                palette = states['palette']
                names = [json.dumps(state, sort_keys=True) for state in palette]
                indices = [0] * 4096 if len(palette) == 1 else census.unpack_indices(
                    states.get('data', []), max(4, (len(palette) - 1).bit_length()), 4096)
                if len(indices) != 4096:
                    raise SystemExit('Malformed completed section in ' + str(region))
                blocks.append([section['Y'], [names[i] for i in indices]])
            key = str(chunk['xPos']) + ',' + str(chunk['zPos'])
            full[key] = hashlib.sha256(json.dumps(blocks, separators=(',', ':')).encode()).hexdigest()
            containers = sorted(chunk.get('block_entities', []),
                key=lambda b: (b.get('x', 0), b.get('y', 0), b.get('z', 0)))
            container_hashes[key] = hashlib.sha256(json.dumps(containers, sort_keys=True).encode()).hexdigest()
            for name, start in chunk.get('structures', {}).get('starts', {}).items():
                if start.get('id') not in (None, 'INVALID'):
                    starts[name] = starts.get(name, 0) + 1
    evidence['fullChunkBlockHashes'] = full
    evidence['fullChunkBlockEntityHashes'] = container_hashes
    evidence['savedValidStructureStarts'] = starts
    result['counts'] = dict(result['counts'])
    evidence['savedChunkCensus'] = result
    evidence['launchMode'] = 'remapped-production' if a.production else 'loom-development'
    if a.production:
        evidence['loadedModArtifacts'] = [{'file': artifact.name, 'sha256': hashlib.sha256((mods / artifact.name).read_bytes()).hexdigest()}
                                          for artifact in artifacts]
    evidence['requestedCenterChunk'] = a.center
    evidence['requestedRadius'] = a.radius
    evidence['requestedUpgradeTarget'] = a.upgrade_to
    evidence['requestedFiniteEdgeRepair'] = a.repair_finite_edge
    evidence['requestedFiniteRepairBuildPreparation'] = a.prepare_finite_repair_fixture
    # Keep immutable token-named copies as well as the convenient latest label. Repeated live
    # commands / different distant samples must not erase the evidence of an earlier stage.
    retained_log = log.with_name(log.stem + '-' + token + log.suffix)
    shutil.copyfile(log, retained_log)
    evidence['retainedServerLog'] = str(retained_log.relative_to(ROOT))
    destination = ROOT / 'logs' / ('sol-1211-' + label + '.json')
    encoded = json.dumps(evidence, indent=2) + '\n'
    destination.write_text(encoded, encoding='utf-8')
    destination.with_name(destination.stem + '-' + token + destination.suffix).write_text(encoded, encoding='utf-8')
    print('Evidence:', destination)
    print('Alias:', evidence.get('alias'), 'chunks:', result['chunks'])
    print('Features:', evidence['features'])
    print('Ores:', evidence['ores'])
    print('Blocks:', sorted(result['counts']))


if __name__ == '__main__':
    main()
