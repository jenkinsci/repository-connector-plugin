package org.jvnet.hudson.plugins.repositoryconnector.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.file.Files;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.plugins.repositoryconnector.Artifact;
import org.jvnet.hudson.plugins.repositoryconnector.util.TokenMacroExpander;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;

public class TokenMacroExpanderTest {

    @Mock
    protected TaskListener mockListener;

    @Mock
    protected PrintStream mockPrintStream;

    @Mock
    protected Run<?, ?> mockRun;

    protected FilePath workspace;

    private TokenMacroExpander tokenExpander;

    @After
    public void after() throws Exception {
        workspace.deleteRecursive();
    }

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(mockListener.getLogger()).thenReturn(mockPrintStream);

        workspace = new FilePath(Files.createTempDirectory(null).toFile());

        tokenExpander = new TokenMacroExpander(mockRun, mockListener, workspace);
    }

    @Test
    public void testNothingToExpand() throws Exception {
        Artifact artifact = createArtifact();

        Artifact expanded = tokenExpander.expand(artifact);

        // This list of asserts is alphabetized like in Artifact
        assertEquals(artifact.getArtifactId(), expanded.getArtifactId());
        assertEquals(artifact.getClassifier(), expanded.getClassifier());
        assertEquals(artifact.getDeployToLocal(), expanded.getDeployToLocal());
        assertEquals(artifact.getDeployToRemote(), expanded.getDeployToRemote());
        assertEquals(artifact.getExtension(), expanded.getExtension());
        assertEquals(artifact.getFailOnError(), expanded.getFailOnError());
        assertEquals(artifact.getGroupId(), expanded.getGroupId());
        assertEquals(artifact.getPomFile(), expanded.getPomFile());
        assertEquals(artifact.getTargetFileName(), expanded.getTargetFileNam()));
        assertEquals(artifact.getVersion(), expanded.getVersion());
    }

    @Test
    public void testNothingToExpandWithOptionalParams() throws Exception {
        Artifact artifact = createArtifact();
        artifact.setClassifier("dummy");
        artifact.setDeployToLocal(false);
        artifact.setDeployToRemote(false);
        artifact.setExtension("war");
        artifact.setFailOnError(true);
        artifact.setPomFile(getTestJar().getAbsolutePath());

        Artifact expanded = tokenExpander.expand(artifact);

        // required params
        assertEquals(artifact.getArtifactId(), expanded.getArtifactId());
        assertEquals(artifact.getGroupId(), expanded.getGroupId());
        assertEquals(artifact.getVersion(), expanded.getVersion());
        assertEquals(artifact.getTargetFileName(), expanded.getTargetFileNam()));

        // optional params
        assertEquals("dummy", expanded.getClassifier());
        assertFalse(expanded.getDeployToLocal());
        assertFalse(expanded.getDeployToRemote());
        assertEquals("war", expanded.getExtension());
        assertTrue(expanded.getFailOnError());
        assertEquals(artifact.getPomFile(), expanded.getPomFile());
    }

    protected File getTestJar() throws URISyntaxException {
        return new File(this.getClass().getResource("test.jar").toURI());
    }

    protected File getTestPom() throws URISyntaxException {
        return new File(this.getClass().getResource("test-pom.xml").toURI());
    }

    protected Artifact createArtifact() throws Exception {
        Artifact artifact = new Artifact("org.junit.jupiter", "junit-jupiter", "5.7.0");
        artifact.setTargetFileName(getTestJar().getAbsolutePath());

        return artifact;
    }
}
