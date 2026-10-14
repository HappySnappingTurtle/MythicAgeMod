package io.github.happysnappingturtle.mythicage.content.greek.agri;

import io.github.happysnappingturtle.mythicage.content.greek.GreekAgriculture;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.Map;

/** 希腊农业的事件挂接（T6b）：盆栽、可燃性、告示牌方块实体、斧头去皮。 */
public final class GreekAgricultureEvents {
    private GreekAgricultureEvents() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(GreekAgricultureEvents::onCommonSetup);
        modBus.addListener(GreekAgricultureEvents::onBlockEntityTypes);
        NeoForge.EVENT_BUS.addListener(GreekAgricultureEvents::onToolModify);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ((FlowerPotBlock) Blocks.FLOWER_POT).addPlant(GreekAgriculture.OLIVE_SAPLING.getId(),
                    GreekAgriculture.POTTED_OLIVE_SAPLING);
            FireBlock fire = (FireBlock) Blocks.FIRE;
            for (Block log : new Block[]{GreekAgriculture.OLIVE_LOG.get(), GreekAgriculture.OLIVE_WOOD.get(),
                    GreekAgriculture.STRIPPED_OLIVE_LOG.get(), GreekAgriculture.STRIPPED_OLIVE_WOOD.get()}) {
                fire.setFlammable(log, 5, 5);
            }
            fire.setFlammable(GreekAgriculture.OLIVE_PLANKS.get(), 5, 20);
            fire.setFlammable(GreekAgriculture.OLIVE_LEAVES.get(), 30, 60);
            fire.setFlammable(GreekAgriculture.GRAPE_TRELLIS.get(), 5, 20);
            fire.setFlammable(GreekAgriculture.WILD_GRAPEVINE.get(), 60, 100);
        });
    }

    private static void onBlockEntityTypes(BlockEntityTypeAddBlocksEvent event) {
        event.modify(BlockEntityType.SIGN, GreekAgriculture.OLIVE_SIGN.get(), GreekAgriculture.OLIVE_WALL_SIGN.get());
        event.modify(BlockEntityType.HANGING_SIGN, GreekAgriculture.OLIVE_HANGING_SIGN.get(),
                GreekAgriculture.OLIVE_WALL_HANGING_SIGN.get());
    }

    /** 斧头去皮：橄榄原木 → 去皮原木，橄榄木 → 去皮木，保持轴向。 */
    private static void onToolModify(BlockEvent.BlockToolModificationEvent event) {
        if (event.getItemAbility() != ItemAbilities.AXE_STRIP) {
            return;
        }
        BlockState state = event.getState();
        for (Map.Entry<Block, Block> e : GreekAgriculture.strippables()) {
            if (state.is(e.getKey())) {
                event.setFinalState(e.getValue().defaultBlockState()
                        .setValue(RotatedPillarBlock.AXIS, state.getValue(RotatedPillarBlock.AXIS)));
                return;
            }
        }
    }
}
