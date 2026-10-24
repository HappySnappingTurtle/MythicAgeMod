package io.github.happysnappingturtle.mythicage.village.economy;

import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.core.village.build.BuildPlacement;
import io.github.happysnappingturtle.mythicage.core.village.build.BuildProject;
import io.github.happysnappingturtle.mythicage.core.village.gen.Box;
import io.github.happysnappingturtle.mythicage.core.village.gen.Turn;
import io.github.happysnappingturtle.mythicage.core.village.economy.VillageStock;
import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.core.village.resource.ResourcePoint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 村庄运行数据（T19 设计文档 §2.2，D2）：SavedData {@code mythic_age_village_economy}，按村庄 id 存一份 {@link State}。
 * 和村庄规划分开：规划是生成时定下的结构，这里是每刻都在变的库存、名册、资源点、施工项目（阶段 ③）。
 * 读档时所有工作单的托管退回库存（工作单不存进度，V1 §4.3）；施工项目的托管（{@code project:}）保留，项目按段续上。
 */
public final class VillageEconomy extends SavedData {
    public static final String NAME = "mythic_age_village_economy";
    public static final int LOG_LIMIT = 32;
    /** {@code lastRealTick} 的「从没真实运行过」。 */
    public static final long NEVER = Long.MIN_VALUE;

    /** 名册一行。{@code keeper}：谷仓管事（D4）。 */
    public record Member(UUID id, String profession, BlockPos home, boolean keeper) {
        public Member withProfession(String p) {
            return new Member(id, p, home, keeper);
        }
    }

    /** 无人值守段（酒瓮发酵）：开始的 MC 日、托管 id。 */
    public record Vessel(BlockPos pos, String recipe, long startDay) {
        public String escrowId() {
            return "vessel:" + pos.asLong();
        }
    }

    /** 发展记录一条：语言键与参数。 */
    public record LogEntry(long day, String key, List<String> args) {
    }

    /** 施工建成的建筑（T19 阶段 ③）：原地替换时按它找旧建筑；容量、次神祠、唯一建筑都看它。 */
    public record Built(String building, Pos origin, Turn turn, Box box, String plot, String god, long day) {
        public BuildPlacement.Placed placed() {
            return new BuildPlacement.Placed(building, origin, turn, box);
        }
    }

    /** 落成通告（§1.4 E7）：{@code sent} 里是已经看过的玩家。 */
    public record Notice(long day, String building, String god, Set<UUID> sent) {
    }

    public static final class State {
        public final UUID village;
        public final VillageStock stock = new VillageStock(0);
        public final Map<UUID, Member> roster = new LinkedHashMap<>();
        public final Map<String, ResourcePoint> points = new LinkedHashMap<>();
        public final Map<BlockPos, Vessel> vessels = new LinkedHashMap<>();
        public final List<LogEntry> log = new ArrayList<>();
        public final Set<UUID> seenBy = new LinkedHashSet<>();
        /** 接管完成：S0 库存已发、资源点已登记、过渡期原版村民已换成居民（D3）。 */
        public boolean initialized;
        public long lastRealTick = NEVER;
        public long constructionBank;
        public long rationsDay = -1;
        /** 最近一次「每日开始」处理的 MC 日。 */
        public long lastDay = -1;
        /** 施工项目（进行中的；建成或取消后移走）。 */
        public final List<BuildProject> projects = new ArrayList<>();
        public final List<Built> constructed = new ArrayList<>();
        /** 建成的地块（PlotWatch 不再检查）。 */
        public final Set<String> builtPlots = new LinkedHashSet<>();
        public final List<Notice> notices = new ArrayList<>();
        public long nextProjectId = 1;
        /** 在途移民（§1.6 H4，每村最多 1 名）：实体 UUID 与动身的世界刻；没有为 null。 */
        public UUID migrant;
        public long migrantSince;

        State(UUID village) {
            this.village = village;
        }

        public void log(long day, String key, String... args) {
            log.add(new LogEntry(day, key, List.of(args)));
            while (log.size() > LOG_LIMIT) {
                log.remove(0);
            }
        }

        public Optional<Member> keeper() {
            return roster.values().stream().filter(Member::keeper).findFirst();
        }

        public Optional<BuildProject> activeProject() {
            return projects.stream().filter(BuildProject::active).findFirst();
        }
    }

    private final Map<UUID, State> states = new LinkedHashMap<>();

