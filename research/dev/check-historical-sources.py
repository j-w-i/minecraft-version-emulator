#!/usr/bin/env python3
"""Check that every source URL cited in research/historical-features.json is reachable.

This is a *reachability* check only. HTTP 200 does not mean the page still says what the
citation claims - content review is manual and its result is recorded in the entry's
`supports` field.

Usage (from the workspace root):
    python research/dev/check-historical-sources.py
    python research/dev/check-historical-sources.py --timeout 20 --json research/source-reachability.json

Exit code 0 = every URL returned 2xx, 1 = at least one failure.
"""
from __future__ import annotations

import argparse
import collections
import json
import ssl
import sys
import urllib.error
import urllib.request
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_DATA = REPO_ROOT / "research" / "historical-features.json"
UA = "dsh-historical-research/1.0 (+source reachability check)"


def probe(url: str, timeout: int) -> tuple[object, str]:
    ctx = ssl.create_default_context()
    last = ("ERR", "unknown")
    for method in ("HEAD", "GET"):
        try:
            req = urllib.request.Request(url, method=method, headers={"User-Agent": UA})
            with urllib.request.urlopen(req, timeout=timeout, context=ctx) as resp:
                if method == "GET":
                    resp.read(128)
                return resp.status, method
        except urllib.error.HTTPError as exc:
            last = (exc.code, f"HTTPError ({method})")
            continue
        except Exception as exc:  # noqa: BLE001 - report the class, keep going
            last = ("ERR", f"{type(exc).__name__} ({method})")
            break
    return last


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--data", type=Path, default=DEFAULT_DATA)
    ap.add_argument("--timeout", type=int, default=30)
    ap.add_argument("--json", type=Path, default=None, help="also write machine-readable results here")
    args = ap.parse_args()

    doc = json.loads(args.data.read_text(encoding="utf-8"))
    citations = collections.Counter()
    for entry in doc["entries"]:
        for ev in entry.get("evidence", []):
            citations[ev["url"]] += 1

    print(f"data            : {args.data}")
    print(f"unique urls     : {len(citations)}")
    print(f"total citations : {sum(citations.values())}\n")

    results, failures = [], []
    for url in sorted(citations):
        status, detail = probe(url, args.timeout)
        ok = isinstance(status, int) and 200 <= status < 300
        if not ok:
            failures.append(url)
        results.append({"url": url, "citations": citations[url], "status": status, "detail": detail, "ok": ok})
        print(f"{'OK ' if ok else 'FAIL'} {str(status):>4}  cites={citations[url]:<2} {url}  {detail if not ok else ''}".rstrip())

    if args.json:
        args.json.write_text(json.dumps({"checked": len(results), "failures": failures, "results": results}, indent=2) + "\n", encoding="utf-8")
        print(f"\nwrote {args.json}")

    print(f"\nreachable: {len(results) - len(failures)}/{len(results)}")
    if failures:
        print("FAILURES:")
        for url in failures:
            print("  -", url)
        return 1
    print("ALL SOURCE URLS REACHABLE")
    return 0


if __name__ == "__main__":
    sys.exit(main())
