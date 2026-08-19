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
4. `docs/events-reference.md` — all 261 listeners with their payloads. Generated
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
`registerAdminConfigSection`, `registerPalette`) *do* have unregister methods, for
a plugin unloading cleanly.

### 3. Listeners run on the subject's world thread

Synchronously, on the world thread of the player the event happened to. So:

- Reading that player's components inside a listener is safe.
- **Do not block.** No sleeps, no `.get()` on a future, no file I/O.
- Touching an entity on a *different* world requires hopping threads first:
  `CompletableFuture.runAsync(task, otherWorld)`.
- A listener that throws is caught, logged and skipped — the mod and other
  addons keep working. Do not rely on this; it hides your bug.

**Two 0.8.0 classes have no subject player, so this rule does not apply as
written.** Check which you are writing before assuming a `ref()`:

- **`StoreBenefitEvents`** fires on the **remote checker thread** — grants are
  discovered by an HTTP sweep, so there is no world in hand, the payload carries a
  bare `UUID`, and the player is frequently offline. `Universe.get().getPlayer(uuid)`,
  check `isValid()`, hop to their world, then read components.
- **`CelestialEvents`** is server-wide. It *is* on a world thread (so still do not
  block), but it is whichever world reached the shared scheduler first, not a
  particular player's, and the payload has a `CelestialEventType` rather than a
  `ref()`. Hop per player before touching anybody.

### 4. `provided` scope, never shaded

Cultivation must be a `provided` Maven dependency. Two copies of
`plugin.siren.Cultivation` on the classpath each get their own static plugin
instance, and every registry call goes into the copy nobody reads. This failure
is silent — no exception, the feature simply never appears.

### 5. Namespace every id

Race ids, technique ids, menu page keys, codex entry ids, palette keys, admin
config section and field keys are all **global across every mod on the server**.
Namespace them with your mod's name — `"MyMod:flame_step"`, `"MyMod:alchemy"`.
Registering an existing id **replaces** the previous holder rather than erroring,
so a collision silently steals another mod's feature.

The same is true of asset paths: every mod's `Common/UI/Custom/` merges into one
tree, so a palette's `documentRoot` needs your mod's name in it too
(`"Pages/MyMod_Frost/"`).

### 6. Lang keys cannot override Cultivation's

Language files from every asset pack merge into one catalog, **first-writer-wins**.
Cultivation's pack loads before any addon that depends on it, so shipping
`server.cultivation.*` keys in your own `server.lang` is silently ignored. To
re-word Cultivation, implement `CultivationTheme` (see `docs/theming.md`) — words
only; its colors are a separate registry, `docs/palettes.md`. Keep your own
strings under your own key prefix.

### 7. A `ProgressionProvider` must call `refreshProgression`

If you install a `ProgressionProvider`, call
`CultivationAPI.refreshProgression(accessor, ref)` **every time you change a
player's level** (and their progress, if you want a live bar). Cultivation cannot
detect a write inside your component. Skip it and the HUD, the rankings and every
realm gate show that player's previous standing until their next meditation tick.

### 8. String UI properties take a String, and a `Message` disconnects the player

If you build UI — your own menu page, or an admin config field — `.TextSpans`
takes a `Message`, `.Text` takes a `String` (tolerating a bare translation
`Message`), and `.TooltipText` takes a `String` **only**. Pushing a `Message` at
`.TooltipText` disconnects the player mid-session rather than failing quietly.
That is why `withTooltip` takes a plain, untranslatable `String`.

Element ids in `.ui` documents also resolve **globally**, not per document, so a
page and a row template that share an id will fight. Prefix ids per document.

### 9. A `.ui` path that does not resolve kills the whole page

Not the one element — the **entire** UI load, as a blank screen with no log line,
and no validator in this repository or the workspace checks `append()` paths.

This is the constraint that shapes `CultivationPalette`: it redirects only the
documents it explicitly declared and falls back to the base path otherwise, and
the declared list is meant to be written by whatever generates the recolored
documents rather than by hand. Matching is on the **bare file name**
(`documentRoot + fileName`), so every document a palette declares must live under
its single `documentRoot` — including `CultivationHud.ui`, which the base mod
keeps in a different folder from its pages.

### 10. `CultivationTheme` and `CultivationPalette` are unrelated

