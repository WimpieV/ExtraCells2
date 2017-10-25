package extracells.integration.WirelessCrafting;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;
import p455w0rd.wct.api.IWirelessCraftingTerminalItem;
import p455w0rd.wct.api.WCTApi;

public class WirelessCrafting {

	public static boolean isLoaded() {
		return Loader.isModLoaded("wct");
	}

	public static void openCraftingTerminal(EntityPlayer player) {
		WCTApi.instance().interact().openWirelessCraftingTerminalGui(player);
	}

	public static Item getBoosterItem() {
		return WCTApi.instance().items().infinityBoosterCard().getItem();
	}

	public static boolean isBoosterEnabled() {
		return WCTApi.isInfinityBoosterCardEnabled();
	}

	public static ItemStack getCraftingTerminal() {
		return WCTApi.instance().items().wirelessCraftingTerminal().getStack();
	}

	public static IWirelessCraftingTerminalItem getCraftingTerminalItem() {
		return (IWirelessCraftingTerminalItem) WCTApi.instance().items().wirelessCraftingTerminal().getItem();
	}

}