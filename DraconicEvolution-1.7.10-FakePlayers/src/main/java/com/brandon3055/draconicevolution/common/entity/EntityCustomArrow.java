package com.brandon3055.draconicevolution.common.entity;

import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.client.handler.ParticleHandler;
import com.brandon3055.draconicevolution.client.render.particle.Particles;
import com.brandon3055.draconicevolution.common.handler.ConfigHandler;
import com.brandon3055.draconicevolution.common.items.weapons.BowHandler;
import com.brandon3055.draconicevolution.common.network.GenericParticlePacket;
import com.gamerforea.draconicevolution.EventConfig;
import com.gamerforea.draconicevolution.ModUtils;
import com.gamerforea.eventhelper.fake.FakePlayerContainer;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityDragonPart;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.S2BPacketChangeGameState;
import net.minecraft.util.*;
import net.minecraft.world.World;

import java.util.List;

public class EntityCustomArrow extends EntityArrow
{
	private int blockX = -1;
	private int blockY = -1;
	private int blockZ = -1;
	private Block blockHit;
	private int inData;
	private boolean inGround;
	private int ticksInGround;
	private int ticksInAir;

	//Arrow Properties
	public boolean ignorSpeed = false; //old
	public boolean explosive = false; //old
	public int knockbackStrength = 0;

	public BowHandler.BowProperties bowProperties = new BowHandler.BowProperties();

	// TODO gamerforEA code start
	public final FakePlayerContainer fake = ModUtils.NEXUS_FACTORY.wrapFake(this);
	// TODO gamerforEA code end

	public EntityCustomArrow(World p_i1753_1_)
	{
		super(p_i1753_1_);
		this.renderDistanceWeight = 40;
	}

	public EntityCustomArrow(World p_i1754_1_, double p_i1754_2_, double p_i1754_4_, double p_i1754_6_)
	{
		super(p_i1754_1_, p_i1754_2_, p_i1754_4_, p_i1754_6_);
		this.renderDistanceWeight = 40;
	}

	public EntityCustomArrow(World p_i1755_1_, EntityLivingBase shooter, EntityLivingBase p_i1755_3_, float p_i1755_4_, float p_i1755_5_)
	{
		super(p_i1755_1_, shooter, p_i1755_3_, p_i1755_4_, p_i1755_5_);
		this.renderDistanceWeight = 40;

		// TODO gamerforEA code start
		this.fake.setRealPlayer(shooter);
		// TODO gamerforEA code end
	}

	public EntityCustomArrow(World par1World, EntityLivingBase shooter, float velocity)
	{
		super(par1World, shooter, velocity);
		this.renderDistanceWeight = 40;

		// TODO gamerforEA code start
		this.fake.setRealPlayer(shooter);
		// TODO gamerforEA code end
	}

	@Override
	protected void entityInit()
	{
		super.entityInit();
		this.dataWatcher.addObject(17, (byte) 0);
		//this.dataWatcher.updateObject(17, Byte.valueOf((byte)(bowProperties.energyBolt ? 1 : 0)));
	}

