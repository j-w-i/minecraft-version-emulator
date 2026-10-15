"""Undo the single-biome settings that were wrongly applied to generation from Beta 1.8.1 onward.

The owner's correction: "in the world upgraded to 1.8.1, NO OTHER BIOME GENERATES THIS IS VERY IMPORTANT ONLY
THE CHUNKS GENERATED PRIOR SHOULD BE AFFECTED".

What went wrong. To make the stored biome read as `late_beta_plains` this project set
`"moderner_beta:provider": "moderner_beta:single"` and `"moderner_beta:single_biome": "moderner_beta:late_beta_plains"`
in those versions' `biomeSettings`. That does make the biome read correctly - but it does it by replacing the whole
biome PROVIDER, so every chunk generated from then on is one biome. The settings preset governs generation; it was
never the right lever for a change that is only supposed to affect chunks that already exist.

What is right. Generation keeps the multi-biome fractal providers those versions ship with, and the stored chunks of
already-generated chunks are rewritten by the chunk converter. New chunks then generate with the full set of biomes,
and only previously generated chunks change - which is exactly what the owner asked for.

This removes ONLY the two keys the change added to `biomeSettings`. The two keys in `caveBiomeSettings` were already
there before that change and are left alone, so the revert returns the files to their previous state exactly.

Usage:
    python scripts/revert-single-biome-generation.py --check
    python scripts/revert-single-biome-generation.py --apply
"""
from __future__ import annotations

import argparse
import json
import pathlib
from collections import OrderedDict

PRESETS = pathlib.Path('src/main/resources/data/historical_experience/moderner_beta/settings_preset')

# The presets the wrong change touched. Alpha v1.2.0's own preset is deliberately NOT here: its provider was already
# `moderner_beta:single` before any of this work, so that build genuinely is a single-biome world.
TARGETS = [
    'beta_1_8_1_cave_fix', 'beta_1_8_1_no_modern_content',
    'beta_1_9_pre_1_no_modern_content', 'beta_1_9_pre_2_no_modern_content', 'beta_1_9_pre_3_no_modern_content',
    'beta_1_9_pre_4_no_modern_content', 'beta_1_9_pre_5_no_modern_content', 'beta_1_9_pre_6_no_modern_content',
    'release_1_0_0_no_modern_content', 'release_1_1_no_modern_content', 'release_1_2_5_no_modern_content',
    'release_1_3_no_modern_content', 'release_1_4_no_modern_content', 'release_1_5_no_modern_content',
    'release_1_6_4_no_modern_content', 'alpha_1_2_0_beta_plains',
]


# Alpha v1.2.0 is the one exception. Its provider was ALREADY `moderner_beta:single` before any of this work - that
# build genuinely is a single-biome world - so the provider key is redundant there and no generation is flattened by
# keeping the biome name the owner asked for.
KEEP_SINGLE_BIOME = {'alpha_1_2_0_beta_plains'}


def revert(data: dict, keep_single_biome: bool) -> list[str]:
    """Drop the single-biome keys the wrong change added. Returns what was removed."""
    removed: list[str] = []
    biome = data.get('biomeSettings')
    if not isinstance(biome, dict):
        return removed
    keys = ['moderner_beta:provider'] if keep_single_biome else \
        ['moderner_beta:provider', 'moderner_beta:single_biome']
    for key in keys:
        if key in biome:
            del biome[key]
            removed.append(key)
    return removed


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
        data = json.loads(path.read_text(encoding='utf-8'), object_pairs_hook=OrderedDict)
        removed = revert(data, name in KEEP_SINGLE_BIOME)
        if not removed:
            continue
        changed += 1
        print('%-46s removes %s from biomeSettings' % (path.name, ', '.join(removed)))
        if args.apply:
            path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')

    verb = 'would change' if args.check else 'changed'
    print('%s %d preset file(s)' % (verb, changed))
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
