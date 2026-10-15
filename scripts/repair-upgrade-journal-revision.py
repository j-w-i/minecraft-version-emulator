"""Repair a world whose upgrade journal and metadata revision disagree.

What went wrong. A biome conversion used to be recorded by appending an entry to
`historical_experience_metadata.json`. So did the version-upgrade transaction - but the upgrade is a
two-step transaction with a journal, and it records the exact metadata revision it expects. When a
conversion landed between the journal being written and the generator advance being applied, the metadata
came out one revision NEWER than the journal described, and `UpgradeRecovery` refuses that on purpose:

    recovery found revision 4 in the world but the journal only describes revision 3;
    refusing to overwrite newer metadata

The refusal is correct - overwriting a newer document with an older one would lose whatever it recorded.
The fix is not to weaken it, but to tell the journal what actually happened, which is exactly one extra
revision that the conversion recorded.

The new code no longer writes to metadata at all (the record has its own file), so this can only happen to
worlds loaded by the affected build.

This script only ever raises the journal's expected revision to match the world, and only when every one of
these holds:
  * the journal exists and is at phase BACKUP_WRITTEN or METADATA_WRITTEN,
  * the world's metadata revision is greater than the journal's target revision by exactly the number of
    conversion entries the world records,
  * the world's profileId and generationPreset equal the journal's target,
  * the metadata history the journal describes is a prefix of the world's history.

If any check fails it changes nothing and says why. The backup is never touched.

Usage:
    python scripts/repair-upgrade-journal-revision.py "<world-dir>" --check
    python scripts/repair-upgrade-journal-revision.py "<world-dir>" --apply
"""
from __future__ import annotations

import argparse
import json
import pathlib
import sys

JOURNAL_NAME = 'historical_experience_upgrade.journal'
METADATA_NAME = 'historical_experience_metadata.json'
RECORD_NAME = 'historical_experience_biome_conversion.json'
REPAIRABLE_PHASES = {'BACKUP_WRITTEN', 'METADATA_WRITTEN'}


def load(path: pathlib.Path):
    try:
        return json.loads(path.read_text(encoding='utf-8'))
    except FileNotFoundError:
        return None
    except json.JSONDecodeError as failure:
        raise SystemExit('%s is not valid JSON: %s' % (path.name, failure))


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('world', type=pathlib.Path)
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument('--check', action='store_true')
    group.add_argument('--apply', action='store_true')
    args = parser.parse_args()

    world = args.world
    journal_path = world / JOURNAL_NAME
    journal = load(journal_path)
    if journal is None:
        print('no journal at %s - nothing to repair' % journal_path)
        return 0
    metadata = load(world / METADATA_NAME)
    if metadata is None:
        print('journal exists but there is no metadata document; not touching anything', file=sys.stderr)
        return 1

    target = json.loads(journal['targetMetadataJson'])
    phase = journal.get('phase')
    if phase not in REPAIRABLE_PHASES:
        print('journal phase is %s, which this repair does not handle; not touching anything' % phase,
              file=sys.stderr)
        return 1

    if metadata['revision'] <= target['revision']:
        print('nothing to do: world revision %d is not newer than the journal target %d'
              % (metadata['revision'], target['revision']))
        return 0

    # The identity must be identical, or the journal describes a different world than the one on disk.
    for key in ('profileId', 'generationPreset'):
        if metadata[key] != target[key]:
            print('refusing: world %s is %r but the journal target is %r'
                  % (key, metadata[key], target[key]), file=sys.stderr)
            return 1

    # The history the journal describes must be a prefix of what the world now holds.
    described = target['history']
    actual = metadata['history']
    if actual[:len(described)] != described:
        print('refusing: the journal\'s history is not a prefix of the world\'s history', file=sys.stderr)
        return 1

    extra = actual[len(described):]
    # Every extra entry must be a conversion record, and the counts must line up exactly. Anything else means
    # something changed the document that this repair does not understand.
    for entry in extra:
        if entry.get('actor') != 'biome conversion':
            print('refusing: revision %s was written by %r, not a conversion'
                  % (entry.get('revision'), entry.get('actor')), file=sys.stderr)
            return 1

    expected = target['revision'] + len(extra)
    if metadata['revision'] != expected:
        print('refusing: world revision is %d but %d conversions over target %d would give %d'
              % (metadata['revision'], len(extra), target['revision'], expected), file=sys.stderr)
        return 1

    print('world    : %s' % world)
    print('journal  : phase %s, target revision %d' % (phase, target['revision']))
    print('metadata : revision %d' % metadata['revision'])
    print('extra    : %d conversion entr(ies) -> %s'
          % (len(extra), ', '.join(e['note'].split(' to ')[-1] for e in extra)))
    print('action   : raise the journal\'s expected revision to %d so recovery can proceed' % metadata['revision'])

    if args.check:
        print('dry run: the journal was not written')
        return 0

    # Only the journal's expected revision changes. Its phase, backup path and target identity are untouched, so
    # recovery still applies the generator advance it owes.
    target['revision'] = metadata['revision']
    target['history'] = actual
    journal['targetMetadataJson'] = json.dumps(target, separators=(',', ':'))
    temp = journal_path.with_name(journal_path.name + '.tmp')
    temp.write_text(json.dumps(journal, separators=(',', ':')), encoding='utf-8')
    temp.replace(journal_path)
    print('wrote %s' % journal_path.name)
    print('next launch: recovery should reach AWAITING_GENERATOR_ADVANCE and apply the advance on world stop')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
