package com.mrh0.createaddition.trains.schedule.condition;

import com.mrh0.createaddition.blocks.portable_energy_interface.PortableEnergyManager;
import com.mrh0.createaddition.port.neoforge.energy.IEnergyStorage;
import com.zurrtum.create.content.trains.entity.Carriage;
import com.zurrtum.create.content.trains.entity.Train;
import com.zurrtum.create.content.trains.schedule.condition.CargoThresholdCondition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

/**
 * PORT-NOTE(26.1): data half only. Create Fly splits schedule entries into data (here) and a client
 * {@code IScheduleInput} render (titles, icons, configuration widgets): {@link EnergyThresholdConditionRender}.
 */
public class EnergyThresholdCondition extends CargoThresholdCondition {
    public EnergyThresholdCondition(Identifier id) {
        super(id);
    }

    @Override
    protected boolean test(Level level, Train train, CompoundTag context) {
        Ops operator = getOperator();
        int target = getThreshold();

        int foundEnergy = 0;
        for (Carriage carriage : train.carriages) {
            if (carriage.anyAvailableEntity() == null) continue;
            IEnergyStorage ies = PortableEnergyManager.get(carriage.anyAvailableEntity().getContraption());
            if (ies == null) continue;
            foundEnergy += ies.getEnergyStored();
        }

        requestStatusToUpdate(foundEnergy / 1000, context);
        return operator.test(foundEnergy, target * 1000);
    }

    @Override
    public MutableComponent getWaitingStatus(Level level, Train train, CompoundTag tag) {
        int lastDisplaySnapshot = getLastDisplaySnapshot(tag);
        if (lastDisplaySnapshot == -1)
            return Component.empty();
        int offset = getOperator() == Ops.LESS ? -1 : getOperator() == Ops.GREATER ? 1 : 0;
        return Component.translatable("create.schedule.condition.threshold.status", lastDisplaySnapshot,
                Math.max(0, getThreshold() + offset), Component.translatable("createaddition.schedule.condition.threshold.unit"));
    }
}
