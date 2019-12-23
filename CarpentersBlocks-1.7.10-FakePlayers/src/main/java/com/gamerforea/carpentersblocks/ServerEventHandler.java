package com.gamerforea.carpentersblocks;

import com.carpentersblocks.block.BlockCarpentersSafe;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;

public final class ServerEventHandler
{
	public static void init()
	{
		MinecraftForge.EVENT_BUS.register(new ServerEventHandler());
	}

	@SubscribeEvent
	public void onBlockBreak(BlockEvent.BreakEvent event)
	{
		if (EventConfig.extendedSafeProtection && event.block instanceof BlockCarpentersSafe)
			if (!((BlockCarpentersSafe) event.block).hasAccess(event.world, event.x, event.y, event.z, event.getPlayer()))
				event.setCanceled(true);
	}
}
