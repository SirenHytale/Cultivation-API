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

These are the least stable types in this list. Read what you need off them inside
a listener; do not build long-lived state around their shape.

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
