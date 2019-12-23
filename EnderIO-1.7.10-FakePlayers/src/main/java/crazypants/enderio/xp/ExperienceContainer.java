package crazypants.enderio.xp;

import crazypants.enderio.EnderIO;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidTankInfo;

import java.security.InvalidParameterException;

public class ExperienceContainer extends FluidTank
{
	// Note: We extend FluidTank instead of implementing IFluidTank because it has
	// some methods we need.

	private int experienceLevel;
	private float experience;
	private int experienceTotal;
	private boolean xpDirty;
	private final int maxXp;

	public ExperienceContainer()
	{
		this(Integer.MAX_VALUE);
	}

	public ExperienceContainer(int maxStored)
	{
		super(null, 0);
		this.maxXp = maxStored;
	}

	public int getMaximumExperiance()
	{
		return this.maxXp;
	}

	public int getExperienceLevel()
	{
		return this.experienceLevel;
	}

	public float getExperience()
	{
		return this.experience;
	}

	public int getExperienceTotal()
	{
		return this.experienceTotal;
	}

	public boolean isDirty()
	{
		return this.xpDirty;
	}

	public void setDirty(boolean isDirty)
	{
		this.xpDirty = isDirty;
	}

	public void set(ExperienceContainer xpCon)
	{
		this.experienceTotal = xpCon.experienceTotal;
		this.experienceLevel = xpCon.experienceLevel;
		this.experience = xpCon.experience;
	}

	public int addExperience(int xpToAdd)
	{
		int j = this.maxXp - this.experienceTotal;

		// TODO gamerforEA code start
		if (j == 0)
			return 0;
		// TODO gamerforEA code end

		if (xpToAdd > j)
			xpToAdd = j;

		this.experience += (float) xpToAdd / (float) this.getXpBarCapacity();
		this.experienceTotal += xpToAdd;
		for (; this.experience >= 1.0F; this.experience /= this.getXpBarCapacity())
		{
			this.experience = (this.experience - 1.0F) * this.getXpBarCapacity();
			this.experienceLevel++;
		}
		this.xpDirty = true;
		return xpToAdd;
	}

	private int getXpBarCapacity()
	{
		return XpUtil.getXpBarCapacity(this.experienceLevel);
	}

	public int getXpBarScaled(int scale)
	{
		return (int) (this.experience * scale);

	}

	public void givePlayerXp(EntityPlayer player, int levels)
	{
		for (int i = 0; i < levels && this.experienceTotal > 0; i++)
		{
			this.givePlayerXpLevel(player);
		}
	}

	public void givePlayerXpLevel(EntityPlayer player)
	{
		// TODO gamerforEA code start
		if (this.experienceTotal <= 0)
			return;
		// TODO gamerforEA code end

		int currentXP = XpUtil.getPlayerXP(player);

		// TODO gamerforEA code start
		if (currentXP == Integer.MAX_VALUE)
			return;
		// TODO gamerforEA code end

		int nextLevelXP = XpUtil.getExperienceForLevel(player.experienceLevel + 1) + 1;

		// TODO gamerforEA code start
		if (nextLevelXP <= 0)
			return;
		// TODO gamerforEA code end

		int requiredXP = nextLevelXP - currentXP;

		requiredXP = Math.min(this.experienceTotal, requiredXP);
		player.addExperience(requiredXP);

		int newXp = this.experienceTotal - requiredXP;
		this.experience = 0;
		this.experienceLevel = 0;
		this.experienceTotal = 0;
		this.addExperience(newXp);
	}

	public void drainPlayerXpToReachContainerLevel(EntityPlayer player, int level)
	{
		int targetXP = XpUtil.getExperienceForLevel(level);
		int requiredXP = targetXP - this.experienceTotal;
		if (requiredXP <= 0)
			return;
		int drainXP = Math.min(requiredXP, XpUtil.getPlayerXP(player));
		this.addExperience(drainXP);
		XpUtil.addPlayerXP(player, -drainXP);
	}

	public void drainPlayerXpToReachPlayerLevel(EntityPlayer player, int level)
	{
		int targetXP = XpUtil.getExperienceForLevel(level);
		int drainXP = XpUtil.getPlayerXP(player) - targetXP;
		if (drainXP <= 0)
			return;
		drainXP = this.addExperience(drainXP);
		if (drainXP > 0)
			XpUtil.addPlayerXP(player, -drainXP);
	}

