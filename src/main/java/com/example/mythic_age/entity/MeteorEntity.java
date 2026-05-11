package com.example.mythic_age.entity;

import com.example.mythic_age.sound.ModSounds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.Heightmap;
import com.example.mythic_age.config.ModConfig;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;

import java.util.List;

/**
 * 陨石实体类
 * 实现陨石的下落动画和爆炸效果
 */
public class MeteorEntity extends Entity {
    private int life = 0;
    private int maxLife = 100; // 5秒下落时间
    private float explosionPower = 4.0f;
    private PlayerEntity caster;
    private boolean playedFallingSound = false;
    private boolean playedWarningSound = false;
    
    public MeteorEntity(EntityType<?> type, World world) {
        super(type, world);
        this.setNoGravity(false);
        // 设置实体的碰撞盒大小
        this.setBoundingBox(this.getBoundingBox().expand(0.5, 0.5, 0.5));
        // 确保实体不会被推动或受到其他物理干扰
        this.noClip = false; // 允许碰撞检测
    }
    
    /**
     * 设置陨石的爆炸威力
     * @param power 爆炸威力值
     */
    public void setExplosionPower(float power) {
        this.explosionPower = power;
        ModConfig.meteorDebug("陨石爆炸威力设置为: " + power);
    }
    
    public MeteorEntity(World world, double x, double y, double z, PlayerEntity caster) {
        this(ModEntityTypes.METEOR, world);
        this.setPosition(x, y, z);
        this.caster = caster;
        
        // 设置初始下落速度
        this.setVelocity(0, -0.5, 0); // 增加初始速度
        
        // 详细日志记录
        ModConfig.meteorDebug("陨石实体已创建:");
        ModConfig.meteorDebug("  位置: (" + x + ", " + y + ", " + z + ")");
        ModConfig.meteorDebug("  世界: " + (world.isClient ? "客户端" : "服务端"));
        ModConfig.meteorDebug("  施法者: " + (caster != null ? caster.getName().getString() : "无"));
        ModConfig.meteorDebug("  初始速度: " + this.getVelocity());
        
        // 播放下落音效
        if (!world.isClient) {
            world.playSound(null, x, y, z, ModSounds.ENTITY_METEOR_FALLING, 
                SoundCategory.HOSTILE, 1.0f, 1.0f);
            this.playedFallingSound = true;
            ModConfig.meteorDebug("下落音效已播放");
        }
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (!this.getWorld().isClient) {
            this.life++;
            
            // 每20刻（1秒）输出一次状态日志
            if (this.life % 20 == 0) {
                ModConfig.meteorDebug("陨石状态更新 (第" + (this.life/20) + "秒):");
                ModConfig.meteorDebug("  当前位置: (" + String.format("%.2f", this.getX()) + ", " + 
                                 String.format("%.2f", this.getY()) + ", " + 
                                 String.format("%.2f", this.getZ()) + ")");
                ModConfig.meteorDebug("  当前速度: " + this.getVelocity());
                ModConfig.meteorDebug("  是否在地面: " + this.isOnGround());
                ModConfig.meteorDebug("  生命值: " + this.life + "/" + this.maxLife);
                
                // 计算距离地面的高度
                double groundDistance = this.getY() - this.getWorld().getTopY(Heightmap.Type.WORLD_SURFACE, this.getBlockX(), this.getBlockZ());
                ModConfig.meteorDebug("  距离地面: " + String.format("%.2f", groundDistance) + " 格");
            }
            
            // 优化重力加速度，创造更真实的下落效果
            Vec3d velocity = this.getVelocity();
            double newYVelocity = Math.max(velocity.y - 0.12, -4.0); // 进一步增强的加速度和更高的最终速度
            this.setVelocity(velocity.x, newYVelocity, velocity.z);
            
            // 强制移动实体，确保物理更新
            Vec3d currentVelocity = this.getVelocity();
            this.move(net.minecraft.entity.MovementType.SELF, currentVelocity);
            
            // 如果移动后速度被重置，强制恢复速度
            if (Math.abs(this.getVelocity().y) < 0.01 && Math.abs(currentVelocity.y) > 0.01) {
                ModConfig.meteorDebug("检测到速度被重置，强制恢复速度");
                this.setVelocity(currentVelocity);
                // 强制设置位置
                this.setPosition(this.getX(), this.getY() + currentVelocity.y, this.getZ());
            }
            
            // 添加调试信息，检查移动是否正常
            if (this.life % 10 == 0) { // 每半秒输出一次
                ModConfig.meteorDebug("移动状态检查:");
                ModConfig.meteorDebug("  设置速度: " + this.getVelocity());
                ModConfig.meteorDebug("  实际位置变化: " + (this.getY() - this.prevY));
                ModConfig.meteorDebug("  NoGravity: " + this.hasNoGravity());
                ModConfig.meteorDebug("  NoClip: " + this.noClip);
                ModConfig.meteorDebug("  OnGround: " + this.isOnGround());
            }
            
            // 增强粒子效果，创造明显的火焰尾迹
            if (this.getWorld() instanceof ServerWorld serverWorld) {
                // 根据距离玩家的远近调整粒子数量
                double nearestPlayerDistance = Double.MAX_VALUE;
                for (PlayerEntity player : serverWorld.getPlayers()) {
                    double distance = player.getPos().distanceTo(this.getPos());
                    if (distance < nearestPlayerDistance) {
                        nearestPlayerDistance = distance;
                    }
                }
                
                // 根据距离调整粒子数量
                int particleCount = nearestPlayerDistance > ModConfig.PARTICLE_DISTANCE_THRESHOLD ? 3 : 12;
                
                // 密集的火焰粒子形成尾迹
                for (int i = 0; i < particleCount; i++) {
                    double offsetX = (this.random.nextDouble() - 0.5) * 4.0;
                    double offsetY = (this.random.nextDouble() - 0.5) * 4.0 + 3.0;
                    double offsetZ = (this.random.nextDouble() - 0.5) * 4.0;
                    
                    serverWorld.spawnParticles(ParticleTypes.FLAME, 
                        this.getX() + offsetX, this.getY() + offsetY, this.getZ() + offsetZ,
                        1, 0, 0.3, 0, 0.2);
                    
                    if (i < particleCount / 2) { // 减少熔岩粒子数量
                        serverWorld.spawnParticles(ParticleTypes.LAVA, 
                            this.getX() + offsetX, this.getY() + offsetY, this.getZ() + offsetZ,
                            1, 0, 0.2, 0, 0.15);
                    }
                }
                
                // 增强烟雾尾迹（远距离时减少）
                int smokeCount = nearestPlayerDistance > ModConfig.PARTICLE_DISTANCE_THRESHOLD ? 2 : 8;
                serverWorld.spawnParticles(ParticleTypes.LARGE_SMOKE, 
                    this.getX(), this.getY() + 2, this.getZ(),
                    smokeCount, 1.5, 1.5, 1.5, 0.3);
                    
                // 添加火花粒子增强视觉效果（仅近距离）
                if (nearestPlayerDistance <= ModConfig.PARTICLE_DISTANCE_THRESHOLD && this.life % 2 == 0) {
                    serverWorld.spawnParticles(ParticleTypes.FIREWORK, 
                        this.getX(), this.getY(), this.getZ(),
                        3, 0.8, 0.8, 0.8, 0.15);
                }
                
                // 添加龙息粒子增强神秘感（仅近距离）
                if (nearestPlayerDistance <= ModConfig.PARTICLE_DISTANCE_THRESHOLD && this.life % 4 == 0) {
                    serverWorld.spawnParticles(ParticleTypes.DRAGON_BREATH, 
                        this.getX(), this.getY() + 1, this.getZ(),
                        2, 1.0, 1.0, 1.0, 0.1);
                }
            }
            
            // 距离地面高度检查，播放预警音效
            double groundDistance = this.getY() - this.getWorld().getTopY(Heightmap.Type.WORLD_SURFACE, this.getBlockX(), this.getBlockZ());
            
            // 预警音效和粒子效果
            if (groundDistance < 30 && !this.playedWarningSound) {
                this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(), 
                    ModSounds.ENTITY_METEOR_WARNING, SoundCategory.HOSTILE, 1.5f, 1.2f);
                this.playedWarningSound = true;
                
                // 在目标区域生成预警粒子
                if (this.getWorld() instanceof ServerWorld serverWorld) {
                    BlockPos targetPos = new BlockPos((int)this.getX(), 
                        this.getWorld().getTopY(Heightmap.Type.WORLD_SURFACE, this.getBlockX(), this.getBlockZ()), 
                        (int)this.getZ());
                    
                    for (int i = 0; i < 20; i++) {
                        double offsetX = (this.random.nextDouble() - 0.5) * 8.0;
                        double offsetZ = (this.random.nextDouble() - 0.5) * 8.0;
                        
                        serverWorld.spawnParticles(ParticleTypes.FLAME, 
                            targetPos.getX() + offsetX, targetPos.getY() + 1, targetPos.getZ() + offsetZ,
                            1, 0, 0.5, 0, 0.1);
                    }
                }
            }
            
            // 撞击前持续预警音效
            if (groundDistance < 20 && this.life % 15 == 0) {
                this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(), 
                    SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, SoundCategory.HOSTILE, 0.8f, 2.0f);
            }
            
