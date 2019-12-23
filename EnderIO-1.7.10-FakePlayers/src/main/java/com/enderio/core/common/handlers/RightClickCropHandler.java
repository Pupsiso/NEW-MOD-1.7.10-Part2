package com.enderio.core.common.handlers;

import com.enderio.core.common.Handlers.Handler;
import com.enderio.core.common.config.ConfigHandler;
import com.enderio.core.common.util.ItemUtil;
import com.gamerforea.eventhelper.util.EventUtils;
import com.google.common.collect.Lists;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.Action;
import net.minecraftforge.event.world.BlockEvent.HarvestDropsEvent;
import net.minecraftforge.oredict.OreDictionary;

import java.util.List;

@Handler
public class RightClickCropHandler
{
	public static class PlantInfo
	{
		public String seed;
		public String block;
		public int meta = 7;
		public int resetMeta = 0;

		private transient ItemStack seedStack;
		private transient Block blockInst;

		public PlantInfo()
		{
		}

		public PlantInfo(String seed, String block, int meta, int resetMeta)
		{
			this.seed = seed;
			this.block = block;
			this.meta = meta;
			this.resetMeta = resetMeta;
		}

		public void init()
		{
			this.seedStack = ItemUtil.parseStringIntoItemStack(this.seed);
			String[] blockinfo = this.block.split(":");
			this.blockInst = GameRegistry.findBlock(blockinfo[0], blockinfo[1]);
		}
	}

	private List<PlantInfo> plants = Lists.newArrayList();

	private PlantInfo currentPlant = null;

	public static final RightClickCropHandler INSTANCE = new RightClickCropHandler();

	private RightClickCropHandler()
	{
	}

	public void addCrop(PlantInfo info)
	{
		this.plants.add(info);
	}

	@SubscribeEvent
	public void handleCropRightClick(PlayerInteractEvent event)
	{
		if (ConfigHandler.allowCropRC && event.action == Action.RIGHT_CLICK_BLOCK)
		{
			EntityPlayer player = event.entityPlayer;
			if (player.getHeldItem() == null || !player.isSneaking())
			{
				int x = event.x;
				int y = event.y;
				int z = event.z;
				Block block = event.world.getBlock(x, y, z);
				int meta = event.world.getBlockMetadata(x, y, z);
				for (PlantInfo info : this.plants)
				{
					if (info.blockInst == block && meta == info.meta)
					{
						if (event.world.isRemote)
							player.swingItem();
						else
						{
							// TODO gamerforEA code start
							if (EventUtils.cantBreak(player, x, y, z))
								break;
							// TODO gamerforEA code end

							this.currentPlant = info;
							block.dropBlockAsItem(event.world, x, y, z, meta, 0);
							this.currentPlant = null;
							event.world.setBlockMetadataWithNotify(x, y, z, info.resetMeta, 3);
							event.setCanceled(true);
						}
						break;
					}
				}
			}
		}
	}

	@SubscribeEvent
	public void onHarvestDrop(HarvestDropsEvent event)
	{
		if (this.currentPlant != null)
			for (int i = 0; i < event.drops.size(); i++)
			{
				ItemStack stack = event.drops.get(i);
				if (stack.getItem() == this.currentPlant.seedStack.getItem())
				{
					int seedDamage = this.currentPlant.seedStack.getItemDamage();
					if (seedDamage == OreDictionary.WILDCARD_VALUE || seedDamage == stack.getItemDamage())
					{
						event.drops.remove(i);
						break;
					}
				}
			}
	}
}
