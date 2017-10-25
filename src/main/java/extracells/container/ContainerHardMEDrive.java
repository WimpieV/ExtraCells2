package extracells.container;

import appeng.api.AEApi;
import extracells.container.slot.SlotRespective;
import extracells.tileentity.TileEntityHardMeDrive;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerHardMEDrive extends Container {

	TileEntityHardMeDrive tile;
	InventoryPlayer inventory;

	public ContainerHardMEDrive(InventoryPlayer inventory, TileEntityHardMeDrive tile) {

		this.tile = tile;
		this.inventory = inventory;

		for (int i = 0; i < 2; i++) {
			addSlotToContainer(new SlotRespective(tile.getInventory(), i, 80, 17 + i * 18) {

				@Override
				public boolean isItemValid(ItemStack item) {
					return AEApi.instance().registries().cell().isCellHandled(item);
				}

			});
		}

		bindPlayerInventory(inventory);
	}

	protected void bindPlayerInventory(IInventory inventoryPlayer) {
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 9; j++) {
				addSlotToContainer(new Slot(inventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
			}
		}

		for (int i = 0; i < 9; i++) {
			addSlotToContainer(new Slot(inventory, i, 8 + i * 18, 142));
		}
	}

	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return tile != null && tile.hasWorld() && tile.isUseableByPlayer(player);
	}

	@Override
	protected void retrySlotClick(int slotId, int clickedButton, boolean mode, EntityPlayer playerIn) {

	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int i) {
		ItemStack itemstack = null;
		Slot slot = inventorySlots.get(i);
		if (slot != null && slot.getHasStack()) {
			ItemStack itemstack1 = slot.getStack();
			itemstack = itemstack1.copy();
			if (AEApi.instance().registries().cell().isCellHandled(itemstack)) {
				if (i < 3) {
					if (!mergeItemStack(itemstack1, 3, 38, false)) {
						return null;
					}
					else if (!mergeItemStack(itemstack1, 0, 3, false)) {
						return null;
					}
					if (itemstack1.stackSize == 0) {
						slot.putStack(null);
					}
					else {
						slot.onSlotChanged();
					}
				}
			}
		}
		return itemstack;
	}

}