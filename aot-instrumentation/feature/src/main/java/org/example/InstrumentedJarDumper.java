package org.example;

import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.*;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;

import static org.example.IoUtils.write;
import static org.example.JarUtils.findManifest;


/**
 * To dump the content of a JAR after its instrumentation by a Java agent.
 * This class is packaged as a JAR that why this class needs to be public.
 *
 * @see AotInstrumentation
 */
public class InstrumentedJarDumper {

    static final Map<String, byte[]> BYTE_CODE_BY_CLASS_NAME_WITH_SLASHES = new HashMap<>();

    private static int noClassDefFoundErrorCount = 0;

    public static void main(String[] args) {
        String inputJarPath = args[0];
        String outputJarPath = args[1];
        String tempDirPath = args[2];
        boolean springBootExecutableJar = Boolean.parseBoolean(args[3]);
        boolean noClassDefFoundErrorReportEnabled = Boolean.parseBoolean(args[4]);
        createJarWithInstrumentedClasses(outputJarPath, JarUtils.newJar(inputJarPath), tempDirPath, springBootExecutableJar, noClassDefFoundErrorReportEnabled);
        if (noClassDefFoundErrorReportEnabled) {
            System.out.println("NoClassDefFoundError count: " + noClassDefFoundErrorCount);
        }
    }

    private static void createJarWithInstrumentedClasses(String outputJarPath, JarFile inputJar, String tempDirPath, boolean springBootExecutableJar, boolean noClassDefFoundErrorReportEnabled) {

        Manifest manifestOfInputJar = findManifest(inputJar);

        if (springBootExecutableJar) {
            createdFolderForInstrumentedJars(tempDirPath);
        }

        try (JarInputStream jarIn = new JarInputStream(Files.newInputStream(Paths.get(inputJar.getName())));
             JarOutputStream jarOutput = new JarOutputStream(Files.newOutputStream(Paths.get(outputJarPath)), manifestOfInputJar)) {
            JarEntry jarEntryOfInputJar;
            while ((jarEntryOfInputJar = jarIn.getNextJarEntry()) != null) {
                if (jarEntryOfInputJar.getName().equals("META-INF/MANIFEST.MF")) {
                    // Sometimes (for example tomcat-embed-core-10.1.5.jar) the Manifest file is parsed as an entry but we give the manifest as a parameter of the JarOutputStream below
                    continue;
                }
                OutputEntryData outputEntryData = buildOutputEntryData(inputJar, tempDirPath, springBootExecutableJar, noClassDefFoundErrorReportEnabled, jarIn, jarEntryOfInputJar);
                jarOutput.putNextEntry(outputEntryData.jarEntry);
                write(outputEntryData.inputStream, jarOutput);
                jarOutput.closeEntry();
            }
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

    }

    private static OutputEntryData buildOutputEntryData(JarFile inputJar, String tempDirPath, boolean springBootExecutableJar, boolean noClassDefFoundErrorReportEnabled, JarInputStream jarIn, JarEntry jarEntryOfInputJar) throws IOException {
        if (isJavaClass(jarEntryOfInputJar) && !jarEntryOfInputJar.getName().startsWith("org/springframework/boot/loader/") // Spring add Java classes (org.springframework.boot.loader package) to a JAR to make it executable. These classes added at build time are not instrumented.
                && !jarEntryOfInputJar.getName().startsWith("META-INF/versions") // We don't instrument the classes of a specific Java version for the multi-release JAR files.
                && !jarEntryOfInputJar.getName().equals("module-info.class") // Sometimes this class is not included in META-INF/versions but at the root level
        ) {
            try {
                return buildOutputDataForClass(jarEntryOfInputJar);
            } catch (NoClassDefFoundError noClassDefFoundError) {
                // A Spring library can use provided dependencies. If it is not provided in the classpath, some classes can't be loaded.
                if (noClassDefFoundErrorReportEnabled) {
                    noClassDefFoundErrorCount++;
                    printNoClassDefFoundError(inputJar, jarEntryOfInputJar, noClassDefFoundError);
                }
                return new OutputEntryData(jarEntryOfInputJar, jarIn);
            }

        } else if (springBootExecutableJar && isJar(jarEntryOfInputJar)) {
            return buildOutputDataForJar(tempDirPath, jarEntryOfInputJar, noClassDefFoundErrorReportEnabled);
        }
        return new OutputEntryData(jarEntryOfInputJar, jarIn);
    }

    private static OutputEntryData buildOutputDataForJar(String tempDirPath, JarEntry jarEntryOfInputJar, boolean noClassDefFoundErrorReportEnabled) throws IOException {

        String pathOfEmbeddedJar = tempDirPath + File.separator + jarEntryOfInputJar.getName();
        JarFile embeddedInputJar = new JarFile(pathOfEmbeddedJar);
        String embeddedOutputJarPath = tempDirPath + File.separator + "instrumented" + File.separator + jarEntryOfInputJar.getName();
        createJarWithInstrumentedClasses(embeddedOutputJarPath, embeddedInputJar, "", false, noClassDefFoundErrorReportEnabled);

        JarEntry nestedJarEntry = new JarEntry(jarEntryOfInputJar.getName());

        byte[] bytes = Files.readAllBytes(FileSystems.getDefault().getPath(embeddedOutputJarPath));
        int length = bytes.length;

        nestedJarEntry.setSize(length);

        // Spring nested jar files must be stored without compression
        nestedJarEntry.setMethod(ZipEntry.STORED);
        nestedJarEntry.setCompressedSize(length);

        CRC32 crc32 = new CRC32();
        crc32.update(bytes, 0, bytes.length);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        nestedJarEntry.setCrc(crc32.getValue());

        return new OutputEntryData(nestedJarEntry, inputStream);
    }

    private static void printNoClassDefFoundError(JarFile inputJar, JarEntry jarEntryOfInputJar, NoClassDefFoundError
            noClassDefFoundError) {
        System.out.println(findClassNameFrom(jarEntryOfInputJar) + " can't be loaded in " + inputJar.getName() + " (probably provided dependency not in the classpath)");
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        noClassDefFoundError.printStackTrace(printWriter);
        System.out.println(stringWriter);
    }

    private static OutputEntryData buildOutputDataForClass
            (JarEntry jarEntryOfInputJar) {

        String classNameWithDots = findClassNameFrom(jarEntryOfInputJar);

        loadJavaClass(classNameWithDots); // Load class to apply instrumentation

        JarEntry classEntry = new JarEntry(jarEntryOfInputJar.getName());
        String classNameWithSlashes = jarEntryOfInputJar.getName().replace(".class", "").replace("BOOT-INF/classes/", "");
        byte[] bytecode = BYTE_CODE_BY_CLASS_NAME_WITH_SLASHES.get(classNameWithSlashes);
        classEntry.setSize(bytecode.length);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytecode);

        return new OutputEntryData(classEntry, inputStream);
    }

