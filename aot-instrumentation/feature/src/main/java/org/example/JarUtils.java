package org.example;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Optional;
import java.util.jar.*;

import static org.example.IoUtils.write;

class JarUtils {

    private JarUtils() {
    }

    static File packageAsJar(File tempDir, String jarName, Collection<Class<?>> classes, Optional<Class<?>> classWithPremain) {

        Manifest manifest = createManifest(classWithPremain);
        File jarFile = new File(tempDir + File.separator + jarName);

        try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(jarFile.toPath()), manifest)
        ) {
            if (classWithPremain.isPresent()) {
                addToJar(classWithPremain.get(), jarOutputStream);
            }
            for (Class<?> clazz : classes) {
                addToJar(clazz, jarOutputStream);
            }
            jarOutputStream.closeEntry();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return jarFile;
    }

    private static Manifest createManifest(Optional<Class<?>> classWithPremain) {
        Manifest manifest = new Manifest();
        if (classWithPremain.isPresent()) {
            Attributes mainAttributes = manifest.getMainAttributes();
            mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
            mainAttributes.put(new Attributes.Name("Premain-Class"), classWithPremain.get().getName());
        }
        return manifest;
    }

    private static void addToJar(Class<?> classWithPremain, JarOutputStream jarOutputStream) throws IOException {
        String resource = '/' + classWithPremain.getName().replace('.', '/') + ".class";
        InputStream classInput = JarUtils.class.getResourceAsStream(resource);
        JarEntry jarEntry = new JarEntry(classWithPremain.getName().replace('.', '/') + ".class");
        jarOutputStream.putNextEntry(jarEntry);
        write(classInput, jarOutputStream);
        classInput.close();
    }

    static JarFile newJar(String stringPath) {
        try {
            return new JarFile(stringPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static Manifest findManifest(JarFile jar) {
        try {
            return jar.getManifest();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static boolean jarContainsEntry(JarFile inputJar, String entry) {
        try (JarInputStream jarInputStream = new JarInputStream(Files.newInputStream(Paths.get(inputJar.getName())))) {
            JarEntry jarEntry;
            while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
                String jarEntryName = jarEntry.getName();
                if(jarEntryName.equals(entry)) {
                    return true;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return false;
    }
}