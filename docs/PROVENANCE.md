# Provenance record

The file-by-file record of what this repository contains, where each part came from, and what licence governs
it. It exists so the claims in [`THIRD_PARTY_NOTICES.md`](../THIRD_PARTY_NOTICES.md) can be checked rather than
believed, and so the next person can extend the record without re-deriving it.

**Established by:** direct source inspection and git history, 2026-10-14. Upstream repositories are pinned as
git-ignored checkouts under `.upstream/`; the revisions used are recorded in every row that has one.

**Method.** For code, each project file's own documentation comment names the Nostalgic Tweaks (NT) file it came
from; those names were resolved against `git ls-tree` of the NT checkout to confirm they exist at the stated
revision. For assets, `scripts/compare-nt-assets.py` compares bytes against the pinned NT checkout and reports
identical, namespace-only, or differing files. For doubt, the rule applied was: a file is listed as NT-derived
only when there is a concrete statement of origin in the file or a byte-level match — a shared *idea*,
algorithm or behaviour is not treated as derivation, and merely naming NT as research is not either.

## Licence summary

| Origin | Licence | Files |
| --- | --- | --- |
| This project's own work | LGPL-3.0-or-later | Everything not listed below |
| Nostalgic Tweaks | LGPL-3.0-only | **103 Java files**, 53 asset files |
| OldVillages | MIT | 7 Java files (including extracted resident coordinates), 1 verification helper, 5 JSON data files |
| Moderner Beta | MIT | 1 JSON data file (linked as a dependency otherwise) |
| GitHub CLI | MIT | `tools/` only |

**How the Nostalgic Tweaks count is established, and why it changed.** The authoritative list is the
`NT_FILES` array in `scripts/apply-licence-headers.py`; the header migration tool and
`LicenceComplianceTest` both read it, so a file cannot be ported without being classified. The first version of
that list was built by grepping the tree for the words "Nostalgic Tweaks". That was wrong in both directions and
the error is recorded here rather than quietly corrected:

- It **missed eleven files** whose documentation names an upstream Nostalgic Tweaks class without naming the
  project. The clearest was `client/lighting/HistoricalFogTweaks.java`, which carried the plain project header
  while holding `OverworldFogRenderer`'s distance and fog-plane constants verbatim.
- It **wrongly included five files** whose own prose says Nostalgic Tweaks has no equivalent for what they
  implement. Those five now carry the plain project header and are named below.

The list is now built by reading each file. Any future port must be added to it explicitly.

## Nostalgic Tweaks

Upstream: https://github.com/Nostalgica-Reverie/Nostalgic-Tweaks · licence **LGPL-3.0-only**.

Branch and commit: **`1.21` = `51d6d3dc1fa33223f8548e1625ed7595183c2b9e`** for 92 of the 96 files;
**`1.21.4` = `010526deaf3aa5e562b45b60a286a667e8eeac12`** for `gameplay/HistoricalDamageValues.java`,
`gameplay/HistoricalSwordBlocking.java`, `gameplay/HistoricalSwordBlockingEvents.java`, and for
`mixin/HumanoidArmorLayerHistoricalTintMixin.java`, whose stated upstream path
`fabric/.../candy/armor_damage/HumanoidArmorLayerMixin.java` does not exist on `1.21` at all. Both heads are
recorded because the licence text is the same on both — it is on the repository's default branch and governs the
project as distributed.

Every file below carries the compound header expression `LGPL-3.0-or-later AND LGPL-3.0-only` and names the
upstream file or tweak it came from in its own documentation comment. "Modifications" describes what differs from
upstream; where it says "gate changed", upstream's process-global config flag was replaced by this project's
per-world historical version gate, which is the modification common to nearly all of them.

### Client — lighting, rendering, HUD

