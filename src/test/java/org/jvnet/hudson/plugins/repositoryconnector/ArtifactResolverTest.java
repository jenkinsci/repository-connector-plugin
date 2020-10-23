package org.jvnet.hudson.plugins.repositoryconnector;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintStream;

import org.junit.Test;
import org.jvnet.hudson.plugins.repositoryconnector.aether.Aether;
import org.jvnet.hudson.plugins.repositoryconnector.aether.AetherException;
import org.jvnet.hudson.plugins.repositoryconnector.util.TokenMacroExpander;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;

public class ArtifactResolverTest extends AbstractArtifactTest {
   
    private ArtifactResolver resolver;
   
    @Override
    public void before() throws Exception {
        super.before();
        
        resolver = new ArtifactResolver(artifacts) {
            @Override
            Aether createAether(Run<?, ?> context, PrintStream console) {
                return mockAether;
            }

            @Override
            TokenMacroExpander createExpander(Run<?, ?> run, FilePath workspace, TaskListener listener) {
                return mockExpander;
            }
        };
    }

    @Test
    public void testSuccess() throws Exception {
        Artifact artifact = createArtifact(true);
        when(mockAether.resolve(any(), eq(artifact))).thenReturn(getTestJar());

        resolver.perform(mockRun, workspace, null, mockListener);

        FilePath[] resolved = workspace.list("test.jar");
        assertEquals(1, resolved.length);
    }

    @Test(expected = AetherException.class)
    public void testFailOnError() throws Exception {
        Artifact artifact = createArtifact(true);
        when(mockAether.resolve(any(), eq(artifact))).thenThrow(new AetherException("failed"));

        resolver.perform(mockRun, workspace, null, mockListener);
    }

    @Test
    public void testSuccessOnError() throws Exception {
        Artifact artifact = createArtifact(false);
        when(mockAether.resolve(any(), eq(artifact))).thenThrow(new AetherException("failed"));

        resolver.perform(mockRun, workspace, null, mockListener);

        FilePath[] resolved = workspace.list("test.jar");
        assertEquals(0, resolved.length);

        verify(mockPrintStream).println(anyString());
    }
}
