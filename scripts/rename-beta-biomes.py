"""Move Moderner Beta biome names along the timeline in the project's settings presets.

The owner's rule: from Beta 1.8.1 to the latest Release the biome should read as `late_beta_plains`, and
"if there are any other beta/late beta it should do that" - so the surrounding names move with it.

Moderner Beta versions the same idea under three prefixes. The rename therefore maps every `beta_*` and
`early_release_*` name to its `late_beta_*` counterpart, and `beta_plains` to `late_beta_plains`. Names that
have no `late_beta_*` counterpart (oceans, savanna, rainforest and the like) are left alone, because inventing
a target for them would be worse than leaving them.

Idempotent: names already in `late_beta_*` form are untouched, so re-running changes nothing.

Usage:
    python scripts/rename-beta-biomes.py --check
    python scripts/rename-beta-biomes.py --apply
"""
from __future__ import annotations

import argparse
import pathlib
import re

PRESETS = pathlib.Path('src/main/resources/data/historical_experience/moderner_beta/settings_preset')

# The versions the rename covers, and the presets their worlds actually generate from. `_no_modern_content` is the
# newest revision for each of these, which is what a new world selects and what an upgrade stages; the base preset is
# listed where the version has no such revision.
TARGETS = [
    'beta_1_8_1', 'beta_1_8_1_cave_fix', 'beta_1_8_1_no_modern_content',
    'beta_1_9_pre_1', 'beta_1_9_pre_1_no_modern_content',
    'beta_1_9_pre_2', 'beta_1_9_pre_2_no_modern_content',
    'beta_1_9_pre_3', 'beta_1_9_pre_3_no_modern_content',
    'beta_1_9_pre_4', 'beta_1_9_pre_4_no_modern_content',
    'beta_1_9_pre_5', 'beta_1_9_pre_5_no_modern_content',
    'beta_1_9_pre_6', 'beta_1_9_pre_6_no_modern_content',
    'release_1_0_0', 'release_1_0_0_no_modern_content',
    'release_1_1', 'release_1_1_no_modern_content',
    'release_1_2_5', 'release_1_2_5_no_modern_content',
    'release_1_3', 'release_1_3_no_modern_content',
    'release_1_4', 'release_1_4_no_modern_content',
    'release_1_5', 'release_1_5_no_modern_content',
    'release_1_6_4', 'release_1_6_4_no_modern_content',
]

# Every `late_beta_*` biome Moderner Beta ships. A `beta_*` name with no counterpart here is deliberately untouched.
LATE_BETA = {
    'late_beta_extreme_hills', 'late_beta_ice_plains', 'late_beta_plains', 'late_beta_swampland', 'late_beta_taiga',
}

# The one name that is not a plain prefix swap: Beta plains becomes LATE Beta plains.
SPECIAL = {
    'moderner_beta:beta_plains': 'moderner_beta:late_beta_plains',
}


def rename(text: str) -> tuple[str, dict[str, int]]:
    """Apply the rename, returning the new text and a count of what changed."""
    counts: dict[str, int] = {}
    for old, new in SPECIAL.items():
        found = text.count(old)
        if found:
            text = text.replace(old, new)
            counts[old] = found

    def swap(match: re.Match[str]) -> str:
        prefix, name = match.group(1), match.group(2)
        if name in LATE_BETA or ('late_' in name):
            return match.group(0)
        if name.startswith('beta_') or name.startswith('early_release_'):
            target = 'moderner_beta:late_beta_' + name.split('_', 1)[1] if name.startswith('beta_') \
                else 'moderner_beta:late_beta_' + name[len('early_release_'):]
            if target.split(':')[1] in LATE_BETA:
                counts[match.group(0)] = counts.get(match.group(0), 0) + 1
                return prefix + target.split(':')[1]
        return match.group(0)

    text = re.sub(r'(moderner_beta:)([a-z_0-9]+)', swap, text)
    return text, counts


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument('--check', action='store_true')
    group.add_argument('--apply', action='store_true')
    args = parser.parse_args()

    total_files = 0
    for name in TARGETS:
        path = PRESETS / (name + '.json')
        if not path.is_file():
            print('  missing preset: %s' % path.name)
            continue
        original = path.read_text(encoding='utf-8')
        updated, counts = rename(original)
        if not counts:
            continue
        total_files += 1
        summary = ', '.join('%s x%d' % (key, value) for key, value in sorted(counts.items()))
        print('%-46s %s' % (path.name, summary))
        if args.apply:
            path.write_text(updated, encoding='utf-8')

    verb = 'would change' if args.check else 'changed'
    print('%s %d preset file(s)' % (verb, total_files))
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
