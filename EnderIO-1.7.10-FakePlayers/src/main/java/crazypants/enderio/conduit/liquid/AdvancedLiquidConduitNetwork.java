package crazypants.enderio.conduit.liquid;

import com.enderio.core.common.util.BlockCoord;
import com.enderio.core.common.util.FluidUtil;
import crazypants.enderio.conduit.IConduit;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class AdvancedLiquidConduitNetwork extends AbstractTankConduitNetwork<AdvancedLiquidConduit>
{

	private final ConduitTank tank = new ConduitTank(0);

	private final Set<LiquidOutput> outputs = new HashSet<LiquidOutput>();

	private Iterator<LiquidOutput> outputIterator;

	private boolean lastSyncedActive = false;

	private int lastSyncedVolume = -1;

	private int ticksEmpty;

	public AdvancedLiquidConduitNetwork()
	{
		super(AdvancedLiquidConduit.class);
	}

	@Override
	public void addConduit(AdvancedLiquidConduit con)
	{
		this.tank.setCapacity(this.tank.getCapacity() + AdvancedLiquidConduit.CONDUIT_VOLUME);
		if (con.getTank().containsValidLiquid())
			this.tank.addAmount(con.getTank().getFluidAmount());
		for (ForgeDirection dir : con.getExternalConnections())
		{
			if (con.getConnectionMode(dir).acceptsOutput())
				this.outputs.add(new LiquidOutput(con.getLocation().getLocation(dir), dir.getOpposite()));
		}
		this.outputIterator = null;
		super.addConduit(con);
	}

	@Override
	public boolean setFluidType(FluidStack newType)
	{
		if (super.setFluidType(newType))
		{

			FluidStack ft = this.getFluidType();
			this.tank.setLiquid(ft == null ? null : ft.copy());
			return true;
		}
		return false;
	}

	@Override
	public void setFluidTypeLocked(boolean fluidTypeLocked)
	{
		super.setFluidTypeLocked(fluidTypeLocked);
		if (!fluidTypeLocked && this.tank.isEmpty())
			this.setFluidType(null);
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
		if (this.tank.containsValidLiquid() && !this.conduits.isEmpty())
		{
			FluidStack fluidPerConduit = this.tank.getFluid().copy();
			int numCons = this.conduits.size();
			int leftOvers = fluidPerConduit.amount % numCons;
			fluidPerConduit.amount = fluidPerConduit.amount / numCons;

			for (AdvancedLiquidConduit con : this.conduits)
			{
				FluidStack f = fluidPerConduit.copy();
				if (leftOvers > 0)
				{
					f.amount += 1;
					leftOvers--;
				}
				con.getTank().setLiquid(f);
				BlockCoord bc = con.getLocation();
				con.getBundle().getEntity().getWorldObj().markTileEntityChunkModified(bc.x, bc.y, bc.z, con.getBundle().getEntity());
			}

		}
	}

	@Override
	public void doNetworkTick()
	{
		if (this.liquidType == null || this.outputs.isEmpty() || !this.tank.containsValidLiquid() || this.tank.isEmpty())
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
			LiquidOutput output = this.outputIterator.next();
			if (output != null)
			{
				IFluidHandler cont = this.getTankContainer(output.location);
				if (cont != null)
				{
					FluidStack offer = this.tank.getFluid().copy();
					int filled = cont.fill(output.dir, offer, true);
					if (filled > 0)
						this.tank.addAmount(-filled);
				}
			}
			numVisited++;
		}

	}

	private void updateActiveState()
	{

		boolean isActive = this.tank.containsValidLiquid() && !this.tank.isEmpty();
		if (!isActive)
		{
			if (!this.fluidTypeLocked && this.liquidType != null)
			{
				this.ticksEmpty++;
				if (this.ticksEmpty > 40)
				{
					this.setFluidType(null);
					this.ticksEmpty = 0;
					for (IConduit con : this.conduits)
					{
						con.setActive(false);
					}
					this.lastSyncedActive = false;
				}
			}
			return;
		}

		this.ticksEmpty = 0;

		if (!this.lastSyncedActive)
		{
			for (IConduit con : this.conduits)
			{
				con.setActive(true);
			}
			this.lastSyncedActive = true;
		}

	}

	public int fill(ForgeDirection from, FluidStack resource, boolean doFill)
	{
		if (resource == null)
			return 0;
		resource.amount = Math.min(resource.amount, AdvancedLiquidConduit.MAX_IO_PER_TICK);
		boolean liquidWasValid = !this.tank.containsValidLiquid();
		int res = this.tank.fill(resource, doFill);
		if (doFill && res > 0 && !liquidWasValid)
		{
			int vol = this.tank.getFluidAmount();
			this.setFluidType(resource);
			this.tank.setAmount(vol);
		}
		return res;
	}

	public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain)
	{
		if (resource == null || this.tank.isEmpty() || !this.tank.containsValidLiquid() || !AbstractTankConduitNetwork.areFluidsCompatable(this.getFluidType(), resource))
			return null;
		int amount = Math.min(resource.amount, this.tank.getFluidAmount());
		amount = Math.min(amount, AdvancedLiquidConduit.MAX_IO_PER_TICK);
		FluidStack result = resource.copy();
		result.amount = amount;
		if (doDrain)
			this.tank.addAmount(-amount);
		return result;
	}

	public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain)
	{
		if (this.tank.isEmpty() || !this.tank.containsValidLiquid())
			return null;
		int amount = Math.min(maxDrain, this.tank.getFluidAmount());
		FluidStack result = this.tank.getFluid().copy();
		result.amount = amount;
		if (doDrain)
			this.tank.addAmount(-amount);
		return result;
	}

	public boolean extractFrom(AdvancedLiquidConduit advancedLiquidConduit, ForgeDirection dir, int maxExtractPerTick)
	{
		if (this.tank.isFull())
			return false;

		IFluidHandler extTank = this.getTankContainer(advancedLiquidConduit, dir);
		if (extTank != null)
		{
			int maxExtract = Math.min(maxExtractPerTick, this.tank.getAvailableSpace());

			if (this.liquidType == null || !this.tank.containsValidLiquid())
			{
				FluidStack drained = extTank.drain(dir.getOpposite(), maxExtract, true);
				if (drained == null || drained.amount <= 0)
					return false;
				this.setFluidType(drained);
				this.tank.setLiquid(drained.copy());
				return true;
			}

			FluidStack couldDrain = this.liquidType.copy();
			couldDrain.amount = maxExtract;

			boolean foundFluid = false;
			FluidTankInfo[] info = extTank.getTankInfo(dir.getOpposite());
			if (info != null)
				for (FluidTankInfo inf : info)
				{
					if (inf != null && inf.fluid != null && inf.fluid.amount > 0)
						foundFluid = true;
				}
			if (!foundFluid)
				return false;

			//      FluidStack drained = extTank.drain(dir.getOpposite(), couldDrain, true);
			//      if(drained == null || drained.amount <= 0) {
			//        return false;
			//      }
			//      tank.addAmount(drained.amount);

			//Have to use this 'double handle' approach to work around an issue with TiC
			FluidStack drained = extTank.drain(dir.getOpposite(), maxExtract, false);
			if (drained == null || drained.amount == 0)
				return false;
			if (drained.isFluidEqual(this.getFluidType()))
			{
				drained = extTank.drain(dir.getOpposite(), maxExtract, true);
				this.tank.addAmount(drained.amount);
			}
			return true;
		}
		return false;
	}

	public IFluidHandler getTankContainer(BlockCoord bc)
	{
		World w = this.getWorld();
		if (w == null)
			return null;

		// TODO gamerforEA code replace, old code:
		// return FluidUtil.getFluidHandler(w.getTileEntity(bc.x, bc.y, bc.z));
		return FluidUtil.getFluidHandler(bc.getSafeTileEntity(w));
		// TODO gamerforEA code end
	}

	public IFluidHandler getTankContainer(AdvancedLiquidConduit con, ForgeDirection dir)
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

	public void removeInput(LiquidOutput lo)
	{
		this.outputs.remove(lo);
		this.outputIterator = null;
	}

	public void addInput(LiquidOutput lo)
	{
		this.outputs.add(lo);
		this.outputIterator = null;
	}

	public void updateConduitVolumes()
	{
		if (this.tank.getFluidAmount() == this.lastSyncedVolume)
			return;
		this.setConduitVolumes();
		this.lastSyncedVolume = this.tank.getFluidAmount();
	}
}
