# Third-party notices

This file records every third-party work this project incorporates, links against, fetches at runtime or
distributes, with the licence that governs it and the obligations that follow. It is the companion to
[`LICENSE`](LICENSE) and [`docs/LICENSING.md`](docs/LICENSING.md). The full file-by-file record of what came
from where is in [`docs/PROVENANCE.md`](docs/PROVENANCE.md).

**A notice is not a licence.** Listing a component here does not relicense it, and does not by itself satisfy
the source-availability obligations of a copyleft licence. Those are addressed in
[`docs/LICENSING.md`](docs/LICENSING.md), section "Distribution obligations".

---

## Nostalgic Tweaks — LGPL-3.0-only — source code and assets copied and adapted

| | |
| --- | --- |
| Full project name | **Nostalgic Tweaks** |
| Copyright holder | Copyright (C) Adrenix and the Nostalgic Tweaks contributors |
| Authoritative upstream repository | https://github.com/Nostalgica-Reverie/Nostalgic-Tweaks |
| Repository the upstream metadata names | https://github.com/Adrenix/Nostalgic-Tweaks (GitHub redirects this to the URL above; it is the same repository, id 466280964) |
| Licence | **GNU Lesser General Public License, version 3 — LGPL-3.0-only** |
| Upstream licence file | `LICENSE.txt`, blob `0a041280bd00a9d068f503b8ee7ce35214bd24a1` |
| Declared by the distributor | Modrinth project `nostalgic-tweaks` reports `{"id": "LGPL-3.0-only"}` |
| Revision this project ports from | Branch `1.21`, commit `51d6d3dc1fa33223f8548e1625ed7595183c2b9e` (2026-08-14), mod version `2.0.0-beta.923-indev.2`, for **96** of the 103 Java files. Seven files state other revisions instead — see below. |
| Local reference checkout | `.upstream/nostalgic-tweaks` (git-ignored; never distributed) |

### What has been incorporated, and how

**This is not a dependency.** Nostalgic Tweaks source is **copied into this repository and modified here**.
Stacking a notice on top of a jar dependency is not what has happened, so LGPL-3.0's obligations to provide
Corresponding Source for the incorporated portions apply to this project's own repository and artefacts.

- **103 Java source files** under `src/main/java/net/historicalexperience/` contain code copied or adapted from
  Nostalgic Tweaks. Each of those files carries a copyright and licence header naming Nostalgic Tweaks, and each
  names the upstream file or tweak constant it came from in its own documentation comment. The
  `SPDX-License-Identifier` expression on those files is `LGPL-3.0-or-later AND LGPL-3.0-only`: the project's own
  contributions in the file are `-or-later`, the adapted Nostalgic Tweaks material remains `LGPL-3.0-only`. The
  authoritative list is the `NT_FILES` array in `scripts/apply-licence-headers.py`, which the header tool and the
  compliance test both use, so the count cannot drift from the files.
- **Four of those files come from branch `1.21.4`, not `1.21`.** `gameplay/HistoricalDamageValues.java`,
  `gameplay/HistoricalSwordBlocking.java` and `gameplay/HistoricalSwordBlockingEvents.java` state revision
  `010526deaf3aa5e562b45b60a286a667e8eeac12`, and
  `mixin/HumanoidArmorLayerHistoricalTintMixin.java` names
  `fabric/.../candy/armor_damage/HumanoidArmorLayerMixin.java`, which exists only on that branch. Both branch
  heads are therefore part of the provenance, and the licence is the same on both: the LGPL text is on the
  repository's default branch and governs the project as distributed. See [docs/PROVENANCE.md](docs/PROVENANCE.md).
