package extracells.tileentity;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.security.IActionHost;
import appeng.api.util.AECableType;
import appeng.api.util.AEPartLocation;
import appeng.api.util.DimensionalCoord;
import extracells.api.IECTileEntity;
import extracells.gridblock.ECGridBlockVibrantChamber;
import extracells.util.FuelBurnTime;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

public class TileEntityVibrationChamberFluid extends TileBase implements IActionHost, IECTileEntity, ITickable {

	private boolean isFirstGridNode;
	private IGridNode node;
	private final ECGridBlockVibrantChamber gridBlock = new ECGridBlockVibrantChamber(this);
	private FluidTank tank;
	private FluidHandler fluidHandler = new FluidHandler();
	private int burnTime, burnTimeTotal, timer, timerEnergy = 0;
	private double energyLeft = 0.0D;
	private TPowerStorage powerStore = new TPowerStorage();

	public TileEntityVibrationChamberFluid() {
		isFirstGridNode = true;
		node = null;
		tank = new FluidTank(16000) {

			@Override
			public FluidTank readFromNBT(NBTTagCompound nbt) {
				if (!nbt.hasKey("Empty")) {
					FluidStack fluid = FluidStack.loadFluidStackFromNBT(nbt);
					setFluid(fluid);
				}
				else {
					setFluid(null);
				}
				return this;
			}
		};
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		powerStore.writePowerToNBT(nbt);
		nbt.setInteger("BurnTime", burnTime);
		nbt.setInteger("BurnTimeTotal", burnTimeTotal);
		nbt.setInteger("timer", timer);
		nbt.setInteger("timerEnergy", timerEnergy);
		nbt.setDouble("energyLeft", energyLeft);
		tank.writeToNBT(nbt);
		return nbt;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		powerStore.readPowerFromNBT(nbt);
		if (nbt.hasKey("BurnTime")) {
			burnTime = nbt.getInteger("BurnTime");
		}
		if (nbt.hasKey("BurnTimeTotal")) {
			burnTimeTotal = nbt.getInteger("BurnTimeTotal");
		}
		if (nbt.hasKey("timer")) {
			timer = nbt.getInteger("timer");
		}
		if (nbt.hasKey("timerEnergy")) {
			timerEnergy = nbt.getInteger("timerEnergy");
		}
		if (nbt.hasKey("energyLeft")) {
			energyLeft = nbt.getDouble("energyLeft");
		}
		tank.readFromNBT(nbt);
	}

	@Override
	public NBTTagCompound getUpdateTag() {
		return writeToNBT(new NBTTagCompound());
	}

	public int getBurnTime() {
		return burnTime;
	}

	public int getBurnTimeTotal() {
		return burnTimeTotal;
	}

	public FluidTank getTank() {
		return tank;
	}

	@Override
	public AECableType getCableConnectionType(AEPartLocation arg0) {
		return AECableType.SMART;
	}

	@Override
	public IGridNode getGridNode(AEPartLocation arg0) {
		if (isFirstGridNode && hasWorld() && !world.isRemote) {
			isFirstGridNode = false;
			try {
				node = AEApi.instance().createGridNode(gridBlock);
				node.updateState();
			}
			catch (Exception e) {
				isFirstGridNode = true;
			}
		}
		return node;
	}

	public IGridNode getGridNodeWithoutUpdate() {
		if (isFirstGridNode && hasWorld() && !world.isRemote) {
			isFirstGridNode = false;
			try {
				node = AEApi.instance().createGridNode(gridBlock);
			}
			catch (Exception e) {
				isFirstGridNode = true;
			}
		}
		return node;
	}

	@Override
	public void securityBreak() {
	}

	@Override
	public void update() {
		if (!hasWorld()) {
			return;
		}
		FluidStack fluidStack1 = tank.getFluid();
		if (fluidStack1 != null) {
			fluidStack1 = fluidStack1.copy();
		}
		if (world.isRemote) {
			return;
		}
		if (burnTime == burnTimeTotal) {
			if (timer >= 40) {
				updateBlock();
				FluidStack fluidStack = tank.getFluid();
				int bTime = 0;
				if (fluidStack != null) {
					bTime = FuelBurnTime.getBurnTime(fluidStack.getFluid());
				}
				else {
					bTime = 0;
				}
				if (fluidStack != null && bTime > 0) {
					if (tank.getFluid().amount >= 250) {
						if (energyLeft <= 0) {
							burnTime = 0;
							burnTimeTotal = bTime / 4;
							tank.drain(250, true);
						}
					}
				}
				timer = 0;
			}
			else {
				timer += 1;
			}
		}
		else {
			burnTime += 1;
			if (timerEnergy == 4) {
				if (energyLeft == 0) {
					IEnergyGrid energy = getGridNode(AEPartLocation.INTERNAL).getGrid().getCache(IEnergyGrid.class);
					energyLeft = energy.injectPower(24.0D, Actionable.MODULATE);
				}
				else {
					IEnergyGrid energy = getGridNode(AEPartLocation.INTERNAL).getGrid().getCache(IEnergyGrid.class);
					energyLeft = energy.injectPower(energyLeft, Actionable.MODULATE);
				}
				timerEnergy = 0;
			}
			else {
				timerEnergy += 1;
			}
		}
		if (fluidStack1 == null && tank.getFluid() == null) {
			return;
		}
		if (fluidStack1 == null || tank.getFluid() == null) {
			updateBlock();
			return;
		}
		if (!(fluidStack1 == tank.getFluid())) {
			updateBlock();
			return;
		}
		if (fluidStack1.amount != tank.getFluid().amount) {
			updateBlock();
			return;
		}
	}

	@Override
	public DimensionalCoord getLocation() {
		return new DimensionalCoord(this);
	}

	@Override
	public double getPowerUsage() {
		return 0.0D;
	}

	@Override
	public IGridNode getActionableNode() {
		return getGridNode(AEPartLocation.INTERNAL);
	}

	public int getBurntTimeScaled(int scale) {
		return burnTime != 0 ? burnTime * scale / burnTimeTotal : 0;
	}

	@Override
	public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
		return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY ? CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(fluidHandler) : super.getCapability(capability, facing);
	}

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY ? true : super.hasCapability(capability, facing);
	}

	protected class FluidHandler implements IFluidHandler {

		@Override
		public IFluidTankProperties[] getTankProperties() {
			return tank.getTankProperties();
		}

		@Override
		public int fill(FluidStack resource, boolean doFill) {
			if (resource == null || resource.getFluid() == null || FuelBurnTime.getBurnTime(resource.getFluid()) == 0) {
				return 0;
			}
			int filled = tank.fill(resource, doFill);
			if (filled != 0 && hasWorld()) {
				updateBlock();
			}
			return filled;
		}

		@Override
		public FluidStack drain(FluidStack resource, boolean doDrain) {
			return null;
		}

		@Override
		public FluidStack drain(int maxDrain, boolean doDrain) {
			return null;
		}
	}

}
