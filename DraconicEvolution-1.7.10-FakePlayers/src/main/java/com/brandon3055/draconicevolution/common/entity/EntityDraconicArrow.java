package com.brandon3055.draconicevolution.common.entity;

import com.brandon3055.draconicevolution.common.handler.ConfigHandler;
import com.gamerforea.draconicevolution.EventConfig;
import com.gamerforea.draconicevolution.ModUtils;
import com.gamerforea.eventhelper.fake.FakePlayerContainer;
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

public class EntityDraconicArrow extends EntityArrow
{
	private int blockX = -1;
	private int blockY = -1;
	private int blockZ = -1;
	private Block blockHit;
	private int inData;
	private boolean inGround;
	private int ticksInGround;
	private int ticksInAir;
	private double damage = 2.0D;
	/**
	 * The amount of knockback an arrow applies when it hits a mob.
	 */
	private int knockbackStrength;
	public boolean ignorSpeed = false;
	public boolean explosive = false;

	// TODO gamerforEA code start
	public final FakePlayerContainer fake = ModUtils.NEXUS_FACTORY.wrapFake(this);
	// TODO gamerforEA code end

	public EntityDraconicArrow(World p_i1753_1_)
	{
		super(p_i1753_1_);
	}

	public EntityDraconicArrow(World p_i1754_1_, double p_i1754_2_, double p_i1754_4_, double p_i1754_6_)
	{
		super(p_i1754_1_, p_i1754_2_, p_i1754_4_, p_i1754_6_);
	}

	public EntityDraconicArrow(World p_i1755_1_, EntityLivingBase shooter, EntityLivingBase p_i1755_3_, float p_i1755_4_, float p_i1755_5_)
	{
		super(p_i1755_1_, shooter, p_i1755_3_, p_i1755_4_, p_i1755_5_);

		// TODO gamerforEA code start
		this.fake.setRealPlayer(shooter);
		// TODO gamerforEA code end
	}

	public EntityDraconicArrow(World par1World, EntityLivingBase shooter, float par3)
	{
		super(par1World, shooter, par3);

		// TODO gamerforEA code start
		this.fake.setRealPlayer(shooter);
		// TODO gamerforEA code end
	}

	@Override
	protected void entityInit()
	{
		super.entityInit();
		//this.dataWatcher.addObject(16, Byte.valueOf((byte) 0));
	}

