# TESTING.md — Minecraft Historical Experience

> **Latest milestone, 2026-10-13 (Beta 1.7.3 passes; leaf litter and vines suppressed at generation time):** **232 tests / 27 suites / zero failures, errors, skipped**, plus **18 verdict negatives and one positive**. A fresh 289-chunk Beta 1.7.3 world reports **`generation.versionAnachronisticBlocks={}` and `generation.anachronisticBlocks={}`** — the first time this version is clean on both checks — with **leaf litter 1,566 → 0**, vines gone and bee nests gone, while era-correct decoration survived (dandelions 12, poppies 4, short grass 345, sugar cane 25, mushrooms, oak and birch). **Suppressed at generation time, never generated and then removed:** the hook cancels the block write, so nothing is placed. It sits on `TreeDecorator.Context.setBlock`, which the 26.3 sources show is the single funnel every decorator writes through — even `placeVine` is just `setBlock(pos, VINE.defaultBlockState()...)` — so one hook covers leaf litter (a `PlaceOnGroundDecorator` Moderner Beta selects on this Minecraft version) and vines (written by the vanilla swamp tree) and any future defect of the same shape. **My first attempt failed to apply** because it redirected the `placeVine` call sites, which sit inside a lambda and therefore a synthetic method. **A redirect on the write, not a check on the state provider:** sampling the provider would consume a random value and change every later placement in the chunk, so reading the state after it is computed keeps generation deterministic. Two further defects surfaced by the checks rather than by looking: **`bee_nest`** (1.16) was not suppressed because bee nests were missing from the mapping table, and the palette is **incomplete** — `cocoa` does not occur in the block class at all although Beta 1.7.3 had it — recorded as a limitation and safe in the direction that matters, since a block absent from both the palette and the mapping is reported UNKNOWN and never suppressed.

