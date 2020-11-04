package org.jvnet.hudson.plugins.repositoryconnector;

import java.util.Objects;

import hudson.model.StringParameterValue;

public class VersionParameterValue extends StringParameterValue {

    private static final long serialVersionUID = 1L;

    private final String artifactId;

    private final String groupId;

    private final String repositoryId;

    public VersionParameterValue(String name, String description, String repositoryId, String groupId, String artifactId, String value) {
        super(name, value, description);

        this.groupId = groupId;
        this.artifactId = artifactId;
        this.repositoryId = repositoryId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        VersionParameterValue other = (VersionParameterValue) obj;
        return Objects.equals(getName(), other.name) &&
                Objects.equals(groupId, other.groupId) &&
                Objects.equals(artifactId, other.artifactId) &&
                Objects.equals(repositoryId, other.groupId);
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), groupId, artifactId, repositoryId);
    }
}
