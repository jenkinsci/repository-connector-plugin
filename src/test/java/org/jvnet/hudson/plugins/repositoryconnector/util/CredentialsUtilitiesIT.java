package org.jvnet.hudson.plugins.repositoryconnector.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.plugins.repositoryconnector.Repository;
import org.jvnet.hudson.plugins.repositoryconnector.Repository.RepositoryType;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.util.ListBoxModel;
import hudson.util.Secret;

public class CredentialsUtilitiesIT {

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Test
    public void testMigrateToCredentialsProvider() {
        Repository repository = createRepository();
        Repository migrated = CredentialsUtilities.migrateToCredentialsProvider(repository);

        verify(repository, migrated);

        // not the best but...
        String credentialsId = migrated.getCredentialsId();
        ListBoxModel model = CredentialsUtilities.getListBox(credentialsId, jenkinsRule.getInstance());

        assertEquals(2, model.size());
        // the first option will be "empty"
        assertEquals(credentialsId, model.get(1).value);
    }

    private Repository createRepository() {
        // we're not connecting, so this is fine
        Repository repository = Repository.MAVEN_CENTRAL;

        Secret user = Secret.fromString("user");
        Secret pass = Secret.fromString("pass");

        // the old values were encrypted
        repository.setUser(user.getEncryptedValue());
        repository.setPassword(pass.getEncryptedValue());

        return repository;
    }

    private void verify(Repository repository, Repository migrated) {
        assertEquals(repository.getId(), migrated.getId());
        assertEquals(repository.getUrl(), migrated.getUrl());

        assertNotNull(migrated.getCredentialsId());

        verify(repository.getReleaseRepository(), migrated.getReleaseRepository());
        assertEquals(repository.isEnableReleaseRepository(), migrated.isEnableReleaseRepository());

        verify(repository.getSnapshotRepository(), migrated.getSnapshotRepository());
        assertEquals(repository.isEnableSnapshotRepository(), migrated.isEnableSnapshotRepository());

        assertNull(migrated.getUser());
        assertNull(migrated.getPassword());
    }

    private void verify(RepositoryType type, RepositoryType migrated) {
        assertEquals(type.getChecksum(), migrated.getChecksum());
        assertEquals(type.getUpdate(), migrated.getUpdate());

        assertEquals(type.getUrl(), migrated.getUrl());
        assertEquals(type.getCredentialsId(), migrated.getCredentialsId());
    }
}
