package net.divinerpg.entities.arcana.projectile;

import com.gamerforea.divinerpg.EntityThrowableByPlayer;
import com.gamerforea.divinerpg.ExplosionByPlayer;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

// TODO gamerforEA replace EntityThrowable to EntityThrowableByPlayer
public class EntityMeteor extends EntityThrowableByPlayer
{
	public EntityMeteor(World par1World)
	{
		super(par1World);
		this.motionX = this.rand.nextGaussian() * 0.05;
		this.motionZ = this.rand.nextGaussian() * 0.05;
		this.motionY = -0.5;
	}

	public EntityMeteor(World par1World, EntityLiving par2EntityLiving)
	{
		super(par1World, par2EntityLiving);
		this.motionX = this.rand.nextGaussian() * 0.05;
		this.motionZ = this.rand.nextGaussian() * 0.05;
		this.motionY = -0.5;
	}

	public EntityMeteor(World par1World, double par2, double par4, double par6)
	{
		super(par1World, par2, par4, par6);
		this.motionX = this.rand.nextGaussian() * 0.05;
		this.motionZ = this.rand.nextGaussian() * 0.05;
		this.motionY = -0.5;
	}

	@Override
	protected void onImpact(MovingObjectPosition mop)
	{
		// TODO gamerforEA add condition [2]
		if (mop.entityHit != null && !this.fake.cantDamage(mop.entityHit))
			mop.entityHit.attackEntityFrom(DamageSource.causeThrownDamage(this, this.getThrower()), 12);

		// TODO gamerforEA use ExplosionByPlayer
		ExplosionByPlayer.createExplosion(this.fake.get(), this.worldObj, this, this.posX, this.posY, this.posZ, 4.5F, false);

		if (!this.worldObj.isRemote)
			this.setDead();
	}

	@Override
	protected float getGravityVelocity()
	{
		return 0.07F;
	}
}
