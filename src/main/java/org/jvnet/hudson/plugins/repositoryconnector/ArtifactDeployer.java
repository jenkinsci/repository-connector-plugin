package org.jvnet.hudson.plugins.repositoryconnector;

import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;

import java.io.File;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.json.JSONObject;

import org.jvnet.hudson.plugins.repositoryconnector.aether.Aether;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.sonatype.aether.deployment.DeploymentException;
import org.sonatype.aether.util.artifact.DefaultArtifact;

/**
 * This builder allows to resolve artifacts from a repository and copy it to any
 * location.
 * 
 * @author domi
 */
public class ArtifactDeployer extends Builder implements Serializable {

	private static final long serialVersionUID = 1L;

	static Logger log = Logger.getLogger(ArtifactDeployer.class.getName());

	public boolean enableRepoLogging = true;
	public String file;
	public String groupId;
	public String artifactId;
	public String classifier;
	public String version;
	public String extension;
	public String repoId;

	@DataBoundConstructor
	public ArtifactDeployer(String groupId, String artifactId, String classifier, String version, String extension, String file, String repoId,
			boolean enableRepoLogging) {
		this.enableRepoLogging = enableRepoLogging;
		this.file = file;
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.classifier = classifier;
		this.version = version;
		this.extension = extension;
		this.repoId = repoId;
	}

	public boolean enableRepoLogging() {
		return enableRepoLogging;
	}

	/**
	 * gets the artifact
	 * 
	 * @return
	 */
	public org.sonatype.aether.artifact.Artifact getAsArtifact() {
		final DefaultArtifact artifact = new DefaultArtifact(groupId, artifactId, classifier, extension, version);
		artifact.setFile(new File(file));
		return artifact;
	}

	public Set<Repository> getRepos() {
		return getResolverDescriptor().getRepos();
	}

	private Repository getRepoById(String id) {
		for (Repository repo : getRepos()) {
			if (repo.getId().equals(id)) {
				return repo;
			}
		}
		return null;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {

		final PrintStream logger = listener.getLogger();
		final Repository repo = getRepoById(repoId);
		Aether aether = new Aether(repo, new File(getResolverDescriptor().getLocalRepository()), logger, enableRepoLogging);

		try {
			aether.deploy(getAsArtifact(), null, repo.getUrl());
		} catch (DeploymentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}

	private ArtifactResolver.DescriptorImpl getResolverDescriptor() {
		final ArtifactResolver.DescriptorImpl resolverDescriptor = (ArtifactResolver.DescriptorImpl) Hudson.getInstance().getBuilder("ArtifactResolver");
		return resolverDescriptor;
	}

	private boolean logError(String msg, final PrintStream logger, Exception e) {
		log.log(Level.SEVERE, msg, e);
		logger.println(msg);
		e.printStackTrace(logger);
		return true;
	}

	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	// @Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

		public DescriptorImpl() {
			load();
		}

		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			return true;
		}

		public String getDisplayName() {
			return "Artifact Deployer";
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {

			save();
			return true;
		}

	}
}
