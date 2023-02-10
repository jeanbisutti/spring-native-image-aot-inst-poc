package test;

import org.example.AotInstrumentation;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarFile;

import static org.assertj.core.api.Assertions.assertThat;
import static test.JarAssert.assertThat;

class UseCasesTest {

    @Test
    void aot_should_instrument_a_jar_from_a_simple_agent() throws IOException, InterruptedException {

        String javaAgentJarPath = findSimpleAgentPath();
        AotInstrumentation aotInstrumentation = new AotInstrumentation(javaAgentJarPath);
        String outputJarPath = findTargetPath() + File.separator + "simple-app-instrumented.jar";
        aotInstrumentation.apply(findSimpleAppPath(), outputJarPath, "org/example/AgentClass.class");

        // Verify the content of the instrumented JAR
        JarFile inJarFile = new JarFile(findSimpleAppPath());
        JarFile outJarFile = new JarFile(outputJarPath);

        assertThat(outJarFile).hasManifest().hasSameManifestAs(inJarFile).hasEntry("META-INF/maven/org.example/simple-app/pom.properties")
                .hasEntry("META-INF/maven/org.example/simple-app/pom.xml");

        // Launch the instrumented JAR and verify the ahead-of-time instrumentation is working
        String fileLocation = findTargetPath() + File.separator + "jar-with-simple-agent.txt";
        File file = new File(fileLocation);
        Files.deleteIfExists(file.toPath());
        String command[] = {"java", "-cp", outputJarPath, "-Dfile.location=" + fileLocation, "org.example.AppMain"};
        int exitCode = new ProcessBuilder(command).inheritIO().start().waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("Impossible ot run the instrumented jar: " + outputJarPath);
        }
        assertThat(file).exists();
    }

    @Test
    void aot_should_instrument_a_spring_boot_jar_from_a_simple_agent() throws IOException, InterruptedException {

        String javaAgentJarPath = findSimpleAgentPath();
        AotInstrumentation aotInstrumentation = new AotInstrumentation(javaAgentJarPath);
        String outputJarPath = findTargetPath() + File.separator + "spring-boot-app-instrumented.jar";
        String springBootJarPath = findSpringBootJarPath();
        aotInstrumentation.apply(springBootJarPath, outputJarPath, "org/example/AgentClass.class");

        // Verify the content of the instrumented JAR
        JarFile inJarFile = new JarFile(springBootJarPath);
        JarFile outJarFile = new JarFile(outputJarPath);

       assertThat(outJarFile).hasSameManifestAs(inJarFile).hasEntry("BOOT-INF/classes/org/example/AgentClass.class");

        // Launch the instrumented JAR and verify the ahead-of-time instrumentation is working
        String fileLocation = findTargetPath() + File.separator + "spring-boot-jar-with-simple-agent.txt";
        File file = new File(fileLocation);
        Files.deleteIfExists(file.toPath());
        String command[] = {"java", "-Dfile.location=" + fileLocation, "-jar", outputJarPath};

        int exitCode = new ProcessBuilder(command).inheritIO().start().waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("Impossible ot run the instrumented jar: " + outputJarPath);
        }
        assertThat(file).exists();
    }

    private String findSpringBootJarPath() {
        File currentFolder = Paths.get("").toFile();
        File parentFile = findParentFileOf(currentFolder);
        return parentFile.getAbsolutePath() + File.separator + "spring-boot-app" + File.separator + "target" + File.separator + "spring-boot-app.jar";
    }

    private static String findSimpleAppPath() {
        File currentFolder = Paths.get("").toFile();
        File parentFile = findParentFileOf(currentFolder);
        return parentFile.getAbsolutePath() + File.separator + "simple-app" + File.separator + "target" + File.separator + "simple-app.jar";
    }

    private static File findParentFileOf(File folder) {
        try {
            return folder.getCanonicalFile().getParentFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String findSimpleAgentPath() {
        File currentFolder = Paths.get("").toFile();
        File parentFile = findParentFileOf(currentFolder);
        return parentFile.getAbsolutePath() + File.separator + "simple-agent" + File.separator + "target" + File.separator + "simple-agent.jar";
    }

    private static String findTargetPath() {
        Path targetDirectory = Paths.get("target");
        return targetDirectory.toFile().getAbsolutePath();
    }

}
