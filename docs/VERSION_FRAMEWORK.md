# Historical version framework — architecture and roadmap

Status: **framework in place and demonstrated with seven declared versions; two have researched gameplay rules, five are terrain-only; most rule domains are still unimplemented.** This document records the audit that produced it, what is reusable versus version-specific, and the ordered roadmap. It replaces the implicit "Beta 1.7.3 is the project" assumption that the earlier code carried.

## 1. Audit: what was actually version-specific

The audit inspected the code rather than the prose. The result was more encouraging than expected in two places and worse in one.

**Already reusable, and kept.** The profile/revision split and its append-only revision discipline were sound. So were the four (now six) generation and simulation hooks, each of which resolves the era from the object it is modifying — the generator's own durable marker, or the level a player is in — rather than from ambient state. Ordinary-world non-interference was enforced by those hooks rather than by convention, so it survived the refactor unchanged.

**Version-specific, and now data.** Five separate tables decided behaviour: the marker list, the profile/binding table, and one table per subsystem (`HistoricalFeatureExclusions.BY_REVISION`, `BetaOreRules.BY_REVISION`, `HistoricalSpawnRules.BY_PROFILE`, and `HistoricalLootRules`' profile set). Adding a version meant editing six places with two different keying conventions, and a missed one failed at world-load time rather than at build time. All five are now derived from a single registry, and a test asserts the invariants a new version must satisfy.

**Worse than expected.** Three hardcoded assumptions were load-bearing and invisible:

1. `ModernerBetaAdapter` compared every world against the **Beta 1.7.3** upstream preset. Correct for one version; it rejects every other version's world for inheriting the *right* terrain. The expected preset now comes from the world's own declared recipe.
2. The adapter had a `preview` flag that really meant "does this recipe declare overrides?" — a question the data already answered. It now always composes the declared deltas, which also removes a case where a legitimate third delta was rejected.
3. The probe demanded `generate-structures=false` for **every** historical world. That is right for Beta 1.7.3 and wrong for Beta 1.8.1, which introduced mineshafts, villages and strongholds — so the harness silently built the Beta 1.8.1 fixture without structures. The requirement is now a version rule, asserted against the version the world declares.

**Verification overhead.** Several tests pinned one version's marker list, binding count and rule sets, so every new version broke them. Those pins are now derived from the registry, and the per-version churn moved into one test whose whole purpose is to state what a new version must satisfy. The old pins were not weakened: the invariants they protected (the frozen declaration does not change, ordinary worlds resolve to nothing, earlier revisions keep their rules) are asserted generically.

## 2. The framework

```
version/
  HistoricalVersion       one version: id, era, chronology, simulation rules, generation revisions
  HistoricalVersions      the registry; the only place a version is declared
```

The two axes are the load-bearing design decision:

| Axis | Holds | Keyed by | May change? |
| --- | --- | --- | --- |
| `GenerationRevision` | generation content: excluded features, own ore population, upstream foundation | the persisted recipe alias | **never** — a revision is append-only, and new behaviour is a new revision |
| `VersionRules` | simulation: spawn set, mob drops, experience, sprinting, hunger, structures | the version | yes — it changes what happens next, not what is stored |

Putting generation rules on the version was the first thing I did and it was wrong: it would have made revisions 0–3 of Beta 1.7.3 suddenly exclude features, changing what already-generated worlds do. The test `generationRulesArePerRevision` now asserts the correct placement.

Adding a version is one entry plus the two resource files its revisions need; the registry test fails if either is missing. That is the scalability claim, and it is enforced rather than documented.

**It was then tested by using it.** Five further versions were added in one pass — Beta 1.9 Prerelease 3, Release 1.0, 1.1, 1.2.5 and 1.6.4 — taking the registry from 2 to **7 declared versions** with no new test, because the existing registry test already checks resource completeness, marker recognition, binding resolution and append-only revision placement for every declared version. The blocking question was not code but data: each of those upstream presets had to be inspected to confirm it declares the same `moderner_beta:overworld_128` envelope as Beta 1.8.1, which is what makes the already-verified override set — inherit the upstream preset, disable deepslate, remove the cave-biome injection — applicable unchanged. It does, for all five. Release 1.2.5 was then verified at runtime: it **establishes and reloads PASS** with complete preset application (14 chunk, 3 biome and 2 cave components all matching, providers initialized) and its own resolved identity ([establish](<../logs/continuation-212-r125.log>), [reload](<../logs/continuation-214-r125.log>)).

Those five are **terrain-only**: their simulation rules are left unmodified rather than copied from Beta 1.7.3 or Beta 1.8.1, because those rule sets were researched for their own versions and applying them elsewhere would be a guess dressed as data. Each entry's evidence string names the real upstream foundation *and* states what is still modern — mob set, mob drops, ore distribution, sea level and structure placement. For Release 1.0 through 1.6.4 the modern defaults are also close to correct in the ways that matter most, since sprinting, hunger and experience all exist by then.

## 3. Version boundaries

[research/version-boundaries.json](<../research/version-boundaries.json>) covers five versions (b1.7.3, b1.8.1, 1.0, 1.2.5, 1.8.9) across 21 mechanics, each marked **verified**, **inferred** or **unknown**, with the client jar hash and the class/method/offset or registry list behind every verified row. Headline results: 97 verified, 4 inferred, 4 unknown.

It also corrected three of my own claims, which is why it was worth commissioning: `crafting` is block-derived and absent from the item registry's own strings; my Beta 1.7.3 item list was missing `bow`, `egg`, `bed`, `map` and `cat`; and — the one that changed behaviour — **villagers do not exist in b1.8.1** (they arrive in 1.0), so the `minecraft:villager` entry I had provisionally put in the Beta 1.8.1 spawn set was removed. Sprinting remains **unknown** for b1.8.1: the jar's sprint fingerprint could not be located, so `allowsSprinting=true` there is documented version history rather than an observation, and the rule's evidence string says so.

Moderner Beta's own inventory supplies the generation foundation for the whole range: 81 published presets including `classic_0_0_14a_08`, `indev`, `infdev_227`/`325`/`415`/`420`/`611`, `alpha`, `beta`, `beta_1_1_02`, `beta_1_8_1`, `beta_1_9_pre_3`, `early_release`, `release_1_0_0`, `release_1_1`, `release_1_2_5`, `release_1_6_4`, plus PE and legacy-console variants. There is **no** exact preset for Release 1.7.10 or 1.8.9 — the nearest are `release_1_6_4` and `release_1_12_2` — so those targets remain an honest gap rather than a nearest-version substitution.

## 4. Roadmap

Ordered by visible, playable difference per unit of work, with the gate each slice must pass.

| # | Slice | Why now | Gate |
| --- | --- | --- | --- |
| 1 | Player harness: a headless `ServerPlayer` so player-centric rules can be observed | four rules (sprint, hunger, experience, combat) cannot currently be exercised, only unit-tested | a spawned player's food/sprint state is measured |
| 2 | Hunger completion for no-hunger versions (eat-to-heal from verified Beta heal values) | eating currently does nothing in a no-hunger world | eating heals, hunger does not exist |
| 3 | Combat: critical hits and Beta's damage rules, per version | the boundary is researched (1.8.9 verified, earlier inferred) | a measured damage value per version |
| 4 | Block and item availability per version | the registries are extracted for five versions | an unavailable block cannot be placed/obtained in that version's world |
| 5 | Structure and decoration content rules per version | Beta 1.8.1 structures are currently uncurated | measured structure starts per version |
| 6 | Crafting and smelting availability | same registry data | a version cannot craft an item it lacked |
| 7 | Remaining versions: Classic, Indev, Infdev and Alpha as registry entries | these are the pre-Beta eras, and they need `sprint=false` / `hunger=false` rules rather than the unmodified defaults the Release versions can honestly use | each version loads, resolves and passes its own gate |
| 8 | Progression and export; Flashback E2E | mandatory, and Flashback needs a real client run | see [FLASHBACK_INTEGRATION.md](<FLASHBACK_INTEGRATION.md>) |

**The upgrade is now an operator feature, not only a test path.** `/hex status`, `/hex versions`, `/hex upgrade <version>` and `/hex recover` are registered on the live server, permission-gated, and verified present by the probe reading the running command tree rather than the source — because a command can compile and pass its tests while never being registered. Every decision lives in a service that is unit-tested without a server; the command layer only parses and renders. The reports state what is not yet true, so a staged upgrade cannot be mistaken for a failed one.

Generation fidelity continues in parallel and does not block the list above: decoration fidelity, historical spawn **weights**, ore-band distributions and the Nether/End remain open for Beta 1.7.3 itself.

## 5. What is still not true

- **Ten versions are declared, not the whole range.** Declared and runtime-verified: Beta 1.7.3, Beta 1.8.1, Alpha v1.1.2_01, Infdev 20100420, Infdev 20100611 and Release 1.2.5. Declared, loading verified by the registry test but **not yet run in a world**: Beta 1.9 Prerelease 3 and Release 1.0, 1.1 and 1.6.4 — they share the `noise_3d` provider and envelope of versions that do load, which is a reason to expect them to work, not evidence that they do. Withdrawn after measurement: Classic 0.30, Classic 0.0.14a_08, Indev 20100223, Infdev 20100227 and Infdev 20100325. Missing for other reasons: Release 1.7.10 and 1.8.9, whose nearest Moderner Beta presets are `release_1_6_4` and `release_1_12_2` — a nearest-version substitution that has deliberately not been made.
- **Only three versions have researched gameplay rules.** Beta 1.7.3, Beta 1.8.1 and now **Alpha v1.1.2_01**, whose ten-mob spawn set and six direct food heals are verified from its own client with version-internal proof for every value. The other nine are terrain-only, and five of those assert *inferred* pre-Beta rules — no hunger, no sprinting, no experience, no structures — justified by monotonicity between the two verified Beta boundaries rather than measured for those versions. A terrain-only profile is explicitly not a complete gameplay profile, and an inferred rule is explicitly weaker evidence than a measured one.
- **Evidence standard for old jars, decided 2026-10-13.** Mojang publishes no Infdev build before `inf-20100618`. The project owner authorised **archive-hosted jars** on explicit conditions: the SHA-1 must be verified against a value published by a source *independent of the download* (an archive index or hash list, not the file itself), the archive, URL and hash source must be recorded per jar, and provenance is labelled as archive-provided rather than Mojang-distributed. A jar whose hash has no independent publication must be flagged as such, not silently treated as verified. This is a deliberate widening of the standard for builds Mojang no longer distributes, and it is recorded here so it cannot quietly become the standard for builds Mojang *does* distribute.
- **The four Infdev versions are pending that extraction.** Their mob sets, drops and food values are being read from archive-hosted copies under the standard above. Until those values exist, the four entries remain terrain-only and say so.
- **Classic and Indev cannot be offered yet, and the cause is upstream.** They are finite worlds, and a world created directly from Moderner Beta's own finite preset hangs on chunk (0,0) after applying its settings completely — with no project code involved. Recorded with the evidence so it is not re-attempted expecting a different answer.
- **Infdev 20100227 and 20100325 were implemented, measured failing, and withdrawn.** They do not load: both hang the server during world creation — "Preparing level" / "Selecting global world spawn" — and produce no verdict at all. The provider is the discriminator, and that is measured rather than inferred: those two presets declare `provider = moderner_beta:infdev_227` plus a unique `moderner_beta:infdev_227_structures` component, while **20100420 and 20100611 declare `provider = moderner_beta:noise_3d` and both PASS with complete preset application**. So the same shape of finding as Classic and Indev: an upstream preset that does not generate in this environment. Their extracted data is preserved — 20100227 instantiates **no mob of any kind**, 20100325 constructs six from a `nextInt(6)` switch, and neither has any food — so the research is not lost and can be reinstated if the provider is fixed. Their registry entries, resources and create-world listings were removed, because a version that never loads must not be selectable.
- **Beta 1.8.1's rule set is partially verified.** Its entity registry, hunger and experience are verified; its sprinting, its drop associations and all of its counts are inferred. Its evidence string says so, and nothing in the framework currently surfaces that distinction to a player.
- **Beta 1.7.3 is the only version with researched mob drops, spawn restrictions, direct food healing and its own ore population.** Every other version leaves those modern, and each entry names the gap rather than borrowing values from a neighbour.
- **Upgrade transactions now exist and are verified end to end.** A staged upgrade takes a verified SHA-256 backup, writes a forced write-ahead journal carrying the complete intended metadata *and the preset the world was on*, publishes the metadata, and defers the generator rewrite to `SERVER_STOPPED` — because a measured run showed the server had already read its level stem and would overwrite any rewrite made while it was running. Recovery at startup publishes the metadata and **keeps the journal**, so the pending advance is never forgotten. Verified in a real world: stage → journal pending → shutdown applies → the next start loads the destination recipe and passes. Twelve unit tests stop the upgrade at each phase boundary and run recovery on the result, and one asserts region files are byte-identical across an upgrade, so the new revision provably applies to future chunks only. What is still missing is the operator-facing entry point: the runtime path is exercised by the probe, not by a player.
- **Most rule domains have no implementation at all**: blocks, items, recipes, combat, inventory, enchanting, progression, dimensions, audio, visuals and interface.
