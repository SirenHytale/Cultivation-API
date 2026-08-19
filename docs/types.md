# Types

The API hands you two kinds of type: the ones defined in `plugin.siren.API`
itself, and a handful from Cultivation's internals that appear in signatures and
event payloads. This page covers the second group — what they mean and how far to
trust them.

## Stability

`plugin.siren.API` is the stable surface. Everything else —
`plugin.siren.ECS.*`, `plugin.siren.Utils.*`, `plugin.siren.Cultivation` — is
free to change shape between versions.

The types below are the exception in practice: they appear in API signatures, so
they cannot change without breaking the API. The **enums** in particular are safe
to rely on. For the **object types**, use the accessors you need and expect to
revisit them on a major version — and grep `api-sources/` rather than assuming a
getter exists.

---

## Enums

### `CultivationRealm`

`plugin.siren.ECS.Realms.CultivationRealm` — the seven realms, weakest first.
Declaration order is meaningful, so `ordinal()` compares.

| Constant | Name |
| --- | --- |
| `BODY_REFINEMENT` | Body Refinement |
| `QI_CONDENSATION` | Qi Condensation |
| `FOUNDATION_ESTABLISHMENT` | Foundation Establishment |
| `GOLDEN_CORE_FORMATION` | Golden Core Formation |
| `NASCENT_SOUL` | Nascent Soul |
| `SOUL_FORMATION` | Soul Formation |
| `VOID_REFINEMENT` | Void Refinement |

`CultivationRealm.fromName(String)` parses a configured name back into the enum,
or `null` if it does not match.

### `CultivationStage`

`plugin.siren.ECS.Realms.CultivationStage` — the sub-stage within a realm.

`EARLY` → `MIDDLE` → `LATE` → `PEAK`. A cultivator at `PEAK` is ready to attempt a
breakthrough into the next realm.

**Always `null` while a [`ProgressionProvider`](progression-provider.md) is
installed** — a replacement progression has no sub-stages.

### `DaoElement`

`plugin.siren.ECS.Dao.DaoElement` — the ten elemental daos (五行道), in two rings
of five. Each element overcomes the next in its own ring, wrapping; cross-ring
matchups are neutral.

| Ring | Cycle |
| --- | --- |
| Classic Wu Xing | `WOOD` → `EARTH` → `WATER` → `FIRE` → `METAL` → `WOOD` |
| Shadow | `ICE` → `WIND` → `POISON` → `LIGHTNING` → `VOID` → `ICE` |

Each maps to a real `DamageCause` asset id. `FIRE`, `ICE` and `POISON` use
vanilla causes; the rest use Cultivation's own shipped assets
(`Server/Entity/Damage/Cultivation_*.json`, all inheriting `Elemental`).

`WOOD` is the healing path — it converts damage into self-healing instead of
taking the flat damage bonus.

### `CultivationPath`

`plugin.siren.ECS.Dao.CultivationPath` — the moral path, derived from a
cultivator's Yin-Yang lean.

`UNALIGNED`, `RIGHTEOUS` (正道), `DEVIL` (魔道). Devil-path cultivators harvest Qi
from player kills; each path carries its own combat perks.

### `SkillTreeBranch`

