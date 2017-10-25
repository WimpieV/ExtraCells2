package extracells.integration;

import extracells.ExtraCells;
import extracells.integration.waila.Waila;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModAPIManager;
import net.minecraftforge.fml.relauncher.Side;

public class Integration {

	public enum Mods {
			WAILA("Waila"),
			OPENCOMPUTERS("OpenComputers"),
			BCFUEL("BuildCraftAPI|fuels", "BuildCraftFuel"),
			MEKANISMGAS("MekanismAPI|gas", "MekanismGas"),
			THAUMATICENERGISTICS("thaumicenergistics", "Thaumatic Energistics"),
			MEKANISM("Mekanism"),
			WIRELESSCRAFTING("ae2wct", "AE2 Wireless Crafting Terminal");

		private final String modID;

		private boolean shouldLoad = true;

		private final String name;

		private final Side side;

		Mods(String modid) {
			this(modid, modid);
		}

		Mods(String modid, String modName, Side side) {
			modID = modid;
			name = modName;
			this.side = side;
		}

		Mods(String modid, String modName) {
			this(modid, modName, null);
		}

		Mods(String modid, Side side) {
			this(modid, modid, side);
		}

		public String getModID() {
			return modID;
		}

		public String getModName() {
			return name;
		}

		public boolean isOnClient() {
			return side != Side.SERVER;
		}

		public boolean isOnServer() {
			return side != Side.CLIENT;
		}

		public void loadConfig(Configuration config) {
			shouldLoad = config.get("Integration", "enable" + getModName(), true, "Enable " + getModName() + " Integration.").getBoolean(true);
		}

		public boolean isEnabled() {
			return (Loader.isModLoaded(getModID()) && shouldLoad && correctSide()) || (ModAPIManager.INSTANCE.hasAPI(getModID()) && shouldLoad && correctSide());
		}

		private boolean correctSide() {
			return ExtraCells.proxy.isClient() ? isOnClient() : isOnServer();
		}

	}

	public void loadConfig(Configuration config) {
		for (Mods mod : Mods.values()) {
			mod.loadConfig(config);
		}
	}

	public void preInit() {

	}

	public void init() {
		if (Mods.WAILA.isEnabled()) {
			Waila.init();
		}
		if (Mods.OPENCOMPUTERS.isEnabled()) {
		}
		if (Mods.MEKANISMGAS.isEnabled()) {
			if (Mods.MEKANISM.isEnabled()) {
				//Mekanism.init();
			}
		}

	}

	public void postInit() {
		if (Mods.MEKANISMGAS.isEnabled()) {
			//MekanismGas.postInit();
		}
	}

}
