# Nostalgic Tweaks — adaptation audit for Minecraft Historical Experience

> **Licensing superseded, 2026-10-14.** The licence section of this audit concluded that copying was not
> permitted on the evidence then available, and set an interim posture of "copy nothing". The owner subsequently
> directed that the implementations be ported, and the licence position the resulting code is distributed under is
> now established and enforced: see [LICENSING.md](LICENSING.md), [PROVENANCE.md](PROVENANCE.md) and
> [THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md). The audit's factual provenance findings — branch, commit,
> absence of an in-tree licence file on the source branch, licence history — remain the evidence this project
> relies on, and the points that could not be settled are carried forward as unresolved in LICENSING.md §3.

**Status:** `WORKER_GENERATED_PENDING_PARENT_VERIFICATION`
**Audit date:** 2026-10-13
**Machine-readable companion:** [`research/nostalgic-tweaks-audit.json`](../research/nostalgic-tweaks-audit.json)

This audit reads the upstream mod's own source and git history. It does not use the upstream README
for feature claims, it does not run anything, and it distinguishes throughout between two different
questions that are easy to confuse:

- **What does Nostalgic Tweaks do, and what boundary does it assert?** — answerable from the source.
- **When did the original game actually change?** — *not* answerable from Nostalgic Tweaks, which cites
  no client jar, class, hash or manifest for any boundary it states.

Nostalgic Tweaks is a secondary source. Every "boundary" below is either a citation of what NT claims,
or an explicit acknowledgement that no boundary is stated at all.

---

## 1. Repository, commit and provenance

| Item | Value |
| --- | --- |
| URL requested by the task | `https://github.com/rdh-gg/Nostalgic-Tweaks` — **does not exist** |
| Canonical URL | `https://github.com/Nostalgica-Reverie/Nostalgic-Tweaks` |
| Default branch | `main` — **docs-only**, contains no `.java` files |
| Branch audited | `origin/1.21` (the branch that actually holds the source, and the most recently pushed) |
| Commit | `51d6d3dc1fa33223f8548e1625ed7595183c2b9e` |
| Commit date | 2026-08-14T00:50:27-04:00 |
| Commit subject | `chore: update changelog` |
| Declared mod version | `2.0.0-beta.923-indev.2` (`gradle.properties:15`) |
| Tags | 16 exist; **none points at the audited commit** |
| Clone location | `.upstream/nostalgic-tweaks` (git-ignored) |

### How the canonical URL was established

The requested URL is not merely wrong, it is nonexistent, so the canonical repository had to be
located rather than assumed:

1. `git ls-remote https://github.com/rdh-gg/Nostalgic-Tweaks` → `remote: Repository not found.` (exit 128).
2. `https://api.github.com/users/rdh-gg` → HTTP 404. The account or organisation does not exist either,
   so this is not a dead repository behind a live owner.
3. `https://api.github.com/repos/Nostalgica-Reverie/Nostalgic-Tweaks` → HTTP 200, `"fork": false`,
   `homepage: https://modrinth.com/mod/nostalgic-tweaks`, topics `[minecraft-mod, nostalgia-project,
   nostalgic-tweaks]`. A non-fork repository pointing at the mod's official Modrinth page.
4. `https://api.github.com/repos/Adrenix/Nostalgic-Tweaks` → HTTP 200 for **the same repository id**
   (`466280964`). GitHub redirects the old owner name to the new one: this is a repository transfer,
   established from the API rather than guessed.

### Why `origin/1.21` and not the default branch

The default branch cannot be used for a feature audit: `git ls-tree --name-only origin/main` returns
exactly `.github`, `LICENSE.txt`, `README.md`, `contributors.md` — zero Java files. `origin/1.21` carries
917 `.java` files across `common/`, `fabric/` and `neoforge/`, and its tip (2026-08-14) is newer than
every other branch, including `origin/26.1.x` (2026-08-13) and `origin/main` (2026-06-01).

