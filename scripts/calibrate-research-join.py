"""Calibrate the flag-to-research join: a name is a lead, not evidence.

Round 66 concluded that ALWAYS_OPEN_CHEST "has a source" because the research names it. Reading the row itself
shows confidence LOW with a note saying the sourced quote runs the OTHER way: the quote is "Chests do not prevent
other chests underneath from opening" (Beta 1.9 Prerelease 6), which is about a chest above a chest, not about a
solid block. So the behaviour is named and unsupported, and it stays out.

Measured across the corpus: 62 rows name a Nostalgic Tweaks tweak, with confidence high 44, medium 6, low 12. The
join answers "is there a row?" and nothing more.

The breeding correction from round 66 survives its own check: that row is confidence HIGH and its notes state the
development version explicitly.
"""
import pathlib

path = pathlib.Path(__file__).resolve().parents[1] / 'docs/nt-port-audit.md'
text = path.read_text(encoding='utf-8')

text += '''

## Calibrating the join: a name is a lead, not evidence (round 67)

Round 66 used the flag-to-research join to conclude that `ALWAYS_OPEN_CHEST` "has a source". That was too fast.
Reading the row itself:

- `confidence: low`
- the notes say the sourced quote runs the **opposite** direction: the quote is "Chests do not prevent other
  chests underneath from opening" (Beta 1.9 Prerelease 6), which is about a chest above a *chest*, not about a
  solid block above a chest.

So the behaviour is **named and unsupported**, and it stays excluded. The join answers "is there a row?" - it does
not answer "is the claim good?".

**Measured across the corpus:** 62 rows name a Nostalgic Tweaks tweak, distributed as **high 44, medium 6, low
12**. Any port drawn from a named row has to read that row's `confidence` and `notes` before it counts as
evidence. Three of the twelve low rows are worth knowing about because they touch work already done:

| Flag | Confidence | Note |
| --- | --- | --- |
| `ALWAYS_OPEN_CHEST` | low | source runs the other way; not ported |
| `DISABLE_ORB_SPAWN` | low | the orb port rests on stronger primary evidence (the b1.8.1 client registry has orb entities, b1.7.3's does not), not on this row |
| `OLD_STYLE_ZOMBIE_VILLAGER_DROPS` | low | one of the fourteen loot mappings; unreachable in these versions anyway |

The breeding correction from round 66 **survives** its own check: that row is `confidence: high` and its notes name
the development version explicitly ("Beta 1.9 Prerelease 2 ... Beta 1.8 and Beta 1.8.1 therefore have NO breeding").
'''

path.write_text(text, encoding='utf-8')
print('audit calibrated:', len(text.splitlines()), 'lines')
