package org.jvnet.hudson.plugins.repositoryconnector.aether;

/*
 * Copyright (c) 2010 Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0, 
 * and you may not use this file except in compliance with the Apache License Version 2.0. 
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, 
 * software distributed under the Apache License Version 2.0 is distributed on an 
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jenkins.model.Jenkins;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.repository.internal.DefaultServiceLocator;
import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.jvnet.hudson.plugins.repositoryconnector.Repository;
import org.omg.stub.java.rmi._Remote_Stub;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.connector.wagon.WagonProvider;
import org.sonatype.aether.connector.wagon.WagonRepositoryConnectorFactory;
import org.sonatype.aether.deployment.DeployRequest;
import org.sonatype.aether.deployment.DeploymentException;
import org.sonatype.aether.installation.InstallRequest;
import org.sonatype.aether.installation.InstallationException;
import org.sonatype.aether.repository.Authentication;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.LocalRepositoryManager;
import org.sonatype.aether.repository.Proxy;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.repository.RepositoryPolicy;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.resolution.DependencyResolutionException;
import org.sonatype.aether.resolution.VersionRangeRequest;
import org.sonatype.aether.resolution.VersionRangeResolutionException;
import org.sonatype.aether.resolution.VersionRangeResult;
import org.sonatype.aether.spi.connector.RepositoryConnectorFactory;
import org.sonatype.aether.util.DefaultRepositorySystemSession;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.repository.DefaultProxySelector;
import org.sonatype.aether.version.Version;

public class Aether {
        private static final Logger log = Logger.getLogger(Aether.class.getName());

    private final List<RemoteRepository> repositories = new ArrayList<RemoteRepository>();
    private final RepositorySystem repositorySystem;
    private final LocalRepository localRepository;
    private final PrintStream logger;
    private final boolean extendedLogging;
    public String snapshotUpdatePolicy;
    public String releaseUpdatePolicy;
    public String snapshotChecksumPolicy;
    public String releaseChecksumPolicy;

    public Aether(Collection<Repository> remoteRepositories, File localRepository) {
            this(remoteRepositories, localRepository, null, false, RepositoryPolicy.UPDATE_POLICY_NEVER, 
                    RepositoryPolicy.CHECKSUM_POLICY_IGNORE, RepositoryPolicy.UPDATE_POLICY_NEVER, RepositoryPolicy.CHECKSUM_POLICY_IGNORE);
        }

    public Aether(Collection<Repository> remoteRepositories, File localRepository, PrintStream logger, boolean extendedLogging,
            String snapshotUpdatePolicy, String snapshotChecksumPolicy, String releaseUpdatePolicy, String releaseChecksumPolicy) {
        this.logger = logger==null?System.out:logger;
        this.repositorySystem = newManualSystem();
        if(localRepository!=null) {
        	this.localRepository = new LocalRepository(localRepository);
        } else {
        	this.localRepository = null;
        }
        this.extendedLogging = extendedLogging;
        this.releaseUpdatePolicy = releaseUpdatePolicy;
        this.releaseChecksumPolicy = releaseChecksumPolicy;
        this.snapshotUpdatePolicy = snapshotUpdatePolicy;
        this.snapshotChecksumPolicy = snapshotChecksumPolicy;
        this.initRemoteRepos(remoteRepositories);
    }

    public Aether(File localRepository, PrintStream logger, boolean extendedLogging) {
        this.logger = logger;
        this.localRepository = new LocalRepository(localRepository);
        this.extendedLogging = extendedLogging;
        this.repositorySystem = newManualSystem();
    }

    private void initRemoteRepos(Collection<Repository> remoteRepositories) {
        for (Repository repo : remoteRepositories) {
            if (logger != null) {
                logger.println("INFO: define repo: " + repo);
            }
            RemoteRepository repoObj = new RemoteRepository(repo.getId(), repo.getType(), repo.getUrl());
            
            RepositoryPolicy snapshotPolicy = new RepositoryPolicy(true, snapshotUpdatePolicy, snapshotChecksumPolicy);
            RepositoryPolicy releasePolicy = new RepositoryPolicy(true, releaseUpdatePolicy, releaseChecksumPolicy);
            final String user = repo.getUser();
            if (!StringUtils.isBlank(user)) {
                if (logger != null) {
                    logger.println("INFO: set authentication for " + user);
                }
                Authentication authentication = new Authentication(user, repo.getPassword());
                repoObj.setAuthentication(authentication);
            }
            log.log(Level.INFO, " ---- adding repo: { user : '"+user+"', url = '"+repo.getUrl()+"', isRepoManager : "+repo.isRepositoryManager()+" } ");
            repoObj.setRepositoryManager(repo.isRepositoryManager());
            repoObj.setPolicy(true, snapshotPolicy);
            repoObj.setPolicy(false, releasePolicy);
            
            if (repoObj.isRepositoryManager()) {
                // well, in case of repository manager, let's have a look one step deeper
                // @see org.sonatype.aether.impl.internal.DefaultMetadataResolver#getEnabledSourceRepositories(org.sonatype.aether.repository.RemoteRepository, org.sonatype.aether.metadata.Metadata.Nature)
            	List<RemoteRepository> repos = resolveMirrors(repoObj);
            	for(RemoteRepository rr : repos) {
            		log.log(Level.INFO, "adding mirror: "+rr.getUrl());
            	}
                repoObj.setMirroredRepositories(repos);
            }
            repositories.add(repoObj);
        }
    }
    
	private void addProxySelectorIfNecessary(DefaultRepositorySystemSession repositorySession) {
		Jenkins jenkins = Jenkins.getInstance();
		if (jenkins.proxy != null && StringUtils.isNotBlank(jenkins.proxy.name)) {
			DefaultProxySelector proxySelector = new DefaultProxySelector();
			Authentication authenticator = new Authentication(jenkins.proxy.getUserName(), jenkins.proxy.getPassword());

			Proxy httpProxy = new Proxy("http", jenkins.proxy.name, jenkins.proxy.port, authenticator);
			Proxy httpsProxy = new Proxy("https", jenkins.proxy.name, jenkins.proxy.port, authenticator);

			String nonProxySettings = convertHudsonNonProxyToJavaNonProxy(jenkins.proxy.noProxyHost);

			proxySelector.add(httpProxy, nonProxySettings);
			proxySelector.add(httpsProxy, nonProxySettings);

			log.log(Level.FINE, "Setting proxy for Aether: host={0}, port={1}, user={2}, password=******, nonProxyHosts={3}",
					new Object[] { jenkins.proxy.name, jenkins.proxy.port, jenkins.proxy.getUserName(), nonProxySettings });
			repositorySession.setProxySelector(proxySelector);
		}
	}

	public String convertHudsonNonProxyToJavaNonProxy(String hudsonNonProxy) {
        if (StringUtils.isEmpty(hudsonNonProxy)) {
            return "";
        }
		String[] nonProxyArray = hudsonNonProxy.split("[ \t\n,|]+");
		String nonProxyOneLine = StringUtils.join(nonProxyArray, '|');
		return nonProxyOneLine;
	}

    /**
     * Resolve mirrors configured in this repository... Or fake it...
     *
     * @param repository the repository
     * @return the list of mirrored repositories
     */
    private List<RemoteRepository> resolveMirrors(RemoteRepository repository) {
        // unfortunately, at this point we don't have a lib to parse the 'meta/repository-metadata.xml' and extract the mirrors
        // just push the repository in the list of mirrored to enable artifact resolution
        return Arrays.asList(new RemoteRepository(repository));
    }


    private RepositorySystem newManualSystem() {
        DefaultServiceLocator locator = new DefaultServiceLocator();
        locator.setServices(WagonProvider.class, new org.jvnet.hudson.plugins.repositoryconnector.wagon.ManualWagonProvider());
        locator.addService(RepositoryConnectorFactory.class, WagonRepositoryConnectorFactory.class);
        return locator.getService(RepositorySystem.class);
    }

    private RepositorySystemSession newSession() {
        MavenRepositorySystemSession session = new MavenRepositorySystemSession();
        session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(localRepository));
        
        if (extendedLogging && logger != null) {
            session.setTransferListener(new ConsoleTransferListener(logger));
            session.setRepositoryListener(new ConsoleRepositoryListener(logger));
        }
        addProxySelectorIfNecessary(session);
        return session;
    }

    public AetherResult resolve(String groupId, String artifactId, String classifier, String extension, String version) throws DependencyCollectionException, ArtifactResolutionException, DependencyResolutionException {
        RepositorySystemSession session = newSession();

        ArtifactRequest aReq = new ArtifactRequest();
        Artifact a = new DefaultArtifact(groupId, artifactId, classifier, extension, version);
        aReq.setArtifact(a);
        aReq.setRepositories(repositories);
        
        ArtifactResult aRes = repositorySystem.resolveArtifact(session, aReq);
        List<File> files = new ArrayList<File>();
        files.add(aRes.getArtifact().getFile());
        
        AetherResult out = new AetherResult(null, files);
        
        return out;
    }

    public List<Version> resolveVersions(String groupId, String artifactId, String extension) throws VersionRangeResolutionException {

        List<Version> versions = new ArrayList<Version>();
        
        for(RemoteRepository r : repositories) {
        	try {
            	log.log(Level.INFO, " ---- new repo session .... "+r.getUrl());
            	MavenRepositorySystemSession session = new MavenRepositorySystemSession();
            	log.log(Level.INFO, " ---- setting update policy ... ");
            	session.setUpdatePolicy(RepositoryPolicy.UPDATE_POLICY_ALWAYS);
            	LocalRepositoryManager lrm = repositorySystem.newLocalRepositoryManager(localRepository);
            	session.setLocalRepositoryManager(lrm);
            	log.log(Level.INFO, " ---- creating artifact query ... ");
                Artifact artifact = new DefaultArtifact(groupId, artifactId, null, extension, "(0,)");

                VersionRangeRequest rangeRequest = new VersionRangeRequest();
                rangeRequest.setArtifact(artifact);
                rangeRequest.setRepositories(Collections.singletonList(r));
                
        		log.log(Level.INFO, " ---- executing aether query: "+r.getUrl());
        		VersionRangeResult rangeResult = repositorySystem.resolveVersionRange(session, rangeRequest);
        		List<Version> vs = rangeResult.getVersions(); 
        		log.log(Level.INFO, " ---- executing aether query: "+r.getUrl()+" / "+vs.size()+" versions found ... ");
                versions.addAll(vs);
			} catch (Exception e) {
        		log.log(Level.INFO, " ---- adding to eather query failed for: "+r.getUrl(),e);
			}
        }

        return versions;
    }

    public void install(Artifact artifact, Artifact pom) throws InstallationException {
        RepositorySystemSession session = newSession();

        InstallRequest installRequest = new InstallRequest();
        installRequest.addArtifact(artifact).addArtifact(pom);

        repositorySystem.install(session, installRequest);
    }

    public void deploy(Repository repository, Artifact artifact, Artifact pom) throws DeploymentException {
        RepositorySystemSession session = newSession();

        RemoteRepository repoObj = new RemoteRepository(repository.getId(), repository.getType(), repository.getUrl());
        repoObj.setRepositoryManager(repository.isRepositoryManager());
        final String user = repository.getUser();
        if (!StringUtils.isBlank(user)) {
                        if (logger != null) {
                            logger.println("INFO: set authentication for " + user);
                        }
            Authentication authentication = new Authentication(user, repository.getPassword());
            repoObj.setAuthentication(authentication);
        }

        DeployRequest deployRequest = new DeployRequest();
        deployRequest.addArtifact(artifact);
        deployRequest.addArtifact(pom);
        deployRequest.setRepository(repoObj);

        repositorySystem.deploy(session, deployRequest);
    }
}
