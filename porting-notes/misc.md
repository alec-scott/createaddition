# Misc workstream (branch `port/misc`)

## 1. Contraption rendering

Create Fly attaches client rendering to a `MovementBehaviour` through `behaviour.attachRender` (a
`MovementRenderBehaviour`, set by `AllMovementRenders` for Create's own blocks). `client/CAMovementRenders` does the
same for CC&A, called from `CreateAdditionClient`.

- **PEI**: `PortableEnergyInterfaceMovementRender` is a copy of Create Fly's `PortableStorageInterfaceMovementRender`
  (`createVisual` -> the existing `PEIActorVisual` / `PEIInstance`; `getRenderState` = non-Flywheel fallback, same
  animation offsets, partials from `PortableEnergyInterfaceRenderer`). The movement behaviour now returns
  `disableBlockEntityRendering() == true` like the PSI, otherwise the virtual block entity would be drawn too. The
  connect/disconnect animation is the same `LerpedFloat` already driven by `PortableEnergyInterfaceMovement.tick`.
- **Liquid blaze burner**: upstream never registered a movement behaviour for it (`renderInContraption` in the
  renderer was dead code; the virtual block entity was drawn by the block entity renderer / visual). Now there is a
  `LiquidBlazeBurnerMovement` (registered in `CABlocks`, hides the block entity, client tick through a static hook
  because the class is common) and `LiquidBlazeBurnerMovementRender` (copy of Create Fly's
  `BlazeBurnerMovementRenderBehaviour` minus the train conductor logic): head turns towards the camera, goggles from the
  block entity data (`Goggles`), liquid hat, rods, smoke particles when standing still. No flame (as before, and as
  Create's burner on contraptions).
- `LiquidBlazeBurnerVisual` and `PEIVisual` (placed blocks) are untouched.
- Untested in game.

## 2. Diamond grit sandpaper

- Animation: Create Fly implements it as an item model type, `create:model/sand_paper` (`SandPaperModel`, registered in
  `AllModels`). `items/diamond_grit_sandpaper.json` now uses it with `createaddition:item/diamond_grit_sandpaper`.
  The item class extends Create Fly's `SandPaperItem` (use, polishing component, sounds).
- Durability: `CAItems` now uses `p.durability(CommonConfig.DIAMOND_GRIT_SANDPAPER_USES.get())`. The earlier note that
  the config is not loaded at registration is wrong: Forge Config API Port opens COMMON configs immediately in
  `ConfigRegistry.register`, which `CreateAddition.onInitialize` calls before the items are registered. A config
  change needs a game restart (it is a component default, not a per-use lookup), and already existing stacks keep the
  value they were created with only through the item default, i.e. they follow the config too.
- Not covered: Create's sandpaper tag/recipe interactions beyond what `SandPaperItem` does; check the polishing
  animation of the held item.

## 3. Seed oil

Create Fly's `create:compacting` has no fluid results (`results` is 1-4 item outputs); `create:mixing` has
`fluid_results` (`FluidStack`, droplets). Restored as `recipe/mixing/seed_oil.json`: `#c:seeds` -> 8100 droplets
(100 mB) of `createaddition:seed_oil`. Differences from upstream: needs the mechanical mixer (not the press), no heat
requirement, default processing time 100 ticks.
