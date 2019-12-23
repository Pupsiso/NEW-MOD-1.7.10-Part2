package net.divinerpg.entities.arcana.projectile;

import com.gamerforea.divinerpg.EntityThrowableByPlayer;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import java.awt.*;

// TODO gamerforEA replace EntityThrowable to EntityThrowableByPlayer
public class EntityStar extends EntityThrowableByPlayer
{
	private Color colour;

	public EntityStar(World par1World)
	{
		super(par1World);
		this.motionX = this.rand.nextGaussian() * 0.05;
		this.motionZ = this.rand.nextGaussian() * 0.05;
		this.motionY = -0.5;
		this.colour = new Color(this.rand.nextInt(255), this.rand.nextInt(255), this.rand.nextInt(255));
	}

	public EntityStar(World par1World, EntityLiving par2EntityLiving)
	{
		super(par1World, par2EntityLiving);
		this.motionX = this.rand.nextGaussian() * 0.05;
		this.motionZ = this.rand.nextGaussian() * 0.05;
		this.motionY = -0.5;
		this.colour = new Color(this.rand.nextInt(255), this.rand.nextInt(255), this.rand.nextInt(255));
	}

	public EntityStar(World par1World, double par2, double par4, double par6)
	{
		super(par1World, par2, par4, par6);
		this.motionX = this.rand.nextGaussian() * 0.05;
		this.motionZ = this.rand.nextGaussian() * 0.05;
		this.motionY = -0.5;
		this.colour = new Color(this.rand.nextInt(255), this.rand.nextInt(255), this.rand.nextInt(255));
	}

	public Color getColour()
	{
		return this.colour;
	}

	@Override
	protected void onImpact(MovingObjectPosition mop)
	{
		// TODO gamerforEA add condition [2]
		if (mop.entityHit != null && !this.fake.cantDamage(mop.entityHit))
			mop.entityHit.attackEntityFrom(DamageSource.causeThrownDamage(this, this.getThrower()), 20);

		if (!this.worldObj.isRemote)
			this.setDead();
	}
}