package test;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

import java.io.IOException;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

public class JarAssert extends AbstractAssert<JarAssert, JarFile> {

    private final JarFile jarFile;

    protected JarAssert(JarFile jarFile) {
        super(jarFile, JarAssert.class);
        this.jarFile = jarFile;
    }

    public static JarAssert assertThat(JarFile actual) {
        return new JarAssert(actual);
    }

    public JarAssert hasManifest() {
        Manifest manifest = null;
        String manifestNotFoundMessage = buildManifestNotFoundMessageFrom(jarFile);
        try {
            manifest = this.jarFile.getManifest();
        } catch (IOException e) {
            failWithMessage(manifestNotFoundMessage);
        }
        if(manifest == null) {
            failWithMessage(manifestNotFoundMessage);
        }
        return this;
    }

    private String buildManifestNotFoundMessageFrom(JarFile jarFile) {
        return "Impossible to find manifest in " + jarFile.getName();
    }

    public JarAssert hasSameManifestAs(JarFile jarFile) {
        hasManifest();
        assertThat(jarFile).hasManifest();
        Manifest manifest = null;
        try {
            manifest = this.jarFile.getManifest();
        } catch (IOException e) {
            //Manifest presence checked before
        }
        Manifest manifestOfJarInParameter = null;
        try {
            manifestOfJarInParameter = jarFile.getManifest();
        } catch (IOException e) {
            //Manifest presence checked before
        }
        Assertions.assertThat(manifest.getMainAttributes()).isEqualTo(manifestOfJarInParameter.getMainAttributes());
        Assertions.assertThat(manifest.getEntries()).isEqualTo(manifestOfJarInParameter.getEntries());
        return this;

    }

    public JarAssert hasEntry(String entryName) {
        ZipEntry zipEntry = this.jarFile.getEntry(entryName);
        if(zipEntry == null) {
            failWithMessage("Can't find entry " + entryName + " in " + jarFile.getName());
        }
        return this;
    }

}
