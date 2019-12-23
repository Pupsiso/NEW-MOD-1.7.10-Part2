package emt.item;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import com.gamerforea.emt.EventConfig;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import emt.EMT;
import emt.util.EMTTextHelper;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.PlayerCapabilities;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.DamageSource;
import net.minecraft.util.IIcon;

import java.util.List;
import java.util.Random;

public class ItemOneRing extends ItemBase implements IBauble
{
	private static final String NBT_FORGE_DATA = "ForgeData";
	private static final String NBT_MIND_CORRUPTION = "MindCorruption";
	public IIcon[] icon = new IIcon[16];
	public Random random = new Random();

	public ItemOneRing()
	{
		super("bauble");
		this.setHasSubtypes(true);
		this.setMaxDamage(0);
		this.setMaxStackSize(1);
	}

	@Override
	public String getUnlocalizedName(ItemStack itemstack)
	{
		String name = "";
		switch (itemstack.getItemDamage())
		{
			case 0:
				name = "oneRing";
				break;
			default:
				name = "nothing";
				break;
		}
		return "item." + EMT.MOD_ID + ".bauble." + name;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister ri)
	{
		this.icon[0] = ri.registerIcon(EMT.TEXTURE_PATH + ":onering");
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
		list.add(new ItemStack(this, 1, 0));
	}

	@Override
	public BaubleType getBaubleType(ItemStack stack)
	{
		if (stack.getItemDamage() <= 0)
			return BaubleType.RING;
		return null;
	}

	@Override
	public void onWornTick(ItemStack stack, EntityLivingBase player)
	{
		if (!player.isInvisible())
			player.setInvisible(true);

		int corruption = 0;

		/* TODO gamerforEA code replace, old code:
		NBTTagCompound playerTag = new NBTTagCompound();
		player.writeToNBT(playerTag);
		NBTTagCompound forgeTag = playerTag.getCompoundTag(NBT_FORGE_DATA);
		if (forgeTag.hasKey(NBT_MIND_CORRUPTION))
			corruption = forgeTag.getInteger(NBT_MIND_CORRUPTION);
		else
			forgeTag.setInteger(NBT_MIND_CORRUPTION, 0); */
		NBTTagCompound playerTag = null;
		NBTTagCompound forgeTag;
		if (EventConfig.oneRingNbtFix)
		{
			forgeTag = player.getEntityData();
			corruption = forgeTag.getInteger(NBT_MIND_CORRUPTION);
		}
		else
		{
			playerTag = new NBTTagCompound();
			player.writeToNBT(playerTag);
			forgeTag = playerTag.getCompoundTag(NBT_FORGE_DATA);
			if (forgeTag.hasKey(NBT_MIND_CORRUPTION))
				corruption = forgeTag.getInteger(NBT_MIND_CORRUPTION);
			else
				forgeTag.setInteger(NBT_MIND_CORRUPTION, 0);
		}
		// TODO gamerforEA code end

		((EntityPlayer) player).capabilities.disableDamage = true;

		if (!player.worldObj.isRemote)
			if (corruption <= 0)
				((EntityPlayer) player).addChatMessage(new ChatComponentText(EMTTextHelper.PURPLE + "You have worn the Ring. Your soul has now been forever " + EMTTextHelper.PURPLE + "tainted. " + EMTTextHelper.RED + EMTTextHelper.ITALIC + "Beware of wearing the ring. The tainting will only " + EMTTextHelper.RED + EMTTextHelper.ITALIC + "increase, and strange things will start happening."));
			else if (corruption > 6000 && corruption < 24000 && this.random.nextInt(2000) == 0)
				player.addPotionEffect(new PotionEffect(Potion.blindness.id, 500, 2, false));
			else if (corruption >= 6000 && corruption < 24000 && this.random.nextInt(2000) == 0)
				player.addPotionEffect(new PotionEffect(Potion.confusion.id, 500, 2, false));
			else if (corruption >= 24000 && corruption < 72000 && this.random.nextInt(2000) == 0)
			{
				((EntityPlayer) player).capabilities.disableDamage = false;

				player.attackEntityFrom(DamageSource.magic, 5);
			}
			else if (corruption >= 72000 && corruption < 120000 && this.random.nextInt(4000) == 0)
			{
				((EntityPlayer) player).capabilities.disableDamage = false;

				player.motionY += 2d;
			}
			else if (corruption >= 120000 && this.random.nextInt(10000) == 0)
			{
				((EntityPlayer) player).capabilities.disableDamage = false;

				player.addPotionEffect(new PotionEffect(Potion.wither.id, 5000, 4, false));
			}
			else // =3333333
				if (corruption + 100 >= Integer.MAX_VALUE)
					player.isDead = true;

		forgeTag.setInteger(NBT_MIND_CORRUPTION, ++corruption);

		/* TODO gamerforEA code replace, old code:
		playerTag.setTag(NBT_FORGE_DATA, forgeTag);
		player.readFromNBT(playerTag); */
		if (!EventConfig.oneRingNbtFix && playerTag != null)
		{
			playerTag.setTag(NBT_FORGE_DATA, forgeTag);
			player.readFromNBT(playerTag);
		}
		// TODO gamerforEA code end
	}

	@Override
	public void onEquipped(ItemStack stack, EntityLivingBase player)
	{
		// TODO gamerforEA code start
		if (EventConfig.oneRingNbtFix)
		{
			player.getEntityData().removeTag(NBT_MIND_CORRUPTION);
			return;
		}
		// TODO gamerforEA code end

		NBTTagCompound tag = new NBTTagCompound();
		player.writeToNBT(tag);
		tag.getCompoundTag(NBT_FORGE_DATA).setInteger(NBT_MIND_CORRUPTION, 0);
		player.readFromNBT(tag);
	}

	@Override
	public void onUnequipped(ItemStack stack, EntityLivingBase player)
	{
		player.setInvisible(false);
		PlayerCapabilities playerCapabilities = ((EntityPlayer) player).capabilities;
		if (!playerCapabilities.isCreativeMode)
			playerCapabilities.disableDamage = false;

		// TODO gamerforEA code start
		if (EventConfig.oneRingNbtFix)
		{
			player.getEntityData().removeTag(NBT_MIND_CORRUPTION);
			player.addPotionEffect(new PotionEffect(Potion.confusion.id, 500, 2, false));
			return;
		}
		// TODO gamerforEA code end

		NBTTagCompound tag = new NBTTagCompound();
		player.writeToNBT(tag);
		tag.removeTag(NBT_FORGE_DATA);
		player.readFromNBT(tag);
		player.addPotionEffect(new PotionEffect(Potion.confusion.id, 500, 2, false));
	}

	@Override
	public boolean canEquip(ItemStack stack, EntityLivingBase player)
	{
		return stack.getItemDamage() == 0 && player instanceof EntityPlayer;
	}

	@Override
	public boolean canUnequip(ItemStack stack, EntityLivingBase player)
	{
		// TODO gamerforEA code start
		if (EventConfig.oneRingNbtFix)
			return player.getEntityData().getInteger(NBT_MIND_CORRUPTION) > 600;
		// TODO gamerforEA code end

		NBTTagCompound tag = new NBTTagCompound();
		player.writeToNBT(tag);
		return tag.getCompoundTag(NBT_FORGE_DATA).getInteger(NBT_MIND_CORRUPTION) > 600;
	}
}
