package extracells.item.storage;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import extracells.integration.Integration;
import extracells.item.ItemECBase;
import extracells.models.ModelManager;

public class ItemStorageCasing extends ItemECBase {

	public ItemStorageCasing() {
		setMaxDamage(0);
		setHasSubtypes(true);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void getSubItems(Item item, CreativeTabs creativeTab, List itemList) {
		for (CellDefinition definition : CellDefinition.values()) {
			if(definition == CellDefinition.GAS && !Integration.Mods.MEKANISMGAS.isEnabled()){
				continue;
			}
			itemList.add(new ItemStack(item, 1, definition.ordinal()));
		}
	}

	@Override
	public String getUnlocalizedName(ItemStack itemStack) {
		CellDefinition definition = CellDefinition.get(itemStack.getItemDamage());
		return "extracells.item.storage.casing." + definition;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerModel(Item item, ModelManager manager) {
		for (CellDefinition definition : CellDefinition.values()) {
			manager.registerItemModel(item, definition.ordinal(), "storage/" + definition + "/casing");
		}
	}
}
