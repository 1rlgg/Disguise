# FyreDisguise

A work-in-progress, open-source disguise framework targeting **Spigot 1.8.8** and **Minecraft 1.8.9 clients**.

## Current status

**Foundation only.** This version registers a Spigot plugin and exposes an in-memory disguise-state API. It does **not** change player names, skins, tab lists, or entity appearance in-game yet. Do not deploy expecting working disguises.

## Build

Requires JDK 8 and Maven:

```bash
mvn clean package
```

The Spigot 1.8.8 API is declared as a provided dependency. The resulting JAR, once built, belongs in the server's `plugins/` directory.

## Developer API

```java
FyreDisguisePlugin plugin = JavaPlugin.getPlugin(FyreDisguisePlugin.class);
DisguiseService service = plugin.getDisguiseService();
service.setDisguise(new Disguise(player.getUniqueId(), "ExampleName"));
```

This stores desired state only. Call the API from the primary server thread. State is cleared on plugin disable and is not persisted.

## Roadmap

1. Packet-based player identity updates with safe restoration.
2. Per-viewer disguises and player refresh lifecycle.
3. Skin property validation, asynchronous fetching, and bounded caching.
4. Scoreboard/nametag compatibility.
5. Tests, CI, performance profiling, and documentation.

## Compatibility

Target server: Spigot 1.8.8. Client: Minecraft 1.8.9. This repository has not yet been verified with a real server integration test.
