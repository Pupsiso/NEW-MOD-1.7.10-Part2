package com.brandon3055.draconicevolution.common.network;

import com.brandon3055.brandonscore.common.utills.DataUtills;
import com.brandon3055.draconicevolution.common.container.ContainerDataSync;
import com.brandon3055.draconicevolution.common.tileentities.TileObjectSync;
import com.gamerforea.eventhelper.util.EventUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;

/**
 * Created by Brandon on 14/11/2014.
 */
public class TileObjectPacket implements IMessage
{

	int x;
	int y;
	int z;
	short index;
	short dataType = -1;
	Object object;
	boolean isContainerPacket;

	/**
	 * Used for Tile and Container synchronization
	 */
	public TileObjectPacket()
	{
	}

	public TileObjectPacket(TileObjectSync tile, byte dataType, int index, Object object)
	{
		this.isContainerPacket = tile == null;
		if (!this.isContainerPacket)
		{
			this.x = tile.xCoord;
			this.y = tile.yCoord;
			this.z = tile.zCoord;
		}
		this.dataType = dataType;
		this.object = object;
		this.index = (short) index;
	}

	@Override
	public void toBytes(ByteBuf bytes)
	{
		bytes.writeBoolean(this.isContainerPacket);

		if (!this.isContainerPacket)
		{
			bytes.writeInt(this.x);
			bytes.writeInt(this.y);
			bytes.writeInt(this.z);
		}

		bytes.writeByte(this.dataType);
		bytes.writeShort(this.index);

		DataUtills.instance.writeObjectToBytes(bytes, this.dataType, this.object);

	}

	@Override
	public void fromBytes(ByteBuf bytes)
	{
		this.isContainerPacket = bytes.readBoolean();

		if (!this.isContainerPacket)
		{
			this.x = bytes.readInt();
			this.y = bytes.readInt();
			this.z = bytes.readInt();
		}

		this.dataType = bytes.readByte();
		this.index = bytes.readShort();

		this.object = DataUtills.instance.readObjectFromBytes(bytes, this.dataType);
	}

	public static class Handler implements IMessageHandler<TileObjectPacket, IMessage>
	{

		@Override
		public IMessage onMessage(TileObjectPacket message, MessageContext ctx)
		{
			if (ctx.side == Side.CLIENT)
			{
				if (message.isContainerPacket)
				{
					ContainerDataSync container = Minecraft.getMinecraft().thePlayer.openContainer instanceof ContainerDataSync ? (ContainerDataSync) Minecraft.getMinecraft().thePlayer.openContainer : null;
					if (container == null)
						return null;
					container.receiveSyncData(message.index, (Integer) message.object);
				}
				else
				{
					if (!(Minecraft.getMinecraft().theWorld.getTileEntity(message.x, message.y, message.z) instanceof TileObjectSync))
						return null;
					((TileObjectSync) Minecraft.getMinecraft().theWorld.getTileEntity(message.x, message.y, message.z)).receiveObjectFromServer(message.index, message.object);
				}
			}
			else if (message.isContainerPacket)
			{
				ContainerDataSync container = ctx.getServerHandler().playerEntity.openContainer instanceof ContainerDataSync ? (ContainerDataSync) ctx.getServerHandler().playerEntity.openContainer : null;
				if (container == null)
					return null;
				container.receiveSyncData(message.index, (Integer) message.object);
			}
			else
			{
				int x = message.x;
				int y = message.y;
				int z = message.z;
				TileEntity tile = ctx.getServerHandler().playerEntity.worldObj.getTileEntity(x, y, z);

				if (tile instanceof TileObjectSync)
				{
					// TODO gamerforEA code start
					if (EventUtils.cantBreak(ctx.getServerHandler().playerEntity, x, y, z))
						return null;
					// TODO gamerforEA code end

					((TileObjectSync) tile).receiveObjectFromClient(message.index, message.object);
				}
			}
			return null;
		}
	}
}
