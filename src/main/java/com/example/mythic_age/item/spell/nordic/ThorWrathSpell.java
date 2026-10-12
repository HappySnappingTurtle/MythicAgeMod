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

import java.util.List;

public class ThorWrathSpell extends AbstractSpell {
    public ThorWrathSpell() { super(SpellType.THOR_WRATH, MythologyType.NORDIC, SpellCategory.OFFENSIVE, 240); }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        // Create multiple lightning strikes around the target
        for (int i = 0; i < 7; i++) {
            double angle = (i * 51.4) * Math.PI / 180.0; // 360/7 degree spacing
            double radius = 3.0 + world.random.nextDouble() * 5.0;
            double x = targetPos.getX() + Math.cos(angle) * radius;
            double z = targetPos.getZ() + Math.sin(angle) * radius;

            LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, world);
            lightning.refreshPositionAfterTeleport(x, targetPos.getY(), z);
            world.spawnEntity(lightning);
            MythicParticleEffects.spawnMajor(world, ParticleTypes.ELECTRIC_SPARK,
                x, targetPos.getY() + 1.0, z,
                18, 0.8, 1.2, 0.8, 0.2);
        }

        // Deal lightning damage to hostile mobs in a large area
        List<HostileEntity> enemies = world.getEntitiesByClass(HostileEntity.class,
            new Box(targetPos).expand(12), entity -> entity.isAlive());

        for (HostileEntity enemy : enemies) {
            enemy.damage(world.getDamageSources().lightningBolt(), 20.0f);
            enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 300, 3));
            enemy.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 300, 2));
        }

        // Grant player Thor's power
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 6000, 2));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 6000, 1));
        caster.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 6000, 1));

        world.playSound(null, targetPos, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 3.0f, 0.5f);
        sendMessage(caster, "mythic_age.spellbook.message.thor_wrath.success");

        return true;
    }
}
