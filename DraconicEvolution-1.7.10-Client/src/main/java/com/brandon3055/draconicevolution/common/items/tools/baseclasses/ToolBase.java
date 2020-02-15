package com.brandon3055.draconicevolution.common.items.tools.baseclasses;

import com.brandon3055.brandonscore.BrandonsCore;
import com.brandon3055.brandonscore.common.utills.DataUtills;
import com.brandon3055.brandonscore.common.utills.InfoHelper;
import com.brandon3055.brandonscore.common.utills.ItemNBTHelper;
import com.brandon3055.draconicevolution.DraconicEvolution;
import com.brandon3055.draconicevolution.client.keybinding.KeyBindings;
import com.brandon3055.draconicevolution.common.handler.ConfigHandler;
import com.brandon3055.draconicevolution.common.items.tools.baseclasses.RFItemBase;
import com.brandon3055.draconicevolution.common.network.ToolModePacket;
import com.brandon3055.draconicevolution.common.utills.IConfigurableItem;
import com.brandon3055.draconicevolution.common.utills.IUpgradableItem;
import com.brandon3055.draconicevolution.common.utills.ItemConfigField;
import com.google.common.collect.Sets;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item.ToolMaterial;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class ToolBase extends RFItemBase {
    private static final Set SHOVEL_OVERRIDES = Sets.newHashSet(new Object[]{Blocks.grass, Blocks.dirt, Blocks.sand, Blocks.gravel, Blocks.snow_layer, Blocks.snow, Blocks.clay, Blocks.farmland, Blocks.soul_sand, Blocks.mycelium, Material.grass, Material.ground, Material.sand, Material.snow, Material.craftedSnow, Material.clay});
    private static final Set PICKAXE_OVERRIDES = Sets.newHashSet(new Object[]{Blocks.cobblestone, Blocks.double_stone_slab, Blocks.stone_slab, Blocks.stone, Blocks.sandstone, Blocks.mossy_cobblestone, Blocks.iron_ore, Blocks.iron_block, Blocks.coal_ore, Blocks.gold_block, Blocks.gold_ore, Blocks.diamond_ore, Blocks.diamond_block, Blocks.ice, Blocks.netherrack, Blocks.lapis_ore, Blocks.lapis_block, Blocks.redstone_ore, Blocks.lit_redstone_ore, Blocks.rail, Blocks.detector_rail, Blocks.golden_rail, Blocks.activator_rail, Material.iron, Material.anvil, Material.rock, Material.glass, Material.ice, Material.packedIce});
    private static final Set AXE_OVERRIDES = Sets.newHashSet(new Object[]{Blocks.planks, Blocks.bookshelf, Blocks.log, Blocks.log2, Blocks.chest, Blocks.pumpkin, Blocks.lit_pumpkin, Material.wood, Material.leaves, Material.coral, Material.cactus, Material.plants, Material.vine});
    private Set blockOverrides;
    private float efficiencyOnProperMaterial = 4.0F;
    public float damageVsEntity;
    protected ToolMaterial toolMaterial;
    public int energyPerOperation = 0;

    protected ToolBase(float baseDamage, ToolMaterial material, Set blockOverrides) {
        this.toolMaterial = material;
        this.blockOverrides = (Set)(blockOverrides == null?new HashSet():blockOverrides);
        super.maxStackSize = 1;
        this.setMaxDamage(material.getMaxUses());
        this.efficiencyOnProperMaterial = material.getEfficiencyOnProperMaterial();
        this.damageVsEntity = baseDamage + material.getDamageVsEntity();
        this.setCreativeTab(DraconicEvolution.tabToolsWeapons);
    }

    public float getEfficiency(ItemStack stack) {
        return this.efficiencyOnProperMaterial;
    }

    public void setHarvestLevel(String toolClass, int level) {
        if(toolClass.equals("pickaxe")) {
            this.blockOverrides.addAll(PICKAXE_OVERRIDES);
        }

        if(toolClass.equals("shovel")) {
            this.blockOverrides.addAll(SHOVEL_OVERRIDES);
        }

        if(toolClass.equals("axe")) {
            this.blockOverrides.addAll(AXE_OVERRIDES);
        }

        super.setHarvestLevel(toolClass, level);
    }

    public float func_150893_a(ItemStack stack, Block block) {
        return !this.blockOverrides.contains(block) && !this.blockOverrides.contains(block.getMaterial())?1.0F:this.getEfficiency(stack);
    }

    public boolean func_150897_b(Block block) {
        return this.getToolClasses((ItemStack)null).contains("pickaxe")?true:this.blockOverrides.contains(block) || this.blockOverrides.contains(block.getMaterial());
    }

    @SideOnly(Side.CLIENT)
    public boolean isFull3D() {
        return true;
    }

    public int getItemEnchantability() {
        return this.toolMaterial.getEnchantability();
    }

    public float getDigSpeed(ItemStack stack, Block block, int meta) {
        float speed;
        if(ForgeHooks.isToolEffective(stack, block, meta)) {
            speed = this.getEfficiency(stack);
        } else {
            speed = super.getDigSpeed(stack, block, meta);
        }

        if(this.getEnergyStored(stack) >= this.energyPerOperation) {
            float f = IConfigurableItem.ProfileHelper.getFloat(stack, "ToolDigMultiplier", 1.0F);
            if(speed > 50.0F) {
                f *= f;
            }

            return f * speed;
        } else {
            return 0.5F;
        }
    }

    public List<ItemConfigField> getFields(ItemStack stack, int slot) {
        List<ItemConfigField> list = super.getFields(stack, slot);
        if(!this.getToolClasses(stack).isEmpty()) {
            list.add((new ItemConfigField(4, slot, "ToolDigMultiplier")).setMinMaxAndIncromente(Float.valueOf(0.0F), Float.valueOf(1.0F), Float.valueOf(0.01F)).readFromItem(stack, Float.valueOf(1.0F)).setModifier("PERCENT"));
        }

        if(!this.getToolClasses(stack).isEmpty()) {
            list.add((new ItemConfigField(6, slot, "BaseSafeAOE")).readFromItem(stack, Boolean.valueOf(false)));
        }

        return list;
    }

    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean extended) {
        boolean show = InfoHelper.holdShiftForDetails(list);
        if(show) {
            if(this.hasProfiles()) {
                int preset = ItemNBTHelper.getInteger(stack, "ConfigProfile", 0);
                list.add(EnumChatFormatting.DARK_PURPLE + StatCollector.translateToLocal("info.de.capacitorMode.txt") + ": " + ItemNBTHelper.getString(stack, "ProfileName" + preset, "Profile " + preset));
            }

            for(ItemConfigField f : this.getFields(stack, 0)) {
                list.add(f.getTooltipInfo());
            }
        }

        holdCTRLForUpgrades(list, stack);
        this.addAditionalInformation(stack, player, list, extended);
        InfoHelper.addEnergyInfo(stack, list);
        if(show && !ConfigHandler.disableLore) {
            InfoHelper.addLore(stack, list, true);
        }

    }

    @SideOnly(Side.CLIENT)
    public void addAditionalInformation(ItemStack stack, EntityPlayer player, List list, boolean extended) {
        String key;
        if(KeyBindings.toolConfig.getKeyCode() >= 0 && KeyBindings.toolConfig.getKeyCode() < 256) {
            key = Keyboard.getKeyName(KeyBindings.toolConfig.getKeyCode());
        } else {
            key = Mouse.getButtonName(KeyBindings.toolConfig.getKeyCode() + 101);
        }

        list.add(StatCollector.translateToLocal("info.de.press.txt") + " " + key + " " + StatCollector.translateToLocal("info.de.toOpenConfigGUI.txt"));
    }

    public EnumRarity getRarity(ItemStack stack) {
        return stack.getUnlocalizedName().contains(":wyvern")?EnumRarity.rare:(stack.getUnlocalizedName().contains(":draconic")?EnumRarity.epic:EnumRarity.uncommon);
    }

    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if(!world.isRemote && !BrandonsCore.proxy.isDedicatedServer()) {
            handleModeChange(stack, player, InfoHelper.isShiftKeyDown(), InfoHelper.isCtrlKeyDown());
        } else if(world.isRemote && BrandonsCore.proxy.getMCServer() == null) {
            handleModeChange(stack, player, InfoHelper.isShiftKeyDown(), InfoHelper.isCtrlKeyDown());
            DraconicEvolution.network.sendToServer(new ToolModePacket(InfoHelper.isShiftKeyDown(), InfoHelper.isCtrlKeyDown()));
        }

        return super.onItemRightClick(stack, world, player);
    }

    public static void handleModeChange(ItemStack stack, EntityPlayer player, boolean shift, boolean ctrl) {
        if(stack != null && stack.getItem() instanceof IConfigurableItem) {
            IConfigurableItem item = (IConfigurableItem)stack.getItem();
            if(shift && !ctrl) {
                for(ItemConfigField field : item.getFields(stack, player.inventory.currentItem)) {
                    if(field.name.equals("ToolDigAOE")) {
                        int aoe = ((Integer)field.value).intValue();
                        ++aoe;
                        if(aoe > ((Integer)field.max).intValue()) {
                            aoe = ((Integer)field.min).intValue();
                        }

                        field.value = Integer.valueOf(aoe);
                        DataUtills.writeObjectToCompound(IConfigurableItem.ProfileHelper.getProfileCompound(stack), field.value, field.datatype, field.name);
                    }
                }
            } else if(ctrl && !shift) {
                for(ItemConfigField field : item.getFields(stack, player.inventory.currentItem)) {
                    if(field.name.equals("ToolDigDepth")) {
                        int aoe = ((Integer)field.value).intValue();
                        ++aoe;
                        if(aoe > ((Integer)field.max).intValue()) {
                            aoe = ((Integer)field.min).intValue();
                        }

                        field.value = Integer.valueOf(aoe);
                        DataUtills.writeObjectToCompound(IConfigurableItem.ProfileHelper.getProfileCompound(stack), field.value, field.datatype, field.name);
                    }
                }
            } else if(ctrl && shift) {
                for(ItemConfigField field : item.getFields(stack, player.inventory.currentItem)) {
                    if(field.name.equals("WeaponAttackAOE")) {
                        int aoe = ((Integer)field.value).intValue();
                        ++aoe;
                        if(aoe > ((Integer)field.max).intValue()) {
                            aoe = ((Integer)field.min).intValue();
                        }

                        field.value = Integer.valueOf(aoe);
                        DataUtills.writeObjectToCompound(IConfigurableItem.ProfileHelper.getProfileCompound(stack), field.value, field.datatype, field.name);
                    }
                }
            }

        }
    }

    public ToolMaterial getToolMaterial() {
        return this.toolMaterial;
    }

    public static void holdCTRLForUpgrades(List list, ItemStack stack) {
        if(stack != null && stack.getItem() instanceof IUpgradableItem) {
            if(!InfoHelper.isCtrlKeyDown()) {
                list.add(StatCollector.translateToLocal("info.de.hold.txt") + " " + EnumChatFormatting.AQUA + "" + EnumChatFormatting.ITALIC + StatCollector.translateToLocal("info.de.ctrl.txt") + EnumChatFormatting.RESET + " " + EnumChatFormatting.GRAY + StatCollector.translateToLocal("info.de.forUpgrades.txt"));
            } else {
                list.addAll(((IUpgradableItem)stack.getItem()).getUpgradeStats(stack));
                list.add(EnumChatFormatting.GOLD + "" + EnumChatFormatting.ITALIC + StatCollector.translateToLocal("info.de.useUpgradeModifier.txt"));
            }

        }
    }
}
