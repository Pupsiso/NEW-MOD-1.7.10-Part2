package com.gamerforea.emt;

import com.gamerforea.eventhelper.util.FastUtils;
import net.minecraftforge.common.config.Configuration;

import static net.minecraftforge.common.config.Configuration.CATEGORY_GENERAL;

public final class EventConfig
{
	public static int thorHammerCooldown = 1;
	public static int electricThorHammerCooldown = 1;
	public static boolean oneRingNbtFix = false;

	public static void init()
	{
		try
		{
			Configuration cfg = FastUtils.getConfig("EMT");
			String c = CATEGORY_GENERAL;
			thorHammerCooldown = cfg.getInt("thorHammerCooldown", c, thorHammerCooldown, 0, Integer.MAX_VALUE, "Кулдаун для Молота Тора (0 - нет кулдауна) в секундах");
			electricThorHammerCooldown = cfg.getInt("electricThorHammerCooldown", c, electricThorHammerCooldown, 0, Integer.MAX_VALUE, "Кулдаун для Электрического молота Тора (0 - нет кулдауна) в секундах");
			oneRingNbtFix = cfg.getBoolean("oneRingNbtFix", c, oneRingNbtFix, "Фикс работы Кольца Всевластия с NBT игрока");
			cfg.save();
		}
		catch (Throwable throwable)
		{
			System.err.println("Failed load config. Use default values.");
			throwable.printStackTrace();
		}
	}

	static
	{
		init();
	}
}