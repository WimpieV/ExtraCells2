package extracells.part.fluid;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidBlock;

import appeng.api.config.Actionable;
import appeng.api.config.SecurityPermissions;
import appeng.api.networking.events.MENetworkChannelChanged;
import appeng.api.networking.events.MENetworkEventSubscribe;
import appeng.api.networking.events.MENetworkPowerStatusChange;
import appeng.api.networking.security.MachineSource;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartModel;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.util.AECableType;
import extracells.gridblock.ECBaseGridBlock;
import extracells.models.PartModels;
import extracells.part.PartECBase;
import extracells.util.FluidHelper;
import extracells.util.PermissionUtil;

public class PartFluidPlaneAnnihilation extends PartECBase {

	@Override
	public float getCableConnectionLength(AECableType cable) {
		return 2.0F;
	}

	@SuppressWarnings("unused")
	@MENetworkEventSubscribe
	public void channelChanged(MENetworkChannelChanged e) {
		if (e.node == getGridNode())
			onNeighborChanged();
	}

	@Override
	public void getBoxes(IPartCollisionHelper bch) {
		bch.addBox(2, 2, 14, 14, 14, 16);
		bch.addBox(5, 5, 13, 11, 11, 14);
	}

	@Override
	public int getLightLevel() {
		return 0;
	}

	@Override
	public double getPowerUsage() {
		return 1.0D;
	}

	@Override
	public boolean onActivate(EntityPlayer player, EnumHand hand, Vec3d pos) {
		if (PermissionUtil.hasPermission(player, SecurityPermissions.BUILD,
				(IPart) this)) {
			return super.onActivate(player, hand, pos);
		}
		return false;
	}

	@Override
	public void onNeighborChanged() {
		TileEntity hostTile = getHostTile();
		ECBaseGridBlock gridBlock = getGridBlock();
		if (hostTile == null || gridBlock == null)
			return;
		IMEMonitor<IAEFluidStack> monitor = gridBlock.getFluidMonitor();
		if (monitor == null)
			return;
		World world = hostTile.getWorld();
		BlockPos pos = hostTile.getPos();
		EnumFacing facing = getFacing();
		BlockPos offsetPos = pos.offset(facing);
		IBlockState blockState = world.getBlockState(offsetPos);
		Block fluidBlock = blockState.getBlock();
		int meta = fluidBlock.getMetaFromState(blockState);

		if (fluidBlock instanceof IFluidBlock) {
			IFluidBlock block = (IFluidBlock) fluidBlock;
			FluidStack drained = block.drain(world, offsetPos, false);
			if (drained == null)
				return;
			IAEFluidStack toInject = FluidHelper.createAEFluidStack(drained);
			IAEFluidStack notInjected = monitor.injectItems(toInject,
					Actionable.SIMULATE, new MachineSource(this));
			if (notInjected != null)
				return;
			monitor.injectItems(toInject, Actionable.MODULATE, new MachineSource(this));
			block.drain(world, offsetPos, true);
		} else if (meta == 0) {
			if (fluidBlock == Blocks.FLOWING_WATER) {
				IAEFluidStack toInject = FluidHelper
						.createAEFluidStack(FluidRegistry.WATER);
				IAEFluidStack notInjected = monitor.injectItems(toInject,
						Actionable.SIMULATE, new MachineSource(this));
				if (notInjected != null)
					return;
				monitor.injectItems(toInject, Actionable.MODULATE,
						new MachineSource(this));
				world.setBlockToAir(offsetPos);
			} else if (fluidBlock == Blocks.FLOWING_LAVA) {
				IAEFluidStack toInject = FluidHelper
						.createAEFluidStack(FluidRegistry.LAVA);
				IAEFluidStack notInjected = monitor.injectItems(toInject,
						Actionable.SIMULATE, new MachineSource(this));
				if (notInjected != null)
					return;
				monitor.injectItems(toInject, Actionable.MODULATE, new MachineSource(this));
				world.setBlockToAir(offsetPos);
			}
		}
	}

	@Override
	public IPartModel getStaticModels() {
		if(isActive() && isPowered()) {
			return PartModels.ANNIHILATION_PLANE_HAS_CHANNEL;
		} else if(isPowered()) {
			return PartModels.ANNIHILATION_PLANE_ON;
		} else {
			return PartModels.ANNIHILATION_PLANE_OFF;
		}
	}

	@Override
	@SuppressWarnings("unused")
	@MENetworkEventSubscribe
	public void setPower(MENetworkPowerStatusChange notUsed) {
		super.setPower(notUsed);
		onNeighborChanged();
	}
}
