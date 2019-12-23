package com.brandon3055.draconicevolution.common.tileentities.gates;

import com.brandon3055.brandonscore.common.utills.Utills;
import com.brandon3055.draconicevolution.common.lib.References;
import com.brandon3055.draconicevolution.common.utills.LogHelper;
import com.gamerforea.draconicevolution.util.SafeRecursiveExecutor;
import com.google.common.base.Objects;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

/**
 * Created by Brandon on 29/6/2015.
 */
public class TileFluidGate extends TileGate implements IFluidHandler
{
	@Override
	public int fill(ForgeDirection from, FluidStack resource, boolean doFill)
	{
		IFluidHandler target = this.getOutputTarget();
		if (target == null)
			return 0;

		/* TODO gamerforEA code replace, old code:
		int transfer = Math.min(this.getActualFlow(), target.fill(from, resource, false));
		if (transfer < resource.amount)
		{
			FluidStack newStack = resource.copy();
			newStack.amount = transfer;
			resource.amount -= transfer;
			return target.fill(from, newStack, doFill);
		}
		return target.fill(from, resource, doFill); */
		return Objects.firstNonNull(SafeRecursiveExecutor.INSTANCE.execute(target, t -> {
			int transfer = Math.min(this.getActualFlow(), t.fill(from, resource, false));
			if (transfer < resource.amount)
			{
				FluidStack newStack = resource.copy();
				newStack.amount = transfer;
				resource.amount -= transfer;
				return t.fill(from, newStack, doFill);
			}
			return t.fill(from, resource, doFill);
		}), 0);
		// TODO gamerforEA code end
	}

	@Override
	public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain)
	{
		return null;
	}

	@Override
	public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain)
	{
		return null;
	}

	@Override
	public boolean canFill(ForgeDirection from, Fluid fluid)
	{
		IFluidHandler target = this.getOutputTarget();

		/* TODO gamerforEA code replace, old code:
		LogHelper.info(target != null && target.canFill(from, fluid));
		return target != null && target.canFill(from, fluid); */
		if (target == null)
		{
			LogHelper.info(false);
			return false;
		}

		return Objects.firstNonNull(SafeRecursiveExecutor.INSTANCE.execute(target, t -> {
			boolean result = t != null && target.canFill(from, fluid);
			LogHelper.info(result);
			return result;
		}), false);
		// TODO gamerforEA code end
	}

	@Override
	public boolean canDrain(ForgeDirection from, Fluid fluid)
	{
		return false;
	}

	@Override
	public FluidTankInfo[] getTankInfo(ForgeDirection from)
	{
		return new FluidTankInfo[0];
	}

	private IFluidHandler getOutputTarget()
	{
		// TODO gamerforEA code start
		if (this.output == null || this.output == ForgeDirection.UNKNOWN)
			return null;
		// TODO gamerforEA code end

		TileEntity tile = this.worldObj.getTileEntity(this.xCoord + this.output.offsetX, this.yCoord + this.output.offsetY, this.zCoord + this.output.offsetZ);
		return tile instanceof IFluidHandler ? (IFluidHandler) tile : null;
	}

	@Override
	public String getFlowSetting(int selector)
	{
		return selector == 0 ? Utills.addCommas(this.flowRSLow) + " MB/t" : Utills.addCommas(this.flowRSHigh) + " MB/t";
	}

	@Override
	public void incrementFlow(int selector, boolean ctrl, boolean shift, boolean add, int button)
	{
		int amount = button == 0 ? shift ? ctrl ? 1000 : 100 : ctrl ? 50 : 5 : shift ? ctrl ? 100 : 50 : ctrl ? 10 : 1;
		if (selector == 0)
		{
			this.flowRSLow += add ? amount : -amount;
			if (this.flowRSLow < 0)
				this.flowRSLow = 0;
			if (this.worldObj.isRemote)
				this.sendObjectToServer(References.INT_ID, 0, this.flowRSLow);
		}
		else
		{
			this.flowRSHigh += add ? amount : -amount;
			if (this.flowRSHigh < 0)
				this.flowRSHigh = 0;
			if (this.worldObj.isRemote)
				this.sendObjectToServer(References.INT_ID, 1, this.flowRSHigh);
		}
	}

	@Override
	public String getToolTip(int selector, boolean shift, boolean ctrl)
	{
		int b1 = shift ? ctrl ? 1000 : 100 : ctrl ? 50 : 5;
		int b2 = shift ? ctrl ? 100 : 50 : ctrl ? 10 : 1;
		return b1 + "/" + b2 + " MB/t";
	}

	@Override
	public String getName()
	{
		return "fluid_gate";
	}

}
