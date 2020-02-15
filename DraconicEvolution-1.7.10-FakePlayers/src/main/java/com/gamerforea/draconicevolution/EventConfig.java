package com.gamerforea.draconicevolution;

import com.gamerforea.eventhelper.config.*;
import net.minecraft.entity.Entity;

import java.util.Collections;

import static net.minecraftforge.common.config.Configuration.CATEGORY_GENERAL;

@Config(name = "DraconicEvolution")
public final class EventConfig
{
	private static final String CATEGORY_BLACKLISTS = "blacklists";
	private static final String CATEGORY_PERFORMANCE = "performance";
	private static final String CATEGORY_OTHER = "other";

	@ConfigClassSet(name = "teleporter",
					category = CATEGORY_BLACKLISTS,
					comment = "Чёрный список сущностей для Charm of Dislocation",
					oldName = "teleporterBlackList",
					oldCategory = CATEGORY_GENERAL)
	public static final ClassSet<Entity> teleporterBlackList = new ClassSet<>(Entity.class);

	@ConfigClassSet(name = "spawner",
					category = CATEGORY_BLACKLISTS,
					comment = "Чёрный список сущностей для спавнера",
					oldName = "spawnerBlackList",
					oldCategory = CATEGORY_GENERAL)
	public static final ClassSet<Entity> spawnerBlackList = new ClassSet<>(Entity.class);

	@ConfigBoolean(category = CATEGORY_PERFORMANCE,
				   comment = "Оптимизация Energy Pylon",
				   oldCategory = CATEGORY_GENERAL)
	public static boolean energyPylonOptimize = true;

	@ConfigBoolean(category = CATEGORY_OTHER, comment = "Включить взрыв реактора", oldCategory = CATEGORY_GENERAL)
	public static boolean enableReactorExplosion = false;

	@ConfigBoolean(category = CATEGORY_OTHER, comment = "Включить взрыв Chaos Crystal")
	public static boolean enableChaosExplosion = false;

	@ConfigBoolean(category = CATEGORY_OTHER,
				   comment = "Включить взрыв стрел DraconicBow",
				   oldCategory = CATEGORY_GENERAL)
	public static boolean enableDraconicBowExplosion = false;

	@ConfigBoolean(category = CATEGORY_OTHER,
				   comment = "Включить функцию \"Place item in world\"",
				   oldCategory = CATEGORY_GENERAL)
	public static boolean enablePlaceItemInWorld = false;

	@ConfigBoolean(category = CATEGORY_OTHER,
			comment = "Только OP могут использовать Спавн камет, так же AdminSpawnEgg",
			oldCategory = CATEGORY_GENERAL)
	public static boolean useCreativeStructureSpawner = true;

	@ConfigString(category = CATEGORY_OTHER,
				  comment = "Разрешение для функции \"Place item in world\"",
				  oldCategory = CATEGORY_GENERAL)
	public static String placeItemInWorldPermission = "draconicevolution.placeitem";

	@ConfigBoolean(category = CATEGORY_OTHER,
				   comment = "Включить спавн скелетов-иссушителей в спавнере",
				   oldCategory = CATEGORY_GENERAL)
	public static boolean enableSpawnerWitcherSkeleton = true;

	@ConfigBoolean(category = CATEGORY_OTHER, comment = "Включить улучшение спавнера", oldCategory = CATEGORY_GENERAL)
	public static boolean enableSpawnerUpgrades = true;

	@ConfigBoolean(category = CATEGORY_OTHER, comment = "Включить спавнер", oldCategory = CATEGORY_GENERAL)
	public static boolean enableSpawner = true;

	@ConfigBoolean(category = CATEGORY_OTHER,
				   comment = "Включить притягивание опыта магнитом",
				   oldCategory = CATEGORY_GENERAL)
	public static boolean enableMagnetXp = true;

	@ConfigBoolean(category = CATEGORY_OTHER,
				   comment = "Включить возможность заряда стаков Draconium Block",
				   oldCategory = CATEGORY_GENERAL)
	public static boolean enableDraconiumBlockStackCharge = true;

	@ConfigBoolean(category = CATEGORY_OTHER,
				   comment = "Расширенная проверка наличия взрыва для Dragon Heart",
				   oldCategory = CATEGORY_GENERAL)
	public static boolean extendedDragonHeartActivation = false;

	@ConfigInt(category = CATEGORY_OTHER,
			   comment = "Максимальный радиус разрушения посохом",
			   oldCategory = CATEGORY_GENERAL,
			   min = 0)
	public static int staffMaxRange = 11;

	@ConfigInt(category = CATEGORY_OTHER,
			   comment = "Максимальный радиус разрушения киркой",
			   oldCategory = CATEGORY_GENERAL,
			   min = 0)
	public static int pickaxeMaxRange = 11;

	@ConfigBoolean(category = CATEGORY_OTHER,
				   comment = "Charm of Dislocation может перемещать только игрока",
				   oldCategory = CATEGORY_GENERAL)
	public static boolean teleporterSelfOnly = false;

	@ConfigBoolean(category = CATEGORY_OTHER,
				   comment = "Проверка приватов при телепортации",
				   oldCategory = CATEGORY_GENERAL)
	public static boolean teleporterCheckRegion = true;

	@ConfigBoolean(category = CATEGORY_OTHER, comment = "Игнорирование кулдауна получения урона у цели")
	public static boolean ignoreHurtResistantTime = true;

	@ConfigBoolean(category = CATEGORY_OTHER, comment = "Включить улучшение Dig Speed")
	public static boolean enableDigSpeedUpgrade = true;

	static
	{
		teleporterBlackList.addRaw(Collections.singletonList("noppes.npcs.entity.EntityNPCInterface"));
		spawnerBlackList.addRaw(Collections.singletonList("noppes.npcs.entity.EntityNPCInterface"));

		teleporterBlackList.addRaw(Collections.singletonList("thaumcraft.common.entities.golems.EntityGolemBase"));
		spawnerBlackList.addRaw(Collections.singletonList("thaumcraft.common.entities.golems.EntityGolemBase"));

		ConfigUtils.readConfig(EventConfig.class);
	}
}
