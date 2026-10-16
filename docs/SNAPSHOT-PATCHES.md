# Snapshot patches and creative inventories

Select **Historical Version → Snapshots** when creating or managing a historical world. These are patches over existing parent profiles, not separate terrain generators:

- **11w49a → Release 1.1 baseline:** press **B** to toggle **Survival ↔ Creative** (one toggle per press, not while held). No cheats are needed for the owner of a private single-player world. The compatibility safety policy refuses privilege escalation on published LAN/dedicated servers or for other players; this is not a claim about the original snapshot's multiplayer code.
- **12w49a → the existing Release 1.4-family baseline:** enchanted books may be applied to **one** block/item in an anvil, including in Survival. Stacked targets are refused even in Creative. Native XP costs, book consumption and enchantment conflict handling remain. The snapshot belongs to **1.4.6**, not 1.4.2.

Existing-world selection uses the normal backup/journal/restart workflow. Changing a patch does not live-swap a running world. Same-parent changes keep the exact generation recipe; existing chunks and player data are not regenerated.

## Creative menus

- **Beta 1.8.1 through before Release 1.3.1:** own scrollable **8-column ×9-row** catalog, with a 9-slot hotbar. Clicking a catalog cell while holding a cursor stack **clears it**, rather than switching to the second item. Same-item and right-click discard are supported too; hotbar placement and keyboard shortcuts remain separate.
- Other declared historical profiles use the host's native tabbed layout with dated item/variant filtering. Unknown/newer/modded entries are not new historical creative grants. Ordinary worlds retain vanilla creative behavior.
- Server checks cover raw creative insertion/drop and creative duplication, not just visible buttons. Enchantment/potion data and rich component overrides are checked, not only item IDs.
- Stored unsupported content is **kept**. To prevent an empty-then-fill creative move from deleting it, raw creative writes freeze unsupported existing stacks. Move these using normal chest or Survival inventory operations instead. Nothing scrubs their saves.
- A carried stack clicked anywhere below the catalogue — the separator rows above the hotbar, the side margins, and the strip under it — goes into the hotbar slot under the pointer. Clicking that gap no longer throws the item into the world, and no longer does nothing until the pointer happens to land squarely on a painted hotbar box. Non-slot padding elsewhere (the title strip and the catalogue's side margin) keeps the cursor. Shift-clicking a grantable hotbar stack deletes that whole stack without dropping it. Unsupported stored stacks remain protected as described above.
- No giants were added to spawn lists.

## Scope and verification

This is a curated positive catalog, not a claim to reproduce every original numeric item order, pixel asset, item membership or snapshot algorithm. Modern registry/storage and runtime item rendering are retained. The linked resource pack was inaccessible/licensing unverified; no code/assets from it were copied.

[Research and limitations](<../research/snapshot-patches.json>) · [Catalog rules](<../research/creative-menu-catalog.json>) · [Current implementation/evidence](<../PROGRESS.md>)

Developer acceptance uses only new project-owned worlds under `run/client`, never personal saves or terms acceptance. Reproduce with Java25 for Gradle (Minecraft uses the pinned Java21 toolchain): `gradlew --offline runClientQuickPlay -PtitleStart -PsnapshotProbe` and separately `gradlew --offline runClientQuickPlay -PtitleStart -PsnapshotAnvilProbe`. Inspect their JSON receipts under `run/client`; a normal process exit alone is not a passing verdict. [11w49a acceptance including hotbar band/QUICK_MOVE deletion](<../run/client/snapshot-probe-1792170860656.json>) and [12w49a/unpatched/ordinary controls](<../run/client/snapshot-anvil-probe-1792162354757.json>) both pass on Minecraft1.21.1/Java21. The four-world run proves scope clearing and ordinary future-item search/CLONE restoration within the same client process. Dedicated/LAN sessions, exact original snapshot algorithms and complete historical catalogs are not certified by these runs.
