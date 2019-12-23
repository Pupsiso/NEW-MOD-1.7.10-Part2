package net.divinerpg.utils.events;

import com.gamerforea.divinerpg.ModUtils;
import cpw.mods.fml.common.ObfuscationReflectionHelper;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.PlayerTickEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.divinerpg.libs.DivineRPGAchievements;
import net.divinerpg.utils.FlyingHelper;
import net.divinerpg.utils.config.ConfigurationHelper;
import net.divinerpg.utils.items.*;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.PlayerCapabilities;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.FoodStats;
import net.minecraft.world.World;

import java.lang.reflect.Field;
import java.util.List;

public class EventArmorTick
{
	private float flyTemp;

	private Item boots = null;
	private Item body = null;
	private Item legs = null;
	private Item helmet = null;

	/* TODO gamerforEA code replace, old code:
	public static final String[] isImmuneToFire = { "ae", "field_70178_ae", "isImmuneToFire" };
	public static final String[] isJumping = { "bc", "field_70703_bu", "isJumping" };
	public static final String[] walkSpeed = { "g", "field_75097_g", "walkSpeed" }; */
	public static final String[] isImmuneToFire = { "field_70178_ae", "isImmuneToFire", "ae" };
	public static final String[] isJumping = { "field_70703_bu", "isJumping", "bc" };
	public static final String[] walkSpeed = { "field_75097_g", "walkSpeed", "g" };
	private static final Field walkSpeedField;

	static
	{
		Field wsField = null;

		try
		{
			wsField = ReflectionHelper.findField(PlayerCapabilities.class, ObfuscationReflectionHelper.remapFieldNames(PlayerCapabilities.class.getName(), walkSpeed));
			wsField.setAccessible(true);
		}
		catch (ReflectionHelper.UnableToFindFieldException e)
		{
			e.printStackTrace();
		}

		walkSpeedField = wsField;
	}
	// TODO gamerforEA code end

	private World world;

