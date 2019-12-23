package crazypants.util;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Optional;
import micdoodle8.mods.galacticraft.core.Constants;
import micdoodle8.mods.galacticraft.core.entities.player.GCPlayerStats;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;

/**
 * This is a near-verbatim copy of Galacticraft's API class 'AccessInventoryGC'.
 * <p>
 * We don't need the extended functionality and already have more APIs included
 * than is good for our sanity.
 */
public class GalacticraftUtil
{
	/* TODO gamerforEA code replace, old code:
	private static Class<?> playerStatsClass;
	private static Method getMethod;
	private static Field extendedInventoryField;

	private static boolean accessFailed = false;

	public static IInventory getGCInventoryForPlayer(EntityPlayerMP player)
	{
		if (!accessFailed)
		{
			try
			{
				if (playerStatsClass == null || getMethod == null || extendedInventoryField == null)
				{
					playerStatsClass = Class.forName("micdoodle8.mods.galacticraft.core.entities.player.GCPlayerStats");
					getMethod = playerStatsClass.getMethod("get", EntityPlayerMP.class);
					extendedInventoryField = playerStatsClass.getField("extendedInventory");
				}
				Object stats = getMethod.invoke(null, player);
				return stats == null ? null : (IInventory) extendedInventoryField.get(stats);
			}
			catch (Exception e)
			{
				Log.info("Galacticraft inventory inaccessable. Most likely because it is not installed.");
				accessFailed = true;
			}
		}

		return null;
	} */
	private static final String MOD_ID_GC_CORE = Constants.MOD_ID_CORE;

	public static IInventory getGCInventoryForPlayer(EntityPlayerMP player)
	{
		return Loader.isModLoaded(MOD_ID_GC_CORE) ? getGCInventoryForPlayer0(player) : null;
	}

	@Optional.Method(modid = MOD_ID_GC_CORE)
	private static IInventory getGCInventoryForPlayer0(EntityPlayerMP player)
	{
		GCPlayerStats stats = GCPlayerStats.get(player);
		return stats == null ? null : stats.extendedInventory;
	}
	// TODO gamerforEA code end
}