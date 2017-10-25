package extracells.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IItemList;
import extracells.container.ContainerTerminal;
import extracells.container.IFluidSelectorContainer;
import extracells.container.StorageType;
import extracells.gui.widget.FluidWidgetComparator;
import extracells.gui.widget.fluid.AbstractFluidWidget;
import extracells.gui.widget.fluid.IFluidSelectorGui;
import extracells.gui.widget.fluid.WidgetFluidSelector;
import extracells.network.packet.part.PacketTerminalOpenContainer;
import extracells.part.fluid.PartFluidTerminal;
import extracells.util.ECConfigHandler;
import extracells.util.NetworkUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

public class GuiTerminal extends GuiContainer implements IFluidSelectorGui {

	private PartFluidTerminal terminal;
	private EntityPlayer player;
	private int currentScroll = 0;
	private GuiTextField searchbar;
	private List<AbstractFluidWidget> fluidWidgets = new ArrayList<AbstractFluidWidget>();
	private ResourceLocation guiTexture = new ResourceLocation("extracells", "textures/gui/terminalfluid.png");
	public IAEFluidStack currentFluid;
	private ContainerTerminal containerTerminal;
	private StorageType type;

	public GuiTerminal(PartFluidTerminal terminal, EntityPlayer player, StorageType type) {
		super(new ContainerTerminal(terminal, player, type));
		containerTerminal = (ContainerTerminal) inventorySlots;
		this.terminal = terminal;
		this.player = player;
		xSize = 176;
		ySize = 204;
		this.type = type;
		NetworkUtil.sendToServer(new PacketTerminalOpenContainer(terminal));
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float alpha, int sizeX, int sizeY) {
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		Minecraft.getMinecraft().renderEngine.bindTexture(guiTexture);
		drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
		searchbar.drawTextBox();
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		fontRendererObj.drawString(I18n.translateToLocal("extracells.part." + type.getName() + ".terminal.name").replace("ME ", ""), 9, 6, 0x000000);
		drawWidgets(mouseX, mouseY);
		if (currentFluid != null) {
			long currentFluidAmount = currentFluid.getStackSize();
			String amountToText = Long.toString(currentFluidAmount) + "mB";
			if (ECConfigHandler.shortenedBuckets) {
				if (currentFluidAmount > 1000000000L) {
					amountToText = Long.toString(currentFluidAmount / 1000000000L) + type.getMega();
				}
				else if (currentFluidAmount > 1000000L) {
					amountToText = Long.toString(currentFluidAmount / 1000000L) + type.getKilo();
				}
				else if (currentFluidAmount > 9999L) {
					amountToText = Long.toString(currentFluidAmount / 1000L) + type.getBuckets();
				}
			}

			fontRendererObj.drawString(I18n.translateToLocal("extracells.tooltip.amount") + ": " + amountToText, 45, 91, 0x000000);
			fontRendererObj.drawString(I18n.translateToLocal("extracells.tooltip.fluid") + ": " + currentFluid.getFluid().getLocalizedName(currentFluid.getFluidStack()), 45, 101, 0x000000);
		}
	}

	public void drawWidgets(int mouseX, int mouseY) {
		int listSize = fluidWidgets.size();
		if (!containerTerminal.getFluidStackList().isEmpty()) {
			outerLoop: for (int y = 0; y < 4; y++) {
				for (int x = 0; x < 9; x++) {
					int widgetIndex = y * 9 + x + currentScroll * 9;
					if (0 <= widgetIndex && widgetIndex < listSize) {
						AbstractFluidWidget widget = fluidWidgets.get(widgetIndex);
						widget.drawWidget(x * 18 + 7, y * 18 + 17);
					}
					else {
						break outerLoop;
					}
				}
			}

			for (int x = 0; x < 9; x++) {
				for (int y = 0; y < 4; y++) {
					int widgetIndex = y * 9 + x;
					if (0 <= widgetIndex && widgetIndex < listSize) {
						if (fluidWidgets.get(widgetIndex).drawTooltip(x * 18 + 7, y * 18 - 1, mouseX, mouseY)) {
							break;
						}
					}
					else {
						break;
					}
				}
			}

			int deltaWheel = Mouse.getDWheel();
			if (deltaWheel > 0) {
				currentScroll++;
			}
			else if (deltaWheel < 0) {
				currentScroll--;
			}

			if (currentScroll < 0) {
				currentScroll = 0;
			}
			if (listSize / 9 < 4 && currentScroll < listSize / 9 + 4) {
				currentScroll = 0;
			}
		}
	}

	@Override
	public IFluidSelectorContainer getContainer() {
		return containerTerminal;
	}

	@Override
	public IAEFluidStack getCurrentFluid() {
		return currentFluid;
	}

	public PartFluidTerminal getTerminal() {
		return terminal;
	}

	@Override
	public int guiLeft() {
		return guiLeft;
	}

	@Override
	public int guiTop() {
		return guiTop;
	}

	@Override
	public void initGui() {
		super.initGui();
		Mouse.getDWheel();

		updateFluids();
		Collections.sort(fluidWidgets, new FluidWidgetComparator());
		searchbar = new GuiTextField(0, fontRendererObj, guiLeft + 81, guiTop + 6, 88, 10) {

			private int xPos = 0;
			private int yPos = 0;
			private int width = 0;
			private int height = 0;

			@Override
			public void mouseClicked(int x, int y, int mouseBtn) {
				boolean flag = x >= xPos && x < xPos + width && y >= yPos && y < yPos + height;
				if (flag && mouseBtn == 3) {
					setText("");
				}
			}
		};
		searchbar.setEnableBackgroundDrawing(false);
		searchbar.setFocused(true);
		searchbar.setMaxStringLength(15);
	}

	@Override
	protected void keyTyped(char key, int keyID) {
		if (keyID == Keyboard.KEY_ESCAPE) {
			mc.player.closeScreen();
		}
		searchbar.textboxKeyTyped(key, keyID);
		updateFluids();
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseBtn) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseBtn);
		searchbar.mouseClicked(mouseX, mouseY, mouseBtn);
		int listSize = fluidWidgets.size();

		for (int x = 0; x < 9; x++) {
			for (int y = 0; y < 4; y++) {
				int index = y * 9 + x;
				if (0 <= index && index < listSize) {
					AbstractFluidWidget widget = fluidWidgets.get(index);
					widget.mouseClicked(x * 18 + 7, y * 18 - 1, mouseX, mouseY);
				}
			}
		}
	}

	public void updateFluids(IItemList<IAEFluidStack> fluidStacks) {
		containerTerminal.updateFluidList(fluidStacks);
		updateFluids();
	}

	public void updateFluids() {
		fluidWidgets = new ArrayList();
		for (IAEFluidStack stack : containerTerminal.getFluidStackList()) {
			FluidStack fluidStack = stack.getFluidStack();
			if (fluidStack.getLocalizedName().toLowerCase().contains(searchbar.getText().toLowerCase()) && type.canSee(stack.getFluidStack())) {
				fluidWidgets.add(new WidgetFluidSelector(this, stack));
			}
		}
		updateSelectedFluid();
	}

	public void receiveSelectedFluid(Fluid currentFluid) {
		containerTerminal.receiveSelectedFluid(currentFluid);
		updateSelectedFluid();
	}

	public void updateSelectedFluid() {
		currentFluid = null;
		for (IAEFluidStack stack : containerTerminal.getFluidStackList()) {
			if (stack.getFluid() == containerTerminal.getSelectedFluid()) {
				currentFluid = stack;
			}
		}
	}
}
