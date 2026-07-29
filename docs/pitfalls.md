# Pitfalls

The mistakes that cost the most time, roughly in order of how badly they bite.

---

## 1. Writing to the `Store` from inside a system or listener

**Symptom:** `IllegalStateException: Store is currently processing!`, and the
world thread dies.

Every callback this API makes hands you a `ComponentAccessor<EntityStore>`, not a
`Store`. That is deliberate — the accessor may be a `CommandBuffer`, and a
`CommandBuffer` is what you get while a system is mid-tick.

```java
// WRONG - takes the world down
ref.getStore().putComponent(ref, MyComponent.getComponentType(), new MyComponent());

// RIGHT - goes through whatever accessor you were handed
accessor.putComponent(ref, MyComponent.getComponentType(), new MyComponent());
```

Reads (`accessor.getComponent(...)`) are safe through either.

One consequence worth knowing: a `CommandBuffer` **cannot see its own pending
writes**. If you create a component and then read it back in the same tick, you
get `null`. Resolve a lazily-created component once and hold the reference for
the rest of that tick.

## 2. Shading Cultivation into your jar

**Symptom:** none. No exception, no log line. Your listeners never fire, your race
never appears, your menu page is not on the bar.

Two copies of `plugin.siren.Cultivation` on the classpath each have their own
static plugin instance and their own static registries. You register into yours;
the server reads the other one.

The dependency must be `<scope>provided</scope>`, and must not be pulled into a
shade/assembly plugin's output.

## 3. Forgetting `refreshProgression` in a `ProgressionProvider`

**Symptom:** the HUD, `/cultivation`, the rankings and every realm gate show a
player's *previous* standing, and only catch up on their next meditation tick.

Cultivation cannot detect a write inside somebody else's component. Whenever you
change a player's level, tell it:

```java
CultivationAPI.refreshProgression(accessor, ref);
```

Call it on progress changes too if you want a live progress bar, and from your
own player-join handling so a player whose component loads after Cultivation's
join hook is not gated on last session's numbers.

## 4. Trying to override Cultivation's lang keys

**Symptom:** `'server.cultivation.x' has multiple definitions` in the log, and
your wording never appears.

Language files from every asset pack merge into a single catalog, and the merge is
**first-writer-wins**. Cultivation's pack loads before any addon that depends on
it, so its key always wins. Even winning the race once would only mean winning it
on that particular boot.

Implement [`CultivationTheme`](theming.md) instead. It keeps your strings under
your own keys — which collide with nothing — and maps Cultivation's keys onto
them at render time.

## 5. Colliding on an id

**Symptom:** another mod's race/technique/menu page/codex article silently
disappears, or yours does.

Registering an existing id **replaces** the previous holder — by design, so a mod
can deliberately take over a built-in entry. That same behavior makes an
accidental collision silent.

Namespace everything with your mod's name:

```java
CultivationAPI.registerTechnique("MyAddon:flame_step", ...);
CultivationAPI.registerMenuPage(CultivationMenuPage.builder("myAddon:alchemy")...);
CultivationAPI.newAdminConfigField("MyAddon:BaseXp", ...);
```

Cultivation's own menu ids are bare words (`overview`, `settings`, `race`,
`skilltree`, `bonuses`, `dao`, `keybinds`, `rankings`, `codex`, `admin`), which
is exactly how you target one on purpose.

## 6. Capturing a config object instead of a supplier

**Symptom:** admin edits or a `/reload` appear to work, then have no effect.

A config reload replaces the instance behind the holder. Capture the *object* and
you are reading — and writing — a discarded copy.

```java
// WRONG
MyConfig config = this.myConfig.get();
CultivationAPI.newAdminConfigField("MyAddon:Rate", label,
        () -> config.getRate(), v -> config.setRate((float) v));

// RIGHT - read through the holder every time
CultivationAPI.newAdminConfigField("MyAddon:Rate", label,
        () -> this.myConfig.get().getRate(),
        v -> this.myConfig.get().setRate((float) v));
```

