"""Report each declared version's newest revision alias and the settings preset it names.

Read-only. Exists because the biome rename has to be applied to the preset that a version's NEWEST revision
points at - that is the one a newly created world selects and the one an upgrade stages - and working that
out by eye across the registry is how mistakes get made.

Usage: python scripts/list-newest-presets.py [--from beta_1_8_1]
"""
from __future__ import annotations

import argparse
import json
import pathlib
import re

REGISTRY = pathlib.Path('src/main/java/net/historicalexperience/version/HistoricalVersions.java')
PRESETS = pathlib.Path('src/main/resources/data/historical_experience/moderner_beta/settings_preset')

# The versions the owner's rename covers, in declaration order, with the chronology each is declared under.
TARGETS = [
    'beta_1_8_1', 'beta_1_9_pre_1', 'beta_1_9_pre_2', 'beta_1_9_pre_3', 'beta_1_9_pre_4',
    'beta_1_9_pre_5', 'beta_1_9_pre_6', 'release_1_0_0', 'release_1_1', 'release_1_2_5',
    'release_1_3', 'release_1_4', 'release_1_5', 'release_1_6_4',
]


def alias_for(version: str) -> list[str]:
    """Every revision alias the registry declares for a version id, in the order it declares them."""
    text = REGISTRY.read_text(encoding='utf-8')
    # Revisions appear as `alias("<name>")` inside a version's block, and the base revision is the id itself.
    start = text.find('"historical_experience:%s"' % version)
    if start < 0:
        return []
    # Walk forward until the next version id is declared.
    rest = text[start:]
    end = rest.find('versions.add', 10)
    block = rest[:end if end > 0 else len(rest)]
    aliases = [version]
    aliases += re.findall(r'alias\("([^"]+)"\)', block)
    aliases += re.findall(r'"historical_experience:(%s[a-z0-9_]*)"' % re.escape(version), block)
    seen, out = set(), []
    for alias in aliases:
        full = alias if ':' in alias else 'historical_experience:' + alias
        if full not in seen:
            seen.add(full)
            out.append(full)
    return out


def preset_of(alias: str) -> str | None:
    """The presets a settings preset inherits from, and its own biome names."""
    path = PRESETS / (alias.split(':')[1] + '.json')
    if not path.is_file():
        return None
    data = json.loads(path.read_text(encoding='utf-8'))
    return data.get('biomeSettings', {}).get('moderner_beta:preset')


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--from', dest='start', default='beta_1_8_1')
    args = parser.parse_args()

    started = False
    for version in TARGETS:
        if version == args.start:
            started = True
        if not started:
            continue
        aliases = alias_for(version)
        newest = aliases[-1] if aliases else None
        inherited = preset_of(newest) if newest else None
        print('%-22s newest revision %-46s inherits %s' % (version, newest, inherited))
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
