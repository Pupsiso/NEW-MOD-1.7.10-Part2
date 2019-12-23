package crazypants.enderio.teleport.packet;

import com.enderio.core.common.util.BlockCoord;
import com.enderio.core.common.util.Util;
import com.enderio.core.common.vecmath.Vector3d;
import com.gamerforea.enderio.EventConfig;
import com.gamerforea.eventhelper.util.EventUtils;
import com.gamerforea.eventhelper.util.PlayerCooldownManager;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import crazypants.enderio.api.teleport.IItemOfTravel;
import crazypants.enderio.api.teleport.ITravelAccessable;
import crazypants.enderio.api.teleport.TeleportEntityEvent;
import crazypants.enderio.api.teleport.TravelSource;
import crazypants.enderio.config.Config;
import crazypants.enderio.item.darksteel.ItemDarkSteelPickaxe;
import crazypants.enderio.item.darksteel.ItemDarkSteelSword;
import crazypants.enderio.teleport.ItemTravelStaff;
import crazypants.enderio.teleport.TravelController;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.MinecraftForge;

public class PacketTravelEvent implements IMessage, IMessageHandler<PacketTravelEvent, IMessage>
{

	int x;
	int y;
	int z;
	int powerUse;
	boolean conserveMotion;
	int entityId;
	int source;

	public PacketTravelEvent()
	{
	}

	public PacketTravelEvent(Entity entity, int x, int y, int z, int powerUse, boolean conserveMotion, TravelSource source)
	{
		this.x = x;
		this.y = y;
		this.z = z;
		this.powerUse = powerUse;
		this.conserveMotion = conserveMotion;
		this.entityId = entity instanceof EntityPlayer ? -1 : entity.getEntityId();
		this.source = source.ordinal();
	}

	@Override
	public void toBytes(ByteBuf buf)
	{
		buf.writeInt(this.x);
		buf.writeInt(this.y);
		buf.writeInt(this.z);
		buf.writeInt(this.powerUse);
		buf.writeBoolean(this.conserveMotion);
		buf.writeInt(this.entityId);
		buf.writeInt(this.source);
	}

	@Override
	public void fromBytes(ByteBuf buf)
	{
		this.x = buf.readInt();
		this.y = buf.readInt();
		this.z = buf.readInt();
		this.powerUse = buf.readInt();
		this.conserveMotion = buf.readBoolean();
		this.entityId = buf.readInt();
		this.source = buf.readInt();
	}

	@Override
	public IMessage onMessage(PacketTravelEvent message, MessageContext ctx)
	{
		EntityPlayerMP sender = ctx.getServerHandler().playerEntity;
		Entity toTp = message.entityId == -1 ? sender : sender.worldObj.getEntityByID(message.entityId);
		int x = message.x, y = message.y, z = message.z;
		TravelSource source = TravelSource.values()[message.source];

		// TODO gamerforEA add sender:EntityPlayerMP parameter
		doServerTeleport(toTp, x, y, z, message.powerUse, message.conserveMotion, source, sender);

		return null;
	}

	// TODO gamerforEA code start
	private static final PlayerCooldownManager BLINK_COOLDOWN_MANAGER = new PlayerCooldownManager(Config.travelStaffBlinkPauseTicks);

	private static boolean isEquippedTravelStuffPickSword(EntityPlayer player)
	{
		ItemStack heldStack = player.getHeldItem();
		if (heldStack != null && heldStack.stackSize > 0)
		{
			Item heldItem = heldStack.getItem();
			return heldItem instanceof ItemTravelStaff || heldItem instanceof ItemDarkSteelPickaxe || heldItem instanceof ItemDarkSteelSword;
		}
		return false;
	}

	private static boolean canTravelToSelectedTarget(EntityPlayer player, int x, int y, int z, TravelSource source)
	{
		if (Config.travelAnchorEnabled)
		{
			TileEntity tile = player.worldObj.getTileEntity(x, y, z);
			if (!(tile instanceof ITravelAccessable) || ((ITravelAccessable) tile).canBlockBeAccessed(player))
			{
				BlockCoord coord = new BlockCoord(x, y, z);
				TravelController travelController = TravelController.instance;
				if (travelController.getRequiredPower(player, source, coord) >= 0)
					if (travelController.isInRangeTarget(player, coord, source.getMaxDistanceTravelledSq()))
						return travelController.isValidTarget(player, coord, source);
			}
		}
		return false;
	}

