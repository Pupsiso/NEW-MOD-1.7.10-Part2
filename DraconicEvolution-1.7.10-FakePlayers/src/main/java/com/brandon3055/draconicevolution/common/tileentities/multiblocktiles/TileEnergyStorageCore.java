package com.brandon3055.draconicevolution.common.tileentities.multiblocktiles;

import com.brandon3055.draconicevolution.common.ModBlocks;
import com.brandon3055.draconicevolution.common.blocks.multiblock.MultiblockHelper.TileLocation;
import com.brandon3055.draconicevolution.common.handler.BalanceConfigHandler;
import com.brandon3055.draconicevolution.common.lib.References;
import com.brandon3055.draconicevolution.common.tileentities.TileObjectSync;
import com.brandon3055.draconicevolution.common.tileentities.TileParticleGenerator;
import com.brandon3055.draconicevolution.common.utills.LogHelper;
import com.gamerforea.draconicevolution.ModUtils;
import com.gamerforea.eventhelper.fake.FakePlayerContainer;
import cpw.mods.fml.common.network.NetworkRegistry;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.util.AxisAlignedBB;

/**
 * Created by Brandon on 25/07/2014.
 */
public class TileEnergyStorageCore extends TileObjectSync
{

	protected TileLocation[] stabilizers = new TileLocation[4];
	protected int tier = 0;
	protected boolean online = false;
	public float modelRotation = 0;
	private long energy = 0;
	private long capacity = 0;
	private long lastTickCapacity = 0;
	private int tick = 0;

	// TODO gamerforEA code start
	public final FakePlayerContainer fake = ModUtils.NEXUS_FACTORY.wrapFake(this);
	// TODO gamerforEA code end

	public TileEnergyStorageCore()
	{
		for (int i = 0; i < this.stabilizers.length; i++)
		{
			this.stabilizers[i] = new TileLocation();
		}
	}

	@Override
	public void updateEntity()
	{
		//energy = 200000000;
		if (!this.online)
			return;
		if (this.worldObj.isRemote)
			this.modelRotation += 0.5;
		if (!this.worldObj.isRemote)
			this.detectAndRendChanges();
		this.tick++;
	}

	/**
	 * ######################MultiBlock Methods#######################
	 */

	public boolean tryActivate()
	{
		if (!this.findStabalyzers())
			return false;
		if (!this.setTier(false))
			return false;
		if (!this.testOrActivateStructureIfValid(false, false))
			return false;
		this.online = true;
		if (!this.testOrActivateStructureIfValid(false, true))
		{
			this.online = false;
			this.deactivateStabilizers();
			return false;
		}
		this.activateStabilizers();
		this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
		return true;
	}

	public boolean creativeActivate()
	{
		if (!this.findStabalyzers())
			return false;
		if (!this.setTier(false))
			return false;
		if (!this.testOrActivateStructureIfValid(true, false))
			return false;
		this.online = true;
		if (!this.testOrActivateStructureIfValid(false, true))
		{
			this.online = false;
			this.deactivateStabilizers();
			return false;
		}
		this.activateStabilizers();
		this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
		return true;
	}

	public boolean isStructureStillValid(boolean update)
	{
		if (!this.checkStabilizers())
			this.online = false;
		if (!this.testOrActivateStructureIfValid(false, false))
			this.online = false;
		if (!this.areStabilizersActive())
			this.online = false;
		this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
		if (!this.online)
			this.deactivateStabilizers();
		if (update && !this.online)
			this.reIntegrate();
		//if (update && !online && worldObj.getTileEntity(xCoord, yCoord + 1, zCoord) != null && worldObj.getTileEntity(xCoord, yCoord + 1, zCoord) instanceof TileInvisibleMultiblock) ((TileInvisibleMultiblock)worldObj.getTileEntity(xCoord, yCoord + 1, zCoord)).isStructureStillValid();
		return this.online;
	}

	private void reIntegrate()
	{
		// TODO gamerforEA code start
		for (int x = this.xCoord - 1; x <= this.xCoord + 1; x++)
		{
			for (int y = this.yCoord - 1; y <= this.yCoord + 1; y++)
			{
				for (int z = this.zCoord - 1; z <= this.zCoord + 1; z++)
				{
					int meta = this.worldObj.getBlockMetadata(x, y, z);
					if ((meta == 0 || meta == 1) && this.cantBreak(x, y, z))
						return;
				}
			}
		}
		// TODO gamerforEA code end

		for (int x = this.xCoord - 1; x <= this.xCoord + 1; x++)
		{
			for (int y = this.yCoord - 1; y <= this.yCoord + 1; y++)
			{
				for (int z = this.zCoord - 1; z <= this.zCoord + 1; z++)
				{
					if (this.worldObj.getBlock(x, y, z) == ModBlocks.invisibleMultiblock)
					{
						int meta = this.worldObj.getBlockMetadata(x, y, z);
						if (meta == 0)
							this.worldObj.setBlock(x, y, z, ModBlocks.draconiumBlock);
						else if (meta == 1)
							this.worldObj.setBlock(x, y, z, BalanceConfigHandler.energyStorageStructureBlock, BalanceConfigHandler.energyStorageStructureBlockMetadata, 3);
					}
				}
			}
		}
	}

