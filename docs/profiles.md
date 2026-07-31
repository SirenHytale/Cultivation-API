# Cultivation profiles

A player keeps several **profiles** — separate saves of their own progress — and
switches between them. Starting a second cultivator from Body Refinement costs
them nothing, because the first one is still sitting in its slot.

This page is about what that means for *your* mod, which is mostly one question:
**does your addon keep state that belongs to a cultivator rather than to an
account?** If it does, you have work to do here. If it does not, you can ignore
profiles entirely and nothing will break.

---

## What a profile holds, and what it does not

A switch replaces the components that describe *what a cultivator has become*:

| Swapped with the profile | Stays with the account |
| --- | --- |
| Realm, stage, Qi | Sect membership and rank |
| Race | Cave abode claim |
| Dao alignment and karma | Placed formations |
| Skill tree and points | Marriage, Life-Bound items |
| Learned techniques and keybind layouts | The server-wide leaderboard entry |
| Running pill effects | HUD layout, palette, keybinds, aura toggles |

The dividing line is ownership, not importance. A sect has other members and a
hall other sects can besiege; an abode is claimed ground; a Trapping array fires
on whoever walks into it. None of those are one player's alone to move, so a
profile switch changes *who you are*, never *what you own*.

---

## Reading the current profile

```java
String name = CultivationAPI.getActiveProfileName(store, ref);
int kept    = CultivationAPI.getProfileCount(store, ref);   // real slots, sandbox excluded
int max     = CultivationAPI.getMaxProfiles();              // 3, unless an addon raised it
boolean sandbox = CultivationAPI.isTestProfileActive(store, ref);
```

These are readable at any time, which matters most on **join**: no switch event
has fired yet, and your own state still has to be keyed to the right cultivator.
After that, react to the events below rather than polling.

---

## Keeping your own progression in step

This is the important part. Cultivation swaps *its* components. Anything you
keep alongside them — your own levels, your own unlocks, a cached view of the
player's rank — is not swapped, and after a switch it describes the cultivator
who just left.

Two events bracket the swap:

```java
// Save YOUR state for the profile being left.
ProfileEvents.onPreProfileSwitch(event -> {
    MyLevels levels = myStore.get(event.player().getUuid());
    myProfileStore.put(key(event.player(), event.from()), levels.snapshot());
});

// Load it back for the profile that just arrived.
ProfileEvents.onProfileSwitch(event -> {
    MySnapshot saved = myProfileStore.get(key(event.player(), event.to()));
    myStore.put(event.player().getUuid(), saved == null ? MyLevels.fresh() : saved.restore());
});
```

`PreProfileSwitchEvent` fires **before** Cultivation saves the outgoing profile,
which is exactly when your own state still belongs to the cultivator being left.
`ProfileSwitchEvent` fires after the new components are in place.

Both run on the world thread that owns the player and **outside any ticking
system**, so unlike most of this API you may write components directly through
the Store. That is deliberate: a profile switch is itself a component rewrite,
and a listener that could only queue its own save would always be one tick late.

---

## If you replace progression entirely

A [`ProgressionProvider`](progression-provider.md) owns the player's level, so a
profile switch that ignored it would leave Cultivation showing a fresh cultivator
while your mod still had them at their old level.

Cultivation therefore **refuses profile switching by default** whenever a
provider is installed. That is what a provider which has never thought about
profiles gets for free, and it is the safe answer.

Once you handle the two events above, say so:

```java
@Override
public boolean supportsProfiles(){
    return true;
}
```

and the refusal lifts.

---

## The sandbox profile

One extra slot, gated behind `cultivation.profile.test`, whose realm, stage, Qi,
skill points and race are set by hand rather than earned. It does not consume one
of the player's normal slots, it carries a required time limit and dissolves when
that runs out, and Cultivation keeps it off the leaderboard — and so out of the
rankings, sect scores and sect rosters.

`Profile.isTest()` tells it apart, and it is worth checking. If your mod mirrors
Cultivation's realm into a ranking of its own, you almost certainly want to skip
that slot for the same reason Cultivation does:

```java
ProfileEvents.onProfileSwitch(event -> {
    if(event.to().isTest()){
        myRankings.suspend(event.player().getUuid());
        return;
    }
    myRankings.resume(event.player().getUuid());
});
```

`ProfileExpireEvent` tells you when a sandbox dissolved, including whether the
player was on it and has just been put back on a real cultivator.

---

## Vetoing

The three `Pre` events are cancellable in the usual way:

```java
ProfileEvents.onPreProfileDelete(event -> {
    if(myTournament.isEntered(event.player().getUuid())){
        event.setCancelled(true);
    }
});
```

A cancelled operation returns `BLOCKED` to whoever asked, and the player is told
that another mod refused it.

---

## Raising the cap

Three profiles by default. An addon may raise it, up to a hard ceiling of six:

```java
CultivationAPI.registerProfileCap("myMod", 5);
// ... and in shutdown()
CultivationAPI.unregisterProfileCap("myMod");
```

Caps are taken as the **highest** registered value, never the sum, so two addons
that both ask for five mean five.

**Nothing is destroyed when the cap falls.** A player who filled five profiles and
then lost the addon that allowed them keeps all five — profiles decode up to the
ceiling regardless of the live cap, and only *adding* is refused until they are
back under it. The same rule governs
`registerTechniquePresetCap`.
