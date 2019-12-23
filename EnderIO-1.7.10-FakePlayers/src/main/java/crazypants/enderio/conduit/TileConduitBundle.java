package crazypants.enderio.conduit;

import appeng.api.networking.IGridNode;
import appeng.api.util.AECableType;
import com.enderio.core.client.render.BoundingBox;
import cpw.mods.fml.common.Optional.Method;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import crazypants.enderio.EnderIO;
import crazypants.enderio.TileEntityEio;
import crazypants.enderio.conduit.facade.ItemConduitFacade.FacadeType;
import crazypants.enderio.conduit.gas.IGasConduit;
import crazypants.enderio.conduit.geom.*;
import crazypants.enderio.conduit.item.IItemConduit;
import crazypants.enderio.conduit.liquid.ILiquidConduit;
import crazypants.enderio.conduit.me.IMEConduit;
import crazypants.enderio.conduit.oc.IOCConduit;
import crazypants.enderio.conduit.power.IPowerConduit;
import crazypants.enderio.conduit.redstone.InsulatedRedstoneConduit;
import crazypants.enderio.config.Config;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasStack;
import mods.immibis.microblocks.api.*;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;

import java.util.*;

public class TileConduitBundle extends TileEntityEio implements IConduitBundle
{

	public static final short NBT_VERSION = 1;

	private final List<IConduit> conduits = new ArrayList<IConduit>();

	private Block facadeId = null;
	private int facadeMeta = 0;
	private FacadeType facadeType = FacadeType.BASIC;

	private boolean facadeChanged;

	private final List<CollidableComponent> cachedCollidables = new ArrayList<CollidableComponent>();

	private final List<CollidableComponent> cachedConnectors = new ArrayList<CollidableComponent>();

	private boolean conduitsDirty = true;
	private boolean collidablesDirty = true;
	private boolean connectorsDirty = true;

	private boolean clientUpdated = false;

	private int lightOpacity = -1;

	@SideOnly(Side.CLIENT)
	private FacadeRenderState facadeRenderAs;

	private ConduitDisplayMode lastMode = ConduitDisplayMode.ALL;

	Object covers;

	public TileConduitBundle()
	{
		this.blockType = EnderIO.blockConduitBundle;
		this.initMicroblocks();
	}

	@Override
	public void dirty()
	{
		this.conduitsDirty = true;
		this.collidablesDirty = true;
	}

	@Override
	public boolean shouldRenderInPass(int arg0)
	{
		if (this.facadeId != null && this.facadeId.isOpaqueCube() && !ConduitUtil.isFacadeHidden(this, EnderIO.proxy.getClientPlayer()))
			return false;
		return super.shouldRenderInPass(arg0);
	}

	@Override
	public void writeCustomNBT(NBTTagCompound nbtRoot)
	{
		NBTTagList conduitTags = new NBTTagList();
		for (IConduit conduit : this.conduits)
		{
			NBTTagCompound conduitRoot = new NBTTagCompound();
			ConduitUtil.writeToNBT(conduit, conduitRoot);
			conduitTags.appendTag(conduitRoot);
		}
		nbtRoot.setTag("conduits", conduitTags);
		if (this.facadeId != null)
		{
			nbtRoot.setString("facadeId", Block.blockRegistry.getNameForObject(this.facadeId));
			nbtRoot.setString("facadeType", this.facadeType.name());
		}
		else
			nbtRoot.setString("facadeId", "null");
		nbtRoot.setInteger("facadeMeta", this.facadeMeta);
		nbtRoot.setShort("nbtVersion", NBT_VERSION);

		if (MicroblocksUtil.supportMicroblocks())
			this.writeMicroblocksToNBT(nbtRoot);
	}

