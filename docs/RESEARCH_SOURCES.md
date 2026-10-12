# Research sources and method — Minecraft Historical Experience

This document records **where the facts in `research/historical-features.json` come from, how far each
source can be trusted, how they were verified, and what is still unknown**. It also carries the
forward research plan for Pre-Classic → Release 1.7.10.

Scope of this pass (milestone M0-research, **revision 2 — review corrections applied**):

- Focus comparison: **Beta 1.7.3 ↔ Beta 1.8 / Beta 1.8.1** (the Adventure Update Part 1 boundary).
- Deliverables: `research/historical-features.json`, `research/historical-features.schema.json`, this file.
- Supporting (checked-in) tooling: `research/dev/validate-historical-features.py`,
  `research/dev/test-historical-features.py`, `research/dev/check-historical-sources.py`,
  `research/dev/inspect-mojang-client.py`. Supporting data:
  `research/historical-features.expected-fields.txt`, `research/source-reachability.json`.
  These paths were unclaimed when this work started; the parent agent owns them from here and may relocate
  them, in which case this section is the pointer that must be updated.
- Explicitly **not** done here: any gameplay implementation, build/Java changes, terrain algorithm work,
  or modification of `.upstream/moderner-beta`.

### Version-naming correction (applied in revision 2)

The original request phrase "preclassic through release1.7.10" was written into the database's
`scope.targetEmulationRange` as **`Beta 1.7.10`**, which is not a real version. The Beta 1.7 line ends at
**Beta 1.7.3**; `1.7.10` is a **Release**-era version. The scope field now lists the eras as
`Beta 1.0-Beta 1.8.1` and `Release 1.0.0-1.7.10` separately, with a note in `scope.inScope` citing
`Java_Edition_version_history` (Beta section lists Beta 1.8.1, 1.8, 1.7.3, 1.7.2, 1.7 and earlier —
no Beta 1.7.4-1.7.10).

**No claim in this document or the JSON is "historically validated" in the runtime sense.** "Validated"
below means only: (a) the JSON parses and conforms to the schema under a standard validator, and (b) the
cited source was reachable and said what is quoted. Per `AGENTS.md`, a wiki summary is not definitive
algorithm evidence, and player-facing behaviour claims require reproducible runtime evidence — none
exists yet. Every entry therefore carries `implementationStatus: not_started`, and no entry carries
`verificationStatus: runtime_verified`.

---

## 1. Evidence tiers

| Tier | Meaning | Used in this pass |
| --- | --- | --- |
| **A — primary artifact** | Mojang-published client jar, verified by SHA-1 against Mojang's own version metadata, inspected locally (string scan, `javap` bytecode, JVM decompilation). Facts read out of it are `artifact_verified`. | ✅ b1.8 and b1.7.3 client jars |
| **B — primary metadata / developer statement** | Mojang version manifests (`piston-meta`), release notes, developer statements (Notch/Jeb tweets, tumblr/forum posts). | ✅ version JSONs, one Jeb tweet |
| **C — authoritative community documentation** | Minecraft Wiki *raw wikitext* fetched directly (not rendered summaries), including per-version changelog pages and the official-update guide. | ✅ main workhorse |
| **D — secondary/archived community** | Archived wiki revisions, forum posts, old wiki mirrors. | ⚠️ only where explicitly labelled; not used as sole support for a value |
| **E — memory/inference** | Not acceptable. Anything not sourced is recorded as `unknown` or omitted. | ❌ never used as evidence |

Tier A is preferred because `AGENTS.md` says wiki summaries alone are not definitive. Where a numeric
value could only be sourced from Tier C, the entry's `values[].confidence` is set to `medium` even when
the feature's overall confidence is `high`. The reverse carve-out also applies and is recorded in the
schema: **a whole-jar string scan returning zero hits is not Tier A evidence** — absence of a marker
string does not prove absence of behaviour in obfuscated code (the b1.8 client, for example, contains no
`sprint` string even though sprinting demonstrably exists there). Only *registry-class inspection*, NBT
field names, and decoded class logic count as artifact-level evidence; string-absence claims are downgraded
to `medium`/`documented_only` and worded as "marker string absent", never as "feature absent".

**Proprietary-content rule (unchanged, re-affirmed in revision 2).** No decompiled source, bytecode dump,
texture, sound or other Mojang asset is copied into this repository or these deliverables. The artifacts
are downloaded to a scratch directory outside the repo, and only *digests, class/method paths and numeric
facts* are recorded. `research/dev/inspect-mojang-client.py` documents the method and prints those facts;
it does not embed any game content.

