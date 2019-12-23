package crazypants.enderio.machine;

import com.enderio.core.common.util.BlockCoord;
import com.enderio.core.common.util.InventoryWrapper;
import com.enderio.core.common.util.ItemUtil;
import com.gamerforea.enderio.ModUtils;
import com.gamerforea.eventhelper.fake.FakePlayerContainer;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import crazypants.enderio.EnderIO;
import crazypants.enderio.TileEntityEio;
import crazypants.enderio.api.redstone.IRedstoneConnectable;
import crazypants.enderio.config.Config;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import org.apache.commons.lang3.ArrayUtils;

import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public abstract class AbstractMachineEntity extends TileEntityEio
		implements ISidedInventory, IMachine, IRedstoneModeControlable, IRedstoneConnectable, IIoConfigurable
{

	public short facing;

	// Client sync monitoring
	protected int ticksSinceSync = -1;
	protected boolean forceClientUpdate = true;
	protected boolean lastActive;
	protected int ticksSinceActiveChanged = 0;

	protected ItemStack[] inventory;
	protected final SlotDefinition slotDefinition;

	protected RedstoneControlMode redstoneControlMode;

	protected boolean redstoneCheckPassed;

	private boolean redstoneStateDirty = true;

	protected Map<ForgeDirection, IoMode> faceModes;

	private final int[] allSlots;

	protected boolean notifyNeighbours = false;

	@SideOnly(Side.CLIENT)
	private MachineSound sound;

	private final ResourceLocation soundRes;

	public boolean isDirty = false;

	// TODO gamerforEA code start
	public final FakePlayerContainer fake = ModUtils.NEXUS_FACTORY.wrapFake(this);
	// TODO gamerforEA code end

	public static ResourceLocation getSoundFor(String sound)
	{
		return sound == null ? null : new ResourceLocation(EnderIO.DOMAIN + ":" + sound);
	}

	public AbstractMachineEntity(SlotDefinition slotDefinition)
	{
		this.slotDefinition = slotDefinition;
		this.facing = 3;

		this.inventory = new ItemStack[slotDefinition.getNumSlots()];
		this.redstoneControlMode = RedstoneControlMode.IGNORE;
		this.soundRes = getSoundFor(this.getSoundName());

		this.allSlots = new int[slotDefinition.getNumSlots()];
		for (int i = 0; i < this.allSlots.length; i++)
		{
			this.allSlots[i] = i;
		}
	}

	@Override
	public IoMode toggleIoModeForFace(ForgeDirection faceHit)
	{
		IoMode curMode = this.getIoMode(faceHit);
		IoMode mode = curMode.next();
		while (!this.supportsMode(faceHit, mode))
		{
			mode = mode.next();
		}
		this.setIoMode(faceHit, mode);
		return mode;
	}

	@Override
	public boolean supportsMode(ForgeDirection faceHit, IoMode mode)
	{
		return true;
	}

	@Override
	public void setIoMode(ForgeDirection faceHit, IoMode mode)
	{
		if (mode == IoMode.NONE && this.faceModes == null)
			return;
		if (this.faceModes == null)
			this.faceModes = new EnumMap<ForgeDirection, IoMode>(ForgeDirection.class);
		this.faceModes.put(faceHit, mode);
		this.forceClientUpdate = true;
		this.notifyNeighbours = true;

		this.updateBlock();
	}

	@Override
	public void clearAllIoModes()
	{
		if (this.faceModes != null)
		{
			this.faceModes = null;
			this.forceClientUpdate = true;
			this.notifyNeighbours = true;
			this.updateBlock();
		}
	}

	@Override
	public IoMode getIoMode(ForgeDirection face)
	{
		if (this.faceModes == null)
			return IoMode.NONE;
		IoMode res = this.faceModes.get(face);
		if (res == null)
			return IoMode.NONE;
		return res;
	}

	public SlotDefinition getSlotDefinition()
	{
		return this.slotDefinition;
	}

	public boolean isValidUpgrade(ItemStack itemstack)
	{
		for (int i = this.slotDefinition.getMinUpgradeSlot(); i <= this.slotDefinition.getMaxUpgradeSlot(); i++)
		{
			if (this.isItemValidForSlot(i, itemstack))
				return true;
		}
		return false;
	}

	public boolean isValidInput(ItemStack itemstack)
	{
		for (int i = this.slotDefinition.getMinInputSlot(); i <= this.slotDefinition.getMaxInputSlot(); i++)
		{
			if (this.isItemValidForSlot(i, itemstack))
				return true;
		}
		return false;
	}

	public boolean isValidOutput(ItemStack itemstack)
	{
		for (int i = this.slotDefinition.getMinOutputSlot(); i <= this.slotDefinition.getMaxOutputSlot(); i++)
		{
			if (this.isItemValidForSlot(i, itemstack))
				return true;
		}
		return false;
	}

	@Override
	public final boolean isItemValidForSlot(int i, ItemStack itemstack)
	{
		if (this.slotDefinition.isUpgradeSlot(i))
			return itemstack != null && itemstack.getItem() == EnderIO.itemBasicCapacitor && itemstack.getItemDamage() > 0;
		return this.isMachineItemValidForSlot(i, itemstack);
	}

	protected abstract boolean isMachineItemValidForSlot(int i, ItemStack itemstack);

	@Override
	public RedstoneControlMode getRedstoneControlMode()
	{
		return this.redstoneControlMode;
	}

	@Override
	public void setRedstoneControlMode(RedstoneControlMode redstoneControlMode)
	{
		this.redstoneControlMode = redstoneControlMode;
		this.redstoneStateDirty = true;
		this.updateBlock();
	}

	public short getFacing()
	{
		return this.facing;
	}

	public ForgeDirection getFacingDir()
	{
		return ForgeDirection.getOrientation(this.facing);
	}

	public void setFacing(short facing)
	{
		this.facing = facing;
	}

	public abstract boolean isActive();

	public String getSoundName()
	{
		return null;
	}

	public boolean hasSound()
	{
		return this.getSoundName() != null;
	}

	public float getVolume()
	{
		return Config.machineSoundVolume;
	}

	public float getPitch()
	{
		return 1.0f;
	}

	protected boolean shouldPlaySound()
	{
		return this.isActive() && !this.isInvalid();
	}

	@SideOnly(Side.CLIENT)
	private void updateSound()
	{
		if (Config.machineSoundsEnabled && this.hasSound())
			if (this.shouldPlaySound())
			{
				if (this.sound == null)
				{
					this.sound = new MachineSound(this.soundRes, this.xCoord + 0.5f, this.yCoord + 0.5f, this.zCoord + 0.5f, this.getVolume(), this.getPitch());
					FMLClientHandler.instance().getClient().getSoundHandler().playSound(this.sound);
				}
			}
			else if (this.sound != null)
			{
				this.sound.endPlaying();
				this.sound = null;
			}
	}

	// --- Process Loop
	// --------------------------------------------------------------------------

	@Override
	public void doUpdate()
	{
		if (this.worldObj.isRemote)
		{
			this.updateEntityClient();
			return;
		} // else is server, do all logic only on the server

		boolean requiresClientSync = this.forceClientUpdate;
		boolean prevRedCheck = this.redstoneCheckPassed;
		if (this.redstoneStateDirty)
		{
			this.redstoneCheckPassed = RedstoneControlMode.isConditionMet(this.redstoneControlMode, this);
			this.redstoneStateDirty = false;
		}

		if (this.shouldDoWorkThisTick(5))
			requiresClientSync |= this.doSideIo();

		requiresClientSync |= prevRedCheck != this.redstoneCheckPassed;

		requiresClientSync |= this.processTasks(this.redstoneCheckPassed);

		if (requiresClientSync)
		{

			// this will cause 'getPacketDescription()' to be called and its result
			// will be sent to the PacketHandler on the other end of
			// client/server connection
			this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
			// And this will make sure our current tile entity state is saved
			this.markDirty();
		}

		if (this.notifyNeighbours)
		{
			this.worldObj.notifyBlocksOfNeighborChange(this.xCoord, this.yCoord, this.zCoord, this.getBlockType());
			this.notifyNeighbours = false;
		}

	}

	protected void updateEntityClient()
	{
		// check if the block on the client needs to update its texture
		if (this.isActive() != this.lastActive)
		{
			this.ticksSinceActiveChanged++;
			if (this.ticksSinceActiveChanged > 20 || this.isActive())
			{
				this.ticksSinceActiveChanged = 0;
				this.lastActive = this.isActive();
				this.forceClientUpdate = true;
			}
		}

		if (this.hasSound())
			this.updateSound();

		if (this.forceClientUpdate)
		{
			if (this.worldObj.rand.nextInt(1024) <= (this.isDirty ? 256 : 0))
				this.isDirty = !this.isDirty;
			this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
			this.forceClientUpdate = false;
		}
	}

	protected boolean doSideIo()
	{
		if (this.faceModes == null)
			return false;

		boolean res = false;
		Set<Entry<ForgeDirection, IoMode>> ents = this.faceModes.entrySet();
		for (Entry<ForgeDirection, IoMode> ent : ents)
		{
			IoMode mode = ent.getValue();
			if (mode.pulls())
				res = res | this.doPull(ent.getKey());
			if (mode.pushes())
				res = res | this.doPush(ent.getKey());
		}
		return res;
	}

	protected boolean doPush(ForgeDirection dir)
	{

		if (this.slotDefinition.getNumOutputSlots() <= 0)
			return false;
		if (!this.shouldDoWorkThisTick(20))
			return false;

		BlockCoord loc = this.getLocation().getLocation(dir);

		// TODO gamerforEA use ModUtils.getTileEntity
		TileEntity te = ModUtils.getTileEntity(this.worldObj, loc.x, loc.y, loc.z);

		return this.doPush(dir, te, this.slotDefinition.minOutputSlot, this.slotDefinition.maxOutputSlot);
	}

	protected boolean doPush(ForgeDirection dir, TileEntity te, int minSlot, int maxSlot)
	{
		if (te == null)
			return false;
		for (int i = minSlot; i <= maxSlot; i++)
		{
			ItemStack item = this.inventory[i];
			if (item != null)
			{
				int num = ItemUtil.doInsertItem(te, item, dir.getOpposite());
				if (num > 0)
				{
					item.stackSize -= num;
					if (item.stackSize <= 0)
						item = null;
					this.inventory[i] = item;
					this.markDirty();
				}
			}
		}
		return false;
	}

	protected boolean doPull(ForgeDirection dir)
	{

		if (this.slotDefinition.getNumInputSlots() <= 0)
			return false;
		if (!this.shouldDoWorkThisTick(20))
			return false;

		boolean hasSpace = false;
		for (int slot = this.slotDefinition.minInputSlot; slot <= this.slotDefinition.maxInputSlot && !hasSpace; slot++)
		{
			hasSpace = this.inventory[slot] == null || this.inventory[slot].stackSize < Math.min(this.inventory[slot].getMaxStackSize(), this.getInventoryStackLimit(slot));
		}
		if (!hasSpace)
			return false;

		BlockCoord loc = this.getLocation().getLocation(dir);

		// TODO gamerforEA use ModUtils.getTileEntity
		TileEntity te = ModUtils.getTileEntity(this.worldObj, loc.x, loc.y, loc.z);

		if (te == null)
			return false;
		if (!(te instanceof IInventory))
			return false;
		ISidedInventory target;
		if (te instanceof ISidedInventory)
			target = (ISidedInventory) te;
		else
			target = new InventoryWrapper((IInventory) te);

		int[] targetSlots = target.getAccessibleSlotsFromSide(dir.getOpposite().ordinal());
		if (targetSlots == null)
			return false;

		for (int inputSlot = this.slotDefinition.minInputSlot; inputSlot <= this.slotDefinition.maxInputSlot; inputSlot++)
		{
			if (this.doPull(inputSlot, target, targetSlots, dir))
				return false;
		}
		return false;
	}

	protected boolean doPull(int inputSlot, ISidedInventory target, int[] targetSlots, ForgeDirection side)
	{
		for (int i = 0; i < targetSlots.length; i++)
		{
			int tSlot = targetSlots[i];
			ItemStack targetStack = target.getStackInSlot(tSlot);
			if (targetStack != null && target.canExtractItem(i, targetStack, side.getOpposite().ordinal()))
			{
				int res = ItemUtil.doInsertItem(this, targetStack, side);
				if (res > 0)
				{
					targetStack = targetStack.copy();
					targetStack.stackSize -= res;
					if (targetStack.stackSize <= 0)
						targetStack = null;
					target.setInventorySlotContents(tSlot, targetStack);
					return true;
				}
			}
		}
		return false;
	}

	protected abstract boolean processTasks(boolean redstoneCheckPassed);

	// ---- Tile Entity
	// ------------------------------------------------------------------------------

	@Override
	public void invalidate()
	{
		super.invalidate();
		if (this.worldObj.isRemote)
			this.updateSound();
	}

	@Override
	public void readCustomNBT(NBTTagCompound nbtRoot)
	{

		this.setFacing(nbtRoot.getShort("facing"));
		this.redstoneCheckPassed = nbtRoot.getBoolean("redstoneCheckPassed");
		this.forceClientUpdate = nbtRoot.getBoolean("forceClientUpdate");
		this.readCommon(nbtRoot);
	}

	/**
	 * Read state common to both block and item
	 */
	public void readCommon(NBTTagCompound nbtRoot)
	{

		// read in the inventories contents
		this.inventory = new ItemStack[this.slotDefinition.getNumSlots()];

		NBTTagList itemList = (NBTTagList) nbtRoot.getTag("Items");
		if (itemList != null)
			for (int i = 0; i < itemList.tagCount(); i++)
			{
				NBTTagCompound itemStack = itemList.getCompoundTagAt(i);
				byte slot = itemStack.getByte("Slot");
				if (slot >= 0 && slot < this.inventory.length)
					this.inventory[slot] = ItemStack.loadItemStackFromNBT(itemStack);
			}

		int rsContr = nbtRoot.getInteger("redstoneControlMode");
		if (rsContr < 0 || rsContr >= RedstoneControlMode.values().length)
			rsContr = 0;
		this.redstoneControlMode = RedstoneControlMode.values()[rsContr];

		if (nbtRoot.hasKey("hasFaces"))
			for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS)
			{
				if (nbtRoot.hasKey("face" + dir.ordinal()))
					this.setIoMode(dir, IoMode.values()[nbtRoot.getShort("face" + dir.ordinal())]);
			}

		// TODO gamerforEA code start
		this.fake.readFromNBT(nbtRoot);
		// TODO gamerforEA code end
	}

	public void readFromItemStack(ItemStack stack)
	{
		if (stack == null || stack.stackTagCompound == null)
			return;
		NBTTagCompound root = stack.stackTagCompound;
		if (!root.hasKey("eio.abstractMachine"))
			return;
		this.readCommon(root);
	}

	@Override
	public void writeCustomNBT(NBTTagCompound nbtRoot)
	{
		nbtRoot.setShort("facing", this.facing);
		nbtRoot.setBoolean("redstoneCheckPassed", this.redstoneCheckPassed);
		nbtRoot.setBoolean("forceClientUpdate", this.forceClientUpdate);
		this.forceClientUpdate = false;

		this.writeCommon(nbtRoot);
	}

	/**
	 * Write state common to both block and item
	 */
	public void writeCommon(NBTTagCompound nbtRoot)
	{
		// write inventory list
		NBTTagList itemList = new NBTTagList();
		for (int i = 0; i < this.inventory.length; i++)
		{
			if (this.inventory[i] != null)
			{
				NBTTagCompound itemStackNBT = new NBTTagCompound();
				itemStackNBT.setByte("Slot", (byte) i);
				this.inventory[i].writeToNBT(itemStackNBT);
				itemList.appendTag(itemStackNBT);
			}
		}
		nbtRoot.setTag("Items", itemList);

		nbtRoot.setInteger("redstoneControlMode", this.redstoneControlMode.ordinal());

		//face modes
		if (this.faceModes != null)
		{
			nbtRoot.setByte("hasFaces", (byte) 1);
			for (Entry<ForgeDirection, IoMode> e : this.faceModes.entrySet())
			{
				nbtRoot.setShort("face" + e.getKey().ordinal(), (short) e.getValue().ordinal());
			}
		}

		// TODO gamerforEA code start
		this.fake.writeToNBT(nbtRoot);
		// TODO gamerforEA code end
	}

	public void writeToItemStack(ItemStack stack)
	{
		if (stack == null)
			return;
		if (stack.stackTagCompound == null)
			stack.stackTagCompound = new NBTTagCompound();

		NBTTagCompound root = stack.stackTagCompound;
		root.setBoolean("eio.abstractMachine", true);
		this.writeCommon(root);

		String name;
		if (stack.hasDisplayName())
			name = stack.getDisplayName();
		else
			name = EnderIO.lang.localizeExact(stack.getUnlocalizedName() + ".name");
		name += " " + EnderIO.lang.localize("machine.tooltip.configured");
		stack.setStackDisplayName(name);
	}

	// ---- Inventory
	// ------------------------------------------------------------------------------

	@Override
	public boolean isUseableByPlayer(EntityPlayer player)
	{
		return this.canPlayerAccess(player);
	}

	@Override
	public int getSizeInventory()
	{
		return this.slotDefinition.getNumSlots();
	}

	public int getInventoryStackLimit(int slot)
	{
		return this.getInventoryStackLimit();
	}

	@Override
	public int getInventoryStackLimit()
	{
		return 64;
	}

	@Override
	public ItemStack getStackInSlot(int slot)
	{
		if (slot < 0 || slot >= this.inventory.length)
			return null;
		return this.inventory[slot];
	}

	@Override
	public ItemStack decrStackSize(int fromSlot, int amount)
	{
		ItemStack fromStack = this.inventory[fromSlot];
		if (fromStack == null)
			return null;
		if (fromStack.stackSize <= amount)
		{
			this.inventory[fromSlot] = null;
			return fromStack;
		}
		ItemStack result = new ItemStack(fromStack.getItem(), amount, fromStack.getItemDamage());
		if (fromStack.stackTagCompound != null)
			result.stackTagCompound = (NBTTagCompound) fromStack.stackTagCompound.copy();
		fromStack.stackSize -= amount;
		return result;
	}

	@Override
	public void setInventorySlotContents(int slot, ItemStack contents)
	{
		if (contents == null)
			this.inventory[slot] = contents;
		else
			this.inventory[slot] = contents.copy();

		if (contents != null && contents.stackSize > this.getInventoryStackLimit(slot))
			contents.stackSize = this.getInventoryStackLimit(slot);
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int i)
	{
		return null;
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
	public String getInventoryName()
	{
		return this.getMachineName();
	}

	@Override
	public boolean hasCustomInventoryName()
	{
		return false;
	}

	@Override
	public int[] getAccessibleSlotsFromSide(int side)
	{
		if (this.isSideDisabled(side))
			// TODO gamerforEA code replace, old code:
			// return new int[0];
			return ArrayUtils.EMPTY_INT_ARRAY;
		// TODO gamerforEA code end

		return this.allSlots;
	}

	@Override
	public boolean canInsertItem(int slot, ItemStack stack, int side)
	{
		if (this.isSideDisabled(side) || !this.slotDefinition.isInputSlot(slot))
			return false;
		ItemStack existing = this.inventory[slot];
		if (existing != null)
		{
			// no point in checking the recipes if an item is already in the slot
			// worst case we get more of the wrong item - but that doesn't change anything

			boolean isEqual = existing.isStackable() && existing.isItemEqual(stack);

			// TODO gamerforEA code start
			isEqual &= ItemStack.areItemStackTagsEqual(existing, stack);
			// TODO gamerforEA code end

			return isEqual;
		}
		// no need to call isItemValidForSlot as upgrade slots are not input slots
		return this.isMachineItemValidForSlot(slot, stack);
	}

	@Override
	public boolean canExtractItem(int slot, ItemStack itemstack, int side)
	{
		if (this.isSideDisabled(side))
			return false;
		if (!this.slotDefinition.isOutputSlot(slot))
			return false;
		return this.canExtractItem(slot, itemstack);
	}

	protected boolean canExtractItem(int slot, ItemStack stack)
	{
		if (this.inventory[slot] == null || this.inventory[slot].stackSize < stack.stackSize)
			return false;

		boolean isEqual = stack.getItem() == this.inventory[slot].getItem();

		// TODO gamerforEA code start
		isEqual &= ItemStack.areItemStackTagsEqual(this.inventory[slot], stack);
		// TODO gamerforEA code end

		return isEqual;
	}

	public boolean isSideDisabled(int var1)
	{
		ForgeDirection dir = ForgeDirection.getOrientation(var1);
		IoMode mode = this.getIoMode(dir);
		return mode == IoMode.DISABLED;
	}

	public void onNeighborBlockChange(Block blockId)
	{
		this.redstoneStateDirty = true;
	}

	/* IRedstoneConnectable */

	@Override
	public boolean shouldRedstoneConduitConnect(World world, int x, int y, int z, ForgeDirection from)
	{
		return true;
	}
}
