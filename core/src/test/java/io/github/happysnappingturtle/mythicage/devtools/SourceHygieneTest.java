package io.github.happysnappingturtle.mythicage.devtools;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourceHygieneTest {

    @Test
    void tc_t2_20_systemOutIsReportedWithLineNumber() {
        List<SourceHygiene.Violation> v = SourceHygiene.findViolations("class A {\n  void f() { System.out.println(\"x\"); }\n}\n");
        assertEquals(1, v.size());
        assertEquals(2, v.get(0).line());
    }

    @Test
    void tc_t2_21_systemErrAndPrintStackTraceAreReported() {
        List<SourceHygiene.Violation> v = SourceHygiene.findViolations("System.err.print(x);\ne.printStackTrace();\n");
        assertEquals(2, v.size());
        assertEquals(1, v.get(0).line());
        assertEquals(2, v.get(1).line());
    }

    @Test
    void tc_t2_22_fullLineCommentIsIgnored() {
        assertTrue(SourceHygiene.findViolations("    // System.out.println 只是注释").isEmpty());
    }

    @Test
    void tc_t2_23_allowMarkerExemptsLine() {
        assertTrue(SourceHygiene.findViolations("System.out.println(x); // hygiene-allow: 测试桩").isEmpty());
    }

    @Test
    void tc_t2_24_bareNameWithoutMemberAccessIsNotReported() {
        assertTrue(SourceHygiene.findViolations("String s = \"System.out\";").isEmpty());
    }

    @Test
    void tc_t2_25_emptyInputsAndCrlf() {
        assertTrue(SourceHygiene.findViolations("").isEmpty());
        assertTrue(SourceHygiene.findViolations("\n\n").isEmpty());
        List<SourceHygiene.Violation> v = SourceHygiene.findViolations("a();\r\nb();\r\nSystem.out.println();\r\n");
        assertEquals(1, v.size());
        assertEquals(3, v.get(0).line());
    }

    @Test
    void tc_t2_26_fullCallInsideStringLiteralIsReported_knownLimitation() {
        assertEquals(1, SourceHygiene.findViolations("LOGGER.info(\"勿用 System.out.println\");").size());
    }
}
