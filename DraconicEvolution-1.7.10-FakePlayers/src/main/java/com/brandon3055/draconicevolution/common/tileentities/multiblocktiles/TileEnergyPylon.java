package com.brandon3055.draconicevolution.common.tileentities.multiblocktiles;

import cofh.api.energy.IEnergyHandler;
import cofh.api.energy.IEnergyReceiver;
import com.brandon3055.draconicevolution.api.IExtendedRFStorage;
import com.brandon3055.draconicevolution.client.handler.ParticleHandler;
import com.brandon3055.draconicevolution.client.render.particle.Particles;
import com.brandon3055.draconicevolution.common.ModBlocks;
import com.brandon3055.draconicevolution.common.blocks.multiblock.MultiblockHelper.TileLocation;
import com.brandon3055.draconicevolution.common.lib.References;
import com.brandon3055.draconicevolution.common.tileentities.TileObjectSync;
import com.brandon3055.draconicevolution.integration.computers.IDEPeripheral;
import com.gamerforea.draconicevolution.EventConfig;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.ChunkCache;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by Brandon on 28/07/2014.
 */
public class TileEnergyPylon extends TileObjectSync implements IEnergyHandler, IExtendedRFStorage, IDEPeripheral
{
	public boolean active = false;
	public boolean lastTickActive = false;
	public boolean reciveEnergy = false; //Power Flow to system
	public boolean lastTickReciveEnergy = false;
	public float modelRotation = 0;
	public float modelScale = 0;
	private List<TileLocation> coreLocatios = new ArrayList<>();
	private int selectedCore = 0;
	private byte particleRate = 0;
	private byte lastTickParticleRate = 0;
	private int lastCheckCompOverride = 0;
	private int tick = 0;

	@Override
	public void updateEntity()
	{
		// TODO gamerforEA code start
		if (this.findCoresTimerByNearbyCheck > 0)
			this.findCoresTimerByNearbyCheck--;
		// TODO gamerforEA code end

		if (this.active && this.worldObj.isRemote)
		{
			this.modelRotation += 1.5;
			this.modelScale += !this.reciveEnergy ? -0.01F : 0.01F;
			if (this.modelScale < 0 && !this.reciveEnergy)
				this.modelScale = 10000F;
			if (this.modelScale < 0 && this.reciveEnergy)
				this.modelScale = 0F;
			this.spawnParticles();
		}
		else if (this.worldObj.isRemote)
			this.modelScale = 0.5F;

		if (this.worldObj.isRemote)
			return;

		this.tick++;
		if (this.tick % 20 == 0)
		{
			int cOut = (int) (this.getEnergyStored() / this.getMaxEnergyStored() * 15D);
			if (cOut != this.lastCheckCompOverride)
			{
				this.worldObj.notifyBlocksOfNeighborChange(this.xCoord, this.yCoord, this.zCoord, this.worldObj.getBlock(this.xCoord, this.yCoord, this.zCoord));
				this.worldObj.notifyBlocksOfNeighborChange(this.xCoord - 1, this.yCoord, this.zCoord, this.worldObj.getBlock(this.xCoord, this.yCoord, this.zCoord));
				this.worldObj.notifyBlocksOfNeighborChange(this.xCoord + 1, this.yCoord, this.zCoord, this.worldObj.getBlock(this.xCoord, this.yCoord, this.zCoord));
				this.worldObj.notifyBlocksOfNeighborChange(this.xCoord, this.yCoord - 1, this.zCoord, this.worldObj.getBlock(this.xCoord, this.yCoord, this.zCoord));
				this.worldObj.notifyBlocksOfNeighborChange(this.xCoord, this.yCoord + 1, this.zCoord, this.worldObj.getBlock(this.xCoord, this.yCoord, this.zCoord));
				this.worldObj.notifyBlocksOfNeighborChange(this.xCoord, this.yCoord, this.zCoord - 1, this.worldObj.getBlock(this.xCoord, this.yCoord, this.zCoord));
				this.worldObj.notifyBlocksOfNeighborChange(this.xCoord, this.yCoord, this.zCoord + 1, this.worldObj.getBlock(this.xCoord, this.yCoord, this.zCoord));
				this.lastCheckCompOverride = cOut;
			}
		}

		if (this.active && !this.reciveEnergy)
			for (ForgeDirection d : ForgeDirection.VALID_DIRECTIONS)
			{
				TileEntity tile = this.worldObj.getTileEntity(this.xCoord + d.offsetX, this.yCoord + d.offsetY, this.zCoord + d.offsetZ);
				if (tile != null && tile instanceof IEnergyReceiver)
					this.extractEnergy(d, ((IEnergyReceiver) tile).receiveEnergy(d.getOpposite(), this.extractEnergy(d, Integer.MAX_VALUE, true), false), false);
			}

		this.detectAndSendChanges();
		if (this.particleRate > 0)
			this.particleRate--;
	}

