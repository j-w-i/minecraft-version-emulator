"""Let the probe prove the damage table, instead of asking the owner to swing at something.

The open question is whether the era's damage values differ from the modern ones AT ALL for the items the
owner tried. A sword is expected to be identical before and after 1.9, so "nothing changed" may be correct.
This prints both numbers for several items, computed through the same ItemAttributeModifiers component the
game reads, on a real server.

It also sets the active session during the probe, because every session-gated rule is inert without it and a
probe that measures nothing is worse than no probe.
"""
import pathlib

root = pathlib.Path(__file__).resolve().parents[1]
path = root / 'src/main/java/net/historicalexperience/server/GenerationVerification.java'
text = path.read_text(encoding='utf-8')

# 1. Act as a joined player would, so session-gated rules are live while measuring.
old = '            report.put("judgedVersionId", versionId);'
new = '''            report.put("judgedVersionId", versionId);
            // Stand in for the player join that normally sets this. Every session-gated rule - the era's
            // damage values, the 1.9 cooldown and sweep - is inert without it, so a probe that does not set
            // it measures the modern game and reports success.
            if (versionId != null) {
                net.historicalexperience.gameplay.HistoricalActiveSession.set(versionId);
            }'''
assert old in text, 'session anchor not found'
text = text.replace(old, new, 1)

# 2. The measurement.
old = '    private static void run(MinecraftServer server) {'
new = '''    /**
     * What the era's damage table does to a few real items, next to what the modern component says.
     *
     * <p>Written because "the damage values did not change" was reported for items where no change is
     * expected: 1.9's 15w34c reduced swords by one and raised axes by four then three, so a diagonal sword is
     * the same number on both sides of the boundary. A probe that prints both numbers turns that into a fact
     * rather than an argument.
     */
    private static Map<String, Object> damageTableProbe() {
        Map<String, Object> out = new LinkedHashMap<>();
        java.util.List<net.minecraft.world.item.Item> items = java.util.List.of(
            net.minecraft.world.item.Items.WOODEN_SWORD, net.minecraft.world.item.Items.DIAMOND_SWORD,
            net.minecraft.world.item.Items.DIAMOND_AXE, net.minecraft.world.item.Items.IRON_AXE,
            net.minecraft.world.item.Items.DIAMOND_PICKAXE, net.minecraft.world.item.Items.DIAMOND_SHOVEL,
            net.minecraft.world.item.Items.DIAMOND_HOE);
        for (net.minecraft.world.item.Item item : items) {
            var stack = new net.minecraft.world.item.ItemStack(item);
            var modern = stack.get(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS);
            if (modern == null) {
                out.put(item.toString(), "no attribute component");
                continue;
            }
            double modernDamage = modern.compute(1.0,
                net.minecraft.world.entity.EquipmentSlot.MAINHAND);
            double eraDamage = net.historicalexperience.gameplay.HistoricalDamageValues
                .rewrite(stack, modern)
                .compute(1.0, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
            out.put(item.toString(), "applicable="
                + net.historicalexperience.gameplay.HistoricalDamageValues.isApplicable(stack)
                + " modern=" + modernDamage + " era=" + eraDamage
                + (modernDamage == eraDamage ? " (SAME on both sides of the boundary)" : ""));
        }
        return out;
    }

    private static void run(MinecraftServer server) {'''
assert old in text, 'run anchor not found'
text = text.replace(old, new, 1)

old = '            report.put("gameplay", gameplayProbe(level, versionId));'
new = '''            report.put("gameplay", gameplayProbe(level, versionId));
            report.put("damageTable", damageTableProbe());'''
assert old in text, 'gameplay anchor not found'
text = text.replace(old, new, 1)

path.write_text(text, encoding='utf-8')
print('probe now measures the damage table and sets the session')
