"""Print the per-version content deltas for the Alpha targets, from the extracted research.

Written because the plan for registering Alpha assumed a shared ruleset, which is wrong: mobs and blocks were added
as the Alpha line progressed. This prints what the research actually records per build so the sets can be built
cumulatively from evidence rather than guessed.
"""
import json
import pathlib

transitions = json.loads(
    pathlib.Path('research/version-transitions.json').read_text(encoding='utf-8'))

versions = transitions['versions'] if isinstance(transitions, dict) else transitions

for version in versions:
    if not str(version.get('era', '')).startswith('alpha') and 'alpha' not in version.get('id', ''):
        continue

    print('=' * 96)
    print('%s  (%s)  order=%s' % (version.get('displayName'), version.get('id'), version.get('order')))
    summary = (version.get('summary') or '').strip()
    if summary:
        print('  summary: %s' % summary[:300])

    for field in ('introduced', 'changed', 'removed', 'incomplete'):
        values = version.get(field) or []
        if values:
            print('  %s (%d):' % (field.upper(), len(values)))
            for value in values[:14]:
                print('    - %s' % str(value)[:170])
