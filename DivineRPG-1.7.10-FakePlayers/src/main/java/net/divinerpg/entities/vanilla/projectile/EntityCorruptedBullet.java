package net.divinerpg.entities.vanilla.projectile;

import com.gamerforea.divinerpg.EntityThrowableByPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

// TODO gamerforEA replace EntityThrowable to EntityThrowableByPlayer
public class EntityCorruptedBullet extends EntityThrowableByPlayer
{
	public static float damage = 10.0F;
	public List<Entity> toExcludeList = new ArrayList<Entity>();

	public EntityCorruptedBullet(World var1)
	{
		super(var1);
	}

	public EntityCorruptedBullet(World var1, EntityLivingBase var2)
	{
		super(var1, var2);
	}

	public EntityCorruptedBullet(World var1, double var2, double var4, double var6)
	{
		super(var1, var2, var4, var6);
	}

	@Override
	protected void onImpact(MovingObjectPosition mop)
	{
		if (mop.entityHit != null)
		{
			// TODO gamerforEA add condition [2]
			if (!this.toExcludeList.contains(mop.entityHit) && !this.fake.cantDamage(mop.entityHit))
				mop.entityHit.attackEntityFrom(DamageSource.causeThrownDamage(this, this.getThrower()), damage);

			List<Entity> surrounding = this.worldObj.getEntitiesWithinAABBExcludingEntity(this, this.boundingBox.expand(1, 1, 1));
			for (Entity e : surrounding)
			{
				if (e instanceof EntityCorruptedBullet)
					((EntityCorruptedBullet) e).toExcludeList.add(mop.entityHit);
			}
		}

		if (!this.worldObj.isRemote)
		{
			this.setDead();
		}
	}
}