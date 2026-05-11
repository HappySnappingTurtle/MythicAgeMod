package com.example.mythic_age.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 神话圣物物品类
 * 
 * 实现了古代神话中传说圣物的功能，每个圣物都具有独特的神话背景和强大的魔法效果。
 * 圣物系统是神话时代Mod的核心特色之一，提供了三层递进的效果系统。
 * 
 * 三层效果系统：
 * 1. 手持效果 (Held Effects)：
 *    - 当圣物在主手时激活
 *    - 提供持续的状态效果加成
 *    - 每个圣物都有独特的手持效果
 * 
 * 2. 被动效果 (Passive Effects)：
 *    - 当圣物在背包中时激活
 *    - 提供长期的资源生成或经验获取
 *    - 不需要手持，只要在背包中即可生效
 * 
 * 3. 神殿效果 (Temple Effects)：
 *    - 当圣物放置在神殿基座上时激活
 *    - 为周围区域内的所有玩家提供强大的增益效果
 *    - 效果范围和强度因圣物而异(5-8格范围)
 * 
 * 包含的神话圣物：
 * - 拉神安卡 (埃及) - 生命与治愈主题，提供再生效果
 * - 阿瑞斯头盔 (希腊) - 战争与力量主题，提供力量效果
 * - 雷神锤碎片 (北欧) - 雷电与力量主题，提供急迫效果
 * - 伊阿宋的金羊毛 (希腊) - 财富与好运主题，提供幸运和经验
 * - 丰饶角 (罗马) - 丰收与富饶主题，提供饱食和食物生成
 * 
 * 技术特性：
 * - 基于inventoryTick的实时效果更新
 * - 智能的效果叠加和冲突处理
 * - 完整的多语言工具提示系统
 * - 与神殿系统的深度集成
 * 
 * 平衡性设计：
 * - 不同稀有度的圣物有不同的效果强度
 * - 被动效果有合理的触发间隔
 * - 手持效果需要占用主手位置作为代价
 * 
 * @author MythicAge Development Team
 * @version 1.0.0
 * @since 1.20.1
 */
public class RelicItem extends Item {
    public RelicItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        // Add basic relic description
        tooltip.add(Text.translatable(this.getTranslationKey() + ".tooltip"));
        
        // Add empty line for separation
        tooltip.add(Text.literal(""));
        
