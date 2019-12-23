package com.enderio.core.common.network;

import com.google.common.reflect.TypeToken;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public abstract class MessageTileEntity<T extends TileEntity> implements IMessage
{

	protected int x;
	protected int y;
	protected int z;

	protected MessageTileEntity()
	{
	}

	protected MessageTileEntity(T tile)
	{
		this.x = tile.xCoord;
		this.y = tile.yCoord;
		this.z = tile.zCoord;
	}

	@Override
	public void toBytes(ByteBuf buf)
	{
		buf.writeInt(this.x);
		buf.writeInt(this.y);
		buf.writeInt(this.z);
	}

	@Override
	public void fromBytes(ByteBuf buf)
	{
		this.x = buf.readInt();
		this.y = buf.readInt();
		this.z = buf.readInt();
	}

	@SuppressWarnings("unchecked")
	protected T getTileEntity(World world)
	{
		if (world == null)
			return null;

		// TODO gamerforEA code start
		if (!world.blockExists(this.x, this.y, this.z))
			return null;
		// TODO gamerforEA code end

		TileEntity te = world.getTileEntity(this.x, this.y, this.z);
		if (te == null)
			return null;
		TypeToken<?> teType = TypeToken.of(this.getClass()).resolveType(MessageTileEntity.class.getTypeParameters()[0]);
		if (teType.isAssignableFrom(te.getClass()))
			return (T) te;
		return null;
	}

	protected World getWorld(MessageContext ctx)
	{
		return ctx.getServerHandler().playerEntity.worldObj;
	}
}
