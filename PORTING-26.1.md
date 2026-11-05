# 26.1.2 Fabric Port — Working Log

Branch `26.1-fabric`, from `1.21.1` (Create Crafts & Additions 1.7.2, Create 6.0.10, Minecraft 1.21.1, NeoForge).
Goal: CC&A on **Fabric 26.1.2**, running on **Create Fly** (our fork at `../Create-Fly`), alongside the Steam 'n'
Rails, Create Aeronautics, Create Big Cannons, Design n' Decor and Copycats+ ports.

Licence: MIT (code and art; bundled audio is Pixabay / CC0). No redistribution limits, but push/release only when
the user asks.

This file tracks the port's state so work can resume across sessions. Update it as you go.

## Starting point

| | |
|---|---|
| Upstream | `mrh0/createaddition` (`upstream`); fork `alec-scott/createaddition` (`origin`) |
| Base branch | `1.21.1`: one NeoForge module, 237 Java files, 8 mixins, Registrate, 218 distinct Create/Catnip/Flywheel/Registrate imports |
| Generated data | Committed in `src/generated/resources` |
| Fabric reference | `origin/fabric-1.20.1` (Create Fabric 0.5, TR Energy): useful for Fabric energy/transfer idioms, not as a base |

Three ports at once, as for Steam 'n' Rails / D&D (see `../Create-Design-n-Decor/PORTING-26.1.md`):
1. **NeoForge → Fabric.**
2. **Minecraft 1.21.1 → 26.1.2.**
3. **Create → Create Fly** (`com.zurrtum.create`, client under `com.zurrtum.create.client.*`, Flywheel/Catnip/Ponder
   bundled, no Registrate or datagen helpers).

## Building

```sh
export JAVA_HOME=/c/Users/akrsc/AppData/Roaming/PrismLauncher/java/java-runtime-epsilon   # Git Bash, JDK 25
./gradlew compileJava --console=plain
```

Error triage: `perl porting-tools/errs_summary.pl build.txt` (one line per error with the missing symbol) and
`perl porting-tools/errs_by_file.pl build.txt` (counts per file; each error is counted twice, gradle repeats them).
Mixin audit: `./gradlew -q --init-script porting-tools/cp.init.gradle printCompileClasspath -DcpOutDir=<dir>`, then
`MIXIN_MODULES=. perl porting-tools/check_mixin_targets.pl . $JAVA_HOME/bin/javap <dir>/cp-createaddition.txt`.

## Decisions

- **Base**: `1.21.1` NeoForge (Create 6), not `fabric-1.20.1` (Create Fabric 0.5 is much further from Create Fly).
- **Build**: the D&D / Steam 'n' Rails template (non-remap Loom 1.17, Java 25, Fabric API 0.155.3, Forge Config API
  Port 26.1.5). `compat/jei`, `compat/computercraft`, `compat/sable`, `compat/simulated`, `datagen` and the two
  Sable/Simulated mixins are excluded in `build.gradle` (`sourceSets.main.java.exclude`).
- **Registrate**: relocated Registrate shim + datagen stubs copied from D&D into `com.mrh0.createaddition.port.*`
  (D&D-only `CopperBlockSet`/`DyedBlockList` dropped). Datagen-only builder calls (`.blockstate`, `.loot`, `.addLayer`,
  `.tag`, `.setTooltipModifierFactory`) stripped with `porting-tools/strip_datagen_calls.pl`.
