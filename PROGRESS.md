# Persistent project status

## Milestone
Phase 1: first real Beta 1.7.3 world in Minecraft26.3. Started 2026-10-12 Australia/Sydney. Product not yet playable or complete.

## Verified environment / decisions
- Workspace originally empty; initialized git. Existing user git identity available.
- Java/javac25 installed, Node24, git2.50.1. No system Gradle found; project wrapper pending.
- Real Mojang26.3 release and compatible published Fabric Moderner Beta/Flashback artifacts checked over HTTP.
- DeepSeek-V41-Flash invocation route available: deepseek-official/deepseek-flash. Actual worker tasks dispatched, not simulated.
- Upstream shallow clone in ignored .upstream/moderner-beta, revision87c015b63e8618795dbeafe41166a3818eb81f8f. Clone job pwsh-619 completed exit0 and output collected.
- ADR001 selects external Moderner Beta pinned dependency, Fabric and Java25; source codec/registry/provider/async-stage inspection supports narrow integration. No terrain algorithm recreation. No live generator swapping.

## Capabilities and tests
No mod build/runtime tests completed yet. No generated world, gameplay, upgrade, export or replay claims accepted. Architecture and agent contract written. Research/database and implementation in progress.

## Active delegated ownership
- 744733bc-dc02-465a-899e-872fe06ba05b: terrain integration source investigation; owns docs/MODERNER_BETA_INTEGRATION.md + research/moderner-beta-evidence.json.
- 80f819fd-efca-4dad-8c35-e5a4223648c3: evidence-backed Beta1.7.3/1.8.1 feature data/schema + docs/RESEARCH_SOURCES.md.
- c8d79fe7-a123-4689-8076-f549e5b9dbeb: Gradle/wrapper, src, scripts, docs/TESTING.md; compiling Fabric26.3 vertical slice, dedicated Beta world preset, validated persisted metadata, unit/runtime verification.
Parent owns AGENTS.md, PROGRESS.md, architecture and ADR. Workers do not commit. Parent must review actual files/diffs and rerun critical tests.

## Unfinished / accuracy risks
Entire gameplay engine, historical UI/assets, progression, finite transitions, exports and Flashback E2E remain pending. Upstream is alpha, not production-proven. Upstream stock Nether/End remain modern. Preset availability is not proof of full historical correctness. Research uncertainties must remain explicit. Runtime server launch may require EULA authorization; do not accept on user's behalf. Personal .minecraft installation is read-only for discovery; never alter saves/accounts/settings.

## Next actions and handoff
1. Collect worker reports; review exact published artifact codec data vs current source; ensure no normal-world auto-tagging and no global-default preset changes.
2. Review bootstrap diffs/tests, independently run wrapper build/test and count tests; diagnose runtime blockers, not just document them.
3. Prove real chunk generation/non-air/seed and save/reload metadata + player-built block preservation in project-local world. Use GameTest/integrated harness if practical; distinguish unit vs runtime tests.
4. Once world slice works, delegate narrow server-authoritative acquisition/recipes/loot implementation using reviewed research. Keep separate feature implementation and confidence status.
5. Update this file with artifacts, commands, exact results, pending bugs and focused reviewed commit.
Persistent goal remains active; continue toward full product without claiming success from planning alone.

## User clarification
Flashback target MUST be https://github.com/j-w-i/Flashback, not the upstream published release. Fork existence verified through GitHub API; clone job pwsh-749 completed exit0 and output collected. Required fork checkout .upstream/flashback-j-w-i master revision076b96b9fa400212d3d75fa2239d0f3836cf085b has version0.43.6.1, Minecraft26.3, loader0.19.5, FabricAPI0.161.0+26.3 in read gradle.properties. Build/replay remain unverified. Worker a0290103-44ac-4bda-a66a-adc53ebd81f2 owns docs/FLASHBACK_COMPATIBILITY.md + research/flashback-fork-evidence.json and unmodified fork build verification. Upstream comparison clone .upstream/flashback job pwsh-727 completed exit0, output collected; not the acceptance target. Implementation/integration workers notified.
