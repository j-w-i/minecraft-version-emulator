#!/usr/bin/env python3
"""Counts what one chunk contains, in two region directories, to separate a harmless re-save from a content change.

    python scripts/region-chunk-contents.py <regionA> <regionB> <chunkX> <chunkZ>

For each side it decompresses the chunk and reports: size, the 'block_ticks' / 'fluid_ticks' marker counts, how many scheduled
oak_leaves entries there are, whether SkyLight / BlockLight arrays are present, the isLightOn flag position, and the entity /
block-entity marker counts. Counting byte markers is crude (it is not an NBT parse) and says so: it is for deciding whether a
differing chunk deserves a real inspection, not for proving equality.
"""
import struct
import sys
import zlib
from pathlib import Path


def chunk_bytes(region_dir, cx, cz):
    path = Path(region_dir) / ('r.%d.%d.mca' % (cx >> 5, cz >> 5))
    data = path.read_bytes()
    slot = (cx & 31) + (cz & 31) * 32
    entry = struct.unpack('>I', data[slot * 4:slot * 4 + 4])[0]
    offset = entry >> 8
    if offset == 0:
        return None
    start = offset * 4096
    length = struct.unpack('>I', data[start:start + 4])[0]
    scheme = data[start + 4]
    blob = data[start + 5:start + 4 + length]
    return zlib.decompress(blob) if scheme == 2 else blob


MARKERS = [b'block_ticks', b'fluid_ticks', b'minecraft:oak_leaves', b'SkyLight', b'BlockLight', b'isLightOn',
           b'block_entities', b'Heightmaps', b'InhabitedTime', b'LastUpdate', b'Status']


def describe(label, blob):
    if blob is None:
        print('%s: <absent>' % label)
        return
    print('%s: %d bytes' % (label, len(blob)))
    for marker in MARKERS:
        print('    %-22s x%d' % (marker.decode(), blob.count(marker)))
    # The tick lists: how many entries sit between block_ticks and the next top-level marker. Crude: count 't' (tick) tags
    # as the 4-byte-aligned name 't' preceded by the int type byte is unreliable, so report the oak_leaves count only.


def main():
    a_dir, b_dir, cx, cz = sys.argv[1], sys.argv[2], int(sys.argv[3]), int(sys.argv[4])
    describe('A (before)', chunk_bytes(a_dir, cx, cz))
    describe('B (after) ', chunk_bytes(b_dir, cx, cz))


if __name__ == '__main__':
    main()
