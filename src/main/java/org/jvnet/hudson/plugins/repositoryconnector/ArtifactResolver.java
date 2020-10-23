package org.jvnet.hudson.plugins.repositoryconnector;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jvnet.hudson.plugins.repositoryconnector.aether.Aether;
import org.jvnet.hudson.plugins.repositoryconnector.aether.AetherBuilder;
import org.jvnet.hudson.plugins.repositoryconnector.aether.AetherException;
import org.jvnet.hudson.plugins.repositoryconnector.aether.AetherBuilderFactory;
import org.jvnet.hudson.plugins.repositoryconnector.util.RepositoryListBox;
import org.jvnet.hudson.plugins.repositoryconnector.util.TokenMacroExpander;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractProject;
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
public class ArtifactResolver extends Builder implements SimpleBuildStep {

    private transient final AetherBuilderFactory aetherFactory;

    private final List<Artifact> artifacts;

    private boolean enableRepositoryLogging;

    private boolean enableTransferLogging;

    private String repositoryId;

    private String targetDirectory;

    @DataBoundConstructor
    public ArtifactResolver(List<Artifact> artifacts) {
        this(artifacts, null);
    }

    // visible for integration testing
    ArtifactResolver(List<Artifact> artifacts, AetherBuilderFactory aetherFactory) {
        this.artifacts = Optional.ofNullable(artifacts)
                .orElse(Collections.emptyList());

        this.aetherFactory = aetherFactory;
    }

    public List<Artifact> getArtifacts() {
        return artifacts;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public String getTargetDirectory() {
        return targetDirectory;
    }

    public boolean isEnableRepositoryLogging() {
        return enableRepositoryLogging;
    }

    public boolean isEnableTransferLogging() {
        return enableTransferLogging;
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
        throws InterruptedException, IOException {

        // TODO: if version parameter in use, verify selected repository matches what is configured here

        TokenMacroExpander tokenExpander = createExpander(run, workspace, listener);
        Aether aether = createAether(run, listener.getLogger());

        try {
            String expandedTarget = tokenExpander.expand(this.targetDirectory);
            FilePath target = expandedTarget == null ? workspace : new FilePath(workspace, expandedTarget);

            for (Artifact artifact : artifacts) {
                Artifact expanded = tokenExpander.expand(artifact);
                download(expanded, aether, target, listener.getLogger());
            }
        } catch (MacroEvaluationException e) {
            throw new AbortException("Maven artifact resolution failed: " + e.getMessage());
        }
    }

    @DataBoundSetter
    public void setEnableRepositoryLogging(boolean enableRepositoryLogging) {
        this.enableRepositoryLogging = enableRepositoryLogging;
    }

    @DataBoundSetter
    public void setEnableTransferLogging(boolean enableTransferLogging) {
        this.enableTransferLogging = enableTransferLogging;
    }

    @DataBoundSetter
    public void setRepositoryId(String repositoryId) {
        this.repositoryId = Util.fixEmpty(repositoryId);
    }

    @DataBoundSetter
    public void setTargetDirectory(String targetDirectory) {
        this.targetDirectory = Util.fixEmpty(targetDirectory);
    }

    // visible for unit testing
    Aether createAether(Run<?, ?> context, PrintStream console) {
        AetherBuilder builder = Optional.ofNullable(aetherFactory)
                .orElse(RepositoryConfiguration.createAetherFactory())
                .createAetherBuilder(context);

        if (enableRepositoryLogging) {
            builder.setRepositoryLogger(console);
        }

        if (enableTransferLogging) {
            builder.setTransferLogger(console);
        }

        return builder.build();
    }

    // visible for unit testing
    TokenMacroExpander createExpander(Run<?, ?> run, FilePath workspace, TaskListener listener) {
        return new TokenMacroExpander(run, listener, workspace);
    }

    private void download(Artifact artifact, Aether aether, FilePath targetDirectory, PrintStream console)
        throws IOException, InterruptedException {

        try {
            File file = aether.resolve(repositoryId, artifact);
            FilePath source = new FilePath(file);

            String targetName = artifact.getTargetFileName();
            FilePath destination = new FilePath(targetDirectory, targetName == null ? source.getName() : targetName);

            if (destination.exists()) {
                destination.delete();
            }

            source.copyTo(destination);
        } catch (AetherException e) {
            if (artifact.isFailOnError()) {
                throw e;
            }

            console.println(String.format("Failed to resolve %s - %s", artifact, e.getMessage()));
        }
    }

    @Extension
    @Symbol("artifactResolver")
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public ListBoxModel doFillRepositoryIdItems() {
            return new RepositoryListBox(RepositoryConfiguration.get().getRepositories())
                    .withSelectAll();
        }

        @Override
        public String getDisplayName() {
            return Messages.ArtifactResolver();
        }

        public boolean hasMultipleRepositories() {
            return RepositoryConfiguration.get().hasMultipleRepositories();
        }

        @Override
        @SuppressWarnings("rawtypes")
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }
    }
}
