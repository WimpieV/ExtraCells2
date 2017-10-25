package extracells.registries;

import java.util.function.Function;

import extracells.block.BlockCertusTank;
import extracells.block.BlockFluidCrafter;
import extracells.block.BlockFluidFiller;
import extracells.block.BlockFluidInterface;
import extracells.block.BlockHardMEDrive;
import extracells.block.BlockVibrationChamberFluid;
import extracells.block.BlockWalrus;
import extracells.integration.Integration;
import extracells.item.block.ItemBlockCertusTank;
import extracells.item.block.ItemBlockFluidFiller;
import extracells.item.block.ItemBlockFluidInterface;
import extracells.util.CreativeTabEC;
import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.text.translation.I18n;

public enum BlockEnum {
		CERTUSTANK("certustank", new BlockCertusTank(), (block) -> new ItemBlockCertusTank(block)),
		WALRUS("walrus", new BlockWalrus()),
		FLUIDCRAFTER("fluidcrafter", new BlockFluidCrafter()),
		ECBASEBLOCK("ecbaseblock", new BlockFluidInterface(), (block) -> new ItemBlockFluidInterface(block)),
		FILLER("fluidfiller", new BlockFluidFiller(), (block) -> new ItemBlockFluidFiller(block)),
		BLASTRESISTANTMEDRIVE("hardmedrive", new BlockHardMEDrive()),
		VIBRANTCHAMBERFLUID("vibrantchamberfluid", new BlockVibrationChamberFluid());

	private final String internalName;
	private Block block;
	private ItemBlock item;
	private Integration.Mods mod;

	BlockEnum(String internalName, Block block, Integration.Mods mod) {
		this(internalName, block, (b) -> new ItemBlock(b), mod);
	}

	BlockEnum(String internalName, Block block) {
		this(internalName, block, (b) -> new ItemBlock(b));
	}

	BlockEnum(String internalName, Block block, Function<Block, ItemBlock> itemFactory) {
		this(internalName, block, itemFactory, null);
	}

	BlockEnum(String internalName, Block block, Function<Block, ItemBlock> factory, Integration.Mods mod) {
		this.internalName = internalName;
		this.block = block;
		this.block.setUnlocalizedName("extracells.block." + this.internalName);
		this.block.setRegistryName(internalName);
		item = factory.apply(block);
		item.setRegistryName(block.getRegistryName());
		this.mod = mod;
		if (mod == null || mod.isEnabled()) {
			this.block.setCreativeTab(CreativeTabEC.INSTANCE);
		}
	}

	public Block getBlock() {
		return block;
	}

	public String getInternalName() {
		return internalName;
	}

	public ItemBlock getItem() {
		return item;
	}

	public String getStatName() {
		return I18n.translateToLocal(block.getUnlocalizedName() + ".name");
	}

	public Integration.Mods getMod() {
		return mod;
	}
}
