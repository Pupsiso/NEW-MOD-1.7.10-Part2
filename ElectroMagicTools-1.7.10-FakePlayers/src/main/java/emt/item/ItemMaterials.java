package emt.item;

import com.gamerforea.emt.EntityLightningBoltByPlayer;
import com.gamerforea.eventhelper.util.EventUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import emt.EMT;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ArrowNockEvent;
import thaumcraft.common.config.Config;
import thaumcraft.common.lib.utils.InventoryUtils;

import java.util.List;

public class ItemMaterials extends Item
{

	public IIcon[] icon = new IIcon[16];

	public ItemMaterials()
	{
		this.setCreativeTab(EMT.TAB);
		this.setMaxDamage(0);
		this.setMaxStackSize(64);
		this.setHasSubtypes(true);
	}

	@Override
	public String getUnlocalizedName(ItemStack itemstack)
	{
		String name = null;
		switch (itemstack.getItemDamage())
		{
			case 0:
				name = "oreClusterUranium";
				break;
			case 1:
				name = "crushedOreAmber";
				break;
			case 2:
				name = "purifiedOreAmber";
				break;
			case 3:
				name = "crushedOreCinnabar";
				break;
			case 4:
				name = "purifiedOreCinnabar";
				break;
			case 5:
				name = "thaumiumPlate";
				break;
			case 6:
				name = "lightningSummoner";
				break;
			case 7:
				name = "featherMesh";
				break;
			case 8:
				name = "glue";
				break;
			case 9:
				name = "ductTape";
				break;
			case 10:
				name = "rubberBall";
				break;
			case 11:
				name = "cardboard";
				break;
			case 12:
				name = "featherGluedCardboardWing";
				break;
			case 13:
				name = "taintedFeather";
				break;
			case 14:
				name = "thaumiumWing";
				break;
			case 15:
				name = "uumatterDrop";
				break;
			default:
				name = "nothing";
				break;
		}
		return "item." + EMT.MOD_ID + "." + name;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister ri)
	{
		this.icon[0] = ri.registerIcon(EMT.TEXTURE_PATH + ":materials/clusteruranium");
		this.icon[1] = ri.registerIcon(EMT.TEXTURE_PATH + ":materials/crushedamber");
		this.icon[2] = ri.registerIcon(EMT.TEXTURE_PATH + ":materials/purifiedamber");
		this.icon[3] = ri.registerIcon(EMT.TEXTURE_PATH + ":materials/crushedcinnabar");
		this.icon[4] = ri.registerIcon(EMT.TEXTURE_PATH + ":materials/purifiedcinnabar");
		this.icon[5] = ri.registerIcon(EMT.TEXTURE_PATH + ":materials/thaumiumplate");
		this.icon[6] = ri.registerIcon(EMT.TEXTURE_PATH + ":materials/lightningsummoner");
		this.icon[7] = ri.registerIcon(EMT.TEXTURE_PATH + ":materials/feathermesh");
		this.icon[8] = ri.registerIcon(EMT.TEXTURE_PATH + ":materials/glue");
		this.icon[9] = ri.registerIcon(EMT.TEXTURE_PATH + ":materials/ducttape");
		this.icon[10] = ri.registerIcon(EMT.TEXTURE_PATH + ":materials/rubberball");
		this.icon[11] = ri.registerIcon(EMT.TEXTURE_PATH + ":materials/cardboard");
		this.icon[12] = ri.registerIcon(EMT.TEXTURE_PATH + ":materials/cardboardsheet");
		this.icon[13] = ri.registerIcon(EMT.TEXTURE_PATH + ":materials/taintedfeather");
		this.icon[14] = ri.registerIcon(EMT.TEXTURE_PATH + ":materials/thaumiumWing");
		this.icon[15] = ri.registerIcon(EMT.TEXTURE_PATH + ":materials/uumatterdrop");
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIconFromDamage(int meta)
	{
		return this.icon[meta];
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void getSubItems(Item item, CreativeTabs par2CreativeTabs, List list)
	{
		for (int i = 1; i <= 15; i++)
		{
			list.add(new ItemStack(this, 1, i));
		}
	}

	@Override
	// TODO gamerforEA use EntityLightningBoltByPlayer
	public void onUsingTick(ItemStack stack, EntityPlayer player, int count)
	{
		if (stack != null && stack.getItemDamage() == 6 && this.getMaxItemUseDuration(stack) - count >= 40)
		{
			World world = player.worldObj;
			player.swingItem();
			float f = 1.0F;
			float f1 = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * f;
			float f2 = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * f;
			double playerX = player.prevPosX + (player.posX - player.prevPosX) * f;
			double playerY = player.prevPosY + (player.posY - player.prevPosY) * f + 1.6200000000000001D - player.yOffset;
			double playerZ = player.prevPosZ + (player.posZ - player.prevPosZ) * f;
			Vec3 playerLoc = Vec3.createVectorHelper(playerX, playerY, playerZ);
			float f3 = MathHelper.cos(-f2 * 0.01745329F - 3.141593F);
			float f4 = MathHelper.sin(-f2 * 0.01745329F - 3.141593F);
			float f5 = -MathHelper.cos(-f1 * 0.01745329F);
			float f6 = MathHelper.sin(-f1 * 0.01745329F);
			float f7 = f4 * f5;
			float f8 = f6;
			float f9 = f3 * f5;
			float d3 = 5000;
			Vec3 vec3d1 = playerLoc.addVector(f7 * d3, f8 * d3, f9 * d3);
			MovingObjectPosition mop = player.worldObj.rayTraceBlocks(playerLoc, vec3d1, true);
			if (mop != null)
			{
				if (mop.typeOfHit == MovingObjectType.BLOCK)
				{
					int x2 = mop.blockX;
					int y2 = mop.blockY;
					int z2 = mop.blockZ;

					// TODO gamerforEA code start
					if (!EventUtils.cantBreak(player, x2, y2, z2))
						// TODO gamerforEA code end
						world.spawnEntityInWorld(new EntityLightningBoltByPlayer(player, world, x2, y2, z2));
				}
				else if (mop.typeOfHit == MovingObjectType.ENTITY)
				{
					Entity entityhit = mop.entityHit;
					double x = entityhit.posX;
					double y = entityhit.posY;
					double z = entityhit.posZ;

					// TODO gamerforEA code start
					if (!EventUtils.cantBreak(player, x, y, z))
						// TODO gamerforEA code end
						world.spawnEntityInWorld(new EntityLightningBoltByPlayer(player, world, x, y, z));
				}
				if (!player.capabilities.isCreativeMode)
					player.inventory.consumeInventoryItem(this);
			}
			player.stopUsingItem();
		}
	}

	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player)
	{
		if (stack.getItemDamage() == 6)
		{
			ArrowNockEvent event = new ArrowNockEvent(player, stack);
			MinecraftForge.EVENT_BUS.post(event);
			if (event.isCanceled())
				return event.result;
			player.setItemInUse(stack, this.getMaxItemUseDuration(stack));
		}
		return stack;
	}

	@Override
	public int getMaxItemUseDuration(ItemStack stack)
	{
		if (stack.getItemDamage() == 6)
			return 72000;
		return super.getMaxItemUseDuration(stack);
	}

	@Override
	public EnumAction getItemUseAction(ItemStack stack)
	{
		if (stack.getItemDamage() == 6)
			return EnumAction.bow;
		return EnumAction.none;
	}

	@Override
	public void onUpdate(ItemStack stack, World world, Entity entity, int par4, boolean par5)
	{
		super.onUpdate(stack, world, entity, par4, par5);
		if (!entity.worldObj.isRemote && stack.getItemDamage() == 13 && entity instanceof EntityLivingBase && !((EntityLivingBase) entity).isEntityUndead() && !((EntityLivingBase) entity).isPotionActive(Config.potionTaintPoisonID) && world.rand.nextInt(4321) <= stack.stackSize)
		{
			((EntityLivingBase) entity).addPotionEffect(new PotionEffect(Config.potionTaintPoisonID, 120, 0, false));
			if (entity instanceof EntityPlayer)
				InventoryUtils.consumeInventoryItem((EntityPlayer) entity, stack.getItem(), stack.getItemDamage());
		}
	}
}