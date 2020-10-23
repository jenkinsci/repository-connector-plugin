package org.jvnet.hudson.plugins.repositoryconnector;

import static org.mockito.Mockito.when;

import java.io.File;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import com.google.common.io.Files;

import org.junit.After;
import org.junit.Before;
import org.jvnet.hudson.plugins.repositoryconnector.aether.Aether;
import org.jvnet.hudson.plugins.repositoryconnector.util.TokenMacroExpander;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;

abstract class AbstractArtifactTest {

    protected List<Artifact> artifacts;

    @Mock
    protected Aether mockAether;

    @Mock
    protected TokenMacroExpander mockExpander;

    @Mock
    protected TaskListener mockListener;

    @Mock
    protected PrintStream mockPrintStream;

    @Mock
    protected Run<?, ?> mockRun;

    protected FilePath workspace;

    @After
    public void after() throws Exception {
        workspace.deleteRecursive();
    }

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(mockListener.getLogger()).thenReturn(mockPrintStream);

        artifacts = new ArrayList<>();
        workspace = new FilePath(Files.createTempDir());
    }

    protected File getTestJar() throws URISyntaxException {
        return new File(this.getClass().getResource("test.jar").toURI());
    }

    @SuppressWarnings("unused")
    protected Artifact createArtifact(boolean failOnError) throws Exception {
        Artifact artifact = new Artifact("org.junit.jupiter", "junit-jupiter", "5.7.0");
        artifact.setFailOnError(failOnError);

        artifacts.add(artifact);
        when(mockExpander.expand(artifact)).thenReturn(artifact);

        return artifact;
    }
}
