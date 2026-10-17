package io.github.happysnappingturtle.mythicage.core.power;

/**
 * 一次施放的状态机（T12 设计文档 §2.3）：AIMING →（确认）→ CHANNELING →（满吟唱刻）→ 由服务端提交或取消 → DONE。
 * 只管状态与时刻；打断条件（受伤、切换手持……）由服务端判断后调用 {@link #cancel}。
 */
public final class CastSession {
    public enum State { AIMING, CHANNELING, DONE }

    /** {@link #tick} 的结果。 */
    public enum Event { NONE, CHANNEL_DONE, AIM_TIMEOUT }

    private final String castId;
    private final String powerId;
    private final long aimStart;
    private final int aimTimeout;
    private State state = State.AIMING;
    private long channelStart;
    private int castTicks;
    private PowerReason result;

    private CastSession(String castId, String powerId, long now, int aimTimeout) {
        this.castId = castId;
        this.powerId = powerId;
        this.aimStart = now;
        this.aimTimeout = aimTimeout;
    }

    public static CastSession aim(String castId, String powerId, long now, int aimTimeoutTicks) {
        return new CastSession(castId, powerId, now, aimTimeoutTicks);
    }

    /** 确认目标，开始吟唱；不在瞄准状态时忽略（重复确认）。 */
    public boolean confirm(long now, int castTicks) {
        if (state != State.AIMING) {
            return false;
        }
        state = State.CHANNELING;
        channelStart = now;
        this.castTicks = castTicks;
        return true;
    }

    public Event tick(long now) {
        if (state == State.AIMING && now - aimStart >= aimTimeout) {
            cancel(PowerReason.AIM_TIMEOUT);
            return Event.AIM_TIMEOUT;
        }
        if (state == State.CHANNELING && now - channelStart >= castTicks) {
            return Event.CHANNEL_DONE;
        }
        return Event.NONE;
    }

    public void cancel(PowerReason reason) {
        if (state != State.DONE) {
            state = State.DONE;
            result = reason;
        }
    }

    public void finish() {
        if (state != State.DONE) {
            state = State.DONE;
            result = PowerReason.COMMITTED;
        }
    }

    /** 吟唱进度 0–1（瞄准时为 0）。 */
    public double progress(long now) {
        if (state != State.CHANNELING || castTicks == 0) {
            return state == State.CHANNELING ? 1 : 0;
        }
        return Math.min(1, (now - channelStart) / (double) castTicks);
    }

    public String castId() {
        return castId;
    }

    public String powerId() {
        return powerId;
    }

    public State state() {
        return state;
    }

    public PowerReason result() {
        return result;
    }
}
