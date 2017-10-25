package extracells.gui.fluid;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

import appeng.api.AEApi;
import appeng.api.config.AccessRestriction;
import extracells.container.fluid.ContainerBusFluidStorage;
import extracells.gui.GuiBase;
import extracells.gui.ISlotRenderer;
import extracells.gui.SlotUpgradeRenderer;
import extracells.gui.widget.WidgetStorageDirection;
import extracells.gui.widget.fluid.WidgetFluidSlot;
import extracells.network.packet.other.IFluidSlotGui;
import extracells.network.packet.part.PacketPartConfig;
import extracells.part.fluid.PartFluidStorage;
import extracells.registries.PartEnum;
import extracells.util.FluidHelper;
import extracells.util.NetworkUtil;

public class GuiBusFluidStorage extends GuiBase<ContainerBusFluidStorage> implements
		WidgetFluidSlot.IConfigurable, IFluidSlotGui {

	private EntityPlayer player;
	private byte filterSize;
	private List<WidgetFluidSlot> fluidSlotList = new ArrayList<WidgetFluidSlot>();
	private boolean hasNetworkTool;
	private final PartFluidStorage part;

	public GuiBusFluidStorage(PartFluidStorage part, EntityPlayer _player) {
		super(new ResourceLocation("extracells", "textures/gui/storagebusfluid.png"), new ContainerBusFluidStorage(part, _player));
		this.part = part;
		((ContainerBusFluidStorage) this.inventorySlots).setGui(this);
		this.player = _player;

		for (int i = 0; i < 9; i++) {
			for (int j = 0; j < 6; j++) {
				WidgetFluidSlot fluidSlot = new WidgetFluidSlot(widgetManager, this.part, i * 6 + j, 18 * i + 7, 18 * j + 17);
				fluidSlotList.add(fluidSlot);
				widgetManager.add(fluidSlot);
			}
		}

		NetworkUtil.sendToServer(new PacketPartConfig(this.part, PacketPartConfig.FLUID_STORAGE_INFO));
		this.hasNetworkTool = this.inventorySlots.getInventory().size() > 40;
		this.xSize = this.hasNetworkTool ? 246 : 211;
		this.ySize = 222;
	}

	@Override
	public void actionPerformed(GuiButton button) throws IOException {
		super.actionPerformed(button);
		if (button instanceof WidgetStorageDirection) {
			AccessRestriction restriction;
			switch (((WidgetStorageDirection) button).getAccessRestriction()) {
			case NO_ACCESS:
				restriction = AccessRestriction.READ;
				break;
			case READ:
				restriction = AccessRestriction.READ_WRITE;
				break;
			case READ_WRITE:
				restriction = AccessRestriction.WRITE;
				break;
			case WRITE:
				restriction = AccessRestriction.NO_ACCESS;
				break;
			default:
				restriction = null;
				break;
			}
			if (restriction != null) {
				NetworkUtil.sendToServer(new PacketPartConfig(part, PacketPartConfig.FLUID_STORAGE_ACCESS, restriction.toString()));
			}
		}
	}

	public void changeConfig(byte _filterSize) {
		this.filterSize = _filterSize;
	}

	@Override
	protected void drawBackground() {
		drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, 176, 222);
		drawTexturedModalRect(this.guiLeft + 179, this.guiTop, 179, 0, 32, 86);
		if (this.hasNetworkTool)
			drawTexturedModalRect(this.guiLeft + 179, this.guiTop + 93, 178, 93, 68, 68);
	}

	@Override
	protected boolean hasSlotRenders() {
		return true;
	}

	@Nullable
	@Override
	protected ISlotRenderer getSlotRenderer(Slot slot) {
		if (slot.getStack() == null && (slot.slotNumber < 4 || slot.slotNumber > 39)) {
			return SlotUpgradeRenderer.INSTANCE;
		}
		return null;
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		super.drawGuiContainerForegroundLayer(mouseX, mouseY);

		fontRendererObj.drawString(PartEnum.FLUIDSTORAGE.getStatName().replace("ME ", ""), 8, 6, 4210752);
		fontRendererObj.drawString(player.inventory.getDisplayName().getUnformattedText(), 8, this.ySize - 96 + 3, 4210752);

		for (Object button : this.buttonList) {
			if (button instanceof WidgetStorageDirection)
				((WidgetStorageDirection) button).drawTooltip(mouseX, mouseY, (this.width - this.xSize) / 2, (this.height - this.ySize) / 2);
		}
	}

	@Override
	public byte getConfigState() {
		return this.filterSize;
	}

	protected Slot getSlotAtPosition(int p_146975_1_, int p_146975_2_) {
		for (int k = 0; k < this.inventorySlots.inventorySlots.size(); ++k) {
			Slot slot = this.inventorySlots.inventorySlots.get(k);

			if (this.isMouseOverSlot(slot, p_146975_1_, p_146975_2_)) {
				return slot;
			}
		}

		return null;
	}

	@Override
	public void initGui() {
		super.initGui();
		this.buttonList.add(new WidgetStorageDirection(0, this.guiLeft - 18, this.guiTop, 16, 16, AccessRestriction.READ_WRITE));
	}

	private boolean isMouseOverSlot(Slot p_146981_1_, int p_146981_2_, int p_146981_3_) {
		return this.isPointInRegion(p_146981_1_.xPos, p_146981_1_.yPos, 16, 16, p_146981_2_, p_146981_3_);
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseBtn) throws IOException {
		Slot slot = getSlotAtPosition(mouseX, mouseY);

		if (slot != null && slot.getStack() != null && AEApi.instance().definitions().items().networkTool().isSameAs(slot.getStack()))
			return;
		super.mouseClicked(mouseX, mouseY, mouseBtn);
	}

	public void shiftClick(ItemStack itemStack) {
		FluidStack containerFluid = FluidHelper.getFluidFromContainer(itemStack);
		Fluid fluid = containerFluid == null ? null : containerFluid.getFluid();
		for (WidgetFluidSlot fluidSlot : this.fluidSlotList) {
			if (fluid != null && (fluidSlot.getFluid() == null || fluidSlot.getFluid() == fluid)) {
				fluidSlot.handleContainer(itemStack);
				return;
			}
		}
	}

	public void updateAccessRestriction(AccessRestriction mode) {
		if (this.buttonList.size() > 0)
			((WidgetStorageDirection) this.buttonList.get(0)).setAccessRestriction(mode);
	}

	@Override
	public void updateFluids(List<Fluid> fluidList) {
		for (int i = 0; i < this.fluidSlotList.size() && i < fluidList.size(); i++) {
			this.fluidSlotList.get(i).setFluid(fluidList.get(i));
		}
	}
}
