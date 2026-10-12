"""Extract pinned dependency data for inspection only; never vendor noise/terrain configs."""
from pathlib import Path
import zipfile
root = Path(__file__).resolve().parents[1]
jar = next((root / '.gradle/loom-cache/remapped_mods/remapped/maven/modrinth').glob('*37f*/*/*.jar'))
with zipfile.ZipFile(jar) as z:
    wanted = [n for n in z.namelist() if n.endswith(('/overworld_128.json', '/beta.json'))
              or 'reduced_height' in n and ('noise_settings/' in n or 'dimension_type/' in n)]
    for n in wanted:
        if n.endswith('/'):
            continue
        out = root / 'logs/upstream1211-data' / n
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_bytes(z.read(n))
        print(out.relative_to(root))
