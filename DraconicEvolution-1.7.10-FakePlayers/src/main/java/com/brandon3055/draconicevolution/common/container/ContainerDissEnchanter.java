package com.brandon3055.draconicevolution.common.container;

import com.brandon3055.brandonscore.common.utills.ItemNBTHelper;
import com.brandon3055.draconicevolution.common.inventory.SlotOutput;
import com.brandon3055.draconicevolution.common.tileentities.TileDissEnchanter;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerDissEnchanter extends Container
{

	private TileDissEnchanter tile;
	private EntityPlayer player;
	private ItemStack cachIn0;
	private ItemStack cachIn1;
	private ItemStack cachIn2;
	private boolean nullCheck0 = false;
	private boolean nullCheck1 = false;
	private boolean nullCheck2 = false;

	public ContainerDissEnchanter(InventoryPlayer invPlayer, TileDissEnchanter tile)
	{
		this.tile = tile;
		this.player = invPlayer.player;

		for (int x = 0; x < 9; x++)
		{
			this.addSlotToContainer(new Slot(invPlayer, x, 8 + 18 * x, 118));
		}

		for (int y = 0; y < 3; y++)
		{
			for (int x = 0; x < 9; x++)
			{
				this.addSlotToContainer(new Slot(invPlayer, x + y * 9 + 9, 8 + 18 * x, 60 + y * 18));
			}
		}

		this.addSlotToContainer(new SlotEnchantedItem(tile, 0, 27, 23));
		this.addSlotToContainer(new SlotBook(tile, 1, 76, 23));
		this.addSlotToContainer(new SlotOutput(tile, 2, 134, 23));
	}

	@Override
	public boolean canInteractWith(EntityPlayer player)
	{
		return this.tile.isUseableByPlayer(player);
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int i)
	{
		Slot slot = this.getSlot(i);

		if (slot != null && slot.getHasStack())
		{
			ItemStack stack = slot.getStack();
			ItemStack result = stack.copy();

			if (i >= 36)
			{
				if (!this.mergeItemStack(stack, 0, 36, false))
					return null;
			}
			else if ((!this.isStackValidForInventory(stack, 0) || !this.mergeItemStack(stack, 36, 37, false)) && (!this.isStackValidForInventory(stack, 1) || !this.mergeItemStack(stack, 37, 38, false)))
				return null;

			if (stack.stackSize == 0)
				slot.putStack(null);
			else
				slot.onSlotChanged();

			slot.onPickupFromSlot(player, stack);

			return result;
		}

		return null;
	}

	private boolean isStackValidForInventory(ItemStack stack, int slot)
	{
		// TODO gamerforEA add condition [2]
		if (slot == 0 && stack.getItem().getItemEnchantability() > 0 && EnchantmentHelper.getEnchantments(stack).size() > 0)
			return true;
		return slot == 1 && stack.getItem().equals(Items.book);
	}

	public class SlotBook extends Slot
	{
		public SlotBook(IInventory inventory, int id, int x, int y)
		{
			super(inventory, id, x, y);
		}

		@Override
		public boolean isItemValid(ItemStack stack)
		{
			return stack.getItem().equals(Items.book);
		}

		@Override
		public int getSlotStackLimit()
		{
			return 64;
		}
	}

	public class SlotEnchantedItem extends Slot
	{
		public SlotEnchantedItem(IInventory inventory, int id, int x, int y)
		{
			super(inventory, id, x, y);
		}

		@Override
		public boolean isItemValid(ItemStack stack)
		{
			// TODO gamerforEA add condition [1]
			return stack.getItem().getItemEnchantability() > 0 && (EnchantmentHelper.getEnchantments(stack).size() > 0 || ItemNBTHelper.getInteger(stack, "RepairCost", 0) > 0);
		}

		@Override
		public int getSlotStackLimit()
		{
			return 1;
		}
	}

	@Override
	public void detectAndSendChanges()
	{
		super.detectAndSendChanges();
		ItemStack stack0 = this.tile.getStackInSlot(0);
		ItemStack stack1 = this.tile.getStackInSlot(1);
		ItemStack stack2 = this.tile.getStackInSlot(2);
		if (stack0 == null != this.nullCheck0)
		{
			this.tile.onInventoryChanged();
			this.nullCheck0 = stack0 == null;
		}
		if (stack1 == null != this.nullCheck1)
		{
			this.tile.onInventoryChanged();
			this.nullCheck1 = stack1 == null;
		}
		if (stack2 == null != this.nullCheck2)
		{
			this.tile.onInventoryChanged();
			this.nullCheck2 = stack2 == null;
		}

		if (stack0 != null && !ItemStack.areItemStacksEqual(stack0, this.cachIn0))
		{
			this.cachIn0 = stack0.copy();
			this.tile.onInventoryChanged();
		}
		if (stack1 != null && !ItemStack.areItemStacksEqual(stack1, this.cachIn1))
		{
			this.cachIn1 = stack1.copy();
			this.tile.onInventoryChanged();
		}
		if (stack2 != null && !ItemStack.areItemStacksEqual(stack2, this.cachIn2))
		{
			this.cachIn2 = stack2.copy();
			this.tile.onInventoryChanged();
		}
	}

	public TileDissEnchanter getTile()
	{
		return this.tile;
	}
}
