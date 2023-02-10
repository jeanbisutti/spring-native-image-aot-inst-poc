package org.example;

import java.lang.instrument.Instrumentation;

/** To have access to have access to the byte code of the loaded classes */
public class JavaClassInterceptorAgent {

    public static void premain(String agentArgs, Instrumentation inst) {
        LoadedClassesInterceptor loadedClassesInterceptor = new LoadedClassesInterceptor();
        inst.addTransformer(loadedClassesInterceptor);
    }

}
