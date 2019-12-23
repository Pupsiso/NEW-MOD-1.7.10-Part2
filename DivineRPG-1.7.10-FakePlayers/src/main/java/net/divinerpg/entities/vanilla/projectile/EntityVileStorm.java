package net.divinerpg.entities.vanilla.projectile;

import com.gamerforea.divinerpg.EntityThrowableByPlayer;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

// TODO gamerforEA replace EntityThrowable to EntityThrowableByPlayer
public class EntityVileStorm extends EntityThrowableByPlayer
{
	public EntityVileStorm(World var1)
	{
		super(var1);
	}

	public EntityVileStorm(World var1, EntityLivingBase var3)
	{
		super(var1, var3);
	}

	@Override
	protected void onImpact(MovingObjectPosition mop)
	{
		if (mop.entityHit != null)
		{
			byte var2 = 4;

			// TODO gamerforEA add condition [2]
			if (mop.entityHit instanceof EntityLiving && !this.fake.cantDamage(mop.entityHit))
			{
				mop.entityHit.attackEntityFrom(DamageSource.causeThrownDamage(this, this.getThrower()), var2);
				((EntityLivingBase) mop.entityHit).addPotionEffect(new PotionEffect(Potion.poison.id, 45, 3));
			}
		}

		if (!this.worldObj.isRemote)
			this.setDead();
	}
}
