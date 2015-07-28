package org.jvnet.hudson.plugins.repositoryconnector.aether;

import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class AetherTest {

    private Aether sut;

    @Before
    public void setup() {
        sut = new Aether(new File("jenkinstest"), System.out, false);
    }

    @Test
    public void convertGivenNonHttpProxySettings() throws Exception {

        String result = sut.convertHudsonNonProxyToJavaNonProxy("localhost\n*.google.com\n\napple.com");

        assertEquals("New lines should be replaced", result, "localhost|*.google.com|apple.com");
    }

    @Test
    public void convertGivenNullNonHttpProxySettings() throws Exception {
        assertEquals("New lines should be replaced", sut.convertHudsonNonProxyToJavaNonProxy(null), "");
    }
}
