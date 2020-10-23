package org.jvnet.hudson.plugins.repositoryconnector;

import org.jenkinsci.Symbol;
import org.jvnet.hudson.plugins.repositoryconnector.util.FormValidator;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;

/**
 * Represents an artifact to be resolved or uploaded.
 * 
 * @author domi
 */
public class Artifact extends AbstractDescribableImpl<Artifact> {

    public static final String DEFAULT_EXTENSION = "jar";

    private final String artifactId;

    private String classifier;

    // deployed artifacts only
    private boolean deployToLocal;

    // deployed artifacts only
    private boolean deployToRemote;

    private String extension;

    private boolean failOnError;

    private final String groupId;

    // deployed artifacts only
    private String pomFile;

    private String targetFileName;

    private final String version;

    @DataBoundConstructor
    public Artifact(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;

        this.deployToLocal = true;
        this.deployToRemote = true;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getClassifier() {
        return classifier;
    }

    public String getExtension() {
        return extension;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getPomFile() {
        return pomFile;
    }

    public String getTargetFileName() {
        return targetFileName;
    }

    public String getVersion() {
        return version;
    }

    public boolean isDeployToLocal() {
        return deployToLocal;
    }

    public boolean isDeployToRemote() {
        return deployToRemote;
    }

    public boolean isFailOnError() {
        return failOnError;
    }

    @DataBoundSetter
    public void setClassifier(String classifier) {
        this.classifier = Util.fixEmpty(classifier);
    }

    @DataBoundSetter
    public void setDeployToLocal(boolean deployToLocal) {
        this.deployToLocal = deployToLocal;
    }
    
    @DataBoundSetter
    public void setDeployToRemote(boolean deployToRemote) {
        this.deployToRemote = deployToRemote;
    }

    @DataBoundSetter
    public void setExtension(String extension) {
        this.extension = Util.fixEmpty(extension);
    }

    @DataBoundSetter
    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }

    @DataBoundSetter
    public void setPomFile(String pomFile) {
        this.pomFile = pomFile;
    }

    @DataBoundSetter
    public void setTargetFileName(String targetFileName) {
        this.targetFileName = Util.fixEmpty(targetFileName);
    }

    @Override
    public String toString() {
        return "[Artifact " + groupId + ":" + artifactId + ":" + extension + ":" + classifier + ":" + version + "]";
    }

    @Extension
    @Symbol("artifact")
    public static class DescriptorImpl extends Descriptor<Artifact> {

        public FormValidation doCheckArtifactId(@QueryParameter String value) {
            return FormValidator.validateArtifactId(value);
        }

        public FormValidation doCheckGroupId(@QueryParameter String value) {
            return FormValidator.validateGroupId(value);
        }

        public FormValidation doCheckVersion(@QueryParameter String value) {
            return FormValidator.validateVersion(value);
        }

        @Override
        public String getDisplayName() {
            return "artifact";
        }
    }
}
