"""Verify that the licence-header migration changed nothing but leading comments.

For every tracked Java file under src/, strips *all* leading comment blocks from both the working copy and
the committed version, then compares what is left. A mismatch means something outside a leading comment
changed — either by this migration, or by an earlier uncommitted edit. In the second case the script prints
the first differing line so the change can be attributed rather than assumed.

Run: python scripts/verify-header-only-change.py
"""
from __future__ import annotations

import difflib
import os
import subprocess
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def strip_leading_comments(text: str) -> str:
    """Remove every leading licence comment, and only those, and normalise two known header corrections.

    Recognised by carrying an `SPDX-License-Identifier:` line, so a file's own documentation comment — which
    sits immediately below the licence header and is also a block comment — is left alone. Stripping every
    leading block comment instead made this checker report differences in files whose only change was the
    header, because the documentation comment came off one side and not the other.

    Two textual corrections are applied to both sides, because they are deliberate and are not code:

      * `MaxTechnik/OldVillages`, which does not exist, was corrected to `MaxTechnik3796/old_villages` in the
        provenance headers of the ported village files.
    """
    normalised = text.replace("\r\n", "\n")
    normalised = normalised.replace(
        "https://github.com/MaxTechnik/OldVillages",
        "https://github.com/MaxTechnik3796/old_villages",
    )
    while normalised.startswith("/*"):
        end = normalised.find("*/")
        if end == -1:
            break
        if "SPDX-License-Identifier:" not in normalised[:end + 2]:
            break
        normalised = normalised[end + 2:].lstrip("\n")
    return normalised


def tracked_java_files() -> list[str]:
    out = subprocess.run(
        ["git", "ls-files", "src"],
        cwd=REPO,
        capture_output=True,
        text=True,
        check=True,
    ).stdout
    return [line for line in out.splitlines() if line.endswith(".java")]


def main() -> int:
    differing: list[tuple[str, str]] = []
    checked = 0
    for rel in tracked_java_files():
        path = os.path.join(REPO, rel)
        if not os.path.isfile(path):
            continue
        head = subprocess.run(
            ["git", "show", f"HEAD:{rel}"],
            cwd=REPO,
            capture_output=True,
            check=True,
        ).stdout.decode("utf-8")
        with open(path, encoding="utf-8", newline="") as handle:
            working = handle.read()
        checked += 1
        before = strip_leading_comments(head)
        after = strip_leading_comments(working)
        if before == after:
            continue
        first = next(
            (
                line
                for line in difflib.unified_diff(before.split("\n"), after.split("\n"), lineterm="", n=0)
                if line[:1] in "+-" and line[:3] not in ("+++", "---")
            ),
            "(no diff line?)",
        )
        differing.append((rel, first))

    print(f"checked {checked} tracked Java files")
    print(f"{len(differing)} file(s) differ outside their leading comments")
    for rel, line in differing:
        print(f"  {rel}\n      {line}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
