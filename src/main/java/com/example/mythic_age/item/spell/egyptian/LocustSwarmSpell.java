package com.example.mythic_age.item.spell.egyptian;

import com.example.mythic_age.item.GodPowerCertificateItem.MythologyType;
import com.example.mythic_age.item.SpellbookItem.SpellType;
import com.example.mythic_age.item.spell.AbstractSpell;
import com.example.mythic_age.item.spell.SpellCategory;
import com.example.mythic_age.particle.MythicParticleEffects;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.block.PlantBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.List;

public class LocustSwarmSpell extends AbstractSpell {
    public LocustSwarmSpell() { super(SpellType.LOCUST_SWARM, MythologyType.EGYPTIAN, SpellCategory.OFFENSIVE, 300); }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        // Destroy vegetation in a large area
        for (int x = -12; x <= 12; x++) {
            for (int z = -12; z <= 12; z++) {
                for (int y = -2; y <= 5; y++) {
                    BlockPos pos = targetPos.add(x, y, z);
                    BlockState state = world.getBlockState(pos);

                    // Destroy plants and crops
                    if (state.getBlock() instanceof PlantBlock ||
                        state.getBlock() instanceof CropBlock ||
                        state.isOf(Blocks.GRASS) || state.isOf(Blocks.TALL_GRASS) ||
                        state.isOf(Blocks.FERN) || state.isOf(Blocks.LARGE_FERN) ||
                        state.getBlock().getDefaultState().isIn(BlockTags.FLOWERS) ||
                        state.getBlock().getDefaultState().isIn(BlockTags.LEAVES)) {

                        if (world.random.nextFloat() < 0.7f) {
                            world.setBlockState(pos, Blocks.AIR.getDefaultState());
                        }
                    }

                    // Convert grass blocks to dirt
                    if (state.isOf(Blocks.GRASS_BLOCK)) {
                        world.setBlockState(pos, Blocks.DIRT.getDefaultState());
                    }
                }
            }
        }

        // Damage creatures in the area
        List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class,
            new Box(targetPos).expand(12),
            entity -> entity.isAlive() && !(entity instanceof PlayerEntity));

        for (LivingEntity entity : entities) {
            if (entity instanceof HostileEntity) {
                // Deal sustained damage to hostile mobs
                entity.damage(world.getDamageSources().magic(), 8.0f);
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 8000, 2));
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 8000, 3));
            } else if (entity instanceof AnimalEntity) {
                // Deal minor damage to animals
                entity.damage(world.getDamageSources().magic(), 3.0f);
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 4000, 1));
            }
        }

        // Summon locusts (represented by bees)
        for (int i = 0; i < 15; i++) {
            BlockPos spawnPos = targetPos.add(
                world.random.nextInt(21) - 10,
                world.random.nextInt(5),
                world.random.nextInt(21) - 10
            );

            BeeEntity locust = EntityType.BEE.create(world);
            if (locust != null) {
                locust.refreshPositionAfterTeleport(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                locust.setAngryAt(caster.getUuid());
                locust.setAngerTime(1200);
                world.spawnEntity(locust);

                // Enhance the locusts
                locust.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 12000, 2));
                locust.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 12000, 1));
            }
        }

        // Grant locust immunity to caster
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.SATURATION, 8000, 2));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 8000, 1));

        // Brown locust particle effect
        for (int i = 0; i < 150; i++) {
            double x = targetPos.getX() + (world.random.nextDouble() - 0.5) * 48;
            double y = targetPos.getY() + world.random.nextDouble() * 10;
            double z = targetPos.getZ() + (world.random.nextDouble() - 0.5) * 48;
            MythicParticleEffects.spawnAmbient(world, ParticleTypes.HAPPY_VILLAGER, x, y, z, 1, 0, 0.2, 0, 0.1);
        }

        sendMessage(caster, "mythic_age.spellbook.message.locust_swarm.success");
        return true;
    }
}
