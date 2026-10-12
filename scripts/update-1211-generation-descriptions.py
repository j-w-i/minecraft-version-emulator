"""Do not advertise unported gameplay in the generation-only 1.21.1 menu."""
import json
from pathlib import Path
path = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/historical_experience/lang/en_us.json'
data = json.loads(path.read_text(encoding='utf-8'))
versions = ['beta_1_7_3', 'beta_1_8_1', 'beta_1_9_pre_3', 'release_1_0_0', 'release_1_1',
            'release_1_2_5', 'release_1_6_4', 'alpha', 'infdev_420', 'infdev_611']
for version in versions:
    if version == 'beta_1_7_3':
        description = ("Moderner Beta's Beta 1.7.3 terrain, generated at Y 0..127, with historical ore "
                       "population and generation-time exclusions for modern blocks, plants and cave biomes. "
                       "Structures are automatically suppressed; dungeons remain. Generation-only backport: "
                       "historical combat, food, HUD and other gameplay are not enabled.")
    elif version in ('alpha', 'infdev_420', 'infdev_611'):
        description = ("Moderner Beta's corresponding historical terrain at Y 0..127, without deepslate, "
                       "modern cave biomes or grass patches. The implemented content, structure and spawn "
                       "restrictions are applied before generation. Generation-only backport; other gameplay "
                       "and exact ore/decoration fidelity for this era are not claimed.")
    else:
        description = ("Moderner Beta's corresponding historical terrain at Y 0..127, with the 26.3 "
                       "project's implemented content restrictions and modern cave-biome removal. "
                       "Generation-only backport; gameplay and fully historical structure/spawn boundaries "
                       "for this era are not claimed.")
    for key in ['historical_experience.world_preset.' + version + '.description',
                'generator.historical_experience.' + version + '.info',
                'createWorld.customize.modern_beta.preset.desc.historical_experience.' + version]:
        if key in data:
            data[key] = description
path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
print('Updated generation-only descriptions for', len(versions), 'public entries.')