| File or component | Origin | Modifications | Applicable licence | Required action |
| --- | --- | --- | --- | --- |
| `client/hud/HudElement.java` | NT `helper/candy/hud/HudElement.java` | Verbatim, `STAMINA` retained | LGPL-3.0-only | Header present; attribution in notices |
| `client/hud/HudHelper.java` | NT `helper/candy/hud/HudHelper.java` | Adapted: pose handling, offsets and element order upstream's; stamina renderer not ported | LGPL-3.0-only | As above |
| `client/hud/HistoricalVersionOverlay.java` | NT `listener/client/GuiListener.java` (`renderTextOverlay`, the `OLD_VERSION_OVERLAY` branch) and `util/client/gui/CornerManager.java` (`drawText`, top-left path) | Adapted: the global flag is replaced by a per-version rule (`HistoricalVersionLabel`, original, derived from the client jars); only the top-left corner is ported | LGPL-3.0-only | As above |
| `client/hud/FlagHolder.java` | NT `util/common/data/FlagHolder.java` | Trimmed | LGPL-3.0-only | As above |
| `client/lighting/HistoricalFogTweaks.java` | NT `helper/candy/level/fog/OverworldFogRenderer.java` (ALPHA_R164) | **Verbatim constants**: `(renderDistance*16)*(renderDistance>28?1:2)`; terrain end 0.7/0.65/0.55/0.48; sky end 0.65/0.5/0.2/0.8. Classic mode not ported | LGPL-3.0-only | Header corrected; was plain project header |
| `client/lighting/HistoricalStars.java` | NT `world_sky/LevelRendererMixin` (`OLD_STARS` selection, ALPHA/BETA formulas) | Adapted; buffer lifecycle re-implemented here | LGPL-3.0-only | Header corrected |
| `client/lighting/HistoricalLightingTweaks.java` | NT `world_lighting/BlockStateBaseMixin` (water absorption), `CandyTweak.OLD_LEAVES_LIGHTING` | Adapted; version gates added | LGPL-3.0-only | Header corrected |
| `client/lighting/HistoricalChestTweaks.java` | NT `helper/candy/block/ChestHelper.java` (`isOld`) | Re-implemented; version gates replace upstream's cache. Boundaries themselves are stated by the wiki | LGPL-3.0-only | Header corrected |
| `client/lighting/HistoricalOldLightMath.java` | NT `helper/candy/light/LightingHelper.java`, `LightTextureHelper.java` | Constants upstream's, unchanged and not re-derived; version gating added | LGPL-3.0-only | As above |
| `client/lighting/HistoricalOldLighting.java` | NT `helper/candy/light/LightingHelper.java`, `NostalgicDataLayer` | Adapted; world version decides instead of config flags | LGPL-3.0-only | As above |
| `client/lighting/HistoricalDataLayer.java` | NT `helper/candy/light/NostalgicDataLayer` | Adapted | LGPL-3.0-only | As above |
| `client/lighting/HistoricalLightmap.java` | NT `LightTextureHelper.setGrayscaleTexture` | Adapted; grey variant only | LGPL-3.0-only | As above |
| `client/lighting/HistoricalSmoothLighting.java` | NT `mixin/tweak/candy/world_lighting/BlockBehaviourMixin.java` (corner-shading branch) | Adapted: corner shading only; the lightmap gap is recorded in-file | LGPL-3.0-only | As above |
| `client/lighting/HistoricalWorldTweaks.java` | NT era menu, recipe-book helper | Adapted | LGPL-3.0-only | As above |
| `client/HistoricalRecipeBookButton.java` | NT `InventoryScreenHelper.setRecipeButton` | Version-gated port of the disabled branch | LGPL-3.0-only | As above |
| `client/inventory/SlotTracker.java` | NT `helper/candy/screen/inventory/SlotTracker.java` | Trimmed to the `OFF_HAND` tracker and its `isEqualTo`/`reset`/`move`; upstream's crafting-grid and result trackers are not ported | LGPL-3.0-only | As above |
| `client/inventory/HistoricalInventoryScreen.java` | NT `helper/candy/screen/inventory/InventoryScreenHelper.java` (`setPositionsForSlots`, `renderOffHandSlot`) | Adapted: the `InventoryShield.INVISIBLE` branch only, with upstream's `-9999` coordinate and its 178,74 background patch; gate changed | LGPL-3.0-only | As above |
| `client/render/ArmorHelper.java` | NT `helper/candy/ArmorHelper.java` | Ported | LGPL-3.0-only | As above |
| `client/render/MobArmHelper.java` | NT `helper/animation/MobArmHelper.java` | Verbatim; the three values per arm are upstream's | LGPL-3.0-only | As above |
| `client/render/SwordBlockingRenderer.java` | NT `helper/gameplay/combat/SwordBlockingRenderer.java` | Verbatim transforms | LGPL-3.0-only | As above |

#### Client files that are *not* Nostalgic Tweaks-derived, and the evidence

Listed because their names or subjects invite the opposite conclusion. Each was read; each is the project's own
work, and each now carries the plain project header.

