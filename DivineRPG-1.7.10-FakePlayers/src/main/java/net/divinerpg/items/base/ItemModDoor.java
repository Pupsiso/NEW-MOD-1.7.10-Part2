package net.divinerpg.items.base;

import com.gamerforea.eventhelper.util.EventUtils;
import net.divinerpg.libs.Reference;
import net.divinerpg.utils.tabs.DivineRPGTabs;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemDoor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

public class ItemModDoor extends ItemMod
{

	private Block door;

	public ItemModDoor(Block block, String name)
	{
		super(name);
		this.door = block;
		this.setCreativeTab(DivineRPGTabs.blocks);
		this.setTextureName(Reference.PREFIX + name);
		this.setUnlocalizedName(name);
	}

	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float xOffset, float yOffset, float zOffset)
	{
		if (side != 1)
			return false;

		if (!world.isRemote)
		{
			++y;
			Block block = this.door;

			// TODO gamerforEA code start
			if (!player.canPlayerEdit(x, y, z, side, stack))
				return false;
			if (!player.canPlayerEdit(x, y + 1, z, side, stack))
				return false;
			if (!block.canPlaceBlockAt(world, x, y, z))
				return false;
			if (!EventUtils.cantBreak(player, x, y, z))
				return false;
			if (!EventUtils.cantBreak(player, x, y + 1, z))
				return false;
			// TODO gamerforEA code end

			int rotation = MathHelper.floor_double((double) ((player.rotationYaw + 180.0F) * 4.0F / 360.0F) - 0.5D) & 3;
			ItemDoor.placeDoorBlock(world, x, y, z, rotation, block);
			--stack.stackSize;
			return true;
		}

		return false;
	}
}