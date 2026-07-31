# Palettes

**New in 0.7.0.** A palette is one *look* a player can wear — the colors every
Cultivation menu, the HUD and the skill tree are drawn in. Cultivation's own is
the crimson and gold every player starts on; a mod may add as many more as it
likes, and a player picks between them.

```java
CultivationAPI.registerPalette(
        CultivationPalette.builder("myAddon:frost")
                .name("server.myaddon.palette.frost")
                .swatch(0x9FD8F0)
                .documentRoot("Pages/MyAddon_Frost/")
                .documents(DOCUMENTS)
                .halo(SkillTreeBranch.VITALITY, 0x7FB8D8)
                // ... the other eight
                .build());
```

Registered from `setup()` like every other registry, in any load order — nothing
reads it until a player opens a menu.

> **This is not `CultivationTheme`.** The names invite exactly the wrong guess,
> and the two facilities are unrelated.

| | Changes | Guide |
| --- | --- | --- |
| `CultivationPalette` | **Colors.** The documents menus are drawn from, plus the nine skill-tree halo hues. | This page |
| `CultivationTheme` | **Words.** Re-maps Cultivation's translation keys onto yours, so "sect" reads as "academy". | [Theming](theming.md) |

A theme changes nothing visual; a palette changes nothing a player reads. They
compose freely, and a mod that wants its own setting *and* its own colors
registers both — a palette's name is resolved fresh on every draw, so a theme can
re-word it.

---

## Why a palette is a set of documents, not a set of hex values

It would be tidier if a palette were twenty hex values pushed at the client, and
the first thing everyone tries to write. The engine does not allow it.

`PatchStyle` is the **only** style object Java may push at the client, and it
carries a flat background fill and nothing else. There is no `TextStyle` class in
the engine at all — so once a label has been drawn, its color cannot be changed
from the server. Roughly **half of what this mod's UI is made of is colored
text**: titles, stat values, realm names, Codex prose, every button caption.

So a palette works the way Cultivation already handles its locked / affordable /
owned skill nodes, its ordinary-versus-your-own ranking rows, and its two recipe
row shapes: **one static document per variant, chosen when the page is built.**
`resolveDocument` is that choice.

