package crazypants.enderio.machine.killera;

import com.enderio.core.api.common.util.ITankAccess;
import com.enderio.core.client.render.BoundingBox;
import com.enderio.core.common.util.BlockCoord;
import com.enderio.core.common.util.FluidUtil;
import com.enderio.core.common.util.ForgeDirectionOffsets;
import com.enderio.core.common.vecmath.Vector3d;
import com.google.common.collect.Sets;
import com.mojang.authlib.GameProfile;
import cpw.mods.fml.common.eventhandler.Event.Result;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import crazypants.enderio.EnderIO;
import crazypants.enderio.ModObject;
import crazypants.enderio.config.Config;
import crazypants.enderio.machine.AbstractMachineEntity;
import crazypants.enderio.machine.FakePlayerEIO;
import crazypants.enderio.machine.SlotDefinition;
import crazypants.enderio.machine.generator.zombie.IHasNutrientTank;
import crazypants.enderio.machine.generator.zombie.PacketNutrientTank;
import crazypants.enderio.machine.wireless.WirelessChargedLocation;
import crazypants.enderio.network.PacketHandler;
import crazypants.enderio.tool.SmartTank;
import crazypants.enderio.xp.ExperienceContainer;
import crazypants.enderio.xp.IHaveExperience;
import crazypants.enderio.xp.PacketExperianceContainer;
import crazypants.enderio.xp.XpUtil;
import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.entity.living.ZombieEvent.SummonAidEvent;
import net.minecraftforge.fluids.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class TileKillerJoe extends AbstractMachineEntity
		implements IFluidHandler, IEntitySelector, IHaveExperience, ITankAccess, IHasNutrientTank
{

	public static class ZombieCache
	{

		private Set<EntityZombie> cache = Sets.newHashSet();

		@SubscribeEvent
		public void onSummonAid(SummonAidEvent event)
		{
			if (!this.cache.isEmpty() && this.cache.remove(event.getSummoner()))
				event.setResult(Result.DENY);
		}
	}

	public static ZombieCache zCache;

	private static final int IO_MB_TICK = 250;

	protected AxisAlignedBB killBounds;

	private int[] frontFaceAndSides;

	protected AxisAlignedBB hooverBounds;

	protected FakePlayer attackera;

	protected WirelessChargedLocation chargedLocation;

	final SmartTank fuelTank = new SmartTank(EnderIO.fluidNutrientDistillation, FluidContainerRegistry.BUCKET_VOLUME * 2);

	int lastFluidLevelUpdate;

	private boolean tanksDirty;

	private boolean isSwingInProgress;

	private int swingProgressInt;

	private float swingProgress;

	private float prevSwingProgress;

	private final ExperienceContainer xpCon = new ExperienceContainer(XpUtil.getExperienceForLevel(Config.killerJoeMaxXpLevel));

	private boolean hadSword;

	public TileKillerJoe()
	{
		super(new SlotDefinition(1, 0, 0));
		if (zCache == null)
		{
			zCache = new ZombieCache();
			MinecraftForge.EVENT_BUS.register(zCache);
		}
	}

	@Override
	public String getMachineName()
	{
		return ModObject.blockKillerJoe.unlocalisedName;
	}

	@Override
	protected boolean isMachineItemValidForSlot(int i, ItemStack itemstack)
	{
		if (itemstack == null)
			return false;
		return itemstack.getItem() instanceof ItemSword;
	}

	@Override
	public boolean isActive()
	{
		return false;
	}

	@Override
	public void doUpdate()
	{
		this.updateArmSwingProgress();
		this.hooverXP();
		if (!this.worldObj.isRemote)
		{
			this.getAttackera().onUpdate();
			if (this.inventory[0] != null != this.hadSword)
			{
				this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
				this.hadSword = this.inventory[0] != null;
			}
		}
		super.doUpdate();
	}

	@Override
	public ExperienceContainer getContainer()
	{
		return this.xpCon;
	}

	private static final int[] slots = new int[1];

	@Override
	public int[] getAccessibleSlotsFromSide(int side)
	{
		return slots;
	}

	@Override
	public boolean canExtractItem(int slot, ItemStack itemstack, int side)
	{
		if (this.isSideDisabled(side))
			return false;
		if (this.inventory[slot] == null || this.inventory[slot].stackSize < itemstack.stackSize)
			return false;
		return itemstack.getItem() == this.inventory[slot].getItem();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected boolean processTasks(boolean redstoneCheckPassed)
	{

		if (!this.shouldDoWorkThisTick(10))
			return false;

		if (this.tanksDirty)
		{
			PacketHandler.sendToAllAround(new PacketNutrientTank(this), this);
			this.tanksDirty = false;
		}
		if (this.xpCon.isDirty())
		{
			PacketHandler.sendToAllAround(new PacketExperianceContainer(this), this);
			this.xpCon.setDirty(false);
		}

		if (!redstoneCheckPassed)
			return false;

		if (this.fuelTank.getFluidAmount() < this.getActivationAmount())
			return false;

		if (this.getStackInSlot(0) == null)
			return false;

		List<EntityLivingBase> entsInBounds = this.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, this.getKillBounds());
		if (!entsInBounds.isEmpty())
			for (EntityLivingBase ent : entsInBounds)
			{
				if (!ent.isDead && ent.deathTime <= 0 && !ent.isEntityInvulnerable() && ent.hurtResistantTime == 0)
				{
					if (ent instanceof EntityPlayer && ((EntityPlayer) ent).capabilities.disableDamage)
						continue; //Ignore players in creative, can't damage them;
					boolean togglePvp = false;
					if (ent instanceof EntityPlayer && !MinecraftServer.getServer().isPVPEnabled())
						if (Config.killerPvPoffDisablesSwing)
							continue;
						else if (Config.killerPvPoffIsIgnored)
							togglePvp = true;
					if (Config.killerJoeMustSee && !this.canJoeSee(ent))
						continue;

					if (ent instanceof EntityZombie)
						zCache.cache.add((EntityZombie) ent);
					FakePlayer fakee = this.getAttackera();
					fakee.setCurrentItemOrArmor(0, this.getStackInSlot(0));
					try
					{
						if (togglePvp)
							MinecraftServer.getServer().setAllowPvp(true);
						fakee.attackTargetEntityWithCurrentItem(ent);
					}
					finally
					{
						if (togglePvp)
							MinecraftServer.getServer().setAllowPvp(false);
					}
					this.useNutrient();
					this.swingWeapon();
					if (this.getStackInSlot(0).stackSize <= 0 || fakee.getCurrentEquippedItem() == null)
						this.setInventorySlotContents(0, null);
					return false;
				}
			}
		return false;
	}

	int getActivationAmount()
	{
		return (int) (this.fuelTank.getCapacity() * 0.7f);
	}

	private boolean canJoeSee(EntityLivingBase ent)
	{
		Vec3 entPos = Vec3.createVectorHelper(ent.posX, ent.posY + (double) ent.getEyeHeight(), ent.posZ);
		for (int facing : this.frontFaceAndSides)
		{
			if (this.worldObj.rayTraceBlocks(Vec3.createVectorHelper(this.xCoord + faceMidPoints[facing][0], this.yCoord + faceMidPoints[facing][1], this.zCoord + faceMidPoints[facing][2]), entPos) == null)
				return true;
		}
		return false;
	}

	@Override
	public void setFacing(short facing)
	{
		super.setFacing(facing);
		this.frontFaceAndSides = new int[] { this.facing, ForgeDirection.ROTATION_MATRIX[0][this.facing], ForgeDirection.ROTATION_MATRIX[1][this.facing] };
	}

	private static final double[][] faceMidPoints = { { 0.5D, 0.0D, 0.5D }, { 0.5D, 1.0D, 0.5D }, { 0.5D, 0.5D, 0.0D }, { 0.5D, 0.5D, 1.0D }, { 0.0D, 0.5D, 0.5D }, { 1.0D, 0.5D, 0.5D } };

	//-------------------------------  XP

	public ExperienceContainer getXpContainer()
	{
		return this.xpCon;
	}

	private void hooverXP()
	{

		double maxDist = Config.killerJoeHooverXpLength;

		List<EntityXPOrb> xp = this.worldObj.selectEntitiesWithinAABB(EntityXPOrb.class, this.getHooverBounds(), this);

		for (EntityXPOrb entity : xp)
		{
			double xDist = this.xCoord + 0.5D - entity.posX;
			double yDist = this.yCoord + 0.5D - entity.posY;
			double zDist = this.zCoord + 0.5D - entity.posZ;

			double totalDistance = Math.sqrt(xDist * xDist + yDist * yDist + zDist * zDist);

			if (totalDistance < 1.5)
				this.hooverXP(entity);
			else
			{
				double d = 1 - Math.max(0.1, totalDistance) / maxDist;
				double speed = 0.01 + d * 0.02;

				entity.motionX += xDist / totalDistance * speed;
				entity.motionZ += zDist / totalDistance * speed;
				entity.motionY += yDist / totalDistance * speed;
				if (yDist > 0.5)
					entity.motionY = 0.12;

			}
		}
	}

	private void hooverXP(EntityXPOrb entity)
	{
		if (!this.worldObj.isRemote)
			if (!entity.isDead)
			{
				this.xpCon.addExperience(entity.getXpValue());
				entity.setDead();
			}
	}

	@Override
	public boolean isEntityApplicable(Entity arg0)
	{
		return true;
	}

	//------------------------------- Weapon stuffs

	void swingWeapon()
	{
		if (this.getStackInSlot(0) == null)
			return;
		if (!this.isSwingInProgress || this.swingProgressInt >= this.getArmSwingAnimationEnd() / 2 || this.swingProgressInt < 0)
		{
			this.swingProgressInt = -1;
			this.isSwingInProgress = true;
			if (this.worldObj instanceof WorldServer)
				PacketHandler.sendToAllAround(new PacketSwing(this), this);
		}
	}

	float getSwingProgress(float p_70678_1_)
	{
		float f1 = this.swingProgress - this.prevSwingProgress;

		if (f1 < 0.0F)
			++f1;

		return this.prevSwingProgress + f1 * p_70678_1_;
	}

	private void updateArmSwingProgress()
	{

		this.prevSwingProgress = this.swingProgress;

		int i = this.getArmSwingAnimationEnd();
		if (this.isSwingInProgress)
		{
			++this.swingProgressInt;
			if (this.swingProgressInt >= i)
			{
				this.swingProgressInt = 0;
				this.isSwingInProgress = false;
			}
		}
		else
			this.swingProgressInt = 0;
		this.swingProgress = (float) this.swingProgressInt / (float) i;
	}

	private int getArmSwingAnimationEnd()
	{
		return 6;
	}

	FakePlayer getAttackera()
	{
		if (this.attackera == null)
			this.attackera = new Attackera();
		return this.attackera;
	}

	WirelessChargedLocation getChargedLocation()
	{
		if (this.chargedLocation == null)
			this.chargedLocation = new WirelessChargedLocation(this);
		return this.chargedLocation;
	}

	private AxisAlignedBB getKillBounds()
	{
		if (this.killBounds == null)
		{
			BoundingBox bb = new BoundingBox(this.getLocation());
			Vector3d min = bb.getMin();
			Vector3d max = bb.getMax();
			max.y += Config.killerJoeAttackHeight;
			min.y -= Config.killerJoeAttackHeight;

			ForgeDirection facingDir = ForgeDirection.getOrientation(this.facing);
			if (ForgeDirectionOffsets.isPositiveOffset(facingDir))
			{
				max.add(ForgeDirectionOffsets.offsetScaled(facingDir, Config.killerJoeAttackLength));
				min.add(ForgeDirectionOffsets.forDir(facingDir));
			}
			else
			{
				min.add(ForgeDirectionOffsets.offsetScaled(facingDir, Config.killerJoeAttackLength));
				max.add(ForgeDirectionOffsets.forDir(facingDir));

			}
			if (facingDir.offsetX == 0)
			{
				min.x -= Config.killerJoeAttackWidth;
				max.x += Config.killerJoeAttackWidth;
			}
			else
			{
				min.z -= Config.killerJoeAttackWidth;
				max.z += Config.killerJoeAttackWidth;
			}
			this.killBounds = AxisAlignedBB.getBoundingBox(min.x, min.y, min.z, max.x, max.y, max.z);
		}
		return this.killBounds;
	}

	private AxisAlignedBB getHooverBounds()
	{
		if (this.hooverBounds == null)
		{
			BoundingBox bb = new BoundingBox(this.getLocation());
			Vector3d min = bb.getMin();
			Vector3d max = bb.getMax();
			max.y += Config.killerJoeAttackHeight;
			min.y -= Config.killerJoeAttackHeight;

			ForgeDirection facingDir = ForgeDirection.getOrientation(this.facing);
			if (ForgeDirectionOffsets.isPositiveOffset(facingDir))
			{
				max.add(ForgeDirectionOffsets.offsetScaled(facingDir, Config.killerJoeHooverXpLength));
				min.add(ForgeDirectionOffsets.forDir(facingDir));
			}
			else
			{
				min.add(ForgeDirectionOffsets.offsetScaled(facingDir, Config.killerJoeHooverXpLength));
				max.add(ForgeDirectionOffsets.forDir(facingDir));

			}
			if (facingDir.offsetX == 0)
			{
				min.x -= Config.killerJoeHooverXpWidth * 2;
				max.x += Config.killerJoeHooverXpWidth * 2;
			}
			else
			{
				min.z -= Config.killerJoeHooverXpWidth * 2;
				max.z += Config.killerJoeHooverXpWidth * 2;
			}
			this.hooverBounds = AxisAlignedBB.getBoundingBox(min.x, min.y, min.z, max.x, max.y, max.z);
		}
		return this.hooverBounds;
	}

	//-------------------------------  Fluid Stuff

	private void useNutrient()
	{
		this.fuelTank.drain(Config.killerJoeNutrientUsePerAttackMb, true);
		this.tanksDirty = true;
	}

	@Override
	protected boolean doPull(ForgeDirection dir)
	{
		boolean res = super.doPull(dir);
		//    BlockCoord loc = getLocation().getLocation(dir);
		//    IFluidHandler target = FluidUtil.getFluidHandler(worldObj, loc);
		//    if(target != null) {
		//      FluidTankInfo[] infos = target.getTankInfo(dir.getOpposite());
		//      if(infos != null) {
		//        for (FluidTankInfo info : infos) {
		//          if(info.fluid != null && info.fluid.amount > 0) {
		//            if(canFill(dir, info.fluid.getFluid())) {
		//              FluidStack canPull = info.fluid.copy();
		//              canPull.amount = Math.min(IO_MB_TICK, canPull.amount);
		//              FluidStack drained = target.drain(dir.getOpposite(), canPull, false);
		//              if(drained != null && drained.amount > 0) {
		//                int filled = fill(dir, drained, false);
		//                if(filled > 0) {
		//                  drained = target.drain(dir.getOpposite(), filled, true);
		//                  fill(dir, drained, true);
		//                  return res;
		//                }
		//              }
		//            }
		//          }
		//        }
		//      }
		//    }
		FluidUtil.doPull(this, dir, IO_MB_TICK);
		return res;
	}

	@Override
	protected boolean doPush(ForgeDirection dir)
	{
		boolean res = super.doPush(dir);
		BlockCoord loc = this.getLocation().getLocation(dir);
		IFluidHandler target = FluidUtil.getFluidHandler(this.worldObj, loc);
		if (target != null)
		{
			FluidStack canDrain = this.drain(dir, IO_MB_TICK, false);
			if (canDrain != null && canDrain.amount > 0)
			{
				int drained = target.fill(dir.getOpposite(), canDrain, true);
				if (drained > 0)
					this.drain(dir, drained, true);
			}
		}
		return res;
	}

	@Override
	public int fill(ForgeDirection from, FluidStack resource, boolean doFill)
	{
		int res = this.fuelTank.fill(resource, doFill);
		if (res > 0 && doFill)
			this.tanksDirty = true;
		return res;
	}

	@Override
	public boolean canFill(ForgeDirection from, Fluid fluid)
	{
		return this.fuelTank.canFill(fluid);
	}

	@Override
	public FluidTankInfo[] getTankInfo(ForgeDirection from)
	{
		return new FluidTankInfo[] { this.fuelTank.getInfo() };
	}

	@Override
	public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain)
	{
		return this.xpCon.drain(from, resource, doDrain);
	}

	@Override
	public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain)
	{
		return this.xpCon.drain(from, maxDrain, doDrain);
	}

	@Override
	public boolean canDrain(ForgeDirection from, Fluid fluid)
	{
		return this.xpCon.canDrain(from, fluid);
	}

	//-------------------------------  Save / Load

	@Override
	public void readCommon(NBTTagCompound nbtRoot)
	{
		super.readCommon(nbtRoot);
		this.fuelTank.readCommon("fuelTank", nbtRoot);
		this.xpCon.readFromNBT(nbtRoot);
	}

	@Override
	public void writeCommon(NBTTagCompound nbtRoot)
	{
		super.writeCommon(nbtRoot);
		this.fuelTank.writeCommon("fuelTank", nbtRoot);
		this.xpCon.writeToNBT(nbtRoot);
	}

	private static final UUID uuid = UUID.fromString("3baa66fa-a69a-11e4-89d3-123b93f75cba");
	private static final GameProfile DUMMY_PROFILE = new GameProfile(uuid, "[EioKillera]");

	// TODO gamerforEA code start
	private GameProfile getProfile()
	{
		GameProfile profile = this.fake.getProfile();
		return profile != null && profile.isComplete() ? profile : DUMMY_PROFILE;
	}
	// TODO gamerforEA code end

	private class Attackera extends FakePlayerEIO
	{
		ItemStack prevWeapon;

		public Attackera()
		{
			// TODO gamerforEA code replace, old code:
			// super(TileKillerJoe.this.getWorldObj(), TileKillerJoe.this.getLocation(), DUMMY_PROFILE);
			super(TileKillerJoe.this.getWorldObj(), TileKillerJoe.this.getLocation(), TileKillerJoe.this.getProfile());
			// TODO gamerforEA code end
		}

		@Override
		public void onUpdate()
		{

			this.setCurrentItemOrArmor(0, TileKillerJoe.this.getStackInSlot(0));

			ItemStack prev = this.prevWeapon;
			ItemStack cur = this.getCurrentEquippedItem();
			if (!ItemStack.areItemStacksEqual(cur, prev))
			{
				if (prev != null)
					this.getAttributeMap().removeAttributeModifiers(prev.getAttributeModifiers());

				if (cur != null)
					this.getAttributeMap().applyAttributeModifiers(cur.getAttributeModifiers());
				this.prevWeapon = cur == null ? null : cur.copy();
			}

			TileKillerJoe.this.getChargedLocation().chargeItems(this.inventory.mainInventory);
		}
	}

	@Override
	public FluidTank getInputTank(FluidStack forFluidType)
	{
		if (forFluidType != null && forFluidType.getFluid() == EnderIO.fluidNutrientDistillation)
			return this.fuelTank;
		/*
		 * if (forFluidType != null && forFluidType.getFluid() ==
		 * EnderIO.fluidXpJuice) { return xpCon; }
		 */
		return null;
	}

	@Override
	public FluidTank[] getOutputTanks()
	{
		return new FluidTank[] { this.xpCon /* , fuelTank */ };
	}

	@Override
	public void setTanksDirty()
	{
		this.tanksDirty = true;
	}

	@Override
	public SmartTank getNutrientTank()
	{
		return this.fuelTank;
	}

}
