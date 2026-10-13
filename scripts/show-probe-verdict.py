"""Summarise a generation-probe verdict for one profile, checking the claims that matter."""
import json
import pathlib
import sys

path = pathlib.Path(sys.argv[1] if len(sys.argv) > 1
                    else 'logs/sol-1211-sol-alpha101-fresh.json')
d = json.loads(path.read_text(encoding='utf-8'))

print('success                :', d.get('success'))
print('judgedVersionId        :', d.get('judgedVersionId'))
print('alias                  :', d.get('alias'))
print('loader blocks sampled  :', len(d.get('sampledFullBlockCounts') or {}))
print('palette failures       :', d.get('sampledPaletteFailures'))
print('unknown blocks         :', d.get('sampledUnknownBlocks'))
print('dated blocks available :', d.get('blockAvailabilityDatedBlocks'))

spawns = d.get('spawns') or {}
if isinstance(spawns, dict):
    for key in list(spawns)[:6]:
        print('spawn rule %-14s:' % key, str(spawns[key])[:110])

gameplay = d.get('gameplay') or {}
if isinstance(gameplay, dict):
    for key in ('versionId', 'drops', 'orbs', 'baby', 'geared'):
        if key in gameplay:
            print('gameplay %-13s:' % key, str(gameplay[key])[:110])

print('ore drops              :', json.dumps(d.get('oreDrops'))[:160])
print('mixin targets          :', {k.split('.')[-1]: v for k, v in (d.get('mixinTargets') or {}).items()
                                 if not isinstance(v, list)})
print('allowed structures     :', str(d.get('allowedStructuresForThisProfile'))[:200])
