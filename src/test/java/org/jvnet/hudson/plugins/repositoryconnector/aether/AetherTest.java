package org.jvnet.hudson.plugins.repositoryconnector.aether;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AetherTest {

    @Test
    public void convertGivenNonHttpProxySettings() throws Exception {

        String result = Aether.convertHudsonNonProxyToJavaNonProxy("localhost\n*.google.com\n\napple.com");

        assertEquals("New lines should be replaced", result, "localhost|*.google.com|apple.com");
    }

    @Test
    public void convertGivenNullNonHttpProxySettings() throws Exception {
        assertEquals("New lines should be replaced", Aether.convertHudsonNonProxyToJavaNonProxy(null), "");
    }

}