> **Note on the clone.** `.upstream/nostalgic-tweaks` already existed with `origin` pointing at the
> canonical URL, checked out detached at `origin/1.21`. It was reused read-only rather than overwritten,
> and `git fetch --all --tags` was run. Deep reading used a detached worktree in `%TEMP%`, deliberately
> outside the repository tree. `git status --porcelain` on the clone is empty.

---

## 2. Licence verdict

**Copying code from this mod into Minecraft Historical Experience is NOT permitted on the available
evidence. `reusePermitted = false`. Treat every feature below as "reimplement from the described
behaviour, do not copy".**

The situation is worse than a simple "restrictive licence", because **the licence governing the audited
revision is not stated anywhere in the repository**:

| Fact | Evidence |
| --- | --- |
| The audited branch has **no licence file** | `git rev-parse origin/1.21:LICENSE.txt` and `...:LICENSE` both fail; the branch root listing contains neither |
| GitHub reports LGPL-3.0 | From `api.github.com`, but GitHub resolves this from the **docs-only** `main` branch, so it does not describe the source |
| The only licence ever recorded against a source-carrying branch was **proprietary** | The Minecraft Mod Public License, blob `c5774af0…`, in force from 2022-10-10 until deleted from source branches on 2025-01-15 |
| The LGPL text lives only on `main` | Added 2026-06-01 by commit `6f2614ef`, whose subject is **"Update README.md"** |
| The mod's own declared licence URL is stale | `gradle.properties:20` points at `LICENSE` on `master`; no such file exists at the audited revision |

### The licence text that governs the source

From the Minecraft Mod Public License, blob `c5774af051e6c7bfaf094040105c48d5cd4033b4`, 136 lines:

```
 95: The User may create Derivative(s) based on the Mod.
 96: A Derivative must contain changes which a reasonably informed person would consider significant.
 98: The User may distribute a Derivative if the following conditions are met:
 99:  - The Derivative does not generate revenue.
100:  - The Derivative provides credit to the Author.
101:  - The Derivative complies with the Mod license.
```

This does **not** forbid derivation, so the task's "if the licence forbids reuse, say so explicitly"
precondition is not met in those words. But it is not permissive either. Line 101 is the operative
blocker: a distributed derivative incorporating MMPL code must itself comply with the MMPL, so it cannot
simply be relicensed under whatever licence HX uses. Line 136 closes the obvious escape hatch:

```
136: The Author may change the Mod license, but cannot do so retroactively.
```

Even if the 2026 LGPL grant were intended to cover the source, it could not retroactively free code
authored under the MMPL.

### Licence history and relicensing

Four distinct licence blobs exist in the repository's entire history, verified by walking
`git log --all --name-status -- 'LICENSE' 'LICENSE.txt'` and resolving every revision's blob:

| Blob | Title | Lines | Proprietary |
| --- | --- | --- | --- |
| `c5774af0…` | Minecraft Mod Public License v2.0.3 | 136 | yes |
| `75c07fbd…` | MMPL v2.0.3-Modified | 136 | yes |
| `de7d7565…` | MMPL v2.0.3-Modified (Last updated: 18 Nov 2022) | 139 | yes |
| `0a041280…` | GNU Lesser General Public License v3 | 165 | no |

Changes in order: `460853d2` (2022-10-10) and `b8ae0287` (2022-11-18) edited the MMPL in place — not
relicensing. `42fc93c4` (2024-08-15) renamed `LICENSE` to `LICENSE.txt`: verified as a rename, because
the added and deleted blobs are **textually identical** (`de7d7565`). `47fd9155` (2025-01-15,
"Init 1.21.4") **deleted** it, leaving every later source branch — including the audited one — with no
licence at all. `6f2614ef` (2026-06-01) added LGPL-3.0 on `main` only.

