package com.example.mythic_age.system;

import com.example.mythic_age.block.StatueBaseBlock;
import com.example.mythic_age.entity.CivilizedVillager;
import com.example.mythic_age.item.GodPowerCertificateItem;
import com.example.mythic_age.item.ModItems;
import com.example.mythic_age.world.MythicAgeWorldState;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.VillagerProfession;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 中华文明证书增强管理器。
 *
 * 管理两条新增产出渠道：
 * - 村民劳作产出（村民在职业站点工作时概率产出）
 * - 玩家交易产出（与中华村民完成原版交易时概率产出）
 */
public class ChineseCertificateManager {

    private static final ChineseCertificateManager INSTANCE = new ChineseCertificateManager();

    private static final int WORK_SCAN_INTERVAL = 100;
    private static final int WORK_VILLAGER_COOLDOWN = 1200;
    private static final int WORK_TEMPLE_CAP_INTERVAL = 2400;
    private static final int WORK_TEMPLE_CAP = 8;

    private static final int TRADE_PAIR_COOLDOWN = 600;
    private static final int TRADE_PLAYER_CAP_INTERVAL = 2400;
    private static final int TRADE_PLAYER_CAP = 10;

    private static final int TEMPLE_RANGE = 32;

    private static final float WORK_BASE_CHANCE = 0.12f;

    private final Map<String, Long> tradePairCooldowns = new HashMap<>();
    private final Map<UUID, TradeCapData> playerTradeCaps = new HashMap<>();
    private final Map<Long, TempleWorkCapData> templeWorkCaps = new HashMap<>();

    public static ChineseCertificateManager getInstance() {
        return INSTANCE;
    }

    // ── 村民劳作产出 ──

    public void tickVillagerWork(ServerWorld world) {
        if (world.getTime() % WORK_SCAN_INTERVAL != 0) return;

        List<VillagerEntity> chineseWorkers = world.getEntitiesByClass(
            VillagerEntity.class,
            new Box(
                world.getWorldBorder().getBoundWest(), world.getBottomY(), world.getWorldBorder().getBoundNorth(),
                world.getWorldBorder().getBoundEast(), world.getTopY(), world.getWorldBorder().getBoundSouth()),
            v -> v.isAlive()
                && v instanceof CivilizedVillager cv
                && cv.mythicAge$getCivilization() == GodPowerCertificateItem.MythologyType.CHINESE
                && isWorking(v)
        );

        for (VillagerEntity villager : chineseWorkers) {
            if (!canGenerateWorkCertificate(villager, world)) continue;
            if (world.getRandom().nextFloat() >= WORK_BASE_CHANCE) continue;

            BlockPos templeCluster = clusterKey(villager.getBlockPos());
            if (!templeWorkCapAllows(templeCluster, world.getTime())) continue;

            generateWorkCertificate(world, villager, 1);
            setVillagerWorkCooldown(villager, WORK_VILLAGER_COOLDOWN);
            recordTempleWorkGeneration(templeCluster, world.getTime(), 1);
        }
    }

    private boolean isWorking(VillagerEntity villager) {
        var profession = villager.getVillagerData().getProfession();
        if (profession == VillagerProfession.NONE || profession == VillagerProfession.NITWIT) {
            return false;
        }
        return villager.getBrain()
            .getOptionalRegisteredMemory(net.minecraft.entity.ai.brain.MemoryModuleType.JOB_SITE)
            .isPresent();
    }

    private boolean canGenerateWorkCertificate(VillagerEntity villager, ServerWorld world) {
        if (!(villager instanceof CivilizedVillager cv)) return false;
        return cv.mythicAge$getWorkCooldown() <= 0;
    }

    private void setVillagerWorkCooldown(VillagerEntity villager, int ticks) {
        if (villager instanceof CivilizedVillager cv) {
            cv.mythicAge$setWorkCooldown(ticks);
        }
    }

    private boolean templeWorkCapAllows(BlockPos cluster, long worldTime) {
        TempleWorkCapData cap = templeWorkCaps.get(cluster.asLong());
        if (cap == null) return true;
        if (worldTime - cap.periodStart >= WORK_TEMPLE_CAP_INTERVAL) return true;
        return cap.generated < WORK_TEMPLE_CAP;
    }

