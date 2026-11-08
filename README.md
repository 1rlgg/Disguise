# FyreDisguise

Experimental Java 8 / Spigot 1.8.8 disguise framework for Minecraft 1.8.9 clients.

## Implemented

- Global and per-viewer alias registry, server-main-thread mutations, quit cleanup.
- ProtocolLib 4.8.0 outgoing `PLAYER_INFO ADD_PLAYER` rewriting, preserving UUID and unrelated profile properties.
- Optional **tab-list-only** updates, disabled by default, now queued and deduplicated with a cap of 2,048 pending pairs and 32 refresh attempts per tick.
- Experimental tab-list resynchronization after login, respawn and world changes.
- Best-effort re-add of original player-info data if a custom tab-list ADD fails.
- Admin testing commands for global/per-viewer alias state.
- Async Mojang skin-property lookups, expiry/caching, coalescing and bounded request workers.
- Java 8 Maven tests, GitHub Actions builds and a downloadable development JAR artifact.

## Build and testing

Use Java 8, Maven, Spigot 1.8.8 and **ProtocolLib 4.8.0** installed separately:

```sh
mvn clean verify
```

To try tab-list updates in a disposable test server, set `experimental-tab-refresh: true` in `plugins/FyreDisguise/config.yml` and restart. It remains off by default. Check with two Minecraft 1.8.9 clients: join, apply and remove an alias, viewer-only override, death/respawn, world change, disconnect and reconnect. Verify no unexpected tab entries, missing skins or invisibility. The queue may drop updates when full, and must not be treated as a guaranteed delivery mechanism.

### Test commands (permission: `fyredisguise.admin`, operator by default)

```text
/fyredisguise set <online-player> <alias>
/fyredisguise clear <online-player>
/fyredisguise status <online-player>
/fyredisguise view <viewer> <online-player> <alias>
/fyredisguise unview <viewer> <online-player>
/fyredisguise viewstatus <viewer> <online-player>
```

## Still missing before a production release

1. Full **in-world entity name and skin disguise rendering**, and safe live entity refresh.
2. Guaranteed restoration of client identity, including plugin disable, reconnect, respawn and world transitions.
3. Overhead nametag/team and vanish/NPC/spectator interoperability.
4. Real 1.8.8 server / 1.8.9 client protocol tests, combat and performance profiling.
5. Owner-selected open-source license, versioned releases and compatibility documentation.

**A green Maven CI build does not demonstrate working world disguises.** Do not deploy this experimental plugin on a production PvP server.