	public void onActivated()
	{
		if (!this.active)
			this.active = this.isValidStructure();
		this.findCores();
	}

	private TileEnergyStorageCore getMaster()
	{
		if (this.coreLocatios.isEmpty())
			return null;
		if (this.selectedCore >= this.coreLocatios.size())
			this.selectedCore = this.coreLocatios.size() - 1;
		TileLocation core = this.coreLocatios.get(this.selectedCore);
		if (core == null || !(this.worldObj.getTileEntity(core.getXCoord(), core.getYCoord(), core.getZCoord()) instanceof TileEnergyStorageCore))
			return null;
		return (TileEnergyStorageCore) this.worldObj.getTileEntity(core.getXCoord(), core.getYCoord(), core.getZCoord());
	}

	// TODO gamerforEA code start
	public boolean findCoresNearbyCheck;
	private int findCoresTimerByNearbyCheck;
	// TODO gamerforEA code end

	private void findCores()
	{
		// TODO gamerforEA code start
		if (EventConfig.energyPylonOptimize && this.findCoresNearbyCheck)
		{
			if (this.findCoresTimerByNearbyCheck > 0)
				return;
			this.findCoresTimerByNearbyCheck = 10;
		}
		// TODO gamerforEA code end

		int yMod = this.worldObj.getBlockMetadata(this.xCoord, this.yCoord, this.zCoord) == 1 ? 15 : -15;
		int range = 15;
		List<TileLocation> locations = new ArrayList<>();

		/* TODO gamerforEA code replace, old code:
		for (int x = this.xCoord - range; x <= this.xCoord + range; x++)
			for (int y = this.yCoord + yMod - range; y <= this.yCoord + yMod + range; y++)
				for (int z = this.zCoord - range; z <= this.zCoord + range; z++)
					if (this.worldObj.getBlock(x, y, z) == ModBlocks.energyStorageCore)
						locations.add(new TileLocation(x, y, z)); */
		int xMin = this.xCoord - range;
		int xMax = this.xCoord + range;
		int yMin = this.yCoord + yMod - range;
		int yMax = this.yCoord + yMod + range;
		int zMin = this.zCoord - range;
		int zMax = this.zCoord + range;
		ChunkCache cache = new ChunkCache(this.worldObj, xMin, yMin, zMin, xMax, yMax, zMax, 0);
		for (int x = xMin; x <= xMax; x++)
		{
			for (int y = yMin; y <= yMax; y++)
			{
				for (int z = zMin; z <= zMax; z++)
				{
					if (cache.getBlock(x, y, z) == ModBlocks.energyStorageCore)
						locations.add(new TileLocation(x, y, z));
				}
			}
		}
		// TODO gamerforEA code end

		if (locations != this.coreLocatios)
		{
			this.coreLocatios.clear();
			this.coreLocatios.addAll(locations);
			this.selectedCore = this.selectedCore >= this.coreLocatios.size() ? 0 : this.selectedCore;
			this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
		}
	}

