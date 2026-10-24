package io.github.happysnappingturtle.mythicage.village;

import net.minecraft.core.GlobalPos;

/**
 * 村庄锚点（19 号 D12、T18 设计文档 §1）：村庄中心在哪里、靠什么判断村庄还在。
 * <ul>
 *   <li>BELL：原版村庄的聚会钟；钟（聚会点兴趣点）没了就开始计消失天数</li>
 *   <li>CENTER：生成村庄模板的中心方块；规划数据还在就算在，不要求有钟（中华的邑这类没有钟的村庄）</li>
 *   <li>FOUNDATION：以后建村道具的奠基石；在奠基石做出来之前按 BELL 处理</li>
 * </ul>
 * 登记表的键仍是 {@link VillageRegistry.Village#bell()}（存档兼容），种类由村庄来源推出。
 */
public record VillageAnchor(GlobalPos pos, Kind kind) {
    public enum Kind { BELL, CENTER, FOUNDATION }

    public static Kind kindOf(VillageRegistry.Origin origin) {
        return switch (origin) {
            case VANILLA -> Kind.BELL;
            case GENERATED -> Kind.CENTER;
            case FOUNDED -> Kind.FOUNDATION;
        };
    }
}
