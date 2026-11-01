package com.mrh0.createaddition.port.neoforge.capabilities;

import com.mrh0.createaddition.port.neoforge.energy.IEnergyStorage;
import com.mrh0.createaddition.port.neoforge.energy.TREnergy;
import com.zurrtum.create.AllTransfer;
import com.zurrtum.create.infrastructure.fluids.FluidInventory;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.base.SingleStackStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * PORT-NOTE(26.1): stand-in for NeoForge's capability constants. Energy goes through Team Reborn Energy
 * ({@link TREnergy}); items and fluids through Create Fly's Fabric transfer wrappers ({@link AllTransfer}).
 * Lookups are {@link #getCapability} instead of {@code level.getCapability} / {@code stack.getCapability}.
 */
public final class Capabilities {
    private Capabilities() {
    }

    public static final class EnergyStorage {
        public static final BlockCapability<IEnergyStorage> BLOCK = new BlockCapability<>("energy");
        public static final ItemCapability<IEnergyStorage> ITEM = new ItemCapability<>("energy");
    }

    public static final class ItemHandler {
        public static final BlockCapability<Container> BLOCK = new BlockCapability<>("item");
    }

    public static final class FluidHandler {
        public static final BlockCapability<FluidInventory> BLOCK = new BlockCapability<>("fluid");
    }

    public record BlockCapability<T>(String name) {
    }

    public record ItemCapability<T>(String name) {
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> T getCapability(Level level, BlockCapability<T> capability, BlockPos pos, @Nullable Direction side) {
        if (capability == EnergyStorage.BLOCK)
            return (T) TREnergy.fromTR(team.reborn.energy.api.EnergyStorage.SIDED.find(level, pos, side));
        if (capability == ItemHandler.BLOCK)
            return (T) AllTransfer.getInventory(level, pos, null, null, side);
        if (capability == FluidHandler.BLOCK)
            return (T) AllTransfer.getFluidInventory(level, pos, null, null, side);
        throw new IllegalArgumentException("Unknown capability " + capability);
    }

    /** Energy storage of a stack, writing changes back into that same stack object. */
    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> T getCapability(ItemStack stack, ItemCapability<T> capability) {
        if (capability != EnergyStorage.ITEM) throw new IllegalArgumentException("Unknown capability " + capability);
        if (stack.isEmpty()) return null;
        return (T) TREnergy.fromTR(team.reborn.energy.api.EnergyStorage.ITEM.find(stack, ContainerItemContext.ofSingleSlot(new InPlaceStackStorage(stack))));
    }

    /** A single "slot" backed by a stack that callers hold on to: committed changes are copied into it. */
    private static final class InPlaceStackStorage extends SingleStackStorage {
        private final ItemStack stack;

        InPlaceStackStorage(ItemStack stack) {
            this.stack = stack;
        }

        @Override
        protected ItemStack getStack() {
            return stack;
        }

        @Override
        protected void setStack(ItemStack newStack) {
            if (newStack == stack) return;
            List<DataComponentType<?>> stale = new ArrayList<>();
            for (var entry : stack.getComponentsPatch().entrySet()) {
                if (newStack.getComponentsPatch().get(entry.getKey()) == null) stale.add(entry.getKey());
            }
            stale.forEach(stack::remove);
            stack.applyComponents(newStack.getComponentsPatch());
            stack.setCount(newStack.getCount());
        }
    }
}
