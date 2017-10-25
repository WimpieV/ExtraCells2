package extracells.container;

import javax.annotation.Nullable;

import appeng.api.AEApi;
import appeng.api.config.SecurityPermissions;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.storage.IBaseMonitor;
import appeng.api.parts.IPart;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.IMEMonitorHandlerReceiver;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IItemList;
import extracells.container.slot.SlotOutput;
import extracells.container.slot.SlotRespective;
import extracells.network.packet.part.PacketTerminalSelectFluidServer;
import extracells.network.packet.part.PacketTerminalUpdateFluid;
import extracells.part.fluid.PartFluidTerminal;
import extracells.util.NetworkUtil;
import extracells.util.PermissionUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;

public class ContainerTerminal extends Container implements IMEMonitorHandlerReceiver<IAEFluidStack>, IFluidSelectorContainer {
	private PartFluidTerminal terminal;
	private IMEMonitor<IAEFluidStack> monitor;
	private IItemList<IAEFluidStack> fluidStackList;
	private Fluid selectedFluid;
	private EntityPlayer player;
	private StorageType type;

	public ContainerTerminal(PartFluidTerminal terminal, EntityPlayer player, StorageType type) {
		this.terminal = terminal;
		this.player = player;
		this.type = type;
		fluidStackList = AEApi.instance().storage().createFluidList();
		if (!this.player.world.isRemote) {
			monitor = this.terminal.getGridBlock().getFluidMonitor();
			if (monitor != null) {
				monitor.addListener(this, null);
				fluidStackList = monitor.getStorageList();
			}
			this.terminal.addContainer(this);
		}

		// Input Slot accepts all FluidContainers
		addSlotToContainer(new SlotRespective(this.terminal.getInventory(), 0, 8, 92));
		// Output Slot accepts nothing
		addSlotToContainer(new SlotOutput(this.terminal.getInventory(), 1, 26, 92));
		bindPlayerInventory(this.player.inventory);
	}

	protected void bindPlayerInventory(InventoryPlayer inventoryPlayer) {
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 9; j++) {
				addSlotToContainer(new Slot(inventoryPlayer, j + i * 9 + 9, 8 + j * 18, i * 18 + 122));
			}
		}

		for (int i = 0; i < 9; i++) {
			addSlotToContainer(new Slot(inventoryPlayer, i, 8 + i * 18, 180));
		}
	}

	@Override
	public boolean canInteractWith(EntityPlayer entityplayer) {
		if (terminal == null) {
			return false;
		}
		return terminal.isValid();
	}

	public void forceFluidUpdate() {
		if (monitor != null) {
			NetworkUtil.sendToPlayer(new PacketTerminalUpdateFluid(monitor.getStorageList()), player);
		}
	}

	public IItemList<IAEFluidStack> getFluidStackList() {
		return fluidStackList;
	}

	public EntityPlayer getPlayer() {
		return player;
	}

	public Fluid getSelectedFluid() {
		return selectedFluid;
	}

	public PartFluidTerminal getTerminal() {
		return terminal;
	}

	@Override
	public boolean isValid(Object verificationToken) {
		return true;
	}

	@Override
	public void onContainerClosed(EntityPlayer entityPlayer) {
		super.onContainerClosed(entityPlayer);
		if (!entityPlayer.world.isRemote) {
			if (monitor != null) {
				monitor.removeListener(this);
			}
			terminal.removeContainer(this);
		}
	}

	@Override
	public void onListUpdate() {

	}

	@Override
	public void postChange(IBaseMonitor<IAEFluidStack> monitor, Iterable<IAEFluidStack> change, BaseActionSource actionSource) {
		fluidStackList = ((IMEMonitor<IAEFluidStack>) monitor).getStorageList();
		NetworkUtil.sendToPlayer(new PacketTerminalUpdateFluid(fluidStackList), player);
	}

	public void receiveSelectedFluid(Fluid _selectedFluid) {
		selectedFluid = _selectedFluid;
	}

	@Override
	public void setSelectedFluid(Fluid fluid) {
		NetworkUtil.sendToServer(new PacketTerminalSelectFluidServer(fluid, terminal));
	}

	@Nullable
	@Override
	public ItemStack slotClick(int slotId, int dragType, ClickType clickTypeIn, EntityPlayer player) {
		/*
		ItemStack returnStack = null;
		boolean hasPermission = true;
		if (slotId == 0 || slotId == 1) {
			ItemStack stack = player.inventory.getItemStack();
			if (stack == null) {
			}
			else {
				if (type.isEmpty(stack) && PermissionUtil.hasPermission(player, SecurityPermissions.INJECT, (IPart) getTerminal())) {
				}
				else if (type.isFilled(stack) && PermissionUtil.hasPermission(player, SecurityPermissions.EXTRACT, (IPart) getTerminal())) {
				}
				else {
					ItemStack slotStack = this.inventorySlots.get(slotId).getStack();
					if (slotStack == null) {
						returnStack = null;
					}
					else {
						returnStack = slotStack.copy();
					}
					hasPermission = false;
				}
			}
		}
		*/
		if (slotId > 1 && slotId < 38) {
			return super.slotClick(slotId, dragType, clickTypeIn, player);
		}
		ItemStack returnStack = null;
		boolean hasPermission = false;
		if (slotId == 0 && PermissionUtil.hasPermission(player, SecurityPermissions.INJECT, (IPart) getTerminal())) {
			hasPermission = true;
		}
		else if (slotId == 1 && PermissionUtil.hasPermission(player, SecurityPermissions.EXTRACT, (IPart) getTerminal())) {
			hasPermission = true;
		}
		if (hasPermission) {
			returnStack = super.slotClick(slotId, dragType, clickTypeIn, player);
		}
		if (player instanceof EntityPlayerMP) {
			EntityPlayerMP p = (EntityPlayerMP) player;
			p.sendContainerToPlayer(this);
		}

		return returnStack;
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int slotnumber) {
		ItemStack itemstack = null;
		Slot slot = inventorySlots.get(slotnumber);
		if (slot != null && slot.getHasStack()) {
			ItemStack itemstack1 = slot.getStack();
			itemstack = itemstack1.copy();
			if (terminal.getInventory().isItemValidForSlot(0, itemstack1)) {
				if (slotnumber == 1 || slotnumber == 0) {
					if (!mergeItemStack(itemstack1, 2, 36, false)) {
						return null;
					}
				}
				else if (!mergeItemStack(itemstack1, 0, 1, false)) {
					return null;
				}
				if (itemstack1.stackSize == 0) {
					slot.putStack(null);
				}
				else {
					slot.onSlotChanged();
				}
			}
			else {
				return null;
			}
		}
		return itemstack;
	}

	public void updateFluidList(IItemList<IAEFluidStack> fluidStacks) {
		fluidStackList = fluidStacks;
	}
}
