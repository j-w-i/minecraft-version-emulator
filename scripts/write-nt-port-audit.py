"""Build the upstream audit that should have been the FIRST step of this port.

Every Nostalgic Tweaks feature has a named carrier file in research/nt-port-worklist.json. That mapping was
available the whole time and was not used: features were reimplemented from the game's bytecode instead. This
writes the table that makes the gap checkable - my file, the tweak it claims to implement, and the upstream
file that actually implements it.
"""
import json
import pathlib

root = pathlib.Path(__file__).resolve().parents[1]
worklist = json.loads((root / 'research/nt-port-worklist.json').read_text(encoding='utf-8'))['worklist']
by_name = {entry['name']: entry for entry in worklist}


def carrier(name):
    entry = by_name.get(name)
    if not entry:
        return '(not in worklist)'
    files = entry.get('carrierFiles') or []
    return files[0].replace('common/src/main/java/mod/adrenix/nostalgic/', '') if files else '(none)'


# My file -> the tweak names it implements. Status is read from where the code actually came from, and the
# three lines marked PORTED are the ones where upstream source was opened and copied.
ROWS = [
    ('mixin/LivingEntityHistoricalSwordBlockingMixin.java', ['SWORD_BLOCKING', 'SHIELD_DISABLE'], 'INVENTED'),
    ('mixin/PlayerHistoricalCombatMixin.java', ['DISABLE_COOLDOWN', 'DISABLE_SWEEP', 'DISABLE_CRITICAL_HIT'],
     'PORTED (round 33)'),
    ('mixin/ServerLevelHistoricalOrbMixin.java', ['DISABLE_ORB_SPAWN'], 'PORTED (round 35)'),
    ('mixin/GuiHistoricalHudMixin.java', ['HIDE_HUNGER_BAR', 'HIDE_EXPERIENCE_BAR'], 'PORTED (round 34)'),
    ('client/hud/HudHelper.java', ['HIDE_HUNGER_BAR'], 'PORTED (round 34)'),
    ('mixin/PlayerHistoricalSprintMixin.java', ['DISABLE_SPRINT'], 'INVENTED'),
    ('mixin/FoodDataHistoricalHungerMixin.java', ['DISABLE_HUNGER'], 'INVENTED'),
    ('mixin/PlayerHistoricalFoodMixin.java', ['CUSTOM_FOOD_HEALTH'], 'INVENTED'),
    ('mixin/LivingEntityHistoricalLootMixin.java', ['OLD_MOB_DROPS', 'DISABLE_XP_DROPS'], 'INVENTED'),
    ('mixin/FleeSunGoalHistoricalMixin.java', ['DISABLE_MONSTER_AVOID_SUN'], 'INVENTED'),
    ('mixin/PanicGoalHistoricalMixin.java', ['DISABLE_ANIMAL_PANIC'], 'INVENTED'),
    ('mixin/TemptGoalHistoricalMixin.java', ['DISABLE_ANIMAL_TEMPTING'], 'INVENTED'),
    ('mixin/AnimalHistoricalBreedingMixin.java', ['DISABLE_ANIMAL_BREEDING'], 'INVENTED'),
    ('mixin/AgeableMobHistoricalSpawnMixin.java', ['DISABLE_BABY_ANIMALS'], 'INVENTED'),
    ('mixin/ZombieHistoricalSpawnMixin.java', ['DISABLE_BABY_ZOMBIES'], 'INVENTED'),
    ('mixin/MobHistoricalItemPickupMixin.java', ['DISABLE_MOB_ITEM_PICKUP'], 'INVENTED'),
    ('mixin/MobHistoricalEquipmentMixin.java', ['DISABLE_MOB_EQUIPMENT', 'DISABLE_MOB_ENCHANTMENTS'], 'INVENTED'),
    ('mixin/RangedBowAttackGoalHistoricalMixin.java', ['DISABLE_SKELETON_STRAFING'], 'INVENTED'),
    ('mixin/ItemStackHistoricalDamageMixin.java', ['OLD_DAMAGE_VALUES'], 'INVENTED'),
    ('client/hud/*', ['HIDE_HUNGER_BAR'], 'PORTED (round 34)'),
]

lines = [
    '# Nostalgic Tweaks port audit',
    '',
    'Generated from `research/nt-port-worklist.json`, whose `carrierFiles` field names the upstream file that',
    'implements each tweak. This table was missing for most of the port, which is why so much was reimplemented',
    'from the game bytecode instead of copied: the mapping existed and was not used.',
    '',
    '| My file | Tweaks it claims | Upstream carrier | Origin |',
    '| --- | --- | --- | --- |',
]
for mine, names, status in ROWS:
    carriers = '<br>'.join(carrier(name) for name in names)
    lines.append('| `%s` | %s | %s | **%s** |' % (mine, ', '.join(names), carriers, status))

lines += [
    '',
    '## Remediation order',
    '',
    'Highest first: the features the owner has actually observed in play, then the rest.',
    '',
    '1. `DISABLE_SPRINT` - carrier `mixin/tweak/gameplay/mechanics_player/LivingEntityMixin.java`.',
    '2. `CUSTOM_FOOD_HEALTH` - carrier `mixin/tweak/gameplay/food_health/FoodPropertiesMixin.java`.',
    '3. `DISABLE_HUNGER` - carrier `mixin/tweak/gameplay/food_health/FoodDataMixin.java`.',
    '4. Sword blocking - carriers `helper/gameplay/...` and `combat_player/MinecraftMixin.java`.',
    '5. The mob AI and spawn gates, each from its own named carrier.',
    '6. `OLD_DAMAGE_VALUES` - carrier `combat_item/DataComponentHolderMixin.java`.',
    '',
    'Each one: read the carrier, copy its targets and injection points, swap the global config flag for this',
    "project's per-version gate, keep the upstream path in the file header, then boot-verify.",
]
out = root / 'docs/nt-port-audit.md'
out.parent.mkdir(exist_ok=True)
out.write_text('\n'.join(lines) + '\n', encoding='utf-8')
print('wrote', out)
for row in ROWS[:6]:
    print(' ', row[0], '->', row[2])
