package crazypants.enderio.conduit.power;

import com.enderio.core.client.render.BoundingBox;
import com.enderio.core.client.render.IconUtil;
import com.enderio.core.common.util.DyeColor;
import com.enderio.core.common.vecmath.Vector3d;
import com.gamerforea.enderio.ModUtils;
import crazypants.enderio.EnderIO;
import crazypants.enderio.conduit.*;
import crazypants.enderio.conduit.geom.CollidableCache.CacheKey;
import crazypants.enderio.conduit.geom.CollidableComponent;
import crazypants.enderio.conduit.geom.ConduitGeometryUtil;
import crazypants.enderio.config.Config;
import crazypants.enderio.item.PacketConduitProbe;
import crazypants.enderio.machine.RedstoneControlMode;
import crazypants.enderio.power.BasicCapacitor;
import crazypants.enderio.power.ICapacitor;
import crazypants.enderio.power.IPowerInterface;
import crazypants.enderio.power.PowerHandlerUtil;
import crazypants.enderio.tool.ToolUtil;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.*;
import java.util.Map.Entry;

public class PowerConduit extends AbstractConduit implements IPowerConduit
{

	static final Map<String, IIcon> ICONS = new HashMap<String, IIcon>();

	private static ICapacitor[] capacitors;

	static final String[] POSTFIX = { "", "Enhanced", "Ender" };

	static ICapacitor[] getCapacitors()
	{
		if (capacitors == null)
			capacitors = new BasicCapacitor[] { new BasicCapacitor(Config.powerConduitTierOneRF, Config.powerConduitTierOneRF), new BasicCapacitor(Config.powerConduitTierTwoRF, Config.powerConduitTierTwoRF), new BasicCapacitor(Config.powerConduitTierThreeRF, Config.powerConduitTierThreeRF) };
		return capacitors;
	}

	static ItemStack createItemStackForSubtype(int subtype)
	{
		ItemStack result = new ItemStack(EnderIO.itemPowerConduit, 1, subtype);
		return result;
	}

	public static void initIcons()
	{
		IconUtil.addIconProvider(new IconUtil.IIconProvider()
		{

			@Override
			public void registerIcons(IIconRegister register)
			{
				for (String pf : POSTFIX)
				{
					ICONS.put(ICON_KEY + pf, register.registerIcon(ICON_KEY + pf));
					ICONS.put(ICON_KEY_INPUT + pf, register.registerIcon(ICON_KEY_INPUT + pf));
					ICONS.put(ICON_KEY_OUTPUT + pf, register.registerIcon(ICON_KEY_OUTPUT + pf));
					ICONS.put(ICON_CORE_KEY + pf, register.registerIcon(ICON_CORE_KEY + pf));
				}
				ICONS.put(ICON_TRANSMISSION_KEY, register.registerIcon(ICON_TRANSMISSION_KEY));
			}

			@Override
			public int getTextureType()
			{
				return 0;
			}

		});
	}

	public static final float WIDTH = 0.075f;
	public static final float HEIGHT = 0.075f;

	public static final Vector3d MIN = new Vector3d(0.5f - WIDTH, 0.5 - HEIGHT, 0.5 - WIDTH);
	public static final Vector3d MAX = new Vector3d(MIN.x + WIDTH, MIN.y + HEIGHT, MIN.z + WIDTH);

	public static final BoundingBox BOUNDS = new BoundingBox(MIN, MAX);

	protected PowerConduitNetwork network;

	private int energyStoredRF;

	private int subtype;

	protected final EnumMap<ForgeDirection, RedstoneControlMode> rsModes = new EnumMap<ForgeDirection, RedstoneControlMode>(ForgeDirection.class);
	protected final EnumMap<ForgeDirection, DyeColor> rsColors = new EnumMap<ForgeDirection, DyeColor>(ForgeDirection.class);

	protected EnumMap<ForgeDirection, Long> recievedTicks;

	private final Map<ForgeDirection, Integer> externalRedstoneSignals = new HashMap<ForgeDirection, Integer>();

	private boolean redstoneStateDirty = true;

	public PowerConduit()
	{
	}

	public PowerConduit(int meta)
	{
		this.subtype = meta;
	}

	@Override
	public boolean getConnectionsDirty()
	{
		return this.connectionsDirty;
	}

