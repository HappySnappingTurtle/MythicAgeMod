package com.example.mythic_age.item.spell.nordic;

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

public class GenerateNordicTempleSpell extends AbstractSpell {

    public GenerateNordicTempleSpell() {
        super(SpellType.GENERATE_NORDIC_TEMPLE, MythologyType.NORDIC, SpellCategory.UTILITY, 600);
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

        StructureGenerator.generateNordicTemple(world, spawnPos);
        sendMessage(caster, "mythic_age.spellbook.message.temple.nordic.success", spawnPos.toShortString());
        return true;
    }
}
