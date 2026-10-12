# research/dev — historical feature research tooling

Developer tooling only; **not part of the game build**. No Minecraft dependency, no Mojang content is
stored here. `docs/RESEARCH_SOURCES.md` is the authoritative write-up of method, sources and unknowns;
this file only lists the commands.

## Setup (isolated)

```powershell
python -m venv .venv
.venv\Scripts\python -m pip install -r research\requirements.txt
```

Use `.venv\Scripts\python` in place of `python` below. `javap` (JDK 21+) is only needed for the
`--class` mode of the artifact tool; set `MCE_JAVAP` if it is not on `PATH`.

## Commands

```powershell
# Validate the database against the schema (established validator: jsonschema, Draft 2020-12)
python research\dev\validate-historical-features.py

# Prove the validator can fail: 1 positive control + 9 negative controls
python research\dev\test-historical-features.py

# Check every cited source URL is reachable (writes research/source-reachability.json)
python research\dev\check-historical-sources.py --json research\source-reachability.json

# Reproduce the primary-artifact evidence behind the entries
python research\dev\inspect-mojang-client.py --list
python research\dev\inspect-mojang-client.py --verify b1.8 b1.7.3
python research\dev\inspect-mojang-client.py --registry b1.7.3
python research\dev\inspect-mojang-client.py --strings b1.7.3 --needles foodLevel,rottenFlesh,melon
$env:MCE_JAVAP = "C:\Program Files\Java\jdk-21\bin\javap.exe"
python research\dev\inspect-mojang-client.py --class b1.8 kc
```

All four exit `0` on success and `1` on failure.

## Notes

- `--verify` compares the downloaded bytes' SHA-1 **and size** against both the live Mojang metadata and
  the pinned identity in the script; a tampered local cache or a re-published artifact fails.
- Client jars are cached outside the repo (`%TEMP%\mce-hist\artifacts`, override with `MCE_ARTIFACT_DIR`).
  Never commit them, and never copy decompiled output into the repository.
- `research/historical-features.expected-fields.txt` lists the field paths the validator asserts exist in
  the schema; update it in the same change as any schema field rename.
- A zero-hit string scan is *absence of a marker string*, not proof of absence of behaviour. The
  `--registry` and `--class` modes are the stronger evidence forms.
