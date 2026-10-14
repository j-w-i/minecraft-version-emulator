# Historical textures: verified mappings, boundaries and provenance

This records what was **inspected** about the three external packs and the original clients, how each version boundary
was established, and the one design choice that makes the system independent of the player's resource-pack menu.
Nothing in this repository contains, copies or re-hosts any pack or texture: the packs are fetched on the player's
machine, and every comparison below was done in memory against files that were never written out.

> **Target version.** The brief names Minecraft 26.3. This repository's only target is 1.21.1 (see AGENTS.md: the 26.3
> tree was deleted at the owner's instruction), so everything here was built and verified on 1.21.1. The three packs
> declare `supported_formats` `[15, 420]` / `15..97`, which covers 1.21.1's resource format.

## The packs, as published

| Role | Pack | Pinned release | Modrinth project / version | Licence |
| --- | --- | --- | --- | --- |
| Baseline | Programmer Art | built in (`minecraft/resourcepacks/programmer_art.zip` in the 1.21.1 asset index) | n/a | Mojang |
| Layer 2 | Programmer Art Continuation Project, **Base** | 2.5.4 | `gx2HeuWD` / `7sZqSYLJ` | All Rights Reserved |
| Layer 3 | Golden Days **Base** | 16.5 | `BFzJ6aQL` / `rhfgMfhx` | All Rights Reserved |
| Layer 4 | Golden Days **Alpha** | 16.5 | `GalExSh7` / `hIV3CAmq` | All Rights Reserved |

Exact file names, sizes and SHA-512 hashes are in `PackPin`, read from `api.modrinth.com/v2/version/<id>` for those exact
versions and confirmed identical to the copies the owner already had installed.

**Golden Days Alpha is an overlay, not a standalone pack.** Its own `usage.txt`: *"This pack is meant to be put over
Programmer Art and Golden Days Base."* It supplies 84 block textures, 3 colourmaps, 3 title images and 41 models; of its 136
`assets/minecraft` files, 75 also exist in Base (it overrides Base). Its `pack.mcmeta` declares overlays that are
config-gated (`golden_days_alpha:classic_dyes` etc.), which this system does not enable.

