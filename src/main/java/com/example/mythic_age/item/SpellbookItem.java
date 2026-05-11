package com.example.mythic_age.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.ParticleType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Box;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import com.example.mythic_age.config.ModConfig;
import com.example.mythic_age.entity.MeteorEntity;
import com.example.mythic_age.entity.ModEntityTypes;
import com.example.mythic_age.entity.ModEntities;
import com.example.mythic_age.entity.WalkingTreeEntity;
import com.example.mythic_age.entity.WalkingTreeVariant;
import net.minecraft.world.Heightmap;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Block;
import net.minecraft.block.CropBlock;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.block.PlantBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.SnowGolemEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.passive.ParrotEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.state.property.Properties;
import net.minecraft.village.VillagerProfession;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * 神力法术书物品类
 * 
 * 这是神话时代Mod的核心物品类，实现了古代神话中各种神力法术的功能。
 * 每个法术书都对应一个特定的神祇和其独特的神力效果。
 * 
 * 核心功能：
 * - 智能目标选择：使用射线检测确定法术施放位置（最远50格）
 * - 冷却机制：每个法术都有独特的冷却时间，防止滥用
 * - 视觉反馈：包含粒子效果、音效和状态消息
 * - 多语言支持：完整的国际化支持，包含详细的工具提示
 * 
 * 支持的法术类型：
 * 1. 闪电风暴 (宙斯) - 在5x5区域召唤闪电攻击
 * 2. 复原术 (雅典娜) - 治疗自己和附近玩家
 * 3. 动物招引 (波塞冬) - 吸引附近的被动动物
 * 4. 冥府军团 (哈迪斯) - 召唤友方骷髅战士
 * 5. 瘟疫 (阿瑞斯) - 削弱敌人生产能力
 * 6. 诅咒 (阿芙洛狄忒) - 将敌人变成猪
 * 7. 地震 (阿尔忒弥斯) - 大范围建筑破坏
 * 8. 停火 (赫尔墨斯) - 暂时停止所有战斗
 * 9. 青铜 (狄俄尼索斯) - 增强友军护甲
 * 10. 冥界通道 (阿波罗) - 创建传送门
 * 11. 雷暴 (赫拉) - 更强力的闪电攻击
 * 12. 丰饶 (赫菲斯托斯) - 生成资源宝库
 * 
 * 技术特性：
 * - 服务器端逻辑确保多人游戏一致性
 * - 基于枚举的法术类型系统，便于扩展
 * - 完整的错误处理和用户反馈
 * - 平衡性考虑：不同法术有不同的冷却时间和效果强度
 * 
 * 使用方法：
 * 玩家右键点击使用法术书，系统会自动检测目标位置并施放对应的神力法术。
 * 法术成功施放后会进入冷却状态，冷却期间无法再次使用。
 * 
 * @author MythicAge Development Team
 * @version 1.0.0
 * @since 1.20.1
 */
public class SpellbookItem extends Item {
    private static final int WALKING_WOODS_SEARCH_RADIUS = 14;
    private static final int WALKING_WOODS_VERTICAL_RADIUS = 10;
    private static final int WALKING_WOODS_MIN_LOGS_PER_TREE = 3;
    private static final int WALKING_WOODS_MAX_TREES = 8;

    /** 法术冷却时间，以游戏刻(tick)为单位，20刻=1秒 */
    private final int cooldownTicks;
    
    /** 法术类型，决定施放的具体神力效果 */
    private final SpellType spellType;

    /**
     * 法术类型枚举
     * 
     * 定义了所有可用的神力法术类型，每种法术对应不同的古代神祇：
     * - LIGHTNING_STORM: 宙斯的闪电风暴，大范围攻击法术
     * - RESTORATION: 雅典娜的复原术，治疗和恢复法术  
     * - LURE: 波塞冬的动物招引，辅助和控制法术
     * - UNDERWORLD_ARMY: 哈迪斯的冥府军团，召唤和战斗法术
     * - PESTILENCE: 阿瑞斯的瘟疫，削弱敌人生产能力
     * - CURSE: 阿芙洛狄忒的诅咒，将敌人变成猪
     * - EARTHQUAKE: 阿尔忒弥斯的地震，大范围建筑破坏
     * - CEASEFIRE: 赫尔墨斯的停火，暂时停止所有战斗
     * - BRONZE: 狄俄尼索斯的青铜，增强友军护甲
     * - UNDERWORLD_PASSAGE: 阿波罗的冥界通道，创建传送门
     * - LIGHTNING_STORM_HERA: 赫拉的雷暴，更强力的闪电攻击
     * - PLENTY: 赫菲斯托斯的丰饶，生成资源宝库
     * - GENERATE_EGYPTIAN_TEMPLE: 生成埃及神殿
     * - GENERATE_CHINESE_TEMPLE: 生成中国神殿
     * - GENERATE_ATLANTEAN_TEMPLE: 生成亚特兰蒂斯神殿
     * - GENERATE_NORDIC_TEMPLE: 生成北欧神殿
     */
    public enum SpellType {
        // 希腊神力
        LIGHTNING_STORM,        // 闪电风暴 - 宙斯神力
        RESTORATION,            // 复原术 - 雅典娜神力
        LURE,                  // 动物招引 - 波塞冬神力
        UNDERWORLD_ARMY,       // 冥府军团 - 哈迪斯神力
        PESTILENCE,            // 瘟疫 - 阿瑞斯神力
        CURSE,                 // 诅咒 - 阿芙洛狄忒神力
        EARTHQUAKE,            // 地震 - 阿尔忒弥斯神力
        CEASEFIRE,             // 停火 - 赫尔墨斯神力
        BRONZE,                // 青铜 - 狄俄尼索斯神力
        UNDERWORLD_PASSAGE,    // 冥界通道 - 阿波罗神力
        LIGHTNING_STORM_HERA,  // 雷暴 - 赫拉神力
        PLENTY,                // 丰饶 - 赫菲斯托斯神力
        
        // 中国神力
        RAIN_PRAYER,           // 祈雨术 - 小神力
        EARTH_GOD_BLESSING,    // 土地神佑 - 小神力
        ENLIGHTENMENT,         // 启迪 - 小神力
        ABUNDANCE,             // 丰收 - 小神力
        FIVE_THUNDER,          // 五雷正法 - 大神力
        CRANE_BLESSING,        // 仙鹤祈福 - 大神力
        FLOOD,                 // 洪水 - 大神力
        CALL_TO_ARMS,          // 召唤守护者 - 大神力
        TAIJI_SHIELD,          // 太极护盾 - 终极神力
        RECREATION,            // 再造 - 终极神力
        
        // 北欧神力
        FENRIR_HOWL,           // 芬里尔之嚎 - 小神力
        ODIN_EYE,              // 奥丁之眼 - 小神力
        GREAT_HUNT,            // 巨鹰 - 小神力
        DWARVEN_MINE,          // 矮人矿坑 - 小神力
        THOR_WRATH,            // 雷神之怒 - 大神力
        VALHALLA_CALL,         // 瓦尔哈拉召唤 - 大神力
        FROST,                 // 霜冻 - 大神力
        WALKING_WOODS,         // 行走的森林 - 大神力
        RAGNAROK,              // 诸神黄昏 - 终极神力
        FIMBULWINTER,          // 芬布尔之冬 - 终极神力
        
        // 埃及神力
        RA_RADIANCE,           // 拉神光辉 - 小神力
        DESERT_STORM,          // 沙漠风暴 - 小神力
        SUN_RAIN,              // 太阳雨 - 小神力
        PROSPERITY,            // 繁荣 - 小神力
        EYE_OF_HORUS,          // 荷鲁斯之眼 - 大神力
        ANUBIS_JUDGMENT,       // 阿努比斯审判 - 大神力
        LOCUST_SWARM,          // 蝗虫群 - 大神力
        TORNADO,               // 龙卷风 - 大神力
        ISIS_RESURRECTION,     // 伊西斯复活术 - 终极神力
        METEOR,                // 陨石雨 - 终极神力
        
        // 神殿生成
        GENERATE_EGYPTIAN_TEMPLE,
        GENERATE_CHINESE_TEMPLE,
        GENERATE_ATLANTEAN_TEMPLE,
        GENERATE_NORDIC_TEMPLE
    }

    public SpellbookItem(Settings settings, int cooldownTicks, SpellType spellType) {
        super(settings);
        this.cooldownTicks = cooldownTicks;
        this.spellType = spellType;
    }

    // Overload for default cooldown if not specified
    public SpellbookItem(Settings settings, SpellType spellType) {
        this(settings, 20, spellType); // Default to 1 second (20 ticks) cooldown
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        // Add spell description
        String descriptionKey = "mythic_age.spellbook." + spellType.name().toLowerCase() + ".description";
        tooltip.add(Text.translatable(descriptionKey).formatted(Formatting.GRAY));
        
        // Add cooldown information
        float cooldownSeconds = cooldownTicks / 20.0f;
        tooltip.add(Text.literal(""));
        tooltip.add(Text.translatable("mythic_age.spellbook.cooldown", String.format("%.1f", cooldownSeconds))
                .formatted(Formatting.BLUE));
        
        // Add spell type information
        String spellTypeKey = "mythic_age.spellbook.type." + getSpellCategory();
        tooltip.add(Text.translatable(spellTypeKey).formatted(Formatting.YELLOW));
        
        super.appendTooltip(stack, world, tooltip, context);
    }
    
    private String getSpellCategory() {
        switch (spellType) {
            // 希腊神力 - 攻击类
            case LIGHTNING_STORM:
            case LIGHTNING_STORM_HERA:
            case EARTHQUAKE:
            // 中国神力 - 攻击类
            case FIVE_THUNDER:
            case FLOOD:
            // 埃及神力 - 攻击类
            case DESERT_STORM:
            case ANUBIS_JUDGMENT:
            case LOCUST_SWARM:
            case TORNADO:
            case METEOR:
            // 北欧神力 - 攻击类
            case THOR_WRATH:
            case FROST:
            case RAGNAROK:
            case FIMBULWINTER:
                return "offensive";
                
            // 希腊神力 - 治疗类
            case RESTORATION:
            // 中国神力 - 治疗类
            case CRANE_BLESSING:
            case RECREATION:
            // 埃及神力 - 治疗类
            case SUN_RAIN:
            case ISIS_RESURRECTION:
                return "healing";
                
            // 希腊神力 - 辅助类
            case LURE:
            case CEASEFIRE:
            // 中国神力 - 辅助类
            case RAIN_PRAYER:
            case EARTH_GOD_BLESSING:
            case ENLIGHTENMENT:
            case TAIJI_SHIELD:
            // 埃及神力 - 辅助类
            case RA_RADIANCE:
            case EYE_OF_HORUS:
            // 北欧神力 - 辅助类
            case ODIN_EYE:
            case WALKING_WOODS:
                return "utility";
                
            // 希腊神力 - 召唤类
            case UNDERWORLD_ARMY:
            // 中国神力 - 召唤类
            case CALL_TO_ARMS:
            // 北欧神力 - 召唤类
            case FENRIR_HOWL:
            case GREAT_HUNT:
            case VALHALLA_CALL:
                return "summoning";
                
            // 希腊神力 - 削弱类
            case PESTILENCE:
            case CURSE:
                return "debuff";
                
            // 希腊神力 - 增益类
            case BRONZE:
                return "buff";
                
            // 希腊神力 - 传送类
            case UNDERWORLD_PASSAGE:
                return "transportation";
                
            // 希腊神力 - 资源类
            case PLENTY:
            // 中国神力 - 资源类
            case ABUNDANCE:
            // 埃及神力 - 资源类
            case PROSPERITY:
            // 北欧神力 - 资源类
            case DWARVEN_MINE:
                return "resource";
                
            default:
                return "unknown";
        }
    }

    // 获取目标位置的辅助方法
    private BlockPos getTargetPos(World world, PlayerEntity player) {
        Vec3d start = player.getCameraPosVec(1.0F);
        Vec3d direction = player.getRotationVec(1.0F);
        Vec3d end = start.add(direction.multiply(50.0));
        
        BlockHitResult hitResult = world.raycast(new RaycastContext(
            start, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, player
        ));
        
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            return hitResult.getBlockPos();
        }
        
