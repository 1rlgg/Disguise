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


## Identity-clear restoration behavior

When a running plugin consumer calls `DisguiseService.clearPlayer(id)` or `clear()`,
the observable registry now publishes per-viewer identity transitions for
still-active disguises. Both global aliases and viewer-specific-only aliases
are included, so the experimental tab/entity renderers may attempt to restore
the original identity. A normal disconnect intentionally performs quiet
registry cleanup: Bukkit is already removing that player's entity.

This remains **best-effort restoration**, not a guarantee that clients have
reloaded their original skins or overhead nametags. A custom
`DisguiseService` implementation must support subject enumeration to publish
whole-registry clear transitions. The built-in in-memory implementation does.
Queued updates can still be dropped at capacity, and real-client validation
is necessary before deploying experimental rendering.

### Still blocking a production release

- Live Spigot 1.8.8 and Minecraft 1.8.9 multi-client testing of full disguise
  and undisguise sequences, including respawn, teleport, death and server shutdown.
- Scoreboard/team nameplate coordination and compatibility with vanish,
  spectator, NPC and combat plugins.
- Measured combat hit registration, visibility stability and packet/tick costs.
- Owner-approved open-source license and supported release policy.


## Viewer cleanup and self-profile safety

`clearPlayer(id)` now publishes transitions in both roles: the ID as a
disguised **subject**, and (when still among online viewers) as a **viewer**
whose per-viewer overrides are removed. Removing a viewer override may reveal
a global disguise; that transition is no longer silently skipped. Actual
disconnects still use quiet cleanup because the player is leaving the server.

Outgoing `PLAYER_INFO ADD_PLAYER` rewriting now preserves the player's own
profile entry, even if their UUID has a global disguise. Aliases are applied
only to other recipients. This deliberately avoids renaming the local player's
profile and does **not** implement self-view disguises.

These safeguards are unit-tested. They do not establish in-world appearance,
nametag, scoreboard, spectator or combat correctness. The experimental entity
respawn switch remains **off by default**. Release still requires two-client
Spigot 1.8.8 / Minecraft 1.8.9 integration tests, real restoration checks,
scoreboard/vanish interoperability, performance measurements and a selected
open-source license.


## Scoreboard coexistence and admin diagnostics

The experimental 1.8 client-side entity destroy/respawn now **skips** a
viewer/subject pair when the viewer's scoreboard already owns the subject's
real-name entry through a team. This avoids interfering with an existing
team's nameplate prefix, visibility and friendly-invisibility rules.
The ordinary tab-list refresh path can still run. It does **not** make
scoreboard overhead nametags display per-viewer disguise aliases; full
team/nametag integration and live compatibility testing remain release blockers.

Operators with `fyredisguise.admin` can run:

```text
/fyredisguise stats
```

The command reports configured experimental refresh flags, queued tab/entity
refresh pairs, tracked spawn snapshots, applied tab pairs and outstanding
skin lookups. These are **runtime diagnostics**, not proof that a Minecraft
client visibly applied the disguise. The reported entity flag reflects
configuration, not whether the runtime fail-open breaker has activated.

Both `experimental-tab-refresh` and `experimental-entity-refresh`
remain **false by default**. Do not deploy experimental respawns on a live
PvP server before verifying entity tracking, nametags, skins, vanish behavior
and combat with real Spigot 1.8.8 / Minecraft 1.8.9 clients.


## Bounded refresh retries and failure recovery

Experimental tab-list and entity-refresh packet sends now have **up to two
retries per viewer/subject pair** after the initial attempt. Retries are
deferred to later ticks, stored in a capacity-bounded ledger, and forgotten
when a player disconnects. A failed undisguise restoration is prioritized
ahead of normal cosmetic refreshes. Persistent packet failures eventually
stop retrying; they are logged and **not silently presented as success**.

If an alias was set and cleared before any tab refresh was delivered, the
renderer now skips the unnecessary remove/add of the original player-info
entry. When a tab alias was previously applied, the same no-op guard does
**not** prevent the necessary restore.

Tab-list replacement prepares the real-profile recovery packet first. If
either its remove or alias-add packet send fails, it attempts to re-add the
original profile and retains any recovery error for logging. Network send
failures are inherently ambiguous, so the recovery is **best effort**, not
proof the client received the original skin or name.

### Still blocking production

1. Prove actual client identity restoration after failed packet delivery,
   death, respawn, reconnect, and plugin shutdown.
