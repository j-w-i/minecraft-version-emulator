# Architecture — implementation contract

## Runtime and first slice
Minecraft Java 26.3, Java 25, Fabric. A separately installed, pinned Moderner Beta dependency supplies all historical terrain. Flashback MUST be the user's `j-w-i/Flashback` fork (https://github.com/j-w-i/Flashback), pinned to a verified compatible revision. It remains a client recording/playback dependency, not a replacement protocol. Upstream release testing does not satisfy fork compatibility. Initial playable target is Beta 1.7.3; it is explicitly incomplete until survival acquisition and mechanics are historical. No blanket completion claims for other profiles.

## Separation of concerns
- `profile`: stable profile IDs distinct from terrain keys; immutable validated rules, explicit implementation/evidence status. Do not infer gameplay equality from equal terrain.
- `integration.modernerbeta`: select/persist exact preset settings through existing registry/codec, never implement noise or mutate global defaults. First creation uses a dedicated historical world preset referencing a project-owned Moderner Beta settings preset that inherits upstream Beta generation with fidelity overrides. The explicit project preset reference is the historical opt-in marker; matching upstream provider IDs alone is never sufficient. Verify reference retention against the pinned compiled artifact. Resolved settings/dimension fingerprinting is required before claiming reproducibility across datapack changes.
- `world`: authoritative per-world metadata, revision and migrations. Ordinary worlds are untouched. Missing metadata on an explicitly selected new historical preset initializes a profile; unknown/corrupt existing metadata aborts historical activation rather than silently substituting defaults. Atomic persistence must not overwrite corrupt originals.
- `gameplay`: server-side gates at acquisition/interaction boundaries. Profile-specific recipes and loot replace modern semantics, not merely filter result IDs. Existing unsupported content is retained and interactions restricted with diagnostic/admin policy. Administrative override is explicit, permissioned and logged; no implicit bypass through command-created stacks.
- `client`: synchronized profile view, historical UI/resources and movement prediction over modern player networking. No client-only enforcement. Prefer deterministic rendering adapters and supplied state over unrecorded custom packets.
- `export`: separate non-destructive offline conversion with preflight loss report and strict mode. Native save remains modern. Reject unmapped data rather than silently dropping it.

## Profile and research data
A profile resolves terrain+biome settings, availability tables, recipes/smelting, loot/spawn, movement/combat/survival/inventory/AI/block/environment adapters, visuals/UI, export target and ordered migration IDs. Implement incrementally rather than generating dummy 'supported' profiles. Rules require stable feature IDs, evidence references and confidence; research records additionally track changes/removals and tests. Runtime must not load uncertain research automatically as authoritative behavior.

## Progression safety
First proof: Beta 1.7.3 → Beta 1.8.1, no downgrade. No live generator reference mutation. Proposed transaction: reject concurrent requests; preflight destinations and complete intermediate migrations; disconnect/save/drain and stop the world; verified backup with manifest; durable prepared journal; update serialized destination generation and profile together; restart with new generator/provider/structure state; validate revision; mark committed. Failure recovery uses original backup/journal rather than guessing. This is a design contract, not an implemented upgrade feature.

Existing FULL chunks retain terrain/biomes/builds; partially generated chunks require an explicit epoch policy before upgrades ship. Structure references, cross-border decoration and finite-to-infinite transitions require tests. Dimension height remains stable unless a reviewed preservation migration exists; historical playable bounds and generation height are distinct from modern storage bounds.

## Acceptance gates
1. Build/unit: wrapper build, metadata positive/negative tests and data validation.
2. Runtime slice: actual Moderner Beta generated FULL non-air chunks, deterministic seed/preset evidence, save/reload metadata and built block preservation.
3. Gameplay: negative acquisition-path tests and survival loop; modern recipes/drops/mechanics absent by server policy.
4. Upgrade: backup/crash recovery/concurrent tasks/chunk preservation/unexplored destination generation tests.
5. Flashback: actual recorded world playback, seeking and state comparisons, not launch-only evidence.
6. Export: independent format parser plus native historical load where supported.
