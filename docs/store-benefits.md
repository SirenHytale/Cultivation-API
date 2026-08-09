# Treasure Pavilion benefits

*New in 0.8.0.*

Cultivation ships an entitlement sync: the server periodically reads a public
list of the Hytale player UUIDs who bought a given product on
[the Treasure Pavilion](https://xianxia.dev/store), and anything on the server
can then ask whether a player owns it.

**The registry is open**, and that is the point of documenting it here. Your mod
registers its own product and gets the HTTP fetching, the caching, the operator's
config switches, the recheck command and (optionally) a wearable title for free —
without shipping a web request, a scheduler, or a credential of its own.

```java
CultivationAPI.registerStoreBenefit(
        StoreBenefit.builder("myMod:store:crown", "my-mod-crown")
                .name("server.myMod.store.crown")
                .title("server.myMod.title.crown")
                .build());
```

That is the whole integration for a cosmetic. For anything more, listen for
[the events](#reacting-to-a-grant-or-a-revoke) — and read the threading warning
there first, because these are the one family of events in this API that does
**not** fire on a world thread.

## What a benefit is, and what it is not

The website's list answers exactly one question: **which players bought this
product.** What that *means* on a server is entirely the registrant's business.

Nothing in this system grants gameplay power by itself, and nothing in it charges
anybody for anything — the transaction already happened elsewhere. A server owner
can also refuse any product by slug without touching the mod that registered it
(see [the operator's switches](#the-operators-switches)), so treat "the player
owns this" as a request you may be told to ignore rather than a fact you can
build a hard dependency on.

## Two ids, and they are not interchangeable

| | What it is | Namespaced? |
| --- | --- | --- |
| `key` | The id **this server** registers the benefit under, and the id of its auto-registered title | **Yes** — `"myMod:store:crown"` |
| `productSlug` | The `<slug>` in the store's `/api/get/entitlements/<slug>.json` | No — it belongs to the store, e.g. `"my-mod-crown"` |

The trap is that you **register with both and query with the slug**:

```java
// Right.
if (CultivationAPI.hasStoreBenefit(uuid, "my-mod-crown")) { … }

// Wrong — silently always false. This is the registry key, not the product.
if (CultivationAPI.hasStoreBenefit(uuid, "myMod:store:crown")) { … }
```

Keep the slug in a constant next to the `builder(...)` call so the two can only
ever disagree in one place.

`key` still follows [the namespacing rule](pitfalls.md#5-colliding-on-an-id) that
every other registry in this API follows: registering an existing key **replaces**
the previous holder rather than erroring.

## Asking whether a player owns one

```java
boolean owns = CultivationAPI.hasStoreBenefit(playerUuid, "my-mod-crown");
```

- Takes a **`UUID`**, not a `PlayerRef` or a `Ref` — so it answers for offline
  players too, which is what makes it usable from a leaderboard or a web hook.
- **Safe from any thread.** It reads a cached set; there is no request behind it.
- Cheap enough for a per-draw check, so there is no reason to cache the answer in
  a component of your own.
- **False** when the operator disabled the whole system, or disabled that one
  product, or the very first sweep has not landed yet. `false` therefore means
  "do not apply this", never "the player is not entitled" — do not write it back
  anywhere as a fact.

A `null` UUID is accepted and answers `false`, so an unresolved player needs no
guard of its own.

## A title for free

`StoreBenefit.Builder#title` also registers a [`CultivationTitle`](registries.md#titles)
for the benefit, unlocked for exactly the players who bought it:

```java
StoreBenefit.builder("myMod:store:crown", "my-mod-crown")
        .name("server.myMod.store.crown")     // how the benefit is listed
        .title("server.myMod.title.crown")    // the label players wear
        .build();
```

The title appears on every player's picker — **greyed** for those who do not own
it, which is both this mod's convention for anything locked and the only
advertising the system does. Omit `.title(...)` and no title is registered; the
benefit then exists purely for your own code to query.

Both strings are translation keys, under [your own prefix](pitfalls.md#4-trying-to-override-cultivations-lang-keys).

## Reacting to a grant or a revoke

`StoreBenefitEvents` fires when a sweep discovers a change. Three post-events,
none of them cancellable:

| Event | Fires when |
| --- | --- |
| `BenefitGrantedEvent` | A player the last sweep did not list is now entitled |
| `BenefitRevokedEvent` | A previously entitled player is no longer listed — a refund, a chargeback, or the server disabling the product |
| `SyncCompletedEvent` | One sweep finished; carries `products`, `granted`, `revoked` and the `failedProducts` whose fetch came to nothing |

Nothing here is cancellable **deliberately**: the sync reports what the store
says is owned, and local policy already has its own switch further up.

### These do not run on a world thread

This is the one place in this API where the threading rule from
[Events](events.md#threading) does not hold. Grants and revokes are discovered by
an HTTP sweep, so the listener runs on **the remote checker thread**, with no
world in hand and no `Ref` in the payload — only a `UUID`.

A listener that touches a player must find them and hop onto their world thread
first:

```java
StoreBenefitEvents.onBenefitGranted(event -> {
    // Bookkeeping is fine right here.
    myPlugin.recordPurchase(event.playerUuid(), event.benefit().getKey());

    // Touching the player is not.
    PlayerRef player = Universe.get().getPlayer(event.playerUuid());
    if (player == null || !player.isValid()) {
        return;   // offline, or gone; the next sweep or their next join will do
    }

    World world = Universe.get().getWorld(player.getWorldUuid());
    if (world == null) {
        return;
    }

    world.execute(() -> {
        Ref<EntityStore> ref = player.getReference();
        if (ref == null || !ref.isValid()) {
            return;
        }
        myPlugin.applyCrown(ref.getStore(), ref);
    });
});
```

Both grant and revoke fire **whether or not the player is online**, so the
offline case is the normal one rather than the edge case. Write the listener so
that doing nothing is a correct outcome and the state is re-derived on join —
which is exactly what the auto-registered title does: its wearer keeps a revoked
title only until their next login, when it is re-validated.

`failedProducts` on `SyncCompletedEvent` names fetches that came to nothing. A
failed fetch **keeps the previous list** rather than clearing it, so a network
blip does not mass-revoke everybody. If you mirror entitlements into your own
storage, check this list before treating a sweep as authoritative.

## The operator's switches

`WebStoreConfig.json`, reachable through
[`CultivationConfigs.webStore()`](config-access.md#the-files):

| Setting | Default | |
| --- | --- | --- |
| `WebStoreEnabled` | `true` | The master switch. Off means `hasStoreBenefit` is always `false` and no sweep runs. |
| `CheckIntervalHours` | `6` | How often the lists are re-read |
| `AllowRecheckCommand` | `true` | Whether `/cultivation store recheck` will run |
| `RecheckCooldownSeconds` | `60` | Rate limit on that command |
| `BaseUrl` | `https://xianxia.dev/api/get/entitlements/` | Where the lists are read from |
| `DisabledBenefits` | *(empty)* | Product **slugs** the server refuses, whoever registered them |

An addon that extends this system should gate on the master switch at the point
of use rather than in `setup()` — it is editable live:

```java
if (!CultivationConfigs.webStore().get().isWebStoreEnabled()) {
    return;
}
```

Registering a benefit on a server with the system disabled is harmless: it sits
in the registry, nothing is fetched, and every query answers `false`.

`/cultivation store` (bare, or `status`) reports every registered product, how many
players hold it and when the next sweep is due; `/cultivation store recheck` forces
a sweep. Both come with registration — you do not add a command. **Both are gated
on `cultivation.admin`**: this is an operator's diagnostic, not a player-facing
storefront. A player's own question — *did my purchase arrive?* — is answered by
seeing their title ungrey on the picker, which is why `.title(...)` is worth setting
even for a benefit whose real effect is something else.

## Registration timing

Register from `setup()` like everything else. **Registering before or after the
sync has started both work** — a late registration is fetched immediately rather
than waiting for the next interval, so there is no ordering requirement against
Cultivation's own boot and no reason to defer.

`unregisterStoreBenefit(key)` exists for a plugin unloading cleanly.
`getStoreBenefits()` returns every registered benefit in registration order, as a
fresh list safe to hold.

## Rules

- **Query by `productSlug`, register with both ids.** The commonest mistake here.
- **Namespace the `key`.** Not the slug — that one is the store's.
- **`false` means "do not apply", not "not entitled".** A disabled system, a
  disabled product and a sweep that has not landed all look the same.
- **The events are not on a world thread.** Hop before you read a component.
- **Handle the offline case as the normal one.** Both events fire for players who
  are not on the server.
- **Re-derive on join** rather than trusting that you saw the revoke.
- **Never charge, unlock, or price anything from this.** It reports a purchase
  that already happened somewhere else.