That last event is the repository's only substantive licence change, and its intent cannot be
established from the repository. Web search surfaced an unverified third-party claim that the
project relicensed to LGPL-3.0; that claim is recorded in the JSON as an explicit unknown and was
**not** used as evidence.

### Two further licence problems

- **The LGPL file is incomplete on its own.** `LICENSE.txt` is the standard *COPYING.LESSER* preamble;
  lines 9–11 incorporate GPLv3 by reference. The repository contains **no GPLv3 text at any revision**.
  Full LGPL-3.0 compliance is therefore not satisfiable from the repository as distributed.
- **No per-file headers, and no exceptions exercised.** A scan of the first 12 lines of every `.java`
  file found exactly one licence header: `NormalizedLevenshtein.java:2-5`, an MIT header by a third
  party. Every other file has none. The MMPL clause allowing "exact or Codebase path exceptions" is
  therefore never exercised, so there is no path-specific carve-out that would let HX copy one
  subsystem more freely than another.

---

## 3. What the mod actually does

The inventory comes from the mod's **own config declaration classes**, cross-checked against its own
mixin registration manifests — not from the README.

**527 tweak declarations**, in six interfaces under `mod/adrenix/nostalgic/tweak/config/`:

| Class | Tweaks |
| --- | --- |
| `CandyTweak` | 308 |
| `GameplayTweak` | 106 |
| `SoundTweak` | 42 |
| `ModTweak` | 28 |
| `AnimationTweak` | 26 |
| `SwingTweak` | 17 |

By declared side (read from each declaration's factory call):

| Side | Count | Meaning |
| --- | --- | --- |
| `client` | 410 | presentation only |
| `server` | 103 | gameplay |
| `dynamic` | 14 | synchronised both ways |

That ratio is the headline: **roughly 80% of this mod is client-facing presentation**, and it shrinks
further when `ModTweak`'s 28 entries (the mod's own config-screen options) are excluded as irrelevant.

### The most important architectural fact

NT does not ask the world anything. It reads a **static, process-global config object**:

- Tweak declarations are `static final` interface fields, e.g. `GameplayTweak.DISABLE_HUNGER` at
  `tweak/config/GameplayTweak.java:204`, and every implementation gates on that static — for instance
  `mixin/tweak/gameplay/food_health/FoodDataMixin.java:34` and `:55`.
- Configuration is persisted per client and per server as JSON in the mod-loader config folder, not per
  world: `config/ServerConfig.java:32` (`@Config(filename = MOD_ID + "-server")`) and
  `config/factory/ConfigHandler.java:100-106`.

This directly contradicts the consuming project's core invariant, documented in its own source at
`HistoricalPlayerRules.java:15-16` (the hook "asks the registry what this version does") and
`HistoricalVersion.java:150-161` (empty collections "leave the modern behaviour alone", which is what
keeps ordinary worlds untouched).

**Consequence:** no NT behaviour can be adopted as a *configuration mechanism*. Only its *algorithms and
its data shapes* are of any use, and each needs HX's own world-derived gate. This is stated once here
and it is the reason several otherwise-attractive features are classified `b` or `c` rather than `a`.

---

## 4. Boundary table — the features that matter most

`boundaryStatus` is about **NT's own statement**, per the constraint that NT is a secondary source:

- **VERIFIED** = NT's source, resource or enum states the boundary, and the citation is given. It does
  **not** mean the boundary is historically true.
- **INFERRED** = the auditor concluded it from structure or context, with the reasoning stated.
- **UNKNOWN** = no boundary could be established, and the search performed is recorded.

