package com.mrh0.createaddition.energy;

import net.minecraft.core.Direction;
import com.mrh0.createaddition.port.neoforge.energy.IEnergyStorage;

import org.jetbrains.annotations.Nullable;

public interface IEnergyProvider {
    IEnergyStorage getEnergyStorage(@Nullable Direction direction);
}
