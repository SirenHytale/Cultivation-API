# Event reference

Every event Cultivation fires, grouped by the class that declares it.
**368 listener hooks** across 51 subsystems.

> Generated from `api-sources/` by `tools/gen_events_reference.py`. Do not edit
> by hand — re-run the script instead. The prose in each entry is the javadoc on
> the event itself.

Read [events.md](events.md) first for the rules that apply to all of them: pre
vs post, threading, cancellation, and what a listener may safely do.

Every listener is registered the same way, once, from your plugin's `setup()`:

```java
CultivationEvents.onBreakthrough(event -> {
    // event.ref(), event.player(), event.newRealm()
});
```

---

## Core progression

`plugin.siren.API.CultivationEvents` — Qi, meditation, rituals, breakthroughs, advancements, demotions, tribulations, the Heart-Devil Trial, Qi Deviation, the Ascension capstone, races, the skill tree and respecs.

**Enums declared here**

- `CultivationEvents.RitualType` — Which timed meditation ritual a ritual event refers to. Values: `BREAKTHROUGH`, `ADVANCEMENT`, `REFINEMENT`
- `CultivationEvents.MeditationStopReason` — Why a player stopped meditating. Values: `COMMAND`, `MOVEMENT`, `RITUAL_COMPLETE`

**Post-events** — fired once the change is committed; cannot be cancelled.

### `BreakthroughEvent`

```java
CultivationEvents.onBreakthrough(event -> { /* ... */ });
```

A player completed a realm breakthrough; `newRealm` is the realm they just entered (their stage is EARLY). `player` is null only if the PlayerRef component was unavailable.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `newRealm()` | `CultivationRealm` |

### `AdvancementEvent`

```java
CultivationEvents.onAdvancement(event -> { /* ... */ });
```

A player completed a sub-stage advancement within `realm`, landing on `newStage`.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `realm()` | `CultivationRealm` |
| `newStage()` | `CultivationStage` |

### `RaceChangeEvent`

```java
CultivationEvents.onRaceChange(event -> { /* ... */ });
```

A player's race changed - via the race menu (`adminOverride` false) or an admin tool (`adminOverride` true). Not fired when an admin "sets" the race the player already has.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `oldRace()` | `PlayerRace` |
| `newRace()` | `PlayerRace` |
| `adminOverride()` | `boolean` |

### `SkillUnlockEvent`

```java
CultivationEvents.onSkillUnlock(event -> { /* ... */ });
```

A player unlocked a skill tree node (points already spent, modifiers already re-applied).

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `node()` | `SkillNode` |

### `TribulationStrikeEvent`

```java
CultivationEvents.onTribulationStrike(event -> { /* ... */ });
```

Tribulation lightning struck a mid-ritual cultivator. `damage` is the post-lethality-cap amount fed to the damage pipeline (pre-armor/reduction); `breakthroughRitual` distinguishes breakthrough strikes from (config-gated) advancement ones.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `damage()` | `float` |
| `breakthroughRitual()` | `boolean` |

### `TribulationOmenEvent`

```java
CultivationEvents.onTribulationOmen(event -> { /* ... */ });
```

A ritual's Storm Omen (see `TribulationOmen`) was decided at its justStarted tick. `storm` is whether it latched - true arms the harder/better-rewarded variant for the rest of this ritual attempt only; `breakthroughRitual` distinguishes breakthrough rituals from advancement/refinement ones. Fired only when the roll actually happened (opted in, or Tribulation-Storm-Omen-Opt-In-Required is false) - a player who was never eligible gets no event either way.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `storm()` | `boolean` |
| `breakthroughRitual()` | `boolean` |

### `LifeBoundLevelUpEvent`

```java
CultivationEvents.onLifeBoundLevelUp(event -> { /* ... */ });
```

A Life-Bound Treasure gained a level from combat XP. `item` is the already-updated stack (its metadata reflects `newLevel`).

| Accessor | Type |
| --- | --- |
| `owner()` | `PlayerRef` |
| `item()` | `ItemStack` |
| `newLevel()` | `int` |

### `HeartDevilTrialEvent`

```java
CultivationEvents.onHeartDevilTrial(event -> { /* ... */ });
```

The Heart-Devil Trial tormented a deeply-leaned cultivator mid-ritual. `composureRemaining` is what's left after this pulse's drain (0 when it broke); `deviated` is true only on the pulse that shattered composure into Qi Deviation; `breakthroughRitual` distinguishes breakthrough trials from (opt-in) advancement ones.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `composureRemaining()` | `float` |
| `deviated()` | `boolean` |
| `breakthroughRitual()` | `boolean` |

### `DreamTrialEvent`

```java
CultivationEvents.onDreamTrial(event -> { /* ... */ });
```

The Dream Trial's Hollow Mirror tested a cultivator mid-attempt. `composureRemaining` is what's left after this pulse's drain (0 when it broke); `broken` is true only on the pulse that shattered composure and failed the attempt; `pressure` is the dreamTrialPressure fraction (0-1) that scaled this pulse's drain - see `DreamTrialManager#dreamTrialPressure`.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `composureRemaining()` | `float` |
| `broken()` | `boolean` |
| `pressure()` | `float` |

### `InnerDemonTrialEvent`

```java
CultivationEvents.onInnerDemonTrial(event -> { /* ... */ });
```

An Inner Demon Rival Duel struck a mid-duel cultivator. `composureRemaining` is what's left after this pulse's drain (0 when it broke); `broken` is true only on the pulse that shattered composure and failed the duel; `echoIntensity` is the 0-1 fraction that scaled this pulse's drain - see `InnerDemonConfig`'s own doc; `nemesisEcho` is true if the phantom wore an active Nemesis's face rather than a generic echo of doubt.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `composureRemaining()` | `float` |
| `broken()` | `boolean` |
| `echoIntensity()` | `float` |
| `nemesisEcho()` | `boolean` |

### `CleanseRiteEvent`

```java
CultivationEvents.onCleanseRite(event -> { /* ... */ });
```

A Marrow-Cleansing Rite pulse tested a mid-rite cultivator. `composureRemaining` is what's left after this pulse's drain (0 when it broke); `broken` is true only on the pulse that shattered composure and failed the rite (deepening the targeted injury); `targetMagnitude` is the targeted injury's magnitude as snapshotted at entry - see `CleanseRiteConfig`'s own doc; `companionPresent` is true if a bonded partner or master/disciple companion was close enough this pulse to reduce the drain.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `composureRemaining()` | `float` |
| `broken()` | `boolean` |
| `targetMagnitude()` | `float` |
| `companionPresent()` | `boolean` |

### `QiGainEvent`

```java
CultivationEvents.onQiGain(event -> { /* ... */ });
```

Qi was just banked toward a player's next rank-up. `amount` is what was actually added (after every race/skill/pill/sect/dao multiplier and after any listener retune); `totalQi` is their new banked total.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `amount()` | `float` |
| `totalQi()` | `float` |

### `MeditationStartEvent`

```java
CultivationEvents.onMeditationStart(event -> { /* ... */ });
```

A player sat down to meditate.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |

### `MeditationStopEvent`

```java
CultivationEvents.onMeditationStop(event -> { /* ... */ });
```

A player stopped meditating. Any ritual penalty for standing up mid-ritual has already been applied.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `reason()` | `MeditationStopReason` |

### `RitualStartEvent`

```java
CultivationEvents.onRitualStart(event -> { /* ... */ });
```

A timed meditation ritual just began (the tick that first accrued progress).

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `type()` | `RitualType` |

### `AscensionEvent`

```java
CultivationEvents.onAscension(event -> { /* ... */ });
```

A cultivator survived the Ascension Capstone (飞升) - the end of the ladder. `ascensionCount` is the total INCLUDING this one, and `prestiged` says whether they chose to begin again (and so have already been reset to the first realm by the time this fires) or to remain at the peak as an Ascended cultivator. Deliberately its own event rather than a `BreakthroughEvent` with a special realm: an ascension is not a breakthrough, and a listener that treats it as one would credit the wrong thing.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `ascensionCount()` | `int` |
| `prestiged()` | `boolean` |

### `AscensionFailedEvent`

```java
CultivationEvents.onAscensionFailed(event -> { /* ... */ });
```

A cultivator's Ascension attempt ended in failure.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `abandoned()` | `boolean` |

### `AscensionLegacyEvent`

```java
CultivationEvents.onAscensionLegacy(event -> { /* ... */ });
```

A cultivator's Ascension completed with the LEGACY ending - `/ascend legacy sect`/`/ascend legacy self` - see `AscensionManager.beginLegacy`. Always fires alongside (and immediately after) `AscensionEvent` for the same completion, since a Legacy ending IS a prestige-shaped reset (`AscensionEvent#prestiged` is true for it too). `sectBeneficiary` is what was actually GRANTED, never merely what was requested: true for a Lineage Stele inscription, false for a Legacy Mote - including the case where a sect-aimed attempt fell back to a Mote because the player's sect was gone by completion, which `sectFallback` distinguishes. `sectName` is set only when `sectBeneficiary` is true.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `sectBeneficiary()` | `boolean` |
| `sectFallback()` | `boolean` |
| `sectName()` | `String` |

### `ReincarnationEvent`

```java
CultivationEvents.onReincarnation(event -> { /* ... */ });
```

A cultivator completed Reincarnation (转世) - the alternate capstone alongside Ascension. `fromRealm`/`toRealm` are the realm given up and the realm landed at after the partial reset; `bloodlinePointsGranted` is what was just added to their `ReincarnationLedger` entry (cumulative, not their new total). Deliberately its own event rather than a `BreakthroughEvent` or `AscensionEvent` with a special flag - a Reincarnation is neither, and a listener that treats it as one would credit the wrong thing.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `fromRealm()` | `CultivationRealm` |
| `toRealm()` | `CultivationRealm` |
| `bloodlinePointsGranted()` | `int` |

### `ReincarnationFailedEvent`

```java
CultivationEvents.onReincarnationFailed(event -> { /* ... */ });
```

A cultivator's Reincarnation attempt ended in failure.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `abandoned()` | `boolean` |

### `DemotionEvent`

```java
CultivationEvents.onDemotion(event -> { /* ... */ });
```

A player was demoted a sub-stage for abandoning a ritual (or for Qi Deviation). Banked Qi has been wiped and the granting skill points revoked.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `realm()` | `CultivationRealm` |
| `oldStage()` | `CultivationStage` |
| `newStage()` | `CultivationStage` |
| `wasBreakthrough()` | `boolean` |

### `QiDeviationEvent`

```java
CultivationEvents.onQiDeviation(event -> { /* ... */ });
```

A cultivator's composure shattered into Qi Deviation (走火入魔). Exactly one of `demoted`/`qiLost` carries the penalty that was applied.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `demoted()` | `boolean` |
| `qiLost()` | `float` |
| `breakthroughRitual()` | `boolean` |

### `RespecEvent`

```java
CultivationEvents.onRespec(event -> { /* ... */ });
```

A player respecced their skill tree; every node was cleared and `refundedPoints` handed back.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `refundedPoints()` | `int` |

### `PlayerKillEvent`

```java
CultivationEvents.onPlayerKill(event -> { /* ... */ });
```

One cultivator killed another, and the kill is worth something. The generic player-versus-player hook: everything the mod itself pays out for a kill - Devil-path Qi, Dao deeds, the Wu Xing reward, a shed manual - is credited immediately after this fires. **Only fires for a kill that PAYS.** `CultivationDeathSystem` runs its own anti-farm gate (`Pk-Same-Victim-Cooldown-Seconds` and `Pk-Min-Victim-Realm` - see `isFarmedKill`) first, and a farmed kill returns before this event exists. That ordering is the whole point: a listener running BEFORE the gate would pay out on exactly the kills the gate exists to make worthless, and no later `return` can take back a credit already made. An addon that rewards kills therefore inherits the mod's own farm protection for free - and must not go looking for an earlier hook to "catch every death", because every death is not what this event means. Also never fires for a self-inflicted death, an environmental one, a kill whose killer has no `PlayerRef`, or a kill a fleeing Nascent Soul landed (see `SoulEscapeManager` - such a kill credits nobody at all, by design). **No `Pre` twin, deliberately.** The only cancellable thing at this point is the kill itself, which belongs to the damage pipeline and is long since resolved by the time anything here runs. Every consumer is a reward path, and a reward path must be gated by its own rules rather than by vetoing somebody else's event. @param killer the slayer. Never the victim - a self-kill returns before this. @param killerPlayer the slayer's `PlayerRef`; always valid at the moment this fires. @param victim the fallen cultivator. @param victimPlayer the fallen cultivator's `PlayerRef`, or null if the component could not be read - the kill is still a player kill (the death system already established that), so this is a read failure rather than "an NPC died". @param sanctionedDuel true when this death resolved a sanctioned duel (plain or Dao). The mod excludes its own general PvP reward on those - a duel already has its own stakes - and any addon paying for kills should do the same, or two accounts can farm each other through a duel that pays both ways.

| Accessor | Type |
| --- | --- |
| `killer()` | `Ref<EntityStore>` |
| `killerPlayer()` | `PlayerRef` |
| `victim()` | `Ref<EntityStore>` |
| `victimPlayer()` | `PlayerRef` |
| `sanctionedDuel()` | `boolean` |

**Pre-events** — fired before the change; `setCancelled(true)` vetoes it, and any setter below re-tunes the numbers the mod then uses.

### `PreBreakthroughEvent`

```java
CultivationEvents.onPreBreakthrough(event -> { /* ... */ });
```

A player is about to complete a realm breakthrough. Cancel to hold them at Peak stage (their ritual progress resets and they may retry); adjust `setQiCost` to change what the breakthrough consumes.

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read (may be null) |
| `fromRealm()` | `CultivationRealm` | read |
| `toRealm()` | `CultivationRealm` | read |
| `qiCost()` | `float` | read |
| `setQiCost(float)` | `void` | re-tune |

### `PreAdvancementEvent`

```java
CultivationEvents.onPreAdvancement(event -> { /* ... */ });
```

A player is about to complete a sub-stage advancement. Cancel to hold them where they are; adjust `setQiCost` to change what it consumes.

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read (may be null) |
| `realm()` | `CultivationRealm` | read |
| `fromStage()` | `CultivationStage` | read |
| `toStage()` | `CultivationStage` | read |
| `qiCost()` | `float` | read |
| `setQiCost(float)` | `void` | re-tune |

### `PreRaceChangeEvent`

```java
CultivationEvents.onPreRaceChange(event -> { /* ... */ });
```

A player's race is about to change. Cancel to keep their current race (the race menu simply reports no change).

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read (may be null) |
| `oldRace()` | `PlayerRace` | read |
| `newRace()` | `PlayerRace` | read |
| `adminOverride()` | `boolean` | read |

### `PreSkillUnlockEvent`

```java
CultivationEvents.onPreSkillUnlock(event -> { /* ... */ });
```

A player is about to unlock a skill tree node. Cancel to refuse it (their points are not spent); adjust `setPointCost` to change the price.

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read (may be null) |
| `node()` | `SkillNode` | read |
| `pointCost()` | `int` | read |
| `setPointCost(int)` | `void` | re-tune |

### `PreTribulationStrikeEvent`

```java
CultivationEvents.onPreTribulationStrike(event -> { /* ... */ });
```

Tribulation lightning is about to strike a mid-ritual cultivator. Cancel to spare them entirely (no bolt, no thunder, no damage); set `setDamage` to 0 to let the bolt fall harmlessly. The damage here is pre-armor/reduction.

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read (may be null) |
| `damage()` | `float` | read |
| `breakthroughRitual()` | `boolean` | read |
| `setDamage(float)` | `void` | re-tune |

### `PreTribulationOmenEvent`

```java
CultivationEvents.onPreTribulationOmen(event -> { /* ... */ });
```

A ritual's Storm Omen roll is about to be decided, at its justStarted tick. Cancel to force it to NONE regardless of what the sky rolled (no message, no latch, the opt-in request is left armed); adjust `setStorm` to force the outcome either way.

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read (may be null) |
| `naturalStormDetected()` | `boolean` | read |
| `storm()` | `boolean` | read |
| `breakthroughRitual()` | `boolean` | read |
| `setStorm(boolean)` | `void` | re-tune |

### `PreLifeBoundLevelUpEvent`

```java
CultivationEvents.onPreLifeBoundLevelUp(event -> { /* ... */ });
```

A Life-Bound Treasure is about to level up. Cancel to hold it at its current level (the XP is still banked).

| Member | Type | |
| --- | --- | --- |
| `owner()` | `PlayerRef` | read |
| `item()` | `ItemStack` | read |
| `oldLevel()` | `int` | read |
| `newLevel()` | `int` | read |

### `PreHeartDevilTrialEvent`

```java
CultivationEvents.onPreHeartDevilTrial(event -> { /* ... */ });
```

A Heart-Devil pulse is about to torment a mid-ritual cultivator. Cancel to skip the pulse entirely; adjust `setComposureDrain` to change how hard it bites (0 makes the apparition purely cosmetic).

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read (may be null) |
| `composureDrain()` | `float` | read |
| `leanFraction()` | `float` | read |
| `pulseIndex()` | `int` | read |
| `breakthroughRitual()` | `boolean` | read |
| `setComposureDrain(float)` | `void` | re-tune |

### `PreDreamTrialEvent`

```java
CultivationEvents.onPreDreamTrial(event -> { /* ... */ });
```

A Dream Trial pulse is about to test a cultivator inside the Hollow Mirror. Cancel to skip the pulse entirely; adjust `setComposureDrain` to change how hard it bites (0 makes the reflection purely cosmetic).

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read (may be null) |
| `composureDrain()` | `float` | read |
| `pressure()` | `float` | read |
| `pulseIndex()` | `int` | read |
| `setComposureDrain(float)` | `void` | re-tune |

### `PreInnerDemonTrialEvent`

```java
CultivationEvents.onPreInnerDemonTrial(event -> { /* ... */ });
```

An Inner Demon Rival Duel pulse is about to strike a mid-duel cultivator. Cancel to skip the pulse entirely; adjust `setComposureDrain` to change how hard it bites (0 makes the apparition purely cosmetic).

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read (may be null) |
| `composureDrain()` | `float` | read |
| `echoIntensity()` | `float` | read |
| `pulseIndex()` | `int` | read |
| `nemesisEcho()` | `boolean` | read |
| `setComposureDrain(float)` | `void` | re-tune |

### `PreCleanseRiteEvent`

```java
CultivationEvents.onPreCleanseRite(event -> { /* ... */ });
```

A Marrow-Cleansing Rite pulse is about to test a mid-rite cultivator. Cancel to skip the pulse entirely; adjust `setComposureDrain` to change how hard it bites (0 makes the pulse purely cosmetic). This is where an addon (e.g. a Meridian Injury raising composure-drain multipliers) recomputes the combined drain for THIS rite.

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read (may be null) |
| `composureDrain()` | `float` | read |
| `targetMagnitude()` | `float` | read |
| `pulseIndex()` | `int` | read |
| `companionPresent()` | `boolean` | read |
| `setComposureDrain(float)` | `void` | re-tune |

### `PreQiGainEvent`

```java
CultivationEvents.onPreQiGain(event -> { /* ... */ });
```

Qi is about to be banked toward a player's next rank-up. Cancel to deny the gain; adjust `setAmount` to re-scale it. Fires for EVERY Qi source (meditation ticks, duel payouts, admin grants), after all of the mod's own multipliers.

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read (may be null) |
| `amount()` | `float` | read |
| `baseAmount()` | `float` | read |
| `setAmount(float)` | `void` | re-tune |

### `PreMeditationStartEvent`

```java
CultivationEvents.onPreMeditationStart(event -> { /* ... */ });
```

A player is about to sit down to meditate. Cancel to keep them on their feet.

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read (may be null) |

### `PreMeditationStopEvent`

```java
CultivationEvents.onPreMeditationStop(event -> { /* ... */ });
```

A player is about to stop meditating. Cancel to keep them seated - useful to make a ritual truly unbreakable, or to suppress the movement-cancel.

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read (may be null) |
| `reason()` | `MeditationStopReason` | read |

### `PreAscensionEvent`

```java
CultivationEvents.onPreAscension(event -> { /* ... */ });
```

A cultivator is about to begin the Ascension Capstone. Cancelling keeps them at the peak untried - the one hook a server needs to gate the ladder's ending behind something of its own (a quest, an item, a date).

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read (may be null) |
| `prestiged()` | `boolean` | read |

### `PreReincarnationEvent`

```java
CultivationEvents.onPreReincarnation(event -> { /* ... */ });
```

A cultivator is about to begin Reincarnation. Cancelling keeps them at their current realm untried - the one hook a server needs to gate this ending behind something of its own (a quest, an item, a date), the same role `PreAscensionEvent` plays for the other capstone.

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read (may be null) |

### `PreRitualStartEvent`

```java
CultivationEvents.onPreRitualStart(event -> { /* ... */ });
```

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read (may be null) |
| `type()` | `RitualType` | read |
| `requiredSeconds()` | `float` | read |
| `setRequiredSeconds(float)` | `void` | re-tune |

### `PreDemotionEvent`

```java
CultivationEvents.onPreDemotion(event -> { /* ... */ });
```

A player is about to be demoted a sub-stage for abandoning a ritual. Cancel to let them walk away free (their banked Qi survives too).

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read (may be null) |
| `realm()` | `CultivationRealm` | read |
| `oldStage()` | `CultivationStage` | read |
| `newStage()` | `CultivationStage` | read |
| `wasBreakthrough()` | `boolean` | read |

### `PreQiDeviationEvent`

```java
CultivationEvents.onPreQiDeviation(event -> { /* ... */ });
```

A cultivator's composure has shattered and Qi Deviation is about to be applied. Cancel to spare them the penalty (the ritual still ends); flip `setDemotes` or re-scale `setQiLoss` to change which penalty lands.

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read (may be null) |
| `demotes()` | `boolean` | read |
| `qiLoss()` | `float` | read |
| `breakthroughRitual()` | `boolean` | read |
| `setDemotes(boolean)` | `void` | re-tune |
| `setQiLoss(float)` | `void` | re-tune |

### `PreRespecEvent`

```java
CultivationEvents.onPreRespec(event -> { /* ... */ });
```

A player is about to respec their skill tree. Cancel to refuse; adjust `setRefundedPoints` to change how many points come back.

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read (may be null) |
| `refundedPoints()` | `int` | read |
| `setRefundedPoints(int)` | `void` | re-tune |


---

## Lifespan (0.10.2)

`plugin.siren.API.LifespanEvents` — Lifespan (寿元), the per-profile online-play-hour budget keyed to the highest realm ever reached: extending it, withering, restoring and expiring. Extend and expire are cancellable. Default-off on the server.

**Enums declared here**

- `LifespanEvents.ExtendSource` — Where an `LifespanExtendEvent`/`PreLifespanExtendEvent` came from. Values: `BREAKTHROUGH`, `PILL`, `ADMIN`

**Post-events** — fired once the change is committed; cannot be cancelled.

### `LifespanExtendEvent`

```java
LifespanEvents.onExtend(event -> { /* ... */ });
```

Bonus hours were actually banked (or, for `ExtendSource#BREAKTHROUGH`, the realm-mark budget rose). `hours` is what actually applied, after any `PreLifespanExtendEvent` scaling.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `hours()` | `float` |
| `source()` | `ExtendSource` |

### `LifespanWitherEvent`

```java
LifespanEvents.onWither(event -> { /* ... */ });
```

A cultivator's Lifespan budget hit 0 and they entered the Withering grace state.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |

### `LifespanRestoreEvent`

```java
LifespanEvents.onRestore(event -> { /* ... */ });
```

A cultivator's Lifespan clock was reset to a fresh budget - a real Reincarnation, a real Ascension, `LifespanConfig.ExpiryAction#NOTHING` clearing Withering, or Mortal Passing's own reset (which additionally fires `LifespanExpireEvent`).

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |

### `LifespanExpireEvent`

```java
LifespanEvents.onExpire(event -> { /* ... */ });
```

A cultivator's Withering grace ran out and `Lifespan-Expiry-Action` is about to run. NOT cancellable through this event - by the time this fires the grace period has already elapsed; use `PreLifespanExpireEvent` to veto Withering itself, further upstream.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `action()` | `LifespanConfig.ExpiryAction` |

**Pre-events** — fired before the change; `setCancelled(true)` vetoes it, and any setter below re-tunes the numbers the mod then uses.

### `PreLifespanExtendEvent`

```java
LifespanEvents.onPreExtend(event -> { /* ... */ });
```

