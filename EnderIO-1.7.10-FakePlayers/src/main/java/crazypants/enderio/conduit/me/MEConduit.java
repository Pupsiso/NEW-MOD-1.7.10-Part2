package crazypants.enderio.conduit.me;

import appeng.api.AEApi;
import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import com.enderio.core.client.render.IconUtil;
import com.enderio.core.common.util.BlockCoord;
import cpw.mods.fml.common.Optional.Method;
import crazypants.enderio.EnderIO;
import crazypants.enderio.conduit.*;
import crazypants.enderio.conduit.geom.CollidableComponent;
import crazypants.enderio.tool.ToolUtil;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.EnumSet;
import java.util.List;

public class MEConduit extends AbstractConduit implements IMEConduit
{

	protected MEConduitNetwork network;
	protected MEConduitGrid grid;

	public static IIcon[] coreTextures;
	public static IIcon[] longTextures;

	private boolean isDense;
	private int playerID = -1;

	public MEConduit()
	{
		this(0);
	}

	public MEConduit(int itemDamage)
	{
		this.isDense = itemDamage == 1;
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

				coreTextures[0] = register.registerIcon(EnderIO.DOMAIN + ":meConduitCore");
				coreTextures[1] = register.registerIcon(EnderIO.DOMAIN + ":meConduitCoreDense");

				longTextures[0] = register.registerIcon(EnderIO.DOMAIN + ":meConduit");
				longTextures[1] = register.registerIcon(EnderIO.DOMAIN + ":meConduitDense");
			}

