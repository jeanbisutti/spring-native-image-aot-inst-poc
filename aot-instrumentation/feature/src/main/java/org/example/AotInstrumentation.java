package org.example;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.jar.*;

import static org.example.IoUtils.write;
import static org.example.JarUtils.*;

/**
 * This class allows you to instrument a JAR before starting an application: Ahead-of-Time instrumentation (AoT).
 */
public final class AotInstrumentation {
    private static final String PRINT_ERROR_LINKAGE_PROPERTY = "aot.print.error-linkage";
    private final String javaAgentJarPath;

    public AotInstrumentation(String javaAgentJarPath) {
        this.javaAgentJarPath = javaAgentJarPath;
    }

    public void apply(String inputJarPath, String outputJarPath, String agentJarEntryStartWith) {

        System.out.println("Start ahead-of-time instrumentation.");
        long start = System.currentTimeMillis();

        JarFile inputJar = newJar(inputJarPath);
        JarFile javaAgentJar = newJar(javaAgentJarPath);

        boolean springBootExecutableJar = jarContainsEntry(inputJar, "BOOT-INF/classpath.idx");

        File tempDir = createTempDir("aot-");

        createJarWithInstrumentedClasses(tempDir, inputJar, outputJarPath, springBootExecutableJar);

        // We do this to avoid the instrumentation of agent classes
        addSomeAgentFilesToTheInstrumentedJar(javaAgentJar, outputJarPath, agentJarEntryStartWith, springBootExecutableJar);

        long durationInSeconds = (System.currentTimeMillis() - start) / 1000;
        System.out.println("Ahead-of-time instrumentation has finished in " + durationInSeconds + " s.");
        System.out.println();
    }

    private void createJarWithInstrumentedClasses(File tempDir, JarFile inputJar, String outputJarPath, boolean springBootExecutableJar) {

        File jarToDumpTheInstrumentedClasses = packageAsJar(tempDir, InstrumentedJarDumper.class.getSimpleName(), Arrays.asList(InstrumentedJarDumper.class, InstrumentedJarDumper.OutputEntryData.class, JarUtils.class, IoUtils.class), Optional.empty());

        String classPath;
        if (springBootExecutableJar) {
            explodeClassesAndJarsInDir(inputJar, tempDir);
            classPath = "\"" + jarToDumpTheInstrumentedClasses.getAbsolutePath() + File.pathSeparator + (tempDir.getAbsolutePath() + File.separator + "BOOT-INF" + File.separator + "classes")
                    + File.pathSeparator + (tempDir.getAbsolutePath() + File.separator + "BOOT-INF" + File.separator + "lib" + File.separator + "*") + "\"";
        } else {
            classPath = inputJar.getName() + File.pathSeparator + jarToDumpTheInstrumentedClasses.getAbsolutePath();
        }

        File byteCodeInterceptorJar = packageAsJar(tempDir, JavaClassInterceptorAgent.class.getSimpleName(), Collections.singleton(LoadedClassesInterceptor.class), Optional.of(JavaClassInterceptorAgent.class));

        boolean printErrorLinkage = Boolean.getBoolean(PRINT_ERROR_LINKAGE_PROPERTY);

        String[] command = {"java", "-javaagent:" + javaAgentJarPath, "-javaagent:" + byteCodeInterceptorJar.getAbsolutePath(), "-cp", classPath, InstrumentedJarDumper.class.getName(), inputJar.getName(), outputJarPath, tempDir.getAbsolutePath(), String.valueOf(springBootExecutableJar), String.valueOf(printErrorLinkage)};
        execute(command);

    }

    private void explodeClassesAndJarsInDir(JarFile jar, File dir) {
        try (JarInputStream jarInputStream = new JarInputStream(Files.newInputStream(Paths.get(jar.getName())))) {
            JarEntry jarEntry;
            while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
                String jarEntryName = jarEntry.getName();
                if (((jarEntryName.startsWith("BOOT-INF/classes/") && jarEntryName.endsWith(".class")))
                        || (jarEntryName.startsWith("BOOT-INF/lib/")) && jarEntryName.endsWith(".jar")) {

                    File file = new File(dir.getAbsolutePath() + File.separator + jarEntryName);

                    Files.createDirectories(new File(file.getParent()).toPath());

                    try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                        write(jarInputStream, fileOutputStream);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private static File createTempDir(String prefix) {
        try {
            return new File(Files.createTempDirectory(prefix).toFile().getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void execute(String[] command) {
        int exitCode;
        try {
            exitCode = new ProcessBuilder(command).inheritIO().start().waitFor();
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
        if (exitCode != 0) {
            throw new IllegalStateException("Could not instrument jar");
        }
    }

    private void addSomeAgentFilesToTheInstrumentedJar(JarFile javaAgentJar, String outputJarPath
            , String agentJarEntryStartWith, boolean springBootJar) {

        String jarWithoutExt = outputJarPath.split(".jar")[0];
        File fileWithNewName = new File(jarWithoutExt + "-without-agent-files.jar");
        try {
            Files.deleteIfExists(fileWithNewName.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        new File(outputJarPath).renameTo(fileWithNewName);
        String absolutePath = fileWithNewName.getAbsolutePath();
        Manifest manifestOfInstrumentedJar = findManifest(newJar(absolutePath));

        try (JarInputStream agentJar = new JarInputStream(Files.newInputStream(Paths.get(javaAgentJar.getName())));
             JarInputStream instrumentedJar = new JarInputStream(Files.newInputStream(fileWithNewName.toPath()));
             JarOutputStream jarOutput = new JarOutputStream(Files.newOutputStream(Paths.get(outputJarPath)), manifestOfInstrumentedJar)) {
            copyAllTheEntriesOfInstrumentedJar(instrumentedJar, jarOutput);
            copyAgentEntries(agentJarEntryStartWith, agentJar, jarOutput, springBootJar);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void copyAgentEntries(String agentJarEntryStartWith, JarInputStream agentJar, JarOutputStream jarOutput, boolean springBootJar) throws IOException {
        JarEntry jarEntryOfAgent;
        while ((jarEntryOfAgent = agentJar.getNextJarEntry()) != null) {
            String jarEntryName = jarEntryOfAgent.getName();
            if (jarEntryName.startsWith(agentJarEntryStartWith)) {
                JarEntry outjarEntry = jarEntryOfAgent;
                if (springBootJar) {
                    outjarEntry = new JarEntry("BOOT-INF/classes/" + jarEntryOfAgent);
                }
                jarOutput.putNextEntry(outjarEntry);
                write(agentJar, jarOutput);
                jarOutput.closeEntry();
            }
        }
    }

    private static void copyAllTheEntriesOfInstrumentedJar(JarInputStream instrumentedJar, JarOutputStream jarOutput) throws IOException {
        JarEntry jarEntryOfInstrumentedJar;
        while ((jarEntryOfInstrumentedJar = instrumentedJar.getNextJarEntry()) != null) {
            jarOutput.putNextEntry(jarEntryOfInstrumentedJar);
            write(instrumentedJar, jarOutput);
            jarOutput.closeEntry();
        }
    }

}
