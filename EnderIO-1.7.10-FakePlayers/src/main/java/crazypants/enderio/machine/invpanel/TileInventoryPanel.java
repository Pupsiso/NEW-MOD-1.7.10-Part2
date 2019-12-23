package crazypants.enderio.machine.invpanel;

import com.enderio.core.api.common.util.ITankAccess;
import crazypants.enderio.EnderIO;
import crazypants.enderio.ModObject;
import crazypants.enderio.conduit.TileConduitBundle;
import crazypants.enderio.conduit.item.FilterRegister;
import crazypants.enderio.conduit.item.ItemConduit;
import crazypants.enderio.conduit.item.ItemConduitNetwork;
import crazypants.enderio.conduit.item.filter.IItemFilter;
import crazypants.enderio.config.Config;
import crazypants.enderio.machine.AbstractMachineEntity;
import crazypants.enderio.machine.IoMode;
import crazypants.enderio.machine.SlotDefinition;
import crazypants.enderio.machine.generator.zombie.IHasNutrientTank;
import crazypants.enderio.machine.generator.zombie.PacketNutrientTank;
import crazypants.enderio.machine.invpanel.client.ClientDatabaseManager;
import crazypants.enderio.machine.invpanel.client.InventoryDatabaseClient;
import crazypants.enderio.machine.invpanel.server.InventoryDatabaseServer;
import crazypants.enderio.network.PacketHandler;
import crazypants.enderio.tool.SmartTank;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.*;

import java.util.ArrayList;
import java.util.WeakHashMap;

public class TileInventoryPanel extends AbstractMachineEntity implements IFluidHandler, ITankAccess, IHasNutrientTank
{

	public static final int SLOT_CRAFTING_START = 0;
	public static final int SLOT_CRAFTING_RESULT = 9;
	public static final int SLOT_VIEW_FILTER = 10;
	public static final int SLOT_RETURN_START = 11;

	public static final int MAX_STORED_CRAFTING_RECIPES = 6;

	protected final SmartTank fuelTank;
	protected boolean tanksDirty;

	private InventoryDatabaseServer dbServer;
	private InventoryDatabaseClient dbClient;

	private boolean active;
	private boolean extractionDisabled;

	public InventoryPanelContainer eventHandler;
	private IItemFilter itemFilter;

	private int guiSortMode;
	private String guiFilterString = "";
	private boolean guiSync;

	private final ArrayList<StoredCraftingRecipe> storedCraftingRecipes;

	// TODO gamerforEA code start
	private final WeakHashMap<EntityPlayer, InventoryPanelContainer> eventHandlers = new WeakHashMap<>();

	public boolean addEventHandler(EntityPlayer player, InventoryPanelContainer container)
	{
		this.eventHandler = container;
		return this.eventHandlers.putIfAbsent(player, container) == null;
	}

	public boolean removeEventHandler(EntityPlayer player, InventoryPanelContainer container)
	{
		this.eventHandler = null;
		return this.eventHandlers.remove(player) != null;
	}
	// TODO gamerforEA code end

	public TileInventoryPanel()
	{
		super(new SlotDefinition(0, 8, 11, 20, 21, 20));
		this.fuelTank = new SmartTank(EnderIO.fluidNutrientDistillation, Config.inventoryPanelFree ? 0 : 2000);
		this.storedCraftingRecipes = new ArrayList<StoredCraftingRecipe>();
	}

	public InventoryDatabaseServer getDatabaseServer()
	{
		return this.dbServer;
	}

	public InventoryDatabaseClient getDatabaseClient(int generation)
	{
		if (this.dbClient != null && this.dbClient.getGeneration() != generation)
		{
			ClientDatabaseManager.INSTANCE.destroyDatabase(this.dbClient.getGeneration());
			this.dbClient = null;
		}
		if (this.dbClient == null)
			this.dbClient = ClientDatabaseManager.INSTANCE.getOrCreateDatabase(generation);
		return this.dbClient;
	}

	public InventoryDatabaseClient getDatabaseClient()
	{
		return this.dbClient;
	}

