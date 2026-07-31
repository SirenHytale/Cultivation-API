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
| `CultivationHooks.java` | Observing (post-events), vetoing and re-tuning (pre-events), cross-world thread hopping, null-guarding `player()` |
| `ExampleContent.java` | `registerRace`, `registerTechnique` + a technique effect, `registerQiAbsorptionItemModifier`, `registerCodexEntry`, `registerAdminConfigSection`, `registerMenuPage` |

### One hook it deliberately does not show

**Palettes.** A palette is inseparable from the recolored `.ui` documents it points
at, and a path that does not resolve fails the whole UI load on the client — so an
illustrative, uncompiled `registerPalette` call with no documents behind it would
be worse than no example at all. [Palettes](../docs/palettes.md) carries a full
worked registration instead, alongside the reasoning for how the documents are
generated.

## Read this before copying

These files are **illustrative**. Every Cultivation API call in them was written
against the real signatures in [`api-sources/`](../api-sources/), but the example
has not been compiled — it references a Hytale server jar and a Cultivation jar
that are not in this repository, plus a `MyAlchemyUIPage` that does not exist.

Treat it as a shape to follow, not a drop-in module. Before shipping anything
derived from it:

1. Check each call against `api-sources/plugin/siren/API/`.
2. Re-read [`docs/pitfalls.md`](../docs/pitfalls.md).
3. Compile against the real jars.

## Building it for real

Follow [Getting started](../docs/getting-started.md) — install the Cultivation jar
into your local Maven repository, then `mvn clean install`.
