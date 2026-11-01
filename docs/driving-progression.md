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

`setQi` writes the *built-in* bank, which an installed `ProgressionProvider` never
reads, so under a provider it changes nothing the cultivator can see — `addQi` is
the one write here that reaches a provider. A NaN or infinite value is ignored and
a negative one is clamped to 0 *(0.11.0)*.

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
should call. Banked Qi is *not* required: a breakthrough granted as a reward on an
empty bank simply spends it to zero.

On the built-in ladder the two are not interchangeable *(0.11.0)*. A breakthrough
is the step from a realm's `PEAK` into the next realm, so `completeBreakthrough`
needs the cultivator at `PEAK`; `completeAdvancement` is every other step, so it
refuses at `PEAK`. Pick by stage:

```java
boolean advanced = CultivationAPI.getStage(accessor, ref) == CultivationStage.PEAK
        ? CultivationAPI.completeBreakthrough(accessor, ref, playerRef)
        : CultivationAPI.completeAdvancement(accessor, ref, playerRef);
```

Both return `true` only when the rank actually advanced, and `false` when a
listener vetoed it, the cultivator is already at max, is at the wrong stage, or
has no `CultivationComponent`. Under a `ProgressionProvider` there is no stage gate
(`getStage` is `null` there): the call is handed to the provider, which owns which
milestone is next — see [Progression provider](progression-provider.md#rank-up-rituals).

> Before 0.11.0, calling the wrong one fired a breakthrough for what was really a
> sub-stage step (and threw at the last realm), and under a provider both returned
> `false` even when they succeeded. Re-check any code that relied on either.

**`setRealm` / `setStage` are the admin path** — they move a cultivator outright
and re-apply their stat bonuses, but fire no breakthrough event and run no ritual,
because nothing was broken through. Use them for a restore or a debug command.
Like `setQi`, they write the built-in ladder only and have no visible effect
under a `ProgressionProvider`.

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

## The end of the ladder: Ascension

*New in 0.8.0.*

A cultivator at the very top of the ladder may attempt the **Ascension Capstone**
(飞升). Surviving it either leaves them at the peak as an Ascended cultivator or —
if the server enabled prestige — resets them to the first realm to climb again with
a permanent stacking bonus.

This is deliberately **observe-and-veto only**. There is no `ascend(...)` call, and
that is not an oversight: an ascension runs a timed ritual with escalating lightning
that can kill, so a synchronous "do it now" call has no sensible semantics. The
three hooks are:

```java
CultivationEvents.onPreAscension(event -> {
    // The one hook for gating the ladder's ending behind something of your own.
    if (!myPlugin.hasHeavenlyMandate(event.player())) {
        event.setCancelled(true);
    }
});

CultivationEvents.onAscension(event -> {
    // ascensionCount INCLUDES this one. prestiged() says they chose to begin again -
    // and by the time this fires they have ALREADY been reset to the first realm.
    myPlugin.recordAscension(event.player(), event.ascensionCount(), event.prestiged());
});

CultivationEvents.onAscensionFailed(event -> {
    // abandoned() distinguishes giving up from being beaten by the tribulation.
    myPlugin.consoleThem(event.player());
});
```

Three things to plan around:

- **An ascension is not a breakthrough.** It gets its own event rather than a
  `BreakthroughEvent` with a special realm, precisely so a listener counting
  breakthroughs does not credit one here. If your mod rewards rank-ups, decide
  separately what an ascension is worth.
- **`prestiged()` means the reset already happened.** Read the player's realm inside
  `onAscension` and you will see the *first* realm, not the peak. Anything that
  needs their pre-ascension standing has to have captured it earlier — in
  `onPreAscension`, which fires while they are still at the top.
- **Prestige is off by default** (`Ascension-Prestige-Enabled`), so on most servers
  `prestiged()` is always `false` and the count only ever reaches 1. Do not build a
  progression that assumes it climbs.

The API exposes no getter for a player's ascension count between events, so record
what `onAscension` hands you rather than expecting to read it back. `isMaxLevel`
still answers whether a cultivator has run out of ladder, which is the gate for
"may they attempt this at all".

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
void    startMeditating(accessor, ref)
void    stopMeditating(accessor, ref)
boolean tryStartMeditating(accessor, ref)   // (0.11.0) true = meditating now
boolean tryStopMeditating(accessor, ref)    // (0.11.0) true = not meditating now
```

Since 0.11.0 both halves run exactly what `/cultivation meditate` runs, so an addon
seating or standing a cultivator gets every gate, message, event and penalty a
player typing the command would.

`startMeditating` refuses a fleeing Nascent Soul and a cultivator inside a Dream
Trial, Inner Demon duel or Cleanse Rite (and tells them why), then fires the
cancellable `PreMeditationStartEvent`. On success the cultivator is anchored where
they sit, plays the cross-legged pose, and `MeditationStartEvent` fires; the
meditation system starts drawing Qi on its next tick. Movement still cancels it,
and a cultivator already meditating is left alone.

`stopMeditating` stands them up *including the stand-up penalty*. Stopping someone
in the middle of a ritual costs what walking away from it costs: a breakthrough or
advancement demotes a stage and scatters the banked Qi, a Closed-Door Retreat
forfeits its payout, an Ascension or Reincarnation fails, a weapon refinement is
forfeited. `PreMeditationStopEvent` fires first with reason `COMMAND`, so a
listener holding a cultivator in a ritual holds them against this call too.

> *Changed in 0.11.0.* `stopMeditating` used to skip the penalty, the events and the
> veto, and this page told you to call `demote` yourself. Do not any more — the
> cultivator would be demoted twice. If you only meant to stand up an idle
> meditator, check `isMeditating` and the ritual state first.

The `try` forms report the outcome: `false` means nothing changed — no cultivation
state, a refusal gate, or a listener's veto. Run all four on the cultivator's own
world thread; `accessor` may be a `Store` or a system's `CommandBuffer`.

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

**Both take chunk coordinates, not block coordinates** — use
`ChunkUtil.chunkCoordinate(blockX)`. Hytale chunks are *32* blocks wide, so that is
`blockX >> 5`, never a divide by 16. `drainSpiritVein` writes to a world's chunk
store, so it must run on **that world's** thread, and returns 0 for a NaN or
infinite request.

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

## Practice, and what practice advances

Three subsystems progress by *doing the thing* rather than by banking Qi, and each
has one call that puts an addon's own activity on the same footing as the mod's:

```java
CultivationAPI.addTechniqueMasteryXp(accessor, ref, playerRef, technique);
CultivationAPI.performBeastArt(accessor, ref, art, playerRef);
int levelsGained = CultivationAPI.addSectProgressXp(memberUuid, breakthrough);
```

`addTechniqueMasteryXp` banks one successful use of an art, and promotes it if
that use was the last requirement outstanding — announcing the promotion to the
player exactly as the mod's own path does. Use it when your addon fires a
technique through some route of its own, so a player practising your way climbs
the ladder like everyone else.

Mastery is worth understanding before you tune anything through
`PreTechniquePerformEvent`, because it moved where an art's strength comes from.
A technique's damage is no longer a function of realm alone: it passes through
its own mastery multiplier, and two cultivators of identical realm can be far
apart on the same art. If you are computing an art's output outside the mod, ask
`getTechniqueMasteryMultiplier` and apply it — otherwise you will be describing a
weaker technique than the player actually has.

`addSectProgressXp` credits one member's advancement to their sect, which is
shared: it is how a sect levels, and levelling is what raises how many buildings
it may hold. Pass `breakthrough = true` for a realm breakthrough and `false` for a
lesser advancement; the return value is how many sect levels that credit
triggered, usually 0.

## Ground with an element of its own

Two things now decide what element the ground presses a meditating cultivator
toward, and both can be read directly:

```java
DaoElement terrain = CultivationAPI.getTerrainElement(biomeName);
DaoElement ground  = CultivationAPI.getGroundDao(world, chunkX, chunkZ);
```

Terrain affinity works the way weather already did — meditating in a place drifts
your element toward it over time. Sect ground is stronger and deliberate: inside a
sect's holdings, Qi absorbed comes mainly as the sect's chosen Dao.

`getGroundDao` returning `null` is not a failure. It is the answer for unclaimed
ground, for a sect that has named no Dao, and for a sect building whose Dao switch
was turned off — that last case being how a sect takes in a member who walks a
different element without bending them toward its own.
