package org.jvnet.hudson.plugins.repositoryconnector.aether;

import hudson.model.Cause;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.tasks.Shell;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.plugins.repositoryconnector.Artifact;
import org.jvnet.hudson.plugins.repositoryconnector.ArtifactResolver;

import java.io.File;
import java.io.PrintStream;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class AetherTest {

    private Aether sut;

    @Before
    public void setup()
    {
        sut = new Aether( new File("jenkinstest"), System.out, false );
    }

    @Test
    public void convertGivenNonHttpProxySettings() throws Exception {

        String result = sut.convertHudsonNonProxyToJavaNonProxy("localhost\n*.google.com\n\napple.com");

        assertEquals("New lines should be replaced", result, "localhost|*.google.com|apple.com");
    }
}
