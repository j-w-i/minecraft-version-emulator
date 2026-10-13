"""Generates the project's HistoricalBlockAvailability from the reviewed research artifacts.

Why generated rather than hand-written: the interesting fact for each modern block is "the earliest
declared version that had it", and for seven of the ten versions that fact is derivable from the
hash-verified per-version block-name sets in research/block-palettes. Transcribing 150-odd names by hand
is how a plausible-but-wrong table gets created, which is the failure mode this project keeps hitting.

Inputs (all read-only):
  * research/block-palettes/*.json          per-version block-name sets extracted from hash-verified jars
  * research/feature-timeline.json          sourced introduction boundaries for individual blocks
  * .../HistoricalBlockPalette.java  the reviewed modern-id -> legacy-name mapping

Output:
  * src/main/java/net/historicalexperience/generation/HistoricalBlockAvailability.java

Run: python scripts/generate-1211-block-availability.py
"""
import json
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[1]
PALETTE_SOURCE = ROOT / 'src/main/java/net/historicalexperience/generation/HistoricalBlockPalette.java'
OUTPUT = ROOT / 'src/main/java/net/historicalexperience/generation/HistoricalBlockAvailability.java'

# Oldest first. Asserted against the version registry by a unit test, so this order cannot silently drift.
CHRONOLOGY = [
    'historical_experience:infdev_420',
    'historical_experience:infdev_611',
    'historical_experience:alpha_1_0_1',
    'historical_experience:alpha_1_0_4',
    'historical_experience:alpha_1_0_6',
    'historical_experience:alpha_1_0_11',
    'historical_experience:alpha_1_0_14',
    'historical_experience:alpha_1_0_17',
    'historical_experience:alpha_1_1_0',
    'historical_experience:alpha',
    'historical_experience:alpha_1_2_0',
    'historical_experience:beta_1_0',
    'historical_experience:beta_1_2',
    'historical_experience:beta_1_3',
    'historical_experience:beta_1_4',
    'historical_experience:beta_1_5',
    'historical_experience:beta_1_6',
    'historical_experience:beta_1_7_3',
    'historical_experience:beta_1_8_1',
    'historical_experience:beta_1_9_pre_1',
    'historical_experience:beta_1_9_pre_2',
    'historical_experience:beta_1_9_pre_3',
    'historical_experience:beta_1_9_pre_4',
    'historical_experience:beta_1_9_pre_5',
    'historical_experience:beta_1_9_pre_6',
    'historical_experience:release_1_0_0',
    'historical_experience:release_1_1',
    'historical_experience:release_1_2_5',
    'historical_experience:release_1_3',
    'historical_experience:release_1_4',
    'historical_experience:release_1_5',
    'historical_experience:release_1_6_4',
]

# Which extracted palette belongs to which declared version.
#
# This drives DATING: a block first seen in one of these palettes is dated to the EARLIEST version mapped to it. So
# only versions whose own client was actually extracted may appear here. Adding Pre1 and Pre2 to this map was wrong
# and a test caught it: because they share Pre3's terrain they were mapped to b1_9_pre3, which then dated
# brewing_stand - a Pre2 block - to Pre1, and would have allowed Pre1 everything Pre2 and Pre3 added.
PALETTE_FILES = {
    'historical_experience:beta_1_7_3': 'b1_7_3',
    'historical_experience:beta_1_8_1': 'b1_8_1',
    'historical_experience:beta_1_9_pre_3': 'b1_9_pre3',
    'historical_experience:release_1_0_0': '1_0_0',
    'historical_experience:release_1_1': '1_1',
    'historical_experience:release_1_2_5': '1_2_5',
    'historical_experience:release_1_3': '1_2_5',
    'historical_experience:release_1_4': '1_2_5',
    'historical_experience:release_1_5': '1_2_5',
    'historical_experience:release_1_6_4': '1_6_4',
}

