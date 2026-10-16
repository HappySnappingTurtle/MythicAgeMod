package io.github.happysnappingturtle.mythicage.faith;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.github.happysnappingturtle.mythicage.MythicAge;
import io.github.happysnappingturtle.mythicage.core.faith.FaithProfile;
import io.github.happysnappingturtle.mythicage.core.player.Civilization;
import io.github.happysnappingturtle.mythicage.trade.TradeTableLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 文明档案（T10b 设计文档 §2）：数据包 {@code data/<ns>/mythic_age/faith/<文明>.json}，文件名就是文明 id。
 * 解析失败时记错误日志并保留上一版；没有档案的文明视为未开放（不生成祭司、不能交易）。
 */
public final class FaithProfiles extends SimpleJsonResourceReloadListener {
    private static volatile Map<Civilization, FaithProfile> profiles = Map.of();
    private static final Map<Civilization, FaithProfile> TEST = new EnumMap<>(Civilization.class);

    private FaithProfiles() {
        super(new Gson(), "mythic_age/faith");
    }

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new FaithProfiles());
    }

    /** 该文明的档案；未开放的文明为空。 */
    public static Optional<FaithProfile> get(Civilization civ) {
        synchronized (TEST) {
            FaithProfile t = TEST.get(civ);
            if (t != null) {
                return Optional.of(t);
            }
        }
        return Optional.ofNullable(profiles.get(civ));
    }

    /** 已开放的文明。 */
    public static boolean open(Civilization civ) {
        return get(civ).isPresent();
    }

    /** 只供 GameTest：临时设定某文明的档案；profile 为 null 时清除该文明的临时档案。 */
    public static void overrideForTest(Civilization civ, @Nullable FaithProfile profile) {
        synchronized (TEST) {
            if (profile == null) {
                TEST.remove(civ);
            } else {
                TEST.put(civ, profile);
            }
        }
    }

    public static void clearTestOverrides() {
        synchronized (TEST) {
            TEST.clear();
        }
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<Civilization, FaithProfile> next = new EnumMap<>(Civilization.class);
        next.putAll(profiles);
        files.forEach((id, json) -> {
            Optional<Civilization> civ = Civilization.byId(id.getPath());
            if (civ.isEmpty()) {
                MythicAge.LOGGER.error("文明档案 {}：文件名不是文明 id，已忽略", id);
                return;
            }
            try {
                List<String> warnings = new ArrayList<>();
                FaithProfile p = FaithProfile.parse(json, TradeTableLoader.ITEMS::maxStack, warnings);
                if (p.civ() != civ.get()) {
                    throw new FaithProfile.FaithProfileException("civilization: 与文件名 " + id.getPath() + " 不一致");
                }
                warnings.forEach(w -> MythicAge.LOGGER.warn("文明档案 {}：{}", id, w));
                next.put(civ.get(), p);
            } catch (FaithProfile.FaithProfileException e) {
                MythicAge.LOGGER.error("文明档案 {} 解析失败，保留上一版：{}", id, e.getMessage());
            }
        });
        profiles = Map.copyOf(next);
    }
}
