"""Record that the worklist is a subset, and what that cost."""

import pathlib

path = pathlib.Path(__file__).resolve().parents[1] / 'docs/nt-port-audit.md'
text = path.read_text(encoding='utf-8')

text += '''

## The worklist is a SUBSET, and that has been misleading me (round 59)

`research/nt-port-worklist.json` holds 200 entries. Its own `counts` field says 489 matching and 471 to port. So
roughly 60 percent of Nostalgic Tweaks is not in the file I have been treating as the to-port authority.

Evidence of the harm: `OLD_SWEEP_PARTICLES` appears NOWHERE in the worklist, yet it is a real tweak, default
**true**, and it was the missing piece behind the owner report "the sweep gui is there even tho the effect is
not". I searched the worklist for it, found nothing, and told the owner its absence meant it was out of scope -
the same mistake as INSTANT_EAT, from the same cause.

**Rule from here: whether a behaviour is a Nostalgic Tweaks feature is decided by its own config classes
(`GameplayTweak`, `CandyTweak`, `SoundTweak`), not by the worklist.** The worklist remains a useful index of
boundaries and historical claims; it is not the feature list.

Porting is still governed by evidence about the VERSION, not by upstream defaults. The crit particle is the
worked example: `OLD_NO_CRIT_PARTICLES` defaults to true upstream, but critical hits and their particle arrive
together in the Beta 1.8 Pre-release and six declared profiles had them, so that flag is a user preference - and
porting it as a version rule would delete part of the era rather than emulate it.

### Combat visuals, as of round 59

| Upstream flag | Default | Ported | Why |
| --- | --- | --- | --- |
| `OLD_SWEEP_PARTICLES` | true | yes | 1.9 particle, absent from every declared version |
| `OLD_NO_DAMAGE_PARTICLES` | true | yes | damage indicator is 1.9 |
| `OLD_NO_MAGIC_HIT_PARTICLES` | true | yes | enchanted-hit sparkle is 1.9 |
| `OLD_NO_CRIT_PARTICLES` | true | **no** | crit and its particle arrive in Beta 1.8; six profiles had them |
| `OLD_ATTACK` (sounds) | true | not yet | the 1.9 attack sound set mapped to the old one - audible, next |
'''

path.write_text(text, encoding='utf-8')
print('audit updated:', len(text.splitlines()), 'lines')
