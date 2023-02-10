package org.example;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class IoUtils {
    private IoUtils() {
    }

    static void write(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[1024];
        int index;
        while ((index = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, index);
        }
    }
}