The consequence worth internalizing: a variant is the *same document* with its
colors substituted. Every layout number, every element id, every event binding is
identical across palettes, because a page's Java build code is written once and
does not know which variant it is filling in. Hand-authoring a variant is how
that stops being true — [generate them](#generating-the-variants).

## Halos are the exception

The nine skill-tree branch halos **are** plain hex, because they are bare `Group`
backgrounds that Cultivation already pushes at runtime through `PatchStyle` — the
one thing the engine does support. A palette carries them directly:

```java
.halo(SkillTreeBranch.VITALITY, 0x7FB8D8)
```

`getHalo(branch, fallback)` answers for any branch, and returns the fallback if
this palette does not recolor halos at all (`hasHalos()` tests that up front).

Supply the **full hue**, not a pre-dimmed one. The skill tree applies brightness
on top of whatever you return to encode unlock state — locked is dimmed toward
the backdrop, affordable is your hue exactly, owned is lightened toward white.

### All nine, or none

`build()` refuses a partial set:

```
Cultivation palette 'myAddon:frost' sets 4 of 9 branch halos. Set all nine or
none - a half-recolored tree reads as a bug, and the nine hues are what tell
branches apart.
```

The rule exists because **hue is what tells a player which branch a node belongs
to.** Nine branches radiate from one center, and a node's color is the only thing
on screen that says "this is Might, not Warding". They therefore have to stay
distinguishable *from each other* while still reading as one family — which is
the hard part of authoring a palette, and the reason it cannot be a hue rotation.

Recolor four and leave five crimson and you have not made a half-finished
palette, you have made a tree that looks broken. Grade the nine toward your
palette; never collapse them onto it.

---

## Registering one

The worked example below is [Cultivation: Jade Slip](https://www.mermaids.dev/cultivation/),
which does nothing privileged — it is the same public `registerPalette` any third
party makes.

```java
public final class MyAddonPalettes {

    private static final String LANG = "server.myaddon.";

    /** Every document these palettes ship a variant of, written by the generator. */
    private static final Set<String> DOCUMENTS = Set.of(
            "ActiveBonusesPage.ui",
            "BonusRow.ui",
            "CodexHeading.ui",
            // ... every one of Cultivation's 49, plus any of your own pages
            "CultivationHud.ui",
            "CultivationNavBar.ui",
            "CultivationStatsPage.ui",
            "CultivationTheme.ui",
            "SkillTreeHalo.ui",
            "SkillTreePage.ui");

    public static void register() {
        CultivationAPI.registerPalette(
                CultivationPalette.builder("myAddon:frost")
                        .name(LANG + "palette.frost")
                        .section(LANG + "palette.section.sects")
                        .swatch(0x9FD8F0)
                        .documentRoot("Pages/MyAddon_Frost/")
                        .documents(DOCUMENTS)
                        .halo(SkillTreeBranch.VITALITY, 0x7FB8D8)
                        .halo(SkillTreeBranch.RESILIENCE, 0x4FC4D8)
                        .halo(SkillTreeBranch.MIGHT, 0x8FA8D0)
                        .halo(SkillTreeBranch.WARDING, 0x5B8FD8)
                        .halo(SkillTreeBranch.INSIGHT, 0xBFD8EE)
                        .halo(SkillTreeBranch.HARMONY, 0x6FD8C8)
                        .halo(SkillTreeBranch.SWIFTNESS, 0x7FD4F0)
                        .halo(SkillTreeBranch.ENDURANCE, 0x9FC4B8)
                        .halo(SkillTreeBranch.SPIRIT, 0xA89FD8)
                        .build());

        // The total, not your own count: it is what a server owner can check
        // against the picker, and it catches a palette that failed to register.
        MyAddon.LOGGER.atInfo().log("%d total palettes in the picker.",
                CultivationAPI.getPalettes().size());
    }
}
```

Called from `setup()`, and handed back in `shutdown()`:

```java
@Override
protected void shutdown() {
    for (CultivationPalette palette : CultivationAPI.getPalettes()) {
        if (palette.getKey().startsWith("myAddon:")) {
            CultivationAPI.unregisterPalette(palette.getKey());
        }
    }
}
```

Unregistering is safe with players still wearing it. Nothing stores a palette
object — only its id — and an id nobody claims resolves back to the default. A
server unloading your mod simply returns its wearers to crimson and gold, and
they get their choice back if it is reinstalled.

### Builder options

| Method | |
| --- | --- |
| `name(String translationKey)` | The picker tile's text, localized. Without it the tile shows the raw key. |
| `section(String translationKey)` | Groups this palette under a captioned section in the picker |
| `swatch(int rgb)` | The color drawn on the picker tile, so the grid can be scanned by eye. Pick the one that most says "this look". Defaults to Cultivation's gold. |
| `documentRoot(String)` | The folder the variants live in, relative to `Common/UI/Custom/`. A trailing slash is added if you leave it off. |
| `documents(Set<String>)` | Bare file names, no folders. See below. |
| `halo(SkillTreeBranch, int rgb)` | One branch's halo color. All nine, or none. |
| `permission(String)` | Hides this palette from players without the node |
| `visible(Predicate<PlayerRef>)` | A gate a permission cannot express. Combines with `permission`; both must pass. |

`build()` refuses three things: an empty key, a partial halo set, and a
`documents` list with no `documentRoot` to find them under.

### Namespace the folder as well as the id

Every mod's `Common/UI/Custom/` merges into **one tree**. Two mods that both ship
`Pages/Cultivation/CultivationStatsPage.ui` collide, and the loser's document is
simply not there. Put your variants under a folder named for your mod —
`Pages/MyAddon_Frost/`, not `Pages/Frost/`.

The palette's id needs the same treatment, for the ordinary reason: registering
an existing id **replaces** its holder rather than erroring, so `"frost"` quietly
steals somebody else's look while `"myAddon:frost"` cannot.

---

## Resolving documents

`resolveDocument` is what a page build calls, usually through
`CultivationAPI.document`:

```java
@Override
public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder,
                  @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {

    // Looked up once, then passed down - a build that appends thirty rows should
    // not re-read the player's settings thirty times.
    CultivationPalette palette = CultivationAPI.getPalette(store, ref);
    commandBuilder.append(CultivationAPI.document(palette, DOCUMENT_PATH));

    String rowDocument = CultivationAPI.document(palette, ROW_PATH);
    for (Thing thing : things) {
        commandBuilder.append("#List", rowDocument);
        // ...
    }
}
```

There is a `document(store, ref, basePath)` overload that looks the palette up
for you, for a build that appends only one document.

### It falls back, and that is the whole point

A palette **only ever redirects a document it explicitly declared**. Anything
else returns `basePath` unchanged.

That is not caution for its own sake. A `.ui` path that does not resolve **fails
the entire UI load on the client** — not the one element, the whole page — and
**no validator in this workspace checks `append()` paths**. A typo, or a document
you forgot to emit, would reach players as a blank screen having passed every
static gate clean. So the declared list comes from the generator, which writes it
from the files it actually emitted, and a page Cultivation adds *after* your
palette was generated simply keeps its default look until you regenerate.

### Matching is by file name, so every variant lives in one folder

`resolveDocument` keeps the file name and discards the folder the base mod had it
in:

```java
// "CultivationStatsPage.ui" is in the declared set:
resolveDocument("Pages/Cultivation/CultivationStatsPage.ui")
        -> "Pages/MyAddon_Frost/CultivationStatsPage.ui"

// "SomethingNew.ui" is not:
resolveDocument("Pages/Cultivation/SomethingNew.ui")
        -> "Pages/Cultivation/SomethingNew.ui"
```

Two consequences:

- **Everything you declare must be under the one `documentRoot`.** Cultivation
  keeps its 48 page documents in `Pages/Cultivation/` and its HUD document in
  `Hud/Cultivation/`, but a palette has a single root — so a palette that themes
  the HUD too has to emit `CultivationHud.ui` into that same folder alongside the
  pages. Emitting it into a mirror of `Hud/` instead leaves the declared name
  pointing at a file that is not there, which is the whole-page failure above.
  Depth is what has to match, not the folder name: both source folders sit two
  levels under `Common/UI/Custom/`, so a `../../Common/ProgressBarFill.png`
  inside a document still resolves from either.
- **Your own pages can be themed the same way.** Declare their file names, emit
  variants beside the rest, and route their `append` calls through `document` —
  a palette picker that is itself stuck in crimson while the menu behind it is
  frost-blue reads as a bug rather than a choice.

### Route every append, including rows and fragments

The failure here is not an exception, it is a page that looks wrong: a themed
page that appends an unthemed row draws that row in the old colors, which is more
jarring than not theming at all. Every `commandBuilder.append(...)` goes through
`document`, not just the root one.

Two that are easy to miss:

- **Row and fragment templates.** Ranking rows, Codex blocks, admin config rows,
  skill-tree nodes and halos — all appended per item, all separate documents.
- **The nav bar.** See below; it has its own overload.

---

## The nav bar

If you [put Cultivation's shared nav bar on your own page](ui.md#putting-the-bar-on-your-own-page),
use the overload that takes `store` and `ref`:

```java
// RIGHT - draws the bar in the viewer's palette
CultivationAPI.buildMenuNav(commandBuilder, eventBuilder, playerRef,
        "myAddon:alchemy", store, ref);

// Back-compat only - always draws it in crimson and gold
CultivationAPI.buildMenuNav(commandBuilder, eventBuilder, playerRef, "myAddon:alchemy");
```

The four-argument overload cannot see which look the player is wearing, so on a
themed page it puts a strip of the old colors across the bottom of an otherwise
recolored menu. It is kept only so pages written before palettes existed still
compile.

---

## What the player is wearing

The choice lives on `CultivationSettingsComponent` under the `Palette` key, and
what is stored is the **id**, never the palette. Settings saved before palettes
existed decode to `null`, which is the default look — which is what those players
already had.

```java
// all static on CultivationAPI
@Nullable CultivationPalette getPalette(Store<EntityStore> store, Ref<EntityStore> ref)
@Nullable CultivationPalette getPalette(String paletteKey)
List<CultivationPalette>     getPalettes()          // registration order, a fresh list
```

`getPalette(store, ref)` returns **`null`** in three cases, all meaning "draw the
default": no settings component, no palette chosen, or a stored id that nobody
currently claims — the mod that provided it has been uninstalled.

Null rather than a default instance is deliberate. It lets every caller say "no
palette, carry on as before", and keeps a server with no palette mod installed on
exactly the code path it had before palettes existed.

> `CultivationPalette.DEFAULT_KEY` (`"cultivation:default"`) is a **reserved
> sentinel** meaning *no palette*, not a registered object. `getPalette(store,
> ref)` short-circuits on it. It is the id a picker offers for "put me back on
> Cultivation's own colors", and it should be stored as `null`. Do not register a
> palette under it: it would appear in `getPalettes()` and never be resolved for
> anybody.

## Who may wear it

```java
boolean isAvailableTo(PlayerRef playerRef)
```

True when the palette has no `permission` and no `visible` predicate, or when
both pass. It is called on the world thread that owns the viewing player, so it
may read that player's components but **must not write to the `Store`**.

Check it in **both** places — when drawing the picker card, and again when
handling the click:

```java
boolean isDefault = CultivationPalette.DEFAULT_KEY.equals(data.palette);
if (!isDefault) {
    CultivationPalette chosen = CultivationAPI.getPalette(data.palette);
    if (chosen == null || !chosen.isAvailableTo(playerRef)) {
        return;
    }
}

settings.setPaletteId(isDefault ? null : data.palette);
```

The card was hidden for a look this player may not wear, but a hand-crafted event
packet does not have to have come from a button.

One more thing a picker has to get right: **reopen the page, do not refresh it.**
Every document on screen belongs to the *old* palette, so an incremental update
leaves the frame in the previous colors with only the labels changed.

```java
CultivationAPI.openMenuPage(store, ref, new MyPickerUIPage(playerRef));
```

---

## Generating the variants

Forty-nine documents times five palettes is 245 files. They are generated, and
the generator is the part of a palette mod worth writing carefully. The shape
that works:

**Read the base mod's own resource tree**, not a copy of it. The variants carry a
copy of each page's *layout* as well as its colors, so a stale variant is how a
themed page silently drifts out of shape after Cultivation moves an element.
Regenerate on every Cultivation update.

**Substitute by color value, not by token.** Cultivation's theme file defines
color tokens, but its own mixins do not all read them — `@XianxiaContainer`
hardcodes `#A8792C(0.95)` rather than spreading the token. Rewriting the token
definitions alone changes almost nothing on screen. Another few dozen literals
live out in the page documents themselves (skill node faces, the center
medallion, Codex block plates, HUD grooves) where no shared mixin reaches them at
all. A pass that replaces every hex literal catches all three.

**Map values to roles, and author roles.** `#D9A63E` → `accent`, `#5C1712` →
`lacquer`, `#1D0D07` → `panel`. A palette is then authored by deciding "what is
my panel color", which is a design decision, rather than by hand-editing thirty
unrelated hex values and hoping they cohere.

**Report unmapped colors as a failure.** A literal the role map does not know
stays the old palette, and one crimson plate in a frost-blue page is the kind of
thing nobody notices until a player screenshots it.

**Emit the Java too.** The `Set<String>` a palette declares must be the list the
generator actually wrote — see [the fallback](#it-falls-back-and-that-is-the-whole-point)
for why hand-maintaining it is how a palette ends up naming a file that is not
there.

**Add a verify pass** that checks every generated folder holds every declared
document, and run it before shipping. Nothing else catches a missing variant: no
validator checks `append()` paths, and the failure surfaces on the client as a
blank screen rather than in a server log.

---

## Rules

- **Generate the documents; never hand-author a variant.** Divergence between
  variants is invisible until a player wears the odd one out.
- **All nine halos or none.** Enforced at `build()`.
- **Namespace the id *and* the folder.** Both namespaces are global.
- **Every declared document under one `documentRoot`.**
- **Route every `append` through `document`**, rows and fragments included.
- **Prefer the six-argument `buildMenuNav`.**
- **Look the palette up once per build**, then pass it down.
- **Re-check `isAvailableTo` in the handler**, not only when drawing the card.
- **Regenerate on every Cultivation update.** The variants carry a copy of the
  base mod's layout, and nothing tells you when it has moved on.
