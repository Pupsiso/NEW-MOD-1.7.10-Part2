package emt.proxy;

import com.gamerforea.emt.EntityLightningBoltByPlayer;
import com.gamerforea.emt.EventConfig;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.IGuiHandler;
import emt.client.gui.GuiEtherealMacerator;
import emt.client.gui.GuiIndustrialWandRecharger;
import emt.client.gui.container.ContainerEtheralMacerator;
import emt.client.gui.container.ContainerIndustrialWandRecharge;
import emt.tile.TileEntityEtherealMacerator;
import emt.tile.TileEntityIndustrialWandRecharge;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityStruckByLightningEvent;

public class CommonProxy implements IGuiHandler
{
	public void load()
	{
		this.registerRenders();

		// TODO gamerforEA code start
		EventConfig.init();
		MinecraftForge.EVENT_BUS.register(this);
		// TODO gamerforEA code end
	}

	// TODO gamerforEA code start
	@SubscribeEvent
	public void onEntityStruckByLightning(EntityStruckByLightningEvent event)
	{
		if (event.lightning instanceof EntityLightningBoltByPlayer && ((EntityLightningBoltByPlayer) event.lightning).fake.cantDamage(event.entity))
			event.setCanceled(true);
	}
	// TODO gamerforEA code end

	public void registerRenders()
	{
		/* Empty in base proxy */
	}

	@Override
	public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z)
	{
		TileEntity entity = world.getTileEntity(x, y, z);

		switch (ID)
		{
			case 0:
				if (entity instanceof TileEntityIndustrialWandRecharge)
					return new ContainerIndustrialWandRecharge(player.inventory, (TileEntityIndustrialWandRecharge) entity);
			case 1:
				if (entity instanceof TileEntityEtherealMacerator)
					return new ContainerEtheralMacerator(player.inventory, entity);
			default:
				return null;
		}
	}

	@Override
	public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z)
	{
		TileEntity entity = world.getTileEntity(x, y, z);

		switch (ID)
		{
			case 0:
				if (entity instanceof TileEntityIndustrialWandRecharge)
					return new GuiIndustrialWandRecharger(player.inventory, (TileEntityIndustrialWandRecharge) entity);
			case 1:
				if (entity instanceof TileEntityEtherealMacerator)
					return new GuiEtherealMacerator(player.inventory, (TileEntityEtherealMacerator) entity);
			default:
				return null;
		}
	}
}
