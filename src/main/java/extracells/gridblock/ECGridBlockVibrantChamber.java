package extracells.gridblock;

import java.util.EnumSet;

import javax.annotation.Nonnull;

import appeng.api.networking.GridFlags;
import appeng.api.networking.GridNotification;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridBlock;
import appeng.api.networking.IGridHost;
import appeng.api.util.AEColor;
import appeng.api.util.DimensionalCoord;
import extracells.tileentity.TileEntityVibrationChamberFluid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

/**
 * @author p455w0rd
 *
 */
public class ECGridBlockVibrantChamber implements IGridBlock {

	protected IGrid grid = null;
	protected int usedChannels = 0;
	private TileEntityVibrationChamberFluid host;

	public ECGridBlockVibrantChamber(@Nonnull TileEntityVibrationChamberFluid hostIn) {
		host = hostIn;
	}

	@Override
	public EnumSet<EnumFacing> getConnectableSides() {
		return EnumSet.of(EnumFacing.DOWN, EnumFacing.UP, EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.WEST);
	}

	@Override
	public EnumSet<GridFlags> getFlags() {
		return EnumSet.noneOf(GridFlags.class);
	}

	@Override
	public AEColor getGridColor() {
		return AEColor.TRANSPARENT;
	}

	@Override
	public double getIdlePowerUsage() {
		return host.getPowerUsage();
	}

	@Override
	public DimensionalCoord getLocation() {
		return host.getLocation();
	}

	@Override
	public IGridHost getMachine() {
		return host;
	}

	@Override
	public ItemStack getMachineRepresentation() {
		DimensionalCoord loc = getLocation();
		if (loc == null) {
			return null;
		}
		IBlockState blockState = loc.getWorld().getBlockState(loc.getPos());
		return new ItemStack(blockState.getBlock(), 1, blockState.getBlock().getMetaFromState(blockState));
	}

	@Override
	public void gridChanged() {
	}

	@Override
	public boolean isWorldAccessible() {
		return true;
	}

	@Override
	public void onGridNotification(GridNotification arg0) {
	}

	@Override
	public void setNetworkStatus(IGrid _grid, int _usedChannels) {
		grid = _grid;
		usedChannels = _usedChannels;
	}

}
