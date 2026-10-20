package io.github.happysnappingturtle.mythicage.core.village.gen;

/** 门控结构的判断（V0 §3.2、W9）：原版村庄的生成位置交给谁。 */
public final class GateRules {
    public enum Decision {
        /** 开关关闭：一律不生成原版村庄。 */
        SKIP,
        /** 该位置属于已有正式模板的文明地域：让给文明村庄。 */
        YIELD,
        /** 交给原版结构生成。 */
        VANILLA
    }

    private GateRules() {
    }

    public static Decision decide(boolean generateVanillaVillages, boolean regionHasTemplate) {
        if (!generateVanillaVillages) {
            return Decision.SKIP;
        }
        return regionHasTemplate ? Decision.YIELD : Decision.VANILLA;
    }
}
