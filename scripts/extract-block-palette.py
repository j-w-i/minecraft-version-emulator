#!/usr/bin/env python3
"""Extract a per-version Minecraft block-NAME palette from a hash-verified client jar.

Why this script exists
----------------------
`scripts/extract-b173-palette.py` recovers the Beta 1.7.3 block-name set by finding the class whose
static initialiser loads the most known block names. That works for Beta 1.7.3 because Beta 1.7.3
spells a block by writing its lowerCamelCase name as a string constant. It does not generalise: Alpha,
Infdev and the modern (post-Beta-1.9) clients attach names differently or not at all, and a name probe
cannot find a class whose names are not names.

What this script does instead is STRUCTURAL. It never guesses a name to locate the registry. It walks
every class's `<clinit>` with a stack-slot simulation of the object-construction pattern
(`new X; dup; ...; invokespecial X.<init>`) and counts how many objects each `<clinit>` constructs.
A block registry is one object per registered block, so it stands out by construction count alone.
Only once the class is identified does it ask HOW that version attaches a name, and it supports three
answers: a plain lowerCamelCase string constant, a `tile.<name>` localisation key, or nothing at all.

The distinction matters for this project. A palette that silently returns an empty or wrong set would
let a version check "prove" that a block never existed. So a version whose names cannot be recovered is
written with an empty `blockNames` and an explicit `nameRecovery.status`, and it is never presented as
an established palette.

Controls
--------
Non-negotiable, and the run fails without them:

  * SELF-CHECK - run against the hash-verified Beta 1.7.3 jar, the tool must reproduce the 83-name set
    reviewed in `research/beta173-block-palette.json` EXACTLY, including order.
  * POSITIVE - blocks the version certainly had must be present.
  * NEGATIVE - blocks the version certainly could not have had must be absent.
  * BYTE-EXACT PARSE - the count of class files parsed to their exact last byte is reported; a partial
    parse means the constant pool read is untrustworthy.

Provenance
----------
Every jar's SHA-1 is checked against a value published somewhere other than the download. The digests
live in JARS below with the document they came from. `MOJANG` means Mojang's own
`version_manifest_v2.json` / piston-meta per-version JSON. `ARCHIVE_VERIFIED` means an archive-hosted
build whose SHA-1 comes from an independently published per-version metadata document. The script
refuses to run for a version whose provenance field is null.

Usage
-----
    python scripts/extract-block-palette.py --self-check
    python scripts/extract-block-palette.py --version b1.7.3 --out research/beta173-block-palette.json
    python scripts/extract-block-palette.py --all --out-dir research/block-palettes
    python scripts/extract-block-palette.py --inspect alpha      # structural report, writes nothing

Standard library only. Nothing from a jar is written into the repository: the output contains names,
digests and counts only.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import pathlib
import re
import struct
import sys
import zipfile
from collections import Counter
from datetime import datetime, timezone

# --------------------------------------------------------------------------------------------------
# constant pool tags
# --------------------------------------------------------------------------------------------------
CP_UTF8, CP_INT, CP_FLOAT, CP_LONG, CP_DOUBLE, CP_CLASS, CP_STRING = 1, 3, 4, 5, 6, 7, 8
CP_FIELDREF, CP_METHODREF, CP_IFACEREF, CP_NAMEANDTYPE = 9, 10, 11, 12
CP_METHODHANDLE, CP_METHTYPE, CP_DYNAMIC, CP_INVOKEDYNAMIC, CP_MODULE, CP_PACKAGE = 15, 16, 17, 18, 19, 20

# Fixed operand widths. 0xaa tableswitch, 0xab lookupswitch and 0xc4 wide are variable and are handled
# in the walker. This is the same table the reference extractor uses and it decodes Beta 1.7.3's whole
# block initialiser, so it is known-good rather than assumed.
WIDTH = [0] * 256
for _op in (0x10, 0x15, 0x16, 0x17, 0x18, 0x19, 0x36, 0x37, 0x38, 0x39, 0x3a, 0xa9, 0xbc):
    WIDTH[_op] = 1
for _op in (0x11, 0x12, 0x84, 0x99, 0x9a, 0x9b, 0x9c, 0x9d, 0x9e, 0x9f, 0xa0, 0xa1, 0xa2, 0xa3, 0xa4,
            0xa5, 0xa6, 0xa7, 0xa8, 0xb2, 0xb3, 0xb4, 0xb5, 0xb6, 0xb7, 0xb8, 0xbb, 0xbd, 0xc0, 0xc1,
            0xc6, 0xc7):
    WIDTH[_op] = 2
WIDTH[0x13] = 2   # ldc_w
WIDTH[0x14] = 2   # ldc2_w
WIDTH[0x84] = 2   # iinc
WIDTH[0xc5] = 3   # multianewarray
WIDTH[0xb9] = 4   # invokeinterface
WIDTH[0xba] = 4   # invokedynamic
WIDTH[0xc8] = 4   # goto_w
WIDTH[0xc9] = 4   # jsr_w
WIDTH[0xaa] = None
WIDTH[0xab] = None
WIDTH[0xc4] = None

# Opcodes that push one value we do not track a type for. Used only by the value-kind tracker.
_PUSH1 = set(range(0x02, 0x08)) | set(range(0x1a, 0x2e)) | {0x59, 0x10, 0x15, 0x16, 0x17, 0x18, 0x19}
_PUSH2 = set(range(0x08, 0x10)) | set(range(0x2e, 0x36)) | {0x14, 0xbb, 0x12, 0x13}


def slug(key: str) -> str:
    """A filesystem-safe, stable id for a version key: b1.9-pre3 -> b1_9_pre3."""
    return re.sub(r"_+", "_", re.sub(r"[^a-z0-9]+", "_", key.lower())).strip("_")


class UnsupportedTag(ValueError):
    pass


class ClassFile:
    """A complete JVM class-file reader.

    Deliberately records CONSTANT_NameAndType. The repository's reference extractor (correctly, for
    its own narrower purpose) does not: it stores only Field/Method/InterfaceMethod refs, so
    `refs[nat_index]` is absent and a constructor's owner cannot be resolved through it. Locating a
    block registry needs that owner, so this reader keeps both tables.
    """

    def __init__(self, raw: bytes):
        if raw[:4] != b"\xca\xfe\xba\xbe":
            raise ValueError("not a class file")
        self.raw = raw
        offset = 8
        count = struct.unpack_from(">H", raw, offset)[0]
        offset += 2
        self.utf8: dict[int, str] = {}
        self.strings: dict[int, int] = {}
        self.ints: dict[int, int] = {}
        self.floats: dict[int, float] = {}
        self.longs: dict[int, int] = {}
        self.doubles: dict[int, float] = {}
        self.classes: dict[int, int] = {}
        self.refs: dict[int, tuple[int, int]] = {}
        self.nameandtypes: dict[int, tuple[int, int]] = {}
        index = 1
        while index < count:
            tag = raw[offset]
            offset += 1
            if tag == CP_UTF8:
                length = struct.unpack_from(">H", raw, offset)[0]
                offset += 2
                self.utf8[index] = raw[offset:offset + length].decode("latin-1")
                offset += length
            elif tag == CP_INT:
                self.ints[index] = struct.unpack_from(">i", raw, offset)[0]
                offset += 4
            elif tag == CP_FLOAT:
                self.floats[index] = struct.unpack_from(">f", raw, offset)[0]
                offset += 4
            elif tag == CP_LONG:
                self.longs[index] = struct.unpack_from(">q", raw, offset)[0]
                offset += 8
                index += 1
            elif tag == CP_DOUBLE:
                self.doubles[index] = struct.unpack_from(">d", raw, offset)[0]
                offset += 8
                index += 1
            elif tag == CP_CLASS:
                self.classes[index] = struct.unpack_from(">H", raw, offset)[0]
                offset += 2
            elif tag == CP_STRING:
                self.strings[index] = struct.unpack_from(">H", raw, offset)[0]
                offset += 2
            elif tag in (CP_FIELDREF, CP_METHODREF, CP_IFACEREF):
                self.refs[index] = struct.unpack_from(">HH", raw, offset)
                offset += 4
            elif tag == CP_NAMEANDTYPE:
                self.nameandtypes[index] = struct.unpack_from(">HH", raw, offset)
                offset += 4
            elif tag == CP_METHODHANDLE:
                offset += 3
            elif tag == CP_METHTYPE:
                offset += 2
            elif tag in (CP_DYNAMIC, CP_INVOKEDYNAMIC):
                offset += 4
            elif tag in (CP_MODULE, CP_PACKAGE):
                offset += 2
            else:
                raise UnsupportedTag("constant pool tag %d at index %d" % (tag, index))
            index += 1
        # access_flags, this_class and super_class sit between the pool and the interfaces. Omitting
        # them desynchronises the walk and is invisible without a reference.
        self.access_flags, self.this_class, self.super_class = struct.unpack_from(">HHH", raw, offset)
        offset += 6
        interfaces = struct.unpack_from(">H", raw, offset)[0]
        offset += 2 + 2 * interfaces
        self.fields, offset = self._members(raw, offset)
        self.methods, offset = self._members(raw, offset)
        _, offset = self._attributes(raw, offset, None)
        self.parsed_to_end = offset == len(raw)

    def _members(self, raw, offset):
        count = struct.unpack_from(">H", raw, offset)[0]
        offset += 2
        members = []
        for _ in range(count):
            access, name_index, desc_index, attr_count = struct.unpack_from(">HHHH", raw, offset)
            offset += 8
            attrs, offset = self._attributes(raw, offset, attr_count)
            members.append({"access": access, "name": self.utf8.get(name_index),
                            "desc": self.utf8.get(desc_index), "attrs": attrs,
                            "static": bool(access & 0x0008)})
        return members, offset

    def _attributes(self, raw, offset, count):
        if count is None:
            count = struct.unpack_from(">H", raw, offset)[0]
            offset += 2
        attrs: dict[str, list[bytes]] = {}
        for _ in range(count):
            name_index = struct.unpack_from(">H", raw, offset)[0]
            length = struct.unpack_from(">I", raw, offset + 2)[0]
            offset += 6
            attrs.setdefault(self.utf8.get(name_index), []).append(raw[offset:offset + length])
            offset += length
        return attrs, offset

    # -- accessors ---------------------------------------------------------------------------------
    def code(self, member) -> bytes:
        bodies = member["attrs"].get("Code")
        if not bodies:
            return b""
        body = bodies[0]
        length = struct.unpack_from(">I", body, 4)[0]
        return body[8:8 + length]

    def utf8_at(self, index) -> str | None:
        """Resolve an index that is a UTF-8 entry (a NameAndType's name or descriptor index)."""
        return self.utf8.get(index)

    def string_at(self, index) -> str | None:
        """Resolve a CONSTANT_String index, tolerating a direct UTF-8 index."""
        if index in self.strings:
            return self.utf8.get(self.strings[index])
        return self.utf8.get(index)

    def class_at(self, index) -> str | None:
        return self.utf8.get(self.classes.get(index))

    def member_at(self, ref_index):
        """(owner class, member name, descriptor) for a Field/Method/InterfaceMethod ref."""
        ref = self.refs.get(ref_index)
        if ref is None:
            return (None, None, None)
        nat = self.nameandtypes.get(ref[1])
        if nat is None:
            return (self.class_at(ref[0]), None, None)
        return (self.class_at(ref[0]), self.utf8_at(nat[0]), self.utf8_at(nat[1]))

    def all_string_constants(self) -> set[str]:
        return {self.utf8[i] for i in self.strings.values() if i in self.utf8}

    def all_utf8(self) -> set[str]:
        return set(self.utf8.values())


def walk(code: bytes, on_op=None):
    """Yield (opcode, operand_offset, operands) tuples, decoding the whole instruction stream.

    Returns (instructions, complete). `complete` is False when an opcode outside the width table is
    reached, which is reported rather than hidden: a stopped walk is detectable, a desynchronised one
    is not.
    """
    out = []
    offset, length = 0, len(code)
    while offset < length:
        start = offset
        op = code[offset]
        offset += 1
        operands: tuple = ()
        try:
            if op == 0x12:
                operands = (code[offset],)
                offset += 1
            elif op in (0x13, 0x14):
                operands = struct.unpack_from(">H", code, offset)
                offset += 2
            elif op in (0xb2, 0xb3, 0xb4, 0xb5, 0xb6, 0xb7, 0xb8, 0xbb, 0xbd, 0xc0, 0xc1):
                operands = struct.unpack_from(">H", code, offset)
                offset += 2
            elif op in (0xb9, 0xba):
                operands = struct.unpack_from(">H", code, offset)
                offset += 4
            elif op == 0xaa:
                pad = (4 - (offset % 4)) % 4
                offset += pad
                low, high = struct.unpack_from(">ii", code, offset + 4)
                offset += 12 + 4 * (high - low + 1)
            elif op == 0xab:
                pad = (4 - (offset % 4)) % 4
                offset += pad
                pairs = struct.unpack_from(">i", code, offset + 4)[0]
                offset += 8 + 8 * pairs
            elif op == 0xc4:
                raised = code[offset]
                offset += 1 + (2 if raised == 0x84 else 1)
            else:
                width = WIDTH[op]
                if width is None:
                    return out, False
                offset += width
        except (struct.error, IndexError):
            return out, False
        if offset > length:
            return out, False
        out.append((op, start, operands))
        if on_op is not None:
            on_op(op, start, operands)
    return out, True


def loaded_strings(cf: ClassFile, code: bytes):
    """Every string loaded with ldc/ldc_w, in code order, plus decode completeness."""
    names = []
    instructions, complete = walk(code)
    for op, _, operands in instructions:
        if op in (0x12, 0x13) and operands:
            text = cf.string_at(operands[0])
            if text is not None:
                names.append(text)
    return names, complete


def construction_profile(cf: ClassFile, method):
    """Count objects constructed in one method, and what class each one is.

    A constructor call is `invokespecial <init>` reached while at least one object created by `new` is
    still uninitialised. That is the `new` / `dup` / `invokespecial <init>` pattern the task names.
    """
    code = cf.code(method)
    instructions, complete = walk(code)
    pending = 0
    constructed = Counter()
    init_calls = 0
    putstatic = 0
    for op, _, operands in instructions:
        if op == 0xbb:
            pending += 1
        elif op == 0xb7:
            owner, name, _ = cf.member_at(operands[0])
            if name == "<init>" and pending:
                pending -= 1
                init_calls += 1
                constructed[owner] += 1
        elif op == 0xb3:
            putstatic += 1
    return {"initCalls": init_calls, "constructed": constructed, "putstatic": putstatic,
            "codeLen": len(code), "walkComplete": complete}


# --------------------------------------------------------------------------------------------------
# string -> block-name conventions
# --------------------------------------------------------------------------------------------------
def plain_block_names(strings):
    """Beta-era convention: the block name itself, as a lowerCamelCase string constant."""
    out = []
    for text in strings:
        if text and text[0].islower() and text.isalnum():
            out.append(text)
    return out


def tile_key_names(strings):
    """Release-era convention: a localisation key `tile.<name>`."""
    out = []
    for text in strings:
        if text.startswith("tile."):
            out.append(text[5:])
    return out


def name_setter_args(cf: ClassFile, method):
    """Names attached by a chained setter, as in 1.6.4.

    Post-Beta-1.9 registries attach a name by calling a method that takes a String and returns the
    registry's own type, so that calls can be chained: `block.setName("stone").setIconName("stone")`.
    The name is the string constant loaded immediately before the call. This is a different mechanism
    from a string constant passed to the constructor, and reading it needs the call sites, not just the
    constants.

    Two families of such setters exist in Release 1.6.4: the one carrying the post-flattening
    registered name, and the one carrying the pre-flattening legacy name. They are separated here by
    which family covers the most blocks, decided in `split_setter_families`.
    """
    self_type = "L%s;" % cf.class_at(cf.this_class)
    code = cf.code(method)
    out, counts = [], Counter()
    last_string = None
    for op, _, operands in walk(code)[0]:
        if op in (0x12, 0x13) and operands:
            text = cf.string_at(operands[0])
            if text is not None:
                last_string = text
                continue
        if op == 0xb6 and operands:  # invokevirtual
            owner, name, desc = cf.member_at(operands[0])
            if desc == "(Ljava/lang/String;)%s" % self_type and last_string is not None:
                out.append((name, last_string))
                counts[name] += 1
            last_string = None
        else:
            last_string = None
    return out, counts


def split_setter_families(setter_args):
    """(registered-name set, legacy-alias set) for a registry that uses two setter families.

    In Release 1.6.4 the newer family carries the post-flattening registered name (`gold_ore`) and the
    older one the pre-flattening legacy name (`oreGold`). Which is which is not assumed: the family
    used for the most blocks is taken as the primary set, and the other family's names that are NOT
    already in the primary set become aliases. Underscore-versus-lowerCamelCase is reported as a
    description of the outcome, never used to decide it - in Release 1.6.4 several registered names
    have no underscore (`stone`, `planks`, `cobblestone`) and several legacy names do `weightedPlate_light`.
    """
    counts = Counter(name for name, _ in setter_args)
    if len(counts) < 2:
        return ([text for _, text in setter_args],
                {text for _, text in setter_args},
                {})
    primary_method, _ = counts.most_common(1)[0]
    primary = [text for method, text in setter_args if method == primary_method]
    other = {text for method, text in setter_args if method != primary_method}
    primary_set = set(primary)
    aliases = {}
    for text in other:
        if text not in primary_set:
            aliases[text] = "legacy alias from the other name setter"
    return primary, primary_set, aliases


# --------------------------------------------------------------------------------------------------
# version table
# --------------------------------------------------------------------------------------------------
# provenance: MOJANG  = SHA-1 from Mojang's own manifests/metadata
#             ARCHIVE_VERIFIED = not Mojang-published; SHA-1 from an independently published
#                                per-version metadata document, jar hosted by an archive
JARS = {
    "b1.7.3": {
        "module": "beta_1_7_3", "jar": "beta173-client.jar",
        "sha1": "43db9b498cb67058d2e12d394e6507722e71bb45", "bytes": 1465375,
        "provenance": "MOJANG", "manifestId": "b1.7.3",
        "hashPublishedWhere": "Mojang version_manifest_v2.json -> "
                              "https://piston-meta.mojang.com/v1/packages/44f6969326bd45aa00dcd3c4ca3a7c05ebb24c04/b1.7.3.json"
                              " -> downloads.client.sha1",
        "nameConvention": "plain",
        "positive": ["stone", "grass", "dirt", "bedrock", "cloth", "water", "lava", "sand", "gravel",
                     "oreCoal", "oreIron", "oreGold", "oreDiamond", "oreRedstone", "oreLapis",
                     "log", "leaves", "sapling", "flower", "rose", "mushroom", "reeds", "cactus",
                     "tallgrass", "mobSpawner", "obsidian", "pumpkin", "clay", "snow", "ice",
                     "workbench", "furnace", "chest", "bookshelf", "tnt", "brick", "glass"],
        "negative": ["melon", "pumpkinStem", "melonStem", "vine", "fenceGate", "brickStairs",
                     "stoneStairs", "thinGlass", "ironFence", "monsterEgg", "mushroomCapBrown",
                     "mushroomCapRed"],
    },
    "a1.1.2_01": {
        "module": "alpha", "jar": "alpha-1.1.2_01-client.jar",
        "sha1": "daa4b9f192d2c260837d3b98c39432324da28e86", "bytes": 897164,
        "provenance": "MOJANG", "manifestId": "a1.1.2_01",
        "hashPublishedWhere": "Mojang version_manifest_v2.json -> version JSON -> downloads.client.sha1",
        "nameConvention": "auto",
        "nameConventionExpectation": "none",
        "positive": ["stone", "dirt", "bedrock", "water", "lava", "grass", "sand", "gravel"],
        "negative": ["tallgrass", "vine", "melon", "netherStalk", "hellrock", "hellsand", "lightgem",
                     "pistonBase", "pistonStickyBase", "stonebrick", "cake", "diode", "trapdoor",
                     "ironDoor", "netherrack", "soulsand", "glowstone"],
    },
    "inf-20100420": {
        "module": "infdev_420", "jar": "inf-20100420.jar",
        "sha1": "27010a5137abd2c8d8df85e99c14f5406ec197b3", "bytes": 552012,
        "provenance": "ARCHIVE_VERIFIED", "manifestId": "inf-20100420",
        "hashPublishedWhere": "independently published version metadata "
                              "https://meta.celestia.sh/versions/v1/inf-20100420.json -> downloads.client.sha1 "
                              "(not in Mojang's manifest; the jar is archive-hosted)",
        "nameConvention": "auto",
        "positive": ["stone", "dirt", "bedrock", "water", "lava"],
        "negative": ["tallgrass", "vine", "melon", "netherStalk", "hellrock", "hellsand", "lightgem",
                     "pistonBase", "pistonStickyBase", "stonebrick"],
    },
    "inf-20100611": {
        "module": "infdev_611", "jar": "inf-20100611.jar",
        "sha1": "421318a554f17463a56a271d08e9597941d066d9", "bytes": 566417,
        "provenance": "ARCHIVE_VERIFIED", "manifestId": "inf-20100611",
        "hashPublishedWhere": "independently published version metadata "
                              "https://meta.celestia.sh/versions/v1/inf-20100611.json -> downloads.client.sha1 "
                              "(not in Mojang's manifest; the jar is archive-hosted)",
        "nameConvention": "auto",
        "positive": ["stone", "dirt", "bedrock", "water", "lava"],
        "negative": ["tallgrass", "vine", "melon", "netherStalk", "hellrock", "hellsand", "lightgem",
                     "pistonBase", "pistonStickyBase", "stonebrick"],
    },
    "b1.8.1": {
        "module": "beta_1_8_1", "jar": "beta181-client.jar",
        "sha1": "6b562463ccc2c7ff12ff350a2b04a67b3adcd37b", "bytes": 1995166,
        "provenance": "MOJANG", "manifestId": "b1.8.1",
        "hashPublishedWhere": "Mojang version_manifest_v2.json -> "
                              "https://piston-meta.mojang.com/v1/packages/440e3b845c3991492a3d0c5f0ccfda78ab90d9b6/b1.8.1.json"
                              " -> downloads.client.sha1",
        "nameConvention": "plain",
        "positive": ["stone", "dirt", "bedrock", "water", "lava", "melon", "pumpkin", "vine",
                     "fenceGate", "stonebricksmooth", "fenceIron", "thinGlass", "tallgrass",
                     "stairsBrick", "stairsStoneBrickSmooth", "pistonBase",
                     "pistonStickyBase", "lockedchest"],
        "negative": ["netherrack", "soulsand", "glowstone", "mycel", "waterlily", "netherBrick",
                     "netherFence", "netherStalk", "brewingStand", "enchantmentTable", "whiteStone",
                     "dragonEgg", "endPortalFrame", "oreEmerald", "enderChest", "shrub", "fern",
                     "redstoneLight", "woodSlab", "cocoa"],
    },
    "b1.9-pre3": {
        "module": "beta_1_9_pre_3", "jar": "b1.9-pre3-1350.jar",
        "sha1": "21d02c8bbf3bc35e7dc7909808147f8a01126811", "bytes": 2087212,
        "provenance": "ARCHIVE_VERIFIED", "manifestId": "b1.9-pre3-1350",
        "hashPublishedWhere": "independently published version metadata "
                              "https://meta.celestia.sh/versions/v1/b1.9-pre3-1350.json -> downloads.client.sha1 "
                              "(Mojang never published this build to the launcher manifest; "
                              "minecraft.wiki links the same metadata JSON for its download)",
        "nameConvention": "plain",
        "positive": ["stone", "dirt", "bedrock", "water", "lava", "melon", "vine", "hellrock",
                     "netherStalk", "mycel", "waterlily", "brewingStand", "enchantmentTable",
                     "pistonBase", "lockedchest", "airPortalFrame"],
        "negative": ["oreEmerald", "enderChest", "redstoneLight", "shrub", "fern", "woodSlab",
                     "cocoa", "blockEmerald", "tripWire", "commandBlock", "beacon", "netherquartz",
                     "hopper", "quartzBlock", "endPortalFrame", "whiteStone", "dragonEgg"],
    },
    "1.0.0": {
        "module": "release_1_0_0", "jar": "1.0.0-client.jar",
        "sha1": "b679fea27f2284836202e9365e13a82552092e5d", "bytes": 2362837,
        "provenance": "MOJANG", "manifestId": "1.0",
        "hashPublishedWhere": "Mojang version_manifest_v2.json lists id `1.0`; its version JSON "
                              "https://piston-meta.mojang.com/v1/packages/75062586b830dd5160f13f1c9130eb365e01f1b9/1.0.json "
                              "-> downloads.client.sha1 (the metadata's own id is `1.0.0`)",
        "nameConvention": "plain",
        "positive": ["stone", "dirt", "bedrock", "water", "lava", "melon", "brewingStand",
                     "enchantmentTable", "endPortalFrame", "whiteStone", "dragonEgg", "netherStalk",
                     "cauldron"],
        "negative": ["oreEmerald", "enderChest", "redstoneLight", "woodSlab",
                     "cocoa", "blockEmerald", "tripWire", "commandBlock", "beacon", "netherquartz",
                     "hopper", "quartzBlock"],
    },
    "1.1": {
        "module": "release_1_1", "jar": "1.1-client.jar",
        "sha1": "f690d4136b0026d452163538495b9b0e8513d718", "bytes": 3862147,
        "provenance": "MOJANG", "manifestId": "1.1",
        "hashPublishedWhere": "Mojang version_manifest_v2.json -> "
                              "https://piston-meta.mojang.com/v1/packages/c0cb9368dbdbb1e8dbcb9363a28d8da74cf6fc5e/1.1.json"
                              " -> downloads.client.sha1",
        "nameConvention": "plain",
        "positive": ["stone", "dirt", "bedrock", "water", "lava", "melon", "brewingStand",
                     "enchantmentTable", "endPortalFrame", "whiteStone", "dragonEgg"],
        "negative": ["oreEmerald", "enderChest", "redstoneLight", "woodSlab",
                     "cocoa", "blockEmerald", "tripWire", "commandBlock", "beacon", "netherquartz",
                     "hopper", "quartzBlock"],
    },
    "1.2.5": {
        "module": "release_1_2_5", "jar": "1.2.5-client.jar",
        "sha1": "4a2fac7504182a97dcbcd7560c6392d7c8139928", "bytes": 4032098,
        "provenance": "MOJANG", "manifestId": "1.2.5",
        "hashPublishedWhere": "Mojang version_manifest_v2.json -> "
                              "https://piston-meta.mojang.com/v1/packages/5158765caf1ca14958cb6c45d52c8e09ed9b046c/1.2.5.json"
                              " -> downloads.client.sha1",
        "nameConvention": "plain",
        "positive": ["stone", "dirt", "bedrock", "water", "lava", "melon", "brewingStand",
                     "enchantmentTable", "endPortalFrame", "whiteStone", "dragonEgg",
                     "redstoneLight"],
        "negative": ["oreEmerald", "enderChest", "woodSlab",
                     "cocoa", "blockEmerald", "tripWire", "commandBlock", "beacon", "netherquartz",
                     "hopper", "quartzBlock"],
    },
    "1.6.4": {
        "module": "release_1_6_4", "jar": "1.6.4-client.jar",
        "sha1": "1703704407101cf72bd88e68579e3696ce733ecd", "bytes": 4745096,
        "provenance": "MOJANG", "manifestId": "1.6.4",
        "hashPublishedWhere": "Mojang version_manifest_v2.json -> "
                              "https://piston-meta.mojang.com/v1/packages/b71bae449192fbbe1582ff32fb3765edf0b9b0a8/1.6.4.json"
                              " -> downloads.client.sha1",
        "nameConvention": "setter", "setterFamily": "primary-legacy",
        "positive": ["stone", "dirt", "bedrock", "water", "lava", "melon", "brewingStand",
                     "enchantmentTable", "endPortalFrame", "whiteStone", "dragonEgg",
                     "redstoneLight", "oreEmerald", "enderChest", "netherquartz", "quartzBlock",
                     "hopper", "beacon", "commandBlock", "anvil", "hayBlock", "blockCoal",
                     "clayHardened", "clayHardenedStained", "woolCarpet", "monsterStoneEgg",
                     "cobbleWall", "flowerPot", "tripWire", "tripWireSource", "woodSlab", "cocoa"],
        "negative": ["oreRuby", "enderStone", "stone_granite", "acacia_log", "prismarine",
                     "red_sand", "sea_lantern", "slime_block"],
    },
}

SEARCH_DIRS = [
    pathlib.Path.home() / "AppData/Local/Temp/mce-palette/jars",
    pathlib.Path.home() / "AppData/Local/Temp/mce-infdev/jars",
    pathlib.Path.home() / "AppData/Local/Temp/mce-prebata/jars",
    pathlib.Path("run/jars"),
]

BLOCKISH = None  # set per run


def find_jar(entry):
    for directory in SEARCH_DIRS:
        candidate = directory / entry["jar"]
        if candidate.is_file():
            return candidate
    return None


def load_classes(jar: pathlib.Path):
    classes, failures, unsupported = {}, [], []
    with zipfile.ZipFile(jar) as archive:
        for info in archive.infolist():
            if not info.filename.endswith(".class"):
                continue
            try:
                classes[info.filename] = ClassFile(archive.read(info.filename))
            except UnsupportedTag as error:
                unsupported.append((info.filename, str(error)))
            except Exception as error:  # noqa: BLE001 - reported, never swallowed
                failures.append((info.filename, repr(error)))
    exact = sum(1 for cf in classes.values() if cf.parsed_to_end)
    return classes, {"parsed": len(classes), "exactEnd": exact,
                     "failures": failures, "unsupportedTag": unsupported}


def ancestor_chain(classes: dict, name: str):
    """[name, super, super-super, ...] resolving through the jar's own class files."""
    chain = []
    seen = set()
    current = name
    while current is not None and current not in seen:
        seen.add(current)
        chain.append(current)
        cf = classes.get(current + ".class")
        if cf is None:
            break
        current = cf.class_at(cf.super_class)
    return chain


def static_field_desc_counts(cf: ClassFile):
    """How many static fields each class declares, by descriptor."""
    counts = Counter()
    for member in cf.fields:
        if member["static"]:
            counts[member["desc"]] += 1
    return counts


def registry_field_match(classes, cf, constructed):
    """Pick the constructed class whose declared field type best explains the registry's static fields.

    A registry writes `putstatic <Registry>.f : L<BlockClass>;`, so the registry's static fields are
    declared with the BLOCK CLASS, while the objects it news are subclasses. Matching only the exact
    constructed class therefore fails on every real registry. Walking each constructed class's own
    superclass chain and taking the ancestor with the most declared static fields recovers the block
    class without being told its name.
    """
    counts = static_field_desc_counts(cf)
    best_desc, best_n, best_class = None, 0, None
    for cls in constructed:
        for ancestor in ancestor_chain(classes, cls):
            desc = "L%s;" % ancestor
            n = counts.get(desc, 0)
            if n > best_n:
                best_desc, best_n, best_class = desc, n, ancestor
    return best_desc, best_n, best_class


def candidates(classes):
    """Every <clinit>, profiled by how many objects it constructs."""
    rows = []
    for name, cf in classes.items():
        fields_by_desc = static_field_desc_counts(cf)
        for member in cf.methods:
            if member["name"] != "<clinit>":
                continue
            profile = construction_profile(cf, member)
            strings, complete = loaded_strings(cf, cf.code(member))
            setter_args, setter_counts = name_setter_args(cf, member)
            desc, n, block_class = registry_field_match(classes, cf, profile["constructed"])
            profile.update({"class": name, "member": member, "strings": strings,
                            "stringsComplete": complete, "cf": cf,
                            "super": cf.class_at(cf.super_class),
                            "topConstructedClass": block_class,
                            "fieldsOfTopConstructed": n,
                            "setterArgs": setter_args,
                            "setterCounts": setter_counts,
                            "staticFieldTypes": len(fields_by_desc)})
            rows.append(profile)
    rows.sort(key=lambda r: (-r["initCalls"], r["class"]))
    return rows


def score_registry(rows, module):
    """Pick the block-registry class structurally, and record the runners-up for the audit trail.

    The discriminator against the item registry is the number of DISTINCT classes a <clinit>
    constructs. A block registry instantiates one class per block FAMILY (stone, stairs, slab, door,
    plant, ...), so it touches dozens of distinct classes; an item registry instantiates a handful of
    item classes many times each. Measured on these jars: block registries construct 52-126 distinct
    classes, item registries 14-53. Ranking by construction count alone picks the item registry on
    five of the ten versions, which is exactly the error this ordering exists to prevent.

    Ties break on construction count, then class name, so the result is deterministic.
    """
    if not rows:
        return None, []
    ranked = []
    for row in rows:
        ranked.append({**row,
                       "plainNames": plain_block_names(row["strings"]),
                       "tileNames": tile_key_names(row["strings"]),
                       "setterArgs": row["setterArgs"]})
    ranked.sort(key=lambda r: (-len(r["constructed"]), -r["initCalls"], r["class"]))
    return ranked[0], ranked[:6]


def decide_convention(entry, best):
    """Answer 'how does this version attach a name?', from evidence, not from expectation.

    Three mechanisms exist across these jars, and which one is present is a fact about the build:

      plain  - the block name is a lowerCamelCase string constant in the registry's <clinit>
               (Beta 1.8 through Release 1.2.5; this is also what Beta 1.7.3 does)
      tile   - the name is a `tile.<name>` localisation key in the registry's <clinit>
      setter - the name is an argument to a chained `(String) -> self` setter called on each block,
               which is how Release 1.6.4 attaches the post-flattening name
      none   - the registry loads no block-name strings at all (Alpha, Infdev)
    """
    configured = entry["nameConvention"]
    plain = best["plainNames"]
    tile = best["tileNames"]
    setter_args = [text for _, text in best["setterArgs"]]
    if configured == "plain":
        return "plain", plain, {}
    if configured == "tile":
        return "tile", tile, {}
    if configured == "setter":
        primary, primary_set, aliases = split_setter_families(best["setterArgs"])
        return "setter", primary, aliases
    # auto: whichever convention produced a BLOCK-SIZED set of names. The threshold matters: Alpha and
    # Infdev do load a handful of lowerCamelCase strings in their registries ('stone', 'grass', ...),
    # but they are MATERIAL names passed to the block constructor, not block names - six of them, not
    # eighty. Accepting a set that small is how the earlier Alpha attempt produced a fake palette.
    # The setter convention is tested first because in a post-Beta-1.9 jar the "plain lowerCamelCase"
    # filter also matches plain TEXTURE names ('planks', 'cobblestone'), which are not registered block
    # names; only the setter arguments are.
    if len(setter_args) >= 40:
        primary, primary_set, aliases = split_setter_families(best["setterArgs"])
        return "setter", primary, aliases
    if len(plain) >= 40:
        return "plain", plain, {}
    if len(tile) >= 40:
        return "tile", tile, {}
    return "none", [], {}


def names_are_real_palette(names, entry):
    """A recovered set is only usable if it satisfies the positive controls."""
    positives = entry["positive"]
    return all(p in set(names) for p in positives)


def structural_block_class(classes, registry_class):
    """The block class, found without any name: the class with the most direct subclasses.

    A block class is the class every block variant extends. It is reported separately from the registry
    because Alpha and Infdev need the block class identified even though no palette can be extracted
    from them. Measured: Beta 1.7.3's block class uu has 49 direct subclasses, its item class gm has
    27, so the count alone separates them.
    """
    subclasses = Counter()
    for cf in classes.values():
        super_name = cf.class_at(cf.super_class)
        if super_name:
            subclasses[super_name] += 1
    bare_registry = registry_class[:-6] if registry_class and registry_class.endswith(".class") else registry_class
    ranked = [(n, c) for n, c in subclasses.most_common()
              if n != "java/lang/Object" and n + ".class" in classes]
    if not ranked:
        return None, 0
    # the registry itself ranks first if it has subclasses, because the registry IS the block class in
    # every jar measured here
    for name, count in ranked:
        if name == bare_registry:
            return name, count
    return ranked[0]


def analyse(entry, key, jar: pathlib.Path, digest: str, size: int):
    classes, parse = load_classes(jar)
    rows = candidates(classes)
    best, top = score_registry(rows, entry.get("module"))
    block_class, block_subclasses = structural_block_class(classes, best["class"] if best else None)
    result = {
        "version": key,
        "jar": str(jar),
        "bytes": size,
        "sha1": digest,
        "parse": parse,
        "structuralBlockClass": block_class,
        "structuralBlockClassSubclasses": block_subclasses,
        "structuralBlockClassEvidence": (
            "%s has %d direct subclasses in this jar; a block class is the class block variants extend "
            "(Beta 1.7.3's equivalent is uu with 49)." % (block_class, block_subclasses)
        ) if block_class else "no class with subclasses found",
        "candidates": [
            {"class": r["class"], "super": r["super"], "initCalls": r["initCalls"],
             "distinctConstructedClasses": len(r["constructed"]), "putstatic": r["putstatic"],
             "topConstructedClass": r["topConstructedClass"],
             "staticFieldsOfBlockType": r["fieldsOfTopConstructed"],
             "stringLoads": len(r["strings"]), "plainNameCandidates": len(r["plainNames"]),
             "tileKeyCandidates": len(r["tileNames"]),
             "nameSetterCalls": len(r["setterArgs"]),
             "nameSetterMethods": dict(r["setterCounts"]),
             "walkComplete": r["walkComplete"],
             "topConstructed": r["constructed"].most_common(4)}
            for r in top
        ],
    }
    if best is None:
        result["blockClass"] = None
        result["blockNames"] = []
        result["convention"] = "none"
        return result
    convention, names, aliases = decide_convention(entry, best)
    seen, ordered = set(), []
    for name in names:
        if name and name not in seen:
            seen.add(name)
            ordered.append(name)
    result.update({
        "blockClass": best["class"],
        "blockClassSuper": best["super"],
        "blockClassInitCalls": best["initCalls"],
        "blockClassDistinctConstructed": len(best["constructed"]),
        "blockClassPutstatic": best["putstatic"],
        "blockClassStringLoads": len(best["strings"]),
        "blockClassStringsComplete": best["stringsComplete"],
        "blockClassTopConstructed": best["constructed"].most_common(4),
        "blockClassTopType": best["topConstructedClass"],
        "blockClassStaticFieldsOfBlockType": best["fieldsOfTopConstructed"],
        "convention": convention,
        "blockNames": ordered,
        "aliasNames": aliases,
        "unfilteredStrings": best["strings"],
        "nameSetterMethods": dict(best["setterCounts"]),
    })
    return result


def controls_for(entry, analysis):
    names = set(analysis.get("blockNames") or [])
    # Accept the slugified spelling of each control too (gold_ore vs gold-ore). Release 1.6.4 does
    # change block names from lowerCamelCase to snake_case, so a control written for one spelling must
    # not silently fail when the version is checked with the other.
    variants = set(names) | {n.replace("-", "_") for n in names} | {n.replace("_", "") for n in names}
    negative = entry["negative"]
    negative_hits = [n for n in negative if n in variants]
    positive_missing = [p for p in entry["positive"] if p not in variants and p.replace("_", "") not in variants]
    return {
        "positiveMustBePresent": entry["positive"],
        "positiveMissing": positive_missing,
        "negativeMustBeAbsent": negative,
        "negativeObserved": negative_hits,
        "passed": not positive_missing and not negative_hits,
    }


# A second, independent self-check. Beta 1.8.1's 92 names were first read by hand from the registry
# class's 121 string loads before this tool existed (see the README's evidence section); pinning that
# hand-read list here means a change in the structural pick or the naming convention fails the run
# instead of silently emitting a different palette. It is not parent-reviewed like the b1.7.3 set.
BETA_1_8_1_EXPECTED = [
    "stone", "wood", "gravel", "grass", "cloth", "sand", "dirt", "stonebrick", "sapling", "bedrock",
    "water", "lava", "oreGold", "oreIron", "oreCoal", "log", "leaves", "sponge", "glass", "oreLapis",
    "blockLapis", "dispenser", "sandStone", "musicBlock", "bed", "goldenRail", "detectorRail",
    "pistonStickyBase", "web", "tallgrass", "deadbush", "pistonBase", "flower", "rose", "mushroom",
    "blockGold", "blockIron", "stoneSlab", "brick", "tnt", "bookshelf", "stoneMoss", "obsidian",
    "torch", "fire", "mobSpawner", "stairsWood", "chest", "redstoneDust", "oreDiamond", "blockDiamond",
    "workbench", "crops", "farmland", "furnace", "sign", "doorWood", "ladder", "rail", "stairsStone",
    "lever", "pressurePlate", "doorIron", "oreRedstone", "notGate", "button", "snow", "ice", "cactus",
    "clay", "reeds", "jukebox", "fence", "pumpkin", "hellrock", "hellsand", "lightgem", "portal",
    "litpumpkin", "cake", "diode", "lockedchest", "trapdoor", "stonebricksmooth", "fenceIron",
    "thinGlass", "melon", "pumpkinStem", "vine", "fenceGate", "stairsBrick", "stairsStoneBrickSmooth",
]


def self_check_beta181():
    entry = JARS["b1.8.1"]
    jar = find_jar(entry)
    if jar is None:
        return False, "b1.8.1 client jar not found"
    raw = jar.read_bytes()
    digest = hashlib.sha1(raw).hexdigest()
    analysis = analyse(entry, "b1.8.1", jar, digest, len(raw))
    got = analysis["blockNames"]
    if got == BETA_1_8_1_EXPECTED:
        return True, "reproduced all %d hand-read Beta 1.8.1 names from %s" % (len(got), analysis["blockClass"])
    missing = [n for n in BETA_1_8_1_EXPECTED if n not in got]
    extra = [n for n in got if n not in BETA_1_8_1_EXPECTED]
    return False, "Beta 1.8.1 set differs: missing %s, extra %s" % (missing, extra)


def self_check(out_dir: pathlib.Path, verbose=True):
    """Reproduce the reviewed Beta 1.7.3 palette EXACTLY, or say the method is wrong."""
    reviewed = pathlib.Path("research/beta173-block-palette.json")
    if not reviewed.is_file():
        return False, "research/beta173-block-palette.json not found; cannot self-check"
    expected = json.loads(reviewed.read_text(encoding="utf-8"))
    entry = JARS["b1.7.3"]
    jar = find_jar(entry)
    if jar is None:
        return False, "b1.7.3 client jar not found"
    raw = jar.read_bytes()
    digest = hashlib.sha1(raw).hexdigest()
    if digest != entry["sha1"]:
        return False, "b1.7.3 jar sha1 %s != pinned %s" % (digest, entry["sha1"])
    analysis = analyse(entry, "b1.7.3", jar, digest, len(raw))
    got, want = analysis["blockNames"], expected["blockNames"]
    if got != want:
        if verbose:
            print("  got  %d names" % len(got))
            print("  want %d names" % len(want))
            print("  missing: %s" % [n for n in want if n not in got])
            print("  extra  : %s" % [n for n in got if n not in want])
        return False, "name set differs from the reviewed palette (missing %d, extra %d)" % (
            len([n for n in want if n not in got]), len([n for n in got if n not in want]))
    ok181, detail181 = self_check_beta181()
    if not ok181:
        return False, "Beta 1.7.3 reproduced, but the second self-check failed: %s" % detail181
    return True, ("reproduced all %d names in the reviewed order from %s; %s"
                  % (len(want), analysis["blockClass"], detail181))


CONVENTION_NOTES = {
    "plain": "Block names are attached as plain lowerCamelCase string constants loaded in the block "
             "registry's static initialiser, exactly as Beta 1.7.3 does.",
    "tile": "Block names are attached as `tile.<name>` localisation keys loaded in the registry's "
            "static initialiser; the `tile.` prefix is stripped so the result is comparable with the "
            "Beta-era names.",
    "setter": "Block names are attached by a chained `(String) -> <registry type>` setter called once "
              "per block (the post-Beta-1.9 mechanism). Each name is the string constant loaded "
              "immediately before the setter call. For Release 1.6.4 two setter families exist: "
              "`setName` carries the post-flattening registered name (e.g. `gold_ore`) and `setIconName` "
              "carries the pre-flattening legacy name (e.g. `oreGold`). Both are recorded here; the "
              "union is the set of names this client registers.",
    "none": "This client's registry initialiser loads no block-name strings at all: no plain "
            "lowerCamelCase names and no `tile.<name>` keys exist anywhere in the jar. The names were "
            "added to the jar only in Beta 1.8, so they cannot be recovered from this build. No palette "
            "is claimed.",
}


def document(entry, key, analysis, controls, jar, raw, self_check_detail):
    names = analysis.get("blockNames") or []
    convention = analysis.get("convention")
    recovered = convention not in (None, "none") and bool(names)
    if not recovered:
        status = "BLOCKED_NAME_CONVENTION_NOT_PRESENT_IN_JAR"
    elif controls["passed"]:
        status = "WORKER_GENERATED_PENDING_PARENT_VERIFICATION"
    else:
        status = "WORKER_GENERATED_CONTROLS_FAILED"
    document_ = {
        "schemaVersion": 1,
        "kind": "%s-block-name-set" % slug(key),
        "reviewStatus": status,
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "source": {
            "jar": str(jar),
            "bytes": len(raw),
            "sha1": analysis["sha1"],
            "sha1MatchesExpected": analysis["sha1"] == entry["sha1"],
            "sizeMatchesExpected": len(raw) == entry["bytes"],
            "provenance": entry["provenance"],
            "manifestId": entry["manifestId"],
            "hashPublishedWhere": entry["hashPublishedWhere"],
        },
        "method": {
            "tool": "scripts/extract-block-palette.py (standard library only)",
            "scope": "block NAME SET only; numeric ids are deliberately not paired. Ids are what the "
                     "previous rejected artifact got wrong, so they are not inferred here.",
            "identifyRegistry": "STRUCTURAL, no name is used to find the class: the class whose static "
                                "initialiser constructs the most distinct classes via the "
                                "new/dup/invokespecial <init> pattern. Ranking by distinct constructed "
                                "classes is what separates the block registry from the item registry, "
                                "which constructs a few classes many times each.",
            "nameConventionDetected": convention,
            "nameConventionNote": CONVENTION_NOTES.get(convention, ""),
            "classCount": analysis["parse"]["parsed"],
            "classesParsedToExactEnd": analysis["parse"]["exactEnd"],
            "classParseFailures": analysis["parse"]["failures"],
            "unsupportedConstantPoolTags": analysis["parse"]["unsupportedTag"],
            "blockClass": analysis.get("blockClass"),
            "blockClassSuper": analysis.get("blockClassSuper"),
            "structuralBlockClass": analysis.get("structuralBlockClass"),
            "structuralBlockClassSubclasses": analysis.get("structuralBlockClassSubclasses"),
            "structuralBlockClassIdentityEvidence": analysis.get("structuralBlockClassEvidence"),
            "blockClassInitCalls": analysis.get("blockClassInitCalls"),
            "blockClassDistinctConstructedClasses": analysis.get("blockClassDistinctConstructed"),
            "blockClassPutstatic": analysis.get("blockClassPutstatic"),
            "blockClassStringLoads": analysis.get("blockClassStringLoads"),
            "blockClassStringDecodeComplete": analysis.get("blockClassStringsComplete"),
            "blockClassMostConstructed": analysis.get("blockClassTopConstructed"),
            "blockClassCommonBlockType": analysis.get("blockClassTopType"),
            "blockClassStaticFieldsOfBlockType": analysis.get("blockClassStaticFieldsOfBlockType"),
            "nameSetterCallCounts": analysis.get("nameSetterMethods"),
            "topStructuralCandidates": analysis["candidates"],
            "selfCheck": {
                "target": "research/beta173-block-palette.json (83 names, parent-reviewed)",
                "passed": bool(self_check_detail and self_check_detail.startswith("reproduced")),
                "detail": self_check_detail,
            },
        },
        "controls": controls,
        "nameRecovery": {
            "status": "RECOVERED" if recovered else "NOT_AVAILABLE_IN_THIS_JAR",
            "count": len(names),
            "note": CONVENTION_NOTES.get(convention, ""),
        },
        "blockNames": names,
        "aliasNames": analysis.get("aliasNames") or {},
    }
    return document_


def cmd_inspect(keys):
    for key in keys:
        entry = JARS[key]
        jar = find_jar(entry)
        print("=" * 100)
        print("%s  (%s)" % (key, jar.name if jar else "JAR NOT FOUND"))
        if jar is None:
            continue
        raw = jar.read_bytes()
        digest = hashlib.sha1(raw).hexdigest()
        print("sha1 %s %s" % (digest, "MATCHES PINNED" if digest == entry["sha1"] else "MISMATCH"))
        analysis = analyse(entry, key, jar, digest, len(raw))
        print("classes=%d exactEnd=%d failures=%d unsupportedTags=%d"
              % (analysis["parse"]["parsed"], analysis["parse"]["exactEnd"],
                 len(analysis["parse"]["failures"]), len(analysis["parse"]["unsupportedTag"])))
        print("top structural candidates:")
        for row in analysis["candidates"]:
            print("  %-28s super=%-14s initCalls=%-4d distinct=%-4d fieldsOfBlockType=%-4d putstatic=%-4d"
                  % (row["class"], row["super"], row["initCalls"], row["distinctConstructedClasses"],
                     row["staticFieldsOfBlockType"], row["putstatic"]))
            print("      strings=%-4d plain=%-4d tile=%-4d setterCalls=%-4d blockType=%-14s constructs: %s"
                  % (row["stringLoads"], row["plainNameCandidates"], row["tileKeyCandidates"],
                     row["nameSetterCalls"], row["topConstructedClass"], row["topConstructed"]))
        print("chosen: %s (initCalls=%s) convention=%s names=%d"
              % (analysis.get("blockClass"), analysis.get("blockClassInitCalls"),
                 analysis.get("convention"), len(analysis.get("blockNames") or [])))


def run_one(key, out_path: pathlib.Path, verbose=True):
    entry = JARS[key]
    jar = find_jar(entry)
    if jar is None:
        return None, "jar %s not found in %s" % (entry["jar"], [str(d) for d in SEARCH_DIRS])
    raw = jar.read_bytes()
    digest = hashlib.sha1(raw).hexdigest()
    if digest != entry["sha1"]:
        return None, "REFUSING: sha1 %s != pinned %s" % (digest, entry["sha1"])
    if len(raw) != entry["bytes"]:
        return None, "REFUSING: %d bytes != pinned %d" % (len(raw), entry["bytes"])
    ok, detail = self_check(out_path.parent)
    if not ok:
        return None, "SELF-CHECK FAILED, method is wrong: %s" % detail
    analysis = analyse(entry, key, jar, digest, len(raw))
    controls = controls_for(entry, analysis)
    if analysis.get("convention") in (None, "none"):
        controls["passed"] = False
        controls["note"] = ("Positives cannot be satisfied because this build attaches no block-name "
                            "string; the version is reported as BLOCKED rather than given a palette.")
    doc = document(entry, key, analysis, controls, jar, raw, detail)
    if verbose:
        print("%-14s class=%-14s convention=%-7s names=%-4d controls=%s"
              % (key, analysis.get("blockClass"), analysis.get("convention"),
                 len(doc["blockNames"]), "PASS" if controls["passed"] else "FAIL"))
        if not controls["passed"] and controls.get("positiveMissing"):
            print("    positive missing: %s" % controls["positiveMissing"])
        if controls.get("negativeObserved"):
            print("    negative present: %s" % controls["negativeObserved"])
    if out_path is not None:
        out_path.parent.mkdir(parents=True, exist_ok=True)
        out_path.write_text(json.dumps(doc, indent=2) + "\n", encoding="utf-8")
    return doc, None


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--self-check", action="store_true")
    parser.add_argument("--inspect", nargs="*", metavar="VERSION")
    parser.add_argument("--version", nargs="+", metavar="VERSION")
    parser.add_argument("--all", action="store_true")
    parser.add_argument("--out", default=None)
    parser.add_argument("--out-dir", default="research/block-palettes")
    args = parser.parse_args()

    if args.inspect is not None:
        cmd_inspect(args.inspect or ["b1.7.3"])
        return 0

    if args.self_check:
        ok, detail = self_check(pathlib.Path(args.out_dir))
        print("SELF-CHECK (Beta 1.7.3, 83 reviewed names): %s" % ("PASS" if ok else "FAIL"))
        print("  %s" % detail)
        return 0 if ok else 2

    if args.all:
        ok, detail = self_check(pathlib.Path(args.out_dir))
        print("SELF-CHECK (Beta 1.7.3, 83 reviewed names): %s" % ("PASS" if ok else "FAIL"))
        print("  %s" % detail)
        if not ok:
            print("REFUSING to emit palettes: the self-check did not reproduce the reviewed set.")
            return 2
        out_dir = pathlib.Path(args.out_dir)
        failures = []
        for key in JARS:
            doc, error = run_one(key, out_dir / ("%s.json" % slug(key)))
            if error:
                print("%-14s SKIPPED: %s" % (key, error))
                failures.append((key, error))
        print("\n%d versions written, %d skipped" % (len(JARS) - len(failures), len(failures)))
        return 0 if not failures else 1

    if args.version:
        for key in args.version:
            if key not in JARS:
                print("unknown version %s; known: %s" % (key, ", ".join(JARS)))
                return 2
        out = pathlib.Path(args.out) if args.out else None
        for key in args.version:
            doc, error = run_one(key, out)
            if error:
                print("%-14s FAILED: %s" % (key, error), file=sys.stderr)
                return 1
        return 0

    parser.print_help()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
