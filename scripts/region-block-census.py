"""Counts blocks in Minecraft region files, without launching the game.

A generation run is not evidence of content. This reads the saved chunks themselves and reports which
blocks are actually there, so a preset can be judged by what it produced rather than by the fact that it
produced something.

Handles the Anvil region container and the 1.18+ chunk format (a `sections` list, each section holding a
`block_states` compound with a `palette` and optionally packed `data`). Only the standard library is used.

Usage:
    python scripts/region-block-census.py <world-dir> [--dimension overworld] [--top N]
"""
import argparse
import collections
import gzip
import io
import pathlib
import struct
import zlib

# --- NBT --------------------------------------------------------------------------------------------

TAG_END, TAG_BYTE, TAG_SHORT, TAG_INT, TAG_LONG, TAG_FLOAT, TAG_DOUBLE = 0, 1, 2, 3, 4, 5, 6
TAG_BYTE_ARRAY, TAG_STRING, TAG_LIST, TAG_COMPOUND, TAG_INT_ARRAY, TAG_LONG_ARRAY = 7, 8, 9, 10, 11, 12


class Reader:
    def __init__(self, data):
        self.data = data
        self.pos = 0

    def take(self, n):
        chunk = self.data[self.pos:self.pos + n]
        if len(chunk) != n:
            raise EOFError('wanted %d bytes at %d, %d available' % (n, self.pos, len(self.data) - self.pos))
        self.pos += n
        return chunk

    def u1(self):
        return self.take(1)[0]

    def i1(self):
        return struct.unpack('>b', self.take(1))[0]

    def i2(self):
        return struct.unpack('>h', self.take(2))[0]

    def u2(self):
        return struct.unpack('>H', self.take(2))[0]

    def i4(self):
        return struct.unpack('>i', self.take(4))[0]

    def i8(self):
        return struct.unpack('>q', self.take(8))[0]

    def f4(self):
        return struct.unpack('>f', self.take(4))[0]

    def f8(self):
        return struct.unpack('>d', self.take(8))[0]

    def string(self):
        return self.take(self.u2()).decode('utf-8', 'replace')


def read_payload(reader, tag):
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
        return reader.take(reader.i4())
    if tag == TAG_STRING:
        return reader.string()
    if tag == TAG_LIST:
        item = reader.u1()
        count = reader.i4()
        # A list of TAG_END is how an empty list is written; anything else would be malformed.
        if item == TAG_END:
            return []
        return [read_payload(reader, item) for _ in range(count)]
    if tag == TAG_COMPOUND:
        out = {}
        while True:
            child = reader.u1()
            if child == TAG_END:
                return out
            name = reader.string()
            out[name] = read_payload(reader, child)
    if tag == TAG_INT_ARRAY:
        return [reader.i4() for _ in range(reader.i4())]
    if tag == TAG_LONG_ARRAY:
        return [reader.i8() for _ in range(reader.i4())]
    raise ValueError('unknown NBT tag %d at %d' % (tag, reader.pos))


def read_nbt(data):
    reader = Reader(data)
    tag = reader.u1()
    if tag != TAG_COMPOUND:
        raise ValueError('root NBT tag is %d, expected a compound' % tag)
    reader.string()
    return read_payload(reader, TAG_COMPOUND)


# --- region container -------------------------------------------------------------------------------

def iter_chunks(region_path):
    """Yields the decompressed NBT bytes of every chunk present in a region file."""
    raw = region_path.read_bytes()
    if len(raw) < 8192:
        return
    for index in range(1024):
        entry = raw[index * 4:index * 4 + 4]
        offset, sectors = struct.unpack('>I', b'\x00' + entry[:3])[0], entry[3]
        if offset == 0 or sectors == 0:
            continue
        start = offset * 4096
        header = raw[start:start + 5]
        if len(header) < 5:
            continue
        length, compression = struct.unpack('>IB', header[:5])
        payload = raw[start + 5:start + 4 + length]
        try:
            if compression == 1:
                yield gzip.decompress(payload)
            elif compression == 2:
                yield zlib.decompress(payload)
            elif compression == 3:
                yield payload
        except (OSError, zlib.error):
            continue


# --- chunk block decoding ---------------------------------------------------------------------------

