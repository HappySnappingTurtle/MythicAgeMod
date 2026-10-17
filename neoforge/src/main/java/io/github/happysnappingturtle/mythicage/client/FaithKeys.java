package io.github.happysnappingturtle.mythicage.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.faith.FaithPayloads;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** 信仰面板快捷键（T11a D12）：默认 J，没有打开其他界面时按下即向服务端请求面板数据。 */
@EventBusSubscriber(modid = MythicAgeIds.MOD_ID, value = Dist.CLIENT)
public final class FaithKeys {
    public static final KeyMapping OPEN = new KeyMapping("key.mythic_age.faith", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J, "key.categories.mythic_age");

    private FaithKeys() {
    }

    @SubscribeEvent
    static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN);
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (OPEN.consumeClick()) {
            if (mc.screen == null && mc.player != null && mc.getConnection() != null) {
                PacketDistributor.sendToServer(new FaithPayloads.Request());
            }
        }
    }
}
