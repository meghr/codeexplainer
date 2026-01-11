package com.codeexplainer.analyzer;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.HashMap;
import java.util.Map;

/**
 * ASM AnnotationVisitor that extracts annotation values.
 * Supports nested annotations and arrays.
 */
public class AnnotationExtractor extends AnnotationVisitor {

    private final String annotationName;
    private final Map<String, Object> values = new HashMap<>();

    public AnnotationExtractor(String annotationName) {
        super(Opcodes.ASM9);
        this.annotationName = annotationName;
    }

    /**
     * Gets the extracted annotation values.
     */
    public Map<String, Object> getValues() {
        return values;
    }

    /**
     * Gets the annotation name.
     */
    public String getAnnotationName() {
        return annotationName;
    }

    @Override
    public void visit(String name, Object value) {
        if (value instanceof Type) {
            values.put(name, ((Type) value).getClassName());
        } else {
            values.put(name, value);
        }
    }

    @Override
    public void visitEnum(String name, String descriptor, String value) {
        String enumType = Type.getType(descriptor).getClassName();
        values.put(name, enumType + "." + value);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String name, String descriptor) {
        String nestedName = Type.getType(descriptor).getClassName();
        AnnotationExtractor nested = new AnnotationExtractor(nestedName);
        // Store as nested map when visitEnd is called
        return new AnnotationVisitor(Opcodes.ASM9) {
            @Override
            public void visit(String n, Object v) {
                nested.visit(n, v);
            }

            @Override
            public void visitEnd() {
                values.put(name, nested.getValues());
            }
        };
    }

    @Override
    public AnnotationVisitor visitArray(String name) {
        java.util.List<Object> arrayValues = new java.util.ArrayList<>();
        return new AnnotationVisitor(Opcodes.ASM9) {
            @Override
            public void visit(String n, Object value) {
                if (value instanceof Type) {
                    arrayValues.add(((Type) value).getClassName());
                } else {
                    arrayValues.add(value);
                }
            }

            @Override
            public void visitEnum(String n, String descriptor, String value) {
                String enumType = Type.getType(descriptor).getClassName();
                arrayValues.add(enumType + "." + value);
            }

            @Override
            public void visitEnd() {
                values.put(name, arrayValues);
            }
        };
    }
}
