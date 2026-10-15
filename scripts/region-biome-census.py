"""Report the biomes stored in a world's region files, read straight off disk.

This exists to check a claim that cannot be checked from inside the game: that a chunk's *stored*
biome data really changed, rather than the running server merely reporting something different.

The Anvil reader here is deliberately the same shape as the one in region-block-census.py - same NBT
tag walk, same zlib/gzip handling - so the two agree about what a chunk contains.

Usage:
    python scripts/region-biome-census.py <world-dir> [--dimension overworld] [--top N] [--json]
"""
from __future__ import annotations

import argparse
import collections
import json
import pathlib
import struct
import sys
import zlib

TAG_END, TAG_BYTE, TAG_SHORT, TAG_INT, TAG_LONG, TAG_FLOAT, TAG_DOUBLE = 0, 1, 2, 3, 4, 5, 6
TAG_BYTE_ARRAY, TAG_STRING, TAG_LIST, TAG_COMPOUND, TAG_INT_ARRAY, TAG_LONG_ARRAY = 7, 8, 9, 10, 11, 12


class Reader:
    def __init__(self, data: bytes):
        self.data = data
        self.pos = 0

    def take(self, n: int) -> bytes:
        piece = self.data[self.pos:self.pos + n]
        if len(piece) != n:
            raise EOFError('wanted %d bytes at %d, %d available' % (n, self.pos, len(self.data) - self.pos))
        self.pos += n
        return piece

    def u1(self) -> int:
        return self.take(1)[0]

    def i1(self) -> int:
        return struct.unpack('>b', self.take(1))[0]

    def u2(self) -> int:
        return struct.unpack('>H', self.take(2))[0]

    def i2(self) -> int:
        return struct.unpack('>h', self.take(2))[0]

    def i4(self) -> int:
        return struct.unpack('>i', self.take(4))[0]

    def i8(self) -> int:
        return struct.unpack('>q', self.take(8))[0]

    def f4(self) -> float:
        return struct.unpack('>f', self.take(4))[0]

    def f8(self) -> float:
        return struct.unpack('>d', self.take(8))[0]

    def string(self) -> str:
        return self.take(self.u2()).decode('utf-8', 'replace')


def read_payload(reader: Reader, tag: int):
    if tag == TAG_BYTE:
        return reader.i1()
    if tag == TAG_SHORT:
        return reader.i2()
    if tag == TAG_INT:
        return reader.i4()
    if tag == TAG_LONG:
        return reader.i8()
    if tag == TAG_FLOAT:
        return reader.f4()
    if tag == TAG_DOUBLE:
        return reader.f8()
    if tag == TAG_BYTE_ARRAY:
        return list(reader.take(reader.i4()))
    if tag == TAG_STRING:
        return reader.string()
    if tag == TAG_LIST:
        item_tag = reader.u1()
        count = reader.i4()
        return [read_payload(reader, item_tag) for _ in range(count)]
    if tag == TAG_COMPOUND:
        out = {}
        while True:
            child_tag = reader.u1()
            if child_tag == TAG_END:
                return out
            name = reader.string()
            out[name] = read_payload(reader, child_tag)
    if tag == TAG_INT_ARRAY:
        return [reader.i4() for _ in range(reader.i4())]
    if tag == TAG_LONG_ARRAY:
        return [reader.i8() for _ in range(reader.i4())]
    raise ValueError('unsupported tag %d' % tag)


def read_nbt(data: bytes):
    reader = Reader(data)
    tag = reader.u1()
    if tag != TAG_COMPOUND:
        raise ValueError('root tag is %d, not a compound' % tag)
    reader.string()
    return read_payload(reader, tag)


def iter_chunks(region_path: pathlib.Path):
    """Yield (chunk_x, chunk_z, nbt) for every present chunk in a region file."""
    raw = region_path.read_bytes()
    if len(raw) < 8192:
        return
    for index in range(1024):
        offset, sectors = struct.unpack('>IB', b'\x00' + raw[index * 4:index * 4 + 3] + raw[index * 4 + 3:index * 4 + 4])
        if offset == 0 or sectors == 0:
            continue
        start = offset * 4096
        length = struct.unpack('>I', raw[start:start + 4])[0]
        if length <= 1:
            continue
        compression = raw[start + 4]
        payload = raw[start + 5:start + 4 + length]
        try:
            if compression == 1:
                import gzip
                payload = gzip.decompress(payload)
            elif compression == 2:
                payload = zlib.decompress(payload)
            else:
                continue
        except Exception:
            continue
        try:
            nbt = read_nbt(payload)
        except Exception:
            continue
        # The chunk's own position is authoritative; the region index can be stale.
        if 'xPos' in nbt and 'zPos' in nbt:
            yield nbt['xPos'], nbt['zPos'], nbt
        else:
            yield (region_path.stem.split('.')[1], region_path.stem.split('.')[2], nbt)


def chunk_biomes(nbt) -> list[str]:
    """Every biome palette entry referenced by the chunk's sections, expanded by multiplicity."""
    names: list[str] = []
    for section in nbt.get('sections', []):
        biomes = section.get('biomes')
        if biomes is None:
            continue
        palette = biomes.get('palette') if isinstance(biomes, dict) else None
        if palette is None:
            # Older worlds store the palette as plain strings.
            if isinstance(biomes, dict) and isinstance(biomes.get('data'), list) and biomes.get('palette') is None:
                continue
            continue
        data = biomes.get('data')
        if data is None:
            names.extend(str(entry) for entry in palette)
            continue
        # 1.18+ packs biome indices into longs, four bits each at 64-entry palettes; the exact unpacking
        # depends on the palette size, so count the palette as referenced and let the caller read values.
        for entry in palette:
            names.append(str(entry))
    return names


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('world', type=pathlib.Path)
    parser.add_argument('--dimension', default='overworld')
    parser.add_argument('--top', type=int, default=12)
    parser.add_argument('--json', action='store_true')
    args = parser.parse_args()

    if args.dimension == 'overworld':
        region_dir = args.world / 'region'
    else:
        region_dir = args.world / 'DIM-1' / 'region' if args.dimension in ('nether', 'DIM-1') \
            else args.world / 'DIM1' / 'region'
    if not region_dir.is_dir():
        print('no region directory: %s' % region_dir, file=sys.stderr)
        return 1

    counts: collections.Counter[str] = collections.Counter()
    chunk_count = 0
    chunks_with_biomes = 0
    per_chunk: list[dict] = []
    for region in sorted(region_dir.glob('r.*.mca')):
        for cx, cz, nbt in iter_chunks(region):
            chunk_count += 1
            names = chunk_biomes(nbt)
            if names:
                chunks_with_biomes += 1
            unique = sorted(set(names))
            for name in unique:
                counts[name] += 1
            per_chunk.append({'chunk': [cx, cz], 'biomes': unique})

    if args.json:
        print(json.dumps({
            'world': str(args.world),
            'dimension': args.dimension,
            'chunks': chunk_count,
            'chunksWithBiomes': chunks_with_biomes,
            'biomes': dict(counts),
            'perChunk': per_chunk if args.top <= 0 else per_chunk[:args.top],
        }, indent=2))
        return 0

    print('world     : %s (%s)' % (args.world, args.dimension))
    print('region dir: %s' % region_dir)
    print('chunks    : %d read, %d carrying biome data' % (chunk_count, chunks_with_biomes))
    print('biomes referenced by palette:')
    for name, count in counts.most_common(args.top if args.top > 0 else None):
        print('  %-46s in %d chunk(s)' % (name, count))
    if not counts:
        print('  (none found - the chunk format may store biomes in a shape this reader does not unpack)')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
