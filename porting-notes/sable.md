# Sable / Simulated (Create Aeronautics) compat — 26.1.2 Fabric

Branch `port/sable` (off `26.1-fabric`). Against the Aeronautics port in `../create-aeronautics-fabric`
(Sable 2.0.3, Simulated 1.3.0; the user's instance runs `create-aeronautics-4in1-fabric-26.1.2-1.3.0.jar`).

## Build

- `sable_jar` in `gradle.properties` → `../create-aeronautics-fabric/sable/build/libs/sable-fabric-26.1.2-2.0.3-fabric.26.1.2-port.jar`,
  `compileOnly files(sable_jar)`. Rebuild it there (`./gradlew :sable:build`) if the file is missing. Simulated is not
  on the classpath (nothing references it any more).
- `compat/sable` is no longer excluded in `build.gradle`; `compat/simulated` and the two mixins are deleted (below).

## Detection (optional dependency)

- 1.21.1: `SIM_ACTIVE = isLoaded("simulated")`, `SABLE_ACTIVE = isLoaded("sable") && isLoaded("sablecompanion")`.
- Fabric: Sable Companion is bundled in the Sable jar (no `sablecompanion` mod id), so `SABLE_ACTIVE =
  isModLoaded("sable")`; `SIM_ACTIVE = isModLoaded("simulated")` (kept as a flag, unused).
- `port/sable/SableUtil` is the facade every non-compat call site uses: it forwards to `compat/sable/SableUtil` only
  when `SABLE_ACTIVE`, otherwise returns the non-Sable answer. `CreateAddition.onInitialize` calls
  `CreateAdditionSable.register()` only when `SABLE_ACTIVE`. Checked in the built classes: only `compat/sable/*`
  reference `dev.ryanhcode`; no mixin targets Sable/Simulated, so no mixin plugin is needed.

## Ported

- `compat/sable/SableUtil`: unchanged logic (Sable Companion's API is the same in the port). Wire break checks
  (`breakWires`, `localNodeDistSq`, `lookupWireNode`), render helpers (`localDown`, `transformRelativeTo`,
  `nodeWireOffsetRelativeTo`). New: `projectOut` (world position of a plot block, for light), and
  `transformRelativeTo` short-circuits when both points are on the same sub-level.
- `SableEvents`/`CreateAdditionSable`: NeoForge `ForgeSablePostPhysicsTickEvent` → `FabricSablePostPhysicsTickEvent.EVENT`
  (`(SubLevelPhysicsSystem, double)`); every 10 game ticks, sub-level wire nodes check their wire lengths.
- Wire rendering (`WireNodeRenderer`): the 1.21.1 `renderSubLevelAware` path moved to extract/submit
  (`extractSubLevelAware`). Used whenever Sable is loaded: wires are computed in the connector's own frame, sag along
  the frame's down (`Wire` now carries the gravity vector; festive lights follow it), one end per loaded pair draws.
  Light is read at the projected world positions. `wireBounds` transforms the other ends into the same frame (Sable
  calls `shouldRender` for sub-level block entities with the camera in plot space).
- Liquid blaze burner: no special renderer needed (see below). Its idle head now tracks the player in the sub-level's
  frame (`tickAnimation`, through the facade).

## Not ported (superseded) and why

- `VanillaSubLevelBERenderMixin` + `ISableRender` + `SableRenderCompat` + `LiquidBlazeBurnerSubLevelRenderer`: on
  1.21.1 Sable rendered sub-level block entities through `VanillaSubLevelBlockEntityRenderer.renderSingleBE`, and CC&A
  hooked it to draw connectors with its own immediate buffer. In the 26.1 Sable port `renderSingleBE` is an empty stub
  that is never called; instead `mixin/sublevel_render/block_entity_render/LevelRendererMixin` extracts every plot
  block entity with vanilla `tryExtractRenderState` (camera moved into plot space for `shouldRender`) and multiplies
  the sub-level transform into the pose stack in `submitBlockEntities` (Sodium path calls the same extraction). So
  CC&A's normal renderers run for sub-level block entities; the work moved into `WireNodeRenderer` instead.
- `DockingConnectorBEMixin` + `compat/simulated` (`DockEnergyStorage`, `DockingConnectorBEAccess`, `SIMCapabilities`):
  Simulated 1.3.0 already has docking connector energy: `DockingConnectorBlockEntity.battery`
  (`DockingConnectorBattery`, connected/disconnected in `setDock`/`unDock`, saved as `Battery`, capacity/throughput
  in Simulated's server config `dockingConnectorFECapacity`/`dockingConnectorFEThroughput`) exposed as Team Reborn
  `EnergyStorage.SIDED` (`FabricSimInventoryService.registerBattery`). Inserting into one connector fills the docked
  partner's battery; extracting takes from its own. CC&A's connectors use it like any TR storage. Registering CC&A's
  storage too would be a duplicate `EnergyStorage.SIDED` provider for `simulated:docking_connector` (Fabric keeps the
  first one, so the result would depend on mod init order). The hooks the mixin used still exist (`otherConnectorPosition`,
  `setDock` → `tank.connect`, `unDock` → `tank.disconnect`) if this is ever wanted. `CommonConfig.DOCKING_CONNECTOR_CAPACITY`
  is kept (config compatibility) but unused; its comment says so.
- `index/CACapabilities` still has the old "Simulated capabilities are excluded" note (left alone to avoid clashing
  with the CC:Tweaked branch); nothing needs registering there.

## Check in game (needs the Aeronautics jar)

- Without Aeronautics: CC&A loads, wires render and connect as before (non-Sable path unchanged).
- Wires between a ship and the ground (and ship ↔ ship, and both ends on one ship): drawn once, from the right
  endpoints, sagging "down" in the world while the ship rolls/pitches; lighting sensible (not plot-dark); visible
  when only the far end is on screen; festive lights along the sag.
- Placing a wire from a ship connector (held-wire preview runs to the player).
- Wire breaking when the ship flies further than the max wire length (break sound at both ends, wire dropped) — from
  the ground connector's tick and from the sub-level physics tick.
- Connecting a ship connector to a ground one: length check uses world distance (`IWireNode.connect` → `localNodeDistSq`).
- Wire graph after leaving/rejoining the world with a ship connected (sub-level nodes register when they load).
- Liquid blaze burner on a ship: blaze, flame and liquid hat rendered and moving with the ship; idle head looks at you.
- Docking connector power: CC&A connector (push) → ship docking connector → docked ground connector → CC&A
  connector (pull) / accumulator; nothing flows when undocked.
