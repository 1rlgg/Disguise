# FyreDisguise

An open-source **work-in-progress** disguise framework for Spigot 1.8.8 with Minecraft 1.8.9 clients.

## Implemented foundation

- Java 8 Maven plugin and a main-thread disguise-state API.
- Global and per-viewer identity selection, efficient change planning, and no-op deduplication.
- Rendering interface for a future protocol-specific backend (current default is **diagnostic only**).
- Quit/shutdown state cleanup.
- Mojang signed texture-property lookup with connection/read timeouts and response size limits.
- Bounded asynchronous texture lookups with expiring, bounded LRU cache.
- JUnit tests and GitHub Actions Maven verification.

**Important:** Visible disguises are **not implemented**. The diagnostic renderer does not send packets or alter skins, tab lists, or entity names. No claim of production readiness is made.

## Build

```bash
mvn clean verify
```

Use a Java 8 JDK; Spigot 1.8.8 API is a provided dependency.

## API example (requests only)

```java
FyreDisguisePlugin plugin = JavaPlugin.getPlugin(FyreDisguisePlugin.class);
DisguiseService service = plugin.getDisguiseService();
service.setDisguise(new Disguise(subject.getUniqueId(), "NewAlias"));
service.setDisguiseFor(viewer.getUniqueId(), new Disguise(subject.getUniqueId(), "ViewerAlias"));
service.removeDisguiseFor(viewer.getUniqueId(), subject.getUniqueId());
```

Disguise state calls must run on the primary Bukkit thread. Async skin lookup futures complete on background workers; schedule any Bukkit activity back to the main thread.

## Remaining release-critical milestones

1. Implement and validate a Spigot 1.8.8 packet transport (player-info add/remove and entity refresh).
2. Render global/per-viewer changes and restore the original identity safely.
3. Add reconnect, teleport, respawn, scoreboard/nametag, vanish, tab-list and skin synchronization behavior.
4. Add protocol integration tests, stress tests, and real-server compatibility checks.
5. Evaluate skin retrieval backpressure and concurrency under load, document limits, choose an open-source license.

This repository does not yet contain a production-ready disguise plugin.
