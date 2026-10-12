#!/usr/bin/env python3
"""Validate research/historical-features.json against research/historical-features.schema.json.

Uses the established `jsonschema` implementation (Draft 2020-12) - no hand-rolled subset
validator is used to declare this file valid.

Checks performed:
  1. Both files parse as JSON.
  2. The database validates against the schema under Draft202012Validator.
  3. The schema's own $schema dialect is supported by the installed validator library.
  4. The in-file `validation` block (entryCount / categoryCounts / confidenceCounts /
     implementationStatusCounts) matches a recomputation from `entries` - so the declared
     coverage cannot drift from the data.
  5. Entry ids are unique; every entry.category is in the schema's closed vocabulary;
     every evidence URL is an absolute http(s) URL.
  6. Every field path named in research/historical-features.expected-fields.txt exists in the
     database's top-level schema properties (guards against typos in the same edit).

Usage (from the workspace root):
    python research/dev/validate-historical-features.py
    python research/dev/validate-historical-features.py --data <file> --schema <file>

Exit code 0 = valid, 1 = invalid.
"""
from __future__ import annotations

import argparse
import collections
import json
import sys
from pathlib import Path
from urllib.parse import urlparse

try:
    import jsonschema
    from jsonschema import Draft202012Validator
except ImportError:  # pragma: no cover - reported as a hard failure below
    print("FATAL: the 'jsonschema' package is required (pip install jsonschema)", file=sys.stderr)
    raise SystemExit(2)

REPO_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_SCHEMA = REPO_ROOT / "research" / "historical-features.schema.json"
DEFAULT_DATA = REPO_ROOT / "research" / "historical-features.json"
EXPECTED_FIELDS = REPO_ROOT / "research" / "historical-features.expected-fields.txt"

CATEGORY_ENUM = [
    "food_hunger", "movement_water", "combat", "mobs", "mob_drops", "spawning",
    "crafting_recipes", "worldgen", "structures", "progression", "hud_ui",
    "lighting_visuals", "technical",
]


def load(path: Path):
    with path.open(encoding="utf-8") as fh:
        return json.load(fh)


def resolve_schema_node(node, schema):
    """Follow a local $ref / items chain until we reach a concrete subschema."""
    seen = 0
    while seen < 10:
        seen += 1
        if isinstance(node, dict) and "$ref" in node:
            ref = node["$ref"]
            if ref.startswith("#/"):
                target = schema
                for part in ref[2:].split("/"):
                    target = target[part]
                node = target
                continue
        if isinstance(node, dict) and "items" in node and isinstance(node["items"], dict):
            node = node["items"]
            continue
        return node
    return node


def check_expected_fields(schema: dict, problems: list[str]) -> int:
    """Every path in the expected-fields list must resolve to a property in the schema."""
    if not EXPECTED_FIELDS.exists():
        problems.append(f"{EXPECTED_FIELDS.name} missing; cannot check field paths")
        return 0
    checked = 0
    for raw in EXPECTED_FIELDS.read_text(encoding="utf-8").splitlines():
        path = raw.strip()
        if not path or path.startswith("#"):
            continue
        cur = schema
        ok = True
        for seg in path.replace("[]", "").split("."):
            cur = resolve_schema_node(cur, schema)
            if not isinstance(cur, dict) or "properties" not in cur:
                ok = False
                break
            props = cur["properties"]
            if seg not in props:
                ok = False
                break
            cur = props[seg]
        checked += 1
        if not ok:
            problems.append(f"expected field path not found in schema: {path}")
    return checked


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--data", type=Path, default=DEFAULT_DATA)
    ap.add_argument("--schema", type=Path, default=DEFAULT_SCHEMA)
    args = ap.parse_args()

    problems: list[str] = []
    try:
        from importlib.metadata import version as _dist_version
        print(f"jsonschema version : {_dist_version('jsonschema')}")
    except Exception:
        print("jsonschema version : unknown")
    print(f"schema             : {args.schema}")
    print(f"data               : {args.data}")

    try:
        schema = load(args.schema)
        data = load(args.data)
    except json.JSONDecodeError as exc:
        print(f"RESULT: INVALID - JSON parse error: {exc}")
        return 1
    print("json parse         : OK (schema + data)")

    dialect = schema.get("$schema", "(none declared)")
    print(f"declared dialect   : {dialect}")

    validator_cls = jsonschema.validators.validator_for(schema, default=Draft202012Validator)
    print(f"validator class    : {validator_cls.__name__}")

    # 1) schema self-check + full validation
    try:
        validator_cls.check_schema(schema)
        print("schema self-check  : OK")
    except jsonschema.exceptions.SchemaError as exc:
        problems.append(f"schema is not a valid {dialect} schema: {exc.message}")

    errors = sorted(validator_cls(schema).iter_errors(data), key=lambda e: list(e.absolute_path))
    print(f"schema violations  : {len(errors)}")
    for err in errors[:40]:
        loc = "$" + "".join(f"[{p}]" if isinstance(p, int) else f".{p}" for p in err.absolute_path)
        problems.append(f"{loc}: {err.message}")

    # 2) declared coverage must equal recomputed coverage
    entries = data.get("entries", [])
    declared = data.get("validation", {})
    recomputed = {
        "entryCount": len(entries),
        "categoryCounts": dict(sorted(collections.Counter(e.get("category") for e in entries).items())),
        "confidenceCounts": dict(sorted(collections.Counter(e.get("confidence") for e in entries).items())),
        "implementationStatusCounts": dict(sorted(collections.Counter(e.get("implementationStatus") for e in entries).items())),
    }
    for key, value in recomputed.items():
        if declared.get(key) != value:
            problems.append(f"validation.{key} declared {declared.get(key)!r} != recomputed {value!r}")
    print(f"entries            : {recomputed['entryCount']} (declared {declared.get('entryCount')})")

    # 3) structural invariants that add value beyond the schema
    ids = [e.get("id") for e in entries]
    dupes = [i for i, n in collections.Counter(ids).items() if n > 1]
    if dupes:
        problems.append(f"duplicate entry ids: {dupes}")
    bad_cat = sorted({e.get("category") for e in entries} - set(CATEGORY_ENUM))
    if bad_cat:
        problems.append(f"categories outside the schema vocabulary: {bad_cat}")

    non_http = []
    for e in entries:
        for ev in e.get("evidence", []):
            parts = urlparse(ev.get("url", ""))
            if parts.scheme not in ("http", "https") or not parts.netloc:
                non_http.append(f"{e.get('id')}: {ev.get('url')}")
    if non_http:
        problems.append(f"non-absolute evidence URLs: {non_http[:5]} (total {len(non_http)})")

    # 4) expected-field paths still resolve
    checked = check_expected_fields(schema, problems)
    print(f"field paths checked: {checked}")

    # 5) explicitly forbid the weaker custom-subset validator from being the basis of a pass
    if (REPO_ROOT / "research" / "dev" / "validate_schema_subset.py").exists():
        print("note               : a custom subset validator is present in research/dev; it must NOT be "
              "used to declare this database valid")

    if problems:
        print(f"RESULT: INVALID ({len(problems)} problem(s))")
        for p in problems:
            print("  -", p)
        return 1
    print("RESULT: VALID (jsonschema Draft 2020-12, coverage self-consistency, ids, categories, URLs, field paths)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
