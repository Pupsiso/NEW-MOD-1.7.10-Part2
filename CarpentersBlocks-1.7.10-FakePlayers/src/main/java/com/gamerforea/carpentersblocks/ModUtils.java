package com.gamerforea.carpentersblocks;

import com.gamerforea.eventhelper.EventHelper;
import com.gamerforea.eventhelper.nexus.ModNexus;
import com.gamerforea.eventhelper.nexus.ModNexusFactory;
import com.gamerforea.eventhelper.nexus.NexusUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import static com.gamerforea.eventhelper.util.ConvertUtils.*;

@ModNexus(name = "CarpentersBlocks", uuid = "ff7a14e5-322e-4d19-82c9-0a68225236ea")
public final class ModUtils
{
	public static final ModNexusFactory NEXUS_FACTORY = NexusUtils.getFactory();

	public static FakePlayer getModFake(World world)
	{
		return NEXUS_FACTORY.getFake(world);
	}

	public static boolean cantInteractPhysical(EntityPlayer player, ItemStack stack, int x, int y, int z, ForgeDirection side)
	{
		try
		{
			Player bPlayer = toBukkitEntity(player);
			PlayerInteractEvent event = new PlayerInteractEvent(bPlayer, Action.PHYSICAL, toBukkitItemStackMirror(stack), bPlayer.getWorld().getBlockAt(x, y, z), toBukkitFace(side));
			EventHelper.callEvent(event);
			return event.isCancelled();
		}
		catch (Throwable throwable)
		{
			System.err.println(String.format("Failed call PlayerInteractEvent: [Player: %s, Item: %s, X:%d, Y:%d, Z:%d, Side: %s]", String.valueOf(player), String.valueOf(stack), x, y, z, String.valueOf(side)));
			if (EventHelper.debug)
				throwable.printStackTrace();
			return true;
		}
	}

	public static void init()
	{
		EventConfig.init();
	}
}