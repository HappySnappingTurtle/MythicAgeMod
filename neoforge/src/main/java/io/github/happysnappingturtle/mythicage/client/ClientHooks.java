package io.github.happysnappingturtle.mythicage.client;

import io.github.happysnappingturtle.mythicage.client.screen.AltarScreen;
import io.github.happysnappingturtle.mythicage.temple.AltarSurveyPayload;
import net.minecraft.client.Minecraft;

/** 网络处理器调用的客户端入口；只在客户端被加载。 */
public final class ClientHooks {
    private ClientHooks() {
    }

    public static void openAltar(AltarSurveyPayload payload) {
        Minecraft.getInstance().setScreen(new AltarScreen(payload));
    }
}
