package extracells.util.recipe;

import java.util.List;

import com.google.common.collect.Lists;

import appeng.api.features.INetworkEncodable;
import appeng.api.implementations.items.IAEItemPowerStorage;
import extracells.item.ItemWirelessTerminalUniversal;
import extracells.item.WirelessTerminalType;
import extracells.registries.ItemEnum;
import extracells.util.UniversalTerminal;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;

public class RecipeUniversalTerminal implements IRecipe {

	private ItemWirelessTerminalUniversal itemUniversal = (ItemWirelessTerminalUniversal) ItemEnum.UNIVERSALTERMINAL.getItem();

	@Override
	public boolean matches(InventoryCrafting inventory, World worldIn) {
		boolean hasWireless = false;
		boolean isUniversal = false;
		boolean hasTerminal = false;
		List<WirelessTerminalType> terminals = Lists.<WirelessTerminalType>newArrayList();
		ItemStack terminal = null;
		int size = inventory.getSizeInventory();
		for (int i = 0; i < size; i++) {
			ItemStack stack = inventory.getStackInSlot(i);
			if (stack != null) {
				Item item = stack.getItem();
				if (item == itemUniversal) {
					if (hasWireless) {
						return false;
					}
					else {
						hasWireless = true;
						isUniversal = true;
						terminal = stack;
					}
				}
				else if (UniversalTerminal.isWirelessTerminal(stack)) {
					if (hasWireless) {
						return false;
					}
					hasWireless = true;
					terminal = stack;
				}
				else if (UniversalTerminal.isTerminal(stack)) {
					hasTerminal = true;
					WirelessTerminalType typeTerminal = UniversalTerminal.getTerminalType(stack);
					if (terminals.contains(typeTerminal)) {
						return false;
					}
					else {
						terminals.add(typeTerminal);
					}
				}
			}
		}
		if (!(hasTerminal && hasWireless)) {
			return false;
		}
		if (isUniversal) {
			for (WirelessTerminalType x : terminals) {
				if (itemUniversal.isInstalled(terminal, x)) {
					return false;
				}
			}
			return true;
		}
		else {
			WirelessTerminalType terminalType = UniversalTerminal.getTerminalType(terminal);
			for (WirelessTerminalType x : terminals) {
				if (x == terminalType) {
					return false;
				}
			}
			return true;
		}
	}

	@Override
	public ItemStack getCraftingResult(InventoryCrafting inventory) {
		boolean isUniversal = false;
		List<WirelessTerminalType> terminals = Lists.<WirelessTerminalType>newArrayList();
		ItemStack terminal = null;
		int size = inventory.getSizeInventory();
		for (int i = 0; i < size; i++) {
			ItemStack stack = inventory.getStackInSlot(i);
			if (stack != null) {
				Item item = stack.getItem();
				if (item == itemUniversal) {
					isUniversal = true;
					terminal = stack.copy();
				}
				else if (UniversalTerminal.isWirelessTerminal(stack)) {
					terminal = stack.copy();
				}
				else if (UniversalTerminal.isTerminal(stack)) {
					WirelessTerminalType typeTerminal = UniversalTerminal.getTerminalType(stack);
					terminals.add(typeTerminal);

				}
			}
		}
		if (isUniversal) {
			for (WirelessTerminalType x : terminals) {
				itemUniversal.installModule(terminal, x);
			}
		}
		else {
			WirelessTerminalType terminalType = UniversalTerminal.getTerminalType(terminal);
			Item itemTerminal = terminal.getItem();
			ItemStack t = new ItemStack(itemUniversal);
			if (itemTerminal instanceof INetworkEncodable) {
				String key = ((INetworkEncodable) itemTerminal).getEncryptionKey(terminal);
				if (key != null) {
					itemUniversal.setEncryptionKey(t, key, null);
				}
			}
			if (itemTerminal instanceof IAEItemPowerStorage) {
				double power = ((IAEItemPowerStorage) itemTerminal).getAECurrentPower(terminal);
				itemUniversal.injectAEPower(t, power);
			}
			if (terminal.hasTagCompound()) {
				NBTTagCompound nbt = terminal.getTagCompound();
				if (!t.hasTagCompound()) {
					t.setTagCompound(new NBTTagCompound());
				}
				if (nbt.hasKey("BoosterSlot")) {
					t.getTagCompound().setTag("BoosterSlot", nbt.getTag("BoosterSlot"));
				}
				if (nbt.hasKey("MagnetSlot")) {
					t.getTagCompound().setTag("MagnetSlot", nbt.getTag("MagnetSlot"));
				}
			}
			if (terminalType == null) {
				return null;
			}
			itemUniversal.installModule(t, terminalType);
			t.getTagCompound().setByte("type", (byte) terminalType.ordinal());
			terminal = t;
			for (WirelessTerminalType x : terminals) {
				itemUniversal.installModule(terminal, x);
			}
		}
		return terminal;
	}

	@Override
	public int getRecipeSize() {
		return 2;
	}

	@Override
	public ItemStack getRecipeOutput() {
		return ItemEnum.UNIVERSALTERMINAL.getDamagedStack(0);
	}

	@Override
	public ItemStack[] getRemainingItems(InventoryCrafting inv) {
		return ForgeHooks.defaultRecipeGetRemainingItems(inv);
	}

}
