package com.example.mythic_age.item.spell.egyptian;

import com.example.mythic_age.item.GodPowerCertificateItem.MythologyType;
import com.example.mythic_age.item.SpellbookItem.SpellType;
import com.example.mythic_age.item.spell.AbstractSpell;
import com.example.mythic_age.item.spell.SpellCategory;
import com.example.mythic_age.particle.MythicParticleEffects;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.List;

public class AnubisJudgmentSpell extends AbstractSpell {
    public AnubisJudgmentSpell() { super(SpellType.ANUBIS_JUDGMENT, MythologyType.EGYPTIAN, SpellCategory.OFFENSIVE, 240); }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        // Find nearby hostile entities for judgment
        List<LivingEntity> enemies = world.getEntitiesByClass(LivingEntity.class,
            new Box(targetPos).expand(20),
            entity -> entity instanceof HostileEntity && entity.isAlive());

        for (LivingEntity enemy : enemies) {
            // Determine judgment based on enemy health percentage
            float healthPercentage = enemy.getHealth() / enemy.getMaxHealth();

            if (healthPercentage > 0.7f) {
                // High health enemies receive moderate punishment
                enemy.damage(world.getDamageSources().magic(), 15.0f);
                enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 8000, 2));
                enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 8000, 2));
            } else if (healthPercentage > 0.3f) {
                // Medium health enemies receive severe punishment
                enemy.damage(world.getDamageSources().magic(), 25.0f);
                enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 6000, 2));
                enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 6000, 0));
            } else {
                // Low health enemies are judged immediately
                enemy.damage(world.getDamageSources().magic(), enemy.getMaxHealth());
            }

            // Summon judgment fire at enemy position
            BlockPos enemyPos = enemy.getBlockPos();
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos firePos = enemyPos.add(x, 0, z);
                    if (world.getBlockState(firePos).isAir()) {
                        world.setBlockState(firePos, Blocks.SOUL_FIRE.getDefaultState());
                    }
                }
            }
        }

        // Summon Anubis guards (represented by wolves)
        for (int i = 0; i < 4; i++) {
            BlockPos spawnPos = targetPos.add(
                world.random.nextInt(11) - 5,
                0,
                world.random.nextInt(11) - 5
            );

            // Ensure safe spawn position
            while (!world.getBlockState(spawnPos).isAir() || !world.getBlockState(spawnPos.up()).isAir()) {
                spawnPos = spawnPos.up();
            }

            WolfEntity anubisGuard = EntityType.WOLF.create(world);
            if (anubisGuard != null) {
                anubisGuard.refreshPositionAfterTeleport(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                anubisGuard.setTamed(true);
                anubisGuard.setOwner(caster);
                world.spawnEntity(anubisGuard);

                // Enhance the Anubis guards
                anubisGuard.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 15000, 3));
                anubisGuard.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 15000, 2));
                anubisGuard.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 15000, 2));
                anubisGuard.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 15000, 0));
            }
        }

        // Grant death god blessing to caster
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 10000, 2));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 10000, 1));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 10000, 0));

        // Purple judgment particle effect
        for (int i = 0; i < 120; i++) {
            double x = targetPos.getX() + (world.random.nextDouble() - 0.5) * 40;
            double y = targetPos.getY() + world.random.nextDouble() * 8;
            double z = targetPos.getZ() + (world.random.nextDouble() - 0.5) * 40;
            MythicParticleEffects.spawnAmbient(world, ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 1, 0, 0.2, 0, 0.1);
        }

        sendMessage(caster, "mythic_age.spellbook.message.anubis_judgment.success");
        return true;
    }
}
