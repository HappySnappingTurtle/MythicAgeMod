package io.github.happysnappingturtle.mythicage.agent;

/**
 * 遥控口总开关（T2b 设计文档 §1）。只有显式传入系统属性 {@code -Dmythicage.agent=remote} 才启用；
 * Gradle 只在 {@code -Pagent=remote} 时设置该属性。发布包中根本不含 agent 源集。
 */
public final class AgentMode {
    private static final String PROPERTY = "mythicage.agent";

    private AgentMode() {
    }

    public static boolean isRemote() {
        return "remote".equals(System.getProperty(PROPERTY));
    }

    public static String worldName() {
        return System.getProperty("mythicage.remote.world", "ma_autotest");
    }

    public static boolean freshWorld() {
        return Boolean.getBoolean("mythicage.remote.fresh");
    }

    public static boolean realtime() {
        return Boolean.getBoolean("mythicage.remote.realtime");
    }

    public static boolean generateStructures() {
        return Boolean.getBoolean("mythicage.remote.structures");
    }

    public static String language() {
        return System.getProperty("mythicage.remote.lang", "zh_cn");
    }

    public static int idleMinutes() {
        return Integer.getInteger("mythicage.remote.idleMinutes", 30);
    }
}
