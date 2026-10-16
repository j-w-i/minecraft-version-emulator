# Indev map settings

Status: **implemented and verified natively.** The chooser, the write-path and the acceptance runs below are done;
the limits at the end of this page are the honest edges of what was exercised.

## What the original offered

The Indev level-generation screen (added Indev 0.31 20100106, removed with the level screen in Infdev 20100327) had
four groups. The project's Indev profile is **20100223, the last Indev build**, so every group below existed for it -
Paradise and Woods arrived 20100214, nine days earlier.

| Group | Values | Notes |
| --- | --- | --- |
| Level type | Island, Floating, Flat, Inland | "Original" became "Inland" on 20100109; Flat is flat grass with a starting house |
| Theme | Normal, Hell, Paradise, Woods | Normal/Hell from 20100107; Paradise/Woods from 20100214 |
| Shape | Square, Long, Deep | Long = twice the length, half the width; Deep = four times the height, half both |
| Size | Small, Normal, Huge | combined with the shape to give the map's dimensions |

Map dimensions (width × length × height), as published:

| Size | Square | Long | Deep |
| --- | --- | --- | --- |
| Small | 128×128×64 | 256×64×64 | 64×64×256 |
| Normal | 256×256×64 | 512×128×64 | 128×128×256 |
| Huge | 512×512×64 | 1024×256×64 | 256×256×256 |

