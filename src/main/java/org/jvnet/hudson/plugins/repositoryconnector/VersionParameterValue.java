package org.jvnet.hudson.plugins.repositoryconnector;

import hudson.model.StringParameterValue;

import java.util.logging.Logger;

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

    private static final Logger log = Logger.getLogger(VersionParameterValue.class.getName());

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
        sb.append(']');
        return sb.toString();
    }
}