	@Override
	public boolean onBlockActivated(EntityPlayer player, RaytraceResult res, List<RaytraceResult> all)
	{
		DyeColor col = DyeColor.getColorFromDye(player.getCurrentEquippedItem());
		if (ConduitUtil.isProbeEquipped(player))
		{
			if (!player.worldObj.isRemote)
				new PacketConduitProbe().sendInfoMessage(player, this);
			return true;
		}
		else if (col != null && res.component != null && this.isColorBandRendered(res.component.dir))
		{
			this.setExtractionSignalColor(res.component.dir, col);
			return true;
		}
		else if (ToolUtil.isToolEquipped(player))
			if (!this.getBundle().getEntity().getWorldObj().isRemote)
				if (res != null && res.component != null)
				{
					ForgeDirection connDir = res.component.dir;
					ForgeDirection faceHit = ForgeDirection.getOrientation(res.movingObjectPosition.sideHit);
					if (connDir == ForgeDirection.UNKNOWN || connDir == faceHit)
					{
						if (this.getConnectionMode(faceHit) == ConnectionMode.DISABLED)
						{
							this.setConnectionMode(faceHit, this.getNextConnectionMode(faceHit));
							return true;
						}
						// Attempt to join networks
						return ConduitUtil.joinConduits(this, faceHit);
					}
					else if (this.externalConnections.contains(connDir))
					{
						this.setConnectionMode(connDir, this.getNextConnectionMode(connDir));
						return true;
					}
					else if (this.containsConduitConnection(connDir))
					{
						ConduitUtil.disconectConduits(this, connDir);
						return true;
					}
				}
		return false;
	}

	private boolean isColorBandRendered(ForgeDirection dir)
	{
		return this.getConnectionMode(dir) != ConnectionMode.DISABLED && this.getExtractionRedstoneMode(dir) != RedstoneControlMode.IGNORE;
	}

	@Override
	public ICapacitor getCapacitor()
	{
		return getCapacitors()[this.subtype];
	}

	@Override
	public void setExtractionRedstoneMode(RedstoneControlMode mode, ForgeDirection dir)
	{
		this.rsModes.put(dir, mode);
		this.setClientStateDirty();
	}

	@Override
	public RedstoneControlMode getExtractionRedstoneMode(ForgeDirection dir)
	{
		RedstoneControlMode res = this.rsModes.get(dir);
		if (res == null)
			res = RedstoneControlMode.IGNORE;
		return res;
	}

	@Override
	public void setExtractionSignalColor(ForgeDirection dir, DyeColor col)
	{
		this.rsColors.put(dir, col);
		this.setClientStateDirty();
	}

	@Override
	public DyeColor getExtractionSignalColor(ForgeDirection dir)
	{
		DyeColor res = this.rsColors.get(dir);
		if (res == null)
			res = DyeColor.RED;
		return res;
	}

	@Override
	protected void readTypeSettings(ForgeDirection dir, NBTTagCompound dataRoot)
	{
		this.setExtractionSignalColor(dir, DyeColor.values()[dataRoot.getShort("extractionSignalColor")]);
		this.setExtractionRedstoneMode(RedstoneControlMode.values()[dataRoot.getShort("extractionRedstoneMode")], dir);
	}

	@Override
	protected void writeTypeSettingsToNbt(ForgeDirection dir, NBTTagCompound dataRoot)
	{
		dataRoot.setShort("extractionSignalColor", (short) this.getExtractionSignalColor(dir).ordinal());
		dataRoot.setShort("extractionRedstoneMode", (short) this.getExtractionRedstoneMode(dir).ordinal());
	}

	@Override
	public void writeToNBT(NBTTagCompound nbtRoot)
	{
		super.writeToNBT(nbtRoot);
		nbtRoot.setShort("subtype", (short) this.subtype);
		nbtRoot.setInteger("energyStoredRF", this.energyStoredRF);

		for (Entry<ForgeDirection, RedstoneControlMode> entry : this.rsModes.entrySet())
		{
			if (entry.getValue() != null)
			{
				short ord = (short) entry.getValue().ordinal();
				nbtRoot.setShort("pRsMode." + entry.getKey().name(), ord);
			}
		}

		for (Entry<ForgeDirection, DyeColor> entry : this.rsColors.entrySet())
		{
			if (entry.getValue() != null)
			{
				short ord = (short) entry.getValue().ordinal();
				nbtRoot.setShort("pRsCol." + entry.getKey().name(), ord);
			}
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbtRoot, short nbtVersion)
	{
		super.readFromNBT(nbtRoot, nbtVersion);
		this.subtype = nbtRoot.getShort("subtype");

		if (nbtRoot.hasKey("energyStored"))
			nbtRoot.setInteger("energyStoredRF", (int) (nbtRoot.getFloat("energyStored") * 10));
		this.setEnergyStored(nbtRoot.getInteger("energyStoredRF"));

		for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS)
		{
			String key = "pRsMode." + dir.name();
			if (nbtRoot.hasKey(key))
			{
				short ord = nbtRoot.getShort(key);
				if (ord >= 0 && ord < RedstoneControlMode.values().length)
					this.rsModes.put(dir, RedstoneControlMode.values()[ord]);
			}
			key = "pRsCol." + dir.name();
			if (nbtRoot.hasKey(key))
			{
				short ord = nbtRoot.getShort(key);
				if (ord >= 0 && ord < DyeColor.values().length)
					this.rsColors.put(dir, DyeColor.values()[ord]);
			}
		}
	}

