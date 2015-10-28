package org.jvnet.hudson.plugins.repositoryconnector;

import java.io.Serializable;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Represents an artifact to be resolved or uploaded.
 * 
 * @author domi
 * 
 */
public class Artifact implements Serializable {

   private static final long serialVersionUID = 1L;

    private final String groupId;
    private final String artifactId;
    private final String classifier;
    private final String version;
    private final String extension;
    private final String targetFileName;
    private final Boolean uploadPOM;

   @DataBoundConstructor
    public Artifact(String groupId, String artifactId, String classifier, String version, String extension, String targetFileName, Boolean uploadPOM) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.classifier = classifier == null ? "" : classifier;
        this.extension = extension == null ? "jar" : extension;
        this.version = version;
        this.targetFileName = targetFileName;
        this.uploadPOM = uploadPOM;
    }

    /**
     * @return the groupId
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * @return the artifactId
     */
    public String getArtifactId() {
        return artifactId;
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * @return the extension
     */
    public String getExtension() {
        return extension;
    }

    /**
     * @return the classifier
     */
    public String getClassifier() {
        return classifier;
    }

    /**
     * @return the targetFileName
     */
    public String getTargetFileName() {
        return targetFileName;
    }

    /**
     * @return the uploadPOM
     */
    public boolean getUploadPOM() {
        // Backwards compatibility: if null, config not updated: fallback to true
        if (uploadPOM == null) {
            return true;
        }
        return uploadPOM;
    }

    @Override
    public String toString() {
        return "[Artifact " + groupId + ":" + artifactId + ":" + extension + ":" + classifier + ":" + version + "]";
    }
}
