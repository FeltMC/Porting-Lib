package io.github.fabricators_of_create.porting_lib.transfer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.github.fabricators_of_create.porting_lib.util.FluidStack;
import io.github.fabricators_of_create.porting_lib.util.LazyOptional;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Utilities for transferring things.
 * for all storage getters, if a direction is not provided,
 * a CombinedStorage of all sides will be returned. All
 * may return null.
 */
public class TransferUtil {
	public static Transaction getTransaction() {
		TransactionContext open = Transaction.getCurrentUnsafe();
		if (open != null) {
			return open.openNested();
		}
		return Transaction.openOuter();
	}

	public static Storage<ItemVariant> getItemStorage(BlockEntity be) {
		return getItemStorage(be, null);
	}

	public static Storage<ItemVariant> getItemStorage(Level level, BlockPos pos) {
		return getItemStorage(level, pos, null);
	}

	public static Storage<ItemVariant> getItemStorage(Level level, BlockPos pos, @Nullable Direction direction) {
		BlockEntity be = level.getBlockEntity(pos);
		if (be == null) return null;
		return getItemStorage(be, direction);
	}

	public static Storage<ItemVariant> getItemStorage(BlockEntity be, @Nullable Direction side) {
		// client handling
		if (Objects.requireNonNull(be.getLevel()).isClientSide()) {
			return null;
		}
		// external handling
		List<Storage<ItemVariant>> itemStorages = new ArrayList<>();
		Level l = be.getLevel();
		BlockPos pos = be.getBlockPos();
		BlockState state = be.getBlockState();

		for (Direction direction : getDirections(side)) {
			Storage<ItemVariant> itemStorage = ItemStorage.SIDED.find(l, pos, state, be, direction);

			if (itemStorage != null) {
				if (itemStorages.size() == 0) {
					itemStorages.add(itemStorage);
					continue;
				}

				for (Storage<ItemVariant> storage : itemStorages) {
					if (!Objects.equals(itemStorage, storage)) {
						itemStorages.add(itemStorage);
						break;
					}
				}
			}
		}


		if (itemStorages.isEmpty()) return null;
		if (itemStorages.size() == 1) return itemStorages.get(0);
		return new CombinedStorage<>(itemStorages);
	}

	// Fluids

	public static Storage<FluidVariant> getFluidStorage(Level level, BlockPos pos) {
		BlockEntity be = level.getBlockEntity(pos);
		if (be == null) return null;
		return getFluidStorage(be);
	}

	public static Storage<FluidVariant> getFluidStorage(BlockEntity be) {
		return getFluidStorage(be, null);
	}

	public static Storage<FluidVariant> getFluidStorage(BlockEntity be, @Nullable Direction side) {
		boolean client = Objects.requireNonNull(be.getLevel()).isClientSide();
		// client handling
		if (client) { // TODO CLIENT TRANSFER
//			IFluidStorage cached = FluidTileDataStorage.getCachedStorage(be);
//			return LazyOptional.ofObject(cached);
			return null;
		}
		// external handling
		List<Storage<FluidVariant>> fluidStorages = new ArrayList<>();
		Level l = be.getLevel();
		BlockPos pos = be.getBlockPos();
		BlockState state = be.getBlockState();

		for (Direction direction : getDirections(side)) {
			Storage<FluidVariant> fluidStorage = FluidStorage.SIDED.find(l, pos, state, be, direction);

			if (fluidStorage != null) {
				if (fluidStorages.size() == 0) {
					fluidStorages.add(fluidStorage);
					continue;
				}

				for (Storage<FluidVariant> storage : fluidStorages) {
					if (!Objects.equals(fluidStorage, storage)) {
						fluidStorages.add(fluidStorage);
						break;
					}
				}
			}
		}

		if (fluidStorages.isEmpty()) return null;
		if (fluidStorages.size() == 1) return fluidStorages.get(0);
		return new CombinedStorage<>(fluidStorages);
	}

	private static Direction[] getDirections(@Nullable Direction direction) {
		if (direction == null) return Direction.values();
		return new Direction[] { direction };
	}

