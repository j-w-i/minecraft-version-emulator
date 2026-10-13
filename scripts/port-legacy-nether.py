"""Port the Legacy-Nether biome into this project's namespace, in the 1.21.1 shape.

Upstream: https://codeberg.org/Nostalgica-Reverie/Legacy-Nether, pinned in .upstream/legacy-nether,
licensed CC BY-SA 4.0. Attribution lives in THIRD_PARTY_NOTICES.md.

WHY THIS IS AN OVERLAY AND NOT A COPY

Upstream's pack.mcmeta declares pack_format 48 - which is 1.21.1 - while its biome JSON is written in an OLDER
shape, so Minecraft runs no datafixers and rejects it:

    Failed to parse historical_experience:worldgen/biome/legacy_nether_wastes.json from pack hex1211
      Not a JSON object: "minecraft:nether_cave"
    "Errors in currently selected data packs prevented the world from loading"

The first two attempts at fixing it each fixed one field and hoped. Both were wrong: fixing carvers and
effects.music was not enough, and a world still refused to load. Guessing at a schema one field at a time is the
mistake, not the individual fields.

So this takes the shape from vanilla 1.21.1's own nether_wastes - a file already proven to parse - and overlays
upstream's values:

  * the KEY SET is vanilla's, exactly. Any key upstream has that vanilla does not is DROPPED, because an unexpected
    field is what a strict codec rejects. music_volume was one such field.
  * the VALUES are upstream's, for every key both files have.
  * the two shape differences are still normalised, because upstream's values for them are in the old shape:
      carvers        "minecraft:nether_cave"                    -> {"air": "minecraft:nether_cave"}
      effects.music  [{"weight": 1, "data": { ...sound... }}]   -> { ...sound... }
  * upstream's feature list is kept, minus minecraft:ore_quartz_nether for the pre-Release-1.5 variant, because
    nether quartz is Release 1.5 content.

Vanilla's file is read from the Gradle Minecraft cache at port time and is NOT checked in: this project does not
copy Mojang data into its deliverables. It is used as a schema reference, which is what it is.

Run from the repository root: python scripts/port-legacy-nether.py
"""

import glob
import json
import os
import pathlib
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[1]
UPSTREAM = ROOT / '.upstream/legacy-nether/data/minecraft/worldgen/biome/nether_wastes.json'
TARGET_DIR = ROOT / 'src/main/resources/data/historical_experience/worldgen/biome'
VANILLA_ENTRY = 'data/minecraft/worldgen/biome/nether_wastes.json'

QUARTZ_ORE = 'minecraft:ore_quartz_nether'


def find_vanilla_biome():
    """Locate and read vanilla's nether_wastes from the Gradle Minecraft cache."""
    override = os.environ.get('HEX_MINECRAFT_JAR')

    if override and pathlib.Path(override).is_file():
        candidates = [override]
    else:
        candidates = glob.glob(str(pathlib.Path.home()
            / '.gradle/loom-cache/minecraftMaven/net/minecraft/*/*loom*/*.jar'))

    if not candidates:
        raise SystemExit('vanilla minecraft jar not found in the Gradle cache; set HEX_MINECRAFT_JAR to it')

    with zipfile.ZipFile(candidates[0]) as jar:
        with jar.open(VANILLA_ENTRY) as handle:
            return json.loads(handle.read().decode('utf-8'))


def upgrade_in_place(values, reference):
    """Normalise the two fields whose VALUES are in the old shape, using vanilla as the reference."""
    carvers = values.get('carvers')

    if isinstance(carvers, str):
        values['carvers'] = {'air': carvers}
    elif not isinstance(carvers, dict):
        raise SystemExit('carvers is neither a string nor an object: %r' % (carvers,))

    effects = values.get('effects') or {}

    if isinstance(effects.get('music'), list):
        music = effects['music']

        if len(music) != 1 or 'data' not in music[0]:
            raise SystemExit('effects.music is an array this script does not understand: %r' % (music,))

        effects['music'] = music[0]['data']

    # The reference decides what the field should look like, so a mismatch is caught here rather than in a world.
    if type(values.get('carvers')) is not type(reference.get('carvers')):
        raise SystemExit('carvers still does not match vanilla in shape')


