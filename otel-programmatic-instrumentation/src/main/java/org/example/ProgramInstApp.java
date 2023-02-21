package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportRuntimeHints;

@SpringBootApplication
@ImportRuntimeHints(RuntimeHints.class)
public class ProgramInstApp {

    public static void main(String[] args) {
        SpringApplication.run(ProgramInstApp.class, args);
    }

}