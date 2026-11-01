# Dao Seals

*New in 0.11.1.* Needs Hytale Update 7 (Server 0.7.2): seals are rune items for
the ability slots Update 7 added.

Dao Seals (道印) are Cultivation's runes. A cultivator binds them into the
vanilla ability slots, casts them for Qi instead of the slot's own cost, and
uses them to *mark* a target with an element. Striking a marked target with the
element that overcomes the mark sets off a *reaction*: the mark is consumed, a
burst of damage lands, and the target suffers a short control effect.

*There are no seal events in this release.* That is deliberate — the reaction
is resolved inside Cultivation's damage pipeline, and no hook there was designed
yet. What an addon can do is read what seals leave behind: damage causes and
entity effects with stable ids, listed below, plus the usual Reveal, Codex,
admin and config ids. Treat that as the whole surface until an event class
ships; do not reach into `plugin.siren.Utils.DaoSeal`.

---

## How seals work

| | Classic ring | Shadow ring |
| --- | --- | --- |
| Elements | Metal, Wood, Water, Fire, Earth | Ice, Wind, Poison, Lightning, Void |
| Overcoming cycle | Metal > Wood > Earth > Water > Fire > Metal | Ice > Wind > Poison > Lightning > Void > Ice |
| Realm floor | `Seal-Min-Realm` (Qi Condensation) | `Shadow-Seal-Min-Realm` (Foundation Establishment) |

The two rings never react across each other. The cycles are the ones
[`DaoElement`](types.md#daoelement) already encodes.

- *Core seals* (`Cultivation_DaoSeal_<Element>`) go in a core slot. Each cast
  costs Qi (`Seal-Qi-Cost-<Element>`), is cheaper and hits harder for a
  cultivator whose own Dao matches it (resonance), deals its own seal damage
  cause and leaves its mark for 6 seconds.
- *Conversion seals* (`Cultivation_DaoSeal_Convert_<Element>`) are support runes.
  They turn a vanilla elemental rune's damage into that element's seal cause, so
  a vanilla rune can set off a reaction. A converted rune detonates marks; it
  never lays one.
- *What sets off a reaction:* a seal, a conversion-seal rune, or a Dao walker's
  melee blow or technique in the overcoming element, landed by a player on a
  target that carries the overcome element's mark. A mark is not tied to whoever
  laid it, so one player's seal and a party member's art combine. Against a
  player the burst is scaled by
  `Seal-Reaction-Pvp-Multiplier` (0.6), and only where Cultivation's own rules
  would let the attacker hit that player at all.
- Inside the attacker's own [Dao Domain](dao-domains.md), a reaction bursts
  harder (`Domain-Seal-Reaction-Bonus-Percent`).
- *Binding:* the Dao Seal Altar block (`Cultivation_DaoSealAltar`) or
  `/cultivation seals` opens the vanilla ability bench over the player's own
  slots, refused during combat by default (`Seal-Bench-Refuse-In-Combat`).
  Like every core rune, a seal is cast with a weapon in hand.

Seals keep no state of their own: the slots are vanilla inventory, and marks and
cooldowns are entity effects that clear on death.

## What an addon can observe

### Damage causes

| Cause | Dealt by | `DaoElement.fromDamageCause` |
| --- | --- | --- |
| `Cultivation_Seal_<Element>` (ten of them) | A seal's own hit, or a vanilla rune converted by a conversion seal | That element — the cause inherits the element's own cause |
| `Cultivation_DaoReaction` | A reaction's burst, and the Metal reaction's lingering ticks | `null` — deliberately no element, so a reaction can never chain another |

A damage system of your own can switch on these ids. `DaoElement.fromDamageCause`
(new in 0.11.1) walks a cause's `Inherits` chain, which is why a seal cause
resolves to its element while `Cultivation_DaoReaction`, inheriting only the
generic `Elemental`, resolves to nothing.

### Entity effects

| Effect | Meaning |
| --- | --- |
| `Cultivation_SealMark_<Element>` | The target carries that element's mark (6 s) |
| `Cultivation_SealReaction_<Element>` | The control outcome of a reaction on that element's mark |
| `Cultivation_SealBurst_<Element>` | The reaction's burst |
| `Cultivation_SealReaction_Cooldown` | Hidden; the target cannot react again yet (4 s) |

`<Element>` is the capitalized element name everywhere: `Metal`, `Wood`,
`Water`, `Fire`, `Earth`, `Ice`, `Wind`, `Poison`, `Lightning`, `Void`.

### Ids

| What | Id |
| --- | --- |
| Progressive Reveal system | `cultivation:daoSeals` |
| Codex article | `cultivation:daoSeals` |
| Admin config section | `Cultivation:DaoSeals` |
| Config file | `Arts/DaoSealConfig` — no `CultivationConfigs` accessor; see [Config access](config-access.md#files-with-no-accessor-yet) |

Gate a seal-related feature of your own on
`CultivationAPI.isSystemRevealed(accessor, ref, "cultivation:daoSeals")` so it
appears when Cultivation's own seals do.

## Config keys

`Arts/DaoSealConfig`: `Dao-Seals-Enabled`,
`Seal-Min-Realm`, `Shadow-Seal-Min-Realm`, `Seal-Qi-Cost-<Element>` (ten),
`Seal-Resonance-Qi-Discount-Percent`, `Seal-Resonance-Damage-Percent`,
`Seal-Reactions-Enabled`, `Seal-Reaction-Damage-Multiplier`,
`Seal-Reaction-Pvp-Multiplier`, `Seal-Reaction-Min-Mark-Age-Seconds`,
`Seal-Miasma-Spread-Radius`, `Seal-Miasma-Spread-Max`, `Seal-Arc-Beams-Enabled`,
`Seal-Bench-Refuse-In-Combat`.