    static class OutputEntryData {

        final JarEntry jarEntry;

        final InputStream inputStream;

        private OutputEntryData(JarEntry jarEntry, InputStream inputStream) {
            this.jarEntry = jarEntry;
            this.inputStream = inputStream;
        }
    }


    private static String findClassNameFrom(JarEntry jarEntryOfInputJar) {
        String classNameWithSlashes = jarEntryOfInputJar.getName().replace(".class", "").replace("BOOT-INF/classes/", "");
        String classNameWithDots = classNameWithSlashes.replace('/', '.');
        return classNameWithDots.replace("BOOT-INF.classes.", "");
    }

    private static void createdFolderForInstrumentedJars(String tempDirPath) {
        File folderForInstrumentedJars = new File(tempDirPath + File.separator + "instrumented" + File.separator + "BOOT-INF" + File.separator + "lib");
        try {
            Files.createDirectories(folderForInstrumentedJars.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isJavaClass(JarEntry entry) {
        return entry.getName().endsWith(".class");
    }

    private static boolean isJar(JarEntry jarEntryOfInputJar) {
        return jarEntryOfInputJar.getName().endsWith(".jar");
    }

    private static void loadJavaClass(String className) {
        try {
            Class.forName(className, false, InstrumentedJarDumper.class.getClassLoader());
        } catch (ClassNotFoundException classNotFoundException) {
            throw new RuntimeException(classNotFoundException);
        }
    }

}
