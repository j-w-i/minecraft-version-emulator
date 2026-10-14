# NT list: researched boundaries (2026-10-14)

Boundaries established by wiki research (`?action=raw` pages) for the owner's 25-item NT list. Recorded
here so each implementation cites the same evidence, and so the UNKNOWNs cannot quietly become guesses.

## Established

| Item | Boundary | Evidence |
|---|---|---|
| Cloud height 108 | Classic 0.0.14a → Beta 1.7.3 | Cloud history: "Added clouds, which float westward between layers 108 and 112" |
| Cloud height 128 | Beta 1.8 → 1.16.5 | Cloud history: "Now clouds float between layers 128 and 132" |
| Cloud height 192 | 1.18+ (also 21w06a, reverted 21w13a) | "Clouds once again float between layers 192 and 196" |
| 2D dropped items | up to Release 1.3; 3D from 12w50a (1.4.6) | Item (entity): "items now render in the fancy graphics setting as 3D spinning animations (non-block items no longer display as sprites)" |
| Inventory items as 3D | already 3D before Alpha 1.0.1 | Inventory: "20100128-2200 Items now show as 3D voxel sprites" |
| Recipe book | added 1.12 (17w13a) — absent from every declared version | "Added the recipe book." |
| Chest model | old model through Beta 1.7.3; redone Beta 1.8 Pre-release | "The chest's model has been redone... it uses an entity model" |
| Ender chest | added 1.3.1 (12w21a); texture changed 1.14 | "Added ender chests." — note the premise that it was Beta 1.8 is wrong |
| Blue void | Beta 1.8 → 1.13 (18w01a) | "The Void's color above Y=0 was changed from the skybox's default color to a deeper blue"; 18w01a "no longer blue" |
| Void fog | Beta 1.8 pre-release → 14w34c | "Fog is now thicker and darker deep underground, and gives off particles"; "Removed void fog and its associated particles" |
| Creative cannot see void fog | from 1.1 (11w48a) | "Players in creative mode can no longer see void fog" |
| Sunrise/sunset fog colour | added Beta 1.8 pre-release | "Now changes color during sunrise and sunset when looking toward the sun" |
| World fog distance change | 13w36a doubled; 13w38a made it 25% of render distance (was 75%) | "Fog now covers 25% of the render distance, compared to 75% before 13w36a" |
| Nether fog colour #330808 | Alpha 1.2.0 | "The fog color in the Nether is #330808" |
| Stars dimmer | Beta 1.5 | "Stars are now slightly dimmer" |
| Stars smaller/brighter | Release 1.3.1 | "Stars are now slightly smaller and brighter" |

**Correction to the request itself:** the blue void, void fog, creative void fog, and the sunrise/sunset fog
colour are all **Beta 1.8 or later**, and void fog ends in 14w34c. The declared catalogue's oldest eight-plus
builds predate them, so those four items either do not apply to the historical world at all in the direction
assumed, or apply only to the Beta 1.8+ half of the catalogue. This is the same shape of error as the Classic
lighting engine.

## UNKNOWN — not to be guessed

- **Star style** as upstream defines "alpha"/"beta"/"modern": the wiki dates individual star changes but never
  describes those three styles, so no boundary can be attached to them.
- **Universal sky colour** per era: upstream's colours exist in code, but the wiki does not date them. The one
  related row (Alpha v1.2.0 "Sky color is now dependent on biome temperature/humidity values") is a different
  feature.
- **Dynamic fog colour** (biome temperature): no wiki evidence found for when a temperature-based fog colour
  existed or changed. Do not infer it from 1.7's biome-temperature changes.
- **Nether fog density**: colour known, density and change-over-time unknown. The modern 10-96 block range must
  not be projected backwards.
- **Cave fog** as distinct from void fog: no separate boundary established.
- **Left-click activation** of doors, levers and buttons: the wiki records **no input transition at all** for any
  of the three. What would settle it is the old block classes from a client jar (b1.7.3 is on disk at
  `~/Downloads/betacraft*/Betacraft/betacraft/versions/b1.7.3.jar`), not the wiki.
- **Old crafting screen** background/texture: no versioned statement found. The recipe book addition is dated;
  the GUI art is not.
- **Chest GUI** texture/background: model change dated, GUI art not.

## Licence note

Old chest and ender chest need model and texture assets, which upstream carries under
`assets/nostalgic_tweaks/models|textures`. Those are Mojang-derived art, not NT's own work, so copying them is a
licence decision that needs the owner's explicit go-ahead rather than a judgement call here.
