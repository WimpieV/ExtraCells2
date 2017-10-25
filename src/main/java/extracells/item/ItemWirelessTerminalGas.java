package extracells.item;

import extracells.api.ECApi;
import extracells.api.IWirelessGasTermHandler;
import extracells.models.ModelManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author p455w0rd
 *
 */
public class ItemWirelessTerminalGas extends WirelessTermBase implements IWirelessGasTermHandler {

	@Override
	public boolean isItemNormalWirelessTermToo(ItemStack is) {
		return false;
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(ItemStack itemStack, World world, EntityPlayer entityPlayer, EnumHand hand) {
		return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, ECApi.instance().openWirelessGasTerminal(entityPlayer, hand, world));
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void registerModel(Item item, ModelManager manager) {
		manager.registerItemModel(item, 0, "terminals/gas_wireless");
	}

}
