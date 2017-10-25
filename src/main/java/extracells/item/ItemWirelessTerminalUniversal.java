package extracells.item;

import static extracells.item.WirelessTerminalType.CRAFTING;
import static extracells.item.WirelessTerminalType.FLUID;
import static extracells.item.WirelessTerminalType.ITEM;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.ImmutableList;

import appeng.api.AEApi;
import appeng.api.features.IWirelessTermHandler;
import extracells.api.ECApi;
import extracells.api.IWirelessFluidTermHandler;
import extracells.api.IWirelessGasTermHandler;
import extracells.integration.WirelessCrafting.WirelessCrafting;
import extracells.models.ModelManager;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author p455w0rd
 *
 */
public class ItemWirelessTerminalUniversal extends CraftingTerminal implements IWirelessFluidTermHandler, IWirelessGasTermHandler, IWirelessTermHandler {

	List<WirelessTerminalType> enabledTerminals = ImmutableList.of();
	int currentMode = 0;

	public ItemWirelessTerminalUniversal() {
		super();
		ImmutableList.Builder<WirelessTerminalType> builder = ImmutableList.builder();
		builder.add(FLUID, ITEM);
		//boolean isTeEnabled = Integration.Mods.THAUMATICENERGISTICS.isEnabled();
		//boolean isMekEnabled = Integration.Mods.MEKANISMGAS.isEnabled();
		//if (Integration.Mods.WIRELESSCRAFTING.isEnabled()) {
		builder.add(CRAFTING);
		//}
		//if (isWcEnabled) {
		ECApi.instance().registerWirelessTermHandler(this);
		AEApi.instance().registries().wireless().registerWirelessHandler(this);
		//}
		//else {
		//	ECApi.instance().registerWirelessTermHandler(new HandlerUniversalWirelessTerminal());
		//	AEApi.instance().registries().wireless().registerWirelessHandler(new HandlerUniversalWirelessTerminal());
		//}
		enabledTerminals = builder.build();
	}

	@Override
	public void getSubItems(Item item, CreativeTabs creativeTab, List<ItemStack> itemList) {
		NBTTagCompound tag = new NBTTagCompound();
		tag.setByte("modules", (byte) 31);
		ItemStack itemStack = new ItemStack(item);
		itemStack.setTagCompound(tag);
		itemList.add(itemStack.copy());
		injectAEPower(itemStack, MAX_POWER);
		itemList.add(itemStack);
	}

