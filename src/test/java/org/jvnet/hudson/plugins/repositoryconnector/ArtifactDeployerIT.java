package org.jvnet.hudson.plugins.repositoryconnector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.jvnet.hudson.test.recipes.LocalData;

import hudson.model.FreeStyleProject;

public class ArtifactDeployerIT extends AbstractArtifactIT {

    @Test
    @LocalData
    public void testConfiguration() {
        FreeStyleProject project = getProject("test-job-configuration");
        // this should be the only resolver in the above job
        ArtifactDeployer resolver = (ArtifactDeployer) project.getPublishersList().get(0);

        assertTrue(resolver.isEnableRepositoryLogging());
        assertFalse(resolver.isEnableTransferLogging());
        assertEquals("central", resolver.getRepositoryId());

        assertEquals(1, resolver.getArtifacts().size());
        Artifact artifact = resolver.getArtifacts().get(0);

        assertEquals("org.junit.jupiter", artifact.getGroupId());
        assertEquals("junit-jupiter", artifact.getArtifactId());
        assertEquals("5.7.0", artifact.getVersion());
        assertEquals("target/junit-jupiter.jar", artifact.getTargetFileName());

        assertTrue(artifact.isFailOnError());
        assertTrue(artifact.isDeployToLocal());
        assertTrue(artifact.isDeployToRemote());
    }
}
