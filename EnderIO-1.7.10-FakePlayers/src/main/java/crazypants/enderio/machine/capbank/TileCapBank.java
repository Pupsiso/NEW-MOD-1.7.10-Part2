package crazypants.enderio.machine.capbank;

import cofh.api.energy.IEnergyContainerItem;
import com.enderio.core.common.util.BlockCoord;
import com.enderio.core.common.util.EntityUtil;
import com.enderio.core.common.util.Util;
import com.enderio.core.common.vecmath.Vector3d;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import crazypants.enderio.EnderIO;
import crazypants.enderio.TileEntityEio;
import crazypants.enderio.conduit.IConduitBundle;
import crazypants.enderio.machine.IIoConfigurable;
import crazypants.enderio.machine.IoMode;
import crazypants.enderio.machine.RedstoneControlMode;
import crazypants.enderio.machine.capbank.network.*;
import crazypants.enderio.machine.capbank.packet.PacketNetworkIdRequest;
import crazypants.enderio.network.PacketHandler;
import crazypants.enderio.power.IInternalPowerReceiver;
import crazypants.enderio.power.IPowerInterface;
import crazypants.enderio.power.IPowerStorage;
import crazypants.enderio.power.PowerHandlerUtil;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class TileCapBank extends TileEntityEio
		implements IInternalPowerReceiver, IInventory, IIoConfigurable, IPowerStorage
{

	private Map<ForgeDirection, IoMode> faceModes;
	private Map<ForgeDirection, InfoDisplayType> faceDisplayTypes;

	private CapBankType type;

	private int energyStored;
	private int maxInput = -1;
	private int maxOutput = -1;

	private RedstoneControlMode inputControlMode = RedstoneControlMode.IGNORE;
	private RedstoneControlMode outputControlMode = RedstoneControlMode.IGNORE;

	private boolean redstoneStateDirty = true;
	private boolean isRecievingRedstoneSignal;

	private final List<EnergyReceptor> receptors = new ArrayList<EnergyReceptor>();
	private boolean receptorsDirty = true;

	private ICapBankNetwork network;

	private final ItemStack[] inventory;

	public TileCapBank()
	{
		this.inventory = new ItemStack[4];
	}

	//Client side refernce to look up network state
	private int networkId = -1;
	private int idRequestTimer = 0;

	private boolean dropItems;
	private boolean displayTypesDirty;
	private boolean revalidateDisplayTypes;
	private int lastComparatorState;

	// TODO gamerforEA code start
	public int modCount;
	// TODO gamerforEA code end

	public CapBankType getType()
	{
		if (this.type == null)
			this.type = CapBankType.getTypeFromMeta(this.getBlockMetadata());
		return this.type;
	}

	public void onNeighborBlockChange(Block blockId)
	{
		this.redstoneStateDirty = true;
		this.revalidateDisplayTypes = true;
		// directly call updateReceptors() to work around issue #1433
		this.updateReceptors();
	}

	//---------- Multiblock

	@SideOnly(Side.CLIENT)
	public void setNetworkId(int networkId)
	{
		this.networkId = networkId;
		if (networkId != -1)
			ClientNetworkManager.getInstance().addToNetwork(networkId, this);
	}

	@SideOnly(Side.CLIENT)
	public int getNetworkId()
	{
		return this.networkId;
	}

	public ICapBankNetwork getNetwork()
	{
		return this.network;
	}

	public boolean setNetwork(ICapBankNetwork network)
	{
		this.network = network;
		return true;
	}

	public boolean canConnectTo(TileCapBank cap)
	{
		CapBankType t = this.getType();
		return t.isMultiblock() && t.getUid().equals(cap.getType().getUid());
	}

	@Override
	public void onChunkUnload()
	{
		if (this.network != null)
		{
			// TODO gamerforEA code start
			for (TileCapBank tile : this.network.getMembers())
			{
				tile.modCount++;
			}
			// TODO gamerforEA code end

			this.network.destroyNetwork();
		}
	}

	@Override
	public void invalidate()
	{
		super.invalidate();
		if (this.network != null)
		{
			// TODO gamerforEA code start
			for (TileCapBank tile : this.network.getMembers())
			{
				tile.modCount++;
			}
			// TODO gamerforEA code end

			this.network.destroyNetwork();
		}
	}

	public void moveInventoryToNetwork()
	{
		if (this.network == null)
			return;
		if (this.network.getInventory().getCapBank() == this && !InventoryImpl.isInventoryEmtpy(this.inventory))
			for (TileCapBank cb : this.network.getMembers())
			{
				if (cb != this)
				{
					for (int i = 0; i < this.inventory.length; i++)
					{
						cb.inventory[i] = this.inventory[i];
						this.inventory[i] = null;
					}
					this.network.getInventory().setCapBank(cb);
					break;
				}
			}
	}

	public void onBreakBlock()
	{
		//If we are holding the networks inventory when we care broken, tranfer it to another member of the network
		this.moveInventoryToNetwork();
	}

	@Override
	public void doUpdate()
	{
		if (this.worldObj.isRemote)
		{
			if (this.networkId == -1)
				if (this.idRequestTimer <= 0)
				{
					PacketHandler.INSTANCE.sendToServer(new PacketNetworkIdRequest(this));
					this.idRequestTimer = 5;
				}
				else
					--this.idRequestTimer;
			return;
		}
		this.updateNetwork(this.worldObj);
		if (this.network == null)
			return;

		if (this.redstoneStateDirty)
		{
			int sig = this.worldObj.getStrongestIndirectPower(this.xCoord, this.yCoord, this.zCoord);
			boolean recievingSignal = sig > 0;
			this.network.updateRedstoneSignal(this, recievingSignal);
			this.redstoneStateDirty = false;
		}

		if (this.receptorsDirty)
			this.updateReceptors();
		if (this.revalidateDisplayTypes)
		{
			this.validateDisplayTypes();
			this.revalidateDisplayTypes = false;
		}
		if (this.displayTypesDirty)
		{
			this.displayTypesDirty = false;
			this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
		}

		// update any comparators, since they don't check themselves
		int comparatorState = this.getComparatorOutput();
		if (this.lastComparatorState != comparatorState)
		{
			this.worldObj.func_147453_f(this.xCoord, this.yCoord, this.zCoord, this.getBlockType());
			this.lastComparatorState = comparatorState;
		}

		this.doDropItems();
	}

	private void updateNetwork(World world)
	{
		if (this.getNetwork() == null)
			NetworkUtil.ensureValidNetwork(this);
		if (this.getNetwork() != null)
			this.getNetwork().onUpdateEntity(this);

	}

	//---------- IO

	@Override
	public IoMode toggleIoModeForFace(ForgeDirection faceHit)
	{
		IPowerInterface rec = this.getReceptorForFace(faceHit);
		IoMode curMode = this.getIoMode(faceHit);
		if (curMode == IoMode.PULL)
		{
			this.setIoMode(faceHit, IoMode.PUSH, true);
			return IoMode.PUSH;
		}
		if (curMode == IoMode.PUSH)
		{
			this.setIoMode(faceHit, IoMode.DISABLED, true);
			return IoMode.DISABLED;
		}
		if (curMode == IoMode.DISABLED)
			if (rec == null || rec.getDelegate() instanceof IConduitBundle)
			{
				this.setIoMode(faceHit, IoMode.NONE, true);
				return IoMode.NONE;
			}
		this.setIoMode(faceHit, IoMode.PULL, true);
		return IoMode.PULL;
	}

	@Override
	public boolean supportsMode(ForgeDirection faceHit, IoMode mode)
	{
		IPowerInterface rec = this.getReceptorForFace(faceHit);
		if (mode == IoMode.NONE)
			return rec == null || rec.getDelegate() instanceof IConduitBundle;
		return true;
	}

	@Override
	public void setIoMode(ForgeDirection faceHit, IoMode mode)
	{
		this.setIoMode(faceHit, mode, true);
	}

	public void setIoMode(ForgeDirection faceHit, IoMode mode, boolean updateReceptors)
	{
		if (mode == IoMode.NONE)
		{
			if (this.faceModes == null)
				return;
			this.faceModes.remove(faceHit);
			if (this.faceModes.isEmpty())
				this.faceModes = null;
		}
		else
		{
			if (this.faceModes == null)
				this.faceModes = new EnumMap<ForgeDirection, IoMode>(ForgeDirection.class);
			this.faceModes.put(faceHit, mode);
		}
		if (updateReceptors)
		{
			this.validateModeForReceptor(faceHit);
			this.receptorsDirty = true;
		}
		if (this.worldObj != null)
		{
			this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
			this.worldObj.notifyBlocksOfNeighborChange(this.xCoord, this.yCoord, this.zCoord, this.getBlockType());
		}
	}

	public void setDefaultIoMode(ForgeDirection faceHit)
	{
		EnergyReceptor er = this.getEnergyReceptorForFace(faceHit);
		if (er == null || er.getConduit() != null)
			this.setIoMode(faceHit, IoMode.NONE);
		else if (er.getReceptor().isInputOnly())
			this.setIoMode(faceHit, IoMode.PUSH);
		else if (er.getReceptor().isOutputOnly())
			this.setIoMode(faceHit, IoMode.PULL);
		else
			this.setIoMode(faceHit, IoMode.PUSH);
	}

	@Override
	public void clearAllIoModes()
	{
		if (this.network != null)
			for (TileCapBank cb : this.network.getMembers())
			{
				cb.doClearAllIoModes();
			}
		else
			this.doClearAllIoModes();
	}

	private void doClearAllIoModes()
	{
		for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS)
		{
			this.setDefaultIoMode(dir);
		}
	}

	@Override
	public IoMode getIoMode(ForgeDirection face)
	{
		if (this.faceModes == null)
			return IoMode.NONE;
		IoMode res = this.faceModes.get(face);
		if (res == null)
			return IoMode.NONE;
		return res;
	}

	//----- Info Display

	public boolean hasDisplayTypes()
	{
		return this.faceDisplayTypes != null;
	}

	public InfoDisplayType getDisplayType(ForgeDirection face)
	{
		if (this.faceDisplayTypes == null)
			return InfoDisplayType.NONE;
		InfoDisplayType res = this.faceDisplayTypes.get(face);
		return res == null ? InfoDisplayType.NONE : res;
	}

	public void setDisplayType(ForgeDirection face, InfoDisplayType type)
	{
		this.setDisplayType(face, type, true);
	}

	public void setDisplayType(ForgeDirection face, InfoDisplayType type, boolean markDirty)
	{
		if (type == null)
			type = InfoDisplayType.NONE;
		if (this.faceDisplayTypes == null && type == InfoDisplayType.NONE)
			return;
		InfoDisplayType cur = this.getDisplayType(face);
		if (cur == type)
			return;

		if (this.faceDisplayTypes == null)
			this.faceDisplayTypes = new EnumMap<ForgeDirection, InfoDisplayType>(ForgeDirection.class);

		if (type == InfoDisplayType.NONE)
			this.faceDisplayTypes.remove(face);
		else
			this.faceDisplayTypes.put(face, type);

		if (this.faceDisplayTypes.isEmpty())
			this.faceDisplayTypes = null;
		this.displayTypesDirty = markDirty;
		this.invalidateDisplayInfoCache();
	}

	public void validateDisplayTypes()
	{
		if (this.faceDisplayTypes == null)
			return;
		List<ForgeDirection> reset = new ArrayList<ForgeDirection>();
		for (Entry<ForgeDirection, InfoDisplayType> entry : this.faceDisplayTypes.entrySet())
		{
			BlockCoord bc = this.getLocation().getLocation(entry.getKey());
			Block block = this.worldObj.getBlock(bc.x, bc.y, bc.z);
			if (block != null && (block.isOpaqueCube() || block == EnderIO.blockCapBank))
				reset.add(entry.getKey());
		}
		for (ForgeDirection dir : reset)
		{
			this.setDisplayType(dir, InfoDisplayType.NONE);
			this.setDefaultIoMode(dir);
		}
	}

	private void invalidateDisplayInfoCache()
	{
		if (this.network != null)
			this.network.invalidateDisplayInfoCache();
	}

	//----------- rendering

	@Override
	public boolean shouldRenderInPass(int pass)
	{
		if (this.faceDisplayTypes == null)
			return false;
		return pass == 0;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public AxisAlignedBB getRenderBoundingBox()
	{
		if (!this.type.isMultiblock() || !(this.network instanceof CapBankClientNetwork))
			return super.getRenderBoundingBox();

		int minX = this.xCoord;
		int minY = this.yCoord;
		int minZ = this.zCoord;
		int maxX = this.xCoord + 1;
		int maxY = this.yCoord + 1;
		int maxZ = this.zCoord + 1;

		if (this.faceDisplayTypes != null)
		{
			CapBankClientNetwork cn = (CapBankClientNetwork) this.network;
			if (this.faceDisplayTypes.get(ForgeDirection.NORTH) == InfoDisplayType.IO)
			{
				CapBankClientNetwork.IOInfo info = cn.getIODisplayInfo(this.xCoord, this.yCoord, this.zCoord, ForgeDirection.NORTH);
				maxX = Math.max(maxX, this.xCoord + info.width);
				minY = Math.min(minY, this.yCoord + 1 - info.height);
			}
			if (this.faceDisplayTypes.get(ForgeDirection.SOUTH) == InfoDisplayType.IO)
			{
				CapBankClientNetwork.IOInfo info = cn.getIODisplayInfo(this.xCoord, this.yCoord, this.zCoord, ForgeDirection.SOUTH);
				minX = Math.min(minX, this.xCoord + 1 - info.width);
				minY = Math.min(minY, this.yCoord + 1 - info.height);
			}
			if (this.faceDisplayTypes.get(ForgeDirection.EAST) == InfoDisplayType.IO)
			{
				CapBankClientNetwork.IOInfo info = cn.getIODisplayInfo(this.xCoord, this.yCoord, this.zCoord, ForgeDirection.EAST);
				maxZ = Math.max(maxZ, this.zCoord + info.width);
				minY = Math.min(minY, this.yCoord + 1 - info.height);
			}
			if (this.faceDisplayTypes.get(ForgeDirection.WEST) == InfoDisplayType.IO)
			{
				CapBankClientNetwork.IOInfo info = cn.getIODisplayInfo(this.xCoord, this.yCoord, this.zCoord, ForgeDirection.WEST);
				minZ = Math.min(minZ, this.zCoord + 1 - info.width);
				minY = Math.min(minY, this.yCoord + 1 - info.height);
			}
		}

		return AxisAlignedBB.getBoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
	}

	//----------- Redstone

	public RedstoneControlMode getInputControlMode()
	{
		return this.inputControlMode;
	}

	public void setInputControlMode(RedstoneControlMode inputControlMode)
	{
		this.inputControlMode = inputControlMode;
	}

	public RedstoneControlMode getOutputControlMode()
	{
		return this.outputControlMode;
	}

	public void setOutputControlMode(RedstoneControlMode outputControlMode)
	{
		this.outputControlMode = outputControlMode;
	}

	//----------- Power

	@Override
	public IPowerStorage getController()
	{
		return this.network;
	}

	@Override
	public long getEnergyStoredL()
	{
		if (this.network == null)
			return this.getEnergyStored();
		return this.network.getEnergyStoredL();
	}

	@Override
	public long getMaxEnergyStoredL()
	{
		if (this.network == null)
			return this.getMaxEnergyStored();
		return this.network.getMaxEnergyStoredL();
	}

	@Override
	public boolean isOutputEnabled(ForgeDirection direction)
	{
		IoMode mode = this.getIoMode(direction);
		return mode == IoMode.PUSH || mode == IoMode.NONE && this.isOutputEnabled();
	}

	private boolean isOutputEnabled()
	{
		if (this.network == null)
			return true;
		return this.network.isOutputEnabled();
	}

	@Override
	public boolean isInputEnabled(ForgeDirection direction)
	{
		IoMode mode = this.getIoMode(direction);
		return mode == IoMode.PULL || mode == IoMode.NONE && this.isInputEnabled();
	}

	private boolean isInputEnabled()
	{
		if (this.network == null)
			return true;
		return this.network.isInputEnabled();
	}

	@Override
	public boolean isNetworkControlledIo(ForgeDirection direction)
	{
		IoMode mode = this.getIoMode(direction);
		return mode == IoMode.NONE || mode == IoMode.PULL;
	}

	@Override
	public boolean isCreative()
	{
		return this.getType().isCreative();
	}

	public List<EnergyReceptor> getReceptors()
	{
		if (this.receptorsDirty)
			this.updateReceptors();
		return this.receptors;
	}

	private void updateReceptors()
	{

		if (this.network == null)
			return;
		this.network.removeReceptors(this.receptors);

		this.receptors.clear();
		for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS)
		{
			IPowerInterface pi = this.getReceptorForFace(dir);
			if (pi != null)
			{
				EnergyReceptor er = new EnergyReceptor(this, pi, dir);
				this.validateModeForReceptor(er);
				IoMode ioMode = this.getIoMode(dir);
				if (ioMode != IoMode.DISABLED && ioMode != IoMode.PULL)
					this.receptors.add(er);
			}
		}
		this.network.addReceptors(this.receptors);

		this.receptorsDirty = false;
	}

	private IPowerInterface getReceptorForFace(ForgeDirection faceHit)
	{
		BlockCoord checkLoc = new BlockCoord(this).getLocation(faceHit);
		TileEntity te = this.worldObj.getTileEntity(checkLoc.x, checkLoc.y, checkLoc.z);
		if (!(te instanceof TileCapBank))
			return PowerHandlerUtil.create(te);
		else
		{
			TileCapBank other = (TileCapBank) te;
			if (other.getType() != this.getType())
				return PowerHandlerUtil.create(te);
		}
		return null;
	}

	private EnergyReceptor getEnergyReceptorForFace(ForgeDirection dir)
	{
		IPowerInterface pi = this.getReceptorForFace(dir);
		if (pi == null || pi.getDelegate() instanceof TileCapBank)
			return null;
		return new EnergyReceptor(this, pi, dir);
	}

	private void validateModeForReceptor(ForgeDirection dir)
	{
		this.validateModeForReceptor(this.getEnergyReceptorForFace(dir));
	}

	private void validateModeForReceptor(EnergyReceptor er)
	{
		if (er == null)
			return;
		IoMode ioMode = this.getIoMode(er.getDir());
		if ((ioMode == IoMode.PUSH_PULL || ioMode == IoMode.NONE) && er.getConduit() == null)
			if (er.getReceptor().isOutputOnly())
				this.setIoMode(er.getDir(), IoMode.PULL, false);
			else if (er.getReceptor().isInputOnly())
				this.setIoMode(er.getDir(), IoMode.PUSH, false);
		if (ioMode == IoMode.PULL && er.getReceptor().isInputOnly())
			this.setIoMode(er.getDir(), IoMode.PUSH, false);
		else if (ioMode == IoMode.PUSH && er.getReceptor().isOutputOnly())
			this.setIoMode(er.getDir(), IoMode.DISABLED, false);
	}

	@Override
	public void addEnergy(int energy)
	{
		if (this.network == null)
			this.setEnergyStored(this.getEnergyStored() + energy);
		else
			this.network.addEnergy(energy);
	}

	@Override
	public void setEnergyStored(int stored)
	{
		this.energyStored = MathHelper.clamp_int(stored, 0, this.getMaxEnergyStored());
	}

	@Override
	public int getEnergyStored()
	{
		return this.energyStored;
	}

	@Override
	public int getEnergyStored(ForgeDirection from)
	{
		return this.getEnergyStored();
	}

	@Override
	public int getMaxEnergyStored()
	{
		return this.getType().getMaxEnergyStored();
	}

	@Override
	public int getMaxEnergyRecieved(ForgeDirection dir)
	{
		return this.getMaxInput();
	}

	@Override
	public int getMaxInput()
	{
		if (this.network == null)
			return this.getType().getMaxIO();
		return this.network.getMaxInput();
	}

	public void setMaxInput(int maxInput)
	{
		this.maxInput = maxInput;
	}

	public int getMaxInputOverride()
	{
		return this.maxInput;
	}

	@Override
	public int getMaxOutput()
	{
		if (this.network == null)
			return this.getType().getMaxIO();
		return this.network.getMaxOutput();
	}

	public void setMaxOutput(int maxOutput)
	{
		this.maxOutput = maxOutput;
	}

	public int getMaxOutputOverride()
	{
		return this.maxOutput;
	}

	@Override
	public int receiveEnergy(ForgeDirection from, int maxReceive, boolean simulate)
	{
		if (this.network == null)
			return 0;
		IoMode mode = this.getIoMode(from);
		if (mode == IoMode.DISABLED || mode == IoMode.PUSH)
			return 0;
		return this.network.receiveEnergy(maxReceive, simulate);
	}

	//  @Override
	//  public int extractEnergy(ForgeDirection from, int maxExtract, boolean simulate) {
	//    return 0;
	//  }

	@Override
	public int getMaxEnergyStored(ForgeDirection from)
	{
		return this.getType().getMaxEnergyStored();
	}

	@Override
	public boolean canConnectEnergy(ForgeDirection from)
	{
		return this.getIoMode(from) != IoMode.DISABLED;
	}

	public int getComparatorOutput()
	{
		double stored = this.getEnergyStored();
		return stored == 0 ? 0 : (int) (1 + stored / this.getMaxEnergyStored() * 14);
	}

	@Override
	public boolean displayPower()
	{
		return true;
	}

	//------------------- Inventory

	@Override
	public boolean isUseableByPlayer(EntityPlayer player)
	{
		return this.canPlayerAccess(player);
	}

	@Override
	public ItemStack getStackInSlot(int slot)
	{
		if (this.network == null)
			return null;
		return this.network.getInventory().getStackInSlot(slot);
	}

	@Override
	public ItemStack decrStackSize(int fromSlot, int amount)
	{
		if (this.network == null)
			return null;
		return this.network.getInventory().decrStackSize(fromSlot, amount);
	}

	@Override
	public void setInventorySlotContents(int slot, ItemStack itemstack)
	{
		if (this.network == null)
			return;
		this.network.getInventory().setInventorySlotContents(slot, itemstack);
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int p_70304_1_)
	{
		return null;
	}

	@Override
	public int getSizeInventory()
	{
		return 4;
	}

	@Override
	public String getInventoryName()
	{
		return EnderIO.blockCapacitorBank.getUnlocalizedName() + ".name";
	}

	@Override
	public boolean hasCustomInventoryName()
	{
		return false;
	}

	@Override
	public int getInventoryStackLimit()
	{
		return 1;
	}

	@Override
	public void openInventory()
	{
	}

	@Override
	public void closeInventory()
	{
	}

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack itemstack)
	{
		if (itemstack == null)
			return false;
		return itemstack.getItem() instanceof IEnergyContainerItem;
	}

	public ItemStack[] getInventory()
	{
		return this.inventory;
	}

	public void dropItems()
	{
		this.dropItems = true;
	}

	public void doDropItems()
	{
		if (!this.dropItems)
			return;
		Vector3d dropLocation;
		EntityPlayer player = this.worldObj.getClosestPlayer(this.xCoord, this.yCoord, this.zCoord, 32);
		if (player != null)
			dropLocation = EntityUtil.getEntityPosition(player);
		else
			dropLocation = new Vector3d(this.xCoord, this.yCoord, this.zCoord);
		Util.dropItems(this.worldObj, this.inventory, (int) dropLocation.x, (int) dropLocation.y, (int) dropLocation.z, false);
		for (int i = 0; i < this.inventory.length; i++)
		{
			this.inventory[i] = null;
		}
		this.dropItems = false;
	}

	//---------------- NBT

	@Override
	protected void writeCustomNBT(NBTTagCompound nbtRoot)
	{
		this.writeCommonNBT(nbtRoot);
	}

	//Values common to both item and block form
	public void writeCommonNBT(NBTTagCompound nbtRoot)
	{
		this.getType().writeTypeToNBT(nbtRoot);
		nbtRoot.setInteger(PowerHandlerUtil.STORED_ENERGY_NBT_KEY, this.energyStored);

		if (this.maxInput != -1)
			nbtRoot.setInteger("maxInput", this.maxInput);
		if (this.maxOutput != -1)
			nbtRoot.setInteger("maxOutput", this.maxOutput);
		if (this.inputControlMode != RedstoneControlMode.IGNORE)
			nbtRoot.setShort("inputControlMode", (short) this.inputControlMode.ordinal());
		if (this.outputControlMode != RedstoneControlMode.IGNORE)
			nbtRoot.setShort("outputControlMode", (short) this.outputControlMode.ordinal());

		//face modes
		if (this.faceModes != null)
		{
			nbtRoot.setByte("hasFaces", (byte) 1);
			for (Entry<ForgeDirection, IoMode> e : this.faceModes.entrySet())
			{
				nbtRoot.setShort("face" + e.getKey().ordinal(), (short) e.getValue().ordinal());
			}
		}

		//display type
		if (this.faceDisplayTypes != null)
		{
			nbtRoot.setByte("hasDisplayTypes", (byte) 1);
			for (Entry<ForgeDirection, InfoDisplayType> e : this.faceDisplayTypes.entrySet())
			{
				if (e.getValue() != InfoDisplayType.NONE)
					nbtRoot.setShort("faceDisplay" + e.getKey().ordinal(), (short) e.getValue().ordinal());
			}
		}

		boolean hasItems = false;
		NBTTagList itemList = new NBTTagList();
		for (int i = 0; i < this.inventory.length; i++)
		{
			if (this.inventory[i] != null)
			{
				hasItems = true;
				NBTTagCompound itemStackNBT = new NBTTagCompound();
				itemStackNBT.setByte("Slot", (byte) i);
				this.inventory[i].writeToNBT(itemStackNBT);
				itemList.appendTag(itemStackNBT);
			}
		}
		if (hasItems)
			nbtRoot.setTag("Items", itemList);
	}

	@Override
	protected void readCustomNBT(NBTTagCompound nbtRoot)
	{
		this.readCommonNBT(nbtRoot);
	}

	//Values common to both item and block form
	public void readCommonNBT(NBTTagCompound nbtRoot)
	{
		this.type = CapBankType.readTypeFromNBT(nbtRoot);
		this.energyStored = nbtRoot.getInteger(PowerHandlerUtil.STORED_ENERGY_NBT_KEY);
		this.setEnergyStored(this.energyStored); //Call this to clamp values in case config changed

		if (nbtRoot.hasKey("maxInput"))
			this.maxInput = nbtRoot.getInteger("maxInput");
		else
			this.maxInput = -1;
		if (nbtRoot.hasKey("maxOutput"))
			this.maxOutput = nbtRoot.getInteger("maxOutput");
		else
			this.maxOutput = -1;

		if (nbtRoot.hasKey("inputControlMode"))
			this.inputControlMode = RedstoneControlMode.values()[nbtRoot.getShort("inputControlMode")];
		else
			this.inputControlMode = RedstoneControlMode.IGNORE;
		if (nbtRoot.hasKey("outputControlMode"))
			this.outputControlMode = RedstoneControlMode.values()[nbtRoot.getShort("outputControlMode")];
		else
			this.outputControlMode = RedstoneControlMode.IGNORE;

		if (nbtRoot.hasKey("hasFaces"))
			for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS)
			{
				String key = "face" + dir.ordinal();
				if (nbtRoot.hasKey(key))
					this.setIoMode(dir, IoMode.values()[nbtRoot.getShort(key)], false);
			}
		else
			this.faceModes = null;

		if (nbtRoot.hasKey("hasDisplayTypes"))
			for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS)
			{
				String key = "faceDisplay" + dir.ordinal();
				if (nbtRoot.hasKey(key))
					this.setDisplayType(dir, InfoDisplayType.values()[nbtRoot.getShort(key)], false);
			}
		else
			this.faceDisplayTypes = null;

		for (int i = 0; i < this.inventory.length; i++)
		{
			this.inventory[i] = null;
		}

		if (nbtRoot.hasKey("Items"))
		{
			NBTTagList itemList = (NBTTagList) nbtRoot.getTag("Items");
			for (int i = 0; i < itemList.tagCount(); i++)
			{
				NBTTagCompound itemStack = itemList.getCompoundTagAt(i);
				byte slot = itemStack.getByte("Slot");
				if (slot >= 0 && slot < this.inventory.length)
					this.inventory[slot] = ItemStack.loadItemStackFromNBT(itemStack);
			}
		}
	}

}
