package crazypants.enderio.conduit.item;

import com.enderio.core.common.util.BlockCoord;
import com.enderio.core.common.util.InventoryWrapper;
import com.enderio.core.common.util.ItemUtil;
import com.enderio.core.common.util.RoundRobinIterator;
import com.gamerforea.enderio.EmptyInventory;
import com.gamerforea.enderio.ModUtils;
import crazypants.enderio.conduit.ConnectionMode;
import crazypants.enderio.conduit.item.filter.IItemFilter;
import crazypants.enderio.config.Config;
import crazypants.enderio.machine.invpanel.TileInventoryPanel;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.*;

public class NetworkedInventory
{
	private ISidedInventory inv;
	IItemConduit con;
	ForgeDirection conDir;
	BlockCoord location;
	int inventorySide;

	List<Target> sendPriority = new ArrayList<Target>();
	RoundRobinIterator<Target> rrIter = new RoundRobinIterator<Target>(this.sendPriority);

	private int extractFromSlot = -1;

	int tickDeficit;

	//work around for a vanilla chest changing into a double chest without doing unneeded checks all the time
	boolean recheckInv = false;
	//Hack for TiC crafting station not working correctly when setting output slot to null
	boolean ticHack = false;

	boolean inventoryPanel = false;

	World world;
	ItemConduitNetwork network;

	// TODO gamerforEA code start
	private TileEntity invTile;
	// TODO gamerforEA code end

	NetworkedInventory(ItemConduitNetwork network, IInventory inv, IItemConduit con, ForgeDirection conDir, BlockCoord location)
	{
		this.network = network;
		this.inventorySide = conDir.getOpposite().ordinal();

		this.con = con;
		this.conDir = conDir;
		this.location = location;
		this.world = con.getBundle().getWorld();

		// TODO gamerforEA use ModUtils.getTileEntity
		TileEntity te = ModUtils.getTileEntity(this.world, location.x, location.y, location.z);

		if (te.getClass().getName().equals("tconstruct.tools.logic.CraftingStationLogic"))
			this.ticHack = true;
		else if (te.getClass().getName().contains("cpw.mods.ironchest"))
			this.recheckInv = true;
		else if (te instanceof TileEntityChest)
			this.recheckInv = true;
		else if (te instanceof TileInventoryPanel)
			this.inventoryPanel = true;
		this.updateInventory();
	}

	public boolean hasTarget(IItemConduit conduit, ForgeDirection dir)
	{
		for (Target t : this.sendPriority)
		{
			if (t.inv.con == conduit && t.inv.conDir == dir)
				return true;
		}
		return false;
	}

	boolean canExtract()
	{
		// TODO gamerforEA code start
		if (!ModUtils.isValidTileEntity(this.invTile))
			return false;
		// TODO gamerforEA code end

		ConnectionMode mode = this.con.getConnectionMode(this.conDir);
		return mode == ConnectionMode.INPUT || mode == ConnectionMode.IN_OUT;
	}

	boolean canInsert()
	{
		if (this.inventoryPanel)
			return false;

		// TODO gamerforEA code start
		if (!ModUtils.isValidTileEntity(this.invTile))
			return false;
		// TODO gamerforEA code end

		ConnectionMode mode = this.con.getConnectionMode(this.conDir);
		return mode == ConnectionMode.OUTPUT || mode == ConnectionMode.IN_OUT;
	}

	boolean isInventoryPanel()
	{
		return this.inventoryPanel;
	}

	boolean isSticky()
	{
		return this.con.getOutputFilter(this.conDir) != null && this.con.getOutputFilter(this.conDir).isValid() && this.con.getOutputFilter(this.conDir).isSticky();
	}

	int getPriority()
	{
		return this.con.getOutputPriority(this.conDir);
	}

	public void onTick()
	{
		if (this.tickDeficit > 0 || !this.canExtract() || !this.con.isExtractionRedstoneConditionMet(this.conDir))
		{
			//do nothing
		}
		else
			this.transferItems();

		this.tickDeficit--;
		//Sleep for a second before checking again.
		if (this.tickDeficit < -1)
			this.tickDeficit = 20;
	}

	private boolean canExtractThisTick(long tick)
	{
		return this.con.isExtractionRedstoneConditionMet(this.conDir);
	}

	private int nextSlot(int numSlots)
	{
		++this.extractFromSlot;
		if (this.extractFromSlot >= numSlots || this.extractFromSlot < 0)
			this.extractFromSlot = 0;
		return this.extractFromSlot;
	}

	private void setNextStartingSlot(int slot)
	{
		this.extractFromSlot = slot;
		this.extractFromSlot--;
	}

