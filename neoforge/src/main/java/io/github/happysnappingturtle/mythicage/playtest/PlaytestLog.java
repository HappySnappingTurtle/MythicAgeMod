package io.github.happysnappingturtle.mythicage.playtest;

import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.advancement.MythEvents;
import io.github.happysnappingturtle.mythicage.core.playtest.MilestoneClock;
import io.github.happysnappingturtle.mythicage.core.playtest.PlaytestNote;
import io.github.happysnappingturtle.mythicage.core.playtest.PlaytestStations;
import io.github.happysnappingturtle.mythicage.core.player.Age;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * 试玩记录文件（GA-3 设计文档 §4.6、§5）：游戏目录 {@code logs/mythic_age_playtest.md}，追加写入。
 * 记站点开始与结束、里程碑（每站每种只记第一次）、玩家的反馈。
 */
public final class PlaytestLog {
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Map<UUID, MilestoneClock> CLOCKS = new ConcurrentHashMap<>();
    /** 进站后这么多刻内的事件不记：准备时提交的神庙扫描、进度补发会晚几刻才到。 */
    static final int QUIET_TICKS = 200;
    private static final Map<UUID, Integer> QUIET_UNTIL = new ConcurrentHashMap<>();
    private static volatile Path file = FMLPaths.GAMEDIR.get().resolve("logs").resolve("mythic_age_playtest.md");
    private static volatile LongSupplier clock = System::currentTimeMillis;

    private PlaytestLog() {
    }

    public static void init() {
        MythEvents.addListener(PlaytestLog::onEvent);
    }

    public static Path file() {
        return file;
    }

    /** 只供 GameTest：改写到临时文件、换时钟。 */
    public static void overrideForTest(Path to, LongSupplier now) {
        file = to;
        clock = now;
    }

    /** 只供 GameTest：去掉进站后的静默期。 */
    public static void unquietForTest(UUID player) {
        QUIET_UNTIL.remove(player);
    }

    private static MilestoneClock clockOf(ServerPlayer p) {
        return CLOCKS.computeIfAbsent(p.getUUID(), k -> new MilestoneClock());
    }

    /** 开始准备新一站：先结束当前站。 */
    public static void interrupt(ServerPlayer p) {
        clockOf(p).interrupt(clock.getAsLong(), p.server.getTickCount()).ifPresent(e -> write(ended(e)));
    }

    public static void start(ServerPlayer p, PlaytestStations.Station s) {
        clockOf(p).start(s.id(), clock.getAsLong(), p.server.getTickCount());
        QUIET_UNTIL.put(p.getUUID(), p.server.getTickCount() + QUIET_TICKS);
        write("");
        write("## " + LocalDateTime.now().format(DAY) + " " + p.getGameProfile().getName() + " 第 " + s.number() + " 站 "
                + s.id() + "（开始）");
    }

    public static Optional<MilestoneClock.Ended> done(ServerPlayer p) {
        Optional<MilestoneClock.Ended> e = clockOf(p).done(clock.getAsLong(), p.server.getTickCount());
        e.ifPresent(x -> write(ended(x)));
        return e;
    }

    static void failed(ServerPlayer p, PlaytestStations.Station s, String reason) {
        write("- " + PlaytestNote.hhmm(LocalTime.now()) + " [失败] " + p.getGameProfile().getName() + " 第 " + s.number()
                + " 站 " + s.id() + "：" + reason);
    }

    static void note(String line) {
        write(line);
    }

    private static String ended(MilestoneClock.Ended e) {
        return "- " + PlaytestNote.hhmm(LocalTime.now()) + (e.switched() ? " 切换，" : " 结束，") + e.station() + " 用时 "
                + MilestoneClock.format(e.seconds()) + "（游戏 " + e.ticks() + " 刻）";
    }

    private static void onEvent(ServerPlayer p, MythEvents.Fired f) {
        MilestoneClock c = CLOCKS.get(p.getUUID());
        if (c == null || c.station().isEmpty()
                || p.server.getTickCount() < QUIET_UNTIL.getOrDefault(p.getUUID(), Integer.MIN_VALUE)) {
            return;
        }
        String key;
        String what;
        switch (f.event()) {
            case MythEvents.BLESSED -> {
                key = "blessed";
                what = "受祝福";
            }
            case MythEvents.FAVOR_GAINED -> {
                key = "favor";
                what = "第一次恩宠入账 +" + f.value();
            }
            case MythEvents.TEMPLE_LEVEL -> {
                key = "temple:" + f.value();
                what = "神庙认定 " + f.value() + " 级";
            }
            case MythEvents.AGE_REACHED -> {
                key = "age:" + f.value();
                what = "时代：" + Age.values()[Math.max(0, Math.min(Age.values().length - 1, f.value()))].id();
            }
            case MythEvents.QUEST_DONE -> {
                key = "quest";
                what = "完成委托";
            }
            case MythEvents.VILLAGE_ENTERED -> {
                key = "village";
                what = "进入村庄";
            }
            default -> {
                if (!f.event().startsWith("bestiary:")) {
                    return;
                }
                key = f.event() + ":" + f.value();
                what = "图鉴 " + f.event().substring("bestiary:".length()) + " 第 " + f.value() + " 档";
            }
        }
        if (c.milestone(key)) {
            write(PlaytestNote.milestone(LocalTime.now(), what));
        }
    }

    private static synchronized void write(String line) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, line + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            MythicAge.LOGGER.warn("写试玩记录失败：{}", file, e);
        }
    }
}