> **Previous milestone, 2026-10-13 (playback and seeking verified; the cave biomes were leaking 1.18 content):** **225 tests / 26 suites / zero failures, errors, skipped**, plus **18 verdict negatives and one positive**. **Playback and seeking are verified**: the fork offers no way to open a replay, so a gated client probe drives its public entry points reflectively (`openReplayWorld`, `getReplayServer`, `goToReplayTick`, `getReplayTick`, `getTotalReplayTicks`), keeping the fork off the compile classpath. `./gradlew runClientReplayVerify` needs no arguments and produced **14 meaningful checks**: `totalTicks=2563` (matching the recording's own metadata), `tickAtOpen=0 → seek 2553 → landed`, then **back to tick 0**, `backSeekLanded=true`, `PROBE_RESULT PASS`. **My first version of that test was vacuous** — it sought to two thirds, which was exactly where the replay already was (`tickAtOpen=1708`, `seekRequested=1708`), so it proved nothing, and it re-wrote its verdict every tick (15,016 duplicate checks); both are fixed, and a backward seek now rules out plain fast-forwarding. **Cave biomes were leaking modern content**: MB's presets assign cave biomes by voronoi with points naming `minecraft:lush_caves` and `minecraft:sulfur_*`, and the previous `moderner_beta:none` mitigation is not a fix because `CaveBiomeProviderNone.getBiome` returns **null**. Cave biomes did not exist before 1.13, so the historical choice is a single era-appropriate biome, applied as **new revisions** (Beta 1.7.3 revision 6; revision 1 for Beta 1.8.1, Beta 1.9-pre-3 and Release 1.0/1.1/1.2.5/1.6.4) because a settings preset *is* a revision and worlds already exist on the old ones. **Alpha and the Infdev builds are untouched**, as specified. Verified after also repointing the create-world entry, which was still on the legacy alias and left the fix reachable only by upgrading: `rawRecipePreset=…_content_preview_6`, `generationRevision=6`, `caveComponents=3`, `caveMatches=true`, **`providersInitialized=true`** — the real proof, since `CaveBiomeProviderSingle` reads its biome with `getOrThrow` and MB could not have built it otherwise. **Four test fixtures went stale** because each used `beta_1_7_3_content_preview_6` as its "plausible but never declared" example and it became real; they now use `_content_preview_99`, and the upgrade test derives its revision number from the registry rather than pinning it.

> **Previous milestone, 2026-10-13 (a real replay of a historical world exists — and my earlier Flashback "blocker" was wrong):** **225 tests / 26 suites / zero failures, errors, skipped**, plus **18 verdict negatives and one positive**. **Correction:** two earlier entries claimed recording was "blocked by the environment" and that "Flashback blocks world load". Both were **false**, and the project owner correctly required a causal chain instead of an assumption. Established by measurement: the **baseline client (Flashback disabled) loads the world** (`Preparing spawn area 16→32→63%`, `Time elapsed: 2120 ms`, `level.dat` rewritten); with **Flashback and a 260-second window** the same run **also loads it** (`Time elapsed: 933 ms`, `level.dat` rewritten) — my 100-second window had simply expired while the client, now carrying Flashback plus bundled ImGui/Lattice/commons-math3, was still starting up. **The thread dump is the proof**: the Render thread sat in `renderFrame → FramerateLimiter.limitDisplayFPS`, i.e. normally rendering the main menu, with **no Flashback frames and no network frames in any thread**; the `sessionserver.mojang.com` timeouts were background `Download`-thread noise, not a blocking cause. **One real defect was mine**: the server defaults to `white-list=true` and, against an empty whitelist, rejects every connection — the harness now pins `white-list=false` and `enforce-whitelist=false` explicitly. **A real replay exists**: the client joined a dedicated server running a **historical Beta 1.7.3** world (`Player769 logged in at (194.5, 64.0, -22.5)`) and Flashback exported `run/client/flashback/replays/2026-10-13T03_08_12.zip`, **8,948,061 bytes**, and well-formed — `metadata.json`, `icon.png`, a level-chunk cache and the replay data, with `total_ticks=2563` (≈128 s), `version_string=26.3`, `data_version=5023` and **`historical_experience` present in `modVersions`**. **Exactness matters: the project owner started that recording manually.** My harness did not automate the start, so auto-start is unverified by me in either direction; the fork arms it in `ClientPlayConnectionEvents.JOIN` (the join-a-server event), and the supported route is Flashback's own config rather than the hand-written partial JSON I had been supplying. **Playback has no command** — `/flashback` offers start/finish/pause/unpause/config/mark, and `openReplayWorld` is UI-or-programmatic only — so playback and seek need a **client gametest** (`fabric-client-gametest-api-v1` is already on the classpath) driving `ReplayServer.goToReplayTick`.

> **Previous milestone, 2026-10-13 (the sweep found non-loading versions, and the provider is the discriminator):** **225 tests / 26 suites / zero failures, errors, skipped**, plus **18 verdict negatives and one positive** ([build](<../logs/continuation-325-final.log>)). A bounded per-version verification script (`verify-version-bounded.ps1`, which kills a run that produces no verdict within a deadline) measured four Infdev builds and isolated the cause: **20100227 and 20100325 hang the server during world creation** — "Preparing level" / "Selecting global world spawn", no verdict at all — while **20100420 and 20100611 PASS with complete preset application** (12/3/1 components, all matching, providers initialized). The static discriminator matches exactly: the two that hang declare `provider = moderner_beta:infdev_227` plus a unique `infdev_227_structures` component; the two that work declare `provider = moderner_beta:noise_3d`, as do Alpha and every Release version that loads. All five non-loading versions (Classic 0.30, Classic 0.0.14a_08, Indev 20100223, Infdev 20100227, Infdev 20100325) are **withdrawn** — entries, resources and create-world listings removed — because a version that never loads must not be selectable. **Ten versions remain declared**, six of them runtime-verified. Their extracted data is preserved: 20100227 instantiates no mob of any kind, 20100325 constructs six from a `nextInt(6)` switch, and neither Infdev build has any food at all (verified: the heal method has zero call sites and there is no hunger system). **A harness lesson:** a hang *before* the probe starts never arms the probe's own watchdog, so a naive sweep hangs forever — the bounded runner exists for that reason, and the probe now logs every chunk coordinate before requesting it.

> **Previous milestone, 2026-10-13 (Infdev data extracted under archive provenance; "nothing spawns" broke the model):** **224 tests / 26 suites / zero failures**. All four Infdev builds were retrieved from Omniarchive Vault BETA with SHA-1s checked against **independently published per-version metadata** — not against the downloads themselves — under the provenance standard the project owner authorised. The extraction method was validated by reproducing Beta 1.7.3's 14 mobs and 10 values exactly, and it recomputed `jc.class`'s SHA-1 to the same value a previous worker recorded. Findings: **no Infdev build has any food** (heal method present, zero call sites, no hunger system), and each build's mobs differ — 20100227 instantiates **no mob at all**. That last fact broke the model: an empty allowed-spawn set meant "unrestricted", so the least mob-populated version would have been given modern mobs and looked like success. `VersionRules` now carries an explicit `restrictsSpawns` flag separate from the set, refuses self-contradictory rule sets, and six tests pin it both ways. Method note for these jars: attribute length is a **4-byte** field, not the spec's 2 — with 2 bytes essentially every class fails to parse. **218 tests / 25 suites / zero failures, errors, skipped**, plus **18 verdict negatives and one positive**. 

> **Previous milestone, 2026-10-13 (Flashback loads in a real client; recording blocked by the environment, measured):** The j-w-i Flashback fork now **loads in a real Minecraft 26.3 client alongside this mod** — the live mod list reads `flashback, historical_experience, lattice, moderner_beta` on a real GPU (`OpenGL 3.3.0 NVIDIA 617.42`). Reaching that needed one measured fact: the plain `flashback-0.43.6.1.jar` dies with `NoClassDefFoundError: imgui/moulberry90/callback/ImStrSupplier` because it carries **none** of its dependencies (725 entries, 0 ImGui classes) while `merged-0.43.6.1.jar` bundles them (1827 entries, 187 ImGui classes). The build depends on the merged jar as a **dev-only local runtime dependency** — not compile-visible, never shipped, and its absence is not an error. **Recording is still not verified, and the blocker is environmental**: the client starts, reaches resource reload, and **never loads a world**; the copied world's region files are untouched and no replay directory is created, because the launch stalls on `sessionserver.mojang.com` profile lookups timing out from this machine. Recording needs a loaded client player, so no world means no recording. A second launch through a `clientQuickPlay` run configuration reproduced the identical state, removing argument plumbing as a cause (Gradle's `--args` *replaces* a `JavaExec` task's configured arguments). Unblocking needs outbound session-service access or a dev account requiring no profile fetch — environment changes, not code. The criterion stays **unverified**: this is a blocker report, not a result.

> **Previous milestone, 2026-10-13 (the finite-world cause is measured, and Alpha gains verified gameplay):** **218 tests / 25 suites / zero failures, errors, skipped**, plus **18 verdict negatives and one positive** ([build](<../logs/continuation-303-final.log>)). **The finite-world question is answered.** A world created **directly from Moderner Beta's own world preset** (`generator-settings={"preset":"moderner_beta:classic_0_30"}`, no project wrapper at all) applies its settings completely — 14 chunk / 3 biome / 1 cave components, providers initialized, resolved as an ordinary upstream world — and then **hangs on chunk (0,0) until the server watchdog kills it**. So Moderner Beta's finite path does not generate in this environment: an upstream finding, not a defect here, and it justifies the withdrawal of Classic and Indev on evidence rather than on inability ([log](<../logs/continuation-298-mbfinite.log>)). **Alpha v1.1.2_01 is no longer terrain-only.** Its mob set (10, from entity registry class `ew`) and all six food heals are verified from its own client (SHA-1 `daa4b9f1…`, 897,164 bytes), and **every food value is proved from that jar's own recipes, drop methods or smelting table** rather than by analogy to Beta — apple 4, bread 5, mushroom stew 10, raw porkchop 3, cooked porkchop 8, golden apple 42; no cookie, fish or cake exist (ids 0–90 only). The extraction method was validated by **reproducing Beta 1.7.3's 14 mobs and 10 values exactly** before being trusted on an older jar. **Measured, and different from Beta 1.7.3**: Alpha's restriction removes **wolf and squid** as well as the modern types, its verdict is **PASS at 116 checks**, and bread at a **full** food level is accepted and heals exactly 5 ([log](<../logs/continuation-302-alpha-verify.log>)). **Infdev provenance is a measured blocker**: Mojang publishes no `inf-20100227`/`inf-20100611` in any of the three official manifests (918 versions each; only `inf-20100618` exists and is a different build), so those four versions' mob sets, drops and food values cannot be verified from a Mojang-distributed jar, and their entries say so.

> **Previous milestone, 2026-10-13 (Classic and Indev implemented, measured failing, withdrawn):** **218 tests / 25 suites / zero failures, errors, skipped**. Classic 0.30, Classic 0.0.14a_08 and Indev were declared, with settings presets that inherit Moderner Beta's finite presets whole and **deliberately take no reduced-envelope substitution** — pointing a finite world at the shared 0..127 generator would replace a finite generator with an infinite one. A Classic world **loaded and applied its preset completely** (14/3/1 components, providers initialized) and then hung on chunk (0,0), the very centre, twice — at radius 1 and at radius 0 — until the watchdog killed the run. All three were **withdrawn completely**: registry entries, five resource files, create-world entries and tag entries, because a version that hangs the server must not be selectable. Two harness defects were fixed as a result: the probe now **logs every chunk coordinate before requesting it**, so a hang identifies itself instead of dying silently, and the grid radius is a run parameter (`-PprobeChunkRadius`) because a finite world has no chunk at every coordinate. A harness that can kill what it verifies reports nothing useful when it does.

> **Previous milestone, 2026-10-13 (the operator entry point exists, and its registration is verified at runtime):** **218 tests / 25 suites / zero failures, errors, skipped**, plus **18 verdict negatives and one positive** ([build](<../logs/continuation-274-final.log>)). `/hex status`, `/hex versions`, `/hex upgrade <version>` and `/hex recover` are registered, gated on `COMMANDS_GAMEMASTER`, and reachable in game. Every decision lives in `HistoricalUpgradeService`, unit-tested against real world directories with no server, so the command layer is only argument parsing and text — which is what prevents the rules drifting into a second copy once a probe, a command and any future interface all need them. **Registration is verified against the live dispatcher, not the source**: a command can compile, pass its unit tests and never be registered, because registration goes through a Fabric event no test can fire, and the probe now requires the root plus every subcommand and the version argument — `CHECK command.registered=hex[recover, status, upgrade, versions]`, verdict **PASS** ([log](<../logs/continuation-273-command.log>)). The reports deliberately state what is **not** yet true: a staged upgrade says the stored generator is "deliberately NOT changed yet… rewritten when the server stops", because an operator expecting an immediate change would otherwise conclude it failed. Nine service tests cover the answers, including that an ordinary world says it is not historical and writes nothing, an unknown version is refused with the list of real ones, and a refusal leaves the document untouched ([test](<../src/test/java/net/historicalexperience/server/HistoricalUpgradeServiceTest.java>)).

> **Previous milestone, 2026-10-13 (measurement falsified the upgrade's apply point — and the fix is verified end to end):** **209 tests / 24 suites / zero failures, errors, skipped**, plus **18 verdict negatives and one positive** ([build](<../logs/continuation-264-final.log>)). The generator advance had been wired into **startup recovery** and the unit tests agreed with it. A real run showed otherwise: the log claimed `ROLLED_FORWARD … advanced the stored generator to …_content_preview_5`, but the level-stem file still named `beta_1_7_3` while the metadata said revision 2. The server **reads its level stem into memory before `SERVER_STARTING` fires and re-writes that file when it saves**, silently undoing the advance. **Corrected protocol**: **stage** (backup, journal, publish metadata), **apply on `SERVER_STOPPED`** after the server has finished saving, and **recover on `SERVER_STARTING`** by publishing metadata if needed while **keeping the journal**, since deleting it there would forget the pending advance forever. A second bug fell out of the same change: `applyPendingAdvance` read its expected source preset from the metadata, which recovery may already have advanced, so it refused every deferred advance — the **journal now records the source preset**. Verified in a real world: staged upgrade → probe reports the advance pending → **after shutdown** the journal is gone, metadata is revision 2, and the stored generator names `..._content_preview_5` in all three places → the next start loads that recipe and the verdict **PASSES** ([staged](<../logs/continuation-261-staged.log>), [reload](<../logs/continuation-263-loads.log>)). A fixture that cannot reproduce the interfering party cannot falsify an ordering assumption: the temp-directory tests passed because no server was there to overwrite the file.

> **Previous milestone, 2026-10-13 (restart-safe generation upgrade transaction):** **191 tests / 22 suites / zero failures, errors, skipped**, plus **18 verdict negatives and one positive** ([build](<../logs/continuation-222-final.log>)). A generation upgrade is now a verified transaction: refuse anything unsafe before writing a byte (ordinary world, undeclared target, generation in flight, unfinished previous upgrade, missing file to back up) → back up and **verify** → write a forced write-ahead journal carrying the **complete intended metadata document** → publish atomically → remove the journal. Recovery **rolls forward** and re-hashes the backup manifest *before* applying anything, so a roll-forward onto an unverifiable recovery point is impossible; on failure it changes nothing and keeps the journal. Both crash windows are tested by stopping the upgrade there and running recovery: journal-written-but-not-published (**rolled forward**, idempotent), and published-but-journal-remains (**completed, not re-applied**). One test hashes a region file across a real upgrade and asserts it is byte-identical — the guarantee that the new revision applies to **future chunks only**, so existing chunks, builds, inventories and entities cannot be affected. Twelve tests, no Minecraft runtime needed ([test](<../src/test/java/net/historicalexperience/world/upgrade/UpgradeTransactionTest.java>)). Writing them caught two of my own defects: I had conflated `HistoricalMetadata.revision` (a change counter) with `GenerationRevision.revision` (a recipe number within a version), and my "after the backup" boundary stopped before the journal existed, so it did not model the crash window the scheme exists for. **Still missing: the operator-facing entry point** — there is no command or screen that starts an upgrade.

> **Previous milestone, 2026-10-13 (version coverage 2 → 7, by using the framework):** **179 tests / 21 suites / zero failures, errors, skipped** ([build](<../logs/continuation-210-seven3.log>)). Five versions were added in one pass — Beta 1.9 Prerelease 3, Release 1.0, 1.1, 1.2.5, 1.6.4 — with **no new test written**, because the registry test already covers resource completeness, marker recognition, binding resolution and append-only revision placement for **every** declared version; it validated all five automatically. The blocking question was data, not code: each upstream preset was inspected and all five declare the same `moderner_beta:overworld_128` envelope as Beta 1.8.1, so the verified override set applies unchanged. **Release 1.2.5 establishes and reloads PASS** with complete preset application (14 chunk / 3 biome / 2 cave components matching, providers initialized) and its own resolved identity ([establish](<../logs/continuation-212-r125.log>), [reload](<../logs/continuation-214-r125.log>)). The five new versions are **terrain-only** and say so: simulation rules are deliberately unmodified rather than copied from Beta 1.7.3 or Beta 1.8.1, and each evidence string names both the real upstream foundation and what is still modern (mob set, drops, ore distribution, sea level, structures). A terrain-only profile is **not** a complete gameplay profile.

> **Nostalgic Tweaks audit, 2026-10-13 — reuse NOT permitted.** *This entry is retained as written: it records the evidence and the interim posture at that date. **Its licensing conclusion is superseded.** The owner then directed that the implementations be ported, and the licence position the ported code is distributed under — including the parts of this finding that remain unresolved — is now set out in [LICENSING.md](LICENSING.md) and [PROVENANCE.md](PROVENANCE.md). The factual provenance findings below (which branch holds the source; that it carries no in-tree licence file) are still the evidence this project relies on.* [Audit](<../research/nostalgic-tweaks-audit.json>), [summary](<NOSTALGIC_TWEAKS.md>). The URL originally supplied (`rdh-gg/Nostalgic-Tweaks`) **does not exist** — re-verified by the parent with `git ls-remote`. Canonical is `Nostalgica-Reverie/Nostalgic-Tweaks`, commit `51d6d3dc…` on branch `1.21`. The branch holding the source (917 Java files) has **no licence file**; GitHub's LGPL-3.0 comes from the docs-only `main` branch, which has **zero** Java files, and the commit that added it touches only `.github/`, `README.md` and `LICENSE.txt` on `main` alone (verified by the parent). The only licence ever attached to a source-carrying branch was the **proprietary Minecraft Mod Public License**, which forbids retroactive change. Therefore **no code, asset or texture may be copied**; the audit's proposals are design proposals, not evidence. Its key architectural finding: it reads a **process-global** config, which contradicts this project's world-derived rule invariant, so none of it is adoptable as a configuration mechanism. It is ~80% client-only, encodes no boundaries as data, and cites no client hash for any historical claim.

> **Previous milestone, 2026-10-13 (player-centric rules measured on a real `ServerPlayer`):** **179 tests / 21 suites / zero failures, errors, skipped**, plus **18 verdict negatives and one positive** ([build](<../logs/continuation-207-final.log>)), and a **three-way PASS**: [Beta 1.7.3](<../logs/continuation-198-v173-player.log>), [Beta 1.8.1](<../logs/continuation-200-v181-player.log>), [ordinary control](<../logs/continuation-206-ord.log>). Sprinting and hunger were previously **applied but unexercised**, because a harness with no player could only unit-test their decisions. Fabric API's `FakePlayer` is a real `ServerPlayer` with a synthetic connection, so the probe now acts through the same path a player uses: it asks to sprint and reads back the result, then drives an exhausting food tick. Measured — Beta 1.7.3 **denies the sprint** (`sprinting=false`, 1 recorded suppression) and leaves food at **10** after an exhausting tick (1 tick suppression); Beta 1.8.1 **allows it** (`sprinting=true`, 0) and drains **10 → 9** (0); the ordinary world behaves like 1.8.1 with **neither counter moving**. Each outcome is asserted against the version's own declaration *and* requires the rule's counter to move, so a pass cannot come from something outside the rules. Two harness findings: difficulty had to be **pinned to normal**, because on PEACEFUL vanilla itself will not drain food and the control could not discriminate "no hunger" from "difficulty suppressed it"; and the ordinary control's first runs failed on **my** missing `-PprobeUpstreamPreset=true`, not a code defect — the probe selects its acceptance scope from that property.

> **Previous milestone, 2026-10-13 (version-driven framework, two versions demonstrated):** **179 tests / 21 suites / zero failures, errors, skipped**, plus **18 verdict negatives and one positive** ([build](<../logs/continuation-193-final.log>)). Behaviour is no longer Beta-1.7.3-specific: a single registry declares each version once, and the marker list, profile bindings and all four per-subsystem rule tables are derived from it. Generation content is keyed by **revision** (append-only) and simulation by **version**, and a test asserts that placement — the first attempt put generation rules on the version, which would have made Beta 1.7.3's already-generated revisions 0–3 suddenly exclude features. Beta 1.8.1 was added as a second version with its own upstream foundation (`moderner_beta:beta_1_8_1`), and **both worlds establish and pass at 105 checks** while the same shared hooks select different measured behaviour: zombie drop **feather 9 → rotten flesh 6**, cow **leather 10 → beef 13**, chicken **feather 7 → chicken 6**, structures **disabled → enabled**, version resolved `beta_1_7_3` → `beta_1_8_1` ([Beta 1.7.3](<../logs/continuation-189-v173.log>), [Beta 1.8.1](<../logs/continuation-195-v181.log>)). Three single-version assumptions were found by audit and removed, including a harness rule that demanded `generate-structures=false` for **every** historical world — wrong for Beta 1.8.1, whose fixture it had silently built without structures. Research integration corrected three of my own claims and falsified one provisional rule (**villagers do not exist in b1.8.1**; sprinting there is **unknown**, recorded as inference). Flashback record/replay/seek E2E remains **unverified** and is **not automatable headlessly** — see [FLASHBACK_INTEGRATION.md](<FLASHBACK_INTEGRATION.md>). Most rule domains (blocks, items, recipes, combat, inventory, enchanting, progression, dimensions, audio, visuals, UI) have **no implementation**; decoration, spawn **weights**, building-height rules and the Nether/End remain unverified, and every content-preview verdict still states `preview.fullHistoricalContentAcceptance=false`. See [version framework](<VERSION_FRAMEWORK.md>).

> **Previous milestone, 2026-10-13 (gameplay rule 2 — the era's mob drops):** **171 tests / 20 suites / zero failures, errors, skipped**, plus **18 verdict negatives and one positive** ([build](<../logs/continuation-142-loot-final-build.log>)). A second gameplay rule replaces mob loot in historical worlds with Beta 1.7.3's own drops and suppresses experience, with the drop items read from each mob's `getDropItemId()` in the hash-verified client and each single-letter item field resolved to its name. Verified by **actually spawning and killing mobs through the real damage path**: the historical world dropped only feather/string/leather (`zombie feather 5, chicken feather 5, spider string 5, cow leather 9`) while the ordinary control dropped rotten flesh 13, chicken 8, string 10 and beef 16 plus leather 13 ([historical](<../logs/continuation-144-loot-historical2.log>), [ordinary](<../logs/continuation-146-loot-ordinary2.log>)). **Experience suppression is applied but unexercised**: vanilla awards no experience for a non-player kill, so a first draft that asserted "an ordinary world awards experience" failed and was removed rather than weakened, and the probe now reports the number as non-discriminating.

> **Previous milestone, 2026-10-13 (Beta ore placement implemented):** **165 tests / 19 suites / zero failures, errors, skipped**, plus **18 verdict negatives and one positive** ([build](<../logs/continuation-131-ore-final-build.log>)). Revision 5 now places Beta 1.7.3's own ore population — the transcribed vein algorithm plus the verified call-site parameters, replacing stone only — and excludes every vanilla ore feature and Moderner Beta's emerald so nothing doubles up. Measured at the same seed: diamond **53 → 15**, redstone **31 → 14**, lapis **55 → 18**, with every maximum inside the derived ceiling (coal 57≤132, iron 57≤67, gold 30≤35) and 2788 veins / 50709 blocks recorded ([establish](<../logs/continuation-124-ore5-establish.log>), [reload](<../logs/continuation-126-ore5-reload.log>)). A freshly generated revision-4 world still shows the vanilla overshoot with zero veins attempted, and an ordinary Moderner Beta world is untouched ([revision 4](<../logs/continuation-128-ore4-establish.log>), [ordinary](<../logs/continuation-130-ordinary.log>)). Individual vein positions still do not match a real Beta world at the same seed, because the random is seeded the way vanilla seeds decoration. Decoration, spawn **weights**, building-height rules, the Nether/End and multi-seed coverage remain **unverified**, and every content-preview verdict still states `preview.fullHistoricalContentAcceptance=false`.

> **Previous milestone, 2026-10-13 (ore vein geometry verified, ore gap measured):** **152 tests / 17 suites / zero failures, errors, skipped**, plus **18 verdict negatives and one positive** ([build](<../logs/continuation-112-ore-research-build.log>)). Beta 1.7.3's complete ore vein algorithm is now primary-verified instruction by instruction from the hash-verified client — including that it iterates `i <= size` (an inclusive bound) and replaces **only stone** ([geometry](<../research/beta173-ore-vein-geometry.json>)). The probe now reports per-ore Y ranges and 16-tall band counts, which measured the distribution gap rather than assuming it: **redstone 1..31** against a reachable maximum of about 19, **diamond 3..53** against about 19, **lapis 2..55** against about 34, while coal, iron and gold sit inside their expected ranges ([log](<../logs/continuation-111-ore-histogram.log>)). Ore **distributions** therefore remain unhistorical and are the next slice; decoration, spawn weights, building-height rules, the Nether/End and multi-seed coverage are also unverified, and every content-preview verdict still states `preview.fullHistoricalContentAcceptance=false`.

> **Previous milestone, 2026-10-13 (first gameplay-layer rule — spawns):** **152 tests / 17 suites / zero failures, errors, skipped**, plus **18 verdict negatives and one positive** ([build](<../logs/continuation-102-spawn-build.log>)). Natural spawning in a Beta 1.7.3 world is now restricted to the entity types that existed then, derived from the complete entity registry of the hash-verified b1.7.3 client ([evidence](<../research/beta173-entity-types.json>)). A historical world's live spawn lists contained six provably post-Beta types (bat, turtle, enderman, witch, zombie_villager, glow_squid); after the rule the effective lists are exactly Beta's own (chicken, cow, pig, sheep, wolf; creeper, skeleton, slime, spider, zombie; squid) with original weights intact ([log](<../logs/continuation-104-spawn-historical.log>)). A fresh establish recorded **10 filter invocations and 10 removals, all `minecraft:turtle`**, proving the hook runs during real chunk generation ([log](<../logs/continuation-106-spawn-establish.log>)), while an ordinary Moderner Beta world is provably unaffected ([log](<../logs/continuation-108-spawn-ordinary.log>)). Spawn **weights** are not historical, only the entity set is; no real natural-spawn event was observed because a headless probe has no players. Ore **distributions**, decoration, building-height rules, the Nether/End and multi-seed coverage remain **unverified**, and every content-preview verdict still states `preview.fullHistoricalContentAcceptance=false`. See [gameplay rules](<GAMEPLAY_RULES.md>) and [generation-content design](<HISTORICAL_GENERATION_CONTENT.md>).

> **Previous milestone, 2026-10-12 (structures measured and gated):** **146 tests / 16 suites / zero failures, errors, skipped**, plus **18 verdict negatives and one positive** ([build](<../logs/continuation-94-structure-final-build.log>)). A Beta 1.7.3 world was measured to be eligible for **30 post-Beta structures** across 17 sets, with **16 sets reachable within 64 chunks**, so the requirement that structure generation be disabled is enforced by the content gate and stated in the create-world text. A correctly created historical world passes with `generate-structures=false` and 0 starts ([log](<../logs/continuation-88-rev4b-reload.log>)); a world created before the fix fails with an actionable message ([log](<../logs/continuation-90-prefix-reload.log>)); the legacy v1 world still loads and enforces with a warning and untouched chunks ([log](<../logs/continuation-92-legacy-structure-reload.log>)); and the ordinary Moderner Beta control keeps structures ([log](<../logs/continuation-82-upstream-struct-reload.log>)). Start-level counts measured zero in every world, so they are reported but never relied on. Ore **distributions**, decoration, spawns, building-height rules and the Nether/End remain **unverified**, and every content-preview verdict still states `preview.fullHistoricalContentAcceptance=false`, so this is **not** historical generation acceptance. See [generation-content design](<HISTORICAL_GENERATION_CONTENT.md>).

> **Previous milestone, 2026-10-12 (generation-content revision 4, first content restrictions):** **146 tests / 16 suites / zero failures, errors, skipped**, plus **18 verdict negatives and one positive** ([build](<../logs/continuation-63-revision-aware-build.log>)). A generation-time hook now suppresses researched anachronistic placed features for revisions that declare them, and it is verified by a controlled three-way comparison at an **identical seed**: revision 4 establishes/reloads **PASS** with **272 placements suppressed** and every restricted family absent ([revision 4 establish](<../logs/continuation-72-rev4-establish.log>), [reload](<../logs/continuation-74-rev4-reload.log>)); a freshly generated revision 3 shows the hook **inert** (0 skips) with all those families present ([log](<../logs/continuation-67-rev3fresh-reload.log>)); and an ordinary Moderner Beta world with no marker is **unaffected**, keeping amethyst, copper, tuff, deepslate and sculk ([log](<../logs/continuation-69-upstreamfresh-reload.log>)). Beta's own ores survive the hook. Ore **distributions**, structures, spawns, decoration, building-height rules and the Nether/End remain **unverified**, coverage is one seed and one biome, and every content-preview verdict still states `preview.fullHistoricalContentAcceptance=false`, so this is **not** historical generation acceptance. See [generation-content design](<HISTORICAL_GENERATION_CONTENT.md>).

> **Previous milestone, 2026-10-12 (generation-content revisions 1–3):** **140 tests / 15 suites**, revision 3 establishing and reloading at **91 checks each** with `minY=0`, bedrock at y=0..3, zero deepslate and zero cave-biome content, and revisions 1–2 still loading their own recorded recipes ([revision 3 establish](<../logs/continuation-50-preview3-establish.log>), [revision 3 reload](<../logs/continuation-52-preview3-reload.log>), [revision 1 preservation](<../logs/continuation-30-preview1-preserve.log>), [revision 2 preservation](<../logs/continuation-54-preview2-preserve.log>)).

> **Previous complete-preset milestone, 2026-10-12:** **101 tests / 12 suites**, plus **17 verdict negatives and one positive** ([build](<../logs/continuation-08-direct-preset-test-build.log>)). A separate world selected the actual upstream Beta preset directly and passed establish/reload **73/72 checks**, with complete **12/6/2** component-map equality and initialized providers ([establish](<../logs/continuation-09-upstream-establish.log>), [reload](<../logs/continuation-10-upstream-reload.log>)). It remained ordinary/unadopted, explicitly `scope=upstream-preset`; this is NOT historical-content acceptance. Existing v1 reloaded unchanged with **78 checks** and full-map equality ([legacy reload](<../logs/continuation-11-legacy-reload.log>)); all three real lifecycle refusal/non-adoption controls passed again ([controls](<../logs/continuation-12-lifecycle-controls.log>)). No gameplay ports or Flashback recording/playback/seeking E2E are accepted yet.

> **Previous startup milestone, 2026-10-12:** Startup repair is verified with **95 unit tests**, fresh establish **PASS 77 checks**, separate-JVM reload **PASS 76 checks**, and three real-server lifecycle negative controls. These establish terrain generation/persistence and fail-closed metadata handling, not historical gameplay fidelity. Root cause was listener-order-dependent MB `getSeaLevel()` before provider initialisation, not the vanilla LEVEL_STEM API; [live diagnostic](<../logs/continuation-04-reload.log#L75-L91>). Current [status](<../PROGRESS.md>) supersedes the original [handoff](<../SESSION_HANDOFF.md>). Older evidence below is explicitly historical where labelled.

Owner: implementation worker for the Fabric 26.3 vertical slice.
Scope: build, test levels, exact commands, exact counts and retained evidence for the slice.
Architecture and ADRs are owned elsewhere (`docs/ARCHITECTURE.md`, `docs/ADR-001-*`).

**Correction notice.** An earlier revision of this document claimed that the runtime probe was
blocked by the EULA and that chunk generation was untested. Both statements are obsolete: the
operator explicitly authorised the EULA for the project-local isolated test server, and the probe
has since run to a full pass in two separate processes. The historical evidence is kept; the
current EULA policy (opt-in only, never accepted by the build) is in section 6.

---

## 1. Toolchain and pinned dependencies

| Component | Pinned value | Evidence |
| --- | --- | --- |
| JDK | 25.0.4.1 (`C:\Program Files\Java\jdk-25.0.4.1`) | `java -version` |
| Gradle wrapper | 9.8.1 | `gradle/wrapper/gradle-wrapper.properties` |
| Fabric Loom | 1.18.4, `net.fabricmc.fabric-loom` (unobfuscated variant) | `build.gradle.kts`; needs Gradle plugin-api 9.7.0+, hence 9.8.1 |
| Minecraft | 26.3 (no mappings) | runtime log `Mappings not present!` |
| Fabric Loader | 0.19.5 | `gradle.properties`; runtime log `Fabric Loader 0.19.5` |
| Fabric API | 0.160.4+26.3 | runtime log `fabric-api 0.160.4+26.3` |
| Moderner Beta | Modrinth `Ucgo1y8G` = `5.0.0-alpha.4+26.3` | runtime log `moderner_beta 5.0.0-alpha.4+26.3` |
| JUnit | `org.junit:junit-bom:5.11.4` | `build.gradle.kts` |

Mod: id `historical_experience`, group `net.historicalexperience`,
jar `historical-experience-0.1.0-alpha.1+mc26.3.jar`.

No remapping configurations are used: for the unobfuscated 26.3 target Loom has no
`modImplementation`, so mods are plain `implementation` dependencies, as Moderner Beta's own
`fabric/build-unobf.gradle.kts` does. `fabric-loader` must be a runtime dependency (it supplies
`KnotServer` and the `ModInitializer` SPI); `compileOnly` caused
`ClassNotFoundException: net.fabricmc.loader.impl.launch.knot.KnotServer`.

---

## 2. Exact commands and results

Environment for every command:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-25.0.4.1'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
```

| # | Command | Exit | Result | Retained log |
| --- | --- | --- | --- | --- |
| 1 | `.\gradlew.bat --console=plain test build --rerun-tasks` | 0 | `BUILD SUCCESSFUL`; **89 tests / 9 suites / 0 failed, errors or skipped** | [build log](<../logs/continuation-02-test-build.log>) |
| 2 | `.\gradlew.bat --console=plain runRuntimeProbe` | 0 | Current fresh establish **PASS, 77 checks, 0 failures**, 9 FULL chunks, 270,210 non-air | [establish log](<../logs/continuation-03-establish.log>) |
| 3 | `.\gradlew.bat --console=plain runRuntimeProbeReload` | 0 | Current fresh-JVM reload **PASS, 76 checks, 0 failures**, gold survived, 270,211 non-air | [reload log](<../logs/continuation-04-reload.log>) |
| 4 | `.\gradlew.bat --console=plain probeVerdictNegativeControls` | 0 | 15 negative fixtures rejected + positive PASS accepted | [verdict controls](<../logs/continuation-01-verdict-controls.log>) |
| 5 | `.\scripts\verify-lifecycle-controls.ps1` | 0 | 3 real-server cases passed; each underlying probe command expected exit1, not a successful historical-world probe | [results](<../logs/lifecycle-20261012T220621012-626a6010/results.json>) |
| 6 | `.\gradlew.bat --console=plain test build` | 0 | Latest test run: **95 tests / 10 suites / 0 failed, errors or skipped**, including six new pure diagnostic tests | [latest build](<../logs/continuation-06-diagnostic-test-build.log>) |

Commands 2 and 3 are the Loom run tasks themselves: the verdict gate is wired *into* the run task
rather than into a wrapper task, so invoking the probe directly cannot bypass validation.

The gate accepts a run only when the verdict file's **first line is exactly**
`PROBE_RESULT PASS`, its `runId` equals the id generated for this invocation, and its `mode`
equals this task's mode. A stale pass, a pass from the other mode, a `FAIL`, a missing verdict and
a first line such as `PROBE_RESULT PASSING-BUT-NOT-A-PASS` are each rejected. The current validator
also requires unique core fields, positive integer `checks`, zero `failures` and no contradictory
`failure:` lines. Fifteen negative-control tasks (`probeVerdictNegativeControls`) cover malformed
and inconsistent PASS verdicts as well as the original seven cases; a valid synthetic PASS is a
positive control against an always-rejecting validator. These fixtures do not launch Minecraft or
run the reload probe. Parent ran `gradlew --console=plain probeVerdictNegativeControls`: exit0,
15/15 negative controls rejected and the positive PASS accepted; [continuation log](<../logs/continuation-01-verdict-controls.log>).
The earlier seven-control evidence remains historical.

Verdict file contract (must stay in step with the probe code):

```
PROBE_RESULT PASS | FAIL      first line, exactly
runId=<id>                    must equal this run's id
mode=establish | reload       must equal this task's mode
checks=<n>
failures=<n>
```

The probe receives `historical_experience.probe.runId`, `...mode`, `...verdictFile`,
`...autoStop`, `...verify` (and `...reload` for process 2) as system properties. The id is
generated per invocation from a timestamp plus a random suffix, so it is unique in time and space;
before each run the verdict, run context and abort marker are deleted, so nothing from an earlier
invocation can survive.

Superseded failures retained deliberately:

* `logs/00-dependencies.log` — `modImplementation`/`providers.gradleProperty` misuse. Fixed.
* `logs/02-test.log` — 39 tests, 1 failed (directory at the metadata path reported as `Normal`
  instead of fail-closed). Fixed by separating "is a readable document" (`exists()`) from "is
  anything at this path" (`hasEntry()`).
* `logs/10-runtime-probe.log` — the original EULA-blocked launch.
* `logs/20-runtime-probe-run1.log` — probe crashed reading `getProvider()` off *unmapped* preset
  settings (`Component of type "moderner_beta:provider" supplied a null value`). Fixed.
* `logs/23-runtime-probe-run1.log` — the same class of defect in the upstream comparison helper.
* `logs/30-run1.log` — probe looked for region files at `<world>/region`; 26.3 stores them at
  `<world>/dimensions/minecraft/overworld/region`. Fixed; both layouts are now reported.

---

### Real-server lifecycle negative controls (new continuation harness)

After a successful current establish/reload baseline, run:

```powershell
.\scripts\verify-lifecycle-controls.ps1
```

This harness uses three unique worlds under the already authorised project `run/probe` instance:
unmarked vanilla (no metadata may be created), corrupt metadata (startup must refuse and bytes
must survive), and valid historical metadata with a vanilla/unmarked overworld (startup must
refuse and bytes must survive). It requires the expected lifecycle decision/reason, the expected
nonzero command exit and preservation checks together; an arbitrary launch failure is not a pass.
It backs up and restores the original server properties and retains worlds/logs plus a JSON result
under the ignored logs directory. Do not run concurrently with another build/probe. It never
accepts the EULA or resets existing worlds. Syntax, missing-baseline refusal and all three real
cases passed. Parent independently checked actual actions/reasons, metadata SHA256 hashes, ordinary
startup reaching the intentional historical-only probe rejection, and byte-for-byte fixture
restoration ([results](<../logs/lifecycle-20261012T220621012-626a6010/results.json>)).

## 3. Unit tests — 95 tests, 95 passed, 0 failed

From `build/test-results/test/*.xml`:

| Test class | Tests | Failures | Errors | Skipped |
| --- | --- | --- | --- | --- |
| `...world.metadata.MetadataJsonTest` | 12 | 0 | 0 | 0 |
| `...server.ColumnStatsTest` | 15 | 0 | 0 | 0 |
| `...server.HistoricalMarkerDecisionTest` | 14 | 0 | 0 | 0 |
| `...world.metadata.HistoricalMetadataStoreTest` | 11 | 0 | 0 | 0 |
| `...integration.modernerbeta.HistoricalSettingsPresetDataTest` | 10 | 0 | 0 | 0 |
| `...server.RuntimeProbePathsTest` | 9 | 0 | 0 | 0 |
| `...world.HistoricalWorldStateTest` | 9 | 0 | 0 | 0 |
| `...profile.HistoricalProfilesTest` | 5 | 0 | 0 | 0 |
| `...world.metadata.HistoricalIdentifierTest` | 4 | 0 | 0 | 0 |
| `...integration.modernerbeta.GeneratorFactsTest` | 6 | 0 | 0 | 0 |
| **total** | **95** | **0** | **0** | **0** |

Contract coverage:

* **Schema/metadata roundtrip and validation**: every field survives encode→decode; repeated
  encode is byte identical; unknown schema; missing field; unknown field; malformed JSON; wrong
  JSON type per field; invalid identifier; non-increasing history; `revision` not equal to the
  history head; empty history; blank actor/note; `revision < 1`; duplicate keys (top level and
  per history entry); trailing content after the document.
* **Persistence and preservation**: write/read roundtrip; atomic replace; no temporary files
  left; missing document is an error, not a default; corrupt document preserved byte for byte;
  directory in place of the document reported; `writeIfAbsent` creates once and never clobbers;
  `writeIfAbsent` refuses to replace an unreadable document; parent directory creation.
* **No-clobber publish under concurrency**: eight concurrent creators with *distinct* values
  produce exactly one winner, a readable self-consistent document matching one offered value, and
  no leftover temporary files; six concurrent creators with the same value likewise produce
  exactly one creation. Publishing uses `Files.createLink` (hard link), which fails atomically
  when the destination exists. `Files.move` without `REPLACE_EXISTING` is deliberately *not*
  used, because the Java documentation states its target-exists behaviour is implementation
  specific and that other options are ignored in that case; when hard links are unusable the
  store fails safely instead of degrading to a check-then-write race.
* **World state machine**: no document ⇒ `Normal`; valid ⇒ `Historical` with matching binding;
  corrupt / unknown profile / preset-profile mismatch / newer schema / directory ⇒ `Unreadable`
  with bytes preserved and never deleted; advanced metadata reloads.
* **Preset data contract**: the preset is *minimal* — exactly `chunkSettings`, `biomeSettings`,
  `caveBiomeSettings`, `name` and `description`, and each settings block declares **only** the
  inheritance component; every block inherits `moderner_beta:beta`; **no block references the
  project preset itself** (a self reference makes `ModernBetaSettings.mapPreset` non-terminating);
  no block declares `moderner_beta:provider`, because declaring one overrides the inherited value
  for that block and collapses the other blocks onto it; the name and description use project-owned
  translation keys; the English label exists under the real vanilla world-type key
  `generator.historical_experience.beta_1_7_3` and its text contains "prototype" and "terrain only"
  rather than claiming gameplay; the world preset's three `provider_settings` reference the project
  preset and the nether/end stay vanilla; the preset is appended to the **vanilla**
  `data/minecraft/tags/worldgen/world_preset/normal.json` tag with `"replace": false` and lists only
  the project preset, so other datapacks' presets survive; and **no stray project-namespace
  `historical_experience:normal` tag exists**, because such a tag would look like selectability
  while being read by nothing.

---

## 4. Prior terrain/persistence evidence — previous implementation

The retained runs in this section are older evidence, not the current probe rewrite. Current
acceptance is in section 2; marker moved to (-8,16,-8) and the deterministic fixture has different
non-air counts. Generation/persistence acceptance does not establish complete historical gameplay.

### 4.1 Launch and metadata establishment

Command 2 produced a real Fabric 26.3 dedicated server. Abridged from `logs/31-run1.log`:

```
Loading Minecraft 26.3 with Fabric Loader 0.19.5
Mappings not present!
Loading 44 mods:  - fabric-api 0.160.4+26.3 / - fabricloader 0.19.5
                  - historical_experience 0.1.0-alpha.1+mc26.3 / - minecraft 26.3
                  - mixinextras 0.5.5 / - moderner_beta 5.0.0-alpha.4+26.3
(historical_experience) Historical Experience historical_experience initialising
(historical_experience) World at .\world\. is marked with this project's historical preset
    historical_experience:beta_1_7_3 and has no metadata yet; establishing it.
```

The world was created with `level-type=historical_experience:beta_1_7_3` in
`run/probe/server.properties`, i.e. through the ordinary dedicated-server world creation path
using this project's own `worldgen/world_preset` datapack entry. **No probe-specific launch
property is needed to establish metadata**: the explicit project-owned preset marker on the
loaded generator is the opt-in, and a world without that marker is left completely untouched.

### 4.2 Process 1 — generation, marker, flush (PASS, 41 checks)

From `logs/31-run1.log`:

```
CHECK metadata.schema=1
CHECK metadata.profile=historical_experience:beta_1_7_3
CHECK metadata.generationPreset=historical_experience:beta_1_7_3
CHECK metadata.profileBindingMatches=true
CHECK generator.class=mod.bluestaggo.modernerbeta.level.chunk.ModernBetaChunkGenerator
CHECK generator.presetMarker=historical_experience:beta_1_7_3
CHECK generator.minY=-64
CHECK generator.genDepth=192
CHECK generator.maxY=127
CHECK generator.seaLevel=64
CHECK generator.heightRange=-64..127
CHECK world.seed=2508291534120694762
CHECK registry.historicalPresetPresent=true
CHECK registry.settingsPresetCount=72
CHECK preset.chunkInheritsPreset=moderner_beta:beta
CHECK preset.biomeInheritsPreset=moderner_beta:beta
CHECK preset.caveBiomeInheritsPreset=moderner_beta:beta
CHECK preset.chunkProvider=moderner_beta:noise_3d
CHECK preset.biomeProvider=moderner_beta:beta
CHECK preset.caveBiomeProvider=moderner_beta:voronoi
CHECK preset.resolvable=true
CHECK preset.upstreamTriple=moderner_beta:noise_3d/moderner_beta:beta/moderner_beta:voronoi
CHECK preset.providersMatchUpstream=true
CHECK generation.chunks=9
CHECK generation.nonAirBlocks=310076
CHECK generation.perChunk=(-1,-1)=32890,(-1,0)=34209,(-1,1)=35953,(0,-1)=33298,
    (0,0)=35096,(0,1)=35687,(1,-1)=33374,(1,0)=34440,(1,1)=35129
CHECK marker.pos=8, 126, 8
CHECK marker.expectedBlock=Block{minecraft:gold_block}
CHECK marker.placed=true
CHECK persistence.originChunkResidentBeforeSave=true
CHECK persistence.levelSaveIssued=true (save(null, flush=true, skipSave=false))
CHECK persistence.regionFiles=r.-1.-1.mca:dimensions=22940:flat=absent,...,
    r.0.0.mca:dimensions=40093:flat=absent
HISTORICAL_PROBE PASS checks=41 failures=0
```

Measured: **9 chunks generated, 310,076 non-air blocks**, region files written to disk.

### 4.3 Process 2 — fresh JVM reload (PASS, 42 checks)

Command 3 started a second, independent JVM and server over the *same* world directory, with no
world reset and no re-establishment. From `logs/32-run2-reload.log`:

```
(historical_experience) Historical world detected: profile=historical_experience:beta_1_7_3
    generationPreset=historical_experience:beta_1_7_3 revision=1 historyEntries=1
CHECK metadata.generationPreset=historical_experience:beta_1_7_3
CHECK generator.presetMarker=historical_experience:beta_1_7_3
CHECK generation.chunks=9
CHECK generation.nonAirBlocks=310077
CHECK marker.persistedBlock=Block{minecraft:gold_block}
CHECK marker.survivedProcessRestart=true
CHECK persistence.regionFiles=r.-1.-1.mca:dimensions=1055162:flat=absent,...,
    r.0.0.mca:dimensions=755454:flat=absent
HISTORICAL_PROBE PASS checks=42 failures=0
```

Measured: the metadata document reloaded from disk in a *different process*; the generator
rebuilt with the same project marker; **the gold block placed by process 1 was still present at
8, 126, 8**; the region files grew to ~0.75–1.05 MB as full chunk data was written.

The 310,076 → 310,077 difference is exactly the single marker block placed by process 1, which is
itself a consistency check that the reloaded chunk contains the previous process's edit.

### 4.4 What this does and does not establish

Established: the mod loads on the real 26.3 stack; the project-owned world preset is selectable
by `level-type`; the settings preset loads from the datapack, inherits `moderner_beta:beta` in all
three blocks, resolves without a cycle, and matches upstream's provider triple; real terrain is
generated by Moderner Beta; chunks flush to disk; a second process reloads the metadata, rebuilds
the same generator, and finds the previous process's block edit intact.

Not established: historical fidelity of the generated terrain, gameplay semantics, and any
client/UI or Flashback behaviour (section 5).

---

## 5. Explicit unknowns and non-claims

1. **Terrain fidelity is NOT claimed.** The project preset currently performs no overrides: all
   three settings blocks inherit `moderner_beta:beta` verbatim. Upstream's `beta` preset includes
   modern deepslate/cave and biome-data features; whether that matches a faithful Beta 1.7.3
   experience is a separate, unresolved question owned by the terrain worker. "Runs on the pinned
   Beta 1.7.3 preset" is supported; "is historically faithful Beta 1.7.3" is not.
2. **The Beta build band is not verified.** The generator reports `minY=-64, genDepth=192,
   maxY=127, seaLevel=64`. `docs/MODERNER_BETA_INTEGRATION.md` §4 also describes a declared
   envelope reaching `Y 255`; the measured maxY is 127. Whether the historical build band 0..127
   is faithfully reproduced inside that envelope is established by neither source reading nor this
   probe. A column probe (bedrock floor, waterline, void above 127) is still needed.
3. **No gameplay.** No block availability, crafting, mob, progression or rule behaviour is
   implemented. The profile is explicitly terrain-only, is named as such (`"Beta 1.7.3 terrain"`),
   and a unit test asserts the name does not overclaim gameplay.
4. **No client/UI.** No world-preset UI entry point; selection is via `level-type`. No client code
   is registered.
5. **Probe failure is a build failure.** The run tasks themselves throw unless the verdict file's
   first line is exactly `PROBE_RESULT PASS` and its `runId`/`mode` match the current run. The probe
   writes a verdict file rather than relying on a JVM exit status, because
   `MinecraftServer.halt(true)` joins the server thread (self-deadlock when called from the server
   thread; confirmed in 26.3 bytecode) and `halt(false)` does not await the asynchronous shutdown.
   A missing verdict is never a pass.
6. **Flashback is untouched.** No dependency, no assumption, no test. The target is the owner's
   `j-w-i/Flashback` fork; the fork verification harness under `scripts/` is owned by another
   worker and was not edited here.
7. `Registry 'moderner_beta:blocksource' was empty after loading` appeared during the original
   EULA-blocked launch, before datapack load. It does **not** appear in the successful runs
   (`logs/31-run1.log`, `logs/32-run2-reload.log`) and generation produced 310,076 non-air blocks,
   so it was an artefact of the aborted startup rather than an active defect.
8. **Selectability is declared, not yet clicked.** The world preset is appended to the **vanilla**
   `minecraft:normal` tag at `src/main/resources/data/minecraft/tags/worldgen/world_preset/normal.json`
   with `"replace": false`; that is the tag the create-world dropdown reads (verified: the 26.3 jar
   ships `data/minecraft/tags/worldgen/world_preset/normal.json` listing `minecraft:normal`, `flat`,
   `large_biomes`, `amplified`, `single_biome_surface`, and `extended` includes `#minecraft:normal`).
   A project-namespace `historical_experience:normal` tag would be a second, unread declaration of
   the same intent and is explicitly forbidden by a test. The English label uses the real vanilla
   key `generator.historical_experience.beta_1_7_3`, which is what
   `WorldCreationUiState$WorldTypeEntry` builds via `Identifier.toLanguageKey("generator")` in 26.3
   bytecode, plus the Moderner Beta preset-screen keys; the wording keeps "terrain prototype". The
   `level-type=historical_experience:beta_1_7_3` path is runtime-verified; the create-world *menu*
   path has not been exercised in a client, because no client run is part of this slice.

---

## 6. Reproducing the environment

```powershell
# from the repository root
$env:JAVA_HOME='C:\Program Files\Java\jdk-25.0.4.1'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"

.\gradlew.bat --console=plain test build     # 88 unit tests, then the mod jar

# Runtime probe: two processes over one world, prepared by the fixture helper.
# The helper resolves the project root from its own location: no personal absolute path is used.
.\scripts\prepare-probe-run.ps1 -Mode establish -ResetWorld   # add -AcceptEula to accept the EULA
.\gradlew.bat --console=plain runRuntimeProbe                  # process 1: generate + save
.\gradlew.bat --console=plain runRuntimeProbeReload            # process 2: fresh JVM, same world

# Prove the verdict gate actually rejects bad verdicts:
.\gradlew.bat --console=plain probeVerdictNegativeControls
```

### EULA policy

Nothing in the build accepts Mojang's EULA. `scripts/prepare-probe-run.ps1` writes `eula=false`
by default, and only writes `eula=true` when the operator passes `-AcceptEula` explicitly, which
is a statement that the operator has read <https://aka.ms/MinecraftEULA>. Even then it affects
only the project-local, git-ignored `run/probe` directory; no personal `.minecraft` file, save,
setting or account is touched. A future user build therefore starts with `eula=false`.

### Seed fixture

`scripts/prepare-probe-run.ps1` writes `run/probe/server.properties` with an explicit
`level-seed` (default `2508291534120694762`), `level-type=historical_experience:beta_1_7_3`,
`online-mode=false`, no rcon/query and `sync-chunk-writes=true`, plus
`run/probe/probe-run.json` recording the run id, mode, seed and level type for that run. The seed
is what makes terrain reproducible; the run id is what makes a verdict attributable. With the
default level type the world would be ordinary vanilla terrain, no project marker would be found,
and the probe would correctly fail.

### Assertions to look for

* process 1: `HISTORICAL_PROBE PASS checks=41 failures=0`, `generation.nonAirBlocks=<n>` with
  `n > 0`, and `marker.placed=true`
* process 2: `HISTORICAL_PROBE PASS checks=42 failures=0`,
  `marker.persistedBlock=Block{minecraft:gold_block}`, and `marker.survivedProcessRestart=true`
* both: `runtime probe verdict accepted: runId=<id> mode=<mode> ...` on the Gradle console
* negative controls: five `negative control '<task>' behaved correctly:` lines
