package extracells.item;

import extracells.api.ECApi;
import extracells.api.IWirelessFluidTermHandler;
import extracells.models.ModelManager;
import extracells.network.GuiHandler;
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
public class ItemWirelessTerminalFluid extends WirelessTermBase implements IWirelessFluidTermHandler {

	public ItemWirelessTerminalFluid() {
		super();
		ECApi.instance().registerWirelessTermHandler(this);
	}

	@Override
	public boolean isItemNormalWirelessTermToo(ItemStack is) {
		return false;
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(ItemStack itemStack, World world, EntityPlayer entityPlayer, EnumHand hand) {
		GuiHandler.launchGui(GuiHandler.getGuiId(0), entityPlayer, world, 0, 0, 0);
		return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, ECApi.instance().openWirelessFluidTerminal(entityPlayer, hand, world));
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void registerModel(Item item, ModelManager manager) {
		manager.registerItemModel(item, 0, "terminals/fluid_wireless");
	}

}
