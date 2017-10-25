package extracells.item;

import extracells.integration.WirelessCrafting.WirelessCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Optional;
import p455w0rd.wct.api.IWirelessCraftingTerminalItem;

@Optional.Interface(iface = "net.p455w0rd.wct.api.IWirelessCraftingTerminalItem", modid = "wct", striprefs = true)
public class CraftingTerminal extends WirelessTermBase implements IWirelessCraftingTerminalItem {

	@Override
	@Optional.Method(modid = "wct")
	public boolean checkForBooster(ItemStack wirelessTerminal) {
		return wirelessTerminal != null && WirelessCrafting.getCraftingTerminalItem().checkForBooster(wirelessTerminal);
	}

	@Override
	@Optional.Method(modid = "wct")
	public boolean isWirelessCraftingEnabled(ItemStack itemStack) {
		if (this instanceof ItemWirelessTerminalUniversal) {
			return ((ItemWirelessTerminalUniversal) this).isInstalled(itemStack, WirelessTerminalType.CRAFTING);
		}
		return false;
	}

	@Override
	public Item getItem() {
		return this;
	}

	@Override
	public ItemStack getStack() {
		return new ItemStack(this);
	}

}