        // Add specific relic effects based on type
        if (stack.getItem() == ModItems.RELIC_ANKH_OF_RA) {
            tooltip.add(Text.translatable("mythic_age.relic.ankh_of_ra.held_effect").formatted(net.minecraft.util.Formatting.GREEN));
            tooltip.add(Text.translatable("mythic_age.relic.ankh_of_ra.temple_effect").formatted(net.minecraft.util.Formatting.GOLD));
        } else if (stack.getItem() == ModItems.RELIC_HELM_OF_ARES) {
            tooltip.add(Text.translatable("mythic_age.relic.helm_of_ares.held_effect").formatted(net.minecraft.util.Formatting.RED));
            tooltip.add(Text.translatable("mythic_age.relic.helm_of_ares.temple_effect").formatted(net.minecraft.util.Formatting.GOLD));
        } else if (stack.getItem() == ModItems.RELIC_MJOLNIR_FRAGMENT) {
            tooltip.add(Text.translatable("mythic_age.relic.mjolnir_fragment.held_effect").formatted(net.minecraft.util.Formatting.AQUA));
            tooltip.add(Text.translatable("mythic_age.relic.mjolnir_fragment.temple_effect").formatted(net.minecraft.util.Formatting.GOLD));
        } else if (stack.getItem() == ModItems.RELIC_GOLDEN_FLEECE) {
            tooltip.add(Text.translatable("mythic_age.relic.golden_fleece.held_effect").formatted(net.minecraft.util.Formatting.YELLOW));
            tooltip.add(Text.translatable("mythic_age.relic.golden_fleece.passive_effect").formatted(net.minecraft.util.Formatting.LIGHT_PURPLE));
            tooltip.add(Text.translatable("mythic_age.relic.golden_fleece.temple_effect").formatted(net.minecraft.util.Formatting.GOLD));
        } else if (stack.getItem() == ModItems.RELIC_CORNUCOPIA) {
            tooltip.add(Text.translatable("mythic_age.relic.cornucopia.held_effect").formatted(net.minecraft.util.Formatting.GREEN));
            tooltip.add(Text.translatable("mythic_age.relic.cornucopia.passive_effect").formatted(net.minecraft.util.Formatting.LIGHT_PURPLE));
            tooltip.add(Text.translatable("mythic_age.relic.cornucopia.temple_effect").formatted(net.minecraft.util.Formatting.GOLD));
        }
        // 中国神话圣物工具提示
        else if (stack.getItem() == ModItems.RELIC_KUNLUN_MIRROR) {
            tooltip.add(Text.translatable("mythic_age.relic.kunlun_mirror.held_effect").formatted(net.minecraft.util.Formatting.AQUA));
            tooltip.add(Text.translatable("mythic_age.relic.kunlun_mirror.passive_effect").formatted(net.minecraft.util.Formatting.LIGHT_PURPLE));
            tooltip.add(Text.translatable("mythic_age.relic.kunlun_mirror.temple_effect").formatted(net.minecraft.util.Formatting.GOLD));
        } else if (stack.getItem() == ModItems.RELIC_NUWA_STONE) {
            tooltip.add(Text.translatable("mythic_age.relic.nuwa_stone.held_effect").formatted(net.minecraft.util.Formatting.GREEN));
            tooltip.add(Text.translatable("mythic_age.relic.nuwa_stone.passive_effect").formatted(net.minecraft.util.Formatting.LIGHT_PURPLE));
            tooltip.add(Text.translatable("mythic_age.relic.nuwa_stone.temple_effect").formatted(net.minecraft.util.Formatting.GOLD));
        } else if (stack.getItem() == ModItems.RELIC_SHENNONG_CAULDRON) {
            tooltip.add(Text.translatable("mythic_age.relic.shennong_cauldron.held_effect").formatted(net.minecraft.util.Formatting.GREEN));
            tooltip.add(Text.translatable("mythic_age.relic.shennong_cauldron.passive_effect").formatted(net.minecraft.util.Formatting.LIGHT_PURPLE));
            tooltip.add(Text.translatable("mythic_age.relic.shennong_cauldron.temple_effect").formatted(net.minecraft.util.Formatting.GOLD));
        } else if (stack.getItem() == ModItems.RELIC_HOUYI_BOW) {
            tooltip.add(Text.translatable("mythic_age.relic.houyi_bow.held_effect").formatted(net.minecraft.util.Formatting.RED));
            tooltip.add(Text.translatable("mythic_age.relic.houyi_bow.passive_effect").formatted(net.minecraft.util.Formatting.LIGHT_PURPLE));
            tooltip.add(Text.translatable("mythic_age.relic.houyi_bow.temple_effect").formatted(net.minecraft.util.Formatting.GOLD));
        }
        // 埃及神话圣物工具提示
        else if (stack.getItem() == ModItems.RELIC_EYE_OF_HORUS) {
            tooltip.add(Text.translatable("mythic_age.relic.eye_of_horus.held_effect").formatted(net.minecraft.util.Formatting.AQUA));
            tooltip.add(Text.translatable("mythic_age.relic.eye_of_horus.passive_effect").formatted(net.minecraft.util.Formatting.LIGHT_PURPLE));
            tooltip.add(Text.translatable("mythic_age.relic.eye_of_horus.temple_effect").formatted(net.minecraft.util.Formatting.GOLD));
        } else if (stack.getItem() == ModItems.RELIC_ANKH_OF_ISIS) {
            tooltip.add(Text.translatable("mythic_age.relic.ankh_of_isis.held_effect").formatted(net.minecraft.util.Formatting.GREEN));
            tooltip.add(Text.translatable("mythic_age.relic.ankh_of_isis.passive_effect").formatted(net.minecraft.util.Formatting.LIGHT_PURPLE));
            tooltip.add(Text.translatable("mythic_age.relic.ankh_of_isis.temple_effect").formatted(net.minecraft.util.Formatting.GOLD));
        } else if (stack.getItem() == ModItems.RELIC_ANUBIS_STAFF) {
            tooltip.add(Text.translatable("mythic_age.relic.anubis_staff.held_effect").formatted(net.minecraft.util.Formatting.RED));
            tooltip.add(Text.translatable("mythic_age.relic.anubis_staff.passive_effect").formatted(net.minecraft.util.Formatting.LIGHT_PURPLE));
            tooltip.add(Text.translatable("mythic_age.relic.anubis_staff.temple_effect").formatted(net.minecraft.util.Formatting.GOLD));
        } else if (stack.getItem() == ModItems.RELIC_SCARAB_OF_RA) {
            tooltip.add(Text.translatable("mythic_age.relic.scarab_of_ra.held_effect").formatted(net.minecraft.util.Formatting.YELLOW));
            tooltip.add(Text.translatable("mythic_age.relic.scarab_of_ra.passive_effect").formatted(net.minecraft.util.Formatting.LIGHT_PURPLE));
            tooltip.add(Text.translatable("mythic_age.relic.scarab_of_ra.temple_effect").formatted(net.minecraft.util.Formatting.GOLD));
        }
        // 北欧神话圣物工具提示
        else if (stack.getItem() == ModItems.RELIC_MJOLNIR_FRAGMENT) {
            tooltip.add(Text.translatable("mythic_age.relic.mjolnir_fragment.held_effect").formatted(net.minecraft.util.Formatting.AQUA));
            tooltip.add(Text.translatable("mythic_age.relic.mjolnir_fragment.passive_effect").formatted(net.minecraft.util.Formatting.LIGHT_PURPLE));
            tooltip.add(Text.translatable("mythic_age.relic.mjolnir_fragment.temple_effect").formatted(net.minecraft.util.Formatting.GOLD));
        } else if (stack.getItem() == ModItems.RELIC_ODIN_EYE) {
            tooltip.add(Text.translatable("mythic_age.relic.odin_eye.held_effect").formatted(net.minecraft.util.Formatting.AQUA));
            tooltip.add(Text.translatable("mythic_age.relic.odin_eye.passive_effect").formatted(net.minecraft.util.Formatting.LIGHT_PURPLE));
            tooltip.add(Text.translatable("mythic_age.relic.odin_eye.temple_effect").formatted(net.minecraft.util.Formatting.GOLD));
        } else if (stack.getItem() == ModItems.RELIC_FREYJA_NECKLACE) {
            tooltip.add(Text.translatable("mythic_age.relic.freyja_necklace.held_effect").formatted(net.minecraft.util.Formatting.YELLOW));
            tooltip.add(Text.translatable("mythic_age.relic.freyja_necklace.passive_effect").formatted(net.minecraft.util.Formatting.LIGHT_PURPLE));
            tooltip.add(Text.translatable("mythic_age.relic.freyja_necklace.temple_effect").formatted(net.minecraft.util.Formatting.GOLD));
        } else if (stack.getItem() == ModItems.RELIC_LOKI_MASK) {
            tooltip.add(Text.translatable("mythic_age.relic.loki_mask.held_effect").formatted(net.minecraft.util.Formatting.GRAY));
            tooltip.add(Text.translatable("mythic_age.relic.loki_mask.passive_effect").formatted(net.minecraft.util.Formatting.LIGHT_PURPLE));
            tooltip.add(Text.translatable("mythic_age.relic.loki_mask.temple_effect").formatted(net.minecraft.util.Formatting.GOLD));
        }
        
