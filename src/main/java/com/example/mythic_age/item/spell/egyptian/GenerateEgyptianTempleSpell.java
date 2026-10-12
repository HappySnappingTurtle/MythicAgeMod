package com.example.mythic_age.item.spell.egyptian;

import com.example.mythic_age.item.GodPowerCertificateItem.MythologyType;
import com.example.mythic_age.item.SpellbookItem.SpellType;
import com.example.mythic_age.item.spell.AbstractSpell;
import com.example.mythic_age.item.spell.SpellCategory;
import com.example.mythic_age.util.StructureGenerator;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;

public class GenerateEgyptianTempleSpell extends AbstractSpell {

    public GenerateEgyptianTempleSpell() {
        super(SpellType.GENERATE_EGYPTIAN_TEMPLE, MythologyType.EGYPTIAN, SpellCategory.UTILITY, 600);
    }

    @Override
    protected boolean executeCast(ServerWorld world, PlayerEntity caster, BlockPos targetPos) {
        Direction facing = caster.getHorizontalFacing();
        BlockPos horizontalTarget = caster.getBlockPos().offset(facing, 12);
        BlockPos spawnPos = new BlockPos(
            horizontalTarget.getX(),
            world.getTopY(Heightmap.Type.WORLD_SURFACE, horizontalTarget.getX(), horizontalTarget.getZ()),
            horizontalTarget.getZ()
        );

        StructureGenerator.generateEgyptianTemple(world, spawnPos);
        sendMessage(caster, "mythic_age.spellbook.message.temple.egyptian.success", spawnPos.toShortString());
        return true;
    }
}
