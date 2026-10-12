package com.example.mythic_age.item.spell;

import com.example.mythic_age.item.GodPowerCertificateItem.MythologyType;
import com.example.mythic_age.item.SpellbookItem.SpellType;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class SpellRegistry {

    private static final Map<SpellType, Spell> SPELLS = new LinkedHashMap<>();

    private SpellRegistry() {}

    public static void register(Spell spell) {
        SPELLS.put(spell.getType(), spell);
    }

    public static Spell get(SpellType type) {
        return SPELLS.get(type);
    }

    public static boolean has(SpellType type) {
        return SPELLS.containsKey(type);
    }

    public static List<Spell> getByCivilization(MythologyType civ) {
        return SPELLS.values().stream()
            .filter(s -> s.getCivilization() == civ)
            .collect(Collectors.toList());
    }

    public static Collection<Spell> getAll() {
        return Collections.unmodifiableCollection(SPELLS.values());
    }

    public static void init() {
        // Greek
        register(new com.example.mythic_age.item.spell.greek.LightningStormSpell());
        register(new com.example.mythic_age.item.spell.greek.RestorationSpell());
        register(new com.example.mythic_age.item.spell.greek.LureSpell());
        register(new com.example.mythic_age.item.spell.greek.UnderworldArmySpell());
        register(new com.example.mythic_age.item.spell.greek.PestilenceSpell());
        register(new com.example.mythic_age.item.spell.greek.CurseSpell());
        register(new com.example.mythic_age.item.spell.greek.EarthquakeSpell());
        register(new com.example.mythic_age.item.spell.greek.CeasefireSpell());
        register(new com.example.mythic_age.item.spell.greek.BronzeSpell());
        register(new com.example.mythic_age.item.spell.greek.UnderworldPassageSpell());
        register(new com.example.mythic_age.item.spell.greek.LightningStormHeraSpell());
        register(new com.example.mythic_age.item.spell.greek.PlentySpell());

        // Egyptian
        register(new com.example.mythic_age.item.spell.egyptian.RaRadianceSpell());
        register(new com.example.mythic_age.item.spell.egyptian.DesertStormSpell());
        register(new com.example.mythic_age.item.spell.egyptian.SunRainSpell());
        register(new com.example.mythic_age.item.spell.egyptian.ProsperitySpell());
        register(new com.example.mythic_age.item.spell.egyptian.EyeOfHorusSpell());
        register(new com.example.mythic_age.item.spell.egyptian.AnubisJudgmentSpell());
        register(new com.example.mythic_age.item.spell.egyptian.LocustSwarmSpell());
        register(new com.example.mythic_age.item.spell.egyptian.TornadoSpell());
        register(new com.example.mythic_age.item.spell.egyptian.IsisResurrectionSpell());
        register(new com.example.mythic_age.item.spell.egyptian.MeteorRainSpell());

        // Chinese
        register(new com.example.mythic_age.item.spell.chinese.RainPrayerSpell());
        register(new com.example.mythic_age.item.spell.chinese.EarthGodBlessingSpell());
        register(new com.example.mythic_age.item.spell.chinese.EnlightenmentSpell());
        register(new com.example.mythic_age.item.spell.chinese.AbundanceSpell());
        register(new com.example.mythic_age.item.spell.chinese.FiveThunderSpell());
        register(new com.example.mythic_age.item.spell.chinese.CraneBlessingSpell());
        register(new com.example.mythic_age.item.spell.chinese.FloodSpell());
        register(new com.example.mythic_age.item.spell.chinese.CallToArmsSpell());
        register(new com.example.mythic_age.item.spell.chinese.TaijiShieldSpell());
        register(new com.example.mythic_age.item.spell.chinese.RecreationSpell());

        // Atlantean
        register(new com.example.mythic_age.item.spell.atlantean.TidalSurgeSpell());
        register(new com.example.mythic_age.item.spell.atlantean.AbyssalCurrentSpell());
        register(new com.example.mythic_age.item.spell.atlantean.CrystalBarrierSpell());
        register(new com.example.mythic_age.item.spell.atlantean.PearlHarvestSpell());
        register(new com.example.mythic_age.item.spell.atlantean.OrichalcumForgeSpell());
        register(new com.example.mythic_age.item.spell.atlantean.SirenSongSpell());
        register(new com.example.mythic_age.item.spell.atlantean.LeviathanCallSpell());
        register(new com.example.mythic_age.item.spell.atlantean.AbyssalRiftSpell());
        register(new com.example.mythic_age.item.spell.atlantean.ChronoStasisSpell());
        register(new com.example.mythic_age.item.spell.atlantean.OceanicWrathSpell());

        // Nordic
        register(new com.example.mythic_age.item.spell.nordic.FenrirHowlSpell());
        register(new com.example.mythic_age.item.spell.nordic.OdinEyeSpell());
        register(new com.example.mythic_age.item.spell.nordic.GreatHuntSpell());
        register(new com.example.mythic_age.item.spell.nordic.DwarvenMineSpell());
        register(new com.example.mythic_age.item.spell.nordic.ThorWrathSpell());
        register(new com.example.mythic_age.item.spell.nordic.ValhallaCallSpell());
        register(new com.example.mythic_age.item.spell.nordic.FrostSpell());
        register(new com.example.mythic_age.item.spell.nordic.WalkingWoodsSpell());
        register(new com.example.mythic_age.item.spell.nordic.RagnarokSpell());
        register(new com.example.mythic_age.item.spell.nordic.FimbulwinterSpell());

        // Temple Generation
        register(new com.example.mythic_age.item.spell.egyptian.GenerateEgyptianTempleSpell());
        register(new com.example.mythic_age.item.spell.chinese.GenerateChineseTempleSpell());
        register(new com.example.mythic_age.item.spell.atlantean.GenerateAtlanteanTempleSpell());
        register(new com.example.mythic_age.item.spell.nordic.GenerateNordicTempleSpell());
    }
}