	private boolean transferItems()
	{

		if (this.recheckInv)
			this.updateInventory();

		int[] slotIndices = this.getInventory().getAccessibleSlotsFromSide(this.inventorySide);
		if (slotIndices == null)
			return false;
		int numSlots = slotIndices.length;
		ItemStack extractItem = null;
		int maxExtracted = this.con.getMaximumExtracted(this.conDir);

		int slot = -1;
		int slotChecksPerTick = Math.min(numSlots, ItemConduitNetwork.MAX_SLOT_CHECK_PER_TICK);
		for (int i = 0; i < slotChecksPerTick; i++)
		{
			int index = this.nextSlot(numSlots);
			slot = slotIndices[index];
			ItemStack item = this.getInventory().getStackInSlot(slot);
			if (this.canExtractItem(item))
			{
				extractItem = item.copy();
				if (this.getInventory().canExtractItem(slot, extractItem, this.inventorySide))
					if (this.doTransfer(extractItem, slot, maxExtracted))
					{
						this.setNextStartingSlot(slot);
						return true;
					}
			}
		}
		return false;
	}

	private boolean canExtractItem(ItemStack itemStack)
	{
		if (itemStack == null)
			return false;
		IItemFilter filter = this.con.getInputFilter(this.conDir);
		if (filter == null)
			return true;
		return filter.doesItemPassFilter(this, itemStack);
	}

	private boolean doTransfer(ItemStack extractedItem, int slot, int maxExtract)
	{
		if (extractedItem == null || extractedItem.getItem() == null)
			return false;
		ItemStack toExtract = extractedItem.copy();
		toExtract.stackSize = Math.min(maxExtract, toExtract.stackSize);

		// TODO gamerforEA swap insert and extract

		int numInserted = this.insertIntoTargets(toExtract);
		if (numInserted <= 0)
			return false;
		this.itemExtracted(slot, numInserted);
		return true;

	}

	public void itemExtracted(int slot, int numInserted)
	{
		ItemStack curStack = this.getInventory().getStackInSlot(slot);
		if (curStack != null)
			if (this.ticHack)
			{
				this.getInventory().decrStackSize(slot, numInserted);
				this.getInventory().markDirty();
			}
			else
			{
				curStack = curStack.copy();
				curStack.stackSize -= numInserted;
				if (curStack.stackSize > 0)
				{
					this.getInventory().setInventorySlotContents(slot, curStack);
					this.getInventory().markDirty();
				}
				else
				{
					this.getInventory().setInventorySlotContents(slot, null);
					this.getInventory().markDirty();
				}
			}
		this.con.itemsExtracted(numInserted, slot);
		this.tickDeficit = Math.round(numInserted * this.con.getTickTimePerItem(this.conDir));
	}

	int insertIntoTargets(ItemStack toExtract)
	{
		if (toExtract == null)
			return 0;

		int totalToInsert = toExtract.stackSize;
		int leftToInsert = totalToInsert;
		boolean matchedStickyInput = false;

		Iterable<Target> targets = this.getTargetIterator();

		//for (Target target : sendPriority) {
		for (Target target : targets)
		{
			if (target.stickyInput && !matchedStickyInput)
			{
				IItemFilter of = target.inv.con.getOutputFilter(target.inv.conDir);
				matchedStickyInput = of != null && of.isValid() && of.doesItemPassFilter(this, toExtract);
			}
			if (target.stickyInput || !matchedStickyInput)
			{
				if (target.inv.recheckInv)
					target.inv.updateInventory();
				int inserted = target.inv.insertItem(toExtract);
				if (inserted > 0)
				{
					toExtract.stackSize -= inserted;
					leftToInsert -= inserted;
				}
				if (leftToInsert <= 0)
					return totalToInsert;
			}
		}
		return totalToInsert - leftToInsert;
	}

	private Iterable<Target> getTargetIterator()
	{
		if (this.con.isRoundRobinEnabled(this.conDir))
			return this.rrIter;
		return this.sendPriority;
	}

	public final void updateInventory()
	{
		// TODO gamerforEA code start
		this.inv = null;
		// TODO gamerforEA code end

		// TODO gamerforEA use ModUtils.getTileEntity
		TileEntity te = ModUtils.getTileEntity(this.world, this.location.x, this.location.y, this.location.z);

		if (te instanceof ISidedInventory)
			this.inv = (ISidedInventory) te;
		else if (te instanceof IInventory)
			this.inv = new InventoryWrapper((IInventory) te);

		// TODO gamerforEA code start
		if (this.inv == null)
		{
			this.inv = new EmptyInventory();
			this.invTile = null;
		}
		else
			this.invTile = te;
		// TODO gamerforEA code end
	}

