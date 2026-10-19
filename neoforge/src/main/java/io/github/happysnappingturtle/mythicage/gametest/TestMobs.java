package io.github.happysnappingturtle.mythicage.gametest;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * 测试里生成的生物的清理。GameTest 测试结束后不清除结构里的实体，要等那块地方下次放结构时才清；
 * 留下的僵尸、掠夺者等会让后面批次的村民恐慌逃跑（祈祷类测试因此偶发失败），所以批次结束时统一移除。
 */
final class TestMobs {
    static final String TAG = "mythic_age_test";

    private TestMobs() {
    }

    static <T extends Entity> T tag(T e) {
        e.addTag(TAG);
        return e;
    }

    /** 移除带测试标签的实体，以及满足 {@code extra} 的实体（如测试中变出来的诅咒猪）。 */
    static int clear(ServerLevel level, Predicate<Entity> extra) {
        List<Entity> all = new ArrayList<>();
        level.getAllEntities().forEach(e -> {
            if (e.getTags().contains(TAG) || extra.test(e)) {
                all.add(e);
            }
        });
        all.forEach(Entity::discard);
        return all.size();
    }
}
