package com.brandon3055.draconicevolution.common.items.tools;

import com.brandon3055.brandonscore.common.utills.InfoHelper;
import com.brandon3055.draconicevolution.client.render.IRenderTweak;
import com.brandon3055.draconicevolution.common.ModItems;
import com.brandon3055.draconicevolution.common.handler.BalanceConfigHandler;
import com.brandon3055.draconicevolution.common.items.tools.baseclasses.MiningTool;
import com.brandon3055.draconicevolution.common.items.tools.baseclasses.ToolHandler;
import com.brandon3055.draconicevolution.common.items.weapons.IEnergyContainerWeaponItem;
import com.brandon3055.draconicevolution.common.utills.IConfigurableItem;
import com.brandon3055.draconicevolution.common.utills.IInventoryTool;
import com.brandon3055.draconicevolution.common.utills.IUpgradableItem;
import com.brandon3055.draconicevolution.common.utills.ItemConfigField;
import com.gamerforea.draconicevolution.EventConfig;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import org.lwjgl.opengl.GL11;

public class DraconicDistructionStaff extends MiningTool implements IInventoryTool, IRenderTweak, IEnergyContainerWeaponItem {
	public DraconicDistructionStaff() {
		super(ModItems.CHAOTIC);
		this.setUnlocalizedName("draconicDistructionStaff");
		this.setHarvestLevel("pickaxe", 10);
		this.setHarvestLevel("shovel", 10);
		this.setHarvestLevel("axe", 10);
		this.setCapacity(BalanceConfigHandler.draconicToolsBaseStorage * 2 + BalanceConfigHandler.draconicWeaponsBaseStorage);
		this.setMaxExtract(BalanceConfigHandler.draconicToolsMaxTransfer * 2 + BalanceConfigHandler.draconicWeaponsMaxTransfer);
		this.setMaxReceive(BalanceConfigHandler.draconicToolsMaxTransfer * 2 + BalanceConfigHandler.draconicWeaponsMaxTransfer);
		super.energyPerOperation = BalanceConfigHandler.draconicToolsEnergyPerAction;
		ModItems.register(this);
	}

	public float func_150893_a(ItemStack stack, Block block) {
		return this.getEfficiency(stack);
	}

	public List<ItemConfigField> getFields(ItemStack stack, int slot) {
		List<ItemConfigField> list = super.getFields(stack, slot);
		list.add((new ItemConfigField(2, slot, "ToolDigAOE")).setMinMaxAndIncromente(Integer.valueOf(0), Integer.valueOf(IUpgradableItem.EnumUpgrade.DIG_AOE.getUpgradePoints(stack)), Integer.valueOf(1)).readFromItem(stack, Integer.valueOf(0)).setModifier("AOE"));
		list.add((new ItemConfigField(2, slot, "ToolDigDepth")).setMinMaxAndIncromente(Integer.valueOf(1), Integer.valueOf(IUpgradableItem.EnumUpgrade.DIG_DEPTH.getUpgradePoints(stack)), Integer.valueOf(1)).readFromItem(stack, Integer.valueOf(1)));
		list.add((new ItemConfigField(2, slot, "WeaponAttackAOE")).setMinMaxAndIncromente(Integer.valueOf(0), Integer.valueOf(IUpgradableItem.EnumUpgrade.ATTACK_AOE.getUpgradePoints(stack)), Integer.valueOf(1)).readFromItem(stack, Integer.valueOf(1)).setModifier("AOE"));
		list.add((new ItemConfigField(6, slot, "ToolVoidJunk")).readFromItem(stack, Boolean.valueOf(false)));
		return list;
	}

	public String getInventoryName() {
		return StatCollector.translateToLocal("info.de.toolInventoryOblit.txt");
	}

	public int getInventorySlots() {
		return 9;
	}

	public boolean isEnchantValid(Enchantment enchant) {
		return enchant.type == EnumEnchantmentType.digger || enchant.type == EnumEnchantmentType.weapon;
	}