	private boolean findStabalyzers()
	{
		boolean flag = true;
		for (int x = this.xCoord; x <= this.xCoord + 11; x++)
		{
			if (this.worldObj.getBlock(x, this.yCoord, this.zCoord) == ModBlocks.particleGenerator)
			{
				if (this.worldObj.getBlockMetadata(x, this.yCoord, this.zCoord) == 1)
				{
					flag = false;
					break;
				}
				this.stabilizers[0] = new TileLocation(x, this.yCoord, this.zCoord);
				break;
			}
			else if (x == this.xCoord + 11)
				flag = false;
		}
		for (int x = this.xCoord; x >= this.xCoord - 11; x--)
		{
			if (this.worldObj.getBlock(x, this.yCoord, this.zCoord) == ModBlocks.particleGenerator)
			{
				if (this.worldObj.getBlockMetadata(x, this.yCoord, this.zCoord) == 1)
				{
					flag = false;
					break;
				}
				this.stabilizers[1] = new TileLocation(x, this.yCoord, this.zCoord);
				break;
			}
			else if (x == this.xCoord - 11)
				flag = false;
		}
		for (int z = this.zCoord; z <= this.zCoord + 11; z++)
		{
			if (this.worldObj.getBlock(this.xCoord, this.yCoord, z) == ModBlocks.particleGenerator)
			{
				if (this.worldObj.getBlockMetadata(this.xCoord, this.yCoord, z) == 1)
				{
					flag = false;
					break;
				}
				this.stabilizers[2] = new TileLocation(this.xCoord, this.yCoord, z);
				break;
			}
			else if (z == this.zCoord + 11)
				flag = false;
		}
		for (int z = this.zCoord; z >= this.zCoord - 11; z--)
		{
			if (this.worldObj.getBlock(this.xCoord, this.yCoord, z) == ModBlocks.particleGenerator)
			{
				if (this.worldObj.getBlockMetadata(this.xCoord, this.yCoord, z) == 1)
				{
					flag = false;
					break;
				}
				this.stabilizers[3] = new TileLocation(this.xCoord, this.yCoord, z);
				break;
			}
			else if (z == this.zCoord - 11)
				flag = false;
		}
		return flag;
	}

	private boolean setTier(boolean force)
	{
		if (force)
			return true;
		int xPos = 0;
		int xNeg = 0;
		int yPos = 0;
		int yNeg = 0;
		int zPos = 0;
		int zNeg = 0;
		int range = 5;

		for (int x = 0; x <= range; x++)
		{
			if (this.testForOrActivateDraconium(this.xCoord + x, this.yCoord, this.zCoord, false, false))
			{
				xPos = x;
				break;
			}
		}

		for (int x = 0; x <= range; x++)
		{
			if (this.testForOrActivateDraconium(this.xCoord - x, this.yCoord, this.zCoord, false, false))
			{
				xNeg = x;
				break;
			}
		}

		for (int y = 0; y <= range; y++)
		{
			if (this.testForOrActivateDraconium(this.xCoord, this.yCoord + y, this.zCoord, false, false))
			{
				yPos = y;
				break;
			}
		}

		for (int y = 0; y <= range; y++)
		{
			if (this.testForOrActivateDraconium(this.xCoord, this.yCoord - y, this.zCoord, false, false))
			{
				yNeg = y;
				break;
			}
		}

		for (int z = 0; z <= range; z++)
		{
			if (this.testForOrActivateDraconium(this.xCoord, this.yCoord, this.zCoord + z, false, false))
			{
				zPos = z;
				break;
			}
		}

		for (int z = 0; z <= range; z++)
		{
			if (this.testForOrActivateDraconium(this.xCoord, this.yCoord, this.zCoord - z, false, false))
			{
				zNeg = z;
				break;
			}
		}

		if (zNeg != zPos || zNeg != yNeg || zNeg != yPos || zNeg != xNeg || zNeg != xPos)
			return false;

		this.tier = xPos;
		if (this.tier > 1)
			this.tier++;
		if (this.tier == 1)
			if (this.testForOrActivateDraconium(this.xCoord + 1, this.yCoord + 1, this.zCoord, false, false))
				this.tier = 2;
		return true;
	}

