package org.jvnet.hudson.plugins.repositoryconnector.aether;

import hudson.model.Run;
import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.spi.connector.layout.RepositoryLayoutProvider;

public class RecorderRepositoryListener extends AbstractRepositoryListener {

    private final RecorderAction action;

    public RecorderRepositoryListener(RepositoryLayoutProvider repositoryLayoutProvider, RepositorySystemSession session, Run<?, ?> context) {
        // assumption: artifacts will only be deployed/installed when run is not null.
        // Otherwise, we're just constructing the Job metadata (adding the post-build step)
        // which will not actually perform these actions.
        action = new RecorderAction(repositoryLayoutProvider, session);
        if (context != null) {
            // inject an action to use for artifact detail persistence.
            context.addAction(action);
        }
    }

    @Override
    public void artifactDeployed(RepositoryEvent event) {
        action.recordArtifactDeployed(event);
    }

    @Override
    public void metadataDeployed(RepositoryEvent event) {
        action.recordMetadataDeployed(event);
    }
}
