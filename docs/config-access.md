# Reading and writing Cultivation's settings

`CultivationConfigs` is the read/write half of the integration surface that
[events](events.md) are the react half of. An addon that wants to **know** a
number reads it here; one that wants to **change** a number permanently writes it
here and saves; one that wants to change it **only for one player, or only this
once**, uses the matching `Pre*` event instead.

```java
import plugin.siren.API.CultivationConfigs;

float regen = CultivationConfigs.spiritVein().get().getSpiritVeinRegenPerSecond();
boolean sects = CultivationConfigs.sect().get().isSectsEnabled();
```

## Which of the three you want

| You want to… | Use |
| --- | --- |
| Read a server's tuning so your own numbers sit sensibly beside it | `CultivationConfigs.x().get()` |
| Change a setting **for this server, permanently** | `CultivationConfigs.x()` + `save()` |
| Change a value **for one player, or one event** | The matching `Pre*` [event](events.md) |
| Expose your **own** settings in Cultivation's admin menu | [`registerAdminConfigSection`](ui.md#admin-config-sections) |

Reaching for a config write when a pre-event would do is the common mistake: a
config write changes the server for everyone, forever, and a server owner who
tuned that number will find your mod has overwritten it.

## Read through the holder, never around it

Every accessor returns the live `Config<T>` **holder**, not the config object.
Hold the holder — a config reload (an admin pressing Save, or the file being
edited on disk) replaces the instance behind it, and a captured instance is then
a discarded copy whose edits go nowhere.

```java
// Right — resolved at the point of use.
if (CultivationConfigs.dao().get().isDaoEnabled()) { … }

// Wrong — a reload silently detaches this.
private final DaoConfig dao = CultivationConfigs.dao().get();
```

Same rule as the `Supplier<RaceConfig>` in [`registerRace`](registries.md), and
the same reason.

## Writing

```java
var holder = CultivationConfigs.spiritVein();
holder.get().setSpiritVeinRegenPerSecond(2.5f);
holder.get().setSpiritVeinRichChance(5f);
holder.save();
```

Persisting is the caller's job, so a batch of edits costs one file write rather
than one per setter. A value changed **without** saving is live for this session
and lost on restart — occasionally exactly what you want, for a seasonal event or
a temporary difficulty swing.

`CultivationConfigs.saveAll()` writes every file. Rarely what you want — prefer
saving the one holder you edited — but useful after a bulk rewrite.

## The files

| Group | Accessor | Covers |
| --- | --- | --- |
| **Progression** | `cultivation()` | The Qi curve and per-level health/damage bonuses |
| | `spiritCores()` | Core drop rates and their Qi values |
| | `spiritVein()` | Per-chunk Qi pools, tiers, regen, drain |
| | `breakthrough()` | What a rank-up costs and pays out |
| | `skillTree()` | Node costs and effects |
| | `raceSystem()` | The race system's own switches |
| | `race(PlayerRace)` | One race's stat block — including a race another mod registered |
| **Arts** | `dao()` `technique()` `manual()` `alchemy()` `refinement()` `lifeBound()` `beast()` | What a cultivator practices, crafts, tempers and binds |
| **Society** | `sect()` `formation()` `dwelling()` `war()` `duel()` `partner()` | Sects, the ground they hold, the homes they build, the fights they pick |
| **Compatibility** | `endlessLeveling()` | How Cultivation shares the stats it raises — see [Compatibility](compatibility.md) |

Note `race(PlayerRace)` returns the `RaceConfig` directly rather than a holder,
because a race's stat block is reached through the race, not through a file of
its own.

## Threading

Reads are safe from anywhere. Writes are safe from `setup()` and from any world
thread, on the same terms Cultivation's own admin menu writes them: the config
objects are plain mutable holders with no cross-thread coordination, so two
threads racing to write the same field is last-writer-wins rather than corrupt.

## Gating on a subsystem being on

Most subsystems have a master switch, and an addon that extends one should check
it rather than registering into a system the server owner turned off:

```java
if (!CultivationConfigs.beast().get().isBeastsEnabled()) {
    return;  // don't add a beast type to a server that has beasts disabled
}
```

Do this at the point of use, not in `setup()` — the switch is editable live from
`/cultivation admin`, so a value read once at boot goes stale.
