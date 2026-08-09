# Events

Cultivation exposes **173 listener hooks** across fifteen subsystems — 93
post-events and 80 cancellable pre-events. This page covers the rules that apply
to all of them. For the full catalog — every event, its payload, and what each
field means — see **[the event reference](events-reference.md)**.

| Class | Covers |
| --- | --- |
| `CultivationEvents` | Qi, meditation, rituals, breakthroughs, advancements, demotions, tribulations, the Heart-Devil Trial, Qi Deviation, the Ascension capstone, races, skill tree, respecs |
| `DaoEvents` | Elements, affinity drift, Yin-Yang alignment, moral paths, karma, Devil harvest |
| `TechniqueEvents` | Performing and learning arts, fusing two into a third, mastery advancement, Sword Flying, timed combat buffs |
| `ItemEvents` | Loot drops, pills, spirit cores, manuals, weapon refinement, Life-Bound treasures |
| `BeastEvents` | Taming, hatching, binding, summoning, companion growth, beast arts, evolution, mounts |
| `SectEvents` | Founding, membership, ranks, abbreviations, halls, inscriptions, the sect Dao, shared progression, buildings |
| `WarEvents` | Declaring sieges and how they resolve |
| `DuelEvents` | Challenges, duels, wager payouts |
| `FormationEvents` | Laying and dispersing spirit arrays, trap strikes |
| `DwellingEvents` | Cave Abodes, Spirit Springs, upkeep, seclusion |
| `CelestialEvents` | *(0.8.0)* Server-wide phenomena starting and ending — Spirit Tide, Meteor Shower, Blood Moon, and [any an addon registers](registries.md#celestial-event-types) |
| `BodyTemperingEvents` | Tempering sessions and the thresholds they cross |
| `FistEvents` | Fist-art levels earned by landing blows bare-handed |
| `ProfileEvents` | Switching between a player's cultivation profiles |
| `StoreBenefitEvents` | *(0.8.0)* [Treasure Pavilion](store-benefits.md) entitlements arriving and leaving. **The one class here that does not fire on a world thread** |

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

### The one exception: `StoreBenefitEvents`

`StoreBenefitEvents` breaks the rule above, because there is no player-world in
hand when it fires. Grants and revokes are discovered by an HTTP sweep, so those
listeners run on **the remote checker thread**, and the payload carries a bare
`UUID` rather than a `Ref` — for a player who may well be offline. Find them and
hop first; see [Treasure Pavilion benefits](store-benefits.md#these-do-not-run-on-a-world-thread).

### `CelestialEvents` has no subject, so it has no *particular* world thread

`CelestialEvents` is still dispatched from a ticking world — `CelestialScheduleSystem`
is an ordinary delayed system — so a listener is on *a* world thread and must not
block. But a celestial event is server-wide and has no subject player, so **which**
world thread runs your listener is whichever one reached the shared scheduler
first, and the payload carries a `CelestialEventType` rather than a `ref()`.

Treat it like the store events for the purpose of touching anybody: enumerate the
players you care about and hop onto each one's own world thread before reading a
component. Everything else in this API follows the subject's-world-thread rule
exactly as described above.

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

Switch on the reason rather than cancelling unconditionally. `MeditationStopReason`
gained a third value in 0.8.0 — `RITUAL_COMPLETE`, the cultivator rising from a
seat they just earned a rank in — and a blanket `setCancelled(true)` now keeps them
sat there afterwards. This is the one stop reason that fires **after** the change
it reports: the rank is already granted and the ritual state already cleared, so
cancelling cannot undo the breakthrough. It only leaves them seated, which reads
as a stuck player rather than as a feature.

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
