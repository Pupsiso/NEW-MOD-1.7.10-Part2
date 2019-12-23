package crazypants.enderio.machine.spawner;

import com.gamerforea.enderio.EventConfig;
import crazypants.enderio.EnderIO;
import crazypants.enderio.ModObject;
import crazypants.enderio.config.Config;
import crazypants.enderio.machine.*;
import crazypants.enderio.power.BasicCapacitor;
import crazypants.enderio.power.Capacitors;
import crazypants.enderio.power.ICapacitor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;

public class TilePoweredSpawner extends AbstractPoweredTaskEntity
{

	public static final int MIN_SPAWN_DELAY_BASE = Config.poweredSpawnerMinDelayTicks;
	public static final int MAX_SPAWN_DELAY_BASE = Config.poweredSpawnerMaxDelayTicks;

	public static final int POWER_PER_TICK_ONE = Config.poweredSpawnerLevelOnePowerPerTickRF;
	private static final BasicCapacitor CAP_ONE = new BasicCapacitor((int) (POWER_PER_TICK_ONE * 1.25), Capacitors.BASIC_CAPACITOR.capacitor.getMaxEnergyStored());

	public static final int POWER_PER_TICK_TWO = Config.poweredSpawnerLevelTwoPowerPerTickRF;
	private static final BasicCapacitor CAP_TWO = new BasicCapacitor((int) (POWER_PER_TICK_TWO * 1.25), Capacitors.ACTIVATED_CAPACITOR.capacitor.getMaxEnergyStored());

	public static final int POWER_PER_TICK_THREE = Config.poweredSpawnerLevelThreePowerPerTickRF;
	private static final BasicCapacitor CAP_THREE = new BasicCapacitor((int) (POWER_PER_TICK_THREE * 1.25), Capacitors.ENDER_CAPACITOR.capacitor.getMaxEnergyStored());

	public static final int MIN_PLAYER_DISTANCE = Config.poweredSpawnerMaxPlayerDistance;
	public static final boolean USE_VANILLA_SPAWN_CHECKS = Config.poweredSpawnerUseVanillaSpawChecks;

	private static final String NULL_ENTITY_NAME = "None";

	private String entityTypeName;
	private boolean isSpawnMode = true;
	private int powerUsePerTick;
	private int remainingSpawnTries;

	public TilePoweredSpawner()
	{
		super(new SlotDefinition(1, 1, 1));
		this.entityTypeName = NULL_ENTITY_NAME;
	}

	public boolean isSpawnMode()
	{
		return this.isSpawnMode;
	}

	public void setSpawnMode(boolean isSpawnMode)
	{
		if (isSpawnMode != this.isSpawnMode)
			this.currentTask = null;
		this.isSpawnMode = isSpawnMode;
	}

	@Override
	protected void taskComplete()
	{
		super.taskComplete();
		if (this.isSpawnMode)
		{
			this.remainingSpawnTries = Config.poweredSpawnerSpawnCount + Config.poweredSpawnerMaxSpawnTries;
			for (int i = 0; i < Config.poweredSpawnerSpawnCount && this.remainingSpawnTries > 0; ++i)
			{
				if (!this.trySpawnEntity())
					break;
			}
		}
		else
		{
			if (this.getStackInSlot(0) == null || this.getStackInSlot(1) != null || !this.hasEntityName())
				return;

			String entityName = this.getEntityName();

			// TODO gamerforEA code start
			if (EventConfig.spawnerDisableWitherSkeletons && EntityList.stringToClassMapping.get(entityName) == EntitySkeleton.class)
				return;
			// TODO gamerforEA code end

			ItemStack res = EnderIO.itemSoulVessel.createVesselWithEntityStub(entityName);
			this.decrStackSize(0, 1);
			this.setInventorySlotContents(1, res);
		}
	}