2. Verify in-world skins, scoreboard nametags, vanish and NPC compatibility.
3. Run real Spigot 1.8.8 / Minecraft 1.8.9 PvP sessions to check no player
   disappearance, combat hit-regression, or unexpected tracking behavior.
4. Record packet load, server tick impact, and queue pressure under realistic
   player counts; choose a release license.

The experimental entity destroy/respawn path and tab refresh remain **OFF by
default** and must not be deployed to a production competitive PvP server
without the live acceptance checks in `RELEASE_CHECKLIST.md`.


## Asynchronous skin isolation and retry telemetry

Viewer-specific skin assignments now use `getViewerOverride(viewer, subject)`
rather than treating `getDisguiseFor` (which may fall back to the global
identity) as proof of an override. This prevents accidental override creation
and also handles the legitimate case where a global and explicit viewer
override initially reference the **same immutable Disguise object**.
Custom `DisguiseService` implementations must override the new method to
allow viewer-specific skin changes; its default behavior is conservative.

Skin lookup requests use bounded **latest-request-wins** sequencing scoped
to global subjects or individual viewer/subject pairs. A prior Mojang
response cannot overwrite a more recent request for the same scope, even if
the older request completes first. Request tokens are cleared on player
disconnect and plugin disable. Global and per-viewer requests remain
independent. UUID and signed texture checks still apply.

`/fyredisguise stats` now additionally reports pending refresh retry
pairs, exhausted retry attempts and attempts rejected by the bounded retry
ledger. These counts are **not** proof of actual client-side restoration;
they are operator-visible signals to investigate. Observing repeated
exhaustion or rejected restores is a release blocker, not an invitation
to increase packet budgets without load testing.

### Remaining high-priority blockers

- Full per-viewer in-world nametags and scoreboard coexistence without
  overwriting existing PvP team entries
- Correct original skin/name restoration across packet failures, disable,
  death, world changes, spectator/vanish/NPC states and reconnect
- Real Spigot 1.8.8 plus Minecraft 1.8.9 multi-client integration
  with verified hit registration, visibility, and measured tick/packet cost
- Explicit owner choice of a release license and production deployment plan

Both `experimental-tab-refresh` and `experimental-entity-refresh`
remain **false** by default. Maven/JUnit success does not validate live PvP.


## Skin upstream outage hardening (development cycle 25)

Failed Mojang texture lookups now enter a short, bounded five-second
per-UUID cooldown. The resolver keeps only UUID/timestamp entries (up to 512)
and rejects repeated failed lookups immediately during that window instead
of repeatedly consuming HTTP worker threads. A success clears its failure
state. Normal successful skins retain their positive cache; this is not an
invalid-texture cache. The admin `/fyredisguise stats` command now reports
how many UUIDs are currently recorded in the failure cooldown table.

The outgoing experimental entity renderer also avoids incorrectly clearing
its circuit-breaker error streak after an ineligible/no-op world refresh.
If entity-packet recovery is necessary after a player moved, recovery uses
**current location and metadata**, not an old captured spawn coordinate.
Superseded async skin callbacks are filtered before queuing a main-thread
Bukkit task. These are software reliability improvements, not a substitute
for live multi-client 1.8.9 PvP compatibility testing.


## Recovery convergence and performance counters

The tab-list renderer now performs a **bounded, round-robin restoration
reconciliation once per 20 ticks**. It examines at most 128 previously applied
viewer/subject entries per scan and queues a restore when the disguise state
has been removed. This provides a recovery path for undisguise work displaced
by a saturated priority queue. Applied entries use stable insertion ordering
to keep the scan fair. After packet retries are exhausted, the pair enters a
bounded terminal quarantine and is not automatically requeued indefinitely;
a new explicit identity transition clears that quarantine. Existing retry
exhaustion and displaced-restore counters reveal these conditions.

`/fyredisguise stats` additionally reports total refresh batches, processed
viewer/subject pairs, and average/peak flush **elapsed wall time** in
microseconds. These are diagnostic wall-clock measurements that include
packet-send call duration, **not** a dedicated server CPU profiler, combat
latency measurement, or proof of production performance. Compare against a
vanilla test-server baseline and packet-rate/TPS measurements.

Failed upstream skin cooldowns are now pruned when queried or updated, so
expired Mojang failures do not inflate the displayed active-cooldown count.
No new gameplay-changing options were enabled. In-world refresh remains
disabled by default; real 1.8.9 multi-client PvP validation and overhead
nametag integration remain release blockers.


