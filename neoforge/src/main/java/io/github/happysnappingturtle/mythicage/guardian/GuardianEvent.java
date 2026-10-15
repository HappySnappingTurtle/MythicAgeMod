package io.github.happysnappingturtle.mythicage.guardian;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

import java.util.Optional;

/**
 * 守护者被伤害或杀死（T9 设计文档 §7）。发在 NeoForge 游戏总线上，由善恶值系统（T18）消费；
 * 本任务只发事件、不扣分。只有玩家造成的伤害才发 Harmed。
 */
public abstract class GuardianEvent extends Event {
    private final GreekGuardianEntity guardian;

    protected GuardianEvent(GreekGuardianEntity guardian) {
        this.guardian = guardian;
    }

    public GreekGuardianEntity guardian() {
        return guardian;
    }

    public static final class Harmed extends GuardianEvent {
        private final ServerPlayer player;

        public Harmed(GreekGuardianEntity guardian, ServerPlayer player) {
            super(guardian);
            this.player = player;
        }

        public ServerPlayer player() {
            return player;
        }
    }

    /** killer 为空表示不是玩家造成的死亡（摔落、怪物等）。 */
    public static final class Killed extends GuardianEvent {
        private final Optional<ServerPlayer> killer;

        public Killed(GreekGuardianEntity guardian, Optional<ServerPlayer> killer) {
            super(guardian);
            this.killer = killer;
        }

        public Optional<ServerPlayer> killer() {
            return killer;
        }
    }
}
