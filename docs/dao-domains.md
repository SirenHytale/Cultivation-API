# Dao Domains

*New in 0.11.1.* Needs Hytale Update 7 (Server 0.7.2): a domain is cast from
one of the core ability slots Update 7 added.

A Dao Domain (道域) is an aura a Nascent Soul cultivator unfolds around
themselves. Enemies inside are slowed and suffer one effect drawn from the
caster's Dao element; allies inside deal more damage and gain one element effect.
The domain never deals damage itself. It is *anchored*: it stays where it was
unfolded, and it collapses the moment its caster steps outside it.

For an addon the surface is one event class, `DaoDomainEvents`, plus the ids
below. There is no `CultivationAPI` method that unfolds or ends a domain — a
domain is a cast, and a cast belongs to the player.

```java
DaoDomainEvents.onPreDomainUnfold(event -> {
    if (isPeaceZone(event.caster())) {
        event.setCancelled(true);              // refused: nothing charged, no cooldown
    }
});

DaoDomainEvents.onDomainUnfold(event ->
        LOGGER.atInfo().log("%s unfolded a tier %d domain (%s Qi).",
                event.caster(), event.tier() + 1, event.qiSpent()));   // tier() is 0-based

DaoDomainEvents.onDomainCollapse(event -> {
    if (event.reason() == DaoDomainEvents.CollapseReason.LEFT_AREA) {
        // they stepped out of their own domain
    }
});
```