# Versions that share another version's extracted palette WITHOUT being dated by it.
#
# Pre1 and Pre2 generate from the single Beta 1.9 terrain preset Moderner Beta ships, so Pre3's hash-verified
# extraction is the only evidence of which block NAMES their client registered. They need that for the palette
# membership question - otherwise HistoricalBlockAvailability.judge answers UNKNOWN for every palette-derived date
# and all twenty of Pre1's village blocks come back unjudgeable - but they must NOT be dated by it, because their
# content is a subset: Pre2 adds brewing and Pre3 adds baby animals.
SHARED_PALETTE_VERSIONS = {
    'historical_experience:beta_1_9_pre_1': 'b1_9_pre3',
    'historical_experience:beta_1_9_pre_2': 'b1_9_pre3',
    'historical_experience:beta_1_9_pre_4': 'b1_9_pre3',
    'historical_experience:beta_1_9_pre_5': 'b1_9_pre3',
    'historical_experience:beta_1_9_pre_6': 'b1_9_pre3',
}

# Era spellings of the same block. A block whose registry name changed between two versions is present in
# both palettes under different names, so a single-name lookup would place it far too late and then delete
# it from a world that legitimately had it. Each alias must name the palette that justifies it.
ALIASES = {
    # Beta 1.9 Prerelease 3 calls the End Portal frame airPortalFrame; 1.0.0 renamed it endPortalFrame.
    'minecraft:end_portal_frame': [
        ('airPortalFrame', 'historical_experience:beta_1_9_pre_3',
         'the beta_1_9_pre_3 palette spells it airPortalFrame and 1.0.0 renamed it endPortalFrame'),
    ],
    # 1.6.4's registry was already using the flattened-ish name for infested stone.
    'minecraft:infested_stone': [
        ('monsterEgg', 'historical_experience:beta_1_8_1',
         'Beta 1.8 introduced silverfish and their monster egg blocks; the name is stated by the '
         'feature-timeline mobs row "mob: silverfish" (Beta 1.8 (Pre-release)) and the b1.8 palette '
         'simply does not recover the name, so the alias is recorded rather than inferred'),
    ],
    # Modern Minecraft splits the single legacy torch block into a floor and a wall variant. The extracted palette
    # has only "torch", because that is what the era registered - so wall_torch is NOT later content, it is the same
    # block under a name this project's registry uses. Recording it as an alias is what keeps it out of a red test
    # that says "beta_1_8_1 must not place a block its own client never registered": that failure appeared the
    # moment the mapping stopped dating every descendant, and it was right to appear.
    'minecraft:wall_torch': [
        ('torch', 'historical_experience:beta_1_7_3',
         'the b1_7_3 palette registers a single torch block; modern Minecraft splits it into torch and wall_torch, '
             'so the wall form is the same era content rather than a later addition'),
    ],
}

