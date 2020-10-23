package org.jvnet.hudson.plugins.repositoryconnector.aether;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.deployment.DeploymentException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.installation.InstallRequest;
import org.eclipse.aether.installation.InstallationException;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.util.artifact.SubArtifact;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.jvnet.hudson.plugins.repositoryconnector.Artifact;
import org.jvnet.hudson.plugins.repositoryconnector.util.VersionFilter;

public class Aether {    
    private final RemoteRepositoryFactory factory;

    private final RepositorySystem repositorySystem;

    private final RepositorySystemSession session;

    Aether(RemoteRepositoryFactory factory, RepositorySystem repositorySystem, RepositorySystemSession repositorySession) {
        this.factory = factory;
        this.session = repositorySession;
        this.repositorySystem = repositorySystem;
    }

    public Collection<File> deploy(String repositoryId, Artifact artifact) throws AetherException {
        try {
            DefaultArtifact toDeploy = createInstallableArtifact(artifact);
            SubArtifact pom = createPomArtifact(toDeploy, artifact);
            
            boolean snapshot = VersionFilter.isSnapshot(artifact.getVersion());

            RemoteRepository destination = factory.getDeloymentRepository(repositoryId, snapshot);
            DeployRequest request = new DeployRequest().addArtifact(toDeploy)
                    .addArtifact(pom)
                    .setRepository(destination);

            return repositorySystem.deploy(session, request)
                    .getArtifacts()
                    .stream()
                    .map(installed -> installed.getFile())
                    .collect(Collectors.toList());

        } catch (DeploymentException e) {
            throw aetherException(e);
        }
    }

    public boolean hasAvailableVersions(String repositoryId, String groupId, String artifactId, VersionFilter filter)
        throws AetherException {

        // oldestFirst doesn't matter
        return !resolveAvailableVersions(repositoryId, new Artifact(groupId, artifactId, null), false, filter).isEmpty();
    }

    public Collection<File> install(Artifact artifact) throws AetherException {
        try {
            DefaultArtifact toInstall = createInstallableArtifact(artifact);
            SubArtifact pom = createPomArtifact(toInstall, artifact);

            InstallRequest request = new InstallRequest().addArtifact(toInstall)
                    .addArtifact(pom);

            return repositorySystem.install(session, request)
                    .getArtifacts()
                    .stream()
                    .map(installed -> installed.getFile())
                    .collect(Collectors.toList());
        } catch (InstallationException e) {
            throw aetherException(e);
        }
    }

    public File resolve(String repositoryId, Artifact artifact) throws AetherException {
        try {
            List<RemoteRepository> repositories = factory.getResolutionRepositories(repositoryId);

            ArtifactRequest request = new ArtifactRequest(createResolvableArtifact(artifact), repositories, null);
            ArtifactResult result = repositorySystem.resolveArtifact(session, request);

            if (result.isMissing()) {
                throw new ArtifactResolutionException(Arrays.asList(result));
            }

            return result.getArtifact().getFile();
        } catch (ArtifactResolutionException e) {
            throw aetherException(e);
        }
    }

    public Collection<String> resolveAvailableVersions(String repositoryId, Artifact artifact, boolean oldestFirst, VersionFilter filter)
        throws AetherException {

        try {
            DefaultArtifact toResolve = createResolvableArtifact(artifact);
            List<RemoteRepository> repositories = factory.getResolutionRepositories(repositoryId);
            
            VersionRangeRequest request = new VersionRangeRequest(toResolve.setVersion("[0,)"), repositories, null);
            /*-
             *  this call includes SNAPSHOT versions contained in 'maven-metadata.xml', regardless of the repository 
             *  policy, so additional filtering needs to occur
             */
            VersionRangeResult result = repositorySystem.resolveVersionRange(session, request);

            // TODO: log exceptions from 'result.getExceptions()' and throw an error

            return result.getVersions()
                    .stream()
                    .sorted((o1, o2) -> oldestFirst ? o1.compareTo(o2) : o2.compareTo(o1))
                    .map(version -> version.toString())
                    .filter(filter::apply)
                    .collect(Collectors.toList());
        } catch (VersionRangeResolutionException e) {
            throw aetherException(e);
        }
    }

    public Collection<File> resolveWithDependencies(String repositoryId, Artifact artifact, String scope) throws AetherException {
        try {
            Dependency dependency = new Dependency(createResolvableArtifact(artifact), scope);
            DependencyFilter classpathFilter = DependencyFilterUtils.classpathFilter(scope);

            List<RemoteRepository> repositories = factory.getResolutionRepositories(repositoryId);

            DependencyRequest request = new DependencyRequest(new CollectRequest(dependency, repositories), classpathFilter);
            DependencyResult result = repositorySystem.resolveDependencies(session, request);

            // TODO: log exceptions from 'result.getCollectExceptions()' and throw an error

            return result.getArtifactResults()
                    .stream()
                    .map(resolved -> resolved.getArtifact().getFile())
                    .collect(Collectors.toList());

        } catch (DependencyResolutionException e) {
            throw aetherException(e);
        }
    }

    private AetherException aetherException(Throwable cause) {
        return new AetherException(cause.getMessage());
    }

    private DefaultArtifact createInstallableArtifact(Artifact artifact) {
        // the passed artifact should have everything already configured
        DefaultArtifact toInstall = createResolvableArtifact(artifact);
        return (DefaultArtifact) toInstall.setFile(new File(artifact.getTargetFileName()));
    }

    private SubArtifact createPomArtifact(DefaultArtifact parent, Artifact artifact) {
        return new SubArtifact(parent, null, "pom", new File(artifact.getPomFile()));
    }

    private DefaultArtifact createResolvableArtifact(Artifact artifact) {
        String extension = Optional.ofNullable(artifact.getExtension())
                .orElse("jar");

        return new DefaultArtifact(artifact.getGroupId(),
                artifact.getArtifactId(),
                artifact.getClassifier(),
                extension,
                artifact.getVersion());
    }
}
