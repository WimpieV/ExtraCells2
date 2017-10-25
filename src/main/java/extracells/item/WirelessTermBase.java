package extracells.item;

import java.util.List;

import appeng.api.config.AccessRestriction;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.util.IConfigManager;
import extracells.wireless.ConfigManager;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author p455w0rd
 *
 */
public class WirelessTermBase extends PowerItem implements IWirelessTermHandler {

	public WirelessTermBase() {
		setMaxStackSize(1);
		MAX_POWER = 1600000D;
	}

	@Override
	public AccessRestriction getPowerFlow(ItemStack stack) {
		return AccessRestriction.READ_WRITE;
	}

	@Override
	public double getDurabilityForDisplay(ItemStack itemStack) {
		return 1 - getAECurrentPower(itemStack) / MAX_POWER;
	}

	@Override
	public boolean showDurabilityBar(ItemStack is) {
		return true;
	}

	@Override
	public boolean canHandle(ItemStack is) {
		return is.getItem() == this;
	}

	@Override
	public String getEncryptionKey(ItemStack itemStack) {
		return ensureTagCompound(itemStack).getString("key");
	}

	@Override
	public void setEncryptionKey(ItemStack itemStack, String encKey, String name) {
		ensureTagCompound(itemStack).setString("key", encKey);
	}

	@Override
	public IConfigManager getConfigManager(ItemStack itemStack) {
		NBTTagCompound nbt = ensureTagCompound(itemStack);
		if (!nbt.hasKey("settings")) {
			nbt.setTag("settings", new NBTTagCompound());
		}
		NBTTagCompound tag = nbt.getCompoundTag("settings");
		return new ConfigManager(tag);
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

	@SideOnly(Side.CLIENT)
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
		String encryptionKey = ensureTagCompound(itemStack).getString("key");
		double aeCurrentPower = getAECurrentPower(itemStack);
		list.add(I18n.translateToLocal("gui.appliedenergistics2.StoredEnergy") + ": " + aeCurrentPower + " AE - " + Math.floor(aeCurrentPower / MAX_POWER * 1e4) / 1e2 + "%");
		list.add(I18n.translateToLocal(encryptionKey != null && !encryptionKey.isEmpty() ? "gui.appliedenergistics2.Linked" : "gui.appliedenergistics2.Unlinked"));
	}

	@Override
	public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
		return slotChanged;
	}

}
