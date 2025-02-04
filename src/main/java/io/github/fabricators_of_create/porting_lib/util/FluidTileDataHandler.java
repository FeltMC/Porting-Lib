package io.github.fabricators_of_create.porting_lib.util;

import java.util.HashMap;
import java.util.Map;

import io.github.fabricators_of_create.porting_lib.PortingLib;
import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import io.github.fabricators_of_create.porting_lib.transfer.fluid.CustomFluidTileDataHandler;
import io.github.fabricators_of_create.porting_lib.transfer.fluid.FluidStack;
import io.github.fabricators_of_create.porting_lib.transfer.fluid.FluidTransferable;
import io.github.fabricators_of_create.porting_lib.transfer.fluid.IFluidHandler;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;

public class FluidTileDataHandler {
	public static final ResourceLocation PACKET_ID = PortingLib.id("fluid_tile_handler_data");

	@Environment(EnvType.CLIENT)
	private static final Map<BlockPos, FluidTankData[]> CACHED_DATA = new HashMap<>();

	public static void sendDataToClients(ServerLevel world, FluidTransferable transferable) {
		world.getPlayers(player -> {
			IFluidHandler handler = TransferUtil.getFluidHandler((BlockEntity) transferable).orElse(null);
			FluidTankData[] tankData = new FluidTankData[handler.getTanks()];
			for (int i = 0; i < tankData.length; i++) {
				FluidStack stack = handler.getFluidInTank(i);
				long amount = stack.getAmount();
				long capacity = handler.getTankCapacity(i);
				tankData[i] = new FluidTankData(amount, capacity, stack.getFluid());
			}
			ServerPlayNetworking.send(player, PACKET_ID, createPacket(tankData, (BlockEntity) transferable));
			return true;
		});
	}

	public static FriendlyByteBuf createPacket(FluidTankData[] data, BlockEntity be) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeInt(data.length);
		for (FluidTankData tank : data) {
			buf.writeLong(tank.amount());
			buf.writeLong(tank.capacity());
			buf.writeResourceLocation(Registry.FLUID.getKey(tank.fluid()));
		}
		buf.writeBlockPos(be.getBlockPos());
		return buf;
	}

	public static FluidTankData[] readPacket(FriendlyByteBuf buf) {
		FluidTankData[] data = new FluidTankData[buf.readInt()];
		for (int i = 0; i < data.length; i++) {
			long amount = buf.readLong();
			long capacity = buf.readLong();
			Fluid fluid = Registry.FLUID.get(buf.readResourceLocation());
			data[i] = new FluidTankData(amount, capacity, fluid);
		}
		return data;
	}

	public static IFluidHandler getCachedHandler(BlockEntity be) {
		FluidTankData[] data = CACHED_DATA.get(be.getBlockPos());
		if (data == null) {
			if (be instanceof CustomFluidTileDataHandler custom) {
				data = custom.getData(CACHED_DATA);
			}
			if (data == null)
				return null;
		}
		FluidTankData[] finalData = data;
		return new IFluidHandler() {
			@Override
			public int getTanks() {
				return finalData.length;
			}

			@Override
			public FluidStack getFluidInTank(int tank) {
				return new FluidStack(finalData[tank].fluid(), finalData[tank].amount());
			}

			@Override
			public long getTankCapacity(int tank) {
				return finalData[tank].capacity();
			}

			@Override
			public long fill(FluidStack stack, boolean sim) {
				return 0;
			}

			@Override
			public FluidStack drain(FluidStack stack, boolean sim) {
				return null;
			}

			@Override
			public FluidStack drain(long amount, boolean sim) {
				return null;
			}
		};
	}

	@Environment(EnvType.CLIENT)
	public static void initClient() {
		ClientPlayNetworking.registerGlobalReceiver(PACKET_ID, (client, handler, buf, sender) -> {
			FluidTankData[] data = readPacket(buf);
			BlockPos pos = buf.readBlockPos();
			client.execute(() -> CACHED_DATA.put(pos, data));
		});
	}

	public record FluidTankData(long amount, long capacity, Fluid fluid) {}
}