			@Override
			public int getTextureType()
			{
				return 0;
			}

		});
	}

	public static int getDamageForState(boolean isDense)
	{
		return isDense ? 1 : 0;
	}

	@Override
	public Class<? extends IConduit> getBaseConduitType()
	{
		return IMEConduit.class;
	}

	@Override
	public ItemStack createItem()
	{
		return new ItemStack(EnderIO.itemMEConduit, 1, getDamageForState(this.isDense));
	}

	@Override
	public AbstractConduitNetwork<?, ?> getNetwork()
	{
		return this.network;
	}

	@Override
	public boolean setNetwork(AbstractConduitNetwork<?, ?> network)
	{
		this.network = (MEConduitNetwork) network;
		return true;
	}

	@Override
	public void writeToNBT(NBTTagCompound nbtRoot)
	{
		super.writeToNBT(nbtRoot);
		nbtRoot.setBoolean("isDense", this.isDense);
		nbtRoot.setInteger("playerID", this.playerID);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbtRoot, short nbtVersion)
	{
		super.readFromNBT(nbtRoot, nbtVersion);
		this.isDense = nbtRoot.getBoolean("isDense");
		if (nbtRoot.hasKey("playerID"))
			this.playerID = nbtRoot.getInteger("playerID");
		else
			this.playerID = -1;
	}

	public void setPlayerID(int playerID)
	{
		this.playerID = playerID;
	}

	@Override
	public int getChannelsInUse()
	{
		int channelsInUse = 0;
		IGridNode node = this.getNode();
		if (node != null)
			for (IGridConnection gc : node.getConnections())
			{
				channelsInUse = Math.max(channelsInUse, gc.getUsedChannels());
			}
		return channelsInUse;
	}

	@Override
	@Method(modid = "appliedenergistics2")
	public boolean canConnectToExternal(ForgeDirection dir, boolean ignoreDisabled)
	{
		World world = this.getBundle().getWorld();
		BlockCoord pos = this.getLocation();

		// TODO gamerforEA replace, old code:
		// TileEntity te = world.getTileEntity(pos.x + dir.offsetX, pos.y + dir.offsetY, pos.z + dir.offsetZ);
		TileEntity te = pos.getLocation(dir).getSafeTileEntity(world);
		// TODO gamerforEA code end

		if (te instanceof TileConduitBundle)
			return false;

		// because the AE2 API doesn't allow an easy query like "which side can connect to an ME cable" it needs this mess
		if (te instanceof IPartHost)
		{
			IPart part = ((IPartHost) te).getPart(dir.getOpposite());
			if (part == null)
			{
				part = ((IPartHost) te).getPart(ForgeDirection.UNKNOWN);
				return part != null;
			}
			if (part.getExternalFacingNode() != null)
				return true;
			String name = part.getClass().getSimpleName();
			return "PartP2PTunnelME".equals(name) || "PartQuartzFiber".endsWith(name) || "PartToggleBus".equals(name) || "PartInvertedToggleBus".equals(name);
		}
		if (te instanceof IGridHost)
		{
			IGridNode node = ((IGridHost) te).getGridNode(dir.getOpposite());
			if (node == null)
				node = ((IGridHost) te).getGridNode(ForgeDirection.UNKNOWN);
			if (node != null)
				return node.getGridBlock().getConnectableSides().contains(dir.getOpposite());
		}
		return false;
	}

	@Override
	public IIcon getTextureForState(CollidableComponent component)
	{
		int state = getDamageForState(this.isDense);
		if (component.dir == ForgeDirection.UNKNOWN)
			return coreTextures[state];
		return longTextures[state];
	}

	@Override
	public IIcon getTransmitionTextureForState(CollidableComponent component)
	{
		return null;
	}

	@Override
	@Method(modid = "appliedenergistics2")
	public void updateEntity(World worldObj)
	{
		if (this.grid == null)
			this.grid = new MEConduitGrid(this);

		if (this.getNode() == null && !worldObj.isRemote)
		{
			IGridNode node = AEApi.instance().createGridNode(this.grid);
			if (node != null)
			{
				node.setPlayerID(this.playerID);
				this.getBundle().setGridNode(node);
				this.getNode().updateState();
			}
		}

		super.updateEntity(worldObj);
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
	public boolean canConnectToConduit(ForgeDirection direction, IConduit conduit)
	{
		if (!super.canConnectToConduit(direction, conduit))
			return false;
		return conduit instanceof IMEConduit;
	}

	@Override
	@Method(modid = "appliedenergistics2")
	public void connectionsChanged()
	{
		super.connectionsChanged();
		BlockCoord loc = this.getLocation();
		if (loc != null)
		{
			this.onNodeChanged(loc);
			IGridNode node = this.getNode();
			if (node != null)
			{
				node.updateState();
				node.getWorld().markBlockForUpdate(loc.x, loc.y, loc.z);
			}
		}
	}

	@Override
	public boolean onBlockActivated(EntityPlayer player, RaytraceResult res, List<RaytraceResult> all)
	{
		if (ToolUtil.isToolEquipped(player))
			if (!this.getBundle().getEntity().getWorldObj().isRemote)
				if (res != null && res.component != null)
				{
					ForgeDirection connDir = res.component.dir;
					ForgeDirection faceHit = ForgeDirection.getOrientation(res.movingObjectPosition.sideHit);
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
						return true;
					}
				}
		return false;
	}

	@Method(modid = "appliedenergistics2")
	private void onNodeChanged(BlockCoord location)
	{
		World world = this.getBundle().getWorld();
		for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS)
		{
			// TODO gamerforEA use BlockCoord.getSafeTileEntity
			TileEntity te = location.getLocation(dir).getSafeTileEntity(world);
			if (te instanceof IGridHost && !(te instanceof IConduitBundle))
			{
				IGridNode node = ((IGridHost) te).getGridNode(ForgeDirection.UNKNOWN);
				if (node == null)
					node = ((IGridHost) te).getGridNode(dir.getOpposite());
				if (node != null)
					node.updateState();
			}
		}
	}

	@Override
	public void onAddedToBundle()
	{
		for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS)
		{
			// TODO gamerforEA use BlockCoord.getSafeTileEntity
			TileEntity te = this.getLocation().getLocation(dir).getSafeTileEntity(this.getBundle().getWorld());
			if (te instanceof TileConduitBundle)
			{
				IMEConduit cond = ((TileConduitBundle) te).getConduit(IMEConduit.class);
				if (cond != null)
				{
					cond.setConnectionMode(dir.getOpposite(), ConnectionMode.IN_OUT);
					ConduitUtil.joinConduits(cond, dir.getOpposite());
				}
			}
		}
	}

	@Override
	@Method(modid = "appliedenergistics2")
	public void onRemovedFromBundle()
	{
		super.onRemovedFromBundle();
		this.getNode().destroy();
		this.getBundle().setGridNode(null);
	}

	@Override
	@Method(modid = "appliedenergistics2")
	public void onChunkUnload(World worldObj)
	{
		super.onChunkUnload(worldObj);
		if (this.getNode() != null)
		{
			this.getNode().destroy();
			this.getBundle().setGridNode(null);
		}
	}

	@Override
	public MEConduitGrid getGrid()
	{
		return this.grid;
	}

	@Method(modid = "appliedenergistics2")
	private IGridNode getNode()
	{
		return this.getBundle().getGridNode(null);
	}

	@Override
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
	public boolean isDense()
	{
		return this.isDense;
	}
}
