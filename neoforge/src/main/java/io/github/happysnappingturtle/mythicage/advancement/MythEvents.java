package io.github.happysnappingturtle.mythicage.advancement;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;

/**
 * 进度事件入口（T11b 设计文档 §5.1）。各系统在事实发生时调用 {@link #fire}；事件 id 见设计文档表：
 * {@code village_entered}、{@code blessed}、{@code favor_gained}、{@code temple_level}、{@code age_reached}。
 */
public final class MythEvents {
    public static final String VILLAGE_ENTERED = "village_entered";
    public static final String BLESSED = "blessed";
    public static final String FAVOR_GAINED = "favor_gained";
    public static final String TEMPLE_LEVEL = "temple_level";
    public static final String AGE_REACHED = "age_reached";

    private static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, MythicAgeIds.MOD_ID);
    public static final DeferredHolder<CriterionTrigger<?>, MythEventTrigger> TRIGGER =
            TRIGGERS.register("myth_event", MythEventTrigger::new);

    private MythEvents() {
    }

    public static void register(IEventBus modBus) {
        TRIGGERS.register(modBus);
    }

    /**
     * 登录时按已有事实补发一次（进度上线前就已祝福、晋升的存档）：各文明的祝福与时代。
     * 神庙等级由各文明自己补发（希腊见 {@code GreekTemple}）。
     */
    public static void onLoggedIn(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        var data = io.github.happysnappingturtle.mythicage.player.MythData.get(p);
        for (Civilization civ : Civilization.values()) {
            var age = data.civ(civ).age();
            if (age.atLeast(io.github.happysnappingturtle.mythicage.core.player.Age.ARCHAIC)) {
                fire(p, BLESSED, civ);
                fire(p, AGE_REACHED, civ, age.ordinal());
            }
        }
    }

    public static void fire(ServerPlayer player, String event, Civilization civ, int value) {
        java.util.function.Consumer<Fired> l = testListener;
        if (l != null) {
            l.accept(new Fired(player.getUUID(), event, civ, value));
        }
        TRIGGER.get().trigger(player, event, Optional.ofNullable(civ), value);
    }

    /** 只供 GameTest：假玩家的进度是空实现，测试改为监听发出的事件。 */
    public record Fired(java.util.UUID player, String event, Civilization civ, int value) {
    }

    public static volatile java.util.function.Consumer<Fired> testListener;

    public static void fire(ServerPlayer player, String event, Civilization civ) {
        fire(player, event, civ, 0);
    }
}