| File | Why it is not NT-derived |
| --- | --- |
| `client/lighting/HistoricalSkyColours.java` | The values are measured from the original clients. The file's own comment rejects NT's contribution: NT offers a flat per-era colour and its Beta entry `0x97A3FF` is wrong. The low-render-distance branch shape is NT's, and the file records that — but the expression here is the measured colour table, not NT's. |
| `mixin/ItemRendererHistoricalChestMixin.java` | Original code with no NT provenance in the file. It *consumes* the chest item models that are NT assets, which is disclosed with the assets rather than through a code header. |
| `mixin/PlayerHistoricalCriticalHitMixin.java` | The file says NT "has no critical hit FORMULA tweak, only DISABLE_CRITICAL_HIT, which is ported separately", and sources the formula from `research/historical-features.json` (`beta18-combat-critical-hits`). |
| `mixin/PlayerHistoricalExhaustionMixin.java` | The file records that NT has no equivalent; the exhaustion costs come from `research/historical-features.json`. |
| `mixin/FoodDataHistoricalRegenerationMixin.java` | The file records that NT has no equivalent; the regeneration rule comes from `research/historical-features.json`. |
| `mixin/DataComponentHolderHistoricalFoodMixin.java` | The file says "Nostalgic Tweaks has no food-value tweak, so this is from the research". |

### Gameplay rules and helpers

| File or component | Origin | Modifications | Applicable licence | Required action |
| --- | --- | --- | --- | --- |
| `gameplay/HistoricalGameplayTweaks.java` | NT tweak groups and `combat_player/PlayerMixin.java` | Adapted; version gate replaces config flags; some members are this project's own research | LGPL-3.0-only | Header present |
| `gameplay/HistoricalFoodHelper.java` | NT `helper/gameplay/FoodHelper.java` | Ported | LGPL-3.0-only | As above |
| `gameplay/HistoricalSwordBlocking.java` | NT `helper/gameplay/combat/SwordBlockingHelper.java` — **branch `1.21.4`, `010526de…`** | Faithful translation; shield case dropped | LGPL-3.0-only | As above |
| `gameplay/HistoricalSwordBlockingEvents.java` | NT `listener/common/InteractionListener.java` — **branch `1.21.4`, `010526de…`** | Faithful port of the right-click handler | LGPL-3.0-only | As above |
| `gameplay/HistoricalDamageValues.java` | NT `helper/gameplay/combat/DamageHelper.java`, `combat_item/DataComponentHolderMixin.java` — **branch `1.21.4`, `010526de…`** | Ported table | LGPL-3.0-only | As above |
| `gameplay/HistoricalMobLootHelper.java` | NT `GameplayTweak.OLD_*_DROPS` tables | Direct port of tables and entity mapping | LGPL-3.0-only | As above |
| `gameplay/HistoricalToolInteractionEvents.java` | NT `helper/gameplay/InteractionHelper.java`, `listener/common/InteractionListener.java` | Direct port; predicates upstream's verbatim | LGPL-3.0-only | As above |
| `gameplay/HistoricalLeftClickEvents.java` | NT `listener/common/InteractionListener.onLeftClickBlock` | Ported | LGPL-3.0-only | As above |
| `gameplay/HistoricalFarmingInteractionEvents.java` | NT `listener/common/InteractionListener.java` | Ported | LGPL-3.0-only | As above |
| `version/ActiveVersion.java` (formerly `gameplay/HistoricalActiveSession.java`; moved and renamed by the architecture refactor, header preserved) | NT config-flag mechanism | Adapted: version gate replaces the flag; now also publishes the resolved `FeatureSet` | LGPL-3.0-only | As above |

### Mixins

Ported from NT's `common/src/main/java/mod/adrenix/nostalgic/mixin/` tree. All are adapted: injection points
are frequently upstream's, the gate is this project's, and the target class names are Mojang-mapped. Unless a
row names a different path, the upstream file is the same class name with `Historical` removed and `Mixin`
retained, and the licence is LGPL-3.0-only. Where a row gives a tweak constant instead of a path, upstream
carries the behaviour on a mixin whose location differs from this project's, so the constant is the verifiable
link; the project file's own documentation comment names the upstream file.

