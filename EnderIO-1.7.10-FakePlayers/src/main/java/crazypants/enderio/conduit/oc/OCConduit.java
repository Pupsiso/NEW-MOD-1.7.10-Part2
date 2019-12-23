package crazypants.enderio.conduit.oc;

import com.enderio.core.client.render.BoundingBox;
import com.enderio.core.client.render.IconUtil;
import com.enderio.core.common.util.BlockCoord;
import com.enderio.core.common.util.DyeColor;
import cpw.mods.fml.common.Optional.Method;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import crazypants.enderio.EnderIO;
import crazypants.enderio.conduit.*;
import crazypants.enderio.conduit.geom.CollidableCache.CacheKey;
import crazypants.enderio.conduit.geom.CollidableComponent;
import crazypants.enderio.conduit.geom.ConduitGeometryUtil;
import crazypants.enderio.config.Config;
import crazypants.enderio.tool.ToolUtil;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.*;

public class OCConduit extends AbstractConduit implements IOCConduit
{

	protected OCConduitNetwork network;

	private Map<ForgeDirection, DyeColor> signalColors = new HashMap<ForgeDirection, DyeColor>();

	public static IIcon[] coreTextures;
	public static IIcon[] longTextures;

	public OCConduit()
	{
	}

	public OCConduit(int meta)
	{
	}

	public static void initIcons()
	{
		IconUtil.addIconProvider(new IconUtil.IIconProvider()
		{

			@Override
			public void registerIcons(IIconRegister register)
			{
				coreTextures = new IIcon[2];
				longTextures = new IIcon[2];

				coreTextures[0] = register.registerIcon(EnderIO.DOMAIN + ":ocConduitCore");
				coreTextures[1] = register.registerIcon(EnderIO.DOMAIN + ":ocConduitCoreAnim");

				longTextures[0] = register.registerIcon(EnderIO.DOMAIN + ":ocConduit");
				longTextures[1] = register.registerIcon(EnderIO.DOMAIN + ":ocConduitAnim");
			}

			@Override
			public int getTextureType()
			{
				return 0;
			}

		});
	}

	@Override
	protected void readTypeSettings(ForgeDirection dir, NBTTagCompound dataRoot)
	{
		this.setSignalColor(dir, DyeColor.values()[dataRoot.getShort("signalColor")]);
	}

	@Override
	protected void writeTypeSettingsToNbt(ForgeDirection dir, NBTTagCompound dataRoot)
	{
		dataRoot.setShort("signalColor", (short) this.getSignalColor(dir).ordinal());
	}

	@Override
	public DyeColor getSignalColor(ForgeDirection dir)
	{
		DyeColor res = this.signalColors.get(dir);
		if (res == null)
			return DyeColor.SILVER;
		return res;
	}

	@Override
	public Collection<CollidableComponent> createCollidables(CacheKey key)
	{
		Collection<CollidableComponent> baseCollidables = super.createCollidables(key);
		if (key.dir == ForgeDirection.UNKNOWN)
			return baseCollidables;

		BoundingBox bb = ConduitGeometryUtil.instance.createBoundsForConnectionController(key.dir, key.offset);
		CollidableComponent cc = new CollidableComponent(IOCConduit.class, bb, key.dir, COLOR_CONTROLLER_ID);

		List<CollidableComponent> result = new ArrayList<CollidableComponent>(baseCollidables);
		result.add(cc);

		return result;
	}

