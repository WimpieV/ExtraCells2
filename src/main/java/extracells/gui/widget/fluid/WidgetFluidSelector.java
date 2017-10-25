package extracells.gui.widget.fluid;

import java.util.ArrayList;
import java.util.List;

import appeng.api.storage.data.IAEFluidStack;
import extracells.util.ECConfigHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

public class WidgetFluidSelector extends AbstractFluidWidget {

	private long amount = 0;
	private int color;
	private int borderThickness;

	public WidgetFluidSelector(IFluidSelectorGui guiFluidTerminal, IAEFluidStack stack) {
		super(guiFluidTerminal, 18, 18, stack.getFluidStack().getFluid());
		amount = stack.getStackSize();
		color = 0xFF00FFFF;
		borderThickness = 1;
	}

	private void drawHollowRectWithCorners(int posX, int posY, int height, int width, int color, int thickness) {
		drawRect(posX, posY, posX + height, posY + thickness, color);
		drawRect(posX, posY + width - thickness, posX + height, posY + width, color);
		drawRect(posX, posY, posX + thickness, posY + width, color);
		drawRect(posX + height - thickness, posY, posX + height, posY + width, color);

		drawRect(posX, posY, posX + thickness + 1, posY + thickness + 1, color);
		drawRect(posX + height, posY + width, posX + height - thickness - 1, posY + width - thickness - 1, color);
		drawRect(posX + height, posY, posX + height - thickness - 1, posY + thickness + 1, color);
		drawRect(posX, posY + width, posX + thickness + 1, posY + width - thickness - 1, color);
	}

	@Override
	public boolean drawTooltip(int posX, int posY, int mouseX, int mouseY) {
		if (fluid == null || amount <= 0 || !isPointInRegion(posX, posY, height, width, mouseX, mouseY)) {
			return false;
		}

		String amountToText = Long.toString(amount) + "mB";
		if (ECConfigHandler.shortenedBuckets) {
			if (amount > 1000000000L) {
				amountToText = Long.toString(amount / 1000000000L) + "MegaB";
			}
			else if (amount > 1000000L) {
				amountToText = Long.toString(amount / 1000000L) + "KiloB";
			}
			else if (amount > 9999L) {
				amountToText = Long.toString(amount / 1000L) + "B";
			}
		}

		List<String> description = new ArrayList<String>();
		description.add(fluid.getLocalizedName(new FluidStack(fluid, 0)));
		description.add(amountToText);
		drawHoveringText(description, mouseX - guiFluidTerminal.guiLeft(), mouseY - guiFluidTerminal.guiTop() + 18, Minecraft.getMinecraft().fontRendererObj);
		return true;
	}

	@Override
	public void drawWidget(int posX, int posY) {
		Minecraft.getMinecraft().renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

		GlStateManager.disableLighting();
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

		IAEFluidStack terminalFluid = ((IFluidSelectorGui) guiFluidTerminal).getCurrentFluid();
		Fluid currentFluid = terminalFluid != null ? terminalFluid.getFluid() : null;
		if (fluid != null) {
			TextureMap map = Minecraft.getMinecraft().getTextureMapBlocks();
			TextureAtlasSprite sprite = map.getAtlasSprite(fluid.getStill().toString());
			drawTexturedModalRect(posX + 1, posY + 1, sprite, height - 2, width - 2);
		}
		if (fluid == currentFluid) {
			drawHollowRectWithCorners(posX, posY, height, width, color, borderThickness);
		}
		GlStateManager.enableLighting();
		GlStateManager.disableBlend();
		RenderHelper.enableStandardItemLighting();
	}

	public long getAmount() {
		return amount;
	}

	@Override
	public void mouseClicked(int posX, int posY, int mouseX, int mouseY) {
		if (fluid != null && isPointInRegion(posX, posY, height, width, mouseX, mouseY)) {
			((IFluidSelectorGui) guiFluidTerminal).getContainer().setSelectedFluid(fluid);
		}
	}

	public void setAmount(long amount) {
		this.amount = amount;
	}
}
