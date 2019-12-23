package com.carpentersblocks.network;

import com.carpentersblocks.block.BlockCoverable;
import com.carpentersblocks.util.BlockProperties;
import com.gamerforea.eventhelper.util.EventUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.io.IOException;

public class PacketActivateBlock extends TilePacket
{

	private int side;

	public PacketActivateBlock()
	{
	}

	public PacketActivateBlock(int x, int y, int z, int side)
	{
		super(x, y, z);
		this.side = side;
	}

	@Override
	public void processData(EntityPlayer player, ByteBufInputStream bbis) throws IOException
	{
		super.processData(player, bbis);

		this.side = bbis.readInt();
		World world = player.worldObj;
		int x = this.x;
		int y = this.y;
		int z = this.z;
		ItemStack heldItem = player.getHeldItem();

		// TODO gamerforEA code start
		if (heldItem != null && heldItem.getItem() instanceof ItemBlock && !BlockProperties.isOverlay(heldItem))
			return;
		if (player.getDistanceSq(x, y, z) > 64)
			return;
		if (!world.blockExists(x, y, z))
			return;
		// TODO gamerforEA code end

		Block block = world.getBlock(x, y, z);

		// TODO gamerforEA code start
		if (!(block instanceof BlockCoverable))
			return;
		if (EventUtils.cantBreak(player, x, y, z))
			return;
		// TODO gamerforEA code end

		boolean result = block.onBlockActivated(world, x, y, z, player, this.side, 1.0F, 1.0F, 1.0F);

		/* TODO gamerforEA replace, old code:
		if (!result && heldItem != null && heldItem.getItem() instanceof ItemBlock)
		{
			heldItem.tryPlaceItemIntoWorld(player, world, x, y, z, this.side, 1.0F, 1.0F, 1.0F);
			EntityLivingUtil.decrementCurrentSlot(player);
		} */
	}

	@Override
	public void appendData(ByteBuf buffer) throws IOException
	{
		super.appendData(buffer);
		buffer.writeInt(this.side);
	}

}