The same applies to `registerRace`'s `Supplier<RaceConfig>` — that is a supplier
precisely so a server owner's edits take effect without a restart.

## 7. Assuming `player()` is non-null

Most event payloads expose `@Nullable PlayerRef player()`. It is null when the
`PlayerRef` component was unavailable at fire time. Check it:

```java
CultivationEvents.onBreakthrough(event -> {
    PlayerRef player = event.player();
    if (player == null) {
        return;
    }
    // ...
});
```

`ref()` is always non-null; `player()` is the one to guard.

## 8. Blocking, or touching another world, inside a listener

Listeners run **synchronously on the world thread of the player the event
happened to**. Blocking there stalls that world for every player on it.

- No sleeps, no `.get()` on a future, no file I/O, no network calls.
- To touch an entity on another world, hop first:
  `CompletableFuture.runAsync(task, otherWorld)` — or `world.execute(...)`.
- Re-check `ref.isValid()` after any hop. The entity may be gone by then.

A listener that throws is caught, logged and skipped, so one broken addon cannot
break the mod or other addons. Do not lean on that — it hides your bug.

## 9. Reading `getStage()` under a `ProgressionProvider`

`CultivationAPI.getStage(...)` returns `null` whenever a provider is installed —
a replacement progression has no sub-stages. Use `getGlobalLevel(...)` for
anything numeric, and `getRealm(...)` for gating, since both are provider-aware.

`getRealm()` also returns the provider's *equivalent* realm rather than a stored
one, which is exactly what you want for a gate.

## 10. Percent-vs-fraction in admin config rows

The admin row widget shows two decimal places. A raw `0.004` chance renders as
`0.00`, and an admin cannot edit what they cannot see.

Scale in both directions when a value is stored as a fraction but read as a
percent:

```java
CultivationAPI.newAdminConfigField("MyAddon:DropChance", label,
        () -> config.get().getChance() * 100f,
        v -> config.get().setChance((float) (v / 100f)));
```

## 11. `TextField` needs an explicit initial `.Value`

If you build your own UI page, a `TextField` renders but **will not accept
typing** unless `.Value` is explicitly set on build and on every refresh — even
to `""`. This is an engine quirk, not a Cultivation one, and it costs an
afternoon every time.

## 12. Pushing a `Message` at a String UI property

**Symptom:** the player is **disconnected mid-session** the moment the page
renders. Not a log line, not a blank label — a disconnect.

Some client-side markup properties take a `String` and some take a rendered
message, and handing one the other is fatal rather than quiet:

| Property | Takes | Notes |
| --- | --- | --- |
| `.TextSpans` | `Message` | The localizable path. Use this for anything a player reads. |
| `.Text` | `String` | Tolerates a bare translation `Message`, and nothing more complex. |
| `.TooltipText` | `String` **only** | A `Message` here disconnects. |

This is why [`withTooltip`](ui.md#tooltips) takes a plain `String` — a tooltip
genuinely cannot be localized. Put anything that must be readable in every
language in the field's *label*, which goes through `TextSpans`.

Related, and just as fatal: a **dangling token** in a `.ui` file fails the whole
UI load, not just that element, so one typo takes out every mod page on the
client.

## 13. UI element ids resolve globally

**Symptom:** `CustomUI Set command selector doesn't match a markup property` for a
selector that is obviously correct — and only for *some* players, or only on some
pages.

Ids in `.ui` documents are **not scoped to their document**. If `#Here` is a
`Label` on your page and also a `Group` inside a row template that page renders,
a `#Here.TextSpans` write resolves against whichever one the client found first
and fails against the wrong element type.

Prefix ids per document (`#SenseFocus`, not `#Focus`) whenever a page and its row
template can be on screen together. The condition-dependence is the tell: a bug
that only appears once enough rows exist to render is almost always this.

## 14. Stale resources in the built jar

If you edit anything under `src/main/resources` (assets, `server.lang`,
`manifest.json`), run `mvn clean install` rather than `mvn install`. Maven will
otherwise keep the previously-packaged copy, and you will debug a fix that never
shipped.
