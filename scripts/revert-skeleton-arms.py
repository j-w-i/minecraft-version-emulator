"""Revert the skeleton arms port: the evidence does not support it, and the behaviour is probably backwards.

I asserted last round that the Indev 20100202 removal of "arm swinging attack animations" covered skeletons as
well as zombies. Checking the timeline for the skeleton bow pose returns NOTHING - only strafing and drops. So the
port rested on an assumption I had written as a shared fact.

Worse, the direction is wrong. Upstream's skeleton branch replaces the bow-drawing pose with MobArmHelper's STATIC
arms plus an idle bob, which is the arms-out idle stance. Beta skeletons visibly raised their arms to draw a bow
when shooting - that is the classic skeleton firing animation - so applying this removes an animation the versions
had, rather than restoring one they lacked.

A Nostalgic Tweaks flag models whatever its author wanted back; it is not a statement about these ten profiles.
The zombie half stays, because the timeline quote is literally about zombies.

Reinstating this needs one thing: a research row dating when the skeleton bow pose changed. Until then it is an
unsupported behaviour change and it comes out.
"""
import json
import pathlib

root = pathlib.Path(__file__).resolve().parents[1]

mixin = root / 'src/main/java/net/historicalexperience/mixin/SkeletonModelHistoricalArmsMixin.java'
if mixin.is_file():
    mixin.unlink()
    print('removed the skeleton arms mixin')

config = root / 'src/main/resources/historical_experience.mixins.json'
document = json.loads(config.read_text(encoding='utf-8'))
document['client'] = [name for name in document.get('client', [])
                      if name != 'SkeletonModelHistoricalArmsMixin']
config.write_text(json.dumps(document, indent=2) + '\n', encoding='utf-8')
print('client mixins now:', len(document['client']))

audit = root / 'docs/nt-port-audit.md'
text = audit.read_text(encoding='utf-8')
text += '''

## OLD_SKELETON_ARMS: ported in round 64, REVERTED in round 65

I ported it, then checked the evidence I had claimed for it, and took it back out. Recording both the mistake and
the reason, because the mistake was a familiar one.

What I claimed: that the Indev 20100202 removal of "arm swinging attack animations" covered skeletons as well as
zombies, so both halves of the mob_arms module rested on the same research row.

What the research actually says: a search of feature-timeline.json for the skeleton bow pose returns NOTHING. The
only skeleton animation row is strafing while shooting. The Indev quote is about zombies, and I carried it across
to skeletons because the two share an upstream directory.

Why the behaviour is probably BACKWARDS, not merely unsupported: upstream's skeleton branch replaces the bow
drawing pose with MobArmHelper.applyStaticArms plus an idle bob - the arms-out idle stance. Beta skeletons raised
their arms to draw a bow when shooting, which is the classic firing animation, so applying this removes an
animation these versions HAD. The zombie branch is the opposite case: it removes swinging that the versions had
already lost.

Reinstating it needs one thing only: a research row dating when the skeleton bow pose changed. Until that exists,
it is an unsupported visual change and it stays out.
'''
audit.write_text(text, encoding='utf-8')
print('audit records the revert')
