package org.jvnet.hudson.plugins.repositoryconnector;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.PrintStream;

import org.junit.Test;
import org.jvnet.hudson.plugins.repositoryconnector.aether.Aether;
import org.jvnet.hudson.plugins.repositoryconnector.aether.AetherException;
import org.jvnet.hudson.plugins.repositoryconnector.util.TokenMacroExpander;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;

public class ArtifactDeployerTest extends AbstractArtifactTest {

    private ArtifactDeployer deployer;

    @Override
    public void before() throws Exception {
        super.before();

        deployer = new ArtifactDeployer(artifacts) {
            @Override
            Aether createAether(Run<?, ?> context, PrintStream console) {
                return mockAether;
            }

            @Override
            TokenMacroExpander createExpander(Run<?, ?> run, FilePath workspace, TaskListener listener) {
                return mockExpander;
            }
        };
        deployer.setRepositoryId("central");
    }

    @Test(expected = AetherException.class)
    public void testFailOnError() throws Exception {
        Artifact artifact = createArtifact(true);
        when(mockAether.deploy("central", artifact)).thenThrow(new AetherException("failed"));

        deployer.perform(mockRun, workspace, null, mockListener);
    }

    @Test
    public void testSkipDeployToRemote() throws Exception {
        Artifact artifact = createArtifact(true);
        artifact.setDeployToRemote(false);

        deployer.perform(mockRun, workspace, null, mockListener);

        verify(mockAether).install(artifact);
        verifyNoMoreInteractions(mockAether);
    }

    @Test
    public void testSuccess() throws Exception {
        Artifact artifact = createArtifact(true);

        deployer.perform(mockRun, workspace, null, mockListener);

        verify(mockAether).install(artifact);
        verify(mockAether).deploy("central", artifact);
    }

    @Test
    public void testSuccessOnError() throws Exception {
        Artifact artifact = createArtifact(false);
        when(mockAether.deploy("central", artifact)).thenThrow(new AetherException("failed"));

        deployer.perform(mockRun, workspace, null, mockListener);

        verify(mockAether).install(artifact);
        verify(mockAether).deploy("central", artifact);

        verify(mockPrintStream).println(anyString());
    }

    @Override
    protected Artifact createArtifact(boolean fail) throws Exception {
        Artifact artifact = super.createArtifact(fail);
        artifact.setTargetFileName(getTestJar().getAbsolutePath());

        return artifact;
    }
}
