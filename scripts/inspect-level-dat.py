"""Inspect a world's level.dat and report where its generation preset marker lives.

Read-only. Used to find the exact NBT path to change when preparing a test world, so the change is a
targeted edit of one string rather than a guess.

Usage: python scripts/inspect-level-dat.py "<world-dir>" [needle ...]
"""
from __future__ import annotations

import importlib.util
import pathlib
import sys


def load_census():
    spec = importlib.util.spec_from_file_location(
        'census', pathlib.Path(__file__).with_name('region-biome-census.py'))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def find(node, needle, trail=''):
    if isinstance(node, dict):
        for key, value in node.items():
            find(value, needle, trail + '/' + str(key))
    elif isinstance(node, list):
        for index, value in enumerate(node):
            find(value, needle, trail + '[%d]' % index)
    elif isinstance(node, str) and needle in node:
        print('  %-70s = %r' % (trail, node))


def read_level_dat(path: pathlib.Path):
    """level.dat is gzipped NBT; a few tools also write it as raw NBT, so both are accepted."""
    raw = path.read_bytes()
    if raw[:2] == b'\x1f\x8b':
        import gzip
        raw = gzip.decompress(raw)
    return load_census().read_nbt(raw)


def main() -> int:
    world = pathlib.Path(sys.argv[1])
    needles = sys.argv[2:] or ['alpha_1_0_1', 'no_modern_content', 'historical_experience', 'moderner_beta']
    data = read_level_dat(world / 'level.dat')
    if len(sys.argv) <= 2:
        print('top-level Data keys:', sorted(data.get('Data', {}).keys()))
    for needle in needles:
        print('--- %s' % needle)
        find(data, needle)
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
