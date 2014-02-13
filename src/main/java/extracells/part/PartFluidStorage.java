package extracells.part;

import appeng.api.networking.events.MENetworkStorageEvent;
import appeng.api.parts.IPartCollsionHelper;
import appeng.api.parts.IPartRenderHelper;
import appeng.api.storage.ICellContainer;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.StorageChannel;
import appeng.api.util.AEColor;
import extracells.inventoryHandler.HandlerPartStorageFluid;
import extracells.render.TextureManager;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.ArrayList;
import java.util.List;

public class PartFluidStorage extends PartECBase implements ICellContainer
{
	int priority = 0;
	HandlerPartStorageFluid handler = new HandlerPartStorageFluid(this);

	@Override
	public void renderInventory(IPartRenderHelper rh, RenderBlocks renderer)
	{
		Tessellator ts = Tessellator.instance;

		IIcon side = TextureManager.BUS_SIDE.getTexture();
		rh.setTexture(side, side, side, TextureManager.STORAGE_FRONT.getTextures()[0], side, side);
		rh.setBounds(2, 2, 15, 14, 14, 16);
		rh.renderInventoryBox(renderer);

		rh.setInvColor(AEColor.Cyan.blackVariant);
		ts.setBrightness(15 << 20 | 15 << 4);
		rh.renderInventoryFace(TextureManager.STORAGE_FRONT.getTextures()[1], ForgeDirection.SOUTH, renderer);
		rh.setBounds(4, 4, 14, 12, 12, 15);
		rh.renderInventoryBox(renderer);

		rh.setBounds(5, 5, 13, 11, 11, 14);
		renderInventoryBusLights(rh, renderer);
	}

	@Override
	public void renderStatic(int x, int y, int z, IPartRenderHelper rh, RenderBlocks renderer)
	{
		Tessellator ts = Tessellator.instance;

		IIcon side = TextureManager.BUS_SIDE.getTexture();
		rh.setTexture(side, side, side, TextureManager.STORAGE_FRONT.getTexture(), side, side);
		rh.setBounds(2, 2, 15, 14, 14, 16);
		rh.renderBlock(x, y, z, renderer);

		ts.setColorOpaque_I(host.getColor().blackVariant);
		ts.setBrightness(15 << 20 | 15 << 4);
		rh.renderFace(x, y, z, TextureManager.STORAGE_FRONT.getTextures()[1], ForgeDirection.SOUTH, renderer);
		rh.setBounds(4, 4, 14, 12, 12, 15);
		rh.renderBlock(x, y, z, renderer);

		rh.setBounds(5, 5, 13, 11, 11, 14);
		renderStaticBusLights(x, y, z, rh, renderer);
	}

	@Override
	public void writeToNBT(NBTTagCompound data)
	{
		super.writeToNBT(data);
		data.setInteger("priority", priority);
	}

	@Override
	public void readFromNBT(NBTTagCompound data)
	{
		super.readFromNBT(data);
		priority = data.getInteger("priority");
	}

	@Override
	public void getBoxes(IPartCollsionHelper bch)
	{
		bch.addBox(2, 2, 15, 14, 14, 16);
		bch.addBox(4, 4, 14, 12, 12, 15);
		bch.addBox(5, 5, 13, 11, 11, 14);
	}

	@Override
	public int cableConnectionRenderTo()
	{
		return 3;
	}

	@Override
	public List<IMEInventoryHandler> getCellArray(StorageChannel channel)
	{
		List<IMEInventoryHandler> list = new ArrayList<IMEInventoryHandler>();
		if (channel == StorageChannel.FLUIDS)
		{
			list.add(handler);
		}
		return list;
	}

	@Override
	public int getLightLevel()
	{
		return 0;
	}

	@Override
	public int getPriority()
	{
		return priority;
	}

	@Override
	public void blinkCell(int slot)
	{
	}

	@Override
	public void onNeighborChanged()
	{
		handler.onNeighborChange();
		if (node != null && node.getGrid() != null && gridBlock != null)
			node.getGrid().postEvent(new MENetworkStorageEvent(gridBlock.getFluidMonitor(), StorageChannel.FLUIDS));
	}

	public TileEntity getHostTile()
	{
		return hostTile;
	}

}