"""Generate the 1.21.1 world presets that select our historical profiles.

The 26.3 project carried these as data in 26.3's schema, which 1.21.1 cannot parse. The shape below is
taken from Moderner Beta's own `world_preset/moderner_beta.json` in the 1.21.1 build - the same generator
and biome source ids, the same `moderner_beta:preset` references - with the preset pointed at ours.

One world preset is written per settings preset, so `level-type=historical_experience:<name>` creates a
world with that profile. A tag adds them all to `minecraft:normal`, which is how Moderner Beta's own preset
reaches the world-creation list.

Run from the repository root. Idempotent.
"""
import json
import pathlib

settings_dir = pathlib.Path('src/main/resources/data/historical_experience/moderner_beta/'
                            'settings_preset')
out_dir = pathlib.Path('src/main/resources/data/historical_experience/worldgen/world_preset')
tag_dir = pathlib.Path('src/main/resources/data/minecraft/tags/worldgen/world_preset')

NAMESPACE = 'historical_experience'


# Public entries select the same latest revisions as 26.3; never expose every legacy recipe in the menu.
# The `_no_modern_content` revision is the newest for each post-Beta-1.7.3 version: it adds the modern-content
# feature exclusions those versions were missing (the 1.13 aquatic plants measured in a real Beta 1.8.1 world,
# the 1.7.2/1.14 flowers, sea pickles, water lilies, the 1.21.5 firefly bushes and - before Release 1.2.1 -
# the desert well). A world created before it keeps the alias it recorded, so nothing already generated moves.
PUBLIC_RECIPES = {
    'beta_1_7_3': 'beta_1_7_3_content_preview_10',
    'beta_1_0': 'beta_1_0_no_modern_content',
    'beta_1_2': 'beta_1_2_no_modern_content',
    'alpha_1_0_1': 'alpha_1_0_1_no_modern_content',
    'alpha_1_0_4': 'alpha_1_0_4_no_modern_content',
    'alpha_1_0_6': 'alpha_1_0_6_no_modern_content',
    'alpha_1_0_11': 'alpha_1_0_11_no_modern_content',
    'alpha_1_0_14': 'alpha_1_0_14_no_modern_content',
    'alpha_1_0_17': 'alpha_1_0_17_no_modern_content',
    'alpha_1_1_0': 'alpha_1_1_0_no_modern_content',
    'alpha_1_2_0': 'alpha_1_2_0_no_modern_content',

    'alpha': 'alpha_no_modern_content',
    'infdev_420': 'infdev_420_no_modern_content',
    'infdev_611': 'infdev_611_no_modern_content',
    **{name: name + '_no_modern_content' for name in [
        'beta_1_8_1', 'beta_1_9_pre_1', 'beta_1_9_pre_2', 'beta_1_9_pre_3', 'beta_1_9_pre_4',
        'beta_1_9_pre_5', 'beta_1_9_pre_6', 'release_1_0_0', 'release_1_1',
        'release_1_2_5', 'release_1_6_4',
        'release_1_3', 'release_1_4', 'release_1_5']},
}


def world_preset(preset_id, reduced=True, quartz=False):
    def settings():
        return {'moderner_beta:preset': preset_id}

    # The Nether biome is PORTED from the Legacy-Nether upstream the owner supplied, re-namespaced into this
    # project so that no ordinary world is affected. Two variants exist because that upstream's feature list
    # contains ore_quartz_nether, and nether quartz is Release 1.5 content: the plain variant is used before it and
    # the quartz variant from Release 1.5 onward, which is why this takes a flag rather than hard-coding a name.
    nether_biome = ('historical_experience:legacy_nether_wastes_quartz' if quartz
                    else 'historical_experience:legacy_nether_wastes')

    return {
        'dimensions': {
            'minecraft:overworld': {
                'type': 'historical_experience:beta_1_7_3_overworld' if reduced else 'minecraft:overworld',
                'generator': {
                    'type': 'moderner_beta:moderner_beta',
                    'biome_source': {
                        'type': 'moderner_beta:moderner_beta',
                        'cave_provider_settings': settings(),
                        'provider_settings': settings(),
                    },
                    'provider_settings': settings(),
                },
            },
            'minecraft:the_end': {
                'type': 'minecraft:the_end',
                'generator': {
                    'type': 'minecraft:noise',
                    'biome_source': {'type': 'minecraft:the_end'},
                    'settings': 'minecraft:end',
                },
            },
            'minecraft:the_nether': {
                'type': 'minecraft:the_nether',
                'generator': {
                    'type': 'minecraft:noise',
                    # ONE biome, because every target here predates the 1.16 Nether Update that split the Nether
                    # into crimson forest, warped forest, soul sand valley, basalt deltas and nether wastes. The
                    # newest version this project declares is Release 1.6.4, so none of them had those biomes.
                    #
                    # This composes VANILLA data rather than reconstructing terrain: 'minecraft:fixed' is a vanilla
                    # biome source and 'minecraft:nether_wastes' is the name the single old Nether region later
                    # received. Moderner Beta ships no historical Nether at all - its only nether data file is
                    # surface_config/nether.json, with no nether noise settings, biome source or world preset - so
                    # the alternative would be authoring density and noise parameters, which this project forbids.
                    #
                    # Recorded limit: the TERRAIN SHAPE is still the modern 1.16+ nether, because that comes from
                    # the 'minecraft:nether' noise settings and there is no historical equivalent to point at.
                    # Biomes are what a player sees first, and those are now correct.
                    'biome_source': {
                        'type': 'minecraft:fixed',
                        'biome': nether_biome,
                    },
                    'settings': 'minecraft:nether',
                },
            },
        },
    }


def main():
    presets = sorted(p.stem for p in settings_dir.glob('*.json'))
    if not presets:
        raise SystemExit('no settings presets found under %s' % settings_dir)

    out_dir.mkdir(parents=True, exist_ok=True)
    tag_dir.mkdir(parents=True, exist_ok=True)

    written = []
    for name in presets:
        path = out_dir / ('%s.json' % name)
        recipe = PUBLIC_RECIPES.get(name, name)
        reduced = name != 'beta_1_7_3_content_preview_1'
        # Quartz ore is Release 1.5 content, so only the presets from that build onward get the variant of the
        # ported Nether biome whose feature list contains it. Everything else names the plain variant.
        quartz = name.startswith('release_1_5') or name.startswith('release_1_6')
        path.write_text(json.dumps(world_preset('%s:%s' % (NAMESPACE, recipe), reduced, quartz), indent=2) + '\n',
                        encoding='utf-8')
        written.append(name)

    # The tag is what makes them selectable; a world preset outside it is still resolvable by id.
    tag = {'replace': False, 'values': ['%s:%s' % (NAMESPACE, name) for name in PUBLIC_RECIPES]}
    (tag_dir / 'normal.json').write_text(json.dumps(tag, indent=2) + '\n', encoding='utf-8')

    print('wrote %d world presets and a tag over them' % len(written))
    print('  first three:', ', '.join(written[:3]))
    print('  select one with level-type=%s:<name>' % NAMESPACE)


if __name__ == '__main__':
    main()
