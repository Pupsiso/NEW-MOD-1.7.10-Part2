package com.carpentersblocks.tileentity;

import com.carpentersblocks.block.BlockCoverable;
import com.carpentersblocks.util.Attribute;
import com.carpentersblocks.util.BlockProperties;
import com.carpentersblocks.util.handler.DesignHandler;
import com.carpentersblocks.util.protection.IProtected;
import com.carpentersblocks.util.protection.ProtectedObject;
import com.carpentersblocks.util.registry.FeatureRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagShort;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

public class TEBase extends TileEntity implements IProtected
{

	public static final String TAG_ATTR = "cbAttribute";
	public static final String TAG_ATTR_LIST = "cbAttrList";
	public static final String TAG_METADATA = "cbMetadata";
	public static final String TAG_OWNER = "cbOwner";
	public static final String TAG_CHISEL_DESIGN = "cbChiselDesign";
	public static final String TAG_DESIGN = "cbDesign";

	public static final byte[] ATTR_COVER = { 0, 1, 2, 3, 4, 5, 6 };
	public static final byte[] ATTR_DYE = { 7, 8, 9, 10, 11, 12, 13 };
	public static final byte[] ATTR_OVERLAY = { 14, 15, 16, 17, 18, 19, 20 };
	public static final byte ATTR_ILLUMINATOR = 21;
	public static final byte ATTR_PLANT = 22;
	public static final byte ATTR_SOIL = 23;
	public static final byte ATTR_FERTILIZER = 24;
	public static final byte ATTR_UPGRADE = 25;

	/**
	 * Map holding all block attributes.
	 */
	protected Map<Byte, Attribute> cbAttrMap = new HashMap<>();

	/**
	 * Chisel design for each side and base block.
	 */
	protected String[] cbChiselDesign = { "", "", "", "", "", "", "" };

	/**
	 * Holds specific block information like facing, states, etc.
	 */
	protected int cbMetadata;

	/**
	 * Design name.
	 */
	protected String cbDesign = "";

	/**
	 * Owner of tile entity.
	 */
	protected String cbOwner = "";

	/**
	 * Indicates lighting calculations are underway.
	 **/
	protected static boolean calcLighting = false;

	/**
	 * Holds last stored metadata.
	 **/
	private int tempMetadata;

	/**
	 * The most recent light value of block.
	 **/
	private int lightValue = -1;

	/**
	 * Comment
	 **/
	@Override
	public void readFromNBT(NBTTagCompound nbt)
	{
		super.readFromNBT(nbt);

		this.cbAttrMap.clear();
		if (nbt.hasKey("owner"))
			TileEntityHelper.updateMappingsOnRead(this, nbt);
		else
		{
			NBTTagList nbttaglist = nbt.getTagList(TAG_ATTR_LIST, 10);
			for (int idx = 0; idx < nbttaglist.tagCount(); ++idx)
			{
				NBTTagCompound nbt1 = nbttaglist.getCompoundTagAt(idx);
				Attribute attribute = Attribute.loadAttributeFromNBT(nbt1);
				if (attribute.getItemStack() != null)
				{
					attribute.getItemStack().stackSize = 1; // All ItemStacks pre-3.2.7 DEV R3 stored original stack sizes, reduce them here.
					byte attrId = (byte) (nbt1.getByte(TAG_ATTR) & 255);
					this.cbAttrMap.put(attrId, attribute);
				}
			}

			for (int idx = 0; idx < 7; ++idx)
			{
				this.cbChiselDesign[idx] = nbt.getString(TAG_CHISEL_DESIGN + "_" + idx);
			}

			// Handle 3.3.7 structure changes
			this.convertDataToInt(nbt);

			this.cbDesign = nbt.getString(TAG_DESIGN);
			this.cbOwner = nbt.getString(TAG_OWNER);
		}

		// Block either loaded or changed, update lighting and render state
		this.updateWorldAndLighting();
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt)
	{
		super.writeToNBT(nbt);

		NBTTagList tagList = new NBTTagList();
		for (Map.Entry<Byte, Attribute> entry : this.cbAttrMap.entrySet())
		{
			NBTTagCompound nbt1 = new NBTTagCompound();
			nbt1.setByte(TAG_ATTR, entry.getKey());
			entry.getValue().writeToNBT(nbt1);
			tagList.appendTag(nbt1);
		}
		nbt.setTag(TAG_ATTR_LIST, tagList);

		for (int idx = 0; idx < 7; ++idx)
		{
			nbt.setString(TAG_CHISEL_DESIGN + "_" + idx, this.cbChiselDesign[idx]);
		}

		nbt.setInteger(TAG_METADATA, this.cbMetadata);
		nbt.setString(TAG_DESIGN, this.cbDesign);
		nbt.setString(TAG_OWNER, this.cbOwner);
	}

