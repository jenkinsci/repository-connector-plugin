package org.jvnet.hudson.plugins.repositoryconnector.aether;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.ProxySelector;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RemoteRepository.Builder;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.jvnet.hudson.plugins.repositoryconnector.Repository;
import org.jvnet.hudson.plugins.repositoryconnector.Repository.RepositoryType;

class RemoteRepositoryFactory {

    private final Function<Repository, Authentication> credentials;

    private final ProxySelector proxySelector;

    private final Collection<Repository> repositories;

    RemoteRepositoryFactory(Collection<Repository> repositories, ProxySelector proxySelector,
            Function<Repository, Authentication> credentials) {
        this.repositories = repositories;
        this.credentials = credentials;
        this.proxySelector = proxySelector;
    }

    RemoteRepository getDeloymentRepository(String repositoryId, boolean snapshot) throws AetherException {
        if (repositoryId == null && repositories.size() > 1) {
            throw new AetherException("repositoryId not specified and multiple repositories are configured, unable to deploy!");
        }

        Repository repository = Optional.ofNullable(repositoryId)
                .map(this::findRepository)
                .map(found -> mapToDeployable(found, snapshot))
                .orElseThrow(() -> new AetherException("no repository configuration found for id [" + repositoryId + "]"));

        return createRepositoryBuilder(repository).build();
    }

    List<RemoteRepository> getResolutionRepositories(String repositoryId) throws AetherException {
        if (repositories.size() < 1) {
            throw new AetherException("no repositories are configured, unable to perform artifact resolution!");
        }

        return repositories.stream()
                .filter(repository -> repositoryId == null || repository.getId().equals(repositoryId))
                .map(repository -> {
                    RemoteRepository.Builder builder = createRepositoryBuilder(repository);

                    addReleasePolicy(builder, repository);
                    addSnapshotPolicy(builder, repository);

                    return builder.build();
                })
                .collect(Collectors.toList());
    }

    private void addReleasePolicy(Builder builder, Repository repository) {
        builder.setReleasePolicy(createRepositoryPolicy(repository.isEnableReleaseRepository(), repository.getReleaseRepository()));
    }

    private void addSnapshotPolicy(Builder builder, Repository repository) {
        builder.setReleasePolicy(createRepositoryPolicy(repository.isEnableSnapshotRepository(), repository.getSnapshotRepository()));
    }

    private RemoteRepository.Builder createRepositoryBuilder(Repository repository) {
        RemoteRepository.Builder builder = new RemoteRepository.Builder(repository.getId(), repository.getType(), repository.getUrl());
        builder.setAuthentication(credentials.apply(repository));

        if (proxySelector != null) {
            // irritating
            builder.setProxy(proxySelector.getProxy(builder.build()));
        }

        return builder;
    }

    private RepositoryPolicy createRepositoryPolicy(boolean enabled, Repository.RepositoryType policy) {
        return Optional.ofNullable(policy)
                .map(p -> new RepositoryPolicy(enabled, p.getUpdate(), p.getChecksum()))
                .orElse(new RepositoryPolicy(false, null, null));
    }

    private Repository findRepository(String repositoryId) {
        return repositories
                .stream()
                .filter(repository -> repository.getId().equals(repositoryId))
                .findFirst()
                .get();
    }

    private Repository mapToDeployable(Repository repository, boolean snapshot) {
        RepositoryType type = snapshot ? repository.getSnapshotRepository() : repository.getReleaseRepository();

        String endpoint = Optional.ofNullable(type.getUrl())
                .orElse(repository.getUrl());

        String credentialsId = Optional.ofNullable(type.getCredentialsId())
                .orElse(repository.getCredentialsId());

        Repository deployable = new Repository(repository.getId(), endpoint);
        deployable.setCredentialsId(credentialsId);

        return deployable;
    }

}
