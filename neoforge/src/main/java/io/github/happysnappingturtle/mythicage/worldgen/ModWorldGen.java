package io.github.happysnappingturtle.mythicage.worldgen;

import io.github.happysnappingturtle.mythicage.content.greek.worldgen.GreekWorldGen;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * 世界生成的汇总（T22 §6.1）：各文明的矿、树、生态点在 {@code content/<文明>/worldgen/} 里，这里只把它们串进数据生成。
 * 以后的文明在三个阶段各加一行。
 */
public final class ModWorldGen {
    private ModWorldGen() {
    }

    public static RegistrySetBuilder builder() {
        return new RegistrySetBuilder()
                .add(Registries.CONFIGURED_FEATURE, GreekWorldGen::configured)
                .add(Registries.PLACED_FEATURE, GreekWorldGen::placed)
                .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, GreekWorldGen::biomeModifiers);
    }
}