	/**
	 * Handles data conversion from short to int for update 3.3.7.
	 *
	 * @param nbt the {@link NBTTagCompound}
	 * @return <code>true</code> if data was converted
	 */
	private boolean convertDataToInt(NBTTagCompound nbt)
	{
		// 3.3.7 DEV converted cbMetadata to integer
		if (nbt.getTag(TAG_METADATA) instanceof NBTTagShort)
		{
			this.cbMetadata = nbt.getShort(TAG_METADATA);
			return true;
		}
		this.cbMetadata = nbt.getInteger(TAG_METADATA);
		return false;
	}

	@Override
	/**
	 * Overridden in a sign to provide the text.
	 */ public Packet getDescriptionPacket()
	{
		NBTTagCompound nbt = new NBTTagCompound();
		this.writeToNBT(nbt);
		return new S35PacketUpdateTileEntity(this.xCoord, this.yCoord, this.zCoord, 0, nbt);
	}

	@Override
	/**
	 * Called when you receive a TileEntityData packet for the location this
	 * TileEntity is currently in. On the client, the NetworkManager will always
	 * be the remote server. On the server, it will be whomever is responsible for
	 * sending the packet.
	 *
	 * @param net The NetworkManager the packet originated from
	 * @param pkt The data packet
	 */ public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt)
	{
		this.readFromNBT(pkt.func_148857_g());
	}

	/**
	 * Called from Chunk.setBlockIDWithMetadata, determines if this tile entity should be re-created when the ID, or Metadata changes.
	 * Use with caution as this will leave straggler TileEntities, or create conflicts with other TileEntities if not used properly.
	 *
	 * @param oldMeta The old metadata of the block
	 * @param newMeta The new metadata of the block (May be the same)
	 * @param world   Current world
	 * @param x       X Position
	 * @param y       Y Position
	 * @param z       Z Position
	 * @return True to remove the old tile entity, false to keep it in tact {and create a new one if the new values specify to}
	 */
	@Override
	public boolean shouldRefresh(Block oldBlock, Block newBlock, int oldMeta, int newMeta, World world, int x, int y, int z)
	{
		/*
		 * This is a curious method.
		 *
		 * Essentially, when doing most block logic server-side, changes
		 * to blocks will momentarily "flash" to their default state
		 * when rendering client-side.  This is most noticeable when adding
		 * or removing covers for the first time.
		 *
		 * Making the tile entity refresh only when the block is first created
		 * is not only reasonable, but fixes this behavior.
		 */
		return oldBlock != newBlock;
	}

	/**
	 * Copies owner from TEBase object.
	 */
	public void copyOwner(final TEBase TE)
	{
		this.cbOwner = TE.getOwner();
		this.markDirty();
	}

	/**
	 * Sets owner of tile entity.
	 */
	@Override
	public void setOwner(ProtectedObject obj)
	{
		this.cbOwner = obj.toString();
		this.markDirty();
	}

	@Override
	public String getOwner()
	{
		return this.cbOwner;
	}

	@Override
	/**
	 * Determines if this TileEntity requires update calls.
	 * @return True if you want updateEntity() to be called, false if not
	 */ public boolean canUpdate()
	{
		return false;
	}

	public boolean hasAttribute(byte attrId)
	{
		return this.cbAttrMap.containsKey(attrId);
	}

	public ItemStack getAttribute(byte attrId)
	{
		Attribute attribute = this.cbAttrMap.get(attrId);
		if (attribute != null)
			return attribute.getItemStack();

		return null;
	}

	public ItemStack getAttributeForDrop(byte attrId)
	{
		ItemStack itemStack = this.cbAttrMap.get(attrId).getItemStack();

		// If cover, check for rotation and restore default metadata
		if (attrId <= ATTR_COVER[6])
			this.setDefaultMetadata(itemStack);

		return itemStack;
	}

	/**
	 * Will restore cover to default state before returning {@link ItemStack}.
	 * <p>
	 * Corrects log rotation, among other things.
	 *
	 * @param itemStack the {@link ItemStack}
	 * @return the cover {@link ItemStack} in it's default state
	 */
	private ItemStack setDefaultMetadata(ItemStack itemStack)
	{
		Block block = BlockProperties.toBlock(itemStack);

		// Correct rotation metadata before dropping block
		if (BlockProperties.blockRotates(itemStack) || block instanceof BlockDirectional)
		{
			int dmgDrop = block.damageDropped(itemStack.getItemDamage());
			Item itemDrop = block.getItemDropped(itemStack.getItemDamage(), this.getWorldObj().rand, /* Fortune */ 0);

			/* Check if block drops itself, and, if so, correct the damage value to the block's default. */

			if (itemDrop != null && itemDrop.equals(itemStack.getItem()) && dmgDrop != itemStack.getItemDamage())
				itemStack.setItemDamage(dmgDrop);
		}

		return itemStack;
	}

	public void addAttribute(byte attrId, ItemStack itemStack)
	{
		if (this.hasAttribute(attrId) || itemStack == null)
			return;

		// Reduce stack size to 1 and save attribute
		ItemStack reducedStack = ItemStack.copyItemStack(itemStack);
		reducedStack.stackSize = 1;
		this.cbAttrMap.put(attrId, new Attribute(reducedStack));

		// Produce world events if specific attributes are set
		World world = this.getWorldObj();
		Block block = BlockProperties.toBlock(itemStack);

		if (attrId < 7)
		{
			if (attrId == ATTR_COVER[6])
			{
				int metadata = itemStack.getItemDamage();
				world.setBlockMetadataWithNotify(this.xCoord, this.yCoord, this.zCoord, metadata, 0);
			}
			world.notifyBlocksOfNeighborChange(this.xCoord, this.yCoord, this.zCoord, block);
		}
		else if (attrId == ATTR_PLANT | attrId == ATTR_SOIL)
			world.notifyBlocksOfNeighborChange(this.xCoord, this.yCoord, this.zCoord, block);

		/* Play sound when fertilizing plants.. though I've never heard it before. */
		if (attrId == ATTR_FERTILIZER)
			this.getWorldObj().playAuxSFX(2005, this.xCoord, this.yCoord, this.zCoord, 0);

		this.updateWorldAndLighting();
		this.markDirty();
	}

	/**
	 * Will remove the attribute from map once block drop is complete.
	 * <p>
	 * Should only be called externally by {@link BlockCoverable#onBlockEventReceived}.
	 *
	 * @param attrId
	 */
	public void onAttrDropped(byte attrId)
	{
		this.cbAttrMap.remove(attrId);
		this.updateWorldAndLighting();
		this.markDirty();
	}

	/**
	 * Initiates block drop event, which will remove attribute from tile entity.
	 *
	 * @param attrId the attribute ID
	 */
	public void createBlockDropEvent(byte attrId)
	{
		this.getWorldObj().addBlockEvent(this.xCoord, this.yCoord, this.zCoord, this.getBlockType(), BlockCoverable.EVENT_ID_DROP_ATTR, attrId);
	}

	public void removeAttributes(int side)
	{
		this.createBlockDropEvent(ATTR_COVER[side]);
		this.createBlockDropEvent(ATTR_DYE[side]);
		this.createBlockDropEvent(ATTR_OVERLAY[side]);

		if (side == 6)
			this.createBlockDropEvent(ATTR_ILLUMINATOR);
	}

	/**
	 * Returns whether block has pattern.
	 */
	public boolean hasChiselDesign(int side)
	{
		return DesignHandler.listChisel.contains(this.getChiselDesign(side));
	}

	/**
	 * Returns pattern.
	 */
	public String getChiselDesign(int side)
	{
		return this.cbChiselDesign[side];
	}

	/**
	 * Sets pattern.
	 */
	public boolean setChiselDesign(int side, String iconName)
	{
		if (!this.cbChiselDesign.equals(iconName))
		{
			this.cbChiselDesign[side] = iconName;
			this.getWorldObj().markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
			this.markDirty();
			return true;
		}

		return false;
	}

	public void removeChiselDesign(int side)
	{
		if (!this.cbChiselDesign.equals(""))
		{
			this.cbChiselDesign[side] = "";
			this.getWorldObj().markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
			this.markDirty();
		}
	}

	/**
	 * Gets block-specific data.
	 *
	 * @return the data
	 */
	public int getData()
	{
		return this.cbMetadata;
	}

	/**
	 * Sets block-specific data.
	 */
	public boolean setData(int data)
	{
		if (data != this.getData())
		{
			this.cbMetadata = data;
			this.getWorldObj().markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
			this.markDirty();
			return true;
		}

		return false;
	}

	public boolean hasDesign()
	{
		return DesignHandler.getListForType(this.getBlockDesignType()).contains(this.cbDesign);
	}

	public String getDesign()
	{
		return this.cbDesign;
	}

	public boolean setDesign(String name)
	{
		if (!this.cbDesign.equals(name))
		{
			this.cbDesign = name;
			this.getWorldObj().markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
			this.markDirty();
			return true;
		}

		return false;
	}

	public boolean removeDesign()
	{
		return this.setDesign("");
	}

	public String getBlockDesignType()
	{
		String name = this.getBlockType().getUnlocalizedName();
		return name.substring("tile.blockCarpenters".length()).toLowerCase();
	}

	public boolean setNextDesign()
	{
		return this.setDesign(DesignHandler.getNext(this.getBlockDesignType(), this.cbDesign));
	}

	public boolean setPrevDesign()
	{
		return this.setDesign(DesignHandler.getPrev(this.getBlockDesignType(), this.cbDesign));
	}

	/**
	 * Sets block metadata without causing a render update.
	 * <p>
	 * As part of mimicking a cover block, the metadata must be changed
	 * to better represent the cover properties.
	 * <p>
	 */
	public void setMetadata(int metadata)
	{
		this.tempMetadata = this.getWorldObj().getBlockMetadata(this.xCoord, this.yCoord, this.zCoord);
		this.getWorldObj().setBlockMetadataWithNotify(this.xCoord, this.yCoord, this.zCoord, metadata, 4);
	}

	/**
	 * Restores default metadata for block from base cover.
	 */
	public void restoreMetadata()
	{
		this.getWorldObj().setBlockMetadataWithNotify(this.xCoord, this.yCoord, this.zCoord, this.tempMetadata, 4);
	}

	/////////////////////////////////////////////////////////////
	// Code below implemented strictly for light updates
	/////////////////////////////////////////////////////////////

	/**
	 * Grabs light value from cache.
	 * <p>
	 * If not cached, will calculate value first.
	 *
	 * @return the light value
	 */
	public int getLightValue()
	{
		if (this.lightValue == -1 && !calcLighting)
			this.updateCachedLighting();
		return this.lightValue;
	}

	/**
	 * Returns the current block light value. This is the only method
	 * that will grab the tile entity to calculate lighting, which
	 * is a very expensive operation to call while rendering, as it is
	 * called often.
	 *
	 * @return a light value from 0 to 15
	 */
	protected int getDynamicLightValue()
	{
		int value = 0;

		if (FeatureRegistry.enableIllumination && this.hasAttribute(ATTR_ILLUMINATOR))
			return 15;
		// Find greatest light output from attributes
		calcLighting = true;
		for (Map.Entry<Byte, Attribute> pair : this.cbAttrMap.entrySet())
		{
			ItemStack itemStack = BlockProperties.getCallableItemStack(pair.getValue().getItemStack());
			Block block = BlockProperties.toBlock(itemStack);

			if (block != Blocks.air)
			{

				// Determine metadata-sensitive light value (usually recursive, and not useful)
				this.setMetadata(itemStack.getItemDamage());
				int sensitiveLight = block.getLightValue(this.getWorldObj(), this.xCoord, this.yCoord, this.zCoord);
				this.restoreMetadata();

				// Grab default light value for block
				if (sensitiveLight > 0)
					value = Math.max(value, sensitiveLight);
				else
					value = Math.max(value, block.getLightValue());

			}
		}
		calcLighting = false;

		return value;
	}

	// TODO gamerforEA code start
	private boolean lightingCacheUpdating;
	// TODO gamerforEA code end

	/**
	 * Updates light value and world lightmap.
	 */
	private void updateCachedLighting()
	{
		// TODO gamerforEA code start
		if (this.lightingCacheUpdating)
			return;
		this.lightingCacheUpdating = true;
		try
		{
			// TODO gamerforEA code end
			this.lightValue = this.getDynamicLightValue();
			this.getWorldObj().func_147451_t(this.xCoord, this.yCoord, this.zCoord); // Updates block lightmap, should help with spawns
			// TODO gamerforEA code start
		}
		finally
		{
			this.lightingCacheUpdating = false;
		}
		// TODO gamerforEA code end
	}

	/**
	 * Performs world update and refreshes lighting.
	 */
	private void updateWorldAndLighting()
	{
		World world = this.getWorldObj();
		if (world != null)
		{
			this.updateCachedLighting();
			world.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
		}
	}
}
