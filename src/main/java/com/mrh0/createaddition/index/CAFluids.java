package com.mrh0.createaddition.index;

import com.mrh0.createaddition.CreateAddition;
import com.mrh0.createaddition.port.registrate.fabric.SimpleFlowableFluid;
import com.mrh0.createaddition.port.registrate.util.entry.FluidEntry;

/**
 * PORT-NOTE(26.1): NeoForge's {@code BaseFlowingFluid}/{@code FluidType} are replaced by the port Registrate's Fabric
 * fluid builder (as CBC's port does). Viscosity/density have no Fabric equivalent; the "no tint" fluid type is the
 * builder's default untinted renderer.
 */
public class CAFluids {
	static {
		CreateAddition.REGISTRATE.setCreativeTab(CreateAddition.MAIN_TAB);
	}

	public static FluidEntry<SimpleFlowableFluid.Flowing> SEED_OIL;
	public static FluidEntry<SimpleFlowableFluid.Flowing> BIOETHANOL;

	private static FluidEntry<SimpleFlowableFluid.Flowing> fluid(String name) {
		return CreateAddition.REGISTRATE.fluid(name, CreateAddition.asResource("fluid/" + name + "_still"), CreateAddition.asResource("fluid/" + name + "_flow"))
				.fluidProperties(p -> p.levelDecreasePerBlock(2)
						.tickRate(15)
						.flowSpeed(6)
						.blastResistance(100f))
				.source(SimpleFlowableFluid.Source::new)
				.bucket()
				.properties(p -> p.stacksTo(1))
				.build()
				.register();
	}

	public static void register() {
		SEED_OIL = fluid("seed_oil");
		BIOETHANOL = fluid("bioethanol");
	}
}
