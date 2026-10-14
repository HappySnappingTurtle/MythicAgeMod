package io.github.happysnappingturtle.mythicage.gametest;

/**
 * GameTest 共用结构模板名。文件位于 {@code data/mythic_age/structure/}。
 */
public final class GameTestTemplates {
    /** 3×3×3 的空气模板，用于不需要场景方块的测试。 */
    public static final String EMPTY_3X3 = "empty_3x3";
    /** 11×11×11 的空气模板，用于需要较大空间的测试（如树木生长）。 */
    public static final String EMPTY_11X11 = "empty_11x11";

    private GameTestTemplates() {
    }
}
