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

### Manual-gated techniques

Cultivation can require a player to have *learned* a technique from a lootable
manual before using it (`Requires-Manual` on the rule). `newTechniqueRule` does
not set that flag, so techniques you register are usable as soon as their realm
gate passes. Listen to `TechniqueEvents.onTechniqueLearn` if you want to track
learning yourself.

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