	@Override
	public void onCapacitorTypeChange()
	{
		ICapacitor refCap;
		int basePowerUse;

		// TODO gamerforEA code start
		if (EventConfig.spawnerIgnoreCapacitorType)
		{
			refCap = CAP_ONE;
			basePowerUse = POWER_PER_TICK_ONE;
		}
		else
			// TODO gamerforEA code end
			switch (this.getCapacitorType())
			{
				default:
				case BASIC_CAPACITOR:
					refCap = CAP_ONE;
					basePowerUse = POWER_PER_TICK_ONE;
					break;
				case ACTIVATED_CAPACITOR:
					refCap = CAP_TWO;
					basePowerUse = POWER_PER_TICK_TWO;
					break;
				case ENDER_CAPACITOR:
					refCap = CAP_THREE;
					basePowerUse = POWER_PER_TICK_THREE;
					break;
			}

		double multiplier = PoweredSpawnerConfig.getInstance().getCostMultiplierFor(this.getEntityName());
		this.setCapacitor(new BasicCapacitor((int) (refCap.getMaxEnergyExtracted() * multiplier), refCap.getMaxEnergyStored()));
		this.powerUsePerTick = (int) Math.ceil(basePowerUse * multiplier);
		this.forceClientUpdate = true;
	}

	@Override
	public String getMachineName()
	{
		return ModObject.blockPoweredSpawner.unlocalisedName;
	}

	@Override
	protected boolean isMachineItemValidForSlot(int i, ItemStack itemstack)
	{
		if (itemstack == null || this.isSpawnMode)
			return false;
		if (this.slotDefinition.isInputSlot(i))
			return itemstack.getItem() == EnderIO.itemSoulVessel && !EnderIO.itemSoulVessel.containsSoul(itemstack);
		return false;
	}

	@Override
	protected IMachineRecipe canStartNextTask(float chance)
	{
		if (!this.hasEntityName())
			return null;
		if (this.isSpawnMode)
		{
			if (MIN_PLAYER_DISTANCE > 0)
				if (this.worldObj.getClosestPlayer(this.xCoord + 0.5, this.yCoord + 0.5, this.zCoord + 0.5, MIN_PLAYER_DISTANCE) == null)
					return null;
		}
		else if (this.getStackInSlot(0) == null || this.getStackInSlot(1) != null)
			return null;
		return new DummyRecipe();
	}

	@Override
	protected boolean startNextTask(IMachineRecipe nextRecipe, float chance)
	{
		return super.startNextTask(nextRecipe, chance);
	}

	@Override
	public int getPowerUsePerTick()
	{
		return this.powerUsePerTick;
	}

	@Override
	protected boolean hasInputStacks()
	{
		return true;
	}

	@Override
	protected boolean canInsertResult(float chance, IMachineRecipe nextRecipe)
	{
		return true;
	}

	@Override
	public void readCommon(NBTTagCompound nbtRoot)
	{
		//Must read the mob type first so we know the multiplier to be used when calculating input/output power
		String mobType = BlockPoweredSpawner.readMobTypeFromNBT(nbtRoot);
		if (mobType == null)
			mobType = NULL_ENTITY_NAME;
		this.entityTypeName = mobType;
		if (!nbtRoot.hasKey("isSpawnMode"))
			this.isSpawnMode = true;
		else
			this.isSpawnMode = nbtRoot.getBoolean("isSpawnMode");
		super.readCommon(nbtRoot);
	}

	@Override
	public void writeCommon(NBTTagCompound nbtRoot)
	{
		if (this.hasEntityName())
			BlockPoweredSpawner.writeMobTypeToNBT(nbtRoot, this.getEntityName());
		else
			BlockPoweredSpawner.writeMobTypeToNBT(nbtRoot, null);
		nbtRoot.setBoolean("isSpawnMode", this.isSpawnMode);
		super.writeCommon(nbtRoot);
	}

	@Override
	protected void updateEntityClient()
	{
		if (this.isActive())
		{
			double x = this.xCoord + this.worldObj.rand.nextFloat();
			double y = this.yCoord + this.worldObj.rand.nextFloat();
			double z = this.zCoord + this.worldObj.rand.nextFloat();
			this.worldObj.spawnParticle("smoke", x, y, z, 0.0D, 0.0D, 0.0D);
			this.worldObj.spawnParticle("flame", x, y, z, 0.0D, 0.0D, 0.0D);
		}
		super.updateEntityClient();
	}

