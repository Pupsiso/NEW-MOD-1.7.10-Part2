package com.carpentersblocks.util.handler;

import com.carpentersblocks.CarpentersBlocks;
import com.carpentersblocks.network.ICarpentersPacket;
import com.carpentersblocks.network.PacketActivateBlock;
import com.carpentersblocks.network.PacketEnrichPlant;
import com.carpentersblocks.network.PacketSlopeSelect;
import com.carpentersblocks.util.ModLogger;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent.ServerCustomPacketEvent;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PacketHandler
{
	private static final List<Class> packetCarrier;

	static
	{
		packetCarrier = new ArrayList<Class>();
		packetCarrier.add(PacketActivateBlock.class);
		packetCarrier.add(PacketEnrichPlant.class);
		packetCarrier.add(PacketSlopeSelect.class);
	}

	@SubscribeEvent
	public void onServerPacket(ServerCustomPacketEvent event) throws IOException
	{
		try (ByteBufInputStream bbis = new ByteBufInputStream(event.packet.payload()))
		{
			EntityPlayerMP player = ((NetHandlerPlayServer) event.handler).playerEntity;
			int packetId = bbis.readInt();
			if (packetId < packetCarrier.size())
				try
				{
					ICarpentersPacket packetClass = (ICarpentersPacket) packetCarrier.get(packetId).newInstance();
					packetClass.processData(player, bbis);
				}
				catch (Exception e)
				{
					// TODO gamerforEA code replace, old code:
					// e.printStackTrace();
					player.playerNetServerHandler.kickPlayerFromServer(e.toString());
					// TODO gamerforEA code end
				}
			else
			{
				String msg = "Encountered out of range packet Id: " + packetId;
				ModLogger.log(Level.WARN, msg);

				// TODO gamerforEA code start
				player.playerNetServerHandler.kickPlayerFromServer(msg);
				// TODO gamerforEA code end
			}
		}
	}

	public static void sendPacketToServer(ICarpentersPacket packet)
	{
		ByteBuf buffer = Unpooled.buffer();
		buffer.writeInt(packetCarrier.indexOf(packet.getClass()));

		try
		{
			packet.appendData(buffer);
		}
		catch (IOException ignored)
		{
		}

		CarpentersBlocks.channel.sendToServer(new FMLProxyPacket(new C17PacketCustomPayload(CarpentersBlocks.MODID, buffer)));
	}

}
