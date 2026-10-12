"""Correct two of my own claims, using the flag-to-research join.

NT declares 504 flags. The research timeline names 95 tweak names, and every one of those 95 is a declared flag.
Joining them exposes two things I asserted wrongly:

1. DISABLE_ANIMAL_BREEDING IS research-supported. Two rounds ago I searched the worklist, found nothing, and
   relabelled the breeding gate as "a PROJECT RULE, not a port". The worklist is the 200-row subset; the timeline
   itself carries the tweak name under the feature "animal breeding". So it is a port, and my correction was the
   thing that was wrong.

2. ALWAYS_OPEN_CHEST IS research-supported - the timeline names it under "chests cannot be opened when a block is
   above". I told the owner it had no source and left it unwired on that basis. The same subset error.

Both fixes are textual, and both matter: a wrong provenance note is how the next reader (me) decides not to do
something that should be done, or to trust something that should not be.
"""
import pathlib

root = pathlib.Path(__file__).resolve().parents[1]

tweaks = root / 'src/main/java/net/historicalexperience/gameplay/HistoricalGameplayTweaks.java'
text = tweaks.read_text(encoding='utf-8')

old = ('"PROJECT RULE, not a port: Nostalgic Tweaks has no breeding toggle - a worklist search for breeding '
       'or "')
assert old in text, 'breeding provenance anchor not found'
start = text.index(old)
end = text.index(');', start)
replacement = ('"A PORT after all. Upstream tweak: DISABLE_ANIMAL_BREEDING, which the research timeline names '
               'under the "\n            + "feature \'animal breeding\'. I previously relabelled this as a '
               'project rule after searching the worklist and finding "\n            + "nothing - but the '
               'worklist is a 200-row subset of the research, and the timeline itself carries the name."')
text = text[:start] + replacement + text[end:]
tweaks.write_text(text, encoding='utf-8')
print('breeding provenance corrected back to a port')

audit = root / 'docs/nt-port-audit.md'
text = audit.read_text(encoding='utf-8')
text += '''

## The flag-to-research join, and two of my own errors it caught (round 66)

Nostalgic Tweaks declares **504** flags. The research timeline names **95** tweak names, and every one of those 95
is a declared flag. That join is the map the remaining work needed: 95 behaviours have research support and can be
ported with a boundary; the other 409 have no row and stay out until one exists.

It also exposed two claims of mine that were wrong, both from treating the 200-row worklist as the research:

| I said | Reality |
| --- | --- |
| "Nostalgic Tweaks has no breeding toggle - a worklist search returns nothing", and relabelled the breeding gate as a project rule | `DISABLE_ANIMAL_BREEDING` is a declared flag AND the timeline names it under the feature "animal breeding". It is a port. |
| "`alwaysOpenChestEnabled` has no source, so it stays unwired" | `ALWAYS_OPEN_CHEST` is named in the timeline under "chests cannot be opened when a block is above". It has a source. |

Both are the same mistake in opposite directions: the first made me distrust a port, the second made me refuse a
behaviour that has evidence. Corrected in the code and here.

Consequence for the plan: the animation group I was about to work through has NO research rows (sneak, swing,
death-topple and arm pose all return nothing), which is why OLD_SKELETON_ARMS was reverted. The 95 supported flags
are where the remaining porting should happen, and `ALWAYS_OPEN_CHEST` is now a candidate rather than an exclusion.
'''
audit.write_text(text, encoding='utf-8')
print('audit updated with the join and both corrections')
