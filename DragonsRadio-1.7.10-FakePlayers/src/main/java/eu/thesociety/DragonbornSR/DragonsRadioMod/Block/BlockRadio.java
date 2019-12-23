package eu.thesociety.DragonbornSR.DragonsRadioMod.Block;

import com.gamerforea.dragonsradio.ModUtils;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import eu.thesociety.DragonbornSR.DragonsRadioMod.Block.Gui.GuiRadio;
import eu.thesociety.DragonbornSR.DragonsRadioMod.Block.Gui.NGuiRadio;
import eu.thesociety.DragonbornSR.DragonsRadioMod.Block.TileEntity.TileEntityRadio;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockRadio extends Block implements ITileEntityProvider
{
	@SideOnly(Side.CLIENT)
	public static IIcon topIcon;
	@SideOnly(Side.CLIENT)
	public static IIcon bottomIcon;
	@SideOnly(Side.CLIENT)
	public static IIcon sideIcon;
	@SideOnly(Side.CLIENT)
	public static IIcon rearIcon;
	@SideOnly(Side.CLIENT)
	public static IIcon backIcon;
	public GuiRadio raadio;

	public BlockRadio()
	{
		super(Material.wood);
		this.setHardness(2.0F);
		this.setResistance(10.0F);
		this.setBlockName("Radio");
		this.setStepSound(Block.soundTypeWood);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister ir)
	{
		sideIcon = ir.registerIcon("dragonsradiomod:radio_side");
		rearIcon = ir.registerIcon("dragonsradiomod:radio_rear");
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int i, int j)
	{
		switch (i)
		{
			case 2:
				if (j == 1)
					return rearIcon;

				return sideIcon;
			case 3:
				if (j == 0)
					return rearIcon;
				if (j == 3)
					return rearIcon;

				return sideIcon;
			case 4:
				if (j == 4)
					return rearIcon;

				return sideIcon;
			case 5:
				if (j == 2)
					return rearIcon;

				return sideIcon;
			default:
				return sideIcon;
		}
	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int par6, float par7, float par8, float par9)
	{
		Side side = FMLCommonHandler.instance().getEffectiveSide();
		if (side == Side.CLIENT)
			this.openGUI(world, x, y, z);

		// TODO gamerforEA code start
		if (!world.isRemote)
		{
			TileEntity tile = world.getTileEntity(x, y, z);
			if (tile instanceof TileEntityRadio && ModUtils.isRealPlayer(player))
				((TileEntityRadio) tile).setLastPlayer(player);
		}
		// TODO gamerforEA code end

		return true;
	}

	@SideOnly(Side.CLIENT)
	private void openGUI(World par1World, int par2, int par3, int par4)
	{
		TileEntityRadio ter = (TileEntityRadio) par1World.getTileEntity(par2, par3, par4);
		this.raadio = new NGuiRadio(ter);
		Minecraft.getMinecraft().displayGuiScreen(this.raadio);
	}

	@Override
	public void onBlockPlacedBy(World par1World, int par2, int par3, int par4, EntityLivingBase par5EntityLiving, ItemStack par6ItemStack)
	{
		int l = MathHelper.floor_double((double) (par5EntityLiving.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;
		par1World.setBlockMetadataWithNotify(par2, par3, par4, l + 1, 2);
	}

	@Override
	public boolean canConnectRedstone(IBlockAccess world, int x, int y, int z, int side)
	{
		return true;
	}

	@Override
	public void onNeighborBlockChange(World world, int i, int j, int k, Block block)
	{
		boolean flag = world.isBlockIndirectlyGettingPowered(i, j, k);

		try
		{
			Side side = FMLCommonHandler.instance().getEffectiveSide();
			if (block.canProvidePower())
			{
				TileEntity tileEntity;
				if (side == Side.SERVER)
					tileEntity = MinecraftServer.getServer().getEntityWorld().getTileEntity(i, j, k);
				else
					tileEntity = FMLClientHandler.instance().getClient().theWorld.getTileEntity(i, j, k);

				((TileEntityRadio) tileEntity).setRedstoneInput(flag);
			}
		}
		catch (Exception ignored)
		{
		}
	}

	@Override
	public boolean shouldCheckWeakPower(IBlockAccess world, int x, int y, int z, int side)
	{
		return true;
	}

	@Override
	public TileEntity createNewTileEntity(World var1, int var2)
	{
		return new TileEntityRadio(var1);
	}
}
