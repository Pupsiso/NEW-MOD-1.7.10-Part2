package net.divinerpg.entities.vethea.projectile;

import com.gamerforea.divinerpg.EntityThrowableByPlayer;
import net.divinerpg.utils.Util;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

// TODO gamerforEA replace EntityThrowable to EntityThrowableByPlayer
public class EntityBouncingProjectile extends EntityThrowableByPlayer
{
	public int damage;
	public EntityLivingBase thrower;
	protected int bounces;

	public EntityBouncingProjectile(World par1)
	{
		super(par1);
	}

	public EntityBouncingProjectile(World par1, EntityLivingBase par2, int par3)
	{
		super(par1, par2);
		this.damage = par3;
		this.thrower = par2;
	}

	public EntityBouncingProjectile(World par1, double par2, double par4, double par6)
	{
		super(par1, par2, par4, par6);
		this.setVelocity(this.motionX * 0.01D, this.motionY * 0.01D, this.motionZ * 0.01D);
	}

	@Override
	protected void onImpact(MovingObjectPosition mop)
	{
		// TODO gamerforEA add condition [3]
		if (mop.entityHit != null && mop.entityHit != this.thrower && !this.fake.cantDamage(mop.entityHit))
		{
			mop.entityHit.attackEntityFrom(Util.causeArcanaDamage(this, this.thrower), this.damage);

			if (!this.worldObj.isRemote)
				this.setDead();
			return;
		}

		if (mop.sideHit == 0 || mop.sideHit == 1)
			this.motionY *= -1.0D;
		else if (mop.sideHit == 2 || mop.sideHit == 3)
			this.motionZ *= -1.0D;
		else if (mop.sideHit == 4 || mop.sideHit == 5)
			this.motionX *= -1.0D;
		this.bounces++;

		if (this.bounces == 7)
			this.setDead();
	}
}