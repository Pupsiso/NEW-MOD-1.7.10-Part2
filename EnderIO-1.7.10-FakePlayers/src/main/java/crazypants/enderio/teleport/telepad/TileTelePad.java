package crazypants.enderio.teleport.telepad;

import cofh.api.energy.EnergyStorage;
import com.enderio.core.api.common.util.IProgressTile;
import com.enderio.core.common.util.BlockCoord;
import com.enderio.core.common.util.Util;
import com.gamerforea.enderio.EventConfig;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import crazypants.enderio.api.teleport.ITelePad;
import crazypants.enderio.api.teleport.TravelSource;
import crazypants.enderio.config.Config;
import crazypants.enderio.machine.AbstractMachineEntity;
import crazypants.enderio.machine.MachineSound;
import crazypants.enderio.machine.PacketPowerStorage;
import crazypants.enderio.network.PacketHandler;
import crazypants.enderio.power.IInternalPowerReceiver;
import crazypants.enderio.rail.TeleporterEIO;
import crazypants.enderio.teleport.TravelController;
import crazypants.enderio.teleport.anchor.TileTravelAnchor;
import crazypants.enderio.teleport.packet.PacketTravelEvent;
import crazypants.enderio.teleport.telepad.PacketTeleport.Type;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecartContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraft.world.Teleporter;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.EnumSet;
import java.util.List;
import java.util.Queue;

public class TileTelePad extends TileTravelAnchor implements IInternalPowerReceiver, ITelePad, IProgressTile
{

	private boolean inNetwork;
	private boolean isMaster;

	private EnergyStorage energy = new EnergyStorage(Config.telepadPowerStorageRF, Config.telepadPowerPerTickRF, Config.telepadPowerPerTickRF);

	private TileTelePad master = null;

	private boolean autoUpdate = false;

	private boolean coordsChanged = false;

	private BlockCoord target = new BlockCoord();
	private int targetDim = Integer.MIN_VALUE;

	private int lastSyncPowerStored;

	private Queue<Entity> toTeleport = Queues.newArrayDeque();
	private int powerUsed;
	private int maxPower;

	private static final ResourceLocation activeRes = AbstractMachineEntity.getSoundFor("telepad.active");
	private MachineSound activeSound = null;

	private boolean redstoneActivePrev;

	public static final String TELEPORTING_KEY = "eio:teleporting";
	public static final String PROGRESS_KEY = "teleportprogress";

	boolean wasBlocked = false;

	// Clientside rendering data
	public float[] bladeRots = new float[3];
	public float spinSpeed = 0;
	public float speedMult = 2.5f;

	@Override
	public void doUpdate()
	{
		super.doUpdate();

		// my master is gone!
		if (this.master != null && this.master.isInvalid())
			this.master.breakNetwork();

		if (this.autoUpdate)
		{
			this.updateConnectedState(true);
			this.autoUpdate = false;
		}

		if (this.targetDim == Integer.MIN_VALUE)
			this.targetDim = this.worldObj.provider.dimensionId;

		if (this.worldObj.isRemote && this.isMaster())
		{
			this.updateRotations();
			if (this.activeSound != null)
				this.activeSound.setPitch(MathHelper.clamp_float(0.5f + this.spinSpeed / 1.5f, 0.5f, 2));
			if (this.active())
			{
				if (this.activeSound == null)
				{
					this.activeSound = new MachineSound(activeRes, this.xCoord, this.yCoord, this.zCoord, 0.3f, 1);
					this.playSound();
				}
				this.updateQueuedEntities();
			}
			else if (!this.active() && this.activeSound != null)
				if (this.activeSound.getPitch() <= 0.5f)
				{
					this.activeSound.endPlaying();
					this.activeSound = null;
				}
		}
		else if (!this.worldObj.isRemote)
		{
			if (this.active())
			{
				if (this.powerUsed >= this.maxPower)
				{
					this.teleport(this.toTeleport.poll());
					this.powerUsed = 0;
				}
				else
					this.powerUsed += this.energy.extractEnergy(Math.min(this.getUsage(), this.maxPower), false);
				if (this.shouldDoWorkThisTick(5))
					this.updateQueuedEntities();
			}

			boolean powerChanged = this.lastSyncPowerStored != this.getEnergyStored() && this.shouldDoWorkThisTick(5);
			if (powerChanged)
			{
				this.lastSyncPowerStored = this.getEnergyStored();
				PacketHandler.sendToAllAround(new PacketPowerStorage(this), this);
			}
			if (this.coordsChanged && this.inNetwork() && this.master != null && this.isMaster())
			{
				this.coordsChanged = false;
				PacketHandler.sendToAllAround(new PacketUpdateCoords(this.master, this.master.getX(), this.master.getY(), this.master.getZ(), this.master.getTargetDim()), this.master);
			}
		}
	}

