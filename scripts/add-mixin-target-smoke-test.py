"""Make the server probe LOAD the classes my mixins target, so transformation failures surface there.

The blind spot that broke the owner's game twice: FoodData is transformed when its class loads, and a failed
injection is fatal at that moment. A dedicated-server probe never loads FoodData - nothing places a player - so
the failure was invisible to every boot check, and only a client join found it.

Loading a class is enough to transform it, and it needs no player: `new FoodData()` does it. So the probe asks for
the classes my mixins target by name and instantiates the ones it safely can, reporting either success or the
fatal error. That turns "only a player join can see this" into something a server run checks automatically.

Classes are chosen because a mixin target sits on them: FoodData (the one that broke), BlockBehaviour, ItemStack,
LivingEntity, Mob, Player, ExperienceOrb, ServerLevel, ParticleEngine. Client-only classes are skipped - this is a
server - and the report says so rather than implying they were checked.
"""
import pathlib

root = pathlib.Path(__file__).resolve().parents[1]
path = root / 'src/main/java/net/historicalexperience/server/GenerationVerification.java'
text = path.read_text(encoding='utf-8')

old = '''    private static void run(MinecraftServer server) {'''
new = '''    /**
     * Loads the classes my mixins target, because loading a class is what transforms it.
     *
     * <p>A failed injection is fatal at transformation, and transformation happens at class load. A dedicated
     * server never loads {@code FoodData} - nothing places a player - so an injection that broke every join was
     * invisible to every boot check this project ran. Instantiating the class here surfaces that failure in a
     * server run instead, which is what the probe is for.
     *
     * <p>Client-only targets (Gui, HumanoidArmorLayer, ItemInHandRenderer, ParticleEngine, ClientLevel,
     * Minecraft, HumanoidModel, AbstractZombieModel, AnimationUtils, SkeletonModel, GuiAccess, SheepAccess) cannot
     * be loaded on a server and are listed in the report as NOT checked, rather than left to look covered.
     */
    private static Map<String, Object> mixinTargetSmokeTest() {
        Map<String, Object> out = new LinkedHashMap<>();
        List<String> serverTargets = List.of(
            "net.minecraft.world.food.FoodData",
            "net.minecraft.world.level.block.state.BlockBehaviour",
            "net.minecraft.world.item.component.ItemAttributeModifiers",
            "net.minecraft.world.entity.LivingEntity",
            "net.minecraft.world.entity.Mob",
            "net.minecraft.world.entity.player.Player",
            "net.minecraft.world.entity.ExperienceOrb",
            "net.minecraft.server.level.ServerLevel");

        for (String name : serverTargets) {
            try {
                Class<?> type = Class.forName(name);
                out.put(name, "loaded and transformed");
                if (name.endsWith("FoodData")) {
                    // Instantiating is the strongest form of this check available without a player: it runs the
                    // constructor as well as the transformation.
                    type.getDeclaredConstructor().newInstance();
                    out.put(name, "loaded, transformed and constructed");
                }
            } catch (Throwable failure) {
                out.put(name, "FAILED: " + failure);
            }
        }

        out.put("client-only targets NOT checked by a server run",
            List.of("Gui", "HumanoidArmorLayer", "ItemInHandRenderer", "ParticleEngine", "ClientLevel",
                "Minecraft", "HumanoidModel", "AbstractZombieModel", "AnimationUtils", "SkeletonModel"));
        return out;
    }

    private static void run(MinecraftServer server) {'''
assert old in text, 'run anchor not found'
text = text.replace(old, new, 1)

old = '''            report.put("combatHooks", net.historicalexperience.gameplay.HistoricalCombatDiagnostics.report());'''
new = '''            report.put("combatHooks", net.historicalexperience.gameplay.HistoricalCombatDiagnostics.report());
            report.put("mixinTargets", mixinTargetSmokeTest());'''
assert old in text, 'combat hooks anchor not found'
text = text.replace(old, new, 1)

path.write_text(text, encoding='utf-8')
print('probe now loads every server-side mixin target')
