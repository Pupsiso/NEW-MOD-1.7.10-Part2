package crazypants.enderio.xp;

import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import java.lang.reflect.Field;

/**
 * Values taken from OpenMods EnchantmentUtils to ensure consistent behavior
 *
 * @see {@link https://github.com/OpenMods/OpenModsLib/blob/master/src/main/java/openmods/utils/EnchantmentUtils.java}
 */
public class XpUtil
{

	//Values taken from OpenBlocks to ensure compatibility

	public static final int XP_PER_BOTTLE = 8;
	public static final int RATIO = 20;
	public static final int LIQUID_PER_XP_BOTTLE = XP_PER_BOTTLE * RATIO;

	public static int liquidToExperience(int liquid)
	{
		return liquid / RATIO;
	}

	public static int experienceToLiquid(int xp)
	{
		// TODO gamerforEA code start
		if (Math.abs(xp) >= 107374183)
			return Integer.MAX_VALUE;
		// TODO gamerforEA code end

		return xp * RATIO;
	}

	public static int getLiquidForLevel(int level)
	{
		return experienceToLiquid(getExperienceForLevel(level));
	}

	// Non negative
	public static int getExperienceForLevel(int level)
	{
		// TODO gamerforEA code start
		if (level < -24748 || level > 24791)
			return Integer.MAX_VALUE;
		// TODO gamerforEA code end

		if (level == 0)
			return 0;

		if (level > 0 && level < 16)
			return level * 17;

		if (level > 15 && level < 31)
			// TODO gamerforEA code replace, old code:
			// return (int) (1.5 * Math.pow(level, 2) - 29.5 * level + 360);
			return (int) ((1.5 * level - 29.5) * level) + 360;
		// TODO gamerforEA code end

		// TODO gamerforEA code replace, old code:
		// return (int) (3.5 * Math.pow(level, 2) - 151.5 * level + 2220);
		return (int) ((3.5 * level - 151.5) * level) + 2220;
		// TODO gamerforEA code end
	}

	// Positive
	public static int getXpBarCapacity(int level)
	{
		// TODO gamerforEA code start
		if (level >= 306783400)
			return Integer.MAX_VALUE;
		// TODO gamerforEA code end

		if (level >= 30)
			return 62 + (level - 30) * 7;
		if (level >= 15)
			return 17 + (level - 15) * 3;
		return 17;
	}

	// Non negative
	public static int getLevelForExperience(int experience)
	{
		// TODO gamerforEA code start
		if (experience <= 0)
			return 0;
		// TODO gamerforEA code end

		int i = 0;
		while (getExperienceForLevel(i) <= experience)
		{
			i++;
		}
		return i - 1;
	}

	// Non negative
	public static int getPlayerXP(EntityPlayer player)
	{
		int experienceForLevel = getExperienceForLevel(player.experienceLevel);
		int xpBar = (int) (player.experience * player.xpBarCap());

		// TODO gamerforEA code start
		if (Integer.MAX_VALUE - experienceForLevel <= xpBar)
			return Integer.MAX_VALUE;
		// TODO gamerforEA code end

		return experienceForLevel + xpBar;
	}

	public static void addPlayerXP(EntityPlayer player, int amount)
	{
		// TODO gamerforEA code start
		if (amount == 0)
			return;
		// TODO gamerforEA code end

		int prevXp = getPlayerXP(player);

		// TODO gamerforEA code start
		amount = amount > 0 ? Math.min(amount, Integer.MAX_VALUE - prevXp) : -Math.min(-amount, prevXp);
		if (amount == 0)
			return;
		// TODO gamerforEA code end

		int experience = prevXp + amount;
		player.experienceTotal = experience;
		player.experienceLevel = getLevelForExperience(experience);
		int expForLevel = getExperienceForLevel(player.experienceLevel);
		player.experience = (float) (experience - expForLevel) / (float) player.xpBarCap();

		// TODO gamerforEA code start
		if (player.experienceTotal < 0 || player.experienceLevel < 0 || player.experience < 0)
		{
			player.experienceTotal = 0;
			player.experienceLevel = 0;
			player.experience = 0;
		}

		if (player instanceof EntityPlayerMP)
			// ((EntityPlayerMP) player).lastExperience = -1;
			try
			{
				LAST_EXPERIENCE_FIELD.set(player, -1);
			}
			catch (IllegalAccessException e)
			{
				e.printStackTrace();
			}
		// TODO gamerforEA code end
	}

	// TODO gamerforEA code start
	private static final Field LAST_EXPERIENCE_FIELD = ReflectionHelper.findField(EntityPlayerMP.class, "field_71144_ck", "lastExperience");
	// TODO gamerforEA code end
}
