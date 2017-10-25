package extracells.util;

import java.util.HashMap;

import javax.annotation.Nonnull;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import net.minecraftforge.fluids.Fluid;

public class FuelBurnTime {

	public static HashMap<Fluid, Integer> fuelBurnTimes = Maps.<Fluid, Integer>newHashMap();

	public static void registerFuel(Fluid fluid, int burnTime) {
		Preconditions.checkNotNull(fluid);
		if (!fuelBurnTimes.containsKey(fluid)) {
			fuelBurnTimes.put(fluid, burnTime);
		}
	}

	public static int getBurnTime(@Nonnull Fluid fluid) {
		Preconditions.checkNotNull(fluid);
		if (fuelBurnTimes.containsKey(fluid)) {
			return fuelBurnTimes.get(fluid);
		}
		return 0;
	}

}
