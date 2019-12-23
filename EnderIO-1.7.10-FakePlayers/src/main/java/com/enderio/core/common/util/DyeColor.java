package com.enderio.core.common.util;

import com.enderio.core.EnderCore;
import com.gamerforea.enderio.util.FastOreDictionary;
import net.minecraft.item.ItemDye;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

public enum DyeColor
{

	BLACK,
	RED,
	GREEN,
	BROWN,
	BLUE,
	PURPLE,
	CYAN,
	SILVER,
	GRAY,
	PINK,
	LIME,
	YELLOW,
	LIGHT_BLUE,
	MAGENTA,
	ORANGE,
	WHITE;

	public static final String[] DYE_ORE_NAMES = { "dyeBlack", "dyeRed", "dyeGreen", "dyeBrown", "dyeBlue", "dyePurple", "dyeCyan", "dyeLightGray", "dyeGray", "dyePink", "dyeLime", "dyeYellow", "dyeLightBlue", "dyeMagenta", "dyeOrange", "dyeWhite" };

	public static final String[] DYE_ORE_UNLOCAL_NAMES = { "item.fireworksCharge.black", "item.fireworksCharge.red", "item.fireworksCharge.green", "item.fireworksCharge.brown", "item.fireworksCharge.blue", "item.fireworksCharge.purple", "item.fireworksCharge.cyan", "item.fireworksCharge.silver", "item.fireworksCharge.gray", "item.fireworksCharge.pink", "item.fireworksCharge.lime", "item.fireworksCharge.yellow", "item.fireworksCharge.lightBlue", "item.fireworksCharge.magenta", "item.fireworksCharge.orange", "item.fireworksCharge.white" };

	public static DyeColor getNext(DyeColor col)
	{
		int ord = col.ordinal() + 1;

		/* TODO gamerforEA code replace, old code:
		if (ord >= DyeColor.values().length)
			ord = 0;
		return DyeColor.values()[ord]; */
		DyeColor[] colors = DyeColor.values();
		if (ord >= colors.length)
			ord = 0;
		return colors[ord];
		// TODO gamerforEA code end
	}

	public static DyeColor getColorFromDye(ItemStack dye)
	{
		if (dye == null)
			return null;

		// TODO gamerforEA code replace, old code:
		// int[] oreIDs = OreDictionary.getOreIDs(dye);
		int[] oreIDs = FastOreDictionary.getOreIDs(dye);
		if (oreIDs.length == 0)
			return null;
		// TODO gamerforEA code end

		for (int i = 0; i < DYE_ORE_NAMES.length; i++)
		{
			int dyeID = OreDictionary.getOreID(DYE_ORE_NAMES[i]);
			for (int oreId : oreIDs)
			{
				if (dyeID == oreId)
					return DyeColor.values()[i];
			}
		}

		return null;
	}

	public static DyeColor fromIndex(int index)
	{
		return DyeColor.values()[index];
	}

	DyeColor()
	{
	}

	public int getColor()
	{
		return ItemDye.field_150922_c[this.ordinal()];
	}

	public String getName()
	{
		return ItemDye.field_150921_b[this.ordinal()];
	}

	public String getLocalisedName()
	{
		return EnderCore.lang.localizeExact(DYE_ORE_UNLOCAL_NAMES[this.ordinal()], false);
	}

	@Override
	public String toString()
	{
		return this.getName();
	}

}