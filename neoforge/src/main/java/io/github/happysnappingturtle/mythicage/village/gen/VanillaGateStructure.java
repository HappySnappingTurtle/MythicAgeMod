package io.github.happysnappingturtle.mythicage.village.gen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.happysnappingturtle.mythicage.config.ModConfigs;
import io.github.happysnappingturtle.mythicage.core.village.gen.GateRules;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;
import java.util.Set;

/**
 * 门控结构（V0 §3.2、W9）：包住一种原版村庄。{@code generateVanillaVillages} 关闭（默认）时一律不生成；
 * 打开时，该点属于已有正式模板的文明地域就让给文明村庄，否则交给原版结构。只影响之后新生成的区块。
 */
public final class VanillaGateStructure extends Structure {
    public static final MapCodec<VanillaGateStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            Structure.CODEC.fieldOf("structure").forGetter(s -> s.wrapped)
    ).apply(i, VanillaGateStructure::new));

    private final Holder<Structure> wrapped;

    public VanillaGateStructure(StructureSettings settings, Holder<Structure> wrapped) {
        super(settings);
        this.wrapped = wrapped;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        boolean generate = ModConfigs.villages().current().generateVanillaVillages();
        GateRules.Decision d = GateRules.decide(generate, generate && regionHasTemplate(context));
        return d == GateRules.Decision.VANILLA ? wrapped.value().findValidGenerationPoint(context) : Optional.empty();
    }

    /** 该候选区块中心属于某个已有正式模板的文明地域。 */
    private static boolean regionHasTemplate(GenerationContext context) {
        Set<String> civs = VillageBuildings.civsWithTemplates();
        var biomes = WorldgenViews.biomes(context.chunkGenerator(), context.randomState());
        int x = context.chunkPos().getMiddleBlockX();
        int z = context.chunkPos().getMiddleBlockZ();
        return civs.stream().anyMatch(civ -> biomes.matches("#mythic_age:region/" + civ, x, z));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.VANILLA_GATE.get();
    }
}
