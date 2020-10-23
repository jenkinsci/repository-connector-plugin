package org.jvnet.hudson.plugins.repositoryconnector;

import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Rule;
import org.jvnet.hudson.plugins.repositoryconnector.aether.Aether;
import org.jvnet.hudson.plugins.repositoryconnector.aether.AetherBuilder;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Cause.UserIdCause;

abstract class AbstractArtifactIT {

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Mock
    protected Aether mockAether;

    @Mock
    protected AetherBuilder mockAetherBuilder;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        when(mockAetherBuilder.build()).thenReturn(mockAether);
    }
    
    protected Artifact createArtifact() {
        Artifact artifact = new Artifact("org.junit.jupiter", "junit-jupiter", "5.7.0");
        artifact.setExtension("jar");
        artifact.setTargetFileName("jupiter/junit.jar");

        return artifact;
    }
    
    protected FreeStyleBuild executeBuild(FreeStyleProject project) throws Exception {
        return jenkinsRule.assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0, new UserIdCause()).get());
    }

    protected FreeStyleProject getProject(String name) {
        return (FreeStyleProject) jenkinsRule.jenkins.getItem(name);
    }
}
