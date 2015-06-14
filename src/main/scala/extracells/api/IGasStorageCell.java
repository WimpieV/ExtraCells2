package extracells.api;

import appeng.api.storage.ICellWorkbenchItem;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;

public interface IGasStorageCell extends ICellWorkbenchItem {

	/**
	 *
	 * @param ItemStack
	 * @return the Fluid Filter. An empty ArrayList or null if the cell accepts
	 *         all Gas
	 */
	public ArrayList<Object> getFilter(ItemStack is);

	public int getMaxBytes(ItemStack is);

	public int getMaxTypes(ItemStack is);

}