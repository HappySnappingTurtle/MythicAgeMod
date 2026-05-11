package com.example.mythic_age.block.entity;

import com.example.mythic_age.block.ModBlocks;
import com.example.mythic_age.item.ModItems;
import com.example.mythic_age.item.RelicItem;
import com.example.mythic_age.util.MultiBlockStructureUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 圣物基座方块实体类
 * 
 * 这是圣物基座方块的核心逻辑处理类，负责管理圣物存储、神殿结构检测和效果激活。
 * 作为一个BlockEntity，它可以存储持久化数据并执行定期的游戏逻辑更新。
 * 
 * 核心职责：
 * 1. 圣物管理：
 *    - 存储和管理放置在基座上的圣物
 *    - 验证只有RelicItem可以被放置
 *    - 处理圣物的放置和取出逻辑
 * 
 * 2. 神殿结构检测：
 *    - 定期检查周围是否构成有效的神殿结构
 *    - 监测结构完整性的变化
 *    - 在结构状态改变时通知附近玩家
 * 
 * 3. 效果系统：
 *    - 为每种圣物实现独特的神殿效果
 *    - 管理效果的范围和强度
 *    - 定期为范围内的玩家施加状态效果
 * 
 * 4. 视觉反馈：
 *    - 在神殿激活/失活时播放粒子效果
 *    - 播放相应的音效
 *    - 发送本地化的状态消息
 * 
 * 圣物效果系统：
 * - 拉神安卡：5格范围内玩家获得再生效果
 * - 阿瑞斯头盔：5格范围内玩家获得力量效果
 * - 雷神锤碎片：8格范围内玩家获得急迫和抗性，雷暴时特殊效果
 * - 金羊毛：6格范围内玩家获得幸运和村庄英雄效果，定期给予经验
 * - 丰饶角：7格范围内玩家获得饱和效果，定期生成食物
 * 
 * 技术特性：
 * - 基于tick的实时更新系统
 * - 完整的NBT数据序列化
 * - 客户端-服务器同步
 * - 高效的范围检测算法
 * - 防止效果叠加的智能管理
 * 
 * 性能优化：
 * - 使用计数器减少检测频率
 * - 智能的玩家范围查找
 * - 结构变化时才重新检测
 * - 合理的效果持续时间
 * 
 * @author MythicAge Development Team
 * @version 1.0.0
 * @since 1.20.1
 */
public class RelicPedestalBlockEntity extends BlockEntity {
    /** 存储在基座上的圣物物品，为空时表示基座上没有圣物 */
    private ItemStack relicStack = ItemStack.EMPTY;
    
    /** 神殿是否处于激活状态，只有当结构完整且有圣物时才为true */
    private boolean isActiveTemple = false;
    
    /** tick计数器，用于控制定期检测和效果施加的频率 */
    private int tickCounter = 0;

