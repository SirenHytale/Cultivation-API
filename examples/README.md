# Examples

`ExampleAddon/` is a complete, minimal Hytale mod that exercises every hook in the
Cultivation API. It is meant to be read top to bottom and copied from.

```
ExampleAddon/
├── pom.xml
└── src/main/
    ├── java/com/example/exampleaddon/
    │   ├── ExampleAddon.java      Plugin lifecycle - setup() and shutdown()
    │   ├── CultivationHooks.java  Every kind of event listener
    │   └── ExampleContent.java    Race, technique, Qi item, codex, admin section
    └── resources/
        └── manifest.json
```

| File | Shows |
| --- | --- |
| `ExampleAddon.java` | Dependency wiring, where registration goes, clean shutdown |
| `CultivationHooks.java` | Observing (post-events), vetoing and re-tuning (pre-events), cross-world thread hopping, null-guarding `player()`, and the two 0.8.0 event classes that have no subject world at all |
| `ExampleContent.java` | `registerRace`, `registerTechnique` + a technique effect, `registerQiAbsorptionItemModifier`, `registerCodexEntry`, `registerAdminConfigSection`, `registerMenuPage` |

The 0.8.0 additions are all in `CultivationHooks.java`: the Ascension capstone
(`onPreAscension` / `onAscension` / `onAscensionFailed`), Technique Fusion
(`onPreTechniqueFusion`), sect abbreviations, celestial events, and a
[Treasure Pavilion](../docs/store-benefits.md) benefit — including the thread hop a
store-benefit listener must make and the slug-vs-key trap it must avoid.

### One hook it deliberately does not show

**Palettes.** A palette is inseparable from the recolored `.ui` documents it points
at, and a path that does not resolve fails the whole UI load on the client — so an
illustrative, uncompiled `registerPalette` call with no documents behind it would
be worse than no example at all. [Palettes](../docs/palettes.md) carries a full
worked registration instead, alongside the reasoning for how the documents are
generated.

## Read this before copying

**It compiles.** Every source file here builds clean with `javac` against the
real `Cultivation-0.9.1.jar` and `Server-0.5.7.jar`, so every API signature in
it is verified rather than merely plausible. You cannot build it straight out
of this repository only because neither jar can be redistributed here — see
[Getting started](../docs/getting-started.md) for installing them.

What is still **illustrative** is the *behavior*: the private helpers at the bottom
of `CultivationHooks.java` (`isDoubleQiWeekend`, `hasHeavenlyMandate`, …) are
deliberate stand-ins returning constants, and the asset and lang ids it names
(`ExampleAddon_RadiantSpiritStone`, `server.exampleAddon.*`) belong to no shipped
pack.

Treat it as a shape to follow, not a drop-in module. Before shipping anything
derived from it:

1. Check each call against `api-sources/plugin/siren/API/`.
2. Re-read [`docs/pitfalls.md`](../docs/pitfalls.md).
3. Replace every stand-in helper and every placeholder id with your own.

## Building it for real

Follow [Getting started](../docs/getting-started.md) — install the Cultivation jar
into your local Maven repository, then `mvn clean install`.
