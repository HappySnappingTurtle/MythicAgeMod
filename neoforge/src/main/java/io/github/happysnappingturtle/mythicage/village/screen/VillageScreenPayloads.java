package io.github.happysnappingturtle.mythicage.village.screen;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import io.github.happysnappingturtle.mythicage.village.economy.AffairsView;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

/**
 * 村庄界面（GA-12）的网络负载。各分页沿用已有的视图：仓库与发展记录用 {@code AffairsPayloads.View}，委托用
 * {@code QuestPayloads.View}，建设的候选用 {@code ProjectPayloads.View}；这里是新增的状态栏、地图、村民、信仰、建筑详情
 * 与几项操作。
 */
public final class VillageScreenPayloads {
    public static final int TAB_MAP = 0;
    public static final int TAB_BUILD = 1;
    public static final int TAB_PEOPLE = 2;
    public static final int TAB_STOCK = 3;
    public static final int TAB_QUESTS = 4;
    public static final int TAB_FAITH = 5;
    public static final int TABS = 6;
    /** 祭司对话页的「村务」按钮：用当前祭司会话所在的村。 */
    public static final UUID FROM_PRIEST = new UUID(0, 0);

    private VillageScreenPayloads() {
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> payloadType(String path) {
        return new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MythicAgeIds.MOD_ID, path));
    }

    /**
     * 状态栏（§2）与木牌上的标记。
     *
     * @param civ         文明 id（地图配色按它取）
     * @param title       村名
     * @param stage       时代 id
     * @param patron      主神 id；没有为空串
     * @param templeLevel 神庙等级；tierKey 等级名语言键（没有神庙为空串）
     * @param alert       最急的一条警报；没有时 key 为空串
     * @param project     在建项目名（文本行）；空闲时 key 为空串
     * @param progress    在建进度百分比；空闲为 −1
     * @param food        口粮份数；floor 底线；没有口粮规则时 floor 为 −1
     * @param quests      进行中的委托数
     * @param alertTabs   哪些分页有警报（按分页号的位）
     * @param atKeeper    这名玩家在谷仓管事身边（交付、捐献主手物品要在管事身边）
     */
    public record Header(String civ, AffairsView.Line title, String stage, String patron, int templeLevel, String tierKey, AffairsView.Line alert,
                         AffairsView.Line project, int progress, int food, int floor, int quests, int alertTabs,
                         boolean atKeeper) {
        public static final StreamCodec<FriendlyByteBuf, Header> CODEC = StreamCodec.of(
                (buf, h) -> {
                    buf.writeUtf(h.civ);
                    AffairsView.LINE.encode(buf, h.title);
                    buf.writeUtf(h.stage);
                    buf.writeUtf(h.patron);
                    buf.writeVarInt(h.templeLevel);
                    buf.writeUtf(h.tierKey);
                    AffairsView.LINE.encode(buf, h.alert);
                    AffairsView.LINE.encode(buf, h.project);
                    buf.writeVarInt(h.progress + 1);
                    buf.writeVarInt(h.food);
                    buf.writeVarInt(h.floor + 1);
                    buf.writeVarInt(h.quests);
                    buf.writeVarInt(h.alertTabs);
                    buf.writeBoolean(h.atKeeper);
                }, buf -> new Header(buf.readUtf(), AffairsView.LINE.decode(buf), buf.readUtf(), buf.readUtf(), buf.readVarInt(), buf.readUtf(),
                        AffairsView.LINE.decode(buf), AffairsView.LINE.decode(buf), buf.readVarInt() - 1, buf.readVarInt(),
                        buf.readVarInt() - 1, buf.readVarInt(), buf.readVarInt(), buf.readBoolean()));

        public boolean alerted(int tab) {
            return (alertTabs & (1 << tab)) != 0;
        }
    }

    /** 服务端 → 客户端：打开村庄界面（或在已开的界面里切到 tab），带状态栏。 */
    public record Opened(UUID village, int tab, Header header) implements CustomPacketPayload {
        public static final Type<Opened> TYPE = payloadType("village_screen_opened");
        public static final StreamCodec<FriendlyByteBuf, Opened> STREAM_CODEC = StreamCodec.of(
                (buf, o) -> {
                    buf.writeUUID(o.village);
                    buf.writeVarInt(o.tab);
                    Header.CODEC.encode(buf, o.header);
                }, buf -> new Opened(buf.readUUID(), buf.readVarInt(), Header.CODEC.decode(buf)));

        @Override
        public Type<Opened> type() {
            return TYPE;
        }
    }

    /** 服务端 → 客户端：状态栏变了。 */
    public record HeaderUpdate(UUID village, Header header) implements CustomPacketPayload {
        public static final Type<HeaderUpdate> TYPE = payloadType("village_screen_header");
        public static final StreamCodec<FriendlyByteBuf, HeaderUpdate> STREAM_CODEC = StreamCodec.of(
                (buf, h) -> {
                    buf.writeUUID(h.village);
                    Header.CODEC.encode(buf, h.header);
                }, buf -> new HeaderUpdate(buf.readUUID(), Header.CODEC.decode(buf)));

        @Override
        public Type<HeaderUpdate> type() {
            return TYPE;
        }
    }

    /** 服务端 → 客户端：地图。full 为真时 data 是 {@code VillageMapData} 全量，否则是相对上一次的 {@code MapDelta}。 */
    public record MapPacket(UUID village, boolean full, int centerX, int centerZ, byte[] data) implements CustomPacketPayload {
        public static final Type<MapPacket> TYPE = payloadType("village_screen_map");
        public static final StreamCodec<FriendlyByteBuf, MapPacket> STREAM_CODEC = StreamCodec.of(
                (buf, m) -> {
                    buf.writeUUID(m.village);
                    buf.writeBoolean(m.full);
                    buf.writeVarInt(m.centerX);
                    buf.writeVarInt(m.centerZ);
                    buf.writeByteArray(m.data);
                }, buf -> new MapPacket(buf.readUUID(), buf.readBoolean(), buf.readVarInt(), buf.readVarInt(), buf.readByteArray(64 * 1024)));

        @Override
        public Type<MapPacket> type() {
            return TYPE;
        }
    }

    /** 岗位：职业语言键、在岗人数、岗位数、提供这个岗位的建筑（地图建筑键，点岗位行时在地图上高亮）。 */
    public record Job(String key, int have, int slots, List<String> buildings) {
    }

    /** 服务端 → 客户端：村民分页（§3.3）。migration 是移民进度一行。 */
    public record People(UUID village, int residents, int beds, int soldiers, int soldierCap, List<Job> jobs,
                         AffairsView.Line migration, List<AffairsView.Count> collaborators) implements CustomPacketPayload {
        public static final Type<People> TYPE = payloadType("village_screen_people");
        private static final StreamCodec<FriendlyByteBuf, Job> JOB = StreamCodec.of(
                (buf, j) -> {
                    buf.writeUtf(j.key);
                    buf.writeVarInt(j.have);
                    buf.writeVarInt(j.slots);
                    buf.writeCollection(j.buildings, FriendlyByteBuf::writeUtf);
                }, buf -> new Job(buf.readUtf(), buf.readVarInt(), buf.readVarInt(), buf.readList(FriendlyByteBuf::readUtf)));
        private static final StreamCodec<FriendlyByteBuf, AffairsView.Count> COUNT = StreamCodec.of(
                (buf, c) -> {
                    buf.writeUtf(c.key());
                    buf.writeBoolean(c.literal());
                    buf.writeVarInt(c.n());
                }, buf -> new AffairsView.Count(buf.readUtf(), buf.readBoolean(), buf.readVarInt()));
        public static final StreamCodec<FriendlyByteBuf, People> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeUUID(p.village);
                    buf.writeVarInt(p.residents);
                    buf.writeVarInt(p.beds);
                    buf.writeVarInt(p.soldiers);
                    buf.writeVarInt(p.soldierCap);
                    buf.writeCollection(p.jobs, JOB);
                    AffairsView.LINE.encode(buf, p.migration);
                    buf.writeCollection(p.collaborators, COUNT);
                }, buf -> new People(buf.readUUID(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                        buf.readList(JOB), AffairsView.LINE.decode(buf), buf.readList(COUNT)));

        @Override
        public Type<People> type() {
            return TYPE;
        }
    }

    /**
     * 服务端 → 客户端：信仰分页（§3.6，GA-13 的内容）。
     *
     * @param present     有神庙（或奠基石祭坛）
     * @param integrity   完好度百分比；coefficient 系数
     * @param shrines     次神祠：神 id → 已建成
     * @param devotee     这名玩家是这里的供奉者；devotees 供奉者人数
     * @param believers   今天在这里为你祈祷的信徒数；favor 今天在这里得到的恩宠
     * @param offered     今天献过供；offerBonus 献供加成百分比
     * @param relics      你放在这里的圣物；relicCap 上限
     */
    public record Faith(UUID village, boolean present, int level, String tierKey, int integrity, double coefficient, String patron,
                        List<Shrine> shrines, boolean devotee, int devotees, int believers, double favor, boolean offered,
                        int offerBonus, List<ItemStack> relics, int relicCap) implements CustomPacketPayload {
        public static final Type<Faith> TYPE = payloadType("village_screen_faith");

        public record Shrine(String god, boolean built) {
        }

        private static final StreamCodec<FriendlyByteBuf, Shrine> SHRINE = StreamCodec.of(
                (buf, s) -> {
                    buf.writeUtf(s.god);
                    buf.writeBoolean(s.built);
                }, buf -> new Shrine(buf.readUtf(), buf.readBoolean()));
        public static final StreamCodec<RegistryFriendlyByteBuf, Faith> STREAM_CODEC = StreamCodec.of(
                (buf, f) -> {
                    buf.writeUUID(f.village);
                    buf.writeBoolean(f.present);
                    buf.writeVarInt(f.level);
                    buf.writeUtf(f.tierKey);
                    buf.writeVarInt(f.integrity);
                    buf.writeDouble(f.coefficient);
                    buf.writeUtf(f.patron);
                    buf.writeCollection(f.shrines, SHRINE);
                    buf.writeBoolean(f.devotee);
                    buf.writeVarInt(f.devotees);
                    buf.writeVarInt(f.believers);
                    buf.writeDouble(f.favor);
                    buf.writeBoolean(f.offered);
                    buf.writeVarInt(f.offerBonus);
                    ItemStack.OPTIONAL_LIST_STREAM_CODEC.encode(buf, f.relics);
                    buf.writeVarInt(f.relicCap);
                }, buf -> new Faith(buf.readUUID(), buf.readBoolean(), buf.readVarInt(), buf.readUtf(), buf.readVarInt(),
                        buf.readDouble(), buf.readUtf(), buf.readList(SHRINE), buf.readBoolean(), buf.readVarInt(), buf.readVarInt(),
                        buf.readDouble(), buf.readBoolean(), buf.readVarInt(), ItemStack.OPTIONAL_LIST_STREAM_CODEC.decode(buf),
                        buf.readVarInt()));

        @Override
        public Type<Faith> type() {
            return TYPE;
        }
    }

    /**
     * 服务端 → 客户端：地图上选中的建筑或空地块的详情（§3.1 详情栏）。
     *
     * @param key           建筑键；空地块为 {@code plot:<id>}
     * @param lines         详情行（床位与住户、岗位、等级……）
     * @param upgradeKey    能升级时对应的候选键（点名用）；不能为空串
     * @param upgradeReason 候选不可用的原因（语言键后缀）；可用为空串
     * @param options       空地块：这里能盖的候选键（建筑名行）
     */
    public record Detail(UUID village, String key, List<AffairsView.Line> lines, String upgradeKey, String upgradeReason,
                         List<AffairsView.Line> options) implements CustomPacketPayload {
        public static final Type<Detail> TYPE = payloadType("village_screen_detail");
        public static final StreamCodec<FriendlyByteBuf, Detail> STREAM_CODEC = StreamCodec.of(
                (buf, d) -> {
                    buf.writeUUID(d.village);
                    buf.writeUtf(d.key);
                    buf.writeCollection(d.lines, AffairsView.LINE);
                    buf.writeUtf(d.upgradeKey);
                    buf.writeUtf(d.upgradeReason);
                    buf.writeCollection(d.options, AffairsView.LINE);
                }, buf -> new Detail(buf.readUUID(), buf.readUtf(), buf.readList(AffairsView.LINE), buf.readUtf(), buf.readUtf(),
                        buf.readList(AffairsView.LINE)));

        @Override
        public Type<Detail> type() {
            return TYPE;
        }
    }

    // ───────────── 客户端 → 服务端 ─────────────

    /** 打开或切换分页；village 为 {@link #FROM_PRIEST} 时用祭司会话所在的村。 */
    public record Open(UUID village, int tab) implements CustomPacketPayload {
        public static final Type<Open> TYPE = payloadType("village_screen_open");
        public static final StreamCodec<FriendlyByteBuf, Open> STREAM_CODEC = StreamCodec.of(
                (buf, o) -> {
                    buf.writeUUID(o.village);
                    buf.writeVarInt(o.tab);
                }, buf -> new Open(buf.readUUID(), buf.readVarInt()));

        @Override
        public Type<Open> type() {
            return TYPE;
        }
    }

    /** 关掉界面：服务端停止发增量。 */
    public record Close(UUID village) implements CustomPacketPayload {
        public static final Type<Close> TYPE = payloadType("village_screen_close");
        public static final StreamCodec<FriendlyByteBuf, Close> STREAM_CODEC = StreamCodec.of(
                (buf, c) -> buf.writeUUID(c.village), buf -> new Close(buf.readUUID()));

        @Override
        public Type<Close> type() {
            return TYPE;
        }
    }

    /** 要地图上一处的详情（建筑键或 {@code plot:<id>}）。 */
    public record DetailRequest(UUID village, String key) implements CustomPacketPayload {
        public static final Type<DetailRequest> TYPE = payloadType("village_screen_detail_request");
        public static final StreamCodec<FriendlyByteBuf, DetailRequest> STREAM_CODEC = StreamCodec.of(
                (buf, d) -> {
                    buf.writeUUID(d.village);
                    buf.writeUtf(d.key);
                }, buf -> new DetailRequest(buf.readUUID(), buf.readUtf()));

        @Override
        public Type<DetailRequest> type() {
            return TYPE;
        }
    }

    /**
     * 操作：{@code donate_missing}（把背包里项目正好缺的材料捐进村库）、{@code relic_offer}（主手圣物放进这里的供奉位）、
     * {@code relic_retrieve}（取回第 arg 件）、{@code leave}（不再供奉此村）、{@code show_blocked}（在世界里标出被挡格）。
     */
    public record Act(UUID village, String action, int arg) implements CustomPacketPayload {
        public static final String DONATE_MISSING = "donate_missing";
        public static final String RELIC_OFFER = "relic_offer";
        public static final String RELIC_RETRIEVE = "relic_retrieve";
        public static final String LEAVE = "leave";
        public static final String SHOW_BLOCKED = "show_blocked";
        public static final Type<Act> TYPE = payloadType("village_screen_act");
        public static final StreamCodec<FriendlyByteBuf, Act> STREAM_CODEC = StreamCodec.of(
                (buf, a) -> {
                    buf.writeUUID(a.village);
                    buf.writeUtf(a.action);
                    buf.writeVarInt(a.arg);
                }, buf -> new Act(buf.readUUID(), buf.readUtf(), buf.readVarInt()));

        @Override
        public Type<Act> type() {
            return TYPE;
        }
    }
}