def unpack_indices(longs, bits, count):
    """Unpacks palette indices. 1.16+ packs values without letting one straddle two longs."""
    if bits == 0:
        return [0] * count
    per_long = 64 // bits
    mask = (1 << bits) - 1
    out = []
    for value in longs:
        value &= (1 << 64) - 1
        for slot in range(per_long):
            if len(out) >= count:
                return out
            out.append((value >> (slot * bits)) & mask)
    return out


def section_blocks(section):
    states = section.get('block_states')
    if not isinstance(states, dict):
        return []
    palette = states.get('palette') or []
    names = [entry.get('Name', 'minecraft:air') for entry in palette]
    if len(names) == 1:
        return [names[0]] * 4096
    bits = max(4, (len(names) - 1).bit_length())
    data = states.get('data') or []
    indices = unpack_indices(data, bits, 4096)
    return [names[i] if i < len(names) else 'minecraft:unknown' for i in indices]


def census_world(world_dir, dimension='overworld'):
    region_dir = pathlib.Path(world_dir) / 'region' if dimension == 'overworld' else \
        pathlib.Path(world_dir) / 'DIM-1' / 'region' if dimension == 'nether' else \
        pathlib.Path(world_dir) / 'DIM1' / 'region'
    files = sorted(region_dir.glob('*.mca'))
    if not files:
        raise SystemExit('no region files under %s' % region_dir)
    counts = collections.Counter()
    chunks, sections_seen = 0, 0
    min_y, max_y = None, None
    for path in files:
        for nbt_bytes in iter_chunks(path):
            try:
                root = read_nbt(nbt_bytes)
            except (ValueError, EOFError, struct.error):
                continue
            chunks += 1
            level = root.get('Level') if isinstance(root.get('Level'), dict) else root
            for section in level.get('sections', []) or []:
                if not isinstance(section, dict):
                    continue
                sections_seen += 1
                y = section.get('Y')
                if isinstance(y, int):
                    min_y = y if min_y is None else min(min_y, y)
                    max_y = y if max_y is None else max(max_y, y)
                for name in section_blocks(section):
                    counts[name] += 1
    return {
        'regionFiles': len(files),
        'chunks': chunks,
        'sections': sections_seen,
        'minSectionY': min_y,
        'maxSectionY': max_y,
        'counts': counts,
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('world')
    parser.add_argument('--dimension', default='overworld',
                        choices=['overworld', 'nether', 'end'])
    parser.add_argument('--top', type=int, default=25)
    parser.add_argument('--known-blocks', default=None,
                        help='a palette JSON whose blockNames are treated as legitimate for this version')
    args = parser.parse_args()

    result = census_world(args.world, args.dimension)
    counts = result['counts']
    total = sum(counts.values())
    print('region files : %d' % result['regionFiles'])
    print('chunks       : %d' % result['chunks'])
    print('sections     : %d  (Y %s..%s)' % (result['sections'], result['minSectionY'],
                                             result['maxSectionY']))
    print('blocks       : %d  distinct %d' % (total, len(counts)))
    print()
    print('top blocks:')
    for name, count in counts.most_common(args.top):
        print('  %-42s %8d  %5.2f%%' % (name, count, 100.0 * count / total if total else 0.0))

    if args.known_blocks:
        import json
        data = json.loads(pathlib.Path(args.known_blocks).read_text(encoding='utf-8'))
        known = {('minecraft:' + n) if ':' not in n else n for n in data.get('blockNames', [])}
        # Only judge non-technical blocks; air/light/water are not evidence about a version.
        skip = {'minecraft:air', 'minecraft:cave_air', 'minecraft:void_air', 'minecraft:light',
                'minecraft:water', 'minecraft:lava', 'minecraft:bedrock', 'minecraft:stone',
                'minecraft:dirt', 'minecraft:grass_block', 'minecraft:sand', 'minecraft:gravel'}
        unexpected = [(n, c) for n, c in counts.most_common()
                      if n not in known and n not in skip and c > 0]
        print()
        print('blocks NOT in the declared palette (%d distinct):' % len(unexpected))
        for name, count in unexpected[:30]:
            print('  %-42s %8d' % (name, count))


if __name__ == '__main__':
    main()
