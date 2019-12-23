package crazypants.enderio.machine.recipe;

import com.gamerforea.enderio.util.FastOreDictionary;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;

public class OreDictionaryRecipeInput extends RecipeInput
{
	private int oreId;

	public OreDictionaryRecipeInput(ItemStack itemStack, int oreId, int slot)
	{
		this(itemStack, oreId, 1, slot);
	}

	public OreDictionaryRecipeInput(ItemStack stack, int oreId, float multiplier, int slot)
	{
		super(stack, true, multiplier, slot);
		this.oreId = oreId;
	}

	public OreDictionaryRecipeInput(OreDictionaryRecipeInput copy)
	{
		super(copy.getInput(), true, copy.getMulitplier(), copy.getSlotNumber());
		this.oreId = copy.oreId;
	}

	@Override
	public RecipeInput copy()
	{
		return new OreDictionaryRecipeInput(this);
	}

	@Override
	public boolean isInput(ItemStack test)
	{
		if (test == null || this.oreId < 0)
			return false;

		try
		{
			/* TODO gamerforEA code replace, old code:
			int[] ids = OreDictionary.getOreIDs(test);
			if (ids == null)
				return false; */
			int[] ids = FastOreDictionary.getOreIDs(test);
			// TODO gamerforEA code end

			for (int id : ids)
			{
				if (id == this.oreId)
					return true;
			}

			return false;
		}
		catch (Exception e)
		{
			return false;
		}
	}

	@Override
	public ItemStack[] getEquivelentInputs()
	{
		ArrayList<ItemStack> res = OreDictionary.getOres(this.oreId);
		if (res == null || res.isEmpty())
			return null;

		/* TODO gamerforEA code replace, old code:
		ItemStack[] res2 = res.toArray(new ItemStack[0]);
		for (int i = 0; i < res.size(); ++i)
		{
			res2[i] = res2[i].copy();
			res2[i].stackSize = this.getInput().stackSize;
		} */
		int size = res.size();
		int stackSize = this.getInput().stackSize;

		ItemStack[] res2 = new ItemStack[size];
		for (int i = 0; i < size; ++i)
		{
			ItemStack stack = res.get(i).copy();
			stack.stackSize = stackSize;
			res2[i] = stack;
		}
		// TODO gamerforEA code end

		return res2;
	}

	@Override
	public String toString()
	{
		return "OreDictionaryRecipeInput [oreId=" + this.oreId + " name=" + OreDictionary.getOreName(this.oreId) + " amount=" + this.getInput().stackSize + "]";
	}

}
