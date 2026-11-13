package io.github.happysnappingturtle.mythicage.core.village.gen;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** GA-14 单元测试 TC-GA14-01～04：门前通道的列与台阶高度。 */
class DoorAccessTest {
    private static final BuildingSpec HOUSE = BuildingSpec.parse(JsonParser.parseString("""
            {"id": "greek:house_small", "civ": "greek", "template": "t", "size": [9, 7, 10], "floor_y": 1, "frame": [-1, -1, -1],
             "door": {"pos": [4, 1, 9], "facing": "south"}}""").getAsJsonObject());
    private static final BuildingSpec GRANARY = BuildingSpec.parse(JsonParser.parseString("""
            {"id": "greek:granary", "civ": "greek", "template": "t", "size": [11, 9, 9], "floor_y": 1, "frame": [-1, -1, -1],
             "door": {"pos": [9, 1, 4], "facing": "east", "width": 2}}""").getAsJsonObject());

    private static Box box(BuildingSpec s, Pos o, Turn t) {
        return TemplateTransform.worldBox(o, s.sizeX(), s.sizeY(), s.sizeZ(), t);
    }

    @Test
    void tc01_gateOnEdge() {
        Pos o = new Pos(100, 64, 100);
        var door = DoorAccess.door(HOUSE, o, Turn.R0);
        assertEquals(new Pos(104, 65, 109), door.pos());
        assertEquals("south", door.facing());
        assertEquals(1, HOUSE.doorWidth(), "默认宽 1");
        List<DoorAccess.Column> c = DoorAccess.corridor(box(HOUSE, o, Turn.R0), door, 5);
        assertEquals(5, c.size());
        assertEquals(new DoorAccess.Column(104, 110, 1, "south"), c.get(0), "院门在占地边缘：外面第一列就是第 1 级");
        assertEquals(new DoorAccess.Column(104, 114, 5, "south"), c.get(4));
    }

    @Test
    void tc02_doubleDoorBehindEaves() {
        Pos o = new Pos(0, 64, 0);
        var door = DoorAccess.door(GRANARY, o, Turn.R0);
        assertEquals(new Pos(9, 65, 4), door.pos());
        List<DoorAccess.Column> c = DoorAccess.corridor(box(GRANARY, o, Turn.R0), door, 3);
        assertEquals(6, c.size(), "宽 2：每级 2 列");
        assertEquals(new DoorAccess.Column(11, 4, 1, "east"), c.get(0), "跳过屋檐那一列（x = 10 还在占地里）");
        assertEquals(new DoorAccess.Column(11, 5, 1, "east"), c.get(1), "第二列在朝东的顺时针一侧（南）");
        assertEquals(new DoorAccess.Column(13, 5, 3, "east"), c.get(5));
    }

    @Test
    void tc03_rotated() {
        // 实机 ma_ga3b 的谷仓：原点 (−159, 67, 1056)，转 270°
        Pos o = new Pos(-159, 67, 1056);
        var door = DoorAccess.door(GRANARY, o, Turn.R270);
        assertEquals(new Pos(-155, 68, 1047), door.pos(), "门下半格（实机 /execute 查到的位置）");
        assertEquals("north", door.facing());
        List<DoorAccess.Column> c = DoorAccess.corridor(box(GRANARY, o, Turn.R270), door, 2);
        assertEquals(new DoorAccess.Column(-155, 1045, 1, "north"), c.get(0), "1046 是屋檐列，挡土墙在 1045");
        assertEquals(new DoorAccess.Column(-154, 1045, 1, "north"), c.get(1), "第二扇门在东边");
        assertEquals(new DoorAccess.Column(-155, 1044, 2, "north"), c.get(2));
    }

    @Test
    void tc04_stairRule() {
        int floor = 68;
        assertFalse(DoorAccess.needsStair(68, floor, 1), "地表顶面 y69 = 地面 + 1：走得上去");
        assertTrue(DoorAccess.needsStair(69, floor, 1), "挡土墙压顶在 y69：要台阶");
        assertEquals(68, DoorAccess.stairY(floor, 1));
        assertEquals(70, DoorAccess.stairY(floor, 3));
        assertTrue(DoorAccess.needsStair(70, floor, 2));
        assertFalse(DoorAccess.needsStair(69, floor, 2));
    }
}
