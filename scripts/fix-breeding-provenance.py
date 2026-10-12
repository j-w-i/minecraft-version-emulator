"""Build the tempting port, and stop my code from citing an upstream tweak that does not exist.

The breeding gate's evidence text names DISABLE_ANIMAL_BREEDING, DISABLE_ANIMAL_TEMPTING and
DISABLE_BABY_ANIMAL_SPAWNING as the upstream tweaks behind it. A search of the whole worklist for breeding or
persistence returns nothing, so DISABLE_ANIMAL_BREEDING is not a Nostalgic Tweaks toggle. The BEHAVIOUR is
still historically grounded - breeding arrives in Beta 1.9 Prerelease 2 per the feature timeline - but it is a
project rule, not a port, and the difference has to be written where the next reader will see it.
"""
import pathlib

path = (pathlib.Path(__file__).resolve().parents[1]
        / 'src/main/java/net/historicalexperience/gameplay/HistoricalGameplayTweaks.java')
text = path.read_text(encoding='utf-8')

old = '"DISABLE_ANIMAL_BREEDING, DISABLE_ANIMAL_TEMPTING, DISABLE_BABY_ANIMAL_SPAWNING."'
new = ('"PROJECT RULE, not a port: Nostalgic Tweaks has no breeding toggle - a worklist search for breeding '
       'or "\n            + "persistence returns nothing - but the timeline puts breeding in Beta 1.9 '
       'Prerelease 2, so this world refuses it. "\n            + "Tempting IS a port: DISABLE_ANIMAL_TEMPTING, '
       'from animal_spawn/TemptGoalMixin."')
assert old in text, 'evidence anchor not found'
path.write_text(text.replace(old, new, 1), encoding='utf-8')
print('breeding provenance corrected')
