"""One-off: the last 1.21.1 API differences in the ported tree.

Each substitution is a rename or a one-line shape change between 1.21.1 and 26.3, and each is commented at
its site so the reason survives. Run once; it is idempotent.
"""
import pathlib
import sys

base = pathlib.Path('src/main/java/net/historicalexperience')


def patch(relative, replacements, required=True):
    path = base / relative
    text = path.read_text(encoding='utf-8')
    before = text
    for old, new in replacements:
        text = text.replace(old, new)
    if text != before:
        path.write_text(text, encoding='utf-8')
        print('patched', relative)
    elif required:
        print('no change in', relative)
    return text


# 1.21.1's SpawnerData exposes its entity type as a field, not a method.
patch('gameplay/HistoricalSpawnFilter.java', [
    ('.getKey(candidate.type())', '.getKey(candidate.type)'),
])

# 1.21.1's Fabric use-item callback predates InteractionResult; its 0.7.x API returns TypedActionResult.
patch('gameplay/HistoricalSwordBlockingEvents.java', [
    ('import net.minecraft.world.InteractionResult;',
     'import net.minecraft.world.ItemStackWithSlot;\nimport net.minecraft.world.TypedActionResult;'),
])

# Remaining 26.3 lookups that my earlier pass missed because the call spans lines.
patch('integration/modernerbeta/ModernerBetaAdapter.java', [
    ('.lookupOrThrow(', '.registryOrThrow('),
    ('server.registryAccess().listRegistryKeys()',
     'server.registryAccess().registries().map(entry -> entry.key())'),
])

# 1.21.1's Holder answers isBound()/value(); 26.3 added isEmpty()/get().
patch('integration/modernerbeta/ModernerBetaSettingsAccess.java', [
    ('if (holder.isEmpty()) {', 'if (!holder.isBound()) {'),
    ('holder.get().value()', 'holder.value()'),
    ('raw.mapped(presetRegistry)', 'raw.mapped(presetRegistry.asLookup())'),
])

# 1.21.1 named the payload registry direction playS2C; 26.3 renamed it clientboundPlay.
patch('server/HistoricalVersionSync.java', [
    ('PayloadTypeRegistry.clientboundPlay()', 'PayloadTypeRegistry.playS2C()'),
    ('handler.player.level()', 'handler.player.serverLevel()'),
])

# The same Optional-vs-direct NBT difference, at the one site the first script did not reach.
patch('world/upgrade/WorldGenSettingsAdvance.java', [
    ('.flatMap(data -> data.getCompound("dimensions"))',
     '.map(data -> childOrNull(data, "dimensions"))\n            .filter(java.util.Objects::nonNull)'),
])

print('done')