## Restoration safety: queue saturation and shutdown

Restoration requests already queued are no longer displaced by later
restoration requests when the urgent queue is full. New requests are
rejected and counted, allowing periodic applied-entry reconciliation to
attempt them later. A deterministic burst test exercises 96 simultaneous
undisguises through an eight-pair queue and verifies eventual convergence.

Applied-entry reconciliation now reuses immutable cached insertion-ordered
snapshots and iterates the requested slice without cloning the entire
collection every second. After a packet send failure, an identity that
**may** have reached a client remains tracked for best-effort restoration,
even if the send failed mid-sequence.

During plugin shutdown, restoration sends **tab-list packets only** and
does not attempt experimental client-side entity destruction or respawning.
Shutdown logs an aggregated outcome; its success counters describe packet
send attempts, not what connected Minecraft clients finally displayed.

These improvements reduce risks but do not replace real-client tests for
skin restoration, player invisibility, scoreboard names, packet bandwidth,
PvP hit registration and combat consistency. Both experimental renderer
flags remain disabled by default.


## Deferred cosmetic refresh reliability (development cycle 29)

The primary bounded refresh scheduler now uses insertion-ordered membership
sets, avoiding linear scans when promoting an already queued cosmetic alias to
an urgent restoration. If the primary queue is saturated, normal cosmetic
updates go into a **separate bounded deferred backlog** instead of being
discarded immediately. At most 32 deferred pairs are moved back to the main
queue after each flush, while urgent restorations remain ahead of cosmetics.
An explicit undisguise transition removes any obsolete deferred cosmetic pair.
Both queues have bounded capacities, and overflow is observable through
`/fyredisguise stats` (deferred pairs and deferred overflow counters).

This reduces lost alias changes during bursts but cannot guarantee eventual
visual correctness on an untested client. If **both** backlogs saturate, the
new cosmetic update can still be dropped, and the counter records that loss.
Real multiplayer PvP client testing and scoreboard-compatible nametags remain
release blockers. The experimental renderer is still disabled by default.


## Lifecycle resync tickets and bounded shutdown restoration

Delayed join, respawn, and world-change resynchronizations now use a
per-player generation ticket. A task queued before a disconnect/reconnect
or a later resync is discarded; cleanup on quit and plugin disable
invalidates outstanding tickets. Regression tests exercise rapid
reconnects and overlapping world-change tasks.

If experimental world refresh throws a transport error, the plugin now
attempts a **tab-list fallback immediately on every failure**, not just
when the entity breaker finally disables the world renderer. A failed
tab fallback does not count as an entity failure and is kept as a
suppressed error on the original failure.

Plugin disable performs **at most 256 synchronous tab restoration attempts**
rather than an unbounded packet burst. A warning reports packet failures,
unavailable/hidden pairs, and entries **not attempted** because of this
safety budget. The count is NOT proof of client-side success. Any remaining
stale player identities require client reconnect/retest; shutting down a
busy server is not a validated identity-restoration mechanism.

These are safeguards for an experimental plugin, not production
certification. Leave experimental refresh flags disabled on PvP
production servers pending real Spigot 1.8.8 and Minecraft 1.8.9
multiplayer tests.


## Restoration ordering and burst reliability (development cycle 32)

With many outstanding viewer pairs at shutdown, the 256-pair synchronous
tab restoration cap now **prioritizes pending undisguise work** before
older applied cosmetics. Entries not attempted are still reported, and
clients may need to reconnect to clear stale identities. This does not
make disable-time restoration comprehensive or client-verified.

When an urgent undisguise evicts an ordinary cosmetic update from the
bounded primary queue, the plugin attempts to place that cosmetic pair
in the bounded deferred backlog. If both queues saturate it still
records losses; it never permits cosmetic work to overtake urgent
restoration. Regression tests cover eviction, replay and a combined
saturation burst.

After an experimental world-renderer failure opens its circuit breaker,
tab-only updates regain the standard per-tick budget (32 in the plugin)
instead of retaining the eight-pair world-respawn safety cap. Both
experimental refresh flags remain off by default.

The remaining production release blockers are unchanged: actual
Minecraft 1.8.9 multi-client appearance and original-skin restoration,
safe per-viewer overhead nametag integration with scoreboard plugins,
vanish/spectator/NPC behavior, and measured PvP hit registration,
packet load and server TPS.
