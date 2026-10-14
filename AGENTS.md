# Minecraft Historical Experience — agent contract

## Purpose and authority
Emulate Pre-Classic through Release 1.7.10, plus the subsequently requested Release 1.8.9 target. Latest direct human instruction (2026-10-13) retargets the active implementation to Minecraft Java 1.21.1 **at the repository root**, superseding the former 26.3-only constraint. **The 26.3 tree has since been deleted at the owner's instruction: 1.21.1 is the ONLY target and there is no reference tree left to compare against.** Immediate scope is ALL the generation features that were ported from it, explicitly INCLUDING world upgrading per the owner's follow-up.

Two later owner requests are the current active scope, in addition to the above:
1. **Port the Nostalgic Tweaks features** into this project **by copying the upstream implementations directly and applying them per historical version**, matching `research/nostalgic-tweaks-candidates.json`, `research/nt-port-worklist.json` and `research/feature-timeline.json`. Owner-stated scope: gameplay, visual (client) and other NT features. For village generation the owner designated https://github.com/MaxTechnik3796/old_villages (MIT as declared in its own `gradle.properties`; it ships no LICENSE file), pinned at `7578013ec061303982539fac7606df67577ffb26` in `.upstream/old_villages`. **The licence position is now implemented, not pending**: the project is `LGPL-3.0-or-later`, the incorporated Nostalgic Tweaks material remains `LGPL-3.0-only`, every ported file carries a header naming its upstream file, and the notices and obligations are in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) and [docs/LICENSING.md](docs/LICENSING.md) — see "Licensing" below before copying anything. Behaviour is still gated per version rather than by a global config flag.
2. **Make every one of the ten profiles content-correct, not just Beta 1.7.3**: no structure its era lacked and no block its era lacked, cross-referenced against `research/feature-timeline.json` (the feature source), `research/block-palettes/` and `research/version-boundaries.json`. Structures and blocks are judged per profile. Generation revisions are append-only, so a new exclusion is a NEW revision plus its settings/world preset, never an edit to an existing recipe.
 Verify actual operator commands, backup/journal/restart, destination future generation and preservation of existing blocks/containers/player data. GPT-6.1 Sol implements and verifies this repair directly before handoff.

Three-agent workflow (human update 2026-10-12):
- DeepSeek 4.1 Flash is the primary substantial implementation worker: Fabric integration/mixins, complex gameplay systems, large changes, difficult build/runtime/API investigation. Verified current route: `deepseek-official/deepseek-flash` (DeepSeek-V41-Flash).
- Qwen3.6-35B-A3B handles initially narrow research/data extraction, isolated small features, focused behavior tests, recipes/loot tables and mechanical refactoring. Expand scope only after reviewed evidence of reliability; narrow it after repeated defects. Discover the actual provider/model route before delegation. At the 2026-10-12 checkpoint the old session's catalog exposed only DeepSeek; inspecting `deepseek-official/Qwen3.6-35B-A3B` returned `not allowed for this Session`. This is a historical observation, not a permanent prohibition: a fresh/forked session MUST rediscover its actual routes before delegation. User confirms new sessions can use Qwen. Fresh-session discovery on 2026-10-12 verified and invoked `lmstudio/qwen3.6-35b-a3b-splash`; rediscover on future session transfers. Never relabel a DeepSeek run as Qwen.
- GPT-6.1 Sol assigns architecture/contracts and reviews actual diffs/runtime results. Optimize for accepted work, not call count. Track recurring defects/rework and change decomposition rather than blindly retrying broad tasks. Delegate independent owned files concurrently; keep one build coordinator per shared Gradle tree.
Report invocation limitations honestly and continue feasible work with available workers.

Every substantial delegation identifies objective, file ownership, constraints, deliverables, acceptance tests, evidence, exclusions and handoff format. Parallel workers must not edit overlapping files. Parent reviews source/diffs and independently verifies significant claims.

## Architecture and invariants
Moderner Beta is the terrain authority: do not recreate generation algorithms. Upstream inspection lives in ignored `.upstream/`; do not modify or vendor it without an ADR/license review. Active runtime is Fabric 1.21.1/Java 21 (Gradle may run on installed Java 25), with pinned Moderner Beta 5.0.0-alpha.4+1.21.1-fabric. The preserved 26.3 tree is a reference, not the current deliverable. Flashback is explicitly deferred by the owner. Published targets and source inspection do not establish runtime/replay compatibility. Keep modern registries, chunk storage, entities and protocol intact. Separate historical gameplay profiles from terrain preset IDs. Server owns enforcement; client is presentation/prediction only.

Planned packages: `profile` immutable resolved profile/data; `world` persisted metadata and progression; `integration.modernerbeta` bounded adapter; `gameplay` server rules; `client` UI/visuals; `export` offline historical writers. Add only modules needed by a verified slice. No live generator-reference swapping. Upgrades require backups, drained generation and restart/transaction design with crash recovery before acceptance. Unsupported stored content is preserved, never silently deleted.

