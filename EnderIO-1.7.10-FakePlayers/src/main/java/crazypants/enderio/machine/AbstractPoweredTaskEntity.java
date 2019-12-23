package crazypants.enderio.machine;

import com.enderio.core.api.common.util.IProgressTile;
import crazypants.enderio.machine.IMachineRecipe.ResultStack;
import crazypants.enderio.power.IInternalPowerReceiver;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public abstract class AbstractPoweredTaskEntity extends AbstractPowerConsumerEntity
		implements IInternalPowerReceiver, IProgressTile
{
	protected IPoweredTask currentTask = null;
	protected IMachineRecipe lastCompletedRecipe;
	protected IMachineRecipe cachedNextRecipe;

	protected final Random random = new Random();

	protected int ticksSinceCheckedRecipe = 0;
	protected boolean startFailed = false;
	protected float nextChance = Float.NaN;

	public AbstractPoweredTaskEntity(SlotDefinition slotDefinition)
	{
		super(slotDefinition);
	}

	@Override
	public int[] getAccessibleSlotsFromSide(int side)
	{
		ForgeDirection dir = ForgeDirection.getOrientation(side);
		IoMode mode = this.getIoMode(dir);
		if (mode == IoMode.DISABLED)
			// TODO gamerforEA code replace, old code:
			// return new int[0];
			return ArrayUtils.EMPTY_INT_ARRAY;
		// TODO gamerforEA code end

		int[] res = new int[this.inventory.length - this.slotDefinition.getNumUpgradeSlots()];
		int index = 0;
		for (int i = 0; i < this.inventory.length; i++)
		{
			if (!this.slotDefinition.isUpgradeSlot(i))
			{
				res[index] = i;
				index++;
			}
		}
		return res;
	}

	@Override
	public boolean isActive()
	{
		return this.currentTask != null && this.currentTask.getProgress() >= 0 && this.hasPower() && this.redstoneCheckPassed;
	}

	@Override
	public float getProgress()
	{
		return this.currentTask == null ? -1 : this.currentTask.getProgress();
	}

	@Override
	public TileEntity getTileEntity()
	{
		return this;
	}

	@Override
	public void setProgress(float progress)
	{
		this.currentTask = progress < 0 ? null : new PoweredTaskProgress(progress);
	}

	public IPoweredTask getCurrentTask()
	{
		return this.currentTask;
	}

	public float getExperienceForOutput(ItemStack output)
	{
		if (this.lastCompletedRecipe == null)
			return 0;
		return this.lastCompletedRecipe.getExperienceForOutput(output);
	}

	public boolean getRedstoneChecksPassed()
	{
		return this.redstoneCheckPassed;
	}

	@Override
	protected boolean processTasks(boolean redstoneChecksPassed)
	{

		if (!redstoneChecksPassed)
			return false;

		boolean requiresClientSync = false;
		// Process any current items
		requiresClientSync |= this.checkProgress(redstoneChecksPassed);

		if (this.currentTask != null || !this.hasPower() || !this.hasInputStacks())
			return requiresClientSync;

		if (this.startFailed)
		{
			this.ticksSinceCheckedRecipe++;
			if (this.ticksSinceCheckedRecipe < 20)
				return false;
		}
		this.ticksSinceCheckedRecipe = 0;

		// Get a new chance when we don't have one yet
		// If a recipe could not be started we will try with the same chance next time
		if (Float.isNaN(this.nextChance))
			this.nextChance = this.random.nextFloat();

		// Then see if we need to start a new one
		IMachineRecipe nextRecipe = this.canStartNextTask(this.nextChance);
		if (nextRecipe != null)
		{
			boolean started = this.startNextTask(nextRecipe, this.nextChance);
			if (started)
				// this chance value has been used up
				this.nextChance = Float.NaN;
			this.startFailed = !started;
		}
		else
			this.startFailed = true;
		this.sendTaskProgressPacket();

		return requiresClientSync;
	}

	protected boolean checkProgress(boolean redstoneChecksPassed)
	{
		if (this.currentTask == null || !this.hasPower())
			return false;
		if (redstoneChecksPassed && !this.currentTask.isComplete())
			this.usePower();
		// then check if we are done
		if (this.currentTask.isComplete())
		{
			this.taskComplete();
			return false;
		}

		return false;
	}

	protected double usePower()
	{
		return this.usePower(this.getPowerUsePerTick());
	}

	public int usePower(int wantToUse)
	{
		int used = Math.min(this.getEnergyStored(), wantToUse);
		this.setEnergyStored(Math.max(0, this.getEnergyStored() - used));
		if (this.currentTask != null)
			this.currentTask.update(used);
		return used;
	}

	protected void taskComplete()
	{
		if (this.currentTask != null)
		{
			this.lastCompletedRecipe = this.currentTask.getRecipe();
			ResultStack[] output = this.currentTask.getCompletedResult();
			if (output != null && output.length > 0)
				this.mergeResults(output);
		}
		this.markDirty();
		this.currentTask = null;
		this.lastProgressScaled = 0;
	}

	protected void mergeResults(ResultStack[] results)
	{
		List<ItemStack> outputStacks = new ArrayList<ItemStack>(this.slotDefinition.getNumOutputSlots());
		if (this.slotDefinition.getNumOutputSlots() > 0)
			for (int i = this.slotDefinition.minOutputSlot; i <= this.slotDefinition.maxOutputSlot; i++)
			{
				ItemStack it = this.inventory[i];
				if (it != null)
					it = it.copy();
				outputStacks.add(it);
			}

		for (ResultStack result : results)
		{
			if (result.item != null)
			{
				int numMerged = this.mergeItemResult(result.item, outputStacks);
				if (numMerged > 0)
					result.item.stackSize -= numMerged;
			}
			else if (result.fluid != null)
				this.mergeFluidResult(result);
		}

		if (this.slotDefinition.getNumOutputSlots() > 0)
		{
			int listIndex = 0;
			for (int i = this.slotDefinition.minOutputSlot; i <= this.slotDefinition.maxOutputSlot; i++)
			{
				ItemStack st = outputStacks.get(listIndex);
				if (st != null)
					st = st.copy();
				this.inventory[i] = st;
				listIndex++;
			}
		}

		this.cachedNextRecipe = null;
	}

	protected void mergeFluidResult(ResultStack result)
	{
	}

	protected void drainInputFluid(MachineRecipeInput fluid)
	{
	}

	protected boolean canInsertResultFluid(ResultStack fluid)
	{
		return false;
	}

	protected int mergeItemResult(ItemStack item, List<ItemStack> outputStacks)
	{

		int res = 0;

		ItemStack copy = item.copy();
		//try to add it to existing stacks first
		for (ItemStack outStack : outputStacks)
		{
			if (outStack != null && copy != null)
			{
				int num = this.getNumCanMerge(outStack, copy);
				outStack.stackSize += num;
				res += num;
				copy.stackSize -= num;
				if (copy.stackSize <= 0)
					return item.stackSize;
			}
		}

		//Try and add it to an empty slot
		for (int i = 0; i < outputStacks.size(); i++)
		{
			ItemStack outStack = outputStacks.get(i);
			if (outStack == null)
			{
				outputStacks.set(i, copy);
				return item.stackSize;
			}
		}

		return 0;
	}

	protected MachineRecipeInput[] getRecipeInputs()
	{
		MachineRecipeInput[] res = new MachineRecipeInput[this.slotDefinition.getNumInputSlots()];
		int fromSlot = this.slotDefinition.minInputSlot;
		for (int i = 0; i < res.length; i++)
		{
			res[i] = new MachineRecipeInput(fromSlot, this.inventory[fromSlot]);
			fromSlot++;
		}
		return res;
	}

	protected IMachineRecipe getNextRecipe()
	{
		if (this.cachedNextRecipe == null)
			this.cachedNextRecipe = MachineRecipeRegistry.instance.getRecipeForInputs(this.getMachineName(), this.getRecipeInputs());
		return this.cachedNextRecipe;
	}

	protected IMachineRecipe canStartNextTask(float chance)
	{
		IMachineRecipe nextRecipe = this.getNextRecipe();
		if (nextRecipe == null)
			return null; // no template
		// make sure we have room for the next output
		return this.canInsertResult(chance, nextRecipe) ? nextRecipe : null;
	}

	protected boolean canInsertResult(float chance, IMachineRecipe nextRecipe)
	{

		ResultStack[] nextResults = nextRecipe.getCompletedResult(chance, this.getRecipeInputs());
		List<ItemStack> outputStacks = new ArrayList<ItemStack>(this.slotDefinition.getNumOutputSlots());
		if (this.slotDefinition.getNumOutputSlots() > 0)
		{
			boolean allFull = true;
			for (int i = this.slotDefinition.minOutputSlot; i <= this.slotDefinition.maxOutputSlot; i++)
			{
				ItemStack st = this.inventory[i];
				if (st != null)
				{
					st = st.copy();
					if (allFull && st.stackSize < st.getMaxStackSize())
						allFull = false;
				}
				else
					allFull = false;
				outputStacks.add(st);
			}
			if (allFull)
				return false;
		}

		for (ResultStack result : nextResults)
		{
			if (result.item != null)
			{
				if (this.mergeItemResult(result.item, outputStacks) == 0)
					return false;
			}
			else if (result.fluid != null)
				if (!this.canInsertResultFluid(result))
					return false;
		}

		return true;
	}

	protected boolean hasInputStacks()
	{
		int fromSlot = this.slotDefinition.minInputSlot;
		for (int i = 0; i < this.slotDefinition.getNumInputSlots(); i++)
		{
			if (this.inventory[fromSlot] != null)
				return true;
			fromSlot++;
		}
		return false;
	}

	protected int getNumCanMerge(ItemStack stack, ItemStack result)
	{
		if (!stack.isItemEqual(result))
			return 0;

		// TODO gamerforEA code start
		if (!ItemStack.areItemStackTagsEqual(stack, result))
			return 0;
		// TODO gamerforEA code end

		return Math.min(stack.getMaxStackSize() - stack.stackSize, result.stackSize);
	}

	protected boolean startNextTask(IMachineRecipe nextRecipe, float chance)
	{
		if (this.hasPower() && nextRecipe.isRecipe(this.getRecipeInputs()))
		{
			// then get our recipe and take away the source items
			this.currentTask = this.createTask(nextRecipe, chance);
			List<MachineRecipeInput> consumed = nextRecipe.getQuantitiesConsumed(this.getRecipeInputs());
			for (MachineRecipeInput item : consumed)
			{
				if (item != null)
					if (item.item != null && item.item.stackSize > 0)
						this.decrStackSize(item.slotNumber, item.item.stackSize);
					else if (item.fluid != null)
						this.drainInputFluid(item);
			}
			return true;
		}
		return false;
	}

	protected IPoweredTask createTask(IMachineRecipe nextRecipe, float chance)
	{
		return new PoweredTask(nextRecipe, chance, this.getRecipeInputs());
	}

	protected IPoweredTask createTask(NBTTagCompound taskTagCompound)
	{
		return PoweredTask.readFromNBT(taskTagCompound);
	}

	@Override
	public void readCustomNBT(NBTTagCompound nbtRoot)
	{
		super.readCustomNBT(nbtRoot);
		this.currentTask = nbtRoot.hasKey("currentTask") ? this.createTask(nbtRoot.getCompoundTag("currentTask")) : null;
		String uid = nbtRoot.getString("lastCompletedRecipe");
		this.lastCompletedRecipe = MachineRecipeRegistry.instance.getRecipeForUid(uid);
	}

	@Override
	public void writeCustomNBT(NBTTagCompound nbtRoot)
	{
		super.writeCustomNBT(nbtRoot);
		if (this.currentTask != null)
		{
			NBTTagCompound currentTaskNBT = new NBTTagCompound();
			this.currentTask.writeToNBT(currentTaskNBT);
			nbtRoot.setTag("currentTask", currentTaskNBT);
		}
		if (this.lastCompletedRecipe != null)
			nbtRoot.setString("lastCompletedRecipe", this.lastCompletedRecipe.getUid());
	}

	@Override
	public void readCommon(NBTTagCompound nbtRoot)
	{
		super.readCommon(nbtRoot);
		this.cachedNextRecipe = null;
	}

	@Override
	public ItemStack decrStackSize(int fromSlot, int amount)
	{
		ItemStack res = super.decrStackSize(fromSlot, amount);
		if (this.slotDefinition.isInputSlot(fromSlot))
			this.cachedNextRecipe = null;
		return res;
	}

	@Override
	public void setInventorySlotContents(int slot, ItemStack contents)
	{
		super.setInventorySlotContents(slot, contents);
		if (this.slotDefinition.isInputSlot(slot))
			this.cachedNextRecipe = null;
	}

}
