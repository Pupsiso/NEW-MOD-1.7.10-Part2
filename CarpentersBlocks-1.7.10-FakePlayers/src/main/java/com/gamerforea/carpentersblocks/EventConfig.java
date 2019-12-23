package com.gamerforea.carpentersblocks;

import com.gamerforea.eventhelper.config.Config;
import com.gamerforea.eventhelper.config.ConfigBoolean;
import com.gamerforea.eventhelper.config.ConfigString;
import com.gamerforea.eventhelper.config.ConfigUtils;

@Config(name = "CarpentersBlocks")
public final class EventConfig
{
	private static final String CATEGORY_OTHER = "other";

	@ConfigString(category = CATEGORY_OTHER, comment = "Permission для доступа к сейфу")
	public static String safeAccessPermission = "carpentersblocks.accesssafe";

	@ConfigBoolean(category = CATEGORY_OTHER,
				   comment = "Использовать PlayerInteractEvent(Action=PHYSICAL) для нажимных плит")
	public static boolean plateValidEvents = false;

	@ConfigBoolean(category = CATEGORY_OTHER, comment = "Расширенная защита Сейфа плотника от разрушения")
	public static boolean extendedSafeProtection = false;

	public static void init()
	{
		ConfigUtils.readConfig(EventConfig.class);
		ServerEventHandler.init();
	}

	static
	{
		init();
	}
}