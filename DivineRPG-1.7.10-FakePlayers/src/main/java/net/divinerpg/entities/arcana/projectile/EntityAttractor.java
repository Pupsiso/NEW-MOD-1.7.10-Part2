package net.divinerpg.entities.arcana.projectile;

import com.gamerforea.divinerpg.EntityThrowableByPlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

// TODO gamerforEA replace EntityThrowable to EntityThrowableByPlayer
public class EntityAttractor extends EntityThrowableByPlayer
{
	int age;

	public EntityAttractor(World w)
	{
		super(w);
	}

	public EntityAttractor(World w, EntityLivingBase e)
	{
		super(w, e);
		this.motionX *= 3;
		this.motionY *= 3;
		this.motionZ *= 3;
	}

	@Override
	protected void onImpact(MovingObjectPosition pos)
	{
		// TODO gamerforEA add condition [2]
		if (pos.entityHit != null && this.getThrower() != null && !this.fake.cantDamage(pos.entityHit))
		{
			double xDist = (this.getThrower().posX - pos.entityHit.posX) / 5, yDist = (this.getThrower().posY - pos.entityHit.posY) / 5, zDist = (this.getThrower().posZ - pos.entityHit.posZ) / 5;
			pos.entityHit.addVelocity(xDist, yDist, zDist);
		}
		this.setDead();
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
		if (this.age > 18)
			this.setDead();//I'm an Adult!
	}
}
