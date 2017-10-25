package extracells.block;

import java.util.Random;

import extracells.models.ModelManager;
import extracells.tileentity.TileEntityFluidInterface;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * @author p455w0rd
 *
 */
public class BlockFluidInterface extends BlockEC {

	public BlockFluidInterface() {
		super(Material.IRON, 2.0F, 10.0F);
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState stat) {
		dropPatter(world, pos);
		super.breakBlock(world, pos, stat);
	}

	@Override
	public TileEntity createNewTileEntity(World worldIn, int meta) {
		return new TileEntityFluidInterface();
	}

	@Override
	public void registerModel(Item item, ModelManager manager) {
		manager.registerItemModel(item, 0, "fluid_interface");
	}

	private void dropPatter(World world, BlockPos pos) {
		Random rand = world.rand;
		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();
		TileEntity tileEntity = world.getTileEntity(pos);
		if (!(tileEntity instanceof TileEntityFluidInterface)) {
			return;
		}
		IInventory inventory = ((TileEntityFluidInterface) tileEntity).inventory;
		for (int i = 0; i < inventory.getSizeInventory(); i++) {
			ItemStack item = inventory.getStackInSlot(i);
			if (item != null && item.stackSize > 0) {
				float rx = rand.nextFloat() * 0.8F + 0.1F;
				float ry = rand.nextFloat() * 0.8F + 0.1F;
				float rz = rand.nextFloat() * 0.8F + 0.1F;
				EntityItem entityItem = new EntityItem(world, x + rx, y + ry, z + rz, item.copy());
				if (item.hasTagCompound()) {
					entityItem.getEntityItem().setTagCompound(item.getTagCompound().copy());
				}
				float factor = 0.05F;
				entityItem.motionX = rand.nextGaussian() * factor;
				entityItem.motionY = rand.nextGaussian() * factor + 0.2F;
				entityItem.motionZ = rand.nextGaussian() * factor;
				world.spawnEntity(entityItem);
				item.stackSize = 0;
			}
		}
	}

}
