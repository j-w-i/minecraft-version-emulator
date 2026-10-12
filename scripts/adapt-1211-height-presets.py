"""Restore the published reduced envelope using MB's supported settings component.

No terrain algorithms, density functions, routers, climate mappings or global registry overrides are copied.
The four envelope fields are read from the pinned 1.21.1 dependency's own reduced-height pack.
"""
import json
from pathlib import Path
import zipfile
root = Path(__file__).resolve().parents[1]
jar = next((root / '.gradle/loom-cache/remapped_mods/remapped/maven/modrinth').glob('*37f*/*/*.jar'))
with zipfile.ZipFile(jar) as z:
    envelope = json.loads(z.read('resourcepacks/reduced_height/data/moderner_beta/worldgen/'
                                'noise_settings/overworld_128.json'))['noise']
assert envelope == {'min_y': 0, 'height': 128, 'size_horizontal': 1, 'size_vertical': 2}
settings = root / 'src/main/resources/data/historical_experience/moderner_beta/settings_preset'
updated = 0
for path in sorted(settings.glob('*.json')):
    data = json.loads(path.read_text(encoding='utf-8'))
    chunk = data['chunkSettings']
    if chunk.get('moderner_beta:noise_generator_settings') != 'moderner_beta:overworld_128':
        continue
    chunk['moderner_beta:noise_settings'] = envelope
    path.write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8')
    updated += 1
print('Restored the upstream reduced envelope through the supported component in', updated, 'recipes.')
