package com.ecolink.api;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EcopointsP2ApplicationTests {

    @Test
    void mainMethodExists() throws Exception {
        Method mainMethod = EcopointsP2Application.class.getMethod("main", String[].class);
        assertTrue(Modifier.isStatic(mainMethod.getModifiers()));
    }
}
