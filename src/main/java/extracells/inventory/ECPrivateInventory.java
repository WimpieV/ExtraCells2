package extracells.inventory;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

public class ECPrivateInventory implements IInventory {

	public ItemStack[] slots;
	public String customName;
	private int stackLimit;
	@Nullable
	private IInventoryListener listener;

	public ECPrivateInventory(String customName, int size, int stackLimit) {
		this(customName, size, stackLimit, null);
	}

	public ECPrivateInventory(String customName, int size, int stackLimit, IInventoryListener listener) {
		slots = new ItemStack[size];
		this.customName = customName;
		this.stackLimit = stackLimit;
		this.listener = listener;
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
	public int getInventoryStackLimit() {
		return stackLimit;
	}

	@Override
	public int getSizeInventory() {
		return slots.length;
	}

	@Override
	public ItemStack getStackInSlot(int i) {
		return slots[i];
	}

	@Nullable
	@Override
	public ItemStack removeStackFromSlot(int index) {
		return ItemStackHelper.getAndRemove(slots, index);
	}

	@Override
	public String getName() {
		return customName;
	}

	@Override
	public boolean hasCustomName() {
		return false;
	}

	@Override
	public ITextComponent getDisplayName() {
		return new TextComponentString(getName());
	}

	/**
	 * Increases the stack size of a slot.
	 *
	 * @param slotId
	 *        ID of the slot
	 * @param amount
	 *        amount to be drained
	 *
	 * @return the added Stack
	 */
	public ItemStack incrStackSize(int slotId, int amount) {
		ItemStack slot = slots[slotId];
		if (slot == null) {
			return null;
		}
		int stackLimit = getInventoryStackLimit();
		if (stackLimit > slot.getMaxStackSize()) {
			stackLimit = slot.getMaxStackSize();
		}
		ItemStack added = slot.copy();
		added.stackSize = slot.stackSize + amount > stackLimit ? stackLimit : amount;
		slot.stackSize += added.stackSize;
		return added;
	}

	@Override
	public boolean isItemValidForSlot(int i, ItemStack itemstack) {
		return true;
	}

	@Override
	public boolean isUsableByPlayer(EntityPlayer entityplayer) {
		return true;
	}

	@Override
	public void markDirty() {
		if (listener != null) {
			listener.onInventoryChanged();
		}
	}

	@Override
	public void openInventory(EntityPlayer player) {
		// NOBODY needs this!
	}

	@Override
	public void closeInventory(EntityPlayer player) {
		// NOBODY needs this!
	}

	public void readFromNBT(NBTTagList nbtList) {
		if (nbtList == null) {
			for (int i = 0; i < slots.length; i++) {
				slots[i] = null;
			}
			return;
		}
		for (int i = 0; i < nbtList.tagCount(); ++i) {
			NBTTagCompound nbttagcompound = nbtList.getCompoundTagAt(i);
			int j = nbttagcompound.getByte("Slot") & 255;

			if (j >= 0 && j < slots.length) {
				slots[j] = ItemStack.loadItemStackFromNBT(nbttagcompound);
			}
		}
	}

	@Override
	public void setInventorySlotContents(int index, ItemStack stack) {
		if (stack != null && stack.stackSize > getInventoryStackLimit()) {
			stack.stackSize = getInventoryStackLimit();
		}
		slots[index] = stack;

		markDirty();
	}

	public NBTTagList writeToNBT() {
		NBTTagList nbtList = new NBTTagList();

		for (int i = 0; i < slots.length; ++i) {
			if (slots[i] != null) {
				NBTTagCompound nbttagcompound = new NBTTagCompound();
				nbttagcompound.setByte("Slot", (byte) i);
				slots[i].writeToNBT(nbttagcompound);
				nbtList.appendTag(nbttagcompound);
			}
		}
		return nbtList;
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
}
