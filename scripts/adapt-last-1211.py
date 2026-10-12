"""One-off: the final four 1.21.1 API differences, each resolved by reading the real jar.

Every substitution here was checked against the mapped 1.21.1 jar with javap or jar tf first, because three
earlier guesses about surrounding types were wrong and guessing again is how wrong hooks get shipped.
"""
import pathlib

base = pathlib.Path('src/main/java/net/historicalexperience')
changed = []


def patch(relative, replacements):
    path = base / relative
    text = path.read_text(encoding='utf-8')
    before = text
    for old, new in replacements:
        if old not in text:
            print('  MISSING in %s: %r' % (relative, old[:70]))
        text = text.replace(old, new)
    if text != before:
        path.write_text(text, encoding='utf-8')
        changed.append(relative)


# 1. Fabric's 1.21.1 use-item callback returns InteractionResultHolder, not InteractionResult.
#    TypedActionResult, the Yarn name for it, is not in the Mojang-mapped jar at all - checked with jar tf.
patch('gameplay/HistoricalSwordBlockingEvents.java', [
    ('import net.minecraft.world.TypedActionResult;', 'import net.minecraft.world.InteractionResultHolder;'),
    ('TypedActionResult.pass(player.getItemInHand(hand))',
     'InteractionResultHolder.pass(player.getItemInHand(hand))'),
    ('TypedActionResult.consume(player.getItemInHand(hand))',
     'InteractionResultHolder.consume(player.getItemInHand(hand))'),
])

# 2. WorldData exposes worldGenOptions() in 1.21.1, not a combined getWorldGenSettings().
patch('server/HistoricalServerLifecycle.java', [
    ('server.getWorldGenSettings().options().generateStructures()',
     'server.getWorldData().worldGenOptions().generateStructures()'),
])

# 3. generatorTag mixed Optional and direct reads after the earlier pass; rewritten as plain steps, which
#    is also easier to read than a chain that mixes the two shapes.
patch('world/upgrade/WorldGenSettingsAdvance.java', [
    ('''        return root.getCompound("data")
            .map(data -> childOrNull(data, "dimensions"))
            .filter(java.util.Objects::nonNull)
            .flatMap(dimensions -> dimensions.getCompound(OVERWORLD))
            .flatMap(overworld -> overworld.getCompound("generator"));''',
     '''        CompoundTag data = childOrNull(root, "data");
        CompoundTag dimensions = childOrNull(data, "dimensions");
        CompoundTag overworld = childOrNull(dimensions, OVERWORLD);
        return Optional.ofNullable(childOrNull(overworld, "generator"));'''),
])

# 4. In 1.21.1 Registry.get(ResourceLocation) hands back the value itself, which may be null, where 26.3
#    handed back a Holder. The absent case is kept as its own answer, not folded into an empty preset.
patch('integration/modernerbeta/ModernerBetaSettingsAccess.java', [
    ('''        var holder = presetRegistry.get(HISTORICAL_PRESET_ID);
        if (!holder.isBound()) {''',
     '''        // 1.21.1's Registry.get(ResourceLocation) returns the value, nullable; 26.3 returned a Holder.
        ModernBetaSettingsPreset raw = presetRegistry.get(HISTORICAL_PRESET_ID);
        if (raw == null) {'''),
    ('''        ModernBetaSettingsPreset raw = holder.value();
        String chunkPreset''', '''        String chunkPreset'''),
])

print('patched:', changed)
