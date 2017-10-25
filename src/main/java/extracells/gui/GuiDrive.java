package extracells.gui;

import extracells.container.ContainerDrive;
import extracells.part.PartDrive;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;

public class GuiDrive extends GuiContainer {

	private EntityPlayer player;
	private ResourceLocation guiTexture = new ResourceLocation("extracells", "textures/gui/drive.png");

	public GuiDrive(PartDrive _part, EntityPlayer _player) {
		super(new ContainerDrive(_part, _player));
		player = _player;
		xSize = 176;
		ySize = 163;
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float alpha, int sizeX, int sizeY) {
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		Minecraft.getMinecraft().renderEngine.bindTexture(guiTexture);
		drawTexturedModalRect(guiLeft, guiTop - 18, 0, 0, xSize, ySize);
		for (Object s : inventorySlots.inventorySlots) {
			renderBackground((Slot) s);
		}
	}

	private void renderBackground(Slot slot) {
		if (slot.getStack() == null && slot.slotNumber < 6) {
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