	@Override
	protected IPoweredTask createTask(IMachineRecipe nextRecipe, float chance)
	{
		PoweredTask res = new PoweredTask(nextRecipe, chance, this.getRecipeInputs());

		int ticksDelay;
		if (this.isSpawnMode)
			ticksDelay = TilePoweredSpawner.MIN_SPAWN_DELAY_BASE + (int) Math.round((TilePoweredSpawner.MAX_SPAWN_DELAY_BASE - TilePoweredSpawner.MIN_SPAWN_DELAY_BASE) * Math.random());
		else
			ticksDelay = TilePoweredSpawner.MAX_SPAWN_DELAY_BASE - (TilePoweredSpawner.MAX_SPAWN_DELAY_BASE - TilePoweredSpawner.MIN_SPAWN_DELAY_BASE) / 2;

		// TODO gamerforEA code start
		if (!EventConfig.spawnerIgnoreCapacitorType)
			// TODO gamerforEA code end
			if (this.getCapacitorType().ordinal() == 1)
				ticksDelay /= 2;
			else if (this.getCapacitorType().ordinal() == 2)
				ticksDelay /= 4;

		int powerPerTick = this.getPowerUsePerTick();
		res.setRequiredEnergy(powerPerTick * ticksDelay);
		return res;
	}

	protected boolean canSpawnEntity(EntityLiving entityliving)
	{
		boolean spaceClear = this.worldObj.checkNoEntityCollision(entityliving.boundingBox) && this.worldObj.getCollidingBoundingBoxes(entityliving, entityliving.boundingBox).isEmpty() && (!this.worldObj.isAnyLiquid(entityliving.boundingBox) || entityliving.isCreatureType(EnumCreatureType.waterCreature, false));
		if (spaceClear && USE_VANILLA_SPAWN_CHECKS)
			//Full checks for lighting, dimension etc
			spaceClear = entityliving.getCanSpawnHere();
		return spaceClear;
	}

	Entity createEntity(boolean forceAlive)
	{
		Entity ent = EntityList.createEntityByName(this.getEntityName(), this.worldObj);
		if (forceAlive && MIN_PLAYER_DISTANCE <= 0 && Config.poweredSpawnerDespawnTimeSeconds > 0 && ent instanceof EntityLiving)
		{
			ent.getEntityData().setLong(BlockPoweredSpawner.KEY_SPAWNED_BY_POWERED_SPAWNER, this.worldObj.getTotalWorldTime());
			((EntityLiving) ent).func_110163_bv();
		}
		return ent;
	}

	protected boolean trySpawnEntity()
	{
		Entity entity = this.createEntity(true);
		if (!(entity instanceof EntityLiving))
			return false;

		EntityLiving entityliving = (EntityLiving) entity;
		int spawnRange = Config.poweredSpawnerSpawnRange;

		if (Config.poweredSpawnerMaxNearbyEntities > 0)
		{
			int nearbyEntities = this.worldObj.getEntitiesWithinAABB(entity.getClass(), AxisAlignedBB.getBoundingBox(this.xCoord - spawnRange * 2, this.yCoord - 4, this.zCoord - spawnRange * 2, this.xCoord + spawnRange * 2, this.yCoord + 4, this.zCoord + spawnRange * 2)).size();
			if (nearbyEntities >= Config.poweredSpawnerMaxNearbyEntities)
				return false;
		}

		while (this.remainingSpawnTries-- > 0)
		{
			double x = this.xCoord + (this.worldObj.rand.nextDouble() - this.worldObj.rand.nextDouble()) * spawnRange;
			double y = this.yCoord + this.worldObj.rand.nextInt(3) - 1;
			double z = this.zCoord + (this.worldObj.rand.nextDouble() - this.worldObj.rand.nextDouble()) * spawnRange;
			entity.setLocationAndAngles(x, y, z, this.worldObj.rand.nextFloat() * 360.0F, 0.0F);

			if (this.canSpawnEntity(entityliving))
			{
				entityliving.onSpawnWithEgg(null);

				// TODO gamerforEA code start
				if (EventConfig.spawnerDisableWitherSkeletons && entity instanceof EntitySkeleton)
				{
					EntitySkeleton skeleton = (EntitySkeleton) entity;
					if (skeleton.getSkeletonType() == 1)
						return false;
				}
				// TODO gamerforEA code end

				this.worldObj.spawnEntityInWorld(entityliving);
				this.worldObj.playAuxSFX(2004, this.xCoord, this.yCoord, this.zCoord, 0);
				entityliving.spawnExplosionParticle();
				return true;
			}
		}

		return false;
	}

	public String getEntityName()
	{
		return this.entityTypeName;
	}

	public boolean hasEntityName()
	{
		return !NULL_ENTITY_NAME.equals(this.entityTypeName);
	}
}
