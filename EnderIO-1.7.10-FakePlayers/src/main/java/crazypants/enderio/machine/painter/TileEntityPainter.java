package crazypants.enderio.machine.painter;

import crazypants.enderio.ModObject;
import crazypants.enderio.machine.*;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;

import java.util.List;
import java.util.Map;

public class TileEntityPainter extends AbstractPoweredTaskEntity implements ISidedInventory
{

	//  private static final short MAX_POWER_USE_PER_TICK = 6;

	public TileEntityPainter()
	{
		// 0 = input slot, 1 = paint source, 2 = output slot
		super(new SlotDefinition(2, 1));
	}

	@Override
	public boolean canExtractItem(int i, ItemStack itemstack, int j)
	{
		return super.canExtractItem(i, itemstack, j) && PainterUtil.isMetadataEquivelent(itemstack, this.inventory[2]);
	}

	@Override
	public String getInventoryName()
	{
		return "Auto Painter";
	}

	@Override
	public boolean hasCustomInventoryName()
	{
		return false;
	}

	@Override
	public boolean isMachineItemValidForSlot(int i, ItemStack itemStack)
	{
		if (i > 1)
			return false;
		if (i == 0)
		{
			List<IMachineRecipe> recipes = MachineRecipeRegistry.instance.getRecipesForInput(this.getMachineName(), MachineRecipeInput.create(i, itemStack));
			if (this.inventory[1] == null)
				return !recipes.isEmpty();
			else
				for (IMachineRecipe rec : recipes)
				{
					if (rec instanceof BasicPainterTemplate)
					{
						BasicPainterTemplate temp = (BasicPainterTemplate) rec;
						if (temp.isValidPaintSource(this.inventory[1]))
							return true;
					}
				}
			return false;
		}
		if (this.inventory[0] == null)
		{
			Map<String, IMachineRecipe> recipes = MachineRecipeRegistry.instance.getRecipesForMachine(this.getMachineName());
			for (IMachineRecipe rec : recipes.values())
			{
				if (rec instanceof BasicPainterTemplate)
				{
					BasicPainterTemplate temp = (BasicPainterTemplate) rec;
					if (temp.isValidPaintSource(itemStack))
						return true;
				}
			}
			return PaintSourceValidator.instance.isValidSourceDefault(itemStack);
		}
		return MachineRecipeRegistry.instance.getRecipeForInputs(this.getMachineName(), i == 0 ? MachineRecipeInput.create(0, itemStack) : this.targetInput(), i == 1 ? MachineRecipeInput.create(1, itemStack) : this.paintSource()) != null;
	}

	@Override
	public String getMachineName()
	{
		return ModObject.blockPainter.unlocalisedName;
	}

	private MachineRecipeInput targetInput()
	{
		return MachineRecipeInput.create(0, this.inventory[0]);
	}

	private MachineRecipeInput paintSource()
	{
		return MachineRecipeInput.create(1, this.inventory[1]);
	}

	@Override
	protected int getNumCanMerge(ItemStack stack, ItemStack result)
	{
		if (result == null || !result.isItemEqual(this.inventory[2]))
			// next result is a different item type
			return 0;
		else if (result.hasTagCompound() && this.inventory[2].hasTagCompound())
		{
			int cookedId = result.getTagCompound().getInteger(BlockPainter.KEY_SOURCE_BLOCK_ID);
			int invId = this.inventory[2].getTagCompound().getInteger(BlockPainter.KEY_SOURCE_BLOCK_ID);
			if (cookedId != invId)
				// next result has a different source item than the current one
				return 0;
		}

		// TODO gamerforEA code start
		if (!ItemStack.areItemStackTagsEqual(result, this.inventory[2]))
			return 0;
		// TODO gamerforEA code end

		return Math.min(stack.getMaxStackSize() - stack.stackSize, result.stackSize);
	}

}
