package net.divinerpg.items.vanilla;

import com.gamerforea.divinerpg.ModUtils;
import cpw.mods.fml.common.FMLCommonHandler;
import net.divinerpg.items.base.ItemMod;
import net.divinerpg.utils.TooltipLocalizer;
import net.divinerpg.utils.tabs.DivineRPGTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S07PacketRespawn;
import net.minecraft.network.play.server.S1DPacketEntityEffect;
import net.minecraft.network.play.server.S2BPacketChangeGameState;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import java.util.Iterator;
import java.util.List;

public class ItemTeleportationCrystal extends ItemMod
{
	public ItemTeleportationCrystal()
	{
		super("teleportationCrystal");
		this.setCreativeTab(DivineRPGTabs.utility);
		this.setMaxStackSize(1);
		this.setMaxDamage(10);
	}

	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player)
	{
		if (player instanceof EntityPlayerMP && !player.isRiding())
		{
			EntityPlayerMP playerMP = (EntityPlayerMP) player;
			ServerConfigurationManager configManager = playerMP.mcServer.getConfigurationManager();
			WorldServer targetWorld = configManager.getServerInstance().worldServerForDimension(0);

			// TODO gamerforEA code start
			if (targetWorld == null)
				return stack;
			// TODO gamerforEA code end

			if (player.dimension != 0)
			{
				// TODO gamerforEA code start
				if (ModUtils.cantTeleport(player, targetWorld, player.posX, player.posY, player.posZ))
					return stack;
				// TODO gamerforEA code end

				transferPlayerToDimension(playerMP, 0);
			}
			else
			{
				// TODO gamerforEA code start
				Vec3 spawnPoint = getSpawnPoint(player, targetWorld);
				if (spawnPoint == null)
					spawnPoint = Vec3.createVectorHelper(player.posX, player.posY, player.posZ);
				if (ModUtils.cantTeleport(player, targetWorld, spawnPoint.xCoord, spawnPoint.yCoord, spawnPoint.zCoord))
					return stack;
				// TODO gamerforEA code end

				movePlayerToSpawn(playerMP, targetWorld);
			}
			player.setPositionAndUpdate(player.posX, player.posY, player.posZ);
			if (!player.capabilities.isCreativeMode)
				stack.damageItem(1, player);
		}
		return stack;
	}

	public static void transferPlayerToWorld(EntityPlayerMP player, WorldServer oldWorld, WorldServer newWorld)
	{
		double moveFactor = oldWorld.provider.getMovementFactor() / newWorld.provider.getMovementFactor();
		double x = player.posX * moveFactor;
		double z = player.posZ * moveFactor;
		x = MathHelper.clamp_double(x, -29999872, 29999872);
		z = MathHelper.clamp_double(z, -29999872, 29999872);
		if (player.isEntityAlive())
		{
			player.setLocationAndAngles(x, player.posY, z, player.rotationYaw, player.rotationPitch);
			newWorld.spawnEntityInWorld(player);
			newWorld.updateEntityWithOptionalForce(player, false);
		}
		player.setWorld(newWorld);
	}

	// TODO gamerforEA code start
	private static Vec3 getSpawnPoint(EntityPlayer player, World world)
	{
		ChunkCoordinates bedSpawn = player.getBedLocation(0);
		if (bedSpawn != null)
		{
			ChunkCoordinates safeBedSpawn = EntityPlayer.verifyRespawnCoordinates(world, bedSpawn, true);
			if (safeBedSpawn != null)
			{
				player.setSpawnChunk(bedSpawn, true);
				return Vec3.createVectorHelper(safeBedSpawn.posX + 0.5, safeBedSpawn.posY + 0.1, safeBedSpawn.posZ + 0.5);
			}
		}
		else
		{
			ChunkCoordinates worldSpawn = world.getSpawnPoint();
			return Vec3.createVectorHelper(worldSpawn.posX + 0.5, worldSpawn.posY + 0.1, worldSpawn.posZ + 0.5);
		}
		return null;
	}
	// TODO gamerforEA code end

	public static void movePlayerToSpawn(EntityPlayerMP player, WorldServer worldServer)
	{
		/* TODO gamerforEA code replace, old code:
		ChunkCoordinates bedSpawn = player.getBedLocation(0);
		if (bedSpawn != null)
		{
			ChunkCoordinates safeBedSpawn = EntityPlayer.verifyRespawnCoordinates(worldServer, bedSpawn, true);
			if (safeBedSpawn != null)
			{
				player.setLocationAndAngles((double) ((float) safeBedSpawn.posX + 0.5F), (double) ((float) safeBedSpawn.posY + 0.1F), (double) ((float) safeBedSpawn.posZ + 0.5F), 0.0F, 0.0F);
				player.setSpawnChunk(bedSpawn, true);
			}
			else
				player.playerNetServerHandler.sendPacket(new S2BPacketChangeGameState(0, 0.0F));
		}
		else
		{
			ChunkCoordinates worldSpawn = worldServer.getSpawnPoint();
			player.setLocationAndAngles((double) ((float) worldSpawn.posX + 0.5F), (double) ((float) worldSpawn.posY + 0.1F), (double) ((float) worldSpawn.posZ + 0.5F), 0.0F, 0.0F);
		} */
		Vec3 spawnPoint = getSpawnPoint(player, worldServer);
		if (spawnPoint == null)
			player.playerNetServerHandler.sendPacket(new S2BPacketChangeGameState(0, 0));
		else
			player.setLocationAndAngles(spawnPoint.xCoord, spawnPoint.yCoord, spawnPoint.zCoord, 0, 0);
		// TODO gamerforEA code end

		worldServer.theChunkProviderServer.loadChunk((int) player.posX >> 4, (int) player.posZ >> 4);
		while (!worldServer.getCollidingBoundingBoxes(player, player.boundingBox).isEmpty())
		{
			player.setPosition(player.posX, player.posY + 1.0D, player.posZ);
		}
	}

	public static void transferPlayerToDimension(EntityPlayerMP player, int newDimension)
	{
		ServerConfigurationManager configManager = player.mcServer.getConfigurationManager();
		int oldDimension = player.dimension;

		WorldServer oldWorldServer = configManager.getServerInstance().worldServerForDimension(oldDimension);
		WorldServer newWorldServer = configManager.getServerInstance().worldServerForDimension(0);

		player.dimension = newDimension;
		player.playerNetServerHandler.sendPacket(new S07PacketRespawn(player.dimension, player.worldObj.difficultySetting, player.worldObj.getWorldInfo().getTerrainType(), player.theItemInWorldManager.getGameType()));
		oldWorldServer.removePlayerEntityDangerously(player);
		player.isDead = false;

		transferPlayerToWorld(player, oldWorldServer, newWorldServer);
		configManager.func_72375_a(player, oldWorldServer);

		movePlayerToSpawn(player, newWorldServer);

		player.playerNetServerHandler.setPlayerLocation(player.posX, player.posY, player.posZ, player.rotationYaw, player.rotationPitch);
		player.theItemInWorldManager.setWorld(newWorldServer);
		configManager.updateTimeAndWeatherForPlayer(player, newWorldServer);
		configManager.syncPlayerInventory(player);
		Iterator<PotionEffect> iterator = player.getActivePotionEffects().iterator();
		while (iterator.hasNext())
		{
			PotionEffect potioneffect = iterator.next();
			player.playerNetServerHandler.sendPacket(new S1DPacketEntityEffect(player.getEntityId(), potioneffect));
		}
		FMLCommonHandler.instance().firePlayerChangedDimensionEvent(player, oldDimension, newDimension);
	}

	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean par4)
	{
		list.add("Teleport to spawn point");
		list.add(TooltipLocalizer.usesRemaining(stack.getMaxDamage() - stack.getItemDamage()));
	}
}