    private void recordTempleWorkGeneration(BlockPos cluster, long worldTime, int amount) {
        long key = cluster.asLong();
        TempleWorkCapData cap = templeWorkCaps.get(key);
        if (cap == null || worldTime - cap.periodStart >= WORK_TEMPLE_CAP_INTERVAL) {
            templeWorkCaps.put(key, new TempleWorkCapData(worldTime, amount));
        } else {
            cap.generated += amount;
        }
    }

    private void generateWorkCertificate(ServerWorld world, VillagerEntity villager, int amount) {
        BlockPos pos = villager.getBlockPos();
        BlockPos statueBase = findNearestChineseStatueBase(world, pos);
        ItemStack cert = new ItemStack(ModItems.GOD_POWER_CERTIFICATE_CHINESE, amount);

        if (statueBase != null) {
            var block = world.getBlockState(statueBase).getBlock();
            if (block instanceof StatueBaseBlock sb) {
                sb.addCertificates(world, statueBase, amount);
            }
        } else {
            world.spawnEntity(new ItemEntity(world,
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, cert));
        }

        notifyNearbyPlayers(world, pos,
            Text.translatable("mythic_age.certificate.chinese_work",
                villager.getVillagerData().getProfession().id(), amount)
                .formatted(Formatting.GOLD));
    }

    // ── 玩家交易产出 ──

    public void onVillagerTrade(PlayerEntity player, VillagerEntity villager, TradeOffer offer) {
        if (player == null || villager == null || offer == null) return;
        if (player.getWorld().isClient()) return;
        if (!(villager instanceof CivilizedVillager cv)) return;
        if (cv.mythicAge$getCivilization() != GodPowerCertificateItem.MythologyType.CHINESE) return;

        ServerWorld world = (ServerWorld) player.getWorld();

        if (!canGenerateTradeCertificate(player, villager, world)) return;

        int level = villager.getVillagerData().getLevel();
        float chance;
        int baseAmount;
        if (level <= 1) {
            chance = 0.30f;
            baseAmount = 1;
        } else if (level <= 3) {
            chance = 0.45f;
            baseAmount = 1 + world.getRandom().nextInt(2);
        } else {
            chance = 0.60f;
            baseAmount = 2 + world.getRandom().nextInt(2);
        }

        if (world.getRandom().nextFloat() >= chance) return;

        recordTradePairCooldown(player, villager, world.getTime());
        recordPlayerTradeGeneration(player.getUuid(), world.getTime(), baseAmount);

        ItemStack cert = new ItemStack(ModItems.GOD_POWER_CERTIFICATE_CHINESE, baseAmount);
        if (!player.getInventory().insertStack(cert)) {
            player.dropItem(cert, false);
        }

        player.sendMessage(
            Text.translatable("mythic_age.certificate.chinese_trade",
                villager.getVillagerData().getProfession().id(), baseAmount)
                .formatted(Formatting.GOLD),
            false);
    }

    private boolean canGenerateTradeCertificate(PlayerEntity player, VillagerEntity villager, ServerWorld world) {
        long now = world.getTime();

        String pairKey = tradePairKey(player, villager);
        Long lastTrade = tradePairCooldowns.get(pairKey);
        if (lastTrade != null && now - lastTrade < TRADE_PAIR_COOLDOWN) return false;

        TradeCapData cap = playerTradeCaps.get(player.getUuid());
        if (cap != null && now - cap.periodStart < TRADE_PLAYER_CAP_INTERVAL && cap.generated >= TRADE_PLAYER_CAP) {
            return false;
        }
        return true;
    }

    private void recordTradePairCooldown(PlayerEntity player, VillagerEntity villager, long worldTime) {
        tradePairCooldowns.put(tradePairKey(player, villager), worldTime);
    }

    private void recordPlayerTradeGeneration(UUID playerUuid, long worldTime, int amount) {
        TradeCapData cap = playerTradeCaps.get(playerUuid);
        if (cap == null || worldTime - cap.periodStart >= TRADE_PLAYER_CAP_INTERVAL) {
            playerTradeCaps.put(playerUuid, new TradeCapData(worldTime, amount));
        } else {
            cap.generated += amount;
        }
    }

    private String tradePairKey(PlayerEntity player, VillagerEntity villager) {
        return player.getUuidAsString() + "|" + villager.getUuidAsString();
    }

