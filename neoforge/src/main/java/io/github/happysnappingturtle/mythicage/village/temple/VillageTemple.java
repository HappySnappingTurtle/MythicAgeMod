package io.github.happysnappingturtle.mythicage.village.temple;

import io.github.happysnappingturtle.mythicage.core.village.temple.TempleStanding;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;

import java.util.List;
import java.util.UUID;

/**
 * 一个村子的神庙此刻的样子（GA-13 §2.1，五文明通用）：由各文明的 {@link VillageTemples.Provider} 按村里的建筑算出，
 * 短时间缓存。
 *
 * @param altar           信徒面向祈祷、玩家献供与供奉圣物的那一块祭坛（希腊：卫城 {@code ritual} 锚点；没有神庙时是奠基石 / 钟）
 * @param center          48 格圣物效果的圆心
 * @param level           神庙等级（希腊：卫城远古 1、古典 2、英雄 3；没有神庙 0）
 * @param tierName        等级名（语言键 {@code mythic_age.temple.tier.<name>}）
 * @param tierCoefficient 等级系数 T，未乘完好度
 * @param integrity       完好度 0–1
 * @param slotLimit       同时可祈祷的人数
 * @param prayerSlots     祈祷位（祭坛周围能站的格子，按选位规则排好，最多 slotLimit 个）
 */
public record VillageTemple(UUID village, String civ, GlobalPos altar, BlockPos center, int level, String tierName,
                            double tierCoefficient, double integrity, int slotLimit, List<BlockPos> prayerSlots) {
    public VillageTemple {
        prayerSlots = List.copyOf(prayerSlots);
    }

    /** 系数：{@code T × max(0.3, 完好度)}。 */
    public double coefficient() {
        return TempleStanding.coefficient(tierCoefficient, integrity);
    }

    /** 0 级（露天祭坛）在下雨时减半。 */
    public boolean openAir() {
        return level == 0;
    }
}
