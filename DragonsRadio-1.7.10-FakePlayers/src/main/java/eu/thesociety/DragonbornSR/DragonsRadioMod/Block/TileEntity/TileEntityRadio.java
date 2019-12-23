package eu.thesociety.DragonbornSR.DragonsRadioMod.Block.TileEntity;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import eu.thesociety.DragonbornSR.DragonsRadioMod.Block.BlockSpeaker;
import eu.thesociety.DragonbornSR.DragonsRadioMod.ModRadioBlock;
import eu.thesociety.DragonbornSR.DragonsRadioMod.misc.Speaker;
import eu.thesociety.DragonbornSR.DragonsRadioMod.network.Message.MessageTERadioBlock;
import eu.thesociety.DragonbornSR.DragonsRadioMod.network.PacketHandler;
import eu.thesociety.DragonbornSR.DragonsRadioMod.player.MP3Player;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class TileEntityRadio extends TileEntity
{
	private MP3Player player = null;
	public boolean isPlaying = false;
	public String streamURL = "";
	public boolean blockExists = true;
	private World world;
	public float volume = 0.1F;
	private boolean redstoneInput = false;
	public boolean listenToRedstone = false;
	private boolean scheduledRedstoneInput = false;
	private boolean scheduleRedstoneInput = false;
	public ArrayList<Speaker> speakers = new ArrayList();
	private int th = 0;
	double cx = 0.0D;
	double cy = 0.0D;
	double cz = 0.0D;
	private int speakersCount = 0;

	// TODO gamerforEA code start
	private static final long MAX_WAIT_TIME = TimeUnit.MINUTES.toMillis(5);
	private UUID lastPlayerId;
	private long lastAccessTime;

	public void setLastPlayer(EntityPlayer player)
	{
		if (player == null)
		{
			this.lastPlayerId = null;
			this.lastAccessTime = 0;
		}
		else
		{
			this.lastPlayerId = player.getUniqueID();
			this.lastAccessTime = System.currentTimeMillis();
		}
	}

	public UUID getLastPlayerId()
	{
		if (this.lastPlayerId == null)
			return null;

		if (System.currentTimeMillis() - this.lastAccessTime > MAX_WAIT_TIME)
		{
			this.lastPlayerId = null;
			this.lastAccessTime = 0;
			return null;
		}

		return this.lastPlayerId;
	}
	// TODO gamerforEA code end

	public TileEntityRadio(World w)
	{
		this.world = w;
		if (this.isPlaying)
			this.startStream();

	}

	public TileEntityRadio()
	{
		if (this.isPlaying)
			this.startStream();

	}

	public void deleted()
	{
		this.blockExists = false;
	}

	public void setWorld(World w)
	{
		this.world = w;
	}

	public void startStream()
	{
		Side side = FMLCommonHandler.instance().getEffectiveSide();
		if (!ModRadioBlock.playerList.contains(this.player))
		{
			this.isPlaying = true;
			if (side == Side.CLIENT)
			{
				this.player = new MP3Player(this.streamURL, this.world, this.xCoord, this.yCoord, this.zCoord);
				ModRadioBlock.playerList.add(this.player);
			}
		}

	}

	public void stopStream()
	{
		Side side = FMLCommonHandler.instance().getEffectiveSide();
		if (ModRadioBlock.playerList.contains(this.player))
		{
			if (side == Side.CLIENT)
				this.player.stop();

			ModRadioBlock.playerList.remove(this.player);
			this.isPlaying = false;
		}

	}

	public boolean isPlaying()
	{
		return this.isPlaying;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void invalidate()
	{
		this.stopStream();
		super.invalidate();
	}

	@Override
	public void updateEntity()
	{
		Side side = FMLCommonHandler.instance().getEffectiveSide();
		if (side == Side.CLIENT)
		{
			++this.th;
			if (this.th >= 10)
			{
				this.th = 0;

				for (Speaker s : this.speakers)
				{
					Block sb = this.getWorldObj().getBlock((int) s.x, (int) s.y, (int) s.z);
					if (!(sb instanceof BlockSpeaker))
					{
						if (this.getWorldObj().getChunkFromBlockCoords((int) s.x, (int) s.z).isChunkLoaded)
							this.speakers.remove(s);
						break;
					}
				}
			}

			if (Minecraft.getMinecraft().thePlayer != null && this.player != null && !this.isInvalid())
			{
				float vol = this.getClosest();
				if (vol > 10000.0F * this.volume)
					this.player.setVolume(0.0F);
				else
				{
					float v2 = 10000.0F / vol / 100.0F;
					if (v2 > 1.0F)
						this.player.setVolume(1.0F * this.volume * this.volume);
					else
						this.player.setVolume(v2 * this.volume * this.volume);
				}

				if (vol == 0.0F)
					this.invalidate();
			}
		}
		else
		{
			if (this.isPlaying())
			{
				++this.th;
				if (this.th >= 60)
				{
					this.th = 0;

					for (Speaker s : this.speakers)
					{
						if (!(this.worldObj.getBlock((int) s.x, (int) s.y, (int) s.z) instanceof BlockSpeaker))
						{
							if (this.worldObj.getChunkFromBlockCoords((int) s.x, (int) s.z).isChunkLoaded)
								this.speakers.remove(s);
							break;
						}
					}
				}
			}

			if (this.scheduleRedstoneInput && this.listenToRedstone)
			{
				if (!this.scheduledRedstoneInput && this.redstoneInput)
				{
					this.isPlaying = !this.isPlaying;
					PacketHandler.INSTANCE.sendToAll(new MessageTERadioBlock(this.xCoord, this.yCoord, this.zCoord, this
							.getWorldObj(), this.streamURL, this.isPlaying, this.volume, 1));
				}

				this.redstoneInput = this.scheduledRedstoneInput;
				this.scheduleRedstoneInput = false;
				this.scheduledRedstoneInput = false;
			}
		}

	}

	@Override
	public void readFromNBT(NBTTagCompound nbt)
	{
		super.readFromNBT(nbt);
		this.streamURL = nbt.getString("streamurl");
		this.volume = nbt.getFloat("volume");
		this.listenToRedstone = nbt.getBoolean("input");
		this.redstoneInput = nbt.getBoolean("lastInput");
		this.isPlaying = nbt.getBoolean("lastState");
		this.speakersCount = nbt.getInteger("speakersCount");

		for (int i = 0; i < this.speakersCount; ++i)
		{
			double x = nbt.getDouble("speakerX" + i);
			double y = nbt.getDouble("speakerY" + i);
			double z = nbt.getDouble("speakerZ" + i);
			this.addSpeaker(this.getWorldObj(), x, y, z);
		}
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt)
	{
		super.writeToNBT(nbt);
		nbt.setString("streamurl", this.streamURL);
		nbt.setFloat("volume", this.volume);
		nbt.setBoolean("input", this.listenToRedstone);
		nbt.setBoolean("lastInput", this.redstoneInput);
		nbt.setBoolean("lastState", this.isPlaying);
		nbt.setInteger("speakersCount", this.speakers.size());

		for (int i = 0; i < this.speakers.size(); ++i)
		{
			nbt.setDouble("speakerX" + i, this.speakers.get(i).x);
			nbt.setDouble("speakerY" + i, this.speakers.get(i).y);
			nbt.setDouble("speakerZ" + i, this.speakers.get(i).z);
		}
	}

	@Override
	public Packet getDescriptionPacket()
	{
		for (Speaker s : this.speakers)
		{
			PacketHandler.INSTANCE.sendToDimension(new MessageTERadioBlock(this.xCoord, this.yCoord, this.zCoord, this.worldObj, "", false, 1.0F, 15, s.x, s.y, s.z), this
					.getWorldObj().provider.dimensionId);
		}

		int mode = 13;
		if (this.listenToRedstone)
			mode = 14;

		return PacketHandler.INSTANCE.getPacketFrom(new MessageTERadioBlock(this));
	}

	public boolean getListenRedstoneInput()
	{
		return this.listenToRedstone;
	}

	public void setRedstoneInput(boolean input)
	{
		if (input)
			this.scheduledRedstoneInput = input;

		this.scheduleRedstoneInput = true;
	}

	private boolean checkIfSpeakerExists(World w, int x, int y, int z)
	{
		return w.getBlock(x, y, z) instanceof BlockSpeaker;
	}

	public int addSpeaker(World w, double x, double y, double z)
	{
		if (this.speakers.size() >= 10)
			return 1;

		for (Speaker s : this.speakers)
		{
			// TODO gamerforEA code replace, old code:
			// if (s.x == x && s.y == y && s.z == z)
			if (s.isSamePos((int) x, (int) y, (int) z))
				// TODO gamerforEA code end
				return 2;
		}

		this.speakers.add(new Speaker(x, y, z, w));
		return 0;
	}

	public int canAddSpeaker(World w, double x, double y, double z)
	{
		if (this.speakers.size() >= 10)
			return 1;

		for (Speaker s : this.speakers)
		{
			// TODO gamerforEA code replace, old code:
			// if (s.x == x && s.y == y && s.z == z)
			if (s.isSamePos((int) x, (int) y, (int) z))
				// TODO gamerforEA code end
				return 2;
		}

		return 0;
	}

	// TODO gamerforEA code start
	@SideOnly(Side.CLIENT)
	// TODO gamerforEA code end
	private float getClosest()
	{
		EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
		float closest = (float) this.getDistanceFrom(player.posX, player.posY, player.posZ);
		if (!this.speakers.isEmpty())
			for (Speaker s : this.speakers)
			{
				float distance = (float) Math.pow(player.getDistance(s.x, s.y, s.z), 2.0D);
				if (closest > distance)
					closest = distance;
			}

		return closest;
	}
}