The names mislead. `CultivationTheme` re-maps **translation keys** — words, and
nothing visual. `CultivationPalette` supplies **colors** — recolored `.ui`
documents plus the nine skill-tree halo hues. If the user asks to "theme" or
"reskin" Cultivation, work out which they mean before writing anything; they
compose freely, and a mod may register both.

### 11. A technique you register is LOCKED by default (0.7.4)

`Requires-Unlock` on a technique rule **defaults to `true`**, so an art you
register cannot be performed until the player comes by it — via a manual,
enlightenment while meditating, a breakthrough, or a sect hall's inscription.

Before 0.7.4 the equivalent flag was `Requires-Manual` and defaulted to *off*, so
a registered art fired the moment its realm gate passed. If you are updating an
addon written against an older version, this is the one behavior change to
re-test: the symptom is an art that silently never fires for a player who by
every visible measure should have it.

Call `.freelyAvailable()` on the rule to keep the old behavior. Either way a
server owner can override it per art in `Arts/TechniqueConfig.json`.

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

**Driving progression** — the write half; every one goes through the same path the
mod's own commands take, so events fire and the HUD refreshes:

```java
void    addQi(accessor, ref, float amount, @Nullable PlayerRef)  // gameplay: all multipliers + PreQiGainEvent
void    setQi(accessor, ref, float qi)                           // admin: raw, no events
void    setRealm(accessor, ref, CultivationRealm)                // admin: no ritual, no event
void    setStage(accessor, ref, CultivationStage)
boolean completeBreakthrough(accessor, ref, @Nullable PlayerRef) // gameplay: full ritual outcome
boolean completeAdvancement(accessor, ref, @Nullable PlayerRef)
void    demote(accessor, ref, @Nullable PlayerRef, boolean wasBreakthrough)

boolean isMaxLevel(accessor, ref)
float   getQiRequiredForNext(accessor, ref)
boolean isReadyForBreakthrough(accessor, ref)   // banked enough, needs only the ritual
boolean isReadyForAdvancement(accessor, ref)

void    grantSkillPoints(accessor, ref, int points)
boolean unlockSkillNode(accessor, ref, String nodeId)  // spends points
boolean grantSkillNode(accessor, ref, String nodeId)   // free, for a reward

void startMeditating(accessor, ref)   // stopMeditating does NOT apply the demotion penalty
void stopMeditating(accessor, ref)
```

There is deliberately **no `ascend(...)`**. The Ascension Capstone (0.8.0, the end
of the ladder) is observe-and-veto only, through `CultivationEvents.onPreAscension`
/ `onAscension` / `onAscensionFailed`. Note that by the time `AscensionEvent` fires
with `prestiged() == true` the player has **already** been reset to the first realm,
so capture any pre-ascension standing in the pre-event. Prestige is off by default,
and no getter exposes a player's ascension count between events — record what the
event hands you.

**The world, and the other subsystems** — chunk coords, not block coords:

```java
SpiritVeinManager.VeinReading readSpiritVein(World, int chunkX, int chunkZ)  // pure, creates nothing
float drainSpiritVein(World, int chunkX, int chunkZ, float amount)           // returns what was ACTUALLY drawn

Sect getSect(UUID)  Sect getSectByName(String)  float getSectQiBonusPercent(UUID)
DaoComponent getOrCreateDao(accessor, ref)  DaoElement getDaoElement(accessor, ref)
CultivationPath getPath(accessor, ref)  float getYinPercent(accessor, ref)  float getKarma(accessor, ref)
Dwelling getAbode(UUID)  Dwelling getDwellingAt(String world, int cx, int cz)
SpiritBeastComponent getBeast(accessor, ref)  DuelManager.Duel getDuel(UUID)
float getMeditationRegenMultiplier(String world, int cx, int cz, UUID player)
```

