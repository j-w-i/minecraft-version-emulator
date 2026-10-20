"""Apply the project's per-file licence headers.

This is a run-once migration tool with a dry-run mode; it is kept so the header policy is
auditable and re-appliable rather than describing a set of hand edits nobody can reproduce.

Policy (see docs/LICENSING.md):

  * A file that contains material copied or adapted from Nostalgic Tweaks keeps a short
    copyright/licence summary naming the upstream project and stating that it was modified.
    Upstream ships no per-file notices, so there is no existing header to preserve verbatim;
    the upstream project path is already named in each file's own documentation comment.
  * Every other file receives the plain project header.

The header is inserted above the first line of the file, so `package`, imports and any
existing block or javadoc comment are left exactly where they were. No other line is touched.

Usage:
    python scripts/apply-licence-headers.py --check     # report files missing a header
    python scripts/apply-licence-headers.py --dry-run   # show what would change
    python scripts/apply-licence-headers.py             # write the headers
"""
from __future__ import annotations

import argparse
import os
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(REPO, "src")

MARKER = "SPDX-License-Identifier:"

NT_LINE = (
    " * Contains material copied or adapted from Nostalgic Tweaks\n"
    " * (https://github.com/Nostalgica-Reverie/Nostalgic-Tweaks), Copyright (C) Adrenix and\n"
    " * Nostalgic Tweaks contributors, licensed LGPL-3.0-only, and MODIFIED for this project.\n"
    " * The upstream file or tweak this class came from is named in its documentation comment.\n"
)

HEADERS = {
    "project": (
        "/*\n"
        " * Minecraft Historical Experience\n"
        " * Copyright (C) 2026 Minecraft Historical Experience contributors\n"
        " *\n"
        " * This program is free software: you can redistribute it and/or modify it under the\n"
        " * terms of the GNU Lesser General Public License as published by the Free Software\n"
        " * Foundation, either version 3 of the License, or (at your option) any later version.\n"
        " *\n"
        " * This program is distributed in the hope that it will be useful, but WITHOUT ANY\n"
        " * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A\n"
        " * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.\n"
        " *\n"
        " * You should have received a copy of the GNU Lesser General Public License along with\n"
        " * this program. If not, see <https://www.gnu.org/licenses/>.\n"
        " *\n"
        " * SPDX-License-Identifier: LGPL-3.0-or-later\n"
        " */\n"
    ),
    "nt": (
        "/*\n"
        " * Minecraft Historical Experience\n"
        " * Copyright (C) 2026 Minecraft Historical Experience contributors\n"
        " *\n"
        + NT_LINE
        + " *\n"
        " * This file is part of a work distributed under the GNU Lesser General Public License,\n"
        " * version 3. See LICENSE and THIRD_PARTY_NOTICES.md.\n"
        " *\n"
        " * SPDX-License-Identifier: LGPL-3.0-or-later AND LGPL-3.0-only\n"
        " */\n"
    ),
}

