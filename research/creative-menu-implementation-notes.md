# Creative menu implementation notes — 2026-10-17

Status: implemented in goal round2; [current catalog/safety scope](<creative-menu-catalog.json>) and [native11w49a receipt](<../run/client/snapshot-probe-1792160232039.json>) now cover real B, old-menu mouse grants, raw-packet preservation and actual save/reopen.[Four-world native controls](<../run/client/snapshot-anvil-probe-1792162354757.json>) now pass12w49a actual result/XP/book/component handling, unpatched negatives, replacement tab/search filtering, ordinary restoration and chest/clone-drag conservation. [Latest11w49a receipt](<../run/client/snapshot-probe-1792162092679.json>) additionally passes the owner's cursor-discard and B-toggle clarifications. The design notes below explain the bounded decisions, not uninstalled future work. Do not reopen the completed finite repair.

## Verified layout boundary

[Creative inventory history](https://minecraft.wiki/w/Creative_inventory/history?action=raw) explicitly describes the pre-12w21b layout as **8 columns ×9 rows (72 catalog slots)**, replaced by9×5 (45) in the1.3.1 cycle. This corrects an unverified9-column/8-row assumption. It also describes blocks first, then numeric item IDs, with multi-entry IDs at the end. Exact original coordinates and every ordering detail remain unverified; no original snapshot jar has been executed. Use own geometry/panel drawing and runtime item rendering, not copied Minecraft textures/implementation. The linked pack remains403/license unverified.

The same source distinguishes **item/block existence from creative-menu availability**: e.g. tall grass/fern and birch/spruce leaves added to Creative in Beta1.9pre6; snow layers in12w21a; potion variants in12w22a, healing/harming in12w38a; petrified oak slabs removed in12w17a; smooth blocks/all-bark logs/farmland/mushroom blocks only added to the catalog in1.13; spawners/dragon eggs only added in1.19.3. Decide/document whether a catalog is an availability limit or exact original catalog; do not silently claim the latter from a block-existence table.

## Positive catalog data

[Crafting allow-list](<../src/main/java/net/historicalexperience/gameplay/HistoricalCraftingEra.java>) exposes allowListed()/firstVersion(), but is **not sufficient as-is**: it has deliberately coarse/bounded early dates, includes all colored beds, and uses modern aliases such as stone_slab. Do not change its unrelated recipe behavior to implement menus. [Block dates](<../src/main/java/net/historicalexperience/generation/HistoricalBlockAvailability.java>) are useful for positive sourced boundaries; UNKNOWN is not permission. Palette-only dates are not reliable introduction dates for pre-Beta profiles without palettes. Chest availability is deliberately fail-open and must not grant arbitrary modern items.

Review dedicated catalog dates/aliases for: smooth_stone_slab vs modern stone_slab; petrified_oak_slab vs oak_slab; only red beds before1.12; blue/brown/black/white dye vs lapis/cocoa/ink/bone meal; plain vs carved pumpkins; stone brick variants; sandstone cut/chiseled vs smooth variants; distinct wood stairs/slabs/fences/doors; later redstone items bounded too early in crafting data. Unknown/modded IDs must not become grants by default. Keep ordinary worlds unaffected and registry/storage intact.

Spawn eggs need **egg-item availability**, not just entity introduction: separate wither-skeleton/zombie-villager/donkey/mule eggs are later than the corresponding mobs. No giant eggs/spawn-list additions. [Enchanting dates](<../src/main/java/net/historicalexperience/gameplay/HistoricalEnchanting.java>) provide a positive enchantment map; snapshot12w49a must not gain later enchantments just because a book item's base ID exists. Check potion variants, custom effects, entity/block-entity/container data and other rich components, not only item IDs.

## Safe authority options (design, not installed)

A server-backed custom catalog is possible, but a smaller safe alternative may reuse native creative packets with **two-sided conservation guards**, not a naive incoming-item rejection:

* Reject new unavailable/rich-unsupported grants, drops and creative CLONE actions.
* Also reject clearing/replacing an existing unavailable stack through creative slot-write packets, resynchronizing the inventory **and cursor** from server truth. Otherwise an empty-then-fill move can delete an already owned future item.
* An identical old stack in the same slot is a no-op, not a new grant. Preserve exact count/components.
* If using this conservative approach, explain that such stacks cannot be rearranged/duplicated through the creative picker; normal server-owned chest/Survival inventory movement must remain available. Test it rather than assuming cursor sync works.
* Never scrub saved inventories, global creative-tab caches or registries. Filter display lists per active world/selection so disconnect/world switching restores ordinary catalogs.
* Network hooks must enforce main-thread execution before inspecting/mutating inventories. Preserve vanilla permission/count/drop limits.

A standalone **client presentation** legacy screen (8×9 catalog plus9-slot hotbar, own scroll/click handling, native server-authoritative grant packets) avoids invasive edits to vanilla's hardcoded45-slot picker. For other historical profiles, a narrow filter on the native picker can preserve the normal UI without globally mutating tab caches. Inspect actual1.21.1 signatures/behavior before selecting this approach.

## Remaining acceptance

Real Minecraft controls: available and unavailable catalog items/variants; search, hotbar load, drops, clone and forged grants; component-bearing owned unsupported items preserved through moves/rejects; ordinary-world and same-process profile switch controls; old-menu bounds and scroll/interaction screenshots. Separately run real snapshot B authority and single/stacked anvil input/output/XP/component tests. Unit metadata/transaction tests and selector screenshots are not these runtime tests.
