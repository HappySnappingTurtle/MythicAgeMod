package com.example.mythic_age.item.spell.egyptian;

import com.example.mythic_age.item.GodPowerCertificateItem.MythologyType;
import com.example.mythic_age.item.SpellbookItem.SpellType;
import com.example.mythic_age.item.spell.AbstractSpell;
import com.example.mythic_age.item.spell.SpellCategory;
import com.example.mythic_age.particle.MythicParticleEffects;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.village.VillagerProfession;

import java.util.List;

public class IsisResurrectionSpell extends AbstractSpell {
    public IsisResurrectionSpell() { super(SpellType.ISIS_RESURRECTION, MythologyType.EGYPTIAN, SpellCategory.HEALING, 400); }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        // Fully heal the caster
        caster.setHealth(caster.getMaxHealth());
        caster.getHungerManager().setFoodLevel(20);
        caster.getHungerManager().setSaturationLevel(20.0f);

        // Clear all status effects
        caster.clearStatusEffects();

        // Grant powerful resurrection blessings
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 24000, 3));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 24000, 2));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 24000, 2));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 24000, 1));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 24000, 1));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 24000, 0));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 24000, 0));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 24000, 0));

        // Revive and heal all friendly entities in a large area
        List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class,
            new Box(targetPos).expand(30),
            entity -> entity.isAlive() && !(entity instanceof HostileEntity));

        for (LivingEntity entity : entities) {
            if (!(entity instanceof PlayerEntity)) {
                // Heal friendly entities
                entity.setHealth(entity.getMaxHealth());
                entity.clearStatusEffects();

                // Grant blessings to friendly entities
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 12000, 2));
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 12000, 1));
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 12000, 1));
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 12000, 1));

                // Put animals into breeding mode
                if (entity instanceof AnimalEntity) {
                    AnimalEntity animal = (AnimalEntity) entity;
                    animal.setLoveTicks(1200);
                }
            }
        }

        // Create a garden of life in the area
        for (int x = -15; x <= 15; x++) {
            for (int z = -15; z <= 15; z++) {
                double distance = Math.sqrt(x * x + z * z);
                if (distance <= 15 && world.random.nextFloat() < 0.3f) {
                    BlockPos pos = targetPos.add(x, 0, z);

                    // Find suitable ground position
                    while (pos.getY() > 0 && world.getBlockState(pos).isAir()) {
                        pos = pos.down();
                    }
                    pos = pos.up();

                    if (world.getBlockState(pos).isAir() && world.getBlockState(pos.down()).isSolidBlock(world, pos.down())) {
                        // Plant various flowers
                        Block[] flowers = {
                            Blocks.ROSE_BUSH, Blocks.PEONY, Blocks.LILAC, Blocks.SUNFLOWER,
                            Blocks.POPPY, Blocks.DANDELION, Blocks.BLUE_ORCHID, Blocks.ALLIUM
                        };

                        Block flower = flowers[world.random.nextInt(flowers.length)];
                        if (flower == Blocks.ROSE_BUSH || flower == Blocks.PEONY ||
                            flower == Blocks.LILAC || flower == Blocks.SUNFLOWER) {
                            // Double-tall flowers
                            if (world.getBlockState(pos.up()).isAir()) {
                                world.setBlockState(pos, flower.getDefaultState().with(TallPlantBlock.HALF, DoubleBlockHalf.LOWER));
                                world.setBlockState(pos.up(), flower.getDefaultState().with(TallPlantBlock.HALF, DoubleBlockHalf.UPPER));
                            }
                        } else {
                            // Single-tall flowers
                            world.setBlockState(pos, flower.getDefaultState());
                        }
                    }
                }
            }
        }

        // Summon life guardians (represented by villagers)
        for (int i = 0; i < 5; i++) {
            BlockPos spawnPos = targetPos.add(
                world.random.nextInt(21) - 10,
                0,
                world.random.nextInt(21) - 10
            );

            // Ensure safe spawn position
            while (!world.getBlockState(spawnPos).isAir() || !world.getBlockState(spawnPos.up()).isAir()) {
                spawnPos = spawnPos.up();
            }

            VillagerEntity lifeGuardian = EntityType.VILLAGER.create(world);
            if (lifeGuardian != null) {
                lifeGuardian.refreshPositionAfterTeleport(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                lifeGuardian.setVillagerData(lifeGuardian.getVillagerData().withProfession(VillagerProfession.CLERIC));
                world.spawnEntity(lifeGuardian);

                // Enhance the life guardians
                lifeGuardian.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 24000, 2));
                lifeGuardian.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 24000, 3));
                lifeGuardian.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 24000, 0));
                lifeGuardian.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 24000, 0));
            }
        }

        // Resurrection particle effect
        for (int i = 0; i < 200; i++) {
            double x = targetPos.getX() + (world.random.nextDouble() - 0.5) * 60;
            double y = targetPos.getY() + world.random.nextDouble() * 20;
            double z = targetPos.getZ() + (world.random.nextDouble() - 0.5) * 60;
            MythicParticleEffects.spawnAmbient(world, ParticleTypes.TOTEM_OF_UNDYING, x, y, z, 1, 0, 0.3, 0, 0.2);
            MythicParticleEffects.spawnAmbient(world, ParticleTypes.HEART, x, y, z, 1, 0, 0.2, 0, 0.1);
        }

        sendMessage(caster, "mythic_age.spellbook.message.isis_resurrection.success");
        return true;
    }
}
