package com.example.mythic_age.entity;

import com.example.mythic_age.item.GodPowerCertificateItem;

/**
 * 文明村民接口
 * 
 * 用于访问通过Mixin添加到原版村民的文明属性
 */
public interface CivilizedVillager {
    
    /**
     * 获取村民的文明类型
     */
    GodPowerCertificateItem.MythologyType mythicAge$getCivilization();
    
    /**
     * 设置村民的文明类型
     */
    void mythicAge$setCivilization(GodPowerCertificateItem.MythologyType civilization);

    /**
     * 获取劳作证书冷却剩余 tick
     */
    int mythicAge$getWorkCooldown();

    /**
     * 设置劳作证书冷却
     */
    void mythicAge$setWorkCooldown(int ticks);
}