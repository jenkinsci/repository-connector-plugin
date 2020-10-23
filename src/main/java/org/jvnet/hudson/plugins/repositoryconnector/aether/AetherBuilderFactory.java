package org.jvnet.hudson.plugins.repositoryconnector.aether;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;

import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.jvnet.hudson.plugins.repositoryconnector.Repository;
import org.jvnet.hudson.plugins.repositoryconnector.util.CredentialsUtilities;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.model.Item;
import hudson.model.Run;
import hudson.util.Secret;

public class AetherBuilderFactory {

    private static final Logger logger = Logger.getLogger(AetherBuilderFactory.class.getName());

    private final Collection<Repository> repositories;

    private final String localDirectory;

    public AetherBuilderFactory(String localDirectory, Repository repository) {
        this(localDirectory, Arrays.asList(repository));
    }

    public AetherBuilderFactory(String localDirectory, Collection<Repository> repositories) {
        this.localDirectory = localDirectory;
        this.repositories = repositories;
    }

    public AetherBuilder createAetherBuilder(Item item) {
        return createAetherBuilder(repository -> getCredentials(repository, item));
    }

    public AetherBuilder createAetherBuilder(Run<?, ?> context) {
        return createAetherBuilder(repository -> getCredentials(repository, context));
    }

    private Authentication getCredentials(Repository repository, Run<?, ?> context) {
        return createAuthentication(CredentialsUtilities.get(repository.getCredentialsId(), context));
    }

    private Authentication getCredentials(Repository repository, Item item) {
        return createAuthentication(CredentialsUtilities.get(repository.getCredentialsId(), item));
    }

    private Authentication createAuthentication(StandardUsernamePasswordCredentials credentials) {
        // convert to Authentication here b/c 'Secret' can't be mocked elsewhere on the credentials class
        return Optional.ofNullable(credentials)
                .map(creds -> createAuthentication(creds.getUsername(), creds.getPassword()))
                .orElse(null);
    }

    private AetherBuilder createAetherBuilder(Function<Repository, Authentication> function) {
        File localRepository = getOrCreateLocalRepository();
        return new AetherBuilder(localRepository, repositories).setCredentials(function);
    }

    static Authentication createAuthentication(String user, Secret password) {
        return new AuthenticationBuilder()
                .addUsername(user)
                .addPassword(password.getPlainText())
                .build();
    }

    @SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED_BAD_PRACTICE", justification = "mkdirs")
    private File getOrCreateLocalRepository() {
        Path path = localDirectory == null ? getTmpPath() : Paths.get(localDirectory);
        File repo = path.toFile();

        if (!repo.exists()) {
            logger.log(Level.FINE, "creating local repository directory [{0}] ", localDirectory);
            repo.mkdirs();
        }

        return repo;
    }

    private Path getTmpPath() {
        return Paths.get(System.getProperty("java.io.tmpdir"), "repositoryconnector-repo");
    }
}
