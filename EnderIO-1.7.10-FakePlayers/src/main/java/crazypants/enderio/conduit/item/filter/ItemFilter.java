package crazypants.enderio.conduit.item.filter;

import com.enderio.core.client.gui.widget.GhostSlot;
import com.enderio.core.common.network.NetworkUtil;
import com.gamerforea.enderio.util.FastOreDictionary;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import crazypants.enderio.conduit.gui.GuiExternalConnection;
import crazypants.enderio.conduit.gui.item.BasicItemFilterGui;
import crazypants.enderio.conduit.gui.item.IItemFilterGui;
import crazypants.enderio.conduit.gui.item.ItemConduitFilterContainer;
import crazypants.enderio.conduit.item.IItemConduit;
import crazypants.enderio.conduit.item.NetworkedInventory;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ItemFilter implements IInventory, IItemFilter
{

	private static final boolean DEFAULT_BLACKLIST = false;

	private static final boolean DEFAULT_META = true;

	private static final boolean DEFAULT_MBT = true;

	private static final boolean DEFAULT_ORE_DICT = false;

	private static final boolean DEFAULT_STICKY = false;

	boolean isBlacklist = DEFAULT_BLACKLIST;
	boolean matchMeta = true;
	boolean matchNBT = true;
	boolean useOreDict = false;
	boolean sticky = false;
	FuzzyMode fuzzyMode = FuzzyMode.DISABLED;

	ItemStack[] items;

	final List<int[]> oreIds;

	private boolean isAdvanced;

	public void copyFrom(ItemFilter o)
	{
		this.isBlacklist = o.isBlacklist;
		this.matchMeta = o.matchMeta;
		this.matchNBT = o.matchNBT;
		this.useOreDict = o.useOreDict;
		this.sticky = o.sticky;
		this.fuzzyMode = o.fuzzyMode;
		this.items = o.items;
		this.oreIds.clear();
		this.oreIds.addAll(o.oreIds);
		this.isAdvanced = o.isAdvanced;
	}

	public ItemFilter()
	{
		this(5, false);
	}

	public ItemFilter(boolean advanced)
	{
		this(advanced ? 10 : 5, advanced);
	}

	private ItemFilter(int numItems, boolean isAdvanced)
	{
		this.isAdvanced = isAdvanced;
		this.items = new ItemStack[numItems];
		this.oreIds = new ArrayList<int[]>(numItems);
		for (int i = 0; i < numItems; i++)
		{
			this.oreIds.add(null);
		}
	}

	@Override
	public boolean doesFilterCaptureStack(NetworkedInventory inv, ItemStack item)
	{
		return this.isSticky() && this.itemMatched(item);
	}

	@Override
	public boolean doesItemPassFilter(NetworkedInventory inv, ItemStack item)
	{
		return this.doesItemPassFilter(item);
	}

	public boolean doesItemPassFilter(ItemStack item)
	{
		if (!this.isValid())
			return true;
		boolean matched = this.itemMatched(item);
		return this.isBlacklist != matched;
	}

	private boolean itemMatched(ItemStack item)
	{
		if (item == null)
			return false;
		boolean doFuzzy = false;
		boolean fuzzyValue = false;
		if (this.fuzzyMode != FuzzyMode.DISABLED && item.getItem().isDamageable())
		{
			doFuzzy = true;
			fuzzyValue = this.fuzzyMode.compare(item);
		}
		boolean matched = false;
		int i = 0;
		for (ItemStack it : this.items)
		{
			if (it != null && Item.getIdFromItem(item.getItem()) == Item.getIdFromItem(it.getItem()))
			{
				matched = true;
				boolean fuzzyOk = doFuzzy && this.fuzzyMode.compare(it) == fuzzyValue;
				if (this.matchMeta && !fuzzyOk && item.getItemDamage() != it.getItemDamage())
					matched = false;
				else if (this.matchNBT && !this.isNBTMatch(item, it))
					matched = false;
			}
			if (!matched && this.useOreDict && this.isOreDicMatch(i, item))
				matched = true;
			if (matched)
				break;
			i++;
		}
		return matched;
	}

	private boolean isOreDicMatch(int filterItemIndex, ItemStack item)
	{
		int[] ids1 = this.getCachedIds(filterItemIndex);
		if (ids1 == null || ids1.length == 0)
			return false;

		/* TODO gamerforEA code replace, old code:
		int[] ids2 = OreDictionary.getOreIDs(item);
		if (ids2 == null || ids2.length == 0)
			return false; */
		int[] ids2 = FastOreDictionary.getOreIDs(item);
		if (ids2.length == 0)
			return false;
		// TODO gamerforEA code end

		for (int id1 : ids1)
		{
			for (int id2 : ids2)
			{
				if (id1 == id2)
					return true;
			}
		}

		return false;
	}

	private boolean isNBTMatch(ItemStack filter, ItemStack item)
	{
		if (filter.stackTagCompound == null && item.stackTagCompound == null)
			return true;
		if (filter.stackTagCompound == null || item.stackTagCompound == null)
			return false;
		if (!filter.getTagCompound().hasKey("GEN"))
			return filter.stackTagCompound.equals(item.stackTagCompound);
		NBTTagCompound filterTag = (NBTTagCompound) filter.getTagCompound().copy();
		NBTTagCompound itemTag = (NBTTagCompound) item.getTagCompound().copy();
		filterTag.removeTag("GEN");
		itemTag.removeTag("GEN");
		return filterTag.equals(itemTag);
	}

	private int[] getCachedIds(int filterItemIndex)
	{
		int[] res = this.oreIds.get(filterItemIndex);
		if (res == null)
		{
			ItemStack item = this.items[filterItemIndex];

			/* TODO gamerforEA code replace, old code:
			if (item == null)
				res = new int[0];
			else
			{
				res = OreDictionary.getOreIDs(item);
				if (res == null)
					res = new int[0];
			} */
			res = item == null ? ArrayUtils.EMPTY_INT_ARRAY : FastOreDictionary.getOreIDsCopy(item);
			// TODO gamerforEA code end

			this.oreIds.set(filterItemIndex, res);
		}

		return res;
	}

	@Override
	public boolean isValid()
	{
		for (ItemStack item : this.items)
		{
			if (item != null)
				return true;
		}
		return false;
	}

	public boolean isBlacklist()
	{
		return this.isBlacklist;
	}

	public void setBlacklist(boolean isBlacklist)
	{
		this.isBlacklist = isBlacklist;
	}

	public boolean isMatchMeta()
	{
		return this.matchMeta;
	}

	public void setMatchMeta(boolean matchMeta)
	{
		this.matchMeta = matchMeta;
	}

	public boolean isMatchNBT()
	{
		return this.matchNBT;
	}

	public void setMatchNBT(boolean matchNbt)
	{
		this.matchNBT = matchNbt;
	}

	public boolean isUseOreDict()
	{
		return this.useOreDict;
	}

	public void setUseOreDict(boolean useOreDict)
	{
		this.useOreDict = useOreDict;
	}

	@Override
	public boolean isSticky()
	{
		return this.sticky;
	}

	public void setSticky(boolean sticky)
	{
		this.sticky = sticky;
	}

	public FuzzyMode getFuzzyMode()
	{
		return this.fuzzyMode;
	}

	public void setFuzzyMode(FuzzyMode fuzzyMode)
	{
		this.fuzzyMode = fuzzyMode;
	}

	@Override
	public void writeToNBT(NBTTagCompound nbtRoot)
	{
		nbtRoot.setBoolean("isBlacklist", this.isBlacklist);
		nbtRoot.setBoolean("matchMeta", this.matchMeta);
		nbtRoot.setBoolean("matchNBT", this.matchNBT);
		nbtRoot.setBoolean("useOreDict", this.useOreDict);
		nbtRoot.setBoolean("sticky", this.sticky);
		nbtRoot.setBoolean("isAdvanced", this.isAdvanced);
		nbtRoot.setByte("fuzzyMode", (byte) this.fuzzyMode.ordinal());

		int i = 0;
		for (ItemStack item : this.items)
		{
			NBTTagCompound itemTag = new NBTTagCompound();
			if (item != null)
			{
				item.writeToNBT(itemTag);
				nbtRoot.setTag("item" + i, itemTag);
			}
			i++;
		}

	}

	@Override
	@SideOnly(Side.CLIENT)
	public IItemFilterGui getGui(GuiExternalConnection gui, IItemConduit itemConduit, boolean isInput)
	{
		ItemConduitFilterContainer cont = new ItemConduitFilterContainer(itemConduit, gui.getDir(), isInput);
		BasicItemFilterGui basicItemFilterGui = new BasicItemFilterGui(gui, cont, !isInput);
		basicItemFilterGui.createFilterSlots();
		return basicItemFilterGui;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbtRoot)
	{
		this.isBlacklist = nbtRoot.getBoolean("isBlacklist");
		this.matchMeta = nbtRoot.getBoolean("matchMeta");
		this.matchNBT = nbtRoot.getBoolean("matchNBT");
		this.useOreDict = nbtRoot.getBoolean("useOreDict");
		this.sticky = nbtRoot.getBoolean("sticky");
		this.isAdvanced = nbtRoot.getBoolean("isAdvanced");
		this.fuzzyMode = FuzzyMode.values()[nbtRoot.getByte("fuzzyMode") & 255];

		int numItems = this.isAdvanced ? 10 : 5;
		this.items = new ItemStack[numItems];
		this.oreIds.clear();
		for (int i = 0; i < numItems; i++)
		{
			this.oreIds.add(null);
		}
		for (int i = 0; i < numItems; i++)
		{
			NBTBase tag = nbtRoot.getTag("item" + i);
			if (tag instanceof NBTTagCompound)
				this.items[i] = ItemStack.loadItemStackFromNBT((NBTTagCompound) tag);
			else
				this.items[i] = null;
		}
	}

	@Override
	public void writeToByteBuf(ByteBuf buf)
	{
		NBTTagCompound root = new NBTTagCompound();
		this.writeToNBT(root);
		NetworkUtil.writeNBTTagCompound(root, buf);
	}

	@Override
	public void readFromByteBuf(ByteBuf buf)
	{
		NBTTagCompound tag = NetworkUtil.readNBTTagCompound(buf);
		this.readFromNBT(tag);
	}

	@Override
	public int getSizeInventory()
	{
		return this.items.length;
	}

	@Override
	public ItemStack getStackInSlot(int i)
	{
		if (i < 0 || i >= this.items.length)
			return null;
		return this.items[i];
	}

	@Override
	public ItemStack decrStackSize(int fromSlot, int amount)
	{
		this.oreIds.set(fromSlot, null);
		ItemStack item = this.items[fromSlot];
		this.items[fromSlot] = null;
		if (item == null)
			return null;
		item.stackSize = 0;
		return item;
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int i)
	{
		return null;
	}

	@Override
	public void setInventorySlotContents(int i, ItemStack itemstack)
	{
		if (itemstack != null)
		{
			this.items[i] = itemstack.copy();
			this.items[i].stackSize = 0;
		}
		else
			this.items[i] = null;
		this.oreIds.set(i, null);
	}

	@Override
	public String getInventoryName()
	{
		return "Item Filter";
	}

	@Override
	public int getInventoryStackLimit()
	{
		return 0;
	}

	@Override
	public boolean hasCustomInventoryName()
	{
		return false;
	}

	@Override
	public void markDirty()
	{
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer entityplayer)
	{
		return true;
	}

	@Override
	public void openInventory()
	{
	}

	@Override
	public void closeInventory()
	{
	}

	@Override
	public boolean isItemValidForSlot(int i, ItemStack itemstack)
	{
		return true;
	}

	@Override
	public void createGhostSlots(List<GhostSlot> slots, int xOffset, int yOffset, Runnable cb)
	{
		int topY = yOffset;
		int leftX = xOffset;
		int index = 0;
		int numRows = this.isAdvanced ? 2 : 1;
		for (int row = 0; row < numRows; ++row)
		{
			for (int col = 0; col < 5; ++col)
			{
				int x = leftX + col * 18;
				int y = topY + row * 20;
				slots.add(new ItemFilterGhostSlot(index, x, y, cb));
				index++;
			}
		}
	}

	@Override
	public int getSlotCount()
	{
		return this.getSizeInventory();
	}

	public boolean isAdvanced()
	{
		return this.isAdvanced;
	}

	public boolean isDefault()
	{
		return !this.isAdvanced && !this.isValid() && this.isBlacklist == DEFAULT_BLACKLIST && this.matchMeta == DEFAULT_META && this.matchNBT == DEFAULT_MBT && this.useOreDict == DEFAULT_ORE_DICT && this.sticky == DEFAULT_STICKY;
	}

	@Override
	public String toString()
	{
		//    return "ItemFilter [isBlacklist=" + isBlacklist + ", matchMeta=" + matchMeta + ", matchNBT=" + matchNBT + ", useOreDict=" + useOreDict + ", sticky="
		//        + sticky + ", items=" + Arrays.toString(items) + ", oreIds=" + Arrays.toString(oreIds) + ", isAdvanced=" + isAdvanced + "]";
		return "ItemFilter [isAdvanced=" + this.isAdvanced + ", items=" + Arrays.toString(this.items) + "]";
	}

	class ItemFilterGhostSlot extends GhostSlot
	{
		private final int slot;
		private final Runnable cb;

		ItemFilterGhostSlot(int slot, int x, int y, Runnable cb)
		{
			this.x = x;
			this.y = y;
			this.slot = slot;
			this.cb = cb;
		}

		@Override
		public void putStack(ItemStack stack)
		{
			if (stack != null)
			{
				stack = stack.copy();
				stack.stackSize = 1;
			}
			ItemFilter.this.items[this.slot] = stack;
			this.cb.run();
		}

		@Override
		public ItemStack getStack()
		{
			return ItemFilter.this.items[this.slot];
		}
	}
}
