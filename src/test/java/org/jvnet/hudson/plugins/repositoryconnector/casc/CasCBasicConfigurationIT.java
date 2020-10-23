package org.jvnet.hudson.plugins.repositoryconnector.casc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.jvnet.hudson.plugins.repositoryconnector.Repository;
import org.jvnet.hudson.plugins.repositoryconnector.RepositoryConfiguration;
import org.jvnet.hudson.test.RestartableJenkinsRule;

import io.jenkins.plugins.casc.misc.RoundTripAbstractTest;

public class CasCBasicConfigurationIT extends RoundTripAbstractTest {

    @Override
    protected final void assertConfiguredAsExpected(RestartableJenkinsRule j, String configContent) {
        Repository repository = getRepositoryFromConfiguration();

        assertEquals(Repository.CENTRAL, repository.getId());
        assertEquals(Repository.ENDPOINT, repository.getUrl());

        validateRepositporyConfiguration(repository);
    }

    @Override
    protected String configResource() {
        return "casc-basic.yml";
    }

    protected String getLocalRepositoryValue() {
        return null;
    }

    @Override
    protected String stringInLogExpected() {
        return "id=central";
    }

    protected void validateRepositporyConfiguration(Repository repository) {
        assertTrue(repository.isEnableReleaseRepository());
        assertTrue(repository.isEnableSnapshotRepository());
    }

    private Repository getRepositoryFromConfiguration() {
        RepositoryConfiguration configuration = RepositoryConfiguration.get();
        
        assertNotNull(configuration);
        assertEquals(getLocalRepositoryValue(), configuration.getLocalRepository());
        
        Iterator<Repository> iterator = configuration.getRepositories().iterator();
        assertTrue(iterator.hasNext());

        return iterator.next();
    }
}
