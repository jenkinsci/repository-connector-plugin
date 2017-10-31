package org.jvnet.hudson.plugins.repositoryconnector;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jvnet.hudson.plugins.repositoryconnector.aether.Aether;
import org.jvnet.hudson.plugins.repositoryconnector.aether.VersionRangeResultWithLatest;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;
import org.sonatype.aether.resolution.VersionRangeResolutionException;
import org.sonatype.aether.version.Version;

import hudson.Extension;
import hudson.model.ParameterValue;
import hudson.model.SimpleParameterDefinition;
import hudson.model.StringParameterValue;
import hudson.util.FormValidation;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

@SuppressWarnings("serial")
public class VersionParameterDefinition extends
        SimpleParameterDefinition {

    private static final Logger log = Logger.getLogger(VersionParameterDefinition.class.getName());

    private final String groupid;
    private final String repoid;
    private final String artifactid;
    private final String propertyName;

    @DataBoundConstructor
    public VersionParameterDefinition(String repoid, String groupid,
            String artifactid, String propertyName, String description) {
        super((propertyName != null && !propertyName.isEmpty()) ? propertyName : groupid + "." + artifactid, description);
        this.repoid = repoid;
        this.groupid = groupid;
        this.artifactid = artifactid;
        this.propertyName = propertyName;
    }

    @Override
    public VersionParameterDefinition copyWithDefaultValue(ParameterValue defaultValue) {
        if (defaultValue instanceof StringParameterValue) {
            // TODO: StringParameterValue value = (StringParameterValue) defaultValue;
            return new VersionParameterDefinition(getRepoid(), "",
                    "", "", getDescription());
        }
        return this;
    }

    @Exported
    public List<VersionLabel> getChoices() {
        Repository r = DESCRIPTOR.getRepo(repoid);
        List<VersionLabel> items = new ArrayList<VersionLabel>();
        if (r != null) {
            File localRepo = RepositoryConfiguration.get().getLocalRepoPath();
            Aether aether = new Aether(DESCRIPTOR.getRepos(), localRepo);
            try {
                // Get the versions
                VersionRangeResultWithLatest versionsWithLatest = aether.resolveVersions(groupid, artifactid);
                List<Version> versions = versionsWithLatest.getVersions();

                // Reverse order to have the latest versions on top of the list
                Collections.reverse(versions);

                // Add the choice items
                for (Version version : versions) {
                    items.add(new VersionLabel(version.toString(), version.toString()));
                }

                // Add the default parameters as needed
                if (!items.isEmpty()) {
                    items.add(0, toDefaultVersion(versionsWithLatest.getLatest(), "LATEST"));
                    items.add(0, toDefaultVersion(versionsWithLatest.getRelease(), "RELEASE"));
                }
            } catch (VersionRangeResolutionException ex) {
                log.log(Level.SEVERE, "Could not determine versions", ex);
            }
        }
        return items;
    }

    /**
     * Return a version type with its optional resolved version.
     */
    private VersionLabel toDefaultVersion(Version version, String type) {
        log.info("toDefaultVersion "+version+","+type);
        if (version == null) {
            // No resolved version for this type
            return new VersionLabel(type, type);
        }

        // Return the type with the version as suffix
        return new VersionLabel(version.toString(), type);
    }

    @Exported
    public String getArtifactid() {
        return artifactid;
    }

    @Exported
    public String getRepoid() {
        return repoid;
    }

    @Exported
    public String getGroupid() {
        return groupid;
    }

    @Exported
    public String getPropertyName() {
        return propertyName;
    }

    @Override
    public ParameterValue createValue(StaplerRequest req, JSONObject jo) {
        return new VersionParameterValue(groupid, artifactid, propertyName, jo.getString("value"));
    }

    /**
     * Creates a {@link ParameterValue} from the string representation. 
     * Manage Maven artifact definition : <code>group:artifact:version</code> 
     * @param input The rw input string.
     * @return a {@link VersionParameterValue} representation. 
     */
    @Override
    public ParameterValue createValue(String input) {
    	final String[] tokens = input.split(":");
        return new VersionParameterValue(tokens[0], tokens[1], tokens[2], tokens[3]);
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return DESCRIPTOR;
    }

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static class DescriptorImpl extends ParameterDescriptor {

        public DescriptorImpl() {
            super(VersionParameterDefinition.class);
            load();
        }

        public Repository getRepo(String id) {
            Repository repo = null;
            RepositoryConfiguration repoConfig = RepositoryConfiguration.get();
            if (repoConfig != null) {
                repo = repoConfig.getRepositoryMap().get(id);
                log.fine("getRepo(" + id + ")=" + repo);
            }
            return repo;
        }

        public Collection<Repository> getRepos() {
            Collection<Repository> repos = null;
            RepositoryConfiguration repoConfig = RepositoryConfiguration.get();
            if (repoConfig != null) {
                repos = repoConfig.getRepos();
                log.fine("getRepos()=" + repos);
            }
            return repos;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) {
            if (formData.has("repo")) {
                try {
                    List l = JSONArray.toList(
                            formData.getJSONArray("repo"), Repository.class);
                    // TODO: ???
                } catch (JSONException ex) {
                    Repository r = (Repository) JSONObject.toBean(
                            formData.getJSONObject("repo"), Repository.class);
                    // TODO: ???
                }
            } else {
                // TODO: Should not happen
            }

            save();
            return true;
        }

        public FormValidation doCheckGroupid(@QueryParameter String groupid,
                @QueryParameter String artifactid,
                @QueryParameter String repoid) {
            FormValidation result = FormValidation.ok();
            if (groupid == null || groupid.isEmpty()) {
                result = FormValidation.error(Messages.EmptyGroupId());
            } else {
                if (artifactid != null && !artifactid.isEmpty() && repoid != null && !repoid.isEmpty()) {
                    result = checkPath(artifactid, groupid, repoid);
                }
            }
            return result;
        }

        public FormValidation doCheckArtifactid(
                @QueryParameter String artifactid,
                @QueryParameter String groupid, @QueryParameter String repoid) {
            FormValidation result = FormValidation.ok();
            if (artifactid == null || artifactid.isEmpty()) {
                result = FormValidation.error(Messages.EmptyArtifactId());
            } else {
                if (groupid != null && !groupid.isEmpty() && repoid != null && !repoid.isEmpty()) {
                    result = checkPath(artifactid, groupid, repoid);
                }
            }
            return result;
        }

        private FormValidation checkPath(String artifactid, String groupid,
                String repoid) {
            FormValidation result = FormValidation.ok();
            File localRepo = RepositoryConfiguration.get().getLocalRepoPath();
            Aether aether = new Aether(DESCRIPTOR.getRepos(), localRepo);
            try {
                List<Version> versions = aether.resolveVersions(groupid, artifactid).getVersions();
                if (versions.isEmpty()) {
                    result = FormValidation.error(Messages.NoVersions() + " " + groupid + "." + artifactid);
                    log.log(Level.FINE, "No versions found for " + groupid + "." + artifactid);
                }
            } catch (VersionRangeResolutionException ex) {
                result = FormValidation.error(Messages.NoVersions() + " " + groupid + "." + artifactid);
                log.log(Level.SEVERE, "Could not determine versions for " + groupid + "." + artifactid, ex);
            }
            return result;
        }

        public FormValidation doCheckRepoid(@QueryParameter String repoid) {
            FormValidation result = FormValidation.ok();
            if (repoid == null || repoid.isEmpty()) {
                result = FormValidation.error(Messages.EmptyRepositoryName());
            }
            return result;
        }

        public FormValidation doCheckBaseurl(@QueryParameter String baseurl) {
            FormValidation result = FormValidation.ok();
            if (baseurl == null || baseurl.isEmpty()) {
                result = FormValidation.error(Messages.EmptyBaseURL());
            }
            return result;
        }

        public FormValidation doCheckPassword(@QueryParameter String password,
                @QueryParameter String username) {
            FormValidation result = FormValidation.ok();
            if (password != null && !password.isEmpty()
                    && (username == null || username.isEmpty())) {
                result = FormValidation.error(Messages.EmptyUsername());
            }
            return result;
        }

        public FormValidation doTestConnection(@QueryParameter String baseurl,
                @QueryParameter String username, @QueryParameter String password) {
            try {
                if (true /*MavenRepositoryClient.testConnection(baseurl, username,
                         password)*/) {
                    return FormValidation.ok(Messages.Success());
                }
                return FormValidation.error(Messages.ConnectionFailed());
            } catch (Exception e) {
                log.log(Level.SEVERE, "Client error: " + e.getMessage(), e);
                return FormValidation.error(Messages.ClientError()
                        + e.getMessage());
            }
        }

        @Override
        public String getDisplayName() {
            return Messages.DisplayName();
        }
    }

    @Override
	public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(this.getClass().getSimpleName());
        sb.append("@[");
        sb.append("name=");
        sb.append(getName());
        sb.append(", description=");
        sb.append(getDescription());
        sb.append(", groupid=");
        sb.append(groupid);
        sb.append(", repoid=");
        sb.append(repoid);
        sb.append(", artifactid=");
        sb.append(artifactid);
        sb.append(']');
        return sb.toString();
    }
}
