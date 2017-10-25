package extracells.inventory;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

public class ECCellInventory implements IInventory {

	private ItemStack storage;
	private String tagId;
	private NBTTagCompound tagCompound;
	private int size;
	private int stackLimit;
	private ItemStack[] slots;
	private boolean dirty = false;

	public ECCellInventory(ItemStack storage, String tagId, int size, int stackLimit) {
		this.storage = storage;
		this.tagId = tagId;
		this.size = size;
		this.stackLimit = stackLimit;
		if (!this.storage.hasTagCompound()) {
			this.storage.setTagCompound(new NBTTagCompound());
		}
		this.storage.getTagCompound().setTag(this.tagId, this.storage.getTagCompound().getCompoundTag(this.tagId));
		tagCompound = this.storage.getTagCompound().getCompoundTag(this.tagId);
		openInventory();
	}

	@Override
	public void closeInventory(EntityPlayer player) {
		closeInventory();
	}

	private void closeInventory() {
		if (dirty) {
			for (int i = 0; i < slots.length; i++) {
				tagCompound.removeTag("ItemStack#" + i);
				ItemStack content = slots[i];
				if (content != null) {
					tagCompound.setTag("ItemStack#" + i, new NBTTagCompound());
					content.writeToNBT(tagCompound.getCompoundTag("ItemStack#" + i));
				}
			}
		}
	}

	@Override
	public ItemStack decrStackSize(int slotId, int amount) {
		ItemStack itemStack = ItemStackHelper.getAndSplit(slots, slotId, amount);

		if (itemStack != null) {
			markDirty();
		}

		return itemStack;
	}

	@Override
	public String getName() {
		return "";
	}

	@Override
	public int getInventoryStackLimit() {
		return stackLimit;
	}

	@Override
	public int getSizeInventory() {
		return size;
	}

	@Override
	public ItemStack getStackInSlot(int slotId) {
		return slots[slotId];
	}

	@Nullable
	@Override
	public ItemStack removeStackFromSlot(int index) {
		return ItemStackHelper.getAndRemove(slots, index);
	}

	@Override
	public boolean hasCustomName() {
		return false;
	}

	@Override
	public boolean isItemValidForSlot(int slotId, ItemStack itemStack) {
		return true;
	}

	@Override
	public boolean isUsableByPlayer(EntityPlayer entityPlayer) {
		return true;
	}

	@Override
	public void markDirty() {
		dirty = true;
		closeInventory();
		dirty = false;
	}

	@Override
	public void openInventory(EntityPlayer player) {
		slots = new ItemStack[size];
		for (int i = 0; i < slots.length; i++) {
			slots[i] = ItemStack.loadItemStackFromNBT(tagCompound.getCompoundTag("ItemStack#" + i));
		}
	}

	private void openInventory() {
		slots = new ItemStack[size];
		for (int i = 0; i < slots.length; i++) {
			slots[i] = ItemStack.loadItemStackFromNBT(tagCompound.getCompoundTag("ItemStack#" + i));
		}
	}

	@Override
	public void setInventorySlotContents(int slotId, ItemStack content) {
		ItemStack slotContent = slots[slotId];
		if (slotContent != content) {
			slots[slotId] = content;
			markDirty();
		}
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
		for (int i = 0; i < slots.length; i++) {
			slots[i] = null;
		}
	}

	@Override
	public ITextComponent getDisplayName() {
		return new TextComponentString(getName());
	}
}
