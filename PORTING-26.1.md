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
- End of session: 110 errors in 49 files (symbol pass; attribution of method bodies will add more, as in S&R).
  Remaining: renderers/visuals (extract/submit; `SafeBlockEntityRenderer`, `RenderType`, `LightTexture`,
  `ContraptionMatrices`), ponder scenes/`CAPonders` (FMLEnvironment/DatagenModLoader), tesla coil (belt processing,
  item charging), PEI movement + contraption storage, modular accumulator (CT, connectivity, display source),
  `EnergyThresholdCondition` (Create Fly schedule API), `DiamondGritSandpaperItem` (sandpaper renderer is client),
  `WireSpool`, the four mixins (`FlowingFluidMixin`, `FireBlockMixin`, `BaseFireBlockMixin`, retargets), commands
  (`hasPermission`), `CASoundScapes`.

## TODO

- [x] Phase 1: build migration, shims copied, imports mapped, entrypoints/registration/configs/networking ported.
- [x] Phase 2: energy → TR Energy (bridge); capabilities → Fabric transfer API.
- [ ] Phase 3: compile clean (block/BE APIs, renderers to extract/submit, mixin retargets).
- [x] Resources: item definitions, recipe/data conversion (done by the resources agent, `931d8fd8`).
- [ ] Restore the `seed_oil` recipe (Create Fly's compacting has no fluid output; needs a CC&A recipe type or a
      Create Fly change).
- [ ] Verify the `fabric:tags_populated` field names (`registry`/`values`) at server boot.
- [ ] Servo motor client value boxes for movement mode and min/max angle (client mixin into Create Fly's
      `ValueSettingsInputHandler`/`ScrollValueRenderer` to iterate all value behaviours, or one composite behaviour).
- [ ] Rolling mill/electric pump: check the CC&A sound scapes and Create Fly audio behaviours together in game.
- [ ] Check `TREnergy` against other TR mods in game (insert/extract inside nested transactions).
- [ ] Dedicated server boot (`../railways-test-server`).
- [ ] Client test (user launches the game).
- [ ] Compat second pass: JEI, CC:Tweaked, Sable/Simulated (Aeronautics).
