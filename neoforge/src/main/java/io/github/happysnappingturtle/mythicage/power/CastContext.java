package io.github.happysnappingturtle.mythicage.power;

import io.github.happysnappingturtle.mythicage.core.power.PowerSpec;
import io.github.happysnappingturtle.mythicage.core.power.TargetCategory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * 一次施放的上下文（T12 设计文档 §2.5）。
 *
 * @param target      单体目标（目标入口选实体时）；否则为空
 * @param category    单体目标所在的列
 * @param reaction    单体目标对本神力的反应键
 * @param origin      效果原点：单体目标的位置、选中的点，或施法者自身（以施法者为中心时）
 * @param blockPos    放置型选中的方块与面（目标入口选方块时）
 * @param seed        表现用随机种子（与 castId 一起发给客户端）
 * @param commitTick  提交时刻（世界时钟）
 */
public record CastContext(ServerPlayer caster, ServerLevel level, PowerSpec spec, String castId,
                          @Nullable Entity target, @Nullable TargetCategory category, String reaction, Vec3 origin,
                          @Nullable BlockPos blockPos, @Nullable Direction face, long seed, long commitTick) {
}
