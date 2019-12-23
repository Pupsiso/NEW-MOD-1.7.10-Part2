package net.divinerpg.entities.iceika.projectile;

import com.gamerforea.divinerpg.EntityThrowableByPlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import java.util.List;

// TODO gamerforEA replace EntityThrowable to EntityThrowableByPlayer
public class EntitySerenadeOfIce extends EntityThrowableByPlayer
{
	int age;

	public EntitySerenadeOfIce(World var1)
	{
		super(var1);
	}

	public EntitySerenadeOfIce(World var1, EntityLivingBase e)
	{
		super(var1, e);
		this.motionX *= 3;
		this.motionY *= 3;
		this.motionZ *= 3;
	}

	public EntitySerenadeOfIce(World var1, double var2, double var4, double var6)
	{
		super(var1, var2, var4, var6);
	}

	@Override
	public float getGravityVelocity()
	{
		return 0;
	}

	@Override
	public void onUpdate()
	{
		super.onUpdate();
		this.age++;
		if (!this.worldObj.isRemote && this.age > 35)
			this.setDead();
	}

	@Override
	protected void onImpact(MovingObjectPosition pos)
	{
		if (pos.entityHit != null)
		{
			List<EntityLivingBase> entities = this.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, pos.entityHit.boundingBox.expand(3, 3, 3));
			for (EntityLivingBase e : entities)
			{
				// TODO gamerforEA add condition [2]
				if (e != this.getThrower() && !this.fake.cantDamage(e))
					e.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 100, 3, true));
			}
		}

		if (!this.worldObj.isRemote)
			this.setDead();
	}
}