# Boundaries the timeline states directly, which take precedence over a palette-derived guess. Each entry
# is the earliest DECLARED version that had the block, plus the evidence, transcribed from
# research/feature-timeline.json (blocks and mobs rows) and research/version-boundaries.json.
TIMELINE = [
    (['minecraft:grass_block'], 'historical_experience:infdev_420',
     'feature-timeline blocks: "grass block (the dirt-with-grass top block)" introduced Pre-Classic'),
    (['minecraft:bedrock'], 'historical_experience:infdev_420',
     'feature-timeline blocks: bedrock introduced Classic 0.0.12a'),
    (['minecraft:coal_ore', 'minecraft:iron_ore', 'minecraft:gold_ore', 'minecraft:gravel'],
     'historical_experience:infdev_420',
     'feature-timeline blocks: these introduced Classic 0.0.14a'),
    (['minecraft:oak_leaves'], 'historical_experience:infdev_420',
     'feature-timeline blocks: leaves introduced Classic 0.0.14a'),
    (['minecraft:oak_sapling'], 'historical_experience:infdev_420',
     'feature-timeline blocks: sapling introduced Pre-Classic rd-161348'),
    (['minecraft:sponge', 'minecraft:glass'], 'historical_experience:infdev_420',
     'feature-timeline blocks: sponge and glass introduced Classic 0.0.19a'),
    (['minecraft:mossy_cobblestone', 'minecraft:bookshelf'], 'historical_experience:infdev_420',
     'feature-timeline blocks: introduced Classic 0.26 SURVIVAL TEST'),
    (['minecraft:stone_bricks' if False else 'minecraft:tnt'], 'historical_experience:infdev_420',
     'TNT is Classic 0.26 SURVIVAL TEST (feature-timeline mechanics: "punching TNT with an empty hand")'),
    (['minecraft:obsidian'], 'historical_experience:infdev_420',
     'feature-timeline blocks: obsidian exists from Classic 0.28, natural formation from Indev'),
    (['minecraft:farmland', 'minecraft:wheat'], 'historical_experience:infdev_420',
     'feature-timeline blocks: wheat crop / farmland introduced Indev 20100206-2034'),
    (['minecraft:diamond_ore'], 'historical_experience:infdev_420',
     'feature-timeline blocks: diamond ore restored Infdev 20100320, before the earliest profile'),
    # 20100420 and 20100611 are both before Infdev 20100625-1917, so neither may have monster spawners. The date is
    # the EARLIEST declared version after that build, not "alpha": alpha used to be the only version on the far side
    # of the boundary, so naming it worked as a proxy. Declaring Alpha 1.0.1 between them turned the proxy into a
    # claim that spawners did not exist there - and the block veto acted on it, emptying Alpha 1.0.1 dungeons.
    (['minecraft:spawner'], 'historical_experience:alpha_1_0_1',
     'feature-timeline blocks: monster spawner introduced Infdev 20100625-1917, after both Infdev profiles'),
    (['minecraft:snow', 'minecraft:ice'], 'historical_experience:alpha_1_0_4',
     'feature-timeline blocks: snow and ice introduced Alpha v1.0.4 (Winter Update)'),
    # Redstone ore was dated to "alpha" - which is Alpha v1.1.2_01 - while its own evidence says Alpha v1.0.1. That
    # was harmless while it was documentation and became a live defect when the block veto started enforcing dates:
    # Alpha 1.0.1 worlds were refusing the ore that build introduced. Same shape as the spawner entry below.
    (['minecraft:redstone_ore'], 'historical_experience:alpha_1_0_1',
     'feature-timeline blocks: redstone ore introduced Alpha v1.0.1 (Seecret Friday Update 3)'),
    # STILL DATED TOO LATE, on purpose, and to be re-pointed the moment the build is declared. Their evidence names
    # an Alpha build that is a target but not yet declared, so "alpha" stays the earliest declared version that had
    # them - conservative, and wrong only for builds that do not exist yet:
    #   cactus      -> Alpha v1.0.6
    #   clay        -> Alpha v1.0.11
    #   sugar_cane  -> Alpha v1.0.11
    # Declaring any of those builds requires moving its entry here, or the veto will refuse a block that build
    # introduced. scripts/audit-proxy-dates.py finds every entry in this state by comparing each date to its own
    # evidence string.
    (['minecraft:cactus'], 'historical_experience:alpha_1_0_6',
     'feature-timeline blocks: cactus introduced Alpha v1.0.6'),
    (['minecraft:clay', 'minecraft:sugar_cane'], 'historical_experience:alpha_1_0_11',
     'feature-timeline blocks: clay block and sugar cane introduced Alpha v1.0.11'),
    # Lily pads are STATED by the research as Prerelease 1 content - "Beta 1.9 Prerelease 1 | lily pads (visually
    # unnamed/grayscale and non-solid)" - while the palette-derived date put them at Pre3, because that is the only
    # Beta 1.9 palette extracted. The timeline statement wins: without this, Pre1 and Pre2 would REFUSE a block
    # their own build introduced, which is the same false-veto shape as redstone ore in Alpha 1.0.1.
    (['minecraft:lily_pad'], 'historical_experience:beta_1_9_pre_1',
     'feature-timeline blocks: lily pads introduced Beta 1.9 Prerelease 1 (the palette-derived date was pre3, '
     'which is the only Beta 1.9 palette extracted and therefore the latest such date, not the earliest)'),
    # The brewing stand is Pre3, matching both the palette and the research, so it is stated rather than left to the
    # palette alone: "Beta 1.9 Prerelease 3 | brewing stand".
    (['minecraft:brewing_stand'], 'historical_experience:beta_1_9_pre_3',
     'feature-timeline blocks: brewing stand introduced Beta 1.9 Prerelease 3 (brewing listed at 30 seconds; a '
     'reduced set of 25 potions and 31 combinations)'),
    # The Nether, and the pumpkin that shipped with it, are Alpha v1.2.0 - later than the Alpha profile.
    (['minecraft:netherrack', 'minecraft:glowstone', 'minecraft:nether_portal', 'minecraft:pumpkin',
      'minecraft:jack_o_lantern'], 'historical_experience:alpha_1_2_0',
     'feature-timeline blocks: netherrack, glowstone and pumpkin introduced Alpha v1.2.0, after the '
     'Alpha v1.1.2_01 profile'),
    # Grass and ferns, and cobweb, were BOTH held at beta_1_7_3 as proxies while Beta 1.5 and Beta 1.6 did not
    # exist: the notes said "after every profile older than Beta 1.7.3", which is what a proxy date looks like when
    # there is no build to name. Declaring those two builds makes both dates TRUE cases, and leaving them would have
    # made a Beta 1.6 world refuse the very vegetation that build introduced - the false-veto shape this file has
    # already produced twice, for redstone ore in Alpha 1.0.1 and for lily pads in Pre1/Pre2.
    (['minecraft:short_grass', 'minecraft:fern', 'minecraft:dead_bush'], 'historical_experience:beta_1_6',
     'feature-timeline blocks: grass (the plant) introduced Beta 1.6 Test Build 3, and version-transitions '
     'beta_1_6 lists "short grass and fern (grass patches in most grassy-soil biomes; fern naturally only in '
     'swamps/rainforests)" and "dead bush (deserts only, placeable only on sand)" - all three are this build\'s'),
    (['minecraft:cobweb'], 'historical_experience:beta_1_5',
     'feature-timeline blocks: cobweb introduced Beta 1.5; version-transitions beta_1_5 lists it with the '
     'explicit caveat that it is "unobtainable without inventory editing", which is noted rather than reproduced '
     'because this project does not gate item obtainability'),
    # Sandstone and lapis were BOTH held at beta_1_7_3 while their own evidence said "introduced Beta 1.2" - proxy
    # dates, correct for the declared set until Beta 1.2 existed. Both appeared in the audit output many rounds ago
    # and I only ever read its Alpha section, which is its own lesson: an audit is only as useful as the part of it
    # that gets read.
    (['minecraft:sandstone'], 'historical_experience:beta_1_2',
     'feature-timeline blocks: sandstone introduced Beta 1.2, so Alpha and Infdev have none'),
    # Beta 1.3's own content, which was NOT dated anywhere before this: beds and redstone repeaters arrive here,
    # along with the cobblestone, wood and sandstone slabs and the smooth stone slab that is present but
    # unobtainable. Without these entries a Beta 1.2 world accepts a bed, because an undated block is never vetoed -
    # no date means no claim - and a bed is one of the most recognisable things a player would notice.
    (['minecraft:repeater',
      'minecraft:cobblestone_slab', 'minecraft:oak_slab', 'minecraft:sandstone_slab',
      'minecraft:smooth_stone_slab',
      'minecraft:white_bed', 'minecraft:orange_bed', 'minecraft:magenta_bed', 'minecraft:light_blue_bed',
      'minecraft:yellow_bed', 'minecraft:lime_bed', 'minecraft:pink_bed', 'minecraft:gray_bed',
      'minecraft:light_gray_bed', 'minecraft:cyan_bed', 'minecraft:purple_bed', 'minecraft:blue_bed',
      'minecraft:brown_bed', 'minecraft:green_bed', 'minecraft:red_bed', 'minecraft:black_bed'],
     'historical_experience:beta_1_3',
     'feature-timeline blocks: beds, redstone repeaters and the cobblestone, wood and sandstone slabs all '
     'introduced Beta 1.3; the smooth stone slab is present in that build but explicitly unobtainable'),
    (['minecraft:lapis_ore', 'minecraft:lapis_block'], 'historical_experience:beta_1_2',
     'feature-timeline blocks: lapis lazuli ore introduced Beta 1.2 (Beta 1.2_02 later adjusted its frequency and '
     'height and permitted bedrock-level generation, which must NOT be backdated into 1.2)'),
    (['minecraft:vine', 'minecraft:melon', 'minecraft:stone_bricks'],
     'historical_experience:beta_1_8_1',
     'feature-timeline blocks: vines, melon and stone bricks introduced Beta 1.8 (Pre-release). The '
     'observed defect this fixes: a 289-chunk Alpha world contained 10 vine blocks'),
    (['minecraft:infested_stone'], 'historical_experience:beta_1_8_1',
     'feature-timeline mobs: silverfish introduced Beta 1.8 (Pre-release), which is when monster egg '
     'blocks arrived; the palette alone would date the block to 1.6.4 because it spells it monsterStoneEgg'),
    (['minecraft:brown_mushroom_block', 'minecraft:red_mushroom_block'], 'historical_experience:beta_1_8_1',
     'giant mushrooms and their cap blocks arrive with the Beta 1.8 Adventure Update; recorded because no '
     'extracted palette recovers a mushroom-cap name, so a palette-only rule would say UNKNOWN forever'),
    (['minecraft:nether_wart'], 'historical_experience:release_1_0_0',
     'feature-timeline blocks: nether wart introduced Beta 1.9 Prerelease (Release 1.0.0)'),
    (['minecraft:end_stone'], 'historical_experience:release_1_0_0',
     'feature-timeline blocks: end stone introduced Beta 1.9 Prerelease (Release 1.0.0)'),
    (['minecraft:emerald_ore', 'minecraft:emerald_block'], 'historical_experience:release_1_6_4',
     'feature-timeline blocks: emerald ore introduced Release 1.3.1 (12w21a), so it is in Release 1.6.4 '
     'and in none of the earlier profiles'),
    (['minecraft:ender_chest', 'minecraft:anvil', 'minecraft:beacon', 'minecraft:hopper',
      'minecraft:dropper', 'minecraft:comparator', 'minecraft:daylight_detector', 'minecraft:quartz_block',
      'minecraft:hay_block', 'minecraft:activator_rail', 'minecraft:command_block', 'minecraft:cocoa',
      'minecraft:tripwire', 'minecraft:cobblestone_wall', 'minecraft:flower_pot', 'minecraft:carrots',
      'minecraft:potatoes', 'minecraft:coal_block', 'minecraft:wooden_slab' if False else 'minecraft:oak_slab'],
     'historical_experience:release_1_6_4',
     'the 1.6.4 palette is the first of the extracted sets to register these, matching Release 1.6.1/1.6.2'),
]