**Cultivation's own settings** — `CultivationConfigs`, one accessor per file
(`cultivation()`, `spiritVein()`, `dao()`, `sect()`, … , `endlessLeveling()`,
`update()`, `webStore()` (0.8.0), plus `race(PlayerRace)`). 0.9.x added accessors
for every new subsystem's config too — `daoComprehension()`, `talisman()`,
`forging()`, `weaponSpirit()`, `breeding()`, `party()`, `dungeon()`, `rival()`,
`treasure()`, `faction()`, `dreamTrial()`, `campaign()`, `oath()`, `market()`,
`tide()`, `partner()`, `masterDisciple()`, `qiDeviation()`, `tournament()` and
`celestial()` — plus `secretRealm()`, which previously had none. Each returns the
live `Config<T>` **holder**; call `.get()` at the point of use and `.save()` after
writing. To change a value for one player or one event, use the matching `Pre*`
event instead — a config write changes the server permanently and overwrites what
its owner tuned.

**Twelve config files have no `CultivationConfigs` accessor** — Bounty, Depths,
Fist, HeavenlyRealm, Land, Leaderboard, Meridian, Quest, Reclusive, Retreat,
SeaOfConsciousness and WorldBoss. Of the original seven named here through
0.8.0, Celestial, Master-Disciple, Qi Deviation, Secret Realm and Tournament all
gained one in 0.9.x; Fist and Land are the two survivors. The other ten are
config files behind 0.9.x subsystems that were never wrapped at all — several of
which (Depths, Meridian, Quest, WorldBoss) DO have a public `*Events` class, so
the events surface for those four is real even though their settings are not
reachable through this registry. Bounty, HeavenlyRealm, Leaderboard, Reclusive,
Retreat and SeaOfConsciousness have **no public API surface at all** — no
`*Events` class in `plugin.siren.API` either — so nothing in this repo covers
them yet.

Every one of these twelve is reachable as `Cultivation.getFistConfig()` and
friends, on the ordinary internals terms: `plugin.siren.Cultivation` may change
shape between versions. Prefer a `Pre*` event where one exists.

**Compatibility flags:**

```java
boolean isEndlessLevelingInstalled()   // EL owns health+damage; put your stat bonuses there too
boolean isPlaceholderApiRegistered()   // the %cultivation_...% expansion is answering
boolean isMarriageInstalled()          // Partnered Cultivation is live
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
    // Fluent config-DEFAULTS on the returned rule:
    //   .freelyAvailable()  (0.7.4) no unlock needed - see the warning below
    //   .charged()          (0.7.4) press once to gather, again to loose
    //   .unarmedOnly()      (0.7.4) refuses while a weapon is in hand
    //   .fusionOnly()       (0.8.0) only ever granted by Technique Fusion; stays
    //                       performable, but leaves the enlightenment/breakthrough/
    //                       manual/"grant all" pool
    //   .forRace(raceId)    locks the art to one race

boolean performTechnique(ComponentAccessor<EntityStore>, Ref<EntityStore>, PlayerRef, Technique)
void    registerQiAbsorptionItemModifier(String itemId, float multiplier)

BeastArt registerBeastArt(String id, String displayName, @Nullable String nameKey,
                          @Nullable String descriptionKey, BeastArtRule defaultRule,
                          BeastArtEffect effect)          // then list the id in the SPECIES' Arts
boolean  registerMasteryStage(MasteryStageRule)           // false = ladder already at 5 rungs
void     registerSectBuildingType(SectBuildingType)       // last ctor arg = does its ground carry the sect Dao
void     registerLifeBoundTrait(LifeBoundTrait)           // enters the weighted roll immediately

void registerMenuPage(CultivationMenuPage)          // + unregisterMenuPage(String)
void registerCodexEntry(CodexEntry)                 // + unregisterCodexEntry(String)
void registerCodexCategory(CodexCategory)           // + unregisterCodexCategory(String)
void registerAdminConfigSection(AdminConfigSection)  // + unregisterAdminConfigSection(String)
void registerPlayerAdminAction(PlayerAdminAction)    // + unregisterPlayerAdminAction(String)
                                                      // Players-tab row targeting the selected player
                                                      // (0.9.x) see docs/ui.md#player-admin-actions-09x
void registerPalette(CultivationPalette)            // + unregisterPalette(String)
void registerTitle(CultivationTitle)                // + unregisterTitle(String)
void registerSectBanner(SectBanner)                 // + unregisterSectBanner(String)

// A title is PURELY cosmetic. Its two gates ask different questions:
//   .permission(node) / .visible(Predicate<PlayerRef>)  may they SEE it   -> hidden if false
//   .unlocked(UnlockCheck)                              have they EARNED it -> greyed + .hint()
// UnlockCheck is (Store, Ref, PlayerRef), not Predicate<PlayerRef>, because every
// built-in title asks about the wearer's own components. Read-only; never write.
CultivationTitle CultivationTitle.builder(String key) ... .build()
@Nullable CultivationTitle getTitle(String key) | getTitle(Store, Ref)   // what they wear
List<CultivationTitle> getTitles()

AdminConfigSection newAdminConfigSection(String key, String labelKey, String hintKey,
                                         int sortOrder, Runnable save, List<AdminConfigField> fields)

// Five field kinds. Pick the one that matches the value's shape.
AdminConfigField newAdminConfigField (String key, Message label, DoubleSupplier, DoubleConsumer)   // NUMBER
AdminConfigField newAdminIntField    (String key, Message label, DoubleSupplier, DoubleConsumer)   // INT
AdminConfigField newAdminBooleanField(String key, Message label, BooleanSupplier, Consumer<Boolean>)
AdminConfigField newAdminChoiceField (String key, Message label, Supplier<List<AdminConfigChoice>>,
                                      Supplier<String> getter, Consumer<String> setter)            // dropdown
AdminConfigField newAdminTextField   (String key, Message label, Supplier<String>, Consumer<String>)
AdminConfigField withTooltip(AdminConfigField field, String tooltip)   // String, NEVER a Message

void setProgressionProvider(@Nullable ProgressionProvider)  // null restores built-in
void setTheme(@Nullable CultivationTheme)                   // null restores built-in
```

