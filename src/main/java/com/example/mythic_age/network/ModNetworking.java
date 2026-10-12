package com.example.mythic_age.network;

import com.example.mythic_age.MythicAgeMod;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;

/**
 * 神话时代 Mod 网络通信类
 * 
 * 负责处理所有网络通信相关的注册和常量定义。
 * 包括服务端到客户端和客户端到服务端的数据包标识符。
 */
public class ModNetworking {
    
    /**
     * 打开自定义神殿守护者交易界面的数据包标识符
     * 服务端发送到客户端，通知客户端打开交易GUI
     */
    public static final Identifier OPEN_CUSTOM_TEMPLE_GUARDIAN_TRADE_SCREEN = 
        new Identifier(MythicAgeMod.MOD_ID, "open_custom_temple_guardian_trade_screen");

    public static final Identifier EXECUTE_CUSTOM_TEMPLE_GUARDIAN_TRADE =
        new Identifier(MythicAgeMod.MOD_ID, "execute_custom_temple_guardian_trade");

    public static final Identifier OPEN_GOD_POWER_GUIDE_SCREEN =
        new Identifier(MythicAgeMod.MOD_ID, "open_god_power_guide_screen");
    
    /**
     * 注册所有网络数据包处理器
     * 在Mod初始化时调用
     */
    public static void registerNetworkPackets() {
        MythicAgeMod.LOGGER.info("Registering network packets for Mythic Age Mod");

        ServerPlayNetworking.registerGlobalReceiver(EXECUTE_CUSTOM_TEMPLE_GUARDIAN_TRADE,
            (server, player, handler, buf, responseSender) -> {
                int entityId = buf.readInt();
                net.minecraft.item.ItemStack input = buf.readItemStack();
                net.minecraft.item.ItemStack output = buf.readItemStack();

                server.execute(() -> {
                    net.minecraft.entity.Entity entity = player.getWorld().getEntityById(entityId);
                    if (entity instanceof com.example.mythic_age.entity.CustomTempleGuardianEntity guardian
                            && player.squaredDistanceTo(guardian) <= 36.0D) {
                        guardian.attemptTrade(player, input, output);
                    }
                });
            });
    }
}