	private boolean testOrActivateStructureIfValid(boolean setBlocks, boolean activate)
	{
		switch (this.tier)
		{
			case 0:
				if (!this.testOrActivateRect(1, 1, 1, "air", setBlocks, activate))
					return false;
				break;
			case 1:
				if (!this.testForOrActivateDraconium(this.xCoord + 1, this.yCoord, this.zCoord, setBlocks, activate) || !this.testForOrActivateDraconium(this.xCoord - 1, this.yCoord, this.zCoord, setBlocks, activate) || !this.testForOrActivateDraconium(this.xCoord, this.yCoord + 1, this.zCoord, setBlocks, activate) || !this.testForOrActivateDraconium(this.xCoord, this.yCoord - 1, this.zCoord, setBlocks, activate) || !this.testForOrActivateDraconium(this.xCoord, this.yCoord, this.zCoord + 1, setBlocks, activate) || !this.testForOrActivateDraconium(this.xCoord, this.yCoord, this.zCoord - 1, setBlocks, activate))
					return false;
				if (!this.isReplacable(this.xCoord + 1, this.yCoord + 1, this.zCoord, setBlocks) || !this.isReplacable(this.xCoord, this.yCoord + 1, this.zCoord + 1, setBlocks) || !this.isReplacable(this.xCoord - 1, this.yCoord + 1, this.zCoord, setBlocks) || !this.isReplacable(this.xCoord, this.yCoord + 1, this.zCoord - 1, setBlocks) || !this.isReplacable(this.xCoord + 1, this.yCoord - 1, this.zCoord, setBlocks) || !this.isReplacable(this.xCoord, this.yCoord - 1, this.zCoord + 1, setBlocks) || !this.isReplacable(this.xCoord - 1, this.yCoord - 1, this.zCoord, setBlocks) || !this.isReplacable(this.xCoord, this.yCoord - 1, this.zCoord - 1, setBlocks) || !this.isReplacable(this.xCoord + 1, this.yCoord, this.zCoord + 1, setBlocks) || !this.isReplacable(this.xCoord - 1, this.yCoord, this.zCoord - 1, setBlocks) || !this.isReplacable(this.xCoord + 1, this.yCoord, this.zCoord - 1, setBlocks) || !this.isReplacable(this.xCoord - 1, this.yCoord, this.zCoord + 1, setBlocks))
					return false;
				if (!this.isReplacable(this.xCoord + 1, this.yCoord + 1, this.zCoord + 1, setBlocks) || !this.isReplacable(this.xCoord - 1, this.yCoord + 1, this.zCoord - 1, setBlocks) || !this.isReplacable(this.xCoord + 1, this.yCoord + 1, this.zCoord - 1, setBlocks) || !this.isReplacable(this.xCoord - 1, this.yCoord + 1, this.zCoord + 1, setBlocks) || !this.isReplacable(this.xCoord + 1, this.yCoord - 1, this.zCoord + 1, setBlocks) || !this.isReplacable(this.xCoord - 1, this.yCoord - 1, this.zCoord - 1, setBlocks) || !this.isReplacable(this.xCoord + 1, this.yCoord - 1, this.zCoord - 1, setBlocks) || !this.isReplacable(this.xCoord - 1, this.yCoord - 1, this.zCoord + 1, setBlocks))
					return false;
				break;
			case 2:
				if (!this.testOrActivateRect(1, 1, 1, "draconiumBlock", setBlocks, activate))
					return false;
				break;
			case 3:
				if (!this.testOrActivateSides(1, "draconiumBlock", setBlocks, activate))
					return false;
				if (!this.testOrActivateRect(1, 1, 1, "redstone", setBlocks, activate))
					return false;
				break;
			case 4:
				if (!this.testOrActivateSides(2, "draconiumBlock", setBlocks, activate))
					return false;
				if (!this.testOrActivateRect(2, 1, 1, "redstone", setBlocks, activate))
					return false;
				if (!this.testOrActivateRect(1, 2, 1, "redstone", setBlocks, activate))
					return false;
				if (!this.testOrActivateRect(1, 1, 2, "redstone", setBlocks, activate))
					return false;
				if (!this.testOrActivateRings(2, 2, "draconiumBlock", setBlocks, activate))
					return false;
				break;
			case 5:
				if (!this.testOrActivateSides(3, "draconiumBlock", setBlocks, activate))
					return false;
				if (!this.testOrActivateSides(2, "redstone", setBlocks, activate))
					return false;
				if (!this.testOrActivateRect(2, 2, 2, "redstone", setBlocks, activate))
					return false;
				if (!this.testOrActivateRings(2, 3, "draconiumBlock", setBlocks, activate))
					return false;
				break;
			case 6:
				if (!this.testOrActivateSides(4, "draconiumBlock", setBlocks, activate))
					return false;
				if (!this.testOrActivateSides(3, "redstone", setBlocks, activate))
					return false;
				if (!this.testOrActivateRect(3, 2, 2, "redstone", setBlocks, activate))
					return false;
				if (!this.testOrActivateRect(2, 3, 2, "redstone", setBlocks, activate))
					return false;
				if (!this.testOrActivateRect(2, 2, 3, "redstone", setBlocks, activate))
					return false;
				if (!this.testOrActivateRings(2, 4, "draconiumBlock", setBlocks, activate))
					return false;
				if (!this.testOrActivateRings(3, 3, "draconiumBlock", setBlocks, activate))
					return false;
				break;
		}
		return true;
	}

	private boolean testOrActivateRect(int xDim, int yDim, int zDim, String block, boolean set, boolean activate)
	{
		for (int x = this.xCoord - xDim; x <= this.xCoord + xDim; x++)
		{
			for (int y = this.yCoord - yDim; y <= this.yCoord + yDim; y++)
			{
				for (int z = this.zCoord - zDim; z <= this.zCoord + zDim; z++)
				{
					if (block.equals("air"))
					{
						if (!(x == this.xCoord && y == this.yCoord && z == this.zCoord) && !this.isReplacable(x, y, z, set))
							return false;
					}
					else if (block.equals("redstone"))
					{
						if (!(x == this.xCoord && y == this.yCoord && z == this.zCoord) && !this.testForOrActivateRedstone(x, y, z, set, activate))
							return false;
					}
					else if (block.equals("draconiumBlock"))
					{
						if (!(x == this.xCoord && y == this.yCoord && z == this.zCoord) && !this.testForOrActivateDraconium(x, y, z, set, activate))
							return false;
					}
					else if (!block.equals("draconiumBlock") && !block.equals("redstone") && !block.equals("air"))
					{
						LogHelper.error("Invalid String In Multiblock Structure Code!!!");
						return false;
					}
				}
			}
		}
		return true;
	}

