package io.github.happysnappingturtle.mythicage.relic;

import io.github.happysnappingturtle.mythicage.core.relic.DamageRules;
import io.github.happysnappingturtle.mythicage.shrine.ShrineBlockEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingConversionEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import top.theillusivec4.curios.api.event.CurioChangeEvent;

import java.util.List;

/**
 * 圣物的事件挂接（T16 设计文档 §5、§6）：对账时机、近战减伤、防僵尸转化、潜行右键供奉。
 * 效果按事件或每 20 刻刷新（总纲 §3.8），对账按玩家 UUID 错开到 20 刻里的不同刻。
 */
public final class RelicEvents {
    public static final int PERIOD = 20;

    private RelicEvents() {
    }

    public static void register(IEventBus game) {
        game.addListener(RelicCards::onAddReloadListeners);
        game.addListener(RelicEvents::onLoggedIn);
        game.addListener(RelicEvents::onRespawn);
        game.addListener(RelicEvents::onChangedDimension);
        game.addListener(RelicEvents::onCurioChange);
        io.github.happysnappingturtle.mythicage.perf.Perf.tick(game, "relic_events", RelicEvents::onServerTickPost);
        game.addListener(RelicEvents::onIncomingDamage);
        game.addListener(RelicEvents::onConversion);
        game.addListener(RelicEvents::onRightClickBlock);
        game.addListener(RelicEvents::onServerStopped);
        game.addListener(RelicCommand::onRegisterCommands);
    }

    static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            RelicService.reconcile(p);
        }
    }

    static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            RelicService.reconcile(p);
        }
    }

    static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            RelicService.reconcile(p);
        }
    }

    static void onCurioChange(CurioChangeEvent e) {
        if (RelicItems.SLOT.equals(e.getIdentifier()) && e.getEntity() instanceof ServerPlayer p) {
            RelicService.reconcile(p);
        }
    }

    static void onServerTickPost(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        int tick = server.getTickCount();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (Math.floorMod(p.getUUID().hashCode() + tick, PERIOD) == 0) {
                RelicService.reconcile(p);
            }
        }
        if (tick % PERIOD == 0) {
            RelicEffects.tickBelievers(server);
        }
    }

    static void onIncomingDamage(LivingIncomingDamageEvent e) {
        if (e.getEntity().level().isClientSide) {
            return;
        }
        List<Double> r = RelicEffects.meleeReductions(e.getEntity(), e.getSource());
        if (!r.isEmpty()) {
            e.setAmount(DamageRules.reduce(e.getAmount(), r, 0));
        }
    }

    static void onConversion(LivingConversionEvent.Pre e) {
        if (e.getOutcome() == EntityType.ZOMBIE_VILLAGER && RelicEffects.protectedFromConversion(e.getEntity())) {
            e.setCanceled(true);
        }
    }

    /** 手持圣物潜行右键神庙 = 供奉（D1）。两端都取消，避免物品自己的右键行为；服务器端执行。 */
    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock e) {
        if (!e.getEntity().isSecondaryUseActive() || !(e.getItemStack().getItem() instanceof RelicItem)
                || !(e.getLevel().getBlockEntity(e.getPos()) instanceof ShrineBlockEntity be)
                || RelicAnchor.Registry.forKind(be.kind()).isEmpty()) {
            return;
        }
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
        if (e.getEntity() instanceof ServerPlayer p && e.getLevel() instanceof ServerLevel level) {
            RelicService.offer(p, level, e.getPos(), e.getHand());
        }
    }

    static void onServerStopped(ServerStoppedEvent e) {
        OfferedIndex.clear();
        RelicEffects.clear();
    }
}
