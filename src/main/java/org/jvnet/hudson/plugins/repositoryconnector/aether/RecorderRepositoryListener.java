package org.jvnet.hudson.plugins.repositoryconnector.aether;

import hudson.model.Run;
import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.layout.RepositoryLayout;
import org.eclipse.aether.spi.connector.layout.RepositoryLayoutProvider;
import org.eclipse.aether.transfer.NoRepositoryLayoutException;

import java.net.URI;

public class RecorderRepositoryListener extends AbstractRepositoryListener {

    private final RecorderAction action;

    private final RepositoryLayoutProvider repositoryLayoutProvider;

    private final RepositorySystemSession session;

    public RecorderRepositoryListener(RepositoryLayoutProvider repositoryLayoutProvider, RepositorySystemSession session, Run<?, ?> context) {
        this.repositoryLayoutProvider = repositoryLayoutProvider;
        this.session = session;
        // assumption: artifacts will only be deployed/installed when run is not null.
        // Otherwise, we're just constructing the Job metadata (adding the post-build step)
        // which will not actually perform these actions.
        action = new RecorderAction();
        if (context != null) {
            // inject an action to use for artifact detail persistence.
            context.addAction(action);
        }
    }

    @Override
    public void artifactDeployed(RepositoryEvent event) {
        action.recordArtifactDeployed(event, getRepositoryLayout(event));
    }

    @Override
    public void metadataDeployed(RepositoryEvent event) {
        action.recordMetadataDeployed(event, getRepositoryLayout(event));
    }

    private RepositoryLayout getRepositoryLayout(RepositoryEvent event) {
        if (!(event.getRepository() instanceof RemoteRepository)) {
            return null;
        }
        try {
            return repositoryLayoutProvider.newRepositoryLayout(session, (RemoteRepository) event.getRepository());
        } catch (NoRepositoryLayoutException ignored) {
            return null;
        }
    }
}
