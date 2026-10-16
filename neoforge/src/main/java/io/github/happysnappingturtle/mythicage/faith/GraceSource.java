package io.github.happysnappingturtle.mythicage.faith;

import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 一个文明的恩宠来源（T10b 设计文档 §6；总纲 P1：五个文明获取恩宠的方式完全不同）。
 * 只负责「给玩家看」——信仰面板（T11a）列出每个来源的状态；结算仍走 T5 的账本。
 */
public interface GraceSource {
    Civilization civ();

    /** {@code <文明>:<来源>}，如 {@code greek:temple}。 */
    String id();

    /** 该玩家在此来源下的各行（每座神庙、每座纪念碑……）。 */
    List<Row> rows(ServerPlayer viewer);

    /**
     * @param village 关联的村庄（填信仰面板的「你的村庄」）；没有时为空
     * @param perDay  预计日产出（估算，不是发放承诺）
     */
    record Row(Component name, GlobalPos pos, Component status, double perDay, Optional<UUID> village) {
    }
}
