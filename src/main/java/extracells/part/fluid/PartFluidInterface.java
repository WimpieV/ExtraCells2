package extracells.part.fluid;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nullable;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.SecurityPermissions;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.networking.events.MENetworkCraftingPatternChange;
import appeng.api.networking.security.MachineSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartModel;
import appeng.api.parts.PartItemStack;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.IStorageMonitorable;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.util.AECableType;
import appeng.api.util.AEPartLocation;
import extracells.api.IFluidInterface;
import extracells.api.crafting.IFluidCraftingPatternDetails;
import extracells.container.IContainerListener;
import extracells.container.fluid.ContainerFluidInterface;
import extracells.crafting.CraftingPattern;
import extracells.crafting.CraftingPattern2;
import extracells.gui.fluid.GuiFluidInterface;
import extracells.gui.widget.fluid.IFluidSlotListener;
import extracells.integration.Capabilities;
import extracells.models.PartModels;
import extracells.part.PartECBase;
import extracells.registries.ItemEnum;
import extracells.registries.PartEnum;
import extracells.util.EmptyMeItemMonitor;
import extracells.util.ItemUtils;
import extracells.util.PermissionUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
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
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class PartFluidInterface extends PartECBase implements IFluidHandler, IFluidInterface, IFluidSlotListener, IStorageMonitorable, IGridTickable, ICraftingProvider {

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
			return I18n.translateToLocal("inventory.fluidInterface");
		}

		@Override
		public ITextComponent getDisplayName() {
			return new TextComponentString(getName());
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
				IGridNode n = getGridNode();
				World w;
				if (n == null) {
					w = getClientWorld();
				}
				else {
					w = n.getWorld();
				}
				if (w == null) {
					return false;
				}
				ICraftingPatternDetails details = ((ICraftingPatternItem) stack.getItem()).getPatternForItem(stack, w);
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
	}

	List<IContainerListener> listeners = new ArrayList<IContainerListener>();
	private List<ICraftingPatternDetails> patternHandlers = new ArrayList<ICraftingPatternDetails>();
	private HashMap<ICraftingPatternDetails, IFluidCraftingPatternDetails> patternConvert = new HashMap<ICraftingPatternDetails, IFluidCraftingPatternDetails>();
	private List<IAEItemStack> requestedItems = new ArrayList<IAEItemStack>();
	private List<IAEItemStack> removeList = new ArrayList<IAEItemStack>();
	public final FluidInterfaceInventory inventory = new FluidInterfaceInventory();

	private boolean update = false;
	private List<IAEStack> export = new ArrayList<IAEStack>();
	private List<IAEStack> removeFromExport = new ArrayList<IAEStack>();

	private List<IAEStack> addToExport = new ArrayList<IAEStack>();
	private IAEItemStack toExport = null;

	private final Item encodedPattern = AEApi.instance().definitions().items().encodedPattern().maybeItem().orElse(null);
	private FluidTank tank = new FluidTank(10000) {
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
	private String fluidFilter = "";
	public boolean doNextUpdate = false;
	private boolean needBreake = false;

	private int tickCount = 0;

	@Override
	public float getCableConnectionLength(AECableType aeCableType) {
		return 3.0F;
	}

	public boolean canDrain(Fluid fluid) {
		FluidStack tankFluid = tank.getFluid();
		return tankFluid != null && tankFluid.getFluid() == fluid;
	}

	public boolean canFill(Fluid fluid) {
		return tank.fill(new FluidStack(fluid, 1), false) > 0;
	}

	@Override
	public FluidStack drain(FluidStack resource, boolean doDrain) {
		FluidStack tankFluid = tank.getFluid();
		if (resource == null || tankFluid == null || tankFluid.getFluid() != resource.getFluid()) {
			return null;
		}
		return drain(resource.amount, doDrain);
	}

	@Override
	public FluidStack drain(int maxDrain, boolean doDrain) {
		FluidStack drained = tank.drain(maxDrain, doDrain);
		if (drained != null) {
			getHost().markForUpdate();
		}
		doNextUpdate = true;
		return drained;
	}

	@Override
	public int fill(FluidStack resource, boolean doFill) {
		if (resource == null) {
			return 0;
		}

		if ((tank.getFluid() == null || tank.getFluid().getFluid() == resource.getFluid()) && resource.getFluid() == FluidRegistry.getFluid(fluidFilter)) {
			int added = tank.fill(resource.copy(), doFill);
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
			filled += tank.fill(new FluidStack(resource.getFluid(), resource.amount - filled), doFill);
		}
		if (filled > 0) {
			getHost().markForUpdate();
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
		FluidStack copy = resource.copy();
		if (doFill) {
			notRemoved = storage.getFluidInventory().injectItems(AEApi.instance().storage().createFluidStack(resource), Actionable.MODULATE, new MachineSource(this));
		}
		else {
			notRemoved = storage.getFluidInventory().injectItems(AEApi.instance().storage().createFluidStack(resource), Actionable.SIMULATE, new MachineSource(this));
		}
		if (notRemoved == null) {
			return resource.amount;
		}
		return (int) (resource.amount - notRemoved.getStackSize());
	}

	private void forceUpdate() {
		getHost().markForUpdate();
		for (IContainerListener listener : listeners) {
			if (listener != null) {
				listener.updateContainer();
			}
		}
		doNextUpdate = false;
	}

	@Override
	public void getBoxes(IPartCollisionHelper bch) {
		bch.addBox(2, 2, 14, 14, 14, 16);
		bch.addBox(5, 5, 12, 11, 11, 14);

	}

	@Override
	public Object getClientGuiElement(EntityPlayer player) {
		return new GuiFluidInterface(player, this, getSide());
	}

	@SideOnly(Side.CLIENT)
	private World getClientWorld() {
		return Minecraft.getMinecraft().world;
	}

	@Override
	public void getDrops(List<ItemStack> drops, boolean wrenched) {
		for (int i = 0; i < inventory.getSizeInventory(); i++) {
			ItemStack pattern = inventory.getStackInSlot(i);
			if (pattern != null) {
				drops.add(pattern);
			}
		}
	}

	@Override
	public Fluid getFilter(AEPartLocation location) {
		return FluidRegistry.getFluid(fluidFilter);
	}

	@Override
	public IMEMonitor<IAEFluidStack> getFluidInventory() {
		if (getGridNode(AEPartLocation.INTERNAL) == null) {
			return null;
		}
		IGrid grid = getGridNode(AEPartLocation.INTERNAL).getGrid();
		if (grid == null) {
			return null;
		}
		IStorageGrid storage = grid.getCache(IStorageGrid.class);
		if (storage == null) {
			return null;
		}
		return storage.getFluidInventory();
	}

	@Override
	public IFluidTank getFluidTank(AEPartLocation location) {
		return tank;
	}

	@Override
	public IMEMonitor<IAEItemStack> getItemInventory() {
		return new EmptyMeItemMonitor();
	}

	@Override
	public ItemStack getItemStack(PartItemStack type) {
		ItemStack is = new ItemStack(ItemEnum.PARTITEM.getItem(), 1, PartEnum.getPartID(this));
		if (type != PartItemStack.BREAK) {
			is.setTagCompound(writeFilter(new NBTTagCompound()));
		}
		return is;
	}

	/*@Override
	public IStorageMonitorable getMonitorable(AEPartLocation location, BaseActionSource src) {
		return this;
	}*/

	@Override
	public IInventory getPatternInventory() {
		return inventory;
	}

	@Override
	public double getPowerUsage() {
		return 1.0D;
	}

	@Override
	public Object getServerGuiElement(EntityPlayer player) {
		return new ContainerFluidInterface(player, this);
	}

	@Override
	public IFluidTankProperties[] getTankProperties() {
		return tank.getTankProperties();
	}

	@Override
	public TickingRequest getTickingRequest(IGridNode node) {
		return new TickingRequest(1, 40, false, false);
	}

	@Override
	public List<String> getWailaBodey(NBTTagCompound tag, List<String> list) {
		FluidStack fluidStack = null;
		String name = "";
		int amount = 0;
		if (tag.hasKey("fluidName") && tag.hasKey("amount")) {
			name = tag.getString("fluidName");
			amount = tag.getInteger("amount");
		}
		if (!name.isEmpty()) {
			Fluid fluid = FluidRegistry.getFluid(name);
			if (fluid != null) {
				fluidStack = new FluidStack(FluidRegistry.getFluid(name), amount);
			}
		}
		if (fluidStack == null) {
			list.add(I18n.translateToLocal("extracells.tooltip.fluid") + ": " + I18n.translateToLocal("extracells.tooltip.empty1"));
			list.add(I18n.translateToLocal("extracells.tooltip.amount") + ": 0mB / 10000mB");
		}
		else {
			list.add(I18n.translateToLocal("extracells.tooltip.fluid") + ": " + fluidStack.getLocalizedName());
			list.add(I18n.translateToLocal("extracells.tooltip.amount") + ": " + fluidStack.amount + "mB / 10000mB");
		}
		return list;
	}

	@Override
	public NBTTagCompound getWailaTag(NBTTagCompound tag) {
		if (tank.getFluid() == null || tank.getFluid().getFluid() == null) {
			tag.setString("fluidName", "");
		}
		else {
			tag.setString("fluidName", tank.getFluid().getFluid().getName());
		}
		tag.setInteger("amount", tank.getFluidAmount());
		return tag;
	}

	@Override
	public void initializePart(ItemStack partStack) {
		if (partStack.hasTagCompound()) {
			readFilter(partStack.getTagCompound());
		}
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
	public boolean onActivate(EntityPlayer player, EnumHand hand, Vec3d pos) {
		if (PermissionUtil.hasPermission(player, SecurityPermissions.BUILD, (IPart) this)) {
			return super.onActivate(player, hand, pos);
		}
		return false;
	}

	@Override
	public void provideCrafting(ICraftingProviderHelper craftingTracker) {
		patternHandlers = new ArrayList<ICraftingPatternDetails>();
		patternConvert.clear();

		for (ItemStack currentPatternStack : inventory.inv) {
			if (currentPatternStack != null && currentPatternStack.getItem() != null && currentPatternStack.getItem() instanceof ICraftingPatternItem) {
				ICraftingPatternItem currentPattern = (ICraftingPatternItem) currentPatternStack.getItem();

				if (currentPattern != null && currentPattern.getPatternForItem(currentPatternStack, getGridNode().getWorld()) != null) {
					IFluidCraftingPatternDetails pattern = new CraftingPattern2(currentPattern.getPatternForItem(currentPatternStack, getGridNode().getWorld()));
					patternHandlers.add(pattern);
					ItemStack is = makeCraftingPatternItem(pattern);
					if (is == null) {
						continue;
					}
					ICraftingPatternDetails p = ((ICraftingPatternItem) is.getItem()).getPatternForItem(is, getGridNode().getWorld());
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
		if (getGridNode().getWorld() == null || export.isEmpty()) {
			return;
		}
		EnumFacing facing = getFacing();
		BlockPos pos = getGridNode().getGridBlock().getLocation().getPos();
		TileEntity tile = getGridNode().getWorld().getTileEntity(pos.offset(facing));
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
					return;
				}
				if (amount == fluid.getStackSize()) {
					handler.fill(fluid.getFluidStack().copy(), true);
					removeFromExport.add(stack0);
				}
				else {
					IAEFluidStack f = fluid.copy();
					f.setStackSize(f.getStackSize() - amount);
					FluidStack fl = fluid.getFluidStack().copy();
					fl.amount = amount;
					handler.fill(fl, true);
					removeFromExport.add(stack0);
					addToExport.add(f);
					return;
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
			IGrid grid = getGridNode().getGrid();
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
		if (tag.hasKey("filter")) {
			fluidFilter = tag.getString("filter");
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound data) {
		super.readFromNBT(data);
		if (data.hasKey("tank")) {
			tank.readFromNBT(data.getCompoundTag("tank"));
		}
		if (data.hasKey("filter")) {
			fluidFilter = data.getString("filter");
		}
		if (data.hasKey("inventory")) {
			inventory.readFromNBT(data.getCompoundTag("inventory"));
		}
		if (data.hasKey("export")) {
			readOutputFromNBT(data.getCompoundTag("export"));
		}
	}

	@Override
	public boolean readFromStream(ByteBuf data) throws IOException {
		super.readFromStream(data);
		NBTTagCompound tag = ByteBufUtils.readTag(data);
		if (tag.hasKey("tank")) {
			tank.readFromNBT(tag.getCompoundTag("tank"));
		}
		if (tag.hasKey("filter")) {
			fluidFilter = tag.getString("filter");
		}
		if (tag.hasKey("inventory")) {
			inventory.readFromNBT(tag.getCompoundTag("inventory"));
		}
		return true;
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
	public IPartModel getStaticModels() {
		if (isActive() && isPowered()) {
			return PartModels.STORAGE_INTERFACE_HAS_CHANNEL;
		}
		else if (isPowered()) {
			return PartModels.STORAGE_INTERFACE_ON;
		}
		else {
			return PartModels.STORAGE_INTERFACE_OFF;
		}
	}

	@Override
	public void setFilter(AEPartLocation side, Fluid fluid) {
		if (fluid == null) {
			fluidFilter = "";
			doNextUpdate = true;
			return;
		}
		fluidFilter = fluid.getName();
		doNextUpdate = true;

	}

	@Override
	public void setFluid(int index, Fluid fluid, EntityPlayer player) {
		setFilter(AEPartLocation.fromOrdinal(index), fluid);
	}

	@Override
	public void setFluidTank(AEPartLocation side, FluidStack fluid) {
		tank.setFluid(fluid);
		doNextUpdate = true;
	}

	@Override
	public TickRateModulation tickingRequest(IGridNode node, int TicksSinceLastCall) {
		if (doNextUpdate) {
			forceUpdate();
		}
		IGrid grid = node.getGrid();
		if (grid == null) {
			return TickRateModulation.URGENT;
		}
		IStorageGrid storage = grid.getCache(IStorageGrid.class);
		if (storage == null) {
			return TickRateModulation.URGENT;
		}
		pushItems();
		if (toExport != null) {
			storage.getItemInventory().injectItems(toExport, Actionable.MODULATE, new MachineSource(this));
			toExport = null;
		}
		if (update) {
			update = false;
			if (getGridNode() != null && getGridNode().getGrid() != null) {
				getGridNode().getGrid().postEvent(new MENetworkCraftingPatternChange(this, getGridNode()));
			}
		}
		if (tank.getFluid() != null && FluidRegistry.getFluid(fluidFilter) != tank.getFluid().getFluid()) {
			FluidStack s = tank.drain(125, false);
			if (s != null) {
				IAEFluidStack notAdded = storage.getFluidInventory().injectItems(AEApi.instance().storage().createFluidStack(s.copy()), Actionable.SIMULATE, new MachineSource(this));
				if (notAdded != null) {
					int toAdd = (int) (s.amount - notAdded.getStackSize());
					storage.getFluidInventory().injectItems(AEApi.instance().storage().createFluidStack(tank.drain(toAdd, true)), Actionable.MODULATE, new MachineSource(this));
					doNextUpdate = true;
					needBreake = false;
				}
				else {
					storage.getFluidInventory().injectItems(AEApi.instance().storage().createFluidStack(tank.drain(s.amount, true)), Actionable.MODULATE, new MachineSource(this));
					doNextUpdate = true;
					needBreake = false;
				}
			}
		}
		if ((tank.getFluid() == null || tank.getFluid().getFluid() == FluidRegistry.getFluid(fluidFilter)) && FluidRegistry.getFluid(fluidFilter) != null) {
			IAEFluidStack extracted = storage.getFluidInventory().extractItems(AEApi.instance().storage().createFluidStack(new FluidStack(FluidRegistry.getFluid(fluidFilter), 125)), Actionable.SIMULATE, new MachineSource(this));
			if (extracted == null) {
				return TickRateModulation.URGENT;
			}
			int accepted = tank.fill(extracted.getFluidStack(), false);
			if (accepted == 0) {
				return TickRateModulation.URGENT;
			}
			tank.fill(storage.getFluidInventory().extractItems(AEApi.instance().storage().createFluidStack(new FluidStack(FluidRegistry.getFluid(fluidFilter), accepted)), Actionable.MODULATE, new MachineSource(this)).getFluidStack(), true);
			doNextUpdate = true;
			needBreake = false;
		}
		return TickRateModulation.URGENT;
	}

	public NBTTagCompound writeFilter(NBTTagCompound tag) {
		if (FluidRegistry.getFluid(fluidFilter) == null) {
			return null;
		}
		tag.setString("filter", fluidFilter);
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
	public void writeToNBT(NBTTagCompound data) {
		writeToNBTWithoutExport(data);
		NBTTagCompound tag = new NBTTagCompound();
		writeOutputToNBT(tag);
		data.setTag("export", tag);
	}

	public void writeToNBTWithoutExport(NBTTagCompound data) {
		super.writeToNBT(data);
		data.setTag("tank", tank.writeToNBT(new NBTTagCompound()));
		data.setString("filter", fluidFilter);
		NBTTagCompound inventory = new NBTTagCompound();
		this.inventory.writeToNBT(inventory);
		data.setTag("inventory", inventory);
	}

	@Override
	public void writeToStream(ByteBuf data) throws IOException {
		super.writeToStream(data);
		NBTTagCompound tag = new NBTTagCompound();
		tag.setTag("tank", tank.writeToNBT(new NBTTagCompound()));
		tag.setString("filter", fluidFilter);
		NBTTagCompound inventory = new NBTTagCompound();
		this.inventory.writeToNBT(inventory);
		tag.setTag("inventory", inventory);
		ByteBufUtils.writeTag(data, tag);
	}

	@Override
	public <T> T getCapability(Capability<T> capability) {
		if (capability == Capabilities.STORAGE_MONITORABLE_ACCESSOR) {
			return Capabilities.STORAGE_MONITORABLE_ACCESSOR.cast((m) -> this);
		}
		if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
			return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(this);
		}
		return super.getCapability(capability);
	}

	@Override
	public boolean hasCapability(Capability<?> capability) {
		return capability == Capabilities.STORAGE_MONITORABLE_ACCESSOR || capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY;
	}
}