| # | Feature | NT's stated boundary | Status | Primary evidence | Class |
| --- | --- | --- | --- | --- | --- |
| 1 | Sprint suppression | none stated | INFERRED | `GameplayTweak.java:142`; description says only "only applied in survival mode" | d |
| 2 | Sprint-swim suppression | none stated | UNKNOWN | `GameplayTweak.java:165` | d |
| 3 | Instant air refill | none stated | UNKNOWN | `GameplayTweak.java:166`; `mechanics_swim/LivingEntityMixin.java:24-36` | b |
| 4 | Hunger suppression + direct healing | none stated | INFERRED | `GameplayTweak.java:204`; `food_health/PlayerMixin.java:29-60` | b |
| 5 | Food heal values | none stated | UNKNOWN | `DefaultListing.java:110-129` (uncited numbers) | a |
| 6 | Eat only when hurt | none stated | UNKNOWN | `food_health/PlayerMixin.java:49-60` | a |
| 7 | Instant eat | none stated | UNKNOWN | `GameplayTweak.java:209-211` | b |
| 8 | Food stack sizes | none stated | UNKNOWN | `DefaultListing.java:131-175` (uncited numbers) | a |
| 9 | Attack cooldown removal | none stated | INFERRED | `GameplayTweak.java:103`; `combat_player/PlayerMixin.java:26-51` | d |
| 10 | Critical hit removal | none stated | INFERRED | `GameplayTweak.java:105`; `combat_player/PlayerMixin.java:53-67` | b |
| 11 | Sword blocking | none stated | INFERRED | `GameplayTweak.java:94-97`; `combat/SwordBlockingHelper.java` | c |
| 12 | Old damage values | none stated | UNKNOWN | `combat/DamageHelper.java:22-30` (uncited formula) | a |
| 13 | Left-click door/lever/button | none stated | UNKNOWN | `GameplayTweak.java:143-145` | b |
| 14 | Axe stripping / shovel pathing | none stated | UNKNOWN | `GameplayTweak.java:199-200`; `InteractionHelper.java:16-29` | b |
| 15 | Old mob movement speeds | **Alpha/Beta, stated** | VERIFIED | `en_us.json` `gameplay.oldMobSpeeds.info`; `DefaultListing.java:63-78` | a |
| 16 | Old animal spawning + cap | **Alpha/Beta, stated** | VERIFIED | `en_us.json` `gameplay.animalSpawnCap.info`; `AnimalSpawnHelper.java:73-253` | c |
| 17 | Monster spawn cap | none (modern value only) | UNKNOWN | `GameplayTweak.java:48-51`; `monster_spawn/MobCategoryMixin.java:27-28` | a |
| 18 | Mob drop substitution | "Classic Mobs" (group name) | UNKNOWN | `MobLootHelper.java:144-198` | d |
| 19 | XP orbs (spawn / render / pickup) | **Beta 1.8 for the sound only** | VERIFIED / UNKNOWN | `SoundTweak.java:76`; `GameplayTweak.java:116-118` | a |
| 20 | Stamina sprint system | none — **not a historical feature** | UNKNOWN | `GameplayTweak.java:127-137`; 900+ lines of helper code | d |
| 21 | Bow/sweep/miss-timer misc | none stated | UNKNOWN | `GameplayTweak.java:104-112` | d |
| 22 | Lighting engines | **Classic / Alpha / Beta, stated** | VERIFIED | `CandyTweak.java:382-399`; `world_lighting/MinecraftMixin.java:14` | d |
| 23 | Fog, sky, clouds, weather | **Alpha – 1.6.4, stated** | VERIFIED | `tweak/enums/WorldFog.java:14`; `world_fog/LevelRendererMixin.java:59` | d |
| 24 | GUI / HUD / inventory | mixed, often stated | VERIFIED / UNKNOWN | `en_us.json` `eyeCandy.*`; `enums/Overlay.java:14`, `PauseLayout` | d |
| 25 | Particles and sounds | **Beta 1.8 / Classic, stated** | VERIFIED / UNKNOWN | `SoundTweak.java:49,76`; `CandyTweak.java:447` | d |

### How NT actually encodes boundaries

This matters more than any individual row, because it determines how much NT can help at all:

