package org.jvnet.hudson.plugins.repositoryconnector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.jvnet.hudson.test.recipes.LocalData;

import hudson.model.FreeStyleProject;
import hudson.model.ParametersDefinitionProperty;

public class VersionParameterDefinitionIT extends AbstractArtifactIT {

    @Test
    @LocalData
    public void testConfiguration() {
        FreeStyleProject project = getProject("test-job-configuration");
        // this should be the only definition in the above job
        VersionParameterDefinition definition = (VersionParameterDefinition) project.getProperty(ParametersDefinitionProperty.class)
                .getParameterDefinitions()
                .get(0);

        assertNotNull(definition);

        assertEquals("JUNIT_VERSION", definition.getName());

        assertEquals("central", definition.getRepositoryId());
        assertEquals("org.junit.jupiter", definition.getGroupId());
        assertEquals("junit-jupiter", definition.getArtifactId());

        assertFalse(definition.isOldestFirst());
        assertEquals("5", definition.getLimit());

        assertTrue(definition.isIncludeReleases());
        assertTrue(definition.isIncludeSnapshots());
        assertTrue(definition.isUseRelease());
        assertTrue(definition.isUseLatest());
    }
}
