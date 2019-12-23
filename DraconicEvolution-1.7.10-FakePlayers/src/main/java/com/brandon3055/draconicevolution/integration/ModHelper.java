package com.brandon3055.draconicevolution.integration;

import com.brandon3055.draconicevolution.common.items.armor.CustomArmorHandler.ArmorSummery;
import com.gamerforea.draconicevolution.EventConfig;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

/**
 * Created by brandon3055 on 29/9/2015.
 */
public class ModHelper
{

	public static boolean isTConInstalled;
	public static boolean isAvaritiaInstalled;
	public static boolean isRotaryCraftInstalled;
	private static Item cleaver;
	private static Item avaritiaSword;
	private static Item bedrockSword;

	public static void init()
	{
		isTConInstalled = Loader.isModLoaded("TConstruct");
		isAvaritiaInstalled = Loader.isModLoaded("Avaritia");
		isRotaryCraftInstalled = Loader.isModLoaded("RotaryCraft");
	}

	public static boolean isHoldingCleaver(EntityPlayer player)
	{
		if (!isTConInstalled)
			return false;
		if (cleaver == null)
			cleaver = GameRegistry.findItem("TConstruct", "cleaver");

		return cleaver != null && player.getHeldItem() != null && player.getHeldItem().getItem().equals(cleaver);
	}

	public static boolean isHoldingAvaritiaSword(EntityPlayer player)
	{
		if (!isAvaritiaInstalled)
			return false;
		if (avaritiaSword == null)
			avaritiaSword = GameRegistry.findItem("Avaritia", "Infinity_Sword");

		return avaritiaSword != null && player.getHeldItem() != null && player.getHeldItem().getItem().equals(avaritiaSword);
	}

	public static boolean isHoldingBedrockSword(EntityPlayer player)
	{
		if (!isRotaryCraftInstalled)
			return false;
		if (bedrockSword == null)
			bedrockSword = GameRegistry.findItem("RotaryCraft", "rotarycraft_item_bedsword");

		return bedrockSword != null && player.getHeldItem() != null && player.getHeldItem().getItem().equals(bedrockSword);
	}

	public static float applyModDamageAdjustments(ArmorSummery summery, LivingAttackEvent event)
	{
		if (summery == null)
			return event.ammount;
		EntityPlayer attacker = event.source.getEntity() instanceof EntityPlayer ? (EntityPlayer) event.source.getEntity() : null;

		if (attacker == null)
			return event.ammount;

		if (isHoldingAvaritiaSword(attacker))
		{
			// TODO gamerforEA code start
			if (EventConfig.ignoreHurtResistantTime)
				// TODO gamerforEA code end
				event.entityLiving.hurtResistantTime = 0;

			return 300F;
		}
		if (isHoldingBedrockSword(attacker))
		{
			summery.entropy += 10;

			if (summery.entropy > 100)
				summery.entropy = 100;

			return Math.max(event.ammount, Math.min(50F, summery.protectionPoints));
		}
		if (event.source.isUnblockable() || event.source.canHarmInCreative())
		{
			summery.entropy += 3;

			if (summery.entropy > 100)
				summery.entropy = 100;

			return event.ammount * 2;
		}

		return event.ammount;
	}

}
