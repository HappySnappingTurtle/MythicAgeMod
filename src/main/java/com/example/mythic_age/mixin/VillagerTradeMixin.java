package com.example.mythic_age.mixin;

import com.example.mythic_age.entity.CivilizedVillager;
import com.example.mythic_age.item.GodPowerCertificateItem;
import com.example.mythic_age.system.ChineseCertificateManager;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.village.TradeOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantEntity.class)
public abstract class VillagerTradeMixin {

    @Inject(method = "trade", at = @At("TAIL"))
    private void afterTrade(TradeOffer offer, CallbackInfo ci) {
        if (!((Object) this instanceof VillagerEntity villager)) return;
        PlayerEntity customer = ((MerchantEntity) (Object) this).getCustomer();
        if (customer == null) return;
        if (!(villager instanceof CivilizedVillager cv)) return;
        if (cv.mythicAge$getCivilization() != GodPowerCertificateItem.MythologyType.CHINESE) return;

        ChineseCertificateManager.getInstance().onVillagerTrade(customer, villager, offer);
    }
}