	private boolean testOrActivateRings(int size, int dist, String block, boolean set, boolean activate)
	{
		for (int y = this.yCoord - size; y <= this.yCoord + size; y++)
		{
			for (int z = this.zCoord - size; z <= this.zCoord + size; z++)
			{
				if (y == this.yCoord - size || y == this.yCoord + size || z == this.zCoord - size || z == this.zCoord + size)
					if (block.equals("air"))
					{
						if (!(this.xCoord + dist == this.xCoord && y == this.yCoord && z == this.zCoord) && !this.isReplacable(this.xCoord + dist, y, z, set))
							return false;
					}
					else if (block.equals("redstone"))
					{
						if (!(this.xCoord + dist == this.xCoord && y == this.yCoord && z == this.zCoord) && !this.testForOrActivateRedstone(this.xCoord + dist, y, z, set, activate))
							return false;
					}
					else if (block.equals("draconiumBlock"))
					{
						if (!(this.xCoord + dist == this.xCoord && y == this.yCoord && z == this.zCoord) && !this.testForOrActivateDraconium(this.xCoord + dist, y, z, set, activate))
							return false;
					}
					else if (!block.equals("draconiumBlock") && !block.equals("redstone") && !block.equals("air"))
					{
						LogHelper.error("Invalid String In Multiblock Structure Code!!!");
						return false;
					}
			}
		}
		for (int y = this.yCoord - size; y <= this.yCoord + size; y++)
		{
			for (int z = this.zCoord - size; z <= this.zCoord + size; z++)
			{
				if (y == this.yCoord - size || y == this.yCoord + size || z == this.zCoord - size || z == this.zCoord + size)
					if (block.equals("air"))
					{
						if (!(this.xCoord - dist == this.xCoord && y == this.yCoord && z == this.zCoord) && !this.isReplacable(this.xCoord - dist, y, z, set))
							return false;
					}
					else if (block.equals("redstone"))
					{
						if (!(this.xCoord - dist == this.xCoord && y == this.yCoord && z == this.zCoord) && !this.testForOrActivateRedstone(this.xCoord - dist, y, z, set, activate))
							return false;
					}
					else if (block.equals("draconiumBlock"))
					{
						if (!(this.xCoord - dist == this.xCoord && y == this.yCoord && z == this.zCoord) && !this.testForOrActivateDraconium(this.xCoord - dist, y, z, set, activate))
							return false;
					}
					else if (!block.equals("draconiumBlock") && !block.equals("redstone") && !block.equals("air"))
					{
						LogHelper.error("Invalid String In Multiblock Structure Code!!!");
						return false;
					}
			}
		}

		for (int x = this.xCoord - size; x <= this.xCoord + size; x++)
		{
			for (int z = this.zCoord - size; z <= this.zCoord + size; z++)
			{
				if (x == this.xCoord - size || x == this.xCoord + size || z == this.zCoord - size || z == this.zCoord + size)
					if (block.equals("air"))
					{
						if (!(x == this.xCoord && this.yCoord + dist == this.yCoord && z == this.zCoord) && !this.isReplacable(x, this.yCoord + dist, z, set))
							return false;
					}
					else if (block.equals("redstone"))
					{
						if (!(x == this.xCoord && this.yCoord + dist == this.yCoord && z == this.zCoord) && !this.testForOrActivateRedstone(x, this.yCoord + dist, z, set, activate))
							return false;
					}
					else if (block.equals("draconiumBlock"))
					{
						if (!(x == this.xCoord && this.yCoord + dist == this.yCoord && z == this.zCoord) && !this.testForOrActivateDraconium(x, this.yCoord + dist, z, set, activate))
							return false;
					}
					else if (!block.equals("draconiumBlock") && !block.equals("redstone") && !block.equals("air"))
					{
						LogHelper.error("Invalid String In Multiblock Structure Code!!!");
						return false;
					}
			}
		}
		for (int x = this.xCoord - size; x <= this.xCoord + size; x++)
		{
			for (int z = this.zCoord - size; z <= this.zCoord + size; z++)
			{
				if (x == this.xCoord - size || x == this.xCoord + size || z == this.zCoord - size || z == this.zCoord + size)
					if (block.equals("air"))
					{
						if (!(x == this.xCoord && this.yCoord - dist == this.yCoord && z == this.zCoord) && !this.isReplacable(x, this.yCoord - dist, z, set))
							return false;
					}
					else if (block.equals("redstone"))
					{
						if (!(x == this.xCoord && this.yCoord - dist == this.yCoord && z == this.zCoord) && !this.testForOrActivateRedstone(x, this.yCoord - dist, z, set, activate))
							return false;
					}
					else if (block.equals("draconiumBlock"))
					{
						if (!(x == this.xCoord && this.yCoord - dist == this.yCoord && z == this.zCoord) && !this.testForOrActivateDraconium(x, this.yCoord - dist, z, set, activate))
							return false;
					}
					else if (!block.equals("draconiumBlock") && !block.equals("redstone") && !block.equals("air"))
					{
						LogHelper.error("Invalid String In Multiblock Structure Code!!!");
						return false;
					}
			}
		}

		for (int y = this.yCoord - size; y <= this.yCoord + size; y++)
		{
			for (int x = this.xCoord - size; x <= this.xCoord + size; x++)
			{
				if (y == this.yCoord - size || y == this.yCoord + size || x == this.xCoord - size || x == this.xCoord + size)
					if (block.equals("air"))
					{
						if (!(x == this.xCoord && y == this.yCoord && this.zCoord + dist == this.zCoord) && !this.isReplacable(x, y, this.zCoord + dist, set))
							return false;
					}
					else if (block.equals("redstone"))
					{
						if (!(x == this.xCoord && y == this.yCoord && this.zCoord + dist == this.zCoord) && !this.testForOrActivateRedstone(x, y, this.zCoord + dist, set, activate))
							return false;
					}
					else if (block.equals("draconiumBlock"))
					{
						if (!(x == this.xCoord && y == this.yCoord && this.zCoord + dist == this.zCoord) && !this.testForOrActivateDraconium(x, y, this.zCoord + dist, set, activate))
							return false;
					}
					else if (!block.equals("draconiumBlock") && !block.equals("redstone") && !block.equals("air"))
					{
						LogHelper.error("Invalid String In Multiblock Structure Code!!!");
						return false;
					}
			}
		}
		for (int y = this.yCoord - size; y <= this.yCoord + size; y++)
		{
			for (int x = this.xCoord - size; x <= this.xCoord + size; x++)
			{
				if (y == this.yCoord - size || y == this.yCoord + size || x == this.xCoord - size || x == this.xCoord + size)
					if (block.equals("air"))
					{
						if (!(x == this.xCoord && y == this.yCoord && this.zCoord - dist == this.zCoord) && !this.isReplacable(x, y, this.zCoord - dist, set))
							return false;
					}
					else if (block.equals("redstone"))
					{
						if (!(x == this.xCoord && y == this.yCoord && this.zCoord - dist == this.zCoord) && !this.testForOrActivateRedstone(x, y, this.zCoord - dist, set, activate))
							return false;
					}
					else if (block.equals("draconiumBlock"))
					{
						if (!(x == this.xCoord && y == this.yCoord && this.zCoord - dist == this.zCoord) && !this.testForOrActivateDraconium(x, y, this.zCoord - dist, set, activate))
							return false;
					}
					else if (!block.equals("draconiumBlock") && !block.equals("redstone") && !block.equals("air"))
					{
						LogHelper.error("Invalid String In Multiblock Structure Code!!!");
						return false;
					}
			}
		}
		return true;
	}

