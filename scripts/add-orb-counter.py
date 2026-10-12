"""Add an orb-refusal counter, so the new ServerLevel hook is observable like the others."""
import pathlib

path = (pathlib.Path(__file__).resolve().parents[1]
        / 'src/main/java/net/historicalexperience/gameplay/HistoricalCombatDiagnostics.java')
text = path.read_text(encoding='utf-8')

anchor = '    private static final LongAdder DAMAGE_REWRITES = new LongAdder();'
assert anchor in text, 'field anchor not found'
text = text.replace(anchor, anchor + '\n    private static final LongAdder ORBS_REFUSED = new LongAdder();', 1)

anchor = '    /** The counters, for /hex status. */'
assert anchor in text, 'report anchor not found'
text = text.replace(anchor, '''    public static void recordOrbRefused() {
        ORBS_REFUSED.increment();
    }

    /** The counters, for /hex status. */''', 1)

anchor = '        report.put("damageHookRewrites", Long.toString(DAMAGE_REWRITES.sum()));'
assert anchor in text, 'report entry anchor not found'
text = text.replace(anchor, anchor + '\n        report.put("orbsRefused", Long.toString(ORBS_REFUSED.sum()));', 1)

path.write_text(text, encoding='utf-8')
print('added the orb-refusal counter')
