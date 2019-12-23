package crazypants.enderio.machine.recipe;

import com.gamerforea.enderio.EventConfig;
import crazypants.enderio.machine.MachineRecipeInput;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Recipe implements IRecipe
{
	private final RecipeInput[] inputs;
	private final RecipeOutput[] outputs;
	private final int energyRequired;
	private final RecipeBonusType bonusType;

	public Recipe(RecipeOutput output, int energyRequired, RecipeBonusType bonusType, RecipeInput... input)
	{
		this(input, new RecipeOutput[] { output }, energyRequired, bonusType);
	}

	public Recipe(RecipeInput input, int energyRequired, RecipeBonusType bonusType, RecipeOutput... output)
	{
		this(new RecipeInput[] { input }, output, energyRequired, bonusType);
	}

	public Recipe(RecipeInput[] input, RecipeOutput[] output, int energyRequired, RecipeBonusType bonusType)
	{
		this.inputs = input;
		this.outputs = output;
		this.energyRequired = energyRequired;
		this.bonusType = bonusType;
	}

	@Override
	public boolean isInputForRecipe(MachineRecipeInput... machineInputs)
	{
		// TODO gamerforEA code start
		if (FAST_CHECK_ENABLED)
			return this.isInputForRecipeFast(machineInputs);
		// TODO gamerforEA code end

		if (machineInputs == null || machineInputs.length == 0)
			return false;

		List<RecipeInput> requiredInputs = new ArrayList<RecipeInput>();
		for (RecipeInput input : this.inputs)
		{
			if (input.getFluidInput() != null || input.getInput() != null)
				requiredInputs.add(input.copy());
		}

		for (MachineRecipeInput input : machineInputs)
		{
			if (input != null && (input.fluid != null || input.item != null))
			{
				RecipeInput required = null;

				for (int i = 0; i < requiredInputs.size() && required == null; i++)
				{
					RecipeInput tst = requiredInputs.get(i);
					if (tst.isInput(input.item) && tst.getInput().stackSize > 0 || tst.isInput(input.fluid))
						required = tst;
				}

				if (required == null)
					return false;

				//reduce the required input quantity by the available amount
				if (input.isFluid())
					required.getFluidInput().amount -= input.fluid.amount;
				else
					required.getInput().stackSize -= input.item.stackSize;
			}
		}

		for (RecipeInput required : requiredInputs)
		{
			/* TODO gamerforEA code replace, old code:
			if (required.isFluid() && required.getFluidInput().amount > 0)
				return false;
			else if (!required.isFluid() && required.getInput().stackSize > 0)
				return false; */
			if (required.isFluid())
			{
				if (required.getFluidInput().amount > 0)
					return false;
			}
			else if (required.getInput().stackSize > 0)
				return false;
			// TODO gamerforEA code end
		}

		return true;
	}

	// TODO gamerforEA code start
	private static final boolean FAST_CHECK_ENABLED = EventConfig.fastRecipeCheck;

	private boolean isInputForRecipeFast(MachineRecipeInput... machineInputs)
	{
		if (machineInputs == null || machineInputs.length == 0)
			return false;

		for (RecipeInput required : this.inputs)
		{
			FluidStack requiredFluid = required.getFluidInput();
			ItemStack requiredItem = required.getInput();

			int requiredFluidAmount = requiredFluid == null ? 0 : Math.max(0, requiredFluid.amount);
			int requiredItemAmount = requiredItem == null ? 0 : Math.max(0, requiredItem.stackSize);
			if (requiredFluidAmount + requiredItemAmount <= 0)
				continue;

			for (MachineRecipeInput input : machineInputs)
			{
				if (input == null)
					continue;

				if (requiredFluidAmount > 0 && required.isInput(input.fluid))
					requiredFluidAmount -= Math.min(requiredFluidAmount, input.fluid.amount);
				if (requiredItemAmount > 0 && required.isInput(input.item))
					requiredItemAmount -= Math.min(requiredItemAmount, input.item.stackSize);
			}

			if (requiredFluidAmount + requiredItemAmount > 0)
				return false;
		}

		return true;
	}
	// TODO gamerforEA code end

	protected int getMinNumInputs()
	{
		return this.inputs.length;
	}

	@Override
	public boolean isValidInput(int slot, ItemStack item)
	{
		return this.getInputForStack(item) != null;
	}

	@Override
	public boolean isValidInput(FluidStack fluid)
	{
		return this.getInputForStack(fluid) != null;
	}

	private RecipeInput getInputForStack(FluidStack input)
	{
		for (RecipeInput ri : this.inputs)
		{
			if (ri.isInput(input))
				return ri;
		}

		return null;
	}

	private RecipeInput getInputForStack(ItemStack input)
	{
		for (RecipeInput ri : this.inputs)
		{
			if (ri.isInput(input))
				return ri;
		}

		return null;
	}

	@Override
	public List<ItemStack> getInputStacks()
	{
		if (this.inputs == null)
			return Collections.emptyList();

		List<ItemStack> res = new ArrayList<ItemStack>(this.inputs.length);
		for (RecipeInput in : this.inputs)
		{
			if (in != null && in.getInput() != null)
				res.add(in.getInput());
		}

		return res;
	}

	@Override
	public List<FluidStack> getInputFluidStacks()
	{
		if (this.inputs == null)
			return Collections.emptyList();

		List<FluidStack> res = new ArrayList<FluidStack>(this.inputs.length);
		for (RecipeInput in : this.inputs)
		{
			if (in != null && in.getFluidInput() != null)
				res.add(in.getFluidInput());
		}

		return res;
	}

	@Override
	public RecipeInput[] getInputs()
	{
		return this.inputs;
	}

	@Override
	public RecipeOutput[] getOutputs()
	{
		return this.outputs;
	}

	@Override
	public RecipeBonusType getBonusType()
	{
		return this.bonusType;
	}

	public boolean hasOuput(ItemStack result)
	{
		if (result == null)
			return false;

		for (RecipeOutput output : this.outputs)
		{
			ItemStack os = output.getOutput();
			if (os != null && os.isItemEqual(result))
				return true;
		}

		return false;
	}

	@Override
	public int getEnergyRequired()
	{
		return this.energyRequired;
	}

	@Override
	public boolean isValid()
	{
		return this.inputs != null && this.outputs != null && this.energyRequired > 0;
	}

	@Override
	public String toString()
	{
		return "Recipe [input=" + Arrays.toString(this.inputs) + ", output=" + Arrays.toString(this.outputs) + ", energyRequired=" + this.energyRequired + "]";
	}

}