About to grant bonus hours (`ExtendSource#PILL`/`ExtendSource#ADMIN` only - a `ExtendSource#BREAKTHROUGH` realm-mark rise has already happened by the time Lifespan notices it and fires only the post event). Cancel to refuse the grant outright, or scale `setHours` to change how much is actually banked.

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read (may be null) |
| `source()` | `ExtendSource` | read |
| `hours()` | `float` | read |
| `setHours(float)` | `void` | re-tune |

### `PreLifespanExpireEvent`

```java
LifespanEvents.onPreExpire(event -> { /* ... */ });
```

A cultivator's Lifespan budget is about to reach 0 and enter Withering. Cancelling here vetoes Withering entirely for this crossing - the clock stays at 0 and this fires again the next tick, so an addon meaning to grant a reprieve should also extend the budget (or it will simply be asked again immediately).

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read (may be null) |


---

## Legacy (0.10.1)

`plugin.siren.API.LegacyEvents` — A retiring cultivator's breakthrough-cost-reduction buff reaching the chosen heir. The payout is cancellable.

**Post-events** — fired once the change is committed; cannot be cancelled.

### `LegacyPayoutEvent`

```java
LegacyEvents.onLegacyPayout(event -> { /* ... */ });
```

A legacy buff has actually reached (or been queued for) an heir - the reduction described here is the FINAL amount, after the recent-profile scale-down for an immediate payout, or the pre-scale base amount for a queued one (see `queued`). @param queued true if this was deferred to the offline-heir mailbox rather than applied immediately - the reduction/expiry here are the values as queued, not yet scaled for profile age; that scaling only happens at actual delivery and is not separately observable through this event.

| Accessor | Type |
| --- | --- |
| `benefactorUuid()` | `UUID` |
| `benefactorName()` | `String` |
| `heirUuid()` | `UUID` |
| `reductionPercent()` | `float` |
| `expiresAtMillis()` | `long` |
| `sourceProfileName()` | `String` |
| `queued()` | `boolean` |

**Pre-events** — fired before the change; `setCancelled(true)` vetoes it, and any setter below re-tunes the numbers the mod then uses.

### `PreLegacyPayoutEvent`

```java
LegacyEvents.onPreLegacyPayout(event -> { /* ... */ });
```

A legacy buff is about to be granted or queued. Cancel to refuse it outright - nothing has been written or queued yet. A listener may also rescale `setReductionPercent` / `setExpiresAtMillis` before dispatch finishes; whatever is left in those fields is what actually gets granted or queued.

| Member | Type | |
| --- | --- | --- |
| `benefactorUuid()` | `UUID` | read |
| `benefactorName()` | `String` | read |
| `heirUuid()` | `UUID` | read |
| `retiringRealmOrdinal()` | `int` | read |
| `sourceProfileName()` | `String` | read |
| `reductionPercent()` | `float` | read |
| `expiresAtMillis()` | `long` | read |
| `setReductionPercent(float)` | `void` | re-tune |
| `setExpiresAtMillis(long)` | `void` | re-tune |


---

## Nascent Soul Escape (0.10.2)

`plugin.siren.API.SoulEscapeEvents` — The fatal-blow reprieve (元婴遁走), the chase, and its four resolutions: extinguished, survived, timed out, forfeited. Players are UUIDs, since the killer may be offline by the time the session resolves.

**Enums declared here**

- `SoulEscapeEvents.SanctuaryKind` — Mirrors `SoulSanctuary.Kind` - kept as its own enum here so this event never has to import Utils.SoulEscape just to name a sanctuary. Values: 
- `SoulEscapeEvents.ForfeitReason` — How a fleeing soul's window ended without ever resolving into a survive/extinguish/timeout. Values: 

**Post-events** — fired once the change is committed; cannot be cancelled.

### `SoulEscapeBeginEvent`

```java
SoulEscapeEvents.onSoulEscapeBegin(event -> { /* ... */ });
```

| Accessor | Type |
| --- | --- |
| `soul()` | `UUID` |
| `killer()` | `UUID` |
| `realm()` | `CultivationRealm` |
| `windowSeconds()` | `float` |
| `qiSpent()` | `float` |
| `lifespanHoursSpent()` | `float` |

### `SoulEscapeSurviveEvent`

```java
SoulEscapeEvents.onSoulEscapeSurvive(event -> { /* ... */ });
```

| Accessor | Type |
| --- | --- |
| `soul()` | `UUID` |
| `killer()` | `UUID` |
| `kind()` | `SanctuaryKind` |
| `world()` | `String` |
| `chunkX()` | `int` |
| `chunkZ()` | `int` |
| `weakenedMinutes()` | `float` |
| `meritToKiller()` | `float` |

### `SoulEscapeExtinguishEvent`

```java
SoulEscapeEvents.onSoulEscapeExtinguish(event -> { /* ... */ });
```

| Accessor | Type |
| --- | --- |
| `soul()` | `UUID` |
| `extinguisher()` | `UUID` |
| `extraKarma()` | `float` |
| `extraYinShift()` | `float` |

### `SoulEscapeTimeoutEvent`

```java
SoulEscapeEvents.onSoulEscapeTimeout(event -> { /* ... */ });
```

| Accessor | Type |
| --- | --- |
| `soul()` | `UUID` |
| `killer()` | `UUID` |

### `SoulEscapeForfeitEvent`

```java
SoulEscapeEvents.onSoulEscapeForfeit(event -> { /* ... */ });
```

| Accessor | Type |
| --- | --- |
| `soul()` | `UUID` |
| `killer()` | `UUID` |
| `reason()` | `ForfeitReason` |

**Pre-events** — fired before the change; `setCancelled(true)` vetoes it, and any setter below re-tunes the numbers the mod then uses.

### `PreSoulEscapeBeginEvent`

```java
SoulEscapeEvents.onPreSoulEscapeBegin(event -> { /* ... */ });
```

The fatal blow is about to be cancelled and the soul about to start fleeing. Cancel -> no escape, nothing charged, the blow stays fatal.

| Member | Type | |
| --- | --- | --- |
| `soul()` | `UUID` | read |
| `killer()` | `UUID` | read (may be null) |
| `realm()` | `CultivationRealm` | read |
| `windowSeconds()` | `float` | read |
| `qiCostPercent()` | `float` | read |
| `lifespanHours()` | `float` | read |
| `setWindowSeconds(float)` | `void` | re-tune |
| `setQiCostPercent(float)` | `void` | re-tune |
| `setLifespanHours(float)` | `void` | re-tune |

### `PreSoulExtinguishEvent`

```java
SoulEscapeEvents.onPreSoulExtinguish(event -> { /* ... */ });
```

A blow just landed on a fleeing soul. Cancel -> this blow does nothing; the soul keeps fleeing.

| Member | Type | |
| --- | --- | --- |
| `soul()` | `UUID` | read |
| `extinguisher()` | `UUID` | read |
| `hitsSoFar()` | `int` | read |
| `hitsRequired()` | `int` | read |
| `extraKarma()` | `float` | read |
| `extraYinShift()` | `float` | read |
| `setExtraKarma(float)` | `void` | re-tune |
| `setExtraYinShift(float)` | `void` | re-tune |

### `PreSoulSurviveEvent`

```java
SoulEscapeEvents.onPreSoulSurvive(event -> { /* ... */ });
```

A fleeing soul just entered sanctuary. Cancel -> the sanctuary does not count; the session is re-inserted and the soul keeps fleeing.

| Member | Type | |
| --- | --- | --- |
| `soul()` | `UUID` | read |
| `killer()` | `UUID` | read (may be null) |
| `kind()` | `SanctuaryKind` | read |
| `world()` | `String` | read |
| `chunkX()` | `int` | read |
| `chunkZ()` | `int` | read |
| `restoreHealthPercent()` | `float` | read |
| `weakenedMinutes()` | `float` | read |
| `spareMerit()` | `float` | read |
| `setRestoreHealthPercent(float)` | `void` | re-tune |
| `setWeakenedMinutes(float)` | `void` | re-tune |
| `setSpareMerit(float)` | `void` | re-tune |


---

## Dao, alignment and karma

`plugin.siren.API.DaoEvents` — Elemental daos, affinity drift, Yin-Yang lean, the Righteous/Devil path split, karma and Devil-path Qi harvesting.

**Enums declared here**

- `DaoEvents.ElementChangeReason` — Why a cultivator's element changed. Values: `CHOSEN`, `DRIFT`

**Post-events** — fired once the change is committed; cannot be cancelled.

### `DaoElementChangeEvent`

```java
DaoEvents.onDaoElementChange(event -> { /* ... */ });
```

A cultivator's elemental dao changed. `oldElement` is null on their very first choice.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `dao()` | `DaoComponent` |
| `oldElement()` | `DaoElement` |
| `newElement()` | `DaoElement` |
| `reason()` | `ElementChangeReason` |
| `qiCost()` | `float` |

### `DaoAffinityGainEvent`

```java
DaoEvents.onDaoAffinityGain(event -> { /* ... */ });
```

Deed affinity was added toward an element - the pressure that eventually causes drift.

| Accessor | Type |
| --- | --- |
| `player()` | `PlayerRef` |
| `dao()` | `DaoComponent` |
| `element()` | `DaoElement` |
| `amount()` | `float` |

### `DaoDriftWarningEvent`

```java
DaoEvents.onDaoDriftWarning(event -> { /* ... */ });
```

A cultivator was warned their dao is drifting toward another element. Fires once per newly-threatening element.

| Accessor | Type |
| --- | --- |
| `player()` | `PlayerRef` |
| `dao()` | `DaoComponent` |
| `chosenElement()` | `DaoElement` |
| `driftingTo()` | `DaoElement` |

### `AlignmentShiftEvent`

```java
DaoEvents.onAlignmentShift(event -> { /* ... */ });
```

A cultivator's Yin-Yang balance moved. `yin`/`yang` are the amounts actually added after race bias split the shift.

| Accessor | Type |
| --- | --- |
| `dao()` | `DaoComponent` |
| `yin()` | `float` |
| `yang()` | `float` |

### `PathChangeEvent`

```java
DaoEvents.onPathChange(event -> { /* ... */ });
```

A cultivator's moral path changed (and was announced to them).

| Accessor | Type |
| --- | --- |
| `player()` | `PlayerRef` |
| `dao()` | `DaoComponent` |
| `oldPath()` | `CultivationPath` |
| `newPath()` | `CultivationPath` |

### `KarmaGainEvent`

```java
DaoEvents.onKarmaGain(event -> { /* ... */ });
```

Karma was charged for a kill. `total` is the ledger after the charge and the Karma-Max cap.

| Accessor | Type |
| --- | --- |
| `player()` | `PlayerRef` |
| `dao()` | `DaoComponent` |
| `amount()` | `float` |
| `total()` | `float` |
| `farmedKill()` | `boolean` |

### `KarmaClearedEvent`

```java
DaoEvents.onKarmaCleared(event -> { /* ... */ });
```

Karma was worked off - by enduring a tribulation strike, or by the wall-clock decay of simply not killing anyone.

| Accessor | Type |
| --- | --- |
| `dao()` | `DaoComponent` |
| `amount()` | `float` |
| `total()` | `float` |
| `fromTribulation()` | `boolean` |

### `DevilHarvestEvent`

```java
DaoEvents.onDevilHarvest(event -> { /* ... */ });
```

A Devil-path cultivator harvested banked Qi from slaying another player.

| Accessor | Type |
| --- | --- |
| `killer()` | `Ref<EntityStore>` |
| `killerPlayer()` | `PlayerRef` |
| `qi()` | `float` |

**Pre-events** — fired before the change; `setCancelled(true)` vetoes it, and any setter below re-tunes the numbers the mod then uses.

### `PreDaoElementChangeEvent`

```java
DaoEvents.onPreDaoElementChange(event -> { /* ... */ });
```

A cultivator is about to take (or switch to) an element. Cancel to refuse it (reported as an unchanged dao); `setQiCost` to re-price the switch - it is charged after this, so a listener can make switching free or ruinous.

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read (may be null) |
| `player()` | `PlayerRef` | read (may be null) |
| `dao()` | `DaoComponent` | read |
| `oldElement()` | `DaoElement` | read (may be null) |
| `newElement()` | `DaoElement` | read |
| `reason()` | `ElementChangeReason` | read |
| `qiCost()` | `float` | read |
| `setNewElement(DaoElement)` | `void` | re-tune |
| `setQiCost(float)` | `void` | re-tune |

### `PreDaoAffinityGainEvent`

```java
DaoEvents.onPreDaoAffinityGain(event -> { /* ... */ });
```

Deed affinity is about to be added. Cancel to deny it; `setAmount` to re-scale how fast this element pulls at them.

| Member | Type | |
| --- | --- | --- |
| `player()` | `PlayerRef` | read (may be null) |
| `dao()` | `DaoComponent` | read |
| `element()` | `DaoElement` | read |
| `amount()` | `float` | read |
| `setAmount(float)` | `void` | re-tune |

### `PreAlignmentShiftEvent`

```java
DaoEvents.onPreAlignmentShift(event -> { /* ... */ });
```

A Yin-Yang shift is about to be applied. Cancel to deny it; `setAmount` to re-scale. The race-bias split happens after this.

| Member | Type | |
| --- | --- | --- |
| `dao()` | `DaoComponent` | read |
| `amount()` | `float` | read |
| `towardYin()` | `boolean` | read |
| `setAmount(float)` | `void` | re-tune |

### `PrePathChangeEvent`

```java
DaoEvents.onPrePathChange(event -> { /* ... */ });
```

A cultivator's moral path is about to change. Cancel to leave them on their current path - the underlying balance is untouched, so this only suppresses the reclassification.

| Member | Type | |
| --- | --- | --- |
| `player()` | `PlayerRef` | read (may be null) |
| `dao()` | `DaoComponent` | read |
| `oldPath()` | `CultivationPath` | read |
| `newPath()` | `CultivationPath` | read |

### `PreKarmaGainEvent`

```java
DaoEvents.onPreKarmaGain(event -> { /* ... */ });
```

Karma is about to be charged for a kill. Cancel to leave the ledger clean; `setAmount` to re-weigh what this life cost.

| Member | Type | |
| --- | --- | --- |
| `player()` | `PlayerRef` | read (may be null) |
| `dao()` | `DaoComponent` | read |
| `amount()` | `float` | read |
| `farmedKill()` | `boolean` | read |
| `setAmount(float)` | `void` | re-tune |

### `PreDevilHarvestEvent`

```java
DaoEvents.onPreDevilHarvest(event -> { /* ... */ });
```

A Devil-path cultivator is about to harvest Qi from a slain player. Cancel to deny the harvest; `setQi` to re-scale it.

| Member | Type | |
| --- | --- | --- |
| `killer()` | `Ref<EntityStore>` | read |
| `killerPlayer()` | `PlayerRef` | read |
| `qi()` | `float` | read |
| `setQi(float)` | `void` | re-tune |


---

## Dao comprehension (0.9.x)

`plugin.siren.API.DaoComprehensionEvents` — The layer on top of the Elemental Dao: the Heavenly Dao (天道) understanding track, the open Personal Dao registry (Sword/Slaughter/Space and whatever a mod adds beside them), and Dao Enlightenment (悟道). Does not replace or collide with `DaoEvents` above.

**Post-events** — fired once the change is committed; cannot be cancelled.

### `HeavenlyDaoGainEvent`

```java
DaoComprehensionEvents.onHeavenlyDaoGain(event -> { /* ... */ });
```

The Heavenly Dao track advanced. `amount` is what was actually applied (after any listener re-scaled it); `total` is the value after.

| Accessor | Type |
| --- | --- |
| `player()` | `PlayerRef` |
| `comprehension()` | `DaoComprehensionComponent` |
| `amount()` | `float` |
| `total()` | `float` |

### `HeavenlyDaoRankChangeEvent`

```java
DaoComprehensionEvents.onHeavenlyDaoRankChange(event -> { /* ... */ });
```

The player's HeavenlyDaoRank changed (and was announced to them).

| Accessor | Type |
| --- | --- |
| `player()` | `PlayerRef` |
| `comprehension()` | `DaoComprehensionComponent` |
| `oldRank()` | `HeavenlyDaoRank` |
| `newRank()` | `HeavenlyDaoRank` |

### `PersonalDaoComprehensionEvent`

```java
DaoComprehensionEvents.onPersonalDaoComprehension(event -> { /* ... */ });
```

A Personal Dao's comprehension advanced. `amount` is what was actually applied; `total` is the value after.

| Accessor | Type |
| --- | --- |
| `player()` | `PlayerRef` |
| `comprehension()` | `DaoComprehensionComponent` |
| `dao()` | `PersonalDao` |
| `amount()` | `float` |
| `total()` | `float` |

### `PersonalDaoManifestEvent`

```java
DaoComprehensionEvents.onPersonalDaoManifest(event -> { /* ... */ });
```

A Personal Dao manifested for this player.

| Accessor | Type |
| --- | --- |
| `player()` | `PlayerRef` |
| `comprehension()` | `DaoComprehensionComponent` |
| `dao()` | `PersonalDao` |

### `PersonalDaoSetAsideEvent`

```java
DaoComprehensionEvents.onPersonalDaoSetAside(event -> { /* ... */ });
```

A manifested Personal Dao was set aside.

| Accessor | Type |
| --- | --- |
| `player()` | `PlayerRef` |
| `comprehension()` | `DaoComprehensionComponent` |
| `dao()` | `PersonalDao` |

### `DaoEnlightenmentEvent`

```java
DaoComprehensionEvents.onDaoEnlightenment(event -> { /* ... */ });
```

A Dao Enlightenment (悟道) fired. `subject` is whichever Heavenly/Personal Dao triggered it.

| Accessor | Type |
| --- | --- |
| `player()` | `PlayerRef` |
| `comprehension()` | `DaoComprehensionComponent` |
| `subject()` | `DaoComprehensionManager.Subject` |
| `comprehensionGain()` | `float` |
| `qiGain()` | `float` |

**Pre-events** — fired before the change; `setCancelled(true)` vetoes it, and any setter below re-tunes the numbers the mod then uses.

### `PreHeavenlyDaoGainEvent`

```java
DaoComprehensionEvents.onPreHeavenlyDaoGain(event -> { /* ... */ });
```

The Heavenly Dao track is about to advance. Cancel to refuse it; `setAmount` to re-scale.

| Member | Type | |
| --- | --- | --- |
| `player()` | `PlayerRef` | read (may be null) |
| `comprehension()` | `DaoComprehensionComponent` | read |
| `amount()` | `float` | read |
| `setAmount(float)` | `void` | re-tune |

### `PreHeavenlyDaoRankChangeEvent`

```java
DaoComprehensionEvents.onPreHeavenlyDaoRankChange(event -> { /* ... */ });
```

The player's HeavenlyDaoRank is about to change. Cancel to leave them on their current rank - the underlying value is untouched, so this only suppresses the reclassification/announcement.

| Member | Type | |
| --- | --- | --- |
| `player()` | `PlayerRef` | read (may be null) |
| `comprehension()` | `DaoComprehensionComponent` | read |
| `oldRank()` | `HeavenlyDaoRank` | read |
| `newRank()` | `HeavenlyDaoRank` | read |

### `PrePersonalDaoComprehensionEvent`

```java
DaoComprehensionEvents.onPrePersonalDaoComprehension(event -> { /* ... */ });
```

A Personal Dao's comprehension is about to advance. Cancel to refuse it; `setAmount` to re-scale.

| Member | Type | |
| --- | --- | --- |
| `player()` | `PlayerRef` | read (may be null) |
| `comprehension()` | `DaoComprehensionComponent` | read |
| `dao()` | `PersonalDao` | read |
| `amount()` | `float` | read |
| `setAmount(float)` | `void` | re-tune |

### `PrePersonalDaoManifestEvent`

```java
DaoComprehensionEvents.onPrePersonalDaoManifest(event -> { /* ... */ });
```

A Personal Dao is about to manifest. Cancel to leave it comprehended but unmanifested.

| Member | Type | |
| --- | --- | --- |
| `player()` | `PlayerRef` | read (may be null) |
| `comprehension()` | `DaoComprehensionComponent` | read |
| `dao()` | `PersonalDao` | read |

### `PreDaoEnlightenmentEvent`

```java
DaoComprehensionEvents.onPreDaoEnlightenment(event -> { /* ... */ });
```

A Dao Enlightenment is about to fire. Cancel to refuse it (as if the roll never happened); `PreDaoEnlightenmentEvent#setComprehensionGain`/ `PreDaoEnlightenmentEvent#setQiGain` to re-scale the reward. **`setQiGain` can lower the Qi burst but cannot raise it past the server's absolute cap.** `Dao-Enlightenment-Qi-Max-Base` x `Dao-Enlightenment-Qi-Max-Growth-Per-Realm ^ realmIndex` is applied both before this event is fired and again immediately after `qiGain()` is read back, so it is a hard rail rather than a default - a listener that sets 10,000,000 on a Qi Gathering cultivator still grants the cap. This is deliberate: an enlightenment is the mod's largest one-shot Qi reward and an unbounded one reads to a player as a bug, not a blessing. A server that genuinely wants no ceiling sets `Dao-Enlightenment-Qi-Max-Base` to 0, which is the operator's decision to make, not a listener's. `setComprehensionGain` is not capped this way - comprehension is clamped to the subject's own `getMaxComprehension()` downstream.

| Member | Type | |
| --- | --- | --- |
| `player()` | `PlayerRef` | read (may be null) |
| `comprehension()` | `DaoComprehensionComponent` | read |
| `subject()` | `DaoComprehensionManager.Subject` | read |
| `comprehensionGain()` | `float` | read |
| `qiGain()` | `float` | read |
| `setComprehensionGain(float)` | `void` | re-tune |
| `setQiGain(float)` | `void` | re-tune |


---

## Techniques

`plugin.siren.API.TechniqueEvents` — Performing and learning arts, fusing two into a third, Sword Flying, and the timed combat buffs.

**Enums declared here**

- `TechniqueEvents.BuffType` — Which timed buff a technique granted. Values: `IRON_BODY`, `QI_INFUSION`, `QI_BARRIER`, `CLOUD_STEP`
- `TechniqueEvents.FlightStopReason` — Why a cultivator came down from sword flight. Values: `TOGGLE`, `QI_EXHAUSTED`, `DEATH`

**Post-events** — fired once the change is committed; cannot be cancelled.

### `TechniquePerformEvent`

```java
TechniqueEvents.onTechniquePerform(event -> { /* ... */ });
```

A technique was performed: the Qi is spent, the cooldown stamped, and the effect has run.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `technique()` | `Technique` |
| `qiCost()` | `float` |

### `TechniqueLearnEvent`

```java
TechniqueEvents.onTechniqueLearn(event -> { /* ... */ });
```

A cultivator learned a technique for good (from a manual). Sect-taught arts are resolved live and never fire this - listen for `SectEvents`' inscription events instead.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `techniqueId()` | `String` |

### `SwordFlightStartEvent`

```java
TechniqueEvents.onSwordFlightStart(event -> { /* ... */ });
```

A cultivator took to the sky on their sword.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `horizontalSpeed()` | `float` |
| `verticalSpeed()` | `float` |

### `SwordFlightStopEvent`

```java
TechniqueEvents.onSwordFlightStop(event -> { /* ... */ });
```

A cultivator came down; their mount (if any) has already despawned.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `reason()` | `FlightStopReason` |

### `TechniqueBuffApplyEvent`

```java
TechniqueEvents.onTechniqueBuffApply(event -> { /* ... */ });
```

A timed technique buff was applied. `magnitude` means whatever that buff measures - a reduction percent, a damage percent, a shield pool, a speed multiplier.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `type()` | `BuffType` |
| `durationSeconds()` | `float` |
| `magnitude()` | `float` |

### `TechniqueFusionEvent`

```java
TechniqueEvents.onTechniqueFusion(event -> { /* ... */ });
```

A Technique Fusion ritual succeeded: `fusionId` names the recipe, `resultTechniqueId` the art just granted. Whether the two parents were consumed is not carried here - listen for the ordinary `TechniqueLearnEvent` the grant fires, and (if the recipe consumed them) two separate learned-set removals happened via `TechniqueUnlockManager.revoke`, which does not fire an event of its own (nothing in play ever removes knowledge except this and an admin tool).

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `fusionId()` | `String` |
| `resultTechniqueId()` | `String` |

### `TechniqueBuffExpireEvent`

```java
TechniqueEvents.onTechniqueBuffExpire(event -> { /* ... */ });
```

