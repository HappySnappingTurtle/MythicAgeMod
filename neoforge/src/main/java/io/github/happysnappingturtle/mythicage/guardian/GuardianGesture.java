package io.github.happysnappingturtle.mythicage.guardian;

import java.util.Locale;
import java.util.Optional;

/**
 * 祭司的手势（T9b 设计文档 §3）：服务端触发，同步给客户端，播完回到待机或行走。各文明的动画名与时长由子类决定。
 * T19 §1.10 J4 加了差事用的三个：奠酒、神谕坐诊、听橡（后两个循环播放）。
 */
public enum GuardianGesture {
    NONE, GREET, ANNOUNCE, POINT, LIBATION_POUR, ORACLE_SIT, LISTEN;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** 循环播放的手势（坐诊、听橡）；其余播一次停在末帧。 */
    public boolean loops() {
        return this == ORACLE_SIT || this == LISTEN;
    }

    public static Optional<GuardianGesture> byId(String id) {
        for (GuardianGesture g : values()) {
            if (g.id().equals(id)) {
                return Optional.of(g);
            }
        }
        return Optional.empty();
    }

    static GuardianGesture byOrdinal(int o) {
        return o >= 0 && o < values().length ? values()[o] : NONE;
    }
}
