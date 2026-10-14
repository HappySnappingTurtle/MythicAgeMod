package io.github.happysnappingturtle.mythicage.agent;

import com.mojang.logging.LogUtils;
import io.github.happysnappingturtle.mythicage.agent.remote.ChatTap;
import io.github.happysnappingturtle.mythicage.agent.remote.RemoteScenario;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * 遥控口的唯一接入点。agent 源集只在 {@code -Pagent=remote} 时并入本 mod，由 NeoForge 扫描到这个订阅类；
 * 玩法代码不引用任何 agent 类。即使类在类路径上，未设置系统属性时也不注册任何监听器。
 */
@EventBusSubscriber(modid = MythicAgeIds.MOD_ID, value = Dist.CLIENT)
public final class AgentEntrypoint {
    public static final Logger LOG = LogUtils.getLogger();

    private AgentEntrypoint() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        if (!AgentMode.isRemote()) {
            return;
        }
        LOG.info("[agent] 遥控口已启用（世界 {}，语言 {}）", AgentMode.worldName(), AgentMode.language());
        AutoWorldBootstrap.configure(AgentMode.worldName(), AgentMode.freshWorld(), AgentMode.generateStructures());
        NeoForge.EVENT_BUS.addListener(AutoWorldBootstrap::onClientTickPost);
        NeoForge.EVENT_BUS.addListener(FrameCapture::onRenderFramePost);
        NeoForge.EVENT_BUS.addListener(ChatTap::onSystem);
        NeoForge.EVENT_BUS.addListener(ChatTap::onPlayer);
        NeoForge.EVENT_BUS.addListener(RemoteScenario::onClientTickPost);
    }
}
