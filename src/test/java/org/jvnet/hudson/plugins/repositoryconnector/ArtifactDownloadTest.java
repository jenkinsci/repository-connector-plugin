package org.jvnet.hudson.plugins.repositoryconnector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Result;
import hudson.model.StringParameterDefinition;
import hudson.model.Cause.UserCause;
import hudson.tasks.Builder;

public class ArtifactDownloadTest {

    @Rule
    public JenkinsRule          j = new JenkinsRule();

    @Test
    public void downloadWithFixVersionAndFixTargetDirAndTargetName() throws Exception {
        j.jenkins.getInjector().injectMembers(this);

        FreeStyleProject p = j.createFreeStyleProject();
        
        Artifact a = new Artifact("commons-logging", "commons-logging", "1.0.4");
        a.setTargetFileName("myJar.jar");
        
        ArtifactResolver resolver = createArtifactResolver(null, a);
        resolver.setTargetDirectory("target");
        
        p.getBuildersList().add(resolver);
        p.getBuildersList().add(new VerifyBuilder("target/myJar.jar"));

        j.assertBuildStatus(Result.SUCCESS, p.scheduleBuild2(0, new UserCause()).get());
    }
    
    @Test
    public void downloadWithFixVersionAndTargetName() throws Exception {
        j.jenkins.getInjector().injectMembers(this);

        FreeStyleProject p = j.createFreeStyleProject();

        Artifact a = new Artifact("commons-logging", "commons-logging", "1.0");
        a.setTargetFileName("myJar.jar");
        
        ArtifactResolver resolver = createArtifactResolver(null, a);
        
        p.getBuildersList().add(resolver);
        p.getBuildersList().add(new VerifyBuilder("myJar.jar"));

        j.assertBuildStatus(Result.SUCCESS, p.scheduleBuild2(0, new UserCause()).get());
    }
    
    @Test
    public void downloadWithFixVersion() throws Exception {
        j.jenkins.getInjector().injectMembers(this);

        FreeStyleProject p = j.createFreeStyleProject();

        Artifact a = new Artifact("commons-logging", "commons-logging", "1.0.1");        
        ArtifactResolver resolver = createArtifactResolver(null, a);
        
        p.getBuildersList().add(resolver);
        p.getBuildersList().add(new VerifyBuilder("commons-logging-1.0.1.jar"));

        j.assertBuildStatus(Result.SUCCESS, p.scheduleBuild2(0, new UserCause()).get());
    }    
    
    @Test
    public void downloadWithVersionAsStringParameter() throws Exception {
        j.jenkins.getInjector().injectMembers(this);

        FreeStyleProject p = j.createFreeStyleProject();

        Artifact a = new Artifact("${MY_ARTID}", "${MY_GROUP}", "${MY_VERSION}");
        a.setClassifier("${MY_CLASSIFIER}");
        a.setExtension("${MY_EXT}");
        a.setTargetFileName("${MY_FILE}");
        
        ArtifactResolver resolver = createArtifactResolver(null, a);
        
        p.getBuildersList().add(resolver);
        p.getBuildersList().add(new VerifyBuilder("aFile.jar"));

     // @formatter:off
        ParametersDefinitionProperty parametersDefinitionProperty = new ParametersDefinitionProperty(new StringParameterDefinition("MY_VERSION", "1.0.4"),
                                                                                                     new StringParameterDefinition("MY_ARTID", "commons-logging"),
                                                                                                     new StringParameterDefinition("MY_GROUP", "commons-logging"),
                                                                                                     new StringParameterDefinition("MY_CLASSIFIER", ""),
                                                                                                     new StringParameterDefinition("MY_EXT", "jar"),
                                                                                                     new StringParameterDefinition("MY_FILE", "aFile.jar"));
        p.addProperty(parametersDefinitionProperty);
     // @formatter:on

        j.assertBuildStatus(Result.SUCCESS, p.scheduleBuild2(0, new UserCause()).get());
    }    

    private ArtifactResolver createArtifactResolver(String targetDir, Artifact... artifacts) {
        ArtifactResolver resolver = new ArtifactResolver(Arrays.asList(artifacts));
        resolver.setTargetDirectory(targetDir);
        
        resolver.setEnableRepoLogging(false);
        resolver.setReleaseUpdatePolicy("always");
        resolver.setSnapshotUpdatePolicy("always");

        return resolver;
    }
    
    private static final class VerifyBuilder extends Builder {

        private final String expectedFile;

        public VerifyBuilder(String expectedFile) {
            this.expectedFile=expectedFile;
        }

        @Override
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
            FilePath f = new FilePath(build.getWorkspace(), expectedFile);
            Assert.assertTrue("File not available: " + f.getRemote(), f.exists());
            return true;
        }
    }    

}
