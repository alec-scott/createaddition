package com.mrh0.createaddition.blocks.creative_energy;

import com.mrh0.createaddition.energy.CreativeEnergyStorage;

import com.mrh0.createaddition.index.CABlockEntities;
import com.zurrtum.create.content.logistics.crate.CrateBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import com.mrh0.createaddition.port.neoforge.capabilities.BlockCapabilityCache;
import com.mrh0.createaddition.port.neoforge.capabilities.Capabilities;
import com.mrh0.createaddition.port.neoforge.capabilities.RegisterCapabilitiesEvent;
import com.mrh0.createaddition.port.neoforge.energy.IEnergyStorage;

import java.util.EnumMap;
import java.util.EnumSet;

public class CreativeEnergyBlockEntity extends CrateBlockEntity {

	protected final CreativeEnergyStorage capability;

	private final EnumSet<Direction> invalidSides = EnumSet.allOf(Direction.class);
	private final EnumMap<Direction, BlockCapabilityCache<IEnergyStorage, Direction>> cache = new EnumMap<>(Direction.class);
	
	public CreativeEnergyBlockEntity(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
		super(tileEntityTypeIn, pos, state);
		capability = new CreativeEnergyStorage();
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(
				Capabilities.EnergyStorage.BLOCK,
				CABlockEntities.CREATIVE_ENERGY.get(),
				(be, context) -> be.capability
		);
	}
	
	private boolean firstTickState = true;
	
	@Override
	public void tick() {
		super.tick();
		if (level == null) return;
		if (level.isClientSide()) return;
		if (firstTickState) firstTick();
		firstTickState = false;
		
		for (Direction d : Direction.values()) {
			IEnergyStorage ies = cache.get(d).getCapability();
			if (ies == null) continue;
			ies.receiveEnergy(Integer.MAX_VALUE, false);
		}
	}
	
	public void firstTick() {
		updateCache();
	}
	
	public void updateCache() {
		if (level == null) return;
		if (level.isClientSide()) return;
		for (Direction side : Direction.values()) {
			cache.put(side, BlockCapabilityCache.create(
				Capabilities.EnergyStorage.BLOCK,
				(ServerLevel) level,
				getBlockPos().relative(side),
				side.getOpposite(),
				() -> !this.isRemoved(),
				() -> invalidSides.add(side)
			));
		}
	}
}