	@Override
	public void setThrowableHeading(double par1, double par3, double par5, float par7, float par8)
	{
		float f2 = MathHelper.sqrt_double(par1 * par1 + par3 * par3 + par5 * par5);
		par1 /= f2;
		par3 /= f2;
		par5 /= f2;
		par1 += this.rand.nextGaussian() * (this.rand.nextBoolean() ? -1 : 1) * 0.007499999832361937D * par8;
		par3 += this.rand.nextGaussian() * (this.rand.nextBoolean() ? -1 : 1) * 0.007499999832361937D * par8;
		par5 += this.rand.nextGaussian() * (this.rand.nextBoolean() ? -1 : 1) * 0.007499999832361937D * par8;
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

	@Override
	@SideOnly(Side.CLIENT)
	public void setPositionAndRotation2(double par1, double par3, double par5, float par7, float par8, int par9)
	{
		this.setPosition(par1, par3, par5);
		this.setRotation(par7, par8);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void setVelocity(double par1, double par3, double par5)
	{
		this.motionX = par1;
		this.motionY = par3;
		this.motionZ = par5;

		if (this.prevRotationPitch == 0.0F && this.prevRotationYaw == 0.0F)
		{
			float f = MathHelper.sqrt_double(par1 * par1 + par5 * par5);
			this.prevRotationYaw = this.rotationYaw = (float) (Math.atan2(par1, par5) * 180.0D / Math.PI);
			this.prevRotationPitch = this.rotationPitch = (float) (Math.atan2(par3, f) * 180.0D / Math.PI);
			this.prevRotationPitch = this.rotationPitch;
			this.prevRotationYaw = this.rotationYaw;
			this.setLocationAndAngles(this.posX, this.posY, this.posZ, this.rotationYaw, this.rotationPitch);
			this.ticksInGround = 0;
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void onUpdate()
	{
		//super.onUpdate();
		this.onEntityUpdate();
		if (this.prevRotationPitch == 0.0F && this.prevRotationYaw == 0.0F)
		{
			float f = MathHelper.sqrt_double(this.motionX * this.motionX + this.motionZ * this.motionZ);
			this.prevRotationYaw = this.rotationYaw = (float) (Math.atan2(this.motionX, this.motionZ) * 180.0D / Math.PI);
			this.prevRotationPitch = this.rotationPitch = (float) (Math.atan2(this.motionY, f) * 180.0D / Math.PI);
		}

		Block block = this.worldObj.getBlock(this.blockX, this.blockY, this.blockZ);

		if (block.getMaterial() != Material.air)
		{
			if (this.explosive)
			{
				this.worldObj.createExplosion(this.shootingEntity, this.prevPosX, this.prevPosY, this.prevPosZ, 4, ConfigHandler.bowBlockDamage);
				this.setDead();
				return;
			}
			block.setBlockBoundsBasedOnState(this.worldObj, this.blockX, this.blockY, this.blockZ);
			AxisAlignedBB axisalignedbb = block.getCollisionBoundingBoxFromPool(this.worldObj, this.blockX, this.blockY, this.blockZ);

			if (axisalignedbb != null && axisalignedbb.isVecInside(Vec3.createVectorHelper(this.posX, this.posY, this.posZ)))
				this.inGround = true;
		}

		if (this.arrowShake > 0)
			--this.arrowShake;

		if (this.inGround)
		{
			int j = this.worldObj.getBlockMetadata(this.blockX, this.blockY, this.blockZ);

			if (block == this.blockHit && j == this.inData)
			{
				++this.ticksInGround;

				if (this.ticksInGround == 1200)
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

			if (mop != null && mop.entityHit instanceof EntityPlayer)
			{
				EntityPlayer entityplayer = (EntityPlayer) mop.entityHit;

				if (entityplayer.capabilities.disableDamage || this.shootingEntity instanceof EntityPlayer && !((EntityPlayer) this.shootingEntity).canAttackPlayer(entityplayer))
					mop = null;
			}

			float f2;
			float f4;

			if (mop != null)
				if (mop.entityHit != null)
				{
					// TODO gamerforEA code start
					if (this.fake.cantDamage(mop.entityHit))
					{
						this.setDead();
						return;
					}
					// TODO gamerforEA code end

					if (this.explosive)
					{
						//world.createExplosion(this.shootingEntity, mop.entityHit.posX, mop.entityHit.posY, mop.entityHit.posZ, 6, false);
						//mop.entityHit.hurtResistantTime = 0;

						// TODO gamerforEA use ExplosionByPlayer
						this.fake.createExplosion(this.shootingEntity, mop.entityHit.posX, mop.entityHit.posY, mop.entityHit.posZ, 4, ConfigHandler.bowBlockDamage);

						this.setDead();
					}
					int k;
					if (!this.ignorSpeed)
					{
						f2 = MathHelper.sqrt_double(this.motionX * this.motionX + this.motionY * this.motionY + this.motionZ * this.motionZ);
						k = MathHelper.ceiling_double_int(f2 * this.damage);

						if (this.getIsCritical())
							k += this.rand.nextInt(k / 2 + 2);
					}
					else
					{
						k = (int) this.damage;
						if (this.getIsCritical())
							k += this.rand.nextInt(k / 2 + 2);
					}

					DamageSource damagesource = null;

					if (this.shootingEntity == null)
						damagesource = DamageSource.causeArrowDamage(this, this);
					else
						damagesource = DamageSource.causeArrowDamage(this, this.shootingEntity);
					if (mop.entityHit instanceof EntityEnderman)
						damagesource = DamageSource.magic;

					if (this.isBurning() && !(mop.entityHit instanceof EntityEnderman))
						mop.entityHit.setFire(5);

					// TODO gamerforEA code start
					if (EventConfig.ignoreHurtResistantTime)
					// TODO gamerforEA code end
					{
						mop.entityHit.hurtResistantTime = 0;
						if (mop.entityHit instanceof EntityDragonPart && ((EntityDragonPart) mop.entityHit).entityDragonObj instanceof EntityDragon)
							((EntityDragon) ((EntityDragonPart) mop.entityHit).entityDragonObj).hurtResistantTime = 0;
					}

					if (mop.entityHit.attackEntityFrom(damagesource, k))
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

						// if (!(mop.entityHit instanceof EntityEnderman))
						//{
						this.setDead();
						//}
					}
					else
					{
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
					f2 = MathHelper.sqrt_double(this.motionX * this.motionX + this.motionY * this.motionY + this.motionZ * this.motionZ);
					this.posX -= this.motionX / f2 * 0.05000000074505806D;
					this.posY -= this.motionY / f2 * 0.05000000074505806D;
					this.posZ -= this.motionZ / f2 * 0.05000000074505806D;
					this.playSound("random.bowhit", 1.0F, 1.2F / (this.rand.nextFloat() * 0.2F + 0.9F));
					this.inGround = true;
					this.arrowShake = 7;

					if (this.blockHit.getMaterial() != Material.air)
						this.blockHit.onEntityCollidedWithBlock(this.worldObj, this.blockX, this.blockY, this.blockZ, this);
				}

			if (this.getIsCritical())
				for (i = 0; i < 4; ++i)
				{
					this.worldObj.spawnParticle("crit", this.posX + this.motionX * i / 4.0D, this.posY + this.motionY * i / 4.0D, this.posZ + this.motionZ * i / 4.0D, -this.motionX, -this.motionY + 0.2D, -this.motionZ);
				}

			this.posX += this.motionX;
			this.posY += this.motionY;
			this.posZ += this.motionZ;
			f2 = MathHelper.sqrt_double(this.motionX * this.motionX + this.motionZ * this.motionZ);
			this.rotationYaw = (float) (Math.atan2(this.motionX, this.motionZ) * 180.0D / Math.PI);

			for (this.rotationPitch = (float) (Math.atan2(this.motionY, f2) * 180.0D / Math.PI); this.rotationPitch - this.prevRotationPitch < -180.0F; this.prevRotationPitch -= 360.0F)
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
			f1 = 0.05F;

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
		}
	}

	@Override
	public void writeEntityToNBT(NBTTagCompound nbt)
	{
		nbt.setShort("xTile", (short) this.blockX);
		nbt.setShort("yTile", (short) this.blockY);
		nbt.setShort("zTile", (short) this.blockZ);
		nbt.setShort("life", (short) this.ticksInGround);
		nbt.setByte("inTile", (byte) Block.getIdFromBlock(this.blockHit));
		nbt.setByte("inData", (byte) this.inData);
		nbt.setByte("shake", (byte) this.arrowShake);
		nbt.setByte("inGround", (byte) (this.inGround ? 1 : 0));
		nbt.setByte("pickup", (byte) this.canBePickedUp);
		nbt.setDouble("damage", this.damage);
		//nbt.setString("shooter", getShooter() instanceof EntityPlayer ? ((EntityPlayer) getShooter()).getCommandSenderName() : "");

		// TODO gamerforEA code start
		this.fake.writeToNBT(nbt);
		// TODO gamerforEA code end
	}

	@Override
	public void readEntityFromNBT(NBTTagCompound nbt)
	{
		this.blockX = nbt.getShort("xTile");
		this.blockY = nbt.getShort("yTile");
		this.blockZ = nbt.getShort("zTile");
		this.ticksInGround = nbt.getShort("life");
		this.blockHit = Block.getBlockById(nbt.getByte("inTile") & 255);
		this.inData = nbt.getByte("inData") & 255;
		this.arrowShake = nbt.getByte("shake") & 255;
		this.inGround = nbt.getByte("inGround") == 1;
		//dataWatcher.updateObject(SHOOTER_DATAWATCHER_INDEX, nbt.hasKey("shooter") ? nbt.getString("shooter") : "");

		if (nbt.hasKey("damage", 99))
			this.damage = nbt.getDouble("damage");

		if (nbt.hasKey("pickup", 99))
			this.canBePickedUp = nbt.getByte("pickup");
		else if (nbt.hasKey("player", 99))
			this.canBePickedUp = nbt.getBoolean("player") ? 1 : 0;

		// TODO gamerforEA code start
		this.fake.readFromNBT(nbt);
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

	@Override
	protected boolean canTriggerWalking()
	{
		return false;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public float getShadowSize()
	{
		return 0.0F;
	}

	@Override
	public void setDamage(double par1)
	{
		this.damage = par1;
	}

	@Override
	public double getDamage()
	{
		return this.damage;
	}

	@Override
	public void setKnockbackStrength(int par1)
	{
		this.knockbackStrength = par1;
	}

	@Override
	public boolean canAttackWithItem()
	{
		return false;
	}

	@Override
	public void setIsCritical(boolean par1)
	{
		byte b0 = this.dataWatcher.getWatchableObjectByte(16);

		if (par1)
			this.dataWatcher.updateObject(16, (byte) (b0 | 1));
		else
			this.dataWatcher.updateObject(16, (byte) (b0 & -2));
	}

	@Override
	public boolean getIsCritical()
	{
		byte b0 = this.dataWatcher.getWatchableObjectByte(16);
		return (b0 & 1) != 0;
	}
}
