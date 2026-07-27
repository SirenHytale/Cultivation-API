# Event reference

Every event Cultivation fires, grouped by the class that declares it.
**135 listener hooks** across 10 subsystems.

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

`plugin.siren.API.CultivationEvents` — Qi, meditation, rituals, breakthroughs, advancements, demotions, tribulations, the Heart-Devil Trial, Qi Deviation, races, the skill tree and respecs.

**Enums declared here**

- `CultivationEvents.RitualType` — Which timed meditation ritual a ritual event refers to. Values: `BREAKTHROUGH`, `ADVANCEMENT`, `REFINEMENT`
- `CultivationEvents.MeditationStopReason` — Why a player stopped meditating. Values: `COMMAND`, `MOVEMENT`

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

### `PreRitualStartEvent`

```java
CultivationEvents.onPreRitualStart(event -> { /* ... */ });
```

A timed meditation ritual is about to begin. Cancel to refuse it - the player keeps meditating (banking Qi) but never enters the ritual.

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

## Techniques

`plugin.siren.API.TechniqueEvents` — Performing and learning arts, Sword Flying, and the timed combat buffs.

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


---

## Items, alchemy and refinement

`plugin.siren.API.ItemEvents` — Loot drops, pills, spirit cores, manuals and weapon refinement.

**Enums declared here**

- `ItemEvents.LootType` — What a cultivation drop is. Values: `CULTIVATION_CORE`, `BEAST_EGG`, `SPIRIT_STONE`, `MANUAL`
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

## Spirit beasts

`plugin.siren.API.BeastEvents` — Taming, hatching, binding, summoning and companion growth.

**Enums declared here**

- `BeastEvents.BindSource` — How a cultivator came by their companion. Values: `TAME`, `HATCH`
- `BeastEvents.DismissReason` — Why a companion's body left the world. Values: `DISMISSED`, `RELEASED`

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


---

## Sects

`plugin.siren.API.SectEvents` — Founding, disbanding, membership, ranks, halls and inscriptions.

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


---

## Sect wars

`plugin.siren.API.WarEvents` — Declaring sieges and how they resolve.

**Enums declared here**

- `WarEvents.SiegeFailReason` — Why a siege ended without the attacker taking the hall. Values: `LAPSED`, `DEFENDER_GONE`

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


---

## Duels

`plugin.siren.API.DuelEvents` — Challenges, duel start/end and Qi wager payouts.

**Enums declared here**

- `DuelEvents.DuelEndReason` — How a duel stopped being active. Values: `DEATH`, `VOIDED`

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

A duel ended. For DEATH, `winner`/`loser` are meaningful and the payout has been queued; for VOIDED they are simply the two participants and nothing changes hands.

| Accessor | Type |
| --- | --- |
| `winner()` | `UUID` |
| `loser()` | `UUID` |
| `wager()` | `int` |
| `reason()` | `DuelEndReason` |

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

