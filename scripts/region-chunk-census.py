#!/usr/bin/env python3
"""Per-chunk census of an Anvil region directory: which chunks exist and a hash of each one's stored bytes.

Developer measurement for the finite-to-infinite upgrade: run it before and after an upgrade and compare. A chunk the
upgrade was meant to PRESERVE must have an identical hash afterwards; a chunk it released must be gone. Reads only.

    python scripts/region-chunk-census.py <world>/region  [--inside-chunks 8]   -> JSON on stdout

--inside-chunks N classifies chunks with x and z in [-N, N) as the island (the 256x256 default footprint is N = 8).
"""
import argparse
import hashlib
import json
import struct
from pathlib import Path


def chunks(region_dir):
    for path in sorted(Path(region_dir).glob('r.*.*.mca')):
        _, rx, rz, _ = path.name.split('.')
        rx, rz = int(rx), int(rz)
        data = path.read_bytes()
        for slot in range(1024):
            entry = struct.unpack('>I', data[slot * 4:slot * 4 + 4])[0]
            offset, count = entry >> 8, entry & 0xFF
            if offset == 0 or count == 0:
                continue
            start = offset * 4096
            length = struct.unpack('>I', data[start:start + 4])[0]
            blob = data[start + 4:start + 4 + length]
            yield rx * 32 + slot % 32, rz * 32 + slot // 32, hashlib.sha256(blob).hexdigest()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('region')
    parser.add_argument('--inside-chunks', type=int, default=8)
    args = parser.parse_args()
    n = args.inside_chunks
    result = {'inside': {}, 'outside': {}}
    for x, z, digest in chunks(args.region):
        bucket = 'inside' if -n <= x < n and -n <= z < n else 'outside'
        result[bucket]['%d,%d' % (x, z)] = digest
    result['insideCount'] = len(result['inside'])
    result['outsideCount'] = len(result['outside'])
    print(json.dumps(result))


if __name__ == '__main__':
    main()