	public static boolean doServerTeleport(Entity toTp, int x, int y, int z, int powerUse, boolean conserveMotion, TravelSource source)
	{
		return doServerTeleport(toTp, x, y, z, powerUse, conserveMotion, source, null);
	}
	// TODO gamerforEA code end

	// TODO gamerforEA add sender:EntityPlayerMP parameter
	public static boolean doServerTeleport(Entity toTp, int x, int y, int z, int powerUse, boolean conserveMotion, TravelSource source, EntityPlayerMP sender)
	{
		// TODO gamerforEA code start
		if (toTp == null)
			return false;
		if (EventConfig.paranoidNetworkTeleportFix && sender != null && sender != toTp)
			return false;
		// TODO gamerforEA code end

		EntityPlayer player = toTp instanceof EntityPlayer ? (EntityPlayer) toTp : null;

		TeleportEntityEvent evt = new TeleportEntityEvent(toTp, source, x, y, z);
		if (MinecraftForge.EVENT_BUS.post(evt))
			return false;

		x = evt.targetX;
		y = evt.targetY;
		z = evt.targetZ;

		// TODO gamerforEA code start
		if (player != null)
		{
			if (EventConfig.networkTeleportFix && sender != null)
			{
				boolean canTeleport = false;

				switch (source)
				{
					case STAFF:
						if (isEquippedTravelStuffPickSword(player) && canTravelToSelectedTarget(player, x, y, z, source))
							canTeleport = true;
						break;
					case STAFF_BLINK:
						if (Config.travelStaffBlinkEnabled)
						{
							Vector3d look = Util.getLookVecEio(player);
							double playerHeight = player.yOffset;
							double lookComp = -look.y * playerHeight;
							double maxDistance = Config.travelStaffMaxBlinkDistance + lookComp + 2;
							double maxDistanceSq = maxDistance * maxDistance;
							if (player.getDistanceSq(x, y, z) <= maxDistanceSq && isEquippedTravelStuffPickSword(player))
								canTeleport = BLINK_COOLDOWN_MANAGER.add(player);
						}
						break;
					case BLOCK:
						if (canTravelToSelectedTarget(player, x, y, z, source))
							canTeleport = true;
						break;
					case TELEPAD:
						// Teleport already handled in crazypants.enderio.teleport.telepad.TileTelePad.teleport -> crazypants.enderio.teleport.telepad.TileTelePad.serverTeleport
						break;
				}

				if (!canTeleport)
					return false;
			}

			if (EventConfig.blinkEvent && source == TravelSource.STAFF_BLINK)
				if (EventUtils.cantBreak(player, (int) player.posX, (int) player.posY, (int) player.posZ) || EventUtils.cantBreak(player, x, y, z))
					return false;
		}
		else if (EventConfig.networkTeleportFix && sender != null)
		{
			switch (source)
			{
				case STAFF:
				case BLOCK:
				case STAFF_BLINK:
				case TELEPAD:
					return false;
				default:
					break;
			}
		}
		// TODO gamerforEA code end

		toTp.worldObj.playSoundEffect(toTp.posX, toTp.posY, toTp.posZ, source.sound, 1.0F, 1.0F);

		toTp.playSound(source.sound, 1.0F, 1.0F);

		if (player != null)
			player.setPositionAndUpdate(x + 0.5, y + 1.1, z + 0.5);
		else
			toTp.setPosition(x, y, z);

		toTp.worldObj.playSoundEffect(x, y, z, source.sound, 1.0F, 1.0F);
		toTp.fallDistance = 0;

		if (player != null)
		{
			if (conserveMotion)
			{
				Vector3d velocityVex = Util.getLookVecEio(player);
				S12PacketEntityVelocity p = new S12PacketEntityVelocity(toTp.getEntityId(), velocityVex.x, velocityVex.y, velocityVex.z);
				((EntityPlayerMP) player).playerNetServerHandler.sendPacket(p);
			}

			if (powerUse > 0 && player.getCurrentEquippedItem() != null && player.getCurrentEquippedItem().getItem() instanceof IItemOfTravel)
			{
				ItemStack item = player.getCurrentEquippedItem().copy();
				((IItemOfTravel) item.getItem()).extractInternal(item, powerUse);
				toTp.setCurrentItemOrArmor(0, item);
			}
		}

		return true;
	}
}
