package com.example.mythic_age.item.spell.greek;

import com.example.mythic_age.item.GodPowerCertificateItem.MythologyType;
import com.example.mythic_age.item.SpellbookItem.SpellType;
import com.example.mythic_age.item.spell.AbstractSpell;
import com.example.mythic_age.item.spell.SpellCategory;
import com.example.mythic_age.particle.MythicParticleEffects;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

public class UnderworldArmySpell extends AbstractSpell {
    public UnderworldArmySpell() { super(SpellType.UNDERWORLD_ARMY, MythologyType.GREEK, SpellCategory.SUMMONING, 300); }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        int summonCount = 3 + world.random.nextInt(3);
        for (int i = 0; i < summonCount; i++) {
            BlockPos spawnPos = caster.getBlockPos().add(world.random.nextInt(6) - 3, 0, world.random.nextInt(6) - 3);
            while (spawnPos.getY() > world.getBottomY() && !world.getBlockState(spawnPos.down()).isSolidBlock(world, spawnPos.down())) {
                spawnPos = spawnPos.down();
            }
            SkeletonEntity skeleton = new SkeletonEntity(EntityType.SKELETON, world);
            skeleton.refreshPositionAfterTeleport(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
            skeleton.setTarget(null);
            world.spawnEntity(skeleton);
            MythicParticleEffects.spawnNormal(world, ParticleTypes.SMOKE,
                spawnPos.getX() + 0.5, spawnPos.getY() + 1, spawnPos.getZ() + 0.5, 15, 0.3, 0.5, 0.3, 0.05);
        }
        world.playSound(null, caster.getBlockPos(), SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 0.5f, 1.2f);
        return true;
    }
}
