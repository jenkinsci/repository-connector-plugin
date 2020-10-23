package org.jvnet.hudson.plugins.repositoryconnector.util;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

import org.jvnet.hudson.plugins.repositoryconnector.Messages;
import org.jvnet.hudson.plugins.repositoryconnector.Repository;

import hudson.model.Item;
import hudson.model.Run;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import jenkins.model.Jenkins;

public class CredentialsUtilities {

    private static final Logger logger = Logger.getLogger(CredentialsUtilities.class.getName());

    public static StandardUsernamePasswordCredentials get(String credentialsId, Item item) {
        return Optional.ofNullable(credentialsId)
                .map(id -> lookupCredentials(id, item))
                .orElse(null);
    }

    public static StandardUsernamePasswordCredentials get(String credentialsId, Run<?, ?> context) {
        return Optional.ofNullable(credentialsId)
                .map(id -> CredentialsProvider.findCredentialById(id, StandardUsernamePasswordCredentials.class, context))
                .orElse(null);
    }

    public static ListBoxModel getListBox(String credentialsId, Jenkins jenkins) {
        if (!jenkins.hasPermission(Jenkins.ADMINISTER)) {
            return new StandardListBoxModel().includeCurrentValue(credentialsId);
        }

        return new StandardUsernameListBoxModel()
                .includeEmptyValue()
                .includeMatchingAs(ACL.SYSTEM, jenkins, StandardUsernamePasswordCredentials.class,
                        Collections.emptyList(), CredentialsMatchers.always());
    }

    public static Repository migrateToCredentialsProvider(Repository repository) {
        if (repository.hasLegacyCredentials()) {
            String repoId = repository.getId();
            logger.log(Level.INFO, "legacy credentials found for repository id [{0}], migrating...", repoId);

            String credentialsId = addToCredentialsProvider(repoId, repository.getUser(), repository.getPassword());
            repository = cloneRepository(repository, credentialsId);
        }

        return repository;
    }

    private static String addToCredentialsProvider(String repoId, String user, String password) {
        String credentialsId = UUID.randomUUID().toString();

        try {
            SystemCredentialsProvider instance = SystemCredentialsProvider.getInstance();

            instance.getCredentials()
                    .add(createCredentials(credentialsId, repoId, user, password));

            instance.save();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        
        return credentialsId;
    }

    private static Repository cloneRepository(Repository old, String credentialsId) {
        Repository repo = new Repository(old);
        repo.setCredentialsId(credentialsId);

        return repo;
    }

    private static UsernamePasswordCredentialsImpl createCredentials(String credentialsId, String repoId, String user, String password) {
        String description = Messages.MigratedDescription(repoId);
        // the username was encrypted prior to using the credentials plugin
        String decryptedUser = Secret.fromString(user).getPlainText();

        return new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, credentialsId, description, decryptedUser, password);
    }

    private static StandardUsernamePasswordCredentials lookupCredentials(String credentialsId, Item item) {
        return CredentialsProvider.lookupCredentials(StandardUsernamePasswordCredentials.class, item, ACL.SYSTEM, Collections.emptyList())
                .stream()
                .filter(credentials -> credentials.getId().equals(credentialsId))
                .findAny()
                .orElse(null);
    }
}
