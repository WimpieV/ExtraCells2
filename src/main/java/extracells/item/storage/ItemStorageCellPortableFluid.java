package extracells.item.storage;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;

import appeng.api.AEApi;
import appeng.api.config.FuzzyMode;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import extracells.api.ECApi;
import extracells.api.IHandlerFluidStorage;
import extracells.api.IPortableFluidStorageCell;
import extracells.inventory.ECFluidFilterInventory;
import extracells.inventory.ECPrivateInventory;
import extracells.item.ItemFluid;
import extracells.item.PowerItem;
import extracells.models.ModelManager;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author p455w0rd
 *
 */
public class ItemStorageCellPortableFluid extends PowerItem implements IPortableFluidStorageCell {

	public ItemStorageCellPortableFluid() {
		MAX_POWER = 20000D;
		setMaxStackSize(1);
		setMaxDamage(0);
	}

	@Override
	public void getSubItems(Item item, CreativeTabs creativeTab, List<ItemStack> itemList) {
		itemList.add(new ItemStack(item));
		ItemStack itemStack = new ItemStack(item);
		injectAEPower(itemStack, MAX_POWER);
		itemList.add(itemStack);
	}

	@SuppressWarnings("deprecation")
	@SideOnly(Side.CLIENT)
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer player, List<String> list, boolean advanced) {
		IMEInventoryHandler<IAEFluidStack> handler = AEApi.instance().registries().cell().getCellInventory(itemStack, null, StorageChannel.FLUIDS);
		if (!(handler instanceof IHandlerFluidStorage)) {
			return;
		}
		IHandlerFluidStorage cellHandler = (IHandlerFluidStorage) handler;
		boolean partitioned = cellHandler.isFormatted();
		long usedBytes = cellHandler.usedBytes();
		double aeCurrentPower = getAECurrentPower(itemStack);
		list.add(String.format(I18n.translateToLocal("extracells.tooltip.storage.fluid.bytes"), (usedBytes / 250), (cellHandler.totalBytes() / 250)));
		list.add(String.format(I18n.translateToLocal("extracells.tooltip.storage.fluid.types"), cellHandler.usedTypes(), cellHandler.totalTypes()));
		if (usedBytes != 0) {
			list.add(String.format(I18n.translateToLocal("extracells.tooltip.storage.fluid.content"), usedBytes));
		}
		if (partitioned) {
			list.add(I18n.translateToLocal("gui.appliedenergistics2.Partitioned") + " - " + I18n.translateToLocal("gui.appliedenergistics2.Precise"));
		}
		list.add(I18n.translateToLocal("gui.appliedenergistics2.StoredEnergy") + ": " + aeCurrentPower + " AE - " + Math.floor(aeCurrentPower / MAX_POWER * 1e4) / 1e2 + "%");
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void registerModel(Item item, ModelManager manager) {
		manager.registerItemModel(item, 0, "storage/fluid/portable");
	}

	@Override
	public String getUnlocalizedName(ItemStack itemStack) {
		return "extracells.item.storage.fluid.portable";
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(ItemStack itemStack, World world, EntityPlayer player, EnumHand hand) {
		return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, ECApi.instance().openPortableFluidCellGui(player, hand, world));
	}

	@Override
	public ArrayList<Fluid> getFilter(ItemStack stack) {
		ECFluidFilterInventory inventory = new ECFluidFilterInventory("", 63, stack);
		ItemStack[] stacks = inventory.slots;
		ArrayList<Fluid> filter = Lists.<Fluid>newArrayList();
		if (stacks.length == 0) {
			return null;
		}
		for (ItemStack s : stacks) {
			if (s != null) {
				Fluid fluid = FluidRegistry.getFluid(ItemFluid.getFluidName(stack));
				if (fluid != null) {
					filter.add(fluid);
				}
			}
		}
		return filter;
	}

	@Override
	public int getMaxBytes(ItemStack is) {
		return 512;
	}

	@Override
	public int getMaxTypes(ItemStack is) {
		return 3;
	}

	@Override
	public IInventory getConfigInventory(ItemStack is) {
		return new ECFluidFilterInventory("configFluidCell", 63, is);
	}

	@Override
	public FuzzyMode getFuzzyMode(ItemStack is) {
		if (is == null) {
			return null;
		}
		NBTTagCompound nbt = ensureTagCompound(is);
		if (nbt.hasKey("fuzzyMode")) {
			return FuzzyMode.valueOf(nbt.getString("fuzzyMode"));
		}
		is.getTagCompound().setString("fuzzyMode", FuzzyMode.IGNORE_ALL.name());
		return FuzzyMode.IGNORE_ALL;
	}

	@Override
	public IInventory getUpgradesInventory(ItemStack is) {
		return new ECPrivateInventory("configInventory", 0, 64);
	}

	@Override
	public boolean isEditable(ItemStack is) {
		return is != null && is.getItem() == this;
	}

	@Override
	public void setFuzzyMode(ItemStack is, FuzzyMode fzMode) {
		if (is == null) {
			return;
		}
		if (!is.hasTagCompound()) {
			is.setTagCompound(new NBTTagCompound());
		}
		NBTTagCompound tag = is.getTagCompound();
		tag.setString("fuzzyMode", fzMode.name());
	}

	@Override
	public boolean hasPower(EntityPlayer player, double amount, ItemStack is) {
		return getAECurrentPower(is) >= amount;
	}

	@Override
	public boolean usePower(EntityPlayer player, double amount, ItemStack is) {
		extractAEPower(is, amount);
		return true;
	}

}