- The ports range from verbatim copies (for example `client/hud/HudElement.java`, `client/render/MobArmHelper.java`)
  through adapted mixins (for example `mixin/LightEngineHistoricalOldLightingMixin.java`, ported from
  `mixin/tweak/candy/world_lighting/LightEngineMixin.java`) to constants and arithmetic taken from upstream
  helpers (`client/lighting/HistoricalOldLightMath.java`, from `helper/candy/light/LightingHelper.java` and
  `LightTextureHelper.java`), and to `client/lighting/HistoricalFogTweaks.java`, whose distance and fog-plane
  constants are upstream's `helper/candy/level/fog/OverworldFogRenderer.java` verbatim.
- **The incorporated implementations have been modified.** The typical modifications are: upstream's process-global
  configuration flags are replaced by this project's per-world historical version gate; upstream's NeoForge and
  Architectury plumbing is replaced by Fabric's; class, package and identifier names are renamed to this project's
  conventions; and the set of historical versions each behaviour applies to is decided by this project's research
  data rather than by upstream's era presets. Per-file documentation comments state the modification where it is
  material. Where a project file merely *names* Nostalgic Tweaks as research, a checklist or a citation of a
  measured constant, it is not listed as incorporated material; those files are named in
  [docs/PROVENANCE.md](docs/PROVENANCE.md) together with the reason.
