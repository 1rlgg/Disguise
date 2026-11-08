# FyreDisguise

Experimental Java 8 / Spigot 1.8.8 disguise framework for Minecraft 1.8.9 clients.

## Working components

- Global/per-viewer disguise identities and operator test commands.
- ProtocolLib 4.8.0 outgoing `PLAYER_INFO ADD_PLAYER` aliases.
- Experimental tab-list-only updates and best-effort restores (disabled by default).
- Async Mojang skin lookup, bounded queue/cache, and single-flight network requests.
- Signed texture lookup source by online username **or exact full Mojang UUID**, including offline sources.
- Sessionserver profile UUID validation so an unexpected profile cannot substitute signed properties.
- Safe per-viewer skin assignments that never synthesize a new viewer override from a global alias.
- Java 8 Maven tests and an uploaded GitHub Actions development JAR.

## Usage

Java 8, Spigot 1.8.8, ProtocolLib 4.8.0 installed separately. Build with `mvn clean verify`. All commands require `fyredisguise.admin` (operators by default).

```text
/fyredisguise set <online-player> <alias>
/fyredisguise view <viewer> <online-player> <alias>
/fyredisguise skin <online-target> <online-source|full-uuid>
/fyredisguise viewskin <viewer> <online-target> <online-source|full-uuid>
/fyredisguise clear <online-player>
/fyredisguise unview <viewer> <online-player>
/fyredisguise status <online-player>
/fyredisguise viewstatus <viewer> <online-player>
```

The target and optional viewer must be online; the source account can be offline when its UUID is known. The source UUID is *not* a username resolver. Mojang must return a signed textures property with matching profile UUID; cryptographic verification of the signature is not currently implemented.

The `experimental-tab-refresh: false` config option stays off by default. Only experiment on a disposable server.

## Outstanding release blockers

1. **No full in-world name or skin disguise implementation.** Existing entities are not re-spawned.
2. Identity restore is limited to best-effort tab entries, not full entity appearance.
3. Nametag/team/vanish/NPC/spectator compatibility and PvP hit registration are untested.
4. Real Spigot 1.8.8 + Minecraft 1.8.9 multi-client verification and load profiling are still missing.
5. Open-source licensing and release criteria need owner approval.

A successful Maven build does **not** establish production-ready Minecraft disguise functionality.