- A scan of all **2095** keys in `en_us.json` found only **78** values mentioning any version-like token.
- A scan of the entire mixin tree found **exactly one** version token in a code comment.
- There is no version-tagged schema, no per-tweak boundary field, and no preset mapping to a specific
  Minecraft version.
- Where a boundary *is* stated it is usually at **era granularity** (Classic, Alpha, Beta), not a version.

The single most precise boundary NT owns is an enum label, not code:
`tweak/enums/WorldFog.java:14` → `'§61.0§r - §61.6.4'`, localised as **"Alpha - 1.6.4"**. Its upper bound
falls inside HX's declared range, which is why fog/sky is the one visual area worth queueing.

### Priority topics the task named that NT cannot answer

Recorded explicitly so they are not mistaken for omissions:

- **Sneak/crouch** — NT has **no sneak gameplay rule at all**. The nearest items are animation-only
  (`AnimationGroup.PLAYER`, side `client`). No crouch height, sneak speed or sneak duration exists.
- **Boat / minecart physics** — NT has six real server-side mechanics (`GameplayTweak.java:170-178`,
  including a detailed two-rail minecart booster description) but **none carries a version**.
- **Mob AI** — eight server-side tweaks (`GameplayTweak.java:26-34,38`); only movement speed states a
  boundary. Several are "disable a modern behaviour", which is weaker than restoring a historical one.

---

## 5. Adaptation classification

The consuming project's rule is *prefer shared implementations with version-specific data; do not force
fundamentally different algorithms into a simple configuration table*. Applying it:

| Class | Count | Meaning |
| --- | --- | --- |
| **a** | 7 | directly adaptable as a version rule (data / parameter change) |
| **b** | 6 | adaptable, but needs a new shared hook |
| **c** | 2 | genuinely a different algorithm — must not be forced into the rule table |
| **d** | 10 | not applicable (client-only cosmetic, conflicts with Moderner Beta authority, or licence-blocked) |
| | **25** | every row classified; three rows carry an additional split, noted in their `adaptationNote` |

### The two class-c items

These are the important negative findings:

1. **Old animal spawning** (`AnimalSpawnHelper.java:73-253`). HX's spawn work is an **allow-list filter
   over candidate entries** (`HistoricalSpawnFilter`, applied from `NaturalSpawnerHistoricalSpawnMixin`
   lines 48–82; `HistoricalSpawnRules.java:122-124`). NT does not filter — it **replaces the algorithm**:
   its own 17×17 chunk polling, its own cap arithmetic (`ANIMAL_SPAWN_CAP * chunks / 256`), its own
   3-attempts × 4-placements loop, its own light floor (`> 8`), its own 24-block spawn exclusion, and
   separate entity-persistence overrides. Forcing that into an allow-list table is precisely the failure
   the project's rule forbids. **The list is class-a data; the algorithm is class c.**

2. **Sword blocking.** A stateful interaction spanning the damage pipeline, an offhand precondition, a
   durability cost and client animation. NT needs a helper, a damage helper and a renderer. This needs
   an ADR, not a record field.

### Client-only features

**Roughly 442 of 527 declarations are client-side or dynamic** and must not enter the server rule table:

- **All sound** (42 tweaks): filtering `SoundTweak` for `side != client` returns **nothing**.
- **All animation and swing** (43 tweaks): registered in the client mixin array.
- **298 of 308 `CandyTweak` entries**; only ten are server-side.
- **`ModTweak`'s config-screen options** — irrelevant to HX.
- **A trap worth flagging:** several features that *look* server-side have a client half a server rule
  cannot substitute for. NT suppresses the attack **miss timer on the client**
  (`combat_player/MinecraftMixin.java:29-53` operates on `Minecraft.missTime`). HX must not assume an
  old-combat behaviour is server-enforceable.

### Why almost everything is `d`

Three independent reasons recur:

1. **Client presentation** — 80% of the mod.
2. **Already implemented in HX from stronger evidence** — mob drops, sprint suppression, spawn
   allow-list, XP-orb suppression, ore placement. NT's equivalents are uncited and sometimes
   *weaker*; e.g. HX's sprint mixin blocks only the *start* request and does not exempt creative or
   spectator, whereas NT rewrites the boolean and exempts both.
3. **Licence** — `reusePermitted = false`, so even a clean architectural fit is a reimplementation.

---

## 6. Ranked recommendations

Five features, ranked by impact against risk. Already-implemented subsystems (sprint suppression,
hunger *suppression*, mob-drop substitution, natural-spawn restriction, ore placement) are excluded by
construction.

### 1. Direct food healing for no-hunger versions — class b

**Boundary.** Before Beta 1.8: food heals hearts directly and there is no hunger bar. HX already owns
the "before Beta 1.8" side from its own b1.8.1 client evidence (`HistoricalVersions.java:123-125`). NT
adds no boundary.

**Evidence.** NT heals on `Player.eat` (`food_health/PlayerMixin.java:29-42`), using the item's modern
nutrition unless a per-item override exists (`:38-41`), and zeroes `exhaustionLevel` and `tickTimer` at
the head of `FoodData.tick` (`FoodDataMixin.java:55-60`) rather than cancelling tick; it also allows
eating only below full health (`:49-60`). HX's gap is documented in its own source:
`HistoricalPlayerRules.java:28-31` — *"Beta 1.7.3 healed directly when the player ate. That is not
implemented yet."*

**Why this is first.** It closes a gap HX has already written down, and the mechanism transfers.

> **Decisive compatibility fact.** NT's *secondary* heal path sits **inside `FoodData.tick`**
> (`FoodDataMixin.java:43-53`). HX **cancels** that method entirely (`FoodDataHistoricalHungerMixin.java:38-51`),
> so that path would be dead on arrival. The path to adopt is the `Player.eat` one, which is **outside**
> tick. Do not uncancel tick to obtain healing — HX's existing suppression, and the per-world gating it
> was designed around, would regress.

**Test.** First a unit test in the project's existing null-object style: for the Beta 1.7.3 profile, a
player below full health eating a known food gains that profile's declared heal amount, while
`foodLevel` and `exhaustionLevel` are unchanged. Then an integration test in `run/` loading a Beta 1.7.3
world **and** a Beta 1.8.1 world in the same server and asserting they behave differently for the same
item — the invariant HX's architecture exists to guarantee. A negative test must confirm an ordinary
world is unaffected.

**Already implemented?** No. The hunger-has-no-effect half is; the healing half is explicitly not.

---

### 2. Version-keyed mob movement speeds — class a

**Boundary.** Alpha/Beta, **as asserted by NT** — one of the few boundaries it states in words.

**Evidence.** `en_us.json` → `gameplay.oldMobSpeeds.info`: *"A value of 0.25 was the movement speed used
for mobs in Minecraft Alpha/Beta other than for zombies and spiders. Zombie (0.23) and Spider (0.3)
speeds have not changed since their introduction."* Data shape at `DefaultListing.java:63-78`; server-side
declaration at `GameplayTweak.java:31-34`.

**Why here.** The shape — entity type → double, keyed by version — is exactly HX's existing
`HistoricalLootRules.DropRule` map. It is the cleanest single fit in the audit.

**Test.** Unit test asserting each declared speed and the fallback for an unlisted entity; integration
test that a mob in a Beta world reports the historical movement-speed attribute while the same entity
type in a Beta 1.8.1 or ordinary world reports the modern value. Because NT's numbers are uncited, the
acceptance criterion should be that values come from an HX-verified source, and the test should fail
loudly while any value is still provisional.

**Already implemented?** No — HX has no attribute data of any kind.

---

### 3. Block/entity interaction restrictions — class b

