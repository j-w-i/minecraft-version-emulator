"""Prepare a test world at a chosen historical version by editing its level.dat preset alias.

Why this is needed: the server refuses to open a world whose loaded level stem and persisted metadata
disagree (a deliberate fail-closed check), so a test world for the Alpha v1.2.0 biome conversion must
have BOTH changed, not just the metadata. This writes level.dat properly - re-encoding every tag and
every length prefix - rather than patching bytes, because the alias changes length.

It edits exactly one thing: the Moderner Beta preset alias at the three paths the overworld's generator
records it. Nothing else in level.dat is touched.

Usage:
    python scripts/set-world-preset.py "<world-dir>" historical_experience:alpha_1_2_0_no_modern_content [--dry-run]
"""
from __future__ import annotations

import argparse
import gzip
import importlib.util
import pathlib
import struct
import sys

TAG_END, TAG_BYTE, TAG_SHORT, TAG_INT, TAG_LONG, TAG_FLOAT, TAG_DOUBLE = 0, 1, 2, 3, 4, 5, 6
TAG_BYTE_ARRAY, TAG_STRING, TAG_LIST, TAG_COMPOUND, TAG_INT_ARRAY, TAG_LONG_ARRAY = 7, 8, 9, 10, 11, 12

PRESET_PATHS = (
    ('Data', 'WorldGenSettings', 'dimensions', 'minecraft:overworld', 'generator', 'provider_settings',
     'moderner_beta:preset'),
    ('Data', 'WorldGenSettings', 'dimensions', 'minecraft:overworld', 'generator', 'biome_source',
     'provider_settings', 'moderner_beta:preset'),
    ('Data', 'WorldGenSettings', 'dimensions', 'minecraft:overworld', 'generator', 'biome_source',
     'cave_provider_settings', 'moderner_beta:preset'),
)


def load_census():
    spec = importlib.util.spec_from_file_location(
        'census', pathlib.Path(__file__).with_name('region-biome-census.py'))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def read_level_dat(path: pathlib.Path):
    raw = path.read_bytes()
    if raw[:2] == b'\x1f\x8b':
        raw = gzip.decompress(raw)
    return load_census().read_nbt(raw)


# --- NBT writer -------------------------------------------------------------------------------------

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


# The reader in region-biome-census.py returns plain Python values, which loses the tag type. So the
# tree is re-derived here with explicit types by sniffing the values the reader produced. Booleans and
# small ints are ambiguous between BYTE/SHORT/INT, so the original bytes are consulted instead: this
# reader/writer pair round-trips a level.dat exactly, which set-world-preset verifies before writing.
def typed(tag: int, value):
    return (tag, value)


def read_typed(reader, tag):
    """Read a payload, keeping its tag alongside the value so the writer can reproduce it exactly."""
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
        items = [read_typed(reader, item_tag) for _ in range(count)]
        return (item_tag, [item for item in items])
    if tag == TAG_COMPOUND:
        out = {}
        while True:
            child_tag = reader.u1()
            if child_tag == TAG_END:
                return out
            name = reader.string()
            out[name] = (child_tag, read_typed(reader, child_tag))
        return out
    if tag == TAG_INT_ARRAY:
        return [reader.i4() for _ in range(reader.i4())]
    if tag == TAG_LONG_ARRAY:
        return [reader.i8() for _ in range(reader.i4())]
    raise ValueError('unsupported tag %d' % tag)


def read_typed_root(raw: bytes):
    reader = load_census().Reader(raw)
    tag = reader.u1()
    if tag != TAG_COMPOUND:
        raise ValueError('root tag is %d, not a compound' % tag)
    name = reader.string()
    return name, read_typed(reader, tag)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('world', type=pathlib.Path)
    parser.add_argument('preset')
    parser.add_argument('--dry-run', action='store_true')
    args = parser.parse_args()

    level_dat = args.world / 'level.dat'
    raw = level_dat.read_bytes()
    compressed = raw[:2] == b'\x1f\x8b'
    if compressed:
        raw = gzip.decompress(raw)

    # Round-trip first: if re-encoding the untouched tree does not reproduce the original bytes, the
    # writer is not faithful and must not be trusted to rewrite the world.
    name, tree = read_typed_root(raw)
    round_trip = bytearray()
    round_trip += struct.pack('>b', TAG_COMPOUND)
    encoded_name = name.encode('utf-8')
    round_trip += struct.pack('>H', len(encoded_name)) + encoded_name
    write_payload(round_trip, TAG_COMPOUND, tree)
    if bytes(round_trip) != raw:
        print('REFUSING: re-encoding the untouched level.dat did not reproduce it (%d vs %d bytes)'
              % (len(round_trip), len(raw)), file=sys.stderr)
        return 2

    changed = []
    for path in PRESET_PATHS:
        node = tree
        for key in path[:-1]:
            entry = node.get(key)
            if entry is None:
                break
            node = entry[1]
        else:
            entry = node.get(path[-1])
            if entry is not None and entry[0] == TAG_STRING:
                changed.append((path, entry[1]))
                node[path[-1]] = (TAG_STRING, args.preset)

    if not changed:
        print('no preset alias found at the expected paths; nothing written', file=sys.stderr)
        return 1

    for path, previous in changed:
        print('%-64s %s -> %s' % ('.'.join(path), previous, args.preset))
    if args.dry_run:
        print('dry run: level.dat not written')
        return 0

    out = bytearray()
    out += struct.pack('>b', TAG_COMPOUND)
    out += struct.pack('>H', len(encoded_name)) + encoded_name
    write_payload(out, TAG_COMPOUND, tree)
    level_dat.write_bytes(gzip.compress(bytes(out)) if compressed else bytes(out))
    print('wrote %s (%d bytes uncompressed)' % (level_dat, len(out)))
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