	@SuppressWarnings("deprecation")
	@SideOnly(Side.CLIENT)
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer player, List<String> list, boolean advanced) {
		NBTTagCompound tag = ensureTagCompound(itemStack);
		if (!tag.hasKey("type")) {
			tag.setByte("type", (byte) 0);
		}
		list.add(I18n.translateToLocal("extracells.tooltip.mode") + ": " + I18n.translateToLocal("extracells.tooltip." + WirelessTerminalType.values()[tag.getByte("type")].toString().toLowerCase()));
		list.add(I18n.translateToLocal("extracells.tooltip.installed"));
		Iterator<WirelessTerminalType> it = getInstalledModules(itemStack).iterator();
		while (it.hasNext()) {
			list.add("- " + I18n.translateToLocal("extracells.tooltip." + it.next().name().toLowerCase()));
		}
		super.addInformation(itemStack, player, list, advanced);
	}

	@Override
	public boolean isItemNormalWirelessTermToo(ItemStack is) {
		return true;
	}

	@Override
	public Item getItem() {
		return this;
	}

	@Override
	public ItemStack getStack() {
		return new ItemStack(this);
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(ItemStack itemStack, World world, EntityPlayer entityPlayer, EnumHand hand) {

		if (world.isRemote) {
			if (entityPlayer.isSneaking()) {
				return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, itemStack);
			}
			NBTTagCompound tag = ensureTagCompound(itemStack);
			if (!tag.hasKey("type")) {
				tag.setByte("type", (byte) 0);
			}
			if (tag.getByte("type") == 4 && isEnabled(CRAFTING)) {
				WirelessCrafting.openCraftingTerminal(entityPlayer);
			}
			return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, itemStack);
		}

		NBTTagCompound tag = ensureTagCompound(itemStack);
		if (!tag.hasKey("type")) {
			tag.setByte("type", (byte) 0);
		}
		if (entityPlayer.isSneaking()) {
			return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, changeMode(itemStack, entityPlayer, tag));
		}
		byte matched = tag.getByte("type");
		switch (matched) {
		case 0:
			AEApi.instance().registries().wireless().openWirelessTerminalGui(itemStack, world, entityPlayer);
			break;
		case 1:
			ECApi.instance().openWirelessFluidTerminal(entityPlayer, hand, world);
			break;
		case 2:
			ECApi.instance().openWirelessGasTerminal(entityPlayer, hand, world);
			break;
		//case 3 => if(isTeEnabled) ThaumaticEnergistics.openEssentiaTerminal(entityPlayer, this)
		default:
			break;
		}
		return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, itemStack);
	}

	public ItemStack changeMode(ItemStack itemStack, EntityPlayer player, NBTTagCompound tag) {
		EnumSet<WirelessTerminalType> installed = getInstalledModules(itemStack);
		byte matched = tag.getByte("type");
		switch (matched) {
		case 0:
			if (installed.contains(FLUID)) {
				tag.setByte("type", (byte) 1);
			}
			//else if (isMekEnabled && installed.contains(WirelessTerminalType.GAS)) {
			//	tag.setByte("type", (byte) 2);
			//}
			//else if (isTeEnabled && installed.contains(WirelessTerminalType.ESSENTIA)) {
			//	tag.setByte("type", (byte) 3);
			//}
			else if (isEnabled(CRAFTING) && installed.contains(CRAFTING)) {
				tag.setByte("type", (byte) 4);
			}
			break;
		case 1:
			//if (isMekEnabled && installed.contains(WirelessTerminalType.GAS)) {
			//	tag.setByte("type", (byte) 2);
			//}
			//else if (isTeEnabled && installed.contains(WirelessTerminalType.ESSENTIA)) {
			//	tag.setByte("type", (byte) 3);
			//}
			if (isEnabled(CRAFTING) && installed.contains(CRAFTING)) {
				tag.setByte("type", (byte) 4);
			}
			else if (installed.contains(ITEM)) {
				tag.setByte("type", (byte) 0);
			}
			break;
		case 2:
			//if (isTeEnabled && installed.contains(WirelessTerminalType.ESSENTIA)) {
			//	tag.setByte("type", (byte) 3);
			//}
			if (isEnabled(CRAFTING) && installed.contains(CRAFTING)) {
				tag.setByte("type", (byte) 4);
			}
			else if (installed.contains(ITEM)) {
				tag.setByte("type", (byte) 0);
			}
			else if (installed.contains(FLUID)) {
				tag.setByte("type", (byte) 1);
			}
			break;
		case 3:
			if (isEnabled(CRAFTING) && installed.contains(CRAFTING)) {
				tag.setByte("type", (byte) 4);
			}
			else if (installed.contains(ITEM)) {
				tag.setByte("type", (byte) 0);
			}
			else if (installed.contains(FLUID)) {
				tag.setByte("type", (byte) 1);
			}
			//else if (isMekEnabled && installed.contains(WirelessTerminalType.GAS)) {
			//	tag.setByte("type", (byte) 2);
			//}
			break;
		default:
			if (installed.contains(ITEM)) {
				tag.setByte("type", (byte) 0);
			}
			else if (installed.contains(FLUID)) {
				tag.setByte("type", (byte) 1);
			}
			//else if (isMekEnabled && installed.contains(WirelessTerminalType.GAS)) {
			//	tag.setByte("type", (byte) 2);
			//}
			//else if (isTeEnabled && installed.contains(WirelessTerminalType.ESSENTIA)) {
			//	tag.setByte("type", (byte) 3);
			//}
			if (isEnabled(CRAFTING) && installed.contains(CRAFTING)) {
				tag.setByte("type", (byte) 4);
			}
			else {
				tag.setByte("type", (byte) 0);
			}
			break;
		}
		itemStack.setTagCompound(tag);
		return itemStack;
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void registerModel(Item item, ModelManager manager) {
		manager.registerItemModel(item, 0, "terminals/universal_wireless");
	}

	public void installModule(ItemStack itemStack, WirelessTerminalType module) {
		if (isInstalled(itemStack, module)) {
			return;
		}
		byte install = (byte) (1 << module.ordinal());
		NBTTagCompound tag = ensureTagCompound(itemStack);
		byte installed = tag.hasKey("modules") ? (byte) (tag.getByte("modules") + install) : install;
		tag.setByte("modules", installed);
	}

	public EnumSet<WirelessTerminalType> getInstalledModules(ItemStack itemStack) {
		if (itemStack == null || itemStack.getItem() == null) {
			return EnumSet.noneOf(WirelessTerminalType.class);
		}
		NBTTagCompound tag = ensureTagCompound(itemStack);
		byte installed = tag.hasKey("modules") ? tag.getByte("modules") : 0;
		EnumSet<WirelessTerminalType> set = EnumSet.noneOf(WirelessTerminalType.class);
		for (WirelessTerminalType x : WirelessTerminalType.values()) {
			if (1 == (installed >> x.ordinal()) % 2) {
				set.add(x);
			}
		}
		return set;
	}

	public boolean isEnabled(WirelessTerminalType type) {
		return enabledTerminals.contains(type);
	}

	public boolean isInstalled(ItemStack itemStack, WirelessTerminalType module) {
		if (itemStack == null || itemStack.getItem() == null) {
			return false;
		}
		NBTTagCompound tag = ensureTagCompound(itemStack);
		byte installed = tag.hasKey("modules") ? tag.getByte("modules") : 0;
		return 1 == (installed >> module.ordinal()) % 2;
	}

}
