package crazypants.enderio.machine.capbank;

import com.enderio.core.common.ContainerEnder;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import crazypants.enderio.machine.capbank.network.InventoryImpl;
import crazypants.util.BaublesUtil;
import crazypants.util.ShadowInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import java.awt.*;

public class ContainerCapBank extends ContainerEnder<TileCapBank>
{
	// TODO gamerforEA code start
	private final int prevModCount;

	@Override
	public boolean canInteractWith(EntityPlayer player)
	{
		return this.getInv().modCount == this.prevModCount && super.canInteractWith(player);
	}
	// TODO gamerforEA code end

	private InventoryImpl inv;

	// Note: Modifying the Baubles inventory on the client side of an integrated
	// server is a bad idea because Baubles actually shares the object between
	// server and client. However, by not doing so, we prevent the callback hooks
	// to be called in the client thread. It doesn't seem to matter now, but if we
	// ever get reports of problems, we need to find a solution to synthesize
	// these calls.
	// In a multiplayer client this works well, but we need to prevent Baubles
	// from calling those callbacks when the server syncs the slot contents to the
	// client (packet 30) initially. They are still called every time the server
	// updates the slots (which is wrong), but the original Baubles inventory
	// works this way, so...
	private IInventory baubles;

	public ContainerCapBank(InventoryPlayer playerInv, TileCapBank cb)
	{
		super(playerInv, cb);

		// TODO gamerforEA code start
		this.prevModCount = cb.modCount;
		// TODO gamerforEA code end
	}

	public boolean hasBaublesSlots()
	{
		return this.baubles != null;
	}

	@Override
	protected void addSlots(final InventoryPlayer playerInv)
	{
		if (this.getInv().getNetwork() != null && this.getInv().getNetwork().getInventory() != null)
			this.inv = this.getInv().getNetwork().getInventory();
		else
			this.inv = new InventoryImpl();

		this.baubles = BaublesUtil.instance().getBaubles(playerInv.player);

		if (this.baubles != null && BaublesUtil.WhoAmI.whoAmI(playerInv.player.worldObj) == BaublesUtil.WhoAmI.SPCLIENT)
			this.baubles = new ShadowInventory(this.baubles);

		int armorOffset = 21;
		for (int i = 0; i < 4; i++)
		{
			this.addSlotToContainer(new SlotImpl(this.inv, i, 59 + armorOffset + i * 20, 59));
		}

		//armor slots
		for (int i = 0; i < 4; ++i)
		{
			final int k = i;
			this.addSlotToContainer(new Slot(playerInv, playerInv.getSizeInventory() - 1 - i, -15 + armorOffset, 12 + i * 18)
			{

				@Override
				public int getSlotStackLimit()
				{
					return 1;
				}

				@Override
				public boolean isItemValid(ItemStack par1ItemStack)
				{
					return par1ItemStack != null && par1ItemStack.getItem().isValidArmor(par1ItemStack, k, playerInv.player);
				}

				@Override
				@SideOnly(Side.CLIENT)
				public IIcon getBackgroundIconIndex()
				{
					return ItemArmor.func_94602_b(k);
				}
			});
		}

		if (this.hasBaublesSlots())
			for (int i = 0; i < this.baubles.getSizeInventory(); i++)
			{
				this.addSlotToContainer(new Slot(this.baubles, i, -15 + armorOffset, 84 + i * 18)
				{
					@Override
					public boolean isItemValid(ItemStack par1ItemStack)
					{
						return this.inventory.isItemValidForSlot(this.getSlotIndex(), par1ItemStack);
					}
				});
			}
	}

	public void updateInventory()
	{
		if (this.getInv().getNetwork() != null && this.getInv().getNetwork().getInventory() != null)
			this.inv.setCapBank(this.getInv().getNetwork().getInventory().getCapBank());
	}

	@Override
	public Point getPlayerInventoryOffset()
	{
		Point p = super.getPlayerInventoryOffset();
		p.translate(21, 0);
		return p;
	}

