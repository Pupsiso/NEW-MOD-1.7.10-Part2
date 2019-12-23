package crazypants.enderio.conduit.liquid;

import com.enderio.core.client.render.IconUtil;
import com.enderio.core.common.util.BlockCoord;
import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import crazypants.enderio.EnderIO;
import crazypants.enderio.conduit.AbstractConduitNetwork;
import crazypants.enderio.conduit.ConduitUtil;
import crazypants.enderio.conduit.ConnectionMode;
import crazypants.enderio.conduit.IConduit;
import crazypants.enderio.conduit.geom.CollidableComponent;
import crazypants.enderio.config.Config;
import crazypants.enderio.network.PacketHandler;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LiquidConduit extends AbstractTankConduit
{

	static final int VOLUME_PER_CONNECTION = FluidContainerRegistry.BUCKET_VOLUME / 4;

	public static final String ICON_KEY = "enderio:liquidConduit";
	public static final String ICON_KEY_LOCKED = "enderio:liquidConduitLocked";
	public static final String ICON_CORE_KEY = "enderio:liquidConduitCore";
	public static final String ICON_EXTRACT_KEY = "enderio:liquidConduitExtract";
	public static final String ICON_EMPTY_EXTRACT_KEY = "enderio:emptyLiquidConduitExtract";
	public static final String ICON_INSERT_KEY = "enderio:liquidConduitInsert";
	public static final String ICON_EMPTY_INSERT_KEY = "enderio:emptyLiquidConduitInsert";

	static final Map<String, IIcon> ICONS = new HashMap<String, IIcon>();

	@SideOnly(Side.CLIENT)
	public static void initIcons()
	{
		IconUtil.addIconProvider(new IconUtil.IIconProvider()
		{

			@Override
			public void registerIcons(IIconRegister register)
			{
				ICONS.put(ICON_KEY, register.registerIcon(ICON_KEY));
				ICONS.put(ICON_CORE_KEY, register.registerIcon(ICON_CORE_KEY));
				ICONS.put(ICON_EXTRACT_KEY, register.registerIcon(ICON_EXTRACT_KEY));
				ICONS.put(ICON_EMPTY_EXTRACT_KEY, register.registerIcon(ICON_EMPTY_EXTRACT_KEY));
				ICONS.put(ICON_EMPTY_INSERT_KEY, register.registerIcon(ICON_EMPTY_INSERT_KEY));
				ICONS.put(ICON_INSERT_KEY, register.registerIcon(ICON_INSERT_KEY));
				ICONS.put(ICON_KEY_LOCKED, register.registerIcon(ICON_KEY_LOCKED));
			}

			@Override
			public int getTextureType()
			{
				return 0;
			}

		});
	}

	private LiquidConduitNetwork network;

	private float lastSyncRatio = -99;

	private int currentPushToken;

	// -----------------------------

	public static final int MAX_EXTRACT_PER_TICK = Config.fluidConduitExtractRate;

	public static final int MAX_IO_PER_TICK = Config.fluidConduitMaxIoRate;

	private ForgeDirection startPushDir = ForgeDirection.DOWN;

	private final Set<BlockCoord> filledFromThisTick = new HashSet<BlockCoord>();

	private long ticksSinceFailedExtract = 0;

	@Override
	public void updateEntity(World world)
	{
		super.updateEntity(world);
		if (world.isRemote)
			return;
		this.filledFromThisTick.clear();
		this.updateStartPushDir();
		this.doExtract();

		if (this.stateDirty)
		{
			this.getBundle().dirty();
			this.stateDirty = false;
			this.lastSyncRatio = this.tank.getFilledRatio();

		}
		else if (this.lastSyncRatio != this.tank.getFilledRatio() && world.getTotalWorldTime() % 2 == 0)
		{

			//need to send a custom packet as we don't want want to trigger a full chunk update, just
			//need to get the required  values to the entity renderer
			BlockCoord loc = this.getLocation();
			PacketHandler.INSTANCE.sendToAllAround(new PacketFluidLevel(this), new TargetPoint(world.provider.dimensionId, loc.x, loc.y, loc.z, 64));
			this.lastSyncRatio = this.tank.getFilledRatio();
		}
	}

	private void doExtract()
	{
		if (!this.hasExtractableMode())
			return;

		// assume failure, reset to 0 if we do extract
		this.ticksSinceFailedExtract++;
		// after 10 ticks of failing, only check every 10 ticks
		if (this.ticksSinceFailedExtract > 9 && this.ticksSinceFailedExtract % 10 != 0)
			return;

		for (ForgeDirection dir : this.externalConnections)
		{
			if (this.autoExtractForDir(dir))
			{

				IFluidHandler extTank = this.getTankContainer(this.getLocation().getLocation(dir));
				if (extTank != null)
				{

					boolean foundFluid = false;
					FluidTankInfo[] info = extTank.getTankInfo(dir.getOpposite());
					if (info != null)
						for (FluidTankInfo inf : info)
						{
							if (inf != null && inf.fluid != null && inf.fluid.amount > 0)
								foundFluid = true;
						}
					if (!foundFluid)
						return;

					FluidStack couldDrain = extTank.drain(dir.getOpposite(), MAX_EXTRACT_PER_TICK, false);
					if (couldDrain != null && couldDrain.amount > 0 && this.canFill(dir, couldDrain.getFluid()))
					{
						int used = this.pushLiquid(dir, couldDrain, true, this.network == null ? -1 : this.network.getNextPushToken());
						extTank.drain(dir.getOpposite(), used, true);
						if (used > 0 && this.network != null && this.network.getFluidType() == null)
							this.network.setFluidType(couldDrain);
						if (used > 0)
							this.ticksSinceFailedExtract = 0;
					}
				}
			}
		}

	}

	@Override
	public int fill(ForgeDirection from, FluidStack resource, boolean doFill)
	{

		if (this.network == null || resource == null)
			return 0;
		if (!this.canFill(from, resource.getFluid()))
			return 0;

		// Note: This is just a guard against mekansims pipes that will continuously
		// call
		// fill on us if we push liquid to them.
		if (this.filledFromThisTick.contains(this.getLocation().getLocation(from)))
			return 0;

		if (this.network.lockNetworkForFill())
		{
			if (doFill)
				this.filledFromThisTick.add(this.getLocation().getLocation(from));
			try
			{
				int res = this.fill(from, resource, doFill, true, this.network == null ? -1 : this.network.getNextPushToken());
				if (doFill && this.externalConnections.contains(from) && this.network != null)
					this.network.addedFromExternal(res);
				return res;
			}
			finally
			{
				this.network.unlockNetworkFromFill();

			}
		}
		else
			return 0;

	}

	public int fill(ForgeDirection from, FluidStack resource, boolean doFill, boolean doPush, int pushToken)
	{
		if (resource == null || resource.amount <= 0)
			return 0;

		if (!this.canFill(from, resource.getFluid()))
			return 0;

		if (this.network == null)
			return 0;
		if (this.network.canAcceptLiquid(resource))
			this.network.setFluidType(resource);
		else
			return 0;
		resource = resource.copy();
		resource.amount = Math.min(MAX_IO_PER_TICK, resource.amount);

		if (doPush)
			return this.pushLiquid(from, resource, doFill, pushToken);
		else
			return this.tank.fill(resource, doFill);
	}

	private void updateStartPushDir()
	{

		ForgeDirection newVal = this.getNextDir(this.startPushDir);
		boolean foundNewStart = false;
		while (newVal != this.startPushDir && !foundNewStart)
		{
			foundNewStart = this.getConduitConnections().contains(newVal) || this.getExternalConnections().contains(newVal);
			newVal = this.getNextDir(newVal);
		}
		this.startPushDir = newVal;
	}

	private ForgeDirection getNextDir(ForgeDirection dir)
	{
		if (dir.ordinal() >= ForgeDirection.UNKNOWN.ordinal() - 1)
			return ForgeDirection.VALID_DIRECTIONS[0];
		return ForgeDirection.VALID_DIRECTIONS[dir.ordinal() + 1];
	}

	private int pushLiquid(ForgeDirection from, FluidStack pushStack, boolean doPush, int token)
	{
		if (token == this.currentPushToken || pushStack == null || pushStack.amount <= 0 || this.network == null)
			return 0;
		this.currentPushToken = token;
		int pushed = 0;
		int total = pushStack.amount;

		ForgeDirection dir = this.startPushDir;
		FluidStack toPush = pushStack.copy();

		int filledLocal = this.tank.fill(toPush, doPush);
		toPush.amount -= filledLocal;
		pushed += filledLocal;

		do
		{
			if (dir != from && this.canOutputToDir(dir) && !this.autoExtractForDir(dir))
				if (this.getConduitConnections().contains(dir))
				{
					ILiquidConduit conduitCon = this.getFluidConduit(dir);
					if (conduitCon != null)
					{
						int toCon = ((LiquidConduit) conduitCon).pushLiquid(dir.getOpposite(), toPush, doPush, token);
						toPush.amount -= toCon;
						pushed += toCon;
					}
				}
				else if (this.getExternalConnections().contains(dir))
				{
					IFluidHandler con = this.getTankContainer(this.getLocation().getLocation(dir));
					if (con != null)
					{
						int toExt = con.fill(dir.getOpposite(), toPush, doPush);
						toPush.amount -= toExt;
						pushed += toExt;
						if (doPush)
							this.network.outputedToExternal(toExt);
					}
				}
			dir = this.getNextDir(dir);
		}
		while (dir != this.startPushDir && pushed < total);

		return pushed;
	}

	private ILiquidConduit getFluidConduit(ForgeDirection dir)
	{
		TileEntity ent = this.getBundle().getEntity();
		return ConduitUtil.getConduit(ent.getWorldObj(), ent, dir, ILiquidConduit.class);
	}

	@Override
	public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain)
	{
		if (this.getConnectionMode(from) == ConnectionMode.INPUT || this.getConnectionMode(from) == ConnectionMode.DISABLED)
			return null;
		return this.tank.drain(maxDrain, doDrain);
	}

	@Override
	public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain)
	{
		if (resource == null || !resource.isFluidEqual(this.tank.getFluid()))
			return null;
		return this.drain(from, resource.amount, doDrain);
	}

	@Override
	public boolean canFill(ForgeDirection from, Fluid fluid)
	{
		if (this.getConnectionMode(from) == ConnectionMode.OUTPUT || this.getConnectionMode(from) == ConnectionMode.DISABLED)
			return false;
		if (this.tank.getFluid() == null)
			return true;
		return fluid != null && fluid.getID() == this.tank.getFluid().getFluidID();
	}

	@Override
	public boolean canDrain(ForgeDirection from, Fluid fluid)
	{
		if (this.getConnectionMode(from) == ConnectionMode.INPUT || this.getConnectionMode(from) == ConnectionMode.DISABLED || this.tank.getFluid() == null || fluid == null)
			return false;
		return this.tank.getFluid().getFluid().getID() == fluid.getID();
	}

	@Override
	public FluidTankInfo[] getTankInfo(ForgeDirection from)
	{
		return new FluidTankInfo[] { this.tank.getInfo() };
	}

	// -----------------------------

	@Override
	public void connectionsChanged()
	{
		super.connectionsChanged();
		this.updateTank();
	}

	@Override
	public ItemStack createItem()
	{
		return new ItemStack(EnderIO.itemLiquidConduit, 1, 0);
	}

	@Override
	public AbstractConduitNetwork<?, ?> getNetwork()
	{
		return this.network;
	}

	@Override
	public boolean setNetwork(AbstractConduitNetwork<?, ?> network)
	{
		if (network == null)
		{
			this.network = null;
			return true;
		}

		/* TODO gamerforEA code replace, old code:
		if (!(network instanceof AbstractTankConduitNetwork))
			return false; */
		if (!(network instanceof LiquidConduitNetwork))
			return false;
		// TODO gamerforEA code end

		AbstractTankConduitNetwork n = (AbstractTankConduitNetwork) network;
		if (this.tank.getFluid() == null)
			this.tank.setLiquid(n.getFluidType() == null ? null : n.getFluidType().copy());
		else if (n.getFluidType() == null)
			n.setFluidType(this.tank.getFluid());
		else if (!this.tank.getFluid().isFluidEqual(n.getFluidType()))
			return false;
		this.network = (LiquidConduitNetwork) network;
		return true;
	}

	@Override
	public boolean canConnectToConduit(ForgeDirection direction, IConduit con)
	{
		if (!super.canConnectToConduit(direction, con))
			return false;
		if (!(con instanceof LiquidConduit))
			return false;
		if (this.getFluidType() != null && ((LiquidConduit) con).getFluidType() == null)
			return false;
		return AbstractTankConduitNetwork.areFluidsCompatable(this.getFluidType(), ((LiquidConduit) con).getFluidType());
	}

	@Override
	public IIcon getTextureForState(CollidableComponent component)
	{
		if (component.dir == ForgeDirection.UNKNOWN)
			return ICONS.get(ICON_CORE_KEY);
		if (this.getConnectionMode(component.dir) == ConnectionMode.INPUT)
			return ICONS.get(this.getFluidType() == null ? ICON_EMPTY_EXTRACT_KEY : ICON_EXTRACT_KEY);
		if (this.getConnectionMode(component.dir) == ConnectionMode.OUTPUT)
			return ICONS.get(this.getFluidType() == null ? ICON_EMPTY_INSERT_KEY : ICON_INSERT_KEY);
		//    if(getFluidType() == null) {
		//      return ICONS.get(ICON_EMPTY_KEY);
		//    }
		return this.fluidTypeLocked ? ICONS.get(ICON_KEY_LOCKED) : ICONS.get(ICON_KEY);
	}

	@Override
	public IIcon getTransmitionTextureForState(CollidableComponent component)
	{
		if (this.tank.getFluid() != null && this.tank.getFluid().getFluid() != null)
			return this.tank.getFluid().getFluid().getStillIcon();
		return null;
	}

	@Override
	public float getTransmitionGeometryScale()
	{
		return this.tank.getFilledRatio();
	}

	@Override
	protected void updateTank()
	{
		int totalConnections = this.getConduitConnections().size() + this.getExternalConnections().size();
		this.tank.setCapacity(totalConnections * VOLUME_PER_CONNECTION);
	}

	@Override
	protected boolean canJoinNeighbour(ILiquidConduit n)
	{
		return n instanceof LiquidConduit;
	}

	@Override
	public AbstractTankConduitNetwork<? extends AbstractTankConduit> getTankNetwork()
	{
		return this.network;
	}

}
