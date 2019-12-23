package eu.thesociety.DragonbornSR.DragonsRadioMod.network;

import com.gamerforea.eventhelper.util.EventUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import eu.thesociety.DragonbornSR.DragonsRadioMod.Block.BlockSpeaker;
import eu.thesociety.DragonbornSR.DragonsRadioMod.Block.TileEntity.TileEntityRadio;
import eu.thesociety.DragonbornSR.DragonsRadioMod.network.Message.MessageTERadioBlock;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class RadioBlockMessageHandlerServer implements IMessageHandler<MessageTERadioBlock, IMessage>
{
	@Override
	public IMessage onMessage(MessageTERadioBlock message, MessageContext ctx)
	{
		/* TODO gamerforEA code replace, old code:
		PacketHandler.INSTANCE.sendToAll(message);
		if (!ModRadioBlock.registered)
		{
			Cmsg.writeline("Reminder: Unregistered server. Radios will NOT work! This might also be an error!!!");
			return null;
		}

		World w = message.world;
		WorldServer targetWorld = null;
		TileEntity tile = null;
		WorldServer[] ws = MinecraftServer.getServer().worldServers;

		for (WorldServer s : ws)
		{
			if (s.provider.dimensionId == message.dim)
			{
				targetWorld = s;
				tile = s.getTileEntity((int) message.x, (int) message.y, (int) message.z);
			}
		}

		if (tile instanceof TileEntityRadio)
		{
			if (message.mode == 15)
			{
				((TileEntityRadio) tile).addSpeaker(targetWorld, message.tx, message.ty, message.tz);
				return null;
			}

			if (message.mode == 11 || message.mode == 14)
				((TileEntityRadio) tile).listenToRedstone = true;

			if (message.mode == 12 || message.mode == 13)
				((TileEntityRadio) tile).listenToRedstone = false;

			if (message.volume > 0.0F && message.volume <= 1.0F)
				((TileEntityRadio) tile).volume = message.volume;

			((TileEntityRadio) tile).streamURL = message.streamURL;
			if (message.mode == 1 || message.mode == 13 || message.mode == 14)
			{
				((TileEntityRadio) tile).isPlaying = message.isPlaying;
				if (message.isPlaying)
					((TileEntityRadio) tile).startStream();
				else
					((TileEntityRadio) tile).stopStream();
			}
		} */
		EntityPlayerMP player = ctx.getServerHandler().playerEntity;
		if (player.dimension != message.dim)
			return null;

		World world = player.worldObj;
		int x = (int) message.x;
		int y = (int) message.y;
		int z = (int) message.z;
		if (player.getDistanceSq(x + 0.5, y + 0.5, z + 0.5) > 10 * 10)
			return null;

		if (!world.blockExists(x, y, z))
			return null;

		TileEntity tile = world.getTileEntity(x, y, z);
		if (!(tile instanceof TileEntityRadio))
			return null;

		TileEntityRadio radio = (TileEntityRadio) tile;
		if (!player.getUniqueID().equals(((TileEntityRadio) tile).getLastPlayerId()))
		{
			if (message.mode != 15)
				return null;

			int tx = (int) message.tx;
			int ty = (int) message.ty;
			int tz = (int) message.tz;
			if (EventUtils.cantBreak(player, tx, ty, tz))
				return null;
		}

		// TODO gamerforEA code start
		if (message.mode == 15)
		{
			int tx = (int) message.tx;
			int ty = (int) message.ty;
			int tz = (int) message.tz;

			int viewDistance = player.mcServer.getConfigurationManager().getViewDistance();
			int maxDistance = (viewDistance + 1) * 16;
			if (player.getDistanceSq(tx, ty, tz) > maxDistance * maxDistance)
				return null;

			if (!world.blockExists(tx, ty, tz))
				return null;
			if (!(world.getBlock(tx, ty, tz) instanceof BlockSpeaker))
				return null;
		}
		// TODO gamerforEA code end

		PacketHandler.INSTANCE.sendToAll(message);

		if (message.mode == 15)
		{
			radio.addSpeaker(world, message.tx, message.ty, message.tz);
			return null;
		}

		if (message.mode == 11 || message.mode == 14)
			radio.listenToRedstone = true;

		if (message.mode == 12 || message.mode == 13)
			radio.listenToRedstone = false;

		if (message.volume > 0.0F && message.volume <= 1.0F)
			radio.volume = message.volume;

		radio.streamURL = message.streamURL;
		if (message.mode == 1 || message.mode == 13 || message.mode == 14)
		{
			radio.isPlaying = message.isPlaying;
			if (message.isPlaying)
				radio.startStream();
			else
				radio.stopStream();
		}
		// TODO gamerforEA code end

		return null;
	}
}
