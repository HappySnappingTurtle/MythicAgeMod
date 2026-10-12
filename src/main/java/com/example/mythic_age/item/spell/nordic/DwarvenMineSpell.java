package com.example.mythic_age.item.spell.nordic;

import com.example.mythic_age.item.GodPowerCertificateItem.MythologyType;
import com.example.mythic_age.item.SpellbookItem.SpellType;
import com.example.mythic_age.item.spell.AbstractSpell;
import com.example.mythic_age.item.spell.SpellCategory;
import com.example.mythic_age.particle.MythicParticleEffects;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class DwarvenMineSpell extends AbstractSpell {
    public DwarvenMineSpell() { super(SpellType.DWARVEN_MINE, MythologyType.NORDIC, SpellCategory.RESOURCE, 200); }

    // Store random shape parameters for each mine hill
    private HillShape currentHillShape;

    private static class HillShape {
        final double radiusX, radiusZ;
        final int maxHeight;
        final double[] heightNoise;
        final double rotation;

        HillShape(net.minecraft.util.math.random.Random random) {
            // Random ellipse radii (4-8 blocks)
            this.radiusX = 4.0 + random.nextDouble() * 4.0;
            this.radiusZ = 4.0 + random.nextDouble() * 4.0;

            // Random height (3-6 layers)
            this.maxHeight = 3 + random.nextInt(4);

            // Random rotation angle
            this.rotation = random.nextDouble() * Math.PI * 2;

            // Generate height noise array for irregular shape
            this.heightNoise = new double[16 * 16];
            for (int i = 0; i < heightNoise.length; i++) {
                heightNoise[i] = 0.7 + random.nextDouble() * 0.6; // 0.7-1.3 random coefficient
            }
        }

        // Get height multiplier at a given position
        double getHeightMultiplier(int x, int z) {
            int index = Math.abs((x + 8) * 16 + (z + 8)) % heightNoise.length;
            return heightNoise[index];
        }
    }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        // Play initial sound effects - earth rumbling
        world.playSound(null, targetPos, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.0f, 0.5f);
        world.playSound(null, targetPos, SoundEvents.BLOCK_GRAVEL_BREAK, SoundCategory.PLAYERS, 2.0f, 0.8f);

        // Create initial dust particle effects
        for (int i = 0; i < 30; i++) {
            double offsetX = (world.random.nextDouble() - 0.5) * 12.0;
            double offsetZ = (world.random.nextDouble() - 0.5) * 12.0;
            double offsetY = world.random.nextDouble() * 3.0;

            MythicParticleEffects.spawnAmbient(world, ParticleTypes.POOF,
                targetPos.getX() + 0.5 + offsetX,
                targetPos.getY() + offsetY,
                targetPos.getZ() + 0.5 + offsetZ,
                1, 0.2, 0.2, 0.2, 0.1);

            MythicParticleEffects.spawnAmbient(world, ParticleTypes.LARGE_SMOKE,
                targetPos.getX() + 0.5 + offsetX,
                targetPos.getY() + offsetY,
                targetPos.getZ() + 0.5 + offsetZ,
                1, 0.1, 0.1, 0.1, 0.05);
        }

        // Build the mine hill in 5 stages, all delayed effects run on server tick main thread
        for (int stage = 1; stage <= 5; stage++) {
            final int currentStage = stage;
            scheduleTask(world, (stage - 1) * 10, () -> {
                buildMineHillStage(world, targetPos, currentStage);

                world.playSound(null, targetPos, SoundEvents.BLOCK_STONE_PLACE, SoundCategory.PLAYERS,
                    0.8f, 0.6f + currentStage * 0.1f);

                for (int i = 0; i < 15; i++) {
                    double offsetX = (world.random.nextDouble() - 0.5) * 8.0;
                    double offsetZ = (world.random.nextDouble() - 0.5) * 8.0;
                    double offsetY = world.random.nextDouble() * 2.0;

                    MythicParticleEffects.spawnAmbient(world, ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        targetPos.getX() + 0.5 + offsetX,
                        targetPos.getY() + currentStage + offsetY,
                        targetPos.getZ() + 0.5 + offsetZ,
                        1, 0.1, 0.1, 0.1, 0.02);
                }
            });
        }

        // Final completion stage
        scheduleTask(world, 50, () -> {
            world.playSound(null, targetPos, SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 1.5f, 1.2f);
            world.playSound(null, targetPos, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0f, 0.8f);

            for (int i = 0; i < 40; i++) {
                double offsetX = (world.random.nextDouble() - 0.5) * 10.0;
                double offsetZ = (world.random.nextDouble() - 0.5) * 10.0;
                double offsetY = world.random.nextDouble() * 6.0;

                MythicParticleEffects.spawnNormal(world, ParticleTypes.FIREWORK,
                    targetPos.getX() + 0.5 + offsetX,
                    targetPos.getY() + 3 + offsetY,
                    targetPos.getZ() + 0.5 + offsetZ,
                    1, 0, 0, 0, 0);
            }

            caster.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 12000, 2));
            caster.addStatusEffect(new StatusEffectInstance(StatusEffects.LUCK, 12000, 2));
            caster.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 12000, 0));

            sendMessage(caster, "mythic_age.spellbook.message.dwarven_mine.success");
            sendMessage(caster, "mythic_age.spellbook.message.dwarven_mine.blessing");
        });

        return true;
    }

    /**
     * Build the mine hill in stages - irregular ellipse shape
     */
    private void buildMineHillStage(World world, BlockPos centerPos, int stage) {
        // Generate random shape on first call
        if (currentHillShape == null || stage == 1) {
            currentHillShape = new HillShape(world.random);
        }

        HillShape shape = currentHillShape;

        // Calculate ellipse range
        int maxRangeX = (int) Math.ceil(shape.radiusX) + 1;
        int maxRangeZ = (int) Math.ceil(shape.radiusZ) + 1;

        for (int x = -maxRangeX; x <= maxRangeX; x++) {
            for (int z = -maxRangeZ; z <= maxRangeZ; z++) {
                // Apply rotation transform
                double rotatedX = x * Math.cos(shape.rotation) - z * Math.sin(shape.rotation);
                double rotatedZ = x * Math.sin(shape.rotation) + z * Math.cos(shape.rotation);

                // Calculate ellipse distance
                double ellipseDistance = (rotatedX * rotatedX) / (shape.radiusX * shape.radiusX) +
                                       (rotatedZ * rotatedZ) / (shape.radiusZ * shape.radiusZ);

                // Inside the ellipse
                if (ellipseDistance <= 1.0) {
                    // Get height multiplier at this position
                    double heightMultiplier = shape.getHeightMultiplier(x, z);

                    // Calculate max height at this position (based on distance from center and random coefficient)
                    double distanceFromCenter = Math.sqrt(ellipseDistance);
                    double heightFactor = (1.0 - distanceFromCenter * 0.8) * heightMultiplier;
                    int localMaxHeight = Math.max(1, (int)(shape.maxHeight * heightFactor));

                    // Build the layers for the current stage
                    for (int y = 0; y < Math.min(stage, localMaxHeight); y++) {
                        BlockPos pos = centerPos.add(x, y, z);

                        // Select block type
                        Block blockType = selectOreType(world.random, y, localMaxHeight);
                        world.setBlockState(pos, blockType.getDefaultState());
                    }
                }
            }
        }
    }

    /**
     * Select ore type based on height and layer count
     */
    private Block selectOreType(net.minecraft.util.math.random.Random random, int currentY, int maxHeight) {
        float rand = random.nextFloat();

        if (currentY == 0) {
            // Bottom layer: mostly stone and basic ores
            if (rand < 0.12f) {
                return Blocks.IRON_ORE;
            } else if (rand < 0.20f) {
                return Blocks.COAL_ORE;
            } else if (rand < 0.25f) {
                return Blocks.COPPER_ORE;
            } else {
                return Blocks.STONE;
            }
        } else if (currentY >= maxHeight - 1) {
            // Top layer: more precious ores
            if (rand < 0.10f) {
                return Blocks.DIAMOND_ORE;
            } else if (rand < 0.25f) {
                return Blocks.GOLD_ORE;
            } else if (rand < 0.40f) {
                return Blocks.IRON_ORE;
            } else if (rand < 0.50f) {
                return Blocks.LAPIS_ORE;
            } else {
                return Blocks.STONE;
            }
        } else {
            // Middle layers
            if (rand < 0.06f) {
                return Blocks.DIAMOND_ORE;
            } else if (rand < 0.18f) {
                return Blocks.GOLD_ORE;
            } else if (rand < 0.35f) {
                return Blocks.IRON_ORE;
            } else if (rand < 0.45f) {
                return Blocks.COAL_ORE;
            } else if (rand < 0.52f) {
                return Blocks.REDSTONE_ORE;
            } else {
                return Blocks.STONE;
            }
        }
    }
}
