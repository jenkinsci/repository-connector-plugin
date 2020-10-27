package org.jvnet.hudson.plugins.repositoryconnector;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.json.JSONObject;

import org.jvnet.hudson.plugins.repositoryconnector.aether.Aether;
import org.jvnet.hudson.plugins.repositoryconnector.aether.AetherException;
import org.jvnet.hudson.plugins.repositoryconnector.util.FormValidator;
import org.jvnet.hudson.plugins.repositoryconnector.util.RepositoryListBox;
import org.jvnet.hudson.plugins.repositoryconnector.util.VersionFilter;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;

import hudson.Extension;
import hudson.Util;
import hudson.model.Item;
import hudson.model.ParameterValue;
import hudson.model.SimpleParameterDefinition;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

public class VersionParameterDefinition extends SimpleParameterDefinition {

    private static final long serialVersionUID = 1L;

    private static final Logger logger = Logger.getLogger(VersionParameterDefinition.class.getName());

    private static final String LATEST = "LATEST";

    private static final String RELEASE = "RELEASE";

    private final String artifactId;

    private final String groupId;

    private boolean includeReleases;

    private boolean includeSnapshots;

    private String limit;

    private boolean oldestFirst;

    private String repositoryId;

    private boolean useLatest;

    private boolean useRelease;

    @DataBoundConstructor
    public VersionParameterDefinition(String name, String description, String groupId, String artifactId) {
        super(Util.fixEmpty(name), Util.fixEmpty(description));

        this.groupId = groupId;
        this.artifactId = artifactId;

        setIncludeReleases(true);
        setIncludeSnapshots(true);

        setUseRelease(true);
        setUseLatest(true);
    }

    @Override
    public ParameterValue createValue(StaplerRequest req, JSONObject json) {
        return createValue(json.getString("value"));
    }

    @Override
    public ParameterValue createValue(String input) {
        // can be called from 'buildWithParameters' via url
        return new VersionParameterValue(getName(), getDescription(), repositoryId, groupId, artifactId, input);
    }

    @Exported
    public String getArtifactId() {
        return artifactId;
    }

    @Exported
    public String getDescriptionOrCoords() {
        return Optional.ofNullable(getDescription())
                .orElse(groupId + ":" + artifactId);
    }

    @Exported
    public String getGroupId() {
        return groupId;
    }

    @Exported
    public String getLimit() {
        return limit;
    }

    @Exported
    public String getRepositoryId() {
        return repositoryId;
    }

    @Exported
    public List<String> getVersions() {
        Artifact artifact = new Artifact(groupId, artifactId, null);
        List<String> versions = new ArrayList<>();

        try {
            Aether aether = createAether();

            versions.addAll(aether.resolveAvailableVersions(repositoryId, artifact, oldestFirst,
                    createVersionFilter(includeReleases, includeSnapshots)));

            if (versions.size() > 0) {
                if (oldestFirst) {
                    versions = adjustOldestFirst(versions);
                } else {
                    versions = adjustNewestFirst(versions);
                }
            }
        } catch (AetherException e) {
            logger.log(Level.WARNING, "failed to resolve versions for artifact: " + artifact, e);
        }

        return versions;
    }

    public boolean isIncludeReleases() {
        return includeReleases;
    }

    public boolean isIncludeSnapshots() {
        return includeSnapshots;
    }

    public boolean isOldestFirst() {
        return oldestFirst;
    }

    public boolean isUseLatest() {
        return useLatest;
    }

    public boolean isUseRelease() {
        return useRelease;
    }

    @DataBoundSetter
    public void setIncludeReleases(boolean includeReleases) {
        this.includeReleases = includeReleases;
    }

    @DataBoundSetter
    public void setIncludeSnapshots(boolean includeSnapshots) {
        this.includeSnapshots = includeSnapshots;
    }

    @DataBoundSetter
    public void setLimit(String limit) {
        this.limit = Util.fixEmpty(limit);
    }

    @DataBoundSetter
    public void setOldestFirst(boolean oldestFirst) {
        // need to take the inverse b/c of the jelly control
        this.oldestFirst = !oldestFirst;
    }

    @DataBoundSetter
    public void setRepositoryId(String repositoryId) {
        this.repositoryId = Util.fixEmpty(repositoryId);
    }

    @DataBoundSetter
    public void setUseLatest(boolean useLatest) {
        this.useLatest = useLatest;
    }

    @DataBoundSetter
    public void setUseRelease(boolean useRelease) {
        this.useRelease = useRelease;
    }

    // visible for unit testing
    Aether createAether() {
        return createAether(getProject());
    }

    // visible for unit testing
    Item getProject() {
        return (Item) Stapler.getCurrentRequest().findAncestor(Item.class).getObject();
    }

    private List<String> adjustNewestFirst(List<String> versions) {
        if (limit != null) {
            versions = versions.subList(0, Integer.parseInt(limit));
        }

        if (useRelease) {
            versions.add(0, RELEASE);
        }

        if (useLatest) {
            versions.add(1, LATEST);
        }

        return versions;
    }

    private List<String> adjustOldestFirst(List<String> versions) {
        if (useRelease) {
            versions.add(RELEASE);
        }

        if (useLatest) {
            versions.add(LATEST);
        }

        return versions;
    }

    private static Aether createAether(Item item) {
        return RepositoryConfiguration.createAetherFactory()
                .createAetherBuilder(item)
                .build();
    }

    private static VersionFilter createVersionFilter(boolean releases, boolean snapshots) {
        return new VersionFilter(releases, snapshots);
    }

    @Extension
    public static class DescriptorImpl extends ParameterDescriptor {

        public FormValidation doCheckArtifactId(@QueryParameter String value) {
            return FormValidator.validateArtifactId(value);
        }

        public FormValidation doCheckGroupId(@QueryParameter String value) {
            return FormValidator.validateGroupId(value);
        }

        public FormValidation doCheckIncludeReleases(@QueryParameter boolean value, @QueryParameter boolean includeSnapshots) {
            return FormValidator.validateReleasesAndOrSnapshots(value, includeSnapshots);
        }

        public FormValidation doCheckIncludeSnapshots(@QueryParameter boolean value, @QueryParameter boolean includeReleases) {
            return FormValidator.validateReleasesAndOrSnapshots(includeReleases, value);
        }

        public FormValidation doCheckLimit(@QueryParameter String value, @QueryParameter boolean oldestFirst) {
            return FormValidator.validateVersionLimit(value, !oldestFirst);
        }

        public FormValidation doCheckName(@QueryParameter String value) {
            return FormValidator.validateVersionParameterName(value);
        }

        public ListBoxModel doFillRepositoryIdItems() {
            return new RepositoryListBox(RepositoryConfiguration.get().getRepositories())
                    .withSelectAll();
        }

        public FormValidation doValidateCoordinates(@QueryParameter String groupId, @QueryParameter String artifactId,
                @QueryParameter boolean includeReleases, @QueryParameter boolean includeSnapshots, @QueryParameter String repositoryId,
                @AncestorInPath Item item) {

            return FormValidator.validateCoordinates(repositoryId, groupId, artifactId, createAether(item),
                    createVersionFilter(includeReleases, includeSnapshots));
        }

        @Override
        public String getDisplayName() {
            return Messages.DisplayName();
        }

        public boolean hasMultipleRepositories() {
            return RepositoryConfiguration.get().hasMultipleRepositories();
        }
    }
}
