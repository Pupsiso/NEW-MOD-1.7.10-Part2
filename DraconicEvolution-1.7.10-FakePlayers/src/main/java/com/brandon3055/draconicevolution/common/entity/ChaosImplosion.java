package com.brandon3055.draconicevolution.common.entity;

import com.brandon3055.brandonscore.common.handlers.IProcess;
import com.brandon3055.brandonscore.common.handlers.ProcessHandler;
import com.brandon3055.brandonscore.common.utills.Utills;
import com.gamerforea.draconicevolution.EventConfig;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Created by brandon3055 on 12/8/2015.
 */
public class ChaosImplosion implements IProcess
{

	public static DamageSource chaosImplosion = new DamageSource("chaosImplosion").setExplosion().setDamageBypassesArmor().setDamageIsAbsolute().setDamageAllowedInCreativeMode();

	private World worldObj;
	private int xCoord;
	private int yCoord;
	private int zCoord;
	private float power;
	private Random random = new Random();

	private double expansion = 0;

	public ChaosImplosion(World world, int x, int y, int z)
	{
		this.worldObj = world;
		this.xCoord = x;
		this.yCoord = y;
		this.zCoord = z;
		this.power = 15F;
		this.isDead = world.isRemote;
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

		int OD = (int) this.expansion;
		int ID = OD - 1;
		int size = (int) this.expansion;

		for (int x = this.xCoord - size; x < this.xCoord + size; x++)
		{
			for (int z = this.zCoord - size; z < this.zCoord + size; z++)
			{
				double dist = Utills.getDistanceAtoB(x, z, this.xCoord, this.zCoord);
				if (dist < OD && dist >= ID)
				{
					float tracePower = this.power - (float) (this.expansion / 10D);
					tracePower *= 1F + (this.random.nextFloat() - 0.5F) * 0.2;
					ProcessHandler.addProcess(new ChaosImplosionTrace(this.worldObj, x, this.yCoord, z, tracePower, this.random));
				}
			}
		}

		this.isDead = this.expansion >= this.power * 10;
		this.expansion += 1;
	}

	private boolean isDead;

	@Override
	public boolean isDead()
	{
		return this.isDead;
	}
}
