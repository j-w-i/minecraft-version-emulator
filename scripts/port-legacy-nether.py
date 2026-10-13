"""Port the Legacy-Nether biome into this project's namespace, with the one change it needs.

Upstream: https://codeberg.org/Nostalgica-Reverie/Legacy-Nether, pinned in .upstream/legacy-nether,
licensed CC BY-SA 4.0. The port carries attribution in THIRD_PARTY_NOTICES.md and keeps upstream's
field values unchanged except where noted below, as the licence's share-alike terms require.

WHY THIS IS A SCRIPT RATHER THAN A COPIED FILE

Two things have to be true at once, and hand-editing JSON tends to break the second:

  1. The biome must live in historical_experience, NOT in minecraft. Upstream overrides the
     minecraft namespace, which would change EVERY world including ordinary ones - and this
     project requires ordinary worlds to be unaffected. Upstream's emptied structure tags for
     bastion remnants and ruined portals are rejected for the same reason: emptying a
     minecraft: tag is a global change. This project suppresses those structures per version in
     HistoricalStructureRules instead, which is the version-aware mechanism and already covers
     bastion remnants and ruined portals for every declared build.

  2. Upstream's feature list contains ore_quartz_nether. Nether quartz is RELEASE 1.5 content, so
     a single biome shared by every historical version would put quartz ore in Beta worlds. Two
     variants are emitted instead - one without quartz and one with - and the world-preset
     generator picks by chronology. Relying on the block veto alone would work only as far as the
     availability table dates quartz, and this makes the boundary explicit rather than emergent.

Run from the repository root: python scripts/port-legacy-nether.py
"""

import json
import pathlib

ROOT = pathlib.Path(__file__).resolve().parents[1]
UPSTREAM = ROOT / '.upstream/legacy-nether/data/minecraft/worldgen/biome/nether_wastes.json'
TARGET_DIR = ROOT / 'src/main/resources/data/historical_experience/worldgen/biome'

# The feature upstream calls nether quartz ore, which is Release 1.5 content.
QUARTZ_ORE = 'minecraft:ore_quartz_nether'


def strip_quartz(document):
    """Remove only the quartz entry from the feature steps, leaving every other step intact."""
    removed = 0
    steps = document.get('features') or []
    for index, step in enumerate(steps):
        if step and QUARTZ_ORE in step:
            steps[index] = [entry for entry in step if entry != QUARTZ_ORE]
            removed += 1
    return removed


def main():
    if not UPSTREAM.is_file():
        raise SystemExit('upstream biome not found at %s; clone it into .upstream first' % UPSTREAM)

    TARGET_DIR.mkdir(parents=True, exist_ok=True)
    upstream = json.loads(UPSTREAM.read_text(encoding='utf-8'))
    quartz = json.loads(UPSTREAM.read_text(encoding='utf-8'))

    removed = strip_quartz(upstream)
    if removed != 1:
        raise SystemExit('expected exactly one feature step containing %s, removed from %d'
                         % (QUARTZ_ORE, removed))

    (TARGET_DIR / 'legacy_nether_wastes.json').write_text(
        json.dumps(upstream, indent=2) + '\n', encoding='utf-8')
    (TARGET_DIR / 'legacy_nether_wastes_quartz.json').write_text(
        json.dumps(quartz, indent=2) + '\n', encoding='utf-8')

    print('ported the Legacy-Nether biome into historical_experience:')
    print('  legacy_nether_wastes        (no nether quartz; before Release 1.5)')
    print('  legacy_nether_wastes_quartz (with nether quartz; Release 1.5 onward)')
    print('monster spawners kept from upstream: %s'
          % ', '.join(entry['type'].split(':')[1] for entry in quartz['spawners']['monster']))


if __name__ == '__main__':
    main()
