package io.github.happysnappingturtle.mythicage.player;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** 网络负载注册。版本号变化时，新旧版本客户端与服务端互相拒绝连接。 */
public final class MythNetwork {
    private static final String PROTOCOL_VERSION = "2";

    private MythNetwork() {
    }

    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar(PROTOCOL_VERSION)
                .playToClient(PlayerSyncPayload.TYPE, PlayerSyncPayload.STREAM_CODEC,
                        (payload, context) -> ClientMythData.accept(payload))
                .playToClient(io.github.happysnappingturtle.mythicage.temple.AltarSurveyPayload.TYPE,
                        io.github.happysnappingturtle.mythicage.temple.AltarSurveyPayload.STREAM_CODEC,
                        (payload, context) -> io.github.happysnappingturtle.mythicage.client.ClientHooks.openAltar(payload));
    }
}