- **Energy**: Team Reborn Energy 5.0.0 (`teamreborn:energy`, jar-in-jar, mod id `team_reborn_energy`), but **bridged,
  not rewritten**: CC&A keeps NeoForge's int/simulate `IEnergyStorage` API internally (`port/neoforge/energy`, same
  signatures as NeoForge's `IEnergyStorage`/`EnergyStorage`) so the wire graph, connectors and machines are unchanged.
  `TREnergy.toTR` exposes an internal storage to TR (a `SnapshotParticipant` tracks pending insert/extract amounts and
  applies them on the outermost commit, so nested aborts roll back); `TREnergy.fromTR` wraps another mod's storage
  (non-simulated calls open or nest a transaction and commit). The NeoForge capability calls are kept through a
  small shim (`port/neoforge/capabilities`: `Capabilities`, `RegisterCapabilitiesEvent`, `BlockCapabilityCache`):
  energy → `EnergyStorage.SIDED`/`ITEM`, items → `ItemStorage.SIDED` via Create Fly's `CachedInventoryBehaviour`,
  fluids → `FluidStorage.SIDED` via `CachedFluidInventoryBehaviour` (same as Create Fly's `AllTransfer`).
  `level.getCapability(cap, pos, side)` → `Capabilities.getCapability(level, cap, pos, side)`;
  `stack.getCapability(ITEM)` → `Capabilities.getCapability(stack, ITEM)` (in-place `SingleStackStorage`, so the
  caller's stack is updated). The capacitor stores energy in TR's `team_reborn_energy:energy` component and uses
  `SimpleEnergyItem.createStorage`. BlockApiCache has no invalidation listener; the cache re-queries each call.
- **Recipes**: Create Fly's single-ingredient record style, matching the converted JSON:
  `charging` = `{ingredient, results, energy, max_charge_rate}`, `rolling` = `{ingredient, results}`,
  `liquid_burning` = `{fluid_ingredient, burn_time, superheated?}` (Create Fly `FluidIngredient`, droplets). Recipe
  types/serializers are plain registry fields (`CARecipes`). Charging is a sequenced-assembly step: Create Fly's
  sequences are plain recipes, the step name is registered with `AllAssemblyRecipeNames` (`$ingredient`/`$result`
  placeholders are filled by Create Fly). `has_fluid_tag` condition dropped (JSON uses `fabric:tags_populated`).
  `compacting/seed_oil` removed from data (Create Fly's compacting has no fluid results).
- **Fluids**: 81 droplets per mB. Liquid blaze burner tank is a Create Fly `FluidTank` (`markDirty` = change callback);
  its burn threshold (100 mB) and recipe lookups are scaled. Seed oil / bioethanol use the port Registrate's Fabric
  fluid builder (viscosity/density dropped).
- **Client/server split (Create Fly)**: goggle tooltips, ambient audio and value boxes are client behaviours
  registered per block entity type (`client/CABlockEntityBehaviours`, like Create Fly's `AllBlockEntityBehaviours`).
  Block entities keep `addToGoggleTooltip`/`tickAudio` and implement Create Fly's (client package)
  `IHaveGoggleInformation`; `CATooltipBehaviour` / `CAKineticTooltipBehaviour` forward to them. Scroll values are
  `Server*ScrollValueBehaviour`s on the block entity. Verify at dedicated server boot that the client-package
  interfaces load (they reference no `net.minecraft.client` classes).
- **Servo motor**: four value settings. Server: distinct BehaviourTypes/NBT keys/netIds, routed by
  `IMultiValueSettings` + `mixin/AllHandleMixin` (Create Fly only routes to `ServerScrollValueBehaviour.TYPE`). Client:
  only the speed box for now (Create Fly's client handles one `ScrollValueBehaviour.TYPE` per block entity; see TODO).
- **Create Fly gaps**: `PumpBlockEntity` hardwires its type → `port/create/BETypeOverride` + `mixin/port/
  SmartBlockEntityMixin` (from D&D). Rolling mill = Create Fly millstone pattern (one 10-slot `SidedItemInventory`).
- **Sable/Simulated**: `CreateAddition.SABLE_ACTIVE`/`SIM_ACTIVE` are always false; `port/sable/SableUtil` gives the
  non-Sable answers to the few call sites outside the compat package.
- **Saved data**: the wire graph is a codec-based `SavedDataType` (one per level, since it needs the level; file name
  kept as `createaddition_wires.dat`). BE NBT goes through `port/minecraft/CAValueIO` (raw tags via `ExtraCodecs.NBT`,
  layout unchanged). The pre-1.21 `node0..` wire NBT conversion is not ported.
- **Dropped**: render layer registration (26.1 takes them from the quads), `CommonConfig.loadConfig` (Forge Config API
  Port loads the registered spec), manual damage type bootstrap (data JSON).

## Port mechanics (how things map)

| 1.21.1 NeoForge / Create 6 | 26.1.2 Fabric / Create Fly here |
|---|---|
| `@Mod` constructor, mod bus | `CreateAddition` (`ModInitializer`), `CreateAdditionClient` (`ClientModInitializer`) |
| `DeferredRegister` (effects, sounds) | `port/porting_lib/util/DeferredRegister` (registers immediately, returns a `DeferredHolder`) |
| `DeferredRegister` creative tab | `FabricCreativeModeTab` registered in `CreateAddition.registerCreativeTab` |
| `container.registerConfig` | Forge Config API Port `ConfigRegistry.INSTANCE.register` |
| `RegisterPayloadHandlersEvent` | `PayloadTypeRegistry.serverboundPlay/clientboundPlay` + `Server/ClientPlayNetworking` |
| `LevelTickEvent`, `ServerTickEvent`, `ChunkEvent.Load`, `LevelEvent.Unload` | `ServerTickEvents.START_LEVEL_TICK/START_SERVER_TICK`, `ServerChunkEvents.CHUNK_LOAD`, `ServerLevelEvents.UNLOAD` |
| `PlayerInteractEvent.RightClickBlock` | `UseBlockCallback` |
| `RegisterCommandsEvent` | `CommandRegistrationCallback` |
| `RegisterClientReloadListenersEvent` | `ResourceManagerHelper.get(CLIENT_RESOURCES).registerReloadListener` |
| `IEnergyStorage`, `Capabilities.*`, `RegisterCapabilitiesEvent` | `port/neoforge/*` shim over TR Energy / Fabric transfer (see Decisions) |
| `ItemStackHandler` / `RecipeWrapper` | vanilla `Container` (`SidedItemInventory`) / `SingleRecipeInput` |
| `FluidTank`, `SmartFluidTank`, `IFluidHandler`, `FluidActionResult` | Create Fly `FluidTank`/`FluidInventory` (droplets), `FluidHelper.getFluidInventory(stack)` |
| `InteractionResultHolder<ItemStack>` | `InteractionResult.SUCCESS.heldItemTransformedTo(stack)` |
| `level.getRecipeManager()` | `((ServerLevel) level).recipeAccess()` |
| `ProcessingRecipe`/`StandardProcessingRecipe` + params | records implementing `CreateSingleStackRollableRecipe` / `CreateRecipe` |
| `SavedData.Factory` | `SavedDataType` with a codec |
| `CompoundTag.getInt(k)` etc. | `getIntOr(k, 0)` etc. (`porting-tools/fix_optional.pl`); `getList(k, TAG)` → `getListOrEmpty(k)` |
| `read/write(CompoundTag, Provider, boolean)` | `read/write(ValueInput/ValueOutput, boolean)` (`fix_valueio.pl`, `fix_valueio_calls.pl`) |
| `AllBlocks.X.get()`, `AllItems.X.isIn(s)`, `AllBlocks.X.has(state)` | `AllBlocks.X`, `s.is(AllItems.X)`, `state.is(AllBlocks.X)` |
| `implements MovementBehaviour` | `extends MovementBehaviour` (`MovementBehaviour.REGISTRY.register(block, ...)`) |
| `BlockStressValues.setGeneratorSpeed(n, b)` (builder transform) | `b -> BlockStressValues.setGeneratorSpeed(b, n, b2)` |
| `DisplaySource.displaySource(entry)` | `CreateTransforms.displaySource(entry)` (lazy supplier overload) |
| `CreateBuiltInRegistries` | `CreateRegistries` |
| `Schedule.CONDITION_TYPES` (Supplier) | `AllSchedules.CONDITION_TYPES` (`Function<Identifier, ...>`) |
| `ScrollValueBehaviour` on the BE | `ServerScrollValueBehaviour` on the BE + client `ScrollValueBehaviour` via `CABlockEntityBehaviours` |
| `addToGoggleTooltip`/`tickAudio` overrides on kinetic BEs | client `CAKineticTooltipBehaviour` / `CAKineticAudioBehaviour` call the BE methods |
| `ResourceLocation`, `javax.annotation.Nullable` | `Identifier`, JetBrains `@Nullable` |

## Session log

### 2026-11-02
- Cloned fork, added `upstream`, created `26.1-fabric` from `1.21.1`.
- Resources converted by a parallel agent (commit `931d8fd8`): 48 item definitions, 127 recipes converted, 35 compat
  recipes for absent mods removed (ae2 kept, gated on `ae2`), `biomass_pellet` loses its 50 mB water by-product,
  `compacting/seed_oil` removed, `has_fluid_tag` → `fabric:tags_populated`, NeoForge conditions →
  `fabric:load_conditions`. That commit also carried the deletion of `src/main/templates/META-INF/neoforge.mods.toml`.
- Build migrated (D&D template + TR Energy), shims copied, 169 Create/Catnip/Flywheel imports mapped (157 by path,
  12 moved; `porting-tools/ca-create-map.tsv`, `ca-port-map.tsv`), datagen calls stripped. First compile: 1343 errors
  (symbol pass).
- Entry points, creative tab, configs, payloads, Fabric callbacks, recipes (Create Fly style), energy bridge,
  capability shim. Mechanical 26.1 passes with the CBC/S&R scripts (`fix_mc_renames*.pl`, `fix_block_methods.pl`,
  `fix_item_methods.pl`, `fix_valueio.pl`, `fix_optional.pl`) plus `fix_valueio_calls.pl` (new).
- Wire graph saved data, verifier chunk reads (ChunkPos/PalettedContainer/Strategy API), liquid blaze burner fluids,
  rolling mill, electric motor/servo/pump behaviours, client entrypoint and client behaviours.
- Midway: 110 errors in 49 files (symbol pass). Then: tesla coil (recipe lookup via `recipeAccess`, armor slots,
  `hurtServer`), accumulator (`preRemoveSideEffects` split like Create Fly's fluid tank, display source config UI
  to a client `SingleLineDisplaySourceRender`, batch-placement sound silencing dropped), PEI block entity, schedule
  condition split into data + client render (`EnergyThresholdConditionRender`, registered in Create Fly's
  `AllScheduleRenders.ALL`), 26.1 block/item signatures, fuels (`FuelValueEvents`), commands (permission checks,
  click/hover event records), `IOnLoad` + `ServerBlockEntityEvents.BLOCK_ENTITY_LOAD` for NeoForge's `onLoad`.
- Renderers on extract/submit: kinetic (motor, alternator, rolling mill, servo disc, pump partials), accumulator
  gauges, PEI (placed block), liquid blaze burner (Create Fly `BlazeBurnerRenderer.getBlazeBurnerRenderData` with
  the liquid hat), wires (`WireNodeRenderer`: wires extracted per connection, drawn with `submitCustomGeometry` in a
  QUADS leash pipeline `CARenderType.WIRE`; festive lights as SuperByteBuffer states; render bounds moved to the block
  entities' `getRenderBoundingBox`). Access widener: `BlockEntityRenderState.blockState`,
  `RenderPipelines.PIPELINES_BY_LOCATION`.
- **`compileJava` succeeds (0 errors); `./gradlew build -x test` produces the jar** (TR Energy in `META-INF/jars`).
  Duplicate lang (`src/main` vs `src/generated`): hand-crafted wins (`DuplicatesStrategy.EXCLUDE`, as upstream).
- Mixins retargeted: `FireBlockMixin` (NeoForge `canCatchFire`/`isFlammable` → `isValidFireLocation`/`canBurn`,
  `getFlammability` → `getBurnOdds`, `isFireSource` → infiniburn `BlockState.is(TagKey)` in `tick`),
  `FlowingFluidMixin` (`canSpreadTo` → `canPassThrough`, fluid is now the 2nd parameter; `doFireTick` →
  `fireSpreadRadiusAroundPlayer == 0`). New: `AllHandleMixin` (value settings routing), `port.SmartBlockEntityMixin`,
  `port.SpawnPlacementsAccessor`. `check_mixin_targets.pl` (classpath via S&R's `cp.init.gradle`
  `printCompileClasspath`): only false positives (commented-out `ContraptionMixin`, inherited `BlockState.is(TagKey)`).
  Handler parameters / injection points are not checked statically: verify at server boot / in game.
- Not yet run: dedicated server boot, client.
- **Dedicated server boots clean** (`../railways-test-server`: Create Fly, Steam 'n' Rails, D&D, CC&A; no CC&A errors or
  warnings, `/reload` clean, 7073 recipes). Data fixes: `create:copper_nugget` → `minecraft:copper_nugget` in the tuff
  crushing overrides; `channeling` charging result uses the 26.1 `stored_enchantments` map (no `levels` wrapper).
  Functional check: `/setblock` of every CC&A machine (motor, pump, servo, alternator, rolling mill, creative energy,
  connectors, relay, tesla coil, liquid blaze burner, accumulator, PEI, digital adapter, barbed wire) ticks without
  errors; block entity NBT saves as expected. `fabric:tags_populated` conditions parsed without errors.
- **Copycats+ bug found** (not CC&A): its `SyncedBlockEntityMixin.doNotSyncVirtual` does `instanceof
  VirtualRenderWorld` (a client class), so any block entity calling `notifyUpdate` on a dedicated server crashes
  (`NoClassDefFoundError: BlockAndTintGetter`). Hit by the liquid blaze burner; affects Create's own block entities
  too. The test above ran with the copycats jar moved to `../ca-test-disabled/`.

### 2026-11-04
- Client test reports: tesla coil not charging, servo not reacting to redstone. Reproduced on the dedicated server:
  - The test server pauses ticking after 60 s with no players (`pause-when-empty-seconds`), which froze both setups;
    set it to -1 while testing (restored to 60 afterwards).
  - **Energy was never saved or synced**: Create Fly's `SmartBlockEntity.write` does not go through `writeSafe`, and
    `AbstractElectricBlockEntity` only wrote energy in `writeSafe`. Now written in `write` too (tesla coil, and every
    other `AbstractElectricBlockEntity`).
  - Tesla coil: hanging from a ceiling is `facing=up` (the clicked face's opposite); `facing=down` is a floor coil, which
    only zaps. With `facing=up` over a depot: gold ingot → electrum ingot, capacitor charges through TR Energy.
  - Servo: works as upstream. Redstone goes on the **top or bottom** (relative to its facing/roll; top = max angle,
    bottom = min angle), it assembles on a non-zero signal, rotates to the target and, in the default "place when
    stopped" mode, places the contraption at the limit (checked with an oriented log: `axis=y` → `axis=x`). Only the
    block attached to the front moves unless glued (as any Create bearing).
- Servo client value boxes: `client/ServoValueBoxes`, one client ScrollValueBehaviour whose sided slot picks the
  setting from the looked-at side (back = speed, top = max angle, bottom = min angle, other two sides = movement
  mode, as in 1.21.1); settings sent with the matching netId. Untested in game.

### 2026-11-05
- In game: tesla coil works. The servo's movement mode box drew the option labels as overlapping text: the single
  composite box was not a `ScrollOptionBehaviour`, so Create Fly's renderer and board used the text style. Replaced
  by real per-side boxes: `ServoValueBoxes.Switcher` (client tick) puts the box for the looked-at side into the
  `ScrollValueBehaviour.TYPE` slot (`attachBehaviourLate`): back = `KineticScrollValueBehaviour` (Create's motor speed
  box), sides = `RotationModeScrollBehaviour` (the bearing's icon box) bound to the servo's mode, top/bottom = angle
  boxes (text, 0-180 in 5s, as 1.21.1).

## Workstreams (2026-11-05)

Run in parallel, each in its own git worktree and branch off `26.1-fabric`; the orchestrator merges them back. Each
writes its notes to `porting-notes/<topic>.md` (merged into this file afterwards) instead of editing this file.

| Topic | Branch | Worktree | Status |
|---|---|---|---|
| JEI compat | `port/jei` | `../createaddition-jei` | in progress |
| CC:Tweaked compat | `port/cc` | `../createaddition-cc` | merged (943e094a); in-game test pending |
| Sable / Simulated compat (Aeronautics) | `port/sable` | `../createaddition-sable` | in progress |
| Contraption rendering (PEI, liquid blaze burner), sandpaper animation, seed oil recipe | `port/misc` | `../createaddition-misc` | merged; in-game test pending |

To resume: check each branch's `git log` and `porting-notes/<topic>.md`; merge finished branches into `26.1-fabric`.

## TODO

- [x] Phase 1: build migration, shims copied, imports mapped, entrypoints/registration/configs/networking ported.
- [x] Phase 2: energy → TR Energy (bridge); capabilities → Fabric transfer API.
- [x] Phase 3: compile clean (block/BE APIs, renderers to extract/submit, mixin retargets). Build produces a jar.
- [x] Resources: item definitions, recipe/data conversion (done by the resources agent, `931d8fd8`).
- [ ] Restore the `seed_oil` recipe (Create Fly's compacting has no fluid output; needs a CC&A recipe type or a
      Create Fly change).
- [x] Servo motor client value boxes (ServoValueBoxes, per-side switcher); re-check the mode icons and editing in game.
- [ ] Rolling mill/electric pump: check the CC&A sound scapes and Create Fly audio behaviours together in game.
- [ ] Check `TREnergy` against other TR mods in game (insert/extract inside nested transactions).
- [ ] PEI and liquid blaze burner rendering on contraptions (was `renderInContraption`/`createVisual` on the movement
      behaviours): Create Fly `MovementRenderBehaviour` via `AllMovementRenders`. Check `PEIActorVisual` and the
      `*Visual` classes (compiled against Create Fly's Flywheel but untested).
- [ ] Diamond grit sandpaper: animated item renderer (was `IClientItemExtensions`); durability is fixed at the config
      default (1024) since durability is a component set at registration.
- [ ] Sable/Simulated: `SableUtil` stub, the sub-level wire/burner render paths and docking connector are gone until the
      compat pass.
- [ ] Verify at boot: client-package `IHaveGoggleInformation` on common block entities loads on a dedicated server;
      `FireBlockMixin`/`FlowingFluidMixin` handler locals (`@Local Direction`, trailing target args on
      `@ModifyExpressionValue`); `WireGraph` saved data (`createaddition_wires.dat`).
- [x] Dedicated server boot (`../railways-test-server`): clean, `/reload` clean, all machines placed and ticked.
- [x] Copycats+: fixed `SyncedBlockEntityMixin` in `../copycats` (7591de4d); jar restored to the test server, CC&A
      machines tick with it loaded.
- [ ] Client test (user launches the game).
- [ ] Compat second pass: JEI, CC:Tweaked, Sable/Simulated (Aeronautics).
