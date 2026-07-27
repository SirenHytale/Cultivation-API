# Cultivation API — assistant instructions

This repository is the public integration surface of the Cultivation mod for
Hytale. If you are helping someone write a mod against it, read the full brief:

@AGENTS.md

The short version:

- **`api-sources/plugin/siren/API/` is authoritative.** Read or grep it before
  writing any call. Never reconstruct a signature from memory.
- **Never write to the `Store` from inside a system or listener.** The accessor
  you are handed may be a `CommandBuffer`; go through it.
- **Everything registers from `setup()`**, in any load order.
- **Namespace every id** with the mod's name — ids are global, and a collision
  silently replaces another mod's feature.
- **`provided` Maven scope**, never shaded.

`docs/pitfalls.md` is the highest-value file in the repository. Read it before
telling anyone their integration is finished.
