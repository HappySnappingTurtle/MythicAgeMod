package io.github.happysnappingturtle.mythicage.client;

import io.github.happysnappingturtle.mythicage.client.screen.GreekAltarScreen;
import io.github.happysnappingturtle.mythicage.client.screen.GuardianScreen;
import io.github.happysnappingturtle.mythicage.content.greek.temple.AltarSurveyPayload;
import io.github.happysnappingturtle.mythicage.trade.TradePayloads;
import net.minecraft.client.Minecraft;

/** 网络处理器调用的客户端入口；只在客户端被加载。 */
public final class ClientHooks {
    private ClientHooks() {
    }

    public static void openAltar(AltarSurveyPayload payload) {
        Minecraft.getInstance().setScreen(new GreekAltarScreen(payload));
    }

    /** 打开或刷新交易界面（T9）。 */
    public static void openTrade(TradePayloads.Open payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof io.github.happysnappingturtle.mythicage.client.screen.AgeUnlockedScreen unlocked
                && unlocked.previous() instanceof GuardianScreen under) {
            under.update(payload.view());
        } else if (mc.screen instanceof GuardianScreen open) {
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

    /** 仪式结果（T11a）：刷新祭司界面并显示原因。 */
    public static void riteResult(io.github.happysnappingturtle.mythicage.rite.RitePayloads.Result payload) {
        // 晋升成功时服务端先发成功界面、再发结果：此时祭司界面在成功界面下面
        net.minecraft.client.gui.screens.Screen s = Minecraft.getInstance().screen;
        if (s instanceof io.github.happysnappingturtle.mythicage.client.screen.AgeUnlockedScreen unlocked) {
            s = unlocked.previous();
        }
        if (s instanceof GuardianScreen open) {
            open.acceptRite(payload);
        }
    }

    /** 晋升成功界面（T11a）：在当前界面之上打开，关闭后回到原界面。 */
    public static void ageUnlocked(io.github.happysnappingturtle.mythicage.rite.AgeUnlockedPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new io.github.happysnappingturtle.mythicage.client.screen.AgeUnlockedScreen(payload, mc.screen));
    }

    /** 信仰面板（T11a）：已开着就刷新，否则在当前界面之上打开，关闭后回到原界面。 */
    public static void faithView(io.github.happysnappingturtle.mythicage.faith.FaithPayloads.View payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof io.github.happysnappingturtle.mythicage.client.screen.FaithScreen open) {
            open.update(payload.view());
        } else {
            mc.setScreen(new io.github.happysnappingturtle.mythicage.client.screen.FaithScreen(payload.view(), mc.screen));
        }
    }

    /** 界面关闭时的回调；目前没有需要清理的客户端状态，留作 T9b 的动画联动入口。 */
    public static void clearTradeScreen(GuardianScreen screen) {
    }
}
