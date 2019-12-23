package com.carpentersblocks.tileentity;

import com.carpentersblocks.data.GarageDoor;
import com.gamerforea.carpentersblocks.ModUtils;
import com.gamerforea.eventhelper.fake.FakePlayerContainer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;

public class TECarpentersGarageDoor extends TEBase
{
	// TODO gamerforEA code start
	public final FakePlayerContainer fake = ModUtils.NEXUS_FACTORY.wrapFake(this);

	@Override
	public void writeToNBT(NBTTagCompound nbt)
	{
		super.writeToNBT(nbt);
		this.fake.writeToNBT(nbt);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt)
	{
		super.readFromNBT(nbt);
		this.fake.readFromNBT(nbt);
	}
	// TODO gamerforEA code end

	@Override
	/**
	 * Garage door state change sounds are handled strictly client-side so that
	 * only the nearest state change is audible.
	 *
	 * @param net
	 *            The NetworkManager the packet originated from
	 * @param pkt
	 *            The data packet
	 */ public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt)
	{
		if (this.getWorldObj().isRemote)
		{
			GarageDoor data = GarageDoor.INSTANCE;
			int oldState = data.getState(this);
			super.onDataPacket(net, pkt);
			if (data.getState(this) != oldState)
				data.playStateChangeSound(this);
		}
	}
}
