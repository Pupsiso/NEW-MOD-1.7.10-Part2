package net.divinerpg.entities.arcana.projectile;

import com.gamerforea.divinerpg.ExplosionByPlayer;
import net.divinerpg.entities.base.EntityHeatSeekingProjectile;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class EntityMerikMissile extends EntityHeatSeekingProjectile
{
	public EntityMerikMissile(World world)
	{
		super(world);
	}

	public EntityMerikMissile(World world, EntityLivingBase shooter)
	{
		super(world, shooter);
	}

	@Override
	protected void onImpact(MovingObjectPosition mop)
	{
		// TODO gamerforEA add condition [2]
		if (mop.entityHit != null && !this.fake.cantDamage(mop.entityHit))
			mop.entityHit.attackEntityFrom(DamageSource.causeThrownDamage(this, this.getThrower()), 22);

		if (!this.worldObj.isRemote)
		{
			// TODO gamerforEA use ExplosionByPlayer
			ExplosionByPlayer.createExplosion(this.fake.get(), this.worldObj, this, this.posX, this.posY, this.posZ, 2, false);

			this.setDead();
		}
	}
}
