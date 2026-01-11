package com.codeexplainer.analyzer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.AnnotationVisitor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AnnotationExtractor - extracts annotation values from
 * bytecode.
 */
@DisplayName("AnnotationExtractor Tests")
class AnnotationExtractorTest {

    @Test
    @DisplayName("Should extract annotation name")
    void shouldExtractAnnotationName() {
        AnnotationExtractor extractor = new AnnotationExtractor("org.junit.jupiter.api.Test");

        assertEquals("org.junit.jupiter.api.Test", extractor.getAnnotationName());
    }

    @Test
    @DisplayName("Should extract simple values")
    void shouldExtractSimpleValues() {
        AnnotationExtractor extractor = new AnnotationExtractor("TestAnnotation");

        extractor.visit("stringValue", "Hello");
        extractor.visit("intValue", 42);
        extractor.visit("booleanValue", true);

        Map<String, Object> values = extractor.getValues();
        assertEquals("Hello", values.get("stringValue"));
        assertEquals(42, values.get("intValue"));
        assertEquals(true, values.get("booleanValue"));
    }

    @Test
    @DisplayName("Should extract enum values")
    void shouldExtractEnumValues() {
        AnnotationExtractor extractor = new AnnotationExtractor("TestAnnotation");

        extractor.visitEnum("priority", "Lcom/example/Priority;", "HIGH");

        Map<String, Object> values = extractor.getValues();
        assertEquals("com.example.Priority.HIGH", values.get("priority"));
    }

    @Test
    @DisplayName("Should extract array values")
    void shouldExtractArrayValues() {
        AnnotationExtractor extractor = new AnnotationExtractor("TestAnnotation");

        AnnotationVisitor arrayVisitor = extractor.visitArray("tags");
        arrayVisitor.visit(null, "tag1");
        arrayVisitor.visit(null, "tag2");
        arrayVisitor.visit(null, "tag3");
        arrayVisitor.visitEnd();

        Map<String, Object> values = extractor.getValues();
        @SuppressWarnings("unchecked")
        List<Object> tags = (List<Object>) values.get("tags");

        assertNotNull(tags);
        assertEquals(3, tags.size());
        assertTrue(tags.contains("tag1"));
        assertTrue(tags.contains("tag2"));
        assertTrue(tags.contains("tag3"));
    }

    @Test
    @DisplayName("Should extract enum array values")
    void shouldExtractEnumArrayValues() {
        AnnotationExtractor extractor = new AnnotationExtractor("TestAnnotation");

        AnnotationVisitor arrayVisitor = extractor.visitArray("priorities");
        arrayVisitor.visitEnum(null, "Lcom/example/Priority;", "HIGH");
        arrayVisitor.visitEnum(null, "Lcom/example/Priority;", "LOW");
        arrayVisitor.visitEnd();

        Map<String, Object> values = extractor.getValues();
        @SuppressWarnings("unchecked")
        List<Object> priorities = (List<Object>) values.get("priorities");

        assertNotNull(priorities);
        assertEquals(2, priorities.size());
        assertTrue(priorities.contains("com.example.Priority.HIGH"));
        assertTrue(priorities.contains("com.example.Priority.LOW"));
    }

    @Test
    @DisplayName("Should extract nested annotation values")
    void shouldExtractNestedAnnotationValues() {
        AnnotationExtractor extractor = new AnnotationExtractor("OuterAnnotation");

        AnnotationVisitor nested = extractor.visitAnnotation("inner", "Lcom/example/InnerAnnotation;");
        nested.visit("name", "nested-value");
        nested.visitEnd();

        Map<String, Object> values = extractor.getValues();
        @SuppressWarnings("unchecked")
        Map<String, Object> innerValues = (Map<String, Object>) values.get("inner");

        assertNotNull(innerValues);
        assertEquals("nested-value", innerValues.get("name"));
    }

    @Test
    @DisplayName("Should handle Class type values")
    void shouldHandleClassTypeValues() {
        AnnotationExtractor extractor = new AnnotationExtractor("TestAnnotation");

        // Simulate how ASM passes Class types via Type objects
        org.objectweb.asm.Type classType = org.objectweb.asm.Type.getType("Ljava/lang/String;");
        extractor.visit("type", classType);

        Map<String, Object> values = extractor.getValues();
        assertEquals("java.lang.String", values.get("type"));
    }

    @Test
    @DisplayName("Should handle empty annotation")
    void shouldHandleEmptyAnnotation() {
        AnnotationExtractor extractor = new AnnotationExtractor("EmptyAnnotation");

        Map<String, Object> values = extractor.getValues();
        assertNotNull(values);
        assertTrue(values.isEmpty());
    }

    @Test
    @DisplayName("Should handle multiple values")
    void shouldHandleMultipleValues() {
        AnnotationExtractor extractor = new AnnotationExtractor("MultiValueAnnotation");

        extractor.visit("name", "Test");
        extractor.visit("version", 1);
        extractor.visit("enabled", true);
        extractor.visitEnum("level", "Lcom/example/Level;", "INFO");

        AnnotationVisitor arrayVisitor = extractor.visitArray("tags");
        arrayVisitor.visit(null, "unit");
        arrayVisitor.visit(null, "test");
        arrayVisitor.visitEnd();

        Map<String, Object> values = extractor.getValues();
        assertEquals(5, values.size());
        assertEquals("Test", values.get("name"));
        assertEquals(1, values.get("version"));
        assertEquals(true, values.get("enabled"));
        assertEquals("com.example.Level.INFO", values.get("level"));

        @SuppressWarnings("unchecked")
        List<Object> tags = (List<Object>) values.get("tags");
        assertEquals(2, tags.size());
    }
}
