# Replacing progression

`ProgressionProvider` hands Cultivation's entire realm/stage/Qi ladder over to
your mod, while **every other subsystem keeps working on top of the
replacement** — sects, daos, techniques, spirit beasts, formations, abodes,
duels, alchemy and the skill tree.

This is how the SoulRings addon turns Cultivation into *Douluo Dalu*: spirit power
across a hundred levels, martial souls and soul rings, with Cultivation's sects
and formations and duels intact underneath.

```java
CultivationAPI.setProgressionProvider(new MyProgression());
```

From that moment on, every command, UI page, HUD line and gameplay gate in
Cultivation reads its numbers from you instead of from `CultivationComponent`:
`/cultivation` shows your rank names, the HUD shows your progress bar, the
meditation ritual runs your rank-ups, and a technique requiring
`GOLDEN_CORE_FORMATION` asks `getEquivalentRealm` whether the player qualifies.

Pass `null` to hand progression back to the built-in system — which your
`shutdown()` should do, so a server unloading only your mod returns to a working
ladder rather than to a provider whose components no longer exist.

> **Only one provider can be live.** The last to register wins, and a warning
> naming both is logged. Two mods each believing they own progression is a
> misconfiguration, not something to silently pick a winner for.

## You own the storage

The interface is deliberately **stateless** — every method is handed the accessor
and ref of the player being asked about, so your own persisted component is the
single source of truth.

The player's `CultivationComponent` still exists (it is what the built-in system
uses when no provider is installed, and it keeps a server's pre-addon save data
intact if your mod is later removed), but nothing in Cultivation reads its
realm/stage/Qi while you are installed. **Do not mirror your numbers into it.**

## Threading

Every method is called on the world thread of the player in question, from inside
a ticking system, a command, or a UI build. Treat the accessor as valid only for
the duration of the call.

The accessor **may be a `CommandBuffer` rather than a `Store`** — so component
creation must go through it rather than `ref.getStore().putComponent`, which
throws `"Store is currently processing!"` and takes the world down. Reads are fine
either way.

## The one call you must not forget

```java
CultivationAPI.refreshProgression(accessor, ref);
```

**Call this every time you change a player's level** — and, for a live progress
bar, whenever you change their banked progress. Cultivation cannot detect a change
inside somebody else's component, so anything that skips this leaves the HUD, the
rankings and every realm gate showing that player's previous standing until their
next meditation tick.

Worth calling from your own player-join handling too, so a player whose component
loads after Cultivation's join hook is not gated on last session's numbers for
their first few seconds.

## The interface

### Level

```java
int getLevel(accessor, ref)          // flat, ever-increasing power number
int getMaxLevel()                    // for progress bars and "%d / %d"
boolean isMaxLevel(accessor, ref)    // meditation stops granting once true
```

`getLevel` does not have to match the built-in 0–27 scale. Cultivation uses it for
the max-health and damage curves, the cross-player rankings, and anywhere a single
"how strong is this cultivator" scalar is needed.

### Progress within a level

```java
float getProgress(accessor, ref)                 // shown as "Qi" by the built-in system
float getProgressRequiredForNext(accessor, ref)  // return Float.MAX_VALUE at max level
void  addProgress(accessor, ref, float amount, @Nullable PlayerRef player)
```

`addProgress` is called from Cultivation's own sources — meditating on a spirit
vein, a Devil-path player kill, an admin `/cultivation addqi`. **Your system
decides what those are worth: return without doing anything to refuse the source
entirely**, which is the right answer for a progression that only advances on
kills.

The amount arrives already through Cultivation's multipliers (race, skill tree,
pills, sect hall, Yin-Yang) and the cancellable `PreQiGainEvent`.

### Display

```java
Message getRankLabel(accessor, ref)      // where the built-in shows "Golden Core Formation"
Message getSubRankLabel(accessor, ref)   // where it shows "Late-Stage"; null shows nothing

// Offline counterparts - the rankings list every cultivator ever seen, including
// everyone offline, so they have only the recorded level to go on.
default Message getRankLabelForLevel(int level)     { return null; }
default Message getSubRankLabelForLevel(int level)  { return null; }
```

Implement the two `ForLevel` defaults whenever level alone determines the rank,
which it usually does. Leaving them returning `null` leaves the rankings falling
back to the realm names recorded alongside the level.

### Gating — the important one

```java
CultivationRealm getEquivalentRealm(accessor, ref)
```