---

## 2. Methodology (reproducible)

### 2.1 Fetching wiki evidence as wikitext, not rendered pages

Rendered wiki pages lose the `History` tables, so every wiki citation is against
`https://minecraft.wiki/index.php?title=<PAGE>&action=raw` (returned HTTP 200 for all pages used).
Example:

```powershell
Invoke-WebRequest -Uri "https://minecraft.wiki/index.php?title=Java_Edition_Beta_1.8&action=raw" -OutFile b18.txt
Select-String -Path b18.txt -Pattern "Sprinting|Blocking|Critical|Hunger"
```

The `History` sections inside these raw pages are what pin *when* a mechanic changed
(e.g. `Damage` → `History` → `1.9|dev=15w34a Added attack cooldown.`).

### 2.2 Primary-artifact verification of the Beta 1.7.3 / Beta 1.8 boundary

Mojang's official client jars were downloaded to a scratch directory **outside this repository**
(`%TEMP%\mce-hist`) and never copied into the project (no Mojang code or assets are redistributed here).

```powershell
# identity of the artifacts, from Mojang's own metadata
Invoke-RestMethod "https://piston-meta.mojang.com/v1/packages/237811ba6f3418f3e64edb26246037625528ddf8/b1.8.json"
Invoke-RestMethod "https://piston-meta.mojang.com/v1/packages/66cafaf8ea5a7b76547eb24c848832af44255ba1/b1.7.3.json"
```

| Version | Official client SHA-1 (from Mojang metadata) | Local SHA-1 after download | Size |
| --- | --- | --- | --- |
| Beta 1.8 | `3139e9c29b2c74f59ea04de760ac2af5bc21b410` | identical ✅ | 1,994,742 B |
| Beta 1.7.3 | `43db9b498cb67058d2e12d394e6507722e71bb45` | identical ✅ | 1,465,375 B |
| Beta 1.8.1 | `6b562463ccc2c7ff12ff350a2b04a67b3adcd37b` (metadata only, jar not analysed) | — | 1,995,166 B |

Facts extracted from the artifacts:

- **Registry inspection (strongest form used here).** The b1.7.3 item registry class `gm` contains the
  registry names `porkchopRaw`, `porkchopCooked`, `feather`, `leather`, `shears`, `appleGold`, `bread`,
  `mushroomStew`, `fishRaw`, `fishCooked`, `cookie`; it contains no `rottenFlesh`, `beefRaw`, `beefCooked`,
  `chickenRaw`, `chickenCooked`, `melon` or `enderPearl`. The b1.8 registry class `sv` contains all of
  them. Registry membership is a much stronger claim than a jar-wide string count.
- **Marker-string probes (weaker; worded as absence of the string).** A byte scan of every class:
  `foodLevel`, `foodSaturationLevel`, `foodExhaustionLevel`, `starve` → 0 hits in b1.7.3, present in b1.8
  (`kc.class`); `XPOrb` → 0 hits in b1.7.3, present in b1.8 (`um.class`). These are recorded as
  "marker string absent", and the corroborating version pages carry the behavioural claim.
- **Hunger/food logic.** Class `kc` (food stats) gave the 20-point cap, the 5.0 starting saturation, the
  NBT field names, the `saturation += hunger * modifier * 2` formula capped at `foodLevel`, the
  exhaustion threshold of 4.0, the regeneration threshold (18) / interval (80 ticks) / +1 HP, and the
  difficulty-dependent starvation floors (10 HP easy / 1 HP normal / none hard).
- **Per-food values.** Class `aea` (food item: `(id, hunger, saturationModifier, wolfFood)` plus
  `getMaxItemUseDuration() == 32`) combined with the `sv` registry initialisers gave the 18-food table,
  including effect probabilities (raw chicken 0.3, rotten flesh 0.8).
- **Mob drops.** Per-mob drop paths referencing registry fields: zombie → `rottenFlesh` only;
  pig → raw/cooked porkchop; chicken → feather + raw/cooked chicken; cow → leather + raw/cooked beef;
  sheep shearing gated on shears + wool ("cloth") block with colour metadata.
- **Milking (revision 2 correction).** Class `py` (cow) interaction: if the held item id equals `sv.ax`
  (bucket, id 69) the held stack is replaced with `new ul(sv.aH)` = `milk` (id 79). This is the **milking**
  path. Revision 1 described it as "mooshroom shearing → milk bucket", which was a false reading and is
  corrected in the JSON; mooshrooms do not exist until 1.0.0 (Beta 1.9 Prerelease).
