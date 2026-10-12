package com.example.mythic_age.item.spell.nordic;

import com.example.mythic_age.item.GodPowerCertificateItem.MythologyType;
import com.example.mythic_age.item.SpellbookItem.SpellType;
import com.example.mythic_age.item.spell.AbstractSpell;
import com.example.mythic_age.item.spell.SpellCategory;
import com.example.mythic_age.particle.MythicParticleEffects;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
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
import net.minecraft.world.World;

import java.util.List;

public class RagnarokSpell extends AbstractSpell {
    public RagnarokSpell() { super(SpellType.RAGNAROK, MythologyType.NORDIC, SpellCategory.OFFENSIVE, 500); }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        // Create multiple explosions
        for (int i = 0; i < 10; i++) {
            double angle = (i * 36.0) * Math.PI / 180.0;
            double radius = 5.0 + world.random.nextDouble() * 10.0;
            double x = targetPos.getX() + Math.cos(angle) * radius;
            double z = targetPos.getZ() + Math.sin(angle) * radius;

            world.createExplosion(null, x, targetPos.getY(), z, 4.0f, World.ExplosionSourceType.NONE);
        }

        // Summon multiple lightning bolts
        for (int i = 0; i < 15; i++) {
            double x = targetPos.getX() + (world.random.nextDouble() - 0.5) * 30;
            double z = targetPos.getZ() + (world.random.nextDouble() - 0.5) * 30;

            LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, world);
            lightning.refreshPositionAfterTeleport(x, targetPos.getY(), z);
            world.spawnEntity(lightning);
        }

        // Deal massive damage to all hostile mobs in a large area
        List<HostileEntity> enemies = world.getEntitiesByClass(HostileEntity.class,
            new Box(targetPos).expand(25), entity -> entity.isAlive());

        for (HostileEntity enemy : enemies) {
            enemy.damage(world.getDamageSources().explosion(null, null), 30.0f);
            enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 400, 2));
        }

        // Grant player ultimate buffs
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 12000, 3));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 12000, 2));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 12000, 0));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 6000, 2));

        MythicParticleEffects.spawnUltimate(world, ParticleTypes.EXPLOSION_EMITTER,
            targetPos.getX() + 0.5, targetPos.getY() + 1.0, targetPos.getZ() + 0.5,
            3, 0.0, 0.0, 0.0, 0.0);
        MythicParticleEffects.spawnMajor(world, ParticleTypes.SOUL_FIRE_FLAME,
            targetPos.getX() + 0.5, targetPos.getY() + 2.0, targetPos.getZ() + 0.5,
            90, 12.0, 5.0, 12.0, 0.12);

        world.playSound(null, targetPos, SoundEvents.ENTITY_ENDER_DRAGON_DEATH, SoundCategory.PLAYERS, 2.0f, 0.5f);
        sendMessage(caster, "mythic_age.spellbook.message.ragnarok.success");

        return true;
    }
}
