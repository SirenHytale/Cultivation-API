# Cultivation API

The public integration surface of **[Cultivation](https://xianxia.dev/)**, a Xianxia
progression mod for Hytale servers — realms and Qi, daos, sects, techniques, spirit
beasts, formations, cave abodes, duels and alchemy.

This repository is for **mod developers**. It contains the API's Java sources,
a complete reference for all **160 events**, and worked examples. Everything here
is the real code the mod ships — nothing is a summary written after the fact.

| | |
| --- | --- |
| **Artifact** | `plugin.siren:Cultivation:0.7.4` |
| **Package** | `plugin.siren.API` |
| **Plugin id** | `Siren:Cultivation` |
| **Hytale server** | `0.5.x` (built against `com.hypixel.hytale:Server:0.5.7`) |
| **Java** | 25 |

---

## What you can do with it

You do not have to fork Cultivation, patch its config files, or reach into its
internals to change how it behaves. Every one of these is a supported hook:

| I want to… | Use | Guide |
| --- | --- | --- |
| Read a player's realm, level, Qi, race, skill nodes | `CultivationAPI` getters | [Reading player state](docs/reading-state.md) |
| React when something happens (breakthrough, tame, siege…) | 84 post-events | [Events](docs/events.md) |
| Veto something, or re-tune its numbers | 76 cancellable pre-events | [Events](docs/events.md) |
| Keep your own progression in step with a player's saves | `ProfileEvents` | [Profiles](docs/profiles.md) |
| Grant Qi, force a breakthrough, hand out a skill node | `addQi`, `completeBreakthrough`, `grantSkillNode` | [Driving progression](docs/driving-progression.md) |
| Read or drain a chunk's spirit vein | `readSpiritVein`, `drainSpiritVein` | [Driving progression](docs/driving-progression.md) |
| Ask what sect / dao / abode / beast a player has | `getSect`, `getDaoElement`, `getAbode`, `getBeast` | [Driving progression](docs/driving-progression.md) |
| Read or change **Cultivation's own** settings | `CultivationConfigs` | [Config access](docs/config-access.md) |
| Add a playable race | `registerRace` | [Registries](docs/registries.md) |
| Add a technique cultivators can perform | `registerTechnique` | [Registries](docs/registries.md) |
| Make an item boost Qi absorption | `registerQiAbsorptionItemModifier` | [Registries](docs/registries.md) |
| Put my own page on the Cultivation menu bar | `registerMenuPage` | [UI integration](docs/ui.md) |
| Add an article to the in-game Codex | `registerCodexEntry` | [UI integration](docs/ui.md) |
| Expose my mod's settings in Cultivation's admin menu | `registerAdminConfigSection` | [UI integration](docs/ui.md) |
| Behave correctly beside Endless Leveling / PlaceholderAPI | `isEndlessLevelingInstalled` and friends | [Compatibility](docs/compatibility.md) |
| **Replace the entire realm/Qi ladder** with my own progression | `ProgressionProvider` | [Progression provider](docs/progression-provider.md) |
| Re-word the mod into a different setting | `CultivationTheme` | [Theming](docs/theming.md) |
| Re-**color** the menus, HUD and skill tree | `registerPalette` | [Palettes](docs/palettes.md) |
| Put my mod on the Info page, and tell admins when it is out of date | `registerUpdateCheck` | [Registries](docs/registries.md) |
| Tell whether my jar is the build that was published | `registerBuildCheck` | [Registries](docs/registries.md) |
| Retract a pairing that turned out to be broken after both shipped | `registerCompatCheck` | [Registries](docs/registries.md) |
| React to a cultivator's body tempering, or scale the XP it earns | `BodyTemperingEvents` | [Event reference](docs/events-reference.md) |
| React to a cultivator's fist art, or scale the XP it earns | `FistEvents` | [Event reference](docs/events-reference.md) |
| Change what a cultivator IS - a bloodline, a constitution, a physique | `registerModifierSource` | [Registries](docs/registries.md) |
| Add a row to the Overview page's bonus list | `newBonus`, `BonusStats` | [Registries](docs/registries.md) |
| Raise a player's Health, Stamina, Mana or Oxygen from my own mod | `applyStatBonus` | [Registries](docs/registries.md) |
| Give a spirit beast an art of my own | `registerBeastArt` | [Registries](docs/registries.md) |
| Ask how far a player has mastered an art | `getTechniqueMasteryStage`, `getTechniqueMasteryMultiplier` | [Reading player state](docs/reading-state.md) |
| Add a kind of sect building | `registerSectBuildingType` | [Registries](docs/registries.md) |
| Add a nature a Life-Bound treasure can roll | `registerLifeBoundTrait` | [Registries](docs/registries.md) |
| Ask what element the ground or a biome pushes toward | `getGroundDao`, `getTerrainElement` | [Driving progression](docs/driving-progression.md) |

The last three are the big ones — and the last two are easy to confuse, so:
`CultivationTheme` changes the **words**, `CultivationPalette` changes the
**colors**. A `ProgressionProvider` lets your mod own levelling
outright while sects, daos, techniques, beasts, formations, abodes, duels, alchemy
and the skill tree keep working on top of it — that is how the
[SoulRings](https://www.mermaids.dev/cultivation/) addon turns Cultivation into
*Douluo Dalu* without touching a line of it.

---

## Install

Cultivation is not on a public Maven repository. Install the released jar into
your local repository once:

```bash
mvn install:install-file \
  -Dfile=Cultivation-0.7.4.jar \
  -DgroupId=plugin.siren \
  -DartifactId=Cultivation \
  -Dversion=0.7.4 \
  -Dpackaging=jar
```

Then depend on it in `provided` scope:

```xml
<dependency>
    <groupId>plugin.siren</groupId>
    <artifactId>Cultivation</artifactId>
    <version>0.7.4</version>
    <scope>provided</scope>
</dependency>
```

> **`provided` is not optional.** The server loads the real Cultivation jar. If you
> shade Cultivation's classes into your own jar, there are two copies of
> `plugin.siren.Cultivation` on the classpath, each with its own static plugin
> instance, and every registry call you make goes into the copy nobody reads.

Finally, declare the dependency in your `manifest.json` so the plugin loader
resolves Cultivation's classes for you:

```json
"Dependencies": {
  "Siren:Cultivation": ">=0.7.4"
}
```

Use `OptionalDependencies` instead if your mod also works without Cultivation —
see [Getting started](docs/getting-started.md#optional-dependencies) for the guard
pattern that keeps the classes from ever being resolved on a server without it.

---

## 60 seconds

Everything registers once, from your plugin's `setup()`. Load order does not
matter — no registry is read until a player actually does something.

```java
public class MyAddon extends JavaPlugin {

    public MyAddon(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        // 1. React to something.
        CultivationEvents.onBreakthrough(event ->
                LOGGER.atInfo().log("A cultivator reached %s", event.newRealm().name()));

        // 2. Veto something.
        CultivationEvents.onPreBreakthrough(event -> {
            if (isBloodMoonTonight()) {
                event.setCancelled(true);
            }
        });

        // 3. Re-tune something, without touching Cultivation's config files.
        CultivationEvents.onPreQiGain(event -> event.setAmount(event.amount() * 2f));
    }
}
```

Reading a player's standing needs only their `ComponentAccessor` and `Ref` — which
you already have inside a system, a command, an interaction or an event listener:

```java
CultivationRealm realm = CultivationAPI.getRealm(accessor, ref);   // null if not a cultivator
int level = CultivationAPI.getGlobalLevel(accessor, ref);
float qi = CultivationAPI.getQi(accessor, ref);
```

---

## Documentation

| Guide | What's in it |
| --- | --- |
| **[Getting started](docs/getting-started.md)** | Dependency wiring, `setup()`, optional-dependency guard, your first listener |
| **[Reading player state](docs/reading-state.md)** | Realms, levels, Qi, race, skill nodes; components and `ComponentAccessor` |
| **[Driving progression](docs/driving-progression.md)** | Granting Qi, ranking up, meditation, spirit veins, and the rest of the world |
| **[Config access](docs/config-access.md)** | Reading and writing Cultivation's own settings, and when not to |
| **[Events](docs/events.md)** | Pre vs post, cancelling, re-tuning, threading, error handling |
| **[Event reference](docs/events-reference.md)** | All 160 listeners with their payloads — generated from source |
| **[Registries](docs/registries.md)** | Races, techniques, Qi-absorption items |
| **[Profiles](docs/profiles.md)** | Keeping your own progression in step with a player's separate saves |
| **[UI integration](docs/ui.md)** | Menu pages, Codex articles, admin config sections |
| **[Compatibility](docs/compatibility.md)** | Endless Leveling, PlaceholderAPI, Marriage; optional-dependency guards |
| **[Progression provider](docs/progression-provider.md)** | Replacing the realm/Qi ladder entirely |
| **[Theming](docs/theming.md)** | Re-wording the mod, and why a language file cannot do it |
| **[Palettes](docs/palettes.md)** | Re-coloring the mod, and why that means shipping documents rather than hex |
| **[Types](docs/types.md)** | The Cultivation and Hytale types the API hands you |
| **[Pitfalls](docs/pitfalls.md)** | The mistakes that cost the most time — read before shipping |
| **[Examples](examples/)** | A complete addon exercising every hook |

## Using this with an AI assistant

[**AGENTS.md**](AGENTS.md) is written for coding agents (Claude Code, Cursor,
Copilot and friends). Point your assistant at this repository and it will find the
hard constraints — threading rules, the ECS write rule, the naming conventions,
and the API's actual signatures — without guessing.

The fastest setup is to clone this repository beside your mod and tell your
assistant to read `AGENTS.md` first.

## Source layout

```
api-sources/plugin/siren/API/   The 24 public API classes, verbatim
docs/                            Guides and the generated event reference
examples/                        A worked example addon
tools/gen_events_reference.py    Regenerates docs/events-reference.md
```

`api-sources/` is a **reading copy**, not a buildable module. These classes
reference Cultivation's internals (components, managers, enums), so they only
compile inside the mod itself. Compile against the jar, per [Install](#install);
read the sources here for the javadoc, which is where the real contract is
written.

## Compatibility

The classes in `plugin.siren.API` are the stable surface — that is the whole point
of them. Everything else (`plugin.siren.ECS.*`, `plugin.siren.Utils.*`,
`plugin.siren.Cultivation` itself) is free to change shape between versions, so
reach for it only when the API has no equivalent, and expect to revisit it.

Some API methods hand you internal types anyway (`CultivationRealm`, `PlayerRace`,
`RaceConfig`, `TechniqueRule`). Those are stable in practice — see
[Types](docs/types.md) for which ones and what they mean.

## Links

- Mod website — <https://xianxia.dev/>
- Author — Siren, <https://www.mermaids.dev/>
