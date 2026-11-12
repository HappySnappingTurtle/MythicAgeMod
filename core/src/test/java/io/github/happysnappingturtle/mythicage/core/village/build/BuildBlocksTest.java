package io.github.happysnappingturtle.mythicage.core.village.build;

import com.google.gson.JsonParser;
import io.github.happysnappingturtle.mythicage.core.village.CivVillageData;
import io.github.happysnappingturtle.mythicage.core.village.gen.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class BuildBlocksTest {
    static BuildSegments.Cell cell(int x, String block) { return new BuildSegments.Cell(new Pos(x,0,0), block, "", 0, false, x); }
    static BuildProject project() { return new BuildProject(1,"test:house","plot","","",new Pos(0,0,0),Turn.R0,null,null,0); }
    @Test void tcGA10_01_supportBeforeAttachment() throws Exception {
        var stone = cell(2,"minecraft:stone_bricks"); var torch = cell(0,"minecraft:wall_torch"); var door = cell(1,"minecraft:oak_door");
        var segment = new BuildSegments.Segment(0,BuildSegments.Kind.PLACE,List.of(torch,door,stone));
        var ordered = BuildSegments.ordered(segment,c -> !c.equals(stone),c -> c.equals(torch) ? Set.of(stone.pos()) : Set.of());
        var rules = io.github.happysnappingturtle.mythicage.core.greek.temple.ArchitectureAttachments.parse(java.nio.file.Files.readString(
                java.nio.file.Path.of(System.getProperty("mythicage.rootDir"), "neoforge/src/main/resources/data/mythic_age/mythic_age/architecture_attach.json")));
        assertTrue(rules.requiresSupport("mythic_age:terracotta_antefix"));
        assertFalse(rules.requiresSupport("minecraft:stone_bricks"));
        assertEquals(stone,ordered.cells().getFirst());
        assertTrue(ordered.cells().indexOf(stone) < ordered.cells().indexOf(torch));
    }
    @Test void tcGA10_02_tripCap() {
        var cells = java.util.stream.IntStream.range(0,40).mapToObj(i -> cell(i,"minecraft:stone_bricks")).toList();
        assertEquals(List.of(16,16,8),BuildSegments.cut(cells,16).stream().map(s -> s.cells().size()).toList());
        assertTrue(BuildSegments.cut(cells,3).stream().allMatch(s -> s.solid() <= 3));
        var withAir = new ArrayList<>(cells);
        for (int i=40;i<100;i++) withAir.add(cell(i,BuildSegments.AIR));
        assertEquals(3,BuildSegments.cut(withAir,16).size(),"空白模板格不增加取货趟数");
    }
    @Test void tcGA10_03_demolishThenPlaceAndLate() {
        var old = java.util.stream.IntStream.range(0,5).mapToObj(i -> cell(i,"minecraft:stone")).toList();
        var next = java.util.stream.IntStream.range(10,30).mapToObj(i -> cell(i,"minecraft:stone_bricks")).toList();
        var result = BuildSegments.replace(old,next,0,p -> p.x() >= 27,16);
        assertEquals(5,result.stream().takeWhile(s -> s.kind()==BuildSegments.Kind.DEMOLISH).mapToInt(s -> s.cells().size()).sum());
        assertEquals(20,result.stream().filter(s -> s.kind()==BuildSegments.Kind.PLACE).mapToInt(s -> s.cells().size()).sum());
        assertTrue(result.getLast().cells().stream().allMatch(BuildSegments.Cell::last));
    }
    @Test void tcGA10_04_timing() { assertEquals(3000,BuildPacing.placementTicks(600,3,15)); }
    @Test void tcGA10_05_globalQuota() {
        var budget = new BuildPacing.Budget();
        for(int i=0;i<8;i++) assertTrue(budget.take(1,8));
        assertFalse(budget.take(1,8)); assertTrue(budget.take(2,8));
    }
    @Test void tcGA10_06_catchUp() {
        long bank=1000; var settings=BuildPacing.Settings.DEFAULT;
        for(int i=0;i<200;i++) { assertEquals(5,BuildPacing.ticks(settings,bank,1)); bank=BuildPacing.spendBank(settings,bank); }
        assertEquals(0,bank); assertEquals(15,BuildPacing.ticks(settings,bank,1));
        assertEquals(2,BuildPacing.ticks(settings,0,10));
    }
    @Test void tcGA10_07_legacyMigration() {
        var p=project(); p.layoutVersion=0; p.placed.set(0);
        var layout=BuildSegments.legacyCut(java.util.stream.IntStream.range(0,40).mapToObj(i -> cell(i,"minecraft:stone")).toList(),16);
        p.migrateCells(layout); p.migrateCells(layout);
        assertEquals(16,BuildProject.count(p.placedCells)); assertTrue(p.blockedCells.isEmpty()); assertTrue(p.missingCells.isEmpty());
    }
    @Test void tcGA10_08_bitmapsRoundTripAndRepair() {
        var p=project(); var segment=new BuildSegments.Segment(0,BuildSegments.Kind.PLACE,List.of(cell(0,"a"),cell(1,"b"),cell(2,"c")));
        p.placedCell(segment,0); BuildProject.mark(p.blockedCells,0,1,true); BuildProject.mark(p.missingCells,0,2,true);
        for(var map:List.of(p.placedCells,p.blockedCells,p.missingCells)) {
            var restored=new TreeMap<Integer,BitSet>(); map.forEach((k,v)->restored.put(k,BitSet.valueOf(v.toLongArray()))); assertEquals(map,restored);
        }
        p.placedCell(segment,1); p.placedCell(segment,2); assertTrue(p.placed.get(0));
        assertTrue(p.blockedCells.isEmpty()); assertTrue(p.missingCells.isEmpty());
        p.unplace(Map.of(0,Map.of("stone",1.0))); assertFalse(p.cellPlaced(0,0));
    }
    @Test void tcGA10_09_defaultsAndPartialData() {
        assertEquals(BuildPacing.Settings.DEFAULT,CivVillageData.parse("test",JsonParser.parseString("{}").getAsJsonObject()).construction());
        var s=CivVillageData.parse("test",JsonParser.parseString("{\"construction\":{\"ticksPerBlock\":30,\"tripSize\":7}}").getAsJsonObject()).construction();
        assertEquals(30,s.ticksPerBlock()); assertEquals(7,s.tripSize()); assertEquals(20,s.fetchTicks()); assertEquals(32,s.fetchRange());
    }
}
