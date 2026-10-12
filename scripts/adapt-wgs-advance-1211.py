"""One-off: adapt WorldGenSettingsAdvance to 1.21.1's NBT accessors.

26.3's CompoundTag accessors return Optional (getCompound/getString), while 1.21.1 returns the value
directly and an empty tag or empty string when absent. Rather than scatter null checks, this adds two
private helpers named for what the caller actually wants - a child that may be absent, and a string that may
be absent - and points the eight call sites at them. Run once; it is idempotent.
"""
import pathlib
import re
import sys

path = pathlib.Path('src/main/java/net/historicalexperience/world/upgrade/WorldGenSettingsAdvance.java')
text = path.read_text(encoding='utf-8')
before = text

# 1. Call sites that wanted "this compound, or null".
text = re.sub(r'(\w+)\.getCompound\("([a-z_]+)"\)\.orElse\(null\)', r'childOrNull(\1, "\2")', text)

# 2. The string read that wanted "this string, or null".
text = text.replace('parent.getString(PRESET_KEY).orElse(null)', 'stringOrNull(parent, PRESET_KEY)')

# 3. biomeSource returned an Optional straight from the accessor.
text = text.replace('return generator.getCompound("biome_source");',
                    'return Optional.ofNullable(childOrNull(generator, "biome_source"));')

# 4. Any remaining bare Optional-returning accessor.
text = re.sub(r'return (\w+)\.getCompound\("([a-z_]+)"\);',
              r'return Optional.ofNullable(childOrNull(\1, "\2"));', text)

helpers = '''
    /**
     * A child compound, or null when it is absent.
     *
     * <p>1.21.1's {@code getCompound} hands back an empty tag for a missing key, where 26.3 handed back an
     * {@code Optional}. This project treats "absent" as a meaningful answer - {@code State.consistent()} is
     * false when a reference is missing - so the distinction is kept rather than flattened to an empty tag,
     * which would silently look like a settings block that exists and says nothing.
     */
    private static CompoundTag childOrNull(CompoundTag parent, String key) {
        if (parent == null) {
            return null;
        }
        CompoundTag child = parent.getCompound(key);
        return child.isEmpty() ? null : child;
    }

    /** A string, or null when the key is absent. The 1.21.1 counterpart of 26.3's Optional-returning read. */
    private static String stringOrNull(CompoundTag parent, String key) {
        if (parent == null || !parent.contains(key)) {
            return null;
        }
        return parent.getString(key);
    }
'''

if 'private static CompoundTag childOrNull' not in text:
    marker = '    private static void setPreset(CompoundTag parent, String value)'
    if marker not in text:
        print('ERROR: could not find the insertion point', file=sys.stderr)
        sys.exit(2)
    text = text.replace(marker, helpers.lstrip('\n') + '\n' + marker, 1)

if text != before:
    path.write_text(text, encoding='utf-8')
    print('rewrote', path)
else:
    print('no change needed (already adapted)')

for i, line in enumerate(text.splitlines(), start=1):
    if 'childOrNull(' in line or 'stringOrNull(' in line:
        print('  %d: %s' % (i, line.strip()))
