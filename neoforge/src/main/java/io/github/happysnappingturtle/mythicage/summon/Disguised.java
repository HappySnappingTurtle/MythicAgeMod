package io.github.happysnappingturtle.mythicage.summon;

import net.minecraft.server.level.ServerPlayer;

/**
 * 会伪装的生物（吕卡翁、树仙女，T15 实现）：侦察单位的「天眼」扫到时让它对主人现形（T14 设计文档 §6.2、§11.4）。
 */
public interface Disguised {
    /** 对 viewer 现形 ticks 刻。 */
    void reveal(ServerPlayer viewer, int ticks);
}
