package org.jvnet.hudson.plugins.repositoryconnector;

import java.io.Serializable;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;

/**
 * Represents an artifact to be resolved or uploaded.
 * 
 * @author domi
 * 
 */
public class Artifact extends AbstractDescribableImpl<Artifact> implements ExtensionPoint, Serializable {

    private static final String DEFAULT_EXTENSION = "jar";
    private static final String EMPTY_SPACE = "";
    
    private static final long serialVersionUID = 1L;

    private final String groupId;
    private final String artifactId;
    private String classifier;
    private final String version;
    private String extension;
    private String targetFileName;

    @DataBoundConstructor
    public Artifact(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        
        this.classifier = EMPTY_SPACE;
        this.extension = DEFAULT_EXTENSION;
        this.targetFileName = EMPTY_SPACE;
    }

    /**
     * @return the artifactId
     */
    public String getArtifactId() {
        return artifactId;
    }

    /**
     * @return the classifier
     */
    public String getClassifier() {
        return classifier;
    }
    
    /**
     * @return the extension
     */
    public String getExtension() {
        return extension;
    }

    /**
     * @return the groupId
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * @return the targetFileName
     */
    public String getTargetFileName() {
        return targetFileName;
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    @DataBoundSetter
    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    @DataBoundSetter
    public void setExtension(String extension) {
        this.extension = extension;
    }

    @DataBoundSetter
    public void setTargetFileName(String targetFileName) {
        this.targetFileName = targetFileName;
    }

    @Override
    public String toString() {
        return "[Artifact " + groupId + ":" + artifactId + ":" + extension + ":" + classifier + ":" + version + "]";
    }
    
    @Extension
    @Symbol("artifact")
    public static class DescriptorImpl extends Descriptor<Artifact>
    {
        @Override
        public String getDisplayName() {
            return "artifact";
        }
    }
}