	public void nextCore()
	{
		this.findCores();
		this.selectedCore++;
		if (this.selectedCore >= this.coreLocatios.size())
			this.selectedCore = 0;
		this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
	}

	@SideOnly(Side.CLIENT)
	private void spawnParticles()
	{
		Random rand = this.worldObj.rand;
		if (this.getMaster() == null || !this.getMaster().isOnline())
			return;

		int x = this.getMaster().xCoord;
		int y = this.getMaster().yCoord;
		int z = this.getMaster().zCoord;
		int cYCoord = this.worldObj.getBlockMetadata(this.xCoord, this.yCoord, this.zCoord) == 1 ? this.yCoord + 1 : this.yCoord - 1;

		float disMod = this.getMaster().getTier() == 0 ? 0.5F : this.getMaster().getTier() == 1 ? 1F : this.getMaster().getTier() == 2 ? 1F : this.getMaster().getTier() == 3 ? 2F : this.getMaster().getTier() == 4 ? 2F : this.getMaster().getTier() == 5 ? 3F : 4F;
		double spawnX;
		double spawnY;
		double spawnZ;
		double targetX;
		double targetY;
		double targetZ;
		if (this.particleRate > 20)
			this.particleRate = 20;
		if (!this.reciveEnergy)
		{
			spawnX = x + 0.5 - disMod + rand.nextFloat() * (disMod * 2);
			spawnY = y + 0.5 - disMod + rand.nextFloat() * (disMod * 2);
			spawnZ = z + 0.5 - disMod + rand.nextFloat() * (disMod * 2);
			targetX = this.xCoord + 0.5;
			targetY = cYCoord + 0.5;
			targetZ = this.zCoord + 0.5;
			if (rand.nextFloat() < 0.05F)
			{
				Particles.EnergyTransferParticle passiveParticle = new Particles.EnergyTransferParticle(this.worldObj, spawnX, spawnY, spawnZ, targetX, targetY, targetZ, true);
				ParticleHandler.spawnCustomParticle(passiveParticle, 35);
			}
			if (this.particleRate > 0)
				if (this.particleRate > 10)
					for (int i = 0; i <= this.particleRate / 10; i++)
					{
						spawnX = x + 0.5 - disMod + rand.nextFloat() * (disMod * 2);
						spawnY = y + 0.5 - disMod + rand.nextFloat() * (disMod * 2);
						spawnZ = z + 0.5 - disMod + rand.nextFloat() * (disMod * 2);
						Particles.EnergyTransferParticle passiveParticle = new Particles.EnergyTransferParticle(this.worldObj, spawnX, spawnY, spawnZ, targetX, targetY, targetZ, false);
						ParticleHandler.spawnCustomParticle(passiveParticle, 35);
					}
				else if (rand.nextInt(Math.max(1, 10 - this.particleRate)) == 0)
				{
					spawnX = x + 0.5 - disMod + rand.nextFloat() * (disMod * 2);
					spawnY = y + 0.5 - disMod + rand.nextFloat() * (disMod * 2);
					spawnZ = z + 0.5 - disMod + rand.nextFloat() * (disMod * 2);
					Particles.EnergyTransferParticle passiveParticle = new Particles.EnergyTransferParticle(this.worldObj, spawnX, spawnY, spawnZ, targetX, targetY, targetZ, false);
					ParticleHandler.spawnCustomParticle(passiveParticle, 35);
				}

		}
		else
		{
			targetX = x + 0.5 - disMod + rand.nextFloat() * (disMod * 2);
			targetY = y + 0.5 - disMod + rand.nextFloat() * (disMod * 2);
			targetZ = z + 0.5 - disMod + rand.nextFloat() * (disMod * 2);
			spawnX = this.xCoord + 0.5;
			spawnY = cYCoord + 0.5;
			spawnZ = this.zCoord + 0.5;
			if (rand.nextFloat() < 0.05F)
			{
				Particles.EnergyTransferParticle passiveParticle = new Particles.EnergyTransferParticle(this.worldObj, spawnX, spawnY, spawnZ, targetX, targetY, targetZ, true);
				ParticleHandler.spawnCustomParticle(passiveParticle, 35);
			}

			if (this.particleRate > 0)
				if (this.particleRate > 10)
					for (int i = 0; i <= this.particleRate / 10; i++)
					{
						targetX = x + 0.5 - disMod + rand.nextFloat() * (disMod * 2);
						targetY = y + 0.5 - disMod + rand.nextFloat() * (disMod * 2);
						targetZ = z + 0.5 - disMod + rand.nextFloat() * (disMod * 2);
						Particles.EnergyTransferParticle passiveParticle = new Particles.EnergyTransferParticle(this.worldObj, spawnX, spawnY, spawnZ, targetX, targetY, targetZ, false);
						ParticleHandler.spawnCustomParticle(passiveParticle, 35);
					}
				else if (rand.nextInt(Math.max(1, 10 - this.particleRate)) == 0)
				{
					targetX = x + 0.5 - disMod + rand.nextFloat() * (disMod * 2);
					targetY = y + 0.5 - disMod + rand.nextFloat() * (disMod * 2);
					targetZ = z + 0.5 - disMod + rand.nextFloat() * (disMod * 2);
					Particles.EnergyTransferParticle passiveParticle = new Particles.EnergyTransferParticle(this.worldObj, spawnX, spawnY, spawnZ, targetX, targetY, targetZ, false);
					ParticleHandler.spawnCustomParticle(passiveParticle, 35);
				}
		}
	}

