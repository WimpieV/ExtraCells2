package extracells.block;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author p455w0rd
 *
 */
public interface TGuiBlock {

	@SideOnly(Side.CLIENT)
	GuiContainer getClientGuiElement(EntityPlayer player, World world, BlockPos pos);

	Container getServerGuiElement(EntityPlayer player, World world, BlockPos pos);

}
