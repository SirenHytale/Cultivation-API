# Registries

Open registries let a mod add content that behaves exactly like Cultivation's
own. All of them are called from your plugin's `setup()`, in any load order, and
re-registering an id is a safe no-op rather than an error.

The three content registries come first (races, techniques, Qi absorption
modifiers); the two release registries at the end put your mod on Cultivation's
Info page.

---

## Races

A race is a player-choosable identity with stat bonuses, unlocked at a realm.
Once registered it appears in the race menu (`/cultivation race`) automatically.

```java
PlayerRace race = CultivationAPI.registerRace(
        "MyAddon:Vampire",                            // id - namespace it
        "Vampire",                                    // fallback display name
        "server.myaddon.race.vampire.name",           // lang key, or null
        CultivationRealm.FOUNDATION_ESTABLISHMENT,    // unlock realm
        MyAddon::buildVampireConfig);                 // Supplier<RaceConfig>
```

The return value is the registered `PlayerRace`, worth keeping as a static field
so you can compare against it later.

### The stats supplier

The last argument is a **`Supplier<RaceConfig>`**, not a `RaceConfig`. Cultivation
calls it every time it needs the race's numbers, so backing it with your own
live-editable config file means an admin's edit takes effect without a restart:

```java
private static RaceConfig buildVampireConfig() {
    VampireConfig source = MyAddon.get().getVampireConfig().get();   // read fresh

    RaceConfig config = new RaceConfig();
    config.setDescription(source.getDescription());
    config.setUnlockRealm(source.getUnlockRealm());
    config.setHealthBonusPercent(source.getHealthBonusPercent());
    config.setDamageBonusPercent(source.getDamageBonusPercent());
    config.setQiGainRatePercentBonus(source.getQiGainRatePercentBonus());
    config.setBreakthroughDurationPercentReduction(source.getBreakthroughDurationPercentReduction());
    config.setQiAlignmentYinBiasPercent(source.getQiAlignmentYinBiasPercent());
    return config;
}
```

`RaceConfig` fields:

| Setter | Meaning |
| --- | --- |
| `setDescription` | Shown in the race menu |
| `setUnlockRealm` | Realm name; overrides the `unlockRealm` argument when set |
| `setHealthBonusPercent` | Percentage max-health bonus |
| `setDamageBonusPercent` | Percentage outgoing-damage bonus |
| `setQiGainRatePercentBonus` | Percentage bonus to Qi banked while meditating |
| `setBreakthroughDurationPercentReduction` | Shortens the breakthrough ritual |
| `setQiAlignmentYinBiasPercent` | Biases the player's Yin-Yang lean |

The `unlockRealm` argument only **seeds** `RaceConfig.UnlockRealm` when the
supplied config leaves it empty — so a server owner editing your JSON stays in
charge of the gate.

### Reacting to the choice

```java
CultivationEvents.onRaceChange(event -> {
    if (event.newRace() == myVampireRace) {
        // they just became a vampire
    }
});
```

Compare by identity (`PlayerRace` hands out singletons) or by
`getId().equals(...)`, which additionally covers another mod having registered
your id first.

---

## Techniques

A technique is an active art a cultivator performs. Once registered it works
through every trigger automatically: `/cultivation technique <id>` and its list,
and any activation item whose `CultivationActivateTechnique` interaction carries
the id as its `TechniqueId`.

```java
public static final Technique FLAME_STEP = CultivationAPI.registerTechnique(
        "MyAddon:flame_step",
        "Flame Step",
        "server.myaddon.technique.flame_step.name",
        "server.myaddon.technique.flame_step.description",
        CultivationAPI.newTechniqueRule(
                "MyAddon:flame_step",
                true,                      // enabled
                true,                      // daoSpecific
                "FIRE",                    // requiredElement (daoSpecific only)
                "FIRE",                    // elements it "carries" (flavor)
                "",                        // damageType - a DamageCause asset id
                "QI_CONDENSATION",         // unlock realm
                30f,                       // Qi cost
                8f,                        // cooldown, seconds
                "BaseDistance", 6f,        // params: alternating key/value
                "DistancePerRealm", 2f),
        MyAddon::flameStep);
```

