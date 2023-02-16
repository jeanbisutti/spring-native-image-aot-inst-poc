# Ahead-of-time (AOT) instrumentation

## Use case 

Apply the instrumentation written for a Java agent but at build time (ahead-of-time).

## API

```java
    AotInstrumentation aotInstrumentation = new AotInstrumentation(javaAgent);
    aotInstrumentation.apply(appJarPath, outputJarPath, filePatternOfAgentFilesToCopy);
```

You can give any agent as a parameter of `AotInstrumentation` constructor, whatever the instrument library used (ASM, Byte Buddy, AspectJ, ...).

## How it works

1) The input jar classes are loaded (see `InstrumentedJarDumper.java` and `AotInstrumentation.java` for the definition of the classpath).
2) A Java agent runs after the Java agent given as a parameter of `AotInstrumentation` constructor and intercepts the instrumented classes (see `JavaClassInterceptorAgent.java`).
3) The content of the input JAR with the instrumented classes are saved on disk (see `InstrumentedJarDumper.java`).
4) Some Java agent classes are added (see `AotInstrumentation.java`).

For a Spring Boot executable JAR, the included JARs are also instrumented.

## Limitations

The presented mechanism provides ahead-of-time instrumentation to a JAR containing the application code.
However, this library does not instrument the JDK  classes, such as the ForkJoinPool. This can potentially breaks the OpenTelemetry context's propagation for asynchronous calls.

NB: We could add a feature to this AOT instrumentation component to save instrumented JDK classes and use after -Xbootclasspath/p or --patch-module java.desktop. However, this would not work for GraalVM.

