#!/usr/bin/env python3
# Minecraft Historical Experience
# SPDX-License-Identifier: LGPL-3.0-or-later
"""Extract the block registry of the Indev 20100223 client from its obfuscated classes.

WHY: the owner asked twice for Indev's block availability to be researched from the jar rather than inherited
("You could also extract from a jar...", "Use Indev source download latest Indev jar and extract the blocks").
Indev 20100223 stores no block NAME strings, so the jar can only give the registered ids with their texture
indices; the names come from the era's documented id table and are recorded separately with their own citation.

HOW: the registry class builds its constants in <clinit> as

    new <BlockClass>; dup; <id>; <textureIndex>; <Material>; invokespecial <init>(...); putstatic <field>

so each putstatic is walked back to the nearest `new` of THAT FIELD'S OWN class - not merely the nearest `new`,
because a block's construction nests other objects (its step sound, its material) whose `new` sits in between.
That nesting, plus reading `iconst_0`..`iconst_5` as values (they carry no operand where sipush/bipush do), is
what earlier scans in PowerShell got wrong in opposite directions.

Usage:  javap -p -c -classpath <extracted-jar-dir> <registry-class> > registry.txt
        python scripts/extract-indev-blocks.py registry.txt research/indev-20100223-blocks.json
"""

import json
import re
import sys

PUSH_PATTERNS = (
    re.compile(r":\s+iconst_(-?\d+)\s*$"),
    re.compile(r":\s+(?:sipush|bipush|ldc)\s+(-?\d+)\s*$"),
)
NEW_RE = re.compile(r":\s+new\s+#\d+\s+//\s+class\s+(\S+)")
PUTSTATIC_RE = re.compile(r"putstatic\s+#\d+\s+//\s+Field\s+(\S+?):(L[^;]+);")


def push_value(line):
    for pattern in PUSH_PATTERNS:
        match = pattern.search(line)
        if match:
            return int(match.group(1))
    return None


def extract(lines):
    blocks = []
    for index, line in enumerate(lines):
        store = PUTSTATIC_RE.search(line)
        if not store:
            continue
        field, descriptor = store.group(1), store.group(2)
        # The putstatic prints a descriptor without its trailing semicolon (Lnet/minecraft/a/a/b/n) while javap prints
        # the `new` comment as a bare binary name (net/minecraft/a/a/b/n). Appending a semicolon to the name before
        # comparing it - which both earlier attempts did - is what made them report zero stores.
        # Prefer an exact class match, and fall back to the nearest construction in the same package: a field is
        # often declared as the base block while the construction is one of its subclasses, and requiring an exact
        # match left 40 of the registry's 71 stores unread.
        exact = None
        package_fallback = None
        package_prefix = descriptor.rsplit("/", 1)[0] + "/"
        for back in range(index - 1, max(-1, index - 40), -1):
            found = NEW_RE.search(lines[back])
            if not found:
                continue
            if "L" + found.group(1) == descriptor:
                exact = back
                break
            if package_fallback is None and ("L" + found.group(1)).startswith(package_prefix):
                package_fallback = back
        construction_at = exact if exact is not None else package_fallback
        if construction_at is None:
            continue
        pushes = []
        for forward in range(construction_at + 1, index):
            value = push_value(lines[forward])
            if value is not None:
                pushes.append(value)
        if not pushes:
            continue
        blocks.append(
            {
                "id": pushes[0],
                "textureIndex": pushes[1] if len(pushes) > 1 else None,
                "field": field,
                "blockClass": descriptor[1:],
            }
        )
    return blocks


def extract_by_spans(lines):
    """Read each store from the span between the previous store and this one.

    The class-walk above misses a store whose construction nests another object, because the walk lands on the
    nested class. Blocks are stored one after another, though, and a block's id is pushed before anything nested
    inside its construction is built (a step sound pushes only strings and floats, a material is a static), so the
    first integer pushed in the span is the id and the second is the texture index. Both readers are kept so the
    two can be compared: an id only one of them sees is worth looking at.
    """
    stores = []
    for index, line in enumerate(lines):
        store = PUTSTATIC_RE.search(line)
        if store:
            stores.append((index, store.group(1), store.group(2)))
    blocks = []
    previous = 0
    for index, field, descriptor in stores:
        pushes = [
            value
            for value in (push_value(lines[position]) for position in range(previous, index))
            if value is not None
        ]
        previous = index
        if not pushes:
            continue
        blocks.append(
            {
                "id": pushes[0],
                "textureIndex": pushes[1] if len(pushes) > 1 else None,
                "field": field,
                "blockClass": descriptor[1:],
            }
        )
    return blocks


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        return 2
    source = sys.argv[1]
    out_file = sys.argv[2] if len(sys.argv) > 2 else "research/indev-20100223-blocks.json"
    with open(source, encoding="utf-8", errors="replace") as handle:
        lines = handle.read().splitlines()

    blocks = extract_by_spans(lines)
    class_walk = extract(lines)
    ordered = sorted(blocks, key=lambda block: (block["id"], block["field"]))
    distinct = sorted({block["id"] for block in blocks})
    classes = sorted({block["blockClass"] for block in blocks})

    print(f"putstatic stores with a parsed id: {len(blocks)} (span reader); class walk sees {len(class_walk)}")
    print(f"distinct block ids: {len(distinct)}")
    print(f"distinct block classes: {len(classes)}")
    print("ids: " + ", ".join(str(value) for value in distinct))
    print("first 10 (id, texture, field, class):")
    for block in ordered[:10]:
        print(f"  {block['id']:>4} {str(block['textureIndex']):>5} {block['field']:<8} {block['blockClass']}")
    print("last 5:")
    for block in ordered[-5:]:
        print(f"  {block['id']:>4} {str(block['textureIndex']):>5} {block['field']:<8} {block['blockClass']}")

    payload = {
        "status": "EXTRACTED FROM ARTIFACT - ids and texture indices read; names still to be mapped from the era id table",
        "source": source,
        "storesParsed": len(blocks),
        "distinctBlockIds": len(distinct),
        "distinctBlockClasses": len(classes),
        "ids": distinct,
        "blocks": ordered,
        "note": "Indev 20100223 stores no block name strings. Names come from the documented id table for the era and must be recorded with their own citation.",
    }
    with open(out_file, "w", encoding="utf-8") as handle:
        json.dump(payload, handle, indent=2)
        handle.write("\n")
    print(f"written: {out_file}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
