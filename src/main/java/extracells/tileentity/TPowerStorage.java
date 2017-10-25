package extracells.tileentity;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.energy.IAEPowerStorage;
import net.minecraft.nbt.NBTTagCompound;

public class TPowerStorage implements IAEPowerStorage {

	PowerInformation powerInformation = new PowerInformation();

	@Override
	public double extractAEPower(double amount, Actionable mode, PowerMultiplier usePowerMultiplier) {
		double toExtract = Math.min(amount, powerInformation.currentPower);
		if (mode == Actionable.MODULATE) {
			powerInformation.currentPower -= toExtract;
		}
		return toExtract;
	}

	@Override
	public double getAECurrentPower() {
		return powerInformation.currentPower;
	}

	@Override
	public double getAEMaxPower() {
		return powerInformation.maxPower;
	}

	@Override
	public AccessRestriction getPowerFlow() {
		return AccessRestriction.READ_WRITE;
	}

	@Override
	public double injectAEPower(double amt, Actionable mode) {
		double maxStore = powerInformation.maxPower - powerInformation.currentPower;
		double notStored = maxStore - amt >= 0 ? 0 : amt - maxStore;
		if (mode == Actionable.MODULATE) {
			powerInformation.currentPower += amt - notStored;
		}
		return notStored;
	}

	@Override
	public boolean isAEPublicPowerStorage() {
		return true;
	}

	public void setMaxPower(double power) {
		powerInformation.maxPower = power;
	}

	public void readPowerFromNBT(NBTTagCompound tag) {
		if (tag.hasKey("currenPowerBattery")) {
			powerInformation.currentPower = tag.getDouble("currenPowerBattery");
		}
	}

	public void writePowerToNBT(NBTTagCompound tag) {
		tag.setDouble("currenPowerBattery", powerInformation.currentPower);
	}

	public static class PowerInformation {
		public double currentPower = 0.0D;
		public double maxPower = 500.0D;
	}

}