`newTechniqueRule`'s `params` are technique-specific named numbers, passed as
alternating `String, float` pairs — an odd number of arguments is a bug.

> The rule you pass is the **only** source of rules for your technique unless a
> server owner adds a matching override entry to Cultivation's
> `TechniqueConfig.json`.

### The effect

`TechniqueEffect` is a functional interface. It runs **only after every gate has
passed and the Qi cost and cooldown have been applied**, so it never has to
re-check availability:

```java
private static void flameStep(@Nonnull TechniqueContext context) {
    float distance = context.getParam("BaseDistance", 6f)
            + context.getParam("DistancePerRealm", 2f) * context.getRealmIndex();

    Vector3d position = context.getPosition();
    Vector3d look = context.getLookDirection();
    if (position == null || look == null) {
        return;
    }

    context.teleport(position.add(look.mul(distance)));
    context.spawnParticle("MyAddon_FlameBurst", position);
    context.sendMessage(Text.of("server.myaddon.technique.flame_step.playerMsg.used"));
}
```

`TechniqueContext` gives you:

| Member | |
| --- | --- |
| `getAccessor()` / `getRef()` / `getPlayerRef()` | The performer |
| `getTechnique()` / `getRule()` | What is being performed, and its resolved rule |
| `getParam(String key, float fallback)` | A named number from the rule |
| `getCultivation()` | Their `CultivationComponent` |
| `getRealmIndex()` / `getStageIndex()` | For scaling with progression |
| `getPosition()` / `getLookDirection()` / `getWorld()` | Where they are |
| `teleport(Vector3d)` | Move them safely |
| `spawnParticle(String particleId, Vector3d)` | VFX |
| `sendMessage(Message)` | Tell them what happened |

### Performing it from your own trigger

To fire a technique from a keybind, a different item, or an event, call:

```java
boolean performed = CultivationAPI.performTechnique(accessor, ref, playerRef, FLAME_STEP);
```

This runs every gate (system enabled, technique enabled, realm unlock, dao match,
Qi cost, cooldown), and on success deducts Qi, stamps the cooldown and runs the
effect. The player is messaged either way — the effect's own success message, or
the failure reason. `false` means a gate blocked it.

### Unlock-gated techniques

**Changed in 0.7.4, and it changes the default for techniques you register.**

Meeting an art's realm, Dao and race gate now makes a player *able to hold* it,
not entitled to use it. Coming by it is a separate event, and the flag that says
so — `Requires-Unlock` on the rule — **defaults to `true`**.

`newTechniqueRule` deliberately does not take the flag, so a technique you
register is **locked until the player comes by it**, including one registered by
a mod written before this existed. Before 0.7.4 the equivalent flag was
`Requires-Manual` and it defaulted to off, so an art you registered fired the
moment its realm gate passed. If your addon assumed that, it is the one thing
here worth re-testing: the symptom is an art that silently never fires for a
player who should have it.

To keep the old behavior, say so on the rule:

```java
TechniqueRule rule = CultivationAPI
        .newTechniqueRule("MyMod:flame_step", true, false, null, "FIRE", null,
                          "QI_CONDENSATION", 30f, 8f)
        .freelyAvailable();   // no unlock needed - realm gate is the only gate
```

`freelyAvailable()`, `charged()` and `unarmedOnly()` are fluent config-*defaults*
— they set what the rule starts as, and a server owner still overrides any of
them per art in `Arts/TechniqueConfig.json`.

