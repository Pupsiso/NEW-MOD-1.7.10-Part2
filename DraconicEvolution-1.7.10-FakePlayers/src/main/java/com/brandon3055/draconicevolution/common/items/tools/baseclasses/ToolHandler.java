package com.brandon3055.draconicevolution.common.items.tools.baseclasses;

import com.brandon3055.draconicevolution.common.ModItems;
import com.brandon3055.draconicevolution.common.entity.EntityDragonProjectile;
import com.brandon3055.draconicevolution.common.items.weapons.IEnergyContainerWeaponItem;
import com.brandon3055.draconicevolution.common.utills.IUpgradableItem;
import com.brandon3055.draconicevolution.common.utills.LogHelper;
import com.gamerforea.eventhelper.util.EventUtils;
import java.util.List;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityDragonPart;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class ToolHandler {
	public static void damageEntityBasedOnHealth(Entity entity, EntityPlayer player, float dmgMult) {
		ItemStack stack = player.getCurrentEquippedItem();
		if(stack != null && stack.getItem() instanceof IEnergyContainerWeaponItem) {
			if(!EventUtils.cantDamage(player, entity)) {
				IEnergyContainerWeaponItem item = (IEnergyContainerWeaponItem)stack.getItem();
				float baseAttack = getDamageAgainstEntity(stack, entity);
				if(entity instanceof EntityLivingBase) {
					float entHealth = ((EntityLivingBase)entity).getHealth();
					baseAttack += entHealth * dmgMult;
				}

				if(entity instanceof EntityDragonPart) {
					List<EntityDragon> list = player.worldObj.getEntitiesWithinAABB(EntityDragon.class, entity.boundingBox.expand(10.0D, 10.0D, 10.0D));
					if(!list.isEmpty() && list.get(0) instanceof EntityDragon) {
						EntityDragon dragon = (EntityDragon)list.get(0);
						float entHealth = dragon.getHealth();
						baseAttack += entHealth * dmgMult;
					}
				}

				int rf = (int)baseAttack * item.getEnergyPerAttack();
				if(rf > item.getEnergyStored(stack)) {
					baseAttack = (float)(item.getEnergyStored(stack) / item.getEnergyPerAttack());
					rf = item.getEnergyStored(stack);
				}

				if(baseAttack <= 0.0F) {
					baseAttack = 1.0F;
				}

				entity.attackEntityFrom(DamageSource.causePlayerDamage(player), baseAttack);
				if(EnchantmentHelper.getEnchantmentLevel(Enchantment.fireAspect.effectId, stack) > 0) {
					entity.setFire(EnchantmentHelper.getEnchantmentLevel(Enchantment.fireAspect.effectId, stack) * 15);
				}

				if(!player.capabilities.isCreativeMode) {
					item.extractEnergy(stack, rf, false);
				}

				if(entity instanceof EntityLivingBase) {
					EntityLivingBase entityLivingBase = (EntityLivingBase)entity;
					double d1 = player.posX - entityLivingBase.posX;

					double d0;
					for(d0 = player.posZ - entityLivingBase.posZ; d1 * d1 + d0 * d0 < 1.0E-4D; d0 = (Math.random() - Math.random()) * 0.01D) {
						d1 = (Math.random() - Math.random()) * 0.01D;
					}

					entityLivingBase.attackedAtYaw = (float)(Math.atan2(d0, d1) * 180.0D / 3.141592653589793D) - entityLivingBase.rotationYaw;
					if(entityLivingBase.worldObj.rand.nextDouble() >= entityLivingBase.getEntityAttribute(SharedMonsterAttributes.knockbackResistance).getAttributeValue()) {
						entityLivingBase.isAirBorne = true;
						float f1 = MathHelper.sqrt_double(d1 * d1 + d0 * d0);
						float f2 = 0.1F + (float)EnchantmentHelper.getKnockbackModifier(player, entityLivingBase) * 0.4F;
						entityLivingBase.motionX /= 2.0D;
						entityLivingBase.motionY /= 2.0D;
						entityLivingBase.motionZ /= 2.0D;
						entityLivingBase.motionX -= d1 / (double)f1 * (double)f2;
						entityLivingBase.motionY += (double)f2;
						entityLivingBase.motionZ -= d0 / (double)f1 * (double)f2;
						if(entityLivingBase.motionY > 0.4000000059604645D) {
							entityLivingBase.motionY = 0.4000000059604645D;
						}
					}
				}

			}
		} else {
			LogHelper.error("[ToolHandler.java:147] WTF? I don\'t get it... Player " + player.getCommandSenderName() + " whacked something with a DE weapon but that they are not holding? Ok someone is messing with my shit...");
		}
	}

	public static void AOEAttack(EntityPlayer player, Entity entity, ItemStack stack, int range) {
		World world = player.worldObj;
		AxisAlignedBB box = AxisAlignedBB.getBoundingBox(entity.posX - (double)range, entity.posY - (double)range, entity.posZ - (double)range, entity.posX + (double)range, entity.posY + (double)range, entity.posZ + (double)range).expand(1.0D, 1.0D, 1.0D);
		List list = world.getEntitiesWithinAABBExcludingEntity(player, box);
		if(range != 0) {
			IEnergyContainerWeaponItem item = (IEnergyContainerWeaponItem)stack.getItem();

			for(Object entityObject : list) {
				if(item.getEnergyStored(stack) < item.getEnergyPerAttack()) {
					break;
				}

				if(!(entityObject instanceof EntityLivingBase)) {
					if(entityObject instanceof EntityDragonProjectile) {
						float dmg = getDamageAgainstEntity(stack, (Entity)entityObject);
						int rf = (int)dmg * item.getEnergyPerAttack();
						if(rf > item.getEnergyStored(stack)) {
							dmg = (float)(item.getEnergyStored(stack) / item.getEnergyPerAttack());
							rf = item.getEnergyStored(stack);
						}

						((Entity)entityObject).attackEntityFrom(DamageSource.causePlayerDamage(player), dmg);
						item.extractEnergy(stack, rf, false);
					}
				} else {
					EntityLivingBase entityLivingBase = (EntityLivingBase)entityObject;
					if(entityLivingBase.getEntityId() != entity.getEntityId() && !EventUtils.cantDamage(player, entityLivingBase)) {
						float dmg = getDamageAgainstEntity(stack, entityLivingBase);
						int rf = (int)dmg * item.getEnergyPerAttack();
						if(rf > item.getEnergyStored(stack)) {
							dmg = (float)(item.getEnergyStored(stack) / item.getEnergyPerAttack());
							rf = item.getEnergyStored(stack);
						}

						entityLivingBase.attackEntityFrom(DamageSource.causePlayerDamage(player), dmg);
						item.extractEnergy(stack, rf, false);
						if(EnchantmentHelper.getEnchantmentLevel(Enchantment.fireAspect.effectId, stack) > 0) {
							entityLivingBase.setFire(EnchantmentHelper.getEnchantmentLevel(Enchantment.fireAspect.effectId, stack) * 15);
						}

						double d1 = player.posX - entityLivingBase.posX;

						double d0;
						for(d0 = player.posZ - entityLivingBase.posZ; d1 * d1 + d0 * d0 < 1.0E-4D; d0 = (Math.random() - Math.random()) * 0.01D) {
							d1 = (Math.random() - Math.random()) * 0.01D;
						}

						if(entityLivingBase.worldObj.rand.nextDouble() >= entityLivingBase.getEntityAttribute(SharedMonsterAttributes.knockbackResistance).getAttributeValue()) {
							entityLivingBase.isAirBorne = true;
							float f1 = MathHelper.sqrt_double(d1 * d1 + d0 * d0);
							float f2 = 0.1F + (float)EnchantmentHelper.getKnockbackModifier(player, entityLivingBase) * 0.4F;
							entityLivingBase.motionX /= 2.0D;
							entityLivingBase.motionY /= 2.0D;
							entityLivingBase.motionZ /= 2.0D;
							entityLivingBase.motionX -= d1 / (double)f1 * (double)f2;
							entityLivingBase.motionY += (double)f2;
							entityLivingBase.motionZ -= d0 / (double)f1 * (double)f2;
							if(entityLivingBase.motionY > 0.4000000059604645D) {
								entityLivingBase.motionY = 0.4000000059604645D;
							}
						}

						entityLivingBase.attackTime = 0;
					}
				}
			}

		}
	}

	public static MovingObjectPosition raytraceFromEntity(World world, Entity player, double range) {
		float f = 1.0F;
		float f1 = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * f;
		float f2 = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * f;
		double d0 = player.prevPosX + (player.posX - player.prevPosX) * (double)f;
		double d1 = player.prevPosY + (player.posY - player.prevPosY) * (double)f;
		if(!world.isRemote && player instanceof EntityPlayer) {
			++d1;
		}

		double d2 = player.prevPosZ + (player.posZ - player.prevPosZ) * (double)f;
		Vec3 vec3 = Vec3.createVectorHelper(d0, d1, d2);
		float f3 = MathHelper.cos(-f2 * 0.017453292F - 3.1415927F);
		float f4 = MathHelper.sin(-f2 * 0.017453292F - 3.1415927F);
		float f5 = -MathHelper.cos(-f1 * 0.017453292F);
		float f6 = MathHelper.sin(-f1 * 0.017453292F);
		float f7 = f4 * f5;
		float f8 = f3 * f5;
		double d3 = range;
		if(player instanceof EntityPlayerMP && range < 10.0D) {
			d3 = ((EntityPlayerMP)player).theItemInWorldManager.getBlockReachDistance();
		}

		Vec3 vec31 = vec3.addVector((double)f7 * d3, (double)f6 * d3, (double)f8 * d3);
		return world.rayTraceBlocks(vec3, vec31);
	}

	public static void updateGhostBlocks(EntityPlayer player, World world) {
		if(!world.isRemote) {
			int xPos = (int)player.posX;
			int yPos = (int)player.posY;
			int zPos = (int)player.posZ;

			for(int x = xPos - 6; x < xPos + 6; ++x) {
				for(int y = yPos - 6; y < yPos + 6; ++y) {
					for(int z = zPos - 6; z < zPos + 6; ++z) {
						((EntityPlayerMP)player).playerNetServerHandler.sendPacket(new S23PacketBlockChange(x, y, z, world));
					}
				}
			}

		}
	}

	public static float getBaseAttackDamage(ItemStack stack) {
		float dmg = 0.0F;
		if(stack == null) {
			return 1.0F;
		} else {
			float sharpMod = (float)EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack) * 4.0F;
			if(stack.getItem() == ModItems.draconicDestructionStaff) {
				dmg = ModItems.CHAOTIC.getDamageVsEntity() + sharpMod;
			} else if(stack.getItem() instanceof ItemSword) {
				dmg = ((ItemSword)stack.getItem()).func_150931_i() + sharpMod;
			}

			dmg = dmg + (float)(IUpgradableItem.EnumUpgrade.ATTACK_DAMAGE.getUpgradePoints(stack) * 5);
			return dmg;
		}
	}

	public static float getDamageAgainstEntity(ItemStack stack, Entity entity) {
		float baseAttack = getBaseAttackDamage(stack);
		float smiteMod = (float)EnchantmentHelper.getEnchantmentLevel(Enchantment.smite.effectId, stack) * 6.0F;
		float athropodsMod = (float)EnchantmentHelper.getEnchantmentLevel(Enchantment.baneOfArthropods.effectId, stack) * 6.0F;
		if(entity instanceof EntityLivingBase && ((EntityLivingBase)entity).isEntityUndead()) {
			baseAttack += smiteMod;
		}

		if(entity instanceof EntitySpider) {
			baseAttack += athropodsMod;
		}

		return baseAttack;
	}
}
