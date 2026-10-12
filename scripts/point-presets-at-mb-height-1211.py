"""Point our settings presets at Moderner Beta's own 128-height noise settings.

This only replaces the schema-incompatible 26.3 noise-settings reference.
IMPORTANT: in 1.21.1 the ordinary moderner_beta:overworld_128 has min_y=-64 and height=192.
This substitution ALONE does NOT preserve the reduced envelope or bedrock. Follow it with
scripts/adapt-1211-height-presets.py, which reads the pinned upstream reduced envelope and applies
only its four supported noise_settings component fields, without copying terrain/router configuration.

Run once; idempotent.
"""
import pathlib

preset_dir = pathlib.Path('src/main/resources/data/historical_experience/moderner_beta/'
                          'settings_preset')
ours = 'historical_experience:beta_1_7_3_overworld_128'
theirs = 'moderner_beta:overworld_128'

changed = []
for path in sorted(preset_dir.glob('*.json')):
    text = path.read_text(encoding='utf-8')
    if ours in text:
        path.write_text(text.replace(ours, theirs), encoding='utf-8')
        changed.append(path.name)

print('rewrote %d presets to use Moderner Beta\'s own 128-height settings' % len(changed))
for name in changed:
    print('  ', name)
