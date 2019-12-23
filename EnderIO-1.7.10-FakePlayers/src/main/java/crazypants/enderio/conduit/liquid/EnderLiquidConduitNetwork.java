package crazypants.enderio.conduit.liquid;

import com.enderio.core.common.util.BlockCoord;
import com.enderio.core.common.util.RoundRobinIterator;
import com.gamerforea.enderio.ModUtils;
import crazypants.enderio.conduit.AbstractConduitNetwork;
import crazypants.enderio.conduit.ConnectionMode;
import crazypants.enderio.config.Config;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

import java.util.*;

public class EnderLiquidConduitNetwork extends AbstractConduitNetwork<ILiquidConduit, EnderLiquidConduit>
{

	public static final int MAX_EXTRACT_PER_TICK = Config.enderFluidConduitExtractRate;
	public static final int MAX_IO_PER_TICK = Config.enderFluidConduitMaxIoRate;

	List<NetworkTank> tanks = new ArrayList<NetworkTank>();
	Map<NetworkTankKey, NetworkTank> tankMap = new HashMap<NetworkTankKey, NetworkTank>();

	Map<NetworkTank, RoundRobinIterator<NetworkTank>> iterators;

	boolean filling;

	public EnderLiquidConduitNetwork()
	{
		super(EnderLiquidConduit.class, ILiquidConduit.class);
	}

	public void connectionChanged(EnderLiquidConduit con, ForgeDirection conDir)
	{
		NetworkTankKey key = new NetworkTankKey(con, conDir);
		NetworkTank tank = new NetworkTank(con, conDir);
		this.tanks.remove(tank); // remove old tank, NB: =/hash is only calced on location and dir
		this.tankMap.remove(key);
		this.tanks.add(tank);
		this.tankMap.put(key, tank);
	}

	public boolean extractFrom(EnderLiquidConduit con, ForgeDirection conDir)
	{
		NetworkTank tank = this.getTank(con, conDir);
		if (tank == null || !tank.isValid())
			return false;
		FluidStack drained = tank.externalTank.drain(conDir.getOpposite(), MAX_EXTRACT_PER_TICK, false);
		if (drained == null || drained.amount <= 0 || !this.matchedFilter(drained, con, conDir, true))
			return false;
		int amountAccepted = this.fillFrom(tank, drained.copy(), true);
		if (amountAccepted <= 0)
			return false;
		drained = tank.externalTank.drain(conDir.getOpposite(), amountAccepted, true);
		return drained != null && drained.amount > 0;
	}

	private NetworkTank getTank(EnderLiquidConduit con, ForgeDirection conDir)
	{
		return this.tankMap.get(new NetworkTankKey(con, conDir));
	}

	public int fillFrom(EnderLiquidConduit con, ForgeDirection conDir, FluidStack resource, boolean doFill)
	{
		return this.fillFrom(this.getTank(con, conDir), resource, doFill);
	}

	public int fillFrom(NetworkTank tank, FluidStack resource, boolean doFill)
	{

		if (this.filling)
			return 0;

		try
		{

			this.filling = true;

			if (resource == null || tank == null || !this.matchedFilter(resource, tank.con, tank.conDir, true))
				return 0;
			resource = resource.copy();
			resource.amount = Math.min(resource.amount, MAX_IO_PER_TICK);
			int filled = 0;
			int remaining = resource.amount;
			//TODO: Only change starting pos of iterator is doFill is true so a false then true returns the same

			for (NetworkTank target : this.getIteratorForTank(tank))
			{
				if (!target.equals(tank) && target.acceptsOuput && target.isValid() && this.matchedFilter(resource, target.con, target.conDir, false))
				{
					int vol = target.externalTank.fill(target.tankDir, resource.copy(), doFill);
					remaining -= vol;
					filled += vol;
					if (remaining <= 0)
						return filled;
					resource.amount = remaining;
				}
			}
			return filled;

		}
		finally
		{
			this.filling = false;
		}
	}

