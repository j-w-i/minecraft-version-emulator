"""Compare this project's ported NT asset files against the pinned NT checkout.

Read-only provenance evidence for THIRD_PARTY_NOTICES.md and docs/PROVENANCE.md. It reports, per asset,
whether the project's file is byte-identical to upstream, differs only by the namespace rewrite
(nostalgic_tweaks -> historical_experience), or differs in some other way.

The upstream asset tree is extracted from the pinned checkout on first run, so the comparison does not
depend on a directory somebody happened to create by hand.

Run:  python scripts/compare-nt-assets.py
"""
from __future__ import annotations

import hashlib
import os
import subprocess
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
NT_CHECKOUT = os.path.join(REPO, ".upstream", "nostalgic-tweaks")
# Git-ignored scratch; extracted from the checkout rather than committed.
NT_EXTRACT = os.path.join(REPO, ".upstream", "_ntassets")
NT_ASSETS = os.path.join(NT_EXTRACT, "common", "src", "main", "resources", "assets", "nostalgic_tweaks")
NT_BRANCH = "origin/1.21"
OWN_ASSETS = os.path.join(REPO, "src", "main", "resources", "assets", "historical_experience")

PROJECT_NAMESPACE = "historical_experience"
UPSTREAM_NAMESPACE = "nostalgic_tweaks"


def ensure_upstream_assets() -> None:
    """Extract the pinned NT asset tree if it is not already there."""
    if os.path.isdir(NT_ASSETS):
        return
    if not os.path.isdir(os.path.join(NT_CHECKOUT, ".git")):
        print(f"No NT checkout at {NT_CHECKOUT}.", file=sys.stderr)
        print("Clone https://github.com/Nostalgica-Reverie/Nostalgic-Tweaks there first.", file=sys.stderr)
        raise SystemExit(2)
    os.makedirs(NT_EXTRACT, exist_ok=True)
    archive = os.path.join(NT_EXTRACT, "nt-assets.tar")
    subprocess.run(
        [
            "git", "-C", NT_CHECKOUT, "archive", "--format=tar", f"-o", archive, NT_BRANCH,
            "common/src/main/resources/assets/nostalgic_tweaks",
        ],
        check=True,
    )
    # tar is resolved from PATH; on Windows this is the bsdtar shipped with the OS.
    subprocess.run(["tar", "-xf", archive, "-C", NT_EXTRACT], check=True)
    os.remove(archive)


def sha256(path: str) -> str:
    with open(path, "rb") as handle:
        return hashlib.sha256(handle.read()).hexdigest()


def main() -> int:
    ensure_upstream_assets()
    if not os.path.isdir(NT_ASSETS):
        print(f"NT asset tree still not present at {NT_ASSETS}", file=sys.stderr)
        return 2

    identical = []
    namespaced = []
    other = []
    missing = []

    for root, _dirs, files in os.walk(OWN_ASSETS):
        for name in files:
            own = os.path.join(root, name)
            rel = os.path.relpath(own, OWN_ASSETS)
            upstream = os.path.join(NT_ASSETS, rel)
            if not os.path.isfile(upstream):
                missing.append(rel)
                continue
            if sha256(own) == sha256(upstream):
                identical.append(rel)
                continue
            if own.endswith(".json"):
                with open(own, "rb") as handle:
                    own_bytes = handle.read()
                with open(upstream, "rb") as handle:
                    upstream_bytes = handle.read()
                rewritten = upstream_bytes.replace(
                    UPSTREAM_NAMESPACE.encode(), PROJECT_NAMESPACE.encode()
                )
                if own_bytes == rewritten:
                    namespaced.append(rel)
                    continue
            other.append(rel)

    def report(title: str, items: list[str]) -> None:
        print(f"\n{title} ({len(items)})")
        for item in sorted(items):
            print(f"  {item}")

    report("byte-identical to NT", identical)
    report("identical apart from the namespace rewrite", namespaced)
    report("NOT present upstream (project-authored or from elsewhere)", missing)
    report("differs from upstream in some other way (review needed)", other)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