Cloud Step's speed multiplier was reverted, either on expiry or on cleanup.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `type()` | `BuffType` |

### `TechniqueMasteryAdvanceEvent`

```java
TechniqueEvents.onTechniqueMasteryAdvance(event -> { /* ... */ });
```

An art's mastery rose a rung.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `techniqueId()` | `String` |
| `stage()` | `int` |

**Pre-events** — fired before the change; `setCancelled(true)` vetoes it, and any setter below re-tunes the numbers the mod then uses.

### `PreTechniquePerformEvent`

```java
TechniqueEvents.onPreTechniquePerform(event -> { /* ... */ });
```

A technique is about to be performed - every gate has already passed. Cancel to refuse it silently (no Qi spent, no cooldown); `setQiCost` and `setCooldownSeconds` re-price this one performance without touching the config.

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read |
| `technique()` | `Technique` | read |
| `qiCost()` | `float` | read |
| `cooldownSeconds()` | `float` | read |
| `setQiCost(float)` | `void` | re-tune |
| `setCooldownSeconds(float)` | `void` | re-tune |

### `PreTechniqueLearnEvent`

```java
TechniqueEvents.onPreTechniqueLearn(event -> { /* ... */ });
```

A technique is about to be learned. Cancel to refuse it (the manual is consumed either way, matching how a manual for an already-known art is spent).

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read (may be null) |
| `techniqueId()` | `String` | read |

### `PreSwordFlightStartEvent`

```java
TechniqueEvents.onPreSwordFlightStart(event -> { /* ... */ });
```

A cultivator is about to take flight. Cancel to keep them grounded; the speed setters re-tune how fast this flight is.

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read |
| `horizontalSpeed()` | `float` | read |
| `verticalSpeed()` | `float` | read |
| `setHorizontalSpeed(float)` | `void` | re-tune |
| `setVerticalSpeed(float)` | `void` | re-tune |

### `PreSwordFlightStopEvent`

```java
TechniqueEvents.onPreSwordFlightStop(event -> { /* ... */ });
```

A cultivator is about to come down. Cancel to keep them airborne - safe for TOGGLE, but cancelling a DEATH stop leaves flight state on a corpse, so gate on `reason()`.

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read |
| `reason()` | `FlightStopReason` | read |

### `PreTechniqueBuffApplyEvent`

```java
TechniqueEvents.onPreTechniqueBuffApply(event -> { /* ... */ });
```

A timed technique buff is about to be applied. Cancel to deny it; the setters re-tune how long and how strong it is.

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read (may be null) |
| `type()` | `BuffType` | read |
| `durationSeconds()` | `float` | read |
| `magnitude()` | `float` | read |
| `setDurationSeconds(float)` | `void` | re-tune |
| `setMagnitude(float)` | `void` | re-tune |

### `PreTechniqueFusionEvent`

```java
TechniqueEvents.onPreTechniqueFusion(event -> { /* ... */ });
```

A fusion ritual is about to run - every gate (both parents mastered, the realm floor, the Qi, the cooldown) has already passed. Cancel to refuse it silently (no Qi spent, no cooldown, neither parent touched); `setQiCost`, `setConsumeParents` and `setFailureChancePercent` re-tune this one attempt without touching `TechniqueConfig.json`.

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read (may be null) |
| `fusionId()` | `String` | read |
| `resultTechniqueId()` | `String` | read |
| `qiCost()` | `float` | read |
| `consumeParents()` | `boolean` | read |
| `failureChancePercent()` | `float` | read |
| `setQiCost(float)` | `void` | re-tune |
| `setConsumeParents(boolean)` | `void` | re-tune |
| `setFailureChancePercent(float)` | `void` | re-tune |

### `PreTechniqueMasteryAdvanceEvent`

```java
TechniqueEvents.onPreTechniqueMasteryAdvance(event -> { /* ... */ });
```

An art is about to rise a rung. Cancel to hold it where it is - the XP and the studied manuals stay banked, so it simply tries again the next time anything about it changes.

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read (may be null) |
| `techniqueId()` | `String` | read |
| `stage()` | `int` | read |


---

## Items, alchemy and refinement

`plugin.siren.API.ItemEvents` — Loot drops, pills, spirit cores, manuals and weapon refinement.

**Enums declared here**

- `ItemEvents.LootType` — What a cultivation drop is. Values: `CULTIVATION_CORE`, `BEAST_EGG`, `SPIRIT_STONE`, `MANUAL`, `TREASURE_MATERIAL`
- `ItemEvents.RefinementOutcome` — How a refinement attempt resolved. Values: `SUCCESS`, `DESTROYED`, `DEMOTED`, `FAILED`

**Post-events** — fired once the change is committed; cannot be cancelled.

### `LootDropEvent`

```java
ItemEvents.onLootDrop(event -> { /* ... */ });
```

A cultivation drop landed in a player's inventory and was announced. Never fires when the roll missed or the item didn't fit.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `type()` | `LootType` |
| `itemId()` | `String` |

### `ManualReadEvent`

```java
ItemEvents.onManualRead(event -> { /* ... */ });
```

A manual was read and its teaching applied.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `techniqueId()` | `String` |
| `skillNodeId()` | `String` |

### `PillConsumeEvent`

```java
ItemEvents.onPillConsume(event -> { /* ... */ });
```

A spirit pill was consumed and its effect applied. `effect` is the interaction's configured effect id.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `effect()` | `String` |

### `SpiritCoreConsumeEvent`

```java
ItemEvents.onSpiritCoreConsume(event -> { /* ... */ });
```

A cultivation core was absorbed. `qi` is what was actually banked, meditation bonus included.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `qi()` | `float` |

### `RefinementStartEvent`

```java
ItemEvents.onRefinementStart(event -> { /* ... */ });
```

A refinement ritual began; the Qi is already spent and the cultivator seated.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `element()` | `DaoElement` |
| `targetTier()` | `int` |
| `qiCost()` | `float` |

### `RefinementCompleteEvent`

```java
ItemEvents.onRefinementComplete(event -> { /* ... */ });
```

A refinement ritual resolved. `stack` is the weapon as it stands afterward, or null when it was destroyed.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `element()` | `DaoElement` |
| `targetTier()` | `int` |
| `outcome()` | `RefinementOutcome` |
| `stack()` | `ItemStack` |

**Pre-events** — fired before the change; `setCancelled(true)` vetoes it, and any setter below re-tunes the numbers the mod then uses.

### `PreLootDropEvent`

```java
ItemEvents.onPreLootDrop(event -> { /* ... */ });
```

A cultivation drop is about to be handed over. Cancel to deny it; `setItemId` to substitute a different item entirely.

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read (may be null) |
| `type()` | `LootType` | read |
| `itemId()` | `String` | read |
| `setItemId(String)` | `void` | re-tune |

### `PreManualReadEvent`

```java
ItemEvents.onPreManualRead(event -> { /* ... */ });
```

A manual is about to teach. Cancel to refuse it - the manual is consumed either way, matching how one for an already-known art is spent.

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read |
| `techniqueId()` | `String` | read (may be null) |
| `skillNodeId()` | `String` | read (may be null) |

### `PrePillConsumeEvent`

```java
ItemEvents.onPrePillConsume(event -> { /* ... */ });
```

A spirit pill is about to take effect. Cancel to refuse it (the pill is not consumed).

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read |
| `effect()` | `String` | read |

### `PreSpiritCoreConsumeEvent`

```java
ItemEvents.onPreSpiritCoreConsume(event -> { /* ... */ });
```

A cultivation core is about to be absorbed. Cancel to refuse it (the core is not consumed); `setQi` to re-value it.

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read |
| `qi()` | `float` | read |
| `meditating()` | `boolean` | read |
| `setQi(float)` | `void` | re-tune |

### `PreRefinementStartEvent`

```java
ItemEvents.onPreRefinementStart(event -> { /* ... */ });
```

A refinement ritual is about to begin. Cancel to refuse it (no Qi is spent); `setQiCost` to re-price it.

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read |
| `element()` | `DaoElement` | read |
| `targetTier()` | `int` | read |
| `qiCost()` | `float` | read |
| `setQiCost(float)` | `void` | re-tune |

### `PreRefinementCompleteEvent`

```java
ItemEvents.onPreRefinementComplete(event -> { /* ... */ });
```

A refinement ritual is about to resolve. Cancel to abandon it silently (the weapon is untouched; the up-front Qi stays spent); `setSuccessChance` to re-weight the roll - 1 guarantees it, 0 dooms it.

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read |
| `element()` | `DaoElement` | read |
| `targetTier()` | `int` | read |
| `successChance()` | `float` | read |
| `setSuccessChance(float)` | `void` | re-tune |


---

## Alchemy (0.10.0)

`plugin.siren.API.AlchemyEvents` — The Pill Cauldron refining RITUAL - starting, resolving, and the Fire Watch (火候) tending prompts along the way. Complements `ItemEvents`' `PillConsumeEvent` above, which covers drinking a finished pill, not brewing one.

**Enums declared here**

- `AlchemyEvents.RefineOutcome` — How a completed (or interrupted) refining ritual resolved. Values: `SUCCESS`, `FAILED`, `BOTCH`

**Post-events** — fired once the change is committed; cannot be cancelled.

### `RefineStartEvent`

```java
AlchemyEvents.onRefineStart(event -> { /* ... */ });
```

A refining ritual began; the herbs and Qi floor check already passed.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `effect()` | `String` |
| `qiDrainPerSecond()` | `float` |

### `RefineCompleteEvent`

```java
AlchemyEvents.onRefineComplete(event -> { /* ... */ });
```

A refining ritual resolved. `grade` is null unless `outcome` is SUCCESS.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `effect()` | `String` |
| `outcome()` | `RefineOutcome` |
| `grade()` | `PillGrade` |

### `TendPromptResolvedEvent`

```java
AlchemyEvents.onTendPromptResolved(event -> { /* ... */ });
```

A Fire Watch (火候) tending prompt was resolved - answered (sharp/steady), missed, or fumbled - during a running refining ritual. `promptIndex` is 0-based (this is the Nth prompt this ritual delivered); `promptCount` is the ritual's configured `Tend-Prompts-Max` ceiling, not how many have fired so far. Post-only, like `RefineStartEvent`/ `RefineCompleteEvent` above - nothing about a single prompt's resolution is meant to be vetoed or re-weighted from outside; only the ritual-end `tendDelta` it eventually folds into is (see `PreRefineCompleteEvent#tendDelta`).

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `action()` | `AlchemyTendAction` |
| `outcome()` | `AlchemyTendOutcome` |
| `promptIndex()` | `int` |
| `promptCount()` | `int` |

**Pre-events** — fired before the change; `setCancelled(true)` vetoes it, and any setter below re-tunes the numbers the mod then uses.

### `PreRefineStartEvent`

```java
AlchemyEvents.onPreRefineStart(event -> { /* ... */ });
```

A refining ritual is about to begin. Cancel to refuse it (no herbs or Qi are spent).

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read |
| `effect()` | `String` | read |
| `durationSeconds()` | `float` | read |
| `setDurationSeconds(float)` | `void` | re-tune |

### `PreRefineCompleteEvent`

```java
AlchemyEvents.onPreRefineComplete(event -> { /* ... */ });
```

A refining ritual is about to resolve into a grade. Cancel to abandon it silently - the herbs and Qi stay spent, nothing is produced, the same shape `ItemEvents.PreRefinementCompleteEvent` and `TalismanEvents.PreInscribeCompleteEvent` both use. `setHerbQualityAvg` and `setMasteryLadderFraction` re-weight the two inputs `AlchemyManager#rollOutcome` actually rolls against. Fires only on a natural completion, never an interruption - see this class's own doc.

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read |
| `effect()` | `String` | read |
| `herbQualityAvg()` | `float` | read |
| `masteryLadderFraction()` | `float` | read |
| `tendDelta()` | `float` | read |
| `setHerbQualityAvg(float)` | `void` | re-tune |
| `setMasteryLadderFraction(float)` | `void` | re-tune |
| `setTendDelta(float)` | `void` | re-tune |


---

## Forging (0.9.x)

`plugin.siren.API.ForgingEvents` — Tempering an already-crafted Cultivation weapon/armor at a Forge Anchor - success, failure and botch outcomes.

**Enums declared here**

- `ForgingEvents.ForgeOutcome` — How a completed (or interrupted) forging attempt resolved. Values: `SUCCESS`, `FAILED`, `BOTCH`

**Post-events** — fired once the change is committed; cannot be cancelled.

### `ForgeCompleteEvent`

```java
ForgingEvents.onForgeComplete(event -> { /* ... */ });
```

A forging attempt resolved. `grade` is null unless `outcome` is SUCCESS. `resultStack` is the item as it now stands (forged, demoted, or unchanged) - null only if there was nowhere to place it back into the cultivator's inventory (see `ForgingManager#placeItem`), which the accompanying player message already reports. The item is never destroyed outright by anything this event could be reporting.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `targetTier()` | `int` |
| `outcome()` | `ForgeOutcome` |
| `grade()` | `ForgeGrade` |
| `resultStack()` | `ItemStack` |

**Pre-events** — fired before the change; `setCancelled(true)` vetoes it, and any setter below re-tunes the numbers the mod then uses.

### `PreForgeEvent`

```java
ForgingEvents.onPreForge(event -> { /* ... */ });
```

A forging attempt is about to begin - every check has passed, but no materials and no item have been touched yet. Cancel to refuse it entirely; nothing is spent and no ritual starts.

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read |
| `itemId()` | `String` | read |
| `targetTier()` | `int` | read |


---

## Talismans (0.9.x)

`plugin.siren.API.TalismanEvents` — Inscribing at a Talisman Desk (start and complete, with success/failed/botch outcomes) and using a finished talisman.

**Enums declared here**

- `TalismanEvents.InscribeOutcome` — How a completed (or interrupted) inscription ritual resolved. Values: `SUCCESS`, `FAILED`, `BOTCH`

**Post-events** — fired once the change is committed; cannot be cancelled.

### `InscribeStartEvent`

```java
TalismanEvents.onInscribeStart(event -> { /* ... */ });
```

An inscription ritual began; materials and the Qi floor check already passed.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `talismanId()` | `String` |
| `element()` | `DaoElement` |
| `qiDrainPerSecond()` | `float` |

### `InscribeCompleteEvent`

```java
TalismanEvents.onInscribeComplete(event -> { /* ... */ });
```

An inscription ritual resolved. `grade`/`stack` are null unless `outcome` is SUCCESS.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `talismanId()` | `String` |
| `element()` | `DaoElement` |
| `outcome()` | `InscribeOutcome` |
| `grade()` | `TalismanGrade` |
| `stack()` | `ItemStack` |

### `TalismanUseEvent`

```java
TalismanEvents.onTalismanUse(event -> { /* ... */ });
```

A talisman was used and its effect applied. `remainingCharges` is what is left AFTER this use - the stack is gone once it reaches 0.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `talismanId()` | `String` |
| `grade()` | `TalismanGrade` |
| `remainingCharges()` | `int` |

**Pre-events** — fired before the change; `setCancelled(true)` vetoes it, and any setter below re-tunes the numbers the mod then uses.

### `PreInscribeStartEvent`

```java
TalismanEvents.onPreInscribeStart(event -> { /* ... */ });
```

An inscription ritual is about to begin. Cancel to refuse it (no materials or Qi are spent).

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read |
| `talismanId()` | `String` | read |
| `element()` | `DaoElement` | read |

### `PreInscribeCompleteEvent`

```java
TalismanEvents.onPreInscribeComplete(event -> { /* ... */ });
```

An inscription ritual is about to resolve into a grade. Cancel to abandon it silently - the same "materials/Qi already sunk, nothing is produced" shape `ItemEvents.PreRefinementCompleteEvent` uses. `setMasteryLadderFraction` and `setAffinityFraction` re-weight the two inputs `TalismanManager#rollOutcome` actually rolls against - the supported way to reshape a talisman's grade odds from an addon (e.g. a race or Sacred Body constitution granting a flat affinity bonus for the roll ONLY, without touching the persisted DaoComponent). Fires only on a natural completion, never an interruption - see this class's own doc.

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read |
| `talismanId()` | `String` | read |
| `element()` | `DaoElement` | read |
| `masteryLadderFraction()` | `float` | read |
| `affinityFraction()` | `float` | read |
| `setMasteryLadderFraction(float)` | `void` | re-tune |
| `setAffinityFraction(float)` | `void` | re-tune |

### `PreTalismanUseEvent`

```java
TalismanEvents.onPreTalismanUse(event -> { /* ... */ });
```

A talisman is about to be used. Cancel to refuse it (the charge is not spent).

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read |
| `talismanId()` | `String` | read |
| `grade()` | `TalismanGrade` | read |


---

## Weapon Spirits (0.9.x)

`plugin.siren.API.WeaponSpiritEvents` — A Life-Bound Treasure's spirit (器灵) stirring awake, gaining a level, and being fed Qi through `/cultivation spirit nurture`.

**Post-events** — fired once the change is committed; cannot be cancelled.

### `WeaponSpiritAwakenEvent`

```java
WeaponSpiritEvents.onWeaponSpiritAwaken(event -> { /* ... */ });
```

A weapon's spirit just stirred awake; `item` is the stack AFTER the awaken is written.

| Accessor | Type |
| --- | --- |
| `owner()` | `PlayerRef` |
| `item()` | `ItemStack` |
| `killsAtAwaken()` | `int` |

### `WeaponSpiritLevelUpEvent`

```java
WeaponSpiritEvents.onWeaponSpiritLevelUp(event -> { /* ... */ });
```

A weapon's spirit just gained a level; `item` is the stack AFTER the level-up is written.

| Accessor | Type |
| --- | --- |
| `owner()` | `PlayerRef` |
| `item()` | `ItemStack` |
| `newLevel()` | `int` |
| `reachedMaturity()` | `boolean` |

### `WeaponSpiritNurtureEvent`

```java
WeaponSpiritEvents.onWeaponSpiritNurture(event -> { /* ... */ });
```

A player just fed their weapon spirit Qi; fires once per successful `nurture` call, whether or not it also leveled the spirit up.

| Accessor | Type |
| --- | --- |
| `owner()` | `PlayerRef` |
| `item()` | `ItemStack` |
| `qiConverted()` | `float` |
| `xpGained()` | `float` |

**Pre-events** — fired before the change; `setCancelled(true)` vetoes it, and any setter below re-tunes the numbers the mod then uses.

### `PreWeaponSpiritAwakenEvent`

```java
WeaponSpiritEvents.onPreWeaponSpiritAwaken(event -> { /* ... */ });
```

A weapon's spirit is about to stir awake - both the refinement-tier and kill bars are already met. Cancel to hold it right at the threshold; the banked kill count is untouched, so the very next qualifying kill fires this again.

| Member | Type | |
| --- | --- | --- |
| `owner()` | `PlayerRef` | read |
| `item()` | `ItemStack` | read |
| `killsAtAwaken()` | `int` | read |

### `PreWeaponSpiritLevelUpEvent`

```java
WeaponSpiritEvents.onPreWeaponSpiritLevelUp(event -> { /* ... */ });
```

A weapon spirit is about to level up. Cancel to hold it at its current level (the Xp is still banked).

| Member | Type | |
| --- | --- | --- |
| `owner()` | `PlayerRef` | read |
| `item()` | `ItemStack` | read |
| `oldLevel()` | `int` | read |
| `newLevel()` | `int` | read |

### `PreWeaponSpiritNurtureEvent`

```java
WeaponSpiritEvents.onPreWeaponSpiritNurture(event -> { /* ... */ });
```

A player is about to feed their weapon spirit Qi. Cancel to refuse the feeding entirely (no Qi spent, no Xp gained); `setQiToConvert` to change how much of the offered Qi actually converts - this is where `WeaponSpirit-Nurture-Max-Qi-Per-Use` is applied by default, and an addon raising or lowering it (a VIP perk, a debuff) does so by adjusting this field rather than the config itself.

| Member | Type | |
| --- | --- | --- |
| `owner()` | `PlayerRef` | read |
| `item()` | `ItemStack` | read |
| `qiToConvert()` | `float` | read |
| `setQiToConvert(float)` | `void` | re-tune |


---

## Spirit beasts

`plugin.siren.API.BeastEvents` — Taming, hatching, binding, summoning and companion growth.

**Enums declared here**

- `BeastEvents.BindSource` — How a cultivator came by their companion. Values: `TAME`, `HATCH`, `DEN`
- `BeastEvents.DismissReason` — Why a companion's body left the world. Values: `DISMISSED`, `RELEASED`, `EXPEDITION`

**Post-events** — fired once the change is committed; cannot be cancelled.

### `BeastTameAttemptEvent`

```java
BeastEvents.onBeastTameAttempt(event -> { /* ... */ });
```

A tame was attempted. `success` says whether the beast was actually bound (the bind event follows when it was).

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `species()` | `BeastSpecies` |
| `chance()` | `float` |
| `success()` | `boolean` |

### `BeastBindEvent`

```java
BeastEvents.onBeastBind(event -> { /* ... */ });
```

A companion is now bound to a cultivator, replacing whatever they had before.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `species()` | `BeastSpecies` |
| `source()` | `BindSource` |

### `BeastSummonEvent`

```java
BeastEvents.onBeastSummon(event -> { /* ... */ });
```

A companion's body was spawned beside its master.

| Accessor | Type |
| --- | --- |
| `owner()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `beast()` | `Ref<EntityStore>` |
| `species()` | `BeastSpecies` |

### `BeastDismissEvent`

```java
BeastEvents.onBeastDismiss(event -> { /* ... */ });
```

A companion's body left the world - sent home, or freed for good.

| Accessor | Type |
| --- | --- |
| `owner()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `reason()` | `DismissReason` |

### `BeastXpGainEvent`

```java
BeastEvents.onBeastXpGain(event -> { /* ... */ });
```

A companion gained cultivation XP. `stagesGained` is how far that carried it (0 when it only banked progress).

| Accessor | Type |
| --- | --- |
| `owner()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `amount()` | `float` |
| `stagesGained()` | `int` |

### `BeastAdvanceEvent`

```java
BeastEvents.onBeastAdvance(event -> { /* ... */ });
```

A companion advanced a stage (or rolled into the next realm). Fires once per stage.

| Accessor | Type |
| --- | --- |
| `owner()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `realm()` | `CultivationRealm` |
| `stage()` | `CultivationStage` |

### `BeastArtEvent`

```java
BeastEvents.onBeastArt(event -> { /* ... */ });
```

A companion performed one of its arts.

| Accessor | Type |
| --- | --- |
| `owner()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `beast()` | `SpiritBeastComponent` |
| `art()` | `BeastArt` |
| `commanded()` | `boolean` |

### `BeastEvolveEvent`

```java
BeastEvents.onBeastEvolve(event -> { /* ... */ });
```

A companion's evolution ritual resolved - `succeeded` says which way, and `to` is null on failure.

| Accessor | Type |
| --- | --- |
| `owner()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `beast()` | `SpiritBeastComponent` |
| `from()` | `BeastSpecies` |
| `to()` | `BeastSpecies` |
| `succeeded()` | `boolean` |

### `BeastMountEvent`

```java
BeastEvents.onBeastMount(event -> { /* ... */ });
```

A companion was summoned in its rideable body.

| Accessor | Type |
| --- | --- |
| `owner()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `beast()` | `SpiritBeastComponent` |
| `species()` | `BeastSpecies` |

### `BeastDenBroodEvent`

```java
BeastEvents.onBeastDenBrood(event -> { /* ... */ });
```

A Spirit Beast Den rolled a brood, now waiting to be claimed once due. See SpiritDenManager.ensureBrood.

| Accessor | Type |
| --- | --- |
| `world()` | `String` |
| `x()` | `int` |
| `y()` | `int` |
| `z()` | `int` |
| `species()` | `BeastSpecies` |
| `appraisalScore()` | `int` |

**Pre-events** — fired before the change; `setCancelled(true)` vetoes it, and any setter below re-tunes the numbers the mod then uses.

### `PreBeastTameAttemptEvent`

```java
BeastEvents.onPreBeastTameAttempt(event -> { /* ... */ });
```

A tame is about to be rolled. Cancel to refuse the attempt outright (no talisman is spent); `setChance` to re-weight the odds - 1 guarantees it, 0 dooms it.

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read (may be null) |
| `species()` | `BeastSpecies` | read |
| `chance()` | `float` | read |
| `setChance(float)` | `void` | re-tune |

### `PreBeastBindEvent`

```java
BeastEvents.onPreBeastBind(event -> { /* ... */ });
```

A companion is about to be bound. Cancel to refuse the bond - the cultivator keeps whatever beast they already had, and the talisman/egg is still spent.

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read (may be null) |
| `species()` | `BeastSpecies` | read |
| `source()` | `BindSource` | read |

### `PreBeastSummonEvent`

```java
BeastEvents.onPreBeastSummon(event -> { /* ... */ });
```

A companion's body is about to be spawned. Cancel to refuse (reported to the player as a failed summon).

| Member | Type | |
| --- | --- | --- |
| `owner()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read (may be null) |
| `species()` | `BeastSpecies` | read |