	@Override
	public boolean canInsertItem(int slot, ItemStack var2, int side)
	{
		return false;
	}

	@Override
	protected boolean canExtractItem(int slot, ItemStack itemstack)
	{
		return !this.extractionDisabled && super.canExtractItem(slot, itemstack);
	}

	@Override
	protected boolean isMachineItemValidForSlot(int slot, ItemStack stack)
	{
		if (slot == SLOT_VIEW_FILTER && stack != null)
			return FilterRegister.isItemFilter(stack) && FilterRegister.isFilterSet(stack);
		return true;
	}

	@Override
	public ItemStack decrStackSize(int fromSlot, int amount)
	{
		ItemStack res = super.decrStackSize(fromSlot, amount);
		if (res != null && fromSlot < SLOT_CRAFTING_RESULT)
		{
			/* TODO gamerforEA code start
			if (this.eventHandler != null)
				this.eventHandler.onCraftMatrixChanged(this); */
			this.eventHandlers.values().forEach(eventHandler -> eventHandler.onCraftMatrixChanged(this));
			// TODO gamerforEA code end
		}

		if (res != null && fromSlot == SLOT_VIEW_FILTER)
			this.updateItemFilter();
		return res;
	}

	@Override
	public void setInventorySlotContents(int slot, ItemStack contents)
	{
		super.setInventorySlotContents(slot, contents);
		if (slot < SLOT_CRAFTING_RESULT)
		{
			/* TODO gamerforEA code start
			if (this.eventHandler != null)
				this.eventHandler.onCraftMatrixChanged(this); */
			this.eventHandlers.values().forEach(eventHandler -> eventHandler.onCraftMatrixChanged(this));
			// TODO gamerforEA code end
		}
		if (slot == SLOT_VIEW_FILTER)
			this.updateItemFilter();
	}

	private void updateItemFilter()
	{
		this.itemFilter = FilterRegister.getFilterForUpgrade(this.inventory[SLOT_VIEW_FILTER]);
	}

	public IItemFilter getItemFilter()
	{
		return this.itemFilter;
	}

	@Override
	public boolean isActive()
	{
		return Config.inventoryPanelFree || this.active;
	}

	@Override
	public void doUpdate()
	{
		if (this.worldObj.isRemote)
		{
			this.updateEntityClient();
			return;
		}

		if (this.shouldDoWorkThisTick(20))
			this.scanNetwork();

		if (this.forceClientUpdate)
		{
			this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
			this.markDirty();
		}

		if (this.tanksDirty)
		{
			this.tanksDirty = false;
			PacketHandler.sendToAllAround(new PacketNutrientTank(this), this);
		}
	}

	private void scanNetwork()
	{
		ForgeDirection facingDir = this.getFacingDir();
		ForgeDirection backside = facingDir.getOpposite();

		ItemConduitNetwork icn = null;

		TileEntity te = this.worldObj.getTileEntity(this.xCoord + backside.offsetX, this.yCoord + backside.offsetY, this.zCoord + backside.offsetZ);
		if (te instanceof TileConduitBundle)
		{
			TileConduitBundle teCB = (TileConduitBundle) te;
			ItemConduit conduit = teCB.getConduit(ItemConduit.class);
			if (conduit != null)
				icn = (ItemConduitNetwork) conduit.getNetwork();
		}

		if (icn != null)
		{
			this.dbServer = icn.getDatabase();
			this.dbServer.sendChangeLogs();
			this.refuelPower(this.dbServer);

			if (this.active != this.dbServer.isOperational())
			{
				this.active = this.dbServer.isOperational();
				this.forceClientUpdate = true;
			}
		}
		else
		{
			if (this.active)
				this.forceClientUpdate = true;
			this.dbServer = null;
			this.active = false;
		}
	}

	public float getAvailablePower()
	{
		return this.getPower() * Config.inventoryPanelPowerPerMB;
	}

	public void refuelPower(InventoryDatabaseServer db)
	{
		float missingPower = Config.inventoryPanelPowerPerMB * 0.5f - db.getPower();
		if (missingPower > 0)
		{
			int amount = (int) Math.ceil(missingPower / Config.inventoryPanelPowerPerMB);
			amount = Math.min(amount, this.getPower());
			if (amount > 0)
			{
				this.useNutrient(amount);
				this.dbServer.addPower(amount * Config.inventoryPanelPowerPerMB);
			}
		}
	}