	private boolean testOrActivateSides(int dist, String block, boolean set, boolean activate)
	{
		dist++;
		for (int y = this.yCoord - 1; y <= this.yCoord + 1; y++)
		{
			for (int z = this.zCoord - 1; z <= this.zCoord + 1; z++)
			{
				if (block.equals("air"))
				{
					if (!(this.xCoord + dist == this.xCoord && y == this.yCoord && z == this.zCoord) && !this.isReplacable(this.xCoord + dist, y, z, set))
						return false;
				}
				else if (block.equals("redstone"))
				{
					if (!(this.xCoord + dist == this.xCoord && y == this.yCoord && z == this.zCoord) && !this.testForOrActivateRedstone(this.xCoord + dist, y, z, set, activate))
						return false;
				}
				else if (block.equals("draconiumBlock"))
				{
					if (!(this.xCoord + dist == this.xCoord && y == this.yCoord && z == this.zCoord) && !this.testForOrActivateDraconium(this.xCoord + dist, y, z, set, activate))
						return false;
				}
				else if (!block.equals("draconiumBlock") && !block.equals("redstone") && !block.equals("air"))
				{
					LogHelper.error("Invalid String In Multiblock Structure Code!!!");
					return false;
				}
			}
		}
		for (int y = this.yCoord - 1; y <= this.yCoord + 1; y++)
		{
			for (int z = this.zCoord - 1; z <= this.zCoord + 1; z++)
			{
				if (block.equals("air"))
				{
					if (!(this.xCoord - dist == this.xCoord && y == this.yCoord && z == this.zCoord) && !this.isReplacable(this.xCoord - dist, y, z, set))
						return false;
				}
				else if (block.equals("redstone"))
				{
					if (!(this.xCoord - dist == this.xCoord && y == this.yCoord && z == this.zCoord) && !this.testForOrActivateRedstone(this.xCoord - dist, y, z, set, activate))
						return false;
				}
				else if (block.equals("draconiumBlock"))
				{
					if (!(this.xCoord - dist == this.xCoord && y == this.yCoord && z == this.zCoord) && !this.testForOrActivateDraconium(this.xCoord - dist, y, z, set, activate))
						return false;
				}
				else if (!block.equals("draconiumBlock") && !block.equals("redstone") && !block.equals("air"))
				{
					LogHelper.error("Invalid String In Multiblock Structure Code!!!");
					return false;
				}
			}
		}

		for (int x = this.xCoord - 1; x <= this.xCoord + 1; x++)
		{
			for (int z = this.zCoord - 1; z <= this.zCoord + 1; z++)
			{
				if (block.equals("air"))
				{
					if (!(x == this.xCoord && this.yCoord + dist == this.yCoord && z == this.zCoord) && !this.isReplacable(x, this.yCoord + dist, z, set))
						return false;
				}
				else if (block.equals("redstone"))
				{
					if (!(x == this.xCoord && this.yCoord + dist == this.yCoord && z == this.zCoord) && !this.testForOrActivateRedstone(x, this.yCoord + dist, z, set, activate))
						return false;
				}
				else if (block.equals("draconiumBlock"))
				{
					if (!(x == this.xCoord && this.yCoord + dist == this.yCoord && z == this.zCoord) && !this.testForOrActivateDraconium(x, this.yCoord + dist, z, set, activate))
						return false;
				}
				else if (!block.equals("draconiumBlock") && !block.equals("redstone") && !block.equals("air"))
				{
					LogHelper.error("Invalid String In Multiblock Structure Code!!!");
					return false;
				}
			}
		}
		for (int x = this.xCoord - 1; x <= this.xCoord + 1; x++)
		{
			for (int z = this.zCoord - 1; z <= this.zCoord + 1; z++)
			{
				if (block.equals("air"))
				{
					if (!(x == this.xCoord && this.yCoord - dist == this.yCoord && z == this.zCoord) && !this.isReplacable(x, this.yCoord - dist, z, set))
						return false;
				}
				else if (block.equals("redstone"))
				{
					if (!(x == this.xCoord && this.yCoord - dist == this.yCoord && z == this.zCoord) && !this.testForOrActivateRedstone(x, this.yCoord - dist, z, set, activate))
						return false;
				}
				else if (block.equals("draconiumBlock"))
				{
					if (!(x == this.xCoord && this.yCoord - dist == this.yCoord && z == this.zCoord) && !this.testForOrActivateDraconium(x, this.yCoord - dist, z, set, activate))
						return false;
				}
				else if (!block.equals("draconiumBlock") && !block.equals("redstone") && !block.equals("air"))
				{
					LogHelper.error("Invalid String In Multiblock Structure Code!!!");
					return false;
				}
			}
		}

		for (int y = this.yCoord - 1; y <= this.yCoord + 1; y++)
		{
			for (int x = this.xCoord - 1; x <= this.xCoord + 1; x++)
			{
				if (block.equals("air"))
				{
					if (!(x == this.xCoord && y == this.yCoord && this.zCoord + dist == this.zCoord) && !this.isReplacable(x, y, this.zCoord + dist, set))
						return false;
				}
				else if (block.equals("redstone"))
				{
					if (!(x == this.xCoord && y == this.yCoord && this.zCoord + dist == this.zCoord) && !this.testForOrActivateRedstone(x, y, this.zCoord + dist, set, activate))
						return false;
				}
				else if (block.equals("draconiumBlock"))
				{
					if (!(x == this.xCoord && y == this.yCoord && this.zCoord + dist == this.zCoord) && !this.testForOrActivateDraconium(x, y, this.zCoord + dist, set, activate))
						return false;
				}
				else if (!block.equals("draconiumBlock") && !block.equals("redstone") && !block.equals("air"))
				{
					LogHelper.error("Invalid String In Multiblock Structure Code!!!");
					return false;
				}
			}
		}
		for (int y = this.yCoord - 1; y <= this.yCoord + 1; y++)
		{
			for (int x = this.xCoord - 1; x <= this.xCoord + 1; x++)
			{
				if (block.equals("air"))
				{
					if (!(x == this.xCoord && y == this.yCoord && this.zCoord - dist == this.zCoord) && !this.isReplacable(x, y, this.zCoord - dist, set))
						return false;
				}
				else if (block.equals("redstone"))
				{
					if (!(x == this.xCoord && y == this.yCoord && this.zCoord - dist == this.zCoord) && !this.testForOrActivateRedstone(x, y, this.zCoord - dist, set, activate))
						return false;
				}
				else if (block.equals("draconiumBlock"))
				{
					if (!(x == this.xCoord && y == this.yCoord && this.zCoord - dist == this.zCoord) && !this.testForOrActivateDraconium(x, y, this.zCoord - dist, set, activate))
						return false;
				}
				else if (!block.equals("draconiumBlock") && !block.equals("redstone") && !block.equals("air"))
				{
					LogHelper.error("Invalid String In Multiblock Structure Code!!!");
					return false;
				}
			}
		}

		return true;
	}

