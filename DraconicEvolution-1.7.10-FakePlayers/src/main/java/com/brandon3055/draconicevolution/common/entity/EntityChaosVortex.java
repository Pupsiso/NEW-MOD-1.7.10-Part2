package com.brandon3055.draconicevolution.common.entity;

import com.brandon3055.brandonscore.common.handlers.ProcessHandler;
import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.client.render.particle.Particles;
import com.brandon3055.draconicevolution.common.network.GenericParticlePacket;
import com.gamerforea.draconicevolution.EventConfig;
import cpw.mods.fml.common.network.NetworkRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import java.util.List;

/**
 * Created by brandon3055 on 3/10/2015.
 */
public class EntityChaosVortex extends Entity
{
	public EntityChaosVortex(World world)
	{
		super(world);
		this.noClip = true;
		this.setSize(0F, 0F);
	}

	@Override
	public boolean isInRangeToRenderDist(double p_70112_1_)
	{
		return true;
	}

	@Override
	protected void entityInit()
	{
		this.dataWatcher.addObject(20, this.ticksExisted);
	}

	@Override
	public void onUpdate()
	{
		// TODO gamerforEA code start
		if (!EventConfig.enableChaosExplosion)
		{
			this.setDead();
			return;
		}
		// TODO gamerforEA code end

		if (!this.worldObj.isRemote)
			this.dataWatcher.updateObject(20, this.ticksExisted);
		else
			this.ticksExisted = this.dataWatcher.getWatchableObjectInt(20);

		if (this.ticksExisted < 30 && this.ticksExisted % 5 == 0 && this.worldObj.isRemote)
			DraconicEvolution.proxy.spawnParticle(new Particles.ChaosExpansionParticle(this.worldObj, this.posX, this.posY, this.posZ, false), 512);
		if (this.ticksExisted >= 100 && this.ticksExisted < 130 && this.ticksExisted % 5 == 0 && this.worldObj.isRemote)
			DraconicEvolution.proxy.spawnParticle(new Particles.ChaosExpansionParticle(this.worldObj, this.posX, this.posY, this.posZ, true), 512);
		if (this.ticksExisted < 100)
			return;

		for (int i = 0; i < 10; i++)
		{
			double x = this.posX - 18 + this.rand.nextDouble() * 36;
			double y = this.posY - 8 + this.rand.nextDouble() * 16;
			double z = this.posZ - 18 + this.rand.nextDouble() * 36;
			if (this.worldObj.isRemote)
				DraconicEvolution.proxy.spawnParticle(new Particles.AdvancedSeekerParticle(this.worldObj, x, y, z, this.posX, this.posY, this.posZ, 2, 1f, 1f, 1f, 100), 128);
		}

		if (this.ticksExisted > 130 && this.worldObj.isRemote && this.ticksExisted % 2 == 0)
			this.shakeScreen();

		if (this.ticksExisted == 600 && !this.worldObj.isRemote)
		{
			DraconicEvolution.network.sendToAllAround(new GenericParticlePacket(GenericParticlePacket.CHAOS_IMPLOSION, this.posX, this.posY, this.posZ), new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 512));
			ProcessHandler.addProcess(new ChaosImplosion(this.worldObj, (int) this.posX, (int) this.posY, (int) this.posZ));
		}

		if (this.ticksExisted > 620)
			this.setDead();
	}

	private void shakeScreen()
	{
		double intensity = (this.ticksExisted - 130) / 100D;
		if (intensity > 1D)
			intensity = 1D;

		@SuppressWarnings("unchecked")
		List<EntityPlayer> players = this.worldObj.getEntitiesWithinAABB(EntityPlayer.class, this.boundingBox.expand(200, 200, 200));

		for (EntityPlayer player : players)
		{
			double x = (this.rand.nextDouble() - 0.5) * 2 * intensity;
			double z = (this.rand.nextDouble() - 0.5) * 2 * intensity;
			player.moveEntity(x / 5D, 0, z / 5D);
			player.rotationYaw -= x * 2;
			player.rotationPitch -= z * 2;
		}
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound p_70037_1_)
	{

	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound p_70014_1_)
	{

	}
}