        return player.getBlockPos().add(0, 0, 5);
    }

    // 中国神力实现方法
    private boolean castEarthGodBlessing(World world, PlayerEntity player) {
        // 土地神佑 - 增强周围土地肥沃度
        BlockPos center = player.getBlockPos();
        ServerWorld serverWorld = (ServerWorld) world;
        
        // 在周围区域增强土地
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                BlockPos pos = center.add(x, -1, z);
                BlockState state = world.getBlockState(pos);
                
                // 将泥土转换为耕地
                if (state.getBlock() == Blocks.DIRT || state.getBlock() == Blocks.GRASS_BLOCK) {
                    world.setBlockState(pos, Blocks.FARMLAND.getDefaultState());
                }
            }
        }
        
        // 给予玩家祝福效果
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 6000, 1));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SATURATION, 1200, 0));
        
        // 粒子效果
        serverWorld.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
            center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 0.5,
            30, 5.0, 2.0, 5.0, 0.1);
        
        world.playSound(null, center, SoundEvents.BLOCK_GRASS_PLACE, SoundCategory.PLAYERS, 1.0f, 1.2f);
        player.sendMessage(Text.literal("§2土地神佑已降临，大地将更加肥沃！"), false);
        
        return true;
    }

    private boolean castAbundance(World world, PlayerEntity player) {
        // 丰收 - 催熟周围作物
        BlockPos center = player.getBlockPos();
        ServerWorld serverWorld = (ServerWorld) world;
        
        for (int x = -8; x <= 8; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -8; z <= 8; z++) {
                    BlockPos pos = center.add(x, y, z);
                    BlockState state = world.getBlockState(pos);
                    
                    // 催熟作物
                    if (state.getBlock() instanceof CropBlock) {
                        CropBlock crop = (CropBlock) state.getBlock();
                        if (!crop.isMature(state)) {
                            world.setBlockState(pos, crop.withAge(crop.getMaxAge()));
                        }
                    }
                }
            }
        }
        
        // 给予玩家丰收祝福
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 12000, 2));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.HERO_OF_THE_VILLAGE, 12000, 1));
        
        // 粒子效果
        serverWorld.spawnParticles(ParticleTypes.COMPOSTER,
            center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 0.5,
            50, 8.0, 3.0, 8.0, 0.2);
        
        world.playSound(null, center, SoundEvents.ITEM_BONE_MEAL_USE, SoundCategory.PLAYERS, 1.5f, 1.0f);
        player.sendMessage(Text.literal("§6丰收神力已施展，作物茁壮成长！"), false);
        
        return true;
    }

    private boolean castFiveThunder(World world, PlayerEntity player) {
        // 五雷正法 - 召唤五道雷电攻击敌人
        BlockPos center = player.getBlockPos();
        ServerWorld serverWorld = (ServerWorld) world;
        
        // 寻找附近的敌对生物
        List<HostileEntity> enemies = world.getEntitiesByClass(HostileEntity.class,
            new Box(center).expand(20), entity -> entity.isAlive());
        
        int thunderCount = Math.min(5, enemies.size());
        if (thunderCount == 0) {
            player.sendMessage(Text.literal("§c附近没有敌人可以攻击！"), false);
            return false;
        }
        
        // 对前5个敌人施放雷电
        for (int i = 0; i < thunderCount; i++) {
            HostileEntity enemy = enemies.get(i);
            BlockPos enemyPos = enemy.getBlockPos();
            
            // 创建雷电
            LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, world);
            lightning.refreshPositionAfterTeleport(enemyPos.getX(), enemyPos.getY(), enemyPos.getZ());
            world.spawnEntity(lightning);
            
            // 额外伤害
            enemy.damage(world.getDamageSources().lightningBolt(), 15.0f);
            enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 200, 2));
        }
        
        // 给予玩家雷法加持
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 6000, 1));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 6000, 0));
        
        world.playSound(null, center, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 2.0f, 0.8f);
        player.sendMessage(Text.literal("§e五雷正法！天雷降世，邪魔退散！"), false);
        
        return true;
    }

    private boolean castCallToArms(World world, PlayerEntity player) {
        // 召唤守护者 - 召唤铁傀儡守护
        BlockPos center = player.getBlockPos();
        ServerWorld serverWorld = (ServerWorld) world;
        
        // 召唤3个铁傀儡
        for (int i = 0; i < 3; i++) {
            double angle = (i * 120.0) * Math.PI / 180.0;
            double x = center.getX() + Math.cos(angle) * 3;
            double z = center.getZ() + Math.sin(angle) * 3;
            
            IronGolemEntity golem = new IronGolemEntity(EntityType.IRON_GOLEM, world);
            golem.refreshPositionAfterTeleport(x, center.getY(), z);
            
            // 设置为玩家创造的
            golem.setPlayerCreated(true);
            
            world.spawnEntity(golem);
        }
        
        // 给予玩家领导力加持
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.HERO_OF_THE_VILLAGE, 12000, 2));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 6000, 1));
        
        // 粒子效果
        serverWorld.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING,
            center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 0.5,
            30, 3.0, 2.0, 3.0, 0.1);
        
        world.playSound(null, center, SoundEvents.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 1.0f, 1.2f);
        player.sendMessage(Text.literal("§6守护者已应召而来，将保护你的安全！"), false);
        
        return true;
    }

    private boolean castTaijiShield(World world, PlayerEntity player) {
        // 太极护盾 - 强大的防护效果
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 12000, 3));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 12000, 0));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 12000, 0));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 12000, 0));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 6000, 1));
        
        ServerWorld serverWorld = (ServerWorld) world;
        BlockPos center = player.getBlockPos();
        
        // 太极粒子效果
        for (int i = 0; i < 100; i++) {
            double angle = (i * 3.6) * Math.PI / 180.0;
            double radius = 3.0;
            double x = center.getX() + Math.cos(angle) * radius;
            double z = center.getZ() + Math.sin(angle) * radius;
            
            serverWorld.spawnParticles(ParticleTypes.END_ROD,
                x, center.getY() + 1.0, z, 1, 0.0, 0.0, 0.0, 0.0);
        }
        
        world.playSound(null, center, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1.5f, 1.0f);
        player.sendMessage(Text.literal("§d太极护盾已开启，阴阳调和，万法不侵！"), false);
        
        return true;
    }

    private boolean castGreatHunt(World world, PlayerEntity player) {
        // 巨鹰 - 召唤鹰协助狩猎
        BlockPos center = player.getBlockPos();
        ServerWorld serverWorld = (ServerWorld) world;
        
        // 召唤鹦鹉作为鹰的替代
        for (int i = 0; i < 3; i++) {
            ParrotEntity eagle = new ParrotEntity(EntityType.PARROT, world);
            eagle.refreshPositionAfterTeleport(
                center.getX() + (world.random.nextDouble() - 0.5) * 6,
                center.getY() + 3,
                center.getZ() + (world.random.nextDouble() - 0.5) * 6
            );
            
            // 设置鹦鹉变种为红色（类似鹰）
            eagle.setVariant(ParrotEntity.Variant.RED_BLUE);
            
            world.spawnEntity(eagle);
        }
        
        // 给予玩家狩猎加持
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 6000, 1));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 6000, 1));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 6000, 2));
        
        world.playSound(null, center, SoundEvents.ENTITY_PARROT_AMBIENT, SoundCategory.PLAYERS, 1.5f, 0.8f);
        player.sendMessage(Text.literal("§6巨鹰翱翔天际，将协助你的狩猎！"), false);
        
        return true;
    }

    private boolean castWalkingWoods(World world, PlayerEntity player) {
        // 行走的森林 - 将附近真实树木唤醒为行走树
        BlockPos center = player.getBlockPos();
        ServerWorld serverWorld = (ServerWorld) world;
        List<WalkingWoodSource> nearbyTrees = findWalkingWoodSources(world, center);

        if (nearbyTrees.isEmpty()) {
            player.sendMessage(Text.literal("§c附近没有足够完整的树木可以被唤醒。"), false);
            return false;
        }

        int awakenedCount = Math.min(nearbyTrees.size(), WALKING_WOODS_MAX_TREES);
        for (int i = 0; i < awakenedCount; i++) {
            WalkingWoodSource source = nearbyTrees.get(i);
            removeWalkingWoodSource(world, source);

            BlockPos spawnPos = source.basePos;
            WalkingTreeEntity walkingTree = new WalkingTreeEntity(ModEntities.WALKING_TREE, world);
            walkingTree.refreshPositionAfterTeleport(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
            walkingTree.setGuardPos(spawnPos);
            walkingTree.setSummoner(player);
            walkingTree.setVariant(source.variant);
            
            world.spawnEntity(walkingTree);

            serverWorld.spawnParticles(
                new BlockStateParticleEffect(ParticleTypes.BLOCK, getWalkingTreeLogBlock(source.variant).getDefaultState()),
                spawnPos.getX() + 0.5,
                spawnPos.getY() + 1.0,
                spawnPos.getZ() + 0.5,
                24,
                0.7,
                1.0,
                0.7,
                0.08
            );
        }
        
        // 给予玩家自然祝福
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 6000, 1));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 6000, 0));
        
        world.playSound(null, center, SoundEvents.BLOCK_WOOD_PLACE, SoundCategory.PLAYERS, 2.0f, 0.8f);
        player.sendMessage(Text.literal("§2森林苏醒了！" + awakenedCount + "棵附近树木化为行走树守护这片土地！"), false);
        
        return true;
    }

    private List<WalkingWoodSource> findWalkingWoodSources(World world, BlockPos center) {
        Set<BlockPos> logPositions = new HashSet<>();
        for (int dx = -WALKING_WOODS_SEARCH_RADIUS; dx <= WALKING_WOODS_SEARCH_RADIUS; dx++) {
            for (int dz = -WALKING_WOODS_SEARCH_RADIUS; dz <= WALKING_WOODS_SEARCH_RADIUS; dz++) {
                if (dx * dx + dz * dz > WALKING_WOODS_SEARCH_RADIUS * WALKING_WOODS_SEARCH_RADIUS) {
                    continue;
                }
                for (int dy = -WALKING_WOODS_VERTICAL_RADIUS; dy <= WALKING_WOODS_VERTICAL_RADIUS; dy++) {
                    BlockPos pos = center.add(dx, dy, dz);
                    if (isWalkingTreeLogBlock(world.getBlockState(pos).getBlock())) {
                        logPositions.add(pos.toImmutable());
                    }
                }
            }
        }

        List<WalkingWoodSource> sources = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        for (BlockPos start : logPositions) {
            if (visited.contains(start)) {
                continue;
            }

            List<BlockPos> cluster = new ArrayList<>();
            List<BlockPos> queue = new ArrayList<>();
            queue.add(start);
            visited.add(start);
            for (int i = 0; i < queue.size(); i++) {
                BlockPos current = queue.get(i);
                cluster.add(current);
                for (Direction direction : Direction.values()) {
                    BlockPos next = current.offset(direction).toImmutable();
                    if (logPositions.contains(next) && visited.add(next)) {
                        queue.add(next);
                    }
                }
            }

            if (cluster.size() < WALKING_WOODS_MIN_LOGS_PER_TREE) {
                continue;
            }

            WalkingTreeVariant variant = determineWalkingTreeVariantFromCluster(world, cluster);
            Set<BlockPos> leaves = collectWalkingTreeLeaves(world, cluster, variant);
            if (leaves.isEmpty()) {
                continue;
            }

            BlockPos basePos = cluster.stream()
                .min(Comparator.comparingInt(BlockPos::getY))
                .orElse(start)
                .toImmutable();
            sources.add(new WalkingWoodSource(basePos, variant, cluster, leaves));
        }

        sources.sort(Comparator.comparingDouble(source -> source.basePos.getSquaredDistance(center)));
        return sources;
    }

    private WalkingTreeVariant determineWalkingTreeVariantFromCluster(World world, List<BlockPos> cluster) {
        Map<WalkingTreeVariant, Integer> scores = new EnumMap<>(WalkingTreeVariant.class);
        for (WalkingTreeVariant value : WalkingTreeVariant.values()) {
            scores.put(value, 0);
        }
        for (BlockPos pos : cluster) {
            WalkingTreeVariant variant = getWalkingTreeVariantForBlock(world.getBlockState(pos).getBlock());
            if (variant != null) {
                scores.put(variant, scores.get(variant) + 1);
            }
        }

        WalkingTreeVariant bestVariant = WalkingTreeVariant.OAK;
        int bestScore = -1;
        for (Map.Entry<WalkingTreeVariant, Integer> entry : scores.entrySet()) {
            if (entry.getValue() > bestScore) {
                bestVariant = entry.getKey();
                bestScore = entry.getValue();
            }
        }
        return bestVariant;
    }

    private Set<BlockPos> collectWalkingTreeLeaves(World world, List<BlockPos> cluster, WalkingTreeVariant variant) {
        Set<BlockPos> leaves = new HashSet<>();
        for (BlockPos logPos : cluster) {
            for (int dx = -4; dx <= 4; dx++) {
                for (int dy = -2; dy <= 7; dy++) {
                    for (int dz = -4; dz <= 4; dz++) {
                        BlockPos pos = logPos.add(dx, dy, dz);
                        Block block = world.getBlockState(pos).getBlock();
                        if (isWalkingTreeLeafBlock(block) && getWalkingTreeVariantForBlock(block) == variant) {
                            leaves.add(pos.toImmutable());
                        }
                    }
                }
            }
        }
        return leaves;
    }

    private void removeWalkingWoodSource(World world, WalkingWoodSource source) {
        for (BlockPos pos : source.logPositions) {
            if (isWalkingTreeLogBlock(world.getBlockState(pos).getBlock())) {
                world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
            }
        }
        for (BlockPos pos : source.leafPositions) {
            if (isWalkingTreeLeafBlock(world.getBlockState(pos).getBlock())) {
                world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
            }
        }
    }

    private WalkingTreeVariant determineWalkingTreeVariant(World world, BlockPos center) {
        Map<WalkingTreeVariant, Integer> scores = new EnumMap<>(WalkingTreeVariant.class);
        for (WalkingTreeVariant value : WalkingTreeVariant.values()) {
            scores.put(value, 0);
        }

        for (int dx = -10; dx <= 10; dx++) {
            for (int dz = -10; dz <= 10; dz++) {
                for (int dy = -2; dy <= 8; dy++) {
                    Block block = world.getBlockState(center.add(dx, dy, dz)).getBlock();
                    WalkingTreeVariant variant = getWalkingTreeVariantForBlock(block);
                    if (variant != null) {
                        int weight = isWalkingTreeLogBlock(block) ? 3 : 1;
                        scores.put(variant, scores.get(variant) + weight);
                    }
                }
            }
        }

        WalkingTreeVariant bestVariant = WalkingTreeVariant.OAK;
        int bestScore = -1;
        for (Map.Entry<WalkingTreeVariant, Integer> entry : scores.entrySet()) {
            if (entry.getValue() > bestScore) {
                bestVariant = entry.getKey();
                bestScore = entry.getValue();
            }
        }
        return bestVariant;
    }

    private @Nullable WalkingTreeVariant getWalkingTreeVariantForBlock(Block block) {
        if (block == Blocks.OAK_LOG || block == Blocks.OAK_WOOD ||
            block == Blocks.STRIPPED_OAK_LOG || block == Blocks.STRIPPED_OAK_WOOD ||
            block == Blocks.OAK_LEAVES) {
            return WalkingTreeVariant.OAK;
        }
        if (block == Blocks.SPRUCE_LOG || block == Blocks.SPRUCE_WOOD ||
            block == Blocks.STRIPPED_SPRUCE_LOG || block == Blocks.STRIPPED_SPRUCE_WOOD ||
            block == Blocks.SPRUCE_LEAVES) {
            return WalkingTreeVariant.SPRUCE;
        }
        if (block == Blocks.BIRCH_LOG || block == Blocks.BIRCH_WOOD ||
            block == Blocks.STRIPPED_BIRCH_LOG || block == Blocks.STRIPPED_BIRCH_WOOD ||
            block == Blocks.BIRCH_LEAVES) {
            return WalkingTreeVariant.BIRCH;
        }
        if (block == Blocks.JUNGLE_LOG || block == Blocks.JUNGLE_WOOD ||
            block == Blocks.STRIPPED_JUNGLE_LOG || block == Blocks.STRIPPED_JUNGLE_WOOD ||
            block == Blocks.JUNGLE_LEAVES) {
            return WalkingTreeVariant.JUNGLE;
        }
        if (block == Blocks.ACACIA_LOG || block == Blocks.ACACIA_WOOD ||
            block == Blocks.STRIPPED_ACACIA_LOG || block == Blocks.STRIPPED_ACACIA_WOOD ||
            block == Blocks.ACACIA_LEAVES) {
            return WalkingTreeVariant.ACACIA;
        }
        if (block == Blocks.DARK_OAK_LOG || block == Blocks.DARK_OAK_WOOD ||
            block == Blocks.STRIPPED_DARK_OAK_LOG || block == Blocks.STRIPPED_DARK_OAK_WOOD ||
            block == Blocks.DARK_OAK_LEAVES) {
            return WalkingTreeVariant.DARK_OAK;
        }
        if (block == Blocks.MANGROVE_LOG || block == Blocks.MANGROVE_WOOD ||
            block == Blocks.STRIPPED_MANGROVE_LOG || block == Blocks.STRIPPED_MANGROVE_WOOD ||
            block == Blocks.MANGROVE_LEAVES || block == Blocks.MANGROVE_ROOTS ||
            block == Blocks.MUDDY_MANGROVE_ROOTS) {
            return WalkingTreeVariant.MANGROVE;
        }
        if (block == Blocks.CHERRY_LOG || block == Blocks.CHERRY_WOOD ||
            block == Blocks.STRIPPED_CHERRY_LOG || block == Blocks.STRIPPED_CHERRY_WOOD ||
            block == Blocks.CHERRY_LEAVES) {
            return WalkingTreeVariant.CHERRY;
        }
        return null;
    }

    private boolean isWalkingTreeLogBlock(Block block) {
        return block == Blocks.OAK_LOG || block == Blocks.OAK_WOOD ||
            block == Blocks.STRIPPED_OAK_LOG || block == Blocks.STRIPPED_OAK_WOOD ||
            block == Blocks.SPRUCE_LOG || block == Blocks.SPRUCE_WOOD ||
            block == Blocks.STRIPPED_SPRUCE_LOG || block == Blocks.STRIPPED_SPRUCE_WOOD ||
            block == Blocks.BIRCH_LOG || block == Blocks.BIRCH_WOOD ||
            block == Blocks.STRIPPED_BIRCH_LOG || block == Blocks.STRIPPED_BIRCH_WOOD ||
            block == Blocks.JUNGLE_LOG || block == Blocks.JUNGLE_WOOD ||
            block == Blocks.STRIPPED_JUNGLE_LOG || block == Blocks.STRIPPED_JUNGLE_WOOD ||
            block == Blocks.ACACIA_LOG || block == Blocks.ACACIA_WOOD ||
            block == Blocks.STRIPPED_ACACIA_LOG || block == Blocks.STRIPPED_ACACIA_WOOD ||
            block == Blocks.DARK_OAK_LOG || block == Blocks.DARK_OAK_WOOD ||
            block == Blocks.STRIPPED_DARK_OAK_LOG || block == Blocks.STRIPPED_DARK_OAK_WOOD ||
            block == Blocks.MANGROVE_LOG || block == Blocks.MANGROVE_WOOD ||
            block == Blocks.STRIPPED_MANGROVE_LOG || block == Blocks.STRIPPED_MANGROVE_WOOD ||
            block == Blocks.MANGROVE_ROOTS || block == Blocks.MUDDY_MANGROVE_ROOTS ||
            block == Blocks.CHERRY_LOG || block == Blocks.CHERRY_WOOD ||
            block == Blocks.STRIPPED_CHERRY_LOG || block == Blocks.STRIPPED_CHERRY_WOOD;
    }

    private boolean isWalkingTreeLeafBlock(Block block) {
        return block == Blocks.OAK_LEAVES ||
            block == Blocks.SPRUCE_LEAVES ||
            block == Blocks.BIRCH_LEAVES ||
            block == Blocks.JUNGLE_LEAVES ||
            block == Blocks.ACACIA_LEAVES ||
            block == Blocks.DARK_OAK_LEAVES ||
            block == Blocks.MANGROVE_LEAVES ||
            block == Blocks.CHERRY_LEAVES;
    }

    private Block getWalkingTreeLogBlock(WalkingTreeVariant variant) {
        return switch (variant) {
            case OAK -> Blocks.OAK_LOG;
            case SPRUCE -> Blocks.SPRUCE_LOG;
            case BIRCH -> Blocks.BIRCH_LOG;
            case JUNGLE -> Blocks.JUNGLE_LOG;
            case ACACIA -> Blocks.ACACIA_LOG;
            case DARK_OAK -> Blocks.DARK_OAK_LOG;
            case MANGROVE -> Blocks.MANGROVE_LOG;
            case CHERRY -> Blocks.CHERRY_LOG;
        };
    }

    private Block getWalkingTreeLeavesBlock(WalkingTreeVariant variant) {
        return switch (variant) {
            case OAK -> Blocks.OAK_LEAVES;
            case SPRUCE -> Blocks.SPRUCE_LEAVES;
            case BIRCH -> Blocks.BIRCH_LEAVES;
            case JUNGLE -> Blocks.JUNGLE_LEAVES;
            case ACACIA -> Blocks.ACACIA_LEAVES;
            case DARK_OAK -> Blocks.DARK_OAK_LEAVES;
            case MANGROVE -> Blocks.MANGROVE_LEAVES;
            case CHERRY -> Blocks.CHERRY_LEAVES;
        };
    }

    private String getWalkingTreeVariantName(WalkingTreeVariant variant) {
        return switch (variant) {
            case OAK -> "橡木";
            case SPRUCE -> "云杉";
            case BIRCH -> "白桦";
            case JUNGLE -> "丛林";
            case ACACIA -> "金合欢";
            case DARK_OAK -> "深色橡木";
            case MANGROVE -> "红树";
            case CHERRY -> "樱花";
        };
    }

    private static class WalkingWoodSource {
        private final BlockPos basePos;
        private final WalkingTreeVariant variant;
        private final List<BlockPos> logPositions;
        private final Set<BlockPos> leafPositions;

        private WalkingWoodSource(BlockPos basePos, WalkingTreeVariant variant, List<BlockPos> logPositions, Set<BlockPos> leafPositions) {
            this.basePos = basePos;
            this.variant = variant;
            this.logPositions = logPositions;
            this.leafPositions = leafPositions;
        }
    }

    // 中国神力的其他方法
    private boolean castRainPrayer(World world, PlayerEntity player, BlockPos targetPos) {
        // 祈雨 - 在目标区域降雨
        if (world.isClient) return false;
        
        ServerWorld serverWorld = (ServerWorld) world;
        
        // 设置天气为雨天
        serverWorld.setWeather(0, 6000, true, false);
        
        // 在目标区域生成雨水效果
        for (int i = 0; i < 50; i++) {
            double x = targetPos.getX() + (world.random.nextDouble() - 0.5) * 20;
            double y = targetPos.getY() + world.random.nextDouble() * 10;
            double z = targetPos.getZ() + (world.random.nextDouble() - 0.5) * 20;
            
            serverWorld.spawnParticles(ParticleTypes.RAIN,
                x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        }
        
        // 给予玩家水之祝福
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 6000, 0));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.CONDUIT_POWER, 6000, 0));
        
        world.playSound(null, targetPos, SoundEvents.WEATHER_RAIN, SoundCategory.WEATHER, 1.0f, 1.0f);
        player.sendMessage(Text.literal("§9祈雨成功！甘露降临大地！"), false);
        
        return true;
    }

    private boolean castEnlightenment(World world, PlayerEntity player, BlockPos targetPos) {
        // 开悟 - 获得经验和智慧
        player.addExperience(500);
        
        // 给予玩家智慧效果
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 12000, 2));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.HERO_OF_THE_VILLAGE, 12000, 1));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 12000, 0));
        
        // 在目标位置生成经验球
        if (!world.isClient) {
            for (int i = 0; i < 10; i++) {
                ExperienceOrbEntity orb = new ExperienceOrbEntity(world,
                    targetPos.getX() + (world.random.nextDouble() - 0.5) * 4,
                    targetPos.getY() + 1,
                    targetPos.getZ() + (world.random.nextDouble() - 0.5) * 4,
                    50);
                world.spawnEntity(orb);
            }
        }
        
        ServerWorld serverWorld = (ServerWorld) world;
        serverWorld.spawnParticles(ParticleTypes.ENCHANT,
            targetPos.getX() + 0.5, targetPos.getY() + 1.0, targetPos.getZ() + 0.5,
            50, 3.0, 3.0, 3.0, 0.1);
        
        world.playSound(null, targetPos, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.5f, 1.2f);
        player.sendMessage(Text.literal("§d开悟之光照耀心灵，智慧如泉涌！"), false);
        
        return true;
    }

    private boolean castCraneBlessing(World world, PlayerEntity player, BlockPos targetPos) {
        // 仙鹤祝福 - 飞行和治疗能力
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 12000, 0));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 12000, 3));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 6000, 2));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 6000, 1));
        
        // 召唤鹦鹉作为仙鹤
        if (!world.isClient) {
            for (int i = 0; i < 2; i++) {
                ParrotEntity crane = new ParrotEntity(EntityType.PARROT, world);
                crane.refreshPositionAfterTeleport(
                    targetPos.getX() + (world.random.nextDouble() - 0.5) * 4,
                    targetPos.getY() + 3,
                    targetPos.getZ() + (world.random.nextDouble() - 0.5) * 4
                );
                crane.setVariant(ParrotEntity.Variant.GRAY);
                world.spawnEntity(crane);
            }
        }
        
        ServerWorld serverWorld = (ServerWorld) world;
        serverWorld.spawnParticles(ParticleTypes.CLOUD,
            targetPos.getX() + 0.5, targetPos.getY() + 2.0, targetPos.getZ() + 0.5,
            30, 3.0, 2.0, 3.0, 0.1);
        
        world.playSound(null, targetPos, SoundEvents.ENTITY_PARROT_AMBIENT, SoundCategory.PLAYERS, 1.0f, 1.5f);
        player.sendMessage(Text.literal("§f仙鹤降临，赐予你飞翔的力量！"), false);
        
        return true;
    }

    private boolean castFlood(World world, PlayerEntity player, BlockPos targetPos) {
        // 洪水 - 在目标区域创造水流
        if (world.isClient) return false;
        
        // 在目标区域放置水
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                for (int y = 0; y <= 2; y++) {
                    BlockPos pos = targetPos.add(x, y, z);
                    if (world.getBlockState(pos).isAir()) {
                        world.setBlockState(pos, Blocks.WATER.getDefaultState());
                    }
                }
            }
        }
        
        // 对区域内的敌对生物造成伤害
        List<HostileEntity> enemies = world.getEntitiesByClass(HostileEntity.class,
            new Box(targetPos).expand(8), entity -> entity.isAlive());
        
        for (HostileEntity enemy : enemies) {
            enemy.damage(world.getDamageSources().drown(), 8.0f);
            enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 400, 2));
        }
        
        // 给予玩家水中优势
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 6000, 0));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, 6000, 1));
        
        ServerWorld serverWorld = (ServerWorld) world;
        serverWorld.spawnParticles(ParticleTypes.SPLASH,
            targetPos.getX(), targetPos.getY() + 1, targetPos.getZ(),
            100, 8.0, 3.0, 8.0, 0.5);
        
        world.playSound(null, targetPos, SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.PLAYERS, 2.0f, 0.8f);
        player.sendMessage(Text.literal("§1洪水滔天！水势汹涌，冲刷一切！"), false);
        
        return true;
    }

    private boolean castRecreation(World world, PlayerEntity player, BlockPos targetPos) {
        // 造化 - 创造生命和植物
        if (world.isClient) return false;
        
        // 在周围生成各种植物
        for (int i = 0; i < 30; i++) {
            double angle = (i * 12.0) * Math.PI / 180.0;
            double radius = 3.0 + world.random.nextDouble() * 5.0;
            int x = (int) (targetPos.getX() + Math.cos(angle) * radius);
            int z = (int) (targetPos.getZ() + Math.sin(angle) * radius);
            
            BlockPos plantPos = new BlockPos(x, targetPos.getY(), z);
            
            if (world.getBlockState(plantPos).isAir() && 
                (world.getBlockState(plantPos.down()).getBlock() == Blocks.GRASS_BLOCK ||
                 world.getBlockState(plantPos.down()).getBlock() == Blocks.DIRT)) {
                
                // 随机生成不同的植物
                Block[] plants = {Blocks.GRASS, Blocks.FERN, Blocks.DANDELION, 
                                Blocks.POPPY, Blocks.BLUE_ORCHID, Blocks.ALLIUM};
                Block plant = plants[world.random.nextInt(plants.length)];
                world.setBlockState(plantPos, plant.getDefaultState());
            }
        }
        
        // 召唤一些友好生物
        for (int i = 0; i < 3; i++) {
            double angle = (i * 120.0) * Math.PI / 180.0;
            double x = targetPos.getX() + Math.cos(angle) * 4;
            double z = targetPos.getZ() + Math.sin(angle) * 4;
            
            // 随机召唤不同的动物
            if (world.random.nextBoolean()) {
                PigEntity pig = new PigEntity(EntityType.PIG, world);
                pig.refreshPositionAfterTeleport(x, targetPos.getY(), z);
                world.spawnEntity(pig);
            } else {
                WolfEntity wolf = new WolfEntity(EntityType.WOLF, world);
                wolf.refreshPositionAfterTeleport(x, targetPos.getY(), z);
                wolf.setOwner(player);
                world.spawnEntity(wolf);
            }
        }
        
        // 给予玩家造化之力
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 6000, 1));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SATURATION, 3000, 1));
        
        ServerWorld serverWorld = (ServerWorld) world;
        serverWorld.spawnParticles(ParticleTypes.HEART,
            targetPos.getX(), targetPos.getY() + 1, targetPos.getZ(),
            50, 5.0, 3.0, 5.0, 0.1);
        
        world.playSound(null, targetPos, SoundEvents.BLOCK_GRASS_PLACE, SoundCategory.PLAYERS, 1.5f, 1.2f);
        player.sendMessage(Text.literal("§a造化之力显现！生命在此地蓬勃生长！"), false);
        
        return true;
    }

    // 北欧神力实现方法
    private boolean castFenrirHowl(World world, PlayerEntity player, BlockPos targetPos) {
        // 芬里尔之嚎 - 召唤狼群并恐吓敌人
        if (world.isClient) return false;
        
        // 召唤狼群
        for (int i = 0; i < 5; i++) {
            double angle = (i * 72.0) * Math.PI / 180.0;
            double x = targetPos.getX() + Math.cos(angle) * 3;
            double z = targetPos.getZ() + Math.sin(angle) * 3;
            
            WolfEntity wolf = new WolfEntity(EntityType.WOLF, world);
            wolf.refreshPositionAfterTeleport(x, targetPos.getY(), z);
            wolf.setOwner(player);
            wolf.setTamed(true);
            world.spawnEntity(wolf);
        }
        
        // 恐吓附近的敌对生物
        List<HostileEntity> enemies = world.getEntitiesByClass(HostileEntity.class,
            new Box(targetPos).expand(15), entity -> entity.isAlive());
        
        for (HostileEntity enemy : enemies) {
            enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 400, 2));
            enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 400, 1));
        }
        
        // 给予玩家狼王加持
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 6000, 1));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 6000, 1));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 6000, 0));
        
        ServerWorld serverWorld = (ServerWorld) world;
        serverWorld.spawnParticles(ParticleTypes.SOUL,
            targetPos.getX(), targetPos.getY() + 1, targetPos.getZ(),
            50, 5.0, 2.0, 5.0, 0.1);
        
        world.playSound(null, targetPos, SoundEvents.ENTITY_WOLF_HOWL, SoundCategory.PLAYERS, 2.0f, 0.8f);
        player.sendMessage(Text.literal("§8芬里尔之嚎响彻天地！狼群集结！"), false);
        
        return true;
    }

    private boolean castOdinEye(World world, PlayerEntity player, BlockPos targetPos) {
        // 奥丁之眼 - 透视和探测能力
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 12000, 0));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 12000, 0));
        
        // 让附近的所有生物发光
        List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class,
            new Box(targetPos).expand(30), entity -> entity.isAlive() && entity != player);
        
        for (LivingEntity entity : entities) {
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 6000, 0));
        }
        
        // 显示附近的矿物（通过粒子效果）
        if (!world.isClient) {
            ServerWorld serverWorld = (ServerWorld) world;
            
            for (int x = -20; x <= 20; x++) {
                for (int y = -10; y <= 10; y++) {
                    for (int z = -20; z <= 20; z++) {
                        BlockPos pos = targetPos.add(x, y, z);
                        BlockState state = world.getBlockState(pos);
                        
                        // 检测贵重矿物
                        if (state.getBlock() == Blocks.DIAMOND_ORE ||
                            state.getBlock() == Blocks.DEEPSLATE_DIAMOND_ORE ||
                            state.getBlock() == Blocks.GOLD_ORE ||
                            state.getBlock() == Blocks.DEEPSLATE_GOLD_ORE ||
                            state.getBlock() == Blocks.IRON_ORE ||
                            state.getBlock() == Blocks.DEEPSLATE_IRON_ORE) {
                            
                            serverWorld.spawnParticles(ParticleTypes.END_ROD,
                                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                                1, 0, 0, 0, 0);
                        }
                    }
                }
            }
        }
        
        world.playSound(null, targetPos, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.0f, 1.2f);
        player.sendMessage(Text.literal("§6奥丁之眼开启！洞察一切隐秘！"), false);
        
        return true;
    }

    private boolean castDwarvenMine(World world, PlayerEntity player, BlockPos targetPos) {
        // 矮人矿坑 - 创建向上凸起的小山包，包含丰富矿石
        if (world.isClient) return false;
        
        ServerWorld serverWorld = (ServerWorld) world;
        
        // 播放初始音效 - 大地震动声
        world.playSound(null, targetPos, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.0f, 0.5f);
        world.playSound(null, targetPos, SoundEvents.BLOCK_GRAVEL_BREAK, SoundCategory.PLAYERS, 2.0f, 0.8f);
        
        // 创建初始烟尘粒子效果
        for (int i = 0; i < 30; i++) {
            double offsetX = (world.random.nextDouble() - 0.5) * 12.0;
            double offsetZ = (world.random.nextDouble() - 0.5) * 12.0;
            double offsetY = world.random.nextDouble() * 3.0;
            
            serverWorld.spawnParticles(ParticleTypes.POOF,
                targetPos.getX() + 0.5 + offsetX, 
                targetPos.getY() + offsetY, 
                targetPos.getZ() + 0.5 + offsetZ,
                1, 0.2, 0.2, 0.2, 0.1);
                
            serverWorld.spawnParticles(ParticleTypes.LARGE_SMOKE,
                targetPos.getX() + 0.5 + offsetX, 
                targetPos.getY() + offsetY, 
                targetPos.getZ() + 0.5 + offsetZ,
                1, 0.1, 0.1, 0.1, 0.05);
        }
        
        // 使用线程实现逐渐隆起的效果
        new Thread(() -> {
            try {
                // 分5个阶段逐渐建造小山包
                for (int stage = 1; stage <= 5; stage++) {
                    final int currentStage = stage;
                    
                    // 在主线程中执行方块放置
                    serverWorld.getServer().execute(() -> {
                        buildMineHillStage(world, targetPos, currentStage);
                        
                        // 每个阶段的音效和粒子效果
                        world.playSound(null, targetPos, SoundEvents.BLOCK_STONE_PLACE, SoundCategory.PLAYERS, 
                            0.8f, 0.6f + currentStage * 0.1f);
                        
                        // 烟尘粒子效果
                        for (int i = 0; i < 15; i++) {
                            double offsetX = (world.random.nextDouble() - 0.5) * 8.0;
                            double offsetZ = (world.random.nextDouble() - 0.5) * 8.0;
                            double offsetY = world.random.nextDouble() * 2.0;
                            
                            serverWorld.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                                targetPos.getX() + 0.5 + offsetX, 
                                targetPos.getY() + currentStage + offsetY, 
                                targetPos.getZ() + 0.5 + offsetZ,
                                1, 0.1, 0.1, 0.1, 0.02);
                        }
                    });
                    
                    // 等待500毫秒再进行下一阶段
                    Thread.sleep(500);
                }
                
                // 最终完成效果
                serverWorld.getServer().execute(() -> {
                    // 最终音效 - 完成声
                    world.playSound(null, targetPos, SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 1.5f, 1.2f);
                    world.playSound(null, targetPos, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0f, 0.8f);
                    
                    // 最终粒子效果 - 金色光芒
                    for (int i = 0; i < 40; i++) {
                        double offsetX = (world.random.nextDouble() - 0.5) * 10.0;
                        double offsetZ = (world.random.nextDouble() - 0.5) * 10.0;
                        double offsetY = world.random.nextDouble() * 6.0;
                        
                        serverWorld.spawnParticles(ParticleTypes.FIREWORK,
                            targetPos.getX() + 0.5 + offsetX, 
                            targetPos.getY() + 3 + offsetY, 
                            targetPos.getZ() + 0.5 + offsetZ,
                            1, 0, 0, 0, 0);
                    }
                    
                    // 给予玩家矮人祝福
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 12000, 2));
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 12000, 2));
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 12000, 0));
                    
                    player.sendMessage(Text.literal("§6矮人矿坑隆起完成！丰富的矿藏等待开采！"), false);
                    player.sendMessage(Text.literal("§7获得矮人祝福：急迫III、幸运III、夜视"), false);
                });
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
        
        return true;
    }
    
    // 存储每个矿坑的随机形状参数
    private static class HillShape {
        final double radiusX, radiusZ;
        final int maxHeight;
        final double[] heightNoise;
        final double rotation;
        
        HillShape(net.minecraft.util.math.random.Random random) {
            // 随机椭圆半径 (4-8格)
            this.radiusX = 4.0 + random.nextDouble() * 4.0;
            this.radiusZ = 4.0 + random.nextDouble() * 4.0;
            
            // 随机高度 (3-6层)
            this.maxHeight = 3 + random.nextInt(4);
            
            // 随机旋转角度
            this.rotation = random.nextDouble() * Math.PI * 2;
            
            // 生成高度噪声数组用于创建不规则形状
            this.heightNoise = new double[16 * 16];
            for (int i = 0; i < heightNoise.length; i++) {
                heightNoise[i] = 0.7 + random.nextDouble() * 0.6; // 0.7-1.3的随机系数
            }
        }
        
        // 获取指定位置的高度系数
        double getHeightMultiplier(int x, int z) {
            int index = Math.abs((x + 8) * 16 + (z + 8)) % heightNoise.length;
            return heightNoise[index];
        }
    }
    
    // 为每个矿坑生成唯一的形状
    private HillShape currentHillShape;
    
    /**
     * 分阶段建造矿坑小山包 - 椭圆形随机形状
     */
    private void buildMineHillStage(World world, BlockPos centerPos, int stage) {
        // 第一次调用时生成随机形状
        if (currentHillShape == null || stage == 1) {
            currentHillShape = new HillShape(world.random);
        }
        
        HillShape shape = currentHillShape;
        
        // 计算椭圆范围
        int maxRangeX = (int) Math.ceil(shape.radiusX) + 1;
        int maxRangeZ = (int) Math.ceil(shape.radiusZ) + 1;
        
        for (int x = -maxRangeX; x <= maxRangeX; x++) {
            for (int z = -maxRangeZ; z <= maxRangeZ; z++) {
                // 应用旋转变换
                double rotatedX = x * Math.cos(shape.rotation) - z * Math.sin(shape.rotation);
                double rotatedZ = x * Math.sin(shape.rotation) + z * Math.cos(shape.rotation);
                
                // 计算椭圆距离
                double ellipseDistance = (rotatedX * rotatedX) / (shape.radiusX * shape.radiusX) + 
                                       (rotatedZ * rotatedZ) / (shape.radiusZ * shape.radiusZ);
                
                // 在椭圆内部
                if (ellipseDistance <= 1.0) {
                    // 获取该位置的高度系数
                    double heightMultiplier = shape.getHeightMultiplier(x, z);
                    
                    // 计算该位置的最大高度（基于距离中心的远近和随机系数）
                    double distanceFromCenter = Math.sqrt(ellipseDistance);
                    double heightFactor = (1.0 - distanceFromCenter * 0.8) * heightMultiplier;
                    int localMaxHeight = Math.max(1, (int)(shape.maxHeight * heightFactor));
                    
                    // 建造当前阶段应该建造的层数
                    for (int y = 0; y < Math.min(stage, localMaxHeight); y++) {
                        BlockPos pos = centerPos.add(x, y, z);
                        
                        // 选择方块类型
                        Block blockType = selectOreType(world.random, y, localMaxHeight);
                        world.setBlockState(pos, blockType.getDefaultState());
                    }
                }
            }
        }
    }
    
    /**
     * 根据高度和层数选择矿石类型
     */
    private Block selectOreType(net.minecraft.util.math.random.Random random, int currentY, int maxHeight) {
        float rand = random.nextFloat();
        
        if (currentY == 0) {
            // 底层主要是石头和基础矿石
            if (rand < 0.12f) {
                return Blocks.IRON_ORE;
            } else if (rand < 0.20f) {
                return Blocks.COAL_ORE;
            } else if (rand < 0.25f) {
                return Blocks.COPPER_ORE;
            } else {
                return Blocks.STONE;
            }
        } else if (currentY >= maxHeight - 1) {
            // 顶层有更多贵重矿石
            if (rand < 0.10f) {
                return Blocks.DIAMOND_ORE;
            } else if (rand < 0.25f) {
                return Blocks.GOLD_ORE;
            } else if (rand < 0.40f) {
                return Blocks.IRON_ORE;
            } else if (rand < 0.50f) {
                return Blocks.LAPIS_ORE;
            } else {
                return Blocks.STONE;
            }
        } else {
            // 中间层
            if (rand < 0.06f) {
                return Blocks.DIAMOND_ORE;
            } else if (rand < 0.18f) {
                return Blocks.GOLD_ORE;
            } else if (rand < 0.35f) {
                return Blocks.IRON_ORE;
            } else if (rand < 0.45f) {
                return Blocks.COAL_ORE;
            } else if (rand < 0.52f) {
                return Blocks.REDSTONE_ORE;
            } else {
                return Blocks.STONE;
            }
        }
    }

    private boolean castThorWrath(World world, PlayerEntity player, BlockPos targetPos) {
        // 雷神之怒 - 强力雷电攻击
        if (world.isClient) return false;
        
        // 在目标位置及周围创造多道雷电
        for (int i = 0; i < 7; i++) {
            double angle = (i * 51.4) * Math.PI / 180.0; // 360/7度间隔
            double radius = 3.0 + world.random.nextDouble() * 5.0;
            double x = targetPos.getX() + Math.cos(angle) * radius;
            double z = targetPos.getZ() + Math.sin(angle) * radius;
            
            LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, world);
            lightning.refreshPositionAfterTeleport(x, targetPos.getY(), z);
            world.spawnEntity(lightning);
        }
        
        // 对大范围内的敌对生物造成雷电伤害
        List<HostileEntity> enemies = world.getEntitiesByClass(HostileEntity.class,
            new Box(targetPos).expand(12), entity -> entity.isAlive());
        
        for (HostileEntity enemy : enemies) {
            enemy.damage(world.getDamageSources().lightningBolt(), 20.0f);
            enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 300, 3));
            enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 300, 2));
        }
        
        // 给予玩家雷神之力
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 6000, 2));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 6000, 1));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 6000, 1));
        
        world.playSound(null, targetPos, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 3.0f, 0.5f);
        player.sendMessage(Text.literal("§e雷神之怒！天雷滚滚，震撼九霄！"), false);
        
        return true;
    }

    private boolean castValhallaCall(World world, PlayerEntity player, BlockPos targetPos) {
        // 英灵殿召唤 - 召唤战士灵魂
        if (world.isClient) return false;
        
        // 召唤骷髅战士作为英灵
        for (int i = 0; i < 6; i++) {
            double angle = (i * 60.0) * Math.PI / 180.0;
            double x = targetPos.getX() + Math.cos(angle) * 4;
            double z = targetPos.getZ() + Math.sin(angle) * 4;
            
            SkeletonEntity warrior = new SkeletonEntity(EntityType.SKELETON, world);
            warrior.refreshPositionAfterTeleport(x, targetPos.getY(), z);
            
            // 给骷髅装备更好的装备
            warrior.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            warrior.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            warrior.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
            
            // 增强属性
            warrior.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 12000, 1));
            warrior.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 12000, 1));
            warrior.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 12000, 0));
            
            world.spawnEntity(warrior);
        }
        
        // 给予玩家英灵加持
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.HERO_OF_THE_VILLAGE, 12000, 2));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 6000, 1));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 6000, 1));
        
        ServerWorld serverWorld = (ServerWorld) world;
        serverWorld.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME,
            targetPos.getX(), targetPos.getY() + 1, targetPos.getZ(),
            50, 5.0, 3.0, 5.0, 0.1);
        
        world.playSound(null, targetPos, SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.PLAYERS, 1.0f, 1.5f);
        player.sendMessage(Text.literal("§6英灵殿之门开启！战士之魂应召而来！"), false);
        
        return true;
    }

    private boolean castFrost(World world, PlayerEntity player, BlockPos targetPos) {
        // 霜冻 - 冰冻区域和敌人
        if (world.isClient) return false;
        
        // 在目标区域创造冰霜效果
        for (int x = -6; x <= 6; x++) {
            for (int z = -6; z <= 6; z++) {
                for (int y = -1; y <= 2; y++) {
                    BlockPos pos = targetPos.add(x, y, z);
                    BlockState state = world.getBlockState(pos);
                    
                    // 将水变成冰
                    if (state.getBlock() == Blocks.WATER) {
                        world.setBlockState(pos, Blocks.ICE.getDefaultState());
                    }
                    // 在地面放置雪
                    else if (y == 0 && state.isAir() && 
                            world.getBlockState(pos.down()).isSolidBlock(world, pos.down())) {
                        world.setBlockState(pos, Blocks.SNOW.getDefaultState());
                    }
                }
            }
        }
        
        // 冰冻附近的敌对生物
        List<HostileEntity> enemies = world.getEntitiesByClass(HostileEntity.class,
            new Box(targetPos).expand(10), entity -> entity.isAlive());
        
        for (HostileEntity enemy : enemies) {
            enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 600, 4));
            enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 600, 2));
            enemy.damage(world.getDamageSources().freeze(), 6.0f);
        }
        
        // 给予玩家冰霜抗性
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 6000, 0));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 6000, 0));
        
        ServerWorld serverWorld = (ServerWorld) world;
        serverWorld.spawnParticles(ParticleTypes.SNOWFLAKE,
            targetPos.getX(), targetPos.getY() + 2, targetPos.getZ(),
            100, 8.0, 3.0, 8.0, 0.1);
        
        world.playSound(null, targetPos, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1.5f, 0.8f);
        player.sendMessage(Text.literal("§b霜冻降临！冰雪封印大地！"), false);
        
        return true;
    }

    private boolean castRagnarok(World world, PlayerEntity player, BlockPos targetPos) {
        // 诸神黄昏 - 毁灭性攻击
        if (world.isClient) return false;
        
        // 创造多个爆炸
        for (int i = 0; i < 10; i++) {
            double angle = (i * 36.0) * Math.PI / 180.0;
            double radius = 5.0 + world.random.nextDouble() * 10.0;
            double x = targetPos.getX() + Math.cos(angle) * radius;
            double z = targetPos.getZ() + Math.sin(angle) * radius;
            
            world.createExplosion(null, x, targetPos.getY(), z, 4.0f, World.ExplosionSourceType.NONE);
        }
        
        // 召唤多道雷电
        for (int i = 0; i < 15; i++) {
            double x = targetPos.getX() + (world.random.nextDouble() - 0.5) * 30;
            double z = targetPos.getZ() + (world.random.nextDouble() - 0.5) * 30;
            
            LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, world);
            lightning.refreshPositionAfterTeleport(x, targetPos.getY(), z);
            world.spawnEntity(lightning);
        }
        
        // 对大范围内的所有敌对生物造成巨大伤害
        List<HostileEntity> enemies = world.getEntitiesByClass(HostileEntity.class,
            new Box(targetPos).expand(25), entity -> entity.isAlive());
        
        for (HostileEntity enemy : enemies) {
            enemy.damage(world.getDamageSources().explosion(null, null), 30.0f);
            enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 400, 2));
        }
        
        // 给予玩家终极加持
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 12000, 3));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 12000, 2));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 12000, 0));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 6000, 2));
        
        world.playSound(null, targetPos, SoundEvents.ENTITY_ENDER_DRAGON_DEATH, SoundCategory.PLAYERS, 2.0f, 0.5f);
        player.sendMessage(Text.literal("§4诸神黄昏！世界末日降临！"), false);
        
        return true;
    }

    private boolean castFimbulwinter(World world, PlayerEntity player, BlockPos targetPos) {
        // 芬布尔之冬 - 极寒风暴
        if (world.isClient) return false;
        
        ServerWorld serverWorld = (ServerWorld) world;
        
        // 设置天气为暴风雪
        serverWorld.setWeather(0, 12000, true, true);
        
        // 在大范围内创造冰雪效果
        for (int x = -15; x <= 15; x++) {
            for (int z = -15; z <= 15; z++) {
                for (int y = -2; y <= 5; y++) {
                    BlockPos pos = targetPos.add(x, y, z);
                    BlockState state = world.getBlockState(pos);
                    
                    // 将水变成冰
                    if (state.getBlock() == Blocks.WATER) {
                        world.setBlockState(pos, Blocks.ICE.getDefaultState());
                    }
                    // 在地面和植物上放置雪
                    else if (y >= 0 && state.isAir() && 
                            world.getBlockState(pos.down()).isSolidBlock(world, pos.down())) {
                        if (world.random.nextFloat() < 0.7f) {
                            world.setBlockState(pos, Blocks.SNOW.getDefaultState());
                        }
                    }
                    // 冰冻植物
                    else if (state.getBlock() instanceof PlantBlock) {
                        if (world.random.nextFloat() < 0.3f) {
                            world.setBlockState(pos, Blocks.AIR.getDefaultState());
                        }
                    }
                }
            }
        }
        
        // 对大范围内的所有生物造成冰冻伤害（除了玩家）
        List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class,
            new Box(targetPos).expand(20), entity -> entity.isAlive() && entity != player);
        
        for (LivingEntity entity : entities) {
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 1200, 3));
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 1200, 2));
            entity.damage(world.getDamageSources().freeze(), 8.0f);
            
            if (entity instanceof HostileEntity) {
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 1200, 2));
            }
        }
        
        // 给予玩家极寒抗性和力量
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 12000, 0));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 12000, 2));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 12000, 2));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 12000, 1));
        
        // 大量雪花粒子效果
        serverWorld.spawnParticles(ParticleTypes.SNOWFLAKE,
            targetPos.getX(), targetPos.getY() + 5, targetPos.getZ(),
            200, 20.0, 8.0, 20.0, 0.2);
        
        world.playSound(null, targetPos, SoundEvents.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.WEATHER, 2.0f, 0.3f);
        player.sendMessage(Text.literal("§f芬布尔之冬降临！永恒的严寒统治大地！"), false);
        
        return true;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);

        if (!world.isClient) {
            // Server-side logic for spell effect
            if (user.getItemCooldownManager().isCoolingDown(this)) {
                user.sendMessage(Text.translatable("item.mythic_age.spellbook.on_cooldown"), true);
                return TypedActionResult.fail(itemStack);
            }

            // Cast the specific god power based on spell type
            boolean success = castGodPower(world, user, spellType);
            
            if (success) {
                // Apply cooldown
                if (this.cooldownTicks > 0) {
                    user.getItemCooldownManager().set(this, this.cooldownTicks);
                }

                playSpellCastSound(world, user.getBlockPos());
                
                user.sendMessage(Text.translatable("item.mythic_age.spellbook.cast_success", 
                    Text.translatable("spell." + spellType.name().toLowerCase())), false);
                
                return TypedActionResult.success(itemStack, world.isClient());
            } else {
                user.sendMessage(Text.translatable("item.mythic_age.spellbook.cast_failed"), true);
                return TypedActionResult.fail(itemStack);
            }
        }

        return TypedActionResult.pass(itemStack);
    }

    private void playSpellCastSound(World world, BlockPos pos) {
        world.playSound(null, pos, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.8F, 1.05F);
    }

    private boolean castGodPower(World world, PlayerEntity player, SpellType spellType) {
        switch (spellType) {
            // 希腊神力
            case LIGHTNING_STORM:
                return castLightningStorm(world, player);
            case RESTORATION:
                return castRestoration(world, player);
            case LURE:
                return castLure(world, player);
            case UNDERWORLD_ARMY:
                return castUnderworldArmy(world, player);
            case PESTILENCE:
                return castPestilence(world, player);
            case CURSE:
                return castCurse(world, player);
            case EARTHQUAKE:
                return castEarthquake(world, player);
            case CEASEFIRE:
                return castCeasefire(world, player);
            case BRONZE:
                return castBronze(world, player);
            case UNDERWORLD_PASSAGE:
                return castUnderworldPassage(world, player);
            case LIGHTNING_STORM_HERA:
                return castLightningStormHera(world, player);
            case PLENTY:
                return castPlenty(world, player);
                
            // 中国神力
            case RAIN_PRAYER:
                return castRainPrayer(world, player, getTargetPos(world, player));
            case EARTH_GOD_BLESSING:
                return castEarthGodBlessing(world, player);
            case ENLIGHTENMENT:
                return castEnlightenment(world, player, getTargetPos(world, player));
            case ABUNDANCE:
                return castAbundance(world, player);
            case FIVE_THUNDER:
                return castFiveThunder(world, player);
            case CRANE_BLESSING:
                return castCraneBlessing(world, player, getTargetPos(world, player));
            case FLOOD:
                return castFlood(world, player, getTargetPos(world, player));
            case CALL_TO_ARMS:
                return castCallToArms(world, player);
            case TAIJI_SHIELD:
                return castTaijiShield(world, player);
            case RECREATION:
                return castRecreation(world, player, getTargetPos(world, player));
                
            // 北欧神力
            case FENRIR_HOWL:
                return castFenrirHowl(world, player, getTargetPos(world, player));
            case ODIN_EYE:
                return castOdinEye(world, player, getTargetPos(world, player));
            case GREAT_HUNT:
                return castGreatHunt(world, player);
            case DWARVEN_MINE:
                return castDwarvenMine(world, player, getTargetPos(world, player));
            case THOR_WRATH:
                return castThorWrath(world, player, getTargetPos(world, player));
            case VALHALLA_CALL:
                return castValhallaCall(world, player, getTargetPos(world, player));
            case FROST:
                return castFrost(world, player, getTargetPos(world, player));
            case WALKING_WOODS:
                return castWalkingWoods(world, player);
            case RAGNAROK:
                return castRagnarok(world, player, getTargetPos(world, player));
            case FIMBULWINTER:
                return castFimbulwinter(world, player, getTargetPos(world, player));
                
            // 埃及神力
            case RA_RADIANCE:
                castRaRadiance(world, player, getTargetPos(world, player));
                return true;
            case DESERT_STORM:
                castDesertStorm(world, player, getTargetPos(world, player));
                return true;
            case SUN_RAIN:
                castSunRain(world, player, getTargetPos(world, player));
                return true;
            case PROSPERITY:
                castProsperity(world, player, getTargetPos(world, player));
                return true;
            case EYE_OF_HORUS:
                castEyeOfHorus(world, player, getTargetPos(world, player));
                return true;
            case ANUBIS_JUDGMENT:
                castAnubisJudgment(world, player, getTargetPos(world, player));
                return true;
            case LOCUST_SWARM:
                castLocustSwarm(world, player, getTargetPos(world, player));
                return true;
            case TORNADO:
                castTornado(world, player, getTargetPos(world, player));
                return true;
            case ISIS_RESURRECTION:
                castIsisResurrection(world, player, getTargetPos(world, player));
                return true;
            case METEOR:
                castMeteorRain(world, player, getTargetPos(world, player));
                return true;

                
            // 神殿生成
            case GENERATE_EGYPTIAN_TEMPLE:
                return castTempleGenerationSpell(world, player, player.getBlockPos(), "egyptian");
            case GENERATE_CHINESE_TEMPLE:
                return castTempleGenerationSpell(world, player, player.getBlockPos(), "chinese");
            case GENERATE_ATLANTEAN_TEMPLE:
                return castTempleGenerationSpell(world, player, player.getBlockPos(), "atlantean");
            case GENERATE_NORDIC_TEMPLE:
                return castTempleGenerationSpell(world, player, player.getBlockPos(), "nordic");
            default:
                return false;
        }
    }

    private boolean castLightningStorm(World world, PlayerEntity player) {
        // 以玩家位置为中心启动闪电风暴
        BlockPos stormCenter = player.getBlockPos();
        
        // 启动持续性闪电风暴系统
        String stormId = "lightning_storm_" + player.getUuid().toString() + "_" + System.currentTimeMillis();
        com.example.mythic_age.util.LightningStormManager.getInstance().startLightningStorm(world, stormCenter, stormId);
        
        // 初始风暴启动效果
        ServerWorld serverWorld = (ServerWorld) world;
        
        // 风暴开始的雷云聚集效果
        for (int i = 0; i < 50; i++) {
            double offsetX = (world.random.nextDouble() - 0.5) * 40.0;
            double offsetZ = (world.random.nextDouble() - 0.5) * 40.0;
            double offsetY = 15 + world.random.nextDouble() * 10;
            
            serverWorld.spawnParticles(ParticleTypes.CLOUD,
                stormCenter.getX() + offsetX, stormCenter.getY() + offsetY, stormCenter.getZ() + offsetZ,
                1, 0, 0, 0, 0);
        }
        
        // 初始闪电展示
        for (int i = 0; i < 5; i++) {
            double offsetX = (world.random.nextDouble() - 0.5) * 20.0;
            double offsetZ = (world.random.nextDouble() - 0.5) * 20.0;
            BlockPos lightningPos = stormCenter.add((int)offsetX, 0, (int)offsetZ);
            
            // 创建装饰性闪电
            LightningEntity lightning = new LightningEntity(net.minecraft.entity.EntityType.LIGHTNING_BOLT, world);
            lightning.refreshPositionAfterTeleport(
                lightningPos.getX() + 0.5, 
                lightningPos.getY(), 
                lightningPos.getZ() + 0.5
            );
            world.spawnEntity(lightning);
            
            // 闪电粒子效果
            serverWorld.spawnParticles(ParticleTypes.ELECTRIC_SPARK, 
                lightningPos.getX() + 0.5, lightningPos.getY() + 5, lightningPos.getZ() + 0.5,
                15, 1.0, 3.0, 1.0, 0.2);
        }
        
        // 风暴启动音效
        world.playSound(null, stormCenter, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 
            SoundCategory.WEATHER, 2.0f, 0.8f);
        world.playSound(null, stormCenter, SoundEvents.ITEM_TRIDENT_THUNDER, 
            SoundCategory.WEATHER, 1.5f, 1.0f);
        
        // 通知玩家
        player.sendMessage(net.minecraft.text.Text.literal("§6闪电风暴已降临！持续60秒，所有生物将面临雷神的愤怒！"), false);
        
        return true;
    }

    private boolean castRestoration(World world, PlayerEntity player) {
        // Heal the player and nearby allies
        player.heal(10.0f); // Heal 5 hearts
        
        // Heal nearby players within 10 blocks
        world.getEntitiesByClass(PlayerEntity.class, 
            player.getBoundingBox().expand(10.0), 
            p -> p != player && p.squaredDistanceTo(player) <= 100.0)
            .forEach(p -> p.heal(10.0f));
        
        // Visual and audio effects
        ServerWorld serverWorld = (ServerWorld) world;
        serverWorld.spawnParticles(ParticleTypes.HEART, 
            player.getX(), player.getY() + 1, player.getZ(),
            20, 1.0, 1.0, 1.0, 0.1);
        
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_LEVELUP, 
            SoundCategory.PLAYERS, 0.5f, 1.5f);
        
        return true;
    }

    private boolean castLure(World world, PlayerEntity player) {
        // Get target position for tower placement
        Vec3d start = player.getEyePos();
        Vec3d direction = player.getRotationVector();
        Vec3d end = start.add(direction.multiply(30.0));
        
        BlockHitResult hitResult = world.raycast(new RaycastContext(start, end, 
            RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, player));
        
        BlockPos lurePos;
        if (hitResult.getType() == HitResult.Type.MISS) {
            // Use a position 8 blocks in front of player if raycast misses
            lurePos = player.getBlockPos().add(
                (int)(direction.x * 8), 0, (int)(direction.z * 8));
        } else {
            lurePos = hitResult.getBlockPos().up(); // Place above the clicked block
        }
        
        // Check if there's space for the 3-block tower
        if (!canPlaceLureTower(world, lurePos)) {
            player.sendMessage(net.minecraft.text.Text.literal("§c此处空间不足，无法建造招引塔！"), false);
            return false;
        }
        
        // Build the animal lure tower
        buildAnimalLureTower(world, lurePos, player);
        
        // Visual effects - magical construction
        ServerWorld serverWorld = (ServerWorld) world;
        
        // Dramatic construction effects
        for (int i = 0; i < 30; i++) {
            double angle = (i / 30.0) * 2 * Math.PI;
            double radius = 3.0;
            double x = lurePos.getX() + 0.5 + Math.cos(angle) * radius;
            double z = lurePos.getZ() + 0.5 + Math.sin(angle) * radius;
            
            serverWorld.spawnParticles(ParticleTypes.HEART,
                x, lurePos.getY() + 1.5, z, 1, 0, 0, 0, 0);
        }
        
        // Construction particle beam
        serverWorld.spawnParticles(ParticleTypes.ENCHANT,
            lurePos.getX() + 0.5, lurePos.getY() + 1.5, lurePos.getZ() + 0.5,
            20, 1.0, 1.5, 1.0, 0.2);
        
        // Sound effects
        world.playSound(null, lurePos, SoundEvents.BLOCK_BEACON_ACTIVATE, 
            SoundCategory.BLOCKS, 1.0f, 1.2f);
        world.playSound(null, lurePos, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 
            SoundCategory.NEUTRAL, 1.5f, 0.8f);
        
        // Notify player
        player.sendMessage(net.minecraft.text.Text.literal("§a动物招引塔建造完成！核心方块朝向您，5层十字形结构将持续5分钟吸引动物并促进草地生长。"), false);
        
        return true;
    }
    
    /**
     * 检查是否可以放置招引塔 - 5层十字形结构
     */
    private boolean canPlaceLureTower(World world, BlockPos basePos) {
        // 定义十字形相对位置 (中心点 + 四个方向)
        BlockPos[] crossPositions = {
            new BlockPos(0, 0, 0),    // 中心
            new BlockPos(1, 0, 0),    // 东
            new BlockPos(-1, 0, 0),   // 西
            new BlockPos(0, 0, 1),    // 南
            new BlockPos(0, 0, -1)    // 北
        };
        
        // 检查5层结构的所有位置
        for (int layer = 0; layer < 5; layer++) {
            BlockPos layerPos = basePos.up(layer);
            
            // 第5层(最顶层)只有中心一个方块
            if (layer == 4) {
                if (!world.getBlockState(layerPos).isAir()) {
                    return false;
                }
            } else {
                // 其他层检查整个十字形
                for (BlockPos relPos : crossPositions) {
                    BlockPos checkPos = layerPos.add(relPos);
                    if (!world.getBlockState(checkPos).isAir()) {
                        return false;
                    }
                }
            }
        }
        
        // 检查地面是否有足够支撑
        for (BlockPos relPos : crossPositions) {
            BlockPos groundPos = basePos.down().add(relPos);
            if (!world.getBlockState(groundPos).isSolidBlock(world, groundPos)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 建造动物招引塔 - 5层十字形结构，核心方块在靠近玩家的一侧
     */
    private void buildAnimalLureTower(World world, BlockPos basePos, PlayerEntity player) {
        // 定义十字形相对位置 (中心点 + 四个方向)
        BlockPos[] crossPositions = {
            new BlockPos(0, 0, 0),    // 中心
            new BlockPos(1, 0, 0),    // 东
            new BlockPos(-1, 0, 0),   // 西
            new BlockPos(0, 0, 1),    // 南
            new BlockPos(0, 0, -1)    // 北
        };
        
        // 计算动物招引塔方块的朝向（正面朝向玩家）
        Direction towerFacing = player.getHorizontalFacing().getOpposite();
        
        // 计算核心方块的相对位置（在靠近玩家的位置）
        BlockPos coreRelativePos = new BlockPos(
            towerFacing.getOffsetX(),   // 方块朝向相反，所以核心位置用正偏移
            0,
            towerFacing.getOffsetZ()
        );
        
        // 第1层：十字形的5个苔石
        for (BlockPos relPos : crossPositions) {
            world.setBlockState(basePos.add(relPos), Blocks.MOSSY_COBBLESTONE.getDefaultState());
        }
        
        // 第2层：十字形的5个苔石
        for (BlockPos relPos : crossPositions) {
            world.setBlockState(basePos.up().add(relPos), Blocks.MOSSY_COBBLESTONE.getDefaultState());
        }
        
        // 第3层：动物招引塔方块在靠近玩家的位置，其余为苔石
        for (BlockPos relPos : crossPositions) {
            BlockPos targetPos = basePos.up(2).add(relPos);
            if (relPos.equals(coreRelativePos)) { // 核心方块位置
                world.setBlockState(targetPos, 
                    com.example.mythic_age.block.ModBlocks.ANIMAL_LURE_TOWER.getDefaultState()
                        .with(com.example.mythic_age.block.AnimalLureTowerBlock.FACING, towerFacing));
            } else {
                world.setBlockState(targetPos, Blocks.MOSSY_COBBLESTONE.getDefaultState());
            }
        }
        
        // 第4层：十字形的5个苔石
        for (BlockPos relPos : crossPositions) {
            world.setBlockState(basePos.up(3).add(relPos), Blocks.MOSSY_COBBLESTONE.getDefaultState());
        }
        
        // 第5层：中心一个苔石
        world.setBlockState(basePos.up(4), Blocks.MOSSY_COBBLESTONE.getDefaultState());
    }

    private boolean castUnderworldArmy(World world, PlayerEntity player) {
        ServerWorld serverWorld = (ServerWorld) world;
        int summonCount = 3 + world.random.nextInt(3); // 3-5 skeletons
        
        for (int i = 0; i < summonCount; i++) {
            // Find a suitable spawn position around the player
            BlockPos spawnPos = player.getBlockPos().add(
                world.random.nextInt(6) - 3, 0, world.random.nextInt(6) - 3);
            
            // Find ground level
            while (spawnPos.getY() > world.getBottomY() && 
                   !world.getBlockState(spawnPos.down()).isSolidBlock(world, spawnPos.down())) {
                spawnPos = spawnPos.down();
            }
            
            // Spawn skeleton
            net.minecraft.entity.mob.SkeletonEntity skeleton = 
                new net.minecraft.entity.mob.SkeletonEntity(net.minecraft.entity.EntityType.SKELETON, world);
            skeleton.refreshPositionAfterTeleport(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
            
            // Make skeleton friendly to the player (won't attack the summoner)
            skeleton.setTarget(null);
            
            world.spawnEntity(skeleton);
            
            // Spawn particles
            serverWorld.spawnParticles(ParticleTypes.SMOKE, 
                spawnPos.getX() + 0.5, spawnPos.getY() + 1, spawnPos.getZ() + 0.5,
                15, 0.3, 0.5, 0.3, 0.05);
        }
        
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_WITHER_SPAWN, 
            SoundCategory.HOSTILE, 0.5f, 1.2f);
        
        return true;
    }

    private boolean castPestilence(World world, PlayerEntity player) {
        // Get target position
        Vec3d start = player.getEyePos();
        Vec3d direction = player.getRotationVector();
        Vec3d end = start.add(direction.multiply(40.0));
        
        BlockHitResult hitResult = world.raycast(new RaycastContext(start, end, 
            RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, player));
        
        BlockPos targetPos;
        if (hitResult.getType() == HitResult.Type.MISS) {
            // Use a position 12 blocks in front of player if raycast misses
            targetPos = player.getBlockPos().add(
                (int)(direction.x * 12), 0, (int)(direction.z * 12));
        } else {
            targetPos = hitResult.getBlockPos();
        }
        
        ServerWorld serverWorld = (ServerWorld) world;
        
        // Apply pestilence effect to hostile mobs in area
        world.getEntitiesByClass(HostileEntity.class,
            Box.of(Vec3d.ofCenter(targetPos), 20, 20, 20),
            mob -> true)
            .forEach(mob -> {
                mob.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 600, 1)); // 30 seconds
                mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 600, 1));
                mob.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 200, 0)); // 10 seconds
            });
        
        // Visual effects - dark particles
        serverWorld.spawnParticles(ParticleTypes.SQUID_INK, 
            targetPos.getX() + 0.5, targetPos.getY() + 1, targetPos.getZ() + 0.5,
            50, 3.0, 2.0, 3.0, 0.1);
        
        world.playSound(null, targetPos, SoundEvents.ENTITY_WITCH_AMBIENT, 
            SoundCategory.HOSTILE, 1.0f, 0.6f);
        
        return true;
    }

    private boolean castCurse(World world, PlayerEntity player) {
        ServerWorld serverWorld = (ServerWorld) world;
        
        // 检测以玩家为中心半径10格内的所有非玩家生物
        List<LivingEntity> nearbyEntities = world.getEntitiesByClass(LivingEntity.class,
            player.getBoundingBox().expand(10.0),
            entity -> entity != player && !(entity instanceof PlayerEntity)); // 排除玩家自己和其他玩家
        
        int transformCount = 0;
        
        // 在玩家位置显示法术启动效果
        serverWorld.spawnParticles(ParticleTypes.WITCH, 
            player.getX(), player.getY() + 1, player.getZ(),
            20, 1.0, 1.0, 1.0, 0.1);
        
        // 播放法术启动音效
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_WITCH_AMBIENT, 
            SoundCategory.NEUTRAL, 1.0f, 1.2f);
        
        // 变换所有找到的非玩家生物
        for (LivingEntity entity : nearbyEntities) {
            // 在原生物位置生成猪
            PigEntity pig = new PigEntity(net.minecraft.entity.EntityType.PIG, world);
            pig.refreshPositionAfterTeleport(entity.getX(), entity.getY(), entity.getZ());
            
            // 如果原生物是驯服的，猪也保持驯服状态
            if (entity instanceof TameableEntity tameable && tameable.isTamed()) {
                // 猪无法驯服，但我们可以给它一个特殊的名字标记
                pig.setCustomName(Text.literal("§6被诅咒的" + entity.getDisplayName().getString()));
            } else {
                pig.setCustomName(Text.literal("§7被诅咒的生物"));
            }
            
            world.spawnEntity(pig);
            
            // 移除原生物
            entity.discard();
            
            // 在变换位置生成粒子效果
            serverWorld.spawnParticles(ParticleTypes.POOF, 
                pig.getX(), pig.getY() + 1, pig.getZ(),
                15, 0.5, 0.5, 0.5, 0.1);
            
            // 播放变换音效
            world.playSound(null, pig.getBlockPos(), SoundEvents.ENTITY_PIG_AMBIENT, 
                SoundCategory.NEUTRAL, 0.8f, 1.0f + world.random.nextFloat() * 0.4f);
            
            transformCount++;
        }
        
        // 在玩家周围显示法术完成效果
        serverWorld.spawnParticles(ParticleTypes.ENCHANT, 
            player.getX(), player.getY() + 1, player.getZ(),
            30, 3.0, 1.0, 3.0, 0.2);
        
        // 根据变换数量播放不同的完成音效
        if (transformCount > 0) {
            world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_WITCH_CELEBRATE, 
                SoundCategory.NEUTRAL, 1.0f, 1.0f);
            
            // 向玩家发送变换成功的消息
            player.sendMessage(Text.literal("§d诅咒成功！变换了 " + transformCount + " 个生物为猪"), false);
        } else {
            // 即使没有生物也播放音效，表示法术成功释放
            world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_WITCH_HURT, 
                SoundCategory.NEUTRAL, 0.5f, 1.5f);
            
            player.sendMessage(Text.literal("§7诅咒释放成功，但附近没有可变换的生物"), false);
        }
        
        // 始终返回true，表示法术成功释放
        return true;
    }

    private boolean castEarthquake(World world, PlayerEntity player) {
        // Get target position for earthquake epicenter
        Vec3d start = player.getEyePos();
        Vec3d direction = player.getRotationVector();
        Vec3d end = start.add(direction.multiply(50.0));
        
        BlockHitResult hitResult = world.raycast(new RaycastContext(start, end, 
            RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, player));
        
        BlockPos epicenter;
        if (hitResult.getType() == HitResult.Type.MISS) {
            // Use a position 15 blocks in front of player if raycast misses
            epicenter = player.getBlockPos().add(
                (int)(direction.x * 15), 0, (int)(direction.z * 15));
        } else {
            epicenter = hitResult.getBlockPos();
        }
        
        // 启动持续性地震系统
        String earthquakeId = "earthquake_" + player.getUuid().toString() + "_" + System.currentTimeMillis();
        com.example.mythic_age.util.EarthquakeManager.getInstance().startEarthquake(world, epicenter, earthquakeId, player.getYaw());
        
        // 初始启动视觉效果
        ServerWorld serverWorld = (ServerWorld) world;
        
        // 震中强烈启动效果
        serverWorld.spawnParticles(ParticleTypes.EXPLOSION_EMITTER,
            epicenter.getX() + 0.5, epicenter.getY() + 1, epicenter.getZ() + 0.5,
            3, 0, 0, 0, 0);
        
        // 地震波扩散效果
        for (int ring = 1; ring <= 3; ring++) {
            for (int i = 0; i < ring * 8; i++) {
                double angle = (i / (ring * 8.0)) * 2 * Math.PI;
                double radius = ring * 3.0;
                double x = epicenter.getX() + Math.cos(angle) * radius;
                double z = epicenter.getZ() + Math.sin(angle) * radius;
                
                serverWorld.spawnParticles(ParticleTypes.CLOUD,
                    x, epicenter.getY() + 1, z, 3, 0.5, 0.5, 0.5, 0.1);
            }
        }
        
        // 地震启动音效
        world.playSound(null, epicenter, SoundEvents.ENTITY_WITHER_SPAWN, 
            SoundCategory.BLOCKS, 2.0f, 0.3f);
        world.playSound(null, epicenter, SoundEvents.ENTITY_GENERIC_EXPLODE, 
            SoundCategory.BLOCKS, 1.5f, 0.2f);
        
        // 通知玩家
        player.sendMessage(net.minecraft.text.Text.literal("§c地震已启动！将持续30秒，面前30x100范围内的方块将被摧毁！"), false);
        
        return true;
    }

    private boolean castCeasefire(World world, PlayerEntity player) {
        ServerWorld serverWorld = (ServerWorld) world;
        
        // Apply pacifying effect to all hostile mobs in large area
        world.getEntitiesByClass(HostileEntity.class,
            player.getBoundingBox().expand(50.0),
            mob -> true)
            .forEach(mob -> {
                mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 1200, 3)); // 60 seconds
                mob.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 1200, 2));
                mob.setTarget(null); // Clear targets
            });
        
        // Visual effects - peaceful particles around player
        serverWorld.spawnParticles(ParticleTypes.HEART, 
            player.getX(), player.getY() + 2, player.getZ(),
            30, 3.0, 1.0, 3.0, 0.1);
        
        world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_BELL_USE, 
            SoundCategory.NEUTRAL, 1.0f, 1.0f);
        
        return true;
    }

    private boolean castBronze(World world, PlayerEntity player) {
        // Apply bronze armor effect to player and nearby allies
        StatusEffectInstance resistance = new StatusEffectInstance(StatusEffects.RESISTANCE, 1800, 1); // 90 seconds
        StatusEffectInstance strength = new StatusEffectInstance(StatusEffects.STRENGTH, 1800, 0);
        
        player.addStatusEffect(resistance);
        player.addStatusEffect(strength);
        
        // Apply to nearby players
        world.getEntitiesByClass(PlayerEntity.class,
            player.getBoundingBox().expand(15.0),
            p -> p != player)
            .forEach(p -> {
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 1800, 1));
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 1800, 0));
            });
        
        // Visual effects - golden particles
        ServerWorld serverWorld = (ServerWorld) world;
        serverWorld.spawnParticles(ParticleTypes.CRIT, 
            player.getX(), player.getY() + 1, player.getZ(),
            25, 1.0, 1.0, 1.0, 0.2);
        
        world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_ANVIL_USE, 
            SoundCategory.PLAYERS, 0.8f, 1.2f);
        
        return true;
    }

    private boolean castUnderworldPassage(World world, PlayerEntity player) {
        // This is a simplified version - in full implementation, you'd want to store portal pairs
        // For now, just create visual effect and teleport player short distance
        
        Vec3d start = player.getEyePos();
        Vec3d direction = player.getRotationVector();
        Vec3d end = start.add(direction.multiply(20.0));
        
        BlockHitResult hitResult = world.raycast(new RaycastContext(start, end, 
            RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, player));
        
        BlockPos targetPos;
        if (hitResult.getType() == HitResult.Type.MISS) {
            // Use a position 8 blocks in front of player if raycast misses
            targetPos = player.getBlockPos().add(
                (int)(direction.x * 8), 0, (int)(direction.z * 8));
        } else {
            targetPos = hitResult.getBlockPos();
        }
        
        // Find safe teleport location
        while (targetPos.getY() > world.getBottomY() && 
               !world.getBlockState(targetPos.down()).isSolidBlock(world, targetPos.down())) {
            targetPos = targetPos.down();
        }
        
        // Teleport player
        player.teleport(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5);
        
        // Visual effects at both locations
        ServerWorld serverWorld = (ServerWorld) world;
        serverWorld.spawnParticles(ParticleTypes.PORTAL, 
            player.prevX, player.prevY + 1, player.prevZ,
            30, 1.0, 1.0, 1.0, 0.5);
        
        serverWorld.spawnParticles(ParticleTypes.PORTAL, 
            targetPos.getX() + 0.5, targetPos.getY() + 1, targetPos.getZ() + 0.5,
            30, 1.0, 1.0, 1.0, 0.5);
        
        world.playSound(null, targetPos, SoundEvents.BLOCK_PORTAL_TRAVEL, 
            SoundCategory.PLAYERS, 1.0f, 1.0f);
        
        return true;
    }

    private boolean castLightningStormHera(World world, PlayerEntity player) {
        // More powerful version of Zeus's lightning storm
        Vec3d start = player.getEyePos();
        Vec3d direction = player.getRotationVector();
        Vec3d end = start.add(direction.multiply(60.0)); // Longer range
        
        BlockHitResult hitResult = world.raycast(new RaycastContext(start, end, 
            RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, player));
        
        BlockPos targetPos;
        if (hitResult.getType() == HitResult.Type.MISS) {
            // Use a position 12 blocks in front of player if raycast misses
            targetPos = player.getBlockPos().add(
                (int)(direction.x * 12), 0, (int)(direction.z * 12));
        } else {
            targetPos = hitResult.getBlockPos();
        }
        
        ServerWorld serverWorld = (ServerWorld) world;
        
        // Create more intense lightning strikes in a 7x7 area
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                if (world.random.nextFloat() < 0.8f) { // 80% chance for each position
                    BlockPos strikePos = targetPos.add(x, 0, z);
                    
                    // Find the highest solid block
                    while (strikePos.getY() < world.getTopY() && 
                           !world.getBlockState(strikePos).isSolidBlock(world, strikePos)) {
                        strikePos = strikePos.up();
                    }
                    
                    // Create lightning entity
                    LightningEntity lightning = new LightningEntity(net.minecraft.entity.EntityType.LIGHTNING_BOLT, world);
                    lightning.refreshPositionAfterTeleport(strikePos.getX() + 0.5, strikePos.getY(), strikePos.getZ() + 0.5);
                    world.spawnEntity(lightning);
                    
                    // More intense particles
                    serverWorld.spawnParticles(ParticleTypes.ELECTRIC_SPARK, 
                        strikePos.getX() + 0.5, strikePos.getY() + 1, strikePos.getZ() + 0.5,
                        20, 0.8, 0.8, 0.8, 0.2);
                }
            }
        }
        
        // Enhanced thunder sound
        world.playSound(null, targetPos, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 
            SoundCategory.WEATHER, 2.0f, 0.8f);
        
        return true;
    }

    private boolean castPlenty(World world, PlayerEntity player) {
        // Get player position and facing direction
        Vec3d playerPos = player.getPos();
        Vec3d direction = player.getRotationVector();
        
        // Calculate altar center position - place it 8 blocks in front of player to avoid surrounding them
        // This ensures the 11x11 structure doesn't trap the player inside
        // Generate at player ground level - 1 (so first layer is at feet level - 1)
        BlockPos centerPos = player.getBlockPos().add(
            (int)(direction.x * 8), -1, (int)(direction.z * 8));

        // Check if there's enough space for the altar structure
        if (!canPlaceAltarStructure(world, centerPos)) {
            player.sendMessage(net.minecraft.text.Text.literal("§c此处位置超出世界边界，无法建造丰饶祭坛！"), false);
            return false;
        }
        
        // Build the abundance altar structure
        buildAbundanceAltar(world, centerPos);
        
        // Visual effects - golden particles and beams
        ServerWorld serverWorld = (ServerWorld) world;
        
        // Create dramatic particle effects
        for (int i = 0; i < 50; i++) {
            double offsetX = (world.random.nextDouble() - 0.5) * 10.0;
            double offsetY = world.random.nextDouble() * 8.0;
            double offsetZ = (world.random.nextDouble() - 0.5) * 10.0;
            
            serverWorld.spawnParticles(ParticleTypes.FIREWORK,
                centerPos.getX() + 0.5 + offsetX, 
                centerPos.getY() + 1 + offsetY, 
                centerPos.getZ() + 0.5 + offsetZ,
                1, 0, 0, 0, 0);
        }
        
        // Central golden explosion
        serverWorld.spawnParticles(ParticleTypes.EXPLOSION_EMITTER,
            centerPos.getX() + 0.5, centerPos.getY() + 1, centerPos.getZ() + 0.5,
            1, 0, 0, 0, 0);
        
        // Mystical particles around the structure
        serverWorld.spawnParticles(ParticleTypes.ENCHANT,
            centerPos.getX() + 0.5, centerPos.getY() + 1, centerPos.getZ() + 0.5,
            30, 3.0, 2.0, 3.0, 0.2);
        
        // Epic sound effects
        world.playSound(null, centerPos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 
            SoundCategory.PLAYERS, 1.5f, 0.8f);
        world.playSound(null, centerPos, SoundEvents.BLOCK_BEACON_ACTIVATE, 
            SoundCategory.PLAYERS, 1.0f, 1.2f);
        
        // Notify player of success
        player.sendMessage(net.minecraft.text.Text.literal("§6丰饶祭坛建造完成！它将定期为您生成宝贵的资源！"), false);
        player.sendMessage(Text.literal("§7右键点击祭坛核心查看状态"), false);
        
        return true;
    }
    
    /**
     * 检查是否可以在指定位置放置丰饶祭坛结构 - 强制替换模式
     */
    private boolean canPlaceAltarStructure(World world, BlockPos centerPos) {
        // 只检查基本的世界边界，强制替换任何现有方块
        int minY = world.getBottomY();
        int maxY = world.getTopY();
        
        // 检查建造区域是否在世界边界内
        if (centerPos.getY() < minY || centerPos.getY() + 10 > maxY) {
            return false;
        }
        
        // 检查建造区域是否在世界边界内 (X和Z轴)
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                BlockPos checkPos = centerPos.add(x, 0, z);
                if (!world.isInBuildLimit(checkPos) || !world.isInBuildLimit(checkPos.add(0, 9, 0))) {
                    return false;
                }
            }
        }
        
        return true; // 强制替换模式：总是可以建造
    }
    
    /**
     * 建造丰饶祭坛结构 - 新的10层复杂结构（强制替换现有方块）
     */
    private void buildAbundanceAltar(World world, BlockPos centerPos) {
        // 第一层 (地面层, y=0)
        buildAltarLayer1(world, centerPos);
        
        // 第二层 (y=1) 
        buildAltarLayer2(world, centerPos);
        
        // 第三层 (y=2)
        buildAltarLayer3(world, centerPos);
        
        // 第四层 (y=3)
        buildAltarLayer4(world, centerPos);
        
        // 第五层 (y=4)
        buildAltarLayer5(world, centerPos);
        
        // 第六层 (y=5)
        buildAltarLayer6(world, centerPos);
        
        // 第七层 (y=6) - 包含核心方块
        buildAltarLayer7(world, centerPos);
        
        // 第八层 (y=7)
        buildAltarLayer8(world, centerPos);
        
        // 第九层：屋顶底部结构
        buildAltarLayer9(world, centerPos);
        
        // 第十层：屋顶顶部结构
        buildAltarLayer10(world, centerPos);
    }
    
    // 建造各层的辅助方法
    
    /**
     * 第一层：11x11的丰饶祭坛砖框架，内部为普通砖
     * 四个转角: Z类型(上下转角金线), 南北边缘: EW类型(东西向金线), 东西边缘: NS类型(南北向金线), 内部: F类型(不带金线)
     * 俯视角看，形成完整的圆形金线效果
     */
    private void buildAltarLayer1(World world, BlockPos center) {
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                BlockPos pos = center.add(x, 0, z);
                if (Math.abs(x) == 5 && Math.abs(z) == 5) {
                    // 四个转角使用Z类型：上下转角金线砖，临时都使用NORTH进行测试
                    net.minecraft.util.math.Direction facing = net.minecraft.util.math.Direction.NORTH;
                    world.setBlockState(pos, com.example.mythic_age.block.ModBlocks.ABUNDANCE_ALTAR_BRICK_GOLD_CORNER.getDefaultState()
                        .with(net.minecraft.block.HorizontalFacingBlock.FACING, facing));
                } else if (Math.abs(z) == 5 && Math.abs(x) < 5) {
                    // 南北边缘（非转角）使用EW类型：东西向金线砖
                    world.setBlockState(pos, com.example.mythic_age.block.ModBlocks.ABUNDANCE_ALTAR_BRICK_GOLD_EW.getDefaultState());
                } else if (Math.abs(x) == 5 && Math.abs(z) < 5) {
                    // 东西边缘（非转角）使用NS类型：南北向金线砖
                    world.setBlockState(pos, com.example.mythic_age.block.ModBlocks.ABUNDANCE_ALTAR_BRICK_GOLD_NS.getDefaultState());
                } else {
                    // 内部使用F类型：不带金线砖
                    world.setBlockState(pos, com.example.mythic_age.block.ModBlocks.ABUNDANCE_ALTAR_BRICK.getDefaultState());
                }
            }
        }
    }
    
    /**
     * 第二层：柱子和墙壁框架
     * B = 丰饶祭坛柱子, D = 丰饶祭坛墙壁(下方带符文), C = 丰饶祭坛墙壁(不带符文)
     */
    private void buildAltarLayer2(World world, BlockPos center) {
        // 9x9的内部区域
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                BlockPos pos = center.add(x, 1, z);
                if (Math.abs(x) == 4 && Math.abs(z) == 4) {
                    // 四个角落放B类型：柱子
                    world.setBlockState(pos, com.example.mythic_age.block.ModBlocks.ABUNDANCE_ALTAR_PILLAR.getDefaultState());
                } else if (Math.abs(x) == 4 || Math.abs(z) == 4) {
                    // 边缘放D类型：带下符文墙壁
                    world.setBlockState(pos, com.example.mythic_age.block.ModBlocks.ABUNDANCE_ALTAR_WALL_RUNE_BOTTOM.getDefaultState());
                } else {
                    // 内部放C类型：普通墙壁
                    world.setBlockState(pos, com.example.mythic_age.block.ModBlocks.ABUNDANCE_ALTAR_WALL.getDefaultState());
                }
            }
        }
    }
    
    /**
     * 第三层：储藏室和墙壁
     * B = 柱子, H = 储藏室(矿物), I = 储藏室(食物), J = 储藏室(木材), C = 墙壁
     */
    private void buildAltarLayer3(World world, BlockPos center) {
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                BlockPos pos = center.add(x, 2, z);
                if (Math.abs(x) == 4 && Math.abs(z) == 4) {
                    // 四个角落放B类型：柱子
                    world.setBlockState(pos, com.example.mythic_age.block.ModBlocks.ABUNDANCE_ALTAR_PILLAR.getDefaultState());
                } else if ((x == -3 || x == 3) && (z == -4 || z == 4)) {
                    // 北南两边的侧面位置
                    if (x == -3) {
                        world.setBlockState(pos, com.example.mythic_age.block.ModBlocks.ABUNDANCE_STORAGE_MINERAL.getDefaultState()); // H
                    } else {
                        world.setBlockState(pos, com.example.mythic_age.block.ModBlocks.ABUNDANCE_STORAGE_WOOD.getDefaultState()); // J
                    }
                } else if (x == 0 && (z == -4 || z == 4)) {
                    // 北南中央位置放I类型：食物储藏室
                    world.setBlockState(pos, com.example.mythic_age.block.ModBlocks.ABUNDANCE_STORAGE_FOOD.getDefaultState());
                } else if ((x == -4 || x == 4) && z == 0) {
                    // 东西中央位置放I类型：食物储藏室
                    world.setBlockState(pos, com.example.mythic_age.block.ModBlocks.ABUNDANCE_STORAGE_FOOD.getDefaultState());
                } else if ((x == -4 || x == 4) && (z == -3 || z == 3)) {
                    // 东西侧面位置放H类型：矿物储藏室
                    world.setBlockState(pos, com.example.mythic_age.block.ModBlocks.ABUNDANCE_STORAGE_MINERAL.getDefaultState());
                } else {
                    // 其他位置放C类型：普通墙壁
                    world.setBlockState(pos, com.example.mythic_age.block.ModBlocks.ABUNDANCE_ALTAR_WALL.getDefaultState());
                }
            }
        }
    }
    
    /**
     * 第四层：符文墙壁和柱子
     * B = 柱子, K = 丰饶祭坛墙壁(上方带符文), C = 普通墙壁
     */
    private void buildAltarLayer4(World world, BlockPos center) {
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                BlockPos pos = center.add(x, 3, z);
                if (Math.abs(x) == 4 && Math.abs(z) == 4) {
                    // 四个角落放B类型：柱子
                    world.setBlockState(pos, com.example.mythic_age.block.ModBlocks.ABUNDANCE_ALTAR_PILLAR.getDefaultState());
                } else if (Math.abs(x) == 4 || Math.abs(z) == 4) {
                    // 边缘放K类型：带上符文墙壁
                    world.setBlockState(pos, com.example.mythic_age.block.ModBlocks.ABUNDANCE_ALTAR_WALL_RUNE_TOP.getDefaultState());
                } else {
                    // 内部放C类型：普通墙壁
                    world.setBlockState(pos, com.example.mythic_age.block.ModBlocks.ABUNDANCE_ALTAR_WALL.getDefaultState());
                }
            }
        }
    }
    
    /**
     * 第五层：9x9的前后左右带金线砖
     * E = 丰饶祭坛砖(前后左右带金线)
     * 俯视角看是空白，从四个侧面看构成圆形金线效果
     */
    private void buildAltarLayer5(World world, BlockPos center) {
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                BlockPos pos = center.add(x, 4, z);
                // 全部使用E类型：前后左右带金线砖
                world.setBlockState(pos, com.example.mythic_age.block.ModBlocks.ABUNDANCE_ALTAR_BRICK_GOLD_V.getDefaultState());
            }
        }
    }
    
    /**
     * 第六层：中央5x5区域
     * B = 柱子, D = 墙壁(下符文), C = 普通墙壁
     */
    private void buildAltarLayer6(World world, BlockPos center) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos pos = center.add(x, 5, z);
                if (Math.abs(x) == 2 && Math.abs(z) == 2) {
                    // 四个角落放B类型：柱子
                    world.setBlockState(pos, com.example.mythic_age.block.ModBlocks.ABUNDANCE_ALTAR_PILLAR.getDefaultState());
                } else if (Math.abs(x) == 2 || Math.abs(z) == 2) {
                    // 边缘放D类型：带下符文墙壁
                    world.setBlockState(pos, com.example.mythic_age.block.ModBlocks.ABUNDANCE_ALTAR_WALL_RUNE_BOTTOM.getDefaultState());
                } else {
                    // 内部放C类型：普通墙壁
                    world.setBlockState(pos, com.example.mythic_age.block.ModBlocks.ABUNDANCE_ALTAR_WALL.getDefaultState());
                }
            }
        }
    }
    
    /**
     * 第七层：核心层 - 包含丰饶祭坛核心
     * B = 柱子, C = 普通墙壁, O = 丰饶祭坛核心
     */
    private void buildAltarLayer7(World world, BlockPos center) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos pos = center.add(x, 6, z);
                if (x == 0 && z == 0) {
                    // 中心放置O类型：核心方块
                    world.setBlockState(pos, com.example.mythic_age.block.ModBlocks.ABUNDANCE_ALTAR_CORE.getDefaultState());
                } else if (Math.abs(x) == 2 && Math.abs(z) == 2) {
                    // 四个角落放B类型：柱子
                    world.setBlockState(pos, com.example.mythic_age.block.ModBlocks.ABUNDANCE_ALTAR_PILLAR.getDefaultState());
                } else {
                    // 其他位置放C类型：普通墙壁
                    world.setBlockState(pos, com.example.mythic_age.block.ModBlocks.ABUNDANCE_ALTAR_WALL.getDefaultState());
                }
            }
        }
    }
    
    /**
     * 第八层：符文墙壁和柱子
     * B = 柱子, K = 墙壁(上符文), C = 普通墙壁
     */
    private void buildAltarLayer8(World world, BlockPos center) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos pos = center.add(x, 7, z);
                if (x == 0 && z == 0) {
                    // 中心放C类型：普通墙壁
                    world.setBlockState(pos, com.example.mythic_age.block.ModBlocks.ABUNDANCE_ALTAR_WALL.getDefaultState());
                } else if (Math.abs(x) == 2 && Math.abs(z) == 2) {
                    // 四个角落放B类型：柱子
                    world.setBlockState(pos, com.example.mythic_age.block.ModBlocks.ABUNDANCE_ALTAR_PILLAR.getDefaultState());
                } else if (Math.abs(x) == 2 || Math.abs(z) == 2) {
                    // 边缘放K类型：带上符文墙壁
                    world.setBlockState(pos, com.example.mythic_age.block.ModBlocks.ABUNDANCE_ALTAR_WALL_RUNE_TOP.getDefaultState());
                } else {
                    // 其他位置放C类型：普通墙壁
                    world.setBlockState(pos, com.example.mythic_age.block.ModBlocks.ABUNDANCE_ALTAR_WALL.getDefaultState());
                }
            }
        }
    }
    
    /**
     * 第九层：屋顶底部结构
     * 从玩家面对方向看呈三角形，类似房子屋顶
     * 使用楼梯方块创建倾斜效果
     */
    private void buildAltarLayer9(World world, BlockPos center) {
        // 第九层：屋顶底部，使用楼梯方块形成倾斜结构
        // 假设玩家面对南方，屋顶从南到北倾斜，从东到西延伸
        
        // 南侧屋顶（面向北的楼梯）
        for (int x = -2; x <= 2; x++) {
            BlockPos pos = center.add(x, 8, -1); 
            world.setBlockState(pos, com.example.mythic_age.block.ModBlocks.ABUNDANCE_ALTAR_STAIRS.getDefaultState()
                .with(net.minecraft.block.StairsBlock.FACING, net.minecraft.util.math.Direction.NORTH)
                .with(net.minecraft.block.StairsBlock.HALF, net.minecraft.block.enums.BlockHalf.BOTTOM));
        }
        
        // 北侧屋顶（面向南的楼梯）
        for (int x = -2; x <= 2; x++) {
            BlockPos pos = center.add(x, 8, 1);
            world.setBlockState(pos, com.example.mythic_age.block.ModBlocks.ABUNDANCE_ALTAR_STAIRS.getDefaultState()
                .with(net.minecraft.block.StairsBlock.FACING, net.minecraft.util.math.Direction.SOUTH)
                .with(net.minecraft.block.StairsBlock.HALF, net.minecraft.block.enums.BlockHalf.BOTTOM));
        }
        
        // 中间脊梁（普通方块）
        for (int x = -2; x <= 2; x++) {
            BlockPos pos = center.add(x, 8, 0);
            world.setBlockState(pos, com.example.mythic_age.block.ModBlocks.ABUNDANCE_ALTAR_WALL.getDefaultState());
        }
    }
    
    /**
     * 第十层：屋顶顶部结构
     * 屋顶的顶部，形成三角形屋顶的尖端
     * 只在中间区域建造，形成屋顶脊梁
     */
    private void buildAltarLayer10(World world, BlockPos center) {
        // 第十层：屋顶顶部，只在中间建造脊梁
        // 这一层更小，形成屋顶的顶端
        
        // 中间脊梁（缩小的范围）
        for (int x = -1; x <= 1; x++) {
            BlockPos pos = center.add(x, 9, 0);
            world.setBlockState(pos, com.example.mythic_age.block.ModBlocks.ABUNDANCE_ALTAR_WALL.getDefaultState());
        }
        
        // 可选：在两端添加装饰性半砖
        BlockPos eastEnd = center.add(2, 9, 0);
        BlockPos westEnd = center.add(-2, 9, 0);
        world.setBlockState(eastEnd, com.example.mythic_age.block.ModBlocks.ABUNDANCE_ALTAR_SLAB.getDefaultState()
            .with(net.minecraft.block.SlabBlock.TYPE, net.minecraft.block.enums.SlabType.BOTTOM));
        world.setBlockState(westEnd, com.example.mythic_age.block.ModBlocks.ABUNDANCE_ALTAR_SLAB.getDefaultState()
            .with(net.minecraft.block.SlabBlock.TYPE, net.minecraft.block.enums.SlabType.BOTTOM));
    }

    private boolean castTempleGenerationSpell(World world, PlayerEntity player, BlockPos pos, String templeType) {
        if (world.isClient) {
            return false;
        }

        Direction facing = player.getHorizontalFacing();
        BlockPos horizontalTarget = pos.offset(facing, 12);
        BlockPos targetPos = new BlockPos(
            horizontalTarget.getX(),
            world.getTopY(Heightmap.Type.WORLD_SURFACE, horizontalTarget.getX(), horizontalTarget.getZ()),
            horizontalTarget.getZ()
        );

        switch (templeType.toLowerCase()) {
            case "egyptian" -> {
                com.example.mythic_age.util.StructureGenerator.generateEgyptianTemple(world, targetPos);
                player.sendMessage(Text.literal("埃及神殿已生成！位置: " + targetPos.toShortString()), false);
            }
            case "chinese" -> {
                com.example.mythic_age.util.StructureGenerator.generateChineseTemple(world, targetPos);
                player.sendMessage(Text.literal("中国神殿已生成！位置: " + targetPos.toShortString()), false);
            }
            case "atlantean" -> {
                com.example.mythic_age.util.StructureGenerator.generateAtlanteanTemple(world, targetPos);
                player.sendMessage(Text.literal("亚特兰蒂斯神殿已生成！位置: " + targetPos.toShortString()), false);
            }
            case "nordic" -> {
                com.example.mythic_age.util.StructureGenerator.generateNordicTemple(world, targetPos);
                player.sendMessage(Text.literal("北欧神殿已生成！位置: " + targetPos.toShortString()), false);
            }
            default -> {
                return false;
            }
        }

        return true;
    }

    // ==================== 中国文明神力实现 ====================
    

    

    

    
    // ==================== 北欧文明神力实现 ====================
    

    

    
    /**
     * 巨鹰 - 小神力
     * 召唤巨鹰协助战斗和侦察
     */
    private void castGiantEagle(World world, PlayerEntity player, BlockPos targetPos) {
        if (!world.isClient) {
            ServerWorld serverWorld = (ServerWorld) world;
            // 召唤2-3只鹦鹉作为巨鹰
            int eagleCount = 2 + world.random.nextInt(2);
            
            for (int i = 0; i < eagleCount; i++) {
                BlockPos spawnPos = targetPos.add(
                    world.random.nextInt(7) - 3,
                    3 + world.random.nextInt(3),
                    world.random.nextInt(7) - 3
                );
                
                ParrotEntity eagle = EntityType.PARROT.create(world);
                if (eagle != null) {
                    eagle.refreshPositionAfterTeleport(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                    eagle.setTamed(true);
                    eagle.setOwner(player);
                    eagle.setVariant(ParrotEntity.Variant.BLUE); // 蓝色鹦鹉作为巨鹰
                    world.spawnEntity(eagle);
                    
                    // 给巨鹰增强效果
                    eagle.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 12000, 1)); // 10分钟力量II
                    eagle.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 12000, 2)); // 10分钟速度III
                    eagle.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 12000, 1)); // 10分钟抗性II
                }
            }
            
            // 给玩家飞行相关效果
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 6000, 0)); // 5分钟缓降
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 6000, 2)); // 5分钟跳跃提升III
            
            // 生成羽毛粒子效果
            for (int i = 0; i < 40; i++) {
                double x = targetPos.getX() + (world.random.nextDouble() - 0.5) * 14;
                double y = targetPos.getY() + world.random.nextDouble() * 8 + 2;
                double z = targetPos.getZ() + (world.random.nextDouble() - 0.5) * 14;
                serverWorld.spawnParticles(ParticleTypes.CLOUD, x, y, z, 1, 0, 0, 0, 0.1);
            }
            
            player.sendMessage(Text.literal("§f巨鹰召唤成功！" + eagleCount + "只巨鹰将为您效力！"), false);
        }
    }
    

    

    

    
    /**
     * 行走的森林 - 大神力
     * 让树木活过来协助战斗
     */
    private void castWalkingForest(World world, PlayerEntity player, BlockPos targetPos) {
        if (!world.isClient) {
            ServerWorld serverWorld = (ServerWorld) world;
            // 在周围生成"活树"（使用铁傀儡代表）
            int treeCount = 3 + world.random.nextInt(3);
            
            for (int i = 0; i < treeCount; i++) {
                BlockPos spawnPos = targetPos.add(
                    world.random.nextInt(15) - 7,
                    0,
                    world.random.nextInt(15) - 7
                );
                
                // 确保生成位置安全
                while (!world.getBlockState(spawnPos).isAir() || !world.getBlockState(spawnPos.up()).isAir()) {
                    spawnPos = spawnPos.up();
                }
                
                // 先在该位置生成一棵树
                for (int treeY = 0; treeY < 6; treeY++) {
                    world.setBlockState(spawnPos.up(treeY), Blocks.OAK_LOG.getDefaultState());
                }
                for (int leafX = -2; leafX <= 2; leafX++) {
                    for (int leafZ = -2; leafZ <= 2; leafZ++) {
                        for (int leafY = 4; leafY <= 6; leafY++) {
                            if (Math.abs(leafX) + Math.abs(leafZ) + Math.abs(leafY - 5) <= 3) {
                                world.setBlockState(spawnPos.add(leafX, leafY, leafZ), Blocks.OAK_LEAVES.getDefaultState());
                            }
                        }
                    }
                }
                
                // 然后召唤铁傀儡作为"活树"
                IronGolemEntity treeGuardian = EntityType.IRON_GOLEM.create(world);
                if (treeGuardian != null) {
                    treeGuardian.refreshPositionAfterTeleport(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                    treeGuardian.setPlayerCreated(true);
                    world.spawnEntity(treeGuardian);
                    
                    // 给树守护者特殊效果
                    treeGuardian.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 15000, 2)); // 12.5分钟力量III
                    treeGuardian.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 15000, 2)); // 12.5分钟抗性III
                    treeGuardian.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 15000, 1)); // 12.5分钟再生II
                }
            }
            
            // 在周围额外生成一些普通树木
            for (int i = 0; i < 8; i++) {
                BlockPos treePos = targetPos.add(
                    world.random.nextInt(21) - 10,
                    0,
                    world.random.nextInt(21) - 10
                );
                
                if (world.getBlockState(treePos).isAir()) {
                    // 生成简单的树
                    for (int treeY = 0; treeY < 5; treeY++) {
                        world.setBlockState(treePos.up(treeY), Blocks.OAK_LOG.getDefaultState());
                    }
                    for (int leafX = -1; leafX <= 1; leafX++) {
                        for (int leafZ = -1; leafZ <= 1; leafZ++) {
                            for (int leafY = 3; leafY <= 5; leafY++) {
                                world.setBlockState(treePos.add(leafX, leafY, leafZ), Blocks.OAK_LEAVES.getDefaultState());
                            }
                        }
                    }
                }
            }
            
            // 生成绿色自然粒子效果
            for (int i = 0; i < 80; i++) {
                double x = targetPos.getX() + (world.random.nextDouble() - 0.5) * 30;
                double y = targetPos.getY() + world.random.nextDouble() * 6;
                double z = targetPos.getZ() + (world.random.nextDouble() - 0.5) * 30;
                serverWorld.spawnParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 1, 0, 0.1, 0, 0.1);
            }
            
            player.sendMessage(Text.literal("§2行走的森林召唤成功！" + treeCount + "棵古树苏醒，守护自然！"), false);
        }
    }
    

    

    
    /**
     * 拉神光辉 - 小神力
     * 太阳神拉的光芒，治疗和净化
     */
    private void castRaRadiance(World world, PlayerEntity player, BlockPos targetPos) {
        if (!world.isClient) {
            ServerWorld serverWorld = (ServerWorld) world;
            // 治疗玩家和附近盟友
            List<LivingEntity> allies = world.getEntitiesByClass(LivingEntity.class,
                new Box(targetPos).expand(10),
                entity -> entity.isAlive() && !(entity instanceof HostileEntity));
            
            for (LivingEntity ally : allies) {
                ally.heal(8.0f);
                // 移除负面状态效果
                ally.removeStatusEffect(StatusEffects.POISON);
                ally.removeStatusEffect(StatusEffects.WITHER);
                ally.removeStatusEffect(StatusEffects.WEAKNESS);
                ally.removeStatusEffect(StatusEffects.SLOWNESS);
                
                // 给予太阳祝福
                ally.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 3000, 1)); // 2.5分钟再生II
                ally.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 6000, 0)); // 5分钟火焰保护
            }
            
            // 驱散附近的亡灵生物
            List<LivingEntity> undead = world.getEntitiesByClass(LivingEntity.class,
                new Box(targetPos).expand(15),
                entity -> entity.isUndead() && entity.isAlive());
            
            for (LivingEntity undeadEntity : undead) {
                undeadEntity.damage(world.getDamageSources().magic(), 12.0f);
                undeadEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 6000, 2)); // 5分钟虚弱III
            }
            
            // 在周围放置光源
            for (int i = 0; i < 8; i++) {
                BlockPos lightPos = targetPos.add(
                    world.random.nextInt(11) - 5,
                    world.random.nextInt(3),
                    world.random.nextInt(11) - 5
                );
                
                if (world.getBlockState(lightPos).isAir()) {
                    world.setBlockState(lightPos, Blocks.GLOWSTONE.getDefaultState());
                }
            }
            
            // 生成金色光芒粒子效果
            for (int i = 0; i < 60; i++) {
                double x = targetPos.getX() + (world.random.nextDouble() - 0.5) * 20;
                double y = targetPos.getY() + world.random.nextDouble() * 8;
                double z = targetPos.getZ() + (world.random.nextDouble() - 0.5) * 20;
                serverWorld.spawnParticles(ParticleTypes.END_ROD, x, y, z, 1, 0, 0.1, 0, 0.1);
            }
            
            player.sendMessage(Text.literal("§6拉神光辉普照！太阳的恩泽驱散黑暗！"), false);
        }
    }
    
    /**
     * 沙漠风暴 - 小神力
     * 召唤沙漠风暴，迷惑敌人
     */
    private void castDesertStorm(World world, PlayerEntity player, BlockPos targetPos) {
        if (!world.isClient) {
            ServerWorld serverWorld = (ServerWorld) world;
            // 在目标区域创建沙尘效果
            for (int x = -8; x <= 8; x++) {
                for (int z = -8; z <= 8; z++) {
                    BlockPos pos = targetPos.add(x, 0, z);
                    
                    // 随机放置沙子
                    if (world.getBlockState(pos).isAir() && world.random.nextFloat() < 0.3f) {
                        world.setBlockState(pos, Blocks.SAND.getDefaultState());
                    }
                }
            }
            
            // 对区域内的敌对生物施加迷惑效果
            List<LivingEntity> enemies = world.getEntitiesByClass(LivingEntity.class,
                new Box(targetPos).expand(8),
                entity -> entity instanceof HostileEntity && entity.isAlive());
            
            for (LivingEntity enemy : enemies) {
                enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 4000, 0)); // 3.3分钟失明
                enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 4000, 1)); // 3.3分钟缓慢II
                enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 4000, 0)); // 3.3分钟虚弱I
                enemy.damage(world.getDamageSources().magic(), 3.0f);
            }
            
            // 给玩家沙漠适应
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 6000, 1)); // 5分钟速度II
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 6000, 0)); // 5分钟火焰保护
            
            // 生成沙尘粒子效果
            for (int i = 0; i < 80; i++) {
                double x = targetPos.getX() + (world.random.nextDouble() - 0.5) * 24;
                double y = targetPos.getY() + world.random.nextDouble() * 6;
                double z = targetPos.getZ() + (world.random.nextDouble() - 0.5) * 24;
                serverWorld.spawnParticles(ParticleTypes.SMOKE, x, y, z, 1, 0, 0.2, 0, 0.1);
            }
            
            player.sendMessage(Text.literal("§e沙漠风暴席卷而来！敌人在沙尘中迷失方向！"), false);
        }
    }
    
    /**
     * 太阳雨 - 小神力
     * 神奇的太阳雨，促进生长
     */
    private void castSunRain(World world, PlayerEntity player, BlockPos targetPos) {
        if (!world.isClient) {
            ServerWorld serverWorld = (ServerWorld) world;
            // 在目标区域促进植物生长
            for (int x = -6; x <= 6; x++) {
                for (int z = -6; z <= 6; z++) {
                    BlockPos pos = targetPos.add(x, 0, z);
                    BlockState groundState = world.getBlockState(pos.down());
                    BlockState currentState = world.getBlockState(pos);
                    
                    // 将泥土转换为耕地
                    if (groundState.isOf(Blocks.DIRT) || groundState.isOf(Blocks.GRASS_BLOCK)) {
                        world.setBlockState(pos.down(), Blocks.FARMLAND.getDefaultState());
                    }
                    
                    // 种植作物
                    if (currentState.isAir() && world.random.nextFloat() < 0.4f) {
                        Block[] crops = {Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS};
                        Block selectedCrop = crops[world.random.nextInt(crops.length)];
                        world.setBlockState(pos, selectedCrop.getDefaultState());
                    }
                    
                    // 催熟现有作物
                    if (currentState.getBlock() instanceof CropBlock) {
                        CropBlock crop = (CropBlock) currentState.getBlock();
                        int maxAge = crop.getMaxAge();
                        world.setBlockState(pos, currentState.with(Properties.AGE_7, maxAge));
                    }
                    
                    // 生成花朵
                    if (currentState.isAir() && world.random.nextFloat() < 0.2f) {
                        Block[] flowers = {Blocks.DANDELION, Blocks.POPPY, Blocks.BLUE_ORCHID, Blocks.ALLIUM};
                        Block selectedFlower = flowers[world.random.nextInt(flowers.length)];
                        world.setBlockState(pos, selectedFlower.getDefaultState());
                    }
                }
            }
            
            // 治疗附近的动物
            List<LivingEntity> animals = world.getEntitiesByClass(LivingEntity.class,
                new Box(targetPos).expand(10),
                entity -> entity instanceof AnimalEntity && entity.isAlive());
            
            for (LivingEntity animal : animals) {
                animal.heal(6.0f);
                animal.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 3000, 0)); // 2.5分钟再生I
                
                // 让动物进入繁殖模式
                if (animal instanceof AnimalEntity) {
                    ((AnimalEntity) animal).setLoveTicks(600); // 30秒繁殖模式
                }
            }
            
            // 生成金色雨滴粒子效果
            for (int i = 0; i < 100; i++) {
                double x = targetPos.getX() + (world.random.nextDouble() - 0.5) * 24;
                double y = targetPos.getY() + world.random.nextDouble() * 10 + 5;
                double z = targetPos.getZ() + (world.random.nextDouble() - 0.5) * 24;
                serverWorld.spawnParticles(ParticleTypes.DRIPPING_HONEY, x, y, z, 1, 0, -0.2, 0, 0.1);
            }
            
            player.sendMessage(Text.literal("§6太阳雨降临！生命在金色雨滴中蓬勃生长！"), false);
        }
    }
    
    /**
     * 繁荣 - 小神力
     * 带来财富和繁荣
     */
    private void castProsperity(World world, PlayerEntity player, BlockPos targetPos) {
        if (!world.isClient) {
            ServerWorld serverWorld = (ServerWorld) world;
            // 在周围生成宝贵资源
            for (int i = 0; i < 12; i++) {
                BlockPos resourcePos = targetPos.add(
                    world.random.nextInt(11) - 5,
                    world.random.nextInt(3) - 1,
                    world.random.nextInt(11) - 5
                );
                
                if (world.getBlockState(resourcePos).isAir() || world.getBlockState(resourcePos).isOf(Blocks.STONE)) {
                    Block[] ores = {Blocks.GOLD_ORE, Blocks.IRON_ORE, Blocks.COAL_ORE, Blocks.LAPIS_ORE, Blocks.REDSTONE_ORE};
                    Block selectedOre = ores[world.random.nextInt(ores.length)];
                    world.setBlockState(resourcePos, selectedOre.getDefaultState());
                }
            }
            
            // 给玩家幸运效果
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 12000, 2)); // 10分钟幸运III
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 12000, 1)); // 10分钟急迫II
            
            // 在玩家背包中添加一些宝贵物品
            ItemStack[] treasures = {
                new ItemStack(Items.GOLD_INGOT, 2 + world.random.nextInt(4)),
                new ItemStack(Items.DIAMOND, 1 + world.random.nextInt(2)),
                new ItemStack(Items.EMERALD, 1 + world.random.nextInt(3)),
                new ItemStack(Items.EXPERIENCE_BOTTLE, 3 + world.random.nextInt(5))
            };
            
            for (ItemStack treasure : treasures) {
                if (!player.getInventory().insertStack(treasure)) {
                    // 如果背包满了，就掉落在地上
                    ItemEntity itemEntity = new ItemEntity(world, player.getX(), player.getY(), player.getZ(), treasure);
                    world.spawnEntity(itemEntity);
                }
            }
            
            // 生成金色财富粒子效果
            for (int i = 0; i < 80; i++) {
                double x = targetPos.getX() + (world.random.nextDouble() - 0.5) * 20;
                double y = targetPos.getY() + world.random.nextDouble() * 6;
                double z = targetPos.getZ() + (world.random.nextDouble() - 0.5) * 20;
                serverWorld.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y, z, 1, 0, 0.1, 0, 0.1);
            }
            
            player.sendMessage(Text.literal("§6繁荣神力降临！财富如潮水般涌来！"), false);
        }
    }
    
    /**
     * 荷鲁斯之眼 - 大神力
     * 天空之神的全视之眼，揭示一切
     */
    private void castEyeOfHorus(World world, PlayerEntity player, BlockPos targetPos) {
        if (!world.isClient) {
            ServerWorld serverWorld = (ServerWorld) world;
            // 给玩家强大的视觉能力
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 12000, 0)); // 10分钟夜视
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 12000, 0)); // 10分钟发光
            
            // 让所有敌对生物发光
            List<LivingEntity> enemies = world.getEntitiesByClass(LivingEntity.class,
                new Box(targetPos).expand(50),
                entity -> entity instanceof HostileEntity && entity.isAlive());
            
            for (LivingEntity enemy : enemies) {
                enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 12000, 0)); // 10分钟发光
                enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 8000, 1)); // 6.7分钟虚弱II
            }
            
            // 在大范围内揭示隐藏的宝藏
            for (int i = 0; i < 20; i++) {
                BlockPos treasurePos = targetPos.add(
                    world.random.nextInt(61) - 30,
                    world.random.nextInt(21) - 10,
                    world.random.nextInt(61) - 30
                );
                
                // 寻找地下的矿物并标记
                for (int y = treasurePos.getY(); y >= treasurePos.getY() - 20 && y >= 0; y--) {
                    BlockPos checkPos = new BlockPos(treasurePos.getX(), y, treasurePos.getZ());
                    BlockState state = world.getBlockState(checkPos);
                    
                    if (state.isOf(Blocks.DIAMOND_ORE) || state.isOf(Blocks.GOLD_ORE) || 
                        state.isOf(Blocks.IRON_ORE) || state.isOf(Blocks.EMERALD_ORE)) {
                        // 在矿物上方放置发光方块作为标记
                        BlockPos markerPos = checkPos.up();
                        while (!world.getBlockState(markerPos).isAir() && markerPos.getY() < world.getTopY()) {
                            markerPos = markerPos.up();
                        }
                        if (world.getBlockState(markerPos).isAir()) {
                            world.setBlockState(markerPos, Blocks.GLOWSTONE.getDefaultState());
                        }
                        break;
                    }
                }
            }
            
            // 召唤守护鹰（使用鹦鹉代表）
            for (int i = 0; i < 3; i++) {
                BlockPos spawnPos = targetPos.add(
                    world.random.nextInt(11) - 5,
                    3 + world.random.nextInt(3),
                    world.random.nextInt(11) - 5
                );
                
                ParrotEntity eagle = EntityType.PARROT.create(world);
                if (eagle != null) {
                    eagle.refreshPositionAfterTeleport(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                    eagle.setVariant(ParrotEntity.Variant.BLUE); // 蓝色鹦鹉代表神鹰
                    world.spawnEntity(eagle);
                    
                    // 给神鹰增强效果
                    eagle.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 12000, 2)); // 10分钟力量III
                    eagle.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 12000, 2)); // 10分钟速度III
                    eagle.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 12000, 1)); // 10分钟抗性II
                }
            }
            
            // 生成金色眼睛粒子效果
            for (int i = 0; i < 100; i++) {
                double x = targetPos.getX() + (world.random.nextDouble() - 0.5) * 40;
                double y = targetPos.getY() + world.random.nextDouble() * 15;
                double z = targetPos.getZ() + (world.random.nextDouble() - 0.5) * 40;
                serverWorld.spawnParticles(ParticleTypes.END_ROD, x, y, z, 1, 0, 0.2, 0, 0.1);
            }
            
            player.sendMessage(Text.literal("§6荷鲁斯之眼开启！天空之神的视线洞察一切！"), false);
        }
    }
    
    /**
     * 阿努比斯审判 - 大神力
     * 死神的审判，惩罚邪恶
     */
    private void castAnubisJudgment(World world, PlayerEntity player, BlockPos targetPos) {
        if (!world.isClient) {
            ServerWorld serverWorld = (ServerWorld) world;
            // 寻找附近的敌对生物进行审判
            List<LivingEntity> enemies = world.getEntitiesByClass(LivingEntity.class,
                new Box(targetPos).expand(20),
                entity -> entity instanceof HostileEntity && entity.isAlive());
            
            for (LivingEntity enemy : enemies) {
                // 根据敌人的生命值决定审判结果
                float healthPercentage = enemy.getHealth() / enemy.getMaxHealth();
                
                if (healthPercentage > 0.7f) {
                    // 生命值高的敌人受到中等惩罚
                    enemy.damage(world.getDamageSources().magic(), 15.0f);
                    enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 8000, 2)); // 6.7分钟缓慢III
                    enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 8000, 2)); // 6.7分钟虚弱III
                } else if (healthPercentage > 0.3f) {
                    // 生命值中等的敌人受到重度惩罚
                    enemy.damage(world.getDamageSources().magic(), 25.0f);
                    enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 6000, 2)); // 5分钟凋零III
                    enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 6000, 0)); // 5分钟失明
                } else {
                    // 生命值低的敌人直接被审判
                    enemy.damage(world.getDamageSources().magic(), enemy.getMaxHealth()); // 直接击杀
                }
                
                // 在敌人位置召唤审判之火
                BlockPos enemyPos = enemy.getBlockPos();
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        BlockPos firePos = enemyPos.add(x, 0, z);
                        if (world.getBlockState(firePos).isAir()) {
                            world.setBlockState(firePos, Blocks.SOUL_FIRE.getDefaultState());
                        }
                    }
                }
            }
            
            // 召唤阿努比斯守卫（使用狼代表）
            for (int i = 0; i < 4; i++) {
                BlockPos spawnPos = targetPos.add(
                    world.random.nextInt(11) - 5,
                    0,
                    world.random.nextInt(11) - 5
                );
                
                // 确保生成位置安全
                while (!world.getBlockState(spawnPos).isAir() || !world.getBlockState(spawnPos.up()).isAir()) {
                    spawnPos = spawnPos.up();
                }
                
                WolfEntity anubisGuard = EntityType.WOLF.create(world);
                if (anubisGuard != null) {
                    anubisGuard.refreshPositionAfterTeleport(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                    anubisGuard.setTamed(true);
                    anubisGuard.setOwner(player);
                    world.spawnEntity(anubisGuard);
                    
                    // 给阿努比斯守卫增强效果
                    anubisGuard.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 15000, 3)); // 12.5分钟力量IV
                    anubisGuard.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 15000, 2)); // 12.5分钟抗性III
                    anubisGuard.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 15000, 2)); // 12.5分钟速度III
                    anubisGuard.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 15000, 0)); // 12.5分钟火焰保护
                }
            }
            
            // 给玩家死神祝福
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 10000, 2)); // 8.3分钟力量III
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 10000, 1)); // 8.3分钟抗性II
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 10000, 0)); // 8.3分钟火焰保护
            
            // 生成紫色审判粒子效果
            for (int i = 0; i < 120; i++) {
                double x = targetPos.getX() + (world.random.nextDouble() - 0.5) * 40;
                double y = targetPos.getY() + world.random.nextDouble() * 8;
                double z = targetPos.getZ() + (world.random.nextDouble() - 0.5) * 40;
                serverWorld.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 1, 0, 0.2, 0, 0.1);
            }
            
            player.sendMessage(Text.literal("§5阿努比斯审判降临！死神的天平称量善恶！"), false);
        }
    }
    
    /**
     * 蝗虫群 - 大神力
     * 召唤蝗虫群，吞噬一切
     */
    private void castLocustSwarm(World world, PlayerEntity player, BlockPos targetPos) {
        if (!world.isClient) {
            ServerWorld serverWorld = (ServerWorld) world;
            // 在大范围内破坏植被
            for (int x = -12; x <= 12; x++) {
                for (int z = -12; z <= 12; z++) {
                    for (int y = -2; y <= 5; y++) {
                        BlockPos pos = targetPos.add(x, y, z);
                        BlockState state = world.getBlockState(pos);
                        
                        // 破坏植物和作物
                        if (state.getBlock() instanceof PlantBlock || 
                            state.getBlock() instanceof CropBlock ||
                            state.isOf(Blocks.GRASS) || state.isOf(Blocks.TALL_GRASS) ||
                            state.isOf(Blocks.FERN) || state.isOf(Blocks.LARGE_FERN) ||
                            state.getBlock().getDefaultState().isIn(BlockTags.FLOWERS) ||
                            state.getBlock().getDefaultState().isIn(BlockTags.LEAVES)) {
                            
                            if (world.random.nextFloat() < 0.7f) {
                                world.setBlockState(pos, Blocks.AIR.getDefaultState());
                            }
                        }
                        
                        // 将草方块变成泥土
                        if (state.isOf(Blocks.GRASS_BLOCK)) {
                            world.setBlockState(pos, Blocks.DIRT.getDefaultState());
                        }
                    }
                }
            }
            
            // 对区域内的生物造成伤害
            List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class,
                new Box(targetPos).expand(12),
                entity -> entity.isAlive() && !(entity instanceof PlayerEntity));
            
            for (LivingEntity entity : entities) {
                if (entity instanceof HostileEntity) {
                    // 对敌对生物造成持续伤害
                    entity.damage(world.getDamageSources().magic(), 8.0f);
                    entity.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 8000, 2)); // 6.7分钟中毒III
                    entity.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 8000, 3)); // 6.7分钟饥饿IV
                } else if (entity instanceof AnimalEntity) {
                    // 对动物造成轻微伤害
                    entity.damage(world.getDamageSources().magic(), 3.0f);
                    entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 4000, 1)); // 3.3分钟缓慢II
                }
            }
            
            // 召唤蝗虫（使用蜜蜂代表）
            for (int i = 0; i < 15; i++) {
                BlockPos spawnPos = targetPos.add(
                    world.random.nextInt(21) - 10,
                    world.random.nextInt(5),
                    world.random.nextInt(21) - 10
                );
                
                BeeEntity locust = EntityType.BEE.create(world);
                if (locust != null) {
                    locust.refreshPositionAfterTeleport(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                    locust.setAngryAt(player.getUuid()); // 让蜜蜂对玩家愤怒
                    locust.setAngerTime(1200); // 设置愤怒时间1分钟
                    world.spawnEntity(locust);
                    
                    // 给蝗虫增强效果
                    locust.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 12000, 2)); // 10分钟速度III
                    locust.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 12000, 1)); // 10分钟力量II
                }
            }
            
            // 给玩家蝗虫免疫
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SATURATION, 8000, 2)); // 6.7分钟饱和III
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 8000, 1)); // 6.7分钟抗性II
            
            // 生成棕色蝗虫粒子效果
            for (int i = 0; i < 150; i++) {
                double x = targetPos.getX() + (world.random.nextDouble() - 0.5) * 48;
                double y = targetPos.getY() + world.random.nextDouble() * 10;
                double z = targetPos.getZ() + (world.random.nextDouble() - 0.5) * 48;
                serverWorld.spawnParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 1, 0, 0.2, 0, 0.1);
            }
            
            player.sendMessage(Text.literal("§2蝗虫群铺天盖地！万物在虫群中凋零！"), false);
        }
    }
    
    /**
     * 龙卷风 - 大神力
     * 召唤持续移动的强大龙卷风，摧毁路径上的一切
     */
    private void castTornado(World world, PlayerEntity player, BlockPos targetPos) {
        if (!world.isClient) {
            ServerWorld serverWorld = (ServerWorld) world;
            
            // 给玩家风神祝福
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 10000, 0)); // 8.3分钟缓降
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 10000, 2)); // 8.3分钟跳跃提升III
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 10000, 2)); // 8.3分钟速度III
            
            // 创建持续移动的龙卷风
            createMovingTornado(serverWorld, targetPos, player);
            
            player.sendMessage(Text.literal("§7龙卷风呼啸而过！风暴之力摧枯拉朽！"), false);
        }
    }
    
    /**
     * 创建持续移动的龙卷风效果
     */
    private void createMovingTornado(ServerWorld world, BlockPos startPos, PlayerEntity caster) {
        // 龙卷风参数
        final int tornadoHeight = 20;
        final int tornadoRadius = 8;
        final int totalDuration = 600; // 30秒持续时间
        final double moveSpeed = 0.2; // 移动速度
        
        // 随机选择初始移动方向
        final double[] moveAngle = {world.random.nextDouble() * 2 * Math.PI};
        final double[] currentPos = {startPos.getX(), startPos.getZ()};
        
        // 创建异步任务来处理龙卷风移动
        Thread tornadoThread = new Thread(() -> {
            for (int tick = 0; tick < totalDuration; tick++) {
                final int currentTick = tick;
                
                // 每20tick（1秒）随机改变方向
                if (currentTick % 20 == 0 && world.random.nextFloat() < 0.4f) {
                    moveAngle[0] = world.random.nextDouble() * 2 * Math.PI;
                }
                
                // 计算新位置
                currentPos[0] += Math.cos(moveAngle[0]) * moveSpeed;
                currentPos[1] += Math.sin(moveAngle[0]) * moveSpeed;
                
                final BlockPos currentCenter = new BlockPos((int)currentPos[0], startPos.getY(), (int)currentPos[1]);
                
                // 在主线程中执行龙卷风效果
                world.getServer().execute(() -> {
                    if (currentTick < totalDuration) {
                        performTornadoEffectAtPosition(world, currentCenter, tornadoHeight, tornadoRadius, currentTick);
                    }
                });
                
                // 控制更新频率
                try {
                    Thread.sleep(50); // 50ms延迟，每秒20次更新
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        
        tornadoThread.setDaemon(true);
        tornadoThread.start();
    }
    
    /**
     * 在指定位置执行龙卷风效果
     */
    private void performTornadoEffectAtPosition(ServerWorld world, BlockPos center, int height, int radius, int currentTick) {
        // 龙卷风核心效果
        for (int y = 0; y < height; y++) {
            double currentRadius = radius * (1.0 - (double)y / height * 0.3); // 上窄下宽
            
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    double distance = Math.sqrt(x * x + z * z);
                    if (distance <= currentRadius) {
                        BlockPos pos = center.add(x, y, z);
                        
                        // 方块破坏逻辑（增强版）
                        if (world.random.nextFloat() < 0.15f) { // 提高破坏概率
                            BlockState state = world.getBlockState(pos);
                            Block block = state.getBlock();
                            
                            // 不破坏基岩、黑曜石、屏障等坚硬方块
                            if (!block.equals(Blocks.BEDROCK) && 
                                !block.equals(Blocks.OBSIDIAN) && 
                                !block.equals(Blocks.BARRIER) &&
                                !block.equals(Blocks.END_PORTAL_FRAME) &&
                                state.getHardness(world, pos) >= 0) {
                                
                                // 有概率掉落物品
                                if (world.random.nextFloat() < 0.6f) {
                                    world.breakBlock(pos, true);
                                } else {
                                    world.setBlockState(pos, Blocks.AIR.getDefaultState());
                                }
                            }
                        }
                        
                        // 粒子效果（增强版）
                        if (world.random.nextFloat() < 0.8f) {
                            double particleX = center.getX() + x + world.random.nextGaussian() * 0.3;
                            double particleY = center.getY() + y + world.random.nextGaussian() * 0.3;
                            double particleZ = center.getZ() + z + world.random.nextGaussian() * 0.3;
                            
                            // 螺旋上升的粒子效果
                            double angle = (currentTick * 0.2 + y * 0.1) % (2 * Math.PI);
                            double spiralX = Math.cos(angle) * distance * 0.1;
                            double spiralZ = Math.sin(angle) * distance * 0.1;
                            
                            world.spawnParticles(ParticleTypes.CLOUD, 
                                particleX + spiralX, particleY, particleZ + spiralZ, 
                                1, 0, 0.1, 0, 0.05);
                            world.spawnParticles(ParticleTypes.SWEEP_ATTACK, 
                                particleX, particleY, particleZ, 
                                1, 0, 0, 0, 0);
                        }
                    }
                }
            }
        }
        
        // 对范围内的生物施加效果
        Box effectBox = new Box(center).expand(radius);
        List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class, effectBox, entity -> true);
        
        for (LivingEntity entity : entities) {
            double distance = entity.getPos().distanceTo(Vec3d.ofCenter(center));
            if (distance <= radius) {
                // 向上拉拽效果
                Vec3d velocity = entity.getVelocity();
                entity.setVelocity(velocity.x * 0.8, Math.max(velocity.y, 0.3), velocity.z * 0.8);
                
                // 对敌对生物造成伤害
                if (entity instanceof HostileEntity) {
                    entity.damage(world.getDamageSources().magic(), 2.0f);
                    // 施加眩晕效果
                    entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 2));
                    entity.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 100, 1));
                }
                
                // 对玩家施加保护效果
                if (entity instanceof PlayerEntity) {
                    entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 100, 0));
                    entity.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 100, 2));
                    entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 100, 1));
                }
            }
        }
        
        // 音效（每10tick播放一次）
        if (currentTick % 10 == 0) {
            world.playSound(null, center, SoundEvents.ENTITY_ENDER_DRAGON_FLAP, 
                SoundCategory.MASTER, 0.8f, 0.8f + world.random.nextFloat() * 0.4f);
        }
    }
    
    /**
     * 伊西斯复活术 - 终极神力
     * 生命女神的终极力量，复活与重生
     */
    private void castIsisResurrection(World world, PlayerEntity player, BlockPos targetPos) {
        if (!world.isClient) {
            // 完全治愈玩家
            player.setHealth(player.getMaxHealth());
            player.getHungerManager().setFoodLevel(20);
            player.getHungerManager().setSaturationLevel(20.0f);
            
            // 清除所有负面状态效果
            player.clearStatusEffects();
            
            // 给予玩家强大的复活祝福
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 24000, 3)); // 20分钟再生IV
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 24000, 2)); // 20分钟抗性III
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 24000, 2)); // 20分钟力量III
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 24000, 1)); // 20分钟速度II
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 24000, 1)); // 20分钟跳跃提升II
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 24000, 0)); // 20分钟水下呼吸
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 24000, 0)); // 20分钟火焰保护
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 24000, 0)); // 20分钟夜视
            
            // 在大范围内复活和治愈所有友好生物
            List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class,
                new Box(targetPos).expand(30),
                entity -> entity.isAlive() && !(entity instanceof HostileEntity));
            
            for (LivingEntity entity : entities) {
                if (!(entity instanceof PlayerEntity)) {
                    // 治愈友好生物
                    entity.setHealth(entity.getMaxHealth());
                    entity.clearStatusEffects();
                    
                    // 给予友好生物祝福
                    entity.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 12000, 2)); // 10分钟再生III
                    entity.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 12000, 1)); // 10分钟抗性II
                    entity.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 12000, 1)); // 10分钟力量II
                    entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 12000, 1)); // 10分钟速度II
                    
                    // 如果是动物，让其进入繁殖模式
                    if (entity instanceof AnimalEntity) {
                        AnimalEntity animal = (AnimalEntity) entity;
                        animal.setLoveTicks(1200); // 1分钟繁殖模式
                    }
                }
            }
            
            // 在区域内创建生命之花园
            for (int x = -15; x <= 15; x++) {
                for (int z = -15; z <= 15; z++) {
                    double distance = Math.sqrt(x * x + z * z);
                    if (distance <= 15 && world.random.nextFloat() < 0.3f) {
                        BlockPos pos = targetPos.add(x, 0, z);
                        
                        // 寻找合适的地面位置
                        while (pos.getY() > 0 && world.getBlockState(pos).isAir()) {
                            pos = pos.down();
                        }
                        pos = pos.up();
                        
                        if (world.getBlockState(pos).isAir() && world.getBlockState(pos.down()).isSolidBlock(world, pos.down())) {
                            // 种植各种花朵和植物
                            Block[] flowers = {
                                Blocks.ROSE_BUSH, Blocks.PEONY, Blocks.LILAC, Blocks.SUNFLOWER,
                                Blocks.POPPY, Blocks.DANDELION, Blocks.BLUE_ORCHID, Blocks.ALLIUM
                            };
                            
                            Block flower = flowers[world.random.nextInt(flowers.length)];
                            if (flower == Blocks.ROSE_BUSH || flower == Blocks.PEONY || 
                                flower == Blocks.LILAC || flower == Blocks.SUNFLOWER) {
                                // 双格高的花
                                if (world.getBlockState(pos.up()).isAir()) {
                                    world.setBlockState(pos, flower.getDefaultState().with(TallPlantBlock.HALF, DoubleBlockHalf.LOWER));
                                    world.setBlockState(pos.up(), flower.getDefaultState().with(TallPlantBlock.HALF, DoubleBlockHalf.UPPER));
                                }
                            } else {
                                // 单格高的花
                                world.setBlockState(pos, flower.getDefaultState());
                            }
                        }
                    }
                }
            }
            
            // 召唤生命守护者（使用村民代表）
            for (int i = 0; i < 5; i++) {
                BlockPos spawnPos = targetPos.add(
                    world.random.nextInt(21) - 10,
                    0,
                    world.random.nextInt(21) - 10
                );
                
                // 确保生成位置安全
                while (!world.getBlockState(spawnPos).isAir() || !world.getBlockState(spawnPos.up()).isAir()) {
                    spawnPos = spawnPos.up();
                }
                
                VillagerEntity lifeGuardian = EntityType.VILLAGER.create(world);
                if (lifeGuardian != null) {
                    lifeGuardian.refreshPositionAfterTeleport(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                    lifeGuardian.setVillagerData(lifeGuardian.getVillagerData().withProfession(VillagerProfession.CLERIC));
                    world.spawnEntity(lifeGuardian);
                    
                    // 给生命守护者增强效果
                    lifeGuardian.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 24000, 2)); // 20分钟再生III
                    lifeGuardian.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 24000, 3)); // 20分钟抗性IV
                    lifeGuardian.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 24000, 0)); // 20分钟火焰保护
                    lifeGuardian.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 24000, 0)); // 20分钟水下呼吸
                }
            }
            
            // 生成复活粒子效果
            ServerWorld serverWorld = (ServerWorld) world;
            for (int i = 0; i < 200; i++) {
                double x = targetPos.getX() + (world.random.nextDouble() - 0.5) * 60;
                double y = targetPos.getY() + world.random.nextDouble() * 20;
                double z = targetPos.getZ() + (world.random.nextDouble() - 0.5) * 60;
                serverWorld.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y, z, 1, 0, 0.3, 0, 0.2);
                serverWorld.spawnParticles(ParticleTypes.HEART, x, y, z, 1, 0, 0.2, 0, 0.1);
            }
            
            player.sendMessage(Text.literal("§d伊西斯复活术！生命女神的恩典降临人间！"), false);
        }
    }
    
    /**
     * 陨石雨 - 终极神力
     * 天空降下毁灭性的陨石雨，带有真实的下落动画和粒子效果
     * 专注于环境破坏和敌人消灭，不给玩家提供增益效果
     * 优化版本：陨石延迟生成，更大范围，更强破坏力
     */
    private void castMeteorRain(World world, PlayerEntity player, BlockPos targetPos) {
        if (!world.isClient) {
            ServerWorld serverWorld = (ServerWorld) world;
            
            ModConfig.meteorDebug("开始施放陨石雨法术:");
            ModConfig.meteorDebug("  施法者: " + player.getName().getString());
            ModConfig.meteorDebug("  目标位置: " + targetPos);
            ModConfig.meteorDebug("  世界: " + world.getClass().getSimpleName());
            
            // 增强陨石雨参数
            int meteorCount = 15; // 增加陨石数量
            int range = 50; // 大幅增大影响范围
            int delayBetweenMeteors = 40; // 每颗陨石间隔2秒(40 ticks)
            
            ModConfig.meteorDebug("  陨石数量: " + meteorCount);
            ModConfig.meteorDebug("  影响范围: " + range + " 格");
            ModConfig.meteorDebug("  生成间隔: " + delayBetweenMeteors + " ticks");
            
            // 使用调度器延迟生成陨石，实现一颗一颗下落的效果
            for (int i = 0; i < meteorCount; i++) {
                final int index = i;
                final int delay = i * delayBetweenMeteors; // 每颗陨石延迟递增
                
                // 预先计算陨石位置
                final BlockPos meteorTarget = targetPos.add(
                    world.random.nextInt(range * 2 + 1) - range,
                    0,
                    world.random.nextInt(range * 2 + 1) - range
                );
                
                // 立即生成预警粒子效果
                final int groundY = world.getTopY(Heightmap.Type.WORLD_SURFACE, meteorTarget.getX(), meteorTarget.getZ());
                for (int j = 0; j < 40; j++) {
                    double x = meteorTarget.getX() + (world.random.nextDouble() - 0.5) * 12;
                    double y = groundY + world.random.nextDouble() * 8;
                    double z = meteorTarget.getZ() + (world.random.nextDouble() - 0.5) * 12;
                    
                    serverWorld.spawnParticles(ParticleTypes.FLAME, x, y, z, 1, 0, 0.3, 0, 0.15);
                    serverWorld.spawnParticles(ParticleTypes.SMOKE, x, y, z, 1, 0, 0.2, 0, 0.1);
                    serverWorld.spawnParticles(ParticleTypes.LAVA, x, y, z, 1, 0, 0.15, 0, 0.05);
                }
                
                // 使用简单的延迟任务来生成陨石
                if (delay == 0) {
                    // 立即生成第一颗陨石
                    createDelayedMeteor(world, player, meteorTarget, index, serverWorld);
                } else {
                    // 延迟生成其他陨石
                    new Thread(() -> {
                        try {
                            Thread.sleep(delay * 50); // 转换为毫秒 (1 tick = 50ms)
                            // 确保在主线程中执行
                            serverWorld.getServer().execute(() -> {
                                createDelayedMeteor(world, player, meteorTarget, index, serverWorld);
                            });
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }).start();
                }
            }
            
            // 仅给玩家基本的火焰保护，避免被自己的法术伤害
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 3600, 0)); // 3分钟火焰保护
            
            // 生成初始天空粒子效果
            for (int i = 0; i < 100; i++) {
                double x = targetPos.getX() + (world.random.nextDouble() - 0.5) * 80;
                double y = targetPos.getY() + 40 + world.random.nextDouble() * 20;
                double z = targetPos.getZ() + (world.random.nextDouble() - 0.5) * 80;
                
                serverWorld.spawnParticles(ParticleTypes.LAVA, x, y, z, 1, 0, -1.0, 0, 0.3);
                serverWorld.spawnParticles(ParticleTypes.FLAME, x, y, z, 1, 0, -0.8, 0, 0.2);
                serverWorld.spawnParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 1, 0, -0.5, 0, 0.1);
            }
            
            // 播放警告音效
            world.playSound(null, targetPos, net.minecraft.sound.SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 
                net.minecraft.sound.SoundCategory.HOSTILE, 1.0f, 0.8f);
            
            player.sendMessage(Text.literal("§c陨石雨从天而降！天神的怒火即将焚烧大地！"), false);
        }
    }
    
    /**
     * 创建延迟生成的陨石
     */
    private void createDelayedMeteor(World world, PlayerEntity player, BlockPos meteorTarget, int index, ServerWorld serverWorld) {
        // 寻找地面高度
        final int currentGroundY = world.getTopY(Heightmap.Type.WORLD_SURFACE, meteorTarget.getX(), meteorTarget.getZ());
        
        // 设置更高的陨石生成高度，确保有足够的下落时间
        double meteorX = meteorTarget.getX() + 0.5;
        double meteorY = currentGroundY + 60 + (index * 5); // 在地面上方60-130格生成
        double meteorZ = meteorTarget.getZ() + 0.5;
        
        ModConfig.meteorDebug("  延迟创建陨石 #" + (index+1) + ":");
        ModConfig.meteorDebug("    目标位置: " + meteorTarget);
        ModConfig.meteorDebug("    地面高度: " + currentGroundY);
        ModConfig.meteorDebug("    生成位置: (" + String.format("%.1f", meteorX) + ", " + 
                         String.format("%.1f", meteorY) + ", " + 
                         String.format("%.1f", meteorZ) + ")");
        
        // 创建增强版陨石实体
        MeteorEntity meteor = new MeteorEntity(world, meteorX, meteorY, meteorZ, player);
        // 设置更强的爆炸威力
        meteor.setExplosionPower(8.0f); // 增强爆炸威力
        boolean spawned = world.spawnEntity(meteor);
        
        ModConfig.meteorDebug("    实体生成结果: " + (spawned ? "成功" : "失败"));
        if (spawned) {
            ModConfig.meteorDebug("    实体ID: " + meteor.getId());
            ModConfig.meteorDebug("    实体UUID: " + meteor.getUuid());
            
            // 播放个别陨石的警告音效
            world.playSound(null, meteorTarget, net.minecraft.sound.SoundEvents.ENTITY_WITHER_SHOOT, 
                net.minecraft.sound.SoundCategory.HOSTILE, 1.5f, 0.7f + (index * 0.1f));
        }
        
        // 在陨石生成时增强预警粒子效果
        for (int j = 0; j < 50; j++) {
            double x = meteorTarget.getX() + (world.random.nextDouble() - 0.5) * 15;
            double y = currentGroundY + world.random.nextDouble() * 10;
            double z = meteorTarget.getZ() + (world.random.nextDouble() - 0.5) * 15;
            
            serverWorld.spawnParticles(ParticleTypes.FLAME, x, y, z, 1, 0, 0.4, 0, 0.2);
            serverWorld.spawnParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 1, 0, 0.3, 0, 0.15);
            serverWorld.spawnParticles(ParticleTypes.LAVA, x, y, z, 1, 0, 0.2, 0, 0.1);
        }
    }
}