### `PreBeastDismissEvent`

```java
BeastEvents.onPreBeastDismiss(event -> { /* ... */ });
```

A companion is about to be sent home or freed. Cancel to keep it where it is.

| Member | Type | |
| --- | --- | --- |
| `owner()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read (may be null) |
| `reason()` | `DismissReason` | read |

### `PreBeastXpGainEvent`

```java
BeastEvents.onPreBeastXpGain(event -> { /* ... */ });
```

A companion is about to gain XP. Cancel to deny it; `setAmount` to re-scale. Fires for every source - meditation shares, kills, and hand-feeding alike.

| Member | Type | |
| --- | --- | --- |
| `owner()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read (may be null) |
| `beast()` | `SpiritBeastComponent` | read |
| `amount()` | `float` | read |
| `setAmount(float)` | `void` | re-tune |

### `PreBeastAdvanceEvent`

```java
BeastEvents.onPreBeastAdvance(event -> { /* ... */ });
```

A companion is about to advance a stage. Cancel to hold it where it is - the XP for that stage is already spent, so this costs it the progress.

| Member | Type | |
| --- | --- | --- |
| `owner()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read (may be null) |
| `fromRealm()` | `CultivationRealm` | read |
| `fromStage()` | `CultivationStage` | read |

### `PreBeastArtEvent`

```java
BeastEvents.onPreBeastArt(event -> { /* ... */ });
```

A companion is about to perform an art. Cancel to stop it - the cooldown is only stamped once the effect has actually run, so a vetoed art costs the beast nothing and it will try again on its next opening.

| Member | Type | |
| --- | --- | --- |
| `owner()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read (may be null) |
| `beast()` | `SpiritBeastComponent` | read |
| `art()` | `BeastArt` | read |
| `commanded()` | `boolean` | read |

### `PreBeastEvolveEvent`

```java
BeastEvents.onPreBeastEvolve(event -> { /* ... */ });
```

A companion is about to be put through the evolution ritual. Cancel to refuse it - the Qi has NOT been taken at this point, so a veto here costs the cultivator nothing.

| Member | Type | |
| --- | --- | --- |
| `owner()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read (may be null) |
| `beast()` | `SpiritBeastComponent` | read |
| `from()` | `BeastSpecies` | read |
| `to()` | `BeastSpecies` | read |
| `successChance()` | `float` | read |
| `setSuccessChance(float)` | `void` | re-tune |

### `PreBeastMountEvent`

```java
BeastEvents.onPreBeastMount(event -> { /* ... */ });
```

A companion is about to be summoned in its rideable body. Cancel to refuse the mount.

| Member | Type | |
| --- | --- | --- |
| `owner()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read (may be null) |
| `beast()` | `SpiritBeastComponent` | read |
| `species()` | `BeastSpecies` | read |

### `PreBeastDenBroodEvent`

```java
BeastEvents.onPreBeastDenBrood(event -> { /* ... */ });
```

A Spirit Beast Den is about to roll a brood. Cancel to leave the den without one (it will simply try again the next time its appraisal sweep calls SpiritDenManager.ensureBrood) - nothing has been persisted yet, so a veto here costs the keeper nothing.

| Member | Type | |
| --- | --- | --- |
| `world()` | `String` | read |
| `x()` | `int` | read |
| `y()` | `int` | read |
| `z()` | `int` | read |
| `species()` | `BeastSpecies` | read |
| `appraisalScore()` | `int` | read |


---

## Spirit beast breeding (0.9.x)

`plugin.siren.API.BreedingEvents` — The two-cultivator ritual at a Beast Pen that produces an egg, and hatching a bred egg into a bound companion. Complements `BeastEvents` above - a bred egg still fires its `onBeastBind` when it hatches.

**Post-events** — fired once the change is committed; cannot be cancelled.

### `BeastBreedEvent`

```java
BreedingEvents.onBeastBreed(event -> { /* ... */ });
```

A breeding ritual completed and produced an egg for `recipient` (whoever sent the offer). Fires once, from the ritual's own completion, not from either offer or accept.

| Accessor | Type |
| --- | --- |
| `recipient()` | `PlayerRef` |
| `partner()` | `PlayerRef` |
| `parentA()` | `BeastSpecies` |
| `parentB()` | `BeastSpecies` |
| `offspring()` | `BeastSpecies` |
| `quality()` | `float` |

### `BeastEggHatchEvent`

```java
BreedingEvents.onBeastEggHatch(event -> { /* ... */ });
```

A bred egg hatched into a bound companion.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `offspring()` | `BeastSpecies` |
| `metadata()` | `BeastEggMetadata` |

**Pre-events** — fired before the change; `setCancelled(true)` vetoes it, and any setter below re-tunes the numbers the mod then uses.

### `PreBeastBreedEvent`

```java
BreedingEvents.onPreBeastBreed(event -> { /* ... */ });
```

A breeding ritual is about to be seated on both cultivators - fires once both are confirmed within a shared pen with enough Qi, before either cultivator's Qi is touched. Cancel to refuse it outright; nothing has been spent yet.

| Member | Type | |
| --- | --- | --- |
| `offerer()` | `PlayerRef` | read |
| `accepter()` | `PlayerRef` | read |
| `parentA()` | `BeastSpecies` | read |
| `parentB()` | `BeastSpecies` | read |

### `PreBeastEggHatchEvent`

```java
BreedingEvents.onPreBeastEggHatch(event -> { /* ... */ });
```

A bred egg is about to hatch. Cancel to refuse it - the egg is NOT consumed, matching a dormant wild-hatched egg (see `BeastEggHatchInteraction`'s bred branch).

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read (may be null) |
| `offspring()` | `BeastSpecies` | read |
| `metadata()` | `BeastEggMetadata` | read |


---

## Sects

`plugin.siren.API.SectEvents` — Founding, disbanding, membership, ranks, abbreviations, halls and inscriptions.

**Enums declared here**

- `SectEvents.JoinMethod` — How a player came to be in a sect. Values: `INVITE`, `OPEN`, `REQUEST`
- `SectEvents.LeaveReason` — Why a player is no longer in a sect. Values: `LEFT`, `KICKED`

**Post-events** — fired once the change is committed; cannot be cancelled.

### `SectCreateEvent`

```java
SectEvents.onSectCreate(event -> { /* ... */ });
```

A new sect was founded and indexed.

| Accessor | Type |
| --- | --- |
| `leader()` | `UUID` |
| `sect()` | `Sect` |

### `SectDisbandEvent`

```java
SectEvents.onSectDisband(event -> { /* ... */ });
```

A sect was disbanded; its members are already unindexed and its formations released. `sect` is the now-orphaned object, still readable for its final roster.

| Accessor | Type |
| --- | --- |
| `leader()` | `UUID` |
| `sect()` | `Sect` |

### `SectInviteEvent`

```java
SectEvents.onSectInvite(event -> { /* ... */ });
```

A manager invited a player. The invite is pending, not accepted.

| Accessor | Type |
| --- | --- |
| `inviter()` | `UUID` |
| `invitee()` | `UUID` |
| `sect()` | `Sect` |

### `SectJoinEvent`

```java
SectEvents.onSectJoin(event -> { /* ... */ });
```

A player joined a sect and is now on its roster.

| Accessor | Type |
| --- | --- |
| `player()` | `UUID` |
| `sect()` | `Sect` |
| `method()` | `JoinMethod` |

### `SectLeaveEvent`

```java
SectEvents.onSectLeave(event -> { /* ... */ });
```

A player is off a sect's roster. `actor` is the kicker for KICKED, and the player themselves for LEFT.

| Accessor | Type |
| --- | --- |
| `player()` | `UUID` |
| `sect()` | `Sect` |
| `reason()` | `LeaveReason` |
| `actor()` | `UUID` |

### `SectJoinRequestEvent`

```java
SectEvents.onSectJoinRequest(event -> { /* ... */ });
```

A player queued a join request against a REQUEST-policy sect.

| Accessor | Type |
| --- | --- |
| `player()` | `UUID` |
| `sect()` | `Sect` |

### `SectJoinRequestDeniedEvent`

```java
SectEvents.onSectJoinRequestDenied(event -> { /* ... */ });
```

A manager denied a pending join request.

| Accessor | Type |
| --- | --- |
| `manager()` | `UUID` |
| `applicant()` | `UUID` |
| `sect()` | `Sect` |

### `SectRankChangeEvent`

```java
SectEvents.onSectRankChange(event -> { /* ... */ });
```

A member's elder rank changed. `promoted` true = plain member -> elder, false = elder -> plain member.

| Accessor | Type |
| --- | --- |
| `leader()` | `UUID` |
| `target()` | `UUID` |
| `sect()` | `Sect` |
| `promoted()` | `boolean` |

### `SectMottoChangeEvent`

```java
SectEvents.onSectMottoChange(event -> { /* ... */ });
```

A sect's motto was replaced.

| Accessor | Type |
| --- | --- |
| `manager()` | `UUID` |
| `sect()` | `Sect` |
| `oldMotto()` | `String` |
| `newMotto()` | `String` |

### `SectAbbreviationChangeEvent`

```java
SectEvents.onSectAbbreviationChange(event -> { /* ... */ });
```

A sect's abbreviation was replaced. `oldAbbreviation` is empty for a sect that never had one.

| Accessor | Type |
| --- | --- |
| `leader()` | `UUID` |
| `sect()` | `Sect` |
| `oldAbbreviation()` | `String` |
| `newAbbreviation()` | `String` |

### `SectBannerChangeEvent`

```java
SectEvents.onSectBannerChange(event -> { /* ... */ });
```

A sect changed the banner flown over its hall.

| Accessor | Type |
| --- | --- |
| `manager()` | `UUID` |
| `sect()` | `Sect` |
| `oldBannerId()` | `String` |
| `newBannerId()` | `String` |

### `SectJoinPolicyChangeEvent`

```java
SectEvents.onSectJoinPolicyChange(event -> { /* ... */ });
```

A sect's join policy was changed.

| Accessor | Type |
| --- | --- |
| `leader()` | `UUID` |
| `sect()` | `Sect` |
| `oldPolicy()` | `Sect.JoinPolicy` |
| `newPolicy()` | `Sect.JoinPolicy` |

### `SectRenameEvent`

```java
SectEvents.onSectRename(event -> { /* ... */ });
```

A sect was renamed; formations, hall springs and pending invites have already been carried over.

| Accessor | Type |
| --- | --- |
| `leader()` | `UUID` |
| `sect()` | `Sect` |
| `oldName()` | `String` |
| `newName()` | `String` |

### `SectInscriptionChangeEvent`

```java
SectEvents.onSectInscriptionChange(event -> { /* ... */ });
```

A sect's hall inscription changed. `newTechniqueId` is empty when the inscription was scoured away.

| Accessor | Type |
| --- | --- |
| `leader()` | `UUID` |
| `sect()` | `Sect` |
| `oldTechniqueId()` | `String` |
| `newTechniqueId()` | `String` |

### `SectHallClaimEvent`

```java
SectEvents.onSectHallClaim(event -> { /* ... */ });
```

A sect claimed (or moved) its hall onto a spirit vein.

| Accessor | Type |
| --- | --- |
| `leader()` | `UUID` |
| `sect()` | `Sect` |
| `world()` | `String` |
| `chunkX()` | `int` |
| `chunkZ()` | `int` |
| `veinTier()` | `int` |

### `SectHallCaptureEvent`

```java
SectEvents.onSectHallCapture(event -> { /* ... */ });
```

A won siege transferred a hall. The defender is now hall-less.

| Accessor | Type |
| --- | --- |
| `attacker()` | `Sect` |
| `defender()` | `Sect` |
| `world()` | `String` |
| `chunkX()` | `int` |
| `chunkZ()` | `int` |
| `veinTier()` | `int` |

### `SectAllianceFormedEvent`

```java
SectEvents.onSectAllianceFormed(event -> { /* ... */ });
```

Two sects formed a mutual Alliance - see AllianceManager.acceptAlliance. `actor` is whoever accepted the proposal.

| Accessor | Type |
| --- | --- |
| `actor()` | `UUID` |
| `sectA()` | `Sect` |
| `sectB()` | `Sect` |

### `SectAllianceBrokenEvent`

```java
SectEvents.onSectAllianceBroken(event -> { /* ... */ });
```

A standing Alliance ended - either side's leader may break one unilaterally, see AllianceManager.breakAlliance. `actor` is whoever broke it.

| Accessor | Type |
| --- | --- |
| `actor()` | `UUID` |
| `sectA()` | `Sect` |
| `sectB()` | `Sect` |

### `SectRelationChangedEvent`

```java
SectEvents.onSectRelationChanged(event -> { /* ... */ });
```

A NON_AGGRESSION or TRADE relation formed or ended between two sects - never fired for ALLIANCE (see `SectAllianceFormedEvent`/`SectAllianceBrokenEvent`, which cover that rung specifically, so an addon already listening for those - a Discord-bridge mod, for instance - is not double-posted). `actor` is whoever accepted the proposal (formed) or broke the relation (not formed). `formed` is true when the relation just started, false when it just ended.

| Accessor | Type |
| --- | --- |
| `actor()` | `UUID` |
| `sectA()` | `Sect` |
| `sectB()` | `Sect` |
| `kind()` | `SectRelationKind` |
| `formed()` | `boolean` |

### `SectLibraryResearchStartEvent`

```java
SectEvents.onSectLibraryResearchStart(event -> { /* ... */ });
```

A sect's Library started researching a new subject (replacing whatever, if anything, it was researching before).

| Accessor | Type |
| --- | --- |
| `actor()` | `UUID` |
| `sect()` | `Sect` |
| `techniqueId()` | `String` |

### `SectLibraryCompileEvent`

```java
SectEvents.onSectLibraryCompile(event -> { /* ... */ });
```

A sect's Library finished compiling an exclusive technique manual.

| Accessor | Type |
| --- | --- |
| `sect()` | `Sect` |
| `techniqueId()` | `String` |

### `SectLibraryRedeemEvent`

```java
SectEvents.onSectLibraryRedeem(event -> { /* ... */ });
```

A member redeemed their own copy of a compiled Library manual.

| Accessor | Type |
| --- | --- |
| `member()` | `UUID` |
| `sect()` | `Sect` |
| `techniqueId()` | `String` |
| `manualItemId()` | `String` |

**Pre-events** — fired before the change; `setCancelled(true)` vetoes it, and any setter below re-tunes the numbers the mod then uses.

### `PreSectCreateEvent`

```java
SectEvents.onPreSectCreate(event -> { /* ... */ });
```

A player is about to found a sect. Cancel to refuse (reported as a disabled/refused creation); `setName` to force a different name - it is re-validated for shape and uniqueness afterward.

| Member | Type | |
| --- | --- | --- |
| `leader()` | `UUID` | read |
| `name()` | `String` | read |
| `setName(String)` | `void` | re-tune |

### `PreSectDisbandEvent`

```java
SectEvents.onPreSectDisband(event -> { /* ... */ });
```

A sect is about to be disbanded. Cancel to keep it standing.

| Member | Type | |
| --- | --- | --- |
| `leader()` | `UUID` | read |
| `sect()` | `Sect` | read |

### `PreSectInviteEvent`

```java
SectEvents.onPreSectInvite(event -> { /* ... */ });
```

An invite is about to be issued. Cancel to refuse it; `setExpiryMillis` to change when it lapses.

| Member | Type | |
| --- | --- | --- |
| `inviter()` | `UUID` | read |
| `invitee()` | `UUID` | read |
| `sect()` | `Sect` | read |
| `expiryMillis()` | `long` | read |
| `setExpiryMillis(long)` | `void` | re-tune |

### `PreSectJoinEvent`

```java
SectEvents.onPreSectJoin(event -> { /* ... */ });
```

A player is about to join a sect. Cancel to keep them out - the invite/request survives, so they can try again.

| Member | Type | |
| --- | --- | --- |
| `player()` | `UUID` | read |
| `sect()` | `Sect` | read |
| `method()` | `JoinMethod` | read |

### `PreSectLeaveEvent`

```java
SectEvents.onPreSectLeave(event -> { /* ... */ });
```

A player is about to leave (or be kicked from) a sect. Cancel to keep them on the roster.

| Member | Type | |
| --- | --- | --- |
| `player()` | `UUID` | read |
| `sect()` | `Sect` | read |
| `reason()` | `LeaveReason` | read |
| `actor()` | `UUID` | read |

### `PreSectJoinRequestEvent`

```java
SectEvents.onPreSectJoinRequest(event -> { /* ... */ });
```

A join request is about to be queued. Cancel to refuse it.

| Member | Type | |
| --- | --- | --- |
| `player()` | `UUID` | read |
| `sect()` | `Sect` | read |

### `PreSectRankChangeEvent`

```java
SectEvents.onPreSectRankChange(event -> { /* ... */ });
```

A member's elder rank is about to change. Cancel to leave their rank as it stands.

| Member | Type | |
| --- | --- | --- |
| `leader()` | `UUID` | read |
| `target()` | `UUID` | read |
| `sect()` | `Sect` | read |
| `promoted()` | `boolean` | read |

### `PreSectMottoChangeEvent`

```java
SectEvents.onPreSectMottoChange(event -> { /* ... */ });
```

A motto is about to be set. Cancel to refuse it; `setMotto` to rewrite it (the 60-char cap still applies afterward).

| Member | Type | |
| --- | --- | --- |
| `manager()` | `UUID` | read |
| `sect()` | `Sect` | read |
| `oldMotto()` | `String` | read |
| `motto()` | `String` | read |
| `setMotto(String)` | `void` | re-tune |

### `PreSectAbbreviationChangeEvent`

```java
SectEvents.onPreSectAbbreviationChange(event -> { /* ... */ });
```

A sect's abbreviation is about to change. Cancel to refuse it; `setAbbreviation` to rewrite it (the 3-6 letters/digits rule and the uniqueness check still apply afterward).

| Member | Type | |
| --- | --- | --- |
| `leader()` | `UUID` | read |
| `sect()` | `Sect` | read |
| `oldAbbreviation()` | `String` | read |
| `abbreviation()` | `String` | read |
| `setAbbreviation(String)` | `void` | re-tune |

### `PreSectBannerChangeEvent`

```java
SectEvents.onPreSectBannerChange(event -> { /* ... */ });
```

A hall banner is about to change. Cancel to refuse it; `setBannerId` to force a different one - useful for a server that wants a sect's banner decided by something other than the sect's own taste (a war outcome, a rank, an alliance). The id is not validated after a listener rewrites it. An id nobody has registered is not an error: it resolves to the vein-tier default light, the same as an id whose mod has been uninstalled.

| Member | Type | |
| --- | --- | --- |
| `manager()` | `UUID` | read |
| `sect()` | `Sect` | read |
| `oldBannerId()` | `String` | read |
| `bannerId()` | `String` | read |
| `setBannerId(String)` | `void` | re-tune |

### `PreSectJoinPolicyChangeEvent`

```java
SectEvents.onPreSectJoinPolicyChange(event -> { /* ... */ });
```

A join policy is about to change. Cancel to keep the current one; `setPolicy` to force a different one.

| Member | Type | |
| --- | --- | --- |
| `leader()` | `UUID` | read |
| `sect()` | `Sect` | read |
| `oldPolicy()` | `Sect.JoinPolicy` | read |
| `policy()` | `Sect.JoinPolicy` | read |
| `setPolicy(Sect.JoinPolicy)` | `void` | re-tune |

### `PreSectRenameEvent`

```java
SectEvents.onPreSectRename(event -> { /* ... */ });
```

A sect is about to be renamed. Cancel to keep the current name; `setNewName` to force a different one - it is re-validated for shape and uniqueness afterward.

| Member | Type | |
| --- | --- | --- |
| `leader()` | `UUID` | read |
| `sect()` | `Sect` | read |
| `oldName()` | `String` | read |
| `newName()` | `String` | read |
| `setNewName(String)` | `void` | re-tune |

### `PreSectInscriptionChangeEvent`

```java
SectEvents.onPreSectInscriptionChange(event -> { /* ... */ });
```

A hall inscription is about to change. Cancel to leave it as it is; `setNewTechniqueId` to carve something else (empty scours it away).

| Member | Type | |
| --- | --- | --- |
| `leader()` | `UUID` | read |
| `sect()` | `Sect` | read |
| `oldTechniqueId()` | `String` | read |
| `newTechniqueId()` | `String` | read |
| `setNewTechniqueId(String)` | `void` | re-tune |

### `PreSectHallClaimEvent`

```java
SectEvents.onPreSectHallClaim(event -> { /* ... */ });
```

A hall is about to be claimed. Cancel to refuse the claim (reported as a chunk already claimed).

| Member | Type | |
| --- | --- | --- |
| `leader()` | `UUID` | read |
| `sect()` | `Sect` | read |
| `world()` | `String` | read |
| `chunkX()` | `int` | read |
| `chunkZ()` | `int` | read |
| `veinTier()` | `int` | read |

### `PreSectHallCaptureEvent`

```java
SectEvents.onPreSectHallCapture(event -> { /* ... */ });
```

A hall is about to change hands to a victorious besieger. Cancel to leave it with its defender (the siege still resolves as won).

| Member | Type | |
| --- | --- | --- |
| `attacker()` | `Sect` | read |
| `defender()` | `Sect` | read |
| `world()` | `String` | read |
| `chunkX()` | `int` | read |
| `chunkZ()` | `int` | read |
| `veinTier()` | `int` | read |

### `PreSectLibraryResearchStartEvent`

```java
SectEvents.onPreSectLibraryResearchStart(event -> { /* ... */ });
```

A sect's Library is about to start researching a new subject. Cancel to leave it researching whatever it was (or nothing).

| Member | Type | |
| --- | --- | --- |
| `actor()` | `UUID` | read |
| `sect()` | `Sect` | read |
| `oldTechniqueId()` | `String` | read |
| `newTechniqueId()` | `String` | read |

### `PreSectLibraryCompileEvent`

```java
SectEvents.onPreSectLibraryCompile(event -> { /* ... */ });
```

A sect's Library is about to compile an exclusive technique. Cancel to hold it at the door - progress stays at/above cost and this fires again on the next settle.

| Member | Type | |
| --- | --- | --- |
| `sect()` | `Sect` | read |
| `techniqueId()` | `String` | read |

### `PreSectLibraryRedeemEvent`

```java
SectEvents.onPreSectLibraryRedeem(event -> { /* ... */ });
```

A member is about to redeem their copy of a compiled Library manual. Cancel to refuse it (not marked redeemed, so it may be retried); `setManualItemId` to substitute a different item - mirrors `ItemEvents.PreLootDropEvent#setItemId`.

| Member | Type | |
| --- | --- | --- |
| `member()` | `UUID` | read |
| `sect()` | `Sect` | read |
| `techniqueId()` | `String` | read |
| `manualItemId()` | `String` | read |
| `setManualItemId(String)` | `void` | re-tune |


---

## Sect wars

`plugin.siren.API.WarEvents` — Declaring sieges and how they resolve.

**Enums declared here**

- `WarEvents.SiegeFailReason` — Why a siege ended without the attacker triggering SUPPRESS. Values: `LAPSED`, `DEFENDER_GONE`, `MUSTER_FAILED`, `DEFENDER_ABSENT`, `ABORTED`
- `WarEvents.BannerBreakSide` — Which side of a siege a Siege Banner's breaker belonged to - see `SiegeBannerBreakEvent`. Values: 

**Post-events** — fired once the change is committed; cannot be cancelled.

### `WarDeclareEvent`

```java
WarEvents.onWarDeclare(event -> { /* ... */ });
```

A sect declared war on another's hall; the siege is live and both sides have been told.

| Accessor | Type |
| --- | --- |
| `attacker()` | `Sect` |
| `defender()` | `Sect` |
| `siege()` | `Siege` |

### `SiegeCaptureEvent`

```java
WarEvents.onSiegeCapture(event -> { /* ... */ });
```

An attacker held a contested hall long enough to take it. The hall transfer has already been attempted (see SectEvents.SectHallCaptureEvent) and the defender's immunity cooldown started.

| Accessor | Type |
| --- | --- |
| `attacker()` | `Sect` |
| `defender()` | `Sect` |
| `siege()` | `Siege` |

### `SiegeFailEvent`

```java
WarEvents.onSiegeFail(event -> { /* ... */ });
```

A siege ended with the hall still in its defender's hands. Sect objects are null when the sect no longer resolves by name.

| Accessor | Type |
| --- | --- |
| `siege()` | `Siege` |
| `reason()` | `SiegeFailReason` |

### `SiegeBannerPlaceEvent`

```java
WarEvents.onSiegeBannerPlace(event -> { /* ... */ });
```

Formations 2.0: a Siege Banner was placed and is now standing.

| Accessor | Type |
| --- | --- |
| `banner()` | `SiegeBanner` |

### `SiegeBannerBreakEvent`

```java
WarEvents.onSiegeBannerBreak(event -> { /* ... */ });
```

Formations 2.0: a Siege Banner was broken - by anyone, no protection. `breakerSide` tells which side (if any) the breaker belonged to.

| Accessor | Type |
| --- | --- |
| `banner()` | `SiegeBanner` |
| `breaker()` | `UUID` |
| `breakerSide()` | `BannerBreakSide` |

**Pre-events** — fired before the change; `setCancelled(true)` vetoes it, and any setter below re-tunes the numbers the mod then uses.

### `PreWarDeclareEvent`

```java
WarEvents.onPreWarDeclare(event -> { /* ... */ });
```

A siege is about to be declared. Cancel to refuse it (reported to the caller as wars being disabled); `setWindowMillis` to give this siege a longer or shorter window than the config's.

| Member | Type | |
| --- | --- | --- |
| `attacker()` | `Sect` | read |
| `defender()` | `Sect` | read |
| `windowMillis()` | `long` | read |
| `setWindowMillis(long)` | `void` | re-tune |

### `PreSiegeCaptureEvent`

```java
WarEvents.onPreSiegeCapture(event -> { /* ... */ });
```

A siege is about to be won. Cancel to leave it running - the attacker keeps holding and will trip this again on their next presence tick, so cancel only while some condition of yours is unmet.

| Member | Type | |
| --- | --- | --- |
| `attacker()` | `Sect` | read |
| `defender()` | `Sect` | read |
| `siege()` | `Siege` | read |

### `PreWarReinforcementRewardEvent`

```java
WarEvents.onPreWarReinforcementReward(event -> { /* ... */ });
```

An allied sect's reinforcement of a successfully-defended (LAPSED) siege is about to be rewarded - see `WarManager#failSiege`'s LAPSED branch, the only place this fires. Cancel to withhold THIS specific ally's reward without affecting any other qualifying ally on the same siege (each reinforcing sect gets its own event); `setContributionPerMember` to rescale the payout for this reward only, mirroring `PreWarDeclareEvent#setWindowMillis`'s re-tuning pattern.

| Member | Type | |
| --- | --- | --- |
| `reinforcingSect()` | `Sect` | read |
| `defender()` | `Sect` | read |
| `siege()` | `Siege` | read |
| `contributionPerMember()` | `int` | read |
| `setContributionPerMember(int)` | `void` | re-tune |

### `PreSiegeBannerPlaceEvent`

```java
WarEvents.onPreSiegeBannerPlace(event -> { /* ... */ });
```

Formations 2.0: a Siege Banner is about to be placed. Cancel to refuse it (reported to the placer as something preventing the placement). Fired from `SiegeBannerManager.tryPlace` BEFORE any monitor is taken - no lock is held during dispatch (see that class's "Locking" doc). A listener may therefore safely read `WarManager` state; the registration cap and siege liveness are re-checked authoritatively after this event, inside the synchronized registration step.

| Member | Type | |
| --- | --- | --- |
| `attackerSect()` | `String` | read |
| `defenderSect()` | `String` | read |
| `world()` | `String` | read |
| `x()` | `int` | read |
| `y()` | `int` | read |
| `z()` | `int` | read |


---

## Sect Guardians (0.10.2)

`plugin.siren.API.GuardianEvents` — Stationing a guardian NPC at a sect hall, one falling, and the last one falling. Stationing is cancellable.

**Post-events** — fired once the change is committed; cannot be cancelled.

### `SectGuardianStationedEvent`

```java
GuardianEvents.onSectGuardianStationed(event -> { /* ... */ });
```

A member successfully stationed a new guardian post at their sect's hall - the post is purchased; the live NPC body itself appears on `SectGuardianSystem`'s next heartbeat once its chunk is resident.

| Accessor | Type |
| --- | --- |
| `sect()` | `Sect` |
| `actor()` | `PlayerRef` |
| `postIndex()` | `int` |

### `SectGuardianFellEvent`

```java
GuardianEvents.onSectGuardianFell(event -> { /* ... */ });
```

One living guardian died. `killerPlayerRef` is null when the killing blow never resolved to a player (environmental damage, a formation trap, or the killer despawned before resolution). `consumed` is true when the killer was the owning sect's own member or an ally - that post is gone for good (no respawn, no refund); false means the normal Guardian-Respawn-Seconds timer was armed instead.

| Accessor | Type |
| --- | --- |
| `sect()` | `Sect` |
| `postIndex()` | `int` |
| `killerPlayerRef()` | `PlayerRef` |
| `consumed()` | `boolean` |

### `SectGuardiansFallenEvent`

```java
GuardianEvents.onSectGuardiansFallen(event -> { /* ... */ });
```

The LAST living guardian at this sect's hall just died (or vanished into a permanent consumed state) - no living guardian remains at this instant. Purely informational: no hold-time bonus, no penalty (see `SectGuardianManager`'s own class javadoc for why).

| Accessor | Type |
| --- | --- |
| `sect()` | `Sect` |

**Pre-events** — fired before the change; `setCancelled(true)` vetoes it, and any setter below re-tunes the numbers the mod then uses.

### `PreSectGuardianStationEvent`

```java
GuardianEvents.onPreSectGuardianStation(event -> { /* ... */ });
```

A member is about to station a new guardian post. Cancel to refuse it (reported to the actor as the station attempt simply failing) - fired AFTER every gate in `SectGuardianManager#station` passes but BEFORE the contribution cost is spent, so a veto never costs the actor anything.

| Member | Type | |
| --- | --- | --- |
| `sect()` | `Sect` | read |
| `actor()` | `PlayerRef` | read |
| `postIndex()` | `int` | read |


---

## Dao Sermons (0.10.2)

`plugin.siren.API.SermonEvents` — A cultivator lecturing (讲道) at their own sect's hall, a meditating listener's first qualifying pulse, and how the sermon ends. Starting one is cancellable.

**Post-events** — fired once the change is committed; cannot be cancelled.

### `SermonStartEvent`

```java
SermonEvents.onSermonStart(event -> { /* ... */ });
```

A sect member successfully started a sermon at their own sect's hall.

| Accessor | Type |
| --- | --- |
| `sect()` | `Sect` |
| `lecturer()` | `PlayerRef` |
| `element()` | `DaoElement` |
| `endsAtMillis()` | `long` |

### `SermonListenerQualifiedEvent`

```java
SermonEvents.onSermonListenerQualified(event -> { /* ... */ });
```

One listener's FIRST qualifying pulse of this sermon - fired once per (sermon, listener) pair, never again for the same pair even across a diminished-rate or hard-stop transition. `lecturerUuid` rather than a `PlayerRef`/`Sect` - the lecturer may be resolved on a different call path than the listener's own tick that fires this.

| Accessor | Type |
| --- | --- |
| `lecturerUuid()` | `UUID` |
| `listener()` | `PlayerRef` |
| `element()` | `DaoElement` |
| `amountApplied()` | `float` |

### `SermonEndEvent`

```java
SermonEvents.onSermonEnd(event -> { /* ... */ });
```

A sermon ended - naturally (duration elapsed), early (`/cultivation sermon stop`), or abnormally (lecturer left the hall chunk/world, hall lost, sect besieged, feature disabled). `forfeited` is true only for the disconnect path (`SermonManager#forget`) - see that method's own doc for why a disconnect forfeits the Merit a graceful stop still pays out.

| Accessor | Type |
| --- | --- |
| `lecturerUuid()` | `UUID` |
| `sectName()` | `String` |
| `qualifiedListenerCount()` | `int` |
| `meritAwarded()` | `float` |
| `forfeited()` | `boolean` |

**Pre-events** — fired before the change; `setCancelled(true)` vetoes it, and any setter below re-tunes the numbers the mod then uses.

### `PreSermonStartEvent`

```java
SermonEvents.onPreSermonStart(event -> { /* ... */ });
```

A sect member is about to start a sermon. Cancel to refuse it (reported to the actor as the start attempt simply failing) - fired AFTER every gate in `SermonManager#start` passes but BEFORE the contribution cost is spent or the cooldown is stamped, so a veto never costs the actor anything.

| Member | Type | |
| --- | --- | --- |
| `sect()` | `Sect` | read |
| `lecturer()` | `PlayerRef` | read |
| `element()` | `DaoElement` | read |


---

## Rogue Cultivators (0.10.2)

`plugin.siren.API.RogueEvents` — A rogue cultivator NPC spawning and being slain. The spawn is cancellable.

**Post-events** — fired once the change is committed; cannot be cancelled.

### `RogueCultivatorSpawnedEvent`

```java
RogueEvents.onRogueCultivatorSpawned(event -> { /* ... */ });
```

A Rogue Cultivator has finished spawning and is tagged/live in the world.

| Accessor | Type |
| --- | --- |
| `encounterId()` | `String` |
| `npcRef()` | `Ref<EntityStore>` |
| `worldName()` | `String` |
| `realmOrdinal()` | `int` |
| `daoElementName()` | `String` |

### `RogueCultivatorSlainEvent`

```java
RogueEvents.onRogueCultivatorSlain(event -> { /* ... */ });
```

A Rogue Cultivator has been slain - any manual/core/Testament roll has already resolved by the time this fires.

| Accessor | Type |
| --- | --- |
| `encounterId()` | `String` |
| `killerUuid()` | `UUID` |
| `worldName()` | `String` |
| `manualAwarded()` | `boolean` |
| `bonusCoreAwarded()` | `boolean` |
| `testamentAwarded()` | `boolean` |

**Pre-events** — fired before the change; `setCancelled(true)` vetoes it, and any setter below re-tunes the numbers the mod then uses.

### `PreRogueCultivatorSpawnEvent`

```java
RogueEvents.onPreRogueCultivatorSpawn(event -> { /* ... */ });
```

A Rogue Cultivator is about to spawn near `anchorPosition` in `worldName`. Cancel to refuse the spawn entirely - the world's next-spawn-due clock still advances normally, exactly like a refused roll that found no valid ground.

| Member | Type | |
| --- | --- | --- |
| `worldName()` | `String` | read |
| `anchorPosition()` | `Vector3dc` | read |
| `anchorPlayerUuid()` | `UUID` | read |
| `realmOrdinal()` | `int` | read |
| `daoElementName()` | `String` | read |
| `setRealmOrdinal(int)` | `void` | re-tune |
| `setDaoElementName(String)` | `void` | re-tune |


---

## Merit (0.10.2)

`plugin.siren.API.MeritEvents` — Gaining Merit (功德) and ranking up on it. The gain is cancellable.

**Post-events** — fired once the change is committed; cannot be cancelled.

### `MeritGainEvent`

```java
MeritEvents.onMeritGain(event -> { /* ... */ });
```

Merit was actually credited - `total`/`multiplier` are the values AFTER the throttle and caps were applied.

| Accessor | Type |
| --- | --- |
| `player()` | `PlayerRef` |
| `deed()` | `MeritDeed` |
| `amount()` | `float` |
| `total()` | `float` |
| `multiplier()` | `float` |

### `MeritRankUpEvent`

```java
MeritEvents.onMeritRankUp(event -> { /* ... */ });
```

This player's Merit rank changed - fired once per crossing, de-duplicated the same way `DaoEvents.PathChangeEvent` is.

| Accessor | Type |
| --- | --- |
| `player()` | `PlayerRef` |
| `from()` | `MeritRank` |
| `to()` | `MeritRank` |

**Pre-events** — fired before the change; `setCancelled(true)` vetoes it, and any setter below re-tunes the numbers the mod then uses.

### `PreMeritGainEvent`

```java
MeritEvents.onPreMeritGain(event -> { /* ... */ });
```

Merit is about to be credited for a deed. Cancel to pay nothing; `setAmount` to re-weigh the deed.

| Member | Type | |
| --- | --- | --- |
| `player()` | `PlayerRef` | read (may be null) |
| `deed()` | `MeritDeed` | read |
| `amount()` | `float` | read |
| `setAmount(float)` | `void` | re-tune |


---

## Duels

`plugin.siren.API.DuelEvents` — Challenges, duel start/end and Qi wager payouts.

**Enums declared here**

- `DuelEvents.DuelEndReason` — How a duel stopped being active. Values: `DEATH`, `YIELD`, `VOIDED`

**Post-events** — fired once the change is committed; cannot be cancelled.

### `DuelChallengeEvent`

```java
DuelEvents.onDuelChallenge(event -> { /* ... */ });
```

A challenge was issued and is now pending the other player's answer.

| Accessor | Type |
| --- | --- |
| `challenger()` | `UUID` |
| `challenged()` | `UUID` |
| `wager()` | `int` |

### `DuelDeclineEvent`

```java
DuelEvents.onDuelDecline(event -> { /* ... */ });
```

A challenge was declined; no duel started.

| Accessor | Type |
| --- | --- |
| `challenger()` | `UUID` |
| `challenged()` | `UUID` |

### `DuelStartEvent`

```java
DuelEvents.onDuelStart(event -> { /* ... */ });
```

A duel is now live - both players are flagged as dueling.

| Accessor | Type |
| --- | --- |
| `challenger()` | `UUID` |
| `challenged()` | `UUID` |
| `wager()` | `int` |

### `DuelEndEvent`

```java
DuelEvents.onDuelEnd(event -> { /* ... */ });
```

A duel ended. For DEATH/YIELD, `winner`/`loser` are meaningful and the payout has been queued; for VOIDED they are simply the two participants and nothing changes hands. @param killer for a DEATH, the uuid of the player whose own damage actually killed the loser, or null when the killing blow was not a player's (lava, a fall, drowning, a formation trap) or could not be attributed. Always null for YIELD and VOIDED. Added for Tournament Wagers, which must not pay out on a death the winner did not cause - see `WagerEvents.WagerVoidReason.NOT_OPPONENT_KILL`. It is a plain extra record component and every existing listener keeps compiling; the four-argument constructor below still exists for callers that genuinely have no attribution.

| Accessor | Type |
| --- | --- |
| `winner()` | `UUID` |
| `loser()` | `UUID` |
| `wager()` | `int` |
| `reason()` | `DuelEndReason` |
| `killer()` | `UUID` |

### `DuelPayoutEvent`

```java
DuelEvents.onDuelPayout(event -> { /* ... */ });
```

A decided duel's wager actually moved: `amount` is what the loser could cover, which is exactly what the winner gained.

| Accessor | Type |
| --- | --- |
| `winner()` | `UUID` |
| `loser()` | `UUID` |
| `amount()` | `int` |

**Pre-events** — fired before the change; `setCancelled(true)` vetoes it, and any setter below re-tunes the numbers the mod then uses.

### `PreDuelChallengeEvent`

```java
DuelEvents.onPreDuelChallenge(event -> { /* ... */ });
```

A challenge is about to be issued. Cancel to refuse it; `setWager` to force a different stake (the configured maximum is re-checked afterward).

| Member | Type | |
| --- | --- | --- |
| `challenger()` | `UUID` | read |
| `challenged()` | `UUID` | read |
| `wager()` | `int` | read |
| `setWager(int)` | `void` | re-tune |

### `PreDuelStartEvent`

```java
DuelEvents.onPreDuelStart(event -> { /* ... */ });
```

A duel is about to start. Cancel to refuse it - the challenge is consumed either way, so the challenger must issue a fresh one.

| Member | Type | |
| --- | --- | --- |
| `challenger()` | `UUID` | read |
| `challenged()` | `UUID` | read |
| `wager()` | `int` | read |
| `setWager(int)` | `void` | re-tune |

### `PreDuelPayoutEvent`

```java
DuelEvents.onPreDuelPayout(event -> { /* ... */ });
```

A decided duel's wager is about to move. Cancel to let the winner take nothing; `setAmount` to re-scale the transfer (the loser can still only forfeit what they actually hold).

| Member | Type | |
| --- | --- | --- |
| `winner()` | `UUID` | read |
| `loser()` | `UUID` | read |
| `amount()` | `int` | read |
| `setAmount(int)` | `void` | re-tune |


---

## Dao Duels (0.10.1)

`plugin.siren.API.DaoDuelEvents` — The Spirit Stone escrow layered on top of `DuelEvents`' plain Qi-wager duels: the challenge and the payout are cancellable, and the end is reported once the escrow settles.

**Enums declared here**

- `DaoDuelEvents.DaoDuelEndReason` — How a decided Dao Duel was resolved. `DaoDuelManager`'s allowed `DuelManager` surface (`onDuelEnd` only, no `onDuelPayout`) never observes a yield directly, but `DuelYieldCmd` now calls `DaoDuelManager#reportYield` explicitly right after ending the underlying Qi duel (Fix 2 - see `DaoDuelManager`'s own class doc, "The known gap this still leaves: duel-yield"), which attributes the yielder as the loser and fires this event with `YIELD`. Values: `DEATH`, `YIELD`

