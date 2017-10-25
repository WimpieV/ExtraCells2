package extracells.block;

import java.util.Random;

import javax.annotation.Nullable;

import appeng.api.AEApi;
import appeng.api.config.SecurityPermissions;
import appeng.api.implementations.items.IAEWrench;
import appeng.api.networking.IGridNode;
import appeng.api.util.AEPartLocation;
import extracells.container.ContainerHardMEDrive;
import extracells.network.GuiHandler;
import extracells.tileentity.TileEntityHardMeDrive;
import extracells.util.PermissionUtil;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class BlockHardMEDrive extends BlockEC implements TGuiBlock {

	public BlockHardMEDrive() {
		super(Material.ROCK, 2.0F, 1000000.0F);
		setUnlocalizedName("block.hardmedrive");
	}

	@Override
	public TileEntity createNewTileEntity(World worldIn, int meta) {
		return new TileEntityHardMeDrive();
	}

	@Override
	public GuiContainer getClientGuiElement(EntityPlayer player, World world, BlockPos pos) {
		return null;
	}

	@Override
	public Container getServerGuiElement(EntityPlayer player, World world, BlockPos pos) {
		TileEntity tile = world.getTileEntity(pos);
		if (tile == null || player == null) {
			return null;
		}
		if (tile instanceof TileEntityHardMeDrive) {
			return new ContainerHardMEDrive(player.inventory, (TileEntityHardMeDrive) tile);
		}
		return null;
	}

	private void dropItems(World world, BlockPos pos) {
		Random rand = world.rand;
		TileEntity tileEntity = world.getTileEntity(pos);
		if (!(tileEntity instanceof TileEntityHardMeDrive)) {
			return;
		}
		IInventory inventory = ((TileEntityHardMeDrive) tileEntity).getInventory();

		for (int i = 0; i < inventory.getSizeInventory(); i++) {

			ItemStack item = inventory.getStackInSlot(i);
			if (item != null && item.stackSize > 0) {
				float rx = rand.nextFloat() * 0.8F + 0.1F;
				float ry = rand.nextFloat() * 0.8F + 0.1F;
				float rz = rand.nextFloat() * 0.8F + 0.1F;
				EntityItem entityItem = new EntityItem(world, pos.getX() + rx, pos.getY() + ry, pos.getZ() + rz, item.copy());
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

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, @Nullable ItemStack heldItem, EnumFacing side, float hitX, float hitY, float hitZ) {
		if (world.isRemote) {
			return true;
		}
		TileEntity tile = world.getTileEntity(pos);
		if (tile instanceof TileEntityHardMeDrive) {
			if (!PermissionUtil.hasPermission(player, SecurityPermissions.BUILD, ((TileEntityHardMeDrive) tile).getGridNode(AEPartLocation.INTERNAL))) {
				return false;
			}
		}
		ItemStack current = player.inventory.getCurrentItem();
		if (player.isSneaking() && current != null) {
			//TODO: Add buildcraft Support
			/*try {
			  if (current.getItem.isInstanceOf[IToolWrench] && (current.getItem.asInstanceOf[IToolWrench]).canWrench(player, x, y, z)) {
			    dropBlockAsItem(world, x, y, z, new ItemStack(this))
			    world.setBlockToAir(x, y, z)
			    (current.getItem.asInstanceOf[IToolWrench]).wrenchUsed(player, x, y, z)
			    return true
			  }
			}
			catch {
			  case e: Throwable => {
			  }
			}*/
			if (current.getItem() instanceof IAEWrench && ((IAEWrench) current.getItem()).canWrench(current, player, pos)) {
				dropBlockAsItem(world, pos, world.getBlockState(pos), 1);
				world.setBlockToAir(pos);
				return true;
			}
		}
		GuiHandler.launchGui(0, player, world, pos.getX(), pos.getY(), pos.getZ());
		return true;
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase entity, ItemStack stack) {
		super.onBlockPlacedBy(world, pos, state, entity, stack);
		double l = MathHelper.floor(entity.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
		//TODO: Add rotation
		/*if (!entity.isSneaking())
		{
		  if (l == 0)
		  {
		    world.setBlockMetadataWithNotify(x, y, z, 2, 2);
		  }

		  if (l == 1)
		  {
		    world.setBlockMetadataWithNotify(x, y, z, 5, 2);
		  }

		  if (l == 2)
		  {
		    world.setBlockMetadataWithNotify(x, y, z, 3, 2);
		  }

		  if (l == 3)
		  {
		    world.setBlockMetadataWithNotify(x, y, z, 4, 2);
		  }
		} else
		{
		  if (l == 0)
		  {
		    world.setBlockMetadataWithNotify(x, y, z, ForgeDirection.getOrientation(2).getOpposite().ordinal(), 2);
		  }

		  if (l == 1)
		  {
		    world.setBlockMetadataWithNotify(x, y, z, ForgeDirection.getOrientation(5).getOpposite().ordinal(), 2);
		  }

		  if (l == 2)
		  {
		    world.setBlockMetadataWithNotify(x, y, z, ForgeDirection.getOrientation(3).getOpposite().ordinal(), 2);
		  }

		  if (l == 3)
		  {
		    world.setBlockMetadataWithNotify(x, y, z, ForgeDirection.getOrientation(4).getOpposite().ordinal(), 2);
		  }
		}*/
		if (world.isRemote) {
			return;
		}
		TileEntity tile = world.getTileEntity(pos);
		if (tile != null) {
			if (tile instanceof TileEntityHardMeDrive) {
				IGridNode node = ((TileEntityHardMeDrive) tile).getGridNode(AEPartLocation.INTERNAL);
				if (entity != null && entity instanceof EntityPlayer) {
					EntityPlayer player = (EntityPlayer) entity;
					node.setPlayerID(AEApi.instance().registries().players().getID(player));
				}
				node.updateState();
			}
		}
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state) {
		if (world.isRemote) {
			return;
		}
		dropItems(world, pos);
		TileEntity tile = world.getTileEntity(pos);
		if (tile != null) {
			if (tile instanceof TileEntityHardMeDrive) {
				IGridNode node = ((TileEntityHardMeDrive) tile).getGridNode(AEPartLocation.INTERNAL);
				if (node != null) {
					node.destroy();
				}
			}
		}
		super.breakBlock(world, pos, state);
	}

}
