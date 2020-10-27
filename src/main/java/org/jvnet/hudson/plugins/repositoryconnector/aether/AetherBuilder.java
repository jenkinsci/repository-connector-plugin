package org.jvnet.hudson.plugins.repositoryconnector.aether;

import java.io.File;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.ProxySelector;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.eclipse.aether.util.repository.DefaultProxySelector;
import org.jvnet.hudson.plugins.repositoryconnector.Repository;

import hudson.ProxyConfiguration;
import hudson.Util;
import hudson.util.Secret;

public class AetherBuilder {

    private static final Logger logger = Logger.getLogger(AetherBuilder.class.getName());

    private Function<Repository, Authentication> credentials;

    private final File localDirectory;

    private ProxyConfiguration proxyConfiguration;

    private final Collection<Repository> repositories;

    private PrintStream repositoryConsole;

    private PrintStream transferConsole;

    AetherBuilder(File localDirectory, Collection<Repository> repositories) {
        this.repositories = repositories;
        this.localDirectory = localDirectory;

        this.credentials = repositoryId -> null;
    }

    public Aether build() {
        ProxySelector proxySelector = createProxySelector();

        RepositorySystem repositorySystem = createRepositorySystem();
        RepositorySystemSession repositorySession = createRepositorySession(repositorySystem, proxySelector);

        return new Aether(new RemoteRepositoryFactory(repositories, proxySelector, credentials), repositorySystem, repositorySession);
    }

    public AetherBuilder setCredentials(Function<Repository, Authentication> credentials) {
        this.credentials = credentials;
        return this;
    }

    public AetherBuilder setRepositoryLogger(PrintStream console) {
        this.repositoryConsole = console;
        return this;
    }

    public void setTransferLogger(PrintStream console) {
        this.transferConsole = console;
    }

    private ProxySelector createProxySelector() {
        if (proxyConfiguration == null) {
            return null;
        }

        int port = proxyConfiguration.getPort();
        String name = Util.fixEmpty(proxyConfiguration.getName());

        if (name == null) {
            return null;
        }

        DefaultProxySelector proxySelector = new DefaultProxySelector();
        Authentication authenticator = createAuthentication(proxyConfiguration.getUserName(),
                proxyConfiguration.getSecretPassword());

        String nonProxyHosts = convertJenkinsNoProxyHosts(proxyConfiguration.getNoProxyHost());

        proxySelector.add(new Proxy(Proxy.TYPE_HTTP, name, port, authenticator), nonProxyHosts);
        proxySelector.add(new Proxy(Proxy.TYPE_HTTPS, name, port, authenticator), nonProxyHosts);

        logger.log(Level.FINE, "configured proxy selector using: host={0}, port={1}, user={2}, nonProxyHosts={3}",
                new Object[] { name, port, authenticator, nonProxyHosts });

        return proxySelector;
    }

    static Authentication createAuthentication(String user, Secret password) {
        return new AuthenticationBuilder()
                .addUsername(user)
                .addPassword(password.getPlainText())
                .build();
    }

    private RepositorySystemSession createRepositorySession(RepositorySystem repositorySystem, ProxySelector proxySelector) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        session.setProxySelector(proxySelector);
        session.setConfigProperty("aether.versionResolver.noCache", Boolean.TRUE);

        // local filesystem repository where artifacts will be installed
        LocalRepository localRepository = new LocalRepository(localDirectory, "default");
        logger.log(Level.FINE, "using local maven artifact repository: {0}", localRepository);

        session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(session, localRepository));

        if (repositoryConsole != null) {
            session.setRepositoryListener(new ConsoleRepositoryListener(repositoryConsole));
        }

        if (transferConsole != null) {
            session.setTransferListener(new ConsoleTransferListener(transferConsole));
        }

        return session;
    }

    private RepositorySystem createRepositorySystem() {

        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

        locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
            @Override
            public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {
                logger.log(Level.SEVERE, "Service creation failed for {0} with implementation {1} - {2}",
                        new Object[] { type, impl, exception });
            }
        });

        return locator.getService(RepositorySystem.class);
    }

    // visible for testing
    static String convertJenkinsNoProxyHosts(String noProxyHost) {
        return Optional.ofNullable(Util.fixEmpty(noProxyHost))
                .map(str -> String.join("|", str.split("[ \t\n,|]+")))
                .orElse(null);
    }
}
