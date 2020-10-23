package org.jvnet.hudson.plugins.repositoryconnector.util;

import java.util.Collection;

import org.jvnet.hudson.plugins.repositoryconnector.Messages;
import org.jvnet.hudson.plugins.repositoryconnector.Repository;

import hudson.util.ListBoxModel;

public class RepositoryListBox extends ListBoxModel {

    private static final long serialVersionUID = 1L;

    public RepositoryListBox(Collection<Repository> repositories) {
        repositories.forEach(repository -> {
            String display = repository.getId() + " - " + repository.getUrl();
            add(display, repository.getId());
        });
    }

    public RepositoryListBox withSelectAll() {
        this.add(0, new Option(String.format("-- %s --", Messages.SearchAllRepositories()), ""));
        return this;
    }
}
