package org.jvnet.hudson.plugins.repositoryconnector;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import net.sf.json.JSONObject;

import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jvnet.hudson.plugins.repositoryconnector.aether.Aether;
import org.jvnet.hudson.plugins.repositoryconnector.aether.AetherBuilder;
import org.jvnet.hudson.plugins.repositoryconnector.aether.AetherException;
import org.jvnet.hudson.plugins.repositoryconnector.aether.AetherBuilderFactory;
import org.jvnet.hudson.plugins.repositoryconnector.util.FilePathUtils;
import org.jvnet.hudson.plugins.repositoryconnector.util.PomGenerator;
import org.jvnet.hudson.plugins.repositoryconnector.util.RepositoryListBox;
import org.jvnet.hudson.plugins.repositoryconnector.util.TokenMacroExpander;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildStep;

/**
 * This builder allows to resolve artifacts from a repository and copy it to any location.
 * 
 * @author domi
 */
public class ArtifactDeployer extends Notifier implements SimpleBuildStep {

    private transient final AetherBuilderFactory aetherFactory;

    private final List<Artifact> artifacts;

    private boolean enableRepositoryLogging;

    private boolean enableTransferLogging;

    private String repositoryId;

    @DataBoundConstructor
    public ArtifactDeployer(List<Artifact> artifacts) {
        this(artifacts, null);
    }

    // visible for integration testing
    ArtifactDeployer(List<Artifact> artifacts, AetherBuilderFactory aetherFactory) {
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

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
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

        TokenMacroExpander tokenExpander = createExpander(run, workspace, listener);
        Aether aether = createAether(run, listener.getLogger());

        try {
            for (Artifact artifact : artifacts) {
                Artifact expanded = tokenExpander.expand(artifact);

                File pom = copyPomToLocal(workspace, expanded);
                File local = copyArtifactToLocal(workspace, expanded);

                deploy(expanded, local, pom, aether, listener.getLogger());
            }
        } catch (MacroEvaluationException e) {
            throw new AbortException("Maven artifact deployment failed: " + e.getMessage());
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
        this.repositoryId = repositoryId;
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

    private File copyArtifactToLocal(FilePath workspace, Artifact artifact) throws IOException, InterruptedException {
        FilePath toUpload = new FilePath(workspace, artifact.getTargetFileName());
        return FilePathUtils.copyToLocal(toUpload);
    }

    private File copyPomToLocal(FilePath workspace, Artifact artifact) throws IOException, InterruptedException {
        String pomFile = artifact.getPomFile();
        if (pomFile == null) {
            return PomGenerator.generate(artifact);
        }

        return FilePathUtils.copyToLocal(new FilePath(workspace, pomFile));
    }

    @SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED_BAD_PRACTICE", justification = "delete")
    private void deploy(Artifact artifact, File local, File pom, Aether aether, PrintStream console) throws AetherException {
        // this is already a copy from the expansion, so safe...
        artifact.setPomFile(pom.getAbsolutePath());
        artifact.setTargetFileName(local.getAbsolutePath());

        try {
            if (artifact.isDeployToLocal()) {
                aether.install(artifact);
            }

            if (artifact.isDeployToRemote()) {
                aether.deploy(repositoryId, artifact);
            }
        } catch (AetherException e) {
            if (artifact.isFailOnError()) {
                throw e;
            }

            console.println(String.format("Warning: failed to deploy %s - %s", artifact, e.getMessage()));
        }
        finally {
            // there are cases this misses, but they will be cleaned up by the os or on shutdown
            pom.delete();
            local.delete();
        }
    }

    @Extension
    @Symbol("artifactDeployer")
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            return true;
        }

        public ListBoxModel doFillRepositoryIdItems() {
            return new RepositoryListBox(RepositoryConfiguration.get().getRepositories());
        }

        @Override
        public String getDisplayName() {
            return Messages.ArtifactDeployer();
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
