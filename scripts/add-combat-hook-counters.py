"""Make the combat hooks countable, so "it did not work" becomes a number.

The owner's /hex status output proves the active session is set and all three combat gates answer true, so
the gate logic is right and the mixins are in the jar. That leaves the hooks themselves: either they are not
being called, or they are called somewhere that does not decide the behaviour. A counter in each hook tells
those apart, and /hex status already proved it is a channel the owner can use.
"""
import pathlib

root = pathlib.Path(__file__).resolve().parents[1]
gp = root / 'src/main/java/net/historicalexperience/gameplay'
mx = root / 'src/main/java/net/historicalexperience/mixin'

# 1. The counter holder.
(gp / 'HistoricalCombatDiagnostics.java').write_text('''package net.historicalexperience.gameplay;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

/**
 * How many times each combat hook has actually run, so a rule that is correct but never called can be told
 * apart from one that is called and ignored.
 *
 * <p>This exists because the owner's report - cooldown, sweep and damage values all still modern - came with
 * a diagnostic proving the active session was set and every gate answered true. That rules out the gate. What
 * it cannot distinguish is "the hook never runs" from "the hook runs and changes nothing that matters", and
 * those need different fixes.
 */
public final class HistoricalCombatDiagnostics {
    private static final LongAdder COOLDOWN_ANSWERS = new LongAdder();
    private static final LongAdder SWEEP_CANCELLED = new LongAdder();
    private static final LongAdder DAMAGE_REWRITES = new LongAdder();

    private HistoricalCombatDiagnostics() {
    }

    public static void recordCooldownAnswer() {
        COOLDOWN_ANSWERS.increment();
    }

    public static void recordSweepCancel() {
        SWEEP_CANCELLED.increment();
    }

    public static void recordDamageRewrite() {
        DAMAGE_REWRITES.increment();
    }

    /** The counters, for /hex status. */
    public static Map<String, String> report() {
        Map<String, String> report = new LinkedHashMap<>();
        report.put("cooldownHookCalls", Long.toString(COOLDOWN_ANSWERS.sum()));
        report.put("sweepHookCancels", Long.toString(SWEEP_CANCELLED.sum()));
        report.put("damageHookRewrites", Long.toString(DAMAGE_REWRITES.sum()));
        return report;
    }
}
''', encoding='utf-8')
print('wrote HistoricalCombatDiagnostics')

# 2. Count in the combat hooks.
combat = mx / 'PlayerHistoricalCombatMixin.java'
text = combat.read_text(encoding='utf-8')
text = text.replace('''        if (HistoricalGameplayTweaks.appliesTo("cooldown", HistoricalActiveSession.current())) {
            callback.setReturnValue(1.0F);''',
'''        if (HistoricalGameplayTweaks.appliesTo("cooldown", HistoricalActiveSession.current())) {
            net.historicalexperience.gameplay.HistoricalCombatDiagnostics.recordCooldownAnswer();
            callback.setReturnValue(1.0F);''', 1)
text = text.replace('''        if (HistoricalGameplayTweaks.appliesTo("sweep", HistoricalActiveSession.current())) {
            callback.cancel();''',
'''        if (HistoricalGameplayTweaks.appliesTo("sweep", HistoricalActiveSession.current())) {
            net.historicalexperience.gameplay.HistoricalCombatDiagnostics.recordSweepCancel();
            callback.cancel();''', 1)
combat.write_text(text, encoding='utf-8')
print('counted the combat hooks')

# 3. Count in the damage hook.
damage = mx / 'ItemStackHistoricalDamageMixin.java'
text = damage.read_text(encoding='utf-8')
old = '''        ItemAttributeModifiers modern = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        return modern == null ? null : HistoricalDamageValues.rewrite(stack, modern);'''
new = '''        ItemAttributeModifiers modern = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (modern == null) {
            return null;
        }
        net.historicalexperience.gameplay.HistoricalCombatDiagnostics.recordDamageRewrite();
        return HistoricalDamageValues.rewrite(stack, modern);'''
assert old in text, 'damage anchor not found'
damage.write_text(text.replace(old, new, 1), encoding='utf-8')
print('counted the damage hook')

# 4. Report them where the owner already knows to look.
service = root / 'src/main/java/net/historicalexperience/server/HistoricalUpgradeService.java'
text = service.read_text(encoding='utf-8')
anchor = '''        return new Report(true, "This world is " + metadata.profileId(), details);'''
addition = '''        net.historicalexperience.gameplay.HistoricalCombatDiagnostics.report()
            .forEach((name, value) -> details.add("combat hook counter: " + name + "=" + value));
        return new Report(true, "This world is " + metadata.profileId(), details);'''
assert anchor in text, 'service anchor not found'
service.write_text(text.replace(anchor, addition, 1), encoding='utf-8')
print('status now reports the combat hook counters')