            // 检查是否撞击地面或达到最大生存时间
            boolean shouldExplode = false;
            String explodeReason = "";
            
            // 更准确的地面碰撞检测
            BlockPos currentPos = this.getBlockPos();
            BlockState blockBelow = this.getWorld().getBlockState(currentPos.down());
            BlockState currentBlock = this.getWorld().getBlockState(currentPos);
            
            // 检查是否撞击到固体方块
            if (!blockBelow.isAir() && blockBelow.isSolidBlock(this.getWorld(), currentPos.down())) {
                shouldExplode = true;
                explodeReason = "撞击固体方块";
            } else if (!currentBlock.isAir() && currentBlock.isSolidBlock(this.getWorld(), currentPos)) {
                shouldExplode = true;
                explodeReason = "进入固体方块";
            } else if (this.isOnGround()) {
                shouldExplode = true;
                explodeReason = "撞击地面";
            } else if (this.life >= maxLife) {
                shouldExplode = true;
                explodeReason = "达到最大生存时间";
            } else if (this.getY() < this.getWorld().getBottomY()) {
                shouldExplode = true;
                explodeReason = "低于世界底部";
            }
            
            // 额外检查：如果陨石距离地面很近且速度很小，强制爆炸
            if (groundDistance <= 1.0 && Math.abs(this.getVelocity().y) < 0.1) {
                shouldExplode = true;
                explodeReason = "接近地面且速度过慢";
            }
            
