package test;

import org.example.AotInstrumentation;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ApplyAotInstrumentation {

   @Disabled
    @Test
    void apply_aot_to_spring_boot_jar() {

        String agentPath = findAgentPath();
        System.out.println("agentPath = " + agentPath);

        AotInstrumentation aotInstrumentation = new AotInstrumentation(agentPath);

        aotInstrumentation.apply(findSpringBootJarPath(), findTargetPath() + File.separator + "spring-boot-3-app-instrumented.jar", "org.example");

    }

    private String findSpringBootJarPath() {
        File currentFolder = Paths.get("").toFile();
        File parentFile = findParentFileOf(currentFolder);
        return parentFile.getAbsolutePath() + File.separator + "spring-boot-app-demo" + File.separator + "target" + File.separator + "spring-boot3-app-1.0-SNAPSHOT.jar";
    }

    private static String findAgentPath() {
        File currentFolder = Paths.get("").toFile();
        File parentFile = findParentFileOf(currentFolder);
        return parentFile.getAbsolutePath() + File.separator + "java-agent" + File.separator + "target" + File.separator + "java-agent-1.0-SNAPSHOT.jar";
    }

    private static File findParentFileOf(File folder) {
        try {
            return folder.getCanonicalFile().getParentFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String findTargetPath() {
        Path targetDirectory = Paths.get("target");
        return targetDirectory.toFile().getAbsolutePath();
    }


}
