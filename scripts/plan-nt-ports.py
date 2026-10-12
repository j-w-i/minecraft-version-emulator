"""Builds the porting worklist from the Nostalgic Tweaks catalogue.

The project owner's direction is that the 1.21.4 branch is the main source and that VOLUME is the point -
there are hundreds of tweaks and only a handful are implemented. Hand-picking three at a time is not a plan,
so this turns the catalogue into an ordered worklist: every tweak, its category, its environment, the
upstream files that implement it, and whether this project has already touched it.

Usage:
    python scripts/plan-nt-ports.py [--category gameplay] [--env server] [--limit 40]
"""
import argparse
import json
import pathlib
import re
import subprocess

REPO = pathlib.Path('.')
NT = REPO / '.upstream/nostalgic-tweaks'
CATALOGUE = REPO / 'research/nostalgic-tweaks-candidates.json'
OUT = REPO / 'research/nt-port-worklist.json'


def ported_names():
    """Tweak ids this project already mentions in its own sources."""
    names = set()
    for path in (REPO / 'src/main').rglob('*.java'):
        text = path.read_text(encoding='utf-8', errors='replace')
        for match in re.finditer(r'\b([A-Z][A-Z0-9_]{6,})\b', text):
            names.add(match.group(1))
    return names


def upstream_revision():
    result = subprocess.run(['git', '-C', str(NT), 'rev-parse', 'HEAD'],
                            capture_output=True, text=True)
    return result.stdout.strip() or 'unknown'


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--category', default=None)
    parser.add_argument('--env', default=None)
    parser.add_argument('--limit', type=int, default=60)
    args = parser.parse_args()

    catalogue = json.loads(CATALOGUE.read_text(encoding='utf-8'))
    tweaks = catalogue['tweaks']
    already = ported_names()

    rows = []
    for entry in tweaks:
        name = entry.get('name')
        if not name:
            continue
        if args.category and entry.get('category') != args.category:
            continue
        if args.env and entry.get('env') != args.env:
            continue

        files = entry.get('implementationFiles') or []
        refs = entry.get('implementationRefs') or []
        # The mixin that carries the behaviour is the useful thing to read; config files are not.
        carriers = [f for f in files if '/mixin/' in f or 'Helper' in f]
        rows.append({
            'name': name,
            'category': entry.get('category'),
            'env': entry.get('env'),
            'group': entry.get('group'),
            'defaultValue': entry.get('defaultValue'),
            'historicalClaim': entry.get('historicalClaim'),
            'boundary': entry.get('historicalVersionBoundary'),
            'verification': entry.get('verificationStatus'),
            'portObstacles': entry.get('portObstacles'),
            'carrierFiles': carriers[:4],
            'referenceCount': len(refs),
            'alreadyTouched': name in already,
        })

    rows.sort(key=lambda r: (not r['carrierFiles'], r['name']))
    planned = [r for r in rows if not r['alreadyTouched']]
    document = {
        'schemaVersion': 1,
        'kind': 'nostalgic-tweaks-port-worklist',
        'upstreamRevision': upstream_revision(),
        'upstreamBranch': '1.21.4 (project-owner direction)',
        'licence': 'LGPL-3.0; see THIRD_PARTY_NOTICES.md - owner accepted the obligation on 2026-10-13',
        'counts': {
            'matching': len(rows),
            'alreadyTouched': len(rows) - len(planned),
            'toPort': len(planned),
        },
        'worklist': planned[:args.limit],
    }
    OUT.write_text(json.dumps(document, indent=2) + '\n', encoding='utf-8')

    print('upstream revision : %s' % document['upstreamRevision'])
    print('matching tweaks   : %d' % document['counts']['matching'])
    print('already touched   : %d' % document['counts']['alreadyTouched'])
    print('to port           : %d' % document['counts']['toPort'])
    print('wrote %s (showing %d)' % (OUT, len(document['worklist'])))
    print()
    for row in document['worklist'][:25]:
        print('  %-34s %-9s %-8s carriers=%d' % (row['name'][:34], row['category'], row['env'],
                                                  len(row['carrierFiles'])))


if __name__ == '__main__':
    main()
