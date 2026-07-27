# Instructions for AI coding assistants

You are helping someone write a Hytale server mod that integrates with
**Cultivation** (`plugin.siren:Cultivation`, plugin id `Siren:Cultivation`).
This repository is the API's reference distribution.

## Rule zero: do not invent signatures

`api-sources/plugin/siren/API/` contains the **actual Java sources** of every
public API class. They are heavily javadoc'd, and the javadoc is where the real
contract lives — threading, nullability, load-order guarantees, what cancelling
means, what a return value of `null` implies.

Before writing any call into this API, **read or grep the relevant source file.**
Do not reconstruct a signature from memory or from this document; this document
can go stale, `api-sources/` cannot.

```bash
# What can I do to a player's Qi?
grep -n "Qi" api-sources/plugin/siren/API/CultivationAPI.java

# What exactly does PreTechniquePerformEvent expose?
sed -n '/class PreTechniquePerformEvent/,/^    }/p' api-sources/plugin/siren/API/TechniqueEvents.java
```

If the user asks for something this API does not expose, say so plainly rather
than inventing a method. The honest answer is usually "the API has no hook for
that; the closest thing is X" — and quite often the pre-event surface *does*
cover it, because nearly every mechanic is re-tunable there.

## Orientation — read in this order

1. **`docs/pitfalls.md`** — the mistakes that crash servers. Read this first.
2. `docs/getting-started.md` — dependency wiring and `setup()`.
3. The guide for whatever the user is doing (see `README.md`'s table).
4. `docs/events-reference.md` — all 135 listeners with their payloads. Generated
   from source, so it is accurate; it is long, so search it rather than reading
   it end to end.

## Hard constraints

These are not style preferences. Getting one wrong takes the server down, or
produces a bug that only shows up on a live server under load.

### 1. Never write to the `Store` from inside a system

`ComponentAccessor` may be a `CommandBuffer` rather than a `Store`. Reads are
fine through either. **Component creation and removal must go through the
`CommandBuffer`** — calling `ref.getStore().putComponent(...)` from inside a
ticking system throws `"Store is currently processing!"` and takes the world
thread down with it.

This applies inside event listeners, `ProgressionProvider` methods, technique
effects, and anything else this API calls back into.

### 2. Everything registers from `setup()`, once

Registries, listeners, providers and themes are all registered from your
plugin's `setup()`. **Load order relative to Cultivation does not matter** —
nothing reads any of these registries until a player actually meditates, opens a
menu, or trips a gate, which is long after every plugin has loaded. Do not write
retry loops, deferred initializers, or `LoadBefore` entries to "make sure
Cultivation is ready".

There is deliberately **no unregister for event listeners** — listener lifetime
is server lifetime. The registries (`registerMenuPage`, `registerCodexEntry`,
`registerAdminConfigSection`) *do* have unregister methods, for a plugin
unloading cleanly.

### 3. Listeners run on the subject's world thread

Synchronously, on the world thread of the player the event happened to. So:

- Reading that player's components inside a listener is safe.
- **Do not block.** No sleeps, no `.get()` on a future, no file I/O.
- Touching an entity on a *different* world requires hopping threads first:
  `CompletableFuture.runAsync(task, otherWorld)`.
- A listener that throws is caught, logged and skipped — the mod and other
  addons keep working. Do not rely on this; it hides your bug.

### 4. `provided` scope, never shaded

Cultivation must be a `provided` Maven dependency. Two copies of
`plugin.siren.Cultivation` on the classpath each get their own static plugin
instance, and every registry call goes into the copy nobody reads. This failure
is silent — no exception, the feature simply never appears.

### 5. Namespace every id

Race ids, technique ids, menu page keys, codex entry ids, admin config section
and field keys are all **global across every mod on the server**. Namespace them
with your mod's name — `"MyMod:flame_step"`, `"MyMod:alchemy"`. Registering an
existing id **replaces** the previous holder rather than erroring, so a collision
silently steals another mod's feature.

### 6. Lang keys cannot override Cultivation's

Language files from every asset pack merge into one catalog, **first-writer-wins**.
Cultivation's pack loads before any addon that depends on it, so shipping
`server.cultivation.*` keys in your own `server.lang` is silently ignored. To
re-word Cultivation, implement `CultivationTheme` (see `docs/theming.md`). Keep
your own strings under your own key prefix.

### 7. A `ProgressionProvider` must call `refreshProgression`

If you install a `ProgressionProvider`, call
`CultivationAPI.refreshProgression(accessor, ref)` **every time you change a
player's level** (and their progress, if you want a live bar). Cultivation cannot
detect a write inside your component. Skip it and the HUD, the rankings and every
realm gate show that player's previous standing until their next meditation tick.

## Conventions to follow

- **Suppliers, not snapshots.** Where the API takes a `Supplier<RaceConfig>` or a
  `DoubleSupplier`, read through the supplier rather than capturing the config
  *object*. A config reload replaces the instance behind the holder, and a
  captured one then edits a discarded copy.
- **Translation keys, not raw text.** Cultivation is localized. Prefer
  `Message.translation("server.mymod.x")` / the `label(String translationKey)`
  builder overloads over `Message.raw(...)` in anything a player sees.
- **Full `server.` prefix in code.** Keys are bare inside a `server.lang` file but
  written in full (`server.mymod.x`) everywhere in Java and JSON.
- **Pre-event to change, post-event to observe.** If the user wants to *alter* a
  mechanic, reach for the `Pre*` event's setters before anything else — that is
  the supported way to re-tune Cultivation without patching its configs.

## Quick signature reference

Verify against `api-sources/` before use. All static, all on `CultivationAPI`
unless noted.

**Reading state** — every one takes `(ComponentAccessor<EntityStore>, Ref<EntityStore>)`:

```java
CultivationRealm getRealm(...)            // null if not a cultivator
CultivationStage getStage(...)            // null under a ProgressionProvider
int              getGlobalLevel(...)      // 0 if no component
float            getQi(...)               // banked progress toward next rank
PlayerRace       getRace(...)             // defaults to Human
boolean          isMeditating(...)
int              getAvailableSkillPoints(...)
boolean          isNodeUnlocked(..., String nodeId)
void             refreshProgression(...)  // @Nonnull args
```

**Registration** (from `setup()`):

```java
PlayerRace registerRace(String id, String displayName, @Nullable String translationKey,
                        CultivationRealm unlockRealm, Supplier<RaceConfig> stats)

Technique  registerTechnique(String id, String displayName, @Nullable String nameKey,
                             @Nullable String descriptionKey, TechniqueRule defaultRule,
                             TechniqueEffect effect)

TechniqueRule newTechniqueRule(String id, boolean enabled, boolean daoSpecific,
                               @Nullable String requiredElement, @Nullable String elements,
                               @Nullable String damageType, String unlockRealm,
                               float qiCost, float cooldownSeconds, Object... params)

boolean performTechnique(ComponentAccessor<EntityStore>, Ref<EntityStore>, PlayerRef, Technique)
void    registerQiAbsorptionItemModifier(String itemId, float multiplier)

void registerMenuPage(CultivationMenuPage)          // + unregisterMenuPage(String)
void registerCodexEntry(CodexEntry)                 // + unregisterCodexEntry(String)
void registerCodexCategory(CodexCategory)           // + unregisterCodexCategory(String)
void registerAdminConfigSection(AdminConfigSection)  // + unregisterAdminConfigSection(String)

AdminConfigField newAdminConfigField(String key, Message label,
                                     DoubleSupplier getter, DoubleConsumer setter)

void setProgressionProvider(@Nullable ProgressionProvider)  // null restores built-in
void setTheme(@Nullable CultivationTheme)                   // null restores built-in
```

**Events** — one class per subsystem, all in `plugin.siren.API`:

`CultivationEvents`, `DaoEvents`, `TechniqueEvents`, `ItemEvents`, `BeastEvents`,
`SectEvents`, `WarEvents`, `DuelEvents`, `FormationEvents`, `DwellingEvents`.

Every listener is `ClassName.onSomething(Consumer<SomethingEvent>)`, and nearly
every mechanic has both `onX` (post, notification) and `onPreX` (pre, cancellable
and re-tunable).

**Enums you will need** (outside `plugin.siren.API` — see `docs/types.md`):

```
CultivationRealm  BODY_REFINEMENT, QI_CONDENSATION, FOUNDATION_ESTABLISHMENT,
                  GOLDEN_CORE_FORMATION, NASCENT_SOUL, SOUL_FORMATION, VOID_REFINEMENT
CultivationStage  EARLY, MIDDLE, LATE, PEAK
DaoElement        WOOD, EARTH, WATER, FIRE, METAL, ICE, WIND, POISON, LIGHTNING, VOID
CultivationPath   UNALIGNED, RIGHTEOUS, DEVIL
```

## Before you tell the user it's done

- [ ] Every API call's signature matched against `api-sources/`, not memory.
- [ ] Cultivation is `provided` scope in `pom.xml`.
- [ ] `manifest.json` declares `"Siren:Cultivation"` under `Dependencies` (or
      `OptionalDependencies`, with every entry point guarded).
- [ ] All registration happens in `setup()`; nothing waits on load order.
- [ ] Every id is namespaced with the mod's name.
- [ ] No `putComponent` on a `Store` from inside a system or listener.
- [ ] Player-facing strings are translation keys under the mod's own prefix.
- [ ] If a `ProgressionProvider` is installed: `refreshProgression` is called on
      every level change, and `shutdown()` passes `null` back to
      `setProgressionProvider` and `setTheme`.

If you could not verify something against the sources, say which part and why —
do not present an unverified integration as working.
