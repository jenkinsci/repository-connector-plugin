package org.jvnet.hudson.plugins.repositoryconnector;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.jvnet.hudson.plugins.repositoryconnector.aether.Aether;
import org.jvnet.hudson.plugins.repositoryconnector.aether.AetherResult;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.repository.RepositoryPolicy;
import org.sonatype.aether.resolution.DependencyResolutionException;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildStep;

/**
 * This builder allows to resolve artifacts from a repository and copy it to any location.
 * 
 * @author domi
 */
public class ArtifactResolver extends Builder implements SimpleBuildStep, Serializable {

    private static final long serialVersionUID = 1L;

    private static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
    
    private static Logger log = Logger.getLogger(ArtifactResolver.class.getName());

    private static final String DEFAULT_TARGET = "";

    private String targetDirectory;
    
    private final List<Artifact> artifacts;
    
    private boolean failOnError = true;
    
    private boolean enableRepoLogging = true;
    
    private String snapshotUpdatePolicy;
    
    private String releaseUpdatePolicy;
    
    private String snapshotChecksumPolicy;
    
    private String releaseChecksumPolicy;
    
    @DataBoundConstructor
    public ArtifactResolver(List<Artifact> artifacts)
    {
        this.artifacts = artifacts;
        
        this.targetDirectory = DEFAULT_TARGET;
        
        this.failOnError = DescriptorImpl.defaultFailOnError;
        this.enableRepoLogging = DescriptorImpl.defaultEnableRepoLogging;
        
        this.releaseUpdatePolicy = DescriptorImpl.defaultReleaseUpdatePolicy;
        this.releaseChecksumPolicy = DescriptorImpl.defaultReleaseChecksumPolicy;
        
        this.snapshotUpdatePolicy = DescriptorImpl.defaultSnapshotUpdatePolicy;
        this.snapshotChecksumPolicy = DescriptorImpl.defaultSnapshotChecksumPolicy;
    }

    @Deprecated
    public ArtifactResolver(String targetDirectory, List<Artifact> artifacts, boolean failOnError, boolean enableRepoLogging, String snapshotUpdatePolicy,
            String snapshotChecksumPolicy, String releaseUpdatePolicy, String releaseChecksumPolicy) {
        this.artifacts = artifacts != null ? artifacts : new ArrayList<Artifact>();
        this.targetDirectory = StringUtils.isBlank(targetDirectory) ? DEFAULT_TARGET : targetDirectory;
        this.failOnError = failOnError;
        this.enableRepoLogging = enableRepoLogging;
        this.releaseUpdatePolicy = releaseUpdatePolicy;
        this.releaseChecksumPolicy = RepositoryPolicy.CHECKSUM_POLICY_WARN;
        this.snapshotUpdatePolicy = snapshotUpdatePolicy;
        this.snapshotChecksumPolicy = RepositoryPolicy.CHECKSUM_POLICY_WARN;
    }
    
    @Override
    public Descriptor<Builder> getDescriptor() {
        return DESCRIPTOR;
    }
    
    /**
     * gets the artifacts
     * 
     * @return
     */
    public List<Artifact> getArtifacts() {
        return artifacts;
    }

    public String getReleaseChecksumPolicy() {
        return releaseChecksumPolicy;
    }

    public String getReleaseUpdatePolicy() {
        return releaseUpdatePolicy;
    }

    public String getSnapshotChecksumPolicy() {
        return snapshotChecksumPolicy;
    }

    public String getSnapshotUpdatePolicy() {
        return snapshotUpdatePolicy;
    }

    public String getTargetDirectory() {
        return targetDirectory;
    }

    public boolean isEnableRepoLogging() {
        return enableRepoLogging;
    }

    public boolean isFailOnError() {
        return failOnError;
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
        throws InterruptedException, IOException {
        
        final PrintStream logger = listener.getLogger();
        final Collection<Repository> repositories = RepositoryConfiguration.get().getRepos();

        File localRepo = RepositoryConfiguration.get().getLocalRepoPath();
        boolean failed = download(run, workspace, listener, logger, repositories, localRepo);

        if (failed && failOnError) {
            throw new AbortException("Failed to resolve artifact");
        }
    }

    @DataBoundSetter
    public void setEnableRepoLogging(boolean enableRepoLogging) {
        this.enableRepoLogging = enableRepoLogging;
    }

    @DataBoundSetter
    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }

    @DataBoundSetter
    public void setReleaseChecksumPolicy(String releaseChecksumPolicy) {
        this.releaseChecksumPolicy = releaseChecksumPolicy;
    }

    @DataBoundSetter
    public void setReleaseUpdatePolicy(String releaseUpdatePolicy) {
        this.releaseUpdatePolicy = releaseUpdatePolicy;
    }

    @DataBoundSetter
    public void setSnapshotChecksumPolicy(String snapshotChecksumPolicy) {
        this.snapshotChecksumPolicy = snapshotChecksumPolicy;
    }

    @DataBoundSetter
    public void setSnapshotUpdatePolicy(String snapshotUpdatePolicy) {
        this.snapshotUpdatePolicy = snapshotUpdatePolicy;
    }

    @DataBoundSetter
    public void setTargetDirectory(String targetDirectory) {
        this.targetDirectory = Util.fixNull(targetDirectory);
    }

