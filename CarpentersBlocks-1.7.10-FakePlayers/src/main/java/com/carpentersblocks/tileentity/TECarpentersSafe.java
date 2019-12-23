package com.carpentersblocks.tileentity;

import com.carpentersblocks.data.Safe;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public class TECarpentersSafe extends TEBase implements ISidedInventory
{
	private final String TAG_SLOT = "Slot";
	private final String TAG_ITEMS = "Items";
	private final int EVENT_ID_STATE_CHANGE = 0;

	/**
	 * Holds contents of block.
	 */
	private ItemStack[] inventoryContents = new ItemStack[54];

	/**
	 * Counts ticks.
	 */
	private int tickCount;

	/**
	 * Used to determine whether capacity strip requires a redraw.
	 */
	private boolean contentsChanged;

	/**
	 * Indicates safe render update should occur next tick.
	 */
	private boolean stateChanged;

	@Override
	/**
	 * Determines if this TileEntity requires update calls.
	 *
	 * @return True if you want updateEntity() to be called, false if not
	 */ public boolean canUpdate()
	{
		return true;
	}

	@Override
	/**
	 * Allows the entity to update its state. Overridden in most subclasses,
	 * e.g. the mob spawner uses this to count ticks and creates a new spawn
	 * inside its implementation.
	 */ public void updateEntity()
	{
		// For chest capacity indicator, process contents changed only once per second
		if (!this.worldObj.isRemote)
			if (this.contentsChanged && this.tickCount % 20 == 0)
			{
				this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
				this.contentsChanged = false;
			}
	}

	/**
	 * Returns the number of slots in the inventory.
	 */
	@Override
	public int getSizeInventory()
	{
		return this.hasAttribute(ATTR_UPGRADE) ? 54 : 27;
	}

	/**
	 * Returns the stack in slot
	 */
	@Override
	public ItemStack getStackInSlot(int slot)
	{
		return this.inventoryContents[slot];
	}

	/**
	 * Removes from an inventory slot (first arg) up to a specified number
	 * (second arg) of items and returns them in a new stack.
	 */
	@Override
	public ItemStack decrStackSize(int slot, int size)
	{
		ItemStack itemStack = null;

		if (this.inventoryContents[slot] != null)
			if (this.inventoryContents[slot].stackSize <= size)
			{
				itemStack = this.inventoryContents[slot];
				this.inventoryContents[slot] = null;
			}
			else
			{
				itemStack = this.inventoryContents[slot].splitStack(size);
				if (this.inventoryContents[slot].stackSize == 0)
					this.inventoryContents[slot] = null;
			}

		if (itemStack == null)
			return null;
		this.markDirty();
		return itemStack;
	}

	/**
	 * When some containers are closed they call this on each slot, then drop
	 * whatever it returns as an EntityItem - like when you close a workbench
	 * GUI.
	 */
	@Override
	public ItemStack getStackInSlotOnClosing(int slot)
	{
		return null;
	}

	/**
	 * Sets the given item stack to the specified slot in the inventory (can be
	 * crafting or armor sections).
	 */
	@Override
	public void setInventorySlotContents(int slot, ItemStack itemStack)
	{
		this.inventoryContents[slot] = itemStack;

		if (itemStack != null && itemStack.stackSize > this.getInventoryStackLimit())
			itemStack.stackSize = this.getInventoryStackLimit();

		this.markDirty();
	}

	@Override
	/**
	 * Called when an the contents of an Inventory change, usually
	 */ public void markDirty()
	{
		this.contentsChanged = true;
		super.markDirty();
	}

	/**
	 * Reads a tile entity from NBT.
	 */
	@Override
	public void readFromNBT(NBTTagCompound nbt)
	{
		super.readFromNBT(nbt);

		/* Compatibility code with older versions prior to v3.2.5 */
		if (nbt.hasKey("inventorySize") && nbt.getInteger("inventorySize") > 27)
			this.addAttribute(ATTR_UPGRADE, new ItemStack(Items.iron_ingot));

		NBTTagList nbttaglist = nbt.getTagList(this.TAG_ITEMS, 10);
		this.inventoryContents = new ItemStack[54];

		for (int idx = 0; idx < nbttaglist.tagCount(); ++idx)
		{
			NBTTagCompound nbt1 = nbttaglist.getCompoundTagAt(idx);
			int j = nbt1.getByte(this.TAG_SLOT) & 255;
			if (j >= 0 && j < this.inventoryContents.length)
				this.inventoryContents[j] = ItemStack.loadItemStackFromNBT(nbt1);
		}
	}

	/**
	 * Writes a tile entity to NBT.
	 */
	@Override
	public void writeToNBT(NBTTagCompound nbt)
	{
		super.writeToNBT(nbt);

		NBTTagList nbttaglist = new NBTTagList();

		for (int idx = 0; idx < this.inventoryContents.length; ++idx)
		{
			if (this.inventoryContents[idx] != null)
			{
				NBTTagCompound nbttagcompound1 = new NBTTagCompound();
				nbttagcompound1.setByte(this.TAG_SLOT, (byte) idx);
				this.inventoryContents[idx].writeToNBT(nbttagcompound1);
				nbttaglist.appendTag(nbttagcompound1);
			}
		}

		nbt.setTag(this.TAG_ITEMS, nbttaglist);
	}

	/**
	 * Do not make give this method the name canInteractWith because it clashes
	 * with Container
	 */
	@Override
	public boolean isUseableByPlayer(EntityPlayer entityPlayer)
	{
		// TODO gamerforEA replace equals(Object) to ==
		if (entityPlayer.getEntityWorld().getTileEntity(this.xCoord, this.yCoord, this.zCoord) == this)
			return entityPlayer.getDistanceSq(this.xCoord + 0.5D, this.yCoord + 0.5D, this.zCoord + 0.5D) <= 64.0D;

		return false;
	}

	/**
	 * Called when a client event is received with the event number and
	 * argument, see World.sendClientEvent
	 */
	@Override
	public boolean receiveClientEvent(int eventId, int eventArg)
	{
		if (eventId == this.EVENT_ID_STATE_CHANGE)
		{
			Safe.setState(this, eventArg);
			String soundName = eventArg == Safe.STATE_OPEN ? "random.door_open" : "random.door_close";
			this.worldObj.playSound(this.xCoord + 0.5D, this.yCoord + 0.5D, this.zCoord + 0.5D, soundName, 1.0F, this.worldObj.rand.nextFloat() * 0.1F + 0.9F, false);
			this.stateChanged = true;
			return true;
		}
		return super.receiveClientEvent(eventId, eventArg);
	}

	@Override
	public void openInventory()
	{
		this.worldObj.addBlockEvent(this.xCoord, this.yCoord, this.zCoord, this.getBlockType(), this.EVENT_ID_STATE_CHANGE, Safe.STATE_OPEN);
	}

	@Override
	public void closeInventory()
	{
		this.worldObj.addBlockEvent(this.xCoord, this.yCoord, this.zCoord, this.getBlockType(), this.EVENT_ID_STATE_CHANGE, Safe.STATE_CLOSED);
	}

	@Override
	public String getInventoryName()
	{
		return "tile.blockCarpentersSafe.name";
	}

	@Override
	public boolean hasCustomInventoryName()
	{
		return false;
	}

	@Override
	public int getInventoryStackLimit()
	{
		return 64;
	}

	@Override
	public int[] getAccessibleSlotsFromSide(int side)
	{
		int sizeInventory = this.getSizeInventory();
		int[] accessibleSlots = new int[sizeInventory];

		for (int idx = 0; idx < sizeInventory; ++idx)
		{
			accessibleSlots[idx] = idx;
		}

		return accessibleSlots;
	}

	@Override
	public boolean canInsertItem(int slot, ItemStack itemStack, int side)
	{
		return Safe.allowsInsertion(this) && Safe.getFacing(this).ordinal() != side;
	}

	@Override
	public boolean canExtractItem(int slot, ItemStack itemStack, int side)
	{
		return Safe.allowsExtraction(this) && Safe.getFacing(this).ordinal() != side;
	}

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack itemstack)
	{
		return true;
	}

}
