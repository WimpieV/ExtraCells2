package extracells;

import java.io.File;

import javax.annotation.Nullable;

import com.google.common.base.Preconditions;

import appeng.api.AEApi;
import appeng.api.features.IRegistryContainer;
import extracells.integration.Integration;
import extracells.item.storage.CellDefinition;
import extracells.network.GuiHandler;
import extracells.network.PacketHandler;
import extracells.proxy.CommonProxy;
import extracells.util.ECConfigHandler;
import extracells.util.ExtraCellsEventHandler;
import extracells.util.FluidCellHandler;
import extracells.util.NameHandler;
import extracells.wireless.AEWirelessTermHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

@Mod(modid = Constants.MOD_ID, version = Constants.VERSION, name = "Extra Cells", dependencies = "after:LogisticsPipes|Main;after:Waila;required-after:appliedenergistics2")
public class ExtraCells {
	@SidedProxy(clientSide = "extracells.proxy.ClientProxy", serverSide = "extracells.proxy.CommonProxy")
	public static CommonProxy proxy = null;

	@Mod.Instance(Constants.MOD_ID)
	public static ExtraCells instance;

	public static final Integration integration = new Integration();
	public static int bcBurnTimeMultiplicator = 4;
	private File configFolder;

	public ExtraCells() {
		FluidRegistry.enableUniversalBucket();
	}

	@Nullable
	private static PacketHandler packetHandler;

	public static PacketHandler getPacketHandler() {
		Preconditions.checkState(packetHandler != null);
		return packetHandler;
	}

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		packetHandler = new PacketHandler();
		NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());

		// Config
		configFolder = event.getModConfigurationDirectory();
		Configuration config = new Configuration(new File(configFolder, "AppliedEnergistics2" + File.separator + "extracells.cfg"));
		ECConfigHandler configHandler = new ECConfigHandler(config);
		configHandler.reload();
		MinecraftForge.EVENT_BUS.register(configHandler);

		proxy.registerItems();
		proxy.registerBlocks();
		CellDefinition.create();
		integration.preInit();
		proxy.registerModels();
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		IRegistryContainer registries = AEApi.instance().registries();
		registries.recipes().addNewSubItemResolver(new NameHandler());
		registries.wireless().registerWirelessHandler(new AEWirelessTermHandler());
		registries.cell().addCellHandler(new FluidCellHandler());
		ExtraCellsEventHandler handler = new ExtraCellsEventHandler();
		MinecraftForge.EVENT_BUS.register(handler);
		proxy.registerMovables();
		proxy.registerRenderers();
		proxy.registerTileEntities();
		proxy.registerFluidBurnTimes();
		proxy.addRecipes(configFolder);
		proxy.registerPackets();
		//RenderingRegistry.registerBlockHandler(new RenderHandler(RenderingRegistry.getNextAvailableRenderId))
		integration.init();
	}

	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		integration.postInit();
	}
}
