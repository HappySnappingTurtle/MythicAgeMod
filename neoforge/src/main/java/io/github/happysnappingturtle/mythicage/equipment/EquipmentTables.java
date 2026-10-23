package io.github.happysnappingturtle.mythicage.equipment;

import io.github.happysnappingturtle.mythicage.core.equipment.EquipmentTable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文明装备数值表（T17 设计文档 §1）：{@code mythic_age/equipment/<文明>.json}，放在模组资源里而不是数据包里，
 * 因为物品注册早于数据包加载。读不到或格式不对时直接抛错（开发期问题，不能静默跳过）。
 */
public final class EquipmentTables {
    private static final Map<String, EquipmentTable> CACHE = new ConcurrentHashMap<>();

    private EquipmentTables() {
    }

    public static EquipmentTable of(String civ) {
        return CACHE.computeIfAbsent(civ, EquipmentTables::load);
    }

    private static EquipmentTable load(String civ) {
        String path = "/mythic_age/equipment/" + civ + ".json";
        try (InputStream in = EquipmentTables.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("缺少装备数值表 " + path);
            }
            return EquipmentTable.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("读取装备数值表 " + path + " 失败", e);
        }
    }
}