**Post-events** — fired once the change is committed; cannot be cancelled.

### `DaoDuelEndEvent`

```java
DaoDuelEvents.onDaoDuelEnd(event -> { /* ... */ });
```

A decided Dao Duel's Spirit Stone pot has been settled - `spiritStoneAmountPaid` is what actually moved from the loser's escrow (plus any elemental bonus that could be charged) to the winner; the winner's own stake is always returned to them separately and is not counted here. `winnerElement`/`loserElement` may be null if a side had no chosen element at resolution time (only possible when `DaoDuel-Requires-Chosen-Element` is off) - `winnerCountered` is false whenever either is null. `decidedByOpponentDamage` is true ONLY when the loser died to the winner's own damage (their weapon, their technique, or their spirit beast, which is credited to its owner). It is false for a `DaoDuelEndReason#YIELD`, and false for a death to lava, a fall, drowning, a formation trap, a third party, or a source that could not be attributed. It exists for `TournamentWagerManager`, which pays real Spirit Stones to spectators and must not pay on a "win" the winner did not cause - the duel itself still resolves identically either way (see `DuelManager#endDuel(UUID, DuelEvents.DuelEndReason, UUID)`).

| Accessor | Type |
| --- | --- |
| `winner()` | `UUID` |
| `loser()` | `UUID` |
| `spiritStoneAmountPaid()` | `long` |
| `winnerElement()` | `DaoElement` |
| `loserElement()` | `DaoElement` |
| `winnerCountered()` | `boolean` |
| `reason()` | `DaoDuelEndReason` |
| `decidedByOpponentDamage()` | `boolean` |

**Pre-events** — fired before the change; `setCancelled(true)` vetoes it, and any setter below re-tunes the numbers the mod then uses.

### `PreDaoDuelChallengeEvent`

```java
DaoDuelEvents.onPreDaoDuelChallenge(event -> { /* ... */ });
```

A Dao Duel challenge is about to be issued (the Qi wager, if any, is handled entirely by `DuelEvents.PreDuelChallengeEvent` - this is the Spirit Stone side only). Cancel to refuse it; `setSpiritStoneAmount` to re-tune the stake (the configured maximum is re-checked afterward).

| Member | Type | |
| --- | --- | --- |
| `challenger()` | `UUID` | read |
| `challenged()` | `UUID` | read |
| `spiritStoneAmount()` | `long` | read |
| `setSpiritStoneAmount(long)` | `void` | re-tune |

### `PreDaoDuelPayoutEvent`

```java
DaoDuelEvents.onPreDaoDuelPayout(event -> { /* ... */ });
```

A decided Dao Duel's Spirit Stone pot is about to move. Cancel to return the loser's stake to the loser untouched; `setAmount` to re-scale how much of the loser's escrowed stake is forfeited (capped by what was actually escrowed - the winner's own stake is unaffected either way, see `DaoDuelEndEvent`'s own doc).

| Member | Type | |
| --- | --- | --- |
| `winner()` | `UUID` | read |
| `loser()` | `UUID` | read |
| `amount()` | `long` | read |
| `setAmount(long)` | `void` | re-tune |


---

## Tournament Wagers (0.10.2)

`plugin.siren.API.WagerEvents` — Spectators staking Spirit Stones on individual Dao Duel Tournament matches (a parimutuel market). *The two market-resolution events fire while the tournament's monitor is held* - zero locking and zero cross-manager calls in those listeners. Read the class javadoc first.

**Enums declared here**

- `WagerEvents.WagerVoidReason` — Why a market paid nobody and returned every stake untouched. Values: `FORFEIT`, `DOUBLE_ELIMINATION`, `VOIDED_DUEL`, `YIELD`, `NOT_OPPONENT_KILL`, `TOO_FEW_BETTORS`, `POOL_TOO_THIN`, `TOURNAMENT_RESET`, `ACCOUNTING_FAILURE`

**Post-events** — fired once the change is committed; cannot be cancelled.

### `WagerPlacedEvent`

```java
WagerEvents.onWagerPlaced(event -> { /* ... */ });
```

A spectator's Spirit Stones have been charged and their stake recorded. `backed` is the duelist they are backing.

| Accessor | Type |
| --- | --- |
| `matchNumber()` | `int` |
| `bettor()` | `UUID` |
| `backed()` | `UUID` |
| `amount()` | `long` |

### `WagerMarketResolvedEvent`

```java
WagerEvents.onWagerMarketResolved(event -> { /* ... */ });
```

A market resolved and paid out. @param payouts bettor uuid to the TOTAL Spirit Stones handed back to them - their own returned stake plus their share of the losing pool. A bettor who backed the loser is present with a payout of 0 only if they are also in `stakes`; read `stakes` for what each one put in. Both maps are unmodifiable. @param stakes bettor uuid to what they originally staked. @param burned the house cut plus the indivisible remainder - Spirit Stones destroyed rather than paid to anyone. There is no house account and no organizer; see `TournamentWagerManager`'s own doc.

| Accessor | Type |
| --- | --- |
| `matchNumber()` | `int` |
| `winner()` | `UUID` |
| `stakes()` | `Map<UUID, Long>` |
| `payouts()` | `Map<UUID, Long>` |
| `winnersPool()` | `long` |
| `losersPool()` | `long` |
| `burned()` | `long` |

### `WagerMarketVoidedEvent`

```java
WagerEvents.onWagerMarketVoided(event -> { /* ... */ });
```

A market paid nobody; every stake in `refunds` was returned in full. Unmodifiable.

| Accessor | Type |
| --- | --- |
| `matchNumber()` | `int` |
| `reason()` | `WagerVoidReason` |
| `refunds()` | `Map<UUID, Long>` |

**Pre-events** — fired before the change; `setCancelled(true)` vetoes it, and any setter below re-tunes the numbers the mod then uses.

### `PreWagerPlacedEvent`

```java
WagerEvents.onPreWagerPlaced(event -> { /* ... */ });
```

A spectator is about to be charged for a stake. Every configured gate (feature enabled, market open, not a duelist, not a live entrant, not a sect-mate, per-bet / per-tournament / per-pool limits) has ALREADY passed by the time this fires; cancelling refuses the bet with no Spirit Stones moved and nothing recorded. The amount is deliberately not settable. Re-tuning a stake here would silently move a player's stones by an amount they never typed, and the per-bet and per-pool ceilings have already been validated against the typed figure.

| Member | Type | |
| --- | --- | --- |
| `matchNumber()` | `int` | read |
| `bettor()` | `UUID` | read |
| `backed()` | `UUID` | read |
| `amount()` | `long` | read |


---

## Combat Depth (0.10.1)

`plugin.siren.API.CombatDepthEvents` — Technique interrupts and the Wu Xing PvP reward a favorable elemental matchup pays out. Both are cancellable. Punish Windows fire no events of their own - they are a damage multiplier consumed inside the combat system.

**Post-events** — fired once the change is committed; cannot be cancelled.

### `TechniqueInterruptEvent`

