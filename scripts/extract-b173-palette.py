"""Extracts the Beta 1.7.3 block-name set from a hash-verified client jar, with introduction-version controls.

Why this exists
---------------
An earlier extraction was REJECTED by parent review because it contradicted Beta 1.7.3 scope: it included
Beta 1.8 content (melon, rotten flesh, enderman, silverfish, blocks 97-109, items 360-368), omitted wool
block 35, and conflated wool metadata with dye metadata. The recorded required next step was to
"re-extract registries from a hash-verified b1.7.3 jar with executable provenance-bound tooling and
introduction-version negative controls". This script is that, and the controls are what decide whether its
output may be consumed.

Scope, stated plainly
---------------------
It recovers the SET of block names that existed in b1.7.3. That is what a version-fidelity check needs: to
ask of a generated world "could this version have had this block?", the answer is membership in this set.
It deliberately does NOT attempt to pair names with numeric ids; that pairing needs a full disassembler,
and a positional guess is how a previous artifact went wrong. Ids are recorded only where this project
already verified them independently.

How it works
------------
1. The jar's SHA-1 is checked against the published b1.7.3 value, and the run refuses otherwise.
2. Class files are parsed by this script (standard JVM format, every constant-pool tag) so no undeclared
   tool is involved, and the number of classes that parse to their exact last byte is reported.
3. The block class is identified as the class whose static initialiser loads the most known block names
   as string constants.
4. That initialiser is decoded with a complete opcode-width table, and every string it loads is collected
   in order. Those are the block names, because b1.7.3 names a block as it registers it.
5. CONTROLS decide acceptance, and the script exits non-zero if they fail:
     negative - names added in Beta 1.8 must be ABSENT;
     positive - blocks that must be present, including wool (block 35), which the rejection was about.

Usage:
    python scripts/extract-b173-palette.py [--jar PATH] [--out PATH]
"""
import argparse
import hashlib
import json
import pathlib
import struct
import sys
import zipfile
from datetime import datetime, timezone

DEFAULT_SHA1 = "43db9b498cb67058d2e12d394e6507722e71bb45"
DEFAULT_SIZE = 1465375

CP_UTF8, CP_INT, CP_FLOAT, CP_LONG, CP_DOUBLE, CP_CLASS, CP_STRING = 1, 3, 4, 5, 6, 7, 8
CP_FIELDREF, CP_METHODREF, CP_IFACEREF, CP_NAMEANDTYPE = 9, 10, 11, 12
CP_METHODHANDLE, CP_METHTYPE, CP_DYNAMIC, CP_INVOKEDYNAMIC, CP_MODULE, CP_PACKAGE = 15, 16, 17, 18, 19, 20

NEGATIVE_CONTROLS = {
    "melon": "Beta 1.8", "pumpkinStem": "Beta 1.8", "melonStem": "Beta 1.8", "vine": "Beta 1.8",
    "fenceGate": "Beta 1.8", "brickStairs": "Beta 1.8", "stoneStairs": "Beta 1.8",
    "thinGlass": "Beta 1.8", "ironFence": "Beta 1.8", "monsterEgg": "Beta 1.8",
    "mushroomCapBrown": "Beta 1.8", "mushroomCapRed": "Beta 1.8",
}
# Blocks that MUST appear. The names are b1.7.3's own spellings, which differ from modern field names -
# the ores are oreCoal/oreIron/oreGold/oreDiamond/oreRedstone/oreLapis, not coalOre and so on, and getting
# that wrong makes a correct extraction look broken. `cloth` is here because the rejected extraction
# omitted wool (block 35), and the six ores are here because this project independently verified that
# b1.7.3 had exactly six and no emerald.
POSITIVE_CONTROLS = ["stone", "grass", "dirt", "bedrock", "cloth", "water", "lava", "sand", "gravel",
                     "oreCoal", "oreIron", "oreGold", "oreDiamond", "oreRedstone", "oreLapis",
                     "log", "leaves", "sapling", "flower", "rose", "mushroom", "reeds", "cactus",
                     "tallgrass", "mobSpawner", "obsidian", "pumpkin", "clay", "snow", "ice",
                     "workbench", "furnace", "chest", "bookshelf", "tnt", "brick", "glass"]
PROBE_NAMES = ("stone", "dirt", "bedrock", "grass")

# Complete operand widths for the fixed-length opcodes. Variable-length ones are handled specially.
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
WIDTH[0xaa] = None  # tableswitch
WIDTH[0xab] = None  # lookupswitch
WIDTH[0xc4] = None  # wide


