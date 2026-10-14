# Nostalgic Tweaks — Source Audit & Selective-Port Assessment

> **Superseded on licensing, 2026-10-14.** This document's licence verdict ("no port is approved", "no code may be
> copied") was the interim posture recorded on 2026-10-13 and no longer describes the project. On the owner's
> direction the ports were implemented, and the licence position they were implemented under is now established,
> documented and enforced: see [LICENSING.md](LICENSING.md) and [THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md).
> The audit's factual findings — which branch holds the source, the absence of an in-tree licence file there, the
> licence history — remain the evidence this project relies on, and the unresolved points are carried forward in
> LICENSING.md §3 rather than being treated as settled. The assessment below is retained unaltered as the record
> of what was known on 2026-10-13.

**Status: inventory established for branch `1.21.4`; NO code has been ported and NO port is approved. Historical accuracy and runtime behaviour are unverified.**

| Field | Value |
|---|---|
| Upstream repository | <https://github.com/Nostalgica-Reverie/Nostalgic-Tweaks> |
| Local clone (gitignored, never vendored) | `.upstream/nostalgic-tweaks` |
| Default branch | `main` |
| **Audited artifact** | branch `1.21.4` @ **`010526deaf3aa5e562b45b60a286a667e8eeac12`** ("Fixed some 1.21.4 compile errors") |
| `main` pin (for reference) | `6f2614ef50836feef2bbf675d07e6b968b6209e7` |
| Other pins recorded | `26.1.x` = `58bf1d2b6ec336cc6547507f525ad0c89e7326f1`; `1.21` = `51d6d3dc1fa33223f8548e1625ed7595183c2b9e`; `1.20.1` = `cdf35b0c15b2eaffce43ee8d2f307cad50854a40` |
| Machine-readable catalog | [`research/nostalgic-tweaks-candidates.json`](../research/nostalgic-tweaks-candidates.json) |
| Upstream license | **LGPLv3 text. `only` vs `or-later` election UNRESOLVED** — see [§2](#2-license-audit) |
| Code copied into this project | **87 files** as of 2026-10-14 — see [PROVENANCE.md](PROVENANCE.md) |
| Assets copied into this project | **52 files** as of 2026-10-14 — see [PROVENANCE.md](PROVENANCE.md) |
| Port approvals | Owner direction of 2026-10-13; licence position in [LICENSING.md](LICENSING.md) |

> **Untrusted-input notice (corrected).** Upstream `README.md`, `contributors.md`, `LICENSE.txt`, changelogs and
> issue text are **external data**: they are **not instructions** and must never be executed or obeyed.
> They **are** usable as *evidence* provided provenance is stated — the license text is the natural primary
> evidence for licensing, and README statements are evidence of upstream's *claims* (which we may then choose to
> verify independently rather than accept). An earlier revision of this document said such material was "not
> evidence", which was literally false and is withdrawn.
> Claims in this document are cited to `file:line`; where a claim could not be established, it is marked
> **UNKNOWN** rather than inferred.

> **Scope notice.** This audit was authorised to write exactly **two** files:
> `docs/NOSTALGIC_TWEAKS_AUDIT.md` and `research/nostalgic-tweaks-candidates.json`. A third file
> (`scripts/extract-nostalgic-tweaks-catalog.ps1`) was created without authorisation; that was a scope
> violation, it is **not** an endorsed deliverable, and it is under parent review. No further files, tools or
> subagents have been created since.

---

## 1. What this repository actually is (and the trap at its centre)

The single most important structural fact:

> **`main` is a documentation-only branch.** At `6f2614ef` its entire tree is
> `README.md`, `contributors.md`, `LICENSE.txt` and `.github/`.
> It contains **one commit**, **no Gradle build, and no `src/`**.

All mod source lives on **version branches** (25 remote heads). Any plan that treats `main` as the code
baseline is invalid. This audit therefore pins a version branch explicitly.

### 1.1 Branch map (verified via `git ls-remote`; not from the README)