	private boolean testForOrActivateDraconium(int x, int y, int z, boolean set, boolean activate)
	{
		if (!activate)
		{
			// TODO gamerforEA code start
			if (this.cantBreak(x, y, z))
				return false;
			// TODO gamerforEA code end

			if (set)
			{
				this.worldObj.setBlock(x, y, z, ModBlocks.draconiumBlock);
				return true;
			}
			else
				return this.worldObj.getBlock(x, y, z) == ModBlocks.draconiumBlock || this.worldObj.getBlock(x, y, z) == ModBlocks.invisibleMultiblock && this.worldObj.getBlockMetadata(x, y, z) == 0;
		}
		else
			return this.activateDraconium(x, y, z);
	}

	private boolean testForOrActivateRedstone(int x, int y, int z, boolean set, boolean activate)
	{
		if (!activate)
		{
			// TODO gamerforEA code start
			if (this.cantBreak(x, y, z))
				return false;
			// TODO gamerforEA code end

			if (set)
			{
				this.worldObj.setBlock(x, y, z, BalanceConfigHandler.energyStorageStructureBlock, BalanceConfigHandler.energyStorageStructureBlockMetadata, 3);
				return true;
			}
			else
				return this.worldObj.getBlock(x, y, z) == BalanceConfigHandler.energyStorageStructureBlock && this.worldObj.getBlockMetadata(x, y, z) == BalanceConfigHandler.energyStorageStructureBlockMetadata || this.worldObj.getBlock(x, y, z) == ModBlocks.invisibleMultiblock && this.worldObj.getBlockMetadata(x, y, z) == 1;
		}
		else
			return this.activateRedstone(x, y, z);
	}

	private boolean activateDraconium(int x, int y, int z)
	{
		if (this.testForOrActivateDraconium(x, y, z, false, false))
		{
			// TODO gamerforEA code start
			if (this.cantBreak(x, y, z))
				return false;
			// TODO gamerforEA code end

			this.worldObj.setBlock(x, y, z, ModBlocks.invisibleMultiblock, 0, 2);
			TileInvisibleMultiblock tile = this.worldObj.getTileEntity(x, y, z) != null && this.worldObj.getTileEntity(x, y, z) instanceof TileInvisibleMultiblock ? (TileInvisibleMultiblock) this.worldObj.getTileEntity(x, y, z) : null;
			if (tile != null)
				tile.master = new TileLocation(this.xCoord, this.yCoord, this.zCoord);
			return true;
		}
		LogHelper.error("Failed to activate structure (activateDraconium)");
		return false;
	}