| File or component | Origin (upstream file) | Modifications | Required action |
| --- | --- | --- | --- |
| `mixin/LightEngineHistoricalOldLightingMixin.java` | `tweak/candy/world_lighting/LightEngineMixin.java` | Rule moved to `HistoricalOldLighting`; Classic engine removed | Header present |
| `mixin/LayerLightSectionStorageHistoricalMixin.java` | `tweak/candy/world_lighting/LayerLightSectionStorageMixin.java` | Adapted for the light array Sodium copies | As above |
| `mixin/BlockAndTintGetterHistoricalMixin.java` | `tweak/candy/world_lighting/BlockAndTintGetterMixin.java` | Time-of-day comparison upstream's | As above |
| `mixin/LightTextureHistoricalMixin.java` | `LightTextureMixin` | Classic branch not ported | As above |
| `mixin/MinecraftHistoricalSmoothLightingMixin.java` | `tweak/candy/.../MinecraftMixin` (smooth-lighting branch) | Client-only registration | As above |
| `mixin/BlockBehaviourHistoricalShadeBrightnessMixin.java` | `CandyTweak.OLD_SMOOTH_LIGHTING` carrier | Client-only registration | As above |
| `mixin/BiomeHistoricalSkyColourMixin.java` | `world_sky/BiomeMixin` (`SkyHelper.getOldColor`, low-distance branch) | Branch shape upstream's; colour values from measurement | Header now correct |
| `mixin/ClientLevelHistoricalNightSkyMixin.java` | `world_sky/ClientLevelMixin.setMinNightSkyColor` | **Verbatim** `@ModifyArg(index = 1, clamp)` returning `0.005F` | Header corrected; was plain project header |
| `mixin/LevelRendererHistoricalStarsMixin.java` | `world_sky/LevelRendererMixin` (`setStarWidth` 0.15F→0.25F, `setStarHeight` 0.1F→0.25F, `onSetupStarColor`) | **Verbatim** slices and constants | Header corrected |
| `mixin/LevelRendererHistoricalStarsAccess.java` | `world_sky/LevelRendererMixin` `createStars` shadow | Accessor form | Header corrected |
| `mixin/DimensionSpecialEffectsHistoricalCloudHeightMixin.java` | `world_sky/DimensionSpecialEffectsMixin` | Version selection added | Header corrected |
| `mixin/FogRendererHistoricalWorldMixin.java` | `world_fog/FogRendererMixin`, `OverworldFogRenderer` | Version gate replaces global enum | Header corrected |
| `mixin/ChestBlockHistoricalRenderShapeMixin.java` | `chest_block/ChestBlockMixin` | **Verbatim** injection and `RenderShape.MODEL` replacement | Header corrected |
| `mixin/CraftingScreenHistoricalRecipeBookMixin.java` | `InventoryScreenHelper.setRecipeButton` init caller (`CraftingScreenMixin`) | Adapted for 1.21.1; Fabric loaders | Header corrected |
| `mixin/FurnaceScreenHistoricalRecipeBookMixin.java` | `InventoryScreenHelper.setRecipeButton` init caller (`AbstractFurnaceScreenMixin`) | Adapted for 1.21.1 | Header corrected |
| `mixin/InventoryScreenHistoricalRecipeBookMixin.java` | `InventoryScreenHelper.setRecipeButton` init caller (`InventoryScreenMixin`) | Adapted for 1.21.1 | Header corrected |
| `mixin/ParticleEngineHistoricalSweepMixin.java` | `candy/particle_engine/ParticleEngineMixin` sweep branch | Adapted | As above |
| `mixin/AbstractZombieModelHistoricalArmsMixin.java` | `animation/mob_arms/AbstractZombieModelMixin` | Adapted; same args and indices | As above |
| `mixin/AnimationUtilsHistoricalArmBobMixin.java` | `animation/mob_arms/AnimationUtilsMixin` | Injection point upstream's | As above |
| `mixin/HumanoidArmorLayerHistoricalTintMixin.java` | `fabric/.../candy/armor_damage/HumanoidArmorLayerMixin.java` — **exists only on branch `1.21.4`** | Adapted; entity captured in `renderArmorPiece` | As above; branch recorded |
| `mixin/InventoryScreenHistoricalOffhandMixin.java` | `candy/inventory_screen/InventoryScreenMixin` + `InventoryScreenHelper` | Upstream's two injections (constructor and `renderBg` both at `TAIL`) and its two helper calls; only its shield-position setting was replaced by the version gate. Upstream's crafting-grid/recipe-button/label/`hasClickedOutside` injections belong to its full old-inventory layout and are not ported | As above |
| `mixin/InventoryShieldSlotMixin.java` | `candy/inventory_screen/InventoryShieldSlotMixin` | Upstream's target (`InventoryMenu$1`), injection and both of its conditions; the config flag is the version gate | As above |
| `mixin/GuiHistoricalHudMixin.java` | `fabric/.../candy/old_hud/GuiMixin.java` | Injection points, slices, shifts and wraps upstream's; stamina hook not ported | As above |
| `mixin/GuiHistoricalAttackIndicatorMixin.java` | `gameplay/combat_player/GuiMixin.java` | Adapted; same slice and target | As above |
| `mixin/HumanoidModelHistoricalBlockingMixin.java` | `gameplay/combat_player/HumanoidModelMixin.java` | Adapted | As above |
| `mixin/ItemInHandRendererHistoricalBlockingMixin.java` | `gameplay/combat_player/ItemInHandRendererMixin.java` | Injection points and ordinals upstream's | As above |
| `mixin/MinecraftHistoricalSwordBlockingMixin.java` | `gameplay/combat_player/MinecraftMixin.java` | Both slices, ordinals and targets upstream's | As above |
| `mixin/MinecraftHistoricalInstantEatMixin.java` | `gameplay/food_health/MinecraftMixin.java` | Same slice, ordinal and target | As above |
| `mixin/LivingEntityHistoricalInstantEatMixin.java` | `gameplay/food_health/LivingEntityMixin.java` | 2 of upstream's 6 companion injections | As above |
| `mixin/LivingEntityHistoricalInstantAirMixin.java` | `gameplay/mechanics_swim/LivingEntityMixin.java` | Guards verbatim | As above |
| `mixin/LivingEntityHistoricalMovementMixin.java` | `gameplay/mechanics_player/LivingEntityMixin.java` | Argument modification form is upstream's | As above |
| `mixin/LivingEntityHistoricalSwordBlockingMixin.java` | `gameplay/combat_player/LivingEntityMixin.java` | Fall-damage half is this project's (upstream has none) | As above |
| `mixin/LivingEntityHistoricalHungerEffectMixin.java` | `gameplay/food_health/LivingEntityMixin.java` | Adapted | As above |
| `mixin/LivingEntityHistoricalLootMixin.java` | `gameplay/mob_drops/LivingEntityMixin.java` | Adapted | As above |
| `mixin/LivingEntityAttackTickerAccess.java` | `gameplay/combat_player/PlayerMixin.java` (`nt$setAttackStrengthTicker`) | Ported as an accessor | As above |
| `mixin/FoodDataHistoricalHungerMixin.java` | `gameplay/food_health/FoodDataMixin.java` | Both injection points upstream's | As above |
| `mixin/PlayerHistoricalFoodMixin.java` | `gameplay/food_health/PlayerMixin.java` | Third injection (burp) omitted | Header present |
| `mixin/PlayerHistoricalCombatMixin.java` | `gameplay/combat_player/PlayerMixin.java` | Targets byte-for-byte upstream's | As above |
| `mixin/DataComponentHolderHistoricalDamageMixin.java` | `gameplay/combat_item/DataComponentHolderMixin.java` | Ported | As above |
| `mixin/BlockBehaviourHistoricalDropMixin.java` | `gameplay/mechanics_block/BlockBehaviourMixin.java` | `@ModifyReturnValue` at the second RETURN | As above |
| `mixin/ServerLevelHistoricalOrbMixin.java` | `gameplay/experience_orb/ServerLevelMixin.java` | Immediate-pickup half not ported | As above |
| `mixin/FleeSunGoalHistoricalMixin.java` | `gameplay/mob_ai/FleeSunGoalMixin.java` | `@ModifyReturnValue` on `canUse` | As above |
| `mixin/PanicGoalHistoricalMixin.java` | `DISABLE_ANIMAL_PANIC` carrier | Direct port | As above |
| `mixin/TemptGoalHistoricalMixin.java` | `DISABLE_ANIMAL_TEMPTING` carrier | Direct port | As above |
| `mixin/MobHistoricalItemPickupMixin.java` | `DISABLE_MONSTER_ITEM_PICKUP` carrier | Direct port | As above |
| `mixin/MobHistoricalEquipmentMixin.java` | `DISABLE_MONSTER_ARMOR_SPAWN` carrier | Direct port | As above |
| `mixin/MobHistoricalCustomLootMixin.java` | `OLD_SKELETON_DROPS` carrier | Skeleton half only | As above |
| `mixin/AnimalHistoricalBreedingMixin.java` | `gameplay/animal_spawn/AnimalMixin.java` | `isFood` expression ported; redirect re-targeted to `mobInteract` | As above |
| `mixin/AgeableMobHistoricalSpawnMixin.java` | `gameplay/animal_spawn/AgeableMobMixin.java` | Gate changed | As above |
| `mixin/ZombieHistoricalSpawnMixin.java` | `gameplay/monster_spawn/ZombieMixin.java` | Baby half only; `nextFloat()` modified | As above |
| `mixin/RangedBowAttackGoalHistoricalMixin.java` | `fabric/.../gameplay/mob_ai/RangedBowAttackGoalMixin.java` | Gate changed from global flag to world version | As above |
| `mixin/ProjectileWeaponItemHistoricalInvincibleBowMixin.java` | `gameplay/combat_bow/ProjectileWeaponItemMixin.java` | Adapted | As above |
| `mixin/FishingHookHistoricalMixin.java` | `tweak/gameplay/mechanics_fishing/FishingHookMixin.java` | Loot half ported | As above |
| `mixin/CropBlockHistoricalBonemealMixin.java` | `tweak/gameplay/mechanics_farming/CropBlockMixin.java` | Ported | As above |
| `mixin/SaplingBlockHistoricalBonemealMixin.java` | `gameplay/mechanics_farming/SaplingBlockMixin.java` | Ported; second injection added | As above |
| `mixin/StemBlockHistoricalBonemealMixin.java` | `gameplay/mechanics_farming/StemBlockMixin.java` | Ported | As above |
| `mixin/CocoaBlockHistoricalBonemealMixin.java` | `gameplay/mechanics_farming/CocoaBlockMixin.java` | Ported | As above |
| `mixin/EnderChestBlockHistoricalRenderShapeMixin.java` | `chest_block/EnderChestBlockMixin` (`nt_chest_block$modifyRenderShape`) | Ported; NT's cube aesthetic, not historical vanilla geometry | As above |
| `mixin/BlockEntityRenderDispatcherHistoricalChestMixin.java` | `chest_block/BlockEntityRenderDispatcherMixin.java` | Same pair, same order | As above |
| `mixin/BlockStateModelLoaderHistoricalChestMixin.java` | `chest_block/BlockStateModelLoaderMixin.java` | Unconditional redirect | As above |
| `mixin/RecipeBookComponentHistoricalMixin.java` | `InventoryScreenHelper.setRecipeButton` | Ported disabled branch | As above |
| `mixin/AxeAccess.java` | `mixin/access/AxeItemAccess.java` | Ported | As above |
| `mixin/ShovelAccess.java` | `mixin/access/ShovelItemAccess.java` | Ported | As above |
| `mixin/SheepAccess.java` | `mixin/access/SheepAccess.java` | Ported | As above |
| `mixin/GuiAccess.java` | `mixin/access/GuiAccess.java` | Ported | As above |
| `mixin/ImageButtonHistoricalSpritesAccess.java` | NT recipe-button helper's sprite use | Adapted | As above |
| `mixin/ClientLevelHistoricalAttackSoundMixin.java` | NT attack-sound mixin | Adapted | As above |

