# FyreDisguise

Experimental Java 8 / Spigot 1.8.8 player-disguise framework for Minecraft 1.8.9 clients.

## Implemented

- Global and per-viewer aliases, skin properties, admin testing commands and bounded Mojang lookups.
- ProtocolLib 4.8.0 player-info rewriting, including alias-aware tab-list display labels.
- Experimental tab-list updating and best-effort restoration.
- Experimental client-side in-world entity refresh using actual previously captured player spawn packets; original UUID and entity ID retained.
- Conservative world refresh guards: skip vanished/invisible, dead, sleeping or mounted players, missing snapshots, cross-world pairs and out-of-range viewers.
- Bounded queues (2,048 pairs; up to 32 tab updates and 8 entity refresh attempts per tick).
- Java 8 Maven CI with downloadable development JAR.

## Build

```sh
mvn clean verify
```

Java 8, Spigot 1.8.8 and ProtocolLib 4.8.0 are required.

## Config (both disabled by default)

```yaml
experimental-tab-refresh: false
experimental-entity-refresh: false
```

**Enable both only on a disposable test server with two Minecraft 1.8.9 clients.** The entity option requires the tab option. Client-side destroy/respawn may flicker or conflict with other plugins even though it never changes server hitboxes or Bukkit player visibility.

## Acceptance tests still required

- Verify profile alias and skin in tab and on in-world model; change and remove them while a player is standing, moving, and crossing view distance.
- Verify original name/skin restoration, login/logout, respawn, teleport, world switches and plugin disable.
- Confirm no disappearing entities, equipment mismatches, hit-registration changes, sudden loss of entity tracking or unintended visibility of vanished players.
- Test player-list display formatting and scoreboard nametag behavior with existing PvP plugins.
- Test near chunk boundaries and at scale; measure network packet rate and tick time.
- Select an open-source license before public release.

**Not production ready.** CI cannot prove client correctness or validate combat interactions.
