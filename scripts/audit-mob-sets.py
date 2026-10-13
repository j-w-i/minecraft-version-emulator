"""Check each version's natural-spawn set against the research, in both directions.

Written because the owner asked the right question - "have you checked what mobs can spawn?" - and the honest answer
was no. The sets existed, two builds shared one correctly, and a spawn filter was observed removing chickens at
runtime. None of that establishes that a set is RIGHT.

Two failure modes matter, and they are not symmetric:

  * a mob ALLOWED that had not arrived yet  -> it spawns in a world that never had it
  * a mob ABSENT that had arrived            -> it never spawns in a world that should have it

The first is the one the owner has been finding by playing. The second is silent: an empty world looks like a
quiet one.
"""
import json
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[1]
SPAWN_RULES = ROOT / 'src/main/java/net/historicalexperience/gameplay/HistoricalSpawnRules.java'
TRANSITIONS = ROOT / 'research/version-transitions.json'

# One canonical name per mob, with the words the research might use to introduce it.
MOBS = {
    'minecraft:creeper': ('creeper',),
    'minecraft:zombie': ('zombie',),
    'minecraft:skeleton': ('skeleton',),
    'minecraft:spider': ('spider',),
    'minecraft:pig': ('pig',),
    'minecraft:sheep': ('sheep',),
    'minecraft:cow': ('cow', 'cattle'),
    'minecraft:chicken': ('chicken',),
    'minecraft:slime': ('slime',),
    'minecraft:giant': ('giant',),
    'minecraft:ghast': ('ghast',),
    'minecraft:zombified_piglin': ('zombie pigman', 'zombie pigmen', 'zombified piglin'),
    'minecraft:squid': ('squid',),
    'minecraft:wolf': ('wolf', 'wolves'),
    'minecraft:enderman': ('enderman', 'endermen'),
    'minecraft:silverfish': ('silverfish',),
    'minecraft:cave_spider': ('cave spider',),
    'minecraft:snow_golem': ('snow golem',),
    'minecraft:mooshroom': ('mooshroom',),
    'minecraft:villager': ('villager',),
    'minecraft:blaze': ('blaze',),
    'minecraft:magma_cube': ('magma cube',),
    'minecraft:ocelot': ('ocelot',),
    'minecraft:iron_golem': ('iron golem',),
    'minecraft:bat': ('bat',),
    'minecraft:witch': ('witch',),
    'minecraft:wither_skeleton': ('wither skeleton',),
    'minecraft:zombie_villager': ('zombie villager',),
    'minecraft:horse': ('horse',),
    'minecraft:wither': ('wither boss', 'the wither'),
}

# The declared versions, oldest first, with the name their research entry uses.
VERSIONS = [
    ('historical_experience:infdev_420', 'Infdev 20100420'),
    ('historical_experience:infdev_611', 'Infdev 20100611'),
    ('historical_experience:alpha_1_0_1', 'Alpha 1.0.1'),
    ('historical_experience:alpha_1_0_4', 'Alpha 1.0.4'),
    ('historical_experience:alpha_1_0_6', 'Alpha 1.0.6'),
    ('historical_experience:alpha', 'Alpha 1.1.0'),
    ('historical_experience:beta_1_7_3', 'Beta 1.7'),
    ('historical_experience:beta_1_8_1', 'Beta 1.8'),
    ('historical_experience:beta_1_9_pre_3', 'Beta 1.9 Prerelease 3'),
    ('historical_experience:release_1_0_0', 'Release 1.0'),
    ('historical_experience:release_1_1', 'Release 1.1'),
    ('historical_experience:release_1_2_5', 'Release 1.2'),
    ('historical_experience:release_1_6_4', 'Release 1.6'),
]


def declared_mob(sets, constant_name):
    """The mobs in one declared spawn set, read from the Java source."""
    match = re.search(
        constant_name + r'\s*=\s*(?:Set\.of|.*?Stream\.concat)\((.*?)\);',
        sets, re.S)
    if not match:
        return None
    return set(re.findall(r'"(minecraft:[a-z_]+)"', match.group(0)))


def arrived_by(transitions):
    """For each mob, the earliest version whose deltas introduce it (or None if never mentioned)."""
    arrivals = {}
    for mob, words in MOBS.items():
        for version_id, display in VERSIONS:
            for version in transitions:
                if version.get('displayName') != display and version.get('id') not in (version_id,):
                    continue
                for field in ('introduced', 'changed'):
                    for item in (version.get(field) or []):
                        lowered = item.lower()
                        if any(word in lowered for word in words):
                            arrivals.setdefault(mob, version_id)
    return arrivals


def main():
    sets = SPAWN_RULES.read_text(encoding='utf-8')
    transitions = json.loads(TRANSITIONS.read_text(encoding='utf-8'))['versions']
    arrivals = arrived_by(transitions)

    print('mobs the research dates to a version:')
    for mob, version in sorted(arrivals.items(), key=lambda pair: pair[1]):
        print('    %-30s from %s' % (mob, version))
    print()

    print('declared spawn sets, as written:')
    for constant in ('INFDEV_MOBS', 'ALPHA_1_0_1_MOBS', 'ALPHA_1_1_2_MOBS',
                     'BETA_1_7_3_MOBS', 'BETA_1_8_1_MOBS'):
        mobs = declared_mob(sets, constant)
        if mobs is not None:
            print('    %-20s %d mobs: %s' % (constant, len(mobs), ', '.join(sorted(mobs))))
    print()

    print('mobs allowed in a set that the research dates LATER than that version:')
    found = False
    for constant, version_id in (
            ('ALPHA_1_0_1_MOBS', 'historical_experience:alpha_1_0_1'),
            ('ALPHA_1_1_2_MOBS', 'historical_experience:alpha'),
            ('BETA_1_7_3_MOBS', 'historical_experience:beta_1_7_3'),
            ('BETA_1_8_1_MOBS', 'historical_experience:beta_1_8_1')):
        mobs = declared_mob(sets, constant) or set()
        order = [v[0] for v in VERSIONS]
        for mob in sorted(mobs):
            arrival = arrivals.get(mob)
            if arrival and order.index(arrival) > order.index(version_id):
                print('    %-20s allows %-28s which arrives %s' % (constant, mob, arrival))
                found = True
    if not found:
        print('    none found by this method')


if __name__ == '__main__':
    main()