**The authoritative list is `NT_FILES` in `scripts/apply-licence-headers.py` — 102 entries**, confirmed against
the files by the tool's `--check` mode and by `LicenceComplianceTest`. Any file above that is not itemised
individually is still in that list, with the same licence and the same modification pattern. Keeping the count in
one machine-readable place is deliberate: the two earlier hand-maintained versions of this table drifted from the
tree in opposite directions, and the `--check` mode is what catches that.

**Files that mention NT but are not derived from it** — each was read, because their names or subjects invite the
opposite conclusion: `mixin/FoodDataHistoricalRegenerationMixin.java`,
`mixin/DataComponentHolderHistoricalToolDurabilityMixin.java`, `mixin/EnchantmentMenuHistoricalEnchantingMixin.java`,
`mixin/PlayerHistoricalJumpExperienceMixin.java`, `mixin/EntityHistoricalSneakingMixin.java`,
`mixin/PlayerHistoricalOffhandMixin.java`, `mixin/ServerLevelHistoricalWeatherMixin.java`,
`mixin/RecipeManagerHistoricalCraftingMixin.java`, `mixin/BlockHistoricalEndPortalFrameMixin.java`,
`client/lighting/HistoricalSkyColours.java`, `mixin/ItemRendererHistoricalChestMixin.java`, and
`test/.../OreSelfDropTest.java`. These cite NT as a checklist, compare against it, or record that "NT has no
equivalent", and state their own research source. They carry the plain project header.

