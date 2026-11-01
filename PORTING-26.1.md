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

## Decisions

- **Base**: `1.21.1` NeoForge (Create 6), not `fabric-1.20.1` (Create Fabric 0.5 is much further from Create Fly).
- **Build**: the D&D / Steam 'n' Rails template (non-remap Loom 1.17, Java 25, Fabric API 0.155.3, Forge Config API
  Port 26.1.5).
- **Registrate**: relocated Registrate shim + datagen stubs copied from D&D into `com.mrh0.createaddition.port.*`.
- **Energy**: NeoForge `IEnergyStorage` / `Capabilities.EnergyStorage` → **Team Reborn Energy 5.0.0**
  (`teamreborn:energy`, maven.fabricmc.net, requires MC >= 26.1, jar-in-jar). Same API the upstream Fabric branch used.
- **Compat, first pass excluded**: JEI (`compat/jei`), ComputerCraft (`compat/computercraft`), Sable / Simulated
  (`compat/sable`, `compat/simulated`, mixins `DockingConnectorBEMixin`, `VanillaSubLevelBERenderMixin`). All have 26.1
  targets available later: JEI 29.47.0.110 and CC:Tweaked 1.120.0 (Modrinth, 26.1.2 Fabric); Sable is ported in
  `../create-aeronautics-fabric`.

## Port mechanics (how things map)

(fill in as discovered; see D&D's table for the shared ones)

## Session log

### 2026-11-02
- Cloned fork, added `upstream`, created `26.1-fabric` from `1.21.1`.

## TODO

- [ ] Phase 1: build migration, shims copied, imports mapped, entrypoints/registration/configs/networking ported.
- [ ] Phase 2: energy → TR Energy; capabilities → Fabric transfer API.
- [ ] Phase 3: compile clean (block/BE APIs, renderers to extract/submit, mixin retargets).
- [ ] Resources: item definitions, recipe/data conversion (`porting-tools/`), CT sheets if any.
- [ ] Dedicated server boot (`../railways-test-server`).
- [ ] Client test (user launches the game).
- [ ] Compat second pass: JEI, CC:Tweaked, Sable/Simulated (Aeronautics).