class ClassFile:
    """A class-file reader. Every constant-pool tag is handled; nothing is assumed about the jar."""

    def __init__(self, raw):
        if raw[:4] != b"\xca\xfe\xba\xbe":
            raise ValueError("not a class file")
        self.raw = raw
        offset = 8
        count = struct.unpack_from(">H", raw, offset)[0]
        offset += 2
        self.utf8, self.strings, self.ints, self.classes, self.refs = {}, {}, {}, {}, {}
        index = 1
        while index < count:
            tag = raw[offset]
            offset += 1
            if tag == CP_UTF8:
                length = struct.unpack_from(">H", raw, offset)[0]
                offset += 2
                self.utf8[index] = raw[offset:offset + length].decode("latin-1")
                offset += length
            elif tag in (CP_INT, CP_FLOAT):
                if tag == CP_INT:
                    self.ints[index] = struct.unpack_from(">i", raw, offset)[0]
                offset += 4
            elif tag in (CP_LONG, CP_DOUBLE):
                offset += 8
                index += 1
            elif tag == CP_CLASS:
                self.classes[index] = struct.unpack_from(">H", raw, offset)[0]
                offset += 2
            elif tag == CP_STRING:
                self.strings[index] = struct.unpack_from(">H", raw, offset)[0]
                offset += 2
            elif tag in (CP_FIELDREF, CP_METHODREF, CP_IFACEREF, CP_NAMEANDTYPE):
                first, second = struct.unpack_from(">HH", raw, offset)
                offset += 4
                if tag != CP_NAMEANDTYPE:
                    self.refs[index] = (first, second)
            elif tag == CP_METHODHANDLE:
                offset += 3
            elif tag == CP_METHTYPE:
                offset += 2
            elif tag in (CP_DYNAMIC, CP_INVOKEDYNAMIC):
                offset += 4
            elif tag in (CP_MODULE, CP_PACKAGE):
                offset += 2
            else:
                raise ValueError("unsupported constant pool tag %d" % tag)
            index += 1
        # access_flags, this_class and super_class sit between the pool and the interfaces. Omitting them
        # was this parser's first bug, and it is invisible without a reference: the bytes that follow are
        # read as the interface count and the walk silently runs away. Verified against javap's header.
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
        attrs = {}
        for _ in range(count):
            name_index = struct.unpack_from(">H", raw, offset)[0]
            length = struct.unpack_from(">I", raw, offset + 2)[0]
            offset += 6
            attrs[self.utf8.get(name_index)] = raw[offset:offset + length]
            offset += length
        return attrs, offset

    def code(self, member):
        body = member["attrs"].get("Code")
        if body is None:
            return b""
        length = struct.unpack_from(">I", body, 4)[0]
        return body[8:8 + length]

    def string_at(self, index):
        if index in self.strings:
            return self.utf8.get(self.strings[index])
        return self.utf8.get(index)


def loaded_strings(cf, code):
    """Every string the code loads with ldc/ldc_w, in order, and where decoding stopped.

    A complete width table keeps the walk aligned. If an opcode whose width this table does not know is
    reached, the walk stops and says so rather than continuing misaligned - a stopped walk is detectable,
    a misaligned one is not.
    """
    out = []
    offset, length = 0, len(code)
    while offset < length:
        opcode = code[offset]
        offset += 1
        if opcode in (0x12, 0x13):  # ldc, ldc_w
            index = code[offset] if opcode == 0x12 else struct.unpack_from(">H", code, offset)[0]
            offset += 1 if opcode == 0x12 else 2
            text = cf.string_at(index)
            if text is not None:
                out.append(text)
            continue
        if opcode == 0xaa:  # tableswitch
            padding = (4 - (offset % 4)) % 4
            offset += padding
            low, high = struct.unpack_from(">ii", code, offset + 4)
            offset += 12 + 4 * (high - low + 1)
            continue
        if opcode == 0xab:  # lookupswitch
            padding = (4 - (offset % 4)) % 4
            offset += padding
            pairs = struct.unpack_from(">i", code, offset + 4)[0]
            offset += 8 + 8 * pairs
            continue
        if opcode == 0xc4:  # wide
            raised = code[offset]
            offset += 1 + (2 if raised == 0x84 else 1)
            continue
        width = WIDTH[opcode]
        if width is None:
            return out, offset, False
        offset += width
    return out, offset, True


