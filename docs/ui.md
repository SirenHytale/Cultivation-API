# UI integration

Three ways to put your mod's interface inside Cultivation's, rather than behind a
command of its own.

---

## Menu pages

Every Cultivation menu carries a shared nav bar. `registerMenuPage` puts your page
on it, alongside Overview, Race, Dao and the rest. The bar scrolls horizontally,
so there is no practical limit on how many pages may be added.

```java
CultivationAPI.registerMenuPage(
        CultivationMenuPage.builder("myAddon:alchemy")
                .label("server.myaddon.nav.alchemy")
                .sortOrder(450)
                .permission("myaddon.alchemy")
                .onOpen((store, ref, playerRef) ->
                        CultivationAPI.openMenuPage(store, ref, new MyAlchemyUIPage(playerRef)))
                .build());
```

### Builder options

| Method | |
| --- | --- |
| `label(String translationKey)` | Button text, localized. The normal case. |
| `label(Message)` / `label(Supplier<Message>)` | Fixed text, or text computed per draw (e.g. carrying a live count) |
| `sortOrder(int)` | Position; lower is further left |
| `permission(String)` | Hides the button, and refuses to open the page, without it |
| `visible(Predicate<PlayerRef>)` | A gate a permission cannot express — e.g. only once the player has joined a sect. Combines with `permission`; both must pass. |
| `onOpen(Opener)` | **Required.** What the button does. |

### Ordering

```java
CultivationMenuPage.SORT_FIRST    // 100  - where Cultivation's first page sits
CultivationMenuPage.SORT_DEFAULT  // 1000 - after every built-in except Admin
CultivationMenuPage.SORT_LAST     // 9000 - where Admin sits, deliberately last
```

The built-ins are spaced 100 apart from `SORT_FIRST`, so `sortOrder(450)` lands
between the Skill Tree and Bonuses without anything needing renumbering.

### The built-in pages

Their ids are bare words — which is exactly why yours must be namespaced.

| Id | Page | Sort order |
| --- | --- | --- |
| `overview` | Stats (active bonuses folded in) | 100 |
| `settings` | Settings | 200 |
| `race` | Identity — race and titles; the id is kept so a replacement still replaces it | 300 |
| `skilltree` | Skill tree | 400 |
| `dao` | Spiritual Root | 600 |
| `way` | Dao Comprehension | 700 |
| `codex` | Codex | 800 |
| `sense` | Spirit Sense — hidden until the viewer's realm can perceive anything | 850 |
| `misc` | Everything without its own button: Rankings, Sect, Land Protection and the command-opened pages | 900 |
| `admin` | Admin — gated on `cultivation.admin` | 9000 (`SORT_LAST`) |
| `info` | Info — deliberately past `SORT_LAST` so it stays the final button | 9100 |

Six more ids open real pages but have no button: `bonuses` (folded into
`overview`), `titles` (folded into `race`), `profiles` and `keybinds` (reached
from `settings`), and, since 0.10.1, `daomastery` and `daoinheritance`
(reached from a link row on `dao`). Registering any of them yourself puts a
button back, since the bar is drawn purely from this registry.

### Taking over a built-in page

Registering an existing id **replaces** it, so pointing the Race button at your
own page is:

```java
CultivationAPI.registerMenuPage(
        CultivationMenuPage.builder("race")
                .label("server.myaddon.nav.myRace")
                .onOpen((store, ref, playerRef) ->
                        CultivationAPI.openMenuPage(store, ref, new MyRacePage(playerRef)))
                .build());
```

This is also why every id you do *not* mean to replace must be namespaced.

### Putting the bar on your own page

Four steps, in your `CustomUIPage`:

1. Give the layout a placeholder:

   ```
   Group #NavBar { LayoutMode: Left; Anchor: (Bottom: 4, Height: 46); }
   ```

2. Draw the bar from your `build`:

   ```java
   CultivationAPI.buildMenuNav(commandBuilder, eventBuilder, playerRef,
           "myAddon:alchemy", store, ref);
   ```

   Passing your own id disables that button, marking where the player is.

   > Prefer this overload. There is a four-argument one without `store`/`ref`,
   > kept so pages written before 0.7.0 still compile, but it cannot see which
   > [palette](palettes.md) the viewer is wearing and always draws the bar in
   > Cultivation's crimson and gold — a strip of the old colors across the
   > bottom of an otherwise recolored menu.

