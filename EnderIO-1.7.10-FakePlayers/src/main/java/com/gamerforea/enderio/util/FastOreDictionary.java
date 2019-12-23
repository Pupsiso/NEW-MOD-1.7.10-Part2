package com.gamerforea.enderio.util;

import com.gamerforea.enderio.EventConfig;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;

public final class FastOreDictionary
{
	private static final boolean CACHE_ENABLED = EventConfig.cacheOreIDs;
	private static final Cache<ItemStackKey, int[]> ORE_IDS_CACHE = CacheBuilder.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES).build();

	/**
	 * Gets all the integer ID for the ores that the specified item stakc is registered to.
	 * If the item stack is not linked to any ore, this will return an empty array and no new entry will be created.
	 *
	 * @param stack The item stack of the ore.
	 * @return An array of ids that this ore is registerd as.
	 */
	@Nonnull
	public static int[] getOreIDsCopy(ItemStack stack)
	{
		if (!CACHE_ENABLED)
			return OreDictionary.getOreIDs(stack);

		int[] oreIDs = getOreIDs(stack);
		return oreIDs.length == 0 ? oreIDs : oreIDs.clone();
	}

	/**
	 * Gets all the integer ID for the ores that the specified item stakc is registered to.
	 * If the item stack is not linked to any ore, this will return an empty array and no new entry will be created.
	 *
	 * @param stack The item stack of the ore.
	 * @return An array of ids that this ore is registerd as.
	 */
	@Nonnull
	public static int[] getOreIDs(ItemStack stack)
	{
		if (!CACHE_ENABLED)
			return OreDictionary.getOreIDs(stack);

		if (stack == null)
			return ArrayUtils.EMPTY_INT_ARRAY;

		Item item = stack.getItem();
		if (item == null)
			return ArrayUtils.EMPTY_INT_ARRAY;

		ItemStackKey key = new ItemStackKey(item, stack.getItemDamage());
		int[] oreIDs = ORE_IDS_CACHE.getIfPresent(key);
		if (oreIDs != null)
			return oreIDs;

		oreIDs = OreDictionary.getOreIDs(stack);
		if (oreIDs.length == 0)
			oreIDs = ArrayUtils.EMPTY_INT_ARRAY;

		ORE_IDS_CACHE.put(key, oreIDs);
		return oreIDs;
	}

	private static final class ItemStackKey
	{
		public final Item item;
		public final int damage;

		public ItemStackKey(Item item, int damage)
		{
			this.item = item;
			this.damage = damage;
		}

		@Override
		public boolean equals(Object o)
		{
			if (this == o)
				return true;
			if (o == null || this.getClass() != o.getClass())
				return false;
			ItemStackKey that = (ItemStackKey) o;
			return this.damage == that.damage && this.item.equals(that.item);
		}

		@Override
		public int hashCode()
		{
			return 31 * this.item.hashCode() + this.damage;
		}
	}
}