def parse_mapping():
    """The reviewed modern-id -> legacy-name table, read out of the Java source so the two cannot drift.

    ONLY THE FIRST TARGET of each map(...) call is returned, because that is the block the legacy name BECAME.

    The later targets are modern DESCENDANTS grouped under the same legacy name so that a legacy world's blocks can
    still be resolved by name - ice, packed ice, blue ice and frosted ice all descend from the one legacy "ice";
    dirt, coarse dirt and rooted dirt from "dirt"; fire and soul fire from "fire"; soul sand and soul soil from
    "hellsand". Reading every target as proof of existence at Beta 1.7.3 minted dates none of them had, which is
    how soul_soil, soul_fire, coarse_dirt and blue_ice came to be permitted in fifteen versions - the leak the
    owner reported seeing in game, and the reason PreReleaseNetherGatingTest exists.

    The duplicate check still runs across EVERY target, so a modern id appearing under two legacy names is still
    caught even though only the first is dated.
    """
    source = PALETTE_SOURCE.read_text(encoding='utf-8')
    mapping = {}
    for match in re.finditer(r'^\s*map\("([^"]+)",\s*(.*?)\);\s*$', source, re.M | re.S):
        legacy = match.group(1)
        for index, modern in enumerate(re.findall(r'"([^"]+)"', match.group(2))):
            modern_id = modern if modern.startswith('minecraft:') else 'minecraft:' + modern
            if modern_id in mapping:
                raise SystemExit('duplicate modern id %s in the mapping table' % modern_id)
            if index == 0:
                mapping[modern_id] = legacy
    return mapping


