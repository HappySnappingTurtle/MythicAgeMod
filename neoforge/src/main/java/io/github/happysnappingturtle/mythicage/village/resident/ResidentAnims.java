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
            case "chat", "greet", "cheer", "browse", "hawk", "listen" -> "idle";
            case "eat", "sit_watch" -> "sit";
            default -> "build_hammer";
        };
    }
}
