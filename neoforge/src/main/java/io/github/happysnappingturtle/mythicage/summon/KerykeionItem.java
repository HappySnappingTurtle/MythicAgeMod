package io.github.happysnappingturtle.mythicage.summon;

import io.github.happysnappingturtle.mythicage.core.summon.SummonMode;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 号令之物（总纲 §3.6；希腊为传令杖 Kerykeion，01_希腊 §5；T14 设计文档 §4.2）。其他文明的号令之物用同一个类。
 * <ul>
 *   <li>右键方块：32 格内跟随中的单位改为驻守该点；</li>
 *   <li>右键生物：32 格内战斗单位集火该目标（自己人、中立动物会被拒绝）；</li>
 *   <li>潜行右键（看着目标，32 格内）：战斗单位立即放特殊攻击；</li>
 *   <li>右键空气：列出自己的单位（阶段 ② 改为打开召唤管理界面）。</li>
 * </ul>
 */
public class KerykeionItem extends Item {
    public KerykeionItem(Properties props) {
        super(props.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getPlayer() instanceof ServerPlayer sp) || sp.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        int n = SummonService.commandGuard(sp, ctx.getClickedPos().above());
        sp.displayClientMessage(n > 0 ? Component.translatable("mythic_age.summon.cmd_guard", n)
                : Component.translatable("mythic_age.summon.cmd_none"), true);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResult.SUCCESS;
        }
        if (target instanceof MythSummon s && sp.getUUID().equals(s.link().owner)) {
            // 右键自己的单位：切回跟随
            SummonService.setMode((Mob) target, SummonMode.FOLLOW, null);
            sp.displayClientMessage(Component.translatable("mythic_age.summon.cmd_follow_one", target.getDisplayName()), true);
            return InteractionResult.SUCCESS;
        }
        report(sp, player.isShiftKeyDown() ? SummonService.commandSpecial(sp, target) : SummonService.commandAttack(sp, target),
                player.isShiftKeyDown());
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.success(stack);
        }
        if (sp.isShiftKeyDown()) {
            LivingEntity target = lookedAt(sp, SummonService.COMMAND_RADIUS);
            if (target == null) {
                sp.displayClientMessage(Component.translatable("mythic_age.summon.cmd_no_target"), true);
            } else {
                report(sp, SummonService.commandSpecial(sp, target), true);
            }
            return InteractionResultHolder.success(stack);
        }
        SummonScreens.open(sp);
        return InteractionResultHolder.success(stack);
    }

    private static void report(ServerPlayer sp, int n, boolean special) {
        Component msg = n < 0 ? Component.translatable("mythic_age.summon.cmd_invalid")
                : n == 0 ? Component.translatable("mythic_age.summon.cmd_none")
                : Component.translatable(special ? "mythic_age.summon.cmd_special" : "mythic_age.summon.cmd_attack", n);
        sp.displayClientMessage(msg, true);
    }

    /** 视线方向 range 格内第一个活着的生物。 */
    static LivingEntity lookedAt(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(p.getLookAngle().scale(range));
        AABB box = p.getBoundingBox().expandTowards(p.getLookAngle().scale(range)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(p, eye, end, box,
                e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator(), range * range);
        Entity e = hit == null ? null : hit.getEntity();
        return e instanceof LivingEntity le ? le : null;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.mythic_age.kerykeion.tip").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
