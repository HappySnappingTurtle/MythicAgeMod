package io.github.happysnappingturtle.mythicage.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * 通用进度触发器 {@code mythic_age:myth_event}（T11b 设计文档 §5.1）：五个文明共用，条件为
 * 事件 id、可选的文明、可选的最小数值（如神庙等级、时代序号）。
 */
public final class MythEventTrigger extends SimpleCriterionTrigger<MythEventTrigger.Instance> {

    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    public void trigger(ServerPlayer player, String event, Optional<Civilization> civ, int value) {
        trigger(player, i -> i.matches(event, civ, value));
    }

    /**
     * @param civilization 文明 id；为空时任何文明都算
     * @param min          数值下限；为空时不看数值
     */
    public record Instance(Optional<ContextAwarePredicate> player, String event, Optional<String> civilization,
                           Optional<Integer> min) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(b -> b.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
                Codec.STRING.fieldOf("event").forGetter(Instance::event),
                Codec.STRING.optionalFieldOf("civilization").forGetter(Instance::civilization),
                Codec.INT.optionalFieldOf("min").forGetter(Instance::min)).apply(b, Instance::new));

        public boolean matches(String e, Optional<Civilization> civ, int value) {
            return event.equals(e)
                    && civilization.map(id -> civ.map(c -> c.id().equals(id)).orElse(false)).orElse(true)
                    && min.map(m -> value >= m).orElse(true);
        }
    }

    public static Criterion<Instance> of(String event, Civilization civ) {
        return MythEvents.TRIGGER.get().createCriterion(new Instance(Optional.empty(), event, Optional.of(civ.id()),
                Optional.empty()));
    }

    public static Criterion<Instance> atLeast(String event, Civilization civ, int min) {
        return MythEvents.TRIGGER.get().createCriterion(new Instance(Optional.empty(), event, Optional.of(civ.id()),
                Optional.of(min)));
    }
}