```java
CombatDepthEvents.onTechniqueInterrupt(event -> { /* ... */ });
```

A charging cultivator's gathering was broken by a hard-enough hit. `damageAmount` is the (possibly re-scaled) figure `PreTechniqueInterruptEvent` settled on.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `damageAmount()` | `float` |

### `WuxingPvpRewardEvent`

```java
CombatDepthEvents.onWuxingPvpReward(event -> { /* ... */ });
```

A favorable-matchup PvP kill paid its Wu Xing reward.

| Accessor | Type |
| --- | --- |
| `killer()` | `Ref<EntityStore>` |
| `killerPlayer()` | `PlayerRef` |
| `spiritStones()` | `int` |

**Pre-events** — fired before the change; `setCancelled(true)` vetoes it, and any setter below re-tunes the numbers the mod then uses.

### `PreTechniqueInterruptEvent`

```java
CombatDepthEvents.onPreTechniqueInterrupt(event -> { /* ... */ });
```

A charging cultivator's gathering is about to be broken by a hard-enough hit. Cancel to let the gathering continue uninterrupted; `setDamageAmount` re-scales the figure carried into the paired post-event (the interrupt itself still happens once this fires - the damage that triggered it has already landed).

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read (may be null) |
| `damageAmount()` | `float` | read |
| `setDamageAmount(float)` | `void` | re-tune |

### `PreWuxingPvpRewardEvent`

```java
CombatDepthEvents.onPreWuxingPvpReward(event -> { /* ... */ });
```

A favorable-matchup PvP kill is about to pay its Wu Xing reward. Cancel to deny it; `setSpiritStones` to re-scale how many are actually granted.

| Member | Type | |
| --- | --- | --- |
| `killer()` | `Ref<EntityStore>` | read |
| `killerPlayer()` | `PlayerRef` | read |
| `spiritStones()` | `int` | read |
| `setSpiritStones(int)` | `void` | re-tune |


---

## Formations

`plugin.siren.API.FormationEvents` — Laying and dispersing spirit arrays, and trap strikes.

**Post-events** — fired once the change is committed; cannot be cancelled.

### `FormationPlaceEvent`

```java
FormationEvents.onFormationPlace(event -> { /* ... */ });
```

A spirit array was laid down and is now live on its chunk.

| Accessor | Type |
| --- | --- |
| `owner()` | `UUID` |
| `sectName()` | `String` |
| `formation()` | `Formation` |

### `FormationRemoveEvent`

```java
FormationEvents.onFormationRemove(event -> { /* ... */ });
```

A spirit array was dispersed by its controller. `formation` is the now-removed object.

| Accessor | Type |
| --- | --- |
| `owner()` | `UUID` |
| `sectName()` | `String` |
| `formation()` | `Formation` |

### `FormationTrapStrikeEvent`

```java
FormationEvents.onFormationTrapStrike(event -> { /* ... */ });
```

A Trapping array wounded an intruder standing inside it. `damage` is the post-lethality-cap amount fed to the damage pipeline (pre-armor/reduction).

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `world()` | `String` |
| `chunkX()` | `int` |
| `chunkZ()` | `int` |
| `damage()` | `float` |

### `FormationTierChangeEvent`

```java
FormationEvents.onFormationTierChange(event -> { /* ... */ });
```

An altar-anchored formation's tier just changed. Purely informational - the tier is already live on `formation` by the time this fires.

| Accessor | Type |
| --- | --- |
| `formation()` | `Formation` |
| `fromTier()` | `int` |
| `toTier()` | `int` |

**Pre-events** — fired before the change; `setCancelled(true)` vetoes it, and any setter below re-tunes the numbers the mod then uses.

### `PreFormationPlaceEvent`

```java
FormationEvents.onPreFormationPlace(event -> { /* ... */ });
```

An array is about to be laid. Cancel to refuse it (reported as the ground being warded); `setRadiusChunks` to change how far it reaches.

| Member | Type | |
| --- | --- | --- |
| `owner()` | `UUID` | read |
| `sectName()` | `String` | read |
| `world()` | `String` | read |
| `chunkX()` | `int` | read |
| `chunkZ()` | `int` | read |
| `type()` | `FormationType` | read |
| `radiusChunks()` | `int` | read |
| `setRadiusChunks(int)` | `void` | re-tune |

### `PreFormationRemoveEvent`

```java
FormationEvents.onPreFormationRemove(event -> { /* ... */ });
```

An array is about to be dispersed by its controller. Cancel to leave it standing.

| Member | Type | |
| --- | --- | --- |
| `owner()` | `UUID` | read |
| `sectName()` | `String` | read |
| `formation()` | `Formation` | read |

### `PreFormationTrapStrikeEvent`

```java
FormationEvents.onPreFormationTrapStrike(event -> { /* ... */ });
```

A Trapping array is about to wound an intruder. Cancel to spare them this tick entirely (no particle, no debuff, no damage); set `setDamage` to 0 to root them harmlessly.

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read (may be null) |
| `world()` | `String` | read |
| `chunkX()` | `int` | read |
| `chunkZ()` | `int` | read |
| `damage()` | `float` | read |
| `setDamage(float)` | `void` | re-tune |

### `PreFormationTierChangeEvent`

```java
FormationEvents.onPreFormationTierChange(event -> { /* ... */ });
```

An altar-anchored formation's tier is about to change. Cancel to refuse the change entirely; `setToTier` to dampen (but not fully block) a rise - see FormationManager.applyTier for how a downgrade-disallowing caller clamps a dampened value back.

| Member | Type | |
| --- | --- | --- |
| `formation()` | `Formation` | read |
| `fromTier()` | `int` | read |
| `toTier()` | `int` | read |
| `setToTier(int)` | `void` | re-tune |


---

## Cave Abodes

`plugin.siren.API.DwellingEvents` — Claiming, abandoning and lapsing an abode, Spirit Spring collection, upkeep and seclusion.

**Post-events** — fired once the change is committed; cannot be cancelled.

### `DwellingClaimEvent`

```java
DwellingEvents.onDwellingClaim(event -> { /* ... */ });
```

A cultivator claimed an abode, or moved an existing one. `moved` distinguishes the two; a move keeps the banked spring and paid upkeep.

| Accessor | Type |
| --- | --- |
| `owner()` | `UUID` |
| `dwelling()` | `Dwelling` |
| `moved()` | `boolean` |

### `DwellingAbandonEvent`

```java
DwellingEvents.onDwellingAbandon(event -> { /* ... */ });
```

A cultivator gave up their abode; whatever the spring held went with it.

| Accessor | Type |
| --- | --- |
| `owner()` | `UUID` |
| `dwelling()` | `Dwelling` |

### `DwellingLapseEvent`

```java
DwellingEvents.onDwellingLapse(event -> { /* ... */ });
```

A personal abode was reclaimed by the world for unpaid upkeep, past its grace period.

| Accessor | Type |
| --- | --- |
| `dwelling()` | `Dwelling` |

### `SpringCollectEvent`

```java
DwellingEvents.onSpringCollect(event -> { /* ... */ });
```

A Spirit Spring was emptied. `amount` is the Qi handed over - the caller credits it.

| Accessor | Type |
| --- | --- |
| `dwelling()` | `Dwelling` |
| `amount()` | `float` |

### `UpkeepDepositEvent`

```java
DwellingEvents.onUpkeepDeposit(event -> { /* ... */ });
```

Upkeep was paid into an abode. `hoursGranted` is what was actually banked, which is less than what was offered once the cap is hit.

| Accessor | Type |
| --- | --- |
| `dwelling()` | `Dwelling` |
| `itemId()` | `String` |
| `quantity()` | `int` |
| `hoursGranted()` | `float` |

### `SeclusionSettleEvent`

```java
DwellingEvents.onSeclusionSettle(event -> { /* ... */ });
```

A cultivator emerged from closed-door seclusion and was paid for their absence. `hours` is the capped absence; `qi` is what was actually credited.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `dwelling()` | `Dwelling` |
| `hours()` | `float` |
| `qi()` | `float` |

**Pre-events** — fired before the change; `setCancelled(true)` vetoes it, and any setter below re-tunes the numbers the mod then uses.

### `PreDwellingClaimEvent`

```java
DwellingEvents.onPreDwellingClaim(event -> { /* ... */ });
```

An abode is about to be claimed or moved. Cancel to refuse it (reported as warded ground); `setRadiusChunks` to change how far it reaches.

| Member | Type | |
| --- | --- | --- |
| `owner()` | `UUID` | read |
| `world()` | `String` | read |
| `chunkX()` | `int` | read |
| `chunkZ()` | `int` | read |
| `veinTier()` | `int` | read |
| `moved()` | `boolean` | read |
| `radiusChunks()` | `int` | read |
| `setRadiusChunks(int)` | `void` | re-tune |

### `PreDwellingAbandonEvent`

```java
DwellingEvents.onPreDwellingAbandon(event -> { /* ... */ });
```

An abode is about to be given up. Cancel to keep it standing.

| Member | Type | |
| --- | --- | --- |
| `owner()` | `UUID` | read |
| `dwelling()` | `Dwelling` | read |

### `PreDwellingLapseEvent`

```java
DwellingEvents.onPreDwellingLapse(event -> { /* ... */ });
```

An abode is about to be reclaimed for unpaid upkeep. Cancel to reprieve it - it survives until the next sweep re-tests it, so cancel from a listener that keeps deciding, not a one-off.

| Member | Type | |
| --- | --- | --- |
| `dwelling()` | `Dwelling` | read |

### `PreSpringCollectEvent`

```java
DwellingEvents.onPreSpringCollect(event -> { /* ... */ });
```

A Spirit Spring is about to be emptied. Cancel to leave it full; `setAmount` to change what the collector walks away with (the spring is emptied regardless).

| Member | Type | |
| --- | --- | --- |
| `dwelling()` | `Dwelling` | read |
| `amount()` | `float` | read |
| `setAmount(float)` | `void` | re-tune |

### `PreUpkeepDepositEvent`

```java
DwellingEvents.onPreUpkeepDeposit(event -> { /* ... */ });
```

Upkeep is about to be paid. Cancel to refuse the payment (reported as nothing banked); `setHours` to change how much time the offering buys.

| Member | Type | |
| --- | --- | --- |
| `dwelling()` | `Dwelling` | read |
| `itemId()` | `String` | read |
| `quantity()` | `int` | read |
| `hours()` | `float` | read |
| `setHours(float)` | `void` | re-tune |

### `PreSeclusionSettleEvent`

```java
DwellingEvents.onPreSeclusionSettle(event -> { /* ... */ });
```

A seclusion retreat is about to pay out. Cancel to forfeit it (reported to the player as a dry spring); `setQi` to re-scale the reward.

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read (may be null) |
| `dwelling()` | `Dwelling` | read |
| `hours()` | `float` | read |
| `qi()` | `float` | read |
| `setQi(float)` | `void` | re-tune |


---

## Celestial events

`plugin.siren.API.CelestialEvents` — Server-wide phenomena - Spirit Tide, Meteor Shower, Blood Moon, and any an addon registered through `CelestialManager.registerEventType`. Both hooks are generic to every type rather than one pair per phenomenon, so switch on `type().id()` to react to a particular one.

**Post-events** — fired once the change is committed; cannot be cancelled.

### `CelestialEventStartEvent`

```java
CelestialEvents.onCelestialEventStart(event -> { /* ... */ });
```

A celestial event just started; its sky is already live.

| Accessor | Type |
| --- | --- |
| `type()` | `CelestialEventType` |
| `startedAtMillis()` | `long` |
| `endsAtMillis()` | `long` |
| `forcedByAdmin()` | `boolean` |

### `CelestialEventEndEvent`

```java
CelestialEvents.onCelestialEventEnd(event -> { /* ... */ });
```

A celestial event just ended; its sky is already clearing.

| Accessor | Type |
| --- | --- |
| `type()` | `CelestialEventType` |
| `startedAtMillis()` | `long` |
| `endedAtMillis()` | `long` |
| `forcedByAdmin()` | `boolean` |

**Pre-events** — fired before the change; `setCancelled(true)` vetoes it, and any setter below re-tunes the numbers the mod then uses.

### `PreCelestialEventStartEvent`

```java
CelestialEvents.onPreCelestialEventStart(event -> { /* ... */ });
```

A celestial event is about to start. Cancel to skip this pick entirely (the scheduler simply waits for its next check rather than substituting another event, so a listener that vetoes every pick would leave none running - the same "silence is a valid answer" shape `PreSectJoinEvent` has); `setDurationMinutes` to run it longer or shorter than `CelestialEventType#durationMinutes()`.

| Member | Type | |
| --- | --- | --- |
| `type()` | `CelestialEventType` | read |
| `durationMinutes()` | `float` | read |
| `forcedByAdmin()` | `boolean` | read |
| `setDurationMinutes(float)` | `void` | re-tune |


---

## Body tempering

`plugin.siren.API.BodyTemperingEvents` — The second ladder, climbed by taking blows rather than by gathering Qi: XP earned from damage that reached the body, and the levels it buys. The pre-XP event carries a MUTABLE amount, so a listener can scale the reward rather than only allow or forbid it.

**Post-events** — fired once the change is committed; cannot be cancelled.

### `XpGainEvent`

```java
BodyTemperingEvents.onXpGain(event -> { /* ... */ });
```

XP was banked. `amount` is what was actually granted, after any pre-event scaling.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `level()` | `int` |
| `amount()` | `float` |

### `LevelUpEvent`

```java
BodyTemperingEvents.onLevelUp(event -> { /* ... */ });
```

A body gained a level. Fires once per level when a single blow crosses several.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `fromLevel()` | `int` |
| `toLevel()` | `int` |

### `StageBreakthroughEvent`

```java
BodyTemperingEvents.onStageBreakthrough(event -> { /* ... */ });
```

A body crossed into a new Tempering Stage (锻体境) - the 9-rung milestone derived from the level above (see `BodyTemperingManager.getTemperingStage`). Fires at most once per `addXp` call even if a single huge blow crossed several levels at once, since the stage is read once before and once after the whole level-up loop, not per level.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `fromStage()` | `int` |
| `toStage()` | `int` |

**Pre-events** — fired before the change; `setCancelled(true)` vetoes it, and any setter below re-tunes the numbers the mod then uses.

### `PreXpGainEvent`

```java
BodyTemperingEvents.onPreXpGain(event -> { /* ... */ });
```

About to bank XP for a blow. Unusually, this one is not merely cancellable - it carries a mutable amount, so a listener can scale the reward rather than being limited to allowing or forbidding it. Setting the amount to 0 or below cancels the gain outright.

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read (may be null) |
| `level()` | `int` | read |
| `amount()` | `float` | read |
| `setAmount(float)` | `void` | re-tune |

### `PreLevelUpEvent`

```java
BodyTemperingEvents.onPreLevelUp(event -> { /* ... */ });
```

About to gain a level. Cancelling holds the body where it is; the XP stays banked.

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read (may be null) |
| `fromLevel()` | `int` | read |
| `toLevel()` | `int` | read |

### `PreStageBreakthroughEvent`

```java
BodyTemperingEvents.onPreStageBreakthrough(event -> { /* ... */ });
```

About to celebrate a Tempering Stage breakthrough. Cancellable, but unlike `PreLevelUpEvent` cancelling this does NOT hold the body at its old stage - it can't, since the stage is derived from the level (already banked by the time this fires) rather than stored on its own. What cancelling suppresses is the CEREMONY: the title, sound, particle and broadcast a real stage breakthrough gets. Refuse it to run your own presentation instead of - or in place of - the built-in one.

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read (may be null) |
| `fromStage()` | `int` | read |
| `toStage()` | `int` | read |


---

## Fist arts

`plugin.siren.API.FistEvents` — The third ladder, climbed by landing blows bare-handed - the mirror of body tempering, whose income is damage received. XP is measured by the damage that actually got through, so a punch a shield ate whole teaches nothing. The pre-XP event carries a MUTABLE amount, so a listener can scale the reward rather than only allow or forbid it.

**Post-events** — fired once the change is committed; cannot be cancelled.

### `XpGainEvent`

```java
FistEvents.onXpGain(event -> { /* ... */ });
```

XP was banked. `amount` is what was actually granted, after any pre-event scaling.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `level()` | `int` |
| `amount()` | `float` |

### `LevelUpEvent`

```java
FistEvents.onLevelUp(event -> { /* ... */ });
```

A cultivator's fists gained a level. Fires once per level when one blow crosses several.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `fromLevel()` | `int` |
| `toLevel()` | `int` |

**Pre-events** — fired before the change; `setCancelled(true)` vetoes it, and any setter below re-tunes the numbers the mod then uses.

### `PreXpGainEvent`

```java
FistEvents.onPreXpGain(event -> { /* ... */ });
```

About to bank XP for a bare-handed blow. Like the tempering equivalent this is not merely cancellable - it carries a mutable amount, so a listener can scale the reward rather than being limited to allowing or forbidding it. Setting the amount to 0 or below cancels the gain outright.

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read (may be null) |
| `level()` | `int` | read |
| `amount()` | `float` | read |
| `setAmount(float)` | `void` | re-tune |

### `PreLevelUpEvent`

```java
FistEvents.onPreLevelUp(event -> { /* ... */ });
```

About to gain a level. Cancelling holds the cultivator where they are; the XP stays banked.

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read (may be null) |
| `fromLevel()` | `int` | read |
| `toLevel()` | `int` | read |


---

## Meridian injuries (0.9.x)

`plugin.siren.API.MeridianEvents` — A named injury being inflicted or deepened, cured, and a Cracked Dantian's Qi spill being armed.

**Post-events** — fired once the change is committed; cannot be cancelled.

### `MeridianInjuryEvent`

```java
MeridianEvents.onMeridianInjury(event -> { /* ... */ });
```

An injury was inflicted (or deepened) - the write has already landed.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `injury()` | `MeridianInjury` |
| `magnitude()` | `float` |
| `durationSeconds()` | `float` |

### `MeridianCureEvent`

```java
MeridianEvents.onMeridianCure(event -> { /* ... */ });
```

An injury fully cleared (wait-it-out, meditation recovery, or an explicit cure).

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `injury()` | `MeridianInjury` |
| `cureSource()` | `String` |

**Pre-events** — fired before the change; `setCancelled(true)` vetoes it, and any setter below re-tunes the numbers the mod then uses.

### `PreMeridianInjuryEvent`

```java
MeridianEvents.onPreMeridianInjury(event -> { /* ... */ });
```

An injury is about to be inflicted (or deepened, if already carried). Cancel to refuse it outright; `setMagnitude`/`setDurationSeconds` to re-scale the roll before it is written.

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read (may be null) |
| `injury()` | `MeridianInjury` | read |
| `cause()` | `String` | read |
| `magnitude()` | `float` | read |
| `durationSeconds()` | `float` | read |
| `setMagnitude(float)` | `void` | re-tune |
| `setDurationSeconds(float)` | `void` | re-tune |

### `PreMeridianCureEvent`

```java
MeridianEvents.onPreMeridianCure(event -> { /* ... */ });
```

An injury is about to be cured. Cancel to refuse it (it stays active).

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read (may be null) |
| `injury()` | `MeridianInjury` | read |
| `cureSource()` | `String` | read |

### `PreMeridianSpillEvent`

```java
MeridianEvents.onPreMeridianSpill(event -> { /* ... */ });
```

A Cracked Dantian's Qi spill is about to be armed (fired once at inflict/deepen, NOT per-tick). Cancel to cap future Qi gain at the new ceiling without draining the excess already banked; `setRatePerSecond` to re-tune how fast the excess drains.

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read (may be null) |
| `excessQi()` | `float` | read |
| `ratePerSecond()` | `float` | read |
| `setRatePerSecond(float)` | `void` | re-tune |


---

## Cultivation profiles

`plugin.siren.API.ProfileEvents` — Switching, creating and erasing the separate saves a player keeps of their own progress, and the expiry of a temporary sandbox profile.

**Post-events** — fired once the change is committed; cannot be cancelled.

### `ProfileSwitchEvent`

```java
ProfileEvents.onProfileSwitch(event -> { /* ... */ });
```

A player is now on a different profile. Their components have already been replaced, so anything read here describes the cultivator they switched TO. @param from the profile they left, or null when they were placed on one without leaving another (a first-time backfill)

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `from()` | `Profile` |
| `to()` | `Profile` |

### `ProfileCreateEvent`

```java
ProfileEvents.onProfileCreate(event -> { /* ... */ });
```

A new, empty profile was created and switched to.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `profile()` | `Profile` |

### `ProfileDeleteEvent`

```java
ProfileEvents.onProfileDelete(event -> { /* ... */ });
```

A profile was erased. It is already off the player's list.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `profile()` | `Profile` |

### `ProfileExpireEvent`

```java
ProfileEvents.onProfileExpire(event -> { /* ... */ });
```

A temp profile's time ran out and it was removed. @param wasActive whether the player was playing it, and so has just been put back on a real one

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `profile()` | `Profile` |
| `wasActive()` | `boolean` |

**Pre-events** — fired before the change; `setCancelled(true)` vetoes it, and any setter below re-tunes the numbers the mod then uses.

### `PreProfileSwitchEvent`

```java
ProfileEvents.onPreProfileSwitch(event -> { /* ... */ });
```

A profile is about to be swapped in. Cancel to refuse the switch. Fired BEFORE the outgoing profile is saved, so this is the point at which an addon's own state still belongs to the cultivator being left - save it here, keyed by `from()`, and restore it in `ProfileSwitchEvent`.

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read |
| `from()` | `Profile` | read (may be null) |
| `to()` | `Profile` | read |

### `PreProfileCreateEvent`

```java
ProfileEvents.onPreProfileCreate(event -> { /* ... */ });
```

A new profile is about to be created. Cancel to refuse it.

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read |

### `PreProfileDeleteEvent`

```java
ProfileEvents.onPreProfileDelete(event -> { /* ... */ });
```

A profile is about to be erased. Cancel to keep it.

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read |
| `profile()` | `Profile` | read |


---

## Parties (0.9.x)

`plugin.siren.API.PartyEvents` — Ad-hoc, session-only grouping - the foundation for a later multiplayer dungeon feature that is not built yet. Forming, joining, leaving, disbanding, and inviting.

**Post-events** — fired once the change is committed; cannot be cancelled.

### `PartyFormedEvent`

```java
PartyEvents.onPartyFormed(event -> { /* ... */ });
```

A solo leader's first invite was accepted - the party now exists as a group, not just a leader waiting alone.

| Accessor | Type |
| --- | --- |
| `leaderUuid()` | `UUID` |
| `firstMemberUuid()` | `UUID` |

### `PartyMemberJoinedEvent`

```java
PartyEvents.onPartyMemberJoined(event -> { /* ... */ });
```

