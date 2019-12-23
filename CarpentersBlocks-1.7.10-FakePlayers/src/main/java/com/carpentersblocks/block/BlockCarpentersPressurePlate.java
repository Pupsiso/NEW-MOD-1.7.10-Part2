package com.carpentersblocks.block;

import com.carpentersblocks.data.PressurePlate;
import com.carpentersblocks.tileentity.TEBase;
import com.carpentersblocks.util.handler.ChatHandler;
import com.carpentersblocks.util.registry.BlockRegistry;
import com.carpentersblocks.util.registry.IconRegistry;
import com.gamerforea.carpentersblocks.EventConfig;
import com.gamerforea.carpentersblocks.ModUtils;
import com.gamerforea.eventhelper.util.EventUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class BlockCarpentersPressurePlate extends BlockSided
{

	private static PressurePlate data = new PressurePlate();

	/**
	 * Whether full bounds should be used for collision purposes.
	 */
	private boolean fullBounds = false;

	public BlockCarpentersPressurePlate(Material material)
	{
		super(material, data);
	}

	@Override
	@SideOnly(Side.CLIENT)
	/**
	 * Returns a base icon that doesn't rely on blockIcon, which is set prior to
	 * texture stitch events.
	 */ public IIcon getIcon()
	{
		return IconRegistry.icon_uncovered_full;
	}

	@Override
	/**
	 * Alters polarity.
	 */ protected boolean onHammerLeftClick(TEBase TE, EntityPlayer entityPlayer)
	{
		int polarity = data.getPolarity(TE) == PressurePlate.POLARITY_POSITIVE ? PressurePlate.POLARITY_NEGATIVE : PressurePlate.POLARITY_POSITIVE;

		data.setPolarity(TE, polarity);
		this.notifyBlocksOfPowerChange(TE.getWorldObj(), TE.xCoord, TE.yCoord, TE.zCoord);

		if (polarity == PressurePlate.POLARITY_POSITIVE)
			ChatHandler.sendMessageToPlayer("message.polarity_pos.name", entityPlayer);
		else
			ChatHandler.sendMessageToPlayer("message.polarity_neg.name", entityPlayer);

		return true;
	}

	@Override
	/**
	 * Alters trigger behavior.
	 */ protected boolean onHammerRightClick(TEBase TE, EntityPlayer entityPlayer)
	{
		int trigger = data.getTriggerEntity(TE);

		if (++trigger > 3)
			trigger = 0;

		data.setTriggerEntity(TE, trigger);
		this.notifyBlocksOfPowerChange(TE.getWorldObj(), TE.xCoord, TE.yCoord, TE.zCoord);

		if (trigger == PressurePlate.TRIGGER_PLAYER)
			ChatHandler.sendMessageToPlayer("message.trigger_player.name", entityPlayer);
		else if (trigger == PressurePlate.TRIGGER_MONSTER)
			ChatHandler.sendMessageToPlayer("message.trigger_monster.name", entityPlayer);
		else if (trigger == PressurePlate.TRIGGER_ANIMAL)
			ChatHandler.sendMessageToPlayer("message.trigger_animal.name", entityPlayer);
		else
			ChatHandler.sendMessageToPlayer("message.trigger_all.name", entityPlayer);

		return true;
	}

	/**
	 * Returns a bounding box from the pool of bounding boxes (this means this
	 * box can change after the pool has been cleared to be reused)
	 */
	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z)
	{
		return null;
	}

	@Override
	/**
	 * Updates the blocks bounds based on its current state. Args: world, x, y,
	 * z
	 */ public void setBlockBoundsBasedOnState(IBlockAccess blockAccess, int x, int y, int z)
	{
		TEBase TE = this.getTileEntity(blockAccess, x, y, z);

		if (TE != null)
		{
			float depth = this.fullBounds | !this.isDepressed(TE) ? 0.0625F : 0.03125F;
			this.setBlockBounds(0.0625F, 0.0625F, 0.0F, 0.9375F, 0.9375F, depth, data.getDirection(TE));
		}
	}

	@Override
	/**
	 * Ticks the block if it's been scheduled
	 */ public void updateTick(World world, int x, int y, int z, Random random)
	{
		if (!world.isRemote)
		{

			TEBase TE = this.getTileEntity(world, x, y, z);

			if (TE != null)
			{

				boolean depressed = false;

				if (this.isDepressed(TE))
					depressed = this.hasTriggerInBounds(TE);

				if (depressed)
					world.scheduleBlockUpdate(x, y, z, this, this.tickRate(world));
				else
					this.toggleOff(TE, world, x, y, z);

			}

		}
	}

	@Override
	/**
	 * Triggered whenever an entity collides with this block (enters into the
	 * block). Args: world, x, y, z, entity
	 */ public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity)
	{
		if (!world.isRemote)
		{
			TEBase tile = this.getTileEntity(world, x, y, z);
			if (tile != null)
				if (this.hasTriggerInBounds(tile))
					this.toggleOn(tile, world, x, y, z);
		}
	}

	/**
	 * Returns whether sensitive area contains an entity that can trigger a
	 * state change.
	 *
	 * @param tile the {@link TEBase}
	 * @return whether sensitive area contains valid {@link Entity}
	 */
	private boolean hasTriggerInBounds(TEBase tile)
	{
		this.fullBounds = true;
		List<Entity> entityList = tile.getWorldObj().getEntitiesWithinAABB(Entity.class, this.getSensitiveAABB(tile.getWorldObj(), tile.xCoord, tile.yCoord, tile.zCoord));
		this.fullBounds = false;

		// TODO gamerforEA code start
		Iterator<Entity> iter = entityList.iterator();
		while (iter.hasNext())
		{
			Entity entity = iter.next();
			if (entity instanceof EntityPlayer)
			{
				boolean doRemove;
				if (EventConfig.plateValidEvents)
					doRemove = ModUtils.cantInteractPhysical((EntityPlayer) entity, ((EntityPlayer) entity).getHeldItem(), tile.xCoord, tile.yCoord, tile.zCoord, ForgeDirection.UP);
				else
					doRemove = EventUtils.cantBreak((EntityPlayer) entity, tile.xCoord, tile.yCoord, tile.zCoord);
				if (doRemove)
					iter.remove();
			}
		}
		// TODO gamerforEA code end

		if (!entityList.isEmpty())
			for (int idx = 0; idx < entityList.size(); ++idx)
			{
				if (this.canEntityTrigger(tile, entityList.get(idx)))
					return true;
			}

		return false;
	}

	/**
	 * Gets the area of bounding box that is sensitive to changes.
	 *
	 * @param x the x coordinate
	 * @param y the y coordinate
	 * @param z the z coordinate
	 * @return the {@link AxisAlignedBB}
	 */
	private AxisAlignedBB getSensitiveAABB(World world, int x, int y, int z)
	{
		this.setBlockBoundsBasedOnState(world, x, y, z);
		return AxisAlignedBB.getBoundingBox(x + this.minX, y + this.minY, z + this.minZ, x + this.maxX, y + this.maxY, z + this.maxZ);
	}

	/**
	 * Activates pressure plate.
	 */
	private void toggleOn(TEBase TE, World world, int x, int y, int z)
	{
		data.setState(TE, PressurePlate.STATE_ON, true);
		this.notifyBlocksOfPowerChange(world, x, y, z);
		world.scheduleBlockUpdate(x, y, z, this, this.tickRate(world));
	}

	/**
	 * Deactivates pressure plate.
	 */
	private void toggleOff(TEBase TE, World world, int x, int y, int z)
	{
		data.setState(TE, PressurePlate.STATE_OFF, true);
		this.notifyBlocksOfPowerChange(world, x, y, z);
	}

	/**
	 * Returns whether pressure plate is in depressed state
	 */
	private boolean isDepressed(TEBase TE)
	{
		return data.getState(TE) == PressurePlate.STATE_ON;
	}

	@Override
	/**
	 * Can this block provide power. Only wire currently seems to have this
	 * change based on its state.
	 */ public boolean canProvidePower()
	{
		return true;
	}

	/**
	 * Returns power level (0 or 15)
	 */
	@Override
	public int getPowerOutput(TEBase TE)
	{
		int polarity = data.getPolarity(TE);

		if (this.isDepressed(TE))
			return polarity == PressurePlate.POLARITY_POSITIVE ? 15 : 0;
		return polarity == PressurePlate.POLARITY_NEGATIVE ? 15 : 0;
	}

	/**
	 * Returns whether pressure plate should trigger based on entity colliding
	 * with it.
	 */
	private boolean canEntityTrigger(TEBase tile, Entity entity)
	{
		if (entity == null)
			return false;

		int trigger = data.getTriggerEntity(tile);

		if (trigger == PressurePlate.TRIGGER_PLAYER)
			return entity instanceof EntityPlayer;
		if (trigger == PressurePlate.TRIGGER_MONSTER)
			return entity.isCreatureType(EnumCreatureType.monster, false);
		if (trigger == PressurePlate.TRIGGER_ANIMAL)
			return entity.isCreatureType(EnumCreatureType.creature, false);
		return true;
	}

	@Override
	/**
	 * Ejects contained items into the world, and notifies neighbours of an
	 * update, as appropriate
	 */ public void breakBlock(World world, int x, int y, int z, Block block, int metadata)
	{
		TEBase TE = this.getSimpleTileEntity(world, x, y, z);

		if (TE != null)
			if (this.isDepressed(TE))
				this.notifyBlocksOfPowerChange(world, x, y, z);

		super.breakBlock(world, x, y, z, block, metadata);
	}

	@Override
	/**
	 * The type of render function that is called for this block
	 */ public int getRenderType()
	{
		return BlockRegistry.carpentersPressurePlateRenderID;
	}

}
