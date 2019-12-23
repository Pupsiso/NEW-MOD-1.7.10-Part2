package net.divinerpg.entities.iceika.projectile;

import com.gamerforea.divinerpg.EntityThrowableByPlayer;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import java.util.Random;

// TODO gamerforEA replace EntityThrowable to EntityThrowableByPlayer
public class EntityCarol extends EntityThrowableByPlayer
{
	Random r = new Random();
	int color = this.r.nextInt(25);

	public EntityCarol(World var1)
	{
		super(var1);
	}

	public EntityCarol(World var1, EntityLivingBase var2)
	{
		super(var1, var2);
	}

	public EntityCarol(World var1, double var2, double var4, double var6)
	{
		super(var1, var2, var4, var6);
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void onUpdate()
	{
		super.onUpdate();
		if (this.color >= 24)
			this.color = 0;
		else
			this.color++;
		this.worldObj.spawnParticle("note", this.posX, this.posY, this.posZ, (double) this.color / 24.0D, 0.0D, 0.0D);
	}

	@Override
	protected void onImpact(MovingObjectPosition mop)
	{
		// TODO gamerforEA add condition [2]
		if (mop.entityHit != null && !this.fake.cantDamage(mop.entityHit))
			mop.entityHit.attackEntityFrom(DamageSource.causeThrownDamage(this, this.getThrower()), 16.0F);

		if (!this.worldObj.isRemote)
			this.setDead();
	}
}