	@Override
	public void writeToNBT(NBTTagCompound nbtRoot)
	{
		super.writeToNBT(nbtRoot);
		if (this.signalColors.size() >= 0)
		{
			byte[] modes = new byte[6];
			int i = 0;
			for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS)
			{
				DyeColor col = this.signalColors.get(dir);
				if (col != null)
					modes[i] = (byte) col.ordinal();
				else
					modes[i] = -1;
				i++;
			}
			nbtRoot.setByteArray("signalColors", modes);
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbtRoot, short nbtVersion)
	{
		super.readFromNBT(nbtRoot, nbtVersion);
		this.signalColors.clear();
		byte[] cols = nbtRoot.getByteArray("signalColors");
		if (cols != null && cols.length == 6)
		{
			int i = 0;
			for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS)
			{
				if (cols[i] >= 0)
					this.signalColors.put(dir, DyeColor.values()[cols[i]]);
				i++;
			}
		}
	}

	@Override
	public void setSignalColor(ForgeDirection dir, DyeColor col)
	{
		if (this.signalColors.get(dir) == col)
			return;
		this.disconnectNode(dir);
		this.signalColors.put(dir, col);
		this.addMissingNodeConnections();
		this.setClientStateDirty();
	}

	@Override
	public Class<? extends IConduit> getBaseConduitType()
	{
		return IOCConduit.class;
	}

	@Override
	public ItemStack createItem()
	{
		return new ItemStack(EnderIO.itemOCConduit, 1, 0);
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
			for (ForgeDirection dir : this.getExternalConnections())
			{
				this.disconnectNode(dir);
			}
		this.network = (OCConduitNetwork) network;
		this.addMissingNodeConnections();
		return true;
	}

	@Override
	public IIcon getTextureForState(CollidableComponent component)
	{
		int state = Config.enableOCConduitsAnimatedTexture ? 1 : 0;
		if (component.dir == ForgeDirection.UNKNOWN)
			return coreTextures[state];
		return longTextures[state];
	}

	@Override
	public boolean shouldMirrorTexture()
	{
		return !Config.enableOCConduitsAnimatedTexture;
	}

	@Override
	public IIcon getTransmitionTextureForState(CollidableComponent component)
	{
		return null;
	}

	private static String prettyNode(Node o)
	{
		String at = "";
		Environment host = o.host();
		if (host instanceof TileEntity)
		{
			BlockCoord bc = new BlockCoord((TileEntity) host);
			at = " at " + bc.x + "/" + bc.y + "/" + bc.z;
		}
		return host.getClass().getName().replaceFirst("^.*\\.", "") + at;
	}

	private static EnumChatFormatting dye2chat(DyeColor dyeColor)
	{
		switch (dyeColor)
		{
			case BLACK:
				return EnumChatFormatting.BLACK;
			case BLUE:
				return EnumChatFormatting.DARK_BLUE;
			case BROWN:
				return EnumChatFormatting.DARK_RED;
			case CYAN:
				return EnumChatFormatting.DARK_AQUA;
			// return EnumChatFormatting.AQUA;
			case GRAY:
				return EnumChatFormatting.DARK_GRAY;
			case GREEN:
				return EnumChatFormatting.DARK_GREEN;
			case LIGHT_BLUE:
				return EnumChatFormatting.BLUE;
			case LIME:
				return EnumChatFormatting.GREEN;
			case MAGENTA:
				return EnumChatFormatting.LIGHT_PURPLE;
			case ORANGE:
				return EnumChatFormatting.GOLD;
			case PINK:
				return EnumChatFormatting.LIGHT_PURPLE;
			case PURPLE:
				return EnumChatFormatting.DARK_PURPLE;
			case RED:
				return EnumChatFormatting.RED;
			case SILVER:
				return EnumChatFormatting.GRAY;
			case WHITE:
				return EnumChatFormatting.WHITE;
			case YELLOW:
				return EnumChatFormatting.YELLOW;
			default:
				return null;
		}
	}

	@Override
	public boolean onBlockActivated(EntityPlayer player, RaytraceResult res, List<RaytraceResult> all)
	{
		DyeColor col = DyeColor.getColorFromDye(player.getCurrentEquippedItem());
		if (col != null && res.component != null)
		{
			this.setSignalColor(res.component.dir, col);
			return true;
		}
		if (ConduitUtil.isProbeEquipped(player))
		{
			if (!player.worldObj.isRemote)
			{
				BlockCoord bc = this.getLocation();
				if (this.network != null)
				{
					boolean noconnections = true;
					for (DyeColor color : DyeColor.values())
					{
						if (this.node(color).neighbors().iterator().hasNext())
						{
							noconnections = false;
							ChatComponentText coltxt = new ChatComponentText(color.getLocalisedName());
							coltxt.getChatStyle().setColor(dye2chat(color));
							ChatComponentText chantxt = new ChatComponentText("Channel ");
							chantxt.appendSibling(coltxt);
							chantxt.appendText(" at " + bc.x + "/" + bc.y + "/" + bc.z);
							player.addChatMessage(chantxt);
							for (Node other : this.node(color).neighbors())
							{
								player.addChatMessage(new ChatComponentText("  Connected to: " + prettyNode(other)));
							}
						}
					}
					if (noconnections)
						player.addChatMessage(new ChatComponentText("No connections at " + bc.x + "/" + bc.y + "/" + bc.z));
				}
				else
					player.addChatMessage(new ChatComponentText("No network at " + bc.x + "/" + bc.y + "/" + bc.z));
			}
			return true;
		}
		if (ToolUtil.isToolEquipped(player))
			if (!this.getBundle().getEntity().getWorldObj().isRemote)
				if (res != null && res.component != null)
				{
					ForgeDirection connDir = res.component.dir;
					ForgeDirection faceHit = ForgeDirection.getOrientation(res.movingObjectPosition.sideHit);
					if (all != null && this.containsExternalConnection(connDir))
						for (RaytraceResult rtr : all)
						{
							if (rtr != null && rtr.component != null && COLOR_CONTROLLER_ID.equals(rtr.component.data))
							{
								this.setSignalColor(connDir, DyeColor.getNext(this.getSignalColor(connDir)));
								return true;
							}
						}
					if (connDir == ForgeDirection.UNKNOWN || connDir == faceHit)
					{
						if (this.getConnectionMode(faceHit) == ConnectionMode.DISABLED)
						{
							this.setConnectionMode(faceHit, ConnectionMode.IN_OUT);
							return true;
						}
						return ConduitUtil.joinConduits(this, faceHit);
					}
					if (this.externalConnections.contains(connDir))
					{
						this.setConnectionMode(connDir, this.getNextConnectionMode(connDir));
						return true;
					}
					if (this.containsConduitConnection(connDir))
					{
						ConduitUtil.disconectConduits(this, connDir);
						this.addMissingNodeConnections();
						return true;
					}
				}
		return false;
	}

	@Override
	public void setConnectionMode(ForgeDirection dir, ConnectionMode mode)
	{
		if (mode == ConnectionMode.DISABLED)
			this.disconnectNode(dir);
		super.setConnectionMode(dir, mode);
	}

	@Override
	public void connectionsChanged()
	{
		super.connectionsChanged();
		this.addMissingNodeConnections();
	}

	private void addMissingNodeConnections()
	{
		BlockCoord loc = this.getLocation();
		if (loc != null && this.network != null)
		{
			World world = this.getBundle().getWorld();
			EnumSet<ForgeDirection> conns = this.getConnections();
			for (DyeColor color : DyeColor.values())
			{
				Set<Node> should = new HashSet<Node>();
				for (ForgeDirection direction : conns)
				{
					if (this.getSignalColor(direction) == color)
					{
						// TODO gamerforEA use BlockCoord.getSafeTileEntity
						TileEntity te = this.getLocation().getLocation(direction).getSafeTileEntity(world);
						Node other = null;
						if (te instanceof SidedEnvironment)
							other = ((SidedEnvironment) te).sidedNode(direction.getOpposite());
						else if (te instanceof Environment)
							other = ((Environment) te).node();
						else
						{
							// We have a connection to something we cannot connect to. Should
							// not happen. Poke debugger in >here< if it does!
						}
						if (other != null && other != this.node(color))
							should.add(other);
					}
				}
				for (Node other : should)
				{
					if (!this.node(color).isNeighborOf(other))
						this.node(color).connect(other);
				}
			}
		}
	}

	private void disconnectNode(ForgeDirection direction)
	{
		World world = this.getBundle().getWorld();
		// TODO gamerforEA use BlockCoord.getSafeTileEntity
		TileEntity te = this.getLocation().getLocation(direction).getSafeTileEntity(world);
		Node other = null;
		if (te instanceof SidedEnvironment)
			other = ((SidedEnvironment) te).sidedNode(direction.getOpposite());
		else if (te instanceof Environment)
			other = ((Environment) te).node();
		if (other != null)
			this.disconnectNode(other, this.getSignalColor(direction));
	}

	/**
	 * This will disconnect a node from our network unless it has another
	 * connection to our network. This only works if all the node's blocks are
	 * adjacent to us. Connecting 2 ManagedEnvironments at different locations
	 * won't work well.
	 *
	 * @param other The node to disconnect from us
	 */
	private void disconnectNode(Node other, DyeColor color)
	{

		// Um. No.
		if (other == this.node(color))
			return;
		// Two conduit networks never connect to each other. They join instead.
		Environment otherHost = other.host();
		if (otherHost instanceof OCConduitNetwork && otherHost != this.network)
		{
			this.node(color).disconnect(other);
			return;
		}

		World world = this.getBundle().getWorld();
		EnumSet<ForgeDirection> conns = this.getConnections();
		// we need to check if that node has another way of connecting to our
		// network. First find out which of our neighbor(s) it belongs to. May
		// be just one, may be many.
		List<TileEntity> toCheck = new ArrayList<TileEntity>();
		if (otherHost instanceof TileEntity)
		{
			TileEntity otherTe = (TileEntity) otherHost;
			toCheck.add(otherTe);
		}
		else
			for (ForgeDirection direction : conns)
			{
				if (this.getSignalColor(direction) == color)
				{
					// TODO gamerforEA use BlockCoord.getSafeTileEntity
					TileEntity te = this.getLocation().getLocation(direction).getSafeTileEntity(world);
					Node other2 = null;
					if (te instanceof SidedEnvironment)
						other2 = ((SidedEnvironment) te).sidedNode(direction.getOpposite());
					else if (te instanceof Environment)
						other2 = ((Environment) te).node();
					if (other2 == other)
						toCheck.add(te);
				}
			}
		// Then see if it still has a connection to our node other than through us.
		boolean stayConnected = false;
		for (TileEntity otherTe : toCheck)
		{
			for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS)
			{
				if (!stayConnected)
				{
					boolean checkThisSide = true;
					if (otherHost instanceof SidedEnvironment)
						checkThisSide = ((SidedEnvironment) otherHost).sidedNode(direction) != null;
					if (checkThisSide)
					{
						BlockCoord otherPos = new BlockCoord(otherTe);
						BlockCoord connTo = otherPos.getLocation(direction);
						if (!connTo.equals(this.getLocation()))
						{
							// TODO gamerforEA use BlockCoord.getSafeTileEntity
							TileEntity connToTe = connTo.getSafeTileEntity(world);
							if (connToTe instanceof SidedEnvironment)
								stayConnected = ((SidedEnvironment) connToTe).sidedNode(direction.getOpposite()) == this.node(color);
							else if (connToTe instanceof Environment)
								stayConnected = ((Environment) connToTe).node() == this.node(color);
						}
					}
				}
			}
		}
		if (!stayConnected)
			this.node(color).disconnect(other);
	}

	public EnumSet<ForgeDirection> getConnections()
	{
		EnumSet<ForgeDirection> cons = EnumSet.noneOf(ForgeDirection.class);
		cons.addAll(this.getConduitConnections());
		for (ForgeDirection dir : this.getExternalConnections())
		{
			if (this.getConnectionMode(dir) != ConnectionMode.DISABLED)
				cons.add(dir);
		}
		return cons;
	}

	@Override
	public ConnectionMode getNextConnectionMode(ForgeDirection dir)
	{
		ConnectionMode mode = this.getConnectionMode(dir);
		mode = mode == ConnectionMode.IN_OUT ? ConnectionMode.DISABLED : ConnectionMode.IN_OUT;
		return mode;
	}

	@Override
	public ConnectionMode getPreviousConnectionMode(ForgeDirection dir)
	{
		return this.getNextConnectionMode(dir);
	}

	@Override
	public boolean canConnectToExternal(ForgeDirection direction, boolean ignoreConnectionMode)
	{
		// TODO gamerforEA use BlockCoord.getSafeTileEntity
		TileEntity te = this.getLocation().getLocation(direction).getSafeTileEntity(this.getBundle().getWorld());
		if (te instanceof SidedEnvironment)
		{
			if (this.getBundle().getWorld().isRemote)
				return ((SidedEnvironment) te).canConnect(direction.getOpposite());
			return ((SidedEnvironment) te).sidedNode(direction.getOpposite()) != null;
		}
		return te instanceof Environment;
	}

	@Override
	@Method(modid = "OpenComputersAPI|Network")
	public Node node()
	{
		return this.network != null ? this.network.node(DyeColor.SILVER) : null;
	}

	@Method(modid = "OpenComputersAPI|Network")
	public Node node(DyeColor subnet)
	{
		return this.network != null ? this.network.node(subnet) : null;
	}

	@Override
	@Method(modid = "OpenComputersAPI|Network")
	public void onConnect(Node node)
	{
	}

	@Override
	@Method(modid = "OpenComputersAPI|Network")
	public void onDisconnect(Node node)
	{
	}

	@Override
	@Method(modid = "OpenComputersAPI|Network")
	public void onMessage(Message message)
	{
	}

	@Override
	@Method(modid = "OpenComputersAPI|Network")
	public Node sidedNode(ForgeDirection side)
	{
		return this.getConnections().contains(side) ? this.node(this.getSignalColor(side)) : null;
	}

	@Override
	@SideOnly(Side.CLIENT)
	@Method(modid = "OpenComputersAPI|Network")
	public boolean canConnect(ForgeDirection side)
	{
		return this.getConnections().contains(side);
	}

	@Override
	public void invalidate()
	{
	}

}
