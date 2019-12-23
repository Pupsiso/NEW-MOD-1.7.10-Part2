package com.gamerforea.dragonsradio;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraftforge.common.util.FakePlayer;

public final class ModUtils
{
	public static boolean isRealPlayer(EntityPlayer player)
	{
		if (player == null)
			return false;

		if (player instanceof EntityPlayerMP)
		{
			if (player instanceof FakePlayer)
				return false;
			NetHandlerPlayServer netHandler = ((EntityPlayerMP) player).playerNetServerHandler;
			return netHandler != null && netHandler.netManager.isChannelOpen();
		}

		return true;
	}
}
