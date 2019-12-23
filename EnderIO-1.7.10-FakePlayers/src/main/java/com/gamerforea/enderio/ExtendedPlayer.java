package com.gamerforea.enderio;

import com.google.common.base.Preconditions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class ExtendedPlayer implements IExtendedEntityProperties
{
	private static final String PROPERTY_NAME = "EnderIOExtendedPlayer";
	public final List<SlotContent> inventoryContent = new ArrayList<SlotContent>();
	public final List<SlotContent> baublesContent = new ArrayList<SlotContent>();
	public final List<SlotContent> galacticraftContent = new ArrayList<SlotContent>();

	@Override
	public void saveNBTData(NBTTagCompound nbt)
	{
		saveContentList(nbt, "InventoryContent", this.inventoryContent);
		saveContentList(nbt, "BaublesContent", this.baublesContent);
		saveContentList(nbt, "GalacticraftContent", this.galacticraftContent);
	}

	@Override
	public void loadNBTData(NBTTagCompound nbt)
	{
		loadContentList(nbt, "InventoryContent", this.inventoryContent);
		loadContentList(nbt, "BaublesContent", this.baublesContent);
		loadContentList(nbt, "GalacticraftContent", this.galacticraftContent);
	}

	@Override
	public void init(Entity entity, World world)
	{
	}

	public static ExtendedPlayer get(EntityPlayer player)
	{
		return (ExtendedPlayer) Preconditions.checkNotNull(player.getExtendedProperties(PROPERTY_NAME), PROPERTY_NAME + " property not found");
	}

	public static void register(EntityPlayer player)
	{
		if (player.getExtendedProperties(PROPERTY_NAME) == null)
			player.registerExtendedProperties(PROPERTY_NAME, new ExtendedPlayer());
	}

	public static boolean isUsed()
	{
		return EventConfig.alternativeSoulBoundRestoration;
	}

	private static void saveContentList(NBTTagCompound nbt, String key, Collection<? extends SlotContent> contentList)
	{
		NBTTagList list = new NBTTagList();
		for (SlotContent slotContent : contentList)
		{
			NBTTagCompound slotNbt = new NBTTagCompound();
			slotContent.writeToNBT(slotNbt);
			list.appendTag(slotNbt);
		}
		nbt.setTag(key, list);
	}

	private static void loadContentList(NBTTagCompound nbt, String key, Collection<? super SlotContent> contentList)
	{
		if (nbt.hasKey(key, Constants.NBT.TAG_LIST))
		{
			NBTTagList list = nbt.getTagList(key, Constants.NBT.TAG_COMPOUND);
			for (int i = 0; i < list.tagCount(); i++)
			{
				NBTTagCompound slotNbt = list.getCompoundTagAt(i);
				contentList.add(new SlotContent(slotNbt));
			}
		}
	}

	public static final class SlotContent
	{
		public final int slot;
		public final ItemStack stack;

		public SlotContent(NBTTagCompound nbt)
		{
			this(nbt.getShort("Slot"), ItemStack.loadItemStackFromNBT(nbt));
		}

		public SlotContent(int slot, ItemStack stack)
		{
			Preconditions.checkArgument(slot >= 0, "slot must be non negative");
			Preconditions.checkNotNull(stack, "stack must not be null");
			this.slot = slot;
			this.stack = stack;
		}

		public void writeToNBT(NBTTagCompound nbt)
		{
			nbt.setShort("Slot", (short) this.slot);
			this.stack.writeToNBT(nbt);
		}
	}
}
