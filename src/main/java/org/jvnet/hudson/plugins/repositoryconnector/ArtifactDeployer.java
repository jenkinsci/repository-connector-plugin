package org.jvnet.hudson.plugins.repositoryconnector;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.json.JSONObject;

import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.jvnet.hudson.plugins.repositoryconnector.aether.Aether;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.sonatype.aether.deployment.DeploymentException;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.artifact.SubArtifact;

/**
 * This builder allows to resolve artifacts from a repository and copy it to any location.
 * 
 * @author domi
 */
public class ArtifactDeployer extends Notifier implements Serializable {

    private static final long serialVersionUID = 1L;

    static Logger log = Logger.getLogger(ArtifactDeployer.class.getName());

    public final UserPwd overwriteSecurity;
    public boolean enableRepoLogging = true;
    public final String repoId;
    public final String snapshotRepoId;
    public List<Artifact> artifacts;

    @DataBoundConstructor
    public ArtifactDeployer(List<Artifact> artifacts, String repoId, String snapshotRepoId, UserPwd overwriteSecurity, boolean enableRepoLogging) {
        this.enableRepoLogging = enableRepoLogging;
        this.artifacts = artifacts != null ? artifacts : new ArrayList<Artifact>();
        this.repoId = repoId;
        this.snapshotRepoId = snapshotRepoId;
        this.overwriteSecurity = overwriteSecurity;
        System.out.println(this.overwriteSecurity);
    }

    public boolean enableRepoLogging() {
        return enableRepoLogging;
    }

    public boolean isOverwriteSecurity() {
        return overwriteSecurity != null;
    }

    public String getUserName() {
        if (isOverwriteSecurity()) {
            return overwriteSecurity.user;
        }
        return null;
    }

    public String getPassword() {
        if (isOverwriteSecurity()) {
            return overwriteSecurity.password;
        }
        return null;
    }

    public Collection<Repository> getRepos() {
        return RepositoryConfiguration.get().getRepos();
    }

    private Repository getRepoById(String id) {
        return RepositoryConfiguration.get().getRepositoryMap().get(id);
    }

    /*
     * @see hudson.tasks.BuildStep#getRequiredMonitorService()
     */
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {

        final PrintStream logger = listener.getLogger();

        Aether aether = new Aether(new File(RepositoryConfiguration.get().getLocalRepository()), logger, enableRepoLogging);

        try {
            for (Artifact a : artifacts) {

                final String version = TokenMacro.expandAll(build, listener, a.getVersion());
                final String classifier = TokenMacro.expandAll(build, listener, a.getClassifier());
                final String artifactId = TokenMacro.expandAll(build, listener, a.getArtifactId());
                final String groupId = TokenMacro.expandAll(build, listener, a.getGroupId());
                final String packaging = TokenMacro.expandAll(build, listener, a.getExtension());
                final String targetFileName = TokenMacro.expandAll(build, listener, a.getTargetFileName());

                Artifact aTmp = new Artifact(groupId, artifactId, version);
                aTmp.setClassifier(classifier);
                aTmp.setExtension(packaging);
                aTmp.setTargetFileName(targetFileName);
                
                String aTmpFileName = aTmp.getTargetFileName();
                FilePath source = new FilePath(build.getWorkspace(), aTmpFileName);
                String f = new File(aTmpFileName).getName();
                int dotPos = f.lastIndexOf(".");
                String extension = f.substring(dotPos + 1);
                final File targetFile = File.createTempFile(f, "." + extension);
                FilePath target = new FilePath(targetFile);

                logger.println("INFO: copy source " + source.toURI() + " to master " + target.toURI());
                source.copyTo(target);

                org.sonatype.aether.artifact.Artifact artifact = new DefaultArtifact(groupId, artifactId, classifier, extension, version);
                logger.println("INFO: deploy artifact " + aTmp);
                artifact = artifact.setFile(targetFile);
                org.sonatype.aether.artifact.Artifact pom = new SubArtifact(artifact, "", "pom");
                final File tmpPom = getTempPom(aTmp);
                pom = pom.setFile(tmpPom);

                final String tmpRepoId = version.contains("SNAPSHOT") ? snapshotRepoId : repoId;
                Repository repo = getRepoById(tmpRepoId);
                logger.println("INFO: deploy to repository " + repo);
                if (isOverwriteSecurity()) {
                    logger.println("INFO: define repo access security...");
                    String tmpuser = TokenMacro.expandAll(build, listener, overwriteSecurity.user);
                    String tmppwd = TokenMacro.expandAll(build, listener, overwriteSecurity.password);
                    repo = new Repository(repo.getId(), repo.getType(), repo.getUrl(), tmpuser, tmppwd, repo.isRepositoryManager());
                }

                aether.install(artifact, pom);
                aether.deploy(repo, artifact, pom);

                // clean the resources
                targetFile.delete();
                tmpPom.delete();
            }
        } catch (DeploymentException e) {
            logger.println("ERROR: possible causes: 1. in case of a SNAPSHOT deployment: does your remote repository allow SNAPSHOT deployments?, 2. in case of a release dpeloyment: is this version of the artifact already deployed then does your repository allow updating artifacts?");
            return logError("DeploymentException: ", logger, e);
        } catch (IOException e) {
            return logError("IOException: ", logger, e);
        } catch (InterruptedException e) {
            return logError("InterruptedException: ", logger, e);
        } catch (Exception e) {
            return logError("Exception: ", logger, e);
        }
        return true;
    }

    private boolean logError(String msg, final PrintStream logger, Exception e) {
        log.log(Level.SEVERE, msg, e);
        logger.println(msg);
        e.printStackTrace(logger);
        return false;
    }

    public DescriptorImpl getDescriptor() {
        return DESCRIPTOR;
    }

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        public DescriptorImpl() {
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // TODO: This disables the extension => change to true
            return false;
        }

        public String getDisplayName() {
            return Messages.ArtifactDeployer();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            return true;
        }
    }

    private File getTempPom(Artifact artifact) {
        File tmpPom = null;
        try {
            final String preparedPom = this.preparedPom(artifact);
            tmpPom = File.createTempFile("pom" + artifact.getArtifactId(), ".xml");
            FileUtils.writeStringToFile(tmpPom, preparedPom);
        } catch (IOException e) {
            log.log(Level.SEVERE, "not able to create temporal pom: " + e.getMessage());
        }
        return tmpPom;
    }

    private String preparedPom(Artifact artifact) {
        String pomContent = null;
        try {
            final InputStream stream = this.getClass().getResourceAsStream("/org/jvnet/hudson/plugins/repositoryconnector/ArtifactDeployer/pom.tmpl");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream));
            StringBuilder stringBuilder = new StringBuilder();
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            bufferedReader.close();
            pomContent = stringBuilder.toString();
            pomContent = pomContent.replace("ARTIFACTID", artifact.getArtifactId());
            pomContent = pomContent.replace("GROUPID", artifact.getGroupId());
            pomContent = pomContent.replace("VERSION", artifact.getVersion());
            // FIXME how to handle packaging vs extension?
            pomContent = pomContent.replace("PACKAGING", artifact.getExtension());

        } catch (Exception e) {
            log.log(Level.SEVERE, "not able to create temporal pom: " + e.getMessage());
        }
        if (log.isLoggable(Level.FINE)) {
            log.log(Level.FINE, "used POM: " + pomContent);
        }
        return pomContent;
    }
}
