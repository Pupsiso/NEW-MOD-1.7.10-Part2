package crazypants.enderio.machine.crafter;

import com.enderio.core.common.util.ItemUtil;
import com.gamerforea.enderio.EventConfig;
import com.mojang.authlib.GameProfile;
import cpw.mods.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;
import crazypants.enderio.ModObject;
import crazypants.enderio.config.Config;
import crazypants.enderio.machine.AbstractPowerConsumerEntity;
import crazypants.enderio.machine.FakePlayerEIO;
import crazypants.enderio.machine.IItemBuffer;
import crazypants.enderio.machine.SlotDefinition;
import crazypants.enderio.power.BasicCapacitor;
import crazypants.enderio.power.Capacitors;
import crazypants.enderio.power.ICapacitor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class TileCrafter extends AbstractPowerConsumerEntity implements IItemBuffer
{

	DummyCraftingGrid craftingGrid = new DummyCraftingGrid();

	private final List<ItemStack> containerItems;

	private boolean bufferStacks = true;

	private long ticksSinceLastCraft = 0;

	private FakePlayer playerInst;

	public TileCrafter()
	{
		super(new SlotDefinition(9, 1));
		this.containerItems = new ArrayList<ItemStack>();
	}

	@Override
	public void onCapacitorTypeChange()
	{
		ICapacitor refCap = this.getCapacitor();
		int maxUse = this.getPowerUsePerTick(this.getCapacitorType());
		int io = Math.max(maxUse, refCap.getMaxEnergyExtracted());
		this.setCapacitor(new BasicCapacitor(io * 4, refCap.getMaxEnergyStored(), io));
	}

	@Override
	public String getMachineName()
	{
		return ModObject.blockCrafter.unlocalisedName;
	}

	@Override
	protected boolean isMachineItemValidForSlot(int slot, ItemStack itemstack)
	{
		if (!this.slotDefinition.isInputSlot(slot))
			return false;
		return this.craftingGrid.inv[slot] != null && compareDamageable(itemstack, this.craftingGrid.inv[slot]);
	}

	@Override
	public boolean isActive()
	{
		return false;
	}

	@Override
	protected boolean processTasks(boolean redstoneCheckPassed)
	{
		this.ticksSinceLastCraft++;
		if (!redstoneCheckPassed || !this.craftingGrid.hasValidRecipe() || !this.canMergeOutput() || !this.hasRequiredPower())
			return false;

		// TODO gamerforEA code replace, old code: Capacitors capacitorType = this.getCapacitorType();
		Capacitors capacitorType = EventConfig.crafterIgnoreCapacitorType ? Capacitors.BASIC_CAPACITOR : this.getCapacitorType();
		// TODO gamerforEA code end

		int ticksPerCraft = this.getTicksPerCraft(capacitorType);

		if (this.ticksSinceLastCraft <= ticksPerCraft)
			return false;
		this.ticksSinceLastCraft = 0;

		// process buffered container items
		if (!this.containerItems.isEmpty())
		{
			Iterator<ItemStack> iter = this.containerItems.iterator();
			while (iter.hasNext())
			{
				ItemStack stack = iter.next();
				if (this.inventory[9] == null)
				{
					this.inventory[9] = stack;
					iter.remove();
				}
				else if (ItemUtil.areStackMergable(this.inventory[9], stack) && this.inventory[9].stackSize + stack.stackSize <= this.inventory[9].getMaxStackSize())
				{
					this.inventory[9].stackSize += stack.stackSize;
					iter.remove();
				}
			}
			return false;
		}

		if (this.craftRecipe())
		{
			int used = Math.min(this.getEnergyStored(), Config.crafterRfPerCraft);
			this.setEnergyStored(this.getEnergyStored() - used);
		}
		return false;
	}

	private boolean hasRequiredPower()
	{
		return this.getEnergyStored() >= Config.crafterRfPerCraft;
	}

	@Override
	public int getPowerUsePerTick()
	{
		// TODO gamerforEA code replace, old code: Capacitors capacitorType = this.getCapacitorType();
		Capacitors capacitorType = EventConfig.crafterIgnoreCapacitorType ? Capacitors.BASIC_CAPACITOR : this.getCapacitorType();
		// TODO gamerforEA code end

		return this.getPowerUsePerTick(capacitorType);
	}

	public int getPowerUsePerTick(Capacitors type)
	{
		int ticks = this.getTicksPerCraft(type);
		return (int) Math.ceil(Config.crafterRfPerCraft / (double) ticks);
	}

	public int getTicksPerCraft(Capacitors type)
	{
		if (type == Capacitors.BASIC_CAPACITOR)
			return 20;
		else if (type == Capacitors.ACTIVATED_CAPACITOR)
			return 10;
		else
			return 2;
	}

	static boolean compareDamageable(ItemStack stack, ItemStack req)
	{
		if (stack.isItemEqual(req))
			return true;
		if (stack.isItemStackDamageable() && stack.getItem() == req.getItem())
			return stack.getItemDamage() < stack.getMaxDamage();
		return false;
	}

	private static final UUID uuid = UUID.fromString("9b381cae-3c95-4a64-b958-1e25b0a4c790");
	private static final GameProfile DUMMY_PROFILE = new GameProfile(uuid, "[EioCrafter]");

	private boolean craftRecipe()
	{

		// (1) Find the items to craft with and put a copy into a temp crafting grid;
		//     also record what was used to destroy it later
		InventoryCrafting inv = new InventoryCrafting(new Container()
		{
			@Override
			public boolean canInteractWith(EntityPlayer var1)
			{
				return false;
			}
		}, 3, 3);

		int[] usedItems = new int[9];

		for (int j = 0; j < 9; j++)
		{
			ItemStack req = this.craftingGrid.getStackInSlot(j);
			if (req != null)
			{
				for (int i = 0; i < 9; i++)
				{
					if (this.inventory[i] != null && this.inventory[i].stackSize > usedItems[i] && compareDamageable(this.inventory[i], req))
					{
						req = null;
						usedItems[i]++;
						ItemStack craftingItem = this.inventory[i].copy();
						craftingItem.stackSize = 1;
						inv.setInventorySlotContents(j, craftingItem);
						break;
					}
				}
				if (req != null)
					return false;
			}
		}

		// (2) Try to craft with the temp grid
		ItemStack output = CraftingManager.getInstance().findMatchingRecipe(inv, this.worldObj);

		// (3) If we got a result, ...
		if (output != null)
		{
			if (this.playerInst == null)
				this.playerInst = new FakePlayerEIO(this.worldObj, this.getLocation(), DUMMY_PROFILE);
			MinecraftForge.EVENT_BUS.post(new ItemCraftedEvent(this.playerInst, output, inv));

			// (3a) ... remove the used up items and ...
			for (int i = 0; i < 9; i++)
			{
				for (int j = 0; j < usedItems[i] && this.inventory[i] != null; j++)
				{
					this.setInventorySlotContents(i, this.eatOneItemForCrafting(this.inventory[i].copy()));
				}
			}

			// (3b) ... put the result into its slot
			if (this.inventory[9] == null)
				this.setInventorySlotContents(9, output);
			else if (ItemUtil.areStackMergable(this.inventory[9], output))
			{
				ItemStack cur = this.inventory[9].copy();
				cur.stackSize += output.stackSize;
				if (cur.stackSize > cur.getMaxStackSize())
				{
					// we check beforehand that there is enough free space, but some mod may return different
					// amounts based on the nbt of the input items (e.g. magical wood)
					ItemStack overflow = cur.copy();
					overflow.stackSize = cur.stackSize - cur.getMaxStackSize();
					cur.stackSize = cur.getMaxStackSize();
					this.containerItems.add(overflow);
				}
				this.setInventorySlotContents(9, cur);
			}
			else
				// some mod may return different nbt based on the nbt of the input items (e.g. TE machines?)
				this.containerItems.add(output);
		}
		else
			// Crafting failed. This is not supposed to happen, but if a recipe is nbt-sensitive, it can.
			// To avoid being stuck in a dead loop, we flush the non-working input items.
			for (int j = 0; j < 9; j++)
			{
				if (usedItems[j] > 0 && this.inventory[j] != null)
				{
					ItemStack rejected = this.inventory[j].copy();
					rejected.stackSize = Math.min(this.inventory[j].stackSize, usedItems[j]);
					this.containerItems.add(rejected);
					if (this.inventory[j].stackSize <= usedItems[j])
						this.inventory[j] = null;
					else
						this.inventory[j].stackSize -= usedItems[j];
				}
			}

		return true;
	}

	private ItemStack eatOneItemForCrafting(ItemStack avail)
	{
		// 101 special cases for container items
		if (avail.getItem().hasContainerItem(avail))
		{
			ItemStack used = avail.getItem().getContainerItem(avail);
			if (used == null)
				// The promised container item does not exist
				avail.stackSize--;
			else if (used.isItemStackDamageable() && used.getItemDamage() > used.getMaxDamage())
			{
				// Container item was used up
				used = null;
				avail.stackSize--;
			}
			else if (used.isItemEqual(avail))
			{
				if (used.isItemStackDamageable())
				{
					// Container item is the same, but may have a different damage value
					if (avail.stackSize == 1)
						// itemstack was used up, container item can replace it
						avail = used;
					else
					{
						// itemstack was not used up, container item goes into overflow
						// (impossible case with vanilla and mods that play by the rules)
						this.containerItems.add(used.copy());
						avail.stackSize--;
					}
				}
				else
				{
					// Container item is exactly the same: item should not be used up
					// Nothing to do here
				}
			}
			else
			{
				// Container item is different (e.g. bucket for lava bucket) and goes into the overflow
				this.containerItems.add(used.copy());
				avail.stackSize--;
			}
		}
		else
			// no container item, use up one item of the stack
			avail.stackSize--;

		if (avail.stackSize == 0)
			avail = null;
		return avail;
	}

	private boolean canMergeOutput()
	{
		if (this.inventory[9] == null)
			return true;
		ItemStack output = this.craftingGrid.getOutput();
		if (!ItemUtil.areStackMergable(this.inventory[9], output))
			return false;
		return output.getMaxStackSize() >= this.inventory[9].stackSize + output.stackSize;
	}

	@Override
	public int getInventoryStackLimit()
	{
		return this.bufferStacks ? 64 : 1;
	}

	@Override
	public boolean isBufferStacks()
	{
		return this.bufferStacks;
	}

	@Override
	public void setBufferStacks(boolean bufferStacks)
	{
		this.bufferStacks = bufferStacks;
	}

	@Override
	public void readCommon(NBTTagCompound nbtRoot)
	{
		super.readCommon(nbtRoot);
		NBTTagCompound craftRoot = nbtRoot.getCompoundTag("craftingGrid");
		this.craftingGrid.readFromNBT(craftRoot);

		if (nbtRoot.hasKey("bufferStacks"))
			this.bufferStacks = nbtRoot.getBoolean("bufferStacks");
		else
			this.bufferStacks = true;

		this.containerItems.clear();
		NBTTagList itemList = (NBTTagList) nbtRoot.getTag("containerItems");
		if (itemList != null)
			for (int i = 0; i < itemList.tagCount(); i++)
			{
				NBTTagCompound itemStack = itemList.getCompoundTagAt(i);
				this.containerItems.add(ItemStack.loadItemStackFromNBT(itemStack));
			}
	}

	@Override
	public void writeCommon(NBTTagCompound nbtRoot)
	{
		super.writeCommon(nbtRoot);
		NBTTagCompound craftingRoot = new NBTTagCompound();
		this.craftingGrid.writeToNBT(craftingRoot);
		nbtRoot.setTag("craftingGrid", craftingRoot);

		nbtRoot.setBoolean("bufferStacks", this.bufferStacks);

		if (this.containerItems.isEmpty())
			nbtRoot.removeTag("containerItems");
		else
		{
			NBTTagList itemList = new NBTTagList();
			for (ItemStack stack : this.containerItems)
			{
				NBTTagCompound itemStackNBT = new NBTTagCompound();
				stack.writeToNBT(itemStackNBT);
				itemList.appendTag(itemStackNBT);
			}
			nbtRoot.setTag("containerItems", itemList);
		}
	}

	public void updateCraftingOutput()
	{
		InventoryCrafting inv = new InventoryCrafting(new Container()
		{

			@Override
			public boolean canInteractWith(EntityPlayer var1)
			{
				return false;
			}
		}, 3, 3);

		for (int i = 0; i < 9; i++)
		{
			inv.setInventorySlotContents(i, this.craftingGrid.getStackInSlot(i));
		}
		ItemStack matches = CraftingManager.getInstance().findMatchingRecipe(inv, this.worldObj);
		this.craftingGrid.setInventorySlotContents(9, matches);
		this.markDirty();

	}

}
