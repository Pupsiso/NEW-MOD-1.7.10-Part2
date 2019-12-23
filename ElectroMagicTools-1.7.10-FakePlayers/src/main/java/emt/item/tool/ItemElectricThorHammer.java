package emt.item.tool;

import com.gamerforea.emt.EntityLightningBoltByPlayer;
import com.gamerforea.emt.EventConfig;
import com.gamerforea.eventhelper.util.EventUtils;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import emt.EMT;
import emt.entity.EntityArcher;
import emt.util.EMTConfigHandler;
import emt.util.EMTTextHelper;
import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class ItemElectricThorHammer extends ItemSword implements IElectricItem
{
	public int maxCharge = 1000000;
	private final int hitCost = 5000;
	private final int lightningCost = 75000;

	// TODO gamerforEA code start
	private final Cache<EntityPlayer, Object> cache;
	// TODO gamerforEA code end

	public ItemElectricThorHammer()
	{
		super(ToolMaterial.EMERALD);
		this.setCreativeTab(EMT.TAB);
		this.setMaxDamage(27);
		this.setMaxStackSize(1);

		// TODO gamerforEA code start
		this.cache = CacheBuilder.newBuilder().weakKeys().expireAfterWrite(EventConfig.electricThorHammerCooldown, TimeUnit.SECONDS).build();
		// TODO gamerforEA code end
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void registerIcons(IIconRegister iconRegister)
	{
		this.itemIcon = iconRegister.registerIcon(EMT.TEXTURE_PATH + ":hammer/electricthorhammer");
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@SideOnly(Side.CLIENT)
	public void getSubItems(Item item, CreativeTabs par2CreativeTabs, List itemList)
	{
		ItemStack itemStack = new ItemStack(this, 1);
		if (this.getChargedItem(itemStack) == this)
		{
			ItemStack charged = new ItemStack(this, 1);
			ElectricItem.manager.charge(charged, 2147483647, 2147483647, true, false);
			itemList.add(charged);
		}
		if (this.getEmptyItem(itemStack) == this)
			itemList.add(new ItemStack(this, 1, this.getMaxDamage()));
	}

	@Override
	public boolean hitEntity(ItemStack itemstack, EntityLivingBase entityliving, EntityLivingBase attacker)
	{
		if (ElectricItem.manager.use(itemstack, this.hitCost, attacker))
			entityliving.attackEntityFrom(DamageSource.causePlayerDamage((EntityPlayer) attacker), 19F);
		return false;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean par4)
	{
		list.add(EMTTextHelper.localize("tooltip.EMT.hammer.electric").replace("%player%", player.getCommandSenderName()));
	}

	// TODO gamerforEA code start
	private static void spawnEntityInWorld(EntityPlayer player, World world, Entity entity)
	{
		if (!EventUtils.cantBreak(player, entity.posX, entity.posY, entity.posZ))
			world.spawnEntityInWorld(entity);
	}
	// TODO gamerforEA code end

	@Override
	// TODO gamerforEA use ItemElectricThorHammer.spawnEntityInWorld()
	// TODO gamerforEA use EntityLightningBoltByPlayer
	public ItemStack onItemRightClick(ItemStack itemstack, World world, EntityPlayer player)
	{
		if (player.capabilities.isCreativeMode)
			return itemstack;

		// TODO gamerforEA code start
		if (this.cache.getIfPresent(player) != null)
			return itemstack;
		this.cache.put(player, new Object());
		// TODO gamerforEA code end

		if (ElectricItem.manager.canUse(itemstack, this.lightningCost))
		{
			player.swingItem();
			// Corners
			spawnEntityInWorld(player, world, new EntityLightningBoltByPlayer(player, world, player.posX + 8, player.posY, player.posZ - 8));
			spawnEntityInWorld(player, world, new EntityLightningBoltByPlayer(player, world, player.posX - 8, player.posY, player.posZ + 8));
			spawnEntityInWorld(player, world, new EntityLightningBoltByPlayer(player, world, player.posX - 8, player.posY, player.posZ - 8));
			spawnEntityInWorld(player, world, new EntityLightningBoltByPlayer(player, world, player.posX + 8, player.posY, player.posZ + 8));

			// Fronts
			spawnEntityInWorld(player, world, new EntityLightningBoltByPlayer(player, world, player.posX + 8, player.posY, player.posZ));
			spawnEntityInWorld(player, world, new EntityLightningBoltByPlayer(player, world, player.posX - 8, player.posY, player.posZ));
			spawnEntityInWorld(player, world, new EntityLightningBoltByPlayer(player, world, player.posX, player.posY, player.posZ - 8));
			spawnEntityInWorld(player, world, new EntityLightningBoltByPlayer(player, world, player.posX, player.posY, player.posZ + 8));

			// Others
			spawnEntityInWorld(player, world, new EntityLightningBoltByPlayer(player, world, player.posX + 8, player.posY, player.posZ + 1));
			spawnEntityInWorld(player, world, new EntityLightningBoltByPlayer(player, world, player.posX - 8, player.posY, player.posZ + 2));
			spawnEntityInWorld(player, world, new EntityLightningBoltByPlayer(player, world, player.posX + 8, player.posY, player.posZ + 3));
			spawnEntityInWorld(player, world, new EntityLightningBoltByPlayer(player, world, player.posX - 8, player.posY, player.posZ + 4));

			spawnEntityInWorld(player, world, new EntityLightningBoltByPlayer(player, world, player.posX + 8, player.posY, player.posZ - 1));
			spawnEntityInWorld(player, world, new EntityLightningBoltByPlayer(player, world, player.posX - 8, player.posY, player.posZ - 2));
			spawnEntityInWorld(player, world, new EntityLightningBoltByPlayer(player, world, player.posX + 8, player.posY, player.posZ - 3));
			spawnEntityInWorld(player, world, new EntityLightningBoltByPlayer(player, world, player.posX - 8, player.posY, player.posZ - 4));

			spawnEntityInWorld(player, world, new EntityLightningBoltByPlayer(player, world, player.posX + 1, player.posY, player.posZ + 8));
			spawnEntityInWorld(player, world, new EntityLightningBoltByPlayer(player, world, player.posX + 2, player.posY, player.posZ - 8));
			spawnEntityInWorld(player, world, new EntityLightningBoltByPlayer(player, world, player.posX + 3, player.posY, player.posZ + 8));
			spawnEntityInWorld(player, world, new EntityLightningBoltByPlayer(player, world, player.posX + 4, player.posY, player.posZ - 8));

			spawnEntityInWorld(player, world, new EntityLightningBoltByPlayer(player, world, player.posX - 1, player.posY, player.posZ + 8));
			spawnEntityInWorld(player, world, new EntityLightningBoltByPlayer(player, world, player.posX - 2, player.posY, player.posZ - 8));
			spawnEntityInWorld(player, world, new EntityLightningBoltByPlayer(player, world, player.posX - 3, player.posY, player.posZ + 8));
			spawnEntityInWorld(player, world, new EntityLightningBoltByPlayer(player, world, player.posX - 4, player.posY, player.posZ - 8));

			EntityArcher archer = new EntityArcher(world);

			// TODO gamerforEA code start
			archer.fake.setProfile(player.getGameProfile());
			// TODO gamerforEA code end

			archer.setPosition(player.posX + 8, player.posY + 2, player.posZ - 8);
			spawnEntityInWorld(player, world, archer);

			EntityArcher archer1 = new EntityArcher(world);

			// TODO gamerforEA code start
			archer1.fake.setProfile(player.getGameProfile());
			// TODO gamerforEA code end

			archer1.setPosition(player.posX - 8, player.posY + 2, player.posZ + 8);
			spawnEntityInWorld(player, world, archer1);

			EntityArcher archer2 = new EntityArcher(world);

			// TODO gamerforEA code start
			archer2.fake.setProfile(player.getGameProfile());
			// TODO gamerforEA code end

			archer2.setPosition(player.posX - 8, player.posY + 2, player.posZ - 8);
			spawnEntityInWorld(player, world, archer2);

			EntityArcher archer3 = new EntityArcher(world);

			// TODO gamerforEA code start
			archer3.fake.setProfile(player.getGameProfile());
			// TODO gamerforEA code end

			archer3.setPosition(player.posX + 8, player.posY + 2, player.posZ + 8);
			spawnEntityInWorld(player, world, archer3);
			ElectricItem.manager.use(itemstack, this.lightningCost, player);
			return itemstack;
		}
		else
		{
			spawnEntityInWorld(player, world, new EntityLightningBoltByPlayer(player, world, player.posX, player.posY, player.posZ));
			player.addPotionEffect(new PotionEffect(Potion.harm.getId(), 1, 1));
			return itemstack;
		}
	}

	@Override
	public boolean isRepairable()
	{
		return false;
	}

	@Override
	public int getItemEnchantability()
	{
		if (EMTConfigHandler.enchanting == false)
			return 0;
		else
			return 4;
	}

	@Override
	public boolean isBookEnchantable(ItemStack itemstack1, ItemStack itemstack2)
	{
		return EMTConfigHandler.enchanting;
	}

	/* IC2 API METHODS */

	@Override
	public Item getChargedItem(ItemStack itemStack)
	{
		return this;
	}

	@Override
	public Item getEmptyItem(ItemStack itemStack)
	{
		return this;
	}

	@Override
	public boolean canProvideEnergy(ItemStack itemStack)
	{
		return false;
	}

	@Override
	public double getMaxCharge(ItemStack itemStack)
	{
		return this.maxCharge;
	}

	@Override
	public int getTier(ItemStack itemStack)
	{
		return 2;
	}

	@Override
	public double getTransferLimit(ItemStack itemStack)
	{
		return 100;
	}
}
