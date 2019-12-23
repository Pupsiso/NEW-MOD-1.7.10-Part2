package com.gamerforea.enderio;

import com.gamerforea.eventhelper.nexus.ModNexus;
import com.gamerforea.eventhelper.nexus.ModNexusFactory;
import com.gamerforea.eventhelper.nexus.NexusUtils;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

@ModNexus(name = "EnderIO", uuid = "b74424a0-197a-4fad-b798-cd07c597a882")
public final class ModUtils
{
	public static final ModNexusFactory NEXUS_FACTORY = NexusUtils.getFactory();

	public static boolean blockExists(IBlockAccess world, int x, int y, int z)
	{
		return world != null && (!(world instanceof World) || blockExists((World) world, x, y, z));
	}

	public static boolean blockExists(World world, int x, int y, int z)
	{
		return world != null && world.blockExists(x, y, z);
	}

	public static TileEntity getTileEntity(World world, int x, int y, int z)
	{
		return blockExists(world, x, y, z) ? world.getTileEntity(x, y, z) : null;
	}

	public static TileEntity getTileEntity(IBlockAccess world, int x, int y, int z)
	{
		return blockExists(world, x, y, z) ? world.getTileEntity(x, y, z) : null;
	}

	public static boolean isValidTileEntity(TileEntity tile)
	{
		return tile != null && !tile.isInvalid() && getTileEntity(tile.getWorldObj(), tile.xCoord, tile.yCoord, tile.zCoord) == tile;
	}
}