	private boolean activateRedstone(int x, int y, int z)
	{
		if (this.testForOrActivateRedstone(x, y, z, false, false))
		{
			// TODO gamerforEA code start
			if (this.cantBreak(x, y, z))
				return false;
			// TODO gamerforEA code end

			this.worldObj.setBlock(x, y, z, ModBlocks.invisibleMultiblock, 1, 2);
			TileInvisibleMultiblock tile = this.worldObj.getTileEntity(x, y, z) != null && this.worldObj.getTileEntity(x, y, z) instanceof TileInvisibleMultiblock ? (TileInvisibleMultiblock) this.worldObj.getTileEntity(x, y, z) : null;
			if (tile != null)
				tile.master = new TileLocation(this.xCoord, this.yCoord, this.zCoord);
			return true;
		}
		LogHelper.error("Failed to activate structure (activateRedstone)");
		return false;
	}

	private boolean isReplacable(int x, int y, int z, boolean set)
	{
		Block block = this.worldObj.getBlock(x, y, z);

		// TODO gamerforEA code start
		if (this.cantBreak(x, y, z))
			return false;
		// TODO gamerforEA code end

		if (set)
		{
			this.worldObj.setBlock(x, y, z, Blocks.air);
			return true;
		}
		else
			return block.isReplaceable(this.worldObj, x, y, z) || this.worldObj.isAirBlock(x, y, z);
	}

	// TODO gamerforEA code start
	private boolean cantBreak(int x, int y, int z)
	{
		return this.worldObj.getBlock(x, y, z).getBlockHardness(this.worldObj, x, y, z) == -1 || this.fake.cantBreak(x, y, z);
	}
	// TODO gamerforEA code end

	public boolean isOnline()
	{
		return this.online;
	}

	private void activateStabilizers()
	{
		for (int i = 0; i < this.stabilizers.length; i++)
		{
			if (this.stabilizers[i] == null)
			{
				LogHelper.error("activateStabilizers stabalizers[" + i + "] == null!!!");
				return;
			}
			TileParticleGenerator tile = this.worldObj.getTileEntity(this.stabilizers[i].getXCoord(), this.stabilizers[i].getYCoord(), this.stabilizers[i].getZCoord()) != null && this.worldObj.getTileEntity(this.stabilizers[i].getXCoord(), this.stabilizers[i].getYCoord(), this.stabilizers[i].getZCoord()) instanceof TileParticleGenerator ? (TileParticleGenerator) this.worldObj.getTileEntity(this.stabilizers[i].getXCoord(), this.stabilizers[i].getYCoord(), this.stabilizers[i].getZCoord()) : null;
			if (tile == null)
			{
				LogHelper.error("Missing Tile Entity (Particle Generator)");
				return;
			}
			tile.stabalizerMode = true;
			tile.setMaster(new TileLocation(this.xCoord, this.yCoord, this.zCoord));
			this.worldObj.setBlockMetadataWithNotify(this.stabilizers[i].getXCoord(), this.stabilizers[i].getYCoord(), this.stabilizers[i].getZCoord(), 1, 2);
		}
		this.initializeCapacity();
	}

	private void initializeCapacity()
	{
		long capacity = 0;
		switch (this.tier)
		{
			case 0:
				capacity = BalanceConfigHandler.energyStorageTier1Storage;
				break;
			case 1:
				capacity = BalanceConfigHandler.energyStorageTier2Storage;
				break;
			case 2:
				capacity = BalanceConfigHandler.energyStorageTier3Storage;
				break;
			case 3:
				capacity = BalanceConfigHandler.energyStorageTier4Storage;
				break;
			case 4:
				capacity = BalanceConfigHandler.energyStorageTier5Storage;
				break;
			case 5:
				capacity = BalanceConfigHandler.energyStorageTier6Storage;
				break;
			case 6:
				capacity = BalanceConfigHandler.energyStorageTier7Storage;
				break;
		}
		this.capacity = capacity;
		if (this.energy > capacity)
			this.energy = capacity;
	}

	public void deactivateStabilizers()
	{
		for (int i = 0; i < this.stabilizers.length; i++)
		{
			if (this.stabilizers[i] == null)
				LogHelper.error("activateStabilizers stabalizers[" + i + "] == null!!!");
			else
			{
				TileParticleGenerator tile = this.worldObj.getTileEntity(this.stabilizers[i].getXCoord(), this.stabilizers[i].getYCoord(), this.stabilizers[i].getZCoord()) != null && this.worldObj.getTileEntity(this.stabilizers[i].getXCoord(), this.stabilizers[i].getYCoord(), this.stabilizers[i].getZCoord()) instanceof TileParticleGenerator ? (TileParticleGenerator) this.worldObj.getTileEntity(this.stabilizers[i].getXCoord(), this.stabilizers[i].getYCoord(), this.stabilizers[i].getZCoord()) : null;
				if (tile == null)
				{
					//LogHelper.error("Missing Tile Entity (Particle Generator)");
				}
				else
				{
					tile.stabalizerMode = false;
					this.worldObj.setBlockMetadataWithNotify(this.stabilizers[i].getXCoord(), this.stabilizers[i].getYCoord(), this.stabilizers[i].getZCoord(), 0, 2);
				}
			}
		}
	}

