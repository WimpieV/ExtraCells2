package extracells.tileentity;

import java.util.ArrayList;
import java.util.List;

import appeng.api.AEApi;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.events.MENetworkCellArrayUpdate;
import appeng.api.networking.security.IActionHost;
import appeng.api.storage.ICellContainer;
import appeng.api.storage.ICellHandler;
import appeng.api.storage.ICellRegistry;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.StorageChannel;
import appeng.api.util.AECableType;
import appeng.api.util.AEPartLocation;
import appeng.api.util.DimensionalCoord;
import extracells.api.IECTileEntity;
import extracells.gridblock.ECGridBlockHardMEDrive;
import extracells.inventory.ECPrivateInventory;
import extracells.inventory.IInventoryListener;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

@SuppressWarnings("rawtypes")
public class TileEntityHardMeDrive extends TileBase implements IActionHost, IECTileEntity, ICellContainer, IInventoryListener {

	private int priority = 0;
	boolean isFirstGridNode = true;
	byte[] cellStatuses = new byte[3];
	List<IMEInventoryHandler> fluidHandlers = new ArrayList<IMEInventoryHandler>();
	List<IMEInventoryHandler> itemHandlers = new ArrayList<IMEInventoryHandler>();
	private final ECGridBlockHardMEDrive gridBlock = new ECGridBlockHardMEDrive(this);

	private ECPrivateInventory inventory = new ECPrivateInventory("extracells.part.drive", 3, 1, this) {

		ICellRegistry cellRegistry = AEApi.instance().registries().cell();

		@Override
		public boolean isItemValidForSlot(int i, ItemStack itemStack) {
			return cellRegistry.isCellHandled(itemStack);
		}
	};

	public IInventory getInventory() {
		return inventory;
	}

	public boolean isUseableByPlayer(EntityPlayer player) {
		return world.getTileEntity(pos) == this && player.getDistanceSq(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
	}

	IGridNode node = null;

	@Override
	public void blinkCell(int i) {

	}

	@Override
	public IGridNode getActionableNode() {
		return getGridNode(AEPartLocation.INTERNAL);
	}

	@Override
	public List<IMEInventoryHandler> getCellArray(StorageChannel channel) {
		if (!isActive()) {
			return new ArrayList<IMEInventoryHandler>();
		}
		return channel == StorageChannel.ITEMS ? itemHandlers : fluidHandlers;
	}

	@Override
	public int getPriority() {
		return priority;
	}

	@Override
	public DimensionalCoord getLocation() {
		return new DimensionalCoord(this);
	}

	@Override
	public double getPowerUsage() {
		return 0;
	}

	@Override
	public IGridNode getGridNode(AEPartLocation location) {
		if (isFirstGridNode && hasWorld() && !world.isRemote) {
			isFirstGridNode = false;
			try {
				node = AEApi.instance().createGridNode(gridBlock);
				node.updateState();
			}
			catch (Exception e) {
				isFirstGridNode = true;
			}
		}

		return node;
	}

	@Override
	public AECableType getCableConnectionType(AEPartLocation location) {
		return AECableType.SMART;
	}

	@Override
	public void securityBreak() {

	}

	@Override
	public void saveChanges(IMEInventory imeInventory) {

	}

	//TODO
	boolean isActive() {
		return true;
	}

	public int getColorByStatus(int status) {
		switch (status) {
		case 1:
			return 0x00FF00;
		case 2:
			return 0xFFFF00;
		case 3:
			return 0xFF0000;
		default:
			return 0x000000;
		}
	}

	@Override
	public void onInventoryChanged() {
		itemHandlers = updateHandlers(StorageChannel.ITEMS);
		fluidHandlers = updateHandlers(StorageChannel.FLUIDS);
		for (int i = 0; i < cellStatuses.length; i++) {
			ItemStack stackInSlot = inventory.getStackInSlot(i);
			IMEInventoryHandler inventoryHandler = AEApi.instance().registries().cell().getCellInventory(stackInSlot, null, StorageChannel.ITEMS);
			if (inventoryHandler == null) {
				inventoryHandler = AEApi.instance().registries().cell().getCellInventory(stackInSlot, null, StorageChannel.FLUIDS);
			}

			ICellHandler cellHandler = AEApi.instance().registries().cell().getHandler(stackInSlot);
			if (cellHandler == null || inventoryHandler == null) {
				cellStatuses[i] = 0;
			}
			else {
				cellStatuses[i] = (byte) cellHandler.getStatusForCell(stackInSlot, inventoryHandler);
			}
		}
		IGridNode node = getGridNode(AEPartLocation.INTERNAL);
		if (node != null) {
			IGrid grid = node.getGrid();
			if (grid != null) {
				grid.postEvent(new MENetworkCellArrayUpdate());
			}
			updateBlock();
		}
	}

	private List<IMEInventoryHandler> updateHandlers(StorageChannel channel) {
		ICellRegistry cellRegistry = AEApi.instance().registries().cell();
		List<IMEInventoryHandler> handlers = new ArrayList<IMEInventoryHandler>();
		for (int i = 0; i < inventory.getSizeInventory(); i++) {
			ItemStack cell = inventory.getStackInSlot(i);
			if (cellRegistry.isCellHandled(cell)) {
				IMEInventoryHandler cellInventory = cellRegistry.getCellInventory(cell, null, channel);
				if (cellInventory != null) {
					handlers.add(cellInventory);
				}
			}
		}
		return handlers;
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		inventory.readFromNBT(tag.getTagList("inventory", 10));
		onInventoryChanged();
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		tag.setTag("inventory", inventory.writeToNBT());
		return tag;
	}

	@Override
	public NBTTagCompound getUpdateTag() {
		NBTTagCompound tag = writeToNBT(new NBTTagCompound());
		int i = 0;
		for (byte aCellStati : cellStatuses) {
			tag.setByte("status#" + i, aCellStati);
			i++;
		}
		return tag;
	}
}
