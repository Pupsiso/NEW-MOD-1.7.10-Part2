package emt.entity;

import com.gamerforea.emt.ModUtils;
import com.gamerforea.eventhelper.fake.FakePlayerContainer;
import com.gamerforea.eventhelper.fake.FakePlayerContainerEntity;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

public class EntityShield extends Entity
{
	public EntityPlayer owner;

	public boolean needCheck = true;

	// TODO gamerforEA code start
	public final FakePlayerContainer fake = new FakePlayerContainerEntity(ModUtils.profile, this);
	// TODO gamerforEA code end

	public EntityShield(World world)
	{
		super(world);
		this.ignoreFrustumCheck = true;
	}

	public EntityShield(World world, EntityPlayer player)
	{
		super(world);
		this.setSize(4, 4);
		this.owner = player;
		this.dataWatcher.updateObject(11, this.owner.getDisplayName());
		this.setPosition(player.posX, player.posY, player.posZ);
		this.ignoreFrustumCheck = true;

		// TODO gamerforEA code start
		this.fake.setRealPlayer(player);
		// TODO gamerforEA code end
	}

	@Override
	protected void entityInit()
	{
		this.dataWatcher.addObject(11, "");
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound nbt)
	{
		// TODO gamerforEA code start
		this.fake.readFromNBT(nbt);
		// TODO gamerforEA code end
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound nbt)
	{
		// TODO gamerforEA code start
		this.fake.writeToNBT(nbt);
		// TODO gamerforEA code end
	}

	@Override
	public AxisAlignedBB getBoundingBox()
	{
		return this.boundingBox;
	}

	@Override
	public boolean canBePushed()
	{
		return true;
	}

	@Override
	public boolean canBeCollidedWith()
	{
		return true;
	}

	@Override
	public void setPosition(double x, double y, double z)
	{
		this.posX = x;
		this.posY = y + 0.5f;
		this.posZ = z;
		float f = this.width / 2.0F;
		float f1 = this.height;
		this.boundingBox.setBounds(x - f, y - this.yOffset - 2 + this.ySize, z - f, x + f, y - this.yOffset - 2 + this.ySize + f1, z + f);
	}

	@Override
	public void onUpdate()
	{
		super.onUpdate();

		if (this.needCheck && this.owner == null)
		{
			this.owner = this.worldObj.getPlayerEntityByName(this.dataWatcher.getWatchableObjectString(11));
			this.needCheck = false;
		}
		if (!this.needCheck && this.owner == null)
		{
			this.setDead();
			return;
		}

		if (!this.worldObj.isRemote && this.owner != null)
		{
			this.setPosition(this.owner.posX, this.owner.posY, this.owner.posZ);
			if (!this.owner.isUsingItem())
				this.setDead();
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public int getBrightnessForRender(float p_70070_1_)
	{
		return 240;
	}

	@Override
	public void applyEntityCollision(Entity entity)
	{
		if (entity.riddenByEntity != this && entity.ridingEntity != this)
		{
			// TODO gamerforEA code start
			if (this.fake.cantDamage(entity))
				return;
			// TODO gamerforEA code end

			double ePosX = entity.posX - this.posX;
			double ePosZ = entity.posZ - this.posZ;
			entity.addVelocity(ePosX / 5D, 0.0D, ePosZ / 5D);
		}
	}

	@SideOnly(Side.CLIENT)
	@Override
	public boolean isInRangeToRender3d(double x, double y, double z)
	{
		return true;
	}
}
