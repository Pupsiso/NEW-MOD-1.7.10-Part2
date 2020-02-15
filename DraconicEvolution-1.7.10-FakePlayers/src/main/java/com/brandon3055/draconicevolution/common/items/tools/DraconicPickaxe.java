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

public class DraconicPickaxe extends MiningTool implements IInventoryTool, IRenderTweak {
    public DraconicPickaxe() {
        super(ModItems.AWAKENED);
        this.setHarvestLevel("pickaxe", 10);
        this.setUnlocalizedName("draconicPickaxe");
        this.setCapacity(BalanceConfigHandler.draconicToolsBaseStorage);
        this.setMaxExtract(BalanceConfigHandler.draconicToolsMaxTransfer);
        this.setMaxReceive(BalanceConfigHandler.draconicToolsMaxTransfer);
        super.energyPerOperation = BalanceConfigHandler.draconicToolsEnergyPerAction;
        ModItems.register(this);
    }

    public List<ItemConfigField> getFields(ItemStack stack, int slot) {
        List<ItemConfigField> list = super.getFields(stack, slot);
        list.add((new ItemConfigField(2, slot, "ToolDigAOE")).setMinMaxAndIncromente(Integer.valueOf(0), Integer.valueOf(IUpgradableItem.EnumUpgrade.DIG_AOE.getUpgradePoints(stack)), Integer.valueOf(1)).readFromItem(stack, Integer.valueOf(0)).setModifier("AOE"));
        list.add((new ItemConfigField(2, slot, "ToolDigDepth")).setMinMaxAndIncromente(Integer.valueOf(1), Integer.valueOf(IUpgradableItem.EnumUpgrade.DIG_DEPTH.getUpgradePoints(stack)), Integer.valueOf(1)).readFromItem(stack, Integer.valueOf(1)));
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
        return enchant.type == EnumEnchantmentType.digger;
    }

    public void tweakRender(ItemRenderType type) {
        GL11.glTranslated(0.34D, 0.69D, 0.1D);
        GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(140.0F, 0.0F, -1.0F, 0.0F);
        GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);
        GL11.glScaled(0.7D, 0.7D, 0.7D);
        if(type == ItemRenderType.INVENTORY) {
            GL11.glScalef(11.8F, 11.8F, 11.8F);
            GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
            GL11.glTranslated(-1.2D, 0.0D, -0.35D);
        } else if(type == ItemRenderType.ENTITY) {
            GL11.glRotatef(90.5F, 0.0F, 1.0F, 0.0F);
            GL11.glTranslated(0.0D, 0.0D, -0.9D);
        }

    }

    public int getUpgradeCap(ItemStack itemstack) {
        return BalanceConfigHandler.draconicToolsMaxUpgrades;
    }

    public int getMaxTier(ItemStack itemstack) {
        return 2;
    }

    public List<String> getUpgradeStats(ItemStack stack) {
        return super.getUpgradeStats(stack);
    }

    public int getCapacity(ItemStack stack) {
        int points = IUpgradableItem.EnumUpgrade.RF_CAPACITY.getUpgradePoints(stack);
        return BalanceConfigHandler.draconicToolsBaseStorage + points * BalanceConfigHandler.draconicToolsStoragePerUpgrade;
    }

    public int getMaxUpgradePoints(int upgradeIndex) {
        if(upgradeIndex == IUpgradableItem.EnumUpgrade.RF_CAPACITY.index) {
            return BalanceConfigHandler.draconicToolsMaxCapacityUpgradePoints;
        } else if(upgradeIndex == IUpgradableItem.EnumUpgrade.DIG_AOE.index) {
            int max = 4;
            max = Math.min(max, EventConfig.pickaxeMaxRange);
            return max;
        } else if(upgradeIndex == IUpgradableItem.EnumUpgrade.DIG_DEPTH.index) {
            int max1 = 5;
            max1 = Math.min(max1, EventConfig.pickaxeDepthMaxRange);
            return max1;
        } else {
            return upgradeIndex == IUpgradableItem.EnumUpgrade.DIG_SPEED.index?32:BalanceConfigHandler.draconicToolsMaxUpgradePoints;
        }
    }

    public int getBaseUpgradePoints(int upgradeIndex) {
        return upgradeIndex == IUpgradableItem.EnumUpgrade.DIG_AOE.index?2:(upgradeIndex == IUpgradableItem.EnumUpgrade.DIG_DEPTH.index?1:(upgradeIndex == IUpgradableItem.EnumUpgrade.DIG_SPEED.index?5:0));
    }
}
