# Historical gameplay rules — profile-scoped simulation

Status: **four rules implemented and runtime-verified across versions; the gameplay layer is otherwise unbuilt.**

## Why gameplay rules are separate from generation revisions

Generation content is immutable per revision because a stored chunk must keep meaning what its
recorded recipe said. A simulation rule is different: it changes what happens next, not what is
already stored, so it is keyed by the **gameplay profile** (the era) and applies to every generation
revision of that era — including worlds that already exist. That is why the spawn rule below needed no
new revision, while every generation-content change did.

## Rule 4 — food heals directly where there was no hunger

**Rule.** In a version with no hunger system, eating restores health directly, and a player may always
eat. In a version with hunger, eating feeds the food level and heals nothing directly.

**Evidence.** Every value is an integer literal from the hash-verified Beta 1.7.3 client (SHA-1
`43db9b498cb67058d2e12d394e6507722e71bb45`, 1,465,375 bytes, checked against Mojang's own manifest
before use). `yw` is the food class; its constructor's second argument is health points; and
`gs.c(int)` resolves to `ls.c(int)`, which adds the value and clamps at 20. Nine applied values —
apple 4, bread 5, mushroom stew 10, raw porkchop 3, cooked porkchop 8, **golden apple 42**, raw cod 2,
cooked cod 5, **cookie 1** — each with its `gm` field and initialiser recorded. The golden apple's 42
exceeds the 20-point cap deliberately, so one always refills a full bar. See
[food healing evidence](<../research/beta173-food-healing.json>).

**Why two hooks, not one.** Healing alone would have been dead code. Modern Minecraft refuses to let a
player eat unless they need food (`Player.canEat` returns `invulnerable || canAlwaysEat ||
needsFood()`), and in a no-hunger world the food level never drains because `FoodData.tick` is
cancelled — so every player sits permanently full, can never eat, and therefore can never heal. The
heal is applied in `FoodProperties.onConsume`, which receives the level, the entity and the stack and
is where vanilla applies nutrition; the gate is opened in `Player.canEat`, driven by the **absence of
hunger** rather than by whether heals have been researched, so the two cannot drift apart.

**Measured, three ways** ([Beta 1.7.3](<../logs/continuation-243-food-173.log>),
[Beta 1.8.1](<../logs/continuation-245-food-181.log>), [ordinary](<../logs/continuation-247-food-ord.log>)):

| bread | Beta 1.7.3 | Beta 1.8.1 | Ordinary |
| --- | --- | --- | --- |
| at a **full** food level | **accepted**, health 10 → **15** | **refused** | **refused** |
| at food level 6 | accepted, health 15, food 6 → 11 | accepted, health **10**, food 6 → 11 | same |
| heal / gate counters | **2 / 2** | 0 / 0 | 0 / 0 |

**Known gaps, deliberately not papered over.** Cake healed 3 points per slice through a *block*
interaction — right-clicking a placed cake, never consuming a held item — so it is recorded in the
evidence table but **not applied**; applying it to the item would heal a player for eating a cake item
that does not exist. The wolf-feeding quirk (feeding a wolf any meat healed it the hard-coded
raw-porkchop value of 3, a genuine bug in the original) is not reproduced. And the hunger bar is still
drawn, because hiding it is a client-side change and there is no client package yet — the level it
shows is inert, since the tick is cancelled.

**A measurement bug worth remembering.** The first version of the probe called `ItemStack.use()` on a
detached stack. `Item.use` ignores the stack it is called on and reads `player.getItemInHand(hand)`, so
with an empty hand it fell through to the other use branches and returned `PASS` — the probe reported
`consumed=false` and no gate override, which is what exposed it. The item now goes into the hand first,
and `use` only *starts* eating, so `finishUsingItem` completes it: both halves are exercised rather
than assumed.

## Rule 1 — natural spawning is restricted to the era's entity types

**Rule.** In a Beta 1.7.3 world only the entity types that existed in Beta 1.7.3 may spawn naturally.