	// TODO gamerforEA code start
	@Override
	public ItemStack slotClick(int slot, int p_75144_2_, int p_75144_3_, EntityPlayer player)
	{
		return this.canInteractWith(player) ? super.slotClick(slot, p_75144_2_, p_75144_3_, player) : null;
	}
	// TODO gamerforEA code end

	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int slotIndex)
	{
		// TODO gamerforEA code start
		if (!this.canInteractWith(player))
			return null;
		// TODO gamerforEA code end

		int startPlayerSlot = 4;
		int endPlayerSlot = startPlayerSlot + 26;
		int startHotBarSlot = endPlayerSlot + 1;
		int endHotBarSlot = startHotBarSlot + 9;
		int startBaublesSlot = endHotBarSlot + 1;
		int endBaublesSlot = this.baubles == null ? 0 : startBaublesSlot + this.baubles.getSizeInventory();

		ItemStack copystack = null;
		Slot slot = (Slot) this.inventorySlots.get(slotIndex);
		if (slot != null && slot.getHasStack())
		{

			ItemStack origStack = slot.getStack();
			copystack = origStack.copy();

			// Note: Merging into Baubles slots is disabled because the used vanilla
			// merge method does not check if the item can go into the slot or not.

			//Check from inv-> charge then inv->hotbar or hotbar->inv
			if (slotIndex < 4)
			{
				// merge from machine input slots to inventory
				if (!this.mergeItemStackIntoArmor(player, origStack, slotIndex) && /*
				 * !(baubles != null && mergeItemStack(origStack,
				 * startBaublesSlot, endBaublesSlot, false)) &&
				 */!this.mergeItemStack(origStack, startPlayerSlot, endHotBarSlot, false))
					return null;

			}
			else if (slotIndex >= startPlayerSlot)
				if (!this.inv.isItemValidForSlot(0, origStack) || !this.mergeItemStack(origStack, 0, 4, false))
					if (slotIndex <= endPlayerSlot)
					{
						if (/*
						 * !(baubles != null && mergeItemStack(origStack,
						 * startBaublesSlot, endBaublesSlot, false)) &&
						 */!this.mergeItemStack(origStack, startHotBarSlot, endHotBarSlot, false))
							return null;
					}
					else if (slotIndex >= startHotBarSlot && slotIndex <= endHotBarSlot)
					{
						if (/*
						 * !(baubles != null && mergeItemStack(origStack,
						 * startBaublesSlot, endBaublesSlot, false)) &&
						 */!this.mergeItemStack(origStack, startPlayerSlot, endPlayerSlot, false))
							return null;
					}
					else if (slotIndex >= startBaublesSlot && slotIndex <= endBaublesSlot)
						if (!this.mergeItemStack(origStack, startHotBarSlot, endHotBarSlot, false) && !this.mergeItemStack(origStack, startPlayerSlot, endPlayerSlot, false))
							return null;

			if (origStack.stackSize == 0)
				slot.putStack(null);
			else
				slot.onSlotChanged();

			slot.onSlotChanged();

			if (origStack.stackSize == copystack.stackSize)
				return null;

			slot.onPickupFromSlot(player, origStack);
		}

		return copystack;
	}

	private boolean mergeItemStackIntoArmor(EntityPlayer entityPlayer, ItemStack origStack, int slotIndex)
	{
		if (origStack == null || !(origStack.getItem() instanceof ItemArmor))
			return false;
		ItemArmor armor = (ItemArmor) origStack.getItem();
		int index = 3 - armor.armorType;
		ItemStack[] ai = entityPlayer.inventory.armorInventory;
		if (ai[index] == null)
		{
			ai[index] = origStack.copy();
			origStack.stackSize = 0;
			return true;
		}
		return false;
	}

	private static class SlotImpl extends Slot
	{
		public SlotImpl(IInventory inv, int idx, int x, int y)
		{
			super(inv, idx, x, y);
		}

		@Override
		public boolean isItemValid(ItemStack itemStack)
		{
			return this.inventory.isItemValidForSlot(this.getSlotIndex(), itemStack);
		}
	}

	/**
	 * called when the content of slots is synced from the server to the client
	 * (packet 30)
	 */
	@Override
	@SideOnly(Side.CLIENT)
	public void putStacksInSlots(ItemStack[] p_75131_1_)
	{
		if (this.hasBaublesSlots() && BaublesUtil.WhoAmI.whoAmI(this.getInv().getWorldObj()) == BaublesUtil.WhoAmI.MPCLIENT)
			try
			{
				BaublesUtil.instance().disableCallbacks(this.baubles, true);
				super.putStacksInSlots(p_75131_1_);
			}
			finally
			{
				BaublesUtil.instance().disableCallbacks(this.baubles, false);
			}
		else
			super.putStacksInSlots(p_75131_1_);
	}

}
