"""Give each Beta 1.8.1-through-Release-1.6.4 preset a single biome of `late_beta_plains`.

Why this shape. The owner's rule is that the biome from Beta 1.8.1 to the latest Release reads as
`late_beta_plains`, and the Alpha v1.2.0 case proved how to do that: set the biome in the preset, because
Moderner Beta's biome source asks its provider for every position and never reads a chunk's stored biome.

Those versions' `biomeSettings` simply inherit `moderner_beta:beta_1_8_1` / `moderner_beta:release_*`, whose
providers are FRACTAL - multi-biome. A single-biome setting alone would be ignored by a fractal provider, so
the provider is set to `moderner_beta:single` as well. That is what makes the world report one biome rather
than a distribution, which is the point of the rule.

The chunk settings and the terrain foundation are never touched, so terrain shape is unchanged.
The cave biome is moved to `moderner_beta:late_beta_taiga` where it was `moderner_beta:beta_taiga`, because the
`beta_*` family is the pre-1.8 naming and none of those names should survive into this range.

Idempotent: a preset already in this shape is reported as unchanged.

Usage:
    python scripts/set-late-beta-biome.py --check
    python scripts/set-late-beta-biome.py --apply
"""
from __future__ import annotations

import argparse
import json
import pathlib
from collections import OrderedDict

PRESETS = pathlib.Path('src/main/resources/data/historical_experience/moderner_beta/settings_preset')

SINGLE_PROVIDER = 'moderner_beta:single'
TARGET_BIOME = 'moderner_beta:late_beta_plains'
TARGET_CAVE_BIOME = 'moderner_beta:late_beta_taiga'

# The presets a world in this range actually generates from: the newest revision of each version.
TARGETS = [
    'beta_1_8_1_cave_fix', 'beta_1_8_1_no_modern_content',
    'beta_1_9_pre_1_no_modern_content', 'beta_1_9_pre_2_no_modern_content', 'beta_1_9_pre_3_no_modern_content',
    'beta_1_9_pre_4_no_modern_content', 'beta_1_9_pre_5_no_modern_content', 'beta_1_9_pre_6_no_modern_content',
    'release_1_0_0_no_modern_content', 'release_1_1_no_modern_content', 'release_1_2_5_no_modern_content',
    'release_1_3_no_modern_content', 'release_1_4_no_modern_content', 'release_1_5_no_modern_content',
    'release_1_6_4_no_modern_content',
]


def rewrite(data: dict) -> tuple[dict, list[str]]:
    """Return the updated preset and a list of the changes made."""
    changes: list[str] = []

    biome = data.setdefault('biomeSettings', OrderedDict())
    if biome.get('moderner_beta:provider') != SINGLE_PROVIDER:
        biome['moderner_beta:provider'] = SINGLE_PROVIDER
        changes.append('biomeSettings provider -> %s' % SINGLE_PROVIDER)
    if biome.get('moderner_beta:single_biome') != TARGET_BIOME:
        biome['moderner_beta:single_biome'] = TARGET_BIOME
        changes.append('biomeSettings single_biome -> %s' % TARGET_BIOME)

    cave = data.setdefault('caveBiomeSettings', OrderedDict())
    previous = cave.get('moderner_beta:single_biome')
    if previous is not None and previous != TARGET_CAVE_BIOME:
        cave['moderner_beta:single_biome'] = TARGET_CAVE_BIOME
        changes.append('caveBiomeSettings single_biome %s -> %s' % (previous, TARGET_CAVE_BIOME))
    cave.setdefault('moderner_beta:provider', SINGLE_PROVIDER)
    return data, changes


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument('--check', action='store_true')
    group.add_argument('--apply', action='store_true')
    args = parser.parse_args()

    changed = 0
    for name in TARGETS:
        path = PRESETS / (name + '.json')
        if not path.is_file():
            print('  missing preset: %s' % path.name)
            continue
        original = path.read_text(encoding='utf-8')
        data = json.loads(original, object_pairs_hook=OrderedDict)
        updated, changes = rewrite(data)
        if not changes:
            continue
        changed += 1
        print('%-46s %s' % (path.name, '; '.join(changes)))
        if args.apply:
            path.write_text(json.dumps(updated, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')

    verb = 'would change' if args.check else 'changed'
    print('%s %d preset file(s)' % (verb, changed))
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
