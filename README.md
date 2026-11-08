# FyreDisguise

Experimental Minecraft 1.8.9 client player-disguise plugin for Java 8 and Spigot 1.8.8.

## What currently works in code

- Global/per-viewer player aliases and Mojang texture-property state.
- Outgoing ProtocolLib 4.8 `PLAYER_INFO ADD_PLAYER` rewrite with signed textures.
- Async skin lookup, cache, permission-protected testing commands.
- Experimental tab-list refresh and **new opt-in in-world entity refresh prototype**.
- Entity prototype captures original server-produced `NAMED_ENTITY_SPAWN` snapshots per viewer, preserving entity ID and UUID; on a disguise transition it sends player-info refresh, entity destroy, cloned spawn with current location and metadata, head rotation and five equipment slots.
- The experiment is bounded (4,096 tracked snapshots and 2,048 queued refreshes; 32 attempts per tick).
- Java 8 CI and unit tests with downloadable development JAR.

## Install and build

Requires Java 8, Spigot 1.8.8, ProtocolLib 4.8.0. Build using `mvn clean verify`.

## Experimental options

In `plugins/FyreDisguise/config.yml`:

```yaml
experimental-tab-refresh: false
experimental-entity-refresh: false
```

**Both are disabled by default.** To evaluate in-world name and skin refresh *only on a disposable test server*, set both to `true` and restart. Entity refresh requires the tab refresh option. Normal PvP/production servers should leave both off.

The entity backend only attempts to refresh a subject if an original spawn packet was observed for that viewer and the viewer can currently see the subject in the same world. It deliberately does not modify Bukkit visibility or server hitboxes. Client-side flicker and incompatibilities may still occur. Client appearance, scoreboard nametags, mounted/sleeping status, and packet ordering require live verification.

## Admin-only test commands

Permission `fyredisguise.admin` (default operator); named players must be online, but skin source can be an exact UUID:

```text
/fyredisguise set <player> <alias>
/fyredisguise clear <player>
/fyredisguise status <player>
/fyredisguise view <viewer> <player> <alias>
/fyredisguise unview <viewer> <player>
/fyredisguise viewstatus <viewer> <player>
/fyredisguise skin <target> <skin-source-or-uuid>
/fyredisguise viewskin <viewer> <target> <skin-source-or-uuid>
```

## Release blockers

1. **Real client verification:** two Minecraft 1.8.9 clients, Spigot 1.8.8, ProtocolLib 4.8.0; compare baseline and experiment with aliases, skins and undisguise.
2. Confirm no disappearing entities, hit registration changes, exploit-prone tracking, gear loss, skin glitches, tab ordering or vanish bypass.
3. Handle nonstandard states: sleeping, passengers, spectating, NPCs, scoreboard nametags and third-party plugins.
4. Correctly restore world and tab identity on all disconnect/reconnect, respawn, world and shutdown paths.
5. Add integration and performance tests, choose open-source license, and audit for release.

**CI compilation/unit tests do not prove Minecraft client correctness. This is NOT production-ready.**