	private boolean areStabilizersActive()
	{
		for (int i = 0; i < this.stabilizers.length; i++)
		{
			if (this.stabilizers[i] == null)
			{
				LogHelper.error("activateStabilizers stabalizers[" + i + "] == null!!!");
				return false;
			}
			TileParticleGenerator tile = this.worldObj.getTileEntity(this.stabilizers[i].getXCoord(), this.stabilizers[i].getYCoord(), this.stabilizers[i].getZCoord()) != null && this.worldObj.getTileEntity(this.stabilizers[i].getXCoord(), this.stabilizers[i].getYCoord(), this.stabilizers[i].getZCoord()) instanceof TileParticleGenerator ? (TileParticleGenerator) this.worldObj.getTileEntity(this.stabilizers[i].getXCoord(), this.stabilizers[i].getYCoord(), this.stabilizers[i].getZCoord()) : null;
			if (tile == null)
				//LogHelper.error("Missing Tile Entity (Particle Generator)");
				return false;
			if (!tile.stabalizerMode || this.worldObj.getBlockMetadata(this.stabilizers[i].getXCoord(), this.stabilizers[i].getYCoord(), this.stabilizers[i].getZCoord()) != 1)
				return false;
		}
		return true;
	}

	private boolean checkStabilizers()
	{
		for (TileLocation stabilizer : this.stabilizers)
		{
			if (stabilizer == null)
				return false;
			TileParticleGenerator gen = this.worldObj.getTileEntity(stabilizer.getXCoord(), stabilizer.getYCoord(), stabilizer.getZCoord()) != null && this.worldObj.getTileEntity(stabilizer.getXCoord(), stabilizer.getYCoord(), stabilizer.getZCoord()) instanceof TileParticleGenerator ? (TileParticleGenerator) this.worldObj.getTileEntity(stabilizer.getXCoord(), stabilizer.getYCoord(), stabilizer.getZCoord()) : null;
			if (gen == null || !gen.stabalizerMode)
				return false;
			if (gen.getMaster().xCoord != this.xCoord || gen.getMaster().yCoord != this.yCoord || gen.getMaster().zCoord != this.zCoord)
				return false;
		}
		return true;
	}

	public int getTier()
	{
		return this.tier;
	}

	/**
	 * ###############################################################
	 */

	@Override
	public void writeToNBT(NBTTagCompound compound)
	{
		super.writeToNBT(compound);
		compound.setBoolean("Online", this.online);
		compound.setShort("Tier", (short) this.tier);
		compound.setLong("EnergyL", this.energy);
		for (int i = 0; i < this.stabilizers.length; i++)
		{
			if (this.stabilizers[i] != null)
				this.stabilizers[i].writeToNBT(compound, String.valueOf(i));
		}

		// TODO gamerforEA code start
		this.fake.writeToNBT(compound);
		// TODO gamerforEA code end
	}

	@Override
	public void readFromNBT(NBTTagCompound compound)
	{
		this.online = compound.getBoolean("Online");
		this.tier = compound.getShort("Tier");
		this.energy = compound.getLong("EnergyL");
		if (compound.hasKey("Energy"))
			this.energy = (long) compound.getDouble("Energy");
		for (int i = 0; i < this.stabilizers.length; i++)
		{
			if (this.stabilizers[i] != null)
				this.stabilizers[i].readFromNBT(compound, String.valueOf(i));
		}
		this.initializeCapacity();

		// TODO gamerforEA code start
		this.fake.readFromNBT(compound);
		// TODO gamerforEA code end

		super.readFromNBT(compound);
	}

	@Override
	public Packet getDescriptionPacket()
	{
		NBTTagCompound nbttagcompound = new NBTTagCompound();
		this.writeToNBT(nbttagcompound);
		return new S35PacketUpdateTileEntity(this.xCoord, this.yCoord, this.zCoord, 1, nbttagcompound);
	}

	@Override
	public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt)
	{
		this.readFromNBT(pkt.func_148857_g());
	}

	/* EnergyHandler */

	public int receiveEnergy(int maxReceive, boolean simulate)
	{
		long energyReceived = Math.min(this.capacity - this.energy, maxReceive);

		if (!simulate)
			this.energy += energyReceived;
		return (int) energyReceived;
	}

	public int extractEnergy(int maxExtract, boolean simulate)
	{
		long energyExtracted = Math.min(this.energy, maxExtract);

		if (!simulate)
			this.energy -= energyExtracted;
		return (int) energyExtracted;
	}

	public long getEnergyStored()
	{
		return this.energy;
	}

	public long getMaxEnergyStored()
	{
		return this.capacity;
	}

	@Override
	public AxisAlignedBB getRenderBoundingBox()
	{
		return INFINITE_EXTENT_AABB;
	}

	@Override
	public double getMaxRenderDistanceSquared()
	{
		return 40960.0D;
	}

	private void detectAndRendChanges()
	{
		if (this.lastTickCapacity != this.energy)
			this.lastTickCapacity = (Long) this.sendObjectToClient(References.LONG_ID, 0, this.energy, new NetworkRegistry.TargetPoint(this.worldObj.provider.dimensionId, this.xCoord, this.yCoord, this.zCoord, 20));
	}

	@Override
	public void receiveObjectFromServer(int index, Object object)
	{
		this.energy = (Long) object;
	}
}
