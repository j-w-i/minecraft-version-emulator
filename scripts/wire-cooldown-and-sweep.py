"""Wire the two 1.9 combat gates: no attack cooldown, no sweep.

Both are 1.9 features and NO declared profile has them (the newest is Release 1.6.4), so the boundary sits
above every version rather than between two of them. They are expressed in the same tested table as every
other gate, with Integer.MAX_VALUE as that boundary, instead of as a second rule the table does not know
about.

Both hooks read HistoricalActiveSession rather than a Level. Sprinting taught that lesson: a Level-based gate
returns null on the client, so the client keeps the behaviour while the server refuses it and the player sees
the modern one.
"""
import pathlib

root = pathlib.Path(__file__).resolve().parents[1]
tweaks = root / 'src/main/java/net/historicalexperience/gameplay/HistoricalGameplayTweaks.java'
text = tweaks.read_text(encoding='utf-8')


def swap(old, new, label):
    global text
    assert old in text, 'anchor not found: ' + label
    text = text.replace(old, new, 1)
    print('patched', label)


swap('''            case "itemPickup", "monsterGear", "monsterEnchant", "babyZombie" ->
                isDeclaredVersion(versionId) && olderThan(versionId, RELEASE_1_6_4);''',
     '''            case "itemPickup", "monsterGear", "monsterEnchant", "babyZombie" ->
                isDeclaredVersion(versionId) && olderThan(versionId, RELEASE_1_6_4);
            // 1.9 combat, and every declared profile is older than 1.9 - the newest is Release 1.6.4 - so
            // these apply to all ten. Expressed as a boundary above every version rather than as a separate
            // rule, so the tested table stays the single source of truth.
            case "cooldown", "sweep" -> isDeclaredVersion(versionId);''', 'appliesTo cases')

swap('''        return List.of("avoidSun", "babyAnimal", "babyZombie", "breeding", "criticalHit", "itemPickup",
            "monsterEnchant", "monsterGear", "panic", "skeletonStrafing", "tempting");''',
     '''        return List.of("avoidSun", "babyAnimal", "babyZombie", "breeding", "cooldown", "criticalHit",
            "itemPickup", "monsterEnchant", "monsterGear", "panic", "skeletonStrafing", "sweep", "tempting");''',
     'gateNames')

swap('''    public static boolean attackCooldownDisabled(Level level) {
        return isDeclaredWorld(level);
    }''',
     '''    public static boolean attackCooldownDisabled(Level level) {
        return appliesTo("cooldown", versionOf(level));
    }''', 'attackCooldownDisabled')

swap('''    public static boolean sweepAttackDisabled(Level level) {
        return isDeclaredWorld(level);
    }''',
     '''    public static boolean sweepAttackDisabled(Level level) {
        return appliesTo("sweep", versionOf(level));
    }''', 'sweepAttackDisabled')

tweaks.write_text(text, encoding='utf-8')

# The test's table must name them, or it fails by construction - which is the point of that check.
test = root / 'src/test/java/net/historicalexperience/gameplay/HistoricalGameplayTweaksTest.java'
text = test.read_text(encoding='utf-8')
swap_old = '''    /** The one gate dated by Release 1.2.1, proxied by Release 1.2.5. */
    private static final List<String> BEFORE_RELEASE_1_2_5 = List.of("avoidSun");'''
swap_new = '''    /** The one gate dated by Release 1.2.1, proxied by Release 1.2.5. */
    private static final List<String> BEFORE_RELEASE_1_2_5 = List.of("avoidSun");

    /**
     * 1.9 combat, which no declared profile has: the newest is Release 1.6.4, so the boundary sits above
     * every version rather than between two of them.
     */
    private static final List<String> ABSENT_FROM_EVERY_DECLARED_VERSION = List.of("cooldown", "sweep");'''
assert swap_old in text, 'test list anchor not found'
text = text.replace(swap_old, swap_new, 1)

anchor = '        BEFORE_RELEASE_1_2_5.forEach(gate -> boundary.put(gate, release125));'
assert anchor in text, 'test boundary anchor not found'
text = text.replace(anchor, anchor + '\n        ABSENT_FROM_EVERY_DECLARED_VERSION.forEach(gate -> boundary.put(gate, Integer.MAX_VALUE));', 1)
test.write_text(text, encoding='utf-8')
print('patched the gate test table')
