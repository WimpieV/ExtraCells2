package extracells.gui;

import extracells.container.ContainerHardMEDrive;
import extracells.registries.BlockEnum;
import extracells.tileentity.TileEntityHardMeDrive;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;

public class GuiHardMEDrive extends GuiContainer {

	private ResourceLocation guiTexture = new ResourceLocation("extracells", "textures/gui/hardmedrive.png");

	public GuiHardMEDrive(InventoryPlayer inventory, TileEntityHardMeDrive tile) {
		super(new ContainerHardMEDrive(inventory, tile));
		xSize = 176;
		ySize = 166;
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		drawDefaultBackground();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		Minecraft.getMinecraft().renderEngine.bindTexture(guiTexture);
		int posX = (width - xSize) / 2;
		int posY = (height - ySize) / 2;
		drawTexturedModalRect(posX, posY, 0, 0, xSize, ySize);
		for (Slot s : inventorySlots.inventorySlots) {
			renderBackground(s);
		}
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		fontRendererObj.drawString(BlockEnum.BLASTRESISTANTMEDRIVE.getStatName(), 5, 5, 0x000000);
	}

	private void renderBackground(Slot slot) {
		if (slot.getStack() == null && slot.slotNumber < 3) {
			GlStateManager.disableLighting();
			GlStateManager.enableBlend();
			GlStateManager.color(1.0F, 1.0F, 1.0F, 0.5F);
			mc.getTextureManager().bindTexture(new ResourceLocation("appliedenergistics2", "textures/guis/states.png"));
			this.drawTexturedModalRect(guiLeft + slot.xPos, guiTop + slot.yPos, 240, 0, 16, 16);
			GlStateManager.disableBlend();
			GlStateManager.enableLighting();
		}
	}

}