`plugin.siren.ECS.SkillTree.SkillTreeBranch` — the nine directions the skill tree
radiates out in, one per stat. Appears in signatures only through
[`CultivationPalette`](palettes.md#halos-are-the-exception), which carries a halo
color per branch.

| Constant | Stat | Constant | Stat |
| --- | --- | --- | --- |
| `VITALITY` | Health | `HARMONY` | Ritual speed |
| `RESILIENCE` | Breath | `SWIFTNESS` | Move speed |
| `MIGHT` | Damage | `ENDURANCE` | Stamina |
| `WARDING` | Damage reduction | `SPIRIT` | Mana |
| `INSIGHT` | Qi gain | | |

**Declaration order is ring order**, 40° apart clockwise from straight up, and
each branch's neighbors (`previous()` / `next()`, wrapping) are what a tier-5/6
hybrid fork borrows from. A palette must give a color to **all nine or none** —
hue is what tells a player which branch a node belongs to.

### New in 0.9.x

Seven more standalone enums arrived with the 0.9.x subsystems, each carried by
its matching `*Events` payload:

| Enum | Package | Constants |
| --- | --- | --- |
| `HeavenlyDaoRank` | `plugin.siren.ECS.Dao` | `UNHEEDING`, `LISTENING`, `GLIMPSING`, `CONVERSANT`, `HEAVEN_ALIGNED` — declaration order is meaningful, each paired with a comprehension-fraction floor |
| `MeridianInjury` | `plugin.siren.ECS.Meridian` | `CRIPPLED_MERIDIAN`, `CRACKED_DANTIAN`, `SLACK_HAND` |
| `OathType` | `plugin.siren.Utils.Oath` | `NON_AGGRESSION`, `SECT_LOYALTY`, `WAGERED_DUEL`, `MASTER_TEACH` |
| `ForgeGrade` | `plugin.siren.Utils.Forging` | `LOW`, `MID`, `HIGH`, `PERFECT` |
| `TalismanGrade` | `plugin.siren.Utils.Talisman` | `LOW`, `MID`, `HIGH`, `PERFECT` |
| `SecretRealmTier` | `plugin.siren.Utils.Realm` | `ORDINARY`, `IMMORTAL_COURT` (仙庭, ascension-gated) |
| `TreasureTier` | `plugin.siren.Utils.Treasure` | `CACHE` (Buried Cache, in-place claim), `VAULT` (Ruin Vault, private-instance entry) |

Four more are declared **nested** inside the class that owns them, one level
down from the enums above but no less part of the stable signature since they
appear in a post-event's payload:

| Enum | Owner | Constants |
| --- | --- | --- |
| `DepthsRun.EndReason` | `plugin.siren.Utils.Depths.DepthsRun` | `EXTRACTED`, `DIED`, `ABANDONED` |
| `SecretRealmSite.Source` | `plugin.siren.Utils.Realm.SecretRealmSite` | how the site came to exist — see `SecretRealmOpenEvent` |
| `TideAssault.Result` | `plugin.siren.Utils.Tide.TideAssault` | `WON`, `LOST` |
| `WorldBossEncounter.Result` | `plugin.siren.Utils.Boss.WorldBossEncounter` | `WON`, `VANISHED`, `TIMED_OUT` |

Three more enums are declared **on the `*Events` class itself** rather than in
`Utils`/`ECS` — `ForgingEvents.ForgeOutcome`, `TalismanEvents.InscribeOutcome`
(both `SUCCESS`/`FAILED`/`BOTCH`) and `OathEvents.FlawCleanseRoute`
(`EXPIRED`/`ITEM`/`COMPANION`). These are listed with their full javadoc in
[the event reference](events-reference.md) rather than duplicated here.

---

## Object types

### `PlayerRace`

`plugin.siren.ECS.Races.PlayerRace` — an **open registry**, not an enum. Cultivation
ships Human and others; mods add their own via
[`registerRace`](registries.md#races).

```java
String  getId()
String  getDisplayName()
String  getTranslationKey()    // nullable
Message toMessage()

static Collection<PlayerRace> all()
static PlayerRace get(@Nullable String id)
static PlayerRace fromName(@Nullable String name)
```

The registry hands out singletons, so reference equality works — though comparing
`getId()` additionally covers another mod having registered your id first.

### `RaceConfig`

`plugin.siren.Utils.Config.RaceConfig` — a plain settings bag describing a race's
bonuses. You construct one and fill it in; see
[Registries](registries.md#the-stats-supplier) for the fields.

### `Technique` and `TechniqueRule`

`plugin.siren.ECS.Technique.Technique` — also an **open registry**, with the
built-ins exposed as static fields (`Technique.ONE_STEP_THOUSAND_LI`,
`SWORD_FLYING`, `SWORD_QI_SLASH`, `NINE_HEAVENS_THUNDER_PALM`, `IRON_BODY`,
`CLOUD_STEP`, `HEALING_PULSE`, `QI_BARRIER`, `QI_INFUSION`).

```java
String        getId()
String        getDisplayName()
String        getNameKey()          // nullable
String        getDescriptionKey()   // nullable
TechniqueRule getDefaultRule()
TechniqueEffect getEffect()
Message       toNameMessage()

static Collection<Technique> all()
static Technique fromId(@Nullable String value)
```

`plugin.siren.Utils.Config.TechniqueRule` is the tuning: `isEnabled()`,
`isDaoSpecific()`, `getRequiredElement()`, `getElements()`, `getDamageType()`,
`isRequiresManual()`, `getUnlockRealm()`, `getQiCost()`, `getCooldownSeconds()`,
`getParam(String key, float fallback)`.

Build one with `CultivationAPI.newTechniqueRule(...)` rather than the constructor.

### `TechniqueEffect` and `TechniqueContext`

`TechniqueEffect` is a functional interface — `void execute(TechniqueContext)`.
`TechniqueContext` carries everything an effect needs; see
[Registries](registries.md#the-effect) for the full member list.

### `SkillNode`

`plugin.siren.ECS.SkillTree.SkillNode` — one node of the radial skill tree,
carried by `SkillUnlockEvent` and `PreSkillUnlockEvent`. Node ids are strings
(`"VITALITY_1"` and friends); `CultivationAPI.isNodeUnlocked(accessor, ref, id)`
tests one without touching this type.

### `Sect`, `Siege`, `Formation`, `Dwelling`, `BeastSpecies`

Domain objects carried by their subsystems' event payloads:

| Type | Package | Carried by |
| --- | --- | --- |
| `Sect` | `plugin.siren.Utils.Sect` | `SectEvents` |
| `Siege` | `plugin.siren.Utils.War` | `WarEvents` |
| `Formation` | `plugin.siren.Utils.Formation` | `FormationEvents` |
| `FormationType` | `plugin.siren.ECS.Formation` | `FormationEvents` |
| `Dwelling` | `plugin.siren.Utils.Dwelling` | `DwellingEvents` |
| `BeastSpecies` | `plugin.siren.Utils.Config` | `BeastEvents` |
| `SectBuilding` | `plugin.siren.Utils.Sect` | `SectEvents` |
| `BeastEggMetadata` | `plugin.siren.Utils.Beast` | `BreedingEvents` *(0.9.x)* |
| `AuctionListing` | `plugin.siren.Utils.Market` | `MarketEvents` *(0.9.x)* |
| `PersonalDao` | `plugin.siren.ECS.Dao` | `DaoComprehensionEvents` *(0.9.x)* — an open registry like `Technique`/`BeastArt` below, not an enum; the built-in Sword/Slaughter/Space daos plus whatever a mod adds. Implements the `DaoComprehensionManager.Subject` marker interface a `DaoEnlightenmentEvent.subject()` may also hold. |

These are the least stable types in this list. Read what you need off them inside
a listener; do not build long-lived state around their shape.

### Rule types

Six plain data classes describe *what the server has configured*, as opposed to
*what a player has done*. Each is a config entry with a public constructor, so an
addon can build one and hand it to the matching registry in
[`registries.md`](registries.md) — with the one exception noted in the table:

| Type | Package | Describes |
| --- | --- | --- |
| `BeastArtRule` | `plugin.siren.Utils.Config` | one beast art's cost, cooldown, unlock realm and damage type |
| `MasteryStageRule` | `plugin.siren.Utils.Config` | one rung of the technique mastery ladder |
| `SectBuildingType` | `plugin.siren.Utils.Config` | a kind of sect building, and whether its ground carries the sect's Dao |
| `LifeBoundTrait` | `plugin.siren.Utils.Config` | a nature a bound treasure can roll, and the art it may unlock |
| `TechniqueParam` | `plugin.siren.Utils.Config` | one named number inside a rule (`Radius`, `BaseDamage`, …) |
| `TechniqueFusionRule` | `plugin.siren.Utils.Config` | *(0.8.0)* one fusion recipe — its two parents, its result, cost, odds and whether it consumes the parents. Config-only; no registry takes one, see [Technique Fusion](registries.md#technique-fusion). |

A rule's `damageType` names a **DamageCause asset**, not a `DaoElement`. Vanilla
ships `Fire`, `Ice`, `Poison`, `Physical` and friends; Cultivation adds one per
element (`Cultivation_Fire`, `Cultivation_Void`, …), and
`DaoElement.getDamageCauseId()` gives you the right string for an element. A name
that resolves to nothing **silently falls back to physical damage** rather than
erroring, so a typo here costs you the element without a log line.

### `CelestialEventType`

*New in 0.8.0.* `plugin.siren.Utils.Celestial.CelestialEventType` is a `record`
carried by both `CelestialEvents` payloads, and the thing you hand
`CelestialManager.registerEventType` to add a phenomenon of your own:

```java
record CelestialEventType(String id, String nameKey, String weatherId,
                          float durationMinutes, float weight, boolean enabled)
```

Deliberately a **thin descriptor, not a callback interface** — it buys scheduling,
the sky and the chat announcement, and nothing else. An event's actual gameplay
effect is the registrant's own systems reading `CelestialManager.active()`, which
is the same shape `SectBuildingType` uses. See
[Celestial event types](registries.md#celestial-event-types).

`id` is stable, lowercase and never shown to players — it is the
`/celestial start <id>` argument and the identity `active()` is compared against.
`weatherId` is a Hytale `Weather` asset id, so an addon supplying its own sky
ships that asset too.

### `StoreBenefit`

*New in 0.8.0.* The one type in this list that **is** in `plugin.siren.API`, built
through `StoreBenefit.builder(key, productSlug)` and carried by both
`StoreBenefitEvents` grant/revoke payloads. Its two ids are not interchangeable —
see [Treasure Pavilion benefits](store-benefits.md#two-ids-and-they-are-not-interchangeable).

### `BeastArt` and `BeastArtEffect`

`plugin.siren.ECS.Beast.BeastArt` is the beast-side counterpart of `Technique`,
and works identically: singleton instances from an open registry, safe to compare
with `==`, with `BeastArtEffect` as the one-method callback that runs the art.
`BeastArtContext` hands the callback **both** ends of the bond — `getBeastRef()`
for the creature performing the art and `getOwnerRef()` for the cultivator it
serves.

> `Ref` has no `equals()`. When an art has to skip its owner, or tell one
> entity from another, compare `getIndex()`.

---

## Hytale engine types

These come from the server, not from Cultivation:

| Type | |
| --- | --- |
| `Ref<EntityStore>` | A handle to an entity. Check `isValid()` after any thread hop. **`Ref` has no `equals()`** — compare identity with `getIndex()`. |
| `Store<EntityStore>` | The component store. Never write to it from inside a system. |
| `ComponentAccessor<EntityStore>` | What every callback hands you. May be a `Store` or a `CommandBuffer`. |
| `ComponentType<S, C>` | The key a component is read and written under. |
| `PlayerRef` | A player handle that survives world hops. `getReference()`, `getWorldUuid()`, `hasPermission(String)`. |
| `Message` | A localizable string. `Message.translation(key)` or `Message.raw(text)`. |
| `ItemStack` | An inventory stack, carried by several `ItemEvents` payloads. |
| `CustomUIPage` | The base of a custom UI page, for `registerMenuPage`. |
| `ChunkStore` | Where `SpiritVeinComponent` lives — veins belong to chunks, not entities. |
