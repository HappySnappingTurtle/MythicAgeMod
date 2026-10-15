package io.github.happysnappingturtle.mythicage.guardian;

import io.github.happysnappingturtle.mythicage.config.ModConfigs;
import io.github.happysnappingturtle.mythicage.content.ModTags;
import io.github.happysnappingturtle.mythicage.core.config.world.WorldRulesConfig;
import io.github.happysnappingturtle.mythicage.core.guardian.GuardianRules;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 守护者的出现、复原与去重（T9 设计文档 §7，决策 D1、D4）。
 * <ul>
 *   <li>村庄：每 100 刻对每位在线玩家查 48 格内的聚会钟；钟在希腊主群系、且还没有祭司 → 在钟旁生成一位。</li>
 *   <li>复原：死亡满 {@code respawnDays} 个 MC 日、居所区块已加载 → 在居所生成，guardianId 不变。</li>
 *   <li>去重：实体进入世界时，若注册表记录的当前实体不是它（例如从旧区块读回的副本），拒绝加入。</li>
 * </ul>
 */
public final class GuardianSpawner {
    public static final int INTERVAL = 100;
    public static final int VILLAGE_RADIUS = 48;
    public static final int BELL_RADIUS = 3;

    private GuardianSpawner() {
    }

    static WorldRulesConfig.Guardians config() {
        return ModConfigs.worldRules().current().guardians();
    }

    /** 交易界面保持打开的最大距离。 */
    public static int interactRange() {
        return config().interactRange();
    }

    /** 生成一位新祭司并登记。 */
    @Nullable
    public static GreekGuardianEntity spawnNew(ServerLevel level, BlockPos at, Optional<BlockPos> bell) {
        GreekGuardianEntity g = ModEntities.GREEK_GUARDIAN.get().create(level);
        if (g == null) {
            return null;
        }
        g.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.random.nextFloat() * 360, 0);
        registerNew(level, g, at, bell);
        g.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null);
        level.addFreshEntity(g);
        return g;
    }

    /** 给没有身份的祭司登记一个新身份（刷怪蛋、命令、村庄规则共用）。 */
    static void registerNew(ServerLevel level, GreekGuardianEntity g, BlockPos home, Optional<BlockPos> bell) {
        UUID id = UUID.randomUUID();
        g.assign(id, home);
        GuardianRegistry.get(level.getServer()).put(new GuardianRegistry.Entry(id, GlobalPos.of(level.dimension(), home),
                bell.map(BlockPos::immutable), true, 0, g.getUUID()));
    }

    // ───────────── 去重 ─────────────

    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof GreekGuardianEntity g) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        GuardianRegistry reg = GuardianRegistry.get(level.getServer());
        if (g.guardianId() == null) {
            registerNew(level, g, g.blockPosition(), Optional.empty());
            return;
        }
        Optional<GuardianRegistry.Entry> entry = reg.get(g.guardianId());
        if (entry.isEmpty()) {
            // 结构或其它存档带来的祭司：按它自带的身份登记
            BlockPos home = g.home() != null ? g.home() : g.blockPosition();
            reg.put(new GuardianRegistry.Entry(g.guardianId(), GlobalPos.of(level.dimension(), home), Optional.empty(),
                    true, 0, g.getUUID()));
        } else if (!entry.get().alive() || !entry.get().entityUuid().equals(g.getUUID())) {
            event.setCanceled(true);
        }
    }

    // ───────────── 每 100 刻 ─────────────

    public static void onServerTickPost(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % INTERVAL != 0) {
            return;
        }
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!p.isSpectator()) {
                spawnForArea(p.serverLevel(), p.blockPosition());
            }
        }
        respawnDue(server, config().respawnDays());
    }

    /** 村庄出现规则的入口：配置关闭时不生成（决策 D1，服主可关）。 */
    public static int spawnForArea(ServerLevel level, BlockPos pos) {
        return config().spawnInVillages() ? trySpawnNear(level, pos) : 0;
    }

    /** 在 pos 周围 48 格内的聚会钟旁补上祭司。返回新生成的数量。 */
    public static int trySpawnNear(ServerLevel level, BlockPos pos) {
        GuardianRegistry reg = GuardianRegistry.get(level.getServer());
        List<BlockPos> bells = level.getPoiManager()
                .findAll(h -> h.is(PoiTypes.MEETING), b -> true, pos, VILLAGE_RADIUS, PoiManager.Occupancy.ANY)
                .map(BlockPos::immutable).toList();
        int spawned = 0;
        for (BlockPos bell : bells) {
            if (reg.byBell(level.dimension(), bell).isPresent()
                    || !level.getBiome(bell).is(ModTags.region(Civilization.GREEK))) {
                continue;
            }
            Optional<BlockPos> spot = standableNear(level, bell, BELL_RADIUS);
            if (spot.isPresent() && spawnNew(level, spot.get(), Optional.of(bell)) != null) {
                spawned++;
            }
        }
        return spawned;
    }

    /** 复原到期的祭司。返回复原数量。 */
    public static int respawnDue(MinecraftServer server, int respawnDays) {
        GuardianRegistry reg = GuardianRegistry.get(server);
        long today = MythDays.today(server);
        int revived = 0;
        for (GuardianRegistry.Entry e : reg.all()) {
            if (e.alive() || !GuardianRules.shouldRespawn(e.deathDay(), today, respawnDays)) {
                continue;
            }
            ServerLevel level = server.getLevel(e.home().dimension());
            BlockPos home = e.home().pos();
            if (level == null || !level.isLoaded(home)) {
                continue;
            }
            GreekGuardianEntity g = ModEntities.GREEK_GUARDIAN.get().create(level);
            if (g == null) {
                continue;
            }
            BlockPos at = standableNear(level, home, BELL_RADIUS).orElse(home);
            g.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.random.nextFloat() * 360, 0);
            g.assign(e.guardianId(), home);
            reg.markAlive(e.guardianId(), g.getUUID());
            level.addFreshEntity(g);
            revived++;
        }
        return revived;
    }

    /** 中心 radius 格内（由近到远）找一个脚下结实、身体两格可通过的位置。 */
    static Optional<BlockPos> standableNear(Level level, BlockPos center, int radius) {
        for (int r = 1; r <= radius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    for (int dy = -2; dy <= 2; dy++) {
                        BlockPos p = center.offset(dx, dy, dz);
                        if (level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)
                                && level.getBlockState(p).getCollisionShape(level, p).isEmpty()
                                && level.getBlockState(p.above()).getCollisionShape(level, p.above()).isEmpty()
                                && level.getFluidState(p).isEmpty()) {
                            return Optional.of(p);
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }
}
