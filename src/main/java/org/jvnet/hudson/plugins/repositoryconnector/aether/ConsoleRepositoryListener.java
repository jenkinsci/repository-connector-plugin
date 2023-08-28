package org.jvnet.hudson.plugins.repositoryconnector.aether;

import java.io.PrintStream;

import org.eclipse.aether.RepositoryEvent;

import hudson.model.Run;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.spi.connector.layout.RepositoryLayoutProvider;

public class ConsoleRepositoryListener extends RecorderRepositoryListener {

    private PrintStream out;

    public ConsoleRepositoryListener(PrintStream out, RepositoryLayoutProvider layoutProvider, RepositorySystemSession session, Run<?, ?> context) {
        super(layoutProvider, session, context);
        this.out = (out != null) ? out : System.out;
    }

    @Override
    public void artifactDeployed(RepositoryEvent event) {
        super.artifactDeployed(event);
        out.println("Deployed " + event.getArtifact() + " to " + event.getRepository());
    }

    @Override
    public void artifactDeploying(RepositoryEvent event) {
        out.println("Deploying " + event.getArtifact() + " to " + event.getRepository());
    }

    @Override
    public void artifactDescriptorInvalid(RepositoryEvent event) {
        out.println("Invalid artifact descriptor for " + event.getArtifact() + ": " + event.getException().getMessage());
    }

    @Override
    public void artifactDescriptorMissing(RepositoryEvent event) {
        out.println("Missing artifact descriptor for " + event.getArtifact());
    }

    @Override
    public void artifactInstalled(RepositoryEvent event) {
        out.println("Installed " + event.getArtifact() + " to " + event.getFile());
    }

    @Override
    public void artifactInstalling(RepositoryEvent event) {
        out.println("Installing " + event.getArtifact() + " to " + event.getFile());
    }

    @Override
    public void artifactResolved(RepositoryEvent event) {
        if (event.getRepository() == null) {
            out.println("Failed to resolve artifact " + event.getArtifact());
        } else {
            out.println("Resolved artifact " + event.getArtifact() + " from " + event.getRepository());
        }
    }

    @Override
    public void artifactResolving(RepositoryEvent event) {
        out.println("Resolving artifact " + event.getArtifact());
    }

    @Override
    public void metadataDeployed(RepositoryEvent event) {
        super.metadataDeployed(event);
        out.println("Deployed " + event.getMetadata() + " to " + event.getRepository());
    }

    @Override
    public void metadataDeploying(RepositoryEvent event) {
        out.println("Deploying " + event.getMetadata() + " to " + event.getRepository());
    }

    @Override
    public void metadataInstalled(RepositoryEvent event) {
        out.println("Installed " + event.getMetadata() + " to " + event.getFile());
    }

    @Override
    public void metadataInstalling(RepositoryEvent event) {
        out.println("Installing " + event.getMetadata() + " to " + event.getFile());
    }

    @Override
    public void metadataInvalid(RepositoryEvent event) {
        out.println("Invalid metadata " + event.getMetadata());
    }

    @Override
    public void metadataResolved(RepositoryEvent event) {
        out.println("Resolved metadata " + event.getMetadata() + " from " + event.getRepository());
    }

    @Override
    public void metadataResolving(RepositoryEvent event) {
        out.println("Resolving metadata " + event.getMetadata() + " from " + event.getRepository());
    }
}
