package com.brandon3055.draconicevolution.common.tileentities.gates;

import cofh.api.energy.IEnergyReceiver;
import com.brandon3055.brandonscore.common.utills.Utills;
import com.brandon3055.draconicevolution.common.lib.References;
import com.gamerforea.draconicevolution.util.SafeRecursiveExecutor;
import com.google.common.base.Objects;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * Created by Brandon on 29/6/2015.
 */
public class TileFluxGate extends TileGate implements IEnergyReceiver
{
	private int transferredThisTick = 0;

	@Override
	public void updateEntity()
	{
		super.updateEntity();

		this.transferredThisTick = 0;
	}

	@Override
	public int receiveEnergy(ForgeDirection from, int maxReceive, boolean simulate)
	{
		IEnergyReceiver target = this.getOutputTarget();
		//		int i = buffer.receiveEnergy(Math.max(0, Math.min(maxReceive, Math.min(getActualFlow(), getActualFlow() - buffer.getEnergyStored()))), simulate);
		//		return i;
		if (target == null)
			return 0;

		// TODO gamerforEA code replace, old code:
		// int transfer = target.receiveEnergy(from, Math.min(Math.max(0, this.getActualFlow() - this.transferredThisTick), target.receiveEnergy(from, maxReceive, true)), simulate);
		Integer transfer = SafeRecursiveExecutor.INSTANCE.execute(target, t -> t.receiveEnergy(from, Math.min(Math.max(0, this.getActualFlow() - this.transferredThisTick), t.receiveEnergy(from, maxReceive, true)), simulate));
		if (transfer == null)
			return 0;
		// TODO gamerforEA code end

		this.transferredThisTick += transfer;
		return transfer;
	}

	//	private EnergyStorage buffer = new EnergyStorage(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
	//
	//	@Override
	//	public void updateEntity() {
	//		super.updateEntity();
	//
	//		IEnergyReceiver receiver = getOutputTarget();
	//		if (receiver != null && !worldObj.isRemote){
	//			buffer.extractEnergy(receiver.receiveEnergy(output.getOpposite(), getActualFlow(), false), false);
	//		}
	//	}
	//
	//	@Override
	//	public int receiveEnergy(ForgeDirection from, int maxReceive, boolean simulate) {
	//		if (buffer.getEnergyStored() > 0) return 0;
	//		int i = buffer.receiveEnergy(Math.max(0, Math.min(maxReceive, Math.min(getActualFlow(), getActualFlow() - buffer.getEnergyStored()))), simulate);
	//		return i;
	//	}

	@Override
	public int getEnergyStored(ForgeDirection from)
	{
		IEnergyReceiver target = this.getOutputTarget();
		if (target == null)
			return 0;

		// TODO gamerforEA code replace, old code:
		// return target.getEnergyStored(from);
		return Objects.firstNonNull(SafeRecursiveExecutor.INSTANCE.execute(target, t -> t.getEnergyStored(from)), 0);
		// TODO gamerforEA code end
	}

	@Override
	public int getMaxEnergyStored(ForgeDirection from)
	{
		IEnergyReceiver target = this.getOutputTarget();
		if (target == null)
			return 0;

		// TODO gamerforEA code replace, old code:
		// return target.getMaxEnergyStored(from);
		return Objects.firstNonNull(SafeRecursiveExecutor.INSTANCE.execute(target, t -> t.getMaxEnergyStored(from)), 0);
		// TODO gamerforEA code end
	}

	@Override
	public boolean canConnectEnergy(ForgeDirection from)
	{
		return from == this.output || from == this.output.getOpposite();
	}

	private IEnergyReceiver getOutputTarget()
	{
		// TODO gamerforEA code start
		if (this.output == null || this.output == ForgeDirection.UNKNOWN)
			return null;
		// TODO gamerforEA code end

		TileEntity tile = this.worldObj.getTileEntity(this.xCoord + this.output.offsetX, this.yCoord + this.output.offsetY, this.zCoord + this.output.offsetZ);
		return tile instanceof IEnergyReceiver ? (IEnergyReceiver) tile : null;
	}

	@Override
	public String getFlowSetting(int selector)
	{
		return selector == 0 ? Utills.addCommas(this.flowRSLow) + " RF/t" : Utills.addCommas(this.flowRSHigh) + " RF/t";
	}

	@Override
	public void incrementFlow(int selector, boolean ctrl, boolean shift, boolean add, int button)
	{
		int amount = button == 0 ? shift ? ctrl ? 10000 : 1000 : ctrl ? 5 : 50 : shift ? ctrl ? 1000 : 100 : ctrl ? 1 : 10;

		if (selector == 0)
		{
			if (ctrl && shift && button == 0)
				amount += this.flowRSLow / 100000 * 1000;
			this.flowRSLow += add ? amount : -amount;
			if (this.flowRSLow < 0)
				this.flowRSLow = 0;
			if (this.worldObj.isRemote)
				this.sendObjectToServer(References.INT_ID, 0, this.flowRSLow);
		}
		else
		{
			if (ctrl && shift && button == 0)
				amount += this.flowRSHigh / 100000 * 1000;
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
		int i = selector == 0 ? this.flowRSLow / 100000 * 10000 : this.flowRSHigh / 100000 * 10000;
		int b1 = shift ? ctrl ? 10000 + i : 1000 : ctrl ? 5 : 50;
		int b2 = shift ? ctrl ? 1000 : 100 : ctrl ? 1 : 10;
		return b1 + "/" + b2 + " RF/t";
	}

	@Override
	public String getName()
	{
		return "flux_gate";
	}
}
