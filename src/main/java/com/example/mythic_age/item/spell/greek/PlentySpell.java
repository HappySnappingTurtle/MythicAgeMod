package com.example.mythic_age.item.spell.greek;

import com.example.mythic_age.block.ModBlocks;
import com.example.mythic_age.item.GodPowerCertificateItem.MythologyType;
import com.example.mythic_age.item.SpellbookItem.SpellType;
import com.example.mythic_age.item.spell.AbstractSpell;
import com.example.mythic_age.item.spell.SpellCategory;
import com.example.mythic_age.particle.MythicParticleEffects;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class PlentySpell extends AbstractSpell {

    public PlentySpell() {
        super(SpellType.PLENTY, MythologyType.GREEK, SpellCategory.RESOURCE, 400);
    }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        Vec3d direction = caster.getRotationVector();

        BlockPos centerPos = caster.getBlockPos().add(
            (int)(direction.x * 8), -1, (int)(direction.z * 8));

        if (!canPlaceAltarStructure(world, centerPos)) {
            sendMessage(caster, "mythic_age.spellbook.message.plenty.no_space");
            return false;
        }

        buildAbundanceAltar(world, centerPos);

        for (int i = 0; i < 50; i++) {
            double offsetX = (world.random.nextDouble() - 0.5) * 10.0;
            double offsetY = world.random.nextDouble() * 8.0;
            double offsetZ = (world.random.nextDouble() - 0.5) * 10.0;

            MythicParticleEffects.spawnAmbient(world, ParticleTypes.FIREWORK,
                centerPos.getX() + 0.5 + offsetX,
                centerPos.getY() + 1 + offsetY,
                centerPos.getZ() + 0.5 + offsetZ,
                1, 0, 0, 0, 0);
        }

        MythicParticleEffects.spawnUltimate(world, ParticleTypes.EXPLOSION_EMITTER,
            centerPos.getX() + 0.5, centerPos.getY() + 1, centerPos.getZ() + 0.5,
            1, 0, 0, 0, 0);

        MythicParticleEffects.spawnMajor(world, ParticleTypes.ENCHANT,
            centerPos.getX() + 0.5, centerPos.getY() + 1, centerPos.getZ() + 0.5,
            30, 3.0, 2.0, 3.0, 0.2);

        world.playSound(null, centerPos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
            SoundCategory.PLAYERS, 1.5f, 0.8f);
        world.playSound(null, centerPos, SoundEvents.BLOCK_BEACON_ACTIVATE,
            SoundCategory.PLAYERS, 1.0f, 1.2f);

        sendMessage(caster, "mythic_age.spellbook.message.plenty.success");
        sendMessage(caster, "mythic_age.spellbook.message.plenty.inspect_hint");

        return true;
    }

    private boolean canPlaceAltarStructure(ServerWorld world, BlockPos centerPos) {
        int minY = world.getBottomY();
        int maxY = world.getTopY();

        if (centerPos.getY() < minY || centerPos.getY() + 10 > maxY) {
            return false;
        }

        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                BlockPos checkPos = centerPos.add(x, 0, z);
                if (!world.isInBuildLimit(checkPos) || !world.isInBuildLimit(checkPos.add(0, 9, 0))) {
                    return false;
                }
            }
        }

        return true;
    }

    private void buildAbundanceAltar(ServerWorld world, BlockPos centerPos) {
        buildAltarLayer1(world, centerPos);
        buildAltarLayer2(world, centerPos);
        buildAltarLayer3(world, centerPos);
        buildAltarLayer4(world, centerPos);
        buildAltarLayer5(world, centerPos);
        buildAltarLayer6(world, centerPos);
        buildAltarLayer7(world, centerPos);
        buildAltarLayer8(world, centerPos);
        buildAltarLayer9(world, centerPos);
        buildAltarLayer10(world, centerPos);
    }

    private void buildAltarLayer1(ServerWorld world, BlockPos center) {
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                BlockPos pos = center.add(x, 0, z);
                if (Math.abs(x) == 5 && Math.abs(z) == 5) {
                    world.setBlockState(pos, ModBlocks.ABUNDANCE_ALTAR_BRICK_GOLD_CORNER.getDefaultState()
                        .with(HorizontalFacingBlock.FACING, Direction.NORTH));
                } else if (Math.abs(z) == 5 && Math.abs(x) < 5) {
                    world.setBlockState(pos, ModBlocks.ABUNDANCE_ALTAR_BRICK_GOLD_EW.getDefaultState());
                } else if (Math.abs(x) == 5 && Math.abs(z) < 5) {
                    world.setBlockState(pos, ModBlocks.ABUNDANCE_ALTAR_BRICK_GOLD_NS.getDefaultState());
                } else {
                    world.setBlockState(pos, ModBlocks.ABUNDANCE_ALTAR_BRICK.getDefaultState());
                }
            }
        }
    }

    private void buildAltarLayer2(ServerWorld world, BlockPos center) {
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                BlockPos pos = center.add(x, 1, z);
                if (Math.abs(x) == 4 && Math.abs(z) == 4) {
                    world.setBlockState(pos, ModBlocks.ABUNDANCE_ALTAR_PILLAR.getDefaultState());
                } else if (Math.abs(x) == 4 || Math.abs(z) == 4) {
                    world.setBlockState(pos, ModBlocks.ABUNDANCE_ALTAR_WALL_RUNE_BOTTOM.getDefaultState());
                } else {
                    world.setBlockState(pos, ModBlocks.ABUNDANCE_ALTAR_WALL.getDefaultState());
                }
            }
        }
    }

    private void buildAltarLayer3(ServerWorld world, BlockPos center) {
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                BlockPos pos = center.add(x, 2, z);
                if (Math.abs(x) == 4 && Math.abs(z) == 4) {
                    world.setBlockState(pos, ModBlocks.ABUNDANCE_ALTAR_PILLAR.getDefaultState());
                } else if ((x == -3 || x == 3) && (z == -4 || z == 4)) {
                    if (x == -3) {
                        world.setBlockState(pos, ModBlocks.ABUNDANCE_STORAGE_MINERAL.getDefaultState());
                    } else {
                        world.setBlockState(pos, ModBlocks.ABUNDANCE_STORAGE_WOOD.getDefaultState());
                    }
                } else if (x == 0 && (z == -4 || z == 4)) {
                    world.setBlockState(pos, ModBlocks.ABUNDANCE_STORAGE_FOOD.getDefaultState());
                } else if ((x == -4 || x == 4) && z == 0) {
                    world.setBlockState(pos, ModBlocks.ABUNDANCE_STORAGE_FOOD.getDefaultState());
                } else if ((x == -4 || x == 4) && (z == -3 || z == 3)) {
                    world.setBlockState(pos, ModBlocks.ABUNDANCE_STORAGE_MINERAL.getDefaultState());
                } else {
                    world.setBlockState(pos, ModBlocks.ABUNDANCE_ALTAR_WALL.getDefaultState());
                }
            }
        }
    }

    private void buildAltarLayer4(ServerWorld world, BlockPos center) {
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                BlockPos pos = center.add(x, 3, z);
                if (Math.abs(x) == 4 && Math.abs(z) == 4) {
                    world.setBlockState(pos, ModBlocks.ABUNDANCE_ALTAR_PILLAR.getDefaultState());
                } else if (Math.abs(x) == 4 || Math.abs(z) == 4) {
                    world.setBlockState(pos, ModBlocks.ABUNDANCE_ALTAR_WALL_RUNE_TOP.getDefaultState());
                } else {
                    world.setBlockState(pos, ModBlocks.ABUNDANCE_ALTAR_WALL.getDefaultState());
                }
            }
        }
    }

    private void buildAltarLayer5(ServerWorld world, BlockPos center) {
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                BlockPos pos = center.add(x, 4, z);
                world.setBlockState(pos, ModBlocks.ABUNDANCE_ALTAR_BRICK_GOLD_V.getDefaultState());
            }
        }
    }

    private void buildAltarLayer6(ServerWorld world, BlockPos center) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos pos = center.add(x, 5, z);
                if (Math.abs(x) == 2 && Math.abs(z) == 2) {
                    world.setBlockState(pos, ModBlocks.ABUNDANCE_ALTAR_PILLAR.getDefaultState());
                } else if (Math.abs(x) == 2 || Math.abs(z) == 2) {
                    world.setBlockState(pos, ModBlocks.ABUNDANCE_ALTAR_WALL_RUNE_BOTTOM.getDefaultState());
                } else {
                    world.setBlockState(pos, ModBlocks.ABUNDANCE_ALTAR_WALL.getDefaultState());
                }
            }
        }
    }

    private void buildAltarLayer7(ServerWorld world, BlockPos center) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos pos = center.add(x, 6, z);
                if (x == 0 && z == 0) {
                    world.setBlockState(pos, ModBlocks.ABUNDANCE_ALTAR_CORE.getDefaultState());
                } else if (Math.abs(x) == 2 && Math.abs(z) == 2) {
                    world.setBlockState(pos, ModBlocks.ABUNDANCE_ALTAR_PILLAR.getDefaultState());
                } else {
                    world.setBlockState(pos, ModBlocks.ABUNDANCE_ALTAR_WALL.getDefaultState());
                }
            }
        }
    }

    private void buildAltarLayer8(ServerWorld world, BlockPos center) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos pos = center.add(x, 7, z);
                if (x == 0 && z == 0) {
                    world.setBlockState(pos, ModBlocks.ABUNDANCE_ALTAR_WALL.getDefaultState());
                } else if (Math.abs(x) == 2 && Math.abs(z) == 2) {
                    world.setBlockState(pos, ModBlocks.ABUNDANCE_ALTAR_PILLAR.getDefaultState());
                } else if (Math.abs(x) == 2 || Math.abs(z) == 2) {
                    world.setBlockState(pos, ModBlocks.ABUNDANCE_ALTAR_WALL_RUNE_TOP.getDefaultState());
                } else {
                    world.setBlockState(pos, ModBlocks.ABUNDANCE_ALTAR_WALL.getDefaultState());
                }
            }
        }
    }

    private void buildAltarLayer9(ServerWorld world, BlockPos center) {
        for (int x = -2; x <= 2; x++) {
            world.setBlockState(center.add(x, 8, -1), ModBlocks.ABUNDANCE_ALTAR_STAIRS.getDefaultState()
                .with(StairsBlock.FACING, Direction.NORTH)
                .with(StairsBlock.HALF, BlockHalf.BOTTOM));
        }

        for (int x = -2; x <= 2; x++) {
            world.setBlockState(center.add(x, 8, 1), ModBlocks.ABUNDANCE_ALTAR_STAIRS.getDefaultState()
                .with(StairsBlock.FACING, Direction.SOUTH)
                .with(StairsBlock.HALF, BlockHalf.BOTTOM));
        }

        for (int x = -2; x <= 2; x++) {
            world.setBlockState(center.add(x, 8, 0), ModBlocks.ABUNDANCE_ALTAR_WALL.getDefaultState());
        }
    }

    private void buildAltarLayer10(ServerWorld world, BlockPos center) {
        for (int x = -1; x <= 1; x++) {
            world.setBlockState(center.add(x, 9, 0), ModBlocks.ABUNDANCE_ALTAR_WALL.getDefaultState());
        }

        world.setBlockState(center.add(2, 9, 0), ModBlocks.ABUNDANCE_ALTAR_SLAB.getDefaultState()
            .with(SlabBlock.TYPE, SlabType.BOTTOM));
        world.setBlockState(center.add(-2, 9, 0), ModBlocks.ABUNDANCE_ALTAR_SLAB.getDefaultState()
            .with(SlabBlock.TYPE, SlabType.BOTTOM));
    }
}