	public static Optional<FluidStack> getFluidContained(ItemStack container) {
		if (container != null && !container.isEmpty()) {
			Storage<FluidVariant> storage = FluidStorage.ITEM.find(container, ContainerItemContext.withInitial(container));
			if (storage != null) {
				FluidStack first = getFirstFluid(storage);
				if (first != null) return Optional.of(first);
			}
		}
		return Optional.empty();
	}

	public static <T> long firstCapacity(Storage<T> storage) {
		List<Long> capacities = capacities(storage, 1);
		return capacities.size() > 0 ? capacities.get(0) : 0;
	}

	public static <T> long totalCapacity(Storage<T> storage) {
		long total = 0;
		List<Long> capacities = capacities(storage, Integer.MAX_VALUE);
		for (Long l : capacities) total += l;
		return total;
	}

	/**
	 * Finds the capacities of each StorageView in the storage.
	 * @param cutoff number of capacities to find before exiting early
	 */
	public static <T> List<Long> capacities(Storage<T> storage, int cutoff) {
		List<Long> capacities = new ArrayList<>();
		try (Transaction t = getTransaction()) {
			for (StorageView<T> view : storage.iterable(t)) {
				capacities.add(view.getCapacity());
				if (capacities.size() == cutoff)
					break;
			}
		}
		return capacities;
	}

	public static FluidStack firstCopyOrEmpty(Storage<FluidVariant> storage) {
		return firstOrEmpty(storage).copy();
	}

	public static FluidStack firstOrEmpty(Storage<FluidVariant> storage) {
		FluidStack stack = getFirstFluid(storage);
		return stack == null ? FluidStack.EMPTY : stack;
	}

	@Nullable
	public static FluidStack getFirstFluid(Storage<FluidVariant> storage) {
		List<FluidStack> stacks = getFluids(storage, 1);
		if (stacks.size() > 0) return stacks.get(0);
		return null;
	}

	public static List<FluidStack> getAllFluids(Storage<FluidVariant> storage) {
		return getFluids(storage, Integer.MAX_VALUE);
	}

	/**
	 * Find all fluids inside a storage.
	 * @param cutoff number of fluids to find before exiting early
	 */
	public static List<FluidStack> getFluids(Storage<FluidVariant> storage, int cutoff) {
		List<FluidStack> stacks = new ArrayList<>();
		try (Transaction t = getTransaction()) {
			for (StorageView<FluidVariant> view : storage.iterable(t)) {
				if (!view.isResourceBlank()) {
					stacks.add(new FluidStack(view.getResource(), view.getAmount()));
				}
				if (stacks.size() == cutoff) {
					break;
				}
			}
		}
		return stacks;
	}

	public static List<ItemStack> getAllItems(Storage<ItemVariant> storage) {
		return getItems(storage, Integer.MAX_VALUE);
	}

	public static List<ItemStack> getItems(Storage<ItemVariant> storage, int cutoff) {
		List<ItemStack> stacks = new ArrayList<>();
		try (Transaction t = getTransaction()) {
			for (StorageView<ItemVariant> view : storage.iterable(t)) {
				if (!view.isResourceBlank()) {
					stacks.add(view.getResource().toStack((int) view.getAmount()));
				}
				if (stacks.size() == cutoff) {
					break;
				}
			}
		}
		return stacks;
	}

	/**
	 * Remove as much as possible from a Storage.
	 * @return true if all removed
	 */
	public static <T> boolean clearStorage(Storage<T> storage) {
		boolean success = true;
		try (Transaction t = getTransaction()) {
			for (StorageView<T> view : storage.iterable(t)) {
				long toRemove = view.getAmount();
				long actual = view.extract(view.getResource(), view.getAmount(), t);
				success &= toRemove == actual;
			}
			t.commit();
		}
		return success;
	}

	public static FluidStack extractAny(Storage<FluidVariant> storage, long maxAmount) {
		try (Transaction t = getTransaction()) {
			for (StorageView<FluidVariant> view : storage.iterable(t)) {
				if (!view.isResourceBlank()) {
					long extracted = view.extract(view.getResource(), view.getAmount(), t);
					if (extracted == 0) continue;
					t.commit();
					return new FluidStack(view.getResource(), extracted);
				}
			}
		}
		return FluidStack.EMPTY;
	}
}