def read_palettes():
    palettes = {}
    for version, name in PALETTE_FILES.items():
        document = json.loads((ROOT / ('research/block-palettes/%s.json' % name)).read_text(encoding='utf-8'))
        names = set(document.get('blockNames') or [])
        # 1.6.4 records a second setter family's names; they are the same blocks under later spellings.
        names |= set(document.get('aliasNames') or [])
        if not names:
            raise SystemExit('%s has no block names; the palette cannot be used for version checks' % name)
        palettes[version] = names
    return palettes


def main():
    mapping = parse_mapping()
    palettes = read_palettes()

    boundaries = {}   # modern id -> (version, evidence, trust)
    for modern in sorted(set(mapping) | set(ALIASES)):
        legacy = mapping.get(modern)
        aliases = ALIASES.get(modern, [])
        # A block that appears ONLY in ALIASES has no mapping entry to start from, and the loop has to reach it
        # anyway or its alias is dead code. That is exactly what happened to wall_torch: the mapping no longer
        # claims it after the first-target fix, so this loop never saw it, the alias was never consulted, and the
        # block stayed undated - which the palette then rejected with "beta_1_8_1 must not place a block its own
        # client never registered". The failure was correct and the loop was wrong.
        candidates = ([(legacy, None, 'the reviewed mapping name')] if legacy else []) + list(aliases)
        for version in CHRONOLOGY:
            palette = palettes.get(version)
            if palette is None:
                continue
            for name, alias_version, why in candidates:
                if name not in palette:
                    continue
                if alias_version is not None and alias_version != version:
                    continue
                boundaries[modern] = (version, 'first appears in the %s extracted palette as "%s" (%s)'
                                      % (version.split(':')[1], name, why), 'palette')
                break
            if modern in boundaries:
                break
        else:
            continue

    conflicts = []
    for modern_ids, version, evidence in TIMELINE:
        for modern in modern_ids:
            existing = boundaries.get(modern)
            if existing is not None and existing[0] != version:
                # Take the EARLIER boundary: it suppresses less, and deleting a block a version did have
                # is worse than leaving one it did not.
                earlier = min(existing[0], version, key=CHRONOLOGY.index)
                conflicts.append((modern, existing[0], version, earlier))
                boundaries[modern] = (earlier,
                    evidence
                    + '; the palette-derived boundary was ' + existing[0].split(':')[1]
                    + ' and the earlier of the two is used so a block a version did have is not deleted',
                    'timeline')
            else:
                boundaries[modern] = (version, 'feature-timeline: ' + evidence, 'timeline')

    unknown = sorted(modern for modern in mapping if modern not in boundaries)
    # A date that rests on a single palette is the weakest kind: it dates one NAME, not the block, so a
    # rename or a metadata variant is indistinguishable from a genuine late arrival. Reported so a reviewer
    # sees them rather than trusting them silently.
    single = {}
    for modern, (version, evidence, trust) in boundaries.items():
        single.setdefault(version, []).append(modern)
    thin = sorted((version, sorted(moderns)) for version, moderns in single.items()
                  if len(moderns) <= 6 and version != 'historical_experience:release_1_6_4')

    lines = []
    add = lines.append
    add('package net.historicalexperience.generation;')
    add('')
    add('import java.util.LinkedHashMap;')
    add('import java.util.List;')
    add('import java.util.Map;')
    add('import java.util.Set;')
    add('')
    add('/**')
    add(' * The earliest declared version that could contain a given modern block, and the evidence for it.')
    add(' *')
    add(' * <h2>Why an introduction table rather than a per-version palette</h2>')
    add(' *')
    add(' * <p>Asking "could this version have had this block?" against a single version\'s own block-name set')
    add(' * only works for the versions whose palette has been extracted, and even then absence is weak')
    add(' * evidence: Beta 1.7.3 and Release 1.6.4 spell the single-height plants differently from Release')
    add(' * 1.0, and a renamed block would look absent from the later set. Alpha v1.1.2_01 and both Infdev')
    add(' * builds have no recoverable block names at all (their registries never attach one), which is why')
    add(' * the earlier implementation could not suppress the ten vines a real Alpha world contained.')
    add(' *')
    add(' * <p>So the question is turned around: each block is dated once, as the earliest declared version')
    add(' * that had it, and every version is judged against that date. The consequences are:')
    add(' *')
    add(' * <ul>')
    add(' *   <li>a block present in the earliest profile is allowed everywhere, which is what makes the answer')
    add(' *       monotonic and immune to renames;')
    add(' *   <li>a block with no recorded date answers {@link Verdict#UNKNOWN} and is counted, never treated')
    add(' *       as acceptable - the same rule the rest of this project uses for unverified content;')
    add(' *   <li>the table is generated from the reviewed artifacts by')
    add(' *       {@code scripts/generate-1211-block-availability.py}, so it cannot drift from them, and a unit')
    add(' *       test re-derives it and fails on any difference.')
    add(' * </ul>')
    add(' *')
    add(' * <p>Evidence classes: {@code timeline} entries are sourced introduction boundaries transcribed from')
    add(' * research/feature-timeline.json; {@code palette} entries are the first extracted block-name set that')
    add(' * registers the block. A palette entry is weaker - it dates a NAME, not a block - which is exactly')
    add(' * why {@link #ALIASES} exists and why the conflict list below is reported rather than hidden.')
    add(' */')
    add('public final class HistoricalBlockAvailability {')
    add('    /** What a check can conclude about one block in one version. */')
    add('    public enum Verdict {')
    add('        /** The version is at or after the block\'s recorded introduction. */')
    add('        ALLOWED,')
    add('        /** The block is dated, and the version predates that date. */')
    add('        ANACHRONISTIC,')
    add('        /** No date is recorded, so nothing is claimed. Counted, never treated as acceptable. */')
    add('        UNKNOWN')
    add('    }')
    add('')
    add('    /**')
    add('     * A recorded introduction.')
    add('     *')
    add('     * @param versionId the earliest declared version that could contain the block')
    add('     * @param evidence  why that version, in words a reviewer can check')
    add('     * @param timeline  true when sourced from the feature timeline, false when dated from a palette')
    add('     */')
    add('    public record Boundary(String versionId, String evidence, boolean timeline) {')
    add('    }')
    add('')
    add('    /** The declared versions oldest first; a unit test asserts this equals the version registry. */')
    add('    public static final List<String> CHRONOLOGY = List.of(')
    for index, version in enumerate(CHRONOLOGY):
        add('        "%s"%s' % (version, ',' if index < len(CHRONOLOGY) - 1 else ''))
    add('    );')
    add('')
    add('    private static final Map<String, Boundary> INTRODUCED_IN = new LinkedHashMap<>();')
    add('')
    add('    /**')
    add('     * The versions whose own client registered a block-name set this project has extracted.')
    add('     *')
    add('     * <p>This is what stops the table from over-claiming for the three pre-Beta profiles. Alpha v1.1.2_01')
    add('     * and both Infdev builds never attach a name to a block at registration - their registries load nine')
    add('     * material names and no block names at all - so they have no palette and cannot show that a block was')
    add('     * ABSENT. A palette-derived date would then read "stone first appears in the Beta 1.7.3 set" as')
    add('     * "Alpha had no stone", which is false and would delete it. Only a sourced timeline boundary may')
    add('     * condemn a block in a version that has no palette of its own.')
    add('     */')
    add('    private static final Set<String> VERSIONS_WITH_EXTRACTED_PALETTE = Set.of(')
    # Dating versions plus the ones that only SHARE a palette for the membership question. Pre1 and Pre2 are here
    # so their blocks are judgeable at all; they are absent from PALETTE_FILES so nothing is dated by them.
    palette_versions = list(PALETTE_FILES) + list(SHARED_PALETTE_VERSIONS)
    for index, version in enumerate(palette_versions):
        add('        "%s"%s' % (version, ',' if index < len(palette_versions) - 1 else ''))
    add('    );')
    add('')
    add('    private static void introduce(String versionId, boolean timeline, String evidence,')
    add('            String... modernBlockIds) {')
    add('        if (!CHRONOLOGY.contains(versionId)) {')
    add('            throw new IllegalStateException("unknown version " + versionId);')
    add('        }')
    add('        for (String modernBlockId : modernBlockIds) {')
    add('            Boundary previous = INTRODUCED_IN.put(modernBlockId,')
    add('                new Boundary(versionId, evidence, timeline));')
    add('            if (previous != null) {')
    add('                throw new IllegalStateException(modernBlockId + " is dated twice");')
    add('            }')
    add('        }')
    add('    }')
    add('')
    static_lines = ['    static {']
    current = None
    buffer = []
    def java_string(value):
        """Escape for a Java string literal. Evidence text quotes palette spellings, so this matters."""
        return (value.replace('\\', '\\\\').replace('"', '\\"')
                .replace('\n', ' ').replace('\r', ' '))

    def flush():
        if current is None:
            return
        version, evidence, trust = current
        static_lines.append('        // %s' % evidence.replace('*/', '* /'))
        static_lines.append('        introduce("%s", %s,' % (version, 'true' if trust == 'timeline' else 'false'))
        # The evidence argument is REQUIRED and must come first. Omitting it does not fail to compile: the
        # first block id is silently consumed as the evidence string and that block is never dated, which is
        # exactly the defect the generated-table test in ProfileGenerationRules1211Test caught.
        static_lines.append('            "%s",' % java_string(evidence))
        # An explicit array rather than varargs, so a reader (and the test that re-parses this file) can tell
        # the evidence string apart from the block ids without relying on the evidence never containing a
        # quote - which it does, because it names the palette spelling it came from.
        static_lines.append('            new String[] {')
        for index, modern in enumerate(buffer):
            static_lines.append('                "%s"%s' % (modern, '});' if index == len(buffer) - 1 else ','))
    for modern in sorted(boundaries, key=lambda m: (CHRONOLOGY.index(boundaries[m][0]),
                                                    'timeline' != boundaries[m][2], m)):
        key = (boundaries[modern][0], boundaries[modern][1], boundaries[modern][2])
        if key != current:
            flush()
            current, buffer = key, []
        buffer.append(modern)
    flush()
    static_lines.append('    }')

    head = lines
    tail = [
        '',
        '    private HistoricalBlockAvailability() {',
        '    }',
        '',
        '    /** The recorded introduction of a modern block, or null when none is recorded. */',
        '    public static Boundary boundaryFor(String modernBlockId) {',
        '        if (modernBlockId == null) {',
        '            return null;',
        '        }',
        '        // Property suffixes are stripped so "minecraft:oak_log[axis=y]" is still recognised.',
        '        int bracket = modernBlockId.indexOf(\'[\');',
        '        String bare = bracket >= 0 ? modernBlockId.substring(0, bracket) : modernBlockId;',
        '        return INTRODUCED_IN.get(bare);',
        '    }',
        '',
        '    /**',
        '     * Judges one block against one declared version.',
        '     *',
        '     * <p>An unrecognised version id answers UNKNOWN rather than guessing: a version this build does',
        '     * not declare is a version whose block set this build has no basis to judge.',
        '     */',
        '    public static Verdict judge(String versionId, String modernBlockId) {',
        '        Boundary boundary = boundaryFor(modernBlockId);',
        '        if (boundary == null || versionId == null) {',
        '            return Verdict.UNKNOWN;',
        '        }',
        '        int version = CHRONOLOGY.indexOf(versionId);',
        '        int introduced = CHRONOLOGY.indexOf(boundary.versionId());',
        '        if (version < 0 || introduced < 0) {',
        '            return Verdict.UNKNOWN;',
        '        }',
        '        if (!boundary.timeline() && !VERSIONS_WITH_EXTRACTED_PALETTE.contains(versionId)) {',
        '            // A palette-derived date, judged against a version that has no palette of its own. The',
        '            // date describes when the NAME appears, and absence from a palette that does not exist',
        '            // proves nothing, so this answers UNKNOWN rather than deleting a block the version had.',
        '            return Verdict.UNKNOWN;',
        '        }',
        '        return version < introduced ? Verdict.ANACHRONISTIC : Verdict.ALLOWED;',
        '    }',
        '',
        '    /** Every modern block this table dates, for reporting and tests. */',
        '    public static Map<String, Boundary> all() {',
        '        return java.util.Collections.unmodifiableMap(INTRODUCED_IN);',
        '    }',
        '',
        '    public static int size() {',
        '        return INTRODUCED_IN.size();',
        '    }',
        '}',
        '',
    ]
    OUTPUT.write_text('\n'.join(head + static_lines + tail), encoding='utf-8')

    print('wrote %s' % OUTPUT.relative_to(ROOT))
    print('dated blocks        : %d of %d mapped' % (len(boundaries), len(mapping)))
    print('  from the timeline : %d' % sum(1 for b in boundaries.values() if b[2] == 'timeline'))
    print('  from a palette    : %d' % sum(1 for b in boundaries.values() if b[2] == 'palette'))
    print('undated (UNKNOWN)   : %d' % len(unknown))
    for modern in unknown:
        print('    %s (legacy name "%s")' % (modern, mapping[modern]))
    if conflicts:
        print()
        print('palette/timeline conflicts resolved to the earlier boundary:')
        for modern, palette_version, timeline_version, chosen in conflicts:
            print('    %-34s palette=%-28s timeline=%-28s chosen=%s'
                  % (modern, palette_version.split(':')[1], timeline_version.split(':')[1],
                     chosen.split(':')[1]))
    if thin:
        print()
        print('dates resting on few palette names (review these):')
        for version, moderns in thin:
            print('    %-30s %s' % (version.split(':')[1], moderns))


if __name__ == '__main__':
    main()
