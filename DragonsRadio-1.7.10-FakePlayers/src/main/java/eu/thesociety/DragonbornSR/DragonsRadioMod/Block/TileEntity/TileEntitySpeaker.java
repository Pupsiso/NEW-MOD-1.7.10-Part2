package eu.thesociety.DragonbornSR.DragonsRadioMod.Block.TileEntity;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

public class TileEntitySpeaker extends TileEntity
{
	// TODO gamerforEA code start
	@Override
	public boolean canUpdate()
	{
		return false;
	}
	// TODO gamerforEA code end

	@Override
	public void readFromNBT(NBTTagCompound nbt)
	{
		super.readFromNBT(nbt);
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt)
	{
		super.writeToNBT(nbt);
	}
}
