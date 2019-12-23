package crazypants.enderio.enderface;

import com.enderio.core.common.Handlers.Handler;
import com.gamerforea.enderio.EventConfig;
import com.gamerforea.enderio.ExtendedPlayer;
import cpw.mods.fml.common.eventhandler.Event.Result;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.PlayerTickEvent;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.ChunkPosition;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerOpenContainerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Handler
public enum EnderIOController
{
	INSTANCE;

	private TObjectIntMap<UUID> openedContainers = new TObjectIntHashMap<UUID>();
	private int clientWindowId;
	private boolean locked = false;

	// TODO gamerforEA code start
	private final Map<UUID, ChunkPosition> blockPositions = new HashMap<UUID, ChunkPosition>();

	public void addBlockPos(EntityPlayer player, ChunkPosition blockPos)
	{
		this.blockPositions.put(player.getGameProfile().getId(), blockPos);
	}

	@SubscribeEvent
	public void onEntityConstructing(EntityEvent.EntityConstructing event)
	{
		if (ExtendedPlayer.isUsed() && event.entity instanceof EntityPlayer)
			ExtendedPlayer.register((EntityPlayer) event.entity);
	}

	@SubscribeEvent
	public void onPlayerClone(PlayerEvent.Clone event)
	{
		if (ExtendedPlayer.isUsed())
		{
			ExtendedPlayer from = ExtendedPlayer.get(event.original);
			ExtendedPlayer to = ExtendedPlayer.get(event.entityPlayer);
			NBTTagCompound nbt = new NBTTagCompound();
			from.saveNBTData(nbt);
			to.loadNBTData(nbt);
		}
	}
	// TODO gamerforEA code end

	public void addContainer(EntityPlayerMP player, Container opened)
	{
		UUID playerId = player.getGameProfile().getId();
		this.openedContainers.put(playerId, opened.windowId);

		// TODO gamerforEA code start
		this.blockPositions.remove(playerId);
		// TODO gamerforEA code end
	}

	void lockAndWaitForChange(int windowId)
	{
		this.clientWindowId = windowId;
		this.locked = true;
	}

	void unlock()
	{
		this.locked = false;
	}

	@SubscribeEvent
	public void onContainerTick(PlayerOpenContainerEvent event)
	{
		// TODO gamerforEA code start
		if (EventConfig.travelStuffStrictGui)
			return;
		// TODO gamerforEA code end

		EntityPlayer player = event.entityPlayer;
		Container c = player.openContainer;
		if (c != null && !(c instanceof ContainerPlayer) && (c.windowId == this.clientWindowId || this.openedContainers.containsValue(c.windowId)))
		{
			// TODO gamerforEA code replace, old code:
			// event.setResult(Result.ALLOW);
			if (!event.canInteractWith)
			{
				Container container = player.openContainer;
				if (container != null && container != player.inventoryContainer)
				{
					ChunkPosition blockPos = this.blockPositions.get(player.getGameProfile().getId());
					if (blockPos != null)
					{
						int x = blockPos.chunkPosX;
						int y = blockPos.chunkPosY;
						int z = blockPos.chunkPosZ;
						if (player.worldObj.blockExists(x, y, z))
						{
							double posX = player.posX;
							double posY = player.posY;
							double posZ = player.posZ;
							try
							{
								player.posX = x;
								player.posY = y;
								player.posZ = z;
								if (container.canInteractWith(player))
									event.setResult(Result.ALLOW);
							}
							finally
							{
								player.posX = posX;
								player.posY = posY;
								player.posZ = posZ;
							}
						}
					}
				}
			}
			// TODO gamerforEA code end
		}
	}

	@SubscribeEvent
	public void onPlayerTick(PlayerTickEvent event)
	{
		if (event.phase == Phase.END)
			if (event.side.isServer())
			{
				UUID playerId = event.player.getGameProfile().getId();
				int windowId = this.openedContainers.get(playerId);
				if (event.player.openContainer == null || event.player.openContainer.windowId != windowId)
				{
					this.openedContainers.remove(playerId);

					// TODO gamerforEA code start
					this.blockPositions.remove(playerId);
					// TODO gamerforEA code end
				}
			}
			else
			{
				int windowId = event.player.openContainer.windowId;
				if (windowId != this.clientWindowId && this.locked)
				{
					this.clientWindowId = windowId;
					this.locked = false;
					System.out.println("Unlocked and set windowId to " + this.clientWindowId);
				}
			}
	}
}
