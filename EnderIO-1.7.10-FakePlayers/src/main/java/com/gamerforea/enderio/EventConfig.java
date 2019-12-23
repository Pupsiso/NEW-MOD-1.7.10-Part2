package com.gamerforea.enderio;

import com.gamerforea.eventhelper.config.Config;
import com.gamerforea.eventhelper.config.ConfigBoolean;
import com.gamerforea.eventhelper.config.ConfigUtils;

@Config(name = "EnderIO")
public final class EventConfig
{
	private static final String CATEGORY_EVENTS = "events";
	private static final String CATEGORY_OTHER = "other";
	private static final String CATEGORY_PERFORMANCE = "performance";

	@ConfigBoolean(name = "teleportBlink",
				   category = CATEGORY_EVENTS,
				   comment = "Ивент для телепортации с помощью меча",
				   oldName = "blinkEvent")
	public static boolean blinkEvent = true;

	@ConfigBoolean(category = CATEGORY_OTHER, comment = "Игнорировать тип конденсатора в спавнере")
	public static boolean spawnerIgnoreCapacitorType = false;

	@ConfigBoolean(category = CATEGORY_OTHER,
				   comment = "Выключить спавн скелетов-иссушителей в спавнере",
				   oldName = "spawnerDisableWitcherSkeletons")
	public static boolean spawnerDisableWitherSkeletons = false;

	@ConfigBoolean(category = CATEGORY_OTHER, comment = "Фикс спавна скелетов-иссушителей")
	public static boolean fixSpawnWitherSkeletons = false;

	@ConfigBoolean(category = CATEGORY_OTHER, comment = "Игнорировать тип конденсатора в крафтере")
	public static boolean crafterIgnoreCapacitorType = false;

	@ConfigBoolean(category = CATEGORY_OTHER,
				   comment = "Убирать невалидные трубы из их сетей (может исправить некоторые дюпы)")
	public static boolean removeInvalidConduitsFromNetwork = false;

	@ConfigBoolean(category = CATEGORY_OTHER,
				   comment = "Строгая проверка валидности GUI для Посоха путешествия (может сломать дистанционное открытие GUI) (фикс дюпа)")
	public static boolean travelStuffStrictGui = false;

	@ConfigBoolean(category = CATEGORY_OTHER, comment = "Запретить 'ремонт' Тёмных ножниц")
	public static boolean noDarkSteelShearsRepair = true;

	@ConfigBoolean(category = CATEGORY_OTHER,
				   comment = "Альтернативный режим сохранения вещей, зачарованных на SoulBound (ВНИМАНИЕ: может как исправить дюп, так и создать его)")
	public static boolean alternativeSoulBoundRestoration = false;

	@ConfigBoolean(category = CATEGORY_OTHER,
				   comment = "Фикс уязвимости с пакетами (телепортация по произвольным координатам)")
	public static boolean networkTeleportFix = true;

	@ConfigBoolean(category = CATEGORY_OTHER,
				   comment = "Расширенный фикс уязвимости с пакетами (телепортация по произвольным координатам)")
	public static boolean paranoidNetworkTeleportFix = true;

	@ConfigBoolean(category = CATEGORY_OTHER, comment = "Только игрок может использовать Телепорт (защита от дюпа)")
	public static boolean telepadPlayerOnly = true;

	@ConfigBoolean(category = CATEGORY_PERFORMANCE, comment = "Кэшировать ID OreDictionary")
	public static boolean cacheOreIDs = true;

	@ConfigBoolean(category = CATEGORY_PERFORMANCE, comment = "Быстрая проверка корректности рецептов")
	public static boolean fastRecipeCheck = true;

	static
	{
		ConfigUtils.readConfig(EventConfig.class);
	}
}