# Files that carry material copied or adapted from Nostalgic Tweaks. Each one's own documentation
# comment names the upstream file or tweak constant it came from; this list is the machine-readable form
# of that. Every entry was checked by reading the file, not by keyword search — an earlier revision of
# this list was built by grepping for the words "Nostalgic Tweaks", which both missed eleven files that
# name an upstream NT class without naming the project, and wrongly included five files whose own prose
# says Nostalgic Tweaks has no equivalent for what they implement.
#
# Deliberately absent, and why:
#   mixin/FoodDataHistoricalRegenerationMixin.java — "Nostalgic Tweaks has no equivalent"; the rule is
#       implemented from research. It hooks the same FoodData class NT's hunger mixin hooks, which is a
#       shared target rather than shared expression, so it is not classified as NT-derived.
#   client/lighting/HistoricalSkyColours.java — the values are measured from the original clients; NT is
#       cited as a rejected source for a flat value it gets wrong.
#   mixin/ItemRendererHistoricalChestMixin.java — original code with no NT provenance; it consumes the
#       chest models that are NT assets, which is recorded in docs/PROVENANCE.md instead.
#
# Classified as NT-derived even though the file says NT "has no equivalent" for the behaviour, because the
# file's own text names an NT feature it is ported alongside, or ports upstream's table into this project's
# hook. Over-attributing is the safe error here, and docs/PROVENANCE.md records the distinction:
#   mixin/PlayerHistoricalCriticalHitMixin.java — "only DISABLE_CRITICAL_HIT, which is ported separately".
#   mixin/PlayerHistoricalExhaustionMixin.java — exhaustion costs are NT subject matter.
#   mixin/DataComponentHolderHistoricalFoodMixin.java — carries NT's food-value table.
NT_FILES = [
    "main/java/net/historicalexperience/client/HistoricalRecipeBookButton.java",
    "main/java/net/historicalexperience/client/hud/FlagHolder.java",
    "main/java/net/historicalexperience/client/hud/HudElement.java",
    "main/java/net/historicalexperience/client/hud/HudHelper.java",
    "main/java/net/historicalexperience/client/hud/HistoricalVersionOverlay.java",
    "main/java/net/historicalexperience/client/lighting/HistoricalChestTweaks.java",
    "main/java/net/historicalexperience/client/lighting/HistoricalDataLayer.java",
    "main/java/net/historicalexperience/client/lighting/HistoricalFogTweaks.java",
    "main/java/net/historicalexperience/client/lighting/HistoricalLightingTweaks.java",
    "main/java/net/historicalexperience/client/lighting/HistoricalLightmap.java",
    "main/java/net/historicalexperience/client/lighting/HistoricalOldLightMath.java",
    "main/java/net/historicalexperience/client/lighting/HistoricalOldLighting.java",
    "main/java/net/historicalexperience/client/lighting/HistoricalSmoothLighting.java",
    "main/java/net/historicalexperience/client/lighting/HistoricalStars.java",
    "main/java/net/historicalexperience/client/lighting/HistoricalWorldTweaks.java",
    "main/java/net/historicalexperience/client/render/ArmorHelper.java",
    "main/java/net/historicalexperience/client/render/MobArmHelper.java",
    "main/java/net/historicalexperience/client/render/SwordBlockingRenderer.java",
    "main/java/net/historicalexperience/client/tooltip/HistoricalTooltips.java",
    "main/java/net/historicalexperience/mixin/client/AbstractContainerScreenHistoricalTooltipMixin.java",
    "main/java/net/historicalexperience/mixin/client/TooltipRenderUtilHistoricalMixin.java",
    "main/java/net/historicalexperience/version/ActiveVersion.java",
    "main/java/net/historicalexperience/gameplay/HistoricalDamageValues.java",
    "main/java/net/historicalexperience/gameplay/HistoricalFarmingInteractionEvents.java",
    "main/java/net/historicalexperience/gameplay/HistoricalFoodHelper.java",
    "main/java/net/historicalexperience/gameplay/HistoricalGameplayTweaks.java",
    "main/java/net/historicalexperience/gameplay/HistoricalLeftClickEvents.java",
    "main/java/net/historicalexperience/gameplay/HistoricalMobLootHelper.java",
    "main/java/net/historicalexperience/gameplay/HistoricalSwordBlocking.java",
    "main/java/net/historicalexperience/gameplay/HistoricalSwordBlockingEvents.java",
    "main/java/net/historicalexperience/gameplay/HistoricalToolInteractionEvents.java",
    "main/java/net/historicalexperience/mixin/client/AbstractZombieModelHistoricalArmsMixin.java",
    "main/java/net/historicalexperience/mixin/common/AgeableMobHistoricalSpawnMixin.java",
    "main/java/net/historicalexperience/mixin/common/AnimalHistoricalBreedingMixin.java",
    "main/java/net/historicalexperience/mixin/client/AnimationUtilsHistoricalArmBobMixin.java",
    "main/java/net/historicalexperience/mixin/common/AxeAccess.java",
    "main/java/net/historicalexperience/mixin/client/BiomeHistoricalSkyColourMixin.java",
    "main/java/net/historicalexperience/mixin/client/BlockAndTintGetterHistoricalMixin.java",
    "main/java/net/historicalexperience/mixin/common/BlockBehaviourHistoricalDropMixin.java",
    "main/java/net/historicalexperience/mixin/client/BlockBehaviourHistoricalShadeBrightnessMixin.java",
    "main/java/net/historicalexperience/mixin/client/BlockEntityRenderDispatcherHistoricalChestMixin.java",
    "main/java/net/historicalexperience/mixin/client/BlockStateModelLoaderHistoricalChestMixin.java",
    "main/java/net/historicalexperience/mixin/client/ChestBlockHistoricalRenderShapeMixin.java",
    "main/java/net/historicalexperience/mixin/client/ClientLevelHistoricalAttackSoundMixin.java",
    "main/java/net/historicalexperience/mixin/client/ClientLevelHistoricalNightSkyMixin.java",
    "main/java/net/historicalexperience/mixin/common/CocoaBlockHistoricalBonemealMixin.java",
    "main/java/net/historicalexperience/mixin/client/CraftingScreenHistoricalRecipeBookMixin.java",
    "main/java/net/historicalexperience/mixin/common/CropBlockHistoricalBonemealMixin.java",
    "main/java/net/historicalexperience/mixin/common/DataComponentHolderHistoricalDamageMixin.java",
    "main/java/net/historicalexperience/mixin/common/DataComponentHolderHistoricalFoodMixin.java",
    "main/java/net/historicalexperience/mixin/client/DimensionSpecialEffectsHistoricalCloudHeightMixin.java",
    "main/java/net/historicalexperience/mixin/client/EnderChestBlockHistoricalRenderShapeMixin.java",
    "main/java/net/historicalexperience/mixin/common/FishingHookHistoricalMixin.java",
    "main/java/net/historicalexperience/mixin/common/FleeSunGoalHistoricalMixin.java",
    "main/java/net/historicalexperience/mixin/client/FogRendererHistoricalWorldMixin.java",
    "main/java/net/historicalexperience/mixin/common/FoodDataHistoricalHungerMixin.java",
    "main/java/net/historicalexperience/mixin/client/FurnaceScreenHistoricalRecipeBookMixin.java",
    "main/java/net/historicalexperience/mixin/client/GuiAccess.java",
    "main/java/net/historicalexperience/mixin/client/GuiHistoricalAttackIndicatorMixin.java",
    "main/java/net/historicalexperience/mixin/client/GuiHistoricalHudMixin.java",
    "main/java/net/historicalexperience/mixin/client/HumanoidArmorLayerHistoricalTintMixin.java",
    "main/java/net/historicalexperience/mixin/client/HumanoidModelHistoricalBlockingMixin.java",
    "main/java/net/historicalexperience/mixin/client/ImageButtonHistoricalSpritesAccess.java",
    "main/java/net/historicalexperience/mixin/client/InventoryScreenHistoricalOffhandMixin.java",
    "main/java/net/historicalexperience/mixin/client/InventoryShieldSlotMixin.java",
    "main/java/net/historicalexperience/client/inventory/SlotTracker.java",
    "main/java/net/historicalexperience/client/inventory/HistoricalInventoryScreen.java",
    "main/java/net/historicalexperience/mixin/client/InventoryScreenHistoricalRecipeBookMixin.java",
    "main/java/net/historicalexperience/mixin/client/ItemInHandRendererHistoricalBlockingMixin.java",
    "main/java/net/historicalexperience/mixin/client/LayerLightSectionStorageHistoricalMixin.java",
    "main/java/net/historicalexperience/mixin/client/LevelRendererHistoricalStarsAccess.java",
    "main/java/net/historicalexperience/mixin/client/LevelRendererHistoricalStarsMixin.java",
    "main/java/net/historicalexperience/mixin/client/LightEngineHistoricalOldLightingMixin.java",
    "main/java/net/historicalexperience/mixin/client/LightTextureHistoricalMixin.java",
    "main/java/net/historicalexperience/mixin/common/LivingEntityAttackTickerAccess.java",
    "main/java/net/historicalexperience/mixin/common/LivingEntityHistoricalHungerEffectMixin.java",
    "main/java/net/historicalexperience/mixin/common/LivingEntityHistoricalInstantAirMixin.java",
    "main/java/net/historicalexperience/mixin/common/LivingEntityHistoricalInstantEatMixin.java",
    "main/java/net/historicalexperience/mixin/common/LivingEntityHistoricalLootMixin.java",
    "main/java/net/historicalexperience/mixin/common/LivingEntityHistoricalMovementMixin.java",
    "main/java/net/historicalexperience/mixin/common/LivingEntityHistoricalSwordBlockingMixin.java",
    "main/java/net/historicalexperience/mixin/client/MinecraftHistoricalInstantEatMixin.java",
    "main/java/net/historicalexperience/mixin/client/MinecraftHistoricalSmoothLightingMixin.java",
    "main/java/net/historicalexperience/mixin/client/MinecraftHistoricalSwordBlockingMixin.java",
    "main/java/net/historicalexperience/mixin/common/MobHistoricalCustomLootMixin.java",
    "main/java/net/historicalexperience/mixin/common/MobHistoricalEquipmentMixin.java",
    "main/java/net/historicalexperience/mixin/common/MobHistoricalItemPickupMixin.java",
    "main/java/net/historicalexperience/mixin/common/PanicGoalHistoricalMixin.java",
    "main/java/net/historicalexperience/mixin/client/ParticleEngineHistoricalSweepMixin.java",
    "main/java/net/historicalexperience/mixin/common/PlayerHistoricalCombatMixin.java",
    "main/java/net/historicalexperience/mixin/common/PlayerHistoricalCriticalHitMixin.java",
    "main/java/net/historicalexperience/mixin/common/PlayerHistoricalExhaustionMixin.java",
    "main/java/net/historicalexperience/mixin/common/PlayerHistoricalFoodMixin.java",
    "main/java/net/historicalexperience/mixin/common/ProjectileWeaponItemHistoricalInvincibleBowMixin.java",
    "main/java/net/historicalexperience/mixin/common/RangedBowAttackGoalHistoricalMixin.java",
    "main/java/net/historicalexperience/mixin/client/RecipeBookComponentHistoricalMixin.java",
    "main/java/net/historicalexperience/mixin/common/SaplingBlockHistoricalBonemealMixin.java",
    "main/java/net/historicalexperience/mixin/common/ServerLevelHistoricalOrbMixin.java",
    "main/java/net/historicalexperience/mixin/common/SheepAccess.java",
    "main/java/net/historicalexperience/mixin/common/ShovelAccess.java",
    "main/java/net/historicalexperience/mixin/common/StemBlockHistoricalBonemealMixin.java",
    "main/java/net/historicalexperience/mixin/common/TemptGoalHistoricalMixin.java",
    "main/java/net/historicalexperience/mixin/common/ZombieHistoricalSpawnMixin.java",
]


