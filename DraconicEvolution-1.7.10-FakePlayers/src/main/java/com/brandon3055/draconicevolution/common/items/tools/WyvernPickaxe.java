package com.brandon3055.draconicevolution.common.items.tools;

import com.brandon3055.draconicevolution.client.render.IRenderTweak;
import com.brandon3055.draconicevolution.common.ModItems;
import com.brandon3055.draconicevolution.common.handler.BalanceConfigHandler;
import com.brandon3055.draconicevolution.common.items.tools.baseclasses.MiningTool;
import com.brandon3055.draconicevolution.common.utills.IInventoryTool;
import com.brandon3055.draconicevolution.common.utills.IUpgradableItem;
import com.brandon3055.draconicevolution.common.utills.ItemConfigField;
import com.gamerforea.draconicevolution.EventConfig;
import java.util.List;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import org.lwjgl.opengl.GL11;

public class WyvernPickaxe extends MiningTool implements IInventoryTool, IRenderTweak {
    public WyvernPickaxe() {
        super(ModItems.WYVERN);
        this.setUnlocalizedName("wyvernPickaxe");
        this.setHarvestLevel("pickaxe", 10);
        this.setCapacity(BalanceConfigHandler.wyvernToolsBaseStorage);
        this.setMaxExtract(BalanceConfigHandler.wyvernToolsMaxTransfer);
        this.setMaxReceive(BalanceConfigHandler.wyvernToolsMaxTransfer);
        super.energyPerOperation = BalanceConfigHandler.wyvernToolsEnergyPerAction;
        ModItems.register(this);
    }

    public List<ItemConfigField> getFields(ItemStack stack, int slot) {
        List<ItemConfigField> list = super.getFields(stack, slot);
        list.add((new ItemConfigField(2, slot, "ToolDigAOE")).setMinMaxAndIncromente(Integer.valueOf(0), Integer.valueOf(IUpgradableItem.EnumUpgrade.DIG_AOE.getUpgradePoints(stack)), Integer.valueOf(1)).readFromItem(stack, Integer.valueOf(0)).setModifier("AOE"));
        list.add((new ItemConfigField(2, slot, "ToolDigDepth")).setMinMaxAndIncromente(Integer.valueOf(1), Integer.valueOf(IUpgradableItem.EnumUpgrade.DIG_DEPTH.getUpgradePoints(stack)), Integer.valueOf(1)).readFromItem(stack, Integer.valueOf(1)));
        return list;
    }

    public String getInventoryName() {
        return StatCollector.translateToLocal("info.de.toolInventoryEnch.txt");
    }

    public int getInventorySlots() {
        return 0;
    }

    public boolean isEnchantValid(Enchantment enchant) {
        return enchant.type == EnumEnchantmentType.digger;
    }

    public void tweakRender(ItemRenderType type) {
        GL11.glTranslated(0.4D, 0.65D, 0.1D);
        GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(140.0F, 0.0F, -1.0F, 0.0F);
        GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);
        GL11.glScaled(0.6D, 0.6D, 0.6D);
        if(type == ItemRenderType.INVENTORY) {
            GL11.glScalef(13.8F, 13.8F, 13.8F);
            GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
            GL11.glTranslated(-1.2D, 0.0D, -0.35D);
        } else if(type == ItemRenderType.ENTITY) {
            GL11.glRotatef(90.5F, 0.0F, 1.0F, 0.0F);
            GL11.glTranslated(-0.1D, 0.0D, -0.9D);
        }

    }

    public int getUpgradeCap(ItemStack itemstack) {
        return BalanceConfigHandler.wyvernToolsMaxUpgrades;
    }

    public int getMaxTier(ItemStack itemstack) {
        return 1;
    }

    public List<String> getUpgradeStats(ItemStack stack) {
        return super.getUpgradeStats(stack);
    }

    public int getCapacity(ItemStack stack) {
        int points = IUpgradableItem.EnumUpgrade.RF_CAPACITY.getUpgradePoints(stack);
        return BalanceConfigHandler.wyvernToolsBaseStorage + points * BalanceConfigHandler.wyvernToolsStoragePerUpgrade;
    }

    public int getMaxUpgradePoints(int upgradeIndex) {
        if(upgradeIndex == IUpgradableItem.EnumUpgrade.RF_CAPACITY.index) {
            return BalanceConfigHandler.wyvernToolsMaxCapacityUpgradePoints;
        } else if(upgradeIndex == IUpgradableItem.EnumUpgrade.DIG_AOE.index) {
            int max = 2;
            max = Math.min(max, EventConfig.pickaxeWyvernMaxRange);
            return max;
        } else if(upgradeIndex == IUpgradableItem.EnumUpgrade.DIG_DEPTH.index) {
            int max1 = 5;
            max1 = Math.min(max1, EventConfig.pickaxeDepthWyvernMaxRange);
            return max1;
        } else {
            return upgradeIndex == IUpgradableItem.EnumUpgrade.DIG_SPEED.index?16:BalanceConfigHandler.wyvernToolsMaxUpgradePoints;
        }
    }

    public int getBaseUpgradePoints(int upgradeIndex) {
        return upgradeIndex == IUpgradableItem.EnumUpgrade.DIG_AOE.index?1:(upgradeIndex == IUpgradableItem.EnumUpgrade.DIG_DEPTH.index?1:(upgradeIndex == IUpgradableItem.EnumUpgrade.DIG_SPEED.index?4:0));
    }

    public List<IUpgradableItem.EnumUpgrade> getUpgrades(ItemStack itemstack) {
        List<IUpgradableItem.EnumUpgrade> list = super.getUpgrades(itemstack);
        return list;
    }
}
