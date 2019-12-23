package com.gamerforea.emt;

import com.gamerforea.eventhelper.util.FastUtils;
import com.mojang.authlib.GameProfile;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;

import java.util.UUID;

public final class ModUtils
{
	public static final GameProfile profile = new GameProfile(UUID.fromString("817349db-db1c-40c2-9c6b-cf3932f6fe91"), "[EMT]");
	private static FakePlayer player = null;

	public static final FakePlayer getModFake(World world)
	{
		if (player == null)
			player = FastUtils.getFake(world, profile);
		else
			player.worldObj = world;

		return player;
	}
}