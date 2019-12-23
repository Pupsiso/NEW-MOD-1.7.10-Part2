package net.divinerpg.entities.iceika.projectile;

import com.gamerforea.divinerpg.EntityThrowableByPlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityBlaze;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

// TODO gamerforEA replace EntityThrowable to EntityThrowableByPlayer
public class EntitySnowflakeShuriken extends EntityThrowableByPlayer
{
	public EntitySnowflakeShuriken(World var1)
	{
		super(var1);
	}

	public EntitySnowflakeShuriken(World var1, EntityLivingBase var3)
	{
		super(var1, var3);
	}

	public EntitySnowflakeShuriken(World var1, double var2, double var4, double var6)
	{
		super(var1, var2, var4, var6);
	}

	@Override
	protected void onImpact(MovingObjectPosition mop)
	{
		// TODO gamerforEA add condition [2]
		if (mop.entityHit != null && !this.fake.cantDamage(mop.entityHit))
		{
			byte var2 = 7;

			if (mop.entityHit instanceof EntityBlaze)
				var2 = 7;

			mop.entityHit.attackEntityFrom(DamageSource.causeThrownDamage(this, this.getThrower()), var2);
		}

		if (!this.worldObj.isRemote)
			this.setDead();
	}
}