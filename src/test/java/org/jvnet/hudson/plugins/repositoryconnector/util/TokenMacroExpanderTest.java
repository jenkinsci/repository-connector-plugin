package org.jvnet.hudson.plugins.repositoryconnector.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.PrintStream;

import org.junit.Test;
import org.jvnet.hudson.plugins.repositoryconnector.util.TokenMacroExpander;

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

        tokenExpander = createExpander(mockRun, workspace, mockListener);
    }

    @Test
    public void testNothingToExpand() throws Exception {
        Artifact artifact = createArtifact();

        Artifact expanded = tokenExpander.expand(artifact);

        // This list of asserts is alphabetized like in Artifact
        assertEquals(artifact.artifactId, expanded.artifactId);
        assertEquals(artifact.classifier, expanded.classifier);
        assertEquals(artifact.deployToLocal, expanded.deployToLocal);
        assertEquals(artifact.deployToRemote, expanded.deployToRemote);
        assertEquals(artifact.extension, expanded.extension);
        assertEquals(artifact.failOnError, expanded.failOnError);
        assertEquals(artifact.groupId, expanded.groupId);
        assertEquals(artifact.pomFile, expanded.pomFile);
        assertEquals(artifact.targetFileName, expanded.targetFileName);
        assertEquals(artifact.version, expanded.version);
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
        assertEquals(artifact.artifactId, expanded.artifactId);
        assertEquals(artifact.groupId, expanded.groupId);
        assertEquals(artifact.version, expanded.version);
        assertEquals(artifact.targetFileName, expanded.targetFileName);

        // optional params
        assertEquals("dummy", expanded.classifier);
        assertFalse(expanded.deployToLocal);
        assertFalse(expanded.deployToRemote);
        assertEquals("war", expanded.extension);
        assertTrue(expanded.failOnError);
        assertEquals(artifact.pomFile, expanded.pomFile);
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
