package net.divinerpg.entities.vethea.projectile;

import com.gamerforea.divinerpg.EntityThrowableByPlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

// TODO gamerforEA replace EntityThrowable to EntityThrowableByPlayer
public class EntityVetheanCannonEversight extends EntityThrowableByPlayer
{
	public int damage;

	public EntityVetheanCannonEversight(World par1)
	{
		super(par1);
	}

	public EntityVetheanCannonEversight(World par1, EntityLivingBase par2, int par3)
	{
		super(par1, par2);
		this.damage = par3;
	}

	public EntityVetheanCannonEversight(World par1, double par2, double par4, double par6)
	{
		super(par1, par2, par4, par6);
	}

	@Override
	protected void onImpact(MovingObjectPosition mop)
	{
		// TODO gamerforEA add condition [2]
		if (mop.entityHit != null && !this.fake.cantDamage(mop.entityHit))
			mop.entityHit.attackEntityFrom(DamageSource.causeThrownDamage(this, this.getThrower()), this.damage);

		if (!this.worldObj.isRemote)
			this.setDead();
	}
}
