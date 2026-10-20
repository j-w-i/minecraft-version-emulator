/*
 * Minecraft Historical Experience
 * Copyright (C) 2026 Minecraft Historical Experience contributors
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package net.historicalexperience.version;

import java.util.List;

/**
 * Every historical deviation from modern Minecraft 1.21.1 that holds over a window of the timeline.
 *
 * <p>This is the one place such a window is declared. A rule elsewhere in the project never compares versions: it
 * asks {@link Features#active(Feature)} (the running world) or {@link Features#active(Feature, String)} (a named
 * version). What the rule then DOES is the owning system's business; this table only says WHEN it applies.
 *
 * <h2>Every feature is a deviation, so an ordinary world is inactive by construction</h2>
 *
 * <p>A feature describes how a historical version differs from the modern game: "no Nether", "food does not stack",
 * "no sneaking". A world with no declared version is inactive for every feature, and so is an unknown id. There is no
 * per-feature default to get wrong; that is what caused the ordinary-world Nether and food-stack defect when the
 * rules were positive predicates with their own null handling.
 *
 * <h2>The boundaries are the code's, not necessarily history's</h2>
 *
 * <p>Each window was transcribed from the rule it replaced, including the cases where the project deliberately uses
 * a declared build as the proxy for a boundary that falls between two declared builds (Release 1.4.2 is proxied by
 * Release 1.6.4, Release 1.2.1 by Release 1.2.5), and one case where the code and its evidence disagree
 * ({@link #NO_ANIMAL_BREEDING}: the evidence says Prerelease 2, the gate has always been Prerelease 3). Changing one is
 * a historical decision with its own test change, not a refactor.
 *
 * <p>Some old rules are NOT in this table because they are not windows: value computations (the jump-experience amount,
 * the arrow charge), the data-derived "this version awards no experience" rule, and the Level-based resolvers.
 */