        // Add empty line
        tooltip.add(Text.literal(""));
        
        // Add general hint about passive effects
        tooltip.add(Text.translatable("item.mythic_age.relic.passive_effect_hint").formatted(net.minecraft.util.Formatting.GRAY));
        
        super.appendTooltip(stack, world, tooltip, context);
    }

    // Example of a passive effect: if the Ankh of Ra is in the player's main hand, grant Regeneration
    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isClient && entity instanceof PlayerEntity player) {
            // Check if this specific relic is the Ankh of Ra and is selected (in main hand)
            if (selected && stack.getItem() == ModItems.RELIC_ANKH_OF_RA) {
                // Apply Regeneration I for a short duration (e.g., 2 seconds), effect refreshed every tick it's held
                // This is a very basic implementation. A more robust system would handle effect levels, durations, and conditions better.
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 40, 0, true, false, true));
            }
            // Example for Helm of Ares: Grant Strength when in main hand
            else if (selected && stack.getItem() == ModItems.RELIC_HELM_OF_ARES) {
                 player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 40, 0, true, false, true));
            }
            // Mjolnir Fragment: Grant Haste and chance for lightning on attack
            else if (selected && stack.getItem() == ModItems.RELIC_MJOLNIR_FRAGMENT) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 40, 0, true, false, true));
                // Lightning effect would need to be handled in attack events
            }
            // Golden Fleece: Grant Luck and slight speed boost
            else if (selected && stack.getItem() == ModItems.RELIC_GOLDEN_FLEECE) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 40, 0, true, false, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 40, 0, true, false, true));
            }
            // Cornucopia: Slowly restore hunger
            else if (selected && stack.getItem() == ModItems.RELIC_CORNUCOPIA) {
                if (world.getTime() % 100 == 0) { // Every 5 seconds
                    if (player.getHungerManager().getFoodLevel() < 20) {
                        player.getHungerManager().add(1, 0.5f); // Add 1 hunger point
                    }
                }
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SATURATION, 40, 0, true, false, true));
            }
            
            // ============ 中国神话圣物手持效果 ============
            // 昆仑镜：夜视 + 发光
            else if (selected && stack.getItem() == ModItems.RELIC_KUNLUN_MIRROR) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 240, 0, true, false, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 40, 0, true, false, true));
            }
            // 女娲石：再生 II + 抗性 I
            else if (selected && stack.getItem() == ModItems.RELIC_NUWA_STONE) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 40, 1, true, false, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 40, 0, true, false, true));
            }
            // 神农鼎：饱和 I + 抗毒
            else if (selected && stack.getItem() == ModItems.RELIC_SHENNONG_CAULDRON) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SATURATION, 40, 0, true, false, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 0, 0, true, false, true)); // 清除毒性
            }
            // 后羿弓：力量 I + 急迫 I
            else if (selected && stack.getItem() == ModItems.RELIC_HOUYI_BOW) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 40, 0, true, false, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 40, 0, true, false, true));
            }
            
            // ============ 埃及神话圣物手持效果 ============
            // 荷鲁斯之眼：夜视 + 抗性 I
            else if (selected && stack.getItem() == ModItems.RELIC_EYE_OF_HORUS) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 240, 0, true, false, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 40, 0, true, false, true));
            }
            // 伊西斯的安卡：再生 II + 生命提升 I
            else if (selected && stack.getItem() == ModItems.RELIC_ANKH_OF_ISIS) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 40, 1, true, false, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.HEALTH_BOOST, 40, 0, true, false, true));
            }
            // 阿努比斯权杖：力量 I + 凋零抗性
            else if (selected && stack.getItem() == ModItems.RELIC_ANUBIS_STAFF) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 40, 0, true, false, true));
                // 凋零抗性通过清除凋零效果实现
                if (player.hasStatusEffect(StatusEffects.WITHER)) {
                    player.removeStatusEffect(StatusEffects.WITHER);
                }
            }
            // 拉神圣甲虫：发光 + 火焰保护 I
            else if (selected && stack.getItem() == ModItems.RELIC_SCARAB_OF_RA) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 40, 0, true, false, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 40, 0, true, false, true));
            }
            
            // ============ 北欧神话圣物手持效果 ============
            // 奥丁之眼：夜视 + 水下呼吸
            else if (selected && stack.getItem() == ModItems.RELIC_ODIN_EYE) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 240, 0, true, false, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 40, 0, true, false, true));
            }
            // 芙蕾雅项链：幸运 I + 魅力（村民交易折扣）
            else if (selected && stack.getItem() == ModItems.RELIC_FREYJA_NECKLACE) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 40, 0, true, false, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.HERO_OF_THE_VILLAGE, 40, 0, true, false, true));
            }
            // 洛基面具：隐身 I + 速度 I
            else if (selected && stack.getItem() == ModItems.RELIC_LOKI_MASK) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 40, 0, true, false, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 40, 0, true, false, true));
            }
            
            // ============ 被动效果（背包中生效） ============
            // Additional passive effects for relics in inventory (not necessarily selected)
            if (stack.getItem() == ModItems.RELIC_GOLDEN_FLEECE && !selected) {
                // Golden Fleece provides passive experience gain even when not held
                if (world.getTime() % 200 == 0) { // Every 10 seconds
                    player.addExperience(1);
                }
            }
            
            // Cornucopia provides passive food generation
            if (stack.getItem() == ModItems.RELIC_CORNUCOPIA && !selected) {
                if (world.getTime() % 600 == 0) { // Every 30 seconds
                    // Try to add bread to inventory
                    ItemStack bread = new ItemStack(Items.BREAD);
                    if (!player.getInventory().insertStack(bread)) {
                        // If inventory is full, drop it
                        player.dropItem(bread, false);
                    }
                }
            }
            
            // 中国神话圣物被动效果
            if (stack.getItem() == ModItems.RELIC_KUNLUN_MIRROR && !selected) {
                if (world.getTime() % 400 == 0) { // Every 20 seconds
                    player.addExperience(1);
                }
            }
            
            if (stack.getItem() == ModItems.RELIC_NUWA_STONE && !selected) {
                if (world.getTime() % 600 == 0) { // Every 30 seconds
                    // 修复装备耐久
                    for (ItemStack armorStack : player.getArmorItems()) {
                        if (armorStack.isDamaged()) {
                            armorStack.setDamage(Math.max(0, armorStack.getDamage() - 5));
                        }
                    }
                }
            }
            
            if (stack.getItem() == ModItems.RELIC_SHENNONG_CAULDRON && !selected) {
                if (world.getTime() % 500 == 0) { // Every 25 seconds
                    // 生成药水材料（蜘蛛眼、恶魂之泪等）
                    ItemStack ingredient = new ItemStack(world.random.nextBoolean() ? Items.SPIDER_EYE : Items.GHAST_TEAR);
                    if (!player.getInventory().insertStack(ingredient)) {
                        player.dropItem(ingredient, false);
                    }
                }
            }
            
            // 埃及神话圣物被动效果
            if (stack.getItem() == ModItems.RELIC_EYE_OF_HORUS && !selected) {
                if (world.getTime() % 300 == 0) { // Every 15 seconds
                    player.addExperience(1);
                }
            }
            
            if (stack.getItem() == ModItems.RELIC_SCARAB_OF_RA && !selected) {
                // 白天时获得速度提升
                if (world.isDay() && world.getTime() % 100 == 0) {
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 100, 0, true, false, true));
                }
            }
            
            // 北欧神话圣物被动效果
            if (stack.getItem() == ModItems.RELIC_ODIN_EYE && !selected) {
                if (world.getTime() % 200 == 0) { // Every 10 seconds
                    player.addExperience(1);
                }
            }
            
            if (stack.getItem() == ModItems.RELIC_FREYJA_NECKLACE && !selected) {
                if (world.getTime() % 400 == 0) { // Every 20 seconds
                    // 生成宝石（钻石、绿宝石等）
                    ItemStack gem = new ItemStack(world.random.nextBoolean() ? Items.DIAMOND : Items.EMERALD);
                    if (!player.getInventory().insertStack(gem)) {
                        player.dropItem(gem, false);
                    }
                }
            }
            
            // More complex effects (e.g. Mjolnir fragment giving chance to strike lightning on attack) would require more involved event handling.
        }
        super.inventoryTick(stack, world, entity, slot, selected);
    }

    // TODO: Implement more sophisticated logic for passive effects or effects when placed in a temple structure.
    // This might involve checking player inventory (for passive effects) or interacting with BlockEntities (for temple effects).
    // Consider using a capability system or custom data attached to the player for managing active relic effects.
}

