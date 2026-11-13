package io.github.happysnappingturtle.mythicage.shrine;

import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * 一种神庙（T10b 设计文档 §3.1；19 号 U1：每个文明都有自己的神庙，圣物放进去才生效）。
 * 通用职责（归属、保护、数量上限、同种间距、登记）由 {@link ShrineService} 负责；
 * 各文明特有的规则通过 {@link Hooks} 接入（例如希腊神庙的扫描体不得相交）。
 *
 * @param id              {@code <文明>:<种类>}，如 {@code greek:altar}；配置 {@code shrines.<文明>.<种类>} 与之对应
 * @param verticalSpacing 同种神庙的间距只在垂直距离小于它时生效
 */
public record ShrineKind(String id, Civilization civ, Supplier<? extends Block> block, int verticalSpacing, Hooks hooks) {
    public ShrineKind {
        if (!id.startsWith(civ.id() + ":")) {
            throw new IllegalArgumentException("神庙种类 " + id + " 不属于文明 " + civ.id());
        }
    }

    /** 文明特有的规则。默认什么都不做。 */
    public interface Hooks {
        Hooks NONE = new Hooks() {
        };

        /** 通用检查（上限、同种间距）通过后的额外检查；返回拒绝原因。 */
        default Optional<Component> placementError(ServerPlayer player, ServerLevel level, BlockPos pos, Direction facing,
                                                   ShrineRegistry registry) {
            return Optional.empty();
        }

        /** 神庙有了主人（玩家放置，或无主检查时发现已绑定）之后。 */
        default void onBound(ServerLevel level, BlockPos pos, Optional<ServerPlayer> placer) {
        }

        /** 没有主人时保留方块（GA-13 的旧祭坛是装饰），不按「只能由玩家放置」移除。 */
        default boolean keepUnowned() {
            return false;
        }

        /** 神庙被移除、登记已注销之后。 */
        default void onRemoved(ServerLevel level, GlobalPos pos) {
        }
    }
}
