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
