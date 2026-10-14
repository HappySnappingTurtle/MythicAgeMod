package io.github.happysnappingturtle.mythicage.devtools.remote;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemoteProtocolTest {

    @Test
    void tc_t2b_01_parsesValidCommand() throws Exception {
        RemoteProtocol.Command c = RemoteProtocol.parse("{\"id\":1,\"cmd\":\"state\"}");
        assertEquals(1, c.id());
        assertEquals("state", c.cmd());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{ broken", "[1,2]", "42"})
    void tc_t2b_02_rejectsNonObject(String line) {
        var e = assertThrows(RemoteProtocol.RemoteProtocolException.class, () -> RemoteProtocol.parse(line));
        assertTrue(e.getMessage().startsWith("不是合法 JSON"), e.getMessage());
    }

    @Test
    void tc_t2b_03_rejectsMissingFields() {
        assertEquals("缺少 id 字段", assertThrows(RemoteProtocol.RemoteProtocolException.class,
                () -> RemoteProtocol.parse("{\"cmd\":\"state\"}")).getMessage());
        assertEquals("缺少 cmd 字段", assertThrows(RemoteProtocol.RemoteProtocolException.class,
                () -> RemoteProtocol.parse("{\"id\":3}")).getMessage());
        assertEquals("缺少 id 字段", assertThrows(RemoteProtocol.RemoteProtocolException.class,
                () -> RemoteProtocol.parse("{\"id\":null,\"cmd\":\"state\"}")).getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"id\":1.5,\"cmd\":\"state\"}", "{\"id\":\"1\",\"cmd\":\"state\"}"})
    void tc_t2b_04_rejectsNonIntegerId(String line) {
        assertEquals("id 必须是整数", assertThrows(RemoteProtocol.RemoteProtocolException.class,
                () -> RemoteProtocol.parse(line)).getMessage());
    }

    @Test
    void tc_t2b_05_resultKeepsNullFields() {
        String line = RemoteProtocol.result(7, "wait", true, null, 99, List.of(), null);
        assertTrue(line.contains("\"error\":null"), line);
        assertTrue(line.contains("\"data\":null"), line);
        JsonObject o = JsonParser.parseString(line).getAsJsonObject();
        assertEquals(7, o.get("id").getAsLong());
        assertEquals(99, o.get("tick").getAsLong());
    }

    @Test
    void tc_t2b_06_resultEscapesChatAndChinese() {
        String line = RemoteProtocol.result(2, "chat", false, "失败：\"引号\"", 5,
                List.of("给予了 3 个 [钻石]", "他说 \"你好\""), null);
        JsonObject o = JsonParser.parseString(line).getAsJsonObject();
        assertEquals("失败：\"引号\"", o.get("error").getAsString());
        assertEquals("他说 \"你好\"", o.getAsJsonArray("chat").get(1).getAsString());
    }

    @Test
    void cmdMustBeString() {
        assertEquals("cmd 必须是字符串", assertThrows(RemoteProtocol.RemoteProtocolException.class,
                () -> RemoteProtocol.parse("{\"id\":1,\"cmd\":5}")).getMessage());
    }
}