            if (shouldExplode) {
                ModConfig.meteorDebug("陨石即将爆炸，原因: " + explodeReason);
                ModConfig.meteorDebug("  最终位置: (" + String.format("%.2f", this.getX()) + ", " + 
                                 String.format("%.2f", this.getY()) + ", " + 
                                 String.format("%.2f", this.getZ()) + ")");
                ModConfig.meteorDebug("  最终生命值: " + this.life);
                this.explode();
                this.discard();
            }
        }
    }
    
    private void explode() {
        if (this.getWorld().isClient) {
            ModConfig.meteorDebug("客户端收到爆炸请求，忽略");
            return;
        }
        
        BlockPos impactPos = this.getBlockPos();
        ModConfig.meteorDebug("开始爆炸处理:");
        ModConfig.meteorDebug("  爆炸位置: " + impactPos);
        ModConfig.meteorDebug("  世界类型: " + this.getWorld().getClass().getSimpleName());
        
        // 播放撞击音效
        this.getWorld().playSound(null, impactPos, ModSounds.ENTITY_METEOR_IMPACT, 
            SoundCategory.HOSTILE, 2.5f, 0.8f);
        ModConfig.meteorDebug("撞击音效已播放");
        
        // 额外的爆炸音效增强震撼感
        this.getWorld().playSound(null, impactPos, SoundEvents.ENTITY_GENERIC_EXPLODE, 
            SoundCategory.HOSTILE, 2.0f, 0.6f);
        ModConfig.meteorDebug("爆炸音效已播放");
        
        // 创建更大的陨石坑
        int craterRadius = 5 + this.random.nextInt(3); // 进一步增大陨石坑
        for (int x = -craterRadius; x <= craterRadius; x++) {
            for (int z = -craterRadius; z <= craterRadius; z++) {
                for (int y = -4; y <= 3; y++) { // 增加深度和高度
                    double distance = Math.sqrt(x * x + z * z);
                    if (distance <= craterRadius) {
                        BlockPos pos = impactPos.add(x, y, z);
                        BlockState state = this.getWorld().getBlockState(pos);
                        
                        // 不破坏基岩和黑曜石
                        if (!state.isOf(Blocks.BEDROCK) && !state.isOf(Blocks.OBSIDIAN)) {
                            if (distance <= craterRadius - 2.0) {
                                // 内圈完全破坏
                                if (this.random.nextFloat() < 0.95f) {
                                    this.getWorld().setBlockState(pos, Blocks.AIR.getDefaultState());
                                }
                            } else {
                                // 外圈部分破坏
                                if (this.random.nextFloat() < 0.7f) {
                                    this.getWorld().setBlockState(pos, Blocks.AIR.getDefaultState());
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // 在陨石坑中心放置陨石残骸
        this.getWorld().setBlockState(impactPos, Blocks.OBSIDIAN.getDefaultState());
        if (this.random.nextFloat() < 0.7f) {
            this.getWorld().setBlockState(impactPos.up(), Blocks.MAGMA_BLOCK.getDefaultState());
        }
        
        // 在陨石坑周围放置更多熔岩和岩浆块
        for (int i = 0; i < 5; i++) {
            BlockPos lavaPos = impactPos.add(
                this.random.nextInt(6) - 3, 
                this.random.nextInt(3) - 1, 
                this.random.nextInt(6) - 3
            );
            if (this.getWorld().getBlockState(lavaPos).isAir()) {
                if (this.random.nextFloat() < 0.3f) {
                    this.getWorld().setBlockState(lavaPos, Blocks.MAGMA_BLOCK.getDefaultState());
                }
            }
        }
        
        // 在更大范围内点燃火焰，持续更久
        for (int x = -craterRadius - 3; x <= craterRadius + 3; x++) {
            for (int z = -craterRadius - 3; z <= craterRadius + 3; z++) {
                BlockPos firePos = impactPos.add(x, 1, z);
                if (this.getWorld().getBlockState(firePos).isAir() && this.random.nextFloat() < 0.6f) {
                    this.getWorld().setBlockState(firePos, Blocks.FIRE.getDefaultState());
                }
                // 在地面也可能点燃火焰
                BlockPos groundFirePos = impactPos.add(x, 0, z);
                if (this.getWorld().getBlockState(groundFirePos).isAir() && this.random.nextFloat() < 0.4f) {
                    this.getWorld().setBlockState(groundFirePos, Blocks.FIRE.getDefaultState());
                }
            }
        }
        
        // 对范围内的生物造成伤害
        List<LivingEntity> victims = this.getWorld().getEntitiesByClass(LivingEntity.class,
            new Box(impactPos).expand(craterRadius + 3),
            entity -> entity.isAlive() && !(entity instanceof PlayerEntity && entity == this.caster));
        
        for (LivingEntity victim : victims) {
            double distanceToMeteor = victim.getPos().distanceTo(Vec3d.ofCenter(impactPos));
            if (distanceToMeteor <= craterRadius + 3) {
                float damage = (float) (35.0f - distanceToMeteor * 3.5f); // 增加伤害
                victim.damage(this.getWorld().getDamageSources().magic(), Math.max(damage, 8.0f));
                victim.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 8000, 3)); // 增强效果
                victim.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 10000, 3));
                
                // 更强的击退效果
                Vec3d knockback = victim.getPos().subtract(Vec3d.ofCenter(impactPos)).normalize().multiply(2.5);
                victim.addVelocity(knockback.x, 0.8, knockback.z);
                victim.velocityModified = true;
            }
        }
        
        // 生成更壮观的爆炸粒子效果
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            // 主爆炸粒子
            for (int i = 0; i < 150; i++) {
                double x = impactPos.getX() + (this.random.nextDouble() - 0.5) * 25;
                double y = impactPos.getY() + this.random.nextDouble() * 15;
                double z = impactPos.getZ() + (this.random.nextDouble() - 0.5) * 25;
                
                serverWorld.spawnParticles(ParticleTypes.EXPLOSION, x, y, z, 1, 0, 0, 0, 0);
                serverWorld.spawnParticles(ParticleTypes.LAVA, x, y, z, 1, 0, 0.8, 0, 0.4);
                serverWorld.spawnParticles(ParticleTypes.FLAME, x, y, z, 1, 0, 0.5, 0, 0.3);
            }
            
            // 冲击波粒子效果
            for (int radius = 1; radius <= 8; radius++) {
                for (int angle = 0; angle < 360; angle += 15) {
                    double radians = Math.toRadians(angle);
                    double x = impactPos.getX() + Math.cos(radians) * radius;
                    double z = impactPos.getZ() + Math.sin(radians) * radius;
                    double y = impactPos.getY() + 1;
                    
                    serverWorld.spawnParticles(ParticleTypes.EXPLOSION, x, y, z, 1, 0, 0, 0, 0);
                    serverWorld.spawnParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 1, 0, 0.3, 0, 0.1);
                }
            }
        }
    }
    
    @Override
    protected void initDataTracker() {
        // 初始化数据追踪器
    }
    
    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.life = nbt.getInt("Life");
        this.explosionPower = nbt.getFloat("ExplosionPower");
        this.playedFallingSound = nbt.getBoolean("PlayedFallingSound");
        this.playedWarningSound = nbt.getBoolean("PlayedWarningSound");
    }
    
    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("Life", this.life);
        nbt.putFloat("ExplosionPower", this.explosionPower);
        nbt.putBoolean("PlayedFallingSound", this.playedFallingSound);
        nbt.putBoolean("PlayedWarningSound", this.playedWarningSound);
    }
    
    @Override
    public boolean canHit() {
        return false;
    }
    
    @Override
    public boolean damage(DamageSource source, float amount) {
        return false;
    }
    
    @Override
    public boolean isPushable() {
        return false;
    }
    
    @Override
    public boolean canBeHitByProjectile() {
        return false;
    }
    
    @Override
    public boolean hasNoGravity() {
        return false; // 确保陨石受重力影响
    }
    
    @Override
    public boolean isCollidable() {
        return true; // 允许碰撞检测
    }
    

}