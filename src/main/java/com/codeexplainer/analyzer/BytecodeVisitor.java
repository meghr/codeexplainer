package com.codeexplainer.analyzer;

import com.codeexplainer.core.model.ClassMetadata;
import com.codeexplainer.core.model.FieldMetadata;
import com.codeexplainer.core.model.MethodMetadata;
import org.objectweb.asm.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * ASM ClassVisitor that extracts metadata from Java bytecode.
 * This is the core bytecode analysis component.
 */
public class BytecodeVisitor extends ClassVisitor {

    private static final Logger log = LoggerFactory.getLogger(BytecodeVisitor.class);

    private final ClassMetadata classMetadata;
    private final boolean includePrivateMethods;

    public BytecodeVisitor() {
        this(false);
    }

    public BytecodeVisitor(boolean includePrivateMethods) {
        super(Opcodes.ASM9);
        this.classMetadata = new ClassMetadata();
        this.includePrivateMethods = includePrivateMethods;
    }

    /**
     * Gets the extracted class metadata after visiting.
     */
    public ClassMetadata getClassMetadata() {
        return classMetadata;
    }

    @Override
    public void visit(int version, int access, String name, String signature,
            String superName, String[] interfaces) {
        // Parse class name
        String fullName = name.replace('/', '.');
        classMetadata.setFullyQualifiedName(fullName);

        int lastDot = fullName.lastIndexOf('.');
        if (lastDot > 0) {
            classMetadata.setPackageName(fullName.substring(0, lastDot));
            classMetadata.setClassName(fullName.substring(lastDot + 1));
        } else {
            classMetadata.setPackageName("");
            classMetadata.setClassName(fullName);
        }

        // Parse superclass
        if (superName != null) {
            classMetadata.setSuperClassName(superName.replace('/', '.'));
        }

        // Parse interfaces
        if (interfaces != null) {
            for (String iface : interfaces) {
                classMetadata.getInterfaces().add(iface.replace('/', '.'));
            }
        }

        // Parse access modifiers and determine class type
        classMetadata.setAccessModifiers(parseAccessModifiers(access));
        classMetadata.setClassType(determineClassType(access));

        // Store version info
        classMetadata.setMajorVersion(version & 0xFFFF);
        classMetadata.setMinorVersion((version >> 16) & 0xFFFF);

        log.debug("Visiting class: {} ({})", fullName, classMetadata.getClassType());
    }

    @Override
    public void visitSource(String source, String debug) {
        classMetadata.setSourceFile(source);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        String annotationName = Type.getType(descriptor).getClassName();
        classMetadata.getAnnotations().add(annotationName);
        return new AnnotationExtractor(annotationName);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor,
            String signature, Object value) {
        FieldMetadata field = new FieldMetadata();
        field.setFieldName(name);
        field.setType(Type.getType(descriptor).getClassName());

        if (signature != null) {
            field.setGenericType(parseSignature(signature));
        }

        field.setAccessModifiers(parseAccessModifiers(access));
        field.setStatic((access & Opcodes.ACC_STATIC) != 0);
        field.setFinal((access & Opcodes.ACC_FINAL) != 0);
        field.setVolatile((access & Opcodes.ACC_VOLATILE) != 0);
        field.setTransient((access & Opcodes.ACC_TRANSIENT) != 0);
        field.setDefaultValue(value);

        classMetadata.getFields().add(field);

        return new FieldVisitor(Opcodes.ASM9) {
            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                String annotationName = Type.getType(desc).getClassName();
                if (field.getAnnotations() == null) {
                    field.setAnnotations(new ArrayList<>());
                }
                field.getAnnotations().add(annotationName);
                return new AnnotationExtractor(annotationName);
            }
        };
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor,
            String signature, String[] exceptions) {
        // Skip private methods if configured
        if (!includePrivateMethods && (access & Opcodes.ACC_PRIVATE) != 0) {
            return null;
        }

        MethodMetadata method = new MethodMetadata();
        method.setMethodName(name);
        method.setDescriptor(descriptor);
        method.setSignature(signature);
        method.setAccessModifiers(parseAccessModifiers(access));
        method.setStatic((access & Opcodes.ACC_STATIC) != 0);
        method.setAbstract((access & Opcodes.ACC_ABSTRACT) != 0);
        method.setSynchronized((access & Opcodes.ACC_SYNCHRONIZED) != 0);
        method.setNative((access & Opcodes.ACC_NATIVE) != 0);

        // Parse return type
        Type returnType = Type.getReturnType(descriptor);
        method.setReturnType(returnType.getClassName());

        // Parse parameters
        Type[] argTypes = Type.getArgumentTypes(descriptor);
        for (int i = 0; i < argTypes.length; i++) {
            com.codeexplainer.core.model.ParameterInfo param = new com.codeexplainer.core.model.ParameterInfo();
            param.setIndex(i);
            param.setType(argTypes[i].getClassName());
            param.setName("arg" + i); // Will be updated if debug info available
            method.getParameters().add(param);
        }

        // Parse exceptions
        if (exceptions != null) {
            for (String exc : exceptions) {
                method.getExceptions().add(exc.replace('/', '.'));
            }
        }

        classMetadata.getMethods().add(method);

        // Return a MethodVisitor to analyze method body
        return new MethodAnalyzer(Opcodes.ASM9, method);
    }

    private Set<String> parseAccessModifiers(int access) {
        Set<String> modifiers = new HashSet<>();

        if ((access & Opcodes.ACC_PUBLIC) != 0)
            modifiers.add("public");
        if ((access & Opcodes.ACC_PRIVATE) != 0)
            modifiers.add("private");
        if ((access & Opcodes.ACC_PROTECTED) != 0)
            modifiers.add("protected");
        if ((access & Opcodes.ACC_STATIC) != 0)
            modifiers.add("static");
        if ((access & Opcodes.ACC_FINAL) != 0)
            modifiers.add("final");
        if ((access & Opcodes.ACC_ABSTRACT) != 0)
            modifiers.add("abstract");
        if ((access & Opcodes.ACC_SYNCHRONIZED) != 0)
            modifiers.add("synchronized");
        if ((access & Opcodes.ACC_VOLATILE) != 0)
            modifiers.add("volatile");
        if ((access & Opcodes.ACC_TRANSIENT) != 0)
            modifiers.add("transient");
        if ((access & Opcodes.ACC_NATIVE) != 0)
            modifiers.add("native");

        return modifiers;
    }

    private ClassMetadata.ClassType determineClassType(int access) {
        if ((access & Opcodes.ACC_ANNOTATION) != 0) {
            return ClassMetadata.ClassType.ANNOTATION;
        }
        if ((access & Opcodes.ACC_INTERFACE) != 0) {
            return ClassMetadata.ClassType.INTERFACE;
        }
        if ((access & Opcodes.ACC_ENUM) != 0) {
            return ClassMetadata.ClassType.ENUM;
        }
        if ((access & Opcodes.ACC_RECORD) != 0) {
            return ClassMetadata.ClassType.RECORD;
        }
        if ((access & Opcodes.ACC_ABSTRACT) != 0) {
            return ClassMetadata.ClassType.ABSTRACT_CLASS;
        }
        return ClassMetadata.ClassType.CLASS;
    }

    private String parseSignature(String signature) {
        // Simplified signature parsing - just return as-is for now
        // Full parsing would use SignatureReader
        return signature;
    }
}
