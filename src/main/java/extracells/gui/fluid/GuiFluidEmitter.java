package extracells.gui.fluid;

import java.io.IOException;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import net.minecraftforge.fluids.Fluid;

import org.lwjgl.input.Keyboard;

import appeng.api.config.RedstoneMode;
import extracells.container.fluid.ContainerFluidEmitter;
import extracells.gui.GuiBase;
import extracells.gui.widget.AbstractWidget;
import extracells.gui.widget.DigitTextField;
import extracells.gui.widget.WidgetRedstoneModes;
import extracells.gui.widget.fluid.WidgetFluidSlot;
import extracells.network.packet.other.IFluidSlotGui;
import extracells.network.packet.part.PacketPartConfig;
import extracells.part.fluid.PartFluidLevelEmitter;
import extracells.registries.PartEnum;
import extracells.util.NetworkUtil;

public class GuiFluidEmitter extends GuiBase<ContainerFluidEmitter> implements IFluidSlotGui {

	public static final int xSize = 176;
	public static final int ySize = 166;
	private DigitTextField amountField;
	private PartFluidLevelEmitter part;
	private EntityPlayer player;

	public GuiFluidEmitter(PartFluidLevelEmitter _part, EntityPlayer _player) {
		super(new ResourceLocation("extracells", "textures/gui/levelemitterfluid.png"), new ContainerFluidEmitter(_part, _player));
		this.player = _player;
		this.part = _part;
		widgetManager.add(new WidgetFluidSlot(widgetManager, this.part, 79, 36));
		NetworkUtil.sendToServer(new PacketPartConfig(part, PacketPartConfig.FLUID_EMITTER_TOGGLE, Boolean.toString(false)));
	}

	@Override
	public void actionPerformed(GuiButton button) {
		switch (button.id) {
		case 0:
			modifyAmount(-1);
			break;
		case 1:
			modifyAmount(-10);
			break;
		case 2:
			modifyAmount(-100);
			break;
		case 3:
			modifyAmount(+1);
			break;
		case 4:
			modifyAmount(+10);
			break;
		case 5:
			modifyAmount(+100);
			break;
		case 6:
			NetworkUtil.sendToServer(new PacketPartConfig(part, PacketPartConfig.FLUID_EMITTER_TOGGLE, Boolean.toString(true)));
			break;

		}
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		this.fontRendererObj.drawString(PartEnum.FLUIDLEVELEMITTER.getStatName(), 5, 5, 0x000000);
		((WidgetRedstoneModes) this.buttonList.get(6)).drawTooltip(mouseX, mouseY, (this.width - xSize) / 2, (this.height - ySize) / 2);
	}

	@Override
	public void drawScreen(int x, int y, float f) {

		String[] buttonNames = { "-1", "-10", "-100", "+1", "+10", "+100" };
		String[] shiftNames = { "-100", "-1000", "-10000", "+100", "+1000", "+10000" };

		for (int i = 0; i < this.buttonList.size(); i++) {
			if (i == 6)
				break;
			GuiButton currentButton = this.buttonList.get(i);

			if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
				currentButton.displayString = shiftNames[i] + "mB";
			} else {
				currentButton.displayString = buttonNames[i] + "mB";
			}
		}

		super.drawScreen(x, y, f);
		this.amountField.drawTextBox();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void initGui() {
		int posX = (this.width - xSize) / 2;
		int posY = (this.height - ySize) / 2;

		this.amountField = new DigitTextField(0, this.fontRendererObj, posX + 10, posY + 40, 59, 10);
		this.amountField.setFocused(true);
		this.amountField.setEnableBackgroundDrawing(false);
		this.amountField.setTextColor(0xFFFFFF);

		this.buttonList.clear();
		this.buttonList.add(new GuiButton(0, posX + 65 - 46, posY + 8 + 6, 42, 20, "-1"));
		this.buttonList.add(new GuiButton(1, posX + 115 - 46, posY + 8 + 6, 42, 20, "-10"));
		this.buttonList.add(new GuiButton(2, posX + 165 - 46, posY + 8 + 6, 42, 20, "-100"));
		this.buttonList.add(new GuiButton(3, posX + 65 - 46, posY + 58 - 2, 42, 20, "+1"));
		this.buttonList.add(new GuiButton(4, posX + 115 - 46, posY + 58 - 2, 42, 20, "+10"));
		this.buttonList.add(new GuiButton(5, posX + 165 - 46, posY + 58 - 2, 42, 20, "+100"));
		this.buttonList.add(new WidgetRedstoneModes(6, posX + 120, posY + 36, 16, 16, RedstoneMode.LOW_SIGNAL, true));

		super.initGui();
	}

	@Override
	protected void keyTyped(char key, int keyID) throws IOException {
		super.keyTyped(key, keyID);
		if ("0123456789".contains(String.valueOf(key)) || keyID == Keyboard.KEY_BACK) {
			this.amountField.textboxKeyTyped(key, keyID);
			String text = amountField.getText();
			NetworkUtil.sendToServer(new PacketPartConfig(part, PacketPartConfig.FLUID_EMITTER_AMOUNT, text.isEmpty() ? "0" : text));
		}
	}

	private void modifyAmount(int amount) {
		if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT))
			amount *= 100;
		NetworkUtil.sendToServer(new PacketPartConfig(part, PacketPartConfig.FLUID_EMITTER_AMOUNT_CHANGE, Integer.toString(amount)));
	}

	public void setAmountField(long amount) {
		this.amountField.setText(Long.toString(amount));
	}

	public void setRedstoneMode(RedstoneMode mode) {
		((WidgetRedstoneModes) this.buttonList.get(6)).setRedstoneMode(mode);
	}

	@Override
	public void updateFluids(List<Fluid> fluids) {
		for (AbstractWidget widget : widgetManager.getWidgets()) {
			if (widget instanceof WidgetFluidSlot) {
				WidgetFluidSlot fluidSlot = (WidgetFluidSlot) widget;
				if (fluids == null || fluids.isEmpty()) {
					fluidSlot.setFluid(null);
					return;
				}
				fluidSlot.setFluid(fluids.get(0));
			}
		}
	}
}