    // ── 工具方法 ──

    private BlockPos clusterKey(BlockPos pos) {
        return new BlockPos(pos.getX() >> 5 << 5, pos.getY(), pos.getZ() >> 5 << 5);
    }

    private BlockPos findNearestChineseStatueBase(ServerWorld world, BlockPos center) {
        BlockPos closest = null;
        double closestDist = Double.MAX_VALUE;
        for (int x = -48; x <= 48; x += 2) {
            for (int z = -48; z <= 48; z += 2) {
                for (int y = -12; y <= 16; y++) {
                    BlockPos check = center.add(x, y, z);
                    if (world.getBlockState(check).getBlock() instanceof StatueBaseBlock sb
                        && sb.getMythologyType() == GodPowerCertificateItem.MythologyType.CHINESE) {
                        double d = center.getSquaredDistance(check);
                        if (d < closestDist) {
                            closestDist = d;
                            closest = check;
                        }
                    }
                }
            }
        }
        return closest;
    }

    private void notifyNearbyPlayers(ServerWorld world, BlockPos pos, Text message) {
        world.getPlayers().stream()
            .filter(p -> p.squaredDistanceTo(pos.getX(), pos.getY(), pos.getZ()) < 1024)
            .forEach(p -> p.sendMessage(message, false));
    }

    // ── NBT 持久化 ──

    public NbtCompound writePersistentState() {
        NbtCompound root = new NbtCompound();

        NbtList pairList = new NbtList();
        for (var entry : tradePairCooldowns.entrySet()) {
            NbtCompound c = new NbtCompound();
            c.putString("Key", entry.getKey());
            c.putLong("Time", entry.getValue());
            pairList.add(c);
        }
        root.put("TradePairCooldowns", pairList);

        NbtList capList = new NbtList();
        for (var entry : playerTradeCaps.entrySet()) {
            NbtCompound c = new NbtCompound();
            c.putUuid("Player", entry.getKey());
            c.putLong("Start", entry.getValue().periodStart);
            c.putInt("Generated", entry.getValue().generated);
            capList.add(c);
        }
        root.put("PlayerTradeCaps", capList);

        NbtList workList = new NbtList();
        for (var entry : templeWorkCaps.entrySet()) {
            NbtCompound c = new NbtCompound();
            c.putLong("Pos", entry.getKey());
            c.putLong("Start", entry.getValue().periodStart);
            c.putInt("Generated", entry.getValue().generated);
            workList.add(c);
        }
        root.put("TempleWorkCaps", workList);

        return root;
    }

    public void readPersistentState(NbtCompound nbt) {
        tradePairCooldowns.clear();
        playerTradeCaps.clear();
        templeWorkCaps.clear();

        if (nbt.contains("TradePairCooldowns", NbtElement.LIST_TYPE)) {
            NbtList list = nbt.getList("TradePairCooldowns", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < list.size(); i++) {
                NbtCompound c = list.getCompound(i);
                tradePairCooldowns.put(c.getString("Key"), c.getLong("Time"));
            }
        }

        if (nbt.contains("PlayerTradeCaps", NbtElement.LIST_TYPE)) {
            NbtList list = nbt.getList("PlayerTradeCaps", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < list.size(); i++) {
                NbtCompound c = list.getCompound(i);
                playerTradeCaps.put(c.getUuid("Player"), new TradeCapData(c.getLong("Start"), c.getInt("Generated")));
            }
        }

        if (nbt.contains("TempleWorkCaps", NbtElement.LIST_TYPE)) {
            NbtList list = nbt.getList("TempleWorkCaps", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < list.size(); i++) {
                NbtCompound c = list.getCompound(i);
                templeWorkCaps.put(c.getLong("Pos"), new TempleWorkCapData(c.getLong("Start"), c.getInt("Generated")));
            }
        }
    }

    // ── 内部数据类 ──

    private static class TradeCapData {
        final long periodStart;
        int generated;
        TradeCapData(long periodStart, int generated) {
            this.periodStart = periodStart;
            this.generated = generated;
        }
    }

    private static class TempleWorkCapData {
        final long periodStart;
        int generated;
        TempleWorkCapData(long periodStart, int generated) {
            this.periodStart = periodStart;
            this.generated = generated;
        }
    }
}
