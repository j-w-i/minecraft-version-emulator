"""Validate that the mixin gate guard can actually fail.

A guard that cannot fail is worse than no guard, and this one was written by escaping a regex through two
languages. So the pattern is run here against the exact lines the two real bugs consisted of, and against a
correct call. If it does not match the first two, the test in HistoricalClientSideGateTest is decorative.
"""
import re

pattern = re.compile(r'(?:Disabled|Enabled)\(\s*[\w.]+\.level\(\)')

samples = [
    ('the breeding bug that was there',
     'if (HistoricalGameplayTweaks.animalBreedingDisabled(animal.level())) {', True),
    ('the sword-block bug the owner reported',
     'return HistoricalGameplayTweaks.swordBlockingEnabled(player.level()) && player.getMainHandItem()', True),
    ('the sprint bug shape',
     'HistoricalGameplayTweaks.monsterAvoidSunDisabled(mob.level())', True),
    ('a correct session call',
     'HistoricalGameplayTweaks.appliesTo("breeding", HistoricalActiveSession.current())', False),
    ('a correct session call with whitespace',
     'HistoricalGameplayTweaks.experienceOrbSpawnDisabled( level )', False),
]

failures = 0
for name, line, should_match in samples:
    matched = bool(pattern.search(line))
    verdict = 'ok' if matched == should_match else 'WRONG'
    if matched != should_match:
        failures += 1
    print('%-42s %-8s %s' % (name, verdict, 'MATCHES' if matched else 'no match'))

print()
print('guard is', 'LIVE' if failures == 0 else 'BROKEN (%d wrong)' % failures)
