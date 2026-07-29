# Driving progression

[Reading player state](reading-state.md) is the read half. This is the write half
— granting Qi, ranking a cultivator up, seating them in meditation, drawing on the
world's Qi. It is for an addon that wants to *feed* Cultivation's progression from
its own content: a quest that rewards Qi, an item that forces a breakthrough, a
boss that hands out a skill node.

If you want to **replace** the ladder rather than feed it, you want a
[`ProgressionProvider`](progression-provider.md) instead.

> **Why not just write the component?** Every call here goes through the same code
> path the mod's own commands and rituals use. So an addon granting Qi fires the
> same events, honours an installed `ProgressionProvider`, and refreshes the HUD
> and rankings exactly as meditating would — none of which is true of reaching
> into `CultivationComponent`.

## Threading

All of these run on the caller's thread and touch only the given entity, so they
are safe from a system, a command, an interaction or an event listener **on that
entity's own world thread**. To act on a player who may be in *another* world, hop
first:

```java
PlayerRef target = Universe.get().getPlayer(uuid);
CompletableFuture.runAsync(() -> {
    Ref<EntityStore> ref = target.getReference();
    CultivationAPI.addQi(ref.getStore(), ref, 500f, target);
}, target.getWorld());
```

## Qi

```java
void  addQi(accessor, ref, float amount, @Nullable PlayerRef playerRef)
void  setQi(accessor, ref, float qi)
float getQiRequiredForNext(accessor, ref)
```

**`addQi` is the gameplay path.** It runs the amount through every multiplier
(race, skill tree, pills, sect hall, Yin-Yang balance), through the cancellable
`PreQiGainEvent`, and into whatever progression is installed. `playerRef` may be
null for a non-player cultivator; pass it when you have it, because the sect-hall
bonus and the event both want it.

**`setQi` is the admin path** — banked Qi outright, skipping every multiplier and
event. Use it for a respec, a debug command or a save-restore, not for something a
player earned. Neither call ranks anyone up on its own.

```java
// A quest reward, from your own command or interaction.
CultivationAPI.addQi(accessor, ref, 500f, playerRef);
```

## Ranking up

```java
boolean completeBreakthrough(accessor, ref, @Nullable PlayerRef)   // realm
boolean completeAdvancement(accessor, ref, @Nullable PlayerRef)    // sub-stage
void    demote(accessor, ref, @Nullable PlayerRef, boolean wasBreakthrough)

void setRealm(accessor, ref, CultivationRealm)
void setStage(accessor, ref, CultivationStage)
```

**`completeBreakthrough` / `completeAdvancement` are the gameplay path** — they
consume the banked Qi, grant the skill points, fire the `Pre*` and post events,
and play the celebration, as though the ritual had just finished. This is what an
addon offering its own route to a breakthrough (a quest, an item, a boss kill)
should call. Both return `false` if a listener vetoed it, the cultivator is
already at max, or the entity has no `CultivationComponent`.

**`setRealm` / `setStage` are the admin path** — they move a cultivator outright
and re-apply their stat bonuses, but fire no breakthrough event and run no ritual,
because nothing was broken through. Use them for a restore or a debug command.

