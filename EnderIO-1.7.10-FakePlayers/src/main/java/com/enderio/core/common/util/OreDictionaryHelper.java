package com.enderio.core.common.util;

import com.gamerforea.enderio.util.FastOreDictionary;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import java.util.List;

public final class OreDictionaryHelper
{

	public static final String INGOT_COPPER = "ingotCopper";
	public static final String INGOT_TIN = "ingotTin";
	public static final String DUST_ENDERPEARL = "dustEnderPearl";
	public static final String INGOT_ENDERIUM = "ingotEnderium";

	public static boolean isRegistered(String name)
	{
		return !getOres(name).isEmpty();
	}

	public static List<ItemStack> getOres(String name)
	{
		return OreDictionary.getOres(name);
	}

	public static boolean hasCopper()
	{
		return isRegistered(INGOT_COPPER);
	}

	public static boolean hasTin()
	{
		return isRegistered(INGOT_TIN);
	}

	public static boolean hasEnderPearlDust()
	{
		return isRegistered(DUST_ENDERPEARL);
	}

	private OreDictionaryHelper()
	{
	}

	public static boolean hasEnderium()
	{
		return isRegistered(INGOT_ENDERIUM);
	}

	public static String[] getOreNames(ItemStack stack)
	{
		// TODO gamerforEA code replace, old code:
		// int[] ids = OreDictionary.getOreIDs(stack);
		int[] ids = FastOreDictionary.getOreIDs(stack);
		// TODO gamerforEA code end

		String[] ret = new String[ids.length];
		for (int i = 0; i < ids.length; i++)
		{
			ret[i] = OreDictionary.getOreName(ids[i]);
		}
		return ret;
	}

	public static boolean hasName(ItemStack stack, String oreName)
	{
		// TODO gamerforEA code replace, old code:
		// return ArrayUtils.contains(getOreNames(stack), oreName);
		if (oreName == null)
			return false;

		int[] ids = FastOreDictionary.getOreIDs(stack);
		for (int id : ids)
		{
			String name = OreDictionary.getOreName(id);
			if (oreName.equals(name))
				return true;
		}

		return false;
		// TODO gamerforEA code end
	}
}
