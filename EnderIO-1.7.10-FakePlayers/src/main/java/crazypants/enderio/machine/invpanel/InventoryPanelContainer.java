package crazypants.enderio.machine.invpanel;

import com.enderio.core.client.gui.widget.GhostBackgroundItemSlot;
import com.enderio.core.client.gui.widget.GhostSlot;
import com.enderio.core.common.util.ItemUtil;
import cpw.mods.fml.common.FMLCommonHandler;
import crazypants.enderio.EnderIO;
import crazypants.enderio.Log;
import crazypants.enderio.machine.gui.AbstractMachineContainer;
import crazypants.enderio.machine.invpanel.server.ChangeLog;
import crazypants.enderio.machine.invpanel.server.InventoryDatabaseServer;
import crazypants.enderio.machine.invpanel.server.ItemEntry;
import crazypants.enderio.network.PacketHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.*;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class InventoryPanelContainer extends AbstractMachineContainer<TileInventoryPanel> implements ChangeLog
{

	public static final int CRAFTING_GRID_X = 24 + 7;
	public static final int CRAFTING_GRID_Y = 16;

	public static final int RETURN_INV_X = 24 + 7;
	public static final int RETURN_INV_Y = 82;

	public static final int FILTER_SLOT_X = 24 + 233;
	public static final int FILTER_SLOT_Y = 7;

	private final HashSet<ItemEntry> changedItems;

	private Slot slotFilter;

	private int slotCraftResult;
	private int firstSlotReturn;
	private int endSlotReturn;
	private int firstSlotCraftingGrid;
	private int endSlotCraftingGrid;

	private boolean updateReturnAreaSlots;
	private boolean storedRecipeExists;

	// TODO gamerforEA code start
	private boolean forceClose;

	@Override
	public boolean canInteractWith(EntityPlayer player)
	{
		return !this.forceClose && super.canInteractWith(player);
	}
	// TODO gamerforEA code end

	public InventoryPanelContainer(InventoryPlayer playerInv, TileInventoryPanel te)
	{
		super(playerInv, te);

		// TODO gamerforEA code replace, old code:
		// te.eventHandler = this;
		this.forceClose = !te.addEventHandler(playerInv.player, this);
		// TODO gamerforEA code end

		if (te.getWorldObj().isRemote)
			this.changedItems = null;
		else
			this.changedItems = new HashSet<ItemEntry>();
	}

	@Override
	protected void addMachineSlots(InventoryPlayer playerInv)
	{
		this.slotCraftResult = this.inventorySlots.size();
		this.addSlotToContainer(new SlotCrafting(playerInv.player, this.getInv(), this.getInv(), TileInventoryPanel.SLOT_CRAFTING_RESULT, CRAFTING_GRID_X + 59, CRAFTING_GRID_Y + 18)
		{
			@Override
			public void onPickupFromSlot(EntityPlayer player, ItemStack p_82870_2_)
			{
				FMLCommonHandler.instance().firePlayerCraftingEvent(player, p_82870_2_, InventoryPanelContainer.this.getInv());
				for (int i = TileInventoryPanel.SLOT_CRAFTING_START; i < TileInventoryPanel.SLOT_CRAFTING_RESULT; i++)
				{
					ItemStack itemstack = InventoryPanelContainer.this.getInv().getStackInSlot(i);
					if (itemstack == null)
						continue;

					InventoryPanelContainer.this.getInv().decrStackSize(i, 1);
					if (!itemstack.getItem().hasContainerItem(itemstack))
						continue;

					ItemStack containerIS = itemstack.getItem().getContainerItem(itemstack);
					if (containerIS != null && containerIS.isItemStackDamageable() && containerIS.getItemDamage() > containerIS.getMaxDamage())
						MinecraftForge.EVENT_BUS.post(new PlayerDestroyItemEvent(player, containerIS));
					else
					{
						if (itemstack.getItem().doesContainerItemLeaveCraftingGrid(itemstack))
						{
							if (ItemUtil.doInsertItem(InventoryPanelContainer.this.getInv(), 10, 20, itemstack) > 0)
								continue;
							if (player.inventory.addItemStackToInventory(containerIS))
								continue;
						}
						if (InventoryPanelContainer.this.getInv().getStackInSlot(i) == null)
							InventoryPanelContainer.this.getInv().setInventorySlotContents(i, containerIS);
						else
							player.dropPlayerItemWithRandomChoice(containerIS, false);
					}
				}
			}

			@Override
			public ItemStack decrStackSize(int p_75209_1_)
			{
				// on a right click we are asked to craft half a result. Ignore that.
				if (this.getHasStack())
					return super.decrStackSize(this.getStack().stackSize);
				return super.decrStackSize(p_75209_1_);
			}

		});

		this.firstSlotCraftingGrid = this.inventorySlots.size();
		for (int y = 0, i = TileInventoryPanel.SLOT_CRAFTING_START; y < 3; y++)
		{
			for (int x = 0; x < 3; x++, i++)
			{
				this.addSlotToContainer(new Slot(this.getInv(), i, CRAFTING_GRID_X + x * 18, CRAFTING_GRID_Y + y * 18));
			}
		}
		this.endSlotCraftingGrid = this.inventorySlots.size();

		this.slotFilter = this.addSlotToContainer(new Slot(this.getInv(), TileInventoryPanel.SLOT_VIEW_FILTER, FILTER_SLOT_X, FILTER_SLOT_Y)
		{
			@Override
			public int getSlotStackLimit()
			{
				return 1;
			}
		});

		this.firstSlotReturn = this.inventorySlots.size();
		for (int y = 0, i = TileInventoryPanel.SLOT_RETURN_START; y < 2; y++)
		{
			for (int x = 0; x < 5; x++, i++)
			{
				this.addSlotToContainer(new Slot(this.getInv(), i, RETURN_INV_X + x * 18, RETURN_INV_Y + y * 18));
			}
		}
		this.endSlotReturn = this.inventorySlots.size();
	}

	public void createGhostSlots(List<GhostSlot> slots)
	{
		slots.add(new GhostBackgroundItemSlot(EnderIO.itemBasicFilterUpgrade, FILTER_SLOT_X, FILTER_SLOT_Y));
	}

	@Override
	public Point getPlayerInventoryOffset()
	{
		return new Point(24 + 39, 130);
	}

	@Override
	public void onContainerClosed(EntityPlayer player)
	{
		super.onContainerClosed(player);

		if (!this.getInv().getWorldObj().isRemote)
			// TODO gamerforEA code replace, old code:
			// this.getInv().eventHandler = null;
			this.getInv().removeEventHandler(player, this);
		// TODO gamerforEA code end

		this.removeChangeLog();
	}

	public TileInventoryPanel getInventoryPanel()
	{
		return this.getInv();
	}

	public Slot getSlotFilter()
	{
		return this.slotFilter;
	}

	@SuppressWarnings("unchecked")
	public List<Slot> getCraftingGridSlots()
	{
		return this.inventorySlots.subList(this.firstSlotCraftingGrid, this.endSlotCraftingGrid);
	}

	@SuppressWarnings("unchecked")
	public List<Slot> getReturnAreaSlots()
	{
		return this.inventorySlots.subList(this.firstSlotReturn, this.endSlotReturn);
	}

	@SuppressWarnings("unchecked")
	public List<Slot> getPlayerInventorySlots()
	{
		return this.inventorySlots.subList(this.startPlayerSlot, this.endPlayerSlot);
	}

	@SuppressWarnings("unchecked")
	public List<Slot> getPlayerHotbarSlots()
	{
		return this.inventorySlots.subList(this.startHotBarSlot, this.endHotBarSlot);
	}

	private void removeChangeLog()
	{
		if (this.changedItems != null)
		{
			InventoryDatabaseServer db = this.getInventoryPanel().getDatabaseServer();
			if (db != null)
				db.removeChangeLog(this);
		}
	}

	@Override
	public void removeCraftingFromCrafters(ICrafting crafting)
	{
		super.removeCraftingFromCrafters(crafting);
		this.removeChangeLog();
	}

	@Override
	public void addCraftingToCrafters(ICrafting crafting)
	{
		if (this.changedItems != null)
			this.sendChangeLog();
		super.addCraftingToCrafters(crafting);
		if (this.changedItems != null)
		{
			InventoryDatabaseServer db = this.getInventoryPanel().getDatabaseServer();
			if (db != null)
			{
				db.addChangeLog(this);
				if (crafting instanceof EntityPlayerMP)
					try
					{
						byte[] compressed = db.compressItemList();
						PacketItemList pil = new PacketItemList(this.getInventoryPanel(), db.getGeneration(), compressed);
						PacketHandler.sendTo(pil, (EntityPlayerMP) crafting);
					}
					catch (IOException ex)
					{
						Logger.getLogger(InventoryPanelContainer.class.getName()).log(Level.SEVERE, "Exception while compressing item list", ex);
					}
			}
		}
	}

	@Override
	public void onCraftMatrixChanged(IInventory inv)
	{
		InventoryCrafting tmp = new InventoryCrafting(new Container()
		{
			@Override
			public boolean canInteractWith(EntityPlayer ep)
			{
				return false;
			}
		}, 3, 3);

		for (int i = 0; i < 9; i++)
		{
			tmp.setInventorySlotContents(i, this.getInv().getStackInSlot(i));
		}

		this.getInv().setInventorySlotContents(9, CraftingManager.getInstance().findMatchingRecipe(tmp, this.getInv().getWorldObj()));

		this.checkCraftingRecipes();
	}

	public void checkCraftingRecipes()
	{
		this.storedRecipeExists = false;
		int storedCraftingRecipes = this.getInventoryPanel().getStoredCraftingRecipes();
		if (this.hasCraftingRecipe() && storedCraftingRecipes > 0)
		{
			List<Slot> craftingGrid = this.getCraftingGridSlots();
			for (int idx = 0; idx < storedCraftingRecipes; idx++)
			{
				if (this.getInventoryPanel().getStoredCraftingRecipe(idx).isEqual(craftingGrid))
				{
					this.storedRecipeExists = true;
					break;
				}
			}
		}
	}

	@Override
	public boolean func_94530_a(ItemStack par1, Slot slot)
	{
		return !(slot instanceof SlotCrafting) && super.func_94530_a(par1, slot);
	}

	public boolean clearCraftingGrid()
	{
		boolean cleared = true;
		for (Slot slot : this.getCraftingGridSlots())
		{
			if (slot.getHasStack())
			{
				this.moveItemsToReturnArea(slot.slotNumber);
				if (slot.getHasStack())
					cleared = false;
			}
		}
		return cleared;
	}

	public boolean hasCraftingRecipe()
	{
		return this.getSlot(this.slotCraftResult).getHasStack();
	}

	public boolean hasNewCraftingRecipe()
	{
		return this.hasCraftingRecipe() && !this.storedRecipeExists;
	}

	@Override
	protected List<SlotRange> getTargetSlotsForTransfer(int slotIndex, Slot slot)
	{
		if (slotIndex == this.slotCraftResult || slotIndex >= this.firstSlotReturn && slotIndex < this.endSlotReturn)
			return Collections.singletonList(this.getPlayerInventorySlotRange(true));
		if (slotIndex >= this.firstSlotCraftingGrid && slotIndex < this.endSlotCraftingGrid)
		{
			ArrayList<SlotRange> res = new ArrayList<SlotRange>();
			res.add(new SlotRange(this.firstSlotReturn, this.endSlotReturn, false));
			res.add(this.getPlayerInventorySlotRange(false));
			return res;
		}
		if (slotIndex >= this.startPlayerSlot)
			return Collections.singletonList(new SlotRange(this.firstSlotReturn, this.endSlotReturn, false));
		return Collections.emptyList();
	}

	@Override
	protected boolean mergeItemStack(ItemStack par1ItemStack, int fromIndex, int toIndex, boolean reversOrder)
	{
		if (!super.mergeItemStack(par1ItemStack, fromIndex, toIndex, reversOrder))
			return false;
		if (fromIndex < this.endSlotReturn && toIndex > this.firstSlotReturn)
			this.updateReturnAreaSlots = true;
		return true;
	}

	@Override
	public void detectAndSendChanges()
	{
		if (this.updateReturnAreaSlots)
		{
			this.updateReturnAreaSlots = false;
			this.sendReturnAreaSlots();
		}
		super.detectAndSendChanges();
	}

	@SuppressWarnings("unchecked")
	private void sendReturnAreaSlots()
	{
		for (int slotIdx = this.firstSlotReturn; slotIdx < this.endSlotReturn; slotIdx++)
		{
			ItemStack stack = ((Slot) this.inventorySlots.get(slotIdx)).getStack();
			if (stack != null)
				stack = stack.copy();
			this.inventoryItemStacks.set(slotIdx, stack);
			for (Object crafter : this.crafters)
			{
				((ICrafting) crafter).sendSlotContents(this, slotIdx, stack);
			}
		}
	}

	@Override
	public void entryChanged(ItemEntry entry)
	{
		this.changedItems.add(entry);
	}

	@Override
	public void databaseReset()
	{
		this.changedItems.clear();

	}

	@Override
	public void sendChangeLog()
	{
		if (!this.changedItems.isEmpty() && !this.crafters.isEmpty())
		{
			InventoryDatabaseServer db = this.getInventoryPanel().getDatabaseServer();
			if (db != null)
				try
				{
					byte[] compressed = db.compressChangedItems(this.changedItems);
					PacketItemList pil = new PacketItemList(this.getInventoryPanel(), db.getGeneration(), compressed);
					for (Object crafting : this.crafters)
					{
						if (crafting instanceof EntityPlayerMP)
							PacketHandler.sendTo(pil, (EntityPlayerMP) crafting);
					}
				}
				catch (IOException ex)
				{
					Logger.getLogger(InventoryPanelContainer.class.getName()).log(Level.SEVERE, "Exception while compressing changed items", ex);
				}
		}
		this.changedItems.clear();
	}

	public int getSlotIndex(IInventory inv, int index)
	{
		for (int i = 0; i < this.inventorySlots.size(); i++)
		{
			Slot slot = (Slot) this.inventorySlots.get(i);
			if (slot.isSlotInInventory(inv, index))
				return i;
		}
		return -1;
	}

	public void executeFetchItems(EntityPlayerMP player, int generation, int dbID, int targetSlot, int count)
	{
		TileInventoryPanel te = this.getInventoryPanel();
		InventoryDatabaseServer db = te.getDatabaseServer();
		if (db == null || db.getGeneration() != generation || !db.isCurrent())
			return;
		ItemEntry entry = db.getExistingItem(dbID);
		if (entry != null)
		{
			ItemStack targetStack;
			Slot slot;
			int maxStackSize;

			if (targetSlot < 0)
			{
				slot = null;
				targetStack = player.inventory.getItemStack();
				maxStackSize = player.inventory.getInventoryStackLimit();
			}
			else
			{
				slot = this.getSlot(targetSlot);
				targetStack = slot.getStack();
				maxStackSize = slot.getSlotStackLimit();
			}

			ItemStack tmpStack = new ItemStack(entry.getItem(), 0, entry.meta);

			Log.info("Loading item from ID " + entry.itemID + ". Result: " + Item.itemRegistry.getNameForObject(tmpStack.getItem()) + "  side: " + FMLCommonHandler.instance().getEffectiveSide());

			tmpStack.stackTagCompound = entry.nbt;
			maxStackSize = Math.min(maxStackSize, tmpStack.getMaxStackSize());

			if (targetStack != null && targetStack.stackSize > 0)
			{
				if (!ItemUtil.areStackMergable(tmpStack, targetStack))
					return;
			}
			else
				targetStack = tmpStack.copy();

			count = Math.min(count, maxStackSize - targetStack.stackSize);
			if (count > 0)
			{
				int extracted = db.extractItems(entry, count, te);
				if (extracted > 0)
				{
					targetStack.stackSize += extracted;

					this.sendChangeLog();

					if (slot != null)
						slot.putStack(targetStack);
					else
					{
						player.inventory.setItemStack(targetStack);
						player.updateHeldItem();
					}
				}
			}
		}
	}

	public boolean moveItemsToReturnArea(int fromSlot)
	{
		return this.moveItems(fromSlot, this.firstSlotReturn, this.endSlotReturn, Short.MAX_VALUE);
	}

	public boolean moveItems(int fromSlot, int toSlotStart, int toSlotEnd, int amount)
	{
		if (!this.executeMoveItems(fromSlot, toSlotStart, toSlotEnd, amount))
			return false;
		if (this.getInv().getWorldObj().isRemote)
			PacketHandler.INSTANCE.sendToServer(new PacketMoveItems(fromSlot, toSlotStart, toSlotEnd, amount));
		return true;
	}

	public boolean executeMoveItems(int fromSlot, int toSlotStart, int toSlotEnd, int amount)
	{
		if (fromSlot >= toSlotStart && fromSlot < toSlotEnd || toSlotEnd <= toSlotStart || amount <= 0)
			return false;

		Slot srcSlot = this.getSlot(fromSlot);
		ItemStack src = srcSlot.getStack();
		if (src != null)
		{
			ItemStack toMove = src.copy();
			toMove.stackSize = Math.min(src.stackSize, amount);
			int remaining = src.stackSize - toMove.stackSize;
			if (this.mergeItemStack(toMove, toSlotStart, toSlotEnd, false))
			{
				remaining += toMove.stackSize;
				if (remaining == 0)
					srcSlot.putStack(null);
				else
				{
					src.stackSize = remaining;
					srcSlot.onSlotChanged();
				}
				return true;
			}
		}
		return false;
	}
}
