package com.example.mythic_age.item.spell.greek;

import com.example.mythic_age.block.AnimalLureTowerBlock;
import com.example.mythic_age.block.ModBlocks;
import com.example.mythic_age.item.GodPowerCertificateItem.MythologyType;
import com.example.mythic_age.item.SpellbookItem.SpellType;
import com.example.mythic_age.item.spell.AbstractSpell;
import com.example.mythic_age.item.spell.SpellCategory;
import com.example.mythic_age.particle.MythicParticleEffects;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class LureSpell extends AbstractSpell {

    public LureSpell() {
        super(SpellType.LURE, MythologyType.GREEK, SpellCategory.UTILITY, 100);
    }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        BlockPos lurePos = targetPos.up();

        if (!canPlaceLureTower(world, lurePos)) {
            sendMessage(caster, "mythic_age.spellbook.message.lure.no_space");
            return false;
        }

        buildAnimalLureTower(world, lurePos, caster);

        for (int i = 0; i < 30; i++) {
            double angle = (i / 30.0) * 2 * Math.PI;
            double radius = 3.0;
            double x = lurePos.getX() + 0.5 + Math.cos(angle) * radius;
            double z = lurePos.getZ() + 0.5 + Math.sin(angle) * radius;

            MythicParticleEffects.spawnAmbient(world, ParticleTypes.HEART,
                x, lurePos.getY() + 1.5, z, 1, 0, 0, 0, 0);
        }

        MythicParticleEffects.spawnNormal(world, ParticleTypes.ENCHANT,
            lurePos.getX() + 0.5, lurePos.getY() + 1.5, lurePos.getZ() + 0.5,
            20, 1.0, 1.5, 1.0, 0.2);

        world.playSound(null, lurePos, SoundEvents.BLOCK_BEACON_ACTIVATE,
            SoundCategory.BLOCKS, 1.0f, 1.2f);
        world.playSound(null, lurePos, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
            SoundCategory.NEUTRAL, 1.5f, 0.8f);

        sendMessage(caster, "mythic_age.spellbook.message.lure.success");
        return true;
    }

    private boolean canPlaceLureTower(ServerWorld world, BlockPos basePos) {
        BlockPos[] crossPositions = {
            new BlockPos(0, 0, 0),
            new BlockPos(1, 0, 0),
            new BlockPos(-1, 0, 0),
            new BlockPos(0, 0, 1),
            new BlockPos(0, 0, -1)
        };

        for (int layer = 0; layer < 5; layer++) {
            BlockPos layerPos = basePos.up(layer);
            if (layer == 4) {
                if (!world.getBlockState(layerPos).isAir()) {
                    return false;
                }
            } else {
                for (BlockPos relPos : crossPositions) {
                    BlockPos checkPos = layerPos.add(relPos);
                    if (!world.getBlockState(checkPos).isAir()) {
                        return false;
                    }
                }
            }
        }

        for (BlockPos relPos : crossPositions) {
            BlockPos groundPos = basePos.down().add(relPos);
            if (!world.getBlockState(groundPos).isSolidBlock(world, groundPos)) {
                return false;
            }
        }

        return true;
    }

    private void buildAnimalLureTower(ServerWorld world, BlockPos basePos, PlayerEntity player) {
        BlockPos[] crossPositions = {
            new BlockPos(0, 0, 0),
            new BlockPos(1, 0, 0),
            new BlockPos(-1, 0, 0),
            new BlockPos(0, 0, 1),
            new BlockPos(0, 0, -1)
        };

        Direction towerFacing = player.getHorizontalFacing().getOpposite();

        BlockPos coreRelativePos = new BlockPos(
            towerFacing.getOffsetX(), 0, towerFacing.getOffsetZ());

        for (BlockPos relPos : crossPositions) {
            world.setBlockState(basePos.add(relPos), Blocks.MOSSY_COBBLESTONE.getDefaultState());
        }

        for (BlockPos relPos : crossPositions) {
            world.setBlockState(basePos.up().add(relPos), Blocks.MOSSY_COBBLESTONE.getDefaultState());
        }

        for (BlockPos relPos : crossPositions) {
            BlockPos targetPos = basePos.up(2).add(relPos);
            if (relPos.equals(coreRelativePos)) {
                world.setBlockState(targetPos,
                    ModBlocks.ANIMAL_LURE_TOWER.getDefaultState()
                        .with(AnimalLureTowerBlock.FACING, towerFacing));
            } else {
                world.setBlockState(targetPos, Blocks.MOSSY_COBBLESTONE.getDefaultState());
            }
        }

        for (BlockPos relPos : crossPositions) {
            world.setBlockState(basePos.up(3).add(relPos), Blocks.MOSSY_COBBLESTONE.getDefaultState());
        }

        world.setBlockState(basePos.up(4), Blocks.MOSSY_COBBLESTONE.getDefaultState());
    }
}
