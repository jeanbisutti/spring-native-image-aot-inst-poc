package org.example;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class SimpleJavaAgent {

    public static void premain(String agentArgs, Instrumentation inst) {
        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                if (className.equals("org/example/AppMain") || className.equals("org/example/SpringBootMain")) {
                    try {
                        ClassReader reader = new ClassReader(classfileBuffer);
                        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                        reader.accept(new  ClassVisitor(writer), ClassReader.EXPAND_FRAMES);
                        return writer.toByteArray();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return classfileBuffer;
            }
        });
    }

    public static class ClassVisitor extends org.objectweb.asm.ClassVisitor {
        public ClassVisitor(org.objectweb.asm.ClassVisitor classVisitor) {
            super(Opcodes.ASM5, classVisitor);
        }
        @Override
        public MethodVisitor visitMethod(int methodAccess, String methodName, String methodDesc, String signature, String[] exceptions) {
            MethodVisitor methodVisitor = cv.visitMethod(methodAccess, methodName, methodDesc, signature, exceptions);
            return new AdviceAdapter(Opcodes.ASM5, methodVisitor, methodAccess, methodName, methodDesc);
        }
    }
    public static class AdviceAdapter extends org.objectweb.asm.commons.AdviceAdapter {
        private String methodName;
        protected AdviceAdapter(int api, MethodVisitor methodVisitor, int methodAccess, String methodName, String methodDesc) {
            super(api, methodVisitor, methodAccess, methodName, methodDesc);
            this.methodName = methodName;
        }
        @Override
        protected void onMethodEnter() {
            if ("<init>".equals(methodName)|| "<clinit>".equals(methodName)) {
                return;
            }
            mv.visitMethodInsn(INVOKESTATIC, "org/example/AgentClass", "write", "()V", false);
        }

        @Override
        protected void onMethodExit(int i) {
            if ("<init>".equals(methodName) || "<clinit>".equals(methodName)) {
                return;
            }
        }
    }

}