3. Add the nav key to your event codec. It carries a literal page id, **not** a
   selector, so it takes no `'@'` prefix:

   ```java
   .addField(new KeyedCodec<>(CultivationAPI.MENU_NAV_EVENT_KEY, Codec.STRING),
           (data, value) -> data.nav = value, data -> data.nav)
   ```

4. Hand the field to the handler from your `handleDataEvent`:

   ```java
   CultivationAPI.handleMenuNav(store, ref, playerRef, data.nav);
   ```

   Safe to call with the `null` the field holds when the event was one of your own
   buttons, and with an id the player is not allowed to open.

### Threading

`getLabel`, `isVisibleTo` and `open` are called on the viewing player's world
thread while their page is being built or swapped. Read that player's components
freely; **do not write to the `Store`**.

### Drawing your page in the viewer's colors

A player may be wearing a [palette](palettes.md), in which case Cultivation's own
pages are drawn from a recolored set of `.ui` documents. Your page keeps its own
look unless you opt in — route each `append` through `CultivationAPI.document`
and ship variants of your documents in the palette's folder. Rows and fragments
included; a themed page that appends an unthemed row draws that row in the old
colors.

---

## Codex articles

The in-game Codex is Cultivation's reference book. An article added here sits in
the index beside the mod's own and reads identically.

```java
CultivationAPI.registerCodexEntry(
        CodexEntry.builder("myAddon:alchemy")
                .title("server.myaddon.codex.alchemy.title")
                .summary("server.myaddon.codex.alchemy.summary")
                .category(CodexCategory.CRAFT)
                .sortOrder(250)
                .body(page -> page
                        .heading("server.myaddon.codex.alchemy.costs")
                        .paragraph("server.myaddon.codex.alchemy.intro")
                        .stat("server.myaddon.codex.alchemy.brewTime",
                              MyAddon.get().getConfig().get().getBrewSeconds())
                        .recipe("MyAddon_ElixirOfNight")
                        .divider()
                        .noteIf(CultivationAPI.getGlobalLevel(page.getAccessor(), page.getRef()) < 10,
                                "server.myaddon.codex.alchemy.locked"))
                .build());
```

### Write against the reader, not as fixed prose

The body is written **fresh every time somebody opens it**, and the page carries
the reader with it (`getAccessor()`, `getRef()`, `getPlayerRef()`). An article
that states this server's real configured numbers and what the reader has actually
reached is the reason to have an in-game codex at all — it is the one thing a wiki
cannot do.

### Block types

| Call | Renders |
| --- | --- |
| `heading(...)` | A section heading |
| `paragraph(...)` | Body text; wraps |
| `stat(label, value)` | A label/value row. Overloads for `Message`, `String`, `int`, `float` |
| `note(...)` / `noteIf(condition, ...)` | A dimmed aside — a caveat, or something true of this reader only |
| `recipe(itemId)` | A crafting recipe, resolved live from the item's own asset. Pass the id of the item that gets **crafted**. An item with no recipe renders nothing. |
| `divider()` | A thin rule |

Every call returns the page, so bodies chain. Two helpers keep numbers readable:
`CodexPage.formatNumber(float)` drops trailing zeroes, and
`CodexPage.formatPercent(float)` turns a 0–1 multiplier into `"25%"`.

### Categories

Four ship with the mod:

| Constant | For |
| --- | --- |
| `CodexCategory.PATH` | Realms, Qi, breakthroughs, tribulation — the progression itself |
| `CodexCategory.SELF` | What a cultivator becomes: race, dao, techniques, companions |
| `CodexCategory.WORLD` | What is out there: veins, sects, abodes, formations, other cultivators |
| `CodexCategory.CRAFT` | Making things: alchemy, refinement, manuals |

Filing under one of those is usually better for a reader than adding a fifth — a
reader looking for spirit beasts wants them under Self, not under the name of the
mod that happened to add them. If you do need your own:

```java
CultivationAPI.registerCodexCategory(
        CodexCategory.of("myAddon:brewing", "server.myaddon.codex.category.brewing", 450));
```

### Visibility

`visible(Predicate<PlayerRef>)` hides an article. Use it sparingly: **a codex that
hides what you have not unlocked cannot tell you how to unlock it.** Prefer saying
so in the body — that is what `noteIf` is for.

---

## Admin config sections

