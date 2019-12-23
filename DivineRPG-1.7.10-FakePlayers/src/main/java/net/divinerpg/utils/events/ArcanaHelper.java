package net.divinerpg.utils.events;

import com.gamerforea.divinerpg.ModUtils;
import net.divinerpg.DivineRPG;
import net.divinerpg.network.MessageArcanaBar;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;

public class ArcanaHelper implements IExtendedEntityProperties
{

	private final EntityPlayer player;
	public static final String NAME = "Arcana";
	private float barValue;
	public int regenDelay;

	public ArcanaHelper(EntityPlayer player)
	{
		this.player = player;
	}

	@Override
	public void saveNBTData(NBTTagCompound n)
	{
		NBTTagCompound tag = this.player.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
		tag.setFloat("ArcanaBarValue", this.barValue);
		tag.setInteger("ArcanaRegenDelay", this.regenDelay);
		this.player.getEntityData().setTag(EntityPlayer.PERSISTED_NBT_TAG, tag);
	}

	@Override
	public void loadNBTData(NBTTagCompound n)
	{
		NBTTagCompound tag = this.player.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
		if (!tag.hasKey("ArcanaBarValue"))
			return;
		this.barValue = tag.getFloat("ArcanaBarValue");
		this.regenDelay = tag.getInteger("ArcanaRegenDelay");
		this.player.getEntityData().setTag(EntityPlayer.PERSISTED_NBT_TAG, tag);
	}

	public static void addProperties(EntityPlayer player)
	{
		player.registerExtendedProperties(NAME, new ArcanaHelper(player));
	}

	public static ArcanaHelper getProperties(EntityPlayer player)
	{
		return (ArcanaHelper) player.getExtendedProperties(NAME);
	}

	public void updateAllBars()
	{
		int amountToRegen = this.barValue != 200 ? 1 : 0;

		// TODO gamerforEA add sync:boolean parameter
		this.regen(amountToRegen, false);

		if (this.barValue >= 200F)
			this.barValue = 200F;

		// TODO gamerforEA add condition [2]
		if (this.player instanceof EntityPlayerMP && ModUtils.isValidPlayer(this.player))
			DivineRPG.network.sendTo(new MessageArcanaBar(this.barValue, this.regenDelay == 0), (EntityPlayerMP) this.player);
	}

	public boolean useBar(float amount)
	{
		if (this.barValue < amount)
		{
			this.regenDelay = 50;
			return false;
		}
		this.barValue -= amount;
		this.regenDelay = 50;

		// TODO gamerforEA add condition [2]
		if (this.player instanceof EntityPlayerMP && ModUtils.isValidPlayer(this.player))
			DivineRPG.network.sendTo(new MessageArcanaBar(this.barValue, this.regenDelay == 0), (EntityPlayerMP) this.player);

		return true;
	}

	// TODO gamerforEA code start
	public void regen(float amount)
	{
		this.setBarValue(amount, true);
	}
	// TODO gamerforEA code end

	// TODO gamerforEA add sync:boolean parameter
	public void regen(float amount, boolean sync)
	{
		if (this.regenDelay == 0)
			this.barValue += amount;
		else
			this.regenDelay--;

		// TODO gamerforEA add condition [2]
		if (sync && this.player instanceof EntityPlayerMP && ModUtils.isValidPlayer(this.player))
			DivineRPG.network.sendTo(new MessageArcanaBar(this.barValue, this.regenDelay == 0), (EntityPlayerMP) this.player);
	}

	public void forceRegen(float amount)
	{
		this.barValue += amount;

		// TODO gamerforEA add condition [2]
		if (this.player instanceof EntityPlayerMP && ModUtils.isValidPlayer(this.player))
			DivineRPG.network.sendTo(new MessageArcanaBar(this.barValue, this.regenDelay == 0), (EntityPlayerMP) this.player);
	}

	public float getBarValue()
	{
		return this.barValue;
	}

	// TODO gamerforEA code start
	public void setBarValue(float amount)
	{
		this.setBarValue(amount, true);
	}
	// TODO gamerforEA code end

	// TODO gamerforEA add sync:boolean parameter
	public void setBarValue(float amount, boolean sync)
	{
		this.barValue = amount;

		// TODO gamerforEA add condition [2]
		if (sync && this.player instanceof EntityPlayerMP && ModUtils.isValidPlayer(this.player))
			DivineRPG.network.sendTo(new MessageArcanaBar(this.barValue, this.regenDelay == 0), (EntityPlayerMP) this.player);
	}

	public void removeValue(float i)
	{
		this.regenDelay = 50;
		this.barValue -= i;

		// TODO gamerforEA add condition [2]
		if (this.player instanceof EntityPlayerMP && ModUtils.isValidPlayer(this.player))
			DivineRPG.network.sendTo(new MessageArcanaBar(this.barValue, this.regenDelay == 0), (EntityPlayerMP) this.player);
	}

	@Override
	public void init(Entity entity, World world)
	{
	}
}