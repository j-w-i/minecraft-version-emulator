"""Add the pre-Beta-1.8 content revisions: the dungeon feature and the desert well.

Observed defect this closes: a real Infdev 20100420 world contained minecraft:spawner and chests, so the
monster room was generating - but dungeons arrive in Infdev 20100625-1917, two months after that build.
The desert well (Release 1.2.1) is the same class of defect for four profiles.

Revisions are append-only, so this adds a NEW revision to each affected version rather than editing one, and
writes its settings preset before pointing the public menu at it.
"""
import json
import pathlib

ROOT = pathlib.Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/net/historicalexperience'
SETTINGS = ROOT / 'src/main/resources/data/historical_experience/moderner_beta/settings_preset'
GENERATOR = ROOT / 'scripts/generate-1211-world-presets.py'


def patch(path, old, new, label):
    text = path.read_text(encoding='utf-8')
    assert old in text, 'anchor not found for ' + label
    path.write_text(text.replace(old, new, 1), encoding='utf-8')
    print('patched', label)


# 1. The exclusion lists.
exclusions = JAVA / 'generation/HistoricalFeatureExclusions.java'
patch(exclusions, '''    /**
     * The same modern-content exclusions, for the versions from Beta 1.8.1 onwards.''', '''    /**
     * {@link #BEFORE_GRASS_EXISTED} plus the desert well, which is Release 1.2.1 (12w04a).
     *
     * <p>The well is a PLACED FEATURE on 1.21.1, not a structure, so the structure gate cannot express it and
     * it belongs here.
     */
    public static final List<Exclusion> BEFORE_GRASS_EXISTED_WITHOUT_DESERT_WELL = List.copyOf(
        java.util.stream.Stream.concat(
            BEFORE_GRASS_EXISTED.stream(),
            java.util.stream.Stream.of(desertWell("Alpha and both Infdev builds"))
        ).toList()
    );

    /**
     * The above, plus the monster room, for the two Infdev builds that predate it.
     *
     * <p>Found by observing a real world rather than by reading the research: an Infdev 20100420 sample
     * contained {@code minecraft:spawner} and chests, so a dungeon was generating in a build two months older
     * than Infdev 20100625-1917, which is when monster rooms arrive. The dungeon is a placed feature, so the
     * structure rules never had an opinion about it.
     */
    public static final List<Exclusion> INFDEV_WITHOUT_DUNGEONS = List.copyOf(
        java.util.stream.Stream.concat(
            BEFORE_GRASS_EXISTED_WITHOUT_DESERT_WELL.stream(),
            java.util.stream.Stream.of(
                new Exclusion("minecraft:monster_room",
                    "Monster rooms arrive in Infdev 20100625-1917, which is after both Infdev builds this "
                        + "project declares; observed generating in a real 20100420 world"),
                new Exclusion("minecraft:monster_room_deep",
                    "The deepslate monster room is modern content and existed in no declared version")
            )).toList()
    );

    /** Beta 1.7.3's modern-feature set plus the desert well, which is Release 1.2.1. */
    public static final List<Exclusion> BETA_1_7_3_WITHOUT_DESERT_WELL = List.copyOf(
        java.util.stream.Stream.concat(
            BETA_1_7_3_WITHOUT_ANY_MODERN_VANILLA_FEATURES.stream(),
            java.util.stream.Stream.of(desertWell("Beta 1.7.3"))
        ).toList()
    );

    /**
     * The same modern-content exclusions, for the versions from Beta 1.8.1 onwards.''', 'exclusion lists')

# 2. Beta 1.7.3 revision 10.
versions = JAVA / 'version/HistoricalVersions.java'
patch(versions, '''                    HistoricalFeatureExclusions.BETA_1_7_3_WITHOUT_ANY_MODERN_VANILLA_FEATURES,
                    BetaOreRules.BETA_1_7_3)
            )));''', '''                    HistoricalFeatureExclusions.BETA_1_7_3_WITHOUT_ANY_MODERN_VANILLA_FEATURES,
                    BetaOreRules.BETA_1_7_3),
                // Revision 10 adds the desert well. It is Release 1.2.1 (12w04a) and a placed feature, so
                // neither the structure gate nor any earlier revision excluded it.
                new GenerationRevision(10, alias("beta_1_7_3_content_preview_10"),
                    "moderner_beta:beta", true,
                    "modern content: the Release 1.2.1 desert well is excluded, which no earlier revision did",
                    HistoricalFeatureExclusions.BETA_1_7_3_WITHOUT_DESERT_WELL,
                    BetaOreRules.BETA_1_7_3)
            )));''', 'beta 1.7.3 revision 10')

# 3. Alpha and Infdev revision 2, with the dungeon boundary derived from the registry's own chronology.
patch(versions, '''                    HistoricalFeatureExclusions.BEFORE_GRASS_EXISTED,
                    List.of())));
    }''', '''                    HistoricalFeatureExclusions.BEFORE_GRASS_EXISTED,
                    List.of()),
                // Revision 2 adds the desert well, and the monster room only where the version predates it.
                // The boundary is derived rather than passed in: the monster room arrives in Infdev
                // 20100625-1917, which falls between the 20100611 build and Alpha, and both are on the
                // registry's own chronology scale. Deriving it here means the two Infdev builds and Alpha
                // cannot drift apart from the research, and no caller has to remember which list it needs.
                new GenerationRevision(
                    2,
                    alias(id.substring(id.indexOf(':') + 1) + "_no_modern_content"),
                    upstreamPreset,
                    true,
                    "modern content: the Release 1.2.1 desert well is excluded, and the monster room as well "
                        + "for the builds older than Infdev 20100625-1917",
                    chronology >= ERA_ALPHA
                        ? HistoricalFeatureExclusions.BEFORE_GRASS_EXISTED_WITHOUT_DESERT_WELL
                        : HistoricalFeatureExclusions.INFDEV_WITHOUT_DUNGEONS,
                    List.of())));
    }''', 'alpha and infdev revision 2')

# 4. The new revisions need settings presets, copied from the revision each one supersedes.
COPIES = {
    'beta_1_7_3_content_preview_10': 'beta_1_7_3_content_preview_9',
    'alpha_no_modern_content': 'alpha_no_grass',
    'infdev_420_no_modern_content': 'infdev_420_no_grass',
    'infdev_611_no_modern_content': 'infdev_611_no_grass',
}
for new, old in COPIES.items():
    source = SETTINGS / (old + '.json')
    assert source.is_file(), 'missing ' + str(source)
    (SETTINGS / (new + '.json')).write_text(source.read_text(encoding='utf-8'), encoding='utf-8')
    print('preset', new, '<-', old)

# 5. Point the public menu at them.
text = GENERATOR.read_text(encoding='utf-8')
text = text.replace("'beta_1_7_3': 'beta_1_7_3_content_preview_9'",
                    "'beta_1_7_3': 'beta_1_7_3_content_preview_10'")
text = text.replace("'alpha': 'alpha_no_grass'", "'alpha': 'alpha_no_modern_content'")
text = text.replace("'infdev_420': 'infdev_420_no_grass'", "'infdev_420': 'infdev_420_no_modern_content'")
text = text.replace("'infdev_611': 'infdev_611_no_grass'", "'infdev_611': 'infdev_611_no_modern_content'")
GENERATOR.write_text(text, encoding='utf-8')
print('public mapping updated')
