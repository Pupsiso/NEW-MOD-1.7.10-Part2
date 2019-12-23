package com.gamerforea.emt;

import com.gamerforea.eventhelper.fake.FakePlayerContainer;
import com.gamerforea.eventhelper.fake.FakePlayerContainerEntity;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

public final class EntityLightningBoltByPlayer extends EntityLightningBolt
{
	public final FakePlayerContainer fake = new FakePlayerContainerEntity(ModUtils.profile, this);

	public EntityLightningBoltByPlayer(EntityPlayer player, World world, double x, double y, double z)
	{
		super(world, x, y, z);
		if (player != null)
			this.fake.setRealPlayer(player);
	}
}
