"""Replace the whole breeding evidence entry, because patching its middle broke the concatenation.

The previous edit cut a `Map.entry(...)` string mid-concatenation and left the entry malformed, which the compiler
reported at line 92. Replacing the entry as a unit is both safer and easier to read than another surgical cut.
"""
import pathlib

path = (pathlib.Path(__file__).resolve().parents[1]
        / 'src/main/java/net/historicalexperience/gameplay/HistoricalGameplayTweaks.java')
lines = path.read_text(encoding='utf-8').splitlines(keepends=True)

# Locate the breeding entry by its first line and replace through its terminating ');'.
start = next(i for i, line in enumerate(lines) if line.strip() == 'Map.entry("breeding",')
end = next(i for i in range(start, len(lines)) if lines[i].rstrip().endswith(');'))

replacement = '''        Map.entry("breeding",
            "Animal breeding AND tempting arrive together in Beta 1.9 Prerelease 2, so Beta 1.8.1 had "
                + "neither. Baby animals are Beta 1.9 Prerelease 3. Upstream tweaks: DISABLE_ANIMAL_BREEDING, "
                + "DISABLE_ANIMAL_TEMPTING, DISABLE_BABY_ANIMAL_SPAWNING. This is a PORT: an earlier note here "
                + "called it a project rule after a worklist search found nothing, but the worklist is a "
                + "200-row subset of the research and the timeline itself names the tweak under the feature "
                + "\'animal breeding\'."),
'''

lines[start:end + 1] = [replacement]
path.write_text(''.join(lines), encoding='utf-8')
print('replaced the breeding entry; removed lines', start + 1, 'to', end + 1)
