package eu.thesociety.DragonbornSR.DragonsRadioMod.network.Message;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import eu.thesociety.DragonbornSR.DragonsRadioMod.Block.TileEntity.TileEntityRadio;
import io.netty.buffer.ByteBuf;
import net.minecraft.world.World;

public class MessageTERadioBlock implements IMessage
{
	// TODO gamerforEA code start
	private static final int MAX_URL_LENGTH = 1000;
	private static final int MAX_URL_LENGTH_IN_BYTES = MAX_URL_LENGTH * 4;
	// TODO gamerforEA code end

	public double x;
	public double y;
	public double z;
	public int mode;
	public int dim;
	public double tx = 0.0D;
	public double ty = 0.0D;
	public double tz = 0.0D;
	public String streamURL;
	public boolean isPlaying;
	public float volume;
	public World world;

	public MessageTERadioBlock()
	{
	}

	public MessageTERadioBlock(TileEntityRadio radio)
	{
		this.x = radio.xCoord;
		this.y = radio.yCoord;
		this.z = radio.zCoord;
		this.world = radio.getWorldObj();
		this.dim = this.world.provider.dimensionId;
		this.streamURL = radio.streamURL;
		this.isPlaying = radio.isPlaying;
		this.volume = radio.volume;
		int mode = 13;
		if (radio.listenToRedstone)
			mode = 14;

		this.mode = mode;
	}

	public MessageTERadioBlock(double x, double y, double z, World world, String streamURL, boolean isPlaying, float volume, int mode)
	{
		this.x = x;
		this.y = y;
		this.z = z;
		this.world = world;
		this.dim = world.provider.dimensionId;
		this.streamURL = streamURL;
		this.isPlaying = isPlaying;
		this.volume = volume;
		this.mode = mode;
	}

	public MessageTERadioBlock(double x, double y, double z, World world, String streamURL, boolean isPlaying, float volume, int mode, double tx, double ty, double tz)
	{
		this.x = x;
		this.y = y;
		this.z = z;
		this.world = world;
		this.dim = world.provider.dimensionId;
		this.streamURL = streamURL;
		this.isPlaying = isPlaying;
		this.volume = volume;
		this.mode = mode;
		this.tx = tx;
		this.ty = ty;
		this.tz = tz;
	}

	@Override
	public void fromBytes(ByteBuf buf)
	{
		this.mode = buf.readInt();
		this.x = buf.readDouble();
		this.y = buf.readDouble();
		this.z = buf.readDouble();
		this.dim = buf.readInt();
		int streamURLlenght = buf.readInt();

		// TODO gamerforEA code start
		if (streamURLlenght > MAX_URL_LENGTH_IN_BYTES)
			throw new IllegalArgumentException("The URL is too long");
		// TODO gamerforEA code end

		String streamURL = new String(buf.readBytes(streamURLlenght).array());

		// TODO gamerforEA code start
		if (streamURL.length() > MAX_URL_LENGTH)
			streamURL = streamURL.substring(0, MAX_URL_LENGTH);
		// TODO gamerforEA code end

		this.streamURL = streamURL;
		this.isPlaying = buf.readBoolean();
		this.volume = buf.readFloat();
		this.tx = buf.readDouble();
		this.ty = buf.readDouble();
		this.tz = buf.readDouble();
	}

	@Override
	public void toBytes(ByteBuf buf)
	{
		buf.writeInt(this.mode);
		buf.writeDouble(this.x);
		buf.writeDouble(this.y);
		buf.writeDouble(this.z);
		buf.writeInt(this.dim);
		buf.writeInt(this.streamURL.length());
		buf.writeBytes(this.streamURL.getBytes());
		buf.writeBoolean(this.isPlaying);
		buf.writeFloat(this.volume);
		buf.writeDouble(this.tx);
		buf.writeDouble(this.ty);
		buf.writeDouble(this.tz);
	}
}
