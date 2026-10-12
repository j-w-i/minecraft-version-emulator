# ADR 001 — external Moderner Beta dependency on Fabric

Status: accepted for the first vertical slice; revisit only with runtime evidence.

## Evidence
- Mojang manifest lists 26.3 as release: https://piston-meta.mojang.com/mc/game/version_manifest_v2.json
- Moderner Beta version Ucgo1y8G publishes `5.0.0-alpha.4+26.3-fabric`; required Fabric API; alpha stability warning: https://api.modrinth.com/v2/version/Ucgo1y8G
- Flashback 0.43.6 version cV1pa0YV publishes Fabric-only 26.3: https://api.modrinth.com/v2/version/cV1pa0YV
- Fabric loader metadata provides 0.19.5: https://meta.fabricmc.net/v2/versions/loader/26.3/0.19.5
- Inspected upstream commit `87c015b63e8618795dbeafe41166a3818eb81f8f`, branch multiversion: https://github.com/Nostalgica-Reverie/moderner-beta/tree/87c015b63e8618795dbeafe41166a3818eb81f8f
- Upstream `versions/26.3/gradle.properties` requires Java25; `fabric/build-unobf.gradle.kts` uses unobfuscated Fabric Loom.
- `ModernBetaSettingsPresets` maps Beta1.7.3 to `moderner_beta:beta`, separately Beta1.8.1 to `moderner_beta:beta_1_8_1`.
- `ModernBetaWorldPresets` builds existing generator and biome source using registry lookups. `ModernBetaChunkGenerator.CODEC` serializes biome_source/provider_settings; constructor binds biome source to generator; initProvider creates seeded provider; createState derives structure state; biome/terrain stages use asynchronous tasks. Reference mutation alone cannot safely update the pipeline.
- Root license is MIT, copyright BlueStaggo/B3spectacled. Root license does not automatically establish rights to every bundled asset/dependency.

## Decision
Option A: external pinned dependency with narrow project-owned adapter/datapack. Fabric is the initial loader choice from published ecosystem evidence. User clarification requires https://github.com/j-w-i/Flashback specifically; its actual revision/runtime/build must be verified separately, and upstream release compatibility is not an acceptance result. Select explicit referenced presets through codecs/registries; do not fork generation, copy upstream source, or change global default preset. Runtime integration tests are required before calling the API stable or world generation working.

## Alternatives and consequences
B source integration would duplicate a multiversion/mod-loader build and require tracking alpha changes/license inventory, with no demonstrated benefit. C hybrid is reserved for a proved API gap and narrowly reviewed licensed components. External dependency is simplest, maintains upstream performance behavior and compatible modern storage/networking; pinning protects against undocumented alpha ABI changes. Reproducible artifact checksum/dependency validation should accompany release packaging. Missing or incompatible dependency fails startup, never silently switches to modern terrain.

Generator upgrades use stopped-world restart-based transactions, not a claimed supported hot-switch API. Published version compatibility is verified, but performance, save/reload, replay, full preset historical accuracy and upgrade behavior remain unverified until tests run. Upstream stock Nether/End are modern: historical dimensions require separate work. Early era preset reuse must be backed by evidence, not profile-name similarity.
