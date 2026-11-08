# FyreDisguise

Work-in-progress player-disguise framework for **Spigot 1.8.8** servers and **Minecraft 1.8.9 clients**.

## Implemented

- Java 8, Maven, Bukkit integration, typed disguise API and per-viewer overrides.
- Thread-safe identity registry reads for packet callbacks and main-thread-only state mutations.
- **ProtocolLib 4.8.0 PLAYER_INFO ADD_PLAYER rewriting:** outgoing profile entries can show an alias and optionally custom signed textures. Original subject UUID, gamemode, latency and display name are retained.
- Mojang texture retrieval, async bounded executor, timeouts and expiring LRU cache.
- Non-mutating render transitions and automated unit tests.

## Important limitations

**This is not yet a complete working in-game disguise plugin.**

The protocol hook rewrites only outgoing `PLAYER_INFO ADD_PLAYER` packets. Already visible entities/old tab entries are **not** refreshed when you call `setDisguise`; identity restoration in clients is not implemented. Existing players may need a normal server-driven respawn/refresh for a change to be visible, but no disruptive hide/show workaround is performed automatically. Skin signature bytes are not cryptographically checked by the library.

### Runtime requirements

- Spigot 1.8.8 and Java 8.
- Install **ProtocolLib 4.8.0**, compatible with legacy 1.8 servers (not current Java-17-only ProtocolLib versions).
- Compile with `mvn clean verify`. The dependency is provided, not bundled.

### API sample

```java
FyreDisguisePlugin plugin = JavaPlugin.getPlugin(FyreDisguisePlugin.class);
DisguiseService service = plugin.getDisguiseService();
service.setDisguise(new Disguise(subject.getUniqueId(), "PlayerAlias"));
service.setDisguiseFor(viewer.getUniqueId(), new Disguise(subject.getUniqueId(), "PrivateAlias"));
```

These state mutations must happen on the server primary thread.

## Release blockers

1. On-server tests of outgoing packet rewriting, list synchronization and client compatibility.
2. Implement safe live refresh and restoration for existing viewers, without bypassing vanish/visibility plugins.
3. Handle rejoin, respawn, world transitions, NPCs, scoreboard teams/nametags and skin switching.
4. Add packet integration tests and network performance profiling.
5. Add an open-source license before treating the repository as publicly reusable.

**No production release is recommended yet.**
