package net.divinerpg.utils.proxies;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.divinerpg.client.ArcanaRenderer;
import net.divinerpg.client.render.BossTickHandler;
import net.divinerpg.client.render.entity.*;
import net.divinerpg.client.render.item.*;
import net.divinerpg.entities.fx.*;
import net.divinerpg.items.base.ItemModBow;
import net.divinerpg.items.base.ItemProjectileShooter;
import net.divinerpg.items.vethea.ItemStaff;
import net.divinerpg.libs.Reference;
import net.divinerpg.libs.Sounds;
import net.divinerpg.utils.LangRegistry;
import net.divinerpg.utils.LogHelper;
import net.divinerpg.utils.Util;
import net.divinerpg.utils.events.*;
import net.divinerpg.utils.items.ArcanaItems;
import net.divinerpg.utils.items.VanillaItemsWeapons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.particle.EntityFlameFX;
import net.minecraft.client.particle.EntityPortalFX;
import net.minecraft.item.Item;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.client.MinecraftForgeClient;

import java.awt.*;
import java.util.Random;

public class ClientProxy extends CommonProxy
{

	private static Random rand = new Random();

	@Override
	public void preInitClient(FMLPreInitializationEvent event)
	{
		// TODO gamerforEA code replace, old code:
		// Util.postFMLEvent(new ArcanaRenderer());
		Util.postForgeEvent(new ArcanaRenderer());
		// TODO gamerforEA code end

		Util.postFMLEvent(new ClientTicker());
		Util.postForgeEvent(new EventOverlay());
		Util.postForgeEvent(new EventExtraArmor());

		ItemProjectileShooter.gunList.remove(VanillaItemsWeapons.scythe);

		for (Item bow : ItemModBow.bowList)
		{
			MinecraftForgeClient.registerItemRenderer(bow, new RenderBow());
		}

		for (Item gun : ItemProjectileShooter.gunList)
		{
			MinecraftForgeClient.registerItemRenderer(gun, new RenderProjectileShooter());
		}

		for (Item phaser : ItemProjectileShooter.phaserList)
		{
			MinecraftForgeClient.registerItemRenderer(phaser, new RenderPhaser());
		}

		for (Item staff : ItemStaff.staffList)
		{
			MinecraftForgeClient.registerItemRenderer(staff, new RenderStaff());
		}

		MinecraftForgeClient.registerItemRenderer(VanillaItemsWeapons.corruptedCannon, new RenderProjectileShooter());
		MinecraftForgeClient.registerItemRenderer(ArcanaItems.arcaniteBlaster, new RenderArcaniteBlaster());

		if (Reference.DEBUG)
		{
			LogHelper.dev("Entering DEBUG mode");
			LangRegistry.registerNames();
			Sounds.init();
		}
	}

	@Override
	public void renderThings()
	{
		VanillaEntityRenderer.init();
		TwilightEntityRenderer.init();
		IceikaEntityRenderer.init();
		VetheaEntityRenderer.init();
		ArcanaEntityRenderer.init();
		Util.postFMLEvent(new BossTickHandler());
		Util.postForgeEvent(new DevHatEvent());
	}

	@Override
	public void spawnParticle(World w, double x, double y, double z, String particle, boolean random)
	{
		this.spawnParticle(w, x, y, z, particle, random, 1);
	}

	@Override
	public void spawnParticle(World w, double x, double y, double z, String particle, boolean random, int randFactor)
	{
		if (w == null)
			w = Minecraft.getMinecraft().theWorld;
		if (!w.getChunkProvider().chunkExists(MathHelper.floor_double(x / 16), MathHelper.floor_double(z / 16)))
			return;
		if (random)
		{
			x += (rand.nextDouble() - rand.nextDouble()) / (4 / randFactor);
			y += (rand.nextDouble() - rand.nextDouble()) / (4 / randFactor);
			z += (rand.nextDouble() - rand.nextDouble()) / (4 / randFactor);
		}
		EntityFX fx = null;
		if (particle.equals("eden"))
			fx = new EntityEdenPortalFX(w, x, y, z, 0.0D, 0.0D, 0.0D);
		else if (particle.equals("wildwood"))
			fx = new EntityWildwoodPortalFX(w, x, y, z, 0.0D, 0.0D, 0.0D);
		else if (particle.equals("apalachia"))
			fx = new EntityPortalFX(w, x, y, z, 0.0D, 0.0D, 0.0D);
		else if (particle.equals("skythern"))
			fx = new EntitySkythernPortalFX(w, x, y, z, 0.0D, 0.0D, 0.0D);
		else if (particle.equals("mortum"))
			fx = new EntityMortumPortalFX(w, x, y, z, 0.0D, 0.0D, 0.0D);
		else if (particle.equals("halite"))
			fx = new EntityGreenPortalFX(w, x, y, z, 0.0D, 0.0D, 0.0D);
		else if (particle.equals("greenFlame"))
			fx = new EntityGreenFlameFX(w, x, y, z, 0.0D, 0.0D, 0.0D);
		else if (particle.equals("blackFlame"))
			fx = new EntityBlackFlameFX(w, x, y, z, 0.0D, 0.0D, 0.0D);
		else if (particle.equals("blueFlame"))
			fx = new EntityBlueFlameFX(w, x, y, z, 0.0D, 0.0D, 0.0D);
		else if (particle.equals("orangeFlame"))
			fx = new EntityFlameFX(w, x, y, z, 0.0D, 0.0D, 0.0D);
		if (fx != null)
			FMLClientHandler.instance().getClient().effectRenderer.addEffect(fx);
	}

	@Override
	public void spawnParticle(World w, double x, double y, double z, Color c, boolean random)
	{
		if (w == null)
			w = Minecraft.getMinecraft().theWorld;
		if (!w.getChunkProvider().chunkExists(MathHelper.floor_double(x / 16), MathHelper.floor_double(z / 16)))
			return;
		if (random)
		{
			x += (rand.nextDouble() - rand.nextDouble()) / 4;
			y += (rand.nextDouble() - rand.nextDouble()) / 4;
			z += (rand.nextDouble() - rand.nextDouble()) / 4;
		}
		EntityFX fx = new EntityColoredFX(w, x, y, z, 0.0D, 0.0D, 0.0D, c);
		if (fx != null)
			FMLClientHandler.instance().getClient().effectRenderer.addEffect(fx);
	}

	@Override
	public void updateClientArcana(float amount)
	{
		ArcanaHelper.getProperties(Minecraft.getMinecraft().thePlayer).setBarValue(amount);
	}
}
