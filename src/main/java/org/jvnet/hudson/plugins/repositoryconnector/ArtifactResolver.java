package org.jvnet.hudson.plugins.repositoryconnector;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.ParameterValue;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.ParametersAction;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ListBoxModel;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.jvnet.hudson.plugins.repositoryconnector.aether.Aether;
import org.jvnet.hudson.plugins.repositoryconnector.aether.AetherResult;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.repository.RepositoryPolicy;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.aether.resolution.DependencyResolutionException;
import org.sonatype.aether.version.Version;

/**
 * This builder allows to resolve artifacts from a repository and copy it to any location.
 * 
 * @author domi
 */
public class ArtifactResolver extends Builder implements Serializable {

    private static final long serialVersionUID = 1L;

    private static Logger log = Logger.getLogger(ArtifactResolver.class.getName());

    private static final String DEFAULT_TARGET = "";

    public String targetDirectory;
    public List<Artifact> artifacts;
    public boolean failOnError = true;
    public boolean enableRepoLogging = true;
    public final String repoid;
    public final String snapshotUpdatePolicy;
    public final String releaseUpdatePolicy;
    public final String snapshotChecksumPolicy;
    public final String releaseChecksumPolicy;
    
    @DataBoundConstructor
    public ArtifactResolver(String repoid, String targetDirectory, List<Artifact> artifacts, boolean failOnError, boolean enableRepoLogging, String snapshotUpdatePolicy,
            String snapshotChecksumPolicy, String releaseUpdatePolicy, String releaseChecksumPolicy) {
    	this.repoid = repoid;
        this.artifacts = artifacts != null ? artifacts : new ArrayList<Artifact>();
        this.targetDirectory = StringUtils.isBlank(targetDirectory) ? DEFAULT_TARGET : targetDirectory;
        this.failOnError = failOnError;
        this.enableRepoLogging = enableRepoLogging;
        this.releaseUpdatePolicy = releaseUpdatePolicy;
        this.releaseChecksumPolicy = RepositoryPolicy.CHECKSUM_POLICY_WARN;
        this.snapshotUpdatePolicy = snapshotUpdatePolicy;
        this.snapshotChecksumPolicy = RepositoryPolicy.CHECKSUM_POLICY_WARN;
    }

    public String getTargetDirectory() {
        return StringUtils.isBlank(targetDirectory) ? DEFAULT_TARGET : targetDirectory;
    }

    public String repoid() {
    	return repoid;
    }
    
    public boolean failOnError() {
        return failOnError;
    }

    public boolean enableRepoLogging() {
        return enableRepoLogging;
    }

    /**
     * gets the artifacts
     * 
     * @return
     */
    public List<Artifact> getArtifacts() {
        return artifacts;
    }
    
    public Collection<Repository> getRepos(AbstractBuild<?,?> build, String repoid) {
    	List<Repository> out = new ArrayList<Repository>();
    	
    	for(Map.Entry<String,Repository> e : RepositoryConfiguration.get().getRepositoryMap().entrySet()) {
    		if("ALL".equals(repoid) || repoid.equals(e.getKey())) {
    			out.add(e.getValue());
    		}
    	}
    	return out;
    }
    

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {

        final PrintStream logger = listener.getLogger();
        final Collection<Repository> repositories = getRepos(build, repoid);

        File localRepo = RepositoryConfiguration.get().getLocalRepoPath();
        boolean failed = download(build, listener, logger, repositories, localRepo);
        
        if (failed && failOnError) {
            return false;
        }
        return true;
    }

    private boolean download(AbstractBuild<?, ?> build, BuildListener listener, final PrintStream logger, final Collection<Repository> repositories, File localRepository) {
        boolean hasError = false;

        Aether aether = new Aether(repositories, localRepository, logger, enableRepoLogging, snapshotUpdatePolicy, snapshotChecksumPolicy, releaseUpdatePolicy, releaseChecksumPolicy);

        for (Artifact a : artifacts) {

            try {

                final String classifier = TokenMacro.expandAll(build, listener, a.getClassifier());
                final String artifactId = TokenMacro.expandAll(build, listener, a.getArtifactId());
                final String groupId = TokenMacro.expandAll(build, listener, a.getGroupId());
                final String extension = TokenMacro.expandAll(build, listener, a.getExtension());
                final String targetFileName = TokenMacro.expandAll(build, listener, a.getTargetFileName());

                String version = TokenMacro.expandAll(build, listener, a.getVersion());

                version = checkVersionOverride(build, listener, groupId, artifactId, version);
                
                if(version==null) {
                	version = "LATEST";
             		logger.println("no version set, resolved version: "+version);
                	/**
                	List<Version> versions = aether.resolveVersions(groupId, artifactId);
                	for(Version v : versions) {
                		logger.println("available version: "+v.getClass().getName()+" / "+v.toString());
                	}
                	if(versions.size()>0) {
                		version = versions.get(versions.size()-1).toString();
                		logger.println("no version set, resolved version: "+version);
                	}
                	**/
                }
         		logger.println("version is: "+version);

                AetherResult result = aether.resolve(groupId, artifactId, classifier, extension, version);

                List<File> resolvedFiles = result.getResolvedFiles();
                for (File file : resolvedFiles) {

                    String fileName = StringUtils.isBlank(targetFileName) ? file.getName() : targetFileName;
                    FilePath source = new FilePath(file);
                    String targetDir = StringUtils.isNotBlank(getTargetDirectory()) ? getTargetDirectory() + "/" : "";  
                    FilePath target = new FilePath(build.getWorkspace(), targetDir + fileName);
                    boolean wasDeleted = target.delete();
                    if (wasDeleted) {
                        logger.println("deleted " + target.toURI());
                    }
                    logger.println("copy " + file + " to " + target.toURI());
                    source.copyTo(target);

                }

            } catch (DependencyCollectionException e) {
                hasError = logError("failed collecting dependency info for " + a, logger, e);
            } catch (ArtifactResolutionException e) {
                hasError = logError("failed to resolve artifact for " + a, logger, e);
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
     * @param build the build
     * @param listener the build listener
     * @param groupId the Maven group id
     * @param artifactId the Maven artifact id
     * @param version the version
     * @return The overridden version
     */
    private String checkVersionOverride(AbstractBuild<?, ?> build, BuildListener listener, String groupId, String artifactId, String version) {
        String result = version;
        List<ParametersAction> parameterActionList = build.getActions(ParametersAction.class);
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

    public DescriptorImpl getDescriptor() {
        return DESCRIPTOR;
    }

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public DescriptorImpl() {
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }
        
        public ListBoxModel doFillRepoidItems() {
        	ListBoxModel repoList = new ListBoxModel();
        	repoList.add("ALL","ALL");
        	for(String v : RepositoryConfiguration.get().getRepositoryMap().keySet()) {
        		repoList.add(v,v);
        	}
            return repoList; 
        }

        public String getDisplayName() {
            return Messages.ArtifactResolver();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws Descriptor.FormException {
            return true;
        }
    }
}
