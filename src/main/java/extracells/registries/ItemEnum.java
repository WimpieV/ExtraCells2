package extracells.registries;

import extracells.integration.Integration;
import extracells.item.ItemFluid;
import extracells.item.ItemFluidPattern;
import extracells.item.ItemInternalCraftingPattern;
import extracells.item.ItemOCUpgrade;
import extracells.item.ItemPartECBase;
import extracells.item.ItemWirelessTerminalFluid;
import extracells.item.ItemWirelessTerminalGas;
import extracells.item.ItemWirelessTerminalUniversal;
import extracells.item.storage.ItemStorageCasing;
import extracells.item.storage.ItemStorageCellFluid;
import extracells.item.storage.ItemStorageCellGas;
import extracells.item.storage.ItemStorageCellPhysical;
import extracells.item.storage.ItemStorageCellPortableFluid;
import extracells.item.storage.ItemStorageCellPortableGas;
import extracells.item.storage.ItemStorageComponent;
import extracells.util.CreativeTabEC;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.translation.I18n;

public enum ItemEnum {
		PARTITEM("part.base", new ItemPartECBase()),
		FLUIDSTORAGE("storage.fluid", new ItemStorageCellFluid()),
		PHYSICALSTORAGE("storage.physical", new ItemStorageCellPhysical()),
		GASSTORAGE("storage.gas", new ItemStorageCellGas(), Integration.Mods.MEKANISMGAS),
		FLUIDPATTERN("pattern.fluid", new ItemFluidPattern()),
		FLUIDWIRELESSTERMINAL("terminal.fluid.wireless", new ItemWirelessTerminalFluid()),
		STORAGECOMPONET("storage.component", new ItemStorageComponent()),
		STORAGECASING("storage.casing", new ItemStorageCasing()),
		FLUIDITEM("fluid.item", new ItemFluid(), null, null), // Internal EC Item
		FLUIDSTORAGEPORTABLE("storage.fluid.portable", new ItemStorageCellPortableFluid()),
		GASSTORAGEPORTABLE("storage.gas.portable", new ItemStorageCellPortableGas(), Integration.Mods.MEKANISMGAS),
		CRAFTINGPATTERN("pattern.crafting", new ItemInternalCraftingPattern(), null, null), // Internal EC Item
		UNIVERSALTERMINAL("terminal.universal.wireless", new ItemWirelessTerminalUniversal()),
		GASWIRELESSTERMINAL("terminal.gas.wireless", new ItemWirelessTerminalGas(), Integration.Mods.MEKANISMGAS),
		OCUPGRADE("oc.upgrade", new ItemOCUpgrade(), Integration.Mods.OPENCOMPUTERS);

	private final String internalName;
	private Item item;
	private Integration.Mods mod;

	ItemEnum(String internalName, Item item) {
		this(internalName, item, null);
	}

	ItemEnum(String internalName, Item item, Integration.Mods mod) {
		this(internalName, item, mod, CreativeTabEC.INSTANCE);
	}

	ItemEnum(String internalName, Item item, Integration.Mods mod, CreativeTabs creativeTab) {
		this.internalName = internalName;
		this.item = item;
		this.item.setUnlocalizedName("extracells." + this.internalName);
		this.item.setRegistryName(this.internalName);
		this.mod = mod;
		if ((creativeTab != null) && (mod == null || mod.isEnabled())) {
			this.item.setCreativeTab(creativeTab);
		}
	}

	public ItemStack getDamagedStack(int damage) {
		return new ItemStack(item, 1, damage);
	}

	public String getInternalName() {
		return internalName;
	}

	public Item getItem() {
		return item;
	}

	public ItemStack getSizedStack(int size) {
		return new ItemStack(item, size);
	}

	public String getStatName() {
		return I18n.translateToLocal(item.getUnlocalizedName());
	}

	public Integration.Mods getMod() {
		return mod;
	}
}
