package org.example;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class AgentClass {

    public static void write() {
        System.out.println("Execute from byte code instrumentation.");
        String fileLocation = System.getProperty("file.location");
        System.out.println("fileLocation = " + fileLocation);
        if(fileLocation != null) {
            FileWriter fileWriter = buildFileWriter(fileLocation);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            printWriter.println("Ok");
            printWriter.flush();
        }
    }

    private static FileWriter buildFileWriter(String fileLocation) {
        try {
            return new FileWriter(fileLocation);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