### Historical generation-content compatibility (latest human refinement)
Use the actual corresponding upstream Moderner Beta preset as the foundation and resolve its complete settings through supported integration; do not manually reconstruct equivalent terrain/noise/climate configurations or keep a parallel terrain database. Necessary, researched compatibility overrides are permitted to exclude demonstrably anachronistic content or restore missing historical features. Separate backend terrain, version-aware generated content, and gameplay; share the historical version registry. Prefer generation-time settings/datapacks; use narrowly scoped hooks only where configuration cannot express per-world restrictions. Never generate modern content and remove it afterward. Ordinary worlds must remain unaffected.

Immediate acceptance is a bounded Beta 1.7.3 generation profile: verify full preset application plus actual permitted blocks, ore distributions, decoration, cave biomes, structures, mob spawns, heights/bedrock and dimension rules across generated chunks and save/reload. Block whitelists or terrain-only persistence are insufficient. Existing v1 fixtures remain preservation evidence, not fidelity. Previously proposed manually reconstructed v2 noise settings are removed from the active plan. Any necessary height adaptation requires independent integration evidence; do not copy density/router/noise parameters under the guise of compatibility. Upgrades apply destination preset AND generation rules only to future chunks, with persisted revisions and restart-safe backup/journal/crash recovery; never retroactively regenerate existing chunks.

## Licensing (read before copying anything from another project)
This project is **LGPL-3.0-or-later**, and it incorporates source copied from another mod, **Nostalgic Tweaks**,
which is **LGPL-3.0-only**. The full position, the obligations and the open questions are in
[docs/LICENSING.md](docs/LICENSING.md); the notices are in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md); the
file-by-file record is in [docs/PROVENANCE.md](docs/PROVENANCE.md).

### Third-Party Code and Licensing Policy

This project is LGPL-licensed and may incorporate appropriately licensed third-party code.

Developers and AI agents may reuse, modify, and incorporate third-party implementations when permitted by their
respective licences.

LGPL-licensed code may be incorporated, modified, and redistributed subject to the applicable LGPL conditions.
LGPL is a weak-copyleft licence, not a permissive licence equivalent to MIT or BSD.

When incorporating third-party code:

- Verify the applicable licence and provenance.
- Preserve required copyright notices.
- Attribute the original project appropriately.
- Clearly identify modifications when required.
- Satisfy applicable source-availability and redistribution requirements.
- Respect the exact licence version and compatibility conditions.
- Do not claim ownership of third-party implementations.
- Do not assume that changing a file's licence header changes its underlying licensing rights.

Existing correctly attributed third-party code should be preserved.

Licensing compliance must not become an excuse to unnecessarily rewrite, remove, or degrade working
functionality.

Where licensing compatibility is genuinely uncertain, investigate and document the uncertainty rather than
ignoring it or automatically deleting the implementation.

**In practice, for this repository:** ported Nostalgic Tweaks files carry the header
`SPDX-License-Identifier: LGPL-3.0-or-later AND LGPL-3.0-only` and name their upstream file in their own
documentation comment. New files that copy or adapt third-party code must do the same and must be added to the
`NT_FILES` list in `scripts/apply-licence-headers.py` (or an equivalent list, with the same evidence). Run
`./gradlew test --tests '*LicenceComplianceTest*'` before committing. Do not remove a licence header, do not
claim `-or-later` over adapted Nostalgic Tweaks material, and do not describe copied source as a dependency.

### Preserve Historical Documentation

Do not rewrite Git history or alter historical records merely because they contain previous licensing
instructions. Correct active instructions and current documentation. Historical records may remain intact.
`PROGRESS.md` is a historical record: leave earlier entries as they are, including entries that record a
now-superseded licensing position.

## Build and tests
Build instructions must be updated when established. Pin dependencies and wrapper; Java 25 installed. Test levels are distinct: unit, integration, Minecraft runtime, end-to-end, historically validated. Track counts, exact commands, exit codes and retained evidence. A launch is not terrain persistence; a recording is not replay compatibility. Flashback target MUST be the user's https://github.com/j-w-i/Flashback fork, not upstream releases. Pin and verify its actual compatible revision. Verify actual fork recording, playback and seeking before claiming support.

## Research and coding
Historical entries require URLs/precise references, confidence, implementation status and test links. Unknowns remain unknown; wiki summaries alone are not definitive algorithm evidence. Do not copy proprietary Minecraft code or textures into deliverables; the provenance of every third-party asset added is recorded in [docs/PROVENANCE.md](docs/PROVENANCE.md), and the copied Nostalgic Tweaks chest textures there are flagged as unresolved rather than assumed clear. Use declarative data for availability/values; dedicated algorithms for behavior. Use tools to read before editing, targeted edits, no fragile source string-splicing scripts. No runtime registry removal or competing implementations. Fail safely on corrupt/unknown metadata.

## Git and completion
Keep generated worlds, credentials, caches and upstream checkouts out of git. Never touch personal `.minecraft` saves/settings/accounts. Test worlds belong in project `run/`. Do not accept legal terms on the user's behalf without explicit authorization. Inspect diffs and run tests before focused commits; no fabricated author identity if git identity is unavailable. Maintain PROGRESS.md at milestones and handoffs with exact pending tasks and evidence.

Definition of done: reviewed implementation, passing appropriate tests, research uncertainty documented, reproducible runtime evidence for player-facing claims, preservation/negative-path tests, docs synchronized. Terrain-only profiles are not complete gameplay profiles. No claim of production readiness until all stated acceptance criteria pass.
