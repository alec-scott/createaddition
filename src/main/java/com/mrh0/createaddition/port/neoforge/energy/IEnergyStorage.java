package com.mrh0.createaddition.port.neoforge.energy;

/**
 * PORT-NOTE(26.1): NeoForge's int-based, simulate-flag energy API, kept as CC&A's internal energy interface so the wire
 * graph and machines are unchanged. Team Reborn Energy (the Fabric API other mods use) is bridged at the block/item
 * boundary by {@link TREnergy}.
 */
public interface IEnergyStorage {
    int receiveEnergy(int toReceive, boolean simulate);

    int extractEnergy(int toExtract, boolean simulate);

    int getEnergyStored();

    int getMaxEnergyStored();

    boolean canExtract();

    boolean canReceive();
}
