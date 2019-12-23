package com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.reactor;

import com.brandon3055.brandonscore.common.handlers.IProcess;
import com.gamerforea.draconicevolution.ModUtils;
import com.gamerforea.eventhelper.fake.FakePlayerContainer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import java.util.List;
import java.util.Random;

/**
 * Created by brandon3055 on 12/8/2015.
 */
public class ReactorExplosionTrace implements IProcess
{

	private World worldObj;
	private int xCoord;
	private int yCoord;
	private int zCoord;
	private float power;
	private Random random;

	// TODO gamerforEA code start
	public final FakePlayerContainer fake;
	// TODO gamerforEA code end

	public ReactorExplosionTrace(World world, int x, int y, int z, float power, Random random)
	{
		this.worldObj = world;
		this.xCoord = x;
		this.yCoord = y;
		this.zCoord = z;
		this.power = power;
		this.random = random;

		// TODO gamerforEA code start
		this.fake = ModUtils.NEXUS_FACTORY.wrapFake(world);
		// TODO gamerforEA code end
	}

	@Override
	public void updateProcess()
	{
		float energy = this.power * 10;

		for (int y = this.yCoord; y > 0 && energy > 0; y--)
		{
			// TODO gamerforEA code start
			if (this.fake.cantBreak(this.xCoord, y, this.zCoord))
				break;
			// TODO gamerforEA code end

			Block block = this.worldObj.getBlock(this.xCoord, y, this.zCoord);

			List<Entity> entities = this.worldObj.getEntitiesWithinAABB(Entity.class, AxisAlignedBB.getBoundingBox(this.xCoord, y, this.zCoord, this.xCoord + 1, y + 1, this.zCoord + 1));
			for (Entity entity : entities)
			{
				// TODO gamerforEA code start
				if (this.fake.cantDamage(entity))
					continue;
				// TODO gamerforEA code end

				entity.attackEntityFrom(ReactorExplosion.fusionExplosion, this.power * 100);
			}

			energy -= block instanceof BlockLiquid ? 10 : block.getExplosionResistance(null);

			boolean blockRemoved = false;
			if (energy >= 0 && block != Blocks.air)
			{
				this.worldObj.setBlockToAir(this.xCoord, y, this.zCoord);
				blockRemoved = true;
			}
			energy -= 0.5F + 0.1F * (this.yCoord - y);

			if (energy <= 0 && this.random.nextInt(20) == 0 && blockRemoved)
			{
				if (this.random.nextInt(3) > 0)
					this.worldObj.setBlock(this.xCoord, y, this.zCoord, Blocks.fire);
				else
				{
					this.worldObj.setBlock(this.xCoord, y, this.zCoord, Blocks.flowing_lava);
					//worldObj.scheduleBlockUpdate(xCoord, y, zCoord, Blocks.flowing_lava, 100);
				}
			}
		}

		energy = this.power * 20;
		this.yCoord++;
		for (int y = this.yCoord; y < 255 && energy > 0; y++)
		{
			// TODO gamerforEA code start
			if (this.fake.cantBreak(this.xCoord, y, this.zCoord))
				break;
			// TODO gamerforEA code end

			Block block = this.worldObj.getBlock(this.xCoord, y, this.zCoord);

			List<Entity> entities = this.worldObj.getEntitiesWithinAABB(Entity.class, AxisAlignedBB.getBoundingBox(this.xCoord, y, this.zCoord, this.xCoord + 1, y + 1, this.zCoord + 1));
			for (Entity entity : entities)
			{
				// TODO gamerforEA code start
				if (this.fake.cantDamage(entity))
					continue;
				// TODO gamerforEA code end

				entity.attackEntityFrom(ReactorExplosion.fusionExplosion, this.power * 100);
			}

			energy -= block instanceof BlockLiquid ? 10 : block.getExplosionResistance(null);
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