**Boundary.** **UNKNOWN** from NT for all five toggles. NT's framing is "disable" (these are modern
conveniences); axe stripping and shovel pathing both postdate Release 1.7.10.

**Evidence.** `GameplayTweak.java:143-145` (left-click door/lever/button, server-side) and
`:199-200` (axe stripping, shovel pathing, dynamic). `InteractionHelper.java:16-29` shows a technique
worth copying **in spirit, not in code**: it reads the runtime's own flattenables/strippables maps
through accessor mixins instead of hard-coding block lists, so future additions cannot silently escape
the restriction.

**Why below the two data-only items.** HX has **no interaction-restriction mixin at all** — this would
be the first. NT also applies a half-second rate limit to left-click interaction, which is *state* and
cannot be a plain version boolean.

**Test.** Integration test in `run/`: in a historical world, right-click a log with an axe and assert the
block is unchanged and no sound plays, then left-click a door and assert it opens subject to the rate
limit; repeat both in an ordinary world and assert vanilla behaviour. The negative path is essential —
a mis-scoped interaction hook is the classic way an emulation mod breaks ordinary worlds.

**Already implemented?** No.

---

### 4. Per-version numeric spawn caps — class a

**Boundary.** **UNKNOWN.** NT states only that the vanilla monster cap is 70 and that animal cap 15
"simulates alpha and beta spawn rates".

**Evidence.** `GameplayTweak.java:48-51` (default 90, overriding `MobCategory.getMaxInstancesPerChunk`
via `monster_spawn/MobCategoryMixin.java:27-28`) and `:59` with the `animalSpawnCap` description.

**Test.** Bounded integration test: count naturally-spawned monsters per player over a fixed number of
ticks in a historical world and assert the count stays within the profile's declared cap, while an
ordinary world is unchanged. The test must assert the cap is applied through the **world-derived
version**, not a global — the exact failure mode NT's architecture would import.

**Already implemented?** No. HX restricts *which* types spawn, not *how many*.

> **Do not** adopt NT's animal spawning algorithm here. Only the numeric parameter is proposed; the
> algorithm is class c (§5).

---

### 5. Critical-hit and cooldown gating as version rules — class b, with a deliberate warning

**Boundary.** Post-Beta 1.8 / post-1.7.10. NT states **no** boundary for either; this follows from HX's
version range.

**Evidence.** `GameplayTweak.java:103` and `:105`, both server-side; enforcement at
`combat_player/PlayerMixin.java:26-51` (cooldown) and `:53-67` (critical hit). Both default to `false`.

> **Do not copy NT's enforcement.** It defeats the vanilla critical-hit precondition by **faking a
> climbable result** (`:62-66` returns `true` from the `onClimbable` expression). In HX that could
> perturb ladder and water logic in the same call path. Adopt the *decision*, not the mechanism.

**Test.** Unit test that the flags resolve per version; integration test that a critical hit does not
occur in a Beta 1.7.3 world but does in an ordinary world — **and** that attacking from a ladder in an
ordinary world still behaves correctly. That last case is the specific regression the climbable trick
risks.

**Already implemented?** No. Ranked last because its boundary is inferred, its mechanism must be
replaced, and the practical impact inside HX's current version range is small.

---

### Two incidental gaps NT reveals (follow-ups, not adaptations)

Recorded because they point at places HX should look at its **own** client evidence — not because NT's
values should be used:

1. **Mob drops.** HX covers zombie, chicken, spider and cow from the hash-verified b1.7.3 client
   (`HistoricalLootRules.java:56-65`). NT's classic set also covers **pig** and **sheep**, and sheep wool
   is colour-dependent with a sheared case (`MobLootHelper.java:186-195`). NT also has a **skeleton**
   entry (arrow + bone, max 2) that HX lacks.