**Programmer Art Continuation** identifies itself in `credits.txt`/`changelog.txt` ("Programmer Art Continuation
Project", heading "PACP Base Pack 2.5.4"); its `pack.mcmeta` description is only `2.5.4 Release / By mzovjn`, so metadata
alone cannot identify it. It ships Base, Beta-Fied and Alpha-Fied variants; only Base is layered, and the others are
reported as the wrong variant.

## What each pack contains that matters here

* `assets/minecraft/textures/block/cobblestone.png`: **Programmer Art** and **Golden Days Base** both supply it.
  Continuation and Golden Days Alpha do not.
* `assets/minecraft/textures/gui/sprites/hud/experience_bar_background.png` and `..._progress.png`: **only Golden Days
  Base** supplies them (and a `modern` variant under a config-gated overlay directory). Neither Programmer Art nor
  Continuation has them in this game version, and 1.21.1 has no `gui/icons.png`. So excluding Golden Days from these two
  paths leaves vanilla's own sprites as the answer.
* Both Golden Days packs carry many config-gated overlay directories (`option_*`, `mod_*`, `shaders_*`); the vanilla pack
  loader only applies overlays whose format range matches, and the config-gated ones have `formats: [0,0]`, so they are
  inert without the Polytone mod. The system opens each pack through the game's own `Pack`, so overlays behave exactly as
  they would for a selected pack.

## Boundaries, and how each was found

**Alpha 1.2.** Two independent lines of evidence, using the original Alpha jars on this machine:

1. A tile-by-tile diff of `terrain.png` between the original clients: Alpha v1.0.11 to v1.1.2_01 changes tiles 74 and 75
   only; v1.1.2_01 to v1.2.6 changes tiles 0, 14, 52, 53, 102-105, 112, 118-120 and 128 (grass, leaves and others); v1.2.6
   to Beta 1.0 changes none.
2. Golden Days Alpha's `grass_block_top.png` and `oak_leaves.png`, resampled to 16 px, equal the **pre-1.2.0** tiles with
   a mean RGB distance of 0.0 and are 132 / 226 away from the 1.2.x tiles.

The wiki agrees (Alpha v1.2.0: grass block, leaves, rails, iron/gold/diamond blocks "texture changed"). The pack's art
is strictly pre-1.2.0; the owner's rule extends it through the 1.2.x line, and that is what is implemented, as the single
named constant `GOLDEN_DAYS_ALPHA_END` (first chronology after Alpha 1.2.x). Nothing is declared between Alpha 1.2.0 and
Beta 1.0, so every declared Alpha build is "with Alpha" and Beta 1.0 is the first without.

**Release 1.2.5.** The owner's rule, as `GOLDEN_DAYS_BASE_END` = one chronology step past 1.2.5.

**Cobblestone, Beta 1.7.** The `terrain.png` cobblestone tile (index 16) is byte-identical in Alpha v1.0.11, Alpha v1.2.6 and
Beta 1.0 through 1.6.6, and changes in Beta 1.7.3 (wiki: *"1.7: The texture of cobblestone has been changed"*).
Golden Days Base's `cobblestone.png` equals the pre-1.7 tile and Programmer Art's equals the 1.7-onward tile, both with
distance 0 (and 119.1 to the other). So "Golden Days through Beta 1.6, Programmer Art afterwards" is the real transition:
Beta 1.6.x stays on the Golden Days side and Beta 1.7 starts the Programmer Art side. Expressed as `COBBLESTONE_CHANGE` =
chronology of Beta 1.7.0.

**Mossy cobblestone** is *not* part of this exception. It changed later (Beta 1.8, per the wiki) and the brief names only
cobblestone; it follows the ordinary layering.

**XP bar.** Never Golden Days, in every version, as an `AssetRule` with `pinToVanilla`.

## The mechanism, and why it is independent of the pack menu

`PackRepository.openAllSelected()` is the list the game hands the resource manager on every reload, lowest priority first.
One mixin (`PackRepositoryHistoricalTexturesMixin`) appends a single synthetic `PackResources`
(`HistoricalTexturePackResources`) to that list for the client's own repository. The repository's *selected* list, the
resource-pack screen and `options.txt` are never touched, so:

* the layer outranks every user pack, but only for the resources the plan defines;
* disabling it removes it on the next reload and leaves nothing behind;
* nothing is duplicated in the pack menu;
* a dedicated server never loads client classes, and the layer is added only for the client's repository, not data packs.

Priority inside the layer is one five-way ordering (Golden Days Alpha > Golden Days Base > Continuation > Programmer Art >
vanilla pin) because a single resource can need a per-asset answer that a list of whole packs cannot express. The
declared exceptions are `AssetRule`s (namespace, exact paths, version window, excluded sources, preferred source, vanilla
pin). Conflicts resolve deterministically (exclusions beat preferences; a `.mcmeta` follows its image).

## Resource reload lifecycle

`HistoricalTextureService.tick` compares the *desired* signature (enforcement switch, active version, installed packs)
with what the last reload actually built, and reloads only when they differ. One comparison covers joining a world,
leaving it, a multiplayer version push, a committed version upgrade, the Mod Menu toggle and a finished download. Adjacent
versions with the same effective stack share a signature and therefore never reload. A failed reload is remembered for
that desired stack and not retried in a loop. The active version comes from `HistoricalClientRules.activeVersion()`:
the integrated server's world profile in single player and the server's push on multiplayer; unknown means no layer.

## Provenance and licensing

All three packs are All Rights Reserved. They are downloaded on the player's machine from Modrinth at pinned,
hash-verified versions (the setup screen says so before anything is fetched and downloading needs an explicit click or the
stored opt-in) and are kept in `<game dir>/historical_experience/texture_packs/`, never in the resource-pack folder. This
repository contains none of their files; the comparisons above read them in memory only. Tests build tiny synthetic
fixtures at run time.
