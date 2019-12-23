package emt.item.tool;

import com.gamerforea.emt.EntityLightningBoltByPlayer;
import com.gamerforea.emt.EventConfig;
import com.gamerforea.eventhelper.util.EventUtils;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import emt.EMT;
import emt.util.EMTTextHelper;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import thaumcraft.api.IRepairable;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class ItemThorHammer extends ItemSword implements IRepairable
{
	// TODO gamerforEA code start
	private final Cache<EntityPlayer, Object> cache;
	// TODO gamerforEA code end

	public ItemThorHammer()
	{
		super(ToolMaterial.EMERALD);
		this.setCreativeTab(EMT.TAB);
		this.setMaxDamage(2000);

		// TODO gamerforEA code start
		this.cache = CacheBuilder.newBuilder().weakKeys().expireAfterWrite(EventConfig.thorHammerCooldown, TimeUnit.SECONDS).build();
		// TODO gamerforEA code end
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void registerIcons(IIconRegister iconRegister)
	{
		this.itemIcon = iconRegister.registerIcon(EMT.TEXTURE_PATH + ":hammer/thorhammer");
	}

	@Override
	// TODO gamerforEA use EntityLightningBoltByPlayer
	public ItemStack onItemRightClick(ItemStack itemstack, World world, EntityPlayer player)
	{
		player.swingItem();
		float f = 1.0F;
		float f1 = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * f;
		float f2 = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * f;
		double d = player.prevPosX + (player.posX - player.prevPosX) * f;
		double d1 = player.prevPosY + (player.posY - player.prevPosY) * f + 1.6200000000000001D - player.yOffset;
		double d2 = player.prevPosZ + (player.posZ - player.prevPosZ) * f;
		Vec3 vec3d = Vec3.createVectorHelper(d, d1, d2);
		float f3 = MathHelper.cos(-f2 * 0.01745329F - 3.141593F);
		float f4 = MathHelper.sin(-f2 * 0.01745329F - 3.141593F);
		float f5 = -MathHelper.cos(-f1 * 0.01745329F);
		float f6 = MathHelper.sin(-f1 * 0.01745329F);
		float f7 = f4 * f5;
		float f8 = f6;
		float f9 = f3 * f5;
		double d3 = 5000D;
		Vec3 vec3d1 = vec3d.addVector(f7 * d3, f8 * d3, f9 * d3);
		MovingObjectPosition mop = player.worldObj.rayTraceBlocks(vec3d, vec3d1, true);
		if (mop == null)
			return itemstack;

		// TODO gamerforEA code start
		if (this.cache.getIfPresent(player) != null)
			return itemstack;
		this.cache.put(player, new Object());
		// TODO gamerforEA code end

		if (mop.typeOfHit == MovingObjectType.BLOCK)
		{
			int i = mop.blockX;
			int j = mop.blockY;
			int k = mop.blockZ;

			// TODO gamerforEA code start
			if (!EventUtils.cantBreak(player, i, j, k))
				// TODO gamerforEA code end
				world.spawnEntityInWorld(new EntityLightningBoltByPlayer(player, world, i, j, k));
		}
		else if (mop.typeOfHit == MovingObjectType.ENTITY)
		{
			Entity entityhit = mop.entityHit;
			double x = entityhit.posX;
			double y = entityhit.posY;
			double z = entityhit.posZ;

			// TODO gamerforEA code start
			if (!EventUtils.cantBreak(player, x, z, y))
				// TODO gamerforEA code end
				world.spawnEntityInWorld(new EntityLightningBoltByPlayer(player, world, x, y, z));
		}
		if (player.capabilities.isCreativeMode)
			return itemstack;
		else
		{
			itemstack.damageItem(20, player);
			return itemstack;
		}
	}

	@Override
	public boolean hitEntity(ItemStack itemstack, EntityLivingBase entityliving, EntityLivingBase attacker)
	{
		// TODO gamerforEA code start
		if (attacker != null && EventUtils.cantDamage(attacker, entityliving))
			return true;
		// TODO gamerforEA code end

		entityliving.attackEntityFrom(DamageSource.causePlayerDamage((EntityPlayer) attacker), 12F);
		itemstack.damageItem(1, attacker);
		return true;
	}

	@Override
	public void addInformation(ItemStack stack, EntityPlayer par2EntityPlayer, List list, boolean par4)
	{
		list.add(EMTTextHelper.localize("tooltip.EMT.hammer.broken.The Hammer of Thor"));
	}
}
