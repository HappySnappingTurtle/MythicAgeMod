package io.github.happysnappingturtle.mythicage.boss;

import io.github.happysnappingturtle.mythicage.conduct.Responsibility;
import io.github.happysnappingturtle.mythicage.power.TempBlocks;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 试炼竞技场的事件（T23 §4.3）：推进、场地保护（挖、炸、战中放方块、神力临时方块）、Boss 受伤（参战者、机制窗口倍率）与死亡。
 */
public final class TrialArenaEvents {
    private TrialArenaEvents() {
    }

    public static void register(IEventBus game) {
        game.addListener(TrialArenaEvents::onServerTick);
        game.addListener(TrialArenaEvents::onServerStarted);
        game.addListener(TrialArenaEvents::onBreak);
        game.addListener(TrialArenaEvents::onPlace);
        game.addListener(TrialArenaEvents::onExplosion);
        game.addListener(TrialArenaEvents::onDamage);
        game.addListener(TrialArenaEvents::onDeath);
        game.addListener(ArenaSpec.Loader::onAddReloadListeners);
        game.addListener(ArenaCommand::onRegisterCommands);
        TempBlocks.PROTECTED.add((level, pos) -> TrialArenas.get(level.getServer()).protecting(level, pos).isPresent());
    }

    static void onServerStarted(ServerStartedEvent e) {
        TrialArenas.get(e.getServer());
    }

    static void onServerTick(ServerTickEvent.Post e) {
        TrialArenas.get(e.getServer()).tick(e.getServer());
    }

    static void onBreak(BlockEvent.BreakEvent e) {
        if (e.getLevel() instanceof ServerLevel level && !e.getPlayer().isCreative()
                && TrialArenas.get(level.getServer()).protecting(level, e.getPos()).isPresent()) {
            e.setCanceled(true);
            e.getPlayer().displayClientMessage(Component.translatable("mythic_age.arena.protected"), true);
        }
    }

    static void onPlace(BlockEvent.EntityPlaceEvent e) {
        if (!(e.getLevel() instanceof ServerLevel level) || !(e.getEntity() instanceof Player p) || p.isCreative()) {
            return;
        }
        TrialArenas.get(level.getServer()).protecting(level, e.getPos()).ifPresent(a -> {
            if (a.phase().fighting() || a.isGate(e.getPos())) {
                e.setCanceled(true);
                p.displayClientMessage(Component.translatable("mythic_age.arena.no_build"), true);
            }
        });
    }

    static void onExplosion(ExplosionEvent.Detonate e) {
        if (e.getLevel() instanceof ServerLevel level) {
            TrialArenas arenas = TrialArenas.get(level.getServer());
            e.getAffectedBlocks().removeIf(p -> arenas.protecting(level, p).isPresent());
        }
    }

    static void onDamage(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof TrialBoss boss) || !(e.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        TrialArenas.get(level.getServer()).byId(boss.arenaId()).ifPresent(a -> {
            Responsibility.Blame blame = Responsibility.of(e.getSource());
            blame.player().ifPresent(id -> a.participants().damage(id, blame.via() != Responsibility.Via.SOLDIER
                    && blame.via() != Responsibility.Via.NONE && !creative(level, id)));
        });
        float m = boss.damageTakenMultiplier();
        if (m != 1.0F) {
            e.setAmount(e.getAmount() * m);
        }
    }

    private static boolean creative(ServerLevel level, java.util.UUID id) {
        Player p = level.getPlayerByUUID(id);
        return p != null && p.isCreative();
    }

    static void onDeath(LivingDeathEvent e) {
        Entity ent = e.getEntity();
        if (ent instanceof TrialBoss boss && ent instanceof Mob mob && ent.level() instanceof ServerLevel level) {
            TrialArenas.get(level.getServer()).byId(boss.arenaId()).ifPresent(a -> a.onBossDeath(level, mob));
        }
    }
}
