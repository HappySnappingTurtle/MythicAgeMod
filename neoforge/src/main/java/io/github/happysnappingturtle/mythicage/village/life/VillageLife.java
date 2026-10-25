package io.github.happysnappingturtle.mythicage.village.life;

import io.github.happysnappingturtle.mythicage.core.village.gen.Pos;
import io.github.happysnappingturtle.mythicage.core.village.life.FestivalRules;
import io.github.happysnappingturtle.mythicage.core.village.life.LifeData;
import io.github.happysnappingturtle.mythicage.core.village.work.Job;
import io.github.happysnappingturtle.mythicage.core.village.work.Priority;
import io.github.happysnappingturtle.mythicage.core.village.work.Scheduler;
import io.github.happysnappingturtle.mythicage.guardian.GuardianEntity;
import io.github.happysnappingturtle.mythicage.guardian.GuardianRegistry;
import io.github.happysnappingturtle.mythicage.village.resident.ResidentEntity;
import io.github.happysnappingturtle.mythicage.village.work.BasicJobTypes;
import io.github.happysnappingturtle.mythicage.village.work.JobType;
import io.github.happysnappingturtle.mythicage.village.work.VillageContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 村庄生活与节庆（T19 设计文档 §1.10，V3-希腊-居民 §5.2–5.7）：通用框架，数据在 {@code village_civ/<civ>.json} 的 {@code life}。
 * <ul>
 *   <li>工作单类型：三餐 {@value #MEAL}、装饰性活动 {@value #LIFE}、打鱼 {@value #FISH}、节庆 {@value #FESTIVAL}；</li>
 *   <li>节庆日的生产让位（{@link #filter}）、口粮倍数、节庆段放宽同时走路上限；</li>
 *   <li>每次调度：祭司差事（{@link PriestDuties}）、节庆的一次性事件（{@link FestivalJob#tick}）。</li>
 * </ul>
 * 全部是表现与交易，不产恩宠（V3 §0）。
 */
public final class VillageLife {
    public static final String MEAL = "meal", LIFE = "life", FISH = "fish", FESTIVAL = "festival";
    /** 节庆段每村同时走路的上限（平时 {@link Scheduler#DEFAULT_MAX_WALKING}）。 */
    public static final int FESTIVAL_WALKING = 12;
    /** 游行、接力提前多少刻开始集合。 */
    public static final int LEAD = 600;
    /** 节庆日在这一刻之前生产让位（宴饮到 13000 结束）。 */
    public static final int FESTIVAL_END = 13000;

    /** 节庆日照常派的工作单类型（文明的行为模块登记，如希腊的祈祷）→ 游行时段是否也派。 */
    private static final Map<String, Boolean> KEEP = new HashMap<>();

    private VillageLife() {
    }

    public static void registerAll() {
        JobType.register(new MealJob());
        JobType.register(new LifeActivityJob());
        JobType.register(new FishJob());
        JobType.register(new FestivalJob());
    }

    /** 文明的行为模块登记：节庆日照常派的工作单类型；{@code duringProcession}：游行时段也派。 */
    public static void keepOnFestival(String type, boolean duringProcession) {
        KEEP.put(type, duringProcession);
    }

    // ───────────── 条件 ─────────────

    /** 主神、特殊日（{@code !tag} 表示不在这天）、阶段都满足。 */
    public static boolean applies(VillageContext ctx, String patron, String special, String minAge) {
        if (!patron.isEmpty() && !patron.equals(ctx.village().patron())) {
            return false;
        }
        if (!special.isEmpty()) {
            boolean not = special.startsWith("!");
            String tag = not ? special.substring(1) : special;
            if (ctx.date().is(tag) == not) {
                return false;
            }
        }
        return FestivalRules.open(minAge, ctx.village().stage());
    }

    /** 名册里、空闲、不在途的居民。 */
    public static List<ResidentEntity> idleMembers(VillageContext ctx) {
        List<ResidentEntity> out = new ArrayList<>();
        for (ResidentEntity r : ctx.residents()) {
            if (r.idle() && !r.inTransit() && ctx.state().roster.containsKey(r.getUUID())) {
                out.add(r);
            }
        }
        return out;
    }

    public static Optional<LifeData.Festival> festivalToday(VillageContext ctx) {
        return FestivalRules.on(ctx.life(), ctx.data().calendar(), ctx.offset(), ctx.today(), ctx.village().stage());
    }

    public static Optional<FestivalRules.Active> festivalNow(VillageContext ctx) {
        return FestivalRules.at(ctx.life(), ctx.data().calendar(), ctx.offset(), ctx.today(), ctx.timeOfDay(),
                ctx.village().stage());
    }

    /** 此刻进行中、或 {@code lead} 刻内开始的节庆段（跨午夜时看明天）。 */
    public static Optional<FestivalRules.Active> festivalSoon(VillageContext ctx, int lead) {
        Optional<FestivalRules.Active> now = festivalNow(ctx);
        if (now.isPresent()) {
            return now;
        }
        int t = ctx.timeOfDay() + lead;
        long day = ctx.today();
        if (t >= 24000) {
            t -= 24000;
            day++;
        }
        return FestivalRules.at(ctx.life(), ctx.data().calendar(), ctx.offset(), day, t, ctx.village().stage());
    }

    public static double rations(VillageContext ctx) {
        return FestivalRules.rations(ctx.life(), ctx.data().calendar(), ctx.offset(), ctx.today(), ctx.village().stage());
    }

    public static int maxWalking(VillageContext ctx) {
        return festivalSoon(ctx, LEAD).isPresent() ? FESTIVAL_WALKING : Scheduler.DEFAULT_MAX_WALKING;
    }

    /**
     * 节庆日 0–13000 的生产让位（§1.10 J5）：只留口粮链、出窑 / 取酒、节庆单、登记过的仪式单（游行时段除外）；
     * 宴饮时段不回家吃饭。平日原样返回。
     */
    public static Optional<Job> filter(VillageContext ctx, Job j) {
        if (ctx.timeOfDay() >= FESTIVAL_END || festivalToday(ctx).isEmpty()) {
            return Optional.of(j);
        }
        if (j.type().equals(FESTIVAL) || j.priority() == Priority.FOOD_SAFETY || j.priority() == Priority.EMERGENCY
                || j.type().equals(BasicJobTypes.COLLECT)) {
            return Optional.of(j);
        }
        String kind = festivalNow(ctx).map(a -> a.phase().kind()).orElse("");
        Boolean keep = KEEP.get(j.type());
        if (keep != null) {
            return keep || !kind.equals("procession") ? Optional.of(j) : Optional.empty();
        }
        if (j.type().equals(MEAL) || j.type().equals(BasicJobTypes.GO_HOME)) {
            return kind.equals("gather") ? Optional.empty() : Optional.of(j);
        }
        return Optional.empty();
    }

    // ───────────── 每次调度 ─────────────

    public static void tick(VillageContext ctx) {
        FestivalJob.tick(ctx);
        PriestDuties.tick(ctx);
    }

    // ───────────── 位置 ─────────────

    /** 游行终点祭坛：离神像站位（没有就离钟）最近的 {@code ritual} 锚点；没有祭坛就是钟。 */
    public static BlockPos altar(VillageContext ctx) {
        BlockPos ref = ctx.anchors("guardian").stream().findFirst().map(VillageContext::block).orElse(ctx.center());
        return ctx.anchors("ritual").stream().map(VillageContext::block)
                .min(Comparator.comparingDouble(p -> p.distSqr(ref))).orElse(ctx.center());
    }

    /** 数据里的地点名：{@code altar}、{@code center}（没有广场锚点就是钟）或锚点名（离钟最近的一个）。 */
    public static Optional<BlockPos> point(VillageContext ctx, String name) {
        if (name.equals("altar")) {
            return Optional.of(altar(ctx));
        }
        BlockPos c = ctx.center();
        Optional<BlockPos> a = ctx.anchors(name).stream().map(VillageContext::block)
                .min(Comparator.comparingDouble(p -> p.distSqr(c)));
        if (a.isEmpty() && name.equals("center")) {
            return Optional.of(c);
        }
        return a;
    }

    /** 能站人：脚下这格与头顶一格空，脚下有地。 */
    static boolean standable(ServerLevel level, BlockPos p) {
        return level.getBlockState(p).getCollisionShape(level, p).isEmpty()
                && level.getBlockState(p.above()).getCollisionShape(level, p.above()).isEmpty()
                && !level.getBlockState(p.below()).getCollisionShape(level, p.below()).isEmpty()
                && level.getFluidState(p).isEmpty();
    }

    /** (x, z) 附近 {@code nearY} 上下 4 格内能站的高度；都不行就按地表高度图。 */
    public static BlockPos ground(ServerLevel level, int x, int z, int nearY) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int d = 0; d <= 4; d++) {
            for (int dy : d == 0 ? new int[]{0} : new int[]{d, -d}) {
                m.set(x, nearY + dy, z);
                if (level.isLoaded(m) && standable(level, m)) {
                    return m.immutable();
                }
            }
        }
        if (level.isLoaded(m.set(x, nearY, z))) {
            return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
        }
        return new BlockPos(x, nearY, z);
    }

    public static BlockPos ground(ServerLevel level, BlockPos p) {
        return ground(level, p.getX(), p.getZ(), p.getY());
    }

    /**
     * 以 {@code c} 为心、{@code r} 为半径、角度 {@code a}（弧度）的落脚点。那里站不了人（水里、墙里）就在附近换半径、
     * 换角度找一处干地（祭坛常在湖岸、广场边有墙）。
     */
    static BlockPos ring(ServerLevel level, BlockPos c, double r, double a) {
        for (double dr : new double[]{0, -1, 1, -2, 2}) {
            for (double da : new double[]{0, 0.3, -0.3, 0.6, -0.6}) {
                double rr = Math.max(1, r + dr);
                BlockPos p = ground(level, (int) Math.floor(c.getX() + 0.5 + Math.cos(a + da) * rr),
                        (int) Math.floor(c.getZ() + 0.5 + Math.sin(a + da) * rr), c.getY());
                if (level.isLoaded(p) && standable(level, p)) {
                    return p;
                }
            }
        }
        return ground(level, (int) Math.floor(c.getX() + 0.5 + Math.cos(a) * r), (int) Math.floor(c.getZ() + 0.5 + Math.sin(a) * r),
                c.getY());
    }

    static Pos pos(BlockPos b) {
        return VillageContext.pos(b);
    }

    // ───────────── 祭司与特效 ─────────────

    /** 本村的祭司（已加载、活着）。 */
    public static Optional<GuardianEntity> priest(VillageContext ctx) {
        return GuardianRegistry.get(ctx.level().getServer()).byBell(ctx.level().dimension(), ctx.center())
                .map(e -> ctx.level().getEntity(e.entityUuid()))
                .filter(e -> e instanceof GuardianEntity g && g.isAlive())
                .map(e -> (GuardianEntity) e);
    }

    /** 祭坛升一缕烟（日常奠酒）。 */
    public static void smoke(ServerLevel level, BlockPos p) {
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, p.getX() + 0.5, p.getY() + 1.2, p.getZ() + 0.5, 4, 0.15, 0.2, 0.15, 0.01);
    }

    /** 圣火大亮（节庆祭品、接力点火）。 */
    public static void blaze(ServerLevel level, BlockPos p) {
        level.sendParticles(ParticleTypes.FLAME, p.getX() + 0.5, p.getY() + 1.2, p.getZ() + 0.5, 40, 0.4, 0.6, 0.4, 0.05);
        level.sendParticles(ParticleTypes.LAVA, p.getX() + 0.5, p.getY() + 1.2, p.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.1);
        level.playSound(null, p, net.minecraft.sounds.SoundEvents.FIRECHARGE_USE, net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 0.9F);
    }
}