	private boolean isValidStructure()
	{
		return (this.isGlass(this.xCoord, this.yCoord + 1, this.zCoord) || this.isGlass(this.xCoord, this.yCoord - 1, this.zCoord)) && (!this.isGlass(this.xCoord, this.yCoord + 1, this.zCoord) || !this.isGlass(this.xCoord, this.yCoord - 1, this.zCoord));
	}

	private boolean isGlass(int x, int y, int z)
	{
		return this.worldObj.getBlock(x, y, z) == ModBlocks.invisibleMultiblock && this.worldObj.getBlockMetadata(x, y, z) == 2;
	}

	@Override
	public void readFromNBT(NBTTagCompound compound)
	{
		super.readFromNBT(compound);
		this.active = compound.getBoolean("Active");
		this.reciveEnergy = compound.getBoolean("Input");
		int i = compound.getInteger("Cores");
		List<TileLocation> list = new ArrayList<>();
		for (int j = 0; j < i; j++)
		{
			TileLocation l = new TileLocation();
			l.readFromNBT(compound, "Core" + j);
			list.add(l);
		}
		this.coreLocatios = list;
		this.selectedCore = compound.getInteger("SelectedCore");
		this.particleRate = compound.getByte("ParticleRate");
	}

	@Override
	public void writeToNBT(NBTTagCompound compound)
	{

		super.writeToNBT(compound);
		compound.setBoolean("Active", this.active);
		compound.setBoolean("Input", this.reciveEnergy);
		int i = this.coreLocatios.size();
		compound.setInteger("Cores", i);
		for (int j = 0; j < i; j++)
		{
			this.coreLocatios.get(j).writeToNBT(compound, "Core" + j);
		}
		compound.setInteger("SelectedCore", this.selectedCore);
		compound.setByte("ParticleRate", this.particleRate);
	}

	@Override
	public Packet getDescriptionPacket()
	{
		NBTTagCompound nbttagcompound = new NBTTagCompound();
		this.writeToNBT(nbttagcompound);
		return new S35PacketUpdateTileEntity(this.xCoord, this.yCoord, this.zCoord, 1, nbttagcompound);
	}

