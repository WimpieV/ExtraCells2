package extracells.registries;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;

import appeng.api.config.Upgrades;
import extracells.integration.Integration;
import extracells.part.PartBattery;
import extracells.part.PartDrive;
import extracells.part.PartECBase;
import extracells.part.PartOreDictExporter;
import extracells.part.fluid.PartFluidConversionMonitor;
import extracells.part.fluid.PartFluidExport;
import extracells.part.fluid.PartFluidImport;
import extracells.part.fluid.PartFluidInterface;
import extracells.part.fluid.PartFluidLevelEmitter;
import extracells.part.fluid.PartFluidPlaneAnnihilation;
import extracells.part.fluid.PartFluidPlaneFormation;
import extracells.part.fluid.PartFluidStorage;
import extracells.part.fluid.PartFluidStorageMonitor;
import extracells.part.fluid.PartFluidTerminal;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public enum PartEnum {
		FLUIDEXPORT("fluid.export", PartFluidExport.class, "fluid.IO", generatePair(Upgrades.CAPACITY, 2), generatePair(Upgrades.REDSTONE, 1), generatePair(Upgrades.SPEED, 2)),
		FLUIDIMPORT("fluid.import", PartFluidImport.class, "fluid.IO", generatePair(Upgrades.CAPACITY, 2), generatePair(Upgrades.REDSTONE, 1), generatePair(Upgrades.SPEED, 2)),
		FLUIDSTORAGE("fluid.storage", PartFluidStorage.class, null, generatePair(Upgrades.INVERTER, 1)),
		FLUIDTERMINAL("fluid.terminal", PartFluidTerminal.class),
		FLUIDLEVELEMITTER("fluid.levelemitter", PartFluidLevelEmitter.class),
		FLUIDPANEANNIHILATION("fluid.plane.annihilation", PartFluidPlaneAnnihilation.class, "fluid.plane"),
		FLUIDPANEFORMATION("fluid.plane.formation", PartFluidPlaneFormation.class, "fluid.plane"),
		DRIVE("drive", PartDrive.class),
		BATTERY("battery", PartBattery.class),
		INTERFACE("interface", PartFluidInterface.class),
		FLUIDMONITOR("fluid.monitor", PartFluidStorageMonitor.class),
		FLUIDCONVERSIONMONITOR("fluid.conversion.monitor", PartFluidConversionMonitor.class),
		OREDICTEXPORTBUS("oredict.export", PartOreDictExporter.class),
	//GASIMPORT("gas.import", PartGasImport.class, "gas.IO", Integration.Mods.MEKANISMGAS, generatePair(Upgrades.CAPACITY, 2), generatePair(Upgrades.REDSTONE, 1), generatePair(Upgrades.SPEED, 2)),
	//GASEXPORT("gas.export", PartGasExport.class, "gas.IO", Integration.Mods.MEKANISMGAS, generatePair(Upgrades.CAPACITY, 2), generatePair(Upgrades.REDSTONE, 1), generatePair(Upgrades.SPEED, 2)),
	//GASTERMINAL("gas.terminal", PartGasTerminal.class, Integration.Mods.MEKANISMGAS),
	//GASSTORAGE("gas.storage", PartGasStorage.class, null, Integration.Mods.MEKANISMGAS, generatePair(Upgrades.INVERTER, 1)),
	//GASLEVELEMITTER("gas.levelemitter", PartGasLevelEmitter.class, Integration.Mods.MEKANISMGAS),
	//GASMONITOR("gas.monitor", PartGasStorageMonitor.class, Integration.Mods.MEKANISMGAS),
	/*GASCONVERSIONMONITOR("gas.conversion.monitor", PartGasConversionMonitor.class, Integration.Mods.MEKANISMGAS)*/;

	private Integration.Mods mod;

	private static Pair<Upgrades, Integer> generatePair(Upgrades _upgrade, int integer) {
		return Pair.of(_upgrade, integer);
	}

	public static int getPartID(Class<? extends PartECBase> partClass) {
		for (int i = 0; i < values().length; i++) {
			if (values()[i].getPartClass() == partClass) {
				return i;
			}
		}
		return -1;
	}

	public static int getPartID(PartECBase partECBase) {
		return getPartID(partECBase.getClass());
	}

	private String unlocalizedName;
	private Class<? extends PartECBase> partClass;
	private String groupName;
	@SideOnly(Side.CLIENT)
	private Optional<ModelResourceLocation> itemModel;
	private Map<Upgrades, Integer> upgrades = new HashMap<Upgrades, Integer>();

	PartEnum(String name, Class<? extends PartECBase> partClass) {
		this(name, partClass, null, (Integration.Mods) null);
	}

	PartEnum(String name, Class<? extends PartECBase> partClass, Integration.Mods mod) {
		this(name, partClass, null, mod);
	}

	PartEnum(String name, Class<? extends PartECBase> partClass, String groupName) {
		this(name, partClass, groupName, (Integration.Mods) null);
	}

	PartEnum(String name, Class<? extends PartECBase> partClass, String groupName, Integration.Mods mod) {
		unlocalizedName = "extracells.part." + name;
		this.partClass = partClass;
		this.groupName = groupName == null || groupName.isEmpty() ? null : "extracells." + groupName;
		this.mod = mod;
		if (FMLCommonHandler.instance().getSide().isClient()) {
			itemModel = Optional.of(new ModelResourceLocation("extracells:part/" + name.replace(".", "_")));
		}
		else {
			itemModel = Optional.empty();
		}
	}

	PartEnum(String _unlocalizedName, Class<? extends PartECBase> _partClass, String _groupName, Pair<Upgrades, Integer>... _upgrades) {
		this(_unlocalizedName, _partClass, _groupName, (Integration.Mods) null);
		for (Pair<Upgrades, Integer> pair : _upgrades) {
			upgrades.put(pair.getKey(), pair.getValue());
		}
	}

	PartEnum(String _unlocalizedName, Class<? extends PartECBase> _partClass, String _groupName, Integration.Mods _mod, Pair<Upgrades, Integer>... _upgrades) {
		this(_unlocalizedName, _partClass, _groupName, _mod);
		for (Pair<Upgrades, Integer> pair : _upgrades) {
			upgrades.put(pair.getKey(), pair.getValue());
		}
	}

	public String getGroupName() {
		return groupName;
	}

	public Class<? extends PartECBase> getPartClass() {
		return partClass;
	}

	public String getStatName() {
		return I18n.translateToLocal(unlocalizedName + ".name");
	}

	public String getUnlocalizedName() {
		return unlocalizedName;
	}

	@SuppressWarnings("unchecked")
	public Map<Upgrades, Integer> getUpgrades() {
		return upgrades;
	}

	public PartECBase newInstance(ItemStack partStack) throws IllegalAccessException, InstantiationException {
		PartECBase partECBase = partClass.newInstance();
		partECBase.initializePart(partStack);
		return partECBase;
	}

	@SideOnly(Side.CLIENT)
	public Optional<ModelResourceLocation> getItemModel() {
		return itemModel;
	}

	public Integration.Mods getMod() {
		return mod;
	}
}