	private boolean matchedFilter(FluidStack drained, EnderLiquidConduit con, ForgeDirection conDir, boolean isInput)
	{
		if (drained == null || con == null || conDir == null)
			return false;
		FluidFilter filter = con.getFilter(conDir, isInput);
		if (filter == null || filter.isEmpty())
			return true;
		return filter.matchesFilter(drained);
	}

	private Iterable<NetworkTank> getIteratorForTank(NetworkTank tank)
	{
		if (this.iterators == null)
			this.iterators = new HashMap<NetworkTank, RoundRobinIterator<NetworkTank>>();
		RoundRobinIterator<NetworkTank> res = this.iterators.get(tank);
		if (res == null)
		{
			res = new RoundRobinIterator<NetworkTank>(this.tanks);
			this.iterators.put(tank, res);
		}
		return res;
	}

	public FluidTankInfo[] getTankInfo(EnderLiquidConduit con, ForgeDirection conDir)
	{
		List<FluidTankInfo> res = new ArrayList<FluidTankInfo>(this.tanks.size());
		NetworkTank tank = this.getTank(con, conDir);
		for (NetworkTank target : this.tanks)
		{
			if (!target.equals(tank) && target.isValid())
			{
				FluidTankInfo[] tTanks = target.externalTank.getTankInfo(target.tankDir);
				if (tTanks != null)
					Collections.addAll(res, tTanks);
			}
		}
		return res.toArray(new FluidTankInfo[0]);
	}

	static class NetworkTankKey
	{

		ForgeDirection conDir;
		BlockCoord conduitLoc;

		public NetworkTankKey(EnderLiquidConduit con, ForgeDirection conDir)
		{
			this(con.getLocation(), conDir);
		}

		public NetworkTankKey(BlockCoord conduitLoc, ForgeDirection conDir)
		{
			this.conDir = conDir;
			this.conduitLoc = conduitLoc;
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + (this.conDir == null ? 0 : this.conDir.hashCode());
			result = prime * result + (this.conduitLoc == null ? 0 : this.conduitLoc.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (this.getClass() != obj.getClass())
				return false;
			NetworkTankKey other = (NetworkTankKey) obj;
			if (this.conDir != other.conDir)
				return false;
			if (this.conduitLoc == null)
			{
				return other.conduitLoc == null;
			}
			else
				return this.conduitLoc.equals(other.conduitLoc);
		}

	}

	static class NetworkTank
	{

		EnderLiquidConduit con;
		ForgeDirection conDir;
		IFluidHandler externalTank;
		ForgeDirection tankDir;
		BlockCoord conduitLoc;
		boolean acceptsOuput;

		public NetworkTank(EnderLiquidConduit con, ForgeDirection conDir)
		{
			this.con = con;
			this.conDir = conDir;
			this.conduitLoc = con.getLocation();
			this.tankDir = conDir.getOpposite();
			this.externalTank = AbstractLiquidConduit.getExternalFluidHandler(con.getBundle().getWorld(), this.conduitLoc.getLocation(conDir));
			this.acceptsOuput = con.getConnectionMode(conDir).acceptsOutput();
		}

		public boolean isValid()
		{
			boolean isValid = this.externalTank != null && this.con.getConnectionMode(this.conDir) != ConnectionMode.DISABLED;

			// TODO gamerforEA code start
			if (isValid && this.externalTank instanceof TileEntity && !ModUtils.isValidTileEntity((TileEntity) this.externalTank))
				return false;
			// TODO gamerforEA code end

			return isValid;
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + (this.conDir == null ? 0 : this.conDir.hashCode());
			result = prime * result + (this.conduitLoc == null ? 0 : this.conduitLoc.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (this.getClass() != obj.getClass())
				return false;
			NetworkTank other = (NetworkTank) obj;
			if (this.conDir != other.conDir)
				return false;
			if (this.conduitLoc == null)
			{
				return other.conduitLoc == null;
			}
			else
				return this.conduitLoc.equals(other.conduitLoc);
		}

	}

}