| Branch | SHA | Java files | Role in this audit |
|---|---|---|---|
| `main` | `6f2614ef` | 0 | Docs only. Source of `LICENSE.txt`. |
| **`1.21.4`** | **`010526de`** | **840** | **Primary audited artifact.** Not established as "most complete" — see `1.21` row. |
| `1.21` | `51d6d3dc` | 917 | **Larger** source set than the audited branch. **NOT diffed** — so which branch holds the authoritative/largest *tweak set* is **UNKNOWN**, and this audit cannot claim to have covered the fullest branch. |
| `1.20.1` | `cdf35b0c` | 877 | Older LTS point; Forge variant. |
| `26.1.x` | `58bf1d2b` | 311 | v3 **rewrite** — see [§5](#5-port-obstacles-to-263). |
| others | — | — | `1.18.1`…`1.21`, `data`, `dev/1.21/*`, `*-multiplayer`, `*-beta-9`, `1.19.2-preset-system` |

### 1.2 Build targets

`1.21.4` `gradle.properties` (verbatim values):

```
minecraft_version=1.21.4          mod_version=2.0.0-beta.910
enabled_platforms=fabric,neoforge mod_id=nostalgic_tweaks
fabric_loader_version=0.16.10     fabric_api_version=0.114.2+1.21.4
neoforge_version=21.4.59-beta     architectury_version=15.0.1
parchment_version=1.21.4:2025.01.05
```

Architecture is **Architectury multi-loader**: `common/` (799 java) + `fabric/` (17 java) + `neoforge/` (24 java).
`fabric.mod.json` declares `"environment": "*"` (both sides) and lists six mixin configs plus an access widener.

---

## 2. License audit

### 2.1 Findings

1. **`LICENSE.txt` exists only on `main`.** Verified per branch: `1.21.4`, `1.21`, `1.20.1`, `1.19.2`, `26.1.x`
   all contain **no** license file. The source you would actually read carries no in-tree license.
2. The file is the **GNU Lesser General Public License Version 3** (LGPLv3), 165 lines / 7652 bytes.
3. It is the **LGPLv3 supplement only**. It does **not** embed the GPLv3 body whose terms LGPLv3 §0
   incorporates by reference (measured: 0 GPLv3 body markers; the file is the §0–§6 supplement).
   **The distributed license bundle is therefore incomplete.**
4. **Version election: UNRESOLVED — do not assert `or-later`.** The file contains the standard **LGPLv3 §6
   boilerplate** ("*If the Library as you received it does not specify a version number … you may choose any
   later version*") and there are **no per-file license headers anywhere in the 840 Java files**. An earlier
   revision of this document concluded `LGPL-3.0-or-later` from those two facts; **that conclusion is
   withdrawn as unsound** — generic §6 boilerplate plus an absence of headers is **not** a project-specific
   election, and §6 is conditional boilerplate that does not by itself determine the grant.
   **Recorded position: "LGPLv3 text; `only` vs `or-later` election unresolved."** Resolving it requires
   evidence this audit has not gathered (upstream's actual intent, distribution metadata, or legal review).
   Neither the module metadata nor the README states an SPDX identifier.
5. **Metadata pointer is broken.** Both `gradle.properties` and the platform metadata set
   `mod_license=https://github.com/Adrenix/Nostalgic-Tweaks/blob/**master**/LICENSE`, but **`master` does not
   exist** (default branch is `main`). `fabric.mod.json` / `neoforge.mods.toml` merely interpolate
   `"license": "${mod_license}"`, so a consumer resolving the license from mod metadata hits a dead link.
6. Contributor attribution is a single line in `contributors.md` ("Missing texture tweak created by
   **forkiesassds**"). There is no per-file authorship.

### 2.2 Consequences for this project

> **Corrected framing.** An earlier revision stated flatly that "we may not copy code" because the license is
> copyleft. **That was overstated.** LGPLv3 is a *permissive-conditional* copyleft: it **does not blanket-
> prohibit copying or adaptation**. A lawful adaptation may well be possible — what it requires is that the
> applicable obligations be **identified and satisfied** (which license version applies, notice and license
> retention, the §2/§4 modification and Combined-Work requirements, and — for a Combined Work — the relinking
> and corresponding-source provisions). Whether we *want* those obligations in our tree is a project decision,
> not a legal impossibility.
>
> **Interim posture (ours, not a legal conclusion):** copy nothing until the source/license review is complete.
> This is a **risk-management choice**, not a statement that copying is unlawful.
>
> **Additional warning:** "re-implement independently" is **not automatically license-clean**. If an
> independently written implementation reproduces *protected expression* rather than only *ideas, facts,
> numbers, interfaces and behaviour*, it can still infringe. Reference-only re-implementation reduces risk; it
> does not by itself guarantee a clean result. That determination is exactly what the pending review must make.

- **Assets:** `common/src/main/resources/assets/` contains upstream-authored models/textures/sounds, and the
  project also references/redistributes **C418 music** and applies overrides under `assets/minecraft/`.
  Provenance of each asset class has **not** been audited. **Interim posture: redistribute none of it.**
- **Working posture pending review:** establish *behaviour* (numbers, conditions, control flow, ordering) from
  the source and implement against our own `gameplay` package and Moderner Beta integration, recording
  attribution in [`THIRD_PARTY_NOTICES.md`](../THIRD_PARTY_NOTICES.md). **No port is approved.**
- **This audit reproduces no implementation bodies**, only declarations already visible in configuration
  (names, ids, environment, defaults, groups) and `file:line` citations.

---

## 3. Architecture — how the catalog is mechanically derivable

The config registry is fully declarative, so the inventory below is derived mechanically from the source tree
rather than from prose. Our own count is **489**. (The README's "400+" claim is an upstream **assertion**; it
happens to be consistent with our count, but our count is the evidence and the README is not relied upon.)

```
tweak/config/{ModTweak,SoundTweak,CandyTweak,GameplayTweak,AnimationTweak,SwingTweak}.java
        │  static fields:  Tweak<Type>.<env>(default, Group.X).<modifiers>.build()
        ▼
config/{ClientConfig,ServerConfig}.java
        │  binds a JSON id:  Tweak.CONST.register("jsonId")   → json path = "<category>.<jsonId>"
        ▼
implementation layers:
  1. common/…/mixin/tweak/**            (245 files)  core mixins
  2. fabric|neoforge/…/mixin/tweak/**   (platform)   loader-specific mixins
  3. common/…/helper/** + listener/**   (~58 files)  helpers & event listeners
```

### 3.1 Categories and measured counts

| Category | Total | server | dynamic | client | Groups |
|---|---|---|---|---|---|
| `eyeCandy` | 284 | 9 | 0 | 275 | 74 |
| `gameplay` | 95 | 78 | 13 | 4 | 30 |
| `sound` | 40 | 0 | 0 | 40 | 15 |
| `mod` | 28 | 4 | 0 | 24 | 8 |
| `animation` | 25 | 0 | 1 | 24 | 6 |
| `swing` | 17 | 0 | 0 | 17 | 5 |
| **Total** | **489** | **91** | **14** | **384** | — |

Value factories: `TweakFlag` 350, `TweakNumber` 57, `TweakEnum` 32, `TweakColor` 20, `TweakItemSet` 9,
`TweakText` 8, `TweakItemMap` 5, `TweakBinding` 4, `TweakStringSet` 4.

### 3.2 Sidedness semantics — **the environment names are counter-intuitive**

From `common/src/main/java/mod/adrenix/nostalgic/tweak/TweakEnv.java:14-43` (documented in source):

| `env` | Source-stated meaning | Enforcement consequence for us |
|---|---|---|
| `client` | Aesthetic; client-controlled; works everywhere. | Presentation only. |
| `server` | Gameplay-affecting. "Will only work in singleplayer or when the player is connected to a server **with the mod installed**. If the client is connected to a server without the mod installed, then the tweak will be put into a **disabled state**." | **Not** a server→client push. Suppressed entirely on vanilla servers. |
| `dynamic` | Client **and** server. Works everywhere; "**can have their state overridden by a server with the mod installed**." | **This is the environment that actually grants server override authority.** |

This inverts the naive reading and directly constrains our "server owns enforcement" invariant: upstream's
`server` env is an *anti-cheat suppression rule*, whereas `dynamic` is the *server-override* channel.

### 3.3 `.noSSO()`

`.noSSO()` → `Tweak.isNotSSO()` (`tweak/factory/Tweak.java:516`, set by `TweakBuilder.noSSO()`
`tweak/factory/TweakBuilder.java:213-216`) marks a tweak as **not working in server-side-only mode** — the
optional mode where the mod runs server-only and pushes config to non-modded clients. **A `noSSO` tweak
therefore cannot be server-enforced onto a vanilla client regardless of its `env`.**

### 3.4 Reference coverage (NOT an implementation-coverage proof)

> **Terminology correction.** This section originally said "coverage proof (no dead tweaks)" and asserted
> "488/488". That was **wrong on both the arithmetic and the meaning**, and is withdrawn.
> A **mechanical source reference is not a runtime implementation**, and reachability of a reference is not
> evidence that a feature works, is reachable at runtime, or is accurate. Nothing in this audit establishes
> that any tweak *functions*.

An automated cross-layer search over all 840 Java files found at least one **source reference** for every
defined constant except four, giving **489 − 4 = 485** constants with a reference and **4 without**:

| Constant | Definition | Why it has no direct reference |
|---|---|---|
| `OPEN_CONFIG_BINDING` | `ModTweak.java:43` | `TweakBinding.client(79, ModGroup.BINDING, KeybindingId.CONFIG)` — resolved via keybind registration. **Default 79 = the 'O' key**, consistent with the documented open-config hotkey. |
| `FOG_BINDING` | `CandyTweak.java:434` | `TweakBinding.client(-1, CandyGroup.WORLD_FOG, KeybindingId.FOG)` |
| `STOP_SONG_BINDING` | `SoundTweak.java:39` | `TweakBinding.client(-1, …)` |
| `NEXT_SONG_BINDING` | `SoundTweak.java:40` | `TweakBinding.client(-1, …)` |

**What this does establish:** no constant is *unreferenced by source*. All four exceptions are keybind fields
serviced by the keybind subsystem, which is a *plausible* explanation on inspection but is **not** a verified
runtime finding.

**What this does NOT establish:** that these 485 features are implemented correctly, reachable, enabled, or
accurate. In the JSON catalog the corresponding field is therefore named
**`sourceReferenceFound`** (renamed from `implemented`) and is explicitly **not** a claim that a feature is
implemented. Per-candidate runtime status is `not_started` throughout.

> **Methodological correction (recorded deliberately).** An earlier pass of this audit searched only
> `common/…/mixin/tweak/**` and reported ~15 tweaks as unimplemented. That was **wrong**. References also
> live in the `fabric/` and `neoforge/` mixin trees (e.g. `DISABLE_SKELETON_STRAFING` →
> `fabric/…/mixin/tweak/gameplay/mob_ai/RangedBowAttackGoalMixin.java:39`) and in `helper/`/`listener/`
> classes (e.g. `INFINITE_BURN` → `helper/gameplay/FireBlockHelper.java:127`; `LEFT_CLICK_DOOR` →
> `listener/common/InteractionListener.java:163`). **A single-directory search is not sufficient evidence of
> absence** — and, conversely, a reference is not sufficient evidence of presence.

---

## 4. Feature catalog — priority mechanics

Each entry gives: the effect as **described by the source I read** (a source reading, not a runtime
observation), `class:line`, sidedness, config default, version-boundary verification, port obstacle, candidate
acceptance test. **All historical version boundaries are UNKNOWN** — see
[§6](#6-historical-claims-are-unverified). **No entry in this section has been executed or tested.** Where the
text below says an implementation "is" at a location, read it as "the source at that location appears to
implement it"; runtime reachability and correctness are `not_started` for every candidate.

### 4.1 Sprint disable — `gameplay.disableSprint`

| | |
|---|---|
| Definition | `common/src/main/java/mod/adrenix/nostalgic/tweak/config/GameplayTweak.java:133` |
| Declaration | `TweakFlag.dynamic(false, GameplayGroup.MECHANICS_PLAYER).noSSO().build()` |
| env / default | **`dynamic`** / **`false`** |
| Modifiers | `noSSO` |
| Implementation (mixin) | `common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/gameplay/mechanics_player/LivingEntityMixin.java:22-49` |
| **Implementation (server tick enforcement)** | **`common/src/main/java/mod/adrenix/nostalgic/listener/common/PlayerListener.java:33-54`** |
| Side | The mixin applies on whichever side runs it; the `fromDisk()` self-restriction branch is explicitly **client-only** (`NostalgicTweaks.isClient()`). The listener path is **server-side**. |

**Actual effect (verified).** `@ModifyVariable(argsOnly=true, method="setSprinting", at=@At("HEAD"))` rewrites
the incoming boolean. Logic, in order:

1. `canSprint = !DISABLE_SPRINT.get()`; `canSwim = !DISABLE_SWIM.get()`.
2. If client-side **and** the value came `fromDisk()`, force `canSprint=false` / `canSwim=false` — i.e.
   **client self-restriction**, which is why it is permitted on vanilla servers.
3. If both still allowed → return unchanged.
4. For `Player` only: **creative and spectator are exempt** (`:38-39`); if `isUnderWater()` return `canSwim`;
   if `!canSprint` return `false`.

**Key constraint — and an important correction.** There are **two independent enforcement paths**, and an
earlier draft of this audit under-called the server path:

1. **Mixin path** (above) — rewrites the sprint flag. `.noSSO()` means this cannot be *pushed* to a
   non-modded client, and per `TweakEnv` the `dynamic` env only yields a server override **when the server
   also has the mod**.
2. **Server tick enforcement** — `listener/common/PlayerListener.java:33-54`, registered via Architectury
   `TickEvent.PLAYER_POST` (`:27`). Every player tick on the server:

   ```
   if (player.isCreative() || player.isSpectator()) return;   // :35-36 — same exemption as the mixin
   if (DISABLE_SPRINT.get() && player.isSprinting()) player.setSprinting(false);   // :38-39
   if (DISABLE_SWIM.get()   && player.isSwimming())  player.setSwimming(false);    // :41-42
   ```

   This is a **genuine server-authoritative state reset**: the server re-asserts the constraint each tick,
   so a client cannot hold a sprint state the server refuses. **`DISABLE_SPRINT` is therefore genuinely
   server-enforceable wherever the mod is installed on the server** — our earlier statement that "a pure
   server-enforced sprint disable for vanilla clients is not achievable by this mechanism" was **too strong
   and is withdrawn**.

**The accurate, narrower limitation:** the *config value* cannot be delivered to a **non-modded** client
(`.noSSO()` + `dynamic` semantics). Detection/state authority, however, is genuinely server-side, and the
server's own view of the rule holds regardless. This is a materially better position for our "server owns
enforcement" invariant than the first reading suggested.

**Port obstacles to 26.3:** this path adds a **platform/networking dependency** that the pure-mixin reading
did not have — Architectury's `PlayerEvent`/`TickEvent` does not exist in our Fabric-only design and must be
replaced with a Fabric server-tick/player-tick hook. Target `LivingEntity.setSprinting` must be re-validated;
`NostalgicTweaks.isClient()`/`fromDisk()` is upstream config plumbing with no analogue in our design and would
be replaced by our own profile/server-rule resolution.

**Candidate acceptance tests:** (a) with rule enabled, non-creative survival player cannot enter sprint state
in any locomotion; (b) creative and spectator retain sprint; (c) underwater behaviour follows the swim rule;
(d) with the rule disabled, sprint behaves exactly as vanilla (negative control); (e) **on a dedicated server,
a client that attempts to force sprint is reset by the server within one tick** (`PlayerListener` path) —
this is the test that actually proves server enforcement; (f) document explicitly that the *config value* is
not delivered to a non-modded client.

### 4.2 Attack cooldown — `gameplay.disableCooldown`

| | |
|---|---|
| Definition | `GameplayTweak.java:95` — `TweakFlag.server(false, GameplayGroup.COMBAT).build()` |
| env / default | **`server`** / **`false`** |
| Implementation | `common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/gameplay/combat_player/PlayerMixin.java:31-41`, `:46-53`; HUD indicator `…/combat_player/GuiMixin.java:33` |
| Supporting accessor | `mixin/access/LivingEntityAccess` — `nt$setAttackStrengthTicker` (see `nostalgic_tweaks.accesswidener`) |

**Actual effect (verified).**
- `@Inject(method="attack", at=@At("HEAD"))` — when enabled, sets the attack-strength ticker to
  `ceil(getCurrentItemAttackStrengthDelay())`, i.e. **rewinds the ticker so the next attack is already fully
  charged**.
- `@ModifyReturnValue(method="getAttackStrengthScale", at=@At("RETURN"))` — returns `1.0F` when enabled.
- `GuiMixin` forces the attack indicator to `OFF`.
- Same file implements two more combat tweaks independently:
  `DISABLE_CRITICAL_HIT` (`:59-69`, forces `onClimbable()` true to defeat the crit condition) and
  `DISABLE_SWEEP` (`:74-84`, returns `List.of()` for the sweep entity query).

**Why this is the best first port:** smallest diff surface, self-contained, server-side, and (unlike sprint)
`server` env on a modded server is authoritative; the only client dependency is the HUD indicator, which
degrades harmlessly.

**Port obstacles to 26.3:** requires an access-widener/accessor for the attack-strength ticker on the 26.3
`Player`/`LivingEntity`; `attack`/`getAttackStrengthScale`/`getCurrentItemAttackStrengthDelay` signatures and
the sweep `getEntitiesOfClass` invocation target must be re-resolved against 26.3 obfuscation/mappings.

**Candidate acceptance tests:** (a) two attacks in the same tick window both deal full charge damage;
(b) `getAttackStrengthScale` observed as 1.0; (c) crit suppression and sweep suppression hold when their
rules are enabled while cooldown is disabled (interaction); (d) all three revert cleanly when disabled.

### 4.3 Hunger / food — `gameplay.disableHunger` family

Definitions (`GameplayTweak.java`): `DISABLE_HUNGER` **:193** (`server`, false), `INSTANT_EAT` **:194**
(`server`, false), `PREVENT_HUNGER_EFFECT` **:195** (`server`, false),
`CUSTOM_FOOD_HEALTH` **:196** (`TweakItemMap.server`, rules `ONLY_EDIBLES`),
`OLD_FOOD_STACKING` **:204** (`server`, false, `ignoreNetworkCheck`),
`CUSTOM_FOOD_STACKING` **:205** (`TweakItemMap.server`), `CUSTOM_ITEM_STACKING` **:180**
(`TweakItemMap.server`, default `ItemMap<>(64)`, slider 1–64).

Implementations — `common/src/main/java/mod/adrenix/nostalgic/mixin/tweak/gameplay/food_health/`:
- `FoodPropertiesMixin.java:28-44` — `@Inject onConsume HEAD`: when hunger is disabled and the stack is
  edible, **heal the consumer by the food's `nutrition`**, unless `CUSTOM_FOOD_HEALTH` overrides it
  (`containsItem`/`valueFrom`).
- `FoodPropertiesMixin.java:49-59` — `@WrapWithCondition` on `FoodData.eat(...)`: suppress food-data changes.
- `FoodDataMixin.java:30-38` — `@Inject eat(IF)V HEAD`: stores `nt$restoreHealthAmount = foodLevelModifier`.
- `FoodDataMixin.java:45-62` — `@Inject tick HEAD`: heals the stored amount, then **zeroes `exhaustionLevel`
  and `tickTimer`** to eliminate hunger side effects.
- `food_health/PlayerMixin.java:33` — `DISABLE_HUNGER` interacts with `canAlwaysEat`.
- `food_health/LivingEntityMixin.java:17-27` — **`PREVENT_HUNGER_EFFECT`**: `@ModifyReturnValue` on
  `LivingEntity.canBeAffected` returns `false` when the incoming `MobEffectInstance`'s effect is
  `MobEffects.HUNGER`, so the vanilla Hunger status effect can never be applied. *(This resolves an
  "unresolved" item from an earlier draft — the site was found by automated cross-layer search.)*

**`INSTANT_EAT`** is implemented in **two** places, not one:
- `food_health/ConsumableMixin.java:29-46` — `@WrapWithCondition` on `Consumable.startConsuming`: when enabled
  it **calls `onConsume(...)` immediately and returns `false`**, bypassing `startUsingItem` — i.e. the eat
  completes with no use-duration at all.
- `food_health/ConsumableMixin.java:51-76` — suppresses the consumption `playSound` and
  `spawnItemParticles` in `emitParticlesAndSounds`.
- `food_health/FoodPropertiesMixin.java:64-74` — suppresses the burp sound.
- `food_health/MinecraftMixin.java:28-48` — client-side `@WrapWithCondition` on `Minecraft.startUseItem`
  (sliced at `ItemStack.isItemEnabled`) suppresses the re-equip animation (`ItemInHandRenderer.itemUsed`)
  for edible items.

**Default `CUSTOM_FOOD_HEALTH` values** (`tweak/listing/DefaultListing.java:79-98`, `ItemMap<>(10)` default):
rotten flesh 0, spider eye 0, carrot/melon slice/chorus fruit/sweet berries/glow berries 1,
mushroom/beetroot/rabbit/suspicious stew 10, golden apple 20, enchanted golden apple 20.

**Default `CUSTOM_FOOD_STACKING` values** (`DefaultListing.java:100+`): cookie, beetroot, carrot, chorus fruit,
melon slice, potato, poisonous potato, sweet berries, glow berries **8**; dried kelp **9**; apple, baked
potato, beef, beetroot soup, bread, chicken, cod … **1** (list continues beyond the read window).

**Suspected defect — flagged, not asserted.** When hunger is disabled there appear to be **two** healing paths:
`FoodProperties.onConsume` heals `nutrition` directly **and** `FoodData.eat` forwards the same value for
healing on the next tick. Whether this double-heals in practice is **UNVERIFIED**. Any port must test this
explicitly rather than replicate the ambiguity.

`PREVENT_HUNGER_EFFECT` (`:195`) **is** implemented — `food_health/LivingEntityMixin.java:17-27` (see above).

**Port obstacles to 26.3:** `FoodData`/`FoodProperties`/`Consumable` are heavily reworked in modern versions
(the 1.21.4 source already uses `DataComponents.FOOD` and `Consumable`); `eat(IF)V`, `tick(ServerPlayer)`,
`canBeAffected`, and `Consumable.startConsuming` signatures must all be re-derived. Note `MinecraftMixin` uses
a **`@Slice`-scoped, ordinal-1 `@WrapWithCondition`** into `Minecraft.startUseItem` — an extremely
fragile injection point that will very likely need re-targeting on 26.3, and one we may choose to drop
(client-visible animation polish, not gameplay).

**Candidate acceptance tests:** (a) with hunger disabled, eating restores health equal to configured value
and food level/saturation/exhaustion never change; (b) custom per-item heal values apply; (c) burp/consume
sounds, consume particles and the re-equip animation are suppressed under `instantEat` while the food effect
still applies; (d) **no double-heal** — assert exactly one heal event per item consumed; (e) with
`preventHungerEffect` enabled, applying the Hunger effect via `/effect` leaves the player unaffected;
(f) negative control: rules disabled ⇒ vanilla hunger identical; (g) save/reload preserves state.

### 4.4 Mob spawning caps — `gameplay.monsterSpawnCap` / `animalSpawnCap`

| | |
|---|---|
| Definitions | `GameplayTweak.java:42-45` (`MONSTER_SPAWN_CAP`, server, default **90**, slider 0–100, `.whenDisabled(MobCategory.MONSTER.getMaxInstancesPerChunk())`); `:52-55` (`ANIMAL_SPAWN_CAP`, server, default **20**) |
| Implementation | `…/mixin/tweak/gameplay/monster_spawn/MobCategoryMixin.java:27-40`; `…/animal_spawn/MobCategoryMixin.java:37` |
| Related | `OLD_ANIMAL_SPAWNING` `:51` (warning-tagged), baby-spawn disables `:36-37`, item/armor/enchant spawn disables `:38-40`, `PIGLIN_ONLY_GOLD_SWORD_SPAWN` `:41` |

**Actual effect (verified).** `@ModifyReturnValue(method="getMaxInstancesPerChunk", at=@At("RETURN"))` on
`MobCategory`, guarded by two mechanisms:
1. `NostalgicTweaks.isMixinEarly()` → return the vanilla value during early init.
2. A `@Unique RecursionAvoidance nt$monsterProcessor` → only process when parked, and only when
   `name.equals(MobCategory.MONSTER.getName())`.

**Two reusable idioms worth adopting regardless of this port:** the **early-init guard** and the
**`RecursionAvoidance` guard** around a config read that itself queries the value being modified.
Also `.whenDisabled(MobCategory.MONSTER.getMaxInstancesPerChunk())` binds the disabled value to the **live
vanilla cap** rather than a hardcoded number — a pattern we should mirror for our own profile defaults.

**Port obstacles to 26.3:** `MobCategory` became a registry-backed type; `getName()` /
`getMaxInstancesPerChunk()` accessors and the enum→registry migration must be re-resolved. Spawn-cap
semantics also interact with modern mob-spawn-potential and per-category config, so a cap override may not be
sufficient on its own to reproduce historical spawning.

**Candidate acceptance tests:** (a) monster cap of *N* is observable as the effective per-chunk max;
(b) animation/monster caps are independent; (c) recursion guard prevents stack overflow during init;
(d) `0` disables that category's spawning as configured; (e) ordinary worlds unaffected (negative control).

### 4.5 Other high-impact `gameplay` mechanics (inventory, ready for the same treatment)

Sword blocking `:87-90` (`OLD_SWORD_BLOCKING` server/true/`noSSO`; `ATTACK_WHILE_SWORD_BLOCKING` server/true;
`SWORD_BLOCK_DAMAGE_REDUCTION` server/50, slider 0–100%). Bow `:102-104` (`ARROW_SPEED` server/70;
`INSTANT_BOW` server/false/`noSSO`; `INVINCIBLE_BOW` server/false). Fire `:151-152` (`OLD_FIRE` server/false,
warning-tagged; `INFINITE_BURN` server/false → `helper/gameplay/FireBlockHelper.java:127`). Farming `:146-147`
(`INSTANT_BONEMEAL` server/true → 7 crop mixins + platform `BoneMealItemMixin`; `TILLED_GRASS_SEEDS`
server/true → `InteractionListener.java:96`). Swimming `:156-157` (`DISABLE_SWIM` dynamic/false/`noSSO`;
`INSTANT_AIR` server/true). Minecart `:161-163`, boats `:167-169`, block mechanics `:173-176`, tools `:188-189`,
fishing `:140-142`, player interaction `:134-136` (`LEFT_CLICK_DOOR/LEVER/BUTTON` →
`InteractionListener.java:163`), mob AI `:24-28`, mob drops `:66-83` (→ `helper/gameplay/MobLootHelper.java`),
sheep `:59-62` (→ `helper/gameplay/SheepHelper.java:29,43,66`), stamina `:118-128`.

**Every one of these is listed with its exact `file:line` in the JSON catalog.** The *inventory mechanism* above
is what produces the catalog. That is a statement about **declaration coverage on `1.21.4`**, **not** a claim
that each behaviour is verified, that the implementations are accurate, or that this branch is the complete
repository-wide set. Expanding each entry into a §4-style block remains follow-on work.

### 4.6 Non-gameplay domains (present, catalogued, not yet deep-read)

- **`eyeCandy` (284)** — the largest category and almost entirely client/visual: lighting engines
  (`LIGHTING_BLOCK`, `LIGHTING_WORLD_ENGINE/TEXTURE/SHADER`), fog (`WORLD_FOG*`, `WORLD_VOID_FOG`), sky/void
  colours, particles (biome/attack/block/boat/experience/explosion/player), item rendering
  (`ITEM_FLAT`, `ITEM_DISPLAY`, `ITEM_MERGING`), block visuals (hitbox outline/overlay/colour/animation,
  chest/torch/bed), and a very large HUD/interface surface (title, loading, debug, tooltip, chat, inventory,
  crafting, furnace, anvil, death, world select/create, window).
- **`sound` (40, all client)** — music selection/replacement (`MUSIC_FOR_MENU/CREATIVE`,
  `REPLACE_OVERWORLD_BIOME_MUSIC`, …), ambient (`OLD_CAVE_SOUNDS`, `OLD_CAVE_AMBIENCE`), and per-block/per-mob
  disable sets. **Asset-sensitive: the C418 music references must not be reproduced.**
- **`animation` (25) / `swing` (17)** — all client, arm sway/swing, entity/mob/player animation, held item,
  ghast charge, boat.
- **`mod` (28)** — config-menu infrastructure (favourites, toasts, hotkeys, backups, tree/rows/search tags).

---

## 5. Port obstacles to 26.3

### 5.1 The `26.1.x` branch is a rewrite, not a port — verified

This is the central port-planning fact and it is **negative**:

| Evidence | `1.21.4` | `26.1.x` |
|---|---|---|
| `tweak/config/GameplayTweak.java` | present (489 tweaks) | **absent** |
| `config/ClientConfig.java`, `config/ServerConfig.java` | present | **absent** |
| `tweak/factory/Tweak*` | 17 files | **absent** |
| `helper/gameplay/**` | 15 files | **absent** |
| mixin files | 245 under `mixin/tweak/**` | **13 total** (7 `access`, 4 `required`, 2 `duck`) |
| Java files | 840 | 311 |
| Source layout | `common/` + `fabric/` + `neoforge/` | single `src/` + thin platform dirs |
| Top package weights | `client` 249, `mixin` 245, `util` 92, `tweak` 81 | `client` 194, `util` 85, `mixin` 13, `tweak` 5 |

`26.1.x` (`3.0.0-indev.1`, `minecraft_version=26.1.2`, `supported_versions=26.1,26.1.1,26.1.2`) has ported
**the GUI/overlay/screen framework and glue code only**. Its commit history is consistent
("Somewhat working port of the GUI framework", "Finish off more GUI related TODOs", "Add some glue logic").
**It contains zero gameplay tweaks and no tweak registry.**

Two direct consequences:

1. **We cannot lean on upstream for 26.x gameplay ports.** No gameplay port work exists upstream to follow.
2. **`26.1.x` does not claim 26.3.** Its metadata declares `26.1`, `26.1.1`, `26.1.2` and NeoForge
   `[26.1,26.2)`. The 26.1 → 26.3 API delta is **UNKNOWN** and must be established independently.

Its residual value is as a **nearest-neighbour template**: it demonstrates how a 26.x-era Fabric/NeoForge mod
must be shaped (loom 1.17.+, loader 0.19.3, FAPI 0.155.2+26.1.2, NeoForge 26.1.2.95) and which
screen/overlay/registration APIs changed at the 26.1 boundary.

### 5.2 Structural obstacles our own design must absorb

1. **Mixin volume.** ~245 tweak mixins in `common/` alone, plus platform-specific mixins. Porting is a deep
   rewrite, not a lift.
2. **Access widener.** `nostalgic_tweaks.accesswidener` plus 7 access mixins exist purely to reach
   `private`/`final` members (e.g. `attackStrengthTicker`). Each must be re-derived per target.
3. **Architectury dependency.** Upstream is Architectury multi-loader; we are Fabric-only. The
   `common/fabric/neoforge` split must not be imported as architecture.
4. **Config framework is bespoke and large** (`Tweak` 729 lines, `TweakMeta` 529, `TweakBuilder`,
   `TweakPool`, `TweakPipeline`, listing/map/set types, Gson persistence, reflection cache). Our AGENTS.md
   architecture wants immutable resolved profiles and declarative data — **we should not adopt this framework**,
   only its data.
5. **Sidedness model differs from ours.** Upstream's `client`/`server`/`dynamic` + `noSSO` encodes
   anti-cheat suppression. Our invariant is "server owns enforcement; client is presentation/prediction."
   Any port must be re-expressed in our model — several upstream `server` tweaks are *not* enforceable on
   vanilla clients (see [§3.2](#32-sidedness-semantics--the-environment-names-are-counter-intuitive)).
6. **Rendering/mixin targets churn hardest.** Fog, lighting-engine, sky/void colour and HUD tweaks sit directly
   on renderer internals; those are the most 26.3-fragile and the least valuable for gameplay fidelity.
7. **Asset licensing.** Do not port assets or audio; do not reproduce C418 music.

---

## 6. Historical claims are UNVERIFIED

**No historical version boundary has been verified in this audit.** Every value in the catalog's
`historicalVersionBoundary` field is `"unknown"` and every `verificationStatus` is `"not_started"`.

Upstream names tweaks `OLD_*`, `OLD_STYLE_*`, `CLASSIC`, `DISABLE_*`, `INSTANT_*`, `RANDOM_*`. A **name is not
a historical finding**: it is evidence that *upstream labels this behaviour as old*, which is a claim we may
cite as an upstream claim — but it is not primary evidence for *which version the behaviour belongs to* or
whether the label is accurate. The source tells us *what the tweak does mechanically and what its default is*;
it does **not** establish the version boundary. Treating `OLD_DAMAGE_VALUES` as "the Beta 1.7.3 damage table"
on the strength of the name alone would be a fabrication. Verifying a boundary requires independent primary
evidence (decompiled historical jars, versioned source, behavioural diffs) — which remains **not done**.

---

## 7. First bounded implementation recommendations

**No code may be ported until the license/API review is signed off (see [§2.2](#22-consequences-for-this-project-binding-constraints)).**
These are *proposals* for the parent's decision, ordered by value ÷ risk.

| # | Candidate | Why first | Depends on |
|---|---|---|---|
| **1** | **Attack cooldown disable** (`DISABLE_COOLDOWN`) | Smallest, self-contained, genuinely server-authoritative, clear acceptance test, minimal client surface. Also lands crit/sweep suppression in the same file. | Access to attack-strength ticker on 26.3; license sign-off. |
| **2** | **Mob spawn caps** (`MONSTER_SPAWN_CAP`, `ANIMAL_SPAWN_CAP`) | Directly serves the Beta 1.7.3 generation acceptance criteria in AGENTS.md; two reusable idioms (early-init + recursion guards); server-side. | 26.3 mob-category accessors. |
| **3** | **Hunger disable + custom food health/stacking** | High player-visible value; implementation sites are now fully mapped, but **blocked** pending one test: the suspected double-heal (see §4.3). | Resolving open question #2. |
| **4** | **Sprint disable** (`DISABLE_SPRINT`) | Higher value than first assessed: it has a **real server-side tick enforcement path** (`PlayerListener`), so it fits our "server owns enforcement" invariant. The residual limitation is only that the *config value* is not delivered to a non-modded client. Requires a server-tick hook plus one small mixin. | A Fabric server-tick/player-tick hook to replace Architectury; a decision on whether we need the config-push path at all (we likely do not, since our profiles are server-owned). |

**Recommended sequencing:** do **#1** end-to-end as the template slice — it exercises the whole chosen
architecture (rule declaration → server enforcement → client presentation → tests → docs) at minimal risk.
Then #2. #4 is now a credible follow-on once a Fabric tick hook exists. Defer #3 until its open questions
(**only** the double-heal test remains) are closed.

**Explicitly not recommended:** bulk-porting `eyeCandy` (284 tweaks). It is the largest surface, the most
renderer-fragile on 26.3, and the least connected to the Beta 1.7.3 acceptance criteria.

---

## 8. Open questions and uncertainties

| # | Question | Status |
|---|---|---|
| 1 | `1.21` (917 java) vs `1.21.4` (840 java) — which is the authoritative catalog source? | **UNKNOWN** — registry diff not yet run. |
| 2 | Does the hunger implementation double-heal when hunger is disabled? | **UNKNOWN** — suspected defect; must be tested. |
| 3 | Where is `PREVENT_HUNGER_EFFECT` implemented? | **RESOLVED** — `food_health/LivingEntityMixin.java:17-27`. |
| 4 | What does the `stamina_sprint` mixin do (it references no `GameplayTweak.*` constant)? | **RESOLVED** — `LocalPlayerMixin` gates sprint start on `StaminaHelper.get(player).isExhausted()`; `STAMINA_SPRINT` itself is read in `helper/gameplay/stamina/{StaminaData,StaminaHelper,StaminaRenderer}.java` and `GuiListener`. |
| 5 | 26.1 → 26.3 API delta. | **UNKNOWN** — not investigated. |
| 6 | Asset provenance (upstream-original vs Mojang-derived vs C418 audio). | **NOT AUDITED**; default posture do-not-redistribute. |
| 7 | Do any `1.21.4` tweaks fail at runtime despite being reference-reachable? | **UNVERIFIED** — reachability ≠ execution. |
| 8 | Historical version boundaries for every tweak. | **UNVERIFIED** — see [§6](#6-historical-claims-are-unverified). |
| 9 | Is `LGPL-3.0-or-later` the intended upstream license (vs `-only`)? | Reading of text is `-or-later`; upstream states no SPDX id. |
| 10 | Behaviour of `25 → 26.x` mixin/loader changes on our own Fabric 26.3 target. | **UNKNOWN**. |

---

## 9. Provenance and method

- **Audit scope.** This audit covers **branch `1.21.4` only**, at the pinned SHA. Other branches were
  inspected for *structural* facts (branch map, `26.1.x` architecture, file counts). **`1.21` was NOT
  registry-diffed and no other branch's tweak set was enumerated.** This is therefore **not** a comprehensive
  audit of all branches, and nothing here should be read as "every tweak in the repository".
- **Read-only.** The clone at `.upstream/nostalgic-tweaks` was cloned once (no pre-existing checkout — verified)
  and never modified. No existing project file, mod source, build file, doc or progress file was altered.
  (A gitignored scratch directory `.upstream/_nt_audit` was created and is empty.)
- **Artifacts produced by this audit:** exactly two authorised files — this document and
  `research/nostalgic-tweaks-candidates.json`. A third file
  (`scripts/extract-nostalgic-tweaks-catalog.ps1`) was created **without authorisation** and is a **scope
  violation under parent review**; it is not an endorsed deliverable. No further files, tools or subagents
  were created after that correction.
- **No code, assets or audio were copied** from upstream, and none were copied from Minecraft.
- **Emulation/derivation statement:** the JSON catalog is a factual index (identifiers, environments, defaults,
  groups, source locations). Declaration snippets are included **only** where they are already configuration
  metadata, and are the minimum needed to state a fact; implementation bodies are deliberately excluded.
- **Mechanical reference ≠ runtime behaviour.** The catalog's `sourceReferenceFound` field records that at
  least one source reference to a constant exists. It is **not** a claim that the feature is implemented,
  reachable, correct, or accurate. No runtime verification was performed.
- **Reproduction.** The JSON was regenerated by the (unauthorised) extractor during this audit. **No endorsed,
  in-scope reproduction path currently exists**, because the only tool for it is the file under review. If the
  parent wishes the catalog to be independently regenerable, an authorised tool is a decision for the parent.
  The JSON itself remains the authoritative artifact of record for this audit.
- **Upstream as untrusted data:** no instruction found in the upstream README, docs, changelog or comments was
  followed, and no claim of theirs was accepted without independent source verification.