	@Override
	public void onTick()
	{
	}

	@Override
	public int getEnergyStored()
	{
		return this.energyStoredRF;
	}

	@Override
	public void setEnergyStored(int energyStored)
	{
		this.energyStoredRF = MathHelper.clamp_int(energyStored, 0, this.getMaxEnergyStored());
	}

	private boolean isRedstoneEnabled(ForgeDirection dir)
	{
		boolean result;
		RedstoneControlMode mode = this.getExtractionRedstoneMode(dir);
		if (mode == RedstoneControlMode.NEVER)
			return false;
		if (mode == RedstoneControlMode.IGNORE)
			return true;

		DyeColor col = this.getExtractionSignalColor(dir);
		int signal = ConduitUtil.getInternalSignalForColor(this.getBundle(), col);
		int exSig = this.getExternalRedstoneSignalForDir(dir);
		boolean res;
		//if checking for a signal, either is fine
		//if checking for no signal, must be no signal from both
		if (mode == RedstoneControlMode.OFF)
			res = RedstoneControlMode.isConditionMet(mode, signal) && (col != DyeColor.RED || RedstoneControlMode.isConditionMet(mode, exSig));
		else
			res = RedstoneControlMode.isConditionMet(mode, signal) || col == DyeColor.RED && RedstoneControlMode.isConditionMet(mode, exSig);
		return res;
	}

	private int getExternalRedstoneSignalForDir(ForgeDirection dir)
	{
		if (this.redstoneStateDirty)
		{
			this.externalRedstoneSignals.clear();
			this.redstoneStateDirty = false;
		}
		Integer cached = this.externalRedstoneSignals.get(dir);
		int result;
		if (cached == null)
		{
			TileEntity te = this.getBundle().getEntity();
			result = te.getWorldObj().getStrongestIndirectPower(te.xCoord, te.yCoord, te.zCoord);
			this.externalRedstoneSignals.put(dir, result);
		}
		else
			result = cached;
		return result;
	}

	@Override
	public int getMaxEnergyRecieved(ForgeDirection dir)
	{
		ConnectionMode mode = this.getConnectionMode(dir);
		if (mode == ConnectionMode.OUTPUT || mode == ConnectionMode.DISABLED || !this.isRedstoneEnabled(dir))
			return 0;
		return this.getCapacitor().getMaxEnergyReceived();
	}

	@Override
	public int getMaxEnergyExtracted(ForgeDirection dir)
	{
		ConnectionMode mode = this.getConnectionMode(dir);
		if (mode == ConnectionMode.INPUT || mode == ConnectionMode.DISABLED || !this.isRedstoneEnabled(dir))
			return 0;
		if (this.recievedRfThisTick(dir))
			return 0;
		return this.getCapacitor().getMaxEnergyExtracted();
	}

	private boolean recievedRfThisTick(ForgeDirection dir)
	{
		if (this.recievedTicks == null || dir == null || this.recievedTicks.get(dir) == null || this.getBundle() == null || this.getBundle().getWorld() == null)
			return false;

		long curTick = this.getBundle().getWorld().getTotalWorldTime();
		long recT = this.recievedTicks.get(dir);
		return curTick - recT <= 5;
	}

	@Override
	public boolean onNeighborBlockChange(Block blockId)
	{
		this.redstoneStateDirty = true;
		if (this.network != null && this.network.powerManager != null)
			this.network.powerManager.receptorsChanged();
		return super.onNeighborBlockChange(blockId);
	}

	@Override
	public void setConnectionMode(ForgeDirection dir, ConnectionMode mode)
	{
		super.setConnectionMode(dir, mode);
		this.recievedTicks = null;
	}

	@Override
	public int receiveEnergy(ForgeDirection from, int maxReceive, boolean simulate)
	{
		if (this.getMaxEnergyRecieved(from) == 0 || maxReceive <= 0)
			return 0;
		int freeSpace = this.getMaxEnergyStored() - this.getEnergyStored();
		int result = Math.min(maxReceive, freeSpace);
		if (!simulate && result > 0)
		{
			this.setEnergyStored(this.getEnergyStored() + result);

			if (this.getBundle() != null)
			{
				if (this.recievedTicks == null)
					this.recievedTicks = new EnumMap<ForgeDirection, Long>(ForgeDirection.class);
				this.recievedTicks.put(from, this.getBundle().getWorld().getTotalWorldTime());
			}

		}
		return result;
	}

	@Override
	public int extractEnergy(ForgeDirection from, int maxExtract, boolean simulate)
	{
		return 0;
	}

	@Override
	public boolean canConnectEnergy(ForgeDirection from)
	{
		return true;
	}

