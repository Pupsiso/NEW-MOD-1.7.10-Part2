package net.divinerpg.client;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.RenderTickEvent;
import net.divinerpg.libs.Reference;
import net.divinerpg.utils.config.ConfigurationHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

public class ArcanaRenderer
{

	Minecraft mc = Minecraft.getMinecraft();

	public static float value;
	public static boolean regen;

	@SubscribeEvent
	public void onRender(RenderTickEvent event)
	{
		// TODO gamerforEA code clear:
		// this.onTickRender();
	}

	private void onTickRender()
	{
		/* TODO gamerforEA code clear:
		ConfigurationHelper cfg = new ConfigurationHelper();
		if (this.mc.currentScreen == null)
		{
			GuiIngame gig = this.mc.ingameGUI;
			ScaledResolution scaledresolution = new ScaledResolution(this.mc, this.mc.displayWidth, this.mc.displayHeight);
			int width = scaledresolution.getScaledWidth();
			int height = scaledresolution.getScaledHeight();
			this.mc.getTextureManager().bindTexture(new ResourceLocation(Reference.MOD_ID, "textures/gui/arcanaBar.png"));
			int y = height - ConfigurationHelper.arcanaY;
			int x = width - ConfigurationHelper.arcanaX;
			gig.drawTexturedModalRect(x, y, 0, 0, 100, 9);
			gig.drawTexturedModalRect(x, y, 0, 9, (int) (12.5 * (value / 25)), 18);
		} */
	}

	// TODO gamerforEA code start
	@SubscribeEvent
	public void onRenderOverlay(RenderGameOverlayEvent.Post event)
	{
		if (event.type == RenderGameOverlayEvent.ElementType.ALL)
		{
			GuiIngame gig = this.mc.ingameGUI;
			int width = event.resolution.getScaledWidth();
			int height = event.resolution.getScaledHeight();
			int y = height - ConfigurationHelper.arcanaY;
			int x = width - ConfigurationHelper.arcanaX;
			this.mc.getTextureManager().bindTexture(new ResourceLocation(Reference.MOD_ID, "textures/gui/arcanaBar.png"));
			gig.drawTexturedModalRect(x, y, 0, 0, 100, 9);
			gig.drawTexturedModalRect(x, y, 0, 9, (int) (12.5 * (value / 25)), 18);
		}
	}
	// TODO gamerforEA code end
}