**Treasure Pavilion store benefits** (0.8.0) — see
[`docs/store-benefits.md`](docs/store-benefits.md):

```java
void registerStoreBenefit(StoreBenefit)     // + unregisterStoreBenefit(String key)
List<StoreBenefit> getStoreBenefits()
boolean hasStoreBenefit(@Nullable UUID playerUuid, String productSlug)

StoreBenefit.builder(String key, String productSlug)
        .name(String translationKey)     // how it is listed
        .title(String translationKey)    // ALSO registers a CultivationTitle for owners
        .build()
```

Two ids, NOT interchangeable: `key` is yours and must be namespaced; `productSlug`
is the store's. **`hasStoreBenefit` takes the slug** — passing the key returns
`false` forever, silently. `false` also means "system disabled" / "product
disabled" / "first sweep has not landed", so it means *do not apply this*, never
*they did not buy it*. Safe from any thread, answers for offline players.

`StoreBenefitEvents` (`onBenefitGranted`, `onBenefitRevoked`, `onSyncCompleted`) is
not cancellable and does **not** run on a world thread — see constraint 3.

**Celestial events** (0.8.0) — the one open registry OUTSIDE `plugin.siren.API`,
in `plugin.siren.Utils.Celestial`:

```java
CelestialManager.registerEventType(new CelestialEventType(
        String id, String nameKey, String weatherId,
        float durationMinutes, float weight, boolean enabled))

@Nullable CelestialEventType CelestialManager.active()   // read this from your own systems
```

Registering buys scheduling, the sky and the chat announcement — **not** a gameplay
effect. `CelestialEventType` is a thin descriptor, not a callback interface, so an
addon adds its effect by reading `active()` and acting while its id is running.
`CelestialEvents.onCelestialEventStart` / `onPreCelestialEventStart` /
`onCelestialEventEnd` are generic to every type; switch on `type().id()`.

**Palettes** (0.7.0) — the *color* half, unrelated to `CultivationTheme` above.
See [`docs/palettes.md`](docs/palettes.md); build one with
`CultivationPalette.builder(key)`:

