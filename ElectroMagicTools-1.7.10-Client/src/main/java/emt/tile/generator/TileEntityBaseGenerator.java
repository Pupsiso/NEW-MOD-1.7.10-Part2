package emt.tile.generator;

import cpw.mods.fml.common.network.NetworkRegistry;
import emt.tile.TileEntityEMT;
import ic2.api.energy.prefab.BasicSource;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IAspectSource;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.fx.PacketFXEssentiaSource;

import java.util.Random;

public abstract class TileEntityBaseGenerator extends TileEntityEMT
{

	public BasicSource energySource = new BasicSource(this, 1000000000, 3);
	public Aspect aspect;
	public double output;
	public int tick = 0;
	public boolean isActive = false;
	Random rnd = new Random();

	public TileEntityBaseGenerator()
	{
		this.output = 2000;
	}

	@Override
	public void updateEntity()
	{
		if (this.tick > 0)
			this.tick--;

		if (this.tick == 0)
		{
			this.energySource.updateEntity();
			this.isActive = false;
			this.createEnergy();
			this.tick = 20;
		}
	}

	public void createEnergy()
	{
		// TODO gamerforEA code start
		if (this.worldObj.isRemote)
			return;
		// TODO gamerforEA code end

		for (int x = this.xCoord - 4; x < this.xCoord + 4; x++)
		{
			for (int y = this.yCoord - 4; y < this.yCoord + 4; y++)
			{
				for (int z = this.zCoord - 4; z < this.zCoord + 4; z++)
				{
					TileEntity tile = this.worldObj.getTileEntity(x, y, z);
					if (tile instanceof IAspectSource)
					{
						IAspectSource as = (IAspectSource) tile;
						if (as.doesContainerContainAmount(this.aspect, 1))
						{
							if (as.takeFromContainer(this.aspect, 1))
							{
								this.isActive = true;
								PacketHandler.INSTANCE.sendToAllAround(new PacketFXEssentiaSource(this.xCoord, this.yCoord, this.zCoord, (byte) (this.xCoord - x), (byte) (this.yCoord - y), (byte) (this.zCoord - z), this.aspect.getColor()), new NetworkRegistry.TargetPoint(this.getWorldObj().provider.dimensionId, this.xCoord, this.yCoord, this.zCoord, 32.0D));
								this.energySource.addEnergy(this.output / 30D);
							}
						}
					}
				}
			}
		}
	}

	@Override
	public void onChunkUnload()
	{
		this.energySource.onChunkUnload();
	}

	@Override
	public void invalidate()
	{
		this.energySource.invalidate();
		super.invalidate();
	}

	@Override
	public void readFromNBT(NBTTagCompound tag)
	{
		super.readFromNBT(tag);
		this.energySource.readFromNBT(tag);
	}

	@Override
	public void writeToNBT(NBTTagCompound tag)
	{
		super.writeToNBT(tag);
		this.energySource.writeToNBT(tag);
	}
}
