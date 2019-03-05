/**
 * 
 */
package org.jvnet.hudson.plugins.repositoryconnector;

import java.io.Serializable;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.util.Secret;

/**
 * Represents a repository where artifacts can be resolved from or uploaded to.
 * 
 * @author domi
 */
public class Repository implements Serializable, Comparable {

    private static final long serialVersionUID = 1L;

    final private String url;
    final private String type;
    final private String id;
    final private Secret user;
    final private Secret password;
    final private boolean isRepositoryManager;

    @DataBoundConstructor
    public Repository(String id, String type, String url, String user, String password, boolean repositoryManager) {
        this.id = id == null ? "central" : id;
        this.type = type == null ? "default" : type;
        this.url = url;
        this.isRepositoryManager = repositoryManager;

        // this object should really be used here but that requires a config file migration...
        // see https://wiki.jenkins.io/display/JENKINS/Hint+on+retaining+backward+compatibility
        this.user = UserPwd.toSecret(user);
        this.password = UserPwd.toSecret(password);
    }

    /**
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "[Repository id=" + id + ", type=" + type + ", url=" + url + ", isRepositoryManager=" + isRepositoryManager + "]";
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Repository)) {
            return false;
        }
        Repository other = (Repository) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        if (type == null) {
            if (other.type != null) {
                return false;
            }
        } else if (!type.equals(other.type)) {
            return false;
        }
        return true;
    }

    /**
     * @return the user
     */
    public String getUser() {
        return Secret.toString(user);
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return Secret.toString(password);
    }

    /**
     * @return the isRepositoryManager
     */
    public boolean isRepositoryManager() {
        return isRepositoryManager;
    }

    public int compareTo(Object o) {
        return id.compareTo(((Repository) o).getId());
    }
}
