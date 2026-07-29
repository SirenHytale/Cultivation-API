# Reading player state

Every read takes the two things you already have wherever entities are involved —
a `ComponentAccessor<EntityStore>` and a `Ref<EntityStore>`:

```java
import plugin.siren.API.CultivationAPI;
import plugin.siren.ECS.Realms.CultivationRealm;
import plugin.siren.ECS.Realms.CultivationStage;
import plugin.siren.ECS.Races.PlayerRace;

CultivationRealm realm = CultivationAPI.getRealm(accessor, ref);
CultivationStage stage = CultivationAPI.getStage(accessor, ref);
int level              = CultivationAPI.getGlobalLevel(accessor, ref);
float qi               = CultivationAPI.getQi(accessor, ref);
PlayerRace race        = CultivationAPI.getRace(accessor, ref);
boolean meditating     = CultivationAPI.isMeditating(accessor, ref);
int skillPoints        = CultivationAPI.getAvailableSkillPoints(accessor, ref);
boolean hasVitality1   = CultivationAPI.isNodeUnlocked(accessor, ref, "VITALITY_1");
```

You have both inside a ticking system, a command, an item interaction, and every
event listener in this API. From a `PlayerRef`, get them with
`playerRef.getReference()` and `ref.getStore()` — on that player's world thread.

## What each one returns

| Call | Returns | When it is empty |
| --- | --- | --- |
| `getRealm` | The player's **effective** realm | `null` if the entity has no `CultivationComponent` (not a player) |
| `getStage` | Their stored sub-stage | `null` if no component, **and always `null` while a `ProgressionProvider` is installed** |
| `getGlobalLevel` | A flat, ever-increasing power number | `0` if no component |
| `getQi` | Banked progress toward the next rank-up | `0` if no component |
| `getRace` | Their race | Defaults to Human — never null |
| `isMeditating` | Whether they are sitting in meditation | `false` if no state component |
| `getAvailableSkillPoints` | Unspent skill points | `0` if no skill tree component |
| `isNodeUnlocked` | Whether a skill node id is unlocked | `false` if no component or no such node |

## The three that are provider-aware

This matters if you want your mod to keep working on a server that has replaced
Cultivation's progression with an addon like SoulRings.

- **`getRealm`** returns the installed [`ProgressionProvider`](progression-provider.md)'s
  *equivalent* realm rather than a stored one. This is what you want for a gate —
  it keeps working whatever ladder the server is running.
- **`getGlobalLevel`** returns the provider's level if there is one, else the
  built-in realm/stage flattened into one number.
- **`getQi`** returns the provider's progress if there is one, else banked Qi.

**`getStage` is the exception** — it returns `null` under a provider, because a
replacement progression has no sub-stages. Never gate on it; use `getRealm` for
gating and `getGlobalLevel` for anything numeric.

## Gating on a realm

`CultivationRealm` is an enum in declaration order, so `ordinal()` compares
cleanly:

```java
CultivationRealm realm = CultivationAPI.getRealm(accessor, ref);
if (realm == null || realm.ordinal() < CultivationRealm.GOLDEN_CORE_FORMATION.ordinal()) {
    // not there yet
    return;
}
```

The seven realms, weakest first:

```
BODY_REFINEMENT  QI_CONDENSATION  FOUNDATION_ESTABLISHMENT  GOLDEN_CORE_FORMATION
NASCENT_SOUL  SOUL_FORMATION  VOID_REFINEMENT
```

`CultivationRealm.fromName(String)` parses a configured name back into the enum,
returning `null` if it does not match — useful when your own config file lets a
server owner pick the gate:

```java
CultivationRealm unlockRealm = CultivationRealm.fromName(config.get().getUnlockRealm());
if (unlockRealm == null) {
    LOGGER.atWarning().log("'%s' is not a Cultivation realm; falling back to Body Refinement.",
            config.get().getUnlockRealm());
    unlockRealm = CultivationRealm.BODY_REFINEMENT;
}
```

## Component types

For the cases the getters above do not cover, the raw component types are
available. Prefer the getters — components are internal types whose shape may
change between versions.

```java
ComponentType<EntityStore, CultivationComponent>         getCultivationComponentType()
ComponentType<EntityStore, CultivationStateComponent>    getCultivationStateComponentType()
ComponentType<EntityStore, CultivationSettingsComponent> getCultivationSettingsComponentType()
ComponentType<EntityStore, RaceComponent>                getRaceComponentType()
ComponentType<EntityStore, SkillTreeComponent>           getSkillTreeComponentType()
ComponentType<EntityStore, TechniqueComponent>           getTechniqueComponentType()
ComponentType<ChunkStore,  SpiritVeinComponent>          getSpiritVeinComponentType()
```

Note the last one lives on the **`ChunkStore`**, not the `EntityStore` — spirit
veins are a property of the chunk, not of any entity.

There are also three convenience component reads that hand back `null` rather
than making you write the `accessor.getComponent(...)` call:

```java
CultivationComponent      getCultivationComponent(accessor, ref)
CultivationStateComponent getCultivationStateComponent(accessor, ref)
RaceComponent             getRaceComponent(accessor, ref)
```

### Reading a chunk's spirit vein

Prefer `CultivationAPI.readSpiritVein(world, chunkX, chunkZ)` — see
[Driving progression](driving-progression.md#the-worlds-qi). It answers truthfully
for a chunk nobody has ever visited, creates nothing, and does not need the chunk
loaded. The raw component is there for the cases it does not cover:

```java
SpiritVeinComponent vein = chunkAccessor.getComponent(
        chunkRef, CultivationAPI.getSpiritVeinComponentType());
```

Never call `World.getBlock` / `getChunk` / `getNonTickingChunk` from inside a
ticking system unless the chunk is already resident — use
`World.getChunkIfInMemory` and skip on `null`.

## Beyond the player

The subsystems this API raises events for also expose read-side entry points —
sect membership, dao and path, karma, abode, bound beast, active duel, and the
meditation multiplier of a piece of ground. They are listed in
[Driving progression](driving-progression.md#reading-the-rest-of-the-world),
alongside the writes they pair with.

Cultivation's own settings are readable too, through
[`CultivationConfigs`](config-access.md) — the right way to ask "what does a
breakthrough cost on *this* server" rather than assuming the defaults.

## Telling Cultivation something changed

If your mod changes a player's progression numbers behind Cultivation's back —
which only a [`ProgressionProvider`](progression-provider.md) really does — say so:

```java
CultivationAPI.refreshProgression(accessor, ref);
```

That re-applies their max-health modifier, refreshes the HUD, updates the
cross-player rankings, and re-reads the realm every gate in the mod tests against.
It is a no-op for an entity with no `CultivationComponent`, and safe from a
system, a command or an interaction.
