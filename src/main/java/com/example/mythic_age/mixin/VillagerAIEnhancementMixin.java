package com.example.mythic_age.mixin;

import com.example.mythic_age.entity.CivilizedVillager;
import com.example.mythic_age.entity.ai.goal.VillagerPilgrimageGoal;
import com.example.mythic_age.worldgen.VillageManager;
import com.example.mythic_age.item.GodPowerCertificateItem;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

/**
 * 村民AI增强Mixin
 * 改进版本：基于村庄管理器的文明系统
 * 
 * 为村民添加文明相关的AI行为，如朝圣等。
 * 现在使用新的村庄管理器来确定村民的文明类型。
 */
@Mixin(VillagerEntity.class)
public class VillagerAIEnhancementMixin implements CivilizedVillager {
    
    private GodPowerCertificateItem.MythologyType mythicAge$civilization = null;
    private boolean mythicAge$civilizationInitialized = false;
    private boolean mythicAge$aiInitialized = false;
    private int mythicAge$workCertificateCooldown = 0;
    
    /**
     * 在村民初始化时设置文明类型
     * 改进版本：使用村庄管理器根据位置确定文明类型
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void initializeCivilization(CallbackInfo ci) {
        if (!mythicAge$civilizationInitialized) {
            VillagerEntity villager = (VillagerEntity) (Object) this;
            
            // 使用村庄管理器设置文明类型
            VillageManager.setVillagerCivilizationByLocation(villager);
            
            mythicAge$civilizationInitialized = true;
        }
    }
    
    /**
     * 在村民tick时检查并初始化AI
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        VillagerEntity villager = (VillagerEntity) (Object) this;

        if (!villager.getWorld().isClient && mythicAge$workCertificateCooldown > 0) {
            mythicAge$workCertificateCooldown--;
        }

        // 只在服务器端执行，且文明已初始化但AI未初始化时
        if (!villager.getWorld().isClient &&
            mythicAge$civilizationInitialized &&
            !mythicAge$aiInitialized &&
            mythicAge$civilization != null) {

            addCivilizationAI(villager);
            mythicAge$aiInitialized = true;
        }
    }
    
    /**
     * 根据文明类型添加相应的AI行为
     * 改进版本：添加时间限制和随机性的朝圣行为
     */
    private void addCivilizationAI(VillagerEntity villager) {
        if (mythicAge$civilization == GodPowerCertificateItem.MythologyType.GREEK) {
            // 为希腊村民添加朝圣AI
            try {
                Field goalSelectorField = MobEntity.class.getDeclaredField("goalSelector");
                goalSelectorField.setAccessible(true);
                GoalSelector goalSelector = (GoalSelector) goalSelectorField.get(villager);
                
                // 添加朝圣目标，优先级设为5（在基本需求之后）
                goalSelector.add(5, new VillagerPilgrimageGoal(villager, 0.6));
                
                System.out.println("[村民AI] 为希腊村民添加朝圣AI: " + villager.getBlockPos());
                
            } catch (Exception e) {
                System.err.println("[村民AI] 添加朝圣AI失败: " + e.getMessage());
            }
        }
        // 可以为其他文明添加不同的AI行为
        // 例如：埃及村民的祭祀行为，北欧村民的战斗训练等
    }
    
    @Override
    public GodPowerCertificateItem.MythologyType mythicAge$getCivilization() {
        return mythicAge$civilization;
    }
    
    @Override
    public void mythicAge$setCivilization(GodPowerCertificateItem.MythologyType civilization) {
        this.mythicAge$civilization = civilization;
        this.mythicAge$civilizationInitialized = true;
        this.mythicAge$aiInitialized = false;
    }

    @Override
    public int mythicAge$getWorkCooldown() {
        return mythicAge$workCertificateCooldown;
    }

    @Override
    public void mythicAge$setWorkCooldown(int ticks) {
        this.mythicAge$workCertificateCooldown = ticks;
    }
    
    /**
     * 检查文明是否已初始化（额外方法）
     */
    public boolean isCivilizationInitialized() {
        return mythicAge$civilizationInitialized;
    }
    
    /**
     * 保存NBT数据
     */
    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void writeCustomDataToNbt(NbtCompound nbt, CallbackInfo ci) {
        if (mythicAge$civilization != null) {
            nbt.putString("MythicAge_Civilization", mythicAge$civilization.name());
        }
        nbt.putBoolean("MythicAge_CivilizationInitialized", mythicAge$civilizationInitialized);
        nbt.putBoolean("MythicAge_AIInitialized", mythicAge$aiInitialized);
        nbt.putInt("MythicAge_WorkCertificateCooldown", mythicAge$workCertificateCooldown);
    }
    
    /**
     * 读取NBT数据
     */
    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void readCustomDataFromNbt(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("MythicAge_Civilization")) {
            try {
                mythicAge$civilization = GodPowerCertificateItem.MythologyType.valueOf(
                    nbt.getString("MythicAge_Civilization"));
            } catch (IllegalArgumentException e) {
                mythicAge$civilization = null;
            }
        }
        mythicAge$civilizationInitialized = nbt.getBoolean("MythicAge_CivilizationInitialized");
        mythicAge$aiInitialized = nbt.getBoolean("MythicAge_AIInitialized");
        mythicAge$workCertificateCooldown = nbt.getInt("MythicAge_WorkCertificateCooldown");
    }
}