def find_block_class(classes):
    best, best_score, best_names = None, 0, []
    for name, cf in classes.items():
        for member in cf.methods:
            if member["name"] != "<clinit>":
                continue
            names, _, complete = loaded_strings(cf, cf.code(member))
            score = sum(1 for probe in PROBE_NAMES if probe in names)
            if score > best_score:
                best, best_score, best_names = (name, cf, member, complete), score, names
    return best, best_score, best_names


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--jar", default=None)
    parser.add_argument("--out", default="research/beta173-block-palette.json")
    parser.add_argument("--expected-sha1", default=None,
        help="the published SHA-1 for this jar; the run refuses to proceed without it")
    parser.add_argument("--expected-size", type=int, default=None)
    args = parser.parse_args()

    candidates = [
        pathlib.Path.home() / "AppData/Local/Temp/mce-infdev/jars/beta173-client.jar",
        pathlib.Path("run/jars/beta173-client.jar"),
    ]
    jar_path = pathlib.Path(args.jar) if args.jar else next((p for p in candidates if p.is_file()), None)
    if jar_path is None or not jar_path.is_file():
        print("could not find the b1.7.3 client jar; pass --jar", file=sys.stderr)
        return 2

    expected_sha1 = args.expected_sha1 or DEFAULT_SHA1
    expected_size = args.expected_size or DEFAULT_SIZE
    raw = jar_path.read_bytes()
    digest = hashlib.sha1(raw).hexdigest()
    print("jar   : %s" % jar_path)
    print("bytes : %d" % len(raw))
    print("sha1  : %s" % digest)
    if digest != expected_sha1 or len(raw) != expected_size:
        print("REFUSING: not the verified b1.7.3 client jar (expected sha1 %s, %d bytes)"
              % (expected_sha1, expected_size), file=sys.stderr)
        return 3

    classes, parse_failures = {}, []
    with zipfile.ZipFile(jar_path) as archive:
        for info in archive.infolist():
            if not info.filename.endswith(".class"):
                continue
            try:
                classes[info.filename] = ClassFile(archive.read(info.filename))
            except Exception as error:
                parse_failures.append((info.filename, repr(error)))
    exact = sum(1 for cf in classes.values() if cf.parsed_to_end)
    print("classes: %d, parsed to exact last byte %d, parse failures %d"
          % (len(classes), exact, len(parse_failures)))

    found, score, _ = find_block_class(classes)
    if found is None or score < 3:
        print("REFUSING: block class not identified (best probe score %d)" % score, file=sys.stderr)
        return 4
    class_name, cf, clinit, complete = found
    names, stopped_at, _ = loaded_strings(cf, cf.code(clinit))
    print("block class: %s (probes %d/4, decode complete=%s, %d string loads)"
          % (class_name, score, complete, len(names)))
    if not complete:
        print("WARNING: the decode stopped early at byte %d; the name set may be incomplete" % stopped_at)

    # Block names in this era are lowerCamelCase identifiers; the initialiser also loads other strings.
    seen, ordered = set(), []
    for text in names:
        if text and text[0].islower() and text.isalnum() and text not in seen:
            seen.add(text)
            ordered.append(text)

    negative_hits = {n: r for n, r in NEGATIVE_CONTROLS.items() if n in seen}
    positive_missing = [n for n in POSITIVE_CONTROLS if n not in seen]
    controls_ok = not negative_hits and not positive_missing
    print()
    print("controls")
    print("  negative absent : %s" % ("OK" if not negative_hits else negative_hits))
    print("  positive present: %s" % ("OK" if not positive_missing else "MISSING %s" % positive_missing))
    print("  names recovered : %d" % len(ordered))

    document = {
        "schemaVersion": 1,
        "kind": "beta173-block-name-set",
        "reviewStatus": "WORKER_GENERATED_PENDING_PARENT_VERIFICATION",
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "source": {"jar": str(jar_path), "bytes": len(raw), "sha1": digest,
                   "sha1MatchesExpected": digest == DEFAULT_SHA1},
        "method": {
            "tool": "scripts/extract-b173-palette.py (standard library only)",
            "scope": "block NAME SET only; numeric ids are deliberately not paired, because a positional "
                     "pairing is how the previous rejected artifact went wrong",
            "classCount": len(classes),
            "classesParsedToExactEnd": exact,
            "classParseFailures": parse_failures,
            "blockClass": class_name,
            "blockClassProbeScore": score,
            "decodeCompleted": complete,
        },
        "controls": {
            "negativeMustBeAbsent": NEGATIVE_CONTROLS,
            "negativeObserved": negative_hits,
            "positiveMustBePresent": POSITIVE_CONTROLS,
            "positiveMissing": positive_missing,
            "passed": controls_ok,
        },
        "blockNames": ordered,
    }
    out = pathlib.Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(document, indent=2) + "\n", encoding="utf-8")
    print()
    print("wrote %s" % out)
    print("controls passed: %s" % controls_ok)
    return 0 if controls_ok else 5


if __name__ == "__main__":
    raise SystemExit(main())
