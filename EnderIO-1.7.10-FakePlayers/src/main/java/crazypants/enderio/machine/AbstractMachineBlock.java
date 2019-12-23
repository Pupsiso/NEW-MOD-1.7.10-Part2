package crazypants.enderio.machine;

import com.enderio.core.api.client.gui.IResourceTooltipProvider;
import com.enderio.core.common.TileEntityEnder;
import cpw.mods.fml.common.network.IGuiHandler;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import crazypants.enderio.BlockEio;
import crazypants.enderio.ClientProxy;
import crazypants.enderio.EnderIO;
import crazypants.enderio.ModObject;
import crazypants.enderio.network.PacketHandler;
import crazypants.enderio.waila.IWailaInfoProvider;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.List;
import java.util.Random;

public abstract class AbstractMachineBlock<T extends AbstractMachineEntity> extends BlockEio
		implements IGuiHandler, IResourceTooltipProvider, IWailaInfoProvider
{

	public static int renderId;

	public IIcon overlayIconPull;
	public IIcon overlayIconPush;
	public IIcon overlayIconPushPull;
	public IIcon overlayIconDisabled;
	public IIcon overlayIconNone;
	public IIcon overlayIconDirty;

	public IIcon selectedFaceIcon;

	@SideOnly(Side.CLIENT)
	protected IIcon[][] iconBuffer;

	protected final Random random;

	protected final ModObject modObject;

	static
	{
		PacketHandler.INSTANCE.registerMessage(PacketIoMode.class, PacketIoMode.class, PacketHandler.nextID(), Side.SERVER);
		PacketHandler.INSTANCE.registerMessage(PacketItemBuffer.class, PacketItemBuffer.class, PacketHandler.nextID(), Side.SERVER);
		PacketHandler.INSTANCE.registerMessage(PacketPowerStorage.class, PacketPowerStorage.class, PacketHandler.nextID(), Side.CLIENT);
	}

	protected AbstractMachineBlock(ModObject mo, Class<T> teClass, Material mat)
	{
		super(mo.unlocalisedName, teClass, mat);
		this.modObject = mo;
		this.setHardness(2.0F);
		this.setStepSound(soundTypeMetal);
		this.setHarvestLevel("pickaxe", 0);
		this.random = new Random();
	}

	protected AbstractMachineBlock(ModObject mo, Class<T> teClass)
	{
		this(mo, teClass, new Material(MapColor.ironColor));
	}

	@Override
	protected void init()
	{
		GameRegistry.registerBlock(this, this.modObject.unlocalisedName);
		GameRegistry.registerTileEntity(this.teClass, this.modObject.unlocalisedName + "TileEntity");
		EnderIO.guiHandler.registerGuiHandler(this.getGuiId(), this);
	}

	@Override
	public int getRenderType()
	{
		return renderId;
	}

	@Override
	public boolean openGui(World world, int x, int y, int z, EntityPlayer player, int side)
	{
		if (!world.isRemote)
		{
			// TODO gamerforEa code start
			if (player.openContainer != null && player.openContainer != player.inventoryContainer)
				return true;
			// TODO gamerforEa code end

			player.openGui(EnderIO.instance, this.getGuiId(), world, x, y, z);
		}

		return true;
	}

	@Override
	public boolean canSilkHarvest(World world, EntityPlayer player, int x, int y, int z, int metadata)
	{
		return false;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister iIconRegister)
	{

		this.iconBuffer = new IIcon[2][12];
		String side = this.getSideIconKey(false);
		// first the 6 sides in OFF state
		this.iconBuffer[0][0] = iIconRegister.registerIcon(this.getBottomIconKey(false));
		this.iconBuffer[0][1] = iIconRegister.registerIcon(this.getTopIconKey(false));
		this.iconBuffer[0][2] = iIconRegister.registerIcon(this.getBackIconKey(false));
		this.iconBuffer[0][3] = iIconRegister.registerIcon(this.getMachineFrontIconKey(false));
		this.iconBuffer[0][4] = iIconRegister.registerIcon(side);
		this.iconBuffer[0][5] = iIconRegister.registerIcon(side);

		side = this.getSideIconKey(true);
		this.iconBuffer[0][6] = iIconRegister.registerIcon(this.getBottomIconKey(true));
		this.iconBuffer[0][7] = iIconRegister.registerIcon(this.getTopIconKey(true));
		this.iconBuffer[0][8] = iIconRegister.registerIcon(this.getBackIconKey(true));
		this.iconBuffer[0][9] = iIconRegister.registerIcon(this.getMachineFrontIconKey(true));
		this.iconBuffer[0][10] = iIconRegister.registerIcon(side);
		this.iconBuffer[0][11] = iIconRegister.registerIcon(side);

		this.iconBuffer[1][0] = iIconRegister.registerIcon(this.getModelIconKey(false));
		this.iconBuffer[1][1] = iIconRegister.registerIcon(this.getModelIconKey(true));

		this.registerOverlayIcons(iIconRegister);

	}

	@SideOnly(Side.CLIENT)
	protected void registerOverlayIcons(IIconRegister iIconRegister)
	{
		this.overlayIconPull = iIconRegister.registerIcon("enderio:overlays/pull");
		this.overlayIconPush = iIconRegister.registerIcon("enderio:overlays/push");
		this.overlayIconPushPull = iIconRegister.registerIcon("enderio:overlays/pushPull");
		this.overlayIconDisabled = iIconRegister.registerIcon("enderio:overlays/disabled");
		this.overlayIconNone = iIconRegister.registerIcon("enderio:overlays/none");
		this.selectedFaceIcon = iIconRegister.registerIcon("enderio:overlays/selectedFace");
		this.overlayIconDirty = iIconRegister.registerIcon("enderio:overlays/dirt");
	}

	@SideOnly(Side.CLIENT)
	public IIcon getOverlayIconForMode(T tile, ForgeDirection face, IoMode mode)
	{
		if (mode == null)
			return null;
		switch (mode)
		{
			case DISABLED:
				return this.overlayIconDisabled;
			case PULL:
				return this.overlayIconPull;
			case PUSH:
				return this.overlayIconPush;
			case PUSH_PULL:
				return this.overlayIconPushPull;
			default:
				return tile.isDirty ? this.overlayIconDirty : null;
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(IBlockAccess world, int x, int y, int z, int blockSide)
	{

		// used to render the block in the world
		TileEntity te = world.getTileEntity(x, y, z);
		int facing = 3;
		if (te instanceof AbstractMachineEntity)
		{
			AbstractMachineEntity me = (AbstractMachineEntity) te;
			facing = me.facing;
		}
		if (this.isActive(world, x, y, z))
			return this.iconBuffer[0][ClientProxy.sideAndFacingToSpriteOffset[blockSide][facing] + 6];
		else
			return this.iconBuffer[0][ClientProxy.sideAndFacingToSpriteOffset[blockSide][facing]];
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int blockSide, int blockMeta)
	{
		// This is used to render the block as an item
		return this.iconBuffer[0][blockSide];
	}

	public IIcon getModelIcon(IBlockAccess world, int x, int y, int z)
	{
		return this.getModelIcon(((AbstractMachineEntity) world.getTileEntity(x, y, z)).isActive());
	}

	public IIcon getModelIcon()
	{
		return this.getModelIcon(false);
	}

	private IIcon getModelIcon(boolean active)
	{
		return active ? this.iconBuffer[1][1] : this.iconBuffer[1][0];
	}

	@Override
	public boolean doNormalDrops(World world, int x, int y, int z)
	{
		return false;
	}

	@Override
	protected void processDrop(World world, int x, int y, int z, TileEntityEnder te, ItemStack stack)
	{
		if (te != null)
			((AbstractMachineEntity) te).writeToItemStack(stack);
	}

	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase player, ItemStack stack)
	{
		super.onBlockPlacedBy(world, x, y, z, player, stack);
		int heading = MathHelper.floor_double(player.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
		AbstractMachineEntity te = (AbstractMachineEntity) world.getTileEntity(x, y, z);
		te.setFacing(this.getFacingForHeading(heading));
		te.readFromItemStack(stack);

		// TODO gamerforEA code start
		if (player instanceof EntityPlayer)
			te.fake.setProfile(((EntityPlayer) player).getGameProfile());
		// TODO gamerforEA code end

		if (world.isRemote)
			return;
		world.markBlockForUpdate(x, y, z);
	}

	protected short getFacingForHeading(int heading)
	{
		switch (heading)
		{
			case 0:
				return 2;
			case 1:
				return 5;
			case 2:
				return 3;
			case 3:
			default:
				return 4;
		}
	}

	@Override
	public void onBlockAdded(World world, int x, int y, int z)
	{
		super.onBlockAdded(world, x, y, z);
		world.markBlockForUpdate(x, y, z);
	}

	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block blockId)
	{
		TileEntity ent = world.getTileEntity(x, y, z);
		if (ent instanceof AbstractMachineEntity)
		{
			AbstractMachineEntity te = (AbstractMachineEntity) ent;
			te.onNeighborBlockChange(blockId);
		}
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void randomDisplayTick(World world, int x, int y, int z, Random rand)
	{
		// If active, randomly throw some smoke around
		if (this.isActive(world, x, y, z))
		{
			float startX = x + 1.0F;
			float startY = y + 1.0F;
			float startZ = z + 1.0F;
			for (int i = 0; i < 4; i++)
			{
				float xOffset = -0.2F - rand.nextFloat() * 0.6F;
				float yOffset = -0.1F + rand.nextFloat() * 0.2F;
				float zOffset = -0.2F - rand.nextFloat() * 0.6F;
				world.spawnParticle("smoke", startX + xOffset, startY + yOffset, startZ + zOffset, 0.0D, 0.0D, 0.0D);
			}
		}
	}

	protected abstract int getGuiId();

	protected abstract String getMachineFrontIconKey(boolean active);

	protected String getSideIconKey(boolean active)
	{
		return "enderio:machineSide";
	}

	protected String getBackIconKey(boolean active)
	{
		return "enderio:machineBack";
	}

	protected String getTopIconKey(boolean active)
	{
		return "enderio:machineTop";
	}

	protected String getBottomIconKey(boolean active)
	{
		return "enderio:machineBottom";
	}

	protected String getModelIconKey(boolean active)
	{
		return this.getSideIconKey(active);
	}

	protected boolean isActive(IBlockAccess blockAccess, int x, int y, int z)
	{
		TileEntity te = blockAccess.getTileEntity(x, y, z);
		if (te instanceof AbstractMachineEntity)
			return ((AbstractMachineEntity) te).isActive();
		return false;
	}

	@Override
	public String getUnlocalizedNameForTooltip(ItemStack stack)
	{
		return this.getUnlocalizedName();
	}

	@Override
	public void getWailaInfo(List<String> tooltip, EntityPlayer player, World world, int x, int y, int z)
	{
	}

	@Override
	public int getDefaultDisplayMask(World world, int x, int y, int z)
	{
		return IWailaInfoProvider.ALL_BITS;
	}
}
