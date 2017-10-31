package org.jvnet.hudson.plugins.repositoryconnector;

import hudson.model.StringParameterValue;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * This class sets the build parameter as environment value
 * "groupid.artifactid=version" or as "name=value".
 *
 * @author mrumpf
 *
 */
@SuppressWarnings("serial")
public class VersionParameterValue extends StringParameterValue {

    private final String groupid;
    private final String artifactid;
    private final String propertyName;

    public String getGroupid() {
        return groupid;
    }

    public String getArtifactid() {
        return artifactid;
    }

    public String getPropertyName() {
        return propertyName;
    }

    @DataBoundConstructor
    public VersionParameterValue(String groupid, String artifactid, String propertyName, String version) {
        super((propertyName != null && !propertyName.isEmpty()) ? propertyName : groupid + "." + artifactid, version);
        this.groupid = groupid;
        this.artifactid = artifactid;
        this.propertyName = propertyName;
    }

    @Override
	public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(this.getClass().getSimpleName());
        sb.append("@[");
        sb.append("name=");
        sb.append(getName());
        sb.append(", groupid=");
        sb.append(groupid);
        sb.append(", artifactid=");
        sb.append(artifactid);
        sb.append(", propertyName=");
        sb.append(propertyName);
        sb.append(", version=");
        sb.append(getValue());
        sb.append(']');
        return sb.toString();
    }
}
