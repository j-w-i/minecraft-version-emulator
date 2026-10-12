# Minecraft version-history research progress

This is the durable research checklist for `minecraft-version-history.md`. A version is **not complete** merely because a heading exists. Mark complete only after cross-checking version page, relevant feature histories, generation state, implementation state, and citations.

## Scope and timeline checkpoint

- [x] Repository inspected; `research/` already exists; requested document did not exist.
- [x] Exact target list transcribed (29 versions: 8 Alpha, 8 Beta releases, six Beta 1.9 prereleases, and 7 Releases including 1.0–1.6).
- [x] Major period boundaries established from Minecraft Wiki timeline: Alpha 30 June–3 December 2010; Beta 20 December 2010–11 November 2011; Release 1.0.0 18 November 2011.
- [ ] Exact date, alias, patch/snapshot distinction recorded for every target version; targeted patch register now covers major Beta and Release deltas but is not exhaustive.
- [x] Feature timeline reconciled with source histories for key corrections (emerald/trading first in Release 1.3; Beta 1.3 beds sleep, Beta 1.4 spawn reset; Release 1.6.4 patch-boundary corrections). Broader audit remains.

## Requested-version status

Status key: `pending` = not yet source-reviewed; `drafted` = evidence captured, requires parent verification; `audited` = completeness/citation review passed.

| Version | Status | Key review areas |
|---|---|---|
| Alpha v1.0.1 | drafted | lost client; redstone, ID collision, ore claims; verify code-level mechanics |
| Alpha v1.0.4 | drafted | winter world type, metadata, freeze/snow behavior; exact artifact test remains |
| Alpha v1.0.6 | drafted | cactus/boat, leaves/sapling, spawn rules; fire/audio unresolved |
| Alpha v1.0.11 | drafted | clay/reeds/slimes, crafting and drops; probabilities unresolved |
| Alpha v1.0.14 | drafted | client reupload, jukebox recipe, dungeon attempts/slime spawn regression |
| Alpha v1.0.17 | drafted | lost client, spider jockey and MP; transient UI bug evidence |
| Alpha v1.1.0 | drafted | multiple reuploads, shared inventory/MP persistence, compass |
| Alpha v1.2.0 | drafted | exact launcher artifact caveat; generator/Nether/biomes, edge-coordinate bug |
| Beta 1.0 | drafted | inventory/tooltips, egg odds/stack, leaf and chunk-save behavior; patch 1.0_01 delta added; generation parameters incomplete |
| Beta 1.2 | drafted | cocoa/editor-only boundary, lapis drops, terrain; _01/_02 patch boundary recorded |
| Beta 1.3 | drafted | sandstone, beds vs respawn boundary, seed selection, debug-spawn reuploads; _01 compatibility delta recorded |
| Beta 1.4 | drafted | wolves/cookies, bed respawn bug, achievements; _01 delta recorded |
| Beta 1.5 | drafted | weather, powered rails/detector rails; _01/_02 split recorded |
| Beta 1.6 | drafted | maps/trapdoors, Nether multiplayer, generation bug, server .exe lost; 1.6.1–1.6.6 patch train recorded |
| Beta 1.7 | drafted | pistons/shears, block-36 crash; 1.7_01/1.7.2/1.7.3 fixes recorded |
| Beta 1.8 | drafted | major gen/mechanic boundaries recorded; exact values, code and patch comparison incomplete |
| Beta 1.9 Prerelease 1 | drafted | numbered-title ambiguity, fortress and inert drops, jump-XP bug; artifact validation pending |
| Beta 1.9 Prerelease 2 | drafted | cauldron prototype, potions, breeding/Hardcore; recipes need full audit |
| Beta 1.9 Prerelease 3 | drafted | original/reupload, UI without enchant effects, brewing/portal partial |
| Beta 1.9 Prerelease 4 | drafted | functional enchant/End, no dragon; reuploads and exact values need check |
| Beta 1.9 Prerelease 5 | drafted | achievements before boss, water regression, mob/Creative updates |
| Beta 1.9 Prerelease 6 | drafted | boss completion, egg/credits, water rollback; dragon AI needs check |
| Release 1.0 | drafted | distinguish full release from pre6; final binary diff and sound/tech incomplete |
| Release 1.1 | drafted | spawn eggs, superflat, beaches, drops/enchantments; patch audit incomplete |
| Release 1.2 | drafted | 1.2.1 stable boundary, height/jungle/iron golems; emerald/trading moved to 1.3; patch family incomplete |
| Release 1.3 | drafted | LAN/integrated server/emerald trading/temples; 1.3.2 delta recorded, technical completeness pending |
| Release 1.4 | drafted | Wither/beacon/commands; patch-specific bugs incomplete |
| Release 1.5 | drafted | redstone components/quartz; tick-level details incomplete |
| Release 1.6 | drafted | horses/resource packs; Java minimum and desert-village/idle-timeout patch details; behavior incomplete |

## Cross-cutting research audit

