package com.example.mythic_age.item.spell.atlantean;

import com.example.mythic_age.item.GodPowerCertificateItem.MythologyType;
import com.example.mythic_age.item.SpellbookItem.SpellType;
import com.example.mythic_age.item.spell.AbstractSpell;
import com.example.mythic_age.item.spell.SpellCategory;
import com.example.mythic_age.particle.MythicParticleEffects;
import com.example.mythic_age.particle.ModParticles;
import com.example.mythic_age.util.GodPowerWeatherManager;
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

import java.util.List;

public class OceanicWrathSpell extends AbstractSpell {
    public OceanicWrathSpell() { super(SpellType.OCEANIC_WRATH, MythologyType.ATLANTEAN, SpellCategory.OFFENSIVE, 500); }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        String wrathId = "oceanic_wrath_" + caster.getUuid().toString() + "_" + world.getTime();
        GodPowerWeatherManager.getInstance().startOceanicWrath(world, targetPos, wrathId);
        List<HostileEntity> enemies = world.getEntitiesByClass(HostileEntity.class,
            new Box(targetPos).expand(20.0, 8.0, 20.0), entity -> entity.isAlive());

        int lightningCount = 0;
        for (HostileEntity enemy : enemies) {
            enemy.damage(world.getDamageSources().magic(), 14.0f);
            enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 600, 3));
            enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 600, 2));
            enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 480, 1));

            if (lightningCount < 5) {
                LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, world);
                lightning.refreshPositionAfterTeleport(enemy.getX(), enemy.getY(), enemy.getZ());
                world.spawnEntity(lightning);
                lightningCount++;
            }
        }

        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.CONDUIT_POWER, 3600, 0));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 2400, 1));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, 2400, 0));

        MythicParticleEffects.spawnUltimate(world, ParticleTypes.SPLASH,
            targetPos.getX() + 0.5, targetPos.getY() + 2.0, targetPos.getZ() + 0.5,
            180, 12.0, 4.5, 12.0, 0.32);
        MythicParticleEffects.spawnUltimate(world, ParticleTypes.EXPLOSION_EMITTER,
            targetPos.getX() + 0.5, targetPos.getY() + 1.0, targetPos.getZ() + 0.5,
            24, 7.0, 1.2, 7.0, 0.0);
        MythicParticleEffects.spawnMajor(world, ModParticles.SHOCKWAVE,
            targetPos.getX() + 0.5, targetPos.getY() + 0.2, targetPos.getZ() + 0.5,
            42, 8.0, 0.1, 8.0, 0.02);
        MythicParticleEffects.spawnMajor(world, ModParticles.DIVINE_SPARKLE,
            targetPos.getX() + 0.5, targetPos.getY() + 2.2, targetPos.getZ() + 0.5,
            90, 9.0, 3.5, 9.0, 0.1);

        world.playSound(null, targetPos, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.WEATHER, 2.2f, 0.65f);
        world.playSound(null, targetPos, SoundEvents.ITEM_TRIDENT_THUNDER, SoundCategory.WEATHER, 1.8f, 0.9f);
        world.playSound(null, targetPos, SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.PLAYERS, 1.3f, 0.7f);
        sendMessage(caster, "mythic_age.spellbook.message.oceanic_wrath.success");
        return true;
    }
}
