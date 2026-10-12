package com.example.mythic_age.item.spell;

import com.example.mythic_age.item.GodPowerCertificateItem.MythologyType;
import com.example.mythic_age.item.SpellbookItem.SpellType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public interface Spell {

    SpellType getType();

    MythologyType getCivilization();

    SpellCategory getCategory();

    int getDefaultCooldown();

    boolean cast(ServerWorld world, PlayerEntity caster, BlockPos targetPos);
}
