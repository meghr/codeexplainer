package com.codeexplainer.parser;

import com.codeexplainer.core.exception.JarParsingException;
import org.objectweb.asm.ClassReader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Reads Java .class files and provides ClassReader for bytecode analysis.
 */
@Component
public class ClassFileReader {

    // Magic number for Java class files
    private static final int CLASS_MAGIC = 0xCAFEBABE;

    /**
     * Reads a class file and returns a ClassReader for ASM analysis.
     *
     * @param classFilePath Path to the .class file
     * @return ClassReader for bytecode analysis
     * @throws JarParsingException if reading fails
     */
    public ClassReader read(String classFilePath) throws JarParsingException {
        return read(Paths.get(classFilePath));
    }

    /**
     * Reads a class file and returns a ClassReader for ASM analysis.
     *
     * @param classFilePath Path to the .class file
     * @return ClassReader for bytecode analysis
     * @throws JarParsingException if reading fails
     */
    public ClassReader read(Path classFilePath) throws JarParsingException {
        if (!Files.exists(classFilePath)) {
            throw new JarParsingException("Class file does not exist: " + classFilePath);
        }

        try {
            byte[] classBytes = Files.readAllBytes(classFilePath);
            validateClassFile(classBytes, classFilePath.toString());
            return new ClassReader(classBytes);
        } catch (IOException e) {
            throw new JarParsingException("Failed to read class file: " + classFilePath, e);
        }
    }

    /**
     * Reads a class file from an InputStream.
     *
     * @param inputStream Input stream containing class data
     * @param name        Name for error messages
     * @return ClassReader for bytecode analysis
     * @throws JarParsingException if reading fails
     */
    public ClassReader read(InputStream inputStream, String name) throws JarParsingException {
        try {
            byte[] classBytes = inputStream.readAllBytes();
            validateClassFile(classBytes, name);
            return new ClassReader(classBytes);
        } catch (IOException e) {
            throw new JarParsingException("Failed to read class: " + name, e);
        }
    }

    /**
     * Reads class bytes for further processing.
     *
     * @param classFilePath Path to the .class file
     * @return Raw class bytes
     * @throws JarParsingException if reading fails
     */
    public byte[] readBytes(Path classFilePath) throws JarParsingException {
        try {
            byte[] bytes = Files.readAllBytes(classFilePath);
            validateClassFile(bytes, classFilePath.toString());
            return bytes;
        } catch (IOException e) {
            throw new JarParsingException("Failed to read class bytes: " + classFilePath, e);
        }
    }

    /**
     * Checks if a file is a valid Java class file.
     *
     * @param path Path to check
     * @return true if valid class file
     */
    public boolean isValidClassFile(Path path) {
        if (!Files.exists(path) || !path.toString().endsWith(".class")) {
            return false;
        }
        try {
            byte[] bytes = Files.readAllBytes(path);
            return isValidClassBytes(bytes);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Gets Java version from class file major version.
     *
     * @param majorVersion The major version from class file
     * @return Java version string (e.g., "Java 17")
     */
    public String getJavaVersion(int majorVersion) {
        return switch (majorVersion) {
            case 45 -> "Java 1.1";
            case 46 -> "Java 1.2";
            case 47 -> "Java 1.3";
            case 48 -> "Java 1.4";
            case 49 -> "Java 5";
            case 50 -> "Java 6";
            case 51 -> "Java 7";
            case 52 -> "Java 8";
            case 53 -> "Java 9";
            case 54 -> "Java 10";
            case 55 -> "Java 11";
            case 56 -> "Java 12";
            case 57 -> "Java 13";
            case 58 -> "Java 14";
            case 59 -> "Java 15";
            case 60 -> "Java 16";
            case 61 -> "Java 17";
            case 62 -> "Java 18";
            case 63 -> "Java 19";
            case 64 -> "Java 20";
            case 65 -> "Java 21";
            case 66 -> "Java 22";
            default -> majorVersion > 66 ? "Java " + (majorVersion - 44) : "Unknown (" + majorVersion + ")";
        };
    }

    private void validateClassFile(byte[] bytes, String name) throws JarParsingException {
        if (!isValidClassBytes(bytes)) {
            throw new JarParsingException("Invalid class file (bad magic number): " + name);
        }
    }

    private boolean isValidClassBytes(byte[] bytes) {
        if (bytes == null || bytes.length < 4) {
            return false;
        }
        // Check for CAFEBABE magic number
        int magic = ((bytes[0] & 0xFF) << 24) |
                ((bytes[1] & 0xFF) << 16) |
                ((bytes[2] & 0xFF) << 8) |
                (bytes[3] & 0xFF);
        return magic == CLASS_MAGIC;
    }
}
