package org.jvnet.hudson.plugins.repositoryconnector;

import hudson.model.StringParameterValue;
import java.util.logging.Level;

import java.util.logging.Logger;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * This class sets the build parameter as environment value
 * "groupid.artifactid=version"
 *
 * @author mrumpf
 *
 */
public class VersionParameterValue extends StringParameterValue {

    private static final Logger log = Logger
            .getLogger(VersionParameterValue.class.getName());

    private final String groupid;
    private final String artifactid;

    @DataBoundConstructor
    public VersionParameterValue(String groupid, String artifactid, String version) {
        super(groupid + "." + artifactid, version);
        if (log.isLoggable(Level.FINE)) {
            log.fine("Creating environment build parameter 'groupid.artifactid=version'");
        }
        this.groupid = groupid;
        this.artifactid = artifactid;
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
        sb.append(']');
        return sb.toString();
    }
}