	@Override
	public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt)
	{
		this.readFromNBT(pkt.func_148857_g());
	}

	/* IEnergyHandler */
	@Override
	public boolean canConnectEnergy(ForgeDirection from)
	{
		return true;
	}

	@Override
	public int receiveEnergy(ForgeDirection from, int maxReceive, boolean simulate)
	{
		if (this.getMaster() == null)
			return 0;
		int received = this.reciveEnergy ? this.getMaster().receiveEnergy(maxReceive, simulate) : 0;
		if (!simulate && received > 0)
			this.particleRate = (byte) Math.min(20, received < 500 && received > 0 ? 1 : received / 500);
		return received;
	}

	@Override
	public int extractEnergy(ForgeDirection from, int maxExtract, boolean simulate)
	{
		if (this.getMaster() == null || !this.getMaster().isOnline())
			return 0;
		int extracted = this.reciveEnergy ? 0 : this.getMaster().extractEnergy(maxExtract, simulate);
		if (!simulate && extracted > 0)
			this.particleRate = (byte) Math.min(20, extracted < 500 && extracted > 0 ? 1 : extracted / 500);
		return extracted;
	}

	@Override
	public int getEnergyStored(ForgeDirection from)
	{
		if (this.getMaster() == null)
			return 0;
		return (int) Math.min(Integer.MAX_VALUE, this.getMaster().getEnergyStored());
	}

	@Override
	public int getMaxEnergyStored(ForgeDirection from)
	{
		if (this.getMaster() == null)
			return 0;
		return (int) Math.min(Integer.MAX_VALUE, this.getMaster().getMaxEnergyStored());
	}

	@Override
	public AxisAlignedBB getRenderBoundingBox()
	{
		return INFINITE_EXTENT_AABB;
	}

	private void detectAndSendChanges()
	{
		if (this.lastTickActive != this.active)
			this.lastTickActive = (Boolean) this.sendObjectToClient(References.BOOLEAN_ID, 0, this.active, new NetworkRegistry.TargetPoint(this.worldObj.provider.dimensionId, this.xCoord, this.yCoord, this.zCoord, 256));
		if (this.lastTickReciveEnergy != this.reciveEnergy)
			this.lastTickReciveEnergy = (Boolean) this.sendObjectToClient(References.BOOLEAN_ID, 1, this.reciveEnergy, new NetworkRegistry.TargetPoint(this.worldObj.provider.dimensionId, this.xCoord, this.yCoord, this.zCoord, 256));
		if (this.lastTickParticleRate != this.particleRate)
			this.lastTickParticleRate = (Byte) this.sendObjectToClient(References.BYTE_ID, 2, this.particleRate);
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void receiveObjectFromServer(int index, Object object)
	{
		switch (index)
		{
			case 0:
				this.active = (Boolean) object;
				break;
			case 1:
				this.reciveEnergy = (Boolean) object;
				break;
			case 2:
				this.particleRate = (Byte) object;
				break;
		}
	}

	@Override
	public double getEnergyStored()
	{
		return this.getMaster() != null ? this.getMaster().getEnergyStored() : 0D;
	}

	@Override
	public double getMaxEnergyStored()
	{
		return this.getMaster() != null ? this.getMaster().getMaxEnergyStored() : 0D;
	}

	@Override
	public long getExtendedStorage()
	{
		return this.getMaster() != null ? this.getMaster().getEnergyStored() : 0L;
	}

	@Override
	public long getExtendedCapacity()
	{
		return this.getMaster() != null ? this.getMaster().getMaxEnergyStored() : 0L;
	}

	@Override
	public String getName()
	{
		return "draconic_rf_storage";
	}

	@Override
	public String[] getMethodNames()
	{
		return new String[] { "getEnergyStored", "getMaxEnergyStored" };
	}

	@Override
	public Object[] callMethod(String method, Object... args)
	{
		if (method.equals("getEnergyStored"))
			return new Object[] { this.getExtendedStorage() };
		else if (method.equals("getMaxEnergyStored"))
			return new Object[] { this.getExtendedCapacity() };
		return new Object[0];
	}
}