	public void useNutrient(int amount)
	{
		this.fuelTank.drain(amount, true);
		this.tanksDirty = true;
	}

	private int getPower()
	{
		return Config.inventoryPanelFree ? 100 : this.fuelTank.getFluidAmount();
	}

	@Override
	protected boolean processTasks(boolean redstoneCheckPassed)
	{
		return false;
	}

	public int getGuiSortMode()
	{
		return this.guiSortMode;
	}

	public String getGuiFilterString()
	{
		return this.guiFilterString;
	}

	public boolean getGuiSync()
	{
		return this.guiSync;
	}

	public void setGuiParameter(int sortMode, String filterString, boolean sync)
	{
		this.guiSortMode = sortMode;
		this.guiFilterString = filterString;
		this.guiSync = sync;
		if (this.worldObj != null && this.worldObj.isRemote)
			PacketHandler.INSTANCE.sendToServer(new PacketGuiSettings(this, sortMode, filterString, sync));
		else
			this.markDirty();
	}

	public int getStoredCraftingRecipes()
	{
		return this.storedCraftingRecipes.size();
	}

	public StoredCraftingRecipe getStoredCraftingRecipe(int index)
	{
		if (index < 0 || index >= this.storedCraftingRecipes.size())
			return null;
		return this.storedCraftingRecipes.get(index);
	}

	public void addStoredCraftingRecipe(StoredCraftingRecipe recipe)
	{
		if (this.worldObj != null && this.worldObj.isRemote)
			PacketHandler.INSTANCE.sendToServer(new PacketStoredCraftingRecipe(PacketStoredCraftingRecipe.ACTION_ADD, 0, recipe));
		else
		{
			this.storedCraftingRecipes.add(recipe);
			this.markDirty();
			this.updateBlock();
		}
	}

	public void removeStoredCraftingRecipe(int index)
	{
		if (this.worldObj != null && this.worldObj.isRemote)
			PacketHandler.INSTANCE.sendToServer(new PacketStoredCraftingRecipe(PacketStoredCraftingRecipe.ACTION_DELETE, index, null));
		else if (index >= 0 && index < this.storedCraftingRecipes.size())
		{
			this.storedCraftingRecipes.remove(index);
			this.markDirty();
			this.updateBlock();
		}
	}

	public boolean isExtractionDisabled()
	{
		return this.extractionDisabled;
	}

	public void setExtractionDisabled(boolean extractionDisabled)
	{
		if (this.worldObj != null)
			if (this.worldObj.isRemote)
				PacketHandler.INSTANCE.sendToServer(new PacketSetExtractionDisabled(this, extractionDisabled));
			else if (this.extractionDisabled != extractionDisabled)
			{
				this.extractionDisabled = extractionDisabled;
				PacketHandler.INSTANCE.sendToDimension(new PacketUpdateExtractionDisabled(this, extractionDisabled), this.worldObj.provider.dimensionId);
			}
	}

	/**
	 * This is called by PacketUpdateExtractionDisabled on the client side
	 *
	 * @param extractionDisabled if extraction is disabled
	 */
	void updateExtractionDisabled(boolean extractionDisabled)
	{
		this.extractionDisabled = extractionDisabled;
	}

	@Override
	public void writeCommon(NBTTagCompound nbtRoot)
	{
		super.writeCommon(nbtRoot);
		this.fuelTank.writeCommon("fuelTank", nbtRoot);
		nbtRoot.setInteger("guiSortMode", this.guiSortMode);
		nbtRoot.setString("guiFilterString", this.guiFilterString);
		nbtRoot.setBoolean("guiSync", this.guiSync);
		nbtRoot.setBoolean("extractionDisabled", this.extractionDisabled);

		if (!this.storedCraftingRecipes.isEmpty())
		{
			NBTTagList recipesNBT = new NBTTagList();
			for (StoredCraftingRecipe recipe : this.storedCraftingRecipes)
			{
				NBTTagCompound recipeNBT = new NBTTagCompound();
				recipe.writeToNBT(recipeNBT);
				recipesNBT.appendTag(recipeNBT);
			}
			nbtRoot.setTag("craftingRecipes", recipesNBT);
		}
	}

