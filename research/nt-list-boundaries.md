# NT list: researched boundaries (2026-10-14)

Boundaries established by wiki research (`?action=raw` pages) for the owner's 25-item NT list. Recorded
here so each implementation cites the same evidence, and so the UNKNOWNs cannot quietly become guesses.

## Established

| Item | Boundary | Evidence |
|---|---|---|
| Cloud height 108 | Classic 0.0.14a → Beta 1.7.3 | Cloud history: "Added clouds, which float westward between layers 108 and 112" |
| Cloud height 128 | Beta 1.8 → 1.17.1 | Cloud history: "Now clouds float between layers 128 and 132" |
| Cloud height 192 | 1.18+ (also 21w06a, reverted 21w13a) | "Clouds once again float between layers 192 and 196" |
| 2D dropped items | includes declared Release 1.4 (published 1.4.2); fancy 3D from 12w50a (1.4.6), first later declared profile 1.5 | Item (entity): "items now render in the fancy graphics setting as 3D spinning animations (non-block items no longer display as sprites)" |
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

**Correction to prior implementation notes:** blue void and void fog DO apply to declared Beta 1.8+ profiles.
Old-style sunrise/sunset fog removal applies before that boundary. These are not reasons to omit the requested
ports. Classic lighting is a separate engine intentionally removed at the owner's request.

Source references: [Cloud](<https://minecraft.wiki/w/Cloud?action=raw>),
[Item entity](<https://minecraft.wiki/w/Item_(entity)?action=raw>),
[Void](<https://minecraft.wiki/w/The_Void?action=raw>), [Fog](<https://minecraft.wiki/w/Fog?action=raw>),
[Sky](<https://minecraft.wiki/w/Sky?action=raw>). Wiki dates are boundary evidence, not complete algorithm evidence.
The NT algorithms themselves remain the porting authority; renderer ports listed below still need runtime validation.

## Implementation choices and remaining uncertainty

- **Stars — NT algorithm ported, historical mapping moderate confidence.** Read NT
  `world_sky/LevelRendererMixin`: ALPHA and BETA both replace geometry terms0.15/0.1 with0.25;
  ALPHA RGB is transparency/0.5, BETA RGB is transparency, both retain transparency as alpha.
  Those concrete differences match the dated transitions: ALPHA through Beta1.4 (brighter),
  BETA from Beta1.5 through Release1.2.5 (dimmer/larger), MODERN from Release1.3.1 (smaller/brighter).
  NT enum labels alone were insufficient; the earlier blanket 'cannot attach any boundary' conclusion
  ignored the available size/brightness implementation. Exact original-client pixel comparison remains pending.
  Per-world style changes explicitly rebuild the cached GPU star buffer; ordinary/title state is MODERN.
- **Old Universal Sky Colour — CORRECTED 2026-10-14: NT's palette is wrong for Beta.** NT's
  `SkyHelper.getOldColor` switch is INF_DEV `0xC6DEFF`, ALPHA `0x8BBDFF`, BETA `0x97A3FF`. The Infdev and
  Beta entries do not survive contact with the original clients, which are on this machine and were
  decompiled into `.upstream/legacy-decompile/`:
  - Alpha v1.0.11 (`a1011/cj.java:20`) holds its sky colour as a field, `private long A = 8961023L`
    (`0x8BBDFF`), applied by the sky getter at `cj.java:868`. NT's Alpha entry agrees with the client, and
    the owner confirmed that colour looked right. Moderner Beta's `ALPHA_SKY_COLOR` is the same value.
  - Beta 1.0 (`b10/dd.java:952`), Beta 1.7.3 (`b173/fd.java:927`) and Beta 1.8.1 (`b181/rv.java:997`) do not
    use a flat colour at all: each takes a temperature and calls `getSkyColorByTemp`, which is
    `Color.getHSBColor(0.62222224F - t*0.05F, 0.5F + t*0.1F, 1.0F)` with `t = clamp(temperature/3, -1, 1)`
    (`b10/gq.java:96`, `b173/kd.java:126`, `b181/vh.java:106`). Reachable range `0x9C99FF` (coldest) through
    `0x80A1FF` (neutral) to `0x66BDFF` (warmest). NT's `0x97A3FF` is outside that family entirely.
  - Beta 1.0-1.8.1 take that temperature from the world's climate noise (`b173/xv.java:30`: 4 octaves,
    scale 0.025, amplitude 0.5, seed*9871), which is exactly what Moderner Beta's `SkyColorSampler` /
    `ClimateSamplerSky` reproduce. Beta 1.9 Pre-release 6 (`b19pre6/uy.java:1015`) and Release 1.0
    (`10/ry.java:1015`) switch to the biome temperature at height 64; that is the HSV function modern
    Minecraft still uses in `OverworldBiomes.calculateSkyColor`, so from Beta 1.9 the game's own colour is
    already correct and nothing is substituted.
  - Infdev: `0xC6DEFF` is uncited. Moderner Beta's extracted `INFDEV_*_SKY_COLOR` is `0x99CCFF`
    (`10079487`); this project now uses Moderner Beta's value and records it as the weaker of the two
    provenances, since no Infdev jar exists locally to re-derive it.
  - Alpha v1.1.x/v1.2.x (`a126/cy.java:948`) already used the temperature form too, but Moderner Beta
    exposes no Alpha sky-temperature sampler, so those profiles keep the Alpha v1.0.x flat colour - the
    same value Moderner Beta assigns its Alpha biomes. Recorded as a known gap, not as fidelity.
  NT's hook point, however, is right and is kept: `Biome.getSkyColor` RETURN replaces the base colour, not
  the final time/weather-modulated `ClientLevel` vector, so the day brightness, rain, thunder and lightning
  processing the original clients also applied is preserved. NT's shader-fog colour branch at render
  distance <=4 is likewise kept.
- **Dynamic fog colour** (biome temperature): no wiki evidence found for when a temperature-based fog colour
  existed or changed. Do not infer it from 1.7's biome-temperature changes.
- **Nether fog density**: colour known, density and change-over-time unknown. The modern 10-96 block range must
  not be projected backwards.
- **Cave fog** as distinct from void fog: no separate boundary established.
- **Left-click activation**: owner explicitly supplied removal at Release 1.4.2. Applied to the declared
  Release 1.4 (first published update) boundary, not Release 1.5. Independent primary-code dating remains pending.
- **Old crafting screen** background/texture: no versioned statement found. The recipe book addition is dated;
  the GUI art is not.
- **Chest GUI** texture/background: model change dated, GUI art not.

## Licence note

Existing chest assets came from NT `assets/nostalgic_tweaks/models|textures`. Immediate source is known;
original texture authorship/licence is not established. The prior categorical Mojang-art assertion was unsupported.
Ender cube geometry is NT's aesthetic, not a historically vanilla model. Distribution provenance review remains pending.
