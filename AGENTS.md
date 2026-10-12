# Minecraft Historical Experience — agent contract

## Purpose and authority
Emulate Pre-Classic through Release 1.7.10 inside Minecraft Java 26.3. Never substitute an older runtime. GPT-6.1 Sol owns architecture, integration, migrations and final acceptance. Delegate bounded research, implementation and verification to `deepseek-official/deepseek-flash` (DeepSeek-V41-Flash). Report route failures honestly.

Every substantial delegation identifies objective, file ownership, constraints, deliverables, acceptance tests, evidence, exclusions and handoff format. Parallel workers must not edit overlapping files. Parent reviews source/diffs and independently verifies significant claims.

## Architecture and invariants
Moderner Beta is the terrain authority: do not recreate generation algorithms. Upstream inspection lives in ignored `.upstream/`; do not modify or vendor it without an ADR/license review. Initial runtime is Fabric 26.3/Java 25 because both Moderner Beta and Flashback publish this target. Keep modern registries, chunk storage, entities and protocol intact. Separate historical gameplay profiles from terrain preset IDs. Server owns enforcement; client is presentation/prediction only.

Planned packages: `profile` immutable resolved profile/data; `world` persisted metadata and progression; `integration.modernerbeta` bounded adapter; `gameplay` server rules; `client` UI/visuals; `export` offline historical writers. Add only modules needed by a verified slice. No live generator-reference swapping. Upgrades require backups, drained generation and restart/transaction design with crash recovery before acceptance. Unsupported stored content is preserved, never silently deleted.

## Build and tests
Build instructions must be updated when established. Pin dependencies and wrapper; Java 25 installed. Test levels are distinct: unit, integration, Minecraft runtime, end-to-end, historically validated. Track counts, exact commands, exit codes and retained evidence. A launch is not terrain persistence; a recording is not replay compatibility. Flashback target MUST be the user's https://github.com/j-w-i/Flashback fork, not upstream releases. Pin and verify its actual compatible revision. Verify actual fork recording, playback and seeking before claiming support.

## Research and coding
Historical entries require URLs/precise references, confidence, implementation status and test links. Unknowns remain unknown; wiki summaries alone are not definitive algorithm evidence. Do not copy proprietary Minecraft code or textures into deliverables. Use declarative data for availability/values; dedicated algorithms for behavior. Use tools to read before editing, targeted edits, no fragile source string-splicing scripts. No runtime registry removal or competing implementations. Fail safely on corrupt/unknown metadata.

## Git and completion
Keep generated worlds, credentials, caches and upstream checkouts out of git. Never touch personal `.minecraft` saves/settings/accounts. Test worlds belong in project `run/`. Do not accept legal terms on the user's behalf without explicit authorization. Inspect diffs and run tests before focused commits; no fabricated author identity if git identity is unavailable. Maintain PROGRESS.md at milestones and handoffs with exact pending tasks and evidence.

Definition of done: reviewed implementation, passing appropriate tests, research uncertainty documented, reproducible runtime evidence for player-facing claims, preservation/negative-path tests, docs synchronized. Terrain-only profiles are not complete gameplay profiles. No claim of production readiness until all stated acceptance criteria pass.
