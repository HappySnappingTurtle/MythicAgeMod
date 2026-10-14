package io.github.happysnappingturtle.mythicage.devtools;

import io.github.happysnappingturtle.mythicage.core.MythicAgeIds;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForbiddenReferenceScannerTest {
    private static final List<String> MC = List.of("net/minecraft/");

    @Test
    void tc_t2_10_realCoreClassHasNoForbiddenReference() throws IOException {
        byte[] bytes;
        try (InputStream in = MythicAgeIds.class.getResourceAsStream("MythicAgeIds.class")) {
            bytes = in.readAllBytes();
        }
        assertTrue(ForbiddenReferenceScanner.findForbiddenReferences(bytes, MC).isEmpty());
    }

    @Test
    void tc_t2_11_internalNameIsDetected() {
        byte[] bytes = new ClassFileBuilder().utf8("net/minecraft/world/level/Level").classRef(1).build();
        assertEquals(List.of("net/minecraft/world/level/Level"),
                ForbiddenReferenceScanner.findForbiddenReferences(bytes, MC));
    }

    @Test
    void tc_t2_12_descriptorIsDetected() {
        byte[] bytes = new ClassFileBuilder().utf8("(Lnet/minecraft/core/BlockPos;)V").build();
        assertEquals(List.of("(Lnet/minecraft/core/BlockPos;)V"),
                ForbiddenReferenceScanner.findForbiddenReferences(bytes, MC));
    }

    @Test
    void tc_t2_13_similarPrefixIsNotDetected() {
        byte[] bytes = new ClassFileBuilder().utf8("net/minecraftforge/common/Foo")
                .utf8("(Lnet/minecraftforge/Bar;)V").build();
        assertTrue(ForbiddenReferenceScanner.findForbiddenReferences(bytes, MC).isEmpty());
    }

    @Test
    void tc_t2_14_longAndDoubleOccupyTwoSlots() {
        byte[] bytes = new ClassFileBuilder().longConstant(42L).doubleConstant(1.5)
                .utf8("net/minecraft/server/MinecraftServer").build();
        assertEquals(List.of("net/minecraft/server/MinecraftServer"),
                ForbiddenReferenceScanner.findForbiddenReferences(bytes, MC));
    }

    @Test
    void tc_t2_15_hitsAreDeduplicatedAndSorted() {
        byte[] bytes = new ClassFileBuilder().utf8("net/minecraft/B").utf8("net/minecraft/A")
                .utf8("net/minecraft/B").build();
        assertEquals(List.of("net/minecraft/A", "net/minecraft/B"),
                ForbiddenReferenceScanner.findForbiddenReferences(bytes, MC));
    }

    @Test
    void tc_t2b_31_exactClassMatchDoesNotHitRuntimeException() {
        byte[] bytes = new ClassFileBuilder().utf8("java/lang/RuntimeException")
                .utf8("(Ljava/lang/RuntimeException;)V").build();
        assertTrue(ForbiddenReferenceScanner.findForbiddenClasses(bytes, List.of("java/lang/Runtime")).isEmpty());
    }

    @Test
    void tc_t2b_31_exactClassMatchFindsRuntime() {
        byte[] bytes = new ClassFileBuilder().utf8("java/lang/Runtime").utf8("()Ljava/lang/Runtime;").build();
        assertEquals(List.of("()Ljava/lang/Runtime;", "java/lang/Runtime"),
                ForbiddenReferenceScanner.findForbiddenClasses(bytes, List.of("java/lang/Runtime")));
    }

    @Test
    void tc_t2b_32_networkReferenceIsDetected() {
        byte[] bytes = new ClassFileBuilder().utf8("java/net/Socket").classRef(1).build();
        assertEquals(List.of("java/net/Socket"),
                ForbiddenReferenceScanner.findForbiddenReferences(bytes, List.of("java/net/", "java/awt/")));
    }

    @Test
    void rejectsNonClassBytes() {
        assertThrows(IllegalArgumentException.class,
                () -> ForbiddenReferenceScanner.findForbiddenReferences(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, MC));
    }

    @Test
    void rejectsTruncatedClassFile() {
        byte[] full = new ClassFileBuilder().utf8("net/minecraft/A").build();
        byte[] truncated = Arrays.copyOf(full, 12);
        assertThrows(IllegalArgumentException.class,
                () -> ForbiddenReferenceScanner.findForbiddenReferences(truncated, MC));
    }

    @Test
    void rejectsPrefixWithoutTrailingSlash() {
        byte[] bytes = new ClassFileBuilder().utf8("x").build();
        assertThrows(IllegalArgumentException.class,
                () -> ForbiddenReferenceScanner.findForbiddenReferences(bytes, List.of("net/minecraft")));
    }
}