	private int insertItem(ItemStack item)
	{
		if (!this.canInsert() || item == null)
			return 0;
		IItemFilter filter = this.con.getOutputFilter(this.conDir);
		if (filter != null)
			if (!filter.doesItemPassFilter(this, item))
				return 0;
		return ItemUtil.doInsertItem(this.getInventory(), item, ForgeDirection.values()[this.inventorySide]);
	}

	void updateInsertOrder()
	{
		this.sendPriority.clear();
		if (!this.canExtract())
			return;
		List<Target> result = new ArrayList<NetworkedInventory.Target>();

		for (NetworkedInventory other : this.network.inventories)
		{
			if ((this.con.isSelfFeedEnabled(this.conDir) || other != this) && other.canInsert() && this.con.getInputColor(this.conDir) == other.con.getOutputColor(other.conDir))
				if (Config.itemConduitUsePhyscialDistance)
					this.sendPriority.add(new Target(other, this.distanceTo(other), other.isSticky(), other.getPriority()));
				else
					result.add(new Target(other, 9999999, other.isSticky(), other.getPriority()));
		}

		if (Config.itemConduitUsePhyscialDistance)
			Collections.sort(this.sendPriority);
		else if (!result.isEmpty())
		{
			Map<BlockCoord, Integer> visited = new HashMap<BlockCoord, Integer>();
			List<BlockCoord> steps = new ArrayList<BlockCoord>();
			steps.add(this.con.getLocation());
			this.calculateDistances(result, visited, steps, 0);

			this.sendPriority.addAll(result);

			Collections.sort(this.sendPriority);
		}

	}

	private void calculateDistances(List<Target> targets, Map<BlockCoord, Integer> visited, List<BlockCoord> steps, int distance)
	{
		if (steps == null || steps.isEmpty())
			return;

		ArrayList<BlockCoord> nextSteps = new ArrayList<BlockCoord>();
		for (BlockCoord bc : steps)
		{
			IItemConduit con = this.network.conMap.get(bc);
			if (con != null)
			{
				for (ForgeDirection dir : con.getExternalConnections())
				{
					Target target = this.getTarget(targets, con, dir);
					if (target != null && target.distance > distance)
						target.distance = distance;
				}

				if (!visited.containsKey(bc))
					visited.put(bc, distance);
				else
				{
					int prevDist = visited.get(bc);
					if (prevDist <= distance)
						continue;
					visited.put(bc, distance);
				}

				for (ForgeDirection dir : con.getConduitConnections())
				{
					nextSteps.add(bc.getLocation(dir));
				}
			}
		}
		this.calculateDistances(targets, visited, nextSteps, distance + 1);
	}

	private Target getTarget(List<Target> targets, IItemConduit con, ForgeDirection dir)
	{
		if (targets == null || con == null || con.getLocation() == null)
			return null;
		for (Target target : targets)
		{
			BlockCoord targetConLoc = null;
			if (target != null && target.inv != null && target.inv.con != null)
				targetConLoc = target.inv.con.getLocation();
			if (targetConLoc != null && target.inv.conDir == dir && targetConLoc.equals(con.getLocation()))
				return target;
		}
		return null;
	}

	private int distanceTo(NetworkedInventory other)
	{
		return this.con.getLocation().getDistSq(other.con.getLocation());
	}

	public ISidedInventory getInventory()
	{
		return this.inv;
	}

	public ISidedInventory getInventoryRecheck()
	{
		if (this.recheckInv)
			this.updateInventory();
		return this.inv;
	}

	public int getInventorySide()
	{
		return this.inventorySide;
	}

	public void setInventorySide(int inventorySide)
	{
		this.inventorySide = inventorySide;
	}

	public String getLocalizedInventoryName()
	{
		String inventoryName = this.getInventory().getInventoryName();
		// don't use Lang.localize as that passes the localized string to
		// String.format which might crash when it contains formatting specifiers
		if (inventoryName == null)
			return "null";
		else
			return StatCollector.translateToLocal(inventoryName);
	}

	static class Target implements Comparable<Target>
	{
		NetworkedInventory inv;
		int distance;
		boolean stickyInput;
		int priority;

		Target(NetworkedInventory inv, int distance, boolean stickyInput, int priority)
		{
			this.inv = inv;
			this.distance = distance;
			this.stickyInput = stickyInput;
			this.priority = priority;
		}

		@Override
		public int compareTo(Target o)
		{
			if (this.stickyInput && !o.stickyInput)
				return -1;
			if (!this.stickyInput && o.stickyInput)
				return 1;
			if (this.priority != o.priority)
				return ItemConduitNetwork.compare(o.priority, this.priority);
			return ItemConduitNetwork.compare(this.distance, o.distance);
		}

	}
}
