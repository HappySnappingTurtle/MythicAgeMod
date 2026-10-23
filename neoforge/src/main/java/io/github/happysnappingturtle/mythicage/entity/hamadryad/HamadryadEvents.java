package io.github.happysnappingturtle.mythicage.entity.hamadryad;

import io.github.happysnappingturtle.mythicage.wild.greek.GreekWild;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.List;

/**
 * 树仙女的护树与赠露（T15 §6）：玩家砍原木（BreakEvent）、对本命橡木用骨粉（RightClickBlock）。
 */
public final class HamadryadEvents {
    /** 本命橡木与她的树根相距多远内都算「她的树」（树干、枝）。 */
    public static final double OWN_TREE_RADIUS = 8;

    private HamadryadEvents() {
    }

    /** pos 附近、本命树在 radius 格内的树仙女。 */
    public static List<HamadryadEntity> near(Level level, BlockPos pos, double radius) {
        Vec3 c = Vec3.atCenterOf(pos);
        return level.getEntitiesOfClass(HamadryadEntity.class, new AABB(pos).inflate(radius + 16),
                h -> h.isAlive() && h.treeRoot() != null && Vec3.atCenterOf(h.treeRoot()).distanceToSqr(c) <= radius * radius);
    }

    public static void onBreak(BlockEvent.BreakEvent event) {
        Player p = event.getPlayer();
        if (!(event.getLevel() instanceof Level level) || level.isClientSide || p == null || p.isCreative()) {
            return;
        }
        BlockState s = event.getState();
        if (!s.is(BlockTags.LOGS)) {
            return;
        }
        for (HamadryadEntity h : near(level, event.getPos(), HamadryadEntity.PROTECT_RADIUS)) {
            if (event.getPos().equals(h.treeRoot())) {
                h.noteRootBroken(p);
            } else {
                h.onChop(p);
            }
        }
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        ItemStack stack = event.getItemStack();
        if (level.isClientSide || !stack.is(Items.BONE_MEAL)
                || !level.getBlockState(event.getPos()).is(GreekWild.HAMADRYAD_OAK_LOG.get())) {
            return;
        }
        List<HamadryadEntity> owners = near(level, event.getPos(), OWN_TREE_RADIUS);
        if (owners.isEmpty()) {
            return;
        }
        if (owners.get(0).onBoneMeal(event.getEntity()) && !event.getEntity().isCreative()) {
            stack.shrink(1);
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
