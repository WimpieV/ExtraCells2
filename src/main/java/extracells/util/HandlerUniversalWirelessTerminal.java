package extracells.util;

import appeng.api.features.IWirelessTermHandler;
import appeng.api.util.IConfigManager;
import extracells.api.ECApi;
import extracells.api.IWirelessFluidTermHandler;
import extracells.api.IWirelessGasTermHandler;
import extracells.item.ItemWirelessTerminalUniversal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class HandlerUniversalWirelessTerminal implements IWirelessTermHandler, IWirelessFluidTermHandler, IWirelessGasTermHandler {

	@Override
	public IConfigManager getConfigManager(ItemStack is) {
		return ((ItemWirelessTerminalUniversal) ECApi.instance().items().wirelessFluidTerminal()).getConfigManager(is);
	}
	/*
	override def getConfigManager(is: ItemStack): IConfigManager = ItemWirelessTerminalUniversal.getConfigManager(is)
	
	override def canHandle(is: ItemStack): Boolean = ItemWirelessTerminalUniversal.canHandle(is)
	
	override def usePower(player: EntityPlayer, amount: Double, is: ItemStack): Boolean = ItemWirelessTerminalUniversal.usePower(player, amount, is)
	
	override def hasPower(player: EntityPlayer, amount: Double, is: ItemStack): Boolean = ItemWirelessTerminalUniversal.hasPower(player, amount, is)
	
	override def isItemNormalWirelessTermToo(is: ItemStack): Boolean = ItemWirelessTerminalUniversal.isItemNormalWirelessTermToo(is)
	
	override def setEncryptionKey(item: ItemStack, encKey: String, name: String): Unit = ItemWirelessTerminalUniversal.setEncryptionKey(item, encKey, name)
	
	override def getEncryptionKey(item: ItemStack): String = ItemWirelessTerminalUniversal.getEncryptionKey(item)
	*/

	@Override
	public String getEncryptionKey(ItemStack arg0) {
		return null;
	}

	@Override
	public void setEncryptionKey(ItemStack arg0, String arg1, String arg2) {
	}

	@Override
	public boolean isItemNormalWirelessTermToo(ItemStack is) {
		return false;
	}

	@Override
	public boolean canHandle(ItemStack arg0) {
		return false;
	}

	@Override
	public boolean hasPower(EntityPlayer arg0, double arg1, ItemStack arg2) {
		return false;
	}

	@Override
	public boolean usePower(EntityPlayer arg0, double arg1, ItemStack arg2) {
		return false;
	}
}