	@SideOnly(Side.CLIENT)
	private void playSound()
	{
		FMLClientHandler.instance().getClient().getSoundHandler().playSound(this.activeSound);
	}

	private void updateQueuedEntities()
	{
		if (this.worldObj.isRemote)
			if (this.active())
				this.getCurrentTarget().getEntityData().setFloat(PROGRESS_KEY, this.getProgress());
		List<Entity> toRemove = Lists.newArrayList();
		for (Entity e : this.toTeleport)
		{
			if (!this.isEntityInRange(e) || e.isDead)
				toRemove.add(e);
		}
		for (Entity e : toRemove)
		{
			this.dequeueTeleport(e, true);
		}
	}

	public void updateConnectedState(boolean fromBlock)
	{

		EnumSet<ForgeDirection> connections = EnumSet.noneOf(ForgeDirection.class);

		for (BlockCoord bc : this.getSurroundingCoords())
		{
			TileEntity te = bc.getTileEntity(this.worldObj);
			ForgeDirection con = Util.getDirFromOffset(bc.x - this.xCoord, 0, bc.z - this.zCoord);
			if (te instanceof TileTelePad)
			{
				// let's find the master and let him do the work
				if (fromBlock)
				{
					// Recurse to all adjacent (diagonal and axis-aligned) telepads, but only 1 deep.
					((TileTelePad) te).updateConnectedState(false);
					// If that telepad turned into a master, we can stop our search, as we were added to its network
					if (((TileTelePad) te).inNetwork() && !this.inNetwork)
						return;
				}
				// otherwise we either are the master or this is a secondary call, so update connections
				if (con != ForgeDirection.UNKNOWN && !((TileTelePad) te).inNetwork())
					connections.add(con);
			}
			else
			{
				connections.remove(con);
				if (this.master == this)
				{
					this.breakNetwork();
					this.updateBlock();
				}
				else if (con != ForgeDirection.UNKNOWN)
					if (this.inNetwork() && this.master != null && fromBlock)
						this.master.updateConnectedState(false);
			}
		}
		if (connections.size() == 4 && !this.inNetwork())
		{
			this.inNetwork = this.formNetwork();
			this.updateBlock();
			if (this.inNetwork())
				if (this.target.equals(new BlockCoord()))
					this.target = new BlockCoord(this);
		}
	}

	public void updateRedstoneState()
	{
		if (!this.inNetwork())
			return;

		boolean redstone = this.isPoweredRedstone();
		if (!this.master.redstoneActivePrev && redstone)
			this.teleportAll();
		this.master.redstoneActivePrev = redstone;
	}

	private boolean formNetwork()
	{
		List<TileTelePad> temp = Lists.newArrayList();

		for (BlockCoord c : this.getSurroundingCoords())
		{
			TileEntity te = c.getTileEntity(this.worldObj);
			if (!(te instanceof TileTelePad) || ((TileTelePad) te).inNetwork())
				return false;
			temp.add((TileTelePad) te);
		}

		for (TileTelePad te : temp)
		{
			te.master = this;
			te.inNetwork = true;
			te.updateBlock();
			te.updateNeighborTEs();
		}
		this.master = this;
		this.isMaster = true;
		return true;
	}