def overlay(vanilla, upstream):
    """Vanilla's shape and key set; upstream's values; nothing upstream has that vanilla does not.

    Strictly one level deep, and that is deliberate. The recursive version of this re-added sub-keys vanilla does
    not have - music_volume inside effects was one - which is exactly what a strict codec rejects. So for every
    object field the key set comes from VANILLA and only the values come from upstream.
    """
    result = json.loads(json.dumps(vanilla))
    dropped = []

    for key, value in upstream.items():
        if key not in vanilla:
            dropped.append(key)
            continue

        reference = vanilla[key]

        if isinstance(reference, dict) and isinstance(value, dict):
            merged = {}
            for sub_key in reference:
                merged[sub_key] = value[sub_key] if sub_key in value else reference[sub_key]
            dropped.extend('%s.%s' % (key, sub) for sub in value if sub not in reference)
            result[key] = merged
        else:
            result[key] = value

    return result, dropped


def strip_quartz(document):
    removed = 0
    steps = document.get('features') or []

    for index, step in enumerate(steps):
        if step and QUARTZ_ORE in step:
            steps[index] = [entry for entry in step if entry != QUARTZ_ORE]
            removed += 1

    return removed


def write(name, document, vanilla):
    """Assert the shape matches vanilla's exactly before writing, because that is what failed twice."""
    if set(document) != set(vanilla):
        raise SystemExit('%s: key set differs from vanilla: extra=%s missing=%s'
                         % (name, sorted(set(document) - set(vanilla)), sorted(set(vanilla) - set(document))))

    for key, reference in vanilla.items():
        if isinstance(reference, dict):
            if not isinstance(document.get(key), dict):
                raise SystemExit('%s: %s should be an object, as vanilla has it' % (name, key))

    effects = document.get('effects') or {}

    if isinstance(effects.get('music'), list):
        raise SystemExit('%s: effects.music is still an array' % name)

    (TARGET_DIR / ('%s.json' % name)).write_text(json.dumps(document, indent=2) + '\n', encoding='utf-8')


def main():
    if not UPSTREAM.is_file():
        raise SystemExit('upstream biome not found at %s; clone it into .upstream first' % UPSTREAM)

    TARGET_DIR.mkdir(parents=True, exist_ok=True)
    vanilla = find_vanilla_biome()
    print('shape reference: vanilla 1.21.1 nether_wastes, %d top-level fields' % len(vanilla))

    with_quartz = json.loads(UPSTREAM.read_text(encoding='utf-8'))
    without_quartz = json.loads(UPSTREAM.read_text(encoding='utf-8'))

    upgrade_in_place(with_quartz, vanilla)
    upgrade_in_place(without_quartz, vanilla)

    with_quartz, dropped = overlay(vanilla, with_quartz)
    without_quartz, _ = overlay(vanilla, without_quartz)

    if dropped:
        print('dropped because vanilla has no such field: %s' % ', '.join(dropped))

    removed = strip_quartz(without_quartz)

    if removed != 1:
        raise SystemExit('expected exactly one feature step containing %s, removed from %d'
                         % (QUARTZ_ORE, removed))

    write('legacy_nether_wastes', without_quartz, vanilla)
    write('legacy_nether_wastes_quartz', with_quartz, vanilla)

    print('ported and shape-checked against vanilla:')
    print('  legacy_nether_wastes        (no nether quartz; before Release 1.5)')
    print('  legacy_nether_wastes_quartz (with nether quartz; Release 1.5 onward)')
    print('monster spawners: %s'
          % ', '.join(entry['type'].split(':')[1] for entry in with_quartz['spawners']['monster']))


if __name__ == '__main__':
    main()
