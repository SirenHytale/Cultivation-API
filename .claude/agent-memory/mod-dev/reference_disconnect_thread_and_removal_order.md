---
name: reference-disconnect-thread-and-removal-order
description: Which thread a PlayerDisconnectEvent fires on decides whether world.execute hops queued in the handler see a live entity; /stop disconnects players before plugin shutdown
metadata:
  type: reference
---

Verified in `.hytale-source-release/0.6.0/` on 2026-10-04 (wave 3, D-115 work):

- `/stop` runs `HytaleServer.shutdown0` on "ShutdownThread": `ShutdownEvent` (DISCONNECT_PLAYERS -> `Universe.disconnectAllPLayers`) fires BEFORE `pluginManager.shutdown()` (HytaleServer.java ~:563-571, Universe.java ~:442, ~:866-872). So a plugin's `clearAll()` never sees an online player at a stop; the disconnect handler does.
- `Universe.removePlayer` dispatches the event, then: off the world thread it QUEUES the entity removal on the world (~:1513-1524), so hops queued during the handler run first with a valid ref; ON the world thread it removes the entity SYNCHRONOUSLY right after the handler returns (~:1499-1505), so every queued hop finds the ref invalid.
- Off-thread: `/stop`, `/kick` and every command (CommandManager runs on `ForkJoinPool.commonPool`). On-thread: SirensCommands' AFK kick (a ticking system, store processing), `loggedInAgain`, world teardown kicks.
- `World.execute` always queues (`taskQueue.offer`), even from the world thread.

**How to apply:** anything a disconnect must persist or credit has to happen inline (non-structural only if the store is processing) for on-thread disconnects, or be deferred to the player's return. See [[hytale-ecs-component-writes-in-systems]].