	public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain)
	{
		if (resource == null || !this.canDrain(from, resource.getFluid()))
			return null;
		return this.drain(from, resource.amount, doDrain);
	}

	public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain)
	{
		if (EnderIO.fluidXpJuice == null)
			return null;
		int available = this.getFluidAmount();
		int canDrain = Math.min(available, maxDrain);
		final int xpAskedToExtract = XpUtil.liquidToExperience(canDrain);
		// only return multiples of 1 XP (20mB) to avoid duping XP when being asked
		// for low values (like 10mB/t)
		final int fluidToExtract = XpUtil.experienceToLiquid(xpAskedToExtract);
		final int xpToExtract = XpUtil.liquidToExperience(fluidToExtract);
		if (doDrain)
		{
			int newXp = this.experienceTotal - xpToExtract;
			this.experience = 0;
			this.experienceLevel = 0;
			this.experienceTotal = 0;
			this.addExperience(newXp);
		}
		return new FluidStack(EnderIO.fluidXpJuice, fluidToExtract);
	}

	public boolean canFill(ForgeDirection from, Fluid fluid)
	{
		return fluid != null && EnderIO.fluidXpJuice != null && fluid.getID() == EnderIO.fluidXpJuice.getID();
	}

	public int fill(ForgeDirection from, FluidStack resource, boolean doFill)
	{
		if (resource == null)
			return 0;
		if (resource.amount <= 0)
			return 0;
		if (!this.canFill(from, resource.getFluid()))
			return 0;
		//need to do these calcs in XP instead of fluid space to avoid type overflows
		int xp = XpUtil.liquidToExperience(resource.amount);
		int xpSpace = this.getMaximumExperiance() - this.getExperienceTotal();
		int canFillXP = Math.min(xp, xpSpace);
		if (canFillXP <= 0)
			return 0;
		if (doFill)
			this.addExperience(canFillXP);
		return XpUtil.experienceToLiquid(canFillXP);
	}

	public boolean canDrain(ForgeDirection from, Fluid fluid)
	{
		return fluid != null && EnderIO.fluidXpJuice != null && fluid.getID() == EnderIO.fluidXpJuice.getID();
	}

	public FluidTankInfo[] getTankInfo(ForgeDirection from)
	{
		if (EnderIO.fluidXpJuice == null)
			return new FluidTankInfo[0];
		return new FluidTankInfo[] { new FluidTankInfo(new FluidStack(EnderIO.fluidXpJuice, this.getFluidAmount()), this.getCapacity()) };
	}

	@Override
	public int getCapacity()
	{
		if (this.maxXp == Integer.MAX_VALUE)
			return Integer.MAX_VALUE;
		return XpUtil.experienceToLiquid(this.maxXp);
	}

	@Override
	public int getFluidAmount()
	{
		return XpUtil.experienceToLiquid(this.experienceTotal);
	}

	@Override
	public FluidTank readFromNBT(NBTTagCompound nbtRoot)
	{
		this.experienceLevel = nbtRoot.getInteger("experienceLevel");
		this.experienceTotal = nbtRoot.getInteger("experienceTotal");
		this.experience = nbtRoot.getFloat("experience");

		// TODO gamerforEA code start
		if (this.experienceLevel > this.maxXp)
		{
			this.experience = 0;
			this.experienceLevel = 0;
			this.experienceTotal = 0;
			this.addExperience(this.maxXp);
		}
		// TODO gamerforEA code end

		return this;
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbtRoot)
	{
		nbtRoot.setInteger("experienceLevel", this.experienceLevel);
		nbtRoot.setInteger("experienceTotal", this.experienceTotal);
		nbtRoot.setFloat("experience", this.experience);
		return nbtRoot;
	}

	public void toBytes(ByteBuf buf)
	{
		buf.writeInt(this.experienceTotal);
		buf.writeInt(this.experienceLevel);
		buf.writeFloat(this.experience);
	}

	public void fromBytes(ByteBuf buf)
	{
		this.experienceTotal = buf.readInt();
		this.experienceLevel = buf.readInt();
		this.experience = buf.readFloat();
	}

	@Override
	public FluidStack getFluid()
	{
		return new FluidStack(EnderIO.fluidXpJuice, this.getFluidAmount());
	}

	@Override
	public FluidTankInfo getInfo()
	{
		return this.getTankInfo(ForgeDirection.UNKNOWN)[0];
	}

	@Override
	public int fill(FluidStack resource, boolean doFill)
	{
		return this.fill(ForgeDirection.UNKNOWN, resource, doFill);
	}

	@Override
	public FluidStack drain(int maxDrain, boolean doDrain)
	{
		return this.drain(ForgeDirection.UNKNOWN, maxDrain, doDrain);
	}

	@Override
	public void setFluid(FluidStack fluid)
	{
		this.experience = 0;
		this.experienceLevel = 0;
		this.experienceTotal = 0;
		if (fluid != null && fluid.getFluid() != null)
			if (EnderIO.fluidXpJuice == fluid.getFluid())
				this.addExperience(XpUtil.liquidToExperience(fluid.amount));
			else
				throw new InvalidParameterException(fluid.getFluid() + " is no XP juice");
		this.xpDirty = true;
	}

	@Override
	public void setCapacity(int capacity)
	{
		throw new InvalidParameterException();
	}

}
