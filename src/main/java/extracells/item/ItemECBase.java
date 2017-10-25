package extracells.item;

import extracells.models.IItemModelRegister;
import extracells.models.ModelManager;
import extracells.util.CreativeTabEC;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ItemECBase extends Item implements IItemModelRegister {
	public ItemECBase() {
		setCreativeTab(CreativeTabEC.INSTANCE);
	}

	@Override
	public void registerModel(Item item, ModelManager manager) {
		manager.registerItemModel(item);
	}

	@Override
	public String getUnlocalizedName(ItemStack itemStack) {
		return super.getUnlocalizedName(itemStack).replace("item.extracells", "extracells.item");
	}
}