Puts a group of your mod's tunable numbers into Cultivation's admin menu, as its
own section button on the Config tab. Editing and saving work exactly as they do
for Cultivation's own settings, and both host pages are gated on
`cultivation.admin`, so a section may expose real balance numbers.

```java
public final class MyAdminSections {
    private static final String PREFIX = "MyAddon:";

    public static void registerAll() {
        CultivationAPI.registerAdminConfigSection(balance());
    }

    public static void unregisterAll() {
        CultivationAPI.unregisterAdminConfigSection(PREFIX + "balance");
    }

    private static AdminConfigSection balance() {
        return new AdminConfigSection() {
            @Nonnull public String getKey()   { return PREFIX + "balance"; }
            @Nonnull public Message getLabel(){ return Text.of("server.myaddon.admin.balance"); }
            @Nonnull public Message getHint() { return Text.of("server.myaddon.admin.balance.hint"); }

            @Nonnull public List<AdminConfigField> getFields() {
                return List.of(
                        CultivationAPI.newAdminConfigField(PREFIX + "BrewSeconds",
                                Text.of("server.myaddon.admin.brewSeconds"),
                                () -> MyAddon.get().getConfig().get().getBrewSeconds(),
                                v  -> MyAddon.get().getConfig().get().setBrewSeconds((float) v)),

                        // Stored as a 0-1 fraction, edited as a percent: the row
                        // widget shows two decimals, so a raw 0.004 reads as 0.00.
                        CultivationAPI.newAdminConfigField(PREFIX + "DropChance",
                                Text.of("server.myaddon.admin.dropChance"),
                                () -> MyAddon.get().getConfig().get().getDropChance() * 100f,
                                v  -> MyAddon.get().getConfig().get().setDropChance((float) (v / 100f))));
            }

            public void save() {
                MyAddon.get().getConfig().save();
            }
        };
    }
}
```

### Building a section in one call

Implementing the interface is fine, but `newAdminConfigSection` builds the same
thing from its parts, which reads better for a plain list of settings:

```java
CultivationAPI.registerAdminConfigSection(
        CultivationAPI.newAdminConfigSection(PREFIX + "power",
                "server.myaddon.admin.power",        // label key
                "server.myaddon.admin.powerHint",    // hint key
                AdminConfigSection.SORT_LAST,
                config::save,
                List.of(
                        CultivationAPI.newAdminBooleanField(PREFIX + "Enabled",
                                Message.translation("server.myaddon.admin.enabled"),
                                () -> config.get().isEnabled(),
                                config.get()::setEnabled),

                        CultivationAPI.withTooltip(
                                CultivationAPI.newAdminIntField(PREFIX + "MaxTier",
                                        Message.translation("server.myaddon.admin.maxTier"),
                                        () -> config.get().getMaxTier(),
                                        v -> config.get().setMaxTier((int) v)),
                                "How far the tier ladder goes. Existing gear is not re-tiered."))));
```

### Field kinds

Since 0.6.1 a row is one of five kinds, each backed by the vanilla widget of that
shape. Everything that does not apply to a kind has a harmless default, so an
implementation only ever writes the half it uses — and a field written before
kinds existed is a `NUMBER` and behaves exactly as it always did.

| Kind | Factory | Value read/written through |
| --- | --- | --- |
| `NUMBER` | `newAdminConfigField` | `get()` / `set(double)` |
| `INT` | `newAdminIntField` | `get()` / `set(double)`, rounded, no decimal places |
| `BOOLEAN` | `newAdminBooleanField` | `getBoolean()` / `setBoolean(boolean)` |
| `CHOICE` | `newAdminChoiceField` | `getText()` / `setText(String)`, options from `getChoices()` |
| `TEXT` | `newAdminTextField` | `getText()` / `setText(String)` |

A **`CHOICE`** takes a `Supplier<List<AdminConfigChoice>>`, re-read on every
render — so a set that depends on what other mods have registered stays current:

```java
CultivationAPI.newAdminChoiceField(PREFIX + "UnlockRealm",
        Message.translation("server.myaddon.admin.unlockRealm"),
        () -> Arrays.stream(CultivationRealm.values())
                    .map(r -> AdminConfigChoice.of(r, r.getTranslationKey()))
                    .toList(),
        () -> config.get().getUnlockRealm(),
        id -> {
            CultivationRealm parsed = CultivationRealm.fromName(id);   // re-resolve, don't trust
            if (parsed != null) {
                config.get().setUnlockRealm(parsed.name());
            }
        });
```

