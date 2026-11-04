package com.mrh0.createaddition.energy;

import net.minecraft.world.level.storage.ValueOutput;

import net.minecraft.world.level.storage.ValueInput;

import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import com.mrh0.createaddition.port.neoforge.capabilities.BlockCapabilityCache;
import com.mrh0.createaddition.port.neoforge.capabilities.Capabilities;
import com.mrh0.createaddition.port.neoforge.energy.IEnergyStorage;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;

public abstract class AbstractElectricBlockEntity extends SmartBlockEntity {

	protected final InternalEnergyStorage localEnergy;

	private final EnumSet<Direction> invalidSides = EnumSet.allOf(Direction.class);
	private final EnumMap<Direction, BlockCapabilityCache<IEnergyStorage, Direction>> escacheMap = new EnumMap<>(Direction.class);

	public AbstractElectricBlockEntity(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
		super(tileEntityTypeIn, pos, state);
		localEnergy = new InternalEnergyStorage(getCapacity(), getMaxIn(), getMaxOut());
		setLazyTickRate(20);
	}

	public abstract int getCapacity();
	public abstract int getMaxIn();
	public abstract int getMaxOut();

	@Override
	public void addBehaviours(List<BlockEntityBehaviour<?>> behaviours) {}

	public abstract boolean isEnergyInput(Direction side);
	public abstract boolean isEnergyOutput(Direction side);

	@Override
	protected void read(ValueInput tag, boolean clientPacket) {
		super.read(tag, clientPacket);
		localEnergy.read(tag);
	}

	// PORT-NOTE(26.1): Create Fly's write() does not go through writeSafe, so the energy was never saved or synced
	@Override
	protected void write(ValueOutput tag, boolean clientPacket) {
		super.write(tag, clientPacket);
		localEnergy.write(tag);
	}

	@Override
	public void writeSafe(ValueOutput tag) {
		super.writeSafe(tag);
		localEnergy.write(tag);
	}

	@Deprecated
	public void outputTick(int max) {
		for(Direction side : Direction.values()) {
			if(!isEnergyOutput(side))
				continue;
			localEnergy.outputToSide(level, worldPosition, side, max);
		}
	}

	@Override
	public void tick() {
		super.tick();
		if(!invalidSides.isEmpty()) {
			invalidSides.forEach(this::updateCache);
			invalidSides.clear();
		}
	}

	public void updateCache(Direction side) {
		if (level == null) return;
		if (level.isClientSide()) return;
		if (!level.isLoaded(worldPosition.relative(side))) {
			escacheMap.put(side, null);
			return;
		}
		/*
		BlockEntity te = level.getBlockEntity(worldPosition.relative(side));
		if(te == null) {
			setCache(side, LazyOptional.empty());
			return;
		}
		LazyOptional<IEnergyStorage> le = te.getCapability(ForgeCapabilities.ENERGY, side.getOpposite());
		if(ignoreCapSide() && !le.isPresent()) le = te.getCapability(ForgeCapabilities.ENERGY);
		// Make sure the side isn't already cached.
		if (le.equals(getCachedEnergy(side))) return;
		setCache(side, le);
		le.addListener((es) -> invalidCache(side));
		*/

		var cache = BlockCapabilityCache.create(
				Capabilities.EnergyStorage.BLOCK, // capability to cache
				(ServerLevel) level, // level
				getBlockPos().relative(side),
				side.getOpposite(),
				() -> !this.isRemoved(), // validity check (because the cache might outlive the object it belongs to)
				() -> { invalidSides.add(side); } // invalidation listener
		);
		escacheMap.put(side, cache);
	}
}
