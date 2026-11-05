# JEI integration (port/jei)

Status: compiles, `./gradlew build -x test` green. Not run in game.

## Wiring
- `maven.modrinth:jei:29.47.0.109` is `compileOnly` (Modrinth maven, `jei_version` in gradle.properties); not bundled.
- `fabric.mod.json`: `jei_mod_plugin` entrypoint `compat.jei.CreateAdditionJEI`, and `suggests: jei`. JEI is the only
  thing that loads the plugin class, so the mod runs without JEI.
- `compat/jei` is no longer excluded in build.gradle.

## Design (mirrors Create Fly's JeiClientPlugin)
- Categories are `CreateCategory<RecipeHolder<T>>` for `charging`, `rolling`, `liquid_burning`; recipe types are
  `IRecipeType.create(createaddition:<name>, RecipeHolder.class)`; recipes come from `Internal.getClientSyncedRecipes()`.
  Layouts/offsets are the old ones. Catalysts: tesla coil, rolling mill, blaze burner, and diamond grit sandpaper added
  as a crafting station of Create Fly's `sandpaper_polishing` type.
- Fluid slot uses Create Fly's `CreateCategory.addFluidSlot(FluidIngredient)` (droplets, no 81x conversion needed).
- Sequenced assembly: Create Fly has no sub-category API, only `SequencedAssemblyCategory.registerRenderer(RecipeType,
  SequencedRenderer)`. `ChargingAssemblyRenderer` (static tesla coil) and `RollingMillAssemblyRenderer` (animated mill)
  are registered for the charging/rolling recipe types; no ingredient slot is added (`addSlot` returns null). Step
  names come from `AllAssemblyRecipeNames`; added the missing registration for `rolling` (`RollingRecipe.getDescriptionForAssembly`,
  key `createaddition.recipe.rolling.sequence`) in `CARecipes.register()`.
- Animated rolling mill: `AnimatedKinetics` no longer exists. New picture-in-picture `RollingMillRenderState` /
  `RollingMillRenderer` (modelled on Create Fly's MillstoneRenderer; two counter-rotating `AllPartialModels.SHAFT` along
  Z plus the mill block), registered through a client-only mixin `mixin/client/GuiRendererMixin` (same WrapOperation hook
  as Create Fly's) in new `createaddition.client.mixins.json` (environment client). Tesla coil is static, drawn through
  `GuiGameElement.of(blockstate)` (cached block texture, no mixin needed).
- Liquid burning blaze burner: Create Fly's `BasinBlazeBurnerRenderState` (burner + animated flame).

## Dropped
- `@JeiPlugin` annotation/`CARecipeCategory`/`AnimatedTeslaCoil`/`AnimatedRollingMill`/sub-category classes (Create
  Fly's API differs). Old `ingredientManager` field unused. Old sandpaper catalyst lookup by `getRecipeType` replaced
  by the direct type constant.
- The 26.1 `GuiGraphicsExtractor.minecraft` field is private here, so text uses `ClientMinecraftWrapper.getFont()`.

## Check in game
- Rolling/charging/liquid burning categories appear with correct titles, icons, catalysts, and recipes (also via
  uses/recipes lookup from the tesla coil, rolling mill, blaze burner, diamond grit sandpaper).
- Rolling mill animation: shafts spin in opposite directions, shaft orientation (Z) and mill model placement/size, and
  that nothing is clipped (box 40x40 px at 22 px/block). Mixin applied (log for GuiRendererMixin errors).
- Tesla coil orientation and size in the charging category (rotation `-22.5, 22.5`, FACING DOWN) and in the sequenced
  assembly step (FACING UP, 18 px block); mill in assembly step (scale 0.6). All offsets are estimates.
- Liquid burning layout: burner position/light under it, heat bar and heat text at y=30/36, burn time text, bucket
  slot cycling through fluids, fluid tooltip amounts.
- A sequenced assembly recipe using charging/rolling steps shows step names ("Charge N with Tesla Coil", "Rolling in Mill").
- Starting the game without JEI still works (client mixin loads without JEI).
