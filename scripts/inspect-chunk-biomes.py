"""Show how one chunk stores its biomes, so a converter knows exactly what it has to rewrite.

Read-only. Prints the biome container's shape: the palette, the packed data length, and the bits per
entry, which is what decides whether a rewrite can be done in place or needs re-encoding.

Usage: python scripts/inspect-chunk-biomes.py <world-dir> [--chunk X,Z]
"""
from __future__ import annotations

import argparse
import importlib.util
import pathlib


def load_census():
    spec = importlib.util.spec_from_file_location(
        'census', pathlib.Path(__file__).with_name('region-biome-census.py'))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('world', type=pathlib.Path)
    parser.add_argument('--chunk', default=None, help='X,Z to inspect a specific chunk')
    args = parser.parse_args()

    census = load_census()
    region_dir = args.world / 'region'
    wanted = None
    if args.chunk:
        cx, cz = (int(part) for part in args.chunk.split(','))
        wanted = (cx, cz)

    shown = 0
    for region in sorted(region_dir.glob('r.*.mca')):
        for cx, cz, nbt in census.iter_chunks(region):
            if wanted is not None and (cx, cz) != wanted:
                continue
            print('chunk %d,%d  sections=%d  status=%s' % (cx, cz, len(nbt.get('sections', [])),
                                                           nbt.get('Status', '?')))
            for index, section in enumerate(nbt.get('sections', [])):
                biomes = section.get('biomes')
                if biomes is None:
                    continue
                palette = biomes.get('palette')
                data = biomes.get('data')
                print('  section %d y=%s' % (index, section.get('Y')))
                print('    biome container keys : %s' % sorted(biomes.keys()))
                print('    palette              : %s' % palette)
                if data is None:
                    print('    data                 : absent (palette-only container)')
                else:
                    bits = max(1, (len(palette) - 1).bit_length()) if palette else 0
                    per_long = 64 // bits if bits else 0
                    expected = -(-4096 // per_long) if per_long else 0
                    print('    data longs           : %d (bits=%d, %d per long, %d expected for 4096 cells)'
                          % (len(data), bits, per_long, expected))
                # The block container beside it, for comparison.
                block_states = section.get('block_states')
                if isinstance(block_states, dict):
                    print('    block palette size   : %d' % len(block_states.get('palette', [])))
            shown += 1
            if wanted is not None or shown >= 1:
                return 0
    if shown == 0:
        print('no chunks found')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