	@Override
	public void readCommon(NBTTagCompound nbtRoot)
	{
		super.readCommon(nbtRoot);
		this.fuelTank.readCommon("fuelTank", nbtRoot);
		this.guiSortMode = nbtRoot.getInteger("guiSortMode");
		this.guiFilterString = nbtRoot.getString("guiFilterString");
		this.guiSync = nbtRoot.getBoolean("guiSync");
		this.extractionDisabled = nbtRoot.getBoolean("extractionDisabled");
		this.faceModes = null;

		this.storedCraftingRecipes.clear();
		NBTTagList recipesNBT = (NBTTagList) nbtRoot.getTag("craftingRecipes");
		if (recipesNBT != null)
			for (int idx = 0; idx < recipesNBT.tagCount() && this.storedCraftingRecipes.size() < MAX_STORED_CRAFTING_RECIPES; idx++)
			{
				NBTTagCompound recipeNBT = recipesNBT.getCompoundTagAt(idx);
				StoredCraftingRecipe recipe = new StoredCraftingRecipe();
				if (recipe.readFromNBT(recipeNBT))
					this.storedCraftingRecipes.add(recipe);
			}

		/* TODO gamerforEA code start
		if (this.eventHandler != null)
			this.eventHandler.checkCraftingRecipes(); */
		this.eventHandlers.values().forEach(InventoryPanelContainer::checkCraftingRecipes);
		// TODO gamerforEA code end
	}

	@Override
	public void readCustomNBT(NBTTagCompound nbtRoot)
	{
		super.readCustomNBT(nbtRoot);
		this.active = nbtRoot.getBoolean("active");
		this.updateItemFilter();
	}

	@Override
	public void writeCustomNBT(NBTTagCompound nbtRoot)
	{
		super.writeCustomNBT(nbtRoot);
		nbtRoot.setBoolean("active", this.active);
	}

	@Override
	public String getMachineName()
	{
		return ModObject.blockInventoryPanel.unlocalisedName;
	}

	@Override
	public IoMode getIoMode(ForgeDirection face)
	{
		return face == this.getIODirection() ? IoMode.NONE : IoMode.DISABLED;
	}

	@Override
	public void setIoMode(ForgeDirection faceHit, IoMode mode)
	{
	}

	@Override
	public IoMode toggleIoModeForFace(ForgeDirection faceHit)
	{
		return this.getIoMode(faceHit);
	}

	private ForgeDirection getIODirection()
	{
		return this.getFacingDir().getOpposite();
	}

	@Override
	public int fill(ForgeDirection from, FluidStack resource, boolean doFill)
	{
		if (from != this.getIODirection())
			return 0;
		int res = this.fuelTank.fill(resource, doFill);
		if (res > 0 && doFill)
			this.tanksDirty = true;
		return res;
	}

	@Override
	public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain)
	{
		return null;
	}

	@Override
	public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain)
	{
		return null;
	}

	@Override
	public boolean canFill(ForgeDirection from, Fluid fluid)
	{
		return from == this.getIODirection() && this.fuelTank.canFill(fluid);
	}

	@Override
	public boolean canDrain(ForgeDirection from, Fluid fluid)
	{
		return false;
	}

	@Override
	public FluidTankInfo[] getTankInfo(ForgeDirection from)
	{
		if (from == this.getIODirection())
			return new FluidTankInfo[] { this.fuelTank.getInfo() };
		return new FluidTankInfo[0];
	}

	@Override
	public FluidTank getInputTank(FluidStack forFluidType)
	{
		if (forFluidType != null && this.fuelTank.canFill(forFluidType.getFluid()))
			return this.fuelTank;
		return null;
	}

	@Override
	public FluidTank[] getOutputTanks()
	{
		return new FluidTank[0];
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