def java_files() -> list[str]:
    found = []
    for root, _dirs, files in os.walk(SRC):
        for name in files:
            if name.endswith(".java"):
                found.append(os.path.relpath(os.path.join(root, name), SRC).replace(os.sep, "/"))
    return sorted(found)


def split_headers(text: str) -> tuple[list[str], str]:
    """Split leading licence-comment blocks from the rest of the file.

    A "licence header" here is any leading block comment containing an `SPDX-License-Identifier:` line. That
    is deliberately content-based rather than text-matched: this tool has changed its own header wording
    twice, and matching on the current wording is what left stale headers in the tree both times.

    Returns the blocks, in order, and the remaining source. The remaining source has no leading blank lines,
    so writing the correct header plus this text is stable under re-running.
    """
    normalised = text.replace("\r\n", "\n")
    blocks: list[str] = []
    while normalised.startswith("/*"):
        end = normalised.find("*/")
        if end == -1:
            break
        block = normalised[:end + 2]
        if "SPDX-License-Identifier:" not in block:
            break
        blocks.append(block)
        normalised = normalised[end + 2:].lstrip("\n")
    return blocks, normalised


def header_exact(blocks: list[str], kind: str) -> bool:
    """Whether the file's leading headers are exactly the single header it should have.

    Compared without the header's trailing newline, which belongs to the separator between the header and the
    code rather than to the header block itself. Comparing with it made this predicate false for every file
    the tool had ever written, which is how a "fix the headers" run turned into a "rewrite all 309 files" run.
    """
    expected = HEADERS[kind].replace("\r\n", "\n").rstrip("\n")
    return len(blocks) == 1 and blocks[0].rstrip("\n") == expected


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="report files whose header is missing or wrong")
    parser.add_argument("--dry-run", action="store_true", help="print the plan without writing")
    args = parser.parse_args()

    nt = set(NT_FILES)
    all_files = java_files()
    unknown = sorted(nt - set(all_files))
    if unknown:
        print("NT_FILES names no such source file (fix the list, do not guess):", file=sys.stderr)
        for name in unknown:
            print(f"  {name}", file=sys.stderr)
        return 2

    # A file with the NT header must be listed as NT-derived; a file listed as NT-derived must carry the NT
    # header. Either mismatch is an error rather than something to paper over.
    expected_kind = {rel: ("nt" if rel in nt else "project") for rel in all_files}

    stale: list[str] = []
    changed: list[str] = []
    for rel in all_files:
        path = os.path.join(SRC, rel)
        with open(path, encoding="utf-8", newline="") as handle:
            text = handle.read()
        kind = expected_kind[rel]
        blocks, body = split_headers(text)
        if header_exact(blocks, kind):
            continue
        stale.append(f"{rel}  [{kind}]  {len(blocks)} leading header(s)")
        if args.check:
            continue
        changed.append(f"{rel}  [{kind}]")
        if not args.dry_run:
            with open(path, "w", encoding="utf-8", newline="") as handle:
                handle.write(HEADERS[kind] + body)

    if args.check:
        if stale:
            print(f"{len(stale)} Java file(s) have a missing or incorrect licence header:")
            for rel in stale:
                print(f"  src/{rel}")
            return 1
        print(f"all {len(all_files)} Java files carry the correct licence header "
              f"({len(nt)} Nostalgic Tweaks-derived, {len(all_files) - len(nt)} project)")
        return 0

    verb = "would change" if args.dry_run else "changed"
    print(f"{verb} {len(changed)} file(s)")
    for line in changed:
        print(f"  {line}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
