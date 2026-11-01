# Sect Array Domains

*New in 0.11.1.*

A sect under siege can raise an *Array Domain* (宗门大阵, the grand sect
array) over its whole territory: a long-lasting defensive aura that strengthens
the defenders and hinders the attackers for as long as the siege lasts. It is the
sect-scale counterpart of a cultivator's [Dao Domain](dao-domains.md), and the two
contest each other when they meet.

Nothing is placed in the world. The array is entity effects, border particles and
sounds over ground the sect already holds.

```java
SectArrayEvents.onPreSectArrayRaise(event -> {
    if (holdsWardingRelic(event.sect())) {
        event.setCost(event.cost() / 2);      // a discount; the raiser must still hold it
    }
});

SectArrayEvents.onSectArrayEnded(event ->
        LOGGER.atInfo().log("%s's array came down (%s) after %.0f s.",
                event.sect().getName(), event.reason(), event.secondsActive()));
```

The full payloads are in
[the event reference](events-reference.md#sect-array-domains-0111), and
`ExampleAddon`'s `CultivationHooks.registerSectArrays()` is a compiling version of
the above.

---

## How a sect raises one

- *Wartime only.* It can be raised once a siege has been declared against the
  sect (any live phase of it), and not before.
- *Who:* the Patriarch or an Elder, with `/sect array raise`. It is paid from
  *their own* sect contribution (`Sect-Array-Cost-Contribution`, 100) — the same
  currency war declarations and guardian posts cost — not from the sect.
- *Where:* every chunk the sect has claimed — its hall and its buildings.
- *How long:* until that siege ends, however it ends. It also ends when the sect
  loses its hall or is disbanded, when the Patriarch or an Elder lowers it with
  `/sect array lower` (no refund), when an admin lowers it, or when the feature is
  switched off.
- *Element:* the attackers' effect follows the sect's Dao element (its Spiritual
  Root Type); a sect with none uses Heavenly Qi.
- It survives a server restart mid-siege: the array's state is saved on the sect.

Admins can raise or lower one outside wartime and free of charge with
`/cultivation admin array raise <sect>` / `lower <sect>` (in game, not from the
console) — handy for testing an addon against it. A test array treats only the
admin who raised it as an intruder, adds nothing to any siege slow, lapses after
`Sect-Array-Admin-Test-Minutes` (30 by default), and does not survive a restart.
If the siege is called off because wars are switched off, the raiser gets the
contribution back; every other end keeps it. (No command cancels a single siege,
admin or otherwise.)

## What it does

The array touches players only, whatever the world's PvP setting — the siege is
the consent.

| Side | Who | Effect (defaults) |
| --- | --- | --- |
| Defenders | The sect's members, its allied sects, and the siege's reinforcements | Take 15% less damage (`Cultivation_SectArray_Defender`) |
| Attackers | The besieging sect's members (the war records reinforcements on the defending side only) | Slowed and deal 10% less damage; a Metal, Water, Fire or Ice array adds one extra (weaker armor, a deeper slow, weaker elemental resistance, no sprinting) (`Cultivation_SectArray_Attacker_<Element>`, or `_Heavenly`) |

The figures live in those entity effects. Both are re-applied every second and
last a second and a half, so "has `Cultivation_SectArray_Defender`" is a usable
test from your own system for "is defended by an array right now".

- The attackers' slow is added to the siege's shared slow budget
  (`Sect-Array-Siege-Slow-Percent`, 15) beside the Warding formation and the sect
  guardians, under the same `War-Total-Slow-Max-Percent` ceiling.
- A Siege Banner mutes the array within its suppression radius.
- A hostile Dao Domain unfolded inside the territory *clashes* with the array.
  The array fights at the higher of `Sect-Array-Base-Realm` (Nascent Soul) and the
  strongest defender standing inside, plus the Wu Xing counter bonus when the
  sect's element overcomes the caster's; the caster gets the same bonus when theirs
  overcomes the sect's. A domain that wins mutes the array inside
  its own cylinder until the domain ends; a domain that loses shatters with the
  full backlash; a tie shatters the domain without backlash and the array holds.
  That contest is reported by `DaoDomainEvents.DomainClashEvent`, with `second()`
  null and `arraySect()` naming the sect — see
  [Domain clashes](dao-domains.md#domain-clashes).

## The events

| Event | When |
| --- | --- |
| `PreSectArrayRaiseEvent` | A Patriarch or Elder is about to raise the array: every gate has passed, nothing is spent. Cancellable; `setCost` re-tunes the price |
| `SectArrayRaisedEvent` | The array stands. `raiser()` is `null` and `adminRaised()` `true` for an admin raise; `cost()` is what was actually charged |
| `SectArrayEndedEvent` | It came down, with an `EndReason`: `SIEGE_ENDED`, `HALL_LOST`, `LOWERED`, `ADMIN`, `DISABLED` |

- *Cancelling a raise* fails it with a generic "something prevents that" line —
  Cultivation tells the raiser, and nothing is charged. A cost a listener leaves is
  clamped to 0–1,000,000, and the raiser must still hold it or the raise fails as
  insufficient contribution.
- *An admin raise skips the pre-event* — it is only for the paid, in-game raise —
  but does fire `SectArrayRaisedEvent`.
- *`sect()` is the live `Sect` object*, as `SectEvents` hands it out: read what you
  need and return. For `HALL_LOST` its name may already be gone from the roster.
- *Never call back into raising or lowering an array from a listener* (the class
  javadoc's rule).
- *Threading:* a raise fires from the command that raised it; an end fires from the
  lowering command, or from whichever world's once-a-second keeper noticed that the
  siege or the hall was gone. That world need not be any member's, so treat these
  like `CelestialEvents`: do not block, and resolve each player and hop to their
  world before touching a component. `raiser()` is a `UUID` because the raiser may
  be offline by then.

## Ids

| What | Id |
| --- | --- |
| Codex article | `cultivation:sectArray` |
| Admin config section | `Cultivation:SectArray` |
| Effects | `Cultivation_SectArray_Defender`, `Cultivation_SectArray_Attacker_<Element>` (`Metal` … `Void`, `Heavenly`) |
| Config keys | `Sect-Array-Enabled`, `Sect-Array-Cost-Contribution`, `Sect-Array-Base-Realm`, `Sect-Array-Siege-Slow-Percent`, `Sect-Array-Defender-Damage-Reduction-Percent` (display only — the effect asset holds the real figure), `Sect-Array-Border-Seconds`, `Sect-Array-Border-Max-Posts`, `Sect-Array-Admin-Test-Minutes` |

The config file (`SectArrayConfig`) has no `CultivationConfigs` accessor. To change
the price for one raise, use `PreSectArrayRaiseEvent` rather than writing the
config.
