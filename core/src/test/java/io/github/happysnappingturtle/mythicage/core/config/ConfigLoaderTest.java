package io.github.happysnappingturtle.mythicage.core.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigLoaderTest {
    private static final TestSchema S = TestSchema.INSTANCE;

    private static ConfigLoadResult<TestSchema.Value> load(String json) {
        return ConfigLoader.load(S, json, null);
    }

    private static void assertSingleWarn(ConfigLoadResult<?> r, String path) {
        assertEquals(1, r.issues().size(), r.issues().toString());
        ConfigIssue issue = r.issues().get(0);
        assertEquals(ConfigIssue.Severity.WARN, issue.severity());
        assertEquals(path, issue.path());
    }

    @Test
    void tc_t3_01_validCompleteFile() {
        var r = load("{\"amount\":42,\"ratio\":0.25,\"mode\":\"slow\",\"inner\":{\"count\":7,\"flag\":false}}");
        assertEquals(new TestSchema.Value(42, 0.25, TestSchema.Mode.SLOW, new TestSchema.Inner(7, false), false), r.value());
        assertTrue(r.issues().isEmpty());
        assertFalse(r.usedFallback());
    }

    @Test
    void tc_t3_02_emptyObjectGivesDefaults() {
        var r = load("{}");
        assertEquals(TestSchema.DEFAULTS, r.value());
        assertTrue(r.issues().isEmpty());
    }

    @Test
    void tc_t3_03_wrongTypeFallsBack() {
        var r = load("{\"amount\":\"10\"}");
        assertEquals(10, r.value().amount());
        assertSingleWarn(r, "amount");
    }

    @Test
    void tc_t3_04_outOfRangeFallsBackWithoutClamping() {
        var r = load("{\"ratio\":1.5}");
        assertEquals(0.5, r.value().ratio());
        assertSingleWarn(r, "ratio");
        assertTrue(r.issues().get(0).message().contains("[0.0, 1.0]"));
    }

    @Test
    void tc_t3_05_invalidEnumFallsBackAndListsChoices() {
        var r = load("{\"mode\":\"turbo\"}");
        assertEquals(TestSchema.Mode.FAST, r.value().mode());
        assertSingleWarn(r, "mode");
        assertTrue(r.issues().get(0).message().contains("[fast, slow]"));
    }

    @Test
    void tc_t3_06_unknownKeysAreReportedWithFullPath() {
        var r = load("{\"extra\":1,\"inner\":{\"foo\":true}}");
        assertEquals(TestSchema.DEFAULTS, r.value());
        List<String> paths = r.issues().stream().map(ConfigIssue::path).sorted().toList();
        assertEquals(List.of("extra", "inner.foo"), paths);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{ \"a\": ", "[1,2]", "", "   ", "42"})
    void tc_t3_07_unparsableWholeFile(String text) {
        var noLast = ConfigLoader.load(S, text, null);
        assertTrue(noLast.usedFallback());
        assertEquals(TestSchema.DEFAULTS, noLast.value());
        assertEquals(ConfigIssue.Severity.ERROR, noLast.issues().get(0).severity());

        var last = new TestSchema.Value(1, 0.1, TestSchema.Mode.SLOW, new TestSchema.Inner(1, false), false);
        var withLast = ConfigLoader.load(S, text, last);
        assertTrue(withLast.usedFallback());
        assertSame(last, withLast.value());
        assertTrue(withLast.hasErrors());
    }

    @Test
    void tc_t3_08_newerSchemaVersionWarnsButReads() {
        var r = load("{\"schemaVersion\":99,\"amount\":5}");
        assertEquals(5, r.value().amount());
        assertSingleWarn(r, "schemaVersion");
    }

    @Test
    void tc_t3_09_missingOrStringSchemaVersion() {
        assertTrue(load("{\"amount\":5}").issues().isEmpty());
        var r = load("{\"schemaVersion\":\"1\",\"amount\":5}");
        assertEquals(5, r.value().amount());
        assertSingleWarn(r, "schemaVersion");
    }

    @Test
    void tc_t3_10_nestedObjectWithWrongTypeUsesDefaultsForSubtree() {
        var r = load("{\"inner\":3}");
        assertEquals(TestSchema.DEFAULTS.inner(), r.value().inner());
        assertSingleWarn(r, "inner");
    }

    @Test
    void tc_t3_11_lockedFieldIsForced() {
        var r = load("{\"locked\":true}");
        assertFalse(r.value().locked());
        assertSingleWarn(r, "locked");
    }

    @Test
    void tc_t3_12_fractionalIntegerIsTypeMismatch() {
        var r = load("{\"amount\":10.5}");
        assertEquals(10, r.value().amount());
        assertSingleWarn(r, "amount");
    }

    @Test
    void integerWrittenWithTrailingZeroIsAccepted() {
        assertEquals(20, load("{\"amount\":20.0}").value().amount());
    }

    @Test
    void jsonNullIsTreatedAsMissing() {
        var r = load("{\"amount\":null}");
        assertEquals(10, r.value().amount());
        assertTrue(r.issues().isEmpty());
    }

    @Test
    void defaultFileRoundTripsWithoutIssues() {
        var r = load(ConfigLoader.defaultFileContent(S));
        assertEquals(TestSchema.DEFAULTS, r.value());
        assertTrue(r.issues().isEmpty(), r.issues().toString());
    }
}
