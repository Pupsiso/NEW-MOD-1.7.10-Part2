package crazypants.enderio.enchantment;

import com.enderio.core.api.common.enchant.IAdvancedEnchant;
import com.gamerforea.enderio.EventConfig;
import com.gamerforea.enderio.ExtendedPlayer;
import com.google.common.collect.Iterables;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import crazypants.enderio.EnderIO;
import crazypants.enderio.config.Config;
import crazypants.util.BaublesUtil;
import crazypants.util.GalacticraftUtil;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.ListIterator;

public class EnchantmentSoulBound extends Enchantment implements IAdvancedEnchant
{

	public static EnchantmentSoulBound create(int id)
	{
		EnchantmentSoulBound res = new EnchantmentSoulBound(id);
		MinecraftForge.EVENT_BUS.register(res);

		// TODO gamerforEA code start
		if (EventConfig.alternativeSoulBoundRestoration)
			FMLCommonHandler.instance().bus().register(res);
		// TODO gamerforEA code end

		return res;
	}

	private final int id;

	private EnchantmentSoulBound(int id)
	{
		super(id, Config.enchantmentSoulBoundWeight, EnumEnchantmentType.all);
		this.id = id;
		this.setName("enderio.soulBound");
	}

	@Override
	public int getMaxEnchantability(int level)
	{
		return super.getMaxEnchantability(level) + 30;
	}

	@Override
	public int getMinEnchantability(int level)
	{
		return super.getMinEnchantability(level);
	}

	@Override
	public int getMaxLevel()
	{
		return 1;
	}

	// TODO gamerforEA code start
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onPlayerRespawn(cpw.mods.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent event)
	{
		if (EventConfig.alternativeSoulBoundRestoration)
		{
			EntityPlayer player = event.player;
			ExtendedPlayer extendedPlayer = ExtendedPlayer.get(player);

			for (ExtendedPlayer.SlotContent slotContent : Iterables.concat(extendedPlayer.inventoryContent, extendedPlayer.baublesContent, extendedPlayer.galacticraftContent))
			{
				if (this.isSoulBound(slotContent.stack))
					this.addToPlayerInventory(player, slotContent.stack);
			}

			extendedPlayer.inventoryContent.clear();
			extendedPlayer.baublesContent.clear();
			extendedPlayer.galacticraftContent.clear();
		}
	}
	// TODO gamerforEA code end

	/*
	 * This is called the moment the player dies and drops his stuff.
	 *
	 * We go early, so we can get our items before other mods put them into some
	 * grave. Also remove them from the list so they won't get duped. If the
	 * inventory overflows, e.g. because everything there and the armor is
	 * soulbound, let the remainder be dropped/graved.
	 */
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onPlayerDeath(PlayerDropsEvent event)
	{
		EntityPlayer player = event.entityPlayer;
		if (player == null || player instanceof FakePlayer || event.isCanceled())
			return;
		if (player.worldObj.getGameRules().getGameRuleBooleanValue("keepInventory"))
			return;

		// TODO gamerforEA code start
		if (EventConfig.alternativeSoulBoundRestoration)
		{
			ExtendedPlayer extendedPlayer = ExtendedPlayer.get(player);
			extendedPlayer.inventoryContent.clear();
			extendedPlayer.baublesContent.clear();
			extendedPlayer.galacticraftContent.clear();

			int slot = 0;
			for (ListIterator<EntityItem> iterator = event.drops.listIterator(); iterator.hasNext(); )
			{
				EntityItem entityItem = iterator.next();
				ItemStack stack = entityItem.getEntityItem();
				if (this.isSoulBound(stack) && extendedPlayer.inventoryContent.add(new ExtendedPlayer.SlotContent(slot++, stack.copy())))
					iterator.remove();
			}

			IInventory baubles = BaublesUtil.instance().getBaubles(player);
			if (baubles != null)
				for (slot = 0; slot < baubles.getSizeInventory(); slot++)
				{
					ItemStack stack = baubles.getStackInSlot(slot);
					if (this.isSoulBound(stack) && extendedPlayer.baublesContent.add(new ExtendedPlayer.SlotContent(slot, stack.copy())))
						baubles.setInventorySlotContents(slot, null);
				}

			if (player instanceof EntityPlayerMP)
			{
				IInventory galacticraft = GalacticraftUtil.getGCInventoryForPlayer((EntityPlayerMP) player);
				if (galacticraft != null)
					for (slot = 0; slot < galacticraft.getSizeInventory(); slot++)
					{
						ItemStack stack = galacticraft.getStackInSlot(slot);
						if (this.isSoulBound(stack) && extendedPlayer.galacticraftContent.add(new ExtendedPlayer.SlotContent(slot, stack.copy())))
							galacticraft.setInventorySlotContents(slot, null);
					}
			}

			return;
		}
		// TODO gamerforEA code end

		for (ListIterator<EntityItem> iterator = event.drops.listIterator(); iterator.hasNext(); )
		{
			EntityItem entityItem = iterator.next();
			ItemStack stack = entityItem.getEntityItem();
			if (this.isSoulBound(stack) && this.addToPlayerInventory(player, stack))
				iterator.remove();
		}

		// Note: Baubles will also add its items to event.drops, but later. We cannot
		// wait for that because gravestone mods also listen to this event. So we have
		// to fetch Baubles items ourselves here.
		// For the same reason we cannot put the items into Baubles slots.
		IInventory baubles = BaublesUtil.instance().getBaubles(player);
		if (baubles != null)
			for (int slot = 0; slot < baubles.getSizeInventory(); slot++)
			{
				ItemStack item = baubles.getStackInSlot(slot);
				if (this.isSoulBound(item) && this.addToPlayerInventory(player, item))
					baubles.setInventorySlotContents(slot, null);
			}

		// Galacticraft. Again we are too early for those items. We just dump the
		// stuff into the normal inventory to not have to keep a separate list.
		if (player instanceof EntityPlayerMP)
		{
			IInventory galacticraft = GalacticraftUtil.getGCInventoryForPlayer((EntityPlayerMP) player);
			if (galacticraft != null)
				for (int slot = 0; slot < galacticraft.getSizeInventory(); slot++)
				{
					ItemStack item = galacticraft.getStackInSlot(slot);
					if (this.isSoulBound(item) && this.addToPlayerInventory(player, item))
						galacticraft.setInventorySlotContents(slot, null);
				}
		}

	}

