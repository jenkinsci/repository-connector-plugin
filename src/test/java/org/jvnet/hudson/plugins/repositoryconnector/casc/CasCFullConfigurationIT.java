package org.jvnet.hudson.plugins.repositoryconnector.casc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.jvnet.hudson.plugins.repositoryconnector.Repository;
import org.jvnet.hudson.plugins.repositoryconnector.Repository.RepositoryType;

public class CasCFullConfigurationIT extends CasCBasicConfigurationIT {

    @Override
    protected String configResource() {
        return "casc-full.yml";
    }

    @Override
    protected String getLocalRepositoryValue() {
        return "/tmp";
    }

    @Override
    protected String stringInLogExpected() {
        return "credentialsId=user-pass";
    }

    @Override
    protected void validateRepositporyConfiguration(Repository repository) {
        assertFalse(repository.isEnableReleaseRepository());
        assertFalse(repository.isEnableSnapshotRepository());

        // even though they aren't in use, the values are still set
        RepositoryType releases = repository.getReleaseRepository();

        assertEquals("fail", releases.getChecksum());
        assertEquals("daily", releases.getUpdate());
        assertEquals("http://domain.com/content/repositories/releases", releases.getUrl());
        assertEquals("release-write", releases.getCredentialsId());

        RepositoryType snapshots = repository.getSnapshotRepository();

        assertEquals("warn", snapshots.getChecksum());
        assertEquals("daily", snapshots.getUpdate());
        assertEquals("http://domain.com/content/repositories/snapshots", snapshots.getUrl());
        assertEquals("snapshot-write", snapshots.getCredentialsId());
    }
}
