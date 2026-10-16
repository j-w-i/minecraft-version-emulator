"""SPDX-License-Identifier: LGPL-3.0-or-later
Create a NEW diagnostic old-upgrade replica under run/verification, never a production repair tool.

Copies a stopped project fixture and reinstates verified immutable pre-migration region images ONLY
in the new copy. Source files are never changed. Excludes transition state, reinstates old biome bits,
and reuses the fixture's previously accepted EULA. Refuses an existing destination or unsafe names.
"""
import argparse
import hashlib
import json
import msvcrt
from pathlib import Path
import re
import shutil
import struct

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / 'run/verification'
PLAN = 'historical_experience_finite_transition.json'
BACKUP = 'historical_experience_finite_transition_backup'
REPAIR = 'historical_experience_finite_edge_repair.json'


def fixture(name):
    if not re.fullmatch(r'sol-[a-z0-9-]+', name):
        raise SystemExit('Only simple sol- project fixtures are accepted')
    path = BASE / name
    if path.is_symlink():
        raise SystemExit('Symlink fixture is not accepted')
    return path


def source_fingerprint(world):
    result = {}
    for path in world.rglob('*'):
        if path.is_symlink():
            raise SystemExit('Symlink in source world is not accepted')
        if path.is_file() and path != world / 'session.lock':
            result[str(path.relative_to(world))] = hashlib.sha256(path.read_bytes()).hexdigest()
    return result


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('source')
    parser.add_argument('destination')
    args = parser.parse_args()
    source, target = fixture(args.source), fixture(args.destination)
    if target.exists():
        raise SystemExit('Destination must be new; diagnostic fixtures are never overwritten')
    world = source / 'world'
    if world.is_symlink():
        raise SystemExit('Symlink source world is not accepted')
    with (world / 'session.lock').open('r+b') as lock:
        try:
            msvcrt.locking(lock.fileno(), msvcrt.LK_NBLCK, 1)
        except OSError as exc:
            raise SystemExit('Source must be stopped and unlocked') from exc
        try:
            plan = json.loads((world / PLAN).read_text(encoding='utf-8'))
            if plan.get('schema') != 3 or not plan.get('complete'):
                raise SystemExit('Source must have a complete, world-bound schema-3 plan')
            images = []
            for change in plan['changes']:
                name = change['region']
                if not re.fullmatch(r'r\.-?\d+\.-?\d+\.mca', name):
                    raise SystemExit('Unsafe region name')
                before = (world / BACKUP / (name + '.before')).read_bytes()
                if hashlib.sha256(before).hexdigest() != change['beforeHash']:
                    raise SystemExit('Corrupt immutable source image: ' + name)
                images.append((name, before, change['slots']))
            if not images or 'eula=true' not in (source / 'eula.txt').read_text(encoding='utf-8'):
                raise SystemExit('Missing finite source images or previously accepted fixture EULA')
            source_before = source_fingerprint(world)
            shutil.copytree(world, target / 'world', ignore=shutil.ignore_patterns(PLAN, BACKUP, REPAIR, 'session.lock'))
            for name in ('eula.txt', 'server.properties'):
                shutil.copyfile(source / name, target / name)
            for name, before, _ in images:
                (target / 'world/region' / name).write_bytes(before)
            # The old upgrade took its pre-existing chunk census before clearing the apron.
            record_file = target / 'world/historical_experience_biome_conversion.json'
            synthetic_record = not record_file.exists()
            if synthetic_record:
                # Dedicated-server upgrades need not run the optional GUI biome conversion. Add a
                # clearly recorded diagnostic old-census record so this replica exercises pruning too.
                metadata = json.loads((world / 'historical_experience_metadata.json').read_text(encoding='utf-8'))
                if metadata['profileId'] not in ('historical_experience:infdev_420', 'historical_experience:infdev_611'):
                    raise SystemExit('Synthetic old biome census is restricted to the two Infdev fixtures')
                record = {'schema': 3, 'completedUpgradeRevision': metadata['revision'],
                          'convertedTargets': ['biome_hierarchy_v4:alpha_only'],
                          'lastConvertedAt': metadata['history'][-1]['timestamp'] + 1, 'convertedChunks': []}
                for name, before, _ in images:
                    _, rx, rz, _ = name.split('.')
                    key = (int(rx) << 32) | (int(rz) & 0xffffffff)
                    mask = ''.join('1' if struct.unpack_from('>I', before, slot * 4)[0] else '0' for slot in range(1024))
                    record['convertedChunks'].append({'region': key, 'chunks': mask})
            else:
                record = json.loads(record_file.read_text(encoding='utf-8'))
            if record.get('schema') != 3:
                raise SystemExit('Expected schema-3 biome record in this diagnostic source')
            masks = {entry['region']: list(entry['chunks']) for entry in record.get('convertedChunks', [])}
            reinstated = 0
            for name, _, slots in images:
                _, rx, rz, _ = name.split('.')
                key = (int(rx) << 32) | (int(rz) & 0xffffffff)
                mask = masks.setdefault(key, list('0' * 1024))
                for slot in slots:
                    if mask[slot] == '0':
                        reinstated += 1
                    mask[slot] = '1'
            record['convertedChunks'] = [{'region': key, 'chunks': ''.join(mask)} for key, mask in sorted(masks.items())]
            record_file.write_text(json.dumps(record, indent=2) + '\n', encoding='utf-8')
            evidence = {'source': args.source, 'destination': args.destination,
                        'restoredRegionImages': len(images), 'reinstatedApronChunks': plan['releasedChunks'],
                        'reinstatedBiomePositions': reinstated, 'syntheticBiomeCensus': synthetic_record,
                        'sourceIdentity': plan['identity']}
            output = ROOT / 'logs' / (args.destination + '-old-replica.json')
            if output.exists():
                raise SystemExit('Refusing to overwrite retained replica provenance')
            marker = (target / 'world/historical_experience_diagnostic_replica.json').resolve()
            if not marker.is_relative_to(target.resolve()):
                raise SystemExit('Diagnostic marker must be inside the NEW target')
            marker.write_text(json.dumps(evidence, indent=2) + '\n', encoding='utf-8')
            if source_fingerprint(world) != source_before:
                raise SystemExit('SOURCE CHANGED during diagnostic setup; no success receipt written')
            evidence['sourceFilesVerifiedUnchanged'] = True
            marker.write_text(json.dumps(evidence, indent=2) + '\n', encoding='utf-8')
            output.write_text(json.dumps(evidence, indent=2) + '\n', encoding='utf-8')
            print(evidence)
        finally:
            lock.seek(0)
            msvcrt.locking(lock.fileno(), msvcrt.LK_UNLCK, 1)


if __name__ == '__main__':
    main()
