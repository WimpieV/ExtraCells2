package extracells.gui;

import javax.annotation.Nullable;
import java.io.IOException;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;

import org.lwjgl.input.Keyboard;

import extracells.container.ContainerOreDictExport;
import extracells.network.packet.part.PacketOreDictExport;
import extracells.part.PartOreDictExporter;
import extracells.util.GuiUtil;
import extracells.util.NetworkUtil;

public class GuiOreDictExport extends GuiBase {

	public static void updateFilter(String newFilter) {
		if (filter != null) {
			filter = newFilter;
			GuiOreDictExport gui = GuiUtil.getGui(GuiOreDictExport.class);
			if (gui != null) {
				gui.setFilter(filter);
			}
		}
	}

	private EntityPlayer player;
	private static String filter = "";
	@Nullable
	private GuiTextField searchbar;

	public GuiOreDictExport(EntityPlayer player, PartOreDictExporter _part) {
		super(new ResourceLocation("extracells", "textures/gui/oredictexport.png"), new ContainerOreDictExport(player, _part));
		this.player = player;
	}

	@Override
	protected void actionPerformed(GuiButton guibutton) {
		NetworkUtil.sendToServer(new PacketOreDictExport(filter));
	}

	public void setFilter(String filter) {
		if (searchbar == null) {
			return;
		}
		searchbar.setText(filter);
	}

	@Override
	protected void drawBackground() {
		super.drawBackground();
		this.searchbar.drawTextBox();
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		super.drawGuiContainerForegroundLayer(mouseX, mouseY);
		this.fontRendererObj.drawString(
				I18n.translateToLocal(
						"extracells.part.oredict.export.name").replace("ME ",
						""), 8, 5, 0x000000);
		this.fontRendererObj.drawString(
				I18n.translateToLocal("container.inventory"), 8,
				this.ySize - 94, 0x000000);

	}

	@Override
	public void initGui() {
		super.initGui();
		this.buttonList.add(new GuiButton(1,
				this.guiLeft + this.xSize / 2 - 44, this.guiTop + 35, 88, 20,
				I18n.translateToLocal("extracells.tooltip.save")));
		this.searchbar = new GuiTextField(0, this.fontRendererObj, this.guiLeft
				+ this.xSize / 2 - 44, this.guiTop + 20, 88, 10) {

			private int xPos = 0;
			private int yPos = 0;
			private int width = 0;
			private int height = 0;

			@Override
			public void mouseClicked(int x, int y, int mouseBtn) {
				boolean flag = x >= this.xPos && x < this.xPos + this.width
						&& y >= this.yPos && y < this.yPos + this.height;
				if (flag && mouseBtn == 3)
					setText("");
			}
		};
		this.searchbar.setEnableBackgroundDrawing(true);
		this.searchbar.setFocused(true);
		this.searchbar.setMaxStringLength(15);
		this.searchbar.setText(filter);
	}

	@Override
	protected void keyTyped(char key, int keyID) {
		if (keyID == Keyboard.KEY_ESCAPE) {
			this.mc.player.closeScreen();
		}
		this.searchbar.textboxKeyTyped(key, keyID);
		filter = this.searchbar.getText();
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseBtn) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseBtn);
		this.searchbar.mouseClicked(mouseX, mouseY, mouseBtn);
	}
}
