package com.gamerforea.draconicevolution;

import cpw.mods.fml.common.Loader;
import java.io.File;
import net.minecraftforge.common.config.Configuration;

public final class EventConfig {
	public static int staffMaxRange = 2;
	public static int staffDepthMaxRange = 0;
	public static int pickaxeMaxRange = 1;
	public static int pickaxeDepthMaxRange = 0;
	public static int pickaxeWyvernMaxRange = 0;
	public static int pickaxeDepthWyvernMaxRange = 0;
	public static boolean enableDigSpeedUpgrade = true;

	public static void load() {
		try {
			Configuration cfg = new Configuration(new File(Loader.instance().getConfigDir(), "Events/DraconicEvolution.cfg"));
			String c = "general";
			staffMaxRange = cfg.getInt("staffMaxRange", c, staffMaxRange, 0, Integer.MAX_VALUE, "Максимальный радиус разрушения посохом силы дракона");
			staffDepthMaxRange = cfg.getInt("staffDepthMaxRange", c, staffDepthMaxRange, 0, Integer.MAX_VALUE, "Максимальный радиус разрушения посохом силы дракона в глубину");
			pickaxeMaxRange = cfg.getInt("pickaxeMaxRange", c, pickaxeMaxRange, 0, Integer.MAX_VALUE, "Максимальный радиус разрушения киркой дракона");
			pickaxeDepthMaxRange = cfg.getInt("pickaxeDepthMaxRange", c, pickaxeDepthMaxRange, 0, Integer.MAX_VALUE, "Максимальный радиус разрушения киркой дракона в глубину");
			pickaxeWyvernMaxRange = cfg.getInt("pickaxeWyvernMaxRange", c, pickaxeWyvernMaxRange, 0, Integer.MAX_VALUE, "Максимальный радиус разрушения киркой виверны");
			pickaxeDepthWyvernMaxRange = cfg.getInt("pickaxeDepthWyvernMaxRange", c, pickaxeDepthWyvernMaxRange, 0, Integer.MAX_VALUE, "Максимальный радиус разрушения киркой виверны в глубину");
			enableDigSpeedUpgrade = cfg.getBoolean("enableDigSpeedUpgrade", c, enableDigSpeedUpgrade, "Включить улучшение Dig Speed");
			cfg.save();
		} catch (Throwable var2) {
			System.err.println("Failed load config. Use default values.");
			var2.printStackTrace();
		}

	}

	static {
		load();
	}
}
