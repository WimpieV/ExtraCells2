package extracells.network.packet.other;

import java.io.IOException;

import extracells.gui.widget.fluid.IFluidSlotListener;
import extracells.network.packet.IPacketHandlerServer;
import extracells.network.packet.Packet;
import extracells.network.packet.PacketBufferEC;
import extracells.network.packet.PacketId;
import extracells.part.PartECBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fluids.Fluid;

public class PacketFluidSlotSelect extends Packet {

	private int index;
	private Fluid fluid;
	private IFluidSlotListener partOrBlock;

	public PacketFluidSlotSelect(IFluidSlotListener partOrBlock, int index, Fluid fluid) {
		this.partOrBlock = partOrBlock;
		this.index = index;
		this.fluid = fluid;
	}

	@Override
	public PacketId getPacketId() {
		return PacketId.FLUID_SLOT;
	}

	@Override
	protected void writeData(PacketBufferEC data) throws IOException {
		if (partOrBlock instanceof PartECBase) {
			data.writeBoolean(true);
			data.writePart((PartECBase) partOrBlock);
		}
		else {
			data.writeBoolean(false);
			data.writeTile((TileEntity) partOrBlock);
		}
		data.writeVarInt(index);
		data.writeFluid(fluid);
	}

	public static class Handler implements IPacketHandlerServer {
		@Override
		public void onPacketData(PacketBufferEC data, EntityPlayerMP player) throws IOException {
			IFluidSlotListener listener;

			if (data.readBoolean()) {
				listener = data.readPart(player.world);
			}
			else {
				listener = data.readTile(player.world, IFluidSlotListener.class);
			}
			int index = data.readVarInt();
			Fluid fluid = data.readFluid();
			if (listener == null) {
				return;
			}
			listener.setFluid(index, fluid, player);
		}
	}
}