	private void breakNetwork()
	{
		this.master = null;
		this.inNetwork = false;
		this.isMaster = false;
		for (BlockCoord c : this.getSurroundingCoords())
		{
			TileEntity te = c.getTileEntity(this.worldObj);
			if (te instanceof TileTelePad)
			{
				TileTelePad telepad = (TileTelePad) te;
				telepad.master = null;
				telepad.inNetwork = false;
				telepad.updateBlock();
				telepad.updateNeighborTEs();
			}
		}
	}

	private List<BlockCoord> getSurroundingCoords()
	{
		List<BlockCoord> ret = Lists.newArrayList();
		for (int x = -1; x <= 1; x++)
		{
			for (int z = -1; z <= 1; z++)
			{
				if (x != 0 || z != 0)
					ret.add(new BlockCoord(this.xCoord + x, this.yCoord, this.zCoord + z));
			}
		}
		return ret;
	}

	private void updateNeighborTEs()
	{
		BlockCoord bc = new BlockCoord(this);
		for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS)
		{
			BlockCoord neighbor = bc.getLocation(dir);
			Block block = neighbor.getBlock(this.worldObj);
			if (!(block instanceof BlockTelePad))
				block.onNeighborChange(this.worldObj, neighbor.x, neighbor.y, neighbor.z, this.xCoord, this.yCoord, this.zCoord);
		}
	}

	@Override
	public boolean shouldUpdate()
	{
		return true;
	}

	@Override
	protected void writeCustomNBT(NBTTagCompound root)
	{
		super.writeCustomNBT(root);
		this.energy.writeToNBT(root);
		this.target.writeToNBT(root);
		root.setInteger("targetDim", this.targetDim);
		root.setBoolean("redstoneActive", this.redstoneActivePrev);
	}

	@Override
	protected void readCustomNBT(NBTTagCompound root)
	{
		super.readCustomNBT(root);
		this.energy.readFromNBT(root);
		this.target = BlockCoord.readFromNBT(root);
		this.targetDim = root.getInteger("targetDim");
		this.redstoneActivePrev = root.getBoolean("redstoneActive");
		this.autoUpdate = true;
	}

	@Override
	public Packet getDescriptionPacket()
	{
		S35PacketUpdateTileEntity pkt = (S35PacketUpdateTileEntity) super.getDescriptionPacket();
		//    pkt.func_148857_g().setBoolean("inNetwork", inNetwork);
		return pkt;
	}

	@Override
	public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt)
	{
		super.onDataPacket(net, pkt);
		//    this.inNetwork = pkt.func_148857_g().getBoolean("inNetwork");
	}

	@Override
	public void invalidate()
	{
		super.invalidate();
		if (this.worldObj.isRemote)
			this.stopPlayingSound();
	}

	@Override
	public void onChunkUnload()
	{
		super.onChunkUnload();
		if (this.worldObj.isRemote)
			this.stopPlayingSound();
	}

	private void stopPlayingSound()
	{
		if (this.activeSound != null)
		{
			this.activeSound.endPlaying();
			this.activeSound = null;
		}
	}

	public int getPowerScaled(int scale)
	{
		return (int) ((float) this.getEnergyStored() / (float) this.getMaxEnergyStored() * scale);
	}

	private int calculateTeleportPower()
	{
		if (this.worldObj.provider.dimensionId == this.targetDim)
		{
			int distance = new BlockCoord(this).getDist(this.target);
			double base = Math.log(0.005 * distance + 1);
			this.maxPower = (int) (base * Config.telepadPowerCoefficient);
			if (this.maxPower <= 0)
				this.maxPower = 1;
		}
		else
			this.maxPower = Config.telepadPowerInterdimensional;
		return this.maxPower;
	}

	public boolean active()
	{
		return !this.toTeleport.isEmpty();
	}

	public Entity getCurrentTarget()
	{
		return this.toTeleport.peek();
	}

	public AxisAlignedBB getBoundingBox()
	{
		if (!this.inNetwork())
			return AxisAlignedBB.getBoundingBox(this.xCoord, this.yCoord, this.zCoord, this.xCoord + 1, this.yCoord + 1, this.zCoord + 1);
		TileTelePad master = this.getMaster();
		return AxisAlignedBB.getBoundingBox(master.xCoord - 1, master.yCoord, master.zCoord - 1, master.xCoord + 2, master.yCoord + 1, master.zCoord + 2);
	}

	@Override
	public AxisAlignedBB getRenderBoundingBox()
	{
		return this.getBoundingBox();
	}

	public void updateRotations()
	{
		if (this.active())
			this.spinSpeed = this.getProgress() * 2;
		else
			this.spinSpeed = Math.max(0, this.spinSpeed - 0.025f);

		for (int i = 0; i < this.bladeRots.length; i++)
		{
			this.bladeRots[i] += this.spinSpeed * (i * 2 + 20);
			this.bladeRots[i] %= 360;
		}
	}

	/* IProgressTile */

	@Override
	public float getProgress()
	{
		return (float) this.powerUsed / (float) this.maxPower;
	}

	@Override
	protected int getProgressUpdateFreq()
	{
		return 1;
	}

	@Override
	public void setProgress(float progress)
	{
		this.powerUsed = progress < 0 ? 0 : (int) (this.maxPower * progress);
	}

	@Override
	public TileEntity getTileEntity()
	{
		return this;
	}

	/* ITelePad */

	@Override
	public boolean isMaster()
	{
		return this.isMaster;
	}

	@Override
	public TileTelePad getMaster()
	{
		return this.master;
	}

	@Override
	public boolean inNetwork()
	{
		return this.inNetwork;
	}

	@Override
	public int getX()
	{
		if (this.inNetwork())
			return this.master.target.x;
		return this.target.x;
	}

	@Override
	public int getY()
	{
		if (this.inNetwork())
			return this.master.target.y;
		return this.target.y;
	}

	@Override
	public int getZ()
	{
		if (this.inNetwork())
			return this.master.target.z;
		return this.target.z;
	}

	@Override
	public int getTargetDim()
	{
		if (this.inNetwork())
			return this.master.targetDim;
		return this.targetDim;
	}

	@Override
	public ITelePad setX(int x)
	{
		return Config.telepadLockCoords ? null : this.setX_internal(x);
	}

	@Override
	public ITelePad setY(int y)
	{
		return Config.telepadLockCoords ? null : this.setY_internal(y);
	}

	@Override
	public ITelePad setZ(int z)
	{
		return Config.telepadLockCoords ? null : this.setZ_internal(z);
	}

	@Override
	public ITelePad setTargetDim(int dimID)
	{
		return Config.telepadLockDimension ? null : this.setTargetDim_internal(dimID);
	}

	@Override
	public void setCoords(BlockCoord coords)
	{
		if (!Config.telepadLockCoords)
			this.setCoords_internal(coords);
	}

	ITelePad setX_internal(int x)
	{
		if (this.inNetwork())
		{
			this.setCoords(new BlockCoord(x, this.target.y, this.target.z));
			return this.master;
		}
		return null;
	}

	ITelePad setY_internal(int y)
	{
		if (this.inNetwork())
		{
			this.setCoords(new BlockCoord(this.target.x, y, this.target.z));
			return this.master;
		}
		return null;
	}

	ITelePad setZ_internal(int z)
	{
		if (this.inNetwork())
		{
			this.setCoords(new BlockCoord(this.target.x, this.target.y, z));
			return this.master;
		}
		return null;
	}

	ITelePad setTargetDim_internal(int dimID)
	{
		if (this.inNetwork())
		{
			this.master.targetDim = dimID;
			this.coordsChanged = true;
			return this.master;
		}
		return null;
	}

	void setCoords_internal(BlockCoord coords)
	{
		if (this.inNetwork())
			if (this.isMaster())
			{
				this.target = coords;
				this.coordsChanged = true;
			}
			else
				this.master.setCoords_internal(coords);
	}

	@Override
	public void teleportSpecific(Entity entity)
	{
		if (!this.inNetwork())
			return;
		if (this.isMaster())
		{
			if (this.isEntityInRange(entity))
				this.enqueueTeleport(entity, true);
		}
		else
			this.master.teleportSpecific(entity);
	}

	@Override
	public void teleportAll()
	{
		if (!this.inNetwork())
			return;
		if (this.isMaster())
			for (Entity e : this.getEntitiesInRange())
			{
				this.enqueueTeleport(e, true);
			}
		else
			this.master.teleportAll();
	}

	@SuppressWarnings("unchecked")
	private List<Entity> getEntitiesInRange()
	{
		return this.worldObj.getEntitiesWithinAABB(Entity.class, this.getRange());
	}

	private boolean isEntityInRange(Entity entity)
	{
		return this.getRange().isVecInside(Vec3.createVectorHelper(entity.posX, entity.posY, entity.posZ));
	}

	private AxisAlignedBB getRange()
	{
		return AxisAlignedBB.getBoundingBox(this.xCoord - 1, this.yCoord, this.zCoord - 1, this.xCoord + 2, this.yCoord + 3, this.zCoord + 2);
	}

	void enqueueTeleport(Entity entity, boolean sendUpdate)
	{
		if (entity == null || this.toTeleport.contains(entity))
			return;

		// TODO gamerforEA code start
		if (EventConfig.telepadPlayerOnly && !(entity instanceof EntityPlayer))
			return;
		// TODO gamerforEA code end

		this.calculateTeleportPower();
		entity.getEntityData().setBoolean(TELEPORTING_KEY, true);
		this.toTeleport.add(entity);
		if (sendUpdate)
			if (entity.worldObj.isRemote)
				PacketHandler.INSTANCE.sendToServer(new PacketTeleport(Type.BEGIN, this, entity.getEntityId()));
			else
				PacketHandler.INSTANCE.sendToAll(new PacketTeleport(Type.BEGIN, this, entity.getEntityId()));
	}

	void dequeueTeleport(Entity entity, boolean sendUpdate)
	{
		if (entity == null)
			return;
		this.toTeleport.remove(entity);
		entity.getEntityData().setBoolean(TELEPORTING_KEY, false);
		if (sendUpdate)
			if (this.worldObj.isRemote)
				PacketHandler.INSTANCE.sendToServer(new PacketTeleport(Type.END, this, entity.getEntityId()));
			else
				PacketHandler.INSTANCE.sendToAll(new PacketTeleport(Type.END, this, entity.getEntityId()));
		if (!this.active())
			this.powerUsed = 0;
	}

	private boolean teleport(Entity entity)
	{
		if (this.maxPower > 0)
		{
			entity.getEntityData().setBoolean(TELEPORTING_KEY, false);
			this.wasBlocked = entity.worldObj.isRemote ? !this.clientTeleport(entity) : !this.serverTeleport(entity);
			PacketHandler.INSTANCE.sendToAll(new PacketTeleport(Type.TELEPORT, this, this.wasBlocked));
			return !this.wasBlocked;
		}
		return false;
	}

	private boolean clientTeleport(Entity entity)
	{
		if (entity.worldObj.provider.dimensionId == this.targetDim)
			return TravelController.instance.doClientTeleport(entity, this.target, TravelSource.TELEPAD, 0, false);
		return true;
	}

	private boolean serverTeleport(Entity entity)
	{
		this.dequeueTeleport(entity, true);

		// TODO gamerforEA code start
		if (EventConfig.telepadPlayerOnly && !(entity instanceof EntityPlayer))
			return false;
		// TODO gamerforEA code end

		int from = entity.dimension;
		if (from != this.targetDim)
		{
			MinecraftServer server = MinecraftServer.getServer();
			WorldServer fromDim = server.worldServerForDimension(from);
			WorldServer toDim = server.worldServerForDimension(this.targetDim);
			Teleporter teleporter = new TeleporterEIO(toDim);
			server.worldServerForDimension(entity.dimension).playSoundEffect(entity.posX, entity.posY, entity.posZ, TravelSource.TELEPAD.sound, 1.0F, 1.0F);
			if (entity instanceof EntityPlayer)
			{
				EntityPlayerMP player = (EntityPlayerMP) entity;
				server.getConfigurationManager().transferPlayerToDimension(player, this.targetDim, teleporter);
				if (from == 1 && entity.isEntityAlive())
				{ // get around vanilla End hacks
					toDim.spawnEntityInWorld(entity);
					toDim.updateEntityWithOptionalForce(entity, false);
				}
			}
			else
			{
				NBTTagCompound tagCompound = new NBTTagCompound();
				float rotationYaw = entity.rotationYaw;
				float rotationPitch = entity.rotationPitch;
				entity.writeToNBT(tagCompound);

				// TODO gamerforEA code start
				if (entity instanceof EntityMinecartContainer)
				{
					EntityMinecartContainer e = (EntityMinecartContainer) entity;
					try
					{
						ReflectionHelper.setPrivateValue(EntityMinecartContainer.class, e, false, "field_94112_b", "dropContentsWhenDead");
					}
					catch (Throwable throwable)
					{
						for (int i = 0; i < e.getSizeInventory(); i++)
						{
							e.setInventorySlotContents(i, null);
						}
					}
				}
				else if (entity instanceof IInventory)
				{
					IInventory entityInventory = (IInventory) entity;
					for (int i = 0; i < entityInventory.getSizeInventory(); i++)
					{
						entityInventory.setInventorySlotContents(i, null);
					}
				}
				// TODO gamerforEA code end

				Class<? extends Entity> entityClass = entity.getClass();
				fromDim.removeEntity(entity);

				try
				{
					Entity newEntity = entityClass.getConstructor(World.class).newInstance(toDim);
					newEntity.readFromNBT(tagCompound);
					newEntity.setLocationAndAngles(this.target.x, this.target.y, this.target.z, rotationYaw, rotationPitch);
					newEntity.forceSpawn = true;
					toDim.spawnEntityInWorld(newEntity);
					newEntity.forceSpawn = false; // necessary?
				}
				catch (Exception e)
				{
					Throwables.propagate(e);
				}
			}
		}
		return PacketTravelEvent.doServerTeleport(entity, this.target.x, this.target.y, this.target.z, 0, false, TravelSource.TELEPAD);
	}

	/* ITravelAccessable overrides */

	@Override
	public boolean canSeeBlock(EntityPlayer playerName)
	{
		return this.isMaster() && this.inNetwork();
	}

	/* IInternalPowerReceiver */

	@Override
	public int getMaxEnergyRecieved(ForgeDirection dir)
	{
		return this.inNetwork() && this.master != null ? this.master == this ? this.energy.getMaxReceive() : this.master.getMaxEnergyRecieved(dir) : 0;
	}

	@Override
	public int getMaxEnergyStored()
	{
		return this.inNetwork() && this.master != null ? this.master == this ? this.energy.getMaxEnergyStored() : this.master.getMaxEnergyStored() : 0;
	}

	@Override
	public boolean displayPower()
	{
		return this.inNetwork() && this.master != null;
	}

	@Override
	public int getEnergyStored()
	{
		return this.inNetwork() && this.master != null ? this.master == this ? this.energy.getEnergyStored() : this.master.getEnergyStored() : 0;
	}

	@Override
	public void setEnergyStored(int storedEnergy)
	{
		if (this.inNetwork() && this.master != null)
			if (this.master == this)
				this.energy.setEnergyStored(storedEnergy);
			else
				this.master.setEnergyStored(storedEnergy);
	}

	@Override
	public boolean canConnectEnergy(ForgeDirection from)
	{
		return this.inNetwork() && this.master != null;
	}

	@Override
	public int receiveEnergy(ForgeDirection from, int maxReceive, boolean simulate)
	{
		return this.inNetwork() && this.master != null ? this.master == this ? this.energy.receiveEnergy(maxReceive, simulate) : this.master.receiveEnergy(from, maxReceive, simulate) : 0;
	}

	@Override
	public int getEnergyStored(ForgeDirection from)
	{
		return this.inNetwork() && this.master != null ? this.master == this ? this.energy.getEnergyStored() : this.master.getEnergyStored() : 0;
	}

	@Override
	public int getMaxEnergyStored(ForgeDirection from)
	{
		return this.inNetwork() && this.master != null ? this.master == this ? this.energy.getMaxEnergyStored() : this.master.getMaxEnergyStored() : 0;
	}

	public int getUsage()
	{
		return this.energy.getMaxReceive();
	}
}
