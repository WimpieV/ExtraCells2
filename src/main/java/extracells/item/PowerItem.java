package extracells.item;

import appeng.api.config.AccessRestriction;
import appeng.api.config.PowerUnits;
import appeng.api.implementations.items.IAEItemPowerStorage;
import cofh.api.energy.IEnergyContainerItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.Optional;

@Optional.Interface(iface = "cofh.api.energy.IEnergyContainerItem", modid = "CoFHAPI|energy", striprefs = true)
public class PowerItem extends ItemECBase implements IAEItemPowerStorage, IEnergyContainerItem {

	public double MAX_POWER;

	@Optional.Method(modid = "CoFHAPI|energy")
	@Override
	public int extractEnergy(ItemStack container, int maxExtract, boolean simulate) {
		if (simulate) {
			return getEnergyStored(container) >= maxExtract ? maxExtract : getEnergyStored(container);
		}
		else {
			return (int) PowerUnits.AE.convertTo(PowerUnits.RF, extractAEPower(container, PowerUnits.RF.convertTo(PowerUnits.AE, maxExtract)));
		}
	}

	@Optional.Method(modid = "CoFHAPI|energy")
	@Override
	public int getEnergyStored(ItemStack stack) {
		return (int) PowerUnits.AE.convertTo(PowerUnits.RF, getAECurrentPower(stack));
	}

	@Optional.Method(modid = "CoFHAPI|energy")
	@Override
	public int getMaxEnergyStored(ItemStack stack) {
		return (int) PowerUnits.AE.convertTo(PowerUnits.RF, getAEMaxPower(stack));
	}

	@Optional.Method(modid = "CoFHAPI|energy")
	@Override
	public int receiveEnergy(ItemStack container, int maxReceive, boolean simulate) {
		if (simulate) {
			double current = PowerUnits.AE.convertTo(PowerUnits.RF, getAECurrentPower(container));
			double max = PowerUnits.AE.convertTo(PowerUnits.RF, getAEMaxPower(container));
			if (max - current >= maxReceive) {
				return maxReceive;
			}
			else {
				return (int) (max - current);
			}
		}
		else {
			double currentAEPower = getAECurrentPower(container);
			if (currentAEPower < getAEMaxPower(container)) {
				double leftOver = PowerUnits.AE.convertTo(PowerUnits.RF, injectAEPower(container, PowerUnits.RF.convertTo(PowerUnits.AE, maxReceive)));
				return (int) (maxReceive - leftOver);
			}
			else {
				return 0;
			}
		}
	}

	@Override
	public double extractAEPower(ItemStack itemStack, double amt) {
		NBTTagCompound tagCompound = ensureTagCompound(itemStack);
		double currentPower = tagCompound.getDouble("power");
		double toExtract = Math.min(amt, currentPower);
		tagCompound.setDouble("power", currentPower - toExtract);
		return toExtract;
	}

	@Override
	public double getAECurrentPower(ItemStack itemStack) {
		return ensureTagCompound(itemStack).getDouble("power");
	}

	@Override
	public double getAEMaxPower(ItemStack stack) {
		return MAX_POWER;
	}

	@Override
	public AccessRestriction getPowerFlow(ItemStack stack) {
		return AccessRestriction.NO_ACCESS;
	}

	@Override
	public double injectAEPower(ItemStack itemStack, double amt) {
		NBTTagCompound tagCompound = ensureTagCompound(itemStack);
		double currentPower = tagCompound.getDouble("power");
		double toInject = Math.min(amt, MAX_POWER - currentPower);
		tagCompound.setDouble("power", currentPower + toInject);
		return toInject;
	}

	protected NBTTagCompound ensureTagCompound(ItemStack itemStack) {
		if (!itemStack.hasTagCompound()) {
			itemStack.setTagCompound(new NBTTagCompound());
		}
		return itemStack.getTagCompound();
	}

}
