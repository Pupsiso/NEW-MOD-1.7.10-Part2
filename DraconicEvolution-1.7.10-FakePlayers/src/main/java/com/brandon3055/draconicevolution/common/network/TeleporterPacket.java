package com.brandon3055.draconicevolution.common.network;

import com.brandon3055.brandonscore.BrandonsCore;
import com.brandon3055.brandonscore.common.utills.ItemNBTHelper;
import com.brandon3055.brandonscore.common.utills.Teleporter.TeleportLocation;
import com.brandon3055.draconicevolution.common.ModItems;
import com.brandon3055.draconicevolution.common.handler.ConfigHandler;
import com.gamerforea.eventhelper.util.EventUtils;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public class TeleporterPacket implements IMessage
{
	public static final int ADDDESTINATION = 0;
	public static final int REMOVEDESTINATION = 1;
	public static final int UPDATENAME = 2;
	public static final int UPDATELOCK = 3;
	public static final int CHANGESELECTION = 4;
	public static final int UPDATEOFFSET = 5;
	public static final int ADDFUEL = 6;
	public static final int UPDATEDESTINATION = 7;
	public static final int TELEPORT = 8;
	public static final int SCROLL = 9;
	public static final int MOVELOCATION = 10;

	private int data = 0;
	private boolean dataB;
	private byte function = -1;
	private TeleportLocation location;

	public TeleporterPacket()
	{
	}

	public TeleporterPacket(int function, int data, boolean b)
	{
		this.data = data;
		this.function = (byte) function;
		this.dataB = b;
	}

	public TeleporterPacket(TeleportLocation location, int function)
	{
		this.location = location;
		this.function = (byte) function;
	}

	public TeleporterPacket(TeleportLocation location, int function, int data)
	{
		this.data = data;
		this.location = location;
		this.function = (byte) function;
	}

	@Override
	public void toBytes(ByteBuf bytes)
	{
		bytes.writeByte(this.function);
		if (this.function == ADDDESTINATION || this.function == UPDATEDESTINATION)
		{
			bytes.writeDouble(this.location.getXCoord());
			bytes.writeDouble(this.location.getYCoord());
			bytes.writeDouble(this.location.getZCoord());
			bytes.writeInt(this.location.getDimension());
			bytes.writeFloat(this.location.getPitch());
			bytes.writeFloat(this.location.getYaw());
			ByteBufUtils.writeUTF8String(bytes, this.location.getName());
			//ByteBufUtils.writeUTF8String(bytes, location.getDimensionName());
			if (this.function == UPDATEDESTINATION)
				bytes.writeInt(this.data);
		}

		if (this.function == UPDATELOCK || this.function == MOVELOCATION)
		{
			bytes.writeInt(this.data);
			bytes.writeBoolean(this.dataB);
		}

		if (this.function == UPDATEOFFSET || this.function == CHANGESELECTION || this.function == REMOVEDESTINATION || this.function == ADDFUEL || this.function == SCROLL)
			bytes.writeInt(this.data);

		if (this.function == UPDATENAME)
		{
			ByteBufUtils.writeUTF8String(bytes, this.location.getName());
			bytes.writeInt(this.data);
		}

		if (this.function == TELEPORT)
			bytes.writeInt(this.data);
	}

	@Override
	public void fromBytes(ByteBuf bytes)
	{
		this.function = bytes.readByte();
		if (this.function == ADDDESTINATION || this.function == UPDATEDESTINATION)
		{
			this.location = new TeleportLocation();
			this.location.setXCoord(bytes.readDouble());
			this.location.setYCoord(bytes.readDouble());
			this.location.setZCoord(bytes.readDouble());
			this.location.setDimension(bytes.readInt());
			this.location.setPitch(bytes.readFloat());
			this.location.setYaw(bytes.readFloat());
			this.location.setName(ByteBufUtils.readUTF8String(bytes));
			//location.setDimentionName(ByteBufUtils.readUTF8String(bytes));
			if (this.function == UPDATEDESTINATION)
				this.data = bytes.readInt();
		}

		if (this.function == UPDATELOCK || this.function == MOVELOCATION)
		{
			this.data = bytes.readInt();
			this.dataB = bytes.readBoolean();
		}

		if (this.function == UPDATEOFFSET || this.function == CHANGESELECTION || this.function == REMOVEDESTINATION || this.function == ADDFUEL || this.function == SCROLL)
			this.data = bytes.readInt();

		if (this.function == UPDATENAME)
		{
			this.location = new TeleportLocation();
			this.location.setName(ByteBufUtils.readUTF8String(bytes));
			this.data = bytes.readInt();
		}

		if (this.function == TELEPORT)
			this.data = bytes.readInt();
	}

	public static class Handler implements IMessageHandler<TeleporterPacket, IMessage>
	{

		@Override
		public IMessage onMessage(TeleporterPacket message, MessageContext ctx)
		{
			EntityPlayerMP player = ctx.getServerHandler().playerEntity;
			ItemStack teleporter = player.getHeldItem();
			if (teleporter == null || teleporter.getItem() != ModItems.teleporterMKII)
				return null;

			NBTTagCompound compound = teleporter.getTagCompound();
			if (compound == null)
				compound = new NBTTagCompound();
			NBTTagList list = (NBTTagList) compound.getTag("Locations");
			if (list == null)
				list = new NBTTagList();

			if (message.function == ADDDESTINATION)
			{
				NBTTagCompound tag = new NBTTagCompound();
				message.location.setDimentionName(BrandonsCore.proxy.getMCServer().worldServerForDimension(message.location.getDimension()).provider.getDimensionName());
				message.location.writeToNBT(tag);
				list.appendTag(tag);
				compound.setTag("Locations", list);
				teleporter.setTagCompound(compound);
			}

			if (message.function == SCROLL)
			{
				int selected = ItemNBTHelper.getShort(teleporter, "Selection", (short) 0);
				int selectionOffset = ItemNBTHelper.getInteger(teleporter, "SelectionOffset", 0);
				int maxSelect = Math.min(list.tagCount() - 1, 11);
				int maxOffset = Math.max(list.tagCount() - 12, 0);

				if (message.data > 0 && selected < maxSelect)
				{
					ItemNBTHelper.setShort(teleporter, "Selection", (short) (selected + 1));
					return null;
				}
				if (message.data > 0 && selectionOffset < maxOffset)
				{
					ItemNBTHelper.setInteger(teleporter, "SelectionOffset", selectionOffset + 1);
					return null;
				}
				if (message.data < 0 && selected > 0)
				{
					ItemNBTHelper.setShort(teleporter, "Selection", (short) (selected - 1));
					return null;
				}
				if (message.data < 0 && selectionOffset > 0)
				{
					ItemNBTHelper.setInteger(teleporter, "SelectionOffset", selectionOffset - 1);
					return null;
				}

			}

			if (message.function == UPDATEDESTINATION)
			{
				NBTTagCompound tag = list.getCompoundTagAt(message.data);
				message.location.setDimentionName(BrandonsCore.proxy.getMCServer().worldServerForDimension(message.location.getDimension()).provider.getDimensionName());
				message.location.writeToNBT(tag);
				list.func_150304_a(message.data, tag);
				compound.setTag("Locations", list);
				teleporter.setTagCompound(compound);
			}

			if (message.function == UPDATELOCK)
			{
				list.getCompoundTagAt(message.data).setBoolean("WP", message.dataB);
				compound.setTag("Locations", list);
				teleporter.setTagCompound(compound);
			}

			if (message.function == REMOVEDESTINATION)
			{
				list.removeTag(message.data);
				compound.setTag("Locations", list);
				teleporter.setTagCompound(compound);
			}

			if (message.function == UPDATENAME)
			{
				list.getCompoundTagAt(message.data).setString("Name", message.location.getName());
				compound.setTag("Locations", list);
				teleporter.setTagCompound(compound);
			}

			if (message.function == TELEPORT)
			{
				int fuel = ItemNBTHelper.getInteger(teleporter, "Fuel", 0);
				if (!player.capabilities.isCreativeMode)
					ItemNBTHelper.setInteger(teleporter, "Fuel", fuel - 1);
				TeleportLocation destination = new TeleportLocation();
				destination.readFromNBT(list.getCompoundTagAt(message.data));

				// TODO gamerforEA code start
				if (!EventUtils.cantBreak(player, destination.getXCoord(), destination.getYCoord(), destination.getZCoord()))
					// TODO gamerforEA code end
					destination.sendEntityToCoords(player);
			}

			if (message.function == MOVELOCATION)
			{
				int selected = ItemNBTHelper.getShort(teleporter, "Selection", (short) 0);
				int selectionOffset = ItemNBTHelper.getInteger(teleporter, "SelectionOffset", 0);
				int maxSelect = Math.min(list.tagCount() - 1, 11);
				int maxOffset = Math.max(list.tagCount() - 12, 0);

				if (message.dataB) //up
				{
					if (selected > 0)
					{
						NBTTagCompound temp = list.getCompoundTagAt(selected + selectionOffset);
						list.func_150304_a(selected + selectionOffset, list.getCompoundTagAt(selected + selectionOffset - 1));
						list.func_150304_a(selected + selectionOffset - 1, temp);
						compound.setTag("Locations", list);
						teleporter.setTagCompound(compound);
						ItemNBTHelper.setShort(teleporter, "Selection", (short) (ItemNBTHelper.getShort(teleporter, "Selection", (short) 0) - 1));
					}
				}
				else //down
					if (selected < maxSelect)
					{
						NBTTagCompound temp = list.getCompoundTagAt(selected + selectionOffset);
						list.func_150304_a(selected + selectionOffset, list.getCompoundTagAt(selected + selectionOffset + 1));
						list.func_150304_a(selected + selectionOffset + 1, temp);
						compound.setTag("Locations", list);
						teleporter.setTagCompound(compound);
						ItemNBTHelper.setShort(teleporter, "Selection", (short) (ItemNBTHelper.getShort(teleporter, "Selection", (short) 0) + 1));
					}
			}

			if (message.function == ADDFUEL)
			{
				int fuel = ItemNBTHelper.getInteger(teleporter, "Fuel", 0);
				int count = 0;
				for (int i = 0; i < message.data; i++)
				{
					if (player.inventory.hasItem(Items.ender_pearl))
					{
						player.inventory.consumeInventoryItem(Items.ender_pearl);
						count++;
					}
					else
						break;
				}
				ItemNBTHelper.setInteger(teleporter, "Fuel", fuel + ConfigHandler.teleporterUsesPerPearl * count);
			}

			if (message.function == CHANGESELECTION)
				ItemNBTHelper.setShort(teleporter, "Selection", (short) message.data);

			if (message.function == UPDATEOFFSET)
				ItemNBTHelper.setInteger(teleporter, "SelectionOffset", message.data);
			return null;
		}
	}
}