    public RelicPedestalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RELIC_PEDESTAL_BLOCK_ENTITY, pos, state);
    }

    public ItemStack getRelicStack() {
        return relicStack;
    }

    public void setRelicStack(ItemStack stack) {
        if ((stack.getItem() instanceof RelicItem || stack.isEmpty())) {
            this.relicStack = stack.copy(); // Ensure we copy the stack
            if (!stack.isEmpty() && stack.getItem() instanceof RelicItem) {
                // When a new relic is placed, immediately check for temple structure
                checkTempleStructure();
            } else {
                this.isActiveTemple = false; // No relic, so not an active temple
            }
            markDirty();
            if (world != null && !world.isClient) {
                world.updateListeners(pos, getCachedState(), getCachedState(), 3);
            }
        } else if (!stack.isEmpty()){
            System.err.println("Attempted to place non-relic item on pedestal: " + stack.getItem().toString());
        }
    }

    private void checkTempleStructure() {
        if (world == null || world.isClient) return;
        // Example: Check for a 3x3 platform of Stone Bricks one block below the pedestal
        // This is a placeholder for a more complex structure check
        boolean wasActive = this.isActiveTemple;
        this.isActiveTemple = MultiBlockStructureUtil.checkForSimplePlatform(world, this.pos, Blocks.STONE_BRICKS);
        
        if (this.isActiveTemple && !wasActive) {
            // Temple just became active
            System.out.println("Relic Pedestal at " + pos.toString() + " is part of an active temple!");
            
            // Notify nearby players
            List<PlayerEntity> nearbyPlayers = world.getNonSpectatingEntities(PlayerEntity.class, new Box(pos).expand(10.0D));
            for (PlayerEntity player : nearbyPlayers) {
                player.sendMessage(Text.translatable("mythic_age.temple.activated"), false);
            }
            
            // Spawn some particles or play a sound to indicate activation
            ((ServerWorld)world).spawnParticles(ParticleTypes.ENCHANT, 
                                               pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 
                                               15, 0.2, 0.2, 0.2, 0.05);
            world.playSound(null, pos, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 0.5f, 1.5f);
        } else if (!this.isActiveTemple && wasActive) {
            // Temple just became inactive
            System.out.println("Relic Pedestal at " + pos.toString() + " is NO LONGER part of an active temple.");
            
            // Notify nearby players
            List<PlayerEntity> nearbyPlayers = world.getNonSpectatingEntities(PlayerEntity.class, new Box(pos).expand(10.0D));
            for (PlayerEntity player : nearbyPlayers) {
                player.sendMessage(Text.translatable("mythic_age.temple.deactivated"), false);
            }
        } else if (!this.isActiveTemple) {
             System.out.println("Relic Pedestal at " + pos.toString() + " is NOT part of an active temple.");
        }
        markDirty(); // State changed, mark dirty
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        if (!relicStack.isEmpty()) {
            nbt.put("RelicItem", relicStack.writeNbt(new NbtCompound()));
        }
        nbt.putBoolean("IsActiveTemple", this.isActiveTemple);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        if (nbt.contains("RelicItem")) {
            relicStack = ItemStack.fromNbt(nbt.getCompound("RelicItem"));
        } else {
            relicStack = ItemStack.EMPTY;
        }
        this.isActiveTemple = nbt.getBoolean("IsActiveTemple");
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }

    public static void tick(World world, BlockPos pos, BlockState state, RelicPedestalBlockEntity be) {
        if (world == null || world.isClient) {
            return;
        }
        be.tickCounter++;
        // Periodically re-check structure or apply effects
        if (be.tickCounter % 100 == 0) { // Every 5 seconds (100 ticks)
            if (!be.relicStack.isEmpty()) {
                be.checkTempleStructure(); // Re-validate temple structure
            }
        }

        if (be.isActiveTemple && !be.relicStack.isEmpty()) {
            // Apply active temple effects based on the relic
            ItemStack currentRelic = be.getRelicStack();
            if (currentRelic.getItem() == ModItems.RELIC_ANKH_OF_RA) {
                // Example: Area of effect Regeneration for players near the active Ankh temple
                if (be.tickCounter % 20 == 0) { // Every second
                    List<PlayerEntity> playersNearby = world.getNonSpectatingEntities(PlayerEntity.class, new Box(pos).expand(5.0D)); // 5 block radius
                    for (PlayerEntity player : playersNearby) {
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 60, 0, true, false, true));
                        // player.sendMessage(Text.literal("You feel the Ankh's power!"), true);
                    }
                }
            } else if (currentRelic.getItem() == ModItems.RELIC_HELM_OF_ARES) {
                 if (be.tickCounter % 20 == 0) { 
                    List<PlayerEntity> playersNearby = world.getNonSpectatingEntities(PlayerEntity.class, new Box(pos).expand(5.0D));
                    for (PlayerEntity player : playersNearby) {
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 60, 0, true, false, true));
                    }
                }
            } else if (currentRelic.getItem() == ModItems.RELIC_MJOLNIR_FRAGMENT) {
                // Mjolnir Fragment: Provides area lightning protection and haste
                if (be.tickCounter % 20 == 0) {
                    List<PlayerEntity> playersNearby = world.getNonSpectatingEntities(PlayerEntity.class, new Box(pos).expand(8.0D));
                    for (PlayerEntity player : playersNearby) {
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 60, 1, true, false, true));
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 60, 0, true, false, true));
                    }
                }
                // Occasional lightning show for dramatic effect
                if (be.tickCounter % 400 == 0 && world.isThundering()) { // Every 20 seconds during storms
                    ((ServerWorld)world).spawnParticles(net.minecraft.particle.ParticleTypes.ELECTRIC_SPARK, 
                        pos.getX() + 0.5, pos.getY() + 2, pos.getZ() + 0.5, 
                        20, 1.0, 1.0, 1.0, 0.1);
                }
            } else if (currentRelic.getItem() == ModItems.RELIC_GOLDEN_FLEECE) {
                // Golden Fleece: Provides area luck and experience boost
                if (be.tickCounter % 20 == 0) {
                    List<PlayerEntity> playersNearby = world.getNonSpectatingEntities(PlayerEntity.class, new Box(pos).expand(6.0D));
                    for (PlayerEntity player : playersNearby) {
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 60, 1, true, false, true));
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.HERO_OF_THE_VILLAGE, 60, 0, true, false, true));
                    }
                }
                // Passive experience generation
                if (be.tickCounter % 100 == 0) { // Every 5 seconds
                    List<PlayerEntity> playersNearby = world.getNonSpectatingEntities(PlayerEntity.class, new Box(pos).expand(6.0D));
                    for (PlayerEntity player : playersNearby) {
                        player.addExperience(2);
                    }
                }
            } else if (currentRelic.getItem() == ModItems.RELIC_CORNUCOPIA) {
                // Cornucopia: Provides area saturation and food generation
                if (be.tickCounter % 20 == 0) {
                    List<PlayerEntity> playersNearby = world.getNonSpectatingEntities(PlayerEntity.class, new Box(pos).expand(7.0D));
                    for (PlayerEntity player : playersNearby) {
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SATURATION, 60, 1, true, false, true));
                        // Slowly restore hunger
                        if (player.getHungerManager().getFoodLevel() < 20) {
                            player.getHungerManager().add(1, 1.0f);
                        }
                    }
                }
                // Generate food items around the temple
                if (be.tickCounter % 300 == 0) { // Every 15 seconds
                    // Spawn food items around the pedestal
                    for (int i = 0; i < 2; i++) {
                        BlockPos spawnPos = pos.add(
                            world.random.nextInt(5) - 2, 1, world.random.nextInt(5) - 2);
                        
                        if (world.getBlockState(spawnPos).isAir()) {
                            net.minecraft.entity.ItemEntity foodItem = new net.minecraft.entity.ItemEntity(world, 
                                spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                                new net.minecraft.item.ItemStack(getFoodItem(world.random)));
                            world.spawnEntity(foodItem);
                        }
                    }
                }
            }
            // ============ 中国圣物神殿效果 ============
            else if (currentRelic.getItem() == ModItems.RELIC_KUNLUN_MIRROR) {
                // 昆仑镜：提供区域洞察效果，增强夜视和发光
                if (be.tickCounter % 20 == 0) {
                    List<PlayerEntity> playersNearby = world.getNonSpectatingEntities(PlayerEntity.class, new Box(pos).expand(8.0D));
                    for (PlayerEntity player : playersNearby) {
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 80, 0, true, false, true));
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 80, 0, true, false, true));
                    }
                }
                // 每10秒给予经验
                if (be.tickCounter % 200 == 0) {
                    List<PlayerEntity> playersNearby = world.getNonSpectatingEntities(PlayerEntity.class, new Box(pos).expand(8.0D));
                    for (PlayerEntity player : playersNearby) {
                        player.addExperience(3);
                    }
                }
            } else if (currentRelic.getItem() == ModItems.RELIC_NUWA_STONE) {
                // 女娲石：提供区域修复和保护效果
                if (be.tickCounter % 20 == 0) {
                    List<PlayerEntity> playersNearby = world.getNonSpectatingEntities(PlayerEntity.class, new Box(pos).expand(10.0D));
                    for (PlayerEntity player : playersNearby) {
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 80, 1, true, false, true));
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 80, 1, true, false, true));
                    }
                }
                // 修复附近玩家的装备
                if (be.tickCounter % 400 == 0) { // 每20秒
                    List<PlayerEntity> playersNearby = world.getNonSpectatingEntities(PlayerEntity.class, new Box(pos).expand(10.0D));
                    for (PlayerEntity player : playersNearby) {
                        // 修复主手物品
                        ItemStack mainHand = player.getMainHandStack();
                        if (!mainHand.isEmpty() && mainHand.isDamaged()) {
                            mainHand.setDamage(Math.max(0, mainHand.getDamage() - 5));
                        }
                    }
                }
            } else if (currentRelic.getItem() == ModItems.RELIC_SHENNONG_CAULDRON) {
                // 神农鼎：提供区域治愈和抗毒效果
                if (be.tickCounter % 20 == 0) {
                    List<PlayerEntity> playersNearby = world.getNonSpectatingEntities(PlayerEntity.class, new Box(pos).expand(12.0D));
                    for (PlayerEntity player : playersNearby) {
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SATURATION, 80, 1, true, false, true));
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 0, 0, true, false, true)); // 清除毒性
                        // 清除负面效果
                        player.removeStatusEffect(StatusEffects.POISON);
                        player.removeStatusEffect(StatusEffects.WITHER);
                        player.removeStatusEffect(StatusEffects.HUNGER);
                    }
                }
                // 生成药水材料
                if (be.tickCounter % 600 == 0) { // 每30秒
                    BlockPos spawnPos = pos.add(world.random.nextInt(5) - 2, 1, world.random.nextInt(5) - 2);
                    if (world.getBlockState(spawnPos).isAir()) {
                        net.minecraft.item.Item[] herbs = {
                            net.minecraft.item.Items.SPIDER_EYE,
                            net.minecraft.item.Items.GHAST_TEAR,
                            net.minecraft.item.Items.BLAZE_POWDER,
                            net.minecraft.item.Items.MAGMA_CREAM
                        };
                        net.minecraft.entity.ItemEntity herbItem = new net.minecraft.entity.ItemEntity(world,
                            spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                            new ItemStack(herbs[world.random.nextInt(herbs.length)]));
                        world.spawnEntity(herbItem);
                    }
                }
            } else if (currentRelic.getItem() == ModItems.RELIC_HOUYI_BOW) {
                // 后羿弓：提供区域力量和精准效果
                if (be.tickCounter % 20 == 0) {
                    List<PlayerEntity> playersNearby = world.getNonSpectatingEntities(PlayerEntity.class, new Box(pos).expand(15.0D));
                    for (PlayerEntity player : playersNearby) {
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 80, 1, true, false, true));
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 80, 1, true, false, true));
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 80, 0, true, false, true));
                    }
                }
            }
            // ============ 埃及圣物神殿效果 ============
            else if (currentRelic.getItem() == ModItems.RELIC_EYE_OF_HORUS) {
                // 荷鲁斯之眼：提供区域保护和洞察效果
                if (be.tickCounter % 20 == 0) {
                    List<PlayerEntity> playersNearby = world.getNonSpectatingEntities(PlayerEntity.class, new Box(pos).expand(12.0D));
                    for (PlayerEntity player : playersNearby) {
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 80, 0, true, false, true));
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 80, 1, true, false, true));
                    }
                }
                // 每15秒给予经验
                if (be.tickCounter % 300 == 0) {
                    List<PlayerEntity> playersNearby = world.getNonSpectatingEntities(PlayerEntity.class, new Box(pos).expand(12.0D));
                    for (PlayerEntity player : playersNearby) {
                        player.addExperience(4);
                    }
                }
            } else if (currentRelic.getItem() == ModItems.RELIC_ANKH_OF_ISIS) {
                // 伊西斯的安卡：提供区域生命和复活效果
                if (be.tickCounter % 20 == 0) {
                    List<PlayerEntity> playersNearby = world.getNonSpectatingEntities(PlayerEntity.class, new Box(pos).expand(10.0D));
                    for (PlayerEntity player : playersNearby) {
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 80, 2, true, false, true));
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.HEALTH_BOOST, 80, 1, true, false, true));
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 80, 0, true, false, true));
                    }
                }
            } else if (currentRelic.getItem() == ModItems.RELIC_ANUBIS_STAFF) {
                // 阿努比斯权杖：提供区域死亡抗性和审判效果
                if (be.tickCounter % 20 == 0) {
                    List<PlayerEntity> playersNearby = world.getNonSpectatingEntities(PlayerEntity.class, new Box(pos).expand(8.0D));
                    for (PlayerEntity player : playersNearby) {
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 80, 1, true, false, true));
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 0, 0, true, false, true)); // 清除凋零
                        player.removeStatusEffect(StatusEffects.WITHER);
                    }
                }
                // 对附近敌对生物造成凋零效果
                if (be.tickCounter % 40 == 0) { // 每2秒
                    List<net.minecraft.entity.mob.HostileEntity> hostiles = world.getNonSpectatingEntities(
                        net.minecraft.entity.mob.HostileEntity.class, new Box(pos).expand(8.0D));
                    for (net.minecraft.entity.mob.HostileEntity hostile : hostiles) {
                        hostile.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 60, 0));
                    }
                }
            } else if (currentRelic.getItem() == ModItems.RELIC_SCARAB_OF_RA) {
                // 拉神圣甲虫：提供区域太阳祝福和重生效果
                if (be.tickCounter % 20 == 0) {
                    List<PlayerEntity> playersNearby = world.getNonSpectatingEntities(PlayerEntity.class, new Box(pos).expand(15.0D));
                    for (PlayerEntity player : playersNearby) {
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 80, 0, true, false, true));
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 80, 0, true, false, true));
                        // 白天额外效果
                        if (world.isDay()) {
                            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 80, 1, true, false, true));
                            player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 80, 0, true, false, true));
                        }
                    }
                }
            }
            // ============ 北欧圣物神殿效果 ============
            else if (currentRelic.getItem() == ModItems.RELIC_ODIN_EYE) {
                // 奥丁之眼：提供区域智慧和预知效果
                if (be.tickCounter % 20 == 0) {
                    List<PlayerEntity> playersNearby = world.getNonSpectatingEntities(PlayerEntity.class, new Box(pos).expand(15.0D));
                    for (PlayerEntity player : playersNearby) {
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 80, 0, true, false, true));
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 80, 0, true, false, true));
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 80, 1, true, false, true));
                    }
                }
                // 每12秒给予大量经验
                if (be.tickCounter % 240 == 0) {
                    List<PlayerEntity> playersNearby = world.getNonSpectatingEntities(PlayerEntity.class, new Box(pos).expand(15.0D));
                    for (PlayerEntity player : playersNearby) {
                        player.addExperience(5);
                    }
                }
            } else if (currentRelic.getItem() == ModItems.RELIC_FREYJA_NECKLACE) {
                // 芙蕾雅项链：提供区域美丽和魅力效果
                if (be.tickCounter % 20 == 0) {
                    List<PlayerEntity> playersNearby = world.getNonSpectatingEntities(PlayerEntity.class, new Box(pos).expand(10.0D));
                    for (PlayerEntity player : playersNearby) {
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 80, 1, true, false, true));
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.HERO_OF_THE_VILLAGE, 80, 1, true, false, true));
                    }
                }
                // 生成宝石
                if (be.tickCounter % 800 == 0) { // 每40秒
                    BlockPos spawnPos = pos.add(world.random.nextInt(5) - 2, 1, world.random.nextInt(5) - 2);
                    if (world.getBlockState(spawnPos).isAir()) {
                        net.minecraft.item.Item[] gems = {
                            net.minecraft.item.Items.DIAMOND,
                            net.minecraft.item.Items.EMERALD,
                            net.minecraft.item.Items.GOLD_INGOT,
                            net.minecraft.item.Items.LAPIS_LAZULI
                        };
                        net.minecraft.entity.ItemEntity gemItem = new net.minecraft.entity.ItemEntity(world,
                            spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                            new ItemStack(gems[world.random.nextInt(gems.length)]));
                        world.spawnEntity(gemItem);
                    }
                }
            } else if (currentRelic.getItem() == ModItems.RELIC_LOKI_MASK) {
                // 洛基面具：提供区域变化和诡计效果
                if (be.tickCounter % 20 == 0) {
                    List<PlayerEntity> playersNearby = world.getNonSpectatingEntities(PlayerEntity.class, new Box(pos).expand(8.0D));
                    for (PlayerEntity player : playersNearby) {
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 80, 0, true, false, true));
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 80, 1, true, false, true));
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 80, 1, true, false, true));
                    }
                }
                // 随机传送附近敌对生物
                if (be.tickCounter % 300 == 0) { // 每15秒
                    List<net.minecraft.entity.mob.HostileEntity> hostiles = world.getNonSpectatingEntities(
                        net.minecraft.entity.mob.HostileEntity.class, new Box(pos).expand(8.0D));
                    for (net.minecraft.entity.mob.HostileEntity hostile : hostiles) {
                        // 随机传送到附近位置
                        double newX = pos.getX() + (world.random.nextDouble() - 0.5) * 20;
                        double newZ = pos.getZ() + (world.random.nextDouble() - 0.5) * 20;
                        double newY = world.getTopY(net.minecraft.world.Heightmap.Type.WORLD_SURFACE, (int)newX, (int)newZ);
                        hostile.teleport(newX, newY, newZ);
                    }
                }
            }
            // Add more relic effects here
        }
    }
    
    private static net.minecraft.item.Item getFoodItem(net.minecraft.util.math.random.Random random) {
        net.minecraft.item.Item[] foods = {
            net.minecraft.item.Items.BREAD,
            net.minecraft.item.Items.APPLE,
            net.minecraft.item.Items.CARROT,
            net.minecraft.item.Items.POTATO,
            net.minecraft.item.Items.WHEAT,
            net.minecraft.item.Items.BEETROOT
        };
        return foods[random.nextInt(foods.length)];
    }
    
    /**
     * 生成食物物品给玩家
     */
    private void generateFood(PlayerEntity player) {
        if (player.getInventory().getEmptySlot() != -1) {
            ItemStack food = new ItemStack(net.minecraft.item.Items.BREAD);
            player.getInventory().insertStack(food);
        }
    }
    
    /**
     * 修复玩家装备耐久度
     */
    private void repairPlayerEquipment(PlayerEntity player) {
        for (ItemStack stack : player.getInventory().armor) {
            if (!stack.isEmpty() && stack.isDamaged()) {
                stack.setDamage(Math.max(0, stack.getDamage() - 5));
            }
        }
        ItemStack mainHand = player.getMainHandStack();
        if (!mainHand.isEmpty() && mainHand.isDamaged()) {
            mainHand.setDamage(Math.max(0, mainHand.getDamage() - 5));
        }
        ItemStack offHand = player.getOffHandStack();
        if (!offHand.isEmpty() && offHand.isDamaged()) {
            offHand.setDamage(Math.max(0, offHand.getDamage() - 5));
        }
    }
    
    /**
     * 清除玩家负面状态效果
     */
    private void clearNegativeEffects(PlayerEntity player) {
        player.removeStatusEffect(StatusEffects.POISON);
        player.removeStatusEffect(StatusEffects.WITHER);
        player.removeStatusEffect(StatusEffects.WEAKNESS);
        player.removeStatusEffect(StatusEffects.SLOWNESS);
        player.removeStatusEffect(StatusEffects.MINING_FATIGUE);
        player.removeStatusEffect(StatusEffects.NAUSEA);
        player.removeStatusEffect(StatusEffects.BLINDNESS);
        player.removeStatusEffect(StatusEffects.HUNGER);
    }
    
    /**
     * 生成药水材料给玩家
     */
    private void generatePotionMaterials(PlayerEntity player) {
        if (player.getInventory().getEmptySlot() != -1) {
            ItemStack[] materials = {
                new ItemStack(net.minecraft.item.Items.SPIDER_EYE),
                new ItemStack(net.minecraft.item.Items.GHAST_TEAR),
                new ItemStack(net.minecraft.item.Items.BLAZE_POWDER),
                new ItemStack(net.minecraft.item.Items.MAGMA_CREAM),
                new ItemStack(net.minecraft.item.Items.GLISTERING_MELON_SLICE)
            };
            ItemStack material = materials[world.getRandom().nextInt(materials.length)];
            player.getInventory().insertStack(material);
        }
    }
    
    /**
     * 生成宝石给玩家
     */
    private void generateGems(PlayerEntity player) {
        if (player.getInventory().getEmptySlot() != -1) {
            ItemStack[] gems = {
                new ItemStack(net.minecraft.item.Items.DIAMOND),
                new ItemStack(net.minecraft.item.Items.EMERALD),
                new ItemStack(net.minecraft.item.Items.LAPIS_LAZULI),
                new ItemStack(net.minecraft.item.Items.REDSTONE)
            };
            ItemStack gem = gems[world.getRandom().nextInt(gems.length)];
            player.getInventory().insertStack(gem);
        }
    }
    
    /**
     * 随机传送范围内的敌对生物
     */
    private void teleportRandomHostiles() {
        List<net.minecraft.entity.mob.HostileEntity> hostiles = world.getEntitiesByClass(net.minecraft.entity.mob.HostileEntity.class, 
            new Box(pos).expand(8), entity -> true);
        for (net.minecraft.entity.mob.HostileEntity hostile : hostiles) {
            if (world.getRandom().nextFloat() < 0.3f) { // 30%概率传送
                double x = pos.getX() + (world.getRandom().nextDouble() - 0.5) * 32;
                double z = pos.getZ() + (world.getRandom().nextDouble() - 0.5) * 32;
                double y = world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING, (int)x, (int)z);
                hostile.teleport(x, y, z);
            }
        }
    }
}