	/*
	 * This is called when the user presses the "respawn" button. The original
	 * inventory would be empty, but onPlayerDeath() above placed items in it.
	 *
	 * Note: Without other death-modifying mods, the content of the old inventory
	 * would always fit into the new one (both being empty but for soulbound items
	 * in the old one) and the old one would be discarded just after this method.
	 * But better play it safe and assume that an overflow is possible and that
	 * another mod may move stuff out of the old inventory, too.
	 */
	@SubscribeEvent
	public void onPlayerClone(PlayerEvent.Clone event)
	{
		if (!event.wasDeath || event.isCanceled())
			return;
		if (event.original == null || event.entityPlayer == null || event.entityPlayer instanceof FakePlayer)
			return;
		if (event.entityPlayer.worldObj.getGameRules().getGameRuleBooleanValue("keepInventory"))
			return;

		// TODO gamerforEA code start
		if (EventConfig.alternativeSoulBoundRestoration)
			return;
		// TODO gamerforEA code end

		for (int slot = 0; slot < event.original.inventory.mainInventory.length; slot++)
		{
			ItemStack item = event.original.inventory.mainInventory[slot];
			if (this.isSoulBound(item) && this.addToPlayerInventory(event.entityPlayer, item))
				event.original.inventory.mainInventory[slot] = null;
		}
		for (int armorSlot = 0; armorSlot < event.original.inventory.armorInventory.length; armorSlot++)
		{
			ItemStack item = event.original.inventory.armorInventory[armorSlot];
			if (this.isSoulBound(item) && this.addToPlayerInventory(event.entityPlayer, item))
				event.original.inventory.armorInventory[armorSlot] = null;
		}
	}

	private boolean isSoulBound(ItemStack item)
	{
		// TODO gamerforEA code start
		if (item == null || item.stackSize <= 0)
			return false;
		// TODO gamerforEA code end

		return EnchantmentHelper.getEnchantmentLevel(this.id, item) > 0;
	}

	private boolean addToPlayerInventory(EntityPlayer player, ItemStack item)
	{
		if (item == null || player == null)
			return false;
		if (item.getItem() instanceof ItemArmor)
		{
			ItemArmor arm = (ItemArmor) item.getItem();
			int armorSlot = 3 - arm.armorType;
			if (player.inventory.armorItemInSlot(armorSlot) == null)
			{
				// TODO gamerforEA code replace, old code:
				// player.inventory.armorInventory[armorSlot] = item;
				player.inventory.armorInventory[armorSlot] = item.copy();
				// TODO gamerforEA code end

				return true;
			}
		}

		InventoryPlayer inv = player.inventory;
		for (int slot = 0; slot < inv.mainInventory.length; slot++)
		{
			if (inv.mainInventory[slot] == null)
			{
				inv.mainInventory[slot] = item.copy();
				return true;
			}
		}

		return false;
	}

	@Override
	public String[] getTooltipDetails(ItemStack stack)
	{
		return new String[] { EnderIO.lang.localizeExact("description.enchantment.enderio.soulBound") };
	}
}
