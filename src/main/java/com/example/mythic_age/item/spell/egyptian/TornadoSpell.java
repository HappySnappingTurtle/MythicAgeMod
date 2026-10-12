package com.example.mythic_age.item.spell.egyptian;

import com.example.mythic_age.item.GodPowerCertificateItem.MythologyType;
import com.example.mythic_age.item.SpellbookItem.SpellType;
import com.example.mythic_age.item.spell.AbstractSpell;
import com.example.mythic_age.item.spell.SpellCategory;
import com.example.mythic_age.particle.MythicParticleEffects;
import com.example.mythic_age.util.GodPowerTaskScheduler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class TornadoSpell extends AbstractSpell {
    public TornadoSpell() { super(SpellType.TORNADO, MythologyType.EGYPTIAN, SpellCategory.OFFENSIVE, 300); }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        // Grant wind god blessing to caster
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 10000, 0));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 10000, 2));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 10000, 2));

        // Create a moving tornado
        createMovingTornado(world, targetPos, caster);

        sendMessage(caster, "mythic_age.spellbook.message.tornado.success");
        return true;
    }

    private void createMovingTornado(ServerWorld world, BlockPos startPos, PlayerEntity caster) {
        final int tornadoHeight = 20;
        final int tornadoRadius = 8;
        final int totalDuration = 600; // 30 seconds
        final double moveSpeed = 0.2;

        // Random initial movement direction
        final double[] moveAngle = {world.random.nextDouble() * 2 * Math.PI};
        final double[] currentPos = {startPos.getX(), startPos.getZ()};

        for (int tick = 0; tick < totalDuration; tick++) {
            final int currentTick = tick;
            GodPowerTaskScheduler.schedule(world, currentTick, () -> {
                if (currentTick % 20 == 0 && world.random.nextFloat() < 0.4f) {
                    moveAngle[0] = world.random.nextDouble() * 2 * Math.PI;
                }

                currentPos[0] += Math.cos(moveAngle[0]) * moveSpeed;
                currentPos[1] += Math.sin(moveAngle[0]) * moveSpeed;

                BlockPos currentCenter = new BlockPos((int) currentPos[0], startPos.getY(), (int) currentPos[1]);
                performTornadoEffectAtPosition(world, currentCenter, tornadoHeight, tornadoRadius, currentTick);
            });
        }
    }

    private void performTornadoEffectAtPosition(ServerWorld world, BlockPos center, int height, int radius, int currentTick) {
        // Tornado core effect
        for (int y = 0; y < height; y++) {
            double currentRadius = radius * (1.0 - (double) y / height * 0.3);

            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    double distance = Math.sqrt(x * x + z * z);
                    if (distance <= currentRadius) {
                        BlockPos pos = center.add(x, y, z);

                        // Block destruction logic
                        if (world.random.nextFloat() < 0.15f) {
                            BlockState state = world.getBlockState(pos);
                            Block block = state.getBlock();

                            // Do not destroy bedrock, obsidian, barriers, etc.
                            if (!block.equals(Blocks.BEDROCK) &&
                                !block.equals(Blocks.OBSIDIAN) &&
                                !block.equals(Blocks.BARRIER) &&
                                !block.equals(Blocks.END_PORTAL_FRAME) &&
                                state.getHardness(world, pos) >= 0) {

                                if (world.random.nextFloat() < 0.6f) {
                                    world.breakBlock(pos, true);
                                } else {
                                    world.setBlockState(pos, Blocks.AIR.getDefaultState());
                                }
                            }
                        }

                        // Enhanced particle effects
                        if (world.random.nextFloat() < 0.8f) {
                            double particleX = center.getX() + x + world.random.nextGaussian() * 0.3;
                            double particleY = center.getY() + y + world.random.nextGaussian() * 0.3;
                            double particleZ = center.getZ() + z + world.random.nextGaussian() * 0.3;

                            // Spiral ascending particle effect
                            double angle = (currentTick * 0.2 + y * 0.1) % (2 * Math.PI);
                            double spiralX = Math.cos(angle) * distance * 0.1;
                            double spiralZ = Math.sin(angle) * distance * 0.1;

                            MythicParticleEffects.spawnAmbient(world, ParticleTypes.CLOUD,
                                particleX + spiralX, particleY, particleZ + spiralZ,
                                1, 0, 0.1, 0, 0.05);
                            MythicParticleEffects.spawnAmbient(world, ParticleTypes.SWEEP_ATTACK,
                                particleX, particleY, particleZ,
                                1, 0, 0, 0, 0);
                        }
                    }
                }
            }
        }

        // Apply effects to entities in range
        Box effectBox = new Box(center).expand(radius);
        List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class, effectBox, entity -> true);

        for (LivingEntity entity : entities) {
            double distance = entity.getPos().distanceTo(Vec3d.ofCenter(center));
            if (distance <= radius) {
                // Upward pull effect
                Vec3d velocity = entity.getVelocity();
                entity.setVelocity(velocity.x * 0.8, Math.max(velocity.y, 0.3), velocity.z * 0.8);

                // Damage hostile mobs
                if (entity instanceof HostileEntity) {
                    entity.damage(world.getDamageSources().magic(), 2.0f);
                    entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 2));
                    entity.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 100, 1));
                }

                // Protect players
                if (entity instanceof PlayerEntity) {
                    entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 100, 0));
                    entity.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 100, 2));
                    entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 100, 1));
                }
            }
        }

        // Sound effect (every 10 ticks)
        if (currentTick % 10 == 0) {
            world.playSound(null, center, SoundEvents.ENTITY_ENDER_DRAGON_FLAP,
                SoundCategory.MASTER, 0.8f, 0.8f + world.random.nextFloat() * 0.4f);
        }
    }
}
