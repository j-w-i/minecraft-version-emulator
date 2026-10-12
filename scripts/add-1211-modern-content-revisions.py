"""Create the settings presets for the new `_no_modern_content` revisions.

A generation revision is identified by the settings-preset ALIAS a world stores, so a new revision needs its
own settings-preset document even when the document's contents are identical to the previous revision's: the
alias is the recipe identity, and the exclusions that differ live in the version registry rather than in this
file. The alias is what the world preset points at, so this script also updates the public mapping and then
re-runs the world-preset generator.

Run from the repository root. Idempotent.
"""
import pathlib

ROOT = pathlib.Path(__file__).resolve().parents[1]
SETTINGS = ROOT / 'src/main/resources/data/historical_experience/moderner_beta/settings_preset'
GENERATOR = ROOT / 'scripts/generate-1211-world-presets.py'
SUFFIX = '_no_modern_content'

# Public profile -> the revision it now selects. Beta 1.8.1 gains its own revision too; the pre-Beta eras
# are not in this batch because their revisions carry the dungeon and desert-well changes instead.
UPGRADED = ['beta_1_8_1', 'beta_1_9_pre_3', 'release_1_0_0', 'release_1_1', 'release_1_2_5',
            'release_1_6_4']


def main():
    written = []
    for version in UPGRADED:
        source = SETTINGS / ('%s_cave_fix.json' % version)
        if not source.is_file():
            raise SystemExit('missing the previous revision for %s: %s' % (version, source))
        destination = SETTINGS / ('%s%s.json' % (version, SUFFIX))
        destination.write_text(source.read_text(encoding='utf-8'), encoding='utf-8')
        written.append(destination.name)

    text = GENERATOR.read_text(encoding='utf-8')
    for version in UPGRADED:
        # The public mapping is a dict comprehension over these names, so there is no per-version literal to
        # rewrite; check that the generator already selects the new suffix instead of editing it blindly.
        if ("name + '%s'" % SUFFIX) not in text:
            raise SystemExit('the world-preset generator does not select %s; update its PUBLIC_RECIPES '
                             'before running this script' % SUFFIX)
    print('world-preset generator already selects %s' % SUFFIX)

    print('wrote %d settings presets:' % len(written))
    for name in written:
        print('   ', name)
    print('now run: python scripts/generate-1211-world-presets.py')


if __name__ == '__main__':
    main()
