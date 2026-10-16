package io.github.happysnappingturtle.mythicage.guardian;

import java.util.Locale;

/** 祭司的一次性手势（T9b 设计文档 §3）：服务端触发，同步给客户端，播完回到待机或行走。各文明的动画名与时长由子类决定。 */
public enum GuardianGesture {
    NONE, GREET, ANNOUNCE, POINT;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    static GuardianGesture byOrdinal(int o) {
        return o >= 0 && o < values().length ? values()[o] : NONE;
    }
}