```java
List<CultivationPalette>     getPalettes()                       // registration order
@Nullable CultivationPalette getPalette(String key)
@Nullable CultivationPalette getPalette(Store, Ref)              // null = the default look

String document(Store, Ref, String basePath)                     // route EVERY append through
String document(@Nullable CultivationPalette, String basePath)   // when built once per page

void buildMenuNav(UICommandBuilder, UIEventBuilder, PlayerRef, String pageKey, Store, Ref)
void buildMenuNav(UICommandBuilder, UIEventBuilder, PlayerRef, String pageKey)  // back-compat,
                                                        // ignores the palette - avoid

// Semantic colors: the strings colored from Java, which no document can re-grade.
// Eight meanings - POSITIVE, NEUTRAL, NEGATIVE, and (0.8.0) HEADING, ACCENT, BODY,
// MUTED, SECONDARY. Individually optional, UNLIKE the nine halos.
int    palette.getSemantic(Semantic, int fallbackRgb)
String palette.getSemanticHex(Semantic, String fallbackHex)
String CultivationPalette.hex(@Nullable CultivationPalette, Semantic, String fallbackHex)
Builder.semantic(Semantic, int rgb)
// ALWAYS pass the real default, never 0 / "#000000" - the fallback is what a
// palette with no opinion renders as. SECONDARY != POSITIVE even though both are
// jade by default: SECONDARY says which world a thing belongs to, not that it is good.
```

**Events** — one class per subsystem, all in `plugin.siren.API`:

`CultivationEvents`, `DaoEvents`, `TechniqueEvents`, `ItemEvents`, `BeastEvents`,
`SectEvents`, `WarEvents`, `DuelEvents`, `FormationEvents`, `DwellingEvents`,
`CelestialEvents`, `BodyTemperingEvents`, `FistEvents`, `ProfileEvents`,
`StoreBenefitEvents` — plus eighteen more that shipped through 0.9.x:
`DaoComprehensionEvents`, `ForgingEvents`, `TalismanEvents`, `WeaponSpiritEvents`,
`BreedingEvents`, `MeridianEvents`, `PartyEvents`, `PartnerEvents`, `OathEvents`,
`CampaignEvents`, `QuestEvents`, `DepthsEvents`, `SecretRealmEvents`,
`TreasureEvents`, `MarketEvents`, `TideEvents`, `RivalEvents`, `WorldBossEvents`.
See [docs/events-reference.md](docs/events-reference.md) for what each covers.

Every listener is `ClassName.onSomething(Consumer<SomethingEvent>)`, and nearly
every mechanic has both `onX` (post, notification) and `onPreX` (pre, cancellable
and re-tunable).

`CelestialEvents` and `StoreBenefitEvents` (0.8.0) are the two exceptions to that
shape: `CelestialEvents` has one pre for two posts (no `PreCelestialEventEnd`),
and `StoreBenefitEvents` has **no pre-events at all** — nothing there is
cancellable, deliberately. Both also break the threading rule above; see
[Hard constraints §3](#3-listeners-run-on-the-subjects-world-thread).

A handful of the 0.9.x classes are **post-only by design**, for the same
reason — not an oversight: `DepthsEvents`, `SecretRealmEvents` and (mostly)
`WorldBossEvents` report the deterministic next state of a machine that is
already running (a floor actually clearing, a site's own scheduler opening it,
a random spawn point being picked) rather than a request with a real veto point.
Read the class javadoc before assuming a subsystem is missing a pre-event by
mistake.

**Enums you will need** (outside `plugin.siren.API` — see `docs/types.md`):

```
CultivationRealm  BODY_REFINEMENT, QI_CONDENSATION, FOUNDATION_ESTABLISHMENT,
                  GOLDEN_CORE_FORMATION, NASCENT_SOUL, SOUL_FORMATION, VOID_REFINEMENT
CultivationStage  EARLY, MIDDLE, LATE, PEAK
DaoElement        WOOD, EARTH, WATER, FIRE, METAL, ICE, WIND, POISON, LIGHTNING, VOID
CultivationPath   UNALIGNED, RIGHTEOUS, DEVIL
SkillTreeBranch   VITALITY, RESILIENCE, MIGHT, WARDING, INSIGHT, HARMONY,
                  SWIFTNESS, ENDURANCE, SPIRIT   (a palette colors all nine or none)
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
- [ ] If a palette is registered: every declared document name exists under the
      one `documentRoot`, all nine halos are set, every `append` is routed through
      `CultivationAPI.document`, and `buildMenuNav` was given `store`/`ref`.
- [ ] If a `StoreBenefitEvents` or `CelestialEvents` listener is registered: it
      does not assume a world thread or a `ref()`, and it hops per player before
      touching a component.
- [ ] If `hasStoreBenefit` is called: it is passed the **product slug**, not the
      registry key, and a `false` answer is not persisted as "did not buy".

If you could not verify something against the sources, say which part and why —
do not present an unverified integration as working.