2. **NT's drop model is not a drop-in match even where the item agrees.** HX records Beta 1.7.3 as
   `random.nextInt(3)` → 0, 1 or 2 (`HistoricalLootRules.java:20-22`). NT models it as a *uniform float*
   between 0.0 and max — a **different distribution** — and additionally applies a looting multiplier
   that Beta 1.7.3 did not have.

---

## 7. What could not be determined

Full detail, with what would settle each, is in `unknowns[]` in the JSON. The most consequential:

| Unknown | Why it matters | What would settle it |
| --- | --- | --- |
| Which licence covers the audited revision | Determines whether *any* copying is legal | A relicensing commit/ADR on a source branch, a licence statement in the source README, or written clarification from the copyright holder |
| Who holds the LGPL copyright | Needed for attribution even under a permissive reading | An LGPL notice naming a holder, or per-file headers |
| Whether LGPL compliance is satisfiable at all here | GPLv3 text is absent though incorporated by reference | An accompanying GPLv3 copy |
| What `rdh-gg/Nostalgic-Tweaks` was meant to be | Both URL and account 404 | The exact URL seen, or a SHA/artefact from that source |
| Precise boundaries for the vast majority of the 527 tweaks | NT encodes boundaries in prose; only 78 of 2095 lang values mention a version, and there is one version token in a code comment | Primary client evidence — which is what HX's own research already does |
| Whether **any** NT boundary is historically correct | NT cites no client, class, hash or manifest, ever | Hash-verified historical clients, as `HistoricalVersions.java:72-75` already does |
| Provenance of NT's numeric tables | Food heal values, stack sizes, mob speeds and damage formulas carry no citation | Independent client-derived evidence |
| "beta 1.8" vs Release 1.8.9 in NT's claims | NT never distinguishes them, yet that is exactly the boundary HX needs | An upstream statement, or a client comparison by HX |
| Sneak, boat and minecart boundaries | Task-named priority topics | Primary client evidence; NT cannot supply it |
| `OLD_CREATIVE_HOTBAR`, `OLD_ITEM_MERGING`, `ITEM_MERGE_LIMIT` | The three server-side `CandyTweak` items that are not obviously cosmetic | A focused read of the `candy.old_hud` and `candy.item_merge` mixins |

**A caution about the two Beta 1.8 sound boundaries.** NT writes *"Bring back the beta 1.8 chest
sounds"* (`SoundTweak.java:49`) and *"the old early 1.8 beta pickup sound"* (`:76`). NT's own supported
Minecraft versions are all 1.18 or later, so it is reasoning about the past with no client to check, and
it never distinguishes Beta 1.8 from Release 1.8. HX targets **both** Beta 1.8.1 and 1.8.9, so this
ambiguity is material.

---

## 8. Method, honesty and scope

- **Primary sources only.** Every claim is sourced to the cloned repository's own files, git objects,
  resource files or build metadata. External pages were used *only* to establish the canonical URL and
  to test whether the requested URL exists. No wiki, README marketing text, forum post or third-party
  summary was used as evidence for any behaviour or boundary.
- **The README was not used** for the feature inventory, as the task required; the config classes and
  mixin manifests were used instead.
- **Nothing was executed.** No build, Gradle task, Minecraft launch, test suite or mod run. All findings
  come from reading. Consequently **every adaptation proposal is a design proposal and is NOT
  runtime-verified** — the `proposingTest` fields describe tests that still have to be written and run.
- **No code was copied.** Findings are `file:line` citations and behavioural descriptions only. No
  source file, asset or texture was copied into either deliverable.
- **The clone was not modified.** `git status --porcelain` on `.upstream/nostalgic-tweaks` is empty;
  deep reading used a worktree outside the repository tree.
- **`boundaryStatus` semantics** are stated at the top of §4 and again in the JSON, because the
  secondary-source distinction is the easiest thing for a reader to lose.

### Files owned by this audit

- `research/nostalgic-tweaks-audit.json`
- `docs/NOSTALGIC_TWEAKS.md` (this file)

No other file was created or modified.
