package org.jvnet.hudson.plugins.repositoryconnector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.jvnet.hudson.plugins.repositoryconnector.aether.AetherBuilder;
import org.jvnet.hudson.plugins.repositoryconnector.aether.AetherBuilderFactory;
import org.jvnet.hudson.test.recipes.LocalData;

import hudson.FilePath;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Run;
import hudson.model.StringParameterDefinition;

public class ArtifactResolverIT extends AbstractArtifactIT {
   
    @Test
    @LocalData
    public void testConfiguration() {
        FreeStyleProject project = getProject("test-job-configuration");
        // this should be the only resolver in the above job
        ArtifactResolver resolver = (ArtifactResolver) project.getBuilders().get(0);

        assertTrue(resolver.isEnableRepositoryLogging());
        assertFalse(resolver.isEnableTransferLogging());
        assertEquals("central", resolver.getRepositoryId());
        assertEquals("target", resolver.getTargetDirectory());

        assertEquals(1, resolver.getArtifacts().size());
        Artifact artifact = resolver.getArtifacts().get(0);

        assertEquals("org.junit.jupiter", artifact.getGroupId());
        assertEquals("junit-jupiter", artifact.getArtifactId());
        assertEquals("5.7.0", artifact.getVersion());
        
        assertTrue(artifact.isFailOnError());
        assertTrue(artifact.isDeployToRemote());
    }

    @Test
    @LocalData
    @Ignore("not to be run as part of ci - connects to maven central")
    public void testLiveDownload() throws Exception {
        FreeStyleProject project = getProject("test-job-configuration");
        FreeStyleBuild build = executeBuild(project);

        FilePath[] resolved = build.getWorkspace().list("target/jupiter/junit.jar");
        assertEquals(1, resolved.length);
    }

    @Test
    public void testTokenExpansion() throws Exception {
        // jenkins won't copy the contents of an empty file so return a tiny, local jar instead
        File file = new File(this.getClass().getResource("test.jar").toURI());
        when(mockAether.resolve(any(), any())).thenReturn(file);

        ArtifactResolver resolver = createResolver(createTokenizedArtifact());
        resolver.setTargetDirectory("target");

        FreeStyleProject project = createProject(resolver);
        project.addProperty(createTokenProperties());

        FreeStyleBuild build = executeBuild(project);

        FilePath[] resolved = build.getWorkspace().list("target/jupiter/junit.jar");
        assertEquals(1, resolved.length);
    }

    private ArtifactResolver createArtifactResolver(List<Artifact> artifacts) {
        return new ArtifactResolver(artifacts, new AetherBuilderFactory(null, Collections.emptyList()) {
            @Override
            public AetherBuilder createAetherBuilder(Run<?, ?> context) {
                return mockAetherBuilder;
            }
        });
    }

    private FreeStyleProject createProject(ArtifactResolver resolver) throws IOException {
        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        project.getBuildersList().add(resolver);

        return project;
    }

    private ArtifactResolver createResolver(Artifact artifact) {
        return createArtifactResolver(Arrays.asList(artifact));
    }

    private Artifact createTokenizedArtifact() {
        Artifact artifact = new Artifact("${GROUP_ID}", "${ARTIFACT_ID}", "${VERSION}");
        artifact.setClassifier("${CLASSIFIER}");
        artifact.setExtension("${EXTENSION}");
        artifact.setTargetFileName("${TARGET}");

        return artifact;
    }

    private ParametersDefinitionProperty createTokenProperties() {
        // classifier intentionally set in definition below
        Artifact artifact = createArtifact();
        artifact.setExtension("jar");
        artifact.setTargetFileName("jupiter/junit.jar");

        return new ParametersDefinitionProperty(
                new StringParameterDefinition("GROUP_ID", artifact.getGroupId()),
                new StringParameterDefinition("ARTIFACT_ID", artifact.getArtifactId()),
                new StringParameterDefinition("VERSION", artifact.getVersion()),
                new StringParameterDefinition("CLASSIFIER", ""),
                new StringParameterDefinition("EXTENSION", artifact.getExtension()),
                new StringParameterDefinition("TARGET", artifact.getTargetFileName()));
    }
}
