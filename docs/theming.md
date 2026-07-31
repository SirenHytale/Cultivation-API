# Theming

`CultivationTheme` re-words Cultivation's entire player-facing vocabulary without
touching its behavior — so a setting with soul masters and academies rather than
cultivators and sects reads as its own game instead of a reskin.

```java
CultivationAPI.setTheme(new MyTheme());
```

> **A theme changes words, not colors.** Despite the name, `CultivationTheme` is
> a pure text facility: it re-maps translation keys and touches nothing visual.
> The one that changes what the menus, HUD and skill tree *look* like is
> `CultivationPalette` — a separate registry, see [Palettes](palettes.md). The
> two compose, and a mod that wants its own setting *and* its own colors
> registers both.

Every string Cultivation shows a player — chat, commands, HUD, menus, item names,
tooltips — is routed through `translate` first, and whatever you return is used
verbatim. Pass `null` to restore Cultivation's own wording.

> **Only one theme can be live.** The last to register wins, and a warning naming
> both is logged.

## Why this is a hook and not a language file

Because the engine will not let you do it with one.

Language files from every asset pack are merged into a single catalog by
`I18nModule`, and that merge is **first-writer-wins**: a duplicate key from a later
pack is discarded and logged as `"'x' has multiple definitions"`. Cultivation's own
pack loads before any addon that depends on it, so an addon shipping
`server.cultivation.playerMsg.breakthrough` in its own `server.lang` is silently
ignored — and even winning the race would only mean winning it on that particular
boot.

So a theme keeps its strings under **its own keys**, which collide with nothing,
and maps Cultivation's keys onto them here.

## The interface

```java
public interface CultivationTheme {
    @Nonnull String getId();
    @Nullable Message translate(@Nonnull String key);
}
```

`getId()` is namespaced with your mod's name (`"SoulRings:soul_land"`) and used
only for logging and for `/cultivation admin` to report which theme is live.

`translate` receives the **full key** Cultivation was about to show, always
including its `server.` prefix. Return the message to show instead, or `null` to
leave that one alone — so a theme may cover as much or as little as it likes.

## Implementation

The contract is: `translate` is called on whatever thread is building the message,
and often **several times per rendered line**. It must be fast, thread-safe, and
free of side effects. A lookup in an immutable map is the intended shape:

```java
public final class MyTheme implements CultivationTheme {

    private static final Map<String, String> KEYS = Map.ofEntries(
            Map.entry("server.cultivation.playerMsg.breakthrough",
                      "server.myaddon.theme.rankUp"),
            Map.entry("server.cultivation.realm.qi_condensation",
                      "server.myaddon.theme.rank.apprentice"),
            Map.entry("server.cultivation.sect.title",
                      "server.myaddon.theme.academy")
            // ...
    );

    @Nonnull
    @Override
    public String getId() {
        return "MyAddon:my_setting";
    }

    @Nullable
    @Override
    public Message translate(@Nonnull String key) {
        String replacement = KEYS.get(key);
        return replacement == null ? null : Message.translation(replacement);
    }

    public int size() {
        return KEYS.size();
    }
}
```

## Two rules

**Return a translation, not raw text.** `Message.translation(yourKey)` wherever you
can — raw text is the same in every language, and Cultivation is translated.

**Keep the placeholders.** Any `.param(...)` placeholders Cultivation fills in are
applied to whatever you return, so a replacement must keep the same placeholder
names. Drop one and it simply renders empty.

## Finding the keys to re-word

Cultivation's keys live in its asset pack's `server.lang`. Every key you might
want starts with `server.cultivation.`. Grep the mod's language file for the
strings you want to change, and map those keys.

Start small — realm names, the sect vocabulary, the breakthrough messages — and
grow the map. A partial theme is perfectly valid; every unmapped key falls through
to Cultivation's own wording.

## Uninstall cleanly

```java
@Override
protected void shutdown() {
    CultivationAPI.setTheme(null);
}
```

Otherwise a server unloading only your mod is left with a theme whose lang keys
went with your asset pack, and every re-worded string renders as a raw key.
