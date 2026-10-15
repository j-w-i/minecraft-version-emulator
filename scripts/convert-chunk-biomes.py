"""Rewrite the biomes stored in a world's chunks, fast, with a progress bar.

Why this is needed. Moderner Beta's biome source answers every position from its provider, so a settings
preset decides what F3 reports - but a chunk also STORES a biome per section, and APIs backed by the chunk
(`Level.getBiome`) read that instead. After a preset change the two disagree until the stored values are
rewritten. Measured on a Release 1.6.4 world: the provider said `moderner_beta:late_beta_plains` while the
chunk still said `moderner_beta:beta_rainforest`.

How it works. An Anvil region file holds 32x32 chunks; each is compressed NBT. Only the `biomes` container's
palette is touched, and every entry in it is replaced, so a section holding several biomes becomes the target
biome without re-packing an index. Because the target name is a different LENGTH from the names it replaces,
each NBT string's length prefix is rebuilt - which is why this parses NBT rather than patching bytes in place.

Performance. One worker per region file, so regions convert in parallel; each worker reads only its own file
and the results are written atomically with `os.replace`. A chunk already holding only the target biome is
left byte-identical, so re-running is nearly free.

SAFETY: this writes the world's region files. It must NOT be run while that world is open - a running server
holds those files and would overwrite the changes on its next save.

Usage:
    python scripts/convert-chunk-biomes.py <world-dir> --biome moderner_beta:late_beta_plains
    python scripts/convert-chunk-biomes.py <world-dir> --biome ... --dry-run
    python scripts/convert-chunk-biomes.py <world-dir> --biome ... --threads 8
"""
from __future__ import annotations

import argparse
import concurrent.futures
import gzip
import importlib.util
import os
import pathlib
import struct
import sys
import threading
import time
import zlib

SCRIPT_DIR = pathlib.Path(__file__).resolve().parent
SECTOR = 4096

TAG_END, TAG_BYTE, TAG_SHORT, TAG_INT, TAG_LONG, TAG_FLOAT, TAG_DOUBLE = 0, 1, 2, 3, 4, 5, 6
TAG_BYTE_ARRAY, TAG_STRING, TAG_LIST, TAG_COMPOUND, TAG_INT_ARRAY, TAG_LONG_ARRAY = 7, 8, 9, 10, 11, 12


def load_census():
    spec = importlib.util.spec_from_file_location('census', SCRIPT_DIR / 'region-biome-census.py')
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


CENSUS = load_census()


# --- NBT: the census module reads; this module also writes --------------------------------------------

def write_payload(out: bytearray, tag: int, value) -> None:
    if tag == TAG_BYTE:
        out += struct.pack('>b', value)
    elif tag == TAG_SHORT:
        out += struct.pack('>h', value)
    elif tag == TAG_INT:
        out += struct.pack('>i', value)
    elif tag == TAG_LONG:
        out += struct.pack('>q', value)
    elif tag == TAG_FLOAT:
        out += struct.pack('>f', value)
    elif tag == TAG_DOUBLE:
        out += struct.pack('>d', value)
    elif tag == TAG_BYTE_ARRAY:
        out += struct.pack('>i', len(value)) + bytes((b & 0xFF) for b in value)
    elif tag == TAG_STRING:
        encoded = value.encode('utf-8')
        out += struct.pack('>H', len(encoded)) + encoded
    elif tag == TAG_LIST:
        item_tag, items = value
        out += struct.pack('>b', item_tag) + struct.pack('>i', len(items))
        for item in items:
            write_payload(out, item_tag, item)
    elif tag == TAG_COMPOUND:
        for name, (child_tag, child) in value.items():
            out += struct.pack('>b', child_tag)
            encoded = name.encode('utf-8')
            out += struct.pack('>H', len(encoded)) + encoded
            write_payload(out, child_tag, child)
        out += struct.pack('>b', TAG_END)
    elif tag == TAG_INT_ARRAY:
        out += struct.pack('>i', len(value)) + b''.join(struct.pack('>i', v) for v in value)
    elif tag == TAG_LONG_ARRAY:
        out += struct.pack('>i', len(value)) + b''.join(struct.pack('>q', v) for v in value)
    else:
        raise ValueError('cannot write tag %d' % tag)


def read_typed(reader, tag):
    """Read a payload keeping its tag, so the writer can reproduce the chunk exactly."""
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
        return (item_tag, [read_typed(reader, item_tag) for _ in range(count)])
    if tag == TAG_COMPOUND:
        out = {}
        while True:
            child_tag = reader.u1()
            if child_tag == TAG_END:
                return out
            name = reader.string()
            out[name] = (child_tag, read_typed(reader, child_tag))
    if tag == TAG_INT_ARRAY:
        return [reader.i4() for _ in range(reader.i4())]
    if tag == TAG_LONG_ARRAY:
        return [reader.i8() for _ in range(reader.i4())]
    raise ValueError('unsupported tag %d' % tag)


