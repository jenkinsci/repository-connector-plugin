package org.jvnet.hudson.plugins.repositoryconnector.aether;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.ProxySelector;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.plugins.repositoryconnector.Repository;
import org.jvnet.hudson.plugins.repositoryconnector.Repository.RepositoryType;
import org.mockito.Mock;

public class RemoteRepositoryFactoryTest {

    private RemoteRepositoryFactory factory;

    // this should be initialized by hand when necessary
    @Mock
    private Authentication mockCredentials;

    private ProxySelector mockProxySelector;

    private List<Repository> repositories;

    @Before
    public void before() {
        repositories = new ArrayList<>();
        factory = new RemoteRepositoryFactory(repositories, mockProxySelector, repository -> mockCredentials);
    }

    @Test
    public void testGetReleaseRepository() throws Exception {
        Repository repository = createDefaultRepository("id");
        RemoteRepository remote = factory.getDeloymentRepository(repository.getId(), false);

        verifyDefaultRepository(repository, remote);
    }

    @Test
    public void testGetReleaseRepositoryOverrides() throws Exception {
        Repository repository = createOverrideRepository("id");
        RemoteRepository remote = factory.getDeloymentRepository(repository.getId(), false);

        assertEquals(repository.getId(), remote.getId());
        assertEquals("releases", remote.getUrl());

        assertNull(remote.getProxy());
        assertNull(remote.getAuthentication());
    }

    @Test
    public void testGetResolutionRepositories() throws Exception {
        Repository repository = createDefaultRepository("id");

        List<RemoteRepository> repositories = factory.getResolutionRepositories(null);
        assertEquals(1, repositories.size());

        verifyDefaultRepository(repository, repositories.get(0));
    }

    @Test
    public void testGetSnapshotRepository() throws Exception {
        Repository repository = createDefaultRepository("id");
        RemoteRepository remote = factory.getDeloymentRepository(repository.getId(), true);

        verifyDefaultRepository(repository, remote);
    }

    @Test
    public void testGetSnapshotRepositoryOverrides() throws Exception {
        mockCredentials();

        Repository repository = createOverrideRepository("id");
        RemoteRepository remote = factory.getDeloymentRepository(repository.getId(), true);

        assertEquals(repository.getId(), remote.getId());
        assertEquals("snapshots", remote.getUrl());

        assertNull(remote.getProxy());
        assertNotNull(remote.getAuthentication());
    }

    private Repository createDefaultRepository(String id) {
        Repository repository = new Repository(id, "url");

        repository.setEnableReleaseRepository(true);
        repository.setReleaseRepository(createRepositoryType(null, null));

        repository.setEnableSnapshotRepository(true);
        repository.setSnapshotRepository(createRepositoryType(null, null));

        repositories.add(repository);

        return repository;
    }

    private Repository createOverrideRepository(String id) {
        Repository repository = createDefaultRepository(id);

        repository.setReleaseRepository(createRepositoryType("releases", "write-snapshot"));
        repository.setSnapshotRepository(createRepositoryType("snapshots", "release-snapshot"));

        return repository;
    }

    private RepositoryType createRepositoryType(String url, String credentialsId) {
        return new RepositoryType(AetherConstants.DEFAULT_CHECKSUM, AetherConstants.DEFAULT_UPDATE, url, credentialsId);
    }

    private void mockCredentials() {
        mockCredentials = mock(Authentication.class);
    }

    private void verifyDefaultRepository(Repository expected, RemoteRepository actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getUrl(), actual.getUrl());

        assertNull(actual.getProxy());
        assertNull(actual.getAuthentication());
    }
}
