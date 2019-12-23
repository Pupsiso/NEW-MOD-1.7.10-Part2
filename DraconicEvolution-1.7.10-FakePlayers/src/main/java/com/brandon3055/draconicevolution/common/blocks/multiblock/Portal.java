package com.brandon3055.draconicevolution.common.blocks.multiblock;

import com.brandon3055.brandonscore.BrandonsCore;
import com.brandon3055.brandonscore.common.utills.Teleporter;
import com.brandon3055.draconicevolution.common.ModBlocks;
import com.brandon3055.draconicevolution.common.blocks.BlockDE;
import com.brandon3055.draconicevolution.common.lib.References;
import com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.TileDislocatorReceptacle;
import com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.TilePortalBlock;
import com.gamerforea.draconicevolution.EventConfig;
import com.gamerforea.eventhelper.util.EventUtils;
import com.gamerforea.eventhelper.util.FastUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;

import java.util.Random;

/**
 * Created by Brandon on 23/5/2015.
 */
public class Portal extends BlockDE implements ITileEntityProvider
{

	public Portal()
	{
		super(Material.portal);
		this.setBlockUnbreakable();
		this.setBlockName("portal");

		ModBlocks.register(this);
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void registerBlockIcons(IIconRegister iconRegister)
	{
		this.blockIcon = iconRegister.registerIcon(References.RESOURCESPREFIX + "transparency");
	}

	@Override
	public int getRenderType()
	{
		return References.idPortal;
	}

	@Override
	public AxisAlignedBB getSelectedBoundingBoxFromPool(World p_149633_1_, int p_149633_2_, int p_149633_3_, int p_149633_4_)
	{
		return AxisAlignedBB.getBoundingBox(0, 0, 0, 0, 0, 0);
	}

	@Override
	public boolean isOpaqueCube()
	{
		return false;
	}

	@Override
	public boolean renderAsNormalBlock()
	{
		return false;
	}

	@Override
	public TileEntity createNewTileEntity(World world, int i)
	{
		return new TilePortalBlock();
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool(World p_149668_1_, int p_149668_2_, int p_149668_3_, int p_149668_4_)
	{
		return null;
	}

	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block block)
	{
		if (world.isRemote)
			return;

		TileDislocatorReceptacle tile = this.getMaster(world, x, y, z);
		if (tile == null)
		{
			world.setBlockToAir(x, y, z);
			return;
		}

		if (tile.isActive)
			tile.validateActivePortal();

		if (!tile.isActive && !tile.updating)
		{
			world.setBlockToAir(x, y, z);
			return;
		}

		this.updateMetadata(world, x, y, z);
	}

	@Override
	public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity)
	{
		if (world.isRemote)
			return;

		TileDislocatorReceptacle tile = this.getMaster(world, x, y, z);
		if (tile != null && tile.isActive && tile.getLocation() != null)
		{
			if (tile.coolDown > 0)
				return;

			tile.coolDown = 1;
			Teleporter.TeleportLocation location = tile.getLocation();

			// TODO gamerforEA code start
			if (EventConfig.teleporterCheckRegion && entity instanceof EntityPlayer)
			{
				World locationWorld = BrandonsCore.proxy.getMCServer().worldServerForDimension(location.getDimension());
				FakePlayer fakePlayer = FastUtils.getFake(locationWorld, ((EntityPlayer) entity).getGameProfile());
				if (EventUtils.cantBreak(fakePlayer, location.getXCoord(), location.getYCoord(), location.getZCoord()))
				{
					tile.coolDown = 20;
					return;
				}
			}
			// TODO gamerforEA code end

			location.sendEntityToCoords(entity);
		}
		else if (tile != null)
			tile.validateActivePortal();
		else
			world.setBlockToAir(x, y, z);
	}

	private TileDislocatorReceptacle getMaster(World world, int x, int y, int z)
	{
		return world.getTileEntity(x, y, z) instanceof TilePortalBlock ? ((TilePortalBlock) world.getTileEntity(x, y, z)).getMaster() : null;
	}

	@Override
	public Item getItemDropped(int p_149650_1_, Random p_149650_2_, int p_149650_3_)
	{
		return null;
	}

	private boolean isPortalOrFrame(IBlockAccess access, int x, int y, int z)
	{
		Block block = access.getBlock(x, y, z);
		return block == ModBlocks.portal || block == ModBlocks.infusedObsidian || block == ModBlocks.dislocatorReceptacle;
	}

	@Override
	public void onBlockAdded(World world, int x, int y, int z)
	{
		this.updateMetadata(world, x, y, z);
	}

	private void updateMetadata(World world, int x, int y, int z)
	{
		if (world.isRemote || world.getBlockMetadata(x, y, z) != 0)
			return;
		int meta = 0;

		if (this.isPortalOrFrame(world, x, y + 1, z) && this.isPortalOrFrame(world, x, y - 1, z) && this.isPortalOrFrame(world, x + 1, y, z) && this.isPortalOrFrame(world, x - 1, y, z))
			meta = 1;
		else if (this.isPortalOrFrame(world, x, y + 1, z) && this.isPortalOrFrame(world, x, y - 1, z) && this.isPortalOrFrame(world, x, y, z + 1) && this.isPortalOrFrame(world, x, y, z - 1))
			meta = 2;
		else if (this.isPortalOrFrame(world, x + 1, y, z) && this.isPortalOrFrame(world, x - 1, y, z) && this.isPortalOrFrame(world, x, y, z + 1) && this.isPortalOrFrame(world, x, y, z - 1))
			meta = 3;

		world.setBlockMetadataWithNotify(x, y, z, meta, 2);
	}
}
