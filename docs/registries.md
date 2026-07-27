# Registries

Three open registries let a mod add content that behaves exactly like
Cultivation's own. All three are called from your plugin's `setup()`, in any load
order, and re-registering an id is a safe no-op rather than an error.

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
