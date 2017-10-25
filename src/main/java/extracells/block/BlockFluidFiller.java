package extracells.block;

import java.util.Random;

import javax.annotation.Nullable;

import appeng.api.config.SecurityPermissions;
import appeng.api.implementations.items.IAEWrench;
import appeng.api.util.AEPartLocation;
import extracells.api.IECTileEntity;
import extracells.network.GuiHandler;
import extracells.tileentity.IListenerTile;
import extracells.tileentity.TileEntityFluidFiller;
import extracells.tileentity.TileEntityFluidInterface;
import extracells.util.PermissionUtil;
import extracells.util.TileUtil;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * @author p455w0rd
 *
 */
public class BlockFluidFiller extends BlockEC {

	public BlockFluidFiller() {
		super(Material.IRON, 2.0F, 10.0F);
	}

	@Override
	public TileEntity createNewTileEntity(World worldIn, int meta) {
		return new TileEntityFluidFiller();
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, @Nullable ItemStack current, EnumFacing side, float hitX, float hitY, float hitZ) {
		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();
		if (world.isRemote) {
			return true;
		}
		Random rand = world.rand;
		TileEntity tile = world.getTileEntity(pos);
		if (tile instanceof IECTileEntity) {
			if (!PermissionUtil.hasPermission(player, SecurityPermissions.BUILD, ((IECTileEntity) tile).getGridNode(AEPartLocation.INTERNAL))) {
				return false;
			}
		}
		if (player.isSneaking() && current != null) {
			/*try {
			         if (current.getItem() instanceof IToolWrench
			             && ((IToolWrench) current.getItem()).canWrench(
			                 player, x, y, z)) {
			           ItemStack block = new ItemStack(this, 1,0);
			           if (tile != null
			               && tile instanceof TileEntityFluidInterface) {
			             block.setTagCompound(((TileEntityFluidInterface) tile)
			                 .writeFilter(new NBTTagCompound()));
			           }
			           dropBlockAsItem(world, pos, state, 1);
			           world.setBlockToAir(pos);
			           ((IToolWrench) current.getItem()).wrenchUsed(player, x,
			               y, z);
			           return true;
			         }
			       } catch (Throwable e) {
			         // No IToolWrench
			       }*/
			if (current.getItem() instanceof IAEWrench && ((IAEWrench) current.getItem()).canWrench(current, player, pos)) {
				ItemStack block = new ItemStack(this, 1, 0);
				if (tile != null && tile instanceof TileEntityFluidInterface) {
					block.setTagCompound(((TileEntityFluidInterface) tile).writeFilter(new NBTTagCompound()));
				}
				dropBlockAsItem(world, pos, state, 1);
				world.setBlockToAir(pos);
				return true;
			}
		}
		GuiHandler.launchGui(0, player, world, x, y, z);
		return true;
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase entity, ItemStack stack) {
		super.onBlockPlacedBy(world, pos, state, entity, stack);
		if (!world.isRemote) {
			TileUtil.setOwner(world, pos, entity);
			TileEntity tile = world.getTileEntity(pos);
			if (tile != null && tile instanceof IListenerTile) {
				((IListenerTile) tile).registerListener();
			}
		}
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state) {
		if (!world.isRemote) {
			TileUtil.destroy(world, pos);
			TileEntity tile = world.getTileEntity(pos);
			if (tile != null && tile instanceof IListenerTile) {
				((IListenerTile) tile).removeListener();
			}
			super.breakBlock(world, pos, state);
		}
	}

}
