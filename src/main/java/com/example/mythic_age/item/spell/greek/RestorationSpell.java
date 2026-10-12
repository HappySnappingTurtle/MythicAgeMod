package com.example.mythic_age.item.spell.greek;

import com.example.mythic_age.item.GodPowerCertificateItem.MythologyType;
import com.example.mythic_age.item.SpellbookItem.SpellType;
import com.example.mythic_age.item.spell.AbstractSpell;
import com.example.mythic_age.item.spell.SpellCategory;
import com.example.mythic_age.particle.MythicParticleEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

public class RestorationSpell extends AbstractSpell {
    public RestorationSpell() { super(SpellType.RESTORATION, MythologyType.GREEK, SpellCategory.HEALING, 180); }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        caster.heal(10.0f);
        world.getEntitiesByClass(PlayerEntity.class,
            caster.getBoundingBox().expand(10.0),
            p -> p != caster && p.squaredDistanceTo(caster) <= 100.0)
            .forEach(p -> p.heal(10.0f));
        MythicParticleEffects.spawnNormal(world, ParticleTypes.HEART,
            caster.getX(), caster.getY() + 1, caster.getZ(), 20, 1.0, 1.0, 1.0, 0.1);
        world.playSound(null, caster.getBlockPos(), SoundEvents.ENTITY_PLAYER_LEVELUP,
            SoundCategory.PLAYERS, 0.5f, 1.5f);
        return true;
    }
}