**`demote`** applies the failed-ritual penalty: one sub-stage down (never below
the current realm's first stage) and the banked Qi wiped, with the demotion events
fired. `wasBreakthrough` only picks which message the cultivator is sent.

### Asking before acting

```java
boolean isMaxLevel(accessor, ref)
boolean isReadyForBreakthrough(accessor, ref)
boolean isReadyForAdvancement(accessor, ref)
```

`isReadyFor*` is true when the cultivator has banked enough and only needs the
ritual — the right gate for an item that says "you are ready" or an NPC that
offers to officiate.

## Skill points and nodes

```java
void    grantSkillPoints(accessor, ref, int points)
boolean unlockSkillNode(accessor, ref, String nodeId)   // spends points
boolean grantSkillNode(accessor, ref, String nodeId)    // free
```

`unlockSkillNode` takes the menu's path — it spends the player's points, checks
the prerequisites, and re-applies the node's stat modifiers. `grantSkillNode`
skips the charge, for an addon handing out a node as a **reward** rather than as a
purchase. Both return `false` if the player has no skill tree component or the
unlock did not succeed.

## Meditation

```java
void startMeditating(accessor, ref)
void stopMeditating(accessor, ref)
```

`startMeditating` seats a cultivator, plays the cross-legged pose and lets the
meditation system start drawing Qi on the next tick — as `/cultivation meditate`
does, anchor included, so the movement-cancel check does not immediately cancel
it. Movement still cancels it, and a cultivator already meditating is left alone.

`stopMeditating` ends it. Note it does **not** apply the failed-ritual penalty —
call `demote` as well if you are interrupting a ritual and mean it to cost
something.

## The world's Qi

```java
SpiritVeinManager.VeinReading readSpiritVein(World world, int chunkX, int chunkZ)
float                         drainSpiritVein(World world, int chunkX, int chunkZ, float amount)
```

`readSpiritVein` reads a chunk's Spirit Vein **without touching it**: no component
is created, nothing is written, and a chunk nobody has ever visited still gives a
truthful answer, because the seeding roll is a pure function of the world seed and
the chunk position. This is what Spirit Sense reads, and the right call for a map
overlay, a divining item, or worldgen decoration that should follow the Qi.

`drainSpiritVein` draws Qi out, creating and seeding the vein if this is the first
time anything has touched it. It regenerates the vein to now first, so the amount
available is the real one rather than whatever it held when last drained. It
returns **how much was actually drawn**, which is less than requested when the
vein is running dry and `0` when the chunk is not loaded — so always use the
return value rather than assuming you got what you asked for.

```java
float drawn = CultivationAPI.drainSpiritVein(world, chunkX, chunkZ, 40f);
if (drawn <= 0f) {
    // the vein is spent; don't pay out
    return;
}
CultivationAPI.addQi(accessor, ref, drawn, playerRef);
```

**Both take chunk coordinates, not block coordinates** — divide by 16, or use
`ChunkUtil.chunkCoordinate(blockX)`. `drainSpiritVein` writes to a world's chunk
store, so it must run on **that world's** thread.

Since 0.6.1, veins form a coherent landscape rather than an independent roll per
chunk, so neighbouring chunks read as a slope you can walk along. An addon that
sends a player prospecting can rely on that: "the Qi rises to the north" is now a
true statement about the world.

## Reading the rest of the world

The subsystems whose *events* this API already exposes also have read-side entry
points, so an addon listening to `SectEvents` no longer has to shadow the whole
registry to answer "what sect is this player in".

```java
Sect       getSect(UUID player)                     // null if none
Sect       getSectByName(String name)               // case-insensitive
float      getSectQiBonusPercent(UUID player)       // 0 if no hall

DaoComponent   getOrCreateDao(accessor, ref)
DaoElement     getDaoElement(accessor, ref)         // null if they walk no dao
CultivationPath getPath(accessor, ref)              // null with no Dao component
float          getYinPercent(accessor, ref)         // 0 = wholly Yang, 100 = wholly Yin
float          getKarma(accessor, ref)

Dwelling   getAbode(UUID player)                    // their claimed Cave Abode, or null
Dwelling   getDwellingAt(String world, int chunkX, int chunkZ)
SpiritBeastComponent getBeast(accessor, ref)        // null if unbound
DuelManager.Duel     getDuel(UUID player)           // null if not duelling

float getMeditationRegenMultiplier(String world, int chunkX, int chunkZ, UUID player)
```

`getMeditationRegenMultiplier` is the combined formation-and-dwelling multiplier
for that chunk and that player — above 1 inside a Qi-gathering array or their own
abode, below 1 where a warding array chokes an outsider. Useful for an addon that
wants its own passive regen to respect the same ground.

`getYinPercent` returns `50` (perfectly balanced) rather than 0 when a cultivator
has no Dao component, since 0 would mean "wholly Yang" and that is a claim about
them you do not have.