	@Override
	public void readCustomNBT(NBTTagCompound nbtRoot)
	{
		short nbtVersion = nbtRoot.getShort("nbtVersion");

		this.conduits.clear();
		this.cachedCollidables.clear();
		NBTTagList conduitTags = (NBTTagList) nbtRoot.getTag("conduits");
		if (conduitTags != null)
			for (int i = 0; i < conduitTags.tagCount(); i++)
			{
				NBTTagCompound conduitTag = conduitTags.getCompoundTagAt(i);
				IConduit conduit = ConduitUtil.readConduitFromNBT(conduitTag, nbtVersion);
				if (conduit != null)
				{
					conduit.setBundle(this);
					this.conduits.add(conduit);
				}
			}
		String fs = nbtRoot.getString("facadeId");
		if (fs == null || "null".equals(fs))
		{
			this.facadeId = null;
			this.facadeType = FacadeType.BASIC;
		}
		else
		{
			this.facadeId = Block.getBlockFromName(fs);
			// backwards compat, never true in freshly placed bundles
			if (nbtRoot.hasKey("facadeType"))
				this.facadeType = FacadeType.valueOf(nbtRoot.getString("facadeType"));
		}
		this.facadeMeta = nbtRoot.getInteger("facadeMeta");

		if (this.worldObj != null && this.worldObj.isRemote)
			this.clientUpdated = true;

		if (MicroblocksUtil.supportMicroblocks())
			this.readMicroblocksFromNBT(nbtRoot);
	}

	@Override
	public boolean hasFacade()
	{
		return this.facadeId != null;
	}

	@Override
	public void setFacadeId(Block blockID, boolean triggerUpdate)
	{
		this.facadeId = blockID;
		if (triggerUpdate)
			this.facadeChanged = true;
	}

	@Override
	public void setFacadeId(Block blockID)
	{
		this.setFacadeId(blockID, true);
	}

	@Override
	public Block getFacadeId()
	{
		return this.facadeId;
	}

	@Override
	public void setFacadeMetadata(int meta)
	{
		this.facadeMeta = meta;
	}

	@Override
	public void setFacadeType(FacadeType type)
	{
		this.facadeType = type;
	}

	@Override
	public int getFacadeMetadata()
	{
		return this.facadeMeta;
	}

