package com.example.mythic_age.block.entity;

import com.example.mythic_age.item.GodPowerCertificateItem;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 神像基座方块实体
 * 
 * 存储各个文明神像基座的神力证明数据，处理村庄系统的神力证明储存和玩家提取。
 */
public class StatueBaseBlockEntity extends BlockEntity {
    
    private final GodPowerCertificateItem.MythologyType mythologyType;
    private int storedCertificates = 0;
    private long lastUpdateTime = 0;
    
    public StatueBaseBlockEntity(BlockPos pos, BlockState state, GodPowerCertificateItem.MythologyType mythologyType) {
        super(getBlockEntityType(mythologyType), pos, state);
        this.mythologyType = mythologyType;
    }
    
    /**
     * 根据文明类型获取对应的 BlockEntityType
     */
    private static BlockEntityType<StatueBaseBlockEntity> getBlockEntityType(GodPowerCertificateItem.MythologyType mythologyType) {
        return switch (mythologyType) {
            case GREEK -> ModBlockEntities.STATUE_GREEK_BASE;
            case EGYPTIAN -> ModBlockEntities.STATUE_EGYPTIAN_BASE;
            case CHINESE -> ModBlockEntities.STATUE_CHINESE_BASE;
            case ATLANTEAN -> ModBlockEntities.STATUE_ATLANTEAN_BASE;
            case NORDIC -> ModBlockEntities.STATUE_NORDIC_BASE;
        };
    }
    
    /**
     * 获取储存的神力证明数量
     */
    public int getStoredCertificates() {
        return storedCertificates;
    }
    
    /**
     * 添加神力证明（村庄系统调用）
     */
    public void addCertificates(int amount) {
        this.storedCertificates += amount;
        this.lastUpdateTime = world != null ? world.getTime() : 0;
        markDirty();
    }
    
    /**
     * 提取所有神力证明（玩家交互）
     */
    public int extractAllCertificates() {
        int amount = this.storedCertificates;
        this.storedCertificates = 0;
        markDirty();
        return amount;
    }
    
    /**
     * 提取指定数量的神力证明
     */
    public int extractCertificates(int requestedAmount) {
        int actualAmount = Math.min(requestedAmount, this.storedCertificates);
        this.storedCertificates -= actualAmount;
        markDirty();
        return actualAmount;
    }
    
    /**
     * 获取文明类型
     */
    public GodPowerCertificateItem.MythologyType getMythologyType() {
        return mythologyType;
    }
    
    /**
     * 获取上次更新时间
     */
    public long getLastUpdateTime() {
        return lastUpdateTime;
    }
    
    /**
     * 方块实体Tick逻辑
     */
    public static void tick(World world, BlockPos pos, BlockState state, StatueBaseBlockEntity blockEntity) {
        // 目前不需要特殊的tick逻辑，预留用于未来扩展
        // 可以在这里添加粒子效果、音效等
        
        if (world.isClient) {
            // 客户端粒子效果（如果有储存的神力证明）
            if (blockEntity.storedCertificates > 0 && world.getTime() % 40 == 0) {
                // 每2秒产生一次粒子效果
                spawnStorageParticles(world, pos, blockEntity.mythologyType);
            }
        }
    }
    
    /**
     * 产生储存指示的粒子效果
     */
    private static void spawnStorageParticles(World world, BlockPos pos, GodPowerCertificateItem.MythologyType mythologyType) {
        if (world.isClient) {
            // 根据文明类型产生不同颜色的粒子
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 1.0;
            double z = pos.getZ() + 0.5;
            
            // 添加微弱的粒子效果来指示有储存的神力证明
            for (int i = 0; i < 2; i++) {
                double offsetX = (world.random.nextDouble() - 0.5) * 0.5;
                double offsetZ = (world.random.nextDouble() - 0.5) * 0.5;
                
                switch (mythologyType) {
                    case GREEK -> world.addParticle(net.minecraft.particle.ParticleTypes.ENCHANT,
                        x + offsetX, y, z + offsetZ, 0, 0.05, 0);
                    case EGYPTIAN -> world.addParticle(net.minecraft.particle.ParticleTypes.FLAME,
                        x + offsetX, y, z + offsetZ, 0, 0.02, 0);
                    case CHINESE -> world.addParticle(net.minecraft.particle.ParticleTypes.DRAGON_BREATH,
                        x + offsetX, y, z + offsetZ, 0, 0.02, 0);
                    case ATLANTEAN -> world.addParticle(net.minecraft.particle.ParticleTypes.BUBBLE_POP,
                        x + offsetX, y, z + offsetZ, 0, 0.02, 0);
                    case NORDIC -> world.addParticle(net.minecraft.particle.ParticleTypes.SNOWFLAKE,
                        x + offsetX, y, z + offsetZ, 0, 0.02, 0);
                }
            }
        }
    }
    
    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putInt("StoredCertificates", storedCertificates);
        nbt.putLong("LastUpdateTime", lastUpdateTime);
        nbt.putString("MythologyType", mythologyType.name());
    }
    
    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.storedCertificates = nbt.getInt("StoredCertificates");
        this.lastUpdateTime = nbt.getLong("LastUpdateTime");
        // mythologyType在构造函数中设置，不需要从NBT读取
    }
}