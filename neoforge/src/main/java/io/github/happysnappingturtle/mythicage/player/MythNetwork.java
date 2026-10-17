package io.github.happysnappingturtle.mythicage.player;

import io.github.happysnappingturtle.mythicage.trade.TradePayloads;
import io.github.happysnappingturtle.mythicage.trade.TradeService;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** 网络负载注册。版本号变化时，新旧版本客户端与服务端互相拒绝连接。 */
public final class MythNetwork {
    private static final String PROTOCOL_VERSION = "8";

    private MythNetwork() {
    }

    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar(PROTOCOL_VERSION)
                .playToClient(PlayerSyncPayload.TYPE, PlayerSyncPayload.STREAM_CODEC,
                        (payload, context) -> ClientMythData.accept(payload))
                .playToClient(io.github.happysnappingturtle.mythicage.content.greek.temple.AltarSurveyPayload.TYPE,
                        io.github.happysnappingturtle.mythicage.content.greek.temple.AltarSurveyPayload.STREAM_CODEC,
                        (payload, context) -> io.github.happysnappingturtle.mythicage.client.ClientHooks.openAltar(payload))
                // 希腊信徒的状态（T10：橄榄枝饰、跪姿）
                .playToClient(io.github.happysnappingturtle.mythicage.content.greek.prayer.BelieverStatePayload.TYPE,
                        io.github.happysnappingturtle.mythicage.content.greek.prayer.BelieverStatePayload.STREAM_CODEC,
                        (payload, context) -> io.github.happysnappingturtle.mythicage.client.ClientBelievers.accept(payload))
                // 仪式与晋升成功界面（T11a）
                .playToClient(io.github.happysnappingturtle.mythicage.rite.RitePayloads.Result.TYPE,
                        io.github.happysnappingturtle.mythicage.rite.RitePayloads.Result.STREAM_CODEC,
                        (payload, context) -> io.github.happysnappingturtle.mythicage.client.ClientHooks.riteResult(payload))
                .playToClient(io.github.happysnappingturtle.mythicage.rite.AgeUnlockedPayload.TYPE,
                        io.github.happysnappingturtle.mythicage.rite.AgeUnlockedPayload.STREAM_CODEC,
                        (payload, context) -> io.github.happysnappingturtle.mythicage.client.ClientHooks.ageUnlocked(payload))
                .playToServer(io.github.happysnappingturtle.mythicage.rite.RitePayloads.Perform.TYPE,
                        io.github.happysnappingturtle.mythicage.rite.RitePayloads.Perform.STREAM_CODEC,
                        io.github.happysnappingturtle.mythicage.rite.RiteService::handlePerform)
                // 信仰面板（T11a 阶段 ②）
                .playToClient(io.github.happysnappingturtle.mythicage.faith.FaithPayloads.View.TYPE,
                        io.github.happysnappingturtle.mythicage.faith.FaithPayloads.View.STREAM_CODEC,
                        (payload, context) -> io.github.happysnappingturtle.mythicage.client.ClientHooks.faithView(payload))
                .playToServer(io.github.happysnappingturtle.mythicage.faith.FaithPayloads.Request.TYPE,
                        io.github.happysnappingturtle.mythicage.faith.FaithPayloads.Request.STREAM_CODEC,
                        io.github.happysnappingturtle.mythicage.faith.FaithService::handleRequest)
                .playToServer(io.github.happysnappingturtle.mythicage.faith.FaithPayloads.Replay.TYPE,
                        io.github.happysnappingturtle.mythicage.faith.FaithPayloads.Replay.STREAM_CODEC,
                        io.github.happysnappingturtle.mythicage.faith.FaithService::handleReplay)
                // 祭司与神庙兑换（T9；T10b 起按文明）
                .playToClient(TradePayloads.Open.TYPE, TradePayloads.Open.STREAM_CODEC,
                        (payload, context) -> io.github.happysnappingturtle.mythicage.client.ClientHooks.openTrade(payload))
                .playToClient(TradePayloads.Result.TYPE, TradePayloads.Result.STREAM_CODEC,
                        (payload, context) -> io.github.happysnappingturtle.mythicage.client.ClientHooks.tradeResult(payload))
                .playToClient(TradePayloads.Close.TYPE, TradePayloads.Close.STREAM_CODEC,
                        (payload, context) -> io.github.happysnappingturtle.mythicage.client.ClientHooks.tradeClose())
                .playToServer(TradePayloads.Buy.TYPE, TradePayloads.Buy.STREAM_CODEC, TradeService::handleBuy)
                .playToServer(TradePayloads.Blessing.TYPE, TradePayloads.Blessing.STREAM_CODEC, TradeService::handleBlessing)
                .playToServer(TradePayloads.Closed.TYPE, TradePayloads.Closed.STREAM_CODEC, TradeService::handleClosed);
    }
}
