# Kai 2.0

A persistent, **PvP-focused AI agent** for Paper that drives a [FakePlayer](https://github.com/tanyaofei/minecraft-fakeplayer)
bot to behave like a real player. Kai runs **fully in-process** — no external backend.

## Requirements

- **Paper** `26.1.2` (Maven artifact `io.papermc.paper:paper-api:26.1.2.build.67-stable`)
- **Java 25**
- **FakePlayer (FPP)** installed on the server (Kai uses it to create the bot; declared as `softdepend`)

> Note: stock FakePlayer `0.3.19` ships NMS modules only up to `1.21.10`. A `26.1.x`-compatible
> FakePlayer build is required at runtime for Kai to spawn its bot. Kai itself is version-independent
> (it compiles against `paper-api` + the public Bukkit/Paper API only).

## Architecture

Kai is split into focused systems, all driven by a single main-thread tick:

| System | Responsibility |
|--------|----------------|
| `scheduler` | One heartbeat (`TickService`) + a budget-based `LoadBalancer` that spreads heavy work across ticks |
| `cache` | Generic tick-based TTL cache (`TtlCache`) — the RAM tier |
| `memory` | `MemorySystem` — what Kai remembers about perceived entities (lookup order: Memory → Cache → Live) |
| `radar` | `ChunkRadar` — tiered awareness (loaded chunks → RAM cache), feeds memory, never force-loads |
| `goal` | `GoalSystem` — picks the single highest-priority intent (ENGAGE / PURSUE / IDLE) |
| `task` | `TaskSystem` — runs one multi-tick task at a time (e.g. `MoveToTask`) |
| `pvp` | `PvpController` — target tracking, distance control, strafing, LOS, gear, melee |
| `bot` | `BotBackend` / `FppBotBackend` — spawns & owns the FakePlayer; `KaiBot` is the control surface |

### Performance principles

- **No per-tick world scans.** Perception runs on cadences; target search is a bounded
  `getNearbyEntities`, not a chunk/block sweep.
- **Flat tick cost.** Deferrable work is queued onto the `LoadBalancer` and run under a fixed
  per-tick time budget.
- **Cache first.** Line-of-sight, gear choice and chunk summaries are cached and only recomputed on
  their own cadences.
- **Responsive combat.** Aiming/moving/attacking run on every control tick (configurable), avoiding
  the sluggish "one decision per tick" feel of Kai 1.x.
- **Main-thread only.** All Bukkit/Paper access happens on the main thread; caches use concurrent
  maps defensively.

## Build

```bash
mvn clean package
```

The plugin jar is produced at `target/Kai-2.0.0.jar`.

## Commands

`/kai <spawn|despawn|status|engage|stop>` (permission `kai.command`, default OP)

- `spawn` — create Kai at your location via FakePlayer
- `despawn` — remove Kai
- `status` — show goal, target, radar/memory/queue stats
- `engage <player>` — force Kai to target an online player
- `stop` — clear Kai's current target and task

Configuration lives in `config.yml` (cadences, budgets, ranges) — see comments there.
