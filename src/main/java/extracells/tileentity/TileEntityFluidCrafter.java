package extracells.tileentity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nullable;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.networking.crafting.ICraftingWatcher;
import appeng.api.networking.crafting.ICraftingWatcherHost;
import appeng.api.networking.events.MENetworkCraftingPatternChange;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.MachineSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.AECableType;
import appeng.api.util.AEPartLocation;
import appeng.api.util.DimensionalCoord;
import extracells.api.IECTileEntity;
import extracells.crafting.CraftingPattern;
import extracells.gridblock.ECFluidGridBlock;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ITickable;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class TileEntityFluidCrafter extends TileBase implements IActionHost, ICraftingProvider, ICraftingWatcherHost, IECTileEntity, ITickable {

	private class FluidCrafterInventory implements IInventory {

		private ItemStack[] inv = new ItemStack[9];

		@Override
		public void closeInventory(EntityPlayer player) {
		}

		@Override
		public ItemStack decrStackSize(int slot, int amt) {
			ItemStack stack = getStackInSlot(slot);
			if (stack != null) {
				if (stack.stackSize <= amt) {
					setInventorySlotContents(slot, null);
				}
				else {
					stack = stack.splitStack(amt);
					if (stack.stackSize == 0) {
						setInventorySlotContents(slot, null);
					}
				}
			}
			update = true;
			return stack;
		}

		@Override
		public String getName() {
			return "inventory.fluidCrafter";
		}

		@Override
		public int getInventoryStackLimit() {
			return 1;
		}

		@Override
		public int getSizeInventory() {
			return inv.length;
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			return inv[slot];
		}

		@Nullable
		@Override
		public ItemStack removeStackFromSlot(int index) {
			return null;
		}

		@Override
		public boolean hasCustomName() {
			return false;
		}

		@Override
		public boolean isItemValidForSlot(int slot, ItemStack stack) {
			if (stack.getItem() instanceof ICraftingPatternItem) {
				ICraftingPatternDetails details = ((ICraftingPatternItem) stack.getItem()).getPatternForItem(stack, getWorld());
				return details != null && details.isCraftable();
			}
			return false;
		}

		@Override
		public boolean isUsableByPlayer(EntityPlayer player) {
			return true;
		}

		@Override
		public void markDirty() {
		}

		@Override
		public void openInventory(EntityPlayer player) {
		}

		public void readFromNBT(NBTTagCompound tagCompound) {

			NBTTagList tagList = tagCompound.getTagList("Inventory", 10);
			for (int i = 0; i < tagList.tagCount(); i++) {
				NBTTagCompound tag = tagList.getCompoundTagAt(i);
				byte slot = tag.getByte("Slot");
				if (slot >= 0 && slot < inv.length) {
					inv[slot] = ItemStack.loadItemStackFromNBT(tag);
				}
			}
		}

		@Override
		public void setInventorySlotContents(int slot, ItemStack stack) {
			inv[slot] = stack;
			if (stack != null && stack.stackSize > getInventoryStackLimit()) {
				stack.stackSize = getInventoryStackLimit();
			}
			update = true;
		}

		public void writeToNBT(NBTTagCompound tagCompound) {

			NBTTagList itemList = new NBTTagList();
			for (int i = 0; i < inv.length; i++) {
				ItemStack stack = inv[i];
				if (stack != null) {
					NBTTagCompound tag = new NBTTagCompound();
					tag.setByte("Slot", (byte) i);
					stack.writeToNBT(tag);
					itemList.appendTag(tag);
				}
			}
			tagCompound.setTag("Inventory", itemList);
		}

		@Override
		public int getField(int id) {
			return 0;
		}

		@Override
		public void setField(int id, int value) {

		}

		@Override
		public int getFieldCount() {
			return 0;
		}

		@Override
		public void clear() {

		}

		@Override
		public ITextComponent getDisplayName() {
			return new TextComponentString(getName());
		}
	}

	private ECFluidGridBlock gridBlock;
	private IGridNode node = null;
	private List<ICraftingPatternDetails> patternHandlers = new ArrayList<ICraftingPatternDetails>();
	private List<IAEItemStack> requestedItems = new ArrayList<IAEItemStack>();
	private List<IAEItemStack> removeList = new ArrayList<IAEItemStack>();
	private ICraftingPatternDetails[] patternHandlerSlot = new ICraftingPatternDetails[9];
	private ItemStack[] oldStack = new ItemStack[9];
	private boolean isBusy = false;

	private ICraftingWatcher watcher = null;

	private boolean isFirstGetGridNode = true;

	public final FluidCrafterInventory inventory;
	private Long finishCraftingTime = 0L;
	private ItemStack returnStack = null;

	private ItemStack[] optionalReturnStack = new ItemStack[0];

	private boolean update = false;

	private final TileEntityFluidCrafter instance;

	public TileEntityFluidCrafter() {
		super();
		gridBlock = new ECFluidGridBlock(this);
		inventory = new FluidCrafterInventory();
		instance = this;
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
		return AECableType.SMART;
	}

	public IGridNode getGridNode() {
		return getGridNode(AEPartLocation.INTERNAL);
	}

	@Override
	public IGridNode getGridNode(AEPartLocation dir) {
		if (FMLCommonHandler.instance().getSide().isClient() && (getWorld() == null || getWorld().isRemote)) {
			return null;
		}
		if (isFirstGetGridNode) {
			isFirstGetGridNode = false;
			getActionableNode().updateState();
		}
		return node;
	}

	public IInventory getInventory() {
		return inventory;
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
	public boolean isBusy() {
		return isBusy;
	}

	@Override
	public void onRequestChange(ICraftingGrid craftingGrid, IAEItemStack what) {
		if (craftingGrid.isRequesting(what)) {
			if (!requestedItems.contains(what)) {
				requestedItems.add(what);
			}
		}
		else if (requestedItems.contains(what)) {
			requestedItems.remove(what);
		}

	}

	@Override
	public void provideCrafting(ICraftingProviderHelper craftingTracker) {
		patternHandlers = new ArrayList<ICraftingPatternDetails>();
		ICraftingPatternDetails[] oldHandler = patternHandlerSlot;
		patternHandlerSlot = new ICraftingPatternDetails[9];
		for (int i = 0; inventory.inv.length > i; i++) {
			ItemStack currentPatternStack = inventory.inv[i];
			ItemStack oldItem = oldStack[i];
			if (currentPatternStack != null && oldItem != null && ItemStack.areItemStacksEqual(currentPatternStack, oldItem)) {
				ICraftingPatternDetails pa = oldHandler[i];
				if (pa != null) {
					patternHandlerSlot[i] = pa;
					patternHandlers.add(pa);
					if (pa.getCondensedInputs().length == 0) {
						craftingTracker.setEmitable(pa.getCondensedOutputs()[0]);
					}
					else {
						craftingTracker.addCraftingOption(this, pa);
					}
					continue;
				}
			}
			if (currentPatternStack != null && currentPatternStack.getItem() != null && currentPatternStack.getItem() instanceof ICraftingPatternItem) {
				ICraftingPatternItem currentPattern = (ICraftingPatternItem) currentPatternStack.getItem();

				if (currentPattern != null && currentPattern.getPatternForItem(currentPatternStack, getWorld()) != null && currentPattern.getPatternForItem(currentPatternStack, getWorld()).isCraftable()) {
					ICraftingPatternDetails pattern = new CraftingPattern(currentPattern.getPatternForItem(currentPatternStack, getWorld()));
					patternHandlers.add(pattern);
					patternHandlerSlot[i] = pattern;
					if (pattern.getCondensedInputs().length == 0) {
						craftingTracker.setEmitable(pattern.getCondensedOutputs()[0]);
					}
					else {
						craftingTracker.addCraftingOption(this, pattern);
					}
				}
			}
			oldStack[i] = currentPatternStack;
		}
		updateWatcher();
	}

	@Override
	public boolean pushPattern(ICraftingPatternDetails patternDetails, InventoryCrafting table) {
		if (isBusy) {
			return false;
		}
		if (patternDetails instanceof CraftingPattern) {
			CraftingPattern patter = (CraftingPattern) patternDetails;
			HashMap<Fluid, Long> fluids = new HashMap<Fluid, Long>();
			for (IAEFluidStack stack : patter.getCondensedFluidInputs()) {
				if (fluids.containsKey(stack.getFluid())) {
					Long amount = fluids.get(stack.getFluid()) + stack.getStackSize();
					fluids.remove(stack.getFluid());
					fluids.put(stack.getFluid(), amount);
				}
				else {
					fluids.put(stack.getFluid(), stack.getStackSize());
				}
			}
			IGrid grid = node.getGrid();
			if (grid == null) {
				return false;
			}
			IStorageGrid storage = grid.getCache(IStorageGrid.class);
			if (storage == null) {
				return false;
			}
			for (Fluid fluid : fluids.keySet()) {
				Long amount = fluids.get(fluid);
				IAEFluidStack extractFluid = storage.getFluidInventory().extractItems(AEApi.instance().storage().createFluidStack(new FluidStack(fluid, (int) (amount + 0))), Actionable.SIMULATE, new MachineSource(this));
				if (extractFluid == null || extractFluid.getStackSize() != amount) {
					return false;
				}
			}
			for (Fluid fluid : fluids.keySet()) {
				Long amount = fluids.get(fluid);
				IAEFluidStack extractFluid = storage.getFluidInventory().extractItems(AEApi.instance().storage().createFluidStack(new FluidStack(fluid, (int) (amount + 0))), Actionable.MODULATE, new MachineSource(this));
			}
			finishCraftingTime = System.currentTimeMillis() + 1000;

			returnStack = patter.getOutput(table, getWorld());

			optionalReturnStack = new ItemStack[9];
			for (int i = 0; i < 9; i++) {
				ItemStack s = table.getStackInSlot(i);
				if (s != null && s.getItem() != null) {
					optionalReturnStack[i] = s.getItem().getContainerItem(s.copy());
				}
			}

			isBusy = true;
		}
		return true;
	}

	@Override
	public void readFromNBT(NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		inventory.readFromNBT(tagCompound);
		if (hasWorld()) {
			IGridNode node = getGridNode();
			if (tagCompound.hasKey("nodes") && node != null) {
				node.loadFromNBT("node0", tagCompound.getCompoundTag("nodes"));
				node.updateState();
			}
		}
	}

	@Override
	public void securityBreak() {

	}

	@Override
	public void update() {
		if (getWorld() == null || getWorld().provider == null) {
			return;
		}
		if (update) {
			update = false;
			if (getGridNode() != null && getGridNode().getGrid() != null) {
				getGridNode().getGrid().postEvent(new MENetworkCraftingPatternChange(instance, getGridNode()));
			}
		}
		if (isBusy && finishCraftingTime <= System.currentTimeMillis() && getWorld() != null && !getWorld().isRemote) {
			if (node == null || returnStack == null) {
				return;
			}
			IGrid grid = node.getGrid();
			if (grid == null) {
				return;
			}
			IStorageGrid storage = grid.getCache(IStorageGrid.class);
			if (storage == null) {
				return;
			}
			storage.getItemInventory().injectItems(AEApi.instance().storage().createItemStack(returnStack), Actionable.MODULATE, new MachineSource(this));
			for (ItemStack s : optionalReturnStack) {
				if (s == null) {
					continue;
				}
				storage.getItemInventory().injectItems(AEApi.instance().storage().createItemStack(s), Actionable.MODULATE, new MachineSource(this));
			}
			optionalReturnStack = new ItemStack[0];
			isBusy = false;
			returnStack = null;
		}
		if (!isBusy && getWorld() != null && !getWorld().isRemote) {
			for (IAEItemStack stack : removeList) {
				requestedItems.remove(stack);
			}
			removeList.clear();
			if (!requestedItems.isEmpty()) {
				for (IAEItemStack s : requestedItems) {
					IGrid grid = node.getGrid();
					if (grid == null) {
						break;
					}
					ICraftingGrid crafting = grid.getCache(ICraftingGrid.class);
					if (crafting == null) {
						break;
					}
					if (!crafting.isRequesting(s)) {
						removeList.add(s);
						continue;
					}
					for (ICraftingPatternDetails details : patternHandlers) {
						if (details.getCondensedOutputs()[0].equals(s)) {
							CraftingPattern patter = (CraftingPattern) details;
							HashMap<Fluid, Long> fluids = new HashMap<Fluid, Long>();
							for (IAEFluidStack stack : patter.getCondensedFluidInputs()) {
								if (fluids.containsKey(stack.getFluid())) {
									Long amount = fluids.get(stack.getFluid()) + stack.getStackSize();
									fluids.remove(stack.getFluid());
									fluids.put(stack.getFluid(), amount);
								}
								else {
									fluids.put(stack.getFluid(), stack.getStackSize());
								}
							}
							IStorageGrid storage = grid.getCache(IStorageGrid.class);
							if (storage == null) {
								break;
							}
							boolean doBreak = false;
							for (Fluid fluid : fluids.keySet()) {
								Long amount = fluids.get(fluid);
								IAEFluidStack extractFluid = storage.getFluidInventory().extractItems(AEApi.instance().storage().createFluidStack(new FluidStack(fluid, (int) (amount + 0))), Actionable.SIMULATE, new MachineSource(this));
								if (extractFluid == null || extractFluid.getStackSize() != amount) {
									doBreak = true;
									break;
								}
							}
							if (doBreak) {
								break;
							}
							for (Fluid fluid : fluids.keySet()) {
								Long amount = fluids.get(fluid);
								IAEFluidStack extractFluid = storage.getFluidInventory().extractItems(AEApi.instance().storage().createFluidStack(new FluidStack(fluid, (int) (amount + 0))), Actionable.MODULATE, new MachineSource(this));
							}
							finishCraftingTime = System.currentTimeMillis() + 1000;

							returnStack = patter.getCondensedOutputs()[0].getItemStack();
							isBusy = true;
							return;
						}
					}
				}
			}
		}
	}

	private void updateWatcher() {
		requestedItems = new ArrayList<IAEItemStack>();
		IGrid grid = null;
		IGridNode node = getGridNode();
		ICraftingGrid crafting = null;
		if (node != null) {
			grid = node.getGrid();
			if (grid != null) {
				crafting = grid.getCache(ICraftingGrid.class);
			}
		}
		for (ICraftingPatternDetails patter : patternHandlers) {
			watcher.reset();
			if (patter.getCondensedInputs().length == 0) {
				watcher.add(patter.getCondensedOutputs()[0]);

				if (crafting != null) {
					if (crafting.isRequesting(patter.getCondensedOutputs()[0])) {
						requestedItems.add(patter.getCondensedOutputs()[0]);
					}
				}
			}
		}
	}

	@Override
	public void updateWatcher(ICraftingWatcher newWatcher) {
		watcher = newWatcher;
		updateWatcher();
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound) {
		super.writeToNBT(tagCompound);
		inventory.writeToNBT(tagCompound);
		if (!hasWorld()) {
			return tagCompound;
		}
		IGridNode node = getGridNode();
		if (node != null) {
			NBTTagCompound nodeTag = new NBTTagCompound();
			node.saveToNBT("node0", nodeTag);
			tagCompound.setTag("nodes", nodeTag);
		}
		return tagCompound;
	}

}
