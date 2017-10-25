package extracells.util;

import javax.annotation.Nonnull;

import appeng.api.AEApi;
import extracells.integration.WirelessCrafting.WirelessCrafting;
import extracells.item.WirelessTerminalType;
import extracells.registries.ItemEnum;
import extracells.registries.PartEnum;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * @author p455w0rd
 *
 */
public class UniversalTerminal {

	public static boolean isWirelessTerminal(@Nonnull ItemStack stack) {
		if (stack == null) {
			return false;
		}
		Item item = stack.getItem();
		int meta = stack.getItemDamage();
		if (item == null) {
			return false;
		}
		ItemStack aeterm = AEApi.instance().definitions().items().wirelessTerminal().maybeStack(1).get();
		if (item == aeterm.getItem() && meta == aeterm.getItemDamage()) {
			return true;
		}
		ItemStack ecterm = ItemEnum.FLUIDWIRELESSTERMINAL.getDamagedStack(0);
		if (item == ecterm.getItem() && meta == ecterm.getItemDamage()) {
			return true;
		}
		ItemStack ectermgas = ItemEnum.GASWIRELESSTERMINAL.getDamagedStack(0);
		if (item == ectermgas.getItem() && meta == ectermgas.getItemDamage()) {
			return true;
		}
		/*if(Mods.THAUMATICENERGISTICS.isEnabled){
		  val thterm = ThaumaticEnergistics.getWirelessTerminal
		  if(item == thterm.getItem && meta == thterm.getItemDamage)
		    return true
		}*/
		if (WirelessCrafting.isLoaded()) {
			ItemStack wcTerm = WirelessCrafting.getCraftingTerminal();
			if (item == wcTerm.getItem() && meta == wcTerm.getItemDamage()) {
				return true;
			}
		}
		return false;
	}

	public static boolean isTerminal(ItemStack stack) {
		if (stack == null) {
			return false;
		}
		Item item = stack.getItem();
		int meta = stack.getItemDamage();
		if (item == null) {
			return false;
		}
		ItemStack aeterm = AEApi.instance().definitions().items().wirelessTerminal().maybeStack(1).get();
		if (item == aeterm.getItem() && meta == aeterm.getItemDamage()) {
			return true;
		}
		ItemStack ecterm = ItemEnum.PARTITEM.getDamagedStack(PartEnum.FLUIDTERMINAL.ordinal());
		if (item == ecterm.getItem() && meta == ecterm.getItemDamage()) {
			return true;
		}
		/*
				  ItemStack ectermgas = ItemEnum.PARTITEM.getDamagedStack(PartEnum.GASTERMINAL.ordinal());
		if(item == ectermgas.getItem() && meta == ectermgas.getItemDamage()) {
		  return true;
		}
		if(Mods.THAUMATICENERGISTICS.isEnabled){
		  val thterm = ThaumaticEnergistics.getTerminal
		  if(item == thterm.getItem && meta == thterm.getItemDamage)
		    return true
		}*/
		return false;
	}

	public static WirelessTerminalType getTerminalType(ItemStack stack) {
		if (stack == null) {
			return null;
		}
		Item item = stack.getItem();
		int meta = stack.getItemDamage();
		if (item == null) {
			return null;
		}
		ItemStack aeterm = AEApi.instance().definitions().parts().terminal().maybeStack(1).get();
		if (item == aeterm.getItem() && meta == aeterm.getItemDamage()) {
			return WirelessTerminalType.ITEM;
		}
		ItemStack ecterm = ItemEnum.PARTITEM.getDamagedStack(PartEnum.FLUIDTERMINAL.ordinal());
		if (item == ecterm.getItem() && meta == ecterm.getItemDamage()) {
			return WirelessTerminalType.FLUID;
		}
		/*
		ItemStack ectermgas = ItemEnum.PARTITEM.getDamagedStack(PartEnum.GASTERMINAL.ordinal());
		if(item == ectermgas.getItem() && meta == ectermgas.getItemDamage()) {
		  return WirelessTerminalType.GAS;
		}
		if(Mods.THAUMATICENERGISTICS.isEnabled){
		  val thterm = ThaumaticEnergistics.getTerminal
		  if(item == thterm.getItem && meta == thterm.getItemDamage)
		    return TerminalType.ESSENTIA
		}*/
		ItemStack aeterm2 = AEApi.instance().definitions().items().wirelessTerminal().maybeStack(1).get();
		if (item == aeterm2.getItem() && meta == aeterm2.getItemDamage()) {
			return WirelessTerminalType.ITEM;
		}
		ItemStack ecterm2 = ItemEnum.FLUIDWIRELESSTERMINAL.getDamagedStack(0);
		if (item == ecterm2.getItem() && meta == ecterm2.getItemDamage()) {
			return WirelessTerminalType.FLUID;
		}
		ItemStack ectermgas2 = ItemEnum.GASWIRELESSTERMINAL.getDamagedStack(0);
		if (item == ectermgas2.getItem() && meta == ectermgas2.getItemDamage()) {
			return WirelessTerminalType.GAS;
		}
		/*if(Mods.THAUMATICENERGISTICS.isEnabled){
		  val thterm = ThaumaticEnergistics.getWirelessTerminal
		  if(item == thterm.getItem && meta == thterm.getItemDamage)
		    return TerminalType.ESSENTIA
		}*/
		//if (Integration.Mods.WIRELESSCRAFTING.isEnabled()) {
		ItemStack wcTerm = WirelessCrafting.getCraftingTerminal();
		if (item == wcTerm.getItem() && meta == wcTerm.getItemDamage()) {
			return WirelessTerminalType.CRAFTING;
		}
		//}
		return null;
	}

}