	@Override
	public int getEnergyStored(ForgeDirection from)
	{
		return this.getEnergyStored();
	}

	@Override
	public int getMaxEnergyStored(ForgeDirection from)
	{
		return this.getMaxEnergyStored();
	}

	@Override
	public AbstractConduitNetwork<?, ?> getNetwork()
	{
		return this.network;
	}

	@Override
	public boolean setNetwork(AbstractConduitNetwork<?, ?> network)
	{
		this.network = (PowerConduitNetwork) network;
		return true;
	}

	@Override
	public boolean canConnectToExternal(ForgeDirection direction, boolean ignoreDisabled)
	{
		IPowerInterface rec = this.getExternalPowerReceptor(direction);

		return rec != null && rec.canConduitConnect(direction);
	}

	@Override
	public boolean canConnectToConduit(ForgeDirection direction, IConduit conduit)
	{
		boolean res = super.canConnectToConduit(direction, conduit);
		if (!res)
			return false;
		if (Config.powerConduitCanDifferentTiersConnect)
			return res;
		if (!(conduit instanceof IPowerConduit))
			return false;
		IPowerConduit pc = (IPowerConduit) conduit;
		return pc.getMaxEnergyStored() == this.getMaxEnergyStored();
	}

	@Override
	public void externalConnectionAdded(ForgeDirection direction)
	{
		super.externalConnectionAdded(direction);
		if (this.network != null)
		{
			TileEntity te = this.bundle.getEntity();
			this.network.powerReceptorAdded(this, direction, te.xCoord + direction.offsetX, te.yCoord + direction.offsetY, te.zCoord + direction.offsetZ, this.getExternalPowerReceptor(direction));
		}
	}

	@Override
	public void externalConnectionRemoved(ForgeDirection direction)
	{
		super.externalConnectionRemoved(direction);
		if (this.network != null)
		{
			TileEntity te = this.bundle.getEntity();
			this.network.powerReceptorRemoved(te.xCoord + direction.offsetX, te.yCoord + direction.offsetY, te.zCoord + direction.offsetZ);
		}
	}

	@Override
	public IPowerInterface getExternalPowerReceptor(ForgeDirection direction)
	{
		TileEntity te = this.bundle.getEntity();
		World world = te.getWorldObj();
		if (world == null)
			return null;

		// TODO gamerforEA code replace, old code:
		// TileEntity test = world.getTileEntity(te.xCoord + direction.offsetX, te.yCoord + direction.offsetY, te.zCoord + direction.offsetZ);
		TileEntity test = ModUtils.getTileEntity(world, te.xCoord + direction.offsetX, te.yCoord + direction.offsetY, te.zCoord + direction.offsetZ);
		// TODO gamerforEA code end

		if (test == null)
			return null;
		if (test instanceof IConduitBundle)
			return null;
		return PowerHandlerUtil.create(test);
	}

	@Override
	public ItemStack createItem()
	{
		return createItemStackForSubtype(this.subtype);
	}

	@Override
	public Class<? extends IConduit> getBaseConduitType()
	{
		return IPowerConduit.class;
	}

	// Rendering
	@Override
	public IIcon getTextureForState(CollidableComponent component)
	{
		if (component.dir == ForgeDirection.UNKNOWN)
			return ICONS.get(ICON_CORE_KEY + POSTFIX[this.subtype]);
		if (COLOR_CONTROLLER_ID.equals(component.data))
			return IconUtil.whiteTexture;
		return ICONS.get(ICON_KEY + POSTFIX[this.subtype]);
	}

	@Override
	public IIcon getTextureForInputMode()
	{
		return ICONS.get(ICON_KEY_INPUT + POSTFIX[this.subtype]);
	}

	@Override
	public IIcon getTextureForOutputMode()
	{
		return ICONS.get(ICON_KEY_OUTPUT + POSTFIX[this.subtype]);
	}

	@Override
	public IIcon getTransmitionTextureForState(CollidableComponent component)
	{
		return null;
	}

	@Override
	public Collection<CollidableComponent> createCollidables(CacheKey key)
	{
		Collection<CollidableComponent> baseCollidables = super.createCollidables(key);
		if (key.dir == ForgeDirection.UNKNOWN)
			return baseCollidables;

		BoundingBox bb = ConduitGeometryUtil.instance.createBoundsForConnectionController(key.dir, key.offset);
		CollidableComponent cc = new CollidableComponent(IPowerConduit.class, bb, key.dir, COLOR_CONTROLLER_ID);

		List<CollidableComponent> result = new ArrayList<>(baseCollidables);
		result.add(cc);

		return result;
	}

	@Override
	public int getMaxEnergyStored()
	{
		return getCapacitors()[this.subtype].getMaxEnergyStored();
	}

	@Override
	public boolean displayPower()
	{
		return true;
	}
}
