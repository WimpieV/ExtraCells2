package extracells.tileentity;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.networking.events.MENetworkCellArrayUpdate;
import appeng.api.networking.events.MENetworkCraftingPatternChange;
import appeng.api.networking.events.MENetworkEventSubscribe;
import appeng.api.networking.events.MENetworkPowerStatusChange;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.MachineSource;
import appeng.api.networking.storage.IBaseMonitor;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.IMEMonitorHandlerReceiver;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.AECableType;
import appeng.api.util.AEPartLocation;
import appeng.api.util.DimensionalCoord;
import extracells.api.IECTileEntity;
import extracells.gridblock.ECFluidGridBlock;
import extracells.util.FluidHelper;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ITickable;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class TileEntityFluidFiller extends TileBase implements IActionHost, ICraftingProvider, IECTileEntity, IMEMonitorHandlerReceiver<IAEFluidStack>, IListenerTile, ITickable {

	private ECFluidGridBlock gridBlock;
	private IGridNode node = null;
	List<Fluid> fluids = new ArrayList<Fluid>();
	public ItemStack containerItem = new ItemStack(Items.BUCKET);
	ItemStack returnStack = null;
	int ticksToFinish = 0;

	private boolean isFirstGetGridNode = true;

	private final Item encodedPattern = AEApi.instance().definitions().items().encodedPattern().maybeItem().orElse(null);

	public TileEntityFluidFiller() {
		super();
		gridBlock = new ECFluidGridBlock(this);
	}

	@MENetworkEventSubscribe
	public void cellUpdate(MENetworkCellArrayUpdate event) {
		IStorageGrid storage = getStorageGrid();
		if (storage != null) {
			postChange(storage.getFluidInventory(), null, null);
		}
	}

	@Override
	public IGridNode getActionableNode() {
		if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
			return null;
		}
		if (node == null) {
			node = AEApi.instance().createGridNode(gridBlock);
		}
		return node;
	}

	@Override
	public AECableType getCableConnectionType(AEPartLocation dir) {
		return AECableType.DENSE;
	}

	@Override
	public NBTTagCompound getUpdateTag() {
		return writeToNBT(new NBTTagCompound());
	}

	@Override
	public IGridNode getGridNode(AEPartLocation location) {
		if (FMLCommonHandler.instance().getSide().isClient() && (world == null || world.isRemote)) {
			return null;
		}
		if (isFirstGetGridNode) {
			isFirstGetGridNode = false;
			getActionableNode().updateState();
			IStorageGrid storage = getStorageGrid();
			storage.getFluidInventory().addListener(this, null);
		}
		return node;
	}

	@Override
	public DimensionalCoord getLocation() {
		return new DimensionalCoord(this);
	}

	private ItemStack getPattern(ItemStack emptyContainer, ItemStack filledContainer) {
		NBTTagList in = new NBTTagList();
		NBTTagList out = new NBTTagList();
		in.appendTag(emptyContainer.writeToNBT(new NBTTagCompound()));
		out.appendTag(filledContainer.writeToNBT(new NBTTagCompound()));
		NBTTagCompound itemTag = new NBTTagCompound();
		itemTag.setTag("in", in);
		itemTag.setTag("out", out);
		itemTag.setBoolean("crafting", false);
		ItemStack pattern = new ItemStack(encodedPattern);
		pattern.setTagCompound(itemTag);
		return pattern;
	}

	@Override
	public double getPowerUsage() {
		return 1.0D;
	}

	private IStorageGrid getStorageGrid() {
		node = getGridNode(AEPartLocation.INTERNAL);
		if (node == null) {
			return null;
		}
		IGrid grid = node.getGrid();
		if (grid == null) {
			return null;
		}
		return grid.getCache(IStorageGrid.class);
	}

	@Override
	public boolean isBusy() {
		return returnStack != null;
	}

	@Override
	public boolean isValid(Object verificationToken) {
		return true;
	}

	@Override
	public void onListUpdate() {
	}

	@Override
	public void postChange(IBaseMonitor<IAEFluidStack> monitor, Iterable<IAEFluidStack> change, BaseActionSource actionSource) {
		List<Fluid> oldFluids = new ArrayList<Fluid>(fluids);
		boolean mustUpdate = false;
		fluids.clear();
		for (IAEFluidStack fluid : ((IMEMonitor<IAEFluidStack>) monitor).getStorageList()) {
			if (!oldFluids.contains(fluid.getFluid())) {
				mustUpdate = true;
			}
			else {
				oldFluids.remove(fluid.getFluid());
			}
			fluids.add(fluid.getFluid());
		}
		if (!(oldFluids.isEmpty() && !mustUpdate)) {
			if (getGridNode(AEPartLocation.INTERNAL) != null && getGridNode(AEPartLocation.INTERNAL).getGrid() != null) {
				getGridNode(AEPartLocation.INTERNAL).getGrid().postEvent(new MENetworkCraftingPatternChange(this, getGridNode(AEPartLocation.INTERNAL)));
			}
		}
	}

	public void postUpdateEvent() {
		if (getGridNode(AEPartLocation.INTERNAL) != null && getGridNode(AEPartLocation.INTERNAL).getGrid() != null) {
			getGridNode(AEPartLocation.INTERNAL).getGrid().postEvent(new MENetworkCraftingPatternChange(this, getGridNode(AEPartLocation.INTERNAL)));
		}
	}

	@MENetworkEventSubscribe
	public void powerUpdate(MENetworkPowerStatusChange event) {
		IStorageGrid storage = getStorageGrid();
		if (storage != null) {
			postChange(storage.getFluidInventory(), null, null);
		}
	}

	@Override
	public void provideCrafting(ICraftingProviderHelper craftingTracker) {
		IStorageGrid storage = getStorageGrid();
		if (storage == null) {
			return;
		}
		IMEMonitor<IAEFluidStack> fluidStorage = storage.getFluidInventory();
		for (IAEFluidStack fluidStack : fluidStorage.getStorageList()) {
			Fluid fluid = fluidStack.getFluid();
			if (fluid == null) {
				continue;
			}
			int maxCapacity = FluidHelper.getCapacity(containerItem);
			if (maxCapacity == 0) {
				continue;
			}
			Pair<Integer, ItemStack> filled = FluidHelper.fillStack(containerItem.copy(), new FluidStack(fluid, maxCapacity));
			if (filled.getRight() == null) {
				continue;
			}
			ItemStack pattern = getPattern(containerItem, filled.getRight());
			ICraftingPatternItem patter = (ICraftingPatternItem) pattern.getItem();
			craftingTracker.addCraftingOption(this, patter.getPatternForItem(pattern, world));
		}

	}

	@Override
	public boolean pushPattern(ICraftingPatternDetails patternDetails, InventoryCrafting table) {
		if (returnStack != null) {
			return false;
		}
		ItemStack filled = patternDetails.getCondensedOutputs()[0].getItemStack();
		FluidStack fluid = FluidHelper.getFluidFromContainer(filled);
		IStorageGrid storage = getStorageGrid();
		if (storage == null) {
			return false;
		}
		IAEFluidStack fluidStack = AEApi.instance().storage().createFluidStack(new FluidStack(fluid.getFluid(), FluidHelper.getCapacity(patternDetails.getCondensedInputs()[0].getItemStack())));
		IAEFluidStack extracted = storage.getFluidInventory().extractItems(fluidStack.copy(), Actionable.SIMULATE, new MachineSource(this));
		if (extracted == null || extracted.getStackSize() != fluidStack.getStackSize()) {
			return false;
		}
		storage.getFluidInventory().extractItems(fluidStack, Actionable.MODULATE, new MachineSource(this));
		returnStack = filled;
		ticksToFinish = 40;
		return true;
	}

	@Override
	public void readFromNBT(NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		if (tagCompound.hasKey("container")) {
			containerItem = ItemStack.loadItemStackFromNBT(tagCompound.getCompoundTag("container"));
		}
		else if (tagCompound.hasKey("isContainerEmpty") && tagCompound.getBoolean("isContainerEmpty")) {
			containerItem = null;
		}
		if (tagCompound.hasKey("return")) {
			returnStack = ItemStack.loadItemStackFromNBT(tagCompound.getCompoundTag("return"));
		}
		else if (tagCompound.hasKey("isReturnEmpty") && tagCompound.getBoolean("isReturnEmpty")) {
			returnStack = null;
		}
		if (tagCompound.hasKey("time")) {
			ticksToFinish = tagCompound.getInteger("time");
		}
		if (hasWorld()) {
			IGridNode node = getGridNode(AEPartLocation.INTERNAL);
			if (tagCompound.hasKey("nodes") && node != null) {
				node.loadFromNBT("node0", tagCompound.getCompoundTag("nodes"));
				node.updateState();
			}
		}
	}

	@Override
	public void registerListener() {
		IStorageGrid storage = getStorageGrid();
		if (storage == null) {
			return;
		}
		IMEMonitor<IAEFluidStack> fluidInventory = storage.getFluidInventory();
		postChange(fluidInventory, null, null);
		fluidInventory.addListener(this, null);
	}

	@Override
	public void removeListener() {
		IStorageGrid storage = getStorageGrid();
		if (storage == null) {
			return;
		}
		IMEMonitor<IAEFluidStack> fluidInventory = storage.getFluidInventory();
		fluidInventory.removeListener(this);
	}

	@Override
	public void securityBreak() {
		//TODO: Find out what func_147480_a is
		/*if (this.getWorldObj() != null)
			getWorldObj().func_147480_a(this.xCoord, this.yCoord, this.zCoord, true);*/
	}

	@Override
	public void update() {
		if (world == null) {
			return;
		}
		if (ticksToFinish > 0) {
			ticksToFinish = ticksToFinish - 1;
		}
		if (ticksToFinish <= 0 && returnStack != null) {
			IStorageGrid storage = getStorageGrid();
			if (storage == null) {
				return;
			}
			IAEItemStack toInject = AEApi.instance().storage().createItemStack(returnStack);
			if (storage.getItemInventory().canAccept(toInject.copy())) {
				IAEItemStack nodAdded = storage.getItemInventory().injectItems(toInject.copy(), Actionable.SIMULATE, new MachineSource(this));
				if (nodAdded == null) {
					storage.getItemInventory().injectItems(toInject, Actionable.MODULATE, new MachineSource(this));
					returnStack = null;
				}
			}
		}
	}

	@Override
	public void updateGrid(IGrid oldGrid, IGrid newGrid) {
		if (oldGrid != null) {
			IStorageGrid storage = oldGrid.getCache(IStorageGrid.class);
			if (storage != null) {
				storage.getFluidInventory().removeListener(this);
			}
		}
		if (newGrid != null) {
			IStorageGrid storage = newGrid.getCache(IStorageGrid.class);
			if (storage != null) {
				storage.getFluidInventory().addListener(this, null);
			}
		}
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound) {
		super.writeToNBT(tagCompound);
		if (containerItem != null) {
			tagCompound.setTag("container", containerItem.writeToNBT(new NBTTagCompound()));
		}
		else {
			tagCompound.setBoolean("isContainerEmpty", true);
		}
		if (returnStack != null) {
			tagCompound.setTag("return", returnStack.writeToNBT(new NBTTagCompound()));
		}
		else {
			tagCompound.setBoolean("isReturnEmpty", true);
		}
		tagCompound.setInteger("time", ticksToFinish);
		if (!hasWorld()) {
			return tagCompound;
		}
		IGridNode node = getGridNode(AEPartLocation.INTERNAL);
		if (node != null) {
			NBTTagCompound nodeTag = new NBTTagCompound();
			node.saveToNBT("node0", nodeTag);
			tagCompound.setTag("nodes", nodeTag);
		}
		return tagCompound;
	}
}
