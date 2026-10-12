package com.example.mythic_age.item.spell.egyptian;

import com.example.mythic_age.item.GodPowerCertificateItem.MythologyType;
import com.example.mythic_age.item.SpellbookItem.SpellType;
import com.example.mythic_age.item.spell.AbstractSpell;
import com.example.mythic_age.item.spell.SpellCategory;
import com.example.mythic_age.particle.MythicParticleEffects;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.ParrotEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.List;

public class EyeOfHorusSpell extends AbstractSpell {
    public EyeOfHorusSpell() { super(SpellType.EYE_OF_HORUS, MythologyType.EGYPTIAN, SpellCategory.UTILITY, 200); }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        // Grant powerful vision abilities to caster
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 12000, 0));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 12000, 0));

        // Make all hostile entities glow
        List<LivingEntity> enemies = world.getEntitiesByClass(LivingEntity.class,
            new Box(targetPos).expand(50),
            entity -> entity instanceof HostileEntity && entity.isAlive());

        for (LivingEntity enemy : enemies) {
            enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 12000, 0));
            enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 8000, 1));
        }

        // Reveal hidden treasures in a large area
        for (int i = 0; i < 20; i++) {
            BlockPos treasurePos = targetPos.add(
                world.random.nextInt(61) - 30,
                world.random.nextInt(21) - 10,
                world.random.nextInt(61) - 30
            );

            // Search for underground ores and mark them
            for (int y = treasurePos.getY(); y >= treasurePos.getY() - 20 && y >= 0; y--) {
                BlockPos checkPos = new BlockPos(treasurePos.getX(), y, treasurePos.getZ());
                BlockState state = world.getBlockState(checkPos);

                if (state.isOf(Blocks.DIAMOND_ORE) || state.isOf(Blocks.GOLD_ORE) ||
                    state.isOf(Blocks.IRON_ORE) || state.isOf(Blocks.EMERALD_ORE)) {
                    // Place a glowing block above the ore as a marker
                    BlockPos markerPos = checkPos.up();
                    while (!world.getBlockState(markerPos).isAir() && markerPos.getY() < world.getTopY()) {
                        markerPos = markerPos.up();
                    }
                    if (world.getBlockState(markerPos).isAir()) {
                        world.setBlockState(markerPos, Blocks.GLOWSTONE.getDefaultState());
                    }
                    break;
                }
            }
        }

        // Summon guardian eagles (represented by parrots)
        for (int i = 0; i < 3; i++) {
            BlockPos spawnPos = targetPos.add(
                world.random.nextInt(11) - 5,
                3 + world.random.nextInt(3),
                world.random.nextInt(11) - 5
            );

            ParrotEntity eagle = EntityType.PARROT.create(world);
            if (eagle != null) {
                eagle.refreshPositionAfterTeleport(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                eagle.setVariant(ParrotEntity.Variant.BLUE);
                world.spawnEntity(eagle);

                // Enhance the guardian eagles
                eagle.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 12000, 2));
                eagle.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 12000, 2));
                eagle.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 12000, 1));
            }
        }

        // Golden eye particle effect
        for (int i = 0; i < 100; i++) {
            double x = targetPos.getX() + (world.random.nextDouble() - 0.5) * 40;
            double y = targetPos.getY() + world.random.nextDouble() * 15;
            double z = targetPos.getZ() + (world.random.nextDouble() - 0.5) * 40;
            MythicParticleEffects.spawnAmbient(world, ParticleTypes.END_ROD, x, y, z, 1, 0, 0.2, 0, 0.1);
        }

        sendMessage(caster, "mythic_age.spellbook.message.eye_of_horus.success");
        return true;
    }
}
