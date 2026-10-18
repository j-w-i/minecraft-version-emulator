#!/usr/bin/env python3
"""Reports the Y distribution of each ore in a saved world, from the region files.

Answers "does any ore appear outside the layers the era allowed?" directly, from the blocks that
were written, rather than from the rules that were meant to write them.

    python scripts/ore-height-census.py <world-dir> [--dimension overworld]

Only the standard library is used; the region/NBT decoding is shared with region-block-census.py.
"""

from __future__ import annotations

import collections
import importlib.util
import pathlib
import struct
import sys

HERE = pathlib.Path(__file__).resolve().parent
_spec = importlib.util.spec_from_file_location("region_block_census", HERE / "region-block-census.py")
_census = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_census)

ORES = (
    "minecraft:coal_ore",
    "minecraft:iron_ore",
    "minecraft:gold_ore",
    "minecraft:redstone_ore",
    "minecraft:diamond_ore",
    "minecraft:lapis_ore",
    "minecraft:deepslate_coal_ore",
    "minecraft:deepslate_iron_ore",
    "minecraft:deepslate_gold_ore",
    "minecraft:deepslate_redstone_ore",
    "minecraft:deepslate_diamond_ore",
    "minecraft:deepslate_lapis_ore",
    "minecraft:emerald_ore",
    "minecraft:copper_ore",
)

SECTION = 16


def main():
    world = sys.argv[1]
    dimension = "overworld"
    if "--dimension" in sys.argv:
        dimension = sys.argv[sys.argv.index("--dimension") + 1]
    region_dir = pathlib.Path(world) / "region" if dimension == "overworld" else \
        pathlib.Path(world) / "DIM-1" / "region" if dimension == "nether" else \
        pathlib.Path(world) / "DIM1" / "region"
    files = sorted(region_dir.glob("*.mca"))
    if not files:
        raise SystemExit("no region files under %s" % region_dir)

    by_y = collections.defaultdict(collections.Counter)
    chunks = 0
    for path in files:
        for nbt_bytes in _census.iter_chunks(path):
            try:
                root = _census.read_nbt(nbt_bytes)
            except (ValueError, EOFError, struct.error):
                continue
            chunks += 1
            level = root.get("Level") if isinstance(root.get("Level"), dict) else root
            for section in level.get("sections", []) or []:
                if not isinstance(section, dict):
                    continue
                section_y = section.get("Y")
                if not isinstance(section_y, int):
                    continue
                blocks = _census.section_blocks(section)
                for index, name in enumerate(blocks):
                    if name not in ORES:
                        continue
                    local_y = index // (SECTION * SECTION)
                    by_y[name][section_y * SECTION + local_y] += 1

    print("world=%s dimension=%s chunks=%d" % (world, dimension, chunks))
    for ore in ORES:
        heights = by_y.get(ore)
        if not heights:
            continue
        total = sum(heights.values())
        above = sorted(y for y in heights if y > 15)
        top = max(heights)
        print("%-34s n=%-6d y=%d..%d  above15=%d  worst=%s"
              % (ore, total, min(heights), top, sum(heights[y] for y in above),
                 ", ".join("%d:%d" % (y, heights[y]) for y in above[-6:])))
    return 0


if __name__ == "__main__":
    sys.exit(main())
