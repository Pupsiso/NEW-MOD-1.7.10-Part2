package com.brandon3055.draconicevolution.common.tileentities;

import com.brandon3055.draconicevolution.common.entity.EntityChaosVortex;
import com.gamerforea.draconicevolution.EventConfig;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;

/**
 * Created by brandon3055 on 24/9/2015.
 */
public class TileChaosShard extends TileEntity
{

	public int tick = 0;
	public boolean guardianDefeated = false;
	private int soundTimer;
	public int locationHash = 0;

	@Override
	public void updateEntity()
	{
		this.tick++;

		if (this.tick > 1 && !this.worldObj.isRemote && this.locationHash != this.getLocationHash(this.xCoord, this.yCoord, this.zCoord, this.worldObj.provider.dimensionId))
			this.worldObj.setBlockToAir(this.xCoord, this.yCoord, this.zCoord);

		if (this.worldObj.isRemote && this.soundTimer-- <= 0)
		{
			this.soundTimer = 3600 + this.worldObj.rand.nextInt(1200);
			this.worldObj.playSound(this.xCoord + 0.5D, this.yCoord + 0.5D, this.zCoord + 0.5D, "draconicevolution:chaosChamberAmbient", 1.5F, this.worldObj.rand.nextFloat() * 0.4F + 0.8F, false);
		}

		if (!this.worldObj.isRemote && this.guardianDefeated && this.worldObj.rand.nextInt(50) == 0)
		{
			int x = 5 - this.worldObj.rand.nextInt(11);
			int z = 5 - this.worldObj.rand.nextInt(11);
			EntityLightningBolt bolt = new EntityLightningBolt(this.worldObj, this.xCoord + x, this.worldObj.getTopSolidOrLiquidBlock(this.xCoord + x, this.zCoord + z), this.zCoord + z);
			bolt.ignoreFrustumCheck = true;
			this.worldObj.addWeatherEffect(bolt);
		}
	}

	public void detonate()
	{
		if (!this.worldObj.isRemote && this.locationHash != this.getLocationHash(this.xCoord, this.yCoord, this.zCoord, this.worldObj.provider.dimensionId))
			this.worldObj.setBlockToAir(this.xCoord, this.yCoord, this.zCoord);
		else
		{
			// TODO gamerforEA code start
			if (!EventConfig.enableChaosExplosion)
				return;
			// TODO gamerforEA code end

			EntityChaosVortex vortex = new EntityChaosVortex(this.worldObj);
			vortex.setPosition(this.xCoord + 0.5, this.yCoord + 0.5, this.zCoord + 0.5);
			this.worldObj.spawnEntityInWorld(vortex);
		}
	}

	public void setDefeated()
	{
		this.guardianDefeated = true;
		this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
	}

	@Override
	public void writeToNBT(NBTTagCompound compound)
	{
		super.writeToNBT(compound);
		compound.setBoolean("GuardianDefeated", this.guardianDefeated);
		compound.setInteger("LocationHash", this.locationHash);
	}

	@Override
	public void readFromNBT(NBTTagCompound compound)
	{
		super.readFromNBT(compound);
		this.guardianDefeated = compound.getBoolean("GuardianDefeated");
		this.locationHash = compound.getInteger("LocationHash");
	}

	@Override
	public Packet getDescriptionPacket()
	{
		NBTTagCompound tagCompound = new NBTTagCompound();
		this.writeToNBT(tagCompound);
		return new S35PacketUpdateTileEntity(this.xCoord, this.yCoord, this.zCoord, 1, tagCompound);
	}

	@Override
	public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt)
	{
		this.readFromNBT(pkt.func_148857_g());
	}

	@Override
	public AxisAlignedBB getRenderBoundingBox()
	{
		return super.getRenderBoundingBox().expand(1, 3, 1);
	}

	public int getLocationHash(int xCoord, int yCoord, int zCoord, int dimension)
	{
		return (String.valueOf(xCoord) + yCoord + zCoord + dimension).hashCode();
	}
}
