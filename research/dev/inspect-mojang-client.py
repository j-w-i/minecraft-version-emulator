#!/usr/bin/env python3
"""Reproduce the primary-artifact inspection behind research/historical-features.json.

This script downloads Mojang's official client jars to a scratch directory OUTSIDE the repository,
verifies each SHA-1 against Mojang's own version metadata, and prints the *facts* that the database
cites: marker-string presence/absence, item-registry membership, and (optionally) decoded class
member/value output via `javap`.

It does NOT store, copy or embed any Minecraft code, bytecode, texture, sound or other asset in the
repository. Only digests, class/method paths and numeric facts are printed. Nothing is written into
the workspace.

Requirements: Python 3.9+ (stdlib only) for --verify/--strings/--registry; a JDK (`javap`) on PATH for
--class. Network access is required unless --jar points at an already-downloaded file.

Usage (from the workspace root):
    python research/dev/inspect-mojang-client.py --list
    python research/dev/inspect-mojang-client.py --verify b1.8 b1.7.3
    python research/dev/inspect-mojang-client.py --strings b1.7.3 --needles foodLevel,rottenFlesh,melon
    python research/dev/inspect-mojang-client.py --registry b1.8
    python research/dev/inspect-mojang-client.py --class b1.8 kc

Exit code 0 = all requested checks passed, 1 = a check failed (e.g. SHA-1 mismatch).
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
import urllib.request
import zipfile
from pathlib import Path

# Version metadata packages on Mojang's piston-meta service. The digests below are the PINNED
# expectation recorded in research/historical-features.json. ensure_jar() checks the DOWNLOADED BYTES
# against BOTH (a) the sha1+size fetched live from this metadata and (b) this pinned table, and fails
# if the fetched metadata disagrees with the pinned identity. A tampered table cannot silently pass,
# and a re-published/different artifact cannot silently pass either.
VERSIONS = {
    "b1.7.3": {"json": "https://piston-meta.mojang.com/v1/packages/66cafaf8ea5a7b76547eb24c848832af44255ba1/b1.7.3.json",
               "id": "b1.7.3",
               "client_sha1": "43db9b498cb67058d2e12d394e6507722e71bb45", "size": 1465375},
    "b1.8": {"json": "https://piston-meta.mojang.com/v1/packages/237811ba6f3418f3e64edb26246037625528ddf8/b1.8.json",
             "id": "b1.8",
             "client_sha1": "3139e9c29b2c74f59ea04de760ac2af5bc21b410", "size": 1994742},
    "b1.8.1": {"json": "https://piston-meta.mojang.com/v1/packages/2f6d2141c99bd626a069d8655135c4b13c45b981/b1.8.1.json",
               "id": "b1.8.1",
               "client_sha1": "6b562463ccc2c7ff12ff350a2b04a67b3adcd37b", "size": 1995166},
}

# Marker strings used for presence/absence probes. Absence of a marker string is WEAK evidence and is
# reported as such: obfuscated code can implement behaviour without an identifying string.
MARKERS = ["foodLevel", "foodSaturationLevel", "foodExhaustionLevel", "starve", "XPOrb",
           "porkchopRaw", "porkchopCooked", "rottenFlesh", "beefRaw", "beefCooked",
           "chickenRaw", "chickenCooked", "feather", "leather", "cloth", "shears",
           "enderman", "stronghold", "melon", "appleGold"]

UA = "dsh-historical-research/1.0"


def scratch_dir() -> Path:
    d = Path(os.environ.get("MCE_ARTIFACT_DIR", Path(tempfile.gettempdir()) / "mce-hist" / "artifacts"))
    d.mkdir(parents=True, exist_ok=True)
    return d


def fetch_meta(version: str) -> dict:
    with urllib.request.urlopen(urllib.request.Request(VERSIONS[version]["json"], headers={"User-Agent": UA}), timeout=60) as r:
        return json.load(r)


def artifact_identity(version: str) -> tuple[str, int]:
    """Resolve the client sha1+size for a version.

    Fetches Mojang metadata live, and cross-checks it against the pinned identity in VERSIONS.
    Returns the metadata's own values (which must equal the pinned ones), so callers compare the
    downloaded bytes against the authority rather than against a constant that could drift.
    """
    pinned_sha, pinned_size = VERSIONS[version]["client_sha1"], VERSIONS[version]["size"]
    meta = fetch_meta(version)
    meta_id = meta.get("id")
    if meta_id != VERSIONS[version]["id"]:
        raise SystemExit(f"refusing to use {version}: metadata id {meta_id!r} != pinned id {VERSIONS[version]['id']!r}")
    dl = meta["downloads"]["client"]
    if dl["sha1"] != pinned_sha or dl["size"] != pinned_size:
        raise SystemExit(
            f"refusing to use {version}: Mojang metadata disagrees with the pinned identity "
            f"(metadata sha1={dl['sha1']} size={dl['size']} vs pinned sha1={pinned_sha} size={pinned_size})"
        )
    return dl["sha1"], dl["size"]


def ensure_jar(version: str, verify: bool = True) -> Path:
    d = scratch_dir()
    path = d / f"{version}-client.jar"
    meta_sha, meta_size = artifact_identity(version)
    if not path.exists():
        url = fetch_meta(version)["downloads"]["client"]["url"]
        print(f"  downloading {version} client -> {path}")
        urllib.request.urlretrieve(url, path)
    if verify:
        data = path.read_bytes()
        got_sha = hashlib.sha1(data).hexdigest()
        got_size = len(data)
        checks = {
            "sha1==metadata": (got_sha == meta_sha, f"{got_sha} vs metadata {meta_sha}"),
            "size==metadata": (got_size == meta_size, f"{got_size} vs metadata {meta_size}"),
            "sha1==pinned": (got_sha == VERSIONS[version]["client_sha1"],
                             f"{got_sha} vs pinned {VERSIONS[version]['client_sha1']}"),
            "size==pinned": (got_size == VERSIONS[version]["size"],
                             f"{got_size} vs pinned {VERSIONS[version]['size']}"),
        }
        for name, (ok, detail) in checks.items():
            print(f"  {version} {name}: {'OK' if ok else 'MISMATCH'} ({detail})")
        if not all(ok for ok, _ in checks.values()):
            raise SystemExit(f"refusing to inspect {version}: artifact identity check failed")
    return path


def classes(path: Path) -> dict[str, bytes]:
    with zipfile.ZipFile(path) as z:
        return {n: z.read(n) for n in z.namelist() if n.endswith(".class")}


def cmd_list() -> int:
    print("known versions (digest re-read from Mojang metadata at run time):")
    for v in VERSIONS:
        meta = fetch_meta(v)
        print(f"  {v:<7} id={meta['id']:<7} release={meta['releaseTime']:<22} "
              f"client_sha1={meta['downloads']['client']['sha1']} size={meta['downloads']['client']['size']}")
    return 0


def cmd_verify(versions: list[str]) -> int:
    for v in versions:
        print(f"verify {v}:")
        ensure_jar(v, verify=True)
    print("all requested artifacts verified")
    return 0


def cmd_strings(version: str, needles: list[str]) -> int:
    path = ensure_jar(version)
    blob = classes(path)
    print(f"marker string scan: {version}, {len(blob)} classes")
    print("NOTE: 0 hits = the STRING is absent. It is not proof the behaviour is absent.")
    joined = b"".join(blob.values())
    for needle in needles:
        raw = needle.encode()
        hits = [n for n, b in blob.items() if raw in b]
        print(f"  {needle:<24} occurrences={joined.count(raw):<3} first_classes={hits[:3]}")
    return 0


def cmd_registry(version: str) -> int:
    """List the item-registry names present in the client, read from the registry class."""
    path = ensure_jar(version)
    blob = classes(path)
    # registry class holds the item field initialisers; find the class with the most known item names
    known = [b"porkchopRaw", b"porkchopCooked", b"appleGold", b"mushroomStew", b"bread", b"cookie"]
    scored = sorted(((sum(1 for k in known if k in b), n) for n, b in blob.items()), reverse=True)
    if not scored or scored[0][0] == 0:
        print("no registry class found")
        return 1
    best = scored[0][1]
    data = blob[best]
    names = sorted({m.group().decode("latin1") for m in re.finditer(rb"[A-Za-z_][A-Za-z0-9_]{2,30}", data)})
    interesting = [n for n in names if any(k in n.lower() for k in
                   ("porkchop", "beef", "chicken", "rotten", "melon", "apple", "bread", "cookie",
                    "fish", "stew", "shears", "feather", "leather", "pearl", "seeds"))]
    print(f"registry class: {best} ({len(data)} bytes) for {version}")
    print("registry-name-like strings found:")
    for n in interesting:
        print("   ", n)
    return 0


def cmd_class(version: str, cls: str) -> int:
    javap = os.environ.get("MCE_JAVAP") or shutil.which("javap")
    if not javap or not Path(javap).exists():
        print("javap not found. Set MCE_JAVAP to a javap path (e.g. C:\\Program Files\\Java\\jdk-21\\bin\\javap.exe) "
              "or add a JDK to PATH.")
        return 1
    path = ensure_jar(version)
    with zipfile.ZipFile(path) as z:
        if f"{cls}.class" not in z.namelist():
            print(f"{cls}.class not present in {version}")
            return 1
        tmp = Path(tempfile.mkdtemp(prefix="mce-class-"))
        (tmp / f"{cls}.class").write_bytes(z.read(f"{cls}.class"))
    print(f"javap -p -c {version}:{cls}  (members and decoded logic; no source is stored)")
    out = subprocess.run([javap, "-p", "-c", str(tmp / f"{cls}.class")], capture_output=True, text=True).stdout
    print(out)
    return 0


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--list", action="store_true", help="list known versions and their official digests")
    ap.add_argument("--verify", nargs="+", metavar="VERSION", help="download+verify official client jar digests")
    ap.add_argument("--strings", metavar="VERSION", help="scan a client jar for marker strings")
    ap.add_argument("--needles", default=",".join(MARKERS), help="comma-separated marker strings")
    ap.add_argument("--registry", metavar="VERSION", help="list item-registry names present in a client jar")
    ap.add_argument("--class", dest="class_name", nargs=2, metavar=("VERSION", "CLASS"),
                    help="javap a single class (e.g. --class b1.8 kc)")
    ap.add_argument("--jar", type=Path, default=None, help="use an already-downloaded jar instead of downloading")
    args = ap.parse_args()

    if args.list:
        return cmd_list()
    if args.verify:
        return cmd_verify(args.verify)
    if args.strings:
        return cmd_strings(args.strings, [n.strip() for n in args.needles.split(",") if n.strip()])
    if args.registry:
        return cmd_registry(args.registry)
    if args.class_name:
        return cmd_class(args.class_name[0], args.class_name[1])
    ap.print_help()
    return 0


if __name__ == "__main__":
    sys.exit(main())
