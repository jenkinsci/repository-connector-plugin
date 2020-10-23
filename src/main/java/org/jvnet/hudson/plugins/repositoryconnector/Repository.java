package org.jvnet.hudson.plugins.repositoryconnector;

import java.util.Objects;

import org.jvnet.hudson.plugins.repositoryconnector.aether.AetherConstants;
import org.jvnet.hudson.plugins.repositoryconnector.util.CredentialsUtilities;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import jenkins.model.Jenkins;

/**
 * Represents a repository where artifacts can be resolved from or uploaded to.
 * 
 * @author domi
 */
public class Repository extends AbstractDescribableImpl<Repository> implements Comparable<Repository> {

    public static final String CENTRAL = "central";

    public static final String ENDPOINT = "https://repo1.maven.org/maven2";

    public static final Repository MAVEN_CENTRAL = createCentralRepository();

    private String credentialsId;

    private boolean enableReleaseRepository;

    private boolean enableSnapshotRepository;

    private final String id;

    @Deprecated
    private Secret password;

    private RepositoryType releaseRepository;

    private RepositoryType snapshotRepository;

    private final String url;

    @Deprecated
    private Secret user;

    public Repository(Repository toClone) {
        this(toClone.id, toClone.url);

        this.credentialsId = toClone.credentialsId;

        this.enableReleaseRepository = toClone.enableReleaseRepository;
        this.enableSnapshotRepository = toClone.enableSnapshotRepository;

        this.releaseRepository = new RepositoryType(toClone.releaseRepository);
        this.snapshotRepository = new RepositoryType(toClone.snapshotRepository);
    }

    @DataBoundConstructor
    public Repository(String id, String url) {
        this.id = id;
        this.url = url;

        this.enableReleaseRepository = true;
        this.releaseRepository = RepositoryType.DEFAULT;

        this.enableSnapshotRepository = true;
        this.snapshotRepository = RepositoryType.DEFAULT;

        // this.isRepositoryManager = true;
    }

