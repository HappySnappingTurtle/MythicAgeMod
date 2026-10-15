package io.github.happysnappingturtle.mythicage.trade;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.core.trade.TradeTable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * 交易表（T9 设计文档 §4）：读取数据包 {@code data/<ns>/mythic_age/trade/<文明>.json}，文件名就是文明 id。
 * 解析失败时记错误日志并保留上一版有效的表（12 号 §5）；首次就失败时该文明的表为空。
 */
public final class TradeTableLoader extends SimpleJsonResourceReloadListener {
    private static volatile Map<Civilization, TradeTable> tables = Map.of();
    @Nullable
    private static volatile TradeTable testOverride;

    private TradeTableLoader() {
        super(new Gson(), "mythic_age/trade");
    }

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new TradeTableLoader());
    }

    public static TradeTable table(Civilization civ) {
        TradeTable o = testOverride;
        if (o != null && o.civ() == civ) {
            return o;
        }
        return tables.getOrDefault(civ, TradeTable.empty(civ));
    }

    /** 物品注册表查询：已注册时返回默认最大堆叠。 */
    public static final TradeTable.ItemLookup ITEMS = id -> {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null ? OptionalInt.empty() : BuiltInRegistries.ITEM.getOptional(rl)
                .map(item -> OptionalInt.of(item.getDefaultMaxStackSize())).orElse(OptionalInt.empty());
    };

    /** 只供 GameTest 使用：临时替换某个文明的表。 */
    public static void overrideForTest(@Nullable TradeTable table) {
        testOverride = table;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<Civilization, TradeTable> next = new EnumMap<>(Civilization.class);
        next.putAll(tables);
        files.forEach((id, json) -> {
            Optional<Civilization> civ = Civilization.byId(id.getPath());
            if (civ.isEmpty()) {
                MythicAge.LOGGER.error("交易表 {}：文件名不是文明 id，已忽略", id);
                return;
            }
            try {
                TradeTable t = TradeTable.parse(json, ITEMS);
                if (t.civ() != civ.get()) {
                    throw new TradeTable.TradeTableException("civilization: 与文件名 " + id.getPath() + " 不一致");
                }
                next.put(civ.get(), t);
            } catch (TradeTable.TradeTableException e) {
                MythicAge.LOGGER.error("交易表 {} 解析失败，保留上一版有效的表：{}", id, e.getMessage());
            }
        });
        tables = Map.copyOf(next);
    }
}