There are four routes a player can come by an art (a manual, enlightenment while
meditating, a breakthrough, or a sect hall's inscription), and **all four go
through one code path**, so you do not have to care which one fired:

```java
// Veto an unlock, whatever produced it.
TechniqueEvents.onPreTechniqueLearn(event -> {
    if(isForbiddenOnThisServer(event.techniqueId())){
        event.setCancelled(true);
    }
});

// Observe one that went through.
TechniqueEvents.onTechniqueLearn(event -> { /* event.techniqueId() */ });
```

Note the rename is deliberate and is *itself* the migration. A config written
before 0.7.4 carries `"Requires-Manual": false` on every entry; had the key kept
its name, that stale `false` would have outvoted the new default forever and left
every existing server with the gate silently switched off. An absent key takes
the Java default instead, and the orphaned `Requires-Manual` is read as an
unknown key and ignored rather than erroring.

### Charged and unarmed techniques

Two more rule flags arrived with 0.7.4, both off unless set:

- **`charged()`** — the art is pressed once to begin gathering and again to
  loose it, growing stronger the longer it is held. The numbers ride along as
  ordinary `Charge*` params, so a server owner retunes a charge exactly as they
  retune anything else.
- **`unarmedOnly()`** — the art refuses while a weapon is in hand. What counts as
  a free hand is Cultivation's call and a server owner can tighten it to a
  strictly empty hand, so test against the flag rather than inspecting the
  player's held item yourself.

---

## Qi absorption item modifiers

Registers the Spirit Vein absorption multiplier a meditating player gets while
holding an item in their active hotbar slot — the same mechanism behind the
built-in Qi Gathering Talisman:

```java
CultivationAPI.registerQiAbsorptionItemModifier("MyAddon_JadeCharm", 1.5f);
```

One line, and the item is part of the meditation economy. Registering an existing
item id overwrites its multiplier.

---

## Update checks

Puts your mod on Cultivation's **Info page** and into its background update
sweep, so a server behind on three mods is told **once** rather than three
times.

```java
CultivationAPI.registerUpdateCheck(
        "MyMod",                                          // stable id
        "My Mod",                                         // shown to a reader
        this.getManifest().getVersion().toString(),       // never hardcode this
        "https://example.com/api/version/MyMod.json",     // your manifest
        "https://example.com/download");                  // optional
```

The manifest is one static JSON object:

```json
{
  "release": "1.2.0",
  "ignore": ["1.1.3"]
}
```

`release` is the version you consider current. `ignore` lists **installed**
versions that are never notified even though they are behind it — a beta you
would rather leave alone, or a build whose upgrade path is not ready.

Administrators (`cultivation.admin`) get one combined message on join naming
every mod that is behind. Everything fails **open**: a dead host, a timeout, a
404, a body that is not JSON, or a manifest missing `release` all mean "no
update", never an error a player sees.

The server owner's `Update-Check-Enabled` governs every registered mod including
yours — if they have switched checking off, your registration is kept but never
fetched.

---

## Build checks

Answers a narrower question: **is this jar the build that was published?**

```java
CultivationAPI.registerBuildCheck(
        "MyMod",
        "My Mod",
        this.getManifest().getVersion().toString(),
        this.getFile(),                                   // your own jar
        "https://example.com/api/build/MyMod.json");
```

```json
{
  "builds": {
    "1.2.0": ["a3f2...9c"],
    "1.1.0": ["71bd...04", "5e90...11"]
  }
}
```

Cultivation hashes your jar's compiled classes and compares. An array per
version, so a version you legitimately rebuilt can carry more than one digest.

Read the verdict with `getBuildStatus("MyMod")`, which returns
[`BuildStatus`](../api-sources/plugin/siren/API/BuildStatus.java):

| Status | Means |
| --- | --- |
| `OFFICIAL` | The code matches a published digest. |
| `UNOFFICIAL` | Digests exist for this exact version and this jar matches none. |
| `UNKNOWN` | Nobody could confirm it — not checked yet, no network, or that version was never published. |

**Read this before relying on it.** This is detection, not protection. Anybody
able to rebuild your mod is equally able to delete the call, and nothing running
on someone else's machine can do better. What it catches is the cases nobody
bothered to strip it from: a repackaged upload, a redistributed paid addon, a
download that arrived damaged. Treat `UNOFFICIAL` as a signal, never a lock —
and be aware that gating features on it mostly punishes honest servers.

It also fails open. A version absent from `builds` leaves an install `UNKNOWN`
rather than condemning it, which is what makes forgetting to publish a digest
harmless rather than an incident.

## Compatibility checks

**New in 0.7.4.** Answers a third question: **is this pairing of your version and
this Cultivation version known to be broken?**

```java
CultivationAPI.registerCompatCheck(
        "MyMod",
        "My Mod",
        this.getManifest().getVersion().toString(),
        "https://example.com/api/compat/MyMod.json",
        check -> {
            if(check.getStatus() == CompatStatus.INCOMPATIBLE){
                standDown(check.getRequiredRange());   // e.g. ">=0.7.4 <0.8.0"
            }
        });
```

The matrix is keyed by **your** version, and each entry describes which
Cultivation versions that build of yours works against:

```json
{
  "compatible": {
    "1.2.0": { "min": "0.7.4", "below": "0.8.0", "blocked": [] },
    "1.1.0": { "min": "0.7.0", "below": "0.7.4", "blocked": ["0.7.2"] },
    "default": { "min": "0.7.4", "below": "0.8.0", "blocked": [] }
  }
}
```

- `min` — the lowest Cultivation this build accepts, inclusive.
- `below` — the first Cultivation it does **not** accept, exclusive.
- `blocked` — exact versions to reject inside that band, for a single release
  that turned out to be broken.

Every field is optional: an entry with neither `min` nor `below` means any
version. `default` is used for any of your versions with no entry of its own,
which is what keeps an old build from silently going `UNKNOWN` after you stop
listing it.

`getRequiredRange()` hands the band back already formatted for a log line
(`">=0.7.4 <0.8.0"`, or `"any version"`), so you do not have to reassemble it.

### Declare a real range in your manifest first

This registry is **not** the gate, and reaching for it instead of a manifest
range is the mistake worth naming. A manifest `Dependencies` entry accepts a full
semver range — it is not limited to a floor:

```json
"Dependencies": { "Siren:Cultivation": ">=0.7.4 <0.8.0" }
```

That makes the engine refuse to load your addon against a Cultivation outside the
band, before a line of your code runs and with no network involved. It is
absolute, offline, and cannot be got wrong at runtime.

What a manifest range *cannot* do is describe a pairing that turned out to be
broken **after** both jars shipped. That is this check's entire job: the matrix is
a file you can edit without cutting a release, so a bad pairing can be corrected
for everyone who already has both installed. Think of it as the retractable
second layer over a fixed first one.

### Stand down; never throw

Act on `INCOMPATIBLE` by withdrawing what you registered and saying so plainly in
the log.

**Do not throw out of `setup()` to refuse a pairing** — a plugin that throws at
boot takes the whole server down with it, and by the time a verdict lands you
have been running for half a minute anyway. The verdict arrives on the check's
own daemon thread, not a world thread: touch registries and the log from it,
never entity state.

| Status | Means |
| --- | --- |
| `COMPATIBLE` | The published matrix says this pairing is fine. |
| `INCOMPATIBLE` | The matrix names this pairing as broken; `getRequiredRange()` says what it wanted. |
| `UNKNOWN` | Nobody could tell — not checked yet, no network, or no entry for your version. |

Like the other two, it fails open in every direction. An unreachable site, a
malformed matrix, or a version nobody has written an entry for all leave your mod
running untouched, so an offline server is never told its mods disagree.

## Beast arts

A beast art is the companion-side twin of a technique: the thing a bound spirit
beast *does* in a fight. The registry is open for the same reason the technique
one is — a Java enum cannot gain constants at runtime.

```java
BeastArt frostFang = CultivationAPI.registerBeastArt(
        "mymod:frost_fang",
        "Frost Fang",
        "mymod.beast.art.frost_fang.name",         // or null for raw display text
        "mymod.beast.art.frost_fang.description",
        new BeastArtRule("mymod:frost_fang", true, "FOUNDATION_ESTABLISHMENT", 12f, "Ice",
                new TechniqueParam[]{
                        new TechniqueParam("Radius", 4f),
                        new TechniqueParam("BaseDamage", 8f),
                        new TechniqueParam("DamagePerLevel", 0.7f)
                }),
        context -> {
            // context carries BOTH ends of the bond: getBeastRef() is where the
            // art comes from, getOwnerRef() is who it serves.
            context.sendMessage(Text.of("mymod.beast.art.frost_fang.playerMsg.hit"));
        });
```

**Registering an art gives it to nobody.** An art belongs to a *species*: its id
has to appear in that species' `Arts` list in `BeastConfig.json` before any beast
can grow into it. That is deliberate — it lets you ship arts and leave the server
owner to decide which creatures learn them.

Each art also has its own `UnlockRealm`, and that realm is the **beast's**, not
its owner's. A cultivator cannot lend their companion their own cultivation.

Reading and driving them:

```java
List<BeastArt> known = CultivationAPI.getKnownBeastArts(accessor, ref);
BeastArtRule rule    = CultivationAPI.getBeastArtRule(frostFang);   // config entry, else your default
boolean fired        = CultivationAPI.performBeastArt(accessor, ref, frostFang, playerRef);
```

`performBeastArt` runs every gate the mod's own callers run — feature enabled,
beast summoned, art known and off cooldown, no addon veto — so you never have to
reproduce them.

## Sect building types

Sect buildings are the holdings a sect raises beyond its hall. Each carries one
switch that matters more than the rest: whether the sect's Dao presses on
cultivators inside it.

```java
CultivationAPI.registerSectBuildingType(
        new SectBuildingType("mymod:frost_terrace", 1.12f, 2, false));
//                            id                   medMult  level  daoActiveByDefault
```

The last argument is the one to think about. `false` makes it **neutral ground** —
a disciple of any element can cultivate there without being turned toward the
sect's Dao. That is how a sect takes in a friend who walks a different element,
so a type meant as a guest hall should default to `false`.

Registering a kind only makes it *known*; a server owner still decides whether
their sects may raise it. Your entry never overrides a config entry with the same
id — the owner's always wins.

## Life-Bound traits

A trait is the nature a bound treasure turns out to have. It is rolled **once**,
at binding, and never re-rolled.

```java
CultivationAPI.registerLifeBoundTrait(
        new LifeBoundTrait("mymod:frostbite", LifeBoundTrait.Slot.WEAPON,
                           1f, 0.5f, 25f, 1.2f)      // base, perLevel, max, roll weight
                .unlocks("frozen_domain", 9));       // an art it lends while held, from level 9
```

Two consequences worth planning around. Your trait enters the weighted roll
immediately, so it changes what *future* bindings can produce — treasures already
bound keep whatever they rolled, and there is no re-roll. And `Slot` is a real
restriction: a `WEAPON` trait never lands on a breastplate, which is what stops
the roll from disappointing half the time.

Reading one:

```java
float lifesteal = CultivationAPI.getLifeBoundTraitAmount(stack, "lifesteal");
String art      = CultivationAPI.getLifeBoundGrantedTechnique(stack);   // null if none, or not yet
```

`getLifeBoundTraitAmount` returns 0 when the treasure is of a different nature,
so it is safe to ask about any trait on any item.

## Mastery rungs

The mastery ladder is normally the server's five configured rungs. An addon may
append one:

```java
boolean added = CultivationAPI.registerMasteryStage(
        new MasteryStageRule("Transcendent", "SOUL_FORMATION", 2500f, 8, 2.6f, 0.7f, 0.65f));
```

It returns **false** if the ladder is already full rather than silently ignoring
you, because the ladder is capped at five and the UI and lang keys only cover
that many. Check the return value.

## Standing modifiers

The hook for anything that changes what a cultivator **is** rather than what they
do — a bloodline, a constitution, a physique. One registration answers a set of
typed channels, and Cultivation asks them at each of its own chokepoints.

```java
CultivationAPI.registerModifierSource("myMod:bloodlines", new CultivationModifierSource(){
    @Override
    public float qiGainMultiplier(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> ref){
        return Bloodline.of(accessor, ref) == Bloodline.DRAGON ? 1.15f : 1f;
    }

    @Override
    public float damageTakenMultiplier(ComponentAccessor<EntityStore> accessor, Ref<EntityStore> ref){
        return Bloodline.of(accessor, ref) == Bloodline.DRAGON ? 0.95f : 1f;
    }
});
```

Every method has a default meaning "no opinion", so implement only the channels
you care about. Withdraw it from `shutdown()` with `unregisterModifierSource`.

### Why this and not events

Events are the right tool for reacting to a *moment*. This is for a standing
*fact* about a player that re-prices a dozen unrelated systems at once — doing
that with events would mean a listener on every one of them, each re-deriving
the same fact.

### The channels

| Channel | What it scales |
| --- | --- |
| `qiGainMultiplier` | Every point of Qi gained, from any source |
| `meditationQiMultiplier` | Qi drawn by meditating; carries an `inLava` flag |
| `damageDealtMultiplier` | All damage dealt |
| `unarmedDamageMultiplier` | Bare-handed damage, **on top of** the above |
| `damageTakenMultiplier` | Damage received — above 1 is fragility |
| `isImmuneToDamageCause` | Cancels a damage cause outright |
| `chargeSecondsMultiplier` | How long a charged art must be gathered for |
| `ignoresDaoElementLock` | Lifts the dao lock on elemental arts entirely |
| `daoAffinityMultiplier` | Affinity gained toward one element |
| `alignmentShiftMultiplier` | How far a Yin or Yang shift moves them |
| `yinYangBalanceToleranceMultiplier` | Widens the band counting as balanced |
| `ritualDifficultyMultiplier` | Ritual length **and** tribulation damage |
| `canMeditateInLava` | Whether lava is a legal meditation seat |
| `auraScaleMultiplier` | Realm aura size — cosmetic |
| `bonuses` | Rows for the Overview's bonus list |
| `traits` | Named lines for the Overview's Constitution section |

### How they combine

Multipliers **multiply** across every registered source; the neutral value is
`1`. Booleans **OR** — one source saying yes is enough. Two addons each granting
+10% produce ×1.21, not ×1.20, which is the same way Cultivation's own internal
multipliers already stack.

A source that throws is caught, logged once, and thereafter ignored, so a broken
addon cannot take the damage pipeline down with it.

### Cost

Every channel returns its neutral value immediately when nothing is registered,
so a server without such an addon pays one emptiness check per chokepoint. Your
own methods are called on the world thread, sometimes from inside the damage
pipeline — keep them to a component read and some arithmetic.

### A worked example

**Cultivation: Sacred Bodies** is built entirely on this registry and nothing
else — sixteen sacred bodies and eight stackable physiques, each re-pricing Qi,
damage, charge time, rituals and the aura, all through one registered source.

It is worth reading as a shape to copy on two counts. It answers every channel
from a *single component read* rather than one per channel, which is what keeps a
source this broad cheap enough to sit in the damage pipeline. And it contributes
its own `traits` lines, so a constitution shows up on the Overview page as
readable text rather than as an unexplained change in the player's numbers — a
standing modifier the player cannot see is a bug report waiting to happen.

## Overview rows and stat bonuses

`bonuses` returns `CultivationBonus` rows built with `newBonus`. Reuse a key from
`CultivationAPI.BonusStats` and your row **sums** with Cultivation's own rather
than printing a second, differently-worded line for the same stat:

```java
CultivationAPI.newBonus("mymod.source.bloodline",
        CultivationAPI.BonusStats.QI_GAIN_PERCENT, 15f, true);
```

Available keys: `HEALTH`, `DAMAGE_PERCENT`, `DAMAGE_REDUCTION_PERCENT`,
`QI_GAIN_PERCENT`, `STAMINA`, `MANA`, `MOVE_SPEED_PERCENT`,
`RITUAL_SPEED_PERCENT`, `QI_COST_REDUCTION_PERCENT`. Anything else is legal and
prints under its own name.

For a real engine stat rather than a display row, use `applyStatBonus`:

```java
CultivationAPI.applyStatBonus(accessor, ref,
        DefaultEntityStatTypes.getStamina(), "MyMod_Stamina", 25f);
```

It is **keyed**: calling it again with the same key replaces that key's
contribution rather than stacking with it, so you can recompute freely without
tracking what you granted last time. Pass `0` to withdraw it.
