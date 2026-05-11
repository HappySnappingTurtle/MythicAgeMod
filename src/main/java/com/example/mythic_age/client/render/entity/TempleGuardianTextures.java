package com.example.mythic_age.client.render.entity;

import com.example.mythic_age.MythicAgeMod;
import com.example.mythic_age.item.GodPowerCertificateItem;
import net.minecraft.util.Identifier;

public final class TempleGuardianTextures {
    private TempleGuardianTextures() {
    }

    public static Identifier forType(GodPowerCertificateItem.MythologyType mythologyType) {
        return switch (mythologyType) {
            case GREEK -> new Identifier(MythicAgeMod.MOD_ID, "textures/entity/temple_guardian_greek.png");
            case EGYPTIAN -> new Identifier(MythicAgeMod.MOD_ID, "textures/entity/temple_guardian_egyptian.png");
            case CHINESE -> new Identifier(MythicAgeMod.MOD_ID, "textures/entity/temple_guardian_chinese.png");
            case ATLANTEAN -> new Identifier(MythicAgeMod.MOD_ID, "textures/entity/temple_guardian_atlantean.png");
            case NORDIC -> new Identifier(MythicAgeMod.MOD_ID, "textures/entity/temple_guardian_nordic.png");
        };
    }
}