	@Override
	public FacadeType getFacadeType()
	{
		return this.facadeType;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public FacadeRenderState getFacadeRenderedAs()
	{
		if (this.facadeRenderAs == null)
			this.facadeRenderAs = FacadeRenderState.NONE;
		return this.facadeRenderAs;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void setFacadeRenderAs(FacadeRenderState state)
	{
		this.facadeRenderAs = state;
	}

	@Override
	public int getLightOpacity()
	{
		if (this.worldObj != null && !this.worldObj.isRemote || this.lightOpacity == -1)
			return this.hasFacade() ? this.facadeId.getLightOpacity() : 0;
		return this.lightOpacity;
	}

	@Override
	public void setLightOpacity(int opacity)
	{
		this.lightOpacity = opacity;
	}

	@Override
	public void onChunkUnload()
	{
		for (IConduit conduit : this.conduits)
		{
			conduit.onChunkUnload(this.worldObj);
		}
	}

	@Override
	public void doUpdate()
	{
		for (IConduit conduit : this.conduits)
		{
			conduit.updateEntity(this.worldObj);
		}

		if (this.conduitsDirty)
			this.doConduitsDirty();

		if (this.facadeChanged)
			this.doFacadeChanged();

		//client side only, check for changes in rendering of the bundle
		if (this.worldObj.isRemote)
			this.updateEntityClient();
	}

	private void doConduitsDirty()
	{
		if (!this.worldObj.isRemote)
		{
			this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
			this.markDirty();
		}
		this.conduitsDirty = false;
	}

	private void doFacadeChanged()
	{
		//force re-calc of lighting for both client and server
		ConduitUtil.forceSkylightRecalculation(this.worldObj, this.xCoord, this.yCoord, this.zCoord);
		//worldObj.updateAllLightTypes(xCoord, yCoord, zCoord);
		this.worldObj.func_147451_t(this.xCoord, this.yCoord, this.zCoord);
		this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
		this.worldObj.notifyBlocksOfNeighborChange(this.xCoord, this.yCoord, this.zCoord, EnderIO.blockConduitBundle);
		this.facadeChanged = false;
	}

	private void updateEntityClient()
	{
		boolean markForUpdate = false;
		if (this.clientUpdated)
		{
			//TODO: This is not the correct solution here but just marking the block for a render update server side
			//seems to get out of sync with the client sometimes so connections are not rendered correctly
			markForUpdate = true;
			this.clientUpdated = false;
		}

		FacadeRenderState curRS = this.getFacadeRenderedAs();
		FacadeRenderState rs = ConduitUtil.getRequiredFacadeRenderState(this, EnderIO.proxy.getClientPlayer());

		if (Config.updateLightingWhenHidingFacades)
		{
			int curLO = this.getLightOpacity();
			int shouldBeLO = rs == FacadeRenderState.FULL ? 255 : 0;
			if (curLO != shouldBeLO)
			{
				this.setLightOpacity(shouldBeLO);
				//worldObj.updateAllLightTypes(xCoord, yCoord, zCoord);
				this.worldObj.func_147451_t(this.xCoord, this.yCoord, this.zCoord);
			}
		}

		if (curRS != rs)
		{
			this.setFacadeRenderAs(rs);
			if (!ConduitUtil.forceSkylightRecalculation(this.worldObj, this.xCoord, this.yCoord, this.zCoord))
				markForUpdate = true;
		}
		else
		{ //can do the else as only need to update once
			ConduitDisplayMode curMode = ConduitDisplayMode.getDisplayMode(EnderIO.proxy.getClientPlayer().getCurrentEquippedItem());
			if (curMode != this.lastMode)
			{
				markForUpdate = true;
				this.lastMode = curMode;
			}

		}
		if (markForUpdate)
			this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
	}

	@Override
	public void onNeighborBlockChange(Block blockId)
	{
		boolean needsUpdate = false;
		for (IConduit conduit : this.conduits)
		{
			needsUpdate |= conduit.onNeighborBlockChange(blockId);
		}
		if (needsUpdate)
			this.dirty();
	}

	@Override
	public void onNeighborChange(IBlockAccess world, int x, int y, int z, int tileX, int tileY, int tileZ)
	{
		boolean needsUpdate = false;
		for (IConduit conduit : this.conduits)
		{
			needsUpdate |= conduit.onNeighborChange(world, x, y, z, tileX, tileY, tileZ);
		}
		if (needsUpdate)
			this.dirty();
	}

	@Override
	public TileConduitBundle getEntity()
	{
		return this;
	}

	@Override
	public boolean hasType(Class<? extends IConduit> type)
	{
		return this.getConduit(type) != null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends IConduit> T getConduit(Class<T> type)
	{
		if (type == null)
			return null;
		for (IConduit conduit : this.conduits)
		{
			if (type.isInstance(conduit))
				return (T) conduit;
		}
		return null;
	}

	@Override
	public void addConduit(IConduit conduit)
	{
		if (this.worldObj.isRemote)
			return;
		this.conduits.add(conduit);
		conduit.setBundle(this);
		conduit.onAddedToBundle();
		this.dirty();
	}

	@Override
	public void removeConduit(IConduit conduit)
	{
		if (conduit != null)
			this.removeConduit(conduit, true);
	}

	public void removeConduit(IConduit conduit, boolean notify)
	{
		if (this.worldObj.isRemote)
			return;
		conduit.onRemovedFromBundle();
		this.conduits.remove(conduit);
		conduit.setBundle(null);
		if (notify)
			this.dirty();
	}

	@Override
	public void onBlockRemoved()
	{
		if (this.worldObj.isRemote)
			return;
		List<IConduit> copy = new ArrayList<IConduit>(this.conduits);
		for (IConduit con : copy)
		{
			this.removeConduit(con, false);
		}
		this.dirty();
	}

	@Override
	public Collection<IConduit> getConduits()
	{
		return this.conduits;
	}

	@Override
	public Set<ForgeDirection> getConnections(Class<? extends IConduit> type)
	{
		IConduit con = this.getConduit(type);
		if (con != null)
			return con.getConduitConnections();
		return null;
	}

	@Override
	public boolean containsConnection(Class<? extends IConduit> type, ForgeDirection dir)
	{
		IConduit con = this.getConduit(type);
		if (con != null)
			return con.containsConduitConnection(dir);
		return false;
	}

	@Override
	public boolean containsConnection(ForgeDirection dir)
	{
		for (IConduit con : this.conduits)
		{
			if (con.containsConduitConnection(dir))
				return true;
		}
		return false;
	}

	@Override
	public Set<ForgeDirection> getAllConnections()
	{
		EnumSet<ForgeDirection> result = EnumSet.noneOf(ForgeDirection.class);
		for (IConduit con : this.conduits)
		{
			result.addAll(con.getConduitConnections());
		}
		return result;
	}

	// Geometry

	@Override
	public Offset getOffset(Class<? extends IConduit> type, ForgeDirection dir)
	{
		if (this.getConnectionCount(dir) < 2)
			return Offset.NONE;
		return Offsets.get(type, dir);
	}

	@Override
	public List<CollidableComponent> getCollidableComponents()
	{

		for (IConduit con : this.conduits)
		{
			this.collidablesDirty = this.collidablesDirty || con.haveCollidablesChangedSinceLastCall();
		}
		if (this.collidablesDirty)
			this.connectorsDirty = true;
		if (!this.collidablesDirty && !this.cachedCollidables.isEmpty())
			return this.cachedCollidables;
		this.cachedCollidables.clear();
		for (IConduit conduit : this.conduits)
		{
			this.cachedCollidables.addAll(conduit.getCollidableComponents());
		}

		this.addConnectors(this.cachedCollidables);

		this.collidablesDirty = false;

		return this.cachedCollidables;
	}

	@Override
	public List<CollidableComponent> getConnectors()
	{
		List<CollidableComponent> result = new ArrayList<CollidableComponent>();
		this.addConnectors(result);
		return result;
	}

	private void addConnectors(List<CollidableComponent> result)
	{

		if (this.conduits.isEmpty())
			return;

		for (IConduit con : this.conduits)
		{
			boolean b = con.haveCollidablesChangedSinceLastCall();
			this.collidablesDirty = this.collidablesDirty || b;
			this.connectorsDirty = this.connectorsDirty || b;
		}

		if (!this.connectorsDirty && !this.cachedConnectors.isEmpty())
		{
			result.addAll(this.cachedConnectors);
			return;
		}

		this.cachedConnectors.clear();

		// TODO: What an unholly mess! (and it doesn't even work correctly...)
		List<CollidableComponent> coreBounds = new ArrayList<CollidableComponent>();
		for (IConduit con : this.conduits)
		{
			this.addConduitCores(coreBounds, con);
		}
		this.cachedConnectors.addAll(coreBounds);
		result.addAll(coreBounds);

		// 1st algorithm
		List<CollidableComponent> conduitsBounds = new ArrayList<CollidableComponent>();
		for (IConduit con : this.conduits)
		{
			conduitsBounds.addAll(con.getCollidableComponents());
			this.addConduitCores(conduitsBounds, con);
		}

		Set<Class<IConduit>> collidingTypes = new HashSet<Class<IConduit>>();
		for (CollidableComponent conCC : conduitsBounds)
		{
			for (CollidableComponent innerCC : conduitsBounds)
			{
				if (!InsulatedRedstoneConduit.COLOR_CONTROLLER_ID.equals(innerCC.data) && !InsulatedRedstoneConduit.COLOR_CONTROLLER_ID.equals(conCC.data) && conCC != innerCC && conCC.bound.intersects(innerCC.bound))
					collidingTypes.add((Class<IConduit>) conCC.conduitType);
			}
		}

		//TODO: Remove the core geometries covered up by this as no point in rendering these
		if (!collidingTypes.isEmpty())
		{
			List<CollidableComponent> colCores = new ArrayList<CollidableComponent>();
			for (Class<IConduit> c : collidingTypes)
			{
				IConduit con = this.getConduit(c);
				if (con != null)
					this.addConduitCores(colCores, con);
			}

			BoundingBox bb = null;
			for (CollidableComponent cBB : colCores)
			{
				if (bb == null)
					bb = cBB.bound;
				else
					bb = bb.expandBy(cBB.bound);
			}
			if (bb != null)
			{
				bb = bb.scale(1.05, 1.05, 1.05);
				CollidableComponent cc = new CollidableComponent(null, bb, ForgeDirection.UNKNOWN, ConduitConnectorType.INTERNAL);
				result.add(cc);
				this.cachedConnectors.add(cc);
			}
		}

		//2nd algorithm
		for (IConduit con : this.conduits)
		{
			if (con.hasConnections())
			{
				List<CollidableComponent> cores = new ArrayList<CollidableComponent>();
				this.addConduitCores(cores, con);
				if (cores.size() > 1)
				{
					BoundingBox bb = cores.get(0).bound;
					float area = bb.getArea();
					for (CollidableComponent cc : cores)
					{
						bb = bb.expandBy(cc.bound);
					}
					if (bb.getArea() > area * 1.5f)
					{
						bb = bb.scale(1.05, 1.05, 1.05);
						CollidableComponent cc = new CollidableComponent(null, bb, ForgeDirection.UNKNOWN, ConduitConnectorType.INTERNAL);
						result.add(cc);
						this.cachedConnectors.add(cc);
					}
				}
			}
		}

		// Merge all internal conduit connectors into one box
		BoundingBox conBB = null;
		for (int i = 0; i < result.size(); i++)
		{
			CollidableComponent cc = result.get(i);
			if (cc.conduitType == null && cc.data == ConduitConnectorType.INTERNAL)
			{
				conBB = conBB == null ? cc.bound : conBB.expandBy(cc.bound);
				result.remove(i);
				i--;
				this.cachedConnectors.remove(cc);
			}
		}

		if (conBB != null)
		{
			CollidableComponent cc = new CollidableComponent(null, conBB, ForgeDirection.UNKNOWN, ConduitConnectorType.INTERNAL);
			result.add(cc);
			this.cachedConnectors.add(cc);
		}

		// External Connectors
		EnumSet<ForgeDirection> externalDirs = EnumSet.noneOf(ForgeDirection.class);
		for (IConduit con : this.conduits)
		{
			Set<ForgeDirection> extCons = con.getExternalConnections();
			if (extCons != null)
				for (ForgeDirection dir : extCons)
				{
					if (con.getConnectionMode(dir) != ConnectionMode.DISABLED)
						externalDirs.add(dir);
				}
		}
		for (ForgeDirection dir : externalDirs)
		{
			BoundingBox bb = ConduitGeometryUtil.instance.getExternalConnectorBoundingBox(dir);
			CollidableComponent cc = new CollidableComponent(null, bb, dir, ConduitConnectorType.EXTERNAL);
			result.add(cc);
			this.cachedConnectors.add(cc);
		}

		this.connectorsDirty = false;
	}

	private void addConduitCores(List<CollidableComponent> result, IConduit con)
	{
		CollidableCache cc = CollidableCache.instance;
		Class<? extends IConduit> type = con.getCollidableType();
		if (con.hasConnections())
		{
			for (ForgeDirection dir : con.getExternalConnections())
			{
				result.addAll(cc.getCollidables(cc.createKey(type, this.getOffset(con.getBaseConduitType(), dir), ForgeDirection.UNKNOWN, false), con));
			}
			for (ForgeDirection dir : con.getConduitConnections())
			{
				result.addAll(cc.getCollidables(cc.createKey(type, this.getOffset(con.getBaseConduitType(), dir), ForgeDirection.UNKNOWN, false), con));
			}
		}
		else
			result.addAll(cc.getCollidables(cc.createKey(type, this.getOffset(con.getBaseConduitType(), ForgeDirection.UNKNOWN), ForgeDirection.UNKNOWN, false), con));
	}

	private int getConnectionCount(ForgeDirection dir)
	{
		if (dir == ForgeDirection.UNKNOWN)
			return this.conduits.size();
		int result = 0;
		for (IConduit con : this.conduits)
		{
			if (con.containsConduitConnection(dir) || con.containsExternalConnection(dir))
				result++;
		}
		return result;
	}

	// ------------ Power -----------------------------

	@Override
	public int receiveEnergy(ForgeDirection from, int maxReceive, boolean simulate)
	{
		IPowerConduit pc = this.getConduit(IPowerConduit.class);
		if (pc != null)
			return pc.receiveEnergy(from, maxReceive, simulate);
		return 0;
	}

	@Override
	public int extractEnergy(ForgeDirection from, int maxExtract, boolean simulate)
	{
		IPowerConduit pc = this.getConduit(IPowerConduit.class);
		if (pc != null)
			return pc.extractEnergy(from, maxExtract, simulate);
		return 0;
	}

	@Override
	public boolean canConnectEnergy(ForgeDirection from)
	{
		IPowerConduit pc = this.getConduit(IPowerConduit.class);
		if (pc != null)
			return pc.canConnectEnergy(from);
		return false;
	}

	@Override
	public int getEnergyStored(ForgeDirection from)
	{
		IPowerConduit pc = this.getConduit(IPowerConduit.class);
		if (pc != null)
			return pc.getEnergyStored(from);
		return 0;
	}

	@Override
	public int getMaxEnergyStored(ForgeDirection from)
	{
		IPowerConduit pc = this.getConduit(IPowerConduit.class);
		if (pc != null)
			return pc.getMaxEnergyStored(from);
		return 0;
	}

	@Override
	public int getMaxEnergyRecieved(ForgeDirection dir)
	{
		IPowerConduit pc = this.getConduit(IPowerConduit.class);
		if (pc != null)
			return pc.getMaxEnergyRecieved(dir);
		return 0;
	}

	@Override
	public int getEnergyStored()
	{
		IPowerConduit pc = this.getConduit(IPowerConduit.class);
		if (pc != null)
			return pc.getEnergyStored();
		return 0;
	}

	@Override
	public int getMaxEnergyStored()
	{
		IPowerConduit pc = this.getConduit(IPowerConduit.class);
		if (pc != null)
			return pc.getMaxEnergyStored();
		return 0;
	}

	@Override
	public void setEnergyStored(int stored)
	{
		IPowerConduit pc = this.getConduit(IPowerConduit.class);
		if (pc != null)
			pc.setEnergyStored(stored);

	}

	//------- Liquids -----------------------------

	@Override
	public int fill(ForgeDirection from, FluidStack resource, boolean doFill)
	{
		ILiquidConduit lc = this.getConduit(ILiquidConduit.class);
		if (lc != null)
			return lc.fill(from, resource, doFill);
		return 0;
	}

	@Override
	public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain)
	{
		ILiquidConduit lc = this.getConduit(ILiquidConduit.class);
		if (lc != null)
			return lc.drain(from, resource, doDrain);
		return null;
	}

	@Override
	public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain)
	{
		ILiquidConduit lc = this.getConduit(ILiquidConduit.class);
		if (lc != null)
			return lc.drain(from, maxDrain, doDrain);
		return null;
	}

	@Override
	public boolean canFill(ForgeDirection from, Fluid fluid)
	{
		ILiquidConduit lc = this.getConduit(ILiquidConduit.class);
		if (lc != null)
			return lc.canFill(from, fluid);
		return false;
	}

	@Override
	public boolean canDrain(ForgeDirection from, Fluid fluid)
	{
		ILiquidConduit lc = this.getConduit(ILiquidConduit.class);
		if (lc != null)
			return lc.canDrain(from, fluid);
		return false;
	}

	@Override
	public FluidTankInfo[] getTankInfo(ForgeDirection from)
	{
		ILiquidConduit lc = this.getConduit(ILiquidConduit.class);
		if (lc != null)
			return lc.getTankInfo(from);
		return null;
	}

	// ---- TE Item Conduits

	@Override
	public ItemStack insertItem(ForgeDirection from, ItemStack item)
	{
		IItemConduit ic = this.getConduit(IItemConduit.class);
		if (ic != null)
			return ic.insertItem(from, item);
		return item;
	}

	// ---- Mekanism Gas Tubes

	@Override
	@Method(modid = "MekanismAPI|gas")
	public int receiveGas(ForgeDirection side, GasStack stack)
	{
		return this.receiveGas(side, stack, true);
	}

	@Override
	@Method(modid = "MekanismAPI|gas")
	public int receiveGas(ForgeDirection side, GasStack stack, boolean doTransfer)
	{
		IGasConduit gc = this.getConduit(IGasConduit.class);
		if (gc != null)
			return gc.receiveGas(side, stack, doTransfer);
		return 0;
	}

	@Override
	@Method(modid = "MekanismAPI|gas")
	public GasStack drawGas(ForgeDirection side, int amount)
	{
		return this.drawGas(side, amount, true);
	}

	@Override
	@Method(modid = "MekanismAPI|gas")
	public GasStack drawGas(ForgeDirection side, int amount, boolean doTransfer)
	{
		IGasConduit gc = this.getConduit(IGasConduit.class);
		if (gc != null)
			return gc.drawGas(side, amount, doTransfer);
		return null;
	}

	@Override
	@Method(modid = "MekanismAPI|gas")
	public boolean canReceiveGas(ForgeDirection side, Gas type)
	{
		IGasConduit gc = this.getConduit(IGasConduit.class);
		if (gc != null)
			return gc.canReceiveGas(side, type);
		return false;
	}

	@Override
	@Method(modid = "MekanismAPI|gas")
	public boolean canDrawGas(ForgeDirection side, Gas type)
	{
		IGasConduit gc = this.getConduit(IGasConduit.class);
		if (gc != null)
			return gc.canDrawGas(side, type);
		return false;
	}

	@Override
	public World getWorld()
	{
		return this.getWorldObj();
	}

	private Object node; // IGridNode object, untyped to avoid crash w/o AE2

	@Override
	@Method(modid = "appliedenergistics2")
	public IGridNode getGridNode(ForgeDirection dir)
	{
		if (dir == null || dir == ForgeDirection.UNKNOWN)
			return (IGridNode) this.node;
		IMEConduit cond = this.getConduit(IMEConduit.class);
		if (cond != null)
		{
			if (cond.getConnectionMode(dir.getOpposite()) == ConnectionMode.IN_OUT)
				return (IGridNode) this.node;
			return null;
		}
		return (IGridNode) this.node;
	}

	@SuppressWarnings("cast")
	@Override
	@Method(modid = "appliedenergistics2")
	public void setGridNode(Object node)
	{
		this.node = node;
	}

	@Override
	@Method(modid = "appliedenergistics2")
	public AECableType getCableConnectionType(ForgeDirection dir)
	{
		IMEConduit cond = this.getConduit(IMEConduit.class);
		if (cond == null)
			return AECableType.NONE;
		return cond.isConnectedTo(dir) ? AECableType.SMART : AECableType.NONE;
	}

	@Override
	@Method(modid = "appliedenergistics2")
	public void securityBreak()
	{
	}

	@Override
	public boolean displayPower()
	{
		return true;
	}

	// Immibis Microblocks

	private void initMicroblocks()
	{
		if (MicroblocksUtil.supportMicroblocks())
			this.createCovers();
	}

	@Method(modid = "ImmibisMicroblocks")
	private void createCovers()
	{
		IMicroblockSystem ims = MicroblockAPIUtils.getMicroblockSystem();
		if (ims != null)
			this.covers = ims.createMicroblockCoverSystem(this);
	}

	@Override
	@Method(modid = "ImmibisMicroblocks")
	public boolean isPlacementBlocked(PartType<?> part, EnumPosition pos)
	{
		EnumPartClass type = part.getPartClass();
		// Let's do some cheaper checks first
		if (type == EnumPartClass.Strip)
		{
			// No pillars
			return pos == EnumPosition.PostX || pos == EnumPosition.PostY || pos == EnumPosition.PostZ;
		}
		else // Anything this small can never intersect conduits
			if (part.getSize() < 0.25)
				return false;
				// Finally just check core BB intersections
				// Ignore anything that is not a core to allow blocking of connections
			else if (type == EnumPartClass.Panel)
			{
				List<CollidableComponent> boxes = this.getCollidableComponents();
				BoundingBox bb = new BoundingBox(Part.getBoundingBoxFromPool(pos, part.getSize()));
				for (CollidableComponent c : boxes)
				{
					if (c.dir == ForgeDirection.UNKNOWN && c.bound.intersects(bb))
						return true;
				}
			}
		return false;
	}

	@Override
	@Method(modid = "ImmibisMicroblocks")
	public IMicroblockCoverSystem getCoverSystem()
	{
		return (IMicroblockCoverSystem) this.covers;
	}

	@Method(modid = "ImmibisMicroblocks")
	private void writeMicroblocksToNBT(NBTTagCompound tag)
	{
		if (this.covers != null)
			((IMicroblockCoverSystem) this.covers).writeToNBT(tag);
	}

	@Method(modid = "ImmibisMicroblocks")
	private void readMicroblocksFromNBT(NBTTagCompound tag)
	{
		if (this.covers != null)
			((IMicroblockCoverSystem) this.covers).readFromNBT(tag);
	}

	@Override
	@Method(modid = "ImmibisMicroblocks")
	public Packet getDescriptionPacket()
	{
		if (this.covers == null)
			return super.getDescriptionPacket();

		NBTTagCompound tag = new NBTTagCompound();
		tag.setByteArray("C", ((IMicroblockCoverSystem) this.covers).writeDescriptionBytes());
		this.writeCustomNBT(tag);
		return new S35PacketUpdateTileEntity(this.xCoord, this.yCoord, this.zCoord, 1, tag);
	}

	@Override
	@Method(modid = "ImmibisMicroblocks")
	public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt)
	{
		super.onDataPacket(net, pkt);
		if (this.covers != null)
			((IMicroblockCoverSystem) this.covers).readDescriptionBytes(pkt.func_148857_g().getByteArray("C"), 0);
	}

