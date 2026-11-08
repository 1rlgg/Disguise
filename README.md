# FyreDisguise

Open-source disguise framework **under development** for Spigot 1.8.8 servers and Minecraft 1.8.9 clients.

## What works today

- Java 8 Spigot plugin entry point and public `DisguiseService`.
- Validated player-name disguise state.
- Global disguise state and per-viewer overrides with global fallback.
- Quit and disable cleanup to avoid stale player state.
- Validated Base64 skin property value object (data model only).
- Unit tests for registry semantics and cleanup.

**Important:** These are desired-identity data structures. **Players do not visibly disguise in-game yet.** No packet engine, skin download service, or visual refresh logic has been implemented. Do not deploy as a working disguise solution.

## Build and test

Use JDK 8 and Maven:

```bash
mvn clean verify
```

The Spigot API is provided by the server at runtime. The JAR, once built, is placed under `target/`.

## Example (state only)

```java
FyreDisguisePlugin plugin = JavaPlugin.getPlugin(FyreDisguisePlugin.class);
DisguiseService service = plugin.getDisguiseService();
service.setDisguise(new Disguise(player.getUniqueId(), "ExampleName"));
service.setDisguiseFor(viewer.getUniqueId(), new Disguise(player.getUniqueId(), "ViewerOnly"));
service.removeDisguiseFor(viewer.getUniqueId(), player.getUniqueId());
```

Call service methods from the primary server thread. Entries are not persisted between sessions and are removed when players disconnect.

## Remaining work before a production release

1. Implement packet-level player-info and entity refresh operations for Spigot 1.8.8.
2. Ensure safe restore, respawn, reconnect, tablist, vanish, and spectator interactions.
3. Apply signed Mojang skin properties and add bounded async caching.
4. Add visual per-viewer disguises and scoreboard/nametag compatibility.
5. Run Maven CI, protocol integration tests, and real-server performance profiling.

## License

No open-source license has yet been selected. Until one is added, ordinary copyright restrictions apply.