	@SubscribeEvent
	public void onTickEvent(PlayerTickEvent event)
	{
		EntityPlayer player = event.player;
		this.world = player.worldObj;
		ItemStack stackBoots = player.inventory.armorItemInSlot(0);
		ItemStack stackLegs = player.inventory.armorItemInSlot(1);
		ItemStack stackBody = player.inventory.armorItemInSlot(2);
		ItemStack stackHelmet = player.inventory.armorItemInSlot(3);

		float speedMultiplier = 1;

		this.boots = stackBoots != null ? stackBoots.getItem() : null;
		this.body = stackBody != null ? stackBody.getItem() : null;
		this.legs = stackLegs != null ? stackLegs.getItem() : null;
		this.helmet = stackHelmet != null ? stackHelmet.getItem() : null;

		if (this.boots != VanillaItemsArmor.angelicBoots && this.body != VanillaItemsArmor.angelicBody && this.legs != VanillaItemsArmor.angelicLegs && this.helmet != VanillaItemsArmor.angelicHelmet)
			FlyingHelper.getProperties(player).couldFly = player.capabilities.allowFlying;
		if (player.capabilities.isCreativeMode)
			FlyingHelper.getProperties(player).couldFly = false;

		if (this.boots == VanillaItemsArmor.angelicBoots && this.body == VanillaItemsArmor.angelicBody && this.legs == VanillaItemsArmor.angelicLegs && this.helmet == VanillaItemsArmor.angelicHelmet && ArcanaHelper.getProperties(player).getBarValue() != 0)
		{
			player.fallDistance = -0.5F;
			player.triggerAchievement(DivineRPGAchievements.whenPigsFly);
			player.capabilities.allowFlying = true;
			if (player.capabilities.isFlying && !player.capabilities.isCreativeMode && !FlyingHelper.getProperties(player).couldFly)
				ArcanaHelper.getProperties(player).useBar(0.5f);
			if (ArcanaHelper.getProperties(player).getBarValue() < 1 && !player.capabilities.isCreativeMode && !FlyingHelper.getProperties(player).couldFly)
			{
				player.capabilities.isFlying = false;
				player.capabilities.allowFlying = false;
			}
		}
		else if (player.capabilities.allowFlying && !player.capabilities.isCreativeMode && !FlyingHelper.getProperties(player).couldFly)
		{
			player.capabilities.isFlying = false;
			player.capabilities.allowFlying = false;
		}

		//Elite Realmite
		if (this.boots == VanillaItemsArmor.eliteRealmiteBoots && this.body == VanillaItemsArmor.eliteRealmiteBody && this.legs == VanillaItemsArmor.eliteRealmiteLegs && this.helmet == VanillaItemsArmor.eliteRealmiteHelmet)
			player.fallDistance = -0.5F;

		//Divine
		if (this.boots == VanillaItemsArmor.divineBoots && this.body == VanillaItemsArmor.divineBody && this.legs == VanillaItemsArmor.divineLegs && this.helmet == VanillaItemsArmor.divineHelmet)
			player.fallDistance = -0.5F;

		//Wildwood
		if (this.boots == TwilightItemsArmor.wildwoodBoots && this.body == TwilightItemsArmor.wildwoodChestplate && this.legs == TwilightItemsArmor.wildwoodLeggings && this.helmet == TwilightItemsArmor.wildwoodHelmet)
			if (player.isInsideOfMaterial(Material.water))
			{
				float current = player.getHealth();
				if (current > 0.0F && current < 20.0F)
					player.heal(0.25f);
			}

		//Korma
		if (this.boots == ArcanaItems.kormaBoots && this.body == ArcanaItems.kormaBody && this.legs == ArcanaItems.kormaLegs && this.helmet == ArcanaItems.kormaHelmet)
			ArcanaHelper.getProperties(player).regen(1);

		//Vemos
		if (this.boots == ArcanaItems.vemosBoots && this.body == ArcanaItems.vemosBody && this.legs == ArcanaItems.vemosLegs && this.helmet == ArcanaItems.vemosHelmet)
		{
			float current = player.getHealth();
			if (current > 0.0F && current < 20.0F)
				player.setHealth(current + 0.1F);
		}

		//Mortum
		if (this.boots == TwilightItemsArmor.mortumBoots && this.body == TwilightItemsArmor.mortumChestplate && this.legs == TwilightItemsArmor.mortumLeggings && this.helmet == TwilightItemsArmor.mortumHelmet)
			player.addPotionEffect(new PotionEffect(Potion.nightVision.id, 210, 10, true));

		//Skythern
		if (this.boots == TwilightItemsArmor.skythernBoots && this.body == TwilightItemsArmor.skythernChestplate && this.legs == TwilightItemsArmor.skythernLeggings && this.helmet == TwilightItemsArmor.skythernHelmet)
			player.fallDistance = -0.5F;

		//Netherite, Inferno, and Bedrock
		if (this.boots == VanillaItemsArmor.netheriteBoots && this.legs == VanillaItemsArmor.netheriteLegs && this.body == VanillaItemsArmor.netheriteBody && this.helmet == VanillaItemsArmor.netheriteHelmet || this.boots == VanillaItemsArmor.infernoBoots && this.legs == VanillaItemsArmor.infernoLegs && this.body == VanillaItemsArmor.infernoBody && this.helmet == VanillaItemsArmor.infernoHelmet || this.boots == VanillaItemsArmor.bedrockBoots && this.legs == VanillaItemsArmor.bedrockLegs && this.body == VanillaItemsArmor.bedrockBody && this.helmet == VanillaItemsArmor.bedrockHelmet)
			player.addPotionEffect(new PotionEffect(Potion.fireResistance.id, 40, 0, true));

		//Aquastrive
		if (this.boots == VanillaItemsArmor.aquastriveBoots && this.body == VanillaItemsArmor.aquastriveBody && this.legs == VanillaItemsArmor.aquastriveLegs && this.helmet == VanillaItemsArmor.aquastriveHelmet)
		{
			float speed = 1.1F;

			// TODO gamerforEA code clear:
			// boolean isJumping = ObfuscationReflectionHelper.getPrivateValue(EntityLivingBase.class, player, EventArmorFullSet.isJumping);

			if (player.isInWater())
			{
				// TODO gamerforEA code start
				boolean isJumping = ObfuscationReflectionHelper.getPrivateValue(EntityLivingBase.class, player, EventArmorFullSet.isJumping);
				// TODO gamerforEA code end

				boolean sneaking = player.isSneaking();
				if (!sneaking && !isJumping)
				{
					if (player.motionX > -speed && player.motionX < speed)
					{
						player.motionX *= speed;
						player.motionY = 0F;
					}
					if (player.motionZ > -speed && player.motionZ < speed)
					{
						player.motionZ *= speed;
						player.motionY = 0F;
					}
				}
				if (isJumping || sneaking)
				{
					player.motionY *= speed;
					if (player.motionX > -speed && player.motionX < speed)
						player.motionX *= speed;
					if (player.motionZ > -speed && player.motionZ < speed)
						player.motionZ *= speed;
				}
			}
		}

		//Shadow
		if (this.boots == VanillaItemsArmor.shadowBoots && this.body == VanillaItemsArmor.shadowBody && this.legs == VanillaItemsArmor.shadowLegs && this.helmet == VanillaItemsArmor.shadowHelmet)
			speedMultiplier = 3;
		//Frozen
		if (this.boots == VanillaItemsArmor.frozenBoots && this.body == VanillaItemsArmor.frozenBody && this.legs == VanillaItemsArmor.frozenLegs && this.helmet == VanillaItemsArmor.frozenHelmet && !player.worldObj.isRemote && Ticker.tick % 10 == 0)
		{
			List<Entity> entities = player.worldObj.getEntitiesWithinAABB(EntityMob.class, player.boundingBox.expand(6, 6, 6));
			for (Entity e : entities)
			{
				((EntityMob) e).addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 40, 1, true));
			}
		}

		//Terran
		if (this.boots == VanillaItemsArmor.terranBoots && this.body == VanillaItemsArmor.terranBody && this.legs == VanillaItemsArmor.terranLegs && this.helmet == VanillaItemsArmor.terranHelmet)
			player.addPotionEffect(new PotionEffect(Potion.digSpeed.id, 20, 2, true));

		//Skeleman
		if (this.boots == VanillaItemsArmor.skelemanBoots && this.body == VanillaItemsArmor.skelemanBody && this.legs == VanillaItemsArmor.skelemanLegs && this.helmet == VanillaItemsArmor.skelemanHelmet)
		{
			FoodStats foodStats = player.getFoodStats();
			if (foodStats.needFood())
				foodStats.addStats(1, 0);
		}

		//Santa
		if (this.boots == IceikaItems.santaBoots && this.body == IceikaItems.santaTunic && this.legs == IceikaItems.santaPants && this.helmet == IceikaItems.santaCap)
			if (player.worldObj.provider.dimensionId == ConfigurationHelper.iceika)
			{
				FoodStats foodStats = player.getFoodStats();
				if (foodStats.needFood())
					foodStats.addStats(1, 0);
				speedMultiplier = 2;
			}

		//Vethean

		if (this.body == VetheaItems.glisteningBody && this.legs == VetheaItems.glisteningLegs && this.boots == VetheaItems.glisteningBoots && this.helmet == VetheaItems.glisteningMask)
			speedMultiplier = 1.4f;

		if (this.body == VetheaItems.demonizedBody && this.legs == VetheaItems.demonizedLegs && this.boots == VetheaItems.demonizedBoots && this.helmet == VetheaItems.demonizedMask)
			speedMultiplier = 1.8f;

		if (this.body == VetheaItems.tormentedBody && this.legs == VetheaItems.tormentedLegs && this.boots == VetheaItems.tormentedBoots && this.helmet == VetheaItems.tormentedMask)
			speedMultiplier = 2.2f;

		float newWalkSpeed = 0.1f * speedMultiplier;

		// TODO gamerforEA code replace, old code:
		// ObfuscationReflectionHelper.setPrivateValue(PlayerCapabilities.class, player.capabilities, newWalkSpeed, walkSpeed);
		if (!ModUtils.isEquals(player.capabilities.getWalkSpeed(), newWalkSpeed))
		{
			if (walkSpeedField == null)
				ObfuscationReflectionHelper.setPrivateValue(PlayerCapabilities.class, player.capabilities, newWalkSpeed, walkSpeed);
			else
				try
				{
					walkSpeedField.set(player.capabilities, newWalkSpeed);
				}
				catch (IllegalAccessException e)
				{
					e.printStackTrace();
				}
		}
		// TODO gamerforEA code end

		if (this.body == VetheaItems.glisteningBody && this.legs == VetheaItems.glisteningLegs && this.boots == VetheaItems.glisteningBoots && this.helmet == VetheaItems.glisteningHood)
			player.fallDistance = -0.5F;

		if (this.body == VetheaItems.demonizedBody && this.legs == VetheaItems.demonizedLegs && this.boots == VetheaItems.demonizedBoots && this.helmet == VetheaItems.demonizedHood)
			player.fallDistance = -0.5F;

		if (this.body == VetheaItems.tormentedBody && this.legs == VetheaItems.tormentedLegs && this.boots == VetheaItems.tormentedBoots && this.helmet == VetheaItems.tormentedHood)
			player.fallDistance = -0.5F;

		if (player.inventory.hasItem(VetheaItems.minersAmulet))
			player.addPotionEffect(new PotionEffect(Potion.digSpeed.id, 1, 2, true));
	}

}