- **53 asset files** are copied from upstream's `common/src/main/resources/assets/nostalgic_tweaks/` into this
  project's own namespace: 4 blockstates, 18 block/item model JSON files, 30 block textures for the old chest,
  ender chest, trapped chest and Christmas chest, and 1 GUI texture (`textures/gui/inventory.png`, used to paint over
  the modern inventory's off-hand slot box). The JSON files differ from upstream only by the
  `nostalgic_tweaks` → `historical_experience` namespace rewrite and, in three blockstates, by whitespace; the 31
  PNGs are byte-identical to upstream's. `assets/historical_experience/icon.png` and `lang/en_us.json` are this
  project's own. The comparison is reproducible with `scripts/compare-nt-assets.py`.
- **Two lines of upstream's access widener** are copied into `historical_experience.accesswidener`, under the same
  `# Inventory` heading: `mutable field net/minecraft/world/inventory/Slot x I` and `... y I`. They lift the
  finality of a slot's screen coordinates, which is what lets this project move a slot with upstream's own
  `slot.x = x; slot.y = y;` rather than through an accessor of its own.

See [`docs/PROVENANCE.md`](docs/PROVENANCE.md) for the file-by-file table.

### Where the Corresponding Source is

The Corresponding Source for the incorporated Nostalgic Tweaks material, including the modifications made here,
is this repository itself:

**https://github.com/j-w-i/minecraft-version-emulator**

The published source JAR (`*-sources.jar`) carries the licence texts and this notice under `META-INF/`, so a
binary recipient receives the Corresponding Source terms and location with the artefact.

**The repository is public** (confirmed 2026-10-14), so it satisfies LGPL-3.0 section 4/6 source availability
for a published binary. The one condition attached: the licensing files must actually be **pushed** — at the
time of writing they are committed locally but not on the remote. See the pre-publication checklist in
[`docs/LICENSING.md`](docs/LICENSING.md).

### The `only` versus `or-later` election

Upstream's `LICENSE.txt` is the unmodified GNU LGPL v3 text — verified byte-for-byte identical to the FSF's
published `lgpl-3.0.txt` (SHA-256 `E3A994D82E644B03A792A930F574002658412F62407F5FEE083F2555C5F23118`, 7,652
bytes), including section 6's "or any later version" mechanism. The document contains **no project-specific
election** of a later version, so the incorporated material is treated as **LGPL-3.0-only**. This project does
not relicense it.

### Why the source branches carry no licence file, and why that is settled

Upstream's *source branches* carry **no in-tree licence file**. The LGPL text exists on the repository's
docs-only default branch (`main`); the commit that added it (`6f2614ef`, 2026-06-01) touches only `.github/`,
`README.md` and `LICENSE.txt` on that branch. Before it, the only licence ever attached to a source branch was
the proprietary **Minecraft Mod Public License v2.0.3-Modified**, which was deleted from source branches between
2022-10-10 and 2025-01-15 and which states that its author "may change the Mod license, but cannot do so
retroactively".

The rights holder distributes the mod under LGPL-3.0-only (Modrinth metadata, above) and has added the LGPL text
to the repository, and **the project owner confirmed on 2026-10-14 that proceeding on that basis is fine.**
LGPL-3.0-only is therefore the governing licence for the adapted portions, and this project's obligations are
discharged against it. Should that confirmation ever be withdrawn, the Minecraft Mod Public License's section 7
terms (no revenue, credit to the Author, compliance with the Mod license) would apply instead — see
[`docs/LICENSING.md`](docs/LICENSING.md), "Open points".

### The copied chest textures are Nostalgic Tweaks' own

The 30 chest, ender-chest, trapped-chest and Christmas-chest PNGs under
`assets/historical_experience/textures/block/` are byte-identical to upstream's, and the owner confirmed on
2026-10-14 that they came **from Nostalgic Tweaks**. They are therefore covered by exactly the LGPL-3.0-only
analysis above: attribution here, the licence texts with the artefacts, and the Corresponding Source obligation.
An earlier revision of this notice asserted they were all original Mojang art; **that was unsupported and is
withdrawn** — the cube ender chest in particular is a Nostalgic Tweaks aesthetic, not historical vanilla
geometry.

---

## Legacy-Nether — CC BY-SA 4.0 — ported, currently switched off

- **Source:** https://codeberg.org/Nostalgica-Reverie/Legacy-Nether (by Nostalgica Reverie)
- **Licence:** Creative Commons Attribution-ShareAlike 4.0 International (CC BY-SA 4.0)
- **Pinned checkout:** `.upstream/legacy-nether` (git-ignored)
- **Ported by:** `scripts/port-legacy-nether.py` (re-runnable)
- **Status: NOT PRESENT in the current tree.** The ported biome data
  (`data/historical_experience/worldgen/biome/legacy_nether_wastes*.json`) was removed by commit `505a5fd`
  ("Switch the Legacy-Nether port OFF: it crashed the client at the title screen") and the directory is empty.
  **No CC BY-SA 4.0 material is distributed by this project at this revision**, so no share-alike obligation
  attaches to the current artefacts. This entry is retained because the port script and the research notes still
  reference it, and because re-enabling it would reinstate the obligation below. If it is ever re-enabled:

  - Every field is upstream's unchanged except that nether quartz is removed from the feature list of the plain
    variant, because nether quartz is Release 1.5 content and must not be generated before then.
  - The modification must be stated, and the ported biome data remains under CC BY-SA 4.0 and is not relicensed by
    this project's LGPL-3.0 licence.
  - Upstream's `data/minecraft/dimension/the_nether.json` and its `has_structure` tag entries would still be
    deliberately **not** ported, because this project composes the same dimension itself and because emptying a
    `minecraft:` tag would change ordinary worlds.

## OldVillages — MIT — village generation copied

- **Source:** https://github.com/MaxTechnik3796/old_villages (author: MaxTechnik)
- **Licence:** MIT, as declared by `mod_license=MIT` in upstream's `gradle.properties`. Upstream ships **no
  `LICENSE` file** and GitHub reports no detectable licence for the repository; the declaration in
  `gradle.properties` is therefore the whole licence statement available, and it is recorded as such rather than
  a licence file being claimed.
- **Revision ported:** `7578013ec061303982539fac7606df67577ffb26`, pinned in `.upstream/old_villages` (git-ignored)
- **Ported into:** `src/main/java/net/historicalexperience/worldgen/` (`HistoricalVillagePieces`,
  `HistoricalVillageBlocks`, `HistoricalVillageStates`, `HistoricalVillageTypes`, `HistoricalVillageRules`,
  `HistoricalVillageStructure`) and `server/VillageVerification.java`
- **Data files copied or adapted** from upstream's `data/old_villages/`: the two chest loot tables
  (`historical_village_blacksmith.json`, `historical_village_blacksmith_early.json` — content identical, only
  `set_count` → `minecraft:set_count` plus formatting, with the early variant derived from the same table), the
  structure and structure-set documents (`historical_village.json`, `historical_villages.json` — namespace and id
  rewrite only; spacing, separation and salt are upstream's), and the `has_structure` biome tag
  (`historical_village.json` — adapted, reduced from eight biomes to three). These are recorded in
  [docs/PROVENANCE.md](docs/PROVENANCE.md); no comment syntax exists in JSON to carry a per-file note.
- **Modification:** upstream is a NeoForge mod that registers its own structure type through a `DeferredRegister`
  and mixes into vanilla village placement. The port reimplements that registration against Fabric and this
  project's own `HistoricalStructureRules`, and gates the structure per historical version instead of by a global
  upstream config flag. Each ported file's header names the upstream file it came from.
- The MIT permission notice requires the copyright notice and permission notice to be included in copies. The
  upstream repository does not state a copyright line; the closest verifiable statement of authorship is
  `mod_authors=MaxTechnik` in `gradle.properties`, which is recorded here in place of an invented notice:

  > OldVillages, by MaxTechnik (https://github.com/MaxTechnik3796/old_villages). Licensed under the MIT Licence as
  > declared in the project's `gradle.properties`.
  >
  > Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
  > documentation files (the "Software"), to deal in the Software without restriction, including without limitation
  > the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
  > to permit persons to whom the Software is furnished to do so, subject to the following conditions:
  >
  > The above copyright notice and this permission notice shall be included in all copies or substantial portions of
  > the Software.
  >
  > THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
  > THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  > AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
  > CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
  > DEALINGS IN THE SOFTWARE.

## Moderner Beta — MIT — two data files copied; otherwise a linked dependency

- **Source:** https://github.com/BlueStaggo/moderner-beta
- **Copyright:** Copyright (c) 2024-2025 BlueStaggo, 2021 B3spectacled
- **Licence:** MIT
- **Pinned artefact:** `5.0.0-alpha.4+1.21.1-fabric` (dependency); research also pins the 26.3-line tag
  `5.0.0-alpha.4`, revision `95f3da6c3f23e7e128b319e5c721201ed4c17243`
- Moderner Beta is the terrain authority for this project and is used as an **external linked dependency**. No
  Moderner Beta source code, texture or audio is included.
- **One data file is carried from upstream, re-homed under this project's namespace** so the reduced level
  envelope applies per historical world instead of globally:
  - `resourcepacks/reduced_height/data/minecraft/dimension_type/overworld.json` →
    `src/main/resources/data/historical_experience/dimension_type/beta_1_7_3_overworld.json`
- An earlier revision of this notice also listed a copied `worldgen/noise_settings/overworld_128.json`. **That
  file is no longer present** (`src/main/resources/data/historical_experience/worldgen/noise_settings/` does not
  exist at this revision), and the notice is corrected rather than left claiming a file the project does not
  ship.
- The 1.21.1 backport reads four noise envelope fields (`min_y`, `height`, `size_horizontal`, `size_vertical`)
  through the supported settings component instead of carrying a copied noise-settings document. It carries no
  copied noise router, density-function database or terrain implementation.
- The MIT permission notice below covers the whole of Moderner Beta, including the data and envelope adaptations.

  > Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
  > documentation files (the "Software"), to deal in the Software without restriction, including without limitation
  > the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
  > to permit persons to whom the Software is furnished to do so, subject to the following conditions:
  >
  > The above copyright notice and this permission notice shall be included in all copies or substantial portions of
  > the Software.
  >
  > THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
  > THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  > AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
  > CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
  > DEALINGS IN THE SOFTWARE.

## GitHub CLI (`tools/`) — MIT — a bundled binary

`tools/bin/gh.exe` and `tools/gh.zip` are a copy of GitHub's `gh` command-line tool, committed to this
repository for offline use. They are **not** part of the mod or its JAR; they are developer tooling.

- Copyright (c) 2019 GitHub Inc. — MIT Licence (`tools/LICENSE`)

## Minecraft

Minecraft is an external, user-owned runtime and is not distributed by this project. No Minecraft JAR, class,
decompiled source, texture or audio is included. Research records identifiers, numeric behavioural facts, hashes
and references only. Minecraft's own EULA governs the game itself, not this project's code.

## j-w-i Flashback fork — all rights reserved — not redistributed

**https://github.com/j-w-i/Flashback**, revision `076b96b9fa400212d3d75fa2239d0f3836cf085b`, version `0.43.6.1`.
Its `LICENSE.md` states "Do not redistribute. All rights reserved." **No fork JAR, class, asset or
implementation is included in this repository or in any release artefact.** A git-ignored local checkout may be
built and tested in a project-local instance; that is not redistribution. See
[`docs/FLASHBACK_COMPATIBILITY.md`](docs/FLASHBACK_COMPATIBILITY.md).

## Resource packs used by the historical texture system — not bundled, not redistributed

The automatic historical texture system layers three third-party resource packs and Minecraft's built-in
Programmer Art. **None of them, and none of their files, is included in this repository or in the mod JAR**; a
test (`NoBundledPackAssetsTest`) fails if a pack archive or a Minecraft-namespace texture is ever added to the
mod's resources.

| Pack | Author | Licence (per Modrinth) | Where it comes from |
| --- | --- | --- | --- |
| Programmer Art Continuation Project (Base 2.5.4) | mzovjn / mzov | All Rights Reserved | https://modrinth.com/resourcepack/pacp |
| Golden Days Base 16.5 | PoeticRainbow | All Rights Reserved | https://modrinth.com/resourcepack/golden-days |
| Golden Days Alpha 16.5 | PoeticRainbow | All Rights Reserved | https://modrinth.com/resourcepack/golden-days-alpha |
| Programmer Art | Mojang Studios | Minecraft EULA | built into the game |

The packs are fetched **on the player's own machine**, from Modrinth's CDN, at exact pinned versions verified
against a fixed SHA-512, only after the setup screen has named the source and the player has chosen to download
(or has stored that choice in the config). They are kept in the mod's own folder
(`<game dir>/historical_experience/texture_packs/`), not in the player's resource-pack folder, and are never
modified: every read is read-only. Players who already have the packs are detected by content and nothing is
downloaded. The authors' own distribution terms govern the downloaded files; this project only reads them in the
player's game. Where the research notes compare pack art with original game art, the comparison was done in
memory and no image was saved.

## Linked build and runtime dependencies — no notice required

These are resolved by Gradle or supplied by the player's own instance. Their source is not copied into this
repository, and under LGPL-3.0 section 4(d)(1) they are used through a mechanism where the recipient already
has the library and can replace it, so their full licence texts are deliberately **not** reproduced here:

| Dependency | Role | Licence |
| --- | --- | --- |
| Fabric Loader | mod loader, runtime | Apache-2.0 |
| Fabric API | runtime dependency | Apache-2.0 |
| Moderner Beta | runtime dependency (see above for the one copied data file) | MIT |
| Mod Menu | optional, compile-only, `suggests` in `fabric.mod.json` | MIT |
| JUnit 5 | test only, not distributed | EPL-2.0 |
| Minecraft, Mojang mappings | provided by the player's installation | Minecraft EULA |

## What this project does *not* distribute

Recorded so the notices above are not read as broader than they are. None of the following is in this repository
or in any release artefact, and a test (`NoBundledPackAssetsTest`) fails if pack assets appear in the mod's
resources:

- Minecraft itself, or any Minecraft class, texture, sound or other asset.
- The Flashback fork, whose licence prohibits redistribution.
- The Golden Days, Golden Days Alpha or Programmer Art Continuation Project resource packs.
- Moderner Beta's source, textures or audio.
- Nostalgic Tweaks' own JAR, or its NeoForge and Architectury build files.
- Legacy-Nether biome data. It was ported once and then removed by commit `505a5fd`; the directory is empty.
