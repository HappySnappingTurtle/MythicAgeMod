package io.github.happysnappingturtle.mythicage.agent.remote;

import io.github.happysnappingturtle.mythicage.devtools.remote.RemoteChannel;

/** 一次遥控口会话的共享状态。 */
public final class RemoteContext {
    private final String sessionId;
    private final RemoteChannel channel;
    private int frameSeq;
    private long tick;

    public RemoteContext(String sessionId, RemoteChannel channel) {
        this.sessionId = sessionId;
        this.channel = channel;
    }

    public String sessionId() {
        return sessionId;
    }

    public RemoteChannel channel() {
        return channel;
    }

    public long tick() {
        return tick;
    }

    void setTick(long tick) {
        this.tick = tick;
    }

    int nextFrameSeq() {
        return ++frameSeq;
    }
}
