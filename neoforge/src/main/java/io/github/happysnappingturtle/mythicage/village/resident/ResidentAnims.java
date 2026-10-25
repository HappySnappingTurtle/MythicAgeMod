package io.github.happysnappingturtle.mythicage.village.resident;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.loading.object.BakedAnimations;

/**
 * 工作单里的动作名 → 模型里实际有的动画（T19 设计文档 §12）。职业动作分批制作，还没做的动作先用通用动作代替，
 * 避免 GeckoLib 找不到动画时整个控制器停住：闲聊类 → {@code idle}，其余干活类 → {@code build_hammer}。
 * 只在客户端的动画控制器里调用。
 */
public final class ResidentAnims {
    public static final ResourceLocation FILE = ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID,
            "animations/entity/resident.animation.json");

    private ResidentAnims() {
    }

    public static String resolve(String name) {
        BakedAnimations baked = GeckoLibCache.getBakedAnimations().get(FILE);
        if (baked == null || baked.getAnimation(name) != null) {
            return name;
        }
        return switch (name) {
            case "chat", "greet", "cheer", "browse", "hawk", "listen", "dance", "touch_bow", "perform", "libation_home",
                 "aulos_play", "wash", "grave_tend", "water_fill" -> "idle";
            case "eat", "sit_watch" -> "sit";
            case "run", "torch_run" -> "walk";
            case "water_carry_head", "carry_basket_head" -> "carry_walk";
            case "fish_cast", "fish_reel", "boat_push" -> "haul_pick";
            default -> "build_hammer";
        };
    }

    /**
     * 走路时的动作（T19 §1.10 J9）：按手上的东西与步态换——头顶水罐、头顶祭篮、边走边吹双管笛、举火炬跑；
     * 其余搬着东西是 {@code carry_walk}，空手是 {@code walk}（跑是 {@code run}）。
     */
    public static String moving(String carry, String gait) {
        boolean run = "run".equals(gait);
        return switch (carry) {
            case "hydria" -> "water_carry_head";
            case "kanoun" -> "carry_basket_head";
            case "aulos" -> "aulos_play";
            case "torch" -> run ? "torch_run" : "carry_walk";
            case "" -> run ? "run" : "walk";
            default -> "carry_walk";
        };
    }
}