    @Override
    public int compareTo(Repository repository) {
        return id.compareTo(repository.getId());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        Repository other = (Repository) obj;
        return Objects.equals(id, other.id);
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public String getId() {
        return id;
    }

    @Deprecated
    public String getPassword() {
        return Util.fixEmpty(Secret.toString(password));
    }

    public RepositoryType getReleaseRepository() {
        return releaseRepository;
    }

    public RepositoryType getSnapshotRepository() {
        return snapshotRepository;
    }

    public String getType() {
        /*-
         * this has to be 'default' otherwise the maven layout resolver won't work
         * 
         * https://stackoverflow.com/questions/28235214/how-to-add-remoterepository-in-aether-to-get-direct-dependencies
         */
        return "default";
    }

    public String getUrl() {
        return url;
    }

    @Deprecated
    public String getUser() {
        return Util.fixEmpty(Secret.toString(user));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public boolean hasLegacyCredentials() {
        return user != null && password != null;
    }

    public boolean isEnableReleaseRepository() {
        return enableReleaseRepository;
    }

    public boolean isEnableSnapshotRepository() {
        return enableSnapshotRepository;
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = Util.fixEmpty(credentialsId);
    }

    @DataBoundSetter
    public void setEnableReleaseRepository(boolean enableReleaseRepository) {
        this.enableReleaseRepository = enableReleaseRepository;
    }

    @DataBoundSetter
    public void setEnableSnapshotRepository(boolean enableSnapshotRepository) {
        this.enableSnapshotRepository = enableSnapshotRepository;
    }

    @Deprecated
    public void setPassword(String password) {
        this.password = Secret.fromString(password);
    }

    @DataBoundSetter
    public void setReleaseRepository(RepositoryType releaseRepository) {
        this.releaseRepository = releaseRepository;
    }

    @DataBoundSetter
    public void setSnapshotRepository(RepositoryType snapshotRepository) {
        this.snapshotRepository = snapshotRepository;
    }

    @Deprecated
    public void setUser(String user) {
        this.user = Secret.fromString(user);
    }

    public String toDescription() {
        return id + " - " + url;
    }

    @Override
    public String toString() {
        return "Repository [id=" + id + ", url=" + url + ", credentialsId=" + credentialsId + "]";
    }

    private static Repository createCentralRepository() {
        Repository repository = new Repository(CENTRAL, ENDPOINT);
        repository.setEnableSnapshotRepository(false);

        return repository;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<Repository> {

        public FormValidation doCheckId(@QueryParameter String value) {
            return Util.fixEmpty(value) == null ? FormValidation.error("Name is required") : FormValidation.ok();
        }

        public FormValidation doCheckUrl(@QueryParameter String value) {
            return Util.fixEmpty(value) == null ? FormValidation.error("Endpoint is required") : FormValidation.ok();
        }

        public FormValidation doCheckUseReleasePolicy(@QueryParameter boolean value, @QueryParameter boolean useSnapshotPolicy) {
            return validateAtLeastOnePolicy(useSnapshotPolicy, value);
        }

        public FormValidation doCheckUseSnapshotPolicy(@QueryParameter boolean value, @QueryParameter boolean useReleasePolicy) {
            return validateAtLeastOnePolicy(value, useReleasePolicy);
        }

        public ListBoxModel doFillCredentialsIdItems(@QueryParameter String credentialsId) {
            return CredentialsUtilities.getListBox(credentialsId, Jenkins.get());
        }

        // this fires but doesn't render the error properly on the page
        private FormValidation validateAtLeastOnePolicy(boolean useSnapshotPolicy, boolean useReleasePolicy) {
            if (useSnapshotPolicy || useReleasePolicy) {
                return FormValidation.ok();
            }

            return FormValidation.error("At least one policy is required");
        }
    }

    public static class RepositoryType extends AbstractDescribableImpl<RepositoryType> {

        public static final RepositoryType DEFAULT =
                new RepositoryType(AetherConstants.DEFAULT_CHECKSUM, AetherConstants.DEFAULT_UPDATE, null, null);

        private final String checksum;

        private final String credentialsId;

        private final String update;

        private final String url;

        @DataBoundConstructor
        public RepositoryType(String checksum, String update, String url, String credentialsId) {
            this.checksum = checksum;
            this.update = update;
            this.url = url;
            this.credentialsId = credentialsId;
        }

        RepositoryType(RepositoryType toClone) {
            this(toClone.checksum, toClone.update, Util.fixEmpty(toClone.url), Util.fixEmpty(toClone.credentialsId));
        }

        public String getChecksum() {
            return checksum;
        }

        public String getCredentialsId() {
            return credentialsId;
        }

        public String getUpdate() {
            return update;
        }

        public String getUrl() {
            return url;
        }

        @Override
        public String toString() {
            return "RepositoryType [checksum=" + checksum + ", update=" + update + ", url=" + url + ", "
                    + "credentialsId=" + credentialsId + "]";
        }

        @Extension
        public static class DescriptorImpl extends Descriptor<RepositoryType> {

            public static final RepositoryType policy = RepositoryType.DEFAULT;

            public ListBoxModel doFillChecksumItems() {
                return createSelectItems(AetherConstants.getChecksumPolicies());
            }

            public ListBoxModel doFillCredentialsIdItems(@QueryParameter String credentialsId) {
                return CredentialsUtilities.getListBox(credentialsId, Jenkins.get());
            }

            public ListBoxModel doFillUpdateItems() {
                return createSelectItems(AetherConstants.getUpdatePolicies());
            }

            private ListBoxModel createSelectItems(String[] choices) {
                ListBoxModel items = new ListBoxModel();

                for (String choice : choices) {
                    items.add(choice);
                }

                return items;
            }
        }
    }
}
