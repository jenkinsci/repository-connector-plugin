package org.jvnet.hudson.plugins.repositoryconnector;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.json.JSONObject;

import org.jenkinsci.Symbol;
import org.jvnet.hudson.plugins.repositoryconnector.aether.AetherBuilderFactory;
import org.jvnet.hudson.plugins.repositoryconnector.util.CredentialsUtilities;
import org.jvnet.hudson.plugins.repositoryconnector.util.FormValidator;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import hudson.Extension;
import hudson.Util;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;

/**
 * Global configuration for the <code>Repository Connector</code> plugin.
 *
 * @author mrumpf
 */
@Extension
@Symbol("repository-connector")
public class RepositoryConfiguration extends GlobalConfiguration {

    private static Logger logger = Logger.getLogger(RepositoryConfiguration.class.getName());

    private String localRepository;

    private boolean migratedCredentials;

    private final Map<String, Repository> repositories;

    public RepositoryConfiguration() {
        this.repositories = new HashMap<>();
        load();
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        return super.configure(req, json);
    }

    public FormValidation doCheckLocalRepository(@QueryParameter String localRepository) {
        return FormValidator.validateLocalDirectory(localRepository);
    }

    public String getLocalRepository() {
        return localRepository;
    }

    public Collection<Repository> getRepositories() {
        return Collections.unmodifiableCollection(repositories.values());
    }

    public boolean hasMultipleRepositories() {
        return repositories.size() > 1;
    }

    @Override
    public synchronized void load() {
        super.load();

        if (repositories.size() == 0) {
            logger.info("no saved repositories found, initializing list using maven central default");
            setRepositories(Arrays.asList(Repository.MAVEN_CENTRAL));
        }
    }

    @Initializer(after = InitMilestone.JOB_LOADED)
    public void migrateCredentials() {
        if (migratedCredentials) {
            logger.info("skipping credentials migration, previously run");
            return;
        }

        for (Repository repository : repositories.values()) {
            repositories.put(repository.getId(), CredentialsUtilities.migrateToCredentialsProvider(repository));
        }

        migratedCredentials = true;
        save();
    }

    @DataBoundSetter
    public void setLocalRepository(String localRepository) {
        this.localRepository = Util.fixEmpty(localRepository);
        save();
    }

    @DataBoundSetter
    public void setRepositories(Collection<Repository> toAdd) {
        repositories.clear();

        toAdd.forEach(repository -> {
            logger.log(Level.INFO, "adding repository [{0}]", repository);
            repositories.put(repository.getId(), repository);
        });

        save();
    }

    public static AetherBuilderFactory createAetherFactory() {
        RepositoryConfiguration configuration = get();
        return new AetherBuilderFactory(configuration.getLocalRepository(), configuration.getRepositories());
    }

    public static RepositoryConfiguration get() {
        return GlobalConfiguration.all().get(RepositoryConfiguration.class);
    }
}
