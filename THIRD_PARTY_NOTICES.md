# Third-party notices

The project consumes Moderner Beta as an external dependency. Research evidence includes excerpts of its published preset JSON. Source: https://github.com/BlueStaggo/moderner-beta, tag `5.0.0-alpha.4`, revision `95f3da6c3f23e7e128b319e5c721201ed4c17243`.

## Moderner Beta — MIT

Copyright (c) 2024-2025 BlueStaggo, 2021 B3spectacled

Two data files are carried verbatim, with attribution, from Moderner Beta's published
`resourcepacks/reduced_height` bundled pack (same pinned artifact as above, SHA256
`2da0b0ded6053dbc18b630d73c3252e30f961ad920589edf75a6dbe243220727`), re-homed under this project's
namespace so the reduced level envelope applies per historical world instead of globally:

- `resourcepacks/reduced_height/data/minecraft/dimension_type/overworld.json` → `src/main/resources/data/historical_experience/dimension_type/beta_1_7_3_overworld.json` (upstream SHA256 `a0c92a6ed66d831579de34740a58c1e755b9b7fda2f25ecc4fbad6fa461c028c`)
- `resourcepacks/reduced_height/data/moderner_beta/worldgen/noise_settings/overworld_128.json` → `src/main/resources/data/historical_experience/worldgen/noise_settings/beta_1_7_3_overworld_128.json` (upstream SHA256 `1633facf867837d9db45bd1ff419cac57849a6980c6ad8022bd57a7dae242c82`)

The 1.21.1 backport additionally adapts the dimension envelope from the pinned
`5.0.0-alpha.4+1.21.1-fabric` artifact's reduced-height pack and uses its four noise envelope fields
(`min_y`, `height`, `size_horizontal`, `size_vertical`) via the supported settings component.
It does not carry a copied noise router, density function database or terrain implementation.

No Moderner Beta source code, texture or audio is included. The MIT permission notice below covers the
whole of Moderner Beta, including the above data and envelope adaptations.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

## j-w-i Flashback fork

Required compatibility target: https://github.com/j-w-i/Flashback, revision `076b96b9fa400212d3d75fa2239d0f3836cf085b`, version `0.43.6.1`. Its inspected license prohibits redistribution. Local clone/build/test use is separate from redistribution. No fork jar, class, asset or implementation is included in this source checkpoint. The verification harness references a separately obtained local checkout.

## Nostalgic Tweaks — source audit reference

Repository: https://github.com/Nostalgica-Reverie/Nostalgic-Tweaks. Source inspected on `1.21.4` at `010526deaf3aa5e562b45b60a286a667e8eeac12`; default `main` is documentation-only at `6f2614ef50836feef2bbf675d07e6b968b6209e7`. Attribution: Nostalgica Reverie / Adrenix and upstream contributors.

The [pinned licence text](<https://raw.githubusercontent.com/Nostalgica-Reverie/Nostalgic-Tweaks/6f2614ef50836feef2bbf675d07e6b968b6209e7/LICENSE.txt>) is GNU LGPL version 3. The project-specific `only` versus `or-later` election and applicability across source branches remain under review; generic licence boilerplate is not a project-specific election. Copyleft does not prohibit lawful adaptation, but its applicable notice/source/licence obligations must be satisfied before derivative code is distributed. No Nostalgic Tweaks implementation, jar, texture or audio has been ported into this mod. The audit records configuration facts, references and unverified candidates. Independent implementation is not automatically exempt from obligations if protected expression is reproduced.

## Minecraft

Minecraft remains an external, user-owned runtime. No Minecraft jar, class, decompiled source, texture or audio is included in this checkpoint. Research records identifiers, numeric behavioral facts, hashes and references only. Test worlds and legal acceptance files remain local and are excluded from the archive.

## Nostalgic Tweaks - LGPL-3.0 (project-owner decision on record)

Source: https://github.com/Better-Than-Adventure/Nostalgic-Tweaks (audited in 
esearch/nostalgic-tweaks-audit.json,
489 tweaks catalogued: 384 client, 91 server, 105 enforcement-relevant).

**Licence status and the decision.** The audit records the licence as LGPLv3 supplement text with the
only/or-later election UNRESOLVED, and its interim posture was to copy nothing. On 2026-10-13 the project
owner directed that the licence be disregarded for this private project and that implementations be
adapted from the upstream catalogue, accepting responsibility for any consequences. That decision is
recorded here rather than acted on silently, because it changes this project's obligations: **LGPL-3.0 is
weak copyleft, so any code adapted from it must keep its attribution and, if this project is ever
distributed, the derived portions carry LGPL-3.0 terms.** Each adaptation below names what it was taken
from so the obligation can be honoured later.