The full payloads are in [the event reference](events-reference.md#dao-domains-0111).
`ExampleAddon`'s `CultivationHooks.registerDaoDomains()` is a compiling version of
the above.

---

## How a player gets one

- At the realm floor (`Domain-Min-Realm`, Nascent Soul by default) the player is
  handed one `Cultivation_DaoDomain_Heart` rune — on the breakthrough into that
  realm, or on their next join if they already stand above it and hold none.
  `/cultivation domain claim` re-grants a lost one. Nothing is stored per player:
  "holds none" is the whole rule.
- They bind it like a Dao Seal — at the Dao Seal Altar or with
  `/cultivation seals` — into one of the two core slots, and cast it with a
  weapon in hand (the engine's rule for every core rune).
- `/cultivation domain` prints the domain they would unfold right now, and why a
  cast would be refused.

## The shape of a domain

| | |
| --- | --- |
| Area | A cylinder centered on the caster's feet at cast time: the tier's radius, and half that radius above and below the anchor |
| Tier | A zero-based index: `0` at the realm floor, `1` one realm above it, `2` two or more above. Players see these as tiers 1, 2 and 3 |
| Defaults by tier | Radius 6 / 8 / 10 blocks, 10 / 13 / 16 seconds, 120 / 150 / 180 Qi; 75 s cooldown on the rune |
| Flavor | The caster's Dao element — the five classic and five shadow elements each have one. `element()` is `null` for a domain cast in *Heavenly Qi*: no Dao, or an element with no flavor of its own (an [off-ring root](registries.md#off-ring-dao-roots-0100) such as the Rubber Duck) |
| Enemies | Slowed, plus the flavor's enemy effect. A player counts as an enemy only where PvP would let the caster hit them (world PvP, their own duel opponent, the opposed Path War side) |
| Allies | +10% outgoing damage, plus the flavor's ally effect. The caster, their party, their sect, allied sects, their own and their allies' spirit beasts, their sect's guardian posts (`Domain-Ally-Scope`) |
| Seals | A [Dao Seal](dao-seals.md) reaction the caster sets off on a target inside their own domain bursts harder (`Domain-Seal-Reaction-Bonus-Percent`, 20) |

Vanilla Duration and area support runes bound beside the Heart stretch the domain
the same way they stretch any core rune.

## The events

| Event | When |
| --- | --- |
| `PreDomainUnfoldEvent` | Every cast gate has passed and no Qi has been spent. Cancellable; radius, duration and Qi cost are re-tunable |
| `DomainUnfoldEvent` | The domain stands. `qiSpent()` is what was actually taken |
| `DomainCollapseEvent` | It ended, with a `CollapseReason` |
| `DomainClashEvent` | Two hostile domains, or a domain and a hostile sect array, overlapped and the clash was settled — see [Domain clashes](#domain-clashes) |

### Cancelling and re-tuning an unfold

A cancel fails the cast exactly like one of Cultivation's own refusals: no Qi,
no cooldown, no domain. Cultivation tells the caster the cast was refused, so —
unlike `PreRitualAbortEvent` — your listener does not have to send a message of
its own.

What a listener leaves in the setters is checked before use:

- the radius is clamped to 1–32 blocks and the duration to 1–25 seconds;
- a negative Qi cost becomes 0;
- a NaN or infinite value is ignored, and the value Cultivation computed stands;
- an admin in bypass mode or a Creative-mode caster pays nothing, whatever cost a
  listener sets.

`tier()` and `element()` are read-only: they describe the caster, not the cast.

### Why a domain collapses

| `CollapseReason` | |
| --- | --- |
| `EXPIRED` | The duration ran out |
| `LEFT_AREA` | The caster stepped outside it |
| `DIED` | The caster died or became a fleeing Nascent Soul. Cultivation sends them no line; the death screen speaks for them |
| `LEFT_WORLD` | The caster is in a different world from the anchor. No collapse burst plays |
| `GATE` | A refusal gate tripped mid-domain: meditating, mid-breakthrough or mid-advancement, a Closed-Door Retreat, a Dream Trial, an Inner Demon duel or Cleanse Rite, the Sea of Consciousness, the Heavenly Realm sanctuary |
| `DISABLED` | An admin switched Dao Domains, or techniques, off |
| `CLASHED` | The domain lost a [clash](#domain-clashes), or tied one. The `DomainClashEvent` for that clash has already fired |

A domain whose caster *disconnects* fires no collapse event at all. The entity that
held it is gone and nothing is left ticking to notice; its effects lapse on their
own within a second. If your addon tracks open domains, also clear them on the
caster's disconnect.

### Threading

All four fire on the casters' own world thread, from inside Cultivation's cast
interaction and the domain system's half-second tick, so a `CommandBuffer` is in
play: *never write to the `Store` from these listeners* ([pitfall 1](pitfalls.md#1-writing-to-the-store-from-inside-a-system-or-listener)).

The payloads carry the caster's `UUID`, not a `Ref`, matching `SoulEscapeEvents`
and `DuelEvents`. Resolve them with `Universe.get().getPlayer(uuid)` and check
`isValid()` before reading anything.

---

## Domain clashes

*0.11.1.* Two *hostile* domains whose cylinders overlap contest each other.
Hostile means the two casters count each other as enemies — world PvP, a duel,
an opposed Path War side — or their sects are in a live siege against each other
and at least one of the two domains stands on the defending sect's claimed ground
(a siege is consent only on the contested land). Allied and neutral domains overlap
freely. Each pair resolves once.

- *Clash power* is the caster's realm ordinal (Body Refinement 0 up to Void
  Refinement 6), plus `Domain-Clash-Counter-Bonus` (1) when the domain's element
  overcomes the opponent's on the Wu Xing cycles (`DaoElement.counters`) — the
  same cycles the [Dao Seals](dao-seals.md) react on, where the classic and
  shadow rings are separate and a cross-ring pairing is neutral. A Heavenly Qi
  domain has no element: it never counters and is never countered.
- *The higher power wins.* The loser's domain shatters, and the loser is slowed
  for `Domain-Clash-Backlash-Seconds` (4) and loses
  `Domain-Clash-Backlash-Qi-Percent` (25) of the Qi they paid for that domain. A
  tie shatters both, with no backlash. Either way the two casters are pushed
  apart (`Domain-Clash-Push-Strength`), unless the siege alone made them hostile.
- With `Domain-Clash-Enabled` off, the older suppression rule applies instead:
  inside a hostile domain whose caster stands in a strictly higher realm, your own
  domain does nothing and you cannot unfold a new one
  (`Domain-Suppression-Enabled`).

Two things reach a listener, in this order:

```java
DaoDomainEvents.onDomainClash(event -> {
    // first() is the caster whose pulse found the overlap - usually whoever unfolded last.
    // second() is null when the opponent was a sect array; arraySect() names that sect.
    if (event.outcome() == DaoDomainEvents.Outcome.TIE) { /* both shattered */ }
    if (event.counterDecided()) { /* a Wu Xing counter changed the result */ }
});

DaoDomainEvents.onDomainCollapse(event -> {
    if (event.reason() == DaoDomainEvents.CollapseReason.CLASHED) {
        // lost or tied a clash; fired AFTER the DomainClashEvent for the same clash
    }
});
```

| `DomainClashEvent` | |
| --- | --- |
| `first()` | The caster whose pulse settled the clash |
| `second()` | The other caster, or `null` when the opponent was a sect array. It can also be a synthetic id that matches no player, when an admin summoned a test rival — always resolve it and handle "no such player" |
| `arraySect()` | The defending sect's name when the opponent was a sect array, else `null` |
| `firstPower()` / `secondPower()` | Clash power, counter bonus included |
| `outcome()` | `Outcome.FIRST_WON`, `SECOND_WON` or `TIE`, from `first()`'s point of view |
| `counterDecided()` | True when removing the counter bonuses would have changed the outcome |

A domain unfolded inside a sect's wartime [Array Domain](sect-array-domains.md)
clashes with the array (`Domain-Clash-Arrays-Enabled`). The array's power is the
higher of its configured base and the strongest defender standing in its
territory, plus the counter bonus if the sect's element overcomes the domain's;
the domain gets the same bonus when its element overcomes the sect's.
A domain that wins mutes the array inside its own cylinder until the domain
ends; a domain that loses shatters with the full backlash; a tie shatters the
domain without backlash and the array holds.

---

## Ids

| What | Id |
| --- | --- |
| Progressive Reveal system | `cultivation:daoDomains` — `CultivationAPI.isSystemRevealed(accessor, ref, "cultivation:daoDomains")` |
| Codex article | `cultivation:daoDomains` |
| Admin config section | `Cultivation:DaoDomains` |
| Config file | `Arts/DaoDomainConfig` — no `CultivationConfigs` accessor; see [Config access](config-access.md#files-with-no-accessor-yet) |
| Rune item | `Cultivation_DaoDomain_Heart` (no recipe, not dropped on death, refused by the Auction House) |
| Cast root interaction | `Root_Cultivation_Domain_Unfold` (interaction type `CultivationDomainUnfold`) |
| Ability keyword | `Cultivation_DaoDomain` |
| Effects | `Cultivation_Domain_<Flavor>_Enemy`, `_Ally`, `_Self` for each of `Metal`, `Wood`, `Water`, `Fire`, `Earth`, `Ice`, `Wind`, `Poison`, `Lightning`, `Void`, `Heavenly`; plus `Cultivation_Domain_SwordIntent_Enemy` / `_Self` |

The enemy and ally effects are re-applied every half second and last one second,
so "does this entity have `Cultivation_Domain_Fire_Enemy`" is a usable test, from
your own system, for "was this entity caught by a hostile Fire domain within the
last second".

## Config keys

All in `Arts/DaoDomainConfig`:

`Dao-Domains-Enabled`, `Domain-Min-Realm`, `Domain-Qi-Cost`,
`Domain-Qi-Cost-Per-Tier`, `Domain-Radius`, `Domain-Radius-Per-Tier`,
`Domain-Duration-Seconds`, `Domain-Duration-Per-Tier-Seconds`,
`Domain-Max-Targets`, `Domain-Affects-Enemy-Players`, `Domain-Ally-Scope`,
`Domain-Suppression-Enabled`, `Domain-Seal-Reaction-Bonus-Percent`,
`Domain-Sword-Intent-Enabled`, `Domain-Grant-On-Join`, and for clashes
`Domain-Clash-Enabled`, `Domain-Clash-Counter-Bonus`,
`Domain-Clash-Backlash-Seconds`, `Domain-Clash-Backlash-Qi-Percent`,
`Domain-Clash-Push-Strength`, `Domain-Clash-Arrays-Enabled`.

To change a domain for one cast, use `PreDomainUnfoldEvent` rather than writing
the config: a config write changes the server for everyone, permanently.
