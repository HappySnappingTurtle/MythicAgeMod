package io.github.happysnappingturtle.mythicage.entity.anim;

import io.github.happysnappingturtle.mythicage.core.anim.ChargeTimeline;

import java.util.Locale;

/**
 * 服务端写入、所有客户端一致播放的动画状态（T7 设计文档 §6.3）。
 * {@link #AUTO} 表示由客户端按移动速度在待机与行走之间切换；IDLE、WALK 只给开发命令强制定格用。
 */
public enum MythAnimState {
    AUTO(null),
    IDLE("idle"),
    WALK("walk"),
    CHARGE_WINDUP("charge_windup"),
    CHARGE_RUN("charge_run"),
    CHARGE_RECOVER("charge_recover"),
    /** 召唤版弥诺陶洛斯（T14）：挥斧普攻、顶飞前的刨地、顶飞。 */
    ATTACK_SWING("attack_swing"),
    PAW("paw"),
    TOSS("toss");

    private static final MythAnimState[] VALUES = values();
    private final String animation;

    MythAnimState(String animation) {
        this.animation = animation;
    }

    /** 动画文件中的动画名；AUTO 为 {@code null}。 */
    public String animation() {
        return animation;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static MythAnimState byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : AUTO;
    }

    public static MythAnimState byId(String id) {
        for (MythAnimState s : VALUES) {
            if (s.id().equals(id)) {
                return s;
            }
        }
        return null;
    }

    public static MythAnimState of(ChargeTimeline.Phase phase) {
        return switch (phase) {
            case NONE -> AUTO;
            case WINDUP -> CHARGE_WINDUP;
            case RUN -> CHARGE_RUN;
            case RECOVER -> CHARGE_RECOVER;
        };
    }
}
