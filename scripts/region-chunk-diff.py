#!/usr/bin/env python3
"""Byte-level diff of ONE chunk between two Anvil region directories. Developer measurement; reads only.

    python scripts/region-chunk-diff.py <regionA> <regionB> <chunkX> <chunkZ>

Decompresses the chunk from each side, reports the sizes, and lists the differing byte runs with printable context so a
reader can tell a harmless re-save (a timestamp, a tick counter) from a real content change (blocks, entities).
"""
import struct
import sys
import zlib
from pathlib import Path


def chunk_bytes(region_dir, cx, cz):
    path = Path(region_dir) / ('r.%d.%d.mca' % (cx >> 5, cz >> 5))
    if not path.exists():
        return None
    data = path.read_bytes()
    slot = (cx & 31) + (cz & 31) * 32
    entry = struct.unpack('>I', data[slot * 4:slot * 4 + 4])[0]
    offset, count = entry >> 8, entry & 0xFF
    if offset == 0 or count == 0:
        return None
    start = offset * 4096
    length = struct.unpack('>I', data[start:start + 4])[0]
    scheme = data[start + 4]
    blob = data[start + 5:start + 4 + length]
    if scheme == 2:
        return zlib.decompress(blob)
    if scheme == 1:
        import gzip
        return gzip.decompress(blob)
    if scheme == 3:
        return blob
    raise SystemExit('unsupported compression scheme %d' % scheme)


def printable(chunk):
    return ''.join(chr(b) if 32 <= b < 127 else '.' for b in chunk)


def main():
    a_dir, b_dir, cx, cz = sys.argv[1], sys.argv[2], int(sys.argv[3]), int(sys.argv[4])
    a, b = chunk_bytes(a_dir, cx, cz), chunk_bytes(b_dir, cx, cz)
    print('A: %s bytes   B: %s bytes' % (None if a is None else len(a), None if b is None else len(b)))
    if a is None or b is None:
        return
    if a == b:
        print('IDENTICAL after decompression')
        return
    runs, i, n = [], 0, min(len(a), len(b))
    while i < n:
        if a[i] != b[i]:
            j = i
            while j < n and (a[j] != b[j] or j - i < 8):
                j += 1
            runs.append((i, j))
            i = j
        else:
            i += 1
    print('differing runs in the common prefix: %d  (length difference: %d)' % (len(runs), len(b) - len(a)))
    for start, end in runs[:12]:
        lo = max(0, start - 40)
        print('--- offset %d..%d' % (start, end))
        print('  A: ' + printable(a[lo:end + 12]))
        print('  B: ' + printable(b[lo:end + 12]))


if __name__ == '__main__':
    main()
