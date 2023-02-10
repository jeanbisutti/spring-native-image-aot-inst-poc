package org.example;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

class LoadedClassesInterceptor implements ClassFileTransformer {

    @Override
    public byte[] transform(ClassLoader loader, String classNameWithSlashes, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        InstrumentedJarDumper.BYTE_CODE_BY_CLASS_NAME_WITH_SLASHES.put(classNameWithSlashes, classfileBuffer);
        return classfileBuffer;
    }

}