**Evidence.** The allowed set is the complete entity registry of the Beta 1.7.3 client, read from the
hash-verified jar's `jc.class` constant pool — the only class in that jar containing the Beta mob
names. Reading the whole constant pool rather than searching for names I suspected is what makes the
list complete: `Chicken, Cow, Creeper, Ghast, Giant, Pig, PigZombie, Sheep, Skeleton, Slime, Spider,
Squid, Wolf, Zombie`, plus the non-mob entities (`Arrow, Boat, FallingSand, Item, Minecart, Painting,
PrimedTnt, Snowball`). See [entity registry evidence](<../research/beta173-entity-types.json>).

**Measured problem.** A Beta 1.7.3 world's live spawn lists contained six entity types that provably
did not exist then: `bat`, `turtle`, `enderman`, `witch`, `zombie_villager`, `glow_squid` ([pre-rule
measurement](<../logs/continuation-99-spawn-historical.log>)).

**Mechanism.** `NaturalSpawnerHistoricalSpawnMixin` filters the candidate list **before any entity is
created**, at both paths that can spawn a mob:

| Path | Hook | Why it is needed |
| --- | --- | --- |
| `NaturalSpawner.mobsAt` | `@Inject` at RETURN | the single choke point for ordinary natural spawning: both the random pick and the "is this spawn data still valid" check go through it |
| `NaturalSpawner.spawnMobsForChunkGeneration` | `@Redirect` on `getMobsToSpawn` | this path picks its own creature list and runs while a chunk is generated |

An **allow list**, not a deny list: the historical set is fully enumerable and closed, so an entity
type that a later Minecraft version adds is denied by construction instead of quietly admitted.
Surviving entries keep the weights the biome declared.

**Result, measured in a fresh historical world.** Raw `creature` = chicken, cow, pig, sheep,
**turtle**, wolf; `monster` = creeper, **enderman**, skeleton, slime, spider, **witch**, zombie,
**zombie_villager**; `ambient` = **bat**; `underground_water_creature` = **glow squid**. After the
rule: `creature` = chicken, cow, pig, sheep, wolf; `monster` = creeper, skeleton, slime, spider,
zombie; `water_creature` = squid — with the original weights intact (chicken 10, cow 8, pig 10,
sheep 12, wolf 5; creeper/skeleton/slime/spider 100, zombie 95). Removed: bat, enderman, glow_squid,
turtle, witch, zombie_villager. [Historical world](<../logs/continuation-104-spawn-historical.log>).

**Live execution, not just configuration.** In a fresh establish — where chunks are really generated
— the hook recorded **10 filter invocations and 10 removed entries**, all `minecraft:turtle`: turtle
is the only anachronistic entry in the chunk-generation `CREATURE` list, which is exactly the
signature a working filter produces. [Fresh establish](<../logs/continuation-106-spawn-establish.log>).

**Non-interference.** An ordinary Moderner Beta world reports `spawn.profileId=null` and
`removedByRestriction=[]`, and keeps bat, turtle, enderman, witch, zombie_villager and glow squid.
[Ordinary control](<../logs/continuation-108-spawn-ordinary.log>).

### What is NOT established

- **Weights and pack sizes are not historical.** The rule fixes *which* entity types may spawn, not
  the historical probabilities. Beta 1.7.3's own per-biome weights are not enumerated.
- **Only the chunk-generation hook was observed executing.** A headless probe has no players, and
  ordinary natural spawning requires players within range, so the `mobsAt` hook is verified as
  applied (the mixin config is `required`, so a failed application aborts startup) and covered by
  unit tests, but no real natural-spawn event was observed.
- **Out of scope by design**: spawn eggs, breeding, spawner blocks, mob conversion and chunk-loading
  entity deserialisation are not natural spawning.
- The separate open question about whether Moderner Beta's `beta` preset models 1.7.3 or a slightly
  later Beta-era game also applies here.

## Rule 2 — mob drops are the era's drops, and experience does not exist

**Rule.** In a Beta 1.7.3 world, the four mobs whose drops changed get Beta's drop item and nothing
else, and no mob awards experience.

**Evidence.** Beta 1.7.3 gave each mob one drop item through `EntityLiving.getDropItemId()` and
dropped `random.nextInt(3)` copies of it on death — 0, 1 or 2, one at a time, with no player-kill
condition and no rare-drop table. Each value was read from the mob class's own `getDropItemId()` in
the hash-verified client, and the single-letter item field it returns was resolved to its name in the
item class's static initialiser:

| Mob | Class | Drops | Modern table also drops |
| --- | --- | --- | --- |
| Zombie | `uz` | `feather` (`gm.J`) | rotten flesh, iron ingots, carrots, potatoes |
| Chicken | `ww` | `feather` (`gm.J`) | chicken |
| Spider | `cn` | `string` (`gm.I`) | spider eye |
| Cow | `bx` | `leather` (`gm.aD`) | beef |

Resolving those fields is not optional: `gm.J` is feather while `gm.j` is arrow, and the two differ
only by case. None of the modern-only items exist in the Beta 1.7.3 item registry at all — it has no
`flesh`, no `beef`, no `chicken` and no `spiderEye` entry — so this is a content difference, not a
tuning one. Experience is suppressed separately for every mob because Beta's entity registry lists
fourteen mobs and eight non-mob entities and **no experience orb**.

**Mechanism.** `LivingEntityHistoricalLootMixin` cancels `dropAllDeathLoot` after spawning Beta's
item, which is what stops the modern loot table appearing alongside it, and cancels `dropExperience`
for the profile. Both resolve the era from the level's own generator marker through the shared
profile lookup.

**Result, measured by actually killing mobs.** Eight trials per mob, killed through the real damage
path, with item counts taken as deltas around each kill so a drop is attributed to the mob that
produced it:

| World | Zombie | Chicken | Spider | Cow |
| --- | --- | --- | --- | --- |
| Historical revision 5 | feather 5 | feather 5 | string 5 | leather 9 |
| Ordinary Moderner Beta | **rotten flesh 13** | **chicken 8** + feather 5 | string 10 | **beef 16** + leather 13 |

No modern-only item appeared in the historical world, and the ordinary world keeps its full modern
tables — which is what makes this a restriction rather than a global change. [Historical
world](<../logs/continuation-144-loot-historical2.log>), [ordinary control](<../logs/continuation-146-loot-ordinary2.log>).

### What is NOT established

- **Experience suppression is applied but unexercised.** Vanilla awards a mob no experience unless a
  **player** killed it, and a headless probe has no players — so an ordinary world observes zero orbs
  too, and the measurement cannot distinguish a suppressed award from one that never applied. The rule
  is pinned by unit test and the hook is required to apply, but the first draft of this probe asserted
  "an ordinary world awards experience" and **failed**, because that assertion was simply wrong. It was
  removed rather than weakened, and the probe now reports the number as explicitly
  non-discriminating.
- **Only four mobs.** Pig, sheep, squid, creeper, skeleton and slime were checked for named-item
  equivalence and left alone; the remaining populations (rare drops, equipment, looting) are not
  modelled and Beta had no looting.
- **Item *names* are not mapped to modern ids beyond the four above.** Beta's `sulphur` is modern
  `gunpowder` and Beta's black `dyePowder` is modern `ink_sac`, so a creeper or squid rule would need
  that mapping stated explicitly rather than assumed.

## What a rule must include before it is accepted

1. A primary evidence source or a measured observation, pinned by hash or log, with the reasoning
   from evidence to rule.
2. Keying by gameplay profile, with ordinary worlds and other eras provably unaffected.
3. Enforcement on the server, before the effect happens — never a correction afterwards.
4. A runtime measurement of the rule's effect on real data, using the same code the hook runs.
5. An explicit statement of what the evidence does **not** establish.
