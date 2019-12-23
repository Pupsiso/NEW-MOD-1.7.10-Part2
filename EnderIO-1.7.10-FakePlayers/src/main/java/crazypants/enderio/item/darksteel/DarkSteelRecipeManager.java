package crazypants.enderio.item.darksteel;

import com.enderio.core.common.util.OreDictionaryHelper;
import com.gamerforea.enderio.EventConfig;
import com.google.common.collect.ImmutableList;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import crazypants.enderio.EnderIO;
import crazypants.enderio.item.darksteel.upgrade.*;
import crazypants.enderio.material.Alloy;
import crazypants.enderio.thaumcraft.ThaumcraftCompat;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.event.AnvilUpdateEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DarkSteelRecipeManager
{

	public static DarkSteelRecipeManager instance = new DarkSteelRecipeManager();

	private List<IDarkSteelUpgrade> upgrades = new ArrayList<IDarkSteelUpgrade>();

	public DarkSteelRecipeManager()
	{
		this.upgrades.add(EnergyUpgrade.EMPOWERED);
		this.upgrades.add(EnergyUpgrade.EMPOWERED_TWO);
		this.upgrades.add(EnergyUpgrade.EMPOWERED_THREE);
		this.upgrades.add(EnergyUpgrade.EMPOWERED_FOUR);
		this.upgrades.add(JumpUpgrade.JUMP_ONE);
		this.upgrades.add(JumpUpgrade.JUMP_TWO);
		this.upgrades.add(JumpUpgrade.JUMP_THREE);
		this.upgrades.add(SpeedUpgrade.SPEED_ONE);
		this.upgrades.add(SpeedUpgrade.SPEED_TWO);
		this.upgrades.add(SpeedUpgrade.SPEED_THREE);
		this.upgrades.add(GliderUpgrade.INSTANCE);
		this.upgrades.add(SoundDetectorUpgrade.INSTANCE);
		this.upgrades.add(SwimUpgrade.INSTANCE);
		this.upgrades.add(NightVisionUpgrade.INSTANCE);
		this.upgrades.add(TravelUpgrade.INSTANCE);
		this.upgrades.add(SpoonUpgrade.INSTANCE);
		this.upgrades.add(SolarUpgrade.SOLAR_ONE);
		this.upgrades.add(SolarUpgrade.SOLAR_TWO);
		if (Loader.isModLoaded("Thaumcraft"))
			ThaumcraftCompat.loadUpgrades(this.upgrades);
		if (Loader.isModLoaded("Forestry"))
		{
			this.upgrades.add(NaturalistEyeUpgrade.INSTANCE);
			this.upgrades.add(ApiaristArmorUpgrade.HELMET);
			this.upgrades.add(ApiaristArmorUpgrade.CHEST);
			this.upgrades.add(ApiaristArmorUpgrade.LEGS);
			this.upgrades.add(ApiaristArmorUpgrade.BOOTS);
		}
	}

	@SubscribeEvent
	public void handleAnvilEvent(AnvilUpdateEvent evt)
	{
		if (evt.left == null || evt.right == null)
			return;

		if (evt.left.getItem() instanceof IDarkSteelItem && OreDictionaryHelper.hasName(evt.right, Alloy.DARK_STEEL.getOreIngot()))
			this.handleRepair(evt);
		else
			this.handleUpgrade(evt);
	}

	private void handleRepair(AnvilUpdateEvent evt)
	{
		ItemStack targetStack = evt.left;
		ItemStack ingots = evt.right;

		//repair event
		IDarkSteelItem targetItem = (IDarkSteelItem) targetStack.getItem();

		// TODO gamerforEA code start
		if (EventConfig.noDarkSteelShearsRepair && targetItem instanceof ItemDarkSteelShears)
			return;
		// TODO gamerforEA code end

		int maxIngots = targetItem.getIngotsRequiredForFullRepair();

		double damPerc = (double) targetStack.getItemDamage() / targetStack.getMaxDamage();
		int requiredIngots = (int) Math.ceil(damPerc * maxIngots);
		if (ingots.stackSize > requiredIngots)
			return;

		int damageAddedPerIngot = (int) Math.ceil((double) targetStack.getMaxDamage() / maxIngots);
		int totalDamageRemoved = damageAddedPerIngot * ingots.stackSize;

		ItemStack resultStack = targetStack.copy();
		resultStack.setItemDamage(Math.max(0, resultStack.getItemDamage() - totalDamageRemoved));

		evt.output = resultStack;
		evt.cost = ingots.stackSize + (int) Math.ceil(getEnchantmentRepairCost(resultStack) / 2);
	}

	private void handleUpgrade(AnvilUpdateEvent evt)
	{
		for (IDarkSteelUpgrade upgrade : this.upgrades)
		{
			if (upgrade.isUpgradeItem(evt.right) && upgrade.canAddToItem(evt.left))
			{
				ItemStack res = new ItemStack(evt.left.getItem(), 1, evt.left.getItemDamage());
				if (evt.left.stackTagCompound != null)
					res.stackTagCompound = (NBTTagCompound) evt.left.stackTagCompound.copy();
				upgrade.writeToItem(res);
				evt.output = res;
				evt.cost = upgrade.getLevelCost();
				return;
			}
		}
	}

	public static int getEnchantmentRepairCost(ItemStack itemStack)
	{
		//derived from ContainerRepair
		int res = 0;
		Map map1 = EnchantmentHelper.getEnchantments(itemStack);
		Iterator iter = map1.keySet().iterator();
		while (iter.hasNext())
		{
			int i1 = (Integer) iter.next();
			Enchantment enchantment = Enchantment.enchantmentsList[i1];

			int level = (Integer) map1.get(i1);
			if (enchantment.canApply(itemStack))
			{
				if (level > enchantment.getMaxLevel())
					level = enchantment.getMaxLevel();
				int costPerLevel = 0;
				switch (enchantment.getWeight())
				{
					case 1:
						costPerLevel = 8;
						break;
					case 2:
						costPerLevel = 4;
					case 3:
					case 4:
					case 6:
					case 7:
					case 8:
					case 9:
					default:
						break;
					case 5:
						costPerLevel = 2;
						break;
					case 10:
						costPerLevel = 1;
				}
				res += costPerLevel * level;
			}
		}
		return res;
	}

	public List<IDarkSteelUpgrade> getUpgrades()
	{
		return this.upgrades;
	}

	public void addCommonTooltipEntries(ItemStack itemstack, EntityPlayer entityplayer, List list, boolean flag)
	{
		for (IDarkSteelUpgrade upgrade : this.upgrades)
		{
			if (upgrade.hasUpgrade(itemstack))
				upgrade.addCommonEntries(itemstack, entityplayer, list, flag);
		}
	}

	public void addBasicTooltipEntries(ItemStack itemstack, EntityPlayer entityplayer, List list, boolean flag)
	{
		for (IDarkSteelUpgrade upgrade : this.upgrades)
		{
			if (upgrade.hasUpgrade(itemstack))
				upgrade.addBasicEntries(itemstack, entityplayer, list, flag);
		}
	}

	public void addAdvancedTooltipEntries(ItemStack itemstack, EntityPlayer entityplayer, List list, boolean flag)
	{

		List<IDarkSteelUpgrade> applyableUpgrades = new ArrayList<IDarkSteelUpgrade>();
		for (IDarkSteelUpgrade upgrade : this.upgrades)
		{
			if (upgrade.hasUpgrade(itemstack))
				upgrade.addDetailedEntries(itemstack, entityplayer, list, flag);
			else if (upgrade.canAddToItem(itemstack))
				applyableUpgrades.add(upgrade);
		}
		if (!applyableUpgrades.isEmpty())
		{
			list.add(EnumChatFormatting.YELLOW + EnderIO.lang.localize("tooltip.anvilupgrades") + " ");
			for (IDarkSteelUpgrade up : applyableUpgrades)
			{
				list.add(EnumChatFormatting.DARK_AQUA + "" + "" + EnderIO.lang.localizeExact(up.getUnlocalizedName() + ".name") + ": ");
				list.add(EnumChatFormatting.DARK_AQUA + "" + EnumChatFormatting.ITALIC + "  " + up.getUpgradeItemName() + " + " + up.getLevelCost() + " " + EnderIO.lang.localize("item.darkSteel.tooltip.lvs"));
			}
		}
	}

	public Iterator<IDarkSteelUpgrade> recipeIterator()
	{
		return ImmutableList.copyOf(this.upgrades).iterator();
	}
}
