package com.brandon3055.draconicevolution.common.entity;

import com.brandon3055.brandonscore.common.handlers.IProcess;
import com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.reactor.ReactorExplosion;
import com.gamerforea.draconicevolution.EventConfig;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import java.util.List;
import java.util.Random;

/**
 * Created by brandon3055 on 12/8/2015.
 */
public class ChaosImplosionTrace implements IProcess
{

	private World worldObj;
	private int xCoord;
	private int yCoord;
	private int zCoord;
	private float power;
	private Random random;

	public ChaosImplosionTrace(World world, int x, int y, int z, float power, Random random)
	{
		this.worldObj = world;
		this.xCoord = x;
		this.yCoord = y;
		this.zCoord = z;
		this.power = power;
		this.random = random;
	}

	@Override
	public void updateProcess()
	{
		// TODO gamerforEA code start
		if (!EventConfig.enableChaosExplosion)
		{
			this.isDead = true;
			return;
		}
		// TODO gamerforEA code end

		float energy = this.power * 10;

		for (int y = this.yCoord; y > 0 && energy > 0; y--)
		{
			List<Entity> entities = this.worldObj.getEntitiesWithinAABB(Entity.class, AxisAlignedBB.getBoundingBox(this.xCoord, y, this.zCoord, this.xCoord + 1, y + 1, this.zCoord + 1));
			for (Entity entity : entities)
			{
				entity.attackEntityFrom(ChaosImplosion.chaosImplosion, this.power * 100);
			}

			//energy -= block instanceof BlockLiquid ? 10 : block.getExplosionResistance(null);

			if (energy >= 0)
				this.worldObj.setBlockToAir(this.xCoord, y, this.zCoord);
			energy -= 0.5F + 0.1F * (this.yCoord - y);
		}

		energy = this.power * 20;
		this.yCoord++;
		for (int y = this.yCoord; y < 255 && energy > 0; y++)
		{
			List<Entity> entities = this.worldObj.getEntitiesWithinAABB(Entity.class, AxisAlignedBB.getBoundingBox(this.xCoord, y, this.zCoord, this.xCoord + 1, y + 1, this.zCoord + 1));
			for (Entity entity : entities)
			{
				entity.attackEntityFrom(ReactorExplosion.fusionExplosion, this.power * 100);
			}

			//energy -= block instanceof BlockLiquid ? 10 : block.getExplosionResistance(null);
			if (energy >= 0)
				this.worldObj.setBlockToAir(this.xCoord, y, this.zCoord);

			energy -= 0.5F + 0.1F * (y - this.yCoord);
		}

		this.isDead = true;
	}

	private boolean isDead = false;

	@Override
	public boolean isDead()
	{
		return this.isDead;
	}
}