    public static VillageEconomy get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(VillageEconomy::new, VillageEconomy::read, null), NAME);
    }

    public State state(UUID village) {
        setDirty();
        return states.computeIfAbsent(village, State::new);
    }

    public Optional<State> peek(UUID village) {
        return Optional.ofNullable(states.get(village));
    }

    public List<State> all() {
        return new ArrayList<>(states.values());
    }

    public void remove(UUID village) {
        if (states.remove(village) != null) {
            setDirty();
        }
    }

    /** 只供 GameTest。 */
    public void clearForTest(UUID village) {
        remove(village);
    }

    // ───────────── 存取 ─────────────

    public static VillageEconomy read(CompoundTag tag, HolderLookup.Provider registries) {
        VillageEconomy e = new VillageEconomy();
        for (Tag t : tag.getList("villages", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            try {
                State s = new State(c.getUUID("village"));
                CompoundTag items = c.getCompound("stock");
                items.getAllKeys().forEach(k -> s.stock.putUnchecked(k, items.getInt(k)));
                CompoundTag esc = c.getCompound("escrow");
                for (String id : esc.getAllKeys()) {
                    CompoundTag m = esc.getCompound(id);
                    Map<String, Integer> what = new LinkedHashMap<>();
                    m.getAllKeys().forEach(k -> what.put(k, m.getInt(k)));
                    s.stock.putEscrowUnchecked(id, what);
                }
                int released = s.stock.releaseJobEscrows();
                if (released > 0) {
                    MythicAge.LOGGER.debug("村庄 {} 读档：退回 {} 笔工作单托管", s.village, released);
                }
                for (Tag r : c.getList("roster", Tag.TAG_COMPOUND)) {
                    CompoundTag m = (CompoundTag) r;
                    Member mem = new Member(m.getUUID("id"), m.getString("profession"), BlockPos.of(m.getLong("home")),
                            m.getBoolean("keeper"));
                    s.roster.put(mem.id(), mem);
                }
                for (Tag p : c.getList("points", Tag.TAG_COMPOUND)) {
                    CompoundTag m = (CompoundTag) p;
                    BlockPos b = BlockPos.of(m.getLong("pos"));
                    ResourcePoint rp = new ResourcePoint(m.getString("id"), m.getString("kind"),
                            new Pos(b.getX(), b.getY(), b.getZ()), m.getInt("remaining"), m.getInt("max"),
                            m.getInt("recover"), m.getLong("lastDay"));
                    s.points.put(rp.id(), rp);
                }
                for (Tag v : c.getList("vessels", Tag.TAG_COMPOUND)) {
                    CompoundTag m = (CompoundTag) v;
                    Vessel ves = new Vessel(BlockPos.of(m.getLong("pos")), m.getString("recipe"), m.getLong("start"));
                    s.vessels.put(ves.pos(), ves);
                }
                for (Tag l : c.getList("log", Tag.TAG_COMPOUND)) {
                    CompoundTag m = (CompoundTag) l;
                    List<String> args = new ArrayList<>();
                    m.getList("args", Tag.TAG_STRING).forEach(a -> args.add(a.getAsString()));
                    s.log.add(new LogEntry(m.getLong("day"), m.getString("key"), List.copyOf(args)));
                }
                for (Tag u : c.getList("seenBy", Tag.TAG_INT_ARRAY)) {
                    s.seenBy.add(net.minecraft.nbt.NbtUtils.loadUUID(u));
                }
                s.initialized = c.getBoolean("initialized");
                s.lastRealTick = c.contains("lastRealTick") ? c.getLong("lastRealTick") : NEVER;
                s.constructionBank = c.getLong("constructionBank");
                s.rationsDay = c.getLong("rationsDay");
                s.lastDay = c.getLong("lastDay");
                for (Tag t2 : c.getList("projects", Tag.TAG_COMPOUND)) {
                    s.projects.add(ProjectNbt.read((CompoundTag) t2));
                }
                for (Tag t2 : c.getList("constructed", Tag.TAG_COMPOUND)) {
                    s.constructed.add(ProjectNbt.readBuilt((CompoundTag) t2));
                }
                c.getList("builtPlots", Tag.TAG_STRING).forEach(t2 -> s.builtPlots.add(t2.getAsString()));
                for (Tag t2 : c.getList("notices", Tag.TAG_COMPOUND)) {
                    s.notices.add(ProjectNbt.readNotice((CompoundTag) t2));
                }
                s.nextProjectId = Math.max(1, c.getLong("nextProjectId"));
                s.migrant = c.hasUUID("migrant") ? c.getUUID("migrant") : null;
                s.migrantSince = c.getLong("migrantSince");
                e.states.put(s.village, s);
            } catch (RuntimeException ex) {
                MythicAge.LOGGER.warn("跳过损坏的村庄运行数据：{}", ex.getMessage());
            }
        }
        e.setDirty(false);
        return e;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (State s : states.values()) {
            CompoundTag c = new CompoundTag();
            c.putUUID("village", s.village);
            CompoundTag items = new CompoundTag();
            s.stock.items().forEach(items::putInt);
            c.put("stock", items);
            CompoundTag esc = new CompoundTag();
            s.stock.escrows().forEach((id, what) -> {
                CompoundTag m = new CompoundTag();
                what.forEach(m::putInt);
                esc.put(id, m);
            });
            c.put("escrow", esc);
            ListTag roster = new ListTag();
            for (Member m : s.roster.values()) {
                CompoundTag r = new CompoundTag();
                r.putUUID("id", m.id());
                r.putString("profession", m.profession());
                r.putLong("home", m.home().asLong());
                r.putBoolean("keeper", m.keeper());
                roster.add(r);
            }
            c.put("roster", roster);
            ListTag points = new ListTag();
            for (ResourcePoint p : s.points.values()) {
                CompoundTag r = new CompoundTag();
                r.putString("id", p.id());
                r.putString("kind", p.kind());
                r.putLong("pos", BlockPos.asLong(p.pos().x(), p.pos().y(), p.pos().z()));
                r.putInt("remaining", p.remaining());
                r.putInt("max", p.max());
                r.putInt("recover", p.recoverPerDay());
                r.putLong("lastDay", p.lastDay());
                points.add(r);
            }
            c.put("points", points);
            ListTag vessels = new ListTag();
            for (Vessel v : s.vessels.values()) {
                CompoundTag r = new CompoundTag();
                r.putLong("pos", v.pos().asLong());
                r.putString("recipe", v.recipe());
                r.putLong("start", v.startDay());
                vessels.add(r);
            }
            c.put("vessels", vessels);
            ListTag log = new ListTag();
            for (LogEntry l : s.log) {
                CompoundTag r = new CompoundTag();
                r.putLong("day", l.day());
                r.putString("key", l.key());
                ListTag args = new ListTag();
                l.args().forEach(a -> args.add(StringTag.valueOf(a)));
                r.put("args", args);
                log.add(r);
            }
            c.put("log", log);
            ListTag seen = new ListTag();
            s.seenBy.forEach(u -> seen.add(net.minecraft.nbt.NbtUtils.createUUID(u)));
            c.put("seenBy", seen);
            c.putBoolean("initialized", s.initialized);
            c.putLong("lastRealTick", s.lastRealTick);
            c.putLong("constructionBank", s.constructionBank);
            c.putLong("rationsDay", s.rationsDay);
            c.putLong("lastDay", s.lastDay);
            ListTag projects = new ListTag();
            s.projects.forEach(pr -> projects.add(ProjectNbt.write(pr)));
            c.put("projects", projects);
            ListTag built = new ListTag();
            s.constructed.forEach(b -> built.add(ProjectNbt.writeBuilt(b)));
            c.put("constructed", built);
            ListTag plots = new ListTag();
            s.builtPlots.forEach(pl -> plots.add(StringTag.valueOf(pl)));
            c.put("builtPlots", plots);
            ListTag notices = new ListTag();
            s.notices.forEach(n -> notices.add(ProjectNbt.writeNotice(n)));
            c.put("notices", notices);
            c.putLong("nextProjectId", s.nextProjectId);
            if (s.migrant != null) {
                c.putUUID("migrant", s.migrant);
                c.putLong("migrantSince", s.migrantSince);
            }
            list.add(c);
        }
        tag.put("villages", list);
        return tag;
    }

    /** 只供 GameTest（TC-T19-50）：存一遍再读回来，模拟重启。 */
    public static VillageEconomy roundTripForTest(VillageEconomy e, HolderLookup.Provider registries) {
        return read(e.save(new CompoundTag(), registries), registries);
    }

    /** 只供 GameTest：用读回来的数据替换某个村庄的状态。 */
    public void replaceForTest(UUID village, VillageEconomy from) {
        from.peek(village).ifPresent(s -> states.put(village, s));
        setDirty();
    }
}
