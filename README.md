# Kai 2.0

A persistent, **PvP/survival AI agent** for Paper that drives a FakePlayer bot to behave like a real
player. Kai runs **fully in-process** — there is **no external backend** (Kai 1.0's OpenRouter LLM
brain has been removed).

## Requirements

- **Paper** `26.1.2` (Maven artifact `io.papermc.paper:paper-api:26.1.2.build.67-stable`)
- **Java 25**
- A **FakePlayer plugin** installed on the server. The ported bot controller (`NMSBot`) talks to the
  plugin named **`FakePlayerPlugin`** via reflection (`getFppApi().spawnBot(Location, Player, String)`),
  which is what Kai 1.0 used. Declared as `softdepend`.
- *(Optional)* **LuckPerms** — `NMSBot` preloads the bot's LuckPerms user if present (cosmetic, guarded).

> Runtime note: a `26.1.x`-compatible FakePlayer build is required for the bot to actually spawn. Kai
> itself is version-independent — it compiles against `paper-api` + the public Bukkit/Paper API only.

## Architecture

Kai 2.0 keeps the **deterministic, local** subsystems from Kai 1.0 and drops everything that depended
on an external service or was outside the PvP/survival scope (OpenRouter gateway, chat moderation,
shop, personality/tease, grief logging, bridge writer).

| Package | System | Responsibility |
|---------|--------|----------------|
| `JonaSMP_AI` | main | Plugin entry; owns the shared `ChunkRadar` (`getInstance().getChunkRadar()`) and the bot |
| `watcher.AIPlayerBot` | brain | Per-tick orchestrator: combat → goals → equip/enchant → inventory/craft → pickup |
| `watcher.BotGoalPlanner` | goals | Deterministic GOAL→STEP planner (gather/mine/build/survive/follow) |
| `watcher.BotCombatManager` | pvp | Target selection, kiting, crit timing, mace-smash, shield-break, W-tap, auto-eat |
| `watcher.BotPathfinder` | pathing | A* navigation |
| `watcher.NMSBot` | bot | Spawns & controls the FakePlayer (FPP reflection + public Paper API) |
| `watcher.BotMemory(Storage)` | memory | Persistent player/threat/resource knowledge with TTL |
| `radar.ChunkRadar` (+`ChunkCache`/`ChunkFileReader`/`DeltaTracker`) | radar | Tiered chunk awareness: live deltas → RAM cache → loaded chunks |
| `watcher.BotAutoEquipper`/`BotAutoEnchanter`/`CraftingPlanner`/`ContainerScanner`/`BurrowManager`/`WorldProtection` | support | Gear, enchanting, crafting, container scanning, combat-burrow, anti-grief guards |

### Key changes vs. Kai 1.0

- **LLM removed.** Kai 1.0 asked an OpenRouter model for one action roughly every second — the cause
  of the sluggish "one decision per tick" feel. Kai 2.0's brain is the deterministic `BotGoalPlanner`
  + `BotCombatManager`, which were already the primary drivers in 1.0; the LLM was only a fallback.
- **Control loop reactivity.** The main `AIPlayerBot` tick task ran on a **20-tick (1 Hz)** period in
  1.0, so combat/movement only updated once per second. It now runs **every tick** (`runTaskTimer(..., 20L, 1L)`),
  with expensive perception/equip work kept on its own cadences.
- **Radar thread-safety.** The radar's chunk read ran on an **async** task in 1.0 while calling
  `World#getChunkAt` / `Chunk#getBlock` (not thread-safe). It now runs on the **main thread** and
  reads **at most one uncached chunk per cycle** to keep per-tick cost flat.

## Build

```bash
mvn clean package
```

The plugin jar is produced at `target/Kai-2.0.0.jar`.

## Commands

`/kai <spawn|despawn|come|say>` (permission `kai.command`, default OP)

- `spawn [name]` — create Kai at your location via FakePlayer (default name `Kai`)
- `despawn` — remove Kai
- `come` — order Kai to walk to you
- `say <text>` — issue a natural-language order routed to the deterministic goal planner
