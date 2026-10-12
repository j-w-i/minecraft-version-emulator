#!/usr/bin/env python3
"""Negative + positive controls for research/dev/validate-historical-features.py.

A validator that cannot fail proves nothing, so this script mutates copies of the real
database in memory and asserts the validator rejects each mutation, then asserts the
unmodified database passes.

Usage (from the workspace root):
    python research/dev/test-historical-features.py

Exit code 0 = all controls behaved as expected.
"""
from __future__ import annotations

import copy
import json
import subprocess
import sys
import tempfile
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
VALIDATOR = REPO_ROOT / "research" / "dev" / "validate-historical-features.py"
DATA = REPO_ROOT / "research" / "historical-features.json"
SCHEMA = REPO_ROOT / "research" / "historical-features.schema.json"


def run_validator(data_path: Path) -> tuple[int, str]:
    proc = subprocess.run(
        [sys.executable, str(VALIDATOR), "--data", str(data_path), "--schema", str(SCHEMA)],
        capture_output=True, text=True,
    )
    return proc.returncode, proc.stdout + proc.stderr


def mutate(base: dict, name: str, fn) -> dict:
    doc = copy.deepcopy(base)
    fn(doc)
    return doc


def set_first_confidence(doc, value):
    doc["entries"][0]["confidence"] = value


def set_first_category(doc, value):
    doc["entries"][0]["category"] = value


def drop_field(doc, entry_index, field):
    del doc["entries"][entry_index][field]


def drift_declared_count(doc):
    doc["validation"]["entryCount"] = doc["validation"]["entryCount"] + 1


def duplicate_id(doc):
    doc["entries"][1]["id"] = doc["entries"][0]["id"]


def bad_url(doc):
    doc["entries"][0]["evidence"][0]["url"] = "not-a-url"


def bad_value_type(doc):
    doc["entries"][0]["values"][0]["value"] = {"nested": {"too": "deep"}}


def wrong_type(doc):
    doc["entries"][0]["changes"] = "not-an-array"


CASES = [
    ("positive control: unmodified database", None, 0),
    ("bad confidence enum", lambda d: set_first_confidence(d, "certain"), 1),
    ("category outside the schema vocabulary", lambda d: set_first_category(d, "terrain_algorithms"), 1),
    ("missing required field 'evidence'", lambda d: drop_field(d, 1, "evidence"), 1),
    ("missing required field 'coverage'", lambda d: drop_field(d, 2, "coverage"), 1),
    ("declared entryCount drifts from entries", drift_declared_count, 1),
    ("duplicate entry id", duplicate_id, 1),
    ("non-absolute evidence URL", bad_url, 1),
    ("wrongly nested values[].value object", bad_value_type, 1),
    ("changes not an array", wrong_type, 1),
]


def main() -> int:
    base = json.loads(DATA.read_text(encoding="utf-8"))
    failures = []
    tmpdir = Path(tempfile.mkdtemp(prefix="mce-hf-tests-"))
    print(f"validator: {VALIDATOR.name}\ncontrols : {len(CASES)}\n")

    for i, (name, mutation, expected) in enumerate(CASES):
        doc = base if mutation is None else mutate(base, name, mutation)
        path = tmpdir / f"case-{i}.json"
        path.write_text(json.dumps(doc, indent=2), encoding="utf-8")
        code, out = run_validator(path)
        ok = (code == expected)
        result = "PASS" if ok else "FAIL"
        print(f"[{result}] {name}: exit {code} (expected {expected})")
        if not ok:
            failures.append(name)
            print("        " + out.strip().replace("\n", "\n        ")[:1200])

    total = len(CASES)
    print(f"\ncontrols passed: {total - len(failures)}/{total}")
    if failures:
        print("FAILED:", ", ".join(failures))
        return 1
    print("ALL CONTROLS PASSED")
    return 0


if __name__ == "__main__":
    sys.exit(main())
