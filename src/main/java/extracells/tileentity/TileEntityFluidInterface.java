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
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.networking.events.MENetworkCraftingPatternChange;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.MachineSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.IStorageMonitorable;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.util.AECableType;
import appeng.api.util.AEPartLocation;
import appeng.api.util.DimensionalCoord;
import extracells.api.IECTileEntity;
import extracells.api.IFluidInterface;
import extracells.api.crafting.IFluidCraftingPatternDetails;
import extracells.container.IContainerListener;
import extracells.crafting.CraftingPattern;
import extracells.crafting.CraftingPattern2;
import extracells.gridblock.ECFluidGridBlock;
import extracells.gui.widget.fluid.IFluidSlotListener;
import extracells.integration.Capabilities;
import extracells.integration.waila.IWailaTile;
import extracells.registries.ItemEnum;
import extracells.util.EmptyMeItemMonitor;
import extracells.util.ItemUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class TileEntityFluidInterface extends TileBase implements IActionHost, IECTileEntity, IFluidInterface, IFluidSlotListener, IStorageMonitorable, ICraftingProvider, IWailaTile, ITickable {

	List<IContainerListener> listeners = new ArrayList<IContainerListener>();
	private ECFluidGridBlock gridBlock;
	private IGridNode node = null;
	public FluidTank[] tanks = new FluidTank[6];
	public String[] fluidFilter = new String[tanks.length];
	public IFluidHandler[] fluidHandlers = new IFluidHandler[6];
	public boolean doNextUpdate = false;
	private boolean wasIdle = false;
	private int tickCount = 0;
	private boolean update = false;
	private List<ICraftingPatternDetails> patternHandlers = new ArrayList<ICraftingPatternDetails>();
	private HashMap<ICraftingPatternDetails, IFluidCraftingPatternDetails> patternConvert = new HashMap<ICraftingPatternDetails, IFluidCraftingPatternDetails>();
	private List<IAEItemStack> requestedItems = new ArrayList<IAEItemStack>();
	private List<IAEItemStack> removeList = new ArrayList<IAEItemStack>();
	public final FluidInterfaceInventory inventory;
	private IAEItemStack toExport = null;

	private final Item encodedPattern = AEApi.instance().definitions().items().encodedPattern().maybeItem().orElse(null);
	private List<IAEStack> export = new ArrayList<IAEStack>();
	private List<IAEStack> removeFromExport = new ArrayList<IAEStack>();

	private List<IAEStack> addToExport = new ArrayList<IAEStack>();

	private List<IAEItemStack> watcherList = new ArrayList<IAEItemStack>();

	private boolean isFirstGetGridNode = true;

	public TileEntityFluidInterface() {
		super();
		inventory = new FluidInterfaceInventory();
		gridBlock = new ECFluidGridBlock(this);
		for (int i = 0; i < tanks.length; i++) {
			FluidTank tank = tanks[i] = new FluidTank(10000) {
				@Override
				public FluidTank readFromNBT(NBTTagCompound nbt) {
					if (!nbt.hasKey("Empty")) {
						FluidStack fluid = FluidStack.loadFluidStackFromNBT(nbt);
						setFluid(fluid);
					}
					else {
						setFluid(null);
					}
					return this;
				}
			};
			fluidFilter[i] = "";
			fluidHandlers[i] = new FluidHandler(tank, i);
		}
	}

	private void forceUpdate() {
		updateBlock();
		for (IContainerListener listener : listeners) {
			if (listener != null) {
				listener.updateContainer();
			}
		}
		doNextUpdate = false;
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
	public AECableType getCableConnectionType(AEPartLocation aePartLocation) {
		return AECableType.DENSE;
	}

	@Override
	public Fluid getFilter(AEPartLocation side) {
		if (side == null || side == AEPartLocation.INTERNAL) {
			return null;
		}
		return FluidRegistry.getFluid(fluidFilter[side.ordinal()]);
	}

	@Override
	public IMEMonitor<IAEFluidStack> getFluidInventory() {
		return getFluidInventory(AEPartLocation.INTERNAL);
	}

	@Override
	public IFluidTank getFluidTank(AEPartLocation side) {
		if (side == null || side == AEPartLocation.INTERNAL) {
			return null;
		}
		return tanks[side.ordinal()];
	}

	@Override
	public IGridNode getGridNode(AEPartLocation dir) {
		if (FMLCommonHandler.instance().getSide().isClient() && (world == null || world.isRemote)) {
			return null;
		}
		if (isFirstGetGridNode) {
			isFirstGetGridNode = false;
			getActionableNode().updateState();
		}
		return node;
	}

	@Override
	public IMEMonitor<IAEItemStack> getItemInventory() {
		return new EmptyMeItemMonitor();
	}

	@Override
	public DimensionalCoord getLocation() {
		return new DimensionalCoord(this);
	}

	@Override
	public IInventory getPatternInventory() {
		return inventory;
	}

	@Override
	public double getPowerUsage() {
		return 1.0D;
	}

	@Override
	public List<String> getWailaBody(List<String> list, NBTTagCompound tag, EnumFacing side) {
		if (side == null) {
			return list;
		}
		list.add(I18n.translateToLocal("extracells.tooltip.direction." + side.ordinal()));
		FluidTank[] tanks = new FluidTank[6];
		for (int i = 0; i < tanks.length; i++) {
			tanks[i] = new FluidTank(10000) {
				@Override
				public FluidTank readFromNBT(NBTTagCompound nbt) {
					if (!nbt.hasKey("Empty")) {
						FluidStack fluid = FluidStack.loadFluidStackFromNBT(nbt);
						setFluid(fluid);
					}
					else {
						setFluid(null);
					}
					return this;
				}
			};
		}

		for (int i = 0; i < tanks.length; i++) {
			if (tag.hasKey("tank#" + i)) {
				tanks[i].readFromNBT(tag.getCompoundTag("tank#" + i));
			}
		}
		FluidTank tank = tanks[side.ordinal()];
		if (tank == null || tank.getFluid() == null || tank.getFluid().getFluid() == null) {
			list.add(I18n.translateToLocal("extracells.tooltip.fluid") + ": " + I18n.translateToLocal("extracells.tooltip.empty1"));
			list.add(I18n.translateToLocal("extracells.tooltip.amount") + ": 0mB / 10000mB");
		}
		else {
			list.add(I18n.translateToLocal("extracells.tooltip.fluid") + ": " + tank.getFluid().getLocalizedName());
			list.add(I18n.translateToLocal("extracells.tooltip.amount") + ": " + tank.getFluidAmount() + "mB / 10000mB");
		}
		return list;
	}

	@Override
	public NBTTagCompound getWailaTag(NBTTagCompound tag) {
		for (int i = 0; i < tanks.length; i++) {
			tag.setTag("tank#" + i, tanks[i].writeToNBT(new NBTTagCompound()));
		}
		return tag;
	}

	@Override
	public boolean isBusy() {
		return !export.isEmpty();
	}

	private ItemStack makeCraftingPatternItem(ICraftingPatternDetails details) {
		if (details == null) {
			return null;
		}
		NBTTagList in = new NBTTagList();
		NBTTagList out = new NBTTagList();
		for (IAEItemStack s : details.getInputs()) {
			if (s == null) {
				in.appendTag(new NBTTagCompound());
			}
			else {
				in.appendTag(s.getItemStack().writeToNBT(new NBTTagCompound()));
			}
		}
		for (IAEItemStack s : details.getOutputs()) {
			if (s == null) {
				out.appendTag(new NBTTagCompound());
			}
			else {
				out.appendTag(s.getItemStack().writeToNBT(new NBTTagCompound()));
			}
		}
		NBTTagCompound itemTag = new NBTTagCompound();
		itemTag.setTag("in", in);
		itemTag.setTag("out", out);
		itemTag.setBoolean("crafting", details.isCraftable());
		ItemStack pattern = new ItemStack(encodedPattern);
		pattern.setTagCompound(itemTag);
		return pattern;
	}

	@Override
	public NBTTagCompound getUpdateTag() {
		return writeToNBTWithoutExport(new NBTTagCompound());
	}

	@Override
	public void provideCrafting(ICraftingProviderHelper craftingTracker) {
		patternHandlers = new ArrayList<ICraftingPatternDetails>();
		patternConvert.clear();

		for (ItemStack currentPatternStack : inventory.inv) {
			if (currentPatternStack != null && currentPatternStack.getItem() != null && currentPatternStack.getItem() instanceof ICraftingPatternItem) {
				ICraftingPatternItem currentPattern = (ICraftingPatternItem) currentPatternStack.getItem();

				if (currentPattern != null && currentPattern.getPatternForItem(currentPatternStack, world) != null) {
					IFluidCraftingPatternDetails pattern = new CraftingPattern2(currentPattern.getPatternForItem(currentPatternStack, world));
					patternHandlers.add(pattern);
					ItemStack is = makeCraftingPatternItem(pattern);
					if (is == null) {
						continue;
					}
					ICraftingPatternDetails p = ((ICraftingPatternItem) is.getItem()).getPatternForItem(is, world);
					patternConvert.put(p, pattern);
					craftingTracker.addCraftingOption(this, p);
				}
			}
		}
	}

	private void pushItems() {
		for (IAEStack s : removeFromExport) {
			export.remove(s);
		}
		removeFromExport.clear();
		for (IAEStack s : addToExport) {
			export.add(s);
		}
		addToExport.clear();
		if (!hasWorld() || export.isEmpty()) {
			return;
		}
		EnumFacing[] facings = EnumFacing.VALUES;
		for (EnumFacing facing : facings) {
			TileEntity tile = getWorld().getTileEntity(pos.offset(facing));
			if (tile != null) {
				IAEStack stack0 = export.iterator().next();
				IAEStack stack = stack0.copy();
				if (stack instanceof IAEItemStack && tile instanceof IInventory) {
					if (tile instanceof ISidedInventory) {
						ISidedInventory inv = (ISidedInventory) tile;
						for (int i : inv.getSlotsForFace(facing.getOpposite())) {
							if (inv.canInsertItem(i, ((IAEItemStack) stack).getItemStack(), facing.getOpposite())) {
								if (inv.getStackInSlot(i) == null) {
									inv.setInventorySlotContents(i, ((IAEItemStack) stack).getItemStack());
									removeFromExport.add(stack0);
									return;
								}
								else if (ItemUtils.areItemEqualsIgnoreStackSize(inv.getStackInSlot(i), ((IAEItemStack) stack).getItemStack())) {
									int max = inv.getInventoryStackLimit();
									int current = inv.getStackInSlot(i).stackSize;
									int outStack = (int) stack.getStackSize();
									if (max == current) {
										continue;
									}
									if (current + outStack <= max) {
										ItemStack s = inv.getStackInSlot(i).copy();
										s.stackSize = s.stackSize + outStack;
										inv.setInventorySlotContents(i, s);
										removeFromExport.add(stack0);
										return;
									}
									else {
										ItemStack s = inv.getStackInSlot(i).copy();
										s.stackSize = max;
										inv.setInventorySlotContents(i, s);
										removeFromExport.add(stack0);
										stack.setStackSize(outStack - max + current);
										addToExport.add(stack);
										return;
									}
								}
							}
						}
					}
					else {
						IInventory inv = (IInventory) tile;
						for (int i = 0; i < inv.getSizeInventory(); i++) {
							if (inv.isItemValidForSlot(i, ((IAEItemStack) stack).getItemStack())) {
								if (inv.getStackInSlot(i) == null) {
									inv.setInventorySlotContents(i, ((IAEItemStack) stack).getItemStack());
									removeFromExport.add(stack0);
									return;
								}
								else if (ItemUtils.areItemEqualsIgnoreStackSize(inv.getStackInSlot(i), ((IAEItemStack) stack).getItemStack())) {
									int max = inv.getInventoryStackLimit();
									int current = inv.getStackInSlot(i).stackSize;
									int outStack = (int) stack.getStackSize();
									if (max == current) {
										continue;
									}
									if (current + outStack <= max) {
										ItemStack s = inv.getStackInSlot(i).copy();
										s.stackSize = s.stackSize + outStack;
										inv.setInventorySlotContents(i, s);
										removeFromExport.add(stack0);
										return;
									}
									else {
										ItemStack s = inv.getStackInSlot(i).copy();
										s.stackSize = max;
										inv.setInventorySlotContents(i, s);
										removeFromExport.add(stack0);
										stack.setStackSize(outStack - max + current);
										addToExport.add(stack);
										return;
									}
								}
							}
						}
					}
				}
				else if (stack instanceof IAEFluidStack && tile.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, facing.getOpposite())) {
					IFluidHandler handler = tile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, facing.getOpposite());
					IAEFluidStack fluid = (IAEFluidStack) stack;
					int amount = handler.fill(fluid.getFluidStack().copy(), false);
					if (amount == 0) {
						continue;
					}
					if (amount == fluid.getStackSize()) {
						handler.fill(fluid.getFluidStack().copy(), true);
						removeFromExport.add(stack0);
					}
					else {
						IAEFluidStack aeFluidStack = fluid.copy();
						aeFluidStack.setStackSize(aeFluidStack.getStackSize() - amount);
						FluidStack fluidStack = fluid.getFluidStack().copy();
						fluidStack.amount = amount;
						handler.fill(fluidStack, true);
						removeFromExport.add(stack0);
						addToExport.add(aeFluidStack);
						return;
					}
				}
			}
		}
	}

	@Override
	public boolean pushPattern(ICraftingPatternDetails patDetails, InventoryCrafting table) {
		if (isBusy() || !patternConvert.containsKey(patDetails)) {
			return false;
		}
		ICraftingPatternDetails patternDetails = patternConvert.get(patDetails);
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
				export.add(extractFluid);
			}
			for (IAEItemStack s : patter.getCondensedInputs()) {
				if (s == null) {
					continue;
				}
				if (s.getItem() == ItemEnum.FLUIDPATTERN.getItem()) {
					toExport = s.copy();
					continue;
				}
				export.add(s);
			}

		}
		return true;
	}

	public void readFilter(NBTTagCompound tag) {
		for (int i = 0; i < fluidFilter.length; i++) {
			if (tag.hasKey("fluid#" + i)) {
				fluidFilter[i] = tag.getString("fluid#" + i);
			}
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		for (int i = 0; i < tanks.length; i++) {
			if (tag.hasKey("tank#" + i)) {
				tanks[i].readFromNBT(tag.getCompoundTag("tank#" + i));
			}
			if (tag.hasKey("filter#" + i)) {
				fluidFilter[i] = tag.getString("filter#" + i);
			}
		}
		if (hasWorld()) {
			IGridNode node = getGridNode(AEPartLocation.INTERNAL);
			if (tag.hasKey("nodes") && node != null) {
				node.loadFromNBT("node0", tag.getCompoundTag("nodes"));
				node.updateState();
			}
		}
		if (tag.hasKey("inventory")) {
			inventory.readFromNBT(tag.getCompoundTag("inventory"));
		}
		if (tag.hasKey("export")) {
			readOutputFromNBT(tag.getCompoundTag("export"));
		}
	}

	private void readOutputFromNBT(NBTTagCompound tag) {
		addToExport.clear();
		removeFromExport.clear();
		export.clear();
		int i = tag.getInteger("remove");
		for (int j = 0; j < i; j++) {
			if (tag.getBoolean("remove-" + j + "-isItem")) {
				IAEItemStack s = AEApi.instance().storage().createItemStack(ItemStack.loadItemStackFromNBT(tag.getCompoundTag("remove-" + j)));
				s.setStackSize(tag.getLong("remove-" + j + "-amount"));
				removeFromExport.add(s);
			}
			else {
				IAEFluidStack s = AEApi.instance().storage().createFluidStack(FluidStack.loadFluidStackFromNBT(tag.getCompoundTag("remove-" + j)));
				s.setStackSize(tag.getLong("remove-" + j + "-amount"));
				removeFromExport.add(s);
			}
		}
		i = tag.getInteger("add");
		for (int j = 0; j < i; j++) {
			if (tag.getBoolean("add-" + j + "-isItem")) {
				IAEItemStack s = AEApi.instance().storage().createItemStack(ItemStack.loadItemStackFromNBT(tag.getCompoundTag("add-" + j)));
				s.setStackSize(tag.getLong("add-" + j + "-amount"));
				addToExport.add(s);
			}
			else {
				IAEFluidStack s = AEApi.instance().storage().createFluidStack(FluidStack.loadFluidStackFromNBT(tag.getCompoundTag("add-" + j)));
				s.setStackSize(tag.getLong("add-" + j + "-amount"));
				addToExport.add(s);
			}
		}
		i = tag.getInteger("export");
		for (int j = 0; j < i; j++) {
			if (tag.getBoolean("export-" + j + "-isItem")) {
				IAEItemStack s = AEApi.instance().storage().createItemStack(ItemStack.loadItemStackFromNBT(tag.getCompoundTag("export-" + j)));
				s.setStackSize(tag.getLong("export-" + j + "-amount"));
				export.add(s);
			}
			else {
				IAEFluidStack s = AEApi.instance().storage().createFluidStack(FluidStack.loadFluidStackFromNBT(tag.getCompoundTag("export-" + j)));
				s.setStackSize(tag.getLong("export-" + j + "-amount"));
				export.add(s);
			}
		}
	}

	public void registerListener(IContainerListener listener) {
		listeners.add(listener);
	}

	public void removeListener(IContainerListener listener) {
		listeners.remove(listener);
	}

	@Override
	public void securityBreak() {

	}

	@Override
	public void setFilter(AEPartLocation side, Fluid fluid) {
		if (side == null || side == AEPartLocation.INTERNAL) {
			return;
		}
		if (fluid == null) {
			fluidFilter[side.ordinal()] = "";
			doNextUpdate = true;
			return;
		}
		fluidFilter[side.ordinal()] = fluid.getName();
		doNextUpdate = true;
	}

	@Override
	public void setFluid(int index, Fluid fluid, EntityPlayer player) {
		setFilter(AEPartLocation.fromOrdinal(index), fluid);
	}

	@Override
	public void setFluidTank(AEPartLocation side, FluidStack fluid) {
		if (side == null || side == AEPartLocation.INTERNAL) {
			return;
		}
		tanks[side.ordinal()].setFluid(fluid);
		doNextUpdate = true;
	}

	private void tick() {
		if (tickCount >= 40 || !wasIdle) {
			tickCount = 0;
			wasIdle = true;
		}
		else {
			tickCount++;
			return;
		}
		if (node == null) {
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
		if (toExport != null) {
			storage.getItemInventory().injectItems(toExport, Actionable.MODULATE, new MachineSource(this));
			toExport = null;
		}
		for (int i = 0; i < tanks.length; i++) {
			if (tanks[i].getFluid() != null && FluidRegistry.getFluid(fluidFilter[i]) != tanks[i].getFluid().getFluid()) {
				FluidStack s = tanks[i].drain(125, false);
				if (s != null) {
					IAEFluidStack notAdded = storage.getFluidInventory().injectItems(AEApi.instance().storage().createFluidStack(s.copy()), Actionable.SIMULATE, new MachineSource(this));
					if (notAdded != null) {
						int toAdd = (int) (s.amount - notAdded.getStackSize());
						storage.getFluidInventory().injectItems(AEApi.instance().storage().createFluidStack(tanks[i].drain(toAdd, true)), Actionable.MODULATE, new MachineSource(this));
						doNextUpdate = true;
						wasIdle = false;
					}
					else {
						storage.getFluidInventory().injectItems(AEApi.instance().storage().createFluidStack(tanks[i].drain(s.amount, true)), Actionable.MODULATE, new MachineSource(this));
						doNextUpdate = true;
						wasIdle = false;
					}
				}
			}
			if ((tanks[i].getFluid() == null || tanks[i].getFluid().getFluid() == FluidRegistry.getFluid(fluidFilter[i])) && FluidRegistry.getFluid(fluidFilter[i]) != null) {
				IAEFluidStack extracted = storage.getFluidInventory().extractItems(AEApi.instance().storage().createFluidStack(new FluidStack(FluidRegistry.getFluid(fluidFilter[i]), 125)), Actionable.SIMULATE, new MachineSource(this));
				if (extracted == null) {
					continue;
				}
				int accepted = tanks[i].fill(extracted.getFluidStack(), false);
				if (accepted == 0) {
					continue;
				}
				tanks[i].fill(storage.getFluidInventory().extractItems(AEApi.instance().storage().createFluidStack(new FluidStack(FluidRegistry.getFluid(fluidFilter[i]), accepted)), Actionable.MODULATE, new MachineSource(this)).getFluidStack(), true);
				doNextUpdate = true;
				wasIdle = false;
			}
		}
	}

	@Override
	public void update() {
		if (world.isRemote) {
			return;
		}
		if (update) {
			update = false;
			IGridNode gridNode = getGridNode(AEPartLocation.INTERNAL);
			if (gridNode != null && gridNode.getGrid() != null) {
				gridNode.getGrid().postEvent(new MENetworkCraftingPatternChange(this, gridNode));
			}
		}
		pushItems();
		if (doNextUpdate) {
			forceUpdate();
		}
		tick();
	}

	public NBTTagCompound writeFilter(NBTTagCompound tag) {
		for (int i = 0; i < fluidFilter.length; i++) {
			tag.setString("fluid#" + i, fluidFilter[i]);
		}
		return tag;
	}

	private NBTTagCompound writeOutputToNBT(NBTTagCompound tag) {
		int i = 0;
		for (IAEStack s : removeFromExport) {
			if (s != null) {
				tag.setBoolean("remove-" + i + "-isItem", s.isItem());
				NBTTagCompound data = new NBTTagCompound();
				if (s.isItem()) {
					((IAEItemStack) s).getItemStack().writeToNBT(data);
				}
				else {
					((IAEFluidStack) s).getFluidStack().writeToNBT(data);
				}
				tag.setTag("remove-" + i, data);
				tag.setLong("remove-" + i + "-amount", s.getStackSize());
			}
			i++;
		}
		tag.setInteger("remove", removeFromExport.size());
		i = 0;
		for (IAEStack s : addToExport) {
			if (s != null) {
				tag.setBoolean("add-" + i + "-isItem", s.isItem());
				NBTTagCompound data = new NBTTagCompound();
				if (s.isItem()) {
					((IAEItemStack) s).getItemStack().writeToNBT(data);
				}
				else {
					((IAEFluidStack) s).getFluidStack().writeToNBT(data);
				}
				tag.setTag("add-" + i, data);
				tag.setLong("add-" + i + "-amount", s.getStackSize());
			}
			i++;
		}
		tag.setInteger("add", addToExport.size());
		i = 0;
		for (IAEStack s : export) {
			if (s != null) {
				tag.setBoolean("export-" + i + "-isItem", s.isItem());
				NBTTagCompound data = new NBTTagCompound();
				if (s.isItem()) {
					((IAEItemStack) s).getItemStack().writeToNBT(data);
				}
				else {
					((IAEFluidStack) s).getFluidStack().writeToNBT(data);
				}
				tag.setTag("export-" + i, data);
				tag.setLong("export-" + i + "-amount", s.getStackSize());
			}
			i++;
		}
		tag.setInteger("export", export.size());
		return tag;
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound data) {
		writeToNBTWithoutExport(data);
		NBTTagCompound tag = new NBTTagCompound();
		writeOutputToNBT(tag);
		data.setTag("export", tag);
		return data;
	}

	public NBTTagCompound writeToNBTWithoutExport(NBTTagCompound tag) {
		super.writeToNBT(tag);
		for (int i = 0; i < tanks.length; i++) {
			tag.setTag("tank#" + i, tanks[i].writeToNBT(new NBTTagCompound()));
			tag.setString("filter#" + i, fluidFilter[i]);
		}
		if (!hasWorld()) {
			return tag;
		}
		IGridNode node = getGridNode(AEPartLocation.INTERNAL);
		if (node != null) {
			NBTTagCompound nodeTag = new NBTTagCompound();
			node.saveToNBT("node0", nodeTag);
			tag.setTag("nodes", nodeTag);
		}
		NBTTagCompound inventory = new NBTTagCompound();
		this.inventory.writeToNBT(inventory);
		tag.setTag("inventory", inventory);
		return tag;
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
			return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(fluidHandlers[facing.ordinal()]);
		}
		if (capability == Capabilities.STORAGE_MONITORABLE_ACCESSOR) {
			return Capabilities.STORAGE_MONITORABLE_ACCESSOR.cast((m) -> this);
		}
		return super.getCapability(capability, facing);
	}

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || capability == Capabilities.STORAGE_MONITORABLE_ACCESSOR || super.hasCapability(capability, facing);
	}

	private class FluidInterfaceInventory implements IInventory {

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
			return "inventory.fluidInterface";
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
				return details != null;
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

	//TODO: Clean Up
	private class FluidHandler implements IFluidHandler {
		FluidTank fluidTank;
		int filterIndex;

		public FluidHandler(FluidTank fluidTank, int filterIndex) {
			this.fluidTank = fluidTank;
		}

		@Override
		public FluidStack drain(FluidStack resource, boolean doDrain) {
			FluidStack tankFluid = fluidTank.getFluid();
			if (resource == null || tankFluid == null || tankFluid.getFluid() != resource.getFluid()) {
				return null;
			}
			return drain(resource.amount, doDrain);

		}

		@Override
		public FluidStack drain(int maxDrain, boolean doDrain) {
			FluidStack drained = fluidTank.drain(maxDrain, doDrain);
			if (drained != null) {
				if (world != null) {
					updateBlock();
				}
			}
			doNextUpdate = true;
			return drained;
		}

		@Override
		public IFluidTankProperties[] getTankProperties() {
			return fluidTank.getTankProperties();
		}

		@Override
		public int fill(FluidStack resource, boolean doFill) {
			if (resource == null) {
				return 0;
			}

			if ((fluidTank.getFluid() == null || fluidTank.getFluid().getFluid() == resource.getFluid()) && resource.getFluid() == FluidRegistry.getFluid(fluidFilter[filterIndex])) {
				int added = fluidTank.fill(resource.copy(), doFill);
				if (added == resource.amount) {
					doNextUpdate = true;
					return added;
				}
				added += fillToNetwork(new FluidStack(resource.getFluid(), resource.amount - added), doFill);
				doNextUpdate = true;
				return added;
			}

			int filled = 0;
			filled += fillToNetwork(resource, doFill);

			if (filled < resource.amount) {
				filled += fluidTank.fill(new FluidStack(resource.getFluid(), resource.amount - filled), doFill);
			}
			if (filled > 0) {
				if (world != null) {
					updateBlock();
				}
			}
			doNextUpdate = true;
			return filled;
		}

		public int fillToNetwork(FluidStack resource, boolean doFill) {
			IGridNode node = getGridNode(AEPartLocation.INTERNAL);
			if (node == null || resource == null) {
				return 0;
			}
			IGrid grid = node.getGrid();
			if (grid == null) {
				return 0;
			}
			IStorageGrid storage = grid.getCache(IStorageGrid.class);
			if (storage == null) {
				return 0;
			}
			IAEFluidStack notRemoved;
			if (doFill) {
				notRemoved = storage.getFluidInventory().injectItems(AEApi.instance().storage().createFluidStack(resource), Actionable.MODULATE, new MachineSource(TileEntityFluidInterface.this));
			}
			else {
				notRemoved = storage.getFluidInventory().injectItems(AEApi.instance().storage().createFluidStack(resource), Actionable.SIMULATE, new MachineSource(TileEntityFluidInterface.this));
			}
			if (notRemoved == null) {
				return resource.amount;
			}
			return (int) (resource.amount - notRemoved.getStackSize());
		}
	}
}