	@Override
	public void setThrowableHeading(double par1, double par3, double par5, float par7, float par8)
	{
		float f2 = MathHelper.sqrt_double(par1 * par1 + par3 * par3 + par5 * par5);
		par1 /= f2;
		par3 /= f2;
		par5 /= f2;
		par1 += this.rand.nextGaussian() * (this.rand.nextBoolean() ? -1 : 1) * 0.0007499999832361937D * par8;
		par3 += this.rand.nextGaussian() * (this.rand.nextBoolean() ? -1 : 1) * 0.0007499999832361937D * par8;
		par5 += this.rand.nextGaussian() * (this.rand.nextBoolean() ? -1 : 1) * 0.0007499999832361937D * par8;
		par1 *= par7;
		par3 *= par7;
		par5 *= par7;
		this.motionX = par1;
		this.motionY = par3;
		this.motionZ = par5;
		float f3 = MathHelper.sqrt_double(par1 * par1 + par5 * par5);
		this.prevRotationYaw = this.rotationYaw = (float) (Math.atan2(par1, par5) * 180.0D / Math.PI);
		this.prevRotationPitch = this.rotationPitch = (float) (Math.atan2(par3, f3) * 180.0D / Math.PI);
		this.ticksInGround = 0;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void onUpdate()
	{
		//region Entity Update And motion
		this.onEntityUpdate();
		if (this.worldObj.isRemote)
			this.bowProperties.energyBolt = this.dataWatcher.getWatchableObjectByte(17) == 1;
		else
			this.dataWatcher.updateObject(17, (byte) (this.bowProperties.energyBolt ? 1 : 0));

		if (this.prevRotationPitch == 0.0F && this.prevRotationYaw == 0.0F)
		{
			float f = MathHelper.sqrt_double(this.motionX * this.motionX + this.motionZ * this.motionZ);
			this.prevRotationYaw = this.rotationYaw = (float) (Math.atan2(this.motionX, this.motionZ) * 180.0D / Math.PI);
			this.prevRotationPitch = this.rotationPitch = (float) (Math.atan2(this.motionY, f) * 180.0D / Math.PI);
		}

		if (this.arrowShake > 0)
			--this.arrowShake;

		//region Block Collision Detection
		Block block = this.worldObj.getBlock(this.blockX, this.blockY, this.blockZ);

		if (block.getMaterial() != Material.air)
		{
			this.onHitAnything();
			block.setBlockBoundsBasedOnState(this.worldObj, this.blockX, this.blockY, this.blockZ);
			AxisAlignedBB axisalignedbb = block.getCollisionBoundingBoxFromPool(this.worldObj, this.blockX, this.blockY, this.blockZ);

			if (axisalignedbb != null && axisalignedbb.isVecInside(Vec3.createVectorHelper(this.posX, this.posY, this.posZ)))
				this.inGround = true;
		}
		//endregion

		if (this.inGround)
		{
			int j = this.worldObj.getBlockMetadata(this.blockX, this.blockY, this.blockZ);

			if (block == this.blockHit && j == this.inData)
			{
				++this.ticksInGround;

				if (this.ticksInGround == 1200 || this.bowProperties.energyBolt)
					this.setDead();
			}
			else
			{
				this.inGround = false;
				this.motionX *= this.rand.nextFloat() * 0.2F;
				this.motionY *= this.rand.nextFloat() * 0.2F;
				this.motionZ *= this.rand.nextFloat() * 0.2F;
				this.ticksInGround = 0;
				this.ticksInAir = 0;
			}
		}
		else
		{
			//region Detect Entity Hit
			++this.ticksInAir;
			Vec3 vec31 = Vec3.createVectorHelper(this.posX, this.posY, this.posZ);
			Vec3 vec3 = Vec3.createVectorHelper(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);
			MovingObjectPosition mop = this.worldObj.func_147447_a(vec31, vec3, false, true, false);
			vec31 = Vec3.createVectorHelper(this.posX, this.posY, this.posZ);
			vec3 = Vec3.createVectorHelper(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);

			if (mop != null)
				vec3 = Vec3.createVectorHelper(mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord);

			Entity entity = null;
			List list = this.worldObj.getEntitiesWithinAABBExcludingEntity(this, this.boundingBox.addCoord(this.motionX, this.motionY, this.motionZ).expand(1.0D, 1.0D, 1.0D));
			double d0 = 0.0D;
			int i;
			float f1;

			for (i = 0; i < list.size(); ++i)
			{
				Entity entity1 = (Entity) list.get(i);

				if (entity1.canBeCollidedWith() && (entity1 != this.shootingEntity || this.ticksInAir >= 5))
				{
					f1 = 0.3F;
					AxisAlignedBB axisalignedbb1 = entity1.boundingBox.expand(f1, f1, f1);
					MovingObjectPosition movingobjectposition1 = axisalignedbb1.calculateIntercept(vec31, vec3);

					if (movingobjectposition1 != null)
					{
						double d1 = vec31.distanceTo(movingobjectposition1.hitVec);

						if (d1 < d0 || d0 == 0.0D)
						{
							entity = entity1;
							d0 = d1;
						}
					}
				}
			}

			if (entity != null)
				mop = new MovingObjectPosition(entity);

			if (mop != null && mop.entityHit != null && mop.entityHit instanceof EntityPlayer)
			{
				EntityPlayer entityplayer = (EntityPlayer) mop.entityHit;

				if (entityplayer.capabilities.disableDamage || this.shootingEntity instanceof EntityPlayer && !((EntityPlayer) this.shootingEntity).canAttackPlayer(entityplayer))
					mop = null;
			}
			//endregion

			float velocity;
			float f4;

			//region Process Entity Hit
			if (mop != null)
				if (mop.entityHit != null)
				{
					this.onHitAnything();
					if (this.isDead)
						return;

					// TODO gamerforEA code start
					if (this.fake.cantDamage(mop.entityHit))
					{
						this.setDead();
						return;
					}
					// TODO gamerforEA code end

					int actualDamage;
					//Calculate Damage
					velocity = MathHelper.sqrt_double(this.motionX * this.motionX + this.motionY * this.motionY + this.motionZ * this.motionZ);
					actualDamage = MathHelper.ceiling_double_int(velocity * this.bowProperties.arrowDamage);

					if (this.bowProperties.energyBolt)
						actualDamage *= 1.1F;

					if (this.getIsCritical())
						actualDamage += this.rand.nextInt(actualDamage / 2 + 2);

					if (this.isBurning() && !(mop.entityHit instanceof EntityEnderman))
						mop.entityHit.setFire(5);

					// TODO gamerforEA code start
					if (EventConfig.ignoreHurtResistantTime)
					// TODO gamerforEA code end
					{
						if (this.bowProperties.energyBolt)
							mop.entityHit.hurtResistantTime = 0;
						if (mop.entityHit instanceof EntityDragonPart && ((EntityDragonPart) mop.entityHit).entityDragonObj instanceof EntityDragon && this.bowProperties.energyBolt)
							((EntityDragon) ((EntityDragonPart) mop.entityHit).entityDragonObj).hurtResistantTime = 0;
					}

					if (mop.entityHit.attackEntityFrom(this.getDamageSource(), actualDamage))
					{
						if (mop.entityHit instanceof EntityLivingBase)
						{
							EntityLivingBase entitylivingbase = (EntityLivingBase) mop.entityHit;

							if (!this.worldObj.isRemote)
								entitylivingbase.setArrowCountInEntity(entitylivingbase.getArrowCountInEntity() + 1);

							if (this.knockbackStrength > 0)
							{
								f4 = MathHelper.sqrt_double(this.motionX * this.motionX + this.motionZ * this.motionZ);

								if (f4 > 0.0F)
									mop.entityHit.addVelocity(this.motionX * this.knockbackStrength * 0.6000000238418579D / f4, 0.1D, this.motionZ * this.knockbackStrength * 0.6000000238418579D / f4);
							}

							if (this.shootingEntity != null && this.shootingEntity instanceof EntityLivingBase)
							{
								EnchantmentHelper.func_151384_a(entitylivingbase, this.shootingEntity);
								EnchantmentHelper.func_151385_b((EntityLivingBase) this.shootingEntity, entitylivingbase);
							}

							if (this.shootingEntity != null && mop.entityHit != this.shootingEntity && mop.entityHit instanceof EntityPlayer && this.shootingEntity instanceof EntityPlayerMP)
								((EntityPlayerMP) this.shootingEntity).playerNetServerHandler.sendPacket(new S2BPacketChangeGameState(6, 0.0F));
						}

						this.playSound("random.bowhit", 1.0F, 1.2F / (this.rand.nextFloat() * 0.2F + 0.9F));
						this.setDead();

					}
					else if (!(this.worldObj.isRemote && this.ticksInAir < 5))
					{ //Fix arrow wobble on fire due to client side collision with shooting entity
						this.motionX *= -0.10000000149011612D;
						this.motionY *= -0.10000000149011612D;
						this.motionZ *= -0.10000000149011612D;
						this.rotationYaw += 180.0F;
						this.prevRotationYaw += 180.0F;
						this.ticksInAir = 0;
					}
				}
				else
				{
					this.blockX = mop.blockX;
					this.blockY = mop.blockY;
					this.blockZ = mop.blockZ;
					block = this.worldObj.getBlock(this.blockX, this.blockY, this.blockZ);
					this.blockHit = block;
					this.inData = this.worldObj.getBlockMetadata(this.blockX, this.blockY, this.blockZ);
					this.motionX = (float) (mop.hitVec.xCoord - this.posX);
					this.motionY = (float) (mop.hitVec.yCoord - this.posY);
					this.motionZ = (float) (mop.hitVec.zCoord - this.posZ);
					velocity = MathHelper.sqrt_double(this.motionX * this.motionX + this.motionY * this.motionY + this.motionZ * this.motionZ);
					this.posX -= this.motionX / velocity * 0.05000000074505806D;
					this.posY -= this.motionY / velocity * 0.05000000074505806D;
					this.posZ -= this.motionZ / velocity * 0.05000000074505806D;
					this.playSound("random.bowhit", 1.0F, 1.2F / (this.rand.nextFloat() * 0.2F + 0.9F));
					this.inGround = true;
					this.arrowShake = 7;

					if (this.blockHit.getMaterial() != Material.air)
						this.blockHit.onEntityCollidedWithBlock(this.worldObj, this.blockX, this.blockY, this.blockZ, this);
				}

			//region Motion
			if ((this.getIsCritical() || this.bowProperties.energyBolt) && this.worldObj.isRemote)
				for (i = 0; i < 4; ++i)
				{
					if (this.bowProperties.energyBolt)
						this.spawnArrowParticles();
					else
						this.worldObj.spawnParticle("crit", this.posX + this.motionX * i / 4.0D, this.posY + this.motionY * i / 4.0D, this.posZ + this.motionZ * i / 4.0D, -this.motionX, -this.motionY + 0.2D, -this.motionZ);
				}

			this.posX += this.motionX;
			this.posY += this.motionY;
			this.posZ += this.motionZ;
			velocity = MathHelper.sqrt_double(this.motionX * this.motionX + this.motionZ * this.motionZ);
			this.rotationYaw = (float) (Math.atan2(this.motionX, this.motionZ) * 180.0D / Math.PI);

			for (this.rotationPitch = (float) (Math.atan2(this.motionY, velocity) * 180.0D / Math.PI); this.rotationPitch - this.prevRotationPitch < -180.0F; this.prevRotationPitch -= 360.0F)
			{
			}

			while (this.rotationPitch - this.prevRotationPitch >= 180.0F)
			{
				this.prevRotationPitch += 360.0F;
			}

			while (this.rotationYaw - this.prevRotationYaw < -180.0F)
			{
				this.prevRotationYaw -= 360.0F;
			}

			while (this.rotationYaw - this.prevRotationYaw >= 180.0F)
			{
				this.prevRotationYaw += 360.0F;
			}

			this.rotationPitch = this.prevRotationPitch + (this.rotationPitch - this.prevRotationPitch) * 0.2F;
			this.rotationYaw = this.prevRotationYaw + (this.rotationYaw - this.prevRotationYaw) * 0.2F;
			float f3 = 0.99F;
			f1 = this.bowProperties.energyBolt ? 0.025F : 0.05F;

			if (this.isInWater())
			{
				for (int l = 0; l < 4; ++l)
				{
					f4 = 0.25F;
					this.worldObj.spawnParticle("bubble", this.posX - this.motionX * f4, this.posY - this.motionY * f4, this.posZ - this.motionZ * f4, this.motionX, this.motionY, this.motionZ);
				}

				f3 = 0.8F;
			}

			if (this.isWet())
				this.extinguish();

			this.motionX *= f3;
			this.motionY *= f3;
			this.motionZ *= f3;
			this.motionY -= f1;
			this.setPosition(this.posX, this.posY, this.posZ);
			this.func_145775_I();
			//endregion
		}
	}

	@SideOnly(Side.CLIENT)
	private void spawnArrowParticles()
	{
		Particles.ArrowParticle particle = new Particles.ArrowParticle(this.worldObj, this.posX - 0.25 + this.rand.nextDouble() * 0.5, this.posY + this.rand.nextDouble() * 0.5, this.posZ - 0.25 + this.rand.nextDouble() * 0.5, 0xff6000, 0.2F + this.rand.nextFloat() * 0.5f);
		double mm = 0.2;
		particle.motionX = (this.rand.nextDouble() - 0.5) * mm;
		particle.motionY = (this.rand.nextDouble() - 0.5) * mm;
		particle.motionZ = (this.rand.nextDouble() - 0.5) * mm;
		ParticleHandler.spawnCustomParticle(particle, 64);
	}

	@Override
	public void writeEntityToNBT(NBTTagCompound compound)
	{
		compound.setShort("xTile", (short) this.blockX);
		compound.setShort("yTile", (short) this.blockY);
		compound.setShort("zTile", (short) this.blockZ);
		compound.setShort("life", (short) this.ticksInGround);
		compound.setByte("inTile", (byte) Block.getIdFromBlock(this.blockHit));
		compound.setByte("inData", (byte) this.inData);
		compound.setByte("shake", (byte) this.arrowShake);
		compound.setByte("inGround", (byte) (this.inGround ? 1 : 0));
		compound.setByte("pickup", (byte) this.canBePickedUp);
		if (this.bowProperties != null)
			this.bowProperties.writeToNBT(compound);

		// TODO gamerforEA code start
		this.fake.writeToNBT(compound);
		// TODO gamerforEA code end
	}

	@Override
	public void readEntityFromNBT(NBTTagCompound compound)
	{
		this.blockX = compound.getShort("xTile");
		this.blockY = compound.getShort("yTile");
		this.blockZ = compound.getShort("zTile");
		this.ticksInGround = compound.getShort("life");
		this.blockHit = Block.getBlockById(compound.getByte("inTile") & 255);
		this.inData = compound.getByte("inData") & 255;
		this.arrowShake = compound.getByte("shake") & 255;
		this.inGround = compound.getByte("inGround") == 1;

		if (compound.hasKey("pickup", 99))
			this.canBePickedUp = compound.getByte("pickup");
		else if (compound.hasKey("player", 99))
			this.canBePickedUp = compound.getBoolean("player") ? 1 : 0;

		if (this.bowProperties != null)
			this.bowProperties.readFromNBT(compound);

		// TODO gamerforEA code start
		this.fake.readFromNBT(compound);
		// TODO gamerforEA code end
	}

	@Override
	public void onCollideWithPlayer(EntityPlayer par1EntityPlayer)
	{
		if (!this.worldObj.isRemote && this.inGround && this.arrowShake <= 0)
		{

			boolean flag = this.canBePickedUp == 1 || this.canBePickedUp == 2 && par1EntityPlayer.capabilities.isCreativeMode;

			if (this.canBePickedUp == 1 && !par1EntityPlayer.inventory.addItemStackToInventory(new ItemStack(Items.arrow, 1)))
				flag = false;

			if (flag)
			{
				this.playSound("random.pop", 0.2F, ((this.rand.nextFloat() - this.rand.nextFloat()) * 0.7F + 1.0F) * 2.0F);
				par1EntityPlayer.onItemPickup(this, 1);
				this.setDead();
			}
		}
	}

	public void onHitEntityLiving(EntityLivingBase entityLivingBase)
	{
	}

	public void onHitAnything()
	{
		// TODO gamerforEA add condition [3]
		if (this.bowProperties.explosionPower > 0 && !this.worldObj.isRemote && EventConfig.enableDraconicBowExplosion)
		{
			// TODO gamerforEA use ExplosionByPlayer
			this.fake.createExplosion(this, this.prevPosX, this.prevPosY, this.prevPosZ, this.bowProperties.explosionPower, ConfigHandler.bowBlockDamage);

			this.setDead();
		}
		if (this.bowProperties.shockWavePower > 0 && !this.worldObj.isRemote)
		{
			DraconicEvolution.network.sendToAllAround(new GenericParticlePacket(GenericParticlePacket.ARROW_SHOCK_WAVE, this.posX, this.posY, this.posZ, (int) (this.bowProperties.shockWavePower * 100)), new NetworkRegistry.TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 256));
			this.worldObj.playSoundEffect(this.posX, this.posY, this.posZ, "random.explode", 4.0F, (1.0F + (this.worldObj.rand.nextFloat() - this.worldObj.rand.nextFloat()) * 0.2F) * 0.7F);

			double range = (double) this.bowProperties.shockWavePower + 5;
			List<Entity> list = this.worldObj.getEntitiesWithinAABB(Entity.class, this.boundingBox.expand(range, range, range));

			float damage = 40F * this.bowProperties.shockWavePower;

			for (Entity e : list)
			{
				if (e instanceof EntityLivingBase)
				{
					Entity entity = e;
					float distanceModifier = 1F - entity.getDistanceToEntity(this) / this.bowProperties.shockWavePower;

					if (e instanceof EntityDragon)
					{
						entity = ((EntityDragon) entity).dragonPartBody;
						distanceModifier = 1F - entity.getDistanceToEntity(this) / (this.bowProperties.shockWavePower * 4);
					}

					if (distanceModifier > 0)
					{
						// TODO gamerforEA code start
						if (this.fake.cantDamage(entity))
							continue;
						// TODO gamerforEA code end

						entity.attackEntityFrom(this.getDamageSource(), distanceModifier * damage);
					}
				}
			}

			this.setDead();
		}
	}

	private DamageSource getDamageSource()
	{
		if (this.bowProperties.energyBolt)
			return new EntityDamageSourceIndirect("customArrowEnergy", this, this.shootingEntity != null ? this.shootingEntity : this).setProjectile().setDamageIsAbsolute();
		else
			return DamageSource.causeArrowDamage(this, this.shootingEntity != null ? this.shootingEntity : this);
	}
}
