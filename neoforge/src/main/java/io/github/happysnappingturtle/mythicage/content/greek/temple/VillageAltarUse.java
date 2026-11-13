package io.github.happysnappingturtle.mythicage.content.greek.temple;

import io.github.happysnappingturtle.mythicage.content.greek.prayer.OfferingLoader;
import io.github.happysnappingturtle.mythicage.content.greek.prayer.PrayerService;
import io.github.happysnappingturtle.mythicage.content.greek.prayer.TemplePrayer;
import io.github.happysnappingturtle.mythicage.village.temple.VillageTemple;
import io.github.happysnappingturtle.mythicage.village.temple.VillageTemples;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * 村庄神庙祭坛上的玩家交互（GA-13 §2.4、§2.5、§2.8）：手持供品右键献供，手持圣物潜行右键供奉，
 * 空手右键（潜行与否）打开检视，下方是自己在这个村子的供奉栏、可以取回。
 * 只有某个村子神庙的那块祭坛有这些交互（卫城 {@code ritual} 旁的村庄祭坛；还没有神庙的新村是奠基石）；
 * 玩家自己合成放下的村庄祭坛照旧是装饰。
 */
public final class VillageAltarUse {
    private VillageAltarUse() {
    }

    /** 这块方块所在位置的村庄神庙（只有那一块祭坛）。 */
    public static Optional<VillageTemple> templeAt(ServerLevel level, BlockPos pos) {
        return VillageTemples.byAltar(level.getServer(), GlobalPos.of(level.dimension(), pos.immutable()));
    }

    /**
     * 手持物品右键。客户端不知道这块是不是神庙祭坛、也不知道供品表：拿着能吃能喝的东西时先吞掉这次点击，
     * 免得一边献供一边开始吃；其余交给默认交互（服务器照样收到点击）。
     */
    public static ItemInteractionResult useItemOn(ItemStack stack, Level level, BlockPos pos, Player player) {
        boolean relic = player.isSecondaryUseActive() && stack.getItem() instanceof io.github.happysnappingturtle.mythicage.relic.RelicItem;
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer sp)) {
            return consumable(stack) || relic ? ItemInteractionResult.SUCCESS : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        Optional<VillageTemple> temple = templeAt(server, pos);
        if (temple.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        // 手持圣物潜行右键：供奉（GA-13 D1）。正常游玩时潜行右键不会走到这里（原版跳过方块交互，由 RelicEvents 接），
        // 直接调用（测试、别的模组）时照样能供奉
        if (relic && io.github.happysnappingturtle.mythicage.relic.TempleRelics.offer(sp, server, temple.get(), stack)) {
            return ItemInteractionResult.CONSUME;
        }
        var offering = OfferingLoader.greek().of(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        if (offering.isEmpty()) {
            return consumable(stack) ? ItemInteractionResult.CONSUME : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        // 名字要在扣物品之前取：扣到 0 个后物品堆变成空气
        Component name = stack.getHoverName().copy();
        PrayerService.OfferResult r = TemplePrayer.offer(sp, temple.get(), stack);
        int percent = (int) Math.round(OfferingLoader.greek().bonus() * 100);
        Component msg = switch (r) {
            case OK -> Component.translatable("mythic_age.offering.ok", name, "+" + percent + "%");
            case ALREADY -> Component.translatable("mythic_age.offering.already");
            case NOT_OWNER, NOT_DEVOTEE -> Component.translatable("mythic_age.offering.not_devotee");
            case NOT_ENOUGH -> Component.translatable("mythic_age.offering.not_enough", name, offering.get().count());
            case NOT_OFFERING -> Component.empty();
        };
        sp.displayClientMessage(msg, true);
        if (r == PrayerService.OfferResult.OK) {
            server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                    6, 0.15, 0.1, 0.15, 0.01);
            server.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 8, 0.2, 0.1, 0.2, 0.01);
            server.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.6F, 1.2F);
        }
        return ItemInteractionResult.CONSUME;
    }

    /** 空手右键：神庙祭坛打开检视；不是神庙祭坛返回 false。 */
    public static boolean useWithoutItem(Level level, BlockPos pos, Player player) {
        if (level instanceof ServerLevel server && player instanceof ServerPlayer sp) {
            Optional<VillageTemple> temple = templeAt(server, pos);
            if (temple.isEmpty()) {
                return false;
            }
            // 供奉栏先到，界面打开时就有数据；空手潜行右键也是这里（D1：打开列表取回）
            io.github.happysnappingturtle.mythicage.relic.TempleRelics.sendShelf(sp, temple.get());
            VillageTempleSurvey.send(sp, temple.get());
        }
        return true;
    }

    /** 能吃能喝（客户端会就地开始使用）的物品。 */
    static boolean consumable(ItemStack stack) {
        return stack.has(DataComponents.FOOD) || stack.is(Items.HONEY_BOTTLE) || stack.getItem() instanceof PotionItem;
    }
}