- **Exhaustion constants.** `sz` (player) `ar()` applies 0.8 exhaustion when sprinting, 0.2 otherwise;
  the attack path applies 0.3.

Decompilation used [Vineflower](https://github.com/Vineflower/vineflower) purely as a local reading aid.
**No decompiled source, bytecode dump, texture, sound or game asset is stored in this repository or in
these deliverables** — only digests, class/method references and numeric facts, which is what the JSON
cites. `research/dev/inspect-mojang-client.py` performs the download/verify/probe/decode steps above and
prints only those facts.

### 2.3 What was intentionally *not* verified

- No runtime was launched; no world was generated; no in-game measurement was taken.
- **Beta 1.8.1's jar was not analysed** (metadata + changelog only). The 1.8 → 1.8.1 entry therefore records
  *documented changelog differences*, explicitly **not** an exhaustive primary-source comparison; an
  artifact diff is listed as the required next step before claiming 1.8.1 ⊆ 1.8 + docs.
- Several Beta 1.7.3 per-item numbers (weapon damage, food health values, mob drop quantities) were not
  re-derived from the b1.7.3 artifact; they are recorded as `unknown` or as wiki-sourced `medium`.
- The "first supported version" of the direct-heal food model is recorded as **unknown**: mushrooms were
  edible in Classic 0.24_SURVIVAL_TEST, apples in Indev 20100110, stew 20100129-2332, bread 20100206-2034.
  Revision 1 arbitrarily used the bread build; the entry now lists bread as a *change* and states the
  ambiguity instead of asserting a boundary the sources do not support.
- No claim is made that the *emulator* behaves like any of this; there is no gameplay code yet.

---

## 3. Source list

All URLs below were re-checked for reachability on **2026-10-12**; every one returned HTTP 200.
`cites` = number of evidence citations in `historical-features.json`.

### Tier A/B — Mojang artifacts, metadata and developer statements

| cites | Source | URL | Trust | Notes |
| --- | --- | --- | --- | --- |
| 25 | Beta 1.8 official client jar | https://launcher.mojang.com/v1/objects/3139e9c29b2c74f59ea04de760ac2af5bc21b410/client.jar | **A** | SHA-1 verified against Mojang metadata; analysed locally. |
| 5 | Beta 1.7.3 official client jar | https://launcher.mojang.com/v1/objects/43db9b498cb67058d2e12d394e6507722e71bb45/client.jar | **A** | SHA-1 verified; used mainly for absence probes. |
| 1 | Beta 1.8 version metadata | https://piston-meta.mojang.com/v1/packages/237811ba6f3418f3e64edb26246037625528ddf8/b1.8.json | **B** | Pins the client SHA-1 and release time. |
| 1 | Beta 1.8.1 version metadata | https://piston-meta.mojang.com/v1/packages/2f6d2141c99bd626a069d8655135c4b13c45b981/b1.8.1.json | **B** | Artifact identity for future analysis. |
| 1 | Jeb tweet, 6 Sep 2011 (starvation floors) | https://twitter.com/jeb_/status/111047534318858240 | **B** | Server-rendered text was fetched successfully (title carries the full quote). It conflicts with the shipped client on the "normal" figure — see §4.1. |

### Tier C — Minecraft Wiki raw wikitext (all fetched with `&action=raw`)

| cites | Page | URL | Notes |
| --- | --- | --- | --- |
| 33 | Java Edition Beta 1.8 | https://minecraft.wiki/index.php?title=Java_Edition_Beta_1.8&action=raw | The core changelog: additions/changes/removals for the Adventure Update Part 1. |
| 10 | Food | https://minecraft.wiki/index.php?title=Food&action=raw | Hunger, saturation, exhaustion, starvation, food tables, and the full `History` timeline. |
| 5 | Java Edition guides/Adventure Update: Part I | https://minecraft.wiki/index.php?title=Java_Edition_guides/Adventure_Update:_Part_I&action=raw | Official-update guide; independently corroborates food values (incl. saturation) and gameplay changes. |
| 4 | Damage | https://minecraft.wiki/index.php?title=Damage&action=raw | Combat/damage `History` (criticals, blocking, cooldown, armor, unarmed damage, drowning). |
| 3 | Melee attack | https://minecraft.wiki/index.php?title=Melee_attack&action=raw | Attack cooldown, critical hits, sprint-knockback, attack-speed attribute. |
| 2 | Java Edition Beta 1.8.1 | https://minecraft.wiki/index.php?title=Java_Edition_Beta_1.8.1&action=raw | The only gameplay-visible 1.8 → 1.8.1 delta (mineshaft beams) plus its crash fixes. |
| 2 | Java Edition Beta 1.7.3 | https://minecraft.wiki/index.php?title=Java_Edition_Beta_1.7.3&action=raw | Baseline: explicitly "last version to not have the Food bar system". |
| 2 | Sprinting | https://minecraft.wiki/index.php?title=Sprinting&action=raw | Activation, speeds, sprint-knockback, FOV, hunger gating, full history. |
| 2 | Sword blocking | https://minecraft.wiki/index.php?title=Sword_blocking&action=raw | Removed-mechanic page: 50% reduction, no durability loss, fall-damage interaction, 1.9 removal. |
| 2 | Armor | https://minecraft.wiki/index.php?title=Armor&action=raw | Pre-1.0.0 durability-scaled protection formula and the "Indev until late Beta" durability table. |
| 2 | Sheep | https://minecraft.wiki/index.php?title=Sheep&action=raw | Wool/shears timeline, flee-on-attack, mutton being much later. |
| 2 | Health | https://minecraft.wiki/index.php?title=Health&action=raw | Unarmed damage 1→2→1 timeline; mob armor points as a 1.0.0 change. |
| 1 | Zombie | https://minecraft.wiki/index.php?title=Zombie&action=raw | Feather → rotten flesh drop change with quantities. |
| 1 | Pig | https://minecraft.wiki/index.php?title=Pig&action=raw | Porkchop drops, fire substitution, 1-3 quantity change in 1.3.1. |
| 1 | Chicken | https://minecraft.wiki/index.php?title=Chicken&action=raw | Raw/cooked chicken added in Beta 1.8; chicken origin in Alpha. |
| 1 | Cow | https://minecraft.wiki/index.php?title=Cow&action=raw | Leather-only drop in Alpha, raw beef/steak from Beta 1.8. |
| 1 | Experience | https://minecraft.wiki/index.php?title=Experience&action=raw | XP orbs added in Beta 1.8 with no use; enchanting later. |
| 1 | Swimming | https://minecraft.wiki/index.php?title=Swimming&action=raw | Sprint-swimming is 1.13; modern swim speeds. |
| 1 | Blocking | https://minecraft.wiki/index.php?title=Blocking&action=raw | Modern shield blocking vs the removed sword blocking; history cross-check. |
| 1 | Java Edition guides/Adventure Update: Part I (rendered) | https://minecraft.wiki/w/Java_Edition_guides/Adventure_Update:_Part_I | **C** | Rendered page cited once for the ravine listing; the raw wikitext is preferred elsewhere. |

**Gotchas observed with this wiki source** (worth knowing before the next pass):

- Some page titles are redirects in raw mode: `Hunger` → `#REDIRECT [[Food#Hunger]]`, `Rotten_flesh` and
  `Raw_porkchop` return ~40-byte stubs. Fetch the target page instead.
- `Passive_mob` and `Enchanting` are also redirects; `Java_Edition_Beta_1.8/Development_versions`
  is a stub (the pre-release details live on category pages).
- `Damaged`/`Damage` and `Melee_attack` overlap; the same change is often documented on both, which is
  useful for corroboration but also for spotting contradictions.
- The wiki's *current* values are 26.3 values. Every modern number used in this pass is explicitly
  labelled as modern in the JSON (`appliesToVersions` / `note`).

---

## 4. Contradictions and unresolved items

### 4.1 Recorded contradictions

1. **Starvation floor wording.** Jeb's 6 Sep 2011 tweet says the food bar drops health "to 5 hearts on
   easy, to 0.5 hearts on normal". The shipped b1.8 client stops starvation at ≤10 HP (5 hearts) on
   **easy** and ≤1 HP (0.5 hearts) on **normal**. Resolution: the decompiled client is authoritative;
   the tweet is retained as developer intent. See `beta18-starvation-damage-thresholds`.
2. **Cake slice hunger in Beta 1.8.** The Beta 1.8 changelog says a cake slice restores **3** hunger
   (instead of 3 HP); the Food page history says **2** per slice and 12 per whole cake. Left unresolved
   (`beta18-cake-slice-hunger-contradiction`) with a concrete next step: decode the b1.8 cake block's
   eat path from the artifact (it is a block with a bite counter, so slice **count** must be checked too).
3. **Beta 1.8 regeneration and exhaustion.** The Adventure Update guide says "sprinting and regenerating
   health drains hunger", but the b1.8 client's regeneration branch contains no exhaustion call, and the
   *Food* history attributes exhaustion-on-heal to 1.6.1 (`13w23a`). Resolution: the artifact wins — the
   entry records `regenerationCostsExhaustion: false` for Beta 1.8 with a note pointing at the guide's
   looser wording.
4. **Drowning damage rate.** Damage history says drowning was reduced to 1 HP/s in Indev; the current
   Damage page documents 2 HP/s. Not resolved for Beta 1.8; swim/drowning values are marked `low`/`unknown`.
5. **Saturation history attribution.** The b1.8 client *already* stores a hidden saturation value and a
   per-food saturation modifier, yet the Food history says saturation boost arrived in 1.9 (`15w40a`).
   These are not actually in conflict (saturation existed from 1.8; the *boost* mechanic is 1.9), but the
   distinction is easy to get wrong and is called out in `beta18-saturation-model`.

### 4.2 Known unknowns (do not implement from assumption)

- Beta 1.8 **mushroom stew** saturation modifier (its 8 hunger is confirmed; the soup-family modifier was
  not decoded).
- Beta 1.8 **drowning rate**, air-supply capacity, and swim speeds.
- Beta 1.8 **mob melee damage** values and the **sprint-knockback** magnitude.
- Beta 1.8 **spawning rules**: hostile light-level threshold, caps, slime conditions, Peaceful behaviour,
  and whether cave spiders were client-visible at all (`caveSpider` produced 0 string hits in the b1.8
  client, so its Beta 1.8 presence rests on the changelog alone).
- Beta 1.7.3 **per-item numbers**: weapon/tool damage, food restore amounts, drop quantities, and the
  exact in-game effect of the Beta 1.5–1.7 "extra +1 damage" floating-point bug.
- Exact Beta 1.8 **fall-damage formula** and the block-place delay constant behind "slightly slower".
- Whether **Moderner Beta's** Beta 1.8 preset already parameterises ravines, sea level 63 and the
  Far-Lands modulo patch (terrain authority — verify, do not reimplement).

---

## 5. Acceptance evidence for this milestone (revision 2)

Commands and observed results (Python 3.11.0 with **jsonschema 4.26.0**, Node v24.20.0, PowerShell 7,
JDK 21 `javap`). The validator is the **established `jsonschema` implementation under Draft 2020-12** —
the earlier custom "subset" validator is **not** used to declare this database valid and has been deleted.
All tooling below is checked into `research/dev/` so the result is reproducible from a clean checkout.

```powershell
# setup (isolated; jsonschema 4.26.0 + pinned transitive deps: research/requirements.txt)
python -m venv .venv
.venv\Scripts\python -m pip install -r research\requirements.txt
# concise command list for every tool below: research/dev/README.md

# 0) tooling exists and the artifact method is reproducible (no Mojang content stored in the repo)
python research/dev/inspect-mojang-client.py --list
# -> b1.7.3 / b1.8 / b1.8.1 ids, release times and official client sha1s, read live from Mojang metadata
python research/dev/inspect-mojang-client.py --verify b1.8 b1.7.3
# -> b1.8 sha1==metadata OK / size==metadata OK / sha1==pinned OK / size==pinned OK (same four lines for b1.7.3)
# -> all requested artifacts verified            (exit 0)
# negative control: a byte-tampered cached jar fails the same check (exit 1, "artifact identity check failed")
python research/dev/inspect-mojang-client.py --registry b1.7.3
# -> registry class gm.class; names include porkchopRaw/Cooked, feather, leather, shears, appleGold,
#    bread, cookie, mushroomStew, fishRaw/Cooked; no rottenFlesh/beef/chicken/melon/enderPearl
python research/dev/inspect-mojang-client.py --strings b1.7.3 --needles foodLevel,rottenFlesh,beefRaw,melon,shears
# -> foodLevel 0, rottenFlesh 0, beefRaw 0, melon 0, shears 1 (gm.class)   [string absence only]
$env:MCE_JAVAP = "C:\Program Files\Java\jdk-21\bin\javap.exe"
python research/dev/inspect-mojang-client.py --class b1.8 kc
# -> FoodStats constructor: bipush 20, bipush 20, ldc float 5.0f  (20-point hunger, 5.0 saturation)

# 1) JSON parses (two independent parsers)
python -c "import json;d=json.load(open('research/historical-features.json',encoding='utf-8'));print(len(d['entries']))"
# -> 46
node -e "const d=require('./research/historical-features.json');console.log(d.entries.length)"
# -> 46

# 2) Schema conformance with the ESTABLISHED validator (jsonschema, Draft 2020-12)
python research/dev/validate-historical-features.py
# -> jsonschema version : 4.26.0
# -> declared dialect   : https://json-schema.org/draft/2020-12/schema
# -> validator class    : Draft202012Validator
# -> schema self-check  : OK
# -> schema violations  : 0
# -> entries            : 46 (declared 46)
# -> field paths checked: 33
# -> RESULT: VALID      (exit 0)

# 3) Negative controls - a validator that cannot fail proves nothing
python research/dev/test-historical-features.py
# -> controls passed: 10/10 ; ALL CONTROLS PASSED   (exit 0)
#    positive control (unmodified) expected 0 ... got 0
#    bad confidence enum / category outside vocabulary / missing 'evidence' / missing 'coverage' /
#    declared entryCount drift / duplicate id / non-absolute evidence URL / badly nested
#    values[].value / changes not an array  -> each expected 1 ... got 1

# 4) Source reachability (every cited URL), checked in and machine-readable
python research/dev/check-historical-sources.py --json research/source-reachability.json
# -> unique urls: 25, total citations: 110, reachable 25/25, ALL SOURCE URLS REACHABLE   (exit 0)
```

Coverage after revision 2: **46 entries**; **26 `artifact_verified`, 19 `documented_only`,
1 `contradicted`**; confidence **32 high / 11 medium / 1 low / 2 unknown**. Counts by category are stored in
the `validation` block of the JSON and are re-derived by the validator (a drift fails the run). All 46 are
`implementationStatus: not_started` because no gameplay code exists yet.

> These results validate *the database file, its schema conformance and its citations*. They do **not**
> validate any Minecraft behaviour, and this milestone does not claim historical validity for the emulator.

### 5.1 Corrections applied in revision 2 (review round 1)

| # | Review point | Resolution |
| --- | --- | --- |
| 1 | `scope.targetEmulationRange` contained the non-existent `Beta 1.7.10` | Replaced with `Beta 1.0-Beta 1.8.1` **and** `Release 1.0.0-1.7.10` as separate eras, plus an in-scope note citing the version-history page (§0 above). |
| 2 | `beta173-instant-food-healing` said `firstSupportedVersion Indev 20100206-2034` while its own change list started at Indev 20100110 | `firstSupportedVersion` is now **`unknown (food-by-food accumulation)`**; the bread build moved into `changes`; the mushrooms/apples/stew/bread dates are listed in `unknowns`. `verificationStatus` downgraded `artifact_verified` → `documented_only` because the per-food HP values are wiki-sourced. |
| 3 | "mooshroom shearing → milk bucket" in `beta18-cow-drops` | **False; corrected.** Re-decoded class `py` (cow): held item `sv.ax` (bucket, id 69) → held stack replaced by `new ul(sv.aH)` (`milk`, id 79) — that is **milking**. Mooshrooms are added in 1.0.0 (Beta 1.9 Prerelease) per the Mooshroom page history, so they do not exist in any Beta 1.8 profile. |
| 4 | "exactly one gameplay-visible delta in Beta 1.8.1" was an unsupported exhaustive claim | Entry renamed to *documented changes*; `beta181JarAnalysed: false` added; confidence lowered to `medium`; the exhaustive-equivalence claim is now listed as an explicit unknown and the required next step is a jar diff. |
| 5 | Zero string hits treated as proof of absence | All 0-hit claims reworded as "marker string absent"; `beta173-no-sprinting-or-hunger-gating` downgraded to `documented_only`/`medium` and now states that the b1.8 client also has no `sprint` string while demonstrably implementing sprinting; registry inspection is called out as the stronger evidence. The schema's `confidenceLevels.high` definition now carries the same carve-out. |
| 6 | Custom subset validator used to declare validity | Deleted. `research/dev/validate-historical-features.py` uses `jsonschema` (Draft 2020-12, `Draft202012Validator`), checks schema self-consistency, coverage drift, ids, category vocabulary, URL shape and field-path regressions. `research/dev/test-historical-features.py` provides 10 controls (1 positive + 9 negative). |
| 7 | Claim that `research/dev/*.ps1` scripts write the historical JSON | **My claim was wrong; retracted.** Verified by grep: `rebuild-evidence-json.ps1` is the only script that writes a research JSON, and it writes `research/moderner-beta-evidence.json` (plus extracted preset JSON under `.upstream/extracted`). `rebuild-integration-doc.ps1` writes documentation; `compare-preset-settings.ps1` writes nothing. None of them reference `historical-features*`. |
| 8 | `inspect-mojang-client.ensure_jar` compared the download against a hardcoded digest while the docstring claimed digests were re-read from metadata | Fixed: `artifact_identity()` fetches the metadata and cross-checks it against the pinned identity (fails if the metadata id/sha1/size disagree with the pin), then `ensure_jar()` verifies the downloaded bytes' **sha1 and size against both** the metadata and the pin (four checks). Local caching in `%TEMP%` is unchanged. Negative control: a byte-tampered cached jar is rejected (exit 1). |
| 9 | Item-ID labelling: the milking note used constructor arguments as if they were saved item ids | Fixed in the JSON. Bytecode: `sv.<init>(int)` stores the constructor argument in field `a` (base id) and sets `br = argument + 256` (`sipush 256; iadd`); `ul(sv)` copies `sv.br` into the stack id field. So the registry constructor args are bucket=69 / milk=79, while **saved stack ids are 325 / 335**. The note now states both and which one stack comparisons use. |
| 10 | Validator dependencies not reproducible | Added `research/requirements.txt` pinning `jsonschema==4.26.0` plus its resolved transitive pins (`attrs 26.1.0`, `jsonschema-specifications 2025.9.1`, `referencing 0.37.0`, `rpds-py 2026.6.3`), with the optional format-checker extras commented out (the validator does not enable format assertion). Added `research/dev/README.md` with the concise command list. Verified end-to-end in a clean venv. |

### 5.2 Release-date convention note

Mojang's `piston-meta` `releaseTime` values are UTC instants one calendar day earlier than the wiki's
release dates (e.g. b1.8 `2011-09-14T22:00:00+00:00` = 15 Sep 2011 08:00 AEST; b1.7.3
`2011-07-07T22:00:00+00:00` = 8 Jul 2011 AEST; b1.8.1 `2011-09-18T22:00:00+00:00` = 19 Sep 2011 AEST).
The JSON uses the wiki/local-date convention (Beta 1.7.3 = 2011-07-08, Beta 1.8 = 2011-09-14,
Beta 1.8.1 = 2011-09-15) and this note is the reconciliation, not a contradiction.

---

## 6. Forward research plan — Pre-Classic → Release 1.7.10

The same method scales: for each era, derive a **baseline artifact** and a **transition artifact**, probe
them for presence/absence, then pin the changelog deltas with raw wikitext and record per-value
confidence. Client jars for most versions are listed in Mojang's version manifest
(`https://launchermeta.mojang.com/mc/game/version_manifest.json`); early versions are not all present, so
archive mirrors will be needed and must be labelled Tier D.

### 6.1 Era table and priority

| Priority | Era / transition | Why it matters | Artifact availability |
| --- | --- | --- | --- |
| P1 | **Beta 1.7.3 → 1.8** (done) | Largest single behaviour break in the target range (hunger, sprint, combat, worldgen). | ✅ both clients in Mojang metadata |
| P1 | **Release 1.0.0 → 1.2.5** | Enchanting, potions, breeding, the Nether-side content, XP becoming meaningful; the "notchian 1.0" experience most players remember. | ✅ launcher metadata |
| P2 | **Beta 1.8 → Release 1.0.0** | Villagers, stronghold loot, armor model rewrite, sword damage change, the End. | ✅ launcher metadata |
| P2 | **Release 1.3 → 1.5** | Singleplayer becomes an internal server (research/emulation-relevant!), XP from mining/smelting, hoppers, quartz, redstone updates. | ✅ launcher metadata |
| P2 | **Release 1.6 → 1.7.10** | Horses, new launcher/versioning model, biome/terrain changes, large-scale worldgen rework; the top of the emulation range. Note: `1.7.10` here is a **Release** version — there is no Beta 1.7.10. | ✅ launcher metadata |
| P3 | **Alpha → Beta 1.7.3** | Where the baseline mechanics originate: health, armor, farming, wolves, pistons, maps, weather. | ✅ b1.7.3 + several Alpha versions |
| P3 | **Classic → Indev → Infdev** | Survival test, health/damage, the world-height and lighting rewrites; sparse documentation. | ⚠️ partial in launcher; archive mirrors needed |
| P4 | **Pre-Classic (Cave game tech test → 0.0.x)** | Mostly of historical interest; extremely thin evidence. | ⚠️ archives only |

### 6.2 Per-era checklist (same shape every time)

1. **Pin artifacts.** Resolve each version's client jar URL + SHA-1 from `piston-meta`; record
   `releaseTime`; download to scratch only; verify SHA-1 before drawing any conclusion.
2. **Probe presence/absence.** For each candidate feature, pick 1–3 marker strings or class names that
   are *known* to exist on one side (e.g. `foodLevel`, `XPOrb`, `enchant`, `potion`). Absence of a string
   is evidence only when a positive control exists in the same jar; note the caveat that some features
   have no client-side string (sprint was such a case).
3. **Decode numbers from the artifact where practical.** Decompiled constructors, constants and
   NBT field names beat any wiki value. Record the class + method/constructor as the `reference`.
4. **Pin timing with raw wikitext.** Prefer per-version pages (`Java_Edition_Beta_1.8`) and per-mechanic
   `History` sections (`Food`, `Damage`, `Armor`, `Spawn`, `Recipe`-family pages). Record the exact
   `HistoryLine` text as the reference.
5. **Cross-check with a second locus** (official guide page, a different mechanic page, or a developer
   statement). If two loci disagree, record a `contradictions` entry and leave the value `unknown` until
   the artifact settles it.
6. **Separate mechanism from values.** Per `AGENTS.md`, availability/values belong in declarative data;
   behaviour belongs in dedicated code. The JSON is the data half; it must not be turned into an
   algorithm description.
7. **Never write to `.upstream/`.** Moderner Beta is the terrain authority; terrain claims are recorded as
   verification checklist items only.

### 6.3 Known terrain-preset constraint (blocks the P3/P4 eras)

This plan covers **gameplay/behaviour** research. Terrain coverage is a separate authority
(Moderner Beta) and is already known to be incomplete for the stated product range — recorded in
`docs/MODERNER_BETA_INTEGRATION.md` and `research/moderner-beta-evidence.json`:

- **No Pre-Classic (`rd-` / Cave game) preset and no Release 1.7.10 preset exist.** The nearest shipped
  preset ids are `moderner_beta:classic_0_0_14a_08` and `moderner_beta:release_1_6_4`; the 1.12.2 preset
  is not a 1.7.10 stand-in.
- Whether Release 1.7.10 shares Release 1.6.4's terrain pipeline is **not established**.

Consequence for this research line: the Pre-Classic and Release 1.7.10 rows below are blocked on a
terrain-authority decision (ADR) before gameplay research for those eras can be validated end-to-end.
Do not silently substitute Classic or 1.6.4 for those eras, and do not drop them.

### 6.4 Topic worklist carried forward

| Topic | Current state (this pass) | Next action |
| --- | --- | --- |
| Food/hunger values for 1.0.0 → 1.7.10 | Only Beta 1.8 pinned | Decode per-food values from 1.0.0, 1.1, 1.3.1, 1.4.2, 1.6.1, 1.7.2 artifacts; watch the 1.9 saturation-boost boundary even though it is outside the range |
| Combat: 1.0.0 sword damage change, 1.6.2 tool +1 damage | Timeline recorded, values not | Decode weapon damage tables from 1.0.0 and 1.6.2 clients |
| Armor model switch (1.0.0) | Formula + durability timeline recorded | Decode both armor paths from b1.8 and 1.0.0 to confirm rounding and durability drain per hit |
| Enchanting/potions (1.0.0) | Presence noted only | Full entries for enchantment availability, XP costs, potion effects (needed to keep them *out* of Beta profiles) |
| Mob drops for 1.0.0+ (spider eyes, gold nuggets, rare drops) | Not started | Per-mob artifact probes + loot tables, version by version |
| Spawning rules (all eras) | Beta 1.8 is `unknown` | Highest-value gap: light thresholds, caps, despawn, Peaceful. Consider decompiling spawn logic from b1.8/b1.7.3 |
| Crafting recipes per era | Beta 1.8 partially decoded | Build a machine-readable recipe delta table from b1.7.3 vs b1.8 vs 1.0.0 recipe initialisers |
| Worldgen flags (sea level, biome set, structure counts, Far Lands) | Beta 1.8 recorded | Cross-check against Moderner Beta preset capabilities before any implementation |
| Nether/End content (1.0.0) | Not started | Needed to keep 1.0.0+ profiles from missing or over-providing content |
| Singleplayer→internal server (1.3) | Not started | Architecture-relevant; also affects how "the emulator" replaces a runtime |
| Pre-Classic → Infdev documentation | Not started | Archive-based evidence; expect `unknown` to dominate, which is acceptable if recorded honestly |

### 6.5 Explicit non-goals for future research passes

- Do not vendor or reimplement Moderner Beta terrain code; research the preset boundary instead.
- Do not copy Mojang code/assets into `research/` or `docs/`; cite class/method and numbers only.
- Do not present wiki-derived values as artifact-verified; keep the two confidence axes (entry-level
  `confidence` and per-value `confidence`) distinct as this file does.
- Do not mark anything `runtime_verified` until an actual run reproduces it with retained evidence.
