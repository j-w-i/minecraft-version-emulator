"""One-off: date lily pads, the last block the runtime checks could not judge.

A Release 1.2.5 world and a Release 1.6.4 world each reported five UNKNOWN lily pads. They are legitimate
there, but "legitimate and unjudged" is a coverage gap: nothing could refuse them in the versions that
predate them either. The b1.9-pre3 extraction registers the era name waterlily, which dates them properly.
"""
import pathlib

path = (pathlib.Path(__file__).resolve().parents[1]
        / 'src/main/java/net/historicalexperience/generation/HistoricalBlockPalette.java')
text = path.read_text(encoding='utf-8')
anchor = '        map("brewingStand", "brewing_stand");'
addition = '''        // Lily pads. Found as the last UNJUDGED block by the per-profile runtime checks: Release 1.2.5 and
        // Release 1.6.4 worlds each reported five lily pads that no table could rule on. The b1.9-pre3
        // extraction registers the era name waterlily, which dates them to the Beta 1.9 Pre-release - where
        // the timeline puts them - so they are now allowed from there and refusable before it, instead of
        // merely unclassified.
        map("waterlily", "lily_pad");
'''
assert anchor in text, 'anchor not found'
assert 'waterlily' not in text, 'already applied'
path.write_text(text.replace(anchor, addition + anchor, 1), encoding='utf-8')
print('lily pad mapping added')
