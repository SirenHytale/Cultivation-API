# Compatibility with other mods

Cultivation detects three other mods and changes its own behaviour when they are
present. Each is exposed as a single boolean, so an addon can ask the same
question without doing its own plugin lookup:

```java
boolean isEndlessLevelingInstalled()   // stats are handed to EL rather than applied here
boolean isPlaceholderApiRegistered()   // %cultivation_...% placeholders answer
boolean isMarriageInstalled()          // Partnered Cultivation is live
```

All three are cheap reads of a flag resolved once at boot — call them wherever you
need them rather than caching.

---

## Endless Leveling

Both mods raise max health and outgoing damage. Left alone, a player running both
gets Cultivation's multiplier *on top of* EL's, and the two compound into
something neither was balanced for.

Since 0.6.1, when EL is installed Cultivation **hands those two stats to EL**
rather than applying them itself. Cultivation's realm bonus becomes an EL bonus
from source `"Cultivation"`, so EL's own display, its diminishing returns and its
caps all apply to the combined number, and a player sees one coherent stat sheet
instead of two mods arguing.

The switches live in `EndlessLevelingConfig` — reachable as
[`CultivationConfigs.endlessLeveling()`](config-access.md) — and are inert on a
server without EL.

### What this means for your addon

If your addon applies stats of its own, **check the flag and put the bonus where
the server's stats live**:

```java
if (CultivationAPI.isEndlessLevelingInstalled()) {
    // EL owns the stat sheet on this server — register your bonus there.
} else {
    // apply it yourself, as before
}
```

An addon that keeps applying its own `EntityStatMap` modifier on an EL server is
not *broken*, but it is the third voice in a conversation the other two have
already settled.

### The handoff is self-correcting

Cultivation publishes its contribution to EL on every stat refresh, and publishes
**zero** when the handoff is switched off. So an admin toggling it mid-session
does not leave a stale bonus behind on either side.

---

## PlaceholderAPI

When PAPI is installed, Cultivation registers a `cultivation` expansion exposing
around 60 placeholders — realm, stage, level, Qi, progress percent, race, dao,
path, karma, sect, beast, abode, duel state, and relational placeholders for
comparing two players.

```
%cultivation_realm%     %cultivation_stage%      %cultivation_level%
%cultivation_qi%        %cultivation_qi_percent% %cultivation_qi_bar%
%cultivation_race%      %cultivation_dao%        %cultivation_path%
%cultivation_karma%     %cultivation_sect%       %cultivation_beast%
%cultivation_vein_qi%   %cultivation_vein_tier%  %cultivation_meditating%
```

Most have a `_key` / `_id` sibling — `%cultivation_realm%` is the localized
display name, `%cultivation_realm_key%` the translation key, and
`%cultivation_realm_id%` the stable enum name to compare against in a script.

The expansion `persist()`s, so a PAPI reload does not unregister it.

`isPlaceholderApiRegistered()` is true only when PAPI is installed **and** it
accepted the registration. Check it before registering an expansion of your own
under a colliding identifier, or before writing a chat format that assumes the
placeholders resolve.

---

## Marriage

Partnered Cultivation — married couples meditating together for a Qi bonus and
Yin-Yang convergence — is gated on the Marriage mod. Without it, nothing in
`CultivationConfigs.partner()` has any effect, and
`isMarriageInstalled()` returns false.

---

## Writing your own optional dependency on Cultivation

The same pattern Cultivation uses for the three above is the one to use for
depending on Cultivation itself when your mod also works without it. Declare it
under `OptionalDependencies` rather than `Dependencies`:

```json
"OptionalDependencies": {
  "Siren:Cultivation": ">=0.6.1"
}
```

…and keep every Cultivation class behind a guard that is only reached when the
plugin is actually present, so its classes are never resolved on a server without
it. [Getting started](getting-started.md#optional-dependencies) has the full
pattern; the short version is one boundary class that touches Cultivation types,
and a single boolean checked before anything calls into it.

A class that merely *mentions* a Cultivation type in a field or a method
signature gets resolved when the enclosing class loads, which is why the boundary
has to be its own class rather than an `if` inside an existing one.
