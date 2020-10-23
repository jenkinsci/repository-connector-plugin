package org.jvnet.hudson.plugins.repositoryconnector.aether;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AetherBuilderTest {

    @Test
    public void testConvertNonProxyHosts() {
        String nonHosts = AetherBuilder.convertJenkinsNoProxyHosts("localhost\n*.google.com\n\napple.com");
        assertEquals("localhost|*.google.com|apple.com", nonHosts);
    }
}