Which realm this player counts as, for **every** realm gate in Cultivation and its
config files: a technique's or race's `Unlock-Realm`, a beast species'
`Min-Realm`, `Pk-Min-Victim-Realm`, the Dao's switch-cost scaling, Sword Flying's
per-realm speed, abode quality, and anything an addon gates with
`CultivationAPI.getRealm`.

Map your ladder onto the seven realms with a lookup table — a proportional split
weighted toward the early realms, so a new player is not locked out of the mod's
toys:

```java
/** Lowest level counting as each realm, indexed by realm ordinal. */
private static final int[] REALM_MIN_LEVEL = {1, 10, 25, 40, 55, 70, 85};

@Nonnull
@Override
public CultivationRealm getEquivalentRealm(@Nonnull ComponentAccessor<EntityStore> accessor,
                                           @Nonnull Ref<EntityStore> ref) {
    int level = getLevel(accessor, ref);

    CultivationRealm[] realms = CultivationRealm.values();
    CultivationRealm held = realms[0];
    for (int i = 0; i < realms.length && i < REALM_MIN_LEVEL.length; i++) {
        if (level >= REALM_MIN_LEVEL[i]) {
            held = realms[i];
        }
    }
    return held;
}
```

Returning a constant works, but then every realm-gated feature in the mod is
permanently open or permanently shut.

### Rank-up rituals

Cultivation drives rank-ups through a timed meditation ritual, in two flavors.
Map your progression onto them however suits it:

| | |
| --- | --- |
| **Advancement** | The routine step. Shorter ritual, lower spirit-vein requirement, no tribulation lightning by default. |
| **Breakthrough** | The milestone step. Longer ritual, higher spirit-vein requirement, tribulation lightning — or the Heart-Devil Trial for a deeply-leaned cultivator. |

```java
boolean isReadyForBreakthrough(accessor, ref)   // tested FIRST
boolean isReadyForAdvancement(accessor, ref)
float   getBreakthroughDurationSeconds(accessor, ref)
float   getAdvancementDurationSeconds(accessor, ref)

void completeBreakthrough(accessor, ref, @Nullable PlayerRef player)
void completeAdvancement(accessor, ref, @Nullable PlayerRef player)
void demote(accessor, ref, @Nullable PlayerRef player, boolean wasBreakthrough)
```

Only one may be ready at a time; `isReadyForBreakthrough` is tested first.
Returning true from neither simply means meditation does its ordinary thing that
tick.

A natural mapping: every level is an *advancement*, and every tenth — a new rank —
is a *breakthrough*.

The `complete*` methods only have to move your own numbers and tell the player.
Cultivation still handles what surrounds them: skill points, the celebration
particle, the HUD refresh, and firing `BreakthroughEvent`.

`demote` fires when a ritual is failed — walked out of, or lost to Qi Deviation.
The built-in system drops the player one sub-stage and wipes their banked Qi; do
whatever the equivalent punishment is for yours.

### Stat curves (optional)

```java
default float getHealthBonus(accessor, ref, float configuredHealthPerLevel) {
    return getLevel(accessor, ref) * configuredHealthPerLevel;
}

default float getDamageMultiplier(accessor, ref, float configuredDamagePercentPerLevel) {
    return 1f + (getLevel(accessor, ref) * (configuredDamagePercentPerLevel / 100f));
}
```

Both default to Cultivation's own curves driven by your `getLevel`. Override only
if your levels want a different shape.

`getHealthBonus` is applied as a keyed stat modifier, so returning a smaller
number later correctly shrinks the bonus. `getDamageMultiplier` has race, skill
tree and technique multipliers applied on top, so **return `1.0` for "no bonus",
never `0`**.

## Skeleton

```java
public final class MyProgression implements ProgressionProvider {

    @Nonnull @Override
    public String getId() {
        return "MyAddon:my_ladder";   // namespaced; used in logs and /cultivation admin
    }

    // ... the methods above, reading and writing YOUR component ...
}
```

Install it last in `setup()` — everything the provider answers with (your
components, your registries) should be in place before Cultivation can ask it
anything. In practice nothing asks until a player actually plays, but the ordering
costs nothing to get right.

```java
@Override
protected void setup() {
    // ... register components, systems, configs ...
    CultivationAPI.setProgressionProvider(new MyProgression());
}

@Override
protected void shutdown() {
    CultivationAPI.setProgressionProvider(null);
}
```

## Pairing it with a theme

A replacement progression usually wants replacement vocabulary — soul masters
rather than cultivators. See [Theming](theming.md); the two are independent, and
installing both is what makes an addon read as its own game rather than a reskin.
