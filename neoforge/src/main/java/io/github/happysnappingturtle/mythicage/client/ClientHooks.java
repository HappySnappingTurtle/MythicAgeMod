package io.github.happysnappingturtle.mythicage.client;

import io.github.happysnappingturtle.mythicage.client.screen.AltarScreen;
import io.github.happysnappingturtle.mythicage.client.screen.GuardianScreen;
import io.github.happysnappingturtle.mythicage.trade.TradePayloads;
import io.github.happysnappingturtle.mythicage.temple.AltarSurveyPayload;
import net.minecraft.client.Minecraft;

/** 网络处理器调用的客户端入口；只在客户端被加载。 */
public final class ClientHooks {
    private ClientHooks() {
    }

    public static void openAltar(AltarSurveyPayload payload) {
        Minecraft.getInstance().setScreen(new AltarScreen(payload));
    }

    /** 打开或刷新交易界面（T9）。 */
    public static void openTrade(TradePayloads.Open payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof GuardianScreen open) {
            open.update(payload.view());
        } else {
            mc.setScreen(new GuardianScreen(payload.view()));
        }
    }

    public static void tradeResult(TradePayloads.Result payload) {
        if (Minecraft.getInstance().screen instanceof GuardianScreen open) {
            open.accept(payload);
        }
    }

    public static void tradeClose() {
        if (Minecraft.getInstance().screen instanceof GuardianScreen open) {
            open.closeByServer();
        }
    }

    /** 界面关闭时的回调；目前没有需要清理的客户端状态，留作 T9b 的动画联动入口。 */
    public static void clearTradeScreen(GuardianScreen screen) {
    }
}