Build choices with `AdminConfigChoice.translated(id, labelKey)`,
`AdminConfigChoice.raw(id, displayName)`, or `AdminConfigChoice.of(enumConstant,
labelKey)`. Reach for `CHOICE` over `TEXT` whenever the valid values are a known
set — a dropdown cannot be typed wrong.

### Tooltips

`withTooltip(field, String)` wraps any field, adding a one-line explanation shown
as the row's tooltip. It composes with every factory.

> **A plain `String`, deliberately — a tooltip cannot be translated.**
> `TooltipText` is a String property client-side, and handing it a `Message`
> **disconnects the player mid-session** rather than failing quietly. Anything
> that must be readable in every language belongs in the field's *label*, which is
> rendered through `TextSpans` and does take a `Message`.

### Ordering and visibility

```java
default int     getSortOrder() { return SORT_LAST; }
default boolean isVisible()    { return true; }
```

Sections sit on the rail lowest-first. Cultivation's own occupy
`SORT_BUILTIN_FIRST` (100) upward in steps of 100, so a value between two of them
slots your section in **among** them rather than after them. `SORT_LAST`
(100,000) is the default and where every section registered before ordering
existed has always appeared. Sections declaring the same order keep registration
order.

`SORT_BUILTIN_LAST` (10,000) is the end of the range Cultivation reserves for
itself. A section ordered above it counts as contributed by another mod, which is
what the settings menu lists — the admin page lists everything either way.

`isVisible()` is read on every render, so a section belonging to a subsystem the
server owner has switched off can hide itself rather than offering settings that
do nothing.

### Rules

- **One section per config file** is the shape Cultivation uses for itself and the
  one that reads best.
- **Read through the holder**, as above — never capture the config object. A
  reload replaces the instance behind it and a captured one edits a discarded copy.
- **Return a stable field list.** The page matches an admin's in-flight edits to
  fields by key, so a list that changes shape between render and save silently
  drops those edits.
- **Field keys are global** across every section of every mod. Namespace them.
- **Anything list-shaped belongs in the config file**, not this editor.
  Cultivation makes the same call for its own Qi-absorption item table and its
  technique rule set.
- **Clamp in `set`**, not after. The page re-displays whatever `get` returns, so a
  coerced value is shown back to the admin rather than silently disagreeing with
  what they typed. Persisting is `save()`'s job.
- **Re-resolve a `CHOICE` id in the setter.** It arrived from a client, and an
  option that has since stopped being valid must not be accepted just because it
  was once offered.

Cultivation's own 19 sections and 109 rows are ordinary API registrations built
this same way — which is what lets an addon reorder, hide or replace one.

## Player admin actions (0.9.x)

The per-player analog of an admin config section: a `PlayerAdminAction` adds a
generic dropdown-plus-button row to `/cultivation admin`'s **Players tab**,
appended below Cultivation's own fixed rows (Realm, Stage, Race, Qi, Ascension,
Level), acting on whichever player the admin currently has selected.

Reach for this instead of an admin config section when the value you are
editing belongs to *one player*, not the server — a bloodline, a constitution,
a Heavenly Flame. Cultivation owns Realm/Stage/Race/Qi/Ascension/Level directly
and renders them as fixed rows; it must never be taught one addon's specific
vocabulary to add a new one, so every other per-player admin control goes
through this registry instead.

```java
CultivationAPI.registerPlayerAdminAction(new PlayerAdminAction() {
    @Nonnull public String getKey()         { return "HeavenlyFlames:setFlame"; }
    @Nonnull public Message getLabel()      { return Message.translation("server.heavenlyflames.admin.fieldFlame"); }
    @Nonnull public Message getButtonLabel(){ return Message.translation("server.heavenlyflames.admin.setFlame"); }
    @Nullable public String getTooltip()    { return null; }

    @Nonnull public List<AdminConfigChoice> getChoices() { return HeavenlyFlame.allAsChoices(); }
    @Nonnull public String getDefaultValue(){ return HeavenlyFlame.NONE; }

    public void apply(Store<EntityStore> targetStore, Ref<EntityStore> targetRef,
            PlayerRef targetPlayerRef, PlayerRef actingAdmin, boolean targetingSelf, String value) {
        HeavenlyFlameManager.applySet(targetStore, targetRef, targetPlayerRef, actingAdmin, targetingSelf, value);
    }
});
```