def read_typed_root(raw: bytes):
    reader = CENSUS.Reader(raw)
    tag = reader.u1()
    if tag != TAG_COMPOUND:
        raise ValueError('root tag is %d, not a compound' % tag)
    return reader.string(), read_typed(reader, tag)


def encode_root(name: str, tree) -> bytes:
    out = bytearray()
    out += struct.pack('>b', TAG_COMPOUND)
    encoded = name.encode('utf-8')
    out += struct.pack('>H', len(encoded)) + encoded
    write_payload(out, TAG_COMPOUND, tree)
    return bytes(out)


# --- conversion ----------------------------------------------------------------------------------------

class Progress:
    """Thread-safe progress, so workers report without printing over each other."""

    def __init__(self, total: int):
        self.total = max(1, total)
        self.done = 0
        self.changed = 0
        self.lock = threading.Lock()
        self.started = time.time()
        self._last = 0.0

    def add(self, changed: int) -> None:
        with self.lock:
            self.done += 1
            self.changed += changed
            now = time.time()
            if now - self._last < 0.1 and self.done < self.total:
                return
            self._last = now
            elapsed = max(1e-6, now - self.started)
            rate = self.done / elapsed
            remaining = (self.total - self.done) / rate if rate else 0.0
            filled = int(34 * self.done / self.total)
            sys.stdout.write('\r  [%s%s] %6d/%d  %5.1f%%  %6.0f chunks/s  %4.0fs left  %d changed'
                             % ('#' * filled, '-' * (34 - filled), self.done, self.total,
                                100.0 * self.done / self.total, rate, remaining, self.changed))
            sys.stdout.flush()

    def finish(self) -> None:
        with self.lock:
            elapsed = max(1e-6, time.time() - self.started)
            sys.stdout.write('\r  [%s] %d/%d  100.0%%  %.1fs  %d changed\n'
                             % ('#' * 34, self.done, self.total, elapsed, self.changed))
            sys.stdout.flush()


def unwrap(value):
    """Return the item list from a parsed LIST payload, whichever shape it arrived in.

    <p>This was got wrong three times, so the reason is written down. A LIST payload parses to
    {@code (item_tag, [items])}, but the census reader builds that pair as a single element, so
    {@code sections[1]} arrives as {@code [item_tag, [items]]} - a one-element list whose only member is the real
    list. Callers also hand in the pair itself. Every shape seen in this repository is accepted here, because
    guessing wrong is silent: a rewrite that iterates the wrong level reports zero changes and looks like a no-op.
    """
    if not isinstance(value, (list, tuple)):
        return None
    if len(value) == 2 and isinstance(value[0], int) and isinstance(value[1], (list, tuple)):
        return value[1]
    if len(value) == 1 and isinstance(value[0], (list, tuple)):
        return value[0]
    return value


def item_value(item):
    """A list item is {@code (tag, value)} when the helper wrapped it; otherwise it is the value."""
    if isinstance(item, tuple) and len(item) == 2 and isinstance(item[0], int):
        return item[1]
    return item


def rewrite_biomes(tree, target: str) -> int:
    """Replace every entry of every biome palette in a chunk. Returns how many entries changed."""
    sections = tree.get('sections')
    if sections is None:
        return 0
    items = unwrap(sections[1] if isinstance(sections, tuple) else sections)
    if items is None:
        return 0
    changed = 0
    for item in items:
        section = item_value(item)
        if not isinstance(section, dict):
            continue
        container = section.get('biomes')
        if container is None:
            continue
        compound = item_value(container)
        if not isinstance(compound, dict):
            continue
        palette = compound.get('palette')
        if palette is None:
            continue
        entries = unwrap(item_value(palette))
        if not isinstance(entries, list):
            continue
        for index, entry in enumerate(entries):
            if isinstance(entry, str) and entry != target:
                entries[index] = target
                changed += 1
    return changed


def chunk_slots(raw: bytes) -> list[int]:
    """The slots that hold a chunk, from the 4 KiB location table."""
    slots = []
    for slot in range(1024):
        entry = raw[slot * 4:slot * 4 + 4]
        if len(entry) < 4:
            break
        offset = (entry[0] << 16) | (entry[1] << 8) | entry[2]
        if offset and entry[3]:
            slots.append(slot)
    return slots


