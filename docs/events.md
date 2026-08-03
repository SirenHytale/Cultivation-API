# Events

Cultivation exposes **160 listener hooks** across thirteen subsystems. This page covers
the rules that apply to all of them. For the full catalogue — every event, its
payload, and what each field means — see
**[the event reference](events-reference.md)**.

| Class | Covers |
| --- | --- |
| `CultivationEvents` | Qi, meditation, rituals, breakthroughs, advancements, demotions, tribulations, the Heart-Devil Trial, Qi Deviation, races, skill tree, respecs |
| `DaoEvents` | Elements, affinity drift, Yin-Yang alignment, moral paths, karma, Devil harvest |
| `TechniqueEvents` | Performing and learning arts, mastery advancement, Sword Flying, timed combat buffs |
| `ItemEvents` | Loot drops, pills, spirit cores, manuals, weapon refinement, Life-Bound treasures |
| `BeastEvents` | Taming, hatching, binding, summoning, companion growth, beast arts, evolution, mounts |
| `SectEvents` | Founding, membership, ranks, halls, inscriptions, the sect Dao, shared progression, buildings |
| `WarEvents` | Declaring sieges and how they resolve |
| `DuelEvents` | Challenges, duels, wager payouts |
| `FormationEvents` | Laying and dispersing spirit arrays, trap strikes |
| `DwellingEvents` | Cave Abodes, Spirit Springs, upkeep, seclusion |
| `BodyTemperingEvents` | Tempering sessions and the thresholds they cross |
| `FistEvents` | Fist-art levels earned by landing blows bare-handed |
| `ProfileEvents` | Switching between a player's cultivation profiles |

## Pre vs post

Nearly every mechanic is exposed **twice**.

A **`Pre*` event** fires *before* the change, extends `CancellableEvent`, and lets
a listener do two things:

```java
CultivationEvents.onPreBreakthrough(event -> {
    // 1. Veto it outright.
    if (!myPlugin.mayAscend(event.player())) {
        event.setCancelled(true);
        return;
    }

    // 2. Or re-tune the numbers driving it.
    event.setQiCost(event.qiCost() * 0.5f);
});
```

Whatever the listeners leave in those fields when dispatch finishes is what the
mod actually uses. **This is the supported way to reshape a mechanic from an addon
without touching Cultivation's config files** — a breakthrough's Qi cost, a
technique's cooldown, a tribulation bolt's damage, a tame's odds, the Qi a
meditation tick banks.

The matching **post-event** is a plain record fired once the change is committed.
It cannot be cancelled and is purely a notification:

```java
CultivationEvents.onBreakthrough(event ->
        myPlugin.announce(event.player(), event.newRealm()));
```

A cancelled pre-event means the post-event never fires.

## What cancelling means

"Don't do this." The mod checks the flag the instant dispatch returns and abandons
the operation, leaving **no state changed**. Nothing is rolled back because
nothing was applied yet — that is the whole reason these fire before the fact.

**Every listener runs, even after one cancels.** A later listener is free to call
`setCancelled(false)` and let the operation through, so plugin load order decides
who wins a disagreement. If you only want to observe, listen for the post-event
instead.

## Threading

Listeners are invoked **synchronously on the world thread of the player the event
happened to**.

- For a post-event, the change has already been applied — a `BreakthroughEvent`'s
  component state already shows the new realm.
- For a pre-event, nothing has been applied yet.
- Reading that player's components inside a listener is safe.
- **Do not block.** No sleeps, no `.get()` on a future, no file I/O.
- To touch anything on a *different* world, hop threads yourself first:

```java
World world = Universe.get().getWorld(playerRef.getWorldUuid());
if (world != null) {
    world.execute(() -> {
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return;   // gone since the event fired
        }
        // ...
    });
}
```

A listener that throws is caught, logged and skipped, so one broken addon can
neither break the mod's own systems nor other addons' listeners. Do not rely on
it — it hides your bug.

## Registration

Register once, from your plugin's `setup()`. The listener lists are
`CopyOnWriteArrayList`s, so registration is safe from any plugin in any load
order — the same guarantee `CultivationAPI`'s registries carry.

There is deliberately **no unregister**: listener lifetime is server lifetime,
matching how plugins load once and stay.

## Payload conventions

Almost every payload carries the subject:

| Field | Meaning |
| --- | --- |
| `ref()` | The entity. Always non-null. |
| `player()` | Their `PlayerRef`. **Nullable** — null when the `PlayerRef` component was unavailable at fire time. Always guard it. |

Post-events are Java `record`s, so their fields are accessed as `event.newRealm()`
— no `get` prefix. Pre-events are classes with the same accessor style plus
`setX(...)` for anything re-tunable.

## Worked examples

**Double Qi gain during a server event**

```java
CultivationEvents.onPreQiGain(event -> {
    if (myPlugin.isDoubleQiWeekend()) {
        event.setAmount(event.amount() * 2f);
    }
});
```

`amount()` is what the gain would be after Cultivation's own race/skill/pill/sect/
dao multipliers; `baseAmount()` is what it was before any listener touched it.

**Make a ritual unbreakable**

```java
CultivationEvents.onPreMeditationStop(event -> {
    if (event.reason() == CultivationEvents.MeditationStopReason.MOVEMENT) {
        event.setCancelled(true);
    }
});
```

**Block sect wars outside a scheduled window**

```java
WarEvents.onPreWarDeclare(event -> {
    if (!myPlugin.isWarWindowOpen()) {
        event.setCancelled(true);
    }
});
```

**Grant your own currency when a player breaks through**

```java
CultivationEvents.onBreakthrough(event -> {
    PlayerRef player = event.player();
    if (player == null) {
        return;
    }
    myPlugin.grantShards(player, event.newRealm().ordinal() * 10);
});
```

**Keep your own state in step with a race change**

```java
CultivationEvents.onRaceChange(event -> {
    PlayerRef player = event.player();
    if (player == null) {
        return;
    }

    World world = Universe.get().getWorld(player.getWorldUuid());
    if (world == null) {
        return;
    }

    world.execute(() -> {
        Ref<EntityStore> ref = player.getReference();
        if (ref == null || !ref.isValid()) {
            return;
        }
        myPlugin.onRaceChanged(ref.getStore(), ref, event.newRace());
    });
});
```
