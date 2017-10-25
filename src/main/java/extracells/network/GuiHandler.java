package extracells.network;

import appeng.api.parts.IPartHost;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEFluidStack;
import extracells.ExtraCells;
import extracells.api.IFluidInterface;
import extracells.api.IPortableFluidStorageCell;
import extracells.api.IPortableGasStorageCell;
import extracells.api.IWirelessFluidTermHandler;
import extracells.api.IWirelessGasTermHandler;
import extracells.block.TGuiBlock;
import extracells.container.fluid.ContainerFluidCrafter;
import extracells.container.fluid.ContainerFluidFiller;
import extracells.container.fluid.ContainerFluidInterface;
import extracells.container.fluid.ContainerFluidStorage;
import extracells.container.gas.ContainerGasStorage;
import extracells.gui.GuiStorage;
import extracells.gui.fluid.GuiFluidCrafter;
import extracells.gui.fluid.GuiFluidFiller;
import extracells.gui.fluid.GuiFluidInterface;
import extracells.part.PartECBase;
import extracells.registries.BlockEnum;
import extracells.tileentity.TileEntityFluidCrafter;
import extracells.tileentity.TileEntityFluidFiller;
import extracells.tileentity.TileEntityFluidInterface;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

public class GuiHandler implements IGuiHandler {

	private static Object[] temp = null;
	private static EnumHand hand = null;

