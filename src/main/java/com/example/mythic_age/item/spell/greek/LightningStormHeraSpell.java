package com.example.mythic_age.item.spell.greek;

import com.example.mythic_age.item.GodPowerCertificateItem.MythologyType;
import com.example.mythic_age.item.SpellbookItem.SpellType;
import com.example.mythic_age.item.spell.AbstractSpell;
import com.example.mythic_age.item.spell.SpellCategory;
import com.example.mythic_age.particle.MythicParticleEffects;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

public class LightningStormHeraSpell extends AbstractSpell {
    public LightningStormHeraSpell() { super(SpellType.LIGHTNING_STORM_HERA, MythologyType.GREEK, SpellCategory.OFFENSIVE, 500); }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                if (world.random.nextFloat() < 0.8f) {
                    BlockPos strikePos = targetPos.add(x, 0, z);
                    while (strikePos.getY() < world.getTopY() && !world.getBlockState(strikePos).isSolidBlock(world, strikePos)) {
                        strikePos = strikePos.up();
                    }
                    LightningEntity lightning = new LightningEntity(net.minecraft.entity.EntityType.LIGHTNING_BOLT, world);
                    lightning.refreshPositionAfterTeleport(strikePos.getX() + 0.5, strikePos.getY(), strikePos.getZ() + 0.5);
                    world.spawnEntity(lightning);
                    MythicParticleEffects.spawnMajor(world, ParticleTypes.ELECTRIC_SPARK,
                        strikePos.getX() + 0.5, strikePos.getY() + 1, strikePos.getZ() + 0.5, 20, 0.8, 0.8, 0.8, 0.2);
                }
            }
        }
        world.playSound(null, targetPos, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.WEATHER, 2.0f, 0.8f);
        return true;
    }
}
