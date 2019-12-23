package com.gamerforea.divinerpg;

import com.gamerforea.eventhelper.EventHelper;
import com.gamerforea.eventhelper.util.ConvertUtils;
import com.gamerforea.eventhelper.util.EventUtils;
import com.gamerforea.eventhelper.util.FastUtils;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.UUID;

public final class ModUtils
{
	public static final GameProfile profile = new GameProfile(UUID.fromString("6762eb19-aa71-4346-963f-7c69cc8a8ab5"), "[DivineRPG]");
	private static FakePlayer player = null;

	public static FakePlayer getModFake(World world)
	{
		if (player == null)
			player = FastUtils.getFake(world, profile);
		else
			player.worldObj = world;

		return player;
	}

	public static ItemStack cloneStackWithSize(ItemStack stack, int stackSize)
	{
		if (stack == null)
			return null;
		ItemStack copy = stack.copy();
		copy.stackSize = stackSize;
		return copy;
	}

	public static boolean isMember(EntityPlayer player, int x, int y, int z)
	{
		FakePlayer fakePlayer = FastUtils.getFake(player.worldObj, player.getGameProfile());
		return !EventUtils.cantBreak(fakePlayer, x, y, z);
	}

	public static boolean cantTeleport(EntityPlayer player, World toWorld, double toX, double toY, double toZ)
	{
		try
		{
			Player bukkitPlayer = ConvertUtils.toBukkitEntity(player);
			org.bukkit.World bukkitWorld = ConvertUtils.toBukkitWorld(toWorld);
			if (bukkitWorld != null)
			{
				Location from = bukkitPlayer.getLocation();
				Location to = new Location(bukkitWorld, toX, toY, toZ, from.getYaw(), from.getPitch());
				PlayerTeleportEvent event = new PlayerTeleportEvent(bukkitPlayer, from, to, PlayerTeleportEvent.TeleportCause.PLUGIN);
				Bukkit.getPluginManager().callEvent(event);
				return event.isCancelled();
			}
		}
		catch (Exception e)
		{
			if (EventHelper.debug)
				e.printStackTrace();
		}
		return false;
	}

	public static boolean isValidPlayer(EntityPlayer player)
	{
		if (player == null || player instanceof FakePlayer)
			return false;

		if (player instanceof EntityPlayerMP)
		{
			EntityPlayerMP playerMp = (EntityPlayerMP) player;
			NetHandlerPlayServer netHandler = playerMp.playerNetServerHandler;
			return netHandler != null && netHandler.func_147362_b().isChannelOpen();
		}

		return true;
	}

	public static boolean isEquals(float a, float b)
	{
		return Math.abs(a - b) <= 0.0001f;
	}
}