	@Override
	public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
		Container con = getContainerBlockElement(player, world, x, y, z);
		if (con != null) {
			return con;
		}
		EnumFacing side = null;
		if (ID < 5) {
			side = EnumFacing.getFront(ID);
		}
		BlockPos pos = new BlockPos(x, y, z);
		IBlockState blockState = world.getBlockState(pos);
		if (blockState.getBlock() == BlockEnum.FLUIDCRAFTER.getBlock()) {
			TileEntity tileEntity = world.getTileEntity(pos);
			if (tileEntity == null || !(tileEntity instanceof TileEntityFluidCrafter)) {
				return null;
			}
			return new ContainerFluidCrafter(player.inventory, ((TileEntityFluidCrafter) tileEntity).getInventory());
		}
		if (blockState.getBlock() == BlockEnum.ECBASEBLOCK.getBlock()) {
			TileEntity tileEntity = world.getTileEntity(pos);
			if (tileEntity == null) {
				return null;
			}
			if (tileEntity instanceof TileEntityFluidInterface) {
				return new ContainerFluidInterface(player, (IFluidInterface) tileEntity);
			}
			else if (tileEntity instanceof TileEntityFluidFiller) {
				return new ContainerFluidFiller(player.inventory, (TileEntityFluidFiller) tileEntity);
			}
			return null;
		}
		if (world != null && side != null) {
			return getPartContainer(side, player, world, x, y, z);
		}
		return getContainer(ID - 6, player, temp);
	}

	@Override
	public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
		Object gui = getGuiBlockElement(player, world, x, y, z);
		if (gui != null) {
			return gui;
		}
		EnumFacing side = null;
		if (ID < 5) {
			side = EnumFacing.getFront(ID);
		}
		BlockPos pos = new BlockPos(x, y, z);
		if (world.getBlockState(pos).getBlock() == BlockEnum.FLUIDCRAFTER.getBlock()) {
			TileEntity tileEntity = world.getTileEntity(pos);
			if (tileEntity == null || !(tileEntity instanceof TileEntityFluidCrafter)) {
				return null;
			}
			return new GuiFluidCrafter(player.inventory, ((TileEntityFluidCrafter) tileEntity).getInventory());
		}
		if (world != null && world.getBlockState(pos).getBlock() == BlockEnum.ECBASEBLOCK.getBlock()) {
			TileEntity tileEntity = world.getTileEntity(pos);
			if (tileEntity == null) {
				return null;
			}
			if (tileEntity instanceof TileEntityFluidInterface) {
				return new GuiFluidInterface(player, (IFluidInterface) tileEntity);
			}
			else if (tileEntity instanceof TileEntityFluidFiller) {
				return new GuiFluidFiller(player, (TileEntityFluidFiller) tileEntity);
			}
			return null;
		}
		if (world != null && side != null) {
			return getPartGui(side, player, world, x, y, z);
		}
		return getGui(ID - 6, player);
	}

	public static void launchGui(int ID, EntityPlayer player, EnumHand handIn, Object... args) {
		temp = args;
		hand = handIn;
		player.openGui(ExtraCells.instance, ID, player.getEntityWorld(), 0, 0, 0);
	}

	public static void launchGui(int ID, EntityPlayer player, World world, int x, int y, int z) {
		player.openGui(ExtraCells.instance, ID, world, x, y, z);
	}

	@SuppressWarnings("unchecked")
	private Container getContainer(int ID, EntityPlayer player, Object... args) {
		switch (ID) {
		case 0:
			IMEMonitor<IAEFluidStack> fluidInventory = ((IMEMonitor<IAEFluidStack>) args[0]);
			return new ContainerFluidStorage(fluidInventory, player, hand);
		case 1:
			IMEMonitor<IAEFluidStack> fluidInventory2 = ((IMEMonitor<IAEFluidStack>) args[0]);
			IWirelessFluidTermHandler handler = ((IWirelessFluidTermHandler) args[1]);
			return new ContainerFluidStorage(fluidInventory2, player, handler, hand);
		case 3:
			IMEMonitor<IAEFluidStack> fluidInventory3 = ((IMEMonitor<IAEFluidStack>) args[0]);
			IPortableFluidStorageCell storageCell = ((IPortableFluidStorageCell) args[1]);
			return new ContainerFluidStorage(fluidInventory3, player, storageCell, hand);
		case 4:
			IMEMonitor<IAEFluidStack> fluidInventory4 = ((IMEMonitor<IAEFluidStack>) args[0]);
			return new ContainerGasStorage(fluidInventory4, player, hand);
		case 5:
			IMEMonitor<IAEFluidStack> fluidInventory5 = ((IMEMonitor<IAEFluidStack>) args[0]);
			IWirelessGasTermHandler handler2 = ((IWirelessGasTermHandler) args[1]);
			return new ContainerGasStorage(fluidInventory5, player, handler2, hand);
		case 6:
			IMEMonitor<IAEFluidStack> fluidInventory6 = ((IMEMonitor<IAEFluidStack>) args[0]);
			IPortableGasStorageCell storageCell2 = ((IPortableGasStorageCell) args[1]);
			return new ContainerGasStorage(fluidInventory6, player, storageCell2, hand);
		default:
			return null;
		}
	}

	private GuiContainer getGui(int ID, EntityPlayer player) {
		switch (ID) {
		case 0:
			return new GuiStorage(new ContainerFluidStorage(player, hand), "extracells.part.fluid.terminal.name");
		case 1:
			return new GuiStorage(new ContainerFluidStorage(player, hand), "extracells.part.fluid.terminal.name");
		case 3:
			return new GuiStorage(new ContainerFluidStorage(player, hand), "extracells.item.storage.fluid.portable.name");
		case 4:
			return new GuiStorage(new ContainerGasStorage(player, hand), "extracells.part.gas.terminal.name");
		case 5:
			return new GuiStorage(new ContainerGasStorage(player, hand), "extracells.part.gas.terminal.name");
		case 6:
			return new GuiStorage(new ContainerGasStorage(player, hand), "extracells.item.storage.gas.portable.name");
		default:
			return null;
		}
	}

	private Object getPartContainer(EnumFacing side, EntityPlayer player, World world, int x, int y, int z) {
		return ((PartECBase) ((IPartHost) world.getTileEntity(new BlockPos(x, y, z))).getPart(side)).getServerGuiElement(player);
	}

	private Object getPartGui(EnumFacing side, EntityPlayer player, World world, int x, int y, int z) {
		return ((PartECBase) ((IPartHost) world.getTileEntity(new BlockPos(x, y, z))).getPart(side)).getClientGuiElement(player);
	}

	private Object getGuiBlockElement(EntityPlayer player, World world, int x, int y, int z) {
		if (world == null || player == null) {
			return null;
		}
		BlockPos pos = new BlockPos(x, y, z);
		Block block = world.getBlockState(pos).getBlock();
		if (block == null) {
			return null;
		}
		if (block instanceof TGuiBlock) {
			return ((TGuiBlock) block).getClientGuiElement(player, world, pos);
		}
		return null;
	}

	private Container getContainerBlockElement(EntityPlayer player, World world, int x, int y, int z) {
		if (world == null || player == null) {
			return null;
		}
		BlockPos pos = new BlockPos(x, y, z);
		Block block = world.getBlockState(pos).getBlock();
		if (block == null) {
			return null;
		}
		if (block instanceof TGuiBlock) {
			return ((TGuiBlock) block).getServerGuiElement(player, world, pos);
		}
		return null;
	}

	public static int getGuiId(int guiId) {
		return guiId + 6;
	}

	public static int getGuiId(PartECBase part) {
		return part.getFacing().ordinal();
	}

}