Call from `setup()`; withdraw it from `shutdown()` with
`CultivationAPI.unregisterPlayerAdminAction(key)`, or a reload leaves a row
behind pointing at classes that are gone. Registering the same key twice
replaces the first, so this is also safe across your own plugin's reload.

### Key namespacing is enforced, not just conventional

`getKey()` doubles as the literal action string the row's button sends back,
riding the same channel Cultivation's own built-in actions (`"setrealm"`,
`"setlevel"`, …) use. Registration **rejects** a key that has no `:` separator,
or that exactly matches one of Cultivation's own reserved built-in keys — logged
as a warning naming the caller and the offending key, and the action is simply
never added (this does not throw; a misbehaving registration cannot crash the
caller's own plugin load). Namespace it — `"HeavenlyFlames:setFlame"` — the
same rule every other id in this API follows.

### Threading — the one place a direct `Store` write is correct

`apply` is handed a real `Store<EntityStore>`, not a `CommandBuffer`, and
writing through it directly — including `Store.putComponent` — is the
established pattern here, unlike a ticking system or event listener. See
[pitfall #18](pitfalls.md#18-routing-playeradminactionapply-through-a-commandbuffer-09x)
for why this is a deliberate exception, not a hole in the "never write to the
Store" rule. `apply` still runs resolved onto the **target's** own world
thread, which may differ from the admin's — report outcomes only through
`PlayerRef#sendMessage` on `targetPlayerRef`/`actingAdmin`.

### Ordering and visibility

Same shape as an admin config section: `getSortOrder()` defaults to
`AdminConfigSection.SORT_LAST`, and `isVisible()` (default `true`) is read on
every render, so a row belonging to a subsystem the server owner switched off
can hide itself. Unlike a config section, the Players tab's own fixed rows are
not part of this registry at all — an addon row always appears below them
regardless of `getSortOrder()`.

## Identity sections (0.10.0)

A third (or fourth, …) tab on Cultivation's **Identity page**, alongside the
built-in Race and Titles tabs. Race and Titles are not entries in this
registry — they keep the partial-update logic specific to their own content —
but every registered section gets a tab button alongside them, always
rendering after both built-ins.

```java
CultivationAPI.registerIdentitySection(CultivationAPI.newIdentitySection(
        "MyMod:classes", Message.translation("server.mymod.identity.classesTab"), 1000,
        context -> {
            context.getCommandBuilder().append(
                    CultivationAPI.document(context.getPalette(), "Pages/MyMod/ClassCard.ui"),
                    context.selector("#ClassList"));
            context.bindAction(CustomUIEventBindingType.Activating, "#SelectButton", "select");
        },
        (context, action, value) -> {
            if("select".equals(action)) {
                ClassManager.applySelection(context.getStore(), context.getRef(), context.getPlayerRef());
            }
        }));
```

`newIdentitySection` builds one from a build callback and an optional action
handler without implementing `IdentitySection` directly; implement the
interface yourself if you need `isVisible` or a non-default `getSortOrder`
too. `build` runs exactly once per page open — never on every tab switch,
which only flips `.Visible`/`.Disabled` — so register every event binding
inside `build`, not lazily on first click.

### Addressing your own widgets

`IdentitySectionContext.getContainerSelector()` is this section's own indexed
slot in the page's `#RegisteredSectionList` — append into it exactly as
`RaceCard.ui` is appended into `#RaceList`, and address anything inside
through `context.selector(childSelector)` rather than a bare id. UI ids are
global to the loaded page, and another registered section's content sits in
the same document.

`context.bindAction(type, childSelector, action)` and
`context.bindValueAction(childSelector, action)` route a click or a pushed
value back to `handleAction` (or the `ActionHandler` passed to
`newIdentitySection`), keyed by this section's own id so the host page
dispatches to the right section without a per-section codec field. Use
`bindAction` for a plain click; `bindValueAction` for a dropdown or text field
whose current value should arrive with it.

### Threading, and a section that throws

`build`, `handleAction` and `isVisible` all run on the viewing player's own
world thread — read that player's components freely, never write to the Store
from inside them. A section that throws is caught and logged by the host page
rather than taking the whole Identity page down for every player, the same
guard `CultivationNav` puts around an addon's own nav page.

Namespace `getId()` with your mod's name, the same convention every other id
in this API follows — it must not equal `"race"` or `"titles"`, the two ids
the built-in tabs have always used.
