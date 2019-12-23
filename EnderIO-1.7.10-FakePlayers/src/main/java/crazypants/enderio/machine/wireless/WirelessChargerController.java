package crazypants.enderio.machine.wireless;

import com.enderio.core.common.util.BlockCoord;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import crazypants.enderio.config.Config;
import crazypants.util.BaublesUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

import java.util.*;

public class WirelessChargerController
{
	public static WirelessChargerController instance = new WirelessChargerController();

	public static final int RANGE = Config.wirelessChargerRange;
	public static final int RANGE_SQ = RANGE * RANGE;

	static
	{
		FMLCommonHandler.instance().bus().register(WirelessChargerController.instance);
		MinecraftForge.EVENT_BUS.register(WirelessChargerController.instance);
	}

	private final Map<Integer, Map<BlockCoord, IWirelessCharger>> perWorldChargers = new HashMap<Integer, Map<BlockCoord, IWirelessCharger>>();
	private int changeCount;

	private WirelessChargerController()
	{
	}

	public void registerCharger(IWirelessCharger charger)
	{
		if (charger == null)
			return;
		Map<BlockCoord, IWirelessCharger> chargers = this.getChargersForWorld(charger.getWorld());
		chargers.put(charger.getLocation(), charger);
		this.changeCount++;
	}

	public void deregisterCharger(IWirelessCharger capBank)
	{
		if (capBank == null)
			return;
		Map<BlockCoord, IWirelessCharger> chargers = this.getChargersForWorld(capBank.getWorld());
		chargers.remove(capBank.getLocation());
		this.changeCount++;
	}

	@SubscribeEvent
	public void onPlayerTick(TickEvent.PlayerTickEvent event)
	{
		/* TODO gamerforEA code clear:
		if (event.side == Side.CLIENT || event.phase != TickEvent.Phase.END)
			return;
		this.chargePlayersItems(event.player); */
	}

	// TODO gamerforEA code start
	@SubscribeEvent
	public void onWorldTick(TickEvent.WorldTickEvent event)
	{
		if (event.side == Side.CLIENT || event.phase != TickEvent.Phase.END)
			return;
		List<EntityPlayer> players = event.world.playerEntities;
		if (players.isEmpty())
			return;
		try
		{
			players = new ArrayList<EntityPlayer>(players);
		}
		catch (ConcurrentModificationException ignored)
		{
			return;
		}
		for (EntityPlayer player : players)
		{
			this.chargePlayersItems(player);
		}
	}
	// TODO gamerforEA code end

	public int getChangeCount()
	{
		return this.changeCount;
	}

	public void getChargers(World world, BlockCoord bc, Collection<IWirelessCharger> res)
	{
		Map<BlockCoord, IWirelessCharger> chargers = this.getChargersForWorld(world);
		for (IWirelessCharger wc : chargers.values())
		{
			if (wc.getLocation().getDistSq(bc) <= RANGE_SQ)
				res.add(wc);
		}
	}

	public void chargePlayersItems(EntityPlayer player)
	{
		// TODO gamerforEA code start
		if (player == null || player instanceof EntityPlayerMP && ((EntityPlayerMP) player).playerNetServerHandler == null)
			return;
		// TODO gamerforEA code end

		Map<BlockCoord, IWirelessCharger> chargers = this.getChargersForWorld(player.worldObj);
		if (chargers.isEmpty())
			return;
		BlockCoord bc = new BlockCoord(player);
		for (IWirelessCharger capBank : chargers.values())
		{
			// TODO gamerforEA code replace, old code:
			// if (capBank.isActive() && capBank.getLocation().getDistSq(bc) <= RANGE_SQ)
			if (capBank.getLocation().getDistSq(bc) <= RANGE_SQ && capBank.isActive())
			// TODO gamerforEA code end
			{
				boolean done = this.chargeFromCapBank(player, capBank);
				if (done)
					return;
			}
		}
	}

	private boolean chargeFromCapBank(EntityPlayer player, IWirelessCharger capBank)
	{
		boolean res = capBank.chargeItems(player.inventory.armorInventory);
		res |= capBank.chargeItems(player.inventory.mainInventory);
		IInventory baubles = BaublesUtil.instance().getBaubles(player);
		if (baubles != null)
		{
			ItemStack[] item = new ItemStack[1];
			for (int i = 0; i < baubles.getSizeInventory(); i++)
			{
				item[0] = baubles.getStackInSlot(i);
				if (capBank.chargeItems(item))
				{
					baubles.setInventorySlotContents(i, item[0]);
					res = true;
				}
			}
		}
		return res;
	}

	private Map<BlockCoord, IWirelessCharger> getChargersForWorld(World world)
	{
		Map<BlockCoord, IWirelessCharger> res = this.perWorldChargers.get(world.provider.dimensionId);
		if (res == null)
		{
			res = new HashMap<BlockCoord, IWirelessCharger>();
			this.perWorldChargers.put(world.provider.dimensionId, res);
		}
		return res;
	}

	public Collection<IWirelessCharger> getChargers(World world)
	{
		return this.getChargerMap(world).values();
	}

	public Map<BlockCoord, IWirelessCharger> getChargerMap(World world)
	{
		return this.perWorldChargers.get(world.provider.dimensionId);
	}
}
