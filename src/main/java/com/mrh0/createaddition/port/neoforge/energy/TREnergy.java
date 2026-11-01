package com.mrh0.createaddition.port.neoforge.energy;

import com.google.common.collect.MapMaker;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import org.jetbrains.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;

import java.util.Map;

/**
 * PORT-NOTE(26.1): bridges CC&A's NeoForge-style {@link IEnergyStorage} and Team Reborn Energy.
 * <ul>
 *     <li>{@link #toTR}: exposes an internal storage to other mods. Inserts/extracts are checked with a simulate call
 *     and applied when the outermost transaction commits (a {@link SnapshotParticipant} tracks the pending amounts, so
 *     nested aborts roll back correctly).</li>
 *     <li>{@link #fromTR}: wraps another mod's storage; a non-simulated call opens (or nests) a transaction and commits.</li>
 * </ul>
 */
public final class TREnergy {
    private static final Map<IEnergyStorage, EnergyStorage> TO_TR = new MapMaker().weakKeys().makeMap();
    private static final Map<EnergyStorage, IEnergyStorage> FROM_TR = new MapMaker().weakKeys().makeMap();

    private TREnergy() {
    }

    @Nullable
    public static EnergyStorage toTR(@Nullable IEnergyStorage storage) {
        if (storage == null) return null;
        if (storage instanceof FromTR from) return from.storage;
        return TO_TR.computeIfAbsent(storage, ToTR::new);
    }

    @Nullable
    public static IEnergyStorage fromTR(@Nullable EnergyStorage storage) {
        if (storage == null) return null;
        if (storage instanceof ToTR to) return to.storage;
        return FROM_TR.computeIfAbsent(storage, FromTR::new);
    }

    static Transaction open() {
        TransactionContext current = Transaction.getCurrentUnsafe();
        return current == null ? Transaction.openOuter() : current.openNested();
    }

    private static int clampInt(long value) {
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE, value));
    }

    static final class ToTR extends SnapshotParticipant<long[]> implements EnergyStorage {
        final IEnergyStorage storage;
        private long pendingIn;
        private long pendingOut;

        ToTR(IEnergyStorage storage) {
            this.storage = storage;
        }

        @Override
        public boolean supportsInsertion() {
            return storage.canReceive();
        }

        @Override
        public long insert(long maxAmount, TransactionContext transaction) {
            StoragePreconditions.notNegative(maxAmount);
            if (maxAmount == 0 || !storage.canReceive()) return 0;
            int canTotal = storage.receiveEnergy(clampInt(pendingIn + maxAmount), true);
            long accepted = Math.max(0, Math.min(maxAmount, canTotal - pendingIn));
            if (accepted > 0) {
                updateSnapshots(transaction);
                pendingIn += accepted;
            }
            return accepted;
        }

        @Override
        public boolean supportsExtraction() {
            return storage.canExtract();
        }

        @Override
        public long extract(long maxAmount, TransactionContext transaction) {
            StoragePreconditions.notNegative(maxAmount);
            if (maxAmount == 0 || !storage.canExtract()) return 0;
            int canTotal = storage.extractEnergy(clampInt(pendingOut + maxAmount), true);
            long extracted = Math.max(0, Math.min(maxAmount, canTotal - pendingOut));
            if (extracted > 0) {
                updateSnapshots(transaction);
                pendingOut += extracted;
            }
            return extracted;
        }

        @Override
        public long getAmount() {
            return Math.max(0, storage.getEnergyStored() + pendingIn - pendingOut);
        }

        @Override
        public long getCapacity() {
            return storage.getMaxEnergyStored();
        }

        @Override
        protected long[] createSnapshot() {
            return new long[]{pendingIn, pendingOut};
        }

        @Override
        protected void readSnapshot(long[] snapshot) {
            pendingIn = snapshot[0];
            pendingOut = snapshot[1];
        }

        @Override
        protected void onFinalCommit() {
            long in = pendingIn, out = pendingOut;
            pendingIn = 0;
            pendingOut = 0;
            if (in > 0) storage.receiveEnergy(clampInt(in), false);
            if (out > 0) storage.extractEnergy(clampInt(out), false);
        }
    }

    static final class FromTR implements IEnergyStorage {
        final EnergyStorage storage;

        FromTR(EnergyStorage storage) {
            this.storage = storage;
        }

        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            if (toReceive <= 0) return 0;
            try (Transaction transaction = open()) {
                long inserted = storage.insert(toReceive, transaction);
                if (!simulate) transaction.commit();
                return clampInt(inserted);
            }
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            if (toExtract <= 0) return 0;
            try (Transaction transaction = open()) {
                long extracted = storage.extract(toExtract, transaction);
                if (!simulate) transaction.commit();
                return clampInt(extracted);
            }
        }

        @Override
        public int getEnergyStored() {
            return clampInt(storage.getAmount());
        }

        @Override
        public int getMaxEnergyStored() {
            return clampInt(storage.getCapacity());
        }

        @Override
        public boolean canExtract() {
            return storage.supportsExtraction();
        }

        @Override
        public boolean canReceive() {
            return storage.supportsInsertion();
        }
    }
}
