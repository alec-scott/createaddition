# CC:Tweaked port notes

- Dependency: `compileOnly maven.modrinth:cc-tweaked:D9kw3xjO` (1.120.0), Modrinth maven added to `build.gradle`. Not bundled; the mod runs without CC.
- Registration: `CACapabilities.register()` calls `compat.computercraft.Peripherals.registerPeripheralCapabilities()` only if `CreateAddition.CC_ACTIVE` (`isModLoaded("computercraft")`), so no CC classes load otherwise. Uses `PeripheralLookup.get().registerForBlockEntity(...)` (Fabric API lookup).
- Lua methods unchanged. Only change in peripherals: Create elevator imports moved to `com.zurrtum.create`.
- Package un-excluded in `build.gradle`.
- Not ported / unchanged: the commented-out per-BlockEntity `LazyOptional` peripheral fields and `EnergyMeterBlockEntity` (energy meter is not a peripheral; file is legacy). `DigitalAdapterBlockItem` creative-tab gating is still a commented TODO (recipe is gated by `computercraft` load condition).
- Check in game: with CC loaded, `peripheral.wrap` on electric motor, servo motor, modular accumulator, PEI, redstone relay, digital adapter; digital adapter elevator and bearing methods; game starts without CC.
