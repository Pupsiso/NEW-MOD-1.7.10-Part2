package crazypants.enderio.conduit.gas;

import com.enderio.core.common.util.BlockCoord;
import crazypants.enderio.conduit.IConduit;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.IGasHandler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class GasConduitNetwork extends AbstractGasTankConduitNetwork<GasConduit>
{

	private final ConduitGasTank tank = new ConduitGasTank(0);

	private final Set<GasOutput> outputs = new HashSet<GasOutput>();

	private Iterator<GasOutput> outputIterator;

	private int ticksActiveUnsynced;

	private boolean lastSyncedActive = false;

	private int lastSyncedVolume = -1;

	public GasConduitNetwork()
	{
		super(GasConduit.class);
	}

	@Override
	public void addConduit(GasConduit con)
	{
		this.tank.setCapacity(this.tank.getMaxGas() + GasConduit.CONDUIT_VOLUME);
		if (con.getTank().containsValidGas())
			this.tank.addAmount(con.getTank().getStored());
		for (ForgeDirection dir : con.getExternalConnections())
		{
			if (con.getConnectionMode(dir).acceptsOutput())
				this.outputs.add(new GasOutput(con.getLocation().getLocation(dir), dir.getOpposite()));
		}
		this.outputIterator = null;
		super.addConduit(con);
	}

	@Override
	public boolean setGasType(GasStack newType)
	{
		if (super.setGasType(newType))
		{

			GasStack ft = this.getGasType();
			this.tank.setGas(ft == null ? null : ft.copy());
			return true;
		}
		return false;
	}

	@Override
	public void destroyNetwork()
	{
		this.setConduitVolumes();
		this.outputs.clear();
		super.destroyNetwork();
	}

	private void setConduitVolumes()
	{
		if (this.tank.containsValidGas() && !this.conduits.isEmpty())
		{
			GasStack gasPerConduit = this.tank.getGas().copy();
			int numCons = this.conduits.size();
			int leftOvers = gasPerConduit.amount % numCons;
			gasPerConduit.amount = gasPerConduit.amount / numCons;

			for (GasConduit con : this.conduits)
			{
				GasStack f = gasPerConduit.copy();
				if (leftOvers > 0)
				{
					f.amount += 1;
					leftOvers--;
				}
				con.getTank().setGas(f);
				BlockCoord bc = con.getLocation();
				con.getBundle().getEntity().getWorldObj().markTileEntityChunkModified(bc.x, bc.y, bc.z, con.getBundle().getEntity());
			}

		}
	}

	@Override
	public void doNetworkTick()
	{
		if (this.gasType == null || this.outputs.isEmpty() || !this.tank.containsValidGas() || this.tank.isEmpty())
		{
			this.updateActiveState();
			return;
		}

		if (this.outputIterator == null || !this.outputIterator.hasNext())
			this.outputIterator = this.outputs.iterator();

		this.updateActiveState();

		int numVisited = 0;
		while (!this.tank.isEmpty() && numVisited < this.outputs.size())
		{
			if (!this.outputIterator.hasNext())
				this.outputIterator = this.outputs.iterator();
			GasOutput output = this.outputIterator.next();
			if (output != null)
			{
				IGasHandler cont = this.getTankContainer(output.location);
				if (cont != null)
				{
					GasStack offer = this.tank.getGas().copy();
					int filled = cont.receiveGas(output.dir, offer);
					if (filled > 0)
						this.tank.addAmount(-filled);
				}
			}
			numVisited++;
		}

	}

	private void updateActiveState()
	{
		boolean isActive = this.tank.containsValidGas() && !this.tank.isEmpty();
		if (this.lastSyncedActive != isActive)
			this.ticksActiveUnsynced++;
		else
			this.ticksActiveUnsynced = 0;
		if (this.ticksActiveUnsynced >= 10 || this.ticksActiveUnsynced > 0 && isActive)
		{
			if (!isActive)
				this.setGasType(null);
			for (IConduit con : this.conduits)
			{
				con.setActive(isActive);
			}
			this.lastSyncedActive = isActive;
			this.ticksActiveUnsynced = 0;
		}
	}

	public int fill(ForgeDirection from, GasStack resource, boolean doFill)
	{
		if (resource == null)
			return 0;
		resource.amount = Math.min(resource.amount, GasConduit.MAX_IO_PER_TICK);
		boolean gasWasValid = this.tank.containsValidGas();
		int res = this.tank.receive(resource, doFill);
		if (doFill && res > 0 && gasWasValid)
		{
			int vol = this.tank.getStored();
			this.setGasType(resource);
			this.tank.setAmount(vol);
		}
		return res;
	}

	public GasStack drain(ForgeDirection from, GasStack resource, boolean doDrain)
	{
		if (resource == null || this.tank.isEmpty() || !this.tank.containsValidGas() || !AbstractGasTankConduitNetwork.areGassCompatable(this.getGasType(), resource))
			return null;
		int amount = Math.min(resource.amount, this.tank.getStored());
		amount = Math.min(amount, GasConduit.MAX_IO_PER_TICK);
		GasStack result = resource.copy();
		result.amount = amount;
		if (doDrain)
			this.tank.addAmount(-amount);
		return result;
	}

	public GasStack drain(ForgeDirection from, int maxDrain, boolean doDrain)
	{
		if (this.tank.isEmpty() || !this.tank.containsValidGas())
			return null;
		int amount = Math.min(maxDrain, this.tank.getStored());
		GasStack result = this.tank.getGas().copy();
		result.amount = amount;
		if (doDrain)
			this.tank.addAmount(-amount);
		return result;
	}

	public boolean extractFrom(GasConduit advancedGasConduit, ForgeDirection dir, int maxExtractPerTick)
	{

		if (this.tank.isFull())
			return false;

		IGasHandler extTank = this.getTankContainer(advancedGasConduit, dir);
		if (extTank != null)
		{
			int maxExtract = Math.min(maxExtractPerTick, this.tank.getAvailableSpace());

			if (this.gasType == null || !this.tank.containsValidGas())
			{
				GasStack drained = extTank.drawGas(dir.getOpposite(), maxExtract);
				if (drained == null || drained.amount <= 0)
					return false;
				this.setGasType(drained);
				this.tank.setGas(drained.copy());
				return true;
			}

			GasStack couldDrain = this.gasType.copy();
			couldDrain.amount = maxExtract;

			//      GasStack drained = extTank.drain(dir.getOpposite(), couldDrain, true);
			//      if(drained == null || drained.amount <= 0) {
			//        return false;
			//      }
			//      tank.addAmount(drained.amount);

			//Have to use this 'double handle' approach to work around an issue with TiC
			GasStack drained = extTank.drawGas(dir.getOpposite(), maxExtract);
			if (drained == null || drained.amount == 0)
				return false;
			else if (drained.isGasEqual(this.getGasType()))
				this.tank.addAmount(drained.amount);
			return true;
		}
		return false;
	}

	public IGasHandler getTankContainer(BlockCoord bc)
	{
		World w = this.getWorld();
		if (w == null)
			return null;

		// TODO gamerforEA code replace, old code:
		// TileEntity te = w.getTileEntity(bc.x, bc.y, bc.z);
		TileEntity te = bc.getSafeTileEntity(w);
		// TODO gamerforEA code end

		if (te instanceof IGasHandler)
			return (IGasHandler) te;
		return null;
	}

	public IGasHandler getTankContainer(GasConduit con, ForgeDirection dir)
	{
		BlockCoord bc = con.getLocation().getLocation(dir);
		return this.getTankContainer(bc);
	}

	World getWorld()
	{
		if (this.conduits.isEmpty())
			return null;
		return this.conduits.get(0).getBundle().getWorld();
	}

	public void removeInput(GasOutput lo)
	{
		this.outputs.remove(lo);
		this.outputIterator = null;
	}

	public void addInput(GasOutput lo)
	{
		this.outputs.add(lo);
		this.outputIterator = null;
	}

	public void updateConduitVolumes()
	{
		if (this.tank.getStored() == this.lastSyncedVolume)
			return;
		this.setConduitVolumes();
		this.lastSyncedVolume = this.tank.getStored();
	}

}