- [ ] World generation: generator epochs, sea level/height, biomes/climate, surfaces, caves/ravines, ores, population/structures, Nether/End, chunk ordering, seed determinism.
- [ ] Blocks/items: historic IDs/metadata, recipe/loot, obtainable versus registered, drops, physical behavior.
- [ ] Entities: historical health/AI/spawning/drops/variant/taming/despawn; don't infer modern behavior.
- [ ] Player: movement/sneak/sprint, hunger/saturation, health, XP, combat, armor, durability, modes, sleep, death.
- [ ] Redstone/physics: signal/updates, pistons/rails, timing, unintended behavior.
- [ ] Progression/dimensions: Nether portals, stronghold/End accessibility, brewing/enchanting, bosses, achievements.
- [ ] Visual/audio: render/light/weather/GUI/texture/sounds/music.
- [ ] Internal/technical: protocol values/packet behavior, client-server architecture, version-specific patch quirks.
- [ ] Experimental/unused content and temporary regressions.
- [ ] Part IV subsystem comparisons and Part V version-status matrix reconcile with individual entries.
- [ ] Part VI source-to-claim traceability, disagreement list, limitations, original-build test proposals.
- [x] Independent adversarial completeness review performed; its corrections are integrated. Review did not pass definition-of-done because explicit research gaps remain.
- [ ] Automated/manual link-resolution and claim-by-claim citation audit.

## Source log and evidence limits

- `https://minecraft.wiki/w/Java_Edition_version_history` — chronology and era framing; wiki explicitly says it is not an official changelog.
- `https://minecraft.wiki/w/World_generation/History` — exact generator-change milestones; page labels itself WIP, so generation details require per-version and technical corroboration.
- `https://minecraft.wiki/w/Java_Edition_Beta_1.8` — Beta 1.8 page, including pre-release scope and gameplay/generation changes.
- `https://minecraft.wiki/w/Java_Edition_Beta_1.9_Prerelease_3` — build-specific record; identifies original/reuploaded client distinction and feature-state claims.
- `https://minecraft.wiki/w/Java_Edition_1.0.0` — official release date/context and 1.0 contents.
- `https://web.archive.org/web/20110225221214/http://mojang.com/2011/02/23/a-short-demystification-of-the-map-seed/` — contemporaneous primary Jeb/Mojang explanation of Beta 1.3 seed entry and hashing.
- Raw exact-version audiovisual records: Alpha 1.0.14/1.2.0, Beta 1.5/1.5_01/1.6/1.8, Release 1.0/1.2.1/1.5/1.6.1; plus Weather/Sound/Music histories. These are incorporated in the main document with explicit secondary-source caveats.
- Project's `research/feature-timeline.json` and `research/version-boundaries.json` are existing evidence artifacts, but have `WORKER_GENERATED_PENDING_PARENT_VERIFICATION` status. Treat as leads/cross-checks, not authoritative sources until sampled and verified.

## Review checkpoint (2026-10-13)

- An independent adversarial review checked all 29 headings and found high-impact factual boundary errors. Corrected: moved emerald ore/trading from Release 1.2 to Release 1.3; separated Beta 1.3 bed sleeping from Beta 1.4's broken respawn reset; removed the unsupported Release 1.6.4 seed/32-bit-coordinate claim and corrected selected patch boundaries; corrected Beta 1.8 villages to empty of villagers; and fixed Beta 1.9 Pre3 to keep table UI distinct from functional enchanting/XP spending.
- An additional patch-history research pass added a selected-delta register for Beta 1.0_01/1.0.2 through Beta 1.8.1 and Release 1.0.1 through 1.6.4. It is evidence-backed secondary changelog research, not binary behavioral testing.
- Added exact audiovisual boundaries for Alpha music/weather, Beta 1.5/1.6 weather/render fixes, Beta 1.8 lighting/fog/clouds, Release 1.0 sound updates and Release 1.2/1.5/1.6 visual/resource-pack changes. These remain secondary source findings, not original renderer/audio tests.
- Added Mojang’s primary Beta 1.3 seed explanation, pre-Beta 1.8 climate/beach mechanics, Beta 1.8 old-chunk biome regeneration, and corrected public Beta 1.6 ore-quadrant fix versus Test Build 3 clay/vegetation behavior.
- The implementation matrix is expanded and mechanically validated: 29 target columns plus feature label on every row. It remains selective, not exhaustive; omitted mechanics must remain explicitly open.
- Critical completeness review passed as a checkpoint, **not** as definition-of-done: primary code/artifact comparison, exhaustive technical parameters, visuals/audio, spawn/AI, patch-level acquisition, immutable page revisions, and original-build tests remain open.

## Active work

- Alpha versions: per-build draft and evidence integrated; exact ore/distribution, combat, rendering/audio and lost-artifact uncertainty remain.
- Beta 1.0–1.8: per-build draft/patch register integrated; detailed mechanics, values, block routes, worldgen and patch-by-patch completeness remain.
- Beta 1.9 prereleases: each build has 16-field entry with contemporary Jeb date receipts; original artifacts/jars and full recipe/mechanic audit remain.
- Releases 1.0–1.6: stable baselines/selected patch deltas and major corrections integrated; remaining patch coverage/technical details remain.
- Terrain generation: generator epochs, seed/height/structure caveats and source confidence integrated; exact algorithms and original-build tests remain.

Last updated: 2026-10-13 (Australia/Sydney).