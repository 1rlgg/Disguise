# FyreDisguise release validation checklist

This is a **manual acceptance plan**, not a record of passed live tests.
The current GitHub Actions pipeline compiles Java 8 code and runs JUnit tests,
but it does not launch a Spigot 1.8.8 server or Minecraft 1.8.9 clients.

## Environment and evidence

- [ ] Disposable Spigot 1.8.8 server with Java 8 and ProtocolLib 4.8.0
- [ ] Two or more vanilla Minecraft 1.8.9 clients, distinct authenticated profiles
- [ ] Record exact server build, ProtocolLib build, installed plugins and config
- [ ] Baseline server TPS, player count, packets/sec and relevant logs
- [ ] Keep last known-good CI JAR ready, along with world/plugin backups
- [ ] Confirm both experimental flags remain `false` in production

## Correctness

- [ ] Global alias visible in incoming player-list entries; original UUID preserved
- [ ] Viewer override affects only intended viewer and never local self-profile
- [ ] Signed skin applies to tab and existing world model where supported
- [ ] Alias change, `clear`, `unview`, `clearPlayer` and plugin shutdown restore expected identity
- [ ] Join, quit, death, respawn, teleport and world change do not leave ghost/duplicate entities
- [ ] Head orientation, held item, armor, skin layers and metadata remain correct after refresh
- [ ] Recovery from packet rejection does not strand a client-side entity

## Competitive gameplay and interoperability

- [ ] Hit registration, reach, knockback, attack cooldown/combos, potions and item use match baseline
- [ ] Vanished players remain invisible to unauthorized viewers
- [ ] Scoreboard team prefixes, suffixes, friendly visibility and collision behavior remain unchanged
- [ ] NPC, spectator, mounted, sleeping and dead player states are not broken
- [ ] Fast repeated aliases and reconnects do not create excess packet bursts
- [ ] No unexpected server tick spikes, off-thread Bukkit access or memory growth

## Load and failure testing

- [ ] Repeat alias/undisguise bursts with 10, 25, 50 and 100+ simulated player pairs
- [ ] Confirm `/fyredisguise stats` reports queue saturation and displaced restoration work
- [ ] Exercise Mojang timeouts, concurrent requests, cancellation and executor saturation
- [ ] Inject packet failures in a disposable environment and inspect recovery logs
- [ ] Validate after 30+ minutes sustained load and several plugin restart cycles
- [ ] Compare packet traffic and TPS with both experiments disabled and enabled separately

## Release gate

Do **not** declare production-ready until all blocking checks are evidenced,
the owner approves gameplay-affecting behavior, and a release license is selected.
Any client invisibility, combat regression, or stranded identity is a no-go:
disable both experimental options immediately and revert the faulty commit normally.


## New hard gates: async session validity and disable load

- [ ] Rapid join/quit/rejoin during the 2-tick resync delay never applies an
      old session's work to the new session
- [ ] Multiple respawn/world-change events coalesce to the most recent
      delayed resync ticket
- [ ] A first failed experimental world refresh attempts tab fallback and
      leaves the world failure breaker accounting accurate
- [ ] On disable with over 256 applied pairs, packet restoration is bounded,
      unattempted pairs are reported, and client reconnect clears any
      stale identity state
- [ ] Measure main-thread shutdown delay, packet rate and client appearance
      before considering a larger shutdown packet budget
