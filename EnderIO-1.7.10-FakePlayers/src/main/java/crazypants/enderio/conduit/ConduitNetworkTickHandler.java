package crazypants.enderio.conduit;

import com.gamerforea.enderio.EventConfig;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import net.minecraft.tileentity.TileEntity;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;

public class ConduitNetworkTickHandler
{
	public static final ConduitNetworkTickHandler instance = new ConduitNetworkTickHandler();

	public interface TickListener
	{
		void tickStart(TickEvent.ServerTickEvent evt);

		void tickEnd(TickEvent.ServerTickEvent evt);
	}

	private final List<TickListener> listeners = new ArrayList<TickListener>();

	private final IdentityHashMap<AbstractConduitNetwork<?, ?>, Boolean> networks = new IdentityHashMap<AbstractConduitNetwork<?, ?>, Boolean>();

	public void addListener(TickListener listener)
	{
		this.listeners.add(listener);
	}

	public void removeListener(TickListener listener)
	{
		this.listeners.remove(listener);
	}

	public void registerNetwork(AbstractConduitNetwork<?, ?> cn)
	{
		this.networks.put(cn, Boolean.TRUE);
	}

	public void unregisterNetwork(AbstractConduitNetwork<?, ?> cn)
	{
		this.networks.remove(cn);
	}

	@SubscribeEvent
	public void onServerTick(TickEvent.ServerTickEvent event)
	{
		if (event.phase == Phase.START)
			this.tickStart(event);
		else
			this.tickEnd(event);
	}

	public void tickStart(TickEvent.ServerTickEvent event)
	{
		for (TickListener h : this.listeners)
		{
			h.tickStart(event);
		}
	}

	public void tickEnd(TickEvent.ServerTickEvent event)
	{
		for (TickListener h : this.listeners)
		{
			h.tickEnd(event);
		}
		this.listeners.clear();

		/* TODO gamerforEA code start
		for (AbstractConduitNetwork<?, ?> cn : this.networks.keySet())
		{
			cn.doNetworkTick();
		} */
		if (EventConfig.removeInvalidConduitsFromNetwork)
		{
			List<IConduit> conduitsForRemove = new ArrayList<IConduit>();

			for (AbstractConduitNetwork<?, ?> cn : this.networks.keySet())
			{
				for (Iterator<? extends IConduit> iterator = cn.conduits.iterator(); iterator.hasNext(); )
				{
					IConduit conduit = iterator.next();
					if (conduit == null)
						iterator.remove();
					else
					{
						IConduitBundle bundle = conduit.getBundle();
						if (bundle == null)
							iterator.remove();
						else if (bundle instanceof TileEntity && ((TileEntity) bundle).isInvalid())
						{
							iterator.remove();
							conduitsForRemove.add(conduit);
						}
					}
				}
			}

			for (IConduit conduit : conduitsForRemove)
			{
				IConduitBundle bundle = conduit.getBundle();
				if (bundle instanceof TileConduitBundle)
					((TileConduitBundle) bundle).removeConduit(conduit, false);
				else
					bundle.removeConduit(conduit);
			}
		}

		AbstractConduitNetwork[] keys = this.networks.keySet().toArray(new AbstractConduitNetwork[0]);
		boolean checkExistence = false;
		for (AbstractConduitNetwork<?, ?> cn : keys)
		{
			// TODO gamerforEA code start
			if (checkExistence || keys.length != this.networks.size())
			{
				checkExistence = true;
				if (!this.networks.containsKey(cn))
					continue;
			}
			// TODO gamerforEA code end

			cn.doNetworkTick();
		}
		// TODO gamerforEA code end
	}

}
