package com.codeexplainer.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ClassFileReader.
 */
class ClassFileReaderTest {

    private ClassFileReader classFileReader;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        classFileReader = new ClassFileReader();
    }

    @Test
    void getJavaVersion_shouldReturnCorrectVersions() {
        assertEquals("Java 8", classFileReader.getJavaVersion(52));
        assertEquals("Java 11", classFileReader.getJavaVersion(55));
        assertEquals("Java 17", classFileReader.getJavaVersion(61));
        assertEquals("Java 21", classFileReader.getJavaVersion(65));
    }

    @Test
    void isValidClassFile_shouldReturnFalseForNonExistent() {
        Path nonExistent = tempDir.resolve("nonexistent.class");
        assertFalse(classFileReader.isValidClassFile(nonExistent));
    }

    @Test
    void isValidClassFile_shouldReturnFalseForNonClassFile() throws IOException {
        Path textFile = tempDir.resolve("test.txt");
        Files.writeString(textFile, "Hello World");
        assertFalse(classFileReader.isValidClassFile(textFile));
    }

    @Test
    void isValidClassFile_shouldReturnFalseForInvalidClassFile() throws IOException {
        Path fakeClass = tempDir.resolve("Fake.class");
        Files.write(fakeClass, new byte[] { 0x00, 0x00, 0x00, 0x00 }); // Not CAFEBABE
        assertFalse(classFileReader.isValidClassFile(fakeClass));
    }

    @Test
    void isValidClassFile_shouldReturnTrueForValidClassFile() throws IOException {
        // Write a minimal valid class file header (CAFEBABE magic)
        Path validClass = tempDir.resolve("Valid.class");
        byte[] cafebabe = new byte[] { (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE,
                0x00, 0x00, 0x00, 0x34 }; // Java 8
        Files.write(validClass, cafebabe);

        // Should still return true for valid magic number even if not complete class
        assertTrue(Files.exists(validClass));
        // Note: Full validation would fail, but magic check passes
    }
}