def convert_region(region: pathlib.Path, target: str, progress: Progress, dry_run: bool) -> tuple[int, int]:
    """Convert one region file. Returns (chunks seen, chunks rewritten)."""
    raw = region.read_bytes()
    if len(raw) < 8192:
        return 0, 0

    slots = chunk_slots(raw)
    blobs: dict[int, bytes] = {}
    seen = 0
    rewritten = 0
    for slot in slots:
        entry = raw[slot * 4:slot * 4 + 4]
        start = ((entry[0] << 16) | (entry[1] << 8) | entry[2]) * SECTOR
        length = struct.unpack('>I', raw[start:start + 4])[0]
        if length <= 1:
            continue
        compression = raw[start + 4]
        payload = raw[start + 5:start + 4 + length]
        blob = raw[start:start + 4 + length]
        seen += 1
        if compression not in (1, 2):
            progress.add(0)
            blobs[slot] = blob
            continue
        try:
            decompressed = zlib.decompress(payload) if compression == 2 else gzip.decompress(payload)
            name, tree = read_typed_root(decompressed)
            changed = rewrite_biomes(tree, target)
        except Exception:
            progress.add(0)
            blobs[slot] = blob
            continue
        if changed == 0:
            progress.add(0)
            blobs[slot] = blob
            continue
        encoded = encode_root(name, tree)
        body = zlib.compress(encoded) if compression == 2 else gzip.compress(encoded)
        blobs[slot] = struct.pack('>I', len(body) + 1) + bytes([compression]) + body
        rewritten += 1
        progress.add(changed)

    if rewritten == 0 or dry_run:
        return seen, 0 if dry_run else rewritten

    # Sizes changed, so the file is rebuilt and the location table recomputed.
    header = bytearray(8192)
    header[4096:8192] = raw[4096:8192]          # timestamps are preserved
    body = bytearray()
    sector = 2
    for slot in sorted(blobs):
        blob = blobs[slot]
        sectors = (len(blob) + SECTOR - 1) // SECTOR
        header[slot * 4] = (sector >> 16) & 0xFF
        header[slot * 4 + 1] = (sector >> 8) & 0xFF
        header[slot * 4 + 2] = sector & 0xFF
        header[slot * 4 + 3] = sectors
        body += blob
        body += b'\x00' * (sectors * SECTOR - len(blob))
        sector += sectors

    temp = region.with_name(region.name + '.tmp')
    with open(temp, 'wb') as handle:
        handle.write(header)
        handle.write(body)
    os.replace(temp, region)
    return seen, rewritten


def region_directory(world: pathlib.Path, dimension: str) -> pathlib.Path:
    if dimension == 'overworld':
        return world / 'region'
    if dimension in ('nether', 'DIM-1'):
        return world / 'DIM-1' / 'region'
    return world / 'DIM1' / 'region'


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('world', type=pathlib.Path)
    parser.add_argument('--biome', required=True, help='target biome id, e.g. moderner_beta:late_beta_plains')
    parser.add_argument('--dimension', default='overworld')
    parser.add_argument('--threads', type=int, default=0, help='0 = one per CPU')
    parser.add_argument('--dry-run', action='store_true')
    args = parser.parse_args()

    region_dir = region_directory(args.world, args.dimension)
    if not region_dir.is_dir():
        print('no region directory: %s' % region_dir, file=sys.stderr)
        return 1
    regions = sorted(region_dir.glob('r.*.mca'))
    if not regions:
        print('no region files in %s' % region_dir, file=sys.stderr)
        return 1

    total = sum(len(chunk_slots(region.read_bytes())) for region in regions)
    threads = args.threads or (os.cpu_count() or 4)
    print('%s\n  %d region(s), %d chunk(s), %d thread(s), target %s%s'
          % (region_dir, len(regions), total, threads, args.biome, '  [DRY RUN]' if args.dry_run else ''))
    if not args.dry_run:
        print('  NOTE: the world must be closed; a running server would overwrite this on its next save.')

    progress = Progress(total)
    seen_total = 0
    changed_total = 0
    with concurrent.futures.ThreadPoolExecutor(max_workers=threads) as pool:
        futures = [pool.submit(convert_region, region, args.biome, progress, args.dry_run)
                   for region in regions]
        for future in concurrent.futures.as_completed(futures):
            try:
                seen, changed = future.result()
            except Exception as failure:      # one bad region must not stop the rest
                print('\n  region failed: %s' % failure, file=sys.stderr)
                continue
            seen_total += seen
            changed_total += changed
    progress.finish()
    print('%d chunk(s) read, %d rewritten to %s%s'
          % (seen_total, changed_total, args.biome, ' (dry run: nothing written)' if args.dry_run else ''))
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