	@Override
	@Method(modid = "ImmibisMicroblocks")
	public void onMicroblocksChanged()
	{
		Set<ForgeDirection> needUpdates = EnumSet.allOf(ForgeDirection.class);
		needUpdates.remove(ForgeDirection.UNKNOWN);
		for (Part p : this.getCoverSystem().getAllParts())
		{
			if (p.type.getPartClass() == EnumPartClass.Panel)
			{
				ForgeDirection dir = MicroblocksUtil.posToDir(p.pos);
				this.updateConnections(dir, true);
				needUpdates.remove(dir);
			}
		}
		for (ForgeDirection dir : needUpdates)
		{
			this.updateConnections(dir, false);
		}

		this.worldObj.notifyBlocksOfNeighborChange(this.xCoord, this.yCoord, this.zCoord, this.getBlockType());
		this.updateBlock();
	}

	@Method(modid = "ImmibisMicroblocks")
	private void updateConnections(ForgeDirection dir, boolean remove)
	{
		// TODO gamerforEA use BlockCoord.getSafeTileEntity
		TileEntity neighbor = this.getLocation().getLocation(dir).getSafeTileEntity(this.worldObj);
		IConduitBundle neighborBundle = (IConduitBundle) (neighbor instanceof IConduitBundle ? neighbor : null);
		for (IConduit c : this.getConduits())
		{
			if (remove)
				this.removeConnection(dir, c);
			else if (neighborBundle != null)
				this.addConnection(dir, c, neighborBundle.getConduit(c.getBaseConduitType()));
			c.connectionsChanged();
		}
		dir = dir.getOpposite();
		if (neighbor instanceof IConduitBundle)
			for (IConduit c : ((TileConduitBundle) neighbor).getConduits())
			{
				if (remove)
					this.removeConnection(dir, c);
				else if (neighborBundle != null)
					this.addConnection(dir, c, this.getConduit(c.getBaseConduitType()));
				c.connectionsChanged();
			}
	}

