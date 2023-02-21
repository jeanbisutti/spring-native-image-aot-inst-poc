package org.example;

import org.springframework.aot.hint.RuntimeHintsRegistrar;

public class RuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(org.springframework.aot.hint.RuntimeHints hints, ClassLoader classLoader) {
        hints.resources().registerResourceBundle("org.apache.commons.dbcp2.LocalStrings");
    }

}
