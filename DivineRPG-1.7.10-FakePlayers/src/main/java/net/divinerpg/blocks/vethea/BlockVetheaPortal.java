package net.divinerpg.blocks.vethea;

import com.gamerforea.divinerpg.EventConfig;
import com.gamerforea.divinerpg.ModUtils;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.divinerpg.dimensions.vethea.TeleporterVetheaToOverworld;
import net.divinerpg.entities.fx.EntitySkythernPortalFX;
import net.divinerpg.libs.Reference;
import net.divinerpg.utils.LangRegistry;
import net.divinerpg.utils.config.ConfigurationHelper;
import net.divinerpg.utils.tabs.DivineRPGTabs;
import net.minecraft.block.BlockBreakable;
import net.minecraft.block.material.Material;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

public class BlockVetheaPortal extends BlockBreakable
{

	public static final int[][] sides = { new int[0], { 3, 1 }, { 2, 0 } };

	public BlockVetheaPortal()
	{
		super(Reference.PREFIX + "vetheaPortal", Material.portal, false);
		String name = "vetheaPortal";
		this.setBlockName(name);
		this.setBlockTextureName(Reference.PREFIX + name);
		if (Reference.DEBUG)
			this.setCreativeTab(DivineRPGTabs.blocks);
		else
			this.setCreativeTab(null);
		this.setTickRandomly(true);
		GameRegistry.registerBlock(this, name);
		LangRegistry.addBlock(this);
		this.setBlockUnbreakable();
		this.setLightLevel(0.8f);
	}

	@Override
	public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity)
	{

		if (entity.ridingEntity == null && entity.riddenByEntity == null)
			if (entity instanceof EntityPlayerMP)
			{
				EntityPlayerMP player = (EntityPlayerMP) entity;
				if (player.timeUntilPortal > 0)
					player.timeUntilPortal = 10;
				else if (player.dimension == ConfigurationHelper.vethea)
				{
					player.timeUntilPortal = 10;

					/* TODO gamerforEA code clear:
					NBTTagCompound persistantData = player.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
					persistantData.setTag("VetheaInv", player.inventory.writeToNBT(new NBTTagList()));
					player.getEntityData().setTag("PlayerPersisted", persistantData);
					player.inventory.clearInventory(null, -1);
					NBTTagList inv = persistantData.getTagList("OverworldInv", 10);
					player.inventory.readFromNBT(inv);
					player.inventoryContainer.detectAndSendChanges(); */

					// TODO gamerforEA code start
					if (EventConfig.portalOwnerOnly && !ModUtils.isMember(player, x, y, z))
						return;
					// TODO gamerforEA code end

					player.mcServer.getConfigurationManager().transferPlayerToDimension(player, 0, new TeleporterVetheaToOverworld(player.mcServer.worldServerForDimension(0)));
				}
			}
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool(World w, int x, int y, int z)
	{
		return null;
	}

	@Override
	public void setBlockBoundsBasedOnState(IBlockAccess blockAccess, int xPos, int yPos, int zPos)
	{
		int meta = this.getMeta(blockAccess.getBlockMetadata(xPos, yPos, zPos));

		if (meta == 0)
		{
			if (blockAccess.getBlock(xPos - 1, yPos, zPos) != this && blockAccess.getBlock(xPos + 1, yPos, zPos) != this)
				meta = 2;
			else
				meta = 1;

			if (blockAccess instanceof World && !((World) blockAccess).isRemote)
				((World) blockAccess).setBlockMetadataWithNotify(xPos, yPos, zPos, meta, 2);
		}

		float f = 0.125F;
		float f1 = 0.125F;

		if (meta == 1)
			f = 0.5F;

		if (meta == 2)
			f1 = 0.5F;

		this.setBlockBounds(0.5F - f, 0.0F, 0.5F - f1, 0.5F + f, 1.0F, 0.5F + f1);
	}

	@Override
	public boolean renderAsNormalBlock()
	{
		return false;
	}

	@SideOnly(Side.CLIENT)
	@Override
	public boolean shouldSideBeRendered(IBlockAccess blockAccess, int xPos, int yPos, int zPos, int side)
	{
		int i1 = 0;

		if (blockAccess.getBlock(xPos, yPos, zPos) == this)
		{
			i1 = this.getMeta(blockAccess.getBlockMetadata(xPos, yPos, zPos));
			if (i1 == 0 || i1 == 1 && side != 3 && side != 2 || i1 == 2 && side != 5 && side != 4)
				return false;
		}

		boolean flag = blockAccess.getBlock(xPos - 1, yPos, zPos) == this && blockAccess.getBlock(xPos - 2, yPos, zPos) != this;
		boolean flag1 = blockAccess.getBlock(xPos + 1, yPos, zPos) == this && blockAccess.getBlock(xPos + 2, yPos, zPos) != this;
		boolean flag2 = blockAccess.getBlock(xPos, yPos, zPos - 1) == this && blockAccess.getBlock(xPos, yPos, zPos - 2) != this;
		boolean flag3 = blockAccess.getBlock(xPos, yPos, zPos + 1) == this && blockAccess.getBlock(xPos, yPos, zPos + 2) != this;
		boolean flag4 = flag || flag1 || i1 == 1;
		boolean flag5 = flag2 || flag3 || i1 == 2;
		return flag4 && side == 4 || flag4 && side == 5 || flag5 && side == 2 || flag5 && side == 3;
	}

	@Override
	public int quantityDropped(Random rand)
	{
		return 0;
	}

	@SideOnly(Side.CLIENT)
	@Override
	public int getRenderBlockPass()
	{
		return 1;
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void randomDisplayTick(World world, int xPos, int yPos, int zPos, Random rand)
	{
		for (int l = 0; l < 4; ++l)
		{
			double d0 = xPos + rand.nextFloat();
			double d1 = yPos + rand.nextFloat();
			double d2 = zPos + rand.nextFloat();
			double d3 = 0.0D;
			double d4 = 0.0D;
			double d5 = 0.0D;
			int i1 = rand.nextInt(2) * 2 - 1;
			d3 = (rand.nextFloat() - 0.5D) * 0.5D;
			d4 = (rand.nextFloat() - 0.5D) * 0.5D;
			d5 = (rand.nextFloat() - 0.5D) * 0.5D;

			if (world.getBlock(xPos - 1, yPos, zPos) != this && world.getBlock(xPos + 1, yPos, zPos) != this)
			{
				d0 = xPos + 0.5D + 0.25D * i1;
				d3 = rand.nextFloat() * 2.0F * i1;
			}
			else
			{
				d2 = zPos + 0.5D + 0.25D * i1;
				d5 = rand.nextFloat() * 2.0F * i1;
			}
			EntityFX var20 = new EntitySkythernPortalFX(world, d0, d1, d2, d3, d4, d5);
			FMLClientHandler.instance().getClient().effectRenderer.addEffect(var20);
		}
	}

	public int getMeta(int meta)
	{
		return meta & 3;
	}
}