	public boolean onLeftClickEntity(ItemStack stack, EntityPlayer player, Entity entity) {
		if(EventConfig.ignoreHurtResistantTime) {
			entity.hurtResistantTime = 0;
		}

		ToolHandler.damageEntityBasedOnHealth(entity, player, 0.3F);
		ToolHandler.AOEAttack(player, entity, stack, IConfigurableItem.ProfileHelper.getInteger(stack, "WeaponAttackAOE", 0));
		return true;
	}

	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
		return super.onItemRightClick(stack, world, player);
	}

	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean extended) {
		super.addInformation(stack, player, list, extended);
		list.add("");
		list.add(EnumChatFormatting.BLUE + "+" + ToolHandler.getBaseAttackDamage(stack) + " " + StatCollector.translateToLocal("info.de.attackDamage.txt"));
		list.add(EnumChatFormatting.BLUE + "+30% " + StatCollector.translateToLocal("info.de.bonusHealthDamage.txt"));
	}

	public void tweakRender(ItemRenderType type) {
		GL11.glTranslated(0.77D, 0.19D, -0.15D);
		GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
		GL11.glRotatef(-35.0F, 0.0F, -1.0F, 0.0F);
		GL11.glScaled(0.7D, 0.7D, 0.7D);
		if(type == ItemRenderType.INVENTORY) {
			GL11.glScalef(6.0F, 6.0F, 6.0F);
			GL11.glRotatef(145.0F, 0.0F, 1.0F, 0.0F);
			GL11.glTranslated(-1.7D, 0.0D, 1.8D);
		} else if(type == ItemRenderType.ENTITY) {
			GL11.glRotatef(-34.5F, 0.0F, 1.0F, 0.0F);
			GL11.glTranslated(-1.1D, 0.0D, -0.2D);
		} else if(type == ItemRenderType.EQUIPPED_FIRST_PERSON) {
			GL11.glTranslated(0.0D, 0.4D, 0.0D);
		}

	}

	public int getUpgradeCap(ItemStack itemstack) {
		return BalanceConfigHandler.draconicStaffMaxUpgrades;
	}

	public int getMaxTier(ItemStack itemstack) {
		return 2;
	}

	public int getMaxUpgradePoints(int upgradeIndex) {
		if(upgradeIndex == IUpgradableItem.EnumUpgrade.RF_CAPACITY.index) {
			return BalanceConfigHandler.draconicStaffMaxCapacityUpgradePoints;
		} else if(upgradeIndex == IUpgradableItem.EnumUpgrade.DIG_AOE.index) {
			int max = 5;
			max = Math.min(max, EventConfig.staffMaxRange);
			return max;
		} else if(upgradeIndex == IUpgradableItem.EnumUpgrade.DIG_DEPTH.index) {
			int max1 = 11;
			max1 = Math.min(max1, EventConfig.staffDepthMaxRange);
			return max1;
		} else {
			return upgradeIndex == IUpgradableItem.EnumUpgrade.ATTACK_AOE.index?13:(upgradeIndex == IUpgradableItem.EnumUpgrade.ATTACK_DAMAGE.index?64:BalanceConfigHandler.draconicStaffMaxUpgradePoints);
		}
	}

	public int getBaseUpgradePoints(int upgradeIndex) {
		return upgradeIndex == IUpgradableItem.EnumUpgrade.DIG_AOE.index?3:(upgradeIndex == IUpgradableItem.EnumUpgrade.DIG_DEPTH.index?7:(upgradeIndex == IUpgradableItem.EnumUpgrade.ATTACK_AOE.index?3:(upgradeIndex == IUpgradableItem.EnumUpgrade.ATTACK_DAMAGE.index?0:0)));
	}

	public int getCapacity(ItemStack stack) {
		int points = IUpgradableItem.EnumUpgrade.RF_CAPACITY.getUpgradePoints(stack);
		return BalanceConfigHandler.draconicToolsBaseStorage * 2 + BalanceConfigHandler.draconicWeaponsBaseStorage + points * (BalanceConfigHandler.draconicToolsStoragePerUpgrade + BalanceConfigHandler.draconicWeaponsStoragePerUpgrade);
	}

	public List<IUpgradableItem.EnumUpgrade> getUpgrades(ItemStack itemstack) {
		List<IUpgradableItem.EnumUpgrade> list = super.getUpgrades(itemstack);
		list.add(IUpgradableItem.EnumUpgrade.ATTACK_AOE);
		list.add(IUpgradableItem.EnumUpgrade.ATTACK_DAMAGE);
		list.remove(IUpgradableItem.EnumUpgrade.DIG_SPEED);
		return list;
	}

	public List<String> getUpgradeStats(ItemStack stack) {
		List<String> list = super.getUpgradeStats(stack);
		list.add(InfoHelper.ITC() + StatCollector.translateToLocal("info.de.attackDamage.txt") + ": " + InfoHelper.HITC() + ToolHandler.getBaseAttackDamage(stack));
		return list;
	}

	public int getEnergyPerAttack() {
		return BalanceConfigHandler.draconicWeaponsEnergyPerAttack;
	}
}