**Three files carry the NT header although their own prose says NT "has no equivalent" for the behaviour.** They
are classified as NT-derived because each ports or precedes an NT feature into this project's hook, and
over-attribution is the safer error:

| File | Why it carries the NT header anyway |
| --- | --- |
| `mixin/DataComponentHolderHistoricalFoodMixin.java` | Carries NT's food-value table; the hook locations are from this project's research, the values are upstream's |
| `mixin/PlayerHistoricalCriticalHitMixin.java` | The file states NT's `DISABLE_CRITICAL_HIT` "is ported separately" and is the same subject matter |
| `mixin/PlayerHistoricalExhaustionMixin.java` | Exhaustion costs are NT subject matter; upstream's constants are the reference the file compares against |

### Assets

Compared byte-for-byte against NT by `scripts/compare-nt-assets.py`.

| File or component | Origin | Modifications | Applicable licence | Required action |
| --- | --- | --- | --- | --- |
| `assets/historical_experience/blockstates/*.json` (4) | NT same-named files | Namespace rewrite; 3 also reindented | LGPL-3.0-only | Attribution in notices |
| `assets/historical_experience/models/block/*.json` (11) | NT same-named files | Namespace rewrite; `chest.json` parent resolves unchanged | LGPL-3.0-only | As above |
| `assets/historical_experience/models/item/*.json` (7) | NT same-named files | Namespace rewrite | LGPL-3.0-only | As above |
| `assets/historical_experience/textures/block/*.png` (30) | NT same-named files | **Byte-identical** | LGPL-3.0-only — confirmed NT-authored by the owner on 2026-10-14 | None: covered by the same attribution, licence texts and source-availability obligation as the copied source |
| `assets/historical_experience/textures/gui/inventory.png` | NT `assets/nostalgic_tweaks/textures/gui/inventory.png` | **Byte-identical**; used for the 18x18 background patch that covers the off-hand slot's box, and for nothing else | LGPL-3.0-only | As above |
| `assets/historical_experience/icon.png` | This project | — | LGPL-3.0-or-later | None |
| `assets/historical_experience/lang/en_us.json` | This project | — | LGPL-3.0-or-later | None |
| `assets/historical_experience/colormap/*.bin.gz` (3) | **Mojang's original clients** — the values of `misc/grasscolor.png` and `misc/foliagecolor.png` from `minecraft-1.0-client.jar` and `minecraft-b1.0-client.jar` | Read out of each jar as the client's own 65536-entry `getRGB` array and re-emitted as gzipped RGB triples; **no image is copied and no original file is redistributed** | Not this project's to license — extracted game data, recorded here rather than assumed clear | Nothing is required of this project to *use* them, but they are Mojang's values and are recorded as such. Source jars, hashes and the regeneration command are in `assets/historical_experience/colormap/README.txt` and `tools/GenerateHistoricalColormaps.java`. The owner directed this extraction ("extract them yourself from the jar files") |