Source, option lists and dates: [Indev map settings](https://minecraft.wiki/w/Indev_map_settings?action=raw), with the
feature list from [Java Edition Indev](https://minecraft.wiki/w/Java_Edition_Indev?action=raw). Both are recorded
with their confidence in [the research record](<../research/indev-map-settings.json>). The numbers are a wiki table,
not read out of an original jar, and are recorded as such.

## What this project can offer

Moderner Beta carries the Indev options in its own settings component,
`moderner_beta:finite_level_properties` (`type`, `theme`, `width`, `length`, `height`), so no terrain work is
involved: the project hands values to upstream's component and does not reconstruct any algorithm.

- **Level type: Island, Floating, Inland are available.** `Flat` is in the original list and **is not available**:
  upstream's Indev types are `island`, `floating`, `inland` and `classic`, and `classic` is Classic-era terrain, not
  Indev's flat map. Flat is shown so the original list is complete, with the reason, and cannot be chosen - nothing
  maps it onto a type that would generate something else.
- **Theme: all four.** Upstream's ids are exactly `normal`, `hell`, `paradise`, `woods`, and it ships one preset per
  theme.
- **Shape and size: all nine combinations**, from the table above.

`indev_latest` keeps the choice it already generates with - Island, Normal, Normal square - so a world created
without opening the chooser is unchanged. Existing Indev saves keep their stored settings; nothing is retrofitted and
no chunk is rewritten.

### One number the sources disagree about

The table gives a height of 64 for every Square and Long map. The pinned upstream Indev preset ships
**256×256×128**. The table's width and length agree with that preset, so the footprint is corroborated and the height
is not. The project records the disagreement instead of picking a side: the researched table is what the model
reports, a world created without touching the chooser keeps upstream's own preset, and there is no silently
"corrected" height.

## Implementation

| Piece | State |
| --- | --- |
| Option set, dimension table, defaults, settings-component builder | [IndevMapSettings](<../src/main/java/net/historicalexperience/worldgen/IndevMapSettings.java>), 5 tests in [IndevMapSettingsTest](<../src/test/java/net/historicalexperience/worldgen/IndevMapSettingsTest.java>) |
| Chooser in world creation | [IndevMapSettingsScreen](<../src/main/java/net/historicalexperience/client/version/IndevMapSettingsScreen.java>), opened from the version picker's "Indev settings..." button while an Indev version is highlighted |
| Applying the choice to the new world | [IndevMapApplication](<../src/main/java/net/historicalexperience/client/indev/IndevMapApplication.java>), through upstream's own modifier |
| Native create-world verification | **[receipt](<../run/client/indev-probe-1792177034051.json>) passes, 25 checks**: chosen options stored in the loaded world, chosen biome generated, border follows the chosen footprint; untouched Indev world gets no override and keeps upstream's biome and 256 border; ordinary world unaffected |

### What the native runs proved, and what they caught

The chosen combination (Floating / Hell / Deep / Small) lands in the created world as
`FiniteLevelProperties[type=FLOATING, theme=HELL, width=64, length=64, height=256]`, the generated biome at spawn is
`moderner_beta:indev_hell`, and the world border follows the chosen footprint (64 rather than upstream's 256). Three
real defects were found only by running it, and are the reason this page exists rather than a summary:

1. **A theme lives in two places upstream** - the chunk settings' `finite_level_properties.theme` (terrain, boundary
   liquid) and the biome settings' `single_biome` (which biome the map is). Setting only the first produced Hell
   terrain under a Normal biome.
2. **The settings object is a preset delta**, on the client during creation as much as on a loaded server: it holds a
   preset reference and none of the components that preset resolves to. Detecting an Indev world by the presence of
   `finite_level_properties` therefore answered "not Indev" and the choice was never applied. Detection now comes from
   the chosen version (`HistoricalVersions.isFiniteWorld`).
3. **`ModernBetaSettings.Builder.replace` leaves an absent component absent**, so the dimensions were silently
   discarded while the biome override - an `add` - took effect. Both are `add` now.

### Custom settings, and the finite-to-infinite migration

The bundled Indev→infinite migration (the apron release that removes the synthetic bedrock/dirt/water shell outside
a finite island, and the biome conversion that follows it) is verified **only against the default map**: its geometry
- the 256×256 footprint, the ±128 edges and the y=127 surface - is fixed in code. A world whose finite settings are
customized is therefore refused, deliberately, before any region byte changes:

> Conversion failed: java.io.IOException: Cannot establish safe default finite source identity: Customized finite
> provider settings cannot use the default apron migration; no region changes performed

Two consequences, both intended:

- **Choosing the default option names writes nothing at all.** The chooser re-applies the world type instead, so the
  world stays preset-only and keeps its Infdev upgrade. Only a genuinely non-default choice writes an override.
- **A customized Indev world cannot be migrated to an infinite one.** It still plays, saves and changes historical
  version normally; only the apron migration refuses. Un-customizing a world that already has an override is *not* a
  repair: its terrain was generated with the customized footprint, so reverting the settings would misalign the saved
  chunks with the generator. Generalizing the apron release to the world's own stated geometry is the only real fix,
  and has not been done.

### Limits

- Only the **Hell** theme and the **Floating** type were exercised natively. Paradise and Woods use the same one-line
  biome lookup; Inland uses the same type id path. The untouched world proves the Island/Normal default.
- **Flat** cannot be chosen: upstream has no flat Indev type (island, floating, inland, classic - and classic is
  Classic-era terrain). It is listed with that reason.
- The height disagreement (published table 64 vs the pinned preset's 128) is unchanged: a world created without
  opening the chooser keeps upstream's preset exactly, and the chooser writes the researched table.

The write-path uses upstream's own public entry points rather than assembling generator settings here:
`ModernBetaChunkGenerator.getChunkSettings()` and `ModernBetaBiomeSource.getBiomeSettings()`/`getCaveBiomeSettings()`
read what the chosen world type already resolved, `ModernBetaSettings.builder(...).replace(...)` swaps the single
`finite_level_properties` component, and `ModernBetaWorldScreenProvider.createModifier(...)` - the modifier Moderner
Beta's own settings editor uses - is applied through vanilla's `WorldCreationUiState.updateDimensions(...)`. No
generator is constructed, no settings JSON is parsed, and no file is written; a world created without opening the
chooser keeps upstream's own preset. The adapter lives outside the screen package because the GUI is guarded against
naming generators at all, and a test now forbids the adapter itself from constructing a generator or resolving a
preset.

Reproduce the current state:

```powershell
.\gradlew.bat --console=plain --offline test --tests '*IndevMapSettingsTest*'
```

Verification owed before this is called done: a native create-world run per level type and per theme, proving the
chosen values reach the generated world (stored settings plus the generated biome and world border), and that an
untouched Indev world and an ordinary world are both unchanged.
