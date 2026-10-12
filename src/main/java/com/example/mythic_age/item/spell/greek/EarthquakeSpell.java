package com.example.mythic_age.item.spell.greek;

import com.example.mythic_age.item.GodPowerCertificateItem.MythologyType;
import com.example.mythic_age.item.SpellbookItem.SpellType;
import com.example.mythic_age.item.spell.AbstractSpell;
import com.example.mythic_age.item.spell.SpellCategory;
import com.example.mythic_age.particle.MythicParticleEffects;
import com.example.mythic_age.util.EarthquakeManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

public class EarthquakeSpell extends AbstractSpell {
    public EarthquakeSpell() { super(SpellType.EARTHQUAKE, MythologyType.GREEK, SpellCategory.OFFENSIVE, 400); }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        BlockPos epicenter = targetPos;
        String earthquakeId = "earthquake_" + caster.getUuid().toString() + "_" + System.currentTimeMillis();
        EarthquakeManager.getInstance().startEarthquake(world, epicenter, earthquakeId, caster.getYaw());

        MythicParticleEffects.spawnUltimate(world, ParticleTypes.EXPLOSION_EMITTER,
            epicenter.getX() + 0.5, epicenter.getY() + 1, epicenter.getZ() + 0.5, 3, 0, 0, 0, 0);
        for (int ring = 1; ring <= 3; ring++) {
            for (int i = 0; i < ring * 8; i++) {
                double angle = (i / (ring * 8.0)) * 2 * Math.PI;
                double radius = ring * 3.0;
                double x = epicenter.getX() + Math.cos(angle) * radius;
                double z = epicenter.getZ() + Math.sin(angle) * radius;
                MythicParticleEffects.spawnMajor(world, ParticleTypes.CLOUD, x, epicenter.getY() + 1, z, 3, 0.5, 0.5, 0.5, 0.1);
            }
        }
        world.playSound(null, epicenter, SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.BLOCKS, 2.0f, 0.3f);
        world.playSound(null, epicenter, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 1.5f, 0.2f);
        sendMessage(caster, "mythic_age.spellbook.message.earthquake.success");
        return true;
    }
}