    private boolean download(Run<?, ?> run, FilePath workspace, TaskListener listener, final PrintStream logger, final Collection<Repository> repositories,
            File localRepository) {
        boolean hasError = false;

        Aether aether = new Aether(repositories, localRepository, logger, enableRepoLogging, snapshotUpdatePolicy, snapshotChecksumPolicy, releaseUpdatePolicy,
                releaseChecksumPolicy);

        for (Artifact a : artifacts) {

            try {

                final String classifier = TokenMacro.expandAll(run, workspace, listener, a.getClassifier());
                final String artifactId = TokenMacro.expandAll(run, workspace, listener, a.getArtifactId());
                final String groupId = TokenMacro.expandAll(run, workspace, listener, a.getGroupId());
                final String extension = TokenMacro.expandAll(run, workspace, listener, a.getExtension());
                final String targetFileName = TokenMacro.expandAll(run, workspace, listener, a.getTargetFileName());
                final String expandedTargetDirectory = TokenMacro.expandAll(run, workspace, listener, getTargetDirectory());

                String version = TokenMacro.expandAll(run, workspace, listener, a.getVersion());
                version = checkVersionOverride(run, listener, groupId, artifactId, version);

                AetherResult result = aether.resolve(groupId, artifactId, classifier, extension, version);

                List<File> resolvedFiles = result.getResolvedFiles();
                for (File file : resolvedFiles) {

                    String fileName = StringUtils.isBlank(targetFileName) ? file.getName() : targetFileName;
                    FilePath source = new FilePath(file);
                    String targetDir = StringUtils.isNotBlank(expandedTargetDirectory) ? expandedTargetDirectory + "/" : "";  
                    FilePath target = new FilePath(workspace, targetDir + fileName);
                    boolean wasDeleted = target.delete();
                    if (wasDeleted) {
                        logger.println("deleted " + target.toURI());
                    }
                    logger.println("copy " + file + " to " + target.toURI());
                    source.copyTo(target);

                }

            } catch (DependencyCollectionException e) {
                hasError = logError("failed collecting dependency info for " + a, logger, e);
            } catch (IOException e) {
                hasError = logError("failed collecting dependency info for " + a, logger, e);
            } catch (InterruptedException e) {
                hasError = logError("interuppted failed to copy file for " + a, logger, e);
            } catch (DependencyResolutionException e) {
                hasError = logError("failed to resolve dependency for " + a, logger, e);
            } catch (Exception e) {
                hasError = logError("failed to expand tokens for " + a, logger, e);
            }

        }
        return hasError;
    }
    
    /**
     * This method searches for a build parameter of type VersionParameterValue and
     * substitutes the configured version by the one, defined by the parameter.
     *
     * @param run the build run
     * @param listener the build listener
     * @param groupId the Maven group id
     * @param artifactId the Maven artifact id
     * @param version the version
     * @return The overridden version
     */
    private String checkVersionOverride(Run<?, ?> run, TaskListener listener, String groupId, String artifactId, String version) {
        String result = version;
        List<ParametersAction> parameterActionList = run.getActions(ParametersAction.class);
        for (ParametersAction parameterAction : parameterActionList) {
            List<ParameterValue> parameterValueList = parameterAction.getParameters();
            for (ParameterValue parameterValue : parameterValueList) {
                if (parameterValue instanceof VersionParameterValue) {
                    VersionParameterValue versionParameterValue = (VersionParameterValue) parameterValue;
                    if (groupId != null && groupId.equals(versionParameterValue.getGroupid()) &&
                            artifactId != null && artifactId.equals(versionParameterValue.getArtifactid())) {
                        listener.getLogger().println("Overriding configured version '" + version + "' with version '"
                                + versionParameterValue.value + "' from build parameter");
                        result = versionParameterValue.value;
                    }
                }
            }
        }
        return result;
    }

    private boolean logError(String msg, final PrintStream logger, Exception e) {
        log.log(Level.SEVERE, msg, e);
        logger.println(msg);
        e.printStackTrace(logger);
        return true;
    }

    @Extension
    @Symbol("artifactResolver")
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        public static final boolean defaultFailOnError = true;
        public static final boolean defaultEnableRepoLogging = true;
        
        public static final String defaultReleaseUpdatePolicy = RepositoryPolicy.UPDATE_POLICY_DAILY;
        public static final String defaultReleaseChecksumPolicy = RepositoryPolicy.CHECKSUM_POLICY_WARN;
        
        public static final String defaultSnapshotUpdatePolicy =  RepositoryPolicy.UPDATE_POLICY_DAILY;
        public static final String defaultSnapshotChecksumPolicy = RepositoryPolicy.CHECKSUM_POLICY_WARN;
        
        private static final String[] CHECKSUM_POLICIES = { 
                RepositoryPolicy.CHECKSUM_POLICY_WARN,
                RepositoryPolicy.CHECKSUM_POLICY_IGNORE, 
                RepositoryPolicy.CHECKSUM_POLICY_FAIL
        };
        
        private static final String[] UPDATE_POLICIES = {
                RepositoryPolicy.UPDATE_POLICY_DAILY,
                RepositoryPolicy.UPDATE_POLICY_ALWAYS, 
                RepositoryPolicy.UPDATE_POLICY_NEVER 
        };
        
        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws Descriptor.FormException {
            return true;
        }

        public ListBoxModel doFillReleaseChecksumPolicyItems() {
            return createSelectItems(CHECKSUM_POLICIES);
        }
        
        public ListBoxModel doFillReleaseUpdatePolicyItems() {
            return createSelectItems(UPDATE_POLICIES);
        }
        
        public ListBoxModel doFillSnapshotChecksumPolicyItems() {
            return createSelectItems(CHECKSUM_POLICIES);
        }        
          
        public ListBoxModel doFillSnapshotUpdatePolicyItems() {
            return createSelectItems(UPDATE_POLICIES);
        }
        
        @Override
		public String getDisplayName() {
            return Messages.ArtifactResolver();
        }

        @Override
		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
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
