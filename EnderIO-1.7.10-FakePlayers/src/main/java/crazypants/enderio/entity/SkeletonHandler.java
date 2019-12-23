package crazypants.enderio.entity;

import com.gamerforea.enderio.EventConfig;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.EntityRegistry;
import crazypants.enderio.EnderIO;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;

public class SkeletonHandler
{
	// TODO gamerforEA code start
	@SubscribeEvent
	public void onSkeletonUpdate(LivingEvent.LivingUpdateEvent event)
	{
		if (!EventConfig.fixSpawnWitherSkeletons)
			return;

		EntityLivingBase entity = event.entityLiving;
		World world = entity.worldObj;
		if (!world.isRemote && entity.ticksExisted >= 20 && this.isWitherSkele(entity) && entity.isEntityAlive())
		{
			EntityWitherSkeleton witherSkeleton = new EntityWitherSkeleton((EntitySkeleton) entity);
			entity.setDead();
			world.spawnEntityInWorld(witherSkeleton);
		}
	}
	// TODO gamerforEA code end

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void skeletonJoinWorld(LivingSpawnEvent event)
	{
		// TODO gamerforEA code start
		if (EventConfig.fixSpawnWitherSkeletons)
			return;
		// TODO gamerforEA code end

		World world = event.world;
		EntityLivingBase entity = event.entityLiving;
		if (!world.isRemote && this.isWitherSkele(entity))
		{
			entity.setDead();
			world.spawnEntityInWorld(new EntityWitherSkeleton((EntitySkeleton) entity));
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void skeletonJoinWorld(EntityJoinWorldEvent event)
	{
		// TODO gamerforEA code start
		if (EventConfig.fixSpawnWitherSkeletons)
			return;
		// TODO gamerforEA code end

		World world = event.world;
		Entity entity = event.entity;
		if (!world.isRemote && this.isWitherSkele(entity) && !entity.isDead)
		{
			event.setCanceled(true);
			world.spawnEntityInWorld(new EntityWitherSkeleton((EntitySkeleton) entity));
		}
	}

	private boolean isWitherSkele(Entity entity)
	{
		return entity.getClass() == EntitySkeleton.class && ((EntitySkeleton) entity).getSkeletonType() == 1;
	}

	public static void registerSkeleton(EnderIO mod)
	{
		int entityID = EntityRegistry.findGlobalUniqueEntityId();
		EntityRegistry.registerGlobalEntityID(EntityWitherSkeleton.class, "witherSkeleton", entityID, 0x00003D, 0x751947);
		EntityRegistry.registerModEntity(EntityWitherSkeleton.class, "witherSkeleton", entityID, mod, 64, 3, true);
		MinecraftForge.EVENT_BUS.register(new SkeletonHandler());
	}
}