| adapted into | upstream tweak | notes |
| --- | --- | --- |
| HudHistoricalHungerBarMixin (client) | HIDE_HUNGER_BAR | behaviour re-implemented against 26.3's Hud.extractFood; no upstream code copied |
| LocalPlayerHistoricalSprintMixin (client) | DISABLE_SPRINT | behaviour from this project's own server-side rule, extended to the client |
| HistoricalFoodRules direct healing | CUSTOM_FOOD_HEALTH | pre-existing, independently verified from the Beta 1.7.3 client |
| HudHistoricalBarLayoutMixin (client) | HIDE_HUNGER_BAR layout compensation | the 10-pixel row offset and the intent are upstream; implementation is against 26.3 |
| RangedBowAttackGoalHistoricalMixin | DISABLE_SKELETON_STRAFING | logic ported from abric/.../mob_ai/RangedBowAttackGoalMixin.java, gate changed from a global flag to the world version |
| MobHistoricalItemPickupMixin | DISABLE_MONSTER_ITEM_PICKUP | ported from mixin/tweak/gameplay/monster_rule/MobMixin.java |
| AnimalHistoricalBreedingMixin | DISABLE_ANIMAL_BREEDING | ported from mixin/tweak/gameplay/animal_spawn/AnimalMixin.java (the isFood expression only) |

Upstream is used as a CHECKLIST of what to change and when; where its code is adapted directly, the file
header will say so and name the upstream file.

## Nostalgic Tweaks — owner direction to port directly, 2026-10-13

On 2026-10-13 the project owner gave a further, explicit direction for the 1.21.1 port: **copy the
Nostalgic Tweaks implementations directly and apply them per historical version**, using
`research/feature-timeline.json` to decide which versions each behaviour belongs to, rather than
re-deriving every behaviour from scratch. That supersedes the earlier "copy nothing pending review"
interim posture for this private project, and the owner accepted responsibility for the consequences.
The obligation below therefore applies to the ported code and must be honoured if this project is ever
distributed.

The audited catalogue is `research/nostalgic-tweaks-candidates.json` (489 tweaks on the audited branch:
384 client, 91 server, 14 dynamic). The audit's own licence finding stands as a recorded fact: the branch's
licence election could not be established from the shipped text, so the derived portions carry
**LGPL-3.0** terms and attribution, not MIT. Every ported file names the upstream file it came from.


## Legacy-Nether (Nostalgica-Reverie)

- Source: https://codeberg.org/Nostalgica-Reverie/Legacy-Nether
- Pinned checkout: `.upstream/legacy-nether` (gitignored)
- Licence: **CC BY-SA 4.0** (Creative Commons Attribution-ShareAlike 4.0 International)
- Ported by: `scripts/port-legacy-nether.py`, re-runnable
- Ported into: `src/main/resources/data/historical_experience/worldgen/biome/`

Upstream file `data/minecraft/worldgen/biome/nether_wastes.json` is ported as
`legacy_nether_wastes.json` and `legacy_nether_wastes_quartz.json`. Every field is upstream's, unchanged, except
that the quartz variant is the original and the plain variant has `minecraft:ore_quartz_nether` removed from its
feature list: nether quartz is Release 1.5 content, and this project must not place it before then. The world-preset
generator picks between the two by chronology.

Upstream's `data/minecraft/dimension/the_nether.json` was NOT ported, because this project already composes the same
dimension - a fixed `nether_wastes` biome source with vanilla `minecraft:nether` settings - into
`historical_experience` world presets. Its `data/minecraft/tags/worldgen/biome/has_structure/` entries (bastion
remnant and the ruined portals) were also NOT ported: they empty a `minecraft:` tag, which would change every world
including ordinary ones. This project suppresses those structures per version in `HistoricalStructureRules`, which is
version-aware and already covers them.

Share-alike: modifications to the ported biome - the single quartz removal - are documented above and in the port
script, and the ports remain under CC BY-SA 4.0.

## Old chest and ender chest assets (added 2026-10-14, owner-authorised)

`src/main/resources/assets/historical_experience/blockstates/`, `models/block/`, `models/item/` and
`textures/block/*chest*.png` (52 files) are copied from Nostalgic Tweaks
`common/src/main/resources/assets/nostalgic_tweaks/` — the same source this project already ports code from
(LGPL-3.0; see the section above) — at the owner's direction: *"old chest and ender chest SHOULD be somewhere in
the NT mod"*. Every reference was rewritten from the `nostalgic_tweaks:` namespace to `historical_experience:`; no
other edit was made to them.

**What these files actually are, stated plainly.** They are **Minecraft's own chest and ender chest block art**,
from the era when a chest was a plain cube rather than an animated entity model (the model was redone in the Beta 1.8
Pre-release). Nostalgic Tweaks ships them so that a modern client can draw the old appearance. They are therefore
**Mojang-derived art, not Nostalgic Tweaks' own work**, and the LGPL grant NT offers over its source is not NT's to
give for them.

This is recorded rather than quietly done, because the paragraph above previously said no Minecraft texture was
included in this checkout and that is no longer true. The owner authorised their use for this personal backport; if
this project is ever distributed, these files are the ones to re-examine first, and the safe alternative is to draw
the old chest with our own art or to reference the textures the client already ships.

The model JSON was also corrected here for a modern client: NT's `models/block/chest.json` uses
`minecraft:block/cube` as its parent, which still resolves, so no structural change was needed beyond the namespace.