A cultivator joined an already-formed party (i.e. not the party's very first member - see `PartyFormedEvent`).

| Accessor | Type |
| --- | --- |
| `leaderUuid()` | `UUID` |
| `memberUuid()` | `UUID` |

### `PartyMemberLeftEvent`

```java
PartyEvents.onPartyMemberLeft(event -> { /* ... */ });
```

A cultivator left a party that still has members remaining afterward. If the leader left, `leaderUuid` is the newly promoted leader.

| Accessor | Type |
| --- | --- |
| `leaderUuid()` | `UUID` |
| `memberUuid()` | `UUID` |

### `PartyDisbandedEvent`

```java
PartyEvents.onPartyDisbanded(event -> { /* ... */ });
```

A party stopped existing - either the leader disbanded it outright, or its last member left. `formerMembers` is a snapshot, not a live view.

| Accessor | Type |
| --- | --- |
| `leaderUuid()` | `UUID` |
| `formerMembers()` | `Set<UUID>` |

**Pre-events** — fired before the change; `setCancelled(true)` vetoes it, and any setter below re-tunes the numbers the mod then uses.

### `PrePartyInviteEvent`

```java
PartyEvents.onPrePartyInvite(event -> { /* ... */ });
```

An invite is about to be sent. Cancel to refuse it silently (the inviter still receives the manager's own result).

| Member | Type | |
| --- | --- | --- |
| `inviterUuid()` | `UUID` | read |
| `targetUuid()` | `UUID` | read |


---

## Partnered Cultivation (0.9.x)

`plugin.siren.API.PartnerEvents` — Two married cultivators drawing on the same spirit vein together, resolved every meditation tick - pairing, unpairing, and the Qi bonus the pairing grants. Requires Marriage; see `docs/compatibility.md`.

**Post-events** — fired once the change is committed; cannot be cancelled.

### `PartnerPairedEvent`

```java
PartnerEvents.onPartnerPaired(event -> { /* ... */ });
```

A cultivator transitioned from unpartnered to partnered - both spouses sat down to meditate within Partner-Radius-Blocks of each other in the same world. Fired once per side (each spouse gets their own event, with `ref`/`player` naming THEM and `partnerUuid` naming their spouse), the moment the transition is detected rather than on a timer.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `partnerUuid()` | `UUID` |

### `PartnerUnpairedEvent`

```java
PartnerEvents.onPartnerUnpaired(event -> { /* ... */ });
```

A cultivator transitioned from partnered back to unpartnered - their spouse stood up, wandered out of radius, changed world, or the pairing otherwise lapsed. `formerPartnerUuid` is who they were partnered with a moment ago. Not fired for the spouse who themselves stood up first; see `PartnerManager.announceTransition`'s own javadoc for why only the side still seated is told.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `formerPartnerUuid()` | `UUID` |

**Pre-events** — fired before the change; `setCancelled(true)` vetoes it, and any setter below re-tunes the numbers the mod then uses.

### `PrePartnerQiBonusEvent`

```java
PartnerEvents.onPrePartnerQiBonus(event -> { /* ... */ });
```

A partnered cultivator's meditation Qi bonus is about to apply. Cancel to deny the bonus entirely for this tick (equivalent to sitting alone); adjust `setMultiplier` to re-scale how much extra Qi this specific pairing draws.

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read (may be null) |
| `partnerUuid()` | `UUID` | read (may be null) |
| `multiplier()` | `float` | read |
| `setMultiplier(float)` | `void` | re-tune |


---

## Heavenly Oaths (0.9.x)

`plugin.siren.API.OathEvents` — Swearing, breaching and peacefully dissolving a Heavenly Oath (天道誓言), and cleansing the Dao-Heart Flaw a breach leaves behind.

**Enums declared here**

- `OathEvents.FlawCleanseRoute` — How a Dao-Heart Flaw stopped being active - see `OathManager`'s three cleanse routes. Values: `EXPIRED`, `ITEM`, `COMPANION`

**Post-events** — fired once the change is committed; cannot be cancelled.

### `OathSwornEvent`

```java
OathEvents.onOathSworn(event -> { /* ... */ });
```

A pending offer was accepted and the oath is now sworn (binding).

| Accessor | Type |
| --- | --- |
| `oathId()` | `String` |
| `type()` | `OathType` |
| `partyA()` | `UUID` |
| `partyB()` | `UUID` |
| `stake()` | `int` |
| `sectId()` | `String` |

### `OathBreachEvent`

```java
OathEvents.onOathBreach(event -> { /* ... */ });
```

A sworn oath was broken and its penalty has already been applied.

| Accessor | Type |
| --- | --- |
| `oathId()` | `String` |
| `type()` | `OathType` |
| `breaker()` | `UUID` |
| `victim()` | `UUID` |
| `qiLost()` | `float` |
| `karmaGained()` | `float` |

### `OathFlawCleanseEvent`

```java
OathEvents.onOathFlawCleanse(event -> { /* ... */ });
```

A cultivator's Dao-Heart Flaw was cleansed by `route`.

| Accessor | Type |
| --- | --- |
| `player()` | `UUID` |
| `route()` | `FlawCleanseRoute` |

### `OathDissolveEvent`

```java
OathEvents.onOathDissolve(event -> { /* ... */ });
```

A sworn oath was peacefully DISSOLVED - no penalty, either via a mutual `/cultivation oath dissolve` or the one system-triggered no-fault case (see `OathManager#dissolveActiveOath`).

| Accessor | Type |
| --- | --- |
| `oathId()` | `String` |
| `type()` | `OathType` |
| `partyA()` | `UUID` |
| `partyB()` | `UUID` |

**Pre-events** — fired before the change; `setCancelled(true)` vetoes it, and any setter below re-tunes the numbers the mod then uses.

### `PreOathSwearEvent`

```java
OathEvents.onPreOathSwear(event -> { /* ... */ });
```

A pending offer is about to be accepted and become a binding oath. Cancel to refuse it - the offer is consumed either way, mirroring `DuelEvents.PreDuelStartEvent`, so the offerer must send a fresh one.

| Member | Type | |
| --- | --- | --- |
| `offerer()` | `UUID` | read |
| `accepter()` | `UUID` | read |
| `type()` | `OathType` | read |
| `stake()` | `int` | read |
| `sectId()` | `String` | read |
| `setStake(int)` | `void` | re-tune |

### `PreOathBreachEvent`

```java
OathEvents.onPreOathBreach(event -> { /* ... */ });
```

A sworn oath is about to be recorded as broken and its penalty applied. Cancel to pardon the breach outright (nothing changes - no Qi loss, no karma, no flaw); adjust the setters to re-tune the penalty instead.

| Member | Type | |
| --- | --- | --- |
| `oathId()` | `String` | read |
| `type()` | `OathType` | read |
| `breaker()` | `UUID` | read |
| `victim()` | `UUID` | read |
| `qiLossPercent()` | `float` | read |
| `karmaSwing()` | `float` | read |
| `flawDurationMinutes()` | `float` | read |
| `flawSeverity()` | `float` | read |
| `setQiLossPercent(float)` | `void` | re-tune |
| `setKarmaSwing(float)` | `void` | re-tune |
| `setFlawDurationMinutes(float)` | `void` | re-tune |
| `setFlawSeverity(float)` | `void` | re-tune |

### `PreOathFlawCleanseEvent`

```java
OathEvents.onPreOathFlawCleanse(event -> { /* ... */ });
```

A Dao-Heart Flaw is about to be cleansed. Cancel to refuse the attempt (the item, if any, is still the caller's to decide whether to consume - see the call site).

| Member | Type | |
| --- | --- | --- |
| `player()` | `UUID` | read |
| `route()` | `FlawCleanseRoute` | read |

### `PreOathDissolveEvent`

```java
OathEvents.onPreOathDissolve(event -> { /* ... */ });
```

A sworn oath is about to be peacefully DISSOLVED. Cancel to keep it active (no change - the requesting side must ask again).

| Member | Type | |
| --- | --- | --- |
| `oathId()` | `String` | read |
| `type()` | `OathType` | read |
| `partyA()` | `UUID` | read |
| `partyB()` | `UUID` | read |


---

## Narrative Campaign (0.9.x)

`plugin.siren.API.CampaignEvents` — The quest-line system's chapter advances (including a campaign's very first chapter) and campaign completion.

**Post-events** — fired once the change is committed; cannot be cancelled.

### `CampaignChapterAdvanceEvent`

```java
CampaignEvents.onCampaignChapterAdvance(event -> { /* ... */ });
```

A chapter just became current for a player - either the campaign's very first chapter (via `CampaignManager.start`) or an advance off a finished one. `newChapterIndex` may still be locked behind its own realm floor; check `CampaignManager.getProgress` if that matters to a listener.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `campaignId()` | `String` |
| `previousChapterIndex()` | `int` |
| `newChapterIndex()` | `int` |

### `CampaignCompleteEvent`

```java
CampaignEvents.onCampaignComplete(event -> { /* ... */ });
```

Every chapter of a campaign is finished. Fires exactly once per run, the moment the last chapter's chain(s) complete.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `campaignId()` | `String` |

**Pre-events** — fired before the change; `setCancelled(true)` vetoes it, and any setter below re-tunes the numbers the mod then uses.

### `PreCampaignChapterAdvanceEvent`

```java
CampaignEvents.onPreCampaignChapterAdvance(event -> { /* ... */ });
```

A player is about to move into a new chapter (or begin the campaign's first one, when `fromChapterIndex` is `-1`). Every built-in refusal (realm gate, already active/completed) has already passed; cancel to refuse it anyway. Nothing is written when a listener cancels - the chapter index is not advanced and no chain is accepted.

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read (may be null) |
| `campaignId()` | `String` | read |
| `fromChapterIndex()` | `int` | read |
| `toChapterIndex()` | `int` | read |


---

## Wandering-NPC quests (0.9.x)

`plugin.siren.API.QuestEvents` — Accepting a quest chain from a wandering NPC giver, advancing through its steps, completing it (reward fully paid), or abandoning it.

**Post-events** — fired once the change is committed; cannot be cancelled.

### `QuestAcceptEvent`

```java
QuestEvents.onQuestAccept(event -> { /* ... */ });
```

A player has taken on a quest chain - the progress row is written and, for a site chain, its site is already rolled.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `chainId()` | `String` |
| `giverRoleId()` | `String` |

### `QuestStepAdvanceEvent`

```java
QuestEvents.onQuestStepAdvance(event -> { /* ... */ });
```

A step of a chain just cleared. @param completedStepIndex the 0-based index of the step that was finished. @param nextStepIndex the 0-based index now current, or `completedStepIndex + 1` past the end when the chain has run out of steps - check `finalStep()` rather than comparing against a step count. @param finalStep true if this was the last step, so the run has moved to awaiting its reward.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `chainId()` | `String` |
| `completedStepIndex()` | `int` |
| `nextStepIndex()` | `int` |
| `finalStep()` | `boolean` |

### `QuestCompleteEvent`

```java
QuestEvents.onQuestComplete(event -> { /* ... */ });
```

A chain is fully finished AND fully paid - every reward component landed. Fires exactly once per run, at the moment the run turns COMPLETED.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `chainId()` | `String` |
| `completionCount()` | `int` |

### `QuestAbandonEvent`

```java
QuestEvents.onQuestAbandon(event -> { /* ... */ });
```

A player gave up on an in-progress run. Never fires for a run that was awaiting a reward - that one cannot be abandoned.

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `chainId()` | `String` |
| `stepIndex()` | `int` |

**Pre-events** — fired before the change; `setCancelled(true)` vetoes it, and any setter below re-tunes the numbers the mod then uses.

### `PreQuestAcceptEvent`

```java
QuestEvents.onPreQuestAccept(event -> { /* ... */ });
```

A player is about to take on a chain. Every built-in refusal (realm gate, once-per-account, cooldown, chain cap) has already passed; cancel to refuse it anyway. Nothing is written when a listener cancels - see the class javadoc.

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read |
| `player()` | `PlayerRef` | read (may be null) |
| `chainId()` | `String` | read |
| `giverRoleId()` | `String` | read |


---

## Secret Realm Depths (0.9.x)

`plugin.siren.API.DepthsEvents` — A solo Depths run: starting, a floor clearing (with the escrow reward it just rolled), extraction actually paying out, and the run ending for any reason. Post-only - every one of these is a deterministic outcome of the run's own state machine.

**Post-events** — fired once the change is committed; cannot be cancelled.

### `DepthsRunStartEvent`

```java
DepthsEvents.onDepthsRunStart(event -> { /* ... */ });
```

A solo Depths run has started, floor 1 about to spawn.

| Accessor | Type |
| --- | --- |
| `runId()` | `String` |
| `ownerUuid()` | `UUID` |
| `siteId()` | `String` |
| `world()` | `String` |

### `DepthsFloorClearEvent`

```java
DepthsEvents.onDepthsFloorClear(event -> { /* ... */ });
```

A floor's beasts are all dead - `escrowSize` is the run's TOTAL unbanked escrow count after this floor's roll (0 or 1 higher than before it, since a roll can miss).

| Accessor | Type |
| --- | --- |
| `runId()` | `String` |
| `ownerUuid()` | `UUID` |
| `floor()` | `int` |
| `escrowSize()` | `int` |

### `DepthsExtractEvent`

```java
DepthsEvents.onDepthsExtract(event -> { /* ... */ });
```

The run's escrow was just actually granted - fired only when `rewardsGranted` is above zero, whether the player chose Extract or the run auto-extracted (logout/realm-close).

| Accessor | Type |
| --- | --- |
| `runId()` | `String` |
| `ownerUuid()` | `UUID` |
| `depthReached()` | `int` |
| `rewardsGranted()` | `int` |

### `DepthsRunEndEvent`

```java
DepthsEvents.onDepthsRunEnd(event -> { /* ... */ });
```

The run is over, for any reason - fired once, after any `DepthsExtractEvent` the same ending also produced.

| Accessor | Type |
| --- | --- |
| `runId()` | `String` |
| `ownerUuid()` | `UUID` |
| `reason()` | `DepthsRun.EndReason` |
| `depthReached()` | `int` |


---

## Secret Realms (0.9.x)

`plugin.siren.API.SecretRealmEvents` — A site's barrier coming down (openable) or going back up (closed). Post-only - opening/closing is a deterministic scheduler outcome, not a request anything downstream could meaningfully veto.

**Post-events** — fired once the change is committed; cannot be cancelled.

### `SecretRealmOpenEvent`

```java
SecretRealmEvents.onSecretRealmOpen(event -> { /* ... */ });
```

A Secret Realm site opened - the barrier is already down and it can be entered.

| Accessor | Type |
| --- | --- |
| `siteId()` | `String` |
| `tier()` | `SecretRealmTier` |
| `world()` | `String` |
| `chunkX()` | `int` |
| `chunkZ()` | `int` |
| `source()` | `SecretRealmSite.Source` |
| `sectName()` | `String` |
| `closesAtMillis()` | `long` |

### `SecretRealmCloseEvent`

```java
SecretRealmEvents.onSecretRealmClose(event -> { /* ... */ });
```

A Secret Realm site closed. `forced` is true only for an admin's immediate override (SecretRealmManager.forceClose); false for the realm's own natural close after its duration/grace window.

| Accessor | Type |
| --- | --- |
| `siteId()` | `String` |
| `tier()` | `SecretRealmTier` |
| `world()` | `String` |
| `chunkX()` | `int` |
| `chunkZ()` | `int` |
| `forced()` | `boolean` |


---

## Trial Pagoda (0.10.2)

`plugin.siren.API.PagodaEvents` — Clearing a floor, a run ending, and the reward, which is cancellable.

**Enums declared here**

- `PagodaEvents.PagodaRunEndReason` — Why a run ended - mirrors `plugin.siren.Utils.Pagoda.PagodaRun.EndReason`, restated here as its own public enum so an addon never needs to import the internal run class. Values: 

**Post-events** — fired once the change is committed; cannot be cancelled.

### `PagodaFloorClearedEvent`

```java
PagodaEvents.onPagodaFloorCleared(event -> { /* ... */ });
```

A Trial Pagoda floor was cleared - `newRecord` is true only on a first clear (the highest-floor record actually advanced), false on a replay.

| Accessor | Type |
| --- | --- |
| `playerUuid()` | `UUID` |
| `floor()` | `int` |
| `newRecord()` | `boolean` |

### `PagodaRunEndedEvent`

```java
PagodaEvents.onPagodaRunEnded(event -> { /* ... */ });
```

A Trial Pagoda run ended, for any reason - `floorReached` is the floor the run was ON when it ended, not necessarily a cleared floor (a death mid-floor never advances the player's own record). Mirrors `DepthsEvents.DepthsRunEndEvent`'s own shape (no live component read needed to fire it).

| Accessor | Type |
| --- | --- |
| `playerUuid()` | `UUID` |
| `floorReached()` | `int` |
| `reason()` | `PagodaRunEndReason` |

**Pre-events** — fired before the change; `setCancelled(true)` vetoes it, and any setter below re-tunes the numbers the mod then uses.

### `PrePagodaRewardEvent`

```java
PagodaEvents.onPrePagodaReward(event -> { /* ... */ });
```

A floor-clear reward is about to be granted. Cancel to grant nothing at all; the caller still advances the run and the record either way.

| Member | Type | |
| --- | --- | --- |
| `playerUuid()` | `UUID` | read |
| `floor()` | `int` | read |
| `spiritStoneAmount()` | `long` | read |
| `qiAmount()` | `float` | read |
| `setSpiritStoneAmount(long)` | `void` | re-tune |
| `setQiAmount(float)` | `void` | re-tune |


---

## Transmission Array (0.10.3)

`plugin.siren.API.ArrayEvents` — Traveling through the Transmission Array network between a Cave Abode, a sect hall, a Secret Realm site, the Sea of Consciousness or the Heavenly Realm. The trip is cancellable.

**Post-events** — fired once the change is committed; cannot be cancelled.

### `ArrayTravelEvent`

```java
ArrayEvents.onArrayTravel(event -> { /* ... */ });
```

A cultivator successfully traveled through the array. `qiCost` is the FINAL amount actually charged (0 for an admin bypass).

| Accessor | Type |
| --- | --- |
| `ref()` | `Ref<EntityStore>` |
| `player()` | `PlayerRef` |
| `destinationKind()` | `ArrayDestination.Kind` |
| `destinationId()` | `String` |
| `qiCost()` | `float` |

**Pre-events** — fired before the change; `setCancelled(true)` vetoes it, and any setter below re-tunes the numbers the mod then uses.

### `PreArrayTravelEvent`

```java
ArrayEvents.onPreArrayTravel(event -> { /* ... */ });
```

A cultivator is about to travel through the array. Cancel to refuse the trip (nothing is charged, nothing moves); `setQiCost` to re-price it - it is charged (and re-checked against the traveler's banked Qi) after this fires, mirroring `DaoEvents.PreDaoElementChangeEvent`'s own mutable-cost shape exactly.

| Member | Type | |
| --- | --- | --- |
| `ref()` | `Ref<EntityStore>` | read (may be null) |
| `player()` | `PlayerRef` | read (may be null) |
| `destinationKind()` | `ArrayDestination.Kind` | read |
| `destinationId()` | `String` | read |
| `qiCost()` | `float` | read |
| `setQiCost(float)` | `void` | re-tune |


---

## Treasure and Ruin Exploration (0.9.x)

`plugin.siren.API.TreasureEvents` — Claiming a Buried Cache or entering a Ruin Vault - covers both Treasure tiers, since both are "claiming" the same kind of site.

**Post-events** — fired once the change is committed; cannot be cancelled.

### `TreasureClaimedEvent`

```java
TreasureEvents.onTreasureClaimed(event -> { /* ... */ });
```

A Treasure site has been claimed/entered - the reward has already been paid.

| Accessor | Type |
| --- | --- |
| `siteId()` | `String` |
| `tier()` | `TreasureTier` |
| `playerUuid()` | `UUID` |
| `worldName()` | `String` |
| `qiAwarded()` | `float` |
| `manualAwarded()` | `boolean` |
| `materialAwarded()` | `boolean` |

**Pre-events** — fired before the change; `setCancelled(true)` vetoes it, and any setter below re-tunes the numbers the mod then uses.

### `PreTreasureClaimEvent`

```java
TreasureEvents.onPreTreasureClaim(event -> { /* ... */ });
```

A player is about to claim a Buried Cache or enter a Ruin Vault. Cancel to refuse it entirely - the site stays unclaimed and the command is a no-op, the same "silence is a valid answer" shape `RivalEvents.PreRivalChallengeEvent` gives a rival challenge.

| Member | Type | |
| --- | --- | --- |
| `siteId()` | `String` | read |
| `tier()` | `TreasureTier` | read |
| `playerUuid()` | `UUID` | read |


---

## Auction House and Traveling Merchant (0.9.x)

`plugin.siren.API.MarketEvents` — Listing, buying, cancelling and expiring auction listings, plus the Traveling Merchant NPC opening and closing for business. Players are identified by UUID - a sold listing routinely pays out to a seller who is offline at the moment of sale.

**Post-events** — fired once the change is committed; cannot be cancelled.

### `AuctionListingCreatedEvent`

```java
MarketEvents.onAuctionListingCreated(event -> { /* ... */ });
```

A listing was created and is now on the shelf.

| Accessor | Type |
| --- | --- |
| `seller()` | `UUID` |
| `listing()` | `AuctionListing` |

### `AuctionListingSoldEvent`

```java
MarketEvents.onAuctionListingSold(event -> { /* ... */ });
```

A listing sold. `listing` is the now-removed shelf entry; `sellerProceeds` is what the seller was credited after the house cut.

| Accessor | Type |
| --- | --- |
| `listing()` | `AuctionListing` |
| `buyer()` | `UUID` |
| `sellerProceeds()` | `long` |

### `AuctionListingCancelledEvent`

```java
MarketEvents.onAuctionListingCancelled(event -> { /* ... */ });
```

A seller pulled their own still-active listing.

| Accessor | Type |
| --- | --- |
| `listing()` | `AuctionListing` |

### `AuctionListingExpiredEvent`

```java
MarketEvents.onAuctionListingExpired(event -> { /* ... */ });
```

An unsold listing aged past Market-Auction-Listing-Duration-Hours and was returned to its seller as a claimable parcel.

| Accessor | Type |
| --- | --- |
| `listing()` | `AuctionListing` |

### `MerchantOpenedEvent`

```java
MarketEvents.onMerchantOpened(event -> { /* ... */ });
```

The Traveling Merchant opened for business in a world.

| Accessor | Type |
| --- | --- |
| `world()` | `String` |
| `x()` | `double` |
| `y()` | `double` |
| `z()` | `double` |

### `MerchantClosedEvent`

```java
MarketEvents.onMerchantClosed(event -> { /* ... */ });
```

The Traveling Merchant's visit ended and the NPC despawned.

| Accessor | Type |
| --- | --- |
| `world()` | `String` |

### `AuctionBidPlacedEvent`

```java
MarketEvents.onAuctionBidPlaced(event -> { /* ... */ });
```

A bid was placed on a timed auction and is now the standing high bid.

| Accessor | Type |
| --- | --- |
| `listing()` | `AuctionListing` |
| `bidder()` | `UUID` |
| `amount()` | `long` |

### `AuctionOutbidEvent`

```java
MarketEvents.onAuctionOutbid(event -> { /* ... */ });
```

A standing high bidder was outbid, or their bid lost to an early buyout - their stones just landed in their claimable parcels (see `/market claim`), even while offline.

| Accessor | Type |
| --- | --- |
| `listing()` | `AuctionListing` |
| `outbidBidder()` | `UUID` |
| `refundedAmount()` | `long` |

### `AuctionClosedEvent`

```java
MarketEvents.onAuctionClosed(event -> { /* ... */ });
```

A timed auction closed with at least one bid - the highest bidder won the item and the seller was credited `sellerProceeds` after the house cut. Mirrors `AuctionListingSoldEvent`'s shape for the timed path; a zero-bid close fires the existing `AuctionListingExpiredEvent` instead, unchanged.

| Accessor | Type |
| --- | --- |
| `listing()` | `AuctionListing` |
| `winner()` | `UUID` |
| `sellerProceeds()` | `long` |

**Pre-events** — fired before the change; `setCancelled(true)` vetoes it, and any setter below re-tunes the numbers the mod then uses.

### `PreAuctionListEvent`

```java
MarketEvents.onPreAuctionList(event -> { /* ... */ });
```

A player is about to list an item. Cancel to refuse it (reported as blocked) - nothing has been touched in the seller's inventory yet.

| Member | Type | |
| --- | --- | --- |
| `seller()` | `UUID` | read |
| `itemId()` | `String` | read |
| `quantity()` | `int` | read |
| `price()` | `long` | read |
| `setPrice(long)` | `void` | re-tune |

### `PreAuctionBuyEvent`

```java
MarketEvents.onPreAuctionBuy(event -> { /* ... */ });
```

A player is about to buy a listing. Cancel to refuse it - nothing has moved yet, the listing stays on the shelf.

| Member | Type | |
| --- | --- | --- |
| `buyer()` | `UUID` | read |
| `listing()` | `AuctionListing` | read |

### `PreAuctionBidEvent`

```java
MarketEvents.onPreAuctionBid(event -> { /* ... */ });
```

A player is about to bid on a timed auction. Cancel to refuse it - nothing has moved yet, and the previous high bidder (if any) has not been refunded. Deliberately no setter, unlike `PreAuctionListEvent`'s `setPrice` - a listener silently changing the amount a player just committed to would be a surprise. Add one only if a real addon need shows up; until then, the supported way to re-tune a bid is to cancel it.

| Member | Type | |
| --- | --- | --- |
| `bidder()` | `UUID` | read |
| `listing()` | `AuctionListing` | read |
| `amount()` | `long` | read |


---

## Beast Tides (0.9.x)

`plugin.siren.API.TideEvents` — A siege (兽潮) on a sect hall or a Cave Abode - starting a wave and resolving win/lose. Fires ALONGSIDE `CelestialEvents` for the tide's own `CelestialEventType` id, carrying the siege-specific detail (which target, how many waves) celestial events don't know about.

**Post-events** — fired once the change is committed; cannot be cancelled.

### `TideStartEvent`

```java
TideEvents.onTideStart(event -> { /* ... */ });
```

A tide's WARNING phase has begun - its target is locked and its beacon will spawn shortly (RISK 2 headstart).

| Accessor | Type |
| --- | --- |
| `assaultId()` | `String` |
| `worldName()` | `String` |
| `abode()` | `boolean` |
| `targetName()` | `String` |
| `waveCount()` | `int` |

### `TideWaveEvent`

```java
TideEvents.onTideWave(event -> { /* ... */ });
```

One wave was just triggered (or attempted - see `PreTideWaveEvent`).

| Accessor | Type |
| --- | --- |
| `assaultId()` | `String` |
| `waveIndex()` | `int` |
| `waveCount()` | `int` |

### `TideResolveEvent`

```java
TideEvents.onTideResolve(event -> { /* ... */ });
```

The assault is over - the outcome (and any reward/suppression) has already been applied.

| Accessor | Type |
| --- | --- |
| `assaultId()` | `String` |
| `worldName()` | `String` |
| `abode()` | `boolean` |
| `targetName()` | `String` |
| `result()` | `TideAssault.Result` |

**Pre-events** — fired before the change; `setCancelled(true)` vetoes it, and any setter below re-tunes the numbers the mod then uses.

### `PreTideStartEvent`

```java
TideEvents.onPreTideStart(event -> { /* ... */ });
```

A target has been picked and a tide is about to enter WARNING. Cancel to abandon this pick entirely - the scheduler simply waits for its next check, the same "silence is a valid answer" shape `PreSectJoinEvent` has.

| Member | Type | |
| --- | --- | --- |
| `worldName()` | `String` | read |
| `abode()` | `boolean` | read |
| `targetName()` | `String` | read |
| `waveCount()` | `int` | read |
| `setWaveCount(int)` | `void` | re-tune |

### `PreTideWaveEvent`

```java
TideEvents.onPreTideWave(event -> { /* ... */ });
```

One wave is about to be triggered. Cancel to skip just this attempt (the timer and wave index still advance - defenders get a breather, not an extra wave).

| Member | Type | |
| --- | --- | --- |
| `assaultId()` | `String` | read |
| `waveIndex()` | `int` | read |

### `PreTideResolveEvent`

```java
TideEvents.onPreTideResolve(event -> { /* ... */ });
```

The assault is about to resolve. Cancel to skip applying the reward (WON) or suppression/cooldown (LOST) - the assault still ends and cleans up either way, only the outcome-specific side effect is skipped. `setSuppressionSteps`/`setSuppressionDurationMinutes` retune a LOSS; `setContributionReward` retunes a WIN.

| Member | Type | |
| --- | --- | --- |
| `assaultId()` | `String` | read |
| `result()` | `TideAssault.Result` | read |
| `suppressionSteps()` | `int` | read |
| `suppressionDurationMinutes()` | `float` | read |
| `contributionReward()` | `int` | read |
| `setSuppressionSteps(int)` | `void` | re-tune |
| `setSuppressionDurationMinutes(float)` | `void` | re-tune |
| `setContributionReward(int)` | `void` | re-tune |


---

## Wandering Rival Cultivators (0.9.x)

`plugin.siren.API.RivalEvents` — Challenging a Wandering Rival Cultivator NPC, and its defeat payout.

**Post-events** — fired once the change is committed; cannot be cancelled.

### `RivalDefeatedEvent`

```java
RivalEvents.onRivalDefeated(event -> { /* ... */ });
```

A challenged Wandering Rival Cultivator has been defeated - the winner's reward has already been paid.

| Accessor | Type |
| --- | --- |
| `encounterId()` | `String` |
| `winnerUuid()` | `UUID` |
| `worldName()` | `String` |
| `qiAwarded()` | `float` |
| `manualAwarded()` | `boolean` |
| `materialAwarded()` | `boolean` |

**Pre-events** — fired before the change; `setCancelled(true)` vetoes it, and any setter below re-tunes the numbers the mod then uses.

### `PreRivalChallengeEvent`

```java
RivalEvents.onPreRivalChallenge(event -> { /* ... */ });
```

A player has right-clicked to challenge a Wandering Rival Cultivator. Cancel to refuse the challenge entirely - the rival stays `ROAMING` and the interaction is a no-op, the same "silence is a valid answer" shape `DuelEvents.PreDuelChallengeEvent` gives a duel challenge.

| Member | Type | |
| --- | --- | --- |
| `encounterId()` | `String` | read |
| `challengerUuid()` | `UUID` | read |
| `npcRef()` | `Ref<EntityStore>` | read |


---

## Calamity Beasts / world boss (0.9.x)

`plugin.siren.API.WorldBossEvents` — A wandering, solo world boss (灾劫兽) with no fixed target, unlike Beast Tide's place-anchored siege - its OMEN phase beginning, the boss NPC actually spawning, and the encounter resolving.

**Post-events** — fired once the change is committed; cannot be cancelled.

### `WorldBossStartEvent`

```java
WorldBossEvents.onWorldBossStart(event -> { /* ... */ });
```

A Calamity Beast's OMEN phase has begun - its spawn point and species are locked and the omen sky/announcement is already live.

| Accessor | Type |
| --- | --- |
| `encounterId()` | `String` |
| `worldName()` | `String` |
| `roleId()` | `String` |
| `position()` | `Vector3d` |

### `WorldBossSpawnEvent`

```java
WorldBossEvents.onWorldBossSpawn(event -> { /* ... */ });
```

The OMEN ended and the boss NPC now actually exists in the world (ACTIVE phase begun).

| Accessor | Type |
| --- | --- |
| `encounterId()` | `String` |
| `worldName()` | `String` |
| `roleId()` | `String` |
| `position()` | `Vector3d` |
| `bossRef()` | `Ref<EntityStore>` |

### `WorldBossResolveEvent`

```java
WorldBossEvents.onWorldBossResolve(event -> { /* ... */ });
```

The encounter is over, for any reason - any reward payout has already been queued.

| Accessor | Type |
| --- | --- |
| `encounterId()` | `String` |
| `worldName()` | `String` |
| `roleId()` | `String` |
| `result()` | `WorldBossEncounter.Result` |
| `contributorCount()` | `int` |
| `totalContribution()` | `float` |

**Pre-events** — fired before the change; `setCancelled(true)` vetoes it, and any setter below re-tunes the numbers the mod then uses.

### `PreWorldBossStartEvent`

```java
WorldBossEvents.onPreWorldBossStart(event -> { /* ... */ });
```

A Calamity Beast is about to begin its OMEN phase. Cancel to abandon this start entirely - the scheduler simply waits for its next check, the same "silence is a valid answer" shape `TideEvents.PreTideStartEvent` has. See the class javadoc for why this carries no re-tunable numbers, unlike Tide's own pre-start event.

| Member | Type | |
| --- | --- | --- |
| `worldName()` | `String` | read |
| `roleId()` | `String` | read |
| `position()` | `Vector3d` | read |


---

## Void Rifts (0.10.0)

`plugin.siren.API.RiftEvents` — A randomly-triggered, server-wide world event: a rift opens, throws a fixed number of corrupted-beast waves, spawns a boss-tier Warden, then resolves SEALED or COLLAPSED. Mostly post-only, the same "auto-picked target, nothing to re-tune" shape as `WorldBossEvents` - only the open itself is cancellable.

**Post-events** — fired once the change is committed; cannot be cancelled.

### `RiftOpenEvent`

```java
RiftEvents.onRiftOpen(event -> { /* ... */ });
```

A Void Rift's OPENING phase has begun - its anchor and spawn point are locked and the world-event marker/announcement is already live.

| Accessor | Type |
| --- | --- |
| `riftId()` | `String` |
| `worldName()` | `String` |
| `position()` | `Vector3d` |

### `RiftWaveEvent`

```java
RiftEvents.onRiftWave(event -> { /* ... */ });
```

One corrupted-beast wave was just thrown during the ASSAULT phase. `waveIndex` counts from 0; `waveCount` is the total for this rift.

| Accessor | Type |
| --- | --- |
| `riftId()` | `String` |
| `worldName()` | `String` |
| `position()` | `Vector3d` |
| `waveIndex()` | `int` |
| `waveCount()` | `int` |
| `spawnConfigId()` | `String` |

### `RiftWardenSpawnEvent`

```java
RiftEvents.onRiftWardenSpawn(event -> { /* ... */ });
```

The ASSAULT phase ended and the Warden NPC now actually exists in the world (WARDEN phase begun).

| Accessor | Type |
| --- | --- |
| `riftId()` | `String` |
| `worldName()` | `String` |
| `roleId()` | `String` |
| `position()` | `Vector3d` |
| `wardenRef()` | `Ref<EntityStore>` |

### `RiftResolveEvent`

```java
RiftEvents.onRiftResolve(event -> { /* ... */ });
```

The encounter is over, for any reason - any reward payout has already been queued. `outcome` is always `RiftPhase#SEALED` or `RiftPhase#COLLAPSED`.

| Accessor | Type |
| --- | --- |
| `riftId()` | `String` |
| `worldName()` | `String` |
| `position()` | `Vector3d` |
| `outcome()` | `RiftPhase` |
| `contributorCount()` | `int` |
| `totalContribution()` | `float` |

**Pre-events** — fired before the change; `setCancelled(true)` vetoes it, and any setter below re-tunes the numbers the mod then uses.

### `PreRiftOpenEvent`

```java
RiftEvents.onPreRiftOpen(event -> { /* ... */ });
```

A Void Rift is about to begin its OPENING phase. Cancel to abandon this open entirely - the scheduler simply waits for its next check, the same "silence is a valid answer" shape `WorldBossEvents.PreWorldBossStartEvent` has. See the class javadoc for why this carries no re-tunable numbers.

| Member | Type | |
| --- | --- | --- |
| `worldName()` | `String` | read |
| `position()` | `Vector3d` | read |


---

## Seasons (0.10.2)

`plugin.siren.API.SeasonEvents` — The shared season cadence opening and closing a season. Post-only - a season boundary is a deterministic outcome of one timestamp and one config value, with nothing usefully vetoable. **The boot self-heal's own open cannot reach an addon listener** (Cultivation's `setup()` runs first), so ask `CultivationAPI.getCurrentSeasonId()` for the current season rather than waiting for the event.

**Post-events** — fired once the change is committed; cannot be cancelled.

### `SeasonOpenEvent`

```java
SeasonEvents.onSeasonOpen(event -> { /* ... */ });
```

A new season has opened and its start timestamp is already persisted. `seasonId` is the season now RUNNING (the closing one's id plus one, or 1 on a fresh install), and `startedAtMillis` is the wall clock the new season's length is measured from. On a rollover this fires immediately after `SeasonCloseEvent` for `seasonId - 1`. On a fresh install (or the first tick after `Season-Enabled` is turned on) it fires alone - there was no previous season to close.

| Accessor | Type |
| --- | --- |
| `seasonId()` | `int` |
| `startedAtMillis()` | `long` |

### `SeasonCloseEvent`

```java
SeasonEvents.onSeasonClose(event -> { /* ... */ });
```

A season has closed: every Hall of Fame history row and champion-uuid set for it is already written to disk, and the leaderboards' season baselines are about to be invalidated by the next open. `seasonId` is the season that just ENDED. Fired after that persist and before `PathWarManager.resetSeasonTotals()`, so a listener still sees the closing season's Path War totals intact. Reads of `CultivationLeaderboard.seasonDelta` are likewise still answering for the closing season at this point - `SeasonOpenEvent` has not bumped the id yet.

| Accessor | Type |
| --- | --- |
| `seasonId()` | `int` |
| `closedAtMillis()` | `long` |


---

## Bounty Board (0.10.2)

`plugin.siren.API.BountyEvents` — A contract being posted to the board through `BountyManager.post` - the rotation's own generated contracts deliberately do NOT fire it - and a completed contract paying out. The claim is cancellable; a PARTIAL claim (a reward that did not fit) never fires the post-event.

**Post-events** — fired once the change is committed; cannot be cancelled.

### `BountyPostedEvent`

```java
BountyEvents.onBountyPosted(event -> { /* ... */ });
```

A contract was explicitly posted to the board - it is already in the board list and already persisted. `bounty` is the live board object, not a copy. Read it; do not mutate it. Its id is what every later `accept`/`claim`/`takeDown` call refers to.

| Accessor | Type |
| --- | --- |
| `bounty()` | `Bounty` |

### `BountyClaimedEvent`

```java
BountyEvents.onBountyClaimed(event -> { /* ... */ });
```

A contract was fully claimed - every promised reward component landed and the acceptance has been marked claimed. Never fires for a `BountyManager.Result#PARTIAL` claim, which leaves the acceptance completed-but-unclaimed for a retry. @param rewardQi the Qi the contract promised, exactly as the board row advertised it. What the cultivator's own multipliers turned that into is `CultivationEvents.QiGainEvent`'s business, not this event's.

| Accessor | Type |
| --- | --- |
| `bountyId()` | `String` |
| `claimantUuid()` | `UUID` |
| `rewardQi()` | `float` |

**Pre-events** — fired before the change; `setCancelled(true)` vetoes it, and any setter below re-tunes the numbers the mod then uses.

### `PreBountyClaimEvent`

```java
BountyEvents.onPreBountyClaim(event -> { /* ... */ });
```

A completed contract is about to pay out. Cancel to refuse the claim outright - nothing has been granted and nothing marked claimed yet, so the acceptance simply stays completed-but-unclaimed and the player may try again. Fired inside `BountyManager.claim` once the claimant has been confirmed to actually HOLD a live acceptance of that contract, and before every other guard. A listener therefore only ever sees attempts that could really have paid out - never a mistyped id or a stale UI row - while still seeing the "tried to collect early" case, because a not-yet-completed acceptance is one the player holds. Without that ordering, every listener's first job would be re-deriving a fact the board already knows, and any that skipped it would count rate limits and audit rows against attempts that could never have paid anything. There is deliberately nothing re-tunable here: the reward is a property of the posted contract, and re-pricing it at claim time would let two claims of the same contract pay differently.

| Member | Type | |
| --- | --- | --- |
| `bountyId()` | `String` | read |
| `claimantUuid()` | `UUID` | read |

### `PreBountyCreditEvent`

```java
BountyEvents.onPreBountyCredit(event -> { /* ... */ });
```

A player kill is about to move a `BountyType.SLAY` contract's progress. Cancel to skip THIS acceptance entirely - no progress is recorded, the contract is not completed, and nothing is said to the killer. Why this exists: one verdict, two consumers A SLAY decree has two halves that must never disagree. The contract is the MONEY (a board reward the killer earns by holding the contract) and whatever posted it usually also runs a STORY of its own (a title, a transfer, a season award). Those two are decided by different rules running in different places, and when they disagree the result is either a payable contract standing against a state that no longer justifies it - an unbounded faucet - or a reward destroyed underneath the player who just earned it. This event is the single verdict both halves obey. The poster listens here, applies exactly the rules it applies to its own reward (pair cooldowns, per-season caps, sandbox and admin-bypass exclusions, whatever it has), and cancels when its own answer is "no". The board then pays only what the poster would itself have paid. A poster that refuses its own reward but lets this event through has re-created the faucet on purpose. Retiring the contract afterwards is `BountyManager.retire`'s job, NOT this event's: `takeDown(id, true)` from inside a credit path destroys the reward the killer just earned. See `BountyManager.retire`'s own javadoc. Fired once per matching LIVE acceptance, after every board-side gate (`isEnabled`, still-on-the-board, unexpired, target matches the victim, the killer is not the contract's own quarry, and the killer is neither on a sandbox profile nor admin-bypassing) and before any progress is written. Read `PreBountyCreditEvent#bounty()`; do not mutate it. Note the class-level lock-order warning above - this one fires from a kill hook, so a listener that blocks stalls a world thread mid-death.

| Member | Type | |
| --- | --- | --- |
| `bountyId()` | `String` | read |
| `killerUuid()` | `UUID` | read |
| `victimUuid()` | `UUID` | read |
| `bounty()` | `Bounty` | read |


---

## Tea Ceremony (0.10.3)

`plugin.siren.API.TeaEvents` — The two-player reflex-timing duet (茶道): starting (cancellable), every step resolving, and how the ceremony ends. Players are UUIDs.

**Enums declared here**

- `TeaEvents.EndReason` — Why a ceremony ended - carried on `TeaCeremonyEndEvent` so a listener can tell a clean finish from a voided one without re-deriving it. Values: `COMPLETED`, `LEFT`, `DRIFTED`, `DISCONNECTED`, `COMBAT`, `DISABLED`

**Post-events** — fired once the change is committed; cannot be cancelled.

### `TeaCeremonyStartEvent`

```java
TeaEvents.onTeaCeremonyStart(event -> { /* ... */ });
```

A ceremony actually began - herbs already spent, the session already live.

| Accessor | Type |
| --- | --- |
| `sessionId()` | `String` |
| `playerA()` | `UUID` |
| `playerB()` | `UUID` |

### `TeaStepResolvedEvent`

```java
TeaEvents.onTeaStepResolved(event -> { /* ... */ });
```

One prompt (a single BOIL/STEEP/POUR/SERVE step) was scored.

| Accessor | Type |
| --- | --- |
| `sessionId()` | `String` |
| `round()` | `int` |
| `step()` | `TeaStep` |
| `playerACorrect()` | `boolean` |
| `playerBCorrect()` | `boolean` |
| `scoreDelta()` | `float` |

### `TeaCeremonyEndEvent`

```java
TeaEvents.onTeaCeremonyEnd(event -> { /* ... */ });
```

A ceremony ended, one way or another - `harmony`/`band` are only meaningful when `reason` is `EndReason#COMPLETED`; every voided reason reports `harmony=0f`/`band=DISCORDANT` and `rewarded=false`, since no reward is ever paid on an abnormal end (herbs still stay spent either way - this event does not cover that, it is applied unconditionally at ceremony start).

| Accessor | Type |
| --- | --- |
| `sessionId()` | `String` |
| `playerA()` | `UUID` |
| `playerB()` | `UUID` |
| `reason()` | `EndReason` |
| `harmony()` | `float` |
| `band()` | `TeaHarmonyBand` |
| `rewarded()` | `boolean` |

**Pre-events** — fired before the change; `setCancelled(true)` vetoes it, and any setter below re-tunes the numbers the mod then uses.

### `PreTeaCeremonyStartEvent`

```java
TeaEvents.onPreTeaCeremonyStart(event -> { /* ... */ });
```

A pending invite is about to be accepted and a ceremony is about to begin (herbs not yet spent). Cancel to refuse it - the invite is consumed either way, mirroring `OathEvents.PreOathSwearEvent`, so the offerer must send a fresh one.

| Member | Type | |
| --- | --- | --- |
| `offerer()` | `UUID` | read |
| `accepter()` | `UUID` | read |
| `herbCost()` | `int` | read |
| `setHerbCost(int)` | `void` | re-tune |


---

## Weiqi (0.10.3)

`plugin.siren.API.WeiqiEvents` — Weiqi (围棋 / Go): invites and declines, the match starting, every move, and how a match resolves. Invite and start are cancellable. Players are UUIDs because a match routinely outlives one participant's session.

**Post-events** — fired once the change is committed; cannot be cancelled.

### `WeiqiInviteEvent`

```java
WeiqiEvents.onWeiqiInvite(event -> { /* ... */ });
```

An invite was sent and is now pending the other player's answer.

| Accessor | Type |
| --- | --- |
| `inviter()` | `UUID` |
| `invitee()` | `UUID` |

### `WeiqiDeclineEvent`

```java
WeiqiEvents.onWeiqiDecline(event -> { /* ... */ });
```

An invite was declined; no match started.

| Accessor | Type |
| --- | --- |
| `inviter()` | `UUID` |
| `invitee()` | `UUID` |

### `WeiqiMatchStartEvent`

```java
WeiqiEvents.onWeiqiMatchStart(event -> { /* ... */ });
```

A match is now live. `blackUuid` is always the ACCEPTER (moves first, a courtesy to the invited player); `whiteUuid` is the original inviter.

| Accessor | Type |
| --- | --- |
| `matchId()` | `String` |
| `blackUuid()` | `UUID` |
| `whiteUuid()` | `UUID` |
| `boardSize()` | `int` |
| `komi()` | `float` |

### `WeiqiMoveEvent`

```java
WeiqiEvents.onWeiqiMove(event -> { /* ... */ });
```

One ply resolved - either a stone placed at `index` (a flat `y * boardSize + x` board index, decodable via `WeiqiBoard.xOf`/`yOf`) or a pass, never both. @param index meaningless (always `-1`) when `pass` is true. @param capturedCount how many enemy stones this move captured - always `0` for a pass.

| Accessor | Type |
| --- | --- |
| `matchId()` | `String` |
| `player()` | `UUID` |
| `color()` | `WeiqiStone` |
| `index()` | `int` |
| `pass()` | `boolean` |
| `capturedCount()` | `int` |

### `WeiqiMatchEndEvent`

```java
WeiqiEvents.onWeiqiMatchEnd(event -> { /* ... */ });
```

A match ended. `winner` is null for `WeiqiMatch.Outcome#ABANDONED`/ `WeiqiMatch.Outcome#TIMED_OUT` (nobody won an undecided match); for `WeiqiMatch.Outcome#TWO_PASS`/`WeiqiMatch.Outcome#RESIGNED` it is always set. `blackScore`/`whiteScore` are only meaningful for `TWO_PASS` (a resignation or an abandonment never runs area scoring) - both are `0` otherwise.

| Accessor | Type |
| --- | --- |
| `matchId()` | `String` |
| `blackUuid()` | `UUID` |
| `whiteUuid()` | `UUID` |
| `winner()` | `UUID` |
| `outcome()` | `WeiqiMatch.Outcome` |
| `blackScore()` | `float` |
| `whiteScore()` | `float` |
| `totalMoves()` | `int` |

**Pre-events** — fired before the change; `setCancelled(true)` vetoes it, and any setter below re-tunes the numbers the mod then uses.

### `PreWeiqiInviteEvent`

```java
WeiqiEvents.onPreWeiqiInvite(event -> { /* ... */ });
```

An invite is about to be sent. Cancel to refuse it outright (e.g. an addon-enforced cooldown or block list).

| Member | Type | |
| --- | --- | --- |
| `inviter()` | `UUID` | read |
| `invitee()` | `UUID` | read |

### `PreWeiqiMatchStartEvent`

```java
WeiqiEvents.onPreWeiqiMatchStart(event -> { /* ... */ });
```

A match is about to start (the accepter just accepted a pending invite). Cancel to refuse it - the invite is consumed either way, so the inviter must send a fresh one. `setKomi` re-tunes the komi this ONE match will actually be latched with; `boardSize` is not mutable here since it is already clamped from config before this event fires.

| Member | Type | |
| --- | --- | --- |
| `blackUuid()` | `UUID` | read |
| `whiteUuid()` | `UUID` | read |
| `boardSize()` | `int` | read |
| `komi()` | `float` | read |
| `setKomi(float)` | `void` | re-tune |


---

## Treasure Pavilion benefits

`plugin.siren.API.StoreBenefitEvents` — Entitlements bought on xianxia.dev arriving and leaving. **These fire on the remote checker thread, not on a world thread**, and none of them is cancellable - both departures from every other class here, so read the class javadoc before a listener touches a player.

**Post-events** — fired once the change is committed; cannot be cancelled.

### `BenefitGrantedEvent`

```java
StoreBenefitEvents.onBenefitGranted(event -> { /* ... */ });
```

A player the last sweep did not list is now entitled to `benefit`.

| Accessor | Type |
| --- | --- |
| `playerUuid()` | `UUID` |
| `benefit()` | `StoreBenefit` |

### `BenefitRevokedEvent`

```java
StoreBenefitEvents.onBenefitRevoked(event -> { /* ... */ });
```

A previously entitled player is no longer listed - a refund, a chargeback, or the server disabling the product. Fired for each player, whether or not they are online; the wearer of an auto-registered title keeps it only until their next join, when it is re-validated.

| Accessor | Type |
| --- | --- |
| `playerUuid()` | `UUID` |
| `benefit()` | `StoreBenefit` |

### `SyncCompletedEvent`

```java
StoreBenefitEvents.onSyncCompleted(event -> { /* ... */ });
```

One sweep finished - every registered product was fetched (or skipped as disabled). `failedProducts` names the fetches that came to nothing; their previous lists were kept, not cleared.

| Accessor | Type |
| --- | --- |
| `products()` | `int` |
| `granted()` | `int` |
| `revoked()` | `int` |
| `failedProducts()` | `List<String>` |