## Other incorporated third-party material

| File or component | Origin | Modifications | Applicable licence | Required action |
| --- | --- | --- | --- | --- |
| `worldgen/HistoricalVillagePieces.java` | OldVillages `worldgen/OldVillagePieces.java` @ `7578013e` | Adapted to Fabric/version gate; original residential coordinates and current-chunk clipping restored, with this project's saved per-resident claims | MIT | Header names upstream; MIT text in notices |
| `worldgen/HistoricalVillageResidents.java` | Same pinned file, resident call-site coordinates at449/450,555,611/612,658,731 | Declarative extraction of five upstream populated piece types; own missing-resident repair additionally gives one NPC to small houses/shacks at inspected clear interior positions. No well/road/farm positions | MIT source attribution retained; own table wrapper LGPL-3.0-or-later | File names source/revision; existing MIT notice retained |
| `worldgen/HistoricalVillageBlocks.java` | OldVillages `worldgen/OldVillagePieces.java` | As above | MIT | As above |
| `worldgen/HistoricalVillageStates.java` | OldVillages `worldgen/OldVillagePieces.java` | As above | MIT | As above |
| `worldgen/HistoricalVillageTypes.java` | OldVillages `OldVillagesMod.java` | NeoForge `DeferredRegister` replaced with Fabric registration | MIT | As above |
| `worldgen/HistoricalVillageRules.java` | OldVillages `OldVillagesCommonConfig.java`, `worldgen/OldVillageStructure.java`, `mixin/DisableVanillaVillagesMixin.java` | Adapted; per-version gate replaces upstream config | MIT | As above |
| `worldgen/HistoricalVillageStructure.java` | OldVillages `worldgen/OldVillageStructure.java` | Adapted; biome matched rather than substring-searched | MIT | As above |
| `server/VillageVerification.java` | OldVillages (verification harness) | Rewritten around this project's diagnostics; the file itself states it is not part of the port | MIT | As above |
| `test/.../HistoricalVillageGeneration1211Test.java` | OldVillages (test) | Adapted | MIT | As above |
| `data/.../loot_table/chests/historical_village_blacksmith.json` | OldVillages `data/old_villages/loot_table/chests/village_blacksmith.json` | Namespace rewrite; `set_count` → `minecraft:set_count`; formatting. Content identical | MIT | Recorded in notices; JSON has no comment syntax |
| `data/.../loot_table/chests/historical_village_blacksmith_early.json` | OldVillages (same table) | Early-era variant derived from the same table | MIT | As above |
| `historical_experience.accesswidener` (2 lines) | NT `common/src/main/resources/nostalgic_tweaks.accesswidener`, under its own `# Inventory` heading | `mutable field net/minecraft/world/inventory/Slot x I` and `... y I`, copied unchanged; the file's other entries are this project's own | LGPL-3.0-only for those two lines | Header comment in the file names upstream; attribution in notices. A slot's coordinates are final in 1.21.1, and lifting that here is what lets `SlotTracker.move` write `slot.x = x` exactly as upstream does, instead of through an accessor of this project's own |
| `data/.../worldgen/structure/historical_village.json` | OldVillages `data/old_villages/worldgen/structure/old_village.json` | Namespace and id rewrite only | MIT | As above |
| `data/.../worldgen/structure_set/historical_villages.json` | OldVillages `…/structure_set/old_village.json` | Identical apart from namespace/id (spacing 60, separation 20, salt 1648146493 are upstream's) | MIT | As above |
| `data/.../tags/worldgen/biome/has_structure/historical_village.json` | OldVillages `…/tags/.../has_structure/old_village.json` | Adapted: eight biomes reduced to three | MIT | As above |
| `data/historical_experience/dimension_type/beta_1_7_3_overworld.json` | Moderner Beta `resourcepacks/reduced_height/.../dimension_type/overworld.json` | Re-homed under this project's namespace; envelope adapted | MIT | MIT text in notices |
| `test/resources/crafting/*.txt` (2) | Facts read out of Minecraft 1.21.1 (recipe-id census) | Not copied code or art — identifiers and counts | Minecraft EULA governs the game, not these facts | None |
| `tools/bin/gh.exe`, `tools/gh.zip`, `tools/LICENSE` | GitHub CLI | None | MIT | Not part of the mod; recorded in notices |

**Previously listed here and no longer present.** An earlier revision of this record listed two Legacy-Nether
biome files and a Moderner Beta `worldgen/noise_settings/beta_1_7_3_overworld_128.json`. Neither exists in the
tree at this revision: the biome directory is empty (the port was switched off by commit `505a5fd` after it
crashed the client at the title screen), and `worldgen/noise_settings/` does not exist at all. **No CC BY-SA 4.0
material is distributed by this project at this revision.** The entries are not carried forward as if they were
present, and the notices were corrected to match.

## Not incorporated, deliberately

| Component | Why it is not here |
| --- | --- |
| Flashback (j-w-i fork) | Its licence prohibits redistribution. Referenced as a local, git-ignored build target only |
| Golden Days / PACP / Programmer Art packs | Fetched on the player's own machine at runtime; never bundled. Enforced by `NoBundledPackAssetsTest` |
| Moderner Beta source | Used as a linked dependency, not vendored |
| Minecraft classes, assets or sounds | External, user-owned runtime |
| Nostalgic Tweaks' own JAR, and its NeoForge/Architectury build files | Not needed; the port is source-level |
| Legacy-Nether biome data | Ported earlier, then removed by `505a5fd`; see above |

## Assets referenced but not shipped

`assets/historical_experience/models/block/grass_block.json` is a namespace-rewritten copy of NT's
`models/block/grass_block.json` and references `historical_experience:block/grass_block_side_overlay`.
That texture is **not shipped** — `textures/block/grass_block_side_overlay.png` exists in NT but was not among
the 30 copied PNGs. The model is therefore unusable as-is. This is a cosmetic-model gap, not a licensing one, and
it is recorded rather than fixed here because the task's scope excludes changes to rendering behaviour. If the
texture is later copied from NT to make the model work, it falls under the same LGPL-3.0-only analysis as the
other 30 and must be added to `scripts/compare-nt-assets.py`'s comparison.