	@Method(modid = "ImmibisMicroblocks")
	private void removeConnection(ForgeDirection dir, IConduit c)
	{
		if (c.getConduitConnections().contains(dir))
			c.conduitConnectionRemoved(dir);
	}

	@Method(modid = "ImmibisMicroblocks")
	private void addConnection(ForgeDirection dir, IConduit c, IConduit connectingTo)
	{
		if (connectingTo != null)
			if (!c.getConduitConnections().contains(dir) && connectingTo.canConnectToConduit(dir, c))
				c.conduitConnectionAdded(dir);
	}

	// OpenComputers

	@Override
	@Method(modid = "OpenComputersAPI|Network")
	public Node node()
	{
		IOCConduit cond = this.getConduit(IOCConduit.class);
		if (cond != null)
			return cond.node();
		return null;
	}

	@Override
	@Method(modid = "OpenComputersAPI|Network")
	public void onConnect(Node node)
	{
		IOCConduit cond = this.getConduit(IOCConduit.class);
		if (cond != null)
			cond.onConnect(node);
	}

	@Override
	@Method(modid = "OpenComputersAPI|Network")
	public void onDisconnect(Node node)
	{
		IOCConduit cond = this.getConduit(IOCConduit.class);
		if (cond != null)
			cond.onDisconnect(node);
	}

	@Override
	@Method(modid = "OpenComputersAPI|Network")
	public void onMessage(Message message)
	{
		IOCConduit cond = this.getConduit(IOCConduit.class);
		if (cond != null)
			cond.onMessage(message);
	}

	@Override
	@Method(modid = "OpenComputersAPI|Network")
	public Node sidedNode(ForgeDirection side)
	{
		IOCConduit cond = this.getConduit(IOCConduit.class);
		if (cond != null)
			return cond.sidedNode(side);
		return null;
	}

	@Override
	@Method(modid = "OpenComputersAPI|Network")
	@SideOnly(Side.CLIENT)
	public boolean canConnect(ForgeDirection side)
	{
		IOCConduit cond = this.getConduit(IOCConduit.class);
		if (cond != null)
			return cond.canConnect(side);
		return false;
	}

	@Override
	public void invalidate()
	{
		super.invalidate();
		IOCConduit cond = this.getConduit(IOCConduit.class);
		if (cond != null)
			cond.invalidate();
	}

}