public enum Feature {
    /** No skeleton strafing. */
    NO_SKELETON_STRAFING(Domain.MOBS, FeatureWindow.before(Milestones.BETA_1_8),
        "RangedBowAttackGoal, which is what strafes while shooting, belongs to the goal-based mob AI "
                        + "that arrives with Beta 1.8/1.9, so none of the pre-Beta-1.8 versions had it. Upstream "
                        + "tweak: DISABLE_SKELETON_STRAFING."),
    /** No critical hits. */
    NO_CRITICAL_HITS(Domain.COMBAT, FeatureWindow.before(Milestones.BETA_1_8),
        "Critical hits were added in the Beta 1.8 Pre-release, so Infdev, Alpha and Beta 1.7.3 had "
                        + "none and Beta 1.8.1 onwards did. Upstream tweak: DISABLE_CRITICAL_HIT."),
    /** No animal panic. */
    NO_ANIMAL_PANIC(Domain.MOBS, FeatureWindow.before(Milestones.BETA_1_8),
        "Animals fleeing when attacked is the Beta 1.8 Pre-release (feature-timeline mechanics row "
                        + "\"animals fleeing when attacked (panic)\"), so Infdev, Alpha and Beta 1.7.3 had no panic "
                        + "response and every later declared version did. Upstream tweak: DISABLE_ANIMAL_PANIC."),
    /** No monster sun avoidance. */
    NO_MONSTER_SUN_AVOIDANCE(Domain.MOBS, FeatureWindow.before(Milestones.RELEASE_1_2_5),
        "Monsters BURNING in daylight is Indev 20100213 - in range everywhere - but the goal that makes "
                        + "them run for shade is Release 1.2.1 (12w04a). Upstream tweak: DISABLE_MONSTER_AVOID_SUN."),
    /** Old skeleton drops. */
    OLD_SKELETON_DROPS(Domain.MOBS, FeatureWindow.before(Milestones.RELEASE_1_2_5),
        "A skeleton dropping its bow is Release 1.2.1 (12w06a, 8.5%): the timeline records arrows from "
                        + "Indev 20100219 and bones from Beta 1.2, with the bow as a separate later behaviour. "
                        + "Upstream tweak: OLD_SKELETON_DROPS."),
    /** No animal breeding. */
    NO_ANIMAL_BREEDING(Domain.MOBS, FeatureWindow.before(Milestones.BETA_1_9_PRE_3),
        "Animal breeding AND tempting arrive together in Beta 1.9 Prerelease 2, so Beta 1.8.1 had "
                        + "neither. Baby animals are Beta 1.9 Prerelease 3. Upstream tweaks: DISABLE_ANIMAL_BREEDING, "
                        + "DISABLE_ANIMAL_TEMPTING, DISABLE_BABY_ANIMAL_SPAWNING. This is a PORT: an earlier note here "
                        + "called it a project rule after a worklist search found nothing, but the worklist is a "
                        + "200-row subset of the research and the timeline itself names the tweak under the feature "
                        + "'animal breeding'."),
    /** No animal tempting. */
    NO_ANIMAL_TEMPTING(Domain.MOBS, FeatureWindow.before(Milestones.BETA_1_9_PRE_3),
        "Animals following a player who holds food arrives with breeding. Gated at Beta 1.9 Prerelease 3 exactly like breeding, although the evidence dates tempting to Prerelease 2: the boundary is preserved as the code had it. Upstream tweak: DISABLE_ANIMAL_TEMPTING."),
    /** No baby animals. */
    NO_BABY_ANIMALS(Domain.MOBS, FeatureWindow.before(Milestones.BETA_1_9_PRE_3),
        "Baby animals arrive in Beta 1.9 Prerelease 3, so Prerelease 3 itself has them. Upstream tweak: DISABLE_BABY_ANIMAL_SPAWNING."),
    /** No monster item pickup. */
    NO_MONSTER_ITEM_PICKUP(Domain.MOBS, FeatureWindow.before(Milestones.RELEASE_1_6_4),
        "Monsters picking dropped items up is 1.4.2 (12w32a/12w34b). Every declared version before "
                        + "Release 1.6.4 predates it; Release 1.6.4 does not. Upstream tweak: "
                        + "DISABLE_MONSTER_ITEM_PICKUP."),
    /** No monster gear. */
    NO_MONSTER_GEAR(Domain.MOBS, FeatureWindow.before(Milestones.RELEASE_1_6_4),
        "Mobs spawning with armour and weapons is 1.4.2 as well, having existed before Indev "
                        + "20100202 and been removed. Upstream tweak: DISABLE_MONSTER_ITEM_SPAWN."),
    /** No monster enchants. */
    NO_MONSTER_ENCHANTS(Domain.MOBS, FeatureWindow.before(Milestones.RELEASE_1_6_4),
        "Mobs spawning with enchanted gear is Release 1.4.2 (12w32a), the same boundary as the armour "
                        + "and weapons themselves, proxied by Release 1.6.4. Upstream tweak: "
                        + "DISABLE_MONSTER_ENCHANT_SPAWN."),
    /** No baby zombies. */
    NO_BABY_ZOMBIES(Domain.MOBS, FeatureWindow.before(Milestones.RELEASE_1_6_4),
        "Baby zombies spawning naturally is Release 1.6.2 (feature-timeline mechanics row \"baby "
                        + "zombies spawning naturally\"), proxied by Release 1.6.4 as the only declared version "
                        + "after it. Baby zombified piglins share the same date and the same hook. Upstream "
                        + "tweaks: DISABLE_BABY_ZOMBIE_SPAWN, DISABLE_BABY_PIGLIN_SPAWN."),
    /** No attack cooldown. */
    NO_ATTACK_COOLDOWN(Domain.COMBAT, FeatureWindow.allDeclared(),
        "The 1.9 attack cooldown: attacks are always at full strength. Every declared version predates Release 1.9, so the window is every declared version. Upstream tweak: DISABLE_ATTACK_COOLDOWN."),
    /** No sweep attack. */
    NO_SWEEP_ATTACK(Domain.COMBAT, FeatureWindow.allDeclared(),
        "The 1.9 sweep attack: hitting one target never damages the ones beside it. Every declared version predates Release 1.9."),
    /** No swimming. */
    NO_SWIMMING(Domain.PLAYER, FeatureWindow.allDeclared(),
        "The 1.13 swimming pose and movement. Every declared version predates it."),
    /** Old zombie arms. */
    OLD_ZOMBIE_ARMS(Domain.MOBS, FeatureWindow.allDeclared(),
        "Zombie arm-swinging attack animations were REMOVED in Indev 20100202, and every declared profile "
                        + "is later (the oldest is Infdev 20100420), so the swinging arms are modern behaviour in all "
                        + "ten. Upstream tweak: OLD_ZOMBIE_ARMS."),
    /** No tool shaping. */
    NO_TOOL_SHAPING(Domain.ITEMS, FeatureWindow.allDeclared(),
        "Axe stripping (1.13, 17w47a) and shovel pathing (1.9, 15w31a) are modern features every declared version predates; one gate covers both. Upstream: DISABLE_AXE_STRIPPING and DISABLE_SHOVEL_PATHING."),
    /** Old fishing loot. */
    OLD_FISHING_LOOT(Domain.ITEMS, FeatureWindow.allDeclared(),
        "Fishing yielded only raw fish from its Alpha 1.2.0 introduction until Release 1.7.2 (13w36a). Release 1.6.4 is the newest declared version, so every declared version has the old single-item catch."),
    /** Punch shears. */
    PUNCH_SHEARS(Domain.MOBS, FeatureWindow.before(Milestones.BETA_1_7_3),
        "Before shears existed, PUNCHING a sheep shorn it and dropped its wool; the Alpha v1.1.2_01 client's "
                        + "sheep class overrides attackEntityFrom with `if (!this.sheared && attacker instanceof "
                        + "EntityLiving) { this.sheared = true; drop 1 + random.nextInt(3) of Item.cloth }` and has NO "
                        + "death-drop override at all, so killing it yielded nothing. Shears are Beta 1.7: the string "
                        + "\"shears\" is absent from alpha-1.1.2_01-client.jar and present in beta173-client.jar, and the "
                        + "owner confirmed the version. So this gate applies to every declared version older than Beta "
                        + "1.7.3, and Beta 1.7.3 itself uses shears."),
    /** Old tool durability. */
    OLD_TOOL_DURABILITY(Domain.ITEMS, FeatureWindow.before(Milestones.BETA_1_2),
        "Tool durability rose across the board in Beta 1.2: version-transitions.json's changed list reads "
                        + "\"tool durability: wood 33 -> 59, stone 65 -> 131, iron 129 -> 250, diamond 1025 -> 1561\", and "
                        + "research/minecraft-version-history.md records the same. So every declared version older than "
                        + "Beta 1.2 breaks tools sooner. Gold is deliberately not covered: the same sentence says gold "
                        + "tools gained faster mining \"while retaining old durability\", so there is no old value to apply "
                        + "and inventing one is exactly what this project refuses to do."),
    /** End portal frame explodes. */
    END_PORTAL_FRAME_EXPLODES(Domain.BLOCKS, FeatureWindow.exactly("historical_experience:beta_1_9_pre_3"),
        "Beta 1.9 Prerelease 3 ONLY. minecraft.wiki's End Portal Frame history, Java Edition table, says "
                        + "of Beta 1.9 Prerelease 3: 'It can drop as an item due to explosions', and of Beta 1.9 "
                        + "Prerelease 4: 'The End portal can no longer be broken by explosions'. So for exactly one "
                        + "declared build the frame is blast-destroyable and yields its own item, and from Pre4 onward it "
                        + "is indestructible as it is today. The same history rows give two further Pre3-only facts not "
                        + "yet implemented: the block emits light at level 15 where Pre4 and later emit 1, and the item "
                        + "form is a flat texture of the filled frame with an empty tooltip. The owner named this bug "
                        + "specifically and called it very important."),
    /** End portal frame light 15. */
    END_PORTAL_FRAME_LIGHT_15(Domain.BLOCKS, FeatureWindow.exactly("historical_experience:beta_1_9_pre_3"),
        "Beta 1.9 Prerelease 3 ONLY, the second half of the same history row that carries the explosion drop. "
                        + "minecraft.wiki, End Portal Frame history, Java Edition: of Pre3, 'Gives off a light level of 15'; "
                        + "of Pre4, 'Now emits light level of 1'. So for exactly one declared build the frame lit its "
                        + "surroundings like a torch and the next build dimmed it. The modern value is deliberately not "
                        + "written into the code - the hook overrides Pre3 and leaves every other build reading whatever "
                        + "the registry declares, so a future change to the block cannot be frozen out by this feature."),
    /** Jump experience bug. */
    JUMP_EXPERIENCE_BUG(Domain.PLAYER, FeatureWindow.exactly("historical_experience:beta_1_9_pre_1"),
        "Beta 1.9 Prerelease 1 ONLY, and our own corpus states it twice. minecraft-version-history.md item 6 "
                        + "for that build: 'An erroneous jump event grants 1-100 XP; auto-repeat jump was removed', item "
                        + "13: 'XP from jumping and disabled auto-repeat; Pre2 removes the XP bug and restores held-jump "
                        + "auto-repeat', and item 11 of Prerelease 2: 'XP-on-jump debug bug removed; held-jump "
                        + "auto-repeat restored'. historical-features.json records the same as 'Experience gained from "
                        + "jumping as a test'. The range is 1-100, so a single jump can be worth a hundred experience and "
                        + "the amount is random per jump. The disabled auto-repeat half is a client input behaviour and is "
                        + "recorded rather than implemented."),
    /** Silky spawners. */
    SILKY_SPAWNERS(Domain.BLOCKS, FeatureWindow.exactly("historical_experience:beta_1_9_pre_4"),
        "Beta 1.9 Prerelease 4 ONLY, and this is the real in-range version of the owner's 'mineable spawners' "
                        + "bug. minecraft.wiki's Monster Spawner history, Java Edition table, says of Beta 1.9 Prerelease 4: "
                        + "'Monster spawners now drop when mined using a tool enchanted with the new Silk Touch "
                        + "enchantment', and of Beta 1.9 Prerelease 5: 'Monster spawners no longer drop when broken using a "
                        + "tool enchanted with Silk Touch.' So exactly one declared build dropped the spawner and the next "
                        + "one took it away. The owner's premise named the Infdev builds, which is wrong for this matrix - "
                        + "the block was ADDED in Infdev 20100625-1917, after both declared Infdev profiles, so there were "
                        + "no spawners to mine. This is the bug that premise was reaching for."),
    /** Snowball surplus. */
    SNOWBALL_SURPLUS(Domain.BLOCKS, FeatureWindow.exactly("historical_experience:alpha_1_0_6"),
        "Alpha 1.0.6 ONLY. minecraft.wiki, Java Edition Alpha v1.0.6, Changes > Snow block: 'Now require only "
                        + "4 snowballs to be crafted instead of 9. They still always drop 6 snowballs, allowing for an "
                        + "infinite source of snowballs.' The recipe half is already the modern one - verified in the jar, "
                        + "not assumed: data/minecraft/recipe/snow_block.json is a 2x2 of snowballs and "
                        + "data/minecraft/loot_table/blocks/snow_block.json returns a count of four - so the surplus is "
                        + "exactly this one build returning six, and only this build needs overriding. The fix boundary was "
                        + "not established by the research, which is why the gate is equality on the one build that is "
                        + "documented rather than a range: a range invented from a single data point is how one build's bug "
                        + "gets handed to three innocent neighbours."),
    /** Instant bow. */
    INSTANT_BOW(Domain.ITEMS, FeatureWindow.before(Milestones.BETA_1_8),
        "A bow fired the moment it was used before the Beta 1.8 Pre-release, which made arrows need charging. Upstream tweak: INSTANT_BOW."),
    /** Arrow speed. */
    ARROW_SPEED(Domain.ITEMS, FeatureWindow.before(Milestones.BETA_1_8),
        "The charge a bow needed before the Beta 1.8 Pre-release. Upstream tweak: ARROW_SPEED (default 70)."),
    /** Invincible bow. */
    INVINCIBLE_BOW(Domain.ITEMS, FeatureWindow.before(Milestones.BETA_1_8),
        "A bow took no damage from shooting before the Beta 1.8 Pre-release. Upstream tweak: INVINCIBLE_BOW."),
    /** Instant air. */
    INSTANT_AIR(Domain.PLAYER, FeatureWindow.before(Milestones.BETA_1_8),
        "Air refills the instant the head breaks the surface, grouped by Nostalgic Tweaks with swimming (MECHANICS_SWIMMING, INSTANT_AIR) and gated on the same pre-Beta-1.8 boundary."),
    /** Instant bone meal. */
    INSTANT_BONE_MEAL(Domain.ITEMS, FeatureWindow.before(Milestones.RELEASE_1_5),
        "One bone meal fully grew a plant until Release 1.5. minecraft.wiki, Bone Meal history: 1.5 13w04a nerfed it. Release 1.5 is the first declared version at or after 13w04a."),
    /** Punch tnt. */
    PUNCH_TNT(Domain.BLOCKS, FeatureWindow.before(Milestones.BETA_1_7_3),
        "Hitting TNT primed it until Beta 1.7. minecraft.wiki, TNT history. The catalogue maps Beta 1.7 to the Beta 1.7.3 profile."),
    /** Grass seeds. */
    GRASS_SEEDS(Domain.ITEMS, FeatureWindow.before(Milestones.BETA_1_6),
        "A hoe on grass could throw wheat seeds until Beta 1.6 Test Build 3. minecraft.wiki, Wheat Seeds history. Beta 1.6 already lacks it."),
    /** No fishing experience. */
    NO_FISHING_EXPERIENCE(Domain.ITEMS, FeatureWindow.before(Milestones.RELEASE_1_4),
        "Fishing gave no experience until 1.4.2 (12w37a). The boundary is placed at the Release 1.4 profile: older builds are certainly earlier, and Release 1.4 keeps the experience."),
    /** Ore drops as blocks. */
    ORE_DROPS_AS_BLOCKS(Domain.BLOCKS, FeatureWindow.allDeclared(),
        "Raw iron, gold and copper arrive in Release 1.17, later than every declared version, so ores drop the ore block. Upstream: SELF_BLOCK_DROPS."),
    /** Old sword blocking. */
    OLD_SWORD_BLOCKING(Domain.COMBAT, FeatureWindow.from(Milestones.BETA_1_8),
        "Sword blocking, from the Beta 1.8 Pre-release. The upper end is deliberately open: the real end (Release 1.9) is above every declared version. Blocking did NOT exist before Beta 1.8."),
    /** Beta exhaustion costs. */
    BETA_EXHAUSTION_COSTS(Domain.PLAYER, FeatureWindow.from(Milestones.BETA_1_8),
        "Exhaustion costs are the Beta 1.8 ones (attacking 0.3, a non-sprint jump 0.2). Only exists with a hunger meter. The research does not date when the modern costs arrived, so every declared version with hunger is treated as Beta-era."),
    /** Beta regeneration. */
    BETA_REGENERATION(Domain.PLAYER, FeatureWindow.from(Milestones.BETA_1_8),
        "Regeneration uses the Beta 1.8 model: food 18 or more, once per 80 ticks, and no exhaustion cost. Same deliberate assumption as the exhaustion costs."),
    /** Beta sprint jump exhaustion. */
    BETA_SPRINT_JUMP_EXHAUSTION(Domain.PLAYER, FeatureWindow.between(Milestones.BETA_1_8, Milestones.RELEASE_1_6_4),
        "A sprint jump costs the Beta 1.8 value of 0.8 rather than 0.2. The research dates 0.8 in Beta 1.8, 0.2 in Release 1.3.1 and 0.8 again in Release 1.7.2; Release 1.6.4 is the proxy for both changes, so the window ends there."),
    /** Beta food hunger values. */
    BETA_FOOD_HUNGER_VALUES(Domain.PLAYER, FeatureWindow.between(Milestones.BETA_1_8, Milestones.RELEASE_1_6_4),
        "Cookies and mushroom stew fill their pre-change hunger values (cookie 1 to 2 in Release 1.3.1, stew 8 to 6 in Release 1.4.2); Release 1.6.4 is the proxy for both."),
    /** Beta critical hit formula. */
    BETA_CRITICAL_HIT_FORMULA(Domain.COMBAT, FeatureWindow.between(Milestones.BETA_1_8, Milestones.RELEASE_1_6_4),
        "Critical hits use the Beta 1.8 formula rather than the flat +50%. The formula changed in the 1.6/1.7 era per research/historical-features.json; Release 1.6.4 is the proxy."),
    /** Beta golden apple. */
    BETA_GOLDEN_APPLE(Domain.PLAYER, FeatureWindow.between(Milestones.BETA_1_8, Milestones.RELEASE_1_1),
        "The golden apple is the pre-Release-1.1 item: 10 hunger, always edible, Regeneration for 30 seconds. Release 1.1 remade it; the boundary is Release 1.1 itself, which is declared."),
    /** Old sword damage. */
    OLD_SWORD_DAMAGE(Domain.COMBAT, FeatureWindow.before(Milestones.BETA_1_9_PRE_3),
        "Swords deal the pre-Release-1.0.0 value (diamond sword 10 rather than 7, an offset of three). research/historical-features.json records the decrease at Release 1.0.0."),
    /** Sword block fall protection. */
    SWORD_BLOCK_FALL_PROTECTION(Domain.COMBAT, FeatureWindow.between(Milestones.BETA_1_8, Milestones.BETA_1_9_PRE_3),
        "Blocking reduced fall damage until it was removed in a Beta 1.9 prerelease, earlier than blocking itself. Beta 1.9 Prerelease 3 is the nearest declared version past the removal, so the window ends there (and therefore includes Prerelease 1 and 2)."),
    /** Enchanting placeholder. */
    ENCHANTING_PLACEHOLDER(Domain.ITEMS, FeatureWindow.between(Milestones.BETA_1_9_PRE_3, Milestones.BETA_1_9_PRE_4),
        "Prerelease 3 only: enchanted items show the literal \"Enchanted!!\" instead of their enchantments, and their name becomes blue. Prerelease 4: tooltips now show the enchantments."),
    /** No real enchanting. */
    NO_REAL_ENCHANTING(Domain.ITEMS, FeatureWindow.before(Milestones.BETA_1_9_PRE_4),
        "Real enchantments (17 of them, numeric levels, levels spent) arrive in Beta 1.9 Prerelease 4. Before it the table either does not exist (before Prerelease 3) or applies a placeholder (Prerelease 3). An experience bar being present is NOT evidence that enchanting works."),
    /** No enchanting. */
    NO_ENCHANTING(Domain.ITEMS, FeatureWindow.before(Milestones.BETA_1_9_PRE_3),
        "The enchanting table refuses to enchant at all: every build before Prerelease 3, where there is no table. Prerelease 3 enchants with placeholder data and is NOT in this window."),
    /** No sneaking. */
    NO_SNEAKING(Domain.PLAYER, FeatureWindow.before(Milestones.ALPHA_1_1_0),
        "Sneaking arrives in Alpha 1.1.0. Before it the shift key did nothing. The research is silent on this boundary; the project owner's correction is the best evidence available."),
    /** No brewing. */
    NO_BREWING(Domain.ITEMS, FeatureWindow.before(Milestones.BETA_1_9_PRE_3),
        "Brewing is functional from Beta 1.9 Prerelease 3. Prerelease 2 carries a cauldron-based prototype with no ordinary block id."),
    /** Inert end portal. */
    INERT_END_PORTAL(Domain.BLOCKS, FeatureWindow.before(Milestones.BETA_1_9_PRE_4),
        "The End portal does nothing when completed before Beta 1.9 Prerelease 4: Prerelease 3 adds the frame and eye of ender but the portal block has no function."),
    /** Hidden xp level. */
    HIDDEN_XP_LEVEL(Domain.PLAYER, FeatureWindow.before(Milestones.BETA_1_9_PRE_3),
        "The experience level number above the bar is first shown in Beta 1.9 Prerelease 3 (the version page for that build; the Experience article's history table says Prerelease 4, which is recorded as the source that misled)."),
    /** No nether. */
    NO_NETHER(Domain.WORLD, FeatureWindow.before(Milestones.ALPHA_1_2_0),
        "The Nether arrives with the Halloween Update, Alpha 1.2.0. Before it no portal may be lit and no travel may happen."),
    /** Unstackable food. */
    UNSTACKABLE_FOOD(Domain.ITEMS, FeatureWindow.before(Milestones.BETA_1_8),
        "Food did not stack until Beta 1.8, the same boundary as the hunger meter and the rest of the Adventure Update."),
    /** No offhand. */
    NO_OFFHAND(Domain.PLAYER, FeatureWindow.before(Milestones.RELEASE_1_9),
        "The offhand slot arrived in Release 1.9; the newest declared version is Release 1.6.4, so no declared version has an offhand."),
    /** F4 portal key. */
    F4_PORTAL_KEY(Domain.PLAYER, FeatureWindow.exactly("historical_experience:alpha_1_2_2a"),
        "Pressing F4 rebuilds a lit Nether portal in Alpha v1.2.2a ONLY. Both clients were hash-checked and decompiled: Alpha 1.2.0 has no F4 case, Alpha 1.2.2a calls the portal builder. An exact build, not a floor.");

    /** The system a feature belongs to, for documentation and for the generated version matrix. */
    public enum Domain {
        COMBAT, MOBS, PLAYER, WORLD, BLOCKS, ITEMS, RENDERING
    }

    private final Domain domain;
    private final List<FeatureWindow> windows;
    private final String evidence;

    Feature(Domain domain, FeatureWindow window, String evidence) {
        this.domain = domain;
        this.windows = List.of(window);
        this.evidence = evidence;
    }

    /** The system this feature belongs to. */
    public Domain domain() {
        return this.domain;
    }

    /** The windows in which this feature is active; it is active when any contains the version. */
    public List<FeatureWindow> windows() {
        return this.windows;
    }

    /** Why the windows are what they are, as recorded when the rule was researched. */
    public String evidence() {
        return this.evidence;
    }

    /** Whether a declared version with this id and key is inside any window. Used once per version, to build its set. */
    boolean appliesTo(String versionId, int key) {
        for (FeatureWindow window : this.windows) {
            if (window.contains(versionId, key)) {
                return true;
            }
        }
        return false;
    }
}
