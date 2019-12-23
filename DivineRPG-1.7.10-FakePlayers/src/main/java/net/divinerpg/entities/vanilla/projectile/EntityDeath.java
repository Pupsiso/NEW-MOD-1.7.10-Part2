package net.divinerpg.entities.vanilla.projectile;

import com.gamerforea.divinerpg.EntityThrowableByPlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

// TODO gamerforEA replace EntityThrowable to EntityThrowableByPlayer
public class EntityDeath extends EntityThrowableByPlayer
{
	public EntityDeath(World var1)
	{
		super(var1);
	}

	public EntityDeath(World var1, EntityLivingBase e)
	{
		super(var1, e);
	}

	public EntityDeath(World var1, double var2, double var4, double var6)
	{
		super(var1, var2, var4, var6);
	}

	@Override
	protected void onImpact(MovingObjectPosition mop)
	{
		// TODO gamerforEA add condition [2]
		if (mop.entityHit != null && !this.fake.cantDamage(mop.entityHit))
		{
			mop.entityHit.attackEntityFrom(DamageSource.causeThrownDamage(this, this.getThrower()), 14.0F);
			if (mop.entityHit instanceof EntityLivingBase)
				((EntityLivingBase) mop.entityHit).addPotionEffect(new PotionEffect(Potion.poison.id, 45, 3));
		}

		if (!this.worldObj.isRemote)
			this.setDead();
	}
}
