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
| `overview` | Stats | 100 |
| `settings` | Settings | 200 |
| `race` | Race selection | 300 |
| `skilltree` | Skill tree | 400 |
| `bonuses` | Active bonuses | 500 |
| `dao` | Dao | 600 |
| `keybinds` | Technique keybinds | 650 |
| `rankings` | Rankings | 700 |
| `codex` | Codex | 800 |
| `admin` | Admin — gated on `cultivation.admin` | 9000 (`SORT_LAST`) |

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
   CultivationAPI.buildMenuNav(commandBuilder, eventBuilder, playerRef, "myAddon:alchemy");
   ```

   Passing your own id disables that button, marking where the player is.

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

### Rules

- **One section per config file** is the shape Cultivation uses for itself and the
  one that reads best.
- **Read through the holder**, as above — never capture the config object. A
  reload replaces the instance behind it and a captured one edits a discarded copy.
- **Return a stable field list.** The page matches an admin's in-flight edits to
  fields by key, so a list that changes shape between render and save silently
  drops those edits.
- **Field keys are global** across every section of every mod. Namespace them.
- **Only numbers.** The row widget is a number input. A boolean is best expressed
  as a 0/1 field with a label saying so; anything list-shaped belongs in the config
  file. Cultivation makes the same call for its own booleans.
- **Clamp in `set`**, not after. The page re-displays whatever `get` returns, so a
  coerced value is shown back to the admin rather than silently disagreeing with
  what they typed. Persisting is `save()`